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

    $Id: //ariba/platform/util/core/ariba/util/core/LinearLRUHashtable.java#4 $
*/

package ariba.util.core;

import ariba.util.log.Log;

/**
    Provides an LRUHashtable that uses linear growth (the LRUHashtable class
    uses exponential growth).

    @aribaapi private
*/
public class LinearLRUHashtable extends LRUHashtable
{
    private int growthAmount;
    
    public LinearLRUHashtable (
                String name,
                int initialEntries,
                int targetEntries,
                int purgeMargin,
                int growthAmount,
                double maxLoad,
                LRURemoveListener removeListener)
    {
        super(name, 
              initialEntries, 
              targetEntries, 
              purgeMargin,
              maxLoad,
              removeListener);
        this.growthAmount = growthAmount;
    }

    /**
        Removes the oldest item from the hashtable
        and returns true if more items need purging.
    */
    public boolean purgeOneItem ()
    {
        if (targetEntries < inUse) {
            removeOldestUntil(inUse-1, null);
            Log.lruDebug.debug("Purging one item, down to %s", 
                           Constants.getInteger(inUse));
        }
        
        return (targetEntries < inUse);
    }
    
    /**
        Intializes the entry table to be of size
        <code>initialEntries</code>.
    */
    protected void initializeEntries (int initialEntries)
    {
        allocateEntries(initialEntries);
    }

    /**
        Grows the table when the maximum load is reached.
    */
    protected void grow ()
    {
        resize(allocated+growthAmount);
    }

    /**
        Generate a hash code
    */
    protected int firstProbe (Object key)
    {
        int mixed = Math.abs((int)(getHashValueForObject(key) * Multiplier));
        return (mixed % allocated);
    }
}

