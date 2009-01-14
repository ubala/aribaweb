package components;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;

public class GMap extends AWComponent
{
    // "http://localhost" key
    public static String GMapsKey = "ABQIAAAASJDgD84hBs40CGHxka0HMhT2yXp_ZAY8_ufC3CFXhHIE1NvwkxTa6vhdd7AsF1e4Iy_SEXIcKRj4nA";

    public static String getGMapsKey()
    {
        return GMapsKey;
    }

    public static void setGMapsKey (String GMapsKey)
    {
        GMap.GMapsKey = GMapsKey;
    }

    public Object _mapId, _lngId, _latId;

    protected void sleep()
    {
        super.sleep();
        _mapId = _latId = _lngId = null;
    }

    public String url ()
    {
        return "http://maps.google.com/maps?file=api&v=2&key=" + GMapsKey;
    }

    public String style ()
    {
        int w = intValueForBinding(AWBindingNames.width, 500);
        int h = intValueForBinding(AWBindingNames.height, 300);
        return Fmt.S("width:%spx; height:%spx", Integer.toString(w), Integer.toString(h));
    }

    public void setEventLatitudeString (String s)
    {
        if (StringUtil.nullOrEmptyString(s)) return;
        double d = Double.parseDouble(s);
        setValueForBinding(new Double(d), "eventLatitude");
    }

    public void setEventLongitudeString (String s)
    {
        if (StringUtil.nullOrEmptyString(s)) return;
        double d = Double.parseDouble(s);
        setValueForBinding(new Double(d), "eventLongitude");
    }
}
