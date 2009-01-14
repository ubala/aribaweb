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

    $Id: //ariba/platform/util/core/ariba/util/core/Blob.java#5 $
*/
package ariba.util.core;

import java.io.InputStream;

/**
    Represents a stream based interface to a "binary large-object" (or
    <i>blob</i> for short.) <p>

    In general we cannot assume that large-objects (lobs) are entirely in memory
    at any point in time, which is why stream based interfaces are provided.
    The actual lob may be in a file on disk, in a database or even being streamed
    in from a network connection.  This is why all of the methods in this interface
    throw {@link LobException}s as connections with these external resources may
    fail at any time. <p>

    @aribaapi ariba
*/
public interface Blob 
{
    /**
        Returns the number of bytes in the underlying blob object.
        @return length of the blob in bytes
        @exception LobException if there is an error accessing the
                   length of the blob
        @aribaapi ariba
    */
    long length() throws LobException;

    /**
        Returns as an array of bytes, part or all of the blob.  The byte
        array contains up to <code>length</code> consecutive bytes
        starting at position <code>pos</code>.

        @param pos the ordinal position of the first byte in the
                   blob value to be extracted; the first byte is at
                   position 0
        @param length the number of consecutive bytes to be copied
        @return a byte array containing up to <code>length</code>
                consecutive bytes from the blob, starting with the
                byte at position <code>pos</code>
        @exception LobException if there is an error accessing the
                   blob
        @aribaapi ariba
    */
    byte[] getBytes(long pos, int length) throws LobException;

    /**
        Retrieves the blob instance as a stream.
        @return a stream containing the blob data
        @exception LobException if there is an error accessing the blob
        @aribaapi ariba
    */
    InputStream getBinaryStream () throws LobException;
}
