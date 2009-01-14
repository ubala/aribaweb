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

    $Id: //ariba/platform/util/core/ariba/util/core/ArithmeticUtil.java#4 $
*/
package ariba.util.core;

/**
    @aribaapi ariba
*/
public abstract class ArithmeticUtil
{
    //--------------------------------------------------------------------------
    // public static methods

    /**
        Returns the instance of {@link Arithmetic} corresponding to evaluating
        a simple arithmetic expression containing <code>first</code> and
        <code>second</code>. <p/>
        @param first the first <code>Number</code>; may not be <code>null</code>
        @param second the second <code>Number</code>; may not be <code>null</code>
        @return the Arithmetic instance; is not <code>null</code>
        @aribaapi ariba
    */
    public static Arithmetic getArithmetic (Number first, Number second)
    {
        if (first == null || second == null) {
            Assert.that(false, "Cannot find Arithmetic for the two numbers '%s' and " +
                               "'%s; where one of the numbers is not specified",
                        first, second);
        }
        int firstType = Arithmetic.getNumberType(first),
                secondType = Arithmetic.getNumberType(second);
        if (firstType == secondType) {
            /* if same type, get the Arithmetic from this type */
            return Arithmetic.getArithmeticFromNumberType(firstType);
        }
        boolean firstIsBigger = firstType > secondType;
        int bigger = firstIsBigger ? firstType : secondType;
        int smaller = firstIsBigger ? secondType : firstType;
        if (bigger <= Arithmetic.DoubleType) {
            return Arithmetic.getArithmeticFromNumberType(bigger);
        }
        if (bigger == Arithmetic.BigIntegerType) {
            if (smaller == Arithmetic.FloatType || smaller == Arithmetic.DoubleType) {
                return Arithmetic.BigDecimalArithmetic;
            }
            return Arithmetic.BigIntegerArithmetic;
        }
        return Arithmetic.BigDecimalArithmetic;
    }

    /**
        Returns <code>true</code> if <code>number</code> is one of the
        integer types and <code>false</code> otherwise. <p/>
        @aribaapi ariba
    */
    public static boolean isInteger (Number number)
    {
        int numberType = Arithmetic.getNumberType(number);
        return numberType <= Arithmetic.LongType ||
                numberType == Arithmetic.BigIntegerType;
    }

    /**
        Evaluates <code>first</code> to <code>second</code> with respect
        to the specified <code>operation</code> and returns the
        result. <p/>

        The value <code>operation</code> must be one of:
        <code>OpAdd, OpSubtract, OpMultiply, OpDivide</code>. <p/>

        @param first the first number; may not be <code>null</code>
        @param operation the arithmetic operation
        @param second the second number; may not be <code>null</code>
        @aribaapi ariba
    */
    public static Number evaluate (
            Number first,
            Arithmetic.Operation operation,
            Number second
    )
    {
        Assert.that(operation != null, "Null is not a valid arithmetic operation");
        Arithmetic arithmetic = getArithmetic(first, second);
        return arithmetic != null ? arithmetic.evaluate(first, operation, second) : null;
    }

    /**
        Adds <code>first</code> to <code>second</code> and returns
        the result.
        @param first the first number; may not be <code>null</code>
        @param second the second number; may not be <code>null</code>
        @aribaapi ariba
    */
    public static Number add (Number first, Number second)
    {
        Arithmetic arithmetic = getArithmetic(first, second);
        return arithmetic != null ? arithmetic.add(first, second) : null;
    }

    /**
        Subtracts <code>second</code> from <code>first</code> and returns
        the result.
        @param first the first number; may not be <code>null</code>
        @param second the second number; may not be <code>null</code>
        @aribaapi ariba
    */
    public static Number subtract (Number first, Number second)
    {
        Arithmetic arithmetic = getArithmetic(first, second);
        return arithmetic != null ? arithmetic.subtract(first, second) : null;
    }

    /**
        Multiplies <code>first</code> by <code>second</code> and returns
        the result.
        @param first the first number; may not be <code>null</code>
        @param second the second number; may not be <code>null</code>
        @aribaapi ariba
    */
    public static Number multiply (Number first, Number second)
    {
        Arithmetic arithmetic = getArithmetic(first, second);
        return arithmetic != null ? arithmetic.multiply(first, second) : null;
    }

    /**
        Divides <code>first</code> by <code>second</code> and returns
        the result.
        @param first the first number; may not be <code>null</code>
        @param second the second number; may not be <code>null</code>
        @aribaapi ariba
    */
    public static Number divide (Number first, Number second)
    {
        Arithmetic arithmetic = getArithmetic(first, second);
        return arithmetic != null ? arithmetic.divide(first, second) : null;
    }

    /**
        Compares <code>first</code> to <code>second</code> and returns
        the result.
        @param first the first number; may not be <code>null</code>
        @param second the second number; may not be <code>null</code>
        @aribaapi ariba
    */
    public static int compare (Number first, Number second)
    {
        Arithmetic arithmetic = getArithmetic(first, second);
        return arithmetic.compare(first, second);
    }
}
