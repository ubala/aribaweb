/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/PerformanceStateCPUTimedCounter.java#2 $

    Responsible: gbhatnagar
*/

package ariba.util.core;

import ariba.util.core.PerformanceStateTimedCounter.Instance;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;

/**
 * A performance counter for thread CPU time.
 *
 * A <code>CPUTimer</code> keeps track of the start and end time for measurement.
 *
 * @aribaapi ariba
 */
public class PerformanceStateCPUTimedCounter extends PerformanceStateTimedCounter
{
    /**
     * Creates a new <code>PerformanceStateCPUTimedCounter</code> instance.
     *
     */
    public PerformanceStateCPUTimedCounter ()
    {
           // ranked just before "Runtime"
        super("Thread CPU Time", PerformanceState.DispatchTimer.getLogRank() - 1,
              PerformanceStateCore.LOG_TIME);
    }

    protected PerformanceStateCPUTimedCounter.Instance newInstance (String name)
    {
        return new Instance(name);
    }

    /**
     * @aribaapi ariba
     */
    public static class Instance extends PerformanceStateTimedCounter.Instance
    {
        public Instance (String name)
        {
            super(name);
        }

        @Override
        protected Stopwatch newStopwatch ()
        {
            return new CPUTimer();
        }
    }
}

class CPUTimer implements Stopwatch
{
    private long _startTime = 0l;

    public void start ()
    {
        _startTime = getCpuTimeInMillis();
    }

    public long runningTime ()
    {
        long result = (getCpuTimeInMillis() - _startTime) * 1000;
        return result;
    }

    public long stop ()
    {
            // multiply millis by 1000 to get microseconds.
        long result = (getCpuTimeInMillis() - _startTime) * 1000;
        _startTime = 0l;
        return result;
    }

    public boolean isRunning ()
    {
        return _startTime != 0l;
    }

    public int resolutionLevel ()
    {
        return 2;
    }

    /** thread CPU time in milliseconds. */
    private long getCpuTimeInMillis ()
    {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime()/1000000: 0L;
    }
}
