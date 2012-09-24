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

    $Id: //ariba/platform/util/core/ariba/util/core/TimerQueue.java#6 $
*/

package ariba.util.core;

/**
    Private class to manage a queue of Timers. The Timers are chained
    together in a linked list sorted by the order in which they will expire.
    @note 1.0 changes to detect and stop deadlocking better

    @aribaapi private
*/
abstract public class TimerQueue implements Runnable
{
    boolean running = false;

    /**
     * Create and start a new Thread to process the timers.
     */
    synchronized void start ()
    {
        Assert.that(!running, "Can't start a TimerQueue that is already running");
        Thread timerThread = new Thread(this, "TimerQueue");
        
        try {
            if (timerThread.getPriority() > Thread.MIN_PRIORITY) {
                timerThread.setPriority(timerThread.getPriority() - 1);
            }
            
            timerThread.setDaemon(true);
        }
        catch (SecurityException e) {
        }
        timerThread.start();
        running = true;
    }

    synchronized void stop ()
    {
        running = false;
        notify();
    }

        /**
        If there are a ton of timers, this method may never return. It
        loops checking to see if the head of the Timer list has
        expired. If it has, it posts the Timer and reschedules it if
        necessary.
    */
    synchronized long postExpiredTimers ()
    {
        long currentTime, timeToWait;
        Timer timer;

        // The timeToWait we return should never be negative and only be zero
        // when we have no Timers to wait for.

        do {
            timer = getFirstTimer();
            if (timer == null) {
                return 0;
            }

            currentTime = System.currentTimeMillis();
            timeToWait = timer.expirationTime - currentTime;

            if (timeToWait <= 0) {
                timer.post();
                removeFirstTimer();

                // This tries to keep the interval uniform at the cost of
                // drift.

                if (timer.getRepeats()) {
                    addTimer(timer, currentTime + timer.getDelay());
                }

                // Allow other threads to call addTimer() and removeTimer()
                // even when we are posting Timers like mad.  Since the wait()
                // releases the lock, be sure not to maintain any state
                // between iterations of the loop.

                try {
                    wait(1);
                }
                catch (InterruptedException e) {
                }
            }
        }
        while (timeToWait <= 0);

        return timeToWait;
    }

    /**
     * Remove the first timer in the queue.
     */
    abstract void removeFirstTimer ();

    /**
     * Get the first timer in the queue
     * @return The first (soonest to fire) Timer.
     */
    abstract Timer getFirstTimer ();

    /**
     * Add a new Timer to the queue that will fire after the specified time
     *
     * @param timer The Timer to add
     * @param expirationTime The time at which the timer should fire.
     */
    abstract void addTimer (Timer timer, long expirationTime);

    /**
     * Remove a Timer from the queue. The passed-in Timer must be == to the removed Timer.
     * @param timer The Timer to remove.
     */
    abstract void removeTimer (Timer timer);

    /**
     * Returns true whether the current Timer is in the queue. (Actually returns true if
     * the Timer is running, i.e. if it's in any currently running TimerQueue).
     * @param timer The Timer to test
     * @return true if the timer is running
     */
    synchronized boolean containsTimer (Timer timer)
    {
        return timer.running;
    }

    /**
     * Run the timer-processing loop.
     */
    public synchronized void run ()
    {
        long timeToWait;

        while (running) {
            timeToWait = postExpiredTimers();
            try {
                wait(timeToWait);
            }
            catch (InterruptedException e) {
            }
        }
    }

    /**
     * Get the number of Timers that are in this queue. (Repeating timers are counted
     * once).
     * @return the current number of pending timers.
     */
    abstract public int size ();
}
