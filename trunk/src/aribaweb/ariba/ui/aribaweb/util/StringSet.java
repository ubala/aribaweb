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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/StringSet.java#6 $
*/

package ariba.ui.aribaweb.util;

import java.util.Map;
import ariba.util.core.MapUtil;

/**
    <code>StringSet</code> is a basic container of Strings.  It provides
    constant-time lookups and insertions.

    @aribaapi private
*/

public final class StringSet
{
    private Map ht = MapUtil.map();

    /**
        Add a string to the set

        @aribaapi private
    */
    public void put (String str)
    {
        ht.put(str,str);
    }

    /**
        Returns true if the set contains the string, false otherwise

        @aribaapi private
    */
    public boolean contains (String str)
    {
        return (ht.containsKey(str));
    }
}
