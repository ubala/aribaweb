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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/PrimitiveTypeProvider.java#14 $
*/

package ariba.util.fieldtype;

import java.util.Map;
import java.util.List;
import java.math.BigInteger;
import java.math.BigDecimal;
import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;

/**
    @aribaapi private
*/
public class PrimitiveTypeProvider extends TypeProvider
{
    // ----------------------------------------------------------------------
    // Private data members

    private static TypeProvider DefaultTypeProvider = new PrimitiveTypeProvider();

    private static Map _classMap = MapUtil.map();

    // ------------------------------------------------------------------
    // Declaration of data types supported in this TypeProvider.

    // The order of the supportedTypes are specified based on the type
    // numbering scheme defined NumericTypes.
    static List SupportedTypes = ListUtil.list(
        Boolean.TYPE,
        Byte.TYPE,
        Character.TYPE,
        Short.TYPE,
        Integer.TYPE,
        Long.TYPE,
        BigInteger.class,
        Float.TYPE,
        Double.TYPE,
        BigDecimal.class
    );

    static List BoxedTypes = ListUtil.list(
        Boolean.class,
        Byte.class,
        Character.class,
        Short.class,
        Integer.class,
        Long.class,
        BigInteger.class,
        Float.class,
        Double.class,
        BigDecimal.class
    );

    /**
     * The list of simple (build-in) types of Java type system.
     */
    static List SimpleTypes = ListUtil.list(
        Boolean.TYPE,
        Byte.TYPE,
        Character.TYPE,
        Short.TYPE,
        Integer.TYPE,
        Long.TYPE,
        null,
        Float.TYPE,
        Double.TYPE,
        null
    );

    /**
     * Widening order of numeric types
     */
    static List NumericTypeWideningOrder = ListUtil.list(
        Byte.class,
        Short.class,
        Character.class,
        Integer.class,
        Long.class,
        BigInteger.class,
        Float.class,
        Double.class,
        BigDecimal.class
    );

    /**
     * The list of numeric types supported in java type system.
     */
    static List NumericDataTypes = ListUtil.list(
        null,
        Byte.class,
        null,
        Short.class,
        Integer.class,
        Long.class,
        BigInteger.class,
        Float.class,
        Double.class,
        BigDecimal.class
    );

    /**
     * Each element in this list represents an encoding name for a simple
     * type.  The encoding name is used to construct the class name for
     * array of a simple type.  The class name for an array class for simple
     * type has the following syntax "[*(encoding)".  For a 3 dimension integer
     * array, the class name is "[[[I".
     */
    static List ArrayElementEncoding = ListUtil.list(
        "Z",
        "B",
        "C",
        "S",
        "I",
        "J",
        null,
        "F",
        "D",
        null
    );

    /**
     * A character used in the class name to indicate the dimension of an
     * array element.
     */
    static final char ArrayDimensionIndicator = '[';

    /**
     * Void data type.
     */
    static final PrimitiveTypeInfo VoidTypeInfo = new PrimitiveTypeInfo(-1, Void.TYPE);

    // ---------------------------------------------------------------------
    // Static Initialization

    static {
         for (int i=0; i < SupportedTypes.size(); i++) {
            _classMap.put(((Class)SupportedTypes.get(i)).getName(),
                           createTypeInfo(i, (Class)SupportedTypes.get(i)));
        }
        for (int i=0; i < BoxedTypes.size(); i++) {
            _classMap.put(((Class)BoxedTypes.get(i)).getName(),
                           createTypeInfo(i, (Class)BoxedTypes.get(i)));
        }
        _classMap.put(VoidTypeInfo.getName(), VoidTypeInfo);
    }

    // ---------------------------------------------------------------------
    // TypeProvider implementation

    private PrimitiveTypeProvider ()
    {
        super(PrimitiveTypeProviderId);
    }

    public static TypeProvider instance ()
    {
        return DefaultTypeProvider;
    }

    public TypeInfo getTypeInfo (String name)
    {
        if (StringUtil.nullOrEmptyOrBlankString(name)) {
            return null;
        }
        // Try to get the type info. If the type info is not found, then
        // find out if this is an array type.
        TypeInfo info = (TypeInfo)_classMap.get(name);
        info = (info != null ? info : JavaTypeProvider.getAliasedClass(this, name));
        return (info != null ? info : getArrayTypeInfo(name));
    }

    /**
     * Get the <code>TypeInfo</code> representing the an array class of the
     * given <code>name</code>.   If the class name is not an array class name
     * or the array element is not a primitive, return null.
     * @param name - the name of the array class
     * @return the <code>TypeInfo</code> for the array class.  If the
     * name does not corresponding to an array class, return null.
     */
    protected TypeInfo getArrayTypeInfo (String name)
    {
        int  count = 0;
        char current = name.charAt(count);
        while (count < name.length() && current == ArrayDimensionIndicator) {
            count++;
            current = name.charAt(count);
        }

        if (count > 0) {
             int typeIndex = ArrayElementEncoding.indexOf(String.valueOf(current));

            // There should be a single character remaining, representing the
            // encoding of the primitive type.
            if ((name.length() - count != 1) || (typeIndex == -1)) {
                return null;
            }

            // Get the type info for the element type.
            String elementTypeName = ((Class)SimpleTypes.get(typeIndex)).getName();
            TypeInfo elementInfo = getTypeInfo(elementTypeName);

            // Get the type info for the array class.
            Class arrayClass = ClassUtil.classForName(name);
            TypeInfo arrayInfo = new JavaTypeProvider.JavaArrayTypeInfo(
                                                     arrayClass, count);

            return new ContainerTypeInfo(arrayInfo, elementInfo);
        }

        return null;
    }

    private static TypeInfo createTypeInfo (int typeId, Class classObj)
    {
        return new PrimitiveTypeInfo(typeId, classObj);
    }

    // ---------------------------------------------------------------------
    // Methods for checking type information

    public static boolean isSupportedType (String name)
    {
        return (_classMap.get(name) != null);
    }


    public static boolean isNumericType (String name)
    {
        // Is this a numeric type?
        TypeInfo info = (TypeInfo)_classMap.get(name);
        if (info == null) {
            return false;
        }

        return isNumericType(info);
    }

    public static Class getClassForType (String name)
    {
        PrimitiveTypeInfo info = (PrimitiveTypeInfo)_classMap.get(name);
        if (info == null) {
            return null;
        }
        return info._proxiedClass;
    }

    public static boolean isNumericType (TypeInfo info)
    {
        PrimitiveTypeInfo type = getBoxedTypeInfo(info);
        if (type == null) {
            return false;
        }

        return (NumericDataTypes.indexOf(type._proxiedClass) != -1);
    }

    public static boolean isFloatingPointNumericType (TypeInfo info)
    {
        return (getBoxedTypeCode(info) >= NumericTypes.MIN_REAL_TYPE);
    }

    public static boolean isSimplePrimitiveType (TypeInfo info)
    {
        if (!(info instanceof PrimitiveTypeInfo)) {
            return false;
        }
        PrimitiveTypeInfo myInfo = (PrimitiveTypeInfo)info;
        return (SimpleTypes.indexOf(myInfo._proxiedClass) != -1 &&
                BoxedTypes.indexOf(myInfo._proxiedClass) == -1);
    }

    static TypeInfo getPrimitiveTypeInfo (int typeCode)
    {
        Class compatibleClass = (Class)SupportedTypes.get(typeCode);
        return (TypeInfo)_classMap.get(compatibleClass.getName());
    }

    public static boolean isUnboxedPrimitiveType (TypeInfo info)
    {
        if (!(info instanceof PrimitiveTypeInfo)) {
            return false;
        }
        PrimitiveTypeInfo myInfo = (PrimitiveTypeInfo)info;
        return (SimpleTypes.indexOf(myInfo._proxiedClass) != -1);
    }

    // ---------------------------------------------------------------------
    // Methods for boxing and unboxing

    static PrimitiveTypeInfo getUnboxedTypeInfo (TypeInfo info)
    {
        if (!(info instanceof PrimitiveTypeInfo)) {
            return null;
        }

        // Is this a box type?
        PrimitiveTypeInfo typeInfo = (PrimitiveTypeInfo)info;
        int index = BoxedTypes.indexOf(typeInfo._proxiedClass);
        if (index != -1) {
            // find its simple type equivalent
            return (PrimitiveTypeInfo)_classMap.get(
                ((Class)SimpleTypes.get(index)).getName());
        }

        // Is this a simple type?
        index = SimpleTypes.indexOf(typeInfo._proxiedClass);
        if (index != -1) {
            return (PrimitiveTypeInfo)info;
        }

        return null;
    }

    public static PrimitiveTypeInfo getBoxedTypeInfo (TypeInfo info)
    {
        if (isBoxedType(info)) {
            return (PrimitiveTypeInfo)info;
        }

        int typeCode = getBoxedTypeCode(info);
        if (typeCode == -1) {
            return null;
        }

        return (PrimitiveTypeInfo)getBoxedTypeInfo(typeCode);
    }

    static TypeInfo getBoxedTypeInfo (int typeCode)
    {
        Class compatibleClass = (Class)BoxedTypes.get(typeCode);
        return (TypeInfo)_classMap.get(compatibleClass.getName());
    }

    static int getBoxedTypeCode (TypeInfo info)
    {
        if (!(info instanceof PrimitiveTypeInfo)) {
            return -1;
        }

        PrimitiveTypeInfo primInfo = (PrimitiveTypeInfo)info;
        Class primClass = primInfo._proxiedClass;

        int typeCode = SupportedTypes.indexOf(primClass);
        if (typeCode != -1) {
            primClass = (Class)BoxedTypes.get(typeCode);
        }

        return BoxedTypes.indexOf(primClass);
    }

    public static boolean isBoxedType (TypeInfo info)
    {
        if (!(info instanceof PrimitiveTypeInfo)) {
            return false;
        }

        PrimitiveTypeInfo primInfo = (PrimitiveTypeInfo)info;
        Class primClass = primInfo._proxiedClass;

        return (BoxedTypes.indexOf(primClass) != -1);
    }

    //---------------------------------------------------------------------
    // Conversion Methods

    /**
     * Given two numeric types <code>first</code> and <code>second</code>, this
     * method returns a widened type that can contain both <code>first</code>
     * and <code>second</code>.  The input <code>TypeInfo</code> can be: <ol>
     * <li>A numeric primitive type<li>
     * <li>A numeric boxed type<li>
     * <li>BigDecimal or BigInteger</li>
     * </ol>
     * If the <code>TypeInfo</code> is not numeric, this method will return null.
     * <br/>
     * The coercion rule is as follows:<ol>
     * <li>If both <code>TypeInfo</code> are whole numbers, return the larger
     * of the two types.</li>
     * <li>If both <code>TypeInfo</code> are real numbers, return the larger
     * of the two types.</li>
     * <li>If one of the <code>TypeInfo</code> is real number, then return this real
     * number type unless (a) the other whole number type is BigInteger (return
     * BigDecimal), (b) the other whole number type is Integer or Long (return
     * the larger of Double and the real number type.</li>
     * </ol>
     * @param first - A numeric type
     * @param second - A numeric type
     * @return A <code>TypeInfo</code> that is wide enough to contain both
     * numeric types.  If one of the types is not numeric, return null.  If
     * one of the types is a boxed type, then the returned result is also
     * a boxed type.
     */
    public static TypeInfo getCoercedType (TypeInfo first,
                                           TypeInfo second)
    {
        if (first.equals(second)) {
            return first;
        }

        if (isNumericType(first) && isNumericType(second)) {
            int firstTC = getBoxedTypeCode(first);
            int secondTC = getBoxedTypeCode(second);
            int compatibleTC = getCoercedNumericType(firstTC, secondTC);

            // If both types are boxed, then the result is boxed.
            boolean useBoxedType = isBoxedType(first) && isBoxedType(second);
            return (useBoxedType ?
                    getBoxedTypeInfo(compatibleTC) :
                    getPrimitiveTypeInfo(compatibleTC));
        }

        return null;
    }

    private static int getCoercedNumericType( int t1, int t2)
    {
        if ( t1 == t2 )
            return t1;

        if ( t1 >= NumericTypes.MIN_REAL_TYPE )
          {
            if ( t2 >= NumericTypes.MIN_REAL_TYPE )
                return Math.max(t1,t2);
            if ( t2 < NumericTypes.INT )
                return t1;
            if ( t2 == NumericTypes.BIGINT )
                return NumericTypes.BIGDEC;
            return Math.max(NumericTypes.DOUBLE,t1);
          }
        else if ( t2 >= NumericTypes.MIN_REAL_TYPE )
          {
            if ( t1 < NumericTypes.INT )
                return t2;
            if ( t1 == NumericTypes.BIGINT )
                return NumericTypes.BIGDEC;
            return Math.max(NumericTypes.DOUBLE,t2);
          }
        else
            return Math.max(t1,t2);
    }

    /**
     * Check to see if <code>source</code> can be converted to type
     * <code>target</code>, following the Java widening primitive conversion.
     * If <code>target</code> or <code>source</code> is a primitive type, it will
     * first be converted to the equivalent primitive type (boxing) before
     * the widening conversion applies.  This method will also handle
     * type widening if <code>source</code> or <code>target</code> is
     * BigInteger and BigDecimal.   This method does not support subtypes of
     * boxed types, BigInteger and BigDecimal.<br/>
     * All primitive types can be converted to the Boolean.  During runtime,
     * non-zero number is covnerted to Boolean.True, and zero is converted
     * to Boolean.False.<br/>
     * If either <code>target</code> or <code>source</code> is not a primitive
     * type, this method returns false.
     * @param target - a primitive type or boxed type to be converted to.
     * @param source - a primitive type or boxed type
     * @return true if <code>source</code> can be converted to <code>target</code>.
     * Otherwise, return false.
     */
    public static boolean isWideningTypeOf (TypeInfo target, TypeInfo source)
    {
        // Find the primitive type info. Boxed type will be converted to its
        // equivalent primitive type.
        PrimitiveTypeInfo type1Prim = getBoxedTypeInfo(target);
        PrimitiveTypeInfo type2Prim = getBoxedTypeInfo(source);

        // Either one is not a primitive, return false.
        if (type1Prim == null || type2Prim == null) {
            return false;
        }

        // Find the relative order of the two primitive types.
        int index1 = NumericTypeWideningOrder.indexOf(type1Prim._proxiedClass);
        int index2 = NumericTypeWideningOrder.indexOf(type2Prim._proxiedClass);

        if (index1 == -1 || index2 == -1) {
            // Cannot determine numeric order of the types. Default to identity
            // conversion with auto-boxing.  Return true if the two equivalent
            // boxed type are the same.
            Class type1Class = type1Prim._proxiedClass;
            Class type2Class = type2Prim._proxiedClass;
            return (type1Class == type2Class);
        }

        boolean result = (index1 >= index2);

        if (result) {
            // handle the special case where byte and short cannot be converted
            // to char.
            if (NumericTypeWideningOrder.get(index1) == Character.class &&
                (NumericTypeWideningOrder.get(index2) == Byte.class ||
                 NumericTypeWideningOrder.get(index2) == Short.class)) {
                return false;
            }

            // handle the special case where BigInteger cannot be converted to
            // float or double.
            if (NumericTypeWideningOrder.get(index2) == BigInteger.class &&
                (NumericTypeWideningOrder.get(index1) == Float.class ||
                 NumericTypeWideningOrder.get(index1) == Double.class)) {
                return false;
            }
        }

        return result;
    }

    /**
     * Check to see if <code>source</code> can be converted to type
     * <code>target</code>, following the Java widening primitive conversion.
     * @param target - a primitive type or boxed type to be converted to.
     * @param source - a primitive type or boxed type
     * @return true if <code>source</code> can be converted to <code>target</code>.
     * Otherwise, return false.
     */
    public static boolean isWideningTypeOf (String target, String source)
    {
        TypeInfo info1 = (TypeInfo)_classMap.get(target);
        TypeInfo info2 = (TypeInfo)_classMap.get(source);

        return isWideningTypeOf(info1,info2);
    }

    //---------------------------------------------------------------------
    // PrimitiveTypeInfo

    /**
     * Subclass of <code>TypeInfo</code> for primtive type.
     */
    static class PrimitiveTypeInfo extends JavaTypeProvider.JavaTypeInfo
    {
        private int _typeId;

        PrimitiveTypeInfo (int typeId, Class proxiedClass)
        {
            super(proxiedClass);
            _typeId = typeId;
        }

        public FieldInfo getField (TypeRetriever retriever, String name)
        {
            if (PrimitiveTypeProvider.isBoxedType(this)) {
                return super.getField(retriever, name);
            }
            return null;
        }

        public MethodInfo getMethodForName (TypeRetriever retriever,
                                            String name,
                                            List<String> parameters,
                                            boolean staticOnly)
        {
            if (PrimitiveTypeProvider.isBoxedType(this)) {
                return super.getMethodForName(retriever, name, parameters, staticOnly);
            }
            return null;
        }

        public int getAccessibility ()
        {
            return TypeInfo.AccessibilitySafe;
        }

        public boolean isAssignableFrom (TypeInfo other)
        {
            if (other instanceof NullTypeInfo && isSimplePrimitiveType(this)) {
                return false;
            }

            return super.isAssignableFrom(other);
        }

        public boolean isCompatible (TypeInfo other)
        {
            // If this is a boolean type, it can be auto-convert to other types.
            if (_proxiedClass == Boolean.TYPE || _proxiedClass == Boolean.class) {
                return true;
            }

            JavaTypeProvider.JavaTypeInfo otherInfo = getTypeInfoInProvider(other);
            if (otherInfo == null) {
                return false;
            }

            // Find out the type code of the "other" type
            int otherTypeCode = SupportedTypes.indexOf(otherInfo._proxiedClass);
            if (otherTypeCode == -1) {
                otherTypeCode = BoxedTypes.indexOf(otherInfo._proxiedClass);
            }

            return (otherTypeCode != -1 || super.isCompatible(other));
        }

        public boolean isWideningTypeOf (TypeInfo other)
        {
            TypeInfo otherInfo = getTypeInfoInProvider(other);
            if (otherInfo == null) {
                return false;
            }

            return isAssignableFrom(otherInfo) ||
                   PrimitiveTypeProvider.isWideningTypeOf(this, otherInfo);
        }

        private JavaTypeProvider.JavaTypeInfo getTypeInfoInProvider (TypeInfo other)
        {
            if (other == null) {
                return null;
            }

            JavaTypeProvider.JavaTypeInfo otherInfo = null;
            otherInfo = (JavaTypeProvider.JavaTypeInfo)
                        (other instanceof JavaTypeProvider.JavaTypeInfo ?
                             other :
                             getTypeProvider().getTypeInfo(other.getImplementationName()));
            return otherInfo;
        }

        public TypeInfo resolveTypeForName (String name)
        {
            return null;
        }

        protected TypeProvider getTypeProvider ()
        {
            return PrimitiveTypeProvider.instance();
        }
    }

}

