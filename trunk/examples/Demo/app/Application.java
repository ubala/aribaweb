package app;

import ariba.ui.aribaweb.core.AWLocalLoginSessionHandler;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSessionValidationException;
import ariba.ui.servletadaptor.AWServletApplication;

/**
    Application class

    We provide a subclass so that we can assign a custom session validator.
    (One that make pages "open" by default, but selectively protected).
 */
public class Application extends AWServletApplication
{
    // Set handler for failed authentication
    public void initSessionValidator ()
    {
        if (_sessionValidator == null) setSessionValidator(new AWLocalLoginSessionHandler() {
            protected AWResponseGenerating showLoginPage (AWRequestContext requestContext,
                                                          CompletionCallback callback)
            {
                LoginPage loginPage = (LoginPage)requestContext.pageWithName(LoginPage.class.getName());
                loginPage.init(callback);
                return loginPage;
            }
        });
    }

    /**
        Called by actions (or component awake() methods) to check whether the
        current user / session is authenticated.
        (In a real implementation we'd probably be checking for specific permissions)
     */
    public void assertAuthenticated (AWRequestContext requestContext)
    {
        Session session = (Session)requestContext.session(false);
        if ((session == null) || !session.isAuthenticated()) {
            throw new AWSessionValidationException();
        }
    }
}
