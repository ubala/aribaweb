/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Spotlight.java#1 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;

public final class Spotlight extends AWComponent
{

    public static final String[] SupportedBindingNames = {
        BindingNames.title,
        BindingNames.value,
        BindingNames.step,
        BindingNames.tagName,
    };

    public AWEncodedString _elementId;

    @Override
    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    @Override
    protected void sleep ()
    {
        _elementId = null;
        super.sleep();
    }

    @Override
    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        super.renderResponse(requestContext, component);
        String step = stringValueForBinding(BindingNames.step); 
        SpotlightFooter.register(requestContext, _elementId, step);
    }
}
