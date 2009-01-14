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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWRadioButtonEnvironment.java#17 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRecordingManager;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWEnvironmentStack;

public final class AWRadioButtonEnvironment extends AWComponent
{
    public static final String RadioValueKey = "awradioValue";
    public static final String RadioDefaultKey = "awradioDefault";
    public AWEncodedString _elementId;

    // ** Thread Safety Considerations: see AWComponent.

    protected void sleep ()
    {
        _elementId = null;
    }

    public AWEncodedString radioButtonName ()
    {
        AWEncodedString radioButtonName = encodedStringValueForBinding(BindingNames.name);
        if (radioButtonName == null) {
            radioButtonName = _elementId;
        }
        return radioButtonName;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        if (!booleanValueForBinding(BindingNames.omitTags) &&
            !AWEditableRegion.disabled(requestContext())) {
            AWEnvironmentStack environmentStack = env();
            environmentStack.push(RadioValueKey, RadioDefaultKey);
            super.applyValues(requestContext, component);
            // get the radioSelection from the environment (put there
            // by one of the RadioButtons) and push to the parent
            Object radioSelection = environmentStack.pop(RadioValueKey);
            if (RadioDefaultKey.equals(radioSelection)) {
                radioSelection = null;
            }
            else {
                while (!(RadioDefaultKey.equals(environmentStack.pop(RadioValueKey)))) {
                    // balance the stack
                }
            }
            setValueForBinding(radioSelection, BindingNames.selection);
        }
        else {
            super.applyValues(requestContext, component);
        }
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        boolean shouldPushSemanticKeyPrefix =
                AWConcreteApplication.IsAutomationTestModeEnabled
                && requestContext.isDebuggingEnabled()
                && !AWBindingNames.UseNamePrefixBinding;
        if (shouldPushSemanticKeyPrefix) {
            requestContext._debugPushSemanticKeyPrefix();
            String keyPrefix = _debugSemanticKey ();
            requestContext._debugSetSemanticKeyPrefix(keyPrefix);
        }
        super.renderResponse(requestContext, component);
        if (shouldPushSemanticKeyPrefix) {
            requestContext._debugPopSemanticKeyPrefix();

        }
    }

        // recording & playback
        // provide a better semantic key
    protected AWBinding _debugPrimaryBinding ()
    {
        return componentReference().bindingForName(BindingNames.selection, parent());
    }

    protected void _debugRecordMapping (AWRequestContext requestContext, AWComponent component)
    {
        if (!booleanValueForBinding(BindingNames.omitTags)) {
            String semanticKey = _debugSemanticKey();
            String elementId = radioButtonName().string();
            if (semanticKey != null) {
                semanticKey = AWRecordingManager.applySemanticKeyPrefix(requestContext, semanticKey, null);
                AWRecordingManager.registerSemanticKey(elementId, semanticKey, requestContext);
            }
        }
    }

    protected boolean _debugRecordMappingInGenericElement ()
    {
        return false;
    }
}


