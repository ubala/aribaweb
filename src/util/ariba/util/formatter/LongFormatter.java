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

    $Id: //ariba/platform/util/core/ariba/util/formatter/LongFormatter.java#4 $
*/

package ariba.util.formatter;

import ariba.util.core.Constants;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;

/**
    <code>LongFormatter</code> is a subclass of <code>Formatter</code> which
    is responsible for formatting, parsing, and comparing <code>long</code>
    values and/or <code>Long</code> objects.

    @aribaapi documented
*/
public class LongFormatter extends Formatter
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.LongFormatter";

    
    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>LongFormatter</code>.

        @aribaapi private
    */
    public LongFormatter ()
    {
    }
    

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    private static final LongFormatter factory = new LongFormatter();


    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given <code>Long</code> in the
        default locale.

        @param  object the <code>Long</code> to format into a string
        @return        a string representation of the <code>Long</code>
        @aribaapi documented
    */
    public static String getStringValue (Long object)
    {
        return getStringValue(object, getDefaultLocale(), null, false);
    }

    /**
        Returns a formatted string for the given <code>Long</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object the <code>Long</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Long</code>,
                       or empty string if the <code>Long</code> is null
        @aribaapi documented
    */
    public static String getStringValue (Long object, Locale locale)
    {
        return getStringValue(object, locale, null, false);
    }
    
    /**
        Returns a formatted string for the given <code>Long</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object  the <code>Long</code> to format into a string
        @param  locale  the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @return         a string representation of the <code>Long</code>,
                        or empty string if the <code>Long</code> is null
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (Long object, Locale locale, String pattern)
    {
        return getStringValue(object, locale, pattern, false);
    }
    
    /**
        Returns a formatted string for the given <code>Long</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object the <code>Long</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return        a string representation of the <code>Long</code>,
                       or empty string if the <code>Long</code> is null
        @aribaapi documented
    */
    public static String getStringValue (Long object, Locale locale, boolean useGrouping)
    {
        return getStringValue(object, locale, null, useGrouping);
    }
    
    /**
        Returns a formatted string for the given <code>Long</code> in the
        given locale.  The <code>locale</code> parameter must be non-null.

        @param  object the <code>Long</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return        a string representation of the <code>Long</code>,
                       or empty string if the <code>Long</code> is null
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        Long    object,
        Locale  locale,
        String  pattern,
        boolean useGrouping)
    {
        if (object == null) {
            return "";
        }

        return getStringValue(object.longValue(), locale, pattern, useGrouping);
    }

    /**
        Returns a formatted string for the given <code>long</code> value in
        the default locale.

        @param  value the <code>long</code> value to format into a string
        @return       a string representation of the <code>long</code>
        @aribaapi documented
    */
    public static String getStringValue (long value)
    {
        return getStringValue(value, getDefaultLocale(), null, false);
    }

    /**
        Returns a formatted string for the given <code>long</code> value in
        the given locale.  The <code>locale</code> parameter must be non-null.

        @param  value  the <code>long</code> value to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>long</code>
        @aribaapi documented
    */
    public static String getStringValue (long value, Locale locale)
    {
        return getStringValue(value, locale, null, false);
    }
    
    /**
        Returns a formatted string for the given <code>long</code> value in
        the given locale.  The <code>locale</code> parameter must be non-null.

        @param  value  the <code>long</code> value to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @return        a string representation of the <code>long</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (long value, Locale locale, String pattern)
    {
        return getStringValue(value, locale, pattern, false);
    }
    
    /**
        Returns a formatted string for the given <code>long</code> value in
        the given locale.  The <code>locale</code> parameter must be non-null.

        @param  value  the <code>long</code> value to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return        a string representation of the <code>long</code>
        @aribaapi documented
    */
    public static String getStringValue (long value, Locale locale, boolean useGrouping)
    {
        return getStringValue(value, locale, null, useGrouping);
    }
    
    /**
        Returns a formatted string for the given <code>long</code> value in
        the given locale.  The <code>locale</code> parameter must be non-null.

        @param  value  the <code>long</code> value to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @param  pattern the DecimalFormat pattern to use for formatting
        @param  useGrouping whether grouping characters should be used
        @return        a string representation of the <code>long</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        long    value,
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
            return fmt.format(value);
        }
        finally
        {
            releaseDecimalFormat(fmt,locale, pattern);
        }
    }

    /*-----------------------------------------------------------------------
        Static Parsing
      -----------------------------------------------------------------------*/

    /**
        Tries to parse the given string as a <code>long</code> in the default
        locale.

        @param     string the string to parse as a <code>long</code>
        @return           a <code>long</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>long</code>
        @aribaapi documented
    */
    public static long parseLong (String string)
      throws ParseException
    {
        return parseLong(string, getDefaultLocale());
    }
    
    /**
        Tries to parse the given string as a <code>long</code> in the given
        <code>locale</code>.

        @param     string the string to parse as a <code>long</code>
        @param     locale the <code>Locale</code> to use for parsing
        @return           a <code>long</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>long</code>
        @aribaapi documented
    */
    public static long parseLong (String string, Locale locale)
      throws ParseException
    {
        return parseLong(string, locale, null);
    }
    
    /**
        Tries to parse the given string as a <code>long</code> in the given
        <code>locale</code>.

        @param  string  the string to parse as a <code>long</code>
        @param  locale  the <code>Locale</code> to use for parsing
        @param  pattern the DecimalFormat pattern to use for parsing
        @return         a <code>long</code> value derived from the string
        @exception      ParseException if the string cannot be parsed as an
                        <code>long</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static long parseLong (String string, Locale locale, String pattern)
      throws ParseException
    {
        Assert.that(locale != null, "invalid null Locale");

        DecimalFormat fmt = null;
        try {
            fmt = acquireDecimalFormat(locale, pattern);
            return fmt.parse(string).longValue();
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
        Returns a <code>long</code> value derived from the given object in the
        default locale.  If the object is not a <code>Long</code>, it is
        converted to a string and parsed.  Returns zero if the object can't be
        converted to a <code>long</code>.

        @param  object the object to convert to an <code>long</code> value
        @return        a <code>long</code> derived from the given object
        @aribaapi documented
    */
    public static long getLongValue (Object object)
    {
        if (object == null) {
            return 0;
        }
        else if (object instanceof Long) {
            return ((Long)object).longValue();
        }
        else {
            String string = object.toString();
            try {
                return parseLong(string);
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
        Compares two <code>Long</code> objects for sorting purposes.  Returns
        an <code>int</code> value which is less than, equal to, or greater
        than zero depending on whether the first object sorts before, the
        same, or after the second object.

        @param  l1 the first <code>Long</code> to compare
        @param  l2 the second <code>Long</code> to compare
        @return    <code>int</code> value which determines how the two objects
                   should be ordered
        @aribaapi documented
    */
    public static int compareLongs (Long l1, Long l2)
    {
        if (l1 == l2) {
            return 0;
        }
        else if (l1 == null) {
            return -1;
        }
        else if (l2 == null) {
            return 1;
        }
        else {
            return compareLongs(l1.longValue(), l2.longValue());
        }
    }

    /**
        Compares two <code>long</code> values for sorting purposes.  Returns an
        <code>int</code> value which is less than, equal to, or greater than
        zero depending on whether the first value sorts before, the same, or
        after the second value.

        @param  l1 the first <code>long</code> value to compare
        @param  l2 the second <code>long</code> value to compare
        @return    <code>int</code> value which determines how the two values
                   should be ordered
        @aribaapi documented
    */
    public static int compareLongs (long l1, long l2)
    {
        if (l1 > l2) {
            return 1;
        }
        else if (l1 < l2) {
            return -1;
        }
        else {
            return 0;
        }
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the given
        locale.  The object must be a non-null <code>Long</code>.

        @param  object the <code>Long</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Long</code>
        @aribaapi documented
    */
    protected String formatObject (Object object, Locale locale)
    {
        Assert.that(object instanceof Long, "invalid type");
        return getStringValue((Long)object, locale);
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Tries to parse the given string into a <code>Long</code> object in
        the given locale.  The string is assumed to be non-null and trimmed of
        leading and trailing whitespace.  The <code>locale</code> parameter is
        currently unused.

        @param     string the string to parse
        @param     locale the <code>Locale</code> to use for parsing (unused)
        @return           a <code>Long</code> object derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Long</code> object in the given locale
        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
      throws ParseException
    {
        return Constants.getLong(parseLong(string));
    }

    /**
        Returns a new <code>Long</code> derived from the given object in the
        given locale.  If the object is not a <code>Long</code>, it is
        converted to a string and parsed.  Returns null if the object can't be
        parsed as a <code>Long</code>.  The <code>locale</code> parameter is
        currently unused.

        @param  object the object to convert to a <code>Long</code>
        @param  locale the <code>Locale</code> to use for conversion (unused)
        @return        a <code>Long</code> derived from the given object
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        return Constants.getLong(getLongValue(object));
    }


    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two objects for sorting purposes in the given locale.  The
        two objects must be non-null <code>Long</code> objects.  Returns a
        value which is less than, equal to, or greater than zero depending on
        whether the first object sorts before, the same, or after the second
        object.  The <code>locale</code> parameter is currently unused.
        
        @param  o1     the first <code>Long</code> to compare
        @param  o2     the second <code>Long</code> to compare
        @param  locale the <code>Locale</code> to use for comparison (unused)
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi documented
    */
    protected int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof Long, "invalid type");
        Assert.that(o2 instanceof Long, "invalid type");

        return compareLongs((Long)o1, (Long)o2);
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
        fmt.setParseIntegerOnly(true);
        return fmt;
    }

    private static DecimalFormat acquireDecimalFormat (Locale locale)
    {
        return acquireDecimalFormat(locale, null);
    }

    private static DecimalFormat acquireDecimalFormat (Locale locale, String pattern)
    {
        return (DecimalFormat)factory.acquireFormat(
                        Formatter.LongFormatterType, locale, pattern);
    }
    
    private static void releaseDecimalFormat (DecimalFormat df,Locale locale)
    {
        releaseDecimalFormat(df, locale, null);
    }

    private static void releaseDecimalFormat (
        DecimalFormat df,Locale locale, String pattern)
    {
        factory.releaseFormat(df,Formatter.LongFormatterType,locale,pattern);
    }
}
