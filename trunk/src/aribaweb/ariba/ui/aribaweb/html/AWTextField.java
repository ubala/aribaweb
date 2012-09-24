/*
    Copyright 1996-2010 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWTextField.java#64 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWInputId;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.aribaweb.core.AWHighLightedErrorScope;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.StringUtil;
import ariba.util.formatter.FormatterHandlesNulls;
import ariba.util.core.ArrayUtil;

// subclassed for validation
public class AWTextField extends AWComponent
{
    protected static final AWEncodedString ForceRefreshFunction =
        new AWEncodedString("return ariba.Handlers.textRefresh(event,this);");
    public static final String[] SupportedBindingNames = {
        BindingNames.id,
        BindingNames.name,
        BindingNames.value,
        BindingNames.editable,
        BindingNames.action,
        BindingNames.formatter,
        BindingNames.emptyStringValue,
        BindingNames.onChange,
        BindingNames.autoselect,
        BindingNames.isRefresh,
        BindingNames.errorKey,
        BindingNames.onKeyDown,
        BindingNames.disabled,
        BindingNames.behavior,
        BindingNames.size,
        BindingNames.classBinding,
        BindingNames.placeholder,
        BindingNames.formValueStrings
    };
    public AWEncodedString _elementId;
    private AWEncodedString _textFieldName;
    private AWBinding _action;
    public boolean _isRefresh;
    private Object _errorKey;
    private Object _formatter;
    private boolean _formatterHandlesNulls;
    private boolean _disabled;
    private String _placeholder;
    private String _formatValue;

    private static final String EmptyString = "";

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void awake ()
    {
        _action = bindingForName(BindingNames.action);
        _isRefresh = booleanValueForBinding(BindingNames.isRefresh);
        _formatter = valueForBinding(BindingNames.formatter);
        if (_formatter instanceof FormatterHandlesNulls) {
            _formatterHandlesNulls = true;
        }
        _disabled = booleanValueForBinding(BindingNames.disabled) ||
            AWEditableRegion.disabled(requestContext());
        _placeholder = stringValueForBinding(BindingNames.placeholder);
    }

    protected void sleep ()
    {
        _elementId = null;
        _textFieldName = null;
        _action = null;
        _isRefresh = false;
        _errorKey = null;
        _formatter = null;
        _formatterHandlesNulls = false;
        _placeholder = null;
        _formatValue = null;
    }

    public String formattedString ()
    {
        return formatValue(valueForBinding(BindingNames.value));
    }

    /**
     * Format some value, using our formatter.
     * This can be used to format the field's value, or the error value
     * @param value The value to be formatted
     * @return the string version of the value
     * @aribaapi private
     */
    private String formatValue (Object value)
    {
        String formattedString = EmptyString;
        if (value != null || _formatterHandlesNulls) {
            if (_formatter == null) {
                formattedString = AWUtil.toString(value);
            }
            else {
                formattedString = AWFormatting.get(_formatter).format(_formatter, value);
            }
        }
        return formattedString;
    }

    public String formatPlaceHolder ()
    {
        return _placeholder == null ? null : StringUtil.strcat(" ", _placeholder);
    }

    public void setFormValue (String formValueString)
    {
        if (_disabled) {
            return;
        }
        if (_placeholder != null &&
            formValueString.indexOf(_placeholder) >= 0) {
            formValueString = "";            
        }
        Object objectValue = null;
        if (formValueString.length() == 0) {
            AWBinding emptyStringValueBinding =
                bindingForName(BindingNames.emptyStringValue, true);
            if (emptyStringValueBinding != null) {
                formValueString = (String)valueForBinding(emptyStringValueBinding);
            }            
        }
        if (formValueString != null) {
            if (_formatter == null) {
                objectValue = formValueString;
            }
            else {
                try {
                    objectValue = AWFormatting.get(_formatter).
                        parseObject(_formatter, formValueString);
                }
                catch (RuntimeException exception) {
                    recordValidationError(exception, errorKey(), formValueString);
                    return;
                }
            }
        }
        if (textFieldName() != null) {
            // get old cached value
            Object _oldV = page().pageComponent().dict(textFieldName().string());
            // get the binding of rawValues and set its binding if it is not null
            AWBinding binding = bindingForName(BindingNames.formValueStrings);
            if (binding != null) {
                setValueForBinding(ArrayUtil.array(_oldV, formValueString),binding);
            }
        }
        setValueForBinding(objectValue, BindingNames.value);
    }

    public AWEncodedString textFieldName ()
    {
        if (_textFieldName == null) {
            _textFieldName = encodedStringValueForBinding(BindingNames.name);
            if (_textFieldName == null) {
                _textFieldName = _elementId;
            }
        }
        return _textFieldName;
    }

    public AWEncodedString textFieldId ()
    {
        AWEncodedString id = null;
        if (hasBinding(BindingNames.id)) {
            id = encodedStringValueForBinding(BindingNames.id);
        }

        if (id == null) {
            id = AWInputId.getAWInputId(requestContext());
            if (id == null) {
                id = textFieldName();
            }
        }

        return id;
    }

    public String tfActionType ()
    {
        if (_action != null) {
            return _isRefresh ? "ROKP" : "AC";
        }
        else if (_isRefresh) {
            return "FRF";
        }
        return null;
    }

    public boolean isSender ()
    {
        return AWGenericActionTag.isHiddenFieldSender(request(), textFieldName());
    }

    public AWResponseGenerating invokeAction ()
    {
        return (AWResponseGenerating)valueForBinding(_action);
    }

    public AWEncodedString onChangeString ()
    {
        return _isRefresh ? null :
                encodedStringValueForBinding(BindingNames.onChange);
    }

    public AWEncodedString onBlurString ()
    {
        return _placeholder == null ? null :
                AWEncodedString.sharedEncodedString("ariba.Handlers.hTextBlur(this, event)");
    }

    private boolean _allowAutoFocus ()
    {
        // if this is a disabled textfield then disable focus
        if (_disabled) {
            return false;
        }

        // if the awAllowAutoFocus environment is set to false, then return disable focus
        Boolean allowFocus = (Boolean)env().peek("awAllowAutoFocus");
        if (allowFocus != null && !allowFocus.booleanValue()) {
            return false;
        }

        if (hasBinding(BindingNames.autoselect)) {
            // otherwise, if there is an autoselect binding use it,
            return booleanValueForBinding(BindingNames.autoselect);
        }
        return true;
    }

    public String allowAutoFocus ()
    {
        return _allowAutoFocus() ? null : "0";
    }

    public String formattedValue ()
    {
        if (_formatValue == null) {
            String errorValue = null;

            // See if we have an error value...  Need to check even for fields without formatters -- they
            // can have (page assigned) errors too...
            if (_formatter != null || hasBinding(BindingNames.errorKey)) {
                AWErrorManager errorManager = errorManager();
                Object errorKey = errorKey();
                Object errorObjValue =
                    (errorKey != null) ? errorManager.errantValueForKey(errorKey) : null;
                if (errorObjValue instanceof String) {
                    errorValue = (String)errorObjValue;
                }
                else if (errorObjValue != null) {
                    try {
                        errorValue = formatValue(errorObjValue);
                    }
                    catch (AWGenericException e) {
                        errorValue = errorObjValue.toString();
                        Log.aribaweb.debug("AWTextField: format exception caught." +
                                           "  Using value.toString.");
                        Log.logException(Log.aribaweb, e);
                    }
                }
            }
            _formatValue = (errorValue == null) ? formattedString() : errorValue;

            if (_placeholder != null &&
                StringUtil.nullOrEmptyString(_formatValue)) {
                _formatValue = formatPlaceHolder();
            }
            
            // 1-55LMEY
            if (_formatValue != null && _formatValue.startsWith("`")) {
                requestContext().forceFullPageRefresh();
            }
        }
        // get the page and cache the value to the page cache
        if (textFieldName() != null) {
            page().pageComponent().dict(textFieldName().string(), _formatValue);
        }
        return _formatValue;
    }

    private Object errorKey ()
    {
        if (_errorKey == null) {
            _errorKey = AWErrorManager.getErrorKeyForComponent(this);
        }
        if (_errorKey == null) {
            _errorKey = _elementId;
        }

        return _errorKey;
    }

    public String isDisabled ()
    {
        return (_disabled || requestContext().isPrintMode()) ?
                BindingNames.awstandalone : null;
    }

    public boolean isEditable ()
    {
        Boolean editable;
        if (hasBinding(AWBindingNames.editable)) {
            editable = booleanValueForBinding(AWBindingNames.editable);
        }
        else {
            editable = (Boolean)env().peek("editable");
        }
        AWRequestContext requestContext = requestContext();
        return ((editable != null) && editable &&
                !requestContext.isPrintMode() && !requestContext.isExportMode());
    }

    public String cssClass ()
    {
        String cls = stringValueForBinding(BindingNames.classBinding);
        cls = cls == null ? "" : " " + cls;
        cls = (valueForBinding(BindingNames.size) == null ?  "tf tfW" : "tf") + cls;
        if (_disabled) {
            cls = "tfDis " + cls;
        }
        if (_placeholder != null) {
            if (formattedValue().equals(formatPlaceHolder())) {
                cls = "ph " + cls;
            }
        }
        return cls;
    }

    public boolean isInHighLightedErrorScope ()
    {
        return AWHighLightedErrorScope.isInHighLightedErrorScope(env());
    }
}
