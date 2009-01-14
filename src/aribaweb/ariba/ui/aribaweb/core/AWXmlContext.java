/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWXmlContext.java#12 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import java.util.Map;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldValueAccessor;

final class AWBindingContext extends AWBinding
{
    private final AWBinding _binding;
    private final AWComponent _component;

    public boolean isConstantValue ()
    {
        return _binding.isConstantValue();
    }

    public boolean isSettableInComponent (Object object)
    {
        return _binding.isSettableInComponent(_component);
    }

    public AWBindingContext (AWBinding binding, AWComponent component)
    {
        _binding = binding;
        _component = component;
        this.init(_binding.bindingName());
    }

    public Object value (Object object)
    {
        return _binding.value(_component);
    }

    public void setValue (Object value, Object object)
    {
        _binding.setValue(value, _component);
    }

    public String bindingDescription ()
    {
        return StringUtil.strcat("<AWBindingContext>:", _binding.bindingDescription());
    }
}

public final class AWXmlContext extends AWBaseObject implements AWXmlNode
{
    private final AWXmlNode _xmlNode;
    private final AWComponent _component;
    private AWBindingDictionary _bindings;

    static {
        FieldValue.registerClassExtension(AWXmlContext.class, new AWXmlContextFieldValueClassExtension());
    }

    public AWXmlContext (AWXmlNode xmlNode, AWComponent component)
    {
        _xmlNode = xmlNode;
        _component = component;
        Assert.that(_xmlNode != null, "xmlNode must not be null");
        Assert.that(_component != null, "component must not be null");
    }

    //////////////////
    // XmlNode Cover
    //////////////////
    public String tagName ()
    {
        return _xmlNode.tagName();
    }

    public AWXmlNode[] childrenWithName (String name)
    {
        AWXmlNode[] xmlNodeArray = _xmlNode.childrenWithName(name);
        AWXmlNode[] convertedNodes = (AWXmlNode[])convertReturnValue(xmlNodeArray);
        return convertedNodes;
    }

    public AWBindingDictionary bindings ()
    {
        if (_bindings == null) {
            AWBindingDictionary bindings = _xmlNode.bindings();
            Map tempHashtable = MapUtil.map();
            for (int index = (bindings.size() - 1); index >= 0; index--) {
                String currentBindingName = bindings.keyAt(index);
                AWBinding currentBinding = bindings.elementAt(index);
                if (currentBinding != null) {
                    AWBindingContext bindingContext = new AWBindingContext(currentBinding, _component);
                    tempHashtable.put(currentBindingName, bindingContext);
                }
            }
            _bindings = AWBinding.bindingsDictionary(tempHashtable);
        }
        return _bindings;
    }

    ////////////
    // Util
    ////////////
    private Object convertReturnValue (Object objectValue)
    {
        if (objectValue instanceof AWXmlNode) {
            // Perf: Might want to recycle these somehow
            objectValue = new AWXmlContext((AWXmlNode)objectValue, _component);
        }
        else if (objectValue instanceof AWBinding) {
            objectValue = ((AWBinding)objectValue).value(_component);
        }
        else if (objectValue instanceof AWXmlNode[]) {
            AWXmlNode[] xmlNodeArray = (AWXmlNode[])objectValue;
            int xmlNodeArrayLength = xmlNodeArray.length;
            AWXmlContext[] xmlNodeContextArray = new AWXmlContext[xmlNodeArrayLength];
            for (int index = (xmlNodeArrayLength - 1); index >= 0; index--) {
                // Perf: another recycle opportunity
                xmlNodeContextArray[index] = new AWXmlContext(xmlNodeArray[index], _component);
            }
            objectValue = xmlNodeContextArray;
        }
        return objectValue;
    }

    static final class AWXmlContextFieldValueClassExtension extends FieldValue_Object
    {
        // Use inner class so we have access to the ivars of AWXmlContext

        public FieldValueAccessor createAccessor (Object receiver, String fieldName)
        {
            throw new AWGenericException(receiver.getClass().getName() + ": createAccessor() not suported");
        }

        public FieldValueAccessor getAccessor (Object receiver, String fieldName)
        {
            throw new AWGenericException(receiver.getClass().getName() + ": getAccessor() not suported");
        }

        public Object getFieldValuePrimitive (Object receiver, String keyString)
        {
            AWXmlContext xmlContext = (AWXmlContext)receiver;
            Object objectValue = FieldValue.getFieldValue(xmlContext._xmlNode, keyString);
            objectValue = xmlContext.convertReturnValue(objectValue);
            return objectValue;
        }

        public Object getFieldValuePrimitive (Object receiver, FieldPath fieldPath)
        {
            return getFieldValuePrimitive(receiver, fieldPath.car());
        }

        public Object getFieldValue (Object receiver, FieldPath fieldPath)
        {
            AWXmlContext xmlContext = (AWXmlContext)receiver;
            Object objectValue = FieldValue.getFieldValue(xmlContext._xmlNode, fieldPath.car());
            FieldPath remainingFieldPath = fieldPath.cdr();
            if (remainingFieldPath != null) {
                objectValue = remainingFieldPath.getFieldValue(objectValue);
            }
            objectValue = xmlContext.convertReturnValue(objectValue);
            return objectValue;
        }

        //////////////////
        // Unsupported kvc
        //////////////////
        private void throwTakeValueException (Object receiver)
        {
            throw new AWGenericException(receiver.getClass().getName() + " does not allow for takeValue...");
        }

        public void setFieldValue (Object receiver, FieldPath fieldPath, Object value)
        {
            throwTakeValueException(receiver);
        }

        public void setFieldValuePrimitive (Object receiver, FieldPath fieldPath, Object value)
        {
            throwTakeValueException(receiver);
        }

        public void setFieldValuePrimitive (Object receiver, String keyString, Object value)
        {
            throwTakeValueException(receiver);
        }
    }
}
