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

    $Id: $
*/

package ariba.util.core;

/**
    This class extends LRUHashtable, but provides for pointer hashing and
    equality instead of hashCode() and equals() equality.

    @aribaapi private
*/
public class EqLRUHashtable extends LRUHashtable
{
    /**
        Constructs an empty EqLRUHashtable. The LRUHashtable will grow on
        demand as more elements are added.
    */
    public EqLRUHashtable (
        String name,
        int initialEntries,
        int targetEntries,
        int purgeMargin,
        double maxLoad,
        LRURemoveListener removeListener)
    {
        super(name, initialEntries, targetEntries, purgeMargin, maxLoad,
              removeListener);
    }


    /**
        Helper function that returns the appropriate hash code for the
        object <b>o</b>. Unless overriden by a subclass, this will
        return the pointer hash value for the object.
    */
    protected int getHashValueForObject (Object o)
    {
        return System.identityHashCode(o);
    }

    /**
        Helper function to determine if two objects are equal.  It
        returns true if and only if <b>obj1</b> == <b>obj2</b>
    */
    protected boolean objectsAreEqualEnough (Object obj1, Object obj2)
    {
        return (obj1 == obj2);
    }

}
