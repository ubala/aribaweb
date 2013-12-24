/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/Chrono.java#10 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import ariba.util.log.Logger;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;

/**
    Simple timing class, used for timing one or more trials of an event.
    The trials must be timed consecutively, so {@link #start} or
    {@link #stop} cannot be called twice in a row.

    @aribaapi ariba
*/
public class Chrono
{

    private static final int overhead = calibrate();
    private static final int calibrateReps = 100;
    private static int calibrate ()
    {
        Chrono c = new Chrono ("calibrate");
        for (int i = 0; i < calibrateReps; i++) {
            c.start();
            c.stop();
        }
        return c.totalTime / calibrateReps;
    }

    protected boolean enabled;
    private long start;
    private long stop;
    private int totalTime;
    private int trials;
    private String name;
    private Logger log;
    private int startInfoId = -1;
    private int stopInfoId = -1;
    private long cpuStart;
    private long cpuStop;
    private long userStart;
    private long userStop;

    private static ThreadLocal<List<String>> _stack = new ThreadLocal<List<String>>()
    {
        @Override
        protected List<String> initialValue ()
        {
            return ListUtil.list();
        }
    };

    private String parent = null;

    /**
        Creates a new instance of a <code>Chrono</code> timer.

        @param name the name of the event.  Used by {@link #toString}
        to print out the timing summary.
    */
    public Chrono (String name)
    {
        this(name, null);
    }

    /**
        Creates a new instance of a <code>Chrono</code> timer.

        @param name the name of the event.  Used by {@link #toString}
        to print out the timing summary.
        @param logger the Logger to use to print debug statements
    */
    public Chrono (String name, Logger logger)
    {
        this.name = name;
        enabled = true;
        start = 0;
        stop = 0;
        cpuStart = 0;
        cpuStop = 0;
        userStart = 0;
        userStop = 0;
        totalTime = 0;
        trials = 0;
        log = logger;
    }

    /**
        Creates a new instance of a <code>Chrono</code> timer.

        @param name the name of the event.  Used by {@link #toString}
        to print out the timing summary.
        @param logger the Logger to use to print debug statements
        @param startInfoId id of a resource for logging an INFO start message to logger.
        @param stopInfoId id of a resource for logging an INFO stop message to logger.
    */
    public Chrono (String name, Logger logger, int startInfoId, int stopInfoId)
    {
        this(name, logger);
        this.startInfoId = startInfoId;
        this.stopInfoId = stopInfoId;
    }

    /**
        Resets the counters to start timing from scratch.
    */
    public void reset ()
    {
        start = 0;
        stop = 0;
        cpuStart = 0;
        cpuStop = 0;
        userStart = 0;
        userStop = 0;
        totalTime = 0;
        trials = 0;
        _stack.get().clear();
    }

    /**
        Starts a trial.  Must always be called before {@link #stop}.
    */
    public void start ()
    {
        if (start != 0) {
                // Chrono {0} started twice
            Log.util.warning(2792, name);
        }
        if (enabled) {
            start = System.currentTimeMillis();
            if (log != null && log.isDebugEnabled()) {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                cpuStart = bean.getCurrentThreadCpuTime();
                userStart = bean.getCurrentThreadUserTime();
            }
            List<String> stack = null;
            if (_stack != null) {
                stack = _stack.get();
            }
            if (!ListUtil.nullOrEmptyList(stack)) {
                parent = stack.get(stack.size() - 1);
            }
            if (stack != null) {
                stack.add(name);                
            }
            if (log != null) {
                String fullName = Fmt.S("%s (%s) (Thread %s)",
                             name, parent, Thread.currentThread().getName());

                if (startInfoId > 0) {
                    log.info(startInfoId, fullName);
                }
                else {
                    log.debug("Phase '%s' started", fullName);
                }
            }
        }
    }

    /**
        Stops timing the current trial.
    */
    public void stop ()
    {
        if (enabled) {
            if (start == 0) {
                    // Chrono {0}: stop() called before start()
                Log.util.warning(4317, name);
                stop = 0;
            }
            else {
                stop = System.currentTimeMillis();
                if (log != null && log.isDebugEnabled()) {
                    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                    cpuStop = bean.getCurrentThreadCpuTime();
                    userStop = bean.getCurrentThreadUserTime();
                }
            }

            int trialTime = (int)(stop - start);
            totalTime += trialTime;
            trials++;
            if (log != null) {
                String fullName = Fmt.S("%s (%s) (Thread %s)",
                             name, parent, Thread.currentThread().getName());
                if (stopInfoId > 0 && log.isInfoEnabled()) {
                    log.info(stopInfoId,
                             fullName,
                             Integer.toString(trials),
                             Double.toString(trialTime / 1000),
                             getExtraInfo(true));
                }
                else if (log.isDebugEnabled()) {
                    log.debug("Phase '%s' (trial #%s) finished: duration=%ss%s",
                              fullName,
                              Integer.toString(trials),
                              Double.toString(trialTime / 1000),
                              getExtraInfo(true));
                }
            }
            start = 0;
            stop = 0;
            List<String> stack = null;
            if (_stack != null) {
                stack = _stack.get();
                if (!ListUtil.nullOrEmptyList(stack)) {
                    stack.remove(stack.size() - 1);
                }
            }
        }
    }

    /**
        Enables the timer, so that calls to {@link #start} and
        {@link #stop} gather more timing information.
    */
    public void enable ()
    {
        enabled = true;
    }

    /**
        Disables the timer.  Subsequent calls to {@link #start} and
        {@link #stop} will have no effect on timing information.
    */
    public void disable ()
    {
        enabled = false;
    }

    /**
        @return the total number of seconds for all trials
    */
    public double getTotalSeconds ()
    {
        return ((double)totalTime) / 1000.0;
    }

    /**
        @return the average number of seconds for all trials.
    */
    public double getAverageSeconds ()
    {
        if (trials == 0) {
            return 0.0;
        }

        return getTotalSeconds() / (double)trials;
    }

    /**
        @return the number of trials that were timed.
    */
    public int getTrialCount ()
    {
        return this.trials;
    }

    /**
        @return a description of the event, number of trials and
        timing information
    */
    public String toString ()
    {
        if (trials ==  0) {
            return Fmt.S("Timing for %s: no trials", name);
        }

        if (trials == 1) {
            return Fmt.S("Timing for %s (%s) (Thread %s): %ss%s",
                         name,
                         parent,
                         Thread.currentThread().getName(),
                         Double.toString(getTotalSeconds()),
                         getExtraInfo(false));
        }
        return Fmt.S("Timing for %s (%s) (Thread %s) (%s trials): total = %ss, avg = %ss%s",
                     new Object[] {name,
                     parent,
                     Thread.currentThread().getName(),
                     Integer.toString(trials),
                     Double.toString(getTotalSeconds()),
                     Double.toString(getAverageSeconds()),
                     getExtraInfo(false)});
    }

    /**
        Hook to get more information from subclasses
        @param latestTrial indicates whether the extra info is only for
               the latest trial or for all the trials that occurred
        @return more information to display
        @aribaapi ariba
    */
    protected String getExtraInfo (boolean latestTrial)
    {
        if (log != null && log.isDebugEnabled()) {
            return Fmt.S(" (User: %sms, Total: %sms)",
                    (userStop - userStart)/1000000, (cpuStop - cpuStart)/1000000);
        }
        return "";
    }
}
