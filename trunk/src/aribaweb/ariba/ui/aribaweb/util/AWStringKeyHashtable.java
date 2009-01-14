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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWStringKeyHashtable.java#8 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Hashtable;
import ariba.util.core.MapUtil;
import java.util.Iterator;
import java.util.Map;

/* ----------
    This class allows for optional case-insensitivity in the keys used.  This has the same characteristics of
    a regular hashtable (except it only accepts Strings as keys) until someone sends the get(key, true) message.
    At this point, the underlying hashtable is populated with all its keys being lowercased.  Once this is done,
    lookups which ignore case will lowercase the target key before looking them up.  Further additions (via put())
    will always lowercase the key and enter the key twice -- once as regular case, and once as lowercase.
    Needless to say, it is constly to switch these hashtables into 'ignoresCase' mode.

    Important: This does NOT allow for ignoresCase for 'put' only for 'get'.  This means that you can add two
    entries (eg Alpha='foo' and alphA='bar') and both entries will exist with their distinct values.  If you
    subsequently do a get('Alpha', true), you will get back 'bar', while
    get('Aplha', false) will return 'foo' when you might have expected it to return 'bar'.

    Ideally this would be a CaseInsensitiveHashtable, but I wasn't prepared to take that on right now.
   ---------- */

public final class AWStringKeyHashtable extends Hashtable
{
    private Map _lowercaseTable;

    public AWStringKeyHashtable ()
    {
        super();
    }

    public AWStringKeyHashtable (int initCapacity)
    {
        super(initCapacity);
    }
    
    public Object put (Object key, Object element)
    {
        return put((String)key, element);
    }

    public Object get (Object key)
    {
        return get((String)key, false);
    }

    public Object put (String key, Object element)
    {
        if (_lowercaseTable != null) {
            _lowercaseTable.put(key.toLowerCase(), element);
        }
        return super.put(key, element);
    }

    public Object get (String key)
    {
        return get(key, false);
    }

    public Object get (String key, boolean ignoresCase)
    {
        Object element;
        if (ignoresCase) {
            if (_lowercaseTable == null) {
                _lowercaseTable = MapUtil.map();
                Iterator keys = keySet().iterator();
                while (keys.hasNext()) {
                    String currentKey = (String)keys.next();
                    Object currentElement = super.get(currentKey);
                    _lowercaseTable.put(currentKey.toLowerCase(), currentElement);
                }
            }
            element = _lowercaseTable.get(key.toLowerCase());
        }
        else {
            element = super.get(key);
        }
        return element;
    }
}
