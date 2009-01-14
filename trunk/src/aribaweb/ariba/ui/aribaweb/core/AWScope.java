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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWScope.java#9 $
*/

package ariba.ui.aribaweb.core;

import java.util.Map;
import java.util.Iterator;

public final class AWScope extends AWContainerElement
{
    private AWBinding[] _destinationBindings;
    private AWBinding[] _sourceBindings;
    private Object[] _previousValues;

    public void init (String tagName, Map bindingsHashtable)
    {
        _destinationBindings = new AWBinding[bindingsHashtable.size()];
        _sourceBindings = new AWBinding[_destinationBindings.length];
        Iterator keys = bindingsHashtable.keySet().iterator();
        int index = 0;
        while (keys.hasNext()) {
            String currentKey = (String)keys.next();
            AWBinding currentBinding = (AWBinding)bindingsHashtable.get(currentKey);
            _sourceBindings[index] = currentBinding;
            _destinationBindings[index] =
                AWBinding.bindingWithNameAndKeyPath(currentKey, currentKey);
            index++;
        }
        super.init(tagName, null);
    }

    private Object[] checkoutPreviousValues ()
    {
        Object[] previousValues = null;
        synchronized (this) {
            previousValues = _previousValues;
            _previousValues = null;
        }
        if (previousValues == null) {
            previousValues = new Object[_sourceBindings.length];
        }
        return previousValues;
    }

    private void checkinPreviousValues (Object[] previousValues)
    {
        _previousValues = previousValues;
    }

    protected Object[] pushBindingValues (AWComponent component)
    {
        Object[] previousValues = checkoutPreviousValues();
        for (int index = _sourceBindings.length - 1; index > -1; index--) {
            AWBinding sourceBinding = _sourceBindings[index];
            AWBinding destinationBinding = _destinationBindings[index];
            previousValues[index] = destinationBinding.value(component);
            Object currentValue = sourceBinding.value(component);
            destinationBinding.setValue(currentValue, component);
        }
        return previousValues;
    }

    protected void popBindingValues (AWComponent component, Object[] previousValues)
    {
        for (int index = _sourceBindings.length - 1; index > -1; index--) {
            AWBinding destinationBinding = _destinationBindings[index];
            destinationBinding.setValue(previousValues[index], component);
        }
        checkinPreviousValues(previousValues);
    }

    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        Object[] previousValues = pushBindingValues(component);
        try {
            super.applyValues(requestContext, component);
        }
        finally {
            popBindingValues(component, previousValues);
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        Object[] previousValues = pushBindingValues(component);
        try {
            actionResults = super.invokeAction(requestContext, component);
        }
        finally {
            popBindingValues(component, previousValues);
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        Object[] previousValues = pushBindingValues(component);
        try {
            super.renderResponse(requestContext, component);
        }
        finally {
            popBindingValues(component, previousValues);
        }
    }
}
