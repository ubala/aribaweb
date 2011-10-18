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

    $Id$
*/

package ariba.util.core;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
    A version of Hashtable that...

    - Is built on the code base of the ariba.util.core.Hashtable

    - Only has methods for querying and putting elements. There is no
      ability to remove an element.

    - Has synchronization built in to allow the following:

       1) No synchronization is necessary (either internally or
       externally) on get() operations

       2) The put() method is internally synchronized on the this
       ref. If the program is not concerned about which object ends up
       in the table after two concurrent puts, no additional
       synchronization is needed. But if a program desires to ensure
       that only one is entered, external synchronization can be done
       on the object to allow synchronization between a retry of a
       failed get and a put. See the
       ariba.util.core.InternCharToString for an example of this.

    -- I believe that a clear() method could also be added without
       requiring synchronization, however I do not have the time to
       validate this.

    @aribaapi ariba
*/
public class GrowOnlyHashtable<K,V> extends AbstractMap<K,V>
{
    static private class Storage<K,V>
    {
        int count;
        int totalCount;
        int shift;
        int capacity;
        int indexMask;

        K keys[];
        V elements[];
        public Storage ()
        {
        }
        public Storage (Storage o)
        {
            count = o.count;
            totalCount = o.totalCount;
            shift = o.shift;
            capacity = o.capacity;
            indexMask = o.indexMask;
        }
    }
    private Storage<K,V> storage = new Storage<K,V>();

    public static final String ClassName = "ariba.util.core.GrowOnlyHashtable";

    private static final int MaxStringLen = 254;

    /**
        For the multiplicative hash, choose the golden ratio:
        <pre>
            A = ((sqrt(5) - 1) / 2) * (1 << 32)
        </pre>
        ala Knuth...
    */
    static final int A = 0x9e3779b9;

    static final String keysField = "keys";
    static final String elementsField = "elements";

    static final int DefaultSize = 2;

    static final Object DeletedMarker = new Object();
    private static final Object emptyArray[] = new Object[0];

    /**
        Constructs an empty Hashtable. The Hashtable will grow on demand
        as more elements are added.
    */
    public GrowOnlyHashtable ()
    {
        super();
        storage.shift = 32 - MathUtil.log2(DefaultSize) + 1;
    }

    /**
        Constructs a Hashtable capable of holding at least
        <b>initialCapacity</b> elements before needing to grow.
    */
    public GrowOnlyHashtable (int initialCapacity)
    {
        this();

        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0");
        }

        grow(initialCapacity);
    }

    /**
        Returns the number of elements in the GrowOnlyHashtable.
    */
    public int count ()
    {
        Storage tmpStorage = storage;
        return tmpStorage.count;
    }

    /**
        Returns the number of elements in the GrowOnlyHashtable.
    */
    public int size ()
    {
        return count();
    }

    /**
        Returns the current capacity of the GrowOnlyHashtable.
    */
    public int capacity ()
    {
        return storage.capacity;
    }

    /**
        Returns <b>true</b> if there are no elements in the GrowOnlyHashtable.
    */
    public boolean isEmpty ()
    {
        return (count() == 0);
    }

    /**
        Returns an Object array containing the Hashtable's keys. If the
        Hashtable contains no keys, an empty array is returned.
        @return the Object array above mentioned.
    */
    public Object[] keysArray ()
    {
        Storage<K,V> tmpStorage = storage;
        int count = tmpStorage.count;
        K[] keys = tmpStorage.keys;

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
        Returns an Object array containing the Hashtable's
        elements. If there the hashtable contains no elements, an
        empty array is returned.
        @return the Object array mentioned above.
    */
    public Object[] elementsArray ()
    {
        Storage<K,V> tmpStorage = storage;
        int count = tmpStorage.count;
        Object[] elements = tmpStorage.elements;


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
        Returns <b>true</b> if the GrowOnlyHashtable contains the
        element. This method is slow -- O(n) -- because it must scan
        the table searching for the element.
    */
    public boolean contains (V element)
    {
        int i;
        Object tmp;
        Storage tmpStorage = storage;
        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (tmpStorage.count == 0) {
            return false;
        }

        if (element == null) {
            throw new NullPointerException();
        }

        if (tmpStorage.elements == null) {
            return false;
        }

        for (i = 0; i < tmpStorage.elements.length; i++) {
            tmp = tmpStorage.elements[i];
            if (tmp != null && tmp != DeletedMarker && element.equals(tmp)) {
                return true;
            }
        }

        return false;
    }

    /**
        Returns <b>true</b> if the GrowOnlyHashtable contains the key
        <b>key</b>.
    */
    public boolean containsKey (Object key)
    {
        return (get(key) != null);
    }

    /**
        Returns the element associated with the <b>key</b>. This
        method returns <b>null</b> if the GrowOnlyHashtable does not
        contain <b>key</b>.  GrowOnlyHashtable hashes and compares
        <b>key</b> using <b>hashCode()</b> and <b>equals()</b>.
    */
    public V get (Object key)
    {
        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.
        Storage<K,V> tmpStorage = storage;
        if (tmpStorage.count == 0) {
            return null;
        }
        int index = tableIndexFor(tmpStorage, (K)key, true);
        if (index == -1) {
            return null;
        }
        V element = tmpStorage.elements[index];
        return (element == DeletedMarker)  ? null : element;
    }


    /**
        Places the <b>key</b>/<b>element</b> pair in the
        GrowOnlyHashtable. Neither <b>key</b> nor <b>element</b> may
        be <b>null</b>. Returns the old element associated with
        <b>key</b>, or <b>null</b> if the <b>key</b> was not present.
    */
    public V put (K key, V element)
    {
        if (element == null) {
            throw new NullPointerException();
        }
        synchronized (this) {

                // Since we delay allocating the data arrays until we
                // actually need them, check to make sure they exist
                // before attempting to put something in them.

            if (storage.keys == null) {
                grow();
            }

            int index = tableIndexFor(storage, key, false);
            V oldValue = storage.elements[index];

                // If the total number of occupied slots (either with
                // a real element or a removed marker) gets too big,
                // grow the table.

            if (oldValue == null || oldValue == DeletedMarker) {
                if (oldValue != DeletedMarker) {
                    if (storage.totalCount >= storage.capacity) {
                        grow();
                        return put(key, element);
                    }
                    storage.totalCount++;
                }
                storage.count++;
            }
                // put element first, so other methods can check for
                // key, and if the key exists, they know the element
                // is valid

            storage.elements[index] = element;
            storage.keys[index] = key;

            return (oldValue == DeletedMarker) ? null : oldValue;
        }
    }

    protected int getHashValueForObject (K o)
    {
        return o.hashCode();
    }

    protected boolean objectsAreEqualEnough (K obj1, K obj2)
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
        Primitive method used internally to find slots in the table.
        If the key is present in the table, this method will return
        the index under which it is stored. If the key is not present,
        then this method will return the index under which it can be
        put. The caller must look at the hashCode at that index to
        differentiate between the two possibilities.
    */
    private int tableIndexFor (Storage<K,V> tmpStorage,
                               K key,
                               boolean failIfNoObject)
    {
        int hash = getHashValueForObject(key);
        int product = hash * A;
        int index = product >>> tmpStorage.shift;

        // Probe the first slot in the table.  We keep track of the first
        // index where we found a REMOVED marker so we can return that index
        // as the first available slot if the key is not already in the table.

        K oldKey = tmpStorage.keys[index];
        if (oldKey == null || objectsAreEqualEnough(key, oldKey)) {
            return index;
        }

        int removedIndex = (oldKey == DeletedMarker) ? index : -1;

        // Our first probe has failed, so now we need to start looking
        // elsewhere in the table.

        int step = ((product >>> (2 * tmpStorage.shift - 32)) &
                    tmpStorage.indexMask) | 1;
        int probeCount = 1;
        do {
            probeCount++;
            index = (index + step) & tmpStorage.indexMask;

            K testKey = tmpStorage.keys[index];
            if (testKey == null) {
                if (failIfNoObject) {
                    return -1;
                }
                if (removedIndex < 0) {
                    return index;
                }
                return removedIndex;
            }
            else if (objectsAreEqualEnough(key, testKey)) {
                return index;
            }
            else if (testKey == DeletedMarker && removedIndex == -1) {
                removedIndex = index;
            }

        }
        while (probeCount <= tmpStorage.totalCount);

        // Something very bad has happened.
        Assert.that(false, "GrowOnlyHashtable overflow");
        return -1;
    }

    /**
        Grows the table to accommodate at least capacity number of
        elements. Since this is only called from the constructor,
        don't need to worry about synchronization with the shift
        element of the storage.
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

        storage.shift = 32 - power + 1;
        grow();
    }

    /**
        Grows the table by a factor of 2 (or creates it if necessary).
        All the REMOVED markers go away and the elements are rehashed
        into the bigger table.
    */
    private void grow ()
    {
        Storage<K,V> tmpStorage = storage;
        Storage<K,V> newStorage = new Storage(tmpStorage);

        int count = tmpStorage.count;
        // The table size needs to be a power of two, and it should double
        // when it grows.  We grow when we are more than 3/4 full.

        newStorage.shift--;
        int power = 32 - newStorage.shift;
        newStorage.indexMask = (1 << power) - 1;
        newStorage.capacity = (3 * (1 << power)) / 4;

        K[] oldKeys = tmpStorage.keys;
        V[] oldValues = tmpStorage.elements;

        newStorage.keys = (K[])new Object[1 << power];
        newStorage.elements = (V[])new Object[1 << power];

        // Reinsert the old elements into the new table if there are any.  Be
        // sure to reset the counts and increment them as the old entries are
        // put back in the table.

        newStorage.totalCount = 0;

        if (count > 0) {
            count = 0;

            for (int i = 0; i < oldKeys.length; i++) {
                K key = oldKeys[i];

                if (key != null && key != DeletedMarker) {
                    int index = tableIndexFor(newStorage, key, false);

                    newStorage.keys[index] = key;
                    newStorage.elements[index] = oldValues[i];

                    count++;
                    newStorage.totalCount++;
                }
            }
        }
        newStorage.count = count;
        storage = newStorage;
    }

    /**
        Returns a List containing the Hashtable's keys.
    */
    public List<K> keysList ()
    {
        int i, vectCount;
        K key;
        List<K> vect;

        Storage<K,V> tmpStorage = storage;
        int count = tmpStorage.count;

        if (count == 0) {
            return ListUtil.list();
        }

        vect = ListUtil.list(count);
        vectCount = 0;

        K keys[] = tmpStorage.keys;

        for (i = 0; i < keys.length && vectCount < count; i++) {
            key = keys[i];
            if (key != null && key != DeletedMarker) {
                vect.add(key);
                vectCount++;
            }
        }

        return vect;
    }

    public String toString ()
    {
        Storage tmpStorage = storage;
        Hashtable output = new Hashtable(tmpStorage.count);
        Object keys[] = tmpStorage.keys;
        Object values[] = tmpStorage.elements;
        if (keys != null) {
            for (int i=0; i<keys.length; i++) {
                Object key = keys[i];
                if (key != null && key != DeletedMarker) {
                    output.put(key, values[i]);
                }
            }
        }
        return output.toString();
    }

    ////////////////////////////////////////////////////////////////
    //  for AbstractMap implementation
    ////////////////////////////////////////////////////////////////

    /**
        @see java.util.Map#entrySet
    */
    public Set<Entry<K,V>> entrySet ()
    {
        return new GrowOnlyHashtableEntrySet<K,V>(storage);
    }

    private static class GrowOnlyHashtableEntrySet<K,V> extends AbstractSet<Entry<K,V>>
    {
        private final Storage<K,V> _storage;

        GrowOnlyHashtableEntrySet (Storage<K,V> storage)
        {
            _storage = storage;
        }

        public Iterator iterator ()
        {
            return new GrowOnlyHashtableIterator(_storage);
        }

        public int size ()
        {
            return _storage.count;
        }
    }

    private static class GrowOnlyHashtableEntry<K,V> implements Map.Entry<K,V>
    {
        private final Storage<K,V> _storage;
        private int _index = -1;
        private K _key;
        private V _value;

        GrowOnlyHashtableEntry (Storage<K,V> storage)
        {
            _storage = storage;
        }

        public void setIndex (int i)
        {
            _index = i;
            _key = _storage.keys[_index];
            _value = _storage.elements[_index];
        }

        public K getKey ()
        {
            return _key;
        }

        public V getValue ()
        {
            return _value;
        }

        public Object setValue (Object value)
        {
            throw new UnsupportedOperationException();
        }

        public boolean equals (Object o)
        {
            if (! (o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e2 = (Map.Entry)o;

            if (o != null && hashCode() != o.hashCode()) {
                return false;
            }

            return (getKey()==null ?
                e2.getKey()==null : getKey().equals(e2.getKey()))  &&
                (getValue()==null ?
                e2.getValue()==null : getValue().equals(e2.getValue()));
        }

        public int hashCode ()
        {
            return _index;
        }
    }

    private static class GrowOnlyHashtableIterator implements Iterator
    {
        private final Storage _storage;
        private int _index;
        private int _returnedCount;
        private int _count;
        private final GrowOnlyHashtableEntry _currentEntry;

        GrowOnlyHashtableIterator (Storage storage)
        {
            _storage = storage;
            _count = _storage.count;
            _currentEntry = new GrowOnlyHashtableEntry(_storage);
            _returnedCount = 0;

            if (_storage.keys != null) {
                _index = _storage.keys.length;
            }
            else {
                _index = 0;
            }
        }

        public boolean hasNext ()
        {
            return (_returnedCount < _count);
        }

        public Object next ()
        {
            _index--;

            while (_index >= 0 &&
                   (_storage.elements[_index] == null ||
                    _storage.elements[_index] == GrowOnlyHashtable.DeletedMarker))
            {
                _index--;
            }

            if (_index < 0 || _returnedCount >= _count) {
                throw new java.util.NoSuchElementException();
            }

            _returnedCount++;

                // Avoid creating a new GrowOnlyHashtableEntry for efficiency reasons.
            _currentEntry.setIndex(_index);
            return _currentEntry;
        }

        public void remove ()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class IdentityMap<K, V> extends GrowOnlyHashtable<K, V>
    {
        protected boolean objectsAreEqualEnough (K obj1, K obj2)
        {
            return  obj1 == obj2;
        }

        protected int getHashValueForObject (K o)
        {
            return System.identityHashCode(o);
        }
    }
}

