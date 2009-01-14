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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWIntKeyHashtable.java#12 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWConstants;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.MathUtil;
import ariba.util.io.DeserializationException;
import ariba.util.io.Deserializer;
import ariba.util.io.FormattingSerializer;
import ariba.util.io.Serializer;
import ariba.util.log.Log;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
    This code duplicated from ariba.util.core.Hashtable and is temporary until we
    get this capability in ariba.util.core.

    @aribaapi private
*/
public final class AWIntKeyHashtable extends AbstractMap implements AWDisposable
{
    /**
        Name of this class.  Value is "ariba.util.core.Hashtable".

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.core.Hashtable";

    private static final int MaxStringLen = 254;
    private static final int linearSearchThreshold = 16;
    private static final Object EmptyElementArray[] = new Object[0];

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
    private static final int UninitializedKey = 0;
    private static final int DeletedKeyMarker = Integer.MIN_VALUE;
    private static final Object DeletedElementMarker = new Object();

    /**
        Number of elements currently in Hashtable.

        @aribaapi private
    */
    private int _count;

    /**
        Total number of occupied slots.

        @aribaapi private
    */
    private int _totalCount;

    private int _shift;

    /**
        Hashtable capacity.

        @aribaapi private
    */
    private int _capacity;

    private int _indexMask;

    /**
        Hashtable keys.

        @aribaapi private
    */
    private int _keys[];

    /**
        Hashtable elements.

        @aribaapi private
    */
    private Object _elements[];

    /**
        provides a values enumeration without generating lots of garbage.
    */
    private AWHashtableValueIterator _sharedValuesIterator;

    /**
        Disosable interface implementation
    */
    public void dispose ()
    {
        _keys = null;

        Object disposeTarget = _elements;
        _elements = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _sharedValuesIterator;
        _sharedValuesIterator = null;
        AWUtil.dispose(disposeTarget);
    }

    /**
        Constructs an empty Hashtable. The Hashtable will grow on demand
        as more elements are added.
        return new empty Hashtable
        @aribaapi private
    */
    public AWIntKeyHashtable ()
    {
        super();
        this._shift = 32 - MathUtil.log2(DefaultSize) + 1;
    }

    /**
        Constructs a Hashtable capable of holding a specified number
        of elements.

        @param initialCapacity the number of elements this Hashtable
        is capable of holding before needing to grow.
        return new empty Hashtable with given initial capacity
        @aribaapi private
    */
    public AWIntKeyHashtable (int initialCapacity)
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
        return new Hashtable with same contents as <code>source</code>
        @aribaapi private
    */
    public AWIntKeyHashtable (java.util.Hashtable source)
    {
        this(source.size());
        this.putAll(source);
    }

    /**
        Creates a shallow copy of the Hashtable. The table itself is cloned,
        but none of the keys or elements are copied.

        @return the cloned copy

        @throws InternalError if the cloning operation fails.

        @aribaapi private
    */
    public Object clone ()
    {
        AWIntKeyHashtable newTable;
        try {
            newTable = (AWIntKeyHashtable)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError("Error in clone(). This shouldn't happen.");
        }

        // If there is nothing in the table, then we cloned to make sure the
        // class was preserved, but just null everything out except for shift,
        // which implies the default initial capacity.

        if (_count == 0) {
            newTable._shift = 32 - MathUtil.log2(DefaultSize) + 1;
            newTable._totalCount = 0;
            newTable._capacity = 0;
            newTable._indexMask = 0;
            newTable._keys = null;
            newTable._elements = null;

            return newTable;
        }

        int len = _keys.length;
        newTable._keys = new int[len];
        newTable._elements = new Object[len];

        System.arraycopy(_keys, 0, newTable._keys, 0, len);
        System.arraycopy(_elements, 0, newTable._elements, 0, len);

        return newTable;
    }

    /**
        Returns the number of elements in the Hashtable.
        @return the number of elements in the Hashtable
        @aribaapi private
    */
    public int count ()
    {
        return _count;
    }

    /**
        Returns the number of elements in the Hashtable.
        @return the number of elements in the Hashtable
        @aribaapi private
    */
    public int size ()
    {
        return _count;
    }

    /**
        Indicates if the hashtable contains any elements.
        @return <b>true</b> if there are no elements in the Hashtable.
        @aribaapi private
    */
    public boolean isEmpty ()
    {
        return (_count == 0);
    }

    /**
        Returns an Iterator of the Hashtable's keys.

        @return an enumeration of the keys in this Hashtable.

        @see #elements
        @aribaapi private
    */
    /*
    public Iterator keys ()
    {
        return new AWIntKeyHashtableEnumerator(this, true);
    }
    */

    private AWHashtableValueIterator checkoutValueIterator ()
    {
        AWHashtableValueIterator enumeration = null;
        synchronized (this) {
            enumeration = _sharedValuesIterator;
            _sharedValuesIterator = null;
        }
        if (enumeration == null) {
            enumeration = new AWHashtableValueIterator(this);
        }
        enumeration.reset(_elements, size());
        return enumeration;
    }

    private void checkinValueIterator (AWHashtableValueIterator enumeration)
    {
        enumeration.reset(null, -1);
        _sharedValuesIterator = enumeration;
    }

    /**
        Returns an Iterator of the Hashtable's elements.

        @return an enumeration of the elements in this Hashtable.

        @see #_keys
        @aribaapi private
    */
    public Iterator elements ()
    {
        return checkoutValueIterator();
    }

    private static class AWHashtableValueIterator
        extends AWBaseObject
        implements Iterator, AWDisposable
    {
        private final AWIntKeyHashtable _intKeyHashtable;
        private Object[] _elements;
        private int _count;
        private int _currentElementIndex;
        private int _countRetuned;

        public void dispose ()
        {
            _elements = null;
        }

        protected AWHashtableValueIterator (AWIntKeyHashtable hashtable)
        {
            _intKeyHashtable = hashtable;
        }

        protected void reset (Object[] elements, int count)
        {
            _elements = elements;
            _count = count;
            _countRetuned = 0;
            _currentElementIndex = -1;
        }

        private void scanForNextElement ()
        {
            Object[] elements = _elements;
            int elemementsLength = elements.length;
            Object currentElement = null;
            while (_currentElementIndex < elemementsLength) {
                _currentElementIndex++;
                currentElement = elements[_currentElementIndex];
                if (currentElement != null && currentElement != DeletedElementMarker) {
                    break;
                }
            }
        }

        /**
            this does not do any checking of its own and will incur exceptions if called too many times.
            the exceptions are not intended to be informative since it does non of its own checking.
            @return
        */
        public Object next ()
        {
            scanForNextElement();
            _countRetuned++;
            return _elements[_currentElementIndex];
        }

        public boolean hasNext ()
        {
            boolean hasMoreElements = _countRetuned < _count;
            // Note: it will cause problems if the enumeration is used after hasMoreElements returns false.
            if (!hasMoreElements) {
                _intKeyHashtable.checkinValueIterator(this);
            }
            return hasMoreElements;
        }
        public void remove ()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
        Returns a Vector containing the Hashtable's keys. If the Hashtable
        contains no keys, return an empty Vector object.

        @return the Vector object mentioned above.
        @aribaapi private
    */
    public AWArrayManager keysVector ()
    {
        AWArrayManager vect = new AWArrayManager(Integer.TYPE, _count);
        if (_count == 0) {
            return vect;
        }

        int vectCount = 0;

        for (int i = 0, length = _keys.length; i < length && vectCount < _count; i++) {
            int key = _keys[i];
            if (key != DeletedKeyMarker) {
                vect.addElement(key);
                vectCount++;
            }
        }

        return vect;
    }

    /**
        Returns a Vector containing the Hashtable's elements. If the hashtable
        containes no elements, an empty Vector ojbect is returned.
        @return the Vector object mentioned above.
        @aribaapi private
    */
    public List elementsVector ()
    {
        if (_count == 0) {
            return ListUtil.list();
        }

        List vect = ListUtil.list(_count);
        int vectCount = 0;

        for (int i = 0, length = _elements.length;
             i < length && vectCount < _count;
             i++)
        {
            Object element = _elements[i];
            if (element != null && element != DeletedElementMarker) {
                vect.add(element);
                vectCount++;
            }
        }

        return vect;
    }

    /**
        Returns an Object array containing the Hashtable's keys. If the
        Hashtable contains no keys, an empty array is returned.
        @return the Object array above mentioned.
        @aribaapi private
    */
    public int[] keysArray ()
    {
        if (_count == 0) {
            return AWConstants.EmptyIntArray;
        }

        int[] array = new int[_count];
        int arrayCount = 0;

        for (int i = 0, length = _keys.length; i < length && arrayCount < _count; i++) {
            int key = _keys[i];
            if (key != DeletedKeyMarker) {
                array[arrayCount++] = key;
            }
        }

        return array;
    }
    /**
        Returns an Object array containing the Hashtable's elements.
        @return the Object array mentioned above. If there the hashtable contains
        no elements, an empty array is returned.
        @aribaapi private
    */
    public Object[] elementsArray ()
    {
        if (_count == 0) {
            return EmptyElementArray;
        }

        Object[] array = new Object[_count];
        int arrayCount = 0;

        for (int i = 0, length = _elements.length;
             i < length && arrayCount < _count;
             i++)
        {
            Object element = _elements[i];
            if (element != null && element != DeletedElementMarker) {
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
        @aribaapi private
    */
    public boolean contains (Object element)
    {
        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (_count == 0) {
            return false;
        }

        if (element == null) {
            throw new NullPointerException();
        }

        if (_elements == null) {
            return false;
        }

        for (int i = 0, length = _elements.length; i < length; i++) {
            Object tmp = _elements[i];
            if (tmp != null && tmp != DeletedElementMarker && element.equals(tmp)) {
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
        @aribaapi private
    */
    public boolean containsKey (int key)
    {
        key = translateZeroKey(key);
        return (get(key) != null);
    }

    /**
        Searches the Hashtable and returns the element associated with
        the matched key.

        @param key the key to search the hashtable's keys
        for. Hashtable hashes and compares <b>key</b> using
        <b>hashCode()</b> and <b>equals()</b>.

        @exception NullPointerException if <b>key</b> is null

        @return the element associated with the <b>key</b>. This
        method returns <b>null</b> if the Hashtable does not contain
        <b>key</b>.
        @aribaapi private
    */
    public Object get (int key)
    {
        key = translateZeroKey(key);
        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (_count == 0) {
            return null;
        }

        Object element = _elements[tableIndexFor(key)];
        return (element == DeletedElementMarker)  ? null : element;
    }

    public Object get (Object key)
    {
        throw new IllegalArgumentException("AWIntKeyHashtable only accepts ints as keys.");
    }

    /**
        Finds and removes <b>key</b> and the element associated with
        it from the Hashtable.

        @param key the key to search the hashtable's keys
        for. Hashtable hashes and compares <b>key</b> using
        <b>hashCode()</b> and <b>equals()</b>.

        @return the element associated with the <b>key</b> removed, or
        <b>null</b> if <b>key</b> was not present.
        @aribaapi private
    */
    public Object remove (int key)
    {
        key = translateZeroKey(key);

        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (_count == 0) {
            return null;
        }

        int index = tableIndexFor(key);
        Object oldValue = _elements[index];
        if (oldValue == null || oldValue == DeletedElementMarker) {
            return null;
        }
        removeAt(index);

        return oldValue;
    }


    private void removeAt (int index)
    {
        if (_keys.length <= linearSearchThreshold) {
            int lastActive = _count-1;
            _elements[index] = _elements[lastActive];
            _keys[index] = _keys[lastActive];
            _elements[lastActive] = null;
            _keys[lastActive] = UninitializedKey;
        }
        else {
            _keys[index] = DeletedKeyMarker;
            _elements[index] = DeletedElementMarker;
        }

        _count--;
    }

    /**
        Finds and removes all occurrences of <b>element</b> from the Hashtable.
        Note that this method is slow -- O(n) -- because it must scan the table
        searching for the element.

        @param element the element to search for.  Hashtable compares elements
        using <b>equals()</b>.

        @return the number of elements removed.
        @aribaapi private
    */
    public int removeElement (Object element)
    {
        // We need to short-circuit here since the data arrays may not have
        // been allocated yet.

        if (_count == 0) {
            return 0;
        }

        if (element == null) {
            throw new NullPointerException();
        }

        if (_elements == null) {
            return 0;
        }

        int numRemoved = 0;
        for (int i = 0, length = _elements.length; i < length; i++) {
            Object tmp = _elements[i];
            if (tmp != null && tmp != DeletedElementMarker && element.equals(tmp)) {
                if (_keys.length <= linearSearchThreshold) {
                    int lastActive = _count-1;
                    _elements[i] = _elements[lastActive];
                    _keys[i] = _keys[lastActive];
                    _elements[lastActive] = null;
                    _keys[lastActive] = UninitializedKey;
                        // trick so we re-process this slot
                    i--;
                }
                else {
                    _keys[i] = DeletedKeyMarker;
                    _elements[i] = DeletedElementMarker;
                }
                numRemoved++;
                _count--;
            }
        }

        return numRemoved;
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
        @aribaapi private
    */
    public Object put (int key, Object element)
    {
        key = translateZeroKey(key);
        if (element == null) {
            throw new NullPointerException();
        }

        // Since we delay allocating the data arrays until we actually need
        // them, check to make sure they exist before attempting to put
        // something in them.

        if (_keys == null) {
            grow();
        }

        int index = tableIndexFor(key);
        Object oldValue = _elements[index];

        // If the total number of occupied slots (either with a real element or
        // a removed marker) gets too big, grow the table.

        if (oldValue == null || oldValue == DeletedElementMarker) {
            if (oldValue != DeletedElementMarker) {
                if (_totalCount >= _capacity) {
                    grow();
                    return put(key, element);
                }
                _totalCount++;
            }
            _count++;
        }

        _keys[index] = key;
        _elements[index] = element;

        return (oldValue == DeletedElementMarker) ? null : oldValue;
    }

    public Object put (Object key, Object value)
    {
        throw new IllegalArgumentException("AWIntKeyHashtable only accepts ints as keys.");
    }

    /**
        Becuase zero is what the int[] is initialized to, we need some way to handle 0,
        so we simply convert it to MAX_VALUE.
    */
    private int translateZeroKey (int key)
    {
        if (key == 0) {
            return Integer.MAX_VALUE;
        }
        return key;
    }

    /**
        Helper function that returns the appropriate hash code for the
        object <b>o</b>. Unless overriden by a subclass, this will
        return the object's hashCode().

        @param intValue
        @return hash code for <code>o</code>
        @aribaapi private
    */
    private int getHashValueForObject (int intValue)
    {
        return intValue;
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
    private boolean objectsAreEqualEnough (int obj1, int obj2)
    {
            // Looking at the implementation of visualcafe 1.1 and sun
            // String.equals() isn't smart enough to avoid full
            // compares when strings are pointer eq. (HP does)
        return obj1 == obj2;
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
    private int tableIndexForLinearSearch (int key)
    {
        for (int i = 0; i < _count; i++) {
            if (_keys[i] == key) {
                return i;
            }
        }
        for (int i = 0; i < _count; i++) {
            int tempKey = _keys[i];
            if (tempKey == UninitializedKey) {
                Log.util.error(2755, i, _count, _keys.length);
                    // make half assed assumption that the key is not
                    // in the table, and return the supposed index of
                    // next element to use
                return _count;
            }
            if (tempKey == key) {
                return i;
            }
        }
        Assert.that(_count <= _elements.length, "Hashtable overflow");
        return _count;
    }

    /**
        Primitive method used internally to find slots in the
        table. If the key is present in the table, this method will return the
        index
        under which it is stored. If the key is not present, then this
        method will return the index under which it can be put. The caller
        must look at the hashCode at that index to differentiate between
        the two possibilities.

        @aribaapi private
    */
    private int tableIndexFor (int key)
    {
        if (_keys.length <= linearSearchThreshold) {
            return tableIndexForLinearSearch(key);
        }
        int hash = getHashValueForObject(key);
        int product = hash * A;
        int index = product >>> _shift;

        // Probe the first slot in the table.  We keep track of the first
        // index where we found a REMOVED marker so we can return that index
        // as the first available slot if the key is not already in the table.

        int oldKey = _keys[index];
        if (oldKey == UninitializedKey || key == oldKey) {
            return index;
        }

        int removedIndex = (oldKey == DeletedKeyMarker) ? index : -1;

        // Our first probe has failed, so now we need to start looking
        // elsewhere in the table.

        int step = ((product >>> (2 * _shift - 32)) & _indexMask) | 1;
        int probeCount = 1;
        do {
            probeCount++;
            index = (index + step) & _indexMask;

            int testKey = _keys[index];
            if (testKey == UninitializedKey) {
                if (removedIndex < 0) {
                    return index;
                }
                return removedIndex;
            }
            if (objectsAreEqualEnough(key, testKey)) {
                return index;
            }
            if (testKey == DeletedKeyMarker && removedIndex == -1) {
                removedIndex = index;
            }

        }
        while (probeCount <= _totalCount);

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

        _shift = 32 - power + 1;
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

        _shift--;
        int power = 32 - _shift;
        _indexMask = (1 << power) - 1;
        _capacity = (3 * (1 << power)) / 4;

        int[] oldKeys = _keys;
        Object[] oldValues = _elements;

        _keys = new int[1 << power];
        _elements = new Object[1 << power];

        // Reinsert the old elements into the new table if there are any.  Be
        // sure to reset the counts and increment them as the old entries are
        // put back in the table.

        _totalCount = 0;

        if (_count > 0) {
            _count = 0;

            for (int i = 0, length = oldKeys.length; i < length; i++) {
                int key = oldKeys[i];

                if (key != UninitializedKey && key != DeletedKeyMarker) {
                    int index = tableIndexFor(key);

                    _keys[index] = key;
                    _elements[index] = oldValues[i];

                    _count++;
                    _totalCount++;
                }
            }
        }
    }

    /**
        Removes all keys and elements from the Hashtable.
        @aribaapi private
    */
    public void clear ()
    {
        if (_keys == null) {
            return;
        }

        // Arbitraily choose 128 as 'break-even' point where
        // its costs about the same to loop here as it does to
        // generate the garbage
        if (_keys.length > 128) {
            _keys = new int[_keys.length];
            _elements = new Object[_elements.length];
        }
        else {
            for (int i = 0, length = _keys.length; i < length; i++) {
                _keys[i] = UninitializedKey;
                _elements[i] = null;
            }
        }

        _count = 0;
        _totalCount = 0;
    }

    /**
        Returns the Hashtable's string representation.
        @return the Hashtable's string representation
        @aribaapi private
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
            //        fullString - the original full string
            // first convert to serialized string
        String fullString = toSerializedString();

            // allocate enough elements in the string array
        int numElements = fullString.length() / MaxStringLen;
        if (fullString.length() % MaxStringLen > 0) {
            ++numElements;
        }

            // allocate the array
        String[] newArray = new String[numElements];

            // break up the string into the string array
        for (int i = 0; i < numElements; ++i) {
            int endIndex = (i < numElements - 1) ?
                           MaxStringLen : fullString.length();
            newArray[i] = fullString.substring(0, endIndex);
            if (i < numElements - 1) {
                fullString = fullString.substring(MaxStringLen);
            }
        }

        return newArray;
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
        StringWriter writer = new StringWriter();
        Serializer serializer = new Serializer(writer);

        try {
            serializer.writeObject(this);
            serializer.flush();
        }
        catch (IOException e) {
            Log.util.error(2756, e);
            return null;
        }

            // remember the result
        String result = writer.toString();

            // close the serializer and the stream
        try {
            serializer.close();
            writer.close();
        }
        catch (IOException e) {
        }

        return result;
    }

    /**
        Populates the hashtable with serialized data from the string.

        @param serialized String containing serialized Hashtable data
        @aribaapi private
    */
    public void fromSerializedString (String serialized)
    {
        StringReader reader = new StringReader(serialized);

        try {
            new Deserializer(reader).readObject(this);
        }
        catch (IOException e) {
            Log.util.error(2757, e);
        }
        catch (DeserializationException e) {
            Log.util.error(2757, e);
        }
        finally {
            reader.close();
        }
    }

    /**
        Implementation of Externalizable interface.  Saves the contents of this
        Hashtable object.

        @serialData iterates through entries, calling writeObject() on each key
                    followed by each value

        @param out the stream to write the object to
        @exception IOException Includes any I/O exceptions that may occur
        @aribaapi private
    */
    /*
    public void writeExternal (ObjectOutput output) throws IOException
    {
        int count = count();
        output.writeInt(count);
        Iterator keys = keys();
        while (keys.hasNext()) {
            --count;
            int key = keys.next();
            if (key == null) {
                Assert.that(false, "Null key in hash table.");
            }
            output.writeObject(key);
            output.writeObject(get(key));
        }
        Assert.that(count == 0, "count is not equal to number of keys");
    }
    */

    /**
        Implementation of Externalizable interface.  Reads the contents into
        this Hashtable.

        @param in the stream to read data from in order to restore the object
        @exception IOException if I/O errors occur
        @exception ClassNotFoundException If the class for an object being
                     restored cannot be found.
        @aribaapi private
    */
    /*
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        int count = input.readInt();
        for (int i = 0; i < count; i++) {
            put(input.readObject(), input.readObject());
        }
    }
    */

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
        @aribaapi private
    */
    public Set entrySet ()
    {
        //return new HashtableEntrySet();
        return null;
    }

    /*
    private class HashtableEntrySet extends AbstractSet
    {
        /**
            Returns an iterator over the elements contained in this collection.

            @return an iterator over the elements contained in this collection.
            @aribaapi private
        /*
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
        /*
        public int size ()
        {
            return Hashtable.this.size();
        }
    }
    */

    /*
    private class HashtableEntry implements Map.Entry
    {
        private int _index = -1;
        private Object _key;
        private Object _value;

        /**
            Creates new HashtableEntry.

            @return new HashtableEntry
            @aribaapi private
        /*
        public HashtableEntry ()
        {
        }

        /**
            Sets the index for this HashtableEntry.

            @param i index
            @aribaapi private
        /*
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
        /*
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
        /*
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
        /*
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
        /*
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
        /*
        public int hashCode ()
        {
            return _index;
        }
    }
    */

    /*
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
        /*
        public boolean hasNext ()
        {
            return (returnedCount < Hashtable.this.count);
        }

        /**
            Returns the next element in the interation.

            @return the next element in the iteration.
            @exception NoSuchElementException iteration has no more elements.
            @aribaapi private
        /*
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
        /*
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
    */

    //private transient Set keySet = null;
    //private transient Collection values = null;

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
        @aribaapi private
    */
    /*
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
    */

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
        @aribaapi private
    */
    /*
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
    */

    /**
        Creates new Hashtable with the same contents as the given Map.

        @param source source Map
        @return new Hashtable with same contents as <code>source</code>
        @aribaapi private
    */
    /*
    public Hashtable (Map source)
    {
        this(source.size());
        putAll(source);
    }
    */
}
