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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/MapForEach.java#2 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import java.util.Map;
import java.util.Iterator;

/**
    Currently this only accepts java.util.Map's.  In order
    to support other similar hashtable objects (like NSDictionary
    and/or java.util.Map), I'll need to create a category for
    that (similar to the OrderedListClassExtension) and use it in
    here.
*/
public final class MapForEach extends AWComponent
{
    private Map _dictionary;

    protected void awake ()
    {
        _dictionary = null;
    }

    private Map dictionary ()
    {
        if (_dictionary == null) {
            _dictionary = (Map)valueForBinding("dictionary");
        }
        return _dictionary;
    }

    public Iterator dictionaryKeys ()
    {
        // NOTE:  we cannot access this from the awl (eg $dictionary.keys) as this will return null since kvc on Map access the keys of the hashtable and not the methods.
        return dictionary().keySet().iterator();
    }
    
    public void setCurrentKey (Object currentKey)
    {
        Object currentValue = dictionary().get(currentKey);
        setValueForBinding(currentKey, "key");
        setValueForBinding(currentValue, "value");
    }
}
