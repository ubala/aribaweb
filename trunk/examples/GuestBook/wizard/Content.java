package wizard;

import ariba.ui.wizard.component.WizardFrameContent;
import ariba.ui.wizard.core.WizardActionTarget;
import ariba.ui.wizard.core.WizardAction;
import ariba.ui.wizard.core.Wizard;
import app.Post;

public class Content extends WizardFrameContent
{
    public WizardActionTarget actionClicked (WizardAction action)
    {
        Wizard wizard = getFrame().getWizard();
        if (wizard.next == action) {
            Post post = (Post)getContext();
            if (post.isPrivate) {
                return wizard.getFrameWithName("summaryFrame");
            }
        }
        return super.actionClicked(action);
    }
}
