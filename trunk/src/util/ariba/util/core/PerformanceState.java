/*
    Copyright (c) 1996-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/PerformanceState.java#54 $
*/

package ariba.util.core;

import ariba.util.core.PerformanceStateCore.MetricObserver;
import ariba.util.log.Log;
import ariba.util.log.LogManager;
import ariba.util.formatter.StringFormatter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 This class maintains performance state for the current thread.

 It can be considered a specialized extension to ThreadDebugState (and, in fact,
 stores itself there).

 @aribaapi ariba
 */
public class PerformanceState
{
    protected static List _RegisteredMetrics = ListUtil.list();
    protected static String NodeName = "UnknownNode";
    protected static int communityId = -1;
    private static boolean _RegistrationComplete = false;
    private static boolean allowSuspending = true;

    // perfTestMode is a flag to indicate that the currently running instance is
    // running performance related tests.  This flag is intended to be used to determine
    // if the gathering of additional diagnostic information should be turned off.  It is *not* intended
    // to affect functionality in the application.
    // The default value is false, performance testing installations should set this
    // value to true.
    private static boolean perfTestMode = false;

    private static final String PlanPrefix = "plan-";

    private static PerfLogger perfLog = new PerfLogger(null);
    private static PerfLogger planLog = new PerfLogger(PlanPrefix);

    public static PerformanceStateTimedCounter DispatchTimer =
            new PerformanceStateTimedCounter("Runtime", 1000,
                    PerformanceStateCore.LOG_TIME);

    /*
        Fixed 1-BYV5PF: Added Search counter/timer for search actions, and SearchRows
        counter for rows returned. Added SearchUpdate counter/timer for updates to searchable
        items, and SearchUpdateBytes for the number of bytes in the updates. The counters for
        Search and SearchUpdate also append perflog columns with event type categorization of
        the counts.
 
        Dave Finlay asked (1) that we add any new metrics in platform, to prevent further
        divergence between upstream and downstream perf csv column headers, (2) that we
        make them generic and reusable for different apps, and (3) that we rank new
        metrics after the previous final column (RuntimeMillis in this case), so existing
        pig scripts will not be broken by changing the column index of any existing
        metric. Better we should come after RuntimeMillis than to break any piggies!
    */

    /**
        This perflog metric is for "user searches", both those done by the user in an
        explicit "search UI", and also searches done on the user's behalf in portlets and
        other contexts. It appends three columns to the perf csv file: (1) "Search" for
        the search count, (2) "SearchMillis" for the search elapsed time, and 
        (3) "SearchEvents" for a categorization of the counts by event name. The stop
        method takes an event name, and can be used to subdivide the searches by event
        type, to provide more context about what is going on. In upstream, it is used for
        project searches, document searches, MyTasks searches, portlet task searches,
        supplier/SPQ searches, and message board / notes searches.
    */
    public static PerformanceStateTimedCounter Search =
        new PerformanceStateTimedCounter("Search", 1001,
                                         (PerformanceStateCore.LOG_COUNT |
                                          PerformanceStateCore.LOG_TIME |
                                          PerformanceStateCore.LOG_EVENTS));

    /**
        This perflog metric is for "user searches", and provides additional information
        for the "Search" metric. It appends one column to the perf csv file: "SearchRows"
        for the number of rows returned by the searches counted by the Search metric. In
        some contexts the row count is not available to set into this metric.
    */
    public static PerformanceStateCounter SearchRows =
        new PerformanceStateCounter("SearchRows", 1002);

    /**
        This perflog metric is for updates to the searchable source data for search
        items. This is measuring changes to the searchable source data, which is usually a
        generated representation of transactional data for a searchable item. It appends three
        columns to the perf csv file: (1) "SearchUpdate" for the count of updates to
        searchable items, (2) "SearchUpdateMillis" for the elapsed time while updating
        searchable items, and (3) "SearchUpdateEvents" for a categorization of the counts by
        event name. The stop method takes an event name, and can be used to subdivide the
        search updates by event type, to provide more context about what is going on. In
        upstream, it is used for updates to AttrsBlob, FileBlob, and NotesBlob, and
        applies both before and after adoption of Arches for upstream search.
    */
    public static PerformanceStateTimedCounter SearchUpdate =
        new PerformanceStateTimedCounter("SearchUpdate", 1003,
                                         (PerformanceStateCore.LOG_COUNT |
                                          PerformanceStateCore.LOG_TIME |
                                          PerformanceStateCore.LOG_EVENTS));

    /**
        This perflog metric is for updates to the searchable source data for search items,
        and provides additional information for the "SearchUpdate" metric. It appends one
        column to the perf csv file: "SearchUpdateBytes" for the number of bytes of
        searchable source data generated by the search updates. In some contexts the byte
        count may not available to set into this metric.
    */
    public static PerformanceStateCounter SearchUpdateBytes =
        new PerformanceStateCounter("SearchUpdateBytes", 1004);

    public static PerformanceStateCPUTimedCounter ThreadCPUTimer =
            new PerformanceStateCPUTimedCounter();

    private static final String ParameterLogArchivingDisabled
            = "System.Performance.ScheduledLogArchivingDisabled";

    public static final String ParameterAllowRecordingSuspensions =
            "System.Performance.AllowRecordingSuspensions";

    public static final String ParameterEnableLoadTestingMode =
          "System.Performance.EnableLoadTesting";


    /**
     Must be called by app after all metrics have been registered.  At that point
     logging can begin (and no further registration should be performed)
     */
    public static void registrationComplete ()
    {
        _RegistrationComplete = true;
    }

    public static final boolean threadStateEnabled ()
    {
        return ThreadDebugState.threadStateEnabled;
    }

    /**
     * Returns true if the current thread is running a qual test.  The intent of
     * this method is to return true when additional diagnostic information should be
     * captured.  There are 3 components to this determination:
     * 1) threadStateEnabled()
     * 2) qualMode - this is set at server startup indicating that thread state is
     * enabled and it is possible that a qual (LQ/BQ) test could be running.
     * (opposed to a performance test).
     * 3) The testId property is non-null on the current thread.
     *
     * @return
     */
    public static final boolean isQualTest ()
    {
        return threadStateEnabled() && !perfTestMode &&
              getThisThreadHashtable().getTestId() != null;
    }

    public static boolean isLoggingEnabled ()
    {
        return Log.perf_log_trace.isDebugEnabled();
    }

    /**
     Remove all values that are currently stored.
     @deprecated  Use ThreadDebugState.clear()
     */
    public static void clear ()
    {
        ThreadDebugState.clear();
    }

    /**
     Set a thread to have its execution time monitored.  If clear() has
     not been called for this thread before the <code>errorDuration</code>
     has passed, then the thread debug state will be written to the error log.

     @param checker -- the performance check object.  It's warningTimeMillis determines
     the logging period
     */
    public static void watchPerformance (PerformanceCheck checker)
    {
        if (threadStateEnabled()) {
            Stats stats = getThisThreadHashtable();

            stats._startTime = System.currentTimeMillis();
            stats._performanceCheck = checker;
            long duration = (checker._errorRuntimeMillis > 0 ?
                    checker._errorRuntimeMillis : 30000);
            stats._deadline = stats._startTime + duration;
            WatcherDaemon.add(ThreadDebugState.getThisThreadHashtable(),
                    Thread.currentThread());
        }
    }

    private static final long DefaultErrorRuntimeMillis = -1;

    public static void restoreDefaultErrorRuntimeMillis ()
    {
        updateErrorRuntimeMillis(DefaultErrorRuntimeMillis);
    }

    public static void updateErrorRuntimeMillis (long duration)
    {
        if (threadStateEnabled()) {
            Stats stats = getThisThreadHashtable();
            if (duration == DefaultErrorRuntimeMillis) {
                duration = stats._performanceCheck._errorRuntimeMillis;
            }
            stats._deadline = stats._startTime + duration;
            WatcherDaemon.update(ThreadDebugState.getThisThreadHashtable(),
                    Thread.currentThread());
        }
    }

    public static void setPerformanceCheck (PerformanceCheck check)
    {
        if (threadStateEnabled()) {
            Stats stats = getThisThreadHashtable();
            stats._performanceCheck = check;
        }
    }

    public static void addObserver (MetricObserver observer)
    {
        if (threadStateEnabled()) {
            Stats stats = getThisThreadHashtable();
            /*
              1-4TEWQL Synchronize to avoid ConcurrentModificationException on observer
              list.  Normally a thread local value like Stats should not need
              synchronization.  Howerver AWSession.ensureAwake use of
              PerformanceState.restoreContinuedHashtable() moves a Stats instance from
              one request to another which opens a small window for two request threads
              to be using the same Stats instance.  Also see synchronized in
              notifyObservers and unregisterObservers.
             */
            synchronized(stats) {
                stats.addObserver(observer);
            }
        }
    }


    /**
     Called by ThreadDebugState.clear();
     */
    protected static void internalClear (ThreadDebugState.StateMap debugState,
                                         boolean endOfEvent)
    {
        if (!threadStateEnabled()) {
            return;
        }

        Stats stats = (Stats)state.get();
        if (stats != null) {
            if (_RegistrationComplete && isLoggingEnabled() && endOfEvent
                    &&!stats.isEmpty() && !stats.isToBeContinued()) {
                logToFile(stats);
                logPerfExceptions(stats);
                notifyObservers(stats);
            }

            // if we were monitoring this thread, remove it from the watch list
            if (stats._startTime != 0) {
                WatcherDaemon.remove(debugState);
                stats._startTime = 0;
                stats._deadline = 0;
            }

            unregisterObservers(stats);
            setThisThreadHashtable(null);
        }
    }

    protected static void registerMetric (PerformanceStateCore metric)
    {
        synchronized (_RegisteredMetrics) {
            // ensure no dup names
            int i = _RegisteredMetrics.size();
            while (i-- > 0) {
                PerformanceStateCore existing =
                        (PerformanceStateCore)_RegisteredMetrics.get(i);
                Assert.that(!metric.hasSameNameAs(existing),
                        "Registering metric with duplicate name: '%s'",
                        metric.getName());
            }

            Assert.that((metric.getLogRank() == 0) || !_RegistrationComplete,
                    "Registered metric after log started ! You must register during init");
            _RegisteredMetrics.add(metric);
        }
    }

    /**

     @aribaapi ariba
     */
    public String toString ()
    {
        return String.valueOf(getThisThreadHashtable());
    }

    private static void notifyObservers (Stats stats)
    {
        synchronized (stats) {
            List/*<MetricObserver>*/ observers = stats.getObservers();
            if (ListUtil.nullOrEmptyList(observers)) {
                return;
            }
            for (Iterator i = observers.iterator(); i.hasNext(); ) {
                MetricObserver observer = (MetricObserver)i.next();
                if (observer != null) {
                    observer.clear();
                }
            }
        }
    }

    private static void unregisterObservers (Stats stats)
    {
        synchronized (stats) {
            List/*<MetricObserver>*/ observers = stats.getObservers();
            if (ListUtil.nullOrEmptyList(observers)) {
                return;
            }
            for (Iterator i = observers.iterator(); i.hasNext(); ) {
                MetricObserver observer = (MetricObserver)i.next();
                if (observer != null) {
                    observer.unregister();
                }
            }
        }
    }

    private static final State state = StateFactory.createState();

    /**
     If you need direct access to the hashtable, you can get it
     this way. If you intend to save the object for later use (past
     the bounds of the current ui boundary, you should call clone
     on the data so that when this object is cleared, your shared
     copy of the hashtable is not also cleared.

     @aribaapi ariba
     */
    public static Stats getThisThreadHashtable ()
    {
        if (!threadStateEnabled()) {
            return null;
        }
        Stats htable = (Stats)state.get();
        if (htable == null) {
            htable = new Stats();
            setThisThreadHashtable(htable);
        }
        return htable;
    }

    protected static void setThisThreadHashtable (Stats stats)
    {
        state.set(stats);
        ThreadDebugState.getThisThreadHashtable()._performanceState = stats;
    }

    public static void restoreContinuedHashtable (Stats stats)
    {
        if (threadStateEnabled()) {
            // remove old state
            internalClear(ThreadDebugState.getThisThreadHashtable(), false);

            // set new stats
            setThisThreadHashtable(stats);

            // watch performance, if this had a watcher on it
            if (stats._performanceCheck != null) {
                watchPerformance(stats._performanceCheck);
            }
        }
    }

    public static void setStatus (String status)
    {
        if (threadStateEnabled()) {
            getThisThreadHashtable().setStatus(status);
        }
    }

    public static boolean isRecordingSuspended ()
    {
        if (threadStateEnabled()) {
            return (getThisThreadHashtable().isRecordingSuspended() &&
                    isSuspendingAllowed());
        }
        return false;
    }

    /**
     * Performance recording should not be suspended if this method returns true.
     * this is done during performance testing so the perf logs more accurately
     * reflect the true load on the system.
     *
     * @return
     */
    public static boolean isSuspendingAllowed ()
    {
        return allowSuspending;
    }

    public static void setAllowRecordingSuspensions (boolean flag)
    {
        allowSuspending = flag;
    }

    /**
     * perfTestMode is a flag to indicate that the currently running instance is
     * running performance related tests.  This flag is intended to be used to determine
     * if the gathering of additional diagnostic information should be turned off.  It is *not* intended
     * to affect functionality in the application.
     * The default value is false, performance testing installations should set this
     * value to true.
     *
     * @param flag
     */
    public static void setPerfTestMode (boolean flag)
    {
        perfTestMode = flag;
    }


    /* Type of activity -- User request, Background task, ... */
    public static final String Type_User = "User";
    public static final String Type_Task = "Task";
    public static final String Type_Work = "Work";
    public static final String Type_Call = "Call";

    /* Result of request */
    public static final String Status_Success = "Success";
    public static final String Status_InternalError = "InternalError";
    public static final String Status_ValidationError = "Validation";
    public static final String Status_Timeout = "Timeout";
    public static final String Status_StoppedWaiting = "StoppedWaiting";
    public static final String Status_Refresh = "Refresh";
    public static final String Status_Cancel = "Cancelled";
    /* Other potential codes:
        Disappointment
        Departed flow
        AutoLogout
        AutoLogoutCanceled
        Help
    */


    /**
     @aribaapi ariba
     */
    public static class Stats extends EqHashtable
    {
        PerformanceCheck _performanceCheck;
        private List observers;

        protected long _startTime;
        protected long _deadline;
        protected boolean toBeContinued;
        protected boolean recordingSuspended;

        protected String realm;
        protected String realmType;
        protected String sessionID;
        protected String ipAddress;
        protected String user;
        protected String sourcePage;
        protected String sourceArea;
        protected String destinationPage;
        protected String destinationArea;
        protected String type;
        protected String status;

        protected PerformanceState appMetric;
        protected String appInfo;
        protected String appDimension1;
        protected String appDimension2;
        protected String referer;
        protected String acceptLanguage;
        protected String userAgent;
        protected String screenSize;
        protected String tabIndex;

        protected String testShortId;
        protected String testId;
        protected String testLine;
        
        protected String shutdownMode;

        public PerformanceCheck getPerformanceCheck ()
        {
            return _performanceCheck;
        }

        void addObserver (MetricObserver observer)
        {
            if (observers == null) {
                observers = ListUtil.list(1);
            }
            observers.add(observer);
        }

        List/*<MetricObserver>*/ getObservers ()
        {
            return observers;
        }

        /**
         Indicates that this performance state will be continued in a subsequent
         request (and so should not be logged on clear().  This is used by AWSession
         when it bridges a single perf measure across the two-request
         form-post-redirect sequence.
         */
        public boolean isToBeContinued ()
        {
            return toBeContinued;
        }

        public void setToBeContinued (boolean toBeContinued)
        {
            this.toBeContinued = toBeContinued;
        }

        /**
            Indicates whether this performance state has suspended recording changes to stats.
        */
        public boolean isRecordingSuspended ()
        {
            return recordingSuspended;
        }

        /**
            Allows suspension of recording stats. Care should be taken to ensure that
            any suspension is revoked by encapsulating the call in a try..finally block:

            <code>
            PerformanceState.Stats stats = PerformanceState.getThisThreadHashtable();
            try
            {
               stats.setRecordingSuspended(true);
               ...
            }
            finally {
               stats.setRecordingSuspended(false);
            }
            </code>
        */
        public void setRecordingSuspended (boolean recordingSuspended)
        {
            this.recordingSuspended = recordingSuspended;
        }

        public String getRealm ()
        {
            return realm;
        }

        public void setRealm (String realm)
        {
            this.realm = realm;
        }

        public String getSessionID ()
        {
            return sessionID;
        }

        public String getRealmType ()
        {
            return realmType;
        }

        public void setRealmType (String realmType)
        {
            this.realmType = realmType;
        }

        public void setSessionID (String sessionID)
        {
            this.sessionID = sessionID;
        }

        public String getIPAddress ()
        {
            return ipAddress;
        }

        public void setIPAddress (String ipAddress)
        {
            this.ipAddress = ipAddress;
        }

        public String getUser ()
        {
            return user;
        }

        public void setUser (String user)
        {
            this.user = user;
        }

        /**
         For AW this is the AW page (class name) that processed the incoming request.
         Could re-appropriated for scheduled tasks to be the scheduled task name
         */
        public String getSourcePage ()
        {
            return sourcePage;
        }

        public void setSourcePage (String sourcePage)
        {
            this.sourcePage = sourcePage;
        }

        /**
         Sub-area within the source page.  Could be the Tab name or Wizard step
         */
        public String getSourceArea ()
        {
            return sourceArea;
        }

        public void setSourceArea (String sourceArea)
        {
            this.sourceArea = sourceArea;
        }

        /**
         The page returned from the request
         */
        public String getDestinationPage ()
        {
            return destinationPage;
        }

        public void setDestinationPage (String destinationPage)
        {
            this.destinationPage = destinationPage;
        }

        public String getDestinationArea ()
        {
            return destinationArea;
        }

        public void setDestinationArea (String destinationArea)
        {
            this.destinationArea = destinationArea;
        }

        /**
         Returns String suitable to display in perf or other logging, encoding context
         information in (source, destination) x (Page, Area), while avoiding boring
         repetition.
         @aribaapi private
         */
        public String getDisplayPageArea ()
        {
            String sp = getSourcePage();
            String sa = getSourceArea();
            String dp = getDestinationPage();
            String da = getDestinationArea();

            if (dp == null || dp.equals(sp)) {
                dp = sp;
                sp = null;
                if (da == null) {
                    da = sa;
                }
                sa = null;
            }
            if (dp != null) {
                StringBuilder buf = new StringBuilder();
                appendPageAndArea(buf, dp, da);
                if (sp != null) {
                    buf.append(" via ");
                    appendPageAndArea(buf, sp, sa);
                }
                return buf.toString();
            }
            return "";
        }

        private void appendPageAndArea (StringBuilder buf, String page, String area)
        {
            buf.append(page);
            if (area != null) {
                buf.append(':');
                buf.append(area);
            }
        }

        /**
        Returns the index of the current page regarding the multitab feature
        0, 1, 2, ... up to TabManager.maxCookies.
        */
        public String getTabIndex ()
        {
            return tabIndex;
        }

        public void setTabIndex (String tabIndex)
        {
            this.tabIndex = tabIndex;
        }

        /**
         Type_User, Type_Task, etc...
         */
        public String getType ()
        {
            return type;
        }

        public void setType (String type)
        {
            this.type = type;
        }

        /**
         Status_Success, Status_InternalError, etc...
         */
        public String getStatus ()
        {
            return (status != null) ? status : Status_Success;
        }

        public void setStatus (String status)
        {
            this.status = status;
        }

        /**
         An additional metric that should be logged in the CSV.

         E.g.:
         PerformanceState.getThisThreadHashtable().setAppMetric(NumberOfSourcingEvents)

         This metric name and value will be logged in the CSV metrics file.
         */
        public PerformanceState getAppMetric ()
        {
            return appMetric;
        }

        public void setAppMetric (PerformanceState appMetric)
        {
            this.appMetric = appMetric;
        }

        /**
         Arbitrary descriptive information that will be written to the CSV.
         Example usage: Name of the Report being opened, ...
         */
        public String getAppInfo ()
        {
            return appInfo;
        }

        public void setAppInfo (String appInfo)
        {
            this.appInfo = appInfo;
        }

        /**
         AppDimension (1 & 2) can be used for storing application-specific
         categorization information that can we used to slice/group information
         during analysis.  For instance, AQS might put RFXType, and RFXName here.
         Analysis might put Fact Table name, and UsesSlowDimensions flag.
         */
        public String getAppDimension1 ()
        {
            return appDimension1;
        }

        public void setAppDimension1 (String appDimension1)
        {
            this.appDimension1 = appDimension1;
        }

        public String getAppDimension2 ()
        {
            return appDimension2;
        }

        public void setAppDimension2 (String appDimension2)
        {
            this.appDimension2 = appDimension2;
        }

        public String getReferer ()
        {
            return referer;
        }

        public void setReferer (String url)
        {
            this.referer = url;
        }

        public String getAcceptLanguage ()
        {
            return acceptLanguage;
        }

        public void setAcceptLanguage (String languages)
        {
            this.acceptLanguage = languages;
        }

        public String getUserAgent ()
        {
            return userAgent;
        }

        public void setUserAgent (String agent)
        {
            this.userAgent = agent;
        }

        public String getTestId ()
        {
            return testId;
        }

        public String getTestShortId ()
        {
            return testShortId;
        }

        public void setTestShortId (String testShortId)
        {
            this.testShortId = testShortId;
        }

        public void setTestId (String testId)
        {
            this.testId = testId;
        }

        public String getTestLine ()
        {
            return testLine;
        }

        public void setTestLine (String testLine)
        {
            this.testLine = testLine;
        }

        public String getShutdownMode ()
        {
            return shutdownMode;
        }

        public void setShutdownMode (String shutdownMode)
        {
            this.shutdownMode = shutdownMode;
        }

        public String getScreenSize ()
        {
            return screenSize;
        }

        public void setScreenSize (String size)
        {
            this.screenSize = size;
        }

        public String toString ()
        {
            Map m = MapUtil.map(this);
            if (realm != null) {
                m.put("Realm", realm);
            }
            if (user != null) {
                m.put("User", user);
            }
            if (sourcePage != null) {
                m.put("SourcePage", sourcePage);
            }
            if (sourceArea != null) {
                m.put("SourceArea", sourceArea);
            }
            if (destinationPage != null) {
                m.put("DestinationPage", destinationPage);
            }
            if (destinationArea != null) {
                m.put("DestinationArea", destinationArea);
            }
            if (sessionID != null) {
                m.put("sessionID", sessionID);
            }
            if (ipAddress != null) {
                m.put("ipAddress", ipAddress);
            }
            if (appInfo != null) {
                m.put("appInfo", appInfo);
            }
            if (referer != null) {
                m.put("referer", referer);
            }
            if (acceptLanguage != null) {
                m.put("acceptLanguage", acceptLanguage);
            }
            if (userAgent != null) {
                m.put("userAgent", userAgent);
            }
            if (testShortId != null) {
                m.put("testShortId", testShortId);
            }
            if (testId != null) {
                m.put("testId", testId);
            }
            if (testLine != null) {
                m.put("testLine", testLine);
            }
            if (shutdownMode != null) {
                m.put("shutdownMode", shutdownMode);
            }
            if (screenSize != null) {
                m.put("screenSize", screenSize);
            }
            if (tabIndex != null) {
                m.put("tabIndex", tabIndex);
            }
            return m.toString();
        }
    }

    /**
     Must be initialized by server prior to first log event
     */
    public static void setNodeName (String nodeName)
    {
        PerformanceState.NodeName = nodeName;
    }

    public static String getNodeName ()
    {
        return NodeName;
    }

    /**
     Must be initialized prior to first log event
     */
    public static void setCommunity (int communityId)
    {
        PerformanceState.communityId =  communityId;
    }

    public static int getCommunity ()
    {
        return communityId;
    }

    protected static final String FileHeader = "Date, Realm, RealmType, Community, "
            + "NodeName, SessionID, User, SourcePage, SourceArea, DestPage, DestArea, "
            + "Type, Status, "
            + "AppMetricName, AppMetric, AppDimension1, AppDimension2, "
            + "AppInfo, Referer, AcceptLanguages, UserAgent, TestShortId, "
            + "TestId, TestLine, ShutdownMode, ScreenSize,";
    /***************************************************************************/
    // Any new column label should be added at the very end of TrailingFileHeader
    /***************************************************************************/
    protected static final String TrailingFileHeader = "TabIndex,";
    protected static final String PlanFileHeader = "Date, Realm, RealmType, Community, "
            + "NodeName, SessionID, User, SourcePage, SourceArea, DestPage, DestArea, "
            + "Type, Status, "
            + "AppDimension1, AppDimension2, AppInfo, TestShortId, TestId, "
            + "TestLine, Query, Plan";

    protected static PerformanceStateCore[] _LogMetrics = null;

    /**
     Returns an array of the {@link PerformanceStateCore} objects to be
     used in logging. <p/>

     @aribaapi ariba
     */
    public static PerformanceStateCore[] logMetrics ()
    {
        if (_LogMetrics == null) {
            synchronized (_RegisteredMetrics) {
                int size=_RegisteredMetrics.size();
                List/*<PerformanceStateCore>*/ rankedMetrics = ListUtil.list(size);
                for (int j=0; j < size; ++j) {
                    PerformanceStateCore metric =
                            (PerformanceStateCore)_RegisteredMetrics.get(j);
                    // just pick out the items marked with a logRank
                    if (metric.getLogRank() > 0) {
                        rankedMetrics.add(metric);
                    }
                }
                PerformanceStateCore[] temp =
                        new PerformanceStateCore[rankedMetrics.size()];
                rankedMetrics.toArray(temp);
                Arrays.sort(temp, PerformanceStateCore.LogRankComparator);
                _LogMetrics = temp;
            }
        }
        return _LogMetrics;
    }

    public static String fileHeaderString (String prefix)
    {
        FormatBuffer buf = new FormatBuffer(100);

        if (prefix.equals(PlanPrefix)) {
            buf.append(PlanFileHeader);
        }
        else {
            buf.append(FileHeader);

            PerformanceStateCore[] logMetrics = logMetrics();
            int len = logMetrics.length;
            for (int i = 0; i < len; i++) {
                PerformanceStateCore metric = logMetrics[i];
                metric.appendCSVHeaders(buf);
            }
            buf.append(TrailingFileHeader);
        }

        return buf.toString();
    }

    private static final char sep = ',';

    private static void csvOutput (FastStringBuffer buf, String s, boolean replaceNewline)
    {
        if (s == null) {
            return;
        }
        buf.append('"');
        String escaped = StringUtil.replaceCharByString(s, '"', "\"\"");
        if (replaceNewline) {
            escaped = escaped.replace('\n', ' ');
        }
        escaped = escaped.replaceAll(StringFormatter.getStringValue(sep), ";");
        buf.append(escaped);
        buf.append('"');
    }


    public static void logToFile (Stats stats)
    {
        // if the test line number is -1, do not log the stats
        if (!StringUtil.nullOrEmptyString(stats.getTestLine()) &&
                stats.getTestLine().equals("-1"))
        {
            return;
        }

        // create a string and add it to a queue.  Another thread will write it
        // to the file.
        FormatBuffer buf = new FormatBuffer(200);

        buf.append(Date.getNow().toString()); buf.append(sep);

        buf.append(stats.getRealm()); buf.append(sep);
        buf.append(stats.getRealmType()); buf.append(sep);
        buf.append(PerformanceState.getCommunity()); buf.append(sep);
        buf.append(PerformanceState.getNodeName()); buf.append(sep);
        buf.append(stats.getSessionID()); buf.append(":");
        buf.append(stats.getIPAddress()); buf.append(sep);
        buf.append(stats.getUser()); buf.append(sep);
        csvOutput(buf, stats.getSourcePage(), false);buf.append(sep);
        csvOutput(buf, stats.getSourceArea(), false);buf.append(sep);
        buf.append(stats.getDestinationPage());buf.append(sep);
        csvOutput(buf,stats.getDestinationArea(),false);buf.append(sep);
        buf.append(stats.getType());buf.append(sep);
        buf.append(stats.getStatus());buf.append(sep);

        if (stats.appMetric != null) {
            buf.append(stats.appMetric);
            buf.append(sep);

            PerformanceStateCore.Instance stat
                    = (PerformanceStateCore.Instance)stats.get(stats.appMetric);
            if (stat != null) {
                buf.append(stat.getCount());
            }
            buf.append(sep);
        }
        else {
            buf.append(sep);
            buf.append(sep);
        }

        csvOutput(buf,stats.getAppDimension1(),false); buf.append(sep);
        csvOutput(buf,stats.getAppDimension2(),false); buf.append(sep);
        csvOutput(buf,stats.getAppInfo(),false); buf.append(sep);

        //add HTTP referer url
        csvOutput(buf,stats.getReferer(),false); buf.append(sep);

        // add HTTP accept_language
        csvOutput(buf,stats.getAcceptLanguage(),false); buf.append(sep);

        // add HTTP user_agent
        csvOutput(buf,stats.getUserAgent(),false); buf.append(sep);

        csvOutput(buf, stats.getTestShortId(), false);
        buf.append(sep);
        csvOutput(buf, stats.getTestId(), false);
        buf.append(sep);
        csvOutput(buf, stats.getTestLine(), false);
        buf.append(sep);

        // add shutdown mode
        csvOutput(buf, stats.getShutdownMode(), false);
        buf.append(sep);

        // add Screen size
        csvOutput(buf, stats.getScreenSize(), false);
        buf.append(sep);
        
        PerformanceStateCore[] logMetrics = logMetrics();
        int len = logMetrics.length;
        for (int i = 0; i < len; i++) {
            PerformanceStateCore metric = logMetrics[i];
            metric.appendCSVData(buf, stats);
        }

        // add tab index
        csvOutput(buf, stats.getTabIndex(), false);
        buf.append(sep);

        /**************************************************************/
        // Any new column should be added at the very end of the table
        /**************************************************************/

        // Log.perf_log.debug("%s", buf);
        perfLog.sendEvent(buf.toString()); // OK
    }

    public static void logPlanFile (String query, String plan)
    {
        if (!threadStateEnabled() || !isLoggingEnabled()) {
            return;
        }

        Stats stats = (Stats)state.get();
        if (stats == null) {
            stats = new Stats();
        }

        // if the test line number is -1, do not log the stats
        if (StringUtil.nullOrEmptyString(stats.getTestLine()) ||
                stats.getTestLine().equals("-1"))
        {
            return;
        }

        // create a string and add it to a queue.  Another thread will write it
        // to the file.
        FormatBuffer buf = new FormatBuffer(200);

        buf.append(Date.getNow().toString()); buf.append(sep);

        buf.append(stats.getRealm()); buf.append(sep);
        buf.append(stats.getRealmType()); buf.append(sep);
        buf.append(PerformanceState.getCommunity()); buf.append(sep);
        buf.append(PerformanceState.getNodeName()); buf.append(sep);
        buf.append(ThreadNameAbbreviation.getName()); buf.append(":");
        buf.append(stats.getSessionID()); buf.append(":");
        buf.append(stats.getIPAddress()); buf.append(sep);
        buf.append(stats.getUser()); buf.append(sep);
        buf.append(stats.getSourcePage());buf.append(sep);
        buf.append(stats.getSourceArea());buf.append(sep);
        buf.append(stats.getDestinationPage());buf.append(sep);
        buf.append(stats.getDestinationArea());buf.append(sep);
        buf.append(stats.getType());buf.append(sep);
        buf.append(stats.getStatus());buf.append(sep);
        buf.append(stats.getAppDimension1()); buf.append(sep);
        buf.append(stats.getAppDimension2()); buf.append(sep);
        buf.append(stats.getAppInfo()); buf.append(sep);
        csvOutput(buf, stats.getTestShortId(), false);
        buf.append(sep);
        csvOutput(buf, stats.getTestId(), false);
        buf.append(sep);
        csvOutput(buf, stats.getTestLine(), false);
        buf.append(sep);
        csvOutput(buf, query, true);
        buf.append(sep);
        csvOutput(buf, plan, true);

        // Log.perf_log.debug("%s", buf);
        planLog.sendEvent(buf.toString()); // OK
    }

    public static void archiveLogFile (Parameters params)
    {
        if (params.getBooleanParameter(ParameterLogArchivingDisabled)) {
            Log.util.info(2926, "PerfLog archiving disabled: local perflog and plan files not archived");
            return;
        }
        perfLog.setArchiveFlag(true);
        planLog.setArchiveFlag(true);
    }

    /**
     If we're logging performance exceptions, then check against registered
     threshholds and log if we're "in the red"...
     */
    static void logPerfExceptions (Stats stats)
    {
        if (!Log.perf_log_exception.isDebugEnabled()) {
            return;
        }
        PerformanceCheck checker = stats.getPerformanceCheck();
        if (checker != null) {
            int level = checker.checkAndRecord(stats, null);
            if (level == PerformanceCheck.SeverityError) {
                Log.perf_log_exception.debug("Perf Trace: %s", stats);
            }
        }


    }

    static protected long deadline (ThreadDebugState.StateMap map)
    {
        Stats stats = map._performanceState;
        return (stats != null && stats._deadline != 0) ? stats._deadline : Long.MAX_VALUE;
    }

    static final class WatcherDaemon implements Runnable
    {
        protected static TreeMap _WatchedStates = null;
        private static WatcherDaemon instance;

        public static void ensureInit ()
        {
            if (instance == null) {
                // sort this heap by deadline
                _WatchedStates = new TreeMap(new Comparator() {
                    public int compare (Object o1, Object o2)
                    {
                        ThreadDebugState.StateMap s1 = (ThreadDebugState.StateMap)o1;
                        ThreadDebugState.StateMap s2 = (ThreadDebugState.StateMap)o2;
                        return (int)(deadline(s1) - deadline(s2));
                    }

                    public boolean equals (Object obj)
                    {
                        return false;
                    }
                }); // OK

                try {
                    instance = new WatcherDaemon();
                    Thread t = new Thread(instance, "Perf_Log_Exception_Daemon");
                    t.setDaemon(true);
                    t.start();
                }
                catch (java.security.AccessControlException e) {
                    // will throw in restricted environments (e.g. Google AppEngine) where thread creation
                    // is not supported.
                }
            }
        }

        static void add (ThreadDebugState.StateMap state, Thread thread)
        {
            synchronized(WatcherDaemon.class) {
                ensureInit();

                _WatchedStates.put(state, thread);
            }
        }

        static void remove (ThreadDebugState.StateMap state)
        {
            synchronized(WatcherDaemon.class) {
                _WatchedStates.remove(state);
            }
        }

        static void update (ThreadDebugState.StateMap state, Thread thread)
        {
            synchronized(WatcherDaemon.class) {
                _WatchedStates.remove(state);
                _WatchedStates.put(state, thread);
            }
        }

        /**
         Start handling the events as they come in.  This is for the
         implementation of Runnable

         @aribaapi private
         */
        public void run ()
        {
            while (true) {
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    // Ignore
                }

                // check top items on queue
                synchronized(WatcherDaemon.class) {

                    if (_WatchedStates.isEmpty()) {
                        continue;
                    }

                    long currentTime = System.currentTimeMillis();

                    ThreadDebugState.StateMap first =
                            (ThreadDebugState.StateMap)_WatchedStates.firstKey();
                    if (currentTime > deadline(first)) {
                        // we've got a performance exception!
                        logPerformanceExceptions(currentTime, _WatchedStates);
                    }
                }
            }
        }

        private void logPerformanceExceptions (long currentTime, TreeMap map)
        {
            boolean didLog = false;
            Map <Object, Object> rescheduled = null;

            Map <Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
            Iterator iter = map.keySet().iterator();
            while (iter.hasNext()) {
                ThreadDebugState.StateMap stateMap =
                        (ThreadDebugState.StateMap)iter.next();
                if (currentTime > deadline(stateMap)) {
                    Thread thread = (Thread)map.get(stateMap);
                    StackTraceElement[] stack = stackTraces.get(thread);
                    String stackString = "";
                    if (stack != null) {
                        FastStringBuffer sb = new FastStringBuffer(
                                "Current stack for long running thread: ");
                        sb.append(thread.toString());
                        for (StackTraceElement line: stack) {
                            sb.append("\n\t");
                            sb.append(line);
                        }
                        stackString = sb.toString();
                    }

                    PerformanceState.Stats stats = stateMap._performanceState;
                    Log.perf_log_exception.info(10132,
                            Integer.toString((int)((currentTime - stats._startTime) / 1000)),
                            stackString,
                            ThreadDebugState.protectedToString(stateMap));

                    didLog = true;

                    // Perform some exponential back-off: triple the interval between
                    // checks...  0, 5, 10, 30, 90, 270, ...
                    iter.remove();

                    long origDeadline = stats._startTime +
                            stats._performanceCheck._errorRuntimeMillis;
                    long oldInterval = stats._deadline - origDeadline;
                    long newInterval = (oldInterval < 10000) ? 10000 : oldInterval * 2;
                    stats._deadline += newInterval;

                    // remember this one to add after iteration complete
                    if (rescheduled == null) {
                        rescheduled = MapUtil.map();
                    }
                    rescheduled.put(stateMap, thread);
                }
                else {
                    break;
                }
            }

            // add back rescheduled items
            if (rescheduled != null) {
                for (Map.Entry <Object, Object> me : rescheduled.entrySet() ) {
                    map.put(me.getKey(), me.getValue());
                }
            }

            if (didLog) {
                // possibly force a full thread trace to the log
                // - For unix, spawn a subprocess that does a kill -QUIT on the
                //   parent process ID (i.e. the JVM).
                // - For Windows, need to spawn custom exe to our process.
                //   e.g. http://www.latenighthacking.com/projects/2003/sendSignal/

                // BREAK -- "not handled"
                // HUP, QUIT -- unknown
                // INT, TERM -- cause the VM to exit
                // sun.misc.Signal.raise(new sun.misc.Signal("SIGBREAK"));
            }
        }
    }
}

// TODO:  Use Log4J async appenders instead...
final class PerfLogger implements Runnable
{
    private ThreadedQueue _queue;
    private PrintWriter _out;

    public String NamePrefix = "perf-";
    private String _namePrefix = null;

    public final static String _suffix = "csv";
    private boolean _archiveFlag = false;

    public PerfLogger (String prefix)
    {
        if (prefix != null) {
            NamePrefix = prefix;
        }
        _queue = new ThreadedQueue();
        Thread t = new Thread(this, "Perf_Log_Trace"+NamePrefix);
        t.setDaemon(true);
        t.start();
    }

    public OutputStream createLogStream (String  prefix)
    {
        String fileName = Fmt.S("%s.%s", prefix, _suffix);
        File logFile = new File(LogManager.getDirectoryName(), fileName);
        if (logFile.exists()) {
            File archive = new File(LogManager.getArchiveDirectoryName());
            archive.mkdirs();
            if (moveTo(logFile, archive)) {
                logFile = new File(LogManager.getDirectoryName(), fileName);
            }
        }

        try {
            logFile.getParentFile().mkdirs();
            OutputStream out = IOUtil.bufferedOutputStream(logFile);
            return out;
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     Start handling the events as they come in.  This is for the
     implementation of Runnable

     @aribaapi private
     */
    public void run ()
    {
        setArchiveFlag(true);
        String pageSummary = null;
        while ((pageSummary = (String)_queue.peekNextObject()) != null) {

            // log rotation - archive the current log file
            if ( getArchiveFlag() == true) {
                startNewLogFile();
            }
            _out.println(pageSummary);
            _out.flush();
            // the code peeked at it before, now remove it.
            _queue.nextObject();
        }
    }

    private void startNewLogFile ()
    {
        if ( _out != null ) {
            _out.close();
        }
        _out = new PrintWriter(createLogStream(namePrefix()));
        _out.println(PerformanceState.fileHeaderString(NamePrefix));
        setArchiveFlag(false);
    }

    /**
     Take the event that is passed and place it in the queue.

     @aribaapi private
     */
    public void sendEvent (String event)
    {
        _queue.insertObject(event);
    }

    /**
     Flush the queue of all of the events that it has.
     */
    void flushQueues ()
    {
        _queue.waitForEmpty();
    }

    private String namePrefix ()
    {
        //get the node name from the Custom tag used by logging.
        //This will return something like "supplierdirect:Samuel_Johnson:isax7g:Node1"
        if (_namePrefix == null) {
            FastStringBuffer fsb = new FastStringBuffer();
            fsb.append(NamePrefix);
            fsb.append(PerformanceState.getNodeName());
            _namePrefix = fsb.toString();
        }
        return _namePrefix;
    }

    public boolean moveTo (File file, File targetDirectory)
    {
        Date now = new Date();
        String logFileSaveName =
                Fmt.S("%s.%s-%02s-%02s_%02s.%02s.%02s.%s",
                        ArrayUtil.array(
                                namePrefix(),
                                Constants.getInteger(Date.getYear(now)),
                                Constants.getInteger(Date.getMonth(now)+1),
                                Constants.getInteger(Date.getDayOfMonth(now)),
                                Constants.getInteger(Date.getHours(now)),
                                Constants.getInteger(Date.getMinutes(now)),
                                Constants.getInteger(Date.getSeconds(now)),
                                _suffix));
        File saveToFile =
                new File(targetDirectory, logFileSaveName);
        // renameTo does not modify the current object - hence
        // file is left unchanged
        boolean success = file.renameTo(saveToFile);
        if (!success) {
            //if renameTo failed, try copying before we give up
            success = IOUtil.copyFile(file, saveToFile);
            if (success) {
                file.delete();
            }
        }
        return success;
    }

    public void setArchiveFlag (boolean flag)
    {
        _archiveFlag = flag;
    }

    public boolean getArchiveFlag ()
    {
        return _archiveFlag;
    }
}

