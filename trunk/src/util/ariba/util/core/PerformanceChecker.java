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

    $Id: //ariba/platform/util/core/ariba/util/core/PerformanceChecker.java#3 $
*/

package ariba.util.core;

import ariba.util.core.PerformanceCheck.ErrorSink;
import ariba.util.core.PerformanceStateCore.EventDetail;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Object used by the PerformanceCheck to check a specific
 * metric (or PerformanceStateCore).
 * The thresholds used to check a metric are defined with parameters.
 * @aribaapi private
 */
public class PerformanceChecker
{
    private final String _metricName;
    private final boolean _isStable;
    private final boolean _checkTimer;    
    private final long[] _globalThresholds;
    private final Map/*<String, long[]>*/ _typeThresholds;
    
    public PerformanceChecker (PerformanceStateCore metric)
    {
        this(metric.name, false, 0, 0, null);
    }

    public PerformanceChecker (PerformanceStateCore metric, 
                               long errorLevel, 
                               long warningLevel)
    {
        this(metric.name, false, errorLevel, warningLevel, null);
    }

    public PerformanceChecker (PerformanceStateCore metric, 
                               boolean checkTimer,
                               long errorLevel, 
                               long warningLevel)
    {
        this(metric.name, checkTimer, 0, 0, null);
    }

    PerformanceChecker (String name,
                        boolean checkTimer,
                        long errorLevel,
                        long warningLevel,
                        Map typeLevels)
    {
        this(name, false, checkTimer, 0, 0, null);
    }
    
    PerformanceChecker (String name,
                        boolean isStable,
                        boolean checkTimer,
                        long errorLevel,
                        long warningLevel,
                        Map typeLevels)
    {
        _metricName = name.intern();
        _isStable = isStable;
        _checkTimer = checkTimer;
        _globalThresholds = new long[] {warningLevel, errorLevel};
        if (typeLevels == null) {
            _typeThresholds = null;
            return;
        }
        _typeThresholds = MapUtil.map(typeLevels.size());
        for (Iterator i = typeLevels.keySet().iterator(); i.hasNext(); ) {
            String type = (String)i.next();
            Map levels = (Map)typeLevels.get(type);
            Object temp = levels.get(PerformanceCheck.WarningKey);
            Number warningTypeLevel; 
            if (temp instanceof Number) {
                warningTypeLevel = (Number)temp;
            }
            else {
                warningTypeLevel = new Integer((String)temp);
            }
            temp = levels.get(PerformanceCheck.ErrorKey);
            Number errorTypeLevel; 
            if (temp instanceof Number) {
                errorTypeLevel = (Number)temp;
            }
            else {
                errorTypeLevel = new Integer((String)temp);
            }
            long[] typeLevel = new long[] {
                warningTypeLevel.longValue(),
                errorTypeLevel.longValue()
            };
            _typeThresholds.put(type, typeLevel);
        }
    }
    
    public final String getMetricName ()
    {
        return _metricName;
    }

    public boolean isStable ()
    {
        return _isStable;
    }
    
    public long getValue (PerformanceState.Stats stats)
    {
        PerformanceStateCore.Instance instance =
                   (PerformanceStateCore.Instance)stats.get(_metricName);
        if (instance == null) return 0;
        return (_checkTimer) ? instance.getElapsedTime() / 1000 : instance.getCount();
    }

    public String getValueString (PerformanceState.Stats stats)
    {
        long val = getValue(stats);
        return (_checkTimer) ? Long.toString(val).concat(" ms")
            : Long.toString(val);
    }
    
    public long[] getThresholds (String type)
    {
        if (type != null && _typeThresholds != null) {
            long[] levels = (long[])_typeThresholds.get(type);
            if (levels != null) {
                return levels;
            }
        }
        return _globalThresholds;
    } 

    public int check (PerformanceState.Stats stats, ErrorSink sink)
    {
        PerformanceStateCore.Instance instance =
            (PerformanceStateCore.Instance)stats.get(_metricName);
        if (instance == null) {
            return PerformanceCheck.SeverityNone;
        }
        // check the total value 
        int severity = checkValue(getValue(stats), null, stats, sink);
        List/*<EventDetail>*/ events = instance.getEventList();
        if (events == null || severity == PerformanceCheck.SeverityError) {
            // return if there's nothing left to check or we've reached the
            // higher severity already
            return severity;
        }
        // check the type specific values
        for (Iterator i = events.iterator(); i.hasNext(); ) {
            EventDetail ev = (EventDetail)i.next();
            int eventSeverity = checkValue(ev._count, ev._type, stats, sink);
            if (eventSeverity > severity) {
                severity = eventSeverity;
                if (severity == PerformanceCheck.SeverityError) {
                    // we've reached the highest severity so no point in keep going
                    return severity;
                }
            }
        }
        return severity;
    }    

    private int checkValue (long value, 
                            String type,
                            PerformanceState.Stats stats, 
                            ErrorSink sink)
    {
        long[] thresholds = getThresholds(type);
        if (thresholds == null) {
            return PerformanceCheck.SeverityNone;
        }
        long warningThreshold = thresholds[0];
        long errorThreshold = thresholds[1];
        if (value != 0) {
            if ( value >= errorThreshold && errorThreshold != 0) {
                if (sink != null) {
                    String msg = Fmt.S("Performance Error: %s %svalue of %s " +
                            "exceeds threshold: %s",
                            getMetricName(), 
                            type == null ? "" : StringUtil.strcat("(", type, ") "),
                            Long.toString(value),
                            Long.toString(errorThreshold));
                    sink.recordError(stats, this, PerformanceCheck.SeverityError, msg);
                }
                return PerformanceCheck.SeverityError;
            }
            else if ( value >= warningThreshold && warningThreshold != 0) {
                if (sink != null) {
                    String msg = Fmt.S("Performance Warning: %s %svalue of %s " +
                            "exceeds threshold: %s",
                            getMetricName(), 
                            type == null ? "" : StringUtil.strcat("(", type, ") "),
                            Long.toString(value),
                            Long.toString(warningThreshold));
                    sink.recordError(stats, this, PerformanceCheck.SeverityWarning, msg);
                }
                return PerformanceCheck.SeverityWarning;
            }
        }
        return PerformanceCheck.SeverityNone;        
    }    
}
