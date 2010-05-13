/*
    Copyright 1996-2010 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Confirmation.java#13 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import java.util.List;

/**
 *
 * @aribaapi private
 */
public class Confirmation extends AWComponent
{
    public static final String ConfirmationId = "AWConfirmationId";
    public static final String ClientSideConfirmationIdList = "ClientSideConfirmationIdList";
    public static final String HideConfirmationId = "AWHideConfirmationId";

    public static final String LazyLoadConfirmation = "lazyLoadConfirmation";

    private static final String OkAction     = "okAction";
    private static final String CancelAction = "cancelAction";
    private static final String Validate     = "validate";

    public String _confId;

    protected Object _pushedErrorManagerState;
    protected Object _errorManagerKey;
    protected boolean _didPush = false;

    public boolean isClientSideConfirmation ()
    {
        boolean isClientSide = false;
        List idList= (List)env().peek(ClientSideConfirmationIdList);
        if (idList != null) {
            isClientSide = idList.contains(encodedStringValueForBinding(AWBindingNames.id));
        }

        return isClientSide;
    }

    public boolean renderConfirmation ()
    {
        return (showConfirmation() || isClientSideConfirmation());
    }

    public boolean useLazyDiv ()
    {
        // use lazy div if we're doing client side confirmation and either
        // the LazyLoadConfirmation binding does not exist or it's set to true
        // (basically default LazyLoad to true)
        return isClientSideConfirmation() &&
               ( !hasBinding(LazyLoadConfirmation) ||
                 booleanValueForBinding(LazyLoadConfirmation));
    }

    public boolean hideConfirmation ()
    {
        return Boolean.TRUE.equals(requestContext().get(HideConfirmationId));
    }

    public boolean showConfirmation ()
    {
        if (isClientSideConfirmation()) {
            return false;
        }

        AWEncodedString confId = (AWEncodedString)env().peek(ConfirmationId);
        if (confId == null) {
            return false;
        }

        Object idBinding = valueForBinding(AWBindingNames.id);
        if (idBinding instanceof String) {
            return idBinding.equals(confId.string());
        }
        else if (idBinding instanceof AWEncodedString) {
            return idBinding.equals(confId);
        }
        // todo: assert here
        return false;
    }

    public AWResponseGenerating cancelAction ()
    {
        hideConfirmation(requestContext());
        return (AWResponseGenerating)valueForBinding(CancelAction);
    }

    public AWResponseGenerating okAction ()
    {
        boolean validate = booleanValueForBinding(Validate);

        if (validate && errorManager().checkErrorsAndEnableDisplay()) {
            return null;
        }
        hideConfirmation(requestContext());
        return (AWResponseGenerating)valueForBinding(OkAction);
    }

    public boolean hasButtonsTemplate ()
    {
        return hasSubTemplateNamed("buttons");
    }

    //
    // Static Utility methods
    //

    public static final AWResponseGenerating showConfirmation (
        AWRequestContext requestContext, AWEncodedString confirmationId)
    {
        requestContext.pageComponent().env().push(Confirmation.ConfirmationId,
                                                  confirmationId);
        return null;
    }

    public static final AWResponseGenerating hideConfirmation (
        AWRequestContext requestContext)
    {
        // in the case of a client side confirmation, there will not be a confirmation
        // id in the env
        if (hasConfirmation(requestContext)) {
            requestContext.pageComponent().env().pop(ConfirmationId);
        }
        requestContext.put(HideConfirmationId, Boolean.TRUE);
        return null;
    }

    public static final boolean hasConfirmation (
            AWRequestContext requestContext)
    {
        return requestContext.pageComponent().env().peek(ConfirmationId) != null;
    }

    static final void setClientSideConfirmation (AWRequestContext requestContext,
                                                 AWEncodedString confirmationId)
    {
        AWEnvironmentStack env = requestContext.pageComponent().env();

        List idList= (List)env.peek(ClientSideConfirmationIdList);
        if (idList == null) {
            idList = ListUtil.list();
            env.push(Confirmation.ClientSideConfirmationIdList, idList);
        }
        idList.add(confirmationId);
    }

    /*
        Nested Error Manager Support...
     */
    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        _confId = StringUtil.strcat("conf", stringValueForBinding(BindingNames.id));
        pushErrorManager();
        super.renderResponse(requestContext, component);
        popErrorManager();
    }

    public void applyValues (AWRequestContext requestContext, AWComponent component)
    {
        pushErrorManager();
        super.applyValues(requestContext, component);
        popErrorManager();
    }

    public AWResponseGenerating invokeAction (AWRequestContext requestContext, AWComponent component)
    {
        pushErrorManager();
        AWResponseGenerating result = super.invokeAction(requestContext, component);
        popErrorManager();
        return result;
    }

    public void pushErrorManager ()
    {
        if (showConfirmation()) {
            _didPush = true;
            // I feel compelled to keep this component stateless, so we're keeping our one piece of state
            // via a key/value on the page
            _errorManagerKey = requestContext().currentElementId();
            Object last = page().get(_errorManagerKey);

            // push our own error manager (or null the first time through)
            _pushedErrorManagerState = page().pushErrorManager(last);
        }
    }

    public void popErrorManager ()
    {
        if (_didPush) {
            _didPush = false;
            // pop our error manager and remember it for later
            Object last = page().popErrorManager(_pushedErrorManagerState);
            page().put(_errorManagerKey, last);
            _pushedErrorManagerState = null;
            _errorManagerKey = null;
        }
    }
}
