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

    $Id: //ariba/platform/util/core/ariba/util/formatter/DecimalFormatterCommon.java#6 $
*/

package ariba.util.formatter;

import ariba.util.core.StringUtil;
import java.text.DecimalFormatSymbols;
import java.util.Locale;


/**
    Base class for BigDecimalFormatter and DoubleFormatter to share
    common formatting utilities.

    @aribaapi documented
*/
public abstract class DecimalFormatterCommon extends Formatter
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.DecimalFormatterCommon";

    // error messages
    protected static final String InvalidCharacterInNumberKey = "InvalidCharacterInNumber";
    protected static final String NoDigitsFoundKey = "NoDigitsFound";
    protected static final String NumberFormatErrorKey = "NumberFormatError";

    protected static final String CanonicalNegativePrefix = "-";

   
    /*-----------------------------------------------------------------------
        Utilities
      -----------------------------------------------------------------------*/

    /**
        Trim the string.  Remove all trailing zero's.  Use the
        default locale for determining the decimal separator character.
        
        @param str the input string from which leading and trailing whitespace is removed
        @return Returns a input string with after removing leading and trailing whitespace, including trailing zero's.
        @aribaapi documented
    */
    public static String trimString (String str)
    {
        return trimString(str, getDefaultLocale());
    }

    /**
        Trim the string.  Remove all trailing zero's

        @param str the input string from which leading and trailing whitespace is removed
        @param locale the locale used to determine the decimal separator when trimming whitespace
        @return Returns a input string with after removing leading and trailing whitespace, including trailing zero's.
        @aribaapi documented
    */
    public static String trimString (String str, Locale locale)
    {
        if (StringUtil.nullOrEmptyOrBlankString(str)) {
            return str;
        }

            // Get the decimal separator char
            // Note: The set of symbols returned by BigDecimalFormatter.getDecimalFormatSymbol
            // will work for both DoubleFormatter and BigDecimalFormatter since the only difference
            // between the DecimalFormat objects is the initial isGroupingUsed value.
            
        DecimalFormatSymbols symbols = BigDecimalFormatter.getDecimalFormatSymbol(locale);
        char decimalSep = symbols.getDecimalSeparator();

            // Search for the decimal separator
        int index = str.indexOf(decimalSep);
        if (index > 0) {

                // Find the first non-zero char.
            int lastIndex = str.length()-1;
            for (int i = lastIndex; i >= index; i--) {
                if (str.charAt(i) != '0') {
                    if (str.charAt(i) == decimalSep) {
                        str = str.substring(0, i);
                    }
                    else if (i < lastIndex) {
                        str = str.substring(0, i+1);
                    }
                    break;
                }
            }
        }

        return str;
    }

}
