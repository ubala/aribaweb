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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWDispatcherServlet.java#21 $
*/

package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Fmt;
import ariba.util.core.HTTP;
import ariba.util.core.PerformanceState;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.ThreadDebugState;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.http.AribaServlet;
import ariba.util.http.multitab.MaximumTabExceededException;
import ariba.util.http.multitab.MultiTabHandler;
import ariba.util.log.Logger;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
    @aribaapi private
*/
public class AWDispatcherServlet extends AribaServlet
{
    protected AWServletApplication _awapplication;


    // ** Thread Safety Considerations: the ivar _awapplication doesn't ever
    // change, so this needs no locking.

    public void init (ServletConfig servletConfig) throws ServletException
    {
        AWServletApplication.initializeServletConfig(servletConfig);
        super.init(servletConfig);
        _awapplication = createApplication();
    }

    public String applicationClassName ()
    {
        return "app.Application";
    }

    public AWServletApplication createApplication ()
    {
        String applicationClassName = applicationClassName();
        return (AWServletApplication)AWConcreteApplication.createApplication(
                        applicationClassName, AWServletApplication.class);
    }

    @Override
    protected Logger getLogger ()
    {
        return Log.servletadaptor;
    }


    @Override
    protected void runBeforeService (HttpServletRequest request,
                                     HttpServletResponse response)
    {
        _awapplication.initAdaptorUrl(request);
    }

    protected String cookieHeader (HttpServletRequest servletRequest)
    {
        String cookieHeader = servletRequest.getHeader("HTTP_COOKIE");

        if (cookieHeader == null) {
            cookieHeader = servletRequest.getHeader("cookie");
        }

        return cookieHeader;
    }

    @Override
    public void processRequest (
            HttpServletRequest request,
            HttpServletResponse response,
            boolean isGet) throws IOException, ServletException
    {
        aribawebDispatcher(request, response);
    }

    @Override
    public String multiTabHandlerName ()
    {
        return MultiTabHandler.AppHandlder;
    }

    @Override
    public void delegateTooManyTabsError (HttpServletRequest servletRequest,
                                          HttpServletResponse servletResponse)
            throws IOException, ServletException
    {
        try {
            AWServletResponse awServletResponse = renderResponse(servletRequest,
                    servletResponse, true);
            // awServletResponse can return in jsp world if
            // IOException/SocketException is encountered
            if (awServletResponse != null) {
                awServletResponse.writeToServletResponse(servletResponse);
            }
        }
        catch (ServletException servletException) {
            servletException.printStackTrace();
        }
    }



    protected void aribawebDispatcher (
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse)
            throws IOException, ServletException
    {
        try {
            // Set default performance exception logging
            PerformanceState.watchPerformance(
                    _awapplication.defaultPerformanceCheck());
            String sessionID = servletRequest.getRequestedSessionId();
            if (!StringUtil.nullOrEmptyString(sessionID)) {
                PerformanceState.getThisThreadHashtable().setSessionID(
                        sessionID);
            }
            if (Log.aribaweb_request.isDebugEnabled()) {
                Log.aribaweb_request.debug(
                        "##########################################" +
                                "################");
                Log.aribaweb_request.debug(
                        "request: %s", requestString(servletRequest));
                Log.aribaweb_request.debug(
                        "cookies: %s", cookieHeader(servletRequest));

                HttpSession session = servletRequest.getSession(false);
                if (!servletRequest.isRequestedSessionIdValid() &&
                        session != null) {
                    if (session.isNew()) {
                        Log.aribaweb_request.debug(
                                "Warning: new HttpSession not created by AW.");
                    }
                    // is this possible?
                    Log.aribaweb_request.debug(
                        "Warning: HttpSession %s exists with " +
                                "invalid request session id %s.",
                        session.getId(),
                            servletRequest.getRequestedSessionId());
                }
            }
            AWServletResponse awServletResponse =
                    renderResponse(servletRequest, servletResponse, false);

            // awServletResponse can return in jsp world if
            // IOException/SocketException is encountered
            if (awServletResponse != null) {
                awServletResponse.writeToServletResponse(servletResponse);
            }
        }
        catch (Throwable throwable) { // OK
            try {
                /*
                    The following test should be converted to a series of
                    catch block. But at the time this code is written, the
                    code in the try block doesn't throw any IOException or
                    ServletException. But in case later it will do it, we
                    don't want to catch these by mistake. That's why there is
                    this instanceof test
                */
                if (throwable instanceof WrapperRuntimeException) {
                    WrapperRuntimeException wrapperException = (
                            WrapperRuntimeException)throwable;
                    Exception originalException =
                            wrapperException.originalException();
                    if (originalException != null && (originalException
                            instanceof java.net.SocketException ||
                        originalException.getClass().getName().equals(
                            "org.apache.catalina.connector." +
                                    "ClientAbortException"))) {
                        // ignore all "Connection reset by peer"
                    }
                    else {
                        logServletException(throwable);
                            //we handle this by creating a 500 error page
                        _awapplication.logWarning(Fmt.S(
                                "** Error: uncaught exception : %s",
                                SystemUtil.stackTrace(throwable)));
                        servletResponse.sendError(HTTP.CodeInternalServerError);
                    }
                }
                else if (throwable instanceof IOException) {
                    // do a string comparison to avoid dependency on a
                    // Tomcat class
                    if (throwable.getClass().getName().equals(
                            "org.apache.catalina.connector." +
                                    "ClientAbortException"))
                    {
                        // similar to Connection reset by peer
                    }
                    else {
                        throw (IOException)throwable;
                    }
                }
                else if (throwable instanceof ServletException) {
                    throw (ServletException)throwable;
                }
                else {
                    logServletException(throwable);
                        // we handle this by creating a 500 error page
                    _awapplication.logWarning(Fmt.S(
                            "** Error: uncaught exception : %s",
                            SystemUtil.stackTrace(throwable)));
                    servletResponse.sendError(HTTP.CodeInternalServerError);
                }
            }
            catch (java.net.SocketException e) {
                // keep the logs clean on the rare chance that the client is
                // closed and we're trying to render a
                // HTTP.CodeInternalServerError response.
                Log.aribaweb_request.debug(
                    "** Error: exception thrown while handling exception : %s",
                    SystemUtil.stackTrace(e));
            }
        }
        finally {
            ThreadDebugState.clear();
        }
    }

    private AWServletResponse renderResponse (HttpServletRequest servletRequest,
                                             HttpServletResponse servletResponse,
                                             boolean isTooManyTabRequest)
            throws IOException, ServletException
    {
        AWServletResponse awServletResponse = null;
        AWServletRequest awServletRequest =
                (AWServletRequest)_awapplication.createRequest(
                        servletRequest);
        awServletRequest.setTooManyTabRequest(isTooManyTabRequest);
        if (isTooManyTabRequest) {
            AWRequestContext requestContext =
                    _awapplication.createRequestContext(awServletRequest);
            try {
                AWResponseGenerating awResponseGenerating =
                        _awapplication.handleMaxWindowException(requestContext,
                                new MaximumTabExceededException());
                awServletResponse = (AWServletResponse)awResponseGenerating.generateResponse();
                requestContext.setHttpSession(awServletRequest.getSession());
            }
            finally {
                // The MultiTabException page is not processed as any other request
                // through the AWDirectActionRequestHandler.
                // Special case for MultiTabException which is using tab 0 sessionId:
                // Lock was checkout by the sessionId at the beginning of the
                // renderResponse method when the session object is created.
                // This gives back the lock (check in) for the sessionId of tab 0
                requestContext.checkInExistingHttpSession();
            }
        }
        else {
            awServletRequest.setupHttpServletRequest(
                    servletRequest, servletResponse, this.getServletContext());
            PerformanceState.getThisThreadHashtable().setUserAgent(
                    awServletRequest.headerForKey(HTTP.HeaderUserAgent));
            awServletResponse = (AWServletResponse)_awapplication.dispatchRequest(
                    awServletRequest);
        }
        return awServletResponse;
    }
}
