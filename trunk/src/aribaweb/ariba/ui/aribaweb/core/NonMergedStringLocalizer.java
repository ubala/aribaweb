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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/NonMergedStringLocalizer.java#8 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWLocal;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.util.core.ClassUtil;
import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import java.util.Map;
import java.util.List;
import java.io.InputStream;

public class NonMergedStringLocalizer implements AWStringLocalizer
{
    public Map getLocalizedAWLStrings (AWComponent component)
    {
        if (component instanceof AWIncludeBlock) {
            component = component.parent();
        }
        String resourceName = localizedAWLStringsResourceName(
            component.resourceClassName());
        AWResource resource = component.resourceManager().resourceNamed(resourceName);
        return loadLocalizedStrings(component.name(), resource);
    }

    public Map getLocalizedJavaStrings (AWComponent component)
    {
        if (component instanceof AWIncludeBlock) {
            component = component.parent();
        }
        Class resourceClass = component.getClass();
        String resourceName = localizedJavaStringsResourceName(resourceClass.getName());
        AWResource resource = component.resourceManager().resourceNamed(resourceName);
        return loadLocalizedStrings(component.name(), resource);
    }

    protected String localizedAWLStringsResourceName (String resourceClassName)
    {
        return localizedResourceName(resourceClassName, AWConcreteApplication.DefaultAWLStringsSuffix);
    }

    protected String localizedJavaStringsResourceName (String resourceClassName)
    {
        return localizedResourceName(resourceClassName, AWConcreteApplication.DefaultJavaStringsSuffix);
    }

    public Map getLocalizedStrings (String stringTable,
                                    String componentName,
                                    AWResourceManager resourceManager)
    {
        String resourceName = stringTable;
        if (stringTable.startsWith("ariba.ui.")) {
            resourceName = StringUtil.strcat("strings/", stringTable, AWConcreteApplication.DefaultPackageStringsSuffix);
        }
        AWResource resource = resourceManager.resourceNamed(resourceName);
        return loadLocalizedStrings(componentName, resource);
    }

    private String localizedResourceName (String resourceClassName, String suffix)
    {
        String resourceName = null;
        if (resourceClassName.startsWith("ariba.ui.")) {
            String packageName = ClassUtil.stripClassFromClassName(resourceClassName);
            resourceName = StringUtil.strcat("strings/", packageName, AWConcreteApplication.DefaultPackageStringsSuffix);
        }
        else {
            resourceName = AWComponentDefinition.computeTemplateName(resourceClassName, suffix);
        }
        return resourceName;
    }

    private Map loadLocalizedStrings (String componentName, AWResource resource)
    {
        Map localizedStrings = null;
        if (resource != null) {
            // The conversion returns the strings table
            // inside another hashtable with the component name.
            if (componentName.indexOf(".") != -1) {
                componentName = ClassUtil.stripPackageFromClassName(componentName);
            }
            localizedStrings = (Map)resource.object();
            if ((localizedStrings == null) ||
                    (AWConcreteApplication.IsRapidTurnaroundEnabled && resource.hasChanged())) {
                InputStream inputStream = resource.inputStream();
                List lines = AWUtil.parseCsvStream(inputStream);
                AWUtil.close(inputStream);
                if (lines != null) {
                    // if debugging is enabled, the resource path is supplied to the
                    // string table creation so that it can be part of the embedded
                    // contextualization information
                    if (AWLocal.IsDebuggingEnabled) {
                        String resourceFilePath = ((AWFileResource)resource)._fullPath();
                        localizedStrings = AWUtil.convertToLocalizedStringsTable(lines, resourceFilePath);
                    } else {
                        localizedStrings = AWUtil.convertToLocalizedStringsTable(lines);
                    }

                    Map saveLocalizedStrings = localizedStrings;
                    if (localizedStrings == null) {
                        String errorMessage =
                                Fmt.S("Cannot locate localizedStrings for component %s in hashtable\n %s" +
                                componentName, saveLocalizedStrings.toString());
                        throw new AWGenericException(errorMessage);
                    }
                    AWUtil.internKeysAndValues(localizedStrings);
                }
                else {
                    localizedStrings = MapUtil.map();
                }
                resource.setObject(localizedStrings);
            }
            // check if it is merged string files
            Object componentLocalizedStringsHashtable = localizedStrings.get(componentName);
            if (componentLocalizedStringsHashtable instanceof Map) {
                localizedStrings = (Map)componentLocalizedStringsHashtable;
            }
        }
        return localizedStrings;
    }
}
