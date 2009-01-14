package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;

public class PortletWrapper extends AWComponent
{
    /*
    <a:Image filename="$minMaxButtonImage"/>
    <a:Image filename="$closeImage"/>
     */
    protected PortletStyle _portletStyle;

    public void sleep ()
    {
        _portletStyle = null;
    }

    public PortletStyle portletStyle ()
    {
        if (_portletStyle == null) {
            String style = stringValueForBinding("portletStyle");
            String width = (String)env().peek("portletWidth");

            if (style == null) style = ("narrow".equals(width)) ? "TOC" : "normal";
            _portletStyle = style.equals("normal") ? _DefaultStyle : _TOCStyle;
        }
        return _portletStyle;
    }

    public String minMaxButtonImage ()
    {
        return booleanValueForBinding("isMinimized")
                ? portletStyle().getMinimizedImage() : portletStyle().getMaximizedImage();
    }
    
    public static PortletStyle _DefaultStyle = new PortletStyle();
    public static PortletStyle _TOCStyle = new TOCStyle();

    public static class PortletStyle
    {
        public String getContainerCssClass () { return "portletBox"; }
        public boolean hideGradient () { return true; }
        public String getMinimizedImage() { return "portletMinimizeAW.gif"; }
        public String getMaximizedImage() { return "portletMaximizeAW.gif"; }
        public String getOptionsImage () { return "portletOptionsAW.gif"; }
        public String getCloseImage () { return "portletCloseAW.gif"; }
    }

    public static class TOCStyle extends PortletStyle
    {
        public String getContainerCssClass () { return "actionPortletBox"; }
        public boolean hideGradient () { return false; }
        public String getMinimizedImage() { return "portletMinimizeGray.gif"; }
        public String getMaximizedImage() { return "portletMaximizeGray.gif"; }
        public String getOptionsImage () { return "portletOptions.gif"; }
        public String getCloseImage () { return "portletCloseGray.gif"; }
    }
}
