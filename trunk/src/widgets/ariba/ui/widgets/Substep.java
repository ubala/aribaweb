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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Substep.java#11 $
*/

package ariba.ui.widgets;

import ariba.ui.widgets.BindingNames;
import ariba.util.core.StringUtil;
import ariba.ui.aribaweb.core.AWComponent;

public final class Substep extends AWComponent
{
    private static final String WizardSubstepStyle = "wizSubstep";
    private static final String WizardSubstepCurrentStyle = "wizSubstepCurrent";
    public static final String ShowSubstepSeparator = "ShowSubstepSeparator";

    public String substepStyle ()
    {
        if (booleanValueForBinding(BindingNames.isSelected)) {
            return WizardSubstepCurrentStyle;
        }
        else {
            return WizardSubstepStyle;
        }
    }

    public String substepLabel ()
    {
        String label = stringValueForBinding(BindingNames.label);
        return StringUtil.replaceCharByString(label, ' ', "&nbsp;");
    }

    public boolean showSubstepSeparator ()
    {
        boolean showSeparator = requestContext().get(ShowSubstepSeparator) != null;
        requestContext().put(ShowSubstepSeparator, Boolean.TRUE);
        return showSeparator;
    }

    public boolean isClickable ()
    {
        if (hasBinding(BindingNames.isClickable)) {
            return booleanValueForBinding(BindingNames.isClickable);
        }
        else {
            return true;
        }
    }
}
