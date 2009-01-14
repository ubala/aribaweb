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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWTdElement.java#7 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWGenericElement;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWTaggedElement;
import ariba.ui.aribaweb.util.AWEncodedString;
import java.util.Map;

public final class AWTdElement extends AWTaggedElement
{
    private AWBinding _background;

    public void init (String tagName, Map bindingsHashtable)
    {
        _background = (AWBinding)bindingsHashtable.remove(BindingNames.background);
        if (_background == null) {
            _background = (AWBinding)bindingsHashtable.remove("BACKGROUND");
        }
        super.init(tagName, bindingsHashtable);
    }

    protected void appendAttributes (AWRequestContext requestContext, AWComponent component, AWResponse response, boolean isBrowserMicrosoft)
    {
        if (_background != null) {
            String filename = _background.stringValue(component);
            if (filename != null) {
                String imageUrl = AWBaseImage.imageUrl(requestContext, component, filename);
                AWEncodedString encodedImageUrl = AWEncodedString.sharedEncodedString(imageUrl);
                AWGenericElement.appendHtmlAttributeToResponse(response, component, BindingNames.background, encodedImageUrl);
            }
        }
        super.appendAttributes(requestContext, component, response, isBrowserMicrosoft);
    }
}
