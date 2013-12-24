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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWConcreteApplication.java#141 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.html.AWMultiTabException;
import ariba.ui.aribaweb.util.AWBookmarker;
import ariba.ui.aribaweb.util.AWBrand;
import ariba.ui.aribaweb.util.AWBrandManager;
import ariba.ui.aribaweb.util.AWCheckoutManager;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWNodeManager;
import ariba.ui.aribaweb.util.AWNodeValidator;
import ariba.ui.aribaweb.util.AWParameters;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWStaticSiteGenerator;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.HTML;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.NamedValue;
import ariba.util.core.PerformanceState;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.fieldvalue.OrderedList;
import ariba.util.http.multitab.MaximumTabExceededException;
import ariba.util.io.CSVConsumer;
import ariba.util.io.CSVReader;
import ariba.util.shutdown.ShutdownDelayer;
import ariba.util.shutdown.ShutdownManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.http.HttpSession;

abstract public class AWConcreteApplication
    extends AWConcreteServerApplication
    implements AWApplication, ShutdownDelayer
{
    private static final String SessionClassName = "Session";
    protected static final String RequestContextClassName = "RequestContext";
    public static final String RetryKey = "awretry";
    public static final String ComponentActionRequestHandlerKey = "aw";
    public static final String DirectActionRequestHandlerKey = "ad";
    public static boolean IsActionLoggingEnabled = false;
    public static boolean IsRequestLoggingEnabled = false;
    public static boolean IsResponseLoggingEnabled = false;
    public static boolean IsDirectConnectEnabled = false;
    public static boolean IsCookieSessionTrackingEnabled = false;
    public static int RemoteHostMask = 0;
    private static Class SessionClass;
    private static Class RequestContextClass;
    private static final String ClassComment = "<!-- class:";
    private static final String ClassCommentAlt = "<!--- class:";
    public static Class DefaultComponentClass = AWComponent.ClassObject;
    // the optional name of the application
    private static String ApplicationType = null;

    private int _pageCacheSize = 15;
    private GrowOnlyHashtable _componentDefinitionHashtable;
    private AWCheckoutManager _httpSessionCheckoutManager;
    private int _sessionTimeout;
    private boolean _refusingNewSessions = false;
    private String _refuseNewSessionsPassword = AWDirectAction.PasswordKey;
    private String _terminateApplicationPassword = AWDirectAction.PasswordKey;
    private AWMonitorStats _monitorStats;
    private AWBookmarker _bookmarker;
    protected String _adaptorUrl;
    protected String _adaptorUrlSecure;
    private boolean _useEmbeddedKeyPathes = true;
    AWBrandManager _brandManager;
    private GrowOnlyHashtable _componentConfigurationSources;
    private AWParameters _configParameters;
    private int _pollInterval;
    private static AWStaticSiteGenerator _Staticizer;

    public static final String DefaultAWLStringsSuffix = ".strings";
    public static final String DefaultJavaStringsSuffix = ".jstrings";
    public static final String DefaultPackageStringsSuffix = ".csv";

    // package level flags
    public static final int TemplateValidationFlag    = 0x00000001;
    public static final int StrictTagNamingFlag       = 0x00000002;
    public static final int AllowGlidEnhancementsFlag = 0x00000004;

    // validation
    public static boolean IsTemplateValidationDefaultVisible  = true;

    // sso
    protected AWSessionValidator _sessionValidator;

    // node management
    protected AWNodeManager _nodeManager;

    // session timeout management
    public static boolean IsSessionManagementEnabled = false;

    // notification
    public static boolean IsNotificationEnabled = false;

    // session status
    private AWSessionStatusManager _sessionStatusManager;
    private AWSessionMonitor _sessionMonitor;
    boolean _didCompleteInit;

    // ** Thread Safety Considerations: We need to serialize access to the
    // _componentDefinitionHashtable.
    // The sessionStore offers its own thread safety as does the timeout manager.

    public static AWApplication defaultApplication ()
    {
        AWApplication defaultApplication =
                (AWApplication)AWConcreteServerApplication.sharedInstance();
        if (defaultApplication == null) {
            defaultApplication =
                    createApplication(AWDefaultApplication.class.getName(),
                    AWDefaultApplication.class);
        }
        return defaultApplication;
    }

    public static AWApplication createApplication (String applicationClassName,
                                                   Class defaultClass)
    {
        Class applicationClass = AWUtil.classForName(applicationClassName);
        if (applicationClass == null) {
            applicationClass = defaultClass;
        }
        AWConcreteApplication application = (AWConcreteApplication)SharedInstance;
        if (application == null) {
            try {
                application = (AWConcreteApplication)applicationClass.newInstance();
            }
            catch (InstantiationException instantiationException) {
                throw new AWGenericException("Unable to create instance of class: \"" +
                                             applicationClass.getName() + "\"");
            }
            catch (IllegalAccessException illegalAccessException) {
                throw new AWGenericException(illegalAccessException);
            }
            application.init();
        }

        if (!application._didCompleteInit) {
            for (DidInitCallback cb : application._PostInitCallbacks) {
                cb.applicationDidInit(application);
            }
            application._PostInitCallbacks = null;
            application._didCompleteInit = true;
        }

        application.awake();

        return application;
    }

    /**
     * Gets the module name of the current running module.
     * @return the module name.
     */
    public String getApplicationType ()
    {
        return ApplicationType;
    }

    /**
     * Sets the module name of the current running module.
     */
    public static void registerApplicationType (String applicationType)
    {
        ApplicationType = applicationType;
    }

    public interface DidInitCallback {
        void applicationDidInit (AWConcreteApplication application);
    }

    List<DidInitCallback> _PostInitCallbacks = ListUtil.list();

    public boolean didCompleteInit ()
    {
        return _didCompleteInit;
    }

    public void registerDidInitCallback (DidInitCallback callback)
    {
        if (_didCompleteInit) {
            callback.applicationDidInit(this);
        }
        else {
            _PostInitCallbacks.add(callback);
        }
    }

    abstract protected String initAdaptorUrl ();
    abstract protected String initAdaptorUrlSecure ();

    protected boolean initActionLoggingEnabled ()
    {
        return false;
    }

    protected boolean initRequestLoggingEnabled ()
    {
        return false;
    }

    protected boolean initResponseLoggingEnabled ()
    {
        return false;
    }

    public boolean isValidRemoteHost (AWRequest request)
    {
        return false;
    }

    /**
     * @deprecated No longer an optional setting.
     */
    protected boolean isRefreshRegionEnabled ()
    {
        return true;
    }

    protected boolean initDirectConnectEnabled ()
    {
        return false;
    }

    /**
     * Provide the option of disabling/enabling user community functionality.
     * Products may over-ride this to turn on or off the user community functionality
     * by using their own configuration.
     */
    protected boolean initUserCommunityEnabled ()
    {
        return false;
    }

    /**
     * Get the user user community product url.
     * This needs to be configured in product configs
     * @return String
     */

    protected String initAribaUserCommunityUrl ()
    {
        return null;
    }

    /**
     * Default value of the window size (in pixels) at which
     * user community In Situ Pane will be folded.
     * Products can override this value.
     * @return int
     */
    protected int initFoldInSituOnWindowSize()
    {
        return 1280;
    }

    protected int initRemoteHostMask ()
    {
        return 0xffff0000;
    }

    protected boolean initCookieSessionTrackingEnabled ()
    {
        return true;
    }

    protected boolean initElementIdMismatchDebuggingEnabled ()
    {
        return false;
    }

    public void initRequestHandlers ()
    {
        AWComponentActionRequestHandler componentActionRequestHandler =
                new AWComponentActionRequestHandler();
        componentActionRequestHandler.init(this, ComponentActionRequestHandlerKey);
        registerRequestHandlerForKey(componentActionRequestHandler,
                ComponentActionRequestHandlerKey);
        setDefaultRequestHandler(componentActionRequestHandler);

        AWDirectActionRequestHandler directActionRequestHandler =
                new AWDirectActionRequestHandler();
        directActionRequestHandler.init(this, DirectActionRequestHandlerKey);
        registerRequestHandlerForKey(directActionRequestHandler,
                DirectActionRequestHandlerKey);
    }

    private void initStandardClasses ()
    {
        SessionClass = AWUtil.classForName(SessionClassName);
        if (SessionClass == null) {
            SessionClass = AWSession.class;
        }
        RequestContextClass = AWUtil.classForName(RequestContextClassName);
        if (RequestContextClass == null) {
            RequestContextClass = AWRequestContext.class;
        }
    }

    protected Class sessionClass ()
    {
        return SessionClass;
    }

    /**
        Ariba Web framework by default does not allow application to
        to add session time and leave it to app server to do that
        but if the application feels the urge to do this, they can
        do this by overriding the sessionTimeout() method
        --adas
    */
    protected int initSessionTimeout ()
    {
        return -1;

    }

    public void init ()
    {
        super.init();
        // force registration or orderedList class extension
        Log.aribaweb.debug("AWConcreteApplication.valueUnbound() called " +
                "with session timeout %d", initSessionTimeout());
        // Touch the AWOrderedList class to force it to initialize.
        OrderedList.class.getName();
        _componentDefinitionHashtable = new GrowOnlyHashtable();
        _httpSessionCheckoutManager = createHttpSessionCheckoutManager();
        _sessionTimeout = initSessionTimeout();
        initStandardClasses();
        IsActionLoggingEnabled = initActionLoggingEnabled();
        IsRequestLoggingEnabled = initRequestLoggingEnabled();
        IsResponseLoggingEnabled = initResponseLoggingEnabled();
        IsDirectConnectEnabled = initDirectConnectEnabled();
        IsUserCommunityEnabled = initUserCommunityEnabled();
        IsCookieSessionTrackingEnabled = initCookieSessionTrackingEnabled();
        RemoteHostMask = initRemoteHostMask();
        _monitorStats = createMonitorStats();
        _bookmarker = createBookmarker();
        _adaptorUrl = initAdaptorUrl();
        _aribaUserCommunityUrl = initAribaUserCommunityUrl();
        _foldInSituOnWindowSize = initFoldInSituOnWindowSize();
        _adaptorUrlSecure = initAdaptorUrlSecure();
        _brandManager = initBrandManager();
        _nodeManager = initNodeManager();
        initNodeValidators();
        _componentConfigurationSources = new GrowOnlyHashtable();
        _configParameters = initConfigParameters();
        _pollInterval = initPollInterval();
        //force init of AWTdContainer (registers td with template parser)
        String className = "ariba.ui.aribaweb.html.AWTdContainer";
        Assert.that(AWUtil.classForName(className) != null,
                "Unable to load %s", className);
        // Load the safe HTML tags and attributes definition
        loadSafeHtmlConfig();

        AWComponent.initTemplateResourceManager(createTemplateResourceManager());
        
        ShutdownManager.addShutdownDelayer(this);
        initSessionMonitor();
        initCommunityContext();
    }

    private static class ContentCollector implements CSVConsumer
    {
        private List result = ListUtil.list();

        public void consumeLineOfTokens (String path, int lineNumber, List line)
        {
            Iterator it = line.listIterator();
            while (it.hasNext()) {
                String tmp = ((String)it.next()).trim().toLowerCase();
                if (tmp.length() > 0) {
                    result.add(tmp);
                }
            }
        }

        private String[] getResult ()
        {
            return (String[])result.toArray(new String[result.size()]);
        }
    }
    /**
       read a csv file, and split the contents into an array of String.
       These Strings are trimmed and converted into lower case.
    */
    private static String[] readFileToStrings (URL url)
    {
        ContentCollector collector = new ContentCollector();
        CSVReader reader = new CSVReader(collector);
        try {
            reader.read(url, "8859_1");
        }
        catch (IOException ioe) {
            Log.aribaweb.warn(Fmt.S("Failed to read from %s", url));
            return null;
        }
        return collector.getResult();
    }

    public static final String SAFE_TAGS_FILE = "HtmlSafeTags.csv";
    public static final String SAFE_ATTRS_FILE = "HtmlSafeAttributes.csv";

    /**
       load safe tags and safe attributes definitions
      */

    private static URL urlForResource (String relativePath, String fileName)
    {
        File rootDir = SystemUtil.getSystemDirectory();
        File dir = new File(rootDir, relativePath);
        if (dir.isDirectory()) {
            File tagsFile = new File(dir, fileName);
            if (tagsFile.canRead()) {
                try {
                    return tagsFile.toURL();
                }
                catch (MalformedURLException e) {
                    // ignore
                }
            }
        }
        return Thread.currentThread().getContextClassLoader().
                getResource(relativePath + fileName);
    }

    private static void loadSafeHtmlConfig ()
    {
        URL safeAttrsUrl = urlForResource("resource/html/", SAFE_ATTRS_FILE);
        URL safeTagsUrl = urlForResource("resource/html/", SAFE_TAGS_FILE);

        if (safeAttrsUrl != null || safeTagsUrl != null) {
            String[] safeTags =
                (safeTagsUrl != null) ? readFileToStrings(safeTagsUrl) : new String[0];
            String[] safeAttrs =
                (safeAttrsUrl != null) ? readFileToStrings(safeAttrsUrl) : new String[0];
            HTML.setSafeConfig(safeTags, safeAttrs);
        }
        else {
            Log.aribaweb.warn("Failed to load safeHtmlConfig");
        }
    }

    public static AWServerApplication sharedInstance ()
    {
        return AWConcreteServerApplication.sharedInstance();
    }

    /**
        This is private -- only public so may be accessed by test code.
    */
    public AWCheckoutManager httpSessionCheckoutManager ()
    {
        return _httpSessionCheckoutManager;
    }

    ///////////////////
    // Global Defaults
    ///////////////////
    public void setPageCacheSize (int intValue)
    {
        _pageCacheSize = intValue;
    }

    public int pageCacheSize ()
    {
        return _pageCacheSize;
    }

    public void setSessionTimeout (int seconds)
    {
        Log.aribaweb.debug("AWConcreteApplication.setSessionTimeout() %d",seconds);
        _sessionTimeout = seconds;
    }

    public int sessionTimeout ()
    {
        return _sessionTimeout;
    }

    abstract public String webserverDocumentRootPath ();

    public String adaptorUrl ()
    {
        return _adaptorUrl;
    }

    public String adaptorUrlSecure ()
    {
        return _adaptorUrlSecure;
    }

    /**
     * Simple class that encapsulates information calculated from the request.
     */
    public class RequestURLInfo
    {
        /**
         The key that used to determine how the request is handled.
         Typically, this is either "aw" (a component request) or "ad"
         (a direct action.)
         For a URL that comes in as follows:
         /{web-app-name}/{servlet-name}/{request-handler-key}/{extra-stuff}
         this data member will hold the value of {request-handler-key}.
         */
        public String requestHandlerKey;

        /**
         The application number for applications which define such a concept.
         Is often null.
         For a URL that comes in as follows:
         /{web-app-name}/{servlet-name}/{application-number}/{request-handler-key}/{extra-stuff}
         this data member will hold the value of {application-number}.
         */
        public String applicationNumber;

        /**
         The tab index of the request. If there is a number after the servlet-
         name, then we need to check the servlet-name for an underscore. This
         indicates that the URL is tabbed and that the number is the
         tab-index and not the application-number.
         For a URL that comes in as follows:
         /{web-app-name}/_{servlet-name}/{tab-index}/{request-handler-key}/{extra-stuff}
         /{web-app-name}/_{servlet-name}/{tab-index}/{application-number}/{request-handler-key}/{extra-stuff}
         this data member will hold the value of {tab-index}.
         */
        public String tabIndex;

        /**
         An array containing: [ {request-handler-key}, {extra-stuff}, ...]
         */
        public String[] requestHandlerPath;
    }

    /*
    Parse URL in app-specific way and return its components.
    Used by AWBaseRequest.InternalRequest for internal direct actiondispatch.
     */
    public RequestURLInfo requestUrlInfo (String url)
    {
        return null;
    }

    public void setUseEmbeddedKeyPathes (boolean useEmbeddedKeyPathes)
    {
        _useEmbeddedKeyPathes = useEmbeddedKeyPathes;
    }

    public boolean useEmbeddedKeyPathes ()
    {
        return _useEmbeddedKeyPathes;
    }

    ////////////////////////////
    // Timezone support
    ////////////////////////////

    public List getPreferredTimezones ()
    {
        return null;
    }

    public Map<String,TimeZone> getTimeZoneByOffsetKeys ()
    {
        return null;
    }

    //////////////////////
    // Page Creation
    //////////////////////
    public AWComponent createPageWithName (String componentName,
                                           AWRequestContext requestContext)
    {
        AWComponentDefinition componentDefinition =
                componentDefinitionForName(componentName);
        if (componentDefinition == null) {
            throw new AWGenericException("Unable to locate page with name \""
                    + componentName + "\"");
        }
        AWComponentReference sharedComponentReference =
                componentDefinition.sharedComponentReference();
        AWComponent newComponent =
                componentDefinition.createComponent(sharedComponentReference,
                        null, requestContext);
        AWPage page = newComponent.page();
        newComponent.ensureAwake(page);
        page.ensureAwake(requestContext);
        if (requestContext != null) {
            requestContext.registerNewPageComponent(newComponent);
        }
        return newComponent;
    }

    protected String _mainPageName = "Main";

    public void setMainPageName (String name)
    {
        _mainPageName = name;
    }

    public String mainPageName ()
    {
        return _mainPageName;
    }

    public AWResponseGenerating mainPage (AWRequestContext requestContext)
    {
        AWResponseGenerating page =
                createPageWithName(mainPageName(), requestContext);

        if (page instanceof AWResponseGenerating.ResponseSubstitution) {
            page =
                ((AWResponseGenerating.ResponseSubstitution)page).replacementResponse();
        }
        return page;
    }

    //////////////////////
    // Session Management
    //////////////////////
    protected AWCheckoutManager createHttpSessionCheckoutManager ()
    {
        return new AWCheckoutManager("HttpSessionIds");
    }

    public AWSession createSession (AWRequestContext requestContext)
    {
        AWSession newSession = null;
        Class sessionClass = sessionClass();
        try {
            newSession = (AWSession)sessionClass.newInstance();
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new AWGenericException(illegalAccessException);
        }
        catch (InstantiationException exception) {
            String message = Fmt.S("Error: cannot create instance of httpSession" +
                    " class \"%s\"", sessionClass.getName());
            throw new AWGenericException(message, exception);
        }
        newSession.init(this, requestContext);

        // default the timezone
        if (newSession.clientTimeZone() == null) {
            newSession.setClientTimeZone(TimeZone.getDefault());
        }

        return newSession;
    }

    public HttpSession restoreHttpSession (AWRequest request, String sessionId)
    {
        HttpSession httpSession = request.getSession(false);
        if (shouldInvalidateHttpSession(httpSession)) {
            Log.aribaweb.debug("restoreHttpSession: httpSession invalidated: %s",
                    sessionId);
            Log.logStack(Log.aribaweb_session);

            httpSession = null;
        }
        return httpSession;
    }

    public void archiveHttpSession (HttpSession httpSession)
    {
        if (shouldInvalidateHttpSession(httpSession)) {
            Log.aribaweb.debug("archiveHttpSession: HttpSession invalidated: %s",
                               httpSession.getId());
            httpSession.invalidate();
        }
    }


    private boolean shouldInvalidateHttpSession (HttpSession httpSession)
    {
        boolean shouldInvalidate = false;
        if (httpSession != null) {
            AWSession session = AWSession.session(httpSession);

            // session is null if httpSession is newly created and does
            // not have the AWSession stored as a session attribute.
            //
            // We have seen HttpServletRequest.getSession(false) return
            // a non-null httpSession even after the user's session has
            // timed out on weblogic.
            //
            // This method is called to determine if the user's session
            // has been terminated by explicitly or has timed out.
            //
            // If there is an httpSession, but there is no AWSession attribute,
            // we will assume that the session has timed out and return true
            // to cause the session to be invalidated.
            //
            // This will result in the timeout page being displayed to the user.

            if (session != null) {
                shouldInvalidate = session.shouldInvalidate();
                Log.aribaweb.debug("Checking AWSession.shouldInvalidate: %s",
                                   shouldInvalidate ? "TRUE" : "FALSE");
            }
            else {
                shouldInvalidate = true;
                Log.aribaweb.debug("Unable to find AWSession associated with " +
                                   "httpsession. Invalidate.");
            }
        }
        return shouldInvalidate;
    }

    public void checkinHttpSessionId (String sessionId)
    {
        _httpSessionCheckoutManager.checkin(sessionId);
    }

    /**
        This method only locks the sessionId -- do not expect it to return an HttpSession
        The method signature is here for backward compatibility, but only the WOAdaptor
        actually returns an HttpSession here.
    */
    public void checkoutHttpSessionId (String sessionId)
    {
        _httpSessionCheckoutManager.checkout(sessionId);
    }

    public HttpSession createHttpSession (AWRequest request)
    {
        return request.getSession(true);
    }

    public int activeHttpSessionCount ()
    {
        return monitorStats().activeSessionCount();
    }

    public boolean isHttpSessionCheckedOut (String sessionId)
    {
        return _httpSessionCheckoutManager.isCheckedOut(sessionId);
    }

    public void terminate ()
    {
        logString("Dispatcher exiting normally. Restart me.");
        SystemUtil.exit(0);
    }

    public void setTerminateApplicationPassword (String terminateApplicationPassword)
    {
        _terminateApplicationPassword = terminateApplicationPassword;
    }

    public String terminateApplicationPassword ()
    {
        return _terminateApplicationPassword;
    }



    public void registerSession (AWSession session)
    {
        if (session.registerActiveSession()) {
            addSessionToStatusTable(session);
        }
    }

    public void deregisterSession (AWSession session)
    {
        // when registering a session that have been marked for termination,
        // we need to decrement the marked for termination session count.
        // Otherwise, the active session count will be incorrect.
        if (session.isMarkedForTermination()) {
            monitorStats().decrementMarkedForTerminationSessionCount(session);
        }
        if (session.unregisterActiveSession()) {
            removeSessionFromStatusTable(session);
        }
    }

    public void incrementMarkedForTerminationSessionCount (AWSession session)
    {
        monitorStats().incrementMarkedForTerminationSessionCount(session);
    }

    protected void updateSessionStatusTable (
            ConcurrentLinkedQueue<AWConcreteApplication.SessionWrapper> connectList,
            List existingSessions)
    {
        if (_sessionStatusManager != null) {
            _sessionStatusManager.updateSessionStatusTable(connectList, existingSessions);
        }
    }

    public AWSessionStatusManager getSessionStatusManager ()
    {
        return _sessionStatusManager;
    }

    public AWResponseGenerating monitorSessionStatsPage (AWRequestContext requestContext)
    {
        AWMonitorSessionStatsPage page = (AWMonitorSessionStatsPage)createPageWithName(
                                                    AWMonitorSessionStatsPage.PageName,
                                                    requestContext);
        return page;
    }

    ////////////////////////
    // Req Handling Support
    ////////////////////////
    protected Class requestContextClass ()
    {
        return RequestContextClass;
    }

    public AWRequestContext createRequestContext (AWRequest request)
    {
        AWRequestContext requestContext = null;
        try {
            requestContext = (AWRequestContext)requestContextClass().newInstance();
        }
        catch (IllegalAccessException exception) {
            throw new AWGenericException(exception);
        }
        catch (InstantiationException exception) {
            throw new AWGenericException("Error: cannot create instance of " +
                    "AWRequestContext class: " + RequestContextClass +
                    " exception: ", exception);
        }
        requestContext.init(this, request);
        if (_Staticizer != null) {
            _Staticizer.didCreateRequestContext(requestContext);
        }
        return requestContext;
    }

    public AWHandleExceptionPage handleExceptionPage (AWRequestContext requestContext)
    {
        return (AWHandleExceptionPage)createPageWithName(AWXHandleExceptionPage.PageName,
                requestContext);
    }

    public AWResponseGenerating handleException (AWRequestContext requestContext,
                                                 Exception exception)
    {
        logString(SystemUtil.stackTrace(exception));
        // Log to server log
        // Log.aribaweb_html.error(3615, SystemUtil.stackTrace(exception));

        AWHandleExceptionPage exceptionPage = handleExceptionPage(requestContext);
        exceptionPage.setException(exception);
        PerformanceState.setStatus(PerformanceState.Status_InternalError);

        return exceptionPage;
    }

    public static void appendGenericExceptionMessageToResponse (AWResponse response,
                                                                Throwable throwable)
    {
        response.appendContent("<html><body>");
        response.appendContent("<h3>Exception encountered.</h3>");
        if (IsDebuggingEnabled) {
            response.appendContent("<pre>Debug Enabled. Showing Stack Trace:\n");
            response.appendContent(HTML.escape(SystemUtil.stackTrace(throwable)));
            response.appendContent("</pre>");
        }
        response.appendContent("</body></html>");
    }

    public AWSessionRestorationErrorPage handleSessionRestorationErrorPage (
            AWRequestContext requestContext)
    {
        return (AWSessionRestorationErrorPage)createPageWithName(
                AWXSessionRestorationErrorPage.PageName, requestContext);
    }

    public AWResponseGenerating handleSessionRestorationError (
            AWRequestContext requestContext)
    {
        AWResponseGenerating response = null;
        AWSessionValidator sessionValidator = getSessionValidator();
        if (sessionValidator != null) {
            response = sessionValidator.handleSessionRestorationError(requestContext);
        }
        else {
            response = handleSessionRestorationErrorPage(requestContext);
        }
        return response;
    }

    public AWResponseGenerating handleComponentActionSessionValidationError (
            AWRequestContext requestContext, Exception exception)
    {
        AWResponseGenerating response = null;
        AWSessionValidator sessionValidator = getSessionValidator();
        if (sessionValidator != null) {
            response = sessionValidator.handleComponentActionSessionValidationError(
                    requestContext, exception);
        }
        return response;
    }


    public AWResponseGenerating handleSiteUnavailableException (
            AWRequestContext requestContext)
    {
        return null;
    }

    public AWResponseGenerating handleRemoteHostMismatchException (
        AWRequestContext requestContext, AWRemoteHostMismatchException exception)
    {
        // default to handle session restoration error
        // overrides should not trigger another remote host mismatch exception
        // see AWComponent.shouldValidateRemoteHost()
        return handleSessionRestorationError(requestContext);
    }

    public AWResponseGenerating handleMaxWindowException (
            AWRequestContext requestContext,
            MaximumTabExceededException exception)
    {
        AWComponentDefinition componentDefinition =
                createComponentDefinitionForNameAndClass(
                        AWMultiTabException.Name, AWMultiTabException.class);
        _componentDefinitionHashtable.put(componentDefinition, AWMultiTabException.Name);
        AWHandleExceptionPage exceptionPage =
                (AWHandleExceptionPage) createPageWithName(
                        AWMultiTabException.Name, requestContext);
        exceptionPage.setException(exception);
        return exceptionPage;
    }

    public AWResponseGenerating handleSessionValidationError (
            AWRequestContext requestContext, Exception exception)
    {
        AWResponseGenerating response = null;
        AWSessionValidator sessionValidator = getSessionValidator();
        if (sessionValidator != null) {
            response = sessionValidator.handleSessionValidationError(
                    requestContext, exception);
        }
        else {
            throw new AWGenericException("AWSessionValidator must be registered " +
                                         "to handle session validation error.");
        }
        return response;
    }

    protected boolean shouldRefuseResponse (AWRequest request)
    {
        // if we're refusing new sessions and there is not an existing httpSession
        // and this is not a special server management direct action, then
        // refuse response
        return (_refusingNewSessions && (request.sessionId() == null) &&
                !AWDirectAction.isServerManagementAction(request));
    }

    protected AWResponse handleRefusedResponse (AWRequest request)
    {
        AWResponse response = createResponse(request);
        String retryCountString = request.formValueForKey(RetryKey, false);
        int retryCount =
            (retryCountString == null) ? 0 : Integer.parseInt(retryCountString);
        if (retryCount < 10) {
            response.setStatus(AWResponse.StatusCodes.RedirectMoved);
            String retryUrl = StringUtil.strcat(applicationUrl(request), "?",
                RetryKey, "=", AWUtil.toString(retryCount + 1));
            response.setHeaderForKey(retryUrl, "Location");
        }
        else {
            appendRefusingNewRequestsMessage(response);
        }
        return response;
    }

    public AWResponse dispatchRequest (AWRequest request)
    {
        if (IsRequestLoggingEnabled) {
            logRequestMessage("uri " + request.uri() + " formValues: "
                    + request.formValues());
        }

        AWResponse response = null;

        try {
            if (PerformanceState.threadStateEnabled()) {
                PerformanceState.DispatchTimer.start();
                PerformanceState.ThreadCPUTimer.start();
            }

            if (shouldRefuseResponse(request)) {
                response = handleRefusedResponse(request);
            }
            else {
                String httpSessionId = request.sessionId();
                if (httpSessionId != null) {
                    boolean isCheckedOut = isHttpSessionCheckedOut(httpSessionId);
                    ((AWBaseRequest)request).setIsQueued(isCheckedOut);
                }
                response = super.dispatchRequest(request);
                _monitorStats.incrementTotalRequestsServed();
            }
            if (IsResponseLoggingEnabled) {
                logResponseMessage(response.contentString());
            }

            ((AWBaseRequest)request).dispose();
        }
        finally {
            if (PerformanceState.threadStateEnabled()) {
                PerformanceState.DispatchTimer.stop(0);
                PerformanceState.ThreadCPUTimer.stop(0);
            }
        }
        return response;
    }

    protected void appendRefusingNewRequestsMessage (AWResponse response)
    {
        response.appendContent(
            "The application you're attempting to access is refusing new sessions.");
    }

    public void setRefusingNewSessions (boolean refusingNewSessions)
    {
        _refusingNewSessions = refusingNewSessions;
        _monitorStats.setIsRefusingNewSessions(refusingNewSessions);
    }

    public void setRefuseNewSessionsPassword (String refuseNewSessionsPassword)
    {
        _refuseNewSessionsPassword = refuseNewSessionsPassword;
    }

    public String refuseNewSessionsPassword ()
    {
        return _refuseNewSessionsPassword;
    }

    abstract public String applicationUrl (AWRequest request);

    public String directActionClassNameForKey (String classNameKey)
    {
        return classNameKey;
    }

    public int hibernationDepth ()
    {
        return 2;
    }

    ////////////////////////////////////
    //     Graceful Shutdown Support
    // Implementation of ShutdownDelayer
    ////////////////////////////////////

    public void initiateShutdown ()
    {
        Log.aribaweb_shutdown.debug("Shutdown request received.  Refusing new sessions.");
        ShutdownManager sm = ShutdownManager.get();
        AWShutdownState.init(sm);
        AWMonitorStats monitorStats = monitorStats();
        setRefusingNewSessions(true);
        monitorStats.setGracefulShutdown(true);
        monitorStats.setRemainingShutdownPeriod(sm.getTimeBeforeShutdown());
    }

    /*
       This method is called multiple times. It is called on a regular
       basis from the time the shutdown was requested and until the
       shutdown is completed.
    */
    public boolean canShutdown ()
    {
        AWMonitorStats monitorStats = monitorStats();
        int activeSessionCount = getUISessionCount();
        Log.aribaweb_shutdown.debug("Shutdown pending -- active sessions: %s",
                                    activeSessionCount);
        if (activeSessionCount > 0) {
            long remainingPeriod = ShutdownManager.get().getTimeBeforeShutdown();
            monitorStats.setRemainingShutdownPeriod(remainingPeriod);
            if (remainingPeriod <= AWShutdownState.WarningPeriod) {
                // When the system is in shutdown warning mode,
                // active sessions will begin to get warning messages until
                // the force shutdown happens.
                monitorStats.setIsInShutdownWarningPeriod(true);
                Log.aribaweb_shutdown.debug(
                    "Shutdown warning period begun.  Forcing shutdown in (%s millis).",
                    Constants.getLong(remainingPeriod));
            }
            List<NamedValue> list = getUISessionCountBuckets();
            for (int i = 0; i < list.size(); i++) {
                NamedValue nv = list.get(i);
                ariba.util.log.Log.shutdown.info(10339, this.name(),
                        nv.getName(), nv.getValue());
            }
            return false;
        }
        return true;
    }

    public int getUISessionCount ()
    {
        if (_sessionMonitor == null) {
            monitorStats().activeSessionCount();
        }
        return _sessionMonitor.sessionCount();
    }

    public List<NamedValue> getUISessionCountBuckets ()
    {
        if (_sessionMonitor == null) {
            monitorStats().activeSessionCountBuckets();
        }
        return _sessionMonitor.sessionCountBuckets();
    }

    public List<NamedValue> getUISessionStatusBuckets ()
    {
        return _sessionMonitor.sessionStatusBuckets();
    }

    public void cancelShutdown ()
    {
        AWMonitorStats monitorStats = monitorStats();
        setRefusingNewSessions(false);
        monitorStats.setGracefulShutdown(false);
        monitorStats.setRemainingShutdownPeriod(0);
        monitorStats.setIsInShutdownWarningPeriod(false);
    }

    ////////////////////////////////
    // ComponentDefinition Handling
    ////////////////////////////////
    public AWComponentDefinition createComponentDefinitionForNameAndClass (
            String componentName, Class componentClass)
    {
        AWComponentDefinition componentDefinition = new AWComponentDefinition();
        componentDefinition.init(componentName, componentClass);
        return componentDefinition;
    }

    protected Class readClassFromTemplate (AWResource resource)
    {
        Class componentClass = null;
        InputStream inputStream = resource.inputStream();
        byte[] bytes = AWUtil.getBytes(inputStream);
        AWUtil.close(inputStream);
        String templateString = new String(bytes);
        int indexOfOpenComment = templateString.indexOf(ClassCommentAlt);
        int indexOfClassName = indexOfOpenComment + ClassCommentAlt.length();
        if (indexOfOpenComment == -1) {
            indexOfOpenComment = templateString.indexOf(ClassComment);
            indexOfClassName = indexOfOpenComment + ClassComment.length();
        }
        if (indexOfOpenComment != -1) {
            int indexOfClosingComment = templateString.indexOf("-->", indexOfClassName);
            if (indexOfClosingComment == -1) {
                throw new AWGenericException("Missing closing comment " +
                        "when parsing class name from " + resource.url());
            }
            String className = templateString.substring(indexOfClassName,
                    indexOfClosingComment).trim();
            componentClass = AWUtil.classForName(className);
        }
        return componentClass;
    }

    public AWComponentDefinition componentDefinitionForName (String componentName)
    {
        // Note: This gets called while warming up but not much after that.  The AWIncludeComponent does call this a lot, though.
        // I have made the _componentDefinitionHashtable effectively immutable -- its always copied rather than added to directly
        AWComponentDefinition componentDefinition = null;
        if (componentName != null) {
            componentDefinition = (AWComponentDefinition)_componentDefinitionHashtable.get(componentName);
            if (componentDefinition == null) {
                synchronized (_componentDefinitionHashtable) {
                    componentDefinition = (AWComponentDefinition)_componentDefinitionHashtable.get(componentName);
                    if (componentDefinition == null) {
                        if ((_resolverInstance != null) &&
                            ((componentDefinition = _resolverInstance.definitionWithName(componentName, null)) != null))
                        {
                            _componentDefinitionHashtable.put(componentName.intern(), componentDefinition);
                        }
                        else {
                            Class componentClass = null;
                            AWResourceManager resourceManager = resourceManager();
                            componentClass = resourceManager.classForName(componentName);
                            if (componentClass == null) {
                                if (Character.isUpperCase(componentName.charAt(0))) {
                                    String templateName = StringUtil.strcat(componentName, AWComponent.ComponentTemplateFileExtension);
                                    AWResource resource = resourceManager.packageResourceNamed(templateName);
                                    if (resource != null) {
                                        componentClass = readClassFromTemplate(resource);
                                        if (componentClass == null) {
                                            componentClass = DefaultComponentClass;
                                        }
                                        if (AWComponent.ClassObject.isAssignableFrom(componentClass)) {
                                            componentDefinition = createComponentDefinitionForNameAndClass(componentName, componentClass);
                                            componentDefinition.setTemplateName(resource.relativePath());
                                            _componentDefinitionHashtable.put(componentName.intern(), componentDefinition);
                                        }
                                        else {
                                            throw new AWGenericException(getClass().getName() + ": invalid class specified for Classless component: " + componentClass.getName());
                                        }
                                    }
                                }
                            }
                            else if (AWComponent.ClassObject.isAssignableFrom(componentClass)) {
                                componentDefinition = createComponentDefinitionForNameAndClass(componentName, componentClass);
                                _componentDefinitionHashtable.put(componentName.intern(), componentDefinition);
                            }
                        }
                    }
                }
            }
        }
        else {
            throw new AWGenericException(getClass().getName() +
                    ": null componentName not allowed.");
        }
        return componentDefinition;
    }

    // Returns a vector of AWTemplate after loading all of the templates

    public List preloadAllTemplates ()
    {
        return preinstantiateAllComponents(false, null);
    }

    /** returns a vector of AWTemplates */
    public List preinstantiateAllComponents (boolean instantiateDefinitions,
                                             AWRequestContext requestContext)
    {
        List allTemplates = ListUtil.list();
        Map alreadyParsedTemplates = MapUtil.map();
        List allResources = resourceManager().allResources();

        for (int index = 0, length = allResources.size(); index < length; index++) {
            AWResource resource = (AWResource)allResources.get(index);
            String relativePath = resource.relativePath();
            if (relativePath == null) {
                logWarning("**** Skipping malformed resource: " + resource);
            }
            else if (!relativePath.equals(alreadyParsedTemplates.get(relativePath))) {
                alreadyParsedTemplates.put(relativePath, relativePath);
                if (relativePath.endsWith("AXEtd.awl")) {
                    logString("*** Skipping parse of AXEtd.awl");
                    continue;
                }
                if (relativePath.endsWith(".awl") || relativePath.endsWith(".htm")
                        || relativePath.endsWith(".html")) {

                    try {
                        if (instantiateDefinitions) {
                            String componentName = (new File(relativePath).getName());
                            componentName = AWUtil.substringTo(componentName, '.');
                            logString("-Loading " + relativePath + " ("
                                    + componentName + ")");

                            AWComponentDefinition componentDefinition = null;
                            try {
                                componentDefinition =
                                        componentDefinitionForName(componentName);
                            }
                            catch (AWGenericException e) {
                                // fall through to test below...
                            }

                            if (componentDefinition == null) {
                                logWarning("         *** null component definition");
                                continue;
                            }

                            AWComponentReference sharedComponentReference =
                                    componentDefinition.sharedComponentReference();
                            // AWComponent instance = componentDefinition.createComponent(sharedComponentReference, null, requestContext);
                            AWComponent instance =
                                    componentDefinition.newComponentInstance();
                            instance._setup(sharedComponentReference,
                                    new AWPage(instance,  requestContext));

                            if (instance == null) {
                                 logWarning("         *** null component instance");
                                 continue;
                            }
                            if (StringUtil.nullOrEmptyString(instance.name())) {
                                logWarning("         *** null component name");
                                continue;
                            }

                            if (AWConcreteApplication.IsDebuggingEnabled) {
                                instance.validate(requestContext.validationContext());
                            }

                            allTemplates.add(instance.loadTemplate());
                        }
                        else {
                            InputStream inputStream = resource.inputStream();
                            String templateString =
                                    AWComponent.readTemplateString(inputStream);
                            logString("-Parsing " + relativePath);
                            AWTemplate template =
                                AWComponent.defaultTemplateParser().
                                templateFromString(templateString, relativePath);
                            allTemplates.add(template);
                        }
                    }
                    catch (RuntimeException runtimeException) {
                        logWarning("**** Failure Parsing: " + relativePath);
                        runtimeException.printStackTrace();
                        //throw runtimeException;
                    }
                }
                else if (!relativePath.endsWith(".gif")
                        && !relativePath.equals("scratch"))  {
                    logString("**** Skipping: " + relativePath);
                }
            }
        }
        return allTemplates;
    }

    // Generate a list of all component definitions

    public List getAllComponentDefinitions ()
    {
        List componentDefinitions = ListUtil.list();
        Map alreadyParsedTemplates = MapUtil.map();
        List allResources = resourceManager().allResources();

        for (int index = 0, length = allResources.size(); index < length; index++) {
            AWResource resource = (AWResource)allResources.get(index);
            String relativePath = resource.relativePath();
            if (relativePath != null) {
                if (!relativePath.equals(alreadyParsedTemplates.get(relativePath))) {
                    alreadyParsedTemplates.put(relativePath, relativePath);
                    if (relativePath.endsWith("AXEtd.awl")) {
                        continue;
                    }
                    if (relativePath.endsWith(".awl") ||
                        relativePath.endsWith(".htm") ||
                        relativePath.endsWith(".html")) {
                        try {
                            String componentName =
                                    getComponentNameFromTemplatePath(relativePath);
                            logString("Generating component definition: "
                                    + componentName);
                            AWComponentDefinition componentDefinition =
                                    componentDefinitionForName(componentName);
                            componentDefinitions.add(componentDefinition);
                        }
                        catch (Throwable t) {
                            logWarning("Error prevented getting component " +
                                    "definition for " + relativePath + ": " + t);
                        }
                    }
                }
            }
        }
        return componentDefinitions;
    }

    private AWSessionValidator getSessionValidator ()
    {
        if (_sessionValidator != null) {
            return _sessionValidator;
        }
        else {
            initSessionValidator();
            return _sessionValidator;
        }
    }
    private String getComponentNameFromTemplatePath (String relativePath)
    {
        String fileName = AWUtil.lastComponent(relativePath, '/');
        String componentName = AWUtil.stripToBaseFilename(fileName);
        return componentName;
    }

    /**
     * This hook is used by the DemoShell to override component lookup
     * to search for .htm templates (relative to the directory of the
     * parent component) rather than just look up .awls with the
     * resource manager.  For regular apps this hook is not used.
     *
     * When component is referenced by a SwitchComponent, parent will be the component
     * in which the tag appears.  When this call is a result of pageWithName, parent will
     * be null
     */
    public interface ComponentDefinitionResolver
    {
        public AWComponentDefinition definitionWithName (String name, AWComponent parent);
    }

    private ComponentDefinitionResolver _resolverInstance = null;

    public ComponentDefinitionResolver _componentDefinitionResolver ()
    {
        return _resolverInstance;
    }

    public void _setComponentDefinitionResolver (ComponentDefinitionResolver instance)
    {
        _resolverInstance = instance;
    }

    public AWComponentDefinition _componentDefinitionForName (String componentName,
                                                              AWComponent component)
    {
        AWComponentDefinition definition = null;

        // we trap out through this interface in case someone registered an
        // alternate resolver.
        if (_resolverInstance != null) {
            definition = _resolverInstance.definitionWithName(componentName, component);
        }

        return (definition != null) ? definition
            : componentDefinitionForName(componentName);
    }

    public AWBookmarker createBookmarker ()
    {
        return new AWBookmarker();
    }

    public AWBookmarker getBookmarker ()
    {
        return _bookmarker;
    }

    //////////////////////
    // Monitoring Support
    //////////////////////
    public AWMonitorStats createMonitorStats ()
    {
        return new AWMonitorStats();
    }

    public AWMonitorStats monitorStats ()
    {
        return _monitorStats;
    }

    public Map customKeyValueStats ()
    {
        return null;
    }

    ///////////////////
    // Debugging
    ///////////////////
    public void logActionMessage (String actionLogMessage)
    {
        debugString(actionLogMessage);
    }

    public void logRequestMessage (String requestLogMessage)
    {
        debugString(requestLogMessage);
    }

    public void logResponseMessage (String responseLogMessage)
    {
        debugString(responseLogMessage);
    }

    public boolean allowsJavascriptUrls ()
    {
        return !IsRapidTurnaroundEnabled;
    }

    public boolean allowsStyleSheetUrls ()
    {
        return !(IsRapidTurnaroundEnabled  || IsDirectConnectEnabled);
    }

    public boolean allowBrandingImages ()
    {
        return AWConcreteApplication.IsDirectConnectEnabled;
    }

    //////////////////////////
    // Package level flags
    ///////////////////////////
    public boolean isPackageLevelFlagEnabled (String packageName, int flag)
    {
        return ((resourceManager().packageFlags(packageName) & flag) != 0);
    }

    public void enablePackageLevelFlag (String packageName, int flag)
    {
        AWMultiLocaleResourceManager resourceManager = resourceManager();
        resourceManager.setPackageFlags(packageName,
                resourceManager().packageFlags(packageName) | flag);
    }

    public void disablePackageLevelFlag (String packageName, int flag)
    {
        AWMultiLocaleResourceManager resourceManager = resourceManager();
        resourceManager.setPackageFlags(packageName,
                resourceManager().packageFlags(packageName) & ~flag);
    }

    //////////////////////
    // Session Validation Support
    //////////////////////
    public void setSessionValidator (AWSessionValidator validator)
    {
        _sessionValidator = validator;
    }

    public void assertExistingSession (AWRequestContext requestContext)
    {
        // default to no-op
        AWSessionValidator sessionValidator = getSessionValidator();
        if (sessionValidator != null) {
            sessionValidator.assertExistingSession(requestContext);
        }
    }

    public void assertValidSession (AWRequestContext requestContext)
    {
        try {
            initSessionValidator();
        }
        catch(Throwable e)
        {
            Log.aribaweb.warning(9240, e.getMessage());
            Log.aribaweb.debug(SystemUtil.stackTrace(e));
        }

        // default to no-op
        AWSessionValidator sessionValidator = getSessionValidator();
        if (sessionValidator != null) {
            sessionValidator.assertValidSession(requestContext);
        }
    }

    public void initSessionValidator ()
    {
    }

    //////////////////////
    // Node Support
    //////////////////////
    protected AWNodeManager initNodeManager ()
    {
        return null;
    }

    public AWNodeManager getNodeManager ()
    {
        return _nodeManager;
    }

    protected void initNodeValidators ()
    {
    }

    public void assertValidNode (AWRequestContext requestContext,
                                 String directActionClassName, String actionName)
    {
        // default to no-op
        if (_nodeManager != null) {
            AWNodeValidator nv =
                _nodeManager.nodeValidatorForDirectAction(directActionClassName,
                                                          actionName);
            if (nv == null) {
                Log.aribaweb.debug("Node validation called on directAction %s.%s" +
                                   " which does not have a AWNodeValidator defined.",
                                   directActionClassName,
                                   actionName);
            }
            else if (!nv.isValid(requestContext)) {
                throw nv.getNodeChangeException();
            }
        }
    }


    public String getNodeName ()
    {
        //Default implementation returns a bogus String
        return "<Unknown_None>";
    }


    //////////////////////
    // Valid request support
    //////////////////////

    public void validateRequest (AWRequestContext requestContext)
    {
        // no-op for now.  Allow subclasses to implement.
        // Subclasses should still call super.validateRequest
        // as we may implement basic validation here in the future.
    }

    ////////////////////////
    // Brand Support
    ////////////////////////

    protected AWBrandManager initBrandManager ()
    {
        return null;
    }

    public AWBrandManager getBrandManager ()
    {
        return _brandManager;
    }

    public AWBrand getBrand (AWRequestContext requestContext)
    {
        if (_brandManager != null) {
            return _brandManager.getBrand(requestContext);
        }
        else {
            return null;
        }
    }

    ///////////////////////////
    // Component Configuration
    ///////////////////////////
    public void registerComponentConfigurationSource (Class componentClass,
                                AWComponentConfigurationSource source)
    {
        _componentConfigurationSources.put(componentClass, source);
    }

    public AWComponentConfigurationSource getComponentConfigurationSource (
        Class componentClass)
    {
        return
            (AWComponentConfigurationSource)_componentConfigurationSources.get(
                    componentClass);
    }

    ///////////////////////////
    // Parameter support
    ///////////////////////////

    public AWParameters getConfigParameters ()
    {
        return _configParameters;
    }

    protected AWParameters initConfigParameters ()
    {
        return new AWParameters();
    }

    ///////////////////////////
    // Poll interval
    ///////////////////////////

    public int getPollInterval ()
    {
        return _pollInterval;
    }

    /**
     * Default pollInterval for the application in seconds.  Should be overridden
     * by applications to set default poll interval.
     * @aribaapi private
     */
    protected int initPollInterval ()
    {
        return 60;
    }

    /**
     * Noop by default. Subclasses can choose to initiate sessionMonitor by calling
     * startSessionMonitor
     * @aribaapi private
     */
    protected void initSessionMonitor ()
    {

    }

    private ConcurrentLinkedQueue<SessionWrapper> _sessionProcessList =
            new ConcurrentLinkedQueue<SessionWrapper>();

    enum SessionOp {
        Add, Remove
    };

    public static class SessionWrapper
    {
        SessionOp op;
        AWSession session;
        Exception callTrace;

        public SessionWrapper (SessionOp o, AWSession sess)
        {
            op = o;
            session = sess;
            if (AWConcreteApplication.IsDebuggingEnabled || 
                Log.aribaweb_userstatus.isDebugEnabled()) {
                callTrace = new Exception();
            }
        }
    }

    protected void addSessionToStatusTable (AWSession session)
    {
        _sessionProcessList.add(new SessionWrapper(SessionOp.Add, session));
    }

    protected void removeSessionFromStatusTable (AWSession session)
    {
        _sessionProcessList.add(new SessionWrapper(SessionOp.Remove, session));
    }

    private class AWSessionMonitor implements Runnable
    {
        private List _sessionList = ListUtil.list();
        private AWConcreteApplication _application;

        AWSessionMonitor (AWConcreteApplication application)
        {
            _application = application;
        }

        public int sessionCount ()
        {
            return _sessionList.size();
        }

        public List<NamedValue> sessionCountBuckets ()
        {
            Map<String, Integer> buckets = MapUtil.map(16);
            for (int i = _sessionList.size() - 1; i >= 0; i--) {
                Object b = ((AWSession)_sessionList.get(i)).monitorBucket();
                String bucket = b == null ? "null" : b.toString();
                Integer count = null;
                if ((count = buckets.get(bucket)) != null) {
                    count = count + 1;
                }
                else {
                    count = Constants.getInteger(1);
                }
                buckets.put(bucket, count);
            }

            List<NamedValue> ret = ListUtil.list();
            for (String bucket : buckets.keySet()) {
                NamedValue nv = new NamedValue(bucket, buckets.get(bucket));
                ret.add(nv);
            }
            return ret;
        }

        /**
         * For defects 1-AS11R9 / 1-BGIP86 / 1-CF04UZ, number of active sessions can be
         * seen from Ops monitoring page as high as 12000 for a realm with 24k users.
         * This maybe caused by the fact that idling user sessions are being timed
         * out by app server (Tomcat)
         * which is set at 30 mins, so as users login and idle, these sessions won't be
         * removed until 30 mins later or til user logs out directly, and so they still being
         * counted as active sessions.
         * <p/>
         * So this method will return a list of active sessions that are accounted for
         * by Ops monitoring page which includes user name and sessionId and session statuses.
         * <br/>For example,
         * &lt;sessionStatusesForBucket2>superuser (776162E3E075B38AC8D0DDBDFF53A79A; false;
         * false; 2013-08-02 19:32:27 EDT), cnoll (97B7D4B6B3529441A3176063EB47CA87;
         * false; false; 2013-08-02 19:30:37 EDT)&lt;/sessionStatusesForBucket2>
         * <p/>
         * Note that this extra sessionStatuses are optional and need the URL param as below:
         * http://czheng:8050/Buyer/Main/ad/monitorStats?showSessionStatus=true
         *
         * @return
         */
        public List<NamedValue> sessionStatusBuckets ()
        {
            Date tempDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");

            // Not need to expanding map until about 12+ realms (in one community)
            Map<String, FastStringBuffer> buckets = MapUtil.map(16);
            for (int i = _sessionList.size() - 1; i >= 0; i--) {
                Object b = ((AWSession)_sessionList.get(i)).monitorBucket();
                String bucket = b == null ? "null" : b.toString();
                FastStringBuffer fsb = buckets.get(bucket);
                if (fsb == null) {
                    // 100 users * 100 chars for status string
                    fsb = new FastStringBuffer(100 * 100);
                    buckets.put(bucket, fsb);
                }
                else {
                    fsb.append(", ");
                }
                AWSession s = (AWSession)_sessionList.get(i);
                tempDate.setTime(s.getLastAccessedTime());
                fsb.append(Fmt.S("%s (%s; %s; %s; %s)",
                    s.getFieldValue("RealUserUniqueName"),
                    s.sessionId(),
                    s.isTerminated(),
                    s.isInvalidated(),
                    sdf.format(tempDate)));
            }

            List<NamedValue> ret = ListUtil.list();
            for (String bucket : buckets.keySet()) {
                NamedValue nv = new NamedValue(bucket, buckets.get(bucket).toString());
                ret.add(nv);
            }
            return ret;
        }

        public void run ()
        {
            while (true) {
                try {
                    Thread.sleep(5000);

                    boolean paused = Log.aribaweb_userstatus_pause.isDebugEnabled();
                    if (paused) {
                        continue;
                    }
                    if (Log.aribaweb_userstatus.isDebugEnabled()) {
                        if (_sessionProcessList != null &&
                            !_sessionProcessList.isEmpty()) {
                            Log.aribaweb_userstatus.debug(
                                "AWSessionMonitor -- existing sessions: %s processing %s",
                                _sessionList.size(), _sessionProcessList.size());
                        }
                    }

                    _application.updateSessionStatusTable(_sessionProcessList,
                            _sessionList);

                }
                catch (InterruptedException e) {
                    Log.aribaweb.error(9023,
                               "AWSessionMonitor -- interrupted",
                                SystemUtil.stackTrace(e));
                }
                catch (Exception e) {
                    Log.aribaweb.error(9023,
                               "AWSessionMonitor -- exception caught",
                                SystemUtil.stackTrace(e));
                }
            }
        }
    }

    public static AWStaticSiteGenerator getStaticizer ()
    {
        return _Staticizer;
    }

    public static void setStaticizer (AWStaticSiteGenerator staticizer)
    {
        _Staticizer = staticizer;
    }

    public String formatUrlForResource (String urlPrefix,
                                        AWResource resource, boolean forCache)
    {
        return (_Staticizer != null)
                ? _Staticizer.formatUrlForResource(urlPrefix, resource, forCache)
                : StringUtil.strcat(urlPrefix, "/",
                resource.relativePath().replace('\\', '/'));
    }

    public boolean canCacheResourceUrls ()
    {
        return _Staticizer == null;
    }

    protected void startSessionMonitor ()
    {
        _sessionStatusManager = createSessionStatusManager();
        _sessionMonitor = new AWSessionMonitor(this);
        Thread sessionMonitorThread =
            new Thread(_sessionMonitor, "AWSessionMonitor Thread");
        // be a good citizen
        sessionMonitorThread.setPriority(Thread.MIN_PRIORITY);
        sessionMonitorThread.setDaemon(true);
        sessionMonitorThread.start();
    }

    protected AWSessionStatusManager createSessionStatusManager ()
    {
        return new AWSessionStatusManager();
    }


    /**
     * Applications should register their domain objects, activities, etc. on AWCommunityContext.
     * They should then call super.
     */
    protected void initCommunityContext()
    {

    }


    protected void initMultiTabSupport ()
    {

    }
    ////////////////////////////
    // Deprecated - remove soon
    ////////////////////////////
    public static boolean UseServletEnginesSession;
    public void sweepExpiredObjects () {}
    public void timeoutForObject (Object o) {}
    public void checkoutHttpSession (HttpSession httpSession) {}
    public void checkinHttpSession (HttpSession httpSession) {}
    public boolean useServletEnginesSession () {return true;}
}
