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

    $Id: //ariba/platform/util/core/ariba/util/core/Hashtable.java#18 $
*/

package ariba.util.core;

import ariba.util.io.FormattingSerializer;
import ariba.util.io.Serializer;
import ariba.util.log.Log;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

    // There needs to be a way to call Object.hashCode() directly when you want
    // to do identity hashing.  ALERT!  (What does this mean? -- mhb 7/12/97)

    // there is now an eqHashtable for just this. In addition there is
    // a hash table that only grows but does not require
    // synchronization on gets and provides internal synchronization
    // on puts. Much of the code is (necessarially) duplicated. If
    // there are any bugs in this Hashtable, it would be worth
    // checking the GrowOnlyHashtable to see if the same bug exists.
    // --ds 12/21/98


/**
    A Hashtable class that has a number of performance improvements
    over other Hashtable classes. The methods are not internally
    synchronized, so care must be taken at the application level to
    synchronize.

    @aribaapi documented
*/
public class Hashtable extends AbstractMap
        implements Cloneable, Externalizable
{

    private static final int linearSearchThreshold = 16;
    private static final Object emptyArray[] = new Object[0];

    /**
        For the multiplicative hash, choose the golden ratio:
        <pre>
        A = ((sqrt(5) - 1) / 2) * (1 << 32)
        </pre>
        ala Knuth...

        @aribaapi private
    */
    protected static final int A = 0x9e3779b9;

    /**
        Name of field for keys.  Value is "keys".

        @aribaapi private
    */
    protected static final String keysField = "keys";

    /**
        Name of field for elements.  Value is "elements".

        @aribaapi private
    */
    protected static final String elementsField = "elements";

    /**
        Default Hashtable size.  Value is 32.

        @aribaapi private
    */
    protected static final int DefaultSize = 32;

    /**
        Placeholder object that represents a deleted entry.

        @aribaapi private
    */
    protected static final Object DeletedMarker = new Object();

    /**
        Number of elements currently in Hashtable.

        @aribaapi private
    */
    protected int count;

    /**
        Total number of occupied slots.

        @aribaapi private
    */
    protected int totalCount;

    protected int shift;

    /**
        Hashtable capacity.

        @aribaapi private
    */
    protected int capacity;

    protected int indexMask;

    /**
        Hashtable keys.

        @aribaapi private
    */
    protected Object keys[];

    /**
        Hashtable elements.

        @aribaapi private
    */
    protected Object elements[];

    /**
        Constructs an empty Hashtable. The Hashtable will grow on demand
        as more elements are added.
        @aribaapi documented
        @deprecated use MapUtil.map
        @see ariba.util.core.MapUtil#map()
    */
    public Hashtable ()
    {
        super();
        this.shift = 32 - MathUtil.log2(DefaultSize) + 1;
    }

    /**
        Constructs a Hashtable capable of holding a specified number
        of elements.

        @param initialCapacity the number of elements this Hashtable
        is capable of holding before needing to grow.
        @aribaapi documented
        @deprecated use MapUtil.map
        @see ariba.util.core.MapUtil#map(int)
    */
    public Hashtable (int initialCapacity)
    {
        this();

        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0");
        }

        grow(initialCapacity);
    }

    /**
        Creates new Hashtable with the same contents as the given Hashtable.

        @param source source Hashtable
        @aribaapi private
        @deprecated use MapUtil.map
        @see ariba.util.core.MapUtil#map(java.util.Map)
    */
    public Hashtable (java.util.Hashtable source)
    {
        this(source.size());
        Enumeration k = source.keys();
        Enumeration e = source.elements();
        while (k.hasMoreElements()) {
            put(k.nextElement(), e.nextElement());
        }
    }

    /**
        Creates new Hashtable with the same contents as the given Map.

        @param source source Map
        @aribaapi private
    */
    public Hashtable (Map source)
    {
        this(source.size());
        putAll(source);
    }

    /**
        Creates a shallow copy of the Hashtable. The table itself is cloned,
        but none of the keys or elements are copied.

        @return the cloned copy

        @throws InternalError if the cloning operation fails.

        @aribaapi documented
    */
    public Object clone ()
    {
        Hashtable newTable;
        try {
            newTable = (Hashtable)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError("Error in clone(). This shouldn't happen.");
        }

            // don't try to share cached collection classes across
            // cloned instances of hashtables.
        newTable.keySet = null;
        newTable.values = null;
        
        // If there is nothing in the table, then we cloned to make sure the
        // class was preserved, but just null everything out except for shift,
        // which implies the default initial capacity.

        if (count == 0) {
            newTable.shift = 32 - MathUtil.log2(DefaultSize) + 1;
            newTable.totalCount = 0;
            newTable.capacity = 0;
            newTable.indexMask = 0;
            newTable.keys = null;
            newTable.elements = null;

            return newTable;
        }

        int len = keys.length;
        newTable.keys = new Object[len];
        newTable.elements = new Object[len];

        System.arraycopy(keys, 0, newTable.keys, 0, len);
        System.arraycopy(elements, 0, newTable.elements, 0, len);

        return newTable;
    }

    /**
        Returns the number of elements in the Hashtable.
        @return the number of elements in the Hashtable
        @aribaapi ariba
        @deprecated replaced by size
        @see #size
    */
    public int count ()
    {
        return count;
    }

    /**
        Returns the number of elements in the Hashtable.
        @return the number of elements in the Hashtable
        @aribaapi documented
    */
    public int size ()
    {
        return count;
    }

    /**
        Indicates if the hashtable contains any elements.
        @return <b>true</b> if there are no elements in the Hashtable.
        @aribaapi ariba
    */
    public boolean isEmpty ()
    {
        return (count == 0);
    }

    /**
        Returns an Enumeration of the Hashtable's keys.

        @return an enumeration of the keys in this Hashtable.
        @deprecated use keySet.iterator() instead
        @see #keySet
        @see java.util.Set#iterator
        @see #elements()
        @aribaapi ariba
    */
    public Enumeration keys ()
    {
        return new HashtableEnumerator(this, true);
    }

    /**
        Returns an Enumeration of the Hashtable's elements.

        @return an enumeration of the elements in this Hashtable.
        @deprecated use values.iterator() instead 
        @see #values
        @see java.util.Collection#iterator
        @see #keys()
        @aribaapi documented
    */
    public Enumeration elements ()
    {
        return new HashtableEnumerator(this, false);
    }

    /**
        Returns a Vector containing the Hashtable's keys. If the Hashtable
        contains no keys, return an empty Vector object.

        @return the Vector object mentioned above.
        @deprecated use MapUtil.keysList
        @see ariba.util.core.MapUtil#keysList
        @aribaapi ariba
    */
    public Vector keysVector ()
    {
        if (count == 0) {
            return new Vector();
        }

        Vector vect = new Vector(count);
        int vectCount = 0;

        for (int i = 0; i < keys.length && vectCount < count; i++) {
            Object key = keys[i];
            if (key != null && key != DeletedMarker) {
                vect.addElement(key);
                vectCount++;
            }
        }

        return vect;
    }

    /**
        Returns a Vector containing the Hashtable's elements. If the hashtable
        containes no elements, an empty Vector object is returned.
        @return the Vector object mentioned above.
        @deprecated use MapUtil.elementsList
        @see ariba.util.core.MapUtil#elementsList
        @aribaapi ariba
    */
    public Vector elementsVector ()
    {
        if (count == 0) {
            return new Vector();
        }

        Vector vect = new Vector(count);
        int vectCount = 0;

        for (int i = 0; i < elements.length && vectCount < count; i++) {
            Object element = elements[i];
            if (element != null && element != DeletedMarker) {
                vect.addElement(element);
                vectCount++;
            }
        }

        return vect;
    }

    /**
        Returns an Object array containing the Hashtable's keys. If the
        Hashtable contains no keys, an empty array is returned.
        @return the Object array above mentioned.
        @deprecated use MapUtil.keysArray
        @see ariba.util.core.MapUtil#keysArray
        @aribaapi ariba
    */
    public Object[] keysArray ()
    {
        if (count == 0) {
            return emptyArray;
        }

        Object[] array = new Object[count];
        int arrayCount = 0;

        for (int i = 0; i < keys.length && arrayCount < count; i++) {
            Object key = keys[i];
            if (key != null && key != DeletedMarker) {
                array[arrayCount++] = key;
            }
        }

        return array;
    }
    /**
        Returns an Object array containing the Hashtable's elements.
        @return the Object array mentioned above. If there the hashtable contains
        no elements, an empty array is returned.
        @aribaapi ariba
        @deprecated use MapUtil.elementsArray
        @see ariba.util.core.MapUtil#elementsArray
    */
    public Object[] elementsArray ()
    {
        if (count == 0) {
            return emptyArray;
        }

        Object[] array = new Object[count];
        int arrayCount = 0;

        for (int i = 0; i < elements.length && arrayCount < count; i++) {
            Object element = elements[i];
            if (element != null && element != DeletedMarker) {
                array[arrayCount++] = element;
            }
        }

        return array;
    }

    /**
        Checks if the Hashtable contains the specified element.  This
        method is slow -- O(n) -- because it must scan the table
        searching for the element.

        @param element the element to search for in the Hashtable
        @exception NullPointerException if element is null

        @return Returns <b>true</b> if the Hashtable contains the
        element.
        @aribaapi ariba
    */
    public boolean contains (Object element)
    {
        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (count == 0) {
            return false;
        }

        if (element == null) {
            throw new NullPointerException();
        }

        if (elements == null) {
            return false;
        }

        for (int i = 0; i < elements.length; i++) {
            Object tmp = elements[i];
            if (tmp != null && tmp != DeletedMarker && element.equals(tmp)) {
                return true;
            }
        }

        return false;
    }

    /**
        Checks if the Hashtable has an entry matching the key
        <b>key</b>.

        @param key the key to search for in the Hashtable
        @exception NullPointerException if key is null

        @return <b>true</b> if the Hashtable contains the key <b>key</b>.
        @aribaapi documented
    */
    public boolean containsKey (Object key)
    {
        return (get(key) != null);
    }

    /**
        Searches the Hashtable and returns the element associated with
        the matched key.

        @param key the key to search the hashtable's keys
        for. Hashtable hashes and compares <b>key</b> using
        <b>hashCode()</b> and <b>equals()</b>.

        @exception NullPointerException if <b>key</b> is null and this <b>Hashtable</b>
        is not empty.

        @return the element associated with the <b>key</b>. This
        method returns <b>null</b> if the Hashtable does not contain
        <b>key</b>.
        @aribaapi documented
    */
    public Object get (Object key)
    {
            // We need to short-circuit here since the data arrays may not have
            // been allocated yet.
        if (count == 0) {
            return null;
        }

        Object element = elements[tableIndexFor(key)];
        return (element == DeletedMarker)  ? null : element;
    }

    /**
        Finds and removes <b>key</b> and the element associated with
        it from the Hashtable.

        @param key the key to search the hashtable's keys
        for. Hashtable hashes and compares <b>key</b> using
        <b>hashCode()</b> and <b>equals()</b>.

        @return the element associated with the <b>key</b> removed, or
        <b>null</b> if <b>key</b> was not present.
        @aribaapi documented
    */
    public Object remove (Object key)
    {
        if (key == null) {
                // map interface allows NPE, not FatalAssertException
            throw new NullPointerException();
        }

        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (count == 0) {
            return null;
        }

        int index = tableIndexFor(key);
        Object oldValue = elements[index];
        if (oldValue == null || oldValue == DeletedMarker) {
            return null;
        }
        removeAt(index);

        return oldValue;
    }


    private void removeAt (int index)
    {
        if (keys.length <= linearSearchThreshold) {
            int lastActive = count-1;
            elements[index] = elements[lastActive];
            keys[index] = keys[lastActive];
            elements[lastActive] = null;
            keys[lastActive] = null;
        }
        else {
            keys[index] = DeletedMarker;
            elements[index] = DeletedMarker;
        }

        count--;
    }

    /**
        Finds and removes all occurrences of <b>element</b> from the Hashtable.
        Note that this method is slow -- O(n) -- because it must scan the table
        searching for the element.

        @param element the element to search for.  Hashtable compares elements
        using <b>equals()</b>.
        @deprecated use MapUtil.removeElement
        @see ariba.util.core.MapUtil#removeElement

        @return the number of elements removed.
        @aribaapi ariba
    */
    public int removeElement (Object element)
    {
        Vector removedKeys = keysForRemovedElement(element);
        return (removedKeys == null ? 0 : removedKeys.count());
    }

    /**
        Finds and removes all occurrences of <b>element</b> from the Hashtable.
        Note that this method is slow -- O(n) -- because it must scan the table
        searching for the element. 

        @param element the element to search for.  Hashtable compares elements
        using <b>equals()</b>.

        @return a Vector containing the keys whose values are the element to be
        removed.
        @aribaapi private
    */
    protected Vector keysForRemovedElement (Object element)
    {
        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (count == 0) {
            return null;
        }

        if (element == null) {
            throw new NullPointerException();
        }

        if (elements == null) {
            return null;
        }

        Vector removedKeys = new Vector();
        for (int i = 0; i < elements.length; i++) {
            Object tmp = elements[i];
            if (tmp != null && tmp != DeletedMarker && element.equals(tmp)) {
                removedKeys.add(keys[i]);
                if (keys.length <= linearSearchThreshold) {
                    int lastActive = count-1;
                    elements[i] = elements[lastActive];
                    keys[i] = keys[lastActive];
                    elements[lastActive] = null;
                    keys[lastActive] = null;
                        // trick so we re-process this slot
                    i--;
                }
                else {
                    keys[i] = DeletedMarker;
                    elements[i] = DeletedMarker;
                }
                count--;
            }
        }

        return removedKeys;
    }

    /**
        Places the <b>key</b>/<b>element</b> pair in the Hashtable. If
        there is a key in the Hashtable matching <b>key</b> the
        corresponding element in the Hashtable is replaced by the
        specified <b>element</b>

        @param key the key to place into the Hashtable; may not be
        null
        @param element the element to place into the Hashtable; may
        not be null

        @return the element previously associated with the key if any,
        <b>null</b> otherwise.

        @exception NullPointerException if <b>element</b> or
        <b>key</b> are null
        @aribaapi documented
    */
    public Object put (Object key, Object element)
    {
        if (element == null) {
            throw new NullPointerException();
        }

        // Since we delay allocating the data arrays until we actually need
        // them, check to make sure they exist before attempting to put
        // something in them.

        if (keys == null) {
            grow();
        }

        int index = tableIndexFor(key);
        Object oldValue = elements[index];

        // If the total number of occupied slots (either with a real element or
        // a removed marker) gets too big, grow the table.

        if (oldValue == null || oldValue == DeletedMarker) {
            if (oldValue != DeletedMarker) {
                if (totalCount >= capacity) {
                    grow();
                    return put(key, element);
                }
                totalCount++;
            }
            count++;
        }

        keys[index] = key;
        elements[index] = element;

        return (oldValue == DeletedMarker) ? null : oldValue;
    }

    /**
        Helper function that returns the appropriate hash code for the
        object <b>o</b>. Unless overriden by a subclass, this will
        return the object's hashCode().

        @param o object
        @return hash code for <code>o</code>
        @aribaapi private
    */
    protected int getHashValueForObject (Object o)
    {
        return o.hashCode();
    }

    /**
        Helper function to determine if two objects are equal. This
        method is overriden for different types of hash tables. Unless
        overriden by a subclass, it returns true if <b>obj1</b> and
        <b>obj2</b> compare as equal using the equals() method on
        <b>obj1</b>

        @param obj1 object to compare
        @param obj2 object to compare
        @return true if <b>obj1</b> and <b>obj2</b> compare as equal using the
                equals() method on <b>obj1</b>
        @aribaapi private
    */
    protected boolean objectsAreEqualEnough (Object obj1, Object obj2)
    {
            // Looking at the implementation of visualcafe 1.1 and sun
            // String.equals() isn't smart enough to avoid full
            // compares when strings are pointer eq. (HP does)
        if (obj1 == obj2) {
            return true;
        }
        return obj1.equals(obj2);
    }

    /**
        Primitive method used internally to find slots in the
        table. If the key is present in the table, this method will
        return the index under which it is stored. If the key is not
        present, then this method will return the index under which it
        can be put. The caller must look at the data at that index to
        differentiate between the two possibilities.

        @param key key
        @return index under which key is stored
        @aribaapi private
    */
    protected int tableIndexForLinearSearch (Object key)
    {
        for (int i = 0; i < count; i++) {
            if (keys[i] == key) {
                return i;
            }
        }
        for (int i = 0; i < count; i++) {
            Object tempKey = keys[i];
            if (tempKey == null) {
                Log.util.error(2755, i, count, keys.length);
                    // make half assed assumption that the key is not
                    // in the table, and return the supposed index of
                    // next element to use
                return count;
            }
            if (objectsAreEqualEnough(tempKey, key)) {
                return i;
            }
        }
        Assert.that(count <= elements.length, "Hashtable overflow");
        return count;
    }

    /**
        Primitive method used internally to find slots in the
        table. If the key is present in the table, this method will return the
        index
        under which it is stored. If the key is not present, then this
        method will return the index under which it can be put. The caller
        must look at the hashCode at that index to differentiate between
        the two possibilities.

        @param key key, may not be <b>null</b>
        @return index under which key is stored
        @exception NullPointerException if <b>key</b> is null
        @aribaapi private
    */
    private int tableIndexFor (Object key)
    {
        if (key == null) {
            throw new NullPointerException();            
        }
        
        if (keys.length <= linearSearchThreshold) {
            return tableIndexForLinearSearch(key);
        }
        int hash = getHashValueForObject(key);
        int product = hash * A;
        int index = product >>> shift;

        // Probe the first slot in the table.  We keep track of the first
        // index where we found a REMOVED marker so we can return that index
        // as the first available slot if the key is not already in the table.

        Object oldKey = keys[index];
        if (oldKey == null || objectsAreEqualEnough(key, oldKey)) {
            return index;
        }

        int removedIndex = (oldKey == DeletedMarker) ? index : -1;

        // Our first probe has failed, so now we need to start looking
        // elsewhere in the table.

        int step = ((product >>> (2 * shift - 32)) & indexMask) | 1;
        int probeCount = 1;
        do {
            probeCount++;
            index = (index + step) & indexMask;

            Object testKey = keys[index];
            if (testKey == null) {
                if (removedIndex < 0) {
                    return index;
                }
                return removedIndex;
            }
            if (objectsAreEqualEnough(key, testKey)) {
                return index;
            }
            if (testKey == DeletedMarker && removedIndex == -1) {
                removedIndex = index;
            }

        }
        while (probeCount <= totalCount);

        // Something very bad has happened.
        Assert.that(false, "Hashtable overflow");
        return -1;
    }

    /**
        Grows the table to accommodate at least <b>capacity</b> number
        of elements.

        @param capacity the minimum number of elements the Hashtable
        must be able to hold without growing
        @aribaapi private
    */
    private void grow (int capacity)
    {
        int tableSize, power;

        // Find the lowest power of 2 size for the table which will allow
        // us to insert initialCapacity number of objects before having to
        // grow.

        tableSize = (capacity * 4) / 3;

        power = 3;
        while ((1 << power) < tableSize)
            power++;

        // Once shift is set, then grow() will do the right thing when
        // called.

        shift = 32 - power + 1;
        grow();
    }

    /**
        Grows the table by a factor of 2 (or creates it if necessary).
        All the REMOVED markers go away and the elements are rehashed
        into the bigger table.
    */
    private void grow ()
    {
        // The table size needs to be a power of two, and it should double
        // when it grows.  We grow when we are more than 3/4 full.

        shift--;
        int power = 32 - shift;
        indexMask = (1 << power) - 1;
        capacity = (3 * (1 << power)) / 4;

        Object[] oldKeys = keys;
        Object[] oldValues = elements;

        keys = new Object[1 << power];
        elements = new Object[1 << power];

        // Reinsert the old elements into the new table if there are any.  Be
        // sure to reset the counts and increment them as the old entries are
        // put back in the table.

        totalCount = 0;

        if (count > 0) {
            count = 0;

            for (int i = 0; i < oldKeys.length; i++) {
                Object key = oldKeys[i];

                if (key != null && key != DeletedMarker) {
                    int index = tableIndexFor(key);

                    keys[index] = key;
                    elements[index] = oldValues[i];

                    count++;
                    totalCount++;
                }
            }
        }
    }

    /**
        Removes all keys and elements from the Hashtable.
        @aribaapi documented
    */
    public void clear ()
    {
        int i;

        if (keys == null) {
            return;
        }

        for (i = 0; i < keys.length; i++) {
            keys[i] = null;
            elements[i] = null;
        }

        count = 0;
        totalCount = 0;
    }

    /**
        Returns the Hashtable's string representation.
        @return the Hashtable's string representation
        @aribaapi documented
    */
    public String toString ()
    {
        return FormattingSerializer.serializeObject(this);
    }

    /**
        Converts a string into a string array for persistence purposes.

        @return string array serialization of this Hashtable
        @aribaapi private
    */
    public String[] toStringArray ()
    {
        return MapUtil.toStringArray(this);
    }

    /**
        Returns a string serialization of the Hashtable using the
        Serializer.

        @see Serializer

        @return string serialization of this Hashtable
        @aribaapi private
    */
    public String toSerializedString ()
    {
        return MapUtil.toSerializedString(this);
    }

    /**
        Populates the hashtable with serialized data from the string.

        @param serialized String containing serialized Hashtable data
        @aribaapi private
    */
    public void fromSerializedString (String serialized)
    {
        MapUtil.fromSerializedString(this, serialized);
    }

    /**
        Implementation of Externalizable interface.  Saves the contents of this
        Hashtable object.

        @serialData iterates through entries, calling writeObject() on each key
                    followed by each value

        @param output the stream to write the object to
        @exception IOException Includes any I/O exceptions that may occur
        @aribaapi private
    */
    public void writeExternal (ObjectOutput output) throws IOException
    {
        int count = count();
        output.writeInt(count);
        Enumeration keys = keys();
        while (keys.hasMoreElements()) {
            --count;
            Object key = keys.nextElement();
            if (key == null) {
                Assert.that(false, "Null key in hash table.");
            }
            output.writeObject(key);
            output.writeObject(get(key));
        }
        Assert.that(count == 0, "count is not equal to number of keys");
    }

    /**
        Implementation of Externalizable interface.  Reads the contents into
        this Hashtable.

        @param input the stream to read data from in order to restore the object
        @exception IOException if I/O errors occur
        @exception ClassNotFoundException If the class for an object being
                     restored cannot be found.
        @aribaapi private
    */
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        int count = input.readInt();
        for (int i = 0; i < count; i++) {
            put(input.readObject(), input.readObject());
        }
    }

        /****************************************
         Implementation of AbstractMap
        *****************************************/
    /**
        Note: While iterating through a set of entries in the Hashtable, do not
        keep references to Key.Entry objects after a subsequent call to next().
        Each entry is valid from the point where next() is called until
        the subsequent call to next(), at which point the old entry is invalid.

        Entries do remain valid after a call to remove(), however.

        @return a set of Key.Entry objects
        @aribaapi documented
    */
    public Set entrySet ()
    {
        return new HashtableEntrySet();
    }

    private class HashtableEntrySet extends AbstractSet
    {
        /**
            Returns an iterator over the elements contained in this collection.

            @return an iterator over the elements contained in this collection.
            @aribaapi private
        */
        public Iterator iterator ()
        {
            return new HashtableIterator(HashtableIterator.Entries);
        }

        /**
            Returns the number of elements in this collection.  If the collection
            contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
            <tt>Integer.MAX_VALUE</tt>.

            @return the number of elements in this collection.
            @aribaapi private
        */
        public int size ()
        {
            return Hashtable.this.size();
        }
    }

    private class HashtableEntry implements Map.Entry
    {
        private int _index = -1;
        private Object _key;
        private Object _value;

        /**
            Creates new HashtableEntry.

            @return new HashtableEntry
            @aribaapi private
        */
        public HashtableEntry ()
        {
        }

        /**
            Sets the index for this HashtableEntry.

            @param i index
            @aribaapi private
        */
        public void setIndex (int i)
        {
            _index = i;
            _key = Hashtable.this.keys[_index];
            _value = Hashtable.this.elements[_index];
        }

        /**
            Returns the key corresponding to this entry.

            @return the key corresponding to this entry.
            @aribaapi private
        */
        public Object getKey ()
        {
            return _key;
        }

        /**
            Returns the value corresponding to this entry.  If the mapping
            has been removed from the backing map (by the iterator's
            <tt>remove</tt> operation), the results of this call are undefined.

            @return the value corresponding to this entry.
            @aribaapi private
        */
        public Object getValue ()
        {
            return _value;
        }

        /**
            Replaces the value corresponding to this entry with the specified
            value (optional operation).  (Writes through to the map.)  The
            behavior of this call is undefined if the mapping has already been
            removed from the map (by the iterator's <tt>remove</tt> operation).

            @param value new value to be stored in this entry.
            @return old value corresponding to the entry.

            @throws UnsupportedOperationException if the <tt>put</tt> operation
                     is not supported by the backing map.
            @throws ClassCastException if the class of the specified value
                      prevents it from being stored in the backing map.
            @throws    IllegalArgumentException if some aspect of this value
                     prevents it from being stored in the backing map.
            @throws NullPointerException the backing map does not permit
                     <tt>null</tt> values, and the specified value is
                     <tt>null</tt>.
            @aribaapi private
        */
        public Object setValue (Object value)
        {
            Object oldValue = Hashtable.this.elements[_index];
            Hashtable.this.elements[_index]=value;
            return oldValue;
        }

        /**
            Compares the specified object with this entry for equality.
            Returns <tt>true</tt> if the given object is also a map entry and
            the two entries represent the same mapping.

            @param o object to be compared for equality with this map entry.
            @return <tt>true</tt> if the specified object is equal to this map
                    entry.
            @aribaapi private
        */
        public boolean equals (Object o)
        {
            if (! (o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e2 = (Map.Entry)o;

            return (getKey()==null ?
                e2.getKey()==null : getKey().equals(e2.getKey()))  &&
                (getValue()==null ?
                e2.getValue()==null : getValue().equals(e2.getValue()));
        }

        /**
            Returns the hash code value for this map entry.

            @return the hash code value for this map entry.
            @see Object#hashCode()
            @see Object#equals(Object)
            @see #equals(Object)
            @aribaapi private
        */
        public int hashCode ()
        {
            return _index;
        }
    }

    private class HashtableIterator implements Iterator
    {
        int index;
        int returnedCount;
        int type;
        boolean canRemove = false;

            // For efficiency, keep only one instead of creating
            // a new one for each iteration.  Iterators should be used only once, and
            // should not be shared.
        final HashtableEntry currentEntry = new HashtableEntry();

        public static final int Keys = 1;
        public static final int Values = 2;
        public static final int Entries = 3;

        HashtableIterator (int type)
        {
            this.type = type;
            returnedCount = 0;

            if (Hashtable.this.keys != null) {
                index = Hashtable.this.keys.length;
            }
            else {
                index = 0;
            }
        }

        /**
            Returns <tt>true</tt> if the iteration has more elements. (In other
            words, returns <tt>true</tt> if <tt>next</tt> would return an element
            rather than throwing an exception.)

            @return <tt>true</tt> if the iterator has more elements.
            @aribaapi private
        */
        public boolean hasNext ()
        {
            return (returnedCount < Hashtable.this.count);
        }

        /**
            Returns the next element in the interation.

            @return the next element in the iteration.
            @exception NoSuchElementException iteration has no more elements.
            @aribaapi private
        */
        public Object next ()
        {
            index--;

            while (index >= 0 &&
                   (Hashtable.this.elements[index] == null ||
                    Hashtable.this.elements[index] == Hashtable.DeletedMarker)) {
                index--;
            }

            if (index < 0 || returnedCount >= Hashtable.this.count) {
                throw new java.util.NoSuchElementException();
            }

            canRemove = true;
            returnedCount++;

            switch (type) {
                case Keys:
                    return Hashtable.this.keys[index];
                case Values:
                    return Hashtable.this.elements[index];
                case Entries:
                        // Avoid creating a new HashtableEntry for efficiency reasons.
                    currentEntry.setIndex(index);
                    return currentEntry;
                default:
                    Assert.that(false,"Invalid type");
                    return null;
            }
        }

        /**
            Removes from the underlying collection the last element returned by the
            iterator (optional operation).  This method can be called only once per
            call to <tt>next</tt>.  The behavior of an iterator is unspecified if
            the underlying collection is modified while the iteration is in
            progress in any way other than by calling this method.

            @exception UnsupportedOperationException if the <tt>remove</tt>
                     operation is not supported by this Iterator.

            @exception IllegalStateException if the <tt>next</tt> method has not
                     yet been called, or the <tt>remove</tt> method has already
                     been called after the last call to the <tt>next</tt>
                     method.

            @aribaapi private
        */
        public void remove ()
        {
            if (! canRemove) {
                throw new IllegalStateException("Removing before calling next()");
            }
            canRemove = false;
            Hashtable.this.removeAt(index);
            returnedCount--;
        }
    }

    private transient Set keySet = null;
    private transient Collection values = null;

    /**
        Returns a Set view of the keys contained in this map.  The Set is
        backed by the map, so changes to the map are reflected in the Set,
        and vice-versa.  (If the map is modified while an iteration over
        the Set is in progress, the results of the iteration are undefined.)
        The Set supports element removal, which removes the corresponding entry
        from the map, via the Iterator.remove, Set.remove,  removeAll
        retainAll, and clear operations.  It does not support the add or
        addAll operations.<p>

        @return a Set view of the keys contained in this map.
        @aribaapi documented
    */
    public Set keySet ()
    {
        if (keySet == null) {
            keySet = new AbstractSet()
            {
                public Iterator iterator ()
                {
                    return new HashtableIterator(HashtableIterator.Keys);
                }
                public int size ()
                {
                    return count;
                }
                public boolean contains (Object o)
                {
                    return containsKey(o);
                }
                public boolean remove (Object o)
                {
                    int oldSize = count;
                    Hashtable.this.remove(o);
                    return count != oldSize;
                }
                public void clear ()
                {
                    Hashtable.this.clear();
                }
            };
        }
        return keySet;
    }

    /**
        Returns a collection view of the values contained in this map.  The
        collection is backed by the map, so changes to the map are reflected in
        the collection, and vice-versa.  (If the map is modified while an
        iteration over the collection is in progress, the results of the
        iteration are undefined.)  The collection supports element removal,
        which removes the corresponding entry from the map, via the
        <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
        <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
        It does not support the <tt>add</tt> or <tt>addAll</tt> operations.<p>

        @return a collection view of the values contained in this map.
        @aribaapi documented
    */
    public Collection values ()
    {
        if (values == null) {
            values = new AbstractCollection ()
            {
                public Iterator iterator ()
                {
                    return new HashtableIterator(HashtableIterator.Values);
                }
                public int size ()
                {
                    return count;
                }
                public boolean contains (Object o)
                {
                    return containsValue(o);
                }
                public void clear ()
                {
                    Hashtable.this.clear();
                }
            };
        }
        return values;
    }

}
