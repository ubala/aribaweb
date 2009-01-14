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

    $Id: //ariba/platform/util/core/ariba/util/core/Pool.java#6 $
*/

package ariba.util.core;

/**
    Generic resource pooling mechanism

    @aribaapi private
*/
public class Pool implements LockHandlerConditions
{
    private String       name;
    private LockHandler  lockHandler;
    private LinkableList freeList    = new LinkableList();

        // inUse list is a memory leak if callers forget to release
        // items - due to an exception for example.
        //private LinkableList inUseList   = new LinkableList();

    private int         inUseCount = 0;

    public int          allocationWaitTimeSeconds = 0;
    public int          allocationSum             = 0;
    public int          allocationMiss            = 0;

    private static final int TimeoutIntervalSeconds = 10;
    private static final long TimeoutIntervalMillis =
        TimeoutIntervalSeconds * Date.MillisPerSecond;

    public Pool (String name)
    {
        this.name = name;
        this.lockHandler = new LockHandler(this.name,
                                           this,
                                           LockHandler.OptionDefault);
    }

    public String toString ()
    {
        int freeListSize;
        int inUseListSize;
        int allocations;
        int miss;
        int waitTime;
        synchronized (this.lockHandler.lock) {
            freeListSize = this.freeList.size();
            inUseListSize = this.inUseCount;
            allocations = this.allocationSum;
            miss = this.allocationMiss;
            waitTime = this.lockHandler.sumWaitTimeSeconds;
        }
        return
            Fmt.S("Pool (%s): free: (%s), in-use: (%s), allocations: (%s), " +
                  "wait time: (%s), misses: (%s)",
                  this.name,
                  Integer.toString(freeListSize),
                  Integer.toString(inUseListSize),
                  Integer.toString(allocations),
                  Integer.toString(waitTime),
                  Integer.toString(miss));
    }

    public int inUseCount ()
    {
        synchronized (this.lockHandler.lock) {
            return inUseCount;
        }
    }

    public int freeCount ()
    {
        synchronized (this.lockHandler.lock) {
            return this.freeList.size();
        }
    }

    public int size ()
    {
        synchronized (this.lockHandler.lock) {
            return this.freeList.size() + inUseCount;
        }
    }

    /**
        Add a new resource to the pool.
    */
    public void add (Linkable linkable)
    {
        synchronized (this.lockHandler.lock) {
            this.freeList.insert(linkable);
        }
    }

    /**
        Remove a resource from the pool. Note, you can only remove
        something from the list if you currently have it allocated.
    */
    public void remove (Linkable linkable)
    {
        synchronized (this.lockHandler.lock) {
            inUseCount--;
        }
    }

    /**
        Allocate a resource for our use. Write diagnotics periodically
        when the lock is held for longer than the timeout.
    */
    public Linkable allocate ()
    {
            // synchronized (this.lockHandler.lock) { - is this needed?
        LockHandlerContext result = new LockHandlerContext();
        this.lockHandler.doWithLock(result);
        return (Linkable)result.payload;
            // }
    }

    public boolean doWithLock (LockHandlerContext result)
    {
        if (this.freeList.empty()) {
            // return false, continue waiting
            return false;
        }
        result.payload = this.allocateAux();
        return true;
    }

    public long timeoutIntervalMillis ()
    {
        return TimeoutIntervalMillis;
    }

    /**
        Allocate a resource for our use if one is available, otherwise
        return null.
    */
    public Linkable allocateIfAny ()
    {
        synchronized (this.lockHandler.lock) {
            if (!this.freeList.empty()) {
                return this.allocateAux();
            }
            this.allocationMiss++;
            return null;
        }
    }

    /**
        Release an allocated resource for others to use.
    */
    public void release (Linkable linkable)
    {
        synchronized (this.lockHandler.lock) {
            Assert.that(linkable.inuse(), "Object is not in use");
            linkable.setInuse(false);
            this.inUseCount--;
            this.freeList.add(linkable);
            this.lockHandler.lock.notify();
        }
    }


    /**
        Helper for allocation -- Do the bookkeeping for allocating an
        item. Assumes the lock is held.
    */
    private Linkable allocateAux ()
    {
        Linkable linkable = this.freeList.first();
        if (linkable != null) {
            this.freeList.remove(linkable);
            this.inUseCount++;
            Assert.that(!linkable.inuse(), "Object is already in use");
            linkable.setInuse(true);
        }
        this.allocationSum++;
        return linkable;
    }
}
