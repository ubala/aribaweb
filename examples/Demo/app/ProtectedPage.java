package app;

import ariba.ui.aribaweb.core.AWComponent;

public class ProtectedPage extends AWComponent
{
    public void awake ()
    {
        ((Application)application()).assertAuthenticated(requestContext());
    }
}
