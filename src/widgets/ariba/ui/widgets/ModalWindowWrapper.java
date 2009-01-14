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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ModalWindowWrapper.java#9 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.util.core.SystemUtil;

// ** TODO: merge this with ModalPageWrapper, if possible.

public final class ModalWindowWrapper extends AWComponent
{
    public final static String EnvironmentKey = "ModalWindowWrapper";
    public RuntimeException _runtimeException;

    private static final String RefreshParentBindingName = "refreshParent";

    // Make this component stateful so that we can retain the _runtimeException.
    public boolean isStateless ()
    {
        return false;
    }

    public static AWComponent closeModalWindowPage (AWRequestContext requestContext)
    {
        return requestContext.pageWithName(ModalWindowClose.ClassName);
    }

    public AWComponent okClicked ()
    {
        return (AWComponent)valueForBinding(BindingNames.okAction);
    }

    public AWComponent cancelClicked ()
    {
        return (AWComponent)valueForBinding(BindingNames.cancelAction);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        try {
            super.applyValues(requestContext, component);
        }
        catch (RuntimeException runtimeException) {
            _runtimeException = runtimeException;
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        try {
            if (_runtimeException == null) {
                return super.invokeAction(requestContext, component);
            }
        }
        catch (RuntimeException runtimeException) {
            _runtimeException = runtimeException;
        }
        return null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        try {
            super.renderResponse(requestContext, component);

            // if the refreshParent binding was true, then set it back to false
            if (refreshParent()) {
                setValueForBinding(false, RefreshParentBindingName);
            }
        }
        catch (RuntimeException runtimeException) {
            ModalWindowExceptionPage modalWindowExceptionPage = (ModalWindowExceptionPage)
                    pageWithName(ModalWindowExceptionPage.class.getName());
            modalWindowExceptionPage._runtimeException = runtimeException;
            AWResponse response = application().createResponse();
            modalWindowExceptionPage.generateResponse(response);
            requestContext.setResponse(response);
        }
        finally {
            _runtimeException = null;
        }
    }

    public String stackTrace ()
    {
        return SystemUtil.stackTrace(_runtimeException);
    }

    public boolean refreshParent ()
    {
        return booleanValueForBinding((RefreshParentBindingName));
    }

    public static boolean isInModalWindow (AWComponent component)
    {
        ModalWindowWrapper instance =
            (ModalWindowWrapper)component.env().peek(EnvironmentKey);
        return instance != null;
    }


}
