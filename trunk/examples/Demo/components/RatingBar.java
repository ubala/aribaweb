package components;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Fmt;

public class RatingBar extends AWComponent
{
    public String divHTML ()
    {
        int total = intValueForBinding("width", 50);
        double maxVal = doubleValueForBinding("max", 100.0);
        double val = doubleValueForBinding("value", 0.0);
        int used = (int)(val*total/maxVal);
        return Fmt.S("<div style='width:%spx;height:8px; border:1px solid gray;background-color:blue;float:left'><div style='width:%spx;height:8px;background-color:#EEEEEE;float:right'></div></div>",
                Integer.toString(total), Integer.toString(total-used));
    }
}
