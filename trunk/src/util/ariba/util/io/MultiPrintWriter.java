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

    $Id: //ariba/platform/util/core/ariba/util/io/MultiPrintWriter.java#4 $
*/

package ariba.util.io;

import ariba.util.core.IOUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
    MultiPrintWriter

    Multiplex over multiple streams (NOTE: not over writers).

    @aribaapi private
*/
public class MultiPrintWriter extends PrintWriter
{
    private MultiOutputStream stream;

    public MultiPrintWriter (String encoding)
      throws UnsupportedEncodingException
    {
        this(new MultiOutputStream(), encoding);
    }

    public MultiPrintWriter (OutputStream outputStream, String encoding)
      throws UnsupportedEncodingException
    {
        this(new MultiOutputStream(), encoding);
        addStream(outputStream);
    }

    public MultiPrintWriter (OutputStream outputStream,
                             String encoding,
                             boolean buffered)
      throws UnsupportedEncodingException
    {
        this(new MultiOutputStream(), encoding);
        addStream(outputStream, buffered);
    }

    private MultiPrintWriter (MultiOutputStream stream, String encoding)
      throws UnsupportedEncodingException
    {
        super(IOUtil.bufferedWriter(stream, encoding), true);
        this.stream = stream;
    }

    public OutputStream stream ()
    {
        return stream;
    }

    public void addStream (OutputStream s)
    {
        stream.addStream(s, false);
    }

    public void addStream (OutputStream s, boolean buffered)
    {
        stream.addStream(s, buffered);
    }

    public void removeStream (OutputStream s)
    {
        stream.removeStream(s);
    }

    public int count ()
    {
        return stream.count();
    }
    
    public boolean isEmpty ()
    {
        return stream.isEmpty();
    }

    public void flushIfNotBuffered ()
    {
        try {
            stream.flushIfNotBuffered();
        }
        catch (IOException x) {
            setError();
        }
    }
}
