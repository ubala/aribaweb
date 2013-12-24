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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PopupMenuLink.java#31 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWRequestContext;

public final class PopupMenuLink extends AWComponent
{
    // Note: the menuId and index bindings are evaluated in PopupMenu.menuId(),
    // so even though they're not referenced in this file, they are SupportedBinding's.
    private static final String[] SupportedBindingNames = {
        BindingNames.tagName, "containerStyle",
        BindingNames.menuId, BindingNames.index, BindingNames.classBinding, BindingNames.onMouseOver,
        BindingNames.onMouseOut, BindingNames.omitTags, BindingNames.title, BindingNames.style,
        BindingNames.actionSetup, BindingNames.position, BindingNames.shouldOpen,  BindingNames.behavior
    };

    public AWEncodedString _elementId;
    public AWBinding _menuIdBinding;
    public AWBinding _indexBinding;
    public AWBinding _positionBinding;
    public AWBinding _actionSetupBinding;
    public AWBinding _classBinding;
    public AWBinding _styleBinding;
    public AWBinding _onMouseOverBinding;
    public AWBinding _onMouseOutBinding;
    public AWBinding _omitTagsBinding;
    public AWBinding _titleBinding;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public boolean isStateless ()
    {
        return true;
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    protected void sleep ()
    {
        _elementId = null;
    }

    public String onClickString ()
    {
        AWEncodedString positioningElement = encodedStringValueForBinding(_positionBinding);
        if (positioningElement == null) {
            positioningElement = Constants.Null;
        }
        appendEventHandler(Constants.OnClick, PopupMenu.ShowMenuFunctionName, positioningElement);
        return null;
    }

    public String onKeyDownString ()
    {
        appendEventHandler(Constants.OnKeyDown, PopupMenu.KeyDownShowMenuFunctionName, Constants.This);
        return null;
    }

    public AWEncodedString menuId()
    {
        return PopupMenu.menuId(this, _menuIdBinding, _indexBinding);
    }

    public String position ()
    {
        // "this" is most common, so we make it the default (i.e. return null
        // so attribute is not emitted in the html
        String result = stringValueForBinding(BindingNames.position);
        if (result == null) result = "null";
        return (result.equals("this")) ? null : result;
    }

    private void appendEventHandler (AWEncodedString eventName, AWEncodedString functionName,
            AWEncodedString positioningElement)
    {
        AWResponse response = response();
        AWEncodedString menuId = PopupMenu.menuId(this, _menuIdBinding, _indexBinding);
        response.appendContent(Constants.Space);
        response.appendContent(eventName);
        response.appendContent(Constants.Equals);
        response.appendContent(Constants.Quote);
        response.appendContent(Constants.Return);
        response.appendContent(Constants.Space);
        response.appendContent(functionName);
        response.appendContent(Constants.OpenParen);
        response.appendContent(positioningElement);
        response.appendContent(Constants.Comma);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(menuId);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(Constants.Comma);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(_elementId);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(Constants.Comma);
        response.appendContent(Constants.Event);
        response.appendContent(Constants.CloseParen);
        response.appendContent(Constants.Semicolon);
        response.appendContent(Constants.Quote);
    }

    public boolean isSender ()
    {
        // We piggy back on this method to allow us to prepare the parent
        // component, but still use the regular invokeAction facility of
        // AWGenericElement when an action binding is provided.

        AWRequestContext requestContext = requestContext();
        String menuLinkSenderId = requestContext.requestSenderId();
        if (_elementId.equals(menuLinkSenderId)) {
            // If we're in here, this menu link was clicked.
            // However, we always return null at the end to allow
            // processing to continue until the PopupMenuItem is found
            // since this is where the user's action really is.
            valueForBinding(_actionSetupBinding);
            // ToDo: make this a feature of AWGenericElement somehow.  Currently do not have general support
            // for multi-action elements and still need to do this trick (ie piggy back off isSender).
            requestContext.dequeueSenderId();
        }
        return false;
    }

    public boolean omitTags ()
    {
        return requestContext().isPrintMode() || requestContext().isExportMode() || booleanValueForBinding(BindingNames.omitTags);
    }

    /*----------------------------------------------------------------------
        record and playback
    --------------------------------------------------------------------*/
    protected boolean _debugSemanticKeyInteresting()
    {
        return false;
    }
}
