package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;

public class AWFocusRegion extends AWComponent
{
    public AWEncodedString _elementId;
    public boolean shouldFocus ()
    {
        boolean shouldFocus =
            booleanValueForBinding(AWBindingNames.focus, false);
        setValueForBinding(false, AWBindingNames.focus);
        return shouldFocus;
    }
}
