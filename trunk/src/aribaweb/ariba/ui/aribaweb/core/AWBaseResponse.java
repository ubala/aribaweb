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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBaseResponse.java#42 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWByteArray;
import ariba.ui.aribaweb.util.AWByteArrayOutputStream;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWPagedVector;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.PerformanceStateTimedCounter;
import ariba.util.core.PerformanceState;
import ariba.util.core.PerformanceStateCore;
import ariba.util.core.PerformanceStateCounter;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

///////////////////
// AWBaseResponse
///////////////////
abstract public class AWBaseResponse extends AWBaseObject implements AWResponse
{
    private static final AWEncodedString RootBufferName = new AWEncodedString("awrootbuffer");
    private static final AWEncodedString BodyTagOpen = new AWEncodedString("<html><body onLoad='ariba.Refresh.completeRefreshOnLoad()'>");
    private static final AWEncodedString BodyTagClose = new AWEncodedString("</body></html>");

    public static PerformanceStateCounter ResponseSizeCounter =
            new PerformanceStateTimedCounter("Response Size", 200,
                    PerformanceStateCore.LOG_COUNT);

    public static PerformanceStateCounter PageGenerationSizeCounter =
            new PerformanceStateTimedCounter("Page Generation", 210,
                    PerformanceStateCore.LOG_COUNT);

    // could be an interface but leaving ourselves room do to more here?
    public static interface AWResponseCompleteCallback
    {
        public void responseCompleted ();
    }

    boolean _browserCachingEnabled;
    private AWResponseBuffer _rootBuffer;
    private AWResponseBuffer _noChangeBuffer;
    private AWResponseBuffer _currentBuffer;
    private List _bufferStack;
    private AWContentType _contentType;
    private AWCharacterEncoding _characterEncoding;
    private AWBaseResponse _previousResponse;
    private AWPagedVector _globalContents;
    private HashMap _scopeChildren;
    protected List _cookies;

    private boolean _deferred = false;

    private boolean _isContentGeneration = false;
    private AWResponseCompleteCallback _responseCompleteCallback;
    protected int _fullSize = 0;
    protected int _bytesWritten = 0;
    protected boolean _writeRefreshRegionBoundaryMarkers = false;

    // ** Thread Safety Considerations: This is never shared -- no locking required

    public void init (AWCharacterEncoding characterEncoding)
    {
        super.init();
        setCharacterEncoding(characterEncoding);
        setContentType(AWContentType.TextHtml);
    }

    private List bufferStack ()
    {
        if (_bufferStack == null) {
            _bufferStack = ListUtil.list(8);
            pushBuffer(RootBufferName, false, false, false);
            _rootBuffer = (AWResponseBuffer)_bufferStack.get(0);

            // Ignore whitespace changes in root buffer for purposes of checksum.
            // (Done to ignore whitespace around <BasicPageWrapper> tags in page temaplates)
            _rootBuffer.setIgnoreWhitespaceDiffs(true);

            // Fake buffer to accumulate CRC for noChange (force-FPR on change) buffers
            _noChangeBuffer = new AWResponseBuffer(AWEncodedString.sharedEncodedString("noChange"),
                    AWResponseBuffer.Type.Normal, false, this);
        }
        return _bufferStack;
    }

    private AWResponseBuffer currentBuffer ()
    {
        if (_currentBuffer == null) {
            bufferStack();
        }
        return _currentBuffer;
    }

    public boolean currentRegionIsScope ()
    {
        AWResponseBuffer buffer = currentBuffer();
        return (buffer != null && buffer.isScope());
    }

    protected HashMap scopeChildren ()
    {
        if (_scopeChildren == null) {
            _scopeChildren = new HashMap(16);
        }
        return _scopeChildren;
    }

    protected AWPagedVector globalContents ()
    {
        if (_globalContents == null) {
            _globalContents = new AWPagedVector(1024);
        }
        return _globalContents;
    }

    protected void pushBuffer (AWEncodedString name, boolean isScope, boolean isScopedChild, boolean alwaysRender)
    {
        AWResponseBuffer.Type type = (isScope ? AWResponseBuffer.Type.Scope
                : (isScopedChild ? AWResponseBuffer.Type.ScopeChild : AWResponseBuffer.Type.Normal));
        AWResponseBuffer responseBuffer = new AWResponseBuffer(name, type, alwaysRender, this);
        if (_currentBuffer != null) {
            _currentBuffer.append(responseBuffer);
        }
        _currentBuffer = responseBuffer;
        bufferStack().add(_currentBuffer);
    }

    protected void popBuffer (boolean isNoChangeBuffer)
    {
        ListUtil.removeLastElement(bufferStack());
        _currentBuffer.close();
        if (isNoChangeBuffer) {
            _currentBuffer.updateParentChecksum(_noChangeBuffer);
        }
        _currentBuffer = (AWResponseBuffer)ListUtil.lastElement(bufferStack());
    }

    protected AWResponseBuffer rootBuffer ()
    {
        return _rootBuffer;
    }

    public boolean hasRootBufferChanged (AWBaseResponse previousReponse)
    {
        return !rootBuffer().isEqual(previousReponse.rootBuffer());
    }

    public boolean hasNoChangeBufferChanged (AWBaseResponse previousReponse)
    {
        return !_noChangeBuffer.isEqual(previousReponse._noChangeBuffer);
    }

    void setWriteRefreshRegionBoundaryMarkers (boolean tf)
    {
        _writeRefreshRegionBoundaryMarkers = tf;
    }
    
    public void init ()
    {
        this.init(AWCharacterEncoding.Default);
    }

    public void setContentType (AWContentType contentType)
    {
        _contentType =
            contentType == AWContentType.ApplicationXZipCompressed || 
            contentType == AWContentType.ApplicationZip ?
                AWContentType.ApplicationDownload : contentType;

    }

    public AWContentType contentType ()
    {
        return _contentType;
    }

    public void setContentFromStream (InputStream stream)
    {
        Assert.that(false, "NOTE: This response class must implement setContentFromStream().  See AWServletResponse for an example.");
    }

    // Perhaps should be protected, but some apps has a dependency on it being be public.
    public int contentLength ()
    {
        Assert.that(false, "Unsupported -- we don't know what the content length will be until we do the diff.");
        return -1;
    }

    public void setCharacterEncoding (AWCharacterEncoding characterEncoding)
    {
        if (_characterEncoding != characterEncoding) {
            _characterEncoding = characterEncoding;
        }
    }

    public AWCharacterEncoding characterEncoding ()
    {
        return _characterEncoding;
    }

    /////////////////////
    // Appending Content
    /////////////////////
    public void appendContent (AWEncodedString encodedString)
    {
        currentBuffer().append(encodedString);
    }

    public void appendContent (String contentString)
    {
        if (contentString != null) {
            AWEncodedString encodedString = AWEncodedString.sharedEncodedString(contentString);
            appendContent(encodedString);
        }
    }

    public void appendContent (AWBaseResponse response)
    {
        AWResponseBuffer root = response.rootBuffer();
        if (root != null) {
            root.close();
            recurseCopy(root, false);
        }
    }

    private void recurseCopy (AWResponseBuffer buff, boolean pushLevel) {
        if (pushLevel) {
            pushBuffer(buff.getName(), buff.isScope(), buff.isScopeChild(), buff.isAlwaysRender());
        }
        int start = buff.getContentStartIndex();
        int end = buff.getContentEndIndex();
        AWPagedVector.AWPagedVectorIterator elements = buff.getGlobalContents().elements(start,end);
        try {
            while (elements.hasNext()) {
                Object element = elements.next();
                if (element instanceof AWEncodedString) {
                    appendContent(((AWEncodedString)element));
                }
                else {
                    AWResponseBuffer childBuffer = (AWResponseBuffer)element;
                    recurseCopy(childBuffer, true);
                    elements.skipTo(childBuffer.getContentEndIndex());
                }
            }
        }
        finally {
            elements.release();
        }
        if (pushLevel) {
            popBuffer(false);// no support for forceRefresh
        }
    }

    public void appendContent (char contentChar)
    {
        // Perf: seems inefficient. Is there more direct way to get a byte[] from char?
        appendContent(String.valueOf(contentChar));
    }

    protected void _writeContent (OutputStream outputStream)
    {
        Assert.that(bufferStack().size() == 1, "Unbalanced stack: didn't pop proper number of buffers.");
        AWResponseBuffer rootBuffer = rootBuffer();
        rootBuffer.close();

//        System.out.println("------------------------------- BEGIN writeContent -----------------");
//        rootBuffer.printStructure(System.out, 0);
//        System.out.println("------------------------------- END writeContent -----------------");

        AWResponseBuffer.WriteContext writeContext
                = new AWResponseBuffer.WriteContext(outputStream, _characterEncoding, 
                                                    _writeRefreshRegionBoundaryMarkers);
        if (_previousResponse == null) {
            rootBuffer.writeTo(writeContext, null);
        }
        else {
            try {
                byte[] bytes = BodyTagOpen.bytes(_characterEncoding);
                outputStream.write(bytes, 0, bytes.length);
                _bytesWritten += bytes.length;

                AWResponseBuffer previousRootBuffer = _previousResponse.rootBuffer();
                rootBuffer.writeTo(writeContext, previousRootBuffer);

                bytes = BodyTagClose.bytes(_characterEncoding);
                outputStream.write(bytes, 0, bytes.length);
                _bytesWritten += bytes.length;
            }
            catch (IOException ioexception) {
                throw new AWGenericException(ioexception);
            }
        }
        _bytesWritten += writeContext._bytesWritten;

        _previousResponse = null;
        if (!AWPage.DEBUG_REFRESH_REGION_TOP_LEVEL_CHANGE) {
            if (_globalContents != null) {
                _globalContents.clear();
                _globalContents = null;
            }
        }
        _currentBuffer = null;
        _cookies = null;
        _bufferStack = null;
    }

    public void flushSizeStat ()
    {
        if (PerformanceState.threadStateEnabled()) {
            ResponseSizeCounter.addCount(_bytesWritten);
            PageGenerationSizeCounter.addCount(_fullSize);
        }
        _bytesWritten = 0;
        _fullSize = 0;
    }

    public void writeContent (OutputStream outputStream)
    {
        _writeContent(outputStream);
    }

    public void writeFullContent (OutputStream outputStream)
    {
        if (rootBuffer() != null) {
            rootBuffer().close();
            AWResponseBuffer.WriteContext writeContext
                    = new AWResponseBuffer.WriteContext(outputStream, _characterEncoding);
            rootBuffer().writeTo(writeContext, null);
        }
    }

    public String contentString ()
    {
        String contentString = null;
        AWByteArray contentByteArray = contentByteArray();
        byte[] contentBytes = contentByteArray.array();
        try {
            contentString = new String(contentBytes, 0, contentByteArray.inUse, _characterEncoding.name);
        }
        catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new AWGenericException(unsupportedEncodingException);
        }

        Log.aribaweb_html.debug(contentString);

        return contentString;
    }

    protected AWByteArray contentByteArray ()
    {
        // iterates through all encodedStringBuffers appending to newly allocated byte buffer of appropriate size
        AWByteArrayOutputStream byteArrayOutputStream = new AWByteArrayOutputStream(8 * 1024);
        _writeContent(byteArrayOutputStream);
        return byteArrayOutputStream.byteArray();
    }

    public byte[] content ()
    {
        return contentByteArray().toByteArray();
    }

//    private int computeContentLength (AWCharacterEncoding characterEncoding)
//    {
//        characterEncoding = null;
//        //AWUtil.notImplemented("AWBaseResponse.computeContentLength(...)");
//        return -1;
//    }

    protected void setResponseCompleteCallback (AWResponseCompleteCallback callback)
    {
        _responseCompleteCallback = callback;
    }

    protected void responseCompleted ()
    {
        if (_responseCompleteCallback != null) {
            _responseCompleteCallback.responseCompleted();
        }
    }
    ////////////////////////
    // AWResponseGenerating
    ////////////////////////
    public AWResponse generateResponse ()
    {
        return this;
    }

    public String generateStringContents ()
    {
        return contentString();
    }

    public void setBrowserCachingEnabled (boolean flag)
    {
        _browserCachingEnabled = flag;
    }

    public boolean browserCachingEnabled ()
    {
        return _browserCachingEnabled;
    }

    // Cookie Support
    public void addCookie (AWCookie cookie)
    {
        if (cookie != null) {
            if (_cookies == null) {
                _cookies = ListUtil.list();
            }
            _cookies.add(cookie);
        }
    }

    /**
       Set to indicate a response that does not need to be cached in the
       requestHistory.  This is useful for responses that are primarily download
       type responses.
    */
    public boolean isContentGeneration ()
    {
        return _isContentGeneration;
    }

    public void setIsContentGeneration (boolean b)
    {
        _isContentGeneration = b;
    }

    public boolean _debugIsStreamingResponse ()
    {
        return false;
    }

    protected void setPreviousResponse (AWBaseResponse response)
    {
        _previousResponse = response;
    }

    public boolean isDeferred ()
    {
        return _deferred;
    }

    public void setDeferred (boolean flag)
    {
        _deferred = flag;
    }

    public boolean hasIncrementalChange ()
    {
        return (!isDeferred() && !isContentGeneration() &&
                _previousResponse != null);
    }
    
    // record playback
    private Map _elementIdToSemanticKeyTable = null;
    private Map _semanticKeyToElementIdTable = null;

    public Map elementIdToSemanticKeyTable ()
    {
        if (_elementIdToSemanticKeyTable == null) {
            _elementIdToSemanticKeyTable = MapUtil.map();
        }
        return _elementIdToSemanticKeyTable;
    }

    public Map semanticKeyToElementIdTable ()
    {
        if (_semanticKeyToElementIdTable == null) {
            _semanticKeyToElementIdTable = MapUtil.map();
        }
        return _semanticKeyToElementIdTable;
    }

    public Map _debugHeaders ()
    {
        return null;
    }

    public void _debugSetRecordPlaybackParameters (AWRecordingManager recordingMgr,
                                                   boolean appendSemanticKeys)
    {
        assertSemanticKeysNotSupported();
    }

    public void _debugSetSemanticKeyMapping (byte[] bytes)
    {
        assertSemanticKeysNotSupported();
    }

    public byte[] _debugGetSemanticKeyMapping ()
    {
        assertSemanticKeysNotSupported();
        return null;
    }

    private void assertSemanticKeysNotSupported ()
    {
        Assert.that(false, "semantic key mapping not implemented in base response. subclass must override");
    }
}
