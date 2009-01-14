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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/WizardStepsParent.java#2 $
*/

package ariba.ui.wizard.core;

import java.util.List;

/**
    @aribaapi ariba
*/
public interface WizardStepsParent
{
    /**
        @aribaapi ariba
    */
    public List getSteps ();

    /**
        @aribaapi ariba
    */
    public void insertStepAt (WizardStep step, int index);

    /**
        @aribaapi ariba
    */
    public void insertStepAfter (WizardStep step, WizardStep afterStep);

    /**
        @aribaapi ariba
    */
    public void insertStepBefore (WizardStep step, WizardStep beforeStep);

    /**
        @aribaapi ariba
    */
    public void removeStep (WizardStep step);

    /**
        @aribaapi ariba
    */
    public void updateChildrenVisible ();
}
