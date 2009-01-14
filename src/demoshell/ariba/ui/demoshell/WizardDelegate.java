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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/WizardDelegate.java#7 $
*/
package ariba.ui.demoshell;

import ariba.ui.wizard.core.*;
import ariba.ui.wizard.component.ComponentActionTarget;
import ariba.ui.aribaweb.core.AWComponent;

public class WizardDelegate extends ariba.ui.wizard.core.WizardDelegate
{    
    public WizardActionTarget targetForAction (WizardAction action)
    {
        if (action instanceof AWXWizardAction.Action) {
            AWComponent next = ((AWXWizardAction.Action)action).invoke();
            return new ComponentActionTarget(((AWXWizardAction.Action)action).getWizard(), next, true);
        }
        return null;
    }
    
    /**
        Determines the name of the strings resource file for the wizard.
    */
    public String stringFile ()
    {
        return null;
    }

    /**
        Determines the strings group name (analogous to a component) for the strings in the wizard.
    */
    public String stringGroupName ()
    {
        return null;
    }
}
