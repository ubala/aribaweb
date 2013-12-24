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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BindingNames.java#56 $
*/

package ariba.ui.widgets;

public class BindingNames extends ariba.ui.aribaweb.html.BindingNames
{
    public static final int SizeUndefined = 0;
    public static final int SizeSmall = 1;
    public static final int SizeMedium = 2;
    public static final int SizeLarge = 3;

    public static int translateSizeString (String sizeString)
    {
        int size = SizeMedium;
        if (sizeString != null) {
            if (sizeString.equalsIgnoreCase("small")) {
                size = SizeSmall;
            }
            else if (sizeString.equalsIgnoreCase("medium")) {
                size = SizeMedium;
            }
            else if (sizeString.equalsIgnoreCase("large")) {
                size = SizeLarge;
            }
        }
        return size;
    }

    public static final String addContentMargin = "addContentMargin";
    public static final String align            = "align";
    public static final String allowsWrapping   = "allowsWrapping";
    public static final String allSelected      = "allSelected";
    public static final String applicationCSS   = "applicationCSS";
    public static final String banner           = "banner";
    public static final String bodyClass        = "bodyClass";
    public static final String bodyHeader       = "bodyHeader";
    public static final String buttonClass      = "buttonClass";
    public static final String buttonWrapperStyle = "buttonWrapperStyle";
    public static final String buttonOverClass  = "buttonOverClass";
    public static final String commands         = "commands";
    public static final String calendarDate     = "calendarDate";
    public static final String dateFactory      = "dateFactory";
    public static final String disableHomeAction = "disableHomeAction";
    public static final String disableHelpAction = "disableHelpAction";
    public static final String disableLogoutAction = "disableLogoutAction";
    public static final String disableUndelegateAction = "disableUndelegateAction";
    public static final String disableProfile   = "disableProfile";
    public static final String isSidebarVisible = "isSidebarVisible";
    public static final String hasSidebarNotch  = "hasSidebarNotch";
    public static final String disableAboutBox  = "disableAboutBox";
    public static final String enabled          = "enabled";
    public static final String formActionUrl    = "formActionUrl";
    public static final String formEncodingType = "formEncodingType";
    public static final String formName         = "formName";
    public static final String hasForm          = "hasForm";
    public static final String hasSubsteps      = "hasSubsteps";
    public static final String helpKey          = "helpKey";
    public static final String image            = "image";
    public static final String isDefault        = "isDefault";
    public static final String isNavigationBarVisible = "isNavigationBarVisible";
    public static final String isSelected       = "isSelected";
    public static final String leftMargin       = "leftMargin";
    public static final String labelRight       = "labelRight";
    public static final String loginForm        = "loginForm";
    public static final String multiSelect      = "multiSelect";
    public static final String position         = "position";
    public static final String preferencesEnabled = "preferencesEnabled";
    public static final String globalNavPrefEnabled = "globalNavPrefEnabled";
    public static final String overrideGlobalBanner = "overrideGlobalBanner";
    public static final String overrideGlobalCommandBar = "overrideGlobalCommandBar";
    public static final String rawForm          = "rawForm";
    public static final String selectAllUrl     = "selectAllUrl";
    public static final String selectedStep     = "selectedStep";
    public static final String searchAction     = "searchAction";
    public static final String shouldOpen       = "shouldOpen";
    public static final String showFooterMessage= "showFooterMessage";
    public static final String showCancel       = "showCancel";
    public static final String showOk           = "showOk";
    public static final String showSelectAll    = "showSelectAll";
    public static final String showSelections   = "showSelections";
    public static final String showTopLine      = "showTopLine";
    public static final String sortAction       = "sortAction";
    public static final String sortDirection    = "sortDirection";
    public static final String sortable         = "sortable";
    public static final String state            = "state";
    public static final String step             = "step";
    public static final String stepAction       = "stepAction";
    public static final String stepIndex        = "stepIndex";
    public static final String stepIsNumbered   = "stepIsNumbered";
    public static final String stepIsVisible    = "stepIsVisible";
    public static final String stepLabel        = "stepLabel";
    public static final String steps            = "steps";
    public static final String stepName         = "stepName";
    public static final String tagId            = "tagId";
    public static final String toc              = "toc";
    public static final String tocHeader        = "tocHeader";
    public static final String visibleStepCount = "visibleStepCount";
    public static final String menuId           = "menuId";
    public static final String onMouseOver      = "onMouseOver";
    public static final String onMouseOut       = "onMouseOut";
    public static final String isBrandStyle     = "isBrandStyle";
    public static final String indentation      = "indentation";
    public static final String imageName        = "imageName";
    public static final String selectAction     = "selectAction";
    public static final String selectionSource  = "selectionSource";
    public static final String showCheck        = "showCheck";
    public static final String showBullet       = "showBullet";
    public static final String actionSetup      = "actionSetup";
    public static final String hilite           = "hilite";
    public static final String isLazy           = "isLazy";
    public static final String isClickable      = "isClickable";
    public static final String omitWrapperFrame = "omitWrapperFrame";
    public static final String clientFormat     = "clientFormat";
    public static final String okAction         = "okAction";
    public static final String cancelAction     = "cancelAction";
    public static final String windowClosed     = "windowClosed";
    public static final String prefix           = "prefix";
    public static final String ignoreCase       = "ignoreCase";
    public static final String showTip          = "showTip";
    public static final String basic            = "basic";
    public static final String removeContentLeftRightMargin = "removeContentLeftRightMargin";
}
