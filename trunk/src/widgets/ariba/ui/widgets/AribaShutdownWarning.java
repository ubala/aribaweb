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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaShutdownWarning.java#10 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.util.AWBookmarker;
import ariba.ui.aribaweb.core.AWRedirect;
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

        String message = localizedJavaString(2,
            "Attention: Routine system maintenance has occurred on the site. You must " +
            "re-authenticate by logging off within {0} minutes of receiving this message. " +
            "Please click <i>Finish Work</i>, save your work and log out. <b>Once logged out, " +
            "you may immediately log back in and continue working.</b> You will be automatically " +
            "logged out in {1} minutes." /*  */);
        message = Fmt.Si(message, remainingShutdownPeriodInMinutes,
                         remainingShutdownPeriodInMinutes);
        return message;
    }

    public AWResponseGenerating finishWork ()
    {
        return Confirmation.hideConfirmation(requestContext());
    }

    public AWResponseGenerating redirect ()
    {
        AWRedirect redirect = null;
        AWBookmarker bookmarker = application().getBookmarker();
        AWRequestContext requestContext = requestContext();
        if (bookmarker.isBookmarkable(requestContext)) {
            String url = bookmarker.getURLString(requestContext);
            redirect = AWRedirect.getRedirect(requestContext, url);
            redirect.setAllowInternalDispatch(false);
        }
        session().terminate();
        return redirect;
    }
}
