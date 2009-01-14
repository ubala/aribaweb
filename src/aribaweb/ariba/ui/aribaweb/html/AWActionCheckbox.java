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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWActionCheckbox.java#9 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;

/**
    This is really a private class to be used only by AWCheckbox.  This is
    required because it needs to be stateful, while AWCheckbox is stateless and
    should remain stateless.
*/
public final class AWActionCheckbox extends AWComponent
{
    private boolean _isChecked;
    private boolean _isExternal;

    private static final String[] SupportedBindingNames = {
        BindingNames.checked, BindingNames.action, BindingNames.isExternal
    };

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();
        _isChecked = booleanValueForBinding(BindingNames.checked);
        _isExternal = booleanValueForBinding(BindingNames.isExternal);
    }

    public Object isChecked ()
    {
        boolean checked =
            _isExternal ? booleanValueForBinding(BindingNames.checked) :
            _isChecked;
        return checked ? BindingNames.awstandalone : null;
    }

    public AWResponseGenerating checkboxClicked ()
    {
        if (_isExternal) {
            setValueForBinding(!booleanValueForBinding(BindingNames.checked),
                               BindingNames.checked);
        }
        else {
            _isChecked = !_isChecked;
            setValueForBinding(_isChecked, BindingNames.checked);
        }
        return (AWResponseGenerating)super.valueForBinding(BindingNames.action);
    }
}
