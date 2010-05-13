package wizard;

import ariba.ui.wizard.core.WizardActionTarget;
import ariba.ui.wizard.core.WizardAction;

public class Exit extends Summary
{
    public WizardActionTarget actionClicked (WizardAction action)
    {
        if ("delete".equals(action.getName())) {
            return mainPageActionTarget();
        }
        return super.actionClicked(action);
    }
}
