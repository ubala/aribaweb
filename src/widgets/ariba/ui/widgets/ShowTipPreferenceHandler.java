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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ShowTipPreferenceHandler.java#2 $
*/
package ariba.ui.widgets;

import ariba.ui.widgets.PreferenceHandler;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;


public class ShowTipPreferenceHandler extends PreferenceHandler
{
    public boolean getBooleanPreference (AWRequestContext requestContext, String key)
    {
        AWSession session = requestContext.session(false);
        if (session != null) {
            Boolean flag = (Boolean)session.dict().get(name());
            if (flag != null) {
                return flag.booleanValue();
            }
        }
        return true;
    }

    public void setBooleanPreference (AWRequestContext requestContext,
                                      String key, boolean flag)
    {
        AWSession session = requestContext.session(false);
        if (session != null) {
            session.dict().put(name(), Boolean.valueOf(flag));
        }
    }

}
