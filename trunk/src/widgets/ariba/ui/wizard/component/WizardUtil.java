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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardUtil.java#2 $
*/

package ariba.ui.wizard.component;

import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.wizard.core.UrlActionTarget;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.core.WizardAction;
import ariba.ui.wizard.core.WizardActionTarget;
import ariba.ui.wizard.core.WizardFrame;
import ariba.util.core.Assert;

public final class WizardUtil
{
    /*----------------------------------------------------------
       list of keys
    -----------------------------------------------------------*/
        // key to WizardFrame.attributes for wizard page associate
        // with the frame
    public final static String WizardPageKey = "wzrd_wizardPage";

    public static AWComponent startWizard (Wizard wizard,
                                           AWRequestContext requestContext)
    {
        wizard.start();
        wizard.setPageCacheMark(requestContext.session().markPageCache());
        WizardFrame currentFrame = wizard.getCurrentFrame();
        return createWizardPage(currentFrame, requestContext);
    }

    public static AWComponent createWizardPage (WizardFrame frame,
                                                AWRequestContext requestContext)
    {
        WizardPage nextPage = (WizardPage)frame.getAttribute(WizardPageKey);
        if (nextPage == null) {
            String wizardPageName = WizardPage.wizardPageName(frame);
            nextPage = (WizardPage)pageWithName(wizardPageName, requestContext);
            nextPage.setFrame(frame);
            frame.setAttribute(WizardPageKey, nextPage);
        }
        else {
            nextPage.page().ensureAwake(requestContext);
        }

        return nextPage;
    }

    public static AWResponseGenerating invokeWizardAction (
        WizardFrame frame,
        WizardAction wizardAction,
        AWRequestContext requestContext)
    {
        Wizard wizard = frame.getWizard();
        WizardActionTarget target = wizard.invokeAction(wizardAction, requestContext);
        AWResponseGenerating targetComponent = null;
        if (target instanceof UrlActionTarget) {
            targetComponent = generateResponseForUrlTarget(
                frame, (UrlActionTarget)target, requestContext);
        }
        else if (target instanceof WizardFrame) {
            WizardFrame targetFrame = (WizardFrame)target;

                // clear up the action handler that could been
                // stashed by WizardExitActionHandler
            if (targetFrame == wizard.getExitFrame()) {
                WizardExitActionHandler.clearActionHandler(targetFrame);
            }
            targetComponent = generateResponseForFrameTarget(
                frame, targetFrame, requestContext);
        }
        else if (target instanceof ComponentActionTarget) {
            targetComponent = generateResponseForComponentTarget(
                frame, (ComponentActionTarget)target, requestContext);
        }
        return targetComponent;
    }

    public static AWResponseGenerating generateResponseForFrameTarget (
        WizardFrame currentFrame,
        WizardFrame targetFrame,
        AWRequestContext requestContext)
    {
        return createWizardPage(targetFrame, requestContext);
    }

    public static AWResponseGenerating generateResponseForUrlTarget (
        WizardFrame currentFrame,
        UrlActionTarget urlTarget,
        AWRequestContext requestContext)
    {
        Wizard wizard = currentFrame.getWizard();
        wizard.setCurrentActionTarget(urlTarget);
        AWResponseGenerating response = null;

        if (urlTarget.terminatesWizard()) {
            response = terminateWizard(wizard, requestContext);
        }
        if (response == null) {
            String redirectComponentName = AWRedirect.class.getName();
            AWRedirect redirect =
                (AWRedirect)(pageWithName(redirectComponentName, requestContext));

            String exitUrl = urlTarget.getUrl();
            redirect.setUrl(exitUrl);
            response = redirect;
        }
        return response;
    }

    public static AWResponseGenerating generateResponseForComponentTarget (
        WizardFrame currentFrame,
        ComponentActionTarget componentTarget,
        AWRequestContext requestContext)
    {
        Wizard wizard = currentFrame.getWizard();
        wizard.setCurrentActionTarget(componentTarget);
        AWResponseGenerating response = null;

        if (componentTarget.terminatesWizard()) {
            response = terminateWizard(wizard, requestContext);
        }
        if (response == null) {
            response = componentTarget.component();
        }
        return response;
    }

    private static AWResponseGenerating terminateWizard (Wizard wizard,
                                                 AWRequestContext requestContext)
    {
        requestContext.session().truncatePageCache(wizard.getPageCacheMark());
        WizardFrame exitFrame = wizard.getExitFrame();
        ActionHandler exitHandler = WizardExitActionHandler.actionHandler(exitFrame);
        wizard.cleanup();
        if (exitHandler != null) {
            return exitHandler.actionClicked(requestContext);
        }
        return null;
    }

    private static AWComponent pageWithName (String pageName,
                                             AWRequestContext requestContext)
    {
        AWApplication application = requestContext.application();
        return application.createPageWithName(pageName, requestContext);
    }

    public static AWComponent pageWithName (String pageName,
                                            Wizard wizard)
    {
        WizardFrame frame = wizard.getCurrentFrame();
        WizardPage wizardPage = (WizardPage)frame.getAttribute(WizardPageKey);
        Assert.that(wizardPage != null, "WizardPage is null for current frame");
        return wizardPage.pageWithName(pageName);
    }
}
