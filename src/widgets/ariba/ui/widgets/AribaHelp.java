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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaHelp.java#7 $
*/

package ariba.ui.widgets;

import java.util.List;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;

public class AribaHelp extends AWComponent
{
    public static final String HelpArea = "HelpArea";
    public static final String AuxiliaryHelpUrl = "AuxiliaryHelpUrl";

    public String _currentArea;
    public List<String> _helpAreas;


    /////////////
    // Awake
    /////////////
    protected void awake()
    {
        super.awake();
        HelpActionHandler handler = handler();
        _helpAreas = (handler != null) ? handler.getHelpAreas(requestContext()) : null;
    }

    protected void sleep()
    {
        super.sleep();
        _currentArea = null;
	    _helpAreas = null;
    }

    public AWResponseGenerating currentItemClicked ()
    {
        if (helpScript() == null) {
            requestContext().put(HelpArea, _currentArea);
            ActionHandler handler = handler();
            return handler.actionClicked(requestContext());
        }

        return null;
    }


    public String helpScript ()
    {
        requestContext().put(HelpArea, _currentArea);
        ActionHandler handler = handler();
        return handler.onClick(requestContext());
    }

    private HelpActionHandler handler ()
    {
        ActionHandler handler = ActionHandler.resolveHandlerInComponent(AribaAction.HelpAction, this);
        return (handler instanceof HelpActionHandler) ? (HelpActionHandler)handler : null;
    }

}

