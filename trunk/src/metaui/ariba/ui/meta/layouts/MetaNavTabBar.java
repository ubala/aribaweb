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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaNavTabBar.java#4 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.widgets.AribaAction;
import ariba.ui.widgets.ModalPageWrapper;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.Context;

import java.util.List;
import java.util.Map;

public class MetaNavTabBar extends AWComponent
{
    public static State getState(AWSession session)
    {
        State state = (State)session.dict().get("_MNBSessState");
        if (state == null) {
            state = new State();
            session.dict().put("_MNBSessState", state);
        }
        return state;
    }

    public static String[] Zones = { "Main" };

    public static class State
    {
        public List<ItemProperties> _modules;
        protected ItemProperties _selectedModule;
        protected List<ItemProperties> _actionCategories;
        protected Map<String, List<ariba.ui.meta.core.ItemProperties>> _actionsByCategory;
        protected ItemProperties _lastSelectedModule;

        public State ()
        {
            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();
            _modules = meta.itemList(context, UIMeta.KeyModule, Zones);
        }

        public AWResponseGenerating userSelectedModule (final AWComponent component, ItemProperties module)
        {
            ActionHandler handler = new ActionHandler() {
                public AWResponseGenerating actionClicked (AWRequestContext requestContext)
                {
                    selectModule(_lastSelectedModule);
                    
                    UIMeta meta = UIMeta.getInstance();
                    Context context = meta.newContext();
                    context.restoreActivation(_lastSelectedModule.activation());
                    String pageName = (String)context.propertyForKey("homePage");
                    AWComponent page = requestContext.pageWithName(pageName);
                    context.pop();
                    return page;
                }
            };

            handler = ActionHandler.resolveHandlerInComponent(
                    AribaAction.HomeAction, component, handler);
            return handler.actionClicked(component.requestContext());
        }

        public ItemProperties getSelectedModule()
        {
            return _selectedModule;
        }

        public List<ItemProperties> getActionCategories()
        {
            return _actionCategories;
        }

        public Map<String, List<ItemProperties>> getActionsByCategory()
        {
            return _actionsByCategory;
        }

        public void setSelectedModule(ItemProperties selectedModule)
        {
            _lastSelectedModule = selectedModule;
        }

        public void selectModule(ItemProperties selectedModule)
        {
            _selectedModule = selectedModule;
            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();
            context.push();
            context.restoreActivation(selectedModule.activation());
            _actionCategories = meta.itemList(context, UIMeta.KeyActionCategory, Zones);
            _actionsByCategory = meta.navActionsByCategory(context);
            context.pop();
        }

        void checkSelectedModule (AWComponent pageComponent)
        {
            // ToDo -- Tab detection protocol
            if (_selectedModule == null && _modules.size() > 0) {
                selectModule(_modules.get(0));
            }
        }

        public AWResponseGenerating fireAction (ItemProperties action, AWRequestContext requestContext)

        {
            // ToDo -- figure out how to fire Action
            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();
            context.push();
            context.restoreActivation(_selectedModule.activation());
            return meta.fireAction(action, context, requestContext);
        }

    }

    public State _state;
    public ItemProperties _currentModule;

    protected void awake ()
    {
        _state = getState(session());
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _state.checkSelectedModule(pageComponent());
        super.renderResponse(requestContext, component);
    }

    protected void sleep()
    {
        super.sleep();
        _state = null;
        _currentModule = null;
    }

    public String currentModuleLabel ()
    {
        // Todo: maybe label should be pre-resolved on the Item...
        Context context = MetaContext.currentContext(this);
        context.restoreActivation(_currentModule.activation());
        String label = (String)context.propertyForKey(UIMeta.KeyLabel);
        context.pop();
        return label;
    }

    public AWResponseGenerating moduleClicked ()
    {
        return _state.userSelectedModule(this, _currentModule);
    }

    public String disabled ()
    {
        ActionHandler handler = ActionHandler.resolveHandlerInComponent(AribaAction.HomeAction, this);
        boolean homeActionIsDisabled = (handler != null) && !handler.isEnabled(requestContext());
        return (isOnModalPage() || homeActionIsDisabled) ? "disabled" : null;
    }

    public boolean isOnModalPage ()
    {
        return ModalPageWrapper.peekInstance(this) != null;
    }
}
