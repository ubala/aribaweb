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

    $Id: //ariba/platform/util/core/ariba/util/core/NonCachingClassLoader.java#9 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
    Implementation of a ClassLoader that can be used to force
    reloading of class files from the file system.  This is useful for
    applications that provide API's for clients to write
    implementations for.  If the NonCachingClassLoader is used to load
    those implementations, then authors of those implementations do
    not need to restart the application that calls their
    implementations.

    For this to work properly, you need to instantiate the class and
    assign it to a variable that is of the type of a superclass or an
    interface of the class that is getting loaded.  If not you will
    get a ClassCastException.  Contrary to popular belief, classes are
    not unique within a VM; they are unique within a class loader
    within a VM.  Thus you can create what you think is a singleton
    class, such as ClassUtil but find that it gets loaded multiple
    times if you use the NonCachingClassLoader.  Each class called
    ClassUtil is scoped by the class loader instance that loaded it.
    If you try to compare the two objects, you will get a
    ClassCastException.

    The code that calls the loadClass method and then calls
    ClassUtil.newInstance with the returned class needs to cast the
    result to an Interface or superclass that was loaded by the
    classloader that also loaded the code that calls loadClass.  If
    not, you will get a ClassCastException.
        
    Reloading a class necessarily creates multiple copies within the
    VM, each constrained to the scope of the instance of
    NonCachingClassLoader that loaded it.  There are other classes
    you'll want reloaded by this classloader.  For example, if you
    want to reload config.java.foo.Bar and config.java.foo.FooUtil
    which contains utils that Bar calls, then you can instantiate a
    NonCachingClassLoader with the pattern of "/config.S/" (where S
    stands for *, which I cannot actually type in a javadoc comment or
    else it will interpret the star slash as the end of this comment).
    However, you probably do not want ariba.util.core.ClassUtil
    reloaded.  You can prevent that by making sure you choose a
    pattern that ariba.util.core.ClassUtil does not conform to.

    Here is an example usage in which DummyClass implements DummyInterface:

    public class ClassUtilTest
    {    
      public void classForNameNonCachingTest ()
      {
        String className = "test.ariba.util.core.DummyClass";        
        Class c = ClassUtil.classForNameNonCaching(className, "/test.*Class/", false);
        DummyInterface inst = (DummyInterface)ClassUtil.newInstance(c);
        Assert.that(inst.alwaysTrue(), "expected true");
      }
    }

    Furthermore this is the logging you will see:

        Loading test.ariba.util.core.DummyClass with NonCachingClassLoader
        Letting java.lang.Object be loaded by primordial class loader
        Letting test.ariba.util.core.DummyInterface be loaded by primordial class loader

    See that DummyInterface is loaded by the primordial class loader?
    That is the same class loader that loaded ClassUtilTest.  The
    following code will throw a ClassCastException:

    public class ClassUtilTest
    {    
      public void classForNameNonCachingTest ()
      {
        String className = "test.ariba.util.core.DummyClass";        
        Class c = ClassUtil.classForNameNonCaching(className, "/test.*Class/", false);
        DummyClass inst = (DummyClass)ClassUtil.newInstance(c);
        Assert.that(inst.alwaysTrue(), "expected true");
      }
    }

        Loading test.ariba.util.core.DummyClass with NonCachingClassLoader
        Letting java.lang.Object be loaded by primordial class loader
        Letting test.ariba.util.core.DummyInterface be loaded by primordial class loader
        java.lang.ClassCastException: test.ariba.util.core.DummyClass
            at test.ariba.util.core.ClassUtilTest.classForNameNonCachingTest(ClassUtilTest.java:240)
            at test.ariba.util.core.ClassUtilTest.runTests(ClassUtilTest.java:54)
            at test.ariba.util.core.ClassUtilTest.main(ClassUtilTest.java:37) 

    @aribaapi private    
*/
public class NonCachingClassLoader extends ClassLoader {

    public static final String ClassName = "ariba.util.core.NonCachingClassLoader";
    private Map classes = MapUtil.map();
    private String pattern;

    private static final String ClassFileExtension = ".class";

    /**
        Performs initialization for NonCachingClassLoader by calling
        the super and setting the pattern for the class.  The
        pattern is the pattern that will be used to identify
        classes that NonCachingClassLoader should always reload from
        disk.

        @param pattern for identifying classes to reload
    */
    public NonCachingClassLoader (String pattern)
    {
        this(ClassUtil.classForName(ClassName, false), pattern);
    }

    /**
        This is a trick to get the compiler happy and still try to
        be efficient and not call classForName more than once
    */
    private NonCachingClassLoader (Class myClass, String pattern)
    {
        this(myClass == null ? null : myClass.getClassLoader(), pattern);
    }
    
    /**
        Performs initialization for NonCachingClassLoader by calling
        the super and setting the pattern for the class.  The
        pattern is the pattern that will be used to identify
        classes that NonCachingClassLoader should always reload from
        disk.

        @param parent parent class loader used for delegation
        @param pattern for identifying classes to reload
    */
    public NonCachingClassLoader (ClassLoader parent, String pattern)
    {
        super(parent == null ? getSystemClassLoader() : parent);
        this.pattern = pattern;
    }

    /**
        Simple version of loadClass for external clients
        since they will always want the class resolved before it is
        returned to them.

        @param className the name of the desired Class
    */
    public synchronized Class loadClass (String className)
      throws ClassNotFoundException
    {
        return (loadClass(className, true));
    }

    /**
        Main loadClass method for loading a class and forcing a reload
        of the class from disk if the class name conforms to the pattern
        set as the 'pattern' for the class.

        @param className the name of the desired Class
        @param resolveIt true if the Class needs to be resolved
    */
    public synchronized Class loadClass (String className, boolean resolveIt)
      throws ClassNotFoundException
    {
        Class result;

            // Check our local cache of classes (required to prevent
            // ClassCastExceptions)
        result = (Class)classes.get(className);
        if (result != null) {
            Log.util.debug("Got %s from classes map cache", className);
            return result;
        }

        if (StringUtil.stringMatchesPattern(className, pattern)) {
            Log.util.debug("Loading %s with NonCachingClassLoader", className);
            byte classData[] = null;
            try {
                String resourceName = StringUtil.replaceCharByString(className,
                                                               '.', "/");
                resourceName = StringUtil.strcat(resourceName, ClassFileExtension);
                InputStream in = getResourceAsStream(resourceName);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtil.inputStreamToOutputStream(in, baos);
                classData = baos.toByteArray();
                in.close();
            }
            catch (IOException ioe) {
                throw new ClassNotFoundException(Fmt.S("Could not read class file: %s",
                                                       ioe));
            }
            
            if (classData == null) {
                throw new ClassNotFoundException(className);
            }
            
            result = defineClass(className, classData, 0, classData.length);
            if (result == null) {
                throw new ClassFormatError();
            }
            
            if (resolveIt) {
                resolveClass(result);
            }
            
            classes.put(className, result);
            
            return result;
        }
        else {
            Log.util.debug("Letting %s be loaded by primordial class loader", className);
                // Check with the primordial class loader
            if (getParent() != null) {
                return getParent().loadClass(className);
            }
            else {
                return findSystemClass(className);
            }
        }
    }
}
