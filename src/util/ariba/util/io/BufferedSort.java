/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/io/BufferedSort.java#2 $
*/
package ariba.util.io;

import ariba.util.core.ListUtil;
import ariba.util.core.SetUtil;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
    Resource-limited sort

    The resource limited sort uses a buffer to manage. First, the overall list is split into a set of buffers constrained by
    the memory resources. Then, we merge sort that list of buffers until we arrive at only one buffer. The number of buffers
    that are merged are restricted in 2 ways:

    - minimally 2 buffers are merged
    - maximally maxBufferLimit buffers are merged
    - a buffer may refuse to open, which allows for a dynamic control over the number of concurrently open buffers

    This is geared towards file handles in a multi-threaded environment. If file reads are expensive, it makes sense to
    merge as many buffers as possible, but if many threads do this, it could lead to an exhaustion of file handles. This
    allows us to do better than the 2 minimum.

    @aribaapi ariba
*/

public class BufferedSort
{
    public static interface Resource
    {
        /*
            Returns some sort of "size" which we want to overall limit.
        */
        public long resourceSize();
    }


    public static interface SortBuffer
    {
        /*
           Next available resource
        */
        public Resource next() throws IOException;
        /*
           Save a resource to the buffer
        */
        public void write (Resource value) throws IOException;

        /*
           Open a sort buffer for read. If there is a limit of
           the number of buffers that can be read from at the same time,
           the method may return false instead of opening the buffer, unless
           force is given.
        */
        public boolean open (boolean force) throws IOException;
        /*
           Close the buffer. Closed buffers should be cheap.
        */
        public void close () throws IOException;

        /*
           Indicated this buffer is no longer needed
        */
        public void dispose ();

        /*
            Creates a new sort buffer, ready to be written to
        */
        public SortBuffer newSortBuffer()
                throws IOException;
    }

    protected SortBuffer source;
    protected long resourceLimit;
    protected long upperBufferLimit;
    protected long minConcurrentBfLimit;
    protected ComparatorHelper comparator;

    protected List<SortBuffer> fromBuffers = ListUtil.list();
    protected List<SortBuffer> toBuffers = ListUtil.list();
    protected Set<SortBuffer> cleanup = SetUtil.set();


    /**
        Prepare for a sort, using a source buffer. This buffer will be opened and read from, and
        child buffers will be created using its factory.

        Note for all intermediate buffers "dispose()" will be called, but it will not be called for
        either the source or the result.

        @param source - the source data
        @param resourceLimit   - maximum resource count of sortable elements
        @param upperBufferLimit - maximum number of sort buffers that are merged simultaneously
        @param comparator - the comparator
     */
    public BufferedSort (SortBuffer source,
                         long resourceLimit,
                         long upperBufferLimit,
                         Comparator<Resource> comparator)
    {
        this.source = source;
        this.resourceLimit = resourceLimit;
        this.upperBufferLimit = upperBufferLimit;
        this.comparator = new ComparatorHelper(comparator);
    }

    /**
        Performs the sort.

        @return a sorted sortBuffer, if a sort was performed. If null is returned, the source is sorted (however
                a sorted source doesn't mean null will be returned for sure).
        @throws IOException
     */
    public SortBuffer sort ()
            throws IOException
    {
        SortBuffer result = null;
        try {
            sortSplit();
            while (toBuffers.size() > 1) {
                merge();
            }
            if (toBuffers.size() == 1) {
                result = toBuffers.get(0);
                cleanup.remove(result);
            }
            return result;
        }
        finally {
            doCleanup();
        }
    }

    private void doCleanup ()
    {
        for (SortBuffer s : cleanup) {
            try {
                s.close();
            }
            catch (IOException ioe) {
                // ignore here
            }
            s.dispose();
        }
    }

    /**
        Merge sort sorted chunks into new chunks.     
    */
    private void merge ()
            throws IOException
    {
        fromBuffers.addAll(toBuffers);
        toBuffers.clear();

        long target = fromBuffers.size();
        if (target > upperBufferLimit) {
            target = Math.min(upperBufferLimit, target/2);
        }
        if (target < 2) {
            target = 2;
        }

        while (fromBuffers.size() != 0) {
            List<SortBuffer> mergable = ListUtil.list();
            List<SortBuffer> remaining = ListUtil.list();
            int numMergable = 0;

            for (SortBuffer source : fromBuffers) {
                if (numMergable < target) {
                    if (numMergable < 2) {
                        source.open(true);
                        numMergable++;
                        mergable.add(source);
                    }
                    else if (source.open(false)) {
                        numMergable++;
                        mergable.add(source);
                    }
                    else {
                        remaining.add(source);
                    }
                }
                else {
                    remaining.add(source);
                }
            }
            if (remaining.size() == 1 &&
                mergable.size() != 2) {
                // don't degenerate to linear unless we are totally constrained
                SortBuffer sort = fromBuffers.remove(fromBuffers.size()-1);
                sort.close();
                remaining.add(sort);
            }
            toBuffers.add(mergeSort(mergable));
            fromBuffers = remaining;
        }
    }

    private static class ComparatorHelper
        implements Comparator<SortHelper>
    {
        private Comparator<Resource> ref;

        public ComparatorHelper (Comparator<Resource> ref)
        {
            this.ref = ref;
        }


        public int compare (SortHelper o1, SortHelper o2)
        {
            int res = ref.compare(o1.orig, o2.orig);
            if (res != 0) {
                return res;
            }
            return o1.position - o2.position;
        }
    }

    /*
         Serves 2 purposes: disambiguate otherwise same items, and
         also keep track of an additional position argument that the
         merge sort uses.
    */
    private static class SortHelper
        implements Resource
    {
        public Resource orig;
        public int position;

        public SortHelper (Resource elem, int position)
        {
            orig = elem;
            this.position = position;
        }

        public long resourceSize ()
        {
            return orig.resourceSize();
        }

        public int hashCode ()
        {
            return orig.hashCode();
        }

        public boolean equals (Object o)
        {
            return (o instanceof SortHelper) &&
                   orig.equals(((SortHelper)o).orig) &&
                   position == ((SortHelper)o).position;
        }
    }


    /*
        Merge multiple files at once
    */
    private SortBuffer mergeSort (List<SortBuffer> mergable)
            throws IOException
    {
        SortBuffer[] mergeList = mergable.toArray(new SortBuffer[mergable.size()]);

        SortedSet<SortHelper> sorted = new TreeSet<SortHelper>(comparator);

        SortBuffer output = source.newSortBuffer();
        cleanup.add(output);

        for (int i = 0;i < mergeList.length;i++) {
            Resource res = mergeList[i].next();
            if (res != null) {
                sorted.add(new SortHelper(res, i));
            }
        }

        while (!sorted.isEmpty()) {
            SortHelper smallest = sorted.first();
            sorted.remove(smallest);
            output.write(smallest.orig);
            int position = smallest.position;
            Resource replacement = mergeList[position].next();
            if (replacement == null) {
                mergeList[position] = null;
            }
            else {
                sorted.add(new SortHelper(replacement, position));
            }
        }

        output.close();

        for (SortBuffer s : mergable) {
            s.close();
            s.dispose();
            cleanup.remove(s);
        }

        return output;
    }

    /*
         Split the initial set of data into sorted chunks according to resource limits.
    */
    private void sortSplit ()
            throws IOException
    {
        long resourcesUsed;
        int position = 0;

        try {
            source.open(true);
            Resource current = source.next();
            while (current != null) {
                resourcesUsed = 0;
                SortedSet<SortHelper> sortBuffer = new TreeSet<SortHelper>(comparator);

                while ((resourcesUsed < resourceLimit || sortBuffer.size() < 2) &&
                       current != null) {
                    resourcesUsed += current.resourceSize();
                    sortBuffer.add(new SortHelper(current, position));
                    position++;
                    current = source.next();
                }

                if (!sortBuffer.isEmpty()) {
                    SortBuffer out = source.newSortBuffer();
                    cleanup.add(out);
                    try {
                        for (SortHelper entry : sortBuffer) {
                            out.write(entry.orig);
                        }
                    }
                    finally {
                        out.close();
                    }
                    toBuffers.add(out);
                }
            }
        }
        finally {
            source.close();
        }
    }
}
