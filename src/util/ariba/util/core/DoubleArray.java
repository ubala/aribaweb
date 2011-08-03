/*
    Copyright 1996-2010 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/DoubleArray.java#1 $
*/
package ariba.util.core;

/**
    A DynamicArray of Doubles
    @see ariba.util.core.DynamicArray

    @aribaapi public
*/
public class DoubleArray extends DynamicArray
{
    private Double[] localArray;

    public DoubleArray (int initialSize)
    {
        super(initialSize);
    }

    /**
     * An empty immutable <code>double</code> array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

    public Object[] alloc (int size)
    {
        localArray = new Double[size];
        return localArray;
    }

    /**
        Typesafe access to the storage array
        @return the Double[] array currently in use by this
        object. It is not copied.
        @aribaapi public
    */
    public final Double[] array ()
    {
        return localArray;
    }

    /**
       Converts the object array to it's primitive equivalent, treating nulls as zeroes.
       
       @aribaapi public
    */
    public final double[] toPrimitiveArray ()
    {
        if (localArray == null) {
            return null;
        }

        if (localArray.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }

        // remove unused capacity before returning samples
        trim();
        
        final double[] result = new double[localArray.length];
        for (int i = 0; i < localArray.length; ++i) {
            Double d = localArray[i];
            result[i] = (d == null ? 0.0 : d.doubleValue());
        }
        return result;
    }


    public void add (double d)
    {
        super.add(Double.valueOf(d));
    }

    public void setArray (Double[] array)
    {
        super.setArray(array);
        this.localArray = array;
    }

}
