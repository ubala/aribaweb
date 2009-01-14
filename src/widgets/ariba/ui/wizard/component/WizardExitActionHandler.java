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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardExitActionHandler.java#2 $
*/

package ariba.ui.wizard.component;

import ariba.ui.widgets.ActionHandler;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;

public final class WizardExitActionHandler extends ActionHandler
{
    public final static String ExitActionHandlerKey = "wzrd_exitActionHandler";
        
    private ActionHandler _defaultHandler;
    private Wizard        _wizard;
    
    public static WizardExitActionHandler createHandler
        (ActionHandler defaultHandler, Wizard wizard)
    {
            // should be safe to cache by defaultHandler.
            // since wizard page is the top level page, the
            // default action handler instances should
            // be the ones registered with the application.
        WizardFrame exitFrame = wizard.getExitFrame();
        WizardExitActionHandler handler =
            (WizardExitActionHandler)exitFrame.getAttribute(defaultHandler);
        if (handler == null) {
            handler = new WizardExitActionHandler(defaultHandler, wizard);
            exitFrame.setAttribute(defaultHandler, handler);
        }
        return handler;
    }
    
    public WizardExitActionHandler (ActionHandler defaultHandler, Wizard wizard)
    {
        _wizard = wizard;
        _defaultHandler = defaultHandler;
    }
    
    public AWResponseGenerating actionClicked (AWRequestContext requestContext)
    {
        WizardFrame exitFrame = _wizard.getExitFrame();
        AWResponseGenerating response = WizardUtil.invokeWizardAction
            (_wizard.getCurrentFrame(),
             _wizard.exit,
             requestContext);
        setActionHandler(exitFrame, _defaultHandler);
        return response;
    }
    
    
    public static void setActionHandler (WizardFrame exitFrame,
                                         ActionHandler actionHandler)
    {
        exitFrame.setAttribute(ExitActionHandlerKey, actionHandler);
    }

    public static void clearActionHandler (WizardFrame exitFrame)
    {
        exitFrame.removeAttribute(ExitActionHandlerKey);
    }

    public static ActionHandler actionHandler (WizardFrame exitFrame)
    {
        return (ActionHandler)exitFrame.getAttribute(ExitActionHandlerKey);
    }
}

