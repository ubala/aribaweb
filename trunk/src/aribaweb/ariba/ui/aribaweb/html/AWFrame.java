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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWFrame.java#10 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;

public final class AWFrame extends AWComponent
{
    private static final String DisableFramePageCache = "disableFramePageCache";
    private static final String[] SupportedBindingNames =
        {BindingNames.pageName, BindingNames.action, BindingNames.value,
         BindingNames.name, DisableFramePageCache};
    public AWEncodedString _elementId;
    private AWEncodedString _frameName;

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void awake ()
    {
        _frameName = AWUtil.UndefinedEncodedString;
    }

    protected void sleep ()
    {
        _frameName = null;
        _elementId = null;
    }

    public AWEncodedString frameName ()
    {
        if (_frameName == AWUtil.UndefinedEncodedString) {
            _frameName = encodedStringValueForBinding(BindingNames.name);
            if (_frameName == null) {
                _frameName = _elementId;
            }
        }
        return _frameName;
    }

    public String srcUrl ()
    {
        return AWComponentActionRequestHandler.SharedInstance.urlWithSenderId(requestContext(), _elementId);
    }

    public AWResponseGenerating getFramePage ()
    {
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
        if (actionResults == null) {
            throw new AWGenericException("Frame page evaluated to null -- results in infinte recursion if this exception weren't thrown.");
        }

        if (!booleanValueForBinding(DisableFramePageCache)) {
            requestContext().setFrameName(frameName());
        }
        else {
            requestContext().setFrameName(requestContext().TopFrameName);
        }
        return actionResults;
    }

    public boolean isSender ()
    {
        return _elementId.equals(requestContext().requestSenderId());
    }
}
