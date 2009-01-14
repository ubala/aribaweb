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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWArrayManager.java#14 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.FastStringBuffer;

import java.lang.reflect.Array;
import java.io.PrintStream;

/**
    This class manages a one-dimensional java array and allows for easily adding to the
    array by managing resizing and by keeping track of how many elements are in the
    array.  Any type of array can be managed, not just Object[] types.
*/
public final class AWArrayManager extends AWBaseObject
{
    private static final int InitialCapacity = 4;
    private Object _array;
    private int _size;

    public AWArrayManager (Object array, int currentSize)
    {
        _array = array;
        _size = currentSize;
    }

    public AWArrayManager (Class componentType, int capacity)
    {
        this(AWArrayManager.arrayNewInstance(componentType, capacity), 0);
    }

    public AWArrayManager (Class componentType)
    {
        this(componentType, InitialCapacity);
    }

    private boolean isIntArray ()
    {
        return _array.getClass().getComponentType() == Integer.TYPE;
    }

    public Object array ()
    {
        return _array;
    }

    public int[] intArray ()
    {
        return (int[])_array;
    }

    public Class componentType ()
    {
        return _array.getClass().getComponentType();
    }

    public int capacity ()
    {
        return Array.getLength(_array);
    }

    public int size ()
    {
        return _size;
    }

    public void clear ()
    {
        // There's no need to actually clear this array if the size is 0 as it
        // will already be clear.
        if (_size > 0) {
            _size = 0;
            // Create a new array here to avoid need to iterate through and reset each
            // slot.  I'm assuming that the memory management scheme is more efficient
            // that I could be at clearing a block of memory.
            _array = AWArrayManager.arrayNewInstance(componentType(), capacity());
        }
    }

    /**
     * Use this instead of clear() when it doesn't matter if the contents are cleared out.
     */
    public void reset ()
    {
        _size = 0;
    }

    //////////
    // Adding
    //////////
    private int addElementSetup ()
    {
        int index = _size;
        _size++;
        grow(_size);
        return index;
    }

    private void grow (int minCapacity)
    {
        int currentCapacity = capacity();
        if (minCapacity >= currentCapacity) {
            int newCapacity = currentCapacity * 2;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            Object newArray = AWArrayManager.arrayNewInstance(componentType(), newCapacity);
            System.arraycopy(_array, 0, newArray, 0, currentCapacity);
            _array = newArray;
        }
    }

    public void addElement (Object value)
    {
        // Note:  do NOT be tempted to collapse this into one line as the side effect
        // within addElementSetup() changes _array, and unfortunately _array is already
        // on the stack in the jvm by that time.  The same goes for all versions of this
        // addElement() method.
        // BROKEN: ((Object[])_array)[addElementSetup()] = value;
        int index = addElementSetup();
        ((Object[])_array)[index] = value;
    }

    public void addElement (boolean value)
    {
        int index = addElementSetup();
        ((boolean[])_array)[index] = value;
    }

    public void addElement (byte value)
    {
        int index = addElementSetup();
        ((byte[])_array)[index] = value;
    }

    public void addElement (char value)
    {
        int index = addElementSetup();
        ((char[])_array)[index] = value;
    }

    public void addElement (double value)
    {
        int index = addElementSetup();
        ((double[])_array)[index] = value;
    }

    public void addElement (float value)
    {
        int index = addElementSetup();
        ((float[])_array)[index] = value;
    }

    public void addElement (int value)
    {
        int index = addElementSetup();
        ((int[])_array)[index] = value;
    }

    public void addElement (long value)
    {
        int index = addElementSetup();
        ((long[])_array)[index] = value;
    }

    public void addElement (short value)
    {
        int index = addElementSetup();
        ((short[])_array)[index] = value;
    }

    public void insertElementAt (int index, Object object)
    {
        if (index >= _size) {
            _size = index + 1;
            grow(_size);
        }
        ((Object[])_array)[index] = object;
    }

    /**
     * Replaces the existing value at index with intValue.  Does NOT attempt to scoot
     * things down.  This may need to change in the future as this naming is a bit
     * misleading.
     * @param intValue
     * @param index
     */
    public void insertElementAt (int index, int intValue)
    {
        if (index >= _size) {
            _size = index + 1;
            grow(_size);
        }
        ((int[])_array)[index] = intValue;
    }

    // Need full compliment of insertElementAt(<type> ele, int index) methods

    //////////////
    // Getters
    //////////////

    public Object objectAt (int index)
    {
        return index < _size ? objectArray()[index] : null;
    }

    public int intAt (int index)
    {
        // A bit suspicous since there's now way to tell if out of bounds.
        return index < _size ? ((int[])_array)[index] : 0;
    }

    ////////////
    // General
    ////////////
    public boolean isEmpty ()
    {
        return size() == 0;
    }

    /**
        Resets the size without decreasing the capacity
        If the componentType is Object or subclass, then
        sets all slots to null, otherwise leaves slots
        with existing values.
    */
    public void removeAllElements ()
    {
        if (componentType() instanceof Object) {
            Object[] objectArray = (Object[])_array;
            int index = size();
            while (index > 0) {
                index--;
                objectArray[index] = null;
            }
        }
        _size = 0;
    }

    //////////////////
    // Object only
    //////////////////
    public Object[] objectArray ()
    {
        return (Object[])_array;
    }

    public Object lastElement ()
    {
        return _size == 0 ? null : objectArray()[_size - 1];
    }

    public Object removeLastElement ()
    {
        Object removedObject = null;
        if (_size > 0) {
            _size--;
            Object[] objectArray = objectArray();
            removedObject = objectArray[_size];
            objectArray[_size] = null;
        }
        else {
            throw new IndexOutOfBoundsException("Attempt to remove too many objects.");
        }
        return removedObject;
    }

    public int lastInt ()
    {
        int[] intArray = (int[])_array;
        return intArray[_size - 1];
    }

    public byte lastByte ()
    {
        byte[] byteArray = (byte[])_array;
        return byteArray[_size - 1];
    }

    public int removeLastInt ()
    {
        int removedInt = 0;
        if (_size > 0) {
            _size--;
            int[] intArray = (int[])_array;
            removedInt = intArray[_size];
            intArray[_size] = 0;
        }
        else {
            throw new IndexOutOfBoundsException("Attempt to remove too many ints.");
        }
        return removedInt;
    }

    /**
     * Note: does NOT clear the value of the last element -- simply reduces the size.
     */
    public void removeLast ()
    {
        if (_size > 0) {
            _size--;
        }
        else {
            throw new IndexOutOfBoundsException("Attempt to remove too many times.");
        }
    }

    //////////////////
    // Conver Methods
    //////////////////
    private static Object arrayNewInstance (Class componentType, int capacity)
    {
        try {
            return Array.newInstance(componentType, capacity);
        }
        catch (NegativeArraySizeException negativeArraySizeException) {
            throw new AWGenericException(negativeArraySizeException);
        }
    }

    public void printf (PrintStream printStream)
    {
        if (isIntArray()) {
            printStream.println("AWAraryManager int[]: {");
            int[] intArray = intArray();
            boolean useReturns = size() > 8;
            for (int index = 0, length = size(); index < length; index++) {
                int intValue = intArray[index];
                if (useReturns) {
                    printStream.print("\n    ");
                }
                printStream.print(intValue);
                printStream.print(", ");
            }
        }
        else {
            printStream.println("AWAraryManager Object[]: {");
            Object[] objectArray = objectArray();
            for (int index = 0, length = size(); index < length; index++) {
                Object object = objectArray[index];
                printStream.print("    ");
                printStream.print(object);
                printStream.print("\n");
            }
        }
        printStream.println("}");
    }

    public String toString ()
    {
        FastStringBuffer buffer = new FastStringBuffer(8 * size());
        if (isIntArray()) {
            buffer.append("AWAraryManager int[]: {");
            int[] intArray = intArray();
            boolean useReturns = size() > 8;
            for (int index = 0, length = size(); index < length; index++) {
                int intValue = intArray[index];
                if (useReturns) {
                    buffer.append("\n    ");
                }
                buffer.append(AWUtil.toString(intValue));
                buffer.append(", ");
            }
        }
        else {
            buffer.append("AWAraryManager Object[]: {");
            Object[] objectArray = objectArray();
            for (int index = 0, length = size(); index < length; index++) {
                Object object = objectArray[index];
                buffer.append("    ");
                buffer.append(object);
                buffer.append("\n");
            }
        }
        buffer.append("}");
        return buffer.toString();
    }

    public Object trimmedArrayCopy ()
    {
        int size = size();
        Object newArray = AWArrayManager.arrayNewInstance(componentType(), size);
        System.arraycopy(_array, 0, newArray, 0, size);
        return newArray;
    }
}
