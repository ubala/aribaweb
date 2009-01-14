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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWCheckbox.java#22 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWXBasicScriptFunctions;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Constants;

// subclassed by SUXCheckbox in Supplier app
public class AWCheckbox extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.type, BindingNames.value, BindingNames.name, BindingNames.checked,
        BindingNames.onClick, BindingNames.isRefresh, BindingNames.action, BindingNames.disabled,
        BindingNames.awname, BindingNames.isExternal
    };
    public boolean _isDisabled;

    // ** Thread Safety Considerations: see AWComponent.

    protected void awake ()
    {
        // if disabled == null equivalent to disabled == false
        String isDisabled = stringValueForBinding(BindingNames.disabled);
        _isDisabled = (isDisabled != null &&
                        (BindingNames.awstandalone.equals(isDisabled) ||
                          booleanValueForBinding(BindingNames.disabled)) ||
                            AWEditableRegion.disabled(requestContext()));
    }

    protected void sleep ()
    {
        _isDisabled = false;
    }

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    private void unrecognizedType (String type)
    {
        throw new AWGenericException(getClass().getName() + ": unrecognized type: \"" + type + "\"");
    }

    //
    // Action checkbox
    //
    public boolean hasAction ()
    {
        return hasBinding(BindingNames.action);
    }

    public boolean getChecked ()
    {
        if (hasBinding(BindingNames.checked)) {
            return booleanValueForBinding(BindingNames.checked);
        }
        else if (hasBinding(BindingNames.value)) {
            String type = (String)valueForBinding(BindingNames.type);
            if ((type == null) || type.equals(BindingNames.booleanType)) {
                return booleanValueForBinding(BindingNames.value);
            }
            else if (type.equals(BindingNames.intType)) {
                return intValueForBinding(BindingNames.value) != 0;
            }
            else if (type.equals(BindingNames.NumberType)) {
                Number numberValue = (Number)valueForBinding(BindingNames.value);
                return ((numberValue != null) && (numberValue.doubleValue() != 0.0));
            }
            else {
                unrecognizedType(type);
            }
        }
        else {
            throw new AWGenericException("Invalid use of AWCheckbox.  When action / isRefresh bindings is used, the checked binding (or the value binding for backward compatibility) must be assigned.");
        }
        return false;
    }

    public void setChecked (boolean flag)
    {
        if (hasBinding(BindingNames.checked)) {
            setValueForBinding(flag, BindingNames.checked);
        }
        else if (hasBinding(BindingNames.value)) {
            setValueForBinding(flag, BindingNames.value);
        }
        else {
            throw new AWGenericException("Invalid use of AWCheckbox.  When action / isRefresh bindings is used, the checked binding (or the value binding for backward compatibility) must be assigned.");
        }
    }

    //
    // Form checkbox
    //
    public void setFormValue (String formValue)
    {
        if (_isDisabled)
            return;

        String type = (String)valueForBinding(BindingNames.type);
        boolean booleanValue = ((formValue == null) || (formValue.length() == 0)) ? false : true;
        if ((type == null) || (type == BindingNames.booleanType) || type.equals(BindingNames.booleanType)) {
            setChecked(booleanValue);
        }
        else if ((type == BindingNames.intType) || type.equals(BindingNames.intType)) {
            int intValue = booleanValue ? 1 : 0;
            setValueForBinding(intValue, BindingNames.value);
        }
        else if ((type == BindingNames.NumberType) || type.equals(BindingNames.NumberType)) {
            Integer integerValue = booleanValue ? Constants.getInteger(1) : Constants.getInteger(0);
            setValueForBinding(integerValue, BindingNames.value);
        }
        else {
            unrecognizedType(type);
        }
    }

    public String checkedString ()
    {
        return getChecked() ? BindingNames.awstandalone : null;
    }

    public String disabledString ()
    {
        return _isDisabled ? BindingNames.awstandalone : null;
    }

    public String onClick ()
    {
        if (hasBinding(BindingNames.onClick)) {
            return stringValueForBinding(BindingNames.onClick);
        }
        else if (booleanValueForBinding(BindingNames.isRefresh)) {
            return
                AWXBasicScriptFunctions.submitFormString(requestContext(),
                                                         "this.form");
        }
        return null;
    }
}
