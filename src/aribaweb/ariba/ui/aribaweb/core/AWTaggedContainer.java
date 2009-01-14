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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWTaggedContainer.java#6 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import java.util.Map;

public class AWTaggedContainer extends AWContainerElement
{
    private AWTaggedElement _taggedElement;
    private AWBinding _omitTags;
    private AWBinding _emitTags;

    public void init (String tagName, Map bindingsHashtable)
    {
        _omitTags = (AWBinding)bindingsHashtable.remove(AWBindingNames.omitTags);
        _emitTags = (AWBinding)bindingsHashtable.remove(AWBindingNames.emitTags);
        _taggedElement = initTaggedElement(tagName, bindingsHashtable);
        super.init(tagName, null);
    }

    protected AWTaggedElement initTaggedElement (String tagName, Map bindingsHashtable)
    {
        AWTaggedElement taggedElement = new AWTaggedElement();
        taggedElement.init(tagName, bindingsHashtable);
        return taggedElement;
    }

    public AWTaggedElement taggedElement ()
    {
        return _taggedElement;
    }

    private boolean emitTags (AWComponent component)
    {
        boolean emitTags = true;
        if (_omitTags != null) {
            emitTags = !_omitTags.booleanValue(component);
        }
        else if (_emitTags != null) {
            emitTags = _emitTags.booleanValue(component);
        }
        return emitTags;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWEncodedString encodedTagName = encodedTagName();
        boolean isBrowserMicrosoft = component.isBrowserMicrosoft();
        AWResponse response = requestContext.response();
        boolean emitTags = emitTags(component);
        if (emitTags) {
            response.appendContent(AWConstants.LeftAngle);
            response.appendContent(encodedTagName);
            _taggedElement.appendAttributes(requestContext, component, response, isBrowserMicrosoft);
            appendAttributes(requestContext, component, response, isBrowserMicrosoft);
            response.appendContent(AWConstants.RightAngle);            
        }
        appendBody(requestContext, component, response, isBrowserMicrosoft);
        if (emitTags) {
            response.appendContent(AWGenericContainer.LeftAngleSlash);
            response.appendContent(encodedTagName);
            response.appendContent(AWConstants.RightAngle);
        }
    }

    protected void appendAttributes (AWRequestContext requestContext, AWComponent component, AWResponse response, boolean isBrowserMicrosoft)
    {
    }

    protected void appendBody (AWRequestContext requestContext, AWComponent component, AWResponse response, boolean isBrowserMicrosoft)
    {
        super.renderResponse(requestContext, component);
    }
}
