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

    $Id: //ariba/platform/util/core/ariba/util/io/FilenameExtensionFilter.java#5 $
*/

package ariba.util.io;

import java.util.List;
import ariba.util.core.ListUtil;
import java.io.File;
import java.io.FilenameFilter;

/**
    A FilenameFilter to pick all files that end with a extension (case
    insensitive match).

    @aribaapi private
*/
public class FilenameExtensionFilter implements FilenameFilter
{
    List extensions;

    public FilenameExtensionFilter (String extension)
    {
        this.extensions = ListUtil.list(extension.toLowerCase());
    }

    /**
        @param source - a List of strings, each a filename extension
    */
    public FilenameExtensionFilter (List source)
    {
        int sourceSize = source.size();
        this.extensions = ListUtil.list(sourceSize);
        for (int idx = 0; idx < sourceSize; idx++) {
            ListUtil.addElementIfAbsent(this.extensions,
                ((String)source.get(idx)).toLowerCase());
        }
    }

    public boolean accept (File dir, String name)
    {
        if (name == null) {
            return false;
        }
        int extensionsSize = this.extensions.size();
        for (int idx = 0; idx < extensionsSize; idx++) {
            String cursor = (String)this.extensions.get(idx);
            if (name.toLowerCase().endsWith(cursor)) {
                return true;
            }
        }
        return false;
    }
}
