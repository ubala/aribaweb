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

    $Id: //ariba/platform/util/core/ariba/util/core/OrderedHashtable.java#13 $
*/

package ariba.util.core;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
    <p>An OrderedHashtable is just like a normal Hashtable except that it
    remembers the order the keys were added to the table and, whenever keys()
    or elements() is called, they are returned in that order.</p>

    <p>Just overrides all the appropriate methods in Hashtable.<br/>
    Adds only one method reverseKeys(), which is the same as keys(), but the
    Enumeration is in the reverse order.</p>

    <p><b>Note:</b>: Because this an ordered collection, its performances maybe
    not be as good as an ordinary Hashtable. Use it only when the order of entry
    matters</p>

    @aribaapi documented
*/
public class OrderedHashtable extends Hashtable
{
        // List of keys that stores ordering information. Note that this
        // vector is guaranteed not to contain duplicate keys.
    private List order = ListUtil.list();

    public OrderedHashtable ()
    {
        super(2);
    }

    public Object put (Object key, Object value)
    {
        ListUtil.addElementIfAbsent(order, key);
        return super.put(key, value);
    }

    public void clear ()
    {
        super.clear();
        order.clear();
    }

    public Object remove (Object key)
    {
        order.remove(key);
        return super.remove(key);
    }

    public int removeElement (Object element)
    {
        List removedKeys = keysForRemovedElement(element);
        int numRemoved = (removedKeys == null) ? 0 :
            removedKeys.size();
        if (numRemoved != 0) {
            for (int i=0; i<numRemoved; i++) {
                if (!order.remove(removedKeys.get(i))) {
                    Assert.that(false, 
                                "expecting vector to contain element %s " +
                                "it does not!", removedKeys.get(i));
                }
            }
        }
        return numRemoved;
    }

    public Enumeration keys ()
    {
        return MapUtil.iteratorToEnumeration(
            Collections.unmodifiableCollection(order).iterator());
    }

    public Enumeration elements ()
    {
        return new OrderedHashtableEnumeration(this, keys());
    }
    /**
        Creates a shallow copy of the OrderedHashtable. The table
        itself is cloned, but none of the keys or elements are copied.

        @return the cloned copy

        @throws InternalError if the cloning operation fails.

        @aribaapi documented
    */
    public Object clone ()
    {
        OrderedHashtable newTable = (OrderedHashtable)super.clone();
        newTable.keySet = null;
        newTable.values = null;
        newTable.order = ListUtil.cloneList(order);
        return newTable;
    }
    
    private transient Set keySet = null;
    private transient Collection values = null;

    /**
        Returns a Set view of the keys contained in this map.  The Set is
        backed by the map, so changes to the map are reflected in the Set,
        and vice-versa.  (If the map is modified while an iteration over
        the Set is in progress, the results of the iteration are undefined.)
        The Set supports element removal, which removes the corresponding entry
        from the map, via the Iterator.remove, Set.remove,  removeAll
        retainAll, and clear operations.  It does not support the add or
        addAll operations.<p>

        @return a Set view of the keys contained in this map.
        @aribaapi documented
    */
    public Set keySet ()
    {
        if (keySet == null) {
            keySet = new AbstractSet()
            {
                public Iterator iterator ()
                {
                    return MapUtil.enumerationToIterator(keys());
                }
                public int size ()
                {
                    return count;
                }
                public boolean contains (Object o)
                {
                    return containsKey(o);
                }
                public boolean remove (Object o)
                {
                    int oldSize = count;
                    OrderedHashtable.this.remove(o);
                    return count != oldSize;
                }
                public void clear ()
                {
                    OrderedHashtable.this.clear();
                }
            };
        }
        return keySet;
    }

    /**
        Returns a collection view of the values contained in this map.  The
        collection is backed by the map, so changes to the map are reflected in
        the collection, and vice-versa.  (If the map is modified while an
        iteration over the collection is in progress, the results of the
        iteration are undefined.)  The collection supports element removal,
        which removes the corresponding entry from the map, via the
        <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
        <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
        It does not support the <tt>add</tt> or <tt>addAll</tt> operations.<p>

        @return a collection view of the values contained in this map.
        @aribaapi documented
    */
    public Collection values ()
    {
        if (values == null) {
            values = new AbstractCollection ()
            {
                public Iterator iterator ()
                {
                    return MapUtil.enumerationToIterator(elements());
                }
                public int size ()
                {
                    return count;
                }
                public boolean contains (Object o)
                {
                    return containsValue(o);
                }
                public void clear ()
                {
                    OrderedHashtable.this.clear();
                }
            };
        }
        return values;
    }

    
    public Enumeration reverseKeys ()
    {
        List reverseKeys = ListUtil.reverse(order);
        return MapUtil.iteratorToEnumeration(reverseKeys.iterator());
    }

    public void insertElementAt (Object key, Object value, int index)
    {
        int prevIndex = order.indexOf(key);
        if (prevIndex > -1) {
            order.remove(key);
            if (prevIndex < index) {
                index--;
            }
        }
        order.add(index, key);
        super.put(key, value);
    }

    /**
        Set the element which is the key/value pair in this OrderedHashtable
        at a given index.

        If the key (say A) corresponding to the given index (say B) is not the same as
        the input key argument, the key/value pair corrsponding to A is not removed
        from the hashtable. Instead the following takes place:

        (1) the key/value pair corresponding to B is updated in
        hashtable (that could mean a new key/value pair is added to the hashtable or that
        the value in B in the hashtable is replaced with the new value).

        (2) the key/value pair corresponding to B will be updated to be at position as
        specified by index. If key exists before but at a different order, this method
        will put key at position index, with the position of the other elements updated
        to fill in the original slot.

        @param key  the key to be set
        @param value the value associated with key
        @param index the order of this key-value pair
    */
    public void setElementAt (Object key, Object value, int index)
    {
            // remove the one and only one key element (if exists)
            // from the vector.
        order.remove(key);
            // now adds it to the specified index. However, since
            // we removed an entry before, we need to be careful
            // if the entry to be set is the very last one.
        if (index == order.size()) {
            order.add(key);
        }
        else {
            order.add(index, key);
        }
            // overwrites the entry in the hashtable
        super.put(key, value);
    }

    public Object removeElementAt (int index)
    {
        Object key = order.remove(index);
        return super.remove(key);
    }

    public int indexOfKey (Object key)
    {
        return order.indexOf(key);
    }

}

class OrderedHashtableEnumeration implements Enumeration
{
    private Enumeration keys;
    private Hashtable values;

    public OrderedHashtableEnumeration (Hashtable values, Enumeration keys)
    {
        this.keys = keys;
        this.values = values;
    }

    public boolean hasMoreElements ()
    {
        return keys.hasMoreElements();
    }

    public Object nextElement ()
    {
        if (hasMoreElements()) {
            return values.get(keys.nextElement());
        }
        else {
            return null;
        }
    }
}
