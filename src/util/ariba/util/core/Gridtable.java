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

    $Id: //ariba/platform/util/core/ariba/util/core/Gridtable.java#13 $
*/

package ariba.util.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    A two-dimensional map. That is, a collection of non-null
    objects indexed by 2 keys.

    @aribaapi private
*/
public class Gridtable
{
    public static final String ClassName = "ariba.util.core.Gridtable";

    private Map primaryTable = MapUtil.map();
    private List emptyList = ListUtil.list();
    private int size = 0;
    protected Compare primaryKeyCompare = null;
    protected Compare secondaryKeyCompare = null;
    

    /**
        Squirrel away a default compare object for sorting the primary keys.
    */
    public void setPrimaryKeyCompare (Compare c)
    {
        primaryKeyCompare = c;
    }


    /**
        Squirrel away a default compare object for sorting the secondary keys.
    */
    public void setSecondaryKeyCompare (Compare c)
    {
        secondaryKeyCompare = c;
    }


    /**
        Store the specified element indexed by <key1, key2>.
        @return the element previously associated with the keys if any,
        <b>null</b> otherwise.
        @exception NullPointerException if <b>element</b> or
        <b>key1</b> or <b>key2</b> are null
    */
    public Object put (Object key1, Object key2, Object element)
    {
        if (key1 == null || key2 == null || element == null) {
            throw new NullPointerException();
        }
        Map secondaryTable = (Map)primaryTable.get(key1);
        if (secondaryTable == null) {
            secondaryTable = makeMap();
            primaryTable.put(key1, secondaryTable);
        }
        Object oldElement = secondaryTable.put(key2, element);
        if (oldElement == null) {
            size++;
        }
        Map optimized = optimize(secondaryTable);
        if (optimized != secondaryTable) {
            primaryTable.put(key1, optimized);
        }
        return oldElement;
    }

    protected Map makeMap ()
    {
        return MapUtil.map();
    }

    protected Map optimize (Map m)
    {
        return m;
    }


    /**
        Return the element indexed by <key1, key2>.
        @exception NullPointerException if <b>key1</b> or
        <b>key2</b> are null
    */
    public Object get (Object key1, Object key2)
    {
        if (key1 == null || key2 == null) {
            throw new NullPointerException();
        }
        Map secondaryTable = (Map)primaryTable.get(key1);
        if (secondaryTable == null) {
            return null;
        }
        return secondaryTable.get(key2);
    }

    /**
        Remove a key from the first dimension of the grid.
        Index of <key1, ?>

        @param key1 The key to remove
        @return The removed section of the grid
        @exception NullPointerException if <b>key1</b> is null
    */
    public Map remove (Object key1)
    {
        if (key1 == null) {
            throw new NullPointerException();
        }
        Map h = (Map)primaryTable.remove(key1);
        if (h != null) {
            this.size = this.size - h.size();
        }
        return h;
    }

    /**
        Remove a key from the specified cell of the grid.
        Index of <key1, key2>

        @param key1 The first dimension of the grid
        @param key2 The second dimension of the grid
        @return The removed cell of the grid
        @exception NullPointerException if 
        <b>key1</b> or <b>key2</b> are null
    */
    public Object remove (Object key1, Object key2)
    {
        if (key1 == null || key2 == null) {
            throw new NullPointerException();
        }
        Map secondaryTable = (Map)primaryTable.get(key1);
        if (secondaryTable == null) {
            return null;
        }
        else {
            Object element = secondaryTable.remove(key2);
            if (element != null) {
                size--;
            }
            return element;
        }
    }

    /**
        Clear all elements from this table.  Analogous to
        Map.clear().
    */
    public void clear ()
    {
        primaryTable.clear();
    }

    /**
        @return the number of elements in this gridtable.
    */
    public int size ()
    {
        return this.size;
    }
    
    /** Returns the primary keys. */
    public Iterator primaryKeys ()
    {
        return primaryTable.keySet().iterator();
    }

    /**
        Returns whether the grid table contains the primary 
        key <code>key1</code>.

        @param  key1    the primary key to check against
        @return whether the grid table contains the primary 
        key <code>key1</code>
    */
    public boolean containsPrimaryKey (Object key1)
    {
        return primaryTable.containsKey(key1);
    }

    /** Returns the primary keys, in sorted order. */
    public Iterator sortedPrimaryKeys ()
    {
        return sortedPrimaryKeys(primaryKeyCompare);
    }

    /** Returns the primary keys, in sorted order. */
    public Iterator sortedPrimaryKeys (Compare c)
    {
        return sortedPrimaryKeysList(c).iterator();
    }

    public List sortedPrimaryKeysList ()
    {
        return sortedPrimaryKeysList(primaryKeyCompare);
    }

    public List sortedPrimaryKeysList (Compare c)
    {
        List result = ListUtil.list();
        Object[] keys = MapUtil.keysArray(primaryTable);

        if (keys != null) {
            if (c != null) {
                Sort.objects(keys, c);
            }
            result = ListUtil.arrayToList(keys, false);
        }

        return result;
    }

    /**
        Looks at the given list of keys to find one that matches the
        specified key, using the given compare function. Returns
        null if there are no matches. If the compare function is null,
        uses the "equals" method.
    */
    public static Object lookupKey (List keys, Object key, Compare compare)
    {
        for (int i=0, s=keys.size(); i<s; i++) {
            Object keyToCheck = keys.get(i);
            boolean equalKeys = (compare == null) ?
                key.equals(keyToCheck) :
                (compare.compare(key, keyToCheck) == 0);

            if (equalKeys) {
                return keyToCheck;
            }
        }
        return null;
    }

    /**
        Returns a vector containing the secondary keys, given a
        primary key.
    */
    public Iterator secondaryKeys (Object key1)
    {
        if (key1 == null) {
            throw new NullPointerException();
        }
        Map secondaryTable = (Map)primaryTable.get(key1);
        if (secondaryTable == null) {
            return emptyList.iterator();
        }
        return secondaryTable.keySet().iterator();
    }

    /**
        Returns an array containing the secondary keys, given a
        primary key.
    */
    public Iterator sortedSecondaryKeys (Object key1)
    {
        return sortedSecondaryKeys(key1, secondaryKeyCompare);
    }

    /**
        Returns an array containing the secondary keys, given a
        primary key.
    */
    public Iterator sortedSecondaryKeys (Object key1, Compare c)
    {
        if (key1 == null) {
            throw new NullPointerException();
        }
        Map secondaryTable = (Map)primaryTable.get(key1);
        if (secondaryTable == null) {
            return emptyList.iterator();
        }
        Object[] keys = MapUtil.keysArray(secondaryTable);
        if (c != null) {
            Sort.objects(keys, c);
        }
        List v = ListUtil.arrayToList(keys, false);
        return v.iterator();
    }

    /**
        Returns all unique objects in the Gridtable as an array.
        Note that the test for uniqueness is on the object itself, not the keys,
        and uses the comparison used by java.util.Map.put(key, object)
        on key.
    */
    public Object[] elementsArray ()
    {
        return elementsList().toArray();
    }
    
    /**
        Returns unique objects in the gridtable as a list given a primary key.
        If the primaryKey is null, throws NullPointerException.
    */
    public List secondaryElementsList (Object primaryKey)
    {
        if (primaryKey == null) {
            throw new NullPointerException();
        }

        Map secondaryTable = (Map)primaryTable.get(primaryKey);
        if (secondaryTable == null) {
            return null;
        }
        return MapUtil.elementsList(secondaryTable);
    }

    /**
        Returns all unique objects in the Gridtable as an array.
        Note that the test for uniqueness is on the object itself, not the keys,
        and uses the comparison used by java.util.Map.put(key, object)
        on key.
    */
    public List elementsList ()
    {
        List vectorOfMaps = MapUtil.elementsList(primaryTable);
        Map allElements = MapUtil.map();
        
        for (int i = 0; i < vectorOfMaps.size(); i++)
        {
            Map secondaryTable = (Map)vectorOfMaps.get(i);
            List secondaryList = MapUtil.elementsList(secondaryTable);
            for (int j = 0; j < secondaryList.size(); j++) {
                allElements.put(
                    secondaryList.get(j), secondaryList.get(j));
            }
            
        }
        return MapUtil.elementsList(allElements);
    }    
    
    /**
        Returns a new Gridtable, with the primary and secondary keys swapped.
    */
    public Gridtable pivot ()
    {
        Gridtable table = new Gridtable();
        table.setPrimaryKeyCompare(this.secondaryKeyCompare);
        table.setSecondaryKeyCompare(this.primaryKeyCompare);
        Iterator e = this.primaryKeys();
        while (e.hasNext()) {
            Object key1 = e.next();
            Iterator ee = this.secondaryKeys(key1);
            while (ee.hasNext()) {
                Object key2 = ee.next();
                Object element = this.get(key1, key2);
                table.put(key2, key1, element);
            }
        }
        return table;
    }
}



