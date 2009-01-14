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

    $Id: //ariba/platform/util/core/ariba/util/io/TempFile.java#5 $
*/

package ariba.util.io;

import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import java.io.File;
import java.util.Random;

/**
    Utility class for creating temporary files.  This class should only
    be used within server code or by command line (i.e., non-GUI) clients.

    @aribaapi private
*/
public class TempFile
{
    private static int TempFileUniqueKey = 0;
    private static final int TempFileMaxTries = 16;
    private static final Object TempFileUniqueKeyLock = new Object();
    private static final Random RandomGenerator = new Random();

    public static File createTempFile (File   directory,
                                       String prefix,
                                       String suffix)
    {
        return createTempFile(0, directory, prefix, suffix, null, 0);
    }

    public static File createTempFile (File   directory,
                                       String prefix,
                                       String suffix,
                                       String nodeName)
    {
        return createTempFile(0, directory, prefix, suffix, nodeName, 0);
    }
    public static File createTempFile (File   directory,
                                       String prefix,
                                       String suffix,
                                       long   sequence)
    {
        return createTempFile(0, directory, prefix, suffix, null, sequence);
    }


    private static File createTempFile (
        int recursion, File directory, String prefix, String suffix,
        String nodeName, long sequence)
    {
            // a crude implementation of gettmpnam - by serializing access to
            // the millisecond clock, and by introducing a short sleep, this
            // will guarantee that all files created by the server (using this
            // routine) will be unique.
        if (!directory.isDirectory()) {
            Log.util.warning(2809, directory.getAbsolutePath());
            return null;
        }
        int key;
        synchronized (TempFileUniqueKeyLock) {
            key = Math.abs(TempFileUniqueKey++);
        }
        String fileName = Fmt.S("%s%/%s_%s%s%s%s",
                                directory.getAbsolutePath(),
                                prefix,
                                Constants.getInteger(key),
                                StringUtil.nullOrEmptyString(nodeName) ?
                                "" : nodeName,
                                Constants.getLong(sequence),
                                (StringUtil.nullOrEmptyString(suffix) ?
                                "" : Fmt.S(".%s", suffix)));

          // now we will test to see if this file already exists if it
          // does it is likely that the last run of the server did not
          // clean up the temp files, we have landed here again.
        File file = new File(fileName);
        if (!file.exists()) {
            return file;
        }

        recursion++;
            // if we do have a match we simply reset our key to
            // another "random" number
        synchronized (TempFileUniqueKeyLock) {
            TempFileUniqueKey = Math.abs(RandomGenerator.nextInt());
        }
            // truly, if we get 16 hits in a row something is wrong...
        if (recursion > TempFileMaxTries) {
            Log.util.warning(2810, Integer.toString(TempFileMaxTries));
            return null;
        }
        return createTempFile(recursion, directory, prefix, suffix, nodeName, sequence);
    }
}
