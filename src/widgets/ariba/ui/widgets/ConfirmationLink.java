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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ConfirmationLink.java#4 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.html.AWHyperlink;
import ariba.ui.aribaweb.html.BindingNames;

// subclassed by network/service/apps/admin/ADPDirectoryHyperlink.java
public class ConfirmationLink extends AWHyperlink
{
    private static final String[] SupportedBindingNames = {
        BindingNames.pageName, BindingNames.action, BindingNames.onClick, BindingNames.omitTags,
        BindingNames.fragmentIdentifier, BindingNames.submitForm,
        BindingNames.target, BindingNames.scrollToVisible, BindingNames.href, BindingNames.senderId,
        BindingNames.onMouseDown, BindingNames.windowAttributes, "confirmationId", BindingNames.behavior
    };

    public AWEncodedString _confirmationId;

    // ** this is pushed by the AWGenericContainer
    public AWEncodedString _elementId;
    public AWEncodedString _senderId;
    public boolean _isSubmitForm;

    // Bindings
    public AWBinding _confirmationIdBinding;

//    public static final String ConfirmationBindingName = "confirmationId";

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    protected void sleep ()
    {
        super.sleep();
        _confirmationId = null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // if there is a confirmationId set, then use client side visibility
        _confirmationId = encodedStringValueForBinding(_confirmationIdBinding);
        if (_confirmationId != null) {
            Confirmation.setClientSideConfirmation(requestContext(), _confirmationId);
        }

        super.renderResponse(requestContext, this);
    }

}
