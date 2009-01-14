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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ModalWindowLink.java#10 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;

/*
 * Ok, how about a little documentation.  ModalWindowLink uses a hyperlink to initiate
 * a js call to open the modal window.  awOpenModalWindow js method takes in a element
 * id.  This element id is then sent as the "sender id" from the new window.  This
 * request (no frame name yet) will end up hitting the same page cache as the parent
 * window and therefor work through the invokeAction phase for the top page
 * on the page cache (ie the parent page).
 *
 * The ModalWindowLink that caused the window to be opened overrides invoke and explicitly
 * checks for a match against the sender id.  When this request is received, a new page
 * cache is created by setting the framename to (aw_<element id>).  At this point, two
 * bindings are evaluated.  "windowOpen" is evaluated to allow any data setup that is
 * required and then the "action" binding is evaluated to retrieve the actual content
 * for the new window.
 *
 * The modal window is closed by returning ModalWindowClose page as the response to a
 * action.  ModalWindowClose closes the child window and invokes the ModalWindow.modalComplete
 * method on the parent window.  ModalWindow.modalComplete then constructs a URL with the same
 * sender id that initiated the modal window (ie, the sender id for the ModalWindowLink)
 * along with the awshouldClose query param.  This request is handled by the parent
 * window page cache and again goes through the invokeAction phase for the top
 * page -- the page that contains the ModalWindowLink.
 *
 * The overridden invoke method in this component then cleans up the page cache for the
 * child window and evaluates the "windowCloseAction" method to determine what page to
 * display.
 */

public final class ModalWindowLink extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.action, BindingNames.omitTags, BindingNames.size,
        BindingNames.submitForm, "windowOpen", "windowRefresh", "tile",
        "windowCloseAction", BindingNames.width, BindingNames.height
    };

    public AWEncodedString _elementId;
    private int _size;

    //////////////////////
    // Stateless/full Support
    //////////////////////
    public boolean isStateless ()
    {
        return false;
    }

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public void sleep ()
    {
        _elementId = null;
        _size = BindingNames.SizeUndefined;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _elementId = requestContext().nextElementId();
        super.renderResponse(requestContext, component);
    }

    public boolean isRefreshRequest ()
    {
        // check if this is a refresh request from the modal window -- if so,
        // then spew out special hook up code to reconnect to modal.
        return _elementId.string().equals(
            requestContext().formValueForKey("awModalWindowId"));
    }

    ///////////////
    // AWCycleable
    ///////////////
    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        _elementId = requestContext().nextElementId();
        super.applyValues(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        _elementId = requestContext().nextElementId();
        String requestSenderId = requestContext.requestSenderId();
        boolean isSender = _elementId.equals(requestSenderId);

        if (isSender) {
            if (isRefreshRequest()) {
                valueForBinding("windowRefresh");
                return null;
            }
            else {
                AWResponseGenerating response = invokeAction();
                if (response == null) {
                    super.invokeAction(requestContext, component);
                }
                return response;
            }
        }
        else {
            return super.invokeAction(requestContext, component);
        }
    }

    public AWComponent invokeAction ()
    {
        AWComponent modalPageComponent = null;
        AWRequestContext requestContext = requestContext();
        String windowNameString = StringUtil.strcat("aw", _elementId.string());
        AWEncodedString windowName =
            AWEncodedString.sharedEncodedString(windowNameString);
        AWRequest request = requestContext.request();
        String shouldClose = request.formValueForKey("awshouldClose");
        if (shouldClose != null) {
            // Go down this path when the modal window closes
            // flush the page cache and cycle the current page
            session().removePageCache(windowName);
            modalPageComponent = (AWComponent)valueForBinding("windowCloseAction");
        }
        else {
            valueForBinding("windowOpen");
            modalPageComponent = (AWComponent)valueForBinding(BindingNames.action);
            // The "aw" prefix is required because (apparently) IE
            // doesn't accept window names that start with "_"
            requestContext().setFrameName(windowName);
        }
        return modalPageComponent;
    }

    private int size ()
    {
        if (_size == BindingNames.SizeUndefined) {
            String sizeString = stringValueForBinding(BindingNames.size);
            _size = BindingNames.translateSizeString(sizeString);
        }
        return _size;
    }

    public String height ()
    {
        String height = stringValueForBinding(BindingNames.height);
        if (height != null) {
            return height;
        }
        switch (size()) {
            case BindingNames.SizeSmall: {
                height = "0.5";
                break;
            }
            case BindingNames.SizeMedium: {
                height = "0.6";
                break;
            }
            case BindingNames.SizeLarge: {
                height = "0.8";
                break;
            }
        }
        return height;
    }

    public String width ()
    {
        String width = stringValueForBinding(BindingNames.width);
        if (width != null) {
            return width;
        }
        switch (size()) {
            case BindingNames.SizeSmall: {
                width = "0.55";
                break;
            }
            case BindingNames.SizeMedium: {
                width = "0.7";
                break;
            }
            case BindingNames.SizeLarge: {
                width = "0.85";
                break;
            }
        }
        return width;
    }

    public boolean tile ()
    {
        return booleanValueForBinding("tile");
    }

}

