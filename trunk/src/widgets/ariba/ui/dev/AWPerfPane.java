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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWPerfPane.java#21 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.ListUtil;
import ariba.util.core.PerformanceState;
import ariba.util.core.PerformanceStateCounter;
import ariba.util.core.PerformanceStateCore;

import java.util.List;
import java.util.Map;

public class AWPerfPane extends AWComponent
{
    public static final String PerfStateCount = "PerfStateCount";
    public static final String PerfStateTime = "PefrStateTime";

    private static final String emptyString = "";

        //bindings
    public String currentItem;

        // vector of strings
    public List metricNameList;

    private Map stats;

    // stateful, so we can retain our stats across phases
    public boolean isStateless ()
    {
        return false;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        stats = session().lastRequestPerfStats();
        if (stats != null) {
            metricNameList = ListUtil.list();
            List others = null;
            PerformanceStateCore[] stdMetrics = PerformanceState.logMetrics();
            for (int j = 0; j < stdMetrics.length; j++) {
                String actualMetricKey = stdMetrics[j].getName();
                if (stats.get(actualMetricKey) != null) {
                    metricNameList.add(actualMetricKey);
                }
                else {
                    if (others == null) {
                        others = ListUtil.list();
                    }
                    others.add(actualMetricKey);
                }
            }
            if (others != null) metricNameList.addAll(others);

            super.renderResponse(requestContext, component);
        }
    }

    public String getCurrentName ()
    {
        return currentItem;
    }

    public String getCurrentCount ()
    {
        PerformanceStateCounter.Instance psc = (PerformanceStateCounter.Instance)stats.get(currentItem);
        return (psc != null) ? psc.countString() : "";
    }

    public static long getCount (Map stats, String item)
    {
        PerformanceStateCounter.Instance psc = (PerformanceStateCounter.Instance)stats.get(item);
        if (psc != null) {
            return psc.getCount();
        }
        return 0;
    }

    public String getCurrentTime ()
    {
        return (String)getTime(stats, currentItem);
    }

    public static String getTime (Map stats, String item)
    {
        PerformanceStateCounter.Instance timer = (PerformanceStateCounter.Instance)stats.get(item);
        return (timer != null) ? timer.elapsedTimeString() : emptyString;
    }

    public boolean show ()
    {
        return PerformanceState.threadStateEnabled();
    }

    public boolean hasMetricDetail ()
    {
        PerformanceStateCore.Instance psc = (PerformanceStateCore.Instance)stats.get(currentItem);
        return (psc != null) && (psc.getEventList() != null);
    }

    public AWComponent viewMetricDetail ()
    {
        PerformanceStateCore.Instance psc = (PerformanceStateCore.Instance)stats.get(currentItem);
        AWPerfDetail page = (AWPerfDetail)pageWithName(AWPerfDetail.class.getName());
        page.setMetric(psc);
        return page;
    }

}
