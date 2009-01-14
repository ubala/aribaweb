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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/WizardDelegate.java#2 $
*/

package ariba.ui.wizard.core;

import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.core.AWStringLocalizer;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.i18n.LocalizedJavaString;
import ariba.util.core.StringUtil;

import java.util.Map;

/**
    The WizardDelegate is called by the core wizard engine to at various
    points to allow an application to override default behaviors.

    @aribaapi ariba
*/
public abstract class WizardDelegate
{
    AWResourceManager _resourceManager = null;
    /**
        let wizarddelegate have a chance to do initialization work before the
        wizard starts.  this method is invoked right before wizard
        displays the first step.
    */
    public void initialize (Wizard wizard)
    {
        _resourceManager = wizard.resourceManager();
    }

    /**
        Allows the WizardDelegate to provide the WizardFrameDelegate instance
        for a given frame.  If this method returns null, then the static
        declaration from the XML meta-data is used.

        @aribaapi ariba
    */
    public WizardFrameDelegate delegateForFrame (WizardFrame frame)
    {
        return null;
    }


    /**
        Allows the WizardDelegate to handle the given action.  The return
        value should be the frame to show next, or null if the delegate wants
        to defer to the default flow.

        @aribaapi ariba
    */
    public WizardActionTarget targetForAction (WizardAction action)
    {
        return null;
    }

    /**
        Allows the WizardDeleget to provide the localized string for labels and hints
    */

    public String localizedStringForKey (String key)
    {
        return localizedStringForKey(key, null);
    }

    public String localizedStringForKey (String key, Wizard wizard)
    {
        if (key.startsWith("$[")) {
            int indexOfRightBrace = key.indexOf(']');
            String originalString = key.substring(indexOfRightBrace + 1);
            String keyString = key.substring(2, indexOfRightBrace);
            String value = null;
            if (!StringUtil.nullOrEmptyOrBlankString(keyString)) {
                AWStringLocalizer localizer = AWConcreteApplication.SharedInstance.getStringLocalizer();
                Map map = localizer.getLocalizedStrings(stringFile(), stringGroupName(), _resourceManager);
                if (map != null) {
                    value = (String)map.get(keyString);
                }
            }
            return value == null ? originalString : value;
        }
        return key;
    }

    /**
        Determines the name of the strings resource file for the wizard.
    */
    abstract public String stringFile ();

    /**
        Determines the strings group name (analogous to a component) for the strings in the wizard.
    */
    abstract public String stringGroupName ();
}


