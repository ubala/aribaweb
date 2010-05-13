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

    $Id: //ariba/platform/util/core/ariba/util/core/Timer.java#6 $
*/

package ariba.util.core;

/**
    Object subclass that causes an action to occur at a predefined
    rate. For example, an animation object can use a Timer as the
    trigger for drawing its next frame. Each Timer has a Target that
    receives a <b>performCommand()</b> message; the command the Timer
    sends to its Target; and a delay (the time between
    <b>performCommand()</b> calls). When delay milliseconds have
    passed, a Timer sends the <b>performCommand()</b> message to its
    Target, passing as parameters the command and the object set using
    the Timer's <b>setData()</b> method. This cycle repeats until
    <b>stop()</b> is called, or halts immediately if the Timer is
    configured to send its message just once.<p> Using a Timer
    involves first creating it, then starting it using the
    <b>start()</b> method.

    @see Target
    @note 1.0 changes to detect and stop dealocking better
    @note 1.0 calling setDelay() on already running Timer works correctly
    @note 1.1 Timer will get the TimerQueue from the most proper application

    @aribaapi private
*/
public class Timer implements EventProcessor
{
    EventLoop   eventLoop;
    Target      target;
    String      command;
    Object      data;
    int         initialDelay;
    int         delay;
    boolean     repeats = true;

    // These fields are maintained by TimerQueue.

    long expirationTime;
    Timer nextTimer;
    boolean running;

    /**
        Constructs a Timer associated with the EventLoop
        <b>eventLoop</b> that sends <b>performCommand()</b> to
        <b>target</b> every <b>delay</b> milliseconds. You only call
        this constructor if you need to associate a Timer with an
        EventLoop other than the application's EventLoop.
    */
    public Timer (EventLoop eventLoop,
                  Target    target,
                  String    command,
                  int       delay)
    {
        Assert.that(eventLoop != null, "eventLoop parameter is null");
        this.eventLoop = eventLoop;
        this.target = target;
        this.command = command;
        setDelay(delay);
        setInitialDelay(delay);
    }

    /**
        Returns the timer queue.
    */
    TimerQueue getTimerQueue()
    {
        return eventLoop.timerQueue();
    }

    /**
        Returns the EventLoop associated with this Timer.
    */
    public EventLoop getEventLoop ()
    {
        return eventLoop;
    }

    /**
        Sets the Target that will receive <b>performCommand()</b>
        messages from the Timer.
    */
    public void setTarget (Target target)
    {
        this.target = target;
    }

    /**
        Returns the Target that will receive <b>performCommand()</b>
        messages from the Timer.

        @see #setTarget
    */
    public Target getTarget ()
    {
        return target;
    }

    /**
        Sets the command the Timer sends to its target in the
        <b>performCommand()</b> method.
        @see #setTarget
    */
    public void setCommand (String command)
    {
        this.command = command;
    }

    /**
        Returns the command the Timer sends to its target in the
        <b>performCommand()</b> message.

        @see #setCommand
    */
    public String getCommand ()
    {
        return command;
    }

    /**
        Sets the Timer's delay, the number of milliseconds between successive
        <b>performCommand()</b> messages to its Target.

        @see #setTarget
        @see #setInitialDelay
    */
    public void setDelay (int delay)
    {
        Assert.that(delay >= 0, "Invalid delay: %s", Constants.getInteger(delay));
        this.delay = delay;
    }

    /**
        Returns the Timer's delay.

        @see #setDelay
    */
    public int getDelay ()
    {
        return delay;
    }

    /**
        Sets the Timer's data.
    */
    public void setData (Object data)
    {
        this.data = data;
    }

    /**
        Returns the Timer's data.
    */
    public Object getData ()
    {
        return data;
    }

    /**
        Sets the Timer's initial delay. This is the number of
        milliseconds the Timer will wait before sending its first
        <b>performCommand()</b> message to its Target. This setting
        has no effect if the Timer is already running.

        @see #setTarget
        @see #setDelay
    */
    public void setInitialDelay (int initialDelay)
    {
        Assert.that(initialDelay >= 0,
                    "Invalid initial delay: %s",
                    Constants.getInteger(initialDelay));
        this.initialDelay = initialDelay;
    }

    /**
        Returns the Timer's initial delay.

        @see #setDelay
    */
    public int getInitialDelay ()
    {
        return initialDelay;
    }

    /**
        If <b>flag</b> is <b>false</b>, instructs the Timer to send a
        <b>performCommand()</b> message to its Target only once, and then stop.
    */
    public void setRepeats (boolean flag)
    {
        repeats = flag;
    }

    /**
        Returns <b>true</b> if the Timer will send a <b>performCommand()</b>
        message to its target multiple times.
        @see #setRepeats
    */
    public boolean getRepeats ()
    {
        return repeats;
    }

    /**
        Starts the Timer, causing it to send <b>performCommand()</b> messages
        to its Target.
        @see #stop
    */
    public void start ()
    {
        getTimerQueue().addTimer(
            this,
            System.currentTimeMillis() + getInitialDelay());
    }

    /**
        Returns <b>true</b> if the Timer is running.
        @see #start
    */
    public boolean isRunning ()
    {
        return getTimerQueue().containsTimer(this);
    }

    /**
        Stops a Timer, causing it to stop sending <b>performCommand()</b>
        messages to its Target.
        @see #start
    */
    public void stop ()
    {
        getTimerQueue().removeTimer(this);
    }

    /**
        EventProcessor interface method implemented to perform the Timer's
        event processing behavior, which is to send the <b>performCommand()</b>
        message to its Target.  You never call this method.
        @see EventProcessor
    */
    public void processEvent ()
    {
        if (target != null) {
            target.performCommand(command, data);
        }
    }

    /**
        Returns the Timer's string representation.
    */
    public String toString ()
    {
        return Fmt.S("Timer {target = %s; command = %s" +
                     "; delay = %s; initialDelay = %s" +
                     "; repeats = %s}",
                     target, command, Constants.getInteger(delay),
                     Constants.getInteger(initialDelay), Constants.getBoolean(repeats));
    }

    void post ()
    {
        eventLoop.addEvent(this);
    }
}
