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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWPrivateScript.java#2 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWGenericException;

public class AWPrivateScript extends AWComponent
{
    private static final String[] SupportedBindingNames = {/*empty*/};

    public String[] supportedBindingNames()
    {
        return SupportedBindingNames;
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        throw new AWGenericException("<script> tag is invalid, please use <AWClientSideScript>.");
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        throw new AWGenericException("<script> tag is invalid, please use <AWClientSideScript>.");
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        throw new AWGenericException("<script> tag is invalid, please use <AWClientSideScript>.");
    }
}