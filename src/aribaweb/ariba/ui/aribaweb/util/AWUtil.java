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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWUtil.java#62 $
*/

package ariba.ui.aribaweb.util;


import ariba.ui.aribaweb.core.AWIf;
import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.DynamicArray;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.HTML;
import ariba.util.core.IOUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.Fmt;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.i18n.I18NUtil;
import ariba.util.i18n.MergedStringLocalizer;
import ariba.util.io.CSVConsumer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.FileFilter;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;

import org.w3c.tidy.Tidy;
import org.w3c.tidy.Configuration;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

public final class AWUtil extends AWBaseObject
{
    // Todo: set these from the Application at init time?
    public static boolean RequiresThreadSafety = true;
    public static boolean IsRapidTurnaroundEnabled = true;
    public static boolean AllowsConcurrentRequestHandling = true;

    private static final String LocalizedStringEndMarker = "<!-- END AWLOCAL -->";
    private static final String LocalizedStringBeginMarkerStart = "<!-- AWLOCAL: ";
    private static final String LocalizedStringBeginMarkerEnd = " -->";
    private static final String LocalizedStringSpace = " ";
    public static final String QuoteString = "&QUOT;";
    private static GrowOnlyHashtable LocalesByBrowserLanguageString = new GrowOnlyHashtable();
    public static final int IdStringRadix = Character.MAX_RADIX;
    public static final AWEncodedString UndefinedEncodedString = new AWEncodedString("UndefinedEncodedString");
    private static final int CharBufSize = 1024;
    private static final int ByteBufferSize = 1024;
    private static final int MaxIntegerStrings = 512;
    private static final String[] IntegerStrings = new String[MaxIntegerStrings];
    private static AWSizeLimitedHashtable HtmlEscapedStrings = new AWSizeLimitedHashtable(512);
    private static AWSizeLimitedHashtable HtmlUnsafeEscapedStrings = new AWSizeLimitedHashtable(512);
    /* Limit the max size of each cache to 100MB. Strings internally have a char array
       so 2 bytes per char. AWEncodedString stores it as a byte array, so it's 1 byte per
       char for ASCI and 2 for unicode. Worse case, it's 2 bytes per char. To limit to
       100MB, each string should be limited to 200KB i.e. 102400 chars.*/
    private static int MaxCacheStringSize = 102400;
    private static GrowOnlyHashtable EncodedHtmlAttributes = new GrowOnlyHashtable();
    private static AWClassLoader TheClassLoader = new AWClassLoader();
    private static Map _environment = null;
    public static final char BeginQueryChar = '?';
    public static final String TokenizerDelim ="&";
    public static final char Equals = '=';

    // ** Thread Safety Considerations: LocalesByBrowserLanguageString require locking -- everything else is either immutable or not shared.

    static
    {
        for (int index = MaxIntegerStrings - 1; index >= 0; index--) {
            IntegerStrings[index] = Integer.toString(index);
        }
    }

    public synchronized static void setClassLoader (AWClassLoader loader)
    {
        if (TheClassLoader != null) loader.setChainedClassLoader(TheClassLoader);
        TheClassLoader = loader;
    }

    public static AWClassLoader getClassLoader ()
    {
        return TheClassLoader;
    }

    // Use isAssignableFromClass?
    public static boolean classInheritsFromClass (Class classInQuestion, Class targetSuperclass)
    {
        boolean doesInherit = false;
        if (classInQuestion == targetSuperclass) {
            doesInherit = true;
        }
        else {
            Class superclassInQuestion = classInQuestion.getSuperclass();
            if (superclassInQuestion != null) {
                doesInherit = classInheritsFromClass(superclassInQuestion, targetSuperclass);
            }
        }
        return doesInherit;
    }

    // Use isAssignableFromClass?
    public static boolean classImplementsInterface (Class targetClass, Class targetInterface)
    {
        // ** this is not known to work yet.
        boolean classImplementsInterface = false;
        Class implmentedInterfaces[] = targetClass.getInterfaces();
        if (AWUtil.containsIdentical(implmentedInterfaces, targetInterface)) {
            classImplementsInterface = true;
        }
        else {
            int interfaceCount = implmentedInterfaces.length;
            for (int index = 0; index < interfaceCount; index++) {
                Class currentInterface = implmentedInterfaces[index];
                classImplementsInterface = classImplementsInterface(currentInterface, targetInterface);
                if (classImplementsInterface) {
                    break;
                }
            }
        }
        if (!classImplementsInterface) {
            Class targetSuperclass = targetClass.getSuperclass();
            if (targetSuperclass != null) {
                classImplementsInterface = classImplementsInterface(targetSuperclass, targetInterface);
            }
        }
        return classImplementsInterface;
    }

    public static Class commonSuperclass (Class class1, Class class2)
    {
        Class commonSuperclass = null;
        if (class1 == class2) {
            commonSuperclass = class1;
        }
        else {
            Class superclass1 = class1.getSuperclass();
            if (AWUtil.classInheritsFromClass(class2, superclass1)) {
                commonSuperclass = superclass1;
            }
            else {
                commonSuperclass = commonSuperclass(superclass1, class2);
            }
        }
        return commonSuperclass;
    }

    public static Class classForName (String className)
    {
        Class classForName = null;
        try {
                // Note static initialization issue -- This class extends AWBaseObject,
                // but this method gets called in the AWBaseObject static initializer,
                // so AWUtil hasn't completed it's own static initialization yet when
                // first called, and TheClassLoader is null
             classForName = (TheClassLoader == null) ?
                 ClassUtil.classForName(className,false):
                 TheClassLoader.getClass(className);
        }
        catch (ClassNotFoundException exception) {
            /*
             AWConcreteApplication.SharedInstance.debugString(
                 "*** classForName() exception: " + className + " -- " + exception);
            */
            exception.printStackTrace();
        }
        return classForName;
    }

    public static String getString (Reader reader)
    {
        FastStringBuffer stringBuffer = new FastStringBuffer();
        char charBuf[] = new char[CharBufSize];
        int actualReadCount = 0;
        try {
            while ((actualReadCount = reader.read(charBuf, 0, CharBufSize)) != -1) {
                stringBuffer.appendCharRange(charBuf, 0, actualReadCount);
            }
        }
        catch (IOException exception) {
            throw new AWGenericException("Error while converting stream to string. ", exception);
        }
        return stringBuffer.toString();
    }

    public static String stringWithContentsOfInputStream (InputStream inputStream, boolean shouldExpectEncoding)
    {
        return stringWithContentsOfInputStream(inputStream, shouldExpectEncoding, AWCharacterEncoding.ISO8859_1.name);
    }

    public static String stringWithContentsOfInputStream (InputStream inputStream,
                                                          boolean shouldExpectEncoding,
                                                          String defaultCharsetName)
    {
        InputStreamReader inputStreamReader = null;
        if (shouldExpectEncoding) {
            try {
                String encoding = IOUtil.readLine(inputStream);
                inputStreamReader = new InputStreamReader(inputStream, encoding);
            }
            catch (IOException exception) {
                throw new AWGenericException(exception);
            }
        }
        else {
            try {
                inputStreamReader = new InputStreamReader(inputStream, defaultCharsetName);
            }
            catch (UnsupportedEncodingException unsupportedEncodingException) {
                throw new AWGenericException(unsupportedEncodingException);
            }
        }
        return getString(inputStreamReader);
    }

    public static String stringWithContentsOfInputStream (InputStream inputStream)
    {
        return stringWithContentsOfInputStream(inputStream, false);
    }

    public static String stringWithContentsOfFile (String filePath, boolean shouldExpectEncoding)
    {
        String string = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filePath);
        }
        catch (FileNotFoundException exception) {
            throw new AWGenericException("Error: unable to locate file with path \"" + filePath + "\" exception: ", exception);
        }
        try {
            string = stringWithContentsOfInputStream(fileInputStream, shouldExpectEncoding);
        }
        catch (RuntimeException exception) {
            throw new AWGenericException("Error: unable to read from file with path \"" + filePath + "\" exception: ", exception);
        }
        finally {
            try {
                fileInputStream.close();
            }
            catch (IOException exception) {
                throw new AWGenericException("Error closing file: \"" + filePath  + "\" exception:", exception);
            }
        }
        return string;
    }

    public static String stringWithContentsOfFile (String filePath)
    {
        return stringWithContentsOfFile(filePath, false);
    }

    public static String stringWithContentsOfFile (File file, boolean shouldExpectEncoding)
    {
        return stringWithContentsOfFile(file.getAbsolutePath(), shouldExpectEncoding);
    }

    public static String stringWithContentsOfFile (File file)
    {
        return stringWithContentsOfFile(file, false);
    }

    public static void close (InputStream inputStream)
    {
        try {
            inputStream.close();
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }

    public static AWByteArray byteArrayForFile (File file)
    {
        AWByteArray byteArray = new AWByteArray(10 * ByteBufferSize);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            byte[] byteBuffer = new byte[ByteBufferSize];
            int bytesRead = 0;
            while ((bytesRead = bufferedInputStream.read(byteBuffer, 0, ByteBufferSize)) != -1) {
                byteArray.append(byteBuffer, 0, bytesRead);
            }
            bufferedInputStream.close();
            fileInputStream.close();
        }
        catch (IOException exception) {
            throw new AWGenericException(exception);
        }
        return byteArray;
    }

    public static AWByteArray byteArrayForInputStream (InputStream input)
    {
        AWByteArray byteArray = new AWByteArray(10 * ByteBufferSize);
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(input);
            byte[] byteBuffer = new byte[ByteBufferSize];
            int bytesRead = 0;
            while ((bytesRead = bufferedInputStream.read(byteBuffer, 0, ByteBufferSize)) != -1) {
                byteArray.append(byteBuffer, 0, bytesRead);
            }
            bufferedInputStream.close();
        }
        catch (IOException exception) {
            throw new AWGenericException(exception);
        }
        return byteArray;
    }

    public static byte[] getBytes (InputStream inputStream)
    {
        return byteArrayForInputStream(inputStream).toByteArray();
    }

    public static byte[] contentsOfFile (File file)
    {
        AWByteArray byteArray = byteArrayForFile(file);
        return byteArray.toByteArray();
    }

    public static int streamCopy (InputStream inputStream, OutputStream outputStream)
    {
        byte[] byteBuffer = new byte[ByteBufferSize];
        int bytesRead = 0, total = 0;
        try {
            while ((bytesRead = inputStream.read(byteBuffer, 0, ByteBufferSize)) != -1) {
                outputStream.write(byteBuffer, 0, bytesRead);
                total += bytesRead;
            }
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
        return total;
    }

    public static File moveAside (File targetFile)
    {
        File parentDirectory = new File(targetFile.getParent());
        File movedFile = uniqueFile(parentDirectory, "AWUtil", ".bak");
        targetFile.renameTo(movedFile);
        return movedFile;
    }

    private static void restoreFromTempFile (File file, File tempFile)
    {
        file.delete();
        tempFile.renameTo(file);
        tempFile.delete();
    }

    public static void writeToFile (String sourceString, File file)
    {
        // This code is similar to writeToFile(byte[]...).  Since byte[] and String do not share any
        // common superclass or a common interface, this code must be duplicated.  Of course we could
        // do an instance of check, but these would need to be sprinkled through this code and would
        // make it even more difficult to understand.
        FileWriter fileWriter = null;
        boolean fileAlreadyExists = (file.exists() && file.isFile());
        File tempFile = null;
        if (fileAlreadyExists) {
            tempFile = moveAside(file);
        }
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(sourceString);
        }
        catch (IOException exception) {
            if (fileAlreadyExists) {
                restoreFromTempFile(file, tempFile);
                fileAlreadyExists = false;
            }
            throw new AWGenericException(exception);
        }
        finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                }
                catch (IOException secondException) {
                    // swallow
                    secondException = null;
                }
                fileWriter = null;
            }
            if (fileAlreadyExists) {
                tempFile.delete();
            }
        }
    }

    public static void writeToFile (String sourceString, String filePath)
    {
        File file = new File(filePath);
        writeToFile(sourceString, file);
    }

    public static void writeToFile (byte[] sourceBytes, File file)
    {
        // Important! see comment above
        boolean fileAlreadyExists = (file.exists() && file.isFile());
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream outputStream = null;
        File tempFile = null;
        if (fileAlreadyExists) {
            tempFile = moveAside(file);
        }
        try {
            fileOutputStream = new FileOutputStream(file);
            outputStream = new BufferedOutputStream(fileOutputStream);
            outputStream.write(sourceBytes);
        }
        catch (IOException exception) {
            if (fileAlreadyExists) {
                restoreFromTempFile(file, tempFile);
                fileAlreadyExists = false;
            }
            throw new AWGenericException(exception);
        }
        finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                    fileOutputStream.close();
                }
                catch (IOException secondException) {
                    // swallow
                    secondException = null;
                }
                outputStream = null;
            }
            if (fileAlreadyExists) {
                tempFile.delete();
            }
        }
    }

    public static void writeToFile (byte[] sourceBytes, String filePath)
    {
        File file = new File(filePath);
        writeToFile(sourceBytes, file);
    }

    public static List parseCsvFile (String filePath)
    {
        List csvLinesVector = null;
        AWCsvConsumer csvConsumer = new AWCsvConsumer();
        CSVReader csvReader = new CSVReader(csvConsumer);
        File csvFile = new File(filePath);
        if (csvFile.exists()) {
            try {
                csvReader.readForSpecifiedEncoding(csvFile);
            }
            catch (IOException exception) {
                throw new AWGenericException("Error parsing file: " + filePath, exception);
            }
            csvLinesVector = csvConsumer.lines();
        }
        return csvLinesVector;
    }

    public static List parseCsvStream (InputStream csvStream)
    {
        List csvLinesVector = null;
        AWCsvConsumer csvConsumer = new AWCsvConsumer();
        CSVReader csvReader = new CSVReader(csvConsumer);
        try {
            csvReader.readForSpecifiedEncoding(csvStream, null);
        }
        catch (IOException exception) {
            throw new AWGenericException("Error parsing CSV from InputStream ", exception);
        }
        csvLinesVector = csvConsumer.lines();

        return csvLinesVector;
    }

    public static String[] parseComponentsString (String receiver, String separator)
    {
        // uses componentsSeparatedByString but returns a Java array sized to the
        // actual length of elements with each element trimmed of whitespace.
        StringArray stringArray = componentsSeparatedByString(receiver, separator);
        // size the StringArray to # of contained elements
        stringArray.trim();
        String[] strings = stringArray.array();
        for (int index = strings.length - 1; index < -1; index--) {
            String alternate = strings[index];
            strings[index] = alternate.trim();
        }
        return strings;
    }

    public static StringArray componentsSeparatedByString (String receiver, String separator)
    {
        StringArray components = new StringArray();
        int receiverLength = receiver.length();
        int separatorLength = separator.length();
        int currentIndex = 0;
        while (currentIndex < receiverLength) {
            int separatorIndex = receiver.indexOf(separator, currentIndex);
            if (separatorIndex == -1) {
                separatorIndex = receiverLength;
            }
            String substring = receiver.substring(currentIndex, separatorIndex);
            components.add(substring);
            currentIndex = separatorIndex + separatorLength;
        }
        if (currentIndex == receiverLength) {
            components.add("");
        }
        return components;
    }

    public static String componentsJoinedByString (StringArray receiver, String joinString)
    {
        String compoundString = null;
        int componentCount = receiver.inUse();
        if (componentCount > 0) {
            AWFastStringBuffer stringBuffer = new AWFastStringBuffer();
            String[] stringArray = receiver.array();
            String currentComponent = stringArray[0];
            stringBuffer.append(currentComponent);
            for (int index = 1; index < componentCount; index++) {
                stringBuffer.append(joinString);
                currentComponent = stringArray[index];
                stringBuffer.append(currentComponent);
            }
            compoundString = stringBuffer.toString();
        }
        return compoundString;
    }

    public static String replaceAllOccurrences (String originalString, String stringToReplace, String replacementString)
    {
        String finalString = originalString;
        if (AWUtil.contains(originalString, stringToReplace)) {
            StringArray stringComponents = componentsSeparatedByString(originalString, stringToReplace);
            finalString = componentsJoinedByString(stringComponents, replacementString);
        }
        return finalString;
    }

    public static String replaceStringByString (String originalString, String markerString, String replaceString)
    {
        int originalStringLength = originalString.length();
        int markerStringLength = markerString.length();
        int replaceStringLength = replaceString.length();
        int occurrences = StringUtil.occurs(originalString, markerString);
        if (occurrences == 0) {
            return(originalString);
        }
        int newLength = originalStringLength + (occurrences * replaceStringLength);
        FastStringBuffer buf = new FastStringBuffer(newLength);

        int oldoffset = 0;
        int offset = originalString.indexOf(markerString, 0);
        while (offset != -1) {
            buf.appendStringRange(originalString, oldoffset, offset);
            buf.append(replaceString);
            offset += markerStringLength;
            oldoffset = offset;
            offset = originalString.indexOf(markerString, offset);
        }
        buf.appendStringRange(originalString, oldoffset, originalStringLength);

        return buf.toString();
    }

    public static String replaceStringByChar (String originalString, String markerString, char replaceChar)
    {
        int originalStringLength = originalString.length();
        int markerStringLength = markerString.length();
        int occurrences = StringUtil.occurs(originalString, markerString);
        if (occurrences == 0) {
            return(originalString);
        }
        int newLength = originalStringLength + occurrences;
        FastStringBuffer buf = new FastStringBuffer(newLength);

        int oldoffset = 0;
        int offset = originalString.indexOf(markerString, 0);
        while (offset != -1) {
            buf.appendStringRange(originalString, oldoffset, offset);
            buf.append(replaceChar);
            offset += markerStringLength;
            oldoffset = offset;
            offset = originalString.indexOf(markerString, offset);
        }
        buf.appendStringRange(originalString, oldoffset, originalStringLength);

        return buf.toString();
    }

    static Pattern _LeadingSpacesPattern = Pattern.compile("(?m)^(\\s+)");

    public static String replaceLeadingSpacesWithNbsps (String str)
    {
        // todo: support for tabs?
        Matcher m = _LeadingSpacesPattern.matcher(str);
        StringBuffer buf = new StringBuffer();
        while (m.find()) {
            int spaceCount = m.group(1).length();
            m.appendReplacement(buf, AWUtil.repeatedString("&nbsp;", spaceCount));
        }
        m.appendTail(buf);
        return buf.toString();
    }

    public static String repeatedString (String str, int count)
    {
        FastStringBuffer buf = new FastStringBuffer(count * str.length());
        for (int i=0; i<count; i++) buf.append(str);
        return buf.toString();
    }

    public static String escapeStringForCsv (String originalString)
    {
        FastStringBuffer fastStringBuffer = new FastStringBuffer();
        int stringLength = originalString.length();
        for (int index = 0; index < stringLength; index++) {
            char currentChar = originalString.charAt(index);
            switch (currentChar) {
                case '\n':
                    fastStringBuffer.append("\\n");
                    break;
                case '\r':
                    fastStringBuffer.append("\\r");
                    break;
                case '\\':
                    fastStringBuffer.append("\\\\");
                    break;
                case '"':
                    fastStringBuffer.append("\"\"");
                    break;
                default:
                    fastStringBuffer.append(currentChar);
                    break;
            }
        }
        return fastStringBuffer.toString();
    }

    public static String unescapeCsvString (String originalString)
    {
        return MergedStringLocalizer.unescapeCsvString(originalString);
    }

    public static String substringTo (String sourceString, char targetChar)
    {
        String substring = null;
        int indexOfTarget = sourceString.indexOf(targetChar);
        if (indexOfTarget != -1) {
            substring = sourceString.substring(0, indexOfTarget);
        }
        return substring;
    }

    public static String substringTo (String sourceString, String targetString)
    {
        String substring = null;
        int indexOfTarget = sourceString.indexOf(targetString);
        if (indexOfTarget != -1) {
            substring = sourceString.substring(0, indexOfTarget);
        }
        return substring;
    }

    /* This was removed when the Util api's changed from 6.1 to 7.0
    public static Object propertyList (String sourceString)
    {
        Object propertyList = null;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceString.getBytes());
        try {
            propertyList = Util.loadObject(Util.bufferedReader(inputStream));
        }
        catch (RuntimeException exception) {
            // swallow for now.
        }
        return propertyList;
    }
    */

    public static boolean isSpace (char charValue)
    {
        return (charValue == ' ') || (charValue == '\t');
    }

    public static boolean isNewline (char charValue)
    {
        return (charValue == '\n') || (charValue == '\r');
    }

    public static boolean isLineFeed (char charValue)
    {
        return charValue == '\n';
    }

    public static boolean isWhitespace (char charValue)
    {
        return isNewline(charValue) || isSpace(charValue);
    }

    public static boolean isWhitespace (String targetString)
    {
        int stringLength = targetString.length();
        if (stringLength > 0) {
            for (int index = stringLength - 1; index >= 0; index--) {
                char currentChar = targetString.charAt(index);
                if (!isWhitespace(currentChar)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String leftJustify (String sourceString)
    {
        char[] sourceChars = sourceString.toCharArray();
        int sourceCharsLength = sourceChars.length;
        char[] destChars = new char[sourceCharsLength];
        int destCharIndex = 0;
        boolean inNewlineMode = false;
        for (int index = 0; index < sourceCharsLength; index++) {
            char currentChar = sourceChars[index];
            if (inNewlineMode) {
                if (isSpace(currentChar)) {
                    continue;
                }
                else {
                    if (!isLineFeed(currentChar)) {
                        inNewlineMode = false;
                    }
                    destChars[destCharIndex] = currentChar;
                    destCharIndex++;
                }
            }
            else {
                if (isNewline(currentChar)) {
                    inNewlineMode = true;
                }
                destChars[destCharIndex] = currentChar;
                destCharIndex++;
            }
        }
        return new String(destChars, 0, destCharIndex);
    }

    public static String stringPlusInt (String intString, int radix, int addend)
    {
        int intValue = Integer.parseInt(intString, radix);
        int resultInt = intValue + addend;
        return Integer.toString(resultInt, radix);
    }

    public static String removeAllWhitespace (String targetString)
    {
        int targetStringLength = targetString.length();
        char[] charBuffer = new char[targetStringLength];
        int charBufferIndex = 0;
        for (int index = 0; index < targetStringLength; index++) {
            char currentChar = targetString.charAt(index);
            if (!isWhitespace(currentChar)) {
                charBuffer[charBufferIndex] = currentChar;
                charBufferIndex++;
            }
        }
        String strippedString = targetString;
        if (charBufferIndex > 0) {
            strippedString = new String(charBuffer, 0, charBufferIndex);
        }
        return strippedString;
    }

    private static AWEncodedString _escapeHtml (Object objectValue, boolean unsafeOnly)
    {
        String keyString = AWUtil.toString(objectValue);
        AWEncodedString escapedEncodedString = null;
        AWSizeLimitedHashtable cache = unsafeOnly ? HtmlUnsafeEscapedStrings : HtmlEscapedStrings;
        if (RequiresThreadSafety) {
            synchronized (cache) {
                escapedEncodedString = (AWEncodedString)cache.get(keyString);
            }
        }
        else {
            escapedEncodedString = (AWEncodedString)cache.get(keyString);
        }
        if (escapedEncodedString == null) {
            String escapedString = null;
            if (AWEncodedString.debuggingEnabled()) {
                escapedString = escapeHTMLExceptEmbeddedContext(keyString, unsafeOnly);
            }
            else {
                escapedString = HTML.escape(keyString, unsafeOnly);
            }
            escapedEncodedString = AWEncodedString.sharedEncodedString(escapedString);
            if (escapedString.length() < MaxCacheStringSize) {
                if (RequiresThreadSafety) {
                    synchronized(cache) {
                        cache.put(keyString, escapedEncodedString);
                    }
                }
                else {
                    cache.put(keyString, escapedEncodedString);
                }
            }
        }
        return escapedEncodedString;
    }

    // Used to escape strings so that the embedded contextualization (EB) comments are not escaped.
    // Splits a string into five sections:  string1, EBbegin, string2, EBEnd, string3
    // and escapes the non-EB strings before reassembling the escaped string.

    public static String escapeHTMLExceptEmbeddedContext (String value)
    {
        return escapeHTMLExceptEmbeddedContext(value, false);
    }

    public static String escapeHTMLExceptEmbeddedContext (String value, boolean unsafeOnly)
    {
        int startOfEmbeddedContextBegin = value.indexOf(LocalizedStringBeginMarkerStart);
        int endOfEmbeddedContextBegin = -1;
        int startOfEmbeddedContextEnd = -1;
        int endOfEmbeddedContextEnd = -1;

        String string1 = null;
        String embeddedContextBegin = null;
        String string2 = null;
        String embeddedContextEnd = null;
        String string3 = null;

        String returnValue = null;

        if (startOfEmbeddedContextBegin != -1) {
            endOfEmbeddedContextBegin = value.indexOf(LocalizedStringBeginMarkerEnd,
                                                      startOfEmbeddedContextBegin + LocalizedStringBeginMarkerStart.length() + 1) +
                                                      LocalizedStringBeginMarkerEnd.length() - 1;
            if (endOfEmbeddedContextBegin != -1) {
                startOfEmbeddedContextEnd = value.indexOf(LocalizedStringEndMarker, endOfEmbeddedContextBegin);
                if (startOfEmbeddedContextEnd != -1) {
                    endOfEmbeddedContextEnd = startOfEmbeddedContextEnd + LocalizedStringEndMarker.length() - 1;

                    if (startOfEmbeddedContextBegin > 0) {
                        string1 = value.substring(0, startOfEmbeddedContextBegin);
                    }
                    embeddedContextBegin = value.substring(startOfEmbeddedContextBegin, endOfEmbeddedContextBegin + 1);
                    if (startOfEmbeddedContextEnd > endOfEmbeddedContextBegin + 1) {
                        string2 = value.substring(endOfEmbeddedContextBegin + 1, startOfEmbeddedContextEnd);
                    }
                    embeddedContextEnd = LocalizedStringEndMarker;
                    if (value.length() > endOfEmbeddedContextEnd + 1) {
                        string3 = value.substring(endOfEmbeddedContextEnd + 1, value.length());
                    }
                    returnValue = StringUtil.strcat(HTML.escape(string1, unsafeOnly),
                                                    embeddedContextBegin,
                                                    HTML.escape(string2, unsafeOnly),
                                                    embeddedContextEnd,
                                                    HTML.escape(string3, unsafeOnly));
                }
            }
        }

        if (returnValue == null) {
            returnValue = HTML.escape(value, unsafeOnly);
        }
        return returnValue;
    }

    public static AWEncodedString escapeHtml (Object objectValue)
    {
        AWEncodedString escapedEncodedString = null;
        if (objectValue instanceof AWEncodedString) {
            escapedEncodedString = ((AWEncodedString)objectValue).htmlEscapedString();
        }
        else if (objectValue != null) {
            escapedEncodedString = _escapeHtml(objectValue, false);
        }
        return escapedEncodedString;
    }

    public static AWEncodedString escapeUnsafeHtml (Object objectValue)
    {
        AWEncodedString escapedEncodedString = null;
        if (objectValue instanceof AWEncodedString) {
            escapedEncodedString = ((AWEncodedString)objectValue).htmlUnsafeEscapedString();
        }
        else if (objectValue != null) {
            escapedEncodedString = _escapeHtml(objectValue, true);
        }
        return escapedEncodedString;
    }

    public static AWEncodedString escapeXml (Object objectValue)
    {
        AWEncodedString encodedString = null;
        if (objectValue instanceof AWEncodedString) {
            encodedString = (AWEncodedString)objectValue;
        }
        else {
            String string = AWUtil.toString(objectValue);
            encodedString = AWEncodedString.sharedEncodedString(string);
        }
        return encodedString.xmlEscapedString();
    }

    public static AWEncodedString escapeHtmlAttribute (Object objectValue)
    {
        AWEncodedString escapedEncodedString = null;
        if (objectValue instanceof AWEncodedString) {
            escapedEncodedString = ((AWEncodedString)objectValue).htmlAttributeString();
        }
        else if (objectValue != null) {
            escapedEncodedString = (AWEncodedString)EncodedHtmlAttributes.get(objectValue);
            if (escapedEncodedString == null) {
                synchronized (EncodedHtmlAttributes) {
                    escapedEncodedString = (AWEncodedString)EncodedHtmlAttributes.get(objectValue);
                    if (escapedEncodedString == null) {
                        String keyString = null;
                        if (objectValue instanceof String) {
                            keyString = (String)objectValue;
                        }
                        else {
                            keyString = AWUtil.toString(objectValue);
                        }
                        if (keyString.indexOf('"') != -1) {
                            keyString = StringUtil.replaceCharByString(keyString, '"', AWUtil.QuoteString);
                            escapedEncodedString = AWEncodedString.sharedEncodedString(keyString);
                        }
                        else {
                            escapedEncodedString = AWEncodedString.sharedEncodedString(keyString);
                        }
                        EncodedHtmlAttributes.put(objectValue, escapedEncodedString);
                    }
                }
            }
        }
        return escapedEncodedString;
    }

    public static String integerIdString (int intValue)
    {
        return Integer.toString(intValue, IdStringRadix);
    }

    public static String toString (int intValue)
    {
        return ((intValue < MaxIntegerStrings) && (intValue >= 0)) ? IntegerStrings[intValue]: Integer.toString(intValue);
    }

    public static String toString (Object objectValue)
    {
        // this test allows us to avoid sending toString to Integer
        // and saves about 20% of object allocations in the system.
        String stringValue = null;
        if (objectValue != null) {
            if (objectValue instanceof String) {
                stringValue = (String)objectValue;
            }
            else if (objectValue instanceof Integer) {
                stringValue = toString(((Integer)objectValue).intValue());
            }
            else {
                stringValue = objectValue.toString();
            }
        }
        return stringValue;
    }

    /////////////////////
    // AWRequest Support
    /////////////////////
    public static Locale localeForBrowserLanguageString (String browserLanguageString)
    {
        Locale locale = (Locale)LocalesByBrowserLanguageString.get(browserLanguageString);
        if (locale == null) {
            synchronized (LocalesByBrowserLanguageString) {
                locale = (Locale)LocalesByBrowserLanguageString.get(browserLanguageString);
                if (locale == null) {
                    // input of the for 01[-23[;q=x.y]] eg. en-us or en-US;q=0.4
                    // note, the 'q=??' part is ignored.
                    String language = browserLanguageString.substring(0, 2).toLowerCase();
                    String country = (browserLanguageString.length() > 2) && (browserLanguageString.charAt(2) == '-') ? browserLanguageString.substring(3, 5).toUpperCase() : "";
                    locale = new Locale(language, country);
                    LocalesByBrowserLanguageString.put(browserLanguageString, locale);
                }
            }
        }
        return locale;
    }

    public static List localesForAcceptLanagugeString (String acceptLanguageString)
    {
        List requestLocales = ListUtil.list();
        int acceptLanguageStringLength = acceptLanguageString.length();
        int currentIndex = 0;
        while (currentIndex < acceptLanguageStringLength) {
            int commaIndex = acceptLanguageString.indexOf(',', currentIndex);
            String browserLanguageString = (commaIndex == -1) ? acceptLanguageString.substring(currentIndex) : acceptLanguageString.substring(currentIndex, commaIndex);
            Locale currentLocale = localeForBrowserLanguageString(browserLanguageString);
            requestLocales.add(currentLocale);
            if (commaIndex == -1) {
                break;
            }
            currentIndex = commaIndex + 1;
        }
        return requestLocales;
    }

    public static String formatErrorUrl (String fileName)
    {
        return StringUtil.strcat("/ERROR/Cannot.find.resource.named/", fileName, "/");
    }

    public static List vectorWithArray (Object objectArray[])
    {
        List newVector = ListUtil.list();
        int arrayCount = objectArray.length;
        for (int anIndex = 0; anIndex < arrayCount; anIndex++) {
            Object currentObject = objectArray[anIndex];
            AWUtil.addElement(newVector, currentObject);
        }
        return newVector;
    }

    public static boolean contains (String receiver, String targetString)
    {
        return (receiver.indexOf(targetString) != -1);
    }

    public static boolean contains (String receiver, char targetChar)
    {
        return (receiver.indexOf(targetChar) != -1);
    }

    public static int indexOf (Object[] array, Object targetElement)
    {
        int targetIndex = indexOfIdentical(array, targetElement, 0);
        if ((targetIndex == -1) && (targetElement != null)) {
            for (int index = array.length - 1; index >= 0; index--) {
                Object currentElement = array[index];
                if (targetElement.equals(currentElement)) {
                    targetIndex = index;
                    break;
                }
            }
        }
        return targetIndex;
    }

    public static boolean contains (Object[] array, Object targetElement)
    {
        return (indexOf(array, targetElement) != -1);
    }

    public static int indexOfIdentical (Object[] array, Object targetElement, int startingIndex)
    {
        int targetIndex = -1;
        int arrayLength = array.length;
        for (int index = startingIndex; index < arrayLength; index++) {
            Object currentElement = array[index];
            if (currentElement == targetElement) {
                targetIndex = index;
                break;
            }
        }
        return targetIndex;
    }

    public static int indexOfIdentical (Object[] array, Object targetElement)
    {
        return indexOfIdentical(array, targetElement, 0);
    }

    public static boolean containsIdentical (Object[] array, Object targetElement)
    {
        return (indexOfIdentical(array, targetElement, 0) != -1);
    }

    public static Object concatenateArrays (Object[] array1, Object[] array2, Class componentType)
    {
        Object newArray = null;
        int length1 = array1.length;
        int length2 = array2.length;
        newArray = Array.newInstance(componentType, (length1 + length2));
        System.arraycopy(array1, 0, newArray, 0, length1);
        System.arraycopy(array2, 0, newArray, length1, length2);
        return newArray;
    }

    public static Object concatenateArrays (Object[] array1, Object[] array2)
    {
        int length1 = array1.length;
        Class componentType1 = (length1 == 0) ? null : array1.getClass().getComponentType();
        int length2 = array2.length;
        Class componentType2 = (length2 == 0) ? null : array2.getClass().getComponentType();
        Class componentType = null;
        if ((componentType1 != null) && (componentType2 != null)) {
            componentType = commonSuperclass(componentType1, componentType2);
        }
        else if (componentType1 != null) {
            componentType = componentType1;
        }
        else if (componentType2 != null) {
            componentType = componentType2;
        }
        else {
            throw new AWGenericException("Unable to concatenate two empty arrays -- use concatenateArrays(Object[], Object[], Class)");
        }
        return concatenateArrays(array1, array2, componentType);
    }

    public static int length (Object[] array)
    {
        return array == null ? 0 : array.length;
    }

    public static boolean equals (byte[] bytes1, byte[] bytes2)
    {
        boolean equals = (bytes1 == bytes2);
        if (!equals && (bytes1 != null) && (bytes2 != null)) {
            int length = bytes1.length;
            if (length == bytes2.length) {
                for (int index = length - 1; index >= 0; index--) {
                    if (bytes1[index] != bytes2[index]) {
                        return false;
                    }
                }
                equals = true;
            }
        }
        return equals;
    }

    public static int indexOf (byte[] bytes, byte[] targetBytes, int startIndex)
    {
        int targetBytesLength = targetBytes.length;
        int searchLength = bytes.length - targetBytesLength + 1;
        byte firstTargetByte = targetBytes[0];
        for (int index = startIndex; index < searchLength; index++) {
            if (bytes[index] == firstTargetByte) {
                for (int targetIndex = 1; targetIndex < targetBytesLength; targetIndex++) {
                    if (bytes[index + targetIndex] != targetBytes[index]) {
                        continue;
                    }
                }
                // we'll only get here if all bytes in targetBytes match.
                return index;
            }
        }
        return -1;
    }

    public static int indexOf (String string, String targetString, int startIndex, boolean ignoresCase)
    {
        int stringLength = string.length();
        int targetLength = targetString.length();
        int searchLength = stringLength - targetLength + 1;
        for (int index = startIndex; index < searchLength; index++) {
            if (string.regionMatches(ignoresCase, index, targetString, 0, targetLength)) {
                return index;
            }
        }
        return AWConstants.NotFound;
    }

    public static Object removeElementAt (Object[] targetArray, int index)
    {
        Object removedElement = targetArray[index];
        int srcPos = index + 1;
        int length = targetArray.length - srcPos;
        System.arraycopy(targetArray, srcPos, targetArray, index, length);
        return removedElement;
    }

    public static void moveToEnd (Object[] targetArray, int index)
    {
        // this assumes the array is exactly the correct length.
        int lastIndex = targetArray.length - 1;
        if (index < lastIndex) {
            Object removedElement = removeElementAt(targetArray, index);
            targetArray[lastIndex] = removedElement;
        }
    }

    public static Object realloc (Object[] targetArray, int newSize)
    {
        Object newArray = targetArray;
        int targetArrayLength = targetArray.length;
        if (newSize > targetArrayLength) {
            Class componentType = targetArray.getClass().getComponentType();
            newArray = Array.newInstance(componentType, newSize);
            System.arraycopy(targetArray, 0, newArray, 0, targetArrayLength);
        }
        return newArray;
    }

    public static char[] realloc (char[] targetArray, int newSize)
    {
        char[] newArray = targetArray;
        int targetArrayLength = targetArray.length;
        if (newSize > targetArrayLength) {
            newArray = new char[newSize];
            System.arraycopy(targetArray, 0, newArray, 0, targetArrayLength);
        }
        return newArray;
    }

    public static int[] realloc (int[] targetArray, int newSize)
    {
        int[] newArray = targetArray;
        int targetArrayLength = targetArray.length;
        if (newSize > targetArrayLength) {
            newArray = new int[newSize];
            System.arraycopy(targetArray, 0, newArray, 0, targetArrayLength);
        }
        return newArray;
    }

    public static short[] realloc (short[] targetArray, int newSize)
    {
        short[] newArray = targetArray;
        int targetArrayLength = targetArray.length;
        if (newSize > targetArrayLength) {
            newArray = new short[newSize];
            System.arraycopy(targetArray, 0, newArray, 0, targetArrayLength);
        }
        return newArray;
    }

    public static long[] realloc (long[] targetArray, int newSize)
    {
        long[] newArray = targetArray;
        int targetArrayLength = targetArray.length;
        if (newSize > targetArrayLength) {
            newArray = new long[newSize];
            System.arraycopy(targetArray, 0, newArray, 0, targetArrayLength);
        }
        return newArray;
    }

    public static Object addElement (Object[] targetArray, Object newElement)
    {
        Object newArray = null;
        int targetArrayLength = 0;
        if (targetArray == null) {
            Class componentType = newElement.getClass();
            newArray = Array.newInstance(componentType, 1);
        }
        else {
            targetArrayLength = targetArray.length;
            newArray = realloc(targetArray, targetArrayLength + 1);
        }
        Array.set(newArray, targetArrayLength, newElement);
        return newArray;
    }

    public static Object sublist (Object[] sourceArray, int fromIndex, int toIndex)
    {
        Class componentType = sourceArray.getClass().getComponentType();
        int sublistLength = toIndex - fromIndex;
        Object newArray = Array.newInstance(componentType, sublistLength);
        System.arraycopy(sourceArray, fromIndex, newArray, 0, sublistLength);
        return newArray;
    }

    public static Object subarray (Object array , int startIndex, int stopIndex)
    {
        Object[] sourceArray = (Object[])array;
        Class componentType = sourceArray.getClass().getComponentType();
        int destinationArrayLength = stopIndex - startIndex  + 1;
        Object destinationArray = Array.newInstance(componentType, destinationArrayLength);
        System.arraycopy(sourceArray, startIndex, destinationArray, 0, destinationArrayLength);
        return destinationArray;
    }

    public static void addElements (Map destination, Map source)
    {
        if (source != null && !source.isEmpty()) {
            Iterator sourceKeyEnumerator = source.keySet().iterator();
            while (sourceKeyEnumerator.hasNext()) {
                Object currentSourceKey = sourceKeyEnumerator.next();
                Object currentSourceElement = source.get(currentSourceKey);
                destination.put(currentSourceKey, currentSourceElement);
            }
        }
    }

    public static void addElements (Map destination, Map source, boolean allowsReplace)
    {
        if (source != null && !source.isEmpty()) {
            boolean notAllowsReplace = !allowsReplace;
            Iterator sourceKeyEnumerator = source.keySet().iterator();
            while (sourceKeyEnumerator.hasNext()) {
                Object currentSourceKey = sourceKeyEnumerator.next();
                if (notAllowsReplace && (destination.get(currentSourceKey) != null)) {
                    throw new AWGenericException("Attempt to add key which already exists: " + currentSourceKey);
                }
                Object currentSourceElement = source.get(currentSourceKey);
                destination.put(currentSourceKey, currentSourceElement);
            }
        }
    }

    public static void addElements (Map destinationHashtable, Object[] objectArray)
    {
        for (int index = objectArray.length - 1; index >= 0; index--) {
            Object currentObject = objectArray[index];
            destinationHashtable.put(currentObject, currentObject);
        }
    }

    public static Map map (Object ...keysAndValues)
    {
        int c = keysAndValues.length;
        Assert.that(c % 2 == 0, "Must have even number of args: key, value, ... : %s");
        Map result = new HashMap(c/2);
        for (int i=0; i < c; i += 2) {
            result.put(keysAndValues[i], keysAndValues[i+1]);
        }
        return result;
    }

    public static List list (Object ...elements)
    {
        int c = elements.length;
        List result = new ArrayList(c);
        for (int i=0; i < c; i ++) {
            result.add(elements[i]);
        }
        return result;
    }

    public static Object[] keys (Map sourceHashtable, Class componentClass)
    {
        Object[] objectArray = (Object[])Array.newInstance(componentClass, sourceHashtable.size());
        if (!sourceHashtable.isEmpty()) {
            Iterator keyEnumerator = sourceHashtable.keySet().iterator();
            int index = 0;
            while (keyEnumerator.hasNext()) {
                Object currentKey = keyEnumerator.next();
                objectArray[index++] = currentKey;
            }
        }
        return objectArray;
    }

    public static Object[] elements (Map sourceHashtable, Class componentClass)
    {
        Object[] objectArray = (Object[])Array.newInstance(componentClass, sourceHashtable.size());
        if (!sourceHashtable.isEmpty()) {
            Iterator elementEnumerator = sourceHashtable.values().iterator();
            int index = 0;
            while (elementEnumerator.hasNext()) {
                Object currentKey = elementEnumerator.next();
                objectArray[index++] = currentKey;
            }
        }
        return objectArray;
    }

    public static Map putInNewHashtable (Map readableHashtable, Object key, Object value)
    {
        Map newHashtable = null;
        if (readableHashtable == null) {
            readableHashtable = MapUtil.map(1);
            newHashtable = readableHashtable;
        }
        else {
            newHashtable = MapUtil.cloneMap(readableHashtable);
        }
        newHashtable.put(key, value);
        return newHashtable;
    }

    public static Map putInHashtable (Map receiverHashtable, Object key, Object value, boolean makeCopy)
    {
        if (makeCopy) {
            receiverHashtable = AWUtil.putInNewHashtable(receiverHashtable, key, value);
        }
        else {
            receiverHashtable.put(key, value);
        }
        return receiverHashtable;
    }

    public static void internKeysAndValues (Map hashtable)
    {
        Iterator keyIterator = hashtable.keySet().iterator();
        while (keyIterator.hasNext()) {
            String currentKey = (String)keyIterator.next();
            Object currentValue = hashtable.get(currentKey);
            if (currentValue instanceof String) {
                currentValue = ((String)currentValue).intern();
            }
            else if (currentValue instanceof Map) {
                internKeysAndValues((Map)currentValue);
            }
            hashtable.put(currentKey.intern(), currentValue);
        }
    }

    public interface ValueMapper {
        public Object valueForObject (Object object);
    }

    // return map of lists of items, keyed by the result of applying accessor to each item
    public static Map groupBy (Collection items, ValueMapper accessor)
    {
        Map result = MapUtil.map();
        for (Object o : items) {
            Object key = accessor.valueForObject(o);
            if (key != null)
            {
                List keyList = (List)result.get(key);
                if (keyList == null) {
                    keyList = ListUtil.list();
                    result.put(key, keyList);
                }
                keyList.add(o);
            }
        }
        return result;
    }

    // Collect into supplied result collection the result of apply accessor to all items
    public static Collection collect (List items, Collection result, ValueMapper accessor)
    {
        for (int i=0, count=items.size(); i < count; i++) {
            Object o = items.get(i);
            Object value = accessor.valueForObject(o);
            result.add(value);
        }
        return result;
    }

    // return list of results of applying accessors to all items
    public static List collect (List items, ValueMapper accessor)
    {
        return (List)collect(items, ListUtil.list(), accessor);
    }

    public static void addElements (List destination, List source)
    {
        if (source != null) {
            int sourceElementCount = source.size();
            for (int index = 0; index < sourceElementCount; index++) {
                Object currentSourceElement = source.get(index);
                destination.add(currentSourceElement);
            }
        }
    }

    public static void addElements (List destination, Object[] source)
    {
        if (source != null) {
            int sourceElementCount = source.length;
            for (int index = 0; index < sourceElementCount; index++) {
                Object currentSourceElement = source[index];
                destination.add(currentSourceElement);
            }
        }
    }

    public static void addElement (List receiver, Object element)
    {
        if (element != null) {
            receiver.add(element);
        }
    }

    public static Object getArray (List receiver, Class componentType)
    {
        Object newArray = Array.newInstance(componentType, receiver.size());
        receiver.toArray((Object[])newArray);
        return newArray;
    }

    public static void removeFromIndex (List receiver, int targetIndex)
    {
        int removalCount = receiver.size() - targetIndex;
        while (removalCount > 0) {
            removalCount--;
            ListUtil.removeLastElement(receiver);
        }
    }

    public static int lastIndexOfIdentical (List receiver, Object targetObject, int startingIndex)
    {
        return ListUtil.lastIndexOfIdentical(receiver, targetObject, startingIndex);
    }

    public static int lastIndexOfIdentical (List receiver, Object targetObject)
    {
        return lastIndexOfIdentical(receiver, targetObject, receiver.size() - 1);
    }

    public static void clear (DynamicArray dynamicArray)
    {
        Object[] array = dynamicArray.array;
        for (int index = array.length - 1; index >= 0; index--) {
            array[index] = null;
        }
        dynamicArray.inUse = 0;
    }

    ///////////////////////
    // Locale Descriptions
    ///////////////////////
    public static String fullLocaleDescription (Locale locale)
    {
        return locale.toString();
    }

    public static String countryLocaleDescription (Locale locale)
    {
        return StringUtil.strcat(locale.getLanguage(), "_", locale.getCountry());
    }

    public static String languageLocaleDescription (Locale locale)
    {
        return locale.getLanguage();
    }

    ///////////////////////////
    // FileManager Extensions
    ///////////////////////////
    public static String[] filesWithExtension (File directory, String fileExtension)
    {
        AWExtensionFilenameFilter filnameFilter = new AWExtensionFilenameFilter(fileExtension);
        return directory.list(filnameFilter);
    }

    public static String[] filesWithExtension (String directoryName, String fileExtension)
    {
        File directory = new File(directoryName);
        return filesWithExtension(directory, fileExtension);
    }

    public interface FileProcessor
    {
        void process (File file);
    }

    public static void eachFile (File dir, FileFilter filter, FileProcessor callback)
    {
        if (dir == null) return;

        try {
            if (!dir.isDirectory()) return;

            File[] list = dir.listFiles(filter);
            if (list != null) {
                for (int i=0; i<list.length; ++i) {
                    File file = list[i];
                    if (!file.isDirectory()) {
                        callback.process(file);
                    }
                    else {
                        eachFile(file, filter, callback);
                    }
                }
            }
        }
        catch (SecurityException se) {
            // ignore
        }
    }

    public static String lastComponent (String delimitedString, String separatorString)
    {
        String lastComponent = delimitedString;
        int indexOfLastSeparator = delimitedString.lastIndexOf(separatorString);
        if (indexOfLastSeparator != -1) {
            lastComponent = delimitedString.substring(indexOfLastSeparator + 1);
        }
        return lastComponent;
    }

    public static String pathToLastComponent (String delimitedString, String separatorString)
    {
        String path = delimitedString;
        int indexOfLastSeparator = delimitedString.lastIndexOf(separatorString);
        if (indexOfLastSeparator != -1) {
            path = delimitedString.substring(0,indexOfLastSeparator);
        }
        return path;
    }

    public static String fileNameToJavaPackage (String delimitedString)
    {
        String path = pathToLastComponent(delimitedString,"/");
        path = path.replace('/','.');
        return path;
    }

    public static String lastComponent (String delimitedString, char separatorChar)
    {
        String lastComponent = delimitedString;
        int indexOfLastSeparator = delimitedString.lastIndexOf(separatorChar);
        if (indexOfLastSeparator != -1) {
            lastComponent = delimitedString.substring(indexOfLastSeparator + 1);
        }
        return lastComponent;
    }

    public static String stripLastComponent (String delimitedString, char separatorChar)
    {
        String lastComponent = delimitedString;
        int indexOfLastSeparator = delimitedString.lastIndexOf(separatorChar);
        if (indexOfLastSeparator != -1) {
            lastComponent = delimitedString.substring(0, indexOfLastSeparator);
        }
        return lastComponent;
    }

    public static File uniqueFile (File directory, String prefix, String suffix)
    {
        File tempFile = null;
        boolean fileExists = true;
        while (fileExists) {
            String currentTimeMillis = Long.toString(System.currentTimeMillis());
            String tempFileName = StringUtil.strcat(prefix, currentTimeMillis, suffix);
            tempFile = new File(directory, tempFileName);
            fileExists = tempFile.exists();
        }
        return tempFile;
    }

    public static String uniqueFileName (File directory, String prefix, String suffix)
    {
        File tempFile = uniqueFile(directory, prefix, suffix);
        return tempFile.getName();
    }

    private static int indexOfFileName (String filePath)
    {
        int slashIndex = filePath.lastIndexOf('/');
        int backslashIndex = filePath.lastIndexOf('\\');
        return AWUtil.max(slashIndex, backslashIndex);
    }

    public static String stripToBaseFilename (String filePath)
    {
        int index = indexOfFileName(filePath);
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex == -1) {
            dotIndex = filePath.length();
        }
        return filePath.substring(index + 1, dotIndex);
    }

    // E.g. decamelize("firstName", ' ', true) --> "First Name"
    static final Pattern _InitialDigits = Pattern.compile("^(\\d+)_(.+)");

    public static String decamelize (String string, char separator, boolean initialCaps)
    {
        // Turn "1_Foo" to "1. Foo"
        Matcher m = _InitialDigits.matcher(string);
        if (m.matches()) {
            string = Fmt.S("%s. %s", m.group(1), m.group(2));
        }

        boolean splitOnUC = !string.contains("_");

        boolean allCaps = true;
        FastStringBuffer buf = new FastStringBuffer();
        int lastUCIndex = -1;
        for (int i=0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i-1 != lastUCIndex && splitOnUC) buf.append(separator);
                lastUCIndex = i;
                if (!initialCaps) c = Character.toLowerCase(c);
            }
            else if (Character.isLowerCase(c)) {
                if (i==0 && initialCaps) c = Character.toUpperCase(c);
                allCaps = false;
            }
            else if (c == '_') {
                c = separator;
            }
            buf.append(c);
        }

        // do mixed (initial word) case for all-caps strings
        if (allCaps) {
            boolean inWord = false;
            for (int i=0, c=buf.length(); i < c; i++) {
                char ch = buf.charAt(i);
                if (Character.isLetter(ch)) {
                    if (inWord && Character.isUpperCase(ch)) {
                        buf.setCharAt(i, Character.toLowerCase(ch));
                    }
                    inWord = true;
                } else {
                    inWord = false;
                }
            }
        }

        return buf.toString();
    }

    /////////////////
    // Debugging
    /////////////////
    public static int burnCpu100 ()
    {
        return burnCpu(2 * 1000 * 1000);
    }

    public static int burnCpu (int loopCount)
    {
        // counter is here to ensure that this will not be optimized away.
        int counter = 0;
        while (loopCount-- > 0) {
            counter++;
        }
        return counter;
    }

    public static java.util.Date currentDate ()
    {
        return new java.util.Date();
    }

    ////////////////////////
    // Localization Support
    ////////////////////////
    private static void writeComponentsLocalizedStringsToCsvFile (FileWriter csvFileWriter, Map componentsLocalizedStrings, String componentName)
    {
        try {
            Iterator localizedStringKeyIterator = componentsLocalizedStrings.keySet().iterator();
            while (localizedStringKeyIterator.hasNext()) {
                String currentNativeStringKey = (String)localizedStringKeyIterator.next();
                AWCommentedString currentCommentedString = (AWCommentedString)componentsLocalizedStrings.get(currentNativeStringKey);
                String currentNativeString = currentCommentedString.string;
                String currentComment = currentCommentedString.comment;
                String escapedKey = AWUtil.escapeStringForCsv(currentNativeStringKey);
                String escapedNativeString = AWUtil.escapeStringForCsv(currentNativeString);
                if (currentComment == null) {
                    currentComment = "-none-";
                }
                csvFileWriter.write("\"");
                csvFileWriter.write(componentName);
                csvFileWriter.write("\",\"");
                csvFileWriter.write(escapedKey);
                csvFileWriter.write("\",\"");
                csvFileWriter.write(escapedNativeString);
                csvFileWriter.write("\",\"");
                csvFileWriter.write(escapedNativeString);
                csvFileWriter.write("\",\"");
                csvFileWriter.write(currentComment);
                csvFileWriter.write("\"\r\n");
            }
        }
        catch (IOException exception) {
            throw new AWGenericException(exception);
        }
    }

    public static void writeLocalizedStringsFile (String directoryName, String componentName, Map localizedStringsHastable)
    {
        if (!localizedStringsHastable.isEmpty()) {
            FileWriter csvFileWriter = null;
            try {
                File directory = new File(directoryName);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, StringUtil.strcat(componentName, ".strings"));
                csvFileWriter = new FileWriter(file);
                writeComponentsLocalizedStringsToCsvFile(csvFileWriter, localizedStringsHastable, componentName);
            }
            catch (IOException exception) {
                throw new AWGenericException(exception);
            }
            finally {
                try {
                    csvFileWriter.close();
                }
                catch (IOException exception) {
                    throw new AWGenericException(exception);
                }
            }
        }
    }

    /**
        Returns a string table for the List of input lines from the
        resource file.
    */

    public static Map convertToLocalizedStringsTable (List lines)
    {
        return convertToLocalizedStringsTable(lines, null);
    }

    /**
        Returns a string table for the List of input lines from the
        resource file.

        If resourcePath is non-null, the embedded contextualization information
        is included as part of the localized strings in the string table.
    */

    public static Map convertToLocalizedStringsTable (List lines, String resourcePath)
    {
        Map allLocalizedStringsTable = MapUtil.map();
        int linesCount = lines.size();
        for (int index = 0; index < linesCount; index++) {
            List currentLineVector = (List)lines.get(index);
            if (currentLineVector.size() >= 4) {
                String componentName = (String)ListUtil.firstElement(currentLineVector);
                String localizedStringKey = (String)currentLineVector.get(1);
                String translatedString = (String)currentLineVector.get(3);
                Map componentStringsHashtable = (Map)allLocalizedStringsTable.get(componentName);
                if (componentStringsHashtable == null) {
                    componentStringsHashtable = MapUtil.map();
                    allLocalizedStringsTable.put(componentName, componentStringsHashtable);
                }
                if (resourcePath != null) {
                    translatedString =
                        addEmbeddedContext(localizedStringKey,
                                           translatedString, resourcePath);
                }
                componentStringsHashtable.put(localizedStringKey, translatedString);
            }
        }
        return allLocalizedStringsTable;
    }

    /**
        Called to return the localized string along with embedded contextualization
        information.
    */

    private static String addEmbeddedContext (String key, String localizedString,
                                              String resourcePath)
    {
        String returnVal = StringUtil.strcat(LocalizedStringBeginMarkerStart,
                                             resourcePath,
                                             LocalizedStringSpace,
                                             key,
                                             LocalizedStringBeginMarkerEnd,
                                             localizedString,
                                             LocalizedStringEndMarker);


        return returnVal;
    }

    public static String addEmbeddedContextForDefaultString (int key, String defaultString, String componentPath)
    {
        String keyString = AWUtil.toString(key);

        String returnVal = StringUtil.strcat(LocalizedStringBeginMarkerStart,
                                             componentPath,
                                             LocalizedStringSpace,
                                             keyString,
                                             LocalizedStringBeginMarkerEnd,
                                             defaultString,
                                             LocalizedStringEndMarker);
        return returnVal;
    }

    public static String getEmbeddedContextBegin (String key, String componentPath)
    {
        String returnVal = StringUtil.strcat(LocalizedStringBeginMarkerStart,
                                             componentPath,
                                             LocalizedStringSpace,
                                             key,
                                             LocalizedStringBeginMarkerEnd);
        return returnVal;
    }

    public static String getEmbeddedContextEnd ()
    {
        return LocalizedStringEndMarker;
    }

    public static boolean deleteRecursive (File targetFile)
    {
        boolean didDelete = false;
        if (targetFile.isFile()) {
            didDelete = targetFile.delete();
        }
        else if (targetFile.isDirectory()) {
            String[] filenameList = targetFile.list();
            if (filenameList != null) {
                for (int index = filenameList.length - 1; index >= 0; index--) {
                    String currentFilename = filenameList[index];
                    File currentFile = new File(targetFile, currentFilename);
                    deleteRecursive(currentFile);
                }
            }
            didDelete = targetFile.delete();
        }
        return didDelete;
    }

    /* ----------------------
        System Command Execution
        --------------------- */

    public static int executeCommand (String commandString)
    {
        int exitValue = 0;
        try {
            //System.out.println("** executeCommand: " + commandString);
            Process process = Runtime.getRuntime().exec(commandString);
            String osName = System.getProperties().getProperty("os.name");
            if (!"Windows NT".equals(osName)) {
                process.waitFor();
            }
            // Note: this is required for process to finish
            AWUtil.stringWithContentsOfInputStream(process.getInputStream());
            InputStream errorStream = process.getErrorStream();
            if (errorStream != null) {
                String errorOutput = AWUtil.stringWithContentsOfInputStream(errorStream);
                System.out.println("** errorOutput: " + errorOutput);
            }
            exitValue = process.exitValue();
            process.destroy();
        }
        catch (InterruptedException exception) {
            System.out.println("** exception: " + exception);
            System.out.println(SystemUtil.stackTrace(exception));
            exitValue = -1;
        }
        catch (IOException exception) {
            System.out.println("** exception: " + exception);
            System.out.println(SystemUtil.stackTrace(exception));
            exitValue = -1;
        }
        return exitValue;
    }

    /**
        Parse a string that is URL encoded.  This method is copied from
        ariba.util.core.net.CGI, that version has a bug that is not yet fixed.
    */

    public static final String DefaultCharacterEncoding = AWCharacterEncoding.Default.name;

    public static String decodeString (String string)
    {
        return decodeString(string, DefaultCharacterEncoding);
    }

    public static String decodeString (String string, String encoding)
    {
        FastStringBuffer buffer = new FastStringBuffer();

        boolean hasError = false;
        int length = string.length();
        int i = -1;
        for (i = 0; i < length; i++) {
            char c = string.charAt(i);

            switch (c) {

              case '+':
                buffer.append(' ');
                break;

              case '%':
                String str = "";
                 // if the % is not trailed by two characters, just move on similar
                  // to the case where the trailing two chars are not valid integer
                if (length < i+3) {
                    hasError = true;
                    break;
                }

                try {
                    str = string.substring(i+1, i+3);
                    int val = IntegerFormatter.parseInt(str, 16);
                    buffer.append((char)val);
                }
                catch (ParseException e) {
                    hasError = true;
                        // if the parse fails just append the chars
                    buffer.append(str);
                }
                i += 2;
                break;

              default:
                buffer.append(c);
                break;
            }
        }

        String result = buffer.toString();
        if (hasError) {
            Log.aribaweb.warn("****** decode warning/invalid string: " + string);
        }

        try {
            return new String(result.getBytes(I18NUtil.EncodingISO8859_1),
                              encoding);
        }
        catch (UnsupportedEncodingException uee0) {
            try {
                return new String(result.getBytes(I18NUtil.EncodingISO8859_1),
                                                  DefaultCharacterEncoding);
            }
            catch (UnsupportedEncodingException uee1) {
                // This never happens.
                return result;
            }
        }
    }

    /**
        Encode a string into x-www-form-urlencoded format.
        This method is based on java.net.URLEncoder.
        Because the original one always uses the system default encoding,
        it doesn't meet our requirement.
        I changed OutputStreamWriter to have an encoding argument
        at its instantiation.

    */
    private static BitSet dontNeedEncoding;
    private static final int caseDiff = ('a' - 'A');

    static {
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
            // encoding a space to a + is done in the encode method
        dontNeedEncoding.set(' ');
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');
    }

    public static String encodeString (String string)
    {
        return encodeString(string, true, DefaultCharacterEncoding);
    }

    public static String encodeString (String string, boolean isPlusAllowed)
    {
        return encodeString(string, isPlusAllowed, DefaultCharacterEncoding);
    }

    /*
      If isPlusAllowed is false, a whitespace is encoded as '%20' instead of '+'.
      This is because Netscape 4.x doesn't like the URL redirect with +'s
      (perhaps a problem with Javascript?)
    */
    public static String encodeString (String string, boolean isPlusAllowed, String encoding)
    {
        if (StringUtil.nullOrEmptyOrBlankString(string) ) {
            return string;
        }
        int maxBytesPerChar = 10;
        FastStringBuffer out = new FastStringBuffer(string.length());
        ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);

        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(buf, encoding);
        }
        catch (UnsupportedEncodingException uee0) {
            try {
                writer = new OutputStreamWriter(buf, DefaultCharacterEncoding);
            }
            catch (UnsupportedEncodingException uee1) {
                    // This never happens.
                uee1.printStackTrace();
            }
        }

        for (int i = 0; i < string.length(); i++) {
            int c = (int)string.charAt(i);
            if (dontNeedEncoding.get(c)) {
                if (c == ' ') {
                    if (isPlusAllowed) {
                        c = '+';
                    }
                    else {
                        out.append("%20");
                        continue;
                    }
                }
                out.append((char)c);
            }
            else {
                    // convert to external encoding before hex conversion
                try {
                    writer.write(c);
                    writer.flush();
                }
                catch(IOException e) {
                    buf.reset();
                    continue;
                }
                byte[] ba = buf.toByteArray();
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                        // converting to use uppercase letter as part of
                        // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                buf.reset();
            }
        }

        return out.toString();
    }

    public static String attributeEscape (String string)
    {
        return StringUtil.replaceCharByString(string, '"', AWUtil.QuoteString);
    }

    /* Pattern to filter out all unsafe chars in HTTP response header */
    private static Pattern UnsafeHeaderPattern = Pattern.compile("\\r|\\n|%0(D|d|A|a)");

    public static String filterUnsafeHeader (String url)
    {
        return UnsafeHeaderPattern.matcher(url).replaceAll("");
    }

    public static AWStringKeyHashtable parseQueryString (String queryString)
    {
        AWStringKeyHashtable queryStringValues = new AWStringKeyHashtable();
        parseQueryString(queryString, queryStringValues);
        return queryStringValues;
    }

    public static void parseQueryString (String queryString, Map queryStringValues)
    {
        if (queryString != null) {
            StringTokenizer tokens = new StringTokenizer(queryString, "&");
            String key;
            String value;
            while (tokens.hasMoreTokens()) {
                String keyval = tokens.nextToken();
                StringTokenizer keyValTokens = new StringTokenizer(keyval, "=");
                key = AWUtil.decodeString(keyValTokens.nextToken());
                try {
                    value = AWUtil.decodeString(keyValTokens.nextToken());
                    String[] existingValue = (String[])queryStringValues.get(key);
                    if (existingValue == null) {
                        String[] newStringArray = {value};
                        existingValue = newStringArray;
                    }
                    else {
                        existingValue = (String[])AWUtil.addElement(existingValue, value);
                    }
                    queryStringValues.put(key, existingValue);
                }
                catch (RuntimeException e) {
                    String[] defaultStringArray = {""};
                    queryStringValues.put(key, defaultStringArray);
                }
            }
        }
    }

    public static Map parseParameters (String url)
    {
        Map map = MapUtil.map();
        int startIndex = url.indexOf(BeginQueryChar);
        if (startIndex == url.length() -1) {
            return map;
        }
        url = url.substring(startIndex+1);
        StringTokenizer st = new StringTokenizer(url, TokenizerDelim);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int indexOfEquals = token.indexOf(Equals);

            if (indexOfEquals != -1) {
                String key = token.substring(0, indexOfEquals);
                if (indexOfEquals == token.length()) {
                    map.put(key, "");
                }
                else {
                    String value = token.substring(indexOfEquals+1, token.length());
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    public static String queryValue (Map queryValues, String key)
    {
        String[] valuesArray = (String[])queryValues.get(key);
        return (valuesArray == null) ? null : valuesArray[0];
    }

    public static void putNonNull (Map hashtable, Object key, Object value)
    {
        if (key != null && value != null) {
            hashtable.put(key, value);
        }
    }

    /**
        For internal use only.
        This should never be called by production code.
    */
    public static synchronized String getenv (String envVar)
    {
        return System.getenv().get(envVar);
    }


    /**
     http://www.espn.com          ->   http://www.espn.com?
     http://www.espn.com?         ->   http://www.espn.com?
     http://www.espn.com?foo=bar  ->   http://www.espn.com?foo=bar&
     http://www.espn.com?foo=bar& ->   http://www.espn.com?foo=bar&
    */
    public static String prepareUrlForQueryValues (String url)
    {
        int index = url.indexOf('?');
        if (index == -1) {
            return StringUtil.strcat(url, "?");
        }
        else if (index < url.length() - 1) {
            if (!url.endsWith("&")) {
                return StringUtil.strcat(url, "&");
            }
        }
        return url;
    }

    public static String urlAddingQueryValues (String url, Map values)
    {
        FastStringBuffer buf = new FastStringBuffer(url);
        Iterator iter = values.keySet().iterator();
        boolean isFirst = true;
        while (iter.hasNext()) {
            String key = (String)iter.next();
            String val = (String)values.get(key);
            if (!StringUtil.nullOrEmptyOrBlankString(val)) {
                char delim = '&';
                if (isFirst && url.indexOf('?') == -1) delim = '?';
                isFirst = false;
                buf.append(delim);
                buf.append(key);
                buf.append('=');
                buf.append(AWUtil.encodeString(val));
            }
        }
        return buf.toString();
    }

    public static String urlAddingQueryValue (String url, String key, String val)
    {
        if (StringUtil.nullOrEmptyOrBlankString(val)) return url;
        FastStringBuffer buf = new FastStringBuffer(url);
        char delim = (url.indexOf('?') == -1) ? '?' : '&';
        buf.append(delim);
        buf.append(key);
        buf.append('=');
        buf.append(AWUtil.encodeString(val));
        return buf.toString();
    }

    static String relativeUrlString (URL startUrl, URL destUrl)
    {
        if (!startUrl.getProtocol().equals(destUrl.getProtocol())
                || !startUrl.getHost().equals(destUrl.getHost())) return destUrl.toExternalForm();

        String[] start = startUrl.getPath().split("/");
        String[] end = destUrl.getPath().split("/");
        int lastCommonIdx = 0;
        int max = Math.min(start.length, end.length);
        for ( ; lastCommonIdx < max; lastCommonIdx++) {
            if (!start[lastCommonIdx].equals(end[lastCommonIdx])) {
                break;
            }
        }
        lastCommonIdx--;
        StringBuilder buf = new StringBuilder();
        int popCount = start.length - lastCommonIdx - 2;
        while (popCount-- > 0) {
            buf.append("../");
        }
        for (int i=lastCommonIdx+1; i < end.length; i++) {
            buf.append(end[i]);
            if (i != end.length-1) buf.append("/");
        }
        return buf.toString();
    }

    public static void dispose (Object object)
    {
        if (object != null) {
            if (object instanceof AWDisposable) {
                ((AWDisposable)object).dispose();
            }
            else if (object instanceof Map) {
                Map hashtable = (Map)object;
                Iterator keyEnumerator = hashtable.keySet().iterator();
                while (keyEnumerator.hasNext()) {
                    Object key = keyEnumerator.next();
                    AWUtil.dispose(hashtable.get(key));
                    AWUtil.dispose(key);
                }
                hashtable.clear();
            }
            else if (object instanceof List) {
                List vector = (List)object;
                for (int index = vector.size() - 1; index > -1; index--) {
                    AWUtil.dispose(vector.get(index));
                }
                vector.clear();
            }
            else if (object instanceof AWArrayManager) {
                AWUtil.dispose(((AWArrayManager)object).array());
            }
            else if (object.getClass().isArray() && !object.getClass().getComponentType().isPrimitive()) {
                Object[] objectArray = (Object[])object;
                for (int index = objectArray.length - 1; index > -1; index--) {
                    AWUtil.dispose(objectArray[index]);
                }
            }
        }
    }

    public static int min (int int1, int int2)
    {
        return int1 < int2 ? int1 : int2;
    }

    public static int max (int int1, int int2)
    {
        return int1 > int2 ? int1 : int2;
    }

    public static String briefStackTrace ()
    {
        String stackTrace = null;
        try {
            throw new RuntimeException();
        }
        catch (RuntimeException runtimeException) {
            stackTrace = SystemUtil.stackTrace(runtimeException);
            int startIndex = stackTrace.indexOf("\n") + 1;
            startIndex = stackTrace.indexOf("\n", startIndex) + 1;
            int endIndex = AWUtil.min(2048, stackTrace.length());
            stackTrace = stackTrace.substring(startIndex, endIndex);
        }
        return stackTrace;
    }

    public static void print (PrintStream out, int[] array, int lastIndex)
    {
        if (lastIndex > 0) {
            out.print(array[0]);
            for (int index = 1; index < lastIndex; index++) {
                out.print(", ");
                out.print(array[index]);
            }
        }
    }

    public static void sleep (long millis)
    {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException interruptedException) {
            throw new AWGenericException(interruptedException);
        }
    }

    public static void notImplemented (String message)
    {
        if (message != null) {
            message = StringUtil.strcat("Not implemented: ", message);
        }
        Assert.that(false, message);
    }

    public static void write (OutputStream outputStream, byte[] bytes, int offset, int length)
    {
        try {
            outputStream.write(bytes, offset, length);
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }

    public static void write (OutputStream outputStream, AWEncodedString encodedString, AWCharacterEncoding characterEncoding)
    {
        byte[] bytes = encodedString.bytes(characterEncoding);
        AWUtil.write(outputStream, bytes, 0, bytes.length);
    }

    /** Iterate over elements of the template.  E.g.
     * {
     *      iterate(template, new Iterator() {
     *          Object process(AWElement e) {
     *              print(e);
     *              return null; // keep iterating
     *          }
     *      });
     */
    public static abstract class ElementIterator
    {
         public abstract Object process (AWElement element);
            // return non-null to stop iteration
    }

    public static Object iterate(AWElement element, ElementIterator iter) {
        Object result = iter.process(element);
        if (result != null) {
            return result;
        }
        if (element instanceof AWTemplate) {
            return iterate(((AWTemplate)element).elementArray(), iter);
        }
        else if (element instanceof AWIf) {
            AWIf.AWIfBlock[] blocks = ((AWIf)element)._conditionBlocks();
            for (int i=0, c=blocks.length; i<c; i++) {
                Object ret = iterate(blocks[i].contentElement(), iter);
                if (ret != null) return ret;
            }
        }
        else if (element instanceof AWContainerElement) {
            return iterate(((AWContainerElement)element).contentElement(), iter);
        }
        return null;
    }

    private static Object iterate(AWElement[] elements, ElementIterator iter) {
        int elementCount = elements.length;
        for (int index = 0; index < elementCount; index++) {
            Object ret = iterate(elements[index], iter);
            if (ret != null) return ret;
        }
        return null;
    }

    public static AWElement elementOfClass (AWTemplate template, Class target)
    {
        AWElement[] elements = template.elementArray();
        int elementCount = elements.length;
        for (int index = 0; index < elementCount; index++) {
            AWElement element = elements[index];
            if (classInheritsFromClass(element.getClass(), target)) {
                return element;
            }
        }
        return null;
    }

    public static Properties loadProperties (URL url)
    {
        if (url == null) return null;
        Properties properties = new Properties();
        try {
            properties.load(url.openStream());
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
        return properties;
    }

    public static URL getResource (String name)
    {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    public static Properties loadProperties (String resourceName)
    {
        return loadProperties(getResource(resourceName));
    }

    /**
     * Utility method to convert HTML input to PDF
     * @param htmlInputStream - HTML input stream
     * @param outputStream - PDF output stream
     */

    public static void convertHTMLToPDF (InputStream htmlInputStream,
                                         OutputStream outputStream,
                                         String fontFileLocation)
    {
        try
        {
            Tidy tidy = new Tidy();
            tidy.setCharEncoding(Configuration.UTF8);
            tidy.setXHTML(true);
            tidy.setShowWarnings(false);

            File dir = SystemUtil.getLocalTempDirectory();
            File tempFile = File.createTempFile("pdf", "pdf", dir);
            FileOutputStream temp = new FileOutputStream(tempFile);

            tidy.parse(htmlInputStream, temp);
            temp.close();

            ITextRenderer renderer = new ITextRenderer();
            if (!StringUtil.nullOrEmptyOrBlankString(fontFileLocation))
            {
			    renderer.getFontResolver().addFont(fontFileLocation, BaseFont.IDENTITY_H , BaseFont.EMBEDDED);
            }
            renderer.setDocument(tempFile);
            renderer.layout();
            renderer.createPDF(outputStream);
            if(tempFile.exists()) {
                tempFile.delete();
            }
        }
        catch (IOException exception) {
            throw new AWGenericException(Fmt.S("Exception when using temp file : %s", SystemUtil.stackTrace(exception)));
        }
        catch (DocumentException e) {
            throw new AWGenericException(Fmt.S("Failed to generate pdf content : %s", SystemUtil.stackTrace(e)));
        }
    }

}

/////////////////////////////
// AWExtensionFilenameFilter
/////////////////////////////
final class AWExtensionFilenameFilter extends AWBaseObject implements FilenameFilter
{
    private final String _fileExtension;

    public AWExtensionFilenameFilter (String fileExtension)
    {
        _fileExtension = (fileExtension.startsWith(".")) ? fileExtension : StringUtil.strcat(".", fileExtension);
    }

    public boolean accept (File dir, String name)
    {
        return name.endsWith(_fileExtension);
    }


}

////////////////////
// AWCsvConsumer
////////////////////
final class AWCsvConsumer extends AWBaseObject implements CSVConsumer
{
    private List _lines = ListUtil.list();

    public void consumeLineOfTokens (String filepath, int lineNumber, List line)
    {
        for (int index = line.size() - 1; index >= 0; index--) {
            String currentString = (String)line.get(index);
            String unescapedString = AWUtil.unescapeCsvString(currentString);
            line.set(index, unescapedString);
        }
        _lines.add(line);
    }

    public List lines ()
    {
        return _lines;
    }
}
