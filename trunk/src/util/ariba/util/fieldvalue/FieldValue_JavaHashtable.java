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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValue_JavaHashtable.java#4 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.ClassUtil;

import java.util.Map;

/**
 The JavaHashtableFieldValueClassExtension converts the behavior of java.util.Map
 so that using fieldValue accessors on them does not attempt to access the fields or
 methods of the Map itself, but will access the values stored in the Map.
 The fieldName in the FieldPath will be treated as a key into the receiver hashtable.
*/
public class FieldValue_JavaHashtable extends FieldValue_Object
{
    /**
    Overridden to disable.  This throws an exception if called.
    */
    public FieldValueAccessor createAccessor (Object receiver, String fieldName, int type)
    {
        throw new FieldValueException(receiver.getClass().getName() +
                                      ": createAccessor() not suported");
    }

    /**
    Overridden to disable.  This throws an exception if called.
    */
    public FieldValueAccessor getAccessor (Object receiver, String fieldName, int type)
    {
        throw new FieldValueException(receiver.getClass().getName() +
                                      ": getAccessor() not suported");
    }

    // Note: Below we reverse the sense of which is a primitive -- the String
    // versions become the primitives and the FieldPath versions call the string versions.

    /**
     Recursively calls getFieldValuePrimitive() with the head of the fieldPath
     list up to the last fieldPath node and then calls setFieldValuePrimitive().
     Each time the recursion iterates, the receiver is the value of the
     previous getFieldValuePrimitive().

     * Unlike the base implementation, if a dotted path assigment is made to a missing
       property, the intermediate Map is created on-demand (i.e. a set to "a.b" where
       there is no "a" defined will create a map for a, then recurse.

    @param target a java.util.Map into which the value will be put at key
    @param fieldPath the key used to put value into receiver (a java.util.Map)
    @param value the value to put into the receiver (a java.util.Map)
    */
    public void setFieldValue (Object target,
                               FieldPath fieldPath, Object value)
    {
        FieldPath fieldPathCdr = fieldPath._nextFieldPath;
        if (fieldPathCdr == null) {
            setFieldValuePrimitive(target, fieldPath, value);
        }
        else {
            Object nextTargetObject = getFieldValuePrimitive(target, fieldPath);
            if (nextTargetObject == null) {
                nextTargetObject = ClassUtil.newInstance(target.getClass());
                ((Map)target).put(fieldPath.car(), nextTargetObject);
            }
            fieldPathCdr.setFieldValue(nextTargetObject, value);
        }
    }

    /**
    Uses the fieldName of the first node of fieldPath as the key to the receiver
     (which MUST be a java.util.Map) to put the value into the receiver.

    @param receiver a java.util.Map into which the value will be put at key
    @param fieldPath the key used to put value into receiver (a java.util.Map)
    @param value the value to put into the receiver (a java.util.Map)
    */
    public void setFieldValuePrimitive (Object receiver, FieldPath fieldPath,
                                        Object value)
    {
        setFieldValuePrimitive(receiver, fieldPath.car(), value);
    }

    /**
    Uses the fieldName of the first node of fieldPath as the key to the receiver
     (which MUST be a java.util.Map) to get the return value from the receiver.

    @param receiver a java.util.Map from which to get the value
    @param fieldPath the key used to get the value from receiver (a java.util.Map)
    @return the value found in the reciever (a java.util.Map)
    */
    public Object getFieldValuePrimitive (Object receiver, FieldPath fieldPath)
    {
        return getFieldValuePrimitive(receiver, fieldPath.car());
    }

    /**
    Uses the key to put the value into the receiver
     (which MUST be a java.util.Map).

    @param receiver a java.util.Map into which the value will be put at key
    @param key the key used to put value into receiver (a java.util.Map)
    @param value the value to put into the receiver (a java.util.Map)
    */
    public void setFieldValuePrimitive (Object receiver, String key, Object value)
    {
        Map hashtableReceiver = (Map)receiver;
        if (value == null) {
            hashtableReceiver.remove(key);
        }
        else {
            hashtableReceiver.put(key, value);
        }
    }

    /**
    Uses the key to access the value in the receiver
     (which MUST be a java.util.Map).

    @param receiver a java.util.Map from which to get the value
    @param key the key used to get the value from receiver (a java.util.Map)
    @return the value found in the reciever (a java.util.Map)
    */
    public Object getFieldValuePrimitive (Object receiver, String key)
    {
        return ((Map)receiver).get(key);
    }

    /**
        @param receiver a <code>Map</code> from which to get the value
        @param key the key used to get the value from <code>receiver</code>
        @return <code>true</code> if <code>receiver</code> has a
        a value for the given <code>key</code> and <code>false</code>
        otherwise; note that <code>receiver</code> may have a
        <code>null</code> value for the requested <code>key</code>

        @aribaapi ariba
    */
    public boolean hasFieldValuePrimitive (Object receiver, String key)
    {
        return ((Map)receiver).containsKey(key);
    }
}
