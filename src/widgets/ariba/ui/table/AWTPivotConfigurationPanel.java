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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTPivotConfigurationPanel.java#6 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.widgets.ModalPageWrapper;
import ariba.util.core.ListUtil;

import java.util.List;

public class AWTPivotConfigurationPanel extends AWComponent
{
    // Types:
    //      RowField, ColumnField, ColumnAttribute, DetailAttribute
    //      Any, None (can't move), Current (Attr to Attr, Field to Field)
    public static final List PivotListColumnField
            = ListUtil.list("Any", "Field", "ColumnField");
    public static final List PivotListRowField
            = ListUtil.list("Any", "Field", "RowField");
    public static final List PivotListColumnAttribute
            = ListUtil.list("Any", "Attribute", "ColumnAttribute");
    public static final List PivotListDetailAttribute
            = ListUtil.list("Any", "Attribute", "DetailAttribute");
    public static final List PivotListHiddenAttribute
            = ListUtil.list("Any", "Attribute", "DetailAttribute", "ColumnAttribute");

    AWTPivotState _pivotState;
    Object[] _lists;
    public List _currentList;
    public int _currentListNumber;
    public Object _currentItem;
    public List _dragList;
    public Object _dragItem;
    protected boolean _didChange = false;

    public void setPivotState (AWTPivotState state)
    {
        _pivotState = state;
        _lists = _pivotState.editableConfig();
    }

    public AWComponent okClicked ()
    {
        if (_didChange) setPivotState(_pivotState.updateWithEditableConfig(_lists));
        return ModalPageWrapper.instance(this).returnPage();
    }

    public boolean isStateless()
    {
        return false;
    }

    public void setListNumber (String num)
    {
        _currentListNumber = Integer.parseInt(num);
        _currentList = (List)_lists[_currentListNumber];
    }

    public void prepare ()
    {
        ((AWTDataTable.Column)_currentItem).prepare(_pivotState.table());
    }

    public String currentItemLabel ()
    {
        return ((AWTDataTable.Column)_currentItem).label(_pivotState.table());
    }

    public String currentItemType ()
    {
        String type = ((AWTDataTable.Column)_currentItem).pivotMoveType(_pivotState.table());
        return (type == null || type.equals("Current"))
                ? ((_currentListNumber <= 1) ? "Field" : "Attribute")
                : (type.equals("None") ? null : type);
    }

    public void dragField ()
    {
        _dragList = _currentList;
        _dragItem = _currentItem;
    }

    public void dropField ()
    {
        if (_currentItem == null) {
            _dragList.remove(_dragItem);
            _currentList.add(_dragItem);
        } else {
            int pos = _currentList.indexOf(_currentItem);
            boolean removeFirst = (_currentList == _dragList) && (pos < _currentList.indexOf(_dragItem));
            if (removeFirst) _dragList.remove(_dragItem);
            _currentList.add(pos, _dragItem);
            if (!removeFirst) _dragList.remove(_dragItem);
        }
        _didChange = true;
    }

    public String itemClass ()
    {
        return (currentItemType() == null) ? "disabled" : null;
    }
}
