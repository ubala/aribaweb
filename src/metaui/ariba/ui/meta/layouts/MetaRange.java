/*
    Copyright 2009 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaRange.java#1 $
*/
package ariba.ui.meta.layouts;

import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.core.Assert;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.persistence.Predicate;
import ariba.ui.aribaweb.core.AWComponent;

/**
    Used a field-level component in search views to render From/To range selection
 */
public class MetaRange extends AWComponent
{
    static {
        FieldValue.registerClassExtension(MetaRange.ValueRedirector.class,
                               new FieldValue_ValueRedirector());

        UIMeta.getInstance().registerValueTransformerForKey("valueRedirector", Meta.Transformer_KeyPresent);
    }

    /*
        Instance assigned as the value property in the context will forward get/sets to the
        given keypath of the metacontext's current object
     */
    public static class ValueRedirector
    {
        ObjectMeta.ObjectMetaContext _ctx;
        String _keyPath;

        public ValueRedirector (ObjectMeta.ObjectMetaContext ctx, String keyPath)
        {
            _ctx = ctx;
            _keyPath = keyPath;
        }

        public Object getSourceObject ()
        {
            String field = (String)_ctx.propertyForKey(ObjectMeta.KeyField);
            Assert.that(field != null, "No current field");
            return FieldValue.getFieldValue(_ctx.object(), field);
        }

        public Object createSourceObject ()
        {
            Predicate.RangeValue obj = new Predicate.RangeValue();
            String field = (String)_ctx.propertyForKey(ObjectMeta.KeyField);
            Assert.that(field != null, "No current field");
            FieldValue.setFieldValue(_ctx.object(), field, obj);
            return obj;
        }

        public Object getValue()
        {
            Object range = getSourceObject();
            return (range == null) ? null : FieldValue.getFieldValue(range, _keyPath);
        }

        public void setValue(Object value)
        {
            Object range = getSourceObject();
            if (range == null && value == null) return;
            if (range == null) range = createSourceObject();
            FieldValue.setFieldValue(range, _keyPath, value);
        }
    }

    public final static class FieldValue_ValueRedirector extends FieldValue_Object
    {
        public void setFieldValuePrimitive (Object target, FieldPath fieldPath, Object value)
        {
            ((ValueRedirector)target).setValue(value);
        }

        public Object getFieldValuePrimitive (Object target, FieldPath fieldPath)
        {
            return ((ValueRedirector)target).getValue();
        }
    }
    
    public Object redirectorFrom ()
    {
        return new ValueRedirector(MetaContext.currentContext(this), "from");
    }

    public Object redirectorTo ()
    {
        return new ValueRedirector(MetaContext.currentContext(this), "to");
    }
}
