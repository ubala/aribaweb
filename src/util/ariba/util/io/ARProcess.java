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

    $Id: //ariba/platform/util/core/ariba/util/io/ARProcess.java#6 $
*/

package ariba.util.io;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.Constants;
import ariba.util.core.ServerUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import java.util.List;
import ariba.util.log.Log;
import java.io.IOException;
import java.io.InputStream;

/**
   Class ARProcess encapsulates the mess Java created with process
   management. There may be more succinct implementations possible but
   this is truly what was necessary to implement an interface to
   allow one to start, monitor and stop a process.

   ARProcess allows you to specify the command line via the
   constructor or setArguments.

   You can then launch the process (i.e. launch())

   After launch you can call isAlive(), get<type>OutputString(),
   join() and kill(). Calling launch again, will discard the results
   in the "outputstring" as expected.

   When isAlive() returns true exitCode() returns the process return code.

   Why the Java process interface sucks:

   1) There isn't a non-blocking check for "aliveness".

   2) Unless you flush the outgoing streams (from the process) the
   process will not terminate.

   3) The standard output from .bat files are not properly captured by
   the return streams.

   4) A process id would be nice (so you could track back to the system)

   5) The InputStream(s) available() member does not work.

   Finally, I later realized that rather than putting the two threads
   that are draining the std-err/out streams in a loop I could trigger
   them when the caller calls isAlive() thereby reducing the wasted
   cycles checking for output. One could also pool the byte arrays
   used to read output - a necessary evil of the fact that available
   doesn't work.

   @deprecated Use ariba.util.io.Exec

   @aribaapi private
*/
public class ARProcess
{
    static Runtime runtime = Runtime.getRuntime();
    Process process     = null;
    int     exitValue   = -1;
    String  [] args = null;
    boolean isAlive = false;
    MonitorContext  monitorContext = new MonitorContext(this);

    public ARProcess (String [] args)
    {
        this.args = args;
    }

    public ARProcess ()
    {
    }

    public String [] arguments ()
    {
        return this.args;
    }

    /**
      Of course this is only meaningful to the next call to launch()
    */
    public void setArguments (String [] args)
    {
        this.args = args;
    }

    /**
      Of course this is only meaningful to the next call to launch()
    */
    public void setArguments (String args)
    {
        this.args = ServerUtil.simpleParse(args," ");
    }

    public boolean launch ()
    {
        Assert.that(!this.isAlive,"Re-launch of live process");
        boolean success = false;
        try {
            this.monitorContext = null;
            this.isAlive = success;
            Log.util.debug("Launching process : %s",
                           ArrayUtil.formatArray("command", this.args));
            this.process = runtime.exec(this.args);
            success = (this.process != null);
            this.isAlive = success;
            if (success) {
                this.monitorContext = new MonitorContext(this);
                this.monitorContext.start();
            }

        }
        catch (IOException e) {
            Log.util.warning(2850, this.args[0], SystemUtil.stackTrace(e));
            return false;
        }

        return success;
    }

    public Process process ()
    {
        return this.process;
    }

    public boolean isAlive ()
    {
        return this.isAlive;
    }

    public void kill ()
    {
        Assert.that(this.process != null, "Null process");
        this.process.destroy();
            // process.destroy() should always work - thus it is
            // safe to join here.
        this.waitFor();
    }

    public void waitFor ()
    {
        try {
            this.process.waitFor();
            this.monitorContext.waitFor();
        }
        catch (InterruptedException ie) {
            Log.util.debug("Thread was interrupted");
        }
    }

    public int exitCode ()
    {
        Assert.that(!this.isAlive,"process still alive");
        return this.process.exitValue();
    }

    public String outputString ()
    {
        String outputString = "";
        String standardOutput = this.standardOutputString();
        if (standardOutput != null) {
            outputString = standardOutput;
        }
        String standardError = this.standardErrorString();
        if (standardError != null) {
            outputString = StringUtil.strcat(outputString, standardError);
        }

        return outputString;
    }


    public String standardOutputString ()
    {
        return this.monitorContext.stdOut.outputString();
    }

    public String standardErrorString ()
    {
        return this.monitorContext.stdErr.outputString();
    }
}

class MonitorContext
{
    public ARTerminate vulture = null;
    public ARIOMonitor stdOut = null;
    public ARIOMonitor stdErr = null;

    ARProcess arProcess = null;

    boolean isAlive = true;

    public MonitorContext (ARProcess arProcess)
    {
        this.arProcess = arProcess;
    }

    public void start ()
    {
        String instanceName = this.arProcess.arguments()[0];
        this.stdOut = new ARIOMonitor(this, arProcess.process.getInputStream());
        this.stdErr = new ARIOMonitor(this, arProcess.process.getErrorStream());

        this.vulture = new ARTerminate(this, arProcess);

        this.stdOut.start(instanceName);
        this.stdErr.start(instanceName);

        this.vulture.start(instanceName);
    }

    public void waitFor ()
    {
        try {
            this.vulture.thread.join();
        }
        catch (InterruptedException ie) {
        }
    }
}

abstract class NamedThread implements Runnable
{
    Thread thread;
    String name;

    public NamedThread (String name)
    {
        this.name = name;
    }

    public void start (String instanceName)
    {
        this.thread = new Thread(this, StringUtil.strcat(this.name, instanceName));
        this.thread.start();
    }

    public void reset () throws InterruptedException
    {
        if (this.thread != null) {
            this.thread.join();
        }
    }

    abstract public void run ();
}

class ARTerminate extends NamedThread implements Runnable
{
    private ARProcess arProcess;
    boolean isAlive = true;
    MonitorContext monitorContext;

    public ARTerminate (MonitorContext monitorContext, ARProcess arProcess)
    {
        super("ARTerminate_");
        this.arProcess = arProcess;
        this.monitorContext = monitorContext;
    }

    public void run ()
    {
        try {
            /*
                rather convoluted, tricky algorithm here - could be
                implemented cleaner

                The goal of the ARTerminate thread is to wait for the
                arProcess to die and then set the ARProcess.isAlive
                flag to false. However, it needs to wait for the two
                io threads (stdout and stderr) to complete as well in
                order that io is completely flushed. The caller/user of
                the ARProcess would expect that when isAlive() is
                false all buffers have been flushed.

                The stdout and stderr threads poll this.isAlive.
            */

            this.arProcess.process.waitFor();

            // let others know that process has died
            this.waitForOthers();

            this.arProcess.isAlive = false;
        }
        catch (InterruptedException e) {
            Log.util.debug("Process monitor thread interrupted");
        }
    }

    private void waitForOthers ()
    {
        this.monitorContext.isAlive = false;
        try {
            this.monitorContext.stdOut.thread.join();
            this.monitorContext.stdErr.thread.join();
        }
        catch (InterruptedException e) {
            Log.util.debug("Process monitor thread interrupted");
        }
    }
}

/**
    To work around the completely LAME Java process functionality one
    has to make sure that the stderr/stdout underlying buffers do not
    block otherwise the launched process never returns. Therefore, you
    have to "real-time" pull the output out of the associated streams
*/
class ARIOMonitor extends NamedThread implements Runnable
{
    MonitorContext monitorContext;
    InputStream    inputStream;
    List           list     = ListUtil.list();
    Object         listLock = new Object();

    // ChunkSize must be a value that can be stored in a byte
    private static final int ChunkSize = 128;

    public ARIOMonitor (MonitorContext monitorContext, InputStream inputStream)
    {
        super("ARIOMonitor_");
        this.inputStream = inputStream;
        this.monitorContext = monitorContext;
    }

    public String outputString ()
    {
        try {
            synchronized (this.listLock) {
                return this.outputStringAux();
            }
        }
        catch (IOException e) {
            Log.util.warning(2852, this, SystemUtil.stackTrace(e));
        }
        return null;
    }

    public void periodicAction ()
    {
        try {
            synchronized (this.listLock) {
                this.read(this.list, this.inputStream);
            }
        }
        catch (IOException ioe) {
            Log.util.warning(2853, SystemUtil.stackTrace(ioe));
        }
    }

    public void run ()
    {
        for (this.periodicAction(); this.monitorContext.isAlive; this.periodicAction()) {
            SystemUtil.sleep(1000);
        }
    }

    /**
        Returns a list of byte arrays. The format of the list is
        as follows:

        List[0] contains an Integer that stores the total payload
        bytes stored in the array

        For each element beyond element zero the List is a reference
        to a byte array.

        byte[0] contains the length of the data within the array which
        starts at byte[1]

        The above scheme was the best way I could figure out to avoid
        double buffering.
    */

    private void read (List outputList, InputStream inputStream) throws IOException
    {
        int totalBytesRead = 0;
        if (outputList.size()==0) {
            // we will store the number of elements in list zero
            outputList.add(Constants.ZeroInteger);
        }
        else {
            totalBytesRead = ((Integer)ListUtil.firstElement(outputList)).intValue();
        }
        int bytesRead = 1;

        while (bytesRead > 0) {
            byte [] byteBuffer = new byte[ChunkSize];
            bytesRead = inputStream.read(byteBuffer, 1, byteBuffer.length-1);
                // Log.util.debug("Read byte %s", new String(byteBuffer, 0, 1, 1));
            byteBuffer[0] = (byte)bytesRead;
            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
                outputList.add(byteBuffer);
            }
        }
        outputList.set(0, Constants.getInteger(totalBytesRead));
    }

    private String outputStringAux () throws IOException
    {
        int listSize = this.list.size();
        String output = null;
        if (listSize > 1) {
            int totalBytes = ((Integer)ListUtil.firstElement(this.list)).intValue();
            char [] memory = new char[totalBytes];
            int next = 0;
            for (int idx = 1; idx < listSize; idx++) {
                byte [] array = (byte[])this.list.get(idx);
                int sizeArray = (int)array[0];
                for (int jdx = 0; jdx < sizeArray; jdx++) {
                    memory[next+jdx] = (char)array[jdx+1];
                }
                next += sizeArray;
            }
            output = new String(memory);
        }
        return output;
    }
}
