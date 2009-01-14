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

    $Id: //ariba/platform/util/core/ariba/util/core/MapUtil.java#22 $
*/

package ariba.util.core;


import ariba.util.io.DeserializationException;
import ariba.util.io.Deserializer;
import ariba.util.io.Serializer;
import ariba.util.log.Log;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Collection;

/**
    Map Utilities. These are helper functions for dealing with
    maps.

    @aribaapi documented
*/
public final class MapUtil
{

    /* prevent people from creating this class */
    private MapUtil ()
    {
    }


    //-------------------------------------------------------------
    //----- UTILITY METHODS TO RETRIEVE THE DEFAULT MAP IMPL. -----
    //-------------------------------------------------------------

    /**
        Constructs an  empty Map. The Map will grow on demand
        as more elements are added.
        @return new empty Map
        @aribaapi documented
    */
    public static <K,V> Map<K,V> map ()
    {
        return new HashMap<K,V>(); // OK
    }

    /**
        Helper method to clone a map
        @param m the map to clone
        @return a new map of the same type

        @aribaapi documented
    */
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> cloneMap (Map<K,V> m)
    {
        return (Map<K,V>)ClassUtil.clone(m);
    }

    /**
        Constructs a Map capable of holding a specified number
        of elements. <p/>

        To construct a type-safe <code>Map</code>:<ul>
        <li><code>Map&lt;K,V> typesafe = MapUtil.map()</code>
        </ul>
        For a raw <code>Map</code>:<ul>
        <li><code>Map raw = MapUtil.map()</code>
        </ul>
        There will be no compile warnings either way.

        @param initialCapacity the number of elements this Map
        is capable of holding before needing to grow.
        @return new empty Map with given initial capacity
        @aribaapi documented
    */
    public static <K,V> Map<K,V> map (int initialCapacity)
    {
        return new HashMap<K,V>(initialCapacity); // OK
    }


    /**
        Creates new Map with the same contents as the given Map.

        To construct a type-safe <code>Map</code>:<ul>
        <li><code>Map&lt;K,V> typesafe = MapUtil.map()</code>
        </ul>
        For a raw <code>Map</code>:<ul>
        <li><code>Map raw = MapUtil.map()</code>
        </ul>
        There will be no compile warnings either way.

        @param source source Map
        @return new Map with same contents as <code>source</code>
        @aribaapi documented
    */
    public static <K,V> Map<K,V> map (Map<? extends K, ? extends V> source)
    {
        return new HashMap<K,V>(source); // OK
    }

    //----------------------------------------------------
    //----- UTILITY METHODS TO RETRIEVE A SORTED MAP -----
    //----------------------------------------------------

    /**
        Creates an empty SortedMap
        @return new empty SortedMap implementation
        @see java.util.SortedMap
        @aribaapi documented
    */
    public static <K,V> SortedMap<K,V> sortedMap ()
    {
        return new TreeMap<K,V>();
    }

    /**
        Creates an empty SortedMap
        @param comparator the comparator that will be used to sort this map
        @return new empty SortedMap implementation
        @see java.util.SortedMap
        @aribaapi documented
    */
    public static <K,V> SortedMap<K,V> sortedMap (Comparator<K> comparator)
    {
        return new TreeMap<K,V>(comparator);
    }

    /**
        Creates a new SortedMap with the same content as the given SortedMap
        @param source the sorted map whose mappings are to be placed in this map
        @return new map containing the same mappings as the given SortedMap
        @see java.util.SortedMap
        @aribaapi documented
    */
    public static <K,V> SortedMap<K,V> sortedMap (
        SortedMap<? extends K, ? extends V> source)
    {
        return new TreeMap<K,V>(source);
    }


    /**
        Returns a List containing the Map's keys. If the Map
        contains no keys, return an empty List object.

        @param ht the table to return keys from
        @return the List object mentioned above.
        @aribaapi documented
    */
    public static <T> List<T> keysList (Map<? extends T, ?> ht)
    {
        return ListUtil.<T>arrayToList(keysArray(ht));
    }



    /**
        Returns a List containing the Map's elements. If the map
        containes no elements, an empty List object is returned.
        @param ht the Map whose elements are to be returned in the
        returned list.
        @return the List object mentioned above.
        @aribaapi documented
    */
    public static <T> List<T> elementsList (Map<?,? extends T> ht)
    {
        return ListUtil.<T>arrayToList(elementsArray(ht));
    }


    /**
        Returns an Object array containing the Map's keys. If the
        Map contains no keys, an empty array is returned.
        @param ht the table to return keys from
        @return the Object array above mentioned.
        @aribaapi documented
    */
    public static <T> T[] keysArray (Map<? extends T,?> ht)
    {
        return ht.keySet().toArray((T[])new Object[0]);
    }

    /**
        Returns an Object array containing the Map's elements.
        @return the Object array mentioned above. If there the map contains
        no elements, an empty array is returned.
        @param ht the table to return elements from
        @aribaapi documented
    */
    public static <T> T[] elementsArray (Map<?,? extends T> ht)
    {
        Collection<? extends T> values = ht.values();
        return values.toArray((T[])new Object[0]);
    }


    /**
        Finds and removes all occurrences of <b>element</b> from the Map.
        Note that this method is slow -- O(n) -- because it must scan the table
        searching for the element.
        @param ht the Map to modify
        @param element the element to search for.  Map compares elements
        using <b>equals()</b>.

        @return the number of elements removed.
        @aribaapi documented
    */
    public static int removeElement (Map ht, Object element)
    {
        int startingSize = ht.size();
        ht.values().removeAll(ListUtil.list(element));
        return (startingSize-ht.size());
    }

    /**
        Returns whether all list elements are strings
    */
    private static boolean allElementsAreStrings (List<?> v)
    {
        for (int i = v.size()-1; i >= 0; i--) {
            if (!(v.get(i) instanceof String)) {
                return false;
            }
        }
        return true;
    }

    /**
        Determine if a Map is null or empty.

        @param map a map object to check

        @return <b>true</b> if <B>map</B> is null or empty,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean nullOrEmptyMap (Map<?,?> map)
    {
        return (map == null) || map.isEmpty();
    }

    /**
        Create a Map of Maps from a List. Creates a new
        map where each key is an element from the List
        <B>v</B>. Each element of the new map is itself an empty
        map.

        @param keys a List of items to become keys in created
        Map

        @return a new Map containing each element of keys.
        @aribaapi documented
    */
    public static Map convertListToMap (List keys)
    {
        Map ht = map(keys.size());
        for (int i=0, s=keys.size(); i<s; i++) {
            ht.put(keys.get(i), map());
        }
        return ht;
    }

    /**
        Merges two Maps together. The source map has precedence. <p/>

        The allowed keys and values in <code>source</code> and <code>dest</code>
        are (essentially) <code>Strings</code>, <code>Lists</code> and
        <code>Maps</code>.  After the merge <code>dest</code> can best
        be described as "a union of the entries in itself and in
        <code>source</code>."
        That is any entries in <code>source</code> but not in
        <code>dest</code> will be copied into <code>dest</code>.
        For entries that are in both <code>source</code> and <code>dest</code>: <ul>
        <li> if both entries are <code>Strings</code>, <code>source</code>
             overwrites <code>dest</code>
        <li> if both entries are <code>Lists</code> they are unioned
        <li> if both entries are <code>Maps</code> they are
             {@link #mergeMapIntoMap merged} (hence this method is recursive)
        </ul>
        If the entries in <code>source</code> and <code>dest</code> are not
        the same type, things start to get complicated and are intuitive about
        half the time. For instance if <code>dest</code> contains a
        <code>List</code> and <code>source</code> a <code>String</code>, the
        <code>String</code> in <code>source</code> gets added. (However,
        the opposite situation is not-at-all intuitive.) <p/>

        <b>Important note:</b> the only map modified in this method is
        <code>dest</code>. No sub-maps of <code>dest</code> (or indeed any
        maps of <code>source</code>) are modified. <p/>

        @param source the map to merge into <code>dest</code>
        @param dest the map into which <code>source</code> is to be merged
        @return the destination map
        @aribaapi private
    */
    public static Map mergeMapIntoMap (Map dest, Map source)
    {
        return mergeMapIntoMap(dest, source, false);
    }

    public static Map mergeMapIntoMap (Map dest, Map source,
                                       boolean appendMergeListProperties)
    {

        Iterator i = source.keySet().iterator();

        while (i.hasNext()) {
            Object key = i.next();
            Object sourceValue = source.get(key);
            Object destValue = dest.get(key);
            if (destValue == null) {
                dest.put(key, ListUtil.copyValue(sourceValue));
                continue;
            }
            else if ((destValue instanceof Map) &&
                     (sourceValue instanceof Map))
            {
                dest.put(key,
                         mergeMapIntoMap(
                             copyMap((Map)destValue),
                             (Map)sourceValue));
            }
            else if ((destValue instanceof Map) &&
                     (sourceValue instanceof List))
            {
                if (allElementsAreStrings((List)sourceValue)) {
                    dest.put(key,
                             mergeMapIntoMap(
                                 copyMap((Map)destValue),
                                 convertListToMap(
                                     (List)sourceValue)));
                }
                else {
                    List sourceVect = ListUtil.copyList((List)sourceValue);
                    if (appendMergeListProperties) {
                        ListUtil.appendAndRemoveIfPresent(sourceVect, destValue);
                    }
                    else {
                        ListUtil.addElementIfAbsent(sourceVect, destValue);
                    }
                    dest.put(key, sourceVect);
                }
            }
            else if ((destValue instanceof List) &&
                     (sourceValue instanceof Map))
            {
                if (allElementsAreStrings((List)destValue)) {
                    dest.put(key,
                             mergeMapIntoMap(
                                 convertListToMap((List)destValue),
                                 (Map)sourceValue));
                }
                else {
                    if (appendMergeListProperties) {
                        ListUtil.appendAndRemoveIfPresent(((List)destValue),
                                                     copyMap((Map)sourceValue));
                    }
                    else {
                        ListUtil.addElementIfAbsent(((List)destValue),
                                                    copyMap((Map)sourceValue));
                    }
                }
            }
            else if ((destValue instanceof Map) &&
                     (sourceValue instanceof String))
            {
                Map destValueMap =
                    copyMap((Map)destValue);
                if (destValueMap.get(sourceValue) == null) {
                    ((Map)destValue).put(sourceValue, map());
                }
            }
            else if ((destValue instanceof String) &&
                     (sourceValue instanceof Map))
            {
                Map sourceHash = copyMap((Map)sourceValue);
                if (sourceHash.get(destValue) == null) {
                    sourceHash.put(destValue, map());
                }
                dest.put(key, sourceHash);
            }
            else if ((destValue instanceof List) &&
                     (sourceValue instanceof List))
            {
                if (appendMergeListProperties) {
                    ListUtil.appendAndRemoveIfPresent((List)destValue,
                                                  ListUtil.copyList((List)sourceValue));
                }
                else {
                    ListUtil.addElementsIfAbsent((List)destValue,
                                                 ListUtil.copyList((List)sourceValue));
                }
            }
            else if ((destValue instanceof List) &&
                     (sourceValue instanceof String))
            {
                if (appendMergeListProperties) {
                    ListUtil.appendAndRemoveIfPresent((List)destValue, sourceValue);
                }
                else {
                    ListUtil.addElementIfAbsent((List)destValue, sourceValue);
                }
            }
            else if ((destValue instanceof String) &&
                     (sourceValue instanceof List))
            {
                List sourceVect = ListUtil.copyList((List)sourceValue);
                if (appendMergeListProperties) {
                    ListUtil.appendAndRemoveIfPresent(sourceVect, destValue);
                }
                else {
                    ListUtil.addElementIfAbsent(sourceVect, destValue);
                }
                dest.put(key, sourceVect);
            }
            else if ((destValue instanceof String) &&
                     (sourceValue instanceof String))
            {
                dest.put(key, sourceValue);
            }
        }
        return dest;
    }

    /**
        Merges two Maps together. The source map has precedence. <p/>

        See mergeMapIntoMap. The only addition is that this method attempts to handle
        non-[String,List,Map] objects by overriding objects of one class with objects of the same class.

        For non-String objects, no complex operation (such as adding scalar objects to vectors) are attempted.

        @param source the map to merge into <code>dest</code>
        @param dest the map into which <code>source</code> is to be merged
        @return the destination map
        @aribaapi private
    */
    public static Map mergeMapIntoMapWithObjects (Map dest, Map source)
    {
        return mergeMapIntoMapWithObjects(dest, source, false);
    }

    public static Map mergeMapIntoMapWithObjects (Map dest, Map source, boolean overwriteMismatchedClasses)
    {
        Iterator i = source.keySet().iterator();

        while (i.hasNext()) {
            Object key = i.next();
            Object sourceValue = source.get(key);
            Object destValue = dest.get(key);
            if (destValue == null) {
                dest.put(key, ListUtil.copyValue(sourceValue));
                continue;
            }
            else if ((destValue instanceof Map) &&
                     (sourceValue instanceof Map))
            {
                dest.put(key,
                         mergeMapIntoMapWithObjects(
                             copyMap((Map)destValue),
                             (Map)sourceValue, overwriteMismatchedClasses));
            }
            else if ((destValue instanceof Map) &&
                     (sourceValue instanceof List))
            {
                if (allElementsAreStrings((List)sourceValue)) {
                    dest.put(key,
                             mergeMapIntoMapWithObjects(
                                 copyMap((Map)destValue),
                                 convertListToMap(
                                     (List)sourceValue), overwriteMismatchedClasses));
                }
                else {
                    List sourceVect = ListUtil.copyList((List)sourceValue);
                    ListUtil.addElementIfAbsent(sourceVect, destValue);
                    dest.put(key, sourceVect);
                }
            }
            else if ((destValue instanceof List) &&
                     (sourceValue instanceof Map))
            {
                if (allElementsAreStrings((List)destValue)) {
                    dest.put(key,
                             mergeMapIntoMapWithObjects(
                                 convertListToMap((List)destValue),
                                 (Map)sourceValue, overwriteMismatchedClasses));
                }
                else {
                    ListUtil.addElementIfAbsent(((List)destValue),
                        copyMap((Map)sourceValue));
                }
            }
            else if ((destValue instanceof Map) &&
                     (sourceValue instanceof String))
            {
                Map destValueMap =
                    copyMap((Map)destValue);
                if (destValueMap.get(sourceValue) == null) {
                    ((Map)destValue).put(sourceValue, map());
                }
            }
            else if ((destValue instanceof String) &&
                     (sourceValue instanceof Map))
            {
                Map sourceHash = copyMap((Map)sourceValue);
                if (sourceHash.get(destValue) == null) {
                    sourceHash.put(destValue, map());
                }
                dest.put(key, sourceHash);
            }
            else if ((destValue instanceof List) &&
                     (sourceValue instanceof List))
            {
                // change related to CR 1-46QA3J
                // we want to overlay the source value onto the dest value
                // and not make a merge, which actually keeps items deleted
                // by the Customer via ABA
                dest.put(key, sourceValue);
            }
            else if ((destValue instanceof List) &&
                     (sourceValue instanceof String))
            {
                ListUtil.addElementIfAbsent((List)destValue, sourceValue);
            }
            else if ((destValue instanceof String) &&
                     (sourceValue instanceof List))
            {
                List sourceVect = ListUtil.copyList((List)sourceValue);
                ListUtil.addElementIfAbsent(sourceVect, destValue);
                dest.put(key, sourceVect);
            }
            else if ((destValue instanceof String) &&
                     (sourceValue instanceof String))
            {
                dest.put(key, sourceValue);
            }
            else if (overwriteMismatchedClasses) {
                dest.put(key, sourceValue);
            }
            else {
                Class destClass = (destValue == null) ? null : destValue.getClass();
                Class sourceClass = (sourceValue == null) ? null : sourceValue.getClass();

                if (destClass == sourceClass) {
                    dest.put(key, sourceValue);
                }
            }
        }
        return dest;
    }

    /**
        Overlays <code>source</code> onto <code>dest</code>. <p/>

        This method has similar behavior to {@link #mergeMapIntoMap} with the
        exception that no merges happen--<code>source</code> is simply written
        on top of <code>dest</code>. That is, every entry in <code>source</code>
        but not in <code>dest</code> is copied into <code>dest</code>.
        For every entry that is in both <code>source</code> and <code>dest</code>
        the <code>source</code> value overwrites the <code>dest</code> value.
        <p/>
        So, in essence this method is alot simpler than {@link #mergeMapIntoMap}.
        <p/>

        @param source the map to merge into <code>dest</code>
        @param dest the map into which <code>source</code> is to be merged
        @return <code>dest</code>
        @aribaapi ariba
    */
    public static Map overlayMapOntoMap (Map dest, Map source)
    {
        for (Iterator iter=source.keySet().iterator(); iter.hasNext(); ) {
            Object key = iter.next();
            Object valueCopy = ListUtil.copyValue(source.get(key));
            dest.put(key, valueCopy);
        }
        return dest;
    }

    /**
        Overwrites the contents of a given Map with the contents
        of another Map.

        @param overriding  the overriding Map
        @param overridden  the Map to be overridden

        @return a new Map that results from the overwriting
        process.

        @aribaapi private
    */
    public static Map overwriteMap (Map overriding,
                                    Map overridden)
    {
            // first make a copy of the one to be overridden
        Map newMap = copyMap(overridden);
        overwriteMapInPlace(overriding, newMap);
        return newMap;
    }

    /**
        A version of overwriteMap that sideeffects the overriden
        Map directly, instead of making a copy to return.

        @aribaapi private
    */
    public static void overwriteMapInPlace (Map overriding,
                                            Map overridden)
    {
        Iterator i = overriding.keySet().iterator();
        while (i.hasNext()) {
            Object key = i.next();
            Object overridingValue = overriding.get(key);
            Object overriddenValue = overridden.get(key);
            if (overridingValue instanceof Map &&
                overriddenValue instanceof Map) {
                    // recurse
                overwriteMapInPlace((Map)overridingValue,
                                    (Map)overriddenValue);
            }
            else {
                    // overwrite the one in overridden
                overridden.put(key, overridingValue);
            }
        }
    }

    /**
        Applies a single deletion or list of  deletions to a map.  This
        is used to implement the deleteProperty AML element.  deletes can be
        either a single string or a list of Objects.

        Returns false if it had any undeletable deletes.

        @aribaapi private
    */
    public static boolean performDeletesOnMap (Object deletes, Map table)
    {
        if (deletes instanceof String) {
            return table.remove(deletes) != null;
        }
        else {
            Assert.that(deletes instanceof List,
                        "Strange type of object to try to delete: %s",
                        deletes);
            List deletesList = (List)deletes;
            boolean returnValue = true;
            for (int i = deletesList.size()-1; i >= 0; i--) {
                if (table.remove(deletesList.get(i)) == null) {
                    returnValue = false;
                }
            }
            return returnValue;
        }
    }

    /**
        Copy a Map.  This method will copy the source map and output
        a destination map.
        For each list or hastable within the source map they will also be
        copied,all other types will be shared.  So for a map of lists or
        maps, this will recurse.

        @param source the source map to copy.
        @return the destination map.
        @aribaapi documented
    */
    public static Map copyMap (Map source)
    {
        Map destination = map(source.size());
        Iterator i = source.keySet().iterator();
        while (i.hasNext()) {
            Object key = i.next();
            Object value = source.get(key);
            destination.put(key, ListUtil.copyValue(value));
        }
        return destination;
    }

    /**
        Determine if the two maps are equal.
        <b>Note:</b> this is different from Map.equals() method because
        this one is recursive.

        @param h1 the first Map
        @param h2 the second Map

        @return <b>true</b> if the two maps are equal, <b>false</b>
        otherwise
        @aribaapi documented
    */
    public static boolean mapEquals (Map h1, Map h2)
    {
        if (h1 == h2) {
            return true;
        }
        if (h1 == null || h2 == null || h1.size() != h2.size()) {
            return false;
        }
        for (Iterator i = h1.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            Object val2 = h2.get(key);
            Object val1 = h1.get(key);
            if (! SystemUtil.objectEquals(val1, val2)) {
                return false;
            }
        }
        return true;
    }

    /**
        Convenience generic method that merges <code>key</code> and
        <code>value</code> into the supplied <code>map</code>. <p/>

        The semantics are, add <code>value</code> to the list of
        <code>values</code> associated with <code>key</code> in
        <code>map</code>. <p/>

        @param map the map to merge into
        @param key the key
        @param value the value

        @aribaapi ariba
    */
    public static <K,V> void merge (Map<K,List<V>> map, K key, V value)
    {
        List<V> list = map.get(key);
        if (list == null) {
            list = ListUtil.list();
            map.put(key, list);
        }
        list.add(value);
    }

    /**
        Merges <code>key</code> and the collection of <code>values</code>
        into <code>map</code>, by finding the collection of
        values already in <code>map</code> and adding <code>values</code>
        to it. <p/>

        @param map the map to merge into
        @param key the key
        @param values the collection of values
        @param factory the factory to use to create new collections if needed

        @aribaapi ariba
    */
    public static <K,V,C extends Collection<V>> void merge (
            Map<K,C> map,
            K key,
            Collection<? extends V> values,
            Factory<C> factory
    )
    {
        C collection = map.get(key);
        if (collection == null) {
            collection = factory.create();
            map.put(key, collection);
        }
        collection.addAll(values);

    }

    /**
        Convenience generic method that merges <code>key</code> and
        <code>value</code> into the supplied <code>map</code>. <p/>

        The semantics are, add <code>value</code> to the list of
        <code>values</code> associated with <code>key</code> in
        <code>map</code>. <p/>

        @param map the map to merge into
        @param key the key
        @param values the values

        @aribaapi ariba
    */
    public static <K,V> void merge (
            Map<K,List<V>> map,
            K key,
            Collection<? extends V> values
    )
    {
        merge(map, key, values, Factory.<V>listFactory());
    }

    /**
        Returns a string serialization of the Hashtable using the
        Serializer.

        @see Serializer

        @return string serialization of this Hashtable
        @aribaapi private
    */
    public static String toSerializedString (Map m)
    {
        StringWriter writer = new StringWriter();
        Serializer serializer = new Serializer(writer);

        try {
            serializer.writeObject(m);
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
    public static void fromSerializedString (Map m, String serialized)
    {
        StringReader reader = new StringReader(serialized);

        try {
            new Deserializer(reader).readObject(m);
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

    private static final int MaxStringLen = 254;

    /**
        Converts a string into a string array for persistence purposes.

        @return string array serialization of this Hashtable
        @aribaapi private
    */
    public static String[] toStringArray (Map m)
    {
            //        fullString - the original full string
            // first convert to serialized string
        String fullString = toSerializedString(m);

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
        Search a list for a map.
        Will only search this list, and not sub lists.

        @param l1 the list
        @param h1 the Map

        @return element location if the map is found, -1 otherwise
        @aribaapi documented
    */
    public static int indexOfMapInList (List l1, Map h1)
    {
        for (int i = 0; i < l1.size(); i++) {
            if ((l1.get(i) instanceof Map) &&
                (mapEquals((Map)l1.get(i), h1))) {
                return i;
            }
        }
        return -1;
    }

    /**
        A simple wrapper to convert an Iterator to an Enumeration.
        @param i the Iterator to convert to an Enumeration
        @return an enumeration wrapped around the iterator
        @aribaapi documented
    */
    public static Enumeration iteratorToEnumeration (Iterator i)
    {
        if (i == null) {
            return  null;
        }
        if (i instanceof WrappedIterator) {
            return ((WrappedIterator)i).backing;
        }
        return new WrappedEnumeration(i);
    }


    /**
        A simple wrapper to convert an Enumeration to an Iterator.
        The returned iterator may not support the remove() operation.

        @param e the Enumeration to convert to an Iterator
        @return an iterator wrapped around the enumeration

        @aribaapi documented
    */
    public static Iterator enumerationToIterator (Enumeration e)
    {
        if (e == null) {
            return null;
        }
        if (e instanceof WrappedEnumeration) {
            return ((WrappedEnumeration)e).backing;
        }
        return new WrappedIterator(e);
    }

    public static final Class ImmutableMapClass =
        Collections.unmodifiableMap(map()).getClass();

    /**
        Test a map to see if it is immutable.

        @param m The Map to be tested
        @return True if the Map m is immutable, False otherwise.
    */
    public static boolean isImmutable (Map m)
    {
        return m.getClass() == ImmutableMapClass;
    }

    /**
        Returns a version of the map that is not modifiable
        If m is null, null will be returned.
        @aribaapi ariba
    */
    public static Map immutableMap (Map m)
    {
            // Do this check to support general routines that protect
            // whatever map they are returning, such as return
            // Map.immutableMap(getFoo()); where the value being
            // passed may be null
        if (m == null) {
            return null;
        }
        return isImmutable(m) ? m : Collections.unmodifiableMap(m);
    }
}

class WrappedEnumeration implements Enumeration
{
    public final Iterator backing;
    public WrappedEnumeration (Iterator i)
    {
        backing = i;
    }
    public boolean hasMoreElements ()
    {
        return backing.hasNext();
    }
    public Object nextElement ()
    {
        return backing.next();
    }
}

class WrappedIterator implements Iterator
{
    public final Enumeration backing;
    public WrappedIterator (Enumeration e)
    {
        backing = e;
    }
    public boolean hasNext ()
    {
        return backing.hasMoreElements();
    }
    public Object next ()
    {
        return backing.nextElement();
    }
    public void remove ()
    {
        throw new UnsupportedOperationException();
    }
}

