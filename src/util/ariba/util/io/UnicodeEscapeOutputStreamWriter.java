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

    $Id: //ariba/platform/util/core/ariba/util/io/UnicodeEscapeOutputStreamWriter.java#3 $
*/

package ariba.util.io;

import ariba.util.core.Assert;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import sun.io.CharToByteConverter;
import sun.io.ConversionBufferFullException;

/**
    UnicodeEscapeOutputStreamWriter is a special outputStreamWriter to write
    UnicodeEscaped (Ariba's internal encoding scheme) data to the underlying
    output stream. This is used internally by our JavaFile class, with the
    underlying output stream being a FileOutputStream.

    @aribaapi ariba
*/
public class UnicodeEscapeOutputStreamWriter extends OutputStreamWriter
{
    private CharToByteConverter ctb = new CharToByteUnicodeEscape();
    private OutputStream out;

    private static final int defaultByteBufferSize = 8192;

    /* bb is a temporary output buffer into which bytes are written. */
    private byte bb[];

    /* nextByte is where the next byte will be written into bb */
    private int nextByte = 0;

    /* nBytes is the buffer size = defaultByteBufferSize in this class */
    private int nBytes = 0;

    /**
        Create an OutputStreamWriter that uses the named character encoding.
        We don't support this constructor because this class only uses the
        Ariba internal encoding. We will assert.
     
        @param  out  An OutputStream
        @param  enc  The encoding
        @exception  UnsupportedEncodingException
        If the named encoding is not supported
    */
    public UnicodeEscapeOutputStreamWriter (OutputStream out, String enc)
      throws UnsupportedEncodingException
    {
        super(out, enc);
        Assert.that(false, "encoding %s is not supported", enc);
    }

    /**
        Create an OutputStreamWriter that uses Ariba internal encoding scheme.
        @param  out  An OutputStream, must not be null.
    */
    public UnicodeEscapeOutputStreamWriter (OutputStream out)
    {
        super(out);
        this.out = out;
        this.bb = new byte[defaultByteBufferSize];
        nBytes = defaultByteBufferSize;
    }

    /**
        Returns the canonical name of the character encoding being used by this
        stream. 
        @return a String representing the encoding name
    */
    public String getEncoding ()
    {
        synchronized (lock) {
            return ctb.getCharacterEncoding();
        }
    }

    /** Check to make sure that the stream has not been closed */
    private void ensureOpen () throws IOException
    {
        if (out == null) {
            throw new IOException("Stream closed");
        }
    }

    /**
        Write a portion of an array of characters.
     
        @param  cbuf  Buffer of characters
        @param  off   Offset from which to start writing characters
        @param  len   Number of characters to write
     
        @exception  IOException  If an I/O error occurs
    */
    public void write (char cbuf[], int off, int len) throws IOException
    {
        synchronized (lock) {
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            else if (len == 0) {
                return;
            }
            int ci = off, end = off + len;
            boolean bufferFlushed = false; 
            while (ci < end) {
                boolean bufferFull = false;
                try {
                    nextByte += ctb.convertAny(cbuf, ci, end,
                                               bb, nextByte, nBytes);
                    ci = end;
                }
                catch (ConversionBufferFullException x) {
                    int nci = ctb.nextCharIndex();
                    if ((nci == ci) && bufferFlushed) {
                        /* If the buffer has been flushed and it 
                            still does not hold even one character */
                        throw new 
                            CharConversionException("Output buffer too small");
                    }
                    ci = nci;
                    bufferFull = true;
                    nextByte = ctb.nextByteIndex();
                } 
                if ((nextByte >= nBytes) || bufferFull) {
                    out.write(bb, 0, nextByte);
                    nextByte = 0;
                    bufferFlushed = true;
                }
            }
        }
    }

    /**
        Flush remaining data to the underlying output stream.
    */
    public void flush () throws IOException
    {
        synchronized (lock) {
            flushBuf();
            out.flush();
        }
    }

    /**
        Flush the output buffer to the underlying byte stream, without flushing
        the byte stream itself.
    */
    private void flushBuf () throws IOException
    {
        synchronized (lock) {
            ensureOpen();

            for (;;) {
                try {
                    nextByte += ctb.flushAny(bb, nextByte, nBytes);
                }
                catch (ConversionBufferFullException x) {
                    nextByte = ctb.nextByteIndex();
                }
                if (nextByte == 0) {
                    break;
                }
                if (nextByte > 0) {
                    out.write(bb, 0, nextByte);
                    nextByte = 0;
                }
            }
        }
    }

    /**
        Close the stream. Note that we do not call the super class's close method
        because they call their own flush, and we don't want to flush our data twice.
        
        @exception  IOException  If an I/O error occurs
    */
    public void close () throws IOException
    {
        synchronized (lock) {
            if (out == null) {
                return;
            }
            flush();
            out.close();
            out = null;
            bb = null;
        }
    }
}

