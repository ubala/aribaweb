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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/PropertyValue.java#8 $
*/
package ariba.ui.meta.core;

import ariba.util.core.Fmt;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.Expression;
import ariba.util.expr.AribaExprEvaluator;
import ariba.ui.aribaweb.util.AWGenericException;

public class PropertyValue
{
    public interface Dynamic
    {
        public Object evaluate (Context context);
    }

    public interface DynamicSettable
    {
        public void evaluateSet (Context context, Object value);
    }

    // "marker" interface for DynamicPropertyValues that depend only on their
    // match context and therefore can be computed and cached statically in the
    // Context Activation tree
    public interface StaticallyResolvable extends Dynamic
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
            StaticallyResolvable orig = (_orig instanceof Meta.PropertyMapAwaking)
                        ? (StaticallyResolvable)((Meta.PropertyMapAwaking)_orig).awakeForPropertyMap(map)
                        : _orig;
            return new StaticDynamicWrapper(orig);
        }

        public Object evaluate (Context context)
        {
            // we lazily static evaluate our value and cache the result
            if (_cached == null) _cached = context.staticallyResolveValue(_orig);

            return _cached;
        }

        public String toString()
        {
            return Fmt.S("StaticDynamicWrapper (%s%s)",
                    ((_cached != null) ? _cached : _orig),
                    ((_cached == null) ? " unevaluated" : ""));
        }

        public int hashCode ()
        {
            return _orig.hashCode();
        }

        public boolean equals (Object o)
        {
            return (o != null) && (o instanceof StaticDynamicWrapper)
                    && _orig.equals(((StaticDynamicWrapper)o)._orig);
        }
    }

    // Wrapper that marks a normally dynamic value (e.g. an Expr) as StaticallyResolvable 
    public static class StaticallyResolvableWrapper implements StaticallyResolvable
    {
        Dynamic _orig;

        public StaticallyResolvableWrapper (Dynamic orig)
        {
            _orig = orig;
        }

        public Object evaluate (Context context)
        {
            return _orig.evaluate(context);
        }

        public String toString()
        {
            return Fmt.S("StaticallyResolvableWrapper (%s)",
                    _orig);
        }
    }

    static class ContextFieldPath extends FieldPath implements Dynamic, DynamicSettable
    {
        public ContextFieldPath (String path)
        {
            super(path);
        }

        public Object evaluate(Context context)
        {
            return getFieldValue(context);
        }

        public void evaluateSet (Context context, Object value)
        {
            setFieldValue(context, value);
        }
    }

    public static class Expr implements Dynamic
    {
        private Expression _expression;

        public Expr (String expressionString)
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

        public String toString()
        {
            return Fmt.S("${%s}", _expression.toString());
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

    protected static class DeferredOperationChain implements Dynamic, Meta.PropertyMapAwaking
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
            return _merger.merge(context.resolveValue(_override), context.resolveValue(_orig),
                                        context.isDeclare());
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof DeferredOperationChain)) return false;
            DeferredOperationChain o = (DeferredOperationChain)other;
            return (_merger == o._merger)
                && Meta.objectEquals(_orig, o._orig)
                && Meta.objectEquals(_override, o._override);
        }

        public String toString()
        {
            return Fmt.S("Chain<%s>: %s => %s", _merger, _override, _orig);
        }

        public Object awakeForPropertyMap (Meta.PropertyMap map)
        {
            Object orig = _orig;
            Object over = _override;
            if (orig instanceof Meta.PropertyMapAwaking) orig = ((Meta.PropertyMapAwaking)orig).awakeForPropertyMap(map);
            if (over instanceof Meta.PropertyMapAwaking) over = ((Meta.PropertyMapAwaking)over).awakeForPropertyMap(map);
            if (orig != _orig || over != _override) return new DeferredOperationChain(_merger, orig, over);
            return this;
        }
    }
}
