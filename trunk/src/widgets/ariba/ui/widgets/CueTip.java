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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/CueTip.java#8 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.util.AWEncodedString;

public final class CueTip extends AWComponent
{
    public static final String PreferenceKey = "CueTipPreference";

    public static final String AutoSizeKey = "autoSize";

    public AWEncodedString _menuId;
    public String _divId;
    public boolean _visible = false;
    public String _value;
    public boolean _isManualFormatting = false;

    protected void sleep ()
    {
        super.sleep();
        _menuId = null;
        _divId = null;
        _visible = false;
        _value = null;
    }

    protected void awake ()
    {
        super.awake();
        _menuId = requestContext().nextElementId();
        _divId = requestContext().nextElementId().toString();
        PreferenceHandler handler =
            PreferenceHandler.resolveHandler(CueTip.PreferenceKey);
        if (handler != null) {
            _visible =
                handler.getBooleanPreference(requestContext(), CueTip.PreferenceKey);
        }
        _value = stringValueForBinding(BindingNames.value);

        if (_value != null) {
            _value = _value.replaceAll("\n","<br/>");
        }

        _isManualFormatting = false;
        AWBinding autoSizeBinding = bindingForName(AutoSizeKey);
        if (autoSizeBinding != null) {
            _isManualFormatting = !booleanValueForBinding(autoSizeBinding);
        }
        if (!_isManualFormatting &&
            _value != null && _value.indexOf(StartHTMLIndicator) != -1) {
            // for value based CueTips, if the value begins with HTMLIndicator, then
            // use manaul formatting and strip off the HTMLIndicator
            _value = _value.substring(StartHTMLIndicator.length(),
                                      _value.length()-EndHTMLIndicator.length());
            // debug
            if(Log.widgets_cuetip.isDebugEnabled()) {
                Log.widgets_cuetip.debug("CueTip: manual formatting: %s", _value);
            }
            _isManualFormatting = true;
        }
    }

    public String getValue ()
    {
        return _value;
    }

    public static final String StartHTMLIndicator = "<html>";
    public static final String EndHTMLIndicator   = "</html>";

    public boolean isManualFormatting ()
    {
        return _isManualFormatting;
    }

    public String getStyle ()
    {
        String style = null;
        if (isManualFormatting()) {
            style = "white-space:nowrap";
        }
        return style;
    }

    public String getOnDisplayHandler ()
    {
        if (!isManualFormatting()) {
            return "ariba.Widgets.sizeMsgDiv(elm)";
        }
        return null;
    }

    public static final void setState (AWRequestContext requestContext, boolean state)
    {
        PreferenceHandler handler =
            PreferenceHandler.resolveHandler(CueTip.PreferenceKey);

        if (handler != null) {
            handler.setBooleanPreference(requestContext, CueTip.PreferenceKey, state);
        }
    }

}
