/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/DetailDataSource.java#7 $
*/
package ariba.ui.meta.persistence;

import ariba.ui.table.AWTDataSource;
import ariba.ui.table.AWTEntity;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.aribaweb.core.AWChecksum;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.RelationshipField;
import ariba.util.fieldvalue.OrderedList;

import java.util.List;
import java.util.ArrayList;

public class DetailDataSource extends AWTDataSource
{
    Object _parentObject;
    FieldPath _detailFieldPath;
    long _prevCRC = 0;

    public DetailDataSource (Object parent, String keyPath)
    {
        _parentObject = parent;
        _detailFieldPath = new FieldPath(keyPath);
    }

    public Object parentObject ()
    {
        return _parentObject;
    }

    public void setParentObject (Object parentObject)
    {
        _parentObject = parentObject;
    }

    public boolean hasChanges ()
    {
        long crc = -1;
        if (_parentObject != null) {
            Object list = _detailFieldPath.getFieldValue(_parentObject);
            crc = (list == null) ? -1 : AWChecksum.crc32HashOrderedList(1, list);
            if (crc == 0) crc = -2;
        }

        if (crc != _prevCRC) {
            _prevCRC = crc;
            return true;
        }

        return false;
    }

    public List fetchObjects()
    {
        if (_parentObject == null) return null;
        Object list = _detailFieldPath.getFieldValue(_parentObject);
        return (list == null) ? null : OrderedList.get(list).toList(list);
    }

    public AWTEntity entity()
    {
        // Todo?
        return null;
    }

    String detailClassName ()
    {
        ObjectMeta meta = UIMeta.getInstance();
        Context context = meta.newContext();
        context.set(ObjectMeta.KeyClass, _parentObject.getClass().getName());
        context.set(ObjectMeta.KeyField, _detailFieldPath.fieldPathString());
        return (String)context.propertyForKey(ObjectMeta.KeyElementType);
    }

    public Object insert()
    {
        if (_parentObject == null) return null;
        ObjectContext ctx = ObjectContext.get();
        Object instance = ctx.create(detailClassName());
        /**
            _parentObject is transient if not saved,
            so Hibernate will db insert it resulting in double insert when the detail object save
            Object parent = ctx.merge(_parentObject);
            Comment out for now until nested session is implemented
        */
        RelationshipField.addTo(_parentObject, _detailFieldPath, instance);
        return instance;
    }

    public void delete(Object object)
    {
        if (_parentObject == null) return;
        ObjectContext ctx = ObjectContext.get();
        /**
            _parentObject is transient if not saved,
            so Hibernate will db insert it resulting in double insert when the detail object save
            Object parent = ctx.merge(_parentObject);
            Comment out for now until nested session is implemented
        */
        RelationshipField.removeFrom(_parentObject, _detailFieldPath, object);
    }
}
