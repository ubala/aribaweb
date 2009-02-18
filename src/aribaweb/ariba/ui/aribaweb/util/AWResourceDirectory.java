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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWResourceDirectory.java#9 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.ListUtil;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.StringUtil;
import java.util.List;
import java.io.File;
import java.util.Locale;

public abstract class AWResourceDirectory extends AWBaseObject
{
    private static int LogResourceLookup = 0;
    private static final String LocaleSeparator = "_";
    private static final Locale LocaleOfLastResort = Locale.US;
    private static Locale SystemDefaultLocale = LocaleOfLastResort;
    private static final GrowOnlyHashtable BackoffDirectories = new GrowOnlyHashtable();

    private boolean _containsPackagedResources;
    
    protected static void setSystemDefaultLocale (Locale locale)
    {
        Assert.that(locale != null, "SystemDefaultLocale must not be null.");
        SystemDefaultLocale = locale;
    }

    protected static Locale systemDefaultLocale ()
    {
        return SystemDefaultLocale;
    }

    protected static Locale localeOfLastResort ()
    {
        return LocaleOfLastResort;
    }

    private static String[] computeBackoffDirectories (Locale locale)
    {
        List directoriesVector = ListUtil.list(3);
        if (locale != null) {
            String language = locale.getLanguage();
            String country  = locale.getCountry();
            String variant  = locale.getVariant();
            String directory = null;
            if (!StringUtil.nullOrEmptyOrBlankString(variant)) {
                directory = StringUtil.strcat(language, LocaleSeparator, country, LocaleSeparator, variant);
                directoriesVector.add(directory);
            }
            if (!StringUtil.nullOrEmptyOrBlankString(country)) {
                directory = StringUtil.strcat(language, LocaleSeparator, country);
                directoriesVector.add(directory);
            }
            if (!StringUtil.nullOrEmptyOrBlankString(language)) {
                directoriesVector.add(language);
            }
        }
        String[] backoffDirectories = new String[directoriesVector.size()];
        directoriesVector.toArray(backoffDirectories);
        return backoffDirectories;
    }

    private static String[] backoffDirectories (Locale locale)
    {
        String[] backoffDirectories = (String[])BackoffDirectories.get(locale);
        if (backoffDirectories == null) {
            synchronized (BackoffDirectories) {
                backoffDirectories = (String[])BackoffDirectories.get(locale);
                if (backoffDirectories == null) {
                    backoffDirectories = computeBackoffDirectories(locale);
                    BackoffDirectories.put(locale, backoffDirectories);
                }
            }
        }
        return backoffDirectories;
    }

    /**
        return a cannonical directory path that always ends with "/"
    */
    public abstract String directoryPath ();
      
    /**
        return the url prefix for all the resources in this directory
    */
    public abstract String urlPrefix ();

    public String formatCacheableUrlForResource (AWResource resource)
    {
        return ((AWConcreteApplication)AWConcreteApplication.sharedInstance()).formatUrlForResource(urlPrefix(), resource, true);
    }

    public String formatUrlForResource (AWResource resource)
    {
        return ((AWConcreteApplication)AWConcreteApplication.sharedInstance()).formatUrlForResource(urlPrefix(), resource, false);
    }

    /**
        return list of files with certain file name extension
    */
    public String[] filesWithExtension (String relativePath, String fileExtension)
    {
        return null;
    }
    
    /**
        return a resource in the given name
    */
    protected abstract AWResource locateResourceWithRelativePath (String resourceName, String relativePath);

    private AWResource locateResourceNamed (String resourceName, String[] localeDirectories)
    {
        if (localeDirectories != null) {
            int localeDirectoriesLength = localeDirectories.length;
            for (int index = 0; index < localeDirectoriesLength; index++) {
                String localeString = localeDirectories[index];
                String relativePath = StringUtil.strcat(localeString, File.separator, resourceName);
                AWResource resource = locateResourceWithRelativePath(resourceName, relativePath);
                if (resource != null) {
                    return resource;
                }
            }
        }
        return null;
    }

    protected AWResource locateResourceNamed (String resourceName, Locale locale)
    {
        /* This code embodies the search rule (assume a user-preferredLocale of fr_CA_UNIX, SystemDefaultLocale of ja_JP_UNIX and LocaleOfLastResort en_US):
            // Users preferred Locale
            fr_CA_UNIX
            fr_CA
            fr
            // SystemDefaultLocale
            ja_JP_UNIX
            ja_JP
            ja
            // LocaleOfLastResort
            en_US
            en
            // No locale at all: Note: this final backstop is not supported by the resourceService in Buyer
            <nothing>
        */
        AWResource resource = null;
        String[] backoffDirectories = null;
        if (locale != null) {
            backoffDirectories = AWResourceDirectory.backoffDirectories(locale);
            resource = locateResourceNamed(resourceName, backoffDirectories);
        }
        if (resource == null) {
            Locale systemDefaultLocale = AWResourceDirectory.SystemDefaultLocale;
            if (!systemDefaultLocale.equals(locale)) {
                backoffDirectories = AWResourceDirectory.backoffDirectories(systemDefaultLocale);
                resource = locateResourceNamed(resourceName, backoffDirectories);
            }
            if (resource == null) {
                Locale localeOfLastResort = AWResourceDirectory.LocaleOfLastResort;
                if (!localeOfLastResort.equals(systemDefaultLocale) && !localeOfLastResort.equals(locale)) {
                    backoffDirectories = AWResourceDirectory.backoffDirectories(localeOfLastResort);
                    resource = locateResourceNamed(resourceName, backoffDirectories);
                }
                if (resource == null) {
                    resource = locateResourceWithRelativePath(resourceName, resourceName);
                }
            }
        }
        return resource;
    }

    public AWResource createResource(String resourceName, String relativePath)
    {
        return null;
    }

    public String toString ()
    {
        return StringUtil.strcat(getClass().getName(),  ": path:", directoryPath(), ": url:", urlPrefix());
    }
    
    protected String removeTrailingSlashes (String s)
    {
        if (s == null) {
            return s;
        }
        int index = s.length() - 1;
        while (index > 0 &&
               (s.charAt(index) == '/' ||
               s.charAt(index) == '\\')) {
            index --;
        }
        index ++;
        return s.substring(0, index);
    }

    private static int ResourceLookupCount = 0;
    protected void logResourceLookup (String path, boolean found)
    {
        if (LogResourceLookup > 0) {
            String resourceLookupCount = Integer.toString(ResourceLookupCount++);
            String logMsg = Fmt.S("%s: Resource %s at %s / %s.", resourceLookupCount,
                                  (found ? "FOUND" : "not found"),
                                    System.getProperty("user.dir"),
                                    path);
            logString(logMsg);
        }
    }

    public static void enableResourceLookupLogging ()
    {
        LogResourceLookup++;
    }

    public static void disableResourceLookupLogging ()
    {
        LogResourceLookup--;
    }

    public boolean containsPackagedResources()
    {
        return _containsPackagedResources;
    }

    public void setContainsPackagedResources(boolean containsPackagedResources)
    {
        _containsPackagedResources = containsPackagedResources;
    }
}
