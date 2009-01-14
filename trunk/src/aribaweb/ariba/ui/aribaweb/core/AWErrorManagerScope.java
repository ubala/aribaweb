package ariba.ui.aribaweb.core;


public class AWErrorManagerScope extends AWComponent
{
    Object _errorManagerKey;
    Object _pushedErrorManagerState;

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        pushErrorManager();
        super.renderResponse(requestContext, component);
        popErrorManager();
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        pushErrorManager();
        super.applyValues(requestContext, component);
        popErrorManager();
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component) {
        pushErrorManager();
        AWResponseGenerating result = super.invokeAction(requestContext, component);
        popErrorManager();
        return result;
    }

    public void pushErrorManager ()
    {
        // I feel compelled to keep this component stateless, so we're keeping our one piece of state
        // via a key/value on the page
        _errorManagerKey = requestContext().currentElementId();
        Object last = page().get(_errorManagerKey);

        // push our own error manager (or null the first time through)
        _pushedErrorManagerState = page().pushErrorManager(last);
    }

    public void popErrorManager ()
    {
        // pop our error manager and remember it for later
        Object last = page().popErrorManager(_pushedErrorManagerState);
        page().put(_errorManagerKey, last);
        _pushedErrorManagerState = null;
        _errorManagerKey = null;
    }
}
