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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWClassLoader.java#12 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ClassUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.ui.aribaweb.core.AWConcreteApplication;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Loader abstraction used by AW to load (and reload) classes for AWComponents.
 * Subclasses can support dynamic class reloading (AWReload), alternate languages (Groovy, etc)
 */
public class AWClassLoader
{
    public static final String ClassReloadedTopic = "AWClassesReloaded";

    AWClassLoader _next;
    
    /**
        Load class.

        @return null if the class is not found, the class (or a
            suitable subclass) otherwise
    */
    public Class getClass (String className) throws ClassNotFoundException
    {
        return (_next != null) ? _next.getClass(className)
                               : ClassUtil.classForName(className, false);
    }

    /**
        @return true if the class might be reloaded, so should not be cached.
    */
    public boolean isReloadable (String name)
    {
        return (_next != null) && _next.isReloadable(name);
    }

    /**
        @param className the name of the class for the component.
        @return The name of the component.

        The className may be that of a subclass of the actual class, not the
        real className.
    */
    public String getComponentNameForClass (String className)
    {
        return (_next != null) ? _next.getComponentNameForClass(className) : className;
    }

    /**
        For class loaders that support reloading non-AWComponent classes, this is a
        trigger point to scan previously loaded files to see if any should be reloaded
        (e.g. scanning .class files in the build directory)
     */
    public void checkForUpdates ()
    {
        if (_next != null) _next.checkForUpdates();
    }

    public Class checkReloadClass (Class cls)
    {
        return (_next != null) ? _next.checkReloadClass(cls) : cls;
    }

    /**
        The chained classloader is forwarded all calls not fully handled by the
        first AWClassLoader (when the first one calls super()).

        @param next the loader to receive forwarded calls
     */
    public void setChainedClassLoader (AWClassLoader next)
    {
        _next = next;
    }

    protected void notifyReload(List<String> reloadedClassNames)
    {
        AWNotificationCenter.notify(AWClassLoader.ClassReloadedTopic, reloadedClassNames);
    }

    public static class LoadInfo {
        public Class cls;
        public AWResource resource;
        public long loadTime;

        public LoadInfo (Class c, AWResource r, long t) { cls = c; resource = r; loadTime = t; }

        public void updateLoadTime () { loadTime = System.currentTimeMillis(); } 
    }

    /**
     * Loader subclass for loaders that look for a source file sitting alongside the awl
     */
    public static abstract class PairedFileLoader extends AWClassLoader
    {
        GrowOnlyHashtable _NameToInfo = new GrowOnlyHashtable();
        GrowOnlyHashtable _ClassToInfo = new GrowOnlyHashtable();
        List<LoadInfo> _SourceInfos = new ArrayList();

        public Class getClass (String className) throws ClassNotFoundException
        {
            LoadInfo info = infoForClassName(className);
            return (info.cls != null) ? info.cls : super.getClass(className);
        }

        public boolean isReloadable (String className)
        {
            LoadInfo info = infoForClassName(className);
            return (info.resource != null) ? true : super.isReloadable(className);
        }

        public Class checkReloadClass (Class cls)
        {
            LoadInfo info = (LoadInfo)_ClassToInfo.get(cls);
            if (info == null || info.resource == null) return super.checkReloadClass(cls);
            if (info.resource.lastModified() > info.loadTime) {
                cls = loadClassFromResource(cls.getName(), info.resource);
                info.updateLoadTime();
                // let others know we did a reload
                notifyReload(Arrays.asList(cls.getName()));
            }
            return info.cls;
        }

        public void checkForUpdates()
        {
            List<LoadInfo>updated = findUpdatedSourceFiles();
            if (updated != null) {
                List<String> reloadedClassNames = reloadSourceFiles(updated);
                notifyReload(reloadedClassNames);
            }
        }

        protected List<LoadInfo> findUpdatedSourceFiles ()
        {
            synchronized (_SourceInfos) {
                List<LoadInfo> needReloading = null;
                for (LoadInfo info : _SourceInfos) {
                    if (info.resource.lastModified() > info.loadTime) {
                        if (needReloading == null) needReloading = new ArrayList();
                        needReloading.add(info);
                    }
                }
                return needReloading;
            }
        }

        protected List<String> reloadSourceFiles (List<LoadInfo> classInfos)
        {
            List<String>reloaded = new ArrayList();

            for (LoadInfo info : classInfos) {
                String className = info.cls.getName();
                compileSourceFromResource(className, info.resource);
                info.updateLoadTime();
                reloaded.add(className);
            }
            return reloaded;
        }

        protected AWResource sourceResourceForClassName (String className)
        {
            AWSingleLocaleResourceManager resourceManager =
                AWConcreteApplication.SharedInstance.resourceManager(Locale.US);
            String origClassName = className.replace('.','/');
            String sourceFileName = origClassName + sourceFileExtension();

            return resourceManager.resourceNamed(sourceFileName);
        }

        protected LoadInfo existingInfoForClassName (String className)
        {
            return (LoadInfo)_NameToInfo.get(className);
        }
        
        protected LoadInfo infoForClassName (String className)
        {
            LoadInfo info = (LoadInfo)_NameToInfo.get(className);
            if (info == null) {
                Class cls = ClassUtil.classForName(className, false);
                AWResource resource = sourceResourceForClassName(className);
                if (cls != null) {
                    // Todo: check resource date against jar for class
                    noteClass(cls);
                }
                else {
                    if (resource != null) cls = loadClassFromResource(className, resource);
                }
                info = recordClassForResource(className, cls, resource);
            }
            return info;
        }

        protected void noteClass (Class cls) {}

        protected LoadInfo recordClassForResource (String name, Class cls, AWResource sourceResource)
        {
            LoadInfo info = (LoadInfo)_NameToInfo.get(name);
            if (info != null) {
                info.cls = cls;
                info.loadTime = System.currentTimeMillis();
            } else {
                info = new LoadInfo(cls, sourceResource, System.currentTimeMillis());
                _NameToInfo.put(name, info);
                if (sourceResource != null) {
                    synchronized (_SourceInfos) {
                        _SourceInfos.add(info);
                    }
                }
            }
            if (cls != null) {
                _ClassToInfo.put(cls, info);
            }
            return info;
        }

        protected abstract String sourceFileExtension ();

        protected abstract void compileSourceFromResource (String className, AWResource sourceResource);

        protected Class loadClassFromResource (String className, AWResource sourceResource)
        {
            compileSourceFromResource(className, sourceResource);
            try {
                return getClass(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }
}
