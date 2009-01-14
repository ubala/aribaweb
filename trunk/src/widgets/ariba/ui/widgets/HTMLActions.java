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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/HTMLActions.java#1 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWDirectActionUrl;

public final class HTMLActions extends AWDirectAction
{
    private static final String RequestContextKey = "AWTMetaColumnActionsURL";
    private static final String ActionTargetKey = "AWTMetaColumnActionsTarget";
    static {
        // register all of our default handlers...
        ActionHandler.setHandler("navigation:http", new HTTPActionHandler());
        ActionHandler.setHandler("navigation:https", new HTTPActionHandler());
    }

    public static AWResponseGenerating handleAction (String actionUrlString, AWComponent component)
    {
        return handleAction(actionUrlString, null, component);
    }

    public static AWResponseGenerating handleAction (String actionUrlString,
                                                     String target,
                                                     AWComponent component)
    {

        /** We expect action URLs of the form "http:// ...", "cxml:// ...", <someScheme>://..."
         * We translate that into an internal action of the form "navigation:http" and
         * dispatch.  The full actionUrlString is available for processing in the
         */
        int pos = actionUrlString.indexOf("://");
        if (pos <= 0) return null;

        String actionName = "navigation:" + actionUrlString.substring(0, pos);

        ActionHandler handler = ActionHandler.resolveHandlerInComponent(actionName, component);
        if (handler == null) {
            ariba.ui.table.Log.table.debug("Unknown handler key: %s", actionUrlString);
            return null;
        }

        AWRequestContext requestContext = component.requestContext();
        requestContext.put(RequestContextKey, actionUrlString);
        if (target != null) {
            requestContext.put(ActionTargetKey, target);
        }

        return handler.actionClicked(requestContext);
    }

    public static String actionURLForRequest (AWRequestContext requestContext)
    {
        return (String)requestContext.get(RequestContextKey);
    }

    public static String actionTargetForRequest (AWRequestContext requestContext)
    {
        return (String)requestContext.get(ActionTargetKey);
    }


    static class HTTPActionHandler extends ActionHandler
    {
        public String url (AWRequestContext requestContext)
        {
            return actionURLForRequest(requestContext);
        }

        public boolean isInterrupting (AWRequestContext requestContext)
        {
            return false;
        }
    }

    public static String directActionURL (String action,
                                          AWRequestContext requestContext)
    {
        if (action.startsWith("http://") || action.startsWith("https://")) {
            return action;
        }
        return AWDirectActionUrl.fullUrlForDirectAction("direct/HTMLActions",
                requestContext, "action", action);
    }

    public AWResponseGenerating directAction ()
    {
        AWRequest request = request();
        String actionURL = request.formValueForKey("action");
        return handleAction(actionURL, null);
    }
}
