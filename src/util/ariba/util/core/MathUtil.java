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

    $Id: //ariba/platform/util/core/ariba/util/core/MathUtil.java#11 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.lang.Character; // OK for javadoc bug
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

/**
    Math Utilities. These are helper functions for dealing with
    math.

    @aribaapi documented
*/
public final class MathUtil
{
	private static final long Offset = power2(32);
	
    /* prevent people from creating instances of this class */
    private MathUtil ()
    {
    }

    /**
        Calculate the log (base 2) of a specified long. This can be
        useful for calculating a bit index from a bit mask.

        @param x a long to calculate the log of

        @return floor(lg(<b>x</b>)), that is, the greatest integer
        less than or equal to log base 2 of <b>x</b>.
        @aribaapi documented
    */
    public static int log2 (long x)
    {
        Assert.that(x > 0, "long x must be greater than 0");
        int lg = -1;
        for (int i = 1; i <= x; i += i) {
            lg++;
        }
        return lg;
    }

    /**
        Calculate 2 raised to a specified power.

        @param x the power to raise 2 to, must be greater or equal to 0

        @return 2 raised to the power of <b>x</b>.
        @aribaapi documented
    */
    public static long power2 (int x)
    {
        Assert.that(x >= 0,
                      "power2 currently requires integers greater or equal to 0");
        long p = 1;
        for (int i = 1; i <= x; i++)
            p += p;
        return p;
    }

    /**
        Returns the specified number with its least significant bit
        zeroed.

        @param n an integer to make even

        @return the specified number with its least significant bit
        zeroed
        @aribaapi documented
    */
    public static int forceEven (int n)
    {
        return (n & 0xfffffffe);
    }

    /**
        Calculate 10 raised to a specified power.

        @param power the power to raise 10 to

        @return 10 raised to the power of <b>power</b>.
        @aribaapi documented
    */
    public static double powerOfTen (int power)
    {
        Assert.that(power >= 0,
                      "power2 currently requires integers greater or equal to 0");
        double value = 10.0;
        if (power == 0) {
            return 1.0;
        }
        for (int i = 1; i < power; i++) {
            value *= 10.0;
        }
        return value;
    }

    /**
        Returns the integer result of (d mod 10).  If round is true, then
        result will be rounded, otherwise, the resulted will be the greatest
        integer at or below the result.

        @param d a double to mod against 10
        @param round if <b>true</b> the double will be rounded to the
        nearest long, if <b>false</b> floor() will be called.

        @return an integer between 0 and 9 of the mod result
        @aribaapi documented
    */
    public static int modulo10 (double d, boolean round)
    {
        long l = round ? Math.round(d) : (long)Math.floor(d);
        int i = (int)(l % 10);
        return i;
    }

    /**
        Returns the specified number with its least significant bit
        set.

        @param n an integer to make odd

        @return the specified number with its least significant bit
        set
        @aribaapi documented
    */
    public static int forceOdd (int n)
    {
        return (n | 0x1);
    }


    /**
        Calculate the greatest common denominator for two longs.

        @param a the first number in long
        @param b the sencond number in long

        @return long value that is the greatest common denominator of the two.
           0 if either of the two numbers is 0 or negative.
        @aribaapi documented
    */
    public static long getGCD (long a,  long b)
    {
        long small = Math.min(a, b);
        long large = Math.max(a, b);
        long temp;

        if (small <= 0) {
            return 1;
        }

        temp = large % small;
        while (temp > 0) {
            large = small;
            small = temp;
            temp = large % small;
        }
        return small;
    }



    /**
        Calculate the least common multiple for two longs.

        @param a the first number in long
        @param b the sencond number in long

        @return long value that is the least common multiple of the two.
           0 if either of the two numbers is 0 or negative.
        @aribaapi documented
    */
    public static long getLCM (long a,  long b)
    {
        long lcd = getGCD(a, b);
        return (a * b / lcd);
    }


    /**
        Adjust the scale of the given BigDecimal to the specified scale.
        @param value the BigDecimal
        @param scale the scale, which can't be negative.
    */
    public static BigDecimal setScale (BigDecimal value, int scale)
    {
        Assert.that(scale >= 0, "Got a negative scale %s",
            String.valueOf(scale));

        try {
           value = value.setScale(scale, BigDecimal.ROUND_HALF_UP);
        }
        catch (ArithmeticException e) {
            Log.util.debug("Should never happen %s", e);
        }
        catch (IllegalArgumentException e) {
            Log.util.debug("Should never happen %s", e);
        }
        return value;
    }

    public static Double setScale (Double value, int scale)
    {
        Assert.that(scale >= 0, "Got a negative scale %s",
            String.valueOf(scale));

        BigDecimal temp = new BigDecimal(value.doubleValue());
        temp = setScale(temp, scale);

        return new Double(temp.doubleValue());
    }

    /*-- Base 64 Conversion -------------------------------------------------*/

    private static final int AlphabetSize = 26;
    private static final int AlphabetSizeTimesTwo = 52;
    private static final int Base64 = 64;

    private static final char base64ConvertTable[] =
    {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
     'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
     'U', 'V', 'W', 'X', 'Y', 'Z',
     'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
     'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
     'u', 'v', 'w', 'x', 'y', 'z',
     '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
     '+', '!'
    };

    /**
        Converts the given long into a base 64 string.

        @param num the number to convert into base 64

        @return a String representation of <b>num</b> in base 64
        @aribaapi documented
    */
    public static String toBase64 (long num)
    {
        Assert.that(num >= 0,
                    "the long passed into MathUtil.toBase64 must be positive.");

            // Long.MAX_VALUE can more than fit in array of size 16.
        char [] buffer = new char[16];
        int offset = toBase64(num, buffer, 15);
        return new String(buffer, offset+1, 15-offset);
    }

    /**
        Converts the given long into a base 64 string. The result is
        stored in the character array <B>chr</B>, starting with the
        character at <B>off</B>, counted from the end of the array.

        @param num the number to convert into base 64
        @param chr the character array to insert the base 64
        representation of the number into
        @param off the offset into the character array to start writing

        @return the index of the character preceding the last
        character written into the buffer.
        @aribaapi documented
    */
    public static int toBase64 (long num, char[] chr, int off)
    {
        if (num == 0) {
            chr[off] = 'A';
            off--;
        }
        else {
            for (; num > 0; off--) {
                chr[off] = base64ConvertTable[(int)(num % 64)];
                num = num / Base64;
            }
        }
        return off;
    }

    /**
        Convert a string representation of a base64 number into a
        long.

        @param s a String representation of a base 64 number

        @return long the number as a long; -1 in case the string s contains
        a character not present in the base64 character set

        @aribaapi documented
    */
    public static long fromBase64 (String s)
    {
        return fromBase64(s, 0, s.length() - 1);
    }

    /**
        Convert a string representation of a base64 number into a
        long.

        @param s a String containing a representation of a base 64
        number
        @param offset the offset of the first character in the string
        to start begin parsing at.

        @return long the number as a long; -1 in case the string s contains
        a character not present in the base64 character set

        @aribaapi documented
    */
    public static long fromBase64 (String s, int offset)
    {
        return fromBase64(s, offset, s.length() - 1);
    }

    /**
        Convert a string representation of a base64 number into a
        long.

        @param s a String containing a representation of a base 64
        number
        @param beginIndex the offset of the first character in the string
        to parse (inclusive).
        @param endIndex the offset of the last character in the string
        to parse (inclusive).

        @return long the number as a long; -1 in case the string s contains
        a character not present in the base64 character set

        @aribaapi documented
    */
    public static long fromBase64 (String s, int beginIndex, int endIndex)
    {
        long sum = 0;
        for (int i = beginIndex; i <= endIndex; i++) {
            char c = s.charAt(i);
            int digit;
            if (c >= 'A' && c <= 'Z') {
                digit = c - 'A';
            }
            else if (c >= 'a' && c <= 'z') {
                digit = c - 'a' + AlphabetSize;
            }
            else if (c >= '0' && c <= '9') {
                digit = c - '0' + AlphabetSizeTimesTwo;
            }
            else if (c == '+') {
                digit = 62;
            }
            else if (c == '!') {
                digit = 63;
            }
            else {
                return -1;
            }
            sum = (sum * Base64) + digit;
        }

        return sum;
    }

    /*-- Base36 Conversion -------------------------------------------------*/

    /**
        Converts the given long into a base 36 string.

        @param num the number to convert into base 36

        @return a String representation of <b>num</b> in base 36
        @aribaapi documented
    */
    public static String toBase36 (long num)
    {
        Assert.that(num >= 0,
                    "the long passed MathUtil.toBase36 must be positive.");
        char [] buffer = new char[32];
        int offset = toBase36(num, buffer, 31);
        return new String(buffer, offset+1, 31-offset);
    }

    /**
        Converts the given long into a base 36 string. The result is
        stored in the character array <B>chr</B>, starting with the
        character at <B>off</B>, counted from the end of the array.

        @param num the number to convert into base 36
        @param chr the character array to insert the base 36
        representation of the number into
        @param off the offset into the character array to start writing

        @return the index of the character preceding the last
        character written into the buffer.
        @aribaapi documented
    */
    public static int toBase36 (long num, char[] chr, int off)
    {
        if (num == 0) {
            chr[off] = '0';
            off--;
        }
        else {
            for (; num > 0; off--) {
                chr[off] = base36ConvertTable[(int)(num % 36)];
                num = num / 36;
            }
        }
        return off;
    }

    private static final char base36ConvertTable[] =
    {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
     'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
     'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
     'u', 'v', 'w', 'x', 'y', 'z'};

    /**
        Parse starting with the offset-th character, until reaching an
        invalid character.
    */
    private static final int AlphaDigitOffset = 'a' - 10;
    private static final int Base36 = 36;

    /**
        Convert a string representation of a base36 number into a
        long.

        @param s a String representation of a base 36 number

        @return long the number as a long

        @exception ParseException if the String <b>s</b> could not be
        parsed
        @aribaapi documented
    */
    public static long fromBase36 (String s) throws ParseException
    {
        for (int i = 0, l = s.length(); i < l ; i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') ||
                  (c >= 'a' && c <= 'z')))
            {
                throw new ParseException(
                    Fmt.S("%s is not a valid Base 36 digit in %s",
                          new Character(c), s),
                    i);
            }
        }
        return fromBase36(s, 0);
    }

    /**
        Convert a string representation of a base36 number into a
        long. Parsing stops at the first character that can not be
        part of a base 36 number.

        @param s a String containing a representation of a base 36
        number
        @param offset the offset of the first character in the string
        to start begin parsing at.

        @return long the number as a long
        @aribaapi documented
    */
    public static long fromBase36 (String s, int offset)
    {
        int len = s.length();
        long sum = 0;
        for (int i = offset; i < len; i++) {
            char c = s.charAt(i);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            }
            else if (c >= 'a' && c <= 'z') {
                digit = c - AlphaDigitOffset;
            }
            else {
                break;
            }
            sum = (sum * Base36) + digit;
        }

        return sum;
    }

        // 2-D hashtable cache of permutation sets
    private static final Gridtable PermsCache = new Gridtable();

    /**
        Returns a List of Lists, where each sub-List is a list of
        Integers representing one permutation of <b>r</b> numbers between 0
        and <b>n</b>.

        @param n upper limit of the range within which the members of the
        permutation fall
        @param r count of integers in the permutation
        @return List of Lists, where each sub-List is a list of
        Integers representing one permutation
        @aribaapi documented
    */
    public static List permutations (int n, int r)
    {
            // check cache first
        List perms =
            (List)PermsCache.get(Constants.getInteger(n), Constants.getInteger(r));
        if (perms == null) {
            Object[] alphabet = new Object[n+1];
            for (int i = 0; i < n; i++) {
                alphabet[i] = Constants.getInteger(i);
            }
            perms = permutations(n, r, alphabet, 0, r);
            PermsCache.put(Constants.getInteger(n), Constants.getInteger(r), perms);
        }

        return perms;
    }

    /*
        Implements a P(n,r) permutations algorithm.  The alphabet <b>a</b> can
        be an array of arbitrary objects, e.g. Integers, Strings, Characters,
        etc.
    */
    private static List permutations (int n, int r, Object[] a, int s, int max)
    {
        Assert.that((r <= n), "MathUtil.permutations: r must be <= n");

        List perms = ListUtil.list();

        if (r == 1) {
            for (int i = 0; i < n; i++) {
                Object[] perm = new Object[max];
                perm[0] = a[i+s];
                perms.add(perm);
            }
        }

        // iterate over all n items in the alphabet
        // append each to the permutations of the rest
        else {

            for (int i = 0; i < n; i++) {

                // take the first element as our prefix
                Object prefix = a[s];

                // calculate all the perms of the rest of the alphabet
                List subs = permutations(n-1, r-1, a, s+1, max);

                // for each sub-perm, tack on the suffix element
                for (int j = 0; j < subs.size(); j++) {
                    Object[] sub = (Object[])subs.get(j);
                    sub[r-1] = prefix;
                }

                // save this set of permutations
                perms.addAll(subs);

                // swap the first element with the next prefix
                a[s] = a[s+i+1];
                a[s+i+1] = prefix;
            }

            // alphabet is shifted right one now, shift it back
            System.arraycopy(a, s+1, a, s, n);
            a[a.length-1] = null;
        }

        return perms;
    }

    /**
        Returns the value of the <code>sgn</code> mathematical function (the
        signum function) on the operands <code>a</code> and <code>b</code>.<p>

        The signum function returns <code>-1</code> if <code>a < b</code>,
        0 if <code>a == b</code> and <code>+1</code> if <code>a > b</code>.
        It serves as a safe way to determine how two integers compare.
        Simply subtracting the integers and comparing the result with
        zero can fail for numbers close to <code>Integer.MAX_VALUE</code>
        and <code>Integer.MIN_VALUE</code>.

        @return the value of the <code>sgn</code> function on the supplied
                operands
        @aribaapi ariba
    */
    public static int sgn (int a, int b)
    {
        return a < b ? -1 : (a == b ? 0 : +1);
    }

    /**
        Returns the value of the <code>sgn</code> mathematical function (the
        signum function) on the operands <code>a</code> and <code>b</code>.<p>

        The signum function returns <code>-1</code> if <code>a < b</code>,
        0 if <code>a == b</code> and <code>+1</code> if <code>a > b</code>.
        It serves as a safe way to determine how two longs compare.
        Simply subtracting the numbers and comparing the result with
        zero can fail for numbers close to <code>Long.MAX_VALUE</code>
        and <code>Long.MIN_VALUE</code>.

        @return the value of the <code>sgn</code> function on the supplied
                operands
        @aribaapi ariba
    */
    public static int sgn (long a, long b)
    {
        return a < b ? -1 : (a == b ? 0 : +1);
    }

    /**
        Bounds <code>value</code> to be within <code>lower</code> and
        <code>upper</code>, inclusively. <p/>

        That is if <code>value &lt; lower</code> then
        <code>lower</code> is returned and if <code>value > upper</code>
        then <code>upper</code> is returned, else <code>value</code> itself
        is returned. <p/>

        Note that if <code>upper</code> is less than <code>lower</code>, this
        function returns as if upper is positive infinity. <p/>

        @param value the value to be bounded
        @param lower the lower bound
        @param upper the upper bound
        @return the bounded value

        @aribaapi documented
    */
    public static int bound (int value, int lower, int upper)
    {
        return value < lower ? lower : (value > upper ? upper : value);
    }
    
    /**
	    Private method that calculates the hash code for the char[]
	    <b>val</b> according to the sun's unique implementation which
	    they say will be standard in 1.2 (love those standards)
    */
	public static final int sunHashCode (char[] val)
	{
	    int i1 = 0;
	    int j = 0;
	    char ach[] = val;
	    int k = val.length;
	    for (int i2 = 0; i2 < k; i2++)
	        i1 = 31 * i1 + ach[j++];
	    return i1;
	}
    
    /**
        Private method that calculates the hash code for the char[]
        <b>val</b> according to the java spec.
    */
	public static final int normalHashCode (char[] val)
	{
	    int h = 0;
	    int off = 0;
	    int len = val.length;
	
	    if (len < 16) {
	        for (int i = len ; i > 0; i--) {
	            h = (h * 37) + val[off++];
	        }
	    }
	    else {
	            // only sample some characters
	        int skip = len / 8;
	        for (int i = len ; i > 0; i -= skip, off += skip) {
	            h = (h * 39) + val[off];
	        }
	    }
	    return h;
	}
	
	/**
     * @param s
     * @return int
     * <br> A convenience method to return an unsigned hash for a given 
     * String. If a hash code generated is negative, it is offset by 
     * adding <code>2 ^ 32</code> 
     */
    public static final long unsignedUniqueId (String s)
    {
    	int jdkHash = sunHashCode(s.toCharArray());
    	long unsignedId = jdkHash < 0 ? jdkHash + Offset :
    		jdkHash;
    	return unsignedId;
    }

}
