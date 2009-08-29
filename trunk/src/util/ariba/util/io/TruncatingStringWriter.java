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

    $Id: //ariba/platform/util/core/ariba/util/io/TruncatingStringWriter.java#1 $
*/

package ariba.util.io;

import ariba.util.core.Fmt;
import java.io.StringWriter;

/*
 * This writer is used as a size limiting StringWriter.
 * It will throw TruncationException to indicate that it has reached the size limit.
 * Please note that TruncationException is a RuntimeException.
 * This is an unchecked exception and so requires special handling.
 */
public class TruncatingStringWriter extends StringWriter
{
    private int m_maxSize = 0;
    private static final String ResourceFile = "Log.util";
    private static final String TruncationKey = "10280";

    /**
     * Create a String buffer writer which will cap the size of the string.
     *
     * @param  maxSize  The approximate maxmimum size of the string to output.
     */
    public TruncatingStringWriter (int maxSize)
    {
        m_maxSize = maxSize;
    }

    private void checkSizeLimit ()
    {
        if (getBuffer().length() >= m_maxSize) {
            String truncationMessage = Fmt.Sil(ResourceFile, TruncationKey, m_maxSize);
            super.write(truncationMessage);
            throw new TruncationException(truncationMessage);
        }
    }

    /**
     * Write a single character.
     */
    public void write (int c)
    {
        super.write(c);
        checkSizeLimit();
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param  cbuf  Array of characters
     * @param  off   Offset from which to start writing characters
     * @param  len   Number of characters to write
     */
    public void write (char cbuf[], int off, int len)
    {
        super.write(cbuf, off, len);
        checkSizeLimit();
    }

    /**
     * Write a string.
     */
    public void write (String str)
    {
        super.write(str);
        checkSizeLimit();
    }

    /**
     * Write a portion of a string.
     *
     * @param  str  String to be written
     * @param  off  Offset from which to start writing characters
     * @param  len  Number of characters to write
     */
    public void write (String str, int off, int len)
    {
        super.write(str, off, len);
        checkSizeLimit();
    }

    /**
     * Appends the specified character sequence to this writer.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.write(csq.toString()) </pre>
     *
     * <p> Depending on the specification of <tt>toString</tt> for the
     * character sequence <tt>csq</tt>, the entire sequence may not be
     * appended. For instance, invoking the <tt>toString</tt> method of a
     * character buffer will return a subsequence whose content depends upon
     * the buffer's position and limit.
     *
     * @param  csq
     *         The character sequence to append.  If <tt>csq</tt> is
     *         <tt>null</tt>, then the four characters <tt>"null"</tt> are
     *         appended to this writer.
     *
     * @return  This writer
     *
     * @since  1.5
     */
    public StringWriter append (CharSequence csq)
    {
        StringWriter result = super.append(csq);
        checkSizeLimit();
        return result;
    }

    /**
     * Appends a subsequence of the specified character sequence to this writer.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq, start,
     * end)</tt> when <tt>csq</tt> is not <tt>null</tt>, behaves in
     * exactly the same way as the invocation
     *
     * <pre>
     *     out.write(csq.subSequence(start, end).toString()) </pre>
     *
     * @param  csq
     *         The character sequence from which a subsequence will be
     *         appended.  If <tt>csq</tt> is <tt>null</tt>, then characters
     *         will be appended as if <tt>csq</tt> contained the four
     *         characters <tt>"null"</tt>.
     *
     * @param  start
     *         The index of the first character in the subsequence
     *
     * @param  end
     *         The index of the character following the last character in the
     *         subsequence
     *
     * @return  This writer
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>start</tt> or <tt>end</tt> are negative, <tt>start</tt>
     *          is greater than <tt>end</tt>, or <tt>end</tt> is greater than
     *          <tt>csq.length()</tt>
     *
     * @since  1.5
     */
    public StringWriter append (CharSequence csq, int start, int end)
    {
        StringWriter result = super.append(csq, start, end);
        checkSizeLimit();
        return result;
    }

    /**
     * Appends the specified character to this writer.
     *
     * <p> An invocation of this method of the form <tt>out.append(c)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.write(c) </pre>
     *
     * @param  c
     *         The 16-bit character to append
     *
     * @return  This writer
     *
     * @since 1.5
     */
    public StringWriter append (char c)
    {
        StringWriter result = super.append(c);
        checkSizeLimit();
        return result;
    }
}
