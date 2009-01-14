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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWShutdownState.java#4 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.shutdown.ShutdownManager;

public final class AWShutdownState extends AWBaseObject
{
    public static long GracePeriod;
    public static long WarningPeriod;
    public static long WarningInterval;

    private long _nextShutdownWarning = System.currentTimeMillis();

    public static void init (ShutdownManager sm)
    {
        GracePeriod = sm.getGracePeriod();
        WarningPeriod = sm.getWarningPeriod();
        WarningInterval = sm.getWarningInterval();
    }

    public boolean shouldDisplayWarning ()
    {
        return  _nextShutdownWarning <= System.currentTimeMillis();
    }

    /**
        The next shutdown warning time advances when we "get" the warning.
     */
    public void getWarning ()
    {
        _nextShutdownWarning = System.currentTimeMillis() + WarningInterval;
    }
    
}

