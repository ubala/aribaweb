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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWRecordingServletResponse.java#2 $
*/
package ariba.ui.servletadaptor;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.i18n.I18NUtil;
import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.core.AWRecordingManager;
import ariba.ui.aribaweb.core.AWBaseResponse;

public final class AWRecordingServletResponse extends HttpServletResponseWrapper
{
    private RecordingOutputStream _servletOutputStreamWrapper;
    private OutputStream _recordingOutputStream;
    private OutputStream _recordingFullResponseOutputStream;

    public static final String Newline = AWConstants.Newline.string();
    private AWRecordingManager _manager;

    public AWRecordingServletResponse (HttpServletResponse response,
                                       AWServletResponse   servletResponse,
                                       AWRecordingManager manager)
    {
        super(response);
        _manager = manager;
        try {
            _recordingOutputStream = _manager.recordingOutputStream();

            if (servletResponse.hasIncrementalChange()) {
                _recordingFullResponseOutputStream =
                    _manager.recordingFullResponseOutputStream();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
            throw new WrapperRuntimeException(ex);
        }
    }

    public ServletOutputStream getOutputStream () throws IOException
    {
        if (_servletOutputStreamWrapper == null) {
            _servletOutputStreamWrapper = new RecordingOutputStream(super.getOutputStream(),
                    _recordingOutputStream);
            _recordingOutputStream.write(Newline.getBytes(I18NUtil.EncodingUTF8));
        }
        return _servletOutputStreamWrapper;
    }

    /**
     * This method returns an output stream to record the full HTML response.
     * This method must be called AFTER the header fields are already written.
     * @return the full response output stream.  Return null if server-side
     * recording is not enabled.
     * @throws IOException
     * @aribaapi private
     */
    public OutputStream getFullResponseOutputStream () throws IOException
    {
        if (_recordingFullResponseOutputStream != null) {
            _recordingFullResponseOutputStream.write(
                    Newline.getBytes(I18NUtil.EncodingUTF8));
        }
        return _recordingFullResponseOutputStream;
    }

    public void addHeader (String key, String value)
    {
        writeSingleHeader(key, value);
        super.addHeader(key, value);
    }

    public void setHeader (String key, String value)
    {
        writeSingleHeader(key, value);
        super.setHeader(key, value);
    }

    public void setContentLength (int i)
    {
        writeSingleHeader("Content-Length", Integer.toString(i));
        super.setContentLength(i);
    }

    public void setContentType(String contentType)
    {
        writeSingleHeader("Content-Type", contentType);
        super.setContentType(contentType);
    }

    public void setStatus(int i)
    {
        writeSingleHeader("Status", Integer.toString(i));
        super.setStatus(i);
    }

    private void writeSingleHeader (String key, String value)
    {
        try {
            String line = StringUtil.strcat(key, ": ", value, Newline);
            _recordingOutputStream.write(line.getBytes());

            if (_recordingFullResponseOutputStream != null) {
                _recordingFullResponseOutputStream.write(line.getBytes());
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
            throw new WrapperRuntimeException(ex);
        }
    }

    public PrintWriter getWriter () throws IOException
    {
        Assert.that(false, "should not be invoked");
        return null;
    }

    public static class RecordingOutputStream extends ServletOutputStream
    {
        private ServletOutputStream _original;
        private OutputStream _recording;

        public RecordingOutputStream (ServletOutputStream original,
                                     OutputStream recording)
        {
            _original = original;
            _recording = recording;
        }

        public void write (int b) throws IOException
        {
            _original.write(b);
            _recording.write(b);
        }

        public void close() throws IOException
        {
            _original.close();
            _recording.close();
        }

        public void flush() throws IOException
        {
            _original.flush();
            _recording.flush();
        }
    }
}
