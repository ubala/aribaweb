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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/WizardFrameDelegate.java#2 $
*/

package ariba.ui.wizard.core;

/**
    The WizardFrameDelegate is called by the core wizard engine at various
    points to allow an application to override default behavior.

    @aribaapi ariba
*/
public interface WizardFrameDelegate
{
    /**
        Allows the WizardFrameDelegate to handle the given action.  The return
        value should be the frame to show next, or null if the delegate wants
        to defer to the default flow.
        
        @aribaapi ariba
    */
    public WizardActionTarget targetForAction (WizardAction action);

    /**
        This method is called before the frame is displayed.  It Allows the
        WizardFrameDelegate to execute any application specific code before
        the frame is to be displayed.
        @aribaapi ariba
    */
    public void prepareForResponse (WizardFrame wizardFrame);
}
