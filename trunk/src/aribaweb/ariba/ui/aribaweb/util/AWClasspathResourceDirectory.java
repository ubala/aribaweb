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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWClasspathResourceDirectory.java#19 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWConcreteApplication;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

public final class AWClasspathResourceDirectory extends AWResourceDirectory
{
    static TreeMap<String, String> _Resources = new TreeMap(new Comparator<String>() {
            public int compare(String p1, String p2)
            {
                return p1.compareTo(p2);
            }
        });

    private String _path;
    private String _urlPrefix;

    protected static String resourcePrefix ()
    {
        String path = ((AWApplication)AWConcreteServerApplication.sharedInstance()).resourceFilePath();
        return path != null ? path : "";
    }

    protected AWClasspathResourceDirectory(String relativePath, String urlPrefix)
    {
        _path = removeTrailingSlashes(relativePath);
        if (StringUtil.nullOrEmptyOrBlankString(_path)) {
            Assert.that(StringUtil.nullOrEmptyOrBlankString(_path), "Registered blank path!");
        }
        if (_path.startsWith("./")) _path = _path.substring(2);
        if (_path.length() > 0) {
            _path = _path.concat("/");
            // Implicitly reroute registrations to "docroot" if necessary
            SortedMap<String, String> sub = resourcesWithPrefix(_path);
            if (sub == null || sub.size() == 0) {
                String docRootPath = resourcePrefix().concat(_path);
                sub = resourcesWithPrefix(docRootPath);
                if (sub != null && sub.size() > 0) {
                    _path = docRootPath;
                    Log.aribaweb.debug("RegisterResourceDirectory:  implicitly prepending docroot on %s", relativePath);
                }
            }
        }

        _urlPrefix = removeTrailingSlashes(urlPrefix);
    }

    public String urlPrefix ()
    {
        return _urlPrefix;
    }

    public String directoryPath ()
    {
        return _path;
    }

    protected URL urlForRelativePath (String relativePath)
    {
        String path = _path.concat(relativePath);
        // URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        String urlString = _Resources.get(path);
        try {
            return urlString != null ? new URL(urlString) : null;
        } catch (MalformedURLException e) {
            throw new AWGenericException(e);
        }

    }
    public AWResource createResource(String resourceName, String relativePath)
    {
        return new AWClasspathResource(resourceName, relativePath, urlForRelativePath(relativePath), this);
    }
    
    protected AWResource locateResourceWithRelativePath (String resourceName, String relativePath)
    {
        AWResource resource = null;
        if (!StringUtil.nullOrEmptyString(relativePath)) {
            if (relativePath.contains("\\")) relativePath = relativePath.replace('\\', '/');
            URL url = urlForRelativePath(relativePath);

            if (url != null) {
                resource = new AWClasspathResource(resourceName, relativePath, url, this);
                logResourceLookup(url.toExternalForm(), resource != null);
            }
            else {
                logResourceLookup(_path.concat(relativePath), false);
            }
        }
        return resource;
    }

    static public void recordResourcePath (String path, String urlString)
    {
        _Resources.put(path, urlString);
    }

    static SortedMap<String, String> resourcesWithPrefix (String prefixPath)
    {
        return _Resources.subMap(prefixPath, prefixPath + "~");
    }

    public static boolean hasResourcesUnderPath (String directoryPath)
    {
        return !MapUtil.nullOrEmptyMap(resourcesWithPrefix(directoryPath));
    }

    private static boolean _checkRegisterResourceDirectory (
                                           String directoryPathString,
                                           String urlPrefixString,
                                           boolean containsPackagedResources,
                                           AWMultiLocaleResourceManager resourceManager)
    {
        if (hasResourcesUnderPath(directoryPathString)) {
            AWClasspathResourceDirectory rd = new AWClasspathResourceDirectory(directoryPathString, urlPrefixString);
            rd.setContainsPackagedResources(containsPackagedResources);
            resourceManager.registerResourceDirectory(rd);
            return true;
        }
        return false;
    }

    protected static boolean checkRegisterResourceDirectory(
                                           String directoryPathString,
                                           String urlPrefixString,
                                           boolean containsPackagedResources,
                                           AWMultiLocaleResourceManager resourceManager)
    {
        if (directoryPathString.startsWith("./")) directoryPathString = directoryPathString.substring(2);
        // we check both the named path AND docroot/<path>
        boolean didReg = _checkRegisterResourceDirectory(directoryPathString,
                urlPrefixString, containsPackagedResources, resourceManager);

        didReg = _checkRegisterResourceDirectory(resourcePrefix().concat(directoryPathString),
                urlPrefixString, containsPackagedResources, resourceManager) | didReg;
        return didReg;
    }

    public String[] filesWithExtension (String relativePath, String fileExtension)
    {
        List<String> results = new ArrayList();
        String fullPath = (StringUtil.nullOrEmptyString(relativePath))
                             ? _path : _path.concat(relativePath);
        if (!fullPath.endsWith("/")) fullPath = fullPath.concat("/");
        if (!fileExtension.startsWith(".")) fileExtension = ".".concat(fileExtension);
        SortedMap<String, String> sub = resourcesWithPrefix(fullPath);
        if (sub != null) {
            for (String name : sub.keySet()) {
                if (name.endsWith(fileExtension)) {
                    String path = name.substring(fullPath.length());
                    if (path.indexOf('/') == -1) results.add(path);
                }
            }
        }

        return (String[])AWUtil.getArray(results, String.class);
    }

    /*
        aribaweb.properties supports several properties to direct resource manager initialization:

          packaged-resource-extensions=<list of extensions>
              - list of file extensions to register as packaged resources
          use-namespace-from-package=<dotted package name>
              - other package from which to copy the AWNamespaceManager Resolver to define awl namespaces)
          depends-on=<jar name (sans .jar extension)>
              - other jars that should be initialized before this one
          initializer=<full path to static initialization function>
              - initialization function that should be called for this jar

        E.g.
          packaged-resource-extensions=awl,htm
          use-namespace-from-package=ariba.ui.widgets
          depends-on=ariba.aribweb
          initializer=ariba.ui.widgets.Widgets.initialize
            
     */
    public static final String AWJarPropertiesPath = "META-INF/aribaweb.properties";
    static final Pattern _URLJarNamePattern = Pattern.compile(".*[/\\\\](.+)\\.(jar|zip)\\!?/.*");;
    static final String _ZipMarker = ".zip!";

    static Map<String, URL> _AWJarUrlsByName = null;
    static Map<String, Properties> _AWJarPropertiesByName = MapUtil.map();

    /**
        Returns list of aribaweb-savvy jars (those containing an aribaweb.properties file)

        @return Map with file name of jar (without extension) as key, and URL to jar as
                value
     */
    public static  Map<String, URL> awJarUrlsByName ()
    {
        if (_AWJarUrlsByName == null) {
            _AWJarUrlsByName = new HashMap();
            Enumeration<URL> urls = null;
            try {
                urls = Thread.currentThread().getContextClassLoader().getResources(AWJarPropertiesPath);
            } catch (IOException e) {
                throw new AWGenericException(e);
            }
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                Matcher m = _URLJarNamePattern.matcher(url.toExternalForm());
                // Assert.that(m.matches(), "Can't find jar name in URL: %s", url);
                if (m.matches()) {
                    String jarName = m.group(1);
                    _AWJarUrlsByName.put(jarName, url);
                }
            }
        }
        return _AWJarUrlsByName;
    }

    public static Properties aribawebPropertiesForName (String jarName)
    {

        Properties properties = _AWJarPropertiesByName.get(jarName);
        if (properties == null) {
            properties = new Properties();
            URL url = awJarUrlsByName().get(jarName);
            Assert.that(url != null, "Lookup of aribawebPropertiesForName for unknown name: %s", jarName);
            try {
                properties.load(url.openStream());
                _AWJarPropertiesByName.put(jarName, properties);
            } catch (IOException e) {
                throw new AWGenericException(e);
            }
        }
        return properties;
    }

    public static Set<String>referencedAWJarNames ()
    {
        Set<String> result = new HashSet();
        for (String fullName : AWClasspathResourceDirectory.awJarUrlsByName().keySet()) {
            result.add(fullName);
            Properties props = aribawebPropertiesForName(fullName);
            String referenced = (String)props.get("inlined-jars");
            if (referenced != null) {
                for (String name : referenced.split(",")) {
                    result.add(name.replaceAll("\\.(jar|zip)$", ""));
                }
            }
        }
        return result;
    }

    public static Object aribawebPropertyValue (String key)
    {
        for (String name : AWClasspathResourceDirectory.awJarUrlsByName().keySet()) {
            Properties props = AWClasspathResourceDirectory.aribawebPropertiesForName(name);
            Object val = props.get(key);
            if (val != null) return val;
        }
        return null;
    }

    /**
        Process all AW jars, registering their resources and running their initializers
     */
    public static void autoRegisterJarResources (final AWMultiLocaleResourceManager resourceManager)
    {
        long startMillis = System.currentTimeMillis();
        // System.out.println("----------- autoRegisterJarResources ------------");
        final String resourceUrl = ((AWConcreteServerApplication) AWConcreteServerApplication.sharedInstance()).resourceUrl();
        // Resource directory for "." (jars)
        final AWClasspathResourceDirectory rd = new AWClasspathResourceDirectory("", resourceUrl);
        rd.setContainsPackagedResources(true);

        Map<String, URL> awJarUrlsByName = awJarUrlsByName();
        final List<String> orderedJarNames = new ArrayList();
        final boolean didLoad = !awJarUrlsByName.isEmpty();
        Set<String> processedJars = new HashSet();

        // Register post-loads after the application has completed initialization
        AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.sharedInstance();

        Map<String, Set<String>> jarToLoadedPackageNames = MapUtil.map();
        for (Map.Entry<String, URL> e : awJarUrlsByName.entrySet()) {
            initJar(resourceManager, rd, e.getValue(), e.getKey(), awJarUrlsByName,
                    processedJars, orderedJarNames, jarToLoadedPackageNames);
        }

        long runtime = System.currentTimeMillis() - startMillis;
        /// System.out.printf("*** Jar scan time = %f", ((float)runtime)/1000);

        // fire the initializers
        for (String jarName : orderedJarNames) {
            Properties properties = aribawebPropertiesForName(jarName);
            String initializer = (String)properties.get("initializer");
            if (initializer != null) {
                AWBinding.fieldBinding("initializer", initializer, null).value(null);
            }

            Set<String> loadedPackageNames = jarToLoadedPackageNames.get(jarName);
            // set the namespace resolver for these packages
            String parentPackage = (String)properties.get("use-namespace-from-package");
            if (parentPackage != null) {
                AWNamespaceManager ns = AWNamespaceManager.instance();
                AWNamespaceManager.Resolver resolver = ns.resolverForPackage(parentPackage);
                Assert.that(resolver != null, "Couldn't find resolver for package: %s", parentPackage);

                String namespaceId = (String)properties.get("namespace-identifier");
                if (namespaceId != null) {
                    resolver = new AWNamespaceManager.Resolver(resolver);
                    resolver.addIncludeToNamespace(namespaceId, new AWNamespaceManager.Import(
                            new ArrayList(loadedPackageNames), Arrays.asList("")));
                }

                for (String packageName : loadedPackageNames) {
                    ns.registerResolverForPackage(packageName, resolver);
                }
            }
        }

        // register to run post initializers after application done initializing
        application.registerDidInitCallback(new AWConcreteApplication.DidInitCallback() {
            public void applicationDidInit (AWConcreteApplication application) {
                // Run the post-initializers
                for (String jarName : orderedJarNames) {
                    Properties properties = aribawebPropertiesForName(jarName);
                    String postInitializer = (String)properties.get("post-initializer");
                    if (postInitializer != null) {
                        AWBinding.fieldBinding("post-initializer", postInitializer, null).value(null);
                    }
                }

                if (didLoad) {
                    // register root resource directory (".")
                    resourceManager.registerResourceDirectory(rd);

                    // register docroot resource directory
                    if (hasResourcesUnderPath(resourcePrefix())) {
                        AWClasspathResourceDirectory docrootDir =
                                new AWClasspathResourceDirectory(resourcePrefix(), resourceUrl);
                        docrootDir.setContainsPackagedResources(true);
                        resourceManager.registerResourceDirectory(docrootDir);
                    }
                }
            }
        });        
    }

    static void initJar (final AWMultiLocaleResourceManager resourceManager,
                         final AWClasspathResourceDirectory rd,
                         URL url, String jarName,
                         Map<String, URL> awJarUrlsByName,  Set<String> processedJars,
                         List<String> orderedJarNames, Map<String, Set<String>> jarToLoadedPackageNames)
    {
        if (processedJars.contains(jarName)) return;
        processedJars.add(jarName);

        try {
            Properties properties = aribawebPropertiesForName(jarName);

            boolean isZip = url.toExternalForm().contains(_ZipMarker);
            boolean shouldRunInitializers = !isZip || Boolean.valueOf((String)properties.get("run-in-zip"));

            String dependsOn = (String)properties.get("depends-on");
            if (dependsOn != null) {
                for (String dep : dependsOn.split(",")) {
                    URL depUrl = awJarUrlsByName.get(dep);
                    Assert.that(depUrl != null || !shouldRunInitializers,
                            "Couldn't find jar \'%s\" referenced in depends-on in jar: %s", dep, url);
                    if (depUrl != null) initJar(resourceManager, rd, depUrl, dep, awJarUrlsByName, processedJars,
                            orderedJarNames, jarToLoadedPackageNames);
                }
            }

            if (shouldRunInitializers) {
                orderedJarNames.add(jarName);
            }

            // fire the pre-initializer (if any)
            String preInitializer = (String)properties.get("pre-initializer");
            if (preInitializer != null && shouldRunInitializers) {
                AWBinding.fieldBinding("pre-initializer", preInitializer, null).value(null);
            }

            // register any added packaged resource extensions
            final Set<String> loadedPackageNames = new HashSet();
            String extString = (String)properties.get("packaged-resource-extensions");
            final List packagedResourceExtensions = new ArrayList();
            if (extString != null) {
                for (String ext : extString.split(",")) {
                    packagedResourceExtensions.add(ext.startsWith(".") ? ext : ".".concat(ext));
                    resourceManager.registerPackagedResourceExtension(ext);
                }
            }

            // walk the jar files, analyzing all classes and recording all resources
            URL jar = AWJarWalker.ClasspathUrlFinder.findResourceBase(url, AWJarPropertiesPath);
            AWJarWalker.StreamIterator iter = AWJarWalker.create(jar, new AWJarWalker.Filter() {
                public boolean accepts(String filename)
                {
                    // System.out.println("checking " + filename);
                    return true;
                }
            });

            while (iter.next()) {
                String filename = iter.getFilename();
                if (filename.endsWith(".class")) {
                    // System.out.println("recording " + filename);
                    AWJarWalker.processBytecode(iter, filename);
                }
                else {
                    if (extString != null && shouldRunInitializers) {
                        String urlString = iter.getURLString();
                        AWClasspathResourceDirectory.recordResourcePath(filename, urlString);
                        int index = filename.lastIndexOf('.');
                        if (index > 0) {
                            String ext = filename.substring(index);
                            if (packagedResourceExtensions.contains(ext)) {
                                _registerClassPathResource(filename, rd, resourceManager, loadedPackageNames);
                            }
                        }
                    }
                }
            }

            jarToLoadedPackageNames.put(jarName, loadedPackageNames);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    static void _registerClassPathResource (String relativePath,
                                            AWResourceDirectory rd,
                                            AWMultiLocaleResourceManager resourceManager,
                                            Set loadedPackageNames)
    {
        relativePath = relativePath.replace('\\', '/');
        boolean isPackaged = false;
        String fileName = relativePath;
        int lastSlash = relativePath.lastIndexOf('/');
        if (lastSlash > 0) {
            String packageName = fileName.substring(0,lastSlash).replace("/", ".");
            fileName = fileName.substring(lastSlash + 1);

            // make sure package is registered
            if (!loadedPackageNames.contains(packageName)) {
                loadedPackageNames.add(packageName);
                resourceManager.registerPackageName(packageName);
            }
            isPackaged = true;
        }

        resourceManager.addResource(fileName, relativePath, isPackaged, rd);
    }
}
