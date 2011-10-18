/*
    Copyright 1996-2011 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ReflectionMethodSetter.java#4 $
*/

package ariba.util.fieldvalue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
The ReflectionMethodSetter is a wrapper for java.lang.reflect.Method
and provides a uniform interface so that users needn't know if the accesor
they're dealing with is either a Method or a Field.
*/
public class ReflectionMethodSetter extends ReflectionMethodAccessor
      implements FieldValueSetter
{
 
    /**
    Constructs a new ReflectionMethodSetter with the given Class and Method.

    @param forClass the class for which the accessor should apply
    @param method the Method which this instance wraps
    */
    protected ReflectionMethodSetter (Class forClass, String fieldName, Method method)
    {
        super(forClass, fieldName, method);
    }

    /**
    Provides an instance of FieldValueAccessor for the given class and fieldName.
    Calls in interal recursive method to do the actual lookup.  Use this static
    method to allow us to provide caching of these accessor if necessary in the
    future, however this is not done currently.

    @param targetClass the class for which the set method should be looked up
    @param targetFieldName the desired fieldName.
    @return a new ReflectionMethodSetter for targetClass and targetFieldName
    */
    protected static FieldValueSetter newInstance (Class targetClass,
                                                        String targetFieldName)
    {
        Method setterMethod =
            FieldValueAccessorUtil.lookupSetterMethod(targetClass, targetFieldName);
        return (setterMethod == null) ? null :
            new ReflectionMethodSetter(targetClass, targetFieldName, setterMethod);
    }

    /**
    Invokes the method of the accessor (treating it as a setter) with the
    target and args.  This is a simple
    cover for Method.invoke(...) and allows for subclasses to override to get the
    desired behavior.

    @param target the target object
    @param value the value to assign
    */
    protected void invokeSetMethod (Object target, Object value)
        throws InvocationTargetException, IllegalAccessException
    {
        if (_compiledAccessor != null) {
            _compiledAccessor.setValue(target, value);
        }
        else {


              _method.invoke(target, value);

            _accessCount++;
            if (CompiledAccessorFactory.compiledAccessorThresholdPassed(_accessCount)) {
                _compiledAccessor = CompiledAccessorFactory.newInstance(_method, true);
                if (_compiledAccessor == null) {
                    _accessCount = 0;
                }
            }
        }
    }

    /**
    Invokes the underlying reflection Method on the target setting the value.

    @param target the object on which the value will be set using the wrapped Method
    @param value the value to be set on the target using the wrapped Method
    */
    public void setValue (Object target, Object value)
    {
        try {
            invokeSetMethod(target, value);
        }
        catch (IllegalAccessException illegalAccessException) {
            FieldValueException.throwException(illegalAccessException);
        }
        catch (InvocationTargetException invocationTargetException) {
            Throwable targetException =
                invocationTargetException.getTargetException();
            FieldValueException.throwException(targetException);
        }
    }
}
