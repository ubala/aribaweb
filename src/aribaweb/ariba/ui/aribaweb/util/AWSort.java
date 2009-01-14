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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWSort.java#5 $
*/

package ariba.ui.aribaweb.util;

// This code adapted from ariba.util.core.Sort

public final class AWSort extends AWBaseObject
{
    // When this was 16 it didn't work.  I had to lower it to 4 to get it to work.  Wassuuuup?
    static final int MinQuickSortLength = 4;

    ///////////////
    // sort int[]
    ///////////////
    public static int[] quicksort (int[] intArray)
    {
        int intArrayLength = intArray.length;
        int[] intArrayCopy = new int[intArrayLength];
        System.arraycopy(intArray, 0, intArrayCopy, 0, intArrayLength);
        quicksort(intArrayCopy, 0, intArrayLength - 1);
        return intArrayCopy;
    }

    /**
        QuickSorts a[ first .. last ] down to partitions of length
        MinQuickSortLength and larger. The final insertion sort above
        will then sort each of the final partitions.
    */
    private static void quicksort (int[] a, int first, int last)
    {
        while (last - first + 1 >= MinQuickSortLength) {
               // partition a such that a[ first ... m - 1 ] <= v <=
                // a[ m ... last ], for some m and v and first < m. */
            int m = partition(a, first, last);
                // recursively quicksort the smaller partition and
                // tail-recursively quicksort the larger partition.  This
                // ensures that we'll use at most O (log n) stack space, even
                // on degenerate inputs.
            if (m - first < last - m + 1) {
                quicksort(a, first, m - 1);
                first = m;
            }
            else {
                quicksort(a, m, last);
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
    private static int partition (int[] a, int first, int last)
    {
        /* Find the median of the first, middle, and last elements */
        int af = a[first];
        int am = a[(first+last)/2];
        int al = a[last];
        int median;

        if (compare(af, am) < 0) {
            if (compare(am, al) < 0) {
                median = am; /* af < am, am < al */
            }
            else {
                /* af < am, al <= am */
                if (compare(af, al) < 0) {
                    median = al; /* af < al, al <= am */
                }
                else {
                    median = af; /* al <= af, af < am */
                }
            }
        }
        else {
            /* m <= f */
            if (compare(af, al) < 0) {
                median = af; /* am <= af, af < al */
            }
            else {
                /* am <= af, al <= af */
                if (compare(am, al) < 0) {
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
            for (; compare(a[i], median) < 0; i++) {
                ;
            }

            /* Advance j backward until a[ j ] <= median. */
            for (; compare(median, a[j]) < 0; j--) {
                ;
            }

            /* If i and j have crossed, then we know that
                a[ i - 1 ] <= median <= a[ i ] and i > first. */
            if (i >= j) {
                return i;
            }

            /* Swap a[i] and a[j]. */
            int temp = a[i];
            a[i] = a[j];
            a[j] = temp;
            /* Now, a[ first ... i ] <= median <= a[ j ... last ]. */
            i++;
            j--;
        }
    }

    private static int compare (int a, int b)
    {
        if (a < b) {
            return -1;
        }
        else if (a > b) {
            return 1;
        }
        return 0;
    }
    
    ///////////////
    // sort long[]
    ///////////////
    public static long[] quicksort (long[] longArray)
    {
        int longArrayLength = longArray.length;
        long[] longArrayCopy = new long[longArrayLength];
        System.arraycopy(longArray, 0, longArrayCopy, 0, longArrayLength);
        quicksort(longArrayCopy, 0, longArrayLength - 1);
        return longArrayCopy;
    }

    /**
        QuickSorts a[ first .. last ] down to partitions of length
        MinQuickSortLength and larger. The final insertion sort above
        will then sort each of the final partitions.
    */
    private static void quicksort (long[] a, int first, int last)
    {
        while (last - first + 1 >= MinQuickSortLength) {
               // partition a such that a[ first ... m - 1 ] <= v <=
                // a[ m ... last ], for some m and v and first < m. */
            int m = partition(a, first, last);
                // recursively quicksort the smaller partition and
                // tail-recursively quicksort the larger partition.  This
                // ensures that we'll use at most O (log n) stack space, even
                // on degenerate inputs.
            if (m - first < last - m + 1) {
                quicksort(a, first, m - 1);
                first = m;
            }
            else {
                quicksort(a, m, last);
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
    private static int partition (long[] a, int first, int last)
    {
        /* Find the median of the first, middle, and last elements */
        long af = a[first];
        long am = a[(first+last)/2];
        long al = a[last];
        long median;

        if (compare(af, am) < 0) {
            if (compare(am, al) < 0) {
                median = am; /* af < am, am < al */
            }
            else {
                /* af < am, al <= am */
                if (compare(af, al) < 0) {
                    median = al; /* af < al, al <= am */
                }
                else {
                    median = af; /* al <= af, af < am */
                }
            }
        }
        else {
            /* m <= f */
            if (compare(af, al) < 0) {
                median = af; /* am <= af, af < al */
            }
            else {
                /* am <= af, al <= af */
                if (compare(am, al) < 0) {
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
            for (; compare(a[i], median) < 0; i++) {
                ;
            }

            /* Advance j backward until a[ j ] <= median. */
            for (; compare(median, a[j]) < 0; j--) {
                ;
            }

            /* If i and j have crossed, then we know that
                a[ i - 1 ] <= median <= a[ i ] and i > first. */
            if (i >= j) {
                return i;
            }

            /* Swap a[i] and a[j]. */
            long temp = a[i];
            a[i] = a[j];
            a[j] = temp;
            /* Now, a[ first ... i ] <= median <= a[ j ... last ]. */
            i++;
            j--;
        }
    }

    private static int compare (long a, long b)
    {
        if (a < b) {
            return -1;
        }
        else if (a > b) {
            return 1;
        }
        return 0;
    }
}
