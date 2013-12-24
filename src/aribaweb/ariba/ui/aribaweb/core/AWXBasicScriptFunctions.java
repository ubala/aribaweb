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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWXBasicScriptFunctions.java#44 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.test.TestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.http.multitab.MultiTabSupport;

public final class AWXBasicScriptFunctions extends AWComponent
{
    private static final AWEncodedString SubmitForm = new AWEncodedString("ariba.Handlers.hSubmit");
    private static final AWEncodedString SubmitFormForElementName = new AWEncodedString("ariba.Handlers.hKeyDown");
    private static final AWEncodedString SubmitFormAtIndexFunction = new AWEncodedString("return ariba.Handlers.hSubmitAtIndex(");
    private static final AWEncodedString CloseFunction = new AWEncodedString("');");
    public static final AWEncodedString EmptyDocScriptlet =
        new AWEncodedString("javascript:void(document.open());void(document.write(\"<html></html>\"));void(document.close());");
    private static AWEncodedString[] _RequestHandlerUrl;


    public AWEncodedString requestHandlerUrl ()
    {
        MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
        if (_RequestHandlerUrl == null) {
            _RequestHandlerUrl = new AWEncodedString[multiTabSupport.maximumNumberOfTabs()];
        }
        boolean useFullURL = false;
        if (AWConcreteServerApplication.IsDebuggingEnabled) {
            // we should consult with the requestContext every time since the
            // url may be dependent on the incoming request
            useFullURL = TestContext.isTestAutomationMode(requestContext());
        }

        int tabIndex = requestContext().getTabIndex();

        if (useFullURL) {
            // do not cache the full url since the incoming request can be
            // either HTTP or HTTPS
            String requestHandlerUrl = AWComponentActionRequestHandler
                    .SharedInstance.requestHandlerUrl(request(), true);
            requestHandlerUrl = multiTabSupport.insertTabInUri(requestHandlerUrl, tabIndex, true);
            return AWEncodedString.sharedEncodedString(requestHandlerUrl);
        }
        else {
            AWEncodedString requestHandlerUrlEncoded =
                    _RequestHandlerUrl[tabIndex];

            if (requestHandlerUrlEncoded == null) {
                // todo: if requestHandlerUrlEncoded was public, we would
                // remove two steps, where we decode only to encode again
                String requestHandlerUrl =
                        AWComponentActionRequestHandler.SharedInstance
                                .requestHandlerUrl(request(), false);
                requestHandlerUrl = multiTabSupport.insertTabInUri(requestHandlerUrl,
                        tabIndex, true);
                requestHandlerUrlEncoded = AWEncodedString.sharedEncodedString(
                        requestHandlerUrl);
                _RequestHandlerUrl[tabIndex] = requestHandlerUrlEncoded;
            }

            return requestHandlerUrlEncoded;
        }
    }

    public boolean allowsWhitespaceCompression ()
    {
        return false;
    }

    public static String setDocumentLocation (String url)
    {
        return StringUtil.strcat("ariba.Request.setDocumentLocation('", url, "')");
    }

    public static void appendSubmitFormAtIndex (AWResponse response, AWEncodedString index, AWEncodedString hiddenFieldName, AWEncodedString elementId)
    {
        response.appendContent(SubmitFormAtIndexFunction);
        response.appendContent(index);
        response.appendContent(AWConstants.Comma);
        response.appendContent(AWConstants.SingleQuote);
        response.appendContent(hiddenFieldName);
        response.appendContent(AWConstants.SingleQuote);
        response.appendContent(AWConstants.Comma);
        response.appendContent(AWConstants.SingleQuote);
        response.appendContent(elementId);
        response.appendContent(CloseFunction);
    }

    public static void appendSubmitCurrentForm (AWRequestContext requestContext, AWEncodedString elementId)
    {
        AWResponse response = requestContext.response();
        response.appendContent(AWConstants.Return);
        response.appendContent(AWConstants.Space);
        response.appendContent(SubmitFormForElementName);
        response.appendContent(AWConstants.OpenParen);
        response.appendContent(AWConstants.SingleQuote);
        response.appendContent(requestContext.currentForm().formName());
        response.appendContent(AWConstants.SingleQuote);
        response.appendContent(AWConstants.Comma);
        response.appendContent(AWConstants.SingleQuote);
        response.appendContent(elementId);
        response.appendContent(AWConstants.SingleQuote);
        response.appendContent(AWConstants.Comma);
        response.appendContent(AWConstants.Event);
        response.appendContent(AWConstants.CloseParen);
        response.appendContent(AWConstants.Semicolon);
    }

    public static String submitFormString (AWRequestContext requestContext,
                                           String formObject,
                                           String target)
    {
        String submitFormString;
        if (!StringUtil.nullOrEmptyOrBlankString(target)) {
            submitFormString = Fmt.S("%s(%s,%s)", SubmitForm, formObject, target);
        } else {
            submitFormString = Fmt.S("%s(%s);", SubmitForm, formObject);
        }

        return submitFormString;
    }

    public static String submitFormString (AWRequestContext requestContext, String formObject)
    {
        return submitFormString (requestContext, formObject, null);
    }

    public String getRefreshUrl ()
    {
        AWSession session = session();
        String refreshURL = session.getRefreshURL();
        if (refreshURL == null) {
            refreshURL = AWComponentActionRequestHandler.SharedInstance
                    .refreshUrl(requestContext());
            session.setRefreshURL(refreshURL);
        }
        return refreshURL;
    }

    public String getBackTrackUrl ()
    {
        AWSession session = session();
        String backTrackURL = session.getBackTrackURL();
        if (backTrackURL == null) {
            backTrackURL = AWComponentActionRequestHandler.SharedInstance
                    .historyRequestHandlerUrl(requestContext(),
                            AWComponentActionRequestHandler
                                    .BackTrackActionName);
            session.setBackTrackURL(backTrackURL);
        }
        return backTrackURL;
    }

    public String getForwardTrackUrl ()
    {
        AWSession session = session();
        String forwardTrackURL = session.getForwardTrackURL();
        if (forwardTrackURL == null) {
            forwardTrackURL = AWComponentActionRequestHandler.SharedInstance
                    .historyRequestHandlerUrl(requestContext(),
                            AWComponentActionRequestHandler
                                    .ForwardTrackActionName);
            session.setForwardTrackURL(forwardTrackURL);
        }
        return forwardTrackURL;
    }

    public String getPingUrl ()
    {
        MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
        return multiTabSupport.insertTabInUri(
                AWDirectActionUrl.urlForDirectAction(AWDirectAction.PingActionName, null),
                requestContext().getTabIndex(), true);
    }

    public String getProgressCheckUrl ()
    {
        if (!AWConcreteServerApplication.AllowsConcurrentRequestHandling) {
            return null;
        }
        MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
        AWDirectActionUrl url = AWDirectActionUrl.checkoutUrl();
        url.setDirectActionName(AWDirectAction.ProgressCheckActionName);

        if (!AWConcreteApplication.IsCookieSessionTrackingEnabled) {
        // Set session id and application number on the url if cookie tracking is disabled.
            url.setSessionId(session().sessionId());
        }
        if (!StringUtil.nullOrEmptyOrBlankString(request().applicationNumber())) {
            // this ensures the progress check goes to the same app instance
            url.setApplicationNumber(request().applicationNumber());
        }
        // Use null request context to avoid associating to the session
        String finishedUrl = url.finishUrl();
        return multiTabSupport.insertTabInUri(
                AWDirectActionUrl.decorateUrl(null, finishedUrl),
                requestContext().getTabIndex(), true);
    }

    public String waitAlertMillis ()
    {
        return AWConcreteApplication.IsDebuggingEnabled ? "6000" : "4000";
    }

    public String openWindowErrorMessage ()
    {
        return localizedJavaString(1, "Please enable window popup for this site to use this function.");
    }

    public boolean includeIndividualJSFiles ()
    {
        // if debugging and the individual files can be found (i.e. the
        // ARIBA_AW_SEARCH_PATH has been set to include <aribaweb_src_dir/>/resource/webserver)
        // the include individual files; otherwise use the compressed combo file
        return AWConcreteApplication.IsDebuggingEnabled
                && resourceManager().resourceNamed("Util.js") != null;
    }
}
