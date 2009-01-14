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

    $Id: //ariba/platform/util/core/ariba/util/core/IntCompanionSort.java#4 $
*/

package ariba.util.core;

    // If there are any bugs in this IntCompanionSort, it would be worth checking the
    // Sort to see if the same bug exists. 

/**
    provides functions for sorting arrays of objects, with integer companion arrays

    @aribaapi documented
*/
public class IntCompanionSort extends Sort
{
    /**
        Sort two arrays in ascending order, using one array for the
        keys. One array has the keys used for the sort, and another
        companion int array is also sorted with the key array.

        @param a an array of objects to sort
        @param c1 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c1 do not affect the sort.
        @param c an object to that implements the comparison interface
        @aribaapi documented
    */
    public static void objects (Object[] a, int[] c1, Compare c)
    {
        objects(a, c1, c, SortAscending);
    }

    /**
        Sort two arrays in a specified order, using one array for the
        keys. One array has the keys used for the sort, and another
        companion int array is also sorted with the key array.

        @param a an array of objects to sort
        @param c1 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c1 do not affect the sort.
        @param c an object to that implements the comparison interface
        @param dir the direction to sort the objects in. Must be
        <b>SortAscending</b> or <b>SortDescending</b>

        @see #SortAscending
        @see #SortDescending
        @aribaapi documented
    */
    public static void objects (Object[] a, int[] c1, Compare c, int dir)
    {
        objects(a, c1, null, c, dir);
    }

    /**
        Sort three arrays in ascending order, using one array for the
        keys. One array has the keys used for the sort, and the other
        two companion int arrays are also sorted with the key array.

        @param a an array of objects to sort
        @param c1 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c1 do not affect the sort.
        @param c2 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c2 do not affect the sort.
        @param c an object to that implements the comparison interface
        @aribaapi documented
    */
    public static void objects (
        Object[] a, int[] c1, int[] c2, Compare c)
    {
        objects(a, c1, c2, c, SortAscending);
    }

    /**
        Sort three arrays in a specified order, using one array for
        the keys. One array has the keys used for the sort, and the
        other two companion int arrays are also sorted with the key array.

        @param a an array of objects to sort
        @param c1 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c1 do not affect the sort.
        @param c2 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c2 do not affect the sort.
        @param c an object to that implements the comparison interface
        @param dir the direction to sort the objects in. Must be
        <b>SortAscending</b> or <b>SortDescending</b>

        @see #SortAscending
        @see #SortDescending
        @aribaapi documented
    */
    public static void objects (
        Object[] a, int[] c1, int[] c2, Compare c, int dir)
    {
        Compare compare = (dir == SortDescending) ? Sort.reverseCompare(c) : c;
        objects(a, c1, c2, 0, a.length, compare);
    }

    /**
        Sort a range of elements in three arrays in ascending order,
        using one array for the keys. Sorts elements <B>first</B>
        through <B>first</B> + n - 1 of the object array <B>a</B>,
        using the Compare object <B>c</B> to compare elements.  The
        companion int arrays <B>c1</B> and <B>c2</B> are also sorted in
        parallel with <B>a</B>.

        @param a an array of objects to sort
        @param c1 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c1 do not affect the sort.
        @param c2 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c2 do not affect the sort.
        @param first the first element to sort
        @param n the number of elements to sort
        @param c an object to that implements the comparison interface
        @aribaapi documented
    */
    public static void objects (
        Object[] a, int[] c1, int[] c2, int first, int n, Compare c)
    {
        if (a.length >= MinQuickSortLength) {
            quickSort(a, c1, c2, first, first + n - 1, c);
        }
        insertionSort(a, c1, c2, first, first + n - 1, c);
    }

    /**
        Sort a range of elements in three arrays in a specified order,
        using one array for the keys. Sorts elements <B>first</B>
        through <B>first</B> + n - 1 of the object array <B>a</B>,
        using the Compare object <B>c</B> to compare elements.  The
        companion int arrays <B>c1</B> and <B>c2</B> are also sorted in
        parallel with <B>a</B>.

        @param a an array of objects to sort
        @param c1 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c1 do not affect the sort.
        @param c2 a companion int array to sort in parallel with the array
        <b>a</b>. The values in c2 do not affect the sort.
        @param first the first element to sort
        @param n the number of elements to sort
        @param c an object to that implements the comparison interface
        @aribaapi documented
        @param dir the direction to sort the objects in. Must be
        <b>SortAscending</b> or <b>SortDescending</b>

        @see #SortAscending
        @see #SortDescending
    */
    public static void objects (Object[] a, int[] c1, int[] c2, int first, int n,
                                Compare c, int dir)
    {
        Compare compare = (dir == SortDescending) ? Sort.reverseCompare(c) : c;
        objects(a, c1, c2, first, n, compare);
    }

    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/

    /**
        QuickSorts a[ first .. last ] down to partitions of length
        MinQuickSortLength and larger. The final insertion sort above
        will then sort each of the final partitions.
    */
    private static void quickSort (
        Object[] a, int[] c1, int[] c2, int first, int last, Compare c)
    {
        while (last - first + 1 >= MinQuickSortLength) {

                // partition a such that a[ first ... m - 1 ] <= v <=
                // a[ m ... last ], for some m and v and first < m. */
            int m = partition(a, c1, c2, first, last, c);

                // recursively quickSort the smaller partition and
                // tail-recursively quickSort the larger partition.  This
                // ensures that we'll use at most O (log n) stack space, even
                // on degenerate inputs.
            if (m - first < last - m + 1) {
                quickSort(a, c1, c2, first, m - 1, c);
                first = m;
            }
            else {
                quickSort(a, c1, c2, m, last, c);
                last = m - 1;
            }
        }
    }

    /**
        Reorders the elements in

        a[ first ... last ]

        such that

        a[ first ... m - 1 ] <= median <= a[ m ... last ]

        for some m, where "median" is the median value of the first,
        middle, and last elements. Returns m.
    */
    private static int partition (
        Object[] a, int[] c1, int[] c2, int first, int last, Compare c)
    {
        /* Find the median of the first, middle, and last elements */
        Object af = a[first];
        Object am = a[(first+last)/2];
        Object al = a[last];
        Object median;

        if (c.compare(af, am) < 0) {
            if (c.compare(am, al) < 0) {
                median = am; /* af < am, am < al */
            }
            else {
                /* af < am, al <= am */
                if (c.compare(af, al) < 0) {
                    median = al; /* af < al, al <= am */
                }
                else {
                    median = af; /* al <= af, af < am */
                }
            }
        }
        else {
            /* m <= f */
            if (c.compare(af, al) < 0) {
                median = af; /* am <= af, af < al */
            }
            else {
                /* am <= af, al <= af */
                if (c.compare(am, al) < 0) {
                    median = al; /* am < al, al <= af */
                }
                else {
                    median = am; /* al <= am, am <= af */
                }
            }
        }

        /* partition such that a[ first ... m ] <= median <= a[ m ... last ],
            for some m. */
        for (int i = first, j = last;;) {

            /* Advance i forward until median <= a[ i ]. */
            for (; c.compare(a[i], median) < 0; i++) {
                ;
            }

            /* Advance j backward until a[ j ] <= median. */
            for (; c.compare(median, a[j]) < 0; j--) {
                ;
            }

            /* If i and j have crossed, then we know that
                a[ i - 1 ] <= median <= a[ i ] and i > first. */
            if (i >= j) {
                return i;
            }

            /* Swap a[i] and a[j]. */
            Object temp = a[i];
            a[i] = a[j];
            a[j] = temp;
            int itemp;
            if (c1 != null) {
                itemp = c1[i];
                c1[i] = c1[j];
                c1[j] = itemp;
            }
            if (c2 != null) {
                itemp = c2[i];
                c2[i] = c2[j];
                c2[j] = itemp;
            }

            /* Now, a[ first ... i ] <= median <= a[ j ... last ]. */
            i++;
            j--;
        }
    }

    /** Sorts a[ first ... last ], using an insertion sort. */
    private static void insertionSort (
        Object[] a, int[] c1, int[] c2, int first, int last, Compare c)
    {
        for (int i = first + 1; i <= last; i++) {
            int j;
            Object ai = a[i];
            int temp1 = 0;
            int temp2 = 0;
            if (c1 != null) {
                temp1 = c1[i];
            }
            if (c2 != null) {
                temp2 = c2[i];
            }
            for (j = i - 1; j >= first; j--) {
                Object aj = a[j];
                if (c.compare(ai, aj) < 0) {
                    a[j+1] = aj;
                    if (c1 != null) {
                        c1[j+1] = c1[j];
                    }
                    if (c2 != null) {
                        c2[j+1] = c2[j];
                    }
                }
                else {
                    break;
                }
            }
            a[j+1] = ai;
            if (c1 != null) {
                c1[j+1] = temp1;
            }
            if (c2 != null) {
                c2[j+1] = temp2;
            }
        }
    }

    /*-----------------------------------------------------------------------
        Private Constructor
      -----------------------------------------------------------------------*/

    /** Private constructor to prevent instances of this class. */
    private IntCompanionSort ()
    {
    }

}
