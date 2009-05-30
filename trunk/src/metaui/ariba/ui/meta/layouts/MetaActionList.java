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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaActionList.java#12 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.Context;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class MetaActionList extends AWComponent
{
    public ariba.ui.meta.core.ItemProperties _actionCategory;
    public ItemProperties _action;
    Map<String, List<ItemProperties>> _actionsByCategory;
    public Object _menuId;
    public boolean _isGlobal;
    
    protected void sleep()
    {
        super.sleep();
        _actionsByCategory = null;
        _action = null;
        _actionCategory = null;
        _menuId = null;
    }

    public void prepareForGlobal ()
    {
        Context context = MetaContext.currentContext(this);
        _isGlobal = booleanValueForBinding("showGlobal", context.values().get(UIMeta.KeyClass) == null);
        if (_isGlobal) {
            MetaContext.currentContext(this);
            MetaNavTabBar.getState(session()).assignCurrentModuleContext(context);
        }

        // bogus -- should take from Context
        String filterActions = stringValueForBinding("filterActions");
        if (filterActions != null) {
            context.set("filterActions", filterActions);
        }
    }

    public List<ItemProperties> actionCategories ()
    {
        if (!_isGlobal) {
            Context context = MetaContext.currentContext(this);
            UIMeta meta = (UIMeta)context.meta();
            context.push();
            _actionsByCategory = new HashMap();
            List<ItemProperties> categories = meta.actionsByCategory(context, _actionsByCategory, UIMeta.ActionZones);
            context.pop();
            return categories;
        }

        List <String> showCategories = (List)valueForBinding("showOnly");
        List<ItemProperties> categories = MetaNavTabBar.getState(session()).getActionCategories();
        if (showCategories != null) {
            List<ItemProperties> filteredCategories = new ArrayList();
            for (ItemProperties category : categories) {
                if (showCategories.contains(category.name())) {
                    filteredCategories.add(category);
                }
            }
            categories = filteredCategories;
        }
        _actionsByCategory = MetaNavTabBar.getState(session()).getActionsByCategory();

        return categories;
    }

    public List<ItemProperties> actions ()
    {
        return _actionsByCategory.get(_actionCategory.name());
    }

    public AWResponseGenerating actionClicked ()
    {
        // if (_isGlobal) return MetaNavTabBar.getState(session()).fireAction(_action, requestContext());
        Context context = MetaContext.currentContext(this);
        UIMeta meta = (UIMeta)context.meta();
        return meta.fireAction(_action, context, requestContext());
    }
}
