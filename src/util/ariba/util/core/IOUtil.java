/*
    Copyright (c) 1996-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/IOUtil.java#30 $

    Responsible: platform

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package ariba.util.core;

import ariba.util.i18n.I18NUtil;
import ariba.util.log.Log;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.UnsupportedEncodingException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.Closeable;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
    Input/Output Utilities. These are helper functions for dealing with
    i/o.

    @aribaapi documented
*/
public final class IOUtil
{
    //--------------------------------------------------------------------------
    // constructors

    /* prevent people from creating instances of this class */
    private IOUtil ()
    {}

    //--------------------------------------------------------------------------
    // constants

    /**
        When internal buffers are used in buffered I/O, the size is
        specified by this value.
        @aribaapi ariba
    */
    public static final int DefaultInternalBufferSize = 2 * 1024;

    //-------------------------------------------------------------------------
    // private static data members

    /**
        Holds a byte buffer for use in this class while copying bytes around.

        @aribaapi private
    */
    public static final class BufferHolder extends ThreadLocalHolder<byte[]>
    {
        public byte[] make ()
        {
            return makeByteBuffer();
        }
    }

    /**
        Is a ThreadLocal of BufferHolders.

        The idea of this
        @aribaapi private
    */
    private static final ThreadLocal<ThreadLocalHolder<byte[]>> _bufferHolders =
        new ThreadLocal<ThreadLocalHolder<byte[]>> ()
        {
            protected ThreadLocalHolder<byte[]> initialValue ()
            {
                return new BufferHolder();
            }
        };

    //--------------------------------------------------------------------------
    // private static methods

    /**
        Returns the per-thread BufferHolder.
        @aribaapi private
    */
    private static ThreadLocalHolder<byte[]> getBufferHolder ()
    {
        return _bufferHolders.get();
    }

    /**
        Checks out and returns a byte buffer of size
        {@link #DefaultInternalBufferSize} out of the per-thread BufferHolderl.

        This byte buffer must be returned via {@link #returnBuffer} to
        this object. To ensure this happens, please use a try/finally
        pattern. <p/>

        @aribaapi private
    */
    private static byte[] checkoutBuffer ()
    {
        return getBufferHolder().checkoutBuffer();
    }

    /**
        Returns the byte buffer to the per-thread BufferHolder.
        This must be the byte buffer that was obtained by calling
        {@link #checkoutBuffer}.  <p/>

        @aribaapi private
    */
    private static void returnBuffer (byte[] buffer)
    {
        getBufferHolder().returnBuffer(buffer);
    }


    /*-- Stream Utilities ---------------------------------------------------*/
    /*
        text   output = printWriter
        text    input = bufferedReader
        binary output = bufferedOutputStream
        binary  input = bufferedInputStream
    */

    /**
        Returns the recursion depth of the internal per-thread
        BufferHolder.
        @return the per-thread BufferHolder recursion depth
        @aribaapi ariba
    */
    public static int getInternalBufferHolderRecursionDepth ()
    {
        return getBufferHolder().getRecursionDepth();
    }

    /**
        Returns a new byte array buffer of size
        {@link #DefaultInternalBufferSize}. This method is used by this
        class for internal buffer allocation. <p/>

        @aribaapi ariba
    */
    public static byte[] makeByteBuffer ()
    {
        return new byte[DefaultInternalBufferSize];
    }

    /**
        Opens a text file with a reasonable stream interface on top of a
        buffered layer.

        @param file the file to open
        @param encoding the encoding to use

        @return a buffered PrintWriter wrapped around the file

        @exception IOException if there was an error opening the file
        @aribaapi documented
    */
    public static PrintWriter printWriter (File file, String encoding)
      throws IOException
    {
        return printWriter(new FileOutputStream(file), encoding);
    }


    /**
        Open a text stream over a binary one on top of a buffered
        layer.

        @aribaapi ariba
    */
    public static PrintWriter printWriter (OutputStream o, String encoding)
      throws UnsupportedEncodingException
    {
        return new PrintWriter(bufferedWriter(o, encoding));
    }

    /**
        Open a text stream over a binary one on top of a buffered
        layer.

        @aribaapi ariba
    */
    public static PrintWriter printWriter (OutputStream o,
                                           String encoding,
                                           boolean autoFlush)
      throws UnsupportedEncodingException
    {
        return new PrintWriter(bufferedWriter(o, encoding), autoFlush);
    }

    /**
        Open a writer with the specified encoding

        @aribaapi ariba
    */
    public static BufferedWriter bufferedWriter (OutputStream o, String encoding)
      throws UnsupportedEncodingException
    {
        return new BufferedWriter(
            new OutputStreamWriter(o, encoding)); // OK
    }

    /**
        Opens a text file on top of a buffered layer.

        @param file the File to opened
        @param encoding the encoding to use

        @return a BufferedReader around the file

        @exception IOException if there was an error opening the file
        @aribaapi documented
    */
    public static BufferedReader bufferedReader (File file, String encoding)
      throws IOException
    {
        BufferedReader result;
        FileInputStream fis = new FileInputStream(file.getPath());
        try {
            result = bufferedReader(fis, encoding);
        }
        catch (UnsupportedEncodingException uee) {
            try {
                fis.close();
            }
            catch (IOException ioe) {
            }
            throw uee;
        }
        return result;
    }

    /**
        Opens a text stream over a binary one on top of a buffered
        layer.

        @aribaapi private
    */
    public static BufferedReader bufferedReader (InputStream i,
                                                 String      encoding)
      throws UnsupportedEncodingException
    {
        // Some OS such as early version of IBM AIX could hang the thread if
        // a bad encoding string is passed on to java.io.InputStreamReader
        // Check ahead to make sure we don't pass a bad encoding 
        if (isInvalidEncodingString(encoding)) {
            String errorMsg = Fmt.S(
                    "UnsupportedEncodingException thrown: encoding (%s) is invalid.",
                    encoding);
            Log.util.debug(errorMsg);
            throw new UnsupportedEncodingException(errorMsg);
        }
        return new BufferedReader(new InputStreamReader(i, encoding)); // OK
    }

    private static boolean isInvalidEncodingString (String encoding)
    {
        if (StringUtil.nullOrEmptyOrBlankString(encoding)) {
            return true;
        }

        // check if it is a supported charset name in the current JVM 
        try {
            if (Charset.isSupported(encoding)) {
                return false;
            }
        }
        catch (IllegalCharsetNameException ex) {
        }

        return true;
    }
    
    
    /**
        Opens a binary file on top of a buffered layer.

        @param file the file to open

        @return a BufferedInputStream wrapping the file

        @exception IOException if there was an error opening the file
        @aribaapi documented
    */
    public static BufferedInputStream bufferedInputStream (File file)
      throws IOException
    {
        return new BufferedInputStream(new FileInputStream(file));
    }

    /**
        Opens a binary file on top of a buffered layer.

        @param file the file to open

        @return a BufferedOutputStream wrapping the file

        @exception IOException if there was an error opening the file
        @aribaapi documented
    */
    public static BufferedOutputStream bufferedOutputStream (File file)
      throws IOException
    {
        return new BufferedOutputStream(new FileOutputStream(file));
    }


    /**
        Opens a binary file on top of a buffered layer.

        @param file the file to open
        @param append true to append to the file, false otherwise.
        @return a BufferedOutputStream wrapping the file

        @exception IOException if there was an error opening the file
        @aribaapi documented
    */
    public static BufferedOutputStream bufferedOutputStream (
        File file, boolean append)
      throws IOException
    {
        return new BufferedOutputStream(new FileOutputStream(file, append));
    }


    /**
        Read a line from an InputStream.

        @param in the stream to read from

        @return the line read, without the newline, or <b>null</b> if
        there is no more data

        @exception IOException if there was an error in reading from
        the stream
        @aribaapi documented
    */
    public static String readLine (InputStream in) throws IOException
    {
        return readLine(in, new char[128]);
    }

    /**
        Read a line from an InputStream.

        @param in the stream to read from
        @param lineBuffer a buffer to use for reading the
        lines. Performance is better if the buffer is big enough for
        the longest line.

        @return the line read, without the newline, or <b>null</b> if
        there is no more data

        @exception IOException if there was an error in reading from
        the stream
        @aribaapi documented
    */
    public static String readLine (InputStream in,
                                   char[]      lineBuffer)
      throws IOException
    {
        char buf[] = lineBuffer;

        int room = buf.length;
        int offset = 0;
        int c;

    loop:
        while (true) {
            switch (c = in.read()) {
              case -1:
              case '\n': {
                  break loop;
              }

              case '\r': {
                  int c2 = in.read();
                  if (c2 != '\n') {
                      throw new IOException(
                          Fmt.S("Expected newline after carriage return but got '%s'",
                                c2));
                  }
                  break loop;
              }

              default: {
                  if (--room < 0) {
                      buf = new char[offset + 128];
                      room = buf.length - offset - 1;
                      System.arraycopy(lineBuffer, 0, buf, 0, offset);
                      lineBuffer = buf;
                  }
                  buf[offset++] = (char)c;
                  break;
              }
            }
        }
        if ((c == -1) && (offset == 0)) {
            return null;
        }
        return String.copyValueOf(buf, 0, offset);
    }

    /**
        Copy a file if the source is newer than the target

        @param sourceFile the source of the copy
        @param targetFile the target of the copy

        @return <b>false</b> if there was a problem.
        @aribaapi documented
    */
    public static boolean copyFileIfNewer (File sourceFile, File targetFile)
    {
        long sourceModified = sourceFile.lastModified();
        long targetModified = targetFile.lastModified();
        if (!targetFile.exists() || sourceModified > targetModified) {
            return copyFile(sourceFile, targetFile);
        }
        return true;
    }

    /**
        Copy the contents of a file to another file.

        @param sourceFile the source
        @param targetFile the target

        @return <b>false</b> if there was a problem. <b>true</b> otherwise.
        @aribaapi documented
    */
    public static boolean copyFile (File sourceFile, File targetFile)
    {
        byte[] buffer = checkoutBuffer();
        try {
            return copyFile(sourceFile, targetFile, buffer);
        }
        finally {
            returnBuffer(buffer);
        }
    }
    /**
        Copy the contents of a file to another file.

        @param sourceFile the source of the copy
        @param targetFile the target of the copy
        @param buffer a buffer to use for the copy

        @return <b>false</b> if there was a problem. <b>true</b> otherwise.
        @deprecated use copyFile(File, File, byte[])
        @aribaapi documented
    */
    public static boolean copyFile (File sourceFile, File targetFile,
                                    char [] buffer)
    {
        return copyFile(sourceFile, targetFile);
    }

    /**
        Copy the contents of a file to another file.

        @param sourceFile the source of the copy
        @param targetFile the target of the copy
        @param buffer a buffer to use for the copy

        @return <b>false</b> if there was a problem. <b>true</b> otherwise.
        @aribaapi documented
    */
    public static boolean copyFile (File sourceFile, File targetFile,
                                    byte [] buffer)
    {
        boolean success = false;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(targetFile);
            success = inputStreamToOutputStream(inputStream, outputStream, buffer, false);
        }
        catch (IOException ioe) {
            success = false;
            Log.util.error(2770, sourceFile, targetFile, ioe);
        }
            // the following code try's very hard (forgive the pun) to
            // close both files
        finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
            catch (IOException ioe) {
                Log.util.error(2771, targetFile, ioe);
                success = false;
            }
            finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                catch (IOException ioe) {
                    Log.util.error(2771, sourceFile, ioe);
                    success = false;
                }
            }
        }
        return success;
    }

    /**
        Copy Reader to Writer.

        @param reader the source of the copy
        @param writer the destination of the copy, null if none

        @return <b>false</b> if there was a problem.
        @aribaapi documented
    */
    public static boolean readerToWriter (Reader reader,
                                          Writer writer)
    {
        return readerToWriter(reader,
                              writer,
                              new char[2048]);
    }

    /**
        Copy Reader to Writer.

        @param reader the source of the copy
        @param writer the destination of the copy, null if none
        @param buffer a buffer to use for the copy

        @return <b>false</b> if there was a problem.
        @aribaapi documented
    */
    public static boolean readerToWriter (Reader reader,
                                          Writer writer,
                                          char[] buffer)
    {
        Assert.that(buffer != null, "Char [] buffer must not be null.");
        try {
            int bufferSize = buffer.length;
            int charsRead = reader.read(buffer, 0, bufferSize);

            while (charsRead > 0) {
                if (writer != null) {
                    writer.write(buffer, 0, charsRead);
                }
                charsRead = reader.read(buffer, 0, bufferSize);
            }
        }
        catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
        Copies up to <code>length</code> bytes from <code>input</code> to
        <code>output</code> using the supplied <code>buffer</code>. <p>

        The copying stops if <code>input</code> is exhausted before
        <code>length</code> bytes are copied. The actual number of bytes copied is
        returned. <p>

        <b>Notes:</b><ul>
        <li> This method does <b>not</b> close either <code>input</code> or
             <code>output</code>.
        <li> Neither <code>input</code> or <code>output</code> has to be
             buffered--this method provides a buffer
        </ul>

        @param input the stream to read from
        @param output the stream to write to
        @param buffer the byte buffer to use
        @param flushOutput if <code>true</code> flush <code>output</code> on each write
        @param length the maximum number of bytes to copy
        @return the number of bytes copied
        @aribaapi documented
    */
    public static long spool (
            InputStream input,
            OutputStream output,
            byte[] buffer,
            boolean flushOutput,
            long length
    )
    throws IOException
    {
        long result = 0;
        int readLength = (int)Math.min(buffer.length, length);
        int read = 0;
        while (readLength > 0 && (read=input.read(buffer, 0, readLength)) != -1)
        {
            // there is code which can call spool with null output param
            // see: inputStreamToOutputStream ()
            if (output != null) {
                output.write(buffer, 0, read);
                if (flushOutput) {
                    output.flush();
                }
            }
            result += read;
            readLength = (int)Math.min(buffer.length, length - result);
        }
        return result;
    }

    /**
        Copies up to <code>length</code> bytes from <code>input</code> to
        <code>output</code> using the supplied <code>buffer</code>. <p>

        The copying stops if <code>input</code> is exhausted before
        <code>length</code> bytes are copied. The actual number of bytes copied is
        returned. <p>

        <b>Notes:</b><ul>
        <li> This method does <b>not</b> close either <code>input</code> or
          <code>output</code>.
        <li> Neither <code>input</code> or <code>output</code> has to be
          buffered--this method provides a buffer
        </ul>

        @param input the stream to read from
        @param output the stream to write to
        @param buffer the byte buffer to use
        @param length the maximum number of bytes to copy
        @return the number of bytes copied
        @aribaapi documented
    */
    public static long spool (
            InputStream input,
            OutputStream output,
            byte[] buffer,
            long length
    )
    throws IOException
    {
        return spool(input, output, buffer, false, length);
    }

    /**
        Copies up to <code>length</code> bytes from <code>input</code> to
        <code>output</code>. <p>

        The copying stops if <code>input</code> is exhausted before
        <code>length</code> bytes are copied. The actual number of bytes copied is
        returned. <p>

        <b>Notes:</b><ul>
        <li> This method does <b>not</b> close either <code>input</code> or
        <code>output</code>.
        <li> Neither <code>input</code> or <code>output</code> has to be
        buffered--this method provides a buffer
        </ul>

        @param input the stream to read from
        @param output the stream to write to
        @param length the maximum number of bytes to copy
        @return the number of bytes copied
        @aribaapi documented
    */
    public static long spool (
            InputStream input,
            OutputStream output,
            long length
    )
    throws IOException
    {
        byte[] buffer = checkoutBuffer();
        try {
            return spool(input, output, buffer, length);
        }
        finally {
            returnBuffer(buffer);
        }
    }

    /**
        Copies all the bytes from <code>input</code> to <code>output</code>. <p>

        The copying stops when <code>input</code> is exhausted.
        The actual number of bytes copied is returned. <p>

        @param input the stream to read from
        @param output the stream to write to
        @return the number of bytes copied
        @aribaapi documented
    */
    public static long spool (InputStream input, OutputStream output)
    throws IOException
    {
        return spool(input, output, Long.MAX_VALUE);
    }

    /**
        Copies up to <code>buffer.length</code> bytes from <code>input</code> to
        <code>buffer</code>. <p>

        The copying stops if <code>input</code> is exhausted before <code>buffer</code>
        is filled. The actual number of bytes copied is returned. <p>

        @param input the stream to read from
        @param buffer the byte buffer to write to
        @return the number of bytes copied
        @aribaapi ariba
    */
    public static int spool (InputStream input, byte[] buffer)
    throws IOException
    {
        int result = 0;
        int readLength = buffer.length;
        int read = 0;
        while (readLength > 0 && (read=input.read(buffer, result, readLength)) != -1)
        {
            result += read;
            readLength = buffer.length - result;
        }
        return result;
    }

    /**
        Copies all data from an InputStream to an OutputStream. <p/>

        If you wish to use a method which throws an <code>IOException</code>
        see {@link #spool(InputStream,OutputStream)}. <p/>

        @param input the source of the copy
        @param output the destination of the copy, null if none

        @return <b>false</b> if there was a problem.
        @aribaapi documented
    */
    public static boolean inputStreamToOutputStream (
        InputStream input,
        OutputStream output
    )
    {
        try {
            spool(input, output, Long.MAX_VALUE);
        }
        catch (IOException ex) {
            return false;
        }
        return true;
    }

    /**
        Copies all data from an InputStream to an OutputStream. <p/>

        If you wish to use a method which throws an <code>IOException</code>
        see the <code>spool</code> method taking the same arguments. <p/>

        @param input the source of the copy
        @param output the destination of the copy, null if none
        @param buffer a buffer to use for copying
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call

        @return <b>false</b> if there was a problem.
        @aribaapi documented
    */
    public static boolean inputStreamToOutputStream (
        InputStream input, OutputStream output, byte[] buffer, boolean flush)
    {
        try {
            spool(input, output, buffer, flush, Long.MAX_VALUE);
        }
        catch (IOException ioe) {
            return false;
        }
        return true;
    }


    /**
        Copies the specified amount of data from an InputStream to an
        OutputStream.

        @param input the source of the copy
        @param output the destination of the copy, null if none
        @param buffer a buffer to use for copying
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call
        @param bytesToCopy the number of bytes to copy

        @return the number of bytes copied
        @aribaapi documented
        @deprecated Use {@link #spool} instead.
    */
    public static int inputStreamToOutputStream (
        InputStream input,
        OutputStream output,
        byte[] buffer,
        boolean flush,
        int bytesToCopy
    )
    throws IOException
    {
        if (output == null) {
            flush = false;
        }
        // safe to cast to int because we never copy more
        // bytes than bytesToCopy
        return (int)spool(input, output, buffer, flush, bytesToCopy);
    }

    /**
        Read the contents of a URL as a byte array
.

        @param url the URL to read
        @return the contents of the URL, or null if there
        are any problems
        @aribaapi documented
    */
    public static byte[] bytesFromURL (URL url)
    {
        try {
            InputStream urlStream = url.openStream();
            int available = urlStream.available();
            byte[] buffer = new byte[available];
            urlStream.read(buffer);
            urlStream.close();

            return buffer;
        }
        catch(IOException ioe) {
            return null;
        }
    }

    /**
     Execute the Url String.
     Going forward this method name makes sense for executing Url

     The reason for not using IOUtil.stringFromURL() api is that
     it uses the available() method on InputStream which is not
     advisable to determine the content length.

     This method will return the content existing at the given URL
     in the String form using the encoding passed. If encoding is
     passed as null, then it will use the UTF8 encoding.
     @param url the URL to read
     @param encoding encoding of the content present at given url
     @return the contents of the URL, or null if there
     are any problems
     @aribaapi documented
     */
    public static String executeURL (String url, String encoding)
    {
        BufferedReader reader = null;
        InputStream inputStream = null;
        try
        {
            URL httpUrl = URLUtil.makeURL(url);
            URLConnection urlConnection = httpUrl.openConnection();
            inputStream = urlConnection.getInputStream();

            if (encoding == null) {
                encoding = I18NUtil.EncodingUTF8;
            }
            reader = IOUtil.bufferedReader(inputStream, encoding);
            String line;
            FastStringBuffer response = new FastStringBuffer();
            while ((line = reader.readLine()) != null)
            {
                response.append(line);
                response.append(System.getProperty("line.separator"));
            }
            return response.toString();
        }
        catch (IOException ioe)
        {
            Log.util.warning(11773, url, SystemUtil.stackTrace(ioe));
            return null;
        }
        finally
        {
            close(reader);
            close(inputStream);
        }
    }

    /**
     * Returns the content of URL in String form using UTF8 encoding.
     * @param url the URL to read
     * @return  the contents of the URL, or null if there
     * are any problems
     */
    public static String executeURL (String url)
    {
        return executeURL(url, null);
    }

    /**
        Read the contents of a URL as a string.

        @param url the URL to read
        @param encoding the encoding to use (as in a String constructor).
        If encoding is null, we use the default encoding
        @return the contents of the URL, or null if there
        are any problems
        @aribaapi documented
    */
    public static String stringFromURL (URL url, String encoding)
    {
        byte[] buffer = bytesFromURL(url);
        if (buffer != null) {
            if (encoding == null) {
                return new String(buffer);
            }
            else {
                try {
                    return new String(buffer, encoding);
                }
                catch (UnsupportedEncodingException uee) {
                    return null;
                }
            }
        }

        return null;
    }
  static final int BufferSize = 4096;
    /**
        Copy a URL (String format) to File.

        @param sourceURLString the URL string reference source content
        @param targetFile the destination for the content
        @exception FileNotFoundException
        @exception MalformedURLException
        @exception IOException
        @aribaapi documented
    */
    public static void copyUrlToFile (String sourceURLString, File targetFile)
      throws FileNotFoundException, MalformedURLException, IOException
    {
        InputStream in = null;
        OutputStream out = null;

        try {
            URL url = URLUtil.makeURL(sourceURLString);
            in = url.openStream();
            out = new FileOutputStream(targetFile);
            spool(in, out);
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            }
            finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
        Copy recursively a directory
        Use this with caution. If an error occurs during the recursive copy, partial
        copying or deletion may result!

        @param source The source
        @param target The target

        @return <b>false</b> if there was a problem.
        @exception NullPointerException if one of the arguments is null
        @aribaapi documented
    */
    public static boolean copyDirectory (File source, File target)
    {
        return copyDirectory(source, target, true, true);
    }

    /**
        Copy recursively a directory<br>
        Use this with caution. If an error occurs during the recursive copy, partial
        copying or deletion may result!

        @param source The source. It has to be a directory that exists.
        @param target The target
        @param recursive if <i>true</i>, will also copy the subdirectories
        @param createTarget if <i>true</i>, will create the target and its
                            subdirectories if needed

        @return <b>false</b> if there was a problem.
        @exception NullPointerException if one the source or the target is null
        @aribaapi documented
    */
    public static boolean copyDirectory (File source,
                                         File target,
                                         boolean recursive,
                                         boolean createTarget)
    {
        Assert.that(source.exists(), "%s doesn't exist", source);
        Assert.that(source.isDirectory(), "%s is not a directory", source);

        /*
            Sanity check
        */
        if (target.exists() && !target.isDirectory()) {
            Log.util.debug("target %s already exists and is not a directory",
                           target);
            return false;
        }
        if (!target.exists() && !createTarget) {
            Log.util.debug("target %s doesn't exist", target);
            return false;
        }
        else if (!target.exists()) {
            if (!target.mkdirs()) {
                Log.util.debug("Problem when creating %s", target);
                return false;
            }
        }

        /*
            Check the rights on the source and the target
        */
        if (!source.canRead()) {
            Log.util.debug("No permission for reading %s", source);
            return false;
        }
        if (!target.canWrite()) {
            Log.util.debug("No permission for writing to %s", target);
            return false;
        }

        String[] elements = source.list();
        for (int f = 0, length = elements.length; f < length; f++) {
            File sourceElement = new File(source, elements[f]);
            File targetElement = new File(target, elements[f]);
            if (sourceElement.isDirectory()) {
                if (recursive && !copyDirectory(sourceElement,
                                                targetElement,
                                                recursive,
                                                createTarget))
                {
                    return false;
                }

            }
            else {
                if (!copyFile(sourceElement, targetElement)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
        Delete a directory and its subdirectories.
        Use this with caution. If an error occurs during the recursive delete, partial
        copying or deletion may result!

        @param dir Directory to delete. It has to be a directory that exists.
        @return  <b>false</b> if there was a problem.
        @exception NullPointerException if the argument is null
        @aribaapi documented
    */
    public static boolean deleteDirectory (File dir)
    {
        Assert.that(dir.isDirectory(), "%s doesn't exist or is not a directory", dir);
        File[] elements = dir.listFiles();
        for (int f = 0; f < elements.length; f++) {
            File element = elements[f];
            if (element.isDirectory()) {
                if (!deleteDirectory(element)) {
                    Log.util.debug("Problem when deleting %s", element);
                    return false;
                }
            }
            else if (!element.delete()) {
                Log.util.debug("Problem when deleting %s", element);
                return false;
            }
        }
        return dir.delete();
    }

    /**
        Return the System Wide default encoding. This is not the
        method you are looking for, move along.

        @aribaapi private
    */
    public static String getDefaultSystemEncoding ()
    {
        return System.getProperty("file.encoding");
    }

    /**
     * Helper method to close an Object which implements
     * Closeable interface without exceptions in
     * a null safe manner.
     * @param obj   Object needs to be closed
     */
    public static void close (Closeable obj)
    {
        if (obj != null) {
            try {
                obj.close();
            }
            catch (IOException ioe) {
                Log.util.warning(11772, obj, SystemUtil.stackTrace(ioe));
            }
        }
    }

    /**
        Greps the supplied <code>reader</code> character stream for <code>regex</code>
        returning <code>true</code> the pattern is found in the character stream
        and <code>false</code> otherwise. <p/>

        Notes: <ul>
        <li> <code>reader</code> is read in a line-by-line manner (there is
             no support currently for "across-line" matches)
        <li> <code>reader</code> is advanced to the end of the first line that
             contains <code>regex</code>
        </ul>

        @param reader the character stream
        @param regex the regular expression
        @aribaapi ariba
    */
    public static boolean grep (Reader reader, String regex)
    throws IOException
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher("");
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        String line;
        while ((line = lineNumberReader.readLine()) != null) {
            matcher.reset(line);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    /**
        Returns <code>true</code> if the <code>file</code> contains
        a string matching <code>regex</code> and <code>false</code>
        otherwise. <p/>

        @param file the file
        @param regex the regular expression
        @param charset the character set used by <code>file</code>
        @aribaapi ariba
    */
    public static boolean grep (File file, String regex, Charset charset)
    throws IOException
    {
        Reader reader = null;
        FileInputStream fis = new FileInputStream(file);
        try {
            reader = IOUtil.bufferedReader(fis, charset.name());
            return grep(reader, regex);
        }
        finally {
            close(fis);
            close(reader);
        }
    }

    /**
        Returns <code>true</code> if the <code>file</code> contains
        a string matching <code>regex</code> and <code>false</code>
        otherwise. <p/>

        @param file the file (UTF-8 encoding assumed)
        @param regex the regular expression
        @aribaapi ariba
    */
    public static boolean grep (File file, String regex)
    throws IOException
    {
        Charset charset = Charset.forName(I18NUtil.EncodingUTF_8);
        return grep(file, regex, charset);
    }

    /**
        Searches the <code>reader</code> character stream and replaces every
        occurrence of <code>regexToFind</code> with the replacement string
        <code>toReplace</code>. Captured groups in <code>regexToFind</code>
        can be referenced in <code>toReplace</code> using the standard
        mechanism (e.g. <code>$1</code> references the first group etc.)
        <p/>
        No support currently for across-line regexes. <p/>

        This method does not close either <code>reader</code> or
        <code>writer</code>. <p/>

        @aribaapi ariba
    */
    public static void searchReplace (
            Reader reader,
            Writer writer,
            String regexToFind,
            String toReplace
    )
    throws IOException
    {
        Pattern pattern = Pattern.compile(regexToFind);
        Matcher matcher = pattern.matcher("");
        BufferedWriter output = new BufferedWriter(writer, 1024);
        LineNumberReader input = new LineNumberReader(reader);
        String line;
        while ((line = input.readLine()) != null) {
            matcher.reset(line);
            output.write(matcher.replaceAll(toReplace));
            output.write('\n');
        }
        output.flush();
    }

    /**
        Searches and replaces <code>file</code> replacing <code>regexToFind</code>
        with <code>toReplace</code> writing to <code>writer</code>. <p/>

        @aribaapi ariba
    */
    public static void searchReplace (
            File input,
            Writer writer,
            String regexToFind,
            String toReplace,
            Charset charset
    )
    throws IOException
    {
        Reader reader = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(input);
            reader = IOUtil.bufferedReader(fis, charset.name());
            searchReplace(reader, writer, regexToFind, toReplace);
        }
        finally {
            close(fis);
            close(reader);
        }
    }

    /**
        Searches and replaces <code>input</code> replacing <code>regexToFind</code>
        with <code>toReplace</code> writing to <code>writer</code>. <p/>

        @aribaapi ariba
    */
    public static void searchReplace (
            File input,
            Writer writer,
            String regexToFind,
            String toReplace
    )
    throws IOException
    {
        Charset charset = Charset.forName(I18NUtil.EncodingUTF_8);
        searchReplace(input, writer, regexToFind, toReplace, charset);
    }

    /**
        Searches and replaces <code>input</code> replacing <code>regexToFind</code>
        with <code>toReplace</code> writing to <code>output</code>. <p/>
        @aribaapi ariba
    */
    public static void searchReplace (
            File input,
            File output,
            String regexToFind,
            String toReplace,
            Charset charset
    )
    throws IOException
    {
        FileOutputStream fos = null;
        Writer writer = null;
        try {
            fos = new FileOutputStream(output);
            writer = IOUtil.bufferedWriter(fos, charset.name());
            searchReplace(input, writer, regexToFind, toReplace);
        }
        finally {
            close(fos);
            close(writer);
        }
    }

    /**
        Searches and replaces <code>input</code> replacing <code>regexToFind</code>
        with <code>toReplace</code> writing to <code>output</code>. <p/>
        @aribaapi ariba
    */
    public static void searchReplace (
            File input,
            File output,
            String regexToFind,
            String toReplace
    )
    throws IOException
    {
        Charset charset = Charset.forName(I18NUtil.EncodingUTF_8);
        searchReplace(input, output, regexToFind, toReplace, charset);
    }

    /**
        Reads and returns the first line from <code>file</code> using
        the specified <code>charset</code> to convert the bytes from
        the file into characters. <p/>

        @param file the file to read from
        @param charset the encoding to be used when reading the file
        @return the first line
        @aribaapi ariba
    */
    public static String readFirstLine (File file, Charset charset)
    throws IOException
    {
        List<String> lines = readLines(file, charset, 1);
        if (ListUtil.nullOrEmptyList(lines)) {
            return null;
        }
        return ListUtil.firstElement(lines);
    }

    /**
        Reads and returns the first <code>n</code> lines from <code>file</code> using
        the specified <code>charset</code> to convert the bytes from
        the file into characters. Use the method with care. <code>n</code> should typically be small as the lines are
        in memory<p/>

        @param file the file to read from
        @param charset the encoding to be used when reading the file
        @param n number of lines to read
        @return the first line
        @aribaapi ariba
    */
    public static List<String> readLines (File file, Charset charset, int n)
      throws IOException
    {
        if (!file.canRead()) {
            return null;
        }
        InputStream is = null;
        List<String> result = ListUtil.list();
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            Reader reader = IOUtil.bufferedReader(is, charset.name());
            LineNumberReader lineNumberReader =
                new LineNumberReader(reader);
            for (int i = 0; i < n; i++) {
                String line = lineNumberReader.readLine();
                if (line == null) {
                    return result;
                }
                result.add(line);
            }
            return result;
        }
        finally {
            close(is);
        }
    }

    /**
        Reads the first line from <code>file</code> assuming UTF-8. <p/>
        @param file the file to read from
        @return the first line
        @aribaapi ariba
    */
    public static String readFirstLine (File file)
    throws IOException
    {
        return readFirstLine(file, Charset.forName(I18NUtil.EncodingUTF_8));
    }

    /**
        Reads the entire contents from <code>file</code> using the specified
        <code>charset</code> to convert the bytes from the file into characters. <p/>

        @param file the file to read from
        @param charset the encoding to be used when reading the file
        @return the entire file content
        @aribaapi ariba
     */
    public static String readFileContent (File file, Charset charset)
    throws IOException
    {
        if (!file.canRead()) {
            return null;
        }
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            Reader reader = IOUtil.bufferedReader(is, charset.name());
            LineNumberReader lineNumberReader = new LineNumberReader(reader);
            FastStringBuffer buffer = new FastStringBuffer();

            String line = null;
            while ((line = lineNumberReader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            return buffer.toString();
        }
        finally {
            close(is);
        }
    }

    /**
    Reads the first line from <code>file</code> assuming UTF-8. <p/>
    @param file the file to read from
    @return the first line
    @aribaapi ariba
     */
    public static String readFileContent (File file)
    throws IOException
    {
        return readFileContent(file, Charset.forName(I18NUtil.EncodingUTF_8));
    }

    /**
        Writes <code>object</code> to <code>file</code> using the
        specified <code>charset</code>. <p/>

        @param file the file to write to
        @param object the object to write (<code>toString()</code> is called)
        @param append whether to append to the file or to truncate and write
        @param charset the encoding to use
        @aribaapi ariba
    */
    public static void writeToFile (
            File file,
            Object object,
            boolean append,
            Charset charset
    )
    throws IOException
    {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file, append));
            Writer writer = IOUtil.bufferedWriter(os, charset.name());
            writer.write(object.toString());
            writer.flush();
        }
        finally {
            close(os);
        }
    }

    /**
        Writes <code>object</code> to <code>file</code> assuming UTF-8. <p/>
        @param file the file to write to
        @param object the object to write (<code>toString()</code> is called)
        @param append whether to append to the file or to truncate and write
        @aribaapi ariba
    */
    public static void writeToFile (File file, Object object, boolean append)
    throws IOException
    {
        writeToFile(file, object, append, Charset.forName(I18NUtil.EncodingUTF_8));
    }


}
