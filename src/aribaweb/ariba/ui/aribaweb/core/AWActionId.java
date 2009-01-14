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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWActionId.java#11 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import java.util.Map;
import java.lang.reflect.Field;

/**
 * Should be used in conjunction with Request.invokeAction javascript:
 * Request.invokeAction('<AWActionID action="$something"/>')
 * Or, use Request.formatUrl('<AWActionID action="$something"/>') to get the equivalent
 * URL to AWActionUrl, but in a RefreshRegion-friendly way.
 */
public final class AWActionId extends AWBindableElement
{
    private AWBinding _action;
    private AWBinding _semanticKeyBinding;

    // ** Thread Safety Considerations: although this is shared, the ivars are immutable.

    public void init (String tagName, Map bindingsHashtable)
    {
        _action = (AWBinding)bindingsHashtable.remove(AWBindingNames.action);
        if (_action == null) {
            throw new AWGenericException(getClass().getName() + ": must have an 'action' binding.");
        }
        _semanticKeyBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.semanticKeyBindingName());
        super.init(tagName, bindingsHashtable);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        AWEncodedString elementId = requestContext.nextElementId();
        if (elementId.equals(requestContext.requestSenderId())) {

            actionResults = (AWResponseGenerating)_action.value(component);

            if (actionResults == null) {
                actionResults = requestContext.page().pageComponent();
            }
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWEncodedString elementId = requestContext.nextElementId();
        requestContext.response().appendContent(elementId);

        // record & playback
        // It is duplicated in AWGenericElement.
         if (requestContext._debugShouldRecord()) {
             String semanticKey = _semanticKeyBinding == null ?
                     AWRecordingManager.actionEffectiveKeyPathInComponent(_action,  component) :
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
