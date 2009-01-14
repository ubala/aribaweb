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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWCatch.java#7 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;

public final class AWCatch extends AWComponent
{
    public void _handleException (Exception exception)
    {
        String targetExceptionName = (String)valueForBinding(AWBindingNames.name);
        Class targetExceptionClass = AWUtil.classForName(targetExceptionName);
        if (targetExceptionClass.isAssignableFrom(exception.getClass())) {
            AWResponseGenerating actionResults = null;
            AWBinding pageNameBinding = bindingForName(AWBindingNames.pageName);
            if (pageNameBinding != null) {
                String pageName = (String)valueForBinding(pageNameBinding);
                actionResults = pageWithName(pageName);
            }
            else {
                AWBinding exceptionBinding = bindingForName(AWBindingNames.exception);
                if (exceptionBinding != null) {
                    setValueForBinding(exception, exceptionBinding);
                }
                actionResults = (AWResponseGenerating)valueForBinding(AWBindingNames.action);
            }
            throw new AWHandledException(actionResults);
        }
        else if (exception instanceof AWGenericException) {
            AWGenericException genericException = ((AWGenericException)exception);
            Throwable subexception = genericException.originalException();
            if ((subexception != null) && (subexception instanceof Exception)) {
                _handleException((Exception)subexception);
            }
            else {
                throw genericException;                
            }
        }
        else {
            throw new AWGenericException(exception);
        }
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        try {
            super.applyValues(requestContext, component);
        }
        catch (Exception exception) {
            _handleException(exception);
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        try {
            actionResults = super.invokeAction(requestContext, component);
        }
        catch (Exception exception) {
            _handleException(exception);
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        try {
            super.renderResponse(requestContext, component);
        }
        catch (Exception exception) {
            _handleException(exception);
        }
    }
}
