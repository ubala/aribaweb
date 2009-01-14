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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWActionUrl.java#18 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import java.util.Map;
import java.lang.reflect.Field;

public final class AWActionUrl extends AWBindableElement
{
    private static final String AbsoluteUrlBindingName = "useAbsoluteUrl";

    private AWBinding _pageName;
    private AWBinding _action;
    private AWBinding _semanticKeyBinding;
    private AWBinding _absoluteUrl;

    // ** Thread Safety Considerations: although this is shared, the ivars are immutable.

    public void init (String tagName, Map bindingsHashtable)
    {
        _pageName = (AWBinding)bindingsHashtable.remove(AWBindingNames.pageName);
        _action = (AWBinding)bindingsHashtable.remove(AWBindingNames.action);
        if ((_action == null) && (_pageName == null)) {
            throw new AWGenericException(getClass().getName() + ": must have either 'pageName' or 'action' binding.");
        }
        _semanticKeyBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.semanticKeyBindingName());
        _absoluteUrl = (AWBinding)bindingsHashtable.remove(AbsoluteUrlBindingName);

        super.init(tagName, bindingsHashtable);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        AWEncodedString elementId = requestContext.nextElementId();
        if (elementId.equals(requestContext.requestSenderId())) {
            if (_pageName != null) {
                String pageName = _pageName.stringValue(component);
                actionResults = requestContext.pageWithName(pageName);
            }
            else {
                actionResults = (AWResponseGenerating)_action.value(component);
            }
            if (actionResults == null) {
                actionResults = requestContext.page().pageComponent();
            }
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWEncodedString elementId = requestContext.nextElementId();
        if (_absoluteUrl != null && _absoluteUrl.booleanValue(component)) {
            AWComponentActionRequestHandler.SharedInstance.appendFullUrlWithSenderId(requestContext, elementId);
        }
        else {
            AWComponentActionRequestHandler.SharedInstance.appendUrlWithSenderId(requestContext, elementId);
        }

        // record & playback
        // It is duplicated in AWGenericElement.
         if (requestContext._debugShouldRecord()) {
             String semanticKey = _semanticKeyBinding == null ?
                     AWRecordingManager.actionEffectiveKeyPathInComponent(_action, component) :
                     _semanticKeyBinding.stringValue(component);
             semanticKey = AWRecordingManager.applySemanticKeyPrefix(requestContext, semanticKey, null);
             AWRecordingManager.registerSemanticKey(elementId.string(), semanticKey, requestContext);
         }
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        // take an elementId to make sure that we're consistent in all 3 phases
        requestContext.nextElementId();
        super.applyValues(requestContext, component);
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
