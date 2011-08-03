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

    $Id: //ariba/platform/util/core/ariba/util/log/LogFile.java#9 $
*/
package ariba.util.log;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import java.io.File;

class LogFile
{
    private String prefix = "";
    private String suffix = "txt";
    private File file;
    protected boolean valid = false;
    private String archivedFileName;

    LogFile (File path, String prefix)
    {
        this.init(path, prefix, "");
    }

    LogFile (File path, String prefix, String suffix)
    {
        this.init(path, prefix, suffix);
    }

    protected void init (File path, String prefix, String suffix)
    {
        if (prefix.length() != 0) {
            this.prefix = prefix;
        }
        if (suffix.length() != 0) {
            this.suffix = suffix;
        }
        this.file = new File(path, Fmt.S("%s.%s", this.prefix, this.suffix));
    }

    File file ()
    {
        return this.file;
    }

    String archivedFileName ()
    {
        return this.archivedFileName;
    }

    /**
     Return an int as a String, making sure its padded with zeroes
     if it is between 0 and 10.
     */
    private static final String pad0 (int value)
    {
        if ((value >= 0) &&
                (value < 10))
        {
            return Fmt.S("0%s", value);
        }
        return Integer.toString(value);
    }

    /**
     LogFile implements MoveTo since it should decide on the
     encoding scheme
     */
    boolean moveTo (File targetDirectory)
    {
        Date now = new Date();
        String logFileSaveName =
                Fmt.S("%s.%s-%02s-%02s_%02s.%02s.%02s.%s",
                        ArrayUtil.array(
                                this.prefix,
                                Constants.getInteger(Date.getYear(now)),
                                Constants.getInteger(Date.getMonth(now)+1),
                                Constants.getInteger(Date.getDayOfMonth(now)),
                                Constants.getInteger(Date.getHours(now)),
                                Constants.getInteger(Date.getMinutes(now)),
                                Constants.getInteger(Date.getSeconds(now)),
                                this.suffix));
        this.archivedFileName = logFileSaveName;
        File saveToFile =
                new File(targetDirectory.getAbsolutePath(), logFileSaveName);
        // renameTo does not modify the current object - hence
        // this.file is left unchanged
        boolean success = false;
        if (this.file.exists()) {
            success = this.file.renameTo(saveToFile);
            if (!success) {
                //if renameTo failed, try copying before we give up
                if (this.file.exists()) {
                    success = IOUtil.copyFile(this.file, saveToFile);
                    if (success) {
                        this.file.delete();
                    }
                    else {
                        this.archivedFileName = null;
                        Log.util.warning(2801,
                                file.getAbsolutePath(),
                                saveToFile.getAbsolutePath());
                    }
                }
            }
        }
        return success;
    }
}
