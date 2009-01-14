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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardAction.java#2 $
*/

package ariba.ui.wizard.component;

import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.aribaweb.core.AWResponseGenerating;

public final class WizardAction extends AWComponent
{
    public AWResponseGenerating actionClicked ()
    {
        String name = stringValueForBinding(AWBindingNames.name);
        WizardFrame frame = ((WizardPage)pageComponent()).frame();
        Wizard wizard = frame.getWizard();
        ariba.ui.wizard.core.WizardAction action = null;
        if (name != null) {
            action = wizard.getActionWithName(name);
        }
        if (action == null) {
            action = wizard.refresh;
        }
        return WizardUtil.invokeWizardAction(frame, action, requestContext());
    }
}
