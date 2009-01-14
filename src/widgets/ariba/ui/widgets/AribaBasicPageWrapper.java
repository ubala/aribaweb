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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaBasicPageWrapper.java#23 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

public final class AribaBasicPageWrapper extends BrandedComponent
{
    // the key to retrieve the mastHead image from the string handler, if available.
    public static final String MastHeadProductImageKey = "mastHeadProductImage";
    public static final String MastHeadBannerImageKey = "mastHeadBannerImage";

    public static final String MastHeadHelpImageKey = "mastHeadHelpImage";
    public static final String MastHeadHomeImageKey = "mastHeadHomeImage";
    public static final String MastHeadLogoutImageKey = "mastHeadLogoutImage";
    public static final String MastHeadTraceLineImageKey ="mastHeadTraceLineImage";
    public static final String MastHeadCmdBarClassKey = "mastHeadCmdBarClass";
    public static final String NavTabClassKey = "navTabClass";

    public static final String HelpActionKey =   "help";
    public static final String HomeActionKey =   "home";
    public static final String LogoutActionKey = "logout";

    public static final String DefaultCmdBarClass = "cmdBarWrapper";
    public static final String DefaultNavTabClass = "navTabWrapper";

    protected String shortTemplateName ()
    {
        return "AribaBasicPageWrapper.htm";
    }

    public String cmdBarClass ()
    {
        return cmdBarClass(requestContext(), this);
    }

    public String navTabClass ()
    {
        return navTabClass(requestContext(), this);
    }

    public static String cmdBarClass(AWRequestContext requestContext,
                                     AWComponent component)
    {
        StringHandler handler =
            StringHandler.resolveHandlerInComponent(MastHeadCmdBarClassKey, component);
        return handler == null ? DefaultCmdBarClass :
                        handler.getString(requestContext);
    }

    public static String navTabClass (AWRequestContext requestContext,
                                     AWComponent component)
    {
        StringHandler handler =
            StringHandler.resolveHandlerInComponent(NavTabClassKey, component);
        String navTabClass = null;
        if (handler != null) {
            navTabClass = handler.getString(requestContext);
        }
        if (navTabClass == null) navTabClass = DefaultNavTabClass;
        return navTabClass;
    }
}
