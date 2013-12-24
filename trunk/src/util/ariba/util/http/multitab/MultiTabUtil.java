/*
    Copyright (c) 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/MultiTabUtil.java#1 $

    Responsible: fkolar
*/
package ariba.util.http.multitab;

import ariba.util.core.FastStringBuffer;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static ariba.util.http.multitab.MultiTabSupport.Instance;

/**
 Since now we are sharing the same behavior across two different applications we need to
 extract common static method calls into this class
 */
public class MultiTabUtil
{

    /**
     @see Instance#initHandler(RequestProcessor, HttpServletRequest)
     */
    public static MultiTabHandler iniHandler (RequestProcessor requestProcessor,
                                              HttpServletRequest request)
            throws IOException
    {
        return Instance.get().initHandler(requestProcessor, request);
    }

    /**
     @see Instance#releaseHandler()
     */
    public static void releaseHandler ()
    {
        Instance.releaseHandler();
    }


    /**
     Efficiently concatenates the query parameters onto the request URL.

     @param servletRequest The http servlet request.
     @return The concatenated string (or the request URL, when no parameters).
     */
    public static String buildFullURL (HttpServletRequest servletRequest)
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
     Prepares the RequestInfo object and submits this for processing

     @param request
     @param response
     @throws IOException
     @throws ServletException
     */
    public static void processRequest (HttpServletRequest request,
                                       HttpServletResponse response)
            throws IOException, ServletException
    {
        MultiTabHandler.RequestInfo data = new MultiTabHandler.RequestInfo
                (buildFullURL(request), request, response);

        MultiTabHandler multiTabHandlerCallback = Instance
                .currentHandler();
        Instance.get().processRequest(data, multiTabHandlerCallback);
    }

}
