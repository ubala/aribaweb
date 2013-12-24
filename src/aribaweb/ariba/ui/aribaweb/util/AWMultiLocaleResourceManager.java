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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMultiLocaleResourceManager.java#57 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.GrowOnlyHashSet;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.StringUtil;
import ariba.util.core.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class AWDummy extends AWBaseObject
{
}

abstract public class AWMultiLocaleResourceManager extends AWResourceManager
{
    public static boolean AllowScanningAllPackages = false;
    protected static String WebserverUrlPrefix = "http://localhost";
    protected static String WebserverUrlPrefixSecure = "https://localhost";
    protected static ResourceVersionManager ResourceVersionManager;
    private static int LogFailedResourceLookups = 0;
    private static final Class AWDummyClass = AWDummy.class;
    private static final Class DeletedMarker = DeletedDummy.class;
    private static final AWResource ResourceNotFoundMarker = new AWFileResource();
    private static final String ComponentTemplateFileExtension = ".awl";
    private static AWResourceManagerFactory _ResourceManagerFactory;
    private GrowOnlyHashtable _classesByNameHashtable = new GrowOnlyHashtable();
    private AWMultiKeyHashtable _resourcesHashtable = new AWMultiKeyHashtable(2);
    private List _resourceDirectories = ListUtil.list();
    private AWMultiLocaleResourceManager _nextResourceManager;
    private GrowOnlyHashtable _singleLocaleResourceManagers = new GrowOnlyHashtable();
    private final AWStringsThunk _stringsClassExtension = new AWStringsThunk(this);

    private List _registeredPackageNamesVector = ListUtil.list();
    private List _registeredPackageDirectoriesVector = ListUtil.list();
    private AWMultiKeyHashtable _packageResources = new AWMultiKeyHashtable(2);
    private GrowOnlyHashtable _directoryPackages = new GrowOnlyHashtable();
    private GrowOnlyHashtable _classDirectories = new GrowOnlyHashtable();
    private GrowOnlyHashtable _classPackages = new GrowOnlyHashtable();

    private GrowOnlyHashtable _packageFlags = new GrowOnlyHashtable();

    //'add consumers to register a callback for packaged resource directory
    private GrowOnlyHashSet _packagedResourceDirectoryCallbacks = new GrowOnlyHashSet();

    public static void setWebserverHostName (String webserverHostName)
    {
        if (webserverHostName.startsWith("http")) {
            // There is no distinction between secure or non secure url.
            // The entire system runs in one mode that is specified
            // in the AppInfo, either http or https.
            WebserverUrlPrefix = webserverHostName;
            WebserverUrlPrefixSecure = webserverHostName;
        }
        else {
            //Original code was doing this only
            WebserverUrlPrefix = StringUtil.strcat("http://", webserverHostName);
            WebserverUrlPrefixSecure = StringUtil.strcat("https://", webserverHostName);
        }
    }

    public static String webserverUrlPrefix (boolean isSecure)
    {
        return isSecure ? WebserverUrlPrefixSecure : WebserverUrlPrefix;
    }

    public static void setResourceVersionManger (ResourceVersionManager manager)
    {
        ResourceVersionManager = manager;
    }

    public static String resourceVersion (String resourceName)
    {
        if (ResourceVersionManager != null) {
            return ResourceVersionManager.version(resourceName);
        }
        return null;
    }

    public static void setResourceManagerFactory (AWResourceManagerFactory resourceManagerFactory)
    {
        _ResourceManagerFactory = resourceManagerFactory;
    }

    public AWMultiLocaleResourceManager ()
    {
        registerPackagedResourceExtension(".awl");
        registerPackagedResourceExtension(".htm");
    }

    public void setSystemDefaultLocale (Locale locale)
    {
        AWResourceDirectory.setSystemDefaultLocale(locale);
    }

    public AWResourceManager _resourceManagerForLocale (Locale locale)
    {
        AWResourceManager resourceManager =
            (AWResourceManager)_singleLocaleResourceManagers.get(locale);
        if (resourceManager == null) {
            synchronized (_singleLocaleResourceManagers) {
                resourceManager =
                    (AWResourceManager)_singleLocaleResourceManagers.get(locale);
                if (resourceManager == null) {
                    resourceManager = _ResourceManagerFactory.createResourceManager(this, locale);
                    _singleLocaleResourceManagers.put(locale, resourceManager);
                }
            }
        }
        return resourceManager;
    }

    public AWResourceManager resourceManagerForLocale (Locale locale)
    {
        // we will return the resource manager appropriate for for this thread
        // this is determined by the ResourceService (and ultimately by RealmProfile)
        locale = ResourceService.getService().getRestrictedLocale(locale);
        return _resourceManagerForLocale(locale);
    }

    public AWResourceManager[] resourceManagers ()
    {
        return (AWResourceManager[])_singleLocaleResourceManagers.elementsArray();
    }

    public List resourceDirectories ()
    {
        return _resourceDirectories;
    }

    public String[] resourceDirectoryPaths ()
    {
        int resourceDirectoryCount = _resourceDirectories.size();
        String[] resourceDirectoryPaths = new String[resourceDirectoryCount];
        for (int index = 0; index < resourceDirectoryCount; index++) {
            AWResourceDirectory currentResourceDirectory =
                (AWResourceDirectory)_resourceDirectories.get(index);
            resourceDirectoryPaths[index] = currentResourceDirectory.directoryPath();
        }
        return resourceDirectoryPaths;
    }

    public void setNextResourceManager (AWMultiLocaleResourceManager resourceManager)
    {
        _nextResourceManager = resourceManager;
    }

    public AWMultiLocaleResourceManager nextResourceManager ()
    {
        return _nextResourceManager;
    }

    public void flush ()
    {
        if (_nextResourceManager != null) {
            _nextResourceManager.flush();
        }
        if (_singleLocaleResourceManagers != null) {
            List keys = _singleLocaleResourceManagers.keysList();
            for (int index = keys.size() - 1; index > -1; index--) {
                Object currentKey = keys.get(index);
                AWSingleLocaleResourceManager currentResourceManager =
                    (AWSingleLocaleResourceManager)_singleLocaleResourceManagers.get(currentKey);
                currentResourceManager.clearCache();
            }
        }
        _resourcesHashtable = new AWMultiKeyHashtable(2);
    }

    protected void addResourceWithNameFromDirectoryWithExtension (String resourceName, AWResourceDirectory resourceDirectory, String fileExtension)
    {
        Locale locale = AWResourceDirectory.systemDefaultLocale();
        AWResource resource = resourceDirectory.locateResourceNamed(resourceName, locale);
        if (_resourcesHashtable.get(resourceName, locale) == null) {
            // if a collision occurs, let the first one remain
            _resourcesHashtable.put(resourceName, locale, resource);
        }
        if (fileExtension.equals(ComponentTemplateFileExtension)) {
            String className = resourceName.substring(0, resourceName.indexOf('.'));
            String directoryPath = resourceDirectory.directoryPath();
            String fileSpeparator = AWUtil.contains(directoryPath, "/") ? "/" : "\\";
            String directoryName = AWUtil.lastComponent(directoryPath, fileSpeparator);
            _classDirectories.put(className, directoryName);
        }
    }

    private void addResourcesFromDirectoryWithExtension (
        AWResourceDirectory resourceDirectory,
        String fileExtension)
    {
        String[] fileList = resourceDirectory.filesWithExtension(null, fileExtension);
        if (fileList != null) {
            for (int index = (fileList.length - 1); index >= 0; index--) {
                String currentResourceName = fileList[index];
                Log.aribawebResource_register.debug(
                    "Adding resource %s from directory %s", currentResourceName,
                    resourceDirectory.directoryPath());
                addResourceWithNameFromDirectoryWithExtension(
                    currentResourceName, resourceDirectory, fileExtension);
            }
        }
    }

    public void registerResourceDirectory (AWResourceDirectory resourceDirectory)
    {
        Log.aribawebResource_register.debug(
            "AWMultiLocaleResourceManager: registerResourceDirectory():" +
            " registering directory %s",
                resourceDirectory.directoryPath());

        synchronized (_resourceDirectories) {
            if (!directoryAlreadyRegistered(resourceDirectory.directoryPath())) {
                _resourceDirectories.add(resourceDirectory);
                addResourcesFromDirectoryWithExtension(resourceDirectory, ComponentTemplateFileExtension);
                addResourcesFromDirectoryWithExtension(resourceDirectory, "gif");
                if (resourceDirectory.containsPackagedResources()) {
                    prepopulateClassPackages(resourceDirectory);
                }
            }
        }
    }

    // allow for callbacks by consumers of a resource directory
    public void registerResourceDirectoryCallback(AWResourceDirectoryHandler handler)
    {
        _packagedResourceDirectoryCallbacks.add(handler);
    }


    private boolean directoryAlreadyRegistered (String directoryPathString)
    {
        int size = _resourceDirectories.size();
        for (int i = 0; i < size; i++) {
            AWResourceDirectory resourceDirectory =
                (AWResourceDirectory)_resourceDirectories.get(i);
            if (resourceDirectory.directoryPath().equals(directoryPathString)) {
                return true;
            }
        }
        return false;
    }

    public void registerResourceDirectory (String directoryPathString, String urlPrefixString)
    {
        registerResourceDirectory(directoryPathString,  urlPrefixString, true);
    }

    public void registerResourceDirectory (String directoryPathString,
                                           String urlPrefixString,
                                           boolean containsPackagedResources)
    {
        Log.aribawebResource_register.debug("---- RegisterResourceDirectory: %s", directoryPathString);
        File resourceDir = new File(directoryPathString);
        if (!resourceDir.isAbsolute() && !resourceDir.exists()) {
            boolean didReg = AWClasspathResourceDirectory.checkRegisterResourceDirectory(directoryPathString,
                urlPrefixString, containsPackagedResources, this);
            if (!didReg) Log.aribawebResource_register.debug("---- RegisterResourceDirectory: %s -- WARNING: not found in file system nor classpath", directoryPathString);
        } else {
            AWResourceDirectory resourceDirectory = new AWFileResourceDirectory(directoryPathString, urlPrefixString);
            resourceDirectory.setContainsPackagedResources(containsPackagedResources);
            registerResourceDirectory(resourceDirectory);
        }
    }

    /**
     * See AWHttpResourceDirectory for more information on deprecation.
     * @deprecated
     * @param url
     * @throws java.net.MalformedURLException
     */
    public void registerHttpResourceDirectory (String url)
      throws java.net.MalformedURLException
    {
        AWResourceDirectory resourceDirectory =
            new AWHttpResourceDirectory(url);
        registerResourceDirectory(resourceDirectory);
    }

    protected AWResource locateResourceNamed (String resourceName, Locale locale)
    {
        AWResource resource = null;
        int resourceDirectoryCount = _resourceDirectories.size();
        for (int index = 0; index < resourceDirectoryCount; index++) {
            AWResourceDirectory currentDirectory =
                (AWResourceDirectory)_resourceDirectories.get(index);
            Log.aribawebResource_lookup.debug("Looking for %s in %s cache %s",
                resourceName, currentDirectory.directoryPath(), cacheEnabled()?True:False);
            resource = currentDirectory.locateResourceNamed(resourceName, locale);
            if (resource != null) {
                break;
            }
        }
        return resource;
    }

    private AWResource chainedLocateResourceNamed (String resourceName, Locale locale, boolean isBrandable)
    {
        AWResource resource = locateResourceNamed(resourceName, locale);
        if ((resource == null) && (_nextResourceManager != null)) {
            // this is the recursion.
            resource = _nextResourceManager.resourceNamed(resourceName, locale, isBrandable);
        }
        return resource;
    }

    private AWResource _resourceNamed (String resourceName, Locale locale, boolean isBrandable)
    {
        AWResource resource = null;
        if (cacheEnabled()) {
            resource = _cachedResourceNamed(resourceName, locale, isBrandable);
        }
        else {
            resource = chainedLocateResourceNamed(resourceName, locale, isBrandable);
            if (resource == null) {
                AWResourceManager base = getBaseResourceManager();
                if (base != null) {
                    resource = base.resourceNamed(resourceName);
                }
            }
            if (resource == null) {
                if (LogFailedResourceLookups > 0) {
                    AWResourceDirectory.enableResourceLookupLogging();
                    chainedLocateResourceNamed(resourceName, locale, isBrandable);
                    AWResourceDirectory.disableResourceLookupLogging();
                }
            }
        }
        return resource;
    }

    private AWResource _cachedResourceNamed (String resourceName, Locale locale, boolean isBrandable)
    {
        AWResource resource = (AWResource)_resourcesHashtable.get(resourceName, locale);
        if (resource == null) {
            resource = chainedLocateResourceNamed(resourceName, locale, isBrandable);
            AWResourceManager base = getBaseResourceManager();
            if (resource == null && base != null) {
                resource = base.resourceNamed(resourceName);
            }
            if (resource == null) {
                if (LogFailedResourceLookups > 0) {
                    AWResourceDirectory.enableResourceLookupLogging();
                    chainedLocateResourceNamed(resourceName, locale, isBrandable);
                    AWResourceDirectory.disableResourceLookupLogging();
                }
                _resourcesHashtable.put(resourceName, locale, ResourceNotFoundMarker);
            }
            else {
                _resourcesHashtable.put(resourceName, locale, resource);
            }
        }
        else if (resource == ResourceNotFoundMarker) {
            resource = null;
        }
        return resource;
    }

    public AWResource resourceNamed (String resourceName, Locale locale)
    {
        return resourceNamed(resourceName, locale, false);
    }

    AWResource resourceNamed (String resourceName, Locale locale, boolean isBrandable)
    {
        // Note:  If we created a separate resource mamanger for each
        // locale, then I could simplify things a bit and not use the _scratchResource
        // (which holds two things, resourceName and locale).
        AWResource resourceNamed = null;
        if (locale == null) {
            locale = AWResourceDirectory.systemDefaultLocale();
        }

        AWResourceManager base = getBaseResourceManager();
        if (!isBrandable && base != null) {
            // if the resource is not brandable and this is a branded resource
            // manager, then defer to the base resource manager
            return base.resourceNamed(resourceName);
        }

        if (AWUtil.AllowsConcurrentRequestHandling) {
            // Note: we appear to be synchronizing on every resource lookup, but...
            // works b/c we assume that single locale resource manager is the real caching mechanism
            synchronized (_resourcesHashtable) {
                resourceNamed = _resourceNamed(resourceName, locale, isBrandable);
            }
        }
        else {
            resourceNamed = _resourceNamed(resourceName, locale, isBrandable);
        }
        return resourceNamed;
    }

    public AWResource resourceNamed (String resourceName)
    {
        return resourceNamed(resourceName, false);
    }

    public AWResource resourceNamed (String resourceName, boolean isBrandable)
    {
        return resourceNamed(resourceName, null, isBrandable);
    }

    private List locateResourcesNamed (String resourceName, Locale locale, boolean isBrandable)
    {
        AWResourceManager base = getBaseResourceManager();
        if (!isBrandable && base != null && base instanceof AWMultiLocaleResourceManager) {
            // if the resource is not brandable and this is a branded resource
            // manager, then defer to the base resource manager
            return ((AWMultiLocaleResourceManager)base).locateResourcesNamed(resourceName, locale, isBrandable);
        }

        List resourcesVector = null;
        if (_nextResourceManager == null) {
            resourcesVector = ListUtil.list();
        }
        else {
            resourcesVector = _nextResourceManager.locateResourcesNamed(
                resourceName, locale, isBrandable);
        }
        AWResource resource = locateResourceNamed(resourceName, locale);
        if (resource != null) {
            resourcesVector.add(resource);
        }
        return resourcesVector;
    }

    public AWResource[] resourcesNamed (String resourceName, Locale locale)
    {
        return resourcesNamed(resourceName, locale, false);
    }

    public AWResource[] resourcesNamed (String resourceName, Locale locale, boolean isBrandable)
    {
        List resourcesVector = locateResourcesNamed(resourceName, locale, isBrandable);
        AWResource[] resourcesArray = new AWResource[resourcesVector.size()];
        resourcesVector.toArray(resourcesArray);
        return resourcesArray;
    }

    public AWResource[] resourcesNamed (String resourceName)
    {
        return resourcesNamed(resourceName, null);
    }

    public List allResources ()
    {
        return _resourcesHashtable.elementsVector();
    }

    //////////////////////////
    // Base Api
    // in all cases,
    //////////////////////////
    /**
    @deprecated use resource().inputStream() to get contents of file
    */
    public String pathForResourceNamed (String resourceName, Locale locale)
    {
        AWResource resource = resourceNamed(resourceName, locale);
        return (resource == null) ? null : ((AWFileResource)resource)._fullPath();
    }

    /**
    @deprecated use resource().inputStream() to get contents of file
    */
    public String pathForResourceNamed (String resourceName)
    {
        AWResource resource = resourceNamed(resourceName, null);
        return (resource == null) ? null : ((AWFileResource)resource)._fullPath();
    }

    public String urlForResourceNamed (String resourceName, Locale locale)
    {
        AWResource resource = resourceNamed(resourceName, locale);
        return (resource == null) ? null : resource.url();
    }

    public String urlForResourceNamed (String resourceName)
    {
        return urlForResourceNamed(resourceName, null);
    }

    public String urlForResourceNamed (String resourceName, boolean isFullUrl, boolean isSecure)
    {
        throw new AWGenericException("urlForResourceNamed(String resourceName, boolean isFullUrl, boolean isSecure, is not yet supported.");
    }

    public String urlForResourceNamed (String resourceName, boolean isFullUrl, boolean isSecure, boolean isVersioned)
    {
        throw new AWGenericException("urlForResourceNamed(String resourceName, boolean isFullUrl, boolean isSecure, boolean isVersioned) is not yet supported.");
    }

    public AWImageInfo imageInfoForName (String imageFilename, Locale locale)
    {
        AWImageInfo imageInfo = null;
        AWResource resource = resourceNamed(imageFilename, locale, true);
        if (resource != null) {
            imageInfo = (AWImageInfo)resource.object();
            if (imageInfo == null) {
                imageInfo = new AWImageInfo(resource);
                resource.setObject(imageInfo);
            }
        }
        return imageInfo;
    }

    public AWImageInfo imageInfoForName (String imageFilename)
    {
        return imageInfoForName(imageFilename, null);
    }

        // remove a class from the cache, so that it must be reloaded.
    public void removeClass (String shortName)
    {
        synchronized (_classesByNameHashtable) {
            // restore the entry in _allClassesMap which are removed as used.
            if (_allClassesMap != null) {
                Class targetClass = (Class)_classesByNameHashtable.get(shortName);
                if (!doesNotExist(targetClass)) {
                    String fullClassName = targetClass.getName();
                    if (_allClassesMap.get(shortName) == null) {
                        _allClassesMap.put(shortName, fullClassName);
                    }
                }
            }
            // then flush the class from the cache.
            if (_classesByNameHashtable.get(shortName) != null) {
                _classesByNameHashtable.put(shortName, DeletedMarker);
            }
        }
    }

    /**
     * Returns a full url
     *
     * This method is to add webserver url prefix to the url if it is a relative url.
     *
     * This method is mainly used to construct a full url to pass it to scripts
     * when the app runs in the test automation mode.
     * If test scripts playback in the iehta mode, //C: is added to the url (relative path)
     * as root directory, not the webserver prefix, i.e. http://localhost. Because of that,
     * it fails to execute secure scripts in the client.
     *
     * @param urlString
     * @param requestContext
     * @return
     */
    public static String fullUrl (String urlString,
                                  AWRequestContext requestContext)
    {
        if (!StringUtil.nullOrEmptyString(urlString) &&
                !urlString.startsWith("http:") && !urlString.startsWith("https:")) {
            if (!urlString.startsWith("/")) {
                urlString = StringUtil.strcat("/", urlString);
            }
            boolean isSecure = requestContext.request() != null &&
                    requestContext.request().isSecureScheme();
            String webserverUrlPrefix = webserverUrlPrefix(isSecure);
            if (webserverUrlPrefix.endsWith("/")) {
                webserverUrlPrefix =
                        webserverUrlPrefix.substring(0, webserverUrlPrefix.length() - 1);
            }
            return StringUtil.strcat(webserverUrlPrefix, urlString);
        }
        return null;
    }

    ////////////////
    // Class Lookup
    ////////////////
    public Class classForName (String className)
    {
        Class classForName = null;
        classForName = (Class)_classesByNameHashtable.get(className);
        if (doesNotExist(classForName)) {
            synchronized (_classesByNameHashtable) {
                classForName = (Class)_classesByNameHashtable.get(className);
                if (doesNotExist(classForName)) {
                    classForName = lookupClassForName(className);
                    if (doesNotExist(classForName)) {
                        if (_nextResourceManager != null) {
                            classForName = _nextResourceManager.classForName(className);
                        }
                        if (doesNotExist(classForName)) {
                            classForName = AWDummyClass;
                        }
                    }
                    className = className.intern();
                    _classesByNameHashtable.put(className, classForName);
                }
            }
        }
        if (classForName == AWDummyClass) {
            classForName = null;
        }
        return classForName;
    }

    private boolean doesNotExist (Class classForName)
    {
        return classForName == null || classForName == DeletedMarker;
    }

    public Locale locale ()
    {
        return null;
    }

    public AWCharacterEncoding characterEncoding ()
    {
        return AWCharacterEncoding.Default;
    }

    /////////////////
    // Localized String - only implemented for AWSingleLocaleResourceManager
    /////////////////
    public String localizedString (String filename, String extension, String componentName, String keyName, String defaultString)
    {
        throw new RuntimeException(
            "AWResourceManager.localizedString() not implemented for this class.");
    }
    public String localizedString (String filename, String componentName, String keyName, String defaultString)
    {
        return localizedString(filename, null, componentName, keyName, defaultString);
    }
    public String localizedString (String fileName, String keyName, String defaultString)
    {
        return localizedString(fileName, fileName, keyName, defaultString);
    }

    public String localizedString (String fileName, String keyName)
    {
        return localizedString(fileName, keyName, null);
    }

    //////////////////
    // Strings tables
    //////////////////
    public String localizedStringForKey (String stringKey, Locale locale)
    {
        return null;
    }

    public String localizedStringForKey (
        String stringKey,
        Locale locale,
        String stringTableName)
    {
        return null;
    }

    public AWStringsThunk strings ()
    {
        return _stringsClassExtension;
    }

    /////////////////
    // Debugging
    /////////////////
    public String toString ()
    {
        return StringUtil.strcat(getClass().getName(),":",super.toString(),": ",
                            _resourceDirectories.toString(), " resources: ",
                            _resourcesHashtable.toString());
    }

        //////////////////////////////
        /// runtime dump for debugging
        ///////////////////////////////
    public void dumpState (PrintStream out)
    {
        out.println("*******State of AWMultiLocaleResourceManager");
        out.println("List of resource directory");
        for (int i = 0; i < _resourceDirectories.size(); i++) {
            out.println(_resourceDirectories.get(i).toString());
        }
        out.println("List of resources cached");
        /*
        for (Iterator e = _resourcesHashtable.iterator();
             e.hasNext();) {
            out.println(e.next().toString());
        }
    */

        out.println("next resource manager in the chain=");
        if (_nextResourceManager != null) {
            _nextResourceManager.dumpState(out);
        }
    }

    public static void enableFailedResourceLookupLogging ()
    {
        LogFailedResourceLookups++;
    }

    public static void disableFailedResourceLookupLogging ()
    {
        LogFailedResourceLookups--;
    }

    ///////////////////////
    // Package Support
    ///////////////////////

    public List registeredPackageNames ()
    {
        return ListUtil.cloneList(_registeredPackageNamesVector);
    }

    public int packageFlags (String packageName)
    {
        int flags = 0;
        Object oflag =_packageFlags.get(packageName);
        if (oflag != null) { // is this possible?
            flags = Integer.parseInt((String)oflag);
        }
        return flags;
    }

    public void setPackageFlags (String packageName, int flags)
    {
        _packageFlags.put(packageName, String.valueOf(flags));
    }

    public void registerPackageName (String packageName)
    {
        registerPackageName(packageName,0);
    }

    public void registerPackageName (String packageName, boolean enforceFullValidation)
    {
        registerPackageName(packageName, (enforceFullValidation ? (AWConcreteApplication.TemplateValidationFlag | AWConcreteApplication.StrictTagNamingFlag) : 0));
    }

    public synchronized void registerPackageName (String packageName, int flags)
    {
        Log.aribaweb.debug("Resource Manager: registering package %s", packageName);
        synchronized (_registeredPackageNamesVector) {
            if (!_registeredPackageNamesVector.contains(packageName)) {
                _packageFlags.put(packageName, String.valueOf(flags));
                _registeredPackageNamesVector.add(0, packageName);
                if (!packageName.equals(".")) {
                    String directoryName = AWUtil.lastComponent(packageName, ".");
                    String[] packageList = (String[])_directoryPackages.get(directoryName);
                    if (packageList == null) {
                        packageList = new String[0];
                    }
                    packageList = (String[])AWUtil.addElement(packageList, packageName);
                    _directoryPackages.put(directoryName, packageList);
                    String packageDirectory = AWUtil.replaceAllOccurrences(packageName, ".", "/");
                    _registeredPackageDirectoriesVector.add(0, packageDirectory);
                    prepopulateClassPackages(packageName, packageDirectory);
                }
                else {
                    _registeredPackageDirectoriesVector.add(0, "");
                    prepopulateClassPackages(packageName, "");
                }
            }
        }
    }

    private void movePackageToEnd (int index)
    {
        _registeredPackageNamesVector.add(_registeredPackageNamesVector.remove(index));
        _registeredPackageDirectoriesVector.add(_registeredPackageDirectoriesVector.remove(index));
    }

    private Class classByScanningAllPackageNames (String className)
    {
        Class namedClass = null;
        synchronized (_registeredPackageNamesVector) {
            int lastIndex = _registeredPackageNamesVector.size() - 1;
            for (int index = lastIndex; index >= 0; index--) {
                String currentPackageName = (String)_registeredPackageNamesVector.get(index);
                String currentClassName = null;
                if (currentPackageName.length() == 0 || currentPackageName.equals(".")) {
                    currentClassName = className;
                }
                else {
                    currentClassName = StringUtil.strcat(currentPackageName, ".", className);
                }
                namedClass = AWUtil.classForName(currentClassName);
                if (namedClass != null) {
                    if (index != lastIndex) {
                        // by putting this on the end, we get an MRU effect.
                        movePackageToEnd(index);
                    }
                    break;
                }
            }
        }
        return namedClass;
    }

    protected Class lookupClassForName (String className)
    {
        Class classForName = null;
        boolean hasDots = (className.indexOf('.') != -1);
        boolean isUppercase = (Character.isUpperCase(className.charAt(0)));
        // todo: should check here for isValidJavaIdentifier
        if (hasDots) {
            classForName = AWUtil.classForName(className);
        } else if (isUppercase) {
            if (_allClassesMap == null) {
                _allClassesMap = computeClassMap();
            }
            synchronized (_allClassesMap) {
                String fullClassName = (String)_allClassesMap.get(className);
                if (fullClassName == null) {
                    if (AllowsTrialAndErrorClassNames) {
                        classForName = _classForTemplateName(className);
                        if (classForName != null || _classPackages.get(className) != null) {
                            return classForName;
                        }
                        String directoryName = (String)_classDirectories.get(className);
                        if (directoryName != null) {
                            String[] packageList = (String[])_directoryPackages.get(directoryName);
                            if (packageList != null) {
                                for (int index = (packageList.length - 1); index >= 0; index--) {
                                    String packageName = packageList[index];
                                    fullClassName = StringUtil.strcat(packageName, ".", className);
                                    classForName = AWUtil.classForName(fullClassName);
                                    if (classForName != null) {
                                        AWUtil.moveToEnd(packageList, index);
                                        break;
                                    }
                                }
                            }
                        }
                        if (classForName == null) {
                            classForName = AWUtil.classForName(className);
                            if (classForName == null) {
                                classForName = classByScanningAllPackageNames(className);
                            }
                        }
                    }
                }
                else {
                    classForName = AWUtil.classForName(fullClassName);
                    Assert.that(classForName != null, "fullClassName doesn't map to an actual class.");
                    _allClassesMap.remove(className);
                    // compact the hashtable after removing 50 items
                    if (_allClassesMap.size() % 50 == 0) {
                        _allClassesMap = MapUtil.map(_allClassesMap);
                    }
                }
            }
        }
        return classForName;
    }

    protected AWResource resourceByScanningAllPackages (String resourceName, Locale locale)
    {
        synchronized (_registeredPackageDirectoriesVector) {
            AWResource resource = null;
            int lastIndex = _registeredPackageDirectoriesVector.size() - 1;
            for (int index = lastIndex; index > -1; index--) {
                String currentPackageDirectory = (String)_registeredPackageDirectoriesVector.get(index);
                String currentResourceName = StringUtil.strcat(currentPackageDirectory, "/", resourceName);
                resource = resourceNamed(currentResourceName, locale);
                if (resource != null) {
                    if (index != lastIndex) {
                        // by putting this on the end, we get an MRU effect.
                        movePackageToEnd(index);
                    }
                    break;
                }
            }
            return resource;
        }
    }

    public AWResource packageResourceNamed (String resourceName, Locale locale)
    {
        synchronized (_packageResources) {
            if (locale == null) {
                locale = AWResourceDirectory.systemDefaultLocale();
            }
            AWResource resource = (AWResource)_packageResources.get(resourceName, locale);
            if (resource == ResourceNotFoundMarker) {
                resource = null;
            }
            else if (AllowScanningAllPackages && resource == null) {
                resource = resourceByScanningAllPackages(resourceName, locale);
                if (resource != null) {
                    logWarning("*-*-*-*-*-*-*-*-* ERROR: resourceByScanningAllPackages succeeded: " + resourceName);
                    Thread.dumpStack();
                }
                _packageResources.put(resourceName, locale, resource == null ?
                                      ResourceNotFoundMarker :
                                      resource);
            }
            return resource;
        }
    }

    public AWResource packageResourceNamed (String resourceName)
    {
        return packageResourceNamed(resourceName, null);
    }

    /////////////////
    // New Stuff
    /////////////////
    private void prepopulateClassPackages (AWResourceDirectory fileResourceDirectory)
    {
        if (fileResourceDirectory.containsPackagedResources()) {
            String directoryPathString = fileResourceDirectory.directoryPath();
            for (int index = 0, count = _registeredPackageDirectoriesVector.size(); index < count; index++) {
                // These two parallel vectors should be conjoined somehow (eg hashtable).
                String packageName = (String)_registeredPackageNamesVector.get(index);
                String packageDirPath = (String)_registeredPackageDirectoriesVector.get(index);
                addClassPackages(fileResourceDirectory, packageDirPath, packageName);
            }
        }
    }

    private void prepopulateClassPackages (String packageName, String packageDirPath)
    {
        for (int index = 0, count = _resourceDirectories.size(); index < count; index++) {
            Object resourceDirectory = _resourceDirectories.get(index);
            if (resourceDirectory instanceof AWFileResourceDirectory) {
                AWFileResourceDirectory fileResourceDirectory = (AWFileResourceDirectory)resourceDirectory;
                if (fileResourceDirectory.containsPackagedResources()) {
                    addClassPackages(fileResourceDirectory, packageDirPath, packageName);
                }
            }
        }
    }

    private void addClassPackages (AWResourceDirectory fileResourceDirectory, String packageDirPath, String packageName)
    {
        if (!(fileResourceDirectory instanceof AWFileResourceDirectory)) return;
        
        String directoryPathString = fileResourceDirectory.directoryPath();
        String packageResourcePath = StringUtil.strcat(directoryPathString, "/", packageDirPath);

        for (int i=0, c=_packagedResourceExtensions.size(); i < c; i++) {
            String[] fileList = filesWithExtension(packageResourcePath, (String)_packagedResourceExtensions.get(i));
            addClassPackages(fileList, packageName);
            addResources(fileResourceDirectory, packageDirPath, fileList);
        }

        for(Object o :  _packagedResourceDirectoryCallbacks) {
            AWResourceDirectoryHandler handler = (AWResourceDirectoryHandler)o;
            handler.registeredResourceDirectory(fileResourceDirectory, packageDirPath, packageName);
        }
    }

    protected String [] filesWithExtension (String path, String extension)
    {
        String[] ret = AWUtil.filesWithExtension(path, extension);
        return ret;
    }

    private void addClassPackages (String[] fileList, String packageName)
    {
        for (int index = 0, fileCount = AWUtil.length(fileList); index < fileCount; index++) {
            String fileName = fileList[index];
            String baseFileName = AWUtil.stripToBaseFilename(fileName);
            if (_classPackages.get(baseFileName) == null) {
                _classPackages.put(baseFileName, packageName);
            }
        }
    }

    private void addResources (AWResourceDirectory resourceDirectory, String packageDirPath, String[] fileList)
    {
        for (int index = 0, fileCount = AWUtil.length(fileList); index < fileCount; index++) {
            String fileName = fileList[index];
            boolean isPackaged = (packageDirPath.length() > 0);
            String relativePath = isPackaged
                    ? StringUtil.strcat(packageDirPath, "/", fileName)
                    : fileName;
            addResource(fileName, relativePath, isPackaged, resourceDirectory);
        }
    }

    protected void addResource(String name, String relativePath, boolean isPackaged, AWResourceDirectory resourceDirectory)
    {
        synchronized (_packageResources) {
            /*
                This code is subtle (and arguably not quite right).

                If a resource with the same name is registered under the same package,
                then "first one wins" -- e.g. first resource *directory* registered wins.

                However, the *last* *package* wins (except "." always loses).
             */
            Locale locale = AWResourceDirectory.systemDefaultLocale();
            AWResource resource = (AWResource)_resourcesHashtable.get(relativePath, locale);
            if (resource == null) {
                resource = createResource(relativePath, relativePath, resourceDirectory);
                _resourcesHashtable.put(relativePath, locale, resource);
            }
            if (isPackaged || _packageResources.get(name, locale) == null) {
                _packageResources.put(name, locale, resource);
            }
        }
    }

    protected AWResource createResource (String resourceName,
                                          String relativePath,
                                          AWResourceDirectory directory)
    {
        return directory.createResource(resourceName, relativePath);
    }

    public Class _classForTemplateName (String templateName)
    {
        Class classForName = null;
        String precomputedPackageName = (String)_classPackages.get(templateName);
        if (precomputedPackageName != null) {
            String fullClassName = precomputedPackageName.equals(".") ?
                templateName :
                StringUtil.strcat(precomputedPackageName, ".", templateName);
            classForName = AWUtil.classForName(fullClassName);
        }
        return classForName;
    }

    /**
        This should be considered private -- only used by AWConcreteApp.
    */
    public Class classForTemplateName (String templateName)
    {
        synchronized (_classesByNameHashtable) {
            Class classForName = (Class)_classesByNameHashtable.get(templateName);
            if (classForName == AWDummyClass) {
                classForName = null;
            }
            else if (doesNotExist(classForName)) {
                classForName = _classForTemplateName(templateName);
                if (classForName != null) {
                    _classesByNameHashtable.put(templateName, classForName);
                }
            }
            return classForName;
        }
    }

    /**
     * method used to pre-populate class cache to avoid trying to load up non-existing class
     * to speed up performance.
     * Once this method is called, AW will assume that the class does not exist and will
     * not try to load it up anymore.
     * @param shortName
     */
    public void registerNonExistingClass (String shortName)
    {
        _classesByNameHashtable.put(shortName, AWDummyClass);

    }

    public void registerClass (String shortName, Class classObject)
    {
        // NOTE: put(...) is syncronized on 'this' (ie on _classesByNameHashtable)
        _classesByNameHashtable.put(shortName, classObject);
    }

    public void registerClass (Class classObject)
    {
        String shortName = ClassUtil.stripPackageFromClassName(classObject.getName());
        registerClass(shortName, classObject);
    }

    final class AWMultiKeyHashtable extends MultiKeyHashtable
    {
        private List _allValues = ListUtil.list();

        public AWMultiKeyHashtable (int keyCount)
        {
            super(keyCount);
        }

        /*
         * MultiKeyHashtable uses a shared Object key array _sharedKeyList to
         * store the keys in and do a lookup. This is not safe and in a multi-threaded 
         * environment; a collision could occur when two operations(get/put) run in 
         * parallel. The following wrapper methods ensure the calls are synchronized.
         * 
         * This will not have a performance impact because AWSingleLocaleManager is
         * the primary cache.
         */
        public synchronized Object get (Object key0, Object key1)
        {
            return super.get(key0, key1);
        }

        public synchronized Object put (Object key0, Object key1, Object value)
        {
            return super.put(key0, key1, value);
        }

        protected Object put (Object[] targetKeyList, Object value,
                              boolean onlyPutIfAbsent)
        {
            _allValues.add(value);
            return super.put(targetKeyList, value, onlyPutIfAbsent);
        }

        protected List elementsVector ()
        {
            return _allValues;
        }
    }

    /**
     * This holds the .
     */
    private Map _allClassesMap;
    /**
     * This flag allows for the new mode which precomputes the mapping between short class names
     * and fulllClassNames by looking within the zips/jars in the classpath.  If false, the old
     * mode which uses trial and error in determing the fullClassNames from the registered packages
     * will be employed.
     */
    private static boolean AllowsTrialAndErrorClassNames = true;

    public static void setAllowsTrialAndErrorClassNames (boolean flag)
    {
        Assert.that(flag, "Non-trial and error mode no longer supported -- call UI Framework team...");
        AllowsTrialAndErrorClassNames = flag;
    }

    private Map computeClassMap ()
    {
        return MapUtil.map();
/*
        if (AllowsTrialAndErrorClassNames) {
            return MapUtil.map();
        }
 /*
        Map zipFiles = MapUtil.map(100);
        Map classMap = MapUtil.map(500);
        ClassPath classPath = newClassPath();
        String[] paths = classPath.getJavaClassPath();
        int suffixLength = 4;
        for (int index = 0, pathsLength = paths.length; index < pathsLength; index++) {
            String path = paths[index];
            int indexOfSuffix = path.length() - suffixLength;
            if (indexOfSuffix > suffixLength &&
                (".jar".regionMatches(true, 0, path, indexOfSuffix, suffixLength) ||
                ".zip".regionMatches(true, 0, path, indexOfSuffix, suffixLength))) {
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(path);
                }
                catch (IOException ioexception) {
                    logWarning("** Warning: CLASSPATH contains unreadable path: " + path);
                    continue;
                }
                addClassNames(zipFiles, classMap, zipFile);
            }
        }
        logString("*** classMap: " + classMap.size() + " " + classMap);
        return classMap;
        */
    }
/*
    private void addClassNames (Map zipFiles, Map classMap, ZipFile zipFile)
    {
        String zipFileName = zipFile.getName();
        if (zipFiles.get(zipFileName) != null) {
            return;
        }
        else {
            zipFiles.put(zipFileName, zipFileName);
        }
        Enumeration enumeration = zipFile.entries();
        String currentPackage = "shouldnotmatch.";
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry)enumeration.nextElement();
            String zipEntryName = zipEntry.getName();
            if (zipEntryName.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                InputStream inputStream = getInputStream(zipFile, zipEntry);
                String manifestContents = AWUtil.stringWithContentsOfInputStream(inputStream);
                String classPathToken = "Class-Path: ";
                int indexOfClassPath = manifestContents.indexOf(classPathToken);
                if (indexOfClassPath > 0) {
                    int indexOfEnd = manifestContents.indexOf("\r\n\r\n");
                    if (indexOfEnd == -1) {
                        indexOfEnd = manifestContents.length() - 1;
                    }
                    String fullClassPath = manifestContents.substring(indexOfClassPath + classPathToken.length(), indexOfEnd);
                    StringArray components = AWUtil.componentsSeparatedByString(fullClassPath, "\n ");
                    fullClassPath = AWUtil.componentsJoinedByString(components, "");
                    String[] classPathEntries = StringUtil.delimitedStringToArray(fullClassPath, ' ');
                    //System.out.println("***: " + zipFileName);
                    String zipFileDirectory = zipFileName.substring(0, zipFileName.lastIndexOf(File.separatorChar) + 1);
                    for (int index = 0, length = classPathEntries.length; index < length; index++) {
                        String classPathEntry = classPathEntries[index];
                        if (classPathEntry.endsWith(".jar") || classPathEntry.endsWith(".zip")) {
                            //System.out.println("classPathEntry: " + classPathEntry);
                            ZipFile currentZipFile = newZipFile(zipFileDirectory + classPathEntry);
                            if (currentZipFile != null) {
                                addClassNames(zipFiles, classMap, currentZipFile);
                            }
                        }
                    }
                    //System.out.println("***");
                }
            }
            else if (zipEntryName.startsWith("ariba/")) {
                int indexOfLastDot = zipEntryName.lastIndexOf('.');
                String fullClassName = zipEntryName.substring(0, indexOfLastDot);
                fullClassName = fullClassName.replace('/', '.');
                String className = ClassUtil.stripPackageFromClassName(fullClassName);
                if (classMap.get(className) == null) {
                    if (fullClassName.startsWith(currentPackage)) {
                        classMap.put(className, fullClassName);
                    }
                    else if (isPackageRegistered(fullClassName)) {
                        currentPackage = ClassUtil.stripClassFromClassName(fullClassName);
                        classMap.put(className, fullClassName);
                    }
                }
                else if (!classMap.get(className).equals(fullClassName)) {
                    logWarning("** Warning: possible conflict with class names.  Will use: " +
                        classMap.get(className) + " in preference to: " + fullClassName);
                }
            }
        }
    }
*/
    private boolean isPackageRegistered (String fullClassName)
    {
        for (int index = 0; index < _registeredPackageNamesVector.size(); index++) {
            String packageName = (String)_registeredPackageNamesVector.get(index);
            if (fullClassName.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }
/*
    private ClassPath newClassPath ()
    {
        try {
            return new ClassPath();
        }
        catch (IOException ioexception) {
            throw new AWGenericException("** Warning: cannot parse CLASSPATH.");
        }
    }
*/
    private ZipFile newZipFile (String path)
    {
        try {
            return new ZipFile(path);
        }
        catch (IOException ioexception) {
            logWarning("** Warning: cannot create zipfile.");
            return null;
        }
    }

    private InputStream getInputStream (ZipFile zipFile, ZipEntry zipEntry)
    {
        try {
            return zipFile.getInputStream(zipEntry);
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }

    public String classesByNameHashtable ()
    {
        return _classesByNameHashtable.toString();
    }

    public AWMultiLocaleResourceManager createBrandedResourceManager (String brandName, String version)
    {
        throw new AWGenericException("AWMultiLocaleResourceManager createBrandedResourceManager not implemented.");
    }

    public static interface ResourceVersionManager
    {
        public String version (String resourceName);    
    }

    public interface AWResourceDirectoryHandler {

        void registeredResourceDirectory(AWResourceDirectory resourceDirectory, String packageDirPath, String packageName);

    }
}

class DeletedDummy {
}
