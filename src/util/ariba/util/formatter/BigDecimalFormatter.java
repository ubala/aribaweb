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

    $Id: //ariba/platform/util/core/ariba/util/formatter/BigDecimalFormatter.java#19 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.StringUtil;
import ariba.util.i18n.LocaleSupport;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
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
    private static final char CanonicalMinusSign        = '-';
    private static final String CanonicalNegativePrefix = "-";

    private static final char CanadianEnglishGroupSeparator = ',';

    final static int NOT_FOUND = -1;

    // error messages
    private static final String InvalidCharacterInNumberKey = "InvalidCharacterInNumber";
    private static final String NoDigitsFoundKey = "NoDigitsFound";
    private static final String NumberFormatErrorKey = "NumberFormatError";

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
        public StrictParseException (String s, int errorOffset)
        {
            super(s, errorOffset);
        }

        public static StrictParseException make (int offset)
        {
            String msg = Formatter.makeParseExceptionMessage("StrictValidationError");
            return new StrictParseException(msg, offset);
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
        Assert.that(locale != null, "invalid null Locale");
        DecimalFormat fmt = null;
        try
        {
            fmt = acquireDecimalFormat(locale);
            return parseBigDecimal(string, fmt);
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
        /*
         * In general we loop through the input string and fill some intermediate
         * buffers with the results.  Then we convert the buffers to other types
         * and pass these to a DecimalParseInfo constructor.
         */

        /* House keeping setup */

            // temporarily disable strict to allow build to complete
            // --mgower 04/05/2010
        strict = false;

            // eg if "10-" is a valid string, ignoreNegativePrefix will be set to true
        boolean ignoreNegativePrefix = false;
            // eg if "-10" is a valid string, ignoreNegativeSuffix will be set to true
            // if "(10)" is a valid string, neither will be ignored
        boolean ignoreNegativeSuffix = false;

        DecimalFormatSymbols symbols = fmt.getDecimalFormatSymbols();

            // Initialize the parsing result buffers
        FastStringBuffer number = new FastStringBuffer();
        FastStringBuffer prefix = new FastStringBuffer();
        FastStringBuffer suffix = new FastStringBuffer();

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
            // if no negative prefix
        if (StringUtil.nullOrEmptyOrBlankString(negativePrefix)) {
            // and negative suffix
            if (!StringUtil.nullOrEmptyOrBlankString(negativeSuffix)) {
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
        else if (StringUtil.nullOrEmptyOrBlankString(negativeSuffix)) {
            ignoreNegativeSuffix = true;
        }

        boolean negPrefixFound = false;
        boolean negSuffixFound = false;
            // eg if "((100))" is negative, the first ')' would have negativeSuffixIndex 0
            // and the second ')' would have negativeSuffixIndex 1
        int negativeSuffixIndex = NOT_FOUND;
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
                number.append(CanonicalDecimalSeparator);
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
                if (suffix.length() > 0 || negSuffixFound) {

                    // if we have exponential notation then process
                    char prevChar = string.charAt(i-1);
                    if (prevChar == 'E' || prevChar == 'e') {

                        // try to create a BigDecimal, convert to plain string
                        // and recall this same function with the plain string
                        try {
                            BigDecimal parsedBigDecimal = new BigDecimal(string);
                            String parsedString = parsedBigDecimal.toPlainString();
                            return BigDecimalFormatter.parseBigDecimal(parsedString, fmt);
                        }
                        catch (ParseException e) {
                            throw e;
                        }
                        catch (NumberFormatException e) {
                            throw makeParseException(NumberFormatErrorKey, 1);
                        }
                    }
                    else {
                        // we have an invalid string
                        throw makeParseException(InvalidCharacterInNumberKey, 1);
                    }
                }

                number.append(curChar);
                foundDigits = true;
                if (decimalLocation > NOT_FOUND) {
                    // decimal place has been found
                    decimalDigits++;
                }
                else {
                    integerDigits++;
                }
            }

                // We're on the canonical negative prefix character ('-').
                // We check for the formatter's negative prefix character above.
            else if (!foundDigits &&
                (!strict || CanonicalNegativePrefix.equals(Character.toString(curChar))))
            {
                prefix.append(curChar);
                // negPrefixFound is needed for accepting -150 as negative
                // when the format is (150), not changing to avoid risk of B/C break
                // -mgower 03/30/2010
            }

                // We're on a number suffix character
                // A different check for a suffix is above.
            else if (
                isNegativeSuffix(curChar, negativeSuffix, negativeSuffixIndex, strict))
            {
                if (strict && suffix.length() == negativeSuffix.length()) {
                    // we found too many suffix characters, ie (100))
                    throw StrictParseException.make(i);
                }
                suffix.append(curChar);
                    // increment or set the negativeSuffixIndex
                if (negativeSuffixIndex == NOT_FOUND) {
                    negativeSuffixIndex = 0;
                }
                else {
                    negativeSuffixIndex++;
                }

                // we found the negative suffix but we aren't flipping the negSuffixFound
                // flag to avoid the risk of B/C break, -mgower 3/30/2010
            }

                // Invalid input with strict flag on
            else {
                throw StrictParseException.make(i);
            }
        } // end input string loop

        /* Convert the buffer data to the format for DecimalParseInfo's constructor */

        // make number buffer negative based on housekeeping data
            // If we did not find the negative prefix or suffix because
            // they are not supposed to be there in the first place
            // ignoreNegativePrefix or ignoreNegativeSuffix will tell
            // us that, then we claim that we have found the neg prefix
            // or suffix. We need this logic to determine if we want to
            // insert the minus sign or not.
        if (!negPrefixFound && ignoreNegativePrefix) {
            Log.util.debug("setting negPrefixFound to true");
            negPrefixFound = true;
        }
        if (!negSuffixFound && ignoreNegativeSuffix) {
            Log.util.debug("setting negSuffixFound to true");
            negSuffixFound = true;
        }
            // Put back a canonical minus sign if the number is negative
        if (negPrefixFound && negSuffixFound) {
            number.insert(CanonicalMinusSign, 0);
        }

        // sanity check
        if (number.length() == 0) {
            throw makeParseException(NoDigitsFoundKey, 0);
        }

        // convert number buffer to big decimal
        String numberString = number.toString();
        BigDecimal amount = Constants.ZeroBigDecimal;
        try {
            amount = new BigDecimal(numberString);
        }
        catch (NumberFormatException e) {
            throw makeParseException(NumberFormatErrorKey, 0);
        }

        // convert other buffers to strings
        String prefixString = prefix.toString();
        if (StringUtil.nullOrEmptyOrBlankString(prefixString)) {
            prefixString = null;
        }
        String suffixString = suffix.toString();
        if (StringUtil.nullOrEmptyOrBlankString(suffixString)) {
            suffixString = null;
        }

        /* Use converted housekeeping data */  

        return new DecimalParseInfo(integerDigits, decimalDigits, decimalLocation,
                                    prefixString, suffixString, amount);
    }

    private static boolean isNegativeSuffix (
        char curChar, String negativeSuffix, int negativeSuffixIndex, boolean strict)
    {
        if (!strict) {
            // if not strict then just assume it's a negative suffix for b/c
            return true;
        }
        else if (StringUtil.nullOrEmptyOrBlankString(negativeSuffix)) {
            // if there is no negative suffix then we can't be there
            return false;
        }

        boolean firstNegativeSuffixChar =
            negativeSuffixIndex == NOT_FOUND && negativeSuffix.charAt(0) == curChar;
        if (firstNegativeSuffixChar) {
            return true;
        }

        boolean otherNegativeSuffixChar =
            negativeSuffixIndex != NOT_FOUND
            && negativeSuffixIndex < negativeSuffix.length()
            && negativeSuffix.charAt(negativeSuffixIndex) == curChar;
        return otherNegativeSuffixChar;
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
