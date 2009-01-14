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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValue_Class.java#2 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.MultiKeyHashtable;

/**
The FieldValue_Class provides an implementation of the FieldValue
interface for the Class class.  The only difference between classes
and regular objects is that their class is themselves.
*/
class FieldValue_Class extends FieldValue_Object
{
    /**
    Same as FieldValue_Class's implementation except that this does not
    dereference the target's class since the target is already a class.

    @param target the object (actually a Class) for which the accessor will be created
    @param fieldName the name of the field for which the accessor will be created
    @return a new FieldValueAccessor (ReflectionFieldValueAccessor by default)
    */
    public FieldValueAccessor createAccessor (Object target, String fieldName, int type)
    {
        FieldValueAccessor accessor = null;
        Class targetClass = (Class)target;
        if (type == Setter) {
            accessor = FieldValueAccessorUtil.newReflectionSetter(targetClass, fieldName);
        }
        else {
            accessor = FieldValueAccessorUtil.newReflectionGetter(targetClass, fieldName);
        }
        return accessor;
    }

    /**
    Same as FieldValue_Class's implementation except that this does not
    dereference the target's class since the target is already a class.

    @param target the object for which the accessor will be looked up
    @param fieldName the name of the field for the accessor
    @return the cached FieldValueAccessor (ReflectionFieldValueAccessor by default)
    */
    public FieldValueAccessor getAccessor (Object target, String fieldName, int type)
    {
        FieldValueAccessor accessor = null;
        Class targetObjectClass = (Class)target;
        MultiKeyHashtable accessorsHashtable = _accessorsHashtable[type];
        synchronized (accessorsHashtable) {
            accessor =
            (FieldValueAccessor)accessorsHashtable.get(targetObjectClass, fieldName);
            if (accessor == null) {
                accessor = createAccessor(targetObjectClass, fieldName, type);
                accessorsHashtable.put(targetObjectClass, fieldName, accessor);
            }
        }
        return accessor;
    }
}
