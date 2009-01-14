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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/Application.java#63 $
*/

package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentConfigurationSource;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWSessionValidator;
import ariba.ui.aribaweb.core.AWLocalLoginSessionHandler;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.servletadaptor.AWServletApplication;
import ariba.ui.servletadaptor.AWServletResourceManager;
import ariba.ui.widgets.TopFrameRedirect;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.widgets.AribaAction;
import ariba.ui.widgets.StringHandler;
import ariba.ui.widgets.Widgets;
import ariba.ui.widgets.WidgetsDelegate;
import ariba.ui.widgets.ConditionHandler;
import ariba.ui.widgets.BindingNames;
import ariba.ui.dev.AWDebugOptions;
import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.URLUtil;
import ariba.util.core.ClassUtil;
import ariba.util.core.ResourceService;
import ariba.util.fieldtype.SafeJavaRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TimeZone;

public class Application extends AWServletApplication
{
    //////////////////
    // User Defaults
    //////////////////
    public boolean isDebuggingEnabled() {
        return true;
    }

    public void init ()
    {
        super.init();

        // Note: We don't want AribaUI -- we're doing it just to get RichText...
        // Initialize AribaUI if it's here...
        String AribaUIClass = "ariba.ui.aribaui.AribaUI";
        if (ClassUtil.classForName(AribaUIClass, false) != null) {
            ClassUtil.invokeStaticMethod(AribaUIClass, "initialize",
                new Class[]{},
                new Object[]{});
        }

        // handle default app actions
        Widgets.initialize();

        // Force initialization now so that resources are registered during init
        DemoShell.sharedInstance();

        // Enable use of BCEL compiled bindings
        // CompiledAccessorFactory.setAccessThresholdToCompile(1);

        // expr language: don't load safe initialization files
        SafeJavaRepository.getInstance(false);

        ActionHandler.setHandler("preferences",
                                 new GotoPageActionHandler(AWDebugOptions.class.getName()));
        ConditionHandler.setHandler(BindingNames.preferencesEnabled, new ConditionHandler() {
            public boolean evaluateCondition(AWRequestContext requestContext) { return true; }
        });
                
        ActionHandler.setHandler(AribaAction.LogoutAction,
                                 new LogoutActionHandler());
        ActionHandler.setHandler("home",
                                 new GotoPageActionHandler(mainPageName()));
        ActionHandler.setHandler("cancelPunchout",
                                 new CancelPunchOutActionHandler());
        ActionHandler.setHandler("toggleTips",
                                 new GotoPageActionHandler(null));
        ActionHandler.setHandler("toggleCurrency",
                                 new GotoPageActionHandler(null));
        ActionHandler.setHandler("help",
                                 new ActionHandler(true, false, "http://localhost"));

        StringHandler.setDefaultHandler(new DefaultStringHandler());
        //AWMultiLocaleResourceManager.enableFailedResourceLookupLogging();
        AWConcreteServerApplication.IsAutomationTestModeEnabled = false;

        ariba.ui.aribaweb.util.Log.aribaweb.setLevel(ariba.util.log.Log.DebugLevel);
        ariba.ui.aribaweb.util.Log.domsync.setLevel(ariba.util.log.Log.DebugLevel);
        // ariba.ui.aribaweb.util.Log.aribaweb_request.setLevel(ariba.util.log.Log.DebugLevel);

        // ariba.ui.aribaweb.util.Log.domsync.setLevel(ariba.util.log.Log.DebugLevel);
        // ariba.ui.aribaweb.util.Log.servletadaptor.setDebugOn(true);
        ariba.ui.widgets.Log.widgets.setLevel(ariba.util.log.Log.DebugLevel);
        // ariba.ui.demoshell.Log.util.setLevel(ariba.util.log.Log.DebugLevel);

        Log.demoshell.setLevel(ariba.util.log.Log.DebugLevel);
        ariba.ui.meta.core.Log.meta.setLevel(ariba.util.log.Log.DebugLevel);
        ariba.ui.meta.core.Log.meta_detail.setLevel(ariba.util.log.Log.DebugLevel);

        Log.demoshell.debug("******** Demoshell Logging!! **********");
    }

    public void initSessionValidator ()
    {
        if (_sessionValidator == null) setSessionValidator(new AWLocalLoginSessionHandler() {
            protected AWResponseGenerating showLoginPage (AWRequestContext requestContext,
                                                          CompletionCallback callback)
            {
                LoginPage loginPage = (LoginPage)requestContext.pageWithName(LoginPage.class.getName());
                loginPage.init(callback);
                return loginPage;
            }
        });
    }

    // we override to return an alternate start page
    // if we're running this as a shell
    public AWResponseGenerating mainPage (AWRequestContext requestContext)
    {
        AWResponseGenerating page = DemoShell.sharedInstance().mainPage(requestContext);
        return (page != null) ? page :  super.mainPage(requestContext);
    }

    public String mainPageName ()
    {
        return "MetaHomePage";
    }

    protected void terminateCurrentSession (AWRequest request)
    {
        // force the timeout of our old session so we don't use it
        AWRequestContext requestContext = this.createRequestContext(request);
        AWSession session = requestContext.session(false);
        if (session != null) {
            // session.terminate();
            session.clearAllPageCaches();

            // this is spooky -- if we don't do this we get a nested checkout exception.  Bogus!
            this.checkinHttpSessionId(request.sessionId());
        }
    }

    /**
     * For URLs of the form "http://localhost/Ariba/FileName.htm" we want to treat
     * FileName.htm as the file to use, not as a (bad) request handler key
     */
    public AWResponse handleMalformedRequest (AWRequest request, String message)
    {
        // always a new session if you hit the front door
        terminateCurrentSession(request);

        // now handle the request with a new session
        request.requestHandlerKey();
        return requestHandlerForKey(AWConcreteApplication.ComponentActionRequestHandlerKey).handleRequest(request);
    }

    public AWSession createSession (AWRequestContext requestContext)
    {
        Session session = new Session();
        session.init(this, requestContext);
        session.setClientTimeZone(TimeZone.getDefault());        
        return session;
    }

    public boolean initIsRapidTurnaroundEnabled ()
    {
        return true;
    }

    protected boolean initDirectConnectEnabled ()
    {
        return false;
    }

    public static class CancelPunchOutActionHandler extends ActionHandler
    {
        public AWResponseGenerating actionClicked (AWRequestContext requestContext)
        {
            TopFrameRedirect redirect = (TopFrameRedirect)
                requestContext.pageWithName(TopFrameRedirect.class.getName());

            redirect.setNextPage((AWComponent)requestContext.application().mainPage(requestContext));
            return redirect;
        }
    }

    public static class GotoPageActionHandler extends ActionHandler
    {
        private String _pageName;

        public GotoPageActionHandler (String pageName)
        {
            _pageName = pageName;
        }

        public AWResponseGenerating actionClicked (AWRequestContext requestContext)
        {
            if (_pageName == null) {
                return null;
            }
            AWResponseGenerating result = requestContext.pageWithName(_pageName);
            if (result == null) {
                result = DemoShell.sharedInstance().mainPage(requestContext);
            }
            return result;
        }
    }

    public static class DefaultStringHandler extends StringHandler
    {
        private static final Map Strings = MapUtil.map();
        static {
            Strings.put("applicationName", "AribaWeb Demonstration");
            Strings.put("userGreeting", "Welcome Valued User");
            Strings.put(Home,Home);
            Strings.put(Help,Help);
            Strings.put(Logout,Logout);
            Strings.put(Preferences,Preferences);
        }

        public String getString (AWRequestContext requestContext)
        {
            // really these would be localized based on requestContext.session
            return (String)Strings.get(this.name());
        }
    }


    private String m_applicationUrl;
    public String applicationUrl (AWRequest request)
    {
        if (m_applicationUrl == null) {
            m_applicationUrl = Fmt.S("%s/%s", servletUrlPrefix(),
                                     "Ariba");
        }
        return m_applicationUrl;
    }

    /*
        Implement DataTable configuration storage
     */
    public AWComponentConfigurationSource getComponentConfigurationSource (Class componentClass)
    {
        AWComponentConfigurationSource source = super.getComponentConfigurationSource(componentClass);
        if (source == null) {
            source = new GlobalConfigSource();
            registerComponentConfigurationSource(componentClass, source);
        }
        return source;
    }

    protected static class GlobalConfigSource implements AWComponentConfigurationSource
    {
        Map _configs = MapUtil.map();

        public Object loadConfiguration(String configurationName) {
            return _configs.get(configurationName);  //To change body of implemented methods use File | Settings | File Templates.
        }

        public  void saveConfigurations (Map configurationsByName) {
            // Horrible way to do a deep clone... (Because AWPage does a deep destruction of the underlying maps
            MapUtil.fromSerializedString(_configs, MapUtil.toSerializedString(configurationsByName));
        }        
    }
}
