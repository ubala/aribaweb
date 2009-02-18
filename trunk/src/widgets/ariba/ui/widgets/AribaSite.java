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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaSite.java#3 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import java.util.List;

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
        SiteActionHandler handler = listHandler();
        _sites = (handler != null) ? handler.getSites(requestContext()) : null;
    }

    public AWResponseGenerating actionClicked ()
    {
        requestContext().put(RealmSite, _currentSite);        
        ActionHandler handler = handler();
        return handler == null ? null : handler.actionClicked(requestContext());
    }
    
    public String siteLabel ()
    {
        return listHandler().siteLabel(_currentSite);
    }
    
    private ActionHandler handler ()
    {
        return ActionHandler.resolveHandlerInComponent(AribaAction.SiteAction, this);
    }
    
    
    protected SiteActionHandler listHandler ()
    {
        ActionHandler handler = ActionHandler.resolveHandlerInComponent(AribaAction.SiteAction, this);
        if (handler instanceof SiteActionHandler) {
            return (SiteActionHandler)handler;
        }
        else if (handler != null) {
            handler = handler.realHandler();
            return (handler instanceof SiteActionHandler) ? (SiteActionHandler)handler : null;
        }
        return null;
    }

}

