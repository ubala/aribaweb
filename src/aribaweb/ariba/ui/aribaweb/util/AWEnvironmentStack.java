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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWEnvironmentStack.java#10 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValueAccessor;
import ariba.util.fieldvalue.FieldValue_Object;
import java.util.Iterator;
import java.util.Map;

public final class AWEnvironmentStack extends AWBaseObject
{
    private Map _stacksHashtable = MapUtil.map();

    // ** Thread Safety Considerations: It is the responsibility of the consumer of this class to synchronize access to it.

    static
    {
        FieldValue.registerClassExtension(
            AWEnvironmentStack.class,
            new AWEnvironmentStackFieldValueClassExtension());
    }

    public void push (String key, Object value)
    {
        FieldValue.setFieldValue(this, key, value);
    }

    public Object pop (String key)
    {
        AWArrayManager stack = (AWArrayManager)_stacksHashtable.get(key);
        if (stack == null || stack.isEmpty()) {
            throw new AWGenericException(
                Fmt.S("%s: attempt to get value from environment " +
                      "which has not been set -- key: %s",
                      getClass(),
                      key));
        }
        Object value =  stack.removeLastElement();
        return value == NullObject ? null : value;
    }

    public Object peek (String key)
    {
        AWArrayManager stack = (AWArrayManager)_stacksHashtable.get(key);
        Object value = (stack == null) ? null : stack.lastElement();
        return value == NullObject ? null : value;
    }

    public void clear (String key)
    {
        AWArrayManager stack = (AWArrayManager)_stacksHashtable.get(key);
        if (stack != null) {
            stack.clear();
        }
    }

    public void clear ()
    {
        _stacksHashtable.clear();
    }

    /////////////////////
    // Debuging
    /////////////////////
    public Map topOfStacks ()
    {
        Map topOfStacksHashtable = MapUtil.map(_stacksHashtable.size());
        if (!_stacksHashtable.isEmpty()) {
            Iterator keyIterator = _stacksHashtable.keySet().iterator();
            while (keyIterator.hasNext()) {
                String currentKey = (String)keyIterator.next();
                Object currentValue =
                    ((AWArrayManager)_stacksHashtable.get(
                        currentKey)).lastElement();
                topOfStacksHashtable.put(currentKey, currentValue);
            }
        }
        return topOfStacksHashtable;
    }

    static final class AWEnvironmentStackFieldValueClassExtension
        extends FieldValue_Object
    {
        public FieldValueAccessor createAccessor (Object receiver, String fieldName)
        {
            throw new AWGenericException(
                Fmt.S("%s: createAccessor() not suported", receiver.getClass()));
        }

        public FieldValueAccessor getAccessor (Object receiver, String fieldName)
        {
            throw new AWGenericException(
                Fmt.S("%s: getAccessor() not suported", receiver.getClass()));
        }

        public void setFieldValuePrimitive (Object receiver,
                                            FieldPath fieldPath,
                                            Object value)
        {
            setFieldValuePrimitive(receiver, fieldPath.car(), value);
        }

        public Object getFieldValuePrimitive (Object receiver, FieldPath fieldPath)
        {
            return getFieldValuePrimitive(receiver, fieldPath.car());
        }

        public void setFieldValuePrimitive (Object receiver, String key, Object value)
        {
            Map stacksHashtable = ((AWEnvironmentStack)receiver)._stacksHashtable;
            AWArrayManager stack = (AWArrayManager)stacksHashtable.get(key);
            if (stack == null) {
                stack = new AWArrayManager(Object.class);
                stacksHashtable.put(key, stack);
            }
            stack.addElement(value);
        }

        public Object getFieldValuePrimitive (Object receiver, String key)
        {
            Object objectValue = null;
            Map stacksHashtable = ((AWEnvironmentStack)receiver)._stacksHashtable;
            AWArrayManager stack = (AWArrayManager)stacksHashtable.get(key);
            if (stack == null) {
                throw new AWGenericException(
                    Fmt.S("%s: attempt to get value from environment " +
                          "which has not been set -- key: %s",
                          getClass(),
                          key));
            }
            int stackSize = stack.size();
            if (stackSize > 0) {
                objectValue = stack.objectArray()[stackSize - 1];
            }
            return (objectValue == NullObject) ? null : objectValue;
        }
    }
}
