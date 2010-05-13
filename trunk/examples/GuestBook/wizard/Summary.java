package wizard;

import ariba.ui.wizard.core.WizardActionTarget;
import ariba.ui.wizard.core.WizardAction;
import ariba.ui.wizard.component.ComponentActionTarget;
import ariba.ui.wizard.component.WizardFrameContent;
import ariba.ui.aribaweb.core.AWComponent;
import app.Post;
import app.Main;
import app.PostService;

public class Summary extends WizardFrameContent
{
    public WizardActionTarget actionClicked (WizardAction action)
    {
        if ("save".equals(action.getName())) {
            Post post = (Post)getContext();
            PostService.addPost(post);
            return mainPageActionTarget();
        }
        return super.actionClicked(action);
    }

    protected WizardActionTarget mainPageActionTarget ()
    {
        AWComponent mainPage = pageWithName(Main.class.getName());
        return new ComponentActionTarget(getFrame().getWizard(),
                              mainPage,
                              true);
    }

}
