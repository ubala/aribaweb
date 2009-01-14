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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/WizardActionTarget.java#2 $
*/

package ariba.ui.wizard.core;

/**
    A WizardActionTarget represents the destination for a wizard action.  Everytime
    an action is invoked, the wizard asks the wizard frame delegate where to go for
    this action.  The wizard frame delegate should return a WizardActionTarget if it
    wants to override the default destination.

    Currently, only two classes of WizardActionTarget are supported by the wizard.
    They are WizardFrame and UrlActionTarget. Clients of wizard frame work should
    not implement they own classes of WizardActionTarget.
    
    @aribaapi ariba
*/
public interface WizardActionTarget
{
    /**
        Returns the frame that is responsible for taking form values and
        handling actions originated from this target.  
        @aribaapi private 
    */
    public WizardFrame getOriginatingFrame ();
    
    /**
        Returns true if this ation target should terminate the current wizard.
        @aribaapi ariba
    */
    public boolean terminatesWizard ();
}
