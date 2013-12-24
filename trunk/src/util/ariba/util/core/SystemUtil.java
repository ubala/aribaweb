/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/SystemUtil.java#39 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import ariba.util.shutdown.ShutdownManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
    System Utilities. These are helper functions for dealing with
    system functions.

    @aribaapi documented
*/
public final class SystemUtil
{
    /*
        No constructor since no non-static methods.
    */
    private SystemUtil ()
    {
    }

    /*
        Static initialization of the ariba, config, install, and internal directories
    */
    static String SystemDirectoryString;
    private static File SystemDirectory;
    static String ConfigDirectoryString;
    private static File ConfigDirectory;
    private static URL ConfigURL;
    private static File LocalTempDirectory;
    private static File SharedTempDirectory;
    private static String InternalDirectoryString = "internal";
    private static File InternalDirectory;
    private static File InstallDirectory;
    private static boolean IsDevelopment;

    static
    {
            // setSystemDirectory and setConfigDirectory calls were moved
            // from here to avoid logging initialization recursion.
            // See assureSystemDirectory and assureConfigDirectory.
        InstallDirectory = getCwdFile();
        InternalDirectory = new File(InstallDirectory, InternalDirectoryString);
        IsDevelopment = hasInternalDirectory();
    }


    public static void sleep (long sleepTime)
    {
        if (sleepTime <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepTime);
        }
        catch (InterruptedException e) {
            consumeInterruptedException(e);
        }
    }

    /**
        Check if two objects are equal in a null safe manner.

        @param one the first of two objects to compare
        @param two the second of two objects to compare

        @return <b>true</b> if the two objects <B>one</B> and
        <B>two</B> are equals according to the equals() method of
        <B>one</B> or if they're both null. <b>false</b> is returned
        otherwise.
        @aribaapi documented
    */
    public static boolean equal (Object one, Object two)
    {
        if (one != null) {
            return one.equals(two);
        }
        return (two == null);
    }

    /**
        Convenience method that returns the <code>hashCode()</code> of
        <code>object</code>. The case of a <code>null</code>
        <code>object</code> is handled; zero is returned. <p/>

        This method is complementary to the method {@link #equal} which tests two
        objects for equality in a <code>null</code> safe manner. <p/>

        @param object the object for which to return the hashcode
        @return the hashcode value or <code>0</code> if <code>object</code> is
                <code>null</code>
        @aribaapi ariba
    */
    public static int hashCode (Object object)
    {
        return object != null ? object.hashCode() : 0;
    }

    /**
        Check if two objects are equal using a deeper way to
        determine their equality.
        The &quot;deeper way &quot; is using
        ListUtil.listEquals for list objects,
        MapUtil.mapEquals for map objects,
        and ArrayUtil.arrayEquals for array objects

        @param one the first of two objects to compare
        @param two the second of two objects to compare
        @return <b>true</b> if the two objects are equal as defined
                above
        @aribaapi private
    */
    static boolean objectEquals (Object one, Object two)
    {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        if ((one instanceof Map) && (two instanceof Map)) {
            return MapUtil.mapEquals((Map)one,(Map)two);
        }
        else if ((one instanceof List) && (two instanceof List)) {
            return ListUtil.listEquals((List)one, (List)two);
        }
        else if ((one instanceof Object[]) && (two instanceof Object[])) {
            return ArrayUtil.arrayEquals((Object[])one, (Object[])two);
        }
        else {
            return one.equals(two);
        }
    }

    /**
        Compares <code>first</code> to <code>second</code> and returns a
        negative, zero or postive integer as <code>first</code> is
        less than, equal to or greater than <code>second</code>,
        respectively. <p>

        <code>first</code> and <code>second</code> may be <code>null</code>.
        A <code>null</code> object is always considered to be less than
        any non-<code>null</code> object. <p>

        @param first the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @param second the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @return the result of the comparison of <code>first</code>
                and <code>second</code>
        @aribaapi ariba
    */
    public static int compare (Comparable first, Comparable second)
    {
        return compare(first, second, false);
    }

    /**
        Compares <code>first</code> to <code>second</code> and returns a
        negative, zero or postive integer as <code>first</code> is
        less than, equal to or greater than <code>second</code>,
        respectively. <p>

        <code>first</code> and <code>second</code> may be <code>null</code>.
        A <code>null</code> object is always considered to be less than
        any non-<code>null</code> object. <p>

        @param first the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @param second the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @return the result of the comparison of <code>first</code>
                and <code>second</code>
        @aribaapi ariba
    */
    public static int compare (
            Comparable first,
            Comparable second,
            boolean nullComparesGreaterThanNonNull
    )
    {
        int result = 0;
        if (first != null) {
            if (second != null) {
                return first.compareTo(second);
            }
            result = +1;
        }
        else if (second != null) {
            result = -1;
        }
        return nullComparesGreaterThanNonNull ? -result : result;
    }

    /**
        Compares <code>first</code> to <code>second</code> using the supplied
        <code>comparator </code>and returns a negative, zero or postive integer
        as <code>first</code> is less than, equal to or greater than
        <code>second</code>, respectively. <p>

        <code>first</code> and <code>second</code> may be <code>null</code>.
        A <code>null</code> object is always considered to be less than
        any non-<code>null</code> object. <p>

        @param first the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @param second the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @param comparator the <code>Comparator</code> to use when comparing, may
               not be <code>null</code>
        @return the result of the comparison of <code>first</code>
                and <code>second</code>
        @aribaapi ariba
    */
    public static int compare (Object first, Object second, Comparator comparator)
    {
        return compare(first, second, comparator, false);
    }

    /**
        Compares <code>first</code> to <code>second</code> using the supplied
        <code>comparator </code>and returns a negative, zero or postive integer
        as <code>first</code> is less than, equal to or greater than
        <code>second</code>, respectively. <p>

        <code>first</code> and <code>second</code> may be <code>null</code>.
        A <code>null</code> object is always considered to be less than
        any non-<code>null</code> object. <p>

        @param first the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @param second the first <code>Comparable</code> to compare,
               may be <code>null</code>
        @param comparator the <code>Comparator</code> to use when comparing
        @return the result of the comparison of <code>first</code>
                and <code>second</code>
        @aribaapi ariba
    */
    public static int compare (
            Object first,
            Object second,
            Comparator comparator,
            boolean nullComparesGreaterThanNonNull
    )
    {
        int result = 0;
        if (first != null) {
            if (second != null) {
                if (comparator != null) {
                    return comparator.compare(first, second);
                }
                return ((Comparable)first).compareTo(second);
            }
            result = +1;
        }
        else if (second != null) {
            result = -1;
        }
        return nullComparesGreaterThanNonNull ? -result : result;
    }

    /**
        Compares <code>first</code> and <code>second</code> and returns the
        result. <p/>

        If <code>falseLessThanTrue == true</code> then <code>-1</code> is
        returned if <code>!first && second</code> and <code>+1</code> is
        returned if <code>first && !second</code> and <code>0</code>
        when <code>first == second</code>. <p/>

        If <code>falseLessThanTrue == false</code> the result is the opposite.
        <p/>

        @param first the first of the two values to compare
        @param second the second of the two values to compare
        @param falseLessThanTrue whether or not <code>false</code> should be
                considered less than <code>true</code>
        @return the result
        @aribaapi ariba
    */
    public static int compare (
            boolean first,
            boolean second,
            boolean falseLessThanTrue
    )
    {
        int result = (first == second) ? 0 : (second ? -1 : +1);
        return falseLessThanTrue ? result : -result;
    }

    /**
        Convenience function that calls {@link #compare(boolean,boolean,boolean)}
        with <code>falseLessThanTrue == true</code>. <p/>

        @aribaapi ariba
    */
    public static int compare (boolean first, boolean second)
    {
        return compare(first, second, true);
    }

    /**
        If ExitException is true, SystemUtil.exit will throw an
        ExitException instead of calling System's exit() method. This
        is currently for the uses of the TestHarness which doesn't
        want the process to really exit.

        @aribaapi private
        @see ariba.util.core.SystemUtil#setExitException
    */
    private static boolean ExitException;

    /**
        Used for unit tests to cause SystemUtil.exit() to thrown an
        exception rather than bring down the process.

        @aribaapi private
    */
    public static void setExitException (boolean value)
    {
        ExitException = value;
    }

    /**
        Used for unit tests to check if an exception will be thrown
        when SystemUtil.exit() is calld.

        @aribaapi private
    */
    public static boolean getExitException ()
    {
        return ExitException;
    }

    /**
        All our unit test classes starts with this prefix.
    */
    private static final String EligibleClassPrefix = "test.ariba.";

    /**
        Checks to make sure we are the specified class and method are
        invoked from unit tests. If not, will Assert. This method is
        useful if there are methods that should be called by our unit
        tests. It makes sure no other code can call the given class/method.
        @param className the class name
        @param methodName the method name
        @aribaapi ariba
    */
    public static final void checkCalledFromUnitTest (String className,
                                                      String methodName)
    {
        StackTraceElement[] callStack = (new Throwable()).getStackTrace();
        for (int i=0; i<callStack.length; i++) {
            if (callStack[i].getClassName().startsWith(EligibleClassPrefix)) {
                return;
            }
        }
        if (StringUtil.nullOrEmptyOrBlankString(methodName)) {
            Assert.that(false, "You are not allowed to invoke %s", className);
        }
        else {
            Assert.that(false, "You are not allowed to invoke %s.%s",
                        className, methodName);
        }

    }

    /**
        Terminate the java process. All shutdown hooks which are
        registered by components in the server will be run.

        @param code the exit code to pass to System.exit()
        @aribaapi documented
    */
    public static void exit (int code)
    {
        if (getExitException()) {
            try {
                flushOutput();
            }
            catch (Throwable e) { // OK
                    // don't want to allow problem in flushOutput to
                    // cause a stack unwind
            }
            throw new ExitException(code);
        }
        else {
            ShutdownManager.forceShutdown(code);
        }
    }

    /**
        Compares <B>len</B> bytes in array <B>a</B> starting with byte
        <B>aIndex</B> with <B>len</B> bytes in array <B>b</B> starting with
        byte <B>bIndex</B>.

        @param a array of memory to compare with b
        @param aIndex where to start compare in array a
        @param b array of memory to compare with a
        @param bIndex where to start compare in array b
        @param len the number of bytes to compare.

        @return <b>true</b> if each byte compared is equal,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean memoryCompare (byte[] a, int aIndex,
                                         byte[] b, int bIndex, int len)
    {
        Assert.that((aIndex >= 0) && (bIndex >= 0),
                    "both indexes must be greater or equal to 0.");
        if ((a.length - aIndex < len) || (b.length - bIndex < len)) {
            return false;

        }

        for (int aTotal = aIndex + len; aIndex < aTotal; aIndex++, bIndex++) {
            if (a [aIndex] != b [bIndex]) {
                return false;
            }
        }

        return true;
    }
    /**
        Compute the local host name and cache it.

        We don't initialize 'hostname' in the declaration since it can
        trigger security exceptions in the client.  It should only be
        initialized as needed.
    */
    private static String HOST_NAME = null;

    /**
        Get the hostname of this machine. If you are in the server,
        you should use Server.hostname() which consults the parameters
        in case they override the name the OS returns

        @return the hostname for this machine

        @see ariba.rpc.server.Server#hostname()

        @aribaapi private
    */
    public static String getHostname ()
    {
        if (HOST_NAME == null) {
            HOST_NAME = getHost().getHostName();
        }
        return HOST_NAME;
    }

    private static InetAddress HOST = null;

    /**
        Get the InetAddress for this machine.

        @return the InetAddress for this machine
        @aribaapi documented
    */
    public static InetAddress getHost ()
    {
        if (HOST == null) {
            HOST = setupHost();
        }
        return HOST;
    }

    private static InetAddress setupHost ()
    {
        try {
            return InetAddress.getLocalHost();
        }
        catch (UnknownHostException e1) {
            Log.util.error(2767, e1);
            try {
                    // should return 127.0.0.1 w/o exception...
                return InetAddress.getByName(null);
            }
            catch (UnknownHostException e2) {
                Log.util.error(2768, e2);
                return null;
            }
        }
    }


    /**
        helper function to convert hostname into int for database
        storage

        @aribaapi private
    */
    public static int hostAsInt ()
    {
        InetAddress tmpHost = getHost();
        byte [] hostAsBytes = tmpHost.getAddress();

        int bytes = hostAsBytes[0];
        bytes = bytes << 8;
        bytes = bytes + hostAsBytes[1];
        bytes = bytes << 8;
        bytes = bytes + hostAsBytes[2];
        bytes = bytes << 8;
        bytes = bytes + hostAsBytes[3];
        return bytes;
    }
    /**
        Find the current working directory.

        @return the present working directory of this VM
        @aribaapi documented
    */
    public static String getPwd ()
    {
        return System.getProperty("user.dir");
    }

    /**
        Get the current working directory as a String.

        @return current working directory as a String
        @aribaapi documented
    */
    public static String getCwd ()
    {
        return getCwdFile().getAbsolutePath();
    }

    /**
        Get the current working directory as a File

        @return current working directory as a File
        @aribaapi documented
    */
    public static File getCwdFile ()
    {
        //return new File("./");
        String installDir = System.getProperty("ariba.server.home");
        return new File(installDir != null ? StringUtil.strcat(installDir,"/./") : "./");
    }

    /**
        Return the default file encoding of this VM
        @return the default file encoding of this VM
        @aribaapi documented
    */
    public static String getFileEncoding ()
    {
        return System.getProperty("file.encoding");
    }

    /**
        Return the class path of this VM.  See also bootClassPath()
        @return the class path of this VM
        @aribaapi documented
    */
    public static String getClassPath ()
    {
        return System.getProperty("java.class.path");
    }

    /**
        Returns the boot classpath of this VM.  This is only really
        relevant for the Sun VM.  There is a switch you can use to
        start the VM that sets the bootClassPath instead of the
        classpath.

        @return the boot classpath of this VM
        @aribaapi documented
    */
    public static String getBootClassPath ()
    {
        return System.getProperty("sun.boot.class.path");
    }

    /**
        Returns the complete classpath used by this VM. It contains
        the boot classpath as well as the class path.
    */
    public static String getCompleteClassPath ()
    {
        String bootClassPath = SystemUtil.getBootClassPath();
        String classPath = SystemUtil.getClassPath();
        if (StringUtil.nullOrEmptyString(classPath)) {
            return bootClassPath;
        }
        if (StringUtil.nullOrEmptyString(bootClassPath)) {
            return classPath;
        }
        return StringUtil.strcat(
            bootClassPath, SystemUtil.getPathSeparator(), classPath);
    }

    /**
        Return the path separator of this VM. That is : for Unix and ;
        for Windows.
        @return the path separator of this VM. That is : for Unix and ;
        for Windows.
        @aribaapi documented
    */
    public static String getPathSeparator ()
    {
        return System.getProperty("path.separator");
    }

    /**
        Return the path separator of this VM. That is : for Unix and ;
        for Windows.
        @return the path separator of this VM. That is : for Unix and ;
        for Windows.
        @aribaapi documented
    */
    public static char getPathSeparatorChar ()
    {
        return getPathSeparator().charAt(0);
    }

    /**
        Return the OS architecture of this VM

        @return the OS architecture of this VM
        @aribaapi documented
    */
    public static String getArchitecture ()
    {
        return System.getProperty("os.arch");
    }

    /**
        Return the OS name of this VM
        @return the OS name of this VM
        @aribaapi documented
    */
    public static String getOperatingSystem ()
    {
        return System.getProperty("os.name");
    }

    /**
        Return the OS type of this VM.
        It will return either Win32, SunOS, AIX, HP-UX, or whatever
        getOperatingSystem returns if unknown.
        @return the OS type of this VM
        @aribaapi documented
    */
    public static String getOperatingSystemType ()
    {
        String os = getOperatingSystem();
        if (os.indexOf("Windows") != -1) {
            return "Win32";
        }
        else if (os.indexOf("Solaris") != -1 || os.indexOf("SunOS") != -1) {
            return "SunOS";
        }
        else if (os.indexOf("AIX") != -1) {
            return "AIX";
        }
        else if (os.indexOf("HP") != -1) {
            return "HP-UX";
        }
        else if (os.indexOf("Linux") != -1) {
            return "Linux";
        }
        else {
            return os;
        }
    }

    /**
        Return this VM's vendor
        @return the vendor of this VM
        @aribaapi documented
    */
    public static String getJavaVendor ()
    {
        return System.getProperty("java.vendor");
    }

    /**
        Return the current user name
        @return the current user name
        @aribaapi documented
    */
    public static String getUserName ()
    {
        return System.getProperty("user.name");
    }

    /**
        Determine if the system is a Windows based system.

        @return <b>true</b> if the operating system is Windows 95 or NT or 2000,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static final boolean isWin32 ()
    {
        return (getOperatingSystem().indexOf("Windows") != -1);
    }

    /**
        Determine if the system is a Sun based system.

        @return <b>true</b> if the operating system is Sun OS
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static final boolean isSunOS ()
    {
        return (getOperatingSystem().indexOf("Solaris") != -1) ||
               (getOperatingSystem().indexOf("SunOS") != -1);
    }

    /**
        Determine if the system is a HP based system.

        @return <b>true</b> if the operating system is HP-UX
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static final boolean isHP ()
    {
        return (getOperatingSystem().indexOf("HP") != -1);
    }

    /**
        Determine if the system is a AIX based system.

        @return <b>true</b> if the operating system is AIX
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static final boolean isAIX ()
    {
        return (getOperatingSystem().indexOf("AIX") != -1);
    }


    /**
        Determine if the system is a Linux based system.

        @return <b>true</b> if the operating system is Linux
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static final boolean isLinux ()
    {
        return (getOperatingSystem().indexOf("Linux") != -1);
    }

    /**
        @aribaapi private
    */
    private static BufferedReader IN;

    /**
        @aribaapi private
    */
    private static PrintWriter OUT;

    /**
        @aribaapi private
    */
    private static PrintWriter ERR;

    /**
        Get a Reader version of System.in

        @return a Reader version of System.in

        @aribaapi documented
    */
    public static BufferedReader in ()
    {
        if (IN == null) {
            try {
                IN = IOUtil.bufferedReader(System.in,
                                           IOUtil.getDefaultSystemEncoding());
            }
            catch (UnsupportedEncodingException ex) {
                Assert.that(false, "Unable to use default system encoding %s",
                            ex);
            }
        }
        return IN;
    }

    /**
        Get a Writer version of System.out

        @return a Writer version of System.out
        @aribaapi documented
    */
    public static PrintWriter out ()
    {
        if (OUT == null) {
            try {
                OUT = IOUtil.printWriter(System.out,
                                         IOUtil.getDefaultSystemEncoding());
            }
            catch (UnsupportedEncodingException ex) {
                Assert.that(false, "Unable to use default system encoding %s",
                            ex);
            }
        }
        return OUT;
    }

    /**
        Get a Writer version of System.err

        @return a Writer version of System.err
        @aribaapi documented
    */
    public static PrintWriter err ()
    {
        if (ERR == null) {
            try {
                ERR = IOUtil.printWriter(System.err,
                                         IOUtil.getDefaultSystemEncoding(),
                                         true);
            }
            catch (UnsupportedEncodingException ex) {
                Assert.that(false, "Unable to use default system encoding %s",
                            ex);
            }
        }
        return ERR;
    }

    /**
        Set the output stream returned by SystemUtil.out();
        @aribaapi private
    */
    public static void setOut (PrintWriter pw)
    {
        OUT = pw;
    }

    /**
        Set the output stream returned by SystemUtil.out();
        @aribaapi private
    */
    public static void setErr (PrintWriter pw)
    {
        ERR = pw;
    }

    /**
        Flush system out and system error.
        @aribaapi documented
    */
    public static void flushOutput ()
    {
        if (OUT != null) {
            OUT.flush();
        }
        if (ERR != null) {
            ERR.flush();
        }
    }


    /**
        Prevent a compiler warning when you don't want to do something
        in a catch block.

        The arguments are passed so that at a later point in time we
        could add some kind of logging to see where in our code we use
        this call.

        @param reason text reason for why you don't want to do anything.
        @param t      throwable (usally exception) that was thrown.
    */
    public static final void consumeException (String reason, Throwable t)
    {
        return;
    }

    /**
        Provide proper handling of InterruptedException from wait()
        and sleep() methods.

        @param e      exception that was thrown.
    */
    public static final void consumeInterruptedException (InterruptedException e)
    {
        Log.util.info(2897, stackTrace(e));
            // should this assert? Probably should.
    }


    /**
        Get a string which represents the current call stack.

        @return a string which represents the current call stack
        @aribaapi documented
    */
    public static String stackTrace ()
    {
        return stackTrace(new Exception("Stack trace"));
    }

    /**
        Returns String stackTrace for current thread, reduced to focus on most relevant
        stack frames for analyzing application execution. It removes the tail of the
        stackTrace starting with the ariba.ui.servletadaptor frame, because the details of
        servlet dispatch are boring and not helpful. It elides (replaces with ...) all
        ariba.ui frames (except AWKeyPathBinding), because they are boring and voluminous,
        and almost never helpful for understanding application performance. It elides the
        details of reflexive method invokation under FieldValue_Object.getFieldValue. Etc.
        @aribaapi private
    */
    public static String stackTraceCodePath ()
    {
        return stackTraceCodePath(stackTrace());
    }

    /**
        Returns String stackTrace for given Throwable, reduced to focus on most relevant
        stack frames for analyzing application execution. It removes the tail of the
        stackTrace starting with the ariba.ui.servletadaptor frame, because the details of
        servlet dispatch are boring and not helpful. It elides (replaces with ...) all
        ariba.ui frames (except AWKeyPathBinding), because they are boring and voluminous,
        and almost never helpful for understanding application performance. It elides the
        details of reflexive method invokation under FieldValue_Object.getFieldValue. Etc.
        @aribaapi private
    */
    public static String stackTraceCodePath (Throwable t)
    {
        return stackTraceCodePath(stackTrace(t));
    }

    /**
        Returns String stackTrace reduced to focus on most relevant stack frames for
        analyzing application execution, given a String stackTrace. It removes the tail of
        the stackTrace starting with the ariba.ui.servletadaptor frame, because the
        details of servlet dispatch are boring and not helpful. It elides (replaces with
        ...) all ariba.ui frames (except AWKeyPathBinding), because they are boring and
        voluminous, and almost never helpful for understanding application performance. It
        elides the details of reflexive method invokation under
        FieldValue_Object.getFieldValue.
        @aribaapi private
    */
    public static String stackTraceCodePath (String st)
    {
        // Discard everything through last SystemUtil.stackTrace frame
        st = st.replaceAll(
            "(?s)^.*\tat ariba\\.util\\.core\\.SystemUtil\\.stackTrace.*?\n", "");

        // Discard everything starting with first servletadapter stack frame, boring.
        // It is never interesting to see the internals of servlet dispatching.
        st = st.replaceAll("(?s)at ariba\\.ui\\.servletadaptor\\..*$", "");

        // Discard everything starting with first rpc.server stack frame, boring.
        st = st.replaceAll("(?s)at ariba\\.rpc\\.server\\..*$", "");

        // Discard everything starting with first ScheduledTask.run stack frame, boring.
        st = st.replaceAll(
            "(?s)at ariba\\.util\\.scheduler\\.ScheduledTask\\.run.*$", "");

        // Discard java.lang.Thread.run frame.
        st = st.replaceAll("\tat java\\.lang\\.Thread\\.run.*\n", "");

        // Protect AWKeyPathBinding from removal, good clue of AWL binding code path.
        st = st.replaceAll(
            "ariba\\.ui\\.aribaweb\\.core\\.AWKeyPathBinding",
            "ariba\\.UI\\.aribaweb\\.core\\.AWKeyPathBinding");

        // Elide all other contiguous ariba.ui stack frames, only aribaweb developers can
        // get much from them, and there are often hundreds of them.  Focus on app frames.
        st = st.replaceAll("(?m)(^\\s+at ariba\\.ui\\..*?$)+", "\t...\n");

        // Elide all fieldsui ARPPage frames, don't really help much.
        st = st.replaceAll("\tat ariba\\.htmlui\\.fieldsui\\.ARPPage\\..*\n", "\t...\n");

        // Restore protected ariba.ui... stack frames.
        st = st.replaceAll("ariba\\.UI\\.", "ariba\\.ui\\.");
    
        // Elide seven stack frame block associated with reflexive method invokation under
        // FieldValue_Object.getFieldValue.
        st = st.replaceAll(
            "(?m)(^\\s+at (sun\\.reflect\\.|java\\.lang\\.reflect\\..|" +
            "ariba\\.util\\.fieldvalue\\.ReflectionMethodGetter\\.|" +
            "ariba\\.util\\.fieldvalue\\.FieldPath\\.getFieldValue|" +
            "ariba\\.util\\.fieldvalue\\.FieldValue_Object\\.).*?$)+",
            "\t...\n");

        // Elide all contiguous javascript frames until last one, mozilla and ariba.
        st = st.replaceAll(
            "(\tat org\\.mozilla\\.javascript\\..*?\n)" +
            "(?:\tat (?:org\\.mozilla|ariba\\.util)\\.javascript\\..*?\n)+" +
            "(\tat ariba\\.util\\.javascript\\..*?\n)", "\t...\n$2");

        // ***** Final cleanups ***** 

        // Keep only the first of repeated calls to the same method path, maybe
        // interleaved with ellipsis.
        st = st.replaceAll(
            "(\tat .*?)\\((.*?):(\\d+)\\)\\s*\n" +
            //1       1  (2   2 3    3  )
            "(?:(?:\t\\.\\.\\.\\s*\n)*\\1\\(\\2:\\d+\\)\\s*\n)+",
            //a b                   b      (          )      a
            "$1($2:$3)\n\t...\n");

        // If we put in two or more successive ellipsises, compress to one.
        st = st.replaceAll("(\t\\.\\.\\.\\s*\n){2,}", "\t...\n");

        // Get rid of dangling ellipsis at the end.
        st = st.replaceAll("\t\\.\\.\\.\\s*\n\\s*$", "");

        // Get rid of blank lines.
        st = st.replaceAll("\n\\s*\n", "\n");

        // Move ellipsis ... to the end of preceding frame line for final format.
        st = st.replaceAll("(\\s*at .*?)\\s*\n\t\\.\\.\\.\\s*\n", "$1 ...\n");

        // Put a blank line at the beginning to set off the stacktrace.
        st = StringUtil.strcat("\n", st);

        return st;
    }

    /**
        Get a string which represents the Throwable's call stack.

        @param t the Throwable to get the stack trace from

        @return a string which represents the Throwable's call stack
        @aribaapi documented
    */
    public static String stackTrace (Throwable t)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter  printWriter  = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        printWriter.close();
        try {
            stringWriter.close();
        }
        catch (IOException e) {
                // Sun changed StringWriter to throw IOException in
                // JDK 1.2. Thank you.
            Assert.that(false, "IOException in SystemUtil.stackTrace");
        }
        return stringWriter.toString();
    }

    /**
        Returns the root cause that cause that causes this exception.
        @param t the Throwable instance whose root cause is to be returned. If this is null, then <code>null</code> is returned.
        @return the root cause that cause that causes this exception, can be <code>null</code>
        @aribaapi documented
    */
    public static Throwable getRootCause (Throwable t)
    {
        if (t == null) {
            return null;
        }
        Throwable cause = t.getCause();
        while (cause != null) {
            t = cause;
            cause = t.getCause();
        }
        return t;
    }

    private static Map _environment;

    /**
        For internal use only
        @return the complete current environment
        @aribaapi ariba
    */
    public static synchronized Map getenv ()
    {
        if (_environment == null) {
            try {
                _environment = MapUtil.map();
                String[] cmdArray;
                if (isWin32()) {
                    cmdArray = new String[] { "cmd", "/c", "set"};
                }
                else {
                    cmdArray = new String[] { "env" };
                }

                Process process = Runtime.getRuntime().exec(cmdArray);
                InputStream input = process.getInputStream();
                String line = IOUtil.readLine(input);
                while (line != null) {
                    int posEqual = line.indexOf('=');
                    if (posEqual == -1) {
                        Log.util.debug("Wrong format from env : %s", line);
                    }
                    else {
                        String key = line.substring(0, posEqual);
                        if (isWin32()) {
                            key = key.toUpperCase();
                        }
                        String value = line.substring(posEqual + 1);
                        _environment.put(key, value);
                    }
                    line = IOUtil.readLine(input);
                }
            }
            catch (IOException e) {
                Log.util.debug("Unexpected IOException : %s", e);
            }
        }
        return _environment;
    }

    /**
        For internal use only
        @param envVar the name of the requested environment variable. Must not be null.
        @return the value of the requested environment variable. Null if the environment
                variable does not exist
        @aribaapi ariba
    */
    public static synchronized String getenv (String envVar)
    {
        if (_environment == null) {
            getenv();
        }

        if (isWin32()) {
            envVar = envVar.toUpperCase();
        }

        return (String)_environment.get(envVar);
    }

    // Control flag to run server under J2EEServer process
    private static boolean J2EEServerInUse = true;

    public static void setJ2EEServerInUse ()
    {
        J2EEServerInUse = true;
    }

    public static void setJ2EEServerInUse (boolean flag)
    {
        J2EEServerInUse = flag;
    }

    public static boolean usingJ2EEServer ()
    {
        return J2EEServerInUse;
    }

    /*
        Support for non standard names for ariba and config directories
    */

    /**
        Defines the location of the system directory.
        @param systemDir path of the system directory. Cannot be null.
        @aribaapi ariba
        @see #getSystemDirectory
    */
    public static final void setSystemDirectory (String systemDir)
    {
        Assert.that(systemDir != null, "systemDir is null !");
        SystemDirectoryString = systemDir;
        SystemDirectory = new File(FileUtil.fixFileSeparators(systemDir));
    }

    /**
        Defines the location of the config directory.
        @param configDir path of the config directory. Cannot be null.
        @aribaapi ariba
        @see #getConfigDirectory
    */
    public static final void setConfigDirectory (String configDir)
    {
        Assert.that(configDir != null, "configDir is null !");
        ConfigDirectoryString = configDir;
        ConfigDirectory = new File(FileUtil.fixFileSeparators(configDir));
        ConfigURL = URLUtil.urlAbsolute(ConfigDirectory);
    }

    /**
        Defines the location of the local temporary directory.
        @param tempDir path of the temp directory. Cannot be null.
        @aribaapi ariba
        @see #getLocalTempDirectory
    */
    public static final void setLocalTempDirectory (String tempDir)
    {
        Assert.that(tempDir != null, "tempDir is null !");
        LocalTempDirectory = new File(FileUtil.fixFileSeparators(tempDir));
        try {
            FileUtil.directory(LocalTempDirectory);
        }
        catch (IOException e) {
            Log.util.warning(8905, SystemUtil.stackTrace(e));
        }

    }

    /**
        Defines the location of the shared temporary directory.
        @param tempDir path of the temp directory. Cannot be null.
        @aribaapi ariba
        @see #getSharedTempDirectory
    */
    public static final void setSharedTempDirectory (String tempDir)
    {
        Assert.that(tempDir != null, "tempDir is null !");
        SharedTempDirectory = new File(FileUtil.fixFileSeparators(tempDir));
        try {
            FileUtil.directory(SharedTempDirectory);
        }
        catch (IOException e) {
            Log.util.warning(8905, SystemUtil.stackTrace(e));
        }
    }

    /**
        Provide default setup for system directory if needed.
        This provides lazy initialization since we want to
        avoid calls at static initialization that could result
        in logging. There have been bugs due to logging static
        initialization recursion.
        @aribaapi private
    */
    private static final void assureSystemDirectory ()
    {
        if (SystemDirectoryString == null) {
                // no need to synchronize here--the last one wins
            setSystemDirectory(Constants.getDefaultSystemDir());
        }
    }

    /**
        Provide default setup for configuration directory if needed.
        This provides lazy initialization since we want to
        avoid calls at static initialization that could result
        in logging. There have been bugs due to logging static
        initialization recursion.
        @aribaapi private
    */
    private static final void assureConfigDirectory ()
    {
        if (ConfigDirectoryString == null) {
            // no need to synchronize here--the last one wins
            setConfigDirectory(Constants.getDefaultConfigDir());
        }
    }

    /**
        Provide default setup for configuration directory if needed.
        This provides lazy initialization since we want to
        avoid calls at static initialization that could result
        in logging. There have been bugs due to logging static
        initialization recursion.
        @aribaapi private
    */
    private static final void assureTempDirectory ()
    {
        if (LocalTempDirectory == null) {
            setLocalTempDirectory(Constants.getDefaultTempDirectory());
        }
        if (SharedTempDirectory == null) {
            setSharedTempDirectory(Constants.getDefaultTempDirectory());
        }
    }

    /**
        Returns the sytem directory.
        @return the sytem directory.
        @aribaapi documented
    */
    public static File getSystemDirectory ()
    {
        assureSystemDirectory();
        return SystemDirectory;
    }

    /**
        Returns the config directory.
        @return the config directory.
        @aribaapi documented
    */
    public static File getConfigDirectory ()
    {
        assureConfigDirectory();
        return ConfigDirectory;
    }

    /**
        @return the config directory name as a string
        @aribaapi documented
    */
    public static String getConfigDirectoryString ()
    {
        assureConfigDirectory();
        return ConfigDirectoryString;
    }

    /**
        Returns the config URL.
        @return the config URL.
        @aribaapi documented
    */
    public static URL getConfigURL ()
    {
        assureConfigDirectory();
        return ConfigURL;
    }

    /**
        Returns the temporary directory for the local server
        This directory can be used to store temporary files which
        do not need to be shared accross multiple processes
        @return the local temporary directory
        @aribaapi documented
    */
    public static File getLocalTempDirectory ()
    {
        assureTempDirectory();
        return LocalTempDirectory;
    }

    /**
        Returns the temporary directory which can be shared accross multiple processes
        This directory can be used to store temporary files which require to be
        accessible by different nodes
        @return the shared temporary directory
        @aribaapi documented
    */
    public static File getSharedTempDirectory ()
    {
        assureTempDirectory();
        return SharedTempDirectory;
    }

    /**
        Modify a relative file path to use custom config and directory locations
        @aribaapi ariba
    */
    public static String fixRelativePath (String path)
    {
        if (path != null) {
                //Make sure path is relative
            if (!path.startsWith("/")) {
                if (path.startsWith("ariba/")) {
                    path = StringUtil.strcat(
                        SystemDirectoryString,
                        path.substring(path.indexOf("/")));
                }
                else if (path.startsWith("config/")) {
                    path = StringUtil.strcat(
                        ConfigDirectoryString,
                        path.substring(path.indexOf("/")));
                }
            }

            return path;
        }
        return null;
    }

    /**
        Returns the installation directory.
        Note: this might return am empty abstract path File, so be prepared to
        deal with that. On Mac, unix, and windows it should be correct, though.
        @return the installation directory or an empty abstract path
        @aribaapi ariba
    */
    public static File getInstallDirectory ()
    {
        return InstallDirectory;
    }

    /**
     Are we in development, not production mode?
     This is a low level check needed for internal testing.
     Where possible clients should use the higher level isProduction
     method defined in ServerInterface.
     @return true if development, false if production
     @aribaapi ariba
     @see ariba.util.core.ServerInterface#isProduction()
    */
    public static boolean isDevelopment ()
    {
        return IsDevelopment;
    }
    /**
        Does the install directory have an internal subdirectory?
        @ return true if <install>/internal exists, false otherwise
        @aribaapi private
    */
    private static boolean hasInternalDirectory ()
    {
        return InternalDirectory.isDirectory();
    }
    /**
        Returns the internal directory. Can be null.
        @return the internal directory, or <code>null</code> if the internal directory
        does not exist.
        @aribaapi ariba
    */
    public static File getInternalDirectory ()
    {
        return InternalDirectory.isDirectory() ? InternalDirectory : null;
    }

    /**
     * Validates that a method is allowed to call another method.
     * @param validCallers an array of valid callers.  See the ValidCaller 
     *        constructor for details on what an entry looks like.
     *        To improve this method's performance, valid callers that require 
     *        a search through the full stack should be at the end of the array.
     * @param fatalAssert if true, an invalid caller causes a fatal assert, otherwise
     *                    a non-fatal assert.
     * @aribaapi private
     */
    public static void validateCaller (ValidCaller[] validCallers, boolean fatalAssert)
    {
        /**
         * The index in the stack array to start looking for a valid caller. Indexes are:
         *  0 - Thread.getStackTrace()
         *  1 - This method
         *  2 - This method's caller which is the method requesting validation of its caller.
         *  3 - Caller of the requesting method.  This is the caller we want to validate.
         */
        final int startIndex = 3;
        boolean found = false;

        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        Assert.that(stes.length > startIndex, "No caller on stack to validate");

        for (ValidCaller validCaller : validCallers) {
            // If not a full stack search, we only look at one method up the stack.
            int endIndex = validCaller._checkFullStack ? stes.length : startIndex + 1;
            for (int i = startIndex; i < endIndex; i++) {
                StackTraceElement ste = stes[i];
                String className = ste.getClassName();
                String methodName = ste.getMethodName();

                if (validCaller._classNameIsPrefix) {
                    // Just need to check if the class name starts with validCaller
                    // class name (which is probably just a package name).
                    if (className.startsWith(validCaller._className) &&
                        (validCaller._methodName == null || 
                         validCaller._methodName.equals(methodName))) {
                        found = true;
                        break;
                    }
                }
                else if (validCaller._className.equals(className) &&
                         (validCaller._methodName == null || 
                          validCaller._methodName.equals(methodName))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            String invalidClassName = stes[startIndex].getClassName();
            String invalidMethodName = stes[startIndex].getMethodName();
            String requestClassName = stes[startIndex-1].getClassName();
            String requestMethodName = stes[startIndex-1].getMethodName();
            String msg = "validateCaller method %s.%s is not allowed to call %s.%s";
            if (fatalAssert) {
                Assert.that(false, msg,
                            invalidClassName, invalidMethodName, 
                            requestClassName, requestMethodName);
            }
            else {
                Assert.assertNonFatal(false, msg,
                                      invalidClassName, invalidMethodName, 
                                      requestClassName, requestMethodName);
            }
        }
    }

    /**
     * A simple bean to hold the attributes of a valid caller for use by validateCaller
     * method.  See constructor for details.
     * @aribaapi private
     */
    public static class ValidCaller
    {
        public final String _className;
        public final String _methodName;
        public final boolean _classNameIsPrefix;
        public final boolean _checkFullStack;
        
        /**
         * Definition of a valid caller.
         * @param className the full class name (package name plus class name).
         *                  If classNameIsPrefix is true this is a prefix for the class
         *                  name, typically a package name.   
         * @param methodName the method name.  If null, any method in class is valid.
         *                   Note that method signature is not checked.
         * @param classNameIsPrefix when true the className is a prefix.  Any full class
         *                          name that starts with the className prefix is valid.
         *                          Typically this is used to validate an entire package.
         * @param checkFullStack if true, the full stack is searched for a match.
         *                       This is useful when there is unknown methods (such as
         *                       reflection methods) on the stack before the valid method.
         *                       If false, only the immediately caller of the requesting
         *                       method is checked. 
         * @aribaapi private
         */
        public ValidCaller (String className, String methodName, 
                            boolean classNameIsPrefix, boolean checkFullStack)
        {
            _className = className;
            _methodName = methodName;
            _classNameIsPrefix = classNameIsPrefix;
            _checkFullStack = checkFullStack;
        }
    }   
}
