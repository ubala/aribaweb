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

    $Id: //ariba/platform/ui/opensourceui/examples/BusObjUI/example/ui/app/BasicForm.java#1 $
*/
package example.ui.app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import example.ui.busobj.User;
import example.ui.busobj.Project;
import example.ui.busobj.Deal;

import java.util.List;
import java.util.ArrayList;

public final class BasicForm extends AWComponent
{
    public Project _project = Project.sharedInstance();
    public String _region, _state;
    public List<String>_statesForSelectedRegion;

    // Called when value selected in chooser
    public void setRegion (String region)
    {
        _region = region;

        // generate state
        _statesForSelectedRegion = new ArrayList();
        for (int i=0; i<10; i++) {
            _statesForSelectedRegion.add("State of " + _region + " " + i);
        }
        // reset chooser
        _state = null;
    }

    public AWComponent submit ()
    {
        // in real app, would go somewhere if checkErrorsAndEnableDisplay()==false
        if (_project.getTitle().indexOf('X') != -1) {
            recordValidationError("project.title", "Your Project name is not so good!", null);
        }
        errorManager().checkErrorsAndEnableDisplay();
        return null;
    }
}
