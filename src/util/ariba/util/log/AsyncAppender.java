/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/log/AsyncAppender.java#11 $
*/

package ariba.util.log;

import ariba.util.core.FastStringBuffer;
import ariba.util.core.SystemUtil;
import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.helpers.BoundedFIFO;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

/**
 Basically cloned from log4j's AsyncAppender in log4j 1.2.8. Unfortunately
 that class was not designed to be overridden from outside its package, so
 it had to be duplicated.

 Key differences to provide robustness and error accounting:

 1) The Dispatcher call to appendLoopOnAppenders(event) is now in a try/catch
 block so if an exception occurs in the filtering or appending (processing)
 of the event it doesn't kill the dispatcher.

 2) When the exceptions mentioned in (1) occur, they are captured by
 AsyncAppender and available externally (see the xxxMessage(s) methods,
 below).

 @see org.apache.log4j.AsyncAppender
 @aribaapi private
*/
public class AsyncAppender extends org.apache.log4j.AppenderSkeleton
{

    /**
        The default buffer size is set to 128 events.
    */
    public static final int DEFAULT_BUFFER_SIZE = 128;

    BoundedFIFO bf = new BoundedFIFO(DEFAULT_BUFFER_SIZE);

    AppenderAttachableImpl aai;

    Dispatcher dispatcher;

    boolean locationInfo = false;

    boolean interruptedWarningMessage = false;

    boolean isConsoleWriteSuspended;
    private static final String PREFIX = "(IMPORTANT) Async appender ";

    private boolean _closed;

    public AsyncAppender (boolean isConsoleWriteSuspended)
    {
        // Note: The dispatcher code assumes that the aai is set once and
        // for all.
        this.isConsoleWriteSuspended = isConsoleWriteSuspended;
        aai = new AppenderAttachableImpl();
        dispatcher = new Dispatcher(bf, this);
        dispatcher.start();
    }

    public void addAppender (Appender newAppender)
    {
        synchronized (aai) {
            aai.addAppender(newAppender);
        }
    }

    public void append (LoggingEvent event)
    {
        // If appender was closed, don't accept any more logging events, so we
        // avoid a NPE on 'bf' below.
        if (_closed) {
            return;
        }
        
        // Set the NDC and thread name for the calling thread as these
        // LoggingEvent fields were not set at event creation time.
        event.getNDC();
        event.getThreadName();
        // Get a copy of this thread's MDC.
        event.getMDCCopy();
        if (locationInfo) {
            event.getLocationInformation();
        }
        synchronized (bf) {
                /**
                    If low level Logger debugging is on, send messages
                    whenever we start with the buffer full.
                    @see Logger#isDebugging
                */
            if (bf.isFull() && Logger.isDebugging()) {
                printMessage("full buffer.", this, bf, null);
            }
            while (bf.isFull()) {
                try {
                    bf.wait();
                }
                catch (InterruptedException e) {
                    if (!interruptedWarningMessage) {
                        interruptedWarningMessage = true;
                        LogLog.warn("AsyncAppender interrupted.", e);
                    }
                    else {
                        LogLog.warn("AsyncAppender interrupted again.");
                    }
                }
            }

            bf.put(event);
            bf.notify();
        }
    }

    /**
     Close this <code>AsyncAppender</code> by interrupting the
     dispatcher thread which will process all pending events before
     exiting.
    */
    public void close ()
    {
        synchronized(this) {
            // avoid multiple close, otherwise one gets NullPointerException
            if (_closed) {
                return;
            }
            _closed = true;
        }

        // The following cannot be synchronized on "this" because the
        // dispatcher synchronizes with "this" in its while loop. If we
        // did synchronize we would systematically get deadlocks when
        // close was called.
        dispatcher.close();
        try {
            dispatcher.join();
        }
        catch (InterruptedException e) {
            LogLog.error("Got an InterruptedException while waiting for the " +
                         "dispatcher to finish.", e);
        }
        dispatcher = null;
        bf = null;
        
        // Unregister this so it doesn't get polled for messages.
        Logger.unregisterAsyncAppender(this);
    }

    public Enumeration getAllAppenders ()
    {
        synchronized (aai) {
            return aai.getAllAppenders();
        }
    }

    public Appender getAppender (String name)
    {
        synchronized (aai) {
            return aai.getAppender(name);
        }
    }

    /**
     Returns the current value of the <b>LocationInfo</b> option.
    */
    public boolean getLocationInfo ()
    {
        return locationInfo;
    }
    
    /**
     Is the appender passed as parameter attached to this category?
    */
    public boolean isAttached (Appender appender)
    {
        return aai.isAttached(appender);
    }

    /**
     The <code>AsyncAppender</code> does not require a layout. Hence,
     this method always returns <code>false</code>.
    */
    public boolean requiresLayout ()
    {
        return false;
    }

    public void removeAllAppenders ()
    {
        synchronized (aai) {
            aai.removeAllAppenders();
        }
    }

    public void removeAppender (Appender appender)
    {
        synchronized (aai) {
            aai.removeAppender(appender);
        }
    }

    public void removeAppender (String name)
    {
        synchronized (aai) {
            aai.removeAppender(name);
        }
    }

    /**
      The <b>LocationInfo</b> option takes a boolean value. By default,
      it is set to false which means there will be no effort to extract
      the location information related to the event. As a result, the
      event that will be ultimately logged will likely to contain the
      wrong location information (if present in the log format).

      <p>Location information extraction is comparatively very slow and
      should be avoided unless performance is not a concern.
    */
    public void setLocationInfo (boolean flag)
    {
        locationInfo = flag;
    }

    /**
      The <b>BufferSize</b> option takes a non-negative integer value.
      This integer value determines the maximum size of the bounded
      buffer. Increasing the size of the buffer is always
      safe. However, if an existing buffer holds unwritten elements,
      then <em>decreasing the buffer size will result in event
      loss.</em> Nevertheless, while script configuring the
      AsyncAppender, it is safe to set a buffer size smaller than the
      {@link #DEFAULT_BUFFER_SIZE default buffer size} because
      configurators guarantee that an appender cannot be used before
      being completely configured.
    */
    public void setBufferSize (int size)
    {
        bf.resize(size);
    }

    /**
     Returns the current value of the <b>BufferSize</b> option.
    */
    public int getBufferSize ()
    {
        return bf.getMaxSize();
    }

    /**
        Print an error or debug message standard out.

        @aribaapi private
    */
    void printMessage(String s, AsyncAppender app, BoundedFIFO buff,
                           Throwable th)
    {
        if (!isConsoleWriteSuspended) {
            FastStringBuffer b = new FastStringBuffer(PREFIX);
            b.append(s);
            b.append(" Appender: ");
            b.append(app.getName());
            b.append('/');
            b.append(app);
            b.append(", buffer: ");
            b.append(buff);
            if (th != null) {
                b.append('\n');
                b.append(SystemUtil.stackTrace(th));
            }
            SystemUtil.out().println(b);
            SystemUtil.flushOutput();
        }
    }

}

// ------------------------------------------------------------------------------
// ------------------------------------------------------------------------------
// ----------------------------------------------------------------------------

class Dispatcher extends Thread
{

    BoundedFIFO bf;

    AppenderAttachableImpl aai;

    boolean interrupted = false;

    AsyncAppender container;

    Dispatcher (BoundedFIFO bf, AsyncAppender container)
    {
        this.bf = bf;
        this.container = container;
        this.aai = container.aai;
        // It is the user's responsibility to close appenders before
        // exiting.
        this.setDaemon(true);
        // set the dispatcher priority to lowest possible value
        this.setPriority(Thread.MIN_PRIORITY);
        // set name based on creating thread so that way if the appender
        // is not closed out the creator can be id'ed
        this.setName(Thread.currentThread().getName()+"-Dispatcher-" + getName());

        // set the dispatcher priority to MIN_PRIORITY plus or minus 2
        // depending on the direction of MIN to MAX_PRIORITY.
        //+ (Thread.MAX_PRIORITY > Thread.MIN_PRIORITY ? 1 : -1)*2);

    }

    void close ()
    {
        synchronized (bf) {
            interrupted = true;
            // We have a waiting dispacther if and only if bf.length is
            // zero.  In that case, we need to give it a death kiss.
            bf.notify();
        }
    }

    /**
     The dispatching strategy is to wait until there are events in the
     buffer to process. After having processed an event, we release
     the monitor (variable bf) so that new events can be placed in the
     buffer, instead of keeping the monitor and processing the remaining
     events in the buffer.

     <p>Other approaches might yield better results.

    */
    public void run ()
    {

        //Category cat = Category.getInstance(Dispatcher.class.getName());

        LoggingEvent event;

        while (true) {
            synchronized (bf) {
                if (bf.length() == 0) {
                    // Exit loop if interrupted but only if the the buffer is empty.
                    if (interrupted) {
                        break;
                    }
                    try {
                        bf.wait();
                    }
                    catch (InterruptedException e) {
                        LogLog.error("The dispatcher should not be interrupted.");
                        break;
                    }
                }
                event = bf.get();
                bf.notify();
            } // synchronized

            // The synchronization on parent is necessary to protect against
            // operations on the aai object of the parent
            synchronized (container.aai) {
                if (aai != null && event != null) {
                        // This is a key difference from the log4j code--we
                        // enclose the appending call in a try catch block
                        // so that event processing errors don't bring down
                        // the dispatcher thread.
                    try {
                        aai.appendLoopOnAppenders(event);
                    }
                    catch (Throwable t) { // OK
                            // Post a message to the container indicating
                            // that we had a problem appending the event
                            // to one or more appender.
                        container.printMessage("dispatcher exception.",
                            container, bf, t);
                    }
                }
            }
        } // while

        // close and remove all appenders
        aai.removeAllAppenders();
    }
}
