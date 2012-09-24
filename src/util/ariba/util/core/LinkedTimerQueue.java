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

    $Id: //ariba/platform/util/core/ariba/util/core/LinkedTimerQueue.java#2 $
*/

package ariba.util.core;

/**
 * A TimerQueue implemented using a linked list of Timers. Since adding a new timer
 * involves a linear scan through the list, this should probably only be used for
 * small TimerQueues.
 */
public class LinkedTimerQueue extends TimerQueue {
    /** The first Timer in the list */
    Timer firstTimer;

    /** The current length of the queue */
    private int _size = 0;

    /**
     * Create a new LinkedTimerQueue and start the associated thread.
     */
    public LinkedTimerQueue ()
    {
        start();
    }

    /**
     * Add a new Timer to the queue that fire at the specified time.
     * This does a linear scan through the existing Timers in the queue, so it may be
     * expensive if the queue is large, especially if the expirationTime is far in the
     * future.
     * @param timer The Timer to add
     * @param expirationTime The time at which the Timer should fire
     */
    @Override
    synchronized void addTimer(Timer timer, long expirationTime)
    {
        Timer previousTimer, nextTimer;

        // If the Timer is already in the queue, then ignore the add.

        if (timer.running) {
            return;
        }

        previousTimer = null;
        nextTimer = firstTimer;

        // Insert the Timer into the linked list in the order they will
        // expire.  If two timers expire at the same time, put the newer entry
        // later so they expire in the order they came in.

        while (nextTimer != null) {
            if (nextTimer.expirationTime > expirationTime) {
                break;
            }

            previousTimer = nextTimer;
            nextTimer = nextTimer.nextTimer;
        }

        if (previousTimer == null) {
            firstTimer = timer;
        }
        else {
            previousTimer.nextTimer = timer;
        }

        timer.expirationTime = expirationTime;
        timer.nextTimer = nextTimer;
        timer.running = true;
        _size++;
        notify();
    }

    /**
     * Remove a timer from the queue.
     * This does a linear scan through the existing Timers in the queue, so it may be
     * expensive if the queue is large, especially if the expirationTime of the Timer
     * is far in the future.
     * @param timer
     */
    synchronized void removeTimer (Timer timer)
    {
        boolean found;
        Timer previousTimer, nextTimer;

        if (!timer.running) {
            return;
        }

        previousTimer = null;
        nextTimer = firstTimer;
        found = false;

        while (nextTimer != null) {
            if (nextTimer == timer) {
                found = true;
                break;
            }

            previousTimer = nextTimer;
            nextTimer = nextTimer.nextTimer;
        }

        if (!found) {
            return;
        }

        if (previousTimer == null) {
            firstTimer = timer.nextTimer;
        }
        else {
            previousTimer.nextTimer = timer.nextTimer;
        }
        _size--;
        timer.expirationTime = 0;
        timer.nextTimer = null;
        timer.running = false;
    }


    public synchronized String toString ()
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("TimerQueue (");

        Timer nextTimer = firstTimer;
        while (nextTimer != null) {
            buf.append(nextTimer.toString());

            nextTimer = nextTimer.nextTimer;
            if (nextTimer != null) {
                buf.append(", ");
            }
        }

        buf.append(")");
        return buf.toString();
    }

    /**
     * Get the number of Timers currently in the queue. Repeating Timers are counted once.
     * @return
     */
    @Override
    public int size ()
    {
        return _size;
    }

    /**
     * Remove the first timer in the queue.
     */
    void removeFirstTimer ()
    {
        removeTimer(firstTimer);
    }

    /**
     * Get the first timer in the queue
     * @return The first (soonest to fire) Timer.
     */
    Timer getFirstTimer ()
    {
        return firstTimer;
    }
}
