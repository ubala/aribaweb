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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWPasswordField.java#24 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.core.FastStringBuffer;
import java.util.regex.Pattern;

// subclassed for validation
public class AWPasswordField extends AWComponent
{
    public static final AWEncodedString CheckCapsLockForceRefreshFunction =
        new AWEncodedString("return ariba.Handlers.checkCapsLockErrorTxtRfrsh(event,this);");

    public static final AWEncodedString CheckCapsLockFunction =
        new AWEncodedString("return ariba.Handlers.checkCapsLockError(event);");

    private static final String MaskedValue = "**********";
    private static final Pattern MaskeValuePattern = Pattern.compile("^\\*+$");

    protected static final String[] SupportedBindingNames = {
        BindingNames.value,
        BindingNames.name,
        BindingNames.formatter,
        BindingNames.isRefresh,
        BindingNames.errorKey,
        BindingNames.onKeyDown,
        BindingNames.classBinding,
        BindingNames.size,
        BindingNames.placeholder
    };

    private AWEncodedString _inputName = null;
    private Object _errorKey;
    public AWEncodedString _warnId = null;
    private boolean _indicateLength = false;
    private String _formattedString;
    private String _placeholder;

    
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
        // recalculate formattedString
        _formattedString = null;
        formattedString();
        _placeholder = stringValueForBinding(AWBindingNames.placeholder);
        // The value of a password field cannot be rendered in an IFrame incremental update.
        // This is due IE browser security against setting/getting password value using javascript.
        // So we need to force a FPR when we have a password field in a non-XMLHTTP on IE
        if (requestContext().isIncrementalUpdateRequest()
                && !requestContext().isXMLHttpIncrementalRequest()
                && request().isBrowserMicrosoft()
                && !StringUtil.nullOrEmptyString(_formattedString)) {
            requestContext.forceFullPageRefresh();
        }

        super.renderResponse(requestContext, component);
    }
    
    public String formattedString ()
    {
        if (_formattedString == null) {
            Object objectValue = valueForBinding(BindingNames.value);
            Object formatter = valueForBinding(BindingNames.formatter);
            if (formatter == null) {
                _formattedString = AWUtil.toString(objectValue);
            }
            else {
                _formattedString = AWFormatting.get(formatter).format(formatter, objectValue);
            }
            if (!StringUtil.nullOrEmptyString(_formattedString)) {
                _formattedString = getMaskedValue(_formattedString);
            }
        }
        return _formattedString;
    }

    public void setFormValue (String formValueString)
    {
        Object objectValue = null;
        boolean shouldSetValue = true;
        if (formValueString.length() == 0) {
            AWBinding emptyStringValueBinding =
                bindingForName(BindingNames.emptyStringValue, true);
            if (emptyStringValueBinding != null) {
                formValueString = (String)valueForBinding(emptyStringValueBinding);
            }
        }
        if (formValueString != null) {
            shouldSetValue = !isMaskedValue(formValueString);
            if (shouldSetValue) {
                // indicate length on next render since the user has changed the value
                _indicateLength = true;
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
        }
        if (shouldSetValue) {
            setValueForBinding(objectValue, BindingNames.value);
        }
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

    public String cssClass ()
    {
        String cls = stringValueForBinding(BindingNames.classBinding);
        return (cls != null) ? cls : ((valueForBinding(BindingNames.size) == null) ? "tf tfW" : "tf");
    }

    private String getMaskedValue (String value)
    {
        if (_indicateLength) {
            int length = value.length();
            FastStringBuffer buffer = new FastStringBuffer(length);
            for (int i = 0; i < length; i++) {
                buffer.append("*");
            }
            return buffer.toString();
        }
        return MaskedValue;
    }

    private boolean isMaskedValue (String value)
    {
        return MaskeValuePattern.matcher(value).matches();
    }

    public String placeholder ()
    {        
        return _placeholder;
    }

    public boolean displayPlaceholder ()
    {
        return _placeholder != null;
    }

    public String pfcClass ()
    {
        if (displayPlaceholder() &&
            StringUtil.nullOrEmptyString(formattedString())) {
            return "pfc";
        }
        return null;
    }

    public String onFocus ()
    {
        return displayPlaceholder() ? "ariba.Handlers.hPassFocus(this, event)" : null;
    }

    public String onBlur ()
    {
        return displayPlaceholder() ? "ariba.Handlers.hPassBlur(this, event)" : null;
    }
}
