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

    $Id: //ariba/platform/util/core/ariba/util/log/Directory.java#9 $
*/

package ariba.util.log;

import ariba.util.core.ListUtil;
import ariba.util.core.Sort;
import java.io.File;
import java.util.List;

class Directory extends File
{
    public Directory (File path)
    {
        super(path,"");
    }

    public Directory (String path)
    {
        super(path);
    }

    public Directory (String path, String file)
    {
        super(path, file);
    }

    public Directory (File path, String file)
    {
        super(path, file);
    }

    private List listing;

    public List listing ()
    {
        return this.listing;
    }

    public File fileAt (int index)
    {
        if (index >= this.listing.size()) {
            return null;
        }
        return (File)this.listing.get(index);
    }

    private long sumBytes = 0;

    public int sumMegaBytes ()
    {
        return (int)(this.sumBytes / BytesPerMegaByte);
    }

    public long sumBytes ()
    {
        return this.sumBytes;
    }

    public boolean instantiate ()
    {
        if (!this.exists()) {
            if (!this.mkdirs()) {
                return false;
            }
        }
        return true;
    }

    public void LogError (String message)
    {
        Log.util.error(2758, message, this.getAbsolutePath());
    }

    static final long BytesPerMegaByte = 1048576;

    public boolean scan ()
    {
            // first scan the directory
        if (!this.exists() || !this.isDirectory()) {
            return false;
        }
            // otherwise list will fail...as in crash which opens a
            // small timing window for failure in which the time
            // between checking for existence and scanning a directory
            // delete or network failure could occur

        this.listing = ListUtil.list();

        this.sumBytes = listFiles(this, this.listing);

        return true;
    }

    private static int listFiles (File directory, List listing)
    {
        String [] directoryList = directory.list();

        if (directoryList.length == 0) {
            return 0;
        }
            // for each file copy the computed data from the File object
            // into our File object

        int sumBytes = 0;
        for (int idx = 0; idx < directoryList.length; idx++)
        {
            File cursorFile = new File(directory, directoryList[idx]);
            if (cursorFile.isDirectory()) {
                sumBytes += listFiles(cursorFile, listing);
            }
            else {
                listing.add(cursorFile);
                sumBytes += cursorFile.length();
            }
        }
        return sumBytes;
    }

    public void sortOnDate ()
    {
        Object[] items = listing.toArray();
        Sort.objects(items,
                     FileComparator.self);
        listing = ListUtil.arrayToList(items);
    }
}
