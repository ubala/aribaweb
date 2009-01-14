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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXConfirmExitPage.java#4 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.wizard.component.WizardExitActionHandler;
import ariba.ui.wizard.component.WizardPage;

public class AWXConfirmExitPage extends AWComponent
{
    public AWResponseGenerating gotoHome ()
    {
        ActionHandler actionHandler = WizardExitActionHandler.actionHandler(((WizardPage)pageComponent()).frame());
        return (actionHandler != null) ? actionHandler.actionClicked(requestContext())
                : application().mainPage(requestContext());
    }
}
