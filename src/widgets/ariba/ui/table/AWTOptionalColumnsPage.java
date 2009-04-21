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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTOptionalColumnsPage.java#3 $
*/

package ariba.ui.table;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.table.AWTDynamicColumns.DynamicColumn;
import ariba.ui.widgets.ModalPageWrapper;
import ariba.util.core.ListUtil;
import java.util.List;


public class AWTOptionalColumnsPage extends AWComponent
{
    public static final String Name = AWTOptionalColumnsPage.class.getName();

    private AWTDataTable.Column _currentColumn;
    public AWTDataTable _table;
    private AWTDisplayGroup _displayGroup;

    public void setTable (AWTDataTable table)
    {
        _table = table;
        AWTDisplayGroup displayGroup = displayGroup();

        List optionalColumns = _table.optionalColumns();
        displayGroup.setObjectArray(optionalColumns);

        List currentDisplayedColumns = _table.displayedColumns();
        List selectedDisplayColumns = ListUtil.list();
        int optionalColumnsSize = optionalColumns.size();

        for (int i = 0; i < optionalColumnsSize; i++) {
            Object optionalColumn = optionalColumns.get(i);
            boolean isCurrentDisplayed =
                ListUtil.containsIdentical(currentDisplayedColumns, optionalColumn);
            if (isCurrentDisplayed) {
                selectedDisplayColumns.add(optionalColumn);
            }
        }
        displayGroup.setSelectedObjects(selectedDisplayColumns);
    }

    public AWTDisplayGroup displayGroup ()
    {
        if (_displayGroup == null) {
            _displayGroup = new AWTDisplayGroup();
        }
        return _displayGroup;
    }

    public AWTDataTable.Column getCurrentColumn ()
    {
        return _currentColumn;
    }

    public void setCurrentColumn (AWTDataTable.Column column)
    {
        if (column instanceof DynamicColumn) {
            ((DynamicColumn)column).prepare(_table);
        }
        _currentColumn = column;
    }

    public String currentColumnLabel ()
    {
        return _currentColumn.label(_table);
    }

    public AWComponent toggleColumns ()
    {
        _table.setColumnVisibilities(_displayGroup.selectedObjects());
        return ModalPageWrapper.returnPage(this);
    }

}
