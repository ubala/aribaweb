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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWEnvironment.java#8 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEnvironmentStack;

import java.lang.reflect.Field;

public final class AWEnvironment extends AWAppendEnvironment
{
    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        AWEnvironmentStack environmentStack = component.env();
        pushBindingValues(environmentStack, component);
        try {
            super.applyValues(requestContext, component);
        }
        finally {
            popBindingValues(environmentStack, component);
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWEnvironmentStack environmentStack = component.env();
        pushBindingValues(environmentStack, component);
        AWResponseGenerating actionResults = null;
        try {
            actionResults = super.invokeAction(requestContext, component);
        }
        finally {
            popBindingValues(environmentStack, component);
        }
        return actionResults;
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
