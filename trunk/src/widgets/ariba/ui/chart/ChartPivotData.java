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

    $Id: //ariba/platform/ui/widgets/ariba/ui/chart/ChartPivotData.java#1 $
*/
package ariba.ui.chart;

import ariba.ui.table.AWTDataTable;
import ariba.ui.table.AWTPivotState;
import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.table.AWTDataTable.Column;
import ariba.ui.table.BindingNames;
import ariba.ui.table.AWTColumn;
import ariba.ui.aribaweb.core.AWComponent;

import java.util.List;
import java.util.Arrays;

public class ChartPivotData extends AWComponent
{
    public static List ChartTypes = Arrays.asList (
        "StackedArea2D", "StackedBar2D", "StackedColumn2D", "StackedColumn3D",
        "MSArea2D", "MSBar2D", "MSColumn2D", "MSColumn2DLineDY",
        "MSColumn3D", "MSColumn3DLineDY"
    /*
        "Candlestick",
        "Gantt"
     */
    );

    public AWTDisplayGroup _displayGroup;
    public AWTPivotState _pivotState;
    public AWTPivotState.EdgeCell _edgeCell;
    public Object _item;
    public List<AWTPivotState.PivotEdgeColumn> _columnAttributeColumns;
    public AWTPivotState.PivotEdgeColumn _attributeColumn;
    public int _index;
    
    protected void awake() {
        super.awake();
        _displayGroup = (AWTDisplayGroup)valueForBinding(BindingNames.displayGroup);
        AWTPivotState pivotState = _displayGroup != null ? _displayGroup.pivotState() : null;

        if (pivotState.rowFields().size() != 1 || pivotState.columnFields().size() != 1) return;

        String valueColumnName = stringValueForBinding("valueColumnName");
        List columnAttributes = pivotState._allColumnAttributes();
        if (columnAttributes.size() == 0) return;

        Column atttributeColumn = (valueColumnName != null)
                ? findColumnWithName(columnAttributes, valueColumnName)
                : (AWTDataTable.Column)columnAttributes.get(0);

        if (atttributeColumn == null) return;

        _columnAttributeColumns = pivotState.leafLevelColumnsFor(atttributeColumn);

        _pivotState = pivotState;
    }

    protected void sleep() {
        _displayGroup = null;
        _edgeCell = null;
        _pivotState = null;
        _item = null;
        _columnAttributeColumns = null;
        _attributeColumn = null;
        super.sleep();
    }

    Column findColumnWithName (List<Column> cols, String name)
    {
        for (Column c : cols) {
            if (c.matchesKey(name)) return c;
        }
        return null;
    }

    Object valueForColumn (AWTDataTable.Column column, Object object)
    {
        // BOGUS: only works for AWTColumn, with value fetchable via key (i.e. no tag content rendering)
        return ((AWTColumn)column).valueForRow(object);
    }

    public Object edgeCellValue ()
    {
        return valueForColumn(_edgeCell.column(), _edgeCell.object());
    }

    public Object columnFieldLabel ()
    {
        AWTPivotState.EdgeCell objectCell = _attributeColumn.edgeCell().objectCell();
        return valueForColumn(objectCell.column(), objectCell.object());
    }

    public Object rowAttributeValue ()
    {
        return valueForColumn((AWTDataTable.Column)_pivotState.rowFields().get(0), _item);
    }

    public Object itemValue ()
    {
        AWTPivotState.EdgeCell cell = _attributeColumn.edgeCell();
        AWTPivotState.PivotGroup group = (AWTPivotState.PivotGroup)_displayGroup.groupingState(_item);
        Object currentItem = cell.itemInGroup(group);
        return valueForColumn(cell.attributeColumn(), currentItem);
    }

    public String color()
    {
        return ChartData.colors[_index % ChartData.colors.length];
    }
}
