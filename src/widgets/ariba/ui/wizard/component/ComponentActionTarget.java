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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/ComponentActionTarget.java#3 $
*/

package ariba.ui.wizard.component;

/**
    An UrlActionTarget  temporarily leaves the wizard and take the user
    to the url specified.

    @aribaapi ariba
*/
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.core.WizardActionTarget;

public final class ComponentActionTarget implements WizardActionTarget
{
    private AWComponent  _component;
    private boolean _quitWizard;
    private WizardFrame _frame;

    /**
        @aribaapi ariba
    */
    public ComponentActionTarget (Wizard wizard,
                                  AWComponent component,
                                  boolean quitWizard)
    {
        _component = component;
        _quitWizard = quitWizard;
        _frame = wizard.getCurrentFrame();
    }

    /**
        @aribaapi ariba
    */
    public boolean terminatesWizard ()
    {
        return _quitWizard;
    }

    /**
        @aribaapi private
    */
    public AWComponent component ()
    {
        return _component;
    }

    /**
        @aribaapi private
    */
    public WizardFrame getOriginatingFrame ()
    {
        return _frame;
    }
}
