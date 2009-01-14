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

    $Id: //ariba/platform/util/core/ariba/util/io/StatsInputStream.java#4 $
*/

package ariba.util.io;

import java.io.InputStream;
import java.io.IOException;

/**
    StatsInputStream is an InputStream that keeps track of the number
    of bytes that have been read in. @see InputStream

    @aribaapi private
*/
public class StatsInputStream extends InputStream
{
    /**
        Number of bytes read.
    */
    public long count;
    private InputStream base;

    /**
        Creates a StatsInputStream.
        @param base the InputStream
    */
    public StatsInputStream (InputStream base)
    {
        this.base = base;
        this.count = 0;
    }

    /**
        Reads a byte of data.
        @return the byte read, or -1 if the end of the stream is reached.
        @exception IOException If an I/O error has occurred.
    */
    public int read () throws IOException
    {
        int n = base.read();
        if (n > 0) {
            count++;
        }
        return n;
    }

    /**
        Reads into an array of bytes.  This method will
        block until some input is available.
        @param b    the buffer into which the data is read
        @return  the actual number of bytes read, -1 is
        returned when the end of the stream is reached.
        @exception IOException If an I/O error has occurred.
    */
    public int read (byte b[]) throws IOException
    {
        int n = base.read(b);
        if (n > 0) {
            count += n;
        }
        return n;
    }

    /**
        Reads into an array of bytes.  This method will
        block until some input is available.
        @param b    the buffer into which the data is read
        @param off the start offset of the data
        @param len the maximum number of bytes read
        @return  the actual number of bytes read, -1 is
        returned when the end of the stream is reached.
        @exception IOException If an I/O error has occurred.
    */
    public int read (byte b[], int off, int len) throws IOException
    {
        int n = base.read(b, off, len);
        if (n > 0) {
            count += n;
        }
        return n;
    }

    /**
        Skips n bytes of input.  We count skipped bytes as "read" in
        the count field.
        @param n the number of bytes to be skipped
        @return    the actual number of bytes skipped.
        @exception IOException If an I/O error has occurred.
    */
    public long skip (long n) throws IOException
    {
        long skipped = base.skip(n);
        if (skipped > 0) {
            count += skipped;
        }
        return skipped;
    }

    /**
        Returns the number of bytes that can be read
        without blocking.
        @return the number of available bytes.
    */
    public int available () throws IOException
    {
        return base.available();
    }

    /**
        Closes the input stream. Must be called
        to release any resources associated with
        the stream.
        @exception IOException If an I/O error has occurred.
    */
    public void close () throws IOException
    {
        base.close();
    }

    /**
        Marks the current position in the input stream.  A subsequent
        call to reset() will reposition the stream at the last
        marked position so that subsequent reads will re-read
        the same bytes.  The stream promises to allow readlimit bytes
        to be read before the mark position gets invalidated.
        *
        @param readlimit the maximum limit of bytes allowed to be read
        before the mark position becomes invalid.
    */
    public void mark (int readlimit)
    {
        base.mark(readlimit);
    }

    /**
        Repositions the stream to the last marked position.  If the
        stream has not been marked, or if the mark has been invalidated,
        an IOException is thrown.  Stream marks are intended to be used in
        situations where you need to read ahead a little to see what's in
        the stream.  Often this is most easily done by invoking some
        general parser.  If the stream is of the type handled by the
        parser, it just chugs along happily.  If the stream is not of
        that type, the parser should toss an exception when it fails,
        which, if it happens within readlimit bytes, allows the outer
        code to reset the stream and try another parser.
        @exception IOException If the stream has not been marked or
        if the mark has been invalidated.
    */
    public void reset () throws IOException
    {
        base.reset();
    }

    /**
        Returns a boolean indicating whether or not this stream type
        supports mark/reset.
        @return true if this stream type supports mark/reset; false
        otherwise.
    */
    public boolean markSupported ()
    {
        return base.markSupported();
    }

}
