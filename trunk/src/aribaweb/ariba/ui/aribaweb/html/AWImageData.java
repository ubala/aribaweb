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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWImageData.java#5 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWEncodedString;

public final class AWImageData extends AWComponent
{
    private static final String[] SupportedBindingNames = {BindingNames.border, BindingNames.bytes, BindingNames.contentType};
    public AWEncodedString _elementId;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void sleep ()
    {
        _elementId = null;
        super.sleep();
    }

    public String url ()
    {
        return AWComponentActionRequestHandler.SharedInstance.urlWithSenderId(requestContext(), _elementId);
    }

    public AWResponse invokeAction ()
    {
        AWResponse response = application().createResponse();
        AWContentType contentType = (AWContentType)valueForBinding(BindingNames.contentType);
        response.setContentType(contentType);
        // byte[] imageBytes = (byte[])valueForBinding(BindingNames.bytes);
        response.setContent((byte[])valueForBinding(BindingNames.bytes));
        return response;
    }
}
