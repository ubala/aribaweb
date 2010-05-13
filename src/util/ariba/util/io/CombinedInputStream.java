/*
    Copyright (c) 1996-2010 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/io/CombinedInputStream.java#1 $

    Responsible: dfinlay
*/
package ariba.util.io;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

/**
    Is the input stream contained by stream a collection of streams one after
    another moving to the next one when one is exhausted. <p/>

    @aribaapi ariba
*/
public class CombinedInputStream extends InputStream
{
    //--------------------------------------------------------------------------
    // data members

    private InputStream[] _streams;
    private InputStream _current;
    private int _idx;

    //--------------------------------------------------------------------------
    // constructors

    /**
        Constructs a new instance.
        @aribaapi ariba
    */
    public CombinedInputStream (InputStream[] streams)
    {
        _streams = streams;
        _idx = -1;
        _current = null;
        advance();
    }

    /**
        Constructs a new instance.
        @aribaapi ariba
    */
    public CombinedInputStream (List<InputStream> streams)
    {
        this(streams.toArray(new InputStream[streams.size()]));
    }

    private InputStream advance ()
    {
        if (_idx < _streams.length) {
            ++_idx;
            _current = (_idx < _streams.length) ? _streams[_idx] : null;
        }
        return _current;
    }

    /**
        As specified in superclass.
        @aribaapi ariba
    */
    public int read () throws IOException
    {
        if (_current == null) {
            return -1;
        }
        int result;
        do {
            result = _current.read();
            if (result != -1) {
                return result;
            }
        } while (advance() != null);
        return result;
    }

    /**
        As specified in superclass.
        @aribaapi ariba
    */
    public int read (byte[] bytes, int offset, int length) throws IOException
    {
        if (_current == null) {
            return -1;
        }
        int off = offset;
        int len = length;
        int result = 0;
        while (len > 0) {
            int read = _current.read(bytes, off, len);
            if (read >= 0) {
                result += read;
                off += read;
                len -= read;
            }
            else {
                if (advance() == null) {
                    break;
                }
            }
        }
        return result;
    }

    /**
        As specified in superclass.
        @aribaapi ariba
    */
    public int available () throws IOException
    {
        return _current != null ? _current.available() : 0;
    }

    /**
        Closes all the streams in this (even if an exception occurs.) <p/>
        @aribaapi ariba
    */
    public void close () throws IOException
    {
        IOException exception = null;
        for (InputStream stream : _streams) {
            try {
                stream.close();
            }
            catch (IOException ex) {
                if (exception == null) {
                    exception = ex;
                }
            }
        }
        _current = null;
        _idx = _streams.length;
        if (exception != null) {
            throw exception;
        }
    }
}
