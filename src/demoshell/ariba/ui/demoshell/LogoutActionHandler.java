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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/LogoutActionHandler.java#5 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.widgets.ActionHandler;
import ariba.util.core.StringUtil;

public class LogoutActionHandler extends ActionHandler
{
    public LogoutActionHandler ()
    {
    }

    public AWResponseGenerating actionClicked (AWRequestContext requestContext)
    {
        return DemoShell.sharedInstance().mainPage(requestContext);

        /*   mdao -- Fix this!

        String appUrl = null;
        if (requestContext.request().isSecureScheme()) {
            appUrl = StringUtil.strcat(requestContext.application().adaptorUrlSecure(),
                                       "/",
                                       requestContext.application().name());
        }
        else {

            appUrl = StringUtil.strcat(requestContext.application().adaptorUrl(),
                                       "/",
                                       requestContext.application().name());
        }

        return AWSSOManager.getSSOManager().ssoClient().initiateLogout(requestContext,appUrl);
    */
    }
}

