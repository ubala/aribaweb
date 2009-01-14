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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/WrapperActionDelimiterConditionHandler.java#5 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;

public class WrapperActionDelimiterConditionHandler extends ConditionHandler
{
    private static final String WrapperActionDelimiterKey = "NeedsWrapperActionDelimiter";

    public static void displayWrapperAction (AWRequestContext requestContext)
    {
        // We have a conditionals around each item in the pagewrapper header.  If any of
        // them evaluate to true, then we increment needsWrapper.  If needsWrapper is
        // more than one, then the next item will get a true back from the
        // WrapperActionDelimiterConditionHandler and the count will be decremented.
        // Reason for this is that the order of operations with the WrapperActionDelimiter
        // is DisplayCondition, DelimeterCheck, DisplayCondition, DelimeterCheck, ...

        Integer itemCount = (Integer)requestContext.get(WrapperActionDelimiterKey);
        if (itemCount == null) {
            itemCount = new Integer(1);
        }
        else {
            int count = itemCount.intValue() + 1;
            itemCount = new Integer(count);
        }
        requestContext.put(WrapperActionDelimiterKey, itemCount);
    }

    public boolean evaluateCondition (AWRequestContext requestContext)
    {
        // decrement count in requestContext
        Integer itemCount = (Integer)requestContext.remove(WrapperActionDelimiterKey);
        if (itemCount != null) {
            int count = itemCount.intValue();
            if (count > 0) {
                requestContext.put(WrapperActionDelimiterKey, new Integer(--count));
                return true;
            }
        }
        return false;
    }
}