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

    $Id: //ariba/platform/util/expr/ariba/util/expr/ConvertibleFieldValue.java#4 $
*/

package ariba.util.expr;

import ariba.util.fieldvalue.FieldValue;
import ariba.util.core.MapUtil;
import ariba.util.core.ClassUtil;

import java.util.Map;

/**
    @aribaapi private
*/
public class ConvertibleFieldValue
{
    private static final Map _registry = MapUtil.map();

    public static Object convert (Object source)
    {
        if (source == null) {
            return null;
        }

        Class sourceClass = source.getClass();
        if (sourceClass == null) {
            return source;
        }

        ConvertibleFieldValue converter = (ConvertibleFieldValue)_registry.get(sourceClass);
        if (converter != null) {
            if (!(converter instanceof ConvertibleFieldValue)) {
                return source;
            }
            return converter.getValue(source);
        }

        return source;
    }

    public static void register (Class source, Object proxy)
    {
        if (source != null && proxy != null) {
            _registry.put(source, proxy);
        }
    }

    protected ConvertibleFieldValue ()
    {
    }

    protected Object getValue (Object source)
    {
        return source;
    }
}
