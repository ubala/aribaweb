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

    $Id: //ariba/platform/util/core/ariba/util/core/MemoryOptimizedMap.java#4 $
*/

package ariba.util.core;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
    This is a map implementation that is very memory efficient for very small maps. It is based on the observation that for very small maps,
    the general overhead can accumulate, especially if you have a lot of them. For example, this is the memory usage for
    some ways to store 2 pointers:

    Object w/ 2 members: 16 bytes
    Object[2] : 24 bytes
    Object w member which is an Object[2]: 40 bytes

    This map implementation uses a very simple linear search, which should not be an issue for the sizes in this map. The map
    switches over to a HashMap if it becomes too large. It behaves exactly like a regular map.

    Note that 2 methods in the EntrySet are not implemented, however those should be rarely used.

    Generally one needs to be very aware of how these maps are being used, so this class needs to be very carefully applied.
    Normally if you feel like you should use this class, you probably did something overall wrong, and should redesign your
    algorithms ...

   @aribaapi private
*/
public abstract class MemoryOptimizedMap<K,V>
    implements Map<K,V>, Cloneable, Serializable
{
    private static Object EMPTY = new Object();

    private HashMap<K,V> _map;

    protected abstract int getInternalSize ();

    protected abstract Object get(int i);
    protected abstract V getValue(int i);
    protected abstract void set(int i, Object k, V v);

    public static <K,V> Map<K,V> getOptimizedMap (int size)
    {
        return getOptimizedMap(size, false);
    }

    public static <K,V> Map<K,V> getOptimizedMap (int size, boolean growOnly)
    {
        switch (size) {
            case 0 : return new MemoryOptimizedMap1<K,V>();
            case 1 : return new MemoryOptimizedMap1<K,V>();
            case 2 : return new MemoryOptimizedMap2<K,V>();
            case 3 : return new MemoryOptimizedMap3<K,V>();
            case 4 : return new MemoryOptimizedMap4<K,V>();
            case 5 : return new MemoryOptimizedMap5<K,V>();
            case 6 : return new MemoryOptimizedMap6<K,V>();
            case 7 : return new MemoryOptimizedMap7<K,V>();
            case 8 : return new MemoryOptimizedMap8<K,V>();
            default : return growOnly ? new GrowOnlyHashtable(size) : MapUtil.map(size);
        }
    }

    public MemoryOptimizedMap ()
    {
        for (int i = 0;i < getInternalSize();i++) {
            set(i, EMPTY, null);
        }
    }

    public int size ()
    {
        if (_map != null) {
            return _map.size();
        }
        int size  = 0;
        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            if (get(i) != EMPTY) {
               size++;
            }
        }
        return size;
    }

    public boolean isEmpty ()
    {
        if (_map != null) {
            return _map.isEmpty();
        }
        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            if (get(i) != EMPTY) {
               return false;
            }
        }
        return true;
    }

    public boolean containsKey (Object key)
    {
        if (_map != null) {
            return _map.containsKey(key);
        }

        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            if (SystemUtil.equal(key, get(i))) {
               return true;
            }
        }
        return false;
    }

    public boolean containsValue (Object value)
    {
        if (_map != null) {
            return _map.containsValue(value);
        }

        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            if (SystemUtil.equal(value, getValue(i)) &&
                get(i) != EMPTY) {
               return true;
            }
        }
        return false;


    }

    public V get (Object key)
    {
        if (_map != null) {
            return _map.get(key);
        }
        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            if (SystemUtil.equal(get(i), key)) {
                return getValue(i);
            }
        }
        return null;
    }

    public V put (K key, V value)
    {
        if (_map != null) {
            return _map.put(key, value);
        }
        int internalSize = getInternalSize();
        int emptySlot = -1;
        for (int i = 0;i < internalSize;i++) {
            Object o = get(i);
            if (SystemUtil.equal(o, key)) {
                V ret = getValue(i);
                set(i,key,value);
                return ret;
            }
            else if (emptySlot == -1 && o == EMPTY) {
                emptySlot = i;
            }
        }
        if (emptySlot == -1) {
            migrate(internalSize+1);
            _map.put(key,value);
        }
        else {
            set(emptySlot, key, value);
        }
        return null;

    }

    private void migrate (int size)
    {
        _map = new HashMap<K,V>(size);
        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            Object k = get(i);
            if (k != EMPTY) {
                _map.put((K)k, getValue(i));
            }
            set(i,null,null);
        }
    }

    public V remove (Object key)
    {
        if (_map != null) {
            return _map.remove(key);
        }
        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            if (SystemUtil.equal(get(i), key)) {
                V ret = getValue(i);
                set(i,EMPTY, null);
                return ret;
            }
        }
        return null;
    }

    public void putAll (Map<? extends K, ? extends V> t)
    {
        int internalSize = getInternalSize();

        if (_map == null && t.size() + size() > internalSize) {
            migrate(t.size()+size());
        }
        if (_map != null) {
            _map.putAll(t);
        }
        else {
            for (K k : t.keySet()) {
                put(k, t.get(k));
            }
        }
    }

    public void clear ()
    {
        if (_map != null) {
            _map.clear();
            _map = null;
        }

        int internalSize = getInternalSize();
        for (int i = 0;i < internalSize;i++) {
            set(i,EMPTY,null);
        }
    }

    public static <K,V> Map<K,V> optimize (Map<K,V> tab)
    {
        return optimize(tab, false);
    }

    public static <K,V> Map<K,V> optimize (Map<K,V> tab, boolean growOnly)
    {
        if (shouldOptimize(tab)) {
            Map<K,V> res = getOptimizedMap(tab.size(), growOnly);
            res.putAll(tab);
            return res;

        }
        if (tab != null &&
            growOnly &&
            !(tab instanceof GrowOnlyHashtable)) {
            Map<K,V> res = new GrowOnlyHashtable<K,V>(tab.size());
            res.putAll(tab);
            return res;
        }
        return tab;

    }

    private static boolean shouldOptimize (Map m)
    {
        if (m == null) {
            return false;
        }
        if (m instanceof MemoryOptimizedMap) {
            return ((MemoryOptimizedMap)m)._map != null ||
                   ((MemoryOptimizedMap)m).getInternalSize() != m.size();
        }
        if (m.size() > 8) {
            return false;
        }
        return true;
    }

    public static Map deepOptimize (Map tab)
    {
    try {
        return deepOptimizeInternal(tab, 0);
    }
    catch (IllegalArgumentException ex) {
        // possibly recursive map, let's leave it alone
        return tab;
    }
    }

    private static Map deepOptimizeInternal (Map tab, int level)
    {
    if (level > 20) {
        throw new IllegalArgumentException();
    }
        if (shouldOptimize(tab)) {
            Map result = MemoryOptimizedMap.getOptimizedMap(tab.size());
            for (Object key : tab.keySet()) {
                Object res = tab.get(key);
                if (res instanceof Map) {
                    result.put(key, deepOptimizeInternal((Map)res, level+1));
                }
                else {
                    result.put(key, res);
                }
            }

            return result;

        }
        return tab;
    }

    public static void optimizeInPlace (Map s)
    {
        for (Object key : s.keySet()) {
            Object val = s.get(key);
            if (val instanceof Map) {
                s.put(key, deepOptimize((Map)val));
            }
        }
    }

    public String toString ()
    {
        FastStringBuffer fsb = new FastStringBuffer();
        Object key;
        fsb.append("{");
        for (int i = 0; i < size() - 1 ; i++) {
            key = get(i);
            fsb.append(key + "=" + get(key) + ", ");
        }
        key = get(size() - 1);
        fsb.append(key + "=" + get(key) + "}");
        return fsb.toString();
    }

    /**
        Returns true iff <code>this</code> is equal to <code>that</code>
    */
    public boolean equals (Object that)
    {
        if (that == this) {
            return true;
        }
        // this also deals with the case where that == null
        if (!(that instanceof Map)) {
            return false;
        }
        Map other = (Map)that;
        if (other.size() != size()) {
            return false;
        }
        Object myKey;
        for (int i = 0; i < size(); i++) {
            myKey = get(i);
            try {
                if (!other.containsKey(myKey)) {
                    return false;
                }
            }
            catch (NullPointerException npe) {
                // the other map doesn't support null keys or values, and we have a null key
                // or value
                return false;
            }
            if (!SystemUtil.equal(get(myKey), other.get(myKey))) {
                return false;
            }
        }
        return true;
    }

    class Entry implements Map.Entry<K,V> {
        final K key;
        V value;


        /**
         * Create new entry.
         */
        Entry (K k, V v)
        {
            value = v;
            key = k;

        }

        public K getKey ()
        {
            return key;
        }

        public V getValue ()
        {
            return value;
        }

        public V setValue (V newValue)
        {
            return put(key, newValue);
        }

        public boolean equals (Object o)
        {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        public int hashCode ()
        {
            return (key==null ? 0 : key.hashCode()) ^
                   (value==null   ? 0 : value.hashCode());
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }


    private Entry getEntry (int position)
    {
        return new Entry((K)get(position), getValue(position));
    }

    private abstract class OptimizedIterator<E> implements Iterator<E> {
        int next;    // next entry to return
        int current;        // current slot

        OptimizedIterator ()
        {
            next = -1;
            current = -1;
            next = nextEntry();
        }

        public boolean hasNext ()
        {
            return next != -1;
        }

        void advance ()
        {
            current = next;
            if (next != -1) {
                next = nextEntry();
            }
            if (current == -1) {
                throw new NoSuchElementException();
            }
        }

        int nextEntry ()
        {
            int sz = getInternalSize();
            int i = current+1;
            for (;i < sz && get(i) == EMPTY; i++);
            if (i != sz) {
                return i;
            }
            else {
                return -1;
            }
        }

        public void remove ()
        {
            if (current == -1)
                throw new IllegalStateException();
            set(current, EMPTY, null);
        }

    }

    private class ValueIterator extends OptimizedIterator<V> {
        public V next ()
        {
            advance();
            return getValue(current);
        }
    }

    private class KeyIterator extends OptimizedIterator<K> {
        public K next ()
        {
            advance();
            return (K)get(current);
        }
    }

    private class EntryIterator extends OptimizedIterator<Map.Entry<K,V>> {
        public Map.Entry<K,V> next ()
        {
            advance();
            return getEntry(current);
        }
    }

    // Subclass overrides these to alter behavior of views' iterator() method
    Iterator<K> newKeyIterator ()
    {
        return new KeyIterator();
    }
    Iterator<V> newValueIterator ()
    {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K,V>> newEntryIterator ()
    {
        return new EntryIterator();
    }

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set<K> keySet() {
        return _map != null ? _map.keySet() : new KeySet();
    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator ()
        {
            return newKeyIterator();
        }
        public int size ()
        {
            return MemoryOptimizedMap.this.size();
        }
        public boolean contains (Object o)
        {
            return containsKey(o);
        }
        public boolean remove (Object o)
        {
            return MemoryOptimizedMap.this.remove(o) != null;
        }
        public void clear ()
        {
            MemoryOptimizedMap.this.clear();
        }
    }

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection<V> values ()
    {
        return _map != null ? _map.values() : new Values();
    }

    private class Values extends AbstractCollection<V>
    {
        public Iterator<V> iterator ()
        {
            return newValueIterator();
        }
        public int size ()
        {
            return MemoryOptimizedMap.this.size();
        }
        public boolean contains (Object o)
        {
            return containsValue(o);
        }
        public void clear ()
        {
            MemoryOptimizedMap.this.clear();
        }
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see Map.Entry
     */
    public Set<Map.Entry<K,V>> entrySet() {
        return _map != null ? _map.entrySet() : new EntrySet();
    }

    private class EntrySet extends AbstractSet/*<Map.Entry<K,V>>*/ {
        public Iterator/*<Map.Entry<K,V>>*/ iterator ()
        {
            return newEntryIterator();
        }
        public boolean contains (Object o)
        {
            throw new UnsupportedOperationException();
        }
        public boolean remove (Object o)
        {
            throw new UnsupportedOperationException();
        }
        public int size ()
        {
            return MemoryOptimizedMap.this.size();
        }
        public void clear ()
        {
            MemoryOptimizedMap.this.clear();
        }
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *           bucket array) is emitted (int), followed  by the
     *           <i>size</i> of the HashMap (the number of key-value
     *           mappings), followed by the key (Object) and value (Object)
     *           for each key-value mapping represented by the HashMap
     *             The key-value mappings are emitted in the order that they
     *             are returned by <tt>entrySet().iterator()</tt>.
     *
     */
    private void writeObject (java.io.ObjectOutputStream s)
        throws IOException
    {
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        // Write out number of buckets
        s.writeObject(_map);
    }

    private static final long serialVersionUID = 362498820763181666L;

    /**
     * Reconstitute the <tt>HashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject (java.io.ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the threshold, loadfactor, and any hidden stuff
        s.defaultReadObject();

        _map = (HashMap<K,V>)s.readObject();
    }

    @Override
    public Object clone ()
    {
        try {
            MemoryOptimizedMap s = (MemoryOptimizedMap)super.clone();
            if (_map != null) {
                s._map = (HashMap)_map.clone();
            }
            return s;
        }
        catch (CloneNotSupportedException e) {
            Assert.fail(e, "Unexpected");
            return null;
        }
    }
}

class MemoryOptimizedMap1<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object key;
    V value;


    protected int getInternalSize ()
    {
        return 1;
    }

    protected Object get (int i)
    {
        return key;
    }

    protected V getValue (int i)
    {
        return value;
    }

    protected void set (int i, Object k, V v)
    {
        if (i != 0) {
            throw new IllegalArgumentException();
        }
        key = k;
        value = v;
    }
}

class MemoryOptimizedMap2<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object k1,k2;
    V v1,v2;


    protected int getInternalSize ()
    {
        return 2;
    }

    protected Object get (int i)
    {
        switch (i) {
            case 0 : return k1;
            case 1 : return k2;
            default : throw new IllegalArgumentException();
        }
    }

    protected V getValue (int i)
    {
        switch (i) {
            case 0 : return v1;
            case 1 : return v2;
            default : throw new IllegalArgumentException();
        }
    }

    protected void set (int i, Object k, V v)
    {
        switch (i) {
            case 0 : k1 = k; v1 = v; return;
            case 1 : k2 = k; v2 = v; return;
            default : throw new IllegalArgumentException();
        }
    }
}

class MemoryOptimizedMap3<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object k1,k2,k3;
    V v1,v2,v3;


    protected int getInternalSize ()
    {
        return 3;
    }

    protected Object get (int i)
    {
        switch (i) {
            case 0 : return k1;
            case 1 : return k2;
            case 2 : return k3;
            default : throw new IllegalArgumentException();
        }
    }

    protected V getValue (int i)
    {
        switch (i) {
            case 0 : return v1;
            case 1 : return v2;
            case 2 : return v3;
            default : throw new IllegalArgumentException();
        }
    }

    protected void set (int i, Object k, V v)
    {
        switch (i) {
            case 0 : k1 = k; v1 = v; return;
            case 1 : k2 = k; v2 = v; return;
            case 2 : k3 = k; v3 = v; return;
            default : throw new IllegalArgumentException();
        }
    }
}

class MemoryOptimizedMap4<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object k1,k2,k3,k4;
    V v1,v2,v3,v4;


    protected int getInternalSize ()
    {
        return 4;
    }

    protected Object get (int i)
    {
        switch (i) {
            case 0 : return k1;
            case 1 : return k2;
            case 2 : return k3;
            case 3 : return k4;
            default : throw new IllegalArgumentException();
        }
    }

    protected V getValue (int i)
    {
        switch (i) {
            case 0 : return v1;
            case 1 : return v2;
            case 2 : return v3;
            case 3 : return v4;
            default : throw new IllegalArgumentException();
        }
    }

    protected void set (int i, Object k, V v)
    {
        switch (i) {
            case 0 : k1 = k; v1 = v; return;
            case 1 : k2 = k; v2 = v; return;
            case 2 : k3 = k; v3 = v; return;
            case 3 : k4 = k; v4 = v; return;
            default : throw new IllegalArgumentException();
        }
    }
}

class MemoryOptimizedMap5<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object k1,k2,k3,k4,k5;
    V v1,v2,v3,v4,v5;


    protected int getInternalSize ()
    {
        return 5;
    }

    protected Object get (int i)
    {
        switch (i) {
            case 0 : return k1;
            case 1 : return k2;
            case 2 : return k3;
            case 3 : return k4;
            case 4 : return k5;
            default : throw new IllegalArgumentException();
        }
    }

    protected V getValue (int i)
    {
        switch (i) {
            case 0 : return v1;
            case 1 : return v2;
            case 2 : return v3;
            case 3 : return v4;
            case 4 : return v5;
            default : throw new IllegalArgumentException();
        }
    }

    protected void set (int i, Object k, V v)
    {
        switch (i) {
            case 0 : k1 = k; v1 = v; return;
            case 1 : k2 = k; v2 = v; return;
            case 2 : k3 = k; v3 = v; return;
            case 3 : k4 = k; v4 = v; return;
            case 4 : k5 = k; v5 = v; return;
            default : throw new IllegalArgumentException();
        }
    }
}

class MemoryOptimizedMap6<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object k1,k2,k3,k4,k5,k6;
    V v1,v2,v3,v4,v5,v6;


    protected int getInternalSize ()
    {
        return 6;
    }

    protected Object get (int i)
    {
        switch (i) {
            case 0 : return k1;
            case 1 : return k2;
            case 2 : return k3;
            case 3 : return k4;
            case 4 : return k5;
            case 5 : return k6;
            default : throw new IllegalArgumentException();
        }
    }

    protected V getValue (int i)
    {
        switch (i) {
            case 0 : return v1;
            case 1 : return v2;
            case 2 : return v3;
            case 3 : return v4;
            case 4 : return v5;
            case 5 : return v6;
            default : throw new IllegalArgumentException();
        }
    }

    protected void set (int i, Object k, V v)
    {
        switch (i) {
            case 0 : k1 = k; v1 = v; return;
            case 1 : k2 = k; v2 = v; return;
            case 2 : k3 = k; v3 = v; return;
            case 3 : k4 = k; v4 = v; return;
            case 4 : k5 = k; v5 = v; return;
            case 5 : k6 = k; v6 = v; return;
            default : throw new IllegalArgumentException();
        }
    }
}

class MemoryOptimizedMap7<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object k1,k2,k3,k4,k5,k6,k7;
    V v1,v2,v3,v4,v5,v6,v7;


    protected int getInternalSize ()
    {
        return 7;
    }

    protected Object get (int i)
    {
        switch (i) {
            case 0 : return k1;
            case 1 : return k2;
            case 2 : return k3;
            case 3 : return k4;
            case 4 : return k5;
            case 5 : return k6;
            case 6 : return k7;
            default : throw new IllegalArgumentException();
        }
    }

    protected V getValue (int i)
    {
        switch (i) {
            case 0 : return v1;
            case 1 : return v2;
            case 2 : return v3;
            case 3 : return v4;
            case 4 : return v5;
            case 5 : return v6;
            case 6 : return v7;
            default : throw new IllegalArgumentException();
        }
    }

    protected void set (int i, Object k, V v)
    {
        switch (i) {
            case 0 : k1 = k; v1 = v; return;
            case 1 : k2 = k; v2 = v; return;
            case 2 : k3 = k; v3 = v; return;
            case 3 : k4 = k; v4 = v; return;
            case 4 : k5 = k; v5 = v; return;
            case 5 : k6 = k; v6 = v; return;
            case 6 : k7 = k; v7 = v; return;
            default : throw new IllegalArgumentException();
        }
    }
}

class MemoryOptimizedMap8<K,V>
    extends MemoryOptimizedMap<K,V>
{
    Object k1,k2,k3,k4,k5,k6,k7,k8;
    V v1,v2,v3,v4,v5,v6,v7,v8;


    protected int getInternalSize ()
    {
        return 8;
    }

    protected Object get (int i)
    {
        switch (i) {
            case 0 : return k1;
            case 1 : return k2;
            case 2 : return k3;
            case 3 : return k4;
            case 4 : return k5;
            case 5 : return k6;
            case 6 : return k7;
            case 7 : return k8;
            default : throw new IllegalArgumentException();
        }
    }

    protected V getValue (int i)
    {
        switch (i) {
            case 0 : return v1;
            case 1 : return v2;
            case 2 : return v3;
            case 3 : return v4;
            case 4 : return v5;
            case 5 : return v6;
            case 6 : return v7;
            case 7 : return v8;
            default : throw new IllegalArgumentException();
        }
    }

    protected void set (int i, Object k, V v)
    {
        switch (i) {
            case 0 : k1 = k; v1 = v; return;
            case 1 : k2 = k; v2 = v; return;
            case 2 : k3 = k; v3 = v; return;
            case 3 : k4 = k; v4 = v; return;
            case 4 : k5 = k; v5 = v; return;
            case 5 : k6 = k; v6 = v; return;
            case 6 : k7 = k; v7 = v; return;
            case 7 : k8 = k; v8 = v; return;
            default : throw new IllegalArgumentException();
        }
    }
}
