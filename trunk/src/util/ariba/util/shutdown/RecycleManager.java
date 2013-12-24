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

    $Id: //ariba/platform/util/core/ariba/util/shutdown/RecycleManager.java#10 $
*/

package ariba.util.shutdown;

import ariba.util.core.Assert;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.GlobalLock;
import ariba.util.core.GlobalLocking;
import ariba.util.core.GlobalLockingException;
import ariba.util.core.GlobalLockingService;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;
import java.util.List;
import java.util.Random;

/**
 * This class is responsible for managing the auto recycle process
 * To participate ( control, get notification, etc) in the recycle process
 * participants should implement the RecylceIfc and register with this class.
 * @aribaapi ariba
 */
public class RecycleManager
{
    public static final String RecycleLock = "RecyleLock_";

    private static final int Infinite = 365 * 24 * 60;
    private String globalLockName = RecycleLock;
    // the maximum taskcount before recycle
    // setting to -1 ( disabled ) unless set by parameter.
    private int maxTaskCount = -1;
    private final List agents = ListUtil.list();
    private long recycleInterval = -1;
    private int recycleWindowBeginHour = 18;
    private int recycleWindowEndHour = 20;

    public synchronized void registerRecycleAgent (RecycleIfc agent)
    {
        ListUtil.addElementIfAbsent(agents, agent);
    }

    public synchronized void removeRecycleAgent (RecycleIfc agent)
    {
        agents.remove(agent);
    }

    /**
     * Checks to see if a auto-recycle can done.
     * @param doWindowCheck if true, checks to see if we are in recycle time window.
     * @aribaapi private
     */
    public synchronized void initiateRecycle (boolean doWindowCheck)
    {
        if (!canAutoRecycle(doWindowCheck)) {
            return;
        }
        // get Global lock and check if safeToRecycle
        GlobalLockingService service = GlobalLocking.getService();
        GlobalLock lock = null;
        try {
            lock = service.acquireLock(globalLockName);
            if (lock == null) {
                // do nothing. We will try to recycle later.
                Log.shutdown.debug("Could not acquire global lock");
                return;
            }
            // Iterate through all the agents
            // If any of them says not safe to recycle, just return aborting
            // the recycle
            for (int i = 0; i < agents.size(); i++) {
                RecycleIfc agent = (RecycleIfc)agents.get(i); 
                if (agent.isSafeToRecycle() == false) {
                    Log.shutdown.debug("Auto recycle pending: waiting for agent: %s",
                                       agent);
                    return;
                }
            }
            // Since some agents can be long running, check again if we can auto recycle.
            if (!canAutoRecycle(doWindowCheck)) {
                return;
            }
            // Starting auto recycle shutdown.
            Log.shutdown.info(9831);
            ShutdownManager.setNodeStatus(ShutdownManager.StatusNodeAutoRecycle);
        }
        finally {
            if ( lock != null && lock.isValid()) {
                releaseLock(lock);
            }
        }

        for (int i = 0; i < agents.size(); i++) {
            if (((RecycleIfc)agents.get(i)).initiateRecycle() == false) {
                Log.shutdown.debug("An error occurred while preparing for Recycle");
            }
        }
        ShutdownManager.get().setShutdownTimes(ShutdownManager.AutoRecycleKey,
                                               Infinite, 0, 0);
        ShutdownManager.shutdown(ShutdownManager.NormalExitNoRestart,
                                 ShutdownManager.AutoRecycleKey);
    }

    /**
     * Check if system and node status allow auto recycle.
     * @return true if we can auto recycle.
     * @param doWindowCheck if true, checks to see if we are in recycle time window.
     * @aribaapi private
     */
    private boolean canAutoRecycle (boolean doWindowCheck)
    {
        int systemStatus = ShutdownManager.getSystemStatus();
        int nodeStatus = ShutdownManager.getNodeStatus();

        // If system or node is already in shutdown, cannot auto recycle.
        if (systemStatus != ShutdownManager.StatusSystemRunning ||
            nodeStatus != ShutdownManager.StatusNodeRunning) {
            Log.shutdown.debug("Cannot auto recycle, shutdown system status=%s," +
                               " shutdown node status=%s", systemStatus, nodeStatus);
            return false;
        }

        if (doWindowCheck && !inRecycleWindow()) {
            Log.shutdown.debug("Cannot auto recycle because current time is not" +
                               " in the recycle window of %s to %s hours",
                               recycleWindowBeginHour, recycleWindowEndHour);
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if the current time is in the auto-recycle window.
     * @return true if in the recycle window.
     * @aribaapi private
     */
    private boolean inRecycleWindow ()
    {
        int hour = Date.getHours(new Date(System.currentTimeMillis()));
        return inRecycleWindow(hour);
    }
    
    /**
     * Checks if the hour is in the auto-recycle window.
     * @param hour the hour within a day.  Relative to zero.
     * @return true if in the recycle window.
     * @aribaapi private
     */
    private boolean inRecycleWindow (int hour)
    {
        if (recycleWindowBeginHour > recycleWindowEndHour) {
            return (recycleWindowBeginHour <= hour || hour < recycleWindowEndHour);
        }
        else {
            return (recycleWindowBeginHour <= hour && hour < recycleWindowEndHour);
        }
    }
    
    /**
     * Calculates the time interval to next recycle check.  The interval
     * is the higher of: the minInterval parameter, or the interval to the
     * beginning of the next recycle window.  If the current
     * time is in the recycle window, minInterval is returned.
     * @param minInterval the minimum time to the next recycle check
     *        in milliseconds.  Must be >= 0 and less than a hour.
     * @return the interval to wait before checking for auto-recycle.
     * @aribaapi private
     */
    public long intervalToNextRecycleCheck (long minInterval)
    {
        Assert.that(minInterval >= 0 && minInterval < Date.MillisPerHour, 
                    "minInterval must be positive and less than one hour");
        
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime);

        // If current time in recycle window, return minimum interval.
        int hour = Date.getHours(date);
        if (inRecycleWindow(hour)) {
            return minInterval;
        }
        
        // Set date to the recycle window begin hour on current day.  We add 1 second 
        // to avoid rounding errors.  Compute interval to next begin time.  If begin hour
        // is before current time, interval will be negative, so add a day to adjust.
        Date.setHoursMinutesSeconds(date, recycleWindowBeginHour, 0, 1);
        long interval = date.getTime() - currentTime;
        interval = (interval >= 0) ? interval : interval + Date.MillisPerDay;
        Assert.assertNonFatal(interval >= 0, Fmt.S("Got negative interval %s", interval));
        // Add a random value between 0 and 10 minutes to make it less likely that two
        // nodes will try at the very beginning of the recycle window.
        interval += (new Random().nextInt(600)) * Date.MillisPerSecond;
        return Math.max(minInterval, interval);
    }

    private void releaseLock (GlobalLock lock)
    {
        try {
            lock.release();
        }
        catch (GlobalLockingException e) {
            Log.shutdown.debug(e.getMessage());
        }
    }
    /**
     * Set the global lockname using the community id
     */
    public void setGlobalLockName (int id )
    {
        globalLockName = RecycleLock+Integer.toString(id);
    }

    // This is kinda ugly, but I don't see any other way to read the system
    // Parameters from this package. So this will be set from server class
    // during initialization.
    public void setMaxTaskCount (int count)
    {
        maxTaskCount =  count;
    }
    public int getMaxTaskCount ()
    {
        return maxTaskCount;
    }

    public void setRecycleInterval (long time)
    {
        recycleInterval  = time;
    }
    public long getRecycleInterval ()
    {
        return recycleInterval;
    }
    
    public void setRecycleWindowBeginHour (int count)
    {
        count = (count >= 0) ? count : 0;
        recycleWindowBeginHour = (count <= 23) ? count : 23;
    }
    public int getRecycleWindowBeginHour ()
    {
        return recycleWindowBeginHour;
    }
    
    public void setRecycleWindowEndHour (int count)
    {
        // We allow a end time of 24 so you can set begin time to 0 and end time to 24
        // to specify a window of all day.
        count = (count >= 0) ? count : 0;
        recycleWindowEndHour = (count <= 24) ? count : 24;
    }
    public int getRecycleWindowEndHour ()
    {
        return recycleWindowEndHour;
    }
}
