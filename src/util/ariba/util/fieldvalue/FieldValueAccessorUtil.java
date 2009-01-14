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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValueAccessorUtil.java#5 $
*/

package ariba.util.fieldvalue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
Several static methods which are used in various places within the FieldValue subsystem to
handle certain reflection operations more gracefully.
*/
public class FieldValueAccessorUtil extends Object
{
    /**
    A cover which hides the try/catch block.

    @param targetClass the class on which to perform getDeclaredMethods
    @return an array of Methods
    */
    protected static Method[] getDeclaredMethods (Class targetClass)
    {
        try {
            return targetClass.getDeclaredMethods();
        }
        catch (SecurityException securityException) {
            throw new FieldValueException(securityException);
        }
    }

    /**
    A cover which hides the try/catch block.

    @param targetClass the class on which to perform getDeclaredFields
    @return an array of Fields
    */
    protected static Field[] getDeclaredFields (Class targetClass)
    {
        try {
            return targetClass.getDeclaredFields();
        }
        catch (SecurityException securityException) {
            throw new FieldValueException(securityException);
        }
    }

    /**
    Determines if firstString equals (secondStringPrefix + secondStringSuffix)
    without actually concatanting the strings.

    @param firstString any String
    @param secondStringPrefix any String
    @param secondStringSuffix any String
    @return a boolean indicating a match
    */
    protected static boolean equals (String firstString, String secondStringPrefix,
                                     String secondStringSuffix)
    {
        int secondStringPrefixLength = secondStringPrefix.length();
        int secondStringSuffixLength = secondStringSuffix.length();
        int secondStringLength = secondStringPrefixLength + secondStringSuffixLength;
        return (firstString.length() == secondStringLength)
            && firstString.regionMatches(0, secondStringPrefix, 0,
                                         secondStringPrefixLength)
            && firstString.regionMatches(secondStringPrefixLength,
                                         secondStringSuffix, 0, secondStringSuffixLength);
    }

    /**
    Determines targetFieldName can be made to match the methodName is the
    prefixString is prepended and the first character of targetFieldName
    is uppercased.  This method does not actually concatenate any strings together.

    @param targetFieldName the fieldName we're trying to match
    @param methodName the actual method name we're trying to mathc with the targetFieldName
    @param prefixString the prefix for the targetFieldName (usually either 'set' or 'get')
    @return a boolean indicating a match
    */
    protected static boolean matchWithPrefix (String targetFieldName,
                                              String methodName, String prefixString)
    {
        boolean doesMatch = false;
        int keyLength = targetFieldName.length();
        if (methodName.length() == (prefixString.length() + keyLength)) {
            if (methodName.startsWith(prefixString)) {
                if (methodName.regionMatches(4, targetFieldName, 1, (keyLength - 1))) {
                    if (methodName.charAt(3) ==
                        Character.toUpperCase(targetFieldName.charAt(0))) {
                        doesMatch = true;
                    }
                }
            }
        }
        return doesMatch;
    }

    /**
    Determines if the methodName can be used given the targetFieldName.
    The 'set' prefix is applied and the first letter of targetFieldName is capitalized.

    @param targetFieldName the desired field name
    @param methodName the actual method name
    @return wheter or not the two match according to the rules for matching setters
    */
    public static boolean matchForSetter (String targetFieldName, String methodName)
    {
        return FieldValueAccessorUtil.matchWithPrefix(targetFieldName, methodName, "set");
    }

    /**
    Determines if the methodName can be used given the targetFieldName.
    First, an excat match is attempted, then  the 'get' prefix is applied
    and the first letter of targetFieldName is capitalized.

    @param targetFieldName the desired field name
    @param methodName the actual method name
    @return wheter or not the two match according to the rules for matching getters
    */
    public static boolean matchForGetter (String targetFieldName,
                                           String methodName)
    {
        return targetFieldName.equals(methodName) ||
            FieldValueAccessorUtil.matchWithPrefix(targetFieldName, methodName, "get");
    }

    /**
    A convenience which does a lookup of a setter Method on a target class.
    This recurses up the class hierarchy until it finds the method (terminates
    at Object, of course).  The argCount parameter allows this method to perform
    lookups for either regular setters (argCount == 1) or for ClassExtension
    setters (argCount == 2).

    @param targetClass the class upon which to look for the method.  If method
    does not exist on targetClass, the superclass will be used and this method
    will be called recursively.
    @param targetFieldName the name of the field for which the setter method applies.
    Note that this would not include the "set" prefix.
    @param argCount the number of arguments expected for the target setter method.
    Generally, this should be 1, but for ClassExtension methods, this will be 2.
    */
    protected static Method lookupSetterMethod (Class targetClass,
        String targetFieldName, int argCount)
    {
        Method methodArray[] = getDeclaredMethods(targetClass);
        for (int index = methodArray.length - 1; index > -1; index--) {
            Method currentPrimitiveMethod = methodArray[index];
            if ((currentPrimitiveMethod.getParameterTypes().length == argCount) &&
                matchForSetter(targetFieldName, currentPrimitiveMethod.getName())) {
                return currentPrimitiveMethod;
            }
        }
        Method setterMethod = null;
        Class targetSuperclass = targetClass.getSuperclass();
        if (targetSuperclass != null) {
            setterMethod = lookupSetterMethod(targetSuperclass, targetFieldName,
                                              argCount);
        }
        return setterMethod;
    }

    /**
    A convenicence method which allows for lookup of single argument setter
    method without specifying the number of arguments (assumes argCount = 1).
    */
    protected static Method lookupSetterMethod (Class targetClass,
        String targetFieldName)
    {
        return lookupSetterMethod(targetClass, targetFieldName, 1);
    }

    /**
    Same as lookupSetterMethod except uses matchForGetter rather than matchForSetter.
    */
    protected static Method lookupGetterMethod (Class targetClass,
        String targetFieldName, int argCount)
    {
        Method methodArray[] = getDeclaredMethods(targetClass);
        for (int index = methodArray.length - 1; index > -1; index--) {
            Method currentPrimitiveMethod = methodArray[index];
            if ((currentPrimitiveMethod.getParameterTypes().length == argCount) &&
                matchForGetter(targetFieldName, currentPrimitiveMethod.getName())) {
                return currentPrimitiveMethod;
            }
        }
        Method getterMethod = null;
        Class targetSuperclass = targetClass.getSuperclass();
        if (targetSuperclass != null) {
            getterMethod = lookupGetterMethod(targetSuperclass, targetFieldName,
                                              argCount);
        }
        return getterMethod;
    }

    /**
    A convenicence method which allows for lookup of single argument getter
    method without specifying the number of arguments (assumes argCount = 1).
    */
    protected static Method lookupGetterMethod (Class targetClass,
        String targetFieldName)
    {
        return lookupGetterMethod(targetClass, targetFieldName, 0);
    }

    /**
    Creates a new instance of a FieldValueSetter based on java.lang.Reflect

    @param targetClass the class on which the desired setter is defined
    @param targetFieldName the name of the field for which the desired setter applies
    @return a new FieldValueSetter which will allow for setting the targetFieldName
        on instances of targetClass
    */
    protected static FieldValueSetter newReflectionSetter (Class targetClass,
                                                           String targetFieldName)
    {
        FieldValueSetter setter = ReflectionMethodSetter
            .newInstance(targetClass, targetFieldName);
        if (setter == null) {
            setter = ClassExtensionSetter.newInstance(targetClass, targetFieldName);
            if (setter == null) {
                setter = ReflectionFieldAccessor.newInstance(targetClass,
                                                             targetFieldName);
            }
        }
        return setter;
    }

    /**
    Creates a new instance of a FieldValueGetter based on java.lang.Reflect

    @param targetClass the class on which the desired getter is defined
    @param targetFieldName the name of the field for which the desired getter applies
    @return a new FieldValueGetter which will allow for getting the targetFieldName
        from instances of targetClass
    */
    protected static FieldValueGetter newReflectionGetter (Class targetClass,
                                                           String targetFieldName)
    {
        FieldValueGetter getter = ReflectionMethodGetter.
            newInstance(targetClass, targetFieldName);
        if (getter == null) {
            getter = ClassExtensionGetter.newInstance(targetClass, targetFieldName);
            if (getter == null) {
                getter = ReflectionFieldAccessor.newInstance(targetClass,
                                                             targetFieldName);
            }
        }
        return getter;
    }

    /**
    This performs a reflection Method invocation and does the appropriate exception
    handling as per the FieldValue subsystem.  That is, it catches InvocationTargetExceptions
    and rethrows the targetException wrapped in a FieldValueException.  Also,
    IllegalAccessExceptions are rethrown wrapped in a FieldValueException.

    @param method a java.lang.reflect.Method to be invoked
    @param target the object for which the Method applies
    @param args the array of args to the method
    @return the result of the method invocation
    */
    protected static Object invokeMethod (Method method, Object target, Object[] args)
    {
        try {
            return method.invoke(target, args);
        }
        catch (InvocationTargetException invocationTargetException) {
            Throwable targetException =
                invocationTargetException.getTargetException();
            FieldValueException.throwException(targetException);
            return null;
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new FieldValueException(illegalAccessException);
        }
    }

    static protected void popuplateFromFields(Class targetClass, FieldInfo.Collection collection)
    {
        Field fieldArray[] = FieldValueAccessorUtil.getDeclaredFields(targetClass);
        for (int i=0, c=fieldArray.length; i < c; i++) {
            Field field = fieldArray[i];
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                String name = field.getName();
                boolean isPublic = (field.getModifiers() & Modifier.PUBLIC) != 0;
                if (name.startsWith("_")) name = name.substring(1);
                collection.updateInfo (name, field.getType(), isPublic, isPublic, isPublic,
                                           null, null, field);
            }
        }
    }

    static protected void popuplateFromMethods(Class targetClass, FieldInfo.Collection collection)
    {
        Method methodArray[] = getDeclaredMethods(targetClass);
        for (int i=0, c=methodArray.length; i < c; i++) {
            Method method = methodArray[i];
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                String name = method.getName();
                boolean isPublic = (method.getModifiers() & Modifier.PUBLIC) != 0;
                Class[] params = method.getParameterTypes();
                int pCount = params.length;
                if (pCount == 0) {
                    if (name.startsWith("get") && name.length() > 3) {
                        name = String.valueOf(Character.toLowerCase(name.charAt(3))) + name.substring(4);
                    }
                    else if (!collection.includeNonBeanStyleGetters()) continue;
                    
                    collection.updateInfo (name, method.getReturnType(), isPublic, true, false,
                                               method, null, null);
                }
                else if ((pCount == 1) && name.startsWith("set") && (name.length() > 3)) {
                    name = String.valueOf(Character.toLowerCase(name.charAt(3))) + name.substring(4);
                    collection.updateInfo (name, params[0], isPublic, false, true,
                                               null, method, null);
                }
            }
        }
    }
    
    /**
        Determines the generic field path for the member.

        @param element the class element can be a field or method
        @return String 
    */
    public static String normalizedFieldPathForMember (Member element)
    {
        String fieldPath = null;
        if (Modifier.isPublic(element.getModifiers())) {
            if (element instanceof Method) {
                Method mtd = (Method)element;
                if (mtd.getName().startsWith("get")) {
                    fieldPath = mtd.getName().substring(3);
                }
                else if (mtd.getGenericParameterTypes().length == 0) {
                    fieldPath = mtd.getName();
                }
            }
            else if (element instanceof Field) {
                if (((Field)element).getName().startsWith("_")) {
                    fieldPath = ((Field)element).getName().substring(1);
                }
                else {
                    fieldPath = ((Field)element).getName().substring(1);
                }
            }
        }
        return fieldPath;
    }
}
