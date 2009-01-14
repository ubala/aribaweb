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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/CompletionPopup.java#4 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.fieldvalue.OrderedList;
import ariba.ui.widgets.BindingNames;

public class CompletionPopup extends AWComponent
{
    private static final AWEncodedString OpenDiv = new AWEncodedString("<div class=\"CPItem\">");
    private static final AWEncodedString CloseDiv = new AWEncodedString("</div>");
    private static final AWEncodedString OtherDiv = new AWEncodedString("<div class=\"CPItem CPOthers\"></div>");
    public AWEncodedString _spanId;
    public AWEncodedString _divId;

    protected void sleep ()
    {
        _spanId = null;
        _divId = null;
    }

    public AWResponse generateList ()
    {
        AWResponse response = application().createResponse();
        String prefix = request().formValueForKey("cpprefix");
        setValueForBinding(prefix, BindingNames.prefix);
        Object stringList = valueForBinding(BindingNames.list);
        OrderedList orderedList = OrderedList.get(stringList);
        int length = orderedList.size(stringList);
        int maxLength = maxLength(length);
        for (int index = 0; index < maxLength; index++) {
            String string = (String)orderedList.elementAt(stringList, index);
            response.appendContent(OpenDiv);
            response.appendContent(string);
            response.appendContent(CloseDiv);
        }
        if (length > maxLength) {
            response.appendContent(OtherDiv);
        }
        return response;
    }

    private int maxLength (int length)
    {
        int maxLength = 100;
        AWBinding maxLengthBinding = bindingForName(BindingNames.maxLength);
        if (maxLengthBinding != null) {
            maxLength = intValueForBinding(maxLengthBinding);
            if (maxLength < 1) {
                maxLength = 10;
            }
        }
        return Math.min(length, maxLength);
    }
}
