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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ModalPageWrapper.java#32 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWPageCacheMark;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Assert;

public class ModalPageWrapper extends AWComponent implements ActionInterceptor
{
    public final static ActionHandler DisabledActionHandler =
        new ActionHandler(false, true, null);
    public final static String EnvironmentKey = "ModalPageWrapper";

    public final static String PreventBackTrackBindingName = "preventBackTrack";

    public boolean _hasBodyAreaSubTemplate;
    public boolean _hasBottomLeftAreaSubTemplate;
    private AWPageCacheMark _pageCacheMark;
    private AWComponent _returnPage;
    public AWEncodedString _panelId;
    public boolean _inClientPanel;

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();

        // see if there's a client panel in our parent chain
        _inClientPanel = inClientPanel(this);

        _hasBodyAreaSubTemplate = hasSubTemplateNamed("bodyArea");
        _hasBottomLeftAreaSubTemplate = hasSubTemplateNamed("bottomLeftArea");

        if (!_inClientPanel) {
            // mark the page cache so we can truncate on exit
            // from this page (prevent backtracking into dialog)
            _pageCacheMark = session().markPageCache();

            // prevent backtrack -- default to false
            boolean preventBacktrack =
                (hasBinding(PreventBackTrackBindingName) &&
                booleanValueForBinding(PreventBackTrackBindingName));
            _pageCacheMark.setPreventsBacktracking(preventBacktrack);
        }

        // Stash the page that preceded us the first time around
        AWPage requestPage = requestContext().requestPage();
        if (requestPage == null) {
            requestPage = page().previousPage();
        }

        if (requestPage != null) {
            _returnPage = requestPage.pageComponent();
        }
        /*  Use this unstead when Charles makes the methods public...
        _returnPage = session().requestHistory(null).pageAtOffsetFromLastElement(1).pageComponent();
        */
    }

    /*
        ModalPageWrapper.okClicked calls prepareToExit and before that is also evaluates
        and other binding for okClicked action.  Some of those bindings were also calling prepareToExit (via backPage)

        The result to 2 calls were made to Confirmation.hideConfirmation(requestContext()), where we only
        need to make one call to maintain correct stack on the env for the confirmation id.

        So we added the checkin to allow one call to prepageToExit per one call to invokeAction.
    */
    private static String PrepareToExit = "PrepareToExit";
    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        requestContext().remove(PrepareToExit);
        return super.invokeAction(requestContext, component);
    }


    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (!_inClientPanel && !_hasBottomLeftAreaSubTemplate) {
            DialogContentWrapper.allowDialogDisplay(requestContext);
        }
        super.renderResponse(requestContext, component);
    }

    /**
     * Static helper method to retrieve the current modal page wrapper which wraps the
     * component.  If there is no modal page wrapper wrapping the component, a
     * FatalAssertionException will be thrown.
     *
     * @param component one of the components which is contained in the template /
     * template hierarchy of the modal page wrapper
     * @return ModalPageWrapper
     */
    public static ModalPageWrapper instance (AWComponent component)
    {
        ModalPageWrapper instance = peekInstance(component);
        Assert.that((instance!=null), "No instance registered in current environment");

        return instance;
    }

    public static ModalPageWrapper peekInstance (AWComponent component)
    {
        ModalPageWrapper instance =
            (ModalPageWrapper)component.env().peek(EnvironmentKey);
        return instance;
    }

    public static boolean inClientPanel (AWComponent component)
    {
        boolean inClientPanel;
        do {
            inClientPanel = component.isClientPanel();
            component = component.parent();
        } while (component != null && !inClientPanel);

        return inClientPanel;
    }

    /**
     * Returns the page that preceded the current modal page.  If there is no modal page
     * wrapper wrapping the component, a FatalAssertionException will be thrown.
     *
     * @param component one of the components which is contained in the template /
     * template hierarchy of the modal page wrapper
     * @return the preceeding page component
     */
    public static AWComponent returnPage (AWComponent component)
    {
        return instance(component).returnPage();
    }

    public AWComponent returnPage ()
    {
        return _returnPage;
    }

    /**
        Action interceptor interface
    */
    public ActionHandler overrideAction (String action, ActionHandler defaultHandler,
                                         AWRequestContext requestContext)
    {
        if (defaultHandler != null && defaultHandler.isInterrupting(requestContext)) {
            return DisabledActionHandler;
        }

        return null;
    }

    /**
     * Utility method which should be called by any custom actions which exit the modal
     * page.  For example, if the buttons template is used to add custom buttons to
     * the button area of the modal page.
     *
     * @param component one of the components which is contained in the template /
     * template hierarchy of the modal page wrapper
     */
    public static void prepareToExit (AWComponent component)
    {
        instance(component).prepareToExit();
    }

    public void prepareToExit ()
    {
        if (requestContext().get(PrepareToExit) != null) {
            return;
        }

        if (_panelId == null) {
            session().truncatePageCache(_pageCacheMark, true);
        } else {
            Confirmation.hideConfirmation(requestContext());
            // AWInstanceInclude.closePanel(this);
            // Todo: would this me the wrong page?
            page().popModalPanel();            
        }
        
        // block extra calls to prepareToExit
        requestContext().put(PrepareToExit, PrepareToExit);
    }

    /* Default impls if okction and cancelAction aren't bound */
    public AWComponent okClicked ()
    {
        AWComponent returnPage = returnPage();
        AWBinding binding = bindingForName("okAction", true);
        if (binding != null) {
            returnPage = (AWComponent)valueForBinding(binding);
        }
        if (returnPage != null) {
            prepareToExit();
        }
        return returnPage;
    }

    public AWComponent cancelClicked ()
    {
        AWComponent returnPage = returnPage();
        AWBinding binding = bindingForName("cancelAction", true);
        if (binding != null) {
            returnPage = (AWComponent)valueForBinding(binding);
        }
        if (returnPage != null) {
            prepareToExit();
        }
        return returnPage;
    }

    public void setPanelId (AWEncodedString id)
    {
        if (_panelId != id) {
            _panelId = id;
            Confirmation.showConfirmation(requestContext(), _panelId);
        }
    }

    public AWEncodedString panelId ()
    {
        return _panelId;
    }
}
