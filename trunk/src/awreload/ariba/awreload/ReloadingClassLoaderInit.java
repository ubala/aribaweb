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

    $Id: //ariba/platform/ui/awreload/ariba/awreload/ReloadingClassLoaderInit.java#14 $
*/

package ariba.awreload;

import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.ClassUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.Fmt;
import ariba.util.core.SystemUtil;
import ariba.util.core.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ReloadingClassLoaderInit
{

    /**
     * Initialization -- Called by AWConcreteServerApplication.init()
     */
    public static void initClassReloading (String searchPath)
    {
        String reloading = SystemUtil.getenv("ARIBA_AWRELOAD");
        String instrumentation = SystemUtil.getenv("ARIBA_JVM_INSTRUMENTATION");
        if ("true".equals(reloading) || "AWReload".equals(instrumentation)) {

            try {
                JavaReloadClassLoader loadGroupClassLoader =
                        new JavaReloadClassLoader();

                AWUtil.setClassLoader(loadGroupClassLoader);
                ClassUtil.setClassFactory(loadGroupClassLoader);
            }
            catch (NoClassDefFoundError e) {
                // UI packages weren't found.  May be doing basegen.
            }
        }
    }

    private static List findPackages (String additionalPath)
    {
        String path = SystemUtil.getenv("ARIBA_AW_SEARCH_PATH");
        if (StringUtil.nullOrEmptyOrBlankString(path) && (additionalPath != null)) {
            path = additionalPath;
        }
        if (!StringUtil.nullOrEmptyOrBlankString(path)) {
            List paths = StringUtil.stringToStringsListUsingBreakChars(path,";");
            if (!ListUtil.nullOrEmptyList(paths)) {
                List packages = ListUtil.list();
                for (int i = 0; i < paths.size(); i++) {
                    try {
                        String s = (String)paths.get(i);
                        File dir = FileUtil.directory(s);
                        doDir(dir, "", packages);
                    }
                    catch (IOException ioe) {
                        Console.println(ioe.toString());
                    }
                }
                return packages;
            }
        }
        return ListUtil.list();
    }

    private static void doDir (File dir, String packagePath, List packages)
    {
        File[] files = dir.listFiles();
        boolean foundAWL = false;
        if (files != null) {
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                if (file.isDirectory()) {
                    String nextPkg = ("".equals(packagePath)) ? file.getName() :
                            Fmt.S("%s.%s",packagePath, file.getName());
                    doDir(file, nextPkg, packages);
                }
                else if (file.getName().toLowerCase().endsWith(".awl")) {
                    if (! foundAWL) {
                        foundAWL = true;
                        packages.add(packagePath);
                    }
                }
            }
        }
    }
}
