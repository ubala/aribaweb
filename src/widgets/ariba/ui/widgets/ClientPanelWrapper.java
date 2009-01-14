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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ClientPanelWrapper.java#2 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWInstanceInclude;
import ariba.ui.aribaweb.util.AWEncodedString;

public class ClientPanelWrapper extends AWComponent
{
    public final static String EnvironmentKey = "ModalPageWrapper";

    public AWComponent _bodyTemplateContext;
    public AWEncodedString _panelId;

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();

        _bodyTemplateContext =
            BrandingComponent.componentWithTemplateNamed("bodyArea", this);
    }

    public void setPanelId (AWEncodedString id)
    {
        if (_panelId != id) {
            _panelId = id;
            Confirmation.showConfirmation(requestContext(), _panelId);
        }
    }

    public Object AWEncodedString ()
    {
        return _panelId;
    }

    protected void prepareToExit ()
    {
        Confirmation.hideConfirmation(requestContext());
        AWInstanceInclude.closePanel(this);
    }

    public AWComponent okClicked ()
    {
        AWComponent returnPage = null;
        ariba.ui.aribaweb.core.AWBinding binding = bindingForName("okAction", true);
        if (binding != null) {
            returnPage = (AWComponent)valueForBinding(binding);
        }
        prepareToExit();
        return returnPage;
    }

    public AWComponent cancelClicked ()
    {
        AWComponent returnPage = null;
        ariba.ui.aribaweb.core.AWBinding binding = bindingForName("cancelAction", true);
        if (binding != null) {
            returnPage = (AWComponent)valueForBinding(binding);
        }
        prepareToExit();
        return returnPage;
    }
}
