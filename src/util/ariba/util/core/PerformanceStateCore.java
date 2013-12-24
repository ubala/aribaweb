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

    $Id: //ariba/platform/util/core/ariba/util/core/PerformanceStateCore.java#13 $
*/

package ariba.util.core;

import ariba.util.core.PerformanceState.Stats;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
    This class is the root for performance counters. Using them is as simple as can possibly be.
    <p>
    For example if you wanted to track how many cluster reconstitutes
    there were:

    <pre>
    private static final ClusterRootReconstitutes =
        new PerformanceStateCounter("Cluster Root Reconstitutes");
    </pre>
    <p>
    ...
    <p>
    <pre>
        ClusterRootReconstitutes.addCount();
    </pre>
    <p>

    You can (and should) declare the counter as a static object. But
    whenever it is used, the statistics are recorded and saved on a
    per thread basis.

    <p>

    The implementation of perf state objects is split into two peices: the static "handle"
    used by clients, and an inner class (called Instance) that actually stores the data.
    The methods on the outer class provide a convenience for accessing common operations.
    One can always "reach through" to the underlying per-thread instance to access the
    full API.  For instance:
        ClusterRootReconstitutes.addCount();
    is a convenience for:
        ((PerformanceStateCounter.Instance)ClusterRootReconstitutes.Instance()).addCount();

    @aribaapi ariba
*/
public abstract class PerformanceStateCore
{
    //--------------------------------------------------------------------------
    // constants

    public static final int LOG_COUNT = 0x01;
    public static final int LOG_TIME = 0x02;
    public static final int LOG_EVENTS = 0x04;

    /*
        Fix 1-BYV5PF: The new Search and SearchUpdate metrics are using events as a way to
        get more information about each search, and the event categorization of counts is
        appended into the perf log. In some cases there may be many events of the same
        type in a single perf log entry, particularly for certain kinds of BgWork doing
        SearchUpdate updates. We don't want to risk the high overhead of capturing stack
        traces when we aren't using AWDebug. So we disable default global stacktrace
        capture by setting the Threshold to the maximum possible value. It can still be
        enabled in the debug UI by setting it to any desired value in AWDebugOptions.
    */

    // Global value to force capture -- set by AWDebugOptions.awl
    protected static int GlobalStackTraceCaptureThreshold = Integer.MAX_VALUE;
    protected static final int DefaultLogRank = 0;

    private static final int DefaultLogFlags = LOG_COUNT;

    /**
        Is a <code>Comparator</code> that compares <code>PerformanceStateCores</code>
        on the basis of their {@link PerformanceStateCore#getLogRank log ranks}.
        @aribaapi ariba
    */
    public static final Comparator LogRankComparator = new Comparator ()
    {
        public int compare (Object first, Object second)
        {
            PerformanceStateCore c1 = (PerformanceStateCore)first;
            PerformanceStateCore c2 = (PerformanceStateCore)second;
            return MathUtil.sgn(c1.getLogRank(), c2.getLogRank());
        }
    };

    //--------------------------------------------------------------------------
    // data members

    protected final String name;
    protected final int logRank;
    protected final int logFlags;

    //--------------------------------------------------------------------------
    // constructors

    /**
        Create a performance counter with the given name.
        If logRank > 0, then metric will be logged (and rank specifies column order)
    */
    public PerformanceStateCore (String name, int logRank, int logFlags)
    {
        Assert.that(name != null, "Perf metrics must have names");
        this.name = name.intern();        
        this.logRank = logRank;
        this.logFlags = logFlags;

        PerformanceState.registerMetric(this);
    }

    public PerformanceStateCore (String name, int logRank)
    {
        this(name, logRank, DefaultLogFlags);
    }

    public PerformanceStateCore (String name)
    {
        this(name, DefaultLogRank);
    }

    //--------------------------------------------------------------------------
    // public methods

    /**
        Returns the name for this performace state object.
        
        @aribaapi ariba
    */
    public String getName ()
    {
        return name;
    }

    /**
        Returns whether <code>this</code> has the same name as <code>that</code>.
        @aribaapi ariba
    */
    public boolean hasSameNameAs (PerformanceStateCore that)
    {
        // identity comparison safe since names are interned
        return name == that.name;
    }

    public int getLogRank ()
    {
        return logRank;
    }

    public void appendCSVData (FormatBuffer buf, PerformanceState.Stats stats)
    {
        PerformanceStateCore.Instance stat
                 = (PerformanceStateCore.Instance)stats.get(name);
        if ((logFlags & LOG_COUNT) != 0) {
            if (stat != null) {
                long c = (stat != null) ? stat.getCount() : 0;
                buf.append(c);
            }
            buf.append(",");
        }

        /*
            Fix 1-BYV5PF: Add flag to append a column of Events information, a colon
            separated list of name=value pairs, where the name is an event type and the
            value is the count for that type. This information could be collected already,
            but it could not be output into the perflog before, it was only viewable via
            the AWDebug widget. This should only be used with great moderation, since it
            could add a lot of bulk to the perflogs if abused.
        */
        if ((logFlags & LOG_EVENTS) != 0) {
            if (stat != null) {
                boolean needsSeparator = false;

                Iterator<String> iter = stat.getEventTypeIterator();
                while (iter.hasNext()) {
                    String eventType = iter.next();

                    if (needsSeparator) {
                        buf.append(":");
                    }
                    buf.append(eventType);
                    buf.append("=");
                    buf.append(stat.getCount(eventType));
                    needsSeparator = true;
                }
            }
            buf.append(",");
        }

        if ((logFlags & LOG_TIME) != 0) {
            if (stat != null) {
                long t = (stat != null) ? stat.getElapsedTime() / 1000 : 0;
                buf.append(t);
            }
            buf.append(",");
        }
    }

    public void appendCSVHeaders (FormatBuffer buf)
    {
        if ((logFlags & LOG_COUNT) != 0) {
            buf.append(name);
            buf.append(",");
        }

        if ((logFlags & LOG_EVENTS) != 0) {
            buf.append(name);
            buf.append("Events");
            buf.append(",");
        }

        if ((logFlags & LOG_TIME) != 0) {
            buf.append(name);
            buf.append("Millis");
            buf.append(",");
        }
    }

    /**
        Factory method to use when a thread local version of this type
        is needed. A clone could be used here instead, but clone is a
        slow method for the VMs.
    */
    protected abstract Instance newInstance (String name);


    /**
        Not a static method, but it returns the object to be used as a
        "this" on operations much the way static methods are used to
        return a singelton.

        @aribaapi ariba
    */
    public Instance instance ()
    {
        if (!PerformanceState.threadStateEnabled()) {
            return null;
        }
        Instance obj = (Instance)PerformanceState.getThisThreadHashtable().get(name);
        if (obj != null) {
            return obj;
        }
            // if there is no object already on the state, create a
            // new one of the type of "this" and save it with the
            // proper name. Do not use "this" itself because this
            // method will be called on a public static final template
            // that should not be shared for actual data.
        obj = this.newInstance(name);
        PerformanceState.getThisThreadHashtable().put(name, obj);
        return obj;
    }

    /**
       The number of events with a given signature before we capture a backtrace
       in the EventDetail
     */
    public static int getGlobalStackTraceCaptureThreshold ()
    {
        return GlobalStackTraceCaptureThreshold;
    }

    public static void setGlobalStackTraceCaptureThreshold (int count)
    {
        GlobalStackTraceCaptureThreshold = count;
    }
    
    /**
       the real data bearing instance
       @aribaapi ariba
     */ 
    public static class Instance
    {        
        private final String _name;
        protected Map _statsByType;
        
        protected Instance (String name)
        {
            _name = name;
        }

        public long getCount ()
        {
            return 0;
        }
        
        /**
            Returns Iterator over String event types, in the order they were added to this
            instance. LinkedHashMap maintains insertion order. Never returns null.
        */
        public Iterator<String> getEventTypeIterator ()
        {
            return (_statsByType != null ? _statsByType.keySet().iterator() :
                    Collections.EMPTY_LIST.iterator());
        }

        public final long getCount (String type)
        {
            if (type == null) {
                return getCount();
            }
            EventDetail ev = getEventDetailForType(type);
            return (ev != null ? ev._count : 0L);
        }

        public long getElapsedTime ()
        {
            return 0;
        }

        public String elapsedTimeString ()
        {
            return "";
        }

        public String countString ()
        {
            return "-";
        }

        public static final String ColumnEventDetail = "EventDetail";

        /**
            Returns a hashtable with all the data from this metric.

            Do not call this method on the static instances.
        */
        public Map getData ()
        {
            IdentityHashMap data = new IdentityHashMap(3);
            if (_statsByType != null) {
                data.put(ColumnEventDetail, _statsByType);
            }
            return data;
        }

        public String toString ()
        {
            return String.valueOf(getData());
        }

        /**
            If type is null do nothing, this is the most common case. If type is non-null,
            get or create its EventDetail record in the map, and increment its count by 1.
            If type is non-null, and the count for this type has exactly reached the
            GlobalStackTraceCaptureThreshold, then it generates a stacktrace and a debug
            string, and puts them int he EventDetail record.
        */
        public void recordEvent (String type)
        {
            recordEvent(type, 1);
        }

        /**
            If type is null do nothing, this is the most common case. If type is non-null,
            get or create its EventDetail record in the map, and increment its count by
            the given increment, usually 1.  If type is non-null, and the count for this
            type has exactly reached the GlobalStackTraceCaptureThreshold, then it
            generates a stacktrace and a debug string, and puts them int he EventDetail
            record.
        */
        public void recordEvent (String type, int increment)
        {
            if (type == null) {
                return;
            }
            EventDetail ev = getEventDetailForType(type);
            ev._count += increment;

            // Record stack trace if we've hit a threshhold
            long threshold = Math.min((long)GlobalStackTraceCaptureThreshold,
                                      ev._stackTraceThreshold);
            if (ev._count == threshold) {
                ev._stackTrace = SystemUtil.stackTrace();
                ev._debugState = ThreadDebugState.makeString();
            }
        }

        public List getEventList ()
        {
            return (_statsByType == null) ? null : MapUtil.elementsList(_statsByType);
        }
        

        private EventDetail getEventDetailForType (String type)
        {
            type = EventDetail.getType(type);
            /*
                Fix 1-BYV5PF: Use a LinkedHashMap so the keySet iterator will return the
                keys in insertion order, which is a stable and informative way to organize
                them in the perflog file contents.
            */
            if (_statsByType == null) {
                _statsByType = new LinkedHashMap();
            }

            EventDetail ev = (EventDetail)_statsByType.get(type);
            if (ev == null) {
                // create a new EventDetail if one does not exist
                Stats stats = PerformanceState.getThisThreadHashtable();
                PerformanceCheck check = stats._performanceCheck;
                long[] levels = check == null ? 
                    null : check.getThresholds(_name, type);
                long stackTraceThreshold = levels == null ? 0 : levels[1];
                ev = new EventDetail(type, stackTraceThreshold);
                _statsByType.put(type, ev);
            }
            
            return ev;            
        }
    }

    /**
     * @aribaapi ariba
     */
    public static class EventDetail
    {
        public static String getType (String type)
        {
            if (type.length() > 60) { 
                type = type.substring(0,59);
            }
            return type;
        }
        
        public final String _type;
        public int _count;
        public String _debugState;
        public String _stackTrace;
        public final long _stackTraceThreshold;

        public EventDetail (String type, long stackTraceThreshold)
        {
            _type = type;
            _stackTraceThreshold = stackTraceThreshold;
        }

        // we return only our count because we're logged as part of a hashtable that
        // already includes our key...
        public String toString ()
        {
            return Integer.toString(_count);
        }
    }
    
    /**
     * @aribaapi ariba
     */
    public interface MetricObserver
    {
        public void clear ();
        public void unregister ();
    }
}
