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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/StepHeader.java#15 $
*/

package ariba.ui.widgets;

import ariba.util.core.Fmt;
import ariba.util.core.Constants;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;

public final class StepHeader extends AWComponent
{
        // step image names
    public static final String[] StepNumberImageNames = {
        "widg/t01.gif", "widg/t02.gif", "widg/t03.gif", "widg/t04.gif", "widg/t05.gif",
        "widg/t06.gif", "widg/t07.gif", "widg/t08.gif", "widg/t09.gif", "widg/t10.gif",
        "widg/t11.gif", "widg/t12.gif", "widg/t13.gif", "widg/t14.gif", "widg/t15.gif",
    };

        // image for non-numbered step
    public static final String NonNumberedStepImageName = "widg/t00.gif";

    private static final int UninitializedStepIndex = -2;
    private static final int NonNumberedStep = -1;

    private int _stepIndex = -2;

    protected void awake ()
    {
        _stepIndex = UninitializedStepIndex;
    }

    protected void sleep ()
    {
        _stepIndex = 0;
    }

    private int stepIndex ()
    {
        if (_stepIndex == UninitializedStepIndex) {
            AWBinding stepIndex = bindingForName(BindingNames.stepIndex);
            if (stepIndex != null) {
                _stepIndex = intValueForBinding(stepIndex);
            }
            else {
                _stepIndex = NonNumberedStep;
            }
        }
        return _stepIndex;
    }

    public String stepNumberImage ()
    {
        int stepIndex = stepIndex();

        if (stepIndex >= 0 && stepIndex < StepNumberImageNames.length) {
            return StepNumberImageNames[stepIndex];
        }
        return NonNumberedStepImageName;
    }

    public String stepNumberHint ()
    {
        int stepIndex = stepIndex();

        if (stepIndex != NonNumberedStep) {
            String fmt = localizedJavaString(1, "Step {0}" /*  */);
            return Fmt.Si(fmt, Constants.getInteger(stepIndex + 1));
        }
        else {
            return localizedJavaString(2, "Non-numbered step" /*  */);
        }
    }

    public boolean hasSubsteps ()
    {
        AWBinding hasSubsteps = bindingForName(BindingNames.hasSubsteps);
        if (hasSubsteps != null) {
            return booleanValueForBinding(hasSubsteps);
        }
            // By default if we have any content, it must be Substeps
        return componentReference().contentElement() != null;
    }

    public void clearSubstepSeparator ()
    {
        requestContext().put(Substep.ShowSubstepSeparator, null);
    }
}
