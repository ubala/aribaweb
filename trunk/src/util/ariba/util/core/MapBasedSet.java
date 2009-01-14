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

    $Id: //ariba/platform/util/core/ariba/util/core/MapBasedSet.java#2 $
*/

package ariba.util.core;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
    A generic implementation of the Set built on top of a Map.

    This implementation allows building of a GrowOnlyHashSet on top of a
    GrowOnlyHashTable.

    This code is almost an exact copy of java.util.TreeSet which builds on
    top of java.util.TreeMap

    @aribaapi private
*/

public abstract class MapBasedSet extends AbstractSet
        implements Cloneable
{
    private transient Map _map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    /**
        Constructs a set backed by the specified map.
    */
    protected MapBasedSet (Map m)
    {
        _map = m;
    }

    public Iterator iterator ()
    {
        return _map.keySet().iterator();
    }

    public int size ()
    {
        return _map.size();
    }

    public boolean isEmpty ()
    {
        return _map.isEmpty();
    }

    public boolean contains (Object o)
    {
        return _map.containsKey(o);
    }

    public boolean add (Object o)
    {
        return _map.put(o, PRESENT) == null;
    }

    public boolean remove (Object o)
    {
        return _map.remove(o) == PRESENT;
    }

    public void clear ()
    {
        _map.clear();
    }

    /**
        Returns a shallow copy of this <tt>TreeSet</tt> instance. (The elements
        themselves are not cloned.)
      
        @return a shallow copy of this set.
    */
    public Object clone ()
    {
        MapBasedSet clone = null;
        try {
            clone = (MapBasedSet)super.clone();
        } 
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }

        clone._map = MapUtil.cloneMap(_map);

        return clone;
    }

}
