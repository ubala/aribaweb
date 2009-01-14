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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaAction.java#35 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;

public class AribaAction extends AWComponent
{
    public final static String HomeAction       = "home";
    public final static String LogoutAction     = "logout";
    public final static String SupportAction    = "support";
    public final static String HelpAction       = "help";
    public final static String SiteAction       = "site";
    public static final String GlobalNavAction  = "globalNav";
    public static final String PreferencesAction = "preferences";

    private String _actionName;
    private ActionHandler _handler;
    public boolean _submitForm;

    protected void sleep ()
    {
        _handler = null;
        _actionName = null;
        _submitForm = false;
    }

    public String url ()
    {
        ActionHandler handler = handler();
        return handler == null ? null : handler.url(requestContext());
    }

    public String effectiveUrl ()
    {
        return _submitForm || (handler().target(requestContext()) != null ||
               url() == null) ? "javascript:void(0);" : url();
    }

    public String onClick ()
    {
        AWResponse response = response();
        ActionHandler handler = handler();
        AWRequestContext requestContext = requestContext();
        String target = handler.target(requestContext);
        response.appendContent(Constants.Space);
        response.appendContent(Constants.OnClick);
        response.appendContent(Constants.Equals);
        response.appendContent(Constants.Quote);
        if (handler.isExternal(requestContext) &&
            handler.onClick(requestContext) != null) {
            response.appendContent(handler.onClick(requestContext));
        }
        else if (_submitForm && target == null && handler.submitFormToUrl()) {
            // This is the case where we are provided a url (eg from jsp) and are within
            // a form.  We do not support submitting a form and opening a new window.
            // If the target is provided, we will simply do a GET and display results
            // in that window.
            response.appendContent("document.forms[0].action='");
            response.appendContent(url());
            response.appendContent("';ariba.Request.submitForm(document.forms[0]);return false");
        }
        else {
            if (target != null) {
                response.appendContent(Constants.Return);
                response.appendContent(Constants.Space);
                response.appendContent(Constants.OpenWindow);
                response.appendContent(Constants.OpenParen);

                response.appendContent(Constants.SingleQuote);
                response.appendContent(url());
                response.appendContent(Constants.SingleQuote);
                response.appendContent(Constants.Comma);
                response.appendContent(Constants.Space);

                response.appendContent(Constants.SingleQuote);
                response.appendContent(target);
                response.appendContent(Constants.SingleQuote);
                response.appendContent(Constants.Comma);
                response.appendContent(Constants.Space);

                response.appendContent(Constants.SingleQuote);
                response.appendContent(handler.windowAttributes(requestContext));
                response.appendContent(Constants.SingleQuote);

                response.appendContent(Constants.CloseParen);
            }
            else {
                response.appendContent(Constants.Return);
                response.appendContent(Constants.Space);
                response.appendContent("ariba.Request.setDocumentLocation");
                response.appendContent(Constants.OpenParen);

                response.appendContent(Constants.SingleQuote);
                response.appendContent(url());
                response.appendContent(Constants.SingleQuote);
                response.appendContent(Constants.Comma);
                response.appendContent(Constants.Space);

                response.appendContent(Constants.Null);

                response.appendContent(Constants.CloseParen);
            }
        }
        response.appendContent(Constants.Semicolon);
        response.appendContent(Constants.Quote);
        return null;
    }

    protected void awake ()
    {
        _submitForm = (requestContext().currentForm() != null ||
                requestContext().get(PageWrapperForm.CurrentHtmlFormKey) != null) &&
                target() == null &&
                (handler() == null || !handler().useComponentAction(requestContext()) || handler().submitFormToComponentAction());
    }

    public AWResponseGenerating actionClicked ()
    {
        ActionHandler handler = handler();
        return handler == null ? null : handler.actionClicked(requestContext());
    }

    public boolean useHyperlink ()
    {
        ActionHandler handler = handler();
        return (url() != null ||
                (handler != null && handler.onClick(requestContext()) != null));
    }

    public boolean useComponentAction ()
    {
        ActionHandler handler = handler();
        return handler != null && handler.useComponentAction(requestContext());
    }

    public boolean isActionDisabled ()
    {
        ActionHandler handler = handler();
        return handler == null ? true : !handler.isEnabled(requestContext());
    }

    public String target ()
    {
        String targetString = null;
        ActionHandler handler = handler();
        if (handler != null) {
            AWRequestContext requestContext = requestContext();
            targetString = handler.target(requestContext);
        }
        return targetString;
    }

    public String actionName ()
    {
        if (_actionName == null) {
            _actionName = getActionName();
            Assert.that(!StringUtil.nullOrEmptyOrBlankString(_actionName),
                            "action name not specified");
        }
        return _actionName;
    }

    public String getActionName ()
    {
        String actionName = stringValueForBinding(BindingNames.name);
        StringHandler handler = StringHandler.resolveHandlerInComponent(actionName,
                                                                        this.parent());
        if (handler != null) {
            String actionNamedByKey = handler.getString(requestContext());
            if (actionNamedByKey != null) {
                actionName = actionNamedByKey;
            }
        }
        return actionName;
    }

    public boolean isToggleSidebar ()
    {
        return actionName().equals(ToggleSidebarActionHandler.ToggleSideBarActionName);
    }

    private ActionHandler handler ()
    {
        if (_handler == null) {
            _handler = ActionHandler.resolveHandlerInComponent(actionName(), this);
        }
        return _handler;
    }

    protected boolean _debugSemanticKeyInteresting ()
    {
        return true;
    }

    protected String _debugSemanticKey ()
    {
        return StringUtil.strcat(componentDefinition().componentName(), ":",
                                 actionName());
    }
}
