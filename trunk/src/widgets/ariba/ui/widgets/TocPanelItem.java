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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TocPanelItem.java#4 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;



// This is subclass'ed at ariba.collaborate.widgets.TocHighLightPanelItem.
// Mandy might need to know about this 

public class TocPanelItem extends AWComponent
{
    public boolean _isVisible;

    /////////////
    // Awake
    /////////////
    protected void awake ()
    {
        AWBinding binding = bindingForName(AWBindingNames.isVisible);
        _isVisible = binding == null ? true : binding.booleanValue(parent());
    }

    protected void sleep ()
    {
        _isVisible = false;
    }

    public boolean isInExpandedView ()
    {
        return SimpleTocPanel.currentInstance(this).isInExpandedView();
    }
}
