/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBaseMonitorStatsPage.java#13 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.NamedValue;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

// subclassed by AWWOMonitorStatsPage
public class AWBaseMonitorStatsPage extends AWComponent implements AWMonitorStatsPage
{
    public static final String PageName = "AWBaseMonitorStatsPage";

    public AWMonitorStats _monitorStats;

    public void setMonitorStats (AWMonitorStats monitorStats)
    {
        _monitorStats = monitorStats;
    }

    public AWMonitorStats getMonitorStats ()
    {
        if (_monitorStats == null) {
            _monitorStats = (AWMonitorStats)valueForBinding("monitorStats");
        }
        return _monitorStats;
    }

        // disable automatic session validation for this page
    protected boolean shouldValidateSession ()
    {
        return false;
    }

    public boolean shouldCachePage ()
    {
        return false;
    }

    //////////////////////
    // Stateless/full Support
    //////////////////////
    public boolean isStateless ()
    {
        return false;
    }

    public int activeSessionCount ()
    {
        return ((AWConcreteApplication)AWConcreteApplication.sharedInstance())
            .getUISessionCount();
    }

    public List<NamedValue> activeSessionCountBuckets ()
    {
        return ((AWConcreteApplication)AWConcreteApplication.sharedInstance())
            .getUISessionCountBuckets();
    }

    public List<NamedValue> activeSessionStatusBuckets ()
    {
        boolean showSessionStatus = "true".equals(pageComponent().requestContext()
            .formValueForKey("showSessionStatus"));
        if (showSessionStatus) {
            return ((AWConcreteApplication)AWConcreteApplication.sharedInstance())
                .getUISessionStatusBuckets();
        }
        return null;
    }

    public List getItemsToDisplay ()
    {
        Map keyValueStats = application().customKeyValueStats();
        if (MapUtil.nullOrEmptyMap(keyValueStats)) {
            return null;
        }
        List result = ListUtil.list();
        SortedSet keyset = new TreeSet(keyValueStats.keySet()); 
        for (Iterator i = keyset.iterator(); i.hasNext();) {
            String name = (String)i.next();
            result.add(
                new NamedValue(                             
                    name,
                    keyValueStats.get(name)));
        }            
        return result;        
    }
    
    /**
     * The current item being displayed by the direct action
     */
    public NamedValue _item;
}
