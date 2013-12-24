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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PopupMenu.java#20 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWValidationContext;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.Fmt;

import java.util.List;

/*
 A PopupMenu is a container for PopupMenuItem's.  To define a menu, you only need to provide a menuId, and then
 define a series of PopupMenuItem's within the PopupMenu contiainer.  Since the menu definition may appear in a
 repetition and, therefore, must have a unique menuId for each menu rendered, you may also provide an index for
 the menuId which will be used by PopupMenu to build a menuId from the concateation of the menuId and the index.
 For example:

<AWFor list="$list" item="$currentItem" index="$currentIndex">
    <PopupMenu menuId="Move" index="$currentIndex">
        <PopupMenuItem action="$moveLeftClicked">\
            Move Left\
        </PopupMenuItem>
        <PopupMenuItem action="$moveRightClicked">\
            Move Right\
        </PopupMenuItem>
    </PopupMenu>
        :
 </AWFor>

 Of course, you will need to define a reference to the menu using a PopupMenuLink (or PopupMenuButton) and
 provide the same menuId (and opitonal index):

        :
 <PopupMenuLink menuId="Move" index="$currentIndex">\
     $currentItem\
 </PopupMenuLink>
        :

 There are two modes in which you can use these menus -- shared or 1:1.  The above axample shows how to use the
 menu in 1:1 mode (one menu per menu reference).  [Actually, in 1:1 mode, the PopupMenu and PopupMenuLink
 usually appear within the same repetition, so the context for both is identical]

 For 1:1 mode, it does not matter whether then PopupMenu comes before the PopupMenuLink since the context
 of the action bound to the PopupMenuItem will be the same in either case (assuming they are defined within
 the same repetition as they generally will be).  However, in shared-menu mode, the PopupMenuLink should appear
 before the PopupMenu so that the link can provide the proper setup before the action of the PopupMenuItem
 is invoked (more on this below).  Also in shared mode, the PopupMenu and its corresponding PopupMenuLink's
 are not within the same repetition (or context).

 What does it mean for a PopupMenuLink to 'setup' before the action of the PopupMenuItem?  Well, when you
 click on the PopupMenuLink in a shared scenario, that link represents some object that you are selecting
 on which you want one of the operations defined in the menu to operate.  However, since the context of
 the link is separate from the context of the Menu itself, you must stash the value(s) you want from the
 link's context so they'll be available in the PopupMenuItem's context.  To make this easy to do, you can
 define "assignment bindings" on the link itself and stash away these values.  An assignment binding is
 one where the left hand side of the binding expression is the name of a variable in your component.
 For example:

     <PopupMenuLink menuId="Foo" actionSetup="$actionSetup">
        :

 In this case, when this link is clicked, no action will be invoked, but the $currentItem will be evaluated
 and pushed into the "selectedItem" variable in your component and the same for $currentValue -> selectedValue.
 Of course, you may use any names you want on the left side as long as there's ivar's in your component with
 those names.  Now that the selectedItem and selectedValue are stashed away, you can use them in whatever
 actions are defined on the shared component that displays when this PopupMenuLink is clicked.

 */

public final class PopupMenu extends AWComponent
{
    private static String PopupMenuString = "PopupMenu";
    protected static final String MenuLinkSenderIdKey = AWComponentActionRequestHandler.SenderKey;
    private static final AWEncodedString MenuLinkSenderId = new AWEncodedString(PopupMenu.MenuLinkSenderIdKey);
    protected static final AWEncodedString MenuClickedFunctionName =
        new AWEncodedString("ariba.Menu.menuClicked");
    protected static final AWEncodedString ShowMenuFunctionName =
        new AWEncodedString("ariba.Menu.menuLinkOnClick");
    protected static final AWEncodedString KeyDownShowMenuFunctionName =
        new AWEncodedString("ariba.Menu.menuLinkOnKeyDown");
    protected static final AWEncodedString MenuKeyDownFunctionName =
        new AWEncodedString("ariba.Menu.menuKeyDown");
    protected static final AWEncodedString ActionNameKey = new AWEncodedString("pman");
    public AWBinding _menuIdBinding;
    public AWBinding _indexBinding;
    public boolean _hasCollapsed;

    protected static AWEncodedString menuId (AWComponent component, AWBinding menuIdBinding, AWBinding indexBinding)
    {
        AWEncodedString menuId = component.encodedStringValueForBinding(menuIdBinding);
        if (indexBinding != null) {
            Object indexObject = component.valueForBinding(indexBinding);
            String menuIdString = StringUtil.strcat(menuId.string(), AWUtil.toString(indexObject));
            menuId = AWEncodedString.sharedEncodedString(menuIdString);
        }
        return menuId;
    }

    public boolean isStateless ()
    {
        return true;
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    public AWEncodedString menuId ()
    {
        return PopupMenu.menuId(this, _menuIdBinding, _indexBinding);
    }

    protected static String actionName (AWRequest request)
    {
        return request.formValueForKey(ActionNameKey);
    }

    public AWEncodedString menuLinkSenderIdKey ()
    {
        return PopupMenu.MenuLinkSenderId;
    }

    public AWEncodedString actionNameKey ()
    {
        return ActionNameKey;
    }

    public static final String debug_MenuIDListKey = "debug_MenuIDListKey";
    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // check to make sure that multiple menus are not created with the same menu id!
        if (AWConcreteApplication.IsDebuggingEnabled) {
            List menuIds = (List)requestContext.get(debug_MenuIDListKey);
            if (menuIds == null) {
                menuIds = ListUtil.list();
                requestContext.put(debug_MenuIDListKey, menuIds);
            }
            AWEncodedString menuId = menuId();

            if (menuIds.contains(menuId)) {
                AWValidationContext validationContext = requestContext.validationContext();
                String msg = Fmt.S("Error: multiple menus found with the same menu id: %s", menuId);
                validationContext.addGeneralError(getMultipleMenusErrorMessage());
                ariba.ui.aribaweb.util.Log.dumpAWStack(component,msg);
            }

            menuIds.add(menuId);
        }
        _hasCollapsed = false;
        super.renderResponse(requestContext, component);
    }
    
    private String getMultipleMenusErrorMessage ()
    {
        String msg = "Multiple menus found with the same menu id";
        return localizedJavaString(PopupMenuString, 1,
                msg,
                AWConcreteApplication.SharedInstance.
                        resourceManager(this.preferredLocale()));
    }

    // may be called back during append by collapsed items to tell us to show expand control
    public static void flagHasCollapsedItem (AWComponent item)
    {
        ((PopupMenu)item.env().peek("popupMenu"))._hasCollapsed = true;
    }

    public boolean shouldAutoClose ()
    {
        return valueForBinding("closeTimeout") != null;
    }

    public int autoCloseTimeout ()
    {
        Integer result = (Integer)valueForBinding("closeTimeout");
        if (result < 3000) {
            return 3000;
        }
        return result;
    }
}
