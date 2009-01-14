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

    $Id: $
*/

package ariba.util.io;

import ariba.util.core.Assert;
import ariba.util.core.IOUtil;
import ariba.util.core.StringUtil;
import ariba.util.i18n.I18NUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
     This class handles convertions between file byte streams and Unicode character
     streams, based of a file type.
     The encoding to be used is determined as follows.

    [Read]

     - The encoding passed as an argument, if specified and supported;
     - The encoding field embedded in the file, if exists and supported;
     - The encoding set by setSystemDefaultEncoding method, which is done
       by FileServer typically;
     - The default encoding of VM

    [Write]

     - The encoding passed as an argument, if specified and supported;
     - The encoding set by setSystemDefaultEncoding method, which is done
       by FileServer typically;
     - The default encoding of VM

   @aribaapi private
*/
public class FileHandler
{
    public static final String CHARSET_8859   = I18NUtil.EncodingISO8859_1;
    public static final String CHARSET_CP1252 = "Cp1252";
    public static final String CHARSET_JIS    = "ISO2022JP";
    public static final String CHARSET_SJIS   = "SJIS";
    public static final String CHARSET_EUC_JP = "EUC_JP";

    private static final String CSVFileExtension     = ".csv";
    private static final String CIFFileExtension     = ".cif";
    private static final String TABLEleFileExtension = ".table";
    private static final String TEXTFileExtension    = ".txt";

    private static final String CIF_DEFAULT_ENCODING = CHARSET_8859;
    private static final String CIF_CHARSET     = "CHARSET";
    private static final String CIF_BEGIN_DATA  = "DATA";

    private static final char Colon  = ':';
    private static final char Period = '.';

    public static final String NO_ENCODING = "";

    private static String SystemDefaultEncoding = NO_ENCODING;

    /**
        This class is not allowed to be instantiated.
        Just a holder of static method calls
    */
    private FileHandler ()
    {
    }

    /**
        Sets system default encoding
        Basically this is used by FileServer

        @param encoding  system default encoding
    */
    public static void setSystemDefaultEncoding (String encoding)
    {
        SystemDefaultEncoding = encoding;
    }

    /**
        Returns system default encoding
    */
    public static String getSystemDefaultEncoding ()
    {
        return SystemDefaultEncoding;
    }

    /**
        Checks if a specified encoding is supported by VM
        Typically this is used before saving a file,
        or for checking whether the first line of a CSV file
        is encoding information or not.
        It returns false when the encoding is null.

        @param encoding    an encoding to be checked
    */
    public static boolean isEncodingSupported (String encoding)
    {
        if (StringUtil.nullOrEmptyOrBlankString(encoding)) {
            return false;
        }

        try {
            "".getBytes(encoding);

            return true;
        }
        catch (UnsupportedEncodingException uee) {
            return false;
        }
        catch (RuntimeException re) {
            return false;
        }
    }

    /**
        Returns a Reader associated with a file

        @param file        a file to be read
        @param encoding    an encoding that will be used
                           If this is invalid, the encoding in the file
                           is used if available.
                           Otherwise the default is used,
                           depending on the file type
    */
    public static Reader getFile (File file, String encoding)
      throws IOException
    {
        BufferedInputStream bis = IOUtil.bufferedInputStream(file);

        if (!isEncodingSupported(encoding)) {
            encoding = getFileEncoding(file, bis);
        }

        return getReader(bis, encoding);
    }

    private static Reader getReader (InputStream input, String encoding)
    {
        Reader isr = null;

        if (encoding == null || NO_ENCODING.equals(encoding)) {
            encoding = SystemDefaultEncoding;
        }

        try {
            isr = IOUtil.bufferedReader(input, encoding);
        }
        catch (UnsupportedEncodingException e) {
            try {
                isr = IOUtil.bufferedReader(input,
                                            IOUtil.getDefaultSystemEncoding());
            }
            catch (UnsupportedEncodingException ex) {
                Assert.that(false, "Unable to use default system encoding %s",
                            ex);
            }

            setSystemDefaultEncoding(IOUtil.getDefaultSystemEncoding());
        }

        return isr;
    }

    /**
        Returns a Writer associated with a file

        @param file        a file to be written
        @param encoding    an encoding that will be used
                           If this is invalid, 
                           the default is used,
                           depending on the file type
    */
    public static Writer putFile (File file, String encoding)
      throws IOException
    {
        return getWriter(new FileOutputStream(file), encoding);
    }

    private static Writer getWriter (OutputStream output, String encoding)
    {
        Writer osw = null;

        if (encoding == null || NO_ENCODING.equals(encoding)) {
            encoding = SystemDefaultEncoding;
        }

        try {
            osw = IOUtil.bufferedWriter(output, encoding);
        }
        catch (UnsupportedEncodingException e) {
            try {
                osw = IOUtil.bufferedWriter(output, IOUtil.getDefaultSystemEncoding());
            }
            catch (UnsupportedEncodingException ex) {
                Assert.that(false, "Unable to use default system encoding: %s",
                            ex);
            }

            setSystemDefaultEncoding(IOUtil.getDefaultSystemEncoding());
        }

        return osw;
    }

    /**
         Returns an extension of the file

         @param file    a file to be checked
    */
    public static String getFileExtension (File file)
    {
        String fileName = file.getPath();
        int periodIndex = fileName.lastIndexOf(Period);
        if (periodIndex == -1) {
            return null;
        }

        return fileName.substring(periodIndex).toLowerCase();
    }

    /**
         Returns the first column in a comma separeted line that could be
         an encoding of the CSV file

         @param firstLine    the first line of a CSV file
    */
    public static String getCSVFileEncodingCandidate (String firstLine)
    {
        String candidate = null;

        StringTokenizer st =
            new StringTokenizer(firstLine, " \t\",");

        try {
            candidate = st.nextToken();
        }
        catch (NoSuchElementException e) {
        }
        return candidate;
    }

    /**
         Returns an encoding that will be used to read the specified file

         @param fileName    a file name to be read
         @param input       a BufferedInputStream associated with the file
    */
    private static String getFileEncoding (File file, BufferedInputStream input)
      throws IOException
    {
        String extension = getFileExtension(file);

        if (extension == null) {
            return SystemDefaultEncoding;
        }

        String encoding = SystemDefaultEncoding;

        input.mark(Integer.MAX_VALUE);

        if (CSVFileExtension.equals(extension)) {
            encoding = getCSVFileEncoding(input);
        }
        else if (CIFFileExtension.equals(extension)) {
            encoding = getCIFFileEncoding(input);
        }

        input.reset();

        return encoding;
    }

    private static String getCSVFileEncoding (BufferedInputStream input)
      throws IOException
    {
        String encoding =
            getCSVFileEncodingCandidate(IOUtil.readLine(input).trim());

        if (!isEncodingSupported(encoding)) {
            encoding = SystemDefaultEncoding;
        }

        return encoding;
    }
    
    private static String getCIFFileEncoding (BufferedInputStream input)
      throws IOException
    {
        String encoding = CIF_DEFAULT_ENCODING;

        String lineBuffer = null;
        while ((lineBuffer = IOUtil.readLine(input).trim()) != null) {
            if (lineBuffer.indexOf(CIF_BEGIN_DATA) != -1) {
                break;
            }
            else if (lineBuffer.indexOf(CIF_CHARSET) != -1) {
                int colonIndex = lineBuffer.lastIndexOf(Colon);
                if (colonIndex != -1) {
                    encoding = lineBuffer.substring(colonIndex + 1).trim();
                }
                break;
            }
        }

            // For backward compatibility,
            // we treat "Cp1252" in case-insensitive.
        if (CHARSET_CP1252.equalsIgnoreCase(encoding)) {
            encoding = CHARSET_CP1252;
        }

        if (!isEncodingSupported(encoding)) {
            encoding = CIF_DEFAULT_ENCODING;
        }

        return encoding;
    }
}
