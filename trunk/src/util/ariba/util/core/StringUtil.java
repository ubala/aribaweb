/*
    Copyright (c) 1996-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/StringUtil.java#41 $
*/

package ariba.util.core;

import ariba.util.formatter.Formatter;
import ariba.util.i18n.I18NUtil;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.Character; // OK for javadoc bug
import java.text.BreakIterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Collection;
import java.util.Iterator;
import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;

/**
    String Utilities. These are utilities for working with strings.

    @aribaapi documented
*/
public final class StringUtil
{
    /* prevent people from creating instances of this class */
    private StringUtil ()
    {
    }

    /**
        Determine if a String is null or empty.

        @param string a String object to check
        @return <b>true</b> if <B>string</B> is null or empty,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean nullOrEmptyString (String string)
    {
        return (string == null) || (string.length() == 0);
    }

    /**
        Determine if a string is null or empty or entirely spaces.

        @param string a String object to check
        @return <b>true</b> if <B>string</B> is null, empty, or all
        spaces; false otherwise
        @aribaapi documented
    */
    public static boolean nullOrEmptyOrBlankString (String string)
    {
        int length = (string != null) ? string.length() : 0;
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                if (!Character.isWhitespace(string.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
        Returns <code>string</code> with the first character upper-cased.
        If <code>string</code> is already uppercased or if it is <code>null</code>
        or less than one in length, the passed-in value is returned. <p/>

        @return <code>string</code> with the first character upper-cased
        @aribaapi ariba
    */
    public static String capitalizeFirstChar (String string)
    {
        String result = string;
        if (string != null && string.length() > 0) {
            char first = string.charAt(0);
            char upperFirst = Character.toUpperCase(first);
            if (first != upperFirst) {
                result = StringUtil.strcat(Character.toString(upperFirst),
                                           string.substring(1));
            }
        }
        return result;
    }

    /*
        @see separateAtCapitalization(String, char)
    */
    public static String separateAtCapitalization (String string)
    {
        return separateAtCapitalization(string, ' ');
    }

    /*
        @param string the string being acted upon
        @param separationCharacter the character used to separate the parts of the string
        @return the string with separation at a capitalized letter
        eg. separateAtCapitalization("ApprovableSummary", ' ') returns "Approvable Summary"
    */
    public static String separateAtCapitalization (String string, char separationChar)
    {
        if (string == null || (string.length() <= 1)) {
            return string;
        }
        int len = string.length();
        FastStringBuffer fsb = new FastStringBuffer(len);
        fsb.append(string.charAt(0));
          //No separation for the first character
        for (int i = 1; i < len; i++) {
            char c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                fsb.append(separationChar);
            }
            fsb.append(c);
        }
        return fsb.toString();
    }

    /**
        Performs a binary search on a sorted string array.

        @param keys a sorted array of string keys to search
        @param key a key to search for

        @return the index of the <B>key</B> in <B>keys</B>, or -1 if
        it's not found.
        @aribaapi documented
    */
    public static final int stringArrayIndex (String[] keys, String key)
    {
        int lo = 0;
        int hi = keys.length - 1;
        int probe;
        int result;
        while (lo <= hi) {
            probe = (lo + hi) >> 1;
            result = key.compareTo(keys[probe]);
            if (result == 0) {
                return probe;
            }
            else if (result > 0) {
                lo = probe + 1;
            }
            else {
                hi = probe - 1;
            }
        }

        return -1;
    }

    public static String intern (String source)
    {
        if (source == null) {
            return null;
        }
        else {
            return source.intern();
        }
    }

    private static Perl5Util perlUtil = new Perl5Util();

    /**
        Checks if a string matches a perl 5 pattern.
        See: org.apache.oro.text.perl.Perl5Util.match()

        example: stringMatchesPattern("foo", "/f.o/") returns true.

        @aribaapi ariba
    */
    public static boolean stringMatchesPattern (String str, String pattern)
    {
        try {
                // method is synchronized, no need to do it ourselves
            return perlUtil.match(pattern, str);
        }
        catch (MalformedPerl5PatternException ex) {
            Assert.that(false, "Malformed Perl Pattern: %s",
                        SystemUtil.stackTrace(ex));
            /* NOTREACHED */
            return false;
        }
    }


    /**
        Search an unsorted array of strings.

        @param keys an array of string keys to search
        @param key a key to search for using the == operator

        @return the index of the <B>key</B> in <B>keys</B>, or -1 if
        it's not found.
        @aribaapi documented
    */
    public static final int unorderedStringArrayIndexIdentical (
        String[] keys, String key)
    {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == key) {
                return i;
            }
        }
        return -1;
    }

    /**
        Search an unsorted array of strings.

        @param keys an array of string keys to search
        @param key a key to search for using the equals() method

        @return the index of the <B>key</B> in <B>keys</B>, or -1 if
        it's not found.
        @aribaapi documented
    */
    public static final int unorderedStringArrayIndex (
        String[] keys, String key)
    {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
        Tokenize a String with a specified separator character and
        return an array.

        <p>
        Takes a string in the form "Description.Price.Amount" with a
        delimiter of '.' and turns it into an array of strings:
        <p>
        <pre>
        path[0] = "Description"
        path[1] = "Price"
        path[2] = "Amount"
        </pre>

        @param str a string to tokenize
        @param delimiter the delimiter to use when tokenizing

        @return an array of Strings containing each substring but
        excluding the delimiter character

        @see ariba.util.core.ListUtil#delimitedStringToList

        @aribaapi documented
    */
    public static String[] delimitedStringToArray (String str, char delimiter)
    {
        int limit = occurs(str, delimiter);
        String[] array = new String[limit+1];

        int start = 0;
        int nextEnd;
        for (int i = 0; i < limit; i++) {
            nextEnd = str.indexOf(delimiter, start);
            array[i] = str.substring(start, nextEnd);
            start = nextEnd + 1;
        }

        array[limit] = str.substring(start);
        return array;
    }

    /**
        Count the number of occurrences of a character in a string.

        @param str the String to count in
        @param ch the character to count the occurrences of

        @return the number of times the character <B>ch</B> occurs in
        the string <B>str</B>.
        @aribaapi documented
    */
    public static int occurs (String str, char ch)
    {
        int count = 0;
        int offset = str.indexOf(ch, 0);
        while (offset != -1) {
            count++;
            offset = str.indexOf(ch, offset + 1);
        }
        return(count);
    }

    /**
        Count the number of occurrences of a substring in a string.

        @param str the String to count in
        @param substr the substring to count the occurrences of

        @return the number of times the String <B>substr</B> occurs in
        the string <B>str</B>. Throws a FatalAssertionException if
        <B>substr</B> is an empty string.
        @aribaapi documented
    */
    public static int occurs (String str, String substr)
    {
        int count = 0;
        int substrLen = substr.length();
        Assert.that(substrLen != 0, "Check substring argument.");
        int offset = str.indexOf(substr, 0);
        while (offset != -1) {
            count++;
            offset += substrLen;
            offset = str.indexOf(substr, offset);
        }
        return(count);
    }

    /**
        Count the number of occurrences of a string in an array of
        Strings.

        @param array an array of Strings to search in. There may not
        be null elements in the array.
        @param string the String to match against

        @return the number of times the String <B>string</B> occurs in
        the String array <B>array</B>.
        @aribaapi documented
    */
    public static int occurs (String[] array, String string)
    {
        int count = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(string)) {
                count++;
            }
        }
        return count;
    }

    /**
        Case insensitive string search for a substring.

        @param string the String to search in
        @param phrase the String to search for

        @return <b>true</b> if <B>phrase</B> is found anywhere within
        <B>string</B>, ignoring case; <b>false</b> otherwise. Returns
        true if <B>phrase</B> is empty.

        @aribaapi documented
    */
    public static boolean contains (String string, String phrase)
    {
        int stringLen = string.length();
        int subStringLen = phrase.length();
        for (int i = 0; i <= stringLen - subStringLen; i++) {
            if (string.regionMatches(true, i, phrase, 0, subStringLen)) {
                return true;
            }
        }
        return false;
    }

    /**
        Search for any of an array of substrings inside of the
        specified string.

        @param string the String to search in
        @param substrings the strings to search for

        @return the index of the first substring that is found inside
        the string or <code>-1</code> if the string is not found.

        @aribaapi private
    */
    public static int indexOf (String string, String[] substrings)
    {
        for (int i=0; i < substrings.length; i++) {
            if (string.indexOf(substrings[i]) != -1) {
                return i;
            }
        }
        return -1;
    }

    /**
        Replace all occurrences of a character in a String with another
        character. Replaces each occurrence of <B>marker</B> in the
        string <B>string</B> with the character <B>replace</B>.

        @param string the string to do do the replacing on.  If
        <code>null</code> or empty string, the string is simply
        returned.
        @param marker the character to remove
        @param replace the character to replace the removed character
        <b>marker</b> with

        @return a string with all occurrences of <b>marker</b> replaced
        by <b>replace</b>
        @aribaapi documented
    */
    public static String replaceCharByChar (
        String string, char marker, char replace)
    {
        if (nullOrEmptyString(string)) {
            return string;
        }
        return string.replace(marker, replace);
    }

    /**
        Replace all occurrences of a character in a String with a
        String. Replaces each occurrence of <B>marker</B> in the string
        <B>string</B> with the String <B>replace</B>.

        @param string the string to do do the replacing on.  If
        <code>null</code> or empty string, the string is simply
        returned.
        @param marker the character to remove
        @param replace the String that will replace the removed
        <b>marker</b>.  If an empty string or <code>null</code> is
        passed, any occurrences of the <b>marker</b> are removed from
        the initial <b>string</b>.

        @return a string with all occurrences of <b>marker</b> replaced
        by <b>replace</b>
        @aribaapi documented
    */
    public static String replaceCharByString (
        String string, char marker, String replace)
    {
        if (nullOrEmptyString(string)) {
            return string;
        }
        if (replace == null) {
            replace = "";
        }
        int replaceLength = replace.length();
        int stringLength = string.length();
        if (replaceLength == 1) {
            return (replaceCharByChar(string, marker, replace.charAt(0)));
        }
        int occurrences = StringUtil.occurs(string, marker);
        if (occurrences == 0) {
            return(string);
        }
        int newLength = stringLength + (occurrences * (replaceLength - 1));
        FastStringBuffer buf = new FastStringBuffer(newLength);

        int oldoffset = 0;
        int offset = string.indexOf(marker, 0);
        while (offset != -1) {
            buf.appendStringRange(string, oldoffset, offset);
            buf.append(replace);
            offset += 1;
            oldoffset = offset;
            offset = string.indexOf(marker, offset);
        }
        buf.appendStringRange(string, oldoffset, stringLength);

        return buf.toString();
    }

    /**
        Replace all repeating instances of a space in a String with a single
        space.  Replaces each repeating occurrence of the space character in
        the string <B>string</B> with exactly one space character.

        @param s the string to be compressed

        @return a string with all repeating occurrences of the space character
        replaced with a single space
        @aribaapi documented
    */
    public static String compressSpaces (String s)
    {
        char spaceChar = ' ';
        int offset = s.indexOf(spaceChar);
        if (offset == -1) {
            return s;
        }

        int stringLength = s.length();
        FastStringBuffer buf = new FastStringBuffer();
        char currChar;
        char prevChar = s.charAt(0);
        buf.append(prevChar);
        for (int i = 1; i < stringLength; i++) {
            currChar = s.charAt(i);
            if (!Character.isSpaceChar(prevChar) ||
                !Character.isSpaceChar(currChar)) {

                buf.append(currChar);
            }

            prevChar = currChar;
        }

        return buf.toString();
    }

    /**
        Compare a character array to a string without allocating a
        temporary string.

        @param chars the characters to check against the String
        @param s the string to compare to the characters

        @return true if the char[] array <B>chars</B> would be equal
        to the String <B>s</B> were the char[] array to be a String;
        <b>false</b> is returned otherwise
        @aribaapi documented
    */
    public static boolean charsEqualString (char[] chars, String s)
    {
        int charLen = chars.length;
        int stringLen = s.length();
        if (charLen != stringLen) {
            return false;
        }
            // go backwards in string since our strings seem to differ
            // more at the end than the front. Many begin with
            // "ariba.common"
        for (int i = stringLen-1; i >= 0; i--)
        {
            if (chars[i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluates if the first CharSequence ends with the second. If both
     * strings are null, then the substring is considered to be a substring
     * of the search string.
     * @param s The search space.
     * @param sub The substring to evaluate.
     * @return True when s ends with sub or both values are null.
     */
    public static boolean endsWith(CharSequence s, CharSequence sub) {
        // special-case nulls, (null, null) should be true
        if (null == s) {
            return null == sub;
        }
        else if (null == sub) {
            return false;
        }

        int l1 = s.length();
        int l2 = sub.length();
        // .toString required, because .equals on CharSequence doesn't
        // behave as expected
        return l1 >= l2 && s.subSequence(l1 - l2, l1).equals(
                sub.toString());
    }

    /**
     * Evaluate if hte first CharSequence ends with any of the following
     * CharSequences. The string null is considered to contain null.
     * @param s The search space.
     * @param subs The substrings to evaluate.
     * @return True when s ends with at least one sub or both values are null.
     */
    public static boolean endsWithAny(
            CharSequence s, CharSequence... subs)
    {
        if ((null != s && s == "") || ArrayUtil.nullOrEmptyArray(subs)) {
            return false;
        }
        for (CharSequence searchString : subs) {
            if (endsWith(s, searchString)) {
                return true;
            }
        }

        return false;
    }

    /**
        Tokenize a string separated by any separator characters into a
        List.

        Breaks up the specified string <B>s</B> into lines, and returns all
        lines in a vector. Blank lines may be optionally ignored.

        @param s the String to tokenize
        @param breakChars a string containing all characters to use as
        separator characters for the tokenize

        @return a List of the substrings, excluding the
        <b>breakChars</b>.
        @aribaapi documented
    */
    public static List stringToStringsListUsingBreakChars (
        String s, String breakChars)
    {
        List v = ListUtil.list();
        StringTokenizer st = new StringTokenizer(s, breakChars);
        while (st.hasMoreTokens()) {
            v.add(st.nextToken());
        }
        return v;
    }

    /**
        Returns the substring of <code>string</code> from
        <code>beginIndex</code> (inclusive) to <code>endIndex</code>,
        exclusive. <p/>

        This function is "safe" in the sense that it will bound
        <code>beginIndex</code> and <code>endIndex</code> appropriately so
        that no exceptions will be thrown. E.g. if <code>beginIndex</code> is
        less than zero, the zero index will be used for the substring instead.

        @param string the string to substring
        @param beginIndex the beginning index, inclusive
        @param endIndex the end index, exclusive
        @return the substring'ed string or <code>null</code> if
                <code>string</code> is <code>null</code>

        @aribaapi documented
    */
    public static String substring (String string, int beginIndex, int endIndex)
    {
        if (string == null) {
            return null;
        }
        int length = string.length();
        beginIndex = MathUtil.bound(beginIndex, 0, length);
        endIndex = MathUtil.bound(endIndex, 0, length);
        beginIndex = Math.min(beginIndex, endIndex);
        return string.substring(beginIndex, endIndex);
    }

    /**
        Returns this string truncated to a max of <code>length</code>
        characters. <p/>

        @param string the string to truncate
        @param length the length of the truncated string
        @return the truncated string or <code>null</code> if
                <code>string</code> is <code>null</code>

        @aribaapi documented
    */
    public static String truncate (String string, int length)
    {
        return substring(string, 0, length);
    }

    /**
        Returns the trailing part of <code>string</code> up to a max of
        <code>length</code> characters. <p/>

        @param string the string to get the trailing part of
        @param length the length of the truncated string
        @return the truncated string or <code>null</code> if
                <code>string</code> is <code>null</code>

        @aribaapi documented
    */
    public static String trailing (String string, int length)
    {
        if (string == null) {
            return null;
        }
        int len = string.length();
        return substring(string, len - length, len);
    }

    /**
        Joins array of elements with a string between each pair of
        elements. <p>

        @param objects an array of Objects each which will be toStringed.
        @param joinString a string to separate each pair of elements, if
               <code>null</code> the empty string is assumed

        @return a string, created by joining the contents
        of the object array, separated by the joinString
        @aribaapi documented
    */
    public static String join (Object[] objects, String joinString)
    {
        return objects!=null ? join(objects, joinString, 0, objects.length) : "";
    }

    /**
        Joins a sub-array of the given <code>objects</code> array elements
        with a string between each pair of elements.  The sub-array is
        specified by the <code>index</code> offset and the
        <code>length</code>.<p>

        Range checking is delegated to the native access provided when
        accessing array elements. <p>

        @param objects an array of Objects each which will be toStringed.
        @param joinString a string to separate each pair of elements, if
               <code>null</code> the empty string is assumed
        @param index the index within the array at which to begin the join of
               the elements; should be <code>>= 0</code> and
               <code>&lt;= objects.length</code>. (Note that if
               <code>index == objects.length</code>, <code>length</code> must equal
               0 in order that an exception will not be thrown.)
        @param length the number of elements within the array to join
               together; should satisfy
               <code>index + length &lt;= objects.length</code>
        @throws IndexOutOfBoundsException if bounds specified lead to access
                outside the bounds of the <code>objects</code> array

        @return a string, created by joining the contents
                of the object sub-array, separated by the joinString
        @aribaapi documented
    */
    public static String join (
            Object[] objects,
            String joinString,
            int index,
            int length
    )
    throws IndexOutOfBoundsException
    {
        if (joinString == null) {
            joinString = "";
        }

        String result = "";
        if (objects != null && length > 0) {
            FastStringBuffer buf = new FastStringBuffer();
            buf.append(objects[index]);
            int endIdx = index + length;
            for (int i = index + 1; i < endIdx; i++) {
                buf.append(joinString);
                buf.append(objects[i]);
            }
            result = buf.toString();
        }

        return result;
    }

    /**
        Joins vector of elements with a string between each pair of
        elements.

        @param vector a vector of Objects each which will be toStringed.
        @param joinString a string to separate each pair of elements.

        @return a string, created by joining the contents
        of the List, separated by the joinString
        @aribaapi documented
    */
    public static String fastJoin (List vector, String joinString)
    {
        FastStringBuffer buf = new FastStringBuffer();

        for (int i = 0, s = vector.size(); i < s; i++) {
            if (i != 0) {
                buf.append(joinString);
            }
            buf.append(vector.get(i));
        }
        return buf.toString();
    }

    /**
        Joins collection of elements with a string between each pair of
        elements.

        @param collection a collection of Objects each which will be toStringed.
        @param joinString a string to separate each pair of elements.

        @return a string, created by joining the contents
        of the List, separated by the joinString
        @aribaapi documented
    */
    public static String fastJoin (Collection collection, String joinString)
    {
        String result = "";
        if (!collection.isEmpty()) {
            Iterator iter = collection.iterator();
            FastStringBuffer buf = new FastStringBuffer();
            /* append the first object, is safe because size > 0 */
            buf.append(iter.next());
            while (iter.hasNext()) {
                /* append join string */
                buf.append(joinString);
                /* append next object */
                buf.append(iter.next());
            }
            result = buf.toString();
        }
        return result;
    }

    /**
        Joins vector of elements with a string between each pair of
        elements. This will invoke formatters to format each object.

        @param vector a vector of Objects each which will be formatted
        using the formatters.
        @param joinString a string to separate each pair of elements.

        @return a string, created by joining the contents
        of the vector, separated by the joinString.  The vector
        contents are formatted by the Formatter class.
        @aribaapi documented
    */
    public static String join (List vector, String joinString)
    {
        if (vector == null) {
            return "";
        }

        String[] array = new String[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            array[i] = Formatter.getStringValue(vector.get(i));
        }

        return join(array, joinString);
    }

    /**
        Check if a string is a legal java identifier.
        @param identifier The string to check

        @return true if identifier is legal, false otherwise.  If
        identifier is null or empty, it is considered invalid
        @aribaapi documented
    */
    public static boolean isJavaIdentifier (String identifier)
    {
        if (nullOrEmptyString(identifier)) {
            return false;
        }

        char[] chars = identifier.toCharArray();

        if (!Character.isJavaIdentifierStart(chars[0])) {
            return false;
        }

        for (int ii = 1; ii < chars.length; ii++) {
            if (!Character.isJavaIdentifierPart(chars[ii])) {
                return false;
            }
        }

        return true;
    }

    /**
        Check if a string starts with a specific substring in a case
        insensitive manner.  Similar to String.startsWith but ignores
        case.

        @param string the string to search in
        @param other the string to check for

        @return <b>true</b> if the first characters of <b>string</b>
        are equal to the string <b>other</b> in a case insensitive
        comparison; <b>false</b> otherwise. Returns <b>true</b> if
        <b>other</b> is an empty string.

        @see java.lang.String#startsWith
        @see java.lang.String#regionMatches(boolean, int, String, int, int)
        @aribaapi documented
    */
    public static boolean startsWithIgnoreCase (String string, String other)
    {
        return string.regionMatches(true, 0, other, 0, other.length());
    }

    /**
        Check if a String is composed entirely of digits using Character.isDigit

        @param string the String to check

        @return <b>true</b> if <b>string</b> is made entirely up of
        digits or is empty, <b>false</b> otherwise.

        @see java.lang.Character#isDigit
        @aribaapi documented
    */
    public static boolean isAllDigits (String string)
    {
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!Character.isDigit(chars[i])) {
                return false;
            }
        }
        return true;
    }

    /*
        BIT operations implemented in strings.  This is useful if you need
        more then 32 bits in a bit operations.  You might think it is only
        needed if you need more then 64 but since SQL Server doesn't do
        bit operations on numerics (only int's), you are limited to 32 if
        you plan on doing SQL operations.
    */

    /**
        Bit Set  - sets the bit with the given position in the given string
        Passing pos = 0 and value = "100" would yield "101".

        @aribaapi private
    */
    public static final String bitSet (String value, int pos)
    {
        return bitOper(value, pos, false);
    }

    /**
        Bit Clear  - clear the bit with the given position in the given string
        Passing pos = 0 and value = "101" would yield "100".

        @aribaapi private
    */
    public static final String bitClear (String value, int pos)
    {
        return bitOper(value, pos, true);
    }

    /**
        Bit Test  - test the bit for the given position in the given string
        Passing pos = 0 and value = "100" would yield false
        Passing pos = 1 and value = "100" would yield false
        Passing pos = 2 and value = "100" would yield true

        @aribaapi private
    */
    public static final boolean bitTest (String value, int pos)
    {
        int valueLength = value.length();
        if (pos >= valueLength) {
            return false;
        }
        int charPos = valueLength - pos - 1;
        char bit = value.charAt(charPos);
        return (bit == '1');
    }

    private static final String bitOper (String value, int pos, boolean clear)
    {
        FastStringBuffer fbuf = new FastStringBuffer();
        if (value == null || value.length() == 0) {
            if (clear) {
                return "";
            }
            fbuf.append('1');
            for (int i = 0; i < pos; i++) {
                fbuf.append('0');
            }
        }
        else {
            int len = value.length();
            int i = 0;
            int indx = 0;

            if (pos < len) {
                for (i = 0; i < len - (pos + 1); i++) {
                    fbuf.append(value.charAt(i));
                }
                if (clear) {
                    fbuf.append('0');
                }
                else {
                    fbuf.append('1');
                }
                indx = i + 1;
            }
            else {
                if (clear) {
                    return value;
                }
                else {
                    fbuf.append('1');
                }
                for (i = 0; i < pos - len; i++) {
                    fbuf.append('0');
                }
                indx = 0;
            }
            for (i = indx; i < len; i++) {
                fbuf.append(value.charAt(i));
            }
        }
        return fbuf.toString();
    }

    /**
        Create the appropriate bit string for use in a LIKE clause for
        a string bit mask.

        @aribaapi private
    */
    public static final String bit (int pint)
    {
        FastStringBuffer fbuf = new FastStringBuffer();
        fbuf.append('*');
        fbuf.append('1');
        for (int i = 0; i < pint; i++) {
            fbuf.append('_');
        }
        return fbuf.toString();
    }


    /*-- String Concatenation -----------------------------------------------*/

    private static final ThreadLocal MyThreadBuffer = new ThreadLocal();


    /*
        This method is not safe to call multiple times from the same
        thread if you are still holding on to the buffer (or
        recurisvly). Use Fmt's methods if you need that. This one
        should remain private and fast, with the guarantee that the
        buffer will always be released before being gotten again from
        the same thread.
    */
    private static FastStringBuffer buffer ()
    {
        FastStringBuffer buffer = (FastStringBuffer)MyThreadBuffer.get();
        if (buffer == null) {
            buffer = new FastStringBuffer(1024);
            MyThreadBuffer.set(buffer);
        }
        else {
                // should not be needed, but if toString has a
                // problem, or someone forgets to release the buffer
                // for some reason, we need to make sure new callers
                // get an empty one.
            buffer.truncateToLength(0);
        }
        return buffer;
    }

    private static String finalizeBuffer (FastStringBuffer buffer)
    {
        String string = buffer.toString();
            // truncate to 0 to free string, set capacity to trim char
            // array

            //ToDo - bounded memory leak introduced until HP
            //finds a solution.

        /*
        buffer.truncateToLength(0);
        if (buffer.getBuffer().length > 1024) {
            buffer.setCapacity(1024);
        }
        */
        return string;
    }

    public static String strcat (String s1, String s2)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String s1, String s2, String s3)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        buffer.append(s3);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String s1, String s2, String s3, String s4)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        buffer.append(s3);
        buffer.append(s4);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String s1, String s2, String s3, String s4, String s5)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        buffer.append(s3);
        buffer.append(s4);
        buffer.append(s5);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String s1, String s2, String s3, String s4, String s5,
                                 String s6)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        buffer.append(s3);
        buffer.append(s4);
        buffer.append(s5);
        buffer.append(s6);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String s1, String s2, String s3, String s4, String s5,
                                 String s6, String s7)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        buffer.append(s3);
        buffer.append(s4);
        buffer.append(s5);
        buffer.append(s6);
        buffer.append(s7);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String s1, String s2, String s3, String s4, String s5,
                                 String s6, String s7, String s8)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        buffer.append(s3);
        buffer.append(s4);
        buffer.append(s5);
        buffer.append(s6);
        buffer.append(s7);
        buffer.append(s8);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String s1, String s2, String s3, String s4, String s5,
                                 String s6, String s7, String s8, String s9)
    {
        FastStringBuffer buffer = buffer();
        buffer.append(s1);
        buffer.append(s2);
        buffer.append(s3);
        buffer.append(s4);
        buffer.append(s5);
        buffer.append(s6);
        buffer.append(s7);
        buffer.append(s8);
        buffer.append(s9);
        return finalizeBuffer(buffer);
    }

    public static String strcat (String[] elems)
    {
        Assert.that(elems != null,
                    "Cannot concatenate with null array");
        FastStringBuffer buffer = buffer();
        for (int i = 0; i < elems.length; i++) {
            buffer.append(elems[i]);            
        }
        return finalizeBuffer(buffer);
    }


    /**
        To remove duplicate elements from a String array <B>s</B>

        @param s a String array to unique

        @return a String array of unique elements
        @aribaapi documented
    */
    public static String[] removeDuplicates (String[] s)
    {
        return(removeDuplicates(s, null));
    }

    /**
        Removes the newlines and carriage returns from <code>string</code>
        and returns the resultant string. <p/>
        @param string the string to remove the carriage returns from
        @return the string with carriage returns removed
        @aribaapi ariba
    */
    public static String removeCarriageReturns (String string)
    {
        if (string == null) {
            return null;
        }
        return string.replaceAll("\\r\\n|\\n", " ");
    }

    /**
        To remove duplicate elements from a String array <B>s</B>

        @param s a String array to unique
        @param duplicateArray a list of duplicate array elements found
        in String array <B>s</B>

        @return a String array of unique elements
        @aribaapi documented
    */
    public static String[] removeDuplicates (String[] s, StringArray duplicateArray)
    {
        StringArray uniqueArray = new StringArray();
        // use hashtable for lookup for performance reasons and simultaneously
        // construct unique StringArray
        Map uniqueHT = MapUtil.map();
        Map duplicateHT = MapUtil.map();

        Assert.that(s != null, "removeDuplicates not allowed to be called with null");
        for (int i=0; i<s.length; i++) {
            if (!uniqueHT.containsKey(s[i])) {
                uniqueHT.put(s[i],s[i]);
                uniqueArray.add(s[i]);
            }
            else {
                if (duplicateArray != null) {
                    if (!duplicateHT.containsKey(s[i])) {
                        duplicateHT.put(s[i],s[i]);
                        duplicateArray.add(s[i]);
                    }
                }
            }
        }
        uniqueArray.trim();
        if (duplicateArray != null) {
            duplicateArray.trim();
        }

        return uniqueArray.array();
    }

    /**
        Removes the trailing slash from the given String. Note that
        only one trailing slash is removed. It is assumed that the
        given path contains at most one traling slash. Note that the
        slash is either forward or backward, based on the OS.

        @param path the given path String, must not be null.
        @return the string with the trailing slash removed, if any.

        @aribaapi documented
    */
    public static String removeTrailingSlashIfAny (String path)
    {
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length()-1);
        }
        return path;
    }

    /**
        Removes the leading slash from the given String. Note that
        only one leading slash is removed. It is assumed that the
        given path contains at most one leading slash. Note that the
        slash is either forward or backward, based on the OS.

        @param path the given path String, must not be null.
        @return the string with the leading slash removed, if any.

        @aribaapi documented
    */
    public static String removeLeadingSlashIfAny (String path)
    {
        if (path.startsWith(File.separator)) {
            path = path.substring(1, path.length());
        }
        return path;
    }

    private static final String DecryptPropertyName = "ariba.mask.secret";
    private static final String SecretValue = "off";
    /**
        @aribaapi ariba
    */
    public static String printSecret (String value)
    {
        if (SecretValue.equals(System.getProperty(DecryptPropertyName))) {
            return value;
        }
        else {
            return "********";
        }
    }

    /**
        Specifies whether the given character is a 7-ascii character
        @param c the character to test
        @return <code>true</code> if the character <code>c</code> is a 7-ascii
                character
        @aribaapi ariba
    */
    public static boolean is7bitAscii (char c)
    {
        return ((int)c) < 128;
    }

    /**
        Returns <code>true</code> if <code>ch</code> is a character which is
        considered to be a valid part of a URI part and <code>false</code> otherwise. <p>

        Specifically this returns <code>true</code> if it {@link #is7bitAscii} and
        any of the following is true:
        <ul>
        <li> <code>Character.isLetterOrDigit(ch)</code>
        <li> <code>ch == '-'</code>
        <li> <code>ch == '_'</code>
        <li> <code>ch == '.'</code>
        </ul>

        If <code>allowExtendedMarks</code> is true then this returns <code>true</code> if
        any of the following is true as well:
        <ul>
        <li> <code>ch == '!'</code>
        <li> <code>ch == '~'</code>
        <li> <code>ch == '*'</code>
        <li> <code>ch == '''</code>
        <li> <code>ch == '('</code>
        <li> <code>ch == ')'</code>
        </ul>

        @param ch the character to test
        @param allowExtendedMarks specifies whether uncommon marks are allowed as well
        @return <code>true</code> if the character can be used to constitute a URI reference
        @aribaapi private
    */
    private static boolean isValidCharForURIPart (char ch, boolean allowExtendedMarks)
    {
        return  is7bitAscii(ch) &&
            (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.' ||
             (allowExtendedMarks && 
              (ch == '!' || ch == '~' || ch == '*' ||
               ch == '\'' ||  ch == '(' || ch == ')')));
    }

    /**
        Specifies whether the given String is a valid URI part.

        According to the RFC 2396, a URI part is made of any alpha-numerical characters
        as well as some authorized marks ( '-' '_' '.' '!' '~' '*' ''' '(' ')' ).
        This returns <code>true</code> if all the characters part of the String follow the RFC
        speficication.</p>

        However because some of these marks can be troublesome, this method will return false
        if they are part of the String and <code>allowForExtendedMarks</code> is <code>false</code>.

        @param string the String to verify.
        @param allowForExtendedMarks if <code>false</code> the marks '!', '~', '*', ''', '(' and ')'
               will not be considered valid characters (although the RFC 2396 allows them.
        @return <code>true<code> when the String complies with RFC 2396
        @aribaapi ariba
    */
    public static boolean isValidURIPart (String string, boolean allowForExtendedMarks)
    {
        if (StringUtil.nullOrEmptyString(string)) {
            return false;
        }
        int length = string.length();
        for (int i=0; i<length; ++i)
        {
            if (!isValidCharForURIPart(string.charAt(i), allowForExtendedMarks)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Run a check on each character of the local part of an email address
     * for allowed valid characters
     * @param ch
     * @return boolean
     */ 
     private static boolean isValidCharForEmailLocalPart (char ch)
     {
         return is7bitAscii(ch) &&
             (Character.isLetterOrDigit(ch) || 
                ch == '!' || ch == '"' || ch == '#' || ch == '`' || ch == '=' ||
                ch == '$' || ch == '%' || ch == '&' || ch == '-' || ch == '~' ||
                ch == '*' || ch == '+' || ch == '/' || ch == '{' || ch == '\''||
                ch == '}' || ch == '|' || ch == '^' || ch == '_' || ch == '?' ||
                ch == '.' );
     }    
     
     /**
      * Validate the local part of an email address per RFC-822
      * @param string
      * @return boolean
      */
     public static boolean isValidEmailLocalPart (String string)
     {
         if (StringUtil.nullOrEmptyString(string)) {
             return false;
         }
         int length = string.length();
         for (int i=0; i<length; ++i)
         {
             if (!isValidCharForEmailLocalPart(string.charAt(i))) {
                 return false;
             }
         }
         return true;
     }   
    
    /**
        Splits a string up into chunks using a whitespace delimiter.
        Quoted items are treated as a single chunk.
        @param str the string to split
        @param keepQuotes if <b>true</b> keep the quotes that encapsulate an element
        @return a List of String containing each seperated element

        @aribaapi ariba
    */
    public static List splitWithQuotes (String str, boolean keepQuotes)
    {
        List result = ListUtil.list();

            //Define some constant char
        final char quote = '"';
        final char space = ' ';

        int pos = 0;
        int length = str.length();
        while (pos < length) {
            /*
                remove the leading spaces
            */
            while (pos < length && str.charAt(pos) == space) {
                pos++;
            }

            if (pos == length) {
                break;
            }

            /*
                start points to the first character of the next element
            */
            int start = pos;
                //test wether or not the string starts with a quote
            if (str.charAt(pos) == quote) {
                pos = str.indexOf(quote, pos + 1);
                while (pos > 0 && pos < length && str.charAt(pos - 1) == '\\') {
                    pos = str.indexOf(quote, pos + 1);
                }
                if (pos == -1 || pos >= length) {
                        //missing quote
                    throw new StringIndexOutOfBoundsException(
                        Fmt.S("quote missing in %s.", str.substring(start)));
                }
                if (keepQuotes) {
                    result.add(str.substring(start, ++pos));
                }
                else {
                    result.add(str.substring(++start, pos++));
                }
            }
            else {
                pos = str.indexOf(space, pos + 1);
                if (pos == -1) {
                        //we have reached the last element
                    pos = length;
                }
                result.add(str.substring(start, pos));
            }
        }
        return result;
    }

    /**
    Compares if two strings are equal or not. It is different from
    equalsIgnoreCase method of java.lang.String in the sense that
    it makes a null check of each of the Strings to be compared.

    @param one the String to be compared
    @param two the String to be compared

    @return true if both are null or equalsIgnoreCase of
    java.lang.String returns true. False if one of them are null
    or equalsIgnoreCase of java.lang.String returns false.

    @aribaapi documented
    */
    public static boolean equalsIgnoreCase (String one, String two)
    {
        if (one == null) {
            return two == null;
        }
        else if (two == null) {
            return one == null;
        }
        else return one.equalsIgnoreCase(two);
    }
    
    /**
     * Return the string argument with "<br>" tag inserted every wrapLength- characters.
     * Doesn't break a word, correctly handles punctuation and hyphenated words.
     * @param string
     * @param wrapLength
     * @return wrapped string
     * @aribaapi private
     */
    public static String wrapHTML (String string, int wrapLength)
    {
        return wrap(string, null, wrapLength);
    }
    
    /**
    * Return the string argument with the lineSeparator inserted every wrapLength- characters.
    * Doesn't break a word, correctly handles punctuation and hyphenated words.
    * <pre>
    * StringUtil.wrap("Hello World!, null, 9) = Hello<br>World!
    * StringUtil.wrap("1234512345", "<br>", 5) = 1234512345
    * StringUtil.wrap("12345", 10) = 12345
    *</pre>
    *
    * @param string The String
    * @param lineSeparator Line break, "<br>" by default
    * @param wrapLength The position to create a line break
    *
    * @return String
    */
    public static String wrap (String string, String lineSeparator, int wrapLength)
    {

        // Null or blank string should return an empty ("") string
        if (nullOrEmptyOrBlankString(string)) {
            return "";
        }

        int stringLength = string.length();

        if (stringLength > wrapLength) {
            Assert.that(wrapLength > 1 && wrapLength < Integer.MAX_VALUE,
                        "Invalid use of wrap, wrap length must be more than 1");

            // Default to HTML line break since web app is  client
            if (nullOrEmptyOrBlankString(lineSeparator)) {
                lineSeparator = "<br>";
            }

            StringBuffer sb = new StringBuffer(stringLength +
                    ((stringLength / wrapLength) * 2 * lineSeparator.length()));
            BreakIterator lineIterator = BreakIterator.getLineInstance();
            lineIterator.setText(string);
            int start = lineIterator.first();
            int lineStart = start;

            for (int end = lineIterator.next(); end != BreakIterator.DONE;
                        start = end, end = lineIterator.next())
            {
                if (end - lineStart < wrapLength) {
                    sb.append(string.substring(start, end));
                }
                else {
                    // wrap
                    if (true || end - start < wrapLength) {
                        sb.append(lineSeparator);
                        sb.append(string.substring(start, end));
                    }
                    lineStart = end;
                }
            }
            string = sb.toString();
        }

        return string;
    }

    // some convenience methods

    public static byte[] getBytesUTF8 (String string)
    {
        try {
            return string.getBytes(I18NUtil.EncodingUTF8);
        }
        catch (UnsupportedEncodingException e)
        {
            Assert.fail(e, "This VM is not worthy!");
            return null;
        }

    }

    public static String getStringUTF8 (byte[] bytes)
    {
        try {
            return new String(bytes, I18NUtil.EncodingUTF8);
        }
        catch (UnsupportedEncodingException e)
        {
            Assert.fail(e, "This VM is not worthy!");
            return null;
        }

    }

    /**
        The set of metacharacters
    */
    private static final String RegexMetaCharacters = "$?()*+.<>[\\]^{|}!=";

    /**
        The regex for matching metacharacters.
    */
    private static String EscapeRegexMetaCharactersRegex = null;

    static {
        EscapeRegexMetaCharactersRegex = RegexMetaCharacters.replaceAll("(.)", "\\\\$1");
        EscapeRegexMetaCharactersRegex = strcat("([", EscapeRegexMetaCharactersRegex, "])");
    }

    /**
        Escapes metacharacters in a regex into literals.
        @aribaapi private
    */
    public static String escapeRegEx (String regex)
    {
        return regex.replaceAll(EscapeRegexMetaCharactersRegex, "\\\\$1");
    }

    /**
        Small struct class that captures the result of search for a string
        within some other unidentified string or stream. <p/>
        @aribaapi ariba
    */
    public static class SearchResult
    {
        /**
            The string that was found.
        */
        public String found;
        /**
            The position at which the {@link #found} string was found.
        */
        public int index;

        /**
            Constructs a new instance.<p/>
        */
        public SearchResult (String found, int index)
        {
            this.found = found;
            this.index = index;
        }

        /**
            @aribaapi ariba
        */
        public String toString ()
        {
            return Fmt.S("found: %s, index: %s", found, Constants.getInteger(index));
        }
    }

    /**
        Searches for the first one of any of the supplied strings 
        @param toSearch the string to search
        @param fromIdx the index at which to begin the search
        @return the result of the search
        @aribaapi ariba
    */
    public static SearchResult search (
            String toSearch,
            int fromIdx,
            Collection/*<String>*/ toFind
    )
    {
        int result = Integer.MAX_VALUE;
        String found = null;
        for (Iterator iter = toFind.iterator(); iter.hasNext();) {
            String string = (String)iter.next();
            int foundIdx = toSearch.indexOf(string, fromIdx);
            if (foundIdx != -1 && result > foundIdx) {
                result = foundIdx;
                found = string;
            }
        }
        return found != null ? new SearchResult(found, result) : null;
    }

    /**
        Parse the input <code>String</code> into two components separated by
        matching left and right parentheses. Only one pair is supported.

        @return an array containing two elements, the first one is the
        first component before the left parenthesis and the second one is
        the component wrapped between the left and right perentheses
    */
    public static String[] parseMatchingParentheses (String str)
    {
        String[] components = new String[2];
        components[0] = null;
        components[1] = str;

        if (str != null) {
            int leftParenthesisPos = str.indexOf("(");
            int rightParenthesisPos = str.indexOf(")");
            boolean hasMatchingParenthesis =
                leftParenthesisPos >= 0 && rightParenthesisPos > leftParenthesisPos;

            if (hasMatchingParenthesis) {

                // we do have a function matched by "(" and ")"
                // Now get the true path inside the function
                components[0] = str.substring(0, leftParenthesisPos);
                components[1] =
                    str.substring(leftParenthesisPos + 1, rightParenthesisPos);
            }
        }

        return components;
    }

    private static final int MaxLengthPerWord           = 12;
    private static final float HalfLengthMultiplier     = 0.50f;
    private static final float CJKLengthMultiplier      = 0.65f;

    /**
     * Returns the ideal size to truncate based on the max number characters to display
     * by trying to fit the most words / characters before truncation.
     *
     * This method tries to truncate CJK characters as best as it can comparing to latin
     * characters.  Also it specifically truncates long words, such as in German, so that
     * they can be displayed on the browser inside a fixed with div without causing layout
     * problems.
     *
     * @param label
     * @param maxSize max ideal length before truncation
     * @param numLines number of lines to display the label in - only used when it's > 0
     * @return number of characters to truncate
     */
    public static int calcTruncateSize (String label, int maxSize, int numLines)
    {
        int numBytes = 0;
        try {
            numBytes = label.getBytes(I18NUtil.EncodingUTF8).length;
        }
        catch (UnsupportedEncodingException ex) {
            numBytes = label.getBytes().length;
        }
        /* Fix CR 1-AXYCVB: Don't get divide by zero when label is empty string. */
        int numChars = label.length();
        //1-CF76UN numBytes / numChars will always result in int value. The float value will be lost
        //In some cases (combination of latin and non-latin characters),
        // value of bytesPerChar may be a float between 1 and 2
        float bytesPerChar = (numChars < 1 ? 1 : (float)numBytes / numChars);

        int idealSize = 0;
        int halfMaxSize = Math.round(maxSize * HalfLengthMultiplier);
        if (bytesPerChar > 1) {
            // This is assumed to be CJK language - twice as wide as latin characters
            // when rendered in the browser - a little more than half of maxSize should
            // fit in the same area. [czheng 08/10/10]
            idealSize = Math.round(maxSize * CJKLengthMultiplier);
        }
        else if (numLines > 0) {
            // This part will try to fit as many words / characters as possible before
            // truncating them. It handles long words that are 12+ characters as well.
            int num = 0;
            int numLine = 0;
            for (String word : label.split("\\s+")) {
                if (num > 0) {
                    idealSize += num;
                    num = 0;
                }
                if (word.length() >= MaxLengthPerWord) {
                    idealSize += MaxLengthPerWord;
                    num = word.length() - MaxLengthPerWord;
                    numLine++;
                }
                else {
                    idealSize += word.length();
                }
                if (idealSize > maxSize || numLine >= numLines ||
                        word.length() > halfMaxSize) {
                    break;
                }
            }
            if (idealSize > maxSize) {
                idealSize = maxSize;
            }
        }
        else {
            idealSize = maxSize;
        }
        return idealSize;
    }

    /**
     * Returns the Base26 representation of the provided integer; where 0 = a,
     * 25 = z, 26 = ba, etc.
     * 
     * Right now, we don't support negative values. The reason for this is that
     * we anticipate that if someone ever wants to use this, they would use it for
     * generating a unique alphabetical-only string based on some incrementing 
     * number. It's not designed to fully support base 26 as a convenience for
     * everyone (if you want this just use Integer.toString(i, 26).) 
     * 
     * To get upper case letters, just toUpper() the result. 
     *
     * @param positive integer
     * @return String
     */
    public static String convertToBase26 (long value)
    {
        Assert.that(value > -1, "Negative values are not supported.");
        if (value < 26) {
            return Character.toString((char)('a' + value));
        }
        FastStringBuffer buf = new FastStringBuffer();
        do {
            int remainder = (int)(value % 26);
            buf.append((char)('a' + remainder));
            value = value / 26;
        }
        while (value > 0);
        FastStringBuffer result = new FastStringBuffer(buf.length());

        // now reverse the string
        for (int i = buf.length() - 1; i >= 0; --i) {
            result.append(buf.charAt(i));
        }
        return result.toString();
    }

    /**
     * Convenience method that converts a number in base 26 (as might have been
     * constructed by {@link #convertToBase26(long)}) back into an integer.
     * 
     * @param numberInBase26
     * @return
     */
    public static long convertFromBase26 (String numberInBase26)
    {
        long result = 0;
        for (int j = 0, length = numberInBase26.length(); j < length; ++j) {
            char c = numberInBase26.charAt(length - j - 1);
            result += ((long)(c - 'a')) * (long)Math.pow(26, j);
        }
        return result;
    }
}
