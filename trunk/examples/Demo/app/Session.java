package app;

import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWSessionValidationException;

/**
    Session class

    We provide a subclass to simulate tracking user login.
    A real world implementation would associate a user object (and
    possibly track the current "effective user" on a thread-global
    for access by non-UI code).
 */
public class Session extends AWSession
{
    private boolean _isAuthenticated = false;

    // for testing sso -- serves the same purpose as the existence of a user object
    // in a "real" application.
    public void setAuthenticated (boolean isAuthenticated)
    {
        _isAuthenticated = isAuthenticated;
    }

    public boolean isAuthenticated ()
    {
        return _isAuthenticated;
    }

    public void assertAuthenticated ()
    {
        if (!isAuthenticated()) throw new AWSessionValidationException();
    }
}
