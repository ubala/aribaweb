/*
    Copyright (c) 2012-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/CharBufferPool.java#3 $

    Responsible: rwells
*/

package ariba.util.core;

import java.nio.CharBuffer;

/**
    This class implements a self cleaning pool of CharBuffers, each wrapped around a char
    array of the configured buffer length. Superclass get() and release(CharBuffer)
    methods can be used to allocate and release CharBuffers from the pool. CharBuffers are
    allocated on demand, and released to the garbage collector after 10 minutes without
    use, unless the superclass setIdleTimeoutSeconds method is called to change the
    timeout. If there is a leak and release is not called to return a CharBuffer after
    use, it will still be released from the pool after 10 minutes, and thus will become
    available to the garbage collector.
    @aribaapi private
*/
public class CharBufferPool extends BufferPool<CharBuffer>
{
    // -----------------------------------------------------------------------------------
    // Public constructors
    // -----------------------------------------------------------------------------------

    public CharBufferPool (int bufferLength)
    {
        super(bufferLength);
    }

    // -----------------------------------------------------------------------------------
    // Protected methods to be implemented by subclasses
    // -----------------------------------------------------------------------------------

    /**
        Returns newly constructed instance of the resource being managed. This factory
        method is overridden here to allocate a CharBuffer wrapped around a newly
        constructed char array of the configured bufferLength.
        This must be called only from inside the pool implementations and never outside
    */
    @Override
    protected CharBuffer createResource ()
    {
        return CharBuffer.wrap(new char[getBufferLength()]);
    }
    
    @Override
    public CharBuffer get ()
    {
        CharBuffer c = super.get();
        c.clear();
        return c;
    }
}
