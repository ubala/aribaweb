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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWMetaRefresh.java#2 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.util.AWEncodedString;

public final class AWMetaRefresh extends AWComponent
{
    public AWEncodedString _elementId;
    public AWBinding _actionBinding;
    public AWBinding _pageNameBinding;

    protected boolean useLocalPool ()
    {
        return true;
    }

    protected void sleep ()
    {
        _elementId = null;
    }

    public AWResponseGenerating invokeAction ()
    {
        return AWGenericActionTag.evaluateActionBindings(this, _pageNameBinding, _actionBinding);
    }

    public boolean hasForm ()
    {
        return requestContext().currentForm() != null;
    }
}