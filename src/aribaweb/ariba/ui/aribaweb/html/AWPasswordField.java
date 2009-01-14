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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWPasswordField.java#20 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.Fmt;

// subclassed for validation
public class AWPasswordField extends AWComponent
{
    public static final AWEncodedString CheckCapsLockForceRefreshFunction =
        new AWEncodedString("return ariba.Handlers.checkCapsLockErrorTxtRfrsh(event,this);");

    public static final AWEncodedString CheckCapsLockFunction =
        new AWEncodedString("return ariba.Handlers.checkCapsLockError(event);");

    protected static final String[] SupportedBindingNames = {
        BindingNames.value,
        BindingNames.name,
        BindingNames.formatter,
        BindingNames.isRefresh,
        BindingNames.errorKey,
        BindingNames.onKeyDown,
    };

    private AWEncodedString _inputName = null;
    private Object _errorKey;
    public AWEncodedString _warnId = null;

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();
        _inputName = null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // The value of a password field cannot be rendered in an IFrame incremental update.
        // This is due IE browser security against setting/getting password value using javascript.
        // So we need to force a FPR when we have a password field in a non-XMLHTTP on IE
        if (requestContext().isIncrementalUpdateRequest()
                && !requestContext().isXMLHttpIncrementalRequest()
                && request().isBrowserMicrosoft()) {
            requestContext.forceFullPageRefresh();
        }
        super.renderResponse(requestContext, component);
    }

    public String formattedString ()
    {
        String formattedString = "";
        Object objectValue = valueForBinding(BindingNames.value);
        Object formatter = valueForBinding(BindingNames.formatter);
        if (formatter == null) {
            formattedString = AWUtil.toString(objectValue);
        }
        else {
            formattedString = AWFormatting.get(formatter).format(formatter, objectValue);
        }
        return formattedString;
    }

    public void setFormValue (String formValueString)
    {
        Object objectValue = null;
        if (formValueString.length() == 0) {
            AWBinding emptyStringValueBinding =
                bindingForName(BindingNames.emptyStringValue, true);
            if (emptyStringValueBinding != null) {
                formValueString = (String)valueForBinding(emptyStringValueBinding);
            }
        }
        if (formValueString != null) {
            Object formatter = valueForBinding(BindingNames.formatter);
            if (formatter == null) {
                objectValue = formValueString;
            }
            else {
                try {
                    objectValue =
                        AWFormatting.get(formatter).parseObject(formatter,
                            formValueString);
                }
                catch (RuntimeException exception) {
                    recordValidationError(exception, errorKey(), formValueString);
                    return;
                }
            }
        }
        setValueForBinding(objectValue, BindingNames.value);
    }

    public AWEncodedString onKeyDownString ()
    {
        return booleanValueForBinding(BindingNames.isRefresh) ?
            CheckCapsLockForceRefreshFunction : CheckCapsLockFunction;
    }

    public AWEncodedString getCheckCapsLockFunction ()
    {
        return CheckCapsLockFunction;
    }

    public String onKeyPressString ()
    {
        return Fmt.S("return ariba.Handlers.noCapsLockTxt(event,this, '%s');", _warnId);
    }

    public boolean hasForm ()
    {
        return requestContext().currentForm() != null;
    }

    public AWEncodedString inputName ()
    {

        if (_inputName == null) {
            AWBinding name = bindingForName(BindingNames.name);
            if (hasBinding(name)) {
                _inputName =
                    AWEncodedString.sharedEncodedString(
                        stringValueForBinding(name));
            }
            // if no name binding or we have a name binding but it is null then
            // grab an element id
            if (_inputName == null) {
                _inputName = requestContext().nextElementId();
            }
        }

        return _inputName;
    }

    private Object errorKey ()
    {
        if (_errorKey == null) {
            _errorKey = AWErrorManager.getErrorKeyForComponent(this);
            if (_errorKey == null) {
                _errorKey = inputName();
            }
        }
        return _errorKey;
    }
}
