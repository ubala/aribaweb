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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaTableDetailColumnRenderer.java#1 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.table.AWTDataTable;
import ariba.ui.table.AWTRowDetail;

import java.util.List;

public class MetaTableDetailColumnRenderer extends AWTDataTable.ColumnRenderer
{
    public static class Column extends AWTDataTable.Column implements AWTDataTable.DetailColumn
    {
        List<String> _fields;
        boolean _renderBeforeRow;
        boolean _showRowLine;

        Column (List<String> fields, boolean renderBeforeRow)
        {
            _fields = fields;
            _renderBeforeRow = renderBeforeRow;
        }

        public String rendererComponentName ()
        {
            return MetaTableDetailColumnRenderer.class.getName();
        }

        public boolean renderBeforeRow (AWTDataTable table)
        {
            return _renderBeforeRow;
        }
    }

    public String _fieldName;
    
    public List<String>fields ()
    {
        return ((Column)column())._fields;
    }

    public String rowClass ()
    {
        Column column = (Column)column();
        if (!column._showRowLine) _table.skipTopLine();
        return _table.rowClass();
    }
    
    public String tdClass ()
    {
        Column column = (Column)column();
        return _table.tdClass(!column._showRowLine || ((Column)column())._renderBeforeRow);
    }

    public int visibleLeadingCols ()
    {
        return _table.colsBeforeData() - (_table.hasInvisibleSelectionColumn() ? 1 : 0);
    }
}
