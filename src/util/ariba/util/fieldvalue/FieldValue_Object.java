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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValue_Object.java#5 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.Fmt;
import ariba.util.core.MultiKeyHashtable;

/**
The FieldValue_Object class extension is the default implementation of the
FieldValue interface.  This default implementation is based
on java.lang.reflection primitives and will work for essentailly
all classes.  Of course, certain subclasses may desire to alter
the default behavior and, as such, should subclass this class and
override the appropriate methods.  For example, a Map
implementation may want to treat the fieldName as a key for get/put
rather than as the name of an instance variable.  Some other subclass
may desire to mix and match the two approaches.  In any case, these
special cases should be implemented in subclasses of FieldValue_Object.
*/
public class FieldValue_Object extends FieldValue
{
    private static final FieldValueAccessor NotFoundAccessor = new NotFoundFieldValueAccessor();
    protected final MultiKeyHashtable[] _accessorsHashtable = {
        new MultiKeyHashtable(2, 8 , true),
        new MultiKeyHashtable(2, 8, true),
    };

    /**
    Creates and returns a new FieldValueAccessor (by default,
    a ReflectionFieldValueAccessor) for the given target and fieldName.
    No caching is done by this method. This method is designed to be
    overridden by subclasses of FieldValue_Object which want to define
    their own specialized accessors.  Note that the target is passed
    rather than its class so that accessors can be created at a finer
    granularity than class.  Certain meta-data driven classes require
    this flexibility.

    @param target the object for which the accessor will be created
    @param fieldName the name of the field for which the accessor will be created
    @param type the type of accessor will be created (either FieldValue.Setter
        or FieldValue.Getter)
    @return a new FieldValueAccessor (ReflectionFieldValueAccessor by default)
    */
    public FieldValueAccessor createAccessor (Object target, String fieldName, int type)
    {
        FieldValueAccessor accessor = null;

        if (fieldName.equals("this")) {
            fieldName = "getThis";
        }

        if (type == Setter) {
            accessor = FieldValueAccessorUtil.newReflectionSetter(target.getClass(), fieldName);
        }
        else {
            accessor = FieldValueAccessorUtil.newReflectionGetter(target.getClass(), fieldName);
        }
        return accessor;
    }

    /**
    Maintains a cache of FieldValueAccessor's for the instance by fieldName.
    In general, all instances of a given class will share the same
    FieldValueAccessor for a given fieldName.  However, certain meta-data
    driven classes may need to introspect upon the instance before returning
    the proper accessor.

    @param target the object for which the accessor will be looked up
    @param fieldName the name of the field for the accessor
    @param type the type of accessor will be created (either FieldValue.Setter
        or FieldValue.Getter)
    @return the cached FieldValueAccessor (ReflectionFieldValueAccessor by default)
    */
    public FieldValueAccessor getAccessor (Object target, String fieldName, int type)
    {
        FieldValueAccessor accessor = null;
        Class targetObjectClass = target.getClass();
        MultiKeyHashtable accessorsHashtable = _accessorsHashtable[type];
        synchronized (accessorsHashtable) {
            fieldName = fieldName.intern();
            accessor = (FieldValueAccessor)accessorsHashtable.get(targetObjectClass,
                                                                  fieldName);
            if (accessor == NotFoundAccessor) {
                accessor = null;
            }
            else if (accessor == null) {
                accessor = createAccessor(target, fieldName, type);
                accessorsHashtable.put(targetObjectClass, fieldName,
                                        (accessor != null) ? accessor : NotFoundAccessor);
            }
        }
        return accessor;
    }

    /**
    Sets the value on the reveiver using the fieldName indicated by
    fieldPath -- only the first node of the fieldPath is considered if it is
    a multi-node path.

    @param target the object on which the value will be set for the field identiifed by fieldPath
    @param fieldPath the fieldPath node (which contains a candidate accessor) to be
    used to set the value on target.
    @param value the value to set on the target
    */
    public void setFieldValuePrimitive (Object target, FieldPath fieldPath, Object value)
    {
        FieldValueSetter setter = fieldPath._previousSetter;
        boolean isAccessorApplicable = (target.getClass() == setter.forClass())
            && setter.isApplicable(target);
        if (!isAccessorApplicable) {
            setter = (FieldValueSetter)getAccessor(target, fieldPath._fieldName, Setter);
            if (setter == null) {
                String message = Fmt.S(
                    "Unable to locate setter method or " +
                    "field for: \"%s\" on target class: \"%s\"",
                    fieldPath._fieldName, target.getClass().getName());
                throw new FieldValueException(message);
            }
            fieldPath._previousSetter = setter;
        }
        setter.setValue(target, value);
    }

    /**
    Gets the value from the reveiver using the fieldName indicated by
    fieldPath -- only the first node of the fieldPath is considered if
    it is a multi-node path.

    @param target the object from which to get the value of the field identified by fieldPath
    @param fieldPath the fieldPath node which identifes the field to get.
    @return the value obtained from the target using the fieldPath
    */
    public Object getFieldValuePrimitive (Object target, FieldPath fieldPath)
    {
        FieldValueGetter getter = fieldPath._previousGetter;
        boolean isAccessorApplicable = (target.getClass() == getter.forClass())
            && getter.isApplicable(target);
        if (!isAccessorApplicable) {
            getter = (FieldValueGetter)getAccessor(target, fieldPath._fieldName, Getter);
            if (getter == null) {
                String message = Fmt.S(
                    "Unable to locate getter method or " +
                    "field for: \"%s\" on target class: \"%s\"",
                    fieldPath._fieldName, target.getClass().getName());
                throw new FieldValueException(message);
            }
            fieldPath._previousGetter = getter;
        }
        return getter.getValue(target);
    }

    /**
    Recursively calls getFieldValuePrimitive() with the head of the fieldPath
    list up to the last fieldPath node and then calls setFieldValuePrimitive().
    Each time the recursion iterates, the receiver is the value of the
    previous getFieldValuePrimitive().

    @param target the object on which to start the recursion for setting
    the value using the fieldPath
    @param fieldPath the linked list of fieldPath nodes used to navigate from
    target to the final object in the chain, upon which the value is set
    @param value the value which is set on the final object in the chain
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
            if (nextTargetObject != null) {
                fieldPathCdr.setFieldValue(nextTargetObject, value);
            }
        }
    }

    /**
    Recursively calls getFieldValuePrimitive() with the head of the fieldPath list.
    Each time the recursion iterates, the receiver is the value of the previous
    getFieldValuePrimitive().

    @param target the first object from which to start the recursion for getting
    the value identified by the fieldPath.
    @param fieldPath the linked list of fieldPath nodes that identifes the value to get
    @return the value obtained from the last object in the chain
    */
    public Object getFieldValue (Object target, FieldPath fieldPath)
    {
        Object value = getFieldValuePrimitive(target, fieldPath);
        FieldPath fieldPathCdr = fieldPath._nextFieldPath;
        if (fieldPathCdr != null && value != null) {
            value = fieldPathCdr.getFieldValue(value);
        }
        return value;
    }

    public void populateFieldInfo (Class targetClass, FieldInfo.Collection collection)
    {
        Class superCls = targetClass.getSuperclass();
        if (superCls != null) populateFieldInfo(superCls, collection);
        if (collection.includeFields()) {
            FieldValueAccessorUtil.popuplateFromFields(targetClass, collection);
        }
        FieldValueAccessorUtil.popuplateFromMethods(targetClass, collection);
    }


    /*
    private boolean fieldEqualsExtensionMethod (Field field, Method extensionMethod)
    {
        String fieldName = field.getName();
        String methodName = extensionMethod.getName();
        return FieldValueAccessorUtil.matchForSetter(fieldName, methodName) ||
            FieldValueAccessorUtil.matchForGetter(fieldName, methodName);
    }

    protected void checkForCollisions (Class otherClass)
    {
        Method[] extensionMethods = getClass().getMethods();
        Field[] otherFields = otherClass.getDeclaredFields();
        for (int index = extensionMethods.length - 1; index > -1; index--) {
            Method currentExtensionMethod = extensionMethods[index];
            for (int otherIndex = otherFields.length - 1; otherIndex > - 1; otherIndex--) {
                Field otherField = otherFields[otherIndex];
                if (fieldEqualsExtensionMethod(otherField, currentExtensionMethod)) {
                    String message = Fmt.S(
                    "ClassExtension/Field collision between: " +
                    "%s/%s and %s/%s.",
                    getClass(), currentExtensionMethod, otherClass, otherField);
                    throw new RuntimeException(message);
                }
            }
        }
        super.checkForCollisions(otherClass);
    }
    */
}

/**
    A dummy/marker class used to maintain a negative cache for accessors which are not
    found.
*/
class NotFoundFieldValueAccessor extends BaseAccessor
{
    public NotFoundFieldValueAccessor ()
    {
        super(Object.class, "NotFound");
    }

    public Object getValue (Object target){return null;}
    public void setValue (Object target, Object value){}
}
