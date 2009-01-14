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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/DemoShellUtil.java#8 $
*/
package ariba.ui.demoshell;

import ariba.util.core.StringUtil;
import java.util.List;
import ariba.util.core.ListUtil;

import java.io.File;
import java.io.IOException;

import ariba.ui.aribaweb.util.AWGenericException;

public class DemoShellUtil
{
    public static String pathByRemovingLastComponent (String path) {
        String prependStr = "";
        path = path.replace('\\', '/');

        if (path.startsWith("//")) {
            prependStr = "//";
        } else if (path.startsWith("/")) {
            prependStr = "/";
        }

        // FIX ME: Slow -- plus, this must exist elsewhere!?!
        List stringList =
            StringUtil.stringToStringsListUsingBreakChars(path, "/");
            // remove classes/ariba.awsample.zip
        ListUtil.removeLastElement(stringList);
        return prependStr + StringUtil.join(stringList, File.separator);
    }

    static String removeSuffix (String string, String suffix)
    {
        if (string.endsWith(suffix)) {
            int len = string.length();
            string = string.substring(0, len - suffix.length());
        }
        return string;
    }

    /**
     * Given a parent directory and a child directory path, returns a path for the child
     * relative to the parent.
     *
     * Returns null if child is not prefix of parent.
     */
    public static String relativePath (String parentPath, String childPath)
    {
        String canonicalChildPath;
        String canonicalParentPath;
        String relativePath = null;

        try {
            canonicalChildPath = new File(childPath).getCanonicalPath();
            canonicalParentPath = new File(parentPath).getCanonicalPath();
        } catch (IOException e) {
            throw new AWGenericException(e);
        }

        if (canonicalChildPath.startsWith(canonicalParentPath)) {
            relativePath = canonicalChildPath.substring(canonicalParentPath.length());

            // use "/" as separator
            relativePath = relativePath.replace('\\', '/');

            // ensure no leading "/" on relative path
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
        }

        return relativePath;
    }
}
