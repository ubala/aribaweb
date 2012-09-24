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

    $Id: //ariba/platform/util/core/ariba/util/core/EventLoop.java#6 $
*/

package ariba.util.core;

import ariba.util.log.Log;

/**
    A simple event loop

    @aribaapi private
*/
public class EventLoop extends EventQueue implements Runnable
{
    public static final String ClassName = "ariba.util.core.EventLoop";

    private EventExceptionListener exceptionEvent = null;

    protected TimerQueue timerQueue;

    public void setExceptionEvent (EventExceptionListener e)
    {
        exceptionEvent = e;
    }

    /**
     * Create a new EventLoop using a new LinkedTimerQueue.
     */
    public EventLoop ()
    {
        this(new LinkedTimerQueue());
    }

    /**
     * Create a new EventLoop using the specified TimerQueue.
     * @param queue The TimerQueue to use.
     */
    public EventLoop (TimerQueue queue)
    {
        timerQueue = queue;
        this.stopRunning = true;
    }


    /**
        Runnable interface method implemented to process Events as
        they appear in the queue.
    */
    public void run ()
    {
        EventProcessor nextEvent;

        stopRunning = false;
        while (!stopRunning) {

                // remove the next event from the queue and release it
            nextEvent = getNextEvent();
            if (!stopRunning) {
                try {
                    nextEvent.processEvent();
                }
                catch (RuntimeException e) {
                    if (exceptionEvent != null) {
                        exceptionEvent.processEvent(nextEvent, e);
                    }
                    else {
                        Log.util.warning(2794, SystemUtil.stackTrace(e));
                    }
                }
            }
        }
        Log.util.debug("%s exiting", this);
    }


    /**
        Stops the EventLoop once control returns from processing the
        current EventProcessor.
    */
    public synchronized void stopRunning ()
    {
        timerQueue.stop();
        stopRunning = true;
    }

    TimerQueue timerQueue ()
    {
        return timerQueue;
    }
}
