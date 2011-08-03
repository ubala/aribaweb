/*
    Copyright 1996-2011 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/DynamicArray.java#12 $
*/

package ariba.util.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
    <P>A DynamicArray is like List except that is exposes the underlying
    array.  DynamicArray is abstract so that clients can supply a method to
    allocate the array to be of a particular type.  This gets back some of
    the runtime type checking that List loses, and allows more concise
    enumeration.  Here is a sample concrete subclass:</P>

    <PRE>
        public class IntegerArray extends DynamicArray
        {
            public Object[] alloc (int size)
            {
                return new Integer[ size ];
            }

            public final Integer[] array ()
            {
                return (Integer[]) array;
            }
        }
    </PRE>

    <P>A use of IntegerArray might look like this:</P>

    <PRE>
        IntegerArray v = Util.integerList();
        v.addElement (Constants.getInteger(1));
        v.addElement (Constants.getInteger(2));

        Integer[] a = v.array();
        for (int k = 0; k < v.inUse; k++) {
            doIntegerOp(a[k]);
        }
    </PRE>

    <P>Using List, each element access in the loop would entail a
    procedure call and a cast.</P>

    <p>Note that this is not a thread-safe implementation. Consumer
    is responsible for synchronizing the access to the object</p>

    @aribaapi public
*/

abstract public class DynamicArray
{
    private static final int GrowIncrement = 4;
    private static final int InitialSize = 4;

    /**
        The storage array.
        This member should be accessed directly for reading only
        It is public for unit tests only.
        @aribaapi private
    */
    public Object[] array = null;

    /**
        The number of members that are currently used.  This is
        the logical length of the array.
        This member should be accessed directly only for reading.
        @deprecated see inUse()
        @aribaapi ariba
    */
    public int inUse = 0;

    /**
        The number of members that are currently used.  This is
        the logical length of the array.
        This member should be accessed directly only for reading.
        @return the number of members that are currently used
        @aribaapi public
    */
    public int inUse ()
    {
        return inUse;
    }

    /**
        Create a DynamicArray with a default storage size.
        @aribaapi public
    */
    public DynamicArray ()
    {
        this(InitialSize);
    }

    /**
        Create a new DynamicArray
        @param size The starting size of the storage array.
        A size of -1 creates an instance with no array allocated.
        @aribaapi public
    */
    public DynamicArray (int size)
    {
        if (size != -1) {
            array = alloc(size);
        }
    }

    /**
        Replace the storage array.

        The new storage array has to be the right type or the next call to
        array() will cause a ClassCastException.  Must override in
        subclass if using the local array hack to get around the
        Netscape varifier bug involving casting of Object arrays.
        @param array The new array to use as storage
        @aribaapi public
    */
    public void setArray (Object[] array)
    {
        this.array = array;
        this.inUse = array.length;
    }

    /**
        Change the size of the storage.
        The size cannot be decreased below the number of elements
        that are currently being used.
        @param size The new requested size
        @aribaapi public
    */
    public void setCapacity (int size)
    {
            // don't reverse the logic here and return, it will not
            // work because trim presumes that there is no slack
            // leftover so the arraycopy is needed.
        if (size < inUse) {
                // don't slice out existing members
            size = inUse;
        }
        Object[] newArray = alloc(size);
        System.arraycopy(array, 0, newArray, 0, inUse);
        array = newArray;
    }

    /**
        Make sure there is room for additional elements.
        @param want The number of elements we want to have space for

        @aribaapi public
    */
    public void makeRoomFor (int want)
    {
        int avail = array.length - inUse;

        if (want < avail) {
            return;
        }

        int proposedIncrease = array.length;

        if (want > avail + proposedIncrease) {
            setCapacity(array.length + want);
        }
        else {
            setCapacity(array.length + proposedIncrease);
        }
    }

    /**
        Add an element to the array.
        @param o The element to add
        @deprecated use add instead
        @see #add(Object)
        @aribaapi public
    */
    public void addElement (Object o)
    {
        add(o);
    }

    /**
        Add an element to the array.
        @param o The element to add

        @aribaapi public
    */
    public void add (Object o)
    {
        if (inUse == array.length) {
            makeRoomFor(1);
        }
        array[inUse++] = o;
    }

    /**
        Add an element to the array, if it isn't in the array
        @param o The element to add.  If o is already in the array
        then it is not added.  o.equals() is used to compare elements.

        @aribaapi public
    */
    public void addElementIfAbsent (Object o)
    {
        if (contains(o)) {
            return;
        }
        addElement(o);
    }

    /**
     * Add all elements in the passed array if not already present.
     * @see #addElementIfAbsent (Object)
     * @param that
     */

    public void addElementsIfAbsent (Object[] that)
    {
        if (that != null && that.length >0 ) {
            for (Object obj : that) {
                addElementIfAbsent(obj);
            }
        }
    }

    /**
        Add an element to the array, if it isn't in the array
        @param o The element to add.  If o is already in the array
        then it is not added.  The == operator is used to
        compare elements.

        @aribaapi public
    */
    public void addIdentical (Object o)
    {
        if (!containsIdentical(o)) {
            addElement(o);
        }
    }

    /**
        Add all the elements from another array.
        @param that The array that we will copy from

        @aribaapi public
    */
    public void addElements (DynamicArray that)
    {
        addAll(that);
    }

    /**
        Add all the elements from another array.
        @param that The array that we will copy from

        @aribaapi public
    */
    public void addAll (DynamicArray that)
    {
        if ((this.inUse + that.inUse) >= this.array.length) {
            makeRoomFor(that.inUse);
        }

        System.arraycopy(that.array, 0, this.array, inUse, that.inUse);
        this.inUse += that.inUse;
    }

    /**
        Add all the elements of the specified Collection.
        @param c The Collection that we will copy from

        @aribaapi public
    */
    public void addAll (Collection c)
    {
        if ((this.inUse + c.size()) >= this.array.length) {
            makeRoomFor(c.size());
        }

        for (Iterator it = c.iterator(); it.hasNext();) {
            add(it.next());
        }
    }

    /**
        Add all the elements from another array.
        @param that The array that we will copy from

        @aribaapi public
    */
    public void addElements (Object[] that)
    {
        addAll(that);
    }

    /**
        Add all the elements from another array.
        @param that The array that we will copy from

        @aribaapi public
    */
    public void addAll (Object[] that)
    {
        if ((this.inUse + that.length) >= this.array.length) {
            makeRoomFor(that.length);
        }

        System.arraycopy(that, 0, this.array, inUse, that.length);
        this.inUse += that.length;
    }

    /**
        Check if the array contains an object.
        The == operator is used to compare objects
        @param x the object to check for
        @return true if the object is in the array

        @aribaapi public
    */
    public boolean containsIdentical (Object x)
    {
        for (int i = 0; i < inUse; i++) {
            if (array[i] == x) {
                return true;
            }
        }
        return false;
    }

    /**
        Check if the array contains an object.
        Object.equals() is used to compare objects.
        @param x the object to check for
        @return true if the object is in the array

        @aribaapi public
    */
    public boolean contains (Object x)
    {
        return -1 != indexOf(x);
    }

    /**
        Find the location of an object in the array.
        Object.equals() is used to compare objects.
        @param o The object to look for
        @return the index of the object, or -1 if it is not found

        @aribaapi public
    */
    public int indexOf (Object o)
    {
        for (int i = 0; i < inUse; i++) {
            if (SystemUtil.equal(o, array[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
        Return a copy of the underlying array sized to fit the number
        of allocated elements.
        @return The array copy

        @aribaapi public
    */
    public Object[] arrayCopy ()
    {
        Object[] newArray = alloc(inUse);
        System.arraycopy(array, 0, newArray, 0, inUse);
        return newArray;
    }

    /**
        Make the allocated length equal the number of elements in use.
        @return the trimmed array.

        @aribaapi public
    */
    public Object[] trim ()
    {
        if (this.array.length > this.inUse) {
            setCapacity(this.inUse);
        }

        return array;
    }

    /**
        Remove an element from the array
        @param index The index of the element to remove
        @return the removed object
        @exception ArrayIndexOutOfBoundsException when index is larger than the
        logical size of the array (inUse) or when it is less than 0.

        @aribaapi public
    */
    public Object removeElementAt (int index)
    {
        return remove(index);
    }

    /**
        Remove an element from the array
        @param index The index of the element to remove
        @return the removed object
        @exception ArrayIndexOutOfBoundsException when index is larger than the
        logical size of the array (inUse) or when it is less than 0.

        @aribaapi public
    */
    public Object remove (int index)
    {
        if (index >= inUse) {
            throw new ArrayIndexOutOfBoundsException(
                Fmt.S("%s >= %s",
                      Constants.getInteger(index),
                      Constants.getInteger(inUse)));
        }

        Object object = array[index];
        int copyCount = inUse - index - 1;
        if (copyCount > 0) {
            System.arraycopy(array, index + 1, array, index, copyCount);
        }

        inUse--;
        array[inUse] = null;

        return object;
    }

    /**
        Insert an object into the array
        @param element The object to be inserted
        @param index The location where the object should be inserted
        @exception ArrayIndexOutOfBoundsException when index is larger than the
        logical size of the array (inUse)

        @aribaapi public
    */
    public void insertElementAt (Object element, int index)
    {
        add(index, element);
    }

    /**
        Insert an object into the array
        @param element The object to be inserted
        @param index The location where the object should be inserted
        @exception ArrayIndexOutOfBoundsException when index is larger than the
        logical size of the array (inUse)

        @aribaapi public
    */
    public void add (int index, Object element)
    {
        if (index >= inUse + 1) {
            throw new ArrayIndexOutOfBoundsException(
                Fmt.S("%s >= %s",
                      Constants.getInteger(index),
                      Constants.getInteger(inUse)));
        }

        makeRoomFor(1);
        System.arraycopy(array, index, array, index + 1, inUse - index);
        array[index] = element;
        inUse++;
    }

    /**
        Allocate the storage array.
        Subclasses should override this, allocating an array of the correct type.
        @param size The size of the array to allocate
        @return The allocated array

        @aribaapi public
    */
    abstract public Object[] alloc (int size);

    /**
        A version of addElement for cases where all you've got is a
        Foo[]

        @param a original array
        @param x element to add
        @return new array of length <code>a.length</code>+1 containing contents
                of <code>a</code> plus x as the last element
        @aribaapi private
    */
    public Object[] extend (Object[] a, Object x)
    {
        Object[] newArray = alloc(a.length + 1);
        System.arraycopy(a, 0, newArray, 0, a.length);
        newArray[a.length] = x;
        return newArray;
    }

    /**
        Converts to a String.
        toString() is called on each element individually and then
        a space is placed between each element.  The entire array
        is enclosed in parentheses
        @return the String representation of the Dynamic array

        @aribaapi public
    */
    public String toString ()
    {
        FastStringBuffer result = new FastStringBuffer('(');
        for (int i = 0; i < inUse; i++) {
            result.append(String.valueOf(array[i]));
            if (i+1 < inUse) {
                result.append(' ');
            }
        }
        result.append(')');
        return result.toString();
    }

    /**
        Converts the array to a List.
        @return A List version of the array

        @aribaapi public
    */
    public List toList ()
    {
        this.trim();
        return ListUtil.arrayToList(array);
    }

    /**
        Resets the array
        The size of underlying array remains the same

        @aribaapi documented
    */
    public void clear ()
    {
        for (int i = 0, n = this.array.length; i < n; i++) {
            this.array[i] = null;
        }
        this.inUse = 0;
    }
}