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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWHoverContainer.java#2 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.html.BindingNames;
import ariba.ui.aribaweb.util.AWEncodedString;

import java.util.Map;

public final class AWHoverContainer extends AWComponent
{

    private static final String[] SupportedBindingNames = {
        BindingNames.tagName
    };

    public AWEncodedString _elementId;

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

    public String behavior ()
    {
        return hasContentForTagName("AWHoverControl") ? "AWHC" : "GH";
    }

}