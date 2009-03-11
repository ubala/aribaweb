/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.
    
    $Id: //ariba/platform/util/core/ariba/util/core/SecurityHelper.java#2 $
    
    Responsible: bjegerlehner
*/
package ariba.util.core;

/**
    Very poor stop-gap measure to prevent bogus access to some vital stuff.


    @aribaapi private

*/

public class SecurityHelper
{
    static final String[] tainted =
            new String[] {
                    "java.lang.System",
                    "java.lang.Runtime",
                    "java.lang.ProcessBuilder",
                    "java.lang.reflect",
                    "java.lang.instrument",
                    "java.lang.Compiler",
                    "java.lang.Class",
                    "java.lang.Thread",
                    "javax",
                    "com",
                    "org",
                    "java.io",
                    "java.nio",
                    "java.security",
                    "java.net",
                    "ariba.util.io.Exec",
                    "ariba.util.core.SystemUtil",
                    "ariba.util.core.MasterPasswordClient"
            };

    public static void validateUnscriptedCaller ()
    {
        StackTraceElement[] el = Thread.currentThread().getStackTrace();

        for (int i = 0;i < el.length;i++) {
            if (el[i].getClassName() != null &&
                (el[i].getClassName().indexOf("javascript") >= 0 ||
                 el[i].getClassName().indexOf("ariba.util.expr.") >= 0)) {
                throw new RuntimeException("Illegal access");
            }
        }
    }

    public static boolean isScriptableClass (String className)
    {
        if (className == null) {
            return true;
        }
        for (int i = 0;i < tainted.length;i++) {
            if (className.startsWith(tainted[i])) {
                return false;
            }
        }

        return true;
    }
}
