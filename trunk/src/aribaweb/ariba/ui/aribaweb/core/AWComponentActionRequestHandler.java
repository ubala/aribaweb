/*
    Copyright (c) 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWComponentActionRequestHandler.java#96 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWNodeChangeException;
import ariba.ui.aribaweb.util.AWNodeManager;
import ariba.ui.aribaweb.util.AWNodeValidator;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.PerformanceState;
import ariba.util.core.ProgressMonitor;
import ariba.util.core.StringUtil;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.http.multitab.MultiTabSupport;

import javax.servlet.http.HttpSession;

public final class AWComponentActionRequestHandler extends AWConcreteRequestHandler
{
    public static boolean IsFormPostRedirectEnabled = true;
    public static final String SenderKey = "awsn";
    public static final String FormSenderKey = "awsnf";
    public static final String FormComponentIdKey = "awfid";
    public static final String DropDestinationKey = "awdd";

    private static final AWEncodedString SenderKeyEncoded = new AWEncodedString("awsn");

    /*
    private static final AWEncodedString QuestionMark = new AWEncodedString("?");
    private static final AWEncodedString Ampersand = new AWEncodedString("&");
    */
    private static final String SessionIdKeyEquals = AWRequestContext.SessionIdKey + "=";
    private static final String ResponseIdKeyEquals =
        AWRequestContext.ResponseIdKey + "=";

    public static AWComponentActionRequestHandler SharedInstance;
    private AWEncodedString[] _requestHandlerUrl;
    private AWEncodedString[] _fullRequestHandlerUrl;
    private AWEncodedString[] _fullRequestHandlerUrlSecure;
    private AWEncodedString[] _fullRequestHandlerBaseUrl;

    private static final String HistoryKey = "awh";
    private static final String HistoryKeyEquals = HistoryKey + "=";

    public static final String BackTrackActionName = "b";
    public static final String ForwardTrackActionName = "f";
    public static final String RefreshActionName = "r";
    public static final String HistoryScriptActionName = "s";
    public static final String DebugRerenderCountKey = "awd_rr";
    public static final String DebugRerenderAsRefreshKey = "awd_inc";

    private String[] RefreshUrl;
    private String[] FullRefreshUrl;

    private static final String ComponentName = "AWComponentActionRequestHandler";

    // ** Thread Safety Considerations: see requestHandlerUrl below

    public void init (AWApplication application, String requestHandlerKey)
    {
        super.init(application, requestHandlerKey);
        SharedInstance = this;
        MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
        _requestHandlerUrl = new AWEncodedString[multiTabSupport.maximumNumberOfTabs()];
        _fullRequestHandlerUrl = new AWEncodedString[multiTabSupport
                .maximumNumberOfTabs()];
        _fullRequestHandlerUrlSecure = new AWEncodedString[multiTabSupport
                .maximumNumberOfTabs()];
        _fullRequestHandlerBaseUrl = new AWEncodedString[multiTabSupport
                .maximumNumberOfTabs()];
        RefreshUrl = new String[multiTabSupport.maximumNumberOfTabs()];
        FullRefreshUrl = new String[multiTabSupport.maximumNumberOfTabs()];
    }

    //////////////////////
    // Url Generation (where should this really go?)
    //////////////////////
    public AWEncodedString requestHandlerUrlEncoded (AWRequest request)
    {
        return requestHandlerUrlEncoded(request, false);
    }

    /**
     Returns the appropriate encoded request handler URL for the current tab.
     This is either a full or relative URL depending on the value of ``fullUrl``
     and automatically determines if SSL should be used. The result will be
     a concatenation of the desired following URL parts:
     {https?}//{domain}/{web-app-name}/_?{servlet-name}/{tab-index}/{application-number}/{request-handler-key}/
     The values returned by this function are cached in a tab aware way, so
     they only need to be created once per-tab, per-application.
     * @param request The AWRequest.
     * @param fullUrl Return a fully qualified URL, instead of relative.
     * @return A base URL to append extra URL parts to.
     */
    private AWEncodedString requestHandlerUrlEncoded (
            AWRequest request, boolean fullUrl)
    {
        MultiTabSupport tabSupport = MultiTabSupport.Instance.get();
        int tabIndex = tabSupport.uriToTabNumber(request.uri(), 0);

        AWEncodedString fullRequestHandlerUrlSecure =
                _fullRequestHandlerUrlSecure[tabIndex];
        AWEncodedString fullRequestHandlerUrl =
                _fullRequestHandlerUrl[tabIndex];
        AWEncodedString requestHandlerUrlEncoded =
                _requestHandlerUrl[tabIndex];

        if (fullRequestHandlerUrlSecure == null || fullRequestHandlerUrl == null
                || requestHandlerUrlEncoded == null) {
            // no need to synchronize this -- worst case it'll get recomputed
            // twice to the same thing.
            String applicationNumber = request.applicationNumber();
            applicationNumber = (applicationNumber == null) ?
                "" : StringUtil.strcat(applicationNumber, "/");
            // note: the use of application name here, means something different
            //  that it does in the tomcat sense.
            String tabAwareAppName = tabSupport.tabNumberToUri(application().name(),
                    applicationNameSuffix(),
                    tabIndex, request.uri());

            // relative URL
            String requestHandlerUrl = StringUtil.strcat(
                    adaptorPrefix(), tabAwareAppName, applicationNumber,
                    requestHandlerKey());
            requestHandlerUrlEncoded = AWEncodedString.sharedEncodedString(
                    requestHandlerUrl);
            _requestHandlerUrl[tabIndex] = requestHandlerUrlEncoded;

            // full URL
            String fullAdaptorUrl = fullAdaptorUrl(false);
            requestHandlerUrl = StringUtil.strcat(
                    fullAdaptorUrl, fullAdaptorUrl.endsWith("/") ? "" : "/",
                    tabAwareAppName, applicationNumber, requestHandlerKey());
            fullRequestHandlerUrl = AWEncodedString.sharedEncodedString(
                    requestHandlerUrl);
            _fullRequestHandlerUrl[tabIndex] = fullRequestHandlerUrl;

            // full SSL URL
            fullAdaptorUrl = fullAdaptorUrl(true);
            if (fullAdaptorUrl != null) {
                requestHandlerUrl = StringUtil.strcat(
                        fullAdaptorUrl, fullAdaptorUrl.endsWith("/") ? "" : "/",
                        tabAwareAppName, applicationNumber,
                        requestHandlerKey());

                fullRequestHandlerUrlSecure =
                        AWEncodedString.sharedEncodedString(requestHandlerUrl);
                _fullRequestHandlerUrlSecure[tabIndex] =
                        fullRequestHandlerUrlSecure;

            }
        }

        if (fullUrl) {
            return !request.isSecureScheme() ? fullRequestHandlerUrl :
                    fullRequestHandlerUrlSecure;
        }
        else {
            return requestHandlerUrlEncoded;
        }
    }

    public String requestHandlerUrl (AWRequest request, boolean fullUrl)
    {
        return requestHandlerUrlEncoded(request, fullUrl).string();
    }

    public String requestHandlerUrl (AWRequest request)
    {
        return requestHandlerUrl(request, false);
    }

    private String formatUrl (AWRequestContext requestContext,
                              AWEncodedString requestHandlerUrl,
                              String senderId, String sessionId,
                              AWEncodedString responseId, AWEncodedString frameName)
    {
        String requestHandlerUrlString = requestHandlerUrl.string();
        String sessionIdString = sessionId;
        String responseIdString = (responseId == null) ? null : responseId.string();
        String formattedUrl = null;
        if (AWConcreteApplication.IsCookieSessionTrackingEnabled) {
            if (senderId == null) {
                formattedUrl = StringUtil.strcat(requestHandlerUrlString, "?",
                    AWRequestContext.ResponseIdKey, "=", responseIdString);
            }
            else {
                formattedUrl = StringUtil.strcat(requestHandlerUrlString, "?",
                    SenderKey, "=", senderId,
                    "&",
                    AWRequestContext.ResponseIdKey, "=", responseIdString);
            }
        }
        else {
            formattedUrl = StringUtil.strcat(requestHandlerUrlString, "?",
                SessionIdKeyEquals, sessionIdString,
                "&",
                ResponseIdKeyEquals, responseIdString);
            if (senderId != null) {
                formattedUrl = StringUtil.strcat(
                    formattedUrl, "&", SenderKey, "=", senderId);
            }
        }
        formattedUrl = StringUtil.strcat(formattedUrl, "&",
                AWRequestContext.SessionSecureIdKeyEquals,
                sessionSecureId(requestContext));
        if (frameName != null) {
            formattedUrl = StringUtil.strcat(formattedUrl, "&",
                AWRequestContext.FrameNameKey, "=", frameName.string());
        }
        return formattedUrl;
    }

    private void appendUrl (AWRequestContext requestContext,
                            AWEncodedString requestHandlerUrl,
                            AWEncodedString senderId,
                            String sessionId,
                            AWEncodedString responseId,
                            AWEncodedString frameName)
    {
        AWResponse response = requestContext.response();
        response.appendContent(requestHandlerUrl);
        response.appendContent(AWConstants.QuestionMark);
        if (senderId != null) {
            response.appendContent(SenderKeyEncoded);
            response.appendContent(AWConstants.Equals);
            response.appendContent(senderId);
            response.appendContent(AWConstants.Ampersand);
        }
        if (!AWConcreteApplication.IsCookieSessionTrackingEnabled) {
            response.appendContent(AWRequestContext.SessionIdKey);
            response.appendContent(AWConstants.Equals);
            response.appendContent(sessionId);
            response.appendContent(AWConstants.Ampersand);
        }
        response.appendContent(AWRequestContext.SessionSecureIdKey);
        response.appendContent(AWConstants.Equals);
        response.appendContent(sessionSecureId(requestContext));
        response.appendContent(AWConstants.Ampersand);

        response.appendContent(AWRequestContext.ResponseIdKey);
        response.appendContent(AWConstants.Equals);
        response.appendContent(responseId);
        if (frameName != null) {
            response.appendContent(AWConstants.Ampersand);
            response.appendContent(AWRequestContext.FrameNameKey);
            response.appendContent(AWConstants.Equals);
            response.appendContent(frameName);
        }
    }

    private AWEncodedString urlPrefix (AWRequestContext requestContext)
    {
        int tabIndex = requestContext.getTabIndex();
        AWEncodedString requestHandlerUrl = _requestHandlerUrl[tabIndex];

        if (requestHandlerUrl == null) {
            requestHandlerUrl = requestHandlerUrlEncoded(
                    requestContext.request());
            _requestHandlerUrl[tabIndex] = requestHandlerUrl;
        }

        return requestContext.isMetaTemplateMode() ?
            fullRequestHandlerBaseUrl(requestContext):
                requestHandlerUrl;
    }

    private String sessionId (AWRequestContext requestContext)
    {
        return requestContext.httpSession().getId();
    }

    private String sessionSecureId (AWRequestContext requestContext)
    {
        return requestContext.session().sessionSecureId();
    }

    public String urlWithSenderId (AWRequestContext requestContext,
                                   AWEncodedString senderId)
    {
        return formatUrl(requestContext, urlPrefix(requestContext), senderId.string(),
                         sessionId(requestContext), requestContext.responseId(),
                         requestContext.frameName());
    }

    public void appendUrlWithSenderId (AWRequestContext requestContext,
                                       AWEncodedString senderId)
    {
        appendUrl(requestContext, urlPrefix(requestContext), senderId,
            sessionId(requestContext), requestContext.responseId(),
            requestContext.frameName());
    }

    /**
    JRHEE:  Added (with review by Charles) in order to support AWXScriptedLink,
    which requires a full url - i.e., a url with the full http specification
    such as http://www.foo.com.  Otherwise, IE4 complains with a javascript
    Access Denied error if you try to set a non-full url in a
    window's location href.
    */
    protected AWEncodedString fullRequestHandlerBaseUrl (
            AWRequestContext requestContext)
    {
        int tabIndex = requestContext.getTabIndex();
        AWEncodedString fullRequestHandlerBaseUrl =
                _fullRequestHandlerBaseUrl[tabIndex];

        if (fullRequestHandlerBaseUrl == null) {
            AWRequest request = requestContext.request();
            String applicationNumber = request.applicationNumber();
            applicationNumber = (applicationNumber == null) ?
                "" : StringUtil.strcat(applicationNumber, "/");
            String fullRequestHandlerUrl =
                StringUtil.strcat(addTrailingSlash(
                        fullAdaptorUrlForRequest(request)),
                    application().name(), applicationNameSuffix(),
                    applicationNumber, requestHandlerKey());

            MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
            fullRequestHandlerBaseUrl = new AWEncodedString(
                    multiTabSupport.insertTabInUri(fullRequestHandlerUrl, tabIndex, true)
            );
            _fullRequestHandlerBaseUrl[tabIndex] = fullRequestHandlerBaseUrl;
        }

        return fullRequestHandlerBaseUrl;
    }

    public String fullUrlWithSenderId (AWRequestContext requestContext,
                                       AWEncodedString senderId)
    {
        return formatUrl(requestContext,
                fullRequestHandlerBaseUrl(requestContext),
                senderId.string(),
                sessionId(requestContext),
                requestContext.responseId(),
                requestContext.frameName());
    }

    public void appendFullUrlWithSenderId (AWRequestContext requestContext,
                                           AWEncodedString senderId)
    {
        appendUrl(requestContext, fullRequestHandlerBaseUrl(requestContext),
                  senderId, sessionId(requestContext), requestContext.responseId(),
                  requestContext.frameName());
    }

    public String urlWithSenderIdAndFragmentIdentifier (AWRequestContext requestContext,
                                                        AWEncodedString senderId,
                                                        String fragmentIdentifier)
    {
        String url = urlWithSenderId(requestContext, senderId);
        if (fragmentIdentifier != null) {
            url = StringUtil.strcat(url, "#", fragmentIdentifier);
        }
        return url;
    }

    public void appendUrlWithSenderIdAndFragmentIdentifier (
        AWRequestContext requestContext,
        AWEncodedString senderId,
        AWEncodedString fragmentIdentifier)
    {
        appendUrlWithSenderId(requestContext, senderId);
        if (fragmentIdentifier != null) {
            AWResponse response = requestContext.response();
            response.appendContent("#");
            response.appendContent(fragmentIdentifier);
        }
    }

    public String effectiveUrlForFormPost (AWRequestContext requestContext,
                                           String uriString)
    {
        AWRequest request = requestContext.request();
        String senderId =
                request.formValueForKey(AWComponentActionRequestHandler.SenderKey);
        String url = formatUrl(requestContext, requestHandlerUrlEncoded(request),
                               senderId, sessionId(requestContext), request.responseId(),
                               requestContext.frameName());
        return AWDirectActionUrl.decorateUrl(requestContext, url);
    }

    //////////////////////
    // handleRequest
    //////////////////////
    private AWResponse historyRequestFilter (AWRequest request,
                                             AWRequestContext requestContext)
    {
        String historyAction = request.formValueForKey(HistoryKey);
        if (!StringUtil.nullOrEmptyOrBlankString(historyAction)) {
            // need to set up the request context before calling session since session
            // relies on these values to do initRequestType.
            if (RefreshActionName.equals(historyAction)) {
                requestContext.setHistoryAction(AWSession.RefreshRequest);
            }
            else if (BackTrackActionName.equals(historyAction)) {
                requestContext.setHistoryAction(AWSession.BacktrackRequest);
            }
            else if (ForwardTrackActionName.equals(historyAction)) {
                requestContext.setHistoryAction(AWSession.ForwardTrackRequest);
            }
            else {
                return application().handleMalformedRequest(request,
                        "unrecognized action: " + historyAction);
            }
        }
        return null;
    }

    // Return a static cacheable resource for IE backtrack support.
    // See Refresh.js:historyEvent() -- if invokes this in an iframe, and
    // this code triggers a callback to historyEvent() when loaded
    protected AWResponse checkHistoryRequest (AWRequest request,
                                              AWRequestContext requestContext)
    {
        String historyAction = request.formValueForKey(HistoryKey);
        if (!StringUtil.nullOrEmptyOrBlankString(historyAction)
                && HistoryScriptActionName.equals(historyAction)) {
            AWResponse response = application().createResponse(request);
            // set this response to be client-cacheable so
            // no server roundtrips result from spinning up backtrack state
            response.setBrowserCachingEnabled(true);
            response.setHeaderForKey("max-age=3600", "Cache-control");
            response.appendContent("<html><body onload=" +
                    "'parent.ariba.Refresh.historyEvent(window.location);'>" +
                    "AW backtrack</body></html>\n");
            return response;
        }
        return null;
    }

    private void formPostFilter (AWRequest request, AWRequestContext requestContext,
                                 AWComponent component)
    {
        AWApplication application = application();

        AWPage page = component.page();
        if (IsFormPostRedirectEnabled &&
            request.method().equalsIgnoreCase("POST") &&
            component.shouldCachePage() &&
            !requestContext.isIncrementalUpdateRequest()) {
            // This saves the actual page and not the redirect page.  When the
            // redirect fires it will restore the saved page, which should be
            // treated as a previousPage (no applyValues/invokeAction).
            AWRedirect redirectComponent = (AWRedirect)application.createPageWithName(
                                                AWRedirect.PageName, requestContext);
            String effectiveUrl = effectiveUrlForFormPost(requestContext, request.uri());
            redirectComponent.setUrl(effectiveUrl);
            redirectComponent.setSelfRedirect(true);
            requestContext.setPage(redirectComponent.page());
            page.ensureAsleep();
        }
        else {
            requestContext.setPage(page);
        }
    }

    public AWResponse handleRequest (AWRequest request, AWRequestContext requestContext)
    {
        AWResponse response = null;
        AWApplication application = application();

        // check if this is a request for static history iframe context.
        // If so, invoke before accessing session
        if ((response = checkHistoryRequest(request, requestContext)) != null) {
            return response;
        }

        // register even before we try for the session lock
        String sessionId = ((AWBaseRequest)request).initSessionId();
        ProgressMonitor.register(sessionId);

        // Appears in page loading status panel
        // use browser preferred locale since we don't have access to session yet
        String msg = localizedJavaString(ComponentName, 1,
                                         "Waiting for previous request to complete ...",
                                         AWConcreteApplication.SharedInstance.resourceManager(request.preferredLocale()));

        ProgressMonitor.instance().prepare(msg, 0);

        // Acquire session lock
        AWSession session = requestContext.session();

        response = historyRequestFilter(request, requestContext);
        if (response != null) return response;

        session.initRequestType();
        AWPage currentPage = session.restoreCurrentPage();

        // Appears in page loading status panel
        msg = localizedJavaString(ComponentName, 2, "Processing request ...",
                                  AWSingleLocaleResourceManager.ensureSingleLocale(session.resourceManager()));

        ProgressMonitor.instance().prepare(msg, 0);

        if (currentPage == null) {
            // AWReload support
            if (AWConcreteApplication.IsRapidTurnaroundEnabled) AWUtil.getClassLoader().checkForUpdates();
            return processFrontDoorRequest(requestContext);
        }
        else {
            boolean isInterrupted = false;
            requestContext.setPage(currentPage);
            if (currentPage.pageComponent().shouldValidateSession()) {
                currentPage.pageComponent().validateSession(requestContext);
            }
            if (currentPage.pageComponent().shouldValidateRequest()) {
                currentPage.pageComponent().validateRequest(requestContext);
            }
            if (! isValidNode(requestContext)) {
                throw new AWGenericException("Security Exception.  Invalid node.");
            }
            int requestType = session.requestType(request);

            AWPage perfSourcePage = null;
            String perfSourceArea = null;

            switch (requestType) {
                case AWSession.BacktrackRequest:
                case AWSession.ForwardTrackRequest: {
                    currentPage.ensureAwake(requestContext);
                    AWPage restoredPage = null;
                    AWBacktrackState backtrackState =
                        (requestType == AWSession.BacktrackRequest) ?
                        currentPage.updateBacktrackStateForBackButton() :
                        currentPage.updateBacktrackStateForForwardButton();

                    // record perf trace info
                    perfSourcePage = currentPage;
                    perfSourceArea = (requestType == AWSession.BacktrackRequest)
                            ? "backtrack" : "forwardtrack";

                    if (backtrackState != null) {
                        requestContext.setBacktrackState(backtrackState);
                        restoredPage  = currentPage;
                    }
                    else {
                        restoredPage = (requestType == AWSession.BacktrackRequest) ?
                            session.restorePreviousPage(currentPage) :
                            session.restoreNextPage(currentPage);
                        restoredPage.ensureAwake(requestContext);
                    }
                    session.savePage(restoredPage);
                    requestContext.setPage(restoredPage);
                    response = requestContext.generateResponse();
                    break;
                }
                case AWSession.InterruptedNewRequest:
                    // this is the case where a request came in from an obsolete page (e.g. the user pushed another
                    // button while the previous action was still running.  We treat this like a refresh and take
                    // them to the "currentPage" -- i.e. the one that was the result of the *first* action.
                case AWSession.RefreshRequest: {
                    // record perf trace info
                    perfSourcePage = currentPage;
                    perfSourceArea = "refresh";

                    // if the current page is a boundary (ie AWRedirect), then
                    // move the server state back one, allowing us to skip over the
                    // boundary page -- note that this may be a refresh request due to a
                    // browser backtrack (since boundary components should not be refreshable)
                    if (currentPage.pageComponent().isBoundary()) {
                        // check to see there is a previous page in the page cache, if
                        // so, then do a server side back.  If not, then do a browser
                        // backtrack.  For the case that there is a boundary page as the
                        // first (and only) page on the page cache (DashboardMain).
                        if (session.previousPage(currentPage) != null) {
                            requestContext.setHistoryAction(AWSession.BacktrackRequest);
                            session.initRequestType();
                            currentPage = session.restorePreviousPage(currentPage);
                        }
                        else {
                            // no previous page so return the browser back here ...
                            response = application.createResponse();
                            AWRedirect.browserBack(response);
                        }

                        // record perf trace info
                        perfSourcePage = currentPage;
                        perfSourceArea = "backtrack";
                    }

                    if (response == null) {
                        // AWReload support
                        if (AWConcreteApplication.IsRapidTurnaroundEnabled) AWUtil.getClassLoader().checkForUpdates();

                        currentPage.ensureAwake(requestContext);
                        session.savePage(currentPage);
                        requestContext.setPage(currentPage);
                        response = requestContext.generateResponse();
                    }
                    break;
                }
                case AWSession.NewRequest: {
                    currentPage.ensureAwake(requestContext);
                    AWResponseGenerating actionResults = null;
                    if (isInterrupted) {
                        actionResults = currentPage.pageComponent();
                    }
                    else {
                        requestContext.applyValues();
                        actionResults = requestContext.invokeActionForRequest();
                        if (actionResults == null) {
                            if (false && AWConcreteApplication.IsDebuggingEnabled) {
                                Assert.that(false,
                                    "Unable to locate item which was clicked.  Target senderId: %s",
                                    requestContext.requestSenderId());
                            }
                            else {
                                actionResults = currentPage.pageComponent();
                            }
                        }
                    }

                    // Allow response to replace itself (see AWRedirect for example)
                    if (actionResults instanceof AWResponseGenerating.ResponseSubstitution) {
                        actionResults = ((AWResponseGenerating.ResponseSubstitution)actionResults).replacementResponse();
                    }

                    if (actionResults instanceof AWComponent) {
                        AWComponent actionResultsComponent = (AWComponent)actionResults;
                        AWPage actionResultsPage = actionResultsComponent.page();
                        if (actionResultsPage != currentPage) {
                            // check if user is allowed to see this page
                            // If not, record it before propagating exception so that post
                            // auth they proceed to new page
                            if (actionResultsComponent.shouldValidateSession()) {
                                try {
                                    actionResultsComponent.validateSession(requestContext);
                                }
                                catch (AWSessionValidationException e) {
                                    session.savePage(actionResultsPage, true);
                                    throw e;
                                }
                            }

                            currentPage.truncateBacktrackState();
                        }

                        actionResultsPage.ensureAwake(requestContext);
                        formPostFilter(request,  requestContext, actionResultsComponent);
                        session.savePage(actionResultsPage);

                        /* Performance debugging feature -- replay request N times
                           -- see AWDebugPane for activation */
                        _debugRepeatRequest(
                            requestContext, actionResultsComponent, request);

                        response = requestContext.generateResponse();
                    }
                    else {
                        response = actionResults.generateResponse();
                    }
                    break;
                }
                case AWSession.NoOpRequest: {
                    response = application().createResponse();
                    break;
                }
                default: {
                    throw new AWGenericException(getClass().getName() +
                        ": unrecognized requestType: " + requestType);
                }
            }

            // Perf trace
            if ((perfSourcePage != null) && PerformanceState.threadStateEnabled()) {
                PerformanceState.Stats stats = PerformanceState.getThisThreadHashtable();
                // Don't overwrite page if we're just a refresh from a continued request
                if (stats.getSourcePage() == null) {
                    stats.setSourcePage(perfSourcePage.perfPageName());
                    stats.setSourceArea(perfSourceArea);
                }
            }
        }
        return response;
    }

    public AWResponse processFrontDoorRequest (AWRequestContext requestContext)
    {
        AWRequest request = requestContext.request();
        AWApplication application = requestContext.application();
        AWSession session = requestContext.session();

        AWResponseGenerating mainPage = application.mainPage(requestContext);
        if (mainPage instanceof AWComponent) {
            AWComponent mainPageComponent = (AWComponent)mainPage;
            // We don't check for a session restoration exception here -- it will be checked
            // when the redirect causing a new incoming request
            return AWDirectActionRequestHandler.SharedInstance.clearBrowserHistory(requestContext, mainPageComponent);
        }
        else {
            application.assertValidSession(requestContext);
            return mainPage.generateResponse();
        }
    }

    /**
        This verifies that the sesson, node role and realm all match properly.
        This is a helper method to handleRequest.
        @aribaapi private
     */
    private boolean isValidNode (AWRequestContext requestContext)
    {
        AWNodeValidator val = AWNodeManager.getComponentActionNodeValidator();
        if (val == null) {
            return true;
        }
        return val.isValid(requestContext);

    }

    static class NullResponse extends AWBaseResponse
    {
        public void setStatus (int status)
        {
        }

        public void setContentFromFile (String filePath)
        {
            Assert.that(false,"Not implemented");
        }

        public void setContent (byte[] bytes)
        {
            Assert.that(false,"Not implemented");
        }

        public void setHeaderForKey (String headerValue, String headerKey)
        {
            Assert.that(false,"Not implemented");
        }

        public AWCookie createCookie (String cookieName, String cookieValue)
        {
            Assert.that(false,"Not implemented");
            return null;
        }

        public void setHeadersForKey (String[] headerValues, String headerKey)
        {
            Assert.that(false,"Not implemented");
        }

        public void disableClientCaching ()
        {
            // nothing...
        }

    }

    private void _debugRepeatRequest (AWRequestContext requestContext,
                                AWComponent actionResultsComponent, AWRequest request)
    {
        Object repeatCountVal = (AWConcreteApplication.IsDebuggingEnabled)
            ? requestContext.get(AWComponentActionRequestHandler.DebugRerenderCountKey)
            : null;

        if (repeatCountVal != null) {
            // generate N times...
            int repeatCount = ((Integer)repeatCountVal).intValue();
            boolean repeatAsRefresh = ((Boolean)requestContext.get(
                AWComponentActionRequestHandler.DebugRerenderAsRefreshKey)).booleanValue();


            Log.aribaweb.debug("Re-rendering page %s times (%s)", repeatCount,
                        (repeatAsRefresh ? "refresh render" : "full init render"));
            while (repeatCount-- > 0) {
                _runNullRender(requestContext, actionResultsComponent, request, repeatAsRefresh);
            }
            requestContext.forceFullPageRefresh();

            Log.aribaweb.debug("Stats: %s",
                        PerformanceState.getThisThreadHashtable());
        }
    }

    protected void _runNullRender (AWRequestContext requestContext,
                                AWComponent actionResultsComponent,
                                AWRequest request,
                                boolean runAsRefresh)
    {
        // swap out the response
        NullResponse nullResponse = new NullResponse();
        nullResponse.init();
        AWResponse origResponse = requestContext.temporarilySwapReponse(nullResponse);
        requestContext.setElementId(new AWElementIdGenerator());

        if (!runAsRefresh) {
            AWPage page = new AWPage(actionResultsComponent, requestContext);
            actionResultsComponent.ensureAwake(page);
            page.ensureAwake(requestContext);
            formPostFilter(request,  requestContext, actionResultsComponent);
        }
        requestContext.generateResponse();

        // restore original response
        requestContext.setElementId(new AWElementIdGenerator());
        requestContext.restoreOriginalResponse(origResponse);
    }

    private AWResponse processExceptionHandlerResults (
        AWResponseGenerating handlerResults,
        AWRequestContext requestContext,
        AWSession session)
    {
        AWResponse response = null;
        if (handlerResults instanceof AWComponent) {
            AWPage handlerPage = ((AWComponent)handlerResults).page();
            if (session != null && handlerPage.pageComponent().shouldCachePage()) {
                session.savePage(handlerPage);
                requestContext.setPage(handlerPage);
                response = requestContext.generateResponse();
            }
        }
        if (response == null) {
            response = handlerResults.generateResponse();
        }
        return response;
    }

    public AWResponse handleRequest (AWRequest request)
    {
        // ** this could be in a base class
        AWResponse response = null;
        AWApplication application = application();
        AWRequestContext requestContext = null;
        try {
            try {
                requestContext = application.createRequestContext(request);
                try {
                    response = requestContext.handleRequest(request, this);
                }
                catch (WrapperRuntimeException e) {
                    if (e.originalException() instanceof AWSessionValidationException)
                        throw (AWSessionValidationException)e.originalException();
                    throw e;
                }

            }
            catch (AWHandledException exception) {
                AWResponseGenerating handlerResults = exception.handlerResults();
                AWSession session = requestContext.session();
                response = processExceptionHandlerResults(
                    handlerResults, requestContext, session);
            }
            catch (AWSessionRestorationException exception) {
                // redirect the user to the front door url of the application if the session id
                // that we are looking for is not found.
                // Redirect the user to main application URL.
                // Example:  If the server is busy and does not respond to the
                // webserver within a speicific amount of time, the webserver
                // will redirect the request to another node.  The node is selected
                // selected randomly, we could end up on a node that does not support
                // the current realm.  The solution is redirect the user to main
                // application url that will result in displaying the home page
                // for the user on the correct node from the realm.
                markMainPageRequest(requestContext);
                AWResponseGenerating handlerResults =
                    handleComponentActionSessionValidationError(
                        requestContext, exception);
                if (handlerResults != null) {
                    response = handlerResults.generateResponse();
                }
                else {
                    HttpSession httpSession = requestContext.createHttpSession();
                    try {
                        handlerResults = handleSessionRestorationError(requestContext);
                        response = processExceptionHandlerResults(handlerResults, requestContext, AWSession.session(httpSession));
                    }
                    catch (RuntimeException secondException) {
                        Log.aribaweb.debug(
                            "*** exception handling session restoration exception: %s",
                            ariba.util.core.SystemUtil.stackTrace(secondException));
                        response = defaultHandleException(requestContext, secondException).generateResponse();
                    }
                }
            }
            catch (AWNodeChangeException e) {
                // This could occur with softafinity when an administrator
                // redirects all users accessing the same object to a specific
                // node.
                AWNodeValidator nv = e.getNodeValidator();
                if (nv == null) {
                    nv = AWNodeManager.getDefaultNodeValidator();
                }

                Assert.that(nv != null, "AWNodeChangeException caught but no valid " +
                                        "AWNodeValidator declared in the nodeValidationException.");
                AWResponseGenerating handlerResults =
                    nv.handleNodeValidationException(requestContext);
                nv.terminateCurrentSession(requestContext);
                response = handlerResults.generateResponse();
            }
            catch (AWSessionValidationException exception) {
                // see comment in handler of AWSessionRestorationException
                // redirec the user to the main application url.
                try {
                    if (AWConcreteApplication.IsDebuggingEnabled) {
                        Log.aribaweb.debug("AWComponentActionRequestHandler: handling session validation exception");
                    }
                    AWResponseGenerating handlerResults =
                        handleComponentActionSessionValidationError(
                            requestContext, exception);
                    if (handlerResults != null) {
                        response = handlerResults.generateResponse();
                    }
                    else {
                        handlerResults = handleSessionValidationError(requestContext, exception);
                        AWSession session = requestContext.session();
                        response = processExceptionHandlerResults(handlerResults, requestContext, session);
                    }
                }
                catch (RuntimeException e) {
                    AWGenericException newException =
                        new AWGenericException(
                            "Error occurred while handling session validation.  Please make" +
                            " sure that if session validation is configured properly.",e);
                    return handleException(requestContext, newException).generateResponse();
                }
            }
            catch (AWSiteUnavailableException e) {
                AWResponseGenerating handlerResults =
                    handleSiteUnavailableException(requestContext);
                if (handlerResults != null) {
                    response = handlerResults.generateResponse();
                }
            }
            catch (AWRemoteHostMismatchException exception) {
                AWResponseGenerating result =
                    handleRemoteHostMismatchException(requestContext, exception);
                response = result.generateResponse();
            }
        }
        catch (ExceptionInInitializerError error) {
            Throwable throwable = error.getException();
            if (throwable instanceof Exception) {
                response = responseForException(requestContext, (Exception)throwable);
            }
            else {
                throwable.printStackTrace();
            }
        }
        catch (Throwable throwable) {
            Exception runtimeException = (throwable instanceof Exception)
                    ? (Exception)throwable
                    : new AWGenericException(throwable);

            boolean ignore = false;
            if (runtimeException instanceof WrapperRuntimeException) {
                WrapperRuntimeException wrapperException =
                    (WrapperRuntimeException)runtimeException;
                Exception originalException = wrapperException.originalException();
                if (originalException instanceof java.net.SocketException) {
                    String message = originalException.getMessage();
                    if (message.startsWith("Connection reset by peer")) {
                        ignore = true;
                    }
                }
            }
            if (!ignore) {
                response = responseForException(requestContext, runtimeException);
            }
        }
        finally {
            // record & playback
            _debugDoRecordAndPlayback(requestContext, request, response);

            // Note: it seems as though this sleep should come after the checkInExistingHttpSession,
            // However this was changed around at some point for some unknown reason, so we don't want
            // to break whatever that was.  So sleep should come before checkInExistingHttpSession.
            // Also note that, in requestContext.sleep(), we don't null out the httpSession to accomodate
            // this reversed logic.  See also: AWDirectActionRequestHandler
            try {
                requestContext.sleep();
            }
            finally {
                requestContext.checkInExistingHttpSession();
            }
        }
        return response;
    }

    private AWResponse responseForException (AWRequestContext requestContext,
                                             Exception exception)
    {
        AWResponse response;
        try {
            AWResponseGenerating handlerResults =
                handleException(requestContext, exception);
            AWSession session = requestContext.session();
            response = processExceptionHandlerResults(handlerResults,
                requestContext, session);
        }
        catch (RuntimeException secondException) {
            debugString("** secondException: " +
                ariba.util.core.SystemUtil.stackTrace(secondException));
            response = defaultHandleException(
                requestContext, exception).generateResponse();
        }
        return response;
    }

    public String historyRequestHandlerUrl (AWRequestContext requestContext,
                                            String actionName)
    {
        String requestUrl = requestHandlerUrl(requestContext.request());
        if (AWConcreteApplication.IsCookieSessionTrackingEnabled) {
            return StringUtil.strcat(requestUrl,"?",HistoryKey, "=", actionName,
                    "&", AWRequestContext.SessionSecureIdKey, "=",
                    sessionSecureId(requestContext));
        }
        else {
            String[] strings = {requestUrl,"?", HistoryKeyEquals, actionName,
                    "&", SessionIdKeyEquals, sessionId(requestContext), "&",
                    AWRequestContext.SessionSecureIdKeyEquals,
                    sessionSecureId(requestContext)};
            return StringUtil.strcat(strings);
        }
    }

    private String constructRefreshUrl (AWRequestContext requestContext, boolean fullUrl)
    {
        AWRequest request = requestContext.request();
        int tabIndex = requestContext.getTabIndex();
        String refreshUrl = RefreshUrl[tabIndex];
        String fullRefreshUrl = FullRefreshUrl[tabIndex];

        if (refreshUrl == null || fullRefreshUrl == null) {
            refreshUrl = StringUtil.strcat(
                    requestHandlerUrl(request), "?",
                    HistoryKeyEquals, RefreshActionName);
            fullRefreshUrl = StringUtil.strcat(
                    requestHandlerUrl(request, true), "?",
                    HistoryKeyEquals, RefreshActionName);
            RefreshUrl[tabIndex] = refreshUrl;
            FullRefreshUrl[tabIndex] = fullRefreshUrl;
        }

        String effectiveUrl = fullUrl ? fullRefreshUrl : refreshUrl;

        if (requestContext.frameName() != null) {
            effectiveUrl =
                StringUtil.strcat(effectiveUrl, "&",
                                  AWRequestContext.FrameNameKey, "=",
                                  requestContext.frameName().toString());
        }

        if (!AWConcreteApplication.IsCookieSessionTrackingEnabled) {
            effectiveUrl = StringUtil.strcat(effectiveUrl, "&",
                                     SessionIdKeyEquals, sessionId(requestContext));
        }
        effectiveUrl =
            StringUtil.strcat(effectiveUrl, "&",
                              AWRequestContext.SessionSecureIdKeyEquals,
                              sessionSecureId(requestContext));

        return AWDirectActionUrl.decorateUrl(requestContext, effectiveUrl, true);

    }

    public String fullRefreshUrl (AWRequestContext requestContext)
    {
        return constructRefreshUrl(requestContext, true);
    }

    public String refreshUrl (AWRequestContext requestContext)
    {
        return constructRefreshUrl(requestContext, false);
    }

    private static final String WasMainPageRequest = "AWCARH_MPR";
    protected static void markMainPageRequest (AWRequestContext requestContext)
    {
        requestContext.put(WasMainPageRequest, true);
    }

    protected static boolean wasMainPageRequest (AWRequestContext requestContext)
    {
        return requestContext.get(WasMainPageRequest) != null;
    }
}
