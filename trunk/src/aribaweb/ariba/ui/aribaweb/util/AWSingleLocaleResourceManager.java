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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWSingleLocaleResourceManager.java#15 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.MapUtil;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import java.util.Map;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.core.MultiKeyHashtable;
import java.util.List;
import ariba.util.i18n.LocaleSupport;
import java.io.InputStream;
import java.util.Locale;

public class AWSingleLocaleResourceManager extends AWResourceManager
{
    private static final Map CharacterEncodingsByLocale = MapUtil.map();
    private static GrowOnlyHashtable ExtendedFilenames = new GrowOnlyHashtable();
    private static int NextIndex = 0;
    private final AWMultiLocaleResourceManager _multiLocaleResourceManager;
    private final Locale _locale;
    private final AWCharacterEncoding _characterEncoding;
    private final AWStringsThunk _stringsClassExtension = new AWStringsThunk(this);
    private GrowOnlyHashtable _resources;
    private GrowOnlyHashtable _fullUrls;
    private final int _index;

    public static AWSingleLocaleResourceManager ensureSingleLocale (AWResourceManager resourceManager)
    {
        if (resourceManager instanceof AWMultiLocaleResourceManager) {
            AWMultiLocaleResourceManager multiLocaleResourceManager =
                (AWMultiLocaleResourceManager)resourceManager;
            Locale systemDefaultLocale = AWResourceDirectory.systemDefaultLocale();
            resourceManager =
                multiLocaleResourceManager.resourceManagerForLocale(systemDefaultLocale);
        }
        return (AWSingleLocaleResourceManager)resourceManager;
    }

    public void setSystemDefaultLocale (Locale locale)
    {
        AWResourceDirectory.setSystemDefaultLocale(locale);
    }

    private static AWCharacterEncoding lookupCharacterEncoding (Locale locale)
    {
        AWCharacterEncoding characterEncoding = null;
        if (locale != null) {
            characterEncoding = (AWCharacterEncoding)CharacterEncodingsByLocale.get(locale);
            if (characterEncoding == null) {
                characterEncoding = AWCharacterEncoding.characterEncodingNamed(
                                        LocaleSupport.uiEncoding(locale));
            }
            CharacterEncodingsByLocale.put(locale, characterEncoding);
        }
        if (characterEncoding == null) {
            characterEncoding = AWCharacterEncoding.Default;
        }
        return characterEncoding;
    }

    public AWSingleLocaleResourceManager (AWMultiLocaleResourceManager multiLocaleResourceManager, Locale locale)
    {
        super();
        _multiLocaleResourceManager = multiLocaleResourceManager;
        _locale = locale;
        _characterEncoding = lookupCharacterEncoding(_locale);
        _resources = new GrowOnlyHashtable();
        _fullUrls = new GrowOnlyHashtable();
        synchronized (this.getClass()) {
            _index = NextIndex;
            NextIndex++;
        }
    }

    public int index ()
    {
        return _index;
    }

    public AWMultiLocaleResourceManager multiLocaleResourceManager ()
    {
        return _multiLocaleResourceManager;
    }

    public Class classForName (String className)
    {
        return _multiLocaleResourceManager.classForName(className);
    }

    public void flush ()
    {
        _multiLocaleResourceManager.flush();
    }

    protected void clearCache ()
    {
        synchronized (_resources) {
            _resources = new GrowOnlyHashtable();
        }
    }

    public AWResource resourceNamed (String resourceName)
    {
        return resourceNamed(resourceName, false);
    }

    public AWResource resourceNamed (String resourceName, boolean isBrandable)
    {
        if (resourceName == null) {
            return null;
        }
        AWResource resource = null;
        AWResourceManager base = getBaseResourceManager();
        if (!isBrandable && base != null) {
            // if the resource is not brandable and this is a branded resource
            // manager, then defer to our base resource manager
            return base.resourceNamed(resourceName);
        }

        if (cacheEnabled()) {
            resource = _cachedResourceNamed(resourceName);
        }
        else {
            resource = _multiLocaleResourceManager.resourceNamed(resourceName, _locale);
            if (resource == null && base != null) {
                resource = base.resourceNamed(resourceName);
            }
        }

        return resource;
    }

    private AWResource _cachedResourceNamed (String resourceName)
    {
        AWResource resource = (AWResource)_resources.get(resourceName);
        if (resource == null) {
            synchronized (_resources) {
                resource = (AWResource)_resources.get(resourceName);
                if (resource == null) {
                    resource = _multiLocaleResourceManager.resourceNamed(resourceName, _locale);
                    if (resource == null) {
                        AWResourceManager base = getBaseResourceManager();
                        if (base != null) {
                            resource = base.resourceNamed(resourceName);
                        }
                    }

                    if (resource != null) {
                        _resources.put(resourceName, resource);
                    }
                }
            }
        }
        return resource;
    }

    public AWResource[] resourcesNamed (String resourceName)
    {
        return _multiLocaleResourceManager.resourcesNamed(resourceName, _locale);
    }

    /**
    @deprecated use resource().inputStream() to get contents of file
    */
    public String pathForResourceNamed (String resourceName)
    {
        AWResource resource = resourceNamed(resourceName);
        return (resource == null) ? null : ((AWFileResource)resource)._fullPath();
    }

    private String fullUrl (AWResource resource, boolean isSecure)
    {
        String fullUrl = (String)_fullUrls.get(resource);
        if (fullUrl == null) {
            synchronized (_fullUrls) {
                fullUrl = (String)_fullUrls.get(resource);
                if (fullUrl == null) {
                    String partialUrl = resource.url();
                    if (partialUrl.startsWith("http:") || partialUrl.startsWith("https:")) {
                            // We are already full! (this directory was registered with a full
                            // url prefix to begin with.
                        fullUrl = partialUrl;
                    }
                    else {
                        if (!partialUrl.startsWith("/")) {
                            partialUrl = StringUtil.strcat("/", partialUrl);
                        }
                        String webserverUrlPrefix = AWMultiLocaleResourceManager.webserverUrlPrefix(isSecure);
                        if (webserverUrlPrefix.endsWith("/")) {
                            webserverUrlPrefix = webserverUrlPrefix.substring(0, webserverUrlPrefix.length() - 1);
                        }
                        fullUrl = StringUtil.strcat(webserverUrlPrefix, partialUrl);
                        _fullUrls.put(resource, fullUrl);
                    }
                }
            }
        }
        return fullUrl;
    }

    public String urlForResourceNamed (String resourceName, boolean isFullUrl, boolean isSecure)
    {
        String url = null;
        AWResource resource = resourceNamed(resourceName);
        if (resource != null) {
            url = isFullUrl ? fullUrl(resource, isSecure) : resource.url();
        }
        return url;
    }

    public String urlForResourceNamed (String resourceName)
    {
        return urlForResourceNamed(resourceName, false, false);
    }

    public AWImageInfo imageInfoForName (String imageFilename)
    {
        AWImageInfo imageInfo = null;
        AWResource resource = resourceNamed(imageFilename, true);
        if (resource != null) {
            imageInfo = (AWImageInfo)resource.object();
            if (imageInfo == null) {
                imageInfo = new AWImageInfo(resource);
                resource.setObject(imageInfo);
            }
        }
        return imageInfo;
    }

    public AWResource packageResourceNamed (String resourceName)
    {
        return _multiLocaleResourceManager.resourceNamed(resourceName, _locale);
    }

    public Locale locale ()
    {
        return _locale;
    }

    public AWCharacterEncoding characterEncoding ()
    {
        return _characterEncoding;
    }

    /////////////////
    // Localized String - only implemented for AWSingleLocaleResourceManager
    /////////////////

    public String localizedString (String filename, String keyName, String defaultString)
    {
        return localizedString(filename, filename, keyName, defaultString);
    }

    public String localizedString (String filename, String componentName, String keyName, String defaultString)
    {
        return localizedString(filename, ".strings", componentName, keyName, defaultString);
    }

    public String localizedString (String filename, String extension, String componentName, String keyName, String defaultString)
    {
        String localizedString = null;
        String extendedFilename = filename;

        if (extension != null) {
            extendedFilename = (String)ExtendedFilenames.get(filename);
            if (extendedFilename == null) {
                extendedFilename = StringUtil.strcat(filename, extension);
                ExtendedFilenames.put(filename, extendedFilename);
            }
        }

        AWResource resource = resourceNamed(extendedFilename);
        if (resource != null) {
            Map localizedStrings = (Map)resource.object();
            if ((localizedStrings == null) ||
                (AWUtil.IsRapidTurnaroundEnabled && resource.hasChanged())) {
                InputStream inputStream = resource.inputStream();
                List lines = AWUtil.parseCsvStream(inputStream);
                AWUtil.close(inputStream);
                if (lines != null) {
                    localizedStrings = AWUtil.convertToLocalizedStringsTable(lines);
                    AWUtil.internKeysAndValues(localizedStrings);
                    resource.setObject(localizedStrings);
                }
            }
            if (localizedStrings != null) {
                // The conversion returns the strings table inside another hashtable with the component name.
                localizedStrings = (Map)localizedStrings.get(componentName);
                if (localizedStrings != null) {
                    localizedString = (String)localizedStrings.get(keyName);
                }
            }
        }
        if (localizedString == null) {
            localizedString = defaultString;
        }
        return localizedString;
    }

    public String localizedString (String filename, String keyName)
    {
        String localizedString = localizedString(filename, keyName, null);
        if (localizedString == null) {
            String errorMessage = Fmt.S("Unable to locate %s/%s for locale %s", filename, keyName, _locale);
            debugString(errorMessage);
            AWSingleLocaleResourceManager lastResortResourceManager =
            (AWSingleLocaleResourceManager)_multiLocaleResourceManager.
                resourceManagerForLocale(AWResourceDirectory.localeOfLastResort());
            localizedString = lastResortResourceManager.localizedString(filename, keyName, null);
            if (localizedString == null) {
                localizedString = errorMessage;
            }
        }
        return localizedString;
    }

    public String toString ()
    {
        return StringUtil.strcat(super.toString(), " ", _locale.toString());
    }

    public AWStringsThunk strings ()
    {
        return _stringsClassExtension;
    }

    public AWResourceManager resourceManagerForLocale (Locale locale)
    {
        return _multiLocaleResourceManager.resourceManagerForLocale(locale);
    }

    private MultiKeyHashtable _brandTable = new MultiKeyHashtable(2);

    public AWResourceManager resolveBrand (String realmName, String version,
                                           boolean shouldCreate)
    {
        // Bad: doublecheck locking
        // Note: using brandName to indicate that this AWRM is the
        // manager for a set of branded ARRM's
        Assert.that(getBrandName() == null,
                    "Invalid AWResourceManager used to resolve brand");
        AWResourceManager rm =
            (AWResourceManager)_brandTable.get(realmName, version);
        if (rm == null && shouldCreate) {
            synchronized(_brandTable) {
                rm = (AWResourceManager)_brandTable.get(realmName, version);
                if (rm == null) {
                    rm = createBrandResourceManager(realmName, version);
                    _brandTable.put(realmName,version, rm);
                }
            }
        }
        return rm;
    }

    private AWSingleLocaleResourceManager createBrandResourceManager (
        String brandName, String version)
    {
        AWMultiLocaleResourceManager mlrm =
            _multiLocaleResourceManager.createBrandedResourceManager(
                brandName, version);
        mlrm.setCacheEnabled(false);

        AWSingleLocaleResourceManager slrm =
            new AWSingleLocaleResourceManager(mlrm, _locale);
        slrm.setBrandName(brandName);
        slrm.setBaseResourceManager(this);

        return slrm;
    }
}
