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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXWizardContentSwitch.java#4 $
*/

package ariba.ui.demoshell;

import ariba.ui.wizard.core.*;
import ariba.ui.wizard.component.WizardPage;

import java.io.File;

import ariba.ui.aribaweb.core.AWComponent;

public class AWXWizardContentSwitch extends AWComponent
{
    public String pathForFrame ()
    {
        WizardFrame frame = ((WizardPage)pageComponent()).frame(); // seems gross, but...
        String absolutePath = frame.getSource();
        Log.demoshell.debug("*** AWXWizardContentSwitch -- frame: %s, path: %s", frame, absolutePath);
        return absolutePath;
    }
}
