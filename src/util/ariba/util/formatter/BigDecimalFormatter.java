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

    $Id: //ariba/platform/util/core/ariba/util/formatter/BigDecimalFormatter.java#22 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.StringUtil;
import ariba.util.core.MapUtil;
import ariba.util.i18n.LocaleSupport;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ariba.util.log.Log;

/**
    Responsible for formatting, parsing, and comparing BigDecimal values.

    @aribaapi documented
*/
public class BigDecimalFormatter extends DecimalFormatterCommon
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.BigDecimalFormatter";

    private static final char CanonicalDecimalSeparator = '.';
    private static final String CanonicalDecimalSeparatorString = ".";
    private static final char CanonicalMinusSign        = '-';

    private static final char CanadianEnglishGroupSeparator = ',';

    private static final int NOT_FOUND = -1;

    /*-----------------------------------------------------------------------
        Nested class
      -----------------------------------------------------------------------*/

    /**
     * If a parse fails ONLY because strict behavior is specified this is used.
     *
     * Non-strict is basically compatibility mode to avoid causing parse errors
     * in far away parts of the system (specifically importing Viking (AKA 8.2.2) xls data
     * caused problems in the past).
     *
     * We also need to differentiate between a regular ParseException and
     * a StrictParseException in order to preserve the old behavior in other
     * parts of the system.
     */
    public static class StrictParseException extends ParseException
    {
        final static protected String MessageKey = "CannotParseBigDecimal";

        public StrictParseException (String s, int errorOffset)
        {
            super(s, errorOffset);
        }

        /**
         *
         * @param offset
         * @param string The offending string
         * @return
         */
        public static StrictParseException make (int offset, String string)
        {
            String msg = Formatter.makeParseExceptionMessage(MessageKey, string);
            return new StrictParseException(msg, offset);
        }

        public static StrictParseException make (ParseException e, String string)
        {

            String msg = Formatter.makeParseExceptionMessage(MessageKey, string);
            StrictParseException parseException = new StrictParseException(msg, 0);
            parseException.initCause(e);
            return parseException;
        }

        public static boolean isStrict (ParseException ex)
        {
            return ex instanceof StrictParseException;
        }
    }

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    private static final BigDecimalFormatter factory = new BigDecimalFormatter();


    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>BigDecimalFormatter</code>.
        @aribaapi documented
    */
    public BigDecimalFormatter ()
    {
    }


    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given BigDecimal
        <code>object</code> in the default locale.
        @param object The BigDecimal used to generate the string value
        @return Returns a string representation of the BigDecimal
        @aribaapi documented
    */
    public static String getStringValue (BigDecimal object)
    {
        return getStringValue(object, getDefaultLocale());
    }

    /**
        Returns a formatted string for this BigDecimal <code>object</code> in
        this <code>locale</code>.  Trailing decimal zeros are removed from the
        string.

        @param object <code>BigDecimal</code> to convert to a string value
        @param locale <code>Locale</code> to use to format the string

        @return Returns a string representation of the <code>BigDecimal</code>
        @aribaapi documented
    */
    public static String getStringValue (BigDecimal object, Locale locale)
    {
        if (object == null) {
            return Constants.EmptyString;
        }

        return trimString(getStringValue(object, object.scale(), locale), locale);
    }

    /**
        Returns a formatted string for this BigDecimal <code>value</code>
        using this <code>scale</code> to determine how many fractional digits
        are shown.  Uses the formatter's default locale.

        @param value <code>BigDecimal</code> to convert to a string value
        @param scale The number of decimal places to create in the string value

        @return Returns a string representation of the <code>BigDecimal</code>
        @aribaapi documented
    */
    public static String getStringValue (BigDecimal value, int scale)
    {
        return getStringValue(value, scale, getDefaultLocale());
    }

    /**
        Returns a formatted string for this BigDecimal <code>value</code>
        using this <code>scale</code> to determine how many fractional digits
        are shown.  Uses the default pattern for this <code>locale</code>.

        @param value <code>BigDecimal</code> to convert to a string value
        @param scale The number of decimal places to create in the string value
        @param locale <code>Locale</code> to use to format the string

        @return Returns a string representation of the <code>BigDecimal</code>
        @aribaapi documented
    */
    public static String getStringValue (BigDecimal value, int scale, Locale locale)
    {
        return getStringValue(value, scale, locale, (String)null);
    }

    /**
        Returns a formatted string for this BigDecimal <code>value</code>
        using this <code>scale</code> to determine how many fractional digits
        are shown.  Uses the default pattern for this <code>locale</code>.

        @param value <code>BigDecimal</code> to convert to a string value
        @param scale The number of decimal places to create in the string value
        @param locale <code>Locale</code> to use to format the string
        @param pattern the DecimalFormat pattern to use for formatting

        @return Returns a string representation of the <code>BigDecimal</code>
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static String getStringValue (
        BigDecimal value,
        int        scale,
        Locale     locale,
        String     pattern)
    {
        DecimalFormat fmt = null;
        try
        {
            fmt = acquireDecimalFormat(locale, pattern);
            return getStringValue(value, scale, locale, fmt);
        }
        finally
        {
            releaseDecimalFormat(fmt,locale, pattern);
        }
    }

    /**
        Returns a formatted string for this <code>value</code> using
        this <code>scale</code> to determine how many fractional digits are
        shown.  Uses the <code>DecimalFormat</code> passed in to format the number.

        @param value <code>BigDecimal</code> to convert to a string value.
        @param scale The number of decimal places to create in the string value.
        @param locale <code>Locale</code> to use to format the string.
        @param fmt The <code>DecimalFormat</code> to use for fetching localized
                   symbols.

        @return Returns a string representation of the <code>BigDecimal</code>.
        @aribaapi documented
    */
    public static String getStringValue (
        BigDecimal    value,
        int           scale,
        Locale        locale,
        DecimalFormat fmt)
    {
        if (value == null) {
            return Constants.EmptyString;
        }

            // Always set the scale to this value so that we get
            // any necessary zero padding.
        if (scale != value.scale()) {
            value = value.setScale(scale, BigDecimal.ROUND_HALF_UP);
        }

        FastStringBuffer buf = new FastStringBuffer(value.toPlainString());

        DecimalFormatSymbols symbols = fmt.getDecimalFormatSymbols();

            // Extract localized characters for parsing numbers
        char decimalSep = symbols.getDecimalSeparator();
        char groupSep   = symbols.getGroupingSeparator();
        int  groupSize  = fmt.getGroupingSize();
        String negativePrefix = fmt.getNegativePrefix();
        String negativeSuffix = fmt.getNegativeSuffix();
        String positivePrefix = fmt.getPositivePrefix();
        String positiveSuffix = fmt.getPositiveSuffix();

            // Remove any canonical negative sign
        int minusSign = buf.indexOf(CanonicalMinusSign);
        if (minusSign > -1) {
            buf.removeCharAt(minusSign);
        }

            // Replace the canonical decimal separator with a localized version
        int decimal = buf.indexOf(CanonicalDecimalSeparator);
        if (decimal > -1) {
            buf.replace(decimal, decimalSep, 1);
        }

            // temporary hack to work around bug in IE where
            // the group separator of Canadian English is ';'
        if (locale.equals(java.util.Locale.CANADA)) {
            groupSep = CanadianEnglishGroupSeparator;
        }

            // Insert any group separators.  If no decimal separator exists,
            // then start inserting group separators relative to the end of
            // the string.
        if (decimal < 0) {
            decimal = buf.length();
        }
        if (groupSize > 0) {
            for (int i = decimal-groupSize; i > 0; i -= groupSize) {
                buf.insert(groupSep, i);
            }
        }

        if (value.signum() >= 0) {
            buf.insert(positivePrefix, 0);
            buf.append(positiveSuffix);
        }
        else {
            buf.insert(negativePrefix, 0);
            buf.append(negativeSuffix);
        }

        return buf.toString();
    }

    /*-----------------------------------------------------------------------
        Static Parsing
      -----------------------------------------------------------------------*/

    /**
        Parses this <code>string</code> as a <code>BigDecimal</code> in the
        default locale.
        @param string The string to be parsed
        @return Returns a BigDecimal for the input string using the default locale
        @aribaapi documented
    */
    public static BigDecimal parseBigDecimal (String string)
      throws ParseException
    {
        return parseBigDecimal(string, getDefaultLocale());
    }

    /**
        Parses this <code>string</code> as a BigDecimal in this
        <code>locale</code>.

        First converts the number string into a "canonical" number string
        which is acceptable to the BigDecimal(string) constructor.  It then
        calls this constructor and returns a new BigDecimal.

        If the parse cannot be performed successfully, the parser throws
        a <code>ParseException</code>.

        @param string The string to parse.
        @param locale The locale to use to get localized number and currency
                      symbols such as the decimal separator.

        @return Returns the <code>BigDecimal</code> corresponding to the
                <code>string</code> parameter.
        @aribaapi documented
    */
    public static BigDecimal parseBigDecimal (String string, Locale locale)
      throws ParseException
    {
        return parseBigDecimal(string, locale, null);
    }

    /**
        Parses this <code>string</code> as a BigDecimal in this
        <code>locale</code>.

        First converts the number string into a "canonical" number string
        which is acceptable to the BigDecimal(string) constructor.  It then
        calls this constructor and returns a new BigDecimal.

        If the parse cannot be performed successfully, the parser throws
        a <code>ParseException</code>.

        @param string  The string to parse.
        @param locale  The locale to use to get localized number and currency
                       symbols such as the decimal separator.
        @param pattern The DecimalFormat pattern to use for parsing

        @return Returns the <code>BigDecimal</code> corresponding to the
                <code>string</code> parameter.
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static BigDecimal parseBigDecimal (
        String string,
        Locale locale,
        String pattern)
      throws ParseException
    {
        return parseBigDecimal(string, locale, pattern, false);
    }


    /**
        Parses this <code>string</code> as a BigDecimal in this
        <code>locale</code>.

        First converts the number string into a "canonical" number string
        which is acceptable to the BigDecimal(string) constructor.  It then
        calls this constructor and returns a new BigDecimal.

        If the parse cannot be performed successfully, the parser throws
        a <code>ParseException</code>.

        @param string  The string to parse.
        @param locale  The locale to use to get localized number and currency
                       symbols such as the decimal separator.
        @param pattern The DecimalFormat pattern to use for parsing
        @param strict If we should be lenient in what we accept, or throw an exception
            on a bad string.

        @return Returns the <code>BigDecimal</code> corresponding to the
                <code>string</code> parameter.
        @aribaapi documented
        @see java.text.DecimalFormat
    */
    public static BigDecimal parseBigDecimal (
        String string,
        Locale locale,
        String pattern,
        boolean strict)
      throws ParseException
    {
        Assert.that(locale != null, "invalid null Locale");
        DecimalFormat fmt = null;
        try
        {
            fmt = acquireDecimalFormat(locale, pattern);
            string = LocaleSupport.normalizeNumber(string, locale);

            DecimalParseInfo info = parseBigDecimal(string, fmt, strict);
            // if strict then the string prefix and string suffix should be empty
            if (strict) {
                BigDecimalParser.assertStrict(info);
            }
            return info.number;
        }
        finally
        {
            releaseDecimalFormat(fmt,locale, pattern);
        }
    }

    /**
        Parses this <code>string</code> as a BigDecimal in this
        <code>locale</code>, returning a <code>DecimalParseInfo</code>
        object describing the parse.

        The BigDecimal result can be accessed through the <code>number</code>
        public member on <code>DecimalParseInfo</code>

        If the parse cannot be performed successfully, the parser throws
        a <code>ParseException</code>.

        @param string The string to parse.
        @param locale The locale to use to get localized number and currency
                      symbols such as the decimal separator.

        @return Returns the <code>DecimalParseInfo</code> object corresponding
                to the <code>string</code> parameter.
        @aribaapi documented
    */
    public static DecimalParseInfo parseBigDecimalInfo (String string, Locale locale)
      throws ParseException
    {
        return parseBigDecimalInfo (string, locale, false);
    }

    /**
        Parses this <code>string</code> as a BigDecimal in this
        <code>locale</code>, returning a <code>DecimalParseInfo</code>
        object describing the parse.

        The BigDecimal result can be accessed through the <code>number</code>
        public member on <code>DecimalParseInfo</code>

        If the parse cannot be performed successfully, the parser throws
        a <code>ParseException</code>.

        @param string The string to parse.
        @param locale The locale to use to get localized number and currency
                      symbols such as the decimal separator.

        @return Returns the <code>DecimalParseInfo</code> object corresponding
                to the <code>string</code> parameter.
        @aribaapi documented
    */
    public static DecimalParseInfo parseBigDecimalInfo (
        String string,
        Locale locale,
        boolean strict)
      throws ParseException
    {
        Assert.that(locale != null, "invalid null Locale");
        DecimalFormat fmt = null;
        try
        {
            fmt = acquireDecimalFormat(locale);
            return parseBigDecimal(string, fmt, strict);
        }
        finally
        {
            releaseDecimalFormat(fmt,locale);
        }
    }

    /**
        Parse the <code>string</code> parameter and return a
        <code>DecimalParseInfo</code> that returns information gleaned
        from parse such as the <code>BigDecimal</code> number
        corresponding to the string, the number of decimal places,
        etc.

        If the parse cannot be performed successfully, the parser throws
        a <code>ParseException</code>.

        @param string The string to parse.
        @param fmt The <code>DecimalFormat</code> to use for fetching localized
                   symbols.

        @return Returns the <code>BigDecimal</code> corresponding to the
                <code>string</code> parameter.
        @aribaapi documented
    */
    public static DecimalParseInfo parseBigDecimal (String string, DecimalFormat fmt)
      throws ParseException
    {
        return parseBigDecimal (string, fmt, false);
    }


    /**
        Parse the <code>string</code> parameter and return a
        <code>DecimalParseInfo</code> that returns information gleaned
        from parse such as the <code>BigDecimal</code> number
        corresponding to the string, the number of decimal places,
        etc.

        If the parse cannot be performed successfully, the parser throws
        a <code>ParseException</code>.

        @param string The string to parse.
        @param fmt The <code>DecimalFormat</code> to use for fetching localized
                   symbols.
        @param strict If we should be lenient in what we accept, or throw an exception
            on a bad string.

        @return Returns the <code>BigDecimal</code> corresponding to the
                <code>string</code> parameter.
        @aribaapi documented
    */
    public static DecimalParseInfo parseBigDecimal (String string,
                                                    DecimalFormat fmt,
                                                    boolean strict)
      throws ParseException
    {
        DecimalParseInfo info = null;
        if (!strict) {
            info = _parseBigDecimal(string, fmt);
        }
        else {
            info = BigDecimalParser.strictParse(string, fmt);
        }
        return info;
    }

    /**
     * TODO: move this into the BigDecimalParser (not done to minimize diff).
     *  -mgower July 12, 2010
     * @param string
     * @param fmt
     * @return
     * @throws ParseException
     */
    private static DecimalParseInfo _parseBigDecimal (String string, DecimalFormat fmt)
        throws ParseException
    {
        /*
         * In general we loop through the input string and fill some intermediate
         * buffers with the results.  Then we convert the buffers to other types
         * and pass these to a DecimalParseInfo constructor.
         */

        /* House keeping setup */

            // eg if "10-" is a valid string, ignoreNegativePrefix will be set to true
        boolean ignoreNegativePrefix = false;
            // eg if "-10" is a valid string, ignoreNegativeSuffix will be set to true
            // if "(10)" is a valid string, neither will be ignored
        boolean ignoreNegativeSuffix = false;

        DecimalFormatSymbols symbols = fmt.getDecimalFormatSymbols();

            // Initialize the parsing result buffers
        FastStringBuffer numberBuffer = new FastStringBuffer();
        FastStringBuffer prefixStringStringBuffer = new FastStringBuffer();
        FastStringBuffer suffixStringStringBuffer = new FastStringBuffer();

        // Extract localized characters for parsing numbers
            // For the string "1,234.56" '.' is the decimal separator.
        char decimalSep = symbols.getDecimalSeparator();
            //For the string "1,234.56" ',' is the grouping separator.
        char groupSep   = symbols.getGroupingSeparator();
            // The negativePrefix will typically be ( or -, as in (100) or -100.
        String negativePrefix = fmt.getNegativePrefix();
            // The negative suffix will typically be ) or '', as in (100) or -100.
            // Something like 100- is allowed in some locals as well though.
        String negativeSuffix = fmt.getNegativeSuffix();
        int length = string.length();

        Log.util.debug("parseBigDecimal: string = %s, neg prefix = %s, neg suffix = %s",
                       string, negativePrefix, negativeSuffix);

        // housekeeping for negative signs
        boolean negativePrefixIsMissing =
                StringUtil.nullOrEmptyOrBlankString(negativePrefix);
        boolean negativeSuffixIsMissing =
                StringUtil.nullOrEmptyOrBlankString(negativeSuffix);
        if (negativePrefixIsMissing) {
            if (!negativeSuffixIsMissing) {
                ignoreNegativePrefix = true;
            }
            else {
                    // We have no negative prefix or suffix,
                    // so we don't have the (optional) negative pattern at all.
                    // The core java doc says to use '-' as prefix.
                negativePrefix = CanonicalNegativePrefix;
                ignoreNegativeSuffix = true;
            }
        }
            // we have a negative prefix and no negative suffix
        else if (negativeSuffixIsMissing) {
            ignoreNegativeSuffix = true;
        }

        boolean negPrefixFound = false;
        boolean negSuffixFound = false;
            // if we've found any digits (decimal or integer) anywhere ever in the loop
        boolean foundDigits = false;
            // count of the decimal digits, ie 1.23 would have 2 at the end of the loop
        int decimalDigits  = 0;
            // count of the integer digits, ie 1.23 would have 1 at the end of the loop
        int integerDigits  = 0;
            // index of the decimal point, ie 1.23 would have decimal location 1
        int decimalLocation = NOT_FOUND;

        /*
        * Read through input string, character by character.
        * This will fill the buffers and replace any localized punctuation
        * characters with canonical ones
        */
        for (int i = 0; i < length; i++) {
            char curChar = string.charAt(i);
            char nextChar = i < length-1 ? string.charAt(i+1) : ' ';

                // Are we on the format's negative prefix?  If so, skip it
                // We check for the canonical negative prefix below
            if (!negPrefixFound && !foundDigits && negativePrefix.length() > 0 &&
                string.indexOf(negativePrefix, i) == i)
            {
                Log.util.debug("found neg prefix at %s", i);
                negPrefixFound = true;
                i += negativePrefix.length()-1;
            }
                // Are we on the negative suffix?  If so, skip it
            else if (!negSuffixFound && foundDigits &&
                     negativeSuffix.length() > 0 &&
                     string.indexOf(negativeSuffix, i) == i)
            {
                negSuffixFound = true;
                Log.util.debug("found neg suffix at %s", i);
                i += negativeSuffix.length()-1;
            }

                // We found a decimal separator if there's a digit either
                // before or after the separator and there aren't more
                // decimal separators after the current one.
            else if (decimalSep == curChar &&
                     (foundDigits || Character.isDigit(nextChar) &&
                     string.lastIndexOf(decimalSep) == i))
            {

                    // Replace with the canonical decimal separator for the
                    // BigDecimal constructor.
                numberBuffer.append(CanonicalDecimalSeparator);
                decimalLocation = i;
            }

                // We're in the group separator (canonical ',')  or in white space
            else if (Character.isWhitespace(curChar) ||
                     Character.isSpaceChar(curChar)  ||
                     groupSep == curChar)
            {
                    // ignore these characters, no state changes at all
            }

                // We're either in the number itself
            else if (Character.isDigit(curChar)) {

                    // If we found a suffix yet there's still digits,
                    // then we've got exponential notation or an invalid number
                boolean exponentialOrInvalid =
                        suffixStringStringBuffer.length() > 0 || negSuffixFound;
                if (exponentialOrInvalid) {
                    char prevChar = string.charAt(i -1);
                    boolean exponential = prevChar == 'E' || prevChar == 'e';
                    if (exponential) {
                        return BigDecimalParser.parseExponentialBigDecimal(string, fmt);
                    }
                    else {
                        throw makeParseException(InvalidCharacterInNumberKey, 1);
                    }
                }

                numberBuffer.append(curChar);
                foundDigits = true;
                if (decimalLocation > NOT_FOUND) {
                    // decimal place has been found
                    decimalDigits++;
                }
                else {
                    integerDigits++;
                }
            }
                // we're on the prefix string, AKA random text before the number
            else if (!foundDigits)
            {
                prefixStringStringBuffer.append(curChar);
            }
                // must be the string suffix
            else {
                suffixStringStringBuffer.append(curChar);
            }
        } // end input string loop

        /* Convert the buffer data to the format for DecimalParseInfo's constructor */

        // make number buffer negative based on housekeeping data
        boolean negativePrefixWasIgnored =
                !negPrefixFound && ignoreNegativePrefix;
        boolean negativePrefixIsValid =
                negativePrefixWasIgnored || negPrefixFound;

        boolean negativeSuffixWasIgnored =
                !negSuffixFound && ignoreNegativeSuffix;
        boolean negativeSuffixIsValid =
                negativeSuffixWasIgnored || negSuffixFound;

            // Put back a canonical minus sign if the number is negative
        boolean isNegative = negativePrefixIsValid && negativeSuffixIsValid;
        if (isNegative) {
            numberBuffer.insert(CanonicalMinusSign, 0);
        }

        // sanity check
        boolean noNumbers = numberBuffer.length() == 0;
        if (noNumbers) {
            throw makeParseException(NoDigitsFoundKey, 0);
        }

        // convert number buffer to big decimal
        String numberString = numberBuffer.toString();
        BigDecimal amount = Constants.ZeroBigDecimal;
        try {
            amount = new BigDecimal(numberString);
        }
        catch (NumberFormatException e) {
            throw makeParseException(NumberFormatErrorKey, 0);
        }

        // convert other buffers to strings
        String prefixString = prefixStringStringBuffer.toString();
        if (StringUtil.nullOrEmptyOrBlankString(prefixString)) {
            prefixString = null;
        }
        String suffixString = suffixStringStringBuffer.toString();
        if (StringUtil.nullOrEmptyOrBlankString(suffixString)) {
            suffixString = null;
        }

        /* Use converted housekeeping data */

        return new DecimalParseInfo(integerDigits, decimalDigits, decimalLocation,
                                    prefixString, suffixString, amount);
    }

    /**
     * Meant for testing only.
     */
    public static void enableParserDebug ()
    {
        BigDecimalParser.enableDebug();
    }

    /**
     * This handles the core of decimal parsing and almost all strict parsing and
     * related functionality.
     *
     * Terms:
     *  matching negatives = a matching negative prefix and suffix, ie "(1)" would have
     *      matching negatives and "-1" or "(1" would not.
     *  stranded negatives = one negative prefix or suffix when both are needed,
     *      ie "(1" and "1)" would have stranded negatives and "(1)" and "-1" would not
     *
     * High Level Implementation Overview:
     *  In general we are using (relatively) simple regexes and then running some
     *  asserts to verify everything.  Since we had so many regex capturing groups
     *  they are named via an enum and accessed via private methods.
     *
     * Java inner static classes don't really have a real private scope ("private" methods
     *  can be seen / used in the containing class) so methods meant to be used inside
     *  of the BigDecimalParser specifically start with "_" and methods meant to only
     *  be used in their section (the area between 
     *  ////////////
     *  // headers
     *  ////////////) start with "__".
     *
     * @see StrictParseException
     */
    private static class BigDecimalParser
    {


        /////////////////////////////////////////////////////////////////////////////
        // "Public" methods (intended to be called from BigDecimalFormatter
        /////////////////////////////////////////////////////////////////////////////


        private static DecimalParseInfo parseExponentialBigDecimal (
            String string,
            DecimalFormat fmt)
                throws StrictParseException
        {
            // try to create a BigDecimal, convert to plain string
            // and recall this same function with the plain string
            try {
                // BigDecimal constructor can choke on commas (aka grouping separator)
                string = _canonicalizeExponentialBigDecimal (string, fmt);
                
                BigDecimal parsedBigDecimal = new BigDecimal(string);
                String parsedString = parsedBigDecimal.toPlainString();
                DecimalFormat canonicalExponentialFormat =
                        acquireDecimalFormat(Locale.ENGLISH);
                return BigDecimalFormatter.parseBigDecimal(parsedString,
                                                           canonicalExponentialFormat);
            }
            catch (ParseException e) {
                throw StrictParseException.make(e, string);
            }
            catch (NumberFormatException e) {
                ParseException parseException =
                    makeParseException(NumberFormatErrorKey, 1);
                parseException.initCause(e);
                throw StrictParseException.make(parseException, string);
            }
        }

        private static void assertStrict (DecimalParseInfo info)
            throws StrictParseException
        {
            // guard
            if (info == null) {
                return;
            }

            // body
            boolean stringPrefixExists =
                    !StringUtil.nullOrEmptyOrBlankString(info.prefix);
            if (stringPrefixExists) {
                throw StrictParseException.make(0, info.prefix);
            }

            boolean stringSuffixExists =
                    !StringUtil.nullOrEmptyOrBlankString(info.suffix);
            if (stringSuffixExists) {
                int position = info.decimalLocation + info.decimalDigits;
                throw StrictParseException.make(position, info.suffix);
            }
        }

        private static DecimalParseInfo strictParse (String string, DecimalFormat fmt)
            throws StrictParseException
        {
            try {
                return _strictParse(string, fmt);
            }
            catch (ParseException e) {
                throw StrictParseException.make(e, string);
            }

        }


        //////////////////////////////////////////////////////////////////////////////////
        // Helpers to parseExponentialBigDecimal
        //////////////////////////////////////////////////////////////////////////////////


        private static ConcurrentMap<DecimalFormat, Pattern> __groupingSeparatorPatterns =
            new ConcurrentHashMap();

        private static Map<DecimalFormat, Pattern> __matchingNegativesPatterns =
            new ConcurrentHashMap();

        private static String _canonicalizeExponentialBigDecimal (
            String string,
            DecimalFormat fmt)
        {
            // get groupingSeparatorPattern, from cache if possible
            Pattern groupingSeparatorPattern   = __groupingSeparatorPatterns.get(fmt);
            boolean groupingSeparatorCacheMiss = groupingSeparatorPattern == null;
            if (groupingSeparatorCacheMiss) {
                String groupingSeparatorRegex =
                    StringUtil.strcat("[", _getGroupingSeparator(fmt), "]");
                groupingSeparatorPattern = Pattern.compile(groupingSeparatorRegex);
                __groupingSeparatorPatterns.put(fmt, groupingSeparatorPattern);
                Log.util.debug(
                    "Total patterns cached in BigDecimalFormatter.BigDecimalParser:%s.",
                    __groupingSeparatorPatterns.size());

            }
            string = groupingSeparatorPattern.matcher(string).replaceAll("");

            // get matchingNegativesPattern as needed, from cache if possible
            if (_formatHasMatchingNegatives(fmt)) {
                Pattern matchingNegativesPattern = __matchingNegativesPatterns.get(fmt);
                boolean matchingNegativesCacheMiss = matchingNegativesPattern == null;
                if (matchingNegativesCacheMiss) {
                    String negativePrefix =
                        StringUtil.escapeRegEx(getNegativePrefix(fmt));
                    String decimalRegex = BigDecimalMatcher.createDecimalRegex(fmt);
                    String negativeSuffix =
                        StringUtil.escapeRegEx(getNegativeSuffix(fmt));
                    String matchingNegativesRegex = StringUtil.strcat(
                        negativePrefix, "(", decimalRegex, ")", negativeSuffix);
                    matchingNegativesPattern = Pattern.compile(matchingNegativesRegex);
                    __matchingNegativesPatterns.put(fmt, matchingNegativesPattern);
                }
                string = matchingNegativesPattern.matcher(string).replaceAll("-$1");
            }

            return string;
        }


        ////////////////////////////////////////////
        // Helpers to strictParse
        ////////////////////////////////////////////


        private static DecimalParseInfo _strictParse (String string, DecimalFormat fmt)
            throws ParseException
        {
            // guard
            Assert.that(null != fmt, "Bad DecimalFormat [%s].", fmt);
            if (string == null) {
                string = "";
            }

            // get and verify matcher
            BigDecimalMatcher matcher = new BigDecimalMatcher(string, fmt);

            // if matches exponential case then process it differently
            DecimalParseInfo decimalParseInfo;
            if (matcher.isExponential())
            {
                decimalParseInfo = _parseExponentialBigDecimal(fmt, matcher);
            }
            else {
                /* create a new DecimalParseInfo */

                // how many integer characters are there, ie "100.0" would have 3
                int integerDigitsCount = matcher._getIntegerDigitsCount();

                // how many decimal digits there are, ie "100.0" would have 1
                int decimalDigitsCount = matcher._getDecimalDigitsCount();

                // the string index of the decimal location, ie "100.0" would have index 3
                int decimalLocation = matcher._getDecimalLocation();

                // whatever is before the numbers (besides the negative prefix),
                // ie "$100" would be "$"
                String prefixString = matcher.getPrefixString();

                // whatever is after the numbers, ie "1 abcd" would be "abcd"
                String suffixString = matcher.getSuffixString();

                // the actual number that needs to be represented, a negative value would
                // be represented here
                BigDecimal amount = matcher.getAmount();

                // create our final object and do final adjustments
                decimalParseInfo = new DecimalParseInfo(integerDigitsCount,
                                                        decimalDigitsCount,
                                                        decimalLocation,
                                                        prefixString,
                                                        suffixString,
                                                        amount);
            }
            _moveStrandedNegativePrefixToPrefixString(decimalParseInfo, matcher, fmt);
            _moveStrandedNegativeSuffixToSuffixString(decimalParseInfo, matcher, fmt);
            return decimalParseInfo;
        }

        private static DecimalParseInfo _parseExponentialBigDecimal (
            DecimalFormat fmt,
            BigDecimalMatcher matcher)
                throws ParseException
        {
            BigDecimal exponentialAmount = matcher.getAmount();
            String exponentialString = matcher.getExponentialString();
            String completeExponentialString = StringUtil.strcat(
                exponentialAmount.toPlainString(), exponentialString);
            DecimalParseInfo exponentialInfo =
                parseExponentialBigDecimal(completeExponentialString, fmt);
            String exponentialPrefixString = matcher.getPrefixString();
            String exponentialSuffixString = matcher.getSuffixString();
            return new DecimalParseInfo(exponentialInfo.integerDigits,
                                        exponentialInfo.decimalDigits,
                                        exponentialInfo.decimalLocation,
                                        exponentialPrefixString,
                                        exponentialSuffixString,
                                        exponentialInfo.number);
        }

        private static void _moveStrandedNegativePrefixToPrefixString (
            DecimalParseInfo info,
            BigDecimalMatcher matcher,
            DecimalFormat fmt)
        {
            boolean hasSingleNegative = !_formatHasMatchingNegatives(fmt);
            if (hasSingleNegative) {
                // nothing to do, stranded negative needs two sides in the format
                return;
            }

            if (matcher.hasStrandedNegativePrefix()) {
                info.prefix = StringUtil.strcat(info.prefix, matcher.getNegativePrefix());
                matcher.removeNegativePrefix();
            }
        }

        private static void _moveStrandedNegativeSuffixToSuffixString (
            DecimalParseInfo info,
            BigDecimalMatcher matcher,
            DecimalFormat fmt)
        {
            if (!_formatHasMatchingNegatives(fmt)) {
                // nothing to do
                return;
            }
            
            if (matcher.hasStrandedNegativeSuffix()) {
                info.suffix = StringUtil.strcat(matcher.getNegativeSuffix(), info.suffix);
                matcher.removeNegativeSuffix();
            }
        }


        //////////////////////////////////////////////////////////////////////////////////
        // DecimalFormat Helpers
        //////////////////////////////////////////////////////////////////////////////////


        /**
         * Multiple helper
         * @param fmt
         * @return Always length 1 or ""
         */
        private static String _getGroupingSeparator (DecimalFormat fmt)
        {
            DecimalFormatSymbols symbols = fmt.getDecimalFormatSymbols();
            char chGroupingSeparator = symbols.getGroupingSeparator();

            // guard
            if (chGroupingSeparator == ' ') {
                return "";
            }

            String stringGroupingSeparator = Character.toString(chGroupingSeparator);
            return StringUtil.escapeRegEx(stringGroupingSeparator);
        }

        /** create pattern helper */
        private static String getNegativePrefix (DecimalFormat fmt)
        {
            // The negativePrefix will typically be ( or -, as in (100) or -100.
            String negativePrefix = fmt.getNegativePrefix();

            // verify assumptions
            boolean negativePrefixIsValid = negativePrefix.length() == 1
                || negativePrefix.length() == 0;
            Assert.that(negativePrefixIsValid, "Unexpected negativePrefix: [%s].",
                        negativePrefix);

            return negativePrefix;
        }

        /**
         * Create pattern helper.
         * @param fmt
         * @return Length 1 string or ""
         */
        private static String _getDecimalSeparator (DecimalFormat fmt)
        {
            DecimalFormatSymbols symbols = fmt.getDecimalFormatSymbols();

            // For the string "1,234.56" '.' is the decimal separator.
            char chDecimalSeparator = symbols.getDecimalSeparator();

            // guard
            if (chDecimalSeparator == ' ') {
                return "";
            }

            String stringDecimalSeparator = Character.toString(chDecimalSeparator);
            return StringUtil.escapeRegEx(stringDecimalSeparator);
        }

        /**
         * Create pattern helper
         * @param fmt
         * @return Nonnull string, may be of 0 length
         */
        private static String getNegativeSuffix (DecimalFormat fmt)
        {
            // The negative suffix will typically be ) or '', as in (100) or -100.
            // Something like 100- is allowed in some locals as well though.
            String negativeSuffix = fmt.getNegativeSuffix();

            // guard
            if (StringUtil.nullOrEmptyOrBlankString(negativeSuffix)) {
                return "";
            }

            // body
            negativeSuffix = negativeSuffix.trim();

            // verify assumptions
            Assert.that(negativeSuffix.length() == 1, "Unexpected negativeSuffix: [%s].",
                        negativeSuffix);

            return negativeSuffix;
        }

        private static boolean _formatHasMatchingNegatives (DecimalFormat fmt)
        {
            boolean negativePrefixExists =
                    !StringUtil.nullOrEmptyOrBlankString(getNegativePrefix(fmt));
            boolean negativeSuffixExists =
                    !StringUtil.nullOrEmptyOrBlankString(getNegativeSuffix(fmt));
            return negativePrefixExists && negativeSuffixExists;
        }


        //////////////////////////////////////////////////////////////////////////////////
        // Debugging
        //////////////////////////////////////////////////////////////////////////////////

        private static boolean __debugging = false;

        private static void enableDebug ()
        {
            __debugging = true;
        }

        private static void _assertValidRegex (String regex, int expectedGroupCount)
        {
            if (!__debugging) {
                return;
            }

            Pattern pattern = Pattern.compile(regex);
            int actualGroupCount = pattern.matcher("").groupCount();
            Assert.that(actualGroupCount == expectedGroupCount,
                        "Unexpected regex group count, was expecting [%s] but got [%s]" +
                                "for regex [%s].",
                        expectedGroupCount,
                        actualGroupCount,
                        regex);
        }


        //////////////////////////////////////////////////////////////////////////////////
        // BigDecimalMatcher
        //////////////////////////////////////////////////////////////////////////////////


        /**
         * This class does a few things with the matcher.  This class will create a regex,
         *  create a (cached) pattern for this regex, recover from a couple of edge-case
         *  error states, verify that no other error states exist, and then provide
         *  getters at a good level of abstraction (with methods like getDecimals()
         *  instead of group(7)).
         */
        public static class BigDecimalMatcher
        {
            Matcher matcher;
            DecimalFormat format;

            //////////////////////////////////////////////////////////////////////////////
            // "Public" Methods
            //////////////////////////////////////////////////////////////////////////////

            private BigDecimalMatcher (String string, DecimalFormat fmt)
                throws ParseException
            {
                format = fmt;
                matcher = _getPattern().matcher(string);
                if (!matcher.matches()) {
                    throw makeParseException(NumberFormatErrorKey, 0);
                }
                _fixEmptyExponent();
                _fixStrandedExponentialNegativeSuffix();
                _assertValid();
            }

            private static String createDecimalRegex (DecimalFormat fmt)
            {
                // separated integer digits regex
                String groupingSize = Integer.toString(fmt.getGroupingSize());
                //For the string "1,234.56" ',' is the grouping separator.
                String groupSep = StringUtil.strcat("[", _getGroupingSeparator(fmt), "]");
                String leadGroup = StringUtil.strcat("\\d{1,", groupingSize, "}");
                String integerDigitsSeparatedRegex = StringUtil.strcat(
                        "(", leadGroup, "(?:", groupSep, "\\d{", groupingSize, "})+)");
                _assertValidRegex(integerDigitsSeparatedRegex, 1);

                // together integer digits regex
                String integerDigitsTogetherRegex = "(\\d+)";
                _assertValidRegex(integerDigitsTogetherRegex, 1);

                // complete integer digits regex
                // keeping the parenthesis on the same line for ease of matching them
                String optionalPlus = "[+]?";
                String integerDigitsRegex = StringUtil.strcat(
                    optionalPlus,
                    "(?:", integerDigitsSeparatedRegex, "|", integerDigitsTogetherRegex, ")?");
                _assertValidRegex(integerDigitsRegex, 2);


                /* decimal fraction */
                String decimalFractionRegex = _createDecimalFractionRegex(fmt);

                String decimalRegex =
                        StringUtil.strcat(integerDigitsRegex, decimalFractionRegex);
                _assertValidRegex(decimalRegex, 4);
                return decimalRegex;
            }

            private boolean isExponential ()
            {
                String exponentialString = getExponentialString();
                boolean exponential =
                    !StringUtil.nullOrEmptyOrBlankString(exponentialString);
                return exponential;
            }

            private boolean hasStrandedNegativePrefix ()
            {
                boolean formatHasMatchingNegatives = _formatHasMatchingNegatives(format);

                String negativePrefix = getNegativePrefix();
                boolean negativePrefixExists =
                        !StringUtil.nullOrEmptyOrBlankString(negativePrefix);

                boolean canonicalNegativePrefix =
                        CanonicalNegativePrefix.equals(negativePrefix);

                String negativeSuffix = getNegativeSuffix();
                boolean negativeSuffixExists =
                        !StringUtil.nullOrEmptyOrBlankString(negativeSuffix);

                return formatHasMatchingNegatives
                    && negativePrefixExists
                    && !canonicalNegativePrefix
                    && !negativeSuffixExists;
            }

            private boolean hasStrandedNegativeSuffix ()
            {
                boolean formatHasMatchingNegatives = _formatHasMatchingNegatives(format);

                String negativePrefix = getNegativePrefix();
                boolean negativePrefixExists =
                        !StringUtil.nullOrEmptyOrBlankString(negativePrefix);

                String negativeSuffix = getNegativeSuffix();
                boolean negativeSuffixExists =
                        !StringUtil.nullOrEmptyOrBlankString(negativeSuffix);

                return formatHasMatchingNegatives
                    && !negativePrefixExists
                    && negativeSuffixExists;
            }

            
            //////////////////////////////////////////////////////////////////////////////
            // Helpers
            //////////////////////////////////////////////////////////////////////////////


            /**
             * EmptyExponent: an exponent sign w/o digits.
             */
            private void _fixEmptyExponent ()
            {
                String exponentialString = getExponentialString();
                boolean exponentExists =
                    !StringUtil.nullOrEmptyOrBlankString(exponentialString);
                boolean isEmptyExponent =
                    exponentExists
                    && exponentialString.length() == 1;
                boolean notEmptyExponent = !exponentExists || !isEmptyExponent;
                if (notEmptyExponent) {
                    return;
                }

                __ignoreExponent = true;
                __additionalSuffixString = exponentialString;
            }

            /**
             * StrandedExponentialNegativeSuffix: A negative suffix in the exponential
             * group w/o a negative prefix there.  It probably belongs to the full string
             * as in the case of "(1e1)".
             */
            private void _fixStrandedExponentialNegativeSuffix () throws ParseException
            {
                // guard: do we have an exponential string to test?
                String exponentialString = getExponentialString();
                boolean exponentExists =
                    !StringUtil.nullOrEmptyOrBlankString(exponentialString);
                if (!exponentExists) {
                    return;
                }

                // guard: do we have matching negatives?
                boolean matchingNegativesExpected = _formatHasMatchingNegatives(format);
                if (!matchingNegativesExpected) {
                    return;
                }

                // guard: do we have something besides a stranded negative suffix?
                String exponentialNegativePrefix = getExponentialNegativePrefix();
                boolean exponentialNegativePrefixExists =
                    !StringUtil.nullOrEmptyOrBlankString(exponentialNegativePrefix);
                String exponentialNegativeSuffix = getExponentialNegativeSuffix();
                boolean exponentialNegativeSuffixExists =
                    !StringUtil.nullOrEmptyOrBlankString(exponentialNegativeSuffix);
                boolean strandedExponentialNegativeSuffixExists =
                    !exponentialNegativePrefixExists
                    && exponentialNegativeSuffixExists;
                if (!strandedExponentialNegativeSuffixExists) {
                    // has matching prefix, not stranded
                    return;
                }

                // body

                // remove from exponent
                removeExponentialNegativeSuffix();
                // add to regular string
                addNegativeSuffix();

            }


            ////////////////////////////////////////////////////////////
            // getPattern and helpers (all static)
            ////////////////////////////////////////////////////////////


            private static Map<DecimalFormat, Pattern> __patterns =
                new ConcurrentHashMap();

            /**
             * getPattern and all of it's regex generating helpers are tightly bound
             * (couldn't be helped) to the _GroupNames enum.  More specifically if the number
             * or position of capturing regex groups changes then the enum must be updated
             * as well.
             *
             * @see ariba.util.formatter.BigDecimalFormatter.BigDecimalParser.BigDecimalMatcher._GroupNames
             *
             * @return
             */
            private Pattern _getPattern ()
            {
                if (__patterns.containsKey(format)) {
                    return __patterns.get(format);
                }

                Pattern pattern = __createPattern();
                __patterns.put(format, pattern);

                return pattern;
            }
            
            /** Helper to getPattern() */
            private Pattern __createPattern ()
            {
                /*
                   High Level Summary - Regex Psudocode:
                    Start,
                    prefixString: not CanonicalNegativePrefix or negativePrefix or number,
                    isNegative: optional CanonicalNegativePrefix, negativePrefix,
                        plus sign, or whitespace
                    integerDigits: number{1-3}(groupSep number{3})*|number*,
                    optional decimalSeparator,
                    decimalDigits: number*, whitespace*,
                    negativeSuffix: from DecimalFormat
                    suffixString: notNumber*
                    end
                 */


                // create actual regexes

                // prefixString
                String notDigit = "([^\\d]*?)";
                String prefixStringRegex = StringUtil.strcat("^", notDigit);
                _assertValidRegex(prefixStringRegex, 1);

                String decimalRegex = __createNegativeDecimalRegex();
                _assertValidRegex(decimalRegex, 7);

                String exponentialRegex = StringUtil.strcat("([eE]", decimalRegex, ")?");
                _assertValidRegex(exponentialRegex, 8);

                String suffixStringRegex = StringUtil.strcat(notDigit,"$");
                _assertValidRegex(suffixStringRegex, 1);

                // final
                String regex = StringUtil.strcat(
                        prefixStringRegex, decimalRegex, exponentialRegex, suffixStringRegex);
                _assertValidRegex(regex, 17);

                Log.util.debug("Created the regex [%s] for the DecimalFormat [%s].",
                               regex,
                               format);

                return Pattern.compile(regex);
            }

            /** createPattern() helper */
            private String __createNegativeDecimalRegex ()
            {
                //  negativePrefix
                String prefixes = CanonicalNegativePrefix;
                String formatPrefix = BigDecimalParser.getNegativePrefix(format);
                if (!CanonicalNegativePrefix.equals(formatPrefix)) {
                    prefixes += formatPrefix;
                }
                String negativePrefixRegex = StringUtil.strcat("([", prefixes, "])");
                String stringPrefixRegex2 =
                    StringUtil.strcat("([^\\d", prefixes, "]*?)");
                String prefixRegex = StringUtil.strcat(
                    "(?:", negativePrefixRegex, stringPrefixRegex2, ")?\\s*");
                _assertValidRegex(prefixRegex, 2);


                //  negativeSuffix
                String negativeSuffix = BigDecimalParser.getNegativeSuffix(format);
                String negativeSuffixRegex = "()";
                if (! StringUtil.nullOrEmptyOrBlankString(negativeSuffix)) {
                    negativeSuffixRegex = StringUtil.strcat(
                        "\\s*([", negativeSuffix, "])?");
                }
                _assertValidRegex(negativeSuffixRegex, 1);

                // decimal regex
                String decimalRegex = createDecimalRegex(format);
                _assertValidRegex(decimalRegex, 4);

                // final decimal regex
                String negativeDecimalRegex = StringUtil.strcat(
                        prefixRegex, decimalRegex, negativeSuffixRegex);
                _assertValidRegex(negativeDecimalRegex, 7);
                return negativeDecimalRegex;
            }

            /** Helper to createDecimalRegex */
            private static String _createDecimalFractionRegex (DecimalFormat format)
            {
                //  decimalSeparator
                String decimalSeparatorRegex = StringUtil.strcat(
                        "(", BigDecimalParser._getDecimalSeparator(format), ")");
                _assertValidRegex(decimalSeparatorRegex, 1);

                //  decimalDigits
                String decimalDigitsRegex = "(\\d*)";
                _assertValidRegex(decimalDigitsRegex, 1);

                String decimalFractionRegex = StringUtil.strcat(
                        "(?:", decimalSeparatorRegex, decimalDigitsRegex, ")?");
                _assertValidRegex(decimalFractionRegex, 2);
                return decimalFractionRegex;
            }


            /////////////////////////////////////////////////////////////////
            // assertValid and friends
            /////////////////////////////////////////////////////////////////


            private void _assertValid () throws ParseException
            {
                // guard
                if (!matcher.matches()) {
                    throw makeParseException(NumberFormatErrorKey, 1);
                }

                // body
                __assertValidIntegerDigits();
                __assertValidNegativePrefix(getNegativePrefix());
                __assertValidNegativeSuffix(getNegativeSuffix());
                __assertValidExponential();

            }
            
            private void __assertValidExponential ()
                throws ParseException
            {
                // if all are null then there is no exponential notation
                String exponent = getExponentialString();
                boolean noExponent = StringUtil.nullOrEmptyOrBlankString(exponent);
                if (noExponent) {
                    // nothing to check, valid
                    return;
                }

                __assertValidExponentialDigits();
                __assertValidExponentialNegatives();

                String exponentPrefix = getExponentialPrefixString2();
                boolean exponentialPrefixExists =
                    !StringUtil.nullOrEmptyOrBlankString(exponentPrefix);
                if (exponentialPrefixExists) {
                    throw makeParseException(NumberFormatErrorKey, 1);
                }
            }

            private void __assertValidExponentialNegatives () throws ParseException
            {
                // ensure that they are individually correct
                String actualNegativePrefix = getExponentialNegativePrefix();
                __assertValidNegativePrefix(actualNegativePrefix);

                String actualNegativeSuffix = getExponentialNegativeSuffix();
                __assertValidNegativeSuffix(actualNegativeSuffix);

                // make sure negative prefix and suffix match if needed
                boolean balancedNegativesExpected = _formatHasMatchingNegatives(format);
                if (balancedNegativesExpected) {
                    boolean negativePrefixExists = !StringUtil.nullOrEmptyOrBlankString(
                        actualNegativePrefix);
                    boolean negativeSuffixExists = !StringUtil.nullOrEmptyOrBlankString(
                        actualNegativeSuffix);
                    boolean balancedNegatives =
                        negativePrefixExists && negativeSuffixExists;
                    boolean noNegatives =
                        !negativePrefixExists && !negativeSuffixExists;
                    boolean canonicalNegative =
                        negativePrefixExists
                        && !negativeSuffixExists
                        && actualNegativePrefix.equals(CanonicalNegativePrefix);
                    boolean validNegatives =
                        balancedNegatives || noNegatives || canonicalNegative;
                    if (!validNegatives) {
                        throw makeParseException(NumberFormatErrorKey, 1);
                    }
                }
            }

            /** Helper to assertValid and assertValidExponential */
            private void __assertValidNegativePrefix (String actualNegativePrefix)
                    throws ParseException
            {
                // negative prefix should either be blank, canonical or the format's

                // null or empty string is valid
                boolean negativePrefixExists =
                        !StringUtil.nullOrEmptyOrBlankString(actualNegativePrefix);
                if (!negativePrefixExists) {
                    return;
                }

                // check if canonical
                boolean notCanonical =
                    !actualNegativePrefix.equals(CanonicalNegativePrefix);

                // check if it's the formatter's prefix
                String expectedNegativePrefix = format.getNegativePrefix();
                boolean notInFormat = !actualNegativePrefix.equals(expectedNegativePrefix);

                // if neither, error
                if (notCanonical && notInFormat) {
                    throw makeParseException(NumberFormatErrorKey, 1);
                }
            }

            /** Helper to assertValid and assertValidExponential */
            private void __assertValidNegativeSuffix (String actualNegativeSuffix)
                    throws ParseException
            {
                // negative suffix should either be nothing or the match the DecimalFormat

                // check for negative suffix
                boolean negativeSuffixExists =
                    !StringUtil.nullOrEmptyOrBlankString(actualNegativeSuffix);
                if (!negativeSuffixExists) {
                    return;
                }

                String expectedNegativeSuffix = format.getNegativeSuffix();
                boolean notInFormat =
                    !actualNegativeSuffix.equals(expectedNegativeSuffix);
                if (notInFormat) {
                    throw makeParseException(NumberFormatErrorKey, 1);
                }
            }

            private void __assertValidIntegerDigits () throws ParseException
            {
                int integerDigitsCount = _getIntegerDigitsCount();

                String decimalDigits = getDecimalDigits();

                __assertValidDigits(integerDigitsCount, decimalDigits);
            }

            private void __assertValidExponentialDigits ()
                throws ParseException
            {
                int integerDigitsCount = _getExponentialIntegerDigitsCount();

                String decimalDigits = getExponentialDecimalDigits();

                __assertValidDigits(integerDigitsCount, decimalDigits);
            }

            private static void __assertValidDigits (
                int integerDigitsCount,
                String decimalDigits)
                    throws ParseException
            {
                //  sanity check: either integerDigits or decimalDigits has a value

                // check integer digits
                boolean integerDigitsExist = integerDigitsCount != 0;

                // check decimalDigits
                boolean decimalDigitsExist =
                        !StringUtil.nullOrEmptyOrBlankString(decimalDigits);

                boolean inErrorState = !integerDigitsExist && !decimalDigitsExist;
                if (inErrorState)
                {
                    ParseException parseException =
                        makeParseException(NumberFormatErrorKey, 1);
                    ParseException causingException =
                        new ParseException("Valid digits not found", 1);
                    
                    parseException.initCause(causingException);
                    throw parseException;
                }
            }

            
            //////////////////////////////////////////////////////////////////////////////
            // Matcher Accessors, also "public"
            //////////////////////////////////////////////////////////////////////////////


            private enum _GroupNames {
                Start,
                PrefixString,
                NegativePrefix,
                PrefixString2,
                IntegerDigitsSeparated,
                IntegerDigitsTogether,
                DecimalSeparator,
                DecimalDigits,
                NegativeSuffix,
                Exponent,
                ExponentialNegativePrefix,
                ExponentialPrefixString2,
                ExponentialIntegerDigitsSeparated,
                ExponentialIntegerDigitsTogether,
                ExponentialDecimalPoint,
                ExponentialDecimalDigits,
                ExponentialNegativeSuffix,
                SuffixString};

            private boolean __ignoreNegativePrefix = false;

            private boolean __ignoreExponent = false;

            private boolean __ignoreExponentialNegativeSuffix = false;

            private boolean __ignoreNegativeSuffix = false;

            private String __additionalSuffixString = "";

            private String getPrefixString ()
            {
                String firstPrefixString =
                    matcher.group(_GroupNames.PrefixString.ordinal());
                String secondPrefixString =
                    matcher.group(_GroupNames.PrefixString2.ordinal());
                return StringUtil.strcat(firstPrefixString, secondPrefixString);
            }

            private BigDecimal getAmount ()
            {
                // get component strings
                String effectiveNegativePrefix = __getEffectiveNegativePrefix();
                String integerDigits = getIntegerDigits();
                String decimalSeparator = CanonicalDecimalSeparatorString;
                String decimalDigits = getDecimalDigits();

                // create final string value
                String amountString = StringUtil.strcat(effectiveNegativePrefix,
                                                        integerDigits,
                                                        decimalSeparator,
                                                        decimalDigits);

                // convert final string value
                return new BigDecimal(amountString);
            }

            private void removeNegativePrefix ()
            {
                __ignoreNegativePrefix = true;
            }

            private String getNegativePrefix ()
            {
                String negativePrefix =
                    matcher.group(_GroupNames.NegativePrefix.ordinal());

                return __ignoreNegativePrefix ? "" : negativePrefix;
            }

            private String getIntegerDigits ()
            {
                String integerDigits = __getIntegerDigitsTogether();

                String integerDigitsSeparated = __getIntegerDigitsSeparated();

                String groupSep = _getGroupingSeparator(format);

                return __getDigits(integerDigits, integerDigitsSeparated, groupSep);
            }

            private String getDecimalSeparator ()
            {
                return matcher.group(_GroupNames.DecimalSeparator.ordinal());
            }

            private String getDecimalDigits ()
            {
                return matcher.group(_GroupNames.DecimalDigits.ordinal());
            }

            private void removeNegativeSuffix ()
            {
                __ignoreNegativeSuffix = true;
            }

            private String __negativeSuffix;

            private void addNegativeSuffix () throws ParseException
            {
                boolean negativeSuffixExists =
                    !StringUtil.nullOrEmptyOrBlankString(__negativeSuffix);
                if (negativeSuffixExists) {
                    throw makeParseException(NumberFormatErrorKey, 1);
                }
                __negativeSuffix = format.getNegativeSuffix();
            }

            private String getNegativeSuffix ()
            {
                if (__negativeSuffix == null) {
                    __negativeSuffix =
                        matcher.group(_GroupNames.NegativeSuffix.ordinal());
                }

                return __ignoreNegativeSuffix ? "" : __negativeSuffix;
            }

            private String __exponentialString;

            private String getExponentialString ()
            {
                if (__exponentialString == null) {
                    __exponentialString = matcher.group(_GroupNames.Exponent.ordinal());
                }
                return __ignoreExponent ? "" : __exponentialString;
            }

            private String getExponentialNegativePrefix ()
            {
                return matcher.group(_GroupNames.ExponentialNegativePrefix.ordinal());
            }

            private String getExponentialPrefixString2 ()
            {
                return matcher.group(_GroupNames.ExponentialPrefixString2.ordinal());
            }

            private String getExponentialIntegerDigits ()
            {
                String exponentialIntegerDigits =
                    __getExponentialIntegerDigitsTogether();

                String exponentialIntegerDigitsSeparated =
                    __getExponentialIntegerDigitsSeparated();

                String groupSep = _getGroupingSeparator(format);

                return __getDigits(exponentialIntegerDigits,
                                   exponentialIntegerDigitsSeparated,
                                   groupSep);
            }

            private String getExponentialDecimalDigits ()
            {
                return matcher.group(_GroupNames.ExponentialDecimalDigits.ordinal());
            }

            private void removeExponentialNegativeSuffix ()
            {
                __ignoreExponentialNegativeSuffix = true;
                String exponentialString = getExponentialString();
                boolean exponentialNegativeSuffixExists = exponentialString.endsWith(
                    BigDecimalParser.getNegativeSuffix(format));
                Assert.that(exponentialNegativeSuffixExists,
                            "Trying to remove non-existent exponential negative suffix.");

                int negativeSuffixIndex = exponentialString.length() - 1;
                __exponentialString = exponentialString.substring(0, negativeSuffixIndex);
            }

            private String getExponentialNegativeSuffix ()
            {
                String exponentialNegativeSuffix =
                    matcher.group(_GroupNames.ExponentialNegativeSuffix.ordinal());
                return __ignoreExponentialNegativeSuffix ? "" : exponentialNegativeSuffix;
            }

            private String getSuffixString ()
            {
                String orig = matcher.group(_GroupNames.SuffixString.ordinal());
                return StringUtil.strcat(__additionalSuffixString, orig);
            }


            //////////////////////////////////////////////////////////////////////////////
            // Matcher Accessor Helpers
            //////////////////////////////////////////////////////////////////////////////


            private static Map<String, Pattern> __groupSeparatorPatternCache =
                new ConcurrentHashMap();

            private String __getEffectiveNegativePrefix ()
            {
                // check if canonically negative
                String actualNegativePrefix = getNegativePrefix();
                final boolean actualNegativePrefixExists =
                        !StringUtil.nullOrEmptyOrBlankString(actualNegativePrefix);

                String actualNegativeSuffix = getNegativeSuffix();
                final boolean actualNegativeSuffixExists =
                        !StringUtil.nullOrEmptyOrBlankString(actualNegativeSuffix);

                boolean canonicallyNegative =
                        CanonicalNegativePrefix.equals(actualNegativePrefix)
                        && !actualNegativeSuffixExists;

                // check if negative from format's prefix
                String formatNegativePrefix = BigDecimalParser.getNegativePrefix(format);
                final boolean formatNegativePrefixExists =
                        !StringUtil.nullOrEmptyOrBlankString(formatNegativePrefix);
                final boolean hasFormatNegativePrefix =
                        actualNegativePrefixExists
                        && formatNegativePrefixExists
                        && actualNegativePrefix.equals(formatNegativePrefix);

                String formatNegativeSuffix = BigDecimalParser.getNegativeSuffix(format);
                final boolean formatNegativeSuffixExists =
                        !StringUtil.nullOrEmptyOrBlankString(formatNegativeSuffix);
                final boolean validEmptySuffix =
                        !actualNegativeSuffixExists && !formatNegativeSuffixExists;

                final boolean negativeFromFormatNegativePrefix =
                        hasFormatNegativePrefix && validEmptySuffix;

                // check if negative from format's suffix
                final boolean validEmptyPrefix =
                        !actualNegativePrefixExists && !formatNegativePrefixExists;
                final boolean hasFormatNegativeSuffix =
                        actualNegativeSuffixExists
                        && formatNegativeSuffixExists
                        && actualNegativeSuffix.equals(formatNegativeSuffix);
                boolean negativeFromFormatNegativeSuffix =
                        validEmptyPrefix && hasFormatNegativeSuffix;

                // check if negative from format's prefix and suffix
                boolean negativeFromFormatNegativePrefixAndSuffix =
                        hasFormatNegativePrefix && hasFormatNegativeSuffix;

                // check if negative
                boolean isNegative =
                        canonicallyNegative
                        || negativeFromFormatNegativePrefix
                        || negativeFromFormatNegativeSuffix
                        || negativeFromFormatNegativePrefixAndSuffix;

                // get canonical negative prefix
                String effectiveNegativePrefix = "";
                if (isNegative) {
                    effectiveNegativePrefix = CanonicalNegativePrefix;
                }
                return effectiveNegativePrefix;
            }

            private int _getIntegerDigitsCount () throws ParseException
            {
                String integerDigits = getIntegerDigits();

                boolean integerDigitsExist =
                    !StringUtil.nullOrEmptyOrBlankString(integerDigits);

                return integerDigitsExist ? integerDigits.length() : 0;
            }

            private String __getIntegerDigitsSeparated ()
            {
                return matcher.group(_GroupNames.IntegerDigitsSeparated.ordinal());
            }

            private String __getIntegerDigitsTogether ()
            {
                return matcher.group(_GroupNames.IntegerDigitsTogether.ordinal());
            }

            private int _getDecimalDigitsCount ()
            {
                String decimalDigits = getDecimalDigits();

                boolean decimalDigitsExist =
                    !StringUtil.nullOrEmptyOrBlankString(decimalDigits);

                return decimalDigitsExist ? decimalDigits.length() : 0;
            }

            private int _getDecimalLocation ()
            {
                return matcher.start(_GroupNames.DecimalSeparator.ordinal());
            }
            
            private int _getExponentialIntegerDigitsCount ()
            {
                String exponentialIntegerDigits = getExponentialIntegerDigits();

                boolean exponentialIntegerDigitsExist =
                    !StringUtil.nullOrEmptyOrBlankString(exponentialIntegerDigits);

                return exponentialIntegerDigitsExist ?
                        exponentialIntegerDigits.length()
                        : 0;
            }

            private String __getExponentialIntegerDigitsTogether ()
            {
                int groupNumber = _GroupNames.ExponentialIntegerDigitsTogether.ordinal();
                return matcher.group(groupNumber);
            }

            private String __getExponentialIntegerDigitsSeparated ()
            {
                int groupNumber = _GroupNames.ExponentialIntegerDigitsSeparated.ordinal();
                return matcher.group(groupNumber);
            }
            
            private static String __getDigits (
                String togetherDigits,
                String separatedDigits,
                String groupSep)
            {
                // get integer digits
                String digits = togetherDigits;
                boolean separatedDigitsExist =
                    !StringUtil.nullOrEmptyOrBlankString(separatedDigits);
                if (separatedDigitsExist) {
                    Pattern pattern = __groupSeparatorPatternCache.get(groupSep);
                    boolean cacheMiss = pattern == null;
                    if (cacheMiss) {
                        pattern = Pattern.compile(groupSep);
                        __groupSeparatorPatternCache.put(groupSep, pattern);
                    }
                    digits = pattern.matcher(separatedDigits).replaceAll("");
                }
                return digits;

            }

        }
    }

    /**
        Returns a BigDecimal for this <code>object</code> using the default
        locale.  If <code>object</code> is not a BigDecimal, the object is converted
        to a string and parsed.  If there is a problem parsing the string, or
        the value is null, we return zero as a default value.

        @param object If object is not a BigDecimal, the object is converted to a string and parsed.
        @return Returns a BigDecimal for the object
        @aribaapi documented
    */
    public static BigDecimal getBigDecimalValue (Object object)
    {
        return getBigDecimalValue(object, getDefaultLocale());
    }

    /**
        Returns a BigDecimal for this <code>object</code> using this
        <code>locale</code>.  If <code>object</code> is not a BigDecimal, the object is
        converted to a string and parsed.  If there is a problem parsing the
        string, or the value is null, we return zero as a default value.

        @param object The object to convert to a <code>BigDecimal</code>.
        @param locale <code>Locale</code> to use to parse the
                      <code>object</code> if necessary.

        @return Returns a <code>BigDecimal</code> corresponding to the
                <code>object</code> parameter.
        @aribaapi documented
    */
    public static BigDecimal getBigDecimalValue (Object object, Locale locale)
    {
        if (object == null) {
            return Constants.ZeroBigDecimal;
        }
        else if (object instanceof Double) {
            try {
                return new BigDecimal(object.toString());
            }
            catch (NumberFormatException e) {
                return Constants.ZeroBigDecimal;
            }
        }
        else if (object instanceof BigDecimal) {
            return (BigDecimal)object;
        }
        else {
            object = object.toString();
            try {
                return parseBigDecimal((String)object, locale);
            }
            catch (ParseException e) {
                return Constants.ZeroBigDecimal;
            }
        }
    }

    /**
        Returns whether this <code>object</code> can be parsed as a BigDecimal using
        the default locale.  If <code>object</code> is not a BigDecimal, the object is
        converted to a string and parsed.  If there is a problem parsing the string, we
        return false.

        @param object If object is not a BigDecimal, the object is converted to a string and parsed.
        @return Returns true if the object can be parsed as a BigDecimal
        @aribaapi documented
    */
    public static boolean canParseBigDecimalValue (Object object)
    {
        return canParseBigDecimalValue(object, getDefaultLocale());
    }

    /**
        Returns wheteher this <code>object</code> can be parsed as a BigDecimal using
        the default locale.  If <code>object</code> is not a BigDecimal, the object is
        converted to a string and parsed.  If there is a problem parsing the string, we
        return false.

        @param object If object is not a BigDecimal, the object is converted to a string and parsed.
        @param locale <code>Locale</code> to use to parse the
                      <code>object</code> if necessary.
        @return Returns true if the object can be parsed as a BigDecimal
        @aribaapi documented
    */
    public static boolean canParseBigDecimalValue (Object object, Locale locale)
    {
        if (object == null) {
            return false;
        }
        else if ((object instanceof Double) ||
            (object instanceof BigDecimal)) {
            return true;
        }
        else {
            object = object.toString();
            try {
                BigDecimal d = parseBigDecimal((String)object, locale);
            }
            catch (ParseException e) {
                return false;
            }
            return true;
        }
    }

    /**
        Returns a BigDecimal for this <code>object</code>.  If <code>object</code> is
        not a BigDecimal, the object is converted to a string and parsed.  If
        there is a problem parsing the string, or the value is null, we return
        zero as a default value.
        @param value a double for which a <code>BigDecimal</code> is returned
        @return Returns a BigDecimal for the double
        @aribaapi documented
    */
    public static BigDecimal getBigDecimalValue (double value)
    {
        // use String representation of value to achieve expected results.  From javadoc
        // for BigDecimal double constructor "Note: the results of this constructor
        // can be somewhat unpredictable."
        BigDecimal bd = new BigDecimal(String.valueOf(value));
        return bd;
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for this <code>object</code>.

        @param object The object to be formatted into a string.  Must be a
                      <code>BigDecimal</code>.
        @param locale The locale to use for retrieving localized symbols needed
                      for building the string representation.

        @return The string value of the <code>object</code> parameter.
        @aribaapi documented
    */
    protected String formatObject (Object object, Locale locale)
    {
        Assert.that(object instanceof BigDecimal, "invalid type");
        return getStringValue((BigDecimal)object, locale);
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Parses this <code>string</code> to create a new BigDecimal object.
        <p>
        If parsing is unsuccessful, returns a zero.

        @param string The string to parse.
        @param locale The locale to use for retrieving localized symbols needed
                      for building the string representation.

        @return The <code>BigDecimal</code> corresponding to the string.
        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
    {
        try {
            return parseBigDecimal(string, locale);
        }
        catch (ParseException e) {
            return Constants.ZeroBigDecimal;
        }
    }

    /**
        Returns a BigDecimal for this <code>object</code> using this
        <code>locale</code>.  The meat of this method is implemented
        in <code>bigDecimalValue(object, locale)</code>.

        @param object The object to convert to a <code>BigDecimal</code>.
        @param locale <code>Locale</code> to use to parse the
                      <code>object</code> if necessary.

        @return Returns a <code>BigDecimal</code> corresponding to the
                <code>object</code> parameter.
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        if (object instanceof BigDecimal) {
            return object;
        }
        return getBigDecimalValue(object, locale);
    }


    /*-----------------------------------------------------------------------
        Utilities
      -----------------------------------------------------------------------*/

    /**
        Returns the reciprocal of this value.  The <code>value</code> BigDecimal
        must have the desired scale of the result.
        @param value the input BigDecimal
        @return Returns a BigDecimal that is the reciprocal of the input
        @aribaapi documented
    */
    public static BigDecimal reciprocal (BigDecimal value)
    {
        if (value.signum() == 0) {
            return value;
        }

        return Constants.OneBigDecimal.divide(
            value, value.scale(), BigDecimal.ROUND_HALF_UP);
    }

    /**
        Rounds the <code>amount</code> to the number of decimal places
        specified by <code>scale</code> and returns the new amount.

        If the <code>amount</code> is already rounded to <code>scale</code>
        or has fewer decimal places than <code>scale</code>, we simply
        return the <code>amount</code>.

        @param amount The number to round.
        @param scale The number of decimal places to round to.

        @return The rounded <code>BigDecimal</code>.
        @aribaapi documented
    */
    public static BigDecimal round (BigDecimal amount, int scale)
    {
        if (scale >= 0 && amount.scale() > scale) {
            return amount.setScale(scale, BigDecimal.ROUND_HALF_UP);
        }
        else {
            return amount;
        }
    }

    /**
        Rounds the money amount toward positive infinity, to the
        number of decimal places specified by <code>scale</code>
        and returns the new amount.
        @param amount The BigDecimal input
        @param scale The number of decimal places to round the input
        @return Returns a big decimal that has been rounded to scale decimal places
        @aribaapi documented
    */
    public static BigDecimal roundCeiling (BigDecimal amount, int scale)
    {
        if (scale >= 0 && amount.scale() > scale) {
            return amount.setScale(scale, BigDecimal.ROUND_CEILING);
        }
        else {
            return amount;
        }
    }

    /**
        Get the decimal format for the default locale.

        @return Returns decimal format for the default locale
        @aribaapi documented
    */
    public static DecimalFormatSymbols getDecimalFormatSymbol ()
    {
        return getDecimalFormatSymbol(getDefaultLocale());
    }

    /**
        Get the decimal format for this locale
        @param locale the locale to use when returning the decimal format
        @return Returns decimal format for the locale
        @aribaapi documented
    */
    public static DecimalFormatSymbols getDecimalFormatSymbol (Locale locale)
    {
        DecimalFormat fmt = null;
        try
        {
            fmt = acquireDecimalFormat(locale);
            return fmt.getDecimalFormatSymbols();
        }
        finally
        {
            releaseDecimalFormat(fmt,locale);
        }
    }

    /**
        Returns <code>true</code> if the <code>amount</code> has a fractional
        component.
        @param amount The <code>BigDecimal</code> input
        @return Returns true if the amount has a fractional component
        @aribaapi documented
    */
    public static boolean hasDecimal (BigDecimal amount)
    {
        BigDecimal absVal = amount.abs();
        BigDecimal wholeValue = new BigDecimal(absVal.toBigInteger());
        BigDecimal decimalValue = absVal.subtract(wholeValue);
        return decimalValue.signum() > 0;
    }

    /**
        Returns the number of digits in the decimal part of the number.
        @param bd The BigDecimal input
        @return Returns the number of digits in the decimal part of the BigNumber
        @aribaapi documented
    */
    public static int numDecimalDigits (BigDecimal bd)
    {
        String str = trimString(getStringValue(bd));
            // Get the decimal separator char
        DecimalFormatSymbols symbols = getDecimalFormatSymbol();
        char decimalSep = symbols.getDecimalSeparator();

            // Search for the decimal separator
        int index = str.indexOf(decimalSep);
        if (index > 0) {
            str = str.substring(index + 1);
        }
        return (index > 0) ? str.length() : 0;
    }

    /**
        Returns the number of digits in the whole part of the number.
        @param bd The BigDecimal being input
        @return Returns the number of digits in the whole part of the number
        @aribaapi documented
    */
    public static int numWholeDigits (BigDecimal bd)
    {
            // Remove fractional part of the number (e.g. 1.61 => 1)
        bd = new BigDecimal((bd.toBigInteger()));

            // Formulate it as string and take the length of
            // the string
        String str = bd.toPlainString();
        return str.length();
    }



    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two BigDecimals and returns 1, 0, or -1 if bd1 is greater,
        equal, or less than bd2.

        @param bd1 <code>BigDecimal</code> used in comparision
        @param bd2 <code>BigDecimal</code> used in comparision
        @return Returns 1, 0, or -1 if bd1 is greater, equal, or less than bd2.
        @aribaapi documented
    */
    public static int compareBigDecimals (BigDecimal bd1, BigDecimal bd2)
    {
        return bd1.compareTo(bd2);
    }

    /**
        Compares <code>o1</code> and <code>o1</code> for sorting purposes.

        @param o1 <code>BigDecimal</code> used in comparision
        @param o2 <code>BigDecimal</code> used in comparision
        @param locale <code>Locale</code> used in comparision
        @return Returns 1, 0, or -1 if bd1 is greater, equal, or less than bd2.
        @aribaapi documented
    */
    protected int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof BigDecimal, "invalid type");
        Assert.that(o2 instanceof BigDecimal, "invalid type");

        return compareBigDecimals((BigDecimal)o1, (BigDecimal)o2);
    }

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
        return fmt;
    }

    private static DecimalFormat acquireDecimalFormat (Locale locale)
    {
        return acquireDecimalFormat(locale, null);
    }

    private static DecimalFormat acquireDecimalFormat (Locale locale, String pattern)
    {
        return (DecimalFormat)factory.acquireFormat(Formatter.BigDecimalFormatterType,
                                                    locale, pattern);
    }

    private static void releaseDecimalFormat (DecimalFormat df,Locale locale)
    {
        releaseDecimalFormat(df, locale, null);
    }

    private static void releaseDecimalFormat (DecimalFormat df,
                                              Locale locale, String pattern)
    {
        factory.releaseFormat(df,Formatter.BigDecimalFormatterType,locale,pattern);
    }
}
