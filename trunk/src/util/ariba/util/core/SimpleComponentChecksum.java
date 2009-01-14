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

    $Id: //ariba/platform/util/core/ariba/util/core/SimpleComponentChecksum.java#4 $
*/
package ariba.util.core;


/**
    A simple implementation of ComponentChecksum that saves
    a stored byte array.  We use this instance when we have to
    store a serialized version of the checksum

    @aribaapi ariba

*/
public class SimpleComponentChecksum extends ComponentChecksum
{
    private byte[] checksum;

    /**
        Create the checksum based on the byte array passed in

        @param name The component name
        @param cs The checksum to be saved
        @aribaapi ariba
    */
    public SimpleComponentChecksum (String name, byte[] cs)
    {
        super(name);
        checksum = cs;
    }

    /**
        create a SimpleComponentChecksum based on another checsum.
        This saves a pointer to the original checksum's checksum.
        This is used to create a serializable version of a ComponentChecksum
        that might contain non-serializable objects (e.g.
        FileComponentChecksum)

        @param cs The ComponentChecksum to copy
        @aribaapi ariba
    */        
    public SimpleComponentChecksum (ComponentChecksum cs)
    {
        super(cs.getName());
        checksum = cs.getChecksum();
    }

    public SimpleComponentChecksum ()
    {
    }

    /**
        get the checksum.  The array returned is read only

        @aribaapi ariba
    */
    public byte[] getChecksum ()
    {
        return checksum;
    }
}

