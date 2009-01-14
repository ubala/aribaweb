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

    $Id: //ariba/platform/ui/awreload/ariba/awreload/Factor.java#9 $
*/

package ariba.awreload;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import ariba.util.fieldvalue.CompiledAccessorFactory;

/**
    A simple class renamer.  Used to take class (typically a non-page-level AWComponent) that have runtime changes that
    cannot be reloaded directly by the VM (e.g. additions of methods or fields) and create a renamed version of that
    class that can be loaded along side the original.
*/
public class Factor implements Constants
{
    protected static String Suffix = "_ver";
    protected static int VerSeq = 0;

    protected String      _destDir;
    protected JavaClass   _javaClass;

    protected ClassGen    _derivedClassGen;
    protected String      _derivedName;

    public static Class renameAndReload(Class origClass, String className, byte[] byteCode, String destPath)
    {
        Factor instance = new Factor();
        return instance._renameAndReload(origClass, className, byteCode, destPath);
    }

    protected Class _renameAndReload(Class origClass, String className, byte[] byteCode, String destPath)
    {
        _destDir = destPath;
        String newName = null;
        InputStream is = new ByteArrayInputStream(byteCode);
        Class cls = null;

        try {
            _javaClass = new ClassParser(is, className).parse();

            _derivedClassGen = new ClassGen(_javaClass.copy());

            String oldClassName = _derivedClassGen.getClassName();
            newName = getDerivedName();

                //set the class name and the name for the member field references all at once.
            changeClassName(_derivedClassGen, newName);

                // workaround to reset cache
            _derivedClassGen.setConstantPool(
                new ConstantPoolGen(_derivedClassGen.getConstantPool().getFinalConstantPool()));

                //call this or the cached value remains old
            _derivedClassGen.setClassName(newName);

            byte[] bytecode =_derivedClassGen.getJavaClass().getBytes();

            // In case our build root is in the class path, try to write out
            // the bytes, then load it
            try {
                String dest = new File(destPath, newName.replace('.','/') +  ".class").toString();
                _derivedClassGen.getJavaClass().dump(dest);
                cls =  JavaReloadClassLoader._classForName(newName);
            } catch (Throwable t) {
            }

            // we probably couldn't find it.  Load it with our own class loader
            if (cls == null) {
                try {
                    cls = CompiledAccessorFactory.ByteArrayClassLoader.loadClass(newName, bytecode, origClass.getProtectionDomain());
                } catch (Exception e) {
                    Console.println("Exception loading transformed class: " + newName);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassFormatException e) {
            e.printStackTrace();
        }
        return cls;
    }

    public static String getOriginalClassName (String alteredName)
    {
        int i = alteredName.indexOf(Suffix);
        return (i==-1) ? alteredName : alteredName.substring(0,i);
    }

    public JavaClass getDerivedClass ()
    {
        return _derivedClassGen.getJavaClass();
    }

    public String getDerivedName ()
    {
        if (_derivedName != null) return _derivedName;

        return _derivedClassGen.getClassName() + Suffix + VerSeq++;
    }

    /**
        Change class name to <old_name>suffix
        @param cg The ClassGen of the class to rename
        @param className New name for the class
    */
    protected void changeClassName (ClassGen cg, String className)
    {
        int index = cg.getClassNameIndex();

        ConstantPoolGen cp = cg.getConstantPool();
        index = ((ConstantClass)cp.getConstant(index)).getNameIndex();
        cp.setConstant(index, new ConstantUtf8(className.replace('.', '/')));
    }


    /**
        @param warning The warning message.
    */
    public static void warn (String warning)
    {
        System.out.println(warning);
    }
}
