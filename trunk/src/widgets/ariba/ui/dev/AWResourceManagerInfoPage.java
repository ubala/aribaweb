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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWResourceManagerInfoPage.java#8 $
*/


package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWResourceDirectory;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import java.util.List;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.AWResource;

public final class AWResourceManagerInfoPage extends AWComponent
{
    public static final String Name = "AWResourceManagerInfoPage";
    AWResourceDirectory _currentResourceDirectory;
    public String _classLookupResult;
    public String _className;
    public String _resourceLookupResult;
    public String _resourceName;
    public String _awlLookupResult;
    public String _awlName;

    public List resourceDirectories ()
    {
        List v = ListUtil.list();
        AWMultiLocaleResourceManager multiLocaleResourceManager = multiLocaleResourceManager();
        // return multiLocaleResourceManager.resourceDirectories();
        while (multiLocaleResourceManager != null) {
            v.addAll(multiLocaleResourceManager.resourceDirectories());
            multiLocaleResourceManager = multiLocaleResourceManager.nextResourceManager();
        }
        return v;
    }

    public List registeredPackageNames ()
    {
        AWMultiLocaleResourceManager manager = multiLocaleResourceManager();
        return manager.registeredPackageNames();
    }

    public AWMultiLocaleResourceManager multiLocaleResourceManager()
    {
        AWSingleLocaleResourceManager resourceManager = (AWSingleLocaleResourceManager)resourceManager();
        AWMultiLocaleResourceManager multiLocaleResourceManager = resourceManager.multiLocaleResourceManager();
        return multiLocaleResourceManager;
    }

    public void setCurrent (AWResourceDirectory current)
    {
        _currentResourceDirectory = current;
    }

    public AWResourceDirectory current ()
    {
        return _currentResourceDirectory;
    }

    public AWComponent lookUpClass ()
    {
        Class c = multiLocaleResourceManager().classForName(_className);
        if (c == null) {
            _classLookupResult = "No class found";
        }
        else {
            _classLookupResult = "Found: " + c.getName();
        }
        return null;
    }

    public AWComponent lookUpResource ()
    {
        AWResource result = application().resourceManager().resourceNamed(_resourceName);
        if (result == null) {
            _resourceLookupResult = "No resource found";
        }
        else {
            _resourceLookupResult = "Found: " + result.toString();
        }
        return null;
    }

    public AWComponent lookUpAWL ()
    {
        if (!_awlName.endsWith(".awl")) {
            // _awlName = _awlName+".awl";
        }

        AWComponentDefinition result = application().componentDefinitionForName(_awlName);
        if (result == null) {
            _awlLookupResult = "No resource found";
        }
        else {
            String templateName = result.templateName();
            AWResource resource = application().resourceManager().resourceNamed(templateName);
            _awlLookupResult = "Class: " + result.componentClass().getName() + "<br>"
                                  + "Template Name: " + templateName  + "<br>"
                                  + "Resource: " + ((resource != null) ? resource.toString() : "null");
        }
        return null;
    }

    public String classesByNameHashtable ()
    {
        AWMultiLocaleResourceManager multiLocaleResourceManager = multiLocaleResourceManager();
        return multiLocaleResourceManager.classesByNameHashtable();
    }
}
