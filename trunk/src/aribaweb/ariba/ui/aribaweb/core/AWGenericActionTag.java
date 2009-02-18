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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWGenericActionTag.java#22 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.html.BindingNames;
import ariba.ui.aribaweb.util.AWEncodedString;

//  This does not handle target properly.  See AWHyperlink for how it adds the frameName to the request context, thus setting up the appropriate page cache.

public final class AWGenericActionTag extends AWComponent
{
    private static final String[] SupportedBindingNames = {AWBindingNames.tagName,
        AWBindingNames.action, AWBindingNames.tabIndex, AWBindingNames.disabled,
        AWBindingNames.submitForm, AWBindingNames.disableClick, AWBindingNames.disableKeyPress,
        AWBindingNames.omitTags, AWBindingNames.target, BindingNames.windowAttributes,
        AWBindingNames.onKeyPress, AWBindingNames.onClick,
        AWBindingNames.name,
        AWBindingNames.formValue, AWBindingNames.isTrigger, AWBindingNames.bh};

    public AWEncodedString _elementId;
    private boolean _isDisabled;
    public AWBinding _tagNameBinding;
    public AWBinding _actionBinding;
    public AWBinding _tabIndexBinding;
    public AWBinding _omitTagsBinding;
    public AWBinding _disabledBinding;
    public AWBinding _submitFormBinding;
    public AWBinding _targetBinding;
    public AWBinding _windowAttributesBinding;
    public AWBinding _disableClickBinding;
    public AWBinding _disableKeyPressBinding;
    public AWBinding _nameBinding;
    public AWBinding _formValueBinding;
    public AWBinding _isTriggerBinding;

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
        _isDisabled = false;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _isDisabled = booleanValueForBinding(_disabledBinding);
        super.renderResponse(requestContext, component);
    }

    public Boolean bhSubmitForm ()
    {
        boolean submitForm =  submitForm(requestContext());
        return submitForm ? null : Boolean.FALSE;
    }
    
    private boolean submitForm (AWRequestContext requestContext)
    {
        return hasBinding(_submitFormBinding) ?
            booleanValueForBinding(_submitFormBinding) :
            requestContext.currentForm() != null;
    }

    public static AWEncodedString appendEventHandler (AWRequestContext requestContext, AWEncodedString eventName, AWEncodedString eventHandlerName,
                                                      AWEncodedString formName, AWEncodedString targetName, AWEncodedString actionName)
    {
        return appendEventHandler(requestContext, eventName, eventHandlerName,
                                  formName, targetName, null, actionName, false);
    }

    public static AWEncodedString appendEventHandler (AWRequestContext requestContext, AWEncodedString eventName, AWEncodedString eventHandlerName,
                                                      AWEncodedString formName, AWEncodedString targetName, AWEncodedString actionName, boolean submitValue)
    {
        return appendEventHandler(requestContext, eventName, eventHandlerName,
                                  formName, targetName, null, actionName, submitValue);
    }

    public static AWEncodedString appendEventHandler (AWRequestContext requestContext, AWEncodedString eventName, AWEncodedString eventHandlerName,
                                                      AWEncodedString formName, AWEncodedString targetName, AWEncodedString windowAttributes,
                                                      AWEncodedString actionName)
    {
        return appendEventHandler(requestContext, eventName, eventHandlerName,
                                  formName, targetName, windowAttributes, actionName, false);
    }

    public static AWEncodedString appendEventHandler (AWRequestContext requestContext, AWEncodedString eventName, AWEncodedString eventHandlerName,
                                                      AWEncodedString formName, AWEncodedString targetName, AWEncodedString windowAttributes,
                                                      AWEncodedString actionName, boolean submitValue)
    {
        AWResponse response = requestContext.response();
        response.appendContent(AWConstants.Space);
        response.appendContent(eventName);
        response.appendContent(AWConstants.Equals);
        response.appendContent(AWConstants.Quote);
        response.appendContent(AWConstants.Return);
        response.appendContent(AWConstants.Space);
        response.appendContent(eventHandlerName);
        response.appendContent(AWConstants.OpenParen);
        response.appendContent(AWConstants.This);
        appendStringArgument(response, formName);
        appendStringArgument(response, targetName);
        appendStringArgument(response, actionName);
        response.appendContent(AWConstants.Comma);
        response.appendContent(AWConstants.Event);
        appendBooleanArgument(response, submitValue);
        appendStringArgument(response, windowAttributes);
        response.appendContent(AWConstants.CloseParen);
        response.appendContent(AWConstants.Semicolon);
        response.appendContent(AWConstants.Quote);

        return null;
    }

    public static void appendStringArgument (AWResponse response, AWEncodedString string)
    {
        response.appendContent(AWConstants.Comma);
        if (string != null) {
            response.appendContent(AWConstants.SingleQuote);
            response.appendContent(string);
            response.appendContent(AWConstants.SingleQuote);
        }
        else {
            response.appendContent(AWConstants.Null);
        }
    }

    public static void appendBooleanArgument (AWResponse response, boolean bool)
    {
        response.appendContent(AWConstants.Comma);
        if (bool) {
            response.appendContent(AWConstants.True);
        }
        else {
            response.appendContent(AWConstants.False);
        }
    }

    public String disabledString ()
    {
        return _isDisabled ? AWBindingNames.awstandalone : null;
    }

    public boolean isTrigger ()
    {
       return hasBinding(_isTriggerBinding) && booleanValueForBinding(_isTriggerBinding);
    }

    public boolean isSender ()
    {
        boolean isSender = false;
        AWRequestContext requestContext = requestContext();
        if (submitForm(requestContext)) {
            isSender = AWGenericActionTag.isHiddenFieldSender(requestContext.request(), _elementId);
        }
        else {
            String senderId = requestContext.requestSenderId();
            isSender = _elementId.equals(senderId);
        }
        // The following code might better be placed in an "invokeAction" method in here, but that doesn't exist, so I'm placing it this method since its called before any invokeAction would have been called.
        if (isSender) {
            requestContext().setFrameName(encodedStringValueForBinding(_targetBinding));
        }
        return isSender;
    }

    public static boolean isHiddenFieldSender (AWRequest request, AWEncodedString elementId)
    {
        return elementId.equals(AWGenericActionTag.hiddenFieldSenderId(request));
    }

    public static String hiddenFieldSenderId (AWRequest request)
    {
        String hiddenFieldSenderId = request.formValueForKey(AWComponentActionRequestHandler.SenderKey);
        return "".equals(hiddenFieldSenderId) ? null : hiddenFieldSenderId;
    }

    public boolean isBrowserNetscape ()
    {
        return (!requestContext().isBrowserMicrosoft() &&
                !"a".equals(stringValueForBinding(_tagNameBinding)));
    }

    public String staticUrl ()
    {
        return (requestContext().isStaticGeneration())
                ? requestContext().staticUrlForActionResults(evaluateActionBindings(this))
                : null;
    }

    ///////////////////
    // Utility Methods
    ///////////////////
    public static AWResponseGenerating evaluateActionBindings (AWComponent component, AWBinding pageNameBinding, AWBinding actionBinding)
    {
        AWResponseGenerating results = null;
        // Note: do not test for hasBinding(pageNameBinding) here since we can only have one of pageName or action.
        // If the pageName binding is defined at all, the action binding should not be defined.  If the pageName
        // binding is a $^ binding and its not bound somewhere, then this will cycle the page.
        if (pageNameBinding != null) {
            String pageName = component.stringValueForBinding(pageNameBinding);
            if (pageName != null) {
                results = component.pageWithName(pageName);
            }
        }
        else {
            results = (AWResponseGenerating)component.valueForBinding(actionBinding);
        }
        return results;
    }

    public static AWResponseGenerating evaluateActionBindings (AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        AWBinding actionBinding = component.bindingForName(AWBindingNames.action);
        if (actionBinding != null) {
            actionResults = (AWResponseGenerating)component.valueForBinding(actionBinding);
        }
        else {
            String pageName = component.stringValueForBinding(AWBindingNames.pageName);
            actionResults = component.pageWithName(pageName);
        }
        return actionResults;
    }
}
