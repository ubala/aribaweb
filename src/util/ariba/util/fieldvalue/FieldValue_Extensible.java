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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValue_Extensible.java#2 $
*/

package ariba.util.fieldvalue;

/**
    FieldValue_Extensible provides an implementation of the FieldValue
    interface for the Extensible interface.  Clasess which implement the
    Extensible interface allow for named fields to be added dynamically
    to their instances.  These fields are managed in a hashtable.  The
    FieldValue_Extensible implementation uses this hashtable as an optional
    place to locate fields after we have failed to find the desired field
    in the usual way.
*/
public class FieldValue_Extensible extends FieldValue_Object
{
    public FieldValueAccessor createAccessor (Object target, String fieldName, int type)
    {
        FieldValueAccessor accessor = super.createAccessor(target, fieldName, type);
        if (accessor == null) {
            // Note: can ignore 'type' since ExtensibleFieldValueAccessor
            // supports both set and get
            accessor = ExtensibleFieldValueAccessor.newInstance(target.getClass(), fieldName);
        }
        return accessor;
    }
}
