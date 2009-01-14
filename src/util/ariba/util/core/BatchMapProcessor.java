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

    $Id: //ariba/platform/util/core/ariba/util/core/BatchMapProcessor.java#2 $
*/
package ariba.util.core;

import java.util.Map;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
    Convenience class that abstracts the notion of processing a map of
    objects in batches. <p/>

    @aribaapi ariba
*/
public abstract class BatchMapProcessor
{
    /**
        Processes the supplied <code>map</code>. <p/>

        @throws BatchProcessingException if there is an exception procesing
                <code>collection</code>
        @aribaapi ariba
    */
    public abstract void processBatch (Map map)
    throws BatchProcessingException;

    private static final Map.Entry InitialSentinel = new Map.Entry()
    {
        public Object getKey ()
        {
            return null;
        }

        public Object getValue ()
        {
            return null;
        }

        public Object setValue (Object value)
        {
            return null;
        }
    };

    /**
        Processes the supplied <code>map</code> which may be of any size.
        This implementation splits <code>map</code> up into batches
        and repeatedly calls {@link #processBatch} as necessary. <p/>

        If <code>map</code> is a <code>SortedMap</code>, then this
        method calls {@link #processBatch} with <code>SortedMaps</code>. <p/>

        Each of the calls to
        <code>process()</code> satisfies the following conditions: <ul>
        <li> the size of the map argument is greater than zero and
             determined by the <code>sizer</code>
        <li> the order of the map is preserved
        <li> all elements are passed exactly once.
        </ul><p>

        @throws BatchProcessingException if there is an exception procesing
                the batches
        @aribaapi ariba
    */
    public final void process (Map map, BatchSizer sizer)
    throws BatchProcessingException
    {
        SortedMap sortedMap = (map instanceof SortedMap) ? (SortedMap) map : null;
        Map subMap = (sortedMap == null) ? new LinkedHashMap() : null;
        Map.Entry begin = InitialSentinel;
        boolean doProcessBatch = false;
        for (Iterator iter=map.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)iter.next();
            if (doProcessBatch) {
                sizer.reset();
                Map sub;
                if (sortedMap != null) {
                    sub = (begin == InitialSentinel)
                          ? sortedMap.headMap(entry.getKey())
                          : sortedMap.subMap(begin.getKey(), entry.getKey());
                    begin = entry;
                }
                else {
                    sub = subMap;
                    subMap = new LinkedHashMap();
                }
                processBatch(sub);
            }
            if (subMap != null) {
                subMap.put(entry.getKey(), entry.getValue());
            }
            doProcessBatch = sizer.addToBatch(entry);
        }
        Map sub;
        if (sortedMap != null) {
            sub = (begin == InitialSentinel)
                  ? sortedMap
                  : sortedMap.tailMap(begin.getKey());
        }
        else {
            sub = subMap;
        }
        if (!sub.isEmpty()) {
            processBatch(sub);
        }
    }

    /**
        Processes the supplied <code>map</code> which may be of any size.
        This implementation splits <code>map</code> up into batches
        and repeatedly calls {@link #processBatch} as necessary. <p/>

        If <code>map</code> is a <code>SortedMap</code>, then this
        method calls {@link #processBatch} with <code>SortedMaps</code>. <p/>
     
        Each of the calls to
        <code>process()</code> satisfies the following conditions: <ul>
        <li> the size of the map argument is greater than zero and less
             than or equal to the batch size
        <li> the order of the map is preserved
        <li> all elements are passed exactly once.
        </ul><p>

        @throws BatchProcessingException if there is an exception procesing
                the batches
        @aribaapi ariba
    */
    public final void process (Map map, int batchSize)
    throws BatchProcessingException
    {
        process(map, new BatchSizer.Fixed(batchSize));
    }
}
