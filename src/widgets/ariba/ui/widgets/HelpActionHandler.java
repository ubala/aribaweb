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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/HelpActionHandler.java#20 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import java.util.List;
import java.util.Collections;

/**
    doc comment goes here

    @aribaapi
*/
public class HelpActionHandler extends ActionHandler
{
    protected static String SharedToken;
    protected static String DefaultHelpKey;
    protected static String HelpUrl;
    protected static String HelpWindowAttributes = 
            "scrollbars=no, status=yes, resizable=yes";

    private static List<String> _AllHelpAreas;

    static {
        /* default HelpAreas are Doc, Training and Support */
        List<String> list = ListUtil.list(StringHandler.Documentation,
                                          StringHandler.Support);
        _AllHelpAreas = Collections.unmodifiableList(list);
    }

    /**
        Returns all the enabled help areas. 
        @aribaapi ariba
    */
    public static List<String> getAllHelpAreas ()
    {
        return _AllHelpAreas;
    }

    /**
        @aribaapi ariba
    */
    public static void setAllHelpAreas (List<String> allHelpAreas)
    {
        _AllHelpAreas = allHelpAreas;
    }

    private String _helpUrl;

    public static boolean showHelpAction (AWRequestContext requestContext)
    {
        return !StringUtil.nullOrEmptyOrBlankString(HelpUrl);
    }

    /**
        Returns the product version if specified for this application in terms
        of how it connects to Help@Ariba.
        This method may return blank or null in which case the value is
        ignored. Else the version is appended to the help key with an
        underscore for the bounce to Help@Ariba.

        E.g. DefaultKey = Buyer; Version = 9r1;

        HelpKey used on the bounce is Buyer_9r1
    */
    protected String getProductVersion ()
    {
        return "";
    }

    protected String getUserType (AWRequestContext requestContext)
    {
        return "";
    }

    protected String getUserName (AWRequestContext requestContext)
    {
        return "";
    }

    protected String getRealmName (AWRequestContext requestContext)
    {
        return "";
    }

    protected String getUserLanguage (AWRequestContext requestContext)
    {
        return "";
    }

    protected List getFeatures (AWRequestContext requestContext)
    {
        return null;
    }

    protected String getANId (AWRequestContext requestContext)
    {
        return "";
    }

    protected String getAuxiliaryHelpUrl (AWRequestContext requestContext)
    {
        return "";
    }

    public void init (AWRequestContext requestContext, String helpKey)
    {
        if (helpKey == null) {
            helpKey = HelpActionHandler.DefaultHelpKey;
        }

        String version = getProductVersion();
        if (!StringUtil.nullOrEmptyString(version)) {
            helpKey = StringUtil.strcat(helpKey, "_", version);
        }

        String userType = getUserType(requestContext);
        String userName = getUserName(requestContext);
        String realmName = getRealmName(requestContext);
        String anId = getANId(requestContext);
        String userLang = getUserLanguage(requestContext);
        List features = getFeatures(requestContext);
        String featureString = "";
        if (features != null) {
            featureString = ListUtil.listToString(features, ",");
        }
        String area = requestContext != null ?
            (String)requestContext.get(AribaHelp.HelpArea) : "";

        String windowAttribute = HelpWindowAttributes;

        if (StringHandler.QuickTour.equals(area)) {
            windowAttribute =
                "scrollbars=no, status=yes, resizable=yes, width=845, height=635";
        }

        String helpUrl = HelpActionHandler.HelpUrl;
        String auxiliaryUrl = getAuxiliaryHelpUrl(requestContext);
        if (!StringUtil.nullOrEmptyString(auxiliaryUrl)) {
            helpUrl = StringUtil.strcat(helpUrl, auxiliaryUrl);
        }
        // need to re-insert the last %s for language, as this is substituted in
        // live during each request/response, ensuring we're using the appropriate
        // session locale
        String[] args = { helpUrl, SharedToken,
                          helpKey, userType, userName,
                          realmName, userLang, anId,
                          featureString, area, windowAttribute};
        _helpUrl = Fmt.S("javascript:ariba.Widgets.gotoDoc('%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s');"+
            "ariba.Event.cancelBubble(event);ariba.Menu.hideActiveMenu();", args);
    }

    public String getActionName ()
    {
        return AribaAction.HelpAction;
    }

    public String onClick (AWRequestContext requestContext)
    {
        return Fmt.S(_helpUrl, getUserLanguage(requestContext));
    }

    public String target (AWRequestContext requestContext)
    {
        // override to return null -- new window opened by js in url()
        return null;
    }

    public String url (AWRequestContext requestContext)
    {
        return null;
    }

    // indicates whether or not the action is an external action.  If so, then the
    // AribaAction is expected to treat URL's, etc. as external actions.
    public boolean isExternal (AWRequestContext requestContext)
    {
        return true;
    }

    public boolean useComponentAction (AWRequestContext requestContext)
    {
        return false;
    }

    public List<String> getHelpAreas (AWRequestContext requestContext)
    {
        return _AllHelpAreas;
}
}
