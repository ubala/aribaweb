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

    $Id: //ariba/platform/util/core/ariba/util/core/GrowOnlyGridTable.java#6 $
*/

package ariba.util.core;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
    A two-dimensional GrowOnlyHashtable. That is, a collection of
    non-null objects indexed by 2 keys.

    @aribaapi private
*/
public class GrowOnlyGridTable
{
    public static final String ClassName = new GrowOnlyGridTable().getClass().getName();

    private GrowOnlyHashtable primaryTable = new GrowOnlyHashtable();

    /**
        Store the specified element indexed by <key1, key2>. Returns
        the old value or null if there was no old value.
    */
    public Object put (Object key1, Object key2, Object element)
    {
        GrowOnlyHashtable secondaryTable = (GrowOnlyHashtable)primaryTable.get(key1);
        if (secondaryTable == null) {
            synchronized (this) {
                secondaryTable = (GrowOnlyHashtable)primaryTable.get(key1);
                if (secondaryTable == null) {
                    secondaryTable = new GrowOnlyHashtable();
                    primaryTable.put(key1, secondaryTable);
                }
            }
        }
        return secondaryTable.put(key2, element);
    }


    /**
        Return the element indexed by <key1, key2>.
    */
    public Object get (Object key1, Object key2)
    {
        GrowOnlyHashtable secondaryTable = (GrowOnlyHashtable)primaryTable.get(key1);
        if (secondaryTable == null) {
            return null;
        }
        return secondaryTable.get(key2);
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

        GrowOnlyHashtable secondaryTable 
            = (GrowOnlyHashtable)primaryTable.get(primaryKey);
        if (secondaryTable == null) {
            return null;
        }
        return MapUtil.elementsList(secondaryTable);
    }

    /**
        Returns the set of keys in the primary table.
    */
    public Set primaryKeySet ()
    {
        return primaryTable.keySet();
    }

    /**
        Returns the set of keys in the secondary table corresponding to the
        primary key.

        If the primaryKey is null, throws NullPointerException.
    */
    public Set secondaryKeySet (Object primaryKey)
    {
        if (primaryKey == null) {
            throw new NullPointerException();
        }

        GrowOnlyHashtable secondaryTable 
            = (GrowOnlyHashtable)primaryTable.get(primaryKey);
        if (secondaryTable == null) {
            return null;
        }
        return secondaryTable.keySet();
    }

    /**
        Return a new GrowOnlyGridTable with reduced content.
        The returned table will have the same content as the input <code>table</code>
        but will skip copying the keys in <code>skippedPrimaryKeys</code> 
    */
    public GrowOnlyGridTable getReducedTable (List skippedPrimaryKeys)
    {
        GrowOnlyGridTable newTable = new GrowOnlyGridTable();
        Set primaryKeys = primaryKeySet();
        for (Iterator it = primaryKeys.iterator(); it.hasNext();) {
            Object tablePrimaryKey = it.next();
            if (skippedPrimaryKeys.contains(tablePrimaryKey)) {
                continue;
            }
            Set secondaryKeys = secondaryKeySet(tablePrimaryKey);
            for (Iterator innerIt = secondaryKeys.iterator(); innerIt.hasNext();) {
                Object secondaryKey = innerIt.next();
                Object tableValue = get(tablePrimaryKey, secondaryKey);
                newTable.put(tablePrimaryKey, secondaryKey, tableValue);
            }
        }

        return newTable;
    }

}



