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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWSystemUtil.java#8 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.SystemUtil;
import ariba.util.log.Log;


public final class AWSystemUtil extends AWBaseObject
{
    public static void runRunnable (Runnable runnable, boolean createNewThread)
    {
        try {
            if (createNewThread) {
                Thread thread = new Thread(runnable);
                thread.start();
            }
            else {
                runnable.run();
            }
        }
        catch (RuntimeException e) {
            Log.util.debug("Exception in runnable %s : %s", runnable, SystemUtil.stackTrace(e)); 
        }
    }
}
