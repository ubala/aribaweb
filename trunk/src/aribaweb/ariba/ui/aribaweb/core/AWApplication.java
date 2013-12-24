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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWApplication.java#39 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBrand;
import ariba.ui.aribaweb.util.AWBrandManager;
import ariba.ui.aribaweb.util.AWNodeManager;
import ariba.ui.aribaweb.util.AWParameters;
import ariba.util.http.multitab.MaximumTabExceededException;
import java.util.Map;
import javax.servlet.http.HttpSession;

/**
    A process wide coordinator of AribaWeb processing.  AWApplication (and its main concrete implementations
    {@link AWConcreteServerApplication},
    {@link AWConcreteApplication}, and {@link ariba.ui.servletadaptor.AWServletApplication}
    provide methods links to via services (e.g. {@link #resourceManager()}) and methods overridden,
    acts as a factory for vital objects involved in request handling (e.g. {@link #createSession(AWRequestContext)},
    {@link #createRequestContext(AWRequest)}, and provides hooks to be overridden by
    by subclasses to affect process flow (e.g. {@link #handleSessionRestorationError(AWRequestContext)}.
 */
public interface AWApplication extends AWServerApplication
{
    ///////////////////
    // Global Defaults
    ///////////////////
    public void setPageCacheSize (int intValue);
    public int pageCacheSize ();
    public String webserverDocumentRootPath ();
        // Returns the web server docroot directory
    public String resourceFilePath ();
        // returns the web server resource URL
    public String resourceURL ();
    public String adaptorUrl ();
    public String adaptorUrlSecure ();
    public void setUseEmbeddedKeyPathes (boolean useEmbeddedKeyPathes);
    public boolean useEmbeddedKeyPathes ();
    public String getApplicationType ();

    //////////////////////
    // Page Creation
    //////////////////////
    public AWComponent createPageWithName (String componentName,
                                           AWRequestContext requestContext);
    public String mainPageName ();
    public AWResponseGenerating mainPage (AWRequestContext requestContext);

    //////////////////////
    // Session Management
    //////////////////////
    public AWSession createSession (AWRequestContext requestContext);
    public int sessionTimeout ();
    public HttpSession createHttpSession (AWRequest request);
    public HttpSession restoreHttpSession (AWRequest request, String sessionId);
    public void archiveHttpSession (HttpSession httpSession);
    public void checkoutHttpSessionId (String sessionId);
    public void checkinHttpSessionId (String sessionId);
    public boolean isHttpSessionCheckedOut (String sessionId);
    public int activeHttpSessionCount ();
    public void terminate ();
    public void setTerminateApplicationPassword (String terminateApplicationPassword);
    public String terminateApplicationPassword ();
    public void registerSession (AWSession session);
    public void deregisterSession (AWSession session);
    public AWSessionStatusManager getSessionStatusManager ();
    public AWResponseGenerating monitorSessionStatsPage (AWRequestContext requestContext);

    ////////////////////////
    // Req Handling Support
    ////////////////////////
    public AWRequestContext createRequestContext (AWRequest request);
    public AWResponseGenerating handleException (AWRequestContext requestContext,
                                                 Exception exception);
    public AWResponseGenerating handleSessionRestorationError (
        AWRequestContext requestContext);
    public AWResponseGenerating handleSessionValidationError (
        AWRequestContext requestContext, Exception exception);
    public AWResponseGenerating handleComponentActionSessionValidationError (
        AWRequestContext requestContext, Exception exception);
    public AWResponseGenerating handleSiteUnavailableException (
        AWRequestContext requestContext);
    public AWResponseGenerating handleRemoteHostMismatchException (
        AWRequestContext requestContext, AWRemoteHostMismatchException exception);
    public AWResponseGenerating handleMaxWindowException (AWRequestContext requestContext,
                                          MaximumTabExceededException exception);

    public String applicationUrl (AWRequest request);
    public void setRefusingNewSessions (boolean refusingNewSessions);
    public void setRefuseNewSessionsPassword (String refuseNewSessionsPassword);
    public String refuseNewSessionsPassword ();
    public String directActionClassNameForKey (String classNameKey);
    public int hibernationDepth ();

    ////////////////////////
    // Component Management
    ////////////////////////
    public AWComponentDefinition createComponentDefinitionForNameAndClass (
        String componentName, Class componentClass);
    public AWComponentDefinition componentDefinitionForName (String componentName);

    ////////////////////////
    // Statistics
    ////////////////////////
    public void logActionMessage (String actionLogMessage);

    //////////////////////
    // Monitoring Support
    //////////////////////
    public AWMonitorStats monitorStats ();
    public AWResponseGenerating monitorStatsPage (AWRequestContext requestContext);
    public Map customKeyValueStats ();
    public boolean isValidRemoteHost (AWRequest request);

    //////////////////////
    // Shutdown Support
    //////////////////////
    public void initiateShutdown ();

    //////////////////////
    // Session Validation Support
    //////////////////////
    public void setSessionValidator (AWSessionValidator validator);
    public void assertExistingSession (AWRequestContext requestContext);
    public void assertValidSession (AWRequestContext requestContext);

    //////////////////////
    // Request Validation Support
    //////////////////////
    public void validateRequest (AWRequestContext requestContext);

    //////////////////////
    // Brand Support
    //////////////////////
    public AWBrandManager getBrandManager ();
    public AWBrand getBrand (AWRequestContext requestContext);

    //////////////////////
    // Node Support
    //////////////////////
    public AWNodeManager getNodeManager ();
    public void assertValidNode (AWRequestContext requestContext,
                                 String directActionClassName, String actionName);
    /**
        Returns the node name of the local server.
        @return name of the local node
        @aribaapi ariba
    */
    public String getNodeName ();

    //////////////////////
    // Component Configuration
    //////////////////////
    public void registerComponentConfigurationSource (
        Class componentClass, AWComponentConfigurationSource source);

    public AWComponentConfigurationSource getComponentConfigurationSource (
        Class componentClass);

    ////////////////////////////
    // Deprecated - remove soon
    ////////////////////////////
    public void sweepExpiredObjects ();
    public void checkinHttpSession (HttpSession httpSession);
    public void checkoutHttpSession (HttpSession httpSession);
    public boolean useServletEnginesSession ();
    public void timeoutForObject (Object object);

    public AWParameters getConfigParameters ();

    public int getPollInterval ();
}
