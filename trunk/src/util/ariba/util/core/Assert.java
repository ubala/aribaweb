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

    $Id: //ariba/platform/util/core/ariba/util/core/Assert.java#9 $
*/

package ariba.util.core;

import ariba.util.log.Log;

/**
    This class defines and implements various Assert methods

    @aribaapi documented
*/
public final class Assert
{
    /* prevent people from creating instances of this class */
    private Assert ()
    {
    }
    
    /**
     * Helper method equivalent to Assert.that(false, message)
     * @param message the message to log
     * @aribaapi ariba
     */
    public static void fail (String message)
    {
        assertFatal(message);
    }
    
    /**
     * Helper method equivalent to Assert.that(false, message, args)
     * @param fmt control string for the error message
     * @param args the array of arguments to the format string for the
     * error message
     * @aribaapi ariba
     */
    public static void fail (String fmt, Object... args)
    {
        assertFatal(Fmt.S(fmt, args));
    }
    
    /**
     * Wraps a throwable into an assertion.
     * @param t the trowable to wrap
     * @param message the message printed with the assertion
     * @aribaapi private
     */
    public static void fail (Throwable t, String message)
    {
        assertFatal(message, t);
    }
    
    /**
     * Wraps a throwable into an assertion.
     * @param t the trowable to wrap
     * @param fmt control string for the error message
     * @param args the array of arguments to the format string for the
     * error message
     * @aribaapi private
     */
    public static void fail (Throwable t, String fmt, Object... args)
    {
        assertFatal(Fmt.S(fmt, args), t);
    }
    
    
    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged and a FatalAssertionException is thrown.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param string message to log with the error

        @exception FatalAssertionException if the invariant <b>b</b>
        is false
        @aribaapi documented
    */
    public static void that (boolean b, String string)
    {
        if (!b) {
            assertFatal(string);
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged and a FatalAssertionException is thrown.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param a1 the first argument to the format string for the
        error message.

        @exception FatalAssertionException if the invariant <b>b</b>
        is false

        @see ariba.util.core.Fmt#S(String, Object)
        @aribaapi documented
    */
    public static void that (boolean b, String fmt, Object a1)
    {
        if (!b) {
            assertFatal(Fmt.S(fmt, a1));
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged and a FatalAssertionException is thrown.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param a1 the first argument to the format string for the
        error message.
        @param a2 the second argument to the format string for the
        error message.

        @exception FatalAssertionException if the invariant <b>b</b>
        is false

        @see ariba.util.core.Fmt#S(String, Object, Object)
        @aribaapi documented
    */
    public static void that (boolean b, String fmt, Object a1, Object a2)
    {
        if (!b) {
            assertFatal(Fmt.S(fmt, a1, a2));
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged and a FatalAssertionException is thrown.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param a1 the first argument to the format string for the
        error message.
        @param a2 the second argument to the format string for the
        error message.
        @param a3 the third argument to the format string for the
        error message.

        @exception FatalAssertionException if the invariant <b>b</b>
        is false

        @see ariba.util.core.Fmt#S(String, Object, Object)
        @aribaapi documented
    */
    public static void that (boolean b, String fmt, Object a1, Object a2,
                               Object  a3)
    {
        if (!b) {
            assertFatal(Fmt.S(fmt, a1, a2, a3));
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged and a FatalAssertionException is thrown.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param a1 the first argument to the format string for the
        error message.
        @param a2 the second argument to the format string for the
        error message.
        @param a3 the third argument to the format string for the
        error message.
        @param a4 the fourth argument to the format string for the
        error message.

        @exception FatalAssertionException if the invariant <b>b</b>
        is false

        @see ariba.util.core.Fmt#S(String, Object, Object)
        @aribaapi documented
    */
    public static void that (boolean b, String fmt, Object a1, Object a2,
                               Object  a3, Object a4)
    {
        if (!b) {
            assertFatal(Fmt.S(fmt, a1, a2, a3, a4));
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged and a FatalAssertionException is thrown.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param a1 the first argument to the format string for the
        error message.
        @param a2 the second argument to the format string for the
        error message.
        @param a3 the third argument to the format string for the
        error message.
        @param a4 the fourth argument to the format string for the
        error message.
        @param a5 the fifth argument to the format string for the
        error message.

        @exception FatalAssertionException if the invariant <b>b</b>
        is false

        @see ariba.util.core.Fmt#S(String, Object, Object)
        @aribaapi documented
    */
    public static void that (boolean b, String fmt, Object a1, Object a2,
                               Object  a3, Object a4, Object a5)
    {
        if (!b) {
            assertFatal(Fmt.S(fmt, a1, a2, a3, a4, a5));
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged and a FatalAssertionException is thrown.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param args the array of arguments to the format string for the
        error message.

        @exception FatalAssertionException if the invariant <b>b</b>
        is false

        @see ariba.util.core.Fmt#S(String, Object)
        @aribaapi documented
    */
    public static void that (boolean b, String fmt, Object[] args)
    {
        if (!b) {
            assertFatal(Fmt.S(fmt, args));
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param string message to log with the error
        @aribaapi documented
    */
    public static void assertNonFatal (boolean b, String string)
    {
        if (!b) {
            assertNonFatal(string);
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param arg the first argument to the format string for the
        error message.
        @aribaapi documented
    */
    public static void assertNonFatal (boolean b, String fmt, Object arg)
    {
        if (!b) {
            assertNonFatal(Fmt.S(fmt, arg));
        }
    }

    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param arg1 the first argument to the format string for the
        error message.
        @param arg2 the second argument to the format string for the
        error message.
        
        @aribaapi documented
    */
    public static void assertNonFatal (boolean b, String fmt, Object arg1,
                                       Object arg2)
    {
        if (!b) {
            assertNonFatal(Fmt.S(fmt, arg1, arg2));
        }
    }
    
    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param arg1 the first argument to the format string for the
        error message.
        @param arg2 the second argument to the format string for the
        error message.
        @param arg3 the third argument to the format string for the
        error message.
        
        @aribaapi documented
    */
    public static void assertNonFatal (boolean b, String fmt, Object arg1,
                                       Object arg2, Object arg3)
    {
        if (!b) {
            assertNonFatal(Fmt.S(fmt, arg1, arg2, arg3));
        }
    }
    
    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param arg1 the first argument to the format string for the
        error message.
        @param arg2 the second argument to the format string for the
        error message.
        @param arg3 the third argument to the format string for the
        error message.
        @param arg4 the fourth argument to the format string for the
        error message.
        
        @aribaapi documented
    */
    public static void assertNonFatal (boolean b, String fmt, Object arg1,
                                       Object arg2, Object arg3, Object arg4)
    {
        if (!b) {
            assertNonFatal(Fmt.S(fmt, arg1, arg2, arg3,arg4));
        }
    }
    
    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param arg1 the first argument to the format string for the
        error message.
        @param arg2 the second argument to the format string for the
        error message.
        @param arg3 the third argument to the format string for the
        error message.
        @param arg4 the fourth argument to the format string for the
        error message.
        @param arg5 the fifth argument to the format string for the
        error message.
        
        @aribaapi documented
    */
    public static void assertNonFatal (boolean b, String fmt, Object arg1,
                                       Object arg2, Object arg3, Object arg4,
                                       Object arg5)
    {
        if (!b) {
            assertNonFatal(Fmt.S(fmt, arg1, arg2, arg3,arg4, arg5));
        }
    }
    
    
    
    /**
        Standard assertion method for declaring invariants. If b is
        false, an error message including the specified string is
        logged.

        @param b <b>true</b> if the invariant is true, <b>false</b>
        otherwise
        @param fmt control string for the error message as used in
        Fmt.S
        @param args the arguments to the format string for the
        error message.
        @aribaapi documented
    */
    public static void assertNonFatal (boolean b, String fmt, Object[] args)
    {
        if (!b) {
            assertNonFatal(Fmt.S(fmt, args));
        }
    }
    
    private static void assertNonFatal (String message)
    {
        assertNonFatal(message, null);
    }
    
    private static void assertNonFatal (String message, Throwable t)
    {
        Log.util.warning(2811,
                         message,
                         Thread.currentThread().getName(),
                         ThreadDebugState.makeString(),
                         t != null ? SystemUtil.stackTrace(t) : SystemUtil.stackTrace());
    }

    private static void assertFatal (String message)
    {
        assertFatal(message, null);
    }
    
    private static void assertFatal (String message, Throwable t)
    {
        assertNonFatal(message, t);
        FatalAssertionException e = new FatalAssertionException(message);
        if (t != null) {
            e.initCause(t);
        }
        throw e; 
    }
}
