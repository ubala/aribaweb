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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWStringDictionary.java#8 $
*/

package ariba.ui.aribaweb.util;

import java.util.List;
import ariba.util.core.ListUtil;

public final class AWStringDictionary extends AWBaseObject
{
    private static final int IntialCapacity = 8;
    private AWEncodedString[] _keys;
    private AWEncodedString[] _values;
    private int _size;

    // AWStringDictionary is preferred to java.util.Map beause it doesn't require an Iterator to iterate.
    // Also, there's no casting required.
    // The performance of this is good for iteration, but for lookup's it is only good if the number of entries is small (ie < 16).

    public AWStringDictionary ()
    {
        super();
        _keys = new AWEncodedString[IntialCapacity];
        _values = new AWEncodedString[IntialCapacity];
        _size = 0;
    }

    private int indexOf (AWEncodedString targetKey)
    {
        int index = 0;
        int lastKeyIndex = _size - 1;
        for (index = lastKeyIndex; index >= 0; index--) {
            AWEncodedString key = _keys[index];
            if (key == targetKey || key.string() == targetKey.string()) {
                return index;
            }
        }
        for (index = lastKeyIndex; index >= 0; index--) {
            if (_keys[index].string().equals(targetKey.string())) {
                return index;
            }
        }
        return -1;
    }

    private void grow ()
    {
        int newCapacity = _keys.length * 2;
        _keys = (AWEncodedString[])AWUtil.realloc(_keys, newCapacity);
        _values = (AWEncodedString[])AWUtil.realloc(_values, newCapacity);
    }

    public void put (AWEncodedString newKey, AWEncodedString newValue)
    {
        int index = indexOf(newKey);
        if (index == -1) {
            index = _size;
            if (index >= _keys.length) {
                grow();
            }
            _size++;
        }
        _keys[index] = newKey;
        _values[index] = newValue;
    }

    public void put (String newKey, AWEncodedString newValue)
    {
        put(AWEncodedString.sharedEncodedString(newKey), newValue);
    }

    public void putIfIdenticalKeyAbsent (AWEncodedString newKey, AWEncodedString newValue)
    {
        int index = indexOf(newKey);
        if (index == -1) {
            index = _size;
            if (index >= _keys.length) {
                grow();
            }
            _size++;
            _keys[index] = newKey;
            _values[index] = newValue;
        }
    }

    public AWEncodedString elementAt (int index)
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
        return _size;
    }

    public List elementsVector ()
    {
        return ListUtil.arrayToList(_values, false);
    }

    public void clear ()
    {
        _size = 0;
    }

    public String toString ()
    {
        AWFastStringBuffer fastStringBuffer = new AWFastStringBuffer();
        fastStringBuffer.append("{\n");
        for (int index = (_size - 1); index >= 0; index--) {
            fastStringBuffer.append("    ");
            fastStringBuffer.append(_keys[index].string());
            fastStringBuffer.append(" = \"");
            fastStringBuffer.append(_values[index].string());
            fastStringBuffer.append("\"\n");
        }
        fastStringBuffer.append("}\n");
        return fastStringBuffer.toString();
    }
}
