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

    $Id: //ariba/platform/util/core/ariba/util/io/CSVWriter.java#9 $
*/

package ariba.util.io;

import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.StringUtil;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

/**
    CSVWriter will stream data to an outputStream or file.

    @aribaapi private
*/
public class CSVWriter
{
    private PrintWriter printWriter;

    /**
        Creates a CSVWriter which writes to a specified Writer after
        prepending a encoding header

        @param file The file where data will be written.
        @param encoding The encoding used to write to the file
        @param header true if an encoding header is desired
    */
    public CSVWriter (File    file,
                      String  encoding,
                      boolean header)
      throws IOException
    {
        this(IOUtil.printWriter(file, encoding),
             header ? encoding : null);
    }

    /**
        Creates a CSVWriter which writes to a specified Writer.

        @param writer The writer where data will be written.
    */
    public CSVWriter (Writer writer)
    {
        this(writer, null);
    }

    /**
        Creates a CSVWriter which writes to a specified Writer after
        prepending a encoding header

        @param writer The Writer where data will be written.
        @param encoding The encoding header to write.
    */
    public CSVWriter (Writer writer,
                      String encoding)
    {
        if (writer instanceof PrintWriter) {
            printWriter = (PrintWriter)writer;
        }
        else {
            printWriter = new PrintWriter(writer);
        }

        if (encoding != null) {
            Fmt.F(printWriter, "%s\n", encoding);
        }
    }

    /**
        Writes a vector of Strings as a CSV row. Each value will be
        separated by a comma, and double quoted correctly in CSV
        format.

        @param vector Strings to write to CSV file
    */
    public void writeLine (List vector)
    {
        writeLine(vector, null);
    }

    /**
        Writes a vector of Strings as a CSV row. Each value will be
        separated by a comma, and double quoted correctly in CSV
        format.

        @param vector Strings to write to CSV file
        @param patternToIgnore is not written in the csv file ie
               you will see ,, in the file as opposed to ,"",
    */
    public void writeLine (List vector, String patternToIgnore)
    {
        for (int i = 0, s = vector.size(); i < s ; i++) {
            String string = (String)vector.get(i);
            if (string != null && !string.equals(patternToIgnore)) {
                quoteStringForCSVFile(printWriter, string);
            }
            if (i+1 != s) {
                Fmt.F(printWriter, ",");
            }
        }
        Fmt.F(printWriter, "\n");
    }

    /**
        closes the stream.
    */
    public void close ()
    {
        printWriter.close();
    }

    /**
        Generally useful string quoting routine for producing CSV output.

        @param out output writer to which string will be written
        @param string string to write
    */
    public static void quoteStringForCSVFile (PrintWriter out, String string)
    {
        Fmt.F(out, "\"%s\"", StringUtil.replaceCharByString(string, '"', "\"\""));
    }
}
