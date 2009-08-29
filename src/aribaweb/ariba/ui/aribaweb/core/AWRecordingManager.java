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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRecordingManager.java#43 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import java.util.Map;
import ariba.util.core.StringUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.core.Date;
import ariba.util.i18n.I18NUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import ariba.ui.aribaweb.util.Log;

import javax.servlet.http.HttpServletRequest;


public class AWRecordingManager extends AWBaseObject
{
    public final static String HeaderSemanticKeyCount = "X-SemanticKeyCount";
    public final static String HeaderSemanticKeySize = "X-SemanticKeySize";
    public final static String HeaderResponseId = "X-ResponseId";
    public final static String HeaderFrameName = "X-FrameName";
    public final static String HeaderRealContentLength = "X-RealContentLength";
    public final static String HeaderContentLength = "Content-Length";
    public final static String HeaderContentType = "Content-Type";
    public final static String HeaderPlayBackMode = "AWPlayBackMode";
    public final static String HeaderStreamingResponse = "X_StreamingResponse";
    public final static String HeaderResponseDate = "X-ResponseDate";
    public final static String HeaderResponseDateTime = "X-ResponseDateTime";
    public final static String HeaderPageName = "X-PageName";
    public final static String HeaderPageType = "X-PageType";

    public final static String MappingSectionStartMark =
        new String("<!---  Semantic Key to Element Key Mapping");
    public final static String MappingSectionEndMark =
        new String("-->");
    public final static String SemanticKeyMark =
            new String("X-SemanticKey");

    public static final String Newline = AWConstants.Newline.string();
    public static final char NewlineChar = '\n';
    public static final String Equal = AWConstants.Equals.toString();
    public final static String RequestFileSuffix  = "_request.txt";
    public final static String ResponseFileSuffix = "_response.txt";
    public final static String FullResponseFileSuffix = "_fullResponse.txt";
    public final static String XMLResponseFileSuffix = "_response.xml";
    public final static String HTMLResponseFileSuffix = "_response.html";
    public final static String HTMLFullResponseFileSuffix = "_fullResponse.html";    

    public final static String CookieRecordingMode = "awRecordingMode";
    public final static String CookiePlaybackMode = "awPlaybackMode";
    public final static String CookieLastRecordDir = "awLastRecordDir";
    public final static int FileTypeResponse = 1;
    public final static int FileTypeFullResponse = 2;

    public static final AWEncodedString AWRecordPlayBackFrameName =
        AWEncodedString.sharedEncodedString("AWRecordPlayBackFrame");

    protected final static Map _recordingInstances = MapUtil.map();
    private static boolean _inPlaybackModeGlobal = false;
    protected static RecordingMonitor _recordingMonitor;

    protected File _recordingDirectory;
    protected int  _requestCount;

    protected static AWRecordingManager instance = new AWRecordingManager();

    public static AWRecordingManager getInstance ()
    {
        return instance;
    }

    protected AWRecordingManager ()
    {
    }

    protected AWRecordingManager (String file)
    {
        _recordingDirectory = new File(file);
        _requestCount = -1;
        if (_recordingDirectory.exists()) {
            if (!_recordingDirectory.isDirectory()) {
                Assert.assertNonFatal(false,  "recording path must be a directory name %s", _recordingDirectory);
            }
//            IOUtil.deleteDirectory(_recordingDirectory);
        }
        else if (!_recordingDirectory.mkdirs()) {
            Assert.assertNonFatal(false, "can't create recording directory %s", _recordingDirectory);
        }
    }

    /*----------------------------------------------------------------
        public static methods
    ---------------------------------------------------------------*/
    public static boolean isInRecordingMode (AWRequestContext requestContext)
    {
        if (requestContext == null) {
            return false;
        }

        AWRequest request = requestContext.request();
        if (request == null) {
            return false;
        }

        // if there is a registered recording monitor, check its recording mode
        if (_recordingMonitor != null) {
            return _recordingMonitor.isInRecordingMode(requestContext);
        }

        return instance(request) != null;
    }

    public boolean startRecording (AWRequestContext requestContext,
                                          AWResponse response,
                                          String recordingPath)
    {
        AWRequest request = requestContext.request();
        if (_recordingInstances.get(recordingPath) != null) {
            return false;
        }

        AWCookie cookie = new AWCookie(CookieRecordingMode,
                recordingPath,
                null,
                requestPath(requestContext),
                request.isSecureScheme(), -1);
        response.addCookie(cookie);
        AWRecordingManager manager = new AWRecordingManager(recordingPath);
        _recordingInstances.put(recordingPath, manager);
        return true;
    }

    public void stopRecording (AWRequestContext requestContext,
                                      AWResponse response)
    {
        AWRequest request = requestContext.request();
        String cookieValue = request.cookieValueForKey(CookieRecordingMode);
        if (StringUtil.nullOrEmptyOrBlankString(cookieValue)) {
            return;
        }
        _recordingInstances.remove(cookieValue);
        AWCookie cookie = new AWCookie(CookieRecordingMode,
                cookieValue, null,
                requestPath(requestContext), request.isSecureScheme(), 0);
        response.addCookie(cookie);
        requestContext._debugSkipRecordPlayback();
        cookie = new AWCookie(CookieLastRecordDir,
                cookieValue, null,
                requestPath(requestContext), request.isSecureScheme(), -1);
        response.addCookie(cookie);
    }

    protected static String requestPath (AWRequestContext requestContext)
    {
        return requestContext.application().applicationUrl(requestContext.request());
    }

    public static AWRecordingManager instance (AWRequest request)
    {
        String cookieValue = request.cookieValueForKey(CookieRecordingMode);
        if (StringUtil.nullOrEmptyOrBlankString(cookieValue)) {
            cookieValue = request.cookieValueForKey(CookiePlaybackMode);
            if (StringUtil.nullOrEmptyOrBlankString(cookieValue)) {
                return null;
            }
        }
        return (AWRecordingManager)_recordingInstances.get(cookieValue);
    }

    public static AWRecordingManager instance (String path)
    {
        return (AWRecordingManager)_recordingInstances.get(path);
    }

    public File recordingDirectory ()
    {
        return _recordingDirectory;
    }
    
    public void recordRequest (AWRequest request)
    {
        _requestCount ++;
        try {
            String fileName = filenameForRequest(_requestCount);
            File file = new File(_recordingDirectory, fileName);
            BufferedOutputStream outputStream = new BufferedOutputStream
                (new FileOutputStream(file));
            recordRequest(request, outputStream);
            outputStream.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public OutputStream recordingOutputStream (int fileType)
            throws IOException
    {
        String fileName = filenameForResponse(_requestCount, fileType);
        File file = new File(_recordingDirectory, fileName);
        OutputStream outputStream = new BufferedOutputStream(
                                       new FileOutputStream(file));
        return outputStream;
    }

    public OutputStream recordingOutputStream ()
            throws IOException
    {
        return recordingOutputStream(FileTypeResponse);
    }

    public OutputStream recordingFullResponseOutputStream ()
            throws IOException
    {
        return recordingOutputStream(FileTypeFullResponse);
    }

        // Note: for recording
        //    Request       = Request-Line             ; Section 5.1
        //                   *( general-header         ; Section 4.5
        //                    | request-header         ; Section 5.3
        //                    | entity-header )        ; Section 7.1
        //                   CRLF
        //                   [ message-body ]          ; Section 7.2

    protected static void recordRequest (AWRequest request, OutputStream output)
      throws IOException
    {

        FastStringBuffer sb = new FastStringBuffer();
        sb.append(request.method());
        sb.append(" ");
        sb.append(request.uri());
        sb.append("?");
        sb.append(request.queryString());
        sb.append(" ");
        sb.append("HTTP/1.1");
        sb.append(Newline);

        output.write(sb.toString().getBytes(I18NUtil.EncodingUTF8));
        writeHeaders(request, output);
        output.write(Newline.getBytes(I18NUtil.EncodingUTF8));

        byte[] content = request.content();
        if (content != null) {
            output.write(content);
        }
    }

    public static void appendSemanticKeyTable (AWResponse response,
                                               OutputStream outputStream)
            throws IOException
    {
        if (hasSemanticKeys(response)) {
            outputStream.write(semanticKeyTableBytes(response));
            response._debugSetSemanticKeyMapping(null);
        }
    }

    private static boolean hasSemanticKeys (AWResponse response)
    {
        return getSemanticKeyToElementIdMappingIfAny(response) != null;
    }

    private static Map getSemanticKeyToElementIdMappingIfAny (AWResponse response)
    {
        if (!((AWBaseResponse)response)._debugIsStreamingResponse()) {
            Map semanticKeyToElementIdTable = response.semanticKeyToElementIdTable();
            return MapUtil.nullOrEmptyMap(semanticKeyToElementIdTable) ?
                    null : semanticKeyToElementIdTable;
        }
        return null;
    }


    private static byte[] semanticKeyTableBytes (AWResponse response)
    {
        byte[] semanticKeys = response._debugGetSemanticKeyMapping();
        if (semanticKeys == null) {
            Map semanticKeyToElementIdTable = getSemanticKeyToElementIdMappingIfAny(response);
            FastStringBuffer buffer = new FastStringBuffer();
            buffer.append(MappingSectionStartMark);
            buffer.append(Newline);
            Iterator keys = semanticKeyToElementIdTable.keySet().iterator();
            while (keys.hasNext()) {
                SemanticKey semanticKey = (SemanticKey)keys.next();
                Object elementId = semanticKeyToElementIdTable.get(semanticKey);
                buffer.append(AWRecordingManager.SemanticKeyMark);
                buffer.append(AWConstants.Colon.toString());
                buffer.append(semanticKey.uniqueKey());
                buffer.append(Equal);
                buffer.append(elementId.toString());
                buffer.append(Newline);
            }
            buffer.append(MappingSectionEndMark);
            try {
                semanticKeys = buffer.toString().getBytes(I18NUtil.EncodingUTF8);
            }
            catch (IOException ex) {
                throw new WrapperRuntimeException(ex);
            }

            response._debugSetSemanticKeyMapping(semanticKeys);
        }
        return semanticKeys;
    }


    private static void writeHeaders (AWRequest request, OutputStream output)
      throws IOException
    {
        Iterator headerKeys = request.headers().keySet().iterator();
        while (headerKeys.hasNext()) {
            // We assume that "toString"ing the key produces the right key.  We
            // have no way of knowing what datatype might be returned here, so
            // this is the best we can do
            String headerKey = headerKeys.next().toString();

            // Need to call request.headerForKey, instead of going directly to
            // the "headers()" hashtable, since we may not be getting back an
            // easily Stringified value from the hashtable.  However,
            // headerForKey is supposed to implement an accurate String
            // representation of the value.
            String headerValue = request.headerForKey(headerKey);
            String line = StringUtil.strcat(headerKey, ": ", headerValue, Newline);
            output.write(line.getBytes(I18NUtil.EncodingUTF8));
        }
    }

    /*-----------------------------------------------------------------
        methods for setting playback
    ------------------------------------------------------------------*/
    public static void setPlaybackMode (boolean flag)
    {
        _inPlaybackModeGlobal = flag;
    }

    public static void startPlayback (AWRequestContext requestContext,
                                      AWResponse response,
                                      String recordingPath)
    {
        AWRequest request = requestContext.request();
        if (isInPlaybackMode(requestContext)) {
            return;
        }

        // Note: the recording path is not sufficient to identify
        // session due to simultaneously playback
        String cookieValue =
            (!StringUtil.nullOrEmptyOrBlankString(recordingPath) ?
             recordingPath : "true");
        AWCookie cookie = new AWCookie(CookiePlaybackMode,
                cookieValue,
                null,
                requestPath(requestContext),
                request.isSecureScheme(), -1);
        response.addCookie(cookie);

        if (!StringUtil.nullOrEmptyOrBlankString(recordingPath)) {
            AWRecordingManager manager = new AWRecordingManager(recordingPath);
            _recordingInstances.put(recordingPath, manager);
        }
    }

    public static void stopPlayback (AWRequestContext requestContext,
                                     AWResponse response)
    {
        AWRequest request = requestContext.request();
        if (!isInPlaybackMode(requestContext)) {
            return;
        }
        // XXX the recording path is not sufficient to identify
        // session due to simultaneously playback
        String cookieValue = request.cookieValueForKey(CookiePlaybackMode);
        if (!StringUtil.nullOrEmptyOrBlankString(cookieValue) &&
            !cookieValue.equals("true")) {
            _recordingInstances.remove(cookieValue);
        }
        AWCookie cookie = new AWCookie(CookiePlaybackMode,
                "true",
                null,
                requestPath(requestContext),
                request.isSecureScheme(), 0);
        response.addCookie(cookie);
    }

    public static void registerRecordingMonitor (RecordingMonitor recordingMonitor)
    {
        _recordingMonitor = recordingMonitor;
    }

    public static boolean isInPlaybackMode (AWRequestContext requestContext)
    {
        if (requestContext == null) {
            return false;
        }

        AWRequest request = requestContext.request();
        if (request == null) {
            return false;
        }

        if (_inPlaybackModeGlobal) {
            return true;
        }

        // if there is a registered recording monitor, check its playback mode
        if (_recordingMonitor != null) {
            return _recordingMonitor.isInPlaybackMode(requestContext);
        }

        if (request.headerForKey(HeaderPlayBackMode) != null) {
            return true;
        }
        // Note: the recording path is not sufficient to identify
        // session due to simultaneously playback
        // if recording manager is created for server-side recording
        if (instance(request) != null) {
            return true;
        }
        String cookieValue = request.cookieValueForKey(CookiePlaybackMode);
        return cookieValue != null && cookieValue.equalsIgnoreCase("true");
    }

    public static void setPlayBackHeaders (AWRequestContext requestContext,
                                           AWRequest request,
                                           AWResponse response)
    {
        AWEncodedString responseId = requestContext._debugResponseIdAsIs();
        if (!((AWBaseResponse)response)._debugIsStreamingResponse()) {
            if (responseId != null) {
                response.setHeaderForKey(request.requestId(), "X-RequestId");
            }

            AWSession session = requestContext.session(false);
            if (session != null) {
                response.setHeaderForKey(session.httpSession().getId(), "X-SessionId");
            }

            AWEncodedString frameName = requestContext.frameName();
            if (frameName != null) {
                response.setHeaderForKey(frameName.string(), AWRecordingManager.HeaderFrameName);
            }
            Map semanticKeyToElementIdMapping = getSemanticKeyToElementIdMappingIfAny(response);
            if (semanticKeyToElementIdMapping != null) {
                response.setHeaderForKey(
                        Integer.toString(semanticKeyToElementIdMapping.size()),
                        AWRecordingManager.HeaderSemanticKeyCount);
                response.setHeaderForKey(
                        Integer.toString(semanticKeyTableBytes(response).length),
                                AWRecordingManager.HeaderSemanticKeySize);
            }
            response.setHeaderForKey(new Date().toConciseDateString(), HeaderResponseDate);
            response.setHeaderForKey(new Date().toString(), HeaderResponseDateTime);
        }
    }

    private final static String[] LeadingZeros = {
        "", "0", "00", "000", "0000"
    };

    private final static int MaxNumberOfDigitsForRequestIndex = 5;

    public static String  filenameForRequest (int requestIndex)
    {
        return sequentialFileName(requestIndex, RequestFileSuffix);
    }

    public static String  filenameForResponse (int requestIndex)
    {
        return filenameForResponse(requestIndex, FileTypeResponse);
    }

    public static String  filenameForResponse (int requestIndex, int fileType)
    {
        switch (fileType)
        {
            case FileTypeFullResponse:
                return sequentialFileName(requestIndex, FullResponseFileSuffix);
            default:
                return sequentialFileName(requestIndex, ResponseFileSuffix);
        }
    }

    public static String xmlFilenameForResponse (int requestIndex)
    {
        return sequentialFileName(requestIndex, XMLResponseFileSuffix);
    }

    public static String htmlFilenameForResponse (int requestIndex)
    {
        return sequentialFileName(requestIndex, HTMLResponseFileSuffix);
    }

    public static String htmlFilenameForFullResponse (int requestIndex)
    {
        return sequentialFileName(requestIndex, HTMLFullResponseFileSuffix);
    }


    private static String sequentialFileName (int index, String fileNameSuffix)
    {
        String indexString = Integer.toString(index);
        int paddingLength = MaxNumberOfDigitsForRequestIndex - indexString.length();
        Assert.that(paddingLength >= 0, "request index exceedes max request index");

        return StringUtil.strcat(LeadingZeros[paddingLength],
                           Integer.toString(index),
                           fileNameSuffix);
    }

    public static String registerSemanticKey (String elementId,
                                            String semanticKey,
                                            AWRequestContext requestContext)
    {
        return registerSemanticKey(elementId, semanticKey, requestContext, requestContext.response());
    }

    public static String registerSemanticKey (String elementId,
                                            String semanticKeyString,
                                            AWRequestContext requestContext,
                                            AWResponse response)
    {
        return registerSemanticKey(elementId, semanticKeyString,
                                   requestContext, response, null);
    }

    public static String applySemanticKeyPrefix(AWRequestContext requestContext, String semanticKeyString,
                                                 String sourceKey)
    {
        SemanticKey semanticKey = (sourceKey == null ?
                                   new SemanticKey(semanticKeyString) :
                                   new SemanticKey(sourceKey));
        String forIndex = requestContext._debugSemanticKeyPrefix();
        if (forIndex != null && semanticKey.prefix() == null) {
            semanticKey.setPrefix(StringUtil.strcat(forIndex, "::"));
        }
        return semanticKey.uniqueKey();
    }
    
    public static String registerSemanticKey (String elementId,
                                            String semanticKeyString,
                                            AWRequestContext requestContext,
                                            AWResponse response,
                                            SemanticKey sourceKey)
    {
        if (semanticKeyString == null) {
            return null;
        }
        Map elementIdToSemanticKeyTable = response.elementIdToSemanticKeyTable();
        Map semanticKeyToElementIdTable = response.semanticKeyToElementIdTable();

        SemanticKey semanticKey = (sourceKey == null ?
                                   new SemanticKey(semanticKeyString) :
                                   new SemanticKey(sourceKey));
        if (elementId != null) {
            SemanticKey existingSemanticKey = (SemanticKey)elementIdToSemanticKeyTable.get(elementId);
            if (existingSemanticKey != null) {
                // in AWForm, there will be two semantic keys for the same element id,
                // AWForm and awsf
                String existingSemanticKeyString = existingSemanticKey.uniqueKey();
                if (!existingSemanticKeyString.equals(semanticKeyString) && !semanticKeyString.equals("awsnf")) {
                    //System.out.println(Fmt.S("can't replace semantic key %s with %s for elementId %s",
                    //                         existingSemanticKey, semanticKeyString, elementId));
                    return semanticKeyString;
                }
            }
        }
        Object existingEntry = semanticKeyToElementIdTable.get(semanticKey);
        if (existingEntry != null) {
            if (sourceKey != null) {
                // If there is a source key, it is expected that the
                // source key is unique.
                Log.aribaweb.debug("%s%s%s", "Semantic Key ",
                                   semanticKey.uniqueKey(), " is not unique.");
            }
            SemanticKey counterKey = new SemanticKey(semanticKeyString);
            counterKey.setSuffix("_AWCounter");
            Integer counterEntry = (Integer)semanticKeyToElementIdTable.get(counterKey);
            if (counterEntry == null) {
                counterEntry = Constants.getInteger(2);
                if (semanticKey.suffix() == null) {
                    // safeguard against reseting the suffix.  This would only
                    // happen if we have duplicate key when merging semantic
                    // keys from one response to another.
                    semanticKey.setSuffix("_1");
                }
            }
            else {
                int currentIndex = counterEntry.intValue();
                counterEntry = Constants.getInteger(currentIndex + 1);
                if (semanticKey.suffix() == null) {
                    semanticKey.setSuffix("_" + Integer.toString(currentIndex));
                }
            }
            semanticKeyToElementIdTable.put(counterKey, counterEntry);
        }
        String uniqueSemanticKey = semanticKey.uniqueKey();
        if (uniqueSemanticKey.matches(".*[^\\x20-\\x7E].*")) {
            String componentPath = requestContext.getCurrentComponent().componentPath("\n").toString();
            Log.aribaweb.debug("%s contains non-ascii character\n%s\n", uniqueSemanticKey, componentPath);
        }
        if (elementId != null) {
            semanticKeyToElementIdTable.put(semanticKey, elementId);
            elementIdToSemanticKeyTable.put(elementId, semanticKey);
        }
        else {
            semanticKeyToElementIdTable.put(semanticKey, semanticKey);
        }
        return semanticKey.uniqueKey();
    }

    public static void mergeSemanticKeys (AWResponse fromResponse, AWResponse toResponse,
                                          AWRequestContext requestContext)
    {
        Map elementIdToSemanticKeyTable = fromResponse.elementIdToSemanticKeyTable();
        Iterator iter = elementIdToSemanticKeyTable.keySet().iterator();
        while (iter.hasNext()) {
            String elementId = (String)iter.next();
            SemanticKey semanticKey = (SemanticKey)elementIdToSemanticKeyTable.get(elementId);
            registerSemanticKey(elementId, semanticKey.key(), requestContext, toResponse, semanticKey);
        }
    }

    public static String actionEffectiveKeyPathInComponent (AWBinding actionBinding,
                                                            AWComponent component)
    {
        String semanticKey = null;
        if (actionBinding.isConstantValue()) {
            Object semanticKeyObject = actionBinding.value(component);
            // $null
            if (semanticKeyObject == null) {
                semanticKey ="refreshCurrentPage";
            }
            // $true / $false
            else if (semanticKeyObject instanceof Boolean) {
                semanticKey = semanticKeyObject.toString();
            }
            // do nothing for dyanamic constants
        }
        if (semanticKey == null) {
            semanticKey = actionBinding.effectiveKeyPathInComponent(component);
            if (semanticKey == null) {
                semanticKey ="refreshCurrentPage";
            }
        }
        return semanticKey;
    }

    // note that this API is used for Selenium tests only.
    // See NodeRedirectServlet.doRedirection() for the usage. 
    public static boolean isInRecordingOrPlaybackMode(HttpServletRequest request)
    {
        if (request == null) {
            return false;
        }

        // if there is a registered recording monitor, check its mode
        if (_recordingMonitor != null) {
            String mode = request.getParameter(AWRequestContext.RecordingModeKey);
            return _recordingMonitor.isInRecordingMode(mode) || _recordingMonitor.isInPlaybackMode(mode);
        }

        return false;
    }

    public static class RecordingMonitor
    {
        public boolean isInRecordingMode (AWRequestContext requestContext)
        {
            String mode = requestContext.request().formValueForKey(AWRequestContext.RecordingModeKey);
            return isInRecordingMode(mode);
        }

        public boolean isInPlaybackMode (AWRequestContext requestContext)
        {
            String mode = requestContext.request().formValueForKey(AWRequestContext.RecordingModeKey);
            return isInPlaybackMode(mode);
        }

        public boolean isInRecordingMode (String mode)
        {
            return CookieRecordingMode.equals(mode);
        }

        public boolean isInPlaybackMode (String mode)
        {
            return CookiePlaybackMode.equals(mode);
        }
    }
}

class SemanticKey
{
    private String _key;
    private String _prefix;
    private String _suffix;
    private String _uniqueKey;

    public SemanticKey (String key)
    {
        _key = key;
        _suffix = null;
        _prefix = null;
        _uniqueKey = _key;
    }

    public SemanticKey (SemanticKey sourceKey)
    {
        _key = sourceKey._key;
        _suffix = sourceKey._suffix;
        _prefix = sourceKey._prefix;
        updateUniqueKey();
    }
    public String key ()
    {
        return _key;
    }

    public String suffix ()
    {
        return _suffix;
    }

    public String prefix ()
    {
        return _prefix;
    }

    public void setSuffix (String suffix)
    {
        _suffix = suffix;
        updateUniqueKey();
    }

    public void setPrefix (String prefix)
    {
        _prefix = prefix;
        updateUniqueKey();
    }

    private void updateUniqueKey ()
    {
        _uniqueKey = StringUtil.strcat(_prefix, _key, _suffix);
    }

    public String uniqueKey ()
    {
        return _uniqueKey;
    }

    public boolean equals (Object key)
    {
        if (key instanceof SemanticKey) {
            SemanticKey sKey = (SemanticKey)key;
            return uniqueKey().equals(sKey.uniqueKey());
        }
        return false;
    }

    public int hashCode()
    {
        return uniqueKey().hashCode();
    }

}
