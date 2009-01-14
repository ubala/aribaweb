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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWMergedStringLocalizer.java#5 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.util.core.ClassUtil;
import ariba.util.i18n.MergedStringLocalizer;
import ariba.util.core.FileUtil;
import java.util.Locale;
import java.util.Map;
import java.io.File;


/**
 * String localizer that fetches strings from the Resource Service.
 * @aribaapi private
 */
public class AWMergedStringLocalizer extends MergedStringLocalizer implements AWStringLocalizer
{
    /*
        Implementation note:
        Please be aware that in the lowest level getLocalizedString method we are
        restricting the locale.  This is part of the "restricted" locale feature
        which is described in ObjectServerSession.  The result is that the strings
        returned may not be in the locale that was asked for.

        Because we depend on the session to tell us how to modify the locale (if at
        all) this introduces a dependency on the executing thread.  It also means
        that you should NOT be caching the results of method calls to this class
        on the passed-in locale, because different threads (from different sessions
        in different realms) may expect different results for the same arguments
        to getLocalizedstring.
    */

    public Map getLocalizedAWLStrings (AWComponent component)
    {
        Map stringMap =  getLocalizedString(component.resourceClassName(),
            component.componentDefinition(),
            component.resourceManager().locale());
        if (stringMap == null) {
            // This can occur when you have a dervied class from a component
            // that uses the base class awl file and the localized table
            // is defined at the base class level.  In this case the looked
            // for the localized table will fail, so we need to do the lookup
            // at the base class level.
            //
            // Solution:
            // If the resource class name is not found, then try walking down
            // the class hierarchy "starting at component instance" until we find
            // a localized table to use.
            //
            // The reason we are starting at the class and not super class,
            // is because component.resourceClassName() could have returned a value
            // that does not represent this component instance class, the method
            // can be overridden by derived classes and they can return any value,
            // so we need to start at the component level and go down the super
            // classes checking one by one.
            Class c = component.getClass();
            AWComponentDefinition definition = component.componentDefinition();
            Locale locale = component.resourceManager().locale();
            while (stringMap == null && c != Object.class) {
                stringMap = getLocalizedString(c.getName(), definition, locale);
                c = c.getSuperclass();
            }
        }
        return stringMap;
    }

    public Map getLocalizedJavaStrings (AWComponent component)
    {
        return getLocalizedString(component.getClass().getName(),
            component.componentDefinition(),
            component.resourceManager().locale());
    }

    public Map getLocalizedStrings (String stringTable, String componentName,
                                    AWResourceManager resourceManager)
    {
        return getLocalizedString(stringTable,  componentName, resourceManager.locale());
    }

    private Map getLocalizedString (String resourceClassName,
                                    AWComponentDefinition componentDefinition,
                                    Locale locale)
    {
        String stringTableName = null;
        String componentName = null;
        if (componentDefinition.isClassless()) {
            // For classless components, we use the template name
            // to derived the string table name and string table key
            String componentTemplateName = componentDefinition.templateName();
            componentTemplateName = FileUtil.fixFileSeparators(componentTemplateName);
            int lastFileSepIndex = componentTemplateName.lastIndexOf(File.separatorChar);
            if (lastFileSepIndex != -1) {
                stringTableName = componentTemplateName.substring(0, lastFileSepIndex);
                stringTableName = stringTableName.replace(File.separatorChar, '.');
            }
            componentName = componentDefinition.componentName();
        }
        else {
            stringTableName =
                ClassUtil.stripClassFromClassName(resourceClassName);
            componentName =
                ClassUtil.stripPackageFromClassName(resourceClassName);
        }
        return getLocalizedString(stringTableName,  componentName,  locale);
    }
}
