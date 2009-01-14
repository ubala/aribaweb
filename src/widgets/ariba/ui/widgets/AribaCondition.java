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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaCondition.java#12 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Assert;

public final class AribaCondition extends AWComponent
{
    public boolean evaluateCondition ()
    {
        boolean shouldNegate = false;
        String condition = stringValueForBinding(AWBindingNames.ifTrue);
        if (condition == null) {
            condition = stringValueForBinding(AWBindingNames.ifFalse);
            shouldNegate = true;
        }
        Assert.that(condition != null, "Condition must have ifTrue or ifFalse binding");

        ConditionHandler handler = ConditionHandler.resolveHandlerInComponent(condition, this);
        boolean result = handler == null ? false : handler.evaluateCondition(requestContext());
        return shouldNegate ? !result : result;
    }
}
