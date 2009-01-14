package example.ui.app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Fmt;

public class AWXRatingBar extends AWComponent
{
    public String divHTML ()
    {
        int total = hasBinding("width") ? intValueForBinding("width") : 50;
        int maxVal = hasBinding("max") ? intValueForBinding("max") : 100;
        int val = intValueForBinding("value");
        int used = val*total/maxVal;
        return Fmt.S("<div style='width:%spx;height:8px;background-color:blue;float:left'></div><div style='width:%spx;height:8px;background-color:#EEEEEE;float:left'></div>",
                Integer.toString(used), Integer.toString(total-used));
    }
}
