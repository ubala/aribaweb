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

    $Id: //ariba/platform/util/core/ariba/util/core/Deque.java#6 $
*/

package ariba.util.core;

import java.util.List;

/**
    This class implements a double-ended queue (pronounced like
    "deck"). It has the behavior that each operation is in constant
    (amortized) time. It supports FIFO operation with the enqueue and
    dequeue methods, and LIFO with push and pop methods. Both access
    methods may be used on a single deque.

    This is a similar interface to the deque described in Intorduction
    to Algorithms by Cormen, Leiserson and Rivest: Eighth printing in
    chapter 11 (Elementary Data Structures) section 1 (Stacks and
    queues)
    
    @aribaapi private
*/
public class Deque implements DebugState
{
    protected Object elementData[];
    protected int headElement;
    protected int tailElement;
    protected int length;
    protected int count;

    /**
        Construct a deque with a default capacity.
    */
    public Deque ()
    {
        this(10);
    }
    
    /**
        Construct a deque with a given initial capacity.

        @param initialCapacity the minimum number of elements that the
        deque is required to hold before having to grow the internal
        storage.
    */
    public Deque (int initialCapacity)
    {
        Assert.that(initialCapacity > 0,
                    "Deque must be able to hold a positive number of elements");
        length = initialCapacity;
        headElement = 0;
        tailElement = 0;
        elementData = new Object[length];
    }

    /**
        Enqueues an object into the deque.

        @param o the object to enqueue
    */
    public void enqueue (Object o)
    {
        if (count == length) {
            setSize(length * 2);
        }
        elementData[tailElement] = o;
        tailElement++;
        if (tailElement >= length) {
            tailElement = 0;
        }
        count++;
    }

    /**
        Dequeues from the deque.

        @return the oldest element in the deque.
    */
    public Object dequeue ()
    {
        Assert.that(count > 0, "Queue underfow");
        Object o = elementData[headElement];
            // don't leak memory
        elementData[headElement] = null;
        headElement++;
        if (headElement >= length) {
            headElement = 0;
        }
        count--;
        return o;
    }

    /**
        Peek in the deque without removing it.

        @return the oldest element in the deque.
    */
    public Object peekDequeue ()
    {
        Assert.that(count > 0, "Queue underfow");
        return elementData[headElement];
    }
    
    /**
        Pushes an object onto the deque in the manner of a stack. This
        method is a synonym for enqueue.

        @param o the object to enqueue
        @see #enqueue(Object)
    */
    public void push (Object o)
    {
        enqueue(o);
    }

    /**
        Peek into the deque without removing any elements.

        @return the youngest element in the deque
    */
    public Object peekPop ()
    {
        Assert.that(count > 0, "Queue underfow");
        int index = tailElement - 1;
        if (index < 0) {
            index = length-1; 
        }
        Object o = elementData[index];
        return o;
    }

    /**
        Pops an element from the deque.

        @return the youngest element in the deque
    */
    public Object pop ()
    {
        Assert.that(count > 0, "Queue underfow");
        tailElement--;
        if (tailElement < 0) {
            tailElement = length-1; 
        }
        count--;
        Object o = elementData[tailElement];
            // don't leak memory
        elementData[tailElement] = null;
        return o;
    }

    /**
        Set the size for the internal storage. This may be larger or
        smaller than the current size.

        @param size the new size for the internal storage. This must
        be greater than or equal to the count and may not be zero.
    */
    public void setSize (int size)
    {
        Assert.that(size >= count,
                    "May not shrink a queue to be smaller than the " +
                    "count of elements.");
        Assert.that(size > 0, "size must be greater than zero");

        Object newElementData[] = new Object[size];

            // calculate the number of items to the right of the head
            // to copy.
        int tailCount=Math.min(length-headElement, count);
        System.arraycopy(elementData, headElement, newElementData, 0,
                         tailCount);
            // copy all the items remaining if they weren't all copied
            // already
        if (tailCount < count) {
            System.arraycopy(elementData, 0, newElementData, tailCount,
                             tailElement);
        }
        headElement = 0;
        tailElement = count;
        if (tailElement == size) {
            tailElement = 0;
        }
        elementData = newElementData;
        length = size;
    }

    /**
        Check the number of elements in the deque.

        @return the number of elements in the deque.
    */
    public int count ()
    {
        return size();
    }

    /**
        Check the number of elements in the deque.

        @return the number of elements in the deque.
    */
    public int size ()
    {
        return count;
    }

    /**
        Check if the deque is empty.

        @return <code>true</code> if and only if the deque is empty.
    */
    public boolean isEmpty ()
    {
        return count == 0;
    }

    /**
        Iterates through the deque from head to tail and calls toString
        on each node
    */
    public Object debugState ()
    {
        List v = ListUtil.list(count);
        int i = 0;
        int pos = headElement;
        while (i < count) {
            v.add(elementData[pos]);
            i++;
            if (pos++ >= length) {
                pos = 0;
            }
        }
        return v;
    }
    
    public String toString ()
    {
        return debugState().toString();
    }
}
