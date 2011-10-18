/*
    Copyright 1996-2011 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/ServerUtil.java#13 $
*/

package ariba.util.core;

import ariba.util.i18n.I18NUtil;
import ariba.util.io.Mapping;
import ariba.util.log.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
    @aribaapi private
*/
public class ServerUtil
{

/*

Rewrite 's' expanding each occurrence of character 'quote' with two of
the same.

*/

    public static String quoteQuotes (String s, char quote)
    {
        if (s == null || (s.indexOf(quote) == -1)) {
            return s;
        }

        FastStringBuffer sb = new FastStringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == quote) {
                sb.append(quote);
            }

            sb.append(c);
        }

        return sb.toString();
    }

    public static boolean compareFilenames (String firstFilename,
                                            String secondFilename)
    {
        FileInputStream firstStream = null;
        FileInputStream secondStream = null;
        try {
            File firstFile = new File(firstFilename);
            File secondFile = new File(secondFilename);
            firstStream = new FileInputStream(firstFile);
            secondStream = new FileInputStream(secondFile);
            return compareInputStreams(firstStream, secondStream);
        }
        catch (FileNotFoundException fnfe) {
            return false;
        }
        finally {
            try {
                if (firstStream != null) {
                    firstStream.close();
                }
                if (secondStream != null) {
                    secondStream.close();
                }
            }
            catch (IOException ioe) {
                Log.util.error(2787, ioe);
            }
        }
    }

    public static boolean compareInputStreams (InputStream firstStream,
        InputStream secondStream)
    {
        byte[] buffer1 = new byte [2048];
        byte[] buffer2 = new byte [2048];
        return compareInputStreams(firstStream, secondStream, buffer1, buffer2);
    }

    public static boolean compareInputStreams (
        InputStream firstStream,
        InputStream secondStream,
        byte[] buffer1,
        byte[] buffer2)
    {
        try {
            int bufferSize = Math.min(buffer1.length, buffer2.length);
            int bytesRead = 0;
            int bytesRead2 = 0;

            boolean result = true;

            while (bytesRead > -1 && result) {
                bytesRead = firstStream.read(buffer1, 0, bufferSize);
                bytesRead2 = secondStream.read(buffer2, 0, bufferSize);

                if (bytesRead != bytesRead2) {
                    return false;
                }

                result = SystemUtil.memoryCompare(buffer1, 0, buffer2, 0, bytesRead);
            }
            return result;
        }
        catch (IOException ioe) {
            return false;
        }
    }

/*

Map file extension to Mime Type

*/

    private static File MimeTypeFile;
    private static final String DefaultMimeType = "application/octet-stream";

    private static Mapping MimeTypeMapping;
    private static boolean MimeTypeInitialized;

    /**
     * Initializes the mime type mapping from the MimeTypes.csv.
     * This method is synchronized to make the initialization thread safe.
     *
     * Thread safety was an issue when enabling multiple upstream realms at the
     * same time and the mime types had not been loaded yet.
     */
    private static synchronized void initMimeType ()
    {
        if (MimeTypeInitialized) {
            return;
        }


        MimeTypeFile = new File(SystemUtil.getConfigDirectory(),
              "MimeTypes.csv");

        MimeTypeMapping = new Mapping(URLUtil.urlAbsolute(MimeTypeFile),
              I18NUtil.EncodingASCII,
              DefaultMimeType);

        try {
            MimeTypeMapping.read();
            MimeTypeInitialized = true;
        }
        catch (IOException ioe) {
            Log.util.error(2788, MimeTypeFile, ioe);
        }
    }

    /**
        if we don't have a current type or the current type is
        the default, then try to map it by file extension.
    */
    public static String mimeType (String currentType, String fileExtension)
    {
        if (currentType == null ||
            "".equals(currentType) ||
            DefaultMimeType.equalsIgnoreCase(currentType)) {

            initMimeType();

            if (MimeTypeMapping == null) {
                Log.util.warning(2887);
                return currentType;
            }
            return MimeTypeMapping.map(fileExtension);
        }

        else {
            return currentType;
        }
    }

    public static String mimeType (String fileExtension)
    {
        return mimeType(null, fileExtension);
    }

    /**
        Returns the list of file extension names for the given mime type.
        It could return null or empty list on a not matched case.
        @param mimeType the mime type
        @see ariba.util.io.Mapping#reverseMap
        @return the mapped list of file extension names.
        @aribaapi private
    */
    public static List/*<String>*/ fileExtension (String mimeType)
    {
        initMimeType();
        return MimeTypeMapping.reverseMap(mimeType);
    }

    public static final String HTTPSProxyPassword = "https.proxyPassword";
    
    public static void setSystemProperty (String key, String value)
    {
        Properties properties = System.getProperties();
        properties.put(key, value);
        System.setProperties(properties);
    }

    public static final boolean isWin32 = SystemUtil.isWin32();

    public static final boolean isUnix = !isWin32;

    public static final boolean isMicrosoftVM =
        SystemUtil.getJavaVendor().equals("Microsoft Corp.");

    /**
        Useful for asking if it is legal to call a win32 specific api
    */
    public static final boolean isWin32Enabled = isWin32 && isMicrosoftVM;

    /**
       filterValue is useful for dumping out a Map for display in which
       a particular key's value is not suitable for display (e.g. contains a
       password).
    */
    public static String filterMapValue (
        Map map, String key, char fillChar)
    {
        return filterMapValue(map.toString(), key, fillChar);
    }

    /**
        Params:
            input: a string, assumed to be a result of Map.toString()
            filterKey: the key for which the value is to be "filtered"
            fillChar: the value for which the value is to be overwritten
        Returns null if the key was not found
    */
    public static String filterMapValue (
        String input, String filterKey, char fillChar)
    {
        int filterIdx = input.indexOf(filterKey);
            // not the most efficient algorithm as we effectively new the
            // string twice, fix when necessary
        if (filterIdx != -1) {
            String image = input;
            int inputLen = input.length();
            char [] buffer = image.toCharArray();
            while (filterIdx != -1) {
                filterIdx = image.indexOf("=", filterIdx) + 1;
                    // find the beginning of the value, skip past whitespace
                while (buffer[filterIdx] == ' ' && filterIdx < inputLen) {
                    filterIdx++;
                }
                int endFilterIdx = image.indexOf(";", filterIdx) - 1;

                if (endFilterIdx != -1) {
                        // find the end of the value, back over whitespace
                    while (buffer[filterIdx] == ' ') {
                        filterIdx--;
                    }
                    for (int idx = filterIdx; idx < endFilterIdx + 1; idx++) {
                        buffer[idx] = fillChar;
                    }
                }
                filterIdx = image.indexOf(filterKey, endFilterIdx);
            }
            image = new String(buffer);
            return image;
        }

        return input;
    }

    public static String[] simpleParse (String line, String delimeter)
    {
        List temp = ListUtil.list();
        Assert.that(line != null, "simpleParse: Null string");
        while (true) {
            line = line.trim();
            if (line.length()==0) {
                break;
            }
            String cursor;
            int whiteSpaceIndex = line.indexOf(' ');
            if (whiteSpaceIndex==-1) {
                cursor = line;
            }
            else {
                // grab the string before the first space
                cursor = line.substring(0, whiteSpaceIndex);
            }
            if (cursor.length()==0) {
                break;
            }
            temp.add(cursor);
            // grab the string after the first blank
            whiteSpaceIndex = line.indexOf(' ');
            int endIndex = line.length();
            if (whiteSpaceIndex==-1) {
                break;
            }
            line = line.substring(whiteSpaceIndex, endIndex);
        }
        String [] args = new String[temp.size()];
        for (int idx = 0; idx < temp.size(); idx++) {
            args[idx] = (String)temp.get(idx);
        }
        return args;
    }

    /**
        Assuming the specified charArray represents an integer, returns that
        integer's value. Throws an exception if the charArray cannot be parsed
        as an int.

        @exception NumberFormatException If the charArray does not contain a
        parsable integer.
    */
    public static int parseInt (
        int offset, int scanLength, char[] charArray, int charArrayLength)
      throws NumberFormatException
    {
        return parseInt(offset, scanLength, charArray, charArrayLength, 10);
    }

    /**
        Assuming the specified charArray represents an integer, returns that
        integer's value.  Throws an exception if the charArray cannot be parsed
        as an int.

        @exception NumberFormatException If the charArray does not contain a
        parsable integer.
    */
    public static int parseInt (
        char [] charArray, int charArrayLength, int radix)
      throws NumberFormatException
    {
        return parseInt(0, charArrayLength, charArray, charArrayLength, radix);
    }

    /**
        Assuming the specified charArray represents an integer, returns that
        integer's value.  Throws an exception if the charArray cannot be parsed
        as an int.

        @exception NumberFormatException If the charArray does not contain a
        parsable integer.
    */
    public static int parseInt (
        char [] charArray, int charArrayLength)
      throws NumberFormatException
    {
        return parseInt(0, charArrayLength, charArray, charArrayLength, 10);
    }


    /**
        Assuming the specified charArray represents an integer, returns that
        integer's value.  Throws an exception if the charArray cannot be parsed
        as an int.

        @exception NumberFormatException If the charArray does not contain a
        parsable integer.
    */
    public static int parseInt (
        int offset, int scanLength, char [] charArray,
        int charArrayLength, int radix)
      throws NumberFormatException
    {
        if (charArrayLength <= 0) {
            throw new NumberFormatException("Empty charArray");
        }
        int scanLastIdx = offset + scanLength - 1;
        if (scanLastIdx > charArrayLength - 1) {
            throw new NumberFormatException("Scan beyond end of buffer");
        }
        int idx = offset;

            // have to set cursor to quite compiler
        char cursor = ' ';

            // skip leading whitespace
        while (idx <= scanLastIdx) {
            cursor = charArray[idx];
            if (!Character.isWhitespace(cursor)) {
                break;
            }
            idx++;
        }
        if (idx > scanLastIdx) {
            throw new NumberFormatException("int not found");
        }
            // check for sign
        boolean negative = false;
        if (cursor == '-') {
            negative = true;
            idx++;
        }
        int result = 0;
        boolean foundDigit = false;
            // parse string, stop if digit's exhausted, complain if you
            // don't get any digits
        while (idx <= scanLastIdx) {
            cursor = charArray[idx];
            int digit = Character.digit(cursor, radix);
            if (digit < 0) {
                if (!foundDigit) {
                    throw new NumberFormatException(Fmt.S(
                        "Unrecognized digit at index: %s", idx));
                }
                break;
            }
            foundDigit = true;
            result = result * radix + digit;
            idx++;
        }

        if (negative) {
            return -result;
        }
        return result;
    }
    
    private static SecureRandom secureNumberGenerator;

    public static boolean UseSecureRandomNumberGenerator = true;
    
    public static final Random secureRandomNumberGenerator ()
    {
        if (!UseSecureRandomNumberGenerator) {
            return randomNumberGenerator();
        }
        if (secureNumberGenerator == null) {
                // calling this will be slow - only do it once
            secureNumberGenerator = new SecureRandom();
        }
        return secureNumberGenerator;
    }
    
    public static final int nextSecureInt ()
    {
        return secureRandomNumberGenerator().nextInt();
    }

    public static final long nextSecureLong ()
    {
        return secureRandomNumberGenerator().nextLong();
    }

    private static Random randomNumberGenerator;
    
    public static final Random randomNumberGenerator ()
    {
        if (randomNumberGenerator == null) {
                // calling this will be slow - only do it once
            randomNumberGenerator = new Random();
        }
        return randomNumberGenerator;
    }
    
    public static final int nextRandomInt ()
    {
        return randomNumberGenerator().nextInt();
    }

    public static final long nextRandomLong ()
    {
        return randomNumberGenerator().nextLong();
    }
}
