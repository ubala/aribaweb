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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVPopup.java#10 $
*/
package ariba.ui.validation;

import ariba.ui.aribaweb.html.AWPopup;
import ariba.ui.aribaweb.html.BindingNames;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWErrorManager;

public final class AWVPopup extends AWPopup
{
    private Object _errorKey;
    private Object _errorSelection;
    private Object _currentItem;
    private Object _selectedItem;

    protected void awake ()
    {
        _errorSelection = UndefinedObject;
        _selectedItem = valueForBinding(AWBindingNames.selection);
    }

    protected void sleep ()
    {
        _errorKey = null;
        _errorSelection = null;
        _currentItem = null;
        _selectedItem = null;
    }

    public void setCurrentItem (Object object)
    {
        _currentItem = object;
        super.setCurrentItem(object);
    }

    private Object errorSelection ()
    {
        if (_errorSelection == UndefinedObject) {
            AWErrorManager errorManager = errorManager();
            Object errorKey = errorKey();
            _errorSelection = errorManager.errantValueForKey(errorKey);
        }
        return _errorSelection;
    }

    public String isCurrentItemSelected ()
    {
        String isSelectedString = null;
        Object errorSelection = errorSelection();
        if ((errorSelection != null && errorSelection.equals(_currentItem)) || _currentItem.equals(_selectedItem)) {
            isSelectedString = AWBindingNames.awstandalone;
        }
        return isSelectedString;
    }

    protected void setSelection (Object selection)
    {
        AWVValidator validator = (AWVValidator)valueForBinding(BindingNames.validator);
        if (validator != null) {
            String errorMessage = validator.validateObject(this, selection);
            if (errorMessage != null) {
                recordValidationError(errorKey(), errorMessage, selection);
            }
            else {
                super.setSelection(selection);
            }
        }
        else {
            super.setSelection(selection);
        }
    }

    private Object errorKey ()
    {
        if (_errorKey == null) {
            _errorKey = AWErrorManager.getErrorKeyForComponent(this);
            if (_errorKey == null) _errorKey = bindingForName(AWBindingNames.list, false).effectiveKeyPathInComponent(this);
        }
        return _errorKey;
    }
}
