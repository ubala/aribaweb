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

    $Id: //ariba/platform/util/core/ariba/util/core/SparseVector.java#6 $
*/

package ariba.util.core;

/**
    Object subclass that manages an array of objects. Unlike a Vector,
    a SparseVector can contain <b>null</b>.

    @aribaapi documented
*/
public class SparseVector extends Vector implements Cloneable
{
    /**
        Constructs a List with an initial capacity of 4 elements.
    */
    public SparseVector ()
    {
        super();

            // turn off null exception checking
        nullException = false;
    }

    /**
        Primitive constructor. Constructs a List large enough to
        hold <b>initialCapacity</b> elements. The List will grow to
        accommodate additional objects, as needed.

        @param initialCapacity the initial capacity; must be greater
        than or equal to zero
    */
    public SparseVector (int initialCapacity, boolean dontAllowException)
    {
        super(initialCapacity);

            // turn off null exception checking
        nullException = dontAllowException;
    }

    /**
        Finds the location of an element in the List starting at a
        given offset.
        
        @param element the element to search for. The comparison is
        performed using <b>equals()</b> with each element.
        @param index the offset in the List to begin searching
        
        @return the index of <b>element</b> in the List. Returns
        <b>-1</b> if the element is not present in the region
        searched.
        @aribaapi documented
    */
    public int indexOf (Object element, int index)
    {
            // handle the case of a null element as concisely as
            // possible
        if (element == null) {
            return indexOfIdentical(element, index);
        }
        return super.indexOf(element, index);
    }

    
    /**
        Search for the the last index of <b>element</b> in the vector, at or
        prior to <b>index</b>.
        
        @param element the element to search for in the List. The
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
        if (element != null) {
            return super.lastIndexOf(element, index);
        }
        if (index >= count()) {
            throw new ArrayIndexOutOfBoundsException(
                Fmt.S("%s >= %s", Constants.getInteger(index),
                      Constants.getInteger(count())));
        }
        
        Object[] array = elementArrayNoCopy();
        for (int i = index; i >= 0; i--) {
            if (element==array[i]) {
                return i;
            }
        }
        return -1;
    }
}
