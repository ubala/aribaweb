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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWCheckoutManager.java#10 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import java.util.Map;
import ariba.util.core.Constants;
import ariba.util.core.ThreadDebugState;

/**
    This class implements a check out mechanism that allows
    threads to do an exclusive "checkout" on a key value. 
    
    Threads that attempt to check out a key value that is already checked 
    out will wait until the key is checked back in and available. 
    Threads that have waited beyond the timeout period will throw an exception.
*/

public final class AWCheckoutManager extends AWBaseObject
{
    private static final int MaxWaitingThreadsPerKey = 5;
    private static final long MaxThreadWaitMillis = 5 * 60 * 1000;

    private final AWCountingHashtable _waitingThreadForKeyCount =
        new AWCountingHashtable();
    private final Map<Object, Thread> _checkedOutKeys = MapUtil.map();
    private int _maxWaitingThreadsPerKey = MaxWaitingThreadsPerKey;
    private long _maxThreadWaitMillis = MaxThreadWaitMillis;

    private String _instanceName;
    
    public AWCheckoutManager (String instanceName)
    {
        super();
        _instanceName = instanceName;
    }

    ///////////////
    // Threasholds
    ///////////////
    public synchronized void setMaxWaitingThreads (int maxWaitingThreadsPerKey)
    {
        _maxWaitingThreadsPerKey = maxWaitingThreadsPerKey;
    }

    public int maxWaitingThreads ()
    {
        return _maxWaitingThreadsPerKey;
    }

    public synchronized void setMaxThreadWaitMillis (long maxThreadWaitMillis)
    {
        _maxThreadWaitMillis = maxThreadWaitMillis;
    }

    public long maxThreadWaitMillis ()
    {
        return _maxThreadWaitMillis;
    }

    ///////////////
    // Checkin/out
    ///////////////
    public synchronized void checkin (Object key)
    {
        _checkedOutKeys.remove(key);
        if (_waitingThreadForKeyCount.count(key) != 0) {
            // only call notify if there are
            // threads actually waiting.
            this.notifyAll();
        }
    }

    public synchronized void checkout (Object key)
    {
        if (_checkedOutKeys.get(key) != null) {
            Assert.that(_checkedOutKeys.get(key) != Thread.currentThread(),
                        "Recursive call to AWCheckoutManager.checkout() detected.");
            // We only enter this if a thread already has the key checked out.
            if (_waitingThreadForKeyCount.count(key) >= _maxWaitingThreadsPerKey) {
                Log.aribaweb.warning(9370, ThreadDebugState.makeString());
                throw new AWMaxWaitingThreadException("instance " +
                    _instanceName + " key " + key);
            }
            _waitingThreadForKeyCount.add(key);
            try {
                long checkoutDeadline = System.currentTimeMillis() + _maxThreadWaitMillis;
                Thread checkedOutThread = _checkedOutKeys.get(key);
                while (checkedOutThread != null) {
                    waitForTimeout();
                    if (System.currentTimeMillis() > checkoutDeadline) {
                        throwThreadTimeoutException(key, checkedOutThread);
                    }
                    checkedOutThread = _checkedOutKeys.get(key);
                }
            }
            finally {
                _waitingThreadForKeyCount.remove(key);
            }
        }
        _checkedOutKeys.put(key, Thread.currentThread());
    }

    private void throwThreadTimeoutException (Object key, Thread checkedOutThread)
    {
        String stackStr = "No Stack";
        StackTraceElement[] stack = checkedOutThread.getStackTrace();
        if (stack != null) {
            FastStringBuffer sb = new FastStringBuffer();
            for (StackTraceElement line: stack) {
                sb.append("\n\t");
                sb.append(line);
            }
            stackStr = sb.toString();
        }
        String message = Fmt.S("instance: %s, key: %s, checked out thread stack: %s",
            _instanceName, key, stackStr);
        throw new AWThreadTimeoutException(message);
    }

    public synchronized boolean isCheckedOut (Object key)
    {
        if (key == null) {
            return false;
        }
        return _checkedOutKeys.get(key) != null;
    }

    private void waitForTimeout ()
    {
        // This will wait until _maxThreadWaitMillis or
        // until notified from the checkin method
        try {
            this.wait(_maxThreadWaitMillis);
        }
        catch (InterruptedException exception) {
            // swallow the exception
            exception = null;
        }
    }
}

/**
    Implements a store of counters accessed via a hashkey, which
    can be incremented and decremented. The counters are added 
    to the hashtable on the first add, and removed when the value
    reaches 0.
    
    Thread safety: Access to this object needs to be synchronized.
*/

final class AWCountingHashtable extends AWBaseObject
{
    private final Map _hashtable;

    public AWCountingHashtable ()
    {
        _hashtable = MapUtil.map();
    }

    public int add (Object key)
    {
        Integer count = (Integer)_hashtable.get(key);
        int resultingCount = count == null ? 1 : count.intValue() + 1;
        count = Constants.getInteger(resultingCount);
        _hashtable.put(key, count);
        return resultingCount;
    }

    public int remove (Object key)
    {
        int resultingCount = 0;
        Integer count = (Integer)_hashtable.get(key);
        if (count != null) {
            resultingCount = count.intValue() - 1;
            if (resultingCount > 0) {
                count = Constants.getInteger(resultingCount);
                _hashtable.put(key, count);
            }
            else if (resultingCount < 0) {
                throw new AWGenericException("internal error: count < 0: " +
                    resultingCount);
            }
            else {
                _hashtable.remove(key);
            }
        }
        else {
            throw new AWGenericException("attempt to remove non-existent key: " + key);
        }
        return resultingCount;
    }

    public int count (Object key)
    {
        Integer count = (Integer)_hashtable.get(key);
        return count == null ? 0 : count.intValue();
    }
}
