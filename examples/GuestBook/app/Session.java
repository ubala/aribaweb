package app;

import ariba.ui.aribaweb.core.AWSession;

public class Session extends AWSession
{
    private int _postTabIndex;

    public int postTabIndex ()
    {
        return _postTabIndex;
    }

    public void setPostTabIndex (int postTabIndex)
    {
        _postTabIndex = postTabIndex;
    }
}
