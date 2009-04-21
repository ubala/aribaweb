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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaNavTabBar.java#20 $
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
import java.util.HashMap;

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

    public static void invalidateState (AWSession session)
    {
        session.dict().remove("_MNBSessState");
    }

    public static class State
    {
        UIMeta.ModuleInfo _moduleInfo;
        public List<ModuleProperties> _modules;
        protected ModuleProperties _selectedModule;
        protected List<String> _selectedModuleClassNames;
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
                _moduleInfo = meta.computeModuleInfo(context, true);
                _modules = _moduleInfo.modules;
                if (_selectedModule != null)_selectedModule = moduleNamed(_selectedModule.name());
                _ruleSetGeneration = meta.ruleSetGeneration();
            }
        }

        public AWResponseGenerating userSelectedModule (final AWComponent component, ItemProperties module)
        {
            ActionHandler handler = new ActionHandler() {
                public AWResponseGenerating actionClicked (AWRequestContext requestContext)
                {
                    return gotoModule(_lastSelectedModule, requestContext);
                }
            };

            handler = ActionHandler.resolveHandlerInComponent(
                    AribaAction.HomeAction, component, handler);
            return handler.actionClicked(component.requestContext());
        }

        public AWResponseGenerating gotoHomeModule (AWRequestContext requestContext)
        {
            return gotoModule(_modules.get(0), requestContext);
        }

        ModuleProperties moduleNamed (String name)
        {
            for (ModuleProperties m : _modules) {
                if (m.name().equals(name)) return m;
            }
            return null;
        }

        public AWResponseGenerating gotoModule (String name, AWRequestContext requestContext)
        {
            ModuleProperties m = moduleNamed(name);
            return (m != null) ? gotoModule(m, requestContext) : null;
        }

        public AWResponseGenerating gotoModule (ModuleProperties module, AWRequestContext requestContext)
        {
            selectModule(module);

            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();
            context.push();
            context.set(UIMeta.KeyModule, module.name());
            String pageName = (String)context.propertyForKey(UIMeta.KeyHomePage);
            AWComponent page = requestContext.pageWithName(pageName);
            meta.preparePage(context, page);
            context.pop();
            return page;
        }

        public ItemProperties getSelectedModule()
        {
            if (_selectedModule == null && !_modules.isEmpty()) _selectedModule = _modules.get(0);
            return _selectedModule;
        }

        public UIMeta.ModuleInfo moduleInfo()
        {
            return _moduleInfo;
        }

        public List<ItemProperties> getActionCategories()
        {
            return _actionCategories;
        }

        public Map<String, List<ItemProperties>> getActionsByCategory()
        {
            return _actionsByCategory;
        }

        public void setSelectedModule (ModuleProperties selectedModule)
        {
            _lastSelectedModule = selectedModule;
        }

        static Object listOrSingleton (Object val)
        {
            return ((val instanceof List) && ((List)val).size() == 1)
                    ? ((List)val).get(0)
                    : val;
        }

        public List<ModuleProperties> getModules ()
        {
            return _modules;
        }

        // Used by page content areas to re-push the module context onto the meta context
        public void assignCurrentModuleContext (Context context)
        {
            context.set(UIMeta.KeyModule, _selectedModule.name());
            context.set(UIMeta.KeyClass, listOrSingleton(_selectedModule.getAllTypes()));
        }

        public void selectModule (ModuleProperties selectedModule)
        {
            if (_selectedModule == selectedModule) return;
            _selectedModule = selectedModule;
            UIMeta meta = UIMeta.getInstance();
            Context context = meta.newContext();

            context.push();

            assignCurrentModuleContext(context);

            _actionsByCategory = new HashMap();
            _actionCategories = meta.actionsByCategory(context, _actionsByCategory, UIMeta.ModuleActionZones);

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
            UIMeta.UIContext context = (UIMeta.UIContext)meta.newContext();
            context.setRequestContext(requestContext);
            context.push();
            context.set(UIMeta.KeyModule, _selectedModule.name());
            String className = (String)action.properties().get(UIMeta.KeyClass);
            if (className != null) context.set(UIMeta.KeyClass, className);
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
