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

    $Id: //ariba/platform/ui/opensourceui/examples/Demo/gallery/html/DragDropExamples.java#1 $
*/
package gallery.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.table.AWTDisplayGroup;
import ariba.util.core.ListUtil;
import ariba.util.core.Fmt;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class DragDropExamples extends AWComponent
{
    private static final String[] PalleteButtonLabels = {"A","B","C","D"};
    private static final String[] RightButtonLabels = {"E","F"};
    private static final String[] TopPopupLabels = {"popup A", "popup B", "popup C"};
    private static final String Parent  = "Parent";
    private static final String Value   = "Value";
    private static final String SubList = "SubList";

    public AWTDisplayGroup[] _displayGroups;

    public List _topButtonList;
    public List _leftButtonList;
    public List _rightButtonList;
    public Map _topButtonItem;
    public Map _leftButtonItem;
    public Map _rightButtonItem;

    public List _topPopupList;
    public Map _topPopupItem;

    public Map _currentTableItem;

    public boolean isStateless ()
    {
        return false;
    }

    /////////////
    // Awake
    /////////////
    protected void awake ()
    {
        // note that we need to clear _currentDragSubItem
        _currentDragSubItem = null;
    }

    private Map createElement (String id)
    {
        return createElement(id,null);
    }

    private Map createElement (String id, Map parent)
    {
        Map map = new HashMap(1);
        map.put(Value, id);
        if (parent != null) {
            map.put(Parent, parent);
        }
        return map;
    }

    public void init ()
    {

        if (_displayGroups == null) {

            _displayGroups = new AWTDisplayGroup[4];

            // Display Group 0
            List dataList = ListUtil.list();
            for (int i=0; i < 5; i++) {
                Map element = createElement(Fmt.S("DataElement %s",String.valueOf(i)));
                dataList.add(element);
            }
            _displayGroups[0] = new AWTDisplayGroup();
            _displayGroups[0].setSortOrderings(ListUtil.list());
            _displayGroups[0].setObjectArray(dataList);

            // Display Group 1
            dataList = ListUtil.list();
            for (int i=5; i < 10; i++) {
                Map element = createElement(Fmt.S("DataElement %s",String.valueOf(i)));
                dataList.add(element);
            }
            _displayGroups[1] = new AWTDisplayGroup();
            _displayGroups[1].setSortOrderings(ListUtil.list());
            _displayGroups[1].setObjectArray(dataList);

            // Display Group 2 -- test empty drop
            _displayGroups[2] = new AWTDisplayGroup();

            // Display Group 3
            dataList = ListUtil.list();
            for (int i=10; i < 15; i++) {
                Map element = createElement(Fmt.S("DataElement %s",String.valueOf(i)));
                dataList.add(element);
            }
            _displayGroups[3] = new AWTDisplayGroup();
            _displayGroups[3].setSortOrderings(ListUtil.list());
            _displayGroups[3].setObjectArray(dataList);

        }

        if (_topButtonList == null) {
            _topButtonList = ListUtil.list();
            _leftButtonList = ListUtil.list();
            _rightButtonList = ListUtil.list();

            for (int i=0; i < 4; i++) {
                _leftButtonList.add(createElement(String.valueOf(i)));
            }
            for (int i=0; i < RightButtonLabels.length; i++) {
                _rightButtonList.add(createElement(RightButtonLabels[i]));
            }
            for (int i=0; i < PalleteButtonLabels.length; i++) {
                _topButtonList.add(createElement(PalleteButtonLabels[i]));
            }
        }

        if (_topPopupList == null) {
            _topPopupList = ListUtil.list();

            for (int i=0; i < TopPopupLabels.length; i++) {
                _topPopupList.add(createElement(TopPopupLabels[i]));
            }
        }
    }

    public AWComponent handleDrag ()
    {
        String dragSource = request().formValueForKey("awds");
        String dragTarget = request().formValueForKey("awdt");

        System.out.println("----> received drag from: " + dragSource + " to: " + dragTarget);

//        int sourceIndex = Integer.parseInt(request().formValueForKey("awds"));
//        int targetIndex = Integer.parseInt(request().formValueForKey("awdt"));
//        System.out.println("----> received move for : " + sourceIndex +
//                           " to: " + targetIndex);

//        List data = _data.allObjects();
//        int batchAdjust = _data.numberOfObjectsPerBatch() * (_data.currentBatchIndex()-1);
//        sourceIndex += batchAdjust - 1;
//        targetIndex += batchAdjust - 1 - ((sourceIndex < targetIndex) ? 1:0);
//
//        Object obj = data.remove(sourceIndex);
//        data.add(targetIndex,obj);
//
//        _data.setObjectArray(data);
        return null;
    }


    public void setCurrTableIndex (String index)
    {
        _currTableIndex = Integer.parseInt(index);
    }

    // ### todo: mdao - should support $displayGroup[0]
    public AWTDisplayGroup getDisplayGroup_0 ()
    {
        return _displayGroups[0];
    }
    public AWTDisplayGroup getDisplayGroup_1 ()
    {
        return _displayGroups[1];
    }
    public AWTDisplayGroup getDisplayGroup_2 ()
    {
        return _displayGroups[2];
    }
    public AWTDisplayGroup getDisplayGroup_3 ()
    {
        return _displayGroups[3];
    }

    //*****************************************************
    // Button / Popup Actions
    //*****************************************************

        //***************
        // button set up
        //***************
    public String topButtonLabel ()
    {
        return (String)_topButtonItem.get(Value);
    }

    public String leftButtonLabel ()
    {
        return (String)_leftButtonItem.get(Value);
    }

    public String rightButtonLabel ()
    {
        return (String)_rightButtonItem.get(Value);
    }

        //***************
        // pop up set up
        //***************

    public String _popupMenuSelection = "none";

    public String topPopupLabel ()
    {
        return (String)_topPopupItem.get(Value);
    }

    public void topPopupMenuSetupAction ()
    {
        _popupMenuSelection = "button " + (String)_topPopupItem.get(Value);
    }
    public void popupMenuAction1 ()
    {
        _popupMenuSelection += " action 1";
    }
    public void popupMenuAction2 ()
    {
        _popupMenuSelection += " action 2";
    }

        //***************
        // drag handlers
        //***************
    private Map _currentDragItem;
    private List _currentDragList;

    public void topDragPopupAction ()
    {
        _currentDragItem = _topPopupItem;
        _currentDragList = _topPopupList;
    }

    public void topDragButtonAction ()
    {
        _currentDragItem = _topButtonItem;
        _currentDragList = _topButtonList;
    }

    public void leftDragButtonAction ()
    {
        _currentDragItem = _leftButtonItem;
        _currentDragList = _leftButtonList;
    }

    public void rightDragButtonAction ()
    {
        _currentDragItem = _rightButtonItem;
        _currentDragList = _rightButtonList;
    }

    private AWTDisplayGroup _currentDragDisplayGroup;
    private Map _currentDragDisplayGroupItem;
    private Map _currentDragSubItem;

    public int _currTableIndex;
    public void tableDragAction ()
    {
        System.out.println(Fmt.S("--- Table %s drag", _currTableIndex));
        _currentDragDisplayGroup = _displayGroups[_currTableIndex];
        _currentDragDisplayGroupItem = _currentTableItem;
    }

        //***************
        // drop handlers
        //***************

    public String _actionType;
    public AWComponent buttonClicked ()
    {
        _actionType = "button clicked";
        return null;
    }

    public AWComponent topDropButtonWrapperAction ()
    {
        appendItem(_topButtonList);
        return null;
    }

    public AWComponent topDropButtonAction ()
    {
        int index = _topButtonList.indexOf(_topButtonItem);
        moveItem(_topButtonList,index);
        return null;
    }

    public AWComponent leftDropButtonWrapperAction ()
    {
        appendItem(_leftButtonList);
        return null;
    }

    public AWComponent leftDropButtonAction ()
    {
        int index = _leftButtonList.indexOf(_leftButtonItem);
        moveItem(_leftButtonList,index);
        return null;
    }

    public AWComponent rightDropButtonWrapperAction ()
    {
        appendItem(_rightButtonList);
        return null;
    }

    public AWComponent rightDropButtonAction ()
    {
        int index = _rightButtonList.indexOf(_rightButtonItem);
        moveItem(_rightButtonList, index);
        return null;
    }

    public AWComponent topDropPopupWrapperAction ()
    {
        appendItem(_topPopupList);
        return null;
    }

    public AWComponent topDropPopupAction ()
    {
        int index = _topPopupList.indexOf(_topPopupItem);
        moveItem(_topPopupList, index);
        return null;
    }

    private void appendItem (List targetList)
    {
        _actionType = "drag/drop";

        _currentDragList.remove(_currentDragItem);
        targetList.add(_currentDragItem);

        _currentDragItem = null;
        _currentDragList = null;
    }

    private void moveItem (List targetList, int index)
    {
        _actionType = "drag/drop";

        if (targetList == _currentDragList) {
            if (targetList.indexOf(_currentDragItem) < index) {
                index--;
            }
        }
        _currentDragList.remove(_currentDragItem);
        targetList.add(index, _currentDragItem);

        _currentDragItem = null;
        _currentDragList = null;
    }

    private void moveTableItem (AWTDisplayGroup from, AWTDisplayGroup to,
                                Map fromItem, Map toItem, Map subFromItem, Map subToItem)
    {

        Map source;
        int targetIndex = 0;
        List toList;
        if (subFromItem != null) {
            if (subToItem != null) {
                // from sublist to sublist
                toList = (List) toItem.get(SubList);
                // do accounting
                int sourceIndex = ((List)fromItem.get(SubList)).indexOf(subFromItem);
                targetIndex = toList.indexOf(subToItem);
                if (subFromItem.get(Parent) == subToItem.get(Parent)) {
                    targetIndex -= ((sourceIndex < targetIndex) ? 1:0);
                }
            }
            else {
                // from sublist to mainlist
                toList = to.allObjects();
                targetIndex = toList.indexOf(toItem);
            }

            // remove the sub item from it's parent's list
            ((List)fromItem.get(SubList)).remove(subFromItem);
            source = subFromItem;
        }
        else {
            List fromList = from.allObjects();

            if (subToItem != null) {
                // from mainlist to sublist
                toList = (List) toItem.get(SubList);
                targetIndex = toList.indexOf(subToItem);
            }
            else {
                // from mainlist to mainlist -- note length check accounts for empty
                // target list (targetIndex defaults to 0)
                toList = to.allObjects();
                if (toList.size() > 0) {
                    targetIndex = toList.indexOf(toItem);
                }
                int sourceIndex = fromList.indexOf(fromItem);
                if (fromList == toList) {
                    targetIndex -= ((sourceIndex < targetIndex) ? 1:0);
                }
            }

            fromList.remove(fromItem);
            if (fromList != toList) {
                from.setObjectArray(fromList);
            }
            source = fromItem;
        }

        if (subToItem != null) {
            // to sublist
            toList.add(targetIndex,source);
        }
        else {
            toList.add(targetIndex, source);
            to.setObjectArray(toList);
        }
    }

    public AWComponent tableDropAction ()
    {
        System.out.println(Fmt.S("--- table %s drop", _currTableIndex));
        moveTableItem(_currentDragDisplayGroup, _displayGroups[_currTableIndex],
                      _currentDragDisplayGroupItem, _currentTableItem,
                      _currentDragSubItem, _subTableItem);
        return null;
    }

    public Map _subTableItem;
    public AWComponent tableReorderAction ()
    {
        System.out.println(Fmt.S("--- table %s reorder", _currTableIndex));
        return null;
    }

    private List getSubList (Map item)
    {
        List subList = (List)item.get(SubList);
        if (subList == null) {
            String currentValue = (String)item.get(Value);
            subList = ListUtil.list();
            for (int i=0; i < 3; i++) {
                Map element = createElement(currentValue+":"+String.valueOf(i), item);
                subList.add(element);
            }
            item.put(SubList, subList);
        }
        return subList;
    }

    public List getSubTableList ()
    {
        return getSubList(_currentTableItem);
    }

    public void subTableDragAction ()
    {
        _currentDragDisplayGroup = null;
        _currentDragDisplayGroupItem = _currentTableItem;
        _currentDragSubItem = _subTableItem;
    }

    public List topList = ListUtil.list(new Object(), new Object());
    public List bottomList = ListUtil.list(new Object(), new Object());
    public Object item;
    private Object draggedItem;

    public void dragAction ()
    {
        draggedItem = item;
    }

    public AWComponent dropToTopAction ()
    {
        ListUtil.addElementIfAbsent(topList, draggedItem);
        bottomList.remove(draggedItem);
        return null;
    }

    public AWComponent dropToBottomAction ()
    {
        ListUtil.addElementIfAbsent(bottomList, draggedItem);
        topList.remove(draggedItem);
        return null;
    }

}
