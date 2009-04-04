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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldInfo.java#3 $
*/
package ariba.util.fieldvalue;

import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class FieldInfo
{
    public static class Collection
    {
        Class _cls;
        Map<String, FieldInfo> _infoByName = MapUtil.map();
        boolean _includeFields;
        boolean _includeNonBeanStyleGetters;

        public Collection (boolean includeFields, boolean includeNonBeanStyleGetters)
        {
            _includeFields = includeFields;
            _includeNonBeanStyleGetters = includeNonBeanStyleGetters;
        }

        public boolean includeFields ()
        {
            return _includeFields;
        }

        public boolean includeNonBeanStyleGetters ()
        {
            return _includeNonBeanStyleGetters;
        }

        // called by FieldValue implementations
        public void updateInfo (String name, Class type, boolean isPublic,
                                    boolean readable, boolean writable,
                                    Method getter, Method setter, Field field)
        {
            FieldInfo info = _infoByName.get(name);
            if (info == null) {
                info = new FieldInfo();
                info._name = name;
                info._rank = _infoByName.size();
                _infoByName.put(name, info);
            }

            // augment any specified info
            if (type != null) info._type = type;
            if (isPublic) info._public = true;
            if (readable) info._readable = true;
            if (writable) info._writable = true;
            if (getter != null) info._getterMethod = getter;
            if (setter != null) info._setterMethod = setter;
            if (field != null) info._field = field;
        }

        public List<FieldInfo> allFieldInfos ()
        {
            List<FieldInfo> result = ListUtil.list();
            for (FieldInfo f : _infoByName.values()) {
                if (f.isPublic()) result.add(f);
            }
            return result;
        }

        FieldInfo infoForField (String name)
        {
            return _infoByName.get(name);
        }

        public Class getCls ()
        {
            return _cls;
        }
    }

    public static Collection fieldInfoForClass (Class cls, boolean includeFields,
                                                           boolean includeNonBeanStyleGetters)
    {
        // could cache, but not for now...
        Collection col = new Collection(includeFields, includeNonBeanStyleGetters);
        col._cls = cls;
        FieldValue.get(cls).populateFieldInfo(cls, col);

        return col;
    }

    String _name;
    int _rank;
    Class _type;
    boolean _readable;
    boolean _writable;
    boolean _public;

    Method _getterMethod;
    Method _setterMethod;
    Field _field;

    public String getName ()
    {
        return _name;
    }

    public int getRank ()
    {
        return _rank;
    }

    public Class getType ()
    {
        return _type;
    }

    public boolean isReadable ()
    {
        return _readable;
    }

    public boolean isWritable ()
    {
        return _writable;
    }

    public boolean isPublic ()
    {
        return _public;
    }

    public Method getGetterMethod ()
    {
        return _getterMethod;
    }

    public Method getSetterMethod ()
    {
        return _setterMethod;
    }

    public Field getField ()
    {
        return _field;
    }
}
