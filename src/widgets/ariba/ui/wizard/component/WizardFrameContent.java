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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardFrameContent.java#3 $
*/
package ariba.ui.wizard.component;

import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.wizard.core.WizardAction;
import ariba.ui.wizard.core.WizardActionTarget;
import ariba.ui.wizard.core.WizardFrameDelegate;
import ariba.ui.aribaweb.core.AWComponent;

abstract public class WizardFrameContent extends AWComponent
{
    private WizardFrame _frame;
    
        // all wizard frame are by default stateful.
    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        _frame = ((WizardPage)pageComponent()).frame();
        if (_frame.getDelegate() == null) {
            _frame.setDelegate(new WizardFrameDelegate() {
                public WizardActionTarget targetForAction(WizardAction action)
                {
                    return actionClicked(action);
                }

                public void prepareForResponse(WizardFrame wizardFrame)
                {
                }
            });
        }
    }
    
    public Object getContext ()
    {
        return _frame.getWizard().getContext();
    }

    public WizardFrame getFrame ()
    {
        return _frame;
    }
    
    protected void setValid (boolean isValid)
    {
        _frame.setValid(isValid);
    }
    
    public WizardActionTarget actionClicked (WizardAction action)
    {
        return null;
    }
}
