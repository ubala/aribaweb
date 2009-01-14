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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWConcreteXmlNode.java#6 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWFastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.GrowOnlyHashtable;
import java.util.Map;
import java.util.List;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldValueAccessor;
import java.lang.reflect.Field;

public final class AWConcreteXmlNode extends AWBindableElement implements AWXmlNode, AWElementContaining
{
    private static final AWXmlNode[] EmptyNodeArray = {};
    private static final AWBindingDictionary EmptyBindingDictionary = new AWBindingDictionary();
    private String _tagName;
    private AWBindingDictionary _bindings = EmptyBindingDictionary;
    private AWXmlNode[] _xmlNodeChildren = EmptyNodeArray;
    private GrowOnlyHashtable _childrenByName;

    static {
        FieldValue.registerClassExtension(AWConcreteXmlNode.class, new AWConcreteXmlNodeFieldValueClassExtension());
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        if (bindingsHashtable != null) {
            if (tagName == null) {
                throw new AWGenericException(getClass().getName() + ": requires a tagName.");
            }
            _tagName = tagName;
            _bindings = AWBinding.bindingsDictionary(bindingsHashtable);
        }
        super.init(tagName, bindingsHashtable);
    }

    public void add (AWElement element)
    {
        if (element instanceof AWXmlNode) {
            _xmlNodeChildren = (AWXmlNode[])AWUtil.addElement(_xmlNodeChildren, element);
        }
    }

    public String tagName ()
    {
        return _tagName;
    }

    protected GrowOnlyHashtable childrenByName ()
    {
        if (_childrenByName == null) {
            _childrenByName = new GrowOnlyHashtable();
        }
        return _childrenByName;
    }

    /////////////////////
    // Child Lookup
    /////////////////////
    public AWXmlNode childWithName (String nodeName)
    {
        int nodeCount = _xmlNodeChildren.length;
        for (int index = 0; index < nodeCount; index++) {
            AWXmlNode xmlNode = _xmlNodeChildren[index];
            String currentNodeTagName = xmlNode.tagName();
            if (nodeName.equals(currentNodeTagName)) {
                return xmlNode;
            }
        }
        return null;
    }

    public AWXmlNode[] childrenWithName (String nodeNameWithWildcard)
    {
        GrowOnlyHashtable childrenByName = childrenByName();
        AWXmlNode[] childrenArray = (AWXmlNode[])childrenByName.get(nodeNameWithWildcard);
        if (childrenArray == null) {
            synchronized (childrenByName) {
                childrenArray = (AWXmlNode[])childrenByName.get(nodeNameWithWildcard);
                if (childrenArray == null) {
                    List childrenVector = ListUtil.list();
                    int nodeCount = _xmlNodeChildren.length;
                    boolean getAllChildren = nodeNameWithWildcard.equals("*");
                    for (int index = 0; index < nodeCount; index++) {
                        AWXmlNode currentChild = _xmlNodeChildren[index];
                        String currentNodeTagName = currentChild.tagName();
                        // This assumes a wildcard char "*" is appended to nodeName.
                        if (getAllChildren || nodeNameWithWildcard.startsWith(currentNodeTagName)) {
                            childrenVector.add(currentChild);
                        }
                    }
                    childrenArray = (AWXmlNode[])AWUtil.getArray(childrenVector, AWConcreteXmlNode.class);
                    childrenByName.put(nodeNameWithWildcard, childrenArray);
                }
            }
        }
        return childrenArray;
    }

    public AWBinding bindingForName (String bindingName)
    {
        return _bindings.get(bindingName);
    }

    public AWBindingDictionary bindings ()
    {
        return _bindings;
    }

    /////////////
    // Debug
    /////////////
    public String toString ()
    {
        AWFastStringBuffer stringBuffer = new AWFastStringBuffer();
        stringBuffer.append("<");
        stringBuffer.append(_tagName);
        if (_bindings != null) {
            for (int index = (_bindings.size() - 1); index >= 0; index--) {
                AWBinding currentBinding = _bindings.elementAt(index);
                stringBuffer.append(" ");
                stringBuffer.append(currentBinding.toString());
            }
        }
        if (_xmlNodeChildren.length == 0) {
            stringBuffer.append("/>");
        }
        else {
            stringBuffer.append(">");
            int childCount = _xmlNodeChildren.length;
            for (int index = 0; index < childCount; index++) {
                AWXmlNode currentChild = _xmlNodeChildren[index];
                stringBuffer.append("\n    ");
                stringBuffer.append(currentChild.toString());
            }
            stringBuffer.append("</");
            stringBuffer.append(_tagName);
            stringBuffer.append(">");
        }
        return stringBuffer.toString();
    }

    static final class AWConcreteXmlNodeFieldValueClassExtension extends FieldValue_Object
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
            Object objectValue = null;
            AWConcreteXmlNode xmlNode = (AWConcreteXmlNode)receiver;
            if (keyString.endsWith("*")) {
                objectValue = xmlNode.childrenWithName(keyString);
            }
            else {
                AWBinding nodeBinding = xmlNode.bindingForName(keyString);
                if (nodeBinding != null) {
                    objectValue = (nodeBinding.isConstantValue()) ? nodeBinding.value(null) : nodeBinding;
                }
                else {
                    objectValue = xmlNode.childWithName(keyString);
                }
            }
            return objectValue;
        }

        public Object getFieldValuePrimitive (Object receiver, FieldPath fieldPath)
        {
            return getFieldValuePrimitive(receiver, fieldPath.car());
        }

        public Object getFieldValue (Object receiver, FieldPath fieldPath)
        {
            Object objectValue = getFieldValuePrimitive(receiver, fieldPath);
            FieldPath remainingFieldPath = fieldPath.cdr();
            if (remainingFieldPath != null) {
                remainingFieldPath.getFieldValue(objectValue);
            }
            return objectValue;
        }

        //////////////////
        // Unsupported kvc
        //////////////////
        private void throwTakeValueException ()
        {
            throw new AWGenericException(getClass().getName() + " does not allow for takeValue...");
        }

        public void setFieldValue (Object receiver, FieldPath fieldPath, Object value)
        {
            throwTakeValueException();
        }

        public void setFieldValuePrimitive (Object receiver, FieldPath fieldPath, Object value)
        {
            throwTakeValueException();
        }

        public void setFieldValuePrimitive (Object receiver, String keyString, Object value)
        {
            throwTakeValueException();
        }
    }

    protected Object getFieldValue (Field field)
     throws IllegalArgumentException, IllegalAccessException
    {
        try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }
}
