/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/StringHandler.java#22 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

public abstract class StringHandler extends BaseHandler
{
    public static final String UserGreeting  = "userGreeting";
    public static final String UserDelegationInfo = "userDelegationInfo";
    public static final String UserInfo = "userInfo";
    // This string is customizable.  Make sure that string is html escaped.
    public static final String ContactInfo = "contactInfo";
    public static final String Home = "Home";
    public static final String Site = "Site";
    public static final String Help = "Help";
    public static final String RefreshSession = "RefreshSession";
    public static final String Documentation = "Documentation";
    public static final String Training = "Training";
    public static final String Support = "Support";
    public static final String QuickTour = "QuickTour";
    public static final String FeatureLink = "FeatureLink";
    public static final String Contact = "Contact";
    public static final String Logout = "Logout";
    public static final String Preferences = "Preferences";
    public static final String Undelegate = "Undelegate";
    public static final String ReturnToServiceManager = "ReturnToServiceManager";
    public static final String JumpToNavigation = "JumpToNavigation";
    public static final String JumpToContent = "JumpToContent";

    public static void setDefaultHandler (StringHandler handler)
    {
        BaseHandler.setDefaultHandler(StringHandler.class, handler);
    }

    public static void setHandler (String string, StringHandler handler)
    {
        BaseHandler.setHandler(string, StringHandler.class, handler);
    }

    public static StringHandler resolveHandlerInComponent (String string, AWComponent component)
    {
        return (StringHandler)resolveHandler(component, string, StringHandler.class);
    }

    public abstract String getString (AWRequestContext requestContext);
}
