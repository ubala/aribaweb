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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ActionHandler.java#19 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.util.core.StringUtil;

/**
    @aribaapi private
*/
public class ActionHandler extends BaseHandler
{
    private boolean _enabled = true;
    private boolean _interrupting = true;
    private String _url = null;
    private String _onClick = null;

    public static void setDefaultHandler (ActionHandler handler)
    {
        BaseHandler.setDefaultHandler(ActionHandler.class, handler);
    }

    public static void setHandler (String action, ActionHandler handler)
    {
        BaseHandler.setHandler(action, ActionHandler.class, handler);
    }

    public static ActionHandler resolveHandlerInComponent (String action, AWComponent component,
                                                           ActionHandler defaultHandler)
    {
        ActionHandler handler =
            overriddenHandlerInComponent(action, defaultHandler, component);
        prepareHandler(handler, action);
        return handler;
    }

    public static ActionHandler resolveHandlerInComponent (String action,
                                                           AWComponent component)
    {
        ActionHandler defaultHandler =
            (ActionHandler)resolveHandler(component, action, ActionHandler.class);
        ActionHandler handler =
            overriddenHandlerInComponent(action, defaultHandler, component);
        prepareHandler(handler, action);
        return handler;
    }

    public static ActionHandler resolveHandler (String action)
    {
        return (ActionHandler)resolveHandler(action, ActionHandler.class);
    }

    public ActionHandler ()
    {
        this(true, true, null);
    }

    public ActionHandler (boolean isEnabled, boolean isInterrupting, String url)
    {
        super();
        _enabled = isEnabled;
        _interrupting = isInterrupting;
        _url = url;
    }

    public boolean isEnabled (AWRequestContext requestContext)
    {
        return _enabled;
    }

    // indicates whether or not the action is an external action.  If so, then the
    // AribaAction is expected to treat URL's, etc. as external actions.
    public boolean isExternal (AWRequestContext requestContext)
    {
        return false;
    }

    public boolean isInterrupting (AWRequestContext requestContext)
    {
        String name = name();
        if (AribaAction.HelpAction.equals(name) ||
            AribaAction.SupportAction.equals(name)) {
            return false;
        }
        return _interrupting;
    }

    public String target (AWRequestContext requestContext)
    {
        if (AribaAction.HelpAction.equals(name())) {
            return AribaAction.HelpAction;
        }
        if (AribaAction.SupportAction.equals(name())) {
            return AribaAction.SupportAction;
        }
        return null;
    }

    public String windowAttributes (AWRequestContext requestContext)
    {
        if (AribaAction.HelpAction.equals(name())) {
            return "toolbar=yes,addressbar=no,scrollbars=yes," +
                   "resizable=yes,width=668, height=400";
        }
        return null;
    }
    
    // Wrapper handler (ARBWizardActionHandler) uses this function to return
    // the underline ActionHanlder
    protected ActionHandler realHandler ()
    {
        return this;
    }
    
    //////////////////////////////
    // The following two methods are only invoked for non-component action
    // based Action
    /////////////////////////////////
    public String url (AWRequestContext requestContext)
    {
        return _url;
    }

    public String onClick (AWRequestContext requestContext)
    {
        return _onClick;
    }

    public boolean submitFormToUrl ()
    {
        return true;
    }

    public boolean submitFormToComponentAction ()
    {
        return true;
    }

    public boolean useComponentAction (AWRequestContext requestContext)
    {
        return true;
    }

    public AWResponseGenerating actionClicked (AWRequestContext requestContext)
    {
        String url = url(requestContext);
        if (!StringUtil.nullOrEmptyString(url)) {
            AWRedirect redirect =
                (AWRedirect)requestContext.pageWithName(AWRedirect.class.getName());
            redirect.setUrl(url);
            return redirect;
        }
        return null;
    }

    private static ActionHandler overriddenHandlerInComponent (
                                                        String action,
                                                        ActionHandler defaultHandler,
                                                        AWComponent component)
    {
        AWRequestContext requestContext = component.requestContext();

        // Walk up the parent chain
        for (AWComponent parent = component.parent(); parent != null;
                                                        parent = parent.parent()) {
            if (parent instanceof ActionInterceptor) {
                ActionHandler override =
                    ((ActionInterceptor)parent).overrideAction(
                                            action, defaultHandler, requestContext);
                if (override != null && override != defaultHandler) {
                    return override;
                }
            }
        }
        // Try to the application object
        AWApplication application = component.application();
        if (application instanceof ActionInterceptor) {
            ActionHandler override =
                ((ActionInterceptor)application).overrideAction(
                                            action, defaultHandler, requestContext);
            if (override != null && override != defaultHandler) {
                return override;
            }
        }
        return defaultHandler;
    }
}
