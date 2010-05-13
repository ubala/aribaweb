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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTOptionsMenuItems.java#19 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Assert;

public final class AWTOptionsMenuItems extends AWComponent
{
    private final static int MaxOptionalColumns = 10;
    private AWTDataTable _table;

    protected void sleep ()
    {
        _table = null;
    }

    public void setTable (AWTDataTable table)
    {
        Assert.that(table!=null, "AWTOptionsMenuItems evaluated outside the scope of an AWTDataTable");
        _table = table;
    }

    public AWTDataTable table ()
    {
        return _table;
    }

    public boolean isExpandAllFalse ()
    {
        return !_table.expandAll();
    }

    public boolean isExpandAllTrue ()
    {
        return _table.expandAll();
    }

    public void expandAll ()
    {
        _table.outlineState().expandAll();
        _table.pushTableConfig();
    }

    public void collapseAll ()
    {
        _table.outlineState().collapseAll();
        _table.resetScrollTop();
        _table.pushTableConfig();
    }

    public void setExpandAllTrue ()
    {
        _table.setExpandAll(true);
    }

        /** Grouping... */
    public boolean isCurrentlyGrouping ()
    {
        return _table.displayGroup().groupingKey() != null;
    }

    public boolean isGroupExpandAllFalse ()
    {
        return !(_table.displayGroup().groupingExpansionDefault() == AWTDisplayGroup.GroupingDefaultAllOpen);
    }

    public boolean isGroupExpandAllTrue ()
    {
        return (_table.displayGroup().groupingExpansionDefault() == AWTDisplayGroup.GroupingDefaultAllOpen);
    }

    public void setGroupExpandAllFalse ()
    {
        AWTDisplayGroup displayGroup = _table.displayGroup();
        displayGroup.setGroupingExpansionDefault(AWTDisplayGroup.GroupingDefaultClosed);
        displayGroup.updateDisplayedObjects();
    }

    public void setGroupExpandAllTrue ()
    {
        AWTDisplayGroup displayGroup = _table.displayGroup();
        displayGroup.setGroupingExpansionDefault(AWTDisplayGroup.GroupingDefaultAllOpen);
        displayGroup.updateDisplayedObjects();
    }

    public void prepare ()
    {
        table()._currentColumn.prepare(_table);
    }

    public String currentColumnLabel ()
    {
        return table()._currentColumn.label(_table);
    }

    public boolean isCurrentColumnGroupable ()
    {
        return table()._currentColumn.isGroupableColumn(_table);
    }

    public boolean shouldDisplayMoreOptionalColumns ()
    {
        boolean displayMoreOptionalColumns = false;

        // if we have more than Max, always display more
        if (_table.optionalColumns().size() > MaxOptionalColumns) {
            displayMoreOptionalColumns = true; 
        }
        else if (_table.hasBinding(BindingNames.displayMoreOptionalColumns)) {        
            displayMoreOptionalColumns = 
                _table.booleanValueForBinding(BindingNames.displayMoreOptionalColumns);
        }
        else {
            displayMoreOptionalColumns  = false;
        }

        return displayMoreOptionalColumns;
    }

    public int optionalColumnsCount ()
    {
        return Math.min(_table.optionalColumns().size(), MaxOptionalColumns);
    }

    public AWComponent displayMoreOptionalColumns ()
    {
        AWTOptionalColumnsPage page =
            (AWTOptionalColumnsPage)pageWithName(AWTOptionalColumnsPage.Name);
        page.setTable(_table);
        return page;
    }

    public boolean disableHideColumn ()
    {
        return _table.displayedColumns().size() == 1 && _table.isCurrentColumnDisplayed();
    }

    public boolean showGroupBy ()
    {
        return booleanValueForBinding("showGroupBy", true) && _table.isGroupByAllowed();
    }
}
