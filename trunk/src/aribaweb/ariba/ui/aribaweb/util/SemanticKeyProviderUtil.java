/*
   Copyright (c) 2010 Ariba, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   All Rights Reserved.

   $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/SemanticKeyProviderUtil.java#2 $

   Responsible: gbhatnagar
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.PerformanceState;

public class SemanticKeyProviderUtil
{
    public static String getKey (SemanticKeyProvider provider, Object receiver, AWComponent component)
    {
        PerformanceState.Stats stats = PerformanceState.getThisThreadHashtable();
        try 
        {
            if (stats != null) {
                stats.setRecordingSuspended(true);
            } 

            String key = provider.getKey(receiver, component);

            Log.aribaweb_html.debug("[SemanticKey] Object: %s, " +
                    "Provider: %s, Component: %s, Key: %s",
                    receiver, provider.getClass().getName(), component.name(),
                    key);
            
            return key;
        }
        finally
        {
            // resume performance counters
            if (stats != null) {
                stats.setRecordingSuspended(false);
            } 
        }
    }
}
