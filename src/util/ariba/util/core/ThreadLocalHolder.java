/*
    Copyright (c) 1996-2007 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadLocalHolder.java#1 $

    Responsible: dfinlay
*/
package ariba.util.core;

/**
    @aribaapi ariba
*/
public abstract class ThreadLocalHolder<V>
{
    private V _object;
    private int _recursionDepth;

    public ThreadLocalHolder ()
    {
        _object = make();
        _recursionDepth = 0;
    }

    public abstract V make ();

    /**
        Checks the byte buffer out of this holder.
    */
    public V checkoutBuffer ()
    {
        V result = _object;
        if (result != null) {
            // null out the buffer
            _object = null;
        }
        else {
            result = make();
        }
        _recursionDepth++;
        return result;
    }

    /**
        Returns the byte buffer to this holder.
    */
    public void returnBuffer (V object)
    {
        // We return the buffer to this.
        _object = object;
        _recursionDepth--;
    }

    /**
        Returns the recursion depth.
    */
    public int getRecursionDepth ()
    {
        return _recursionDepth;
    }
}
