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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardPageWrapper.java#2 $
*/

package ariba.ui.wizard.component;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.wizard.core.WizardFrame;
import ariba.util.core.Assert;

public final class WizardPageWrapper extends AWComponent
{   
    private WizardFrame _frame;

    protected void sleep ()
    {
        _frame = null;
    }
    
    protected void awake ()
    {
        _frame = (WizardFrame)valueForBinding(BindingNames.frame);
        Assert.that(_frame != null, "wizard frame is null");
    }

    public WizardFrame frame ()
    {
        return _frame;
    }

    public String toc ()
    {
        return _frame.isDialogFrame() ? null : WizardTOC.class.getName();
    }
}
