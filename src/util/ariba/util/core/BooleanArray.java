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

    $Id: //ariba/platform/util/core/ariba/util/core/BooleanArray.java#7 $
*/
package ariba.util.core;

/**
    A DynamicArray of Booleans
    @see ariba.util.core.DynamicArray

    @aribaapi public
*/
public class BooleanArray extends DynamicArray
{
    /**
        Allocate the storage array of type Boolean.
        @param size The size of the array to allocate
        @return The allocated array

        @aribaapi public
    */
    public Object[] alloc (int size)
    {
        return new Boolean[ size ];
    }

    /**
        Typesafe access to the storage array

        @return the Boolean[] array currently in use by this
        object. It is not copied.

        @aribaapi public
    */
    public final Boolean[] array ()
    {
        return (Boolean[])array;
    }

    /**
        Add an element to the array.
        @param val boolean representing the element to add

        @aribaapi public
    */
    public void addElement (boolean val)
    {
        add(val);
    }

    /**
        Add an element to the array.
        @param val boolean representing the element to add

        @aribaapi documented
    */
    public void add (boolean val)
    {
        super.add(val ? Boolean.TRUE : Boolean.FALSE);
    }
}
