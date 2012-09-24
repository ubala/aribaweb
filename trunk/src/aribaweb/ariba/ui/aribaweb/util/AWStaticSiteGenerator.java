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

    $ $
*/
package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBaseRequest;
import ariba.ui.aribaweb.core.AWDefaultApplication;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.util.core.Assert;
import ariba.util.core.URLUtil;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.InputStream;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.net.MalformedURLException;

/**
    Used to statically render a site by internally "spidering".

    How it works:
        - As page is rendered, actions are *immediately evaluated* to get the destination component.
        - The URL for the action results are then determined (by asking the component its Naming.staticPath()
          (if it knows how to determine is static URL from its state) or using the template name (if not).
        - Components with previously unseeing URLs are enqueued for generation as well.
        - Resources urls (for .css, .js, and images) trigger callbacks.  Their files are copied and an aliased
          url (relative to the current component's relative path) is returned.
        - Referenced .css files are scanned for additional resource ("url(...)") references, and these are
          processed as well.

    Limitations:
        This is only intended for a limited class of (essentially stateless) navigation only sites.
        E.g.:
            - No Dynamic forms
            - No components that update internal state and return same component (e.g. internal tab sets)

    Note that this class does have a main() and can be used to generate a site for an appropriately formatted
    source directory (without compiling it), as long as the site uses only static .awls, expr, and inline groovy.

    This support is used to generate the aribaweb.org static website.
 */
public class AWStaticSiteGenerator
{
    static FakeRequest _fakeRequest = new FakeRequest(new FakeHttpSession());

    public interface Naming {
        String staticPath ();
    }

    File _outputDir;
    String _siteUrlPrefix = "/";
    String _resourcesPath = "resource/";
    Queue<String> _urlsToProcess = new LinkedList();
    Map<String, AWComponent> _componentForUrl = new HashMap();
    Map<String, AWResource> _resourceForRelativePath = new HashMap();
    URL _currentUrl = url("http://NOTINITIALIZED");
    Map<String, String>_aliasForPath = new HashMap();

    public AWStaticSiteGenerator(File outputDir)
    {
        _outputDir = outputDir;
        URL urlObject = URLUtil.urlAbsolute(_outputDir);
        _siteUrlPrefix = urlObject.toExternalForm();
        AWConcreteApplication.setStaticizer(this);
    }

    void noteResource (String relativePath, AWResource resource)
    {
        System.out.printf("  -- Ref to resource %s\n", relativePath);
        if (relativePath.endsWith(".oss") || relativePath.endsWith(".awl")) return;
        if (_resourceForRelativePath.get(relativePath) == null) {
            _resourceForRelativePath.put(relativePath, resource);
            File resourceFile = fileForResource(relativePath);
            System.out.printf("  -- Writing to file %s\n", resourceFile.getAbsolutePath());
            resourceFile.getParentFile().mkdirs();
            byte[] bytes = AWUtil.getBytes(resource.inputStream());
            AWUtil.writeToFile(bytes, resourceFile);
            if (relativePath.endsWith(".css")) noteResourcesInCss(resourceFile, resource);
        }
    }

    Pattern _CssUrlRef = Pattern.compile("url\\s*\\(\\s*[\"\']?([\\w\\.\\-]+)[\"\']?\\s*\\)");

    void noteResourcesInCss (File file, AWResource cssResource)
    {
        final AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.sharedInstance();
        String css = AWUtil.stringWithContentsOfInputStream(cssResource.inputStream());
        Matcher m = _CssUrlRef.matcher(css);
        while (m.find()) {
            String ref = m.group(1);
            AWResource resource = cssResource.relativeResource(ref , application.resourceManager());
            System.out.printf("     -- CSS %s ref to resource %s (%s)\n", file.getPath(), ref, resource.name());
            noteResource(resource.name(), resource);
        }
    }

    private File fileForResource(String relativePath) {
        File resourceFile = new File(fileForComponentPath(_resourcesPath), relativePath);
        return resourceFile;
    }

    public String formatUrlForResource (String urlPrefix, AWResource resource, boolean forCache)
    {
        // can't cache: need to reeval for each source component
        if (forCache) return null;

        String relativePath = resource.relativePath().replace('\\', '/');
        noteResource(relativePath, resource);
        return AWUtil.relativeUrlString(_currentUrl, URLUtil.url(fileForResource(relativePath)));
        // return StringUtil.strcat(_siteUrlPrefix, _resourcesPath, relativePath);
    }


    String urlForComponent (AWComponent component)
    {
        String url = (component instanceof Naming) ? ((Naming)component).staticPath()
                                             : component.templateName();
        if (!url.endsWith(".htm")) url = AWUtil.pathToLastComponent(url, ".") + ".htm";
        if (url.length() > 1 && url.startsWith("/")) url = url.substring(1);
        return url;
    }

    public String note (AWComponent component)
    {
        if (component instanceof AWRedirect) return ((AWRedirect)component).url();
        String url = urlForComponent(component);
        url = url.replaceAll("\\\\", "/");
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        System.out.printf("  -- Ref to %s\n", url);
        if (_componentForUrl.get(url) == null) {
            System.out.printf("  -- Enqueing %s\n", url);
            _componentForUrl.put(url, component);
            _urlsToProcess.offer(url);
        }
        // return StringUtil.strcat(_siteUrlPrefix, url);
        url = AWUtil.relativeUrlString(_currentUrl, URLUtil.url(fileForComponentPath(url)));
        String alias = _aliasForPath.get(url);
        if (alias != null) return alias;
        return url;        
    }

    private File fileForComponentPath(String url) {
        return new File(_outputDir, url);
    }

    static URL url (String url)
    {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new AWGenericException("Error formatting URL", e);
        }
    }
    
    void processComponent (AWComponent component, String relativePath)
    {
        System.out.printf("> Processing %s\n", relativePath);
        File outputFile = fileForComponentPath(relativePath);
        _currentUrl = URLUtil.url(outputFile);
        Assert.that (!outputFile.exists(), "Collision! File already exists: %s", relativePath);
        outputFile.getParentFile().mkdirs();
        String output = component.generateStringContents();
        AWUtil.writeToFile(output, outputFile);
    }

    void processAll ()
    {
        String url;
        while ((url = _urlsToProcess.poll()) != null) {
            AWComponent component = _componentForUrl.get(url);
            processComponent(component, url);
        }
    }

    public void didCreateRequestContext (AWRequestContext requestContext)
    {
        requestContext._overrideRequest(_fakeRequest);
    }

    public void processFromRoot (AWComponent startPage)
    {
        note(startPage);
        processAll();
    }

    static String AppDir = null;
    public static void main (String[] args)
    {
        Assert.that(args.length >= 1 && args.length <= 4, "Usage: AWStaticizer outputDir [appDirectory] [mainComponentName] [mainAlias]");
        File outputDir = new File(args[0]);
        AWStaticSiteGenerator staticizer = new AWStaticSiteGenerator(outputDir);

        if (args.length > 1) {
            File appDir = new File(args[1]);
            Assert.that(appDir.isDirectory(), "Not a directory: %s", args[1]);
            // AWConcreteServerApplication.registerDebugSearchPath(appDir.getAbsolutePath());
            AppDir = appDir.getAbsolutePath();
        }

        final AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.createApplication(
                ExtendedDefaultApplication.class.getName(), ExtendedDefaultApplication.class);
        AWConcreteApplication.IsDebuggingEnabled = false;
        
        Assert.that(outputDir.isDirectory(), "Not a directory: %s", args[0]);
        AWComponent startPage;
        String mainName = application.mainPageName();

        if (args.length > 2 && !args[2].equals(".")) {
            mainName = args[2];
            startPage = AWComponent.createPageWithName(mainName);
        }
        else {
            startPage = (AWComponent)application.mainPage(application.createRequestContext(_fakeRequest));
        }

        if (args.length > 3) {
            staticizer._aliasForPath.put(staticizer.urlForComponent(startPage), args[3]);
        }

        Assert.that(startPage != null, "Failed to create component named: %s", mainName);

        staticizer.processFromRoot(startPage);
    }

    public static class ExtendedDefaultApplication extends AWDefaultApplication
    {
        public void registerResourceDirectories (AWMultiLocaleResourceManager resourceManager) {
            super.registerResourceDirectories(resourceManager);
            if (AppDir != null) {
                resourceManager().registerPackageName("");
                AWNamespaceManager ns = AWNamespaceManager.instance();
                ns.registerResolverForPackage("", ns.resolverForPackage("ariba.ui.meta"));
                ns.registerResolverForPackage("app", ns.resolverForPackage("ariba.ui.meta"));

                resourceManager().registerPackageName("app");

                resourceManager().registerResourceDirectory(AppDir, "/");
                File resourceDir = new File(AppDir, "resource/webserver/branding");
                if (resourceDir.exists()) resourceManager().registerResourceDirectory(resourceDir.getPath(), "/");

                resourceManager().registerResourceDirectory(AppDir, "/");
                resourceDir = new File(AppDir, "resource/webserver");
                if (resourceDir.exists()) resourceManager().registerResourceDirectory(resourceDir.getPath(), "/");
            }
        }

        protected String initAdaptorUrl ()
        {
            return "http://STATIC_SITE";
        }

        @java.lang.Override
        public String name ()
        {
            return "StaticSite";
        }
    }

    static class FakeRequest extends AWBaseRequest
    {
        HttpSession _session;

        FakeRequest (HttpSession session) {
            _session = session;
        }

        public HttpSession getSession(boolean shouldCreate) {
            return _session;
        }

        protected int applicationNumberInt() {
            return 0;
        }

        public InputStream inputStream() {
            return null;
        }

        protected byte[] initContent() {
            return new byte[0];
        }

        protected String initSessionId() {
            return null;
        }

        public String remoteHost() {
            return null;
        }

        public String remoteHostAddress() {
            return null;
        }

        public String method() {
            return null;
        }

        public String headerForKey(String requestHeaderKey) {
            return null;
        }

        public AWFileData fileDataForKey(String formValueKey) {
            return null;
        }

        public Map headers() {
            return null;
        }

        public String requestHandlerKey() {
            return null;
        }

        public String[] requestHandlerPath() {
            return new String[0];
        }

        public String uri() {
            return null;
        }

        public String queryString() {
            return null;
        }

        public String serverPort() {
            return null;
        }

        public String requestString() {
            return null;
        }
    }

    static class FakeHttpSession implements HttpSession {
        Map _attributes = new HashMap();

        public long getCreationTime() {
            return 0;
        }

        public String getId() {
            return null;
        }

        public long getLastAccessedTime() {
            return 0;
        }

        public ServletContext getServletContext() {
            return null;
        }

        public void setMaxInactiveInterval(int i) {

        }

        public int getMaxInactiveInterval() {
            return 0;
        }

        public HttpSessionContext getSessionContext() {
            return null;
        }

        public Object getAttribute(String s) {
            return _attributes.get(s);
        }

        public Object getValue(String s) {
            return _attributes.get(s);
        }

        public Enumeration getAttributeNames() {
            return new IteratorEnumeration(_attributes.keySet().iterator());
        }

        public String[] getValueNames() {
            Collection<String> names = _attributes.keySet();
            return names.toArray(new String[names.size()]);
        }

        public void setAttribute(String s, Object o) {
            _attributes.put(s, o);
        }

        public void putValue(String s, Object o) {
            _attributes.put(s, o);
        }

        public void removeAttribute(String s) {
            _attributes.remove(s);
        }

        public void removeValue(String s) {
            _attributes.remove(s);
        }

        public void invalidate() {

        }

        public boolean isNew() {
            return false;
        }
    }

    static class IteratorEnumeration implements Enumeration {
        Iterator _iter;

        IteratorEnumeration(Iterator iter) {
            _iter = iter;
        }

        public boolean hasMoreElements() {
            return _iter.hasNext();
        }

        public Object nextElement() {
            return _iter.next();
        }
    }
}
