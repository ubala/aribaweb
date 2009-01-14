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

    $Id: //ariba/platform/util/core/ariba/util/io/UnicodeEscapeInputStreamReader.java#3 $
*/

package ariba.util.io;

import ariba.util.core.Assert;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import sun.io.ByteToCharConverter;
import sun.io.ConversionBufferFullException;


/**
    UnicodeEscapeInputStreamReader is a special inputStreamWriter to
    read from the underlying input stream and convert the data into
    UnicodeEscaped (Ariba's internal encoding scheme) format. This is
    used internally by our stubgenerator, should not be used
    elsewhere.

    @aribaapi ariba
*/
public class UnicodeEscapeInputStreamReader extends InputStreamReader {

    private ByteToCharConverter btc = new ByteToCharUnicodeEscape();
    private InputStream in;

    private static final int defaultByteBufferSize = 8192;
    /* Input buffer */
    private byte bb[];

    /**
        Create an InputStreamReader that uses internal Ariba encoding.
        @param in An InputStream, must not be null.
    */
    public UnicodeEscapeInputStreamReader (InputStream in)
    {
        super(in);
        this.in = in;
        this.bb = new byte[defaultByteBufferSize];
    }

    /**
        Constructor: we don't support this constructor. It will assert.
        @param  in   An InputStream
        @param  enc  The encoding
        @exception  UnsupportedEncodingException
        If the named encoding is not supported
    */
    public UnicodeEscapeInputStreamReader (InputStream in, String enc)
      throws UnsupportedEncodingException
    {
        super(in, enc);
        Assert.that(false, "encoding %s is not supported", enc);
    }

    /**
        Returns the canonical name of the character encoding being
        used by this stream. Note that for this class, the Ariba
        internal UnicodeEscape encoding is used.
     
        @return a String representing the encoding name.
    */
    public String getEncoding ()
    {
        synchronized (lock) {
            return btc.getCharacterEncoding();
        }
    }

    /* -1 implies EOF has been reached */
    private int nBytes = 0;

    private int nextByte = 0;

    /**
        Converts an array of bytes containing characters in an
        external encoding into an array of Unicode characters using
        Ariba's internal encoding.

        @param cbuf character array to receive conversion result. 
        @param off  start writing to output array at this offset. 
        @param end  stop writing to output array at this offset (exclusive). 
        @return the number of bytes written to the output array. 
        @exception IOException when IO errors occur.
    */
    private int convertInto (char cbuf[], int off, int end) throws IOException
    {
        int nc = 0;
        if (nextByte < nBytes) {
            try {
                nc = btc.convert(bb, nextByte, nBytes,
                                 cbuf, off, end);
                nextByte = nBytes;
                Assert.that(btc.nextByteIndex() == nextByte,
                            "Converter malfunction for Ariba internal encoding");
            }
            catch (ConversionBufferFullException x) {
                nextByte = btc.nextByteIndex();
                nc = btc.nextCharIndex() - off;
            }
        }
        return nc;
    }

    /**
        Flushes remaining bytes into given buffer
    */
    private int flushInto (char cbuf[], int off, int end) throws IOException
    {
        int nc = 0;
        try {
            nc = btc.flush(cbuf, off, end);
        }
        catch (ConversionBufferFullException x) {
            nc = btc.nextCharIndex() - off;
        }
        return nc;
    }

    private int fill (char cbuf[], int off, int end) throws IOException
    {
        int nc = 0;

        if (nextByte < nBytes) {
            nc = convertInto(cbuf, off, end);
        }

        while (off + nc < end) {

            if (nBytes != -1) {
                if ((nc > 0) && !inReady()) {
                    /* Block at most once */
                    break;
                }
                nBytes = in.read(bb);
            }

            if (nBytes == -1) {
                nBytes = 0; /* Allow file to grow */
                nc += flushInto(cbuf, off + nc, end);
                if (nc == 0) {
                    return -1;
                }
                else {
                    break;
                }
            }
            else {
                nextByte = 0;
                nc += convertInto(cbuf, off + nc, end);
            }
        }
        return nc;
    }

    /**
        Tell whether the underlying byte stream is ready to be read.  Return
        false for those streams that do not support available(), such as the
        Win32 console stream.
    */
    private boolean inReady ()
    {
        try {
            return in.available() > 0;
        }
        catch (IOException x) {
            return false;
        }
    }

    /** Check to make sure that the stream has not been closed */
    private void ensureOpen () throws IOException
    {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }

    /**
        Read characters into a portion of an array.
     
        @param      cbuf  Destination buffer
        @param      off   Offset at which to start storing characters
        @param      len   Maximum number of characters to read
        
        @return     The number of characters read, or -1 if the end of the stream
        has been reached
        
        @exception  IOException  If an I/O error occurs
    */
    public int read (char cbuf[], int off, int len) throws IOException
    {
        synchronized (lock) {
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            else if (len == 0) {
                return 0;
            }
            return fill(cbuf, off, off + len);
        }
    }

    /**
        Tell whether this stream is ready to be read.  An InputStreamReader is
        ready if its input buffer is not empty, or if bytes are available to be
        read from the underlying byte stream.

        @exception  IOException  If an I/O error occurs
    */
    public boolean ready () throws IOException
    {
        synchronized (lock) {
            ensureOpen();
            return (nextByte < nBytes) || inReady();
        }
    }

    /**
        Close the stream.

        @exception  IOException  If an I/O error occurs
    */
    public void close () throws IOException
    {
        synchronized (lock) {
            super.close();
            in = null;
            bb = null;
        }
    }
}
