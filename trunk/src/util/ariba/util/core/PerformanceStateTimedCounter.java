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

    $Id: //ariba/platform/util/core/ariba/util/core/PerformanceStateTimedCounter.java#19 $
*/

package ariba.util.core;

import ariba.util.formatter.DoubleFormatter;
import java.util.Map;

/**
    @aribaapi ariba
*/
public class PerformanceStateTimedCounter extends PerformanceStateCounter
{
    // states
    public static final int STOPPED = 1,
                            RUNNING = 2,
                            PAUSED  = 3;

    private static final int DefaultLogFlags = LOG_COUNT | LOG_TIME;

    public PerformanceStateTimedCounter (String name, int logRank, int logFlags)
    {
        super(name, logRank, logFlags);
    }

    public PerformanceStateTimedCounter (String name, int logRank)
    {
        this(name, logRank, DefaultLogFlags);
    }

    public PerformanceStateTimedCounter (String name)
    {
        this(name, DefaultLogRank);
    }

    /**
        Start a timer
    */
    public void start ()
    {
        if (!PerformanceState.threadStateEnabled() || PerformanceState.isRecordingSuspended()) {
            return;
        }
        ((Instance)instance()).start();
    }

    /**
        Pause the timer (stop gathering time)
    */
    public void pause ()
    {
        if (!PerformanceState.threadStateEnabled()) {
            return;
        }
        ((Instance)instance()).pause(0);
    }

    /**
        Resume timing.
    */
    public void resume ()
    {
        if (!PerformanceState.threadStateEnabled()) {
            return;
        }
        ((Instance)instance()).resume();
    }

    /**
        Stop the timer.
    */
    public long stop ()
    {
        return stop(1);
    }

    /**
        Stop the timer and record a quantity of items
    */
    public long stop (long quantity, long count2Inc, String type)
    {
        if (!PerformanceState.threadStateEnabled() || PerformanceState.isRecordingSuspended()) {
            return 0l;
        }
        return ((Instance)instance()).stop(quantity, count2Inc, type);
    }

    public long stop (long quantity, long count2Inc)
    {
        if (!PerformanceState.threadStateEnabled() || PerformanceState.isRecordingSuspended()) {
            return 0l;
        }
        return ((Instance)instance()).stop(quantity, count2Inc);
    }

    public long stop (long quantity, String type)
    {
        if (!PerformanceState.threadStateEnabled() || PerformanceState.isRecordingSuspended()) {
            return 0l;
        }
        return ((Instance)instance()).stop(quantity, type);
    }

    public long stop (long quantity)
    {
        if (!PerformanceState.threadStateEnabled() || PerformanceState.isRecordingSuspended()) {
            return 0l;
        }
        return ((Instance)instance()).stop(quantity);
    }


    public int getState ()
    {
        if (!PerformanceState.threadStateEnabled()) {
            return STOPPED;
        }
        return ((Instance)instance()).getState();
    }

    protected PerformanceStateCore.Instance newInstance (String name)
    {
        Instance obj = new Instance(name);
        return obj;
    }

    /**
     * the real instance
     * @aribaapi ariba
     */
    public static class Instance extends PerformanceStateCounter.Instance
    {
        protected long elapsedTime = 0;
        protected int state = STOPPED;
            // don't double count time that is already counted in this
            // counter
        protected int recursionDepth = 0;

        protected Stopwatch stopwatch;

        public Instance (String name)
        {
            super(name);
            stopwatch = newStopwatch();
        }

        public void start ()
        {
            recursionDepth++;
            state = RUNNING;
            if (recursionDepth == 1) {
                stopwatch.start();
            }
        }

        protected void addTime (long additionalTime)
        {
            elapsedTime += additionalTime;
        }

        /**
            Stop the timer and record a quantity of items
        */
        public long stop (long quantity, long count2Inc, String type)
        {
            addCount(quantity, count2Inc, type);
            recursionDepth--;
                // you are only ready to add the completed time when the
                // highest level nested timer is finished
            long elapsed = 0l;
            if (recursionDepth == 0) {
            	elapsed = stopwatch.stop(); 
                addTime(elapsed);
                state = STOPPED;
            }
            return elapsed;
        }

        public long stop (long quantity, long count2Inc)
        {
            return stop(quantity, count2Inc, null);
        }

        public long stop (long quantity, String type)
        {
            return stop(quantity, 0, type);
        }

        public long stop (long quantity)
        {
            return stop(quantity, null);
        }

        public int getState ()
        {
            return state;
        }


        /**
            Pause the timer and record a quantity of elements to add to
            the metrics.
        */
        public void pause (long quantity)
        {
            addCount(quantity);
            addTime(stopwatch.stop());
            state = PAUSED;
        }

        public void resume ()
        {
            stopwatch.start();
            state = RUNNING;
        }


        /**
            Returns the currently elapsed time, without stopping the timer
        */
        public long getElapsedTime ()
        {
            if (!stopwatch.isRunning()) {
                return elapsedTime;
            }
            return elapsedTime + stopwatch.runningTime();
        }

        public String elapsedTimeString ()
        {
            long time = getElapsedTime();
            int resolution = resolutionLevel();

            double dtime = time / 1000000.d;
            return DoubleFormatter.getStringValue(dtime,resolution,resolution);
        }

        protected Stopwatch newStopwatch ()
        {
            /*
                There used to be a HiResTimer; the deleted code is in
                //ariba/platform/util/performance/ariba/util/performance/HiResTimer.java
                along with some also-deleted native code.  This could be resurrected someday
                but the native code portion probably supports only Solaris and not Linux.
                Besides, this seems unnecessary since on Linux the LoResTimer gives millisecond
                accuracy which is adequate.
             */
            return new LoResTimer();
        }

        public int resolutionLevel ()
        {
            return stopwatch.resolutionLevel();
        }


        /**
            The key for the elapsed time in the results EqHashtable.
        */
        public static final String ColumnElapsedTime =
            new String("elapsed time"); // OK

        /**
            Returns a hashtable with all the data from this metric.

            Do not call this method on the static instances.
        */
        public Map getData ()
        {
            Map data = super.getData();
            data.put(ColumnElapsedTime, Constants.getLong(getElapsedTime()/1000));
            return data;
        }
    }
}

class LoResTimer implements Stopwatch {

    private long _startTime = 0l;

    public void start ()
    {
        _startTime = System.currentTimeMillis();
    }

    public long runningTime ()
    {
        long result = (System.currentTimeMillis() - _startTime) * 1000;
        return result;
    }

    public long stop ()
    {
            // multiply millis by 1000 to get microseconds.
        long result = (System.currentTimeMillis() - _startTime) * 1000;
        _startTime = 0l;
        return result;
    }

    public boolean isRunning ()
    {
        return _startTime != 0l;
    }

    public int resolutionLevel ()
    {
            // UNIX systems resolve down to about 1 millisecond, but
            // Windows NT based systems only resolve to 10 milliseconds.
        return SystemUtil.isWin32() ? 2 : 3;
    }
}
