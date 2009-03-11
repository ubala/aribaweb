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

    $Id: //ariba/platform/ui/awreload/ariba/awreload/JavaReloadClassLoader.java#4 $
*/
package ariba.awreload;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWClassLoader;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.util.core.Assert;
import ariba.util.core.ClassFactory;
import ariba.util.core.Compare;
import ariba.util.core.Sort;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.fieldvalue.CompiledAccessorFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.security.ProtectionDomain;

public class JavaReloadClassLoader extends AWClassLoader.PairedFileLoader implements ClassFactory
{
    String BuildRoot;
    String TimestampPath = StringUtil.strcat(BuildRoot, "/lastReload.touch");
    File[] _SingleRoot = null;
    boolean _IsAntBuild = false;
    long _LastClassCheck = 0;
    WatchedDir _BuildRootDir = null;

    static {
        HotSwapFactory.instance();
    }

    public JavaReloadClassLoader ()
    {
        // "CafePath" is an Ariba-ism
        String root = AWUtil.getenv("AW_RELOAD_CLASSES_ROOT");
        if (!StringUtil.nullOrEmptyString(root)) {
            _IsAntBuild = true;
        } else {
            root = AWUtil.getenv("ARIBA_BUILD_ROOT");
        }
        File buildRootDir = new File(root);
        File cafeDir = new File(buildRootDir, "classes/cafe/");
        if (cafeDir.exists()) {
            _SingleRoot = new File[] { cafeDir };
            BuildRoot = cafeDir.toString();
        }
        else  {
            BuildRoot = buildRootDir.toString();
            _BuildRootDir = new WatchedDir(buildRootDir, DirectoryFilter);
        }

        // Count as changed only files updated since now...
        // touch(new File(TimestampPath));
        _LastClassCheck = System.currentTimeMillis();
    }

    File[] getClassRoots ()
    {
        return (_SingleRoot != null) ? _SingleRoot : _BuildRootDir.files();
    }


    public Class forName (String className)
    {
        if (className == null) {
            return null;
        }

        LoadInfo info = existingInfoForClassName(className);
        if (info != null) return info.cls;

        //Do same stuff as as what ClassUtil.classForName does.
        //I can't call that method directly because it would
        //cause an infinite loop.

        return _classForName(className);
    }

    static Class _classForName(String className) {
        try {
            // load it from the normal class path
            Class classObj = Class.forName(className); // OK

            return classObj;
        }
        catch (ClassNotFoundException e) {
            //System.out.println("Stage 2: CNFException"+className+e);
            return null;
        }
        catch (NoClassDefFoundError e) {
            //System.out.println("State 2: NCDFError"+className+e);
                // Supposed you as for class foo.bar.Bazqux, and on NT
                // it finds the class file foo/bar/BazQux.class. On
                // JDK11, this was just a ClassNotFoundException. On
                // JDK12, this throws a NoClassDefFoundError. We
                // always report the error in this case, because
                // someone probing for the class probably really wants
                // to know about the typo.
            return null;
        }
        catch (SecurityException e) {
                // Netscape 4.x Browser VM throws a
                // netscape.security.AppletSecurityException which
                // extends java.lang.SecurityException. This does not
                // conform to the Java API for Class.forName()
            return null;
        }
    }

    /***************************************************
        AWClassLoader implementation
    ***************************************************/


    /*
    public Class checkReloadClass (Class cls)
    {
        AWResource source = javaSourceFileResource(cls);
        if (source == null) return super.checkReloadClass(cls);

        String className = getComponentNameForClass(cls.getName());
        File classFile = getClassFile(className);
        if (classFile != null && classFile.exists() &&
            source.lastModified() > classFile.lastModified()) {
        }

        try {
            cls = getClass(className);
        }
        catch (ClassNotFoundException cnfe) {
            //swallow
        }
        return cls;
    }
    */
    public void checkForUpdates ()
    {
        List<LoadInfo>updated = findUpdatedSourceFiles();
        if (updated != null) {
            reloadSourceFiles(updated);
        }
        reloadModifiedClasses();
    }

    protected Class loadClassFromResource (String className, AWResource sourceResource)
    {
        compileSourceFromResource(className, sourceResource);
        reloadModifiedClasses();
        try {
            return getClass(Factor.getOriginalClassName(className));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public String getComponentNameForClass (String className)
    {
        // Todo: doesn't handle chained loaders correctly (can't tell if this name is ours...)
        return Factor.getOriginalClassName(className);
    }


    protected String sourceFileExtension()
    {
        return ".java";
    }

    protected void compileSourceFromResource (String className, AWResource sourceResource)
    {
        Compiler.compile(sourceResource);

        // in case we haven't seen a class in this build directory before, find it now
        File classFile = getClassFile(className);
        if (classFile != null) {
            noteClassDirectory(classFile.getParentFile());
        }
        else {
            Assert.assertNonFatal(false, "Compilation of class: %s did not result in class file -- " +
                    "perhaps class has mismatched 'package' declaration?", className);
            recordClassForResource(className, null, sourceResource);
        }
    }

    protected void reloadModifiedClasses ()
    {
        List<File> classFiles = classFilesModifiedSince(_LastClassCheck);
        _LastClassCheck = System.currentTimeMillis();

        List<HotSwapFactory.ClassSpec> classSpecs = new ArrayList();
        List<String> classList = new ArrayList();

        for (File classFile : classFiles) {
            String name = classFileToName(classFile);
            Console.println("Changed file: " + name);
            Class orig;
            try {
                orig = forName(name);
            } catch (java.lang.UnsatisfiedLinkError ex) {
                Assert.assertNonFatal(false, "Exception looking up class: %s", ex);
                continue;
            }

            File f = getClassFile(name);
            Assert.that((f != null && f.exists()), "Can't find file: %s", f);
            byte[] bytecode = getFileBytes(f);
            Assert.assertNonFatal((bytecode!=null), "Can't find bytecode for updated class: %s", orig);

            if (orig == null) {
                // we may have compiled a class into the build root, but the build root
                // could be outside the class path

                // if file is a modified version, don't try again here
                if (!name.equals(Factor.getOriginalClassName(name))) continue;
                
                // FIXME: we should find another class in this ones package to use
                // for the protection domain!
                ProtectionDomain domain = AWConcreteApplication.sharedInstance().getClass().getProtectionDomain();

                Class cls = CompiledAccessorFactory.ByteArrayClassLoader.loadClass(
                        name, bytecode, domain);
                recordClassForResource(name, cls, sourceResourceForClassName(name));
            } else {
                // reload existing class.  Need to use true orig (minus rename) so it matches new name
                Class cls = _classForName(name);
                if (cls == null || (cls.getName().equals(orig.getName()))) cls = orig;
                HotSwapFactory.ClassSpec spec = new HotSwapFactory.ClassSpec(cls, bytecode, f);
                classSpecs.add(spec);
                classList.add(orig.getName());
            }
        }

        if (classSpecs != null) {
            // sort list by timestamp
            sortList(classSpecs, true, new Compare() {
                public int compare(Object o1, Object o2) {
                    return (int)(((HotSwapFactory.ClassSpec)o1)._file.lastModified()
                                - ((HotSwapFactory.ClassSpec)o2)._file.lastModified());
                }
            });

            // hotswap
            HotSwapFactory.hotswap(classSpecs);

            // let others know we did a reload
            notifyReload(classList);
        }
    }

    protected void noteClass (Class cls)
    {
        File classFile = getClassFile(cls.getName());
        if (classFile != null) {
            noteClassDirectory(classFile.getParentFile());
        }
    }

    static class WatchedDir
    {
        File _dir;
        File[] _files;
        long _lastCheck;
        FilenameFilter _filter;

        WatchedDir(File dir, FilenameFilter filter)
        {
            _dir = dir;
            _filter = filter;
        }

        File[] files()
        {
            if ((_dir.lastModified() > _lastCheck || _files == null)) {
                _files = _dir.exists() ? _dir.listFiles(_filter) : new File[] {};

                _lastCheck = System.currentTimeMillis();
            }

            return _files;
        }

        void addFilesModifiedSince (long time, List<File> list)
        {
            for (File f: files()) {
                if (f.lastModified() > time) list.add(f);
            }
        }
    }

    static FilenameFilter ClassFileFilter = new FilenameFilter() {
            public boolean accept(File file, String s)
            {
                return s.endsWith(".class");
            }
        };

    static FilenameFilter DirectoryFilter = new FilenameFilter() {
            public boolean accept(File file, String s)
            {
                return file.isDirectory();
            }
        };

    GrowOnlyHashtable _ClassDirsByPath = new GrowOnlyHashtable();
    protected void noteClassDirectory (File dir)
    {
        WatchedDir classDir = (WatchedDir)_ClassDirsByPath.get(dir.getPath());
        if (classDir != null) return;
        classDir = new WatchedDir(dir, ClassFileFilter);
        _ClassDirsByPath.put(dir.getPath(), classDir);
    }

    protected List<File>classFilesModifiedSince (long time)
    {
        List<File> list = new ArrayList();
        for (Object v : _ClassDirsByPath.values()) {
            ((WatchedDir)v).addFilesModifiedSince(time, list);
        }
        return list;
    }


    private byte[] getFileBytes (File file)
    {
        int length = (int)file.length();
        byte[] byteArray = new byte[length];
        try {
            FileInputStream istream = new FileInputStream(file);
            istream.read(byteArray);
            istream.close();
        }
        catch (IOException ioe) {
            throw new AWGenericException("Error generating byte array for file: " + ioe);
        }
        return byteArray;
    }

    /*
        Return the File containing this class, or null if it cannot be found.
    */
    public File getClassFile (String className)
    {
        String relativePath = className.replace('.','/') + ".class";
        for (File dir : getClassRoots()) {
            if (dir.isDirectory()) {
                File file = new File(dir, relativePath);
                if (file.exists()) return file;
            }
        }
        return null;
    }

    public File getClassRootForClass (String className)
    {
        String relativePath = className.replace('.','/') + ".class";
        for (File dir : getClassRoots()) {
            if (dir.isDirectory()) {
                File file = new File(dir, relativePath);
                if (file.exists()) return dir;
            }
        }
        return null;
    }

    /** Like Unix touch **/
    public static void touch (File file)
    {
        try
        {
            // OutputStream out = new java.io.FileOutputStream(file);
            // out.close();
            file.delete();
            file.createNewFile();
        }
        catch( IOException ioe )
        {
        }
    }

    protected AWResource javaSourceFileResource (Class cls)
    {
        AWResource resource = null;
        AWSingleLocaleResourceManager resourceManager =
            AWConcreteApplication.SharedInstance.resourceManager(Locale.US);
        String origComponentName = AWUtil.getClassLoader().getComponentNameForClass(cls.getName());
        String origClassName = origComponentName.replace('.','/');
        String sourceFileName = Fmt.S("%s.java",origClassName);

        resource = resourceManager.resourceNamed(sourceFileName);
        if (resource == null) {
            //
        }
        return resource;
    }

    protected String classFileToName (File classFile)
    {
        try {
            String classFilePath = classFile.getCanonicalPath();
            for (File d : getClassRoots()) {
                String rootPath = d.getCanonicalPath();
                if (classFilePath.startsWith(rootPath)) {
                    String name = classFilePath.substring(rootPath.length()).replace('\\','.').replace('/','.');
                    name = name.substring(0,(name.length()-".class".length()));
                    if (name.startsWith(".")) name = name.substring(1);
                    return name;
                }
            }
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
        return null;
    }

    /*
    protected synchronized List updateModifiedClasses ()
    {
        Console.println("--- Scanning for changed files... ");
        List list = null;
        List<String> classList = null;
        try {
            String[] cmdArray;

            if (SystemUtil.isWin32()) {
                cmdArray = new String[] { "cmd", "/c", "find", BuildRoot, "-newer", TimestampPath, "-name", "*.class", "-print" };
            }
            else {
                cmdArray = new String[] { "find", BuildRoot, "-newer", TimestampPath, "-name", "*.class", "-print" };
            }
            Process process = Runtime.getRuntime().exec(cmdArray);
            InputStream input = process.getInputStream();
            String line = IOUtil.readLine(input);
            while (line != null) {
                String name = classFileToName(new File(line));
                Console.println("Changed file: " + name);
                Class orig;
                try {
                    orig = _classForName(name);
                } catch (java.lang.UnsatisfiedLinkError ex) {
                    Assert.assertNonFatal(false, "Exception looking up class: %s", ex);
                    continue;
                }
                if (orig != null) {
                    File f = getClassFile(orig.getName());
                    Assert.that((f != null && f.exists()), "Can't find file: %s", f);
                    byte[] bytecode = getFileBytes(f);
                    Assert.assertNonFatal((bytecode!=null), "Can't find bytecode for updated class: %s", orig);

                    if (list == null ) {
                        list = ListUtil.list();
                        classList = ListUtil.list();
                    }

                    // Todo: no resource (null)
                    HotSwapFactory.ClassSpec spec = new HotSwapFactory.ClassSpec(orig, bytecode, f);
                    list.add(spec);
                    classList.add(orig.getName());
                } else {
                    Assert.assertNonFatal(false, "Failed to lookup changed class: %s", name);
                }
                line = IOUtil.readLine(input);
            }
        }
        catch (IOException e) {
            Log.util.debug("Unexpected IOException : %s", e);
        }
        touch(new File(TimestampPath));

        if (list != null) {
            // sort list by timestamp
            sortList(list, true, new Compare() {
                public int compare(Object o1, Object o2) {
                    return (int)(((HotSwapFactory.ClassSpec)o1)._file.lastModified()
                                - ((HotSwapFactory.ClassSpec)o2)._file.lastModified());
                }
            });

            // hotswap
            HotSwapFactory.hotswap(list);

            // let others know we did a reload
            notifyReload(classList);
        }
        return list;
    }
    */
    public static void sortList (List l, boolean ascending, Compare comparator)
    {
        Object s[] = new Object[l.size()];
        l.toArray(s);

        Sort.objects(s, null, null, 0, l.size(), comparator,
                     ascending ? Sort.SortAscending : Sort.SortDescending);
        l.clear();
        l.addAll(Arrays.asList(s));
    }


    private static boolean _Reloading = false;

    public void loadSucceeded (HotSwapFactory.ClassSpec spec)
    {
        Console.println("Hot swapped class: " + spec._cls.getName() + " -- " + spec._bytecode.length + " bytes");
        recordClassForResource(spec._cls.getName(), spec._cls, sourceResourceForClassName(spec._cls.getName()));
    }

    public void loadFailed (HotSwapFactory.ClassSpec spec)
    {
        Class origClass = spec._cls;
        if (_Reloading) {
            // don't recurse
            Console.println("Failed on reload of rewitten class: " + origClass.getName());
            return;
        }

        try {
            _Reloading = true;

            if (AWComponent.class.isAssignableFrom(origClass)) {
                // try to rename the class and reload
                Console.println("Your changes to AWComponent class (" + origClass.getName()
                         + ") were not directly reloadable.  Attempting to reload renamed version... ");
                String className = origClass.getName();
                File rootDir = getClassRootForClass(className);
                Class cls = Factor.renameAndReload(origClass, className, spec._bytecode, rootDir.toString());
                if (cls != null) {
                    recordClassForResource(className, cls, sourceResourceForClassName(className));
                } else {
                    Console.println("Unable to load renamed version.");
                }
            } else {
                Console.println("***  HotSwap failed for " + origClass.getName() + " ***");
                Console.println(" --  NOTE: adding/removing instance variables and methods is only supported for "
                        + "*non-page-level* AWComponents");
            }
        } finally {
            _Reloading = false;
        }
    }
}
