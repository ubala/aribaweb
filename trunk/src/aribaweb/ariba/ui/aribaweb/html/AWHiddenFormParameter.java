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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWHiddenFormParameter.java#4 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWUtil;

/**
    A component that encapsulates encoding of parameter values that are used in a form.
    
    @aribaapi
*/
public class AWHiddenFormParameter extends AWComponent
{
    public String escapedName ()
    {
        return escapedValueForBinding(AWBindingNames.name);
    }

    public String escapedValue ()
    {
        return escapedValueForBinding(AWBindingNames.value);
    }

    private String escapedValueForBinding (String bindingName)
    {
        String value = stringValueForBinding(bindingName);
        if (value != null) {
            value = AWUtil.escapeHtml(value).string();
        }
        return value;
    }
}
