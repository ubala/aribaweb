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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWIFrame.java#10 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.StringUtil;

public final class AWIFrame extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.src, BindingNames.pageName, BindingNames.value, BindingNames.name, BindingNames.action,
    };
    public AWEncodedString _elementId;
    private AWEncodedString _frameName;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void sleep ()
    {
        _frameName = null;
        _elementId = null;
    }

    public AWEncodedString frameName ()
    {
        if (_frameName == null) {
            _frameName = encodedStringValueForBinding(BindingNames.name);
            if (_frameName == null) {
                _frameName = _elementId;
            }
        }
        return _frameName;
    }

    public String srcUrl ()
    {
        String srcUrl;
        AWBinding binding = bindingForName(BindingNames.src, false);

        if (binding != null) {
            srcUrl = (String)valueForBinding(binding);
        }
        else {
            srcUrl = AWComponentActionRequestHandler.SharedInstance.urlWithSenderId(requestContext(), _elementId);
        }

        // append the frameName onto the URL, so AW can know it is
        // operating in an iframe
        return StringUtil.strcat(
                srcUrl, "&", AWRequestContext.FrameNameKey, "=",
                this.frameName().toString());
    }

    public AWResponseGenerating getFrameContent ()
    {
        // This is same as AWFrame
        AWResponseGenerating actionResults = null;
        AWBinding pageNameBinding = null;
        AWBinding actionBinding = null;
        if ((pageNameBinding = bindingForName(BindingNames.pageName)) == null &&
                (actionBinding = bindingForName(BindingNames.action)) == null) {
            actionResults = (AWResponseGenerating)valueForBinding(BindingNames.value);
        }
        else {
            actionResults = AWGenericActionTag.evaluateActionBindings(this, pageNameBinding, actionBinding);
        }
        requestContext().setFrameName(frameName());
        return actionResults;
    }
}
