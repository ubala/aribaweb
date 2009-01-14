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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ReflectionFieldAccessor.java#2 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.MultiKeyHashtable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
The ReflectionFieldAccessor is a wrapper for java.lang.reflect.Field and
provides a uniform interface so that users needn't know if the accesor
they're dealing with is either a Method or a Field.
*/
class ReflectionFieldAccessor extends BaseAccessor implements FieldValueSetter, FieldValueGetter
{
    private static final MultiKeyHashtable FieldAccessors = new MultiKeyHashtable(2, 8, true);
    private final Field _field;
    private CompiledAccessor _compiledField;
    private int _accessCount = 0;

    /**
    Constructs a new ReflectionFieldAccessor with the given Class and Field.

    @param forClass the class for which the wrapped Field applies
    @param fieldName the wrapped field
    */
    private ReflectionFieldAccessor (Class forClass, String fieldName,  Field field)
    {
        super(forClass, fieldName);
        _field = field;
        Assert.that(Modifier.isPublic(_field.getModifiers()),
                    "Fields referenced via FieldValue must be public: %s", _field);
        Assert.that(Modifier.isPublic(_field.getDeclaringClass().getModifiers()),
                    "Fields referenced via FieldValue must be within a public class:\n\t%s\n\t%s\n",
                    _field.getDeclaringClass(), _field);
    }

    /**
    Determines if the actualFieldName can be used given the
    targetFieldName.  First an exact match is tried, then we try
    matching by prefixing targetFieldName with an '_'.

    @param targetFieldName the fieldName we're searching for
    @param actualFieldName the actual fieldName we're trying to
    see if can match targetFieldName given the matchin rules
    @return a boolean indicating a match
    */
    protected static boolean matchTargetAndActualFieldName (String targetFieldName,
                                                            String actualFieldName)
    {
        return targetFieldName.equals(actualFieldName) ||
            FieldValueAccessorUtil.equals(actualFieldName, "_", targetFieldName);
    }

    /**
    Provides an instance of ReflectionFieldAccessor for
    the given class and fieldName.  This recurses up the
    targetClass' superclass chain looking for an appropriate method.

    @param targetClass the class for which to locate the targetFieldName
    @param targetFieldName the name of the desired field
    @param currentClass the class for which the current iteration of the
    recursion applies
    @return a new ReflectionFieldAccessor wrapping the Field object
    from targetClass and for targetFieldName
    */
    private static ReflectionFieldAccessor newInstance (Class targetClass,
        String targetFieldName, Class currentClass)
    {
        ReflectionFieldAccessor accessorField = null;
        Field fieldArray[] = FieldValueAccessorUtil.getDeclaredFields(currentClass);
        for (int index = fieldArray.length - 1; index > -1; index--) {
            Field currentPrimitiveField = fieldArray[index];
            String currentFieldName = currentPrimitiveField.getName();
            if (matchTargetAndActualFieldName(targetFieldName, currentFieldName)) {
                accessorField = new ReflectionFieldAccessor(targetClass,
                    targetFieldName, currentPrimitiveField);
                break;
            }
        }
        if (accessorField == null) {
            Class currentSuperclass = currentClass.getSuperclass();
            if (currentSuperclass != null) {
                accessorField = newInstance(targetClass, targetFieldName,
                                         currentSuperclass);
            }
        }
        return accessorField;
    }

    /**
    Provides an instance of ReflectionFieldAccessor for the given
    class and fieldName.  This recurses up the targetClass' superclass
    chain looking for an appropriate method.

    @param targetClass the class for which to locate the targetFieldName
    @param targetFieldName the name of the desired field
    @return a new ReflectionFieldAccessor wrapping the Field object
    from targetClass and for targetFieldName
    */
    protected static ReflectionFieldAccessor newInstance (Class targetClass,
                                                       String targetFieldName)
    {
        // The reason we cache the fields in here is because they are used for
        // both setting and getting, and the other caches do not cross reference.
        ReflectionFieldAccessor reflectionFieldAccessor = null;
        synchronized (FieldAccessors) {
            targetFieldName = targetFieldName.intern();
            reflectionFieldAccessor = (ReflectionFieldAccessor)FieldAccessors
                .get(targetClass, targetFieldName);
            if (reflectionFieldAccessor == null) {
                reflectionFieldAccessor =
                    newInstance(targetClass, targetFieldName, targetClass);
                if (reflectionFieldAccessor != null) {
                    FieldAccessors.put(targetClass, targetFieldName,
                                       reflectionFieldAccessor);
                }
            }
        }
        return reflectionFieldAccessor;
    }

    /**
    Calls 'set' on the underlying reflection Field with the target,
    thus setting the value.

    @param target the object on which to set the value using the wrapped Field
    @param value the value to be set of the target
    */
    public void setValue (Object target, Object value)
    {
        try {
            if (_compiledField != null) {
                _compiledField.setValue(target, value);
            }
            else {
                _field.set(target, value);
                _accessCount++;
                if (CompiledAccessorFactory.compiledAccessorThresholdPassed(_accessCount)) {
                    _compiledField = CompiledAccessorFactory.newInstance(_field);
                    if (_compiledField == null) {
                        _accessCount = 0;
                    }
                }
            }
        }
        catch (IllegalAccessException illegalAccessException) {
            FieldValueException.throwException(illegalAccessException);
        }
    }

    /**
    Calls 'get' on the underlying reflection Field with the target,
    returning the existing value.

    @param target the object on which to invoke 'get' for the wrapped Field
    @return the result of the get
    */
    public Object getValue (Object target)
    {
        try {
            if (_compiledField != null) {
                return _compiledField.getValue(target);
            }
            else {
                _accessCount++;
                if (CompiledAccessorFactory.compiledAccessorThresholdPassed(_accessCount)) {
                    _compiledField = CompiledAccessorFactory.newInstance(_field);
                    if (_compiledField == null) {
                        _accessCount = 0;
                    }
                }
                return _field.get(target);
            }
        }
        catch (IllegalAccessException illegalAccessException) {
            FieldValueException.throwException(illegalAccessException);
        }
        return null;
    }

    public String toString ()
    {
        return Fmt.S("<%s field: %s>", getClass().getName(), _field);
    }
}
