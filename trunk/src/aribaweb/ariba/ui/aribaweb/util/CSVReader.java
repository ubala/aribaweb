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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/CSVReader.java#9 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.IOUtil;
import java.util.List;
import ariba.util.io.CSVConsumer;
import ariba.util.log.Log;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

/**
    CSVReader parses files in CSV (comma separated values) format.
    Subclassers should implement the consumeLineOfTokens method.

    @aribaapi private
*/
public final class CSVReader
{
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

        /*
        Taken from Core Java 2 Volume I, pp639-641
        */
    private static final String[] charEncodings = {
        "8859_1",
        "8859_2",
        "8859_3",
        "8859_4",
        "8859_5",
        "8859_6",
        "8859_7",
        "8859_8",
        "8859_9",
        "Big5",
        "CNS11643",
        "Cp037",
        "Cp1006",
        "Cp1025",
        "Cp1026",
        "Cp1046",
        "Cp1097",
        "Cp1098",
        "Cp1112",
        "Cp1123",
        "Cp1124",
        "Cp1250",
        "Cp1251",
        "Cp1252",
        "Cp1253",
        "Cp1254",
        "Cp1255",
        "Cp1256",
        "Cp1257",
        "Cp1258",
        "Cp1381",
        "Cp1383",
        "Cp273",
        "Cp277",
        "Cp278",
        "Cp280",
        "Cp284",
        "Cp285",
        "Cp297",
        "Cp33722",
        "Cp420",
        "Cp424",
        "Cp437",
        "Cp500",
        "Cp737",
        "Cp775",
        "Cp838",
        "Cp850",
        "Cp852",
        "Cp855",
        "Cp857",
        "Cp860",
        "Cp861",
        "Cp862",
        "Cp863",
        "Cp864",
        "Cp865",
        "Cp866",
        "Cp869",
        "Cp871",
        "Cp874",
        "Cp875",
        "Cp918",
        "Cp921",
        "Cp922",
        "Cp930",
        "Cp933",
        "Cp935",
        "Cp937",
        "Cp939",
        "Cp942",
        "Cp948",
        "Cp949",
        "Cp950",
        "Cp964",
        "Cp970",
        "EUCJIS",
        "GB2312",
        "GBK",
        "ISO2022CN",
        "ISO2022CN_CNS",
        "ISO2022CN_GB",
        "ISO2022KR",
        "JIS",
        "JIS0208",
        "KOI8_R",
        "KSC5601",
        "MS874",
        "MacArabic",
        "MacCentralEurope",
        "MacCroatian",
        "MacCyrillic",
        "MacDingbat",
        "MacGreek",
        "MacHebrew",
        "MacIceland",
        "MacRoman",
        "MacRomania",
        "MacSymbol",
        "MacThai",
        "MacTurkish",
        "MacUkrain",
        "SJIS",
        "UTF8"
        };

    static private StringSet sEncodingStrings = new StringSet();
    static {
        for (int i = 0; i < charEncodings.length; ++i) {
            sEncodingStrings.put(charEncodings[i]);
        }
    }

    static final int TokenBufferSize = 4 * 8192;

    private CSVConsumer csvConsumer;
    private String encoding;
    private boolean encodingIsExplicitlySet = false;

    /**
        Create a new CSVReader using a specific CSVConsumer to handle
        the rows. After reading has been performed, the CSVReader can
        be asked what the encoding was used.
        @aribaapi documented
    */
    public CSVReader (CSVConsumer csvConsumer)
    {
        this.csvConsumer = csvConsumer;
    }

    /**
        Return the encoding used for the last read operation. May be
        null if a Reader was passed in so we never know the underlying
        encoding if any.
        @aribaapi documented
        @return The value of the content-encoding header field.
    */
    public String getEncoding ()
    {
        return encoding;
    }

    /**
        Returns true if the file had the encoding as the first line in the file,
        e.g. 8859_1 or 8859_1,,

        @aribaapi documented
        @return boolean indicating if the file has the encoding in the first line
    */
    public boolean isEncodingExplicitlySet ()
    {
        return encodingIsExplicitlySet;
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
        Reader in = IOUtil.bufferedReader(url.openStream(), encoding);
        read(in, url.toString());
        in.close();
    }

    /**
        Reads the specified file, using the character encoding for the
        default locale.

        @param file a path to the file to read
        @param encoding The encoding used for reading the file

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
        Reader in = IOUtil.bufferedReader(url.openStream(), encoding);
        read(in, url.toString());
        in.close();
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
        BufferedInputStream in = IOUtil.bufferedInputStream(file);
        readForSpecifiedEncoding(in, file.getCanonicalPath());
        in.close();
    }

    /**
        Will read from the specified stream in the encoding specified on the
        first line of the stream.  For instance, the first line of the file
        may be "8859_1", or may be "8859_1,,,".  The "8859_1" will be passed
        into read(inputStream, encoding, path) as the encoding.

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
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        inputStream.mark(2048);
        encoding = IOUtil.readLine(inputStream);

            // Handle the case where there are trailing commas
        int commaIndex = encoding.indexOf(Comma);
        if (commaIndex != -1) {
            encoding = encoding.substring(0, commaIndex);
        }

        try {
            this.encodingIsExplicitlySet = true;
            read(new InputStreamReader(inputStream, encoding), location);
        }
        catch (UnsupportedEncodingException uee) {
            inputStream.reset();
            read(new InputStreamReader(inputStream), location);
        }
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
        int     next       = 0;
        int     lineNumber = 1;
        int     ch         = 0;
        int     state      = StateFirstLine;
        List  tokens     = ListUtil.list();
        char[]  token      = new char[TokenBufferSize];
        boolean currentTokenIsQuoted = false;

        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader);
        }

        try {
            while (state != StateEOF) {
                switch (state) {

                  case StateFirstLine: {
                      ch = reader.read();
                      state = StateBeginningOfField;
                      break;
                  }

                  case StateBeginningOfLine: {
                      csvConsumer.consumeLineOfTokens(location,
                                                      lineNumber - 1,
                                                      tokens);
                      tokens = ListUtil.list ();
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
                      String currentToken = new String(token, 0, next);

                          // only trim if the value is not double-quoted
                      if (!currentTokenIsQuoted) {
                          currentToken = currentToken.trim();
                      }

                      while ((ch == Space) || (ch == Tab)) {
                          ch = reader.read();
                      }

                      tokens.add(currentToken);
                      next = 0;

                      if (ch == Comma) {
                          state = StateBeginningOfField;
                          ch = reader.read();
                      }

                      else if (ch == LF || ch == CR) {
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
                          lineNumber++;
                      }

                      else {
                          Log.util.warning(2793, location, lineNumber);
                          state = StateBeginningOfField;
                      }

                      break;
                  }

                  case StateInUnquotedField: {
                      while (ch >= 0 && ch != Comma && ch != CR && ch != LF) {
                          if (next < TokenBufferSize) {
                              token[ next++ ] = (char)ch;
                          }
                          ch = reader.read();
                      }

                      state = StateEndOfField;
                      break;
                  }

                  case StateInQuotedField: {
                      while (state == StateInQuotedField) {
                          while (ch >= 0 && ch != DoubleQuote) {
                              if (ch != CR) {
                                  if (next < TokenBufferSize) {
                                      token[ next++ ] = (char)ch;
                                  }
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
                                  if (next < TokenBufferSize) {
                                      token[ next++ ] = (char)ch;
                                  }
                                  ch = reader.read();
                              }
                              else {
                                  /* that was the matching quote */
                                  break;
                              }
                          }
                          else {
                              Log.util.warning(2793, location, lineNumber);
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

            if (!tokens.isEmpty()) {
                csvConsumer.consumeLineOfTokens(location,
                                                lineNumber - 1,
                                                tokens);
            }
        }
        finally {
            reader.close();
        }
    }
}
