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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWStaticContent.java#8 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.GrowOnlyHashtable;
import java.util.Map;
import java.lang.reflect.Field;

public final class AWStaticContent extends AWContainerElement
{
    private GrowOnlyHashtable _staticContentsTable = new GrowOnlyHashtable();
    private AWBinding _key;

    public void init (String tagName, Map bindingsHashtable)
    {
        _key = (AWBinding)bindingsHashtable.remove(AWBindingNames.key);
        super.init(tagName, bindingsHashtable);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        return null;
    }

    private String renderContentString (AWRequestContext requestContext, AWComponent component)
    {
        AWResponse tempResponse = requestContext.application().createResponse(requestContext.request());
        AWResponse actualResponse = requestContext.temporarilySwapReponse(tempResponse);
        super.renderResponse(requestContext, component);
        String contentString = tempResponse.contentString();
        requestContext.restoreOriginalResponse(actualResponse);
        return contentString;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        Object key = _key.value(component);
        //logString("**** AWStaticContent key: " + key);
        String contentString = (String)_staticContentsTable.get(key);
        if (contentString == null) {
            synchronized(_staticContentsTable) {
                contentString = (String)_staticContentsTable.get(key);
                if (contentString == null) {
                    contentString = renderContentString(requestContext, component);
                    _staticContentsTable.put(key, contentString);
                    //logString("**** caching: " + key + " " + contentString);
                }
            }
        }
        requestContext.response().appendContent(contentString);
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
