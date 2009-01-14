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

    $Id: //ariba/platform/util/core/ariba/util/log/AvoidDuplicateEvents.java#4 $
*/

package ariba.util.log;

import java.util.WeakHashMap;
import org.apache.log4j.spi.Filter;

/**
    A simple Filter to filter out duplicate log messages so they won't
    get printed multiple times.
    @aribaapi private
*/
public class AvoidDuplicateEvents extends Filter
{
    private final WeakHashMap recentEvents = new WeakHashMap();
    private static final Object exists = new Object();

    /** 
        Empty contructor.
    */
    public AvoidDuplicateEvents ()
    {
    }

    public int decide (org.apache.log4j.spi.LoggingEvent event)
    {
        if (recentEvents.get(event) == exists) {
            return DENY;
        }
        recentEvents.put(event, exists);
        return NEUTRAL;
    }
        
}
