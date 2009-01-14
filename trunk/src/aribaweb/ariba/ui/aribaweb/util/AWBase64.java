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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWBase64.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.SystemUtil;
import ariba.util.core.Assert;

// Despite this class's name, it can encode in either base64 or base32

public class AWBase64 extends AWBaseObject
{
    //////////////////
    // Base64 for int
    //////////////////

    private static final int EncodedStringMaxLength = 8;
    private static final char[] EncodeValues = new char[64];
    private static final byte[] DecodeValues = new byte['z' + 1];

    private static int _base = 64;
    private static int _bitCount = 6;
    private static long _bitMask = 0x3f;

    static {
        try {
            initEncodeValues();
            initDecodeValues();
            test(false);
        }
        catch (RuntimeException runtimeException) {
            Log.aribaweb.error(9023, "Unable to initialize AWBase64",
                               SystemUtil.stackTrace(runtimeException));
        }
    }

    /**
     * Set the base we use for encoding the semantic keys.
     * @param base - the base to use, must be either 32 or 64
     * @aribaapi private
     */
    public static void setEncodingBase (int base)
    {
        Assert.that(base == 32 || base == 64, "invalid base set in AWBase64");
        _base = base;
        if (_base == 32) {
            _bitCount = 5;
            _bitMask = 0x1f;
        }
        else if (_base == 64) {
            _bitCount = 6;
            _bitMask = 0x3f;
        }
    }

    private static void initEncodeValues ()
    {
        int charValueIndex = 0;
        char currentChar = 'a';
        for (int index = 0; index < 26; index++, charValueIndex++, currentChar++) {
            EncodeValues[charValueIndex] = currentChar;
        }

        currentChar = 'A';
        for (int index = 0; index < 26; index++, charValueIndex++, currentChar++) {
            EncodeValues[charValueIndex] = currentChar;
        }

        currentChar = '0';
        for (int index = 0; index < 10; index++, charValueIndex++, currentChar++) {
            EncodeValues[charValueIndex] = currentChar;
        }

        // These two chars are chosen as they can be legal javascript identifier chars.
        EncodeValues[charValueIndex] = '_';
        charValueIndex++;
        EncodeValues[charValueIndex] = '$';
    }

    private static void initDecodeValues ()
    {
        for (byte index = 0; index < 64; index++) {
            char currentChar = EncodeValues[index];
            DecodeValues[currentChar] = index;
        }
    }

    private static void test (boolean doTest)
    {
        if (!doTest) {
            return;
        }
        long millis = System.currentTimeMillis();
        Log.aribaweb.debug("*** performing 50000000 tests");
        for (long index = 0; index < 50000000; index++) {
            String encodedString = base64EncodeLong(index);
            if (base64DecodeLong(encodedString, 0) != index) {
                Log.aribaweb.debug("***** FAILURE encoding/decoding: " + index);
            }
        }
        Log.aribaweb.debug("*** DONE performing 50000000 tests: " + ((System.currentTimeMillis() - millis)/1000));
    }

    //////////////////
    // Public Methods
    //////////////////

    /**
     *
     * @param longValue a 32-bit value to be Base64 encoded.  We use long here to avoid
     * issues with negative values.
     */
    public static String base64EncodeLong (long longValue)
    {
        Assert.that(longValue > -1, "cannot Base64 encode negative values (with this algorithm)");
        int nonzeroOffset = 0;
        final char[] charArray = new char[EncodedStringMaxLength];
        for (int charIndex = 0; charIndex < EncodedStringMaxLength; charIndex++, longValue >>= _bitCount) {
            // only use _bitCount bits at a time.  (note rightshift above)
            final int offset = (int)(longValue & _bitMask);
            charArray[charIndex] = EncodeValues[offset];
            if (offset > 0) {
                nonzeroOffset = charIndex;
            }
        }
        return new String(charArray, 0, nonzeroOffset + 1);
    }

    /**
     *
     * @param encodedString
     * @param startPos this allows for a prefix ont he encodedString.  All elementIds start with '_'
     */
    public static long base64DecodeLong (String encodedString, int startPos)
    {
        long longValue = 0;
        int bitShiftAmount = 0;
        for (int charIndex = startPos, length = encodedString.length();
             charIndex < length;
             charIndex++, bitShiftAmount += _bitCount) {
            char currentChar = encodedString.charAt(charIndex);
            long currentByte = DecodeValues[currentChar];
            longValue |= (currentByte << bitShiftAmount);
        }
        return longValue;
    }
}
