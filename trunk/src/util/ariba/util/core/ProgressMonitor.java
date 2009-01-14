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

    $Id: //ariba/platform/util/core/ariba/util/core/ProgressMonitor.java#2 $
*/

package ariba.util.core;

import java.util.Map;

/**
    This class stores the user-visible progress information for the current
    thread so that it can be queried / reflected by a progress panel in the UI.

    The state is updated by the active current thread and is registered in a global
    Map keyed by SessionId so that progress polling (from another thread) can get the
    latest state.

    @aribaapi ariba
*/
public class ProgressMonitor
{
    // Thread Local -- instance for the current thread
    private static final State state = StateFactory.createState();

    // registry of currently active thread Monitors
    private static final Map _MonitorsByKey = MapUtil.map();

    public static ProgressMonitor instance()
    {
        ProgressMonitor cur = (ProgressMonitor)state.get();
        if (cur == null) {
            cur = new ProgressMonitor();
            state.set(cur);
        }
        return cur;
    }

    public static void register (String key)
    {
        ProgressMonitor cur = instance();
        if (cur._key != null && !key.equals(cur._key)) {
            Assert.assertNonFatal(false, "Registering ProcessMonitor under different key (%s <-> %s) -- " +
                    "possibly failed to clear thread state?", key, cur._key);
            _MonitorsByKey.remove(cur._key);
        }
        cur._key = key;
        _MonitorsByKey.put(key, instance());
    }

    /*
        Called by AW direct action to get status (i.e. by different thread).

        This is always for the latest request (which may be one that is blocked
        on the session lock waiting for the previous long running request from that 
        user to complete
     */
    public static ProgressMonitor getInstanceForKey (String key)
    {
        return (ProgressMonitor)_MonitorsByKey.get(key);
    }

    // Called by ThreadDebugState -- we rely on application's existing calls to clear that to
    // establish our lifecycle
    protected static void internalClear ()
    {
        ProgressMonitor cur = instance();
        // if another thread didn't already supercede us for this key, then remove my state
        if (cur._key != null && (getInstanceForKey(cur._key) == cur)) _MonitorsByKey.remove(cur._key);
        state.set(null);
    }

    /*
        Instance state
     */
    protected String _key;

    protected int _currentCount;
    protected int _totalCount;
    protected String _messageFormatString;

    protected ProgressMonitor ()
    {
        _messageFormatString = "";
    }

    /*
        Set the message and total and reset the count.
        This is the preferred way to set up for reporting operation status.
     */
    public synchronized void prepare (String formatString, int total)
    {
        // synchronized because could overlap with generateMessage call made by other thread
        _messageFormatString = formatString;
        _totalCount = total;
        _currentCount = 0;
    }

    public synchronized String generateMessage ()
    {
        // not strictly thread-safe but harmless
        return Fmt.S(_messageFormatString, Integer.toString(_currentCount), Integer.toString(_totalCount));
    }

    public void incrementCurrentCount ()
    {
        // no need to syncronize -- worst case a call to generateMessage gets a stale value
        _currentCount++;
    }

    public String getMessageFormatString ()
    {
        return _messageFormatString;
    }

    public void setMessageFormatString (String messageFormatString)
    {
        _messageFormatString = messageFormatString;
    }

    public int getCurrentCount()
    {
        return _currentCount;
    }

    public void setCurrentCount (int count)
    {
        _currentCount = count;
    }

    public int getTotalCount ()
    {
        return _totalCount;
    }

    public void setTotalCount (int total)
    {
        _totalCount = total;
    }
}
