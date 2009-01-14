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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaTableColumnRenderer.java#2 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.table.AWTDataTable;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;

import java.util.Map;

public final class MetaTableColumnRenderer extends AWTDataTable.ColumnRenderer
{
    UIMeta.UIContext _metaContext;

    public void sleep ()
    {
        _metaContext = null;
        super.sleep();
    }

    public ariba.ui.meta.layouts.MetaTableColumn thisColumn ()
    {
        return (ariba.ui.meta.layouts.MetaTableColumn)column();
    }

    public UIMeta.UIContext metaContext ()
    {
        if (_metaContext == null) _metaContext = (UIMeta.UIContext) MetaContext.currentContext(this);
        return _metaContext;
    }

    public Object properties ()
    {
        return metaContext().properties();
    }

    public Object columnBindingValue (String key, Object defaultVal)
    {
        Object val = defaultVal;
        Map map = (Map)metaContext().propertyForKey("columnBindings");
        if (map != null) {
            Object bval = map.get(key);
            if (bval != null) val = bval;
        }

        return val;
    }

    public Object columnWidth ()
    {
        // min width for image columns
        return columnBindingValue("width", (column().isValueColumn() ? null : "1px"));
    }
    
    /** Evaluators for the curentColumn's bindings */
    public Object cc_style ()
    {
        return _table.emitColumnStyle((String)metaContext().propertyForKey("style"));
    }

    public Object cc_nowrap ()
    {
        return null;
    }

    public String keyPathString ()
    {
        return thisColumn().keyPathString();
    }

    public Object cc_sortKey ()
    {
        return keyPathString();
    }

    public Object cc_label ()
    {
        return thisColumn().label(_table);
    }

    public Object cc_sortCaseSensitively ()
    {
        return null;
    }

    public Object cc_noSort ()
    {
        return null;
    }

    public Object disableSort ()
    {
        return (_table.renderToExcel()) ? Boolean.TRUE : cc_noSort();
    }

    public Object cc_formatter ()
    {
        return thisColumn().formatter();
    }

    public String columnVAlignment ()
    {
        return _table.globalVAlignment();
    }

    public Object cc_align ()
    {
        return metaContext().propertyForKey("align");
    }
}
