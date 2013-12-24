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

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/MultiTabHandler.java#1 $

    Responsible: fkolar
 */
package ariba.util.http.multitab;


import ariba.util.core.MapUtil;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 Handler for Serving multi-tab request. Once the request is served then is sent for
 actual processing.We can see
 this handler a  link between the MultiTabSupport and the Servletadaptor
 */
public interface MultiTabHandler
{
    public static final String AppHandlder = "AppHandler";
    public static final String InspectorHandler = "InspectorHandler";

    public static final String StringTable = "ariba.util.core";
    public static final String MultiTabExceptionMessageKey = "MultiTabExceptionMessage";
    public static final String MultiTabExceptionReportMessageKey =
            "MultiTabExceptionReportMessage";


    /**
     Tabs are false by default, please override if your need to enable it
     or has more complex logic.

     @param data Data wrapping object.
     @return Indicates if tab logic should run.
     */
    boolean isTabEnabled (RequestInfo data);

    /**
     What should the application do by default or when tabbing is disabled?

     @param data Data wrapping object.
     @throws java.io.IOException
     */
    public void handleDefault (RequestInfo data) throws IOException;

    /**
     What should the application do when processing a new tab request.

     @param data Data wrapping object.
     @throws IOException
     */
    public void handleNewTab (RequestInfo data) throws IOException;

    /**
     What should the application do when the request is coming from existing tab.
     The redirectUrl will be set to the `data` before calling this function?

     @param data Data wrapping object.
     @throws IOException
     */
    public void handleExistingTab (RequestInfo data) throws IOException;

    /**
     Function that can be overridden to allow munging of the redirect url.

     @param redirectUrl The redirect url to munge.
     @return The munged redirect url.
     */
    public String mungeRedirectUrl (String redirectUrl);

    /**
     What should the application do when too many tabs error is reached?

     @param data Data wrapping object.
     @throws IOException
     */

    public void handleTooManyTabs (RequestInfo data) throws IOException;

    /**
     Gives option to make some initialization
     */
    void initialize ();

    /**
     Mainly used for our factory methods which instantiates this handlers.
     */
    public void setServletRequest (HttpServletRequest request);

    public void setRequestProcessor (RequestProcessor processor);

    /**
     A wrapper object that contains the data needed to process multitab request
     */
    public static class RequestInfo
    {
        String _fullUrl;
        String _redirectUrl;
        Object _request;
        Object _response;
        Map _params;

        /**
         Information needed to process MultiTab and then continue with
         normal dispatching for the underlying technology.

         @param fullUrl  The full path, including query parameters. Will be
         used to assert that we aren't redirecting to the same
         URL (and therefore infinite loop) in the locked case.
         @param request  A generic request object.
         @param response A generic response object.
         */
        public RequestInfo (
                String fullUrl, Object request, Object response)
        {
            _fullUrl = fullUrl;
            _request = request;
            _response = response;
            _params = MapUtil.map();

            if (request instanceof HttpServletRequest) {
                Cookie[] cookies = ((HttpServletRequest)request).getCookies();
                _params.put("cookies", cookies);
            }
        }

        public String getFullUrl ()
        {
            return _fullUrl;
        }

        public String getRedirectUrl ()
        {
            return _redirectUrl;
        }

        /**
         Set the redirection url. Should be called before calling the
         handleLocked function.

         @param redirectUrl The URL to redirect to.
         */
        public void setRedirectUrl (String redirectUrl)
        {
            _redirectUrl = redirectUrl;
        }

        public Object getRequest ()
        {
            return _request;
        }

        public Object getResponse ()
        {
            return _response;
        }

        public Map getParams ()
        {
            return _params;
        }
    }

    /**
     Generic MultiTabException.
     */
    public class MultiTabException extends RuntimeException
    {
        /**
         Constructs a new instance with the specified <code>message</code>.<p>
         */
        public MultiTabException (String message)
        {
            super(message);
        }

        /**
         Constructs a new instance with the specified <code>cause</code>.<p>
         */
        public MultiTabException (Throwable cause)
        {
            super(cause);
        }

        /**
         Constructs a new instance with the specified <code>message</code>
         and <code>cause</code>.<p>
         */
        public MultiTabException (String message, Throwable cause)
        {
            super(message, cause);
        }
    }

}