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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/component/WizardPage.java#4 $
*/

package ariba.ui.wizard.component;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.widgets.ActionInterceptor;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.wizard.core.WizardFrameDelegate;
import ariba.ui.wizard.core.WizardStep;
import ariba.util.core.MapUtil;
import ariba.util.core.PerformanceState;
import java.util.Map;

/**
    This the top level page component that shows current frame of the wizard
    @aribaapi private
*/
public class WizardPage extends AWComponent implements ActionInterceptor
{
    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/
    protected WizardFrame _frame;
    protected Wizard _wizard;
    private static Map WizardPagesByFrameType = MapUtil.map();

    /*-----------------------------------------------------------------------
        Public Accessors
      -----------------------------------------------------------------------*/

    public WizardFrame frame ()
    {
        return _frame;
    }

    public void setFrame (WizardFrame frame)
    {
        _frame = frame;
        _wizard = frame.getWizard();
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
            // we need to set the current frame in the
            // case of browser back
        _wizard.setCurrentActionTarget(_frame);
        WizardFrameDelegate frameDelegate = _frame.getDelegate();
        if (frameDelegate != null) {
            frameDelegate.prepareForResponse(_frame);
        }

        // For wizards we record the wizard label as the "Page" (even though
        // it's not) so we can record any frame as the "area"
        if (PerformanceState.threadStateEnabled()) {
            PerformanceState.getThisThreadHashtable().setDestinationPage(_wizard.getName());
            PerformanceState.getThisThreadHashtable().setDestinationArea(_frame.getName());
        }

        super.renderResponse(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        if (PerformanceState.threadStateEnabled()) {
            PerformanceState.getThisThreadHashtable().setSourcePage(_frame.getName());
            WizardStep step = _frame.getStep();
            // step could be null if it is the exit confirmation page.
            if (step != null && !step.isTopLevelStep()) {
                PerformanceState.getThisThreadHashtable().setSourceArea(step.getName());
            }
        }
        return super.invokeAction(requestContext, component);
    }

    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/
    /**
        Implements the ActionInterceptor interface
    */
    public ActionHandler overrideAction (String action,
                                         ActionHandler defaultHandler,
                                         AWRequestContext requestContext)
    {
        if (defaultHandler != null && defaultHandler.isInterrupting(requestContext)) {
            if (_frame == _wizard.getExitFrame()) {
                    // We are already on the exit frame, so ignore these links
                ActionHandler override = new ActionHandler(false, true, null);
                return override;
            }
            WizardExitActionHandler override =
                WizardExitActionHandler.createHandler(defaultHandler, _wizard);
            return override;
        }
        return null;
    }

    public final static void registerWizardPageForFrameType (String frameType,
                                                             String componentName)
    {
        synchronized (WizardPagesByFrameType) {
            WizardPagesByFrameType.put(frameType, componentName);
        }
    }

    public final static String wizardPageName (WizardFrame wizardFrame)
    {
        String type = wizardFrame.getType();
        String pageName = null;
        if (type != null) {
            pageName = (String)WizardPagesByFrameType.get(type);
        }
        if (pageName == null) {
            pageName = WizardPage.class.getName();
        }
        return pageName;
    }
}

