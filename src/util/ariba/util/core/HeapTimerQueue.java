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

    $Id: //ariba/platform/util/core/ariba/util/core/HeapTimerQueue.java#2 $
*/

package ariba.util.core;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * An implementation of a TimerQueue that stores the Timers in a PriorityQueue. If you
 * are creating a TimerQueue that needs to store many timers, this is probably a good
 * choice.
 */
public class HeapTimerQueue extends TimerQueue
{
    /** The PriorityQueue that stores the Timers */
    private PriorityQueue<Timer> heap;

    /**
     * Create and start a new HeapTimerQueue
     */
    public HeapTimerQueue ()
    {
        heap = new PriorityQueue<Timer>(100,
                new TimerComparator());
        start();
    }

    /**
     * Add a new Timer to the queue that will fire after the specified time
     *
     * @param timer The Timer to add
     * @param expirationTime The time at which the timer should fire.
     */
    @Override
    synchronized void addTimer (Timer timer, long expirationTime)
    {
        timer.expirationTime = expirationTime;
        heap.add(timer);
        notify();        
    }

    /**
     * Remove a Timer from the queue. This does a linear search through the elements in
     * the queue, so it should probably be avoided if possible.
     * @param timer
     */
    @Override
    synchronized void removeTimer (Timer timer)
    {
        heap.remove(timer);
    }

    /**
     * Get the number of Timers that are in this queue. (Repeating timers are counted
     * once).
     * @return the current number of pending timers.
     */
    @Override
    public int size ()
    {
        return heap.size();
    }

    /**
     * Remove the first timer in the queue.
     */
    void removeFirstTimer ()
    {
        heap.remove();
    }

    /**
     * Get the first timer in the queue
     * @return The first (soonest to fire) Timer.
     */
    Timer getFirstTimer ()
    {
        return heap.peek();
    }

    public String toString ()
    {
        return Fmt.S("HeapTimerQueue (%s)", heap);
    }

    /**
     * Comparator used for ordering the timers in the queue.
     */
    static class TimerComparator implements Comparator<Timer>
    {
        @Override
        public int compare (Timer t1, Timer t2)
        {
            if (t1 == t2) {
                return 0;
            }
            if (t1 == null) {
                return 1;
            }
            if (t2 == null) {
                return -1;
            }
            // most important is comparing the expirationTime
            if (t1.expirationTime != t2.expirationTime) {
                return t1.expirationTime < t2.expirationTime ? -1 : 1;
            }
            // add in some other comparisons to make the result more stable
            else if (t1.repeats != t2.repeats) {
                return t1.repeats ? -1 : 1;
            }
            else if (t1.delay != t2.delay) {
                return t1.delay < t2.delay ? -1 : 1;
            }
            return t1.getCommand().compareTo(t2.getCommand());
        }
    }
}
