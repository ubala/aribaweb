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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TextButton.java#47 $
*/

package ariba.ui.widgets;

import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWBindingNames;

public final class TextButton extends AWComponent
{
    public static final AWEncodedString BtnWrap = new AWEncodedString("btnWrap");
    public static final AWEncodedString BtnBrandWrap = new AWEncodedString("btnBrandWrap");
    public static final AWEncodedString BtnClassNormal = new AWEncodedString("btn");
    public static final AWEncodedString BtnClassBrand = new AWEncodedString("btnBrand");
    public static final AWEncodedString BtnClassNormalDisabled = new AWEncodedString("btnDisabled");
    public static final AWEncodedString BtnClassBrandDisabled = new AWEncodedString("btnBrandDisabled");
    public static final AWEncodedString BtnOverClassNormal = new AWEncodedString("btnOver");
    public static final AWEncodedString BtnClassSpecial1 = new AWEncodedString("btnSpecial1");
    public static final AWEncodedString BtnOverClassBrand = new AWEncodedString("btnBrandOver");
    public static final AWEncodedString BtnWrapperLeftRight = new AWEncodedString("LeftRight");

    public static final String ConfirmationBindingName = "confirmationId";
    public static final String PlainStyle = new String("float:none;margin:0px 0px 0px 0px;");

    private static final String MouseOverFmt = "return ariba.Widgets.btnMouseOver(this, '%s');";
    private static final String MouseOutFmt = "return ariba.Widgets.btnMouseOut(this, '%s');";

    public static final AWEncodedString MouseOverNormal =
        new AWEncodedString(Fmt.S(MouseOverFmt, BtnOverClassNormal.string()));
    public static final AWEncodedString MouseOutNormal =
        new AWEncodedString(Fmt.S(MouseOutFmt, BtnClassNormal.string()));

    public boolean _isDisabled;
    private boolean _isBrandStyle;
    private boolean _isHilite;
    private boolean _isPlain;
    public AWEncodedString _formName;
    public AWBinding _actionBinding;
    public AWBinding _isBrandStyleBinding;
    public AWBinding _disabledBinding;
    public AWBinding _hiliteBinding;
    public AWBinding _plainBinding;
    public AWBinding _styleBinding;
    public AWBinding _formNameBinding;
    public AWBinding _formValueBinding;
    private AWEncodedString _buttonClass;
    public AWEncodedString _buttonWrapperStyle;
    private AWEncodedString _onClick;
    public AWEncodedString _confirmationId;

    public boolean isStateless ()
    {
        return true;
    }

    public boolean useLocalPool ()
    {
        return true;
    }

    protected void awake ()
    {
        _isDisabled = booleanValueForBinding(_disabledBinding) ||
            AWEditableRegion.disabled(requestContext());
    }

    protected void sleep ()
    {
        _isDisabled = false;
        _isBrandStyle = false;
        _isHilite = false;
        _isPlain = false;
        _formName = null;
        _buttonClass = null;
        _onClick = null;
        _confirmationId = null;
        _buttonWrapperStyle = null;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        _isBrandStyle = booleanValueForBinding(_isBrandStyleBinding);
        _isHilite = booleanValueForBinding(_hiliteBinding);
        _isPlain = booleanValueForBinding(_plainBinding);
        _buttonWrapperStyle = encodedStringValueForBinding(BindingNames.buttonWrapperStyle);
        // The formName attribute should only be provided when NOT using an AWForm
        _formName = encodedStringValueForBinding(_formNameBinding);
        // Use the onClick binding normally it is a javascript function
        _onClick = encodedStringValueForBinding(BindingNames.onClick);

        // if there is a confirmationId on the TextButton, then use client side
        // visibility
        _confirmationId = encodedStringValueForBinding(ConfirmationBindingName);
        if (_confirmationId != null) {
            Confirmation.setClientSideConfirmation(requestContext(), _confirmationId);
        }

        super.renderResponse(requestContext, component);
    }

    public AWEncodedString buttonClassString ()
    {
        if (_buttonClass == null) {
            _buttonClass = encodedStringValueForBinding(BindingNames.buttonClass);

            if (_buttonClass == null) {
                if (_isDisabled) {
                    _buttonClass = _isBrandStyle ? BtnClassBrandDisabled : BtnClassNormalDisabled;
                }
                else if (_isHilite) {
                    _buttonClass = BtnClassSpecial1;
                }
                else {
                    _buttonClass = _isBrandStyle ? BtnClassBrand : BtnClassNormal;
                }
            }
            else {
               if (_isDisabled) {
                    _buttonClass = AWEncodedString.sharedEncodedString(
                        StringUtil.strcat(_buttonClass.string(), "Disabled"));
                }
            }
        }

        return _buttonClass;
    }

    public AWEncodedString wrapClassString ()
    {
        AWEncodedString cls = encodedStringValueForBinding(AWBindingNames.classBinding);
        return (cls != null) ? cls : (_isBrandStyle ? BtnBrandWrap : BtnWrap);
    }

    public boolean useLeftRightWrapper ()
    {
        return BtnWrapperLeftRight.equals(_buttonWrapperStyle);
    }


    public Boolean isIE4 ()
    {
        return Util.isIE4(requestContext());
    }

    public Object appendStyleString ()
    {
        /*
            If the button has a hilite attribute and that evaluates to false, then add 3px margin to the bottom
            to make it line up with buttons that have hilite and eval to true.  This makes it easy to have a
            button in a repetition and only have on of those buttons have hilite, but all the buttons will line up.
        */
        if (_styleBinding != null || _hiliteBinding != null || _isPlain) {
            AWEncodedString userStyle = encodedStringValueForBinding(_styleBinding);

            if (userStyle != null || _isPlain) {
                AWResponse response = response();
                response.appendContent(Constants.Space);
                response.appendContent(Constants.Style);
                response.appendContent(Constants.Equals);
                response.appendContent(Constants.Quote);
                if (_isPlain) {
                    response.appendContent(PlainStyle);
                }
                if (userStyle != null) {
                    response.appendContent(userStyle);
                }
                response.appendContent(Constants.Quote);
            }
        }
        return null;
    }

    public AWEncodedString onClickString ()
    {
        if (!_isDisabled && _onClick != null && _confirmationId == null) {
            AWResponse response = response();
            response.appendContent(Constants.Space);
            response.appendContent(Constants.OnClick);
            response.appendContent(Constants.Equals);
            response.appendContent(Constants.Quote);
            response.appendContent(_onClick);
            response.appendContent(Constants.Quote);
        }
        return null;
    }

    public String behaviorName ()
    {
        return (!_isDisabled) ? ((_onClick == null) ? "TB" : "TBc") : null;
    }

    public boolean useActionButton ()
    {
        return (_formName == null) && !_isDisabled &&
               (_onClick == null) && (_confirmationId == null);
    }

    public Object isTrigger ()
    {
        return (_formValueBinding != null) ? Boolean.TRUE : null;
    }

    public void setFormValue (String value)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(value)) {
            setValueForBinding("", _formValueBinding);
        }
    }

    public AWResponseGenerating action ()
    {
        // if there is a Confirmation Dialog Id, then display that rather than
        // calling the action on the page
        return (AWResponseGenerating)valueForBinding(_actionBinding);
    }

    /*----------------------------------------------------------------------
        record and playback
    --------------------------------------------------------------------*/
    protected boolean _debugSemanticKeyInteresting()
    {
        return false;
    }
}
