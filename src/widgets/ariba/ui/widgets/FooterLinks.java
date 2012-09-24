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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/FooterLinks.java#4 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.StringUtil;

/**
    Provides a method for applications to register a footer links component.
    
    @aribaapi
*/
public class FooterLinks extends AWComponent
{
    private static String FooterLinksComponent = null;
    private static String FooterMenusComponent = null;
    private static Object HideFooter = new Object();
    
    private static boolean _FooterEnabled = true;
    
    /**
        @aribaapi ariba
    */
    public static boolean isFooterEnabled ()
    {
        return _FooterEnabled;
    }
    
    /**
        @aribaapi ariba
    */
    public static void setFooterEnabled (boolean value) 
    {
        _FooterEnabled = value;
    }

    public boolean doubleHeight;

    public static void registerFooterLinksComponent (String componentName)
    {
        FooterLinksComponent = componentName;
    }

    public boolean hasFooterLinks ()
    {
        if (!_FooterEnabled) {
            return false;    
        }
        PageWrapper page = PageWrapper.instance(this);
        boolean disableFooter = page != null && 
            page.booleanValueForBinding("disableFooter");
        return !StringUtil.nullOrEmptyOrBlankString(FooterLinksComponent) &&
                session(false) != null &&
                session().dict().get(HideFooter) == null &&
                !disableFooter;                
    }

    public String footerLinksComponent ()
    {
        return FooterLinksComponent;
    }

    public boolean hasFooterMenus ()
    {
        return !StringUtil.nullOrEmptyOrBlankString(FooterMenusComponent);
    }

    public static void registerFooterMenusComponent (String componentName)
    {
        FooterMenusComponent = componentName;
    }

    public String footerMenusComponent ()
    {
        return FooterMenusComponent;
    }

    public void hideFooter ()
    {
        hideFooter(session());
    }
    
    public static void hideFooter (AWSession session)
    {
        session.dict().put(HideFooter, HideFooter);
    }
    
    public static void showFooter (AWSession session)
    {
        session.dict().remove(HideFooter);
    }    
}

