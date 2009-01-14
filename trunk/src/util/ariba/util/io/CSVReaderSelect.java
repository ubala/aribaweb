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

    $Id: //ariba/platform/util/core/ariba/util/io/CSVReaderSelect.java#5 $
*/

package ariba.util.io;

import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.SystemUtil;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
    Read's a CSV file and based on command line arguments, only
    prints the columns requested. A sort of "select foo, bar from
    file". A utility class used by the build.

    @aribaapi private
*/
public class CSVReaderSelect implements CSVConsumer
{
    /**
        Our classname, for the usage message.
    */
    public static final String ClassName =
        "ariba.util.io.CSVReaderSelect";

    /*
        For I/O
    */
    private static PrintWriter out = SystemUtil.out();
    private static PrintWriter err = SystemUtil.err();

    /**
        Make a CSVReaderSelect object so it can implement CSVConsumer
    */
    public static void main (String[] args)
    {
        new CSVReaderSelect(args);
    }

    /**
        The command arguments, the 0th is the file, 1st is the encoding
    */
    private String[] args;

    /**
        Skip the first 2 args as NonColumnNames.

        0th is the file, 1st is the encoding
    */
    private static final int NonColumnNames = 2;

    /**
        The output csv file
    */
    private CSVWriter csvWriter = new CSVWriter(out);

    /**
        Setup the CSVReader and kick off the reading which calls back
        to consumeLineOfTokens
    */
    private CSVReaderSelect (String[] args)
    {
        this.args = args;

        if (args.length == 0) {
            Fmt.F(err,
                  "usage: java %s " +
                  "filename encoding columnname columnname ...\n",
                  ClassName);
            err.flush();
            SystemUtil.exit(1);
        }

        File file = new File(args[0]);
        String encoding = args[1];
        try {
            new CSVReader(this).read(file, encoding);
            out.flush();
        }
        catch (IOException ioe) {
            Fmt.F(err, "Problem with file %s: %s",
                  file, SystemUtil.stackTrace(ioe));
            err.flush();
            SystemUtil.exit(1);
        }
    }

    /**
        The number of columns in each line.
    */
    private int columns;

    /**
        The indexes of the columns to print.
    */
    private int[] selection;

    /**
        Process the first row which contains the column names.
    */
    private void setupSelect (List line)
    {
        columns = line.size();

            // If there were no columns specified, assume all columns
            // ala "select * from file"
        if (args.length == NonColumnNames) {
            selection = new int[columns];
            for (int s = 1; s < selection.length ; s++) {
                selection[s] = s;
            }
            return;
        }

            // turn the intersection of any column and any argument to true
        selection = new int[args.length-NonColumnNames];
        for (int a = NonColumnNames; a < args.length ; a++) {
            String arg = args[a];
            boolean found = false;
            for (int l = 0, s = line.size(); l < s ; l++) {
                String column = (String)line.get(l);
                if (column.equals(arg)) {
                    if (found) {
                        Fmt.F(err, "Duplicate column %s\n", arg);
                        err.flush();
                        SystemUtil.exit(1);
                    }
                    else {
                        selection[a-NonColumnNames] = l;
                        found = true;
                    }
                }
            }
            if (!found) {
                Fmt.F(err, "Unknown column %s\n", arg);
                err.flush();
                SystemUtil.exit(1);
            }
        }
    }

    //--- CSVConsumer ---------------------------------------------------------

    /**
        Called once per CSV line read.

        @param path - the CSV source file
        @param lineNumber - the current line being reported, 1-based.
        @param line - a List of tokens parsed from a one line in the file
    */
    public void consumeLineOfTokens (String path,
                                     int    lineNumber,
                                     List line)
    {
        if (lineNumber == 1) {
            setupSelect(line);
                // we fall through to print the header line
        }

        if (line.size() != columns) {
            Fmt.F(err,
                  "Expected all rows in %s to have %s columns like 1st row\n",
                  path, Constants.getInteger(columns));
            err.flush();
            SystemUtil.exit(1);
        }

            // iterate through the selection
        List row = ListUtil.list(selection.length);
        for (int i = 0, s = selection.length; i < s ; i++) {
            int column = selection[i];
            String string = (String)line.get(column);
            row.add(string);
        }
        csvWriter.writeLine(row);
    }
}
