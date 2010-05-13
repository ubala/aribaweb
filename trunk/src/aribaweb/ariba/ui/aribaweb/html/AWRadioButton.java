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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWRadioButton.java#14 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRecordingManager;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWXBasicScriptFunctions;
import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.util.fieldvalue.FieldValue;

public final class AWRadioButton extends AWComponent
{
    private static final String RadioNameKey = "awradioName";
    private static final String RadioSelectionKey = "awradioSelection";
    private static final String RadioIsRefreshKey = "awradioIsRefresh";
    private static final String RadioOnClickKey = "awradioOnClick";
    public AWEncodedString _elementId;
    public AWEncodedString _radioName;
    public Object _selection;
    public Object _value;

    // ** Thread Safety Considerations: see AWComponent.

    // Note: AWRadioButton is so specialized and unique that it doesn't make sense to support otherBindings on it.

    protected void awake ()
    {
        AWEnvironmentStack environmentStack = env();
        FieldValue envStackClassExtension = FieldValue.get(environmentStack);
        _radioName = (AWEncodedString)envStackClassExtension.getFieldValue(environmentStack, RadioNameKey);
        _selection = envStackClassExtension.getFieldValue(environmentStack, RadioSelectionKey);
        _value = valueForBinding(BindingNames.value);
    }

    protected void sleep ()
    {
        _elementId = null;
        _radioName = null;
        _selection = null;
        _value = null;
    }

    public void setFormValue (String formValue)
    {
        if (formValue.equals(_elementId.string())) {
            // the awradioValue will be popped by the AWRadioButtonEnvironment.
            env().push(AWRadioButtonEnvironment.RadioValueKey, _value);
        }
    }

    public boolean isSender ()
    {
        return _elementId.equals(requestContext().requestSenderId());    
    }

    public String isChecked ()
    {
        return ((_value == _selection) || ((_value != null) && _value.equals(_selection))) ? BindingNames.awstandalone : null ;
    }

    public String onClickString ()
    {
        // Order of precedence for getting the onClickString:
        // 1) if isRefreshRegion, that trumps all others
        // 2) if onClick is provided on AWRadioButton, that trumps...
        // 3) onClick provided on AWRadioButtonEnvironment, which, if specified,
        //    will provide an onClickString for all other radio buttons.
        String onClickString = null;

        Boolean isRefresh = (Boolean)env().peek(RadioIsRefreshKey);
        if (isRefresh != null && isRefresh.booleanValue()) {
            AWResponse response = requestContext().response();
            response.appendContent(AWConstants.Space);
            response.appendContent(AWConstants.OnClick);
            response.appendContent(AWConstants.Equals);
            response.appendContent(AWConstants.Quote);
            AWXBasicScriptFunctions.appendSubmitCurrentForm(requestContext(), _elementId);
            response.appendContent(AWConstants.Quote);
        }
        else {
            AWBinding onClickBinding = bindingForName(BindingNames.onClick, false);
            if (onClickBinding != null) {
                onClickString = stringValueForBinding(onClickBinding);
            }
            else {
                onClickString = (String)env().peek(RadioOnClickKey);
                if (onClickString == AWRadioButtonEnvironment.RadioDefaultKey) {
                    onClickString = null;
                }
            }
        }
        return onClickString;
    }

        // recording & playback
        // provide a better semantic key
    protected AWBinding _debugPrimaryBinding ()
    {
        return componentReference().bindingForName(BindingNames.value, parent());
    }

    protected void _debugRecordMapping (AWRequestContext requestContext, AWComponent component)
    {
        String semanticKey = _debugSemanticKey();
        String elementId = _elementId.string();
        semanticKey = AWRecordingManager.applySemanticKeyPrefix(requestContext, semanticKey, null);             
        AWRecordingManager.registerSemanticKey(elementId, semanticKey, requestContext);
    }

    protected boolean _debugRecordMappingInGenericElement ()
    {
        return false;
    }

    public Object disabled ()
    {
        AWRequestContext requestContext = requestContext();
        if (requestContext.isPrintMode() ||
            AWEditableRegion.disabled(requestContext)) {
            return Boolean.TRUE;
        }
        else {
            return valueForBinding(AWBindingNames.disabled);
        }
    }
}
