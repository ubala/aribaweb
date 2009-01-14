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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWAppendEnvironment.java#7 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.ui.aribaweb.util.AWUtil;
import java.lang.reflect.Field;
import java.util.Map;

public class AWAppendEnvironment extends AWContainerElement
{
    AWBindingDictionary _bindings;
    // ** Thread Safety Considerations: Subcomponents have no threading issues, especially this one since there are no ivars.

    public void init (String tagName, Map bindingsHashtable)
    {
        _bindings = AWBinding.bindingsDictionary(bindingsHashtable);
        super.init(tagName, null);
    }

    public AWBinding[] allBindings ()
    {
        AWBinding[] superBindings = super.allBindings();
        java.util.List bindingVector = _bindings.elementsVector();
        AWBinding[] myBindings = new AWBinding[bindingVector.size()];
        bindingVector.toArray(myBindings);
        return (AWBinding[])(AWUtil.concatenateArrays(superBindings, myBindings));
    }

    protected void pushBindingValues (AWEnvironmentStack environmentStack, AWComponent component)
    {
        AWBindingDictionary bindings = _bindings;
        for (int index = bindings.size() - 1; index >= 0; index--) {
            AWBinding currentBinding = bindings.elementAt(index);
            Object currentValue = currentBinding.value(component);
            String currentBindingName = bindings.keyAt(index);
            environmentStack.push(currentBindingName, currentValue);
        }
    }

    protected void popBindingValues (AWEnvironmentStack environmentStack, AWComponent component)
    {
        AWBindingDictionary bindings = _bindings;
        for (int index = bindings.size() - 1; index >= 0; index--) {
            String currentBindingName = bindings.keyAt(index);
            environmentStack.pop(currentBindingName);
        }
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWEnvironmentStack environmentStack = environmentStack(component);
        pushBindingValues(environmentStack, component);
        try {
            super.renderResponse(requestContext, component);
        }
        finally {
            popBindingValues(environmentStack, component);            
        }
    }

    protected AWEnvironmentStack environmentStack (AWComponent component)
    {
        return component.env();
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
