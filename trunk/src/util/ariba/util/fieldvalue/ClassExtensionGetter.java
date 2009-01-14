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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ClassExtensionGetter.java#3 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.Fmt;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
A special FieldValueAccessor subclass which supports the use of the
FieldValue subsystem to invoke ClassExtension methods.  The main
difference between a ClassExtension method and a normal method
(implemented on the targetClass proper) is that the first argument
of a ClassExtension method is always the targetObject.  The
ClassExtensionGetter handles this case by invoking the ClassExtension
methods with the ClassExtension as the target of the invocation and
by passing the actual targte as the first argument in the args array.
*/
public class ClassExtensionGetter extends ReflectionMethodGetter
{
    private static final int ArgValuesCount = 1;
    private final ClassExtension _classExtension;
    protected Object[] _sharedArgValuesArray;

    /**
    Allocate and initialize a new ClassExtensionGetter for the named ClassExtension,
    the class of the target object, and the name of the field.  This first attempts to
    locate the appropriate class extension for the given name and target class.  If it
    cannot be found, throws a FieldValueException with an informative message.  Otherwise,
    this will lookup the appropriate getter method for the classExtension and
    fieldName.

    @param targetClass the class of the target object
    @param fieldName the name of the field for which the accessor applies
    @return a newly constructed ClassExtensionGetter for getting values
    */
    public static FieldValueGetter newInstance (Class targetClass, String fieldName)
    {
        ClassExtensionGetter classExtensionGetter = null;
        List classExtensions = ClassExtensionRegistry.getClassExtensions(targetClass);
        int classExtensionsSize = classExtensions.size();
        if (classExtensionsSize > 0) {
            Method method = null;
            for (int index = 0; index < classExtensionsSize; index++) {
                ClassExtension currentClassExtension  =
                    (ClassExtension)classExtensions.get(index);
                Class classExtensionClass = currentClassExtension.getClass();
                method = FieldValueAccessorUtil.lookupGetterMethod(classExtensionClass,
                                                                          fieldName, 1);
                if (method != null) {
                    classExtensionGetter = new ClassExtensionGetter(targetClass,
                        currentClassExtension, fieldName, method);
                    break;
                }
            }
        }
        else {
            throw new FieldValueException(
                Fmt.S("Cannot locate ClassExtensions for class \"%s\"", targetClass));
        }
        return classExtensionGetter;
    }

    /**
    Constructs a ClassExtensionGetter

    @param targetClass the class of the target object
    @param fieldName the name of the field for which the accessor applies
    @return a newly constructed ClassExtensionGetter
    */
    private ClassExtensionGetter (Class targetClass, ClassExtension classExtension,
                                    String fieldName, Method method)
    {
        super(targetClass, fieldName, method);
        _classExtension = classExtension;
    }

    /**
    Overrides the superclass' implementation to make the classExtension for the accessor
    the target of the method invocation, and to make the actual target object be the first
    argument of the invocation.  This is compliant with the convention that all classExtension
    methods are invoked on the classExtension itself with the first argument being the
    actual target object.

    @param target the target object
    @return the result of invoking the getter
    */
    protected Object invokeGetMethod (Object target)
        throws InvocationTargetException, IllegalAccessException
    {
        // This does a 'checkout' of _sharedArgValuesArray
        Object[] argValuesArray = null;
        synchronized (this) {
            argValuesArray = _sharedArgValuesArray;
            _sharedArgValuesArray = null;
        }
        if (argValuesArray == null) {
            argValuesArray = new Object[ArgValuesCount];
        }
        argValuesArray[0] = target;
        Object value = _method.invoke(_classExtension, argValuesArray);
        argValuesArray[0] = null;
        // ...and this checks it back in
        _sharedArgValuesArray = argValuesArray;
        return value;
    }
}
