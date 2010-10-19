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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWServletRequest.java#7 $
*/

package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWFileData;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.core.AWBaseRequest;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.HTTP;
import java.util.Map;
import ariba.util.core.MIME;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

/**
    An @{Link ariba.ui.aribaweb.core.AWRequest} wrapping around HttpServletRequest. 
 */
public class AWServletRequest extends AWBaseRequest
{
    protected HttpServletRequest _servletRequest;
    protected HttpServletResponse _servletResponse;
    private String[] _requestHandlerPath;
    private String _serverPort;
    private Map _headers;
    private String _requestHandlerKey = UndefinedString;

        // ** Thread Safety Considerations: A servlet request is never accessed by more than one thread -- no sync required.

    // the way this works is lame -- may also want to see  http:/servlets.com/jsp/examples/ch12/index.html#ex12_11

    public void init (HttpServletRequest servletRequest)
    {
        _servletRequest = servletRequest;
        String characterEncodingName = _servletRequest.getCharacterEncoding();
        AWCharacterEncoding characterEncoding = AWCharacterEncoding.characterEncodingNamed(characterEncodingName);
        if (characterEncoding == null) {
            characterEncoding = AWCharacterEncoding.Default;
        }
        setCharacterEncoding(characterEncoding);

        super.init();
    }

    /**
     * This returns the HTTP header information or null if it is not present.
     * @param requestHeaderKey
     * @return This returns the header information or null.
     */
    public String headerForKey (String requestHeaderKey)
    {
        return _servletRequest.getHeader(requestHeaderKey);
    }

    public Map headers ()
    {
        if (_headers == null) {
            _headers = MapUtil.map();
            java.util.Enumeration headerKeys = _servletRequest.getHeaderNames();
            while (headerKeys.hasMoreElements()) {
                String headerKey = (String)headerKeys.nextElement();
                String headerValue = _servletRequest.getHeader(headerKey);
                _headers.put(headerKey, headerValue);
            }
        }
        return _headers;
    }

    /*
    * Using Servlet Cookies doesn't seem to work ...
    * We'll just fall back on the AWBaseRequest implementation...

    protected Map computeCookieValues ()
    {
        Map result = null;
        Cookie[] cookies = _servletRequest.getCookies();
        int count = cookies.length;
        if (count > 0) {
            result = MapUtil.map();
            for (int i=0; i<count; i++) {
                Cookie cookie = cookies[i];
                result.put(cookie.getName(), cookie.getValue());
            }
        }
        return result;
    }
    */

    public AWFileData fileDataForKey (String formValueKey)
    {
        return (AWFileData)formValues().get(formValueKey);
    }

    public String requestHandlerKey ()
    {
        if (_requestHandlerKey == UndefinedString) {
            AWConcreteApplication.RequestURLInfo result = ((AWServletApplication)AWConcreteApplication.sharedInstance()).requestUrlInfo(_servletRequest.getRequestURI());
            _requestHandlerKey = result.requestHandlerKey;
            _requestHandlerPath = result.requestHandlerPath;
            setApplicationNumber(result.applicationNumber);
        }
        return _requestHandlerKey;
    }

    public String[] requestHandlerPath ()
    {
        if (_requestHandlerPath == null) {
            // force computation of requestHandlerPath
            requestHandlerKey();
        }
        return _requestHandlerPath;
    }

    protected byte[] initContent ()
    {
        byte[] contentBytes = null;
        try {
            // This block of code largely borrowed from ariba.util.net.https.EntrustHttpsURLConnection.java
            int contentLength = _servletRequest.getContentLength();
            if (contentLength < 0) {
                return new byte[0];
            }
            contentBytes = new byte[contentLength];
            int bytesReadSoFar = 0;
            int bytesToRead = contentLength;
            InputStream inputStream = _servletRequest.getInputStream();
            while (bytesToRead > 0) {
                int bytesRead = inputStream.read(contentBytes, bytesReadSoFar, bytesToRead);
                if (bytesRead == -1) {
                    throw new IOException(
                        Fmt.S("%s=%s but only %s bytes found",
                              MIME.HeaderContentLength,
                              Constants.getInteger(contentLength),
                              Constants.getInteger(bytesReadSoFar)));
                }
                bytesReadSoFar += bytesRead;
                bytesToRead -= bytesRead;
            }
            Assert.that(bytesReadSoFar == contentLength,
                        "bytesReadSoFar == contentLength");
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
        return contentBytes;
    }

    public int applicationNumberInt ()
    {
        return -1;
    }

    public String method ()
    {
        return _servletRequest.getMethod();
    }

    public String uri ()
    {
        return _servletRequest.getRequestURI();
    }

    public String queryString ()
    {
        return _servletRequest.getQueryString();
    }

    public boolean isSecureScheme ()
    {
        return HTTP.SecureProtocol.equals(_servletRequest.getScheme());
    }

    public String serverPort ()
    {
        if (_serverPort == null) {
            _serverPort = Constants.getInteger(_servletRequest.getServerPort()).toString();
        }

        return _serverPort;
    }

    public String remoteHostAddress ()
    {
        String remoteAddr = formValueForKey("REMOTE_ADDR", false);
        if (remoteAddr != null )
        {
            Log.aribaweb.debug("AWServletRequest.remoteHostAddress: REMOTE_ADDR = '%s'",
                               remoteAddr);
        }
        else {
            remoteAddr =  _servletRequest.getRemoteAddr();
            Log.aribaweb.debug("AWServletRequest.remoteHostAddress: getRemoteAddr() = '%s'",
                               remoteAddr);
        }
        return remoteAddr;
    }

    public String remoteHost ()
    {
        return _servletRequest.getRemoteHost();
    }

    public InputStream inputStream ()
    {
        InputStream inputStream = null;
        try {
            inputStream = _servletRequest.getInputStream();
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
        return inputStream;
    }

    public HttpSession getSession (boolean shouldCreate)
    {
        verifySessionIsValid();
        return _servletRequest.getSession(shouldCreate);
    }

    /**
     * If the request does not have a request handler key or if it does not have the
     * sessionRendevous key or return from authentication key (awsso_ar), then the
     * existing session should be invalidated and a new session should be established.
     */
    protected void verifySessionIsValid ()
    {
        if (shouldTerminateSession()) {
            HttpSession httpSession = _servletRequest.getSession(false);
            if (httpSession != null) {
                AWSession awsession = AWSession.session(httpSession);
                if (awsession != null) {
                    // AWSession.terminate must be called before httpSession.invalidate because
                    // subclasses of AWSession may implement terminate() which requires data from the
                    // session. See defect 93887 for more information.
                    awsession.terminate();
                }
                Log.aribaweb.debug("verifySessionIsValid: HttpSession invalidated: %s",
                                   httpSession.getId());
                httpSession.invalidate();
            }
        }
    }

    private boolean shouldTerminateSession ()
    {
        // if there is no request handler, then the request's session (if it exists)
        // is not valid (ie, establish a new session for this user)
        // isSessionRendevous -- needed to keep the return from authentication
        // from killing the session.
        // Error sequence:
        // 1) request without requesthandler causes existing session to be killed
        // 2) new session established
        // 3) new session not validated so bounce to session validator (ie authenticator)
        // 4) session validation sequence occrs (ie authentication)
        // 5) replay initial request on return from session validation
        // 6) same request as 1) so session gets killed again  goto 1)
        // Fix this by adding the sessionRendevous in step 5)
        return (!hasHandler() && !isSessionRendevous());
    }

    protected String initSessionId ()
    {
        // only return the session id if it is valid
        String sessionId = _servletRequest.getRequestedSessionId();
        if (!_servletRequest.isRequestedSessionIdValid()) {
            sessionId = null;
        }
        return sessionId;
    }

    public HttpServletRequest httpServletRequest ()
    {
        return _servletRequest;
    }

    public HttpServletResponse httpServletResponse ()
    {
        return _servletResponse;
    }

    protected void setHttpServletResponse (HttpServletResponse servletResponse)
    {
        _servletResponse = servletResponse;
    }

        // Note that JSPRequest overrides this method
    protected void setupHttpServletRequest (HttpServletRequest servletRequest,
                                            HttpServletResponse servletResponse,
                                            ServletContext servletContext)
    {
        setHttpServletResponse(servletResponse);
    }

    public String requestString ()
    {
        return (_servletRequest.getScheme() +"://" +
                _servletRequest.getServerName() + ":" + _servletRequest.getServerPort() +
                _servletRequest.getContextPath() +
                _servletRequest.getServletPath() +
                ((_servletRequest.getPathInfo() != null) ? _servletRequest.getPathInfo() : "") +
                ((_servletRequest.getQueryString() != null) ? "?"+_servletRequest.getQueryString() : ""));
    }
}
