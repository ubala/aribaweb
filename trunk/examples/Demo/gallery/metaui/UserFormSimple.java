package gallery.metaui;

import ariba.ui.aribaweb.core.AWComponent;
import busobj.*;

public class UserFormSimple extends AWComponent
{
    public User user = User.getEffectiveUser();

    public AWComponent save ()
    {
        if (errorManager().checkErrorsAndEnableDisplay()) return null;
        // Do save...
        return null;
    }

    public boolean isStateless()
    {
        return false;
    }
}
