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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ToggleBox.java#3 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWCycleable;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.util.core.Assert;


public class ToggleBox extends AWComponent
{

    private AWElement _headingContentElement = null;
    private AWElement _bodyContentElement = null;;
    private boolean _initialized = false;

    public boolean isStateless ()
    {
        return false;
    }

    public abstract static class ToggleBoxElement extends AWContainerElement
    {
        public abstract void initializeElement (ToggleBox toggleBox);
    }

    public void setHeadingContentElement (AWElement headingContentElement)
    {
        Assert.that(_headingContentElement == null, "Cannot have more than one heading content element.");
        _headingContentElement = headingContentElement;
    }

    public AWElement headingContentElement ()
    {
        return _headingContentElement;
    }

    public void setBodyContentElement (AWElement bodyContentElement)
    {
        Assert.that(_bodyContentElement == null, "Cannot have more than one body content element.");
        _bodyContentElement = bodyContentElement;
    }

    public AWElement bodyContentElement ()
    {
        return _bodyContentElement;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        initElements();
        super.renderResponse(requestContext, component);
    }

    private void initElements ()
    {
        if (!_initialized) {
            AWElement contentElement = componentReference().contentElement();
            if (contentElement != null) {
                if (contentElement instanceof AWTemplate) {
                    AWTemplate elementsTemplate = (AWTemplate)contentElement;
                    AWCycleable[] elementArray = elementsTemplate.elementArray();
                    int elementCount = elementArray.length;
                    for (int index = 0; index < elementCount; index++) {
                        AWElement currentElement = (AWElement)elementArray[index];
                        if ((currentElement != null)) {
                            if (currentElement instanceof ToggleBoxElement) {
                                ((ToggleBoxElement)currentElement).initializeElement(this);
                            }
                        }
                    }
                }
                else if (contentElement instanceof ToggleBoxElement) {
                    ((ToggleBoxElement)contentElement).initializeElement(this);
                }
            }
            _initialized = true;
        }
    }

}
