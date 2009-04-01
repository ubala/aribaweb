package app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.appcore.User;

public class WelcomeContent extends AWComponent
{
    public String loggedInString ()
    {
        return User.isLoggedIn()
                ? "Logged in"
                : "Not Logged In";
    }
}
