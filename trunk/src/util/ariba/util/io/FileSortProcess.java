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

    $Id: //ariba/platform/util/core/ariba/util/io/FileSortProcess.java#6 $
*/

package ariba.util.io;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ServerUtil;
import ariba.util.core.SystemUtil;
import ariba.util.log.Log;
import ariba.util.log.Logger;
import java.io.File;
import org.apache.log4j.Level;
/**
   @aribaapi private
*/
public class FileSortProcess extends ARProcess
{
    public File inputFile;
    public File outputFile;

    int column = 1;
    char delimiter = ' ';

    public FileSortProcess (File inputOutputFile)
    {
        this(inputOutputFile, inputOutputFile);
    }

    public FileSortProcess (File inputFile,
                            File outputFile)
    {
        this.inputFile  = inputFile;
        this.outputFile = outputFile;
        setupArguments();
    }

    /**
        Note: Column is one-based
    */
    public void setKey (int column, char delimiter)
    {
        this.column    = column;
        this.delimiter = delimiter;
    }

    public boolean sort ()
    {
            // check the input file size - it should later match the
            // output file size
        long unsortedByteCount = fileSize(inputFile);
        if (unsortedByteCount == -1) {
            return false;
        }

        if (!launch()) {
            Log.util.warning(2856, ArrayUtil.formatArray("args", arguments()));
            return false;
        }
        waitFor();
        long sortByteCount = fileSize(outputFile);
        if (sortByteCount == -1) {
            Log.util.warning(2857,
                             outputFile.getAbsolutePath(),
                             ArrayUtil.formatArray("command", 
                             arguments()));
            return false;
        }
        if (sortByteCount != unsortedByteCount) {
            Log.util.warning(2858,
                             inputFile.getAbsolutePath(),
                             outputFile.getAbsolutePath(),
                             ArrayUtil.formatArray("command", arguments()));
            return false;
        }
        return true;
    }

    private void setupArguments ()
    {

        String inputFileName  = inputFile.getAbsolutePath();
        String outputFileName = outputFile.getAbsolutePath();
        String command;
        if (ServerUtil.isUnix) {
            command = "/bin/sort";
        }
        else {
            command = ".\\bin\\x86\\sort.exe";
        }
        String[] args = arguments(command, inputFileName, outputFileName);
        setArguments(args);
    }

    private String [] arguments (
        String program,
        String inputFileName,
        String outputFileName)
    {
        String [] args = new String[6];
        int idx = 0;
        args[idx++] = program;
        // note: MKS Sort requires -o argument to be prior to input filename
        args[idx++] = "-o";
        args[idx++] = outputFileName;
        args[idx++] = Fmt.S("-t%s", String.valueOf(delimiter));
        args[idx++] = Fmt.S("-k%s", column);
        args[idx++] = inputFileName;
        return args;
    }

    private static long fileSize (File file)
    {
        if (!file.isFile()) {
            Log.util.warning(2859, file.getAbsolutePath());
            return -1;
        }
        if (!file.canRead()) {
            Log.util.warning(2860, file.getAbsolutePath());
            return -1;
        }
        return file.length();
    }

    public static void main (String [] args)
    {
        Logger.getRootLogger().setLevel(Level.DEBUG);

        File inputFile =
            new File("ariba\\server\\server\\FileSortProcess.java");
        File outputFile =
            new File("FileSortProcess.java.sorted");

        if (args.length > 1) {
            outputFile = new File(args[1]);
            if (args.length > 0) {
                inputFile = new File(args[0]);
            }
        }

        Log.util.debug("Attempting sort.  Input file: %s, Output file: %s",
                   inputFile,
                   outputFile);

        FileSortProcess fileSortProcess = new FileSortProcess(inputFile,
                                                              outputFile);
        if (fileSortProcess.sort()) {
            Log.util.debug("File was sorted succesfully");
        }
        else {
            Log.util.debug("File was NOT sorted succesfully");
        }
        Log.util.debug("Stderr: %s", fileSortProcess.standardErrorString());
        Log.util.debug("Stdout: %s", fileSortProcess.standardOutputString());

        Log.util.debug("Test program exits");
        SystemUtil.exit(0);
    }
}
