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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/HintMessage.java#17 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.StringUtil;

public final class HintMessage extends AWComponent
{
    private static String OpenHelpWindow = "auiOpenHelpWindow('";
    private static String CloseFunction = "');";
    public boolean _isVisible;
    public boolean _showFooterMessage;
    public boolean _showTopLine;
    public AWEncodedString _elementId;

    protected void awake ()
    {
        _isVisible = computeIsVisible();
        if (_isVisible) {
            _showFooterMessage = booleanValueForBinding(BindingNames.showFooterMessage);
            _showTopLine = booleanValueForBinding(BindingNames.showTopLine);
        }
    }

    protected void sleep ()
    {
        _isVisible = false;
        _showFooterMessage = false;
        _showTopLine = false;
        _elementId = null;
    }

    private boolean computeIsVisible ()
    {
        boolean isVisible = true;
        AWBinding isVisibleBinding = bindingForName(AWBindingNames.isVisible);
        if (isVisibleBinding != null) {
            isVisible = booleanValueForBinding(isVisibleBinding);
        }
        else {
            isVisible = Widgets.getDelegate().hintMessagesVisible(requestContext());
        }
        return isVisible;
    }

    public boolean showTopLine ()
    {
        return _showTopLine;
    }

    public boolean hasFooterText ()
    {
        return _showFooterMessage;
    }

    /**
     * This returns the css class for the hint box.
     * @return
     */
    public String hintBoxClass ()
    {
        return booleanValueForBinding("isExpanded") ? "hintBoxOpen" : "hintBox";
    }


    private String resolveUrl (String url)
    {
        if (url.startsWith("/") ||
            url.startsWith("http:") ||
            url.startsWith("https:") ||
            url.startsWith("file:"))
        {
            return url;
        }

        // Support for relative anchors e.g. howToUrl="help/Foo.htm#SomeAnchor"
        int anchorIndex = url.lastIndexOf("#");
        String urlPart;
        String anchorPart;
        if (anchorIndex != -1) {
            urlPart = url.substring(0, anchorIndex);
            anchorPart = url.substring(anchorIndex);
        }
        else {
            urlPart = url;
            anchorPart = null;
        }

        String resolved = resourceManager().urlForResourceNamed(urlPart);
        if (resolved != null && anchorPart != null) {
            resolved = StringUtil.strcat(resolved, anchorPart);
        }
        return resolved == null ? url : resolved;
    }
}
