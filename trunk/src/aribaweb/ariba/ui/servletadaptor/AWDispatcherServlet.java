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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWDispatcherServlet.java#5 $
*/

package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.FatalAssertionException;
import ariba.util.core.Fmt;
import ariba.util.core.HTTP;
import ariba.util.core.PerformanceState;
import ariba.util.core.SystemUtil;
import ariba.util.core.ThreadDebugState;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.log.LogManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;


/**
    @aribaapi private
*/
public class AWDispatcherServlet extends HttpServlet
{
    AWServletApplication _awapplication;

    // ** Thread Safety Considerations: the ivar _awapplication doesn't ever change, so this needs no locking.

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

        AWServletApplication servletApplication =
                (AWServletApplication)AWConcreteApplication.createApplication(
                        applicationClassName, AWServletApplication.class);

        return servletApplication;
    }

    public void service (HttpServletRequest  request,
                         HttpServletResponse response)
      throws ServletException, IOException
    {
        try {
            logServletServiceRequest(request, response);
            _awapplication.initAdaptorUrl(request);
            super.service(request, response);
        }
        catch (ServletException ex) {
            logServletException(ex);
            throw(ex);
        }
        catch (IOException ex) {
            logServletException(ex);
            throw(ex);
        }
        catch (RuntimeException ex) {
            logServletException(ex);
            throw(ex);
        }
        catch (Error ex) {
            logServletException(ex);
            throw(ex);
        }
        finally {
                // the finally for the parent class, LoggingServlet,
                // will already have run, so it is ok to clear the
                // thread debug state now.
            ThreadDebugState.clear();
        }
    }

    /**
        Servlet logging method. This is used internally by
        LoggingServlet to log messages. Or if your class can not
        extend LoggingServlet, you can manually call
        logServletException which is static for your calling
        convinence.
        @aribaapi ariba
    */
    public static void logServletException (Throwable t)
    {
        if (LogManager.loggingInitialized()) {
            if (t instanceof FatalAssertionException) {
                Log.servletadaptor.warning(6567, t);
            }
            else {
                Log.servletadaptor.warning(6566,
                                           ThreadDebugState.makeString(),
                                           SystemUtil.stackTrace(t));
            }
        }
    }

    /**
        Servlet logging method. This is used internally by
        LoggingServlet to log messages. Or if your class can not
        extend LoggingServlet, you can manually call
        logServletException which is static for your calling
        convinence.
        @aribaapi ariba
    */
    public static void logServletServiceRequest (HttpServletRequest  request,
                                                 HttpServletResponse response)
    {
        if (LogManager.loggingInitialized()) {
            Log.servletadaptor.debug("ServletRequest: %s", request);
        }
    }
    protected String cookieHeader (HttpServletRequest servletRequest)
    {
        String cookieHeader = servletRequest.getHeader("HTTP_COOKIE");
        if (cookieHeader == null) {
            cookieHeader = servletRequest.getHeader("cookie");
        }
        return cookieHeader;
    }

    protected String requestString (HttpServletRequest servletRequest)
    {
        FastStringBuffer requestString = new FastStringBuffer();
        requestString.append(servletRequest.getScheme());
        requestString.append("://");
        requestString.append(servletRequest.getServerName());
        requestString.append(":");
        requestString.append(String.valueOf(servletRequest.getServerPort()));
        requestString.append(servletRequest.getContextPath());
        requestString.append(servletRequest.getServletPath());
        requestString.append(((servletRequest.getPathInfo() != null) ? servletRequest.getPathInfo() : ""));
        requestString.append(((servletRequest.getQueryString() != null) ? "?"+servletRequest.getQueryString() : ""));
        return requestString.toString();
    }

    protected void aribawebDispatcher (HttpServletRequest servletRequest, HttpServletResponse servletResponse)
        throws IOException, ServletException
    {
        try {
            // Set default performance exception logging
            PerformanceState.watchPerformance(_awapplication.defaultPerformanceCheck());
            if (Log.aribaweb_request.isDebugEnabled()) {
                Log.aribaweb_request.debug("##########################################################");
                Log.aribaweb_request.debug("request: %s", requestString(servletRequest));
                Log.aribaweb_request.debug("cookies: %s", cookieHeader(servletRequest));

                HttpSession session = servletRequest.getSession(false);
                if (!servletRequest.isRequestedSessionIdValid() && session != null) {
                    if (session.isNew()) {
                        Log.aribaweb_request.debug("Warning: new HttpSession not created by AW.");
                    }
                    // is this possible?
                    Log.aribaweb_request.debug(
                        "Warning: HttpSession %s exists with invalid request session id %s.",
                        session.getId(), servletRequest.getRequestedSessionId());
                }
            }
            AWServletRequest awservletRequest = (AWServletRequest)_awapplication.createRequest(servletRequest);
            awservletRequest.setupHttpServletRequest(servletRequest, servletResponse, this.getServletContext());
            AWServletResponse awresponse = (AWServletResponse)_awapplication.dispatchRequest(awservletRequest);
            // awresponse can return in jsp world if IOException/SocketException is encounted
            if (awresponse != null) {
                awresponse.writeToServletResponse(servletResponse);
            }
        }
        catch (Throwable throwable) { // OK
            try {
                /*
                    The following test should be converted to a series of catch block
                    But at the time this code is written, the code in the try block
                    doesn't throw any IOException or ServletException. But in case later
                    it will do it, we don't want to catch these by mistake. That's why there is
                    this instanceof test
                */
                if (throwable instanceof WrapperRuntimeException) {
                    WrapperRuntimeException wrapperException = (WrapperRuntimeException)throwable;
                    Exception originalException = wrapperException.originalException();
                    if (originalException != null && (originalException instanceof java.net.SocketException ||
                        originalException.getClass().getName().equals(
                            "org.apache.catalina.connector.ClientAbortException"))) {
                        // ignore all "Connection reset by peer"
                    }
                    else {
                        logServletException(throwable);
                            //we handle this by creating a 500 error page
                        _awapplication.logWarning(Fmt.S("** Error: uncaught exception : %s",
                                                       SystemUtil.stackTrace(throwable)));
                        servletResponse.sendError(HTTP.CodeInternalServerError);
                    }
                }
                else if (throwable instanceof IOException) {
                    // do a string comparison to avoid dependency on a Tomcat class
                    if (throwable.getClass().getName().equals(
                            "org.apache.catalina.connector.ClientAbortException"))
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
                    _awapplication.logWarning(Fmt.S("** Error: uncaught exception : %s",
                                                   SystemUtil.stackTrace(throwable)));
                    servletResponse.sendError(HTTP.CodeInternalServerError);
                }
            }
            catch (java.net.SocketException e) {
                // keep the logs clean on the rare chance that the client is closed and
                // we're trying to render a HTTP.CodeInternalServerError response.
                Log.aribaweb_request.debug(
                    "** Error: exception thrown while handling exception : %s",
                    SystemUtil.stackTrace(e));
            }
        }
        finally {
            ThreadDebugState.clear();
        }
    }

    protected void doGet (HttpServletRequest servletRequest, HttpServletResponse servletResponse)
        throws ServletException, java.io.IOException
    {
        aribawebDispatcher(servletRequest, servletResponse);
    }

    protected void doPost (HttpServletRequest servletRequest, HttpServletResponse servletResponse)
        throws ServletException, java.io.IOException
    {
        aribawebDispatcher(servletRequest, servletResponse);
    }
}
