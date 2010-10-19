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

    $Id: //ariba/platform/util/core/ariba/util/formatter/DoubleFormatter.java#7 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
    <code>DoubleFormatter</code> is a subclass of <code>Formatter</code> which
    is responsible for formatting, parsing, and comparing <code>double</code>
    values and/or <code>Double</code> objects.

    @aribaapi documented
*/
public class DoubleFormatter extends DecimalFormatterCommon
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.DoubleFormatter";

    private static final String CanonicalPattern =
        "+#,##0.0###################;-#,##0.0###################";
    
    private static final String CanonicalPositivePrefix = "+";

    private static final String CanonicalPercentSign = "%";

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // the cache of DecimalFormat instances
    private static final DoubleFormatter factory = new DoubleFormatter();


    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>DoubleFormatter</code>.

        @aribaapi private
    */
    public DoubleFormatter ()
    {
    }
    

    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given <code>Double</code> in the
        default locale.  By default, a precision of zero is used.

        @param  object the <code>Double</code> to format into a string
        @return        a string representation of the <code>Double</code>,
                       or empty string if the <code>Double</code> is null
        @aribaapi documented
    */
    public static String getStringValue (Double object)
    {
        return getStringValue(object, 0, 0, null, getDefaultLocale());
    }

    /**
        Returns a formatted string for the given <code>Double</code> in the
        given locale.  By default, a precision of zero is used.  The
        <code>locale</code> parameter must be non-null.

        @param  object the <code>Double</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Double</code>,
                       or empty string if the <code>Double</code> is null
        @aribaapi documented
    */
    public static String getStringValue (Double object, Locale locale)
    {
        return getStringValue(object, 0, 0, null, locale);
    }

    /**
        Returns a formatted string for the given <code>Double</code> in the
        default locale.  The <code>precision</code> parameter is used to
        determine how many fractional digits are shown.

        @param  object    the <code>Double</code> to format into a string
        @param  precision the number of fractional digits to show
        @return           a string representation of the <code>Double</code>,
                          or empty string if the <code>Double</code> is null
        @aribaapi documented
    */
    public static String getStringValue (Double object, int precision)
    {
        return getStringValue(object, precision, precision, null, getDefaultLocale());
    }

    /**
        Returns a formatted string for the given <code>Double</code> in the
        given locale.  The <code>precision</code> parameter is used to
        determine how many fractional digits are shown.  The
        <code>locale</code> parameter must be non-null.

        @param  object    the <code>Double</code> to format into a string
        @param  precision the number of fractional digits to show
        @param  locale    the <code>Locale</code> to use for formatting
        @return           a string representation of the <code>Double</code>,
                          or empty string if the <code>Double</code> is null
        @aribaapi documented
    */
    public static String getStringValue (Double object, int precision, Locale locale)
    {
        return trimString(getStringValue(object, precision, precision, null, locale),
                          locale);
    }

    /**
        Returns a formatted string for the given <code>Double</code> in the
        given locale.  The <code>precision</code> parameter is used to
        determine how many fractional digits are shown.  The
        <code>locale</code> parameter must be non-null.

        @param object the <code>Double</code> to format into a string
        @param minPrecision the minimum number of fractional digits to show
        @param maxPrecision the maximum number of fractional digits to show
        @param locale the <code>Locale</code> to use for formatting
        @return a string representation of the <code>Double</code>,
                or empty string if the <code>Double</code> is null
        @aribaapi documented
    */
    public static String getStringValue (
        Double object,
        int    minPrecision,
        int    maxPrecision,
        Locale locale)
    {
        return getStringValue(object, minPrecision, maxPrecision, null, locale);
    }

    /**
        Returns a formatted string for the given <code>Double</code> in the
        given locale.  The <code>precision</code> parameter is used to
        determine how many fractional digits are shown.  The
        <code>locale</code> parameter must be non-null.

        @param object the <code>Double</code> to format into a string
        @param minPrecision the minimum number of fractional digits to show
        @param maxPrecision the maximum number of fractional digits to show
        @param pattern the DecimalFormat pattern to use for formatting
        @param locale the <code>Locale</code> to use for formatting
        @return a string representation of the <code>Double</code>,
                or empty string if the <code>Double</code> is null
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        Double object,
        int    minPrecision,
        int    maxPrecision,
        String pattern,
        Locale locale)
    {
        if (object == null) {
            return "";
        }

        double d = object.doubleValue();
        return getStringValue(d, minPrecision, maxPrecision, pattern, locale);
    }

    /**
        Returns a formatted string for the given <code>double</code> in the
        default locale.  By default, a precision of zero is used.

        @param  value the <code>double</code> to format into a string
        @return       a string representation of the <code>double</code>
        @aribaapi documented
    */
    public static String getStringValue (double value)
    {
        return getStringValue(value, 0, 0, null, getDefaultLocale());
    }

    /**
        Returns a formatted string for the given <code>double</code> in the
        given locale.  By default, a precision of zero is used.  The
        <code>locale</code> parameter must be non-null.

        @param  value  the <code>double</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>double</code>
        @aribaapi documented
    */
    public static String getStringValue (double value, Locale locale)
    {
        return getStringValue(value, 0, 0, null, locale);
    }

    /**
        Returns a formatted string for the given <code>double</code> in the
        default locale.  The <code>precision</code> parameter is used to
        determine how many fractional digits are shown.

        @param  value     the <code>double</code> to format into a string
        @param  precision the number of fractional digits to show
        @return           a string representation of the <code>double</code>
        @aribaapi documented
    */
    public static String getStringValue (double value, int precision)
    {
        return getStringValue(value, precision, precision, null, getDefaultLocale());
    }

    /**
        Returns a formatted string for the given <code>double</code> in the
        given locale.  The <code>precision</code> parameter is used to
        determine how many fractional digits are shown.  The
        <code>locale</code> parameter must be non-null.

        @param  value     the <code>double</code> to format into a string
        @param  precision the number of fractional digits to show
        @param  locale    the <code>Locale</code> to use for formatting
        @return           a string representation of the <code>double</code>
        @aribaapi documented
    */
    public static String getStringValue (double value, int precision, Locale locale)
    {
        Assert.that(locale != null, "invalid null Locale");
        return getStringValue(value, precision, precision, null, locale);
    }

    /**
        Returns a formatted string for the given <code>double</code> in the
        default locale.

        @param  value     the <code>double</code> to format into a string
        @param  minPrecision the minimum number of fractional digits to show
        @param  maxPrecision the maximum number of fractional digits to show
        @return           a string representation of the <code>double</code>
        @aribaapi documented
    */
    public static String getStringValue (double value, int minPrecision, int maxPrecision)
    {
        return getStringValue(
            value, minPrecision, maxPrecision, null, getDefaultLocale());
    }

    /**
        Returns a formatted string for the given <code>double</code> in the
        given <code>locale</code>.

        @param  value the <code>double</code> to format into a string
        @param  minPrecision the minimum number of fractional digits to show
        @param  maxPrecision the maximum number of fractional digits to show
        @param  locale the <code>Locale</code> to use for formatting
        @return a string representation of the <code>double</code>
        @aribaapi documented
    */
    public static String getStringValue (
        double value,
        int    minPrecision,
        int    maxPrecision,
        Locale locale)
    {
        return getStringValue(value, minPrecision, maxPrecision, null, locale);
    }

     /**
        Returns a formatted string for the given <code>double</code> in the
        given <code>locale</code>.

        @param  value the <code>double</code> to format into a string
        @param  minPrecision the minimum number of fractional digits to show
        @param  maxPrecision the maximum number of fractional digits to show
        @param  pattern the DecimalFormat pattern to use for formatting
        @param  locale the <code>Locale</code> to use for formatting
        @return a string representation of the <code>double</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        double value,
        int    minPrecision,
        int    maxPrecision,
        String pattern,
        Locale locale)
     {
         return getStringValue(value, minPrecision, maxPrecision, pattern, locale, true);
     }

    /**
        Returns a formatted string for the given <code>double</code> in the
        given <code>locale</code>.

        @param  value the <code>double</code> to format into a string
        @param  minPrecision the minimum number of fractional digits to show
        @param  maxPrecision the maximum number of fractional digits to show
        @param  pattern the DecimalFormat pattern to use for formatting
        @param  locale the <code>Locale</code> to use for formatting
        @param  trim true if leading and trailing zeros should be removed.
        @return a string representation of the <code>double</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        double value,
        int    minPrecision,
        int    maxPrecision,
        String pattern,
        Locale locale,
        boolean trim)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        DecimalFormat fmt = null;
        try
        {
            fmt = acquireDecimalFormat(locale, pattern);
            // for now, we force at least one digit for the integral part
            fmt.setMinimumIntegerDigits(1);
            fmt.setMinimumFractionDigits(minPrecision);
            fmt.setMaximumFractionDigits(maxPrecision);
            String formattedValue = fmt.format(value);
            return trim ? trimString(formattedValue, locale) : formattedValue;
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
        Tries to parse the given string as a <code>double</code> in the
        default locale.

        @param     string the string to parse as a <code>double</code>
        @return           a <code>double</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as a
                          <code>double</code>
        @aribaapi documented
    */
    public static double parseDouble (String string)
      throws ParseException
    {
        return parseDouble(string, null, getDefaultLocale());
    }

    /**
        Tries to parse the given string as a <code>double</code> in the given
        locale.  The <code>locale</code> parameter must be non-null.

        @param     string the string to parse as a <code>double</code>
        @param     locale the <code>Locale</code> to use for parsing
        @return           a <code>double</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>double</code>
        @aribaapi documented
    */
    public static double parseDouble (String string, Locale locale)
      throws ParseException
    {
        return parseDouble(string, locale, false);
    }

    /**
        Tries to parse the given string as a <code>double</code> in the given
        locale.  The <code>locale</code> parameter must be non-null.

        @param     string the string to parse as a <code>double</code>
        @param     locale the <code>Locale</code> to use for parsing
        @return           a <code>double</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>double</code>
        @aribaapi documented
    */
    public static double parseDouble (String string, Locale locale, boolean strict)
      throws ParseException
    {
        return parseDouble(string, null, locale, strict);
    }

    /**
        Tries to parse the given string as a <code>double</code> in the given
        locale.  The <code>locale</code> parameter must be non-null.

        @param     string the string to parse as a <code>double</code>
        @param     pattern the DecimalFormat pattern to use for parsing
        @param     locale the <code>Locale</code> to use for parsing
        @return           a <code>double</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>double</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static double parseDouble (String string, String pattern, Locale locale)
      throws ParseException
    {
        return parseDouble(string, pattern, locale, false);
    }

    /**
        Tries to parse the given string as a <code>double</code> in the given
        locale.  The <code>locale</code> parameter must be non-null.

        @param     string the string to parse as a <code>double</code>
        @param     pattern the DecimalFormat pattern to use for parsing
        @param     locale the <code>Locale</code> to use for parsing
        @return           a <code>double</code> value derived from the string
        @exception        ParseException if the string cannot be parsed as an
                          <code>double</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static synchronized double parseDouble (
        String string, String pattern, Locale locale, boolean strict) 
      throws ParseException
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        DecimalFormat specifiedFormat = null;
        DecimalFormat canonicalFormat = null;
        try {
            specifiedFormat = acquireDecimalFormat(locale, pattern);
            Number number;
            if (strict) {
                // guard
                assertValidGroupingSeparators(string, specifiedFormat);

                // parse with passed in values
                ParsePosition parsePosition = new ParsePosition(0);
                number = specifiedFormat.parse(string, parsePosition);
                int parseEndIndex = parsePosition.getIndex();
                boolean parsedWithSpecifiedFormat =
                    parseEndIndex == string.length() && number != null;

                // parse with canonical values if previous failed
                boolean parsedWithCanonicalFormat = false;
                if (!parsedWithSpecifiedFormat) {
                    canonicalFormat = acquireDecimalFormat(locale, CanonicalPattern);
                    parsePosition.setIndex(0);
                    number = canonicalFormat.parse(string, parsePosition);
                    parseEndIndex = parsePosition.getIndex();
                    parsedWithCanonicalFormat =
                        parseEndIndex == string.length() && number != null;
                }

                // throw exception if no parse succeeded
                boolean failedParse =
                    !parsedWithSpecifiedFormat && !parsedWithCanonicalFormat;
                if (failedParse) {
                    throw new ParseException(
                        "Parse ended before index [" + string.length() + "], " +
                            "it parsed [" + number + "].", 
                        parseEndIndex);
                }
            }
            else {
                number = specifiedFormat.parse(string);
            }
            if (number == null) {
                throw new ParseException("Null parse for [" + string + "]", 0);
            }
            return number.doubleValue();
        }
        catch (Exception e) {
            ParseException parseException = new ParseException("Error parsing double.", 0);
            parseException.initCause(e);
            throw parseException;
        }
        finally
        {
            releaseDecimalFormat(specifiedFormat,locale, pattern);
            // canonicalFormat can be null
            releaseDecimalFormat(canonicalFormat, locale, CanonicalPattern);
        }
    }

    protected static ConcurrentMap<DecimalFormat, Pattern> groupingSeparatorsPatternCache
        = new ConcurrentHashMap();

    protected static final Pattern FractionOnly = Pattern.compile("^\\D*\\.");

    protected static void assertValidGroupingSeparators (
        String inputString, DecimalFormat fmt)
      throws ParseException
    {
        // guard: ensure that we're grouping
        
        int groupingSize = fmt.getGroupingSize();
        String groupingSeparator =
            String.valueOf(fmt.getDecimalFormatSymbols().getGroupingSeparator());
        
        boolean notGrouping =
            !fmt.isGroupingUsed()
            || groupingSize <= 0
            || StringUtil.nullOrEmptyOrBlankString(groupingSeparator)
            || FractionOnly.matcher(inputString).find();
        if (notGrouping) {
            // nothing to check
            return;
        }

        // body
        
        Pattern groupingPattern = groupingSeparatorsPatternCache.get(fmt);
        boolean cacheMiss = groupingPattern == null;

        if (cacheMiss) {
            groupingPattern = createGroupingSeparatorPattern(fmt);
            groupingSeparatorsPatternCache.put(fmt, groupingPattern);

        }

        // verify
        Matcher matcher = groupingPattern.matcher(inputString);
        boolean invalidGroupingSeparators = !matcher.find() || matcher.start() != 0;
        if (invalidGroupingSeparators) {
            throw makeParseException(InvalidCharacterInNumberKey, 0);
        }

        // verified!  Do nothing.
    }

    protected static Pattern createGroupingSeparatorPattern (DecimalFormat fmt)
    {
        // create regex

        // create negative prefix / positive prefix regex
        char minusChar = fmt.getDecimalFormatSymbols().getMinusSign();
        String localizedMinusSign = String.valueOf(minusChar);
        // StringUtil.escapeRegEx() fails to escape -
        if (!StringUtil.nullOrEmptyOrBlankString(localizedMinusSign)) {
            localizedMinusSign = StringUtil.strcat("\\", localizedMinusSign);
        }
        String canonicalMinusSign = StringUtil.strcat("\\", CanonicalNegativePrefix);
        // make optional negativePrefix
        String negativePrefix = fmt.getNegativePrefix();
        if (!StringUtil.nullOrEmptyOrBlankString(negativePrefix)) {
            negativePrefix = StringUtil.escapeRegEx(negativePrefix);
//            StringUtil.strcat(negativePrefix, "?");
        }
        String positivePrefix = fmt.getPositivePrefix();
        String prefixes = StringUtil.strcat(
            canonicalMinusSign, localizedMinusSign, positivePrefix, CanonicalPositivePrefix, negativePrefix);
        String negativeSigns = Fmt.S("^\\s*[%s]?\\s*", prefixes);

        // create initial ungrouped digits regex
        int groupingSize = fmt.getGroupingSize();
        String firstDigits = Fmt.S("\\d{1,%s}", groupingSize);

        // create additional grouped digits regex
        String groupingSeparator =
            String.valueOf(fmt.getDecimalFormatSymbols().getGroupingSeparator());
        String groupedDigits = Fmt.S("(?:%s\\d{%s})*", groupingSeparator, groupingSize);
//        String finalGroupedDigits = Fmt.S("(?:%s\\D)?", groupedDigits);
        
        // not (digit or grouping separator) or end of string
        String end = Fmt.S("(?:[^\\d%s]|$)", groupingSeparator);

        // create final regex
        String groupingRegex =
            StringUtil.strcat(negativeSigns, firstDigits, groupedDigits, end);

        // compile and done
        return Pattern.compile(groupingRegex);
    }

    public static String getLocalizedPercentSign (Locale locale)
    {
        DecimalFormat format = null;
        try {
            format = acquireDecimalFormat(locale, CanonicalPattern);
            Character ch = format.getDecimalFormatSymbols().getPercent();
            return ch.toString();
        }
        finally {
            releaseDecimalFormat(format, locale, CanonicalPattern);
        }
    }

    public static String getCanonicalPercentSign ()
    {
        return CanonicalPercentSign;
    }

    /**
        Returns a <code>double</code> value derived from the given object in
        the default locale.  If the object is not a <code>Double</code>, it is
        converted to a string and parsed.  Returns <code>0.0</code> if the
        object can't be converted to a <code>double</code>.

        @param  object the object to convert to an <code>double</code> value
        @return        a <code>double</code> derived from the given object
        @aribaapi documented
    */
    public static double getDoubleValue (Object object)
    {
        return getDoubleValue(object, getDefaultLocale());
    }

    /**
        Returns a <code>double</code> value derived from the given object in
        the default locale.  If the object is not a <code>Double</code>, it is
        converted to a string and parsed.  Returns <code>0.0</code> if the
        object can't be converted to a <code>double</code>.

        @param  object the object to convert to an <code>double</code> value
        @param  locale the <code>Locale</code> to use for conversion
        @return        a <code>double</code> derived from the given object
        @aribaapi documented
    */
    public static double getDoubleValue (Object object, Locale locale)
    {
        if (object == null) {
            return 0.0;
        }
        else if (object instanceof Number) {
            return ((Number)object).doubleValue();
        }
        else {
            String string = object.toString();
            try {
                return parseDouble(string, locale);
            }
            catch (ParseException e) {
                return 0.0;
            }
        }
    }


    /*-----------------------------------------------------------------------
        Static Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two <code>Double</code> objects for sorting purposes.  Returns
        an <code>int</code> value which is less than, equal to, or greater
        than zero depending on whether the first object sorts before, the
        same, or after the second object.

        @param  d1 the first <code>Double</code> to compare
        @param  d2 the second <code>Double</code> to compare
        @return    <code>int</code> value which determines how the two objects
                   should be ordered
        @aribaapi documented
    */
    public static int compareDoubles (Double d1, Double d2)
    {
        if (d1 == d2) {
            return 0;
        }
        else if (d1 == null) {
            return -1;
        }
        else if (d2 == null) {
            return 1;
        }
        else {
            return compareDoubles(d1.doubleValue(), d2.doubleValue());
        }
    }

    /**
        Compares two <code>double</code> values for sorting purposes.  Returns
        an <code>int</code> value which is less than, equal to, or greater
        than zero depending on whether the first value sorts before, the
        same, or after the second value.

        @param  d1 the first <code>double</code> to compare
        @param  d2 the second <code>double</code> to compare
        @return    <code>int</code> value which determines how the two values
                   should be ordered
        @aribaapi documented
    */
    public static int compareDoubles (double d1, double d2)
    {
        if (d1 > d2) {
            return 1;
        }
        else if (d1 < d2) {
            return -1;
        }
        else {
            return 0;
        }
    }


    /*-----------------------------------------------------------------------
        !!! Flexible Double !!!
      -----------------------------------------------------------------------*/

    /**
        Flexible conversion of a string into a Double.  Converts decimal
        format, as well as fractional format like "12 3/8 = 12.375" or "1-3/4
        = 1.75". Also supports localized format: "12,3/4 = 12.75".  If it
        cannot convert, it returns null.

        @aribaapi private
    */
    public static Double flexibleDoubleValue (Object stringValue)
    {
        try {
            double doubleValue;
            String string = stringValue.toString().trim();

            int slashIndex = string.indexOf('/');

                // decimal format
            if (slashIndex == -1) {
                doubleValue = parseDouble(string, null);
            }
                // fractional format
            else {
                String whole = null;
                String numerator = null;
                String denominator = null;

                    // Check for negative
                boolean isNegative = string.startsWith("-");

                    // Replace minus or comma with space delimiter
                string = string.replace('-', ' ');
                string = string.replace(',', ' ');

                    // search for space, for delimiter after whole number
                int spaceIndex = string.lastIndexOf(' ', slashIndex);

                    // fraction only
                if (spaceIndex == -1 || spaceIndex == 0) {
                    numerator = string.substring(0, slashIndex);
                    denominator = string.substring(slashIndex+1);
                }
                    // whole amount as well
                else {
                    whole = string.substring(0, spaceIndex);
                    numerator = string.substring(spaceIndex+1, slashIndex);
                    denominator = string.substring(slashIndex+1);
                }

                Integer integer;
                double wholeValue = 0.0;
                double numeratorValue = 0.0;
                double denominatorValue = 0.0;

                if (whole != null) {
                    integer = Integer.valueOf(whole.trim());
                    wholeValue = integer.doubleValue();
                }
                integer = Integer.valueOf(numerator.trim());
                numeratorValue = integer.doubleValue();
                integer = Integer.valueOf(denominator.trim());
                denominatorValue = integer.doubleValue();

                double d = 0.0;
                if (denominatorValue == 0.0) {
                    d = wholeValue;
                }
                else {
                    d = wholeValue + (numeratorValue / denominatorValue);
                }

                if (isNegative) {
                    d = d * -1.0;
                }

                doubleValue = d;
            }
            return new Double(doubleValue);
        }
        catch (ParseException e) {
            return null;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the given
        locale.  The object must be a non-null <code>Double</code>.

        @param  object the <code>Double</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Double</code>
        @aribaapi documented
    */
    protected String formatObject (Object object, Locale locale)
    {
        Assert.that(object instanceof Double, "invalid type");
        return getStringValue((Double)object, locale);
    }

    /**
        Returns a string representation of the given object in the given
        locale with the given precision.
        The object must be a non-null <code>Double</code>.

        @param  object the <code>Double</code> to format into a string
        @param  precision the number of fractional digits to show
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Double</code>
        @aribaapi documented
    */
    public String formatObject (Object object, int precision, Locale locale)
    {
        Assert.that(object instanceof Double, "invalid type");
        return getStringValue((Double)object, precision, locale);
    }

    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Tries to parse the given string into a <code>Double</code> object in
        the given locale.  The string is assumed to be non-null and trimmed of
        leading and trailing whitespace.

        @param     string the string to parse
        @param     locale the <code>Locale</code> to use for parsing (unused)
        @return           a <code>Double</code> object derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Double</code> object in the given locale
        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
      throws ParseException
    {
        return new Double(parseDouble(string, locale));
    }

    /**
        Returns a new <code>Double</code> derived from the given object in the
        given locale.  If the object is not a <code>Double</code>, it is
        converted to a string and parsed.  Returns null if the object can't be
        parsed as a <code>Double</code>.

        @param  object the object to convert to a <code>Double</code>
        @param  locale the <code>Locale</code> to use for conversion (unused)
        @return        a <code>Double</code> derived from the given object
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        return new Double(getDoubleValue(object, locale));
    }


    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two objects for sorting purposes in the given locale.  The
        two objects must be non-null <code>Double</code> objects.  Returns a
        value which is less than, equal to, or greater than zero depending on
        whether the first object sorts before, the same, or after the second
        object.  The <code>locale</code> parameter is currently unused.

        @param  o1     the first <code>Double</code> to compare
        @param  o2     the second <code>Double</code> to compare
        @param  locale the <code>Locale</code> to use for comparison (unused)
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi documented
    */
    protected int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof Double, "invalid type");
        Assert.that(o2 instanceof Double, "invalid type");

        return compareDoubles((Double)o1, (Double)o2);
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
        fmt.setGroupingUsed(true);
        return fmt;
    }

    private static DecimalFormat acquireDecimalFormat (Locale locale)
    {
        return acquireDecimalFormat(locale, null);
    }

    private static DecimalFormat acquireDecimalFormat (Locale locale, String pattern)
    {
        return (DecimalFormat)factory.acquireFormat(
                        Formatter.DoubleFormatterType, locale, pattern);
    }

    private static void releaseDecimalFormat (DecimalFormat df,Locale locale)
    {
        releaseDecimalFormat(df, locale, null);
    }

    private static void releaseDecimalFormat (
        DecimalFormat df,Locale locale, String pattern)
    {
        factory.releaseFormat(df, Formatter.DoubleFormatterType,
                                locale, pattern);
    }

}
