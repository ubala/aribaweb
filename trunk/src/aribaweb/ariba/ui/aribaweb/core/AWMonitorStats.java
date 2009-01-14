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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWMonitorStats.java#15 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.Log;
import java.util.Date;

// subclassed by AWWOMonitorStats
public class AWMonitorStats extends AWBaseObject
{
    private boolean _isRefusingNewSessions;
    private boolean _isInGracefulShutdown;
    private long _remainingShutdownPeriod;
    private boolean _isInShutdownWarningPeriod;
    private int _activeSessionCount;
    private int _totalSessionsServed;
    private int _totalRequestsServed;
    private int _terminatedSessionCount;
    private final Date _upSince;

    public AWMonitorStats ()
    {
        super();
        _upSince = new Date();

    }

    public void setIsRefusingNewSessions (boolean flag)
    {
        _isRefusingNewSessions = flag;
    }

    public void setGracefulShutdown (boolean flag)
    {
        _isInGracefulShutdown = flag;
    }

    public synchronized void incrementActiveSessionCount ()
    {
        Log.aribaweb_monitor_sessionCount.debug("--- increment active count");

        _activeSessionCount++;
        _totalSessionsServed++;
    }

    public synchronized void decrementActiveSessionCount ()
    {
        Log.aribaweb_monitor_sessionCount.debug("--- decrement active count");
        _activeSessionCount--;
    }

    public synchronized void incrementMarkedForTerminationSessionCount ()
    {
        Log.aribaweb_monitor_sessionCount.debug("--- increment terminate count");
        Log.logStack(Log.aribaweb_shutdown);

        _terminatedSessionCount++;
    }

    public synchronized void decrementMarkedForTerminationSessionCount ()
    {
        Log.aribaweb_monitor_sessionCount.debug("--- decrement terminate count");
        Log.logStack(Log.aribaweb_shutdown);

        _terminatedSessionCount--;
    }

    public synchronized void incrementTotalRequestsServed ()
    {
        _totalRequestsServed++;
    }

    ////////////////
    // Public
    ////////////////
    public boolean isRefusingNewSessions ()
    {
        return _isRefusingNewSessions;
    }

    public boolean isGracefulShutdown ()
    {
        return _isInGracefulShutdown;
    }

    public void setRemainingShutdownPeriod (long remainingShutdownPeriod)
    {
        _remainingShutdownPeriod = remainingShutdownPeriod;
    }

    public long remainingShutdownPeriod ()
    {
        return _remainingShutdownPeriod;
    }

    public void setIsInShutdownWarningPeriod (boolean isInShutdownWarningPeriod)
    {
        _isInShutdownWarningPeriod = isInShutdownWarningPeriod;
    }

    public boolean isInShutdownWarningPeriod ()
    {
        return _isInShutdownWarningPeriod;
    }

    public int activeSessionCount ()
    {
        return _activeSessionCount - _terminatedSessionCount;
    }

    public int totalSessionsServed ()
    {
        return _totalSessionsServed;
    }

    public int totalRequestsServed ()
    {
        return _totalRequestsServed;
    }

    public Date upSince ()
    {
        return _upSince;
    }

    public int upTimeSeconds ()
    {
        long currentMillis = System.currentTimeMillis();
        long initialMillis = _upSince.getTime();
        return (int)(currentMillis - initialMillis) / 1000;
    }
}
