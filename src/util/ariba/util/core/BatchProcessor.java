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

    $Id: //ariba/platform/util/core/ariba/util/core/BatchProcessor.java#2 $
*/
package ariba.util.core;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

/**
    Convenience class that abstracts the notion of
    processing a collection of objects in chunks of a given size.
    <p/>

    @aribaapi ariba
*/
public abstract class BatchProcessor
{
    /**
        Processes the supplied <code>collection</code>. <p/>

        @throws BatchProcessingException if there is an exception procesing
                <code>collection</code>
        @aribaapi ariba
    */
    public abstract void processBatch (Collection collection)
    throws BatchProcessingException;

    /**
        Represents the position "before the first element" in a collection.
        @aribaapi private
    */
    private static final Object InitialSentinel = new Object();

    /**
        Processes the supplied <code>collection</code> which may be of any size.
        This implementation splits <code>collection</code> up into batches
        and repeatedly calls {@link #processBatch} as necessary. <p/>

        If <code>collection</code> is a <code>List</code>, then this
        method calls {@link #processBatch} with <code>Lists</code>. Similarly
        for <code>SortedSets</code>. <p/>
     
        Each of the calls to
        <code>process()</code> satisfies the following conditions: <ul>
        <li> the size of the collection argument is greater than zero and
             determined by the <code>sizer</code>
        <li> the order of the collection is preserved
        <li> all elements are passed exactly once.
        </ul><p>

        @throws BatchProcessingException if there is an exception procesing
                the batches
        @aribaapi ariba
    */
    public final void process (Collection collection, BatchSizer sizer)
    throws BatchProcessingException
    {
        if (sizer instanceof BatchSizer.Fixed && collection instanceof List) {
            BatchSizer.Fixed fixed = (BatchSizer.Fixed)sizer;
            if (fixed.currentSize() == 0) {
                process((List)collection, fixed.batchSize());
                return;
            }
        }
        SortedSet set = (collection instanceof SortedSet) ? (SortedSet)collection : null;
        List list = (collection instanceof List) ? (List)collection : null;
        List subList = (set == null && list == null) ? ListUtil.list() : null;
        Object begin = InitialSentinel;
        int beginIdx = 0;
        boolean doProcessBatch = false;
        int idx = 0;
        for (Object object : collection)
        {
            if (doProcessBatch) {
                sizer.reset();
                Collection subCollection;
                if (set != null) {
                    subCollection = (begin == InitialSentinel)
                                    ? set.headSet(object)
                                    : set.subSet(begin, object);
                    begin = object;
                }
                else if (list != null) {
                    subCollection = list.subList(beginIdx, idx);
                    beginIdx = idx;
                }
                else {
                    subCollection = subList;
                    subList = ListUtil.list();
                }
                processBatch(subCollection);
            }
            ++idx;
            if (subList != null) {
                subList.add(object);
            }
            doProcessBatch = sizer.addToBatch(object);
        }
        Collection subCollection;
        if (set != null) {
            subCollection = (begin == InitialSentinel) ? set : set.tailSet(begin);
        }
        else if (list != null) {
            subCollection = list.subList(beginIdx, list.size());
        }
        else {
            subCollection = subList;
        }
        if (!subCollection.isEmpty()) {
            processBatch(subCollection);
        }
    }

    /**
        Processes the supplied <code>collection</code> which may be of any size.
        This implementation splits <code>collection</code> up into batches
        and repeatedly calls {@link #processBatch} as necessary. <p/>

        If <code>collection</code> is a <code>List</code>, then this
        method calls {@link #processBatch} with <code>Lists</code>. Similarly
        for <code>SortedSets</code>. <p/>

        Each of the calls to
        <code>process()</code> satisfies the following conditions: <ul>
        <li> the size of the collection argument is greater than zero and less
             than or equal to the batch size
        <li> the order of the collection is preserved
        <li> all elements are passed exactly once.
        </ul><p>

        @throws BatchProcessingException if there is an exception procesing
                the batches
        @aribaapi ariba
    */
    public final void process (Collection collection, int batchSize)
    throws BatchProcessingException
    {
        if (collection instanceof List) {
            process((List)collection, batchSize);
        }
        else {
            process(collection, new BatchSizer.Fixed(batchSize));
        }
    }

    /**
        Processes the supplied <code>list</code> which may be of any size.
        This implementation splits <code>list</code> up into batches
        and repeatedly calls {@link #processBatch} as necessary.
        Each of the calls to
        <code>process()</code> satisfies the following conditions: <ul>
        <li> the size of the list argument is greater than zero and less
             than or equal to the batch size
        <li> the order of the list is preserved
        <li> all elements are passed exactly once.
        </ul><p>

        @throws BatchProcessingException if there is an exception procesing
                the batches
        @aribaapi ariba
    */
    public final void process (List list, int batchSize)
    throws BatchProcessingException
    {
        int remaining = list.size();
        if (remaining <= batchSize) {
            processBatch(list);
        }
        else {
            int startIdx = 0;
            do {
                int length = Math.min(batchSize, remaining);
                List subList = list.subList(startIdx, startIdx + length);
                processBatch(subList);
                remaining -= length;
                startIdx += length;
            }
            while (remaining > 0);
        }
    }
}
