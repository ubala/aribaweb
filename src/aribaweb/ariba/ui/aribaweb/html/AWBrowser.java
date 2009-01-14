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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWBrowser.java#12 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWXBasicScriptFunctions;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.fieldvalue.OrderedList;

public final class AWBrowser extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.list, BindingNames.selections, BindingNames.multiple, BindingNames.item,
        BindingNames.index, BindingNames.noSelectionString, BindingNames.size, BindingNames.name,
        BindingNames.onChange, BindingNames.isRefresh, BindingNames.isItemSelected, BindingNames.action,
    };
    public static final String NoSelectionString = "no";
    private Object _currentItem;
    public int _currentIndex;
    private Object _selections;
    private AWBinding _itemSelectedBinding;
    private boolean _itemSelectedBindingHasBeenCached;
    private Object _orderedList;
    private OrderedList _orderedListClassExtension;
    private OrderedList _selectionsListClassExtension;
    public AWEncodedString _elementId;

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void sleep ()
    {
        _selections = null;
        _itemSelectedBinding = null;
        _itemSelectedBindingHasBeenCached = false;
        _orderedList = null;
        _orderedListClassExtension = null;
        _selectionsListClassExtension = null;
        _currentItem = null;
        _currentIndex = 0;
        _elementId = null;
    }

    public void setCurrentItem (Object object)
    {
        _currentItem = object;
        setValueForBinding(_currentItem, BindingNames.item);
    }

    public Object currentItem ()
    {
        return _currentItem;
    }

    public void setCurrentIndex (int intValue)
    {
        _currentIndex = intValue;
        setValueForBinding(_currentIndex, BindingNames.index);
    }

    public Object orderedList ()
    {
        if (_orderedList == null) {
            _orderedList = valueForBinding(BindingNames.list);
            if (_orderedList != null) {
                _orderedListClassExtension = OrderedList.get(_orderedList);
            }
        }
        return _orderedList;
    }

    public void setSelectedIndexArray (String[] optionIndexArray)
    {
        // ** This is called once during applyValues phase.
        Object orderedList = orderedList();
        Object resultList = _orderedListClassExtension.mutableInstance(orderedList);
        OrderedList resultListClassExtension = OrderedList.get(resultList);
        int optionIndexCount = optionIndexArray.length;
        if (optionIndexCount > 0) {
            for (int index = 0; index < optionIndexCount; index++) {
                String optionIndexString = optionIndexArray[index];
                if (!optionIndexString.equals(NoSelectionString)) {
                    int optionIndex = Integer.parseInt(optionIndexString);
                    Object object = _orderedListClassExtension.elementAt(orderedList, optionIndex);
                    resultListClassExtension.addElement(resultList, object);
                }
            }
        }
        setValueForBinding(resultList, BindingNames.selections);
    }

    public Object selections ()
    {
        if (_selections == null) {
            _selections = valueForBinding(BindingNames.selections);
            if (_selections != null) {
                _selectionsListClassExtension = OrderedList.get(_selections);
            }
        }
        return _selections;
    }

    public String isCurrentItemSelected ()
    {
        String isCurrentItemSelected = null;
        // If we have a itemSelectedBinding, let that define
        // if this item is selected.
        AWBinding itemSelectedBinding = itemSelectedBinding();
        if (itemSelectedBinding != null) {
            if (booleanValueForBinding(itemSelectedBinding)) {
                isCurrentItemSelected = BindingNames.awstandalone;
            }
        }
        else {
            Object selectionsList = selections();
            if ((selectionsList != null) && (_selectionsListClassExtension.contains(selectionsList, _currentItem))) {
                // ** awstandalone is a keyword which results in only the attribute
                // ** name being rendered into the html (feature of AWGenericElement).
                isCurrentItemSelected = BindingNames.awstandalone;
            }
        }
        return isCurrentItemSelected;
    }

    private AWBinding itemSelectedBinding ()
    {
        if (!_itemSelectedBindingHasBeenCached) {
            _itemSelectedBindingHasBeenCached = true;
            _itemSelectedBinding = bindingForName(BindingNames.isItemSelected);
        }
        return _itemSelectedBinding;
    }

    public String isMultiple ()
    {
        String awstandaloneString = null;
        boolean isMultiple = booleanValueForBinding(BindingNames.multiple);
        if (isMultiple) {
            awstandaloneString = BindingNames.awstandalone;
        }
        return awstandaloneString;
    }

    public String onChangeString ()
    {
        String onChangeString = null;
        if (bindingForName(BindingNames.action) != null || booleanValueForBinding(BindingNames.isRefresh)) {
            AWRequestContext requestContext = requestContext();
            AWResponse response = requestContext.response();
            response.appendContent(AWConstants.Space);
            response.appendContent(AWConstants.OnChange);
            response.appendContent(AWConstants.Equals);
            response.appendContent(AWConstants.Quote);
            AWXBasicScriptFunctions.appendSubmitCurrentForm(requestContext, _elementId);
            response.appendContent(AWConstants.Quote);
        }
        else {
            onChangeString = stringValueForBinding(BindingNames.onChange);
        }
        return onChangeString;
    }

    public boolean isHiddenFieldSender ()
    {
        return AWGenericActionTag.isHiddenFieldSender(request(), _elementId);
    }

    // recording & playback
    // provide a better semantic key
    protected AWBinding _debugPrimaryBinding ()
    {
        return componentReference().bindingForName(BindingNames.list, parent());
    }

}
