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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaSite.java#2 $
*/

package ariba.ui.widgets;

import java.util.List;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWFormRedirect;
import ariba.util.core.Fmt;

public class AribaSite extends AWComponent
{
    public static final String RealmSite = "RealmSite";

    public String _currentSite;
    public List<String> _sites;

    public boolean isStateless ()
    {
        return false;
    }
    
    public void init ()
    {
        super.init();
        SiteActionHandler handler = handler();
        _sites = (handler != null) ? handler.getSites(requestContext()) : null;
    }

    public String siteScript ()
    {
        requestContext().put(RealmSite, _currentSite);
        ActionHandler handler = handler();
        String url = handler.url(requestContext());
        return Fmt.S("javascript:window.location='%s';ariba.Event.cancelBubble(event);",
                url);
    }

    public String siteLabel ()
    {
        return handler().siteLabel(_currentSite);
    }

    private SiteActionHandler handler ()
    {
        ActionHandler handler = ActionHandler.resolveHandlerInComponent(AribaAction.SiteAction, this);
        return (handler instanceof SiteActionHandler) ? (SiteActionHandler)handler : null;
    }

}

