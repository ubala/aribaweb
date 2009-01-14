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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWStreamingServletResponse.java#2 $
*/
package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.core.AWCookie;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.servletadaptor.AWServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

public final class AWStreamingServletResponse extends AWServletResponse
{
    private HttpServletResponse _servletResponse;
    private OutputStream _outputStream;

    // ** Thread Safety Considerations: This is never shared -- no locking required

    public void init (AWCharacterEncoding characterEncoding, HttpServletResponse servletResponse)
    {
        _servletResponse = servletResponse;
        super.init(characterEncoding);
        try {
            _outputStream = servletResponse.getOutputStream();
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }

    public void init (AWCharacterEncoding characterEncoding)
    {
        throwUnsuported();
    }

    private void throwUnsuported ()
    {
        throw new AWGenericException("Unsupported");
    }

    protected List encodedStringBuffers ()
    {
        throwUnsuported();
        return null;
    }

    public void setPreviousResponse (AWResponse previousResponse)
    {
        throwUnsuported();
    }

    public int contentLength ()
    {
        throwUnsuported();
        return 0;
    }

    /////////////////////////
    // RefreshRegion support
    /////////////////////////
    public void startRefreshRegion (AWEncodedString refreshRegionName)
    {
        throwUnsuported();
    }

    public void stopRefreshRegion (AWEncodedString refreshRegionName)
    {
        throwUnsuported();
    }

    public boolean hasRefreshRegions ()
    {
        throwUnsuported();
        return false;
    }

    public void setRefreshRegionPollInterval (int pollInterval)
    {
        throwUnsuported();
    }

    /////////////////////
    // Appending Content
    /////////////////////
    protected int maximumStringLength ()
    {
        throwUnsuported();
        return 0;
    }

    private void appendContent (byte[] bytes)
    {
        try {
            // set _servletResponse to null to avoid anything being done with this after this point.
            _servletResponse = null;
            _outputStream.write(bytes, 0, bytes.length);
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }

    public OutputStream outputStream ()
    {
        // set _servletResponse to null to avoid anything being done with this after this point.
        _servletResponse = null;
        return _outputStream;
    }

    public void appendContent (AWEncodedString encodedString)
    {
        if (encodedString != null) {
            appendContent(encodedString.bytes(characterEncoding()));
        }
    }

    public void appendContent (String contentString)
    {
        if (contentString != null) {
            AWEncodedString encodedString = AWEncodedString.sharedEncodedString(contentString);
            appendContent(encodedString);
        }
    }

    public void appendContent (char contentChar)
    {
        try {
            _outputStream.write(contentChar);
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }

    public void writeContent (OutputStream outputStream)
    {
        throwUnsuported();
    }

    public String contentString ()
    {
        throwUnsuported();
        return null;
    }

    public byte[] content ()
    {
        throwUnsuported();
        return null;
    }

    // to overwrite the superclass behavior, which is not applied to the streaming response,
    // because the superclass will flush the buffer, which is no needed in the streaming response,
    protected void writeToServletResponse (HttpServletResponse servletResponse, boolean useGzip)
    {
        writeToServletResponse(servletResponse);
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
        throwUnsuported();
        return null;
    }

    // Cookie Support
    public void addCookie (AWCookie cookie)
    {
        throwUnsuported();
    }

    //////////////////////////
    // Abstract in superclass
    //////////////////////////
    public void writeToServletResponse (HttpServletResponse servletResponse)
    {
        // do nothing but close the stream -- everything already written.
        try {
            _outputStream.close();
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
        finally {
            responseCompleted();
        }
    }

    public void setContentFromFile (String filePath)
    {
        throwUnsuported();
    }

    public void setHeaderForKey (String headerValue, String headerKey)
    {
        _servletResponse.setHeader(headerKey, headerValue);
    }

    public void setHeadersForKey (String[] headerValues, String headerKey)
    {
        throwUnsuported();
    }

    public void setStatus (int status)
    {
        _servletResponse.setStatus(status);
    }

    public AWCookie createCookie (String cookieName, String cookieValue)
    {
        throwUnsuported();
        return null;
    }

    public void disableClientCaching ()
    {
        throwUnsuported();
    }

    public void setContentType (AWContentType contentType)
    {
        _servletResponse.setContentType(contentType.header(characterEncoding()));
    }

    public boolean _debugIsStreamingResponse ()
    {
        return true;
    }
}
