/*
    Copyright (c) 1996-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/ReadOnlyMap.java#7 $

    Responsible: jshultis
*/

package ariba.util.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import ariba.util.io.FormattingSerializer;


/**
 * A Map-like object that can't be modified - not because it is immutable
 * (which would raise a run-time exception), but because it has no API
 * for modifying it. In a better world, it could be defined as a superinterface
 * of Map.
 *
 * A ReadOnlyMap wraps an ordinary Map, which may contain other maps as values
 * (i.e., the Map may be nested). In that case, when returning a value that
 * is a Map, the ReadOnlyMap wraps that value in another ReadOnlyMap, so that
 * IT can't be modified, either. The wrapping doesn't extend to values of
 * other types, unfortunately, but it is at least a good start toward getting
 * a declarative, statically-enforced, read-only data structure.
 *
 * @aribaapi ariba
*/

public class ReadOnlyMap <K,V>
{
    Map<K,V> map = null;

    public ReadOnlyMap (Map<K,V> aMap)
    {
        map = MapUtil.copyMap(aMap);
    }

    public static ReadOnlyMap emptyInstance ()
    {
        return new ReadOnlyMap(MapUtil.map());
    }

    // XXX JCS: Create ReadOnlyList to protect those values, too.
    public V get (K key)
    {
        V value = null;
        if (map != null) {
            value = map.get(key);
            if (value != null && value instanceof Map) {
                value = (V)new ReadOnlyMap((Map)value);
            }
        }
        return value;
    }

    public boolean containsKey (K key)
    {
        return map != null && map.containsKey(key);
    }

    public boolean containsValue (V value)
    {
        return map != null && map.containsValue(value);
    }

    public boolean equals (Object o)
    {
        return o != null &&
               o.getClass().equals(this.getClass()) &&
               ((ReadOnlyMap)o).map.equals(map);
    }

    public boolean isEmpty ()
    {
        return map == null || map.isEmpty();
    }

    public static <K,V> boolean isNullOrEmpty (ReadOnlyMap<K,V> m)
    {
        return (m == null) || (m.map == null) || m.map.isEmpty();
    }

    public int size ()
    {
        return map == null ? 0 : map.size();
    }

    public Set<K> keySet ()
    {
        Set<K> keys = null;
        if (map == null) {
            keys = SetUtil.set();
        }
        else {
            keys = map.keySet();
        }
        return keys;
    }

    public Collection<V> values ()
    {
        Collection<V> vals = null;
        if (map == null) {
            vals = ListUtil.list();
        }
        else {
            vals = map.values();
        }
        return vals;
    }

    public int hashCode ()
    {
        return map == null ? 0 : map.hashCode();
    }

    public Set<Entry<K, V>> entrySet ()
    {
        Set<Entry<K, V>> myEntries = SetUtil.set(map.size());
        for (java.util.Map.Entry<K, V> entry : map.entrySet()) {
            Entry<K, V> myEntry = new Entry<K, V>(entry);
            myEntries.add(myEntry);
        }
        return myEntries;
    }

    public static final char Dot = '.';

    /**
     * Fetch a value from a nested map using a path of keys using
     * dot notation.
     * @param path The dotted path of key values
     * @return The value at that path. If the value is a Map, return a
     * ReadOnlyMap, so the result is equivalent to doing a sequence of
     * get operations on the path elements.
     */
    public Object getPath (String path)
    {
        Object value = map;
        String[] segments = StringUtil.delimitedStringToArray(path, Dot);
        for (int i=0; i<segments.length; i++) {
            if (value instanceof Map) {
                value = ((Map)value).get(segments[i]);
            }
            else {
                return null;
            }
        }
        return (value instanceof Map) ? new ReadOnlyMap((Map)value) : value;
    }

    public String toString ()
    {
        return map == null ? null : FormattingSerializer.serializeObject(map);
    }

    /**
     * Get a copy of the underlying Map. It's not the actual
     * underlying Map, so is can't be used as a 'back door' to
     * modify the state of the ReadOnlyMap.
     * The static method {@link #mapCopy(ReadOnlyMap)} is
     * recommended unless the caller is certain that both <code>this</code>
     * and the <code>Map</code> it encloses are non-null.
     * @return A copy of the wrapped Map.
     */
    public Map<K,V> mapCopy ()
    {
        return MapUtil.copyMap(map);
    }

    /**
     * Get a copy of the underlying Map. It's not the actual
     * underlying Map, so is can't be used as a 'back door' to
     * modify the state of the ReadOnlyMap. If
     * @param roMap The ReadOnlyMap to copy. Return null if roMap is
     * null, or its enclosed Map is null or empty.
     * @return A copy of the wrapped Map.
     */
    public static Map mapCopy (ReadOnlyMap roMap)
    {
        return (roMap == null) ? null : roMap.mapCopy();
    }

    public Map<K,V> immutableMap ()
    {
        return MapUtil.immutableMap(map);
    }

    public static Map immutableMap (ReadOnlyMap roMap)
    {
        return (roMap == null || roMap.map == null) ? null :
                MapUtil.immutableMap(roMap.map);
    }

    public class Entry <K,V>
    {
        private Map.Entry<K,V> entry;

        Entry (Map.Entry<K,V> anEntry)
        {
            entry = anEntry;
        }

        public boolean equals (Object anObject)
        {
            return entry.equals(anObject);
        }

        public int hashCode ()
        {
            return entry.hashCode();
        }

        public K getKey ()
        {
            return entry.getKey();
        }

        public Object getValue ()
        {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = new ReadOnlyMap((Map)value);
            }
            return value;
        }

    }

}

