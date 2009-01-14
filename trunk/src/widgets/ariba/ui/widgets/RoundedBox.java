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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/RoundedBox.java#5 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Fmt;

public class RoundedBox extends AWComponent
{
    public static final Style Top =         new Style(true,  false, "rbT", false);
    public static final Style Bottom =      new Style(false, true,  "rbBt", false);
    public static final Style Left =        new Style(true,  true,  "rbL", false);
    public static final Style Right =       new Style(true,  true,  "rbR", false);
    public static final Style TopLeft =     new Style(true,  false, "rbTL", false);
    public static final Style TopRight =    new Style(true,  false, "rbTR", false);
    public static final Style BottomLeft =  new Style(false, true,  "rbBL", false);
    public static final Style BottomRight = new Style(false, true,  "rbBR", false);
    public static final Style All =         new Style(true,  true,  "rbA", false);
    public static final Style SmallAll =    new Style(true,  true,  "rbSA", true);
    public static final Style None =        new Style(false, false, "", false);

    private static final String[] SupportedBindingNames =
        { BindingNames.classBinding, "hideGradient", "roundStyle"};

    private Style _style;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    /////////////
    // Awake
    /////////////
    protected void awake()
    {
        super.awake();
        _style = Top;
        Object styleObject = valueForBinding("roundStyle");
        if (styleObject instanceof Style) {
            _style = (Style)styleObject;
        }
    }

    protected void sleep()
    {
        _style = null;
        super.sleep();
    }

    public String cssClass ()
    {
        Object cssClassValue = valueForBinding(BindingNames.classBinding);
        String cssClass = null;
        if (cssClassValue != null) {
            if (cssClassValue instanceof AWEncodedString) {
                cssClass = ((AWEncodedString)cssClassValue).string();                
            }
            else {
                cssClass = (String)cssClassValue;
            }
        }
        return cssClass != null ?
            Fmt.S("%s %s", _style.roundClass(), cssClass) :
            _style.roundClass();
    }

    public Style getStyle ()
    {
        return _style;
    }

    public static class Style
    {
        private boolean _showTop;
        private boolean _showBottom;
        private String _roundClass;
        private boolean _isSmall;

        private Style (boolean showTop, boolean showBottom,
                       String roundClass, boolean isSmall)
        {
            _showTop = showTop;
            _showBottom = showBottom;
            _roundClass = roundClass;
            _isSmall = isSmall;
        }

        public boolean showTop ()
        {
            return _showTop;
        }

        public boolean showBottom ()
        {
            return _showBottom;
        }

        public String roundClass ()
        {
            return _roundClass;
        }

        public boolean isSmall ()
        {
            return _isSmall;
        }
    }

}
