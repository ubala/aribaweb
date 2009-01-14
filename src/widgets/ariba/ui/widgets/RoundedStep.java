package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;

public class RoundedStep extends AWComponent {

    public String currentCssClass ()
    {
        return booleanValueForBinding("selected") ? "wizStepCurrent" : "wizStep";
    }
    
}
