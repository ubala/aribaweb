package app;

import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWDirectAction;

/**
    Some samples of implementing protected actions
 */
public class DirectAction extends AWDirectAction
{
    public AWResponseGenerating docAction ()
    {
        // Not protected
        return requestContext().pageWithName("SearchSource");
    }

    public AWResponseGenerating sampleSecureAction ()
    {
        // Assert protection here
        ((Application)application()).assertAuthenticated(requestContext());
        return requestContext().pageWithName("SearchSource");
    }

    public AWResponseGenerating protectedPageAction ()
    {
        // Action is not protected, but the destination should be...
        return requestContext().pageWithName("ProtectedPage");
    }
}
