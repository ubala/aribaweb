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

    $Id: //ariba/platform/ui/awreload/ariba/awreload/FactorNotUsed.java#2 $
*/

package ariba.awreload;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ClassUtil;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DCONST;
import org.apache.bcel.generic.DRETURN;
import org.apache.bcel.generic.FCONST;
import org.apache.bcel.generic.FRETURN;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IRETURN;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LCONST;
import org.apache.bcel.generic.LRETURN;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.PUTSTATIC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
    A class for factoring a class file (bytecode) into two parts.  The
    Base part has the interface and the statics, the Derived part
    has the implementation.  Useful for reloading classes without restarting the
    server.

    THIS CODE IS CURRENTLY NOT USED.

    If we want to support dynamic reloading of page-level components, an updated version of this may
    be the ticket.  We would dynamically factor the class into a version conformant to the orignal
    (same fields and methods), dynamically hotswap *that*, and then create a subclass with the changes
    and change classForName() to load that.

    The exact kind of refactor below is not really the implementation described above.  It creates a stripped-down
    abstractified parent.  In this case we'd really just want to diff the new version against the original and
    factor all new code down.  So, this code remains here to simply serve as an example for that future work.
*/
public class FactorNotUsed extends Factor
{
    private ClassGen    _baseClassGen;
        // start by assuming the best until proven wrong.
    private boolean     _classShouldBeSplit = true;

    private long        _classFileTimeStamp = 0l;

    public JavaClass getBaseClass ()
    {
        return _baseClassGen.getJavaClass();
    }

    /**
        Factor a class into a base class and a derived class.
        @param className The path and name of the class.  E.g. "mypkg.vaMyClass"

        @return true if we could factor the class.  False if it isn't a final,leaf class.
    */
    public boolean factor (String className, String srcDir, String destDir) throws IOException
    {
        if (!_classShouldBeSplit) {
            return false;
        }

        _destDir = destDir;

        String path = getSourcePath(srcDir, className);

        File file = getSourceFile(srcDir, className);

        // the file won't exist outside of the jar unless it's been built explicitly
        if (file.exists())
        {
            long lastModified = file.lastModified();

            if (lastModified <= 0){
                warn("LastModified should be positive: "+lastModified);
            }
            if (lastModified > _classFileTimeStamp)
            {
                _classFileTimeStamp = lastModified;
                _javaClass = new ClassParser(path).parse();

                if (!_javaClass.isFinal()){
                    // only factor "final" classes because
                    // we need the guarantee that the class is a leaf
                    _classShouldBeSplit = false;
                    return false;
                }
                String superClassName = _javaClass.getSuperclassName();
                Class classSuper = ClassUtil.classForName(superClassName);
                if (!AWComponent.class.isAssignableFrom(classSuper)) {
                    _classShouldBeSplit = false;
                    return false;
                }

                createClasses();
            }
            return true;
         }
        else {
            throw new FileNotFoundException(path);
        }

    }

    public static File getSourceFile (String srcDir, String className)
    {
        String path = getSourcePath(srcDir, className);
        File file = new File(path);
        return file;
    }

    private static String getSourcePath (String srcDir, String className)
    {
        String path = srcDir + className.replace('.','/') + ".class";
        return path;
    }

    public void factor (JavaClass javaClass)
    {
        _javaClass = javaClass;
        createClasses();
    }

    private void createClasses ()
    {
        _baseClassGen = new ClassGen(_javaClass.copy());
        _derivedClassGen = new ClassGen(_javaClass.copy());

        if (_baseClassGen.isFinal()) {
            _baseClassGen.isFinal(false);
            _derivedClassGen.isFinal(false);
            try {
                createBaseClass();
                createDerivedClass(_baseClassGen.getClassName());
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        else {
            warn("Found non-final class");
        }
    }

    /**
        Create the base class.
    */
    public void createBaseClass () throws IOException
    {
        abstractifyMethods(_baseClassGen);

        removeConstructors(_baseClassGen);

        makePrivateProtected(_baseClassGen);

        removeStaticFields(_baseClassGen, false);

        _baseClassGen.isFinal(false);

        _baseClassGen.getJavaClass().dump(_destDir +
            _baseClassGen.getClassName().replace('.','/') + ".class");
    }


    /**
        Make private static members protected
    */
    private void makePrivateProtected (ClassGen cg)
    {
        Field[] fields = cg.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (f.isPrivate()  && f.isStatic()) {
                f.isPrivate(false);
                f.isProtected(true);
            }
        }
    }

    /**
        Create the derived class.
        @param superName The name of the new base class.  May be the same as the original
               file name.
    */
    public void createDerivedClass (String superName) throws IOException
    {
            // remove all interfaces
        String[] interfaces = _derivedClassGen.getInterfaceNames();
        for (int i=0; i < interfaces.length; i++) {
            _derivedClassGen.removeInterface(interfaces[i]);
        }

            //set the class name and the name for the member field references all at once.
        changeClassName(_derivedClassGen, getDerivedName());

            // workaround to reset cache
        _derivedClassGen.setConstantPool(
            new ConstantPoolGen(_derivedClassGen.getConstantPool().getFinalConstantPool()));

        String oldClassName = _derivedClassGen.getClassName();

            //call this or the cached value remains old
        _derivedClassGen.setClassName(getDerivedName());

        _derivedClassGen.setSuperclassName(superName);

            // Since we are only interested in classes which are loaded with
            // Class.forName().newInstance(), we don't need to preserve any
            // constructors besides the default (no argument) one.
        rewriteConstructors(_derivedClassGen,superName);

        removeStaticFields(_derivedClassGen, true);
        removeStaticMethods(_derivedClassGen);

        moveStaticReferences(_derivedClassGen, superName);

        _derivedClassGen.getJavaClass().dump(
            _destDir + _derivedClassGen.getClassName().replace('.','/') +  ".class");
    }

    private void rewriteConstructors (ClassGen cg, String superName)
    {
        Method[] methods = cg.getMethods();
        MethodGen con = null;

        for (int i=0; i < methods.length; i++)
        {
            if (methods[i].getName().equals("<init>"))
            {
                con = new MethodGen(methods[i],cg.getClassName(),cg.getConstantPool());
                cg.removeMethod(methods[i]);
                i=-1;
                methods=cg.getMethods();
            }
        }

        ConstantPoolGen cp = cg.getConstantPool();
        InstructionList patch = new InstructionList();

        patch.append(InstructionConstants.THIS); // Push `this'
        patch.append(new INVOKESPECIAL(cp.addMethodref(superName,
                            "<init>", "()V")));

        InstructionList il = con.getInstructionList();

        InstructionHandle[] ihs = il.getInstructionHandles();
        boolean found = false;
        for (int j = 1; j < ihs.length; j++) {
            Instruction in = ihs[j].getInstruction();
            if (found) {
                patch.append(in);
            }
            else if (in instanceof INVOKESPECIAL) {
                 found = true;
            }
         }

        con.setInstructionList(patch);
        cg.addMethod(con.getMethod());
    }

    private void moveStaticReferences(ClassGen cg, String newClass)
    {
        Method[] methods = cg.getMethods();
        for (int i=0; i < methods.length; i++)
        {
            methods[i] = moveStaticReferences(methods[i], cg.getConstantPool(), cg.getClassName(), newClass);
        }
    }

    private Method moveStaticReferences (Method m,
                                         ConstantPoolGen cp,
                                         String oldClass,
                                         String newClass)
    {
        MethodGen mg = new MethodGen(m, oldClass, cp);
        InstructionList il = mg.getInstructionList();
        InstructionHandle[] ihs = il.getInstructionHandles();

        for (int j = 0; j < ihs.length; j++) {
            Instruction i = ihs[j].getInstruction();
            if (i instanceof GETSTATIC || i instanceof PUTSTATIC) {
                FieldInstruction g = (FieldInstruction)i;
                String s = g.getClassName(cp);
                int index = g.getIndex();
                ConstantFieldref cfr =(ConstantFieldref)cp.getConstant(index);
                int classIndex = cfr.getClassIndex();
                if (oldClass.equals(s)) {
                    int newIndex = cp.addClass(newClass);
                    cfr.setClassIndex(newIndex);
                }
            }
        }
        il.dispose();
        return mg.getMethod();
    }

    /**
        Delete all constructors of a class.  Leave one default, do-nothing constructor.
        @param cg The ClassGen of the class.
    */
    private void removeConstructors (ClassGen cg)
    {
        Method[] methods = cg.getMethods();
        for (int i=0; i < methods.length; i++)
        {
            if (methods[i].getName().equals("<init>"))
            {
                cg.removeMethod(methods[i]);
                i=-1;
                methods=cg.getMethods();
            }
        }
        cg.addEmptyConstructor(ACC_PUBLIC);
    }

    /**
        Tear out the guts of a class.
        1) Delete any private methods
        2) Delete any methods which call a super version of themselves.  (Since they
           are defined in a super class, they may be deleted without changing the
           interface of the class.
        3) Make the remaining non-static methods of a class abstract

        @param cg The ClassGen of the class to gut.
    */
    private void abstractifyMethods (ClassGen cg)
    {
            // this is a copy of the methods.
        Method[] methods = cg.getMethods();
        ConstantPoolGen cp = cg.getConstantPool();

        Vector discardMethods = new Vector();
        outer:
        for (int j = 0; j < methods.length; j++) {
            MethodGen mg = new MethodGen(methods[j], cg.getClassName(), cp);
            InstructionList il = mg.getInstructionList();

            if (mg.isStatic()) {
                continue;
            }

            Iterator iter = il.iterator();
            boolean superCall = false;
            while (iter.hasNext()) {
                Instruction instruction = (Instruction)
                    ((InstructionHandle)iter.next()).getInstruction();
                if (instruction instanceof INVOKESPECIAL) {
                    INVOKESPECIAL is = (INVOKESPECIAL)instruction;
                    String methodName = is.getMethodName(cp);
                    if (methodName.equals(mg.getName())) {
                        superCall = true;
                    }
                }
                if (superCall || mg.isPrivate()) {
                    discardMethods.addElement(methods[j]);
                    continue outer;
                }
            }
            methods[j] = abstractifyMethod(methods[j],cg.getClassName(), cp);
        }
        cg.setMethods(methods);

        for (int i =0; i < discardMethods.size(); i++) {
            cg.removeMethod((Method)discardMethods.elementAt(i));
        }
    }

    /**
        Remove static fields from a class.

        @param cg The ClassGen for the class
        @param statik Set to true to remove static fields, false to remove non-static fields
    */
    public void removeStaticFields (ClassGen cg, boolean statik)
    {
        Field[] fields = cg.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isStatic() == statik) {
                cg.removeField(fields[i]);
                    // this ugliness is needed because bcel doesn't expose the vector
                    // of fields directly, nor provide any access besides this method.
                fields = cg.getFields();
                i=-1;
            }
        }
    }

    /**
        Delete all static methods from a class

        @param cg The ClassGen for the class
    */
    public void removeStaticMethods (ClassGen cg)
    {
        Method[] methods = cg.getMethods();

        for (int i = 0; i < methods.length; i++) {
            if (methods[i].isStatic() && !methods[i].getName().equals("main")) {
                cg.removeMethod(methods[i]);
                methods = cg.getMethods();
                i = -1;
            }
        }
    }

/*
    Remove code from a method
    @param m The method
*/
    private void removeCode (Method m)
    {
        if (m.getCode() == null) {
            return;
        }
        Attribute[] old = m.getAttributes();
        Attribute[] a = new Attribute[old.length - 1];
        int j = 0;
        for (int i = 0; i < old.length; i++) {
            if (!(old[i] instanceof Code)) {
                a[j++] = old[i];
            }
        }
        m.setAttributes(a);
    }


    /**
        Make a method abstract.
        @param m The method
    */
    private Method abstractifyMethod (Method m, String className, ConstantPoolGen cp)
    {
        Code code = m.getCode();
        if (m.isNative() ||  m.isAbstract() ||  m.isStatic() || m.isPrivate() ||
            (code == null)) {
            return m;
        }
            //it's illegal to have an empty constructor. Must at least call super.<init>()
        if (!m.getName().equals("<init>")) {
            m = removeCodeExceptReturn(m,className,cp);
        }
        return m;
    }

    private static Method removeCodeExceptReturn (Method m, String className, ConstantPoolGen cp)
    {
        Code   code  = m.getCode();
        int    flags = m.getAccessFlags();
        String name  = m.getName();

        InstructionList patch  = new InstructionList();
        MethodGen           mg  = new MethodGen(m, className, cp);
        Type type = Type.getReturnType(m.getSignature());
        switch (type.getType()) {
            case Constants.T_VOID:
                patch.append(new RETURN());
            break;
            case Constants.T_INT:
            case Constants.T_BOOLEAN:
            case Constants.T_SHORT:
            case Constants.T_CHAR:
                patch.append(new ICONST(0));
                patch.append(new IRETURN());
            break;
            case Constants.T_FLOAT:
                patch.append(new FCONST(0));
                patch.append(new FRETURN());
            break;
            case Constants.T_DOUBLE:
                patch.append(new DCONST(0));
                patch.append(new DRETURN());
            break;
            case Constants.T_LONG:
                patch.append(new LCONST(0));
                patch.append(new LRETURN());
            break;
            default:
                patch.append(new ACONST_NULL());
                patch.append(new ARETURN());
        }
        mg.stripAttributes(true);
        mg.setInstructionList(patch);
        mg.removeExceptionHandlers();
        mg.removeLocalVariables();

        m = mg.getMethod();

        return m;
    }
}