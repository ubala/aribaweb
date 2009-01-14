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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMap_AribaHashtable.java#11 $
*/

package ariba.ui.aribaweb.util;

import java.util.Map;

/**
    AWMap implementation based on java.util.Map.

    @aribaapi private
*/
public final class AWMap_AribaHashtable extends AWMap
{
    // ** Thread Safety Considerations: no state here -- no locking required.

    /**
        Creates a new AWMap_AribaHashtable object.

        @aribaapi private
    */
    public AWMap_AribaHashtable ()
    {
        super();
    }

    /**
        Removes all mappings from the given map.

        @aribaapi private

        @param receiver map implementation
    */
    public void clear (Object receiver)
    {
        ((Map)receiver).clear();
    }

    /**
        Returns <code>true</code> if the given map contains a mapping for the
        specified key.

        @aribaapi private

        @param receiver map implementation
        @param key key whose presence in map is to be tested
    */
    public boolean containsKey (Object receiver, Object key)
    {
        return ((Map)receiver).containsKey(key);
    }

    /**
        Returns true if the given map maps one or more keys to the specified value.
        More formally, returns true if and only if this map contains at least
        one mapping to a value v such that (value==null ? v==null :
        value.equals(v)).

        @aribaapi private

        @param receiver map implementation
        @param value value whose presence in map is to be tested
        @return true if map maps one or more keys to the specified value
    */
    public boolean containsValue (Object receiver, Object value)
    {
        return ((Map)receiver).values().contains(value);
    }

    /**
        Returns the value to which the given map maps the specified key.  Returns
        <code>null</code> if the map contains no mapping for this key.

        @aribaapi private

        @param receiver map implementation
        @param key key whose associated value is to be returned
        @return value to which map maps the specified key, or null if the map
            contains no mapping for this key
    */
    public Object get (Object receiver, Object key)
    {
        return ((Map)receiver).get(key);
    }

    /**
        Returns true if the map contains no key-value mappings.

        @aribaapi private

        @param receiver map implementation
        @return true if map contains no key-value mappings
    */
    public boolean isEmpty (Object receiver)
    {
        return ((Map)receiver).isEmpty();
    }

    /**
        Returns an enumeration of the keys in the map.

        @aribaapi private

        @param receiver map implementation
        @return enumeration of the keys in the map
    */
    public java.util.Iterator keys (Object receiver)
    {
        return ((Map)receiver).keySet().iterator();
    }

    /**
        Associates the specified value with the specified key in the map.  If
        the map previously contained a mapping for this key, the old value is
        replaced.

        @aribaapi private

        @param receiver map implementation
        @param key key with which the specified value is to be associated
        @param value value to be associated with the specified key
        @return previous value associated with specified key, or
            <code>null</code> if there was no mapping for key
    */
    public Object put (Object receiver, Object key, Object value)
    {
        return ((Map)receiver).put(key, value);
    }

    /**
        Removes the mapping for this key from this map if present.

        @aribaapi private

        @param receiver map implementation
        @param key key whose mapping is to be removed from the map
        @return previous value associated with specified key, or
            <code>null</code> if there was no mapping for key
    */
    public Object remove (Object receiver, Object key)
    {
        return ((Map)receiver).remove(key);
    }

    /**
        Returns the number of key-value mappings in the map.

        @aribaapi private

        @param receiver map implementation
        @return number of key-value mappings in map
    */
    public int size (Object receiver)
    {
        return ((Map)receiver).size();
    }

    /**
        Returns an enumeration of the values contained in the map.

        @aribaapi private

        @param receiver map implementation
        @return an enumeration of the values contained in the map
    */
    public java.util.Iterator values (Object receiver)
    {
        return ((Map)receiver).values().iterator();
    }
}
