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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWSizeLimitedHashtable.java#6 $
*/

package ariba.ui.aribaweb.util;

/*

 In this class, a timestamp really is the current value of _totalHits.  The totalHits counter keeps trak of the number of times either a get or put is performed.

 NOTE: by using a "long" for the timestamp, if we do 100,000,000 hits per second, we can run the app for 2996.6 years before the counter will turn over.  Hence, I never check for overflow.


 This hash table uses an "open addressing" approach which means all keys are in the same array and collisions are resolved by using the next available slot.  Removed objects are marked as RemovedObject so as not to break the implicit lists of collisions.  Occasionally, when the ration of RemovedObject to nulls gets too high (> 3/2), we rehash the values in the table to remove all RemovedObjects.

 An attempt is made to keep keys of the same hashIndex together as muchas possible.  To this end, the insertion algorithm slides down entries which are of greater hashIndex than the key to be inserted.  This takes a bit longer at insertion time, but results better performance for the common case, ie lookup.

 Also, in order to improve performance, this table retains the hashIndexes for each entry and uses these values to locate the best place to insert new values as well as for eliminating unnecessary comparisons (via equals()).  Again, some cost is incurred in maintaining this value, but the common case of lookup benefits a great deal from having the value cahed.

 Currently, this table must keep 4 arrays in sync.  It would be better, if Java supported structs, to use an array of structs.  I may consider creating an object that I use as a struct and pre-populate an array with these struct-like objects so we simplify some of the code herein.  For a table with 1000 entries, this would require about 2000 instances of these objects, so it may not be worth the overhead.

*/

public final class AWSizeLimitedHashtable extends AWBaseObject implements Cloneable
{
    private static final Object RemovedObject = new Object();
    private Object[] _keys;
    private Object[] _values;
    private short[] _hashIndexes;
    // see note above for overflow considerations
    private long _totalHits;
    private long[] _timestamps;
    private long _minimumTimestamp;
    private int _arrayLength;
    private int _hashSpace;
    private int _size;
    private int _desiredSize;
    private int _maxSize;

    public AWSizeLimitedHashtable (int desiredSize)
    {
        super();
        _size = 0;
        _desiredSize = desiredSize;
        if (_desiredSize > 16) {
            // allows for 25% oversize before purge.
            _maxSize = (_desiredSize * 5) / 4;
            // allows for a load factor of 5/10 at max size.
            _arrayLength = (_maxSize * 10) / 5;
            _hashSpace = (_arrayLength * 98) / 100;
        }
        else {
            // small tables need to be handled differently to avoid round-off errors.
            _maxSize = _desiredSize * 2;
            _arrayLength = _maxSize * 3;
            _hashSpace = _arrayLength - 2;
        }
        if (_arrayLength >= Short.MAX_VALUE) {
            throw new AWGenericException(getClass().getName() + ": requestedSize too large: " + _desiredSize);
        }
        _hashIndexes = new short[_arrayLength];
        _keys = new Object[_arrayLength];
        _values = new Object[_arrayLength];
        _timestamps = new long[_arrayLength];
        _totalHits = 0;
        _minimumTimestamp = 1;
    }

    protected void grow ()
    {
        // this is only used in the event we use up
        // all the buffer space at the end.
        int newAarrayLength = (_arrayLength * 102) / 100;
        _arrayLength = (newAarrayLength == _arrayLength) ? _arrayLength + 5 : newAarrayLength;
        if (_arrayLength >= Short.MAX_VALUE) {
            _arrayLength = Short.MAX_VALUE;
        }
        _hashIndexes = AWUtil.realloc(_hashIndexes, _arrayLength);
        _keys = (Object[])AWUtil.realloc(_keys, _arrayLength);
        _values = (Object[])AWUtil.realloc(_values, _arrayLength);
        _timestamps = AWUtil.realloc(_timestamps, _arrayLength);
    }

    protected void remove (int targetIndex)
    {
        int nextIndex = targetIndex + 1;
        if (nextIndex < _arrayLength && _keys[nextIndex] != null) {
            _keys[targetIndex] = RemovedObject;
        }
        else {
            _keys[targetIndex] = null;
            int index = targetIndex - 1;
            while ((index >= 0) && (_keys[index] == RemovedObject)) {
                _keys[index] = null;
                index--;
            }
        }
        _hashIndexes[targetIndex] = 0;
        _values[targetIndex] = null;
        _timestamps[targetIndex] = 0;
        _size--;
    }

    protected void purge ()
    {
        // This is called when the table gets too full and
        // needs to have the oldest objects removed.
        long cutoffTimeStamp = _minimumTimestamp + (_maxSize - _desiredSize);
        _minimumTimestamp = _totalHits;
        int nullCount = 0;
        int removedCount = 0;
        for (int index = _arrayLength - 1; index >= 0; index--) {
            long currentTimestamp = _timestamps[index];
            if (currentTimestamp > 0) {
                if (currentTimestamp < cutoffTimeStamp) {
                    remove(index);
                }
                else if (currentTimestamp < _minimumTimestamp) {
                    _minimumTimestamp = currentTimestamp;
                }
            }

            Object currentKey = _keys[index];
            if (currentKey == null) {
                nullCount++;
            }
            else if (currentKey == RemovedObject) {
                removedCount++;
            }
        }
        if ((removedCount * 1.5) > nullCount) {
            //System.out.println("*************** BEFORE rehash");
            //printKeysCondition();
            rehash();
            //System.out.println("*************** AFTER rehash");
            //printKeysCondition();
        }
    }

    private short computeHashIndex (Object targetKey)
    {
        int hashCode = targetKey.hashCode();
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        return (short)(hashCode % _hashSpace);
    }

    protected int indexForKey (Object targetKey)
    {
        // This is the most time critical method in this class.  All other performance considerations must take a back seat to making this fast.
        int arrayLength = _arrayLength;
        int targetHashIndex = computeHashIndex(targetKey);
        Object currentKey = null;

        int index = targetHashIndex;
        while ((currentKey = _keys[index]) != null) {
            if ((currentKey != RemovedObject) && (_hashIndexes[index] == targetHashIndex) && (currentKey == targetKey || currentKey.equals(targetKey))) {
                // see note above for overflow considerations
                _totalHits++;
                return index;
            }
            index++;
            if (index >= arrayLength) {
                break;
            }
        }
        return -1;
    }

    protected int insertionIndexForKey (Object targetKey, int targetHashIndex)
    {
        int index = targetHashIndex;
        Object currentKey = null;
        while ((currentKey = _keys[index]) != null) {
            short currentHashIndex = _hashIndexes[index];
            if (currentKey == RemovedObject || (currentHashIndex == targetHashIndex && (currentKey == targetKey || currentKey.equals(targetKey)))) {
                break;
            }
            if (currentHashIndex >= targetHashIndex) {
                int nextIndex = index + 1;
                int indexOfNull = nextOpenKeySlot(nextIndex);
                if (indexOfNull == -1) {
                    grow();
                    indexOfNull = nextOpenKeySlot(nextIndex);
                }
                int length = indexOfNull - index;

                System.arraycopy(_keys, index, _keys, nextIndex, length);
                System.arraycopy(_values, index, _values, nextIndex, length);
                System.arraycopy(_timestamps, index, _timestamps, nextIndex, length);
                System.arraycopy(_hashIndexes, index, _hashIndexes, nextIndex, length);

                clearEntry(index);

                break;
            }
            index++;
            if (index >= _arrayLength) {
                grow();
            }
        }
        return index;
    }

    protected Object insert (Object key, Object value, boolean onlyPutIfAbsent)
    {
        Object existingValue = null;
        if (key != null && value != null) {
            if (_size > _maxSize) {
                purge();
            }
            short hashIndex = computeHashIndex(key);
            int insertionIndex = insertionIndexForKey(key, hashIndex);
            existingValue = _values[insertionIndex];
            boolean shouldPut = onlyPutIfAbsent ? existingValue == null : true;
            if (shouldPut) {
                // see note above for overflow considerations
                _totalHits++;
                _keys[insertionIndex] = key;
                _values[insertionIndex] = value;
                _timestamps[insertionIndex] = _totalHits;
                _hashIndexes[insertionIndex] = hashIndex;
                if (existingValue == null) {
                    _size++;
                }
            }
        }
        return existingValue;
    }

    public Object put (Object key, Object value)
    {
        return insert(key, value, false);
    }

    public void putIfAbsent (Object key, Object value)
    {
        insert(key, value, true);
    }

    public Object get (Object key)
    {
        Object value = null;
        if (key != null) {
            int indexForKey = indexForKey(key);
            if (indexForKey > -1) {
                value = _values[indexForKey];
                _timestamps[indexForKey] = _totalHits;
            }
        }
        return value;
    }

    /**
        Allows for looking up a key to see if it exists and to get its value
    */
    public Object getKey (Object key)
    {
        Object existingKey = null;
        if (key != null) {
            int indexForKey = indexForKey(key);
            if (indexForKey > -1) {
                existingKey = _keys[indexForKey];
                _timestamps[indexForKey] = _totalHits;
            }
        }
        return existingKey;
    }

    public int size ()
    {
        return _size;
    }

    public Object clone ()
    {
        try {
            AWSizeLimitedHashtable newHashtable = (AWSizeLimitedHashtable)super.clone();
            newHashtable._keys = (Object[])_keys.clone();
            newHashtable._values = (Object[])_values.clone();
            newHashtable._timestamps = (long[])_timestamps.clone();
            newHashtable._hashIndexes = (short[])_hashIndexes.clone();
            return newHashtable;
        }
        catch (CloneNotSupportedException cloneNotSupportedException) {
            throw new AWGenericException(cloneNotSupportedException);
        }
    }

    /////////////
    // Rehash
    /////////////
    private void clearEntry (int index)
    {
        _keys[index] = null;
        _values[index] = null;
        _timestamps[index] = 0;
        _hashIndexes[index] = 0;
    }

    private void rehash ()
    {
        for (int index = 1; index < _arrayLength; index++) {
            Object currentKey = _keys[index];
            if (currentKey == RemovedObject) {
                clearEntry(index);
            }
            else if (currentKey != null) {
                for (int insertionIndex = _hashIndexes[index]; insertionIndex < index; insertionIndex++) {
                    Object insertionKey = _keys[insertionIndex];
                    if (insertionKey == RemovedObject || insertionKey == null) {
                        _keys[insertionIndex] = _keys[index];
                        _values[insertionIndex] = _values[index];
                        _timestamps[insertionIndex] = _timestamps[index];
                        _hashIndexes[insertionIndex] = _hashIndexes[index];
                        clearEntry(index);
                    }
                }
            }
        }
    }

    private int nextOpenKeySlot (int index)
    {
        while (index < _arrayLength) {
            Object currentKey = _keys[index];
            if (currentKey == null || currentKey == RemovedObject) {
                return index;
            }
            index++;
        }
        return -1;
    }

    ////////////////
    // Debug
    ////////////////
}
