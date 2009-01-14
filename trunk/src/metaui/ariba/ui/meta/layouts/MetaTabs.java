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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaTabs.java#1 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.Context;

public class MetaTabs extends MetaLayout
{
    protected ariba.ui.meta.core.ItemProperties _selectedLayout;
    public ItemProperties _tabLayout;

    public String tabLabel ()
    {
        Context context = MetaContext.currentContext(this);
        context.restoreActivation(_tabLayout.activation());
        context.set(UIMeta.KeyLayoutProperties, true);
        String label = (String)context.propertyForKey(UIMeta.KeyLabel);
        context.pop();
        return label;
    }

    public Object selectedLayout ()
    {
        if (_selectedLayout == null) _selectedLayout = _allLayouts.get(0);
        _layout = _selectedLayout;
        return _selectedLayout;
    }

    public void setSelectedLayout (Object layout)
    {
        _layout = _selectedLayout = (ItemProperties)layout;
    }
}
