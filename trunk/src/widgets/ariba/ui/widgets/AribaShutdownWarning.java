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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaShutdownWarning.java#12 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWShutdownState;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Fmt;

/**
    AW component that renders the shutdown warning message as needed. 
    @aribaapi ariba
*/
public class AribaShutdownWarning extends AWComponent
{
    public static final AWEncodedString AribaShutdownWarningConfId =
        AWEncodedString.sharedEncodedString("AribaShutdownWarningConfId");

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        boolean shouldRender = shouldRender();
        if (shouldRender) {
            session().getShutdownWarning();
            Confirmation.showConfirmation(requestContext, AribaShutdownWarningConfId);
        }
        super.renderResponse(requestContext, component);
    }

    private boolean shouldRender ()
    {
        // Should render warning dialog box only if:
        // 1) we are in the shutdown warning period
        // AND
        // 2) there is a warning to show
        // AND
        // 3) there are no other server side confirmation on the page.
        return application().monitorStats().isInShutdownWarningPeriod() &&
               session().hasShutdownWarning() &&
               !Confirmation.hasConfirmation(requestContext());
    }

    public String warningMessage ()
    {
        // (full shutdown time - warning time  60-30?
        // how long any user is going to be logged in before the first warning occurs
        int toShutdownWarningInMinutes =
            (int) (AWShutdownState.GracePeriod - AWShutdownState.WarningPeriod) / 1000 / 60;

        // how much time until shutdown
        int remainingShutdownPeriodInMinutes =
            (int)application().monitorStats().remainingShutdownPeriod() / 1000 / 60;
        int msg = 2;
        
        if (showRefreshSession()) {
            msg = 1;
        }
        String message = localizedJavaString(msg,
            "<p><b>Attention:</b> Routine system maintenance has occurred on the site " +
            "and we need to refresh your login session.</p><p>To refresh, please click " +
            "the <i>Refresh Session</i> button. <b>Note: you will not be logged out." +
            "</b></p><p>If you are in the middle of some work, you may click <i>Finish Work</i>, " +
            "complete what you are doing and click the <i>Refresh Session</i> link in the " +
            "top-right of the page.</p><p>Your session will be refreshed automatically in {0} minutes.</p>");
        message = Fmt.Si(message, remainingShutdownPeriodInMinutes,
                         remainingShutdownPeriodInMinutes);
        return message;
    }

    public boolean showRefreshSession ()
    {
        ConditionHandler handler = 
            ConditionHandler.resolveHandlerInComponent("disableReturnToHome", this);
        return handler.evaluateCondition(requestContext());
    }

    public AWResponseGenerating finishWork ()
    {
        return Confirmation.hideConfirmation(requestContext());
    }

    public AWResponseGenerating redirect ()
    {
        ActionHandler handler = ActionHandler.resolveHandler("refreshSession");
        return handler.actionClicked(requestContext());
    }
}
