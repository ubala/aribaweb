//--------------------------------------------------------------------------
//  Copyright (c) 1998-2004, Drew Davidson and Luke Blanshard
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions are
//  met:
//
//  Redistributions of source code must retain the above copyright notice,
//  this list of conditions and the following disclaimer.
//  Redistributions in binary form must reproduce the above copyright
//  notice, this list of conditions and the following disclaimer in the
//  documentation and/or other materials provided with the distribution.
//  Neither the name of the Drew Davidson nor the names of its contributors
//  may be used to endorse or promote products derived from this software
//  without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
//  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
//  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
//  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
//  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
//  AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
//  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
//  THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
//  DAMAGE.
//--------------------------------------------------------------------------
package ariba.util.expr;

import ariba.util.fieldtype.TypeProvider;
import ariba.util.fieldvalue.OrderedList;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import ariba.util.fieldtype.NumericTypes;
import ariba.util.fieldtype.PrimitiveTypeProvider;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.core.ArithmeticOperations;
import ariba.util.core.StringUtil;

/**
 * This is an abstract class with static methods that define the operations of AribaExpr.
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
public abstract class ExprOps implements NumericTypes
{
    private static ConcurrentHashMap<String, String> _customNumericTypes =
        new ConcurrentHashMap<String, String>();

    /**
        Registers the specified type as a non-built-in numeric type. <p>
        Notes:<ul>
        <li> This type must have an associated ArithmeticOperations class extension
        <li> It is assumed that all built-in types are convertible to
             this custom type. Thus any binary expression that includes a
             custom numeric type, the custom numeric types ArithmeticOperations
             interface will be called.
        <li> It is assumed that the custom numeric type is *not* convertible
             to the built-in types.
        </ul>

        @aribaapi
    */
    public static void registerNumericType (String typeName)
    {
        _customNumericTypes.put(typeName, typeName);
    }

    /**
     * Compares two objects for equality, even if it has to convert
     * one of them to the other type.  If both objects are numeric
     * they are converted to the widest type and compared.  If
     * one is non-numeric and one is numeric the non-numeric is
     * converted to double and compared to the double numeric
     * value.  If both are non-numeric and Comparable and the
     * types are compatible (i.e. v1 is of the same or superclass
     * of v2's type) they are compared with Comparable.compareTo().
     * If both values are non-numeric and not Comparable or of
     * incompatible classes this will throw and IllegalArgumentException.
     *
     * @param v1    First value to compare
     * @param v2    second value to compare
     *
     * @return integer describing the comparison between the two objects.
     *          A negative number indicates that v1 < v2.  Positive indicates
     *          that v1 > v2.  Zero indicates v1 == v2.
     *
     * @throws IllegalArgumentException if the objects are both non-numeric
     *              yet of incompatible types or do not implement Comparable.
     */
    public static int compareWithConversion (Object v1, Object v2)
    {
        return compareWithConversion(v1, v2, false);
    }

    /**
     * Compares two objects for equality, even if it has to convert
     * one of them to the other type.  If both objects are numeric
     * they are converted to the widest type and compared.  If
     * one is non-numeric and one is numeric the non-numeric is
     * converted to double and compared to the double numeric
     * value.  If both are non-numeric and Comparable and the
     * types are compatible (i.e. v1 is of the same or superclass
     * of v2's type) they are compared with Comparable.compareTo().
     * If both values are non-numeric and not Comparable or of
     * incompatible classes this will throw and IllegalArgumentException.
     *
     * @param v1    First value to compare
     * @param v2    second value to compare
     *
     * @return integer describing the comparison between the two objects.
     *          A negative number indicates that v1 < v2.  Positive indicates
     *          that v1 > v2.  Zero indicates v1 == v2.
     *
     * @throws IllegalArgumentException if the objects are both non-numeric
     *              yet of incompatible types or do not implement Comparable.
     */
    public static int compareWithConversion (Object v1, Object v2, boolean equals)
    {
        int     result;

        if (v1 == v2) {
            result = 0;
        }
        else {
            int     t1 = getNumericType(v1),
                    t2 = getNumericType(v2),
                    type = getNumericType(t1, t2, true);

            switch (type) {
                case BIGINT:
                    result = bigIntValue(v1).compareTo(bigIntValue(v2));
                    break;

                case BIGDEC:
                    result = bigDecValue(v1).compareTo(bigDecValue(v2));
                    break;

                case CUSTOMNUMERICTYPE:
                    ArithmeticOperations operations = (t1 > t2) ?
                            getArithmeticOperations(v1)
                            : getArithmeticOperations(v2);
                    result = operations.compare(v1, v2);
                    break;

                case NONNUMERIC:
                    if ((t1 == NONNUMERIC || v1 == null) &&
                        (t2 == NONNUMERIC || v2 == null)) {
                        if ((v1 == null) || (v2 == null)) {
                            result = (v1 == v2) ? 0 : (v1 == null ? -1 : 1);
                            break;
                        }
                        else {
                            if (v1.getClass().isAssignableFrom(v2.getClass()) ||
                                    v2.getClass().isAssignableFrom(v1.getClass())) {
                                if (v1 instanceof Comparable) {
                                    result = ((Comparable)v1).compareTo(v2);
                                    break;
                                }
                                else {
                                    if (equals) {
                                        result = v1.equals(v2) ? 0 : 1;
                                        break;
                                    }
                                }
                            }
                            if (equals) {
                                // Equals comparison between non-numerics that are not of a common
                                // superclass return not equal
                                result = 1;
                                break;
                            }
                            else {
                                throw new IllegalArgumentException(
                                        "invalid comparison: " + v1.getClass().getName() +
                                        " and " + v2.getClass().getName());
                            }
                        }
                    }
                    // else fall through
                case FLOAT:
                case DOUBLE:
                    try {
                        double      dv1 = doubleValue(v1),
                                    dv2 = doubleValue(v2);

                        return (dv1 == dv2) ? 0 : ((dv1 < dv2) ? -1 : 1);
                    }
                    catch (NumberFormatException e) {
                        // handle the "fall-through" case where one of the
                        // operands is non-numeric.
                        if (equals) {
                            return v1.equals(v2) ? 0 : 1;
                        }
                        throw e;
                    }

                default:
                    long        lv1 = longValue(v1),
                                lv2 = longValue(v2);

                    return (lv1 == lv2) ? 0 : ((lv1 < lv2) ? -1 : 1);
              }
        }
        return result;
    }

    /**
     * Returns true if object1 is equal to object2 in either the
     * sense that they are the same object or, if both are non-null
     * if they are equal in the <CODE>equals()</CODE> sense.
     *
     * @param object1 First object to compare
     * @param object2 Second object to compare
     *
     * @return true if v1 == v2
     */
    public static boolean isEqual (Object object1, Object object2)
    {
        boolean     result = false;

          if (object1 == object2) {
              result = true;
          }
          else {
              if ((object1 != null) && (object2 != null)) {
                  if (object1.getClass().isArray() && object2.getClass().isArray() &&
                          (object2.getClass() == object1.getClass())) {
                      result = (Array.getLength(object1) == Array.getLength(object2));
                    if (result) {
                        for (int i = 0, icount = Array.getLength(object1); result &&
                                (i < icount); i++) {
                            result = isEqual(Array.get(object1, i),
                                    Array.get(object2, i));
                        }
                    }
                  }
                  else {
                      if ((object1 != null) && (object2 != null)) {
                          // Check for converted equivalence first, then equals() equivalence
                        result = (compareWithConversion(object1, object2, true) == 0) ||
                                object1.equals(object2);
                    }
                }
            }
        }
        return result;
    }

      /**
       * Evaluates the given object as a boolean: if it is a Boolean object, it's easy; if
       * it's a Number or a Character, returns true for non-zero objects; and otherwise
       * returns true for non-null objects.
       *
       * @param value an object to interpret as a boolean
       * @return the boolean value implied by the given object
       */
    public static boolean booleanValue (Object value)
    {
        if (value == null)
            return false;
        Class c = value.getClass();
        if (c == Boolean.class)
            return (Boolean)value;
//        if ( c == String.class )
//            return ((String)value).length() > 0;
        if (c == Character.class)
            return (Character)value != 0;
        if (value instanceof Number)
            return ((Number)value).doubleValue() != 0;

        // non-null
        return true;
    }

      /**
       * Evaluates the given object as a long integer.
       *
       * @param value an object to interpret as a long integer
       * @return the long integer value implied by the given object
       * @throws NumberFormatException if the given object can't be understood as a long integer
       */
    public static long longValue (Object value) throws NumberFormatException
    {
        if (value == null)
            return 0L;
        Class c = value.getClass();
        if ( c.getSuperclass() == Number.class )
            return ((Number)value).longValue();
        if ( c == Boolean.class )
            return (Boolean)value? 1 : 0;
        if ( c == Character.class )
            return (Character)value;
        return Long.parseLong(stringValue(value, true));
    }

      /**
       * Evaluates the given object as a double-precision floating-point number.
       *
       * @param value an object to interpret as a double
       * @return the double value implied by the given object
       * @throws NumberFormatException if the given object can't be understood as a double
       */
    public static double doubleValue (Object value) throws NumberFormatException
    {
        if (value == null)
            return 0.0;
        Class c = value.getClass();
        if (c.getSuperclass() == Number.class)
            return ((Number)value).doubleValue();
        if (c == Boolean.class)
            return (Boolean)value? 1 : 0;
        if (c == Character.class)
            return (Character)value;
        String  s = stringValue(value, true);

        return (s.length() == 0) ? 0.0 : Double.parseDouble(s);
        /*
            For 1.1 parseDouble() is not available
         */
        //return Double.valueOf( value.toString() ).doubleValue();
    }

      /**
       * Evaluates the given object as a BigInteger.
       *
       * @param value an object to interpret as a BigInteger
       * @return the BigInteger value implied by the given object
       * @throws NumberFormatException if the given object can't be understood as a BigInteger
       */
    public static BigInteger bigIntValue (Object value) throws NumberFormatException
    {
        if (value == null)
            return BigInteger.valueOf(0L);
        Class c = value.getClass();
        if (c == BigInteger.class)
            return (BigInteger)value;
        if (c == BigDecimal.class)
            return ((BigDecimal)value).toBigInteger();
        if (c.getSuperclass() == Number.class)
            return BigInteger.valueOf(((Number)value).longValue());
        if (c == Boolean.class)
            return BigInteger.valueOf((Boolean)value? 1 : 0 );
        if (c == Character.class)
            return BigInteger.valueOf((Character)value);
        return new BigInteger(stringValue(value, true));
    }

      /**
       * Evaluates the given object as a BigDecimal.
       *
       * @param value an object to interpret as a BigDecimal
       * @return the BigDecimal value implied by the given object
       * @throws NumberFormatException if the given object can't be understood as a BigDecimal
       */
    public static BigDecimal bigDecValue (Object value) throws NumberFormatException
    {
        if (value == null)
            return BigDecimal.valueOf(0L);
        Class c = value.getClass();
        if (c == BigDecimal.class)
            return (BigDecimal)value;
        if (c == BigInteger.class)
            return new BigDecimal((BigInteger)value);
        if (c.getSuperclass() == Number.class )
            return new BigDecimal(((Number)value).doubleValue());
        if (c == Boolean.class)
            return BigDecimal.valueOf((Boolean)value? 1 : 0);
        if (c == Character.class)
            return BigDecimal.valueOf(((Character)value).charValue());
        return new BigDecimal(stringValue(value, true));
    }

      /**
       * Evaluates the given object as a String and trims it if the trim flag is true.
       *
       * @param value an object to interpret as a String
       * @return the String value implied by the given object as returned by the toString() method,
                or "null" if the object is null.
       */
    public static String stringValue (Object value, boolean trim)
    {
        String      result;

        if (value == null) {
            result = ExprRuntime.NULL_VALUE_STRING;
        }
        else {
            result = value.toString();
            if (trim) {
               result = result.trim();
            }
        }
        return result;
    }

      /**
       * Evaluates the given object as a String.
       *
       * @param value an object to interpret as a String
       * @return the String value implied by the given object as returned by the toString() method,
                or "null" if the object is null.
       */
    public static String stringValue (Object value)
    {
        return stringValue(value, false);
    }

      /**
       * Returns a constant from the NumericTypes interface that represents the numeric
       * type of the given object.
       *
       * @param value an object that needs to be interpreted as a number
       * @return the appropriate constant from the NumericTypes interface
       */

      public static int getNumericType (Object value)
      {
          return getNumericTypeForName(value == null ? null :
                                                       value.getClass().getName());
      }

      public static int getNumericTypeForName (String typeName)
      {
          TypeProvider tp = PrimitiveTypeProvider.instance();
          TypeInfo ti = PrimitiveTypeProvider.getBoxedTypeInfo(tp.getTypeInfo(typeName));
          if (ti != null) {
              typeName = ti.getName();
          }

          if (typeName != null) {
              if (typeName.equals(Integer.class.getName())) {
                  return INT;
              }
              if (typeName.equals(Double.class.getName())) {
                  return DOUBLE;
              }
              if (typeName.equals(Boolean.class.getName())) {
                  return BOOL;
              }
              if (typeName.equals(Byte.class.getName())) {
                  return BYTE;
              }
              if (typeName.equals(Character.class.getName())) {
                  return CHAR;
              }
              if (typeName.equals(Short.class.getName())) {
                  return SHORT;
              }
              if (typeName.equals(Long.class.getName())) {
                  return LONG;
              }
              if (typeName.equals(Float.class.getName())) {
                  return FLOAT;
              }
              if (typeName.equals(BigInteger.class.getName())) {
                  return BIGINT;
              }
              if (typeName.equals(BigDecimal.class.getName())) {
                  return BIGDEC;
              }
              if (_customNumericTypes.containsKey(typeName)) {
                  return CUSTOMNUMERICTYPE;
              }
              return NONNUMERIC;
          }
        return NULL;
    }

    /**
       * Returns the value converted numerically to the given class type
       *
       * This method also detects when arrays are being converted and
       * converts the components of one array to the type of the other.
       *
       * @param value an object to be converted to the given type
       * @param toType class type to be converted to
       * @return converted value of the type given, or value if the value
       *                cannot be converted to the given type.
     */
    public static Object convertValue (Object value, Class toType)
    {
        Object  result = null;

        if (value != null) {
            /* If array -> array then convert components of array individually */
            if ( value.getClass().isArray() && toType.isArray() ) {
                Class       componentType = toType.getComponentType();

                result = Array.newInstance(componentType, Array.getLength(value));
                for (int i = 0, icount = Array.getLength(value); i < icount; i++) {
                    Array.set(result, i,
                            convertValue(Array.get(value, i), componentType));
                }
            }
            else {
                if ((toType == Integer.class) || (toType == Integer.TYPE )) {
                    result = new Integer((int)longValue(value));
                }
                if ((toType == Double.class) || (toType == Double.TYPE)) {
                    result = new Double(doubleValue(value));
                }
                if ((toType == Boolean.class) || (toType == Boolean.TYPE)) {
                    result = booleanValue(value) ? Boolean.TRUE : Boolean.FALSE;
                }
                if ((toType == Byte.class) || (toType == Byte.TYPE)) {
                    result = new Byte((byte)longValue(value));
                }
                if ((toType == Character.class) || (toType == Character.TYPE)) {
                    result = new Character((char)longValue(value));
                }
                if ((toType == Short.class) || (toType == Short.TYPE)) {
                    result = new Short((short)longValue(value));
                }
                if ((toType == Long.class) || (toType == Long.TYPE)) {
                    result = new Long(longValue(value));
                }
                if ((toType == Float.class) || (toType == Float.TYPE)) {
                    result = new Float(doubleValue(value));
                }
                if (toType == BigInteger.class) {
                    result = bigIntValue(value);
                }
                if (toType == BigDecimal.class) {
                    result = bigDecValue(value);
                }
                if (toType == String.class) {
                    result = stringValue(value);
                }
            }
        }
        else {
            if (toType.isPrimitive()) {
                result = ExprRuntime.getPrimitiveDefaultValue(toType);
            }
        }
        return result;
    }

      /**
       * Returns the constant from the NumericTypes interface that best expresses the type
       * of a numeric operation on the two given objects.
       *
       * @param v1 one argument to a numeric operator
       * @param v2 the other argument
       * @return the appropriate constant from the NumericTypes interface
       */
    public static int getNumericType (Object v1, Object v2)
    {
        return getNumericType( v1, v2, false );
    }

      /**
       * Returns the constant from the NumericTypes interface that best expresses the type
       * of an operation, which can be either numeric or not, on the two given types.
       *
       * @param t1 type of one argument to an operator
       * @param t2 type of the other argument
       * @param canBeNonNumeric whether the operator can be interpreted as non-numeric
       * @return the appropriate constant from the NumericTypes interface
       */
    public static int getNumericType (int t1, int t2, boolean canBeNonNumeric)
    {
        if ( t1 == t2 && t1 != NULL)
            return t1;

        if (canBeNonNumeric && (t1 == NONNUMERIC || t2 == NONNUMERIC ||
                t1 == CHAR || t2 == CHAR))
            return NONNUMERIC;

        if (t1 == NONNUMERIC) {
            // Try to interpret strings as doubles...
            t1 = DOUBLE;
        }
        if (t2 == NONNUMERIC) {
            // Try to interpret strings as doubles...
            t2 = DOUBLE;
        }

        // handling null.  If both operands are null, return NONNUMERIC.
        // If one of the operand is null, then return the type of the other
        // operand.
        if (t1 == NULL) {
            return (t2 != NULL ? t2 : NONNUMERIC);
        }
        if (t2 == NULL) {
            return t1;
        }

        if ( t1 >= MIN_REAL_TYPE )
        {
            if ( t2 >= MIN_REAL_TYPE )
                return Math.max(t1,t2);
            if ( t2 < INT )
                return t1;
            if ( t2 == BIGINT )
                return BIGDEC;
            return Math.max(DOUBLE,t1);
        }
        else if ( t2 >= MIN_REAL_TYPE )
        {
            if ( t1 < INT )
                return t2;
            if ( t1 == BIGINT )
                return BIGDEC;
            return Math.max(DOUBLE,t2);
        }
        else {
            return Math.max(t1,t2);
        }
    }

      /**
       * Returns the constant from the NumericTypes interface that best expresses the type
       * of an operation, which can be either numeric or not, on the two given objects.
       *
       * @param v1 one argument to an operator
       * @param v2 the other argument
       * @param canBeNonNumeric whether the operator can be interpreted as non-numeric
       * @return the appropriate constant from the NumericTypes interface
       */
    public static int getNumericType (Object v1, Object v2, boolean canBeNonNumeric)
    {
        return getNumericType(getNumericType(v1), getNumericType(v2), canBeNonNumeric);
    }

    /**
        Returns the constant from the NumericTypes interface for the given
        <code>value></code> and <code>fieldType</code>.
    */
    public static int getNumericType (Object v, String type, boolean canBeNonNumeric)
    {
        // dlee 10/14/2010: I don't quite understand the semantic of canBeNonNumeric.
        // Currently, getNumericTypeForName would return NONNUMERIC regardless of the
        // value of canBeNonNumeric.  It only return NULL when the type is null.
        // However, it still won't be set to NONNUMERIC later even if canBeNonNumeric
        // is true since type is null.

        int t = getNumericType(v);
        if (t == NULL) {
            t = getNumericTypeForName(type);
        }

        if (canBeNonNumeric) {
            // if the numeric type is NULL, we need to find out if the
            // real type of v1 is non-numeric type
            if (t == NULL &&
                    !StringUtil.nullOrEmptyOrBlankString(type) &&
                    !PrimitiveTypeProvider.isNumericType(type)) {
                t = NONNUMERIC;
            }
        }
        return t;
    }


    public static int getNumericType (
            Object v1,
            Object v2,
            String type1,
            String type2,
            boolean canBeNonNumeric)
    {
        int t1 = getNumericType(v1, type1, canBeNonNumeric);
        int t2 = getNumericType(v2, type2, canBeNonNumeric);
        return getNumericType(t1, t2, canBeNonNumeric);
    }

      /**
       * Returns a new Number object of an appropriate type to hold the given integer
       * value.  The type of the returned object is consistent with the given type
       * argument, which is a constant from the NumericTypes interface.
       *
       * @param type    the nominal numeric type of the result, a constant from the NumericTypes interface
       * @param value   the integer value to convert to a Number object
       * @return        a Number object with the given value, of type implied by the type argument
       */
    public static Number newInteger (int type, long value)
    {
        switch ( type )
        {
            case NULL:
            case BOOL:
            case CHAR:
            case INT:
                return new Integer( (int)value );

            case FLOAT:
                if ( (long)(float)value == value ) {
                    return new Float( (float)value );
                }
                // else fall through:
            case DOUBLE:
                if ( (long)(double)value == value ) {
                    return new Double( (double)value );
                }
                // else fall through:
            case LONG:
                return new Long( value );

            case BYTE:
                return new Byte( (byte)value );

            case SHORT:
                return new Short( (short)value );

            default:
                return BigInteger.valueOf( value );
        }
    }

      /**
       * Returns a new Number object of an appropriate type to hold the given real value.
       * The type of the returned object is always either Float or Double, and is only
       * Float if the given type tag (a constant from the NumericTypes interface) is
       * FLOAT.
       *
       * @param type    the nominal numeric type of the result, a constant from the NumericTypes interface
       * @param value   the real value to convert to a Number object
       * @return        a Number object with the given value, of type implied by the type argument
       */
    public static Number newReal (int type, double value)
    {
        if ( type == FLOAT ) {
            return new Float( (float)value );
        }
        return new Double( value );
    }

    public static Object binaryOr (Object v1, Object v2)
    {
        int type = getNumericType(v1,v2);
        if ( type == BIGINT || type == BIGDEC ) {
            return bigIntValue(v1).or(bigIntValue(v2));
        }
        return newInteger( type, longValue(v1) | longValue(v2) );
    }

    public static Object binaryXor (Object v1, Object v2)
    {
        int type = getNumericType(v1,v2);
        if ( type == BIGINT || type == BIGDEC ) {
            return bigIntValue(v1).xor(bigIntValue(v2));
        }
        return newInteger( type, longValue(v1) ^ longValue(v2) );
    }

    public static Object binaryAnd (Object v1, Object v2)
    {
        int type = getNumericType(v1,v2);
        if ( type == BIGINT || type == BIGDEC ) {
            return bigIntValue(v1).and(bigIntValue(v2));
        }
        return newInteger( type, longValue(v1) & longValue(v2) );
    }

    public static boolean equal (Object v1, Object v2)
    {
        if (v1 == null) {
            return (v2 == null);
        }
        if ( v1 == v2 || isEqual(v1, v2) ) {
            return true;
        }
        if ( v1 instanceof Number && v2 instanceof Number ) {
            return ((Number)v1).doubleValue() == ((Number)v2).doubleValue();
        }
        return false;
    }

    public static boolean less (Object v1, Object v2)
    {
        return compareWithConversion(v1, v2) < 0;
    }

    public static boolean greater (Object v1, Object v2)
    {
        return compareWithConversion(v1, v2) > 0;
    }

    public static boolean in (Object v1, Object v2)
    {
        if ( v2 == null ) {
            // A null collection is always treated as empty
            return false;
        }

        OrderedList listInterface = OrderedList.get(v2);

        for (Iterator e = listInterface.elements(v2); e.hasNext();) {
            Object      o = e.next();

            if ( equal(v1, o) ) {
                return true;
            }
        }
        return false;
    }

    public static Object shiftLeft (Object v1, Object v2)
    {
        int type = getNumericType(v1);
        if (type == BIGINT || type == BIGDEC) {
            return bigIntValue(v1).shiftLeft((int)longValue(v2));
        }
        return newInteger(type, longValue(v1) << (int)longValue(v2));
    }

    public static Object shiftRight (Object v1, Object v2)
    {
        int type = getNumericType(v1);
        if ( type == BIGINT || type == BIGDEC ) {
            return bigIntValue(v1).shiftRight((int)longValue(v2));
        }
        return newInteger(type, longValue(v1) >> (int)longValue(v2));
    }

    public static Object unsignedShiftRight (Object v1, Object v2)
    {
        int type = getNumericType(v1);
        if ( type == BIGINT || type == BIGDEC ) {
            return bigIntValue(v1).shiftRight((int)longValue(v2));
        }
        if ( type <= INT ) {
            return newInteger(INT, ((int)longValue(v1)) >>> (int)longValue(v2));
        }
        return newInteger(type, longValue(v1) >>> (int)longValue(v2));
    }

    public static Object add (
            Object v1,
            Object v2,
            String v1Type,
            String v2Type)
    {

        int type = getNumericType(v1, v2, v1Type, v2Type, true);
        switch ( type )
          {
            case BIGINT:    return bigIntValue(v1).add(bigIntValue(v2));
            case BIGDEC:    return bigDecValue(v1).add(bigDecValue(v2));
            case FLOAT:
            case DOUBLE:    return newReal( type, doubleValue(v1) + doubleValue(v2) );
            case NONNUMERIC:
            {
                // if this is modified, please modify the getArithmeticOperations
                // in TypeChecker as well
                ArithmeticOperations ops = getArithmeticOperations(v1Type, v2Type);
                if (ops != null) {
                    return ops.add(v1, v2);
                }

                // Now the operands are not numeric and they do not support
                // ArithmeticOperations.  The addition becomes concatenation.
                return concatenate(v1, v2);
            }
            case CUSTOMNUMERICTYPE:
                ArithmeticOperations operations =
                        getArithmeticOperations(v1Type, v2Type);
                return operations.add(v1, v2);
            default:
                return newInteger( type, longValue(v1) + longValue(v2) );
          }
    }

    public static Object concatenate (Object v1, Object v2)
    {
        return stringValue(v1) + stringValue(v2);
    }

    public static Object subtract (
            Object v1,
            Object v2,
            String v1Type,
            String v2Type)
    {
        int type = getNumericType(v1, v2, v1Type, v2Type, true);
        switch ( type )
        {
            case BIGINT:    return bigIntValue(v1).subtract(bigIntValue(v2));
            case BIGDEC:    return bigDecValue(v1).subtract(bigDecValue(v2));
            case FLOAT:
            case DOUBLE:    return newReal( type, doubleValue(v1) - doubleValue(v2) );
            case NONNUMERIC :
                // if this is modified, please modify the getArithmeticOperations
                // in TypeChecker as well
                ArithmeticOperations ops = getArithmeticOperations(v1Type, v2Type);
                if (ops != null) {
                    return ops.substract(v1, v2);
                }
                return newReal( type, doubleValue(v1) - doubleValue(v2) );
            case CUSTOMNUMERICTYPE:
                ArithmeticOperations operations =
                        getArithmeticOperations(v1Type, v2Type);
                return operations.substract(v1, v2);
            default:
                return newInteger( type, longValue(v1) - longValue(v2) );
        }
    }

    public static Object multiply (
            Object v1,
            Object v2,
            String v1Type,
            String v2Type)
    {
        boolean canBeNonNumeric = true;
        int type = getNumericType(v1, v2, v1Type, v2Type, canBeNonNumeric);
        ArithmeticOperations ops;
        switch ( type )
        {
            case BIGINT:    return bigIntValue(v1).multiply(bigIntValue(v2));
            case BIGDEC:    return bigDecValue(v1).multiply(bigDecValue(v2));
            case FLOAT:
            case DOUBLE:    return newReal(type, doubleValue(v1) * doubleValue(v2));
            case NONNUMERIC:
                // if this is modified, please modify the getArithmeticOperations
                // in TypeChecker as well
                ops = getArithmeticOperations(v1Type);
                if (ops != null) {
                    return ops.multiply(v1, bigDecValue(v2));
                }
                else {
                    ops = getArithmeticOperations(v2Type);
                    if (ops != null) {
                        return ops.multiply(v2, bigDecValue(v1));
                    }
                }
                return newReal(type, doubleValue(v1) * doubleValue(v2));
            case CUSTOMNUMERICTYPE:
                // one of the types should be custom numeric (that is
                // how we get here in the first place).  we need to
                // find out which one
                int t1 = getNumericType(v1, v1Type, canBeNonNumeric);
                ops = getArithmeticOperations(v1Type, v2Type);
                if (ops != null) {
                    // dlee 10/14/2010: we need to revisit this when we support
                    // more than one custom numeric types.
                    if (t1 == CUSTOMNUMERICTYPE) {
                        return ops.multiply(v1, bigDecValue(v2));
                    }
                    else {
                        return ops.multiply(v2, bigDecValue(v1));
                    }
                }
            default :
                return newInteger(type, longValue(v1) * longValue(v2));
        }
    }

    public static Object divide (
            Object v1,
            Object v2,
            String v1Type)
    {
        int type = getNumericType(v1, v2, v1Type, null, true);
        ArithmeticOperations ops;
        switch ( type )
        {
            case BIGINT:
                return bigIntValue(v1).divide(bigIntValue(v2));
            case BIGDEC:
                return bigDecValue(v1).divide(bigDecValue(v2),
                        BigDecimal.ROUND_HALF_EVEN);
            case FLOAT:
            case DOUBLE:
                return newReal(type, doubleValue(v1) / doubleValue(v2));
            case NONNUMERIC :
                // if this is modified, please modify the getArithmeticOperations
                // in TypeChecker as well
                ops = getArithmeticOperations(v1Type);
                if (ops != null) {
                    return ops.divide(v1, v2);
                }
                return newReal(type, doubleValue(v1) / doubleValue(v2));
            case CUSTOMNUMERICTYPE:
                // support money/BigDecimal and
                // money/money as in getting percentage
                ops = getArithmeticOperations(v1Type);
                return ops.divide(v1, v2);
            default :
                return newInteger(type, longValue(v1) / longValue(v2));
        }
    }

    public static boolean isNull (Object v1)
    {
        return v1 == null;
    }

    /**
        Returns the {@link ArithmeticOperations} for the given
        <code>obj1</code> and <code>obj2</code>.
    */
    private static ArithmeticOperations getArithmeticOperations (
        Object obj1, Object obj2)
    {
        ArithmeticOperations op1 = getArithmeticOperations(obj1);
        ArithmeticOperations op2 = getArithmeticOperations(obj2);
        return getArithmeticOperationsInternal(op1, op2);
    }

    /**
        Returns the {@link ArithmeticOperations} for the given
        <code>obj</code>.  If there is no {@link ArithmeticOperations}
        for the given <code>obj</code>, <code>null</code> is returned.
    */
    private static ArithmeticOperations getArithmeticOperations (Object obj)
    {
        ArithmeticOperations op = null;
        if (obj != null) {
            op = ArithmeticOperations.get(obj);
        }
        return op;
    }

    /**
        Returns the {@link ArithmeticOperations} for the 2 operands
        of the given <code>type1</code> and <code>type2</code>.

        @aribaapi private
    */
    public static ArithmeticOperations getArithmeticOperations (
            String type1, String type2)
    {
        ArithmeticOperations op1 = getArithmeticOperations(type1);
        ArithmeticOperations op2 = getArithmeticOperations(type2);
        return getArithmeticOperationsInternal(op1, op2);
    }

    /**
        Returns the {@link ArithmeticOperations} for the operand
        of the given <code>type</code>.  If there is no {@link ArithmeticOperations}
        for the given <code>type</code>, <code>null</code> is returned.
    */
    public static ArithmeticOperations getArithmeticOperations (String type)
    {
        ArithmeticOperations op = null;
        if (!StringUtil.nullOrEmptyOrBlankString(type)) {
            op = ArithmeticOperations.getByName(type);
        }
        return op;
    }

    private static ArithmeticOperations getArithmeticOperationsInternal (
            ArithmeticOperations op1,
            ArithmeticOperations op2)
    {
        if (op1 == null) {
            return op2;
        }

        if (op2 == null) {
            return op1;
        }

        if (op1 == op2) {
            return op1;
        }

        return null;
    }

    public static Object remainder (Object v1, Object v2)
    {
        int type = getNumericType(v1,v2);
        switch ( type )
        {
            case BIGDEC:
            case BIGINT:    return bigIntValue(v1).remainder(bigIntValue(v2));
            default:        return newInteger( type, longValue(v1) % longValue(v2) );
        }
    }

    public static Object negate (Object value)
    {
        int type = getNumericType(value);
        switch ( type )
        {
            case BIGINT:    return bigIntValue(value).negate();
            case BIGDEC:    return bigDecValue(value).negate();
            case FLOAT:
            case DOUBLE:    return newReal(type, -doubleValue(value));
            default:        return newInteger(type, -longValue(value));
        }
    }

    public static Object bitNegate (Object value)
    {
        int type = getNumericType(value);
        switch ( type )
        {
            case BIGDEC:
            case BIGINT:    return bigIntValue(value).not();
            default:        return newInteger(type, ~longValue(value));
        }
    }
}
