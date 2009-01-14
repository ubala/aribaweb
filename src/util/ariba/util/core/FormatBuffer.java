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

    $Id: //ariba/platform/util/core/ariba/util/core/FormatBuffer.java#8 $
*/

package ariba.util.core;

import java.lang.Character; // OK for javadoc bug
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
    A version of a FastStringBuffer that is able to append some basic
    java types directly without having to use intermediate
    strings. This class is used to try to improve performance by
    avoiding memory allocations in string formatting.

    @aribaapi documented
*/
public class FormatBuffer extends FastStringBuffer
{

/*---------------------------------------------------------------------------
    Private constants
  ---------------------------------------------------------------------------*/

    private static final int DefaultBufferSize = 2048;
    private static final int ReportThreshold   = 50000;

/*---------------------------------------------------------------------------
    Constructors
  ---------------------------------------------------------------------------*/

    /**
        Make a FormatBuffer.  Allocate the underlying string buffer to
        a default size.
        @aribaapi documented
    */
    public FormatBuffer ()
    {
        this(DefaultBufferSize);
    }

    /**
        Make a FormatBuffer.  Allocate the underlying string buffer to
        a specified size.

        @param size the number of characters the FormatBuffer can hold
        without growing.
        @aribaapi documented
    */
    public FormatBuffer (int size)
    {
        super(size);
        setDoublesCapacityWhenGrowing(true);
    }

/*---------------------------------------------------------------------------
    Methods
  ---------------------------------------------------------------------------*/

    /**
        Append a character to the FormatBuffer.

        @param c the character to append
        @aribaapi documented
    */
    public void append (char c)
    {
        super.append(c);
    }

    /**
        Append an integer to the FormatBuffer.

        @param i the integer to append
        @aribaapi documented
    */
    public void append (int i)
    {
        append(i, 10);
    }

    /**
        Append an integer in a radix format to the FormatBuffer.

        @param i the integer to append
        @param radix the radix to use for encoding the integer
        <b>i</b>. The radix must be between <b>Character.MIN_RADIX</b>
        and <b>Character.MAX_RADIX</b>

        @see java.lang.Character#MAX_RADIX
        @see java.lang.Character#MIN_RADIX
        @aribaapi documented
    */
    public void append (int i, int radix)
    {
        makeRoomFor(radix >= 8 ? 12 : 33);

        if (i < 0) {
            buffer[length++] = '-';
        }
        else {
            i = -i;
        }

        int front = length;

        while (i <= -radix) {
            buffer[length++] = Character.forDigit(-(i % radix), radix);
            i = i / radix;
        }

        buffer[length++] = Character.forDigit(-i, radix);

            // reverse answer
        int back = length - 1;
        while (front < back) {
            char temp = buffer[front];
            buffer[front] = buffer[back];
            buffer[back] = temp;
            front++;
            back--;
        }
    }

    /**
        Append a long to the FormatBuffer.

        @param l the long to append
        @aribaapi documented
    */
    public void append (long l)
    {
        if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            append((int)l, 10);
        }
        else {
            append(Long.toString(l));
        }
    }

    /**
        Append a double to the FormatBuffer.

        @param d the double to append
        @aribaapi documented
    */
    public void append (double d)
    {
        append(Double.toString(d));
    }

    /**
        Append a one FormatBuffer to another FormatBuffer. This method
        is faster than converting the second FormatBuffer buffer to a
        string first.

        @param that a FormatBuffer to append to this FormatBuffer
        @aribaapi documented
    */
    public void append (FormatBuffer that)
    {
        int len  = that.length;
        makeRoomFor(len);
        for (int i = 0; i < len; i++) {
            this.buffer[length++] = that.buffer[i];
        }
    }

/*---------------------------------------------------------------------------
    Supporting old interface
  ---------------------------------------------------------------------------*/


    /**
        Return the number of bytes used in buffer.

        @return the current number of characters stored.

        @aribaapi documented
    */
    public int size ()
    {
        return length;
    }

    /**
        Determine if the FormatBuffer is empty.

        @return <b>true</b> if the FormatBuffer is empty, <b>false</b>
        otherwise
        @aribaapi documented
    */
    public boolean isEmpty ()
    {
        return length == 0;
    }

    /*-----------------------------------------------------------------------
        output
      -----------------------------------------------------------------------*/
    /**
        Write the contents of this FormatBuffer to a <code>Writer</code>.

        @param out the PrintWriter to write the output into.
        @aribaapi documented
    */
    public void print (PrintWriter out)
    {
        out.write(buffer, 0, length);
    }

    /**
        Write the contents of this FormatBuffer to a Writer.

        @param out the PrintWriter to write the output into.
        @aribaapi documented
    */
    public void print (Writer out) throws IOException
    {
        out.write(buffer, 0, length);
    }

    /**
        Write the contents of this FormatBuffer to an OutputStream.

        @param out the OutputStream to write the output into.

        @exception IOException if an error occurs in writing to
        <b>out</b>
        @aribaapi documented
    */
    public void print (OutputStream out) throws IOException
    {
            // Originally for PrintStreams, changed it to be for
            // Streams in general. The algorithm is just as inefficent
            // as PrintStream was so don't feel like you are missing
            // out on performance or anything.
        for (int i = 0; i < length; i++) {
            out.write(buffer[i]);
        }
    }

    /*-----------------------------------------------------------------------
        resize
      -----------------------------------------------------------------------*/

    void setCapacity(int newCapacity)
    {
        super.setCapacity(newCapacity);
        if (newCapacity > ReportThreshold) {
                // We don't use logging here because this method can be called
                // during processing of a logging event, which can lead to a
                // deadlock
            SystemUtil.out().println(StringUtil.strcat(
                "Info: util core FormatBuffer grew to ",
                Integer.toString(newCapacity)));
        }
    }
}



