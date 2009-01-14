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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWXBasicScriptFunctions.java#31 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;

public final class AWXBasicScriptFunctions extends AWComponent
{
    private static final AWEncodedString SubmitForm = new AWEncodedString("ariba.Handlers.hSubmit");
    private static final AWEncodedString SubmitFormForElementName = new AWEncodedString("ariba.Handlers.hKeyDown");
    private static final AWEncodedString SubmitFormAtIndexFunction = new AWEncodedString("return ariba.Handlers.hSubmitAtIndex(");
    private static final AWEncodedString CloseFunction = new AWEncodedString("');");
    public static final AWEncodedString EmptyDocScriptlet =
        new AWEncodedString("javascript:void(document.open());void(document.write(\"<html></html>\"));void(document.close());");
    private static AWEncodedString _RequestHandlerUrl;

    private static String BackTrackUrl;
    private static String ForwardTrackUrl;

    public AWEncodedString requestHandlerUrl ()
    {
        if (_RequestHandlerUrl == null) {
            String requestHandlerUrl =
                AWComponentActionRequestHandler.SharedInstance.requestHandlerUrl(request());
            _RequestHandlerUrl = AWEncodedString.sharedEncodedString(requestHandlerUrl);
        }
        return _RequestHandlerUrl;
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
        return AWComponentActionRequestHandler.SharedInstance.refreshUrl(requestContext());
    }

    public String getBackTrackUrl ()
    {
        if (AWConcreteApplication.IsCookieSessionTrackingEnabled) {
            if (BackTrackUrl == null) {
                BackTrackUrl =
                    AWComponentActionRequestHandler.SharedInstance.historyRequestHandlerUrl(requestContext(),
                                AWComponentActionRequestHandler.BackTrackActionName);
            }
            return BackTrackUrl;
        }
        else {
            return AWComponentActionRequestHandler.SharedInstance.historyRequestHandlerUrl(requestContext(),
                                AWComponentActionRequestHandler.BackTrackActionName);
        }
    }

    public String getForwardTrackUrl ()
    {
        if (AWConcreteApplication.IsCookieSessionTrackingEnabled) {

            if (ForwardTrackUrl == null) {
                ForwardTrackUrl =
                    AWComponentActionRequestHandler.SharedInstance.historyRequestHandlerUrl(requestContext(),
                                AWComponentActionRequestHandler.ForwardTrackActionName);
            }
            return ForwardTrackUrl;
        }
        else  {
            return AWComponentActionRequestHandler.SharedInstance.historyRequestHandlerUrl(requestContext(),
                                AWComponentActionRequestHandler.ForwardTrackActionName);

        }
    }

    public String getPingUrl ()
    {
        return AWDirectActionUrl.urlForDirectAction(AWDirectAction.PingActionName, null);
    }

    public String getProgressCheckUrl ()
    {
        return AWDirectActionUrl.urlForDirectAction(AWDirectAction.ProgressCheckActionName, null);
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
