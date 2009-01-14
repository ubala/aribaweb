/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVModalPageWrapper.java#5 $
*/

package ariba.ui.validation;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.widgets.ActionInterceptor;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWPage;

public final class AWVModalPageWrapper extends AWComponent implements ActionInterceptor
{
    public final static ActionHandler DisabledActionHandler = new ActionHandler(false, true, null);
    protected static final String ReturnPageKey = "AWXMReturnPage";

    public void awake ()
    {
        // Stash the page that preceded us the first time around
        AWComponent returnPage = returnPage(this);
        if (returnPage == null) {
            AWPage requestPage = requestContext().requestPage();
            if (requestPage != null) {
                returnPage = requestPage.pageComponent();
                pageComponent().dict().put(ReturnPageKey, returnPage);
            }
        }
    }

    /** the page we will return to by default */
    public static AWComponent returnPage (AWComponent component)
    {
        return (AWComponent)component.pageComponent().dict().get(ReturnPageKey);
    }

    /**
        Action interceptor interface
    */
    public ActionHandler overrideAction (String action, ActionHandler defaultHandler, AWRequestContext requestContext)
    {
        return DisabledActionHandler;
    }

    /* Default impls if okayAction and cancelAction aren't bound */
    public AWComponent okayClicked ()
    {
        return (hasBinding("okayAction")) ? (AWComponent)valueForBinding("okayAction") : returnPage(this);
    }

    public AWComponent cancelClicked ()
    {
        return (hasBinding("cancelAction")) ? (AWComponent)valueForBinding("cancelAction") : returnPage(this);
    }
}
