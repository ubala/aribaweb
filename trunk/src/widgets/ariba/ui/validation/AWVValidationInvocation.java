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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVValidationInvocation.java#10 $
*/
package ariba.ui.validation;

import java.util.Map;
import ariba.ui.aribaweb.core.AWBindableElement;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWMissingBindingException;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWIf;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.html.BindingNames;

public final class AWVValidationInvocation extends AWBindableElement
{
    private AWBinding _object;
    private AWBinding _validator;
    private AWBinding _errorKey;
    private AWBinding _isValid;
    private AWBinding _message;

    // ** Thread Safety Considerations: All ivars are immutable so no locking required for this class.

    public void init (String tagName, Map bindingsHashtable)
    {
        _object = (AWBinding)bindingsHashtable.remove(AWBindingNames.target);
        _validator = (AWBinding)bindingsHashtable.remove(BindingNames.validator);
        _errorKey = (AWBinding)bindingsHashtable.remove(BindingNames.errorKey);
        _isValid = (AWBinding)bindingsHashtable.remove(BindingNames.isValid);
        _message = (AWBinding)bindingsHashtable.remove(BindingNames.message);

        if (_isValid == null && (_validator == null || _object == null)) {
            throw new AWMissingBindingException(getClass().getName() + ": Missing binding -- must have either \"isValid\" or both \"validator\" and \"object\" bindings.");
        }
        super.init(tagName, bindingsHashtable);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        String error = null;

        if (_validator != null) {
            AWVValidator validator = (AWVValidator)_validator.value(component);
            Object object = _object.value(component);
            error = validator.validateObject(component, object);
        }
        else {
            boolean isValid = AWIf.evaluateConditionInComponent(_isValid, component, false);
            if (!isValid) {
                error = _message.stringValue(component);
            }
        }

        if (error != null) {
            String errorKey;

            if (_errorKey != null) {
                errorKey = _errorKey.stringValue(component);
            }
            else {
                errorKey = AWErrorManager.GeneralErrorKey;
            }
            component.recordValidationError(errorKey, error, null);
        }
    }
}
