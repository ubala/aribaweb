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

    $Id: //ariba/platform/util/expr/ariba/util/expr/TypeConversionHelper.java#8 $
*/

package ariba.util.expr;

import ariba.util.expr.ExprOps;
import ariba.util.core.ClassUtil;
import ariba.util.core.StringUtil;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldtype.PrimitiveTypeProvider;
import ariba.util.fieldtype.NullTypeInfo;
import ariba.util.fieldtype.TypeRetriever;
import ariba.util.formatter.Formatter;
/**
 *
 * @aribaapi private
*/
public class TypeConversionHelper
{
    /**
     * Find out if the type <code>convertedFrom</code> is compatible with
     * the type <code>convertedTo</code>.
     * @param provider
     * @param convertedTo
     * @param convertedFrom
     */
    public static boolean isCompatible (TypeRetriever provider,
                                        String convertedTo,
                                        String convertedFrom)
    {
        if (StringUtil.nullOrEmptyOrBlankString(convertedTo) ||
            StringUtil.nullOrEmptyOrBlankString(convertedFrom)) {
            return false;
        }

        TypeInfo convertedToType = provider.getTypeInfo(convertedTo);
        TypeInfo convertedFromType = provider.getTypeInfo(convertedFrom);

        if (convertedToType == null || convertedFromType == null) {
            return false;
        }

        return convertedToType.isCompatible(convertedFromType);
    }

    /**
     * Find out if the type <code>convertedFrom</code> can be widening to
     * the type <code>convertedTo</code>.  If both are primtive types, then
     * this method will check if primtive narrowing is possible.
     * @param convertedTo
     * @param convertedFrom
     */
    public static boolean canCastTo (TypeInfo convertedTo,
                                     TypeInfo convertedFrom)
    {
        if (canWideningTo(convertedTo, convertedFrom)) {
            return true;
        }

        // if both convertedTo and covertedFrom are primitive, then
        // check if they can be narrowed.
        if (PrimitiveTypeProvider.isSupportedType(convertedTo.getName())&&
            PrimitiveTypeProvider.isSupportedType(convertedFrom.getName())) {
            return canWideningTo(convertedFrom, convertedTo);
        }

        return false;
    }

    /**
     * Find out if the type <code>convertedFrom</code> can be widening to
     * the type <code>convertedTo</code>.
     * @param convertedTo
     * @param convertedFrom
     */
    public static boolean canWideningTo (TypeInfo convertedTo,
                                         TypeInfo convertedFrom)
    {
        // If the convertedTo is "Null", then it is always false.
        if (convertedTo instanceof NullTypeInfo) {
            return false;
        }

        // If the convertedFrom is "Null" and the convertedTo is a simple
        // (unboxed) primitive type, then widening is not allowed.
        if (convertedFrom instanceof NullTypeInfo &&
            PrimitiveTypeProvider.isUnboxedPrimitiveType(convertedTo)) {
            return false;
        }

        // Check Reference Widening
        if (convertedTo.isWideningTypeOf(convertedFrom)) {
            return true;
        }

        return false;
    }

    /**
     * Find out if the two types are the same.
     * @param convertedTo
     * @param convertedFrom
     */
    public static boolean exactMatch (TypeInfo convertedTo,
                                      TypeInfo convertedFrom)
    {
        return convertedTo.getName().equals(convertedFrom.getName());
    }

    /**
     * This method converts the <code>source</code> to an instance of
     * type <code>convertTo</code> if <code>source</code> is a primitive
     * and <code>convertTo</code> is also a primitive type.   Otherwise,
     * this method return the <code>source</code> withou conversion.
     * @param convertTo
     * @param source
     */
    public static Object convertPrimitive (TypeInfo convertTo, Object source)
    {
        if (convertTo == null || source == null) {
            return source;
        }
        return convertPrimitive(convertTo.getName(), source);
    }

    /**
     * This method converts the <code>source</code> to an instance of
     * type <code>convertTo</code> if <code>source</code> is a primitive
     * and <code>convertTo</code> is also a primitive type.   If the
     * <code>convertTo</code> is a string and the <code>convertFrom</code>
     * is a character, then conversion is allowed.  Otherwise,
     * this method return the <code>source</code> without conversion.
     *
     * @param convertTo
     * @param source
     */
    public static Object convertPrimitive (String convertTo, Object source)
    {
        if (StringUtil.nullOrEmptyOrBlankString(convertTo) || source == null) {
            return source;
        }

        Object result = source;
        Class toClass = PrimitiveTypeProvider.getClassForType(convertTo);
        Class fromClass = PrimitiveTypeProvider.getClassForType(
                                           source.getClass().getName());

        if (toClass != null && toClass == fromClass) {
            return source;
        }


        if (toClass != null && fromClass != null) {
            // For PRIMITIVE types, they are convertible from one another.
            // For the special case where converting from a char to string,
            // we have to explicitly check for it -- since this method cannot
            // use the TypeInfo to check if the type can be widened.
            Object newValue = ExprOps.convertValue(result, toClass);
            result = (newValue != null ? newValue : result);
        }
        else if (convertTo.equals(String.class.getName()) &&
                 source.getClass().getName().equals(Character.class.getName())) {
            // If we are trying to convert to String (which is not a primitve),
            // and the object is a character.  Then we can still convert.
            Object newValue = ExprOps.convertValue(result, String.class);
            result = (newValue != null ? newValue : result);
        }
        else if (convertTo.equals(Boolean.class.getName()) ||
                 convertTo.equals(Boolean.TYPE.getName())) {
            // Convert for boolean.  For example, null object will be converted
            // to boolean false.
            result = Boolean.valueOf(ExprOps.booleanValue(source));
        }
        else if (convertTo.equals(String.class.getName())) {
            // If the target type is String, then use the formatter to convert.
            result = Formatter.getStringValue(source);
        }
        // This is the fallback case.  If the object's type is not the same
        // as the target type, then it will type to convert.  Otherwise, just
        // return the source object (since the type matches).
        /*
        else if (source != null && !source.getClass().getName().equals(convertTo)) {
            // If conversion fails (the object is not convertible), then
            // return null.
            Object newValue = ExprOps.convertValue(result, toClass);
            result = (newValue != null ? newValue : null);
        }
        */

        return result;
    }

    public static boolean canConvert (Class convertTo, Object source)
    {
        if (convertTo == null || source == null) {
            return false;
        }

        // Exact match
        if (convertTo == source.getClass()) {
            return true;
        }
        // If the target is a primitive class, then check if the target is
        // a widening type of the source
        else if ( PrimitiveTypeProvider.isSupportedType(convertTo.getName()) ) {
            if (PrimitiveTypeProvider.isWideningTypeOf(
                                         convertTo.getName(),
                                         source.getClass().getName())) {
                return true;
            }
        }
        // If the target is string, then check if target is char.
        else if (convertTo.equals(String.class.getName()) &&
                 source.getClass().getName().equals(Character.class.getName())) {
            return true;
        }
        // If class is not primitive, then check object is assignment-compatible.
        else if (convertTo.isInstance(source)) {
            return true;
        }

        return false;
    }

    public static Class getClassForType (String name)
    {
        Class primitiveClass = PrimitiveTypeProvider.getClassForType(name);
        return (primitiveClass != null ? primitiveClass :
                ClassUtil.classForName(name,false));
    }

}

