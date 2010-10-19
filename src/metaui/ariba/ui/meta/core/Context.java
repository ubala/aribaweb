/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Context.java#39 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.core.AWBindableElement;
import ariba.ui.meta.editor.OSSWriter;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Constants;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.Extensible;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;

import org.apache.log4j.Level;

/**
    Context represents a stack of assignments (e.g. class=User, field=birthDay, operation=edit)
    The current set of assignments can be retrieved via values().

    The current values are run against the Meta rule set to compute the effective PropertyMap
    (e.g. visible:true, editable:true, component:AWTextField).
    Some rule evaluations result in *chaining* -- where additional assignments that are
    "implied" by the current assignments are applied, (resulting in a revised computation
    of the current PropertyMap, and possible further chaining).
    (e.g. field=birthDay may result in type=Date which may result in component:DatePicker)

    Assignments can be scoped and popped (push(), set(key, value); ...; pop()).

    The actual computation of rule matches is cached so once a "path" down the context
    tree has been exercized subsequent matching traversals (even by other threads/users)
    is fast.    
 */
public class Context implements Extensible
{
    private static boolean _CacheActivations = true;
    private static boolean _ExpensiveContextConsistencyChecksEnabled = false;
    protected static boolean _DebugRuleMatches = false;
    protected static int MaxContextStackSize = 200;
    public static final Meta.PropertyMap EmptyMap = new Meta.PropertyMap();
    
    Meta _meta;
    Map<String, Object> _values = new HashMap();
    List <_Assignment> _entries = new ArrayList();
    List <Integer> _frameStarts = new ArrayList();
    PropertyAccessor _accessor = new PropertyAccessor();
    Meta.PropertyMap _currentProperties;
    _Activation _rootNode;
    _Activation _currentActivation;
    List <_Assignment> _recPool = new ArrayList();

    static {
        FieldValue.registerClassExtension(Context.PropertyAccessor.class,
                               new FieldValue_ContextPropertyAccessor());
    }

    public Context (Meta meta)
    {
        _meta = meta;
        _currentActivation = _rootNode = getActivationTree(meta);
    }

    public void push ()
    {
        _frameStarts.add(Constants.getInteger(_entries.size()));
    }

    public void pop ()
    {
        int frameStartsSize = _frameStarts.size();
        Assert.that(frameStartsSize > 0, "Popping empty stack");
        int pos = _frameStarts.remove(frameStartsSize-1);
        int entriesSize;
        while ((entriesSize = _entries.size()) > pos) {
            int recIdx = entriesSize - 1;
            _Assignment rec = _entries.remove(recIdx);
            if (rec.srec.lastAssignmentIdx == -1) {
                _values.remove(rec.srec.key);
            } else {
                _undoOverride(rec, recIdx);
            }

            _currentActivation = (recIdx > 0)
                    ? _entries.get(recIdx - 1).srec.activation
                    : _rootNode;
            assertContextConsistent();            

            // check rec back into pool for reuse
            rec.reset();
            _recPool.add(rec);
        }
        _currentProperties = null;
    }

    public void set (String key, Object value)
    {
        _set(key, value, false, false);
    }

    public void merge (String key, Object value)
    {
        _set(key, value, true, false);
    }

    public void setScopeKey(String key)
    {
        Assert.that(meta().keyData(key).isPropertyScope(), "%s is not a valid context key", key);
        String current = _currentPropertyScopeKey();
        // Assert.that(current != null, "Can't set %s as context key when no context key on stack", key);
        // FIXME: if current key isChaining then we need to set again to get a non-chaining assignment
        if (!key.equals(current)) {
            Object val = values().get(key);
            // Assert.that(val != null, "Can't set %s as context key when it has no value already on the context", key);
            if (val == null) val = Meta.KeyAny;
            set(key, val);
        }
    }

    public Meta meta ()
    {
        return _meta;
    }

    public Map values ()
    {
        Map propVals;
        return (_entries.isEmpty() || ((propVals = ListUtil.lastElement(_entries).propertyLocalValues(this)) == null))
                ? _values : propVals;
    }

    // FieldValue lookup will find extra props in our values hashtable
    public Map extendedFields ()
    {
        return values();
    }

    // For FieldValue
    public Object properties()
    {
        return _accessor;
    }

    public Object propertyForKey (String key)
    {
        Object val = allProperties().get(key);
        return resolveValue(val);
    }

    public List listPropertyForKey (String key)
    {
        Object val = propertyForKey(key);
        return (val == null) ? new ArrayList()
                : (val instanceof List) ? (List)val : ListUtil.list(val);
    }

    public boolean booleanPropertyForKey (String key, boolean defaultVal)
    {
        Object val = propertyForKey(key);
        return (val == null) ? defaultVal
                : (Boolean)val;
    }

    public Meta.PropertyMap allProperties ()
    {
        if (_currentProperties == null) {
            Match.MatchResult m = lastMatch();
            if (m != null) {
                _currentProperties = m.properties();
            }
        }
        return (_currentProperties != null) ? _currentProperties : EmptyMap;
    }

    public Object resolveValue (Object value)
    {
        Object lastValue = null;
        while (value != lastValue && value != null && value instanceof PropertyValue.Dynamic) {
            lastValue = value;
            value = ((PropertyValue.Dynamic)value).evaluate(this);
        }
        return value;
    }

    public Object staticallyResolveValue (Object value)
    {
        Object lastValue = null;
        while (value != lastValue && value != null && value instanceof PropertyValue.StaticallyResolvable) {
            lastValue = value;
             value = ((PropertyValue.Dynamic)value).evaluate(this);
        }
        return value;
    }

    public Object pushAndResolve (Map<String,Object> contextVals, String propertyKey, boolean staticResolve)
    {
        String scopeKey = null;
        push();
        for (Map.Entry<String,Object> e : contextVals.entrySet()) {
            Object val = e.getValue();
            if ("*".equals(val)) {
                scopeKey = e.getKey();
            } else {
                set(e.getKey(),val);
            }
        }
        if (scopeKey != null) setScopeKey(scopeKey);
        Object val = allProperties().get(propertyKey);
        val = staticResolve ? staticallyResolveValue(val) : resolveValue(val);
        pop();

        return val;
    }

    public Object pushAndResolve (Map<String,Object> contextVals, String propertyKey)
    {
        return pushAndResolve(contextVals, propertyKey, false);
    }

    // a (usable) snapshot of the current state of the context
    public Snapshot snapshot()
    {
        return new Snapshot(this);
    }

    public static class Snapshot
    {
        Meta _meta;
        Class _origClass;
        List <_AssignmentSnapshot> _assignments;

        public Context hydrate ()
        {
            Context context = (Context)newInnerInstance(_origClass, _meta);
            for (_AssignmentSnapshot a: _assignments) {
                context.set(a.key, a.value);
            }
            return context;
        }

        Snapshot (Context context)
        {
            _meta = context.meta();
            _origClass = context.getClass();
            _assignments = context._activeAssignments();
        }
    }


    /**
        Implementation notes:

        Context maintains a stack (_entries) of _ContextRecs (one per assignment) as well as
        as _frameStack recording the stack positions for each push()/pop().

        Performance through aggressive global caching of all statically computatble data:
            - The static (reusable/immutable) part of a ContextRec is factored into _StaticRec
            - StaticRecs represent individual assignments (context key = value) and cache the
                 resulting Meta.MatchResult (and associated PropertyMap)
            - The sub-stack (of forward chained) records associated with each external set()
              (or chained *dynamic* value) is recorded in an Activation.
            - Process-global tree of Activations
                - each activation keeps list of its ContextKey/Value-keyed decended Activations

        Thread-safety considerations
            - The Activations, and their list of _StaticRecs are global and so are accessed
              by multiple threads concurrently.  These structures are essentially *grow-only* --
              once an Activation is created it (and its _StaticRecs) are immutable.  Only then
              is it pushed onto the (GrowOnly) shared Activation tree.

        Property Contexts.
            The notion of a "PropertyContext" makes the going tricky...
            A "PropertyContextKey" is a key for an "entity" that properties describe.
            (e.g. class, field, action, and layout are property context keys, but editing,
            operation, ... are not)
            E.g. On an assignment stack with module=Admin class=Foo, field=name, editable=false,
            we want the property "label" to be the label for the *field*, not the class or module
            -- i.e. the *top-most* assignment of a PropertyContextKey determines which property
            context rules are active.

            These rules are activated via a synthetic context key of like "field_p" or "class_p".
            Logically, after each assigment we need to figure of which context key should be in
            affect an set it on the context, but then automatically pop it off upon the next
            assignment (and then recompute again).

            Of course, actually pushing and popping context key assignment on every set()
            would be expensive so instead we cache the "propertyActivation" associated with
            each activation, and use its values and properties rather than those on the
            activation.
     */

    // the root activation (cache)
    static _Activation getActivationTree (Meta meta)
    {
        _Activation root = (_Activation)meta.identityCache().get(_Activation.class);
        if (root == null) {
            synchronized (_Activation.class) {
                root = (_Activation)meta.identityCache().get(_Activation.class);
                if (root == null) {
                    root = new _Activation(null);
                    meta.identityCache().put(_Activation.class, root);
                }
            }
        }
        return root;
    }

    _Activation currentActivation ()
    {
        return _currentActivation;
    }

    static class _AssignmentSnapshot {String key; Object value; }

    List<_AssignmentSnapshot> _activeAssignments ()
    {
        List<_AssignmentSnapshot> list = new ArrayList();
        for (int i=0, c=_entries.size(); i<c; i++) {
            _Assignment rec = _entries.get(i);
            if (rec.maskedByIdx == 0 && !rec.srec.fromChaining) {
                _AssignmentSnapshot a = new _AssignmentSnapshot();
                a.key = rec.srec.key;
                a.value = rec.val;
                list.add(a);
            }
        }
        return list;
    }

    protected void _set (String key, Object value, boolean merge, boolean chaining)
    {
        Object sval = _meta.transformValue(key, value);
        boolean didSet = false;
        _Activation activation = _currentActivation.getChildActivation(key, sval, chaining);
        if (activation == null) {
            synchronized (_currentActivation) {
                activation = _currentActivation.getChildActivation(key, sval, chaining);
                if (activation == null) {
                    Log.meta_detail.debug("Creating new activation for %s: %s", key, value);
                    didSet = _createNewFrameForSet(key, sval, value, merge, chaining);
                }
            }
        }

        if (activation != null) {
            Log.meta_context.debug("Found existing activation for %s: %s", key, value);
            didSet = _applyActivation(activation, value);
            if (Log.meta_context.isEnabledFor(Level.DEBUG)) _logContext();
        }

        if (didSet) {
            awakeCurrentActivation();
            if (Log.meta_detail.isEnabledFor(Level.DEBUG)) _logContext();
        }
    }

    _Assignment newContextRec ()
    {
        int count = _recPool.size();
        return (count > 0) ? _recPool.remove(count - 1) : new _Assignment();
    }

    /**
        Cached case: apply a previously computed Activation
     */
    boolean _applyActivation (_Activation activation, Object firstVal)
    {
        Assert.that(activation._parent == _currentActivation, "Attempt to apply activation on mismatched parent");
        if (_entries.size() != activation._origEntryCount) {
            Assert.that(false, "Mismatched context stack size (%s) from when activation was popped (%s)",
                    Integer.toString(_entries.size()),
                    Integer.toString(activation._origEntryCount));
        }
        int count=activation._recs.size();
        if (count == 0) return false;
        for (int i=0; i<count; i++) {
            _StaticRec srec = activation._recs.get(i);
            _Assignment rec = newContextRec();
            rec.srec = srec;

            // Apply masking for any property that we mask out
            if (srec.lastAssignmentIdx != -1) {
                _prepareForOverride(_entries.size(), srec.lastAssignmentIdx);
            }

            rec.val = (i == 0 && firstVal != Meta.NullMarker) ? firstVal : srec.val;
            _values.put(srec.key, rec.val);
            _entries.add(rec);
        }
        _currentActivation = activation;
        _currentProperties = null;
        return true;
    }

    private void awakeCurrentActivation ()
    {
        // See if this activation requires further chaining
        List<_DeferredAssignment> deferredAssignments = _currentActivation.deferredAssignments;
        if (deferredAssignments != null) {
            applyDeferredAssignments(deferredAssignments);
        }
    }

    private void applyDeferredAssignments (List<_DeferredAssignment> deferredAssignments)
    {
        for (_DeferredAssignment da : deferredAssignments) {
            // verify that deferred value still applies
            Object currentPropValue = staticallyResolveValue(allProperties().get(da.key));
            if (da.value.equals(currentPropValue)) {
                Object resolvedValue = resolveValue(da.value);
                Log.meta_context.debug("_set applying deferred assignment of derived value: %s <- %s (%s)",
                        da.key, resolvedValue, da.value);
                _set(da.key, resolvedValue, false, true);
            } else {
                Log.meta_context.debug("_set SKIPPING deferred assignment of derived value: %s <- %s --" +
                        " no longer matches property in context: %s",
                        da.key, da.value, currentPropValue);
            }
        }
    }

    boolean _inDeclare ()
    {
        Match.MatchResult match = lastMatchWithoutContextProps();
        return match != null && (match._keysMatchedMask & _meta.declareKeyMask()) != 0;
    }

    /**
        Non-cached access: create a new activation
     */
    boolean _createNewFrameForSet (String key, Object svalue, Object value, boolean merge, boolean chaining)
    {
        _Activation lastActivation = _currentActivation;
        _Activation newActivation = new _Activation(lastActivation);
        newActivation._origEntryCount = _entries.size();
        _currentActivation = newActivation;

        // set this value
        boolean didSet =_set(key, svalue, value, merge, chaining);
        // mirror properties
        if (didSet) while (_checkApplyProperties()) /* repeat */;

        // Remember for the future
        if (_CacheActivations) lastActivation.cacheChildActivation(key, svalue, newActivation, chaining);

        _currentActivation = (didSet) ? newActivation : lastActivation;

        return _currentActivation != lastActivation;
    }

    /**
        Called lazily to compute the property activation for this activation
     */
    private _Activation _createNewPropertyContextActivation (_Activation parentActivation)
    {
        // Compute the static part of the property activation
        // we accumulate the property settings on a side activation off the main stack
        // and apply it virtually if our parent is not covered
        // (that way we don't have to apply and unapply all the time)
        push();
        _Activation propActivation = new _Activation(parentActivation);
        propActivation._origEntryCount = _entries.size();

        _currentActivation = propActivation;
        Map origValues = _values;
        NestedMap nestedMap = new NestedMap(origValues);
        _values = nestedMap;
        applyPropertyContextAndChain();
        if (propActivation._recs.size() > 0 || propActivation.deferredAssignments != null) {
            propActivation._nestedValues = nestedMap;
            _values = EmptyRemoveMap;  // hack -- empty map so that undo is noop -- ((NestedMap)_values).dup();
        } else {
            propActivation = null;
        }
        pop();
        _values = origValues;
        _currentActivation = parentActivation;

        return propActivation;
    }

    static final Map EmptyRemoveMap = new HashMap();

    /**
        Apply a previously created / shared prop activation (applyNewPropertyContextActivation)
     */
    private void _applyPropertyActivation (_Activation propActivation, _Assignment rec)
    {
        Map propValues = _values;
        if (propActivation._nestedValues != null) {
            propValues = propActivation._nestedValues.reparentedMap(propValues);
        }

        // set up propLocal results
        // Now, see if we need to compute a dynamic property activation as well
        if (propActivation.deferredAssignments != null) {
            push();
            // nest a dynamic nested map on our static nested map (which is on our last dynamic nested map...)
            Map origValues = _values;
            _values = new NestedMap(propValues);
            _applyActivation(propActivation, Meta.NullMarker);
            applyDeferredAssignments(propActivation.deferredAssignments);
            rec._propertyLocalValues = _values;
            rec._propertyLocalSrec = ListUtil.lastElement(_entries).srec;
            _values = EmptyRemoveMap;  // hack -- empty map so that undo is noop -- ((NestedMap)_values).dup();
            pop();
            _values = origValues;
        }
        else {
            // can use static versions
            rec._propertyLocalValues = propValues;
            rec._propertyLocalSrec = ListUtil.lastElement(propActivation._recs);
        }
    }
    
    protected boolean _isNewValue (Object oldVal, Object newVal)
    {
        return (oldVal != newVal && (oldVal == null ||
                (!oldVal.equals(newVal)
                        && (!(oldVal instanceof List) || !((List)oldVal).contains(newVal)))));
    }

    protected boolean isDeclare ()
    {
        return propertyForKey(Meta.KeyDeclare) != null;
    }

    protected void assertContextConsistent ()
    {
        if (!_ExpensiveContextConsistencyChecksEnabled) return;

        // Verify that each value in context has matching (enabled) context record
        for (Map.Entry<String, Object> e : _values.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            int lastAssignmentIdx = findLastAssignmentOfKey(key);
            Assert.that(lastAssignmentIdx >=0, "Value in context but no assignment record found (%s = %s)",
                    key, val);
            Object contextVal = _entries.get(lastAssignmentIdx).val;
            Assert.that (val==contextVal || (val != null && val.equals(contextVal)),
                    "Value in context (%s) doesn't match value on stack (%s)",
                    val, contextVal);
        }

        // check entries for proper relationship with any previous records that they override
        for (int i=_entries.size()-1 ; i >=0; i--) {
            _Assignment r = _entries.get(i);
            boolean foundFirst = false;
            for (int j=i-1; j >=0; j--) {
                _Assignment pred = _entries.get(j);
                if (pred.srec.key.equals(r.srec.key)) {
                    // Predecessors must be masked
                    Assert.that((!foundFirst && pred.maskedByIdx == i) || ((foundFirst || pred.srec.fromChaining) && pred.maskedByIdx > 0),
                            "Predecessor (%s=%s) does not have matching maskedByIdx (%s != %s) for override (%s)",
                            pred.srec.key, pred.val, Integer.toString(pred.maskedByIdx), Integer.toString(i),
                            r.val);
                    Assert.that(((!foundFirst && r.srec.lastAssignmentIdx == j) || foundFirst || pred.srec.fromChaining), 
                            "Override (%s=%s) does not have proper lastAssignmentIdx (%s != %s) for predecessor (%s)",
                            r.srec.key, r.val, Integer.toString(r.srec.lastAssignmentIdx), Integer.toString(j),
                            pred.val);
                    foundFirst = true;
                }
            }
        }
    }


    protected boolean _set (String key, Object svalue, Object value, boolean merge, boolean isChaining)
    {
        boolean hasOldValue = _values.containsKey(key);
        Object oldVal = hasOldValue ? _values.get(key) : null;
        boolean isNewValue = !hasOldValue || _isNewValue(oldVal, value);
        boolean matchingPropKeyAssignment = !isNewValue && !isChaining
                && ((meta().keyData(key).isPropertyScope())
                        && !key.equals(_currentPropertyScopeKey()));
        if (isNewValue || matchingPropKeyAssignment) {
            Match.MatchResult lastMatch= null, newMatch = null;
            int salience = _frameStarts.size();
            int lastAssignmentIdx = -1;
            if (oldVal == null) {
                lastMatch = lastMatchWithoutContextProps();
            }
            else {
                // We recompute that match up to this point by recomputing forward
                // from the point of the last assignment to this key (skipping it), then
                // match against the array of our value and the old
                int recIdx = _entries.size();
                lastAssignmentIdx = findLastAssignmentOfKey(key);
                Assert.that(lastAssignmentIdx >=0, "Value in context but no assignment record found (%s = %s)",
                        key, oldVal);

                if (matchingPropKeyAssignment) {
                    // cheap version of masking for a matching set:
                    _entries.get(lastAssignmentIdx).maskedByIdx = recIdx;
                    lastMatch = lastMatchWithoutContextProps();
                }
                else {
                    // be able to override a non-chaining assignment.  Our problem is, though, if
                    // the developer wanted to force a re-assignment in the new frame, we'd filter it
                    // out as a duplicate assignment above.  Now, we could allow that assignment through,
                    // but it would then break invariants when searching back to mask a key (we wouldn't
                    // realize that we need to go further back to find the original one).
                    _Assignment oldRec = _entries.get(lastAssignmentIdx);
                    if (oldRec.srec.salience == salience) {
                        int prev = findLastAssignmentOfKey(key, value);
                        if (prev != -1 && _entries.get(prev).srec.salience == salience) {
                            Log.meta_detail.debug("Set of key '%s': '%s' -> '%s' skipped (found previous assignment of same value with same salience)",
                                key, oldVal, value);
                            return false;
                        }
                    }
                    if (isChaining && (oldRec.srec.salience > salience || !oldRec.srec.fromChaining)) {
                        Log.meta_detail.debug("Set of key '%s': '%s' -> '%s' skipped (salience %s <= %s)",
                            key, oldVal, value, salience, oldRec.srec.salience);
                        return false;
                    }
                    int firstAssignmentIdx = _prepareForOverride(recIdx, lastAssignmentIdx);
                    newMatch = _rematchForOverride(key, svalue, recIdx, firstAssignmentIdx);

                    if (merge) value = Meta.PropertyMerger_List.merge(oldVal, value, isDeclare());
                }
                // Todo: this isn't quite right -- a chaining assignment from a *newer frame* should
            }

            Assert.that(_entries.size() <= MaxContextStackSize, "MetaUI context stack exceeded max size (%s) -- likely infinite chaining", _entries.size());
            _StaticRec srec = new _StaticRec();
            srec.key = key;
            // todo: conversion
            srec.val = svalue;
            srec.lastAssignmentIdx = lastAssignmentIdx;
            srec.salience = salience;
            srec.fromChaining = isChaining;
            // Log.meta_detail.debug("Context set %s = %s  (was: %s)", key, value, oldVal);
            if (newMatch == null) newMatch = (value != null) ? _meta.match(key, svalue, lastMatch) : lastMatch;
            srec.match = newMatch;
            srec.activation = _currentActivation;
            _currentActivation._recs.add(srec);

            _Assignment rec = newContextRec();
            rec.srec = srec;
            rec.val = value;
            _entries.add(rec);
            _currentProperties = null;

            _values.put(key, value);

            if (_DebugRuleMatches) _checkMatch(srec.match, key, value);
            assertContextConsistent();
            return true; // !matchingPropKeyAssignment;
        } else {
            Log.meta_context.debug("Context skipped assignment of matching property value %s = %s (isChaining == %s, isPropKey == %s)",
                    key, value, Boolean.toString(isChaining), (meta().keyData(key).isPropertyScope()));
            if (!isChaining && meta().keyData(key).isPropertyScope()) {
                // slam down a rec for property context
            }
        }
        return false;
    }

    private void _undoRecValue (_Assignment rec)
    {
        if (rec.srec.lastAssignmentIdx == -1 ||  _entries.get(rec.srec.lastAssignmentIdx).maskedByIdx > 0) {
            _values.remove(rec.srec.key);
        } else {
            _values.put(rec.srec.key, _entries.get(rec.srec.lastAssignmentIdx).val);
        }
    }

    // Undoes and masks assignments invalidated by override of given record
    // Returns stack index for first assignment (i.e. where match recomputation must start)
    int _prepareForOverride (int overrideIndex, int lastAssignmentIdx)
    {
        // if we're overriding a prop context override of a matching value, back up further
        int lastLastIdx;
        while (((lastLastIdx = _entries.get(lastAssignmentIdx).srec.lastAssignmentIdx) != -1)
                && ( _entries.get(lastAssignmentIdx).maskedByIdx <= 0)) {
            // mark it! (we'll pick it up below...)
            _entries.get(lastAssignmentIdx).maskedByIdx = -1;
            lastAssignmentIdx = lastLastIdx;
        }   

        // undo all conflicting or dervied assignments (and mark them)
        for (int i=_entries.size()-1; i >= lastAssignmentIdx; i--) {
            _Assignment r = _entries.get(i);
            // we need to undo (and mask) any record that conflict or are derived
            // NOTE: We are skipping the remove all chained records, because this can result in undoing
            // derived state totally unrelated to this key.  Ideally we'd figure out what depended on what...
            if (r.maskedByIdx <= 0
                    && (i == lastAssignmentIdx || r.maskedByIdx == -1)) { // || r.srec.fromChaining
                // mark and undo it
                r.maskedByIdx = overrideIndex;
                _undoRecValue(r);
            }
        }
        return lastAssignmentIdx;
    }

    private Match.MatchResult _rematchForOverride (String key, Object svalue, int overrideIndex, int firstAssignmentIdx)
    {
        // start from the top down looking for that last unmasked record
        Match.MatchResult lastMatch = null;
        int i=0;
        for (; i<firstAssignmentIdx; i++) {
            _Assignment rec = _entries.get(i);
            if (rec.maskedByIdx != 0) break;
            lastMatch = rec.srec.match;
        }

        Match.UnionMatchResult overridesMatch = null;

        // Rematch skipping over the last assignment of this property
        // and all assignments from chainging
        for (int end=_entries.size(); i < end; i++) {
            _Assignment r = _entries.get(i);
            // rematch on any unmasked records
            if (r.maskedByIdx == 0) {
                lastMatch = _meta.match(r.srec.key, r.srec.val, lastMatch);
            } else {
                // accumulate masked ("_o") match
                overridesMatch = _meta.unionOverrideMatch(r.srec.key, r.srec.val, overridesMatch);
            }
        }

        if (svalue != null || lastMatch == null) {
            lastMatch = _meta.match(key, svalue, lastMatch);
        }

        lastMatch.setOverridesMatch(overridesMatch);
        return lastMatch;
    }

    private void _undoOverride (_Assignment rec, int recIdx)
    {
        int lastAssignmentIdx = rec.srec.lastAssignmentIdx, lastLastIdx;

        // back up further if necessary
        while (((lastLastIdx = _entries.get(lastAssignmentIdx).srec.lastAssignmentIdx) != -1)
                    && (_entries.get(lastLastIdx).maskedByIdx == recIdx)) {
            lastAssignmentIdx = lastLastIdx;
        }

        for (int i=lastAssignmentIdx, c=_entries.size(); i < c; i++) {
            _Assignment r = _entries.get(i);
            if (r.maskedByIdx == recIdx) {
                _values.put(r.srec.key, r.val);
                r.maskedByIdx = 0;
            }
        }
    }
    
    void _checkMatch (Match.MatchResult match, String key, Object value)
    {
        match._checkMatch(_values, _meta);
    }

    int findLastAssignmentOfKey (String key)
    {
        for (int i=_entries.size()-1; i >= 0; i--) {
            _Assignment rec = _entries.get(i);
            if (rec.srec.key.equals(key) && rec.maskedByIdx == 0) return i;
        }
        return -1;
    }

    int findLastAssignmentOfKey (String key, Object value)
    {
        for (int i=_entries.size()-1; i >= 0; i--) {
            _Assignment rec = _entries.get(i);
            if (rec.srec.key.equals(key) && !_isNewValue(rec.val, value)) return i;
        }
        return -1;
    }


    // Check if we have value mirroring (property to context) to do
    // Dynamic property mirroring will be added to the currentActivation deferredAssignment list
    boolean _checkApplyProperties ()
    {
        boolean didSet = false;
        int numEntries;
        int lastSize = 0;
        String declareKey = _inDeclare() ? (String)_values.get(Meta.KeyDeclare) : null;
        while ((numEntries = _entries.size()) > lastSize) {
            lastSize = numEntries;
            _Assignment rec = _entries.get(numEntries-1);
            Meta.PropertyMap properties = rec.srec.properties();
            List<Meta.PropertyManager> contextKeys = properties.contextKeysUpdated();
            if (contextKeys != null) {
                for (int i=0,c=contextKeys.size(); i < c; i++) {
                    Meta.PropertyManager propMgr  = contextKeys.get(i);
                    String key = propMgr._name;
                    if (declareKey != null && key.equals(declareKey)) continue;
                    // ToDo: applying resolved value -- need to defer resolution on true dynamic values
                    // Suppress chained assignment if:
                    //   1) Our parent will assign this property (has a deferred activation for it), or
                    //   2) There's already a matching assignment with higher salience
                    Object newVal = staticallyResolveValue(properties.get(key));                    
                    Meta.PropertyMap prevProps = null;
                    /*
                    if (_frameStarts.size() > 1) {
                        // find the nearest enabled rec from the previous frame
                        for (int prevFrameEnd = _frameStarts.get(_frameStarts.size()-1) - 1; prevFrameEnd >= 0; prevFrameEnd--) {
                            _Assignment prevRec = _entries.get(prevFrameEnd);
                            if (prevRec.maskedByIdx == 0) {
                                prevProps = prevRec.srec.properties();
                                break;
                            }
                        }
                    }
                    */
                    boolean suppress = (prevProps != null && prevProps.containsKey(key)
                            && !_isNewValue(staticallyResolveValue(prevProps.get(key)), newVal))
                            || (_currentActivation._parent.hasDeferredAssignmentForKey(key)
                                /* && _values.containsKey(key) */);
                    if (!suppress) {
                        String mirrorKey = propMgr._keyDataToSet._key;
                        if (newVal instanceof PropertyValue.Dynamic) {
                            Log.meta_detail.debug("(deferred) chaining key: %s", propMgr._keyDataToSet._key);
                            _currentActivation.addDeferredAssignment(mirrorKey, (PropertyValue.Dynamic)newVal);
                        } else {
                            // compare this value to the value from the end of the last frame
                            Log.meta_detail.debug("chaining key: %s", propMgr._keyDataToSet._key);
                            if (_set(mirrorKey, newVal, newVal, false, true)) {
                                didSet |= true;
                            }
                        }
                    } else {
                        Log.meta_detail.debug("SUPPRESSED chaining key: %s", propMgr._keyDataToSet._key);
                    }
                }
            }
        }
        return didSet;
    }

    void applyPropertyContextAndChain ()
    {
        if(_checkPropertyContext()) {
            while (_checkApplyProperties()) /* repeat */;
        }
    }

    String _currentPropertyScopeKey ()
    {
        String foundKey = null;
        _Activation foundActivation = null;
        // search backward for the first scope key
        for (int i = _entries.size() - 1; i >=0; i--) {
            _Assignment rec = _entries.get(i);
            if (foundActivation != null && rec.srec.activation != foundActivation) break;
            if (meta().keyData(rec.srec.key).isPropertyScope()) {
                if (!rec.srec.fromChaining) return rec.srec.key;
                // for chaining assignments, we keep looking until we exhaust the first non-chaining activation
                // Todo: broken -- disabling set of context key from chaining 
                // if (foundKey == null) foundKey = scopeKey;
            }
            if (foundKey != null && !rec.srec.fromChaining) foundActivation = rec.srec.activation;
        }
        return foundKey;
    }

    // Apply a "property context" property (e.g. field_p for field) to the context if necessary
    boolean _checkPropertyContext ()
    {
        Assert.that(_values instanceof NestedMap, "Property assignment on base map?");
        String scopeKey = _currentPropertyScopeKey();
        if (scopeKey != null) {
            return _set(Meta.ScopeKey, scopeKey, scopeKey, false, false);
        }
        return false;
    }

    public void debug ()
    {
        // set debugger breakpoint here
        Log.meta_context.debug("******  Debug Call ******");
        _logContext();
    }

    public String debugString ()
    {
        StringWriter strWriter = new StringWriter();        
        PrintWriter out = new PrintWriter(strWriter);
        out.println("Context:  (" + _entries.size() + " entries)");
        for (int i=0, c=_entries.size(); i<c; i++) {
            int sp = i; while (sp-- > 0) out.print("  ");
            _Assignment r = _entries.get(i);
            out.println(Fmt.S("%s : %s%s%s", r.srec.key, r.srec.val,
                                (r.srec.fromChaining ? " ^" : ""),
                                (r.maskedByIdx != 0 ? " X" : "")));
        }
        _Activation propertyActivation = currentActivation()._propertyActivation;
        if (propertyActivation != null) {
            List<_StaticRec> srecs = propertyActivation._recs;
            out.println("  PropertyActivation...");
            for (int i=0, c=srecs.size(); i<c; i++) {
                int sp = i + _entries.size() + 1; while (sp-- > 0) out.print("  ");
                _StaticRec r = srecs.get(i);
                out.println(Fmt.S("%s : %s%s", r.key, r.val, (r.fromChaining ? "" : " !")));
            }
        }
        out.printf("\nProps:\n");
        OSSWriter.writeProperties(out, allProperties(), 1, false);

        if (Log.meta_context.isEnabledFor(Level.DEBUG)) {
            out.printf("\nValues:\n", values());
            OSSWriter.writeProperties(out, values(), 1, false);

            out.printf("\n\nAssignments:\n");
            _logAssignmentMap(assignmentMap(lastStaticRec()), out);

            Match.MatchResult match = lastMatch();
            if (match != null) out.println("\n" + lastMatch().debugString());
        }
        return strWriter.toString();
    }

    void _logContext ()
    {
        String debugString = debugString();
        Log.meta_context.debug(debugString);
    }

    void _logAssignmentMap (Map<Rule.AssignmentSource, List<AWDebugTrace.Assignment>> assignmentMap, PrintWriter out)
    {
        List<Rule.AssignmentSource> rules = new ArrayList(assignmentMap.keySet());
        Collections.sort(rules, new Comparator<Rule.AssignmentSource>() {
            public int compare (Rule.AssignmentSource r1, Rule.AssignmentSource r2)
            {
                return r1.getRank() - r2.getRank();
            }
        });
        for (Rule.AssignmentSource r : rules) {
            out.printf("  -- %s\n", r.getRule().getSelectors());
            for (AWDebugTrace.Assignment asn : assignmentMap.get(r)) {
                out.printf("      %s: %s %s\n", asn.getKey(), asn.getValue(), asn.isOverridden() ? "  X" : "");
            }
        }
    }

    Match.MatchResult lastMatchWithoutContextProps ()
    {
        return _entries.isEmpty() ? null : _entries.get(_entries.size()-1).srec.match;
    }

    Match.MatchResult lastMatch ()
    {
        if (_entries.isEmpty()) return null;
        Match.MatchResult match = ListUtil.lastElement(_entries).propertyLocalMatches(this);
        return (match != null) ? match : lastMatchWithoutContextProps();
    }

    _StaticRec lastStaticRec ()
    {
        if (_entries.isEmpty()) return null;
        _StaticRec rec = ListUtil.lastElement(_entries).propertyLocalStaticRec(this);
        return (rec != null) ? rec : ListUtil.lastElement(_entries).srec;
    }
/*
    public String toString ()
    {
        return "Context values: " + _values; // + ", props: " + allProperties();
    }
*/
    // Interface for editor calls
    public interface AssignmentRecord
    {
        String key ();
        Object value ();
    }

    /**
        Record for an assignment in the Context stack.
        The static part of the data associated with the assignment
        is factored into the (shared) _ContextRec
     */
    static class _Assignment {
        _StaticRec srec;
        Object val;

        int maskedByIdx;
        boolean _didInitPropContext;
        _StaticRec _propertyLocalSrec;
        Map _propertyLocalValues;

        Match.MatchResult propertyLocalMatches (Context context)
        {
            if (!_didInitPropContext) initPropContext(context);
            return (_propertyLocalSrec != null) ? _propertyLocalSrec.match : null;
        }

        _StaticRec propertyLocalStaticRec (Context context)
        {
            if (!_didInitPropContext) initPropContext(context);
            return _propertyLocalSrec;
        }

        Map propertyLocalValues (Context context)
        {
            if (!_didInitPropContext) initPropContext(context);
            return _propertyLocalValues;
        }

        void initPropContext (Context context)
        {
            _didInitPropContext = true;
            Assert.that(!_ExpensiveContextConsistencyChecksEnabled || ListUtil.lastElement(context._entries) == this, "initing prop context on record not on top of stack");
            // Todo: base it on whether we've tries yet to process them.
            // if(srec.activation.deferredAssignments != null) return;
            // "shouldn't be creating prop context on activation with deferred assignments -- but could happen if deferred assignment yields skipped set (value match)
            _Activation propActivation =  (srec.activation.propertyActivation(context));
            if (propActivation != null) {
                context._applyPropertyActivation(propActivation, this);
            }
        }

        void reset ()
        {
            srec = null;
            val = null;
            maskedByIdx = 0;
            _didInitPropContext = false;
            _propertyLocalSrec = null;
            _propertyLocalValues = null;
        }
    }

    /**
        The "static" (sharable) part of a context value assignment record.
        Theses are created by the first _Assignment that needs them
        and then cached for re-application in their _Activation
        (which, in turn, is stored in the global activation tree)
     */
    static class _StaticRec implements AssignmentRecord {
        _Activation activation;
        String key;
        Object val;
        Match.MatchResult match;
        int salience;
        boolean fromChaining;
        int lastAssignmentIdx;

        Meta.PropertyMap properties ()
        {
            return (match != null) ? match.properties() : EmptyMap;
        }

        public String key ()
        {
            return key;
        }

        public Object value ()
        {
            return val;
        }
    }

    /**
        A sharable/re-applicable block of Assignment _StaticRecs.  An Activation contains
        the list of assignment records resulting from (chaining from) a single original
        assignment (as well as _DeferredAssignment records for dynamic values that cannot
        be statically resolved to records).  Activations form a shared/cached tree, based
        on context assignment paths previously traversed via assignments to some Context.
        Subsequent traversals of these paths (likely by different Context instances)
        are greatly optimized: an existing Activation is retrieved and its records appended
        to the context's _entries stack; all of the traditional computation of rule match lookups,
        chained assignments and override indexes is bypassed.

        _Activation gives special treatment to the "propertyActivation", i.e. the activation
        resulting from the application of the "scopeKey" to the current context.  Property lookup
        following and context assignment require application of the scopeKey, but then the scope key
        must immediately be popped for the next context assignment.  To avoid this constant push/pop
        on the bottom of the stack, _Activations cache a side activation (the propertyActivation)
        for the result of applying the scopeKey to the current activation.  This stack (and its properties)
        are cached on the side, and can be accessed without actually modifying the main context stack.
     */
    static class _Activation
    {
        _Activation _parent;
        List<_StaticRec> _recs = new ArrayList();
        int _origEntryCount;
        GrowOnlyHashtable<String, GrowOnlyHashtable> _valueNodeMapByContextKey;
        GrowOnlyHashtable<String, GrowOnlyHashtable> _valueNodeMapByContextKeyChaining;
        _Activation _propertyActivation;
        NestedMap _nestedValues;
        List <_DeferredAssignment> deferredAssignments;

        _Activation(_Activation parent) { _parent = parent; }

        _Activation getChildActivation (String contextKey, Object value, boolean chaining)
        {
            if (value == null) value = Meta.NullMarker;
            Map<String, GrowOnlyHashtable> byKey = (chaining) ? _valueNodeMapByContextKeyChaining : _valueNodeMapByContextKey;
            if (byKey == null) return null;
            GrowOnlyHashtable byVal = byKey.get(contextKey);
            return (byVal == null) ? null : (_Activation)byVal.get(value);
        }

        void cacheChildActivation (String contextKey, Object value, _Activation activation, boolean chaining)
        {
            if (value == null) value = Meta.NullMarker;
            GrowOnlyHashtable<String, GrowOnlyHashtable> byKey;
            // Thread safety:  Okay to lose an activation?  (e.g. write it to an lost map)
            if (chaining) {
                if ((byKey = _valueNodeMapByContextKeyChaining) == null)
                    byKey = _valueNodeMapByContextKeyChaining = new GrowOnlyHashtable();
            } else {
                if ((byKey = _valueNodeMapByContextKey) == null)
                    byKey = _valueNodeMapByContextKey = new GrowOnlyHashtable();
            }

            GrowOnlyHashtable byVal = byKey.get(contextKey);
            if (byVal == null) {
                byVal = new GrowOnlyHashtable();
                byKey.put(contextKey, byVal);
            }
            byVal.put(value, activation);
        }

        void addDeferredAssignment (String key, PropertyValue.Dynamic value) {
            _DeferredAssignment newDa = null;
            if (deferredAssignments == null) {
                deferredAssignments = new ArrayList();
            } else {
                for (_DeferredAssignment da : deferredAssignments) {
                    if (da.key.equals(key)) {
                        newDa = da;
                        break;
                    }
                }
            }
            if (newDa == null) {
                newDa = new _DeferredAssignment();
                newDa.key = key;
                deferredAssignments.add(newDa);
            }
            newDa.value = value;
        }

        boolean hasDeferredAssignmentForKey (String key)
        {
            if (deferredAssignments != null) {
                for (_DeferredAssignment da : deferredAssignments) {
                    if (da.key.equals(key)) return true;
                }
            }
            return false;
        }

        _Activation propertyActivation (Context context)
        {
            Assert.that(context.currentActivation() == this, "PropertyActivation sought on non top of stack activation");
            if (_propertyActivation == null) {
                synchronized(this) {
                    if (_propertyActivation == null) {
                        _propertyActivation = context._createNewPropertyContextActivation(this);
                        if (_propertyActivation == null) _propertyActivation = this; // this as null marker
                    }
                }
            }

            return _propertyActivation != this ? _propertyActivation : null;
        }

        _Activation findExistingPropertyActivation ()
        {
            _Activation activation = this;
            while (activation != null) {
                _Activation propertyActivation = activation._propertyActivation;
                if (propertyActivation != null && propertyActivation != activation
                        && !ListUtil.nullOrEmptyList(propertyActivation._recs)) {
                    return propertyActivation;
                }
                activation = activation._parent;
            }
            return null;
        }
    }

    static class _DeferredAssignment {
        String key;
        PropertyValue.Dynamic value;
    }


    /**
        PropertyAccessor provides a handle for FieldValue lookups on the Context properties
        that should lookup and resolve property values.  Each Context instance holds a single PropertyAccessor
        instance and returns it from its properties() methods.  This enables FieldPath lookups of the form
        "$metaContext.properties.visible" to result is the lookup and dynamic resolution of the "visible"
        property value in the Context.
     */
    // self reference thunk for FieldValue
    public class PropertyAccessor {
        Object get (String key) { return propertyForKey(key); }
        public String toString () { return allProperties().toString(); }
    }

    public static Object newInnerInstance(Class innerClass, Object outerInstance)
    {
        try {
            // Wacky way to dynamically instantiate an inner class
            return innerClass.getConstructor(new Class[]{outerInstance.getClass()}).newInstance(new Object[]{outerInstance});
        } catch (Exception e) {
            throw new AWGenericException(e);
        }
    }

    public final static class FieldValue_ContextPropertyAccessor extends FieldValue_Object
    {
        public void setFieldValuePrimitive (Object target, FieldPath fieldPath, Object value)
        {
            Assert.that(false, "Assignment to Context properties not allowed");
        }

        public Object getFieldValuePrimitive (Object target, FieldPath fieldPath)
        {
            return ((PropertyAccessor)target).get(fieldPath.car());
        }
    }

    public static Object elementOrFirst (Object listOrElement)
    {
        return (listOrElement instanceof List)
                ? ListUtil.firstElement((List)listOrElement)
                : listOrElement;
    }

    /**
        An externally intelligible snapshot of current Context state for use
        by Inspectors / Editors.
     */
    public static class InspectorInfo
    {
        public Map<String, Object> contextMap = new HashMap();
        public List<String> contextKeys = ListUtil.list();
        public Map properties;
        public String scopeKey;
        public boolean scopeKeyFromChaining;
        public List <AssignmentInfo> assignmentStack;

        public InspectorInfo() {}

        // Copy, overriding scope key value
        public InspectorInfo(InspectorInfo orig, Object overrideScopeValue)
        {
            contextKeys = ListUtil.cloneList(contextKeys);
            contextMap = MapUtil.cloneMap(orig.contextMap);
            properties = MapUtil.cloneMap(orig.properties);  // BOGUS
            scopeKey = orig.scopeKey;
            scopeKeyFromChaining = orig.scopeKeyFromChaining;

            contextMap.put(scopeKey,  overrideScopeValue);
        }

        public Object getValue(String key)
        {
            return contextMap.get(key);
        }

        public Object getSingleValue(String key)
        {
            return elementOrFirst(contextMap.get(key));
        }

    }

    /**
        An externally intelligible snapshot of an assignment in a InspectorInfo Context snapshot
        for use by Inspectors / Editors.
     */
    public static class AssignmentInfo
    {
        public String key;
        public Object value;
        public int level;
        public boolean overridden;
        public boolean fromChaining;
        public boolean scopeKeyAssignment;
        public AssignmentRecord rec;
    }

    public static Map<Rule.AssignmentSource, List<AWDebugTrace.Assignment>> assignmentMap (AssignmentRecord arec)
    {
        AWDebugTrace.AssignmentRecorder recorder = new AWDebugTrace.AssignmentRecorder();
        Match.MatchResult match = ((_StaticRec)arec).match;
        match._meta.propertiesForMatch(match, recorder);
        return (Map<Rule.AssignmentSource, List<AWDebugTrace.Assignment>>)((Map)recorder.getAssignments());
    }

    public static InspectorInfo staticContext (Meta meta, AssignmentRecord arec)
    {
        return staticContext(meta, arec, false);
    }

    public static InspectorInfo staticContext (Meta meta, AssignmentRecord arec, boolean includeSetInfo)
    {
        InspectorInfo result = new InspectorInfo();
        result.properties = ((_StaticRec)arec).properties();
        _Activation activation = ((_StaticRec)arec).activation;
        int level = 0;
        if (includeSetInfo) {
            result.assignmentStack = ListUtil.list();
            for (_Activation aP = activation; aP._parent != null; aP = aP._parent) { level++; }
        }

        addStaticContextForActivation(meta, activation, result, level);

        Collections.reverse(result.contextKeys);

        if (includeSetInfo) {
            for (AssignmentInfo si : result.assignmentStack) {
                if (result.scopeKey != null && result.scopeKey.equals(si.key)
                            && result.contextMap.get(si.key).equals(si.value)) {
                    si.scopeKeyAssignment = true;
                    break;
                }
            }
            Collections.reverse(result.assignmentStack);
        }

        return result;
    }

    private static void addStaticContextForActivation (Meta meta, _Activation activation, InspectorInfo info, int level)
    {
        List<_StaticRec> recs = activation._recs;
        for (int i=recs.size()-1; i >= 0; i--) {
            _StaticRec srec = recs.get(i);
            if (!Meta.isPropertyScopeKey(srec.key)) {
                boolean overridden = true;
                if (!info.contextMap.containsKey(srec.key)) {
                    overridden = false;
                    info.contextKeys.add(srec.key);
                    info.contextMap.put(srec.key, srec.val);
                    if (info.scopeKey == null || (info.scopeKeyFromChaining && !srec.fromChaining)) {
                        if (meta.keyData(srec.key).isPropertyScope()) {
                            info.scopeKey = srec.key;
                            info.scopeKeyFromChaining = srec.fromChaining;
                        }
                    }
                }

                if (info.assignmentStack != null) {
                    AssignmentInfo assignmentInfo = new AssignmentInfo();
                    assignmentInfo.key = srec.key;
                    assignmentInfo.value = srec.val;
                    assignmentInfo.overridden = overridden;
                    assignmentInfo.fromChaining = srec.fromChaining;
                    assignmentInfo.level = level;
                    assignmentInfo.rec = srec;
                    info.assignmentStack.add(assignmentInfo);
                }
            }
        }
        if (activation._parent != null) addStaticContextForActivation(meta, activation._parent, info, --level);
    }

    /**
        Support for Component Inspector (AWDebugTrace.MetaProvider).
     */
    protected static class MetaProvider_Context extends AWDebugTrace.MetaProvider
    {
        public String title (Object receiver, String title, AWBindableElement element)
        {
            return (element instanceof MetaContext)
                    ? contextTitle((_StaticRec)receiver)
                    : usageTitle((_StaticRec)receiver);
        }

        public Map metaProperties (Object receiver, AWBindableElement element)
        {
            _StaticRec srec = (_StaticRec)receiver;
            return (element instanceof MetaContext)
                    ? metaContext(srec)
                    : srec.properties();
        }

        public void recordAssignments (Object receiver, AWDebugTrace.AssignmentRecorder recorder,
                                       AWBindableElement element)
        {
            Match.MatchResult match = ((_StaticRec)receiver).match;
            match._meta.propertiesForMatch(match, recorder);
        }

        Map metaContext (_StaticRec srec)
        {
            Map result = new LinkedHashMap();
            _Activation activation = srec.activation;
            addContextForActivation(activation, result);
            return result;
        }

        private void addContextForActivation (_Activation activation, Map map)
        {
            if (activation._parent != null) addContextForActivation(activation._parent, map);
            List<_StaticRec> recs = activation._recs;
            StringBuffer buf = new StringBuffer();
            for (_StaticRec srec : recs) {
                int idx = map.size();
                int sp = idx; while (sp-- > 0) buf.append(". ");
                String key = Fmt.S("%s%s", buf.toString(), srec.key);
                Object val = Fmt.S("%s%s", srec.val, (srec.fromChaining ? " ^" : ""));
                map.put(key, val);
                buf.setLength(0);
            }
        }

        String contextTitle (_StaticRec focusRec)
        {
            // return list of assignments made in this activation (but not propert activation)
            StringBuffer buf = new StringBuffer();
            _Activation activation = findPropertyActivation(focusRec);
            activation = (activation != null) ? activation._parent : focusRec.activation;
            for (_StaticRec srec : activation._recs) {
                if (buf.length() != 0) buf.append(", ");
                buf.append(Fmt.S("%s=%s", srec.key, srec.val));
            }

            return buf.toString();
        }

        String usageTitle (_StaticRec srec)
        {
            // return value of property context key
            _Activation propertyActivation = findPropertyActivation(srec);
            String propertyScopeKey = (propertyActivation != null)
                    ? propertyActivation._recs.get(0).key : null;
            Assert.that(propertyScopeKey != null && Meta.isPropertyScopeKey(propertyScopeKey),
                    "Did not find propertyScopeKey where expected: %s", propertyScopeKey);
            String propertyKey = (String)propertyActivation._recs.get(0).val;

            _StaticRec assignment = firstRecWithKey(propertyActivation, propertyKey);
            Assert.that(assignment != null, "Unable to find assignment of key: %s", propertyKey);

            return Fmt.S("%s=%s", assignment.key, assignment.val);
        }

        _Activation findPropertyActivation (_StaticRec srec)
        {
            _Activation activation = srec.activation;
            return activation != null ? activation.findExistingPropertyActivation() : null;
        }

        _StaticRec firstRecWithKey (_Activation activation, String key)
        {
            while (activation != null) {
                // iterating forward is okay -- won't assign same key within single activation
                for (_StaticRec srec : activation._recs) {
                    if (srec.key.equals(key)) return srec;
                }
                activation = activation._parent;
            }
            return null;
        }

        public String inspectorComponentName (Object receiver, AWBindableElement element)
        {
            return "MetaInspectorPane";
        }
    }

    private Map resolvedProperties ()
    {
        Map<String, Object> source = allProperties();
        Map<String, Object> result = source;

        for (String key : source.keySet()) {
            Object val = source.get(key);
            Object resolvedVal = resolveValue(val);
            if (resolvedVal != val) {
                if (source == result) {
                    result = new HashMap(source);
                }
                result.put(key, resolvedVal);
            }
        }
        return result;
    }
    
    static {
        AWDebugTrace.MetaProvider.registerClassExtension(_StaticRec.class, new MetaProvider_Context());
    }

    public Object debugTracePropertyProvider ()
    {
        return lastStaticRec();
    }

    /*
        Context records are considered to match if they're scopeKeys match and
        have matching values
     */
    public boolean currentRecordPathMatches (AssignmentRecord rec)
    {
        // horribly inefficient implementation -- maybe okay because only used in dev editor
        return currentRecordPathMatches(staticContext(_meta, rec));
    }

    public boolean currentRecordPathMatches (InspectorInfo other)
    {
        // horribly inefficient implementation -- maybe okay because only used in dev editor
        InspectorInfo current = staticContext(_meta, lastStaticRec());

        for (String key : current.contextKeys) {
            if (_meta.keyData(key).isPropertyScope()) {
                if (!current.contextMap.get(key).equals(other.contextMap.get(key))) return false;
            }
        }
        return true;
    }
}
