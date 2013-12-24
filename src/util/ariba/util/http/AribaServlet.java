/*
    Copyright (c) 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/http/AribaServlet.java#17 $

    Responsible: msnider
*/

package ariba.util.http;

import ariba.util.core.FastStringBuffer;
import ariba.util.core.ThreadDebugState;
import ariba.util.http.multitab.MultiTabHandler;
import ariba.util.http.multitab.MultiTabUtil;
import ariba.util.http.multitab.RequestProcessor;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 Helper servlet class that all servlets in the ariba space should
 extend. We may add additional functionality in the future. This was move
 to here, so it is available to AribaWeb.
 It also implements the MultiTab, so we don't need another class.

 @aribaapi ariba
 */
public abstract class AribaServlet extends LoggingServlet implements RequestProcessor
{

    public void service (HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException
    {
        // The scope for this handler must be local and survive only this request.
        // We could use context or session but these are not really thread safe
        try {
            MultiTabUtil.iniHandler(this, request);
            super.service(request, response);
        }
        finally {
            // the finally for the parent class, LoggingServlet,
            // will already have run, so it is ok to clear the
            // thread debug state now.
            ThreadDebugState.clear();
            MultiTabUtil.releaseHandler();
        }
    }

    @Override
    public String multiTabHandlerName ()
    {
        return MultiTabHandler.AppHandlder;
    }

    @Override
    public void delegateTooManyTabsError (HttpServletRequest servletRequest,
                                          HttpServletResponse
            servletResponse) throws IOException, ServletException
    {
        throw new IllegalStateException("No such implementation here.");
    }

    /**
     Efficiently concatenates the query parameters onto the request URL.

     @param servletRequest The http servlet request.
     @return The concatenated string (or the request URL, when no parameters).
     */
    public String requestString (HttpServletRequest servletRequest)
    {
        if (servletRequest.getQueryString() != null) {
            FastStringBuffer requestString = new FastStringBuffer();
            requestString.append(servletRequest.getRequestURL());
            requestString.append("?");
            requestString.append(servletRequest.getQueryString());
            return requestString.toString();
        }

        return servletRequest.getRequestURL().toString();
    }

    /**
     Option to alter existing request before is sent for processing.

     @param request
     @param response
     @throws IOException
     @throws ServletException
     */
    protected void wrapRequest (HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        MultiTabUtil.processRequest(request, response);
    }

    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        wrapRequest(request, response);
    }

    @Override
    protected void doPost (HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        wrapRequest(request, response);
    }
}