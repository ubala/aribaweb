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

    $Id: //ariba/platform/util/core/ariba/util/core/HashtableEnumerator.java#5 $
*/

package ariba.util.core;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
    @aribaapi private
*/
class HashtableEnumerator implements Enumeration
{
    private boolean keyEnum;
    private int index;
    private int returnedCount;
    private Hashtable table;

    HashtableEnumerator (Hashtable table, boolean keyEnum)
    {
        super();
        this.table = table;
        this.keyEnum = keyEnum;
        returnedCount = 0;

        if (table.keys != null) {
            index = table.keys.length;
        }
        else {
            index = 0;
        }
    }

    public boolean hasMoreElements ()
    {
        return returnedCount < table.count;
    }

    public Object nextElement ()
    {
        index--;

        while (index >= 0 &&
               (table.elements[index] == null ||
                table.elements[index] == Hashtable.DeletedMarker)) {
            index--;
        }

        if (index < 0 || returnedCount >= table.count) {
            throw new NoSuchElementException();
        }

        returnedCount++;

        if (keyEnum) {
            return table.keys[index];
        }
        return table.elements[index];
    }
}
