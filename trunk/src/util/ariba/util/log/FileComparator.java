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

    $Id: //ariba/platform/util/core/ariba/util/log/FileComparator.java#5 $
*/

package ariba.util.log;

import ariba.util.core.Compare;
import java.io.File;

class FileComparator implements Compare
{
    static final FileComparator self = new FileComparator();

    private FileComparator ()
    {
    }

    public int compare (Object o1, Object o2)
    {
        File left =(File)o1;
        File right =(File)o2;
        long difference = left.lastModified() - right.lastModified();

            // wasn't sure how long to int casting works so I did this
            // the safe way:
        if (difference < 0) {
            return -1;
        }
        if (difference > 0) {
            return 1;
        }

        return 0;
    }
}
