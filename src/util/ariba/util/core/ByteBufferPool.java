/*
    Copyright (c) 2012-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/ByteBufferPool.java#3 $

    Responsible: rwells
*/

package ariba.util.core;

import java.nio.ByteBuffer;

/**
    This class implements a self cleaning pool of ByteBuffers, each wrapped around a byte
    array of the configured buffer length. Superclass get() and release(ByteBuffer)
    methods can be used to allocate and release ByteBuffers from the pool. ByteBuffers are
    allocated on demand, and released to the garbage collector after 10 minutes without
    use, unless the superclass setIdleTimeoutSeconds method is called to change the
    timeout. If there is a leak and release is not called to return a ByteBuffer after
    use, it will still be released from the pool after 10 minutes, and thus will become
    available to the garbage collector.
    @aribaapi private
*/
public class ByteBufferPool extends BufferPool<ByteBuffer>
{
    // -----------------------------------------------------------------------------------
    // Public constructors
    // -----------------------------------------------------------------------------------

    public ByteBufferPool (int bufferLength)
    {
        super(bufferLength);
    }

    // -----------------------------------------------------------------------------------
    // Protected methods to be implemented by subclasses
    // -----------------------------------------------------------------------------------

    /**
        Returns newly constructed instance of the resource being managed. This factory
        method is overridden here to allocate a ByteBuffer wrapped around a newly
        constructed char array of the configured bufferLength.
        This must be called only from inside the pool implementations and never outside
    */
    @Override
    protected ByteBuffer createResource ()
    {
        return ByteBuffer.wrap(new byte[getBufferLength()]);
    }
    
    @Override
    public ByteBuffer get ()
    {
        ByteBuffer b = super.get();
        b.clear();
        return b;
    }
}
