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

    $Id: //ariba/platform/util/core/ariba/util/core/LockManager.java#5 $
*/


package ariba.util.core;

import java.util.List;

/**
    @aribaapi private
*/
public class LockManager
{
    private List list = ListUtil.list();

    public LockManager ()
    {
    }

    public int sumRequests = 0;
    public int maxWaiters = 0;
    public int sumWaitTimeSeconds = 0;
    public int sumSync = 0;

    public void updateMetrics ()
    {
        LockHandler [] copy = this.list();
        for (int idx = 0; idx < copy.length; idx++) {
            LockHandler cursor = copy[idx];
            this.copyMetrics(cursor);
            cursor.resetMetrics();
        }
    }

    private void copyMetrics (LockHandler lockHandler)
    {
        this.sumWaitTimeSeconds += lockHandler.sumWaitTimeSeconds();
        this.maxWaiters += lockHandler.maxWaiters();
        this.sumRequests += lockHandler.sumRequests();
        this.sumSync += lockHandler.syncCount();
    }

    public void clearMetrics ()
    {
        this.maxWaiters = 0;
        this.sumWaitTimeSeconds = 0;
        this.sumRequests = 0;
        this.sumSync = 0;
    }

    public LockHandler createLockHandler (String                name,
                                          LockHandlerConditions conditions,
                                          Object                lock,
                                          int                   options)
    {
        LockHandler lockHandler = new LockHandler(name,
                                                  conditions,
                                                  lock,
                                                  options);
        this.add(lockHandler);
        return lockHandler;
    }

    public LockHandler createLockHandler (String                name,
                                          LockHandlerConditions conditions,
                                          int                   options)
    {
        LockHandler lockHandler = new LockHandler(name, conditions, options);
        this.add(lockHandler);
        return lockHandler;
    }

    public void destroyLockHandler (LockHandler lockHandler)
    {
        this.remove(lockHandler);
    }

    public void add (LockHandler lockHandler)
    {
        synchronized (this.list) {
            ListUtil.addElementIfAbsent(this.list, lockHandler);
        }
    }

    public void remove (LockHandler lockHandler)
    {
        synchronized (this.list) {
            this.copyMetrics(lockHandler);
            this.list.remove(lockHandler);
        }
    }

    public LockHandler [] list ()
    {
        LockHandler [] elementArray;
        synchronized (this.list) {
            elementArray = new LockHandler[this.list.size()];
            this.list.toArray(elementArray);
        }
        return elementArray;
    }

}
