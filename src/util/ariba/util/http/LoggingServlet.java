/*
    Copyright (c) 1996-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/http/LoggingServlet.java#2 $

    Responsible: msnider
*/

package ariba.util.http;

import ariba.util.core.FatalAssertionException;
import ariba.util.core.SystemUtil;
import ariba.util.core.ThreadDebugState;
import ariba.util.log.Log;
import ariba.util.log.LogManager;
import ariba.util.log.Logger;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
    A servlet that will log all important exceptions that occur while
    servicing the request.
    
    @aribaapi ariba
*/
public abstract class LoggingServlet extends HttpServlet
{
    public void service (HttpServletRequest  request,
                         HttpServletResponse response)
      throws ServletException, IOException
    {
        try {
            logServletServiceRequest(request, response);
            runBeforeService(request, response);
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
    }

    /**
     * Returns the logger used by this servlet.
     * @return The logger instance.
     */
    protected Logger getLogger ()
    {
        return Log.httpServlet;
    }

    /**
        Servlet logging method. This is used internally by
        LoggingServlet to log messages.
        @aribaapi ariba
    */
    public void logServletException (Throwable t)
    {
        if (LogManager.loggingInitialized()) {
            if (t instanceof FatalAssertionException) {
                getLogger().warning(6567, t);
            }
            else {
                getLogger().warning(6566,
                                           ThreadDebugState.makeString(),
                                           SystemUtil.stackTrace(t));
            }
        }
    }

    /**
        Servlet logging method. This is used internally by
        LoggingServlet to log messages.
        @aribaapi ariba
    */
    public void logServletServiceRequest (HttpServletRequest  request,
                                                 HttpServletResponse response)
    {
        if (LogManager.loggingInitialized()) {
            getLogger().debug("ServletRequest: %s", request);
        }
    }

    /**
     * This function will be called before super.service() is called, and
     * allows some application specific customization in child servlets,
     * such as AWDispatcherServlet.
     */
    protected void runBeforeService (HttpServletRequest  request,
                                  HttpServletResponse response)
    {

    }
}
