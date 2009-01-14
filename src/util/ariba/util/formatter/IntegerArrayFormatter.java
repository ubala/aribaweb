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

    $Id: //ariba/platform/util/core/ariba/util/formatter/IntegerArrayFormatter.java#6 $
*/

package ariba.util.formatter;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.Compare;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Sort;
import java.text.ParseException;
import java.util.Locale;

/**
    <code>IntegerArrayFormatter</code> is a subclass of <code>Formatter</code>
    which is responsible for formatting <code>int</code> arrays and/or
    <code>Integer</code> arrays.

    @aribaapi documented
*/
public class IntegerArrayFormatter extends Formatter
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.IntegerArrayFormatter";


    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>IntegerArrayFormatter</code>.

        @aribaapi private
    */
    public IntegerArrayFormatter ()
    {
    }
    

    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given <code>Integer</code> array.
        The array is first sorted and then formatted like "5, 1, 7".  Ranges
        of consecutive numbers are also consolidated, so the array "1,3,4,5"
        would be turned into the string "1, 3-5".

        @param  array the <code>Integer</code> array to format into a string
        @return       a string representation of the <code>Integer</code> array
        @aribaapi documented
    */
    public static String getStringValue (Integer[] array)
    {
            // return empty string if array is null or empty
        if (ArrayUtil.nullOrEmptyArray(array)) {
            return("");
        }

        FastStringBuffer buf = new FastStringBuffer();

            // copy the array
        int count = array.length;
        Integer[] integers = new Integer[count];
        System.arraycopy(array, 0, integers, 0, count);

            // sort the ints
        Compare compare = Formatter.getFormatterForType(Constants.IntegerType);
        Sort.objects(integers, compare);

            // turn the array into a string
        boolean inRange = false;
        Integer previous = null;
        for (int i = 0; i < count; i++) {
            Integer current = integers[i];
            if (i > 0) {
                    // start a range if the current value is one more than the
                    // previous value
                if (current.intValue() == previous.intValue() + 1) {
                    inRange = true;
                    previous = current;
                        // allow code below to handle range at end of array
                    if (i < count - 1) {
                        continue;
                    }
                }
                    // either the current value is not one more than the
                    // previous, or we're at the end of the array
                if (inRange) {
                    buf.append("-");
                    buf.append(previous.toString());
                    inRange = false;
                        // bail out if we're closing a range at the end of the
                        // array, otherwise fall through to the code below
                    if (previous == current) {
                        break;
                    }
                }
            }

                // add the current value to the buffer, with a comma if
                // it's not the first value in the array
            if (i != 0) {
                buf.append(", ");
            }
            buf.append(current.toString());

                // keep track of the current value for next time...
            previous = current;
        }

        return buf.toString();
    }

    /**
        Returns a formatted string for the given <code>int</code> array.  The
        array is first sorted and then formatted like "5, 1, 7".  Ranges of
        consecutive numbers are also consolidated, so the array "1,3,4,5"
        would be turned into the string "1, 3-5".

        @param  array the <code>int</code> array to format into a string
        @return       a string representation of the <code>int</code> array
        @aribaapi documented
    */
    public static String getStringValue (int[] array)
    {
            // return empty string if array is null or empty
        if (ArrayUtil.nullOrEmptyIntArray(array)) {
            return("");
        }

            // convert ints to Integers
        int count = array.length;
        Integer[] integers = new Integer[count];
        for (int i = 0; i < count; ++i) {
            integers[i] = Constants.getInteger(array[i]);
        }

        return(getStringValue(integers));
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the given
        locale.  The object must be a non-null <code>Integer</code> array or
        <code>int</code> array.

        @param  value the array to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the array
        @aribaapi documented
    */
    protected String formatObject (Object value, Locale locale)
    {
        Assert.that((value instanceof int[] || value instanceof Integer[]),
                    "invalid type passed to IntegerFormatter.formatObject()");

        if (value instanceof Integer[]) {
            return getStringValue((Integer[])value);
        }
        else {
            return getStringValue((int[])value);
        }
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    protected Object parseString (String string, Locale locale)
      throws ParseException
    {
        throw new ParseException("parse not supported for integer arrays", 0);
    }

    public Object getValue (Object object, Locale locale)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        if ((object instanceof Integer[]) || (object instanceof int[])) {
            return object;
        }
        else {
            try {
                return parseString(object.toString(), locale);
            }
            catch (ParseException e) {
                return null;
            }
        }
    }


    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    protected int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof Integer[] || o1 instanceof int[], "invalid type");
        Assert.that(o2 instanceof Integer[] || o2 instanceof int[], "invalid type");

            // no clear way to sort one array before the other
        return 0;
    }
}
