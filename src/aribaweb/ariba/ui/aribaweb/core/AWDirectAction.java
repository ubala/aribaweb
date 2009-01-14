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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWDirectAction.java#55 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWBrand;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWMemoryStats;
import ariba.ui.aribaweb.util.AWMutableRefCount;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import ariba.util.core.PerformanceState;
import ariba.util.core.ProgressMonitor;
import ariba.util.core.HTML;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValueException;
import ariba.util.shutdown.ShutdownManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpSession;

abstract public class AWDirectAction extends AWBaseObject
{
    private static final String GifExtension = "gif";
    private static final int GifExtensionLength = GifExtension.length();
    protected static final AWEncodedString InvalidMachineMessage =
        new AWEncodedString("You may not perform this action from the machine you are on.");
    public static final String PasswordKey = "awpwd";
    public static final String EnableKey = "enable";
    public static final String DefaultActionName = "default";
    public static final String AWImgActionName = "awimg";
    public static final String AWResActionName = "awres";
    public static final String AWResTestActionName = "test_awres";
    public static final String PingActionName = "ping";
    public static final String ProgressCheckActionName = "progressCheck";

    private static GrowOnlyHashtable ActionMethodNames = new GrowOnlyHashtable();
    // ** This is the abstract superclass for all DirectAction classes.  The default class is DirectAction, which must be provided by the user's application if DirectActions are to be used.
    private AWRequestContext _requestContext;

    // ** Thread Safety Considerations: direct actions are not shared by multiple threads -- no locking required.

    public void init (AWRequestContext requestContext)
    {
        _requestContext = requestContext;
        this.init();
    }

    /////////////////////////
    // RequestContext Access
    /////////////////////////
    public AWRequestContext requestContext ()
    {
        return _requestContext;
    }

    public AWApplication application ()
    {
        return _requestContext.application();
    }

    public AWSession session ()
    {
        return _requestContext.session();
    }

    public HttpSession httpSession ()
    {
        return _requestContext.httpSession();
    }

    public AWRequest request ()
    {
        return _requestContext.request();
    }

    ///////////////////
    // Page Creation
    ///////////////////
    public AWComponent pageWithName (String pageName)
    {
        // ** This needs some more thought on the session (should I use existingSession or something like that??)
        AWComponent component = _requestContext.pageWithName(pageName);
        _requestContext.setPage(component.page());
        return component;
    }

    ///////////////////
    // Action Handling
    ///////////////////
    public static final String ActiveHttpSessionCountActionName = "activeHttpSessionCount";

    public AWResponse activeHttpSessionCountAction ()
    {
        AWApplication application = application();
        AWResponse newResponse = application.createResponse(request());
        newResponse.appendContent("Active session count is: " +
                                  application.activeHttpSessionCount());
        return newResponse;
    }

    ///////////////////
    // Basic actions
    ///////////////////
    public AWResponseGenerating defaultAction ()
    {
        AWResponse newResponse = application().createResponse(request());
        newResponse.appendContent(
            "<b>Note:</b>  You should add your own defaultAction()" +
            " to your DirectAction class.");
        return newResponse;
    }

    protected boolean isValidRemoteHost ()
    {
        return application().isValidRemoteHost(request());
    }

    private void terminateApplication (AWApplication application)
    {
        AWApplicationTerminationThread applicationTerminationThread =
            new AWApplicationTerminationThread(application);
        applicationTerminationThread.start();
    }

    static boolean isServerManagementAction (AWRequest request)
    {
        return !StringUtil.nullOrEmptyOrBlankString(
                request.formValueForKey(AWDirectAction.PasswordKey));
    }

    public AWResponse refuseNewSessionsAction ()
    {
        AWApplication application = application();
        AWResponse newResponse = application.createResponse(request());
        if (isValidRemoteHost()) {
            String actualPassword = request().formValueForKey(PasswordKey, false);
            String targetPassword = application.refuseNewSessionsPassword();
            if ((targetPassword != null) && targetPassword.equals(actualPassword)) {
                application().setRefusingNewSessions(true);
                newResponse.appendContent("Successfully set refuseNewSessions to true.");
            }
            else {
                newResponse.appendContent(
                    "You must supply a valid password to perform this operation.");
            }
        }
        else {
            newResponse.appendContent(InvalidMachineMessage);
        }
        return newResponse;
    }

    public AWResponse shutdownAction ()
    {
        AWApplication application = application();
        AWResponse newResponse = application.createResponse(request());
        if (isValidRemoteHost()) {
            String actualPassword = request().formValueForKey(PasswordKey, false);
            String targetPassword = application.terminateApplicationPassword();
            if ((targetPassword != null) && targetPassword.equals(actualPassword)) {
                ShutdownManager.shutdown(ShutdownManager.NormalExitNoRestart,
                                        ShutdownManager.DirectActionKey);
                newResponse.appendContent(
                    "Successfully initiated node shutdown.  Node will restart when all" +
                    " user sessions are closed.");
            }
            else {
                newResponse.appendContent(
                    "You must supply a valid password to perform this operation.");
            }
        }
        else {
            newResponse.appendContent(InvalidMachineMessage);
        }
        return newResponse;
    }

    public AWResponseGenerating killAction ()
    {
        AWApplication application = application();
        AWResponse newResponse = application.createResponse(request());
        if (isValidRemoteHost()) {
            String actualPassword = request().formValueForKey(PasswordKey, false);
            String targetPassword = application.terminateApplicationPassword();
            if ((targetPassword != null) && targetPassword.equals(actualPassword)) {
                String nodeName = application.getNodeName();
                newResponse.appendContent(Fmt.S("%s exiting normally.", nodeName));
                terminateApplication(application);
            }
            else {
                newResponse.appendContent(
                    "You must supply a valid password to perform this operation.");
            }
        }
        else {
            newResponse.appendContent(InvalidMachineMessage);
        }
        return newResponse;
    }

    public AWResponseGenerating logResponsesAction ()
    {
        AWApplication application = application();
        AWResponse newResponse = application.createResponse(request());
        if (isValidRemoteHost()) {
            String enableString = request().formValueForKey(EnableKey, false);
            boolean enable = "1".equals(enableString);
            AWConcreteApplication.IsRequestLoggingEnabled = enable;
            AWConcreteApplication.IsResponseLoggingEnabled = enable;
            newResponse.appendContent("Request/Response logging was " +
                                      (enable ? "enabled" : "disabled"));
        }
        else {
            newResponse.appendContent(InvalidMachineMessage);
        }
        return newResponse;
    }

    public AWResponseGenerating monitorStatsAction ()
    {
        AWResponseGenerating monitorStatsResults = null;
        if (isValidRemoteHost()) {
            monitorStatsResults = application().monitorStatsPage(requestContext());
        }
        else {
            AWResponse response = application().createResponse(request());
            response.appendContent(InvalidMachineMessage);
            monitorStatsResults = response;
        }
        return monitorStatsResults;
    }

    public AWResponseGenerating monitorSessionStatsAction ()
    {
        AWResponseGenerating results = null;
        if (isValidRemoteHost()) {
            results = application().monitorSessionStatsPage(requestContext());
        }
        else {
            AWResponse response = application().createResponse(request());
            response.appendContent(InvalidMachineMessage);
            results = response;
        }
        return results;
    }

    public AWResponseGenerating testSessionStatsAction ()
    {
        AWResponseGenerating results = null;
        if (isValidRemoteHost()) {
            application().getSessionStatusManager().test(requestContext());
            AWResponse response = application().createResponse(request());
            response.appendContent("Test run");
            results = response;
        }
        else {
            AWResponse response = application().createResponse(request());
            response.appendContent(InvalidMachineMessage);
            results = response;
        }
        return results;
    }

    public static String brandUrlForResourceNamed (AWRequestContext requestContext,
                                                   String resourceName, AWBrand brand)
    {
        // Use full URL -- so URLs work when served into remote dashboard content
        AWDirectActionUrl directActionUrl =
            AWDirectActionUrl.checkoutFullUrl(requestContext);

        directActionUrl.setDirectActionName(
            brand.isResourceTestMode(requestContext) ?
                AWDirectAction.AWResTestActionName :
                AWDirectAction.AWResActionName);

        String resourceUrl = directActionUrl.finishUrl();

        resourceUrl =
            StringUtil.strcat(resourceUrl, "/",
                              brand.getName(), "/",
                              brand.getSessionVersion(requestContext), "/",
                              resourceName);
        return resourceUrl;
    }

        // Date Formatter used to print the date in the HTML header of the
        // resources served by the awresAction
    private SimpleDateFormat fmt  = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

    public AWResponse test_awresAction ()
    {
        return awresAction(false);
    }

    public AWResponse awresAction ()
    {
        return awresAction(true);
    }

    //
    // todo:
    // 1) generate the locale onto the URL so we don't use
    // the system locale to determine the locale of the resource
    // 2) review the structure of the single locale / multi-locale resource
    // manager trees to make sure we do are duplicating the resource hierarchies
    // for each brand and at the global level
    //
    public AWResponse awresAction (boolean enableBrowserCache)
    {
        // Note that this method must be sessionless -- the response from this
        // method should be able to be cached remotely using the URL of the
        // current request as the key.   As a result, we cannot use any
        // information not contained in the URL (ie, in the session) to
        // retrieve the resource.
        AWRequest request = request();
        AWApplication application = application();
        AWResponse response = application.createResponse(request);

        if (!((AWConcreteApplication)application).allowBrandingImages()) {
            response.appendContent("action not allowed");
            return response;
        }

        response.setBrowserCachingEnabled(enableBrowserCache);

        //
        // construct the filename
        //
        // awres/brand/version/blah/blah/filename.jpg
        String[] requestHandlerPathComponents = request.requestHandlerPath();
        String brandName = requestHandlerPathComponents[1];
        String version = requestHandlerPathComponents[2];
        String filename = requestHandlerPathComponents[3];
        if (requestHandlerPathComponents.length > 4) {
            FastStringBuffer sb = new FastStringBuffer();
            for (int i=3; i < requestHandlerPathComponents.length -1; i++) {
                sb.append(requestHandlerPathComponents[i]);
                sb.append("/");
            }
            sb.append(requestHandlerPathComponents[requestHandlerPathComponents.length -1]);
            filename = sb.toString();
        }

        if (!isValidResourceFilename(filename)) {
            response.appendContent("Invalid request: " + HTML.escape(filename));
            return response;
        }             

        //
        // set the content type
        //
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            String ext = filename.substring(dot + 1);
            AWContentType type = AWContentType.contentTypeForFileExtension(ext);
            if (type != null) {
                response.setContentType(type);
            }
        }

        //
        // get the right resource manager
        //
        AWResourceManager rm = application.resourceManager();
        rm = rm.resolveBrand(brandName, version);

        AWResource resource = rm.resourceNamed(filename, true);

        if (resource == null) {
            response.appendContent(Fmt.S("Cannot find resource named: %s",HTML.escape(filename)));
        }
        else {
            if (Log.aribawebResource_brand.isDebugEnabled()) {
                String location = resource.url();
                if (resource instanceof AWFileResource) {
                    AWFileResource file = (AWFileResource)resource;
                    location = file._fullPath();
                }
                Log.aribawebResource_brand.debug("Found: %s", location);
            }

            String lastModified = fmt.format(new Date(resource.lastModified()));
            response.setHeaderForKey(lastModified, "Last-Modified");

            if (enableBrowserCache) {
                // 24 hours
                response.setHeaderForKey("max-age=86400", "Cache-control");
            }

            if (resource instanceof AWFileResource) {
                AWFileResource file = (AWFileResource)resource;
                response.setContentFromFile(file._fullPath());
            }
            else {
                ((AWBaseResponse)response).setContentFromStream(resource.inputStream());
            }
        }
        return response;
    }

    private static Pattern invalidImageFilename = Pattern.compile(".*\\.\\..*");
    private static Pattern validResourceFilename = Pattern.compile(".*\\.(gif|jpg|css|ico|js)");

    protected static boolean isValidResourceFilename(String filename)
    {
        if (invalidImageFilename.matcher(filename).matches()) {
            return false;
        }
        return validResourceFilename.matcher(filename).matches();
    }

    public AWResponse awimgAction ()
    {
        AWRequest request = request();
        AWApplication application = application();
        AWResponse response = application.createResponse(request);
        AWSession session = requestContext().session(false);
        String filename = request.formValueForKey("name", false);

        if (!((AWConcreteApplication)application).allowBrandingImages()) {
            response.appendContent("action not allowed");
            return response;
        }
        if (!isValidResourceFilename(filename)) {
            response.appendContent("Invalid request: " + HTML.escape(filename));
            return response;
        }

        AWResource imageResource;

        if (session != null) {
            imageResource = session.resourceManager().resourceNamed(filename,true);
        }
        else {
            imageResource = application.resourceManager().resourceNamed(filename,true);
        }

        if (imageResource == null) {
            response.appendContent("Cannot find image named: " + HTML.escape(filename));
        }
        else {
            response.setContentFromFile(((AWFileResource)imageResource)._fullPath());
            int indexOfDot = filename.lastIndexOf('.');
            if (filename.regionMatches(true, indexOfDot + 1, GifExtension, 0, GifExtensionLength)) {
                response.setContentType(AWContentType.ImageGif);
            }
            else {
                response.setContentType(AWContentType.ImageJpeg);
            }
        }

        // 24 hours
        response.setBrowserCachingEnabled(true);
        response.setHeaderForKey("max-age=86400", "Cache-control");

        return response;
    }

    public AWResponseGenerating pingAction ()
    {
        AWApplication application = application();
        AWResponse newResponse = application.createResponse(request());
        newResponse.appendContent("<html><body>ping</body></html>");
        return newResponse;
    }

    public AWResponseGenerating progressCheckAction ()
    {
        String key = ((AWBaseRequest)request()).initSessionId();
        ProgressMonitor progress = ProgressMonitor.getInstanceForKey(key);
        AWApplication application = application();
        AWResponse newResponse = application.createResponse(request());
        if (progress != null) {
            newResponse.appendContent(HTML.escapeUnsafe(progress.generateMessage()));
        } else {
            // Special marker value for No Request in Progress
            newResponse.appendContent("--NO_REQUEST--");
        }

        return newResponse;
    }

    /////////////////
    // Dispatching
    /////////////////
    private String actionMethodName (String actionName)
    {
        String actionMethodName = (String)ActionMethodNames.get(actionName);
        if (actionMethodName == null) {
            synchronized (ActionMethodNames) {
                actionMethodName = (String)ActionMethodNames.get(actionName);
                if (actionMethodName == null) {
                    actionMethodName = StringUtil.strcat(actionName, "Action");
                    ActionMethodNames.put(actionName, actionMethodName);
                }
            }
        }
        return actionMethodName;
    }

    public AWResponseGenerating performActionNamed (String actionName)
    {
        // Record request location for trace
        if (PerformanceState.threadStateEnabled()) {
            ((PerformanceState.getThisThreadHashtable())).setSourcePage(this.getClass().getName());
            ((PerformanceState.getThisThreadHashtable())).setSourceArea(actionName);
        }

        String actionMethodName = actionMethodName(actionName);
        AWResponseGenerating actionResults = null;
        try {
            actionResults = (AWResponseGenerating)FieldValue.getFieldValue(this, actionMethodName);
        }
        catch (FieldValueException fieldValueException) {
            String message = Fmt.S("Unable to invoke action named \"%s\"", actionName);
            logString(fieldValueException.getMessage());
            if (AWConcreteApplication.IsDebuggingEnabled) {
                Log.logStack(Log.aribaweb_request, message);
            }
            actionResults = application().handleMalformedRequest(request(), message);
        }
        return actionResults;
    }

    protected AWResponseGenerating handleSessionRestorationException (
        String actionName,
        AWSessionRestorationException sessionRestorationException)
    {
        throw sessionRestorationException;
    }

    protected boolean skipValidation (String actionName)
    {
        return ProgressCheckActionName.equals(actionName) ||
               AWResActionName.equals(actionName) || AWImgActionName.equals(actionName);
    }

    /**
        Allows AWSessionValidator to evaluate before the direct action is called.
        Should be overridden by page components that do not require session validation.

        In this case, no actions defined in that subclass will have session validation
        enabled and thus will need to explicitly enable session validation on a per action basis by calling
        validateSession(requestContext) at the point that a valid session is required.
        @return true by default
    */
    protected boolean shouldValidateSession ()
    {
        return true;
    }

    /**
        Allows AWSessionValidator to evaluate before the actual direct action is performed.
        Called before the invocation of all direct actions.
        @param requestContext
    */
    protected void validateSession (AWRequestContext requestContext)
    {
        application().assertValidSession(requestContext);

        // Notify observers of the direct action only after session has been validated.
        // Dashboard picks off piggyback payload.
        notifyObservers(requestContext);
    }

    /**
        Allows AWNodeValidator to evaluate before the direct action is called.
        Should be overridden by page components that require node validation.

        By default, no actions defined in a subclass of AWDirectAction will have node
        validation enabled and can explicitly enable node validation on a per action basis
        by calling validateNode(requestContext) at the point that a valid node is
        required.
        @return false by default
    */
    protected boolean shouldValidateNode ()
    {
        return false;
    }

    /**
        Allows AWNodeValidator to evaluate before the actual direct action is performed.
        If an AWNodeManager is defined in the application class, then the AWNodeValidator
        registered for the directActionClassName + actionName pair is retrieved and
        checked for validity.  If the current node is not valid, then an
        AWNodeValidationException will be thrown.

        See AWDirectActionRequestHandler for handling of AWNodeValidationException.

        @param requestContext
        @aribaapi private
    */
    protected void validateNode (AWRequestContext requestContext,
                                 String directActionClassName, String actionName)
    {
        application().assertValidNode(requestContext, directActionClassName, actionName);
    }

    /**
     * Allows request validation before executing the request.  If this method returns
     * false, verification can still occur on an action by action basis by directly
     * calling validateRequest(RequestContext).
     * @return true if all requests in the DirectAction class should be verified
     */
    protected boolean shouldValidateRequest ()
    {
        return false;
    }

    /**
     * Hook into AWApplication.validateRequest.
     * @param requestContext current request
     */
    protected void validateRequest (AWRequestContext requestContext)
    {
        application().validateRequest(requestContext);
    }

    /////////////////
    // Debugging
    /////////////////
    public AWResponse memoryAction ()
    {
        AWResponse response = application().createResponse(request());
        response.appendContent("<html><body><pre>");
        AWMutableRefCount[] sortedRefCounts = AWMemoryStats.sortedRefCounts();
        if (sortedRefCounts != null) {
            response.appendContent((ListUtil.arrayToList(sortedRefCounts)).toString());
        }
        else {
            response.appendContent("Memory stats not enabled.");
        }
        response.appendContent("</pre></body></html>");
        return response;
    }

    public AWResponse awpreloadAction ()
    {
        AWResponse response = application().createResponse(request());
        if (application().isDebuggingEnabled()) {
            logString("**** PRE-LOADING all templates");
            long millis = System.currentTimeMillis();
            ((AWConcreteApplication)application()).preloadAllTemplates();
            String message = "---> preloading templates took (millis): " + (System.currentTimeMillis() - millis);
            logString(message);
            response.appendContent(message);
        }
        else {
            response.appendContent("only allowed when debugging enabled");
        }
        return response;
    }

    public AWResponse formTestAction ()
    {
        AWResponse response = application().createResponse(request());
        if (application().isDebuggingEnabled()) {
            response.appendContent("<html><body><b>Form Test</b><pre>");
            Map formValues = requestContext().formValues();
            Iterator keys = formValues.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String)keys.next();
                String[] value = (String[])formValues.get(key);
                response.appendContent(Fmt.S("key:%s value: %s\n", key, value[0]));
            }
            response.appendContent("</pre></body></html>");
        }
        else {
            response.appendContent("only allowed when debugging enabled");
        }
        return response;
    }

    public interface DirectActionObserver {
        // Observers can peek at form values
        public void notifyAfterSessionValidation(AWRequestContext requestContext);
    }

    protected static List<DirectActionObserver> _observers = ListUtil.list();

    public static void registerObserver (DirectActionObserver observer)
    {
        _observers.add(observer);
    }

    public static void notifyObservers (AWRequestContext requestContext)
    {
        for (DirectActionObserver observer : _observers) {
            observer.notifyAfterSessionValidation(requestContext);
        }
    }
}

final class AWApplicationTerminationThread extends Thread
{
    AWApplication _application = null;

    AWApplicationTerminationThread (AWApplication application)
    {
        super();
        _application = application;
    }

    public void run ()
    {
        try {
            sleep(1000);
        }
        catch (InterruptedException exception) {
            // ignore
            exception = null;
        }
        _application.terminate();
    }
}
