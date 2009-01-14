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

    $Id: //ariba/platform/util/core/ariba/util/core/MultiValueHashtable.java#8 $
*/

package ariba.util.core;

import java.util.Iterator;
import java.util.Map;
import java.util.List;

/**
    A map like class that can store multiple values for the
    keys.
    @aribaapi ariba
*/
public class MultiValueHashtable
{
    private final Map entries = MapUtil.map();

    /**
        Get the vector of entries for the specified key.
        <code>null</code> will be returned if no keys have been set.
    */
    public List getList (Object key)
    {
        if (key == null) {
            throw new NullPointerException();
        }
        return (List)entries.get(key);
    }

    /**
        Get the vector of entries for the specified key. If no keys
        have been set a vector will be created and returned.
    */
    public List getOrCreateList (Object key)
    {
        List result = getList(key);
        if (result == null) {
            result = ListUtil.list();
            entries.put(key, result);
        }
        return result;
    }

    /**
        Returns the last element set for a specific key.
    */
    public Object getLastValue (Object key)
    {
        List values = getList(key);
        if (values == null) {
            return null;
        }
        return ListUtil.lastElement(values);
    }

    /**
        Returns the first element set for a specific key.
    */
    public Object getFirstValue (Object key)
    {
        List values = getList(key);
        if (values == null) {
            return null;
        }
        return ListUtil.firstElement(values);
    }

    /**
        Add a value to the list of values for the specified key.
    */
    public void put (Object key, Object value)
    {
        List values = getOrCreateList(key);
        if (value == null) {
            throw new NullPointerException();
        }
        values.add(value);
    }
    /**
        Removes all keys and elements from the MultiValueHashtable.
    */
    public void clear ()
    {
        entries.clear();
    }

    public String toString ()
    {
        return entries.toString();
    }

    public Iterator keys ()
    {
        return entries.keySet().iterator();
    }

    public Iterator values ()
    {
        Iterator[] enums = new Iterator[entries.size()];
        Iterator e = entries.values().iterator();
        for (int i = 0; e.hasNext(); i++) {
            List v = (List)e.next();
            enums[i] = v.iterator();
        }
        return new IteratorUnion(enums);
    }

    /**
        @return the number of values in this MultiValueHashtable, which
        may be greater than the number of keys.
    */
    public int size ()
    {
        int total = 0;
        Iterator e = entries.values().iterator();
        while (e.hasNext()) {
            List v = (List)e.next();
            total += v.size();
        }
        return total;
    }
}
