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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/idea/lang/util/AWPathUtil.java $
*/
package ariba.ideplugin.idea.lang.util;

import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;

public class AWPathUtil
{
    public static final String LIB_NAME = "AribaWeb";
    public static final String AW_FILE_EXTENSION = ".awl";
    public static final String AW_HOME = "AW_HOME";
    public static final String A_PREFIX = "a";
    public static final String T_PREFIX = "t";
    public static final String W_PREFIX = "w";
    public static final String AW_W_URI = getAribaWebHome() + "/docs/xsd/w-api.xsd";
    public static final String AW_A_URI = getAribaWebHome() + "/docs/xsd/a-api.xsd";
    public static final String AW_T_URI = getAribaWebHome() + "/docs/xsd/t-api.xsd";

    public static String getAribaWebHome ()
    {
        PathMacros macros = PathMacros.getInstance();
        return macros.getValue(AW_HOME);
    }

    public static String getPrefixForNameSpace (String ns)
    {
        return AWPathUtil.AW_A_URI.equals(ns) ? A_PREFIX:
                AWPathUtil.AW_W_URI.equals(ns) ? W_PREFIX:
                        AWPathUtil.AW_T_URI.equals(ns) ? T_PREFIX: "";
    }

    public static boolean isAribaWebFile (PsiFile file)
    {
        if (!(file instanceof XmlFile)) {
            return false;
        }

        String name = file.getName();
        return name.endsWith(AW_FILE_EXTENSION);
    }

    public static boolean isAWProject (Project project)
    {
        return ProjectLibraryTable.getInstance(project).getLibraryByName(LIB_NAME) !=
                null;
    }
}
