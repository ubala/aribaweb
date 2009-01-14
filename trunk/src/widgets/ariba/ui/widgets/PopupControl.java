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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PopupControl.java#3 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWAction;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.core.AWRefreshRegion;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.html.BindingNames;
import ariba.ui.aribaweb.html.AWPopup;

public class PopupControl extends AWComponent
{
    public Object _menuId;
    public AWEncodedString _noSelectionString;
    public Integer _currentIndex;
    public AWAction _currentAction;
    public Object _orderedList;
    public AWAction[] _actionList;
    private AWBinding _actionBinding;
    private Object _currentItem;
    public Object _selectedItem;
    public boolean _disabled;

    protected void awake ()
    {
        _noSelectionString = encodedStringValueForBinding(BindingNames.noSelectionString);
        _selectedItem = valueForBinding(BindingNames.selection);
        _orderedList = valueForBinding(BindingNames.list);
        _actionList = AWPopup.initActionList(componentReference());
        _disabled = booleanValueForBinding(BindingNames.disabled) ||
            AWEditableRegion.disabled(requestContext());
        _actionBinding = bindingForName(BindingNames.action);
        if (_actionBinding == null) {
            _actionBinding = AWRefreshRegion.NullActionBinding;
        }
    }

    protected void sleep ()
    {
        _noSelectionString = null;
        _selectedItem = null;
        _orderedList = null;
        _actionList = null;
        _disabled = false;
        _actionBinding = null;
        _currentIndex = null;
        _currentAction = null;
        _currentItem = null;
        _menuId = null;
    }

    public void setCurrentItem (Object object)
    {
        _currentItem = object;
        setValueForBinding(_currentItem, BindingNames.item);
    }

    public void setCurrentIndex (Integer integer)
    {
        _currentIndex = integer;
        setValueForBinding(integer, BindingNames.index);
    }

    public String isCurrentItemSelected ()
    {
        if (_currentItem == null) {
            return null;
        }
        return _currentItem.equals(_selectedItem) ? BindingNames.awstandalone : null;
    }

    protected void setSelection (Object selection)
    {
        setValueForBinding(selection, BindingNames.selection);
    }

    public AWResponseGenerating itemClickedAction ()
    {
        setSelection(_currentItem);
        return (AWResponseGenerating)valueForBinding(_actionBinding);
    }

    public AWResponseGenerating actionClickedAction ()
    {
        return (AWResponseGenerating)valueForBinding(_currentAction._action);
    }

    public String buttonClass ()
    {
        return (_disabled) ? "popupMenuButton disabled" : "popupMenuButton";
    }
}
