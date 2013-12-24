/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/PerformanceStateCounter.java#15 $
*/

package ariba.util.core;

import java.util.Map;

/**
    A thread specific counter.
    @aribaapi ariba
*/
public class PerformanceStateCounter extends PerformanceStateCore
{
    private static final int DefaultLogFlags = LOG_COUNT;

    public PerformanceStateCounter (String name, int logRank, int logFlags)
    {
        super(name, logRank, logFlags);
    }

    public PerformanceStateCounter (String name, int logRank)
    {
        this(name, logRank, DefaultLogFlags);
    }

    public PerformanceStateCounter (String name)
    {
        this(name, DefaultLogRank);
    }

    protected PerformanceStateCore.Instance newInstance (String name)
    {
        return new Instance(name);
    }

    /**
        Add 1 to the count.
    */
    public void addCount ()
    {
        addCount(1, 0, null);
    }

    /**
        Add <code>quantity</code> to the count.
    */
    public void addCount (long quantity)
    {
        addCount(quantity, 0, null);
    }

    /**
     * Add 1 to the count of the specific type.
     */
    public void addCount (String type)
    {
        addCount(1, 0, type);
    }

    /**
        Add quantity to the count of the specific type.
    */
    public void addCount (long quantity, String type)
    {
        addCount(quantity, 0, type);
    }

    /**
        Add c1 to the count, and add c2 to the secondary count.
    */
    public void addCount (long c1, long c2)
    {
        addCount(c1, c2, null);
    }

    /**
        Add c1 to the count of the specific type, and add c2 to the secondary count of the
        specific type.
    */
    public void addCount (long c1, long c2, String type)
    {
        if (!PerformanceState.threadStateEnabled() ||
            PerformanceState.isRecordingSuspended()) {
            return;
        }
        Instance operationalObj = (Instance)this.instance();
        operationalObj.addCount(c1, c2, type);
    }

    /**
     * the real instance
     * @aribaapi ariba
     */

    public static class Instance extends PerformanceStateCore.Instance
    {
        protected long count = 0;
        protected long count2 = 0;
        
        protected Instance (String name)
        {
            super(name);
        }

        public void addCount (long additionalCount, long count2Inc, String type)
        {
            count += additionalCount;
            count2 += count2Inc;

            // ToDo: check if we're recording
            if (type != null) {
                /*
                    Fix: 1-BYV5PF: The sum of counts for recorded events should equal the
                    overall count, which is a long value. Unfortunately the EventDetail
                    used by recordEvent has a public int _count. It is too scary to try to
                    change a public member that could be accessed in either upstream,
                    downstream, or platform, and the actual counts I am most worried about
                    are 0 and 1, so I just cast the long count to an int count, should
                    work fine for all normal cases.
                */
                recordEvent(type, (int)additionalCount);
            }
        }

        public void addCount (long additionalCount, long count2Inc)
        {
            addCount(additionalCount, count2Inc, null);
        }

        public void addCount (long additionalCount)
        {
            addCount(additionalCount, 0);
        }

        /**
            Get the current count.
        */
        public long getCount ()
        {
            return count;
        }

        public String countString ()
        {
            if (count2 == 0) {
                return Long.toString(count);
            }
            FastStringBuffer fsb = new FastStringBuffer();
            fsb.append(Long.toString(count));
            fsb.append(" (");
            fsb.append(Long.toString(count2));
            fsb.append(')');
            return fsb.toString();
        }

        /**
            The string used as a key in the hashtable for the data on this
            object.
            @see PerformanceStateCounter.Instance#getData
        */
        public static final String ColumnNameCount = new String("count"); // OK

        /**
            Returns a hashtable with all the data from this metric.
            Do not call this method on the static instances.
        */
        public Map getData ()
        {
            Map data = super.getData();
            data.put(ColumnNameCount, Constants.getLong(count));
            return data;
        }
    }
}
