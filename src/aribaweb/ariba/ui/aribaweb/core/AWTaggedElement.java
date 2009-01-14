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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWTaggedElement.java#7 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import java.util.Map;

public class AWTaggedElement extends AWBindableElement
{
    private AWBinding _otherBindings;
    private AWBindingDictionary _unrecognizedBindingsDictionary;

    public void init (String tagName, Map bindingsHashtable)
    {
        if (AWGenericElement.hasGenericElementAttributes(bindingsHashtable)) {
            throw new AWGenericException(getClass().getName() + ": contains AWGenericElementAttributes. " + bindingsHashtable);
        }
        _otherBindings = (AWBinding)bindingsHashtable.remove(AWBindingNames.otherBindings);
        _unrecognizedBindingsDictionary = AWGenericElement.escapedBindingsDictionary(bindingsHashtable);
        super.init(tagName, null);
    }

    protected void appendAttributes (AWRequestContext requestContext, AWComponent component, AWResponse response, boolean isBrowserMicrosoft)
    {
        AWGenericElement.appendOtherBindings(response, component, _unrecognizedBindingsDictionary, _otherBindings);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        String tagName = tagName();
        AWResponse response = requestContext.response();
        response.appendContent("<");
        response.appendContent(tagName);
        boolean isBrowserMicrosoft = component.isBrowserMicrosoft();
        appendAttributes(requestContext, component, response, isBrowserMicrosoft);
        response.appendContent(">");
    }
}
