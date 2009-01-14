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

    $Id: //ariba/platform/util/core/ariba/util/core/ArrayUtil.java#9 $
*/

package ariba.util.core;

import java.io.PrintStream;
import java.util.List;

/**
    Array Utilities. These are helper functions for dealing with
    arrays.

    @aribaapi documented
*/
public final class ArrayUtil
{

    /** keep people from creating this class */
    private ArrayUtil ()
    {}

    /**
        Convert a List into an Object array with null check.
        
        @param vector a List to pull the contents from

        @return a new Object array containing all elements of
        <b>vector</b>, or a new empty Object array if <b>vector</b> was
        <b>null</b>
        @aribaapi documented
    */
    public static Object[] array (List vector)
    {
        if (vector == null) {
            return new Object[0];
        }
        return vector.toArray();
    }

    /**
        Helper function to create a new empty Object array

        @return a new empty Object array
        @aribaapi documented
    */
    public static Object[] array ()
    {
        return new Object[] { };
    }

    /**
        Helper function to create a new Object array containing one
        Object.

        @param a the first Object to add to the List

        @return a new Object array containing the specified object
        @aribaapi documented
    */
    public static Object[] array (Object a)
    {
        return new Object[] { a };
    }

    /**
        Helper function to create a new Object array containing two
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List

        @return a new Object array containing the specified objects
        @aribaapi documented
    */
    public static Object[] array (Object a, Object b)
    {
        return new Object[] { a, b };
    }

    /**
        Helper function to create a new Object array containing three
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List

        @return a new Object array containing the specified objects
        @aribaapi documented
    */
    public static Object[] array (Object a, Object b, Object c)
    {
        return new Object[] { a, b, c };
    }

    /**
        Helper function to create a new Object array containing four
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List

        @return a new Object array containing the specified objects
        @aribaapi documented
    */
    public static Object[] array (Object a, Object b, Object c, Object d)
    {
        return new Object[] { a, b, c, d };
    }

    /**
        Helper function to create a new Object array containing five
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List

        @return a new Object array containing the specified objects
        @aribaapi documented
    */
    public static Object[] array (Object a, Object b, Object c, Object d,
                                  Object e)
    {
        return new Object[] { a, b, c, d, e };
    }

    /**
        Helper function to create a new Object array containing six
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List

        @return a new Object array containing the specified objects
        @aribaapi documented
    */
    public static Object[] array (Object a, Object b, Object c, Object d,
                                  Object e, Object f)
    {
        return new Object[] { a, b, c, d, e, f };
    }

    /**
        Helper function to create a new Object array containing seven
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List

        @return a new Object array containing the specified objects
        @aribaapi documented
    */
    public static Object[] array (Object a, Object b, Object c, Object d,
                                  Object e, Object f, Object g)
    {
        return new Object[] { a, b, c, d, e, f, g };
    }

    /**
        Helper function to create a new Object array containing eight
        Objects.

        @param a the first Object to add to the List
        @param b the second Object to add to the List
        @param c the third Object to add to the List
        @param d the fourth Object to add to the List
        @param e the fifth Object to add to the List
        @param f the sixth Object to add to the List
        @param g the seventh Object to add to the List
        @param h the eighth Object to add to the List

        @return a new Object array containing the specified objects
        @aribaapi documented
    */
    public static Object[] array (Object a, Object b, Object c, Object d,
                                  Object e, Object f, Object g, Object h)
    {
        return new Object[] { a, b, c, d, e, f, g, h };
    }

    /**
         Helper function to prepend an Object to an array and return the
         new array.

         @param a the Object to be prepended
         @param in the array to prepend a onto

         @return a new Object array containing a + the elements of in
         @aribaapi documented
    */
    public static Object[] prepend (Object a, Object[] in)
    {
        Object[] out = new Object[in.length + 1];
        out[0] = a;
        System.arraycopy(in, 0, out, 1, in.length);
        return out;
    }

    /**
         Helper function to append an Object to an array and return the
         new array.

         @param in the array to prepend a onto
         @param a the Object to be appended

         @return a new Object array containing the elements of in + a
         @aribaapi documented
    */
    public static Object[] append (Object[] in, Object a)
    {
        Object[] out = new Object[in.length + 1];
        out[in.length] = a;
        System.arraycopy(in, 0, out, 0, in.length);
        return out;
    }

    /**
        Determine if an Object array is null or empty.
        
        @param array the Object array to check

        @return <b>true</b> if <B>array</B> is null or empty,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean nullOrEmptyArray (Object[] array)
    {
        return (array == null || array.length == 0);
    }

    /**
        Determine if an int array is null or empty.

        @param array the int array to check

        @return <b>true</b> if <B>array</B> is null or empty,
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean nullOrEmptyIntArray (int[] array)
    {
        return (array == null || array.length == 0);
    }
    
    /**
        Write an array into a PrintStream.

        @param out a PrintStream to write the output to
        @param head a header to prefix the list with
        @param args the array of Strings to print

        @see #formatArray(String, String[])

        @aribaapi private
    */
    public static void printArray (PrintStream out, String head, String args[])
    {
        out.println(formatArray(head, args));
    }

    /**
        Returns a String with the array printed as formatted text
        
        @param header a header to prefix the list with
        @param args the array of Strings to print

        @return a String with the array printed as formatted text.

        @aribaapi private
    */
    public static String formatArray (String header, String args[])
    {
        if (args == null) {
            return Fmt.S("%s (null)", header);
        }
        if (args.length == 0) {
            return Fmt.S("%s (<empty>)", header);
        }
        FormatBuffer retval = new FormatBuffer();
        Fmt.B(retval, "%s", header);
        for (int i = 0; i < args.length; i++) {
            Fmt.B(retval, " args[%s] = (%s)", Constants.getInteger(i), args[i]);
        }

        return retval.toString();
    }

    /**
        Check if the array contains an object.
        The == operator is used to compare objects
        @param array the array to check
        @param x the object to check for
        @return true if the object is in the array

        @aribaapi public
    */
    public static boolean containsIdentical (Object[] array, Object x)
    {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == x) {
                return true;
            }
        }
        return false;
    }

    /**
        Check if the array contains an object.
        Object.equals() is used to compare objects.
        @param array the array to check
        @param x the object to check for
        @return true if the object is in the array

        @aribaapi public
    */
    public static boolean contains (Object[] array, Object x)
    {
        return -1 != indexOf(array, x);
    }

    /**
        Find the location of an object in the array
        Object.equals() is used to compare objects.
        @param array the array to check
        @param x The object to look for
        @return the index of the object, or -1 if it is not found

        @aribaapi public
    */
    public static int indexOf (Object[] array, Object x)
    {
        for (int i = 0; i < array.length; i++) {
            if (SystemUtil.equal(x, array[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
        Determines if the two arrays are equal
        Two arrays are equal if their size is identical and
        if for each position in the array, both corresponding
        elements are equal

        @param a1 first array
        @param a2 second array
        @return <code>true</code> if the two arrays are equal
        @aribaapi documented
    */
    public static boolean arrayEquals (Object[] a1, Object[] a2)
    {
        if (a1 == null && a2 == null) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }
        if (a1.length != a2.length) {
            return false;
        }
        for (int e = 0, l = a1.length; e < l; e++) {
            if (!SystemUtil.objectEquals(a1[e], a2[e])) {
                return false;
            }
        }
        return true;
    }

    /**
        Reverses the order of the objects in this array. <p/>

        @param objects the object array  
        @aribaapi ariba
    */
    public static void reverse (Object[] objects)
    {
        int midpoint = objects.length / 2;
        for (int i=0; i<midpoint; ++i) {
            int oppositeIndex = objects.length - 1 - i;
            Object temp = objects[i];
            objects[i] = objects[oppositeIndex];
            objects[oppositeIndex] = temp;
        }
    }
}
