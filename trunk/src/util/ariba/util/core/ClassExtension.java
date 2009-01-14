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

    $Id: //ariba/platform/util/core/ariba/util/core/ClassExtension.java#7 $
*/

package ariba.util.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
The ClassExtension class is the abstract superclass for all ClassExtension subclasses.
This class provides a slot for the intended class for the ClassExtension as well
as an implementation of the clone() method.  Both these methods are used by
the ClassExtensionRegistry class which provide a convenient mechanism for caching
Categories by their corresponding class.

A ClassExtension is a collection of behavior/methods that can be associated with
an existing class.  Often, this is serves as a way to provide a concrete
implementation for an 'interface' and attach that interface to existing classes.
For example, the FieldValue interface uses the FieldValue_Object as the base
implementation which is associated with the Object class.  Categories can be
subclassed when some of the behavior in a superclass' classExtension needs to be
overridden.  Often, a Cateogry will be implemented as a static innerclass so
it may have access to the private instance variables of the class with which
the ClassExtension is being associated.

It should be noted that the ClassExtensionRegistry class will clone its registered
Categories as needed to flesh out the cache so that each class which needs a
Cateogry from the cache will be able to get it directly without traversing up
the superclass chain.  This is why we expose the setForClass() method -- to allow
the ClassExtensionRegistry object the ability to clone and reset the forClass for each
subclass for which the ClassExtension applies.

The ClassExtensionRegistry uses the forClass variable to determine if the previousClassExtension
can be used for the current target object.

@aribaapi private

*/
abstract public class ClassExtension extends Object implements Cloneable
{
    private static final Class ClassExtensionClass = ClassExtension.class;
    public Class forClass;

    /**
    Sets the forClass variable which indicates for which class this
    copy of the classExtension applies.

    @param classValue the class for which this copy of the ClassExtension applies.
    */
    public void setForClass (Class classValue)
    {
        forClass = classValue;
    }

    /**
    Returns the forClass variable which indicates for which class this
    copy of the classExtension applies.

    @return the class for which this copy of the ClassExtension applies.
    */
    public Class forClass ()
    {
        return forClass;
    }

    /**
    Allows for cloning existing Categories so that the same ClassExtension
    may be cached multiple times, once for each subclass that inherits the ClassExtension.

    @return a new ClassExtension instance with the same instance variables
    */
    public Object clone ()
    {
        ClassExtension classExtension = null;
        try {
            classExtension = (ClassExtension)super.clone();
        }
        catch (CloneNotSupportedException exception) {
            throw new InternalError("Error in clone(). This shouldn't happen.");
        }
        return classExtension;
    }

    /**
     * Returns the real Class for the target object; if target is a kind of
     * ClassProxy, call getRealClass on it, otherwise just call getClass.
     * Throws NullPointerException if target is null.
     * @aribaapi private
     */
    public static Class getRealClass (Object target)
    {
        Class targetClass;
        if (target instanceof ClassProxy) {
            targetClass = ((ClassProxy)target).getRealClass();
        }
        else {
            targetClass = target.getClass();
        }
        return targetClass;
    }

    /**
        A utility method to determine if to methods are equivalent
        (based on their names and method signatures)

     @param method1 a method to be comared
     @param method2 a method to be comared
     @return a boolean inidcating if the two methods are equal base don comparing their
     names, parameters types, and return type.
    */
    private boolean methodsAreEqual (Method method1, Method method2)
    {
        boolean methodsAreEqual = method1 == method2;
        if (!methodsAreEqual) {
            methodsAreEqual = method1.getName().equals(method2.getName());
            if (methodsAreEqual) {
                Class[] parameterTypes1 = method1.getParameterTypes();
                Class[] parameterTypes2 = method2.getParameterTypes();
                methodsAreEqual = parameterTypes1.length == parameterTypes2.length;
                if (methodsAreEqual) {
                    for (int index = parameterTypes1.length - 1; index > -1; index--) {
                        Class parameter1 = parameterTypes1[index];
                        Class parameter2 = parameterTypes2[index];
                        if (parameter1 != parameter2) {
                            methodsAreEqual = false;
                            break;
                        }
                    }
                }
            }
        }
        return methodsAreEqual;
    }

    protected void checkForCollisions (ClassExtension otherClassExtension)
    {
        Class thisClass = getClass();
        Class otherClassExtensionClass = otherClassExtension.getClass();
        if (otherClassExtensionClass != thisClass) {
            Method[] methods = thisClass.getMethods();
            Method[] otherMethods = otherClassExtension.getClass().getMethods();
            for (int index = methods.length - 1; index > -1; index--) {
                Method method = methods[index];
                if (method.getDeclaringClass() != ClassExtensionClass &&
                    !Modifier.isStatic(method.getModifiers())) {
                    for (int otherIndex = otherMethods.length - 1; otherIndex > -1;
                         otherIndex--) {
                        Method otherMethod = otherMethods[otherIndex];
                        Class otherDeclaringClass = otherMethod.getDeclaringClass();
                        if (ClassExtensionClass.isAssignableFrom(otherDeclaringClass) &&
                            methodsAreEqual(method, otherMethod)) {
                            String message = Fmt.S(
                            "ClassExtension/ClassExtension method collision between: " +
                                                   "%s/%s and %s/%s.",
                            thisClass, method, otherClassExtensionClass, otherMethod);
                            throw new RuntimeException(message);
                        }
                    }
                }
            }
        }
    }

    private boolean methodEqualsExtensionMethod (Method regularMethod,
                                                 Method extensionMethod)
    {
        boolean methodsAreEqual =
            regularMethod.getName().equals(extensionMethod.getName());
        if (methodsAreEqual) {
            Class[] regularParameterTypes = regularMethod.getParameterTypes();
            Class[] extensionParameterTypes = extensionMethod.getParameterTypes();
            methodsAreEqual =
                regularParameterTypes.length == (extensionParameterTypes.length - 1);
            if (methodsAreEqual) {
                // There may be methods in the extension that are not intended
                // for use as extensions (eq wait(long)), so only compare those
                // which apply (ie == forClass)
                methodsAreEqual = extensionParameterTypes[0] == forClass;
                if (methodsAreEqual) {
                    for (int index = regularParameterTypes.length - 1; index > -1;
                        index--) {
                        Class regularParameter = regularParameterTypes[index];
                        Class extensionParameter = extensionParameterTypes[index + 1];
                        if (regularParameter != extensionParameter) {
                            methodsAreEqual = false;
                            break;
                        }
                    }
                }
            }
        }
        return methodsAreEqual;
    }

    protected void checkForCollisions (Class otherClass)
    {
        Method[] otherMethods = otherClass.getDeclaredMethods();
        Method[] extensionMethods = getClass().getMethods();
        for (int index = extensionMethods.length - 1; index > -1; index--) {
            Method currentExtensionMethod = extensionMethods[index];
            for (int otherIndex = otherMethods.length - 1; otherIndex > - 1;
                otherIndex--) {
                Method otherMethod = otherMethods[otherIndex];
                if (methodEqualsExtensionMethod(otherMethod, currentExtensionMethod)) {
                    String message = Fmt.S(
                    "ClassExtension/Class method collision between: \"%s\" and \"%s\".",
                    currentExtensionMethod, otherMethod);
                    throw new RuntimeException(message);
                }
            }
        }
        Class otherSuperclass = otherClass.getSuperclass();
        if (otherSuperclass != null) {
            checkForCollisions(otherSuperclass);
        }
    }

    protected void checkForCollisions ()
    {
        try {
            List classExtensions = ClassExtensionRegistry.getClassExtensions(forClass);
            int classExtensionsSize = classExtensions.size();
            for (int index = 0; index < classExtensionsSize; index++) {
                ClassExtension currentClassExtension =
                    (ClassExtension)classExtensions.get(index);
                checkForCollisions(currentClassExtension);
            }
            //checkForCollisions(forClass);
        }
        catch (RuntimeException exception) {
            exception.printStackTrace();
            throw exception;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            throw new WrapperRuntimeException(exception);
        }
    }
}
