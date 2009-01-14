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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BrandingComponent.java#12 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWElement;

abstract public class BrandingComponent extends AWComponent
{
    public Object findValueForBinding (String bindingName)
    {
        AWComponent parent;
        for (parent = parent(); parent != null; parent = parent.parent()) {
            AWBinding binding = parent.bindingForName(bindingName);
            if (binding != null) {
                return parent.valueForBinding(binding);
            }
        }
        return null;
    }

    public String resolveTemplateOrComponentBasedInclude (String bindingName)
    {
        String bindingValue = (String)findValueForBinding(bindingName);
        if (bindingValue == null &&
            BrandingComponent.componentWithTemplateNamed(bindingName, this) != null)
        {
            return bindingName;
        }
        return bindingValue;
    }

    public static AWComponent componentWithTemplateNamed (String templateName,
                                                          AWComponent component)
    {
        AWComponent context = component;
        for (; context != null; context = context.parent()) {
            AWElement contentElement = context.componentReference().contentElement();
            if (contentElement instanceof AWTemplate) {
                AWTemplate template = (AWTemplate)contentElement;
                if (template.indexOfNamedSubtemplate(templateName, component) != -1) {
                    return context;
                }
            }
        }
        return null;
    }
}
