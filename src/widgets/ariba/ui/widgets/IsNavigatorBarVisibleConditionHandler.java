package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;

public final class IsNavigatorBarVisibleConditionHandler extends ConditionHandler
{
    public boolean evaluateCondition (AWRequestContext requestContext)
    {
        AWComponent pageWrapper = PageWrapper.instance(requestContext.getCurrentComponent());
        if (pageWrapper.hasBinding(BindingNames.isNavigationBarVisible)) {
            return pageWrapper.booleanValueForBinding(BindingNames.isNavigationBarVisible);
        }
        return true;
    }
}
