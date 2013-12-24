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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWGenericContainer.java#10 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import java.util.Map;
import ariba.util.core.StringUtil;
import java.lang.reflect.Field;
import static ariba.ui.aribaweb.core.AWComponent.RenderingListener.InterestLevel;

public class AWGenericContainer extends AWContainerElement
{
    protected static final AWEncodedString LeftAngleSlash = new AWEncodedString("</");
    protected static final AWEncodedString RightAngle = new AWEncodedString(">");
    private AWGenericElement _genericElement;

    // ** Thread Safety Considerations: This is shared but all ivars are immutable -- no locking required.

    public void init (String tagName, Map bindingsHashtable)
    {
        _genericElement = new AWGenericElement();
        _genericElement.init(tagName, bindingsHashtable, true);
        super.init(tagName, null);
    }

    public AWGenericElement _genericElement ()
    {
        return _genericElement;
    }

    public AWBinding[] allBindings ()
    {
        return _genericElement.allBindings();
    }

    public void setTemplateName (String name)
    {
        _genericElement.setTemplateName(name);
        super.setTemplateName(name);
    }

    public void setLineNumber (int line)
    {
        _genericElement.setLineNumber(line);
        super.setLineNumber(line);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        _genericElement.applyValues(requestContext, component);
        super.applyValues(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = _genericElement.invokeAction(requestContext, component);
        if (actionResults == null) {
            actionResults = super.invokeAction(requestContext, component);
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        InterestLevel interestLevel = InterestLevel.Interested;

        if (interestLevel == InterestLevel.Interested) {
            interestLevel = component.componentWillRender(requestContext, this, component);
        }
        AWEncodedString encodedTagName = _genericElement.tagNameInComponent(component);
        // let this method decide how to deal with null bytes
        _genericElement.renderResponse(requestContext, component, encodedTagName);
        super.renderResponse(requestContext, component);
        if (encodedTagName != null) {
            AWResponse response = requestContext.response();
            // hook for listeners to inject any more content
            if (interestLevel == InterestLevel.Interested) {
                interestLevel = component.componentClosingTag(requestContext, this, component);
            }
            response.appendContent(LeftAngleSlash);
            response.appendContent(encodedTagName);
            response.appendContent(RightAngle);
        }
        if (interestLevel == InterestLevel.Interested) {
            interestLevel = component.componentFinishedRender(requestContext, this, component);
        }

    }

    public String toString ()
    {
        return StringUtil.strcat(super.toString(), ":\"", _genericElement.staticTagName(), "\"");
    }

    protected Object getFieldValue (Field field)
      throws IllegalArgumentException, IllegalAccessException
    {
        try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }
}
