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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PopupMenuItem.java#26 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;

public final class PopupMenuItem extends AWComponent
{
    public static int IndentationIncrement = 10;
    public AWEncodedString _elementId;
    public boolean _disabled;
    public int _indentation;
    public AWBinding _showCheckBinding;
    public AWBinding _showBulletBinding;
    public AWBinding _disabledBinding;
    public AWBinding _indentationBinding;
    public AWBinding _submitFormBinding;
    public AWBinding _styleBinding;
    public AWBinding _actionBinding;
    public AWBinding _targetBinding;
    public AWBinding _collapsedBinding;
    protected boolean _useStandardHandler;

    // register client side js which gets called before the action binding is fired
    // usage: action                 -- default case, AW handles client side interactions
    //                                  and invokes action binding
    //        action + clientTrigger -- clientTrigger js executed on client before AW
    //                                  handling invocation of action binding
    //        onClick + onKeyPress   -- completely handled by js defined by
    //                                  onClick / onKeyPress
    public AWBinding _clientTriggerBinding;
    private AWEncodedString _frameName = AWUtil.UndefinedEncodedString;

    protected boolean useLocalPool ()
    {
        return true;
    }

    public void awake ()
    {
        _disabled = booleanValueForBinding(_disabledBinding);
        _frameName = AWUtil.UndefinedEncodedString;
    }

    protected void sleep ()
    {
        _elementId = null;
        _disabled = false;
        _indentation = 0;
        _frameName = null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _indentation = intValueForBinding(_indentationBinding) * IndentationIncrement;
        super.renderResponse(requestContext, component);
    }

    public AWResponseGenerating invokeAction ()
    {
        AWResponseGenerating actionResults = AWGenericActionTag.evaluateActionBindings(this, null, _actionBinding);
        requestContext().setFrameName(frameName());
        return actionResults;
    }

    public boolean submitForm ()
    {
        boolean submitForm = false;
        if (_submitFormBinding != null) {
            submitForm = booleanValueForBinding(_submitFormBinding);
        }
        else {
            submitForm = requestContext().currentForm() != null;
        }
        return submitForm;
    }

    public Boolean bhSubmitForm ()
    {
        // Our behavior script will default to submitting if we're in a form.
        // Only emit if our behavior differs.

        boolean submit = submitForm();
        if (submit == (requestContext().currentForm() != null)) return null;
        return (submit) ? Boolean.TRUE : Boolean.FALSE;
    }

    public AWEncodedString frameName ()
    {
        if (_frameName == AWUtil.UndefinedEncodedString) {
            _frameName = encodedStringValueForBinding(_targetBinding);
        }
        return _frameName;
    }

    protected void defaultMouseDownAction ()
    {
        AWResponse response = response();
        response.appendContent(Constants.Return);
        response.appendContent(Constants.Space);
        response.appendContent(PopupMenu.MenuClickedFunctionName);
        response.appendContent(Constants.OpenParen);
        response.appendContent(Constants.This);
        response.appendContent(Constants.Comma);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(_elementId);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(Constants.Comma);
        if (submitForm()) {
            response.appendContent(Constants.SingleQuote);
            response.appendContent(requestContext().currentForm().formName());
            response.appendContent(Constants.SingleQuote);
        }
        else {
            response.appendContent(Constants.Null);
        }
        response.appendContent(Constants.CloseParen);
        response.appendContent(Constants.Semicolon);
    }

    public String onMouseDownString ()
    {
        boolean hasOnClick = hasBinding(AWBindingNames.onClick);
        boolean hasClientTrigger = hasBinding(_clientTriggerBinding);
        if (hasOnClick || hasClientTrigger) {
            AWResponse response = response();
            response.appendContent(Constants.Space);
            response.appendContent(Constants.OnMouseDown);
            response.appendContent(Constants.Equals);
            response.appendContent(Constants.Quote);
            if (hasOnClick) {
                response.appendContent(stringValueForBinding(AWBindingNames.onClick));
                response.appendContent("return false;");
            }
            else {
                if (hasClientTrigger) {
                    response.appendContent(stringValueForBinding(_clientTriggerBinding));
                }
                defaultMouseDownAction();
            }
            response.appendContent(Constants.Quote);
        }
        return null;
    }

    protected void defaultKeyDownAction ()
    {
        AWResponse response = response();
        response.appendContent(Constants.Return);
        response.appendContent(Constants.Space);
        response.appendContent(PopupMenu.MenuKeyDownFunctionName);
        response.appendContent(Constants.OpenParen);
        response.appendContent(Constants.This);
        response.appendContent(Constants.Comma);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(_elementId);
        response.appendContent(Constants.SingleQuote);
        response.appendContent(Constants.Comma);
        if (submitForm()) {
            response.appendContent(Constants.SingleQuote);
            response.appendContent(requestContext().currentForm().formName());
            response.appendContent(Constants.SingleQuote);
        }
        else {
            response.appendContent(Constants.Null);
        }
        response.appendContent(Constants.Comma);
        response.appendContent(Constants.Event);
        response.appendContent(Constants.CloseParen);
        response.appendContent(Constants.Semicolon);
    }

    public String onKeyDownString ()
    {
        boolean hasKeyPress = hasBinding(AWBindingNames.onKeyPress);
        boolean hasClientTrigger = hasBinding(_clientTriggerBinding);
        if (hasKeyPress || hasClientTrigger) {
            AWResponse response = response();
            response.appendContent(Constants.Space);
            response.appendContent(Constants.OnKeyDown);
            response.appendContent(Constants.Equals);
            response.appendContent(Constants.Quote);
            if (hasKeyPress) {
                response.appendContent("var handle = ariba.Menu.shouldHandleMenuKeyDown(this,event); if (handle) {");
                response.appendContent(stringValueForBinding(AWBindingNames.onKeyPress));
                response.appendContent("ariba.Menu.hideActiveMenu(); return false;} return handle;");
            }
            else {
                if (hasClientTrigger) {
                    response.appendContent(stringValueForBinding(_clientTriggerBinding));
                }
                defaultKeyDownAction();
            }
            response.appendContent(Constants.Quote);
        }
        return null;
    }

    public String checkmarkFilename ()
    {
        return booleanValueForBinding(_showCheckBinding) ?
            "widg/smallCheck.gif" :
            "cleardot.gif";
    }

    public String bulletFilename ()
    {
        return booleanValueForBinding(_showBulletBinding) ?
            "widg/bullet_smblk.gif" :
            "cleardot.gif";
    }

    public String disabledClassString ()
    {
        return classString(true);
    }

    public String classString ()
    {
        return classString(false);
    }

    private String classString (boolean disabled)
    {
        AWResponse response = response();
        response.appendContent(Constants.Space);
        response.appendContent(Constants.ClassAttributeName);
        response.appendContent(Constants.Equals);
        response.appendContent(Constants.Quote);
        response.appendContent(disabled? Constants.DisabledMenuClass : Constants.MenuClass);
        if (booleanValueForBinding(_showCheckBinding)) {
            response.appendContent(Constants.Space);
            response.appendContent(Constants.Check);
        }
        if (booleanValueForBinding(_showBulletBinding)) {
            response.appendContent(Constants.Space);
            response.appendContent(Constants.Bullet);
        }
        // If item is collapsed, then style to hide it, and tell parent menu
        // that it has collapsed children (and should show expansion control
        if (booleanValueForBinding(_collapsedBinding)) {
            response.appendContent(Constants.Space);
            response.appendContent("cellColl");
            PopupMenu.flagHasCollapsedItem(this);
        }
        response.appendContent(Constants.Quote);
        return null;
    }


    // in some circumstances, actions on <a/> tags are ignored on the macintosh, and the
    // dummy link "#" is followed instead when the user clicks the link. For macintosh users,
    // we use the <div> tag for the popup menu item instead of an <a/> tag.
    public String tagName ()
    {
        return request().isMacintosh() ? "div" : "a";
    }
}
