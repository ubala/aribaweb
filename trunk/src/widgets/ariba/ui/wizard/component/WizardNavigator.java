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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardNavigator.java#3 $
*/

package ariba.ui.wizard.component;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.wizard.core.WizardAction;
import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.wizard.core.WizardStep;
import ariba.ui.wizard.core.WizardStepsParent;
import ariba.ui.wizard.core.Wizard;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import java.util.List;

public final class WizardNavigator extends AWComponent
{
    public static final String ActionButtonNameSuffix = "button";

    public WizardFrame _frame;
    public Wizard _wizard;
    public WizardStep _currentSubstep;
    private WizardAction _currentAction;
    private String _currentActionButtonName;

    protected void sleep ()
    {
        _frame = null;
        _currentAction = null;
        _wizard = null;
        _currentSubstep = null;
        _currentActionButtonName = null;
    }

    protected void awake ()
    {
        _frame = (WizardFrame)valueForBinding(BindingNames.frame);
        Assert.that(_frame != null, "Wizard frame is null in WizardNavigator");
        _wizard = _frame.getWizard();
    }

    public void setCurrentAction (WizardAction action)
    {
        _currentAction = action;
        _currentActionButtonName = StringUtil.strcat(_currentAction.getName(),
                                               ActionButtonNameSuffix);
    }

    public WizardAction currentAction ()
    {
        return _currentAction;
    }

    public String buttonName ()
    {
        return _currentActionButtonName;
    }

    public String actionName ()
    {
        return _currentAction.getName();
    }

    public AWResponseGenerating actionClicked ()
    {
        return WizardUtil.invokeWizardAction(
            _frame, _currentAction, requestContext());
    }

    public boolean submitFormForCurrentAction ()
    {
        return _currentAction.shouldTakeValues();
    }

    public boolean currentStepHasSubsteps ()
    {
            // dialog frames never have sub-steps
        if (_frame.isDialogFrame()) {
            return false;
        }

            // see if there are any visible sub-steps
        List substeps = substeps();
        int visibleSubstepCount = 0;
        if (!ListUtil.nullOrEmptyList(substeps)) {
            for (int index = substeps.size() - 1; index >= 0; index--) {
                if (((WizardStep)substeps.get(index)).isVisible()) {
                    visibleSubstepCount++;
                }
            }
        }
        // Need thorough comment here for the whaquie logic
        return visibleSubstepCount > 1 || (visibleSubstepCount == 1 && substeps.size() == 1);
    }

    public List substeps ()
    {
        return topLevelStep().getSteps();
    }

    public boolean isCurrentSubstepSelected ()
    {
        return _currentSubstep == _frame.getStep();
    }

    public AWComponent substepClicked ()
    {
        WizardFrame frame = _frame.getWizard().gotoStep(_currentSubstep);
        return WizardUtil.createWizardPage(frame, requestContext());
    }

    public WizardStep topLevelStep ()
    {
        return topLevelStep(_frame.getStep());
    }

    private WizardStep topLevelStep (WizardStep step)
    {
        WizardStepsParent parent;
        while ((parent = step.getParent()) instanceof WizardStep) {
            step = (WizardStep)parent;
        }
        return step;
    }

    public boolean showWizardTOC ()
    {
        boolean isDialogFrame = _frame.isDialogFrame();
        boolean hasMultiSteps = hasMultiSteps();
        boolean showSteps = _wizard.showSteps();
        return !isDialogFrame
                && hasMultiSteps
                && !requestContext().isPrintMode()
                && showSteps;
    }

    public boolean hasMultiSteps ()
    {
        boolean hasMultiSteps = false;
        List steps = _wizard.getSteps();
        if (steps != null && steps.size() > 1) {
            hasMultiSteps = true;
        }
        return hasMultiSteps;
    }

}
