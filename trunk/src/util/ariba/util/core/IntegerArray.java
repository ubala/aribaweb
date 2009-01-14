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

    $Id: //ariba/platform/util/core/ariba/util/core/IntegerArray.java#6 $
*/
package ariba.util.core;

/**
    A DynamicArray of Integers
    @see ariba.util.core.DynamicArray

    @aribaapi public
*/
public class IntegerArray extends DynamicArray
{
    private Integer[] localArray;

    public Object[] alloc (int size)
    {
        localArray = new Integer[size];
        return localArray;
    }

    /**
        Typesafe access to the storage array
        @return the Integer[] array currently in use by this
        object. It is not copied.
        @aribaapi public
    */
    public final Integer[] array ()
    {
        return localArray;
    }

    public void add (int i)
    {
        super.add(Constants.getInteger(i));
    }

    public void setArray (Integer[] array)
    {
        super.setArray(array);
        this.localArray = array;
    }

}
