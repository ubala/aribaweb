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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaTableColumns.java#5 $
*/
package ariba.ui.meta.layouts;

import java.util.List;
import java.util.Map;

import ariba.ui.table.AWTDataTable;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.util.core.ListUtil;

/**
 * This class is used inside an AWTDataTable to cause meta-data-specified
 *  columns to be inserted in its place in the table.
 * e.g.    <AWTDataTable>
 *              <AWTColumn> some normal column </AWTColumn>
 *
 *              <MetaTableColumns/>     ... columns from meta data inserted here
 *
 *              <AWTColumn> some other column </AWTColumn>
 *         </AWTDataTable>
 *
 * This class exists primarily for SET UP.  The Bulk of the rendering functionality
 * is in the MetaTableColumn class
 */
public final class MetaTableColumns extends AWTDataTable.Column
{
    public String rendererComponentName ()
    {
        return MetaTableColumnsRenderer.class.getName();
    }

    public void initializeColumn (AWTDataTable table)
    {
        // set this column as a "wrapper column"
        table.setWrapperColumn(this);

        UIMeta.UIContext context = MetaContext.currentContext(table);
        // get cached field list
        context.push();
        context.setScopeKey(UIMeta.KeyClass);
        Map<String, List> fieldsByZone = (Map)context.propertyForKey(UIMeta.PropFieldsByZone);

        for (String fieldName : context.uiMeta().flattenVisible(fieldsByZone, UIMeta.ZonesMTLRB,
                UIMeta.KeyField, context)) {
            // evaluate visibility (without object in context)
            ariba.ui.meta.layouts.MetaTableColumn column = new ariba.ui.meta.layouts.MetaTableColumn();
            column.init(table, fieldName);
            table.registerColumn(column);
        }

        // detail columns
        List<String>visibleFields = context.uiMeta().flattenVisible(fieldsByZone, UIMeta.ZonesDetail,
                UIMeta.KeyField, context);
        if (!ListUtil.nullOrEmptyList(visibleFields)) {
            table.setDetailColumn(new MetaTableDetailColumnRenderer.Column(visibleFields, false));
        }

        context.pop();
    }
}
