/*
    Copyright 1996-2010 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



    $Id: //ariba/platform/util/core/ariba/util/io/CSVReader.java#27 $
*/

package ariba.util.io;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.SystemUtil;
import ariba.util.log.Log;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.MalformedInputException;
import java.util.List;

/**
    CSVReader parses files in CSV (comma separated values) format.
    Subclassers should implement the consumeLineOfTokens method.

    @aribaapi documented
*/
public final class CSVReader
{
    public static final int ErrorMissingComma = 1;
    public static final int ErrorUnbalancedQuotes = 2;
    public static final int ErrorIllegalCharacterOrByteSequence = 3;
      //represents All Lines
    public static final int AllLines = -1;

    private static final char DoubleQuote  = '"';
    private static final char Comma        = ',';
    private static final char CR           = '\r';
    private static final char LF           = '\n';
    private static final char Space        = ' ';
    private static final char Tab          = '\t';

    private static final int StateFirstLine        = 1;
    private static final int StateBeginningOfLine  = 2;
    private static final int StateBeginningOfField = 3;
    private static final int StateEndOfField       = 4;
    private static final int StateInUnquotedField  = 5;
    private static final int StateInQuotedField    = 6;
    private static final int StateEOF              = 7;

    static final int TokenBufferSize = 8192;

    private CSVConsumer csvConsumer;
    private CSVErrorHandler csvErrorHandler;
    private String encoding;
    private boolean encodingIsExplicitlySet = false;
    private boolean returnNoValueAsNull = false;
      //Max number of lines to consider while reading. -1 implies all.
    private int numberOfLines = AllLines;

    /**
        Create a new CSVReader using a specific CSVConsumer to handle
        the rows. After reading has been performed, the CSVReader can
        be asked what the encoding was used.

        @param csvConsumer CSVConsumer for handling rows
        @aribaapi documented
    */
    public CSVReader (CSVConsumer csvConsumer)
    {
        this(csvConsumer, null);
    }

    /**
        Create a new CSVReader using a specific CSVConsumer to handle
        the rows and a specified CSVErrorHandler to handle the errors.
        After reading has been performed, the CSVReader can
        be asked what the encoding was used.

        @param csvConsumer CSVConsumer for handling rows
        @param csvErrorHandler CSVErrorHandler for handling CSV format errors;
                if null, CSVDefaultErrorHandler will be used
        @aribaapi ariba
    */
    public CSVReader (CSVConsumer csvConsumer, CSVErrorHandler csvErrorHandler)
    {
        this.csvConsumer = csvConsumer;
        if (csvErrorHandler == null) {
            this.csvErrorHandler = new CSVDefaultErrorHandler();
        }
        else {
            this.csvErrorHandler = csvErrorHandler;
        }
    }

    /**
        This private constructor is needed by the static method
        readAllLines in order to initialize the inner class
        CSVConsumerHelper.

    */
    private CSVReader ()
    {
    }

    /**
        Return the encoding used for the last read operation. May be
        null if a Reader was passed in so we never know the underlying
        encoding if any.
        @return encoding name

        @aribaapi documented
    */
    public String getEncoding ()
    {
        return encoding;
    }

    /**
        Returns true if the file had the encoding as the first line in the file,
        e.g. 8859_1 or 8859_1,,
        @return true if encoding is set at the beginning of the file

        @aribaapi documented
    */
    public boolean isEncodingExplicitlySet ()
    {
        return encodingIsExplicitlySet;
    }

    /**
        Sets the number of lines consider while reading.
        @param n number of lines
        @aribaapi private
    */
    public void setNumberOfLines (int n)
    {
        Assert.that(n >= AllLines, "Number of lines %s is not valid.", n);
        numberOfLines = n;
    }

    /**
     * Alters the behavior of the CSV Reader when parsing empty value
     * (i.e an <i>unquoted</i> empty or blank string).
     *
     * When set to <code>true</code> empty values are returned as null
     * while when set to <code>false</code> they are returned as empty String.
     * The default behavior is to return empty Strings,
     *
     * @param value the new value for the toggling attribute
     * @aribaapi documented
     */
    public void setReturnEmptyValueAsNull (boolean value)
    {
        returnNoValueAsNull = value;
    }

    /**
        Reads the specified URL, using the character encoding for the
        default locale.

        @param url the URL to read the data from
        @param defaultEncoding the encoding to use to read the data if
        none can be determined from the URLConnection

        @exception IOException any IOException reading from the URL
        @aribaapi documented
    */
    public void read (URL    url,
                      String defaultEncoding)
      throws IOException
    {
        URLConnection urlConnection = url.openConnection();
        encoding = urlConnection.getContentEncoding();
        if (encoding == null) {
            encoding = defaultEncoding;
        }
        Assert.that(encoding != null, "null encoding for %s", url);
        Reader in = IOUtil.bufferedReader(urlConnection.getInputStream(),
                                          encoding);
        read(in, url.toString());
        in.close();
    }

    /**
        Reads the specified file, using the character encoding for the
        default locale.

        @param file a path to the file to read
        @param encoding the encoding to use to read the data

        @exception IOException any IOException reading from the file
        could not be read.
        @aribaapi documented
    */
    public void read (File file, String encoding)
      throws IOException
    {
        Reader in = IOUtil.bufferedReader(file, encoding);
        read(in, file.getCanonicalPath());
        in.close();
    }

    /**
        Will read from the specified stream in the encoding specified on the
        first line of the stream.  For instance, the first line of the file
        may be "8859_1", or may be "8859_1,,,".  The "8859_1" will be passed
        into read(inputStream, encoding, path) as the encoding.

        @param url the URL to read the data from

        @exception IOException any IOException reading from the URL
        @aribaapi documented
    */
    public void readForSpecifiedEncoding (URL url)
      throws IOException
    {
        InputStream in = url.openStream();
        try {
            readForSpecifiedEncoding(in, url.toString());
        }
        finally {
            in.close();
        }
    }

    /**
        Will read from the specified stream in the encoding specified on the
        first line of the stream.  For instance, the first line of the file
        may be "8859_1", or may be "8859_1,,,".  The "8859_1" will be passed
        into read(inputStream, encoding, path) as the encoding.

        @param file the path to the file to read

        @exception IOException if an IOException occurs while reading
        the file

        @see #read(Reader, String)
        @aribaapi documented
    */
    public final void readForSpecifiedEncoding (File file)
      throws IOException
    {
        readForSpecifiedEncoding(file, null);
    }

    /**
        Will read from the specified stream in the encoding specified on the
        first line of the stream.  For instance, the first line of the file
        may be "8859_1", or may be "8859_1,,,".  The "8859_1" will be passed
        into read(inputStream, encoding, path) as the encoding.

        @param file the path to the file to read
        @param altEncoding alternate encoding to be used if not specified

        @exception IOException if an IOException occurs while reading
        the file

        @see #read(Reader, String)
        @aribaapi documented
    */
    public final void readForSpecifiedEncoding (File file, String altEncoding)
      throws IOException
    {
        BufferedInputStream in = IOUtil.bufferedInputStream(file);
        readForSpecifiedEncoding(in, file.getCanonicalPath(), altEncoding);
        in.close();
    }

    /**
        Will read from the specified stream in the encoding specified on the
        first line of the stream.  For instance, the first line of the file
        may be "8859_1", or may be "8859_1,,,".  The "8859_1" will be passed
        into read(inputStream, encoding, path) as the encoding.

        If there is no encoding line, this function should handle it gracefully.

        @param inputStream the InputStream to read the data from
        @param location the path to the data source for debugging messages

        @exception IOException if an IOException occurs while reading
        the file

        @see #read(Reader, String)
        @aribaapi documented
    */
    public final void readForSpecifiedEncoding (InputStream inputStream,
                                                String      location)
      throws IOException
    {
        readForSpecifiedEncoding(inputStream, location, null);
    }


    /**
        Will read from the specified stream in the encoding specified on the
        first line of the stream.  For instance, the first line of the file
        may be "8859_1", or may be "8859_1,,,".  The "8859_1" will be passed
        into read(inputStream, encoding, path) as the encoding.

        If there is no encoding line, use the altEncoding if available.
        Otherwise, use the system default encoding.

        @param inputStream the InputStream to read the data from
        @param location the path to the data source for debugging messages
        @param altEncoding alternate encoding to be used if not specified

        @exception IOException if an IOException occurs while reading
        the file

        @see #read(Reader, String)
        @aribaapi documented
    */
    public final void readForSpecifiedEncoding (InputStream inputStream,
                                                String      location,
                                                String      altEncoding)
      throws IOException
    {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        inputStream.mark(2048);
            // extra buffering to make sure the reading of the first
            // line doesn't go past the length marked
        byte buf[] = new byte[2048];
        int len = inputStream.read(buf, 0, 2048);
        inputStream.reset();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf,0, len);

        //check for unicode marker
        ByteBuffer bb = java.nio.ByteBuffer.wrap(buf);
        CharBuffer cb = bb.asCharBuffer();
        char marker = cb.get(0);

        if (marker == 0xfffe || marker == 0xfeff) {
            Reader reader = IOUtil.bufferedReader(bais, "UTF-16");
            LineNumberReader line = new LineNumberReader(reader);
            encoding = line.readLine();
        }
        else {
            encoding =  IOUtil.readLine(bais);
        }

        if (encoding == null) {
                //The file is completely empty
            return;
        }
        encoding = extractEncoding(encoding);
        try {
                // test the encoding
            new String(new byte[0], encoding);
            encodingIsExplicitlySet = true;
                // pull the first line (encoding) back out of the
                // buffer
            IOUtil.readLine(inputStream);
        }
        catch (UnsupportedEncodingException uee) {
            encodingIsExplicitlySet = false;
            if (altEncoding != null) {
                encoding = altEncoding;
            }
            else {
                encoding = IOUtil.getDefaultSystemEncoding();
            }
        }
        catch (RuntimeException e) {
            /*
                We actually want to catch java.nio.charset.IllegalCharsetNameException
                but Tibco code has to run on JDK 1.3 so we can't catch it yet
                Defect 124155 has been filed to track this
            */
            Log.util.debug("Runtime exception caught " +
                           "while parsing encoding for a CSV file." +
                           "Encoding read is '%s'.",
                           encoding);
            encodingIsExplicitlySet = false;
            if (altEncoding != null) {
                encoding = altEncoding;
            }
            else {
                encoding = IOUtil.getDefaultSystemEncoding();
            }
        }
        read(IOUtil.bufferedReader(inputStream, encoding), location);
    }

    /**
        Extract the encoding from the given string. The encoding may
        have trailing commas, or it may be double quoted. The double
        quotes are stripped from the encoding string.
        <p>
        Example: 8859_1 will be returned in all the following cases: (i)
        8859_1,, (ii) "8859_1",, (iii) 8859_1 (iv) "8859_1".
        <p>
        Implementation details: to make this method efficient, an
        assumption is made that any valid encoding will not contain a
        comma. An input string "8859,_1" will result in the string
        "8859 (with the leading quote) returned. Strictly speaking, we
        should return 8859,_1, but note that both of these returned
        forms cannot be a valid encoding. The caller of this method
        will test the returned String to make sure it is a valid (and
        supported) encoding. So we just save the cpu cycles of parsing
        this correctly (they will both end up wrong anyway). To be
        really correct, we need to take care of escaped double quotes
        as well... It's just not worth the effort.
        <p>
        @param encoding the raw encoding String from the CSV file, must not be null.
        @return the encoding String.
    */
    private String extractEncoding (String encoding)
    {
        /**
            finds the indices of the first and last non white spaces
            (if any) from the input string in the range [0..x), where
            x is the index of the Comma (if present) or the length of
            the string. Returns the chars in between.
        */
        int loLimit = firstNonWhiteSpaceIndex(encoding);
        if (loLimit == encoding.length()) {
                // not much we can do. This is either an empty String or
                // all characters are spaces. Note that this would be
                // an invalid encoding, which will be handled by
                // readForSpecifiedEncoding
            return encoding;
        }
        int hiLimit = encoding.indexOf(Comma);
        if (hiLimit == -1) {
            hiLimit = encoding.length();
        }
        hiLimit = lastNonWhiteSpaceIndexPlusOne(encoding, loLimit, hiLimit);
        if (encoding.charAt(loLimit) != DoubleQuote ||
            encoding.charAt(hiLimit-1) != DoubleQuote) {
            return (loLimit == 0 && hiLimit == encoding.length())
                ? encoding : encoding.substring(loLimit, hiLimit);
        }
        return encoding.substring(loLimit+1, hiLimit-1);
    }

    /**
        Returns the index of the first non white space (actually the
        first non ASCII control characters) of the given string.
        @param str the specified string, must not be null.
        @return the index of the first non white space (actually the
        first non ASCII control characters) of the given string. Note
        that a returned value equal to the str.length() means that all
        characters in the string are ASCII control characters.
    */
    private int firstNonWhiteSpaceIndex (String str)
    {
        int st = 0;
        int len = str.length();
        while ((st < len) && (str.charAt(st) <= Space)) {
            st++;
        }
        return st;
    }

    /**
        Returns the index of the last non white space (actually non ASCII control
        characters) of the given string.
        @param str the given string, must not be null.
        @param start the index to start searching.
        @param onePastEnd the index to stop searching.
        @return the index of the last non white space (actually non ASCII control
        characters) of the given string. Note that a returned value of start means that
        all the characters of the given string are ACSII control characters.
    */
    private int lastNonWhiteSpaceIndexPlusOne (String str, int start, int onePastEnd)
    {
        int index = onePastEnd;
        while (start < index && str.charAt(index-1) <= Space) {
            index--;
        }
        return index;
    }

    /**
        The primitive read entry point. This method calls the
        CSVConsumer callback for each line read.

        @param reader the data source as passed in by one of the
        higher level read methods
        @param location the path for debugging messages

        @exception IOException if an IOException occurs while reading
        from <b>reader</b>

        @see ariba.util.io.CSVConsumer#consumeLineOfTokens
        @aribaapi documented
    */
    public final void read (Reader reader,
                            String location)
      throws IOException
    {
            // this is the number of the line currently begin processed
        int     lineNumber = 1;
            // this is the line number passed to the consumeLineOfTokens method.
        int lineNumberToConsume = lineNumber;

        int     ch         = 0;
        int     state      = StateFirstLine;
        List  tokens     = ListUtil.list();
        FastStringBuffer token       = new FastStringBuffer(TokenBufferSize);
        boolean currentTokenIsQuoted = false;

        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader);
        }
        reader.mark(1);
        try {
            if (reader.read() == -1) {
                return;
            }
        }
        catch (MalformedInputException e) {
            csvErrorHandler.handleError(
                ErrorIllegalCharacterOrByteSequence, location, lineNumber);
            throw e;
        }
        reader.reset();
        try {
            while (state != StateEOF) {
                switch (state) {

                  case StateFirstLine: {
                      ch = reader.read();
                      state = StateBeginningOfField;
                      break;
                  }

                  case StateBeginningOfLine: {
                      if (canConsume(lineNumberToConsume)) {
                          csvConsumer.consumeLineOfTokens(location,
                                                          lineNumberToConsume,
                                                          tokens);
                      }
                      else {
                          state = StateEOF;
                          break;
                      }
                      tokens = ListUtil.list();
                      state = StateBeginningOfField;
                      break;
                  }

                  case StateBeginningOfField: {
                      while ((ch == Space) || (ch == Tab)) {
                          ch = reader.read();
                      }
                      if (ch == DoubleQuote) {
                          state = StateInQuotedField;
                          currentTokenIsQuoted = true;
                          ch = reader.read();
                      }
                      else {
                          state = StateInUnquotedField;
                          currentTokenIsQuoted = false;
                      }
                      break;
                  }

                  case StateEndOfField: {
                      String currentToken = token.toString();

                          // only trim if the value is not double-quoted
                      if (!currentTokenIsQuoted) {
                          currentToken = currentToken.trim();
                      }

                      while ((ch == Space) || (ch == Tab)) {
                          ch = reader.read();
                      }

                      if (returnNoValueAsNull &&
                          !currentTokenIsQuoted &&
                          currentToken.length() == 0)
                      {
                          tokens.add(null);
                      }
                      else {
                          tokens.add(currentToken);
                      }
                      token.truncateToLength(0);

                      if (ch == Comma) {
                          state = StateBeginningOfField;
                          ch = reader.read();
                      }

                      else if (ch == LF || ch == CR) {
                          lineNumberToConsume = lineNumber;
                          state = StateBeginningOfLine;
                          if (ch == LF) {
                              lineNumber++;
                          }

                          for (; ch == LF || ch == CR;) {
                              ch = reader.read();
                              if (ch == LF) {
                                  lineNumber++;
                              }
                          }

                          if (ch == -1) {
                              state = StateEOF;
                          }
                      }

                      else if (ch == -1) {
                          state = StateEOF;
                          lineNumberToConsume = lineNumber;
                          lineNumber++;
                      }

                      else {
                          csvErrorHandler.handleError(
                              ErrorMissingComma, location, lineNumber);
                          state = StateBeginningOfField;
                      }

                      break;
                  }

                  case StateInUnquotedField: {
                      while (ch >= 0 && ch != Comma && ch != CR && ch != LF) {
                          token.append((char)ch);
                          ch = reader.read();
                      }

                      state = StateEndOfField;
                      break;
                  }

                  case StateInQuotedField: {
                      while (state == StateInQuotedField) {
                          while (ch >= 0 && ch != DoubleQuote) {
                              if (ch != CR) {
                                  token.append((char)ch);
                              }
                              ch = reader.read();
                          }

                          /*
                              A doubleQuote ends the quoted token, unless there
                              are two in a row.  Two doubleQuotes in a row is
                              taken to mean a doubleQuote character value.
                          */
                          if (ch == DoubleQuote) {
                              ch = reader.read();
                              if (ch == DoubleQuote) {
                                  token.append((char)ch);
                                  ch = reader.read();
                              }
                              else {
                                  /* that was the matching quote */
                                  break;
                              }
                          }
                          else {
                              csvErrorHandler.handleError(
                                  ErrorUnbalancedQuotes, location, lineNumber);
                              break;
                          }
                      }

                      state = StateEndOfField;
                      break;
                  }

                  default: {
                      state = StateEOF;
                      break;
                  }
                }
            }

            if (!tokens.isEmpty() && canConsume(lineNumberToConsume)) {
                csvConsumer.consumeLineOfTokens(location,
                                                lineNumberToConsume,
                                                tokens);
            }
        }
        catch (MalformedInputException e) {
            csvErrorHandler.handleError(
                ErrorIllegalCharacterOrByteSequence, location, lineNumber);
            throw e;
        }
        finally {
            reader.close();
        }
    }

    /**
        @param lineNumberToConsume line number of the line being consumed
        @return true if the line can be consumed
    */
    private boolean canConsume (int lineNumberToConsume)
    {
        return ((numberOfLines == AllLines) || (lineNumberToConsume <= numberOfLines));
    }

    /**
        Reads all the lines into the memory specified in the input file
        and returns a list of list each of which contain one line
        of input. Please note, that the complete csv file will be stored
        in memory.

        @param file the data source as passed in by one of the
               higher level read methods
        @param encoding the encoding to use to read the data
        @return vector of vector contains each line of input
        @aribaapi documented
    */
    public static List readAllLines (File file,
                                     String encoding)
      throws IOException
    {
        return readAllLines(file, encoding, false);
    }

    /**
        Reads all the lines into the memory specified in the input file
        and returns a list of list each of which contain one line
        of input. Please note, that the complete csv file will be stored
        in memory.

        @param file the data source as passed in by one of the
               higher level read methods
        @param encoding the encoding to use to read the data
        @param ignoreComments if true it ignores all lines starting with '#'
        @return vector of vector contains each line of input
        @aribaapi documented
    */
    public static List readAllLines (File file,
                                     String encoding,
                                     boolean ignoreComments)
      throws IOException
    {
        return readAllLines(file, encoding, ignoreComments, null, false);
    }

    /**
        Reads all the lines into the memory specified in the input file
        and returns a list of list each of which contain one line
        of input. Please note, that the complete csv file will be stored
        in memory.

        @param file the data source as passed in by one of the
               higher level read methods
        @param encoding the encoding to use to read the data
        @param ignoreComments if true it ignores all lines starting with '#'
        @param emptyValueAsNull see {@link #setReturnEmptyValueAsNull}
        @return vector of vector contains each line of input
        @aribaapi documented
    */
    public static List readAllLines (File file,
                                     String encoding,
                                     boolean ignoreComments,
                                     boolean emptyValueAsNull)
      throws IOException
    {
        return readAllLines(file, encoding, ignoreComments, null, emptyValueAsNull);
    }

    /**
        Reads all the lines into the memory specified in the input file
        and returns a list of list each of which contain one line
        of input. Please note, that the complete csv file will be stored
        in memory.

        @param file the data source as passed in by one of the
               higher level read methods
        @param encoding the encoding to use to read the data
        @param ignoreComments if true it ignores all lines starting with
               the commentMarker String
        @param commentMarker String that marks the begin of a comment. It
               has to be at the beginning of a line. If null is specified
               and ignoreComments is true '#' is used as comment marker
        @param emptyValueAsNull see {@link #setReturnEmptyValueAsNull}
        @return vector of vector contains each line of input
        @aribaapi documented
    */
    public static List readAllLines  (File    file,
                                      String  encoding,
                                      boolean ignoreComments,
                                      String  commentMarker,
                                      boolean emptyValueAsNull)
      throws IOException
    {
         return readNLines(file, encoding, ignoreComments,
                           commentMarker, emptyValueAsNull, AllLines);
    }

     /**
         @see #readAllLines(File, String, boolean, String, boolean)
         @param nLines number of lines to read
     */
     public static List readNLines  (File    file,
                                       String  encoding,
                                       boolean ignoreComments,
                                       String  commentMarker,
                                       boolean emptyValueAsNull,
                                       int nLines)
       throws IOException
     {
        Assert.that(file != null, "File is null!");

        if (!file.canRead()) {
            throw new FileNotFoundException(Fmt.S("Cannot read file %s",
                file));
        }

        List allLines = ListUtil.list();
        CSVConsumerHelper csvConsumerHelper =
            new CSVReader().new CSVConsumerHelper(allLines,
                ignoreComments,
                commentMarker);
        CSVReader reader = new CSVReader(csvConsumerHelper);
        reader.setNumberOfLines(nLines);
        reader.setReturnEmptyValueAsNull(emptyValueAsNull);
        if (encoding == null) {
            reader.readForSpecifiedEncoding(file);
        }
        else {
            reader.read(file, encoding);
        }
        return allLines;
    }

    /**
        Implements the CSVConsumer interface.

        @aribaapi private
    */
    class CSVConsumerHelper implements CSVConsumer
    {
        private boolean _ignoreComments;
        private List    _allLines;
        private CommentChecker _commentChecker = null;

        CSVConsumerHelper (List    allLines,
                           boolean ignoreComments,
                           String  commentMarker)
        {
            _allLines         = allLines;
            _ignoreComments   = ignoreComments;
            if (_ignoreComments) {
                _commentChecker   = new CommentChecker(commentMarker);
            }
        }

        /**
            Implementation of callback specified by CSVConsumer.
            It cumulates the input lines.

            @param path the CSV source file
            @param lineNumber the current line being reported, 1-based.
            @param line a List of tokens parsed from a one line in the file
            @aribaapi private
        */
        public void consumeLineOfTokens (String path,
                                         int lineNumber,
                                         List line)
        {
            if (_ignoreComments) {
                if (_commentChecker.isComment(line)) {
                    return;
                }
            }
            _allLines.add(line);
        }
    }

    /**
        Reads a CSV file and outputs the results.  Useful for testing.

        @param args command-line arguments; arg[0] is the CSV filename, arg[1]
            is the encoding type
        @exception IOException I/O error occurred while reading the file
        @aribaapi private
    */
    public static void main (String args[])
      throws IOException
    {
        File f = new File(args[0]);
        String encoding = null;
        if (args.length == 2) {
            encoding = args[1];
        }
        List v = CSVReader.readAllLines(f, encoding);
        for (int i=0 ; i<v.size(); i++) {
            Fmt.F(SystemUtil.out(), "Line %s has the following values:\n", i);
            List elements = (List)v.get(i);
            for (int j=0; j<elements.size(); j++) {
                Fmt.F(SystemUtil.out(), "\t%s\n", elements.get(j));
            }
        }
        SystemUtil.exit(0);
    }
}

/**
    This class checks to see if a line (with its tokens represented as
    a List) is a comment.
*/
class CommentChecker
{
    private static final String DefaultCommentMarker = "#";

    /**
        commentMarker specifies what a comment is.
    */
    private String commentMarker;

    /**
        Construct an instance with the given commentMarker
        @param commentMarker the string that indicates a
        comment. Lines beginning with this value will be treated as
        comments. If null, the default ('#') will be used.
    */
    CommentChecker (String commentMarker)
    {
        this.commentMarker = (commentMarker == null) ?
            DefaultCommentMarker :
            commentMarker;
    }

    /**
        Checks to see if the given line is a comment.
        @param line the given line
        @return true if this line is a comment, false otherwise.
    */
    boolean isComment (List line)
    {
        Object first = ListUtil.firstElement(line);
        return ((first instanceof String) &&
                ((String)first).startsWith(commentMarker));
    }
}
