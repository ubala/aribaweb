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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/DisableHelpActionConditionHandler.java#6 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;

public class DisableHelpActionConditionHandler extends ConditionHandler
{
    public boolean evaluateCondition (AWRequestContext requestContext)
    {
        AWComponent parentComponent = requestContext.getCurrentComponent().parent();
        if (!(parentComponent instanceof PageWrapper)) {
            parentComponent = parentComponent.parent();
        }
        return parentComponent.booleanValueForBinding(BindingNames.disableHelpAction);
    }
}