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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWSetValue.java#9 $
*/

package ariba.ui.aribaweb.core;

import java.util.Map;
import java.util.Iterator;

public class AWSetValue extends AWBindableElement
{
    protected static final int Append_Phase = 0;
    protected static final int Invoke_Phase = 1;
    protected static final int Take_Phase = 2;

    private AWBinding[] _destinationBindings;
    private AWBinding[] _sourceBindings;

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

    protected void pushBindingValues (AWComponent component)
    {
        for (int index = _sourceBindings.length - 1; index > -1; index--) {
            AWBinding sourceBinding = _sourceBindings[index];
            AWBinding destinationBinding = _destinationBindings[index];
            Object currentValue = sourceBinding.value(component);
            destinationBinding.setValue(currentValue, component);
        }
    }

    protected boolean shouldPush (AWComponent component, int phase)
    {
            // we re-set value every time through
        return true;
    }

    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        if (shouldPush(component, Take_Phase)) {
            pushBindingValues(component);
        }
        super.applyValues(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        if (shouldPush(component, Invoke_Phase)) {
            pushBindingValues(component);
        }
        actionResults = super.invokeAction(requestContext, component);
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (shouldPush(component, Append_Phase)) {
            pushBindingValues(component);
        }
        super.renderResponse(requestContext, component);
    }
}
