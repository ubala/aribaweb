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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/SafeJavaRepository.java#20 $
*/

package ariba.util.fieldtype;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.SetUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.i18n.I18NUtil;
import ariba.util.io.CSVConsumer;
import ariba.util.io.CSVReader;
import ariba.util.log.Log;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
    A singleton class that acts as a repository for storing information
    about Java safeness for Expr-based expression.

    @aribaapi ariba
*/
public class SafeJavaRepository
{
    private static final String SafeJavaCSVFilename = "SafeJava.csv";
    private static final File SafeJavaCSVRoot = new File("etc/safejava");
    private static final File SafeJavaCSVInternalRoot = new File("internal/etc/safejava");
    private static final FilenameFilter SafeJavaCSVFilter = 
        new FilenameFilter ()
        {
            public boolean accept (File dir, String name)
            {
                return !StringUtil.nullOrEmptyOrBlankString(name) &&
                       SafeJavaCSVFilename.equals(name);
            }
        };
    
    private static SafeJavaRepository INSTANCE = null;

    public static SafeJavaRepository getInstance ()
    {
        return getInstance(true);
    }

    public static SafeJavaRepository getInstance (boolean initialized)
    {
        if (INSTANCE == null) {
            synchronized (SafeJavaRepository.class) {
                if (INSTANCE == null) {
                    SafeJavaRepository temp = new SafeJavaRepository(initialized);
                    INSTANCE = temp;
                }
            }
        }
        return INSTANCE;
    }

    //-----------------------------------------------------------------------
    // nested class
    
    /**
        A class that knows how to read CSV into Java class name to
        method specification table.

        @aribaapi private
    */
    private static class CSVSafeJavaTableConsumer implements CSVConsumer
    {
        private static final List/*<String>*/ PrimitiveTypeConstants =
            Collections.unmodifiableList(
                    ListUtil.list(
                            Constants.BooleanPrimitiveType,
                            Constants.IntPrimitiveType,
                            Constants.DoublePrimitiveType,
                            Constants.LongPrimitiveType));

        private static final List/*<Class>*/ PrimitiveTypes =
            Collections.unmodifiableList(
                    ListUtil.list(
                            Boolean.TYPE,
                            Integer.TYPE,
                            Double.TYPE,
                            Long.TYPE));

        private Map/*<String, MethodSpecification>*/ _name2Spec;

        public CSVSafeJavaTableConsumer ()
        {
            _name2Spec = MapUtil.map();
        }
        
        public void consumeLineOfTokens (String path, int lineNumber, List line)
        {
            // check to see if we have at least Java class name and method name
            // specified for the current line
            if (line.size() < 2) {
                String msg = Fmt.S(
                    "%s:%s requires at least 2 columns but it " +
                    "has %s columns: %s",
                    path,
                    Constants.getInteger(lineNumber),
                    Constants.getInteger(line.size()),
                    line);
                    Log.util.warn(msg);
                return;
            }
            
            // check to see if the specified Java class name is valid
            Iterator iter = line.iterator();
            String className = (String)iter.next();
            Class javaClass = getClassForName(path, className);
            if (javaClass == null) {
                String msg = Fmt.S("%s:%s contains undefined java class: %s",
                                   path, Constants.getInteger(lineNumber), className);
                Log.util.warn(msg);
                return;
            }
            /*
               check to see if the specified Java method name is a wildcard
               character.  If so, put an AllMethodSpecification (and overwrite
               existing value if there is any) to indicate
               all methods defined in the specified Java class name are safe.
            */
            String methodName = (String)iter.next();
            if ("*".equals(methodName)) {
                _name2Spec.put(javaClass.getName(),
                               new AllMethodSpecification(javaClass));
                return;
            }
            // process parameter types if they are specified
            List/*<Class>*/ javaParameterTypes = ListUtil.list();
            while (iter.hasNext()) {
                String typeName = (String)iter.next();
                // check to see if the specified parameter type is null or empty string
                if (StringUtil.nullOrEmptyOrBlankString(typeName)) {
                    String msg = Fmt.S(
                        "%s:%s contains null or empty parameter type: %s " +
                        "(class: %s, method: %s)",
                        path, Constants.getInteger(lineNumber), typeName, 
                        javaClass.getName(), methodName);
                    Log.util.warn(msg);
                    return;
                }
                Class parameterTypeClass = isPrimitive(typeName)?
                    (Class)PrimitiveTypes.get(getPrimitiveTypesIndex(typeName)):
                        getClassForName(path, typeName);
                // check to see if the specified parameter type is valid
                if (parameterTypeClass == null) {
                    String msg = Fmt.S(
                        "%s:%s contains undefined parameter type: %s " +
                        "(class: %s, method: %s)",
                        path, Constants.getInteger(lineNumber), typeName, 
                        javaClass.getName(), methodName);
                    Log.util.warn(msg);
                    return;
                }
                javaParameterTypes.add(parameterTypeClass);
            }
            // check to see if any parameter type is specified.  If not, all the
            // overloading methods with the same Java method name are safe
            if (javaParameterTypes.isEmpty()) {
                // ToDo: 01.22.2007 - not sure if we need to filter out all
                // non-public methods
                Method[] methods = javaClass.getDeclaredMethods();
                List collection = ListUtil.list();
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    if (method.getName().equals(methodName)) {
                        collection.add(method);
                    }
                }
                addToName2Spec(javaClass.getName(), collection);
                return;
            }
            // check to see if the specified method name is valid
            Class[] javaParameterTypesAsArray = new Class[javaParameterTypes.size()];
            javaParameterTypes.toArray(javaParameterTypesAsArray);
            try {
                Method javaMethod = javaClass.getDeclaredMethod(
                    methodName,
                    javaParameterTypesAsArray);
                addToName2Spec(javaClass.getName(), javaMethod);
            }
            catch (NoSuchMethodException ex) {
                String msg = Fmt.S(
                    "%s:%s contains undefined java method " +
                    "(class: %s, method: %s, parameter type(s): %s)",
                    path, Constants.getInteger(lineNumber), 
                    javaClass.getName(), methodName, javaParameterTypes);
                Log.util.warn(msg);
                return;
            }
        }

        /*
            Returns the Java class name to method specifications table
        */
        private Map/*<String, MethodSpecification>*/ getName2Spec ()
        {
            return _name2Spec;
        }
        
        private Class getClassForName (String path, String name)
        {
            String className = name;
            // ToDo: 04/01/2008: it seems like we only support short name
            // for classes in the same package/path of the current
            // SafeJava.csv
            if (isShortName(name)) {
                path = stripSafeJavaRootFromPath(path);
                path = stripSafeJavaFilenameFromPath(path);
                String packageName = path.replace(File.separatorChar, '.');
                className = StringUtil.strcat(packageName, ".", name);
            }
            return ClassUtil.classForName(className, false);
        }
        
        private boolean isShortName (String name)
        {
            return !StringUtil.nullOrEmptyOrBlankString(name) && name.indexOf('.') < 0;   
        }
        
        private String stripSafeJavaRootFromPath (String path)
        {
            String result = path;
            Set<File> safeJavaCSVRoots = SetUtil.set(2);
            safeJavaCSVRoots.add(SafeJavaCSVRoot);
            safeJavaCSVRoots.add(SafeJavaCSVInternalRoot);
            for (File safeJavaRoot : safeJavaCSVRoots) {
                String safeJavaRootName = safeJavaRoot.getName();
                int pos = result.indexOf(safeJavaRootName);
                if (pos > 0) {
                    pos += safeJavaRootName.length() + File.separator.length();
                    result = result.substring(pos);
                    break;
                }               
            }
            return result;
        }
        
        private String stripSafeJavaFilenameFromPath (String path)
        {
            int pos = path.lastIndexOf(SafeJavaCSVFilename);
            if (pos > 0) {
                pos -= File.separator.length();
                return path.substring(0, pos);
            }
            return path;        
        }
        
        /*
            Populates the Java class name to method specification table.

            If the table contains a mapping for the specified Java class name,
            the list of methods that are not already in there would be added to 
            the existing method specification (if the existing method specification
            is an instance of <code>AllMethodSpecification</code>, it would be a no-op).

            If the table does not contain a mapping for the specified Java
            class name already, a new <code>SimpleMethodSpecification</code> would be
            created.

            @param className the name of the Java class
            @param methods the list of <code>Method</code> for which method
            specification is created
        */
        private void addToName2Spec (String className, Collection/*<Method>*/ methods)
        {
            if (_name2Spec.containsKey(className)) {
                MethodSpecification spec = (MethodSpecification)_name2Spec.get(className);
                if (!(spec instanceof AllMethodSpecification)) {
                    ((SimpleMethodSpecification)spec).addAllIfAbsent(methods);
                }
                    // if the existing method spec is an instance of AllMethodSpecification,
                    // it would be a no-op
            }
            else {
                SimpleMethodSpecification spec = new SimpleMethodSpecification();
                spec.addAll(methods);
                _name2Spec.put(className, spec);
            }
        }

        /*
            Populates the Java class name to method specification table.

            If the table contains a mapping for the specified Java class name,
            the method would be added to the existing method specification if
            it is not already there. (if the existing method specification is 
            an instance of <code>AllMethodSpecification</code>, it would be a no-op).

            If the table does not contain a mapping for the specified Java
            class name already, a new <code>SimpleMethodSpecification</code> would be
            created.

            @param className the name of the Java class
            @param method <code>Method</code> for which method
            specification is created
        */
        private void addToName2Spec (String className, Method method)
        {
            if (_name2Spec.containsKey(className)) {
                MethodSpecification spec = (MethodSpecification)_name2Spec.get(className);
                if (!(spec instanceof AllMethodSpecification)) {
                    ((SimpleMethodSpecification)spec).addIfAbsent(method);
                }
            }
            else {
                SimpleMethodSpecification spec = new SimpleMethodSpecification();
                spec.add(method);
                _name2Spec.put(className, spec);
            }           
        }

        private boolean isPrimitive (String type)
        {
            return PrimitiveTypeConstants.contains(type);
        }

        private int getPrimitiveTypesIndex (String type)
        {
            return PrimitiveTypeConstants.indexOf(type);
        }
    }

    //-----------------------------------------------------------------------
    // private data members

    // _name2Specification should not contain
    // {@link MethodSpecification#AlwaysFalseMethodSpecification} and
    // {@link MethodSpecification#CompositeMethodSpecification} since they
    // are cons up on the fly during runtime.

    private GrowOnlyHashtable/*<String, MethodSpecification>*/ _name2Specification;

    //-----------------------------------------------------------------------
    // constructor

    /*
        Suppresses default constructor for noninstantiability
    */
    private SafeJavaRepository (boolean initialize)
    {
        _name2Specification = new GrowOnlyHashtable();
        if (initialize) {
            fullInitialize();
        }
    }

    //-----------------------------------------------------------------------
    // public methods

    /**
     * This method re-initialize the safe java method repository from the
     * given <code>file</code>.  The file can be a directory or a CSV file.
     * If it is a directory, then it will look up SafeJava.csv files located
     * under this directory.
     * @param files - list of files. The file can be directory.
     */
    public void registerSafeJavaFile (List <File> files)
    {
        Assert.that(!ListUtil.nullOrEmptyList(files), "No safe java file specified.");
        resetState();

        for (int i=0; i < files.size(); i++) {
            File file = files.get(i);
            try {
                if (file.isDirectory()) {
                    Set rootDirs = SetUtil.set();
                    rootDirs.add(file.getCanonicalPath());
                    load(rootDirs);
                }
                else {
                    load(file);
                }
            }
            catch (IOException ex) {
                String msg = Fmt.S("file %s cannot be read", file.getPath());
                Log.util.warn(msg);
            }
        }
    }
        
    /**
        Gets the method specification for the specified <code>Class</code>

        @param aClass the <code>Class</code> for which <MethodSpecification>
        is obtained
        @return the method specification
    */
    public MethodSpecification getAllSafeMethodsForClass (Class aClass)
    {
        List<MethodSpecification> collection  = ListUtil.list();
        collectAllSafeMethodsForClass(aClass, collection);
        return new CompositeMethodSpecification(collection);
    }

    /**
     * This method returns all the class names in the safe java files.
     * Caller must not modify the return value.
     * @return A set of safe class names used in the safe java files.
     */
    public Set getAllSafeClassNames ()
    {
        return Collections.unmodifiableSet(_name2Specification.keySet());
    }

    /**
     * This method should only be used for testing ONLY.
     * @param file
     * @aribaapi private
     */
    public Map switchSafeJavaRepository (File file)
    {
        Map existing = _name2Specification;
        registerSafeJavaFile(ListUtil.list(file));
        return existing;
    }

    /**
     * This method should only be used for testing ONLY.
     * @aribaapi private
     */
    public void restoreSafeJavaRepository (Map repository)
    {
        _name2Specification = (GrowOnlyHashtable)repository;
    }
    
    //-----------------------------------------------------------------------
    // private methods

    /**
     * Initialize the default safe Java CSV Roots
     */
    private void fullInitialize ()
    {
        Set/*<File>*/ safeJavaCSVRoots = SetUtil.set();
        safeJavaCSVRoots.add(SafeJavaCSVRoot);
        if (!skipLoadingFromInternalRoot()) {
            safeJavaCSVRoots.add(SafeJavaCSVInternalRoot);
        }
        
        resetState();
        load(safeJavaCSVRoots);
    }

    /**
        Initializes the repository given a set of root directories.
    */
    private void load (Set safeJavaCSVRoots)
    {
        Set/*<File>*/ safeJavaCSVs = SetUtil.set();
        for (Iterator iter = safeJavaCSVRoots.iterator(); iter.hasNext();) {
            File safeJavaCSVRoot = (File)iter.next();
            safeJavaCSVs.addAll(listFilesRecursively(safeJavaCSVRoot, SafeJavaCSVFilter));
        }
        for (Iterator iter = safeJavaCSVs.iterator(); iter.hasNext();) {
            readSafeJavaCSV((File)iter.next());
        }
    }

   /**
        Initializes the repository from a single file.
    */
    private void load (File file)
    {
        readSafeJavaCSV(file);
    }

    /**
     * Reset the repository
     */
    private void resetState ()
    {
        _name2Specification = new GrowOnlyHashtable();
    }

    /**
        Returns <code>true</code> if it should skip loading any SafeJava.csv
        in the internal directory, or <code>false</code> otherwise.  It has
        no effect on file registered explicitly thru
        {@link #registerSafeJavaFile(java.util.List)}.
     */
    private static boolean skipLoadingFromInternalRoot ()
    {
        // currently, we load from internal root only if school is installed
        boolean isSchoolInstalled = false;
        File aribaRoot = SystemUtil.getSystemDirectory();
        if (aribaRoot != null && aribaRoot.isDirectory()) {
            File variantsRoot = new File(aribaRoot, "variants");
            if (variantsRoot != null && variantsRoot.isDirectory()) {
                String[] contents = variantsRoot.list();
                isSchoolInstalled = contents != null &&
                        ArrayUtil.contains(contents, "PlainCornell");
            }
        }
        return !isSchoolInstalled;
    }

    private List/*<File>*/ listFilesRecursively (File directory, FilenameFilter filter)
    {
        List/*<File>*/ result = ListUtil.list();
        File[] entries = directory.listFiles();
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
                File entry = entries[i];
                if (filter == null || filter.accept(directory, entry.getName())) {
                    result.add(entry);
                }
                if (entry.isDirectory()) {
                    result.addAll(listFilesRecursively(entry, filter));
                }
            }
        }
        return result;
    }
    
    private void readSafeJavaCSV (File safeJavaCSV)
    {
        if (safeJavaCSV.exists()) {
            CSVSafeJavaTableConsumer consumer = new CSVSafeJavaTableConsumer();
            CSVReader reader = new CSVReader(consumer);
            try {
                reader.read(safeJavaCSV, I18NUtil.EncodingUTF8);
                merge(consumer.getName2Spec(), _name2Specification);
            }
            catch (IOException ex) {
                String msg = Fmt.S("file %s cannot be read", safeJavaCSV.getPath());
                Log.util.warn(msg);
            }
        }       
    }
    
    private void collectAllSafeMethodsForClass (Class aClass, List<MethodSpecification> collector)
    {
        MethodSpecification methodSpec = getSafeMethodsForClass(aClass);
        if (methodSpec != null) {
            ListUtil.addElementIfAbsent(collector, methodSpec);
        }
        if (aClass.getSuperclass() != null) {
            collectAllSafeMethodsForClass(aClass.getSuperclass(), collector);
        }
        Class[] interfaces = aClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            collectAllSafeMethodsForClass(interfaces[i], collector);
        }
    }
    
    private MethodSpecification getSafeMethodsForClass (Class aClass)
    {
        String className = aClass.getName();
        // if we could not find the specification, we return an instance  
        // of AlwaysFalseMethodSpecification.
        if (!_name2Specification.containsKey(className)) {
            return MethodSpecification.AlwaysFalseMethodSpecification;
        }
        return (MethodSpecification)_name2Specification.get(className);
    }
    
    private void merge (
            Map/*<String, MethodSpecification>*/ source, 
            Map/*<String, MethodSpecification>*/ dest)
    {
        for (Iterator keyIter = source.keySet().iterator(); keyIter.hasNext();) {
            String className = (String)keyIter.next();
            if (!dest.containsKey(className)) {
                // if we have not had it already, just add it
                dest.put(className, source.get(className)); 
            }
            else {
                /*
                    if we have had it already, we do the following
                    1) dest entry: AllMethodSpecification
                       source entry: SimpleMethodSpecification
                       result: no-op
                    2) dest entry: AllMethodSpecification
                       source entry: AllMethodSpecification
                       result: no-op   
                    3) dest entry: SimpleMethodSpecification
                       source entry: AllMethodSpecification
                       result: simply replace the dest spec with the source spec
                    4) dest entry: SimpleMethodSpecification
                       source entry: SimpleMethodSpecification
                       result: add all the methods in source spec to desc spec (if absent)
                */
                MethodSpecification destSpec =
                    (MethodSpecification)dest.get(className);
                MethodSpecification sourceSpec =
                    (MethodSpecification)source.get(className);
                if (destSpec instanceof AllMethodSpecification) {
                    // no-op
                    continue;
                }
                else { 
                    if (sourceSpec instanceof AllMethodSpecification) {
                        // meaning destSpec is SimpleMethodSpecification 
                        // and sourceSpec is AllMethodSpecification
                        dest.put(className, sourceSpec);
                    }
                    else {
                        // meaning destSpec is SimpleMethodSpecification
                        // and sourceSpec is SimpleMethodSpecification
                        SimpleMethodSpecification simpleSourceSpec =
                            (SimpleMethodSpecification)sourceSpec;
                        SimpleMethodSpecification simpleDestSpec =
                            (SimpleMethodSpecification)destSpec;
                        simpleDestSpec.addAllIfAbsent(simpleSourceSpec.getMethods());
                    }
                }
            }
        }
    }
}
