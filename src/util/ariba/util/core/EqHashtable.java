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
    This class extends Hashtable, but provides for pointer hashing and
    equality instead of hashCode() and equals() equality.

    in 1.4 this can be replaced by java.util.IdentityHashMap
    
    @aribaapi private
*/
public class EqHashtable extends Hashtable
{
    /**
        Constructs an empty EqHashtable. The Hashtable will grow on
        demand as more elements are added.
    */
    public EqHashtable ()
    {
        super();
    }

    /**
        Constructs an EqHashtable capable of holding at least
        <b>initialCapacity</b> elements before needing to grow.
    */
    public EqHashtable (int initialCapacity)
    {
        super(initialCapacity);
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

    /**
        Primitive method used internally to find slots in the
        table. If the key is present in the table, this method will
        return the index under which it is stored. If the key is not
        present, then this method will return the index under which it
        can be put. The caller must look at the data at that index to
        differentiate between the two possibilities.
    */
    protected int tableIndexForLinearSearch (Object key)
    {
        for (int i = 0; i < count; i++) {
            if (keys[i] == key) {
                return i;
            }
        }
        Assert.that(count <= elements.length, "Hashtable overflow");
        return count;
    }


}
