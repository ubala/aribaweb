package app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.component.WizardUtil;
import java.util.List;

public class Main extends AWComponent
{
    public List _posts = Post.Posts;
    public Post _newPost = new Post();
    public Post _currentPost;
    public AWTDisplayGroup _displayGroup = new AWTDisplayGroup();

    public void init () {
        updateDisplayGroup();
    }
    
    public void add () {
        if (errorManager().checkErrorsAndEnableDisplay()) return;
        _posts.add(_newPost);        
        updateDisplayGroup();
        _displayGroup.setSelectedObject(_newPost);
        _newPost = new Post();
    }

    public void deleteCurrent () {
        delete(_currentPost);        
    }

    public void delete (Post post) {
        _posts.remove(post);
        updateDisplayGroup();
    }

    private void updateDisplayGroup () {
        _displayGroup.setObjectArray(_posts);
    }

    public AWComponent guideAction ()
    {
        Wizard wizard = new Wizard
            ("wizard/GuestBook", _newPost, resourceManager());
        return WizardUtil.startWizard(wizard, requestContext());
    }
}
