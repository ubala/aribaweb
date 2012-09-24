/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWSingleLocaleResourceManager.java#23 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.MapUtil;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import java.util.Map;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.ResourceService;

import java.util.List;
import ariba.util.i18n.LocaleSupport;
import ariba.util.i18n.I18NUtil;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWConcreteApplication;

import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AWSingleLocaleResourceManager extends AWResourceManager
{
    private static final GrowOnlyHashtable CharacterEncodingsByLocale = new GrowOnlyHashtable();
    private static GrowOnlyHashtable ExtendedFilenames = new GrowOnlyHashtable();
    private static int NextIndex = 0;
    private final AWMultiLocaleResourceManager _multiLocaleResourceManager;
    private final Locale _locale;
    private final AWCharacterEncoding _characterEncoding;
    private final AWStringsThunk _stringsClassExtension = new AWStringsThunk(this);
    private GrowOnlyHashtable _resources;
    private GrowOnlyHashtable _urlsForResource;
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
        _urlsForResource = new GrowOnlyHashtable();
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

    private String urlForResource (AWResource resource, boolean isFullUrl, boolean isSecure, boolean isVersioned)
    {
        String resourceUrl = resource.url();
        if (!isFullUrl && !isVersioned) {
            return resourceUrl;
        }
        String urlForResource = (String)_urlsForResource.get(resource);
        if (urlForResource == null) {
            synchronized (_urlsForResource) {
                urlForResource = (String) _urlsForResource.get(resource);
                if (urlForResource == null) {
                    urlForResource = resourceUrl;
                    String webserverUrlPrefix = "";
                    if (isFullUrl && !resourceUrl.startsWith("http:") &&
                        !resourceUrl.startsWith("https:")) {
                        if (!resourceUrl.startsWith("/")) {
                            resourceUrl = StringUtil.strcat("/", resourceUrl);
                        }
                        webserverUrlPrefix = AWMultiLocaleResourceManager.webserverUrlPrefix(isSecure);
                        if (webserverUrlPrefix.endsWith("/")) {
                            webserverUrlPrefix = webserverUrlPrefix.substring(0, webserverUrlPrefix.length() - 1);
                        }
                        urlForResource = StringUtil.strcat(webserverUrlPrefix, resourceUrl);
                    }
                    if (isVersioned) {
                        String version = AWMultiLocaleResourceManager.resourceVersion(resource.name());
                        if (!StringUtil.nullOrEmptyOrBlankString(version))
                        {
                            int index = webserverUrlPrefix.length() + 1;
                            // urlprefix can be of the form 
                            // <AWApplication.resourceURL>/<pathToResource> in this case,
                            // insert version after the first slash
                            int firstSlash = urlForResource.indexOf('/', index + 1);
                            if (firstSlash > -1) {
                                index = firstSlash;
                            }

                            String resourcePrefix = urlForResource.substring(0, index);
                            String resourceSuffix = urlForResource.substring(index);
                            urlForResource = Fmt.S("%s/%s%s",
                                    resourcePrefix, version, resourceSuffix); 
                        }
                    }
                    if (urlForResource != resourceUrl) {
                        _urlsForResource.put(resource, urlForResource);
                    }
                }
            }
        }
        return urlForResource;
    }

    public String urlForResourceNamed (String resourceName, boolean isFullUrl, boolean isSecure)
    {
        return urlForResourceNamed(resourceName, isFullUrl, isSecure, false);
    }

    public String urlForResourceNamed (String resourceName, boolean isFullUrl,
                                       boolean isSecure, boolean isVersioned)
    {
        String url = null;
        AWResource resource = resourceNamed(resourceName);
        if (resource != null) {
            url = urlForResource(resource, isFullUrl, isSecure, isVersioned);
        }
        return url;
    }

    public String urlForResourceNamed (String resourceName)
    {
        return urlForResourceNamed(resourceName, false, false, false);
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

    static Locale _PseudoLocaleFiles = I18NUtil.getLocaleFromString("eu");  // basque
    static Locale _PseudoLocaleAll = I18NUtil.getLocaleFromString("ee");    // ewe

    static class PseudoLocalizer implements ResourceService.PseudoLocalizer
    {
        public Map process (Locale locale, Map strings)
        {
            return (locale == _PseudoLocaleFiles || locale == _PseudoLocaleAll) ? pseudoLocalize(strings, true) : strings;
        }

        public String process (Locale locale, String string)
        {
            return (locale == _PseudoLocaleFiles || locale == _PseudoLocaleAll) ? pseudoLocalize(string) : string;
        }

        Map pseudoLocalize (Map<String, Object> strings, boolean recurse)
        {
            Map localized = MapUtil.map();
            for (Map.Entry<String, Object> e : strings.entrySet()) {
                Object val = e.getValue();
                if (val instanceof String) {
                    val = pseudoLocalize((String) val);
                }
                else if (val instanceof Map) {
                    val = recurse ? pseudoLocalize((Map)val, false) : null;
                }
                localized.put(e.getKey(), val);
            }
            return localized;
        }

        Pattern _TagPattern = Pattern.compile("([^<]*)(?:(<[^>]+?>)(.*?))?");

        String pseudoLocalize (String s)
        {
            Matcher m = _TagPattern.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String pre = m.group(1), tag = m.group(2), suf = m.group(3);
                if (pre != null) sb.append(pseudoLocalizePart(pre));
                if (tag != null) sb.append(tag);
                if (suf != null) sb.append(pseudoLocalizePart(suf));
            }
            return sb.toString();
        }

        String pseudoLocalizePart (String s)
        {
            s = StringUtil.replaceCharByChar(s, 'a', '\u00e0');
            s = StringUtil.replaceCharByChar(s, 'e', '\u00e8');
            s = StringUtil.replaceCharByChar(s, 'i', '\u00ed');
            s = StringUtil.replaceCharByChar(s, 'o', '\u00f4');
            s = StringUtil.replaceCharByChar(s, 'u', '\u00fc');
            s = StringUtil.replaceCharByChar(s, 'c', '\u00e7');
            s = StringUtil.replaceCharByChar(s, 'n', '\u00f1');
            return s;
        }
    }

    public enum PseudoMode { Off, Files, All }

    public PseudoMode _pseudoLocalizationMode ()
    {
        Locale locale = locale();
        return (locale == _PseudoLocaleFiles) ? PseudoMode.Files
                : ((locale == _PseudoLocaleAll) ? PseudoMode.All :PseudoMode.Off);
    }

    public boolean pseudoLocalizingAll ()
    {
        return AWConcreteApplication.IsDebuggingEnabled && _pseudoLocalizationMode() == PseudoMode.All;
    }

    public String pseudoLocalizeUnKeyed (String string)
    {
        return (_pseudoLocalizationMode() == PseudoMode.All)
                ? ResourceService._getPseudoLocalizer().process(locale(), string) 
                : string;
    }

    static public void _setPseudoLocalizationMode (AWSession session, PseudoMode mode)
    {
        if (mode != PseudoMode.Off) {
            PseudoLocalizer pl = (PseudoLocalizer)ResourceService._getPseudoLocalizer();
            if (pl == null) {
                pl = new PseudoLocalizer();
                ResourceService._setPseudoLocalizer(pl);
            }
            session.dict().put(PseudoLocalizer.class, session.preferredLocale());            
            session._forceLocale(mode == PseudoMode.Files ? _PseudoLocaleFiles : _PseudoLocaleAll);
        } else {
            Locale l = (Locale)session.dict().get(PseudoLocalizer.class);
            session._forceLocale(l != null ? l : Locale.US);
        }
        
    }

    static public PseudoMode _pseudoLocalizationMode (AWSession session)
    {
        return ((AWSingleLocaleResourceManager)session.resourceManager())._pseudoLocalizationMode();
    }
}
