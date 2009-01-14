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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Context.java#8 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Constants;
import ariba.util.expr.AribaExprEvaluator;
import ariba.util.fieldvalue.Expression;
import ariba.util.fieldvalue.Extensible;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValue_Object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.log4j.Level;

/**
    Context represents a stack of assignments (e.g. class=User, field=birthDay, operation=edit)
    The current set of assignments can be retrieved via values().

    The current values are run against the Meta rule set to compute the effective PropertyMap
    (e.g. visible:true, editable:true, component:AWTextField).
    Some rule evaluations result in *chaining* -- where additional assignments that are
    "implied" by the current assignments are applied, (resulting in a revided computation
    of the current PropertyMap, and possible further chainging).
    (e.g. field=birthDay may result in type=Date which may result in component:DatePicker)

    Assignments can be scoped and popped (push(), pop()).

    The actual computation of rule matches is cached so once a "path" down the context
    tree has been exercised subsequent matching traversals (even by other threads/users)
    is fast.    
 */
public class Context implements Extensible
{
    protected static boolean _DebugRuleMatches = false;
    
    Meta _meta;
    Map<String, Object> _values = new HashMap();
    List <_ContextRec> _entries = new ArrayList();
    List <Integer> _frameStarts = new ArrayList();
    PropertyAccessor _accessor = new PropertyAccessor();
    Meta.PropertyMap _currentProperties;
    Activation _rootNode;
    Activation _currentActivation;
    List <_ContextRec> _recPool = new ArrayList();
    public static final Meta.PropertyMap EmptyMap = new Meta.PropertyMap();
    private static boolean _CacheActivations = true;

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
        Assert.that(_frameStarts.size() > 0, "Popping empty stack");
        int pos = ListUtil.removeLastElement(_frameStarts);
        while (_entries.size() > pos) {
            _ContextRec rec = ListUtil.removeLastElement(_entries);
            int recIdx = _entries.size();
            if (rec.srec.lastAssignmentIdx == -1) {
                _values.remove(rec.srec.key);
            } else {
                _undoOverride(rec, recIdx);
            }

            // check rec back into pool for reuse
            rec.reset();
            _recPool.add(rec);
            
            _currentActivation = (_entries.size() > 0)
                    ? ListUtil.lastElement(_entries).srec.activation
                    : _rootNode;
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

    public void setContextKey (String key)
    {
        String requested = meta().keyData(key).propertyScopeKey();
        Assert.that(requested != null, "%s is not a valid context key", key);
        String current = _currentPropertyScopeKey();
        // Assert.that(current != null, "Can't set %s as context key when no context key on stack", key);

        if (!requested.equals(current)) {
            Object val = values().get(key);
            Assert.that(val != null, "Can't set %s as context key when it has no value already on the context", key);
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
                : ((Boolean)val).booleanValue();
    }

    public Meta.PropertyMap allProperties ()
    {
        if (_currentProperties == null) {
            Meta.MatchResult m = lastMatch();
            if (m != null) {
                _currentProperties = m.properties();
            }
        }
        return (_currentProperties != null) ? _currentProperties : EmptyMap;
    }

    public Object resolveValue (Object value)
    {
        return (value != null && value instanceof DynamicPropertyValue)
                ? ((DynamicPropertyValue)value).evaluate(this)
                : value;
    }

    public Object staticallyResolveValue (Object value)
    {
        Object lastValue = null;
        while (value != lastValue && value != null && value instanceof StaticallyResolvable) {
            lastValue = value;
             value = ((DynamicPropertyValue)value).evaluate(this);
        }
        return value;
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

    public static class Snapshot
    {
        Meta _meta;
        Class _origClass;
        List <_Assignment> _assignments;

        public Context hydrate ()
        {
            Context context = (Context)newInnerInstance(_origClass, _meta);
            for (_Assignment a: _assignments) {
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

    List<_Assignment> _activeAssignments ()
    {
        List<_Assignment> list = new ArrayList();
        for (int i=0, c=_entries.size(); i<c; i++) {
            _ContextRec rec = _entries.get(i);
            if (rec.maskedByIdx == 0 && !rec.srec.fromChaining) {
                _Assignment a = new _Assignment();
                a.key = rec.srec.key;
                a.value = rec.val;
                list.add(a);
            }
        }
        return list;
    }

    static class _Assignment {String key; Object value; }

    // a (usable) snapshot of the current state of the context
    public Snapshot snapshot()
    {
        return new Snapshot(this);
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
    static Activation getActivationTree (Meta meta)
    {
        Activation root = (Activation)meta.identityCache().get(Activation.class);
        if (root == null) {
            root = new Activation(null);
            meta.identityCache().put(Activation.class, root);
        }
        return root;
    }

    Activation currentActivation ()
    {
        return _currentActivation;
    }

    protected void _set (String key, Object value, boolean merge, boolean chaining)
    {
        Object sval = _meta.transformValue(key, value);
        Activation parentActivation = currentActivation();
        Activation activation = parentActivation.get(key, sval, chaining);
        boolean didSet;
        if (activation == null) {
            Log.meta_detail.debug("Creating new activation for %s: %s", key, value);
            didSet = _applyNewFrameForSet(key, sval, value, merge, chaining);
            if (didSet) {
                awakeCurrentActivation();
            }
            if (didSet && Log.meta_detail.isEnabledFor(Level.DEBUG)) _logContext(System.out);
        }
        else {
            Log.meta_context.debug("Found existing activation for %s: %s", key, value);
            didSet = _applyActivation(activation, value);
            if (didSet) {
                awakeCurrentActivation();
            }
            if (Log.meta_context.isEnabledFor(Level.DEBUG)) _logContext(System.out);
        }
    }

    _ContextRec newContextRec ()
    {
        int count = _recPool.size();
        return (count > 0) ? _recPool.remove(count - 1) : new _ContextRec();
    }

    boolean _applyActivation (Activation activation, Object firstVal)
    {
        Assert.that(activation._parent == _currentActivation, "Attempt to apply activation on mismatched parent");
        Assert.that(_entries.size() == activation._origEntryCount,
                "Mismatched context stack size (%s) from when activation was popped (%s)",
                Integer.toString(_entries.size()),
                Integer.toString(activation._origEntryCount));
        int count=activation._recs.size();
        if (count == 0) return false;
        for (int i=0; i<count; i++) {
            _StaticRec srec = activation._recs.get(i);
            _ContextRec rec = newContextRec();
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
            Object resolvedValue = resolveValue(da.value);
            Log.meta_context.debug("_set applying deferred assignment of derived value: %s <- %s (%s)",
                    da.key, resolvedValue, da.value);
            _set(da.key, resolvedValue, false, true);
        }
    }

    private void _initPropertyActivation (Activation propActivation, _ContextRec rec)
    {
        if (propActivation._nestedValues != null) {
            propActivation._nestedValues.reparent(_values);
        }

        // set up propLocal results
        // Now, see if we need to compute a dynamic property activation as well
        if (propActivation.deferredAssignments != null) {
            push();
            // nest a dynamic nested map on our static nested map (which is on our last dynamic nested map...)
            Map origValues = _values;
            _values = new NestedMap(propActivation._nestedValues);
            _applyActivation(propActivation, Meta.NullMarker);
            applyDeferredAssignments(propActivation.deferredAssignments);
            rec._propertyLocalValues = _values;
            rec._propertyLocalMatches = ListUtil.lastElement(_entries).srec.match;
            _values = new HashMap();  // hack -- empty map so that undo is noop
            pop();
            _values = origValues;
        }
        else {
            // can use static versions
            rec._propertyLocalValues = propActivation._nestedValues;
            rec._propertyLocalMatches = ListUtil.lastElement(propActivation._recs).match;
        }
    }

    boolean _inDeclare ()
    {
        return (lastMatch()._keysMatchedMask & _meta.declareKeyMask()) != 0;
    }

    /*
        Non-cached access
     */
    boolean _applyNewFrameForSet (String key, Object svalue, Object value, boolean merge, boolean chaining)
    {
        Activation lastActivation = _currentActivation;
        Activation newActivation = new Activation(lastActivation);
        newActivation._origEntryCount = _entries.size();
        _currentActivation = newActivation;

        // set this value
        boolean didSet =_set(key, svalue, value, merge, chaining);
        // mirror properties
        if (didSet) while (_checkApplyProperties()) /* repeat */;

        // Remember for the future
        if (_CacheActivations) lastActivation.put(key, svalue, newActivation, chaining);

        _currentActivation = (didSet) ? newActivation : lastActivation;

        return _currentActivation != lastActivation;
    }

    private Activation applyNewPropertyContextActivation (Activation parentActivation)
    {
        // Compute the static part of the property activation
        // we accumulate the property settings on a side activation off the main stack
        // and apply it virtually if our parent is not covered
        // (that way we don't have to apply and unapply all the time)
        push();
        Activation propActivation = new Activation(parentActivation);
        propActivation._origEntryCount = _entries.size();

        _currentActivation = propActivation;
        Map origValues = _values;
        NestedMap nestedMap = new NestedMap(origValues);
        _values = nestedMap;
        applyPropertyContextAndChain();
        if (propActivation._recs.size() > 0 || propActivation.deferredAssignments != null) {
            propActivation._nestedValues = nestedMap;
            _values = new HashMap();  // hack -- empty map so that undo is noop
        } else {
            propActivation = null;
        }
        pop();
        _values = origValues;
        _currentActivation = parentActivation;

        return propActivation;
    }

    protected boolean _isNewValue (Object oldVal, Object newVal)
    {
        return (oldVal != newVal && (oldVal == null ||
                (!oldVal.equals(newVal)
                        && (!(oldVal instanceof List) || !((List)oldVal).contains(newVal)))));
    }

    protected boolean _set (String key, Object svalue, Object value, boolean merge, boolean isChaining)
    {
        boolean hasOldValue = _values.containsKey(key);
        Object oldVal = hasOldValue ? _values.get(key) : null;
        boolean isNewValue = !hasOldValue || _isNewValue(oldVal, value);
        String scopeKey;
        boolean matchingPropKeyAssignment = !isNewValue && !isChaining
                && (((scopeKey = meta().keyData(key).propertyScopeKey()) != null)
                        && !scopeKey.equals(_currentPropertyScopeKey()));
        if (isNewValue || matchingPropKeyAssignment) {
            Meta.MatchResult lastMatch;
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
                Assert.that(lastAssignmentIdx >=0, "Value in context but no assignment record found");

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
                    _ContextRec oldRec = _entries.get(lastAssignmentIdx);
                    if (isChaining && (oldRec.srec.salience >= salience || !oldRec.srec.fromChaining)) {
                        Log.meta_detail.debug("Set of key '%s' -> '%s' skipped -- salience %s <= %s)",
                            key, value, salience, oldRec.srec.salience);
                        return false;
                    }
                    int firstAssignmentIdx = _prepareForOverride(recIdx, lastAssignmentIdx);
                    lastMatch = _rematchForOverride(recIdx, firstAssignmentIdx);

                    if (merge) value = Meta.PropertyMerger_List.merge(oldVal, value);
                }
                // Todo: this isn't quite right -- a chaining assignment from a *newer frame* should
            }

            _StaticRec srec = new _StaticRec();
            srec.key = key;
            // todo: conversion
            srec.val = svalue;
            srec.lastAssignmentIdx = lastAssignmentIdx;
            srec.salience = salience;
            srec.fromChaining = isChaining;
            // Log.meta_detail.debug("Context set %s = %s  (was: %s)", key, value, oldVal);
            srec.match = (value != null) ? _meta.match(key, svalue, lastMatch) : lastMatch;
            srec.activation = _currentActivation;
            _currentActivation._recs.add(srec);

            _ContextRec rec = newContextRec();
            rec.srec = srec;
            rec.val = value;
            _entries.add(rec);
            _currentProperties = null;

            _values.put(key, value);

            if (_DebugRuleMatches) _checkMatch(srec.match, key, value);
            return true; // !matchingPropKeyAssignment;
        } else {
            Log.meta_context.debug("Context skipped assignment of matching property value %s = %s (isChaining == %s, isPropKey == %s)",
                    key, value, Boolean.toString(isChaining), (meta().keyData(key).propertyScopeKey() != null));
            if (!isChaining && meta().keyData(key).propertyScopeKey() != null) {
                // slam down a rec for property context
            }
        }
        return false;
    }

    private void _undoRecValue (_ContextRec rec)
    {
        if (rec.srec.lastAssignmentIdx == -1) {
            _values.remove(rec.srec.key);
        } else {
            _values.put(rec.srec.key, _entries.get(rec.srec.lastAssignmentIdx).val);
        }
    }

    // Undoes and masks assignments invalidated by override of given record
    // Returns stack index for first asignment (i.e. where match recomputation must start)
    int _prepareForOverride (int overrideIndex, int lastAssignmentIdx)
    {
        // if we're overriding a prop context override of a matching value, back up further
        int lastLastIdx;
        while ((lastLastIdx = _entries.get(lastAssignmentIdx).srec.lastAssignmentIdx) != -1)  {
            // mark it! (we'll pick it up below...
            _entries.get(lastAssignmentIdx).maskedByIdx = -1;
            lastAssignmentIdx = lastLastIdx;
        }

        // undo all conflicting or dervied assignments (and mark them)
        for (int i=_entries.size()-1; i >= lastAssignmentIdx; i--) {
            _ContextRec r = _entries.get(i);
            // we need to undo (and mask) any record that conflict or are derived
            if (r.maskedByIdx <= 0
                    && (i == lastAssignmentIdx || r.maskedByIdx == -1 || r.srec.fromChaining)) {
                // mark and undo it
                r.maskedByIdx = overrideIndex;
                _undoRecValue(r);
            }
        }
        return lastAssignmentIdx;
    }

    private Meta.MatchResult _rematchForOverride (int overrideIndex, int firstAssignmentIdx)
    {
        // start from the top down looking for that last unmasked record
        Meta.MatchResult lastMatch = null;
        int i=0;
        for (; i<firstAssignmentIdx; i++) {
            _ContextRec rec = _entries.get(i);
            if (rec.maskedByIdx != 0) break;
            lastMatch = rec.srec.match;
        }

        // Rematch skipping over the last assignment of this property
        // and all assignments from chainging
        for (int end=_entries.size(); i < end; i++) {
            _ContextRec r = _entries.get(i);
            // rematch on any unmasked records
            if (r.maskedByIdx == 0) {
                lastMatch = _meta.match(r.srec.key, r.srec.val, lastMatch);
            }
        }
        return lastMatch;
    }

    private void _undoOverride (_ContextRec rec, int recIdx)
    {
        int lastAssignmentIdx = rec.srec.lastAssignmentIdx, lastLastIdx;

        // back up further if necessary
        while (((lastLastIdx = _entries.get(lastAssignmentIdx).srec.lastAssignmentIdx) != -1)
                    && (_entries.get(lastLastIdx).maskedByIdx == recIdx)) {
            lastAssignmentIdx = lastLastIdx;
        }

        for (int i=lastAssignmentIdx, c=_entries.size(); i < c; i++) {
            _ContextRec r = _entries.get(i);
            if (r.maskedByIdx == recIdx) {
                _values.put(r.srec.key, r.val);
                r.maskedByIdx = 0;
            }
        }
    }
    
    void _checkMatch (Meta.MatchResult match, String key, Object value)
    {
        _meta._checkMatch(match, _values);
    }

    int findLastAssignmentOfKey (String key)
    {
        for (int i=_entries.size()-1; i >= 0; i--) {
            _ContextRec rec = _entries.get(i);
            if (rec.srec.key.equals(key) && rec.maskedByIdx == 0) return i;
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
        while ((numEntries = _entries.size()) > lastSize) {
            lastSize = numEntries;
            _ContextRec rec = _entries.get(numEntries-1);
            Meta.PropertyMap properties = rec.srec.properties();
            List<Meta.PropertyManager> contextKeys = properties.contextKeysUpdated();
            if (contextKeys != null) {
                for (int i=0,c=contextKeys.size(); i < c; i++) {
                    Meta.PropertyManager propMgr  = contextKeys.get(i);
                    String key = propMgr._name;
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
                            _ContextRec prevRec = _entries.get(prevFrameEnd);
                            if (prevRec.maskedByIdx == 0) {
                                prevProps = prevRec.srec.properties();
                                break;
                            }
                        }
                    }

                    boolean suppress = (prevProps != null && prevProps.containsKey(key)
                            && !_isNewValue(staticallyResolveValue(prevProps.get(key)), newVal))
                            || _currentActivation._parent.hasDeferredAssignmentForKey(key);
                    */
                    boolean suppress = (prevProps != null && prevProps.containsKey(key)
                            && !_isNewValue(staticallyResolveValue(prevProps.get(key)), newVal))
                            || (_currentActivation._parent.hasDeferredAssignmentForKey(key)
                                /* && _values.containsKey(key) */);
                    if (!suppress) {
                        if (newVal instanceof DynamicPropertyValue) {
                            Log.meta_detail.debug("(deferred) chaining key: %s", propMgr._keyDataToSet._key);
                            _currentActivation.addDeferredAssignment(propMgr._keyDataToSet._key, (DynamicPropertyValue)newVal);
                        } else {
                            // compare this value to the value from the end of the last frame
                            Log.meta_detail.debug("chaining key: %s", propMgr._keyDataToSet._key);
                            didSet |= _set(propMgr._keyDataToSet._key, newVal, newVal, false, true);
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
        Activation foundActivation = null;
        // search backward for the first scope key
        for (int i = _entries.size() - 1; i >=0; i--) {
            _ContextRec rec = _entries.get(i);
            if (foundActivation != null && rec.srec.activation != foundActivation) break;
            String scopeKey = meta().keyData(rec.srec.key).propertyScopeKey();
            if (scopeKey != null) {
                if (!rec.srec.fromChaining) return scopeKey;
                // for chaining assignments, we keep looking until we exhaust the first non-chaining activation
                // Todo: broken -- disabling set of context key from chainging 
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
            _set(scopeKey, true, true, false, false);
            return true;
        }
        return false;
    }

    public void debug ()
    {
        // set debugger breakpoint here
        System.out.println("******  Debug Call ******");
        _logContext(System.out);
        // System.out.println("Resolved Properties: " + resolvedProperties());
    }

    public String debugString ()
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        _logContext(new PrintStream(os));
        return os.toString();
    }

    void _logContext (PrintStream out)
    {
        out.println("Context:  (" + _entries.size() + " entries)");
        for (int i=0, c=_entries.size(); i<c; i++) {
            int sp = i; while (sp-- > 0) out.print("  ");
            _ContextRec r = _entries.get(i);
            out.println(Fmt.S("%s : %s%s%s", r.srec.key, r.srec.val,
                                (r.srec.fromChaining ? " ^" : ""),
                                (r.maskedByIdx != 0 ? " X" : "")));
        }
        Activation propertyActivation = currentActivation()._propertyActivation;
        if (propertyActivation != null) {
            List<_StaticRec> srecs = propertyActivation._recs;
            out.println("  PropertyActivation...");
            for (int i=0, c=srecs.size(); i<c; i++) {
                int sp = i + _entries.size() + 1; while (sp-- > 0) out.print("  ");
                _StaticRec r = srecs.get(i);
                out.println(Fmt.S("%s : %s%s", r.key, r.val, (r.fromChaining ? "" : " !")));
            }
        }
        Meta.MatchResult match = lastMatch();
        if (match != null) out.println("  " + lastMatch().debugString());
        out.println("  Props:  " + allProperties());
        out.println("  Values:  " + values());
    }

    Meta.MatchResult lastMatchWithoutContextProps ()
    {
        return _entries.isEmpty() ? null : _entries.get(_entries.size()-1).srec.match;
    }

    Meta.MatchResult lastMatch ()
    {
        if (_entries.isEmpty()) return null;
        Meta.MatchResult match = ListUtil.lastElement(_entries).propertyLocalMatches(this);
        return (match != null) ? match : lastMatchWithoutContextProps();
    }

/*
    public String toString ()
    {
        return "Context values: " + _values; // + ", props: " + allProperties();
    }
*/
    static class _StaticRec {
        Activation activation;
        String key;
        Object val;
        Meta.MatchResult match;
        int salience;
        boolean fromChaining;
        int lastAssignmentIdx;

        Meta.PropertyMap properties ()
        {
            return (match != null) ? match.properties() : EmptyMap;
        }
    }

    static class _ContextRec {
        _StaticRec srec;
        Object val;

        int maskedByIdx;
        boolean _didInitPropContext;
        Meta.MatchResult _propertyLocalMatches;
        Map _propertyLocalValues;

        Meta.MatchResult propertyLocalMatches (Context context)
        {
            if (!_didInitPropContext) initPropContext(context);
            return _propertyLocalMatches;
        }

        Map propertyLocalValues (Context context)
        {
            if (!_didInitPropContext) initPropContext(context);
            return _propertyLocalValues;
        }

        void initPropContext (Context context)
        {
            _didInitPropContext = true;
            Assert.that(ListUtil.lastElement(context._entries) == this, "initing prop context on record not on top of stack");
            // Todo: base it on whether we've tries yet to process them.
            // if(srec.activation.deferredAssignments != null) return;
            // "shouldn't be creating prop context on activation with deferred assignments -- but could happen if deferred assignment yields skipped set (value match)
            Activation propActivation =  (srec.activation.propertyActivation(context));
            if (propActivation != null) {
                context._initPropertyActivation(propActivation, this);
            }
        }

        void reset ()
        {
            srec = null;
            val = null;
            maskedByIdx = 0;
            _didInitPropContext = false;
            _propertyLocalMatches = null;
            _propertyLocalValues = null;
        }
    }


    static class Activation
    {
        Activation _parent;
        List<_StaticRec> _recs = new ArrayList();
        int _origEntryCount;
        GrowOnlyHashtable _valueNodeMapByContextKey;
        GrowOnlyHashtable _valueNodeMapByContextKeyChaining;
        Activation _propertyActivation;
        boolean _didInitPropertyActivation;
        NestedMap _nestedValues;
        List <_DeferredAssignment> deferredAssignments;

        Activation (Activation parent) { _parent = parent; }

        Activation get (String contextKey, Object value, boolean chaining)
        {
            if (value == null) value = Meta.NullMarker;
            Map byKey = (chaining) ? _valueNodeMapByContextKeyChaining : _valueNodeMapByContextKey;
            if (byKey == null) return null;
            GrowOnlyHashtable byVal = (GrowOnlyHashtable)byKey.get(contextKey);
            return (byVal == null) ? null : (Activation)byVal.get(value);
        }

        void put (String contextKey, Object value, Activation activation, boolean chaining)
        {
            if (value == null) value = Meta.NullMarker;
            GrowOnlyHashtable byKey;
            if (chaining) {
                if ((byKey = _valueNodeMapByContextKeyChaining) == null)
                    byKey = _valueNodeMapByContextKeyChaining = new GrowOnlyHashtable();
            } else {
                if ((byKey = _valueNodeMapByContextKey) == null)
                    byKey = _valueNodeMapByContextKey = new GrowOnlyHashtable();
            }

            GrowOnlyHashtable byVal = (GrowOnlyHashtable)byKey.get(contextKey);
            if (byVal == null) {
                byVal = new GrowOnlyHashtable();
                byKey.put(contextKey, byVal);
            }
            byVal.put(value, activation);
        }

        void addDeferredAssignment (String key, DynamicPropertyValue value) {
            _DeferredAssignment a = new _DeferredAssignment();
            a.key = key;
            a.value = value;
            if (deferredAssignments == null) deferredAssignments = new ArrayList();
            deferredAssignments.add(a);
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

        Activation propertyActivation (Context context)
        {
            Assert.that(context.currentActivation() == this, "PropertyActivation sought on non top of stack activation");
            if (!_didInitPropertyActivation) {
                _didInitPropertyActivation = true;
                _propertyActivation = context.applyNewPropertyContextActivation(this);
            }
            return _propertyActivation;
        }
    }

    static class _DeferredAssignment {
        String key;
        DynamicPropertyValue value;
    }


    // self reference thunk for FieldValue
    public class PropertyAccessor {
        Object get (String key) { return propertyForKey(key); }
        public String toString () { return allProperties().toString(); }
    }

    public interface DynamicPropertyValue
    {
        public Object evaluate (Context context);
    }

    // "marker" interface for DynamicPropertyValues that depend only on their
    // match context and therefore can be computed and cached statically in the
    // Context Activation tree
    public interface StaticallyResolvable extends DynamicPropertyValue
    {
    }

    public static class StaticDynamicWrapper implements StaticallyResolvable, Meta.PropertyMapAwaking
    {
        StaticallyResolvable _orig;
        Object _cached;

        public StaticDynamicWrapper (StaticallyResolvable value) { _orig = value; }

        public StaticallyResolvable getDynamicValue () { return _orig; }

        public Object awakeForPropertyMap(Meta.PropertyMap map)
        {
            // copy ourselves so there's a fresh copy for each context in which is appears
            return new StaticDynamicWrapper(_orig);
        }

        public Object evaluate(Context context)
        {
            // we lazily static evaluate our value and cace the result
            if (_cached == null) _cached = context.staticallyResolveValue(_orig);

            return _cached;
        }

        public String toString()
        {
            return super.toString() + " - " + ((_cached != null) ? _cached : " (unevaluated)");
        }
    }

    static class ContextFieldPath extends FieldPath implements DynamicPropertyValue
    {
        public ContextFieldPath (String path)
        {
            super(path);
        }

        public Object evaluate(Context context)
        {
            return getFieldValue(context);
        }
    }

    static class ExprValue implements DynamicPropertyValue
    {
        private Expression _expression;

        public ExprValue (String expressionString)
        {
            _expression = AribaExprEvaluator.instance().compile(expressionString);

        }

        public Object evaluate(Context context)
        {
            try {
                return _expression.evaluate(context, null);
            } catch (Exception e) {
                throw new AWGenericException(Fmt.S("Exception evaluating expression '%s' in context: %s --\n",
                        _expression, context), e);
            }
        }

        /*
            public void setValue (Object value, Object object)
            {
                assertExpression();
                if (_substituteBinding != null) {
                    _substituteBinding.setValue(value, object);
                } else {
                    ((AribaExprEvaluator.Expression)_expression).evaluateSet(object, value, null);
                }
            }
        */
    }

    protected static class DeferredOperationChain implements DynamicPropertyValue
    {
        Meta.PropertyMerger _merger;
        Object _orig;
        Object _override;

        public DeferredOperationChain (Meta.PropertyMerger merger, Object orig, Object override)
        {
            _merger = merger;
            _orig = orig;
            _override = override;
        }

        public Object evaluate(Context context)
        {
            return _merger.merge(context.resolveValue(_override), context.resolveValue(_orig));
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof DeferredOperationChain)) return false;
            DeferredOperationChain o = (DeferredOperationChain)other;
            return (_merger == o._merger)
                && Meta.objectEquals(_orig, o._orig)
                && Meta.objectEquals(_override, o._override);
        }
    }

    // Merge lists
    public static Meta.PropertyMerger PropertyMerger_DeclareList =  new Meta.PropertyMergerDynamic()
    {
        public Object merge(Object orig, Object override) {
            // if we're override a single element with itself, don't go List...
            if (!(orig instanceof List) && !(override instanceof List)
                    && Meta.objectEquals(orig, override)) {
                return orig;
            }

            MergeIfDeclare prev = (orig instanceof MergeIfDeclare) ? ((MergeIfDeclare)orig)
                    : new MergeIfDeclare(orig, null);
            return new MergeIfDeclare(override, prev);
        }
    };

    static class MergeIfDeclare implements StaticallyResolvable, Meta.PropertyMapAwaking
    {
        MergeIfDeclare _prev;
        Object _head;
        Object _cache;
        boolean canCache;

        public MergeIfDeclare (Object value, MergeIfDeclare prev)
        {
            _head = value;
            _prev = prev;
            canCache = (!(value instanceof DynamicPropertyValue) || (value instanceof StaticallyResolvable))
                    && (prev == null || prev.canCache);
        }

        public Object awakeForPropertyMap(Meta.PropertyMap map)
        {
            // copy ourselves so there's a fresh copy for each context in which is appears
            return new MergeIfDeclare(_head, _prev);
        }

        public Object evaluate(Context context) {
            Object result;
            if (_cache == null) {
                if (context.values().containsKey(Meta.KeyDeclare)) {
                    result = new ArrayList();
                    populate(context, (List)result);
                } else {
                    result = _head;
                }
                if (canCache) _cache = result;
            } else {
                result = _cache;
            }
            return result;
        }

        void populate (Context context, List result)
        {
            if (_prev != null) _prev.populate(context, result);
            Object value = (context != null) ? context.resolveValue(_head): _head;
            ListUtil.addElementsIfAbsent(result, Meta.toList(value));
        }

        public String toString ()
        {
            List result = new ArrayList();
            populate(null, result);
            return "<MergeIfDeclare: " + result + ">";
        }
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
            return ((Context.PropertyAccessor)target).get(fieldPath.car());
        }
    }
}

