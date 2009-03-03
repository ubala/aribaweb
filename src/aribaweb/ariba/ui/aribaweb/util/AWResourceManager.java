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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWResourceManager.java#12 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Locale;

/**
    The application-wide manager of resource lookups.  AWResourceManagers handle lookup of
    localized strings, webserver resources, and (packaged) UI templates.

    {@link AWMultiLocaleResourceManager} is the typical concrete master resource manager used
    by {@link ariba.ui.aribaweb.core.AWConcreteApplication}.  It, in turn, uses multiple
    {@link AWSingleLocaleResourceManager}s.

    ResourceManagers support "flattening out" multiple resgistered directories into a single
    coalesced lookup space, and support unpackaged lookups of packaged resources
    (via {@link #packageResourceNamed(String)}) and classes (via {@link #classForName(String)}). 
 */
public abstract class AWResourceManager extends AWBaseObject
{
    abstract public void setSystemDefaultLocale (Locale locale);
    abstract public Class classForName (String className);
    abstract public void flush ();

    abstract public AWResource resourceNamed (String resourceName);
    abstract public AWResource resourceNamed (String resourceName, boolean isBrandable);
    abstract public AWResource[] resourcesNamed (String resourceName);
    /**
    @deprecated use resource().inputStream() to get contents of file
    */
    abstract public String pathForResourceNamed (String resourceName);
    abstract public String urlForResourceNamed (String resourceFileName);
    abstract public String urlForResourceNamed (String resourceFileName, boolean isFullUrl, boolean isSecure);
    abstract public String urlForResourceNamed (String resourceFileName, boolean isFullUrl, boolean isSecure, boolean isVersioned);
    abstract public AWImageInfo imageInfoForName (String imageFilename);
    abstract public AWResource packageResourceNamed (String resourceName);
    abstract public Locale locale ();
    abstract public AWCharacterEncoding characterEncoding ();
    abstract public AWStringsThunk strings ();
    abstract public String localizedString (String filename, String extension, String componentName, String keyName, String defaultString);
    abstract public String localizedString (String filename, String componentName, String keyName, String defaultString);
    abstract public String localizedString (String fileName, String key, String defaultString);
    abstract public String localizedString (String fileName, String keyName);
    abstract public AWResourceManager resourceManagerForLocale (Locale locale);

    private AWResourceManager _baseResourceManager;
    private String _brandName;
    private boolean _cache = true;
    protected List _packagedResourceExtensions = ListUtil.list();


    protected void setBrandName (String brandName)
    {
        _brandName = brandName;
    }
    protected String getBrandName ()
    {
        return _brandName;
    }

    public AWResourceManager resolveBrand (String brandName, String version)
    {
        return resolveBrand(brandName, version, true);
    }

    public AWResourceManager resolveBrand (String brandName, String version,
                                           boolean shouldCreate)
    {
        throw new AWGenericException("AWResourceManager resolveBrand not implemented.");
    }

    public void setBaseResourceManager (AWResourceManager rm)
    {
        _baseResourceManager = rm;
    }
    public AWResourceManager getBaseResourceManager ()
    {
        return _baseResourceManager;
    }

    public void setCacheEnabled (boolean flag)
    {
        _cache = flag;
    }
    public boolean cacheEnabled ()
    {
        return _cache;
    }

    public void registerPackagedResourceExtension (String extension)
    {
        if (!extension.startsWith(".")) extension = "." + extension;
        ListUtil.addElementIfAbsent(_packagedResourceExtensions, extension);
    }
}
