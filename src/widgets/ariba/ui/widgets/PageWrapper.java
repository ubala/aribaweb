/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PageWrapper.java#30 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.util.core.StringUtil;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract public class PageWrapper extends AWComponent implements ActionInterceptor
{
        // the name of the dynamic string for the application name
    public static final String ApplicationStringName = "applicationName";

    public static final int PAGE_WRAPPER_NO_COLUMNS = 4;
    public static final String IsDialogPageKey = "widg_IsDialogPage";

    public String currentMetaKey = null;

    private final static String EnvironmentKey = "PageWrapper";

    /**
     * Static helper method to retrieve the current page wrapper which wraps the
     * component.
     *
     * @param component one of the components which is contained in the template /
     * template hierarchy of the modal page wrapper
     * @return PageWrapper
     */
    public static PageWrapper instance (AWComponent component)
    {
        return (PageWrapper)component.env().peek(EnvironmentKey);
    }

    private Map _metaTags = null;
    private List<String> _metaKeys = null;
    private String _commands;
    private String _toc;
    private String _helpKey;
    private String _windowTitle;

    protected void awake ()
    {
        _commands = UndefinedString;
        _toc = UndefinedString;
        _helpKey = UndefinedString;
    }

    protected void sleep ()
    {
        currentMetaKey = null;
        _metaTags = null;
        _metaKeys = null;
        _commands = null;
        _toc = null;
        _helpKey = null;
        _windowTitle = null;
    }

    private String helpKey ()
    {
        if (_helpKey == UndefinedString) {
            _helpKey = stringValueForBinding(BindingNames.helpKey);
        }
        return _helpKey;
    }

    public boolean enableHelpLink ()
    {
        return true;
    }

    public String commands ()
    {
        if (_commands == UndefinedString) {
            _commands = stringValueForBinding(BindingNames.commands);
            if (_commands == null) {
                _commands = "CommandBar";
            }
        }
        return _commands;
    }

    public String toc ()
    {
        if (_toc == UndefinedString) {
            _toc = stringValueForBinding(BindingNames.toc);
        }
        return _toc;
    }

    public String windowTitle ()
    {
        if (_windowTitle == null) {
            StringHandler handler =
                StringHandler.resolveHandlerInComponent(ApplicationStringName, this);

            if (handler != null) {
                _windowTitle = handler.getString(requestContext());
                if (_windowTitle == null) {
                    _windowTitle = "";
                }
            }
        }
        return _windowTitle;
    }

    public List getMetaKeys ()
    {
        _metaTags = (Map)valueForBinding("metaTags");
        if (_metaTags != null) {
            _metaKeys = new ArrayList<String>(_metaTags.keySet());
        }
        return _metaKeys;
    }

    public String getCurrentMetaValue ()
    {
        return _metaTags.get(currentMetaKey).toString();
    }

    public String debugTitle ()
    {
        String debugTitle = null;
        if (application().isDebuggingEnabled()) {
            if (AWConcreteServerApplication.IsAutomationPageTitleTestModeEnabled) {
                debugTitle = pageComponent().name();
            }
            else {
                debugTitle = stringValueForBinding(BindingNames.debugTitle);
                if (StringUtil.nullOrEmptyOrBlankString(debugTitle)) {
                    debugTitle = pageComponent().name();
                }
            }
        }
        return debugTitle;
    }


    /*---------------------------------------------------------------
         ActionInterceptor interface
    -----------------------------------------------------------------*/

    public ActionHandler overrideAction (
        String           action,
        ActionHandler    defaultHandler,
        AWRequestContext requestContext)
    {
        ActionHandler actionHandler = null;
        if (AribaAction.HelpAction.equals(action)) {
            String helpKey = helpKey();
            WidgetsDelegate delegate = Widgets.getDelegate();
            if (delegate != null) {
                actionHandler = delegate.getHelpActionHandler(requestContext, helpKey);
            }
        }
        return actionHandler;
    }

    public void renderResponse(AWRequestContext requestContext,
                                  AWComponent component)
    {
        Boolean isDialogPage = (Boolean)requestContext.get(IsDialogPageKey);

        if (isDialogPage == null) {
            if (isDialogPage() ) {
                requestContext.put(IsDialogPageKey, Boolean.TRUE);
            }
            else {
                requestContext.put(IsDialogPageKey, Boolean.FALSE);
                HttpSession session =  requestContext.existingHttpSession();
                if (session != null) {
                    WidgetsSessionState.get(session).setDialogActionInterceptor(null);
                }

            }
        }

        super.renderResponse(requestContext, component);
    }

    protected boolean isDialogPage ()
    {
        return false;
    }

}
