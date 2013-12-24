/*
    Copyright (c) 2006-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/shutdown/ShutdownManager.java#26 $
*/

package ariba.util.shutdown;

import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.SignalHandler;
import ariba.util.core.SystemUtil;
import ariba.util.core.ThreadFactory;
import ariba.util.log.Log;
import ariba.util.log.LogManager;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The ShutdownManager is responsible for executing shutdown requests
 * as graceful as possible.
 *
 * There are several elements in the shutdown sequence:
 * <ol>
 * <li>ShutdownDelayer are objects which can delay the shutdown request</li>
 * <li>ShutdownHook are objects which are invoked during the actual shutdown process</li>
 * <li>ExitHook is the object which is invoked last to stop the VM
 * </ol>
 * @aribaapi ariba
 */
@SuppressWarnings("nls")
public class ShutdownManager
{
    /**
     * This is an interface defined to allow the shutdown manager to callback a notifier
     * just before the actual shutdown.
     * This needs to be implemented in a higher level class to send a final shutdown
     * notification before exit.
     * @aribaapi private
     */
    public interface ShutdownNotifier
    {
        public void execute ();
    }

    /**
     * Exit code to use on normal exit with no restart
     * @aribaapi ariba
     */
    public static final int NormalExitNoRestart = 0;

    /**
     * Exit code to use on fatal failure with restart.
     * @aribaapi ariba
     */
    public static final int FatalExitRestart = 1;

    /**
     * Exit code to use on fatal failure without restart.
     * @aribaapi ariba
     */
    public static final int FatalExitNoRestart = 2;

    /*
     * Shutdown statuses of the system (cluster).
     */
    /** System is in normal running state. */
    public static final int StatusSystemRunning = 0;
    /** System is in Rolling Upgrade. */
    public static final int StatusSystemRollingUpgrade = 1;
    /** System is in Rolling Restart. */
    public static final int StatusSystemRollingRestart = 2;

    public static final String SystemStatusUnknown = "unknown";
    public static final String[] SystemStatusStrings = {
        "Running",
        "RollingUpgrade",
        "RollingRestart"
    };

    /*
     * Shutdown statuses of the current node.
     */
    /** Nodes is in normal running state. */
    public static final int StatusNodeRunning = 0;
    /**
     * Node is in immediate (synchronous, forced) shutdown.  There is
     * no grace period.
     */
    public static final int StatusNodeImmediate = 1;
    /**
     * Node is in shutdown initiated by a Direct Action.
     * Has DirectAction grace period.
     */
    public static final int StatusNodeDirectAction = 2;
    /**
     * Node is in shutdown initiated by a signal.  Has Signal grace period.
     */
    public static final int StatusNodeSignal = 3;
    /**
     * Node is in Auto Recycle shutdown.  Has AutoRecycle grace period.
     */
    public static final int StatusNodeAutoRecycle = 4;

    public static final String NodeStatusUnknown = "unknown";
    public static final String[] NodeStatusStrings = {
        "Running",
        "ImmediateShutdown",
        "DirectActionShutdown",
        "SignalShutdown",
        "AutoRecycleShutdown"
    };

    public static final String DirectActionKey = "DirectAction";
    public static final String SignalKey = "Signal";
    public static final String AutoRecycleKey = "AutoRecycle";
    public static final String FinalShutdownNoticeTopic = "FinalShutdownNoticeTopic";
    public static final String NodeKey = "Node";

    /**
     * Time at which initial "stable" state was determined for current node/JVM.
     * {@code 0} indicates no determination attempted yet.
     * @see #getMetastableSystemStatus(int, int)
     */
    private static AtomicLong initialStableTime = new AtomicLong(0L);
    
    /**
     * Presumed initial cluster state: may be inaccurate during initial cluster start.
     * To get a "stable" version of this, use {@link #getMetastableSystemStatus(int, int)}.
     */
    private int systemStatus = StatusSystemRunning;
    
    /** Current node status: clearly the current node is initially running */
    private int nodeStatus = StatusNodeRunning;
    private static long shutdownStartTime = -1;

    // IMPORTANT: Don't change the line below without talking with someone from Ops !
    // Ops is parsing the message to decide whether this is a normal shutdown or a failure
    private static final String NormalShutdownString = "Exiting normally. Restart me.";
    private ShutdownNotifier notifier = null;

    private static final ShutdownManager Singleton = new ShutdownManager();
    private RecycleManager recycler = null;
    private ShutdownManagerProcessorIfc shutdownManagerProcessor = null;

    private AsyncShutdown asyncShutdown = null;
    private Thread asyncShutdownThread;
    private static final long CancelledShutdownJoinLimit = 30 * Date.MillisPerSecond;


    /**
     * Returns the instance of the ShutdownManager
     * @return the singleton ShutdownManager
     * @aribaapi ariba
     */
    public static ShutdownManager get ()
    {
        return Singleton;
    }

    public RecycleManager getRecycleMgr ()
    {
        return recycler;
    }
    /**
     * Adds a ShutdownDelayer to be invoked during the
     * asynhchronous shutdown
     * @param delayer the ShutdownDelayer to be added
     * @aribaapi ariba
     */
    public static void addShutdownDelayer (ShutdownDelayer delayer)
    {
        get().registerShutdownDelayer(delayer);
    }

    /**
     * Adds a ShutdownHook to be invoked before the exit
     * @param hook the ShutdownHook to be added
     * @aribaapi ariba
     */
    public static void addShutdownHook (Thread hook)
    {
        get().registerShutdownHook(hook);
    }

    /**
     * Adds a ShutdownHook to be invoked before the exit
     * @param hook the ShutdownHook to be added
     * @aribaapi ariba
     */
    public static void addLastShutdownHook (Thread hook)
    {
        get().registerLastShutdownHook(hook);
    }

    /**
     * Changes the ExitHook
     * @param hook the new ExitHook to use
     * @aribaapi ariba
     */
    public static void setExitHook (ExitHook hook)
    {
        get().registerExitHook(hook);
    }

    /**
     * Triggers the start of the asynchronous shutdown sequence.
     * @param exitCode the exit code to return at the end
     * @aribaapi ariba
     */
    public static void shutdown (int exitCode, String type)
    {
        get().delayedShutdown(exitCode, type);
    }

    /**
     * Triggers the synchronous and immediate shutdown sequence
     * @param exitCode the exit code to return
     * @aribaapi private
     */
    public static void forceShutdown (int exitCode)
    {
        get().shutdownNow(exitCode, true);
    }
    
    /**
     * Shutdowns a node so that it can restarted after a transient
     * startup error.  For Ops instances, a normal exit is used,
     * so Ops is not paged.
     * Caller must log an error message before calling this method.
     * @aribaapi private
     */
    public static void restartAfterStartupError ()
    {
        if (get()._printExtraMessages) {
            // For Ops Service, do a shutdown that prints the special
            // message that restarts the node without a page.  
            get().shutdownNow(ShutdownManager.NormalExitNoRestart, true);
        }
        else {
            get().shutdownNow(ShutdownManager.FatalExitRestart, true);
        } 
    }

    private ExitHook _exitHook;
    private final List/*<Thread>*/ _hooks;
    private Thread _lastHook;
    private final List/*<ShutdownDelayer>*/ _delayers;

    private long _hookRunnerTimeout;
    private long _delayRunnerTimeout;
    private Map _times = MapUtil.map();
    private long _pingInterval;
    private ThreadFactory _threadFactory;
    private boolean _printExtraMessages;

    private boolean _shutdownAlreadyRequested = false;
    private String _gracefulShutdownTypeRequested = null;

    private long _shutdownScheduledTime = -1;

    private ShutdownManager ()
    {
        _exitHook = new DefaultVMExit();
        _hooks = ListUtil.list();
        _lastHook = null;
        _delayers = ListUtil.list();
        setHookRunnerTimeout(60);
        setPingInterval(30);
        recycler = new RecycleManager();
    }

    /**
     * Defines the delay for which we wait for all the ShutdownHooks
     * to complete.
     * @param timeout delay in minutes
     * @aribaapi ariba
     */
    public void setHookRunnerTimeout (int timeout)
    {
        _hookRunnerTimeout = minutesToMillis(timeout);
    }

    /**
     * Sets a group of shutdown times for a given key.
     * @param type shutdown type
     * @param gracePeriod overall delay period in minutes
     * @param warningPeriod total period to issue warnings in UI in minutes
     * @param warningInterval interval between UI warnings
     * @aribaapi ariba
     */
    public void setShutdownTimes (String type,
                                  int gracePeriod,
                                  int warningPeriod,
                                  int warningInterval)
    {
        _times.put(type, new ShutdownTimes(gracePeriod, warningPeriod,
                                            warningInterval));
    }

    /**
     * Returns the status code given a shutdown type.
     * @return node shutdown status code.
     * @aribaapi ariba
     */
    public int getNodeStatus (String type)
    {
        if (DirectActionKey.equals(type)) {
            return StatusNodeDirectAction;
        }
        else if (SignalKey.equals(type)) {
            return StatusNodeSignal;
        }
        else if (AutoRecycleKey.equals(type)) {
            return StatusNodeAutoRecycle;
        }
        else {
            Assert.that(false,
                        "unknown shutdown type %s ", type);
        }
        return -1;
    }

    /**
     * Record start of Rolling Upgrade.
     * @aribaapi private
     */
    public void rollingUpgradeBegin ()
    {
        Assert.assertNonFatal(systemStatus == StatusSystemRunning,
            "System shutdown status should be Running to do RollingUpgradeBegin");
        setSystemStatus(StatusSystemRollingUpgrade);
        cancelDelayedShutdown();
    }

    /**
     * Record end of Rolling Upgrade.
     * @aribaapi private
     */
    public void rollingUpgradeEnd ()
    {
        Assert.assertNonFatal(systemStatus == StatusSystemRollingUpgrade,
           "System shutdown status should be RollingUpgrade to do RollingUpgradeEnd");
        setSystemStatus(StatusSystemRunning);
    }

    /**
     * Record start of Rolling Restart.
     * @aribaapi private
     */
    public void rollingRestartBegin ()
    {
        Assert.assertNonFatal(systemStatus == StatusSystemRunning,
            "System shutdown status should be Running to do RollingRestartBegin");
        setSystemStatus(StatusSystemRollingRestart);
        cancelDelayedShutdown();
    }

    /**
     * Record end of Rolling Restart.
     * @aribaapi private
     */
    public void rollingRestartEnd ()
    {
        Assert.assertNonFatal(systemStatus == StatusSystemRollingRestart,
            "System shutdown status should be RollingRestart to do RollingRestartEnd");
        setSystemStatus(StatusSystemRunning);
    }

    /**
     * Returns the milliseconds for which we wait for all the ShutdownDelayers
     * to complete.
     * @return grace period in milliseconds
     * @aribaapi ariba
     */
    public long getGracePeriod ()
    {
        return getShutdownTimes().getGracePeriod();
    }

    /**
     * Returns the millisecond period during which issue shutdown warnings
     * through the UI.
     * @return warning period in milliseconds
     * @aribaapi ariba
     */
    public long getWarningPeriod ()
    {
        return getShutdownTimes().getWarningPeriod();
    }

    /**
     * Returns the millisecond interval between warnings during the warning
     * perdiod.
     * @return warning interval in milliseconds
     * @aribaapi ariba
     * @see ShutdownManager#getWarningPeriod
     */
    public long getWarningInterval ()
    {
        return getShutdownTimes().getWarningInterval();
    }

    /**
     * Defines the interval between queries to the ShutdownDelayer
     * @param interval delay in seconds
     * @aribaapi ariba
     */
    public void setPingInterval (int interval)
    {
        _pingInterval = secondsToMillis(interval);
    }

    private long secondsToMillis (int seconds)
    {
        return seconds * Date.MillisPerSecond;
    }

    private long minutesToMillis (int minutes)
    {
        return minutes * Date.MillisPerMinute;
    }

    private Long millisToMinutes (long millis)
    {
        return Constants.getLong(millis / Date.MillisPerMinute);
    }

    /**
     * Sets the ThreadFactory that will be used for creating
     * new threads as needed during the shutdown sequence
     * @param tf the new ThreadFactory
     * @aribaapi ariba
     */
    public void setThreadFactory (ThreadFactory tf)
    {
        _threadFactory = tf;
    }
    public ThreadFactory getThreadFactory ()
    {
        return _threadFactory;
    }
    /**
     * Specifies whether to print extra messages during the shutdown.
     * The Ops infrastructure looks for specific String to determine
     * whether the server shutdown normally or crashed. These special
     * String are only printed when <code>true</code> is passed to that
     * method
     * @param value if <code>true</code> the extra messages will be printed
     * on the console
     * @aribaapi ariba
     */
    public void setPrintExtraMessages (boolean value)
    {
        _printExtraMessages = value;
    }

    /**
     * Creates and registers a {@link SignalHandler} for the given signal
     * name which will trigger the execution of the asynchronous shutdown
     * @param signal name of the signal to register with
     * @aribaapi ariba
     */
    public void registerSignalHandlerForAsynchronousShutdown (String signal)
    {
        newSignalHandler().registerForAsyncShutdown(signal);
    }

    /**
     * Creates and registers a {@link SignalHandler} for the given signal
     * name which will trigger the execution of the asynchronous shutdown
     * @param signal name of the signal to register with
     * @aribaapi ariba
     */
    public void registerSignalHandlerForSynchronousShutdown (String signal)
    {
        newSignalHandler().registerForSyncShutdown(signal);
    }

    public void registerShutdownManagerProcessor (
        ShutdownManagerProcessorIfc smp)
    {
        Assert.that(shutdownManagerProcessor == null,
                    "shutdownManagerProcessor is not null");
        shutdownManagerProcessor = smp;
    }

    /**
     * Returns the number of milliseconds before we will force the shutdown
     * (unless all ShutdownDelayer let the shutdown proceed).
     * @return the delay in milliseconds before the shutdown
     * @aribaapi ariba
     */
    public long getTimeBeforeShutdown ()
    {
        Assert.that(_shutdownScheduledTime != -1,
                    "getTimeBeforeShutdown cannot be called before " +
                    "a graceful shutdown is requested");
        return _shutdownScheduledTime - System.currentTimeMillis();
    }

    /**
     * Get the ShutdownTimes for the requested shutdown type.
     * @return ShutdownTimes for the current shutdown type
     * @aribaapi ariba
     */
    private ShutdownTimes getShutdownTimes ()
    {
        Assert.that(_gracefulShutdownTypeRequested != null,
                    "request made that depends on shutdown type " +
                    "before a graceful shutdown is requested");
        return
            (ShutdownTimes)_times.get(_gracefulShutdownTypeRequested);
    }

    /**
     * Triggers the start of the asynchronous shutdown sequence.
     * @param exitCode the exit code to return at the end
     * @aribaapi ariba
     */
    private synchronized void delayedShutdown (int exitCode, String type)
    {
        if (systemStatus != StatusSystemRunning &&
            !SignalKey.equals(type)) {
            Log.shutdown.warning(9669,
                                 type,
                                 getSystemStatusAsString(getSystemStatus()));
            return;
        }
        if (_gracefulShutdownTypeRequested != null) {
            Log.shutdown.warning(9262);
            return;
        }
        _gracefulShutdownTypeRequested = type;
        Log.shutdown.info(10573,type, exitCode);
        
        updateNodeStatus(getNodeStatus(type));
        Assert.that(_times.get(_gracefulShutdownTypeRequested) != null,
            "no ShutdownTimes was set for shutdown type %s ",
            _gracefulShutdownTypeRequested);
        _delayRunnerTimeout = getGracePeriod();

        asyncShutdown = new AsyncShutdown(exitCode);
        if (_threadFactory != null) {
            asyncShutdownThread =_threadFactory.createThread(
                asyncShutdown, "ShutdownManager:AsyncShutdown");
        }
        else {
            asyncShutdownThread = new Thread(asyncShutdown,
                                        "ShutdownManager:AsyncShutdown");
        }
        asyncShutdownThread.setPriority(Thread.MIN_PRIORITY);
        setShutdownStartTime(System.currentTimeMillis());
        asyncShutdownThread.start();
    }

    private synchronized void cancelDelayedShutdown ()
    {
        if (_gracefulShutdownTypeRequested == null) {
            return;
        }
        Assert.that(asyncShutdown != null,
                    "ShutdownManager.asyncShutdown should not be null");
        if (asyncShutdown.cancelShutdown()) {
            // We were able to cancel the shutdown.
            try {
                 asyncShutdownThread.join(CancelledShutdownJoinLimit);
            }
            catch (InterruptedException e) {
            }
            if (!asyncShutdown.isFinished()) {
                Log.shutdown.warning(9678);
            }
            // Set ShutdownManager state as if the delayed shutdown never happened.
            _gracefulShutdownTypeRequested = null;
            asyncShutdown = null;
            asyncShutdownThread = null;
            if (nodeStatus != StatusNodeImmediate) {
                updateNodeStatus(StatusNodeRunning);
            }
            Log.shutdown.info(9676);
        }
    }

    /**
     * Triggers the synchronous and immediate shutdown sequence
     * @param exitCode the exit code to return
     * @aribaapi private
     */
    private void shutdownNow (int exitCode, boolean updateNodeStatus)
    {
        synchronized(this) {
            if (_shutdownAlreadyRequested) {
                Log.shutdown.warning(9262);
                return;
            }
            _shutdownAlreadyRequested = true;
            if (updateNodeStatus) {
                updateNodeStatus(StatusNodeImmediate);
            }
        }
        Log.shutdown.info(10575, exitCode, nodeStatus);
        
        //Call the notifier to send out the final shutdown message.
        if ( notifier != null ) {
            notifier.execute();
        }
        ShutdownHookRunner hookRunner = new ShutdownHookRunner();
        Thread hookRunnerThread;
        if (_threadFactory != null) {
            hookRunnerThread =_threadFactory.createThread(
                hookRunner,
                "ShutdownManager:HookRunner");
        }
        else {
            hookRunnerThread = new Thread(
                hookRunner,
                "ShutdownManager:HookRunner");
        }
        hookRunnerThread.start();
        try {
            hookRunnerThread.join(_hookRunnerTimeout);
        }
        catch (InterruptedException e) {
        }

        if (hookRunner.isDone()) {
            Log.shutdown.info(10576);
        }
        else {
            Log.shutdown.warning(9257, millisToMinutes(_hookRunnerTimeout));
        }
        // if exitcode is 0 only then it will print proper message which will be 
        //captured by KR logs to know if the restart is normal.
        Log.shutdown.info(10580, exitCode, Boolean.valueOf(_printExtraMessages));
        if (exitCode == NormalExitNoRestart && _printExtraMessages) {
            SystemUtil.out().println(NormalShutdownString); // OK
        }
        try {
            LogManager.shutdown();
            SystemUtil.flushOutput();
            //wait for 2 sec to get logs flushed before exit
            SystemUtil.sleep(2000);
        }
        catch (Throwable e) { // OK
            // don't want to allow problem in flushOutput to
            // cause a stack unwind
        }
        _exitHook.exit(exitCode);
    }

    /**
     * Set the shutdownNotifier. This is the call back mechanism to send out the final
     * shutdown notification.
     */
    public void setShutdownNotifier (ShutdownNotifier notifier)
    {
        this.notifier = notifier;
    }

    /**
     * Adds a ShutdownDelayer to be invoked during the
     * asynhchronous shutdown
     * @param delayer the ShutdownDelayer to be added
     * @aribaapi ariba
     */
    private void registerShutdownDelayer (ShutdownDelayer delayer)
    {
        _delayers.add(delayer);
    }

    /**
     * Adds a ShutdownHook to be invoked before the exit
     * @param hook the ShutdownHook to be added
     * @aribaapi ariba
     */
    private void registerShutdownHook (Thread hook)
    {
        //thread should not be started already
        if (hook.isAlive()) {
            throw new IllegalArgumentException("Shutdown hook has already been started");
        }
        _hooks.add(hook);
    }

    /**
     * Adds a ShutdownHook to be invoked before the exit
     * @param hook the ShutdownHook to be added
     * @aribaapi ariba
     */
    private void registerLastShutdownHook (Thread hook)
    {
        //thread should not be started already
        if (hook.isAlive()) {
            throw new IllegalArgumentException("Shutdown hook has already been started");
        }
        _lastHook = hook;
    }

    /**
     * Changes the ExitHook
     * @param hook the new ExitHook to use
     * @aribaapi ariba
     */
    private void registerExitHook (ExitHook hook)
    {
        if (hook == null) {
            throw new IllegalArgumentException("ExitHook cannot be null !");
        }
        if (_exitHook != null) {
            Log.shutdown.info(9271, hook, _exitHook);
        }
        _exitHook = hook;
    }

    private synchronized ShutdownSignalHandler newSignalHandler ()
    {
        return new ShutdownSignalHandler(this);
    }

    /**
     * Determine the current system shutdown (cluster) status.
     * During initialization of a node this system shutdown status may be inaccurate for
     *  an interval; use {@link #getMetastableSystemStatus(int, int)} instead to allow
     *  for a delayed response.
     *
     * @return possibly incorrect system status
     */
    public static int getSystemStatus ()
    {
        return get().systemStatus;
    }

    /**
     * Get a meta-stable value of the Shutdown Manager's state.
     * <p/>
     * Since the initialization of nodes may cause a false presumption of "normal running"
     *  ({@link #StatusSystemRunning}) while all the nodes get to the correct stable
     *  state, wait up to the specified number of minutes before returning the shutdown
     *  state of the cluster.
     * Subsequent calls only wait additional time if the largest prior window, plus any
     *  time elapsed since then, is not long enough to cover the new window.
     * If two distinct invocations have differing minute windows, both window requests
     *  will be effectively honored.
     * 
     * @param minuteWindow maximum number of minutes to wait before considering the system
     *  stable; values below {@code 1} treated as {@code 1};
     *  values over {@code 60} treated as {@code 60} (i.e., maximum delay to obtain a
     *  stable sample is an hour)
     * @param samples number of times to sample the shutdown state during window;
     *  values below {@code 2} treated as {@code 2}
     *  values over (the limited value of) <em>2*minuteWindow</em> treated as that value
     *  (i.e., the maximum sample rate is twice per minute)
     * @return stable cluster state
     */
    public static int getMetastableSystemStatus (final int minuteWindow, final int samples)
    {
        final int minuteCount = Math.min(60, Math.max(1, minuteWindow));
        final int sampleCount = Math.min(minuteCount * 2, Math.max(2, samples));
        final long windowTotalTime = Date.MillisPerMinute * minuteCount;
        final long pause = Date.MillisPerMinute * minuteCount / sampleCount;
        final long now = System.currentTimeMillis();
        final long lastStableTime = initialStableTime.get();
        long timeNeeded = Math.min(windowTotalTime, now - lastStableTime);
        
        // since initial state is always StatusSystemRunning, wait for other values
        
        while (timeNeeded > 0L) {
            if (getSystemStatus() != ShutdownManager.StatusSystemRunning) {
                break;
            }
            SystemUtil.sleep(pause);
            timeNeeded -= pause;
        }
        
        // either time is up or there has been a change from StatusSystemRunning

        final int stableStatus = getSystemStatus();
        final long elapsedTime = System.currentTimeMillis() - now;
        
        /*
         * Two threads running concurrently could set initialStableTime to the more recent
         * time, but that would not affect the correctness of the log or the result.  In
         * the worst case, some unneeded delays may be caused in future calls.
         */
        if (lastStableTime == 0L) {
            initialStableTime.set(now);
        }
        Log.shutdown.info(11946,
                ShutdownManager.getSystemStatusAsString(stableStatus),
                Long.valueOf(elapsedTime));
        
        return stableStatus;
    }

    public static String getSystemStatusAsString (int status)
    {
        if (status >= 0 && status < SystemStatusStrings.length) {
            return SystemStatusStrings[status];
        }
        return SystemStatusUnknown;
    }

    public static void setSystemStatus (int status)
    {
        if (Log.shutdown.isInfoEnabled()) {
            Log.shutdown.info(9671,
                getSystemStatusAsString(getSystemStatus()),
                getSystemStatusAsString(status));
        }
        get().systemStatus = status;
    }

    public static int getNodeStatus ()
    {
        return get().nodeStatus;
    }

    public static String getNodeStatusAsString (int status)
    {
        if (status >= 0 && status < NodeStatusStrings.length) {
            return NodeStatusStrings[status];
        }
        return NodeStatusUnknown;
    }

    static void setNodeStatus (int status)
    {
        ShutdownManager t = get();
        synchronized (t) {
            t.updateNodeStatus(status);
        }
    }

    /**
     * Update the node status and have ShutdownManagerProcessor send
     * out notification.
     * Caller should already be synchronized.
     * @param status
     * @aribaapi private
     */
    private void updateNodeStatus (int status)
    {
        if (status != nodeStatus) {
            nodeStatus = status;
            if (shutdownManagerProcessor != null) {
                shutdownManagerProcessor.newNodeStatus(nodeStatus);
            }
        }
    }

    /**
     * Implementation of the asynchronous shutdown
     * @aribaapi private
     */
    private class AsyncShutdown implements Runnable
    {
        private final int _exitCode;
        private boolean cancelShutdown = false;
        private boolean shutdownNow = false;
        private boolean finished = false;

        AsyncShutdown (int exitCode)
        {
            _exitCode = exitCode;
        }

        synchronized boolean cancelShutdown ()
        {
            if (!shutdownNow) {
                Log.shutdown.debug("Cancelling shutdown in AsyncShutdown thread");
                cancelShutdown = true;
                notify();
            }
            return cancelShutdown;
        }

        boolean isFinished ()
        {
            return finished;
        }

        public void run ()
        {
            Log.shutdown.info(9264);
            _shutdownScheduledTime = System.currentTimeMillis() + _delayRunnerTimeout;


            for (Iterator i = _delayers.iterator(); i.hasNext();) {
                ShutdownDelayer delayer = (ShutdownDelayer)i.next();
                try {
                    delayer.initiateShutdown();
                }
                catch (Throwable t) { // OK
                    Log.shutdown.warning(9260, delayer, SystemUtil.stackTrace(t));
                    i.remove();
                }
            }
            boolean canShutdown = false;
            while (!canShutdown && !cancelShutdown &&
                   System.currentTimeMillis() < _shutdownScheduledTime) {
                canShutdown = true;
                for (Iterator i = _delayers.iterator(); i.hasNext();) {
                    ShutdownDelayer delayer = (ShutdownDelayer)i.next();
                    try {
                        if (!delayer.canShutdown()) {
                            canShutdown = false;
                            // there used to be a break here, but we don't want
                            // to starve shutdown delayers (like AWConcreteApplication)
                            // that use canShutdown calls for side effects
                            Log.shutdown.info("Shutdown pending: waiting for delayer- "
                                   + delayer.toString());
                        }
                    }
                    catch (Throwable t) { // OK
                        Log.shutdown.warning(9261, delayer, SystemUtil.stackTrace(t));
                        i.remove();
                    }
                }
                if (!canShutdown) {
                    synchronized (this) {
                        try {
                            wait(_pingInterval);
                        }
                        catch (InterruptedException e) {
                        }
                    }
                }
            }
            synchronized (this) {
                if (!cancelShutdown) {
                    // Too late to cancel shutdown, starting shutdownNow().
                    shutdownNow = true;
                }
            }
            if (cancelShutdown) {
                // Tell shutdown delayers that shutdown is cancelled.
                for (Iterator i = _delayers.iterator(); i.hasNext();) {
                    ShutdownDelayer delayer = (ShutdownDelayer)i.next();
                    try {
                        delayer.cancelShutdown();
                    }
                    catch (Throwable t) { // OK
                        Log.shutdown.warning(9675, delayer, SystemUtil.stackTrace(t));
                        i.remove();
                    }
                }
            }
            else {
                if (!canShutdown &&
                    System.currentTimeMillis() >= _shutdownScheduledTime) {
                    Log.shutdown.warning(
                        9258, millisToMinutes(_delayRunnerTimeout));
                }
                // Continue with immediate shutdown.
                Log.shutdown.info(10574);
                shutdownNow(_exitCode, false);
            }
            finished = true;
        }
    }

    /**
     * Thread which runs the ShutdownHooks
     * @aribaapi private
     */
    private class ShutdownHookRunner implements Runnable
    {
        private boolean _done;

        boolean isDone ()
        {
            return _done;
        }

        public void run ()
        {
            Log.shutdown.info(10581);
            for (Iterator i = _hooks.iterator(); i.hasNext();) {
                Thread hook = (Thread)i.next();
                hook.start();
            }

            for (Iterator i = _hooks.iterator(); i.hasNext();) {
                Thread hook = (Thread)i.next();
                try {
                    Log.shutdown.info(10582,ClassUtil.getClassNameOfObject(hook));
                    hook.join();
                }
                catch (InterruptedException e) {
                }
            }
            if (_lastHook != null) {
                Log.shutdown.info(10577);
                _lastHook.start();
                try {
                    _lastHook.join();
                }
                catch (InterruptedException e) {
                }
            }
            Log.shutdown.info(10583,ClassUtil.getClassNameOfObject(_lastHook));
            _done = true;
        }
    }

    private class ShutdownTimes
    {
        private long _gracePeriod;
        private long _warningPeriod;
        private long _warningInterval;

        public ShutdownTimes (int graceP, int warningP, int warningI)
        {
            _gracePeriod = minutesToMillis(graceP);
            _warningPeriod = minutesToMillis(warningP);
            _warningInterval = minutesToMillis(warningI);
        }

        public long getGracePeriod ()
        {
            return _gracePeriod;
        }

        public long getWarningPeriod ()
        {
            return _warningPeriod;
        }

        public long getWarningInterval ()
        {
            return _warningInterval;
        }
    }

    public static long getShutdownStartTime ()
    {
        return shutdownStartTime;
    }
    static void setShutdownStartTime (long timeMillis)
    {
        shutdownStartTime = timeMillis;
    }
}
