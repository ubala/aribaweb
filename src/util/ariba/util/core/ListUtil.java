/*
    Copyright 1996-2010 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/ListUtil.java#38 $
*/

package ariba.util.core;

import ariba.util.io.DeserializationException;
import ariba.util.io.Deserializer;
import ariba.util.log.Log;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;

/**
    List Utilities. These are helper functions for dealing with
    lists.

    @aribaapi documented
*/
public abstract class ListUtil
{
    //-----------------------------------------------------------------------
    // nested class

    /**
        Is a <code>java.util.Comparator</code> for <code>Lists</code> based on
        a supplied <code>elementComparator</code> that delegates to
        {@link ListUtil#compare} when doing the comparison. <p/>

        @aribaapi ariba
    */
    public static final class Comparator<T extends List<?>>
    implements java.util.Comparator<T>
    {
        private java.util.Comparator<T> _elementComparator;

        public Comparator (java.util.Comparator<T> elementComparator)
        {
            _elementComparator = elementComparator;
        }

        public Comparator ()
        {
            this(null);
        }

        /**
            Returns the element <code>java.util.Comparator</code> of
            <code>this</code>. <p/>

            @aribaapi ariba
        */
        public java.util.Comparator<T> getElementComparator ()
        {
            return _elementComparator;
        }

        /**
            Compares <code>first</code> and <code>second</code> as lists
            according to the <code>elementComparator</code>. <p/>

            @aribaapi ariba
        */
        public int compare (T first, T second)
        {
            return ListUtil.compare(first, second, _elementComparator);
        }

        public boolean equals (Comparator<T> that)
        {
            return that != null && SystemUtil.equal(_elementComparator,
                                                    that._elementComparator);
        }

        public boolean equals (Object that)
        {
            return that instanceof Comparator && equals((Comparator<T>)that);
        }

        public int hashCode ()
        {
            return Comparator.class.hashCode() ^
                    SystemUtil.hashCode(_elementComparator);
        }
    }


    //-------------------------------------------------------------------------
    // public methods

    /**
        An empty immutable list.
        @deprecated use {@link Collections#emptyList()} instead
        @aribaapi public
    */
    public static List ImmutableEmptyList = ListUtil.immutableList(
            ListUtil.list());

    /**
        Constructs a new type-safe List with a small initial capacity. <p/>

        To construct a type-safe <code>List</code>:<ul>
        <li><code>List&lt;X> typesafe = ListUtil.list()</code>
        </ul>
        For a raw <code>List</code>:<ul>
        <li><code>List raw = ListUtil.list()</code>
        </ul>
        There will be no compile warnings either way.

        @return new List
        @aribaapi public
    */
    public static <T> List<T> list ()
    {
        return new ArrayList<T>(1); // OK
    }

    /**
        Primitive constructor. Constructs a List large enough to
        hold <b>initialCapacity</b> elements. The List will grow to
        accommodate additional objects, as needed. <p/>

        To construct a type-safe <code>List</code>:<ul>
        <li><code>List&lt;X> typesafe = ListUtil.list()</code>
        </ul>
        For a raw <code>List</code>:<ul>
        <li><code>List raw = ListUtil.list()</code>
        </ul>
        There will be no compile warnings either way.

        @param initialCapacity the initial capacity; must be greater
        than or equal to zero
        @return an empty List with the specified capacity

        @aribaapi public
    */
    public static <T> List<T> list (int initialCapacity)
    {
        return new ArrayList<T>(initialCapacity); // OK
    }

    /**
        Helper method to clone a List.
        @param l the list to clone
        @return a new list of the same type

        @aribaapi documented
    */
    @SuppressWarnings("unchecked")
    public static <T> List<T> cloneList (List<? extends T> l)
    {
        return (List<T>)ClassUtil.clone(l);
    }

    /**
        Returns a list which is a sub-collection of <code>values</code>
        from index <code>fromIndex</code> (inclusive) to <code>toIndex</code>
        (exclusive). <p/>

        @param values the collection of values
        @param fromIndex the beginning index, inclusive
        @param toIndex the end index, exclusive
        @param createNewList if <code>true</code>, this method uses a new
                list to create the sublist, else this method attempts to use
                a computationally efficient result which may refer to
                <code>values</code> (and so is not modifiable)
        @aribaapi ariba
    */
    public static <V> List<V> subList (
            Iterable<V> values,
            int fromIndex,
            int toIndex,
            boolean createNewList)
    {
        if (values instanceof List) {
            List<V> list = (List<V>)values;
            List<V> result = list.subList(fromIndex, toIndex);
            return createNewList ? new ArrayList<V>(result) : result;
        }
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException(Fmt.S("fromIndex = %s", fromIndex));
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(Fmt.S("fromIndex(%s) > toIndex(%s)",
                                                     fromIndex, toIndex));
        }
        if (fromIndex == toIndex) {
            return createNewList ? ListUtil.<V>list() : Collections.<V>emptyList();
        }
        List<V> result = ListUtil.list();
        int idx = 0;
        for (V value : values) {
            if (idx >= fromIndex) {
                if (idx < toIndex) {
                    result.add(value);
                }
                else {
                    break;
                }
            }
            ++idx;
        }
        return result;
    }
    /**
        @aribaapi ariba
    */
    public static <V> List<V> subList (Collection<V> values, int fromIndex, int toIndex)
    {
        return subList(values, fromIndex, toIndex, false);
    }


    /**
        Creates a List from the objects in the given array.

        The elements in the array are copied.

        @param array the array to use as the initial contents of the
        list
        @return a List containing objects from the specified array

        @aribaapi documented
    */
    public static <T> List<T> arrayToList (T[] array)
    {
        List<T> l = list(array.length);
        l.addAll(Arrays.asList(array));
        return l;
    }

    /**
        Creates a List from the objects in the given array.

        The elements in the array are copied if the boolean is true.
        @param array the array to use as the initial contents of the
        list
        @param copy if <b>true</b>, the array is copied; if <b>false</b>
        the array is shared. However internal <b>List</b> operations
        may copy or modify the array at a later point, so sharing the
        array in a active manner should be avoided.
        @return a List containing objects from the specified array
        @aribaapi documented
    */
    public static <T> List<T> arrayToList (T[] array, boolean copy)
    {
        if (copy) {
            return arrayToList(array);
        }
        else {
            return new Vector(array, false); // OK
        }
    }

    /**
        Creates a new List with the same contents as the given Collection.

        @param source The collection to copy
        @return List with same contents as Collection
        @aribaapi public
    */
    public static <T> List<T> collectionToList (Collection<T> source)
    {
        return new ArrayList<T>(source); // OK
    }

    /**
        Returns new List with all the items in source Collection added to it. Combines
        ListUtil.list() and List addAll(Collection). Is easier to type and remember than
        collectionToList. Throws NullPointerException if source is null.
        @aribaapi ariba
    */
    public static <T> List<T> listAddAll (Collection<T> source)
    {
        return collectionToList(source);
    }

    /**
        Tokenize a String with a specified separator character and
        return a list with the tokenized elements.

        <p>
        Takes a string in the form "Description.Price.Amount" with a
        delimiter of '.' and turns it into an list of strings:
        <p>
        <pre>
        list.get(0) = "Description"
        list.get(1) = "Price"
        list.get(2) = "Amount"
        </pre>

        @param str a string to tokenize
        @param delimiter the delimiter to use when tokenizing

        @return a list of Strings containing each substring but
        excluding the delimiter character

        @see ariba.util.core.StringUtil#delimitedStringToArray

        @aribaapi documented
    */
    public static List<String> delimitedStringToList (String str, char delimiter)
    {
        int limit = StringUtil.occurs(str, delimiter);
        List<String> list = list(limit+1);
        int start = 0;
        int nextEnd;
        for (int i = 0; i < limit; i++) {
            nextEnd = str.indexOf(delimiter, start);
            list.add(str.substring(start, nextEnd));
            start = nextEnd + 1;
        }

        list.add(str.substring(start));
        return list;
    }

    /**
        Populates the list with serialized data from the string.

        @param serialized String containing serialized list data
        @aribaapi private
    */
    public static void fromSerializedString (List l, String serialized)
    {
        StringReader reader = new StringReader(serialized);

        try {
            new Deserializer(reader).readObject(l);
        }
        catch (IOException e) {
            Log.util.error(10371, e);
        }
        catch (DeserializationException e) {
            Log.util.error(10371, e);
        }
        finally {
            reader.close();
        }
    }

        // string table for localized strings
    private static final String StringTable = "ariba.util.core";

        // resource tags for localized strings
    private static final String ListSeparator = "ListSeparator";

    /**
        Copy a Collection.  This method will copy the source list and
        output a destination list.  For each list or hastable
        within the source list they will also be copied, all other
        types will be shared.  So for a list of lists or
        hashtables, this will recurse.

        @param source the source list to copy.
        @return the destination list.
        @aribaapi documented
    */
    public static List copyList (List source)
    {
        List destination = list(source.size());
        for (int i=0, s=source.size(); i<s; i++) {
            Object value = source.get(i);
            destination.add(copyValue(value));
        }
        return destination;
    }

    /**
     * This method will do 2 things:
     * 1) Copy objects which are not of type Map into a new List object.
     * 2) Copy the contents of the Map entries into a SortedMap and then copy the 
     *    SortedMap back into the List.
     *    
     * @param list
     * @return a List with its original ordering intact and Map objects copied into a 
     *         SortedMap.
     */
    public static List copyAndSortMapInList (List list)
    {
        List newList = ListUtil.list();
        
        if (ListUtil.nullOrEmptyList(list)) {
            return list;
        }
        
        for (ListIterator i = list.listIterator(); i.hasNext(); ) {
            Object listObj = i.next();
            if (listObj instanceof List) {
                // Recurse if we have a List within a List
                List l = copyAndSortMapInList((List)listObj);
                newList.add(l);
            }
            else if (listObj instanceof Map) {
                // Place the contents of the Map into a SortedMap
                SortedMap sm = MapUtil.copyAndSortMap((Map)listObj);
                newList.add(sm);
            }
            else {
                newList.add(listObj);
            }
        }
        return newList;
    }    
    
        //protected method used in hashtableUtil and ListUtil
    static Object copyValue (Object value)
    {
        if (value instanceof Map) {
            return MapUtil.copyMap((Map)value);
        }
        else if (value instanceof List) {
            return copyList((List)value);
        }
        else {
            return value;
        }
    }

    /**
        Adds <b>element</b> as the last element of the List, if not
        already present within the List.

        @param l the List to add to
        @param element the element to add
        @exception NullPointerException if <b>element</b> is <b>null</b>.
        @aribaapi documented
    */
    public static <T> void addElementIfAbsent (List<T> l, T element)
    {
        if (!l.contains(element)) {
            l.add(element);
        }
    }

    /**
        Simple convenience method that adds the array elements in <code>toAdd</code>
        to the <code>collection</code>. <p/>
        @param collection the collection to add the elements to; may not be
               <code>null</code>
        @param toAdd the array containing the elements to add; may not be
               <code>null</code>
        @aribaapi ariba
    */
    public static <T> void addToCollection (Collection<? super T> collection, T[] toAdd)
    {
        for (int i=0; i<toAdd.length; ++i) {
            collection.add(toAdd[i]);
        }
    }

    /**
        Simple convenience method that adds the array elements in <code>toAdd</code>
        to the <code>collection</code>. <p/>
        @param collection the collection to add the elements to; may not be
               <code>null</code>
        @param toAdd the array containing the elements to add; may not be
               <code>null</code>
        @aribaapi ariba
    */
    public static <T> void addAll (Collection<? super T> collection, Iterable<T> toAdd)
    {
        for (T t : toAdd) {
            collection.add(t);
        }
    }

    /**
        Adds the elements contained in <b>aList</b> that are not already
        present in the List to the end of the List.
        @param destList the list to add elements to
        @param aColl a collection of the elements to be added to destList

        @aribaapi documented
    */
    public static <T> void addElementsIfAbsent (List<T> destList,
                                                Collection<? extends T> aColl)
    {
        if (aColl == null) {
            return;
        }

        Iterator<? extends T> i = aColl.iterator();
        while (i.hasNext()) {
            addElementIfAbsent(destList, i.next());
        }
    }

    /**
        Adds <b>element</b> as the last element of the List; delete the old
        element if existed .

        @param l the List to add to
        @param element the element to add
        @exception NullPointerException if <b>element</b> is <b>null</b>.
        @aribaapi documented
    */
    public static void appendAndRemoveIfPresent (List l, Object element)
    {
        int index = l.indexOf(element);
        if (index >=0) {
            l.remove(index);
        }
        l.add(element);
    }

    /**
        Adds the elements contained in <b>aList</b>; delete the old
        element if existed
        @param destList the list to add elements to
        @param aColl a collection of the elements to be added to destList

        @aribaapi documented
    */
    public static void appendAndRemoveIfPresent (List destList,
                                                 Collection aColl)
    {
        if (aColl == null) {
            return;
        }

        Iterator i = aColl.iterator();
        while (i.hasNext()) {
            appendAndRemoveIfPresent(destList, i.next());
        }
    }


    /**
        Checks if the list contains the element <b>element</b> using
        the <b>==</b> operator.

        @param l the list to search
        @param element the element to search for; the <b>==</b>
        operator is used for comparison

        @return <b>true</b> if the List contains <b>element</b>,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static <T> boolean containsIdentical (List<T> l, T element)
    {
        return (indexOfIdentical(l, element) != -1);
    }


    /**
        Finds the location of an element in the List using the
        <b>==</b> operator for comparison.

        @param element the element to search for. The comparison is
        performed using <b>==</b> with each element.
        @param l the list to search

        @return the index of <b>element</b> in the List.  Returns
        <b>-1</b> if the element is not present.
        @aribaapi public
    */
    public static <T> int indexOfIdentical (List<T> l, T element)
    {
        if (l instanceof Vector) {
                //This is because BaseVector has a different
                //implementation
            return ((Vector)l).indexOfIdentical(element);
        }
        int location = -1;
        Iterator<T> i = l.iterator();
        while (i.hasNext()) {
            location++;
            if (element == i.next()) {
                return location;
            }
        }
        return -1;
    }

    /**
        Finds the last location of an element in the List using the
        <b>==</b> operator for comparison.

        @param element the element to search for. The comparison is
        performed using <b>==</b> with each element.
        @param l the list to search
        @param startingAt the starting element in the list to search
        back from
        @return the index of <b>element</b> in the List.  Returns
        <b>-1</b> if the element is not present.
        @aribaapi public
    */

    public static <T> int lastIndexOfIdentical (List<T> l,
                                                T element,
                                                int startingAt)
    {
        for (int i=startingAt; i>=0; i--) {
            if (l.get(i) == element) {
                return i;
            }
        }
        return -1;
    }
    /**
        Finds the last location of an element in the List using the
        <b>==</b> operator for comparison.

        @param element the element to search for. The comparison is
        performed using <b>==</b> with each element.
        @param l the list to search

        @return the index of <b>element</b> in the List.  Returns
        <b>-1</b> if the element is not present.
        @aribaapi public
    */
    public static <T>int lastIndexOfIdentical (List<T> l, T element)
    {
        return lastIndexOfIdentical(l, element, l.size()-1);
    }

    /**
        Finds the location of an element in the List using the
        <b>==</b> operator for comparison.

        @param element the element to search for. The comparison is
        performed using <b>==</b> with each element.
        @param l the list to search

        @return the index of <b>element</b> in the List.  Returns
        <b>-1</b> if the element is not present.
        @aribaapi public
    */
    public static <T> boolean removeElementIdentical (List<T> l, T element)
    {
        int i = indexOfIdentical(l, element);
        if (i < 0) {
            return false;
        }

        l.remove(i);
        return true;
    }

    /**
        Returns the List's first element without modification of the
        List.
        @param l the list to look in
        @return the first element, or null if there are no elements.
        @aribaapi public
    */
    public static <T> T firstElement (List<T> l)
    {
        if (l.isEmpty()) {
            return null;
        }
        return l.get(0);
    }

    /**
        Returns the List's last element without modification of the
        List.
        @param l the list to look in
        @return the last element, or null if there are no elements.
        @aribaapi public
    */
    public static <T> T lastElement (List<T> l)
    {
        if (l.isEmpty()) {
            return null;
        }
        return l.get(l.size()-1);
    }


    /**
        Inserts <b>element</b> after <b>existingElement</b> in the
        List.

        @param l the list to insert into
        @param element the element to insert into the list
        @param existingElement the element to insert after. If
        <b>existingElement</b> is <b>null</b> or cannot be found, this
        method does nothing

        @return <b>true</b> if <b>element</b> was inserted and
        <b>false</b> if it was not.

        @exception NullPointerException if <b>element</b> is <b>null</b>.

        @aribaapi documented
    */
    public static <T> boolean insertElementAfter (List<T> l,
                                                  T element,
                                                  T existingElement)
    {
        int offset = l.indexOf(existingElement);
        if (offset == -1) {
            return false;
        }
        l.add(offset+1, element);
        return true;
    }

    /**
        Inserts <b>element</b> before <b>existingElement</b> in the
        List.

        @param l the list to insert into
        @param element the element to insert into the list
        @param existingElement the element to insert before. If
        <b>existingElement</b> is <b>null</b> or cannot be found, this
        method does nothing

        @return <b>true</b> if <b>element</b> was inserted and
        <b>false</b> if it was not.

        @exception NullPointerException if <b>element</b> is <b>null</b>.

        @aribaapi documented
    */
    public static <T> boolean insertElementBefore (List<T> l,
                                                   T element,
                                                   T existingElement)
    {
        int offset = l.indexOf(existingElement);
        if (offset == -1) {
            return false;
        }
        l.add(offset, element);
        return true;
    }



    /**
        Sorts the List's contents. String comparisons are case
        sensitive.

        @param l the List to sort
        @param ascending if <b>true</b> the List will be sorted in
        ascending order, if <b>false</b> in descending order

        @exception ClassCastException if the
        List's contents are not all Strings, or are not Comparable.
        @aribaapi documented
    */
    public static void sortStrings (List<String> l, boolean ascending)
    {
        sortStrings(l, ascending, false);
    }


    /**
        Sorts the List's contents, presuming all elements are of
        type <b>String</b>.

        @param l the List to sort
        @param ascending if <b>true</b> the List will be sorted in
        ascending order, if <b>false</b> in descending order
        @param ignoreCase if <b>true</b> the List's Strings will be
        sorted in a case insensitive manner, if <b>false</b> in a case
        sensitive manner

        @exception ClassCastException if the List's contents are not
        all Strings.
        @aribaapi documented
    */
    public static void sortStrings (List<String> l, boolean ascending, boolean ignoreCase)
    {
        String s[] = new String[l.size()];
        l.toArray(s);

        Sort.objects(s,
                     null,
                     null,
                     0,
                     l.size(),
                     ignoreCase ? StringCompareIgnoreCase.self : StringCompare.self,
                     ascending ? Sort.SortAscending : Sort.SortDescending);
        l.clear();
        l.addAll(Arrays.asList(s));
    }

    /**
        Copies the List's elements into <b>anArray</b> at a
        specified offset. This array must be large enough to contain
        the elements.

        @param l the List to copy into

        @param anArray the array to copy the List's elements
        into. Any elements existing in <b>anArray</b> beyond the count
        of elements copied from this List are left unchanged.

        @param startingAt the location in <b>anArray</b> to begin
        copying into

        @aribaapi documented
    */
    public static void copyInto (List l, Object anArray[], int startingAt)
    {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            anArray[startingAt++]=i.next();
        }
    }

    /**
        Copies <code>length</code> elements of the list <code>from</code> to the list
        <code>to</code> starting at the specified <code>fromIndex</code>. <p/>

        Regardless of how big <code>length</code> actually is, we stop copying
        when we reach the end of <code>from</code>. The actual number of elements
        copied is returned. <p/>

        @param from the <code>List</code> to copy from
        @param to the <code>List</code> to copy to
        @param fromIndex the index of the first element in <code>from</code>
               to copy
        @param length the number of elements of <code>from</code> to copy
        @return the number of elements copied
        @aribaapi ariba
    */
    public static int copyInto (List from, List to, int fromIndex, int length)
    {
        if (length < 0) {
            Assert.that(false, "Invalid negative length: %s",
                        Constants.getInteger(length));
        }
        int fencePost = Math.min(from.size(), fromIndex + length);
        for (int i=fromIndex; i < fencePost; ++i) {
            to.add(from.get(i));
        }
        return fencePost - fromIndex;
    }


    /**
        Helper function to create a new List containing one Object.

        @param object the Object to add to the List

        @return a new List containing the specified <b>object</b>
        @aribaapi documented
    */
    public static <T> List<T> list (T object)
    {
        List<T> l = list(1);
        l.add(object);
        return l;
    }

    /**
        Helper function to create a new List containing two Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static <T> List<T> list (T a, T b)
    {
        List<T> l = list(2);
        l.add(a);
        l.add(b);
        return l;
    }

    /**
        Helper function to create a new List containing three
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c)
    {
        List l = list(3);
        l.add(a);
        l.add(b);
        l.add(c);
        return l;
    }

    /**
        Helper function to create a new List containing four
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d)
    {
        List l = list(4);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        return l;
    }
    /**
        Helper function to create a new List containing five
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e)
    {
        List l = list(5);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(e);
        return l;
    }

    /**
        Helper function to create a new List containing six Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e, Object f)
    {
        List l = list(6);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(e);
        l.add(f);
        return l;
    }

    /**
        Helper function to create a new List containing seven
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e, Object f, Object g)
    {
        List l = list(7);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(e);
        l.add(f);
        l.add(g);
        return l;
    }

    /**
        Helper function to create a new List containing eight
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List
        @param h the eighth Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e, Object f, Object g, Object h)
    {
        List l = list(8);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(e);
        l.add(f);
        l.add(g);
        l.add(h);
        return l;
    }

    /**
        Helper function to create a new List containing nine
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List
        @param h the eighth Object to add to the List
        @param i the ninth Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e, Object f, Object g, Object h,
                                 Object i)
    {
        List l = list(9);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(e);
        l.add(f);
        l.add(g);
        l.add(h);
        l.add(i);
        return l;
    }

    /**
        Helper function to create a new List containing ten Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List
        @param h the eighth Object to add to the List
        @param i the ninth Object to add to the List
        @param j the tenth Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e, Object f, Object g, Object h,
                                 Object i, Object j)
    {
        List l = list(10);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(e);
        l.add(f);
        l.add(g);
        l.add(h);
        l.add(i);
        l.add(j);
        return l;
    }

    /**
        Helper function to create a new List containing eleven
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List
        @param h the eighth Object to add to the List
        @param i the ninth Object to add to the List
        @param j the tenth Object to add to the List
        @param k the eleventh Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e, Object f, Object g, Object h,
                                 Object i, Object j, Object k)
    {
        List l = list(11);
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(e);
        l.add(f);
        l.add(g);
        l.add(h);
        l.add(i);
        l.add(j);
        l.add(k);
        return l;
    }

    /**
        Helper function to create a new List containing twelve
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List
        @param h the eighth Object to add to the List
        @param i the ninth Object to add to the List
        @param j the tenth Object to add to the List
        @param k the eleventh Object to add to the List
        @param l the twelfth Object to add to the List

        @return a new List containing the specified objects
        @aribaapi documented
    */
    public static List list (Object a, Object b, Object c, Object d,
                                 Object e, Object f, Object g, Object h,
                                 Object i, Object j, Object k, Object l)
    {
        List list = list(12);
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(d);
        list.add(e);
        list.add(f);
        list.add(g);
        list.add(h);
        list.add(i);
        list.add(j);
        list.add(k);
        list.add(l);
        return list;
    }

    /**
        Null safe method to check the length of a List.

        @param list a list to check the size of. <b>null</b> is an
        acceptable value for this argument.

        @return the length of the <B>list</B>, or zero if it is
        null.
        @aribaapi documented
    */
    public static int getListSize (List<?> list)
    {
        if (list != null) {
            return list.size();
        }
        return 0;
    }

    /**
        Determine if a List is null or empty.

        @param list a List object to check

        @return <b>true</b> if <B>list</B> is null or empty,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean nullOrEmptyList (List<?> list)
    {
        return (list == null) || list.isEmpty();
    }

    /**
        Removes and returns the element at index 0, or <b>null</b> if the
        List is empty.
        @param l the List object to process
        @return the first element that was in the list, but has now
        been removed.
        @aribaapi documented
    */
    public static <T> T removeFirstElement (List<T> l)
    {
        if (l.isEmpty()) {
            return null;
        }

        return l.remove(0);
    }

    /**
        Removes and returns the element at the end (size-1), or
        <b>null</b> if the List is empty.
        @param l the List object to process
        @return the last element that was in the list, but has now
        been removed.
        @aribaapi documented
    */
    public static <T> T removeLastElement (List<T> l)
    {
        if (l.isEmpty()) {
            return null;
        }

        return l.remove(l.size()-1);
    }

    /**
        Removes all occurrences of <b>element</b> from the Vector.
        @param l the list to remove from
        @param o the element to remove.
        @aribaapi documented
    */
    public static <T> void removeAll (List<T> l, T o)
    {
        if (l instanceof Vector) {
            ((Vector)l).removeAll(o);
        }
        else {
            l.removeAll(list(o));
        }
    }

    /**
        Removes all elements from the two lists <B>v1</B> and <B>v2</B>
        which are contained in both. If a particular element appears N times
        in <B>v1</B> and M times in <B>v2</B>, it will be deleted min(N, M)
        times from both lists.

        @param v1 the first List
        @param v2 the second List
        @aribaapi documented
    */
    public static void removeEqualElements (List v1, List v2)
    {
        List dups = equalElements(v1, v2);
        for (int i=0; i<dups.size(); i++) {
            Object o = dups.get(i);
            while (v1.contains(o) && v2.contains(o)) {
                v1.remove(o);
                v2.remove(o);
            }
        }
    }

    /**
        Determine all elements common between two Lists.

        @param l1 the first List
        @param l2 the second List

        @return a list of all elements common to <b>v1</b> and
        <b>v2</b>.The comparison is made using List.contains().
        @aribaapi documented
    */
    public static List equalElements (List l1, List l2)
    {
        HashSet s1 = new HashSet(l1);
        HashSet s2 = new HashSet(l2);
        s1.retainAll(s2);
        return Arrays.asList(s1.toArray());
    }

    /**
        Generate a comma separated list of strings from a List.

        @param l a List of objects to put into the string
        @return a comma separated list of the strings in <b>l</b>
        @aribaapi documented
    */
    public static String listToCSVString (List l)
    {
        return StringUtil.fastJoin(l,
                                   ResourceService.getString(StringTable, ListSeparator));
    }


    /**
        Generate a string from a list, separating the elements with
        the specified string.  Include a space on the separator if you
        want spaces after the separator literal.

        @param l a List of objects to put into the string
        @param separator a String that should separate each element of l
        @return a String containing elements of l separated by separator
        @aribaapi documented
    */
    public static String listToString (List l, String separator)
    {
        return StringUtil.fastJoin(l, separator);
    }

    /**
        Determine if the two lists are equal.
        <b>Note:</b> this is different from List.equals() method because
        this one is recursive.

        @param v1 the first List
        @param v2 the second List

        @return <b>true</b> if the two lists are equal, <b>false</b>
        otherwise
        @aribaapi documented
    */
    public static boolean listEquals (List v1, List v2)
    {
        if (v1 == v2) {
            return true;
        }
        if (v1 == null || v2 == null || v1.size() != v2.size()) {
            return false;
        }
        Iterator i1 = v1.iterator();
        Iterator i2 = v2.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            if (!SystemUtil.objectEquals(i1.next(), i2.next())) {
                return false;
            }
        }
        if (i1.hasNext() || i2.hasNext()) {
            Log.util.warning(7801);
                //This should never happen
                //since we already checked that they have the same size
            return false;
        }
        return true;
    }

    /**
        Return a new list with the keys reversed.
        @param list the list to reverse
        @return a new list with the keys in the opposite order

        @aribaapi documented
    */
    public static <T> List<T> reverse (List<T> list)
    {
        List newList = list(list.size());
        for (int i = list.size()-1; i>=0; i--) {
            newList.add(list.get(i));
        }
        return newList;
    }

    /**
        Replaces the element <b>o</b> with <b>n</b>.

        @param list the list to search
        @param o which element to replace
        @param n the element to replace the existing element with

        @return <b>o</b> if it was replaced or <b>null</b>
        if it was not found.

        @exception NullPointerException if <b>n</b> is
        <b>null</b>
        @aribaapi public
    */
    public static Object replace (List list, Object o, Object n)
    {
        int index = list.indexOf(o);
        if (index != -1) {
            return list.set(index, n);
        }
        return null;
    }

    public static final Class ImmutableListClass =
        Collections.unmodifiableList(list()).getClass();

    /**
        Returns a version of the list that is not modifiable
        If l is null, null will be returned.
        @aribaapi ariba
    */
    public static <T> List<T> immutableList (List<? extends T> l)
    {
            // Do this check to support general routines that protect
            // whatever list they are returning, such as return
            // List.immutableList(getFoo()); where the value being
            // passed may be null
        if (l == null) {
            return null;
        }

        if (l.getClass() == ImmutableListClass) {
            return (List<T>)l;
        }
        else {
            return Collections.unmodifiableList(l);
        }
    }

    /**
        Writes the List's data out to the given stream.

        @serialData calls writeObject() on each List element, in order
        @param l the list to write from
        @param output the stream to write the object to
        @exception IOException Includes any I/O exceptions that may occur
        @aribaapi private
    */
    public static void writeExternal (List l, ObjectOutput output) throws IOException
    {
        if (l instanceof Vector) {
            ((Vector)l).writeExternal(output);
        }
        else {
            output.writeInt(l.size());
            for (int i = 0; i < l.size(); i++) {
                output.writeObject(l.get(i));
            }
        }
    }

    /**
        Reads object data from the given input and restores the contents of the given List.

        @param input the stream to read data from in order to restore the object
        @param l the list to read into
        @exception IOException if I/O errors occur
        @exception ClassNotFoundException If the class for an object being
                     restored cannot be found.
        @aribaapi private
    */
    public static void readExternal (List l, ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        if (l instanceof Vector) {
            ((Vector)l).readExternal(input);
        }
        else {
            int numItems = input.readInt();
            for (int i = 0; i < numItems; i++) {
                l.add(input.readObject());
            }
        }
    }

    /**
        Returns true if list <code>subList</code> is the subList of
        list <code>l</code>.

        <pre>
        For example, if list l is A[1..n], list subList is B[1..m], l contains
        the whole list subList if and only if
            A1, A2, A3, A4, ..., Am+2, ... An
                    B1, B2, ..., Bm
        such that A[3] == B[1], A[4] == B[2], ... , A[m+2] = B[m]
        </pre>

        @param subList the sub list
        @param l the containing list
        @return <code>true</code> if l contains all the elements of subList
                consecutively

        @aribaapi ariba
    */
    public static <T> boolean isSubList (List<T> subList, List<T> l)
    {
        Assert.that(l != null && subList != null,
                "List %s or %s is null", l, subList);
        if (l.size() < subList.size()) {
            return false;
        }
            // Note we consider any non null list contains empty list
        else if (subList.isEmpty()) {
            return true;
        }

        int i = 0;
        final int lSize = l.size();
        final int subListSize = subList.size();
        while (i <= (lSize -subListSize)) {
            int p = i;
            int q = 0;
            while (l.get(p).equals(subList.get(q))) {
                p++;
                q++;
                if (q >= subListSize) {
                    return true;
                }
            }
            i++;
        }

        return false;
    }

    /**
        Returns true if the 2 input lists has the same size and contain
        the same elements, even though the elements may be stored at
        possibly a different order in the 2 lists.
        @param l1 one list
        @param l2 the other list
        @return <code>true</code> if the content of the two lists is identical
                without necessary using the same order

        @aribaapi ariba
    */
    public static <T> boolean hasSameElements (List<T> l1, List<T> l2)
    {
        /*
            Todo: implementation is inconsitent with JavaDoc...
            ariba.util.core.ListUtil.hasSameElements(
                ariba.util.core.ListUtil.list('a', 'a'),
                ariba.util.core.ListUtil.list('a'))
            returns true. The size is not the same.
        */
        if (l1 == null || l2 == null) {
            return l1 == l2;
        }
        HashSet<T> s1 = new HashSet<T>(l1);
        HashSet<T> s2 = new HashSet<T>(l2);
        return s1.equals(s2);
    }

    /**
        Returns values in one list which are not present in the other
        @param l1 one list
        @param l2 the other list
        @return values in one list (<code>l1</code> which are not present in
                the other

        @aribaapi ariba
    */
    public static <T> List<T> minus (List<T> l1, List<T> l2)
    {
        Assert.that((l1 != null) && (l2 != null), "l1 or l2 should not be null");
        List<T> minus = ListUtil.list();
        for (int i = 0, sz = l1.size(); i < sz; i++) {
            T val = l1.get(i);
            if (!l2.contains(val)) {
                minus.add(val);
            }
        }
        return minus;
    }

    /**
        Returns a new instance of {@link ListUtil.Comparator} with the
        specified <code>elementComparator</code>. <p/>

        @param elementComparator the element <code>Comparator</code> the
               <code>ListUtil.Comparator</code> will use
        @return the new comparator
        @aribaapi ariba
    */
    public static ListUtil.Comparator createComparator (
            java.util.Comparator elementComparator
    )
    {
        return new Comparator(elementComparator);
    }

    /**
        Compares the elements of <code>first</code> and <code>second</code>
        and returns a negative, zero or positive integer as <code>first</code> is
        less than, equal to or greater than <code>second</code>, respectively.
        <p/>

        Everything else being equal, longer lists are greater than shorter lists.
        <p/>
        @param first the first <code>List</code>; may not be <code>null</code>
        @param second the second <code>List</code>; may not be <code>null</code>
        @param elementComparator the <code>Comparator</code> to be used
               when comparing elements of the <code>Lists</code>; may be
               <code>null</code> in which case the elements are assumed to
               be <code>Comparables</code>
        @return the result of the comparison
        @throws NullPointerException if either of <code>first</code> or
                <code>second</code> is <code>null</code>
        @aribaapi ariba
    */
    public static int compare (
            List first,
            List second,
            java.util.Comparator elementComparator
    )
    {
        int firstSize = first.size();
        int secondSize = second.size();
        int size = Math.min(firstSize, secondSize);
        for (int i=0; i<size; ++i)
        {
            Object o1 = first.get(i);
            Object o2 = second.get(i);
            int result = (elementComparator != null)
                    ? SystemUtil.compare(o1, o2, elementComparator)
                    : SystemUtil.compare((Comparable)o1, (Comparable)o2);
            if (result != 0) {
                return result;
            }
        }
        return firstSize - secondSize;
    }

    /**
        A simple wrapper to convert a List to an Enumeration.
        NOTE: the Enumeration returned is not thread safe.
        @param list the List to convert to an Enumeration
        @return an enumeration wrapped around the list
        @aribaapi ariba
    */
    public static <T> Enumeration<T> listToEnumeration (List<T> list)
    {
        if (list == null) {
            return  null;
        }

        return new WrappedListEnumeration(list);
    }

    public static List diff (List change, List baseline)
    {
        List delta = ListUtil.list();
        Iterator items = change.iterator();
        while (items.hasNext()) {
            Object item = items.next();
            if (!baseline.contains(item)) {
                delta.add(item);
            }
        }
        return delta;
    }
  
    public static List[] getChunkedLists (List list, int maxChunkSize)
    {
        int totalCount = (list != null ? list.size() : 0);
        if (maxChunkSize < 1) {
            maxChunkSize = totalCount;
        }
        int chunkCount = (totalCount + maxChunkSize - 1) / maxChunkSize;
        List[] results = new List[chunkCount];
        if (chunkCount == 1) {
            results[0] = list;
        }
        else {
            for (int i = 0; i < chunkCount; i++) {
                int startIdx = i * maxChunkSize;
                int endIdx = startIdx + maxChunkSize;
                if (endIdx > totalCount) {
                    endIdx = totalCount;
                }
                results[i] = list.subList(startIdx, endIdx);
            }
        }
        return results;
    }

    public static int size (Collection list)
    {
        return list != null ? list.size() : 0;
    }

    public static <T> List<T> concatenate (List<T>... lists)
    {
        int size = 0;
        boolean needNewList = false;
        for (int i = 0; i < lists.length; i++) {
            List<T> list = lists[i];
            int s = size(list);
            size =+ s;
            if (s > 0 && i > 0) {
                needNewList = true;
            }
        }
        List<T> result;
        if (!needNewList) {
            result = lists.length > 0 ? lists[0] : null;
        }
        else {
            result = ListUtil.list(size);
            for (List<T> list : lists) {
                if (list != null) {
                    result.addAll(list);
                }
            }
        }
        return result != null ? result : ListUtil.<T>list();
    }

}

/**
    This Enumeration is not thread safe.
*/
class WrappedListEnumeration<T> implements Enumeration<T>
{
    public final List<T> backing;
    private int index = 0;
    public WrappedListEnumeration (List<T> l)
    {
        backing = l;
    }
    public boolean hasMoreElements ()
    {
        return index < backing.size();
    }
    public T nextElement ()
    {
        if (index >= backing.size()) {
            throw new NoSuchElementException("Trying to enumerate past the end.");
        }
        return backing.get(index++);
    }
}

