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

    Based on "scannotation" (http://scannotation.sourceforge.net/)
    (by <a href="mailto:bill@burkecentral.com">Bill Burke</a>)
    also licensed under Apache v 2.0.
    
    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWJarWalker.java#2 $
*/
package ariba.ui.aribaweb.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AWJarWalker
{
    private static final ConcurrentHashMap<String, DirectoryIteratorFactory> registry
            = new ConcurrentHashMap<String, DirectoryIteratorFactory>();

    static {
        registry.put("file", new FileProtocolIteratorFactory());
    }


    public static URL[] findWebInfLibClasspaths(ServletContextEvent servletContextEvent)
    {
        return WarUrlFinder.findWebInfLibClasspaths(servletContextEvent);
    }

    public static URL[] findClassPaths()
    {
        return ClasspathUrlFinder.findClassPaths();
    }

    public static StreamIterator create(URL url, Filter filter) throws IOException
    {
        String urlString = url.toString();
        if (urlString.endsWith("!/")) {
            urlString = urlString.substring(4);
            urlString = urlString.substring(0, urlString.length() - 2);
            url = new URL(urlString);
        }

        if (!urlString.endsWith("/")) {
            return new JarIterator(url, filter);
        } else {
            DirectoryIteratorFactory factory = registry.get(url.getProtocol());
            if (factory == null)
                throw new IOException("Unable to scan directory of protocol: " + url.getProtocol());
            return factory.create(url, filter);
        }
    }


    public interface StreamIterator
    {
        /**
         * @return true until past last item
         */
        boolean next();

        /**
         * User is resposible for closing the InputStream returned
         *
         * @return null if no more streams left to iterate on
         */
        InputStream getInputStream ();

        /**
         * @return file name (relative path)
         */
        String getFilename ();

        /**
         * @return URL for current item
         */
        String getURLString ();

        /**
         * Cleanup any open resources of the iterator
         */
        void close();
    }

    public interface Filter
    {
        boolean accepts(String filename);
    }

    public interface UrlFilter
    {
        boolean accepts(URL url);
    }

    public interface DirectoryIteratorFactory
    {
        StreamIterator create(URL url, Filter filter) throws IOException;
    }

    public static class InputStreamWrapper extends InputStream
    {
        private InputStream delegate;

        public InputStreamWrapper(InputStream delegate)
        {
            this.delegate = delegate;
        }

        public int read()
                throws IOException
        {
            return delegate.read();
        }

        public int read(byte[] bytes)
                throws IOException
        {
            return delegate.read(bytes);
        }

        public int read(byte[] bytes, int i, int i1)
                throws IOException
        {
            return delegate.read(bytes, i, i1);
        }

        public long skip(long l)
                throws IOException
        {
            return delegate.skip(l);
        }

        public int available()
                throws IOException
        {
            return delegate.available();
        }

        public void close()
                throws IOException
        {
            // ignored
        }

        public void mark(int i)
        {
            delegate.mark(i);
        }

        public void reset()
                throws IOException
        {
            delegate.reset();
        }

        public boolean markSupported()
        {
            return delegate.markSupported();
        }
    }

    public static class JarIterator implements StreamIterator
    {
        JarInputStream jar;
        JarEntry next;
        Filter filter;
        boolean initial = true;
        boolean closed = false;
        String urlPrefix;

        public JarIterator(URL url, Filter filter) throws IOException
        {
            urlPrefix = "jar:".concat(url.toExternalForm()).concat("!/");
            this.filter = filter;
            jar = new JarInputStream(url.openStream());
        }

        private void setNext()
        {
            initial = true;
            try {
                if (next != null) jar.closeEntry();
                next = null;
                do {
                    next = jar.getNextJarEntry();
                }
                while (next != null && (next.isDirectory() || (filter == null || !filter.accepts(next.getName()))));
                if (next == null) {
                    close();
                }
            }
            catch (IOException e) {
                throw new RuntimeException("failed to browse jar", e);
            }
        }

        public boolean next()
        {
            if (closed || (next == null && !initial)) return false;
            setNext();
            return (next != null);
        }

        public InputStream getInputStream ()
        {
            return (next == null) ? null : new InputStreamWrapper(jar);
        }

        public String getFilename()
        {
            return next.getName();
        }

        public String getURLString()
        {
            return urlPrefix.concat(next.getName());
        }

        public void close()
        {
            try {
                closed = true;
                jar.close();
            }
            catch (IOException ignored) {

            }

        }
    }

    public static class FileIterator implements StreamIterator
    {
        private ArrayList<File> files;
        private int index = 0;

        public FileIterator(File file, Filter filter)
        {
            files = new ArrayList();
            try {
                create(files, file, filter);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected static void create(List list, File dir, Filter filter) throws Exception
        {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    create(list, files[i], filter);
                } else {
                    if (filter == null || filter.accepts(files[i].getAbsolutePath())) {
                        list.add(files[i]);
                    }
                }
            }
        }

        public boolean next()
        {
            return (index++ < files.size());
        }

        public InputStream getInputStream ()
        {
            if (index >= files.size()) return null;
            File fp = (File) files.get(index);
            try {
                return new FileInputStream(fp);
            }
            catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public String getFilename()
        {
            return (index >= files.size()) ? null : files.get(index).getAbsolutePath();
        }

        public String getURLString()
        {
            try {
                return (index >= files.size()) ? null
                        : ((File)files.get(index)).toURL().toExternalForm();
            } catch (MalformedURLException e) {
                throw new AWGenericException(e);
            }
        }

        public void close()
        {
        }
    }

    public static class FileProtocolIteratorFactory implements DirectoryIteratorFactory
    {

        public StreamIterator create(URL url, Filter filter) throws IOException
        {
            File f = new File(url.getPath());
            if (f.isDirectory()) {
                return new FileIterator(f, filter);
            } else {
                return new JarIterator(url, filter);
            }
        }
    }

    public static class ClasspathUrlFinder
    {

        /**
         * Find the classpath URLs for a specific classpath resource.  The classpath URL is extracted
         * from loader.getResources() using the baseResource.
         *
         * @param baseResource
         * @return
         */
        public static URL[] findResourceBases(String baseResource, ClassLoader loader, UrlFilter filter)
        {
            ArrayList<URL> list = new ArrayList<URL>();
            try {
                Enumeration<URL> urls = loader.getResources(baseResource);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    if (filter == null || filter.accepts(url)) {
                        list.add(findResourceBase(url, baseResource));
                    }
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return list.toArray(new URL[list.size()]);
        }

        /**
         * Find the classpath URLs for a specific classpath resource.  The classpath URL is extracted
         * from loader.getResources() using the baseResource.
         *
         * @param baseResource
         * @return
         */
        public static URL[] findResourceBases(String baseResource)
        {
            return findResourceBases(baseResource, Thread.currentThread().getContextClassLoader(), null);
        }

        public static URL[] findResourceBasesContainingManifestKey (String manifestKey)
        {
            final Attributes.Name Key = new Attributes.Name(manifestKey);

            return findResourceBases("META-INF/MANIFEST.MF",
                    Thread.currentThread().getContextClassLoader(),
                    new UrlFilter() {
                        public boolean accepts(URL url)
                        {
                            try {
                                return new Manifest(url.openStream()).getMainAttributes().get(Key) != null;
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
        }

        public static URL findResourceBase(URL url, String baseResource)
        {
            String urlString = url.toString();
            int idx = urlString.lastIndexOf(baseResource);
            urlString = urlString.substring(0, idx);
            URL deployUrl = null;
            try {
                deployUrl = new URL(urlString);
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return deployUrl;
        }

        /**
         * Find the classpath URL for a specific classpath resource.  The classpath URL is extracted
         * from Thread.currentThread().getContextClassLoader().getResource() using the baseResource.
         *
         * @param baseResource
         * @return
         */
        public static URL findResourceBase(String baseResource)
        {
            return findResourceBase(baseResource, Thread.currentThread().getContextClassLoader());
        }

        /**
         * Find the classpath URL for a specific classpath resource.  The classpath URL is extracted
         * from loader.getResource() using the baseResource.
         *
         * @param baseResource
         * @param loader
         * @return
         */
        public static URL findResourceBase(String baseResource, ClassLoader loader)
        {
            URL url = loader.getResource(baseResource);
            return findResourceBase(url, baseResource);
        }

        /**
         * Find the classpath for the particular class
         *
         * @param clazz
         * @return
         */
        public static URL findClassBase(Class clazz)
        {
            String resource = clazz.getName().replace('.', '/') + ".class";
            return findResourceBase(resource, clazz.getClassLoader());
        }

        /**
         * Uses the java.class.path system property to obtain a list of URLs that represent the CLASSPATH
         *
         * @return
         */
        public static URL[] findClassPaths()
        {
            List<URL> list = new ArrayList<URL>();
            String classpath = System.getProperty("java.class.path");
            StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);

            while (tokenizer.hasMoreTokens()) {
                String path = tokenizer.nextToken();
                File fp = new File(path);
                if (!fp.exists()) continue; // throw new RuntimeException("File in java.class.path does not exist: " + fp);
                try {
                    list.add(fp.toURL());
                }
                catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            return list.toArray(new URL[list.size()]);
        }

        /**
         * Uses the java.class.path system property to obtain a list of URLs that represent the CLASSPATH
         * <p/>
         * paths is used as a filter to only include paths that have the specific relative file within it
         *
         * @param paths comma list of files that should exist in a particular path
         * @return
         */
        public static URL[] findClassPaths(String... paths)
        {
            ArrayList<URL> list = new ArrayList<URL>();

            String classpath = System.getProperty("java.class.path");
            StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
            for (int i = 0; i < paths.length; i++) {
                paths[i] = paths[i].trim();
            }

            while (tokenizer.hasMoreTokens()) {
                String path = tokenizer.nextToken().trim();
                boolean found = false;
                for (String wantedPath : paths) {
                    if (path.endsWith(File.separator + wantedPath)) {
                        found = true;
                        break;
                    }
                }
                if (!found) continue;
                File fp = new File(path);
                if (!fp.exists())
                    throw new RuntimeException("File in java.class.path does not exists: " + fp);
                try {
                    list.add(fp.toURL());
                }
                catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            return list.toArray(new URL[list.size()]);
        }
    }

    public static class WarUrlFinder
    {
        public static URL[] findWebInfLibClasspaths(ServletContextEvent servletContextEvent)
        {
            ServletContext servletContext = servletContextEvent.getServletContext();
            return findWebInfLibClasspaths(servletContext);
        }

        public static URL[] findWebInfLibClasspaths(ServletContext servletContext)
        {
            ArrayList<URL> list = new ArrayList<URL>();
            Set libJars = servletContext.getResourcePaths("/WEB-INF/lib");
            for (Object jar : libJars) {
                try {
                    list.add(servletContext.getResource((String) jar));
                }
                catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            return list.toArray(new URL[list.size()]);
        }

        public static URL findWebInfClassesPath(ServletContextEvent servletContextEvent)
        {
            ServletContext servletContext = servletContextEvent.getServletContext();
            return findWebInfClassesPath(servletContext);
        }

        /**
         * Find the URL pointing to "/WEB-INF/classes"  This method may not work in conjunction with IteratorFactory
         * if your servlet container does not extract the /WEB-INF/classes into a real file-based directory
         *
         * @param servletContext
         * @return null if cannot determin /WEB-INF/classes
         */
        public static URL findWebInfClassesPath(ServletContext servletContext)
        {
            String path = servletContext.getRealPath("/WEB-INF/classes");
            if (path == null) return null;
            File fp = new File(path);
            if (fp.exists() == false) return null;
            try {
                return fp.toURL();
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}