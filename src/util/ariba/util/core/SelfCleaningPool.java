/*
    Copyright (c) 2012-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/SelfCleaningPool.java#3 $

    Responsible: rwells
*/

package ariba.util.core;

import java.lang.ref.SoftReference;
import java.util.List;

/**
    Generic resource pooling mechanism, that uses Timer to return allocated storage to be
    garbage collected when it is no longer needed for current usage patterns. By default
    resources are released from the pool 10 minutes after they were last acquired or
    released to the pool. There is no upper bound to the pool size, it will grow to meet
    simultaneous demand for the resources, and then shrink over time when simultaneous
    demand is not so high. The resources are remembered through soft references, so the GC
    can reclaim them at any time if there are no strong references to them, and the code
    here defends against the resource having been reclaimed this way. This class is
    completely thread-safe, there is expected to be only one instance of a given pool
    across all realms on a node, so the pool of resources can be reused without generating
    garbage by different realms and threads at different times as needed.

    @aribaapi private
*/
public abstract class SelfCleaningPool<T> implements Target
{
    // -----------------------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------------------

    private static final int IdleTimeoutSecondsDefault = 10 * 60;

    // -----------------------------------------------------------------------------------
    // Private instance variables
    // -----------------------------------------------------------------------------------

    private int _idleTimeoutSeconds = IdleTimeoutSecondsDefault;
    private long _idleTimeoutMillis = _idleTimeoutSeconds * 1000L;

    private List<PoolFloat<T>> _free = null;
    private List<PoolFloat<T>> _inUse = null;
    private long _timerTimeoutMillis = 0L;
    private Timer _timer = null;

    // -----------------------------------------------------------------------------------
    // Private static variables
    // -----------------------------------------------------------------------------------

    /**
        We cannot create a Timer without an EventLoop. The EventLoop we want is the one
        managed through BaseServer in persistence.base. The BaseServer constructor will
        register the one true base server event loop with us, so we can find it.
    */
    private static EventLoop _staticEventLoop = null;

    // -----------------------------------------------------------------------------------
    // Public methods
    // -----------------------------------------------------------------------------------

    /**
        Returns an instance of the resource T. Synchronizes on this instance for thread
        safety while calling internal methods to do the work. Never returns null.
    */
    public T get ()
    {
        T rv = null;
        synchronized (this)
        {
            rv = getInternal();
        }
        return rv;
    }

    /**
        Releases a previously allocated instance of the resource item back to the pool.
        If the resource item is null or was not previously returned by get, it is ignored.
        Instance identity via the == operator is used to recognize resource instances, so
        we do not depend on any properties of the hashcode or equals methods of T.
    */
    public void release (T item)
    {
        synchronized (this)
        {
            releaseInternal(item);
        }
    }

    /**
        Returns int number of seconds after the time when a given resource was most
        recently used, at which the resource will be declared idle and reclaimed for
        garbage collection.
    */
    public int getIdleTimeoutSeconds ()
    {
        return _idleTimeoutSeconds;
    }

    /**
        Sets int number of seconds after the time when a given resource was most recently
        used, at which the resource will be declared idle and reclaimed for garbage
        collection. This defaults to 10 minutes, but can be set as desired. While it is
        negative or zero, timeouts are not used, and resources are never reclaimed by the
        pool.
    */
    public void setIdleTimeoutSeconds (int seconds)
    {
        _idleTimeoutSeconds = seconds;
        _idleTimeoutMillis = _idleTimeoutSeconds * 1000L;
        refreshTimer();
    }

    // -----------------------------------------------------------------------------------
    // Public static methods
    // -----------------------------------------------------------------------------------

    /**
        This static method is called by the BaseServer constructor to register the one
        true master event loop for the node with us, so we can create timers, and so our
        client code in util.core doesn't have to figure out how to get an event loop to
        pass us. We synchronize just to be squeeky clean in race conditions during
        startup, and to make sure multi-chip cpus on a board flush their chip cache line
        for the static variable.
    */
    public static void registerEventLoop (EventLoop eventLoop)
    {
        synchronized (SelfCleaningPool.class) {
            _staticEventLoop = eventLoop;
        }
    }

    /**
        Returns EventLoop previously registered by the singleton BaseServer constructor.
        Never returns null.
    */
    public EventLoop getEventLoop ()
    {
        EventLoop eventLoop = null;
        synchronized (SelfCleaningPool.class) {
            eventLoop = _staticEventLoop;
        }
        Assert.that(eventLoop != null, "EventLoop has not been registered by BaseServer");
        return eventLoop;
    }

    // -----------------------------------------------------------------------------------
    // Method for Target interface
    // -----------------------------------------------------------------------------------

    /**
        This method is called when the timer fires. We simply delegate  to calling
        refreshTimer in a synchronized loop, since this is another public method that
        will be called by a timer thread from the event loop.
    */
    public final void performCommand (String command, Object data)
    {
        synchronized (this) {
            refreshTimer();
        }
    }
    
    // -----------------------------------------------------------------------------------
    // Protected methods to be implemented by subclasses
    // -----------------------------------------------------------------------------------

    /**
        Returns newly constructed instance of the resource being managed. This factory
        method must be implemented by subclasses.
    */
    protected abstract T createResource ();
    
    // -----------------------------------------------------------------------------------
    // Private methods
    // -----------------------------------------------------------------------------------

    /**
        Returns T resource instance. If one is available from the free list and it has not
        been garbage collected, remove it from the free list and add it to the inUse list,
        and return it. When it is added to the list, a timer refresh will be done. If the
        free list has no usable instance, create a new one using the factory method, add
        it to the inUse list, and return it. The free list is really managed as a stack,
        so we tend to reuse the resource at the top of the stack, and the ones further
        done in the stack will tend to time out and be reclaimed after peak demand
        periods. Never returns null.
    */
    private T getInternal ()
    {
        int size = (_free != null ? _free.size() : 0);
        for (int i = size - 1; i >= 0; i--) {
            PoolFloat<T> poolFloat = _free.get(i);
            T floatItem = poolFloat.get();
            if (floatItem != null) {
                _free.remove(i);
                addToInUseList(poolFloat);
                return floatItem;
            }
            else {
                _free.remove(i);
            }
        }
        T floatItem = createResource();
        PoolFloat<T> poolFloat = new PoolFloat(floatItem);
        addToInUseList(poolFloat);
        return floatItem;
    }

    /*
        Releases given resource item back to the pool, if it is not null and is found in
        the inUse list, and thus was previously returned by get.  We do == identity
        comparison in the inUse list to recognize the item, so we don't depend on any
        properties of its equals or hashcode methods. If it is found we remove it from the
        inUse list and add it back to the free list. Along the way if we encounter a
        garbage collected resource, we remove its entry from the pool, and refresh the
        timer.
    */
    private void releaseInternal (T item)
    {
        int size = (_inUse != null && item != null ? _inUse.size() : 0);
        for (int i = size - 1; i >= 0; i--) {
            PoolFloat<T> poolFloat = _inUse.get(i);
            T floatItem = poolFloat.get();
            if (floatItem == item) {
                _inUse.remove(i);
                addToFreeList(poolFloat);
                break;
            }
            else if (floatItem == null) {
                _inUse.remove(i);
                refreshTimer();
            }
        }
    }

    /**
        Update the RecentlyUsedTimeMillis on the given resource wrapper, create the _inUse
        list if it is currently null, and add the resource wrapper to the _inUse
        list. Then refresh the timer.
    */
    private void addToInUseList (PoolFloat<T> poolFloat)
    {
        poolFloat.updateRecentlyUsedTimeMillis();
        if (_inUse == null) {
            _inUse = ListUtil.list();
        }
        _inUse.add(poolFloat);
        refreshTimer();
    }

    /**
        Update the RecentlyUsedTimeMillis on the given resource wrapper, create the _free
        list if it is currently null, and add the resource wrapper to the _free
        list. Then refresh the timer.
    */
    private void addToFreeList (PoolFloat<T> poolFloat)
    {
        poolFloat.updateRecentlyUsedTimeMillis();
        if (_free == null) {
            _free = ListUtil.list();
        }
        _free.add(poolFloat);
        refreshTimer();
    }

    /**
        Reclaim pool items that have passed their idle timeout. Create, start, and stop
        our timer as needed in support of this.
    */
    private void refreshTimer ()
    {
        long nextTimeoutMillis = 0L;
        long idleMillis = _idleTimeoutMillis;
        long currentMillis = System.currentTimeMillis();
        /*
            If the idle timeout has been disabled by setting IdleTimeoutSeconds to a
            non-positive value, don't update the lists to reclaim items that have timed
            out, and leave nextTimeoutMillis set to zero, so any current timer that is
            running will be stopped, and no timer will be started.  If we have an idle
            timeout defined, then update the list to get rid of timed out items, and
            figure out when the timer should fire next based on the oldest recently
            used time in both lists.
        */
        if (idleMillis > 0) {
            long cutoffMillis = currentMillis - idleMillis;
            updateListForTimeout(_inUse, cutoffMillis);
            updateListForTimeout(_free, cutoffMillis);
            nextTimeoutMillis = getNextTimeoutMillis(idleMillis);
        }
        /*
            If the next timeout moment is different from the current timer moment, if any,
            we need to make changes to the timer. Either the current or next moment may be
            0L indicating no timeout pending. If they are the same, then nothing needs to
            be done to the timer.
        */
        if (nextTimeoutMillis != _timerTimeoutMillis) {
            /*
                If the timeout moment is changing and the timer has a current timeout set,
                clear the current timeout and stop the timer if it is not null. If
                _timerTimeoutMillis is positive, _timer should always be non-null.
            */
            if (_timerTimeoutMillis > 0L) {
                _timerTimeoutMillis = 0L;
                if (_timer != null) {
                    _timer.stop();
                }
            }
            /*
                If we need to have a timeout, then allocate a Timer if we don't have one,
                and set it to only fire once, and not repeat. Set the initial delay on
                our timer, start it, and remember when the timeout is set to fire.
            */
            if (nextTimeoutMillis > 0L) {
                if (_timer == null) {
                    _timer = new Timer(getEventLoop(), this, null, 0);
                    _timer.setRepeats(false);
                }
                int delayMillis = (int)(nextTimeoutMillis - currentMillis);
                _timer.setInitialDelay(delayMillis);
                _timer.start();
                _timerTimeoutMillis = nextTimeoutMillis;
            }
        }
        /*
            If no timer is started, check to see if we have reclaimed all items, and
            should give up our lists and timer, to return to our initially constructed
            minimized storage state.
        */
        if (_timerTimeoutMillis <= 0L) {
            resetIfEmpty();
        }
    }

    /**
        Go through the given list, and remove the first item as long as its
        RecentlyUsedTimeMillis is not greater than the given cutoffMillis. We remove all
        the items that are at least as old as cutoffMillis. By construction, the lists are
        ordered by RecentlyUsedTimeMillis, with the earliest at the beginning of the list,
        and the most recent at the end of the list. As soon as we hit an element that is
        beyond the cutoffMillis, we break out of the loop.
    */
    private void updateListForTimeout (List<PoolFloat<T>> list, long cutoffMillis)
    {
        if (list != null) {
            while (!list.isEmpty()) {
                PoolFloat<T> poolFloat = list.get(0);
                if (poolFloat.getRecentlyUsedTimeMillis() <= cutoffMillis) {
                    list.remove(0);
                }
                else {
                    break;
                }
            }
        }
    }

    /**
        Returns long time millisecond value for when the timer should next fire, using the
        given idleMillis setting. If either the inUse or the free list are non-empty, we
        return the oldest RecentlyUsedTimeMillis value with idleMillis added to it. If
        both lists are empty, we return zero.
    */
    private long getNextTimeoutMillis (long idleMillis)
    {
        long inUseMillis = (_inUse != null && !_inUse.isEmpty() ?
                            _inUse.get(0).getRecentlyUsedTimeMillis() : Long.MAX_VALUE);
        long freeMillis = (_free != null && !_free.isEmpty() ?
                           _free.get(0).getRecentlyUsedTimeMillis() : Long.MAX_VALUE);
        long oldestMillis = (inUseMillis < freeMillis ? inUseMillis : freeMillis);
        return (oldestMillis < Long.MAX_VALUE ? oldestMillis + idleMillis : 0L);
    }

    /**
        If both lists are empty or null, we can reset all our instance variables and
        minimize our storage footprint. This will happen if the resource pool is not used
        for 10 minutes or more, with the default setting. Since it is only called
        from refreshTimer when there is no Timer started, we don't have to worry about
        stopping the timer.
    */
    private void resetIfEmpty ()
    {
        if ((_free == null || _free.isEmpty()) && (_inUse == null || _inUse.isEmpty())) {
            _free = null;
            _inUse = null;
            _timerTimeoutMillis = 0L;
            _timer = null;
        }
    }

    // -----------------------------------------------------------------------------------
    // Nested class PoolFloat
    // -----------------------------------------------------------------------------------

    /**
        Nested class that wraps up an actual resource instance. It extends SoftReference,
        so the GC can garbage collect the resource if memory gets very tight. This means
        that get() may return null at any time when there is not a strong reference to
        the resource in a local variable or somewhere else other than the soft reference.
        It also maintains the time in milliseconds when the resource was last returned to
        the free list, so we can free them after they haven't been used for a while.
    */
    public static class PoolFloat<T> extends SoftReference<T>
    {
        private long _recentlyUsedTimeMillis;

        protected PoolFloat (T resource)
        {
            super(resource);
            updateRecentlyUsedTimeMillis();
        }

        protected long getRecentlyUsedTimeMillis ()
        {
            return _recentlyUsedTimeMillis;
        }

        protected void updateRecentlyUsedTimeMillis ()
        {
            _recentlyUsedTimeMillis = System.currentTimeMillis();
        }
    }
}
