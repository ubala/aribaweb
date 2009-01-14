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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWNavigation.java#3 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWBaseObject;

public final class AWNavigation extends AWBaseObject
{
    public static final int Hide = 0;   // will hide content (and perhaps never return)
    public static final int Drill = 1;  // will drill down to another page, but will return
    public static final int Leave = 2;  // will leave, and never return

    /**
     * Implementor may catch attempts to hide its content (perhaps to avoid obscuring
     * forms with validation errors).
     * Possible responses:
     *  1) null -- proceed
     *  2) pageComponent()  -- do not proceed -- recycle current page
     *  3) alternate component -- navigate elsewhere instead
     */
    interface Interceptor {
        AWComponent alternateResponseForNavigationAction (AWComponent target, int action);
    }

    /**
     * Run the parent chain seeing if a component wants to prevent navigation
     *
     * A component (e.g. tabset, expando/collapso) that is about to hide content
     * would use this as follows:
         AWComponent action () {
            // check if we're allowed to hide...
            AWComponent r;
            if ((r = AWVNavigation.requestNavigation(this, AWVNavigation.Hide)) != null) {
                return r;
            }

            // do it...

        }
     */
    public static AWComponent requestNavigation (AWComponent target, int action)
    {
        AWErrorManager errorManager = target.errorManager();
        return errorManager.alternateResponseForNavigationAction(target, action);        
    }
}
