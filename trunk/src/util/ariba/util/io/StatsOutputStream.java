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

    $Id: //ariba/platform/util/core/ariba/util/io/StatsOutputStream.java#4 $
*/

package ariba.util.io;

import java.io.OutputStream;
import java.io.IOException;

/**
    StatsOutputStream is an OutputStream that keeps track of the
    number of bytes that have been written.

    @see OutputStream

    @aribaapi private
*/
public class StatsOutputStream extends OutputStream
{
    /**
        Number of bytes written.
    */
    public long count;
    private OutputStream base;

    /**
        Creates a StatsOutputStream.
        @param base the OutputStream
    */
    public StatsOutputStream (OutputStream base)
    {
        this.base = base;
        this.count = 0;
    }

    /**
        Writes a byte. This method will block until the byte is actually
        written.
        @param b    the byte
        @exception IOException If an I/O error has occurred.
    */
    public void write (int b) throws IOException
    {
        base.write(b);
        count++;
    }

    /**
        Writes an array of bytes. This method will block until the bytes
        are actually written.
        @param b    the data to be written
        @exception IOException If an I/O error has occurred.
    */
    public void write (byte b[]) throws IOException
    {
        base.write(b);
        count += b.length;
    }

    /**
        Writes a sub array of bytes.
        @param b    the data to be written
        @param off    the start offset in the data
        @param len    the number of bytes that are written
        @exception IOException If an I/O error has occurred.
    */
    public void write (byte b[], int off, int len) throws IOException
    {
        base.write(b, off, len);
        count += len;
    }

    /**
        Flushes the stream. This will write any buffered
        output bytes.
        @exception IOException If an I/O error has occurred.
    */
    public void flush () throws IOException
    {
        base.flush();
    }

    /**
        Closes the stream. This method must be called
        to release any resources associated with the
        stream.
        @exception IOException If an I/O error has occurred.
    */
    public void close () throws IOException
    {
        base.close();
    }
}
