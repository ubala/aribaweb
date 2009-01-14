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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ToggleImage.java#3 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;

// subclassed by ...catalog/admin/client/apps/admin/compoonent/ACCTypeToggleImage
public class ToggleImage extends AWComponent
{
    private boolean _state;
    private AWBinding _trueBinding;
    private AWBinding _falseBinding;
    public boolean _isExternalState;
    private boolean _useCheckboxIcon;

    // ** Thread Safety Considerations: see AWComponent.

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();
        _state = booleanValueForBinding(AWBindingNames.initState);
        _isExternalState = booleanValueForBinding(AWBindingNames.isExternal);
        _trueBinding = bindingForName(AWBindingNames.trueImageName);
        _falseBinding = bindingForName(AWBindingNames.falseImageName);
        _useCheckboxIcon = booleanValueForBinding("useCheckboxIcon");
    }

    public String currentImageFilename ()
    {
        return _state ? trueImage() : falseImage();
    }

    public void resetToggleValue ()
    {
        if (_isExternalState) {
            pullToggleValue();
        }
        else {
            pushToggleValue();
        }
    }

    public void pushToggleValue ()
    {
        setValueForBinding(_state, AWBindingNames.state);
    }

    public void pullToggleValue ()
    {
        _state = booleanValueForBinding(AWBindingNames.state);
    }

    public AWResponseGenerating toggleImageClicked ()
    {
        _state = !_state;
        if (_isExternalState) {
            pushToggleValue();
        }
        return null;
    }

    private String trueImage ()
    {
        String trueImageName = null;
        if (_useCheckboxIcon) {
            trueImageName = "widg/checkedbox.gif";
        }
        else if (_trueBinding != null) {
            trueImageName = (String)valueForBinding(_trueBinding);
        }
        if (trueImageName == null) {
            trueImageName = "awxToggleImageTrue.gif";
        }
        return trueImageName;
    }

    private String falseImage ()
    {
        String falseImageName = null;
        if (_useCheckboxIcon) {
            falseImageName = "widg/checkbox.gif";
        }
        else if (_falseBinding != null) {
            falseImageName = (String)valueForBinding(_falseBinding);
        }

        if (falseImageName == null) {
            falseImageName = "awxToggleImageFalse.gif";
        }
        return falseImageName;
    }

        // record & playback
    protected AWBinding _debugPrimaryBinding ()
    {
        return componentReference().bindingForName(AWBindingNames.state, parent());
    }
}
