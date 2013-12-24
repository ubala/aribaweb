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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWTextArea.java#33 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWInputId;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.core.AWHighLightedErrorScope;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.util.core.Fmt;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;
import ariba.util.formatter.FormatterHandlesNulls;
import ariba.util.formatter.IntegerFormatter;

// subclassed for validation
public class AWTextArea extends AWComponent
{
    private static final String[] SupportedBindingNames = {
         BindingNames.name,
         BindingNames.value,
         BindingNames.emptyStringValue,
         BindingNames.escapeHtml,
         BindingNames.formatter,
         BindingNames.isRefresh,
         BindingNames.maxLength,
         BindingNames.onChange, // deprecated?
         BindingNames.errorKey,
         BindingNames.editable,
         BindingNames.onKeyDown,
         BindingNames.showMaxLength,
         BindingNames.placeholder,
         BindingNames.behavior
    };
    public AWEncodedString _elementId;
    public AWEncodedString _textAreaId;
    private AWEncodedString _textAreaName;
    private Object _errorKey;
    private Object _formatter;
    private boolean _formatterHandlesNulls;
    private boolean _disabled;
    private String _placeholder;

    private static final String LimitTextLengthFmt =
        "ariba.Dom.limitTextLength(document.%s.%s,%s);";

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void awake ()
    {
        _formatter = valueForBinding(BindingNames.formatter);
        if (_formatter instanceof FormatterHandlesNulls) {
            _formatterHandlesNulls = true;
        }
        _disabled = AWEditableRegion.disabled(requestContext());
        _placeholder = stringValueForBinding("placeholder");
    }

    protected void sleep ()
    {
        _elementId = null;
        _textAreaId = null;
        _textAreaName = null;
        _errorKey = null;
        _formatter = null;
        _formatterHandlesNulls = false;
        _disabled = false;
        _placeholder = null;
    }

    public AWEncodedString limitTextJavaScriptString ()
    {
        return AWEncodedString.sharedEncodedString(Fmt.S(LimitTextLengthFmt,
                                   requestContext().currentForm().formName(),
                                   textAreaId(), maxLength()));
    }

    public AWEncodedString textAreaName ()
    {
        if (_textAreaName == null) {
            _textAreaName = encodedStringValueForBinding(BindingNames.name);
            if (_textAreaName == null) {
                _textAreaName = _elementId;
            }
        }
        return _textAreaName;
    }

    public Object maxLength ()
    {
        Object objectValue = valueForBinding(BindingNames.maxLength);
        if (objectValue instanceof Integer) {
            return ((Integer)objectValue);
        }
        else if (objectValue instanceof String &&
                 !StringUtil.nullOrEmptyOrBlankString((String)objectValue)) {
            return objectValue;
        }
        else {
            return Constants.getInteger(0);
        }
    }

    private int maxLengthInt ()
    {
        return IntegerFormatter.getIntValue(maxLength());
    }

    public String formattedString ()
    {
        String formattedString = null;
        Object objectValue = valueForBinding(BindingNames.value);
        if (objectValue != null || _formatterHandlesNulls) {
            if (_formatter == null) {
                formattedString = AWUtil.toString(objectValue);
            }
            else {
                formattedString =
                    AWFormatting.get(_formatter).format(_formatter, objectValue);
            }
        }
        if (_placeholder != null &&
            StringUtil.nullOrEmptyString(formattedString)) {
            formattedString = formatPlaceHolder();
        }
        return formattedString;
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
                    objectValue = AWFormatting.get(_formatter).parseObject(
                                                        _formatter, formValueString);
                }
                catch (RuntimeException exception) {
                    recordValidationError(exception, errorKey(), formValueString);
                    return;
                }
            }
        }
        setValueForBinding(objectValue, BindingNames.value);
    }

    public AWEncodedString textAreaId ()
    {
        if (_textAreaId == null) {
            _textAreaId = AWInputId.getAWInputId(requestContext());
            if (_textAreaId == null) {
                _textAreaId = textAreaName();
            }
        }
        return _textAreaId;
    }

    public AWEncodedString onKeyDownString ()
    {
        return booleanValueForBinding(BindingNames.isRefresh) ? AWTextField.ForceRefreshFunction : null;
    }

    public String formattedValue ()
    {
        String value = null;
        if (_formatter != null) {
            AWErrorManager errorManager = errorManager();
            Object errorKey = errorKey();
            value = (String)errorManager.errantValueForKey(errorKey);
        }
        return (value == null) ? formattedString() : value;
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

    public boolean isEditable ()
    {
        if (hasBinding(AWBindingNames.editable)) {
            return booleanValueForBinding(AWBindingNames.editable);
        }
        AWRequestContext requestContext = requestContext();
        Boolean editable = (Boolean)env().peek("editable");
        return ((editable != null) && editable.booleanValue() &&
                !requestContext.isPrintMode() && !requestContext.isExportMode());
    }

    public boolean isInHighLightedErrorScope ()
    {
        return AWHighLightedErrorScope.isInHighLightedErrorScope(env());
    }

    public Object disabled ()
    {
        return _disabled ? BindingNames.awstandalone : null;
    }

    public boolean showMaxLengthIndicator ()
    {
        return maxLengthInt() > 0 && booleanValueForBinding(BindingNames.showMaxLength); 
    }

    public AWEncodedString maxLengthIndicatorId ()
    {
        return AWEncodedString.sharedEncodedString(
                    StringUtil.strcat(textAreaId().toString(), "MLI"));            
    }

    public int maxLengthIndicatorString ()
    {
        int maxLength = maxLengthInt();
        String formattedString = formattedString();
        int stringLength = formattedString != null ? formattedString.length() : 0;
        return Math.max(maxLength - stringLength, 0);
    }

    public String cssClass ()
    {
        if (_placeholder != null) {
            if (formattedValue().equals(formatPlaceHolder())) {
                return "ph";
            }
        }
        return "";
    }

    public String formatPlaceHolder ()
    {
        return _placeholder == null ? null : StringUtil.strcat(" ", _placeholder);
    }

    public AWEncodedString onBlurString ()
    {
        return _placeholder == null ? null :
                AWEncodedString.sharedEncodedString("ariba.Handlers.hTextBlur(this, event)");
    }

    public AWEncodedString onPasteString ()
    {
        return AWEncodedString.sharedEncodedString(
            "var fnc=function(){ariba.Dom.limitTextLength(this," + maxLength()
                    + ");}; setTimeout(fnc.bind(this),250)");
    }

}
