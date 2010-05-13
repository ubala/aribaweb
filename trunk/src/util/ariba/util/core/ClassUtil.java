/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/ClassUtil.java#23 $
*/

package ariba.util.core;

import java.util.Map;
import ariba.util.log.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
    ClassUtil. A set of helpers for dealing with java classes.
    @aribaapi documented
*/
public final class ClassUtil
{
    public static String NativeInteger = "int";
    public static String NativeBoolean = "boolean";
    public static String NativeDouble = "double";
    public static String NativeFloat = "float";
    public static String NativeLong = "long";
    public static String NativeByte = "byte";
    public static String NativeShort = "short";
    public static String NativeChar = "char";

    /* prevent people from creating this class */
    private ClassUtil ()
    {
    }
    
    /**
        Make sure a class' static inits have run. This method loads
        the specified class to make sure that it's static methods have
        run.  If the class is not found, no warning will be printed.

        @param name the name of the class to load.
        @aribaapi documented
       
    */
    public static void classTouch (String name)
    {
       classForName(name, Object.class, false);
    }

    public static Class classForNativeType (String typeName)
    {
        if (NativeInteger.equals(typeName)) {
            return Integer.TYPE;
        }
        if (NativeBoolean.equals(typeName)) {
            return Boolean.TYPE;
        }
        if (NativeDouble.equals(typeName)) {
            return Double.TYPE;
        }
        if (NativeFloat.equals(typeName)) {
            return Float.TYPE;
        }
        if (NativeLong.equals(typeName)) {
            return Long.TYPE;
        }
        if (NativeByte.equals(typeName)) {
            return Byte.TYPE;
        }
        if (NativeShort.equals(typeName)) {
            return Short.TYPE;
        }
        if (NativeChar.equals(typeName)) {
            return Character.TYPE;
        }
        return null;            
    }

    /**
        Find a Class for the specified class name. A warning will be
        printed if the class was not found.

        @param className the name of the class to find

        @return the Class for the given <B>className</B>, or null if
        the Class doesn't exist.

        @see #newInstance
        @aribaapi documented
        
    */
    public static Class classForName (String className)
    {
        return classForName(className, Object.class, true);
    }

    /**
        Find a Class for the specified class name. A warning will be
        printed if the class was not found.

        @param className the name of the class to find
        @param supposedSuperclass The class of the required
        superclass for the class specified by className

        @return the Class for the given <B>className</B>, or null if
        the Class doesn't exist.

        @see #newInstance
        @aribaapi documented
        
    */
    public static Class classForName (String className,
                                      Class  supposedSuperclass)
    {
        return classForName(className, supposedSuperclass, true);
    }

    private static final GrowOnlyHashtable ClassForNameCache =
        new GrowOnlyHashtable();

    private static final Object NoCachedClassFound = Constants.NullObject;

    private static final GrowOnlyHashtable LocaleCache =
        new GrowOnlyHashtable();

    private static ClassFactory classFactory = null;

    /**
        @aribaapi ariba

        set the ClassFactory to be used by classForName and
        classTouch. This is for development mode only, it is *not*
        supported for production.
    */
    public static ClassFactory setClassFactory (ClassFactory cf)
    {
        ClassFactory oldcf = classFactory;
        classFactory = cf;
        return oldcf;
    }
    
    /**
        @aribaapi ariba

        get the ClassFactory to be used by classForName and
        classTouch. This is for development mode only, it is *not*
        supported for production.
    */
    public static ClassFactory getClassFactory ()
    {
        return classFactory;
    }
    
    /**
        Find a Class for the specified class name. Prints a warning
        message if the class can't be found and <B>warning</B> is
        true.

        @param className the name of the class to find
        @param warning if <b>true</b> and the class can not be found,
        a warning will be printed

        @return the Class for the given <B>className</B>, or null if
        the Class doesn't exist.

        @see #newInstance
        @aribaapi documented
    */
    public static Class classForName (String className,
                                      boolean warning)
    {
        return classForName(className, Object.class, warning);
    }

    /**
        Find a Class for the specified class name. Prints a warning
        message if the class can't be found and <B>warning</B> is
        true.

        @param className the name of the class to find
        @param supposedSuperclass The required superclass for the class
        @param warning if <b>true</b> and the class can not be found,
        or it is not assignable to the supposedSuperclass, a warning
        will be printed

        @return the Class for the given <B>className</B>, or null if
        the Class doesn't exist.

        @see #newInstance
        @aribaapi documented
    */
    public static Class classForName (String className,
                                      Class supposedSuperclass,
                                      boolean warning)
    {
        if (classFactory != null) {
            return classFactory.forName(className);
        }
        
        if (className == null) {
            return null;
        }

        if (useContextClassLoader) {
            return classForNameUsingContextClassLoader(className,
                                                       supposedSuperclass,
                                                       warning);
        }

        // Check for Generics as they are irrelevant and will cause null to be returned.
        // So we turn "java.util.List<String>" into "java.util.List"
        int leftGenericIndex = className.indexOf('<');
        if (leftGenericIndex > 0) {
            if (className.charAt(className.length() - 1) == '>') {
                className = className.substring(0, leftGenericIndex);
            }
            else {
                Log.util.debug("Getting malformed Generics className: %s", className);
            }
        }

        Object cachedClass = ClassForNameCache.get(className);
        if (cachedClass == NoCachedClassFound) {
            if (warning) {
                Log.util.error(2764, className);
            }
            return null;
        }
        if (cachedClass != null) {
                // any cached value other than noCachedClassFound is a
                // class that can be returned
            return checkInstanceOf((Class)cachedClass,
                                   supposedSuperclass,
                                   warning);
        }

            // if it was not found in the cache, check for the real
            // class object
        try {
            Class classObj = Class.forName(className); // OK
                // save the value
            ClassForNameCache.put(className, classObj);
            return checkInstanceOf(classObj, supposedSuperclass, warning);
        }
        catch (ClassNotFoundException e) {
                // if there was an exception store that object was not
                // found and call again. (This lets there be a single path
                // for the log error message.)
            if (Log.util.isDebugEnabled()) {
                Log.util.debug("classForName: %s", SystemUtil.stackTrace(e));
            }
            ClassForNameCache.put(className, NoCachedClassFound);
            return classForName(className, warning);
        }
        catch (NoClassDefFoundError e) {
                // Supposed you as for class foo.bar.Bazqux, and on NT
                // it finds the class file foo/bar/BazQux.class. On
                // JDK11, this was just a ClassNotFoundException. On
                // JDK12, this throws a NoClassDefFoundError. We
                // always report the error in this case, because
                // someone probing for the class probably really wants
                // to know about the typo.
            if (Log.util.isDebugEnabled()) {
                Log.util.debug("classForName: %s", SystemUtil.stackTrace(e));
            }
            ClassForNameCache.put(className, NoCachedClassFound);
            return classForName(className, warning);
        }
        catch (SecurityException e) {
                // Netscape 4.x Browser VM throws a 
                // netscape.security.AppletSecurityException which
                // extends java.lang.SecurityException. This does not
                // conform to the Java API for Class.forName()
            if (Log.util.isDebugEnabled()) {
                Log.util.debug("classForName: %s", SystemUtil.stackTrace(e));
            }
            ClassForNameCache.put(className, NoCachedClassFound);
            return classForName(className, warning);
        }    
    }
    
    /**
        Find a Class for the specified class name. Throws an exception
        if the class is not found.
        <p>
        The java spec does not define behavior with a null
        className. If a null is passed in a ClassNotFoundException
        will be thrown.
        
        @param className the name of the class to find

        @return the Class for the given <B>className</B>

        @exception ClassNotFoundException if the class can not be
        found.

        @see #newInstance
        @aribaapi documented
    */
    public static Class classForNameWithException (String className)
      throws ClassNotFoundException
    {
        Class returnVal = classForName(className, Object.class, false);
        if (returnVal == null) {
            throw new ClassNotFoundException(Fmt.S("Could not find class %s",
                                                   className));
        }
        return returnVal;
    }

    /**
        Find a Class for the specified class name. Throws an exception
        if the class is not found.
        <p>
        Each call to classForNameNonCaching reloads the byte codes into
        the VM.  This is very useful debugging programs that have long 
        start-up times because classes loaded with classForNameNonCaching 
        can be recompiled and reloaded into the VM without restarting.
        <p>
        The pattern parameter allows you to extend the dynamic type of 
        class loading to other classes that are instantiated by the
        class identified in className.  If you provide null for this
        parameter, then the only class that gets dynamically reloaded
        is the className class.  Any classes it instantiates will
        be based on byte codes cached in the VM.  Note the className
        needs to conform to the pattern.
        <p>
        The java spec does not define behavior with a null
        className. If a null is passed in a ClassNotFoundException
        will be thrown.

        This will dynamically load RequisitionTester plus any classes that
        RequisitionTester instantiates that also belong to packages that
        begin with the string "test".
        
        @param className the name of the class to find - null not allowed
        @param pattern the classname pattern for other classes to reload
        @param warning if <b>true</b> and the class can not be found,
        a warning will be printed

        @return the Class for the given <B>className</B>

        @see #newInstance
        @see ariba.util.core.StringUtil#stringMatchesPattern
        @aribaapi private
    */
    public static Class classForNameNonCaching (String className,
                                                String pattern,
                                                boolean warning)
    {
        Assert.that(className != null, "className should not be null");

            // See other classForName for explanations on each of
            // these exception types.
        try {
            NonCachingClassLoader nccl = new NonCachingClassLoader(pattern);
            return nccl.loadClass(className);
        }
        catch (ClassNotFoundException e) {
                // dealt with below
        }
        catch (NoClassDefFoundError e) {
                // dealt with below
        }
        catch (SecurityException e) {
                // dealt with below
        }
        if (warning) {
            Log.util.error(2764, className);
        }
        return null;
    }

    /**
        Check if an object is an instance of a class identified by
        it's name.

        @param object the object to test the instance of
        @param className the name of the class being tested for

        @return true if <B>object</B> is an instance the class
        specified by <B>className</B>, including subclasses;
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean instanceOf (Object object, String className)
    {
        if (object == null) {
            return false;
        }
        return instanceOf(object.getClass(), classForName(className));
    }

    /**
        Check if one class inherits from another class.

        @param instance the class to check the inheritance tree of, must not be null.
        @param target the class to check against. If null, will return false.

        @return <b>true</b> if the class <B>instance</B> is, or
        inherits from, the class <B>target</B>; <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean instanceOf (Class instance, Class target)
    {
        if (target == null) {
            return false;
        }
        return target.isAssignableFrom(instance);
    }

    /**
        Returns the name of the class of the specified object. It is
        shorthand for o.getClass().getName()

        @param o the object to find the class name of
        @return the name as returned by o.getClass().getName()
        @aribaapi documented
    */
    public static String getClassNameOfObject (Object o)
    {
        if (o == null) {
            return "null";
        }
        return o.getClass().getName();
    }

    /**
        Creates a new instance of the specified class. If the class
        <B>className</B> is derived from BaseObject and you want the
        new instance to be properly initialized, you should call
        BaseObject.New() instead. If the class can not be found a
        warning will be printed.

        @param className the name of class to create a new instance of

        @return a new instance of the class specified by
        <B>className</B>. If the class can not be found, <b>null</b>
        will be returned.

        @see #classForName
        @aribaapi documented
    */
    public static Object newInstance (String className)
    {
        return newInstance(className, true);
    }

    /**
        Creates a new instance of the specified class. If the class
        <B>className</B> is derived from BaseObject and you want the
        new instance to be properly initialized, you should call
        BaseObject.New() instead. If the class can not be found an
        optional warning may be printed.

        @param className the name of class to create a new instance of
        @param warning if <b>true</b> and the class can not be found,
        a warning will be printed

        @return a new instance of the class specified by
        <B>className</B>. If the class can not be found, <b>null</b>
        will be returned.

        @see #classForName
        @aribaapi documented
    */
    public static Object newInstance (String className, boolean warning)
    {
        return newInstance(classForName(className, warning));
    }

    /**
        Creates a new Instance of the specified class with error
        checking. 

        Use this when the new object created should be a subclass of
        some known class. Prints an error message if the class cannot
        be created.

        @param className the class to create a new instance of
        @param supposedSuperclassName The name of the required
        superclass for the new class

        @return a new instance of the class <b>theClass</b>. If there
        is an error, <b>null</b> will be returned.
        @aribaapi documented
    */
    public static Object newInstance (String className,
                                      String supposedSuperclassName)
    {
        return newInstance(className, supposedSuperclassName, true);
    }

    /**
        Creates a new Instance of the specified class with error
        checking.

        Use this when the new object created should be a subclass of
        some known class.

        @param className the class to create a new instance of
        @param supposedSuperclassName The name of the required
        superclass for the new class
        @param warning if <b>true</b> and the class can not be found,
        a warning will be printed

        @return a new instance of the class <b>theClass</b>. If there
        is an error, <b>null</b> will be returned.
        @aribaapi documented
    */
    public static Object newInstance (String className,
                                      String supposedSuperclassName,
                                      boolean warning)
    {
        return newInstance(className,
                           classForName(supposedSuperclassName, warning),
                           warning);
    }
    /**
        Creates a new Instance of the specified class with error
        checking.

        Use this when the new object created should be a subclass of
        some known class.

        @param className the class to create a new instance of
        @param supposedSuperclass The class of the required
        superclass for the new class
        @param warning if <b>true</b> and the class can not be found,
        a warning will be printed

        @return a new instance of the class <b>theClass</b>. If there
        is an error, <b>null</b> will be returned.
        @aribaapi documented
    */
    public static Object newInstance (String  className,
                                      Class   supposedSuperclass,
                                      boolean warning)
    {
        return newInstance(classForName(className, warning),
                           supposedSuperclass,
                           warning);
    }

    /**
        Creates a new Instance of the specified class with error
        checking.

        Use this when the new object created should be a subclass of
        some known class.

        @param classObj the class to create a new instance of
        @param supposedSuperclass The class of the required
        superclass for the new class
        @param warning if <b>true</b> and the class can not be found,
        a warning will be printed

        @return a new instance of the class <b>theClass</b>. If there
        is an error, <b>null</b> will be returned.
        @aribaapi documented
    */
    public static Object newInstance (Class   classObj,
                                      Class   supposedSuperclass,
                                      boolean warning)
    {
        if (classObj == null) {
            return null;
        }
        if (supposedSuperclass == null) {
            return null;
        }
        Class clazz = checkInstanceOf(classObj, supposedSuperclass, warning);
        if (clazz == null) {
            return null;
        }
        return newInstance(clazz);
    }

    /**
        Creates a new instance of the specified class.

        If the class <B>theClass</B> is derived from BaseObject and
        you want the new instance to be properly initialized, you
        should call BaseObject.New() instead. If the instance can not
        be created, a warning will be printed.

        @param theClass the class to create a new instance of

        @return a new instance of the class <b>theClass</b>. If there
        is an error, <b>null</b> will be returned.

        @see #classForName
        @aribaapi documented
    */
    public static Object newInstance (Class theClass)
    {
        if (theClass == null) {
            return null;
        }
        try {
            return theClass.newInstance();
        }
        catch (InstantiationException e) {
            Log.util.error(2765, theClass.getName(), e);
        }
        catch (IllegalAccessException e) {
            Log.util.error(2766, theClass.getName(), e);
        }
        return null;
    }

    /**
        Strips any package specifiers from the given class name. For
        instance, "java.util.List" will become "List".

        @param className class name to strip

        @return the class name without the package prefix
        @aribaapi documented
    */
    public static String stripPackageFromClassName (String className)
    {
        int pos = className.lastIndexOf('.');
        if (pos > 0) {
            return className.substring(pos + 1);
        }
        return className;
    }

    /**
        Find the package specifier for a given class name. For
        instance, "java.util.List" will become "java.util".

        @param className class name to strip

        @return the package specifier for the given <B>className</B>
        @aribaapi documented
    */
    public static String stripClassFromClassName (String className)
    {
        int pos = className.lastIndexOf('.');
        if (pos > 0) {
            return className.substring(0, pos);
        }
        return "";
    }

    /**
        Invokes the specified static method of the specified class. If
        thrown, NoSuchMethodException, ClassNotFoundException,
        InvocationTargetException, and IllegalAccessException are
        silently caught and null is returned.

        @param className the name of the class to invoke the static
        method on
        @param methodName the name of the method to call

        @return the result of the method invocation, or if there was
        an exception while trying to find or invoke the method, null
        is returned.

        @see java.lang.reflect.Method#invoke
        @aribaapi documented
    */
    public static Object invokeStaticMethod (String   className,
                                             String   methodName)
    {
        try {
            Class c = ClassUtil.classForName(className);
            if (c != null) {
                Method m = c.getMethod(methodName);
                return m.invoke(null);
            }
        }
        catch (NoSuchMethodException e) {
        }
        catch (InvocationTargetException e) {
        }
        catch (IllegalAccessException e) {
        }
        return null;
    }

    /**
        Invokes the specified static method of the specified class. 

        @param className the name of the class to invoke the static
        method on
        @param methodName the name of the method to call
        @param paramTypes an array of Class types that *exactly* match
        the signature of the method being invoked.
        @param args an array of Object arguments to the method

        @return the result of the method invocation, or if there was
        an exception while trying to find or invoke the method, null
        is returned.

        @see java.lang.reflect.Method#invoke
        @aribaapi documented
    */
    public static Object invokeStaticMethod (String   className,
                                             String   methodName,
                                             Class[]  paramTypes,
                                             Object[] args)
    {
        try {
            Class c = ClassUtil.classForName(className);
            if (c != null) {
                Method m = c.getMethod(methodName, paramTypes);
                return m.invoke(null, args);
            }
        }
        catch (NoSuchMethodException e) {
            Assert.that(false, "NoSuchMethod :%s", SystemUtil.stackTrace(e));
        }
        catch (InvocationTargetException e) {
            Assert.that(false, "InvocationTargetException :%s", SystemUtil.stackTrace(e));
        }
        catch (IllegalAccessException e) {
            Assert.that(false, "IllegalAccessException :%s", SystemUtil.stackTrace(e));
        }
        return null;
    }

    /**
        ClassUtil.getDeclaredFields returns all the declared fields
        for the class and it's superclasses.

        The standard Class.getFields only returns public fields for
        the class and it's superclases, and the
        Class.getDeclaredFields returns all fields, public and
        private, but doesn't return superclass fields.

        This method does both private fields and superclass fields.
        @param clazz the Class to discover the fields of
        @return an array of all public and private fields accessable
        from this class
        @aribaapi documented
    */
    public static Field[] getDeclaredFields (Class clazz)
    {
            // Also count fields to allocate array size
        int fieldCount = 0;
        Class c = clazz;
        while (c != null) {
            fieldCount += c.getDeclaredFields().length;
            c = c.getSuperclass();
        }
        Field[] fields = new Field[fieldCount];

        c = clazz;
        while (c != null) {
            Field[] declaredFields = c.getDeclaredFields();
            int length = declaredFields.length;
            fieldCount -= length;
            System.arraycopy(declaredFields, 0,
                             fields, fieldCount, 
                             length);
            c = c.getSuperclass();
        }
        return fields;
    }

    private static final Map typeToVMMap = MapUtil.map();
    static {
        typeToVMMap.put(Constants.CharPrimitiveType,
                        Constants.JavaCharAbbreviation);
        typeToVMMap.put(Constants.BytePrimitiveType,
                        Constants.JavaByteAbbreviation);
        typeToVMMap.put(Constants.ShortPrimitiveType,
                        Constants.JavaShortAbbreviation);
        typeToVMMap.put(Constants.IntPrimitiveType,
                        Constants.JavaIntAbbreviation);
        typeToVMMap.put(Constants.LongPrimitiveType,
                        Constants.JavaLongAbbreviation);
        typeToVMMap.put(Constants.FloatPrimitiveType,
                        Constants.JavaFloatAbbreviation);
        typeToVMMap.put(Constants.DoublePrimitiveType,
                        Constants.JavaDoubleAbbreviation);
        typeToVMMap.put(Constants.BooleanPrimitiveType,
                        Constants.JavaBooleanAbbreviation);
    }
    /**
        Convert from a class name into an internal java representation
        of that class String

        int -> I
        java.lang.String -> Ljava.lang.String;
        
        @aribaapi private
    */
    public static String typeToVMType (String type)
    {
        String abbrev = (String)typeToVMMap.get(type);
        if (abbrev != null) {
            return abbrev;
        }
        return Fmt.S("L%s;", type);
    }

    /**
        Find a Class for the specified class name using the current thread's
        context class loader. Prints a warning message if the class can't be found
        and <B>warning</B> is true.

        @param className the name of the class to find
        @param warning if <b>true</b> and the class can not be found,
        a warning will be printed

        @return the Class for the given <B>className</B>, or null if
        the Class doesn't exist.

        @see #newInstance
    */
    private static Class classForNameUsingContextClassLoader (
        String  className,
        Class   supposedSuperclass,
        boolean warning)
    {
        try {
            ClassLoader loader =
                Thread.currentThread().getContextClassLoader();
            return checkInstanceOf(Class.forName(className, true, loader), // OK
                                   supposedSuperclass,
                                   warning);
        }
        catch (ClassNotFoundException e) {
                // dealt with below
        }
        catch(NoClassDefFoundError e) {
                // dealt with below
        }
        catch (SecurityException e) {
                // dealt with below
        }
        if (warning) {
            Log.util.error(2764, className);
        }
        return null;
    }

    /**
        If the classObj is assignable to the supposedSuperclass,
        return it. Otherwise optionally print a warning and return
        null.
        
        @param classObj the class to test
        @param supposedSuperclass The required superclass for the new class
        @param warning if <b>true</b> and the class is not assigned to
        the supposed superclass, a warning will be printed

        @aribaapi private
    */
    private static Class checkInstanceOf (Class classObj,
                                          Class supposedSuperclass,
                                          boolean warning)
    {
        if (instanceOf(classObj, supposedSuperclass)) {
            return classObj;
        }
        if (warning) {
            Log.util.error(4803, classObj.getName(), supposedSuperclass.getName());
        }
        return null;
    }

    /**
        This system property if true allows the use of the current thread's
        context class loader. Some application (like Sourcing) needs to
        use this class loader.
    */
    private static final String UseContextClassLoaderProperty =
        "ariba.util.core.ClassUtil.useContextClassLoader";

    /**
        This boolean allows the use of the context class loader via
        the use of UseContextClassLoaderProperty.
    */
    private static final boolean useContextClassLoader;

    static {
        boolean b = Boolean.getBoolean(UseContextClassLoaderProperty);
        useContextClassLoader = b;
    }

    private static final Class[] cloneArgType = new Class[0];
    private static final Object[] cloneArgValues = new Object[0];

    /**
        clone java.lang.Object.
        
        @aribaapi private
    */
    static Object clone (Object o)
    {
        Assert.that(o instanceof Cloneable, "Object is not cloneable: %s",
                    o.getClass().getName());
        try {
            Class thisClass = o.getClass();
            Method m = thisClass.getMethod("clone", cloneArgType);
            return m.invoke(o, cloneArgValues);
        }
        catch (NoSuchMethodException e) {
            Assert.that(false, "NoSuchMethod :%s", SystemUtil.stackTrace(e));
        }
        catch (InvocationTargetException e) {
            Assert.that(false, "InvocationTargetException :%s", SystemUtil.stackTrace(e));
        }
        catch (IllegalAccessException e) {
            Assert.that(false, "IllegalAccessException :%s", SystemUtil.stackTrace(e));
        }
        return null;
    }
}
