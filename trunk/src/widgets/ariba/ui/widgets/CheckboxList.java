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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/CheckboxList.java#3 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.fieldvalue.OrderedList;
import ariba.ui.aribaweb.util.AWUtil;

import java.util.List;

public final class CheckboxList extends AWComponent
{
    public AWEncodedString _wrapperElementId;
    public Object _currentItem;
    public Integer _currentIndex;
    private Object _itemList;
    private Object _selections;

    protected void awake ()
    {
        _itemList = null;
        _selections = null;
    }

    public Object itemList ()
    {
        if (_itemList == null) {
            _itemList = valueForBinding(AWBindingNames.list);
        }
        return _itemList;
    }

    public void setCurrentItem (Object currentItem)
    {
        _currentItem = currentItem;
        setValueForBinding(currentItem, AWBindingNames.item);
    }

    public void setCurrentIndex (Integer currentIndex)
    {
        _currentIndex = currentIndex;
        setValueForBinding(currentIndex, AWBindingNames.index);
    }

    public Object selections ()
    {
        if (_selections == null) {
            _selections = valueForBinding(AWBindingNames.selections);
        }
        return _selections;
    }

    public String isCurrentItemSelected ()
    {
        String isCurrentItemSelected = null;
        Object selectionsList = selections();
        if ((selectionsList != null) && (OrderedList.get(selectionsList).contains(selectionsList, _currentItem))) {
            isCurrentItemSelected = AWBindingNames.awstandalone;
        }
        return isCurrentItemSelected;
    }
    
    public void setFormValues (String[] selectedIndexes)
    {
        Object itemList = itemList();
        OrderedList itemListClassExtension = OrderedList.get(itemList);
        Object resultList = itemListClassExtension.mutableInstance(itemList);
        OrderedList resultListClassExtension = OrderedList.get(resultList);
        int selectedIndexCount = selectedIndexes.length;
        for (int index = 0; index < selectedIndexCount; index++) {
            String selectedIndexString = selectedIndexes[index];
            int selectedIndex = Integer.parseInt(selectedIndexString);
            Object object = itemListClassExtension.elementAt(itemList, selectedIndex);
            resultListClassExtension.addElement(resultList, object);
        }
        if (itemList instanceof Object[]) {
            resultList = AWUtil.getArray((List)resultList, itemList.getClass().getComponentType());
        }
        setValueForBinding(resultList, AWBindingNames.selections);
    }
}
