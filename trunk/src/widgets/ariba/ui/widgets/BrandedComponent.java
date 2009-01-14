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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BrandedComponent.java#14 $
*/

package ariba.ui.widgets;

import ariba.util.core.StringUtil;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;

abstract public class BrandedComponent extends AWComponent
{
    protected abstract String shortTemplateName ();

    public AWResource templateResource ()
    {
        AWResource resource = safeTemplateResource();
        if (resource == null) {
            throw new AWGenericException("Branded template: " + templateName() + " not found");
        }
        return resource;
    }

    protected AWResource safeTemplateResource ()
    {
        AWResourceManager resourceManager = resourceManager();
        AWResource resource = resourceManager.resourceNamed(shortTemplateName());

        if (resource == null) {
            String templateName = templateName();
            int awlIndex = templateName.lastIndexOf(ComponentTemplateFileExtension);
            if (awlIndex != -1) {
                templateName = templateName.substring(0, awlIndex);
                templateName = StringUtil.strcat(templateName, ".htm");
            }
            resource = resourceManager.resourceNamed(templateName);
        }
        return resource;
    }

    public boolean hasMultipleTemplates ()
    {
        return true;
    }
}
