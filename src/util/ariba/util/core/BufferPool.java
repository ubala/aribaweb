/*
    Copyright (c) 2012-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/BufferPool.java#3 $

    Responsible: rwells
*/

package ariba.util.core;

import java.nio.Buffer;

/**
    This abstract self cleaning pool of Buffer instances can be extended by subclasses
    for specific types of Buffers, for example CharBuffer and ByteBuffer. It maintains
    a buffer length that is set in the constructor and never changed. All the buffers
    in the pool will be uniformly the same size.

    @aribaapi private
*/
public abstract class BufferPool<T extends Buffer> extends SelfCleaningPool<T>
{
    // -----------------------------------------------------------------------------------
    // Private instance variables
    // -----------------------------------------------------------------------------------

    private int _bufferLength;

    // -----------------------------------------------------------------------------------
    // Public constructors
    // -----------------------------------------------------------------------------------

    public BufferPool (int bufferLength)
    {
        super();
        Assert.that(bufferLength > 0, "bufferLength must be positive");
        _bufferLength = bufferLength;
    }

    // -----------------------------------------------------------------------------------
    // Public getter methods
    // -----------------------------------------------------------------------------------

    /**
        Returns newly constructed instance of the resource being managed. This factory
        method must be implemented by subclasses.
    */
    public int getBufferLength ()
    {
        return _bufferLength;
    }
}
