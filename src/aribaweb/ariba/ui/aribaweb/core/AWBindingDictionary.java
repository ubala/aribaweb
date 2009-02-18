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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBindingDictionary.java#16 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

public final class AWBindingDictionary extends AWBaseObject
{
    private static final  AWEncodedString[] EmptyKeyArray = new AWEncodedString[0];
    private static final  AWBinding[] EmptyValueArray = new AWBinding[0];
    private Map _hashtable;
    private AWEncodedString[] _keys;
    private AWBinding[] _values;

    public AWBindingDictionary ()
    {
        this(MapUtil.map(0));
    }

    public AWBindingDictionary (Map hashtable)
    {
        super();
        int size = hashtable.size();
        if (size == 0) {
            _keys = EmptyKeyArray;
            _values = EmptyValueArray;
            _hashtable = EmptyHashtable;
        }
        else {
            _keys = new AWEncodedString[size];
            _values = new AWBinding[size];
            _hashtable = MapUtil.cloneMap(hashtable);
            int index = 0;
            Iterator keyEnumerator = hashtable.keySet().iterator();
            while (keyEnumerator.hasNext()) {
                String currentKey = (String)keyEnumerator.next();
                AWBinding currentValue = (AWBinding)hashtable.get(currentKey);
                _keys[index] = AWEncodedString.sharedEncodedString(currentKey.intern());
                _values[index] = currentValue;
                index++;
            }
        }
    }

    public void _put (String bindingName, AWBinding binding)
    {
        bindingName = bindingName.intern();
        if (_hashtable.get(bindingName) == null) {
            synchronized (this) {
                if (_hashtable.get(bindingName) == null) {
                    AWEncodedString encodedBindingName = AWEncodedString.sharedEncodedString(bindingName);
                    _keys = (AWEncodedString[])AWUtil.addElement(_keys, encodedBindingName);
                    _values = (AWBinding[])AWUtil.addElement(_values, binding);
                    Map hashtable = _hashtable == EmptyHashtable ? MapUtil.map() : MapUtil.cloneMap(_hashtable);
                    hashtable.put(bindingName, binding);
                    _hashtable = hashtable;
                }
            }
        }
    }

    public AWBinding get (String targetKey)
    {
        return (AWBinding)_hashtable.get(targetKey);
    }

    public AWBinding elementAt (int index)
    {
        return _values[index];
    }

    public AWEncodedString nameAt (int index)
    {
        return _keys[index];
    }

    public String keyAt (int index)
    {
        return nameAt(index).string();
    }

    public int size ()
    {
        return _hashtable.size();
    }

    public List elementsVector ()
    {
        return ListUtil.arrayToList(_values, false);
    }

    public int hashCode ()
    {
        return _hashtable.hashCode();
    }

    public boolean equals (Object obj)
    {
        return (obj instanceof AWBindingDictionary) && _hashtable.equals(((AWBindingDictionary)obj)._hashtable);
    }
}
