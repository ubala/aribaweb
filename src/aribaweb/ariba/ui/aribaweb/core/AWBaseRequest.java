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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBaseRequest.java#81 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWDisposable;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFileData;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWMalformedRequestException;
import ariba.ui.aribaweb.util.AWMimeReader;
import ariba.ui.aribaweb.util.AWStringKeyHashtable;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.Parameters;
import ariba.ui.aribaweb.html.BindingNames;
import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MIME;
import ariba.util.core.MapUtil;
import ariba.util.core.ProgressMonitor;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;
import ariba.util.formatter.IntegerFormatter;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

abstract public class AWBaseRequest extends AWBaseObject
    implements AWRequest, AWDisposable
{
    public static final String InitialRequestId = "awinit";
    public static final String AcceptLanguageKey = "accept-language";
    public static final String ContentLengthKey = "content-length";
    public static final String ContentTypeKey = "content-type";
    public static final String DefaultBrowserLanguage = "en-US";
    public static final String IsSessionRendezvousFormKey = "awsso_ar";
    public static final String AllowFailedComponentRendezvousFormKey = "aw_afcr";


    private static final String DefaultSecureHttpPort =
        AWConcreteRequestHandler.DefaultSecureHttpPort;
    private static final String NoApplicationNumber = "NoApplicationNumber";
    private static final String GETMethod = "GET";
    private List _requestLocales;
    private Locale _preferredLocale;
    private String _applicationNumber = NoApplicationNumber;
    private AWStringKeyHashtable _formValues;
    private Map _cookieValues;
    private AWEncodedString _responseId;
    private String _requestId;
    private String _sessionId;
    private String _sessionSecureId;
    private AWEncodedString _frameName;
    private boolean _isSessionRendevous = false;
    private String _contentType;
    private int _contentLength;
    private boolean _isBrowserFirefox;
    private boolean _isBrowserMicrosoft;
    private boolean _isBrowserIE55;
    private boolean _isBrowserSafari;
    private boolean _isBrowserChrome;
    private boolean _isMacintosh;
    private boolean _isIPad;
    private boolean _isTooManyTabRequest;
    private byte[] _content;

    public static final String CharacterEncodingKey = "awcharset";
    private AWCharacterEncoding _characterEncoding = AWCharacterEncoding.Default;
    private String[] _senderIds;
    private boolean _isQueued = false;

    private static final String ComponentName = "AWBaseRequest";

    public static final String AWLogFilterListKey = "AWLogFilter";
    abstract protected int applicationNumberInt ();
    abstract public InputStream inputStream ();
    abstract protected byte[] initContent ();

    // ** Thread Safety Considerations: This is never shared -- no locking required

    public void init ()
    {
        super.init();
        _contentType = headerForKey(ContentTypeKey);
        String contentLength = headerForKey(ContentLengthKey);
        if (!StringUtil.nullOrEmptyOrBlankString(contentLength)) {
            _contentLength = Integer.parseInt(contentLength);
        }
        _formValues = initFormValuesAndSenderId();
        String responseId = formValueForKey(AWRequestContext.ResponseIdKey, false);

        if (responseId != null) {
            _responseId = AWEncodedString.sharedEncodedString(responseId);
            _requestId = responseId;
            String senderId = formValueForKey(AWComponentActionRequestHandler.SenderKey);
            if (senderId != null) {
                _requestId = StringUtil.strcat(_requestId, ".", senderId);
            }
        }
        else {
            _requestId = AWBaseRequest.InitialRequestId;
        }
        _sessionSecureId = formValueForKey(AWRequestContext.SessionSecureIdKey, false);

        //check for explicit session rendevous -- used to signal that the session
        // should not be explicitly terminated (see AWServletRequest.verifySessionIsValid)
        // AWSSOConstants.SSOAuthReturnKey = "awsso_ar" -- can't use constant b/c of
        // packaging and can't add SessionRendevousKey to SSO process or else backward
        // compatibility between our applications will be be broken.
        if (formValueForKey(IsSessionRendezvousFormKey) != null) {
            _isSessionRendevous = true;
        }

        // always set up the session id
        _sessionId = initSessionId();

        _isBrowserFirefox = initIsBrowserFirefox();
        _isBrowserMicrosoft = initIsBrowserMicrosoft();
        _isBrowserSafari = initIsBrowserSafari();
        _isBrowserChrome = initIsBrowserChrome();
        _isMacintosh = initisMacintosh();
        _isIPad = initIsIPad();
        _isTooManyTabRequest = false;
        String frameName = formValueForKey(AWRequestContext.FrameNameKey, false);
        if (frameName != null) {
            _frameName = AWEncodedString.sharedEncodedString(frameName);
        }
    }

    public void dispose ()
    {
        _requestLocales = null;
        _applicationNumber = null;
        Object disposeTarget = _formValues;
        _formValues = null;
       AWUtil.dispose(disposeTarget);
        // Don't need to dispose on _cookieValues because they only hold strings
        // and dispose generates garbage for hash tables.
        _cookieValues = null;
        _responseId = null;
        _requestId = null;
        _sessionId = null;
        _frameName = null;
        _contentType = null;
        _content = null;
    }

    abstract protected String initSessionId ();

    protected AWStringKeyHashtable initFormValuesAndSenderId ()
    {
        return isMultipartEncoded() ?
            parseMultipartEncodedRequest() : parseFormEncodedRequest();
    }

    public boolean hasHandler ()
    {
        return !StringUtil.nullOrEmptyOrBlankString(requestHandlerKey());
    }

    public boolean isMultipartEncoded ()
    {
        // Note: we must check to make sure the method is not GET because
        // IE will use a content-type of MultipartFormData if its redirecting
        // from a response that uses MultipartFormData
        String contentType = contentType();
        return (!GETMethod.equals(method()) &&
                (contentType != null) &&
                (contentType.startsWith(AWContentType.MultipartFormData.name)));
    }

    protected AWStringKeyHashtable parseMultipartEncodedRequest ()
    {
        try {
            AWStringKeyHashtable newHashtable = new AWStringKeyHashtable(1);
            AWMimeReader mimeReader = new AWMimeReader(
                contentStream(), contentLength(), contentType());
            while (true) {
                Parameters parameters = mimeReader.nextHeaders();
                if (parameters == null) {
                    break;
                }
                String disposition = parameters.getParameter(
                    MIME.HeaderContentDisposition);
                if (disposition == null) {
                        // This is the workaround to a bug in the Macintosh version of IE.
                    disposition = parameters.getParameter(
                        AWRequest.HeaderContentDispositionForMacintosh);
                }
                String name = AWMimeReader.mimeArgumentValue(
                    disposition, MIME.ParameterName);
                String fileName = AWMimeReader.mimeArgumentValue(
                    disposition, MIME.ParameterFilename);
                if (fileName != null) {
                    String headerContentType =
                        parameters.getParameter(MIME.HeaderContentType);
                    if (headerContentType == null) {
                        headerContentType = MIME.ContentTypeApplicationOctetStream;
                    }

                    // get locale for this request.  Default to browser preferred locale.
                    Locale locale = preferredLocale();
                    // get max size for this request
                    int maxLength = AWMimeReader.maxBytesPerChunk();
                    boolean encrypted = false;
                    HttpSession httpSession = getSession(false);
                    if (httpSession != null) {
                        Integer length = (Integer)httpSession.getAttribute(name);
                        if (length != null) {
                            maxLength = length.intValue();
                        }
                        Boolean enc = (Boolean)httpSession.getAttribute(BindingNames.encrypt +"."+name);
                        if (enc != null) {
                            encrypted = enc;
                        }
                        locale = (Locale)httpSession.getAttribute(Locale.class.getName());
                    }

                    String sessionId = initSessionId();
                    ProgressMonitor.register(sessionId);

                    // Message for file upload status panel
                    String msg = localizedJavaString(ComponentName, 1, "Uploaded %s KB of %s KB...",
                                        AWConcreteServerApplication.sharedInstance().resourceManager(locale));

                    ProgressMonitor.instance().prepare(msg, contentLength()/1024);

                    AWFileData fileData =
                        mimeReader.nextChunk(fileName, headerContentType, maxLength, encrypted);

                    if (fileData != null) {
                        newHashtable.put(name, fileData);
                    }
                }
                else {
                    byte[] nextChunk = mimeReader.nextChunk();
                    int nextChunkLength = nextChunk.length;
                    if (nextChunkLength > 0) {
                        if (nextChunk[nextChunkLength - 1] == '\n') {
                            nextChunkLength--;
                            if (nextChunk[nextChunkLength - 1] == '\r') {
                                nextChunkLength--;
                            }
                        }
                    }
                    // Todo: This shouldn't be hard coded to UTF8
                    String nextChunkString = new String(nextChunk, 0,
                        nextChunkLength, AWCharacterEncoding.UTF8.name);
                    String[] nextChunkArray = (String[])newHashtable.get(name);

                    if (nextChunkArray != null) {
                        nextChunkArray = (String[])AWUtil.addElement(
                            nextChunkArray, nextChunkString);
                    }
                    else {
                        nextChunkArray = new String[1];
                        nextChunkArray[0] = nextChunkString;
                    }

                    newHashtable.put(name, nextChunkArray);
                }
            }
            return newHashtable;
        }
        catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new AWGenericException(unsupportedEncodingException);
        }
        catch (ProtocolException pe) {
            if (Log.aribaweb_fileupload.isDebugEnabled()) {
                Log.logException(Log.aribaweb_fileupload, pe);
            }
            throw new AWMalformedRequestException(pe);
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
        catch (Exception e) {
            throw new AWMalformedRequestException(e);
        }

    }

    protected AWStringKeyHashtable parseFormEncodedRequest ()
    {
        AWStringKeyHashtable newHashtable = null;
        Parameters formValues = AWBaseRequest.parameters(this);
        if (formValues != null) {
            String characterEncodingName =
                formValues.getParameter(AWBaseRequest.CharacterEncodingKey);
            AWCharacterEncoding characterEncoding = null;
            if (!StringUtil.nullOrEmptyOrBlankString(characterEncodingName)) {
                characterEncoding = AWCharacterEncoding.characterEncodingNamed(
                    characterEncodingName);
                if (characterEncoding != null) {
                    setCharacterEncoding(characterEncoding);
                }
            }
            // subclass might have initialized it.
            characterEncoding = characterEncoding();
            int elementCount = formValues.getParameterCount();
            newHashtable =  new AWStringKeyHashtable(
                (elementCount == 0) ? 1 : elementCount);
            Iterator keyEnumerator = formValues.getParameterNames();

            String[] debugFilterKeys = null;
            boolean isAribawebDebugEnabled = Log.aribaweb_request.isDebugEnabled();
            if (isAribawebDebugEnabled) {
                Log.aribaweb_request.debug("---> form values");
                // check for debug filter values
                debugFilterKeys = formValues.getParameterValues(AWLogFilterListKey);
            }
            while (keyEnumerator.hasNext()) {
                String keyString = (String)keyEnumerator.next();
                String[] formValuesArray = formValues.getParameterValues(keyString);
                if (isAribawebDebugEnabled) {
                    boolean filter = false;
                    if (debugFilterKeys != null) {
                        for (int i=0, size=debugFilterKeys.length;
                             i < size && !filter; i++) {
                            filter = debugFilterKeys[i].equals(keyString);
                        }
                    }

                    Log.aribaweb_request.debug("name: %s,  value: %s",
                        keyString, filter ? "**** filtered ****" :
                        formValuesArray.length == 1 ? formValuesArray[0] :
                            ArrayUtil.formatArray("",formValuesArray));
                }
                if (characterEncoding != null &&
                    !characterEncoding.equals(AWCharacterEncoding.ISO8859_1)) {
                    AWBaseRequest.convertStrings(formValuesArray, characterEncoding);
                }
                newHashtable.put(keyString, formValuesArray);
            }
        }
        else {
            newHashtable = new AWStringKeyHashtable(1);
        }
        return newHashtable;
    }

    /**
     * Returns the HTTP User-Agent field or null if one does not exist.
     * See also RFC2616, section 14.43.
     * @return Returns the User-Agent or null.
     */
    public String userAgent ()
    {
        return headerForKey("user-agent");
    }

    public String contentType ()
    {
        return _contentType;
    }

    public int contentLength ()
    {
        return _contentLength;
    }

    public abstract String remoteHost ();
    public abstract String remoteHostAddress ();

    public boolean initIsBrowserFirefox ()
    {
        boolean isBrowserFirefox = false;
        String userAgent = userAgent();
        if ((userAgent != null) && (userAgent.indexOf("Gecko/") != -1)) {
            isBrowserFirefox = true;
        }
        return isBrowserFirefox;
    }

    public boolean initIsBrowserMicrosoft ()
    {
        boolean isBrowserMicrosoft = false;
        String userAgent = userAgent();
        if ((userAgent != null) && (userAgent.indexOf("MSIE") != -1)) {
            isBrowserMicrosoft = true;
            if (userAgent.indexOf("5.5") != -1) {
                _isBrowserIE55 = true;
            }
        }
        return isBrowserMicrosoft;
    }

    public boolean initIsBrowserSafari ()
    {
        boolean isBrowserSafari = false;
        String userAgent = userAgent();
        if ((userAgent != null) && (userAgent.indexOf("Safari") != -1)
            && (userAgent.indexOf("Version/") != -1)) {
            isBrowserSafari = true;
        }
        return isBrowserSafari;
    }

    public boolean initIsBrowserChrome ()
    {
        boolean isBrowserChrome = false;
        String userAgent = userAgent();
        if ((userAgent != null) && (userAgent.indexOf("Chrome") != -1)) {
            isBrowserChrome = true;
        }
        return isBrowserChrome;
    }

    public boolean initisMacintosh ()
    {
        boolean isMacintosh = false;
        String userAgent = userAgent();
        if ((userAgent != null) && (userAgent.indexOf("Mac_PowerPC") != -1)) {
            isMacintosh = true;
        }
        return isMacintosh;
    }

    public boolean initIsIPad ()
    {
        boolean isIPad = false;
        String userAgent = userAgent();
        if ((userAgent != null) && (userAgent.indexOf("iPad") != -1)) {
            isIPad = true;
        }
        return isIPad;
    }

    public String acceptLanguage ()
    {
        return headerForKey(AcceptLanguageKey);
    }

    public List requestLocales ()
    {
        if (_requestLocales == null) {
            String acceptLanguageString = acceptLanguage();
            _requestLocales = AWUtil.localesForAcceptLanagugeString(acceptLanguageString);
        }
        return _requestLocales;
    }

    public Locale preferredLocale ()
    {
        if (_preferredLocale == null) {
            String acceptLanguageString = acceptLanguage();
            String browserLanguageString = DefaultBrowserLanguage;
            if ((acceptLanguageString != null) && (acceptLanguageString.length() > 0)) {
                int commaIndex = acceptLanguageString.indexOf(',');
                browserLanguageString = (commaIndex == -1) ?
                    acceptLanguageString :
                    acceptLanguageString.substring(0, commaIndex);
            }
            _preferredLocale =
                AWUtil.localeForBrowserLanguageString(browserLanguageString);
        }
        return _preferredLocale;
    }

    protected void setApplicationNumber (String applicationNumber)
    {
        _applicationNumber = applicationNumber;
    }

    public String applicationNumber ()
    {
        if (_applicationNumber == NoApplicationNumber) {
            int applicationNumberInt = applicationNumberInt();
            if (applicationNumberInt == -1) {
                setApplicationNumber(null);
            }
            else {
                setApplicationNumber(AWUtil.toString(applicationNumberInt));
            }
        }
        return _applicationNumber;
    }

    public boolean isBrowserFirefox()
    {
        return _isBrowserFirefox;
    }

    public boolean isBrowserMicrosoft ()
    {
        return _isBrowserMicrosoft;
    }

    public boolean isBrowserIE55 ()
    {
        return _isBrowserIE55;
    }

    public boolean isBrowserSafari ()
    {
        return _isBrowserSafari;
    }

    public boolean isBrowserChrome ()
    {
        return _isBrowserChrome;
    }

    public boolean isMacintosh ()
    {
        return _isMacintosh;
    }

    public boolean isIPad ()
    {
        return _isIPad;
    }

    public boolean isSecureScheme ()
    {
            // Default implementation -- if request came in on a secure port, use a
            // secure scheme when manufacturing URLs for this request.
        String serverPort = serverPort();
        if (serverPort == null) {
            return false;
        }

        return serverPort.equals(DefaultSecureHttpPort) ||
            serverPort.equals(AWDirectActionUrl.alternateSecurePort());
    }

    public boolean isTooManyTabRequest ()
    {
        return _isTooManyTabRequest;
    }

    public void setTooManyTabRequest (boolean isTooManyTabRequest)
    {
        _isTooManyTabRequest = isTooManyTabRequest;
    }

    /////////////////////
    // Sender/responseId
    /////////////////////
    public String requestId ()
    {
        return  _requestId;
    }

    protected void setRequestId (String requestId)
    {
        _requestId = requestId;
    }

    public AWEncodedString responseId ()
    {
        return _responseId;
    }

    protected void setResponseId (String responseId)
    {
        _responseId = AWEncodedString.sharedEncodedString(responseId);
    }

    public String sessionId ()
    {
        return _sessionId;
    }

    public String sessionSecureId ()
    {
        return _sessionSecureId;
    }

    public AWEncodedString frameName ()
    {
        return _frameName;
    }

    public void resetRequestId ()
    {
        _requestId = InitialRequestId;
    }

    protected boolean isSessionRendevous ()
    {
        return _isSessionRendevous;
    }

    /**
     Produce a URI which can be sent to the client and used later
     to replay the request.

     This method should decorate the URI or mutate the form values as necessary
     to ensure the request will be associated back to the same application instance
     and session (not necessary when cookies are used for this purpose).

     @param requestContext from the original request
     @param formValues form values from the original request (which will also be included in
     the replayed request); may be changed by this method
     @return URI for client
     */
    protected String uriForReplay (AWRequestContext requestContext, Map formValues)
    {
        return AWRequestUtil.getRequestUrlMinusQueryString(requestContext);
    }

    ///////////////
    // Form Values
    ///////////////
    public Map formValues ()
    {
        return _formValues;
    }

    public String formValueForKey (String formValueKey, boolean ignoresCase)
    {
        String[] formValuesArray = formValuesForKey(formValueKey, ignoresCase);
        return (formValuesArray == null) ? null : formValuesArray[0];
    }

    public String formValueForKey (String formValueKey)
    {
        return formValueForKey(formValueKey, false);
    }

    public String formValueForKey (AWEncodedString formValueKey)
    {
        return formValueForKey(formValueKey.string(), false);
    }

    public String[] formValuesForKey (String formValueKey, boolean ignoresCase)
    {
        String[] formValuesForKey = null;
        if (_formValues != null) {
            formValuesForKey = (String[])_formValues.get(formValueKey, ignoresCase);
        }
        return formValuesForKey;
    }

    public String[] formValuesForKey (String formValueKey)
    {
        return formValuesForKey(formValueKey, false);
    }

    public String[] formValuesForKey (AWEncodedString formValueKey)
    {
        return formValuesForKey(formValueKey.string(), false);
    }

    public boolean hasFormValueForKey (String formValueKey, boolean ignoresCase)
    {
        String[] formValuesArray = formValuesForKey(formValueKey, ignoresCase);
        return (formValuesArray != null);
    }

    public boolean hasFormValueForKey (String formValueKey)
    {
        return hasFormValueForKey(formValueKey, false);
    }

    public boolean hasFormValueForKey (AWEncodedString formValueKey)
    {
        return hasFormValueForKey(formValueKey.string(), false);
    }

    public AWCharacterEncoding characterEncoding ()
    {
        return _characterEncoding;
    }

    public void setCharacterEncoding (AWCharacterEncoding characterEncoding)
    {
        _characterEncoding = characterEncoding;
    }

    //////////////////
    // Util
    //////////////////
    /////////////////
    // Input Parsing
    /////////////////
    /*
        This is quite inefficient compared to what it could be if we didn't keep converting everything to strings.  We should first find the characterEncoding by scanning the content bytes, then we can iterate through the bytes of the content doing the url decoding as we go.  When we encounter an '=' or '&', we start finish the current string and start a new one, putting the pairs into the parameters as we go.
    */
    /**
        Parse a string that is URL encoded.

        NOTE: This code is based on ariba.util.net.CGI
    */
    public static String decodeString (String string, FastStringBuffer buffer)
    {
        buffer.truncateToLength(0);
        int stringLength = string.length();
        for (int index = 0; index < stringLength; index++) {
            char currentChar = string.charAt(index);
            switch (currentChar) {
              case '+':
                buffer.append(' ');
                break;
              case '%':
                String hexString = null;
                try {
                    hexString = string.substring(index + 1, index + 3);
                    char hexValue = (char)IntegerFormatter.parseInt(hexString, 16);
                    buffer.append(hexValue);
                }
                catch (ParseException exception) {
                        // if the parse fails just append the chars
                    buffer.append(hexString);
                }
                index += 2;
                break;
              default:
                buffer.append(currentChar);
                break;
            }
        }
        return buffer.toString();
    }

    /**
        Parse CGI form content.

        NOTE: This code is based on ariba.util.net.CGI
     */
    public static Parameters parametersFromUrlEncodedString (String formValuesString)
    {
        // Catch any query strings with escaped html.
        if (formValuesString.matches(".*&(amp|lt|gt|quot);.*")) {
            throw new AWGenericException(
                "Invalid formValuesString -- contains escaped html: " +
                formValuesString);
        }
        Parameters parameters = new Parameters();
        FastStringBuffer buffer = new FastStringBuffer();
        StringTokenizer tokens = new StringTokenizer(formValuesString, "&");
        while (tokens.hasMoreTokens()) {
            String keyValuePair = tokens.nextToken();
            int pos = keyValuePair.indexOf('=');
            if (pos == -1) {
                // if there is no = sign, then log warning and skip this parameter
                // "Invalid formValuesString -- missing '=', skipping: %s"
                Log.aribaweb.warning(9282, keyValuePair);
                continue;
            }
            String key = keyValuePair.substring(0, pos);
            String value = keyValuePair.substring(pos+1, keyValuePair.length());
            key = decodeString(key, buffer);
            value = decodeString(value, buffer);
            parameters.putParameter(key, value);
        }
        return parameters;
    }

    /**
        NOTE: This code is based on ariba.util.net.HTTPRequest
    */
    protected static Parameters parameters (AWRequest request)
    {
        Parameters parameters = null;
        String contentString = null;
        String queryStringForPost = null;
        String contentType = request.contentType();
        if (!StringUtil.nullOrEmptyOrBlankString(contentType)) {
            String[] values = StringUtil.delimitedStringToArray(contentType, ';');
            contentType = values[0].trim();
        }
        if (contentType == null || GETMethod.equals(request.method())) {
            String queryString = request.queryString();
            if (queryString != null) {
                contentString = queryString;
                if (contentString.equals("null")) {
                    contentString = null;
                }
            }
        }
        else if (contentType.equalsIgnoreCase(
            AWContentType.ApplicationWWWFormUrlEncoded.name)) {
            byte[] contentBytes = request.content();
            if (contentBytes != null && contentBytes.length > 0) {
                contentString = new String(contentBytes);
            }
            //sometimes we need parameters in the query string for POST also.
            //add query string parameters also to form values
            //if they don't exist already
            queryStringForPost = request.queryString();
            Log.aribaweb_request.debug("queryStringForPost:%s", queryStringForPost);
            if (queryStringForPost != null
                    && queryStringForPost.equals("null")) {
                queryStringForPost = null;
            }

        }
        if (!StringUtil.nullOrEmptyOrBlankString(contentString)) {
            parameters = parametersFromUrlEncodedString(contentString);
        }
        if (!StringUtil.nullOrEmptyOrBlankString(queryStringForPost) &&
                parameters != null) {
            Parameters queryParameters = parametersFromUrlEncodedString(queryStringForPost);
            Iterator itr = queryParameters.getParameterNames();
            while(itr.hasNext()) {
                String name = (String)itr.next();
                //add to parameters if it is not already exist.
                if (parameters.getParameterValues(name) == null) {
                    Log.aribaweb_request.debug("Adding query string parameter to form:%s",
                                                        name);
                    parameters.putParameter(name,
                            queryParameters.getParameter(name));
                }
            }
        }

        return parameters;
    }

    protected static void convertStrings (String[] formValuesArray,
                                          AWCharacterEncoding characterEncoding)
    {
        int formValuesArrayLength = formValuesArray.length;
        String iso8859Name = AWCharacterEncoding.ISO8859_1.name;
        for (int index = 0; index < formValuesArrayLength; index++) {
            try {
                String currentString = formValuesArray[index];
                // this trick contributed by tkanno.
                byte[] iso8859Bytes = currentString.getBytes(iso8859Name);
                formValuesArray[index] = new String(iso8859Bytes, characterEncoding.name);
            }
            catch (UnsupportedEncodingException unsupportedEncodingException) {
                throw new AWGenericException(unsupportedEncodingException);
            }
        }
    }

    //////////////////////
    // Public Convenience
    //////////////////////
    /*
        Currently, this is not used internally, but is a way for Garrick
        to take advantage of the parsing code of this class.
        Eventually, I want to completely rewrite the code for this parsing
        stuff to be more efficient, and at that time,
        I'll unify the code paths so that my code uses this as well.
    */
    public static Map parseUrlEncodedFormValues (String formValuesString)
    {
        AWStringKeyHashtable newHashtable = null;
        Parameters parameters =
            AWBaseRequest.parametersFromUrlEncodedString(formValuesString);
        if (parameters != null) {
            String characterEncodingName =
                parameters.getParameter(AWBaseRequest.CharacterEncodingKey);
            AWCharacterEncoding characterEncoding = null;
            if (!StringUtil.nullOrEmptyOrBlankString(characterEncodingName)) {
                characterEncoding =
                    AWCharacterEncoding.characterEncodingNamed(characterEncodingName);
            }
            int elementCount = parameters.getParameterCount();
            newHashtable = new AWStringKeyHashtable(
                (elementCount == 0) ? 1 : elementCount);
            Iterator keyEnumerator = parameters.getParameterNames();
            while (keyEnumerator.hasNext()) {
                String keyString = (String)keyEnumerator.next();
                String[] formValuesArray = parameters.getParameterValues(keyString);
                if (characterEncoding != null &&
                    !characterEncoding.equals(AWCharacterEncoding.ISO8859_1)) {
                    AWBaseRequest.convertStrings(formValuesArray, characterEncoding);
                }
                newHashtable.put(keyString, formValuesArray);
            }
        }
        else {
            newHashtable = new AWStringKeyHashtable(1);
        }
        return newHashtable;
    }

    public HttpSession getSession ()
    {
        return getSession(true);
    }

    // Cookie Support

    protected static Map parseCookieHeader (String cookieHeaderString)
    {
        Map cookieValues = MapUtil.map();
        StringArray pairs = AWUtil.componentsSeparatedByString(cookieHeaderString, ";");
        String[] pairsArray = pairs.array();
        for (int pairIndex = (pairs.inUse() - 1); pairIndex > -1; pairIndex--) {
            String pair = pairsArray[pairIndex];
            pair = pair.trim();
            StringArray keyAndValue = AWUtil.componentsSeparatedByString(pair, "=");
            if (keyAndValue.inUse() != 2) {
                continue;
            }
            String[] keyAndValueArray = keyAndValue.array();
            String key = keyAndValueArray[0];
            String value = keyAndValueArray[1];
            Object existingValues = cookieValues.get(key);
            if (existingValues == null) {
                cookieValues.put(key, value);
            }
            else {
                String[] valueArray = null;
                if (existingValues instanceof String) {
                    valueArray = new String[2];
                    valueArray[0] = (String)existingValues;
                    valueArray[1] = value;
                }
                else {
                    valueArray = (String[])AWUtil.addElement(
                        (String[])existingValues, value);
                }
                cookieValues.put(key, valueArray);
            }
        }
        return cookieValues;
    }

    protected String cookieHeader ()
    {
        String cookieHeader = headerForKey("HTTP_COOKIE");
        if (cookieHeader == null) {
            cookieHeader = headerForKey("cookie");
        }
        return cookieHeader;
    }

    /**
     * Overridden by subclasses (e.g. AWServletAdaptor) to use container-specific APIs for cookie handling.
     */
    protected Map computeCookieValues ()
    {
        String cookieHeader = cookieHeader();
        if (cookieHeader != null) {
            return parseCookieHeader(cookieHeader);
        }
        return null;
    }

    protected Map cookieValues ()
    {
        // Note: this contains a mixture of String's and String[]'s
        if (_cookieValues == null) {
            _cookieValues = MapUtil.map();
            Map parsedCookies = computeCookieValues();
            if (parsedCookies != null) {
                AWUtil.addElements(_cookieValues, parsedCookies);
            }
        }
        return _cookieValues;
    }

    public void removeCookieValue (String cookieName)
    {
        cookieValues().remove(cookieName);
    }

    public String[] cookieValuesForKey (String cookieName)
    {
        String[] valueArray = null;
        Map cookieValues = cookieValues();
        Object value = cookieValues.get(cookieName);
        if (value instanceof String) {
            valueArray = new String[1];
            valueArray[0] = (String)value;
            cookieValues.put(cookieName, valueArray);
        }
        else {
            valueArray = (String[])value;
        }
        return valueArray;
    }

    public String cookieValueForKey (String cookieName)
    {
        String cookieValue = null;
        Object value = cookieValues().get(cookieName);
        if (value != null) {
            cookieValue = (value instanceof String) ?
                (String)value : ((String[])value)[0];
        }
        return cookieValue;
    }


    public InputStream contentStream ()
    {
        if (_content != null) {
            return new ByteArrayInputStream(content());
        }
        else {
            return inputStream();
        }
    }

    public byte[] content ()
    {
        if (_content == null) {
            _content = initContent();
        }
        return _content;
    }

    public AWEncodedString senderId ()
    {
        Assert.that(false,
            "Do not use call method.  Call AWRequestContext.requestSenderId()");
        return null;
    }

    public String[] senderIds ()
    {
        if (_senderIds == null) {
            String senderId = formValueForKey(AWComponentActionRequestHandler.SenderKey);
            if (senderId != null) {
                _senderIds = StringUtil.delimitedStringToArray(senderId, ',');
            }
        }
        return _senderIds;
    }

    protected void setIsQueued (boolean flag)
    {
        _isQueued = flag;
    }

    protected boolean isQueued ()
    {
        return _isQueued;
    }

    /*
        Creates an AWRequest suitable for internal dispatch (i.e. in process) via
        AWDirectActionRequestHandler.internalDispatch
    */
    public static AWBaseRequest createInternalRequest (String url, AWBaseRequest originalRequest)
    {
        AWBaseRequest.InternalRequest request = new AWBaseRequest.InternalRequest(url, originalRequest, null);
        request.init();
        return request;
    }

    public static AWBaseRequest createInternalRequest (String url, String applicationNumber)
    {
        AWBaseRequest.InternalRequest request = new AWBaseRequest.InternalRequest(url, null, applicationNumber);
        request.init();
        return request;
    }

    public static boolean isInternalRequest (AWRequestContext requestContext)
    {
        return requestContext.request() instanceof InternalRequest;
    }

    public AWBaseRequest getBaseRequest ()
    {
        return this;
    }
    /*
        A request object for dispatching requests internally.
        This supports internal dispatch of DirectActions by providing an overlay URL (for the direct action
        name and query params) and uses the original (external) request for other info (headers, cookies, ...)
    */
    static protected class InternalRequest extends AWBaseRequest
    {
        URL _url;
        AWConcreteApplication.RequestURLInfo _urlInfo;

        AWBaseRequest _baseRequest;
        InputStream _inputStream;
        int _applicationNumberInt;
        String _initSessionId;
        String _remoteHost;
        String _remoteHostAddress;
        Map _headers;
        String _serverPort;

        public InternalRequest (String url, AWBaseRequest baseRequest, String applicationNum)
        {
            try {
                _url = new URL(url);
            } catch (MalformedURLException e) {
                throw new AWGenericException(e);
            }

            _urlInfo = ((AWConcreteApplication)AWConcreteApplication.sharedInstance()).requestUrlInfo(url);

            if (baseRequest != null) {
                _baseRequest = baseRequest;
                _inputStream = _baseRequest.inputStream();
                _applicationNumberInt = _baseRequest.applicationNumberInt();
                _initSessionId = _baseRequest.initSessionId();
                // note: use remoteHostAddress here to prevent a reverse DNS look up
                //       which, in the case of slow DNS server, can cause requests to lag
                _remoteHost = _baseRequest.remoteHostAddress();
                _remoteHostAddress = _baseRequest.remoteHostAddress();
                _headers = _baseRequest.headers();
                _serverPort = _baseRequest.serverPort();
            } else {
               _headers = MapUtil.map();
               _applicationNumberInt = (applicationNum != null) ? Integer.parseInt(applicationNum) : -1;
            }
        }

        public String method() {
            return GETMethod;
        }

        public String requestHandlerKey() {
            return _urlInfo.requestHandlerKey;
        }

        public String[] requestHandlerPath() {
            return _urlInfo.requestHandlerPath;
        }

        public String uri() {
            return _url.getPath();
        }

        public String queryString() {
            return _url.getQuery();
        }

        public String requestString() {
            return _url.toExternalForm();
        }

        protected byte[] initContent() {
            return new byte[0];
        }

        public AWFileData fileDataForKey(String formValueKey) {
            return null;
        }

        public AWBaseRequest getBaseRequest ()
        {
            return _baseRequest;
        }

        public String headerForKey(String requestHeaderKey) {
            return (String)headers().get(requestHeaderKey);
        }

        public HttpSession getSession(boolean shouldCreate) {
            Assert.that(_baseRequest != null, "Attempt to create session during session-less InternalRequest");
            return _baseRequest.getSession(shouldCreate);
        }

        public InputStream inputStream() {
            return _inputStream;
        }

        protected int applicationNumberInt() {
            return _applicationNumberInt;
        }

        protected String initSessionId() {
            return _initSessionId;
        }

        public String remoteHost() {
            return _remoteHost;
        }

        public String remoteHostAddress() {
            return _remoteHostAddress;
        }

        public Map headers() {
            return _headers;
        }

        public String serverPort() {
            return _serverPort;
        }

        public void setInputStream(InputStream inputStream) {
            _inputStream = inputStream;
        }

        public void setApplicationNumberInt(int applicationNumberInt) {
            _applicationNumberInt = applicationNumberInt;
        }

        public void setInitSessionId(String initSessionId) {
            _initSessionId = initSessionId;
        }

        public void setRemoteHost(String remoteHost) {
            _remoteHost = remoteHost;
        }

        public void setRemoteHostAddress(String remoteHostAddress) {
            _remoteHostAddress = remoteHostAddress;
        }

        public void setHeaders(Map headers) {
            _headers = headers;
        }

        public void setServerPort(String serverPort) {
            _serverPort = serverPort;
        }
    }
}
