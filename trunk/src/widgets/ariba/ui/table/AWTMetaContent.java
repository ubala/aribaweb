/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTMetaContent.java#9 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.OrderedList;

import java.util.Map;
import ariba.util.core.Assert;
import ariba.ui.table.AWTDataTable;
import ariba.ui.validation.AWVFormatterFactory;
import org.w3c.dom.Element;

/**
 * This class is used inside an AWTTable to cause AWL group-based columns to be inserted in its place in the table.
 * e.g.    <AWTDataTable ...>
 *              <AWTColumn  > some normal column </AWTColumn>
 *
 *              <AWTMetaContent group="someGroupName"/>     <--- columns from group inserted here
 *
 *              <AWTColumn> some other column </AWTColumn>
 *         </AWTDataTable>
 *
 * This class exists primarily for SET UP.  The Bulk of the "GroupView" functionality is in the AWTMetaContentRenderer
 */
public final class AWTMetaContent extends AWTDataTable.Column
{
    // public, but only for the use of AWTDataTable...
    public AWBinding      _layoutBinding;
    public AWBinding      _useXMLFieldPathBinding;
    public AWBinding      _sessionless;
    public AWBinding      _formatters;

    public String rendererComponentName ()
    {
        return AWTMetaContentRenderer.class.getName();
    }

    public void initializeColumn (AWTDataTable table)
    {
        // set this column as a "wrapper column"
        table.setWrapperColumn(this);
        boolean useDirectActionBinding = table.booleanValueForBinding(_sessionless);
        Map formatters = (Map)table.valueForBinding(_formatters);
        if (formatters == null) {
            formatters = AWVFormatterFactory.formattersForComponent(table);
        }

        Element layout = (Element)table.valueForBinding(_layoutBinding);
        if (layout == null) return;

        Object columnMetas = FieldValue.getFieldValue(layout, "children");
        OrderedList listImpl = OrderedList.get(columnMetas);
        int count = listImpl.size(columnMetas);

        Assert.that(count > 0, "Meta data table must have > 0 columns in the Layout");

        // alloc and register AWTGroupColumns based on the meta data
        boolean useXMLFieldPath = table.booleanValueForBinding(_useXMLFieldPathBinding);
        for (int i=0; i<count; i++) {
            Element columnMeta = (Element)listImpl.elementAt(columnMetas, i);
            String key = columnMeta.getAttribute("key");
            String label = columnMeta.hasAttribute("label") ? columnMeta.getAttribute("label") : null;
            AWTMetaColumn column = new AWTMetaColumn();
            column.init(table, key, label, columnMeta, useDirectActionBinding, formatters, useXMLFieldPath);
            table.registerColumn(column);
        }
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        _layoutBinding = (AWBinding)bindingsHashtable.remove(BindingNames.layout);
        _useXMLFieldPathBinding = (AWBinding)bindingsHashtable.remove(BindingNames.useXMLFieldPath);
        _sessionless = (AWBinding)bindingsHashtable.remove(BindingNames.sessionless);
        _formatters = (AWBinding)bindingsHashtable.remove(BindingNames.formatters);
        super.init(tagName, bindingsHashtable);
    }
}
