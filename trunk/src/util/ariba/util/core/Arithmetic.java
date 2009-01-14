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

    $Id: //ariba/platform/util/core/ariba/util/core/Arithmetic.java#4 $
*/
package ariba.util.core;

import java.math.BigInteger;
import java.math.BigDecimal;

/**
    Arithmetic is an abstract class that encapsulates the idea of doing simple
    arithmetic operations on instances of the sub-types of
    <code>java.lang.Number</code>. <p/>

    For example: {@link #IntegerArithmetic} does integer arithmetic on the
    <code>Numbers</code> passed to it via the <code>Number.intValue()</code>
    method. <p/>

    @aribaapi ariba
*/
public abstract class Arithmetic
{
    //--------------------------------------------------------------------------
    // public abstract member methods

    /**
        Adds <code>first</code> and <code>second</code> and returns the result. <p/>
        @aribaapi ariba
    */
    public abstract Number add (Number first, Number second);

    /**
        Subtracts <code>second</code> from <code>first</code> and
        returns the result. <p/>
        @aribaapi ariba
    */
    public abstract Number subtract (Number first, Number second);

    /**
        Multiplies <code>first</code> by <code>second</code> and returns the result. <p/>
        @aribaapi ariba
    */
    public abstract Number multiply (Number first, Number second);

    /**
        Divides <code>first</code> by <code>second</code> and returns the result. <p/>
        @aribaapi ariba
    */
    public abstract Number divide (Number first, Number second);

    /**
        Compares <code>first</code> and <code>second</code> and returns the result. <p/>
        @aribaapi ariba
    */
    public abstract int compare (Number first, Number second);

    //--------------------------------------------------------------------------
    // public convenience member methods

    /**
        Evaluates <code>first</code> and <code>second</code> with respect to
        <code>operation</code> which must be one of:
        <code>OpAdd, OpSubtract, OpMultiply, OpDivide</code>.
        @aribaapi ariba
    */
    public final Number evaluate (Number first, Operation operation, Number second)
    {
        switch (operation.rank()) {
        case Operation.OpAdd:
            return add(first, second);
        case Operation.OpSubtract:
            return subtract(first, second);
        case Operation.OpMultiply:
            return multiply(first, second);
        case Operation.OpDivide:
            return divide(first, second);
        default:
            Assert.that(false, "Cannot evaluate '%s' and '%s' with respect " +
                               "to the invalid operation '%s",
                        first, second, operation);
        }
        return null;
    }

    //--------------------------------------------------------------------------
    // nested class

    /**
        Enumeration class capturing binary operations on instances of
        <code>java.lang.Number</code>. <p/>

        @aribaapi ariba
    */
    public static class Operation
    {
        public static final int OpAdd           = 1;
        public static final int OpSubtract      = 2;
        public static final int OpMultiply      = 3;
        public static final int OpDivide        = 4;
        public static final int OpCompare       = 5;

        /**
            Represents addition.
            @aribaapi ariba
        */
        public static final Operation Add      = new Operation("add",      OpAdd);
        /**
            Represents subtraction.
            @aribaapi ariba
        */
        public static final Operation Subtract = new Operation("subtract", OpSubtract);
        /**
            Represents multiplication.
            @aribaapi ariba
        */
        public static final Operation Multiply = new Operation("multiply", OpMultiply);
        /**
            Represents division.
            @aribaapi ariba
        */
        public static final Operation Divide   = new Operation("divide",   OpDivide);
        /**
            Represents comparision.
            @aribaapi ariba
        */
        public static final Operation Compare  = new Operation("compare",  OpCompare);

        //--------------------------------------------------------------------------
        // data members
        private String _name;
        private int _rank;

        /**
            @aribaapi private
        */
        private Operation (String name, int rank)
        {
            _name = name;
            _rank = rank;
        }

        //--------------------------------------------------------------------------
        // public methods

        /**
            @aribaapi ariba
        */
        public int rank ()
        {
            return _rank;
        }

        /**
            @aribaapi ariba
        */
        public String toString ()
        {
            return _name;
        }
    }


    //--------------------------------------------------------------------------
    // constants

    /*
        The following collection of constants represent the individual subtypes
        of java.lang.Number.
        They are powers of 2 to allow for bitwise operations and the order
        is important. Please do not change it.
    */

    /**
        Represents the <code>Byte</code> type.<p/>
        @aribaapi ariba
    */
    public static final int ByteType        = 1 << 0;
    /**
        Represents the <code>Short</code> type.<p/>
        @aribaapi ariba
    */
    public static final int ShortType       = 1 << 1;
    /**
        Represents the <code>Integer</code> type.<p/>
        @aribaapi ariba
    */
    public static final int IntegerType     = 1 << 2;
    /**
        Represents the <code>Long</code> type.<p/>
        @aribaapi ariba
    */
    public static final int LongType        = 1 << 3;
    /**
        Represents the <code>Float</code> type.<p/>
        @aribaapi ariba
    */
    public static final int FloatType       = 1 << 4;
    /**
        Represents the <code>Double</code> type.<p/>
        @aribaapi ariba
    */
    public static final int DoubleType      = 1 << 5;
    /**
        Represents the <code>BigInteger</code> type.<p/>
        @aribaapi ariba
    */
    public static final int BigIntegerType  = 1 << 6;
    /**
        Represents the <code>BigDecimal</code> type.<p/>
        @aribaapi ariba
    */
    public static final int BigDecimalType  = 1 << 7;

    /**
        @aribaapi ariba
    */
    public static final Arithmetic IntegerArithmetic = new Arithmetic()
    {
        public Number add (Number first, Number second)
        {
            return Constants.getInteger(first.intValue() + second.intValue());
        }

        public Number subtract (Number first, Number second)
        {
            return Constants.getInteger(first.intValue() - second.intValue());
        }

        public Number multiply (Number first, Number second)
        {
            return Constants.getInteger(first.intValue() * second.intValue());
        }

        public Number divide (Number first, Number second)
        {
            return Constants.getInteger(first.intValue() / second.intValue());
        }

        public int compare (Number first, Number second)
        {
            return MathUtil.sgn(first.intValue(), second.intValue());
        }
    };
    /**
        @aribaapi ariba
    */
    public static final Arithmetic LongArithmetic = new Arithmetic()
    {
        public Number add (Number first, Number second)
        {
            return Constants.getLong(first.longValue() + second.longValue());
        }

        public Number subtract (Number first, Number second)
        {
            return Constants.getLong(first.longValue() - second.longValue());
        }

        public Number multiply (Number first, Number second)
        {
            return Constants.getLong(first.longValue() * second.longValue());
        }

        public Number divide (Number first, Number second)
        {
            return Constants.getLong(first.longValue() / second.longValue());
        }

        public int compare (Number first, Number second)
        {
            return MathUtil.sgn(first.longValue(), second.longValue());
        }
    };
    /**
        @aribaapi ariba
    */
    public static final Arithmetic DoubleArithmetic = new Arithmetic()
    {
        public Number add (Number first, Number second)
        {
            return new Double(first.doubleValue() + second.doubleValue());
        }

        public Number subtract (Number first, Number second)
        {
            return new Double(first.doubleValue() - second.doubleValue());
        }

        public Number multiply (Number first, Number second)
        {
            return new Double(first.doubleValue() * second.doubleValue());
        }

        public Number divide (Number first, Number second)
        {
            return new Double(first.doubleValue() / second.doubleValue());
        }

        public int compare (Number first, Number second)
        {
            return Double.compare(first.doubleValue(), second.doubleValue());
        }
    };
    /**
        @aribaapi ariba
    */
    public static final Arithmetic BigIntegerArithmetic = new Arithmetic()
    {
        public Number add (Number first, Number second)
        {
            BigInteger firstValue = asBigInteger(first),
                       secondValue = asBigInteger(second);
            return firstValue.add(secondValue);
        }

        public Number subtract (Number first, Number second)
        {
            BigInteger firstValue = asBigInteger(first),
                       secondValue = asBigInteger(second);
            return firstValue.subtract(secondValue);
        }

        public Number multiply (Number first, Number second)
        {
            BigInteger firstValue = asBigInteger(first),
                       secondValue = asBigInteger(second);
            return firstValue.multiply(secondValue);
        }

        public Number divide (Number first, Number second)
        {
            BigInteger firstValue = asBigInteger(first),
                       secondValue = asBigInteger(second);
            return firstValue.divide(secondValue);
        }

        public int compare (Number first, Number second)
        {
            BigInteger firstValue = asBigInteger(first),
                       secondValue = asBigInteger(second);
            return firstValue.compareTo(secondValue);
        }
    };
    /**
        @aribaapi ariba
    */
    public static final Arithmetic BigDecimalArithmetic = new Arithmetic()
    {
        public Number add (Number first, Number second)
        {
            BigDecimal firstValue = asBigDecimal(first),
                       secondValue = asBigDecimal(second);
            return firstValue.add(secondValue);
        }

        public Number subtract (Number first, Number second)
        {
            BigDecimal firstValue = asBigDecimal(first),
                       secondValue = asBigDecimal(second);
            return firstValue.subtract(secondValue);
        }

        public Number multiply (Number first, Number second)
        {
            BigDecimal firstValue = asBigDecimal(first),
                       secondValue = asBigDecimal(second);
            return firstValue.multiply(secondValue);
        }

        public Number divide (Number first, Number second)
        {
            BigDecimal firstValue = asBigDecimal(first),
                       secondValue = asBigDecimal(second);
            return firstValue.divide(secondValue, BigDecimal.ROUND_HALF_UP);
        }

        public int compare (Number first, Number second)
        {
            BigDecimal firstValue = asBigDecimal(first),
                       secondValue = asBigDecimal(second);
            return firstValue.compareTo(secondValue);
        }
    };

    //--------------------------------------------------------------------------
    // public static methods

    /**
        @aribaapi ariba
    */
    public static BigInteger asBigInteger (Number number)
    {
        if (number instanceof BigInteger) {
            return (BigInteger)number;
        }
        return new BigInteger(Long.toString(number.longValue()));
    }

    /**
        @aribaapi ariba
    */
    public static BigDecimal asBigDecimal (Number number)
    {
        if (number instanceof BigDecimal) {
            return (BigDecimal)number;
        }
        else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger)number, 0);
        }
        return new BigDecimal(number.doubleValue());
    }

    /**
        Returns one of the constants representing
        @aribaapi ariba
    */
    public static int getNumberType (Number number)
    {
        if (number instanceof Integer) {
            return IntegerType;
        }
        if (number instanceof Double) {
            return DoubleType;
        }
        if (number instanceof Long) {
            return LongType;
        }
        if (number instanceof Float) {
            return FloatType;
        }
        if (number instanceof Byte) {
            return ByteType;
        }
        if (number instanceof Short) {
            return ShortType;
        }
        if (number instanceof BigDecimal) {
            return BigDecimalType;
        }
        if (number instanceof BigInteger) {
            return BigIntegerType;
        }
        return 0;
    }

    /**
        Returns the <code>Arithmetic</code> instance corresponding to the
        supplied <code>numberType</code>. <p/>

        @aribaapi ariba
    */
    public static Arithmetic getArithmeticFromNumberType (int numberType)
    {
        switch (numberType) {
        case ByteType:
        case ShortType:
        case IntegerType:
            return IntegerArithmetic;
        case LongType:
            return LongArithmetic;
        case FloatType: case DoubleType:
            return DoubleArithmetic;
        case BigIntegerType:
            return BigIntegerArithmetic;
        case BigDecimalType:
            return BigDecimalArithmetic;
        }
        Assert.that(false, "Cannot get Arithmetic for invalid number type '%s'",
                    Constants.getInteger(numberType));
        return null;
    }

    /**
        @aribaapi ariba
    */
    public static Number negate (Number number)
    {
        if (number instanceof Long) {
            return Constants.getLong(-number.longValue());
        }
        if (number instanceof Integer) {
            return Constants.getInteger(-number.intValue());
        }
        if (number instanceof Double) {
            return new Double(-number.doubleValue());
        }
        else if (number instanceof Float) {
            return new Float(-number.floatValue());
        }
        else if (number instanceof Short) {
            return new Short((short)-number.shortValue());
        }
        else if (number instanceof Byte) {
            return new Byte((byte)-number.shortValue());
        }
        else if (number instanceof BigInteger) {
            return ((BigInteger)number).negate();
        }
        else if (number instanceof BigDecimal) {
            return ((BigDecimal)number).negate();
        }
        else if (number == null) {
            return null;
        }
        Assert.that(false, "Number '%s' is not one of the known subtypes of the " +
                           "class java.lang.Number");
        return null;
    }
}
