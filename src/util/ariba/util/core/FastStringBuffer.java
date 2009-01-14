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

    $Id: //ariba/platform/util/core/ariba/util/core/FastStringBuffer.java#11 $
*/

package ariba.util.core;

/**
   Object subclass resembling the java.lang.StringBuffer class (an object that
   manages a mutable string).  Unlike java.lang.StringBuffer, none of
   FastStringBuffer's methods are synchronized, which results in a significant
   performance increase.  FastStringBuffer also has additional API that allows
   it to be more easily modified than the standard StringBuffer.

   @aribaapi documented
*/
public class FastStringBuffer
{
    String      string;
    char        buffer[];
    int         length;
    boolean     doublesCapacity;

        // the maximum number of bytes we'll add to our capacity
    int maxGrowIncrement = 4 * 1024 * 1024;

    /* constructors */

    /**
        Constructs an empty String buffer with the specified initial length.
        @param length the initial length, must be greater than or
        equal to zero
        @aribaapi documented
    */
    public FastStringBuffer (int length)
    {
        this.string = null;
        this.buffer = new char[length];
        this.length = 0;
        this.doublesCapacity = true;
    }

    /**
        Creates a FastStringBuffer containing the empty string.
        @aribaapi documented
    */
    public FastStringBuffer ()
    {
        this("");
    }

    /**
        Creates a FastStringBuffer containing the characters in <b>aString</b>.
        @param aString   the initial string to hold
        @aribaapi documented
    */
    public FastStringBuffer (String aString)
    {
        super();

        if (aString == null || aString.equals("")) {
            buffer = new char[8];
        }
        else {
            buffer = new char[aString.length() + 1];
            setStringValue(aString);
        }
        doublesCapacity=true;
    }

    /**
        Creates a FastStringBuffer containing the characters in <b>aString</b>
        from the index <b>start</b> to <b>end</b>.

        @param aString  the string to copy a range of characters from
        @param start    the index of the string to start copying from
        @param end      the index of the string to end copying by, the
        character at position <b>end</b> is excluded.
        @aribaapi documented
    */
    public FastStringBuffer (String aString, int start, int end)
    {
        buffer = new char[end - start];
        length = end-start;
        string = null;
        doublesCapacity = true;
        aString.getChars(start, end, buffer, 0);
    }

    /**
        Creates a FastStringBuffer containing the character <b>aChar</b>.

        @param aChar the initial character to contain
        @aribaapi documented
    */
    public FastStringBuffer (char aChar)
    {
        super();

        buffer = new char[8];
        buffer[0] = aChar;
        length = 1;
        doublesCapacity=true;
    }


    /**
        Return the buffer holding the state of this FormatBuffer.  The
        effect on the returned array of operations performed on this
        FormatBuffer is not defined.

        @return returns the character array that is used for internal
        storage. There may be garbage characters after the length.

        @aribaapi documented
    */
    public char[] getBuffer ()
    {
        return buffer;
    }

    void setCapacity (int newCapacity)
    {
        char oldBuffer[] = buffer;
        buffer = new char[newCapacity];
        System.arraycopy(oldBuffer, 0, buffer, 0, length);
    }

    void makeRoomFor (int want)
    {
        int avail = buffer.length - length;

        if (want <= avail) {
            return;
        }

        int proposedIncrease = doublesCapacity ? buffer.length : 20;
        proposedIncrease = Math.min(maxGrowIncrement, proposedIncrease);

        if (want > avail + proposedIncrease) {
            setCapacity(buffer.length + want);
        }
        else {
            setCapacity(buffer.length + proposedIncrease);
        }
    }



    /**
        Set whether the FastStringBuffer should double its size
        when some data is inserted and the internal buffer is
        too small.

        @param aFlag if true (the default value if the method is not
        called) the array will double capacity when growing. If false
        it will only pad by up to 20 characters.
        @aribaapi documented
    */
    public void setDoublesCapacityWhenGrowing (boolean aFlag)
    {
        doublesCapacity = aFlag;
    }

    /**
        Returns whether FastStringBuffer doubles its size when
        some data is inserted and the internal buffer is
        too small.
        @return true if the buffer will double when growing, false
        otherwise.
        @aribaapi documented
    */
    public boolean getDoublesCapacityWhenGrowing ()
    {
        return doublesCapacity;
    }

    /**
        Sets the FastStringBuffer's contents to the characters in
        <b>aString</b>.
        @param aString the string to set the contents of the
        fastStringBuffer to. Any previous contents will be cleared.
        @aribaapi documented
    */
    public void setStringValue (String aString)
    {
        length = 0;
        append(aString);
        string = aString;
    }

    /**
        Returns the String for the FastStringBuffer's contents.

        @param startIndex the index of the FastStringBuffer to start
        copying from.
        @param endIndex the index of the FastStringBuffer to end
        copying with. The character at endIndex is excluded.

        @return a new String containing the characters between the
        specified start and end index into the FastStringBuffer

        @exception StringIndexOutOfBoundsException if the
        <code>startIndex</code> or <code>endIndex</code> arguments
        index characters outside the bounds of the FastStringBuffer.

        @aribaapi documented
    */
    public String substring (int startIndex, int endIndex)
    {
        if (startIndex < 0 || startIndex > length) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex > length) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        return new String(buffer, startIndex, endIndex - startIndex);
    }

    /**
        Returns the String for the FastStringBuffer's contents.
        @return a string representing the contents of this buffer.
        @aribaapi documented
    */
    public String toString ()
    {
        if (string == null) {
            if (length == 0) {
                string = "";
            }
            else {
                string = new String(buffer, 0, length);
            }
        }

        return string;
    }

    /**
        Returns the character at <b>index</b>.
        @param index the index of the character to return.
        @return the character at the location specified.
        @exception StringIndexOutOfBoundsException If the index is invalid
        @aribaapi documented
    */
    public char charAt (int index)
    {
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException(index);
        }

        return buffer[index];
    }

    /**
        Check if the FastStringBuffer's characters start with the
        contents of a specified String.

        @param value the String to match against
        @return <b>true</b> if the specified <b>value</b> matches the
        first characters in the FastStringBuffer, <b>false</b>
        otherwise. Returns true if <b>value</b> is empty. If value is
        not empty, but the FastStringBuffer is empty, it will return
        false.

        @aribaapi documented
    */
    public boolean startsWith (String value)
    {
        return this.startsWith(value, 0);
    }

    /**
        Check if the FastStringBuffer's characters start with the
        contents of a specified String.

        @param value the String to match against.
        @param offset the offset of FastStringBuffer's contents to
        start the comparison at.

        @return <b>true</b> if the specified <b>value</b> matches the
        offset characters in the FastStringBuffer, <b>false</b>
        otherwise. Returns true if <b>value</b> is empty as long as
        the offset is valid.

        @aribaapi documented
    */
    public boolean startsWith (String value, int offset)
    {
        int valueLength = value.length();
        if (valueLength == 0) {
            return true;
        }
        if (length == 0) {
            return false;
        }
        if (offset < 0 || offset + valueLength > length) {
            return false;
        }
        for (int idx = 0; idx < valueLength; idx++) {
            if (this.buffer[offset+idx]!=value.charAt(idx)) {
                return false;
            }
        }
        return true;
    }

    /**
        Returns the index of the first occurrence of <b>value</b> in the
        FastStringBuffer.  If the <b>value</b> is not found, -1 is returned.
        @param value the String to search for in the FastStringBuffer

        @return the index of the first occurrence of the String in the
        character sequence represented by this object, or
        <code>-1</code> if the String does not occur.
        @aribaapi documented
    */
    public int indexOf (String value)
    {
        return this.indexOf(value, 0);
    }

    /**
        Returns the index of the first occurrence of <b>value</b> in the
        FastStringBuffer, starting at character <b>offset</b>.  If the
        <b>value</b> is not found, -1 is returned.
        @param value the String to search for in the FastStringBuffer
        @param offset the offset into the FastStringBuffer's contents
        to start searching at.

        @return the index of the first occurrence of the String in the
        character sequence represented by this object starting at the
        specified offset, or <code>-1</code> if the String does not
        occur or if the index is out of bounds.

        @aribaapi documented
    */
    public int indexOf (String value, int offset)
    {
        if (offset < 0 || offset >= this.length) {
            return -1;
        }

        if (offset + value.length() > this.length) {
            return -1;
        }
        /* String.java: There is an empty string at index 0 in an
           empty string. */
        if (this.length == 0 && value.length() == 0 && offset == 0) {
            return 0;
        }
        if (this.length == 0) {
            return -1;
        }
        char sentinal = value.charAt(0);

        int idx = offset;
        int startMatch;
        int lastSearchIndex = this.length - value.length();

        /*
            The while loop is broken into two sections:

            1) First we quickly scan for the start of a potential match

            2) If the first character matches then compare on a
            char-by-char basis for match

            If 1 fails there will be no match

            If 2 fails continue scanning from just beyond the last scan hit
        */
        while (true) {
            boolean found = false;
            // 1) Quick scan
            while (!found && idx <= lastSearchIndex) {
                if (this.buffer[idx] == sentinal) {
                    found = true;
                }
                idx++;
            }

            if (!found) {
                return -1;
            }

            // 2) Exhaustive match
            startMatch = idx - 1;
            int valueLength = value.length();
            for (int jdx = 1; jdx < valueLength; jdx++) {

                if (this.buffer[idx] != value.charAt(jdx)) {
                    found = false;
                    break;
                }
                idx++;
            }
            if (found) {
                return startMatch;
            }

                // don't forget to rewind back to the last place we
                // stopped our scan for sentinal
            idx = startMatch + 1;
        }
    }

    /**
        Returns the index of the first occurrence of <b>aChar</b> in the
        FastStringBuffer, starting at character <b>offset</b>.
        @param aChar the character to locate in the FastStringBuffer
        @param offset the offset in the FastStringBuffer for the first
        character to search

        @return the index of the first occurrence of the char in the
        character sequence represented by this object starting at the
        specified offset, or <code>-1</code> if the char does not
        occur.

        @aribaapi documented
    */
    public int indexOf (char aChar, int offset)
    {
        int     i;

        if (offset < 0 || offset >= length) {
            return -1;
        }

        for (i = offset; i < length; i++) {
            if (buffer[i] == aChar) {
                return i;
            }
        }

        return -1;
    }

    /**
        Returns the index of the first occurrence of <b>aChar</b> in the
        FastStringBuffer.  Equivalent to the code:
        <pre>
        indexOf(aChar, 0);
        </pre>

        @return the index of the first occurrence of the char in the
        character sequence represented by this object, or
        <code>-1</code> if the char does not occur.

        @see #indexOf
        @param aChar the character to locate in the FastStringBuffer
        @aribaapi documented
    */
    public int indexOf (char aChar)
    {
        return indexOf(aChar, 0);
    }

    /**
        Check if a tab or a space occurs at the specified position in
        the FastStringBuffer.

        @param index the index of the FastStringBuffer's contents to check.
        @return <b>true</b> if the FastStringBuffer contains a space
        or tab character at position <b>index</b>, <b>false</b>
        otherwise
        @exception StringIndexOutOfBoundsException If the index is invalid.
        @aribaapi documented
    */
    public boolean tabOrSpaceAt (int index)
    {
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException(index);
        }

        return (buffer[index] == ' ' || buffer[index] == '\t');
    }

    /**
        Sets the character at the location <b>index</b> to the
        character <b>ch</b>.
        @param index the index of the character in the
        FastStringBuffer to replace. It must be >= 0 and <= the length.
        @param ch the character to replace the value with.
        @exception StringIndexOutOfBoundsException If the index is invalid.
        @aribaapi documented
    */
    public void setCharAt (int index, char ch)
    {
        if ((index < 0) || (index >= length)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        buffer[index] = ch;
        string = null;
    }

    /**
        Appends <b>aChar</b> to the FastStringBuffer.
        @param aChar the character to append to the FastStringBuffer
        @aribaapi documented
    */
    public void append (char aChar)
    {
        if (length == buffer.length) {
            makeRoomFor(1);
        }

        buffer[length++] = aChar;
        string = null;
    }

    private static final String JavaNullString = String.valueOf((String)null);
    /**
        Appends <b>aString</b> to the FastStringBuffer.
        The behavior of append(null) is not defined.
        @param aString the String to append to the FastStringBuffer
        @aribaapi documented
    */
    public void append (String aString)
    {
        boolean wasEmpty = (this.length == 0);
            // I want to assert, but that breaks catalog. StringBuffer
            // appends String.valueOf((String)null) but that would
            // also break catalog.
        if (aString == null) {
            return;
        }
        appendStringRange(aString, 0, aString.length());
        if (wasEmpty) {
            string = aString;
        }
    }

    /**
        Appends a string representation of <b>aObject</b> to the
        FastStringBuffer. Equivalent to the code:
        <pre>
        append(String.valueOf(aObject));
        </pre>

        @param aObject the Object to append the String value of.
        @aribaapi documented
    */
    public void append (Object aObject)
    {
        append(String.valueOf(aObject));
    }

    /**
        Appends the char array to the FastStringBuffer.

        @param value the character array to append to the string
        @aribaapi documented
    */
    public void append (char[] value)
    {
        replace(length, value);
    }

    /**
        Appends a FastStringBuffer to this FastStringBuffer.

        @param aBuffer the FastStringBuffer to append to this
        FastStringBuffer.
        @aribaapi documented
    */
    public void append (FastStringBuffer aBuffer)
    {
        appendCharRange(aBuffer.buffer, 0, aBuffer.length);
    }

    /**
        Copies characters from the string into the FastStringBuffer.
        <p>
        The first character to be copied is at index <code>startOffset</code>;
        the last character to be copied is at index <code>endOffset-1</code>
        (thus the total number of characters to be copied is
        <code>endOffset-startOffset</code>).
        @param aString the source String
        @param startOffset index of the first character in the string
        to copy.
        @param endOffset index after the last character in the string
        to copy.
        @exception StringIndexOutOfBoundsException If srcBegin or
        srcEnd is out of range, or if srcBegin is greater than the
        srcEnd.
        @aribaapi documented
    */
    public void appendStringRange (String aString,
                                   int startOffset,
                                   int endOffset)
    {
            // can't find docs on boundry conditions...play it safe
        if (startOffset == endOffset) {
            return;
        }
            // give room for the new data
        makeRoomFor(endOffset - startOffset);
        aString.getChars(startOffset, endOffset, buffer, length);
        length += endOffset - startOffset;
        string = null;
    }

    /**
        Copies characters from the character array into the
        FastStringBuffer.

        <p>
        The first character to be copied is at index <code>startOffset</code>;
        the last character to be copied is at index <code>endOffset-1</code>
        (thus the total number of characters to be copied is
        <code>endOffset-startOffset</code>).
        @param      aChar       the source character array
        @param      startOffset index of the first character in the array
                                to copy.
        @param      endOffset   index after the last character in the array
                                to copy.
        @exception StringIndexOutOfBoundsException If startOffset or
        endOffset is out of range, or if startOffset is greater than the
        endOffset.

        @aribaapi documented
    */
    public void appendCharRange (char []aChar,
                                 int startOffset,
                                 int endOffset)
    {
            // can't find docs on boundry conditions...play it safe
        if (startOffset == endOffset) {
            return;
        }
            // give room for the new data
        makeRoomFor(endOffset - startOffset);
        System.arraycopy(aChar, startOffset, this.buffer, this.length,
                         endOffset - startOffset);
        length += endOffset - startOffset;
        string = null;
    }

    /**
        Replaces <b>value</b> at <b>index</b>.  If <b>index</b> +
        value.length() is greater than or equal to the number of
        characters within the buffer, it is expanded to accommodate.

        @param index the index in the FastStringBuffer to start
        replacing characters at.
        @param value a character array used for the replacement
        contents.

        @exception StringIndexOutOfBoundsException if the index is invalid.
        @aribaapi documented
    */
    public void replace (int index, char [] value)
    {
        if ((index < 0) || (index > length)) {
            throw new StringIndexOutOfBoundsException(index);
        }

        int extraSpaceNeeded = index + value.length - this.length;
        if (extraSpaceNeeded > 0) {
            this.makeRoomFor(extraSpaceNeeded);
            this.length += extraSpaceNeeded;
        }
        System.arraycopy(value, 0, this.buffer, index, value.length);
        this.string = null;
    }

    /**
        Replaces <b>value</b> at <b>index</b> for span chars. If
        <b>index</b> + value.length() is greater than or equal to the
        number of characters within the buffer is expanded to
        accommodate.

        @param index index to start replacing at
        @param value character to replace with
        @param span number of characters to replace

        @exception StringIndexOutOfBoundsException if the index is
        invalid
        @aribaapi documented
    */
    public void replace (int index, char value, int span)
    {
        if ((index < 0) || (index >= length)) {
            throw new StringIndexOutOfBoundsException(index);
        }

        int extraSpaceNeeded = index + span - this.length;
        if (extraSpaceNeeded > 0) {
            this.makeRoomFor(extraSpaceNeeded);
            this.length += extraSpaceNeeded;
        }

        for (int idx = 0; idx < span; idx++) {
            this.buffer[idx+index] = value;
        }
        this.string = null;
    }

    /**
        For each instance of <code>oldString</code> found in the
        buffer, we'll replace it with <code>newString</code>.

        @param oldString string to replace
        @param newString replacement string
        @aribaapi documented
    */
    public void replace (String oldString, String newString)
    {
        Assert.that(newString != null && oldString != null,
                    "You may not use a null string with StringBuffers");

        if (oldString.length()==0) {
            return;
        }
        int i = indexOf(oldString);
        int oldLength = oldString.length();
        int newLength = newString.length();
        while (i > -1) {
            int from = i+oldLength;
            if (from < length()) {
                moveChars(from, i);
            }
            else {
                truncateToLength(i);
            }
            insert(newString, i);
            i += newLength;

            if (i >= length()) {
                break;
            }

            i = indexOf(oldString, i);
        }
    }

    /**
        Inserts <b>aChar</b> at <b>index</b>.  If <b>index</b> is
        greater than or equal to the number of characters within the buffer,
        appends <b>aChar</b>.

        @param aChar character to insert
        @param index location to insert character at

        @exception StringIndexOutOfBoundsException if the index is invalid.
        @aribaapi documented
    */
    public void insert (char aChar, int index)
    {
        char    oldBuffer[];

        if (index < 0) {
            throw new StringIndexOutOfBoundsException(index);
        }
        else if (index >= length) {
            append(aChar);
            return;
        }

        if (length < buffer.length) {
            System.arraycopy(buffer, index, buffer, index + 1,
                             length - index);
            buffer[index] = aChar;
            length++;
            string = null;
            return;
        }

        oldBuffer = buffer;
        buffer = new char[buffer.length + 20];
        if (index > 0) {
            System.arraycopy(oldBuffer, 0, buffer, 0, index);
        }
        if (index < length) {
            System.arraycopy(oldBuffer, index, buffer, index + 1,
                             length - index);
        }
        buffer[index] = aChar;
        length++;

        string = null;
    }

    /**
        Inserts <b>aString</b> at <b>index</b>.  If <b>index</b> is
        greater than or equal to the number of characters within the buffer,
        appends <b>aString</b>.

        @param aString string to insert into the FastStringBuffer
        @param index location to insert string.

        @exception StringIndexOutOfBoundsException If the index is invalid.
        @aribaapi documented
    */
    public void insert (String aString, int index)
    {
        Assert.that(aString != null,
                    "May not use null strings with FastStringBuffer");
        char    oldBuffer[];
        int     stringLength;

        if (index < 0) {
            throw new StringIndexOutOfBoundsException(index);
        }
        else if (index > length) {
            append(aString);
            return;
        }
        else if (aString.equals("")) {
            return;
        }

        stringLength = aString.length();
        if (length + stringLength < buffer.length) {
            System.arraycopy(buffer, index, buffer, index + stringLength,
                             length - index);
            aString.getChars(0, stringLength, buffer, index);
            length += stringLength;
            string = null;
            return;
        }

        oldBuffer = buffer;
        buffer = new char[length + stringLength + 20];
        if (index > 0) {
            System.arraycopy(oldBuffer, 0, buffer, 0, index);
        }
        System.arraycopy(oldBuffer, index, buffer, index + stringLength,
                         length - index);
        aString.getChars(0, stringLength, buffer, index);
        length += stringLength;

        string = null;
    }

    /**
        Removes the character at <b>index</b>.
        @param index location to remove the character from

        @exception StringIndexOutOfBoundsException if the index is invalid.
        @aribaapi documented
    */
    public void removeCharAt (int index)
    {
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException(index);
        }

        if (index + 1 == length) {
            length--;
            string = null;
            return;
        }

        System.arraycopy(buffer, index + 1, buffer, index, length - (index+1));
        length--;

        string = null;
    }

    /**
        Truncates the FastStringBuffer to <b>aLength</b> characters.  If
        <b>aLength</b> is invalid, does nothing.

        @param aLength the length to truncate the FastStringBuffer to
        @aribaapi documented
    */
    public void truncateToLength (int aLength)
    {
        if (aLength < 0 || aLength > length) {
            return;
        }

        length = aLength;

        string = null;
    }

    /**
        Returns the number of characters in the FastStringBuffer.

        @return the current number of characters stored.

        @aribaapi documented
    */
    public int length ()
    {
        return length;
    }

    /**
        This removes the characters between <b>fromIndex</b> and
        <b>toIndex</b> (non inclusive.)

        if fromIndex <= toIndex the method does nothing.

        @param fromIndex index to start removing characters from
        @param toIndex index to end removing characters
        @exception StringIndexOutOfBoundsException if the fromIndex
        and toIndex are outside the range of the string.
        @aribaapi documented
    */
    public void moveChars (int fromIndex, int toIndex)
    {
        if (fromIndex <= toIndex) {
            return;
        }
        else if (fromIndex < 0 || fromIndex >= length) {
            throw new StringIndexOutOfBoundsException(fromIndex);
        }
        else if (toIndex < 0 || toIndex >= length) {
            throw new StringIndexOutOfBoundsException(toIndex);
        }

        System.arraycopy(buffer, fromIndex, buffer, toIndex,
                         length - fromIndex);
        length -= fromIndex - toIndex;

        string = null;
    }

    /**
        Returns the FastStringBuffer's char array, for situation where it is
        needed.  For example, you can draw the FastStringBuffer's contents
        by passing the array to the Graphic's <b>drawString()</b> method that
        takes a char array, rather than first convert the StringBuffer to a
        String.  You should never modify this array yourself.

        @return returns the character array that is used for internal
        storage. There may be garbage characters after the length.
        @aribaapi documented
    */
    public char[] charArray ()
    {
        return buffer;
    }
}
