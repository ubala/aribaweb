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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTDataSource.java#6 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.util.AWGenericException;

import java.util.List;

public abstract class AWTDataSource
{
    /**
     * Returns the list of objects.  Should list should be sorted if dataSourceDoesSort() has
     * been overridden to return true
     * @return array of fetched objects
     */
    public abstract List fetchObjects ();

    public AWTEntity entity()
    {
        return null;
    }

    /**
     * Should return true when underlying changes in the datasource indicate that the DisplayGroup should
     * automatcally call fetchObjects() as soon as possible to present the user with recent changes.
     * Datasources for which fetching is not "free" should override this method to return false
     * either always, or at least unless a special event has occured that indicates re-performing
     * a fetch is warrented.
     * @return true if a fetch should be triggered immediately.
     */
    public boolean hasChanges ()
    {
        return true;
    }

    public void setSortOrderings (List<AWTSortOrdering> orderings)
    {

    }

    /**
     * Apps that wish to sort in the database and *re-fetch* upon a user sort change should
     * do so by implementing a AWTDataSource subclass and binding that to the AWTDisplayGroup for their table.
     * When using a DataSource, the app should *not* assign objects directly to the DisplayGroup via
     * setObjectArray(), *nor* should it use the DataTable's `list` binding.  Instead, it should rely on the
     * DataSource's fetch method to return the (sorted) list of objects.
     * 
     * @return whether data source fetchObjects() method will return objects sorted according
     * the most recent sort order set via setSortOrderings()
     */
    public boolean dataSourceDoesSort ()
    {
        return false;
    }
    
    public Object insert ()
    {
        throw new AWGenericException("Not implemented");
    }

    public void delete (Object object)
    {
        throw new AWGenericException("Not implemented");
    }
}
