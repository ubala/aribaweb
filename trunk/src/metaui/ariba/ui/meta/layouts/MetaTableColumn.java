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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaTableColumn.java#3 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.table.AWTDataTable;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.Context;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;

import java.util.Map;

public final class MetaTableColumn extends AWTDataTable.Column
{
    protected String _fieldName;

    protected FieldPath _fieldPath;
    protected String _label;
    protected Object _formatter; // AWFormatting

    public String rendererComponentName ()
    {
        return MetaTableColumnRenderer.class.getName();
    }

    public void initializeColumn (AWTDataTable table)
    {
        // we expect to get created and registered by the AWTMetaContent
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        super.init(tagName, bindingsHashtable);
    }

    /** Convenience for initing a dynamic column */
     public void init (AWTDataTable table,
                       String fieldName)
    {
        Context context = MetaContext.currentContext(table);
        context.push();
        context.set(UIMeta.KeyField, fieldName);

        _fieldName = fieldName;
        _fieldPath = ((UIMeta.UIContext)context).fieldPath();

        _label = (String) context.propertyForKey(UIMeta.KeyLabel);

        String formatterName = (String)context.propertyForKey("formatter");
        if (formatterName != null) {
            _formatter = FieldValue.getFieldValue(FieldValue.getFieldValue(table, "formatters"),
                    formatterName);
        }

        context.pop();
    }

    public String fieldName ()
    {
        return _fieldName;
    }

    public FieldPath fieldPath ()
    {
        return _fieldPath;
    }

    public String keyPathString ()
    {
        return (_fieldPath != null) ? _fieldPath.fieldPathString() : null;
    }

    public String label ()
    {
        return _label;
    }

    public boolean isValueColumn ()
    {
        return true;
    }

    public boolean isOptional (AWTDataTable sender)
    {
        return true;    // meta-data specified?
    }

    public boolean initiallyVisible ()
    {
        return true;
    }

    public Object formatter ()
    {
        return _formatter;
    }
}
