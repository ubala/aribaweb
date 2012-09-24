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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWDirectActionRequestHandler.java#67 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.AWNodeChangeException;
import ariba.ui.aribaweb.util.AWNodeManager;
import ariba.ui.aribaweb.util.AWNodeValidator;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import java.util.List;

public final class AWDirectActionRequestHandler extends AWConcreteRequestHandler
{
    public static final String DefaultDirectActionClassName = "DirectAction";
    public static final String DefaultActionName = "default";
    private static final int ClassNameIndex = 0;
    private static final int ActionNameIndex = 1;
    public static final String DirectActionRedirectKey = "dard";

    public static AWDirectActionRequestHandler SharedInstance;
    public static final String DefaultHttpPort = "80";

    private List _defaultRequestFilter;

    // ** Thread Safety Considerations: no ivars -- no locking required.

    public void init (AWApplication application, String requestHandlerKey)
    {
        super.init(application, requestHandlerKey);
        SharedInstance = this;
        AWDirectActionUrl.setup(adaptorPrefix(), application.name(),
            applicationNameSuffix(), alternateSecurePort());
        _defaultRequestFilter = ListUtil.list();
    }

    /*
        Add a a direct action request handler filter.  The filter will be invoked
        on the request prior to invoking the direct action.
        Filters will be execute in the order that are added to the list, but the
        filters can be added to the list in different order from run to run.
        So no assumtions should be made about the execution order of the filters.
    */
    public void addDefaultRequestFilter (AWDefaultRequestFilter filter)
    {
        addRequestFilter(filter, false);
    }

    /*
        We are synchrozing for the case were allowDuplicate is set to false.
        For this case we need to synchronize to avoid a race condition that will
        allow duplicates.
    */
    private synchronized void addRequestFilter (AWDefaultRequestFilter newFilter,
                                                boolean allowDuplicates)
    {
        if (allowDuplicates) {
            _defaultRequestFilter.add(newFilter);
        }
        else {
            boolean found = false;
            int size = _defaultRequestFilter.size();
            for (int i=0; i<size; i++) {
                AWDefaultRequestFilter existingFilter =
                    (AWDefaultRequestFilter)_defaultRequestFilter.get(i);
                if (newFilter.sameAs(existingFilter)) {
                    found = true;
                }
            }
            if (!found) {
                _defaultRequestFilter.add(newFilter);
            }
        }
    }

    public void setAdaptorPrefix (String prefixString)
    {
        AWDirectActionUrl.setDefaultAdaptorUrl(prefixString);
        super.setAdaptorPrefix(prefixString);
    }

    public void setApplicationNameSuffix (String suffixNameString)
    {
        AWDirectActionUrl.setDefaultApplicationSuffix(suffixNameString);
        super.setApplicationNameSuffix(suffixNameString);
    }

    public static String getActionName (AWRequestContext requestContext)
    {
        String actionName = DefaultActionName;
        String[] requestHandlerPathComponents =
            requestContext.request().requestHandlerPath();
        if (requestHandlerPathComponents != null) {
            int requestHandlerPathComponentsLength = requestHandlerPathComponents.length;
            if (requestHandlerPathComponentsLength > 0) {
                actionName = requestHandlerPathComponents[0];
            }
        }
        return actionName;
    }

    private String[] parseDirectActionRequest (AWRequest request)
    {
        String actionName = DefaultActionName;
        String className = DefaultDirectActionClassName;
        AWApplication application = application();

        Class directActionClass = null;
        String[] requestHandlerPathComponents = request.requestHandlerPath();
        if (requestHandlerPathComponents != null) {
            AWNodeManager nodeManager = application.getNodeManager();
            if (nodeManager != null) {
                requestHandlerPathComponents =
                    nodeManager.filterUrlForNodeCallback(requestHandlerPathComponents);
            }
            int requestHandlerPathComponentsLength = requestHandlerPathComponents.length;
            if (requestHandlerPathComponentsLength > 0) {
                actionName = requestHandlerPathComponents[0];
            }
            // Hack -- check explicitly for awres so we can use
            // path rather than query string -- for webserver caching of resources --
            // example: http://host/ACM/Main/ad/awres/realm/imagename.jpg
            // if we can modify the webserver cache to understand query strings
            // then we can switch to
            // http://host/ACM/Main/ad/awimg?realm=xxxx&filename=xxxx
            if (requestHandlerPathComponentsLength > 1 &&
                !AWDirectAction.AWResActionName.equals(actionName)) {
                className = application.directActionClassNameForKey(
                    requestHandlerPathComponents[1]);

                // try to find a class for the className
                if (StringUtil.nullOrEmptyOrBlankString(className)) {
                    className = DefaultDirectActionClassName;
                }
                else {
                    directActionClass = AWUtil.classForName(className);
                    if (directActionClass == null) {
                        directActionClass =
                            application.resourceManager().classForName(className);
                        if (directActionClass == null) {
                            className = DefaultDirectActionClassName;
                        }
                    }
                }
            }
        }
        String [] directActionName = new String[2];
        directActionName[ClassNameIndex] = className;
        directActionName[ActionNameIndex] = actionName;
        return directActionName;
    }

    private boolean attemptSavePage (AWRequestContext requestContext, AWPage page)
    {
        // attemptSavePage checks for either a valid sessionId or an existing session
        // in the requestContext
        // session id, no existing session -- normal request that does not cause the
        //      session to get set in the requestContext during take or invoke
        // no session id, existing session (before append) -- request that causes a new
        //      session to be associated with the requestContext during take, invoke,
        //      or session validation (aka sso)
        // no session id, existing session (after append) -- request that causes a new
        //      session to be associated with the requestContext during append to response
        // see Note [sessionless requests] below
        boolean pageSaved = false;
        if (page.pageComponent().shouldCachePage() &&
            (!StringUtil.nullOrEmptyOrBlankString(requestContext.request().sessionId()) ||
            requestContext.session(false) != null)) {
            Log.aribaweb.debug(
                    "AWDirectActionRequestHandler: session exists -- saving page for %s",
                    page.pageComponent().getClass().getName());
            requestContext.session().savePage(page);
            pageSaved = true;
        }
        return pageSaved;
    }

    public AWResponse handleRequest (AWRequest request, AWRequestContext requestContext)
    {
        AWResponseGenerating actionResults = processAction(request, requestContext);
        AWResponse response = null;
        if (actionResults instanceof AWComponent) {
            Log.aribaweb.debug("checking to clear browser history for %s",
                                request.uri());

            response = clearBrowserHistory(requestContext,
                                           (AWComponent)actionResults);
        }
        else {
            response = actionResults.generateResponse();
        }
        return response;
    }

    public AWResponseGenerating processAction (AWRequest request, AWRequestContext requestContext)
    {
        AWApplication application = application();
        AWResponseGenerating response = null;

        String[] directActionName = parseDirectActionRequest(request);

        // isReloadable() returns true if we have hotswap reloading enabled
        String className = application.directActionClassNameForKey(directActionName[ClassNameIndex]);
        directActionName[ClassNameIndex] = className;

        Class directActionClass =
            application.resourceManager().classForName(className);

        if (AWUtil.getClassLoader().isReloadable(className)) {
            Class reloadedClass = AWUtil.getClassLoader().checkReloadClass(directActionClass);
            if (reloadedClass != directActionClass) {
                application.resourceManager().removeClass(className);
                directActionClass = reloadedClass;
            }
        }
        String actionName = directActionName[ActionNameIndex];
        requestContext.setCurrentDirectAction(className, actionName);
        AWDirectAction directAction = null;
        try {
            directAction = (AWDirectAction)directActionClass.newInstance();
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new AWGenericException(illegalAccessException);
        }
        catch (InstantiationException instantiationException) {
            throw new AWGenericException(
                "*** Error: cannot instantiate direct action class named \"" +
                directActionName[ClassNameIndex] + "\"", instantiationException);
        }

        directAction.init(requestContext);

        try {
            if (request.formValueForKey(AWRequestContext.RefreshRequestKey) != null) {
                // if this is a refresh request (ie. forward track over a redirect from
                // another app, then see if we have a valid session and we have a
                // page to refresh.  Otherwise, this app must have been restarted but
                // the other app was not so there is a mismatch in state.  In this case,
                // actually execute the direct action.
                AWSession session = requestContext.session(false);
                if (session != null) {
                    AWPage page = session.restoreCurrentPage();
                    if (page != null) {
                        if (page.pageComponent().isBoundary()) {
                            // need to forward track here and then redirect to
                            // refresh the page ...
                            requestContext.setHistoryAction(
                                AWSession.ForwardTrackRequest);
                            session.initRequestType();
                            page = session.restoreNextPage(page);
                        }
                        AWRedirect redirectComponent =
                            (AWRedirect)application.createPageWithName(
                                AWRedirect.PageName, requestContext);
                        String effectiveUrl =
                            AWComponentActionRequestHandler.SharedInstance.refreshUrl(
                                requestContext);

                        redirectComponent.setUrl(effectiveUrl);
                        redirectComponent.setSelfRedirect(true);
                        requestContext.setPage(redirectComponent.page());
                        response = requestContext.generateResponse();
                    }
                }
            }

            if (response == null) {
                AWResponseGenerating actionResults = null;

                if (!request.hasHandler()) {
                    // If there is not handler specified, then the user is
                    // accessing the /Main page and that is the only time
                    // that we want to execute the filters.
                    for (int i=0; i<_defaultRequestFilter.size(); i++) {
                        AWDefaultRequestFilter filter =
                            (AWDefaultRequestFilter)_defaultRequestFilter.get(i);
                        filter.execute(requestContext);
                    }
                }
                if (!directAction.skipValidation(actionName)) {
                    if (directAction.shouldValidateTopFrame(actionName))
                    {
                        response = directAction.validateTopFramePost(requestContext);
                        if (response != null) {
                            return response;
                        }

                    }

                    if (!AWDirectAction.isServerManagementAction(request) &&
                        directAction.shouldValidateNode(actionName)) {
                        directAction.validateNode(requestContext,
                                                  directActionName[ClassNameIndex],
                                                  actionName);
                    }

                    if (directAction.shouldValidateSession(actionName)) {
                        directAction.validateSession(requestContext);
                    }

                    if (directAction.shouldValidateRequest(actionName)) {
                        directAction.validateRequest(requestContext);
                    }
                }

                actionResults =
                    directAction.performActionNamed(directActionName[ActionNameIndex]);

                Assert.that(actionResults != null,
                            "Direct action result cannot be null.");

                // Allow response to replace itself (see AWRedirect for example)
                if (actionResults instanceof AWResponseGenerating.ResponseSubstitution) {
                    actionResults = ((AWResponseGenerating.ResponseSubstitution)actionResults).replacementResponse();
                }
                
                return actionResults;
            }
        }
        catch (AWNodeChangeException e) {
            AWNodeValidator nv = e.getNodeValidator();
            AWResponseGenerating handlerResults =
                nv.handleNodeValidationException(requestContext);
            nv.terminateCurrentSession(requestContext);
            response = handlerResults.generateResponse();
        }
        catch (AWSessionValidationException exception) {
            try {
                if (AWConcreteApplication.IsDebuggingEnabled) {
                    Log.aribaweb.debug("AWDirectActionRequestHandler: handling session " +
                                       "validation exception");
                }

                // if there is a default validator, run it here
                AWNodeValidator nv = AWNodeManager.getDefaultNodeValidator();
                if (nv != null && !nv.isValid(requestContext)) {
                    Log.aribaweb_nodeValidate.debug("AWDirectActionRequestHandler: invalid node. " +
                                       "NodeValidator generating response.");
                    AWResponseGenerating handlerResults =
                        nv.handleNodeValidationException(requestContext);
                    nv.terminateCurrentSession(requestContext);
                    response = handlerResults.generateResponse();
                }
                else {
                    AWResponseGenerating handlerResults =
                        handleSessionValidationError(requestContext, exception);
                    // If there is not valid HttpSession, create a new one to enable
                    // load balancers which use the sessionid.
                    if (requestContext.httpSession(false) == null) {
                        // request.getSession(true);
                        requestContext.createHttpSession();
                        Log.aribaweb_session.debug("AWDirectActionRequestHandler: create HttpSession");
                    }
                    if (exception instanceof AWClearBrowserHistoryException) {
                        AWComponent actionResultsComponent = (AWComponent)handlerResults;
                        response = clearBrowserHistory(requestContext,
                            actionResultsComponent);
                    }
                    else if (requestContext.session(false) != null &&
                        handlerResults instanceof AWComponent) {
                        AWComponent actionResultsComponent = (AWComponent)handlerResults;
                        AWPage actionResultsPage = actionResultsComponent.page();
                        attemptSavePage(requestContext, actionResultsPage);
                    }
                    if (response == null) {
                        response = handlerResults.generateResponse();
                    }
                }
            }
            catch (RuntimeException e) {
                AWGenericException newException =
                    new AWGenericException(
                        "Error occurred while handling session validation.  Please make" +
                        " sure session validation is configured properly.",e);
                return handleException(requestContext, newException).generateResponse();
            }
        }
        catch (AWSessionRestorationException exception) {
            requestContext.createHttpSession();
            response = handleSessionRestorationError(requestContext).generateResponse();
        }
        catch (AWSiteUnavailableException e) {
            response = handleSiteUnavailableException(requestContext);
        }
        catch (AWRemoteHostMismatchException exception) {
            response = handleRemoteHostMismatchException(requestContext, exception);
        }
        catch (Throwable t) {
            response = handleUncaughtException(requestContext, t);
        }
        return response;
    }

    /*
        Internally dispatch (as if the direct action had come in from the outside) and return the result
    */
    public AWResponseGenerating internalDispatch (String url, AWRequestContext requestContext)
    {
        Log.domsync.debug("Internal direct action dispatch to URL: %s", url);
        AWRequest originalRequest = requestContext.request();
        AWBaseRequest request = AWBaseRequest.createInternalRequest(url, (AWBaseRequest)originalRequest);

        AWResponseGenerating response;
        try {
            requestContext._overrideRequest(request);
            response = processAction(request,  requestContext);
        } finally {
            requestContext._overrideRequest(originalRequest);
        }
        return response;
    }

    /*
       Saves the result of the current request on the session, does a 302
       redirect into a direct action to clear the browser history.  In the
       handler of redirect request, it returns the page that was saved on
       the request.
    */
    protected AWResponse clearBrowserHistory (AWRequestContext requestContext,
                                            AWComponent actionResultsComponent)
    {
        AWPage actionResultsPage = actionResultsComponent.page();
        actionResultsPage.ensureAwake(requestContext);
        // This rewrites the DirectAction request URL into a ComponentAction
        // request URL in the browser by using a redirect.
        // Note [sessionless requests]: ordering of condition is important.  Let
        // component declare "non-refresh" (no cache or no redirect) before
        // attempting to interact with the session -- allows for "sessionless"
        // direct actions.  This is important since we always attempt to
        // rendevous with the session id now.  Fixes race condition at logout
        // in suite integrated mode.  LogoutAck from another app starts processing
        // before ClientLogout for authenticator is complete.  RequestContext for
        // LogoutAck starts off with a valid sessionid, but by the time it gets
        // here, the session has been invalidated.  LogoutAck needs to be
        // completely sessionless.
        if (AWComponentActionRequestHandler.IsFormPostRedirectEnabled &&
            actionResultsComponent.shouldCachePage() &&
            actionResultsComponent.shouldRedirect() &&
            requestContext.session(false) != null &&
            !requestContext.isIncrementalUpdateRequest()) {
            // This saves the actual page and not the redirect page.  When the
            // redirect fires it will restore the saved page, which should be
            // treated as a refreshRequest (no applyValues/invokeAction).

            // associate a response id with the current request for page
            // history maintenance
            AWSession session = requestContext.session();
            session.incrementResponseId();

            AWApplication application = application();
            AWRedirect redirectComponent =
                (AWRedirect)application.createPageWithName(
                    AWRedirect.PageName, requestContext);
            String effectiveUrl =
                AWComponentActionRequestHandler.SharedInstance.refreshUrl(
                    requestContext);
            effectiveUrl = AWUtil.urlAddingQueryValue(effectiveUrl, DirectActionRedirectKey, "1");
            redirectComponent.setUrl(effectiveUrl);
            redirectComponent.setSelfRedirect(true);
            requestContext.setPage(redirectComponent.page());
            actionResultsPage.ensureAsleep();
            Log.aribaweb.debug("Sending redirect to clear browser history");
        }
        else {
            requestContext.setPage(actionResultsPage);
        }

        // We need to check for the existence of a session and save the page
        // before generateResponse in order to maintain uniformity with
        // AWComponentActionRequestHandler.
        // Since generateResponse() can cause a new session to be
        // associated with the request, we need to make sure we try save
        // the page after generateResponse if the page was not already saved.
        boolean pageSaved =
            attemptSavePage(requestContext, actionResultsPage);

        AWResponse response = requestContext.generateResponse();

        if (!pageSaved) {
            attemptSavePage(requestContext, actionResultsPage);
        }
        return response;
    }

    public static boolean wasDirectActionRedirect (AWRequestContext requestContext)
    {
        return requestContext.request().formValueForKey(DirectActionRedirectKey) != null;
    }
    
    private AWResponse handleUncaughtException (AWRequestContext requestContext,
                                                Throwable t)
    {
        AWResponse response = null;
        Exception exception = (t instanceof Exception)
                ? (Exception)t
                : new AWGenericException(t);        
        try {
            AWResponseGenerating handleExceptionResults = handleException(requestContext,
                                                                          exception);
            if (handleExceptionResults instanceof AWComponent) {
                AWPage handlerPage = ((AWComponent)handleExceptionResults).page();
                if (requestContext.request().sessionId() != null ||
                    requestContext.session(false) != null) {
                    requestContext.session().savePage(handlerPage);
                }
                requestContext.setPage(handlerPage);
                response = requestContext.generateResponse();
            }
            else {
                response = handleExceptionResults.generateResponse();
            }
        }
        catch (RuntimeException secondException) {
            response = defaultHandleException(requestContext,
                                              exception).generateResponse();
        }
        return response;
    }

    public AWResponse handleRequest (AWRequest request)
    {
        AWRequestContext requestContext = application().createRequestContext(request);
        AWResponse response = null;
        try {
            response = requestContext.handleRequest(request, this);
        }
        catch (AWSessionRestorationException exception) {
            requestContext.createHttpSession();
            response = handleSessionRestorationError(requestContext).generateResponse();
        }
        catch (AWRemoteHostMismatchException exception) {
            response = this.handleRemoteHostMismatchException(requestContext, exception).generateResponse();
        }
        catch (Exception exception) {
            response = handleUncaughtException(requestContext, exception);
        }
        finally {
                // record & playback
            // record & playback
            _debugDoRecordAndPlayback(requestContext, request, response);

            // Note: it seems as though this sleep should come after the checkInExistingHttpSession,
            // However this was changed around at some point for some unknown reason, so we don't want
            // to break whatever that was.  So sleep should come before checkInExistingHttpSession.
            // Also note that, in requestContext.sleep(), we don't null out the httpSession to accomodate
            // this reversed logic.  See also: AWComponentActionRequestHandler
            try {
                requestContext.sleep();
            }
            finally {
                requestContext.checkInExistingHttpSession();
            }
        }
        return response;
    }

    private String applicationNumber (AWRequestContext requestContext)
    {
        String applicationNumberComponent = "";
        if (requestContext != null) {
            String applicationNumber = requestContext.request().applicationNumber();
            if (applicationNumber != null) {
                applicationNumberComponent = StringUtil.strcat(applicationNumber, "/");
            }
        }
        return applicationNumberComponent;
    }

    public String requestHandlerUrl (AWRequestContext requestContext)
    {
        return StringUtil.strcat(adaptorPrefix(), application().name(),
            applicationNameSuffix(), applicationNumber(requestContext),
            AWConcreteApplication.DirectActionRequestHandlerKey, "/");
    }


}
