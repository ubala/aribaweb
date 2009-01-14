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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ExtensibleFieldValueAccessor.java#2 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.Fmt;
import ariba.util.core.MultiKeyHashtable;

/**
    FieldValue_Extensible provides an implementation of the FieldValue
    interface for the Extensible interface.  Clasess which implement the
    Extensible interface allow for named fields to be added dynamically
    to their instances.  These fields are managed in a hashtable.  The
    FieldValue_Extensible implementation uses this hashtable as an optional
    place to locate fields after we have failed to find the desired field
    in the usual way.
*/
public class ExtensibleFieldValueAccessor extends BaseAccessor implements FieldValueSetter, FieldValueGetter
{
    //  This uses the identityComparison option.
    private static final MultiKeyHashtable ExtensibleAccessors =
        new MultiKeyHashtable(2, 8 , true);
    private static final String NullMarker = "NullMarker";

    /**
    Constructs a new ExtensibleFieldValueAccessor.

    @param forClass the class for which the access applies.
    @param fieldName the name of the desired field
    @return a new ExtensibleFieldValueAccessor
    */
    private ExtensibleFieldValueAccessor (Class forClass, String fieldName)
    {
        super(forClass, fieldName);
    }

    /**
    Creates a new instance of an ExtensibleFieldValueAccessor.

    @param targetClass the class for which the access applies.
    @param targetFieldName the name of the desired field
    @return a new ExtensibleFieldValueAccessor
    */
    public static ExtensibleFieldValueAccessor newInstance (Class targetClass, String targetFieldName)
    {
        ExtensibleFieldValueAccessor extensibleAccessor = null;
        synchronized (ExtensibleAccessors) {
            targetFieldName = targetFieldName.intern();
            extensibleAccessor = (ExtensibleFieldValueAccessor)ExtensibleAccessors
                .get(targetClass, targetFieldName);
            if (extensibleAccessor == null) {
                extensibleAccessor =
                    new ExtensibleFieldValueAccessor(targetClass, targetFieldName);
                ExtensibleAccessors.put(targetClass, targetFieldName,
                                        extensibleAccessor);
            }
        }
        return extensibleAccessor;
    }

    /**
    Gets a value from the target's extendedFields hashtable.  You must call
    setValue(...) before calling this or a FieldValueException will be thrown.

    @param target the object on which the receiver will
    perform the get operation.
    @return the object found in the extendedFields hashtable.
    */
    public Object getValue (Object target)
    {
        Object value = ((Extensible)target).extendedFields().get(_fieldName);
        if (value == null) {
            String message = Fmt.S("%s: attempt to get a value which has not been set: %s",
                                   target.getClass().getName(), _fieldName);
            throw new FieldValueException(message);
        }
        else if (value == NullMarker) {
            value = null;
        }
        return value;
    }

    /**
    Puts a value in the target's extendedFields hashtable.

    @param target the object on which the receiver will
    perform the set operation.
    @param value the object put in the hashtable.
    */
    public void setValue (Object target, Object value)
    {
        if (value == null) {
            value = NullMarker;
        }
        ((Extensible)target).extendedFields().put(_fieldName, value);
    }
}
