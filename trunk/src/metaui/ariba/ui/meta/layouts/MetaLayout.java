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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaLayout.java#3 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.Context;

import java.util.List;
import java.util.Map;

public class MetaLayout extends AWComponent
{
    protected List<ItemProperties> _allLayouts;
    protected Map<String, Object> _layoutsByZones;
    protected ItemProperties _layout;
    protected Meta.PropertyMap _propertyMap;

    public boolean isStateless()
    {
        return false;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        Context context = MetaContext.currentContext(this);
        UIMeta meta = (UIMeta)context.meta();
        _allLayouts = meta.itemList(context, UIMeta.KeyLayout, zones());

        super.renderResponse(requestContext, component);
    }

    public List<ItemProperties> allLayouts ()
    {
        if (_allLayouts == null) {
            Context context = MetaContext.currentContext(this);
            UIMeta meta = (UIMeta)context.meta();
            _allLayouts = meta.itemList(context, UIMeta.KeyLayout, zones());
        }
        return _allLayouts;
    }

    public Map<String, Object> layoutsByZones ()
    {
        if (_layoutsByZones == null) {
            Context context = MetaContext.currentContext(this);
            UIMeta meta = (UIMeta)context.meta();
            _layoutsByZones =  meta.itemsByZones(context, UIMeta.KeyLayout, zones());
        }
        return _layoutsByZones;
    }

    protected String[] zones ()
    {
        return UIMeta.ZonesTLRB;
    }

    public void setLayout (ItemProperties layout)
    {
        _layout = layout;
        _propertyMap = null;
    }

    public ItemProperties layout ()
    {
        return _layout;
    }

    public Meta.PropertyMap propertyMap ()
    {
        if (_propertyMap == null) {
            Context context = MetaContext.currentContext(this);
            context.push();  
            _propertyMap = context.allProperties();
            context.pop();
        }
        return _propertyMap;
    }

    public String label ()
    {
        Context context = MetaContext.currentContext(this);
        return (String)context.resolveValue(propertyMap().get(UIMeta.KeyLabel));
    }

}
