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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/RemoveContentLeftRightMarginHandler.java#1 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

public final class RemoveContentLeftRightMarginHandler extends ConditionHandler
{
    public boolean evaluateCondition (AWRequestContext requestContext)
    {
        Boolean value = (Boolean)findValueForBinding(requestContext.getCurrentComponent(),
            BindingNames.removeContentLeftRightMargin);
        return value != null ? value.booleanValue() : false;
    }

    private Object findValueForBinding (AWComponent component, String bindingName)
    {
        for (; component != null; component = component.parent()) {
            AWBinding binding = component.bindingForName(bindingName);
            if (binding != null) {
                return component.valueForBinding(binding);
            }
        }
        return null;
    }
}