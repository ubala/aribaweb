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

    $Id: //ariba/platform/util/core/ariba/util/io/Exec.java#8 $
*/

package ariba.util.io;

import ariba.util.core.Assert;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.MasterPasswordClient;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.log.Log;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
    Runtime.exec made simple

    @aribaapi ariba
*/
public final class Exec
{
    public static int exec (String[]     cmdarray,
                            String[]     envp,
                            int          timeout,
                            InputStream  stdin,
                            OutputStream stdout,
                            OutputStream stderr) throws IOException
    {
        return new Exec(cmdarray, envp, timeout,
                        stdin, stdout, stderr).exit;
    }

    /**
        Exec the given command adding the master password info if necessary.
        Specifically, the -masterPasswordNoPrompt option is appended to the
        cmdarray array, and the master password is fetched and passed to the
        stdin of the exec call. </p>

        Two limitations:</p>
        (1) Original command that requres input data is not supported. In fact,
        there is no provision for the caller to provide the stdin
        (2) The -masterPasswordNoPrompt is appended at the end of the command line
        array, so commands that pipe or redirect output such as {"tableedit", "-script",
        "scriptfile", ">", "outfile"} will become {"tableedit", "-script",
        "scriptfile", ">", "outfile", "-masterPasswordNoPrompt"), which clearly won't
        work.
        
        @param cmdarray the command to execute, must be non null and not empty
        @param envp the environment to execute, can be mull.
        @param timeout timeout, a value of timeout will timeout immediately, negative value
        means no timeout (wait forever)
        @param stdout the stdout
        @param stderr the stderr
        @return the status of the exec'ed process. A value of 0 indicates normal termination.
        @exception IOException if an I/O error occurs
        @aribaapi ariba
    */
    public static int execWithMasterPassword (String[] cmdarray,
                                              String[] envp,
                                              int timeout,
                                              OutputStream stdout,
                                              OutputStream stderr)
      throws IOException
    {
        String mpw = MasterPasswordClient.getMasterPasswordClient().getMasterPassword();
        if (mpw == null) {
            return exec(cmdarray, envp, timeout, null, stdout, stderr);
        }
            // cmdarray must not be null or empty, it would not make sense to exec
            // an empty command
        String[] newCmdArray = new String[cmdarray.length+1];
        System.arraycopy(cmdarray, 0, newCmdArray, 0, cmdarray.length);
        newCmdArray[cmdarray.length] =
            MasterPasswordClient.DashOptionMasterPasswordNoPrompt;
        ByteArrayInputStream in = new ByteArrayInputStream(
            StringUtil.strcat(mpw, "\n").getBytes());
        return exec(newCmdArray, envp, timeout, in, stdout, stderr);
    }
      
        /**
        The exit value of the Process
    */
    private int exit;

    /*
        We watch the Threads to see if they are still alive.

        Even if the Process exits, we wait for the I/O to complete. If
        it doesn't complete before the timeout it is still an error.
    */
    ExecWaiter waiter;
    ExecCopier in;
    ExecCopier out;
    ExecCopier err;

    /**
        true if there is an error on any copier
    */
    boolean error;
        
    /**
        Using this class involves 5 Threads.
        
        The main thread starts the Process and then kicks off a waiter
        thread to watch for the Process to end.
        
        The main thread then kicks three copier threads off to handle
        stdin, stdout, and stderr.
        
        Then the main thread watches what is going on and watches the
        clock. If we are out of time, we throw an IOException. If not,
        we sleep for second. We then look to see if the waiter and
        copier threads are finished. If so, we're all done. If not, we
        look to see if a copier thread has found an error. If an error
        has been found, the main thread throws an IOException.
        
        @aribaapi private
    */
    private Exec (String[]     cmdarray,
                  String[]     envp,
                  int          timeout,
                  InputStream  stdin,
                  OutputStream stdout,
                  OutputStream stderr) throws IOException
    {
        Process process = null;
            //Modify the first command to be an executable
        Assert.that(cmdarray != null, "cmdarray shouldnot be null");
        Assert.that(cmdarray.length > 0, "length of cmdarray should be greater than 0");
        Log.util.debug("Exec received : %s", cmdarray);
        cmdarray[0] = lookForExecutable(cmdarray[0]);
        Log.util.debug("Exec will execute : %s", cmdarray);

        try {
                // Start process
            process  = Runtime.getRuntime().exec(cmdarray, envp);

                // Start thread to wait
            waiter = new ExecWaiter(this, process);

                // Start copier threads
            in  = new ExecCopier("stdin",
                                 this,
                                 stdin,
                                 process.getOutputStream(),
                                 true);
            out = new ExecCopier("stdout",
                                 this,
                                 process.getInputStream(),
                                 stdout,
                                 false);
            err = new ExecCopier("stderr",
                                 this,
                                 process.getErrorStream(),
                                 stderr,
                                 false);
            int execDone = 0;
            while (true) {
                    // times up, time to die
                if (timeout == 0) {
                    throw new IOException("Process timed out");
                }
                SystemUtil.sleep(Date.MillisPerSecond);

                    // unless we are waiting forever, decrement time left.
                if (timeout != -1) {
                    timeout--;
                }

                    // all done
                if (!waiter.isAlive() &&
                    !in.isAlive()     &&
                    !out.isAlive()    &&
                    !err.isAlive())
                {
                    exit = process.exitValue();
                    return;
                }

                if (!waiter.isAlive()) {
                    if (execDone++ == 10) {
                        exit = process.exitValue();
                        return;
                    }
                }
                    // copier thread had problem, time to die
                if (error) {
                    throw new IOException("Process IOException");
                }
            }
        }
        finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public static void main (String[] args)
    {
        if (args.length < 5) {
            Fmt.F(SystemUtil.err(),
                  "usage: ariba.util.io.Exec " +
                  "stdin stdout stderr timeout cmd arg1 arg2 ...\n");
            SystemUtil.exit(1);
        }
        String[] cmdarray = new String[args.length - 4];
        System.arraycopy(args, 4, cmdarray, 0, cmdarray.length);

        String timeoutString = args[3];
        int timeout;
        try {
            timeout = IntegerFormatter.parseInt(timeoutString);
        }
        catch (ParseException e) {
            Fmt.F(SystemUtil.err(),
                  "Could not parse timeout value %s: %s\n",
                  timeoutString,
                  e);
            SystemUtil.exit(1);
            return;
        }

        try {

            File stdin = new File(args[0]);
            InputStream in;
            if (stdin.exists()) {
                in = IOUtil.bufferedInputStream(stdin);
            }
            else {
                in = null;
            }

            OutputStream out = IOUtil.bufferedOutputStream(new File(args[1]));
            OutputStream err = IOUtil.bufferedOutputStream(new File(args[2]));
                
            SystemUtil.exit(
                Exec.exec(cmdarray,
                          null,
                          timeout,
                          in,
                          out,
                          err));
        }
        catch (IOException ioe) {
            Fmt.F(SystemUtil.err(),
                  "Problem running %s: %s\n",
                  Arrays.asList(cmdarray),
                  SystemUtil.stackTrace(ioe));
            SystemUtil.exit(1);
        }
    }


    /**
        For windows, try to find the first executable file from the given name
        Do nothing on UNIX.
        @param name of the program to be run
        @return on UNIX, returns the parameter; on NT, returns the real name of the executable
                by appending the PATH and the extension if found and needed.
    */
    private static String lookForExecutable (String cmd)
    {
        if (!SystemUtil.isWin32()) {
            return cmd;
        }
        /*
            Check first if the cmd is neither absolute nor relative
            To do so, just check if the name contains the pathSeparator
        */
        if (cmd.indexOf('/') > -1 || cmd.indexOf('\\') > -1) {
            return cmd;
        }
        
        /*
            Then check if the cmd contains an extension
            To do so, just check if the name contains a dot
        */
        if (cmd.indexOf('.') > -1) {
            return cmd;
        }

        /*
            Look for first in the current directory
        */
        String cmdInCurrentIfAny = lookForExecutableInDirectory(cmd, ".");
        if (cmdInCurrentIfAny != null) {
            return cmdInCurrentIfAny;
        }

        /*
            Look for in each element of the variable PATH
        */
        String varPath = SystemUtil.getenv("PATH");
        if (varPath == null) {
            Log.util.debug("PATH returns null");
            return cmd;
        }
        StringTokenizer paths = new StringTokenizer(varPath, File.pathSeparator);
        while (paths.hasMoreTokens()) {
            String path = paths.nextToken();
            String newCmd = lookForExecutableInDirectory(cmd, path);
            if (newCmd != null) {
                return newCmd;
            }
        }
        return cmd;
    }

    /**
        Try to find an executable in a specific directory.
        This method is only called on NT.
        @param cmd name of the command
        @param path directory where to look for
        @return the full name of the command if exists, null otherwise
    */
    private static String lookForExecutableInDirectory (String cmd, String path)
    {
        String varPathExt = SystemUtil.getenv("PATHEXT");
        if (varPathExt == null) {
            Log.util.debug("PATHEXT returns null");
            return null;
        }
        StringTokenizer extensions = new StringTokenizer(varPathExt, File.pathSeparator);
        while (extensions.hasMoreTokens()) {
            String extension = extensions.nextToken();
            String newCmd = StringUtil.strcat(cmd, extension);
            if ((new File(path, newCmd)).exists()) {
                return newCmd;
            }
        }
        return null;
    }
}

class ExecWaiter extends Thread
{
    private Exec    exec;
    private Process process;

    ExecWaiter (Exec    exec,
                Process process)
    {
        this.exec    = exec;
        this.process = process;
        start();
    }

    public void run ()
    {
        while (true) {
            try {
                process.waitFor();
                return;
            }
            catch (InterruptedException e) {
                Log.util.debug("Unexpected exception: %s", SystemUtil.stackTrace(e));
            }
        }
    }
}

class ExecCopier extends Thread
{
    private Exec         exec;
    private InputStream  input;
    private OutputStream output;
    private boolean      close;

    ExecCopier (String       name,
                Exec         exec,
                InputStream  input,
                OutputStream output,
                boolean      close)
    {
        super(name);
        this.exec   = exec;
        this.input  = input;
        this.output = output;
        this.close  = close;
        start();
    }

    public void run ()
    {
        if (input == null) {
            return;
        }

        if (output == null) {
            return;
        }

        if (!IOUtil.inputStreamToOutputStream(input,
                                            output,
                                            new byte[2048],
                                            true)) {
            exec.error = true;
        }
        
        try {
            output.flush();
        }
        catch (IOException ioe) {
            exec.error = true;
        }
    }
}
