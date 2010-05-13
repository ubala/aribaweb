package wizard;

import ariba.ui.wizard.component.WizardFrameContent;
import ariba.ui.wizard.core.WizardActionTarget;
import ariba.ui.wizard.core.WizardAction;
import app.Main;

public class Information extends WizardFrameContent
{

    public WizardActionTarget actionClicked (WizardAction action)
    {
        if (errorManager().checkErrorsAndEnableDisplay()) {
            return getFrame();
        }
        return super.actionClicked(action);
    }
}
