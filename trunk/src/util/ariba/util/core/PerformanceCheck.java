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

    $Id: //ariba/platform/util/core/ariba/util/core/PerformanceCheck.java#9 $
*/

package ariba.util.core;

import java.util.Iterator;
import java.util.Map;

/**
    <p>PerformanceCheck defines a set of expected performance boundaries for task
    execution.  The PerformanceCheck is initialed with a list of PerformanceChecker
    objects associated with a PerformanceState metric (e.g. Metric_AQLQueries, 
    Metric_PageGeneration, ...). The warning and the error thresholds used by the check
    are defined in the Parameters.table.</p>
    
    <p>The expected pattern is that a PerformanceCheck instance is initialized (On startup)
    and remains immutable there after.  It is then assigned to threads as requests are
    processed, via
    <pre>    PerformanceState.watchPerformance(Checker)</pre></p>

    <p>For an example of use, see AWDispatcherServlet and
    BaseUIApplication.defaultPerformanceCheck()</p>

    <p>The applied PerformanceCheck is currently applied in two ways:
        1) In production, the errorRuntimeMillis is monitored by the
           PerformanceState.WatcherDaemon to trigger error logging of run away thread.

        2) At dev time, the AWDebugPane displays warning and error metrics as the
           developer navigates page to page in the app -- to flag problemmatic areas.
    
     
    @aribaapi ariba
*/
public class PerformanceCheck
{
    public static final int SeverityNone = 0;
    public static final int SeverityWarning = 1;
    public static final int SeverityError = 2;

    static final String WarningKey = "Warning";
    static final String ErrorKey   = "Error";

    private static final String ParameterPrefix = "System.Debug.PerformanceThresholds";
    private static final String TypeKey         = "Types";
    private static final String TimerKey        = "Timer";
    //default not stable
    private static final String StableKey        = "Stable";    
    
    private static final Map/*<String, PerformanceCheck>*/ Registry = MapUtil.map(1);
    
    public static final PerformanceCheck getPerformanceCheck (String name, 
                                                              Parameters params)
    {
        PerformanceCheck check = (PerformanceCheck)Registry.get(name);
        if (check == null) {
            check = new PerformanceCheck(name, params);
            // we don't need to synchronize. Worst case we create two instances
            // of the same PerformanceCheck and the last one will be the one stored
            Registry.put(name, check);
        }
        return check;
    }

    protected long _warningRuntimeMillis;
    protected long _errorRuntimeMillis;
    private final Map/*<String, PerformanceChecker>*/ _checks;    
    
    public PerformanceCheck (long warningRuntimeMillis, 
                             long errorRuntimeMillis,
                             Parameters params)
    {
        _warningRuntimeMillis = warningRuntimeMillis;
        _errorRuntimeMillis = errorRuntimeMillis;
        _checks = MapUtil.map(1);
        addChecker(new PerformanceChecker(
            PerformanceState.DispatchTimer.getName(),
            true,
            warningRuntimeMillis,
            errorRuntimeMillis,
            null));
    }
    
    private PerformanceCheck (String name, Parameters params)
    {
        String sectionParam = StringUtil.strcat(ParameterPrefix + ".", name);
        Map section = params.getMapParameter(sectionParam);
        _checks = MapUtil.map(section.size());
        Iterator checkNames = section.keySet().iterator();
        while (checkNames.hasNext()) {
            String metricName = (String)checkNames.next();
            long errorLevel = params.getLongParameter(
                Fmt.S("%s.%s.%s", sectionParam, metricName, ErrorKey));
            long warningLevel = params.getLongParameter(
                Fmt.S("%s.%s.%s", sectionParam, metricName, WarningKey));
            boolean isStable = params.getBooleanParameter(
                Fmt.S("%s.%s.%s", sectionParam, metricName, StableKey));
            boolean useTimer = params.getBooleanParameter(
                Fmt.S("%s.%s.%s", sectionParam, metricName, TimerKey));
            Map typeLevels = params.getMapParameter(
                Fmt.S("%s.%s.%s", sectionParam, metricName, TypeKey));
            PerformanceChecker check = new PerformanceChecker(
                metricName,
                isStable,
                useTimer,
                errorLevel,
                warningLevel,
                typeLevels);
            addChecker(check);
            
            // set our runtime level
            if (metricName.equals("Runtime")) {
                _warningRuntimeMillis = warningLevel;
                _errorRuntimeMillis = errorLevel;
            }
        }
    }
    
    public void addChecker (PerformanceChecker check)
    {
        _checks.put(check.getMetricName(), check);
    }

    public Map/*<String, PerformanceChecker>*/  getCheckers () {
        return _checks;
    }
    
    public int checkAndRecord (PerformanceState.Stats stats, ErrorSink sink)
    {
        int severity = SeverityNone;

        // Check the count metrics
        for (Iterator i = _checks.values().iterator(); i.hasNext(); ) {
            PerformanceChecker checker = (PerformanceChecker)i.next();
            int thisSeverity = checker.check(stats, sink);
            if (thisSeverity > severity) {
                severity = thisSeverity;
            }
        }

        return severity;
    }
    
    /**
     * Returns a couple of long representing the warning and the error levels
     * for the given metric and subtype.
     * If subtype is null or if no subtype levels have been defined, 
     * the global levels will be returned.
     * If there's no check defined for that metric, null will be returned.
     * @param metricName the metric for which to return the thresholds
     * @param type the subtype
     * @return a couple of long with the warning and the error levels
     * @aribaapi private
     */
    long[] getThresholds (String metricName, String type)
    {
        PerformanceChecker check = (PerformanceChecker)_checks.get(metricName);
        if (check == null) {
            return null;
        }
        return check.getThresholds(type);
    }
    
    /**
        Callback interface for to record warnings/erros detected.
        @aribaapi ariba
    */
    public interface ErrorSink
    {
        public void recordError (PerformanceState.Stats stats, PerformanceChecker check, int severity,
                                 String message);
    }
}
