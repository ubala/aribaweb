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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXInputTag.java#4 $
*/

package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWBinding;

public class AWXInputTag extends AWComponent
{
    public AWResponseGenerating action ()
    {
        Object val = valueForBinding("name");
        if (!(val instanceof AWResponseGenerating)) {
            val = null;
        }
        return (AWResponseGenerating)val;
    }

    /**
     * these are here to avoid the AW choking if the template binds an INPUT tag with a
     * 'name' that is not a dynamic binding.
     */
    public Object value ()
    {
        return valueForBinding("value");
    }

    public void setValue (Object value)
    {
        AWBinding binding = bindingForName("value");
        if ((binding != null) && !binding.isConstantValue()) {
            setValueForBinding(value, binding);
        }
    }
}
