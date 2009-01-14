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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWGroupingRepetition.java#3 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.fieldvalue.OrderedList;

import ariba.util.fieldvalue.FieldValue;

public final class AWGroupingRepetition extends AWComponent
{
    private String GroupByKeyPathBinding = "groupByKeyPath";
    private String ItemKeyPathBinding = "itemKeyPath";
    private String ItemBinding = "item";
    Object _orderedList;
    String _groupByKeyPath;
    String _itemKeyPath;
    int _nextGroupIndex;
    int _orderedListCount;

    // ** Thread Safety Considerations: see AWComponent.

    protected void awake ()
    {
        // refresh all values for each cycle.
        _orderedList = valueForBinding(AWBindingNames.list);
        _orderedListCount = OrderedList.get(_orderedList).size(_orderedList);
        _groupByKeyPath = (String)valueForBinding(GroupByKeyPathBinding);
        _itemKeyPath = (String)valueForBinding(ItemKeyPathBinding);
        _nextGroupIndex = 0;
    }

    private void pushItemFromSublist (Object sublist)
    {
        AWBinding itemBinding = bindingForName(ItemBinding, true);
        if (itemBinding != null) {
            Object item = OrderedList.get(sublist).lastElement(sublist);
            if (_itemKeyPath != null) {
                item = FieldValue.getFieldValue(item, _itemKeyPath);
            }
            setValueForBinding(item, itemBinding);
        }
    }

    public Object nextSublist ()
    {
        Object sublist = null;
        int startIndex = _nextGroupIndex;
        if (startIndex < _orderedListCount) {
            OrderedList orderedListClassExtension = OrderedList.get(_orderedList);
            Object currentItem = orderedListClassExtension.elementAt(_orderedList, startIndex);
            Object groupingValue = FieldValue.getFieldValue(currentItem, _groupByKeyPath);
            int currentIndex = startIndex + 1;
            while (currentIndex < _orderedListCount) {
                Object currentElement = orderedListClassExtension.elementAt(_orderedList, currentIndex);
                Object currentValue = FieldValue.getFieldValue(currentElement, _groupByKeyPath);
                if ((groupingValue == currentValue) || ((groupingValue != null) && groupingValue.equals(currentValue))) {
                    currentIndex++;
                }
                else {
                    break;
                }
            }
            _nextGroupIndex = currentIndex;
            sublist = orderedListClassExtension.sublist(_orderedList, startIndex, _nextGroupIndex);
            // ** For convenience, push an element of the sublist into the parent.
            pushItemFromSublist(sublist);
        }
        return sublist;
    }
}
