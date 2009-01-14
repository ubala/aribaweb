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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardTOC.java#2 $
*/

package ariba.ui.wizard.component;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.wizard.core.WizardStep;
import ariba.ui.wizard.core.WizardStepsParent;
import ariba.util.core.Assert;
import java.util.List;

public final class WizardTOC extends AWComponent
{
    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    public Wizard _wizard;
    public WizardStep _currentStep;
    public WizardStep _selectedStep;
    public List _wizardSteps;
    private WizardFrame _frame;

    protected void sleep ()
    {
        _wizard = null;
        _selectedStep = null;
        _currentStep = null;
        _wizardSteps = null;
        _frame = null;
    }

    private WizardStep topLevelStep (WizardStep step)
    {
        WizardStepsParent parent;
        while ((parent = step.getParent()) instanceof WizardStep) {
            step = (WizardStep)parent;
        }
        return step;
    }

    protected void awake ()
    {
        _frame = ((WizardPage)pageComponent()).frame();
        Assert.that(_frame != null, "wizard frame is null in WizardTOC");
        _wizard = _frame.getWizard();
        _wizardSteps = _wizard.getSteps();
        _selectedStep = topLevelStep(_frame.getStep());
    }

    public String label ()
    {
        return (_wizard.getSelectionsFrame() == null) ? _wizard.getLabel() : null;
    }

    public AWComponent stepClicked ()
    {
        WizardFrame frame = _wizard.gotoStep(_currentStep);
        return WizardUtil.createWizardPage(frame, requestContext());
    }

    public AWComponent selectionsClicked ()
    {
        WizardFrame frame = _wizard.gotoFrame(_wizard.getSelectionsFrame());
        return WizardUtil.createWizardPage(frame, requestContext());
    }
}
