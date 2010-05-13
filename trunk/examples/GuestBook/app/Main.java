package app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWChangeNotifier;
import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.component.WizardUtil;
import ariba.ui.widgets.Confirmation;
import ariba.ui.widgets.ChooserState;
import ariba.ui.widgets.ChooserSelectionSource;
import ariba.ui.widgets.ChooserSelectionState;
import ariba.util.core.ProgressMonitor;
import ariba.util.core.ListUtil;

import java.util.List;

public class Main extends AWComponent implements ChooserSelectionState
{
    public List _posts;
    public Post _newPost = new Post();
    public Post _currentPost;
    private Post _draggedPost;
    public AWTDisplayGroup _displayGroup = new AWTDisplayGroup();
    private boolean _updateList = true;
    public AWEncodedString _deleteAllConfId;
    public ChooserState _chooserState;
    public ChooserSelectionSource  _chooserSelectionSource;
    public List<Continent> _continentSelections;
    private boolean _invalidateContinentSelections = true;

    public void init () {
        AWPage page = page();
        AWChangeNotifier changeNotifier =
            page.getChangeNotifier();
        PostService.registerChangeListener(changeNotifier);
        page.setPollingInitiated(true);
        page.setPollInterval(5);
        List continents = ListUtil.arrayToList(Continent.values());
        _chooserSelectionSource =
            new ChooserSelectionSource.ListSource(continents, "name");
        _chooserState = new ChooserState(this);
    }

    public void notifyChange () {
        _updateList = true;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component) {
        if (_updateList) {
            _updateList = false;
            _posts = PostService.getPosts();
            _displayGroup.setObjectArray(_posts);                        
        }
        if (_invalidateContinentSelections) {
            _continentSelections = null;
            _invalidateContinentSelections = false;
        }
        super.renderResponse(requestContext, component);
    }

    public void add () {
        if (errorManager().checkErrorsAndEnableDisplay()) return;
        PostService.addPost(_newPost);
        _newPost = new Post();
    }

    public void deleteCurrent () {
        delete(_currentPost);        
    }

    public void delete (Post post) {
        PostService.removePost(post);
    }

    public AWComponent guideAction ()
    {
        requestContext().put("PostAlert", "This is Post #" + _posts.size());
        Wizard wizard = new Wizard
            ("wizard/GuestBook", _newPost, resourceManager());
        return WizardUtil.startWizard(wizard, requestContext());
    }

    public void dragPost ()
    {
        _draggedPost = _currentPost;
    }

    public AWComponent insertPost ()
    {
        if (_draggedPost != _currentPost) {
            _posts.remove(_draggedPost);
            int index = _posts.indexOf(_currentPost);
            _posts.add(index, _draggedPost);
        }
        return null;
    }

    public AWComponent deletePost ()
    {
        delete(_draggedPost);
        return null;
    }

    public String exportURL ()
    {
        AWDirectActionUrl url = AWDirectActionUrl.checkoutFullUrl(requestContext());
        url.setDirectActionName(DirectAction.PostsAction);
        return url.finishUrl();
    }

    public void confirmDeleteAll ()
    {
        Confirmation.showConfirmation(requestContext(), _deleteAllConfId);
    }
    
    public void deleteAllAction ()
    {
        ProgressMonitor progressMonitor =
            ProgressMonitor.instance();
        int size = _posts.size();
        progressMonitor.prepare("Deleting %s of %s posts", size);
        for (int i = size - 1; i >= 0; i--) {
            progressMonitor.incrementCurrentCount();
            Post post = (Post)_posts.get(i);
            PostService.removePost(post);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {                
            }
        }
    }

    public List continentSelections ()
    {
        if (_continentSelections == null) {
            _continentSelections = ListUtil.list();
            if (_newPost.continent != null) {
                _continentSelections.add(_newPost.continent);
            }
            ListUtil.addElementIfAbsent(
                _continentSelections, Continent.Asia);
            ListUtil.addElementIfAbsent(
                _continentSelections, Continent.Australia);
        }
        return _continentSelections;
    }

    public void setSelectionState (Object selection, boolean selected)
    {
        _newPost.continent = selected ? (Continent)selection : null;
        _invalidateContinentSelections = true;
    }

    public Object selectedObject ()
    {
        return _newPost.continent;
    }

    public List selectedObjects ()
    {
        return null;
    }

    public boolean isSelected (Object selection)
    {
        return selection.equals(_newPost.continent);
    }

    public ContinentChooser chooserSearchAction ()
    {
        ContinentChooser chooser =
            pageWithClass(ContinentChooser.class);
        chooser.setup(_chooserState);
        return chooser;
    }
}