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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Context.java#4 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
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

public class Context implements Extensible
{
    protected static boolean _DebugRuleMatches = false;
    
    Meta _meta;
    HashMap<String, Object> _values = new HashMap();
    ArrayList <_ContextRec> _entries = new ArrayList();
    ArrayList <Integer> _frameStarts = new ArrayList();
    PropertyAccessor _accessor = new PropertyAccessor();
    Meta.PropertyMap _currentProperties;
    boolean _didPushContextScope;

    static {
        FieldValue.registerClassExtension(Context.PropertyAccessor.class,
                               new FieldValue_ContextPropertyAccessor());
    }

    public Context (Meta meta)
    {
        _meta = meta;
    }

    public void push ()
    {
        prepareToEvaluate();
        _clearPropertyContext();
        _frameStarts.add(_entries.size());
    }

    void _pop (Activation activation)
    {
        if (activation != null) {
            prepareToEvaluate();
        }
        Assert.that(_frameStarts.size() > 0, "Popping empty stack");
        int pos = (Integer) ListUtil.removeLastElement(_frameStarts);
        while (_entries.size() > pos) {
            _popRec(activation);
        }
        _currentProperties = null;
    }

    private _ContextRec _popRec(Activation activation)
    {
        _ContextRec rec = (_ContextRec) ListUtil.removeLastElement(_entries);
        if (rec.oldVal == null) {
            _values.remove(rec.key);
        } else {
            _values.put(rec.key, rec.oldVal);
        }
        if (activation != null) {
            activation._recs.add(rec);
        }
        if (rec.isPropContext) _didPushContextScope = false;
        return rec;
    }

    public void set (String key, Object value)
    {
        _set(key, value, false);
    }

    public void merge (String key, Object value)
    {
        _set(key, value, true);
    }

    protected boolean _set (String key, Object value, boolean merge)
    {
        boolean hasOldValue = _values.containsKey(key);
        Object oldVal = hasOldValue ? _values.get(key) : null;
        if (!hasOldValue || (oldVal != value && (oldVal == null ||
                (!oldVal.equals(value) && (!(oldVal instanceof List) || !((List)oldVal).contains(value)))))) {
            _clearPropertyContext();
            Meta.MatchResult lastMatch;
            _ContextRec rec = new _ContextRec();

            if (oldVal == null) {
                lastMatch = lastMatch();
            }
            else {
                // We recompute that match up to this point by recomputing forward
                // from the point of the last assignment to this key (skipping it), then
                // match against the array of our value and the old
                int prevIdx = findLastAssignmentOfKey(key);
                Assert.that(prevIdx >=0, "Value in context but no assignment record found");
                if (merge) value = Meta.PropertyMerger_List.merge(oldVal, value);
                rec.oldVal = oldVal;
                lastMatch = (prevIdx > 0) ? _entries.get(prevIdx - 1).match : null;
                for (int i=prevIdx+1, c=_entries.size(); i < c; i++) {
                    _ContextRec r = _entries.get(i);
                    lastMatch = _meta.match(r.key, r.val, lastMatch);
                }
            }

            rec.key = key;
            rec.val = value;
            _values.put(key, value);
            // Log.meta_detail.debug("Context set %s = %s  (was: %s)", key, value, oldVal);
            rec.match = (value != null) ? _meta.match(key, value, lastMatch) : lastMatch;
            if (_DebugRuleMatches) _checkMatch(rec.match, key, value);
            _entries.add(rec);
            _currentProperties = null;
            return true;
        }
        return false;
    }

    void _checkMatch (Meta.MatchResult match, String key, Object value)
    {
        _meta._checkMatch(match, _values);
    }

    int findLastAssignmentOfKey (String key)
    {
        for (int i=_entries.size()-1; i >= 0; i--) {
            _ContextRec rec = _entries.get(i);
            if (rec.key.equals(key)) return i;
        }
        return -1;
    }

    public void pop ()
    {
        _pop(null);
    }

    /*
        Freeze-dry a context frame so that it can be restored later
            -- can only be restored (safely) when context has otherwise been restored to
               the same state as the the prior push()
     */
    public Activation popActivation ()
    {
        Activation activation = new Activation();
        _pop(activation);
        activation._origEntryCount = _entries.size();
        return activation;
    }

    public void restoreActivation (Activation activation)
    {
        push();
        Assert.that(_entries.size() == activation._origEntryCount,
                "Mismatched context stack size (%s) from when activation was popped (%s)",
                Integer.toString(_entries.size()),
                Integer.toString(activation._origEntryCount));
        int i = activation._recs.size();
        while (i-- > 0) {
            _ContextRec rec = activation._recs.get(i);
            if (rec.isPropContext) _didPushContextScope = true;
            _values.put(rec.key, rec.val);
            _entries.add(rec);
        }
        _currentProperties = null;
    }

    Meta.MatchResult lastMatch ()
    {
        return _entries.isEmpty() ? null : _entries.get(_entries.size()-1).match;
    }

    // Check if we have value mirroring (property to context) to do
    boolean _checkApplyProperties ()
    {
        boolean didSet = false;
        int numEntries;
        _ContextRec rec;
        while ((numEntries = _entries.size()) > 0
                && !((rec = _entries.get(numEntries-1)).didRunPropMirroring)) {
            rec.didRunPropMirroring = true;
            Meta.PropertyMap properties = rec.match.properties();
            List<Meta.PropertyManager> contextKeys = properties.contextKeysUpdated();
            if (contextKeys != null) {
                for (int i=0,c=contextKeys.size(); i < c; i++) {
                    Meta.PropertyManager propMgr  = contextKeys.get(i);
                    Log.meta_detail.debug("checking key: %s", propMgr._keyDataToSet._key);
                    didSet |= _set(propMgr._keyDataToSet._key, resolveValue(properties.get(propMgr._name)), false);
                }
            }
        }
        return didSet;
    }

    // Check if we have value mirroring (property to context) to do
    boolean _checkPropertyContext ()
    {
        if (_didPushContextScope) return false;

        for (int i = _entries.size() - 1; i >=0; i--) {
            _ContextRec rec = _entries.get(i);
            String scopeKey = meta().keyData(rec.key).propertyScopeKey();
            if (scopeKey != null) {
                _set(scopeKey, true, false);
                ListUtil.lastElement(_entries).isPropContext = true;
                _didPushContextScope = true;
                return true;
            }
        }
        return false;
    }

    void _clearPropertyContext ()
    {
        if (!_didPushContextScope) return;

        _currentProperties = null;
        while (_entries.size() > 0) {
            _ContextRec rec = _popRec(null);
            if (rec.isPropContext) return;
        }
        Assert.that(false, "Had _didPushContextScope==true, but didn't find rec with isPropContext==true");
    }

    void prepareToEvaluate ()
    {
        boolean didAdd, first = true;
        do {
            didAdd =_checkPropertyContext();
            if (first || didAdd) didAdd |=_checkApplyProperties();
            first = false;
        } while (didAdd);
    }


    public Meta.PropertyMap allProperties ()
    {
        if (_currentProperties == null) {
            prepareToEvaluate();
            Meta.MatchResult m = lastMatch();
            if (m != null) {
                _currentProperties = m.properties();
                /*
                try {
                    throw new AWGenericException(Fmt.S("Set current Properties: %s", _currentProperties));
                } catch (Exception e) {
                    Log.meta.debug("Set current Properties: %s", _currentProperties);
                    e.printStackTrace();
                }
                */
            }
        }
        return _currentProperties;
    }

    public Map resolvedProperties ()
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

    public Object resolveValue (Object value)
    {
        return (value != null && value instanceof DynamicPropertyValue)
                ? ((DynamicPropertyValue)value).evaluate(this)
                : value;
    }

    public Meta meta ()
    {
        return _meta;
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

    public Map values ()
    {
        return _values;
    }

    // FieldValue lookup will find extra props in our values hashtable
    public Map extendedFields ()
    {
        return _values;
    }
/*
    public String toString ()
    {
        return "Context values: " + _values; // + ", props: " + allProperties();
    }
*/
    // self reference thunk for FieldValue
    public class PropertyAccessor {
        Object get (String key) { return propertyForKey(key); }
        public String toString () { return resolvedProperties().toString(); }
    }

    // a (usable) snapshot of the current state of the context
    public Context snapshot()
    {
        Context copy = (Context)newInnerInstance(getClass(), _meta);
        copy._entries.addAll(_entries);
        copy._frameStarts.addAll(_frameStarts);
        copy._values.putAll(_values);
        return copy;
    }


    static class _ContextRec {
        String key;
        Object val;
        Object oldVal;
        Meta.MatchResult match;
        boolean didRunPropMirroring;
        boolean isPropContext;
    }

    public static class Activation
    {
        List<_ContextRec> _recs = new ArrayList();
        int _origEntryCount;
    }

    public interface DynamicPropertyValue
    {
        public Object evaluate (Context context);
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

