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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaTabs.java#3 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.Context;
import ariba.ui.aribaweb.core.AWComponent;

import java.util.List;

public class MetaTabs extends AWComponent
{
    public List<String> _tabLayoutNames;
    public String _currentTabName;
    String _selectedTabName;

    public boolean isStateless()
    {
        return false;
    }

    public String getSelectedTabName()
    {
        return _selectedTabName == null ? _tabLayoutNames.get(0) : _selectedTabName;
    }

    public void setSelectedTabName(String selectedTabName)
    {
        _selectedTabName = selectedTabName;
    }

    // Todo: evaluate visibility
    
    public String currentTabLabel ()
    {
        Context context = MetaContext.currentContext(this);
        context.push();
        context.set(UIMeta.KeyLayout, _currentTabName);
        String label = (String)context.propertyForKey(UIMeta.KeyLabel);
        context.pop();
        return label;
    }
}
