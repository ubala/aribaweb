/*
    Copyright 1996-2010 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRedirect.java#38 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.ListUtil;
import ariba.util.core.PerformanceState;
import ariba.util.core.URLUtil;
import ariba.util.core.Fmt;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

// This is subclassed by ARPRedirectError
public class AWRedirect extends AWComponent implements AWResponseGenerating.ResponseSubstitution
{
    private static final String DisallowInternalDispatchKey = "DisallowInternalDispatch";
    protected final static AWEncodedString BrowserHistoryBack = new AWEncodedString(
        "<script id='AWRefreshComplete'>history.go(-1);</script>");

    public static final String PageName = "AWRedirect";
    private static String AppHost;
    private static String AppPort;
    private static final AWEncodedString RedirectStringStart =
        new AWEncodedString("<script id='AWRefreshComplete'>");
    private static final AWEncodedString RedirectStringFinish =
        new AWEncodedString("</script>");
    private static final AWEncodedString FullPageRedirectStringStart =
        new AWEncodedString("<a id='AWRedirect' href='");
    private static final AWEncodedString FullPageRedirectStringMiddle =
        new AWEncodedString("'>");
    private static final AWEncodedString FullPageRedirectStringFinish =
        new AWEncodedString("</a>");

    private String _url;
    private boolean _isSelfRedirect = false;
    protected boolean _isRefresh = false;
    protected boolean _allowInternalDispatch = true;

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        _isSelfRedirect = false;
        _isRefresh = false;
        _url = null;
        initFromRequestContext();
    }

    // If we're not doing self redirect then we need to
    // put ourselves in the page cache so that a post backtrack forward track will
    // redirect the client to the other application.
    public boolean shouldCachePage ()
    {
        return !_isSelfRedirect;
    }

    // Indicates to the page cache that this component delineates an application boundary
    // so our js pseudo-backtrack mechanism will stop creating backtrack steps and the
    // browser backtrack will kick in and the user will be backtracked back to this
    // application.
    protected boolean isBoundary ()
    {
        return _isRefresh;
    }

    public String url ()
    {
        return _url;
    }

    public void setUrl (String url)
    {
        _url = url;
        setSelfRedirect(url.startsWith(requestContext().application().adaptorUrl()));
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        throw new AWGenericException("AWRedirect: applyValues should never be called since this page should never be cached.");
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        throw new AWGenericException("AWRedirect: invokeAction should never be called since this page should never be cached.");
    }

    public void setSelfRedirect (boolean flag)
    {
        _isSelfRedirect = flag;
    }

    /*
        Sets whether AW is allowed to try to handle this redirect by dispatching internally (wihout round-tripping
        through the browser.  This should be set to false, for instance, if the code relies on cookies being
        set to a /ad/... domain.
     */
    public boolean doesAllowInternalDispatch() {
        return _allowInternalDispatch;
    }

    public void setAllowInternalDispatch(boolean allowInternalDispatch) {
        _allowInternalDispatch = allowInternalDispatch;
    }

    public AWResponseGenerating replacementResponse () {
        if (_allowInternalDispatch && !requestContext().didAddCookies() && requestContext().request() != null
                && AWDirectActionUrl.isLocalDirectActionUrl(_url, requestContext())
                && (((AWConcreteApplication)AWConcreteApplication.sharedInstance()).requestUrlInfo(_url) != null)) {
            // internal dispatch...
            return AWDirectActionRequestHandler.SharedInstance.internalDispatch(_url, requestContext());
        }
        return this;
    }
    
    protected String getRedirectUrl (AWRequestContext requestContext)
    {
        boolean isLocal = _isSelfRedirect || AWDirectActionUrl.isLocalDirectActionUrl(_url, requestContext());
        String url = decorateUrl(_url, isLocal);

        // if this is not a self redirect and we're replaying this redirect again, then
        // we must be doing a forward track.  We need to append the RefrshRequestKey so
        // that the direct action request handler will take the appropriate action
        // (ie, refresh the current top of the page cache rather than actually reexecuting
        // the direct action which would cause the rest of the page cache to be lost)
        if (!_isSelfRedirect && _isRefresh) {
            // needed to tell directaction request handler on the other side that this is
            // a refresh request
            return AWRequestUtil.addQueryParam(url, AWRequestContext.RefreshRequestKey, True);
        }
        else {
            return url;
        }
    }

    protected static void incrementalUpdateRedirect (AWRequestContext requestContext, String url)
    {
        AWResponse redirectResponse = requestContext.application().createResponse();
        redirectResponse.appendContent(RedirectStringStart);
        redirectResponse.appendContent(AWCurrWindowDecl.currWindowDecl(requestContext));
        redirectResponse.appendContent(Fmt.S("var url = '%s'; if (ariba.Request && ariba.Dom && !ariba.Dom.IsIE6Only) ariba.Request.redirect(url); "
                                                + "else top.location.href = url;",
                                        escapeJavascript(url)));
        redirectResponse.appendContent(RedirectStringFinish);
        requestContext.setXHRRCompatibleResponse(redirectResponse);
    }

    protected static void fullPageRedirect (AWRequestContext requestContext, String url)
    {
        AWRedirect.setupHeaders(requestContext.response(), url);
    }

    public static void initiateRedirect (AWRequestContext requestContext, String url)
    {
        if (requestContext.isIncrementalUpdateRequest()) {
            Log.aribaweb.debug("AWRedirect -- incremental %s", url);
            incrementalUpdateRedirect(requestContext, url);
        }
        else {
            Log.aribaweb.debug("AWRedirect -- full page redirect %s", url);
            fullPageRedirect(requestContext, url);
        }
    }

    public static void disallowInternalDispatch (AWRequestContext requestContext)
    {
        if (requestContext != null) {
            requestContext.put(DisallowInternalDispatchKey, Boolean.TRUE);
        }
    }

    public void initFromRequestContext ()
    {
        Object disallowInternalDispatch =
            requestContext().get(DisallowInternalDispatchKey);
        if (disallowInternalDispatch != null &&
            ((Boolean)disallowInternalDispatch).booleanValue()) {
            setAllowInternalDispatch(false);
        }
    }

    private void initiateRedirect (AWRequestContext requestContext)
    {
        initiateRedirect(requestContext, getRedirectUrl(requestContext));
    }

    public static void browserBack (AWResponse response)
    {
        response.appendContent(BrowserHistoryBack);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // ok, here's the deal.
        // There are two types of history.  We have the server side
        // page cache (and request id cache) which are used to determine how much
        // pseudo-history we need to spin up on the browser.  We're able to (need to) spin
        // up pseudo-state on the browser since we're using iframes to initiate all
        // requests (to allow incremental update) and the iframes are discarded after the
        // request is complete which then removes the request from the browser history.
        // In order to allow the browser back/forward track buttons to work correctly,
        // we use js to spin up our own history on the browser when the request is
        // complete.
        // AWRedirect has to play the game correctly since it is used to move us back and
        // forth across applications.
        // There are the two problems that we are trying to solve/support:
        // 1) app1                          app2
        //    p1
        //    AWRedirect   -- redirect -->  p2
        //                 <-- backtrack--
        //    ** should be back at p1
        //
        // 2) p1
        //     AWRedirect1  -- redirect -->  p2
        //                                  p3
        //    p4          <-- redirect --  AWRedirect2
        //    p5
        //    ** backtrack order: p5, p4, p3, p2, p1
        //    The key here is that p5 to p4 is handled via our pseudo browser history
        //    and we need to handle things correctly when p4 is redisplayed so that we
        //    can use the real browser history to get us back to AWRedirect2 (which then
        //    needs to move us back to p3.
        //
        // On the initial request side, the AWRedirect is left in the page cache (and
        // marks the app boundary -- for convenience/performance since we could in theory
        // scan the page cache for a page component which has isBoundary() set to true).
        // This marker is used to properly calculate the page cache state when creating
        // our browser side pseudo-history.
        // On subsequent refresh requests (caused by browser backtrack), the existence of
        // the boundary component allows us to move the server state back one thereby
        // "skipping" the AWRedirect on the browser backtrack (see
        // AWComponentActionRequestHandler.handleRequest) -- solving problem #1 above.
        //
        // For problem #2, we have to first make sure that after the backtrack from p4,
        // the page cache is pointing to AWRedirect1.  This allows the browser backtrack
        // from p2 on app2 to a page on app1 to correctly hit AWRedirect1.  We do this by
        // having the page cache count the AWRedirect as a element for the backtrack count.
        // When we backtrack onto the AWRedirect, we leave the page cache in its current
        // state and initiate a browser back (history.go(-1)) to return to app2.  Second,
        // we need to make sure that a post backtrack forward track, from AWRedirect2 back
        // to app1 will be able to properly refresh p4.  We handle this by checking for
        // the existence of a refresh flag + the current page cache pointing to a boundary
        // component and then moving the page cache up one in this case
        // (see AWDirectActionRequestHandler.handleRequest).

        // remember it's a self redirect (to be continued)
        /*  Todo: add another flag to flush on toBeContinued
        if (PerformanceState.threadStateEnabled() && _isSelfRedirect) {
            PerformanceState.getThisThreadHashtable().setToBeContinued(true);
        }
        */
        if (requestContext.isHistoryRequest() &&
            requestContext.historyAction() == AWSession.BacktrackRequest) {
            AWResponse response = requestContext.response();
            browserBack(response);
            return;
        }

        PerformanceState.getThisThreadHashtable().setAppDimension1(url());

        initiateRedirect(requestContext);

        if (!_isSelfRedirect) {
            AWSession session = requestContext.session(false);
            if (session != null) {
                session.markAppBoundary();
            }
            // set up this component for subsequent (post backtrack) forward tracks
            _isRefresh = true;
        }
    }

    protected boolean shouldValidateSession ()
    {
        return false;
    }

    public static void setupHeaders (AWResponse response, String unsafeUrlLocation)
    {
        String safeUrlLocation = AWUtil.filterUnsafeHeader(unsafeUrlLocation);
        response.setHeaderForKey(safeUrlLocation, "location");
        response.setContentType(AWContentType.TextHtml);
        response.setStatus(AWResponse.StatusCodes.RedirectFound);
        response.appendContent(FullPageRedirectStringStart);
        response.appendContent(safeUrlLocation);
        response.appendContent(FullPageRedirectStringMiddle);
        response.appendContent(safeUrlLocation);
        response.appendContent(FullPageRedirectStringFinish);        
    }

    public static void setupHeaders (HttpServletResponse httpServletResponse,
                                     String urlLocation)
    {
        try {
            httpServletResponse.sendRedirect(urlLocation);
        } catch (IOException e) {
            throw new AWGenericException("IO Exception encountered ", e);
        }
    }
    private static final String SchemeDelimiter = "://";
    private static final int SchemeDelimiterLength = SchemeDelimiter.length();
    public static AWRedirect getRedirect (AWRequestContext requestContext, String sUrl)
    {
        AWRedirect redirect = null;
        try {
            if (AppHost == null) {
                synchronized (AWRedirect.class) {
                    if (AppHost == null) {
                        URL url = URLUtil.makeURL(requestContext.application().adaptorUrl());
                        AppHost = url.getHost().toLowerCase();
                        AppPort = String.valueOf(url.getPort());
                    }
                }
            }

            int start = sUrl.indexOf(SchemeDelimiter);
            start += SchemeDelimiterLength;
            int end = sUrl.indexOf('/',start);
            if (end == -1) {
                end = sUrl.length();
            }
            if (requestContext.isBrowserMicrosoft()) {
                int hostIndex = sUrl.toLowerCase().indexOf(AppHost,start);
                int portIndex = sUrl.indexOf(AppPort,start);
                // AppHost name exists in the host string, but App port does not
                if (hostIndex != -1 && hostIndex < end && (portIndex == -1 || portIndex > end)){
                    Log.aribaweb.debug("getRedirectUrl: AWUrlRedirect");
                    redirect = (AWRedirect)requestContext.pageWithName(AWUrlRedirect.PageName);
                }
            }
            if (redirect == null) {
                Log.aribaweb.debug("getRedirectUrl: AWRedirect");
                redirect = (AWRedirect)requestContext.pageWithName(AWRedirect.PageName);
            }
            redirect.setUrl(sUrl);
        }
        catch (MalformedURLException ex) {
            throw new AWGenericException("Unable to parse url:" + sUrl, ex);
        }

        return redirect;
    }

    // Needed because AWRedirect is an AWComponent.  If we change the construction / use
    // of AWRedirect to generate an AWResponse directly, then we may be able to remove
    // this method.
    protected boolean allowDeferredResponse ()
    {
        return false;
    }

    public static String escapeJavascript (String receiver)
    {
        String escaped = receiver;
        if (escaped == null) return escaped;
        // replace \ with \\
        escaped = AWUtil.replaceAllOccurrences(escaped, "\\", "\\\\");
        // replace ' with \'
        escaped = AWUtil.replaceAllOccurrences(escaped, "'", "\\'");
        Log.aribaweb.debug("AWRedirect -- escaping url for js %s %s", receiver, escaped);
        return escaped;
    }

    // Globally registered decorators get a shot at adding context to all redirect URLs
    public interface URLDecorator {
        // receiver should return alternate (updated version) or original
        public String prepareUrl(String originalUrl, boolean isLocal);
    }

    protected static List <URLDecorator>_Decorators = ListUtil.list();

    public static void registerDecorator (URLDecorator decorator)
    {
        _Decorators.add(decorator);
    }

    public static String decorateUrl (String originalUrl, boolean isLocal)
    {
        String result = originalUrl;
        for (URLDecorator decorator : _Decorators) {
            result = decorator.prepareUrl(result, isLocal);
        }
        return result;
    }
}
