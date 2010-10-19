/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/Vector.java#26 $
    Responsible: bluo
*/

package ariba.util.core;

import ariba.util.io.FormattingSerializer;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
    Object subclass that manages an array of objects. A Vector cannot
    contain <b>null</b>.

    @aribaapi documented
*/
public class Vector extends AbstractList implements Cloneable, Externalizable
{

    private Object array[];
    private int count;
    private boolean isModifiable = true;

    /**
        to allow null in vector by subclasses

        @aribaapi private
    */
    protected boolean nullException = true;

    static final int DefaultSize = 4;

    private static final Object theEmptyArray[] = Constants.EmptyArray;

    static final String ARRAY_KEY = "array";

    /**
        Constructs a Vector with an initial capacity of 4 elements.
        @aribaapi documented
        @deprecated use ListUtil.list
        @see ariba.util.core.ListUtil#list()
    */
    public Vector ()
    {
        super();
        this.array = theEmptyArray;
        this.count = 0;
    }

    /**
        Primitive constructor. Constructs a Vector large enough to
        hold <b>initialCapacity</b> elements. The Vector will grow to
        accommodate additional objects, as needed.

        @param initialCapacity the initial capacity; must be greater
        than or equal to zero
        @deprecated use ListUtil.list
        @see ariba.util.core.ListUtil#list(int)
        @aribaapi documented
    */
    public Vector (int initialCapacity)
    {
        super();
        this.array = initialCapacity > 0 ? new Object[initialCapacity] : theEmptyArray;
        this.count = 0;
    }

    /**
        Creates a Vector from the objects in the given array.

        The elements in the array are copied if the boolean is true.
        @param array the array to use as the initial contents of the
        vector
        @param copy if <b>true</b>, the array is copied; if <b>false</b>
        the array is shared. However internal <b>Vector</b> operations
        may copy or modify the array at a later point, so sharing the
        array in a active manner should be avoided.
        @deprecated use ListUtil.arrayToList
        @see ariba.util.core.ListUtil#arrayToList(java.lang.Object[], boolean)
        @aribaapi documented
    */
    public Vector (Object[] array, boolean copy)
    {
        if (copy) {
            count = array.length;
            this.array = new Object[count];
            System.arraycopy(array, 0, this.array, 0, count);
        }
        else {
            this.array = array;
            count = array.length;
        }
    }

    /**
        Creates a Vector from the objects in the given array.

        The elements in the array are copied.

        @param array the array to use as the initial contents of the
        vector
        @deprecated use ListUtil.list
        @see ariba.util.core.ListUtil#arrayToList(java.lang.Object[])

        @aribaapi documented
    */
    public Vector (Object[] array)
    {
        this(array, true);
    }

    /**
        Creates a Vector from the objects in the given Vector.

        The elements in the array are copied.

        @param copy the array to use as the initial contents of the
        vector
        @deprecated use ListUtil.list
        @see ariba.util.core.ListUtil#collectionToList(java.util.Collection)
        @aribaapi private
    */
    public Vector (Vector copy)
    {
        this(copy.array, true);
            // after the copy we need to set the actual count
            // based on the one passed in
        this.count = copy.count;
    }

    /**
        Clones the Vector.  Does not clone its elements.

        @return a shallow copy of this Vector
        @aribaapi documented
    */
    public Object clone ()
    {
        Vector newVect;
        try {
            newVect = (Vector)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError("Error in clone(). This shouldn't happen.");
        }

        if (count == 0) {
            newVect.array = theEmptyArray;
            return newVect;
        }

        newVect.array = new Object[count];
        System.arraycopy(array, 0, newVect.array, 0, count);

        return newVect;
    }

    /**
        Returns the number of elements in the Vector. This returns the
        same value as size().
        @return the number of elements currently stored in the vector.
        @deprecated use size()
        @see #size
        @aribaapi ariba
    */
    public int count ()
    {
        return count;
    }

    /**
        Returns the number of elements in the Vector. This returns the
        same value as count().
        @return the number of elements currently stored in the vector.
        @aribaapi public
    */
    public int size ()
    {
        return count;
    }

    /**
        Returns whether <code>this</code> is modifiable, that is whether the methods
        that change the elements of this instance are supported or not.  <p>

        For the client, unmodifiability is a promise that the elements of a
        <code>Vector</code> won't change, not that the internal representation
        won't change. <p>

        Modifiable <code>Vectors</code> can be made unmodifiable
        (see {@link #setUnmodifiable()}) but not vice-versa. Setting a
        <code>Vector</code> to be unmodifiable is thus a one-way operation.
        Unmodifiable <code>Vectors</code> are consistent with the JDK 1.2
        specification in that they will throw an UnsupportedOperationException if
        a mutator method is called. <p>

        Modifiable is the default state of <code>Vectors</code>. All constructors
        of this class create modifiable <code>Vectors</code>.  Note that {@link #clone}
        honors modifiability, that is, clones of unmodifiable <code>Vectors</code>
        are unmodifiable. <p>

        @return <code>true</code> if <code>this</code> is modifiable,
                <code>false</code> otherwise
        @aribaapi ariba
        @deprecated not supported with Collections.unmodifiableList

    */
    public boolean isModifiable ()
    {
        return isModifiable;
    }

    /**
        Sets <code>this</code> to be unmodifiable.  This freezes the
        <code>Vector</code> and it cannot subsequently be made modifiable again. <p>
        @deprecated use Collections.unmodifiableList
        @see java.util.Collections#unmodifiableList(java.util.List)

        @aribaapi ariba
    */
    public void setUnmodifiable ()
    {
        isModifiable = false;
    }

    /**
        Checks if the Vector is empty.
        @return <b>true</b> if the Vector contains no elements.
        @aribaapi documented
    */
    public boolean isEmpty ()
    {
        return (count == 0);
    }

    /**
        Adds <b>element</b> as the last element of the Vector, if not
        already present within the Vector.

        @param element the element to add
        @exception NullPointerException if <b>element</b> is <b>null</b>.
        @deprecated use ListUtil.addElementIfAbsent. Since
        performance of this method is linear with the size of the
        list, consider using a different datastructure such as
        java.util.HashSet
        @see ariba.util.core.ListUtil#addElementIfAbsent
        @aribaapi ariba
    */
    public void addElementIfAbsent (Object element)
    {
        if (element == null && nullException) {
            throw new NullPointerException("It is illegal to store nulls in Vectors.");
        }

        if (!contains(element)) {
            addElement(element);
        }
    }

    /**
        Inserts <b>element</b> before <b>existingElement</b> in the
        Vector.

        @param element the element to insert into the vector
        @param existingElement the element to insert before. If
        <b>existingElement</b> is <b>null</b> or cannot be found, this
        method does nothing

        @return <b>true</b> if <b>element</b> was inserted and
        <b>false</b> if it was not.

        @deprecated use ListUtil
        @see ariba.util.core.ListUtil#insertElementBefore

        @exception NullPointerException if <b>element</b> is <b>null</b>.

        @aribaapi ariba
    */
    public boolean insertElementBefore (Object element, Object existingElement)
    {

        if (element == null && nullException) {
            throw new NullPointerException("It is illegal to store nulls in Vectors.");
        }

        if (existingElement == null) {
            return false;
        }
        int index = indexOf(existingElement);
        if (index == -1) {
            return false;
        }
        else {
            insertElementAt(element, index);
            return true;
        }
    }

    /**
        Inserts <b>element</b> after <b>existingElement</b> in the
        Vector.

        @param element the element to insert into the vector
        @param existingElement the element to insert after. If
        <b>existingElement</b> is <b>null</b> or cannot be found, this
        method does nothing

        @return <b>true</b> if <b>element</b> was inserted and
        <b>false</b> if it was not.

        @exception NullPointerException if <b>element</b> is <b>null</b>.
        @deprecated use ListUtil
        @see ariba.util.core.ListUtil#insertElementAfter
        @aribaapi ariba
    */
    public boolean insertElementAfter (Object element, Object existingElement)
    {
        if (element == null && nullException) {
            throw new NullPointerException("It is illegal to store nulls in Vectors.");
        }

        if (existingElement == null) {
            return false;
        }
        int index = indexOf(existingElement);
        if (index == -1) {
            return false;
        }
        if (index >= count - 1) {
            addElement(element);
        }
        else {
            insertElementAt(element, index + 1);
        }
        return true;
    }

    /**
        Adds the elements contained in <b>aVector</b> that are not already
        present in the Vector to the end of the Vector.

        @param aVector a vector of the elements to be added to this vector
        @deprecated use ListUtil.addElementsIfAbsent. Since
        performance of this method is linear with the size of the
        list, consider using a different datastructure such as
        java.util.HashSet
        @see ariba.util.core.ListUtil#addElementsIfAbsent

        @aribaapi ariba
    */
    public void addElementsIfAbsent (Vector aVector)
    {
        if (aVector == null) {
            return;
        }

        int addCount = aVector.count();
        for (int i = 0; i < addCount; i++) {
            Object nextObject = aVector.elementAt(i);
            if (!contains(nextObject)) {
                addElement(nextObject);
            }
        }
    }

    /**
        Adds the elements contained in <b>aVector</b> to the end of the Vector.

        @param aVector a vector of the elements to be added to this vector
        @deprecated use addAll instead
        @see #addAll
        @aribaapi ariba
    */
    public void addElements (Vector aVector)
    {
        if (aVector == null) {
            return;
        }

        int addCount = aVector.count();

        if ((count + addCount) >= array.length) {
            ensureCapacity(count + addCount);
        }

        for (int i = 0; i < addCount; i++)
            addElement(aVector.elementAt(i));
    }

    /**
        Removes all occurrences of <b>element</b> from the Vector.

        @param element the element to remove.
        @aribaapi documented
    */
    public void removeAll (Object element)
    {
        int i = size();

        while (i-- > 0) {
            if (get(i).equals(element)) {
                remove(i);
            }
        }
    }

    /**
        Removes and returns the element at index 0, or <b>null</b> if the
        Vector is empty.
        @return the first element that was in the vector, but has now
        been removed.
        @aribaapi ariba
    */
    public Object removeFirstElement ()
    {
        if (count == 0) {
            return null;
        }

        return removeElementAt(0);
    }

    /**
        Removes and returns the element at index count() - 1 (the last object)
        or <b>null</b> if the Vector is empty.
        @return the last element that was in the vector, but has now
        been removed.
        @aribaapi ariba
    */
    public Object removeLastElement ()
    {
        if (count == 0) {
            return null;
        }

        return removeElementAt(count - 1);
    }

    /**
        Replaces the element at <b>index</b> with <b>element</b>.

        @param index the location of the element to replace
        @param element the element to replace the existing element with

        @return the replaced object

        @exception ArrayIndexOutOfBoundsException if <b>index</b> is
        an illegal index value.
        @exception NullPointerException if <b>element</b> is
        <b>null</b>
        @deprecated use set
        @see #set(int, java.lang.Object)
        @aribaapi ariba
    */
    public Object replaceElementAt (int index, Object element)
    {
        return set(index, element);
    }

    /**
        Replaces the element <b>oldElement</b> with <b>newElement</b>.

        @param oldElement which element to replace
        @param newElement the element to replace the existing element with

        @return <b>oldElement</b> if it was replaced or <b>null</b>
        if it was not found.

        @exception NullPointerException if <b>newElement</b> is
        <b>null</b>
        @deprecated use ListUtil.replace
        @see ariba.util.core.ListUtil#replace(java.util.List,java.lang.Object,java.lang.Object)
        @aribaapi ariba
    */
    public Object replaceElement (Object oldElement, Object newElement)
    {
        int index = indexOf(oldElement);

        if (index != -1) {
            return replaceElementAt(index, newElement);
        }

        return null;
    }

    /**
        Returns an array containing the Vector's contents. The Vector
        does not keep a reference to the returned value so it is safe
        to modify if desired.
        @return a copy of the internal array with a length exactly
        equal to the number of elements in the array.
        @deprecated use toArray
        @see #toArray()
        @aribaapi ariba
    */
    public Object[] elementArray ()
    {
        Object newArray[] = new Object[count];
        if (count > 0) {
            System.arraycopy(array, 0, newArray, 0, count);
        }

        return newArray;
    }

    /**
        Returns an array containing the Vector's contents.
        Warning!  Does not copy the array!
        Changing the elements in the returned array is very likely to
        change the elements of this <code>Vector</code> even if it is
        unmodifiable.  Use this method with caution. <p>

        @return the internal array used to store elements. The length
        may be larger than the number of elements currently in use--as
        returned by count(). This storage is still activly used by the
        vector which has no way of knowing about any changes you might
        make to it.

        @deprecated this is not supported with java's collection
        classes and should not be used by Ariba code. Callers should
        take the performance hit of using the supported methods
        <code>toArray</code> <code>clear</code> and
        <code>addElements(ListUtil.arrayToList(Object[]))</code>

        @aribaapi ariba
    */
    public Object[] elementArrayNoCopy ()
    {
        return array;
    }

    /**
        Copies the Vector's elements into <b>anArray</b>. This array must be
        large enough to contain the elements.

        @param anArray the array to copy the Vector's elements
        into. Any elements existing in <b>anArray</b> beyond the count
        of elements in this Vector are left unchanged.
        @deprecated use toArray(Object[])
        @see #toArray(java.lang.Object[])

        @aribaapi ariba
    */
    public void copyInto (Object anArray[])
    {
        if (count > 0) {
            System.arraycopy(array, 0, anArray, 0, count);
        }
    }

    /**
        Copies the Vector's elements into <b>anArray</b> at a
        specified offset. This array must be large enough to contain
        the elements.

        @param anArray the array to copy the Vector's elements
        into. Any elements existing in <b>anArray</b> beyond the count
        of elements copied from this Vector are left unchanged.

        @param startingAt the location in <b>anArray</b> to begin
        copying into

        @deprecated use ariba.util.core.ListUtil.copyInto()
        @see ariba.util.core.ListUtil#copyInto

        @aribaapi ariba
    */
    public void copyInto (Object anArray[], int startingAt)
    {
        if (count > 0) {
            System.arraycopy(array, 0, anArray, startingAt, count);
        }
    }

    /**
        Minimizes the Vector's storage area. This will cause
        <b>elementArray</b>'s array to have a length equal to the
        <b>count</b> of the elements in the Vector.

        @see #count
        @see #elementArray
        @deprecated callers should not worry about internal storage
        details.
        @aribaapi ariba
    */
    public void trimToSize ()
    {
        if (count == 0) {
            array = theEmptyArray;
        }
        else if (count != array.length) {
            array = elementArray();
        }
    }

    /**
        Increases, if necessary, the the Vector's storage area so that it can
        contain at least <b>minCapacity</b> elements.

        @param minCapacity the number of elements that this Vector can
        hold without having to grow it's internal storage.
        @deprecated callers do not need to be aware of internal
        storage details
        @aribaapi ariba
    */
    public void ensureCapacity (int minCapacity)
    {
        if (array == theEmptyArray) {
            array = new Object[DefaultSize];
        }

        if (minCapacity < array.length) {
            return;
        }

        int newLength = (array.length < DefaultSize) ? DefaultSize : array.length;
        while (newLength < minCapacity) {
            newLength = 2 * newLength;
        }

        Object newArray[] = new Object[newLength];
        System.arraycopy(array, 0, newArray, 0, count);

        array = newArray;
    }

    /**
        Returns the number of elements that can be stored in the Vector
        without increasing the Vector's storage area.
        @return the current size of the internal storage of the
        vector. This may be different from the value returned by
        size() and count().
        @deprecated code should not be concerned with such internal
        details
        @aribaapi ariba
    */
    public int capacity ()
    {
        return array.length;
    }

    /**
        Returns an Enumeration that can be used to iterate through all of the
        Vector's elements.

        @return an enumeration over the elements in the Vector.
        @deprecated use iterator ()
        @see #iterator
        @aribaapi ariba
    */
    public Enumeration elements ()
    {
        return new VectorEnumerator(this);
    }

    /**
        Returns an Enumeration that can be used to iterate through the vector's
        elements beginning at element <b>index</b>.

        @param index the index of the first element to be used in the
        iteration

        @return an enumeration over the specified elements in the
        Vector.
        @deprecated use listIterator(int)
        @see #listIterator(int)

        @aribaapi ariba
    */
    public Enumeration elements (int index)
    {
        return new VectorEnumerator(this, index);
    }

    /**
        Searches for the specified <b>element</b> in the Vector.

        @param element the element to search for

        @return <b>true</b> if the Vector contains <b>element</b>. The
        comparison is performed using <b>equals()</b> with each
        element.
        @aribaapi documented
    */
    public boolean contains (Object element)
    {
        if (indexOf(element, 0) != -1) {
            return true;
        }

        return false;
    }

    /**
        Checks if the vector contains <b>element</b> and nothing else.

        @param element the element to check for; It uses
        <b>equals()</b> to perform the containment test.

        @return <b>true</b> if the RowVector contains only
        <b>element</b> and no other object; and <b>false</b>
        otherwise.
        @aribaapi ariba
    */
    public boolean onlyContains (Object element)
    {
        return  (size() == 1 &&
                 elementAt(0).equals(element));
    }

    /**
        Checks if the vector contains the element <b>element</b> using
        the <b>==</b> operator.

        @param element the element to search for; the <b>==</b>
        operator is used for comparison

        @return <b>true</b> if the Vector contains <b>element</b>,
        <b>false</b> otherwise
        @deprecated use ListUtil.containsIdentical instead
        @see ariba.util.core.ListUtil#containsIdentical
        @aribaapi ariba
    */
    public boolean containsIdentical (Object element)
    {
        if (indexOfIdentical(element, 0) != -1) {
            return true;
        }

        return false;
    }

    /**
        Finds the location of an element in the Vector.

        @param element the element to search for. The comparison is
        performed using <b>equals()</b> with each element.

        @return the index of <b>element</b> in the Vector.  Returns <b>-1</b>
        if the element is not present.
        @aribaapi ariba
    */
    public int indexOf (Object element)
    {
        return indexOf(element, 0);
    }

    /**
        Finds the location of an element in the Vector starting at a
        given offset.

        @param element the element to search for. The comparison is
        performed using <b>equals()</b> with each element.
        @param index the offset in the Vector to begin searching

        @return the index of <b>element</b> in the Vector. Returns
        <b>-1</b> if the element is not present in the region
        searched.
        @deprecated use subList() and indexOf(Object) instead
        @see #subList
        @see #indexOf(Object)
        @aribaapi documented
    */
    public int indexOf (Object element, int index)
    {
        if (element == null) {
            return -1;
        }
        for (int i = index; i < count; i++) {
            if (element.equals(array[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
        Finds the location of an element in the Vector starting at a
        given offset using the <b>==</b> operator.

        @param element the element to search for. The comparison is
        performed using <b>==</b> with each element.
        @param index the offset in the Vector to begin searching

        @return the index of <b>element</b> in the Vector. Returns
        <b>-1</b> if the element is not present in the region
        searched.
        @deprecated use subList and ListUtil.indexOfIdentical instead
        @see ariba.util.core.ListUtil#indexOfIdentical
        @see #subList
        @aribaapi ariba
    */
    public int indexOfIdentical (Object element, int index)
    {
        for (int i = index; i < count; i++) {
            if (array[i] == element) {
                return i;
            }
        }

        return -1;
    }

    /**
        Finds the location of an element in the Vector using the
        <b>==</b> operator for comparison.

        @param element the element to search for. The comparison is
        performed using <b>==</b> with each element.

        @return the index of <b>element</b> in the Vector.  Returns
        <b>-1</b> if the element is not present.
        @deprecated use ListUtil.indexOfIdentical instead
        @see ariba.util.core.ListUtil#indexOfIdentical
        @aribaapi ariba
    */
    public int indexOfIdentical (Object element)
    {
        return indexOfIdentical(element, 0);
    }

    /**
        Searches for the last occurrence of the element <b>element</b>
        in the Vector.

        @param element the element to search for. The comparison is
        performed using <b>equals()</b> with each element.

        @return the last index of <b>element</b> in the Vector.
        Returns <b>-1</b> if the element is not present.
        @aribaapi ariba
    */
    public int lastIndexOf (Object element)
    {
        return lastIndexOf(element, count - 1);
    }

    /**
        Search for the the last index of <b>element</b> in the vector, at or
        prior to <b>index</b>.

        @param element the element to search for in the Vector. The
        comparison is performed using <b>equals()</b> with each
        element.
        @param index the offset in the vector to start searching
        backwards from. The search is inclusive of the element at
        location <b>index</b>

        @return the index of the found element, or <b>-1</b> if the
        element is not present in the region searched.
        @aribaapi documented
    */
    public int lastIndexOf (Object element, int index)
    {
        if (index >= count) {
            throw new ArrayIndexOutOfBoundsException(
                Fmt.S("%s >= %s",
                      Constants.getInteger(index),
                      Constants.getInteger(count)));
        }
        if (element == null) {
            return -1;
        }
        for (int i = index; i >= 0; i--) {
            if (element.equals(array[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
        Find the element at the specified location.

        @param index the index of the element in the Vector to return

        @exception ArrayIndexOutOfBoundsException if the index is
        greater than or equal to the number of elements in the Vector

        @return the element at <b>index</b>.
        @deprecated use get
        @see #get(int)

        @aribaapi ariba
    */
    public Object elementAt (int index)
    {
        return get(index);
    }

    /**
        Returns the Vector's first element without modification of the
        Vector.
        @return the first element, or null if there are no elements.
        @deprecated see ariba.util.core.ListUtil.firstElement
        @see ariba.util.core.ListUtil#firstElement
        @aribaapi ariba
    */
    public Object firstElement ()
    {
        if (count == 0) {
            return null;
        }

        return array[0];
    }

    /**
        Returns the Vector's last element without modification of the
        Vector.
        @return the last element, or null if there are no elements.
        @aribaapi ariba
    */
    public Object lastElement ()
    {
        if (count == 0) {
            return null;
        }

        return array[count - 1];
    }

    /**
        Sets the element at <b>index</b> to <b>element</b>.

        @param element the element to place into the Vector at the
        specified location
        @param index the location in the vector to place the element.

        @exception NullPointerException if <b>element</b> is <b>null</b>
        @exception ArrayIndexOutOfBoundsException if <b>index</b> is
        an illegal index value.

        @aribaapi ariba
    */
    public void setElementAt (Object element, int index)
    {
        set(index, element);
    }

    /**
        Removes the element at <b>index</b>.

        @param index the index of the element in the Vector to remove

        @return the element that was removed

        @exception ArrayIndexOutOfBoundsException if <b>index</b> is
        an illegal index value.
        @deprecated use remove(int)
        @see #remove(int)
        @aribaapi ariba
    */
    public Object removeElementAt (int index)
    {
        return remove(index);
    }

    /**
        Inserts <b>element</b> into the Vector at <b>index</b>. All
        elements at and after <b>index</b> are moved.

        @param element the element to insert into the Vector
        @param index the location to insert the element at

        @exception ArrayIndexOutOfBoundsException if <b>index</b> is
        an illegal index value.
        @exception NullPointerException if <b>element</b> is
        <b>null</b>.
        @deprecated use add(int, Object) instead
        @see #add(int, java.lang.Object)
        @aribaapi ariba
    */
    public void insertElementAt (Object element, int index)
    {
        add(index, element);
    }

    /**
        Adds <b>element</b> to the end of the Vector.

        @param element the element to add to the Vector

        @exception NullPointerException if <b>element</b> is
        <b>null</b>
        @deprecated use add ()
        @see #add(Object)
        @aribaapi ariba
    */
    public void addElement (Object element)
    {
        add(element);
    }

    /**
        Removes the first occurrence of <b>element</b> from the
        Vector. The <b>equals()</b> comparison is used. At most one
        instance of the element will be removed on each call.

        @param element the element to remove from the Vector.  The
        comparison is performed using <b>equals()</b> with each
        element.

        @return <b>true</b> if the element was removed, <b>false</b>
        otherwise
        @aribaapi ariba
    */
    public boolean remove (Object element)
    {
        int i = indexOf(element);
        if (i < 0) {
            return false;
        }

        removeElementAt(i);
        return true;
    }

    /**
        Removes the first occurrence of <b>element</b> from the
        Vector. The <b>equals()</b> comparison is used. At most one
        instance of the element will be removed on each call.

        @param element the element to remove from the Vector.  The
        comparison is performed using <b>equals()</b> with each
        element.

        @return <b>true</b> if the element was removed, <b>false</b>
        otherwise
        @deprecated use remove(java.lang.Object)
        @aribaapi ariba
    */
    public boolean removeElement (Object element)
    {
        return remove(element);
    }

    /**
        Removes the first occurrence of <b>element</b> from the
        Vector. The <b>==</b> comparison is used. At most one
        instance of the element will be removed on each call.

        @param element the element to remove from the Vector.  The
        comparison is performed using <b>==</b> with each
        element.

        @return <b>true</b> if the element was removed, <b>false</b>
        otherwise
        @aribaapi ariba
    */
    public boolean removeElementIdentical (Object element)
    {
        int i = indexOfIdentical(element, 0);
        if (i < 0) {
            return false;
        }

        removeElementAt(i);
        return true;
    }

    /**
        Empties the Vector, but leaves its capacity unchanged.
        @deprecated use clear
        @see #clear
        @aribaapi ariba
    */
    public void removeAllElements ()
    {
        clear();
    }

    /**
        Sorts the Vector's contents. String comparisons are case
        sensitive.

        @param ascending if <b>true</b> the Vector will be sorted in
        ascending order, if <b>false</b> in descending order

        @exception ClassCastException if the
        Vector's contents are not all Strings, or are not Comparable.
        @deprecated use ListUtil.sortStrings(List, boolean)
        @see ariba.util.core.ListUtil#sortStrings(List, boolean)

        @aribaapi ariba
    */
    public void sort (boolean ascending)
    {
        Assert.that(nullException,
                    "You can not sort an array that can contain nulls");
        sortStrings(ascending, false);
    }

    /**
        Sorts the Vector's contents, presuming all elements are of
        type <b>String</b>.

        @param ascending if <b>true</b> the Vector will be sorted in
        ascending order, if <b>false</b> in descending order
        @param ignoreCase if <b>true</b> the Vector's Strings will be
        sorted in a case insensitive manner, if <b>false</b> in a case
        sensitive manner

        @deprecated use ListUtil.sortStrings(List, boolean, boolean)
        @see ariba.util.core.ListUtil#sortStrings(List, boolean, boolean)

        @exception ClassCastException if the Vector's contents are not
        all Strings.
        @aribaapi ariba
    */
    public void sortStrings (boolean ascending, boolean ignoreCase)
    {
        Assert.that(nullException,
                    "You can not sort an array that can contain nulls");
        if (!isModifiable) {
            throw new UnsupportedOperationException("Cannot modify unmodifiable Vector");
        }

        Sort.objects(array,
                     null,
                     null,
                     0,
                     count,
                     ignoreCase ? StringCompareIgnoreCase.self : StringCompare.self,
                     ascending ? Sort.SortAscending : Sort.SortDescending);
    }

    /**
        Returns the Vector's string representation.
        @return a formatted string representation of the Vector's
        contents. It will be recursivly formated if the Vector
        contains other Lists or Maps.
        @aribaapi documented
    */
    public String toString ()
    {
        return FormattingSerializer.serializeObject(this);
    }

    /**
        Writes this Vector's data out to the given stream.  Implementation of
        the Externalizable interface

        @serialData calls writeObject() on each Vector element, in order
        @param output the stream to write the object to
        @exception IOException Includes any I/O exceptions that may occur
        @aribaapi private
    */
    public void writeExternal (ObjectOutput output) throws IOException
    {
        output.writeInt(size());
        for (int i = 0; i < size(); i++) {
            output.writeObject(elementAt(i));
        }
    }

    /**
        Reads object data from the given input and restores the contents of this
        object.  Implementation of the Externalizable interface.

        @param input the stream to read data from in order to restore the object
        @exception IOException if I/O errors occur
        @exception ClassNotFoundException If the class for an object being
                     restored cannot be found.
        @aribaapi private
    */
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        int numItems = input.readInt();
        for (int i = 0; i < numItems; i++) {
            addElement(input.readObject());
        }
    }

    /**
        Compares the specified object with this list for equality.  Returns
        <tt>true</tt> if and only if the specified object is also a list, both
        lists have the same size, and all corresponding pairs of elements in
        the two lists are <i>equal</i>.  (Two elements <tt>e1</tt> and
        <tt>e2</tt> are <i>equal</i> if <tt>(e1==null ? e2==null :
        e1.equals(e2))</tt>.)  In other words, two lists are defined to be
        equal if they contain the same elements in the same order.<p>

        @param that the object to be compared for equality with this list.

        @return <tt>true</tt> if the specified object is equal to this list.

        @aribaapi documented
    */
    public boolean equals (Object that)
    {
            // performance enhancement over iterating over each element
        if (that instanceof Vector) {
            if (((Vector)that).count != this.count) {
                return false;
            }
        }
        return super.equals(that);
    }

        /********************************************
            List implementation
        *********************************************/

    /**
        Returns the element at the specified position in this list.

        @param index index of element to return.

        @return the element at the specified position in this list.
        @throws IndexOutOfBoundsException if the given index is out of range
                  (<tt>index &lt; 0 || index &gt;= size()</tt>).
        @aribaapi ariba
    */
    public Object get (int index)
    {
        if (index >= count) {
            if (nullException) {
                throw new ArrayIndexOutOfBoundsException(
                    Fmt.S("%s >= %s",
                          Constants.getInteger(index),
                          Constants.getInteger(count)));
            }
            return null;
        }

        return array[index];
    }

    /**
        Replaces the element at the specified position in this list with the
        specified element. <p>

        @param index index of element to replace.
        @param element element to be stored at the specified position.
        @return the element previously at the specified position.

        @throws ClassCastException if the class of the specified element
                  prevents it from being added to this list.
        @throws IllegalArgumentException if some aspect of the specified
                 element prevents it from being added to this list.

        @throws IndexOutOfBoundsException if the specified index is out of
                   range (<tt>index &lt; 0 || index &gt;= size()</tt>).
        @aribaapi documented
    */
    public Object set (int index, Object element)
    {
        if (element == null && nullException) {
            throw new NullPointerException("It is illegal to store nulls in Vectors.");
        }

        if (index >= count && nullException) {
            throw new ArrayIndexOutOfBoundsException(
                Fmt.S("%s >= %s",
                      Constants.getInteger(index),
                      Constants.getInteger(count)));
        }
        else if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(Fmt.S("%s < 0", index));
        }
        if (!isModifiable) {
            throw new UnsupportedOperationException("Cannot modify unmodifiable Vector");
        }

        ensureCapacity(index+1);
        if (index >= count) {
            count = index+1;
        }
        Object oldObject = elementAt(index);
        array[index] = element;

        return oldObject;
    }

    /**
        Inserts the specified element at the specified position in this list.
        Shifts the element currently at that position
        (if any) and any subsequent elements to the right (adds one to their
        indices).<p>

        @param index index at which the specified element is to be inserted.
        @param element element to be inserted.

        @throws ClassCastException if the class of the specified element
                  prevents it from being added to this list.
        @throws IllegalArgumentException if some aspect of the specified
                 element prevents it from being added to this list.
        @throws IndexOutOfBoundsException index is out of range (<tt>index &lt;
                 0 || index &gt; size()</tt>).
        @aribaapi documented
    */
    public void add (int index, Object element)
    {
        if (index >= count + 1 && nullException) {
            throw new ArrayIndexOutOfBoundsException(
                Fmt.S("%s >= %s",
                      Constants.getInteger(index),
                      Constants.getInteger(count)));
        }

        if (element == null && nullException) {
            throw new NullPointerException("It is illegal to store nulls in Vectors.");
        }
        if (!isModifiable) {
            throw new UnsupportedOperationException(
                Fmt.S("Cannot add element '%s' to unmodifiable Vector", element));
        }

        if (count >= array.length) {
            ensureCapacity(Math.max(count, index) + 1);
        }

        System.arraycopy(array, index, array, index + 1, count - index);
        array[index] = element;
        if (index >= count) {
            count = index+1;
        }
        else {
            count++;
        }
    }

    /**
        Appends the specified element to the end of this List (optional
        operation). <p>

        @param element element to be appended to this list.

        @return <tt>true</tt> (as per the general contract of
        <tt>Collection.add</tt>).

        @throws ClassCastException if the class of the specified element
                  prevents it from being added to this set.

        @throws IllegalArgumentException some aspect of this element prevents
                   it from being added to this collection.
        @aribaapi documented
    */
    public boolean add (Object element)
    {
        if (element == null && nullException) {
            throw new NullPointerException("It is illegal to store nulls in Vectors.");
        }

        if (!isModifiable) {
            throw new UnsupportedOperationException(
                Fmt.S("Cannot add element '%s' to unmodifiable Vector", element));
        }
        if (count >= array.length) {
            ensureCapacity(count + 1);
        }

        array[count] = element;
        count++;
        return true;
    }

    /**
        Adds the elements contained in <b>c</b> to the end of the Vector.

        @param c a collection of the elements to be added to this vector
        @return <b>true</b> if this vector changed as a result of the call
        @aribaapi documented
    */
    public boolean addAll (Collection c)
    {
        if (c == null) {
            return false;
        }
        return super.addAll(c);
    }


    /**
        Removes the element at the specified position in this list (optional
        operation).  Shifts any subsequent elements to the left (subtracts one
        from their indices).  Returns the element that was removed from the
        list.<p>

        @param index the index of the element to remove.
        @return the element previously at the specified position.

        @throws IndexOutOfBoundsException if the specified index is out of
                  range (<tt>index &lt; 0 || index &gt;= size()</tt>).
        @aribaapi ariba
    */
    public Object remove (int index)
    {
        if (index >= count) {
            throw new ArrayIndexOutOfBoundsException(
                Fmt.S("%s >= %s",
                      Constants.getInteger(index),
                      Constants.getInteger(count)));
        }
        if (!isModifiable) {
            throw new UnsupportedOperationException(
                Fmt.S("Cannot remove element '%s' from unmodifiable Vector",
                      array[index]));
        }

        Object object = array[index];
        int copyCount = count - index - 1;
        if (copyCount > 0) {
            System.arraycopy(array, index + 1, array, index, copyCount);
        }

        count--;
        array[count] = null;

        return object;
    }

    /**
        Creates a new Vector with the same contents as the given Collection.

        @param collection collection
        @deprecated use ListUtil.list()
        @see ariba.util.core.ListUtil#collectionToList(java.util.Collection)
        @aribaapi documented
    */
    public Vector (Collection collection)
    {
        this(collection.size());
        addAll(collection);
    }

    /**
        Removes all of the elements from this collection.
        The collection will be empty after this call returns (unless it throws
        an exception).<p>

        @aribaapi public
    */
    public void clear ()
    {
        if (!isModifiable) {
            throw new UnsupportedOperationException("Cannot modify unmodifiable Vector");
        }
        for (int i = 0; i < count; i++) {
            array[i] = null;
        }

        count = 0;
    }

    /**
        Removes from this list all of the elements whose index is between
        <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.
        Shifts any succeeding elements to the left (reduces their index).  This
        call shortens the ArrayList by <tt>(toIndex - fromIndex)</tt>
        elements.  (If <tt>toIndex==fromIndex</tt>, this operation has no
        effect.)<p>

        @param fromIndex index of first element to be removed.
        @param toIndex index after last element to be removed.
        @aribaapi ariba
    */
    public void removeRange (int fromIndex, int toIndex)
    {
        if (fromIndex <= toIndex && fromIndex >=0 && toIndex <= count) {
            if (!isModifiable) {
                throw new UnsupportedOperationException(
                    "Cannot modify unmodifiable Vector");
            }
            int len = toIndex - fromIndex;
            System.arraycopy(array, toIndex, array, fromIndex, count - toIndex);

            for (int i = count - len; i < count; i++) {
                array[i] = null;
            }

            count -= len;
        }
        else {
            throw new IndexOutOfBoundsException(
                    Fmt.S("fromIndex: %s, toIndex: %s, count: %s",
                          Constants.getInteger(fromIndex),
                          Constants.getInteger(toIndex),
                          Constants.getInteger(count)));
        }
    }

    /**
        Removes from this collection all of its elements that are contained in
        the specified collection. <p>

        @param c elements to be removed from this collection.
        @return <tt>true</tt> if this collection changed as a result of the
        call.

        @see #remove(Object)
        @see #contains(Object)
        @aribaapi documented
    */
        // This is repeated here to avoid a "Ambiguous invocation" compiler error message
        // for code that calls removeAll(Object o) if we do not explicitly have the
        // definition of removeAll(Collection) in this compilation unit.
    public boolean removeAll (Collection c)
    {
        return super.removeAll(c);
    }
}

