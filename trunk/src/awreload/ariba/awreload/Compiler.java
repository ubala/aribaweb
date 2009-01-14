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

    $Id: //ariba/platform/ui/awreload/ariba/awreload/Compiler.java#6 $
*/
package ariba.awreload;

import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.Fmt;
import ariba.util.core.SystemUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ClassUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;

public class Compiler
{

        /**
        Compile the class for this component.  Only called while debugging

        @return In an ideal world, I would parse the output of the shell script
            to determine if the compile was successful.  Or even better, compilation
            would be done in-process through a compiler library.  For now
            this always returns true.
    */
    public static boolean compile (AWResource source)
    {
        String fullURL = source.fullUrl();
            // get dir
        String dir = fullURL.substring(0,fullURL.length() - source.name().length());
            // remove starting "file:/"
        dir = dir.substring(6);
        if (!SystemUtil.isWin32()) dir = "/".concat(dir);
            // replace trailing ".java" with ".class"

        JavaReloadClassLoader loader = (JavaReloadClassLoader) ClassUtil.getClassFactory();
        if (loader._IsAntBuild) {
            antBuild(dir, source.name());
        } else {
            gnuMake(dir, source.name());
        }
        return true;
    }

    /*
        use this internal script to kick off the build of this class.
    */
    private static void antBuild (String dir, String fileName)
    {
        String antExe = SystemUtil.isWin32() ? "ant.bat" : "ant";
        String antHome = AWUtil.getenv("ANT_HOME");
        if (!StringUtil.nullOrEmptyString(antHome)) antExe = (new File(new File(antHome, "bin"), antExe)).toString();
                
        String[] cmdArray = new String[] { antExe,
                "-emacs", "-logger", "org.apache.tools.ant.NoBannerLogger",
                "-f", new File(dir, "build.xml").toString(),
                "compile-file", "-Dfile=" + fileName };
        runCmd(cmdArray);
    }

    /*
        use this internal script to kick off the build of this class.
    */
    private static void gnuMake (String dir, String fileName)
    {
        String target = Fmt.S("%s.class",fileName.substring(0, fileName.length()-5));
        target = target.replace('/','.');

        // String cmd = Fmt.S("sh internal/gnumake.sh \"%s\" %s",dir,fileName);
        String[] cmdArray = new String[] { "sh", "internal/gnumake.sh", dir, target };
        runCmd(cmdArray);
    }

    /*
        use this internal script to kick off the build of this class.
    */
    private static void runCmd (String[] cmdArray)
    {
        try {
            // String cmd = Fmt.S("sh internal/gnumake.sh \"%s\" %s",dir,fileName);
            // Process p = Runtime.getRuntime().exec(cmd);
            Process p = Runtime.getRuntime().exec(cmdArray);

                // getInputStream gives an Input stream connected to
                // the process p's standard output (and vice versa). We use
                // that to construct a BufferedReader so we can readLine() it.
            BufferedReader is =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = null;
            while ((line = is.readLine()) != null) {
                    // show the programmer what's going on on the console.
                Console.println(line);
            }
        }
        catch (IOException ioe) {
            Console.println(ioe.toString());
        }
        catch (Throwable t) {
            Console.println("Exception thrown running build command: " + t.toString());
        }
    }
}
