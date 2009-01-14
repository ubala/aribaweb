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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWArea.java#8 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;

public final class AWArea extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.pageName, BindingNames.action, BindingNames.onClick, BindingNames.disabled
    };
    public AWBinding _pageNameBinding;
    public AWBinding _actionBinding;

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public boolean isStateless ()
    {
        return true;
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    public AWResponseGenerating invokeAction ()
    {
        return AWGenericActionTag.evaluateActionBindings(this, _pageNameBinding, _actionBinding);
    }
}
