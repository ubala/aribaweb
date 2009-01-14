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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ReflectionMethodGetter.java#3 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.Fmt;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
The ReflectionMethodGetter is a wrapper for java.lang.reflect.Method
and provides a uniform interface so that users needn't know if the accesor
they're dealing with is either a Method or a Field.
*/
public class ReflectionMethodGetter extends ReflectionMethodAccessor implements FieldValueGetter
{
    public static final Object[] EmptyArray = new Object[0];

    /**
    Constructs a new ReflectionMethodGetter with the given Class and Method.

    @param forClass the class for which the accessor should apply
    @param method the Method which this instance wraps
    */
    protected ReflectionMethodGetter (Class forClass, String fieldName, Method method)
    {
        super(forClass, fieldName, method);
    }

    /**
    Provides an instance of ReflectionMethodGetter for the given class and
    fieldName.  This recurses up the targetClass' superclass chain looking
    for an appropriate method.

    @param targetClass the class on which to start looking for a method
    matching targetFieldName
    @param targetFieldName the name of the field indicating which method we want
    @param currentClass the class of the current recusive iteration
    @return the ReflectionMethodGetter wrapping the Method found for
    targetClass and targetFieldName or null if not found.
    */
    private static FieldValueGetter newInstance (Class targetClass,
                                            String targetFieldName, Class currentClass)
    {
        Method getterMethod =
            FieldValueAccessorUtil.lookupGetterMethod(targetClass, targetFieldName);
        return (getterMethod == null) ? null :
            new ReflectionMethodGetter(targetClass, targetFieldName, getterMethod);
    }

    /**
    Provides an instance of ReflectionMethodGetter for the given class and
    fieldName.  Calls in interal recursive method to do the actual lookup.
    Use this static method to allow us to provide caching of these accessor
    if necessary in the future, however this is not done currently.

    @param targetClass the class on which to start looking for a method
    matching targetFieldName
    @param targetFieldName the name of the field indicating which method we want
    @return the ReflectionMethodGetter wrapping the Method found for
    targetClass and targetFieldName or null if not found.
    */
    protected static FieldValueGetter newInstance (Class targetClass,
                                                String targetFieldName)
    {
        return newInstance(targetClass, targetFieldName, targetClass);
    }

    /**
    Invokes the method of the accessor (treating it as a getter) with the
    target and args.  This is a simple
    cover for Method.invoke(...) and allows for subclasses to override to get the
    desired behavior.

    @param target the target object
    @return the result of invoking the getter
    */
    protected Object invokeGetMethod (Object target)
        throws InvocationTargetException, IllegalAccessException
    {
        if (_compiledAccessor != null) {
            return _compiledAccessor.getValue(target);
        }
        else {
            _accessCount++;
            if (CompiledAccessorFactory.compiledAccessorThresholdPassed(_accessCount)) {
                _compiledAccessor = CompiledAccessorFactory.newInstance(_method, false);
                if (_compiledAccessor == null) {
                    _accessCount = 0;
                }
            }
            return _method.invoke(target, EmptyArray);
        }
    }

    /**
    Invokes the underlying reflection Method on the target and returns
    the resulting value.

    @param target the object on which the underlying Method will be invoked to get the value from
    @return the value of invoking the underlying method on target
    */
    public Object getValue (Object target)
    {
        Object value = null;
        try {
            value = invokeGetMethod(target);
        }
        catch (IllegalAccessException illegalAccessException) {
            FieldValueException.throwException(illegalAccessException);
        }
        catch (InvocationTargetException invocationTargetException) {
            Throwable targetException =
                invocationTargetException.getTargetException();
            FieldValueException.throwException(targetException);
            return null;
        }
        return value;
    }

    public String toString ()
    {
        return Fmt.S("<%s method: %s>", getClass().getName(), _method);
    }
}
