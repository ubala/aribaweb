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

import java.io.PrintStream;

/**
    A MultiKeyHashtable class that allows for multiple keys per entry.
    This is useful when several keys are required to uniquely identify
    an object. For example, to uniquely identify a Method, we must
    have the Class and the name of the Method.

    Note that <b>this class is not thread-safe</b>.

    This does NOT currently support removing values.

    @aribaapi documented
*/
public class MultiKeyHashtable extends Object implements Cloneable
{
    protected static final Object RemovedObject = new Object();
    protected static final Object[] RemovedObjects = {
        RemovedObject, RemovedObject, RemovedObject, RemovedObject
    };
    private static final int InitialSize = 8;
    private final boolean _useIdentityComparison;
    private final int _keyCount;
    private Object[] _sharedKeyList;
    private int _size;
    private int _keyArrayLength;
    private int _resizeLevel;
    private Object[] _keys;
    private Object[] _values;

    /**
        Constructs a new, empty MultiKeyHashtable which
        uses the specified number of keys.
        
        @param keyCount the number of keys which the table will use.
        All gets and puts must use this number of keys to access the table.
    */
    public MultiKeyHashtable (int keyCount)
    {
        super();
        _keyCount = keyCount;
        checkForLegalKeyCount();
        _sharedKeyList = initSharedKeyList();
        _size = 0;
        _keyArrayLength = InitialSize * _keyCount;
        _keys = new Object[_keyArrayLength];
        _values = new Object[InitialSize];
        _resizeLevel = computeResizeLevel();
        _useIdentityComparison = false;
    }
    
    /**
        Constructs a new, empty MultiKeyHashtable which uses the specified
        number of keys, initial size, and whether or not comparisons use
        the 'equals' method or simply '=='.
        
        @param keyCount the number of keys which the table will use.  All gets
        and puts must use this number of keys to access the table.
        @param initialSize the number of slots created when the hashtable is first created.
        @param useIdentityComparison a flag to inidicate if the internal
        comparisons should use the 'equals' method or simply use '=='.
    */
    public MultiKeyHashtable (int keyCount, int initialSize,
                              boolean useIdentityComparison)
    {
        super();
        _keyCount = keyCount;
        checkForLegalKeyCount();
        _sharedKeyList = initSharedKeyList();
        _size = 0;
        _keyArrayLength = initialSize * _keyCount;
        _keys = new Object[_keyArrayLength];
        _values = new Object[initialSize];
        _resizeLevel = computeResizeLevel();
        _useIdentityComparison = useIdentityComparison;
    }
    
    /**
        Checks to ensure that the number of keys specified in a constructor is valid.
        Throws IllegalArgumentException if number of keys is not supported.  Currently,
        anything less than 2 or greater than 4 is invalid.
    */
    private void checkForLegalKeyCount ()
    {
        if (_keyCount < 2 || _keyCount > 4) {
            String message = Fmt.S("Invalid keyCount (%s) must be >= 2 and <= 4.",
                                   Constants.getInteger(_keyCount));
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
        Provides a consistent way to create a keyList array for a single entry.
        
        @return Returns an empty Object[] for use as a list of keys that
        can be passed around internally and used to lookup existing entries.
    */
    private Object[] initSharedKeyList ()
    {
        return new Object[_keyCount];
    }

    /**
        Provides a consistent way to compute the next point at which
        the table will need to grow.
        
        @return Returns the size of the table beyond which the
        table will need to grow.
    */
    protected int computeResizeLevel ()
    {
        return (_values.length * 5) / 8;
    }
    
    /**
        Adds all the entries from the keyIterator and valueIterator.
        These two iterators must be from the same MultiKeyHashtable.
        
        @param keyIterator enumerates over the keys in a source MultiKeyHashtable.
        @param valueIterator enumerates over the value in a source MultiKeyHashtable.
    */
    protected void add (MultiKeyHashtableKeyIterator keyIterator,
                        MultiKeyHashtableValueIterator valueIterator)
    {
        Object[] currentKeyList = null;
        while ((currentKeyList = keyIterator.next()) != null) {
            Object currentValue = valueIterator.next();
            this.put(currentKeyList, currentValue, false);
        }
    }

    /**
        Doubles the size of the existing internal structures.
    */
    protected void grow ()
    {
        MultiKeyHashtableKeyIterator keyIterator = keys();
        MultiKeyHashtableValueIterator valueIterator = values();
        _keyArrayLength *= 2;
        _keys = new Object[_keyArrayLength];
        _values = new Object[_values.length * 2];
        _size = 0;
        _resizeLevel = computeResizeLevel();
        add(keyIterator, valueIterator);
    }
    
    /**
        Compares each entry in targetKeyList against the keys stored
        internally starting at keysBaseIndex.
        
        @param targetKeyList list of keys.
        @param keysBaseIndex base index of targetKeyList to start comparison.
        @return Returns true if all keys match (either using equals()
        or '==' as determined by the useIdentityComparison flag in the
        constructor).
    */
    protected boolean keyListMatchesAtIndex (Object[] targetKeyList, int keysBaseIndex)
    {
        // make sure each key in both lists match.
        boolean doesMatch = true;
        for (int index = _keyCount - 1; index > -1; index--) {
            if (targetKeyList[index] != _keys[keysBaseIndex + index]) {
                doesMatch = false;
                break;
            }
        }
        if (!doesMatch && !_useIdentityComparison) {
            doesMatch = true;
            for (int index = _keyCount - 1; index > -1; index--) {
                if (!objectsAreEqualEnough(targetKeyList[index], 
                                           _keys[keysBaseIndex + index], index)) {
                    doesMatch = false;
                    break;
                }
            }            
        }
        return doesMatch;
    }

    /**
        Compares each entry in targetKeyList against the keys stored
        internally starting at keysBaseIndex.

        @param obj1 key from set 1.
        @param obj2 key from set 2.
        @param index position of the keys within their respective multi-key set.
        @return Returns true if all keys match.
    */
    protected boolean objectsAreEqualEnough (Object obj1, Object obj2, int index)
    {
        return obj1.equals(obj2);
    }

    /**
        Computes the index where the targetKeyList would like to
        appear within the table.  If something is already stored there,
        then the next slot must be tried, but this method is only
        concerned with the starting point.
        
        @param targetKeyList a list of keys to hash and mod in order to
        compute the initial index.
        @return Returns the initial index where the targetKeyList would
        like to appear, if it can.
    */
    private int computeInitialIndex (Object[] targetKeyList)
    {
        int hashCode = getHashValueForObject(targetKeyList[0], 0);
        for (int index = _keyCount - 1; index >= 1; index--) {
            hashCode <<= 1;
            hashCode ^= getHashValueForObject(targetKeyList[index], index);
        }
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        return (hashCode % _values.length) * _keyCount;
    }

    /**
        Computes the index where the targetKeyList would like to
        appear within the table.  If something is already stored there,
        then the next slot must be tried, but this method is only
        concerned with the starting point.

        @param obj a key component to hash.
        @param index the position in the key.
        @return Returns hash value for this key component.
    */
    protected int getHashValueForObject (Object obj, int index)
    {
        return obj.hashCode();
    }


    /**
        Locates an existing set of keys and returns the index if found.  This is
        used for 'get' only; to compute the insertion index, see insertionIndexForKeyList().
        
        @param targetKeyList a list of keys to be located.
        @return Returns the index where the targetKeyList is found, or -1 if not found.
    */
    protected int indexForKeyList (Object[] targetKeyList)
    {
        int initialIndex = computeInitialIndex(targetKeyList);
        int index = initialIndex;
        while (_keys[index] != null) {
            if (_keys[index] != RemovedObject &&
                keyListMatchesAtIndex(targetKeyList, index)) {
                return index;
            }
            index += _keyCount;
            if (index >= _keyArrayLength) {
                index = 0;
            }
            if (index == initialIndex) {
                break;
            }
        }
        return -1;
    }

    /**
        Adds an object to the table keyed by the set of keys targetKeyList.
        If the targetKeyList is already in the table, the existing value will
        only be replaced if 'onlyPutIfAbsent' is false.  In any case, the
        existing value will always be returned.
        
        @param targetKeyList a list of keys to be located.
        @param onlyPutIfAbsent if true, only put if there is no existing value
                               for the list of keys.
        @return Returns the previously existing value or null if no value
        exists.  The return value is independent upon the onlyPutIfAbsent
        flag and always returns the value located at the target insertion slot.
    */
    protected Object put (Object[] targetKeyList, Object value,
                          boolean onlyPutIfAbsent)
    {
        if (_size >= _resizeLevel) {
            grow();
        }
        
        Object existingValue = null;
        int keyInsertionIndex = -1;
        int keyExistingIndex = -1;
        
        int keyInitialIndex = computeInitialIndex(targetKeyList);
        int keyCurrentIndex = keyInitialIndex;
        Object currentKey = null;
        while ((currentKey = _keys[keyCurrentIndex]) != null) {
            if (currentKey == RemovedObject) {
                if (keyInsertionIndex < 0) {
                    keyInsertionIndex = keyCurrentIndex;
                }    
            }
            else if (keyListMatchesAtIndex(targetKeyList, keyCurrentIndex)) {
                keyExistingIndex = keyCurrentIndex;
                break;
            }
            keyCurrentIndex += _keyCount;
            if (keyCurrentIndex >= _keyArrayLength) {
                keyCurrentIndex = 0;
            }
            if (keyCurrentIndex == keyInitialIndex) {
                break;
            }
        }
        if (keyInsertionIndex < 0) {
            keyInsertionIndex = keyCurrentIndex;
        }
        
        if (keyExistingIndex >= 0) { 
            existingValue = _values[keyExistingIndex / _keyCount];
            if (!onlyPutIfAbsent) {
                System.arraycopy(targetKeyList, 0, _keys, keyInsertionIndex, _keyCount);
                _values[keyInsertionIndex / _keyCount] = value;
                if (keyInsertionIndex != keyExistingIndex) {
                    System.arraycopy(RemovedObjects, 0, _keys, keyExistingIndex,
                                     _keyCount);
                    _values[keyExistingIndex / _keyCount] = RemovedObject;
                }
            }
        }
        else {
            System.arraycopy(targetKeyList, 0, _keys, keyInsertionIndex, _keyCount);
            _values[keyInsertionIndex / _keyCount] = value;
            _size++;
        }
        return existingValue;
    }

    /**
        Locates and returns existing values corresponding to keyList.
        
        @param targetKeyList a list of keys to be located.
        @return Returns the existing value or null if no value exists.
    */
    protected Object get (Object[] targetKeyList)
    {
        int indexForKeyList = indexForKeyList(targetKeyList);
        return (indexForKeyList > -1) ? _values[indexForKeyList / _keyCount] : null;
    }

    /**
     * Removes from the table the object value for a list of target keys.
     * @param targetKeyList the list of target keys to remove.
     * @return the existing value for the target keys or null if no existing value.
     * @aribaapi private
     */
    public Object remove (Object[] targetKeyList)
    {
        checkAttemptedArgCount(targetKeyList.length);
        int indexForKeyList = indexForKeyList(targetKeyList);
        if (indexForKeyList <= -1) {
            // no match - do nothing
            return null;
        }
        Object existingValue = _values[indexForKeyList / _keyCount];
        System.arraycopy(RemovedObjects, 0, _keys, indexForKeyList, _keyCount);
        _values[indexForKeyList / _keyCount] = RemovedObject;
        _size--;
        return existingValue;
    }

    /**
        Checks a candidate value to ensure its not null.
        
        @param value a candidate value which may be inserted in the table.
        @return Returns the value or throws a runtime exception if not valid.
    */
    private final Object checkForLegalValue (Object value)
    {
        if (value == null) {
            throw new IllegalArgumentException("invalid value -- cannot be null");
        }
        return value;
    }

    /**
        Checks a candidate key to ensure its not null.
        
        @param key a candidate key which may be inserted in the table.
        @return Returns the key or throws a runtime exception if not valid.
    */
    private final Object checkForLegalKey (Object key)
    {
        if (key == null) {
            throw new IllegalArgumentException("invalid key -- cannot be null");
        }
        return key;
    }

    /**
        Checks to ensure that the number of keys for a put is the valid number.
        Since users may use any number of 'put' cover methods, we must ensure
        that they are using the proper number of keys.  This will throw a
        runtime exception if the attemptedArgCount does not match the keyCount
        specified in the constructor.
        
        @param attemptedArgCount the number of keys in the attempted put operation.
    */
    private final void checkAttemptedArgCount (int attemptedArgCount)
    {
        if (_keyCount != attemptedArgCount) {
            String message = Fmt.S("invalid argument count %s -- requires %s",
                Integer.toString(attemptedArgCount), Integer.toString(_keyCount));
            Assert.that(false, message);
        }
    }

    /**
        Locates and returns the value keyed by the two keys key0, key1.
        This puts the keys into and internal, shared keyList, so its
        imperative that you lock around this and all access to this class
        if its to be used in a multi-threaded scenario.
        
        @param key0 the first key.
        @param key1 the second key.
        @return Returns the value keyed by key0, key1 or null if not found.
    */
    public Object get (Object key0, Object key1)
    {
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = key0;
        sharedKeyList[1] = key1;
        Object value = get(sharedKeyList);
        return value;
    }
    
    /**
        Locates and returns the value keyed by the three keys key0, key1, key2.
        This puts the keys into and internal, shared keyList, so its imperative
        that you lock around this and all access to this class if its to be used
        in a multi-threaded scenario.
        
        @param key0 the first key.
        @param key1 the second key.
        @param key2 the second key.
        @return Returns the value keyed by key0, key1, key2 or null if not found.
    */
    public Object get (Object key0, Object key1, Object key2)
    {
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = key0;
        sharedKeyList[1] = key1;
        sharedKeyList[2] = key2;
        Object value = get(sharedKeyList);
        return value;
    }

    /**
        Locates and returns the value keyed by the fours keys key0, key1, key2, key3.
        This puts the keys into and internal, shared keyList, so its imperative that
        you lock around this and all access to this class if its to be used in a
        multi-threaded scenario.
        
        @param key0 the first key.
        @param key1 the second key.
        @param key2 the second key.
        @param key3 the second key.
        @return Returns the value keyed by key0, key1, key2, key3 or null if not found.
    */
    public Object get (Object key0, Object key1, Object key2, Object key3)
    {
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = key0;
        sharedKeyList[1] = key1;
        sharedKeyList[2] = key2;
        sharedKeyList[3] = key3;
        Object value = get(sharedKeyList);
        return value;
    }
    
    /**
        Inserts the value keyed by the two keys key0, key1.  This puts the keys
        into and internal, shared keyList, so its imperative that you lock around
        this and all access to this class if its to be used in a multi-threaded scenario.
        
        @param key0 the first key.
        @param key1 the second key.
        @return Returns the value keyed by key0, key1 or null if not found.
    */
    public Object put (Object key0, Object key1, Object value)
    {
        checkAttemptedArgCount(2);
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = checkForLegalKey(key0);
        sharedKeyList[1] = checkForLegalKey(key1);
        Object oldValue = put(sharedKeyList, checkForLegalValue(value), false);
        return oldValue;
    }

    /**
        Inserts the value keyed by the three keys key0, key1, key2.  This puts the
        keys into and internal, shared keyList, so its imperative that you lock
        around this and all access to this class if its to be used in a
        multi-threaded scenario.
        
        @param key0 the first key.
        @param key1 the second key.
        @param key2 the third key.
        @return Returns the value keyed by key0, key1, key2 or null if not found.
    */
    public Object put (Object key0, Object key1, Object key2, Object value)
    {
        checkAttemptedArgCount(3);
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = checkForLegalKey(key0);
        sharedKeyList[1] = checkForLegalKey(key1);
        sharedKeyList[2] = checkForLegalKey(key2);
        Object oldValue = put(sharedKeyList, checkForLegalValue(value), false);
        return oldValue;
    }    

    /**
        Inserts the value keyed by the four keys key0, key1, key2, key3.
        This puts the keys into and internal, shared keyList, so its imperative
        that you lock around this and all access to this class if its to be used
        in a multi-threaded scenario.
        
        @param key0 the first key.
        @param key1 the second key.
        @param key2 the third key.
        @param key3 the fourth key.
        @return Returns the value keyed by key0, key1, key2, key3 or null if not found.
    */
    public Object put (Object key0, Object key1, Object key2, Object key3, Object value)
    {
        checkAttemptedArgCount(4);
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = checkForLegalKey(key0);
        sharedKeyList[1] = checkForLegalKey(key1);
        sharedKeyList[2] = checkForLegalKey(key2);
        sharedKeyList[3] = checkForLegalKey(key3);
        Object oldValue = put(sharedKeyList, checkForLegalValue(value), false);
        return oldValue;
    }    

    /**
        See put(..).  This is the same as the corresponding put() method, except it
        will not replace the existing value if one already exists.  In either case,
        the return value is the existing value.
        
        @param key0 the first key.
        @param key1 the second key.
        @return Returns the value keyed by key0, key1 or null if not found.
    */
    public Object putIfAbsent (Object key0, Object key1, Object value)
    {
        checkAttemptedArgCount(2);
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = checkForLegalKey(key0);
        sharedKeyList[1] = checkForLegalKey(key1);
        Object oldValue = put(sharedKeyList, checkForLegalValue(value), true);
        return oldValue;
    }

    /**
        See put(..).  This is the same as the corresponding put() method, except it
        will not replace the existing value if one already exists.  In either case,
        the return value is the existing value.
        
        @param key0 the first key.
        @param key1 the second key.
        @param key2 the third key.
        @return Returns the value keyed by key0, key1, key2 or null if not found.
    */
    public Object putIfAbsent (Object key0, Object key1, Object key2, Object value)
    {
        checkAttemptedArgCount(3);
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = checkForLegalKey(key0);
        sharedKeyList[1] = checkForLegalKey(key1);
        sharedKeyList[2] = checkForLegalKey(key2);
        Object oldValue = put(sharedKeyList, checkForLegalValue(value), true);
        return oldValue;
    }

    /**
        See put(..).  This is the same as the corresponding put() method, except it
        will not replace the existing value if one already exists.  In either case,
        the return value is the existing value.
        
        @param key0 the first key.
        @param key1 the second key.
        @param key2 the third key.
        @param key3 the fourth key.
        @return Returns the value keyed by key0, key1, key2, key3 or null if not found.
    */
    public Object putIfAbsent (Object key0, Object key1,
                               Object key2, Object key3, Object value)
    {
        checkAttemptedArgCount(4);
        Object[] sharedKeyList = _sharedKeyList;
        sharedKeyList[0] = checkForLegalKey(key0);
        sharedKeyList[1] = checkForLegalKey(key1);
        sharedKeyList[2] = checkForLegalKey(key2);
        sharedKeyList[3] = checkForLegalKey(key3);
        Object oldValue = put(sharedKeyList, checkForLegalValue(value), true);
        return oldValue;
    }

    /**
        The current size (or number of entries) in the table.  This varies as
        items are added.  Note: remove is currently not supported.
        
        @return Returns the current number of entries in the table.
    */
    public int size ()
    {
        return _size;
    }
    
    /**
        The number of keys expected by the table.  Once this number is established
        in the costructor, it will never change.
        
        @return Returns the number of keys expected by the table.
    */
    public int keyCount ()
    {
        return _keyCount;
    }

    /**
        Creates a new MultiKeyHashtableKeyIterator for use in enumerating
        over the keys of the table.  This does not duplicate the keys array,
        so care must be taken not to alter the hashtable while iterating over it.

        @return Returns a new MultiKeyHashtableKeyIterator.
    */
    protected MultiKeyHashtableKeyIterator keys ()
    {
        return new MultiKeyHashtableKeyIterator(_keys, _keyCount);
    }
    
    /**
        Creates a new MultiKeyHashtableValueIterator for use in enumerating
        over the values of the table.  This does not duplicate the values array,
        so care must be taken not to alter the hashtable while iterating over it.
        
        @return Returns a new MultiKeyHashtableValueIterator.
    */
    protected MultiKeyHashtableValueIterator values ()
    {
        return new MultiKeyHashtableValueIterator(_values);
    }

    /**
        Adds all the items from otherHashtable to the reciever.  The keyCounts
        for both table must be the same, else a runtime exception will be thrown.
        
        @param otherHashtable is another MultiKeyHashtable with the same keyCount.
    */
    public void add (MultiKeyHashtable otherHashtable)
    {
        Assert.that(otherHashtable.keyCount() == _keyCount,
                    "Cannot add from MultiKeyHashtable - mismatched keyCount's");
        MultiKeyHashtableKeyIterator keyIterator = otherHashtable.keys();
        MultiKeyHashtableValueIterator valueIterator = otherHashtable.values();
        add(keyIterator, valueIterator);            
    }

    /**
        Creates a copy of the receiver including a copy of the internal keys and values arrays.
        
        @return a new copy of the receiver.  The internal keys and values arrays
        are also cloned.
    */
    public Object clone ()
    {
        try {
            MultiKeyHashtable newHashtable = (MultiKeyHashtable)super.clone();
            newHashtable._sharedKeyList = initSharedKeyList();
            newHashtable._keys = (Object[])_keys.clone();
            newHashtable._values = (Object[])_values.clone();
            return newHashtable;
        }
        catch (CloneNotSupportedException cloneNotSupportedException) {
            throw new InternalError("Error in clone(). This shouldn't happen.");
        }
    }

    /**
        Prints the MultiKeyHashtable to System.out (for debugging purposes only).
    */
    protected void printf ()
    {
        PrintStream out = System.out;
        out.println("{");
        MultiKeyHashtableKeyIterator keyEnum = keys();
        MultiKeyHashtableValueIterator valueEnum = values();
        Object[] keys = null;
        while ((keys = keyEnum.next()) != null) {
            out.print("    ");
            for (int index = _keyCount - 1; index >= 0; index--) {
                out.print(keys[index].toString());
                out.print(", ");
            }
            out.print("= ");
            out.print(valueEnum.next());
            out.print(";\n");
        }
        out.println("}");
    }
}

/**
MultiKeyHashtableKeyIterator allows for enumation over the keys of a
MultiKeyHashtable.  This does not follow the java.util.Iterator
interface since that is less efficient than the approach of simply
returning null if there are no more elements.
*/
class MultiKeyHashtableKeyIterator extends Object
{
    private final Object[] _sharedKeyList;
    private final int _keyCount;
    private final Object[] _keys;
    private int _currentIndex;

    /**
    Constructs a new MultiKeyHashtableKeyIterator from the internal
    keysArray which is based on keyCount keys.

    @param keysArray the internal array of keys from the MultiKeyHashtable
    @param keyCount the count of keys for the target MultiKeyHashtable
    */
    protected MultiKeyHashtableKeyIterator (Object[] keysArray, int keyCount)
    {
        super();
        _keyCount = keyCount;
        _sharedKeyList = new Object[_keyCount];
        _keys = keysArray;
        _currentIndex = 0;
    }

    /**
    Return the next set of keys in from the keysArray.  This populates a
    shared keyList (Object[]) and returns that.  Hence, you must not allow
    this code to be shared by multiple threads while you are iterating.

    @return an array containing the next available set of keys or null if
    all keys have been seen
    */
    protected Object[] next ()
    {
        Object[] nextKeyList = null;
        int keysLength = _keys.length;
        if (_currentIndex < keysLength) {
            Object currentKey = null;
            while (((currentKey = _keys[_currentIndex]) == null)
                   || (currentKey == MultiKeyHashtable.RemovedObject)) {
                _currentIndex += _keyCount;
                if (_currentIndex >= keysLength) {
                    return null;
                }
            }
            System.arraycopy(_keys, _currentIndex, _sharedKeyList, 0, _keyCount);
            _currentIndex += _keyCount;
            nextKeyList = _sharedKeyList;            
        }
        return nextKeyList;
    }
}

/**
MultiKeyHashtableValueIterator allows for enumation over the value of
a MultiKeyHashtable.  This does not follow the java.util.Iterator
interface since that is less efficient than the approach of simply
returning null if there are no more elements.
*/
class MultiKeyHashtableValueIterator extends Object
{
    private final Object[] _values;
    private int _currentIndex;

    /**
    Constructs a new MultiKeyHashtableValueIterator from the
    internal keysArray which is based on keyCount keys.

    @param valuesArray the internal values array from a MultiKeyHashtable
    */
    protected MultiKeyHashtableValueIterator (Object[] valuesArray)
    {
        super();
        _values = valuesArray;
        _currentIndex = 0;
    }

    /**
    Return the next value from the valuesArray.

    @return the next value from the valuesArray or null if all have been seen
    */
    protected Object next ()
    {
        Object currentValue = null;
        int valuesLength = _values.length;
        while (((currentValue = _values[_currentIndex++]) == null) 
               || (currentValue == MultiKeyHashtable.RemovedObject)) {
            if (_currentIndex >= valuesLength) {
                return null;
            }
        }
        return currentValue;
    }
}
