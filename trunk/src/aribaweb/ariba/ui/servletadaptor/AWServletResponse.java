/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWServletResponse.java#10 $
*/

package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.core.AWBaseResponse;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.core.AWCookie;
import ariba.ui.aribaweb.core.AWRecordingManager;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import java.util.Map;
import ariba.util.core.StringUtil;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;

/**
    An @{link ariba.ui.aribaweb.core.AWResponse} that ultimately writes its data to an
    HttpServletResponse.
 */
public class AWServletResponse extends AWBaseResponse
{
    private Map _headers = MapUtil.map();
    private int _status = -1;
    private String _filePath;
    private InputStream _inputStream;
    private byte[] _bytes;

    // record & playback
    private AWRecordingManager _recordingManager;
    private boolean _shouldAppendSemanticKey;
    private byte[] _semanticKeyMappingBytes;
    private boolean _inRecordPlayback = false;

    // ** Thread Safety Considerations: A servlet response is never accessed by more than one thread -- no sync required.

    public void init ()
    {
        super.init();
        setBrowserCachingEnabled(false);
    }

    public void setHeaderForKey (String headerValue, String headerKey)
    {
        _headers.put(headerKey, headerValue);
    }

    public void setHeadersForKey (String[] headerValues, String headerKey)
    {
        throw new AWGenericException(
            StringUtil.strcat(getClass().getName(),
                              ": setHeadersForKey() not yet supported."));
    }

    public void setStatus (int status)
    {
        _status = status;
    }

    /**
     * This will return a HTTP code, or -1 for an uninitialized value.
     * @return
     */
    public int getStatus ()
    {
        return _status;
    }

    //////////////////
    // Write Response
    //////////////////

    private void writeContentLength (HttpServletResponse servletResponse)
    {
        if (_bytes != null) {
            servletResponse.setContentLength(_bytes.length);
        }
        else if (_filePath != null) {
            File file = new File(_filePath);
            long fileLength = file.length();
            servletResponse.setContentLength((int)fileLength);
        }
        else if (_inputStream != null) {
            // can't do it
        }
    }

    private void writeStatus (HttpServletResponse servletResponse)
    {
        if (_status != -1) {
            servletResponse.setStatus(_status);
        }
    }

    private void writeHeaders (HttpServletResponse servletResponse)
    {
        if (!_headers.isEmpty()) {
            Iterator headerKeyIterator = _headers.keySet().iterator();
            while (headerKeyIterator.hasNext()) {
                String currentHeaderKey = (String)headerKeyIterator.next();
                String currentHeaderValue = (String)_headers.get(currentHeaderKey);
                servletResponse.setHeader(currentHeaderKey, currentHeaderValue);
            }
        }
        if (!browserCachingEnabled()) {
            // Need all cache-control items for security reasons
            servletResponse.setHeader("cache-control", 
                "no-store, no-cache, must-revalidate, private");
            servletResponse.setHeader("pragma", "no-cache");
            servletResponse.setHeader("expires", "-1");
        }
        if (_cookies != null) {
            for (int i=0,length=_cookies.size(); i<length; i++) {
                AWCookie cookie = (AWCookie)_cookies.get(i);
                // servletResponse.addCookie(cookie.getCookie());
                servletResponse.addHeader("Set-Cookie",cookie.headerString());
            }
        }
    }

    private void _privateWriteContent (OutputStream outputStream) throws IOException
    {
        if (_bytes != null) {
            outputStream.write(_bytes);
            _bytesWritten += _bytes.length;
        }
        else if (_filePath != null) {
            try {
                File file = new File(_filePath);
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                _bytesWritten += AWUtil.streamCopy(bufferedInputStream, outputStream);
                bufferedInputStream.close();
            }
            catch (FileNotFoundException fileNotFoundException) {
                throw new AWGenericException(fileNotFoundException);
            }
            catch (IOException ioexception) {
                throw new AWGenericException(ioexception);
            }
        }
        else if (_inputStream != null) {
            _bytesWritten += AWUtil.streamCopy(_inputStream, outputStream);
            _inputStream.close();
        }
        else {
            writeContent(outputStream);
        }
        flushSizeStat();
    }

    private void writeContent (HttpServletResponse servletResponse)
    {
        try {
            if (_inRecordPlayback) {
                if (hasIncrementalChange()) {
                    // If it is incremental update, write the full content
                    // during recording.   Since the content is different
                    // than what is being written to the servlet output
                    // stream, this method controls what writes to the
                    // recording manager.
                    if (servletResponse instanceof AWRecordingServletResponse) {
                        OutputStream output =
                            ((AWRecordingServletResponse)servletResponse).
                                getFullResponseOutputStream();
                        writeFullContent(output);
                        output.flush();
                        output.close();
                    }
                }
            }
            OutputStream servletResponseOutputStream = servletResponse.getOutputStream();
            _privateWriteContent(servletResponseOutputStream);
            if (_shouldAppendSemanticKey) {
                AWRecordingManager.appendSemanticKeyTable(this, servletResponseOutputStream);
            }
            servletResponseOutputStream.close();
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
        finally {
            responseCompleted();
        }
    }

    protected void writeToServletResponse (HttpServletResponse servletResponse)
    {
        if (_inRecordPlayback) {
            servletResponse = new AWRecordingServletResponse(
                                      servletResponse,
                                      this,
                                      _recordingManager);
        }
        String contentTypeHeader = contentType().header(characterEncoding());
        servletResponse.setContentType(contentTypeHeader);
        writeContentLength(servletResponse);
        writeStatus(servletResponse);
        writeHeaders(servletResponse);
        writeContent(servletResponse);
    }

    public void setContentFromFile (String filePath)
    {
        _filePath = filePath;
    }

    public void setContentFromStream (InputStream stream)
    {
        _inputStream = stream;
    }

    public void setContent (byte[] bytes)
    {
        _bytes = bytes;
    }

    public byte[] content ()
    {
        if (_bytes != null) {
            return _bytes;
        }
        if (_filePath != null) {
            try {
                File file = new File(_filePath);
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                AWUtil.streamCopy(bufferedInputStream, outputStream);
                bufferedInputStream.close();
                return outputStream.toByteArray();
            }
            catch (IOException ex) {
                throw new AWGenericException(ex);
            }
        }
        return super.content();
    }

    public int contentLength ()
    {

        if (_bytes != null) {
            return _bytes.length;
        }
        if (_filePath != null) {
            File file = new File(_filePath);
            long fileLength = file.length();
            return (int)fileLength;
        }
        return super.contentLength();
    }

    public void disableClientCaching ()
    {
        setBrowserCachingEnabled(false);
    }

    ///////////////////
    // Cookie Suupport
    ///////////////////
    public AWCookie createCookie (String cookieName, String cookieValue)
    {
        throw new AWGenericException(getClass().getName() + ": createCookie() not yet supported.");
    }

    // record & playback
    public Map _debugHeaders()
    {
        return _headers;
    }


    public void _debugSetRecordPlaybackParameters (AWRecordingManager recordingMgr,
                                                   boolean inPlayback)
    {
        _recordingManager = recordingMgr;
        _shouldAppendSemanticKey = inPlayback;
        if (_recordingManager != null) {
            _inRecordPlayback = true;
        }
    }

    public void _debugSetSemanticKeyMapping (byte[] bytes)
    {
        _semanticKeyMappingBytes = bytes;
    }

    public byte[] _debugGetSemanticKeyMapping ()
    {
        return _semanticKeyMappingBytes;
    }
}
