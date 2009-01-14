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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadedQueue.java#7 $
*/

package ariba.util.core;

import ariba.util.log.Log;

/**
    This is a ThreadedQueue used for a producer/consumer model of
    threads. Order out is guranteed to be the same as the order in. If
    maxItems is specified, there will never be more than
    <b>maxItems</b> pending in the queue, if the max number are
    already present, the producer will block until items are consumed.

    @aribaapi private
*/
public class ThreadedQueue
{
    private int maxItems;
    private boolean moreData = true;
    private Deque items;
    private Object error;

    /**
       Create a threaded queue with a max number of elements that can
       be held before blocking.  
    */
    private int m_refCount = 0;

        // The List is a naive implementation. This should be a
        // circular array, but as there is no indication that this is
        // a performance issue yet, this has not been done. DS

    public ThreadedQueue (int maxItems)
    {
        this.maxItems = maxItems;
        items = new Deque(maxItems == Integer.MAX_VALUE ? 10 : maxItems);
        incrementUseCount();
    }
    
    /**
       Create a threaded queue with an unbounded number of elements.
    */
    public ThreadedQueue ()
    {
        this(Integer.MAX_VALUE);
    }

    public void setMaxItems (int maxItems)
    {
        this.maxItems = maxItems;
    }
    
    /**
        Called by producer. If there was an error on the consumer end,
        it will return it.
        Do not, under any circumstances, put a Log message in this method.
        It will cause an infinite recursion.

    */
    public Object insertObject (Object o)
    {
        Assert.that(o != null,
                    "You may not insert a null object in the queue");
        synchronized (items) {
            while (items.size() >= maxItems && moreData && error == null) {
                this.waitAux();
            }
            if (!moreData) {
                return null;
            }
            if (error != null) {
                return(error);
            }

            items.enqueue(o);
                // I think this is safe as a notify...only one thread
                // needs read from one insert. But better safe than
                // sorry. And, during normal operation, there really
                // shouldn't be (m)any threads waiting...
            items.notifyAll();
        }
        return null;
    }

    public void incrementUseCount ()
    {
        synchronized (items) {
            m_refCount++;
        }
    }


    /** Called by either side to set an error */
    public void setError (Object o)
    {
        synchronized (items) {
            error = o;
            items.notifyAll();
        }
    }
    
    /** 
        Called by either side to test for error; generally 
        called by producer one time after all consumer threads 
        die to test for errors occuring after its last insertObject 
        and before consumer threads are done
    */
    public Object error ()
    {
        return error;
    }

    /**
        Called by producer.
    */
    public boolean isEmpty ()
    {
        synchronized (items) {
            return this.items.isEmpty();
        }
    }

    /**
        Determine how many items are in the queue
        @aribaapi private
    */
    public int count ()
    {
        return items.size();
    }
    
    /**
        Called by producer.
    */
    public boolean moreData ()
    {
        return this.moreData;
    }

    /**
        Called by producer; useful when waiting to
        flush queue before continuing.
    */
    public void waitForEmpty ()
    {
        synchronized (this.items) {
            
            while (!this.items.isEmpty()) {
                this.waitAux();
            }
        }
    }

    /**
        Called by producer.
    */
    public void setNoMoreData ()
    {
        synchronized (items) {
            if (--m_refCount == 0) { 
                moreData = false;
                items.notifyAll();
            }
        }
    }

    /**
        Called by consumer.
    */
    public Object peekNextObject ()
    {
        synchronized (items) {
            while (items.isEmpty() && moreData && error == null) {
                this.waitAux();
            }
            if (items.isEmpty()) {
                items.notifyAll();
                return null;
            }
            if (error != null) {
                items.notifyAll();
                return null;
            }
            Object o = items.peekDequeue();
            return(o);
        }
    }

    /**
        Called by consumer.
    */
    public Object nextObject ()
    {
        synchronized (items) {
            while (items.isEmpty() && moreData && error == null) {
                this.waitAux();
            }
            if (items.isEmpty()) {
                items.notifyAll();
                return null;
            }
            if (error != null) {
                items.notifyAll();
                return null;
            }
            Object o = items.dequeue();
            items.notifyAll();
            return(o);
        }
    }

    private void waitAux ()
    {
        try {
            items.wait();
        }
        catch (InterruptedException intEx) {
            Log.util.info(2897, SystemUtil.stackTrace(intEx));
        }
    }
}
