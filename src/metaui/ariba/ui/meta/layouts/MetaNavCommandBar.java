/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaNavCommandBar.java#3 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.widgets.PageWrapper;
import ariba.ui.widgets.AribaAction;
import ariba.ui.widgets.ModalPageWrapper;
import ariba.ui.widgets.BrandingComponent;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
// import ariba.dashboard.component.GroupItemSelectionPage;

import java.util.List;
import java.util.ArrayList;

public class MetaNavCommandBar extends BrandingComponent
{
    public ItemProperties _actionCategory;
    public ItemProperties _action;
    public Object _currentMenuId;

    public boolean isStateless ()
    {
        return false;
    }

    public List<ItemProperties> actionCategories ()
    {
        return MetaNavTabBar.getState(session()).getActionCategories();
    }

    public List<ItemProperties> actions ()
    {
        return MetaNavTabBar.getState(session()).getActionsByCategory().get(_actionCategory.name());
    }

    public boolean showMenus ()
    {
        return true;
    }

    public boolean showHomeIcon ()
    {
        boolean disableHome = false;
        AWComponent parentComponent = requestContext().getCurrentComponent().parent();
        while (parentComponent != null && !(parentComponent instanceof PageWrapper) ) {
            parentComponent = parentComponent.parent();
        }
        if (parentComponent != null) {
            disableHome = parentComponent.booleanValueForBinding(
                                ariba.ui.widgets.BindingNames.disableHomeAction);
        }
        // temporary check for login page
        return !disableHome && !(pageComponent() instanceof MetaHomePage);
    }

    public List overflowActions ()
    {
        /*
        List<Action>[] actions = _currentAggregator.getActionsForCategory(_currentCategoryName);
        return actions[3];
         */
        return null;
    }

    public boolean currentItemIsInTop ()
    {
        // since topItems is a small set, this is faster than hashing
        // return ListUtil.indexOfIdentical(_currentCategoryTopItems, _currentItem) >= 0;
        return true;
    }

    public boolean showCurrentCategory ()
    {
        // maybe do dynamic evaluation of visibility?
        return true;
    }

    public AWResponseGenerating currentItemClicked ()
    {
        ActionHandler handler = ActionHandler.resolveHandlerInComponent(
                AribaAction.GlobalNavAction, this, new NavActionHandler(_action));

        return handler.actionClicked(requestContext());
    }

    public AWResponseGenerating overflowItemClicked ()
    {
        // _currentAggregator.updateActionChoice(_currentItem, pageComponent());
        return currentItemClicked();
    }

    public AWResponseGenerating showAllClicked ()
    {
        // ToDo: move GroupItemSelection page to widgets
        return null;
        /*
        GroupItemSelectionPage page = (GroupItemSelectionPage)pageWithName(GroupItemSelectionPage.class.getName());
        page.init(actionRecsForActions(overflowActions()), (String)_actionCategory.properties().get(UIMeta.KeyLabel),
                "providerName", "label", null,
                new AWActionCallback(this) {
                    public AWResponseGenerating doneAction (AWComponent sender) {
                        _action = ((_ActionRec)((GroupItemSelectionPage)sender).getSelectedItem()).action;
                        // _currentAggregator.updateActionChoice(_currentItem, pageComponent());
                        return currentItemClicked();
                    }
                });
        return page;
        */
    }

    public static class _ActionRec { ItemProperties action; public String label, providerName; }

    public static List<_ActionRec> actionRecsForActions (List<ItemProperties> actions)
    {
        // Flatten list with labels and providerNames
        List<_ActionRec> typeRecs = new ArrayList();
        for (ariba.ui.meta.core.ItemProperties action : actions) {
            _ActionRec rec = new _ActionRec();
            rec.action = action;
            rec.label = (String)action.properties().get(UIMeta.KeyLabel);
            rec.providerName = (String)action.properties().get("package");
            if (rec.providerName == null) rec.providerName = "Other" /* */;
            typeRecs.add(rec);
        }
        return typeRecs;
    }

    public String pageCommandBar ()
    {
        // Note this component is expected to be referenced from within a component
        // which itself is referenced from within a PageWrapper.
        String commands = resolveTemplateOrComponentBasedInclude(ariba.ui.widgets.BindingNames.commands);
        return (commands != null && !commands.equals(componentDefinition().componentName())) ? commands : null;
    }

    public boolean disabled ()
    {
        return isOnModalPage() || actions() == null;
    }

    public boolean isOnModalPage ()
    {
        return ModalPageWrapper.peekInstance(this) != null;
    }

    public String dashboardIcon ()
    {
        return isOnModalPage() ?
            "icn_disabled_home_arrow.gif" : "icn_home_arrow.gif";
    }

    public static class NavActionHandler extends ActionHandler
    {
        ItemProperties _action;

        public NavActionHandler (ItemProperties action)
        {
            _action = action;
        }

        public AWResponseGenerating actionClicked (AWRequestContext requestContext)
        {
            return MetaNavTabBar.getState(requestContext.session()).fireAction(_action, requestContext);
        }
    }
}
