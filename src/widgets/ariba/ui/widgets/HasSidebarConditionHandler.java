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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/HasSidebarConditionHandler.java#2 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;

public final class HasSidebarConditionHandler extends ConditionHandler
{
    public boolean evaluateCondition (AWRequestContext requestContext)
    {
        AWComponent currComponent = requestContext.getCurrentComponent();
        ConditionHandler sidebarVisible =
            ConditionHandler.resolveHandlerInComponent(BindingNames.isSidebarVisible,
                                                       currComponent);
        ConditionHandler hasNotch =
            ConditionHandler.resolveHandlerInComponent(BindingNames.hasSidebarNotch,
                                                       currComponent);

        // sidebar is removed if (!sidebarVisible && !hasNotch)
        return sidebarVisible.evaluateCondition(requestContext) ||
               hasNotch.evaluateCondition(requestContext);
    }
}
