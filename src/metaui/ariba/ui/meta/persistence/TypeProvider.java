/*
    Copyright 2009 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/TypeProvider.java#1 $
*/
package ariba.ui.meta.persistence;

import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.ObjectMeta;
import ariba.util.core.ClassUtil;

public class TypeProvider
{
    Meta _meta;
    String _className;

    class Info
    {
        String _keyPath;

        Info (String keyPath)
        {
            _keyPath = keyPath;
        }

        public Class typeClass ()
        {
            Context ctx = _meta.newContext();
            ctx.set(ObjectMeta.KeyClass, _className);
            ctx.set(ObjectMeta.KeyField, _keyPath);
            String type = (String)ctx.propertyForKey(ObjectMeta.KeyType);
            return type != null ? ClassUtil.classForName(type) : null;
        }
    }

    public TypeProvider (Meta meta, String className)
    {
        _meta = meta;
        _className = className;
    }

    public Info infoForKeyPath (String keyPath)
    {
        return new Info(keyPath);
    }
}
