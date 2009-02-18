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

    $Id: //ariba/platform/util/core/ariba/util/formatter/IntegerFormatter.java#10 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;

/**
    <code>IntegerFormatter</code> is a subclass of <code>Formatter</code>
    which is responsible for formatting, parsing, and comparing
    <code>int</code> values and/or <code>Integer</code> objects.

    @aribaapi documented
*/
public class IntegerFormatter extends Formatter
{
    private static final String StringTable = "ariba.util.core";
    private static final String MaxValueExceededKey = "MaxValueExceeded";
    private static final String MinValueExceededKey = "MinValueExceeded";
    private static final String WholeNumberRequiredKey = "WholeNumberRequiredKey";
    private static final String NumberFormatErrorKey = "NumberFormatError";


    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.IntegerFormatter";

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    private static final IntegerFormatter factory = new IntegerFormatter();


    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>IntegerFormatter</code>.

        @aribaapi private
    */
    public IntegerFormatter ()
    {
    }


    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given <code>Integer</code> in the
        default locale.

        @param  object the <code>Integer</code> to format into a string
        @return        a string representation of the <code>Integer</code>
        @aribaapi documented
    */
    public static String getStringValue (Integer object)
    {
        return getStringValue(object, getDefaultLocale(), null, false);
    }

    /**
        Returns a formatted string for the given <code>Integer</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object the <code>Integer</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Integer</code>,
                       or empty string if the <code>Integer</code> is null
        @aribaapi documented
    */
    public static String getStringValue (Integer object, Locale locale)
    {
        return getStringValue(object, locale, null, false);
    }

    /**
        Returns a formatted string for the given <code>Integer</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object  the <code>Integer</code> to format into a string
        @param  locale  the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @return         a string representation of the <code>Integer</code>,
                        or empty string if the <code>Integer</code> is null
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (Integer object, Locale locale, String pattern)
    {
        return getStringValue(object, locale, pattern, false);
    }

    /**
        Returns a formatted string for the given <code>Integer</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object the <code>Integer</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return         a string representation of the <code>Integer</code>,
                        or empty string if the <code>Integer</code> is null
        @aribaapi documented
    */
    public static String getStringValue (
        Integer object,
        Locale  locale,
        boolean useGrouping)
    {
        return getStringValue(object, locale, null, useGrouping);
    }

    /**
        Returns a formatted string for the given <code>Integer</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object the <code>Integer</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return         a string representation of the <code>Integer</code>,
                        or empty string if the <code>Integer</code> is null
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        Integer object,
        Locale  locale,
        String  pattern,
        boolean useGrouping)
    {
        if (object == null) {
            return "";
        }

        return getStringValue(object.intValue(), locale, pattern, useGrouping);
    }

    /**
        Returns a formatted string for the given <code>int</code> value in the
        default locale.

        @param  integer the <code>int</code> value to format into a string
        @return         a string representation of the <code>int</code>
        @aribaapi documented
    */
    public static String getStringValue (int integer)
    {
        return getStringValue(integer, getDefaultLocale(), null, false);
    }

    /**
        Returns a formatted string for the given <code>int</code> value in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  integer the <code>int</code> value to format into a string
        @param  locale  the <code>Locale</code> to use for formatting
        @return         a string representation of the <code>int</code>
        @aribaapi documented
    */
    public static String getStringValue (int integer, Locale locale)
    {
        return getStringValue(integer, locale, null, false);
    }

    /**
        Returns a formatted string for the given <code>int</code> value in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  integer the <code>int</code> value to format into a string
        @param  locale  the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @return         a string representation of the <code>int</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (int integer, Locale locale, String pattern)
    {
        return getStringValue(integer, locale, pattern, false);
    }

    /**
        Returns a formatted string for the given <code>int</code> value in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  integer the <code>int</code> value to format into a string
        @param  locale  the <code>Locale</code> to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return         a string representation of the <code>int</code>
        @aribaapi documented
    */
    public static String getStringValue (int integer, Locale locale, boolean useGrouping)
    {
        return getStringValue(integer, locale, null, useGrouping);
    }

    /**
        Returns a formatted string for the given <code>int</code> value in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  integer the <code>int</code> value to format into a string
        @param  locale  the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return         a string representation of the <code>int</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        int     integer,
        Locale  locale,
        String  pattern,
        boolean useGrouping)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        DecimalFormat fmt = null;
        try
        {
            fmt = acquireDecimalFormat(locale, pattern);
            fmt.setGroupingUsed(useGrouping);
            return fmt.format((long)integer);
        }
        finally
        {
            releaseDecimalFormat(fmt, locale, pattern);
        }
    }


    /*-----------------------------------------------------------------------
        Static Parsing
      -----------------------------------------------------------------------*/

    /**
        Tries to parse the given string as an integer in the default locale.

        @param     string the string to parse as an <code>int</code>
        @return           an <code>int</code> value derived from the string
        @exception ParseException if the string cannot be parsed as an
                          <code>int</code>
        @aribaapi documented
    */
    public static int parseInt (String string)
      throws ParseException
    {
        return parseInt(string, getDefaultLocale());
    }

    /**
        Tries to parse the given string as an integer in the default locale.  If the
        parsed value is less than Integer.MIN_VALUE or greater than Integer.MAX_VALUE,
        throws ParseException.

        @param     string the string to parse as an <code>int</code>
        @return           an <code>int</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>int</code> or if the string causes an overflow / underflow
                          when parsed.
        @aribaapi documented
    */
    public static int parseIntNoOverflow (String string)
      throws ParseException
    {
        return parseIntNoOverflow(string, getDefaultLocale());
    }

    /**
        Tries to parse the given string as an integer in the specified locale.  If the
        parsed value is less than Integer.MIN_VALUE or greater than Integer.MAX_VALUE,
        throws ParseException.

        @param     string the string to parse as an <code>int</code>
        @param     locale the locale to use for parsing.
        @return           an <code>int</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>int</code> or if the string causes an overflow / underflow
                          when parsed.
        @aribaapi documented
    */
    public static int parseIntNoOverflow (String string, Locale locale)
      throws ParseException
    {
        return parseInt(string, locale, null, true, false);
    }

    /**
        Tries to parse the given string as an integer in the given
        <code>locale</code>.

        @param  string the string to parse as an <code>int</code>
        @param  locale the <code>Locale</code> to use for parsing
        @return        an <code>int</code> value derived from the string
        @exception     ParseException if the string cannot be parsed as an
                       <code>int</code>
        @aribaapi documented
    */
    public static int parseInt (String string, Locale locale)
      throws ParseException
    {
        return parseInt(string, locale, null, true);
    }

    /**
        Tries to parse the given string as an integer in the given
        <code>locale</code>.

        @param  string  the string to parse as an <code>int</code>
        @param  locale  the <code>Locale</code> to use for parsing
        @param  pattern the DecimalFormat pattern to use for parsing
        @param  useGrouping whether grouping characters should be used
        @return         an <code>int</code> value derived from the string
        @exception      ParseException if the string cannot be parsed as an
                        <code>int</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static int parseInt (String string, Locale locale, String pattern,
                                boolean useGrouping)
      throws ParseException
    {
        return parseInt(string, locale, pattern, useGrouping, true);
    }

    /**
        Tries to parse the given string as an integer in the given
        <code>locale</code>.

        @param  string  the string to parse as an <code>int</code>
        @param  locale  the <code>Locale</code> to use for parsing
        @param  pattern the DecimalFormat pattern to use for parsing
        @param  useGrouping whether grouping characters should be used
        @param  allowOverflow if false, then thows ParseException if value is &lt; Integer.MIN_VALUE or
                              Integer.MAX_VALUE &gt; val
        @return         an <code>int</code> value derived from the string
        @exception      ParseException if the string cannot be parsed as an
                        <code>int</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static int parseInt (String string, Locale locale, String pattern,
                                boolean useGrouping, boolean allowOverflow)
      throws ParseException
    {
        return parseInt(string, locale, pattern,
                        useGrouping, allowOverflow, false,
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
        Tries to parse the given string as an integer in the given
        <code>locale</code>.

        @param  string  the string to parse as an <code>int</code>
        @param  locale  the <code>Locale</code> to use for parsing
        @param  pattern the DecimalFormat pattern to use for parsing
        @param  useGrouping whether grouping characters should be used
        @param  allowOverflow if false, then thows ParseException if value is &lt; Integer.MIN_VALUE or
                              Integer.MAX_VALUE &gt; val
        @param  strictParsing if true, then throws ParseException if only a portion of the string was
                              evaluated to an integer. Example: "12 T" would evaluate to 12 if
                              strictParsing is false, but with the flag on, an exception would be thrown
        @param  minValue  the minimum valid integer value allowed
        @param  maxValue  the maximum valid integer value allowed
        @return         an <code>int</code> value derived from the string
        @exception      ParseException if the string cannot be parsed as an
                        <code>int</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static int parseInt (String string, Locale locale, String pattern,
                                boolean useGrouping, boolean allowOverflow,
                                boolean strictParsing,
                                int minValue,
                                int maxValue)
      throws ParseException
    {
        Assert.that(locale != null, "invalid null Locale");

        DecimalFormat fmt = null;
        try {
            fmt = acquireDecimalFormat(locale, pattern);
            fmt.setGroupingUsed(useGrouping);

            Number num = fmt.parse(string);
            if (!allowOverflow) {
                double val = num.doubleValue();
                if (val < minValue) {
                    throw new ParseException(
                                Fmt.Sil(StringTable, MinValueExceededKey,
                                        string, minValue),
                                0);
                }
                else if (maxValue < val) {
                    throw new ParseException(
                                Fmt.Sil(StringTable, MaxValueExceededKey,
                                        string, maxValue),
                                0);
                }
            }

            if (strictParsing) {
                ParsePosition pp = new ParsePosition(0);
                Number n = fmt.parse(string, pp);
                if (string != null && string.length() != pp.getIndex()) {
                    throw makeParseException(NumberFormatErrorKey, 0);
                }
            }
            //if (num.doubleValue() % 1 > 0) {
            //    throw new ParseException(
            //                Fmt.Sil(StringTable, WholeNumberRequiredKey, string), 0);
            //}

            return num.intValue();
        }
        catch (NumberFormatException e) {
            throw new ParseException(e.getMessage(), 0);
        }
        finally
        {
                releaseDecimalFormat(fmt,locale, pattern);
        }
    }

    /**
        Tries to parse the given character array as an <code>int</code> in the
        default locale.  The <code>offset</code> and <code>scan</code>
        parameters determine where in the character array to begin parsing,
        and how many characters to use.

        @param     chars  the character array to parse as an <code>int</code>
        @param     offset the index of the first character in the array to use
        @param     scan   the number of characters from the array to use
        @return           an <code>int</code> value derived from the array
        @exception        ParseException if the character array cannot be parsed
                          as an <code>int</code>
        @aribaapi documented
    */
    public static int parseInt (char[] chars, int offset, int scan)
      throws ParseException
    {
        String string = new String(chars, offset, scan);
        return parseInt(string);
    }

    /**
        Tries to parse the given string as an <code>int</code> in the
        specified <code>base</code>.

        @param     string the string to parse as an <code>int</code>
        @param     base   the numeric base to use for parsing
        @return           an <code>int</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>int</code> in the specified base
        @aribaapi documented
    */
    public static int parseInt (String string, int base)
      throws ParseException
    {
        try {
            return Integer.parseInt(string, base);
        }
        catch (NumberFormatException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /**
        Tries to parse the given character array as an <code>int</code> in the
        given <code>base</code>.  The <code>offset</code> and
        <code>scan</code> parameters determine where in the character array to
        begin parsing, and how many characters to use.

        @param     chars  the character array to parse as an <code>int</code>
        @param     offset the index of the first character in the array to use
        @param     scan   the number of characters from the array to use
        @param     base   the numeric base to use for parsing
        @return           an <code>int</code> value derived from the array
        @exception        ParseException if the character array cannot be parsed
                          as an <code>int</code> in the specified base
        @aribaapi documented
    */
    public static int parseInt (char[] chars, int offset, int scan, int base)
      throws ParseException
    {
        String string = new String(chars, offset, scan);
        return parseInt(string, base);
    }

    /**
        Convenience method for parsing a string into an integer in the
        given base.  Uses the IntegerFormatter class to parse the
        string. If there is an exception while parsing the string, the
        specified default value is returned.

        @param string a string representation of an Integer
        @param defaultValue the value to return if the string can not
        be parsed

        @return an int represented by the specified string if the
        string could be parsed, <b>defaultValue</b> otherwise

        @see ariba.util.formatter.IntegerFormatter#parseInt(String)
        @aribaapi private
    */
    public static final int parseIntWithDefault (String string, int defaultValue)
    {
        try {
            return parseInt(string);
        }
        catch (ParseException e) {
            return defaultValue;
        }
    }

    /**
        Returns an <code>int</code> value derived from the given object in the
        default locale.  If the object is not an <code>Integer</code>, it is
        converted to a string and parsed.  Returns zero if the object can't be
        converted to an <code>int</code>.

        @param  object the object to convert to an <code>int</code> value
        @return        an <code>int</code> derived from the given object
        @aribaapi documented
    */
    public static int getIntValue (Object object)
    {
        if (object == null) {
            return 0;
        }
        else if (object instanceof Integer) {
            return ((Integer)object).intValue();
        }
        else {
            try {
                return parseInt(object.toString());
            }
            catch (ParseException e) {
                return 0;
            }
        }
    }


    /*-----------------------------------------------------------------------
        Static Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two <code>Integer</code> objects for sorting purposes.
        Returns an <code>int</code> value which is less than, equal to, or
        greater than zero depending on whether the first object sorts before,
        the same, or after the second object.

        @param  i1 the first <code>Integer</code> to compare
        @param  i2 the second <code>Integer</code> to compare
        @return    <code>int</code> value which determines how the two objects
                   should be ordered
        @aribaapi documented
    */
    public static int compareIntegers (Integer i1, Integer i2)
    {
        if (i1 == i2) {
            return 0;
        }
        else if (i1 == null) {
            return -1;
        }
        else if (i2 == null) {
            return 1;
        }
        else {
            return compareInts(i1.intValue(), i2.intValue());
        }
    }

    /**
        Compares two <code>int</code> values for sorting purposes.  Returns an
        <code>int</code> value which is less than, equal to, or greater than
        zero depending on whether the first value sorts before, the same, or
        after the second value.

        @param  i1 the first <code>int</code> value to compare
        @param  i2 the second <code>int</code> value to compare
        @return    <code>int</code> value which determines how the two values
                   should be ordered
        @aribaapi documented
    */
    public static int compareInts (int i1, int i2)
    {
            // return the difference
        return (i1 - i2);
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the given
        locale.  The object must be a non-null <code>Integer</code>.

        @param  value the <code>Integer</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Integer</code>
        @aribaapi documented
    */
    protected String formatObject (Object value, Locale locale)
    {
        Assert.that(value instanceof Integer, "invalid type");
        return getStringValue((Integer)value, locale);
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Tries to parse the given string into an <code>Integer</code> object in
        the given locale.  The string is assumed to be non-null and trimmed of
        leading and trailing whitespace.  The <code>locale</code> parameter is
        currently unused.

        @param     string the string to parse
        @param     locale the <code>Locale</code> to use for parsing (unused)
        @return           an <code>Integer</code> object derived from the string
        @exception        ParseException if the string can't be parsed as an
                          <code>Integer</code> object in the given locale
        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
      throws ParseException
    {
        return Constants.getInteger(parseInt(string));
    }

    /**
        Returns a new <code>Integer</code> derived from the given object in
        the given locale.  If the object is not an <code>Integer</code>, it is
        converted to a string and parsed.  Returns null if the object can't be
        parsed as a <code>Integer</code>.  The <code>locale</code> parameter
        is currently unused.

        @param  object the object to convert to an <code>Integer</code>
        @param  locale the <code>Locale</code> to use for conversion (unused)
        @return        an <code>Integer</code> derived from the given object
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        return Constants.getInteger(getIntValue(object));
    }


    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two objects for sorting purposes in the given locale.  The
        two objects must be non-null <code>Integer</code> objects.  Returns a
        value which is less than, equal to, or greater than zero depending on
        whether the first object sorts before, the same, or after the second
        object.  The <code>locale</code> parameter is currently unused.

        @param  o1     the first <code>Integer</code> to compare
        @param  o2     the second <code>Integer</code> to compare
        @param  locale the <code>Locale</code> to use for comparison (unused)
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi documented
    */
    public int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof Integer, "invalid type");
        Assert.that(o2 instanceof Integer, "invalid type");

        return compareIntegers((Integer)o1, (Integer)o2);
    }


    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/

    /*-----------------------------------------------------------------------
        Caching
      -----------------------------------------------------------------------*/
    protected Format instantiateFormat (int type, Locale locale, String pattern)
    {
        pattern = (pattern == null) ? "" : pattern;
        DecimalFormat fmt = (DecimalFormat)NumberFormat.getNumberInstance(locale);
        if (!StringUtil.nullOrEmptyString(pattern)) {
            fmt.applyLocalizedPattern(pattern);
        }

        // set this to false so we can detect fractional values and throw exception
        // see parseInt()
        fmt.setParseIntegerOnly(false);

        // Work around for Java DecimalFormat issue with French formatted integers
        // which use ' ' for the grouping separator.  Java uses a non-breaking space
        // (0xA0) by default but the issue is that all browser entered values will use
        // a breaking space (0x20).  Until this is fixed in Java, we'll replace any
        // occurrence of 0xA0 with 0x20.
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6318800
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4510618
        DecimalFormatSymbols dfs = fmt.getDecimalFormatSymbols();
        if (dfs.getGroupingSeparator() == 0xA0) {
            dfs.setGroupingSeparator(' ');
            fmt.setDecimalFormatSymbols(dfs);
        }
        return fmt;
    }

    private static DecimalFormat acquireDecimalFormat (Locale locale, String pattern)
    {
        return (DecimalFormat)factory.acquireFormat(
                    Formatter.IntegerFormatterType, locale, pattern);
    }

    private static void releaseDecimalFormat (
        DecimalFormat df,Locale locale, String pattern)
    {
        factory.releaseFormat(df,Formatter.IntegerFormatterType,locale,pattern);
    }
}
