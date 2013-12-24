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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWConcreteServerApplication.java#73 $
*/

package ariba.ui.aribaweb.core;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import ariba.ui.aribaweb.util.AWBase64;
import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWClasspathResourceDirectory;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWLock;
import ariba.ui.aribaweb.util.AWLogHandling;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.ui.aribaweb.util.AWResourceManagerFactory;
import ariba.ui.aribaweb.util.AWSelfAccess;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.ClassUtil;
import ariba.util.core.HTML;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.PerformanceCheck;
import ariba.util.core.PerformanceChecker;
import ariba.util.core.PerformanceState;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import org.apache.log4j.Level;

abstract public class AWConcreteServerApplication extends AWBaseObject
        implements AWServerApplication, AWResourceManagerFactory, AWLogHandling
{
    public static boolean AllowsConcurrentRequestHandling = false;
    public static boolean IsRapidTurnaroundEnabled = false;
    public static boolean IsDebuggingEnabled = false;
    protected static boolean IsUserCommunityEnabled = false;
    public static boolean IsVerboseMode = false;
    public static boolean IsStatisticsGatheringEnabled = false;
    public static boolean IsAutomationTestModeEnabled = false;
    public static boolean IsAutomationPageTitleTestModeEnabled = false;
    public static AWServerApplication SharedInstance;
    public static int ResourceManagerFlushThreshold = 1024;
    private String _name;
    private AWMultiLocaleResourceManager _multiLocaleResourceManager;
    private Map _requestHandlers;
    private AWRequestHandler _defaultRequestHandler;
    private AWLock _cooperativeMultithreadingLock;
    private int _requestsSinceLastFlush = 0;
    private String _resourceUrl;
    protected static String _aribaUserCommunityUrl = null;
    protected static int _foldInSituOnWindowSize = 1280;
    
    // ** Thread Safety Considerations: all the ivars in this object are either read-only
    // after init time, or immutable, or offer their own locking (eg resourceMangaer) or
    // are not interdependent so that, if one thing changes, it won't affect others.  As
    // an example of this, if the IsRapidTurnaroundEnabled flag is read and then it
    // changes, before some action can be taken based on its value, it shouldn't matter.

    abstract public AWMultiLocaleResourceManager createResourceManager ();
    abstract public AWRequest createRequest (Object nativeRequestObject);
    abstract public AWResponse createResponse ();
    abstract public void initRequestHandlers ();

    abstract public boolean initIsStatisticsGatheringEnabled ();

    public AWResponse createResponse (AWRequest request)
    {
        return createResponse();
    }

    public void init ()
    {
        super.init();
        AWBaseObject.LogHandling = this;
        if (SharedInstance != null) {
            throw new AWGenericException(getClass().getName() +
                    ": Attempt to create more than one AWConcreteServerApplication.");
        }
        SharedInstance = this;
        AllowsConcurrentRequestHandling = allowsConcurrentRequestHandling();

        // Note: even if AllowsConcurrentRequestHandling is false, we still need to
        // initialize the cooperativemultithreadinglock. This is because there are apps
        // that uses this lock.
        _cooperativeMultithreadingLock = initCooperativeMultithreadingLock();

        IsDebuggingEnabled = isDebuggingEnabled();
        IsRapidTurnaroundEnabled = initIsRapidTurnaroundEnabled();
        if (IsRapidTurnaroundEnabled) {
            String AWReloadingClass = "ariba.awreload.ReloadingClassLoaderInit";
            if (ClassUtil.classForName(AWReloadingClass, false) != null) {
                ClassUtil.invokeStaticMethod(AWReloadingClass,
                                             "initClassReloading",
                                             new Class[]{String.class},
                                             new Object[]{null});
            }
        }
        AWUtil.RequiresThreadSafety = requiresThreadSafety();
        AWUtil.IsRapidTurnaroundEnabled = IsRapidTurnaroundEnabled;
        AWUtil.AllowsConcurrentRequestHandling = AllowsConcurrentRequestHandling;

        IsStatisticsGatheringEnabled = initIsStatisticsGatheringEnabled();
        _resourceUrl = initResourceUrl();
        AWMultiLocaleResourceManager.setResourceManagerFactory(this);
        _multiLocaleResourceManager = createResourceManager();
        _requestHandlers = MapUtil.map();
        initRequestHandlers();
        registerResourceDirectories(_multiLocaleResourceManager);

        // force state validation if debugging is enabled
        if (isDebuggingEnabled() && !Log.aribawebvalidation_state.isDebugEnabled()) {
            Log.aribawebvalidation_state.setLevel(Level.DEBUG);
            List loggers = ListUtil.list(Log.aribawebvalidation_state);
            ariba.util.log.Logger.addCommonAppendersToEmptyLoggers(loggers);
        }

        // force registration of perf metrics
        if (isDebuggingEnabled()) {
            ariba.ui.aribaweb.util.Log.perf_log.setLevel(ariba.util.log.Log.DebugLevel);
            ariba.ui.aribaweb.util.Log.perf_log_detail.setLevel(ariba.util.log.Log.DebugLevel);
            // Turn off perf logging by default for OSAW; otherwise it
            // would default to logging in "./logs" which is
            // undesireable.  TODO figure out how an OSAW app can have
            // a suitable default for
            // ariba.util.log.LogManager.getDirectoryName()
            if (IsJarApplication) {
                ariba.ui.aribaweb.util.Log.perf_log_trace.setLevel(ariba.util.log.Log.ErrorLevel);
            }
        }

        // PageGenerationSizeCounter
        ClassUtil.classTouch(AWBaseResponse.class.getName());

        // ResponseSizeCounter
        ClassUtil.classTouch(AWBaseResponse.class.getName());

        // ElemendIdInstantiationsCounter
        ClassUtil.classTouch(AWElementIdPath.class.getName());

        // Keys for fieldValue access
        ClassUtil.classTouch(AWSelfAccess.class.getName());

        // semantic key generation setup
        AWBase64.setEncodingBase(getSemanticKeyBase());

        // flag that logging can begin
        PerformanceState.registrationComplete();
    }

    // Called post init (and init callbacks) to begin processing
    protected void awake ()
    {

    }

    /**
     * Get the base that we should use for generating semantic keys.
     * @return the base, either 64 or 32
     * @aribaapi private
     */
    protected int getSemanticKeyBase ()
    {
        return 64;
    }

    protected AWLock initCooperativeMultithreadingLock ()
    {
        return new AWLock();
    }

    //////////////////////////
    // RequestHandler Support
    //////////////////////////
    public void registerRequestHandlerForKey (AWRequestHandler requestHandler, String requestHandlerKey)
    {
        _requestHandlers.put(requestHandlerKey, requestHandler);
    }

    public AWRequestHandler requestHandlerForKey (String requestHandlerKey)
    {
        return (AWRequestHandler)_requestHandlers.get(requestHandlerKey);
    }

    public void setDefaultRequestHandler (AWRequestHandler requestHandler)
    {
        _defaultRequestHandler = requestHandler;
    }

    public AWRequestHandler defaultRequestHandler ()
    {
        return _defaultRequestHandler;
    }

    ///////////////
    //
    ///////////////
    public AWSingleLocaleResourceManager createResourceManager (AWMultiLocaleResourceManager multiLocaleResourceManager, Locale locale)
    {
        AWSingleLocaleResourceManager singleLocaleResourceManager
                = new AWSingleLocaleResourceManager(multiLocaleResourceManager, locale);
        singleLocaleResourceManager.init();
        return singleLocaleResourceManager;
    }

    public AWSingleLocaleResourceManager createTemplateResourceManager ()
    {
        return resourceManager(Locale.US);
    }

    public AWMultiLocaleResourceManager resourceManager ()
    {
        return _multiLocaleResourceManager;
    }

    public AWSingleLocaleResourceManager resourceManager (Locale locale)
    {
        return (AWSingleLocaleResourceManager)resourceManager().resourceManagerForLocale(locale);
    }

    public void flushResourceManager ()
    {
        _multiLocaleResourceManager.flush();
    }

    public String deploymentRootDirectory ()
    {
        return null;
    }

    static List<String>_ExtraSearchPaths = ListUtil.list();

    public static void registerDebugSearchPath (String pathString)
    {
        Set jarNameSuffixes = _awJarNameSuffixes();

        // we suppress any directories for which we can deduce the
        // jar name and can tell that it's not in our search path

        String[] paths = AWUtil.componentsSeparatedByString(pathString, ";").array();
        for (String path : paths) {
            if (!StringUtil.nullOrEmptyOrBlankString(path)) {
                String jarSuffix = _jarNameSuffixForSourceDirectory(path);
                if (jarSuffix == null || jarNameSuffixes.contains(jarSuffix)) {
                    _ExtraSearchPaths.add(path);
                }
            }
        }
    }

    public static List<String>_debugSearchPaths ()
    {
        return _ExtraSearchPaths;
    }

    protected void registerResourceDirectories (AWMultiLocaleResourceManager resourceManager)
    {
        if (AWConcreteApplication.IsRapidTurnaroundEnabled) {
            String awSearchPath = AWUtil.getenv("ARIBA_AW_SEARCH_PATH");
            if (awSearchPath != null) registerDebugSearchPath(awSearchPath);
        }
        for (String p : _ExtraSearchPaths) registerAWSourcePath(p, resourceManager);
        
        String aribaDeployRoot = deploymentRootDirectory();
        if (aribaDeployRoot != null) {
            String deployedResourcesRoot = aribaDeployRoot + "/lib/resource";
            String resourcesGlobal = deployedResourcesRoot + "/global";
            String resourcesWebserver = deployedResourcesRoot + "/webserver";
            resourceManager.registerResourceDirectory(resourcesGlobal, null);
            resourceManager.registerResourceDirectory(resourcesWebserver, "/w");
        }
        resourceManager.registerPackageName("ariba.ui.aribaweb.html", true);
        resourceManager.registerPackageName("ariba.ui.aribaweb.core", true);
        
        if (AWConcreteServerApplication.IsDebuggingEnabled) {
            // force the class file to load so it registers with the annotation listener
            String cls = "ariba.ui.aribaweb.test.TestLinkManager";
            if (ClassUtil.classForName(cls, false) != null) {
                ClassUtil.invokeStaticMethod(cls, "forceClassLoad",
                        new Class[]{},
                        new Object[]{});
            }
        }
        // Register Elements -- this helps the resourceManager find these
        // classes and avoids our having to scan through all registered packages.
        resourceManager.registerClass(AWAction.class);
        resourceManager.registerClass(AWActionId.class);
        resourceManager.registerClass(AWActionUrl.class);
        resourceManager.registerClass(AWApi.class);
        resourceManager.registerClass(AWAppendEnvironment.class);
        resourceManager.registerClass(AWIncludeContent.class);
        resourceManager.registerClass(AWIf.class);
        resourceManager.registerClass(AWElse.class);
        resourceManager.registerClass(AWEnvironment.class);
        resourceManager.registerClass(AWGenericContainer.class);
        resourceManager.registerClass(AWGenericElement.class);
        resourceManager.registerClass(AWInitializeValue.class);
        resourceManager.registerClass(AWLocal.class);
        resourceManager.registerClass(AWMappingRepetition.class);
        resourceManager.registerClass(AWMessageArgument.class);
        resourceManager.registerClass(AWMetaTemplateConditional.class);
        resourceManager.registerClass(AWMethodInvocation.class);
        resourceManager.registerClass(AWContent.class);
        resourceManager.registerClass(AWParentTemplate.class);
        resourceManager.registerClass(AWPrimitiveString.class);
        resourceManager.registerClass(AWRedirect.class);
        resourceManager.registerClass(AWFor.class);
        resourceManager.registerClass(AWResourceUrl.class);
        resourceManager.registerClass(AWScope.class);
        resourceManager.registerClass(AWSetValue.class);
        resourceManager.registerClass(AWString.class);
        resourceManager.registerClass(AWIncludeBlock.class);
        resourceManager.registerClass(AWBlock.class);
        resourceManager.registerClass(AWIncludeComponent.class);
        resourceManager.registerClass(AWWhile.class);

        // register default a: namespace for our components
        AWNamespaceManager ns = AWNamespaceManager.instance();
        AWNamespaceManager.AllowedGlobalsResolver globals = new AWNamespaceManager.AllowedGlobalsResolver(null);
        AWComponent.initializeAllowedGlobalTags(globals);

        AWNamespaceManager.Resolver resolver = new AWNamespaceManager.Resolver(globals);
        resolver.addIncludeToNamespace("a", new AWNamespaceManager.Import(
                Arrays.asList("ariba.ui.aribaweb"),
                Arrays.asList("AW")));
        resolver.addIncludeToNamespace("x", new AWNamespaceManager.Import(
                Arrays.asList("ariba.ui.aribaweb"),
                Arrays.asList("AWX")));     // FIXME -- rename remaining "AWX" components and get rid of this
        ns.registerResolverForPackage("ariba.ui.aribaweb", resolver);

        AWClasspathResourceDirectory.autoRegisterJarResources(resourceManager);

        // default for using XMLHttpRequests
        String def = AWUtil.getenv("ARIBA_AW_USE_XMLHTTP");
        if (def != null) AWRequestContext.UseXmlHttpRequests = Boolean.parseBoolean(def);
    }

    static String _AppName = null;
    public static boolean IsJarApplication = false;

    // Invoked by aribaweb.properties to set defaults if we're in an Open Source AW (jar) application
    static public void initializeForJarApplication ()
    {
        // we default to true for OSAW apps, false for Ariba apps (unless overridded by ARIBA_AW_USE_XMLHTTP)
        AWRequestContext.UseXmlHttpRequests = true;
        IsJarApplication = true;
        
        // Set util temp dir (used, for instance, in storing file uploads)
        _AppName = (String)AWClasspathResourceDirectory.aribawebPropertyValue("app-name");
        if (_AppName == null) _AppName = "AWApp";
        String tempName = System.getProperty("java.io.tmpdir");
        try {
            SystemUtil.setSharedTempDirectory(new File(tempName, _AppName).getAbsolutePath());
            // todo: unique local directory (using PID equivalent?)
            SystemUtil.setLocalTempDirectory(new File(tempName, _AppName).getAbsolutePath());
        } catch (java.security.AccessControlException e) {
            // Swallow
        }
    }

    private static String urlForPath (String path)
    {
        return AWXDebugResourceActions.urlForResourceInDirectory(null, path, "");
    }

    private static void registerAWSourcePath (String path, AWMultiLocaleResourceManager resourceManager)
    {
        resourceManager.registerResourceDirectory(path, urlForPath(path));
        File resourceDir = new File(path, "resource/webserver/branding/ariba");
        if (resourceDir.exists() && resourceDir.isDirectory()) {
                        resourceManager.registerResourceDirectory(resourceDir.getPath(), urlForPath(resourceDir.getPath()), false);
        }
        resourceDir = new File(path, "resource/webserver/branding");
        if (resourceDir.exists() && resourceDir.isDirectory()) {
                        resourceManager.registerResourceDirectory(resourceDir.getPath(), urlForPath(resourceDir.getPath()), false);
        }
        resourceDir = new File(path, "resource/webserver");
        if (resourceDir.exists() && resourceDir.isDirectory()) {
                        resourceManager.registerResourceDirectory(resourceDir.getPath(), urlForPath(resourceDir.getPath()), false);
        }
    }

    static Set<String> _awJarNameSuffixes ()
    {
        Set<String> result = new HashSet();
        for (String fullName : AWClasspathResourceDirectory.referencedAWJarNames()) {
            int index = fullName.indexOf('.');
            String shortName = (index != -1) ? fullName.substring(index+1) : fullName;
            result.add(shortName);
        }
        return result;
    }

    private static final Pattern _BuildProjectName = Pattern.compile(
            "<project.*?\\s+name=\\\"(\\w+)\\\".+?>");
    /**
        Attempt to deduce the jar that would be produced by the given source path.
        Looks for a build.xml at the path and uses the name attribute of the project
        @return the likely jar name (sans entension) or null
     */
    static String _jarNameSuffixForSourceDirectory (String path)
    {
        File buildFile = new File(path, "build.xml");
        if (buildFile.exists()) {
            String contents = AWUtil.stringWithContentsOfFile(buildFile);
            Matcher m = _BuildProjectName.matcher(contents);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    public String initResourceUrl ()
    {
        return null;
    }

    /**
        This is somewhat temporary and is added for the benefit of the ariba.ui.validation package.  At some point we will rationalize all of this and this may go away.
    */
    public String resourceUrl ()
    {
        return _resourceUrl;
    }

    public void setSharedInstance (AWServerApplication application)
    {
        SharedInstance = application;
    }

    public static AWServerApplication sharedInstance ()
    {
        return SharedInstance;
    }

    public void setName (String nameString)
    {
        _name = nameString;
    }

    public String name ()
    {
        return _name;
    }

    //////////////////
    // User Defaults
    //////////////////
    public boolean isDebuggingEnabled ()
    {
        return "true".equals(System.getProperty("ariba.aribaweb.Debug"));
    }

    public boolean initIsRapidTurnaroundEnabled ()
    {
        return !StringUtil.nullOrEmptyOrBlankString(AWUtil.getenv("ARIBA_AW_SEARCH_PATH"));
    }

    public boolean isStateValidationEnabled ()
    {
        return Log.aribawebvalidation_state.isDebugEnabled();
    }

    public boolean isRapidTurnaroundEnabled ()
    {
        return IsRapidTurnaroundEnabled;
    }

    public boolean isStatisticsGatheringEnabled ()
    {
        return IsStatisticsGatheringEnabled;
    }

    public boolean requiresThreadSafety ()
    {
        return AllowsConcurrentRequestHandling && !IsRapidTurnaroundEnabled;
    }

    ///////////////////////
    // Request Dispatching
    ///////////////////////
    protected AWResponse _dispatchRequest (AWRequest request)
    {
        AWResponse response = null;
        AWRequestHandler requestHandler = null;
        requestHandler = requestHandlerForRequest(request);

        if (requestHandler == null) {
            response = handleMalformedRequest(request,
                                              "Unable to locate request handler");
            debugString(
                    "Unable to locate request handler for key " + request.requestHandlerKey());
        }
        if (response == null) {
            response = requestHandler.handleRequest(request);
        }
        return response;
    }

    AWRequestHandler requestHandlerForRequest (AWRequest request)
    {
        AWRequestHandler requestHandler;
        String requestHandlerKey = request.requestHandlerKey();
        if (requestHandlerKey == null) {
            requestHandler = defaultRequestHandler();
            if (requestHandler == null) {
                throw new AWGenericException("*** DefaultRequestHandler was not initialized.");
            }
        }
        else {
            requestHandler = requestHandlerForKey(requestHandlerKey);
        }
        return requestHandler;
    }

    public AWResponse handleMalformedRequest (String message)
    {
        return handleMalformedRequest(null, message);
    }

    public AWResponse handleMalformedRequest (AWRequest request, String message)
    {
        AWResponse response = createResponse(request);
        response.setStatus(AWResponse.StatusCodes.ErrorNotFound);
        response.appendContent("<h1>HTTP 404: Not Found Error</h1><br>" + HTML.escape(message));
        return response;
    }

    protected AWResponse dispatchCooperativeMultitaskingRequest (AWRequest request)
    {
        return _dispatchRequest(request);
    }

    public AWResponse dispatchRequest (AWRequest request)
    {
        AWResponse response = null;
        AWFileResource.notifyNewRequest();
        if (AllowsConcurrentRequestHandling) {
            response = _dispatchRequest(request);
        }
        else {
            lockRequestHandlingForRequest();
            try {
                response = dispatchCooperativeMultitaskingRequest(request);
            }
            finally {
                unlockRequestHandlingForRequest();
            }
        }
        return response;
    }

    private static PerformanceCheck _PerformanceChecker = null;

    public PerformanceCheck defaultPerformanceCheck ()
    {
        if (_PerformanceChecker == null) {
            // warn at 15 sec, error at one minute
            PerformanceCheck checker = new PerformanceCheck(4000,30000, null);

            // Page sizes: warn at 130K, error at 200K
            checker.addChecker(
                new PerformanceChecker(
                    AWBaseResponse.PageGenerationSizeCounter,
                    130000,
                    200000));

            _PerformanceChecker = checker;
        }
        return _PerformanceChecker;
    }

    public void handleGarbageCollectionIssues ()
    {
        if (_requestsSinceLastFlush > ResourceManagerFlushThreshold) {
            flushResourceManager();
            ResourceManagerFlushThreshold *= 2;
            _requestsSinceLastFlush = -1;
        }
        _requestsSinceLastFlush++;
    }

    ////////////////
    // Threading
    ////////////////
    public boolean allowsConcurrentRequestHandling ()
    {
        return false;
    }

    public void lockRequestHandlingForRequest ()
    {
        _cooperativeMultithreadingLock.lock();
    }

    public void unlockRequestHandlingForRequest ()
    {
        _cooperativeMultithreadingLock.unlock();
    }

    public void temporarilyUnlockRequestHandling ()
    {
        _cooperativeMultithreadingLock.unlock();
    }

    public void relockRequestHandling ()
    {
        _cooperativeMultithreadingLock.relock();
    }

    ////////////////
    // Debugging
    ////////////////
    public void logString (String message)
    {
        Log.aribaweb.debug(message);
    }

    public void debugString (String message)
    {
        if (AWConcreteServerApplication.IsDebuggingEnabled) {
            Log.aribaweb.debug(message);
        }
    }

    /**
     * Provide the option of disabling/enabling user community functionality.
     * Products may over-ride this by implementing:
     * initUserCommunityEnabled
     * to turn on or off the user community functionality
     * by using their own configuration.
     */
    public static boolean isUserCommunityEnabled()
    {
        return IsUserCommunityEnabled;
    }

    /**
     * Get the Ariba user community product url
     * @return String
     */
    public static String getAribaUserCommunityUrl ()
    {
        return _aribaUserCommunityUrl;
    }

    /**
     * Get the window size at which In Situ pane will be displayed in folded manner.
     * Each application can override this value.
     * @return int
     */
    public static int getFoldInSituWindowSize ()
    {
        return _foldInSituOnWindowSize;
    }
}
