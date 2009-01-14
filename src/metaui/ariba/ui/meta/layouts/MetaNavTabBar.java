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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaNavTabBar.java#7 $
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
import ariba.ui.meta.core.UIMeta.ModuleProperties;
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
        state.prepare();
        return state;
    }

    public static String[] Zones = { "Main" };

    public static class State
    {
        public List<ModuleProperties> _modules;
        protected ModuleProperties _selectedModule;
        protected List<ItemProperties> _actionCategories;
        protected Map<String, List<ariba.ui.meta.core.ItemProperties>> _actionsByCategory;
        protected ModuleProperties _lastSelectedModule;
        int _ruleSetGeneration = 0;

        public State ()
        {
        }

        void prepare ()
        {
            UIMeta meta = UIMeta.getInstance();
            if (_modules == null || _ruleSetGeneration < meta.ruleSetGeneration()) {
                Context context = meta.newContext();
                _modules = meta.modules(context);
                _ruleSetGeneration = meta.ruleSetGeneration();
            }
        }

        public AWResponseGenerating userSelectedModule (final AWComponent component, ItemProperties module)
        {
            ActionHandler handler = new ActionHandler() {
                public AWResponseGenerating actionClicked (AWRequestContext requestContext)
                {
                    selectModule(_lastSelectedModule);
                    
                    UIMeta meta = UIMeta.getInstance();
                    Context context = meta.newContext();
                    context.push();
                    context.set(UIMeta.KeyModule, _lastSelectedModule.name());
                    String pageName = (String)context.propertyForKey(UIMeta.KeyHomePage);
                    AWComponent page = requestContext.pageWithName(pageName);
                    meta.preparePage(context, page);
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

        public void setSelectedModule(ModuleProperties selectedModule)
        {
            _lastSelectedModule = selectedModule;
        }

        public void selectModule(ModuleProperties selectedModule)
        {
            if (_selectedModule == selectedModule) return;
            _selectedModule = selectedModule;
            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();
            context.push();
            context.set(UIMeta.KeyModule, _selectedModule.name());
            _actionCategories = meta.itemList(context, UIMeta.KeyActionCategory, Zones);
            _actionsByCategory = meta.navActionsByCategory(context);
            context.pop();
        }

        void checkSelectedModule (AWComponent pageComponent)
        {
            // Auto select the best match for the current page
            UIMeta meta = UIMeta.getInstance();
            selectModule(meta.matchForPage(_modules, pageComponent, _selectedModule));
        }

        public AWResponseGenerating fireAction (ItemProperties action, AWRequestContext requestContext)

        {
            // ToDo -- figure out how to fire Action
            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();
            context.push();
            context.set(UIMeta.KeyModule, _selectedModule.name());
            AWResponseGenerating response =  meta.fireAction(action, context, requestContext);
            context.pop();
            return response;
        }

        public AWResponseGenerating redirectForPage (AWComponent pageComponent)
        {
            AWComponent page = pageComponent;
            checkSelectedModule(pageComponent);
            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();
            context.push();
            context.set(UIMeta.KeyModule, _selectedModule.name());
            String pageName = (String)context.propertyForKey(UIMeta.KeyHomePage);
            if (pageName != null && !pageName.equals(pageComponent.componentDefinition().componentName())) {
                page = pageComponent.requestContext().pageWithName(pageName);
                meta.preparePage(context, page);
            }
            context.pop();
            return page;
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
        Context context = MetaContext.currentContext(this);
        context.push();
        context.set(UIMeta.KeyModule, _currentModule.name());
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
