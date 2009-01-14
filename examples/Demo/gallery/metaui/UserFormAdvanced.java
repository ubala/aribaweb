package gallery.metaui;

import ariba.ui.aribaweb.core.AWComponent;
import busobj.*;

public class UserFormAdvanced extends AWComponent
{
    public String op = "edit";
    public User user = User.getEffectiveUser();

    public boolean isStateless()
    {
        return false;
    }
}