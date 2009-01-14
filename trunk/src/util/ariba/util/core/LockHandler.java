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

    $Id: //ariba/platform/util/core/ariba/util/core/LockHandler.java#6 $
*/

package ariba.util.core;

import ariba.util.log.Log;

/**
    @aribaapi private
*/
public class LockHandler
{
    /* Options a la bit-wise or */
    public static final int OptionNone              = 0;
    public static final int OptionContinueOnTimeout = 1;
    public static final int OptionWarnOnTimeout     = 2;

    public static final int OptionDefault = (OptionContinueOnTimeout |
                                             OptionWarnOnTimeout);

    LockHandlerConditions   conditions;
    public Object           lock;

    // List waiterList = ListUtil.list();

    public String           name;
    int                     options = OptionNone;

    int                     waiters = 0;
    int                     maxWaiters = 0;
    int                     sumRequests = 0;
    int                     syncCount = 0;
    int                     sumWaitTimeSeconds = 0;

    public LockHandler (String                name,
                        LockHandlerConditions conditions,
                        Object                lock,
                        int                   options)
    {
        this.name = name;
        this.lock = lock;
        this.conditions = conditions;
        this.options = options;
    }

    public LockHandler (String                name,
                        LockHandlerConditions conditions,
                        int                   options)
    {
        this.name = name;
        this.lock = new Object();
        this.conditions = conditions;
        this.options = options;
    }

    public int sumWaitTimeSeconds ()
    {
        synchronized (this.lock) {
            return this.sumWaitTimeSeconds;
        }
    }

    public int maxWaiters ()
    {
        synchronized (this.lock) {
            return this.waiters;
        }
    }

    public int sumRequests ()
    {
        synchronized (this.lock) {
            return this.sumRequests;
        }
    }

    public int syncCount ()
    {
        synchronized (this.lock) {
            return this.syncCount;
        }
    }

    public void resetMetrics ()
    {
        synchronized (this.lock) {
            this.sumWaitTimeSeconds = 0;
            this.maxWaiters = 0;
            this.sumRequests = 0;
            this.syncCount = 0;
        }
    }

    public boolean doWithLock ()
    {
        return this.doWithLock(new LockHandlerContext());
    }

    public boolean doWithLock (LockHandlerContext lockHandlerContext)
    {
        long loopStartTimeMillis;
        int  loopWaitTimeSeconds;

        lockHandlerContext.waitTimeSeconds = 0;
        boolean endLock;

        boolean inWait = false;

        // LockWaiter lockWaiter = null;

        for (lockHandlerContext.iteration = 0;
             ;
             lockHandlerContext.iteration++)
        {
            synchronized (this.lock) {
                this.syncCount++;
                lockHandlerContext.logger = null;
                endLock = this.conditions.doWithLock(lockHandlerContext);
                if (endLock) {
                    if (inWait) {
                        this.waiters--;
                        // this.waiterList.remove(lockWaiter);
                    }
                    this.sumRequests++;
                    break;
                }
                    // if this is the first wait then update our bookeeping
                if (!inWait) {
                    inWait = true;
                    this.waiters++;

                    // lockWaiter = new LockWaiter(Thread.currentThread();
                    // this.waiterList.add(lockWaiter);

                    if (this.waiters > this.maxWaiters) {
                        this.maxWaiters = this.waiters;
                    }
                }

                    // basically wait for notify or timeout and do
                    // some book keeping to track excessive wait time
                loopStartTimeMillis = System.currentTimeMillis();

                try {
                    this.lock.wait(conditions.timeoutIntervalMillis());
                }
                catch (InterruptedException e) {
                    Log.util.warning(2799, this.name);
                }
                loopWaitTimeSeconds = (int)
                    ((System.currentTimeMillis() - loopStartTimeMillis)
                      / Date.MillisPerSecond);
                this.sumWaitTimeSeconds += loopWaitTimeSeconds;
            }

                // during doWithLock the implementor may have wished
                // to log a message - to avoid Logging while holding a
                // lock we do it here
            if (lockHandlerContext.logger != null) {
                lockHandlerContext.logger.debug(
                    lockHandlerContext.message);
            }

            lockHandlerContext.waitTimeSeconds += loopWaitTimeSeconds;
            if (lockHandlerContext.waitTimeSeconds >=
                this.conditions.timeoutIntervalMillis()/Date.MillisPerSecond)
            {
                if  ((this.options & OptionWarnOnTimeout)!=0) {
                    Log.util.warning(2800,
                                     this.name,
                                     Integer.toString(
                                         (int)(conditions.timeoutIntervalMillis()
                                               /Date.MillisPerSecond)),
                                     Integer.toString(
                                         lockHandlerContext.waitTimeSeconds),
                                     this,
                                     Integer.toString(
                                         lockHandlerContext.iteration));
                }
                if ((this.options & OptionContinueOnTimeout)==0) {
                    this.sumRequests++;
                    return false;
                }
            }
        }
            // we only get here when doWithLock returns true

        if (lockHandlerContext.logger != null) {
            lockHandlerContext.logger.debug(
                lockHandlerContext.message);
        }
        return true;
    }

    public String toString ()
    {
        return
            Fmt.S("(LockHandler, name:(%s), waiters:(%s), " +
                  "maxWaiters:(%s), sumRequests:(%s), " +
                  "syncCount:(%s), sumWaitTimeSeconds:(%s)",
                  this.name,
                  Integer.toString(this.waiters),
                  Integer.toString(this.maxWaiters),
                  Integer.toString(this.sumRequests),
                  Integer.toString(this.syncCount),
                  Integer.toString(this.sumWaitTimeSeconds));
    }
}
