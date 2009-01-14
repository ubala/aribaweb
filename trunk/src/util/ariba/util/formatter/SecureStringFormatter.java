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

    $Id: //ariba/platform/util/core/ariba/util/formatter/SecureStringFormatter.java#6 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;


/**
    Formats strings such that certain characters are
    "blocked out".  For example, the string "1234" can be formatted to
    display "XX34".

    This formatter uses a format string to format string values. This format
    string supports the following syntax:

    D - display a character as is
    X - replaces the character with the cross out character

    The string value is right justified relative to the format.  Any excess
    characters are trimmed away.  If the format string is longer than the
    value, then the value will be padded by the format string's characters.

    So, for example, if the format "XXD" with blockoutChar "*" is applied to
    "12345", the user will see "**5".

    @aribaapi documented
*/
public class SecureStringFormatter extends StringFormatter
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        The class name of this formatter.
        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.SecureStringFormatter";

        // String resources
    private static final String StringTable = "ariba.util.core";
    private static final String DefaultBlockoutChar = "BlockoutChar";


    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a <code>SecureStringFormatter</code>.
        @aribaapi private
    */
    public SecureStringFormatter ()
    {
    }

    /*-----------------------------------------------------------------------
        Static methods
      -----------------------------------------------------------------------*/

    /**
        Returns a string formatted according to the <code>format</code> and
        <code>blockoutString</code> parameters.

        @param value The value to format.
        @param format The format string that controls which characters of the
                      value get blocked out.
        @param blockoutString A string of length 1 that contains the
                              If the <code>blockoutString</code> is null, then
                              we'll use the default blockout character in the
                              string resources.
        @return Returns a string formatted according to the format and blockoutString parameters.
        @aribaapi documented
    */
    public static String getStringValue (Object value,
                                      String format,
                                      String blockoutString)
    {
        String strValue = getStringValue(value);
        char   blockoutChar = getBlockoutChar(blockoutString);

        if (StringUtil.nullOrEmptyOrBlankString(format)) {
            Assert.assertNonFatal(false,
                "SecureTextFieldViewer:  must specify a format property");
            return strValue;
        }

        FastStringBuffer buf = new FastStringBuffer(strValue);
        int i,j;
        for (i = format.length()-1, j = buf.length()-1;
             i >= 0;
             i--, j--) {

            char curChar = format.charAt(i);

                // If we found the 'D' format character, then let the
                // character pass through unchanged.
            if (curChar == 'D') {
                continue;
            }

            if (curChar == 'X') {

                    // If we found any other character, then
                    // replace the digit with the cross out character.
                if (j >= 0) {
                    buf.setCharAt(j, blockoutChar);
                }
                else {
                    buf.insert(blockoutChar, 0);
                }
                continue;
            }

                // Otherwise, insert the character into
                // the return string.
            j++;
            int k = Math.max(0, j);
            buf.insert(curChar, k);
        }

            // If we got to the beginning of the format before getting to the
            // beginning of the number, then strip away the remaining digits.
        if (i < j) {
            j++;
            strValue = buf.substring(j, buf.length());
        }
        else {
            strValue = buf.toString();
        }

        return strValue;
    }

    /*
        Returns the blockoutChar from the field properties.
    */
    private static char getBlockoutChar (String blockoutString)
    {
        if (StringUtil.nullOrEmptyString(blockoutString)) {
            String str =
                ResourceService.getString(StringTable, DefaultBlockoutChar);
            return str.charAt(0);
        }
        else {
            return blockoutString.charAt(0);
        }
    }
}
