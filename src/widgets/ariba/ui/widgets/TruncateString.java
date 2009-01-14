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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TruncateString.java#14 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.StringUtil;

public final class TruncateString extends AWComponent
{
    private static final AWEncodedString Elipsis =
        AWEncodedString.sharedEncodedString("...");
    public AWEncodedString _elipsis = null;
    public String _value, _front = null, _end = null;

    public boolean isStateless()
    {
        return true;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        _front = _value = stringValueForBinding(BindingNames.value);
        if (!StringUtil.nullOrEmptyOrBlankString(_value)) {
            AWBinding sizeBinding = bindingForName(BindingNames.size, true);
            if (sizeBinding != null) {
                int size = intValueForBinding(sizeBinding);
                int len = _value.length();
                if (_value.length() > (size + 3)) {
                    _elipsis = Elipsis;
                    int style = intValueForBinding("truncationStyle");
                    if (style == 0) {
                        // front
                        _front = _value.substring(0, size);
                    }
                    else if (style == 1) {
                        // middle
                        int frontLen = size/2, endLen = (size - (size/2));
                        _front = _value.substring(0, frontLen);
                        _end = _value.substring(len - endLen, len);
                    }
                    else {
                        // end
                        _front = null;
                        _end = _value.substring(len - size, len);
                    }
                }
            }
        }
        super.renderResponse(requestContext, component);
        _value = _front = _end = null;
        _elipsis = null;
    }

    /**
        @param str String to truncated.  Must be plain text.
    */
    public static String truncatedString (String str, int size)
    {
        if (str.length() > (size + 3)) {
            str = Fmt.S("%s...", str.substring(0, size));
        }

        return str;
    }
}
