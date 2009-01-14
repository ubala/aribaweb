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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWBaseObject.java#23 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Hashtable;
import ariba.util.core.Vector;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.ListUtil;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.fieldvalue.FieldValue;
import ariba.ui.aribaweb.core.AWStringLocalizer;
import ariba.ui.aribaweb.core.AWConcreteApplication;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

final class AWEmptyVector extends Vector
{
    public boolean add (Object element)
    {
        throw new AWGenericException(getClass().getName() + ": attempt to add() to immutable List");
    }

    public void add (int index, Object element)
    {
    throw new AWGenericException(getClass().getName() + ": attempt to insertElementAt() on immutable List");
    }

    public void setElementAt (Object element, int index)
    {
    throw new AWGenericException(getClass().getName() + ": attempt to setElementAt() on immutable List");
    }

}

final class AWEmptyHashtable extends Hashtable
{
    public Object put (Object key, Object element)
    {
        throw new AWGenericException(getClass().getName() + ": attempt to put() to immutable Map");
    }
}

final class AWEmptyMap extends HashMap
{
    public void putAll (Map t)
    {
        throw new AWGenericException(getClass().getName() + ": attempt to putAll() to immutable Map");
    }

    public Object put (Object key, Object element)
    {
        throw new AWGenericException(getClass().getName() + ": attempt to put() to immutable Map");
    }
}

/** @aribaapi private */

abstract public class AWBaseObject extends Object implements AWObject
{
    public static AWLogHandling LogHandling;
    public static final Class ClassClass = Class.class;
    public static final Class ObjectClass = Object.class;
    public static final Class IntegerClass = Integer.class;
    public static final Class StringClass = String.class;
    public static final Class JavaMapClass = java.util.Map.class;
    public static final Class AribaHashtableClass = ariba.util.core.Hashtable.class;
    public static final Class AribaVectorClass = ariba.util.core.Vector.class;
    public static final Class JavaHashtableClass = java.util.Hashtable.class;
    public static final Class JavaVectorClass = java.util.Vector.class;
    public static final Object NullObject = new Object();

    public static final Map EmptyMap = new AWEmptyMap();
    public static final Map EmptyHashtable = new AWEmptyHashtable();
    public static final List EmptyVector = new AWEmptyVector();
    public static final String UndefinedString = "UndefinedString";
    public static final int UninitializedRealNumber = -1;
    public static final Object UndefinedObject = new Object();

    public static final String True = "true";
    public static final String False = "false";
    private static GrowOnlyHashtable ScannedClasses;


    // ** Thread Safety Considerations: no ivars -- no locking required.

    /* Note:

        This code is commented out because it incurrs unnecessary overhead during runtime.
        This is only useful when doing analysis on memory allocation issues.

    public AWBaseObject ()
    {
        super();
        AWMemoryStats.add(this);
    }

    public void finalize ()
    {
        try {
            AWMemoryStats.remove(this);
            super.finalize();
        }
        catch (Throwable throwable) {
        }
    }
    */

    public void init ()
    {
    }

    public boolean isKindOfClass (Class targetClass)
    {
        return AWUtil.classInheritsFromClass(getClass(), targetClass);
    }

    public final void setFieldValue (String keyPathString, Object value)
    {
        FieldValue.setFieldValue(this, keyPathString, value);
    }

    public final Object getFieldValue (String keyPathString)
    {
        return FieldValue.getFieldValue(this, keyPathString);
    }

    /////////////
    // Logging
    /////////////
    public void logString (String string)
    {
        LogHandling.logString(string);
    }

    public void debugString (String string)
    {
        LogHandling.debugString(string);
    }

    /**
     * Send the warning message to a generic log message
     * @param message - the message to be logged as a warning
     */
    public void logWarning (String message)
    {
        Log.aribaweb.warning(9339, message);
    }

    private void printValueError (Field field, String value)
    {
        String message = Fmt.S(
            "****** Unclean stateless object %s: %s %s == %s",
            getClass().getName(),
            field.getType().getName(),
            field.getName(),
            value);
        Log.aribawebvalidation_state.debug(message);
        ScannedClasses.put(getClass(), getClass());
        // todo: allow for throwing this exception by cleaning up all stateless/localPool components
        //throw new AWGenericException(message);
    }

    private void ensureFieldValuesClear (Class classObject, Object[] fields)
    {
        try {
            for (int index = 0, length = fields.length; index < length; index++) {
                Field field = (Field)fields[index];
                Class fieldType = field.getType();
                // if a primitive field is not cleaned up and the
                // aribawebvalidation.state_primitive is enabled, then log them
                if (fieldType.isPrimitive()) {
                    if (Boolean.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getBoolean(this)) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else if (Byte.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getByte(this) != 0) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else if (Character.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getChar(this) != 0) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else if (Double.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getDouble(this) != 0) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else if (Float.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getFloat(this) != 0) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else if (Integer.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getInt(this) != 0) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else if (Long.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getLong(this) != 0) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else if (Short.TYPE.isAssignableFrom(fieldType)) {
                        if (field.getShort(this) != 0) {
                            printValueError(field, field.get(this).toString());
                        }
                    }
                    else {
                        throw new AWGenericException("unsupported primitive type: " + fieldType.getName());
                    }
                }
                else if (field.get(this) != null) {
                    printValueError(field, field.get(this).toString());
                }
            }
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new AWGenericException(illegalAccessException);
        }
    }

    // To be overridden by subclasses to determine whether field type must be cleared upon
    // checkin to pool.  Overridden by AWComponent
    protected boolean isFieldRequiredClear (Field field)
    {
        int modifiers = field.getModifiers();
        Class fieldType = field.getType();

        // exclude static/final fields.  Also, exclude primitive fields (and Strings) unless
        // aribawebvalidation_state_primitive is enabled
        return (!Modifier.isStatic(modifiers) &&
                !Modifier.isFinal(modifiers) &&
                (!fieldType.isPrimitive() && !fieldType.equals(String.class)
                    || Log.aribawebvalidation_state_primitive.isDebugEnabled()));
    }

    protected void addScannableFields (Class thisClass, Class topClass, List resultList)
    {
        Field[] fields = thisClass.getDeclaredFields();
        for (int index = 0, length = fields.length; index < length; index++) {
            Field field = fields[index];
            if (isFieldRequiredClear(field)) {
                field.setAccessible(true);
                resultList.add(field);
            }
        }
        // recurse
        thisClass = thisClass.getSuperclass();
        if (thisClass != null && thisClass != topClass) {
            addScannableFields(thisClass, topClass, resultList);
        }
    }

    /**
       Only called in debug mode...
       Scans fields of pool-shared components to ensure that they've been reset.
    */
    protected void ensureFieldValuesClear (Class leafClass, Class topClass)
    {
        if (ScannedClasses == null) {
            ScannedClasses = new GrowOnlyHashtable();
        }
        Object data = ScannedClasses.get(leafClass);

        if (data == null) {
            // compute and cache a flattened out list of scannable fields
            List fields = ListUtil.list();
            addScannableFields(leafClass, topClass, fields);
            data = fields.toArray();
            ScannedClasses.put(leafClass, data);
        }

        // marker for class with known errors
        if (data instanceof Class) return;

        ensureFieldValuesClear(leafClass, (Object[])data);
    }

    public void ensureFieldValuesClear ()
    {
        ensureFieldValuesClear(getClass(), AWBaseObject.class);
    }


    private static MultiKeyHashtable LocalizedStrings;
    private static String StringTableName = "ariba.ui.aribaweb.core";

    public static String localizedJavaString (String componentName,
                                              int stringId, String originalString,
                                              AWSingleLocaleResourceManager resourceManager)
    {
        String localizedString = null;
        if (LocalizedStrings == null) {
            LocalizedStrings = new MultiKeyHashtable(3);
        }
        int resourceManagerIndex = resourceManager.index();
        localizedString = (String)LocalizedStrings.get(resourceManagerIndex, componentName, stringId);
        if (localizedString == null) {
            synchronized (LocalizedStrings) {
                localizedString = (String)LocalizedStrings.get(resourceManagerIndex, componentName, stringId);
                if (localizedString == null) {
                    AWStringLocalizer localizer = AWConcreteApplication.SharedInstance.getStringLocalizer();
                    Map localizedStringsHashtable =
                        localizer.getLocalizedStrings(StringTableName,
                                                      componentName,
                                                      resourceManager);

                    if (localizedStringsHashtable != null) {
                        MultiKeyHashtable localizedStringsCopy = (MultiKeyHashtable)LocalizedStrings.clone();
                        Iterator keyEnumerator = localizedStringsHashtable.keySet().iterator();
                        while (keyEnumerator.hasNext()) {
                            String currentStringId = (String)keyEnumerator.next();
                            // XXX aliu: an application might choose to merge awl strings and java strings into one single string
                            // file, so we need to check for the integer key. all the awl strings will start with a letter such as
                            // "a001".
                            char firstCharacter = currentStringId.charAt(0);
                            if (firstCharacter >= '0' && firstCharacter <= '9') {
                                String currentLocalizedString = (String)localizedStringsHashtable.get(currentStringId);
                                localizedStringsCopy.put(resourceManagerIndex, componentName,
                                        Integer.parseInt(currentStringId), currentLocalizedString);
                            }
                        }
                        localizedString = (String)localizedStringsCopy.get(resourceManagerIndex, componentName, stringId);
                        if (localizedString == null) {
                            localizedString = originalString;
                            localizedStringsCopy.put(resourceManagerIndex, componentName, stringId, localizedString);
                        }
                        LocalizedStrings = localizedStringsCopy;
                    }
                    else {
                        localizedString = originalString;
                    }
                }
            }
        }
        return localizedString;
    }
}
