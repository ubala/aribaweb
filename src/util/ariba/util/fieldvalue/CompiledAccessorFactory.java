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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/CompiledAccessorFactory.java#3 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.log.Log;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Type;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

/**
    CompiledAccessorFactory creates instances of CompiledAccessor given either a
java.lang.reflect.Field or java.lang.reflect.Method.  For Field, both the setter and
getter methods are generated, but for Method, only the appropriate getter or setter method
is generated, depending upon if the Method is for a Setter or Getter.  The
CompiledAccessor's are cached and are only created once for for each Field or Method.

    This class employs the Apache Jakarta project's BCEL (Byte Code Engineering Library)
to generate the classes at runtime.  Also, a custom ClassLoader (ByteArrayClassLoader)
has been created to allow for loading the newly created classes without wirting anything
to disk.

*/
public class CompiledAccessorFactory extends Object
{
    // This is the increment of the moving threshold
    /** @deprecated use setAccessThresholdToCompile(int)  */
    public static int AccessThresholdToCompile = -1;
    // This is the moving threshold.  Set it high to begin with so that all accessors have
    // an opportunity to get used before the threshold starts moving.
    /** @deprecated use setAccessThresholdToCompile(int)  */
    public static int AccessThresholdCurrent = -1;
    // This is the maximum number of accessors to compile
    // -1 means there is no maximum. 0 means no compiled accessor. positive number is a limit
    private static int MaxCompiledAccessorCount = -1;
    private static int CompiledAccessorCount = 0;
    public static boolean VerboseEnabled = false;
    private static final String[] EmptyStringArray = new String[0];
    private static final Type[] EmptyTypeArray = new Type[0];
    private static final String[] GetterArgNamesArray = new String[] {"target"};
    private static final Type[] GetterArgTypesArray = new Type[] {Type.OBJECT};
    private static final String[] SetterArgNamesArray = new String[] {"target", "value"};
    private static final Type[] SetterArgTypesArray =
        new Type[] {Type.OBJECT, Type.OBJECT};
    private static final String _ThisPackageName = ClassUtil.
        stripClassFromClassName(CompiledAccessorFactory.class.getName());

    // This keeps all compiled accessors by their declaringClass and Name.
    private static final MultiKeyHashtable CompiledFields = new MultiKeyHashtable(2);
    // Also caches on type (set vs get)
    private static final MultiKeyHashtable CompiledMethods = new MultiKeyHashtable(3);

    public static void setAccessThresholdToCompile (int integer)
    {
        Log.util.debug("Setting AccessThresholdToCompile to %s", integer);
        AccessThresholdToCompile = integer;
        if (AccessThresholdToCompile != -1) {
            AccessThresholdCurrent = 10 * AccessThresholdToCompile;
        }
        else {
            AccessThresholdCurrent = -1;
        }
    }

    public static void _setAccessThresholdToCompile (int integer)
    {
        AccessThresholdCurrent = 10 * AccessThresholdToCompile;
    }

    public static void setMaxCompiledAccessorCount (int count)
    {
        Log.util.debug("Setting MaxCompiledAccessorCount to %s", count);
        MaxCompiledAccessorCount = count;
    }


    protected static boolean compiledAccessorThresholdPassed (int accessCount)
    {
        // Check that the count of compiled accessors have not exceeded the maximum
        if ((CompiledAccessorCount >= MaxCompiledAccessorCount) && (MaxCompiledAccessorCount != -1)) {
            return false;
        }
        return ((accessCount >= AccessThresholdCurrent) && (AccessThresholdCurrent != -1));
    }

    public static int compiledAccessorCount ()
    {
        return CompiledAccessorCount;
    }

    //////////////////
    // Field Accesors
    //////////////////
    public static CompiledAccessor newInstance (java.lang.reflect.Field field)
    {
        // Note: this will rarely be called, so the scope of this synchronized.
        // block is not an issue for concurrency.
        CompiledAccessor compiledField = null;
        if (!isAccessible(field)) {
            Log.util.debug("field is not accessible: %s", field);            
            return null;
        }
        synchronized (CompiledFields) {
            Class declaringClass = field.getDeclaringClass();
            String fieldName = field.getName();
            compiledField =
                (CompiledAccessor)CompiledFields.get(declaringClass, fieldName);
            if (compiledField == null) {
                try {
                    AccessThresholdCurrent += AccessThresholdToCompile;
                    CompiledAccessorCount++;
                    if ((CompiledAccessorCount >= MaxCompiledAccessorCount) && (MaxCompiledAccessorCount != -1)) {
                        Log.util.debug("The Maximum Accessor Count has been hit : %s", MaxCompiledAccessorCount);
                    }
                    compiledField = constructNewAccessor(field);
                    Assert.that(compiledField != null,
                                "constructNewAccessor failed to return compiledField");
                }
                catch (RuntimeException exception) {
                    String stackTrace = SystemUtil.stackTrace(exception);
                    String message = Fmt.S("Cannot create newInstance for field: %s\n%s",
                                           field, stackTrace);
                    throw new FieldValueException(message);
                }
                CompiledFields.put(declaringClass, fieldName, compiledField);
            }
        }
        return compiledField;
    }

    private static CompiledAccessor constructNewAccessor (java.lang.reflect.Field field)
    {
        String strippedClassName = strippedClassName(field);
        String newClassName = StringUtil.strcat(
            newClassNamePrefix(strippedClassName), "_field_", field.getName());
        String javaFileName = StringUtil.strcat(strippedClassName, ".java");
        ClassGen classGen = new ClassGen(newClassName,
            CompiledAccessor.class.getName(),
            javaFileName,
            Constants.ACC_PUBLIC | Constants.ACC_FINAL | Constants.ACC_SUPER,
            EmptyStringArray);
        classGen.addEmptyConstructor(Constants.ACC_PUBLIC);

        ConstantPoolGen constantPoolGen = classGen.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGen,
                                                                       constantPoolGen);

        org.apache.bcel.classfile.Method getterMethod =
            constructGetterMethod(newClassName, constantPoolGen,
                                  instructionFactory, field);
        classGen.addMethod(getterMethod);

        org.apache.bcel.classfile.Method setterMethod =
            constructSetterMethod(newClassName, constantPoolGen,
                                  instructionFactory, field);
        classGen.addMethod(setterMethod);

        return generateClassAndGetInstance(classGen, newClassName, 
                                           field.getDeclaringClass().getProtectionDomain());
    }

    private static org.apache.bcel.classfile.Method
        constructSetterMethod (String newClassName, ConstantPoolGen constantPoolGen,
        InstructionFactory instructionFactory, java.lang.reflect.Field field)
    {
        // Constructing a method that looks as follows:
        // public void setValue (Object target, Object value)
        // {
        //       Note that fieldName name be a static
        //     ((<TargetClass>)target).<fieldName> = (<ValueClass>)value;
        // }

        // In the case of primitive typed fields...
        // Constructing a method that looks as follows:
        // public void setValue (Object target, Object value)
        // {
        //     ((<TargetClass>)target).<fieldName> =
        //            CompiledAccessorFactory.get<ValueClass>(value);
        // }
        String declaringClassName = field.getDeclaringClass().getName();
        InstructionList instructionList = new InstructionList();
        // push 'target' onto operand stack
        instructionList.append(new ALOAD(1));
        ReferenceType targetType = new ObjectType(declaringClassName);
        instructionList.append(instructionFactory.createCheckCast(targetType));

        Class fieldTypeClass = field.getType();
        Type fieldType = computeType(fieldTypeClass);
        pushAndConvertValueArg(instructionList, instructionFactory,
                               fieldTypeClass, fieldType);

        FieldInstruction fieldInstruction = null;
        if (Modifier.isStatic(field.getModifiers())) {
            fieldInstruction =
                instructionFactory.createPutStatic(declaringClassName, field.getName(),
                                                   fieldType);
        }
        else {
            fieldInstruction =
                instructionFactory.createPutField(declaringClassName,
                                                  field.getName(), fieldType);
        }
        instructionList.append(fieldInstruction);
        return handleReturnAndGenerateSetterMethod(newClassName, instructionList,
                                                   constantPoolGen);
    }

    private static org.apache.bcel.classfile.Method
        constructGetterMethod (String newClassName, ConstantPoolGen constantPoolGen,
        InstructionFactory instructionFactory, java.lang.reflect.Field field)
    {
        // Constructing a method that looks as follows:
        // public Object getValue (Object target)
        // {
        //     Note this may invoke conversion routines before returning.
        //     return ((<TargetClass>)target).<fieldName>;
        // }
        InstructionList instructionList = new InstructionList();
        // pushd 'target' onto operand stack
        instructionList.append(new ALOAD(1));

        boolean isStaticField = Modifier.isStatic(field.getModifiers());
        String declaringClassName = field.getDeclaringClass().getName();
        ReferenceType targetType = new ObjectType(declaringClassName);
        Class fieldTypeClass = field.getType();
        Type fieldType = computeType(fieldTypeClass);
        if (isStaticField) {
            // handle static fields -- simply pop rather than checkcast
            instructionList.append(new POP());
            instructionList.append(
                instructionFactory.createGetStatic(declaringClassName,
                    field.getName(), fieldType));
        }
        else {
            // handle instance fields
            instructionList.append(instructionFactory.createCheckCast(targetType));
            instructionList.append(instructionFactory.createGetField(declaringClassName,
                                                            field.getName(), fieldType));
        }
        return handleReturnAndGenerateGetterMethod(newClassName,
            fieldTypeClass, instructionList, instructionFactory, constantPoolGen);
    }

    ////////////////////
    // Method Accessors
    ////////////////////
    public static CompiledAccessor newInstance (java.lang.reflect.Method method,
                                                boolean isSetter)
    {
        CompiledAccessor compiledMethod = null;
        if (!isAccessible(method)) {
            Log.util.debug("method is not accessible: %s", method);            
            return null;
        }
        if (isSetter) {
            // handle case where the signature of the setter is setFoo(SomeNonpublicClass x)
            Class[] paramterTypes = method.getParameterTypes();
            Class parameter = paramterTypes[0];
            int modifiers = parameter.getModifiers();
            if (!Modifier.isPublic(modifiers)) {
                Log.util.debug("setter method argument must be public: %s", method);
                // returning null here will cause the existing reflection Method to continue being used.
                return null;
            }
        }
        synchronized (CompiledMethods) {
            Class declaringClass = method.getDeclaringClass();
            String methodName = method.getName();
            String type = methodType(isSetter);
            compiledMethod = (CompiledAccessor)CompiledMethods.get(declaringClass,
                                                                   methodName, type);
            if (compiledMethod == null) {
                try {
                    AccessThresholdCurrent += AccessThresholdToCompile;
                    CompiledAccessorCount++;
                    if ((CompiledAccessorCount >= MaxCompiledAccessorCount) && (MaxCompiledAccessorCount != -1)) {
                        Log.util.debug("The Maximum Accessor Count has been hit : %s", MaxCompiledAccessorCount);
                    }
                    compiledMethod = constructNewAccessor(method, isSetter);
                    Assert.that(compiledMethod != null,
                                "constructNewAccessor failed to return compiledMethod");
                }
                catch (RuntimeException exception) {
                    String stackTrace = SystemUtil.stackTrace(exception);
                    String message = Fmt.S(
                        "Cannot create newInstance for method: %s isSetter: %s\n%s",
                        method, isSetter ? "true" : "false", stackTrace);
                    throw new FieldValueException(message);
                }
                CompiledMethods.put(declaringClass, methodName, type, compiledMethod);
            }
        }
        return compiledMethod;
    }

    private static boolean isAccessible (Member member)
    {
        int memberModifiers = member.getModifiers();
        boolean isAccessible = Modifier.isPublic(memberModifiers);
        if (isAccessible) {
            Class declaringClass = member.getDeclaringClass();
            int classModifiers = declaringClass.getModifiers();
            isAccessible = Modifier.isPublic(classModifiers);
            if (isAccessible && member instanceof Field) {
                Class fieldTypeClass = ((Field)member).getType();
                int fieldTypeModifiers = fieldTypeClass.getModifiers();
                isAccessible = Modifier.isPublic(fieldTypeModifiers);
            }
        }
        return isAccessible;
    }

    private static String methodType (boolean isSetter)
    {
        return isSetter ? "set" : "get";
    }

    private static String strippedClassName (java.lang.reflect.Field field)
    {
        String className = field.getDeclaringClass().getName();
        return ClassUtil.stripPackageFromClassName(className);
    }

    private static String strippedClassName (java.lang.reflect.Method method)
    {
        String className = method.getDeclaringClass().getName();
        return ClassUtil.stripPackageFromClassName(className);
    }

    private static String newClassNamePrefix (String strippedClassName)
    {
        return StringUtil.strcat(_ThisPackageName, ".compiled.", strippedClassName);
    }

    private static CompiledAccessor constructNewAccessor (java.lang.reflect.Method method,
                                                          boolean isSetter)
    {
        String strippedClassName = strippedClassName(method);
        String newClassName =
            StringUtil.strcat(newClassNamePrefix(strippedClassName), "_",
                              methodType(isSetter), "Method_", method.getName());
        String javaFileName = StringUtil.strcat(strippedClassName, ".java");
        ClassGen classGen = new ClassGen(newClassName,
            CompiledAccessor.class.getName(),
            javaFileName,
            Constants.ACC_PUBLIC | Constants.ACC_FINAL | Constants.ACC_SUPER,
            EmptyStringArray);
        classGen.addEmptyConstructor(Constants.ACC_PUBLIC);

        ConstantPoolGen constantPoolGen = classGen.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGen,
                                                                       constantPoolGen);

        org.apache.bcel.classfile.Method accessorMethod = null;
        if (isSetter) {
            accessorMethod =
                constructSetterMethod(newClassName, constantPoolGen,
                                      instructionFactory, method);
        }
        else {
            accessorMethod =
                constructGetterMethod(newClassName, constantPoolGen,
                                      instructionFactory, method);
        }
        classGen.addMethod(accessorMethod);

        return generateClassAndGetInstance(classGen, newClassName, 
                                           method.getDeclaringClass().getProtectionDomain());
    }

    private static org.apache.bcel.classfile.Method
        constructGetterMethod (String newClassName, ConstantPoolGen constantPoolGen,
                               InstructionFactory instructionFactory,
                               java.lang.reflect.Method method)
    {
        // we're trying to generate code for the line of code:
        // public Object getValue (Object target)
        // {
        //     return ((<TargetType>)target).<methodName>();
        // }
        InstructionList instructionList = new InstructionList();
        // pushd 'target' onto operand stack
        instructionList.append(new ALOAD(1));

        String targetClassName = method.getDeclaringClass().getName();
        ReferenceType targetType = new ObjectType(targetClassName);
        // generate invoke instruction
        Class returnTypeClass = method.getReturnType();
        Type returnType = computeType(returnTypeClass);
        boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
        if (!isStaticMethod) {
            instructionList.append(instructionFactory.createCheckCast(targetType));
        }
        short invokeKind = isStaticMethod ?
            Constants.INVOKESTATIC : Constants.INVOKEVIRTUAL;
        instructionList.append(instructionFactory.createInvoke(targetClassName,
            method.getName(), returnType, EmptyTypeArray, invokeKind));
        return handleReturnAndGenerateGetterMethod(newClassName,
            returnTypeClass, instructionList, instructionFactory, constantPoolGen);
    }

    private static org.apache.bcel.classfile.Method
        constructSetterMethod (String newClassName, ConstantPoolGen constantPoolGen,
            InstructionFactory instructionFactory, java.lang.reflect.Method method)
    {
        // we're trying to generate code for the line of code:
        // public void setValue (Object target, Object value)
        // {
        //     ((<TargetType>)target).<set_MethodName>((<ValueType>)value);
        // }

        // In the case of primitive typed Methods...
        // Constructing a method that looks as follows:
        // public void setValue (Object target, Object value)
        // {
        //     ((<TargetType>)target).<set_MethodName>
        //            (CompiledAccessorFactory.get<ValueClass>(value));
        // }
        Class returnTypeClass = method.getReturnType();
        Type returnType = computeType(returnTypeClass);
        String targetClassName = method.getDeclaringClass().getName();
        InstructionList instructionList = new InstructionList();
        // pushd 'target' and 'value' onto operand stack and check type of each
        instructionList.append(new ALOAD(1));
        ReferenceType targetType = new ObjectType(targetClassName);
        instructionList.append(instructionFactory.createCheckCast(targetType));

        Class valueTypeClass = method.getParameterTypes()[0];
        Type valueType = computeType(valueTypeClass);
        pushAndConvertValueArg(instructionList, instructionFactory, valueTypeClass,
                               valueType);
        // generate invoke instruction
        Type[] argTypes = new Type[] {
            valueType,
        };
        short invokeKind = Modifier.isStatic(method.getModifiers()) ?
                Constants.INVOKESTATIC : Constants.INVOKEVIRTUAL;
        instructionList.append(
            instructionFactory.createInvoke(targetClassName, method.getName(),
                                            returnType, argTypes, invokeKind));

        return handleReturnAndGenerateSetterMethod(newClassName,
                                                    instructionList, constantPoolGen);
    }

    ////////////////
    // Util
    ////////////////
    private static void convertToPrimitiveType (String utilityMethodName, Type fieldType,
        InstructionList instructionList, InstructionFactory instructionFactory)
    {
        instructionList.append(
            instructionFactory.createInvoke(CompiledAccessorFactory.class.getName(),
                                            utilityMethodName,
                                            fieldType,
                                            GetterArgTypesArray,
                                            Constants.INVOKESTATIC));
    }

    private static InvokeInstruction basicTypeTranslation (Class returnClass,
    Class argTypeClass, String utilityMethodName, InstructionFactory instructionFactory)
    {
        Type argType = computeType(argTypeClass);
        Type returnType = computeType(returnClass);
        InvokeInstruction invokeInstruction =
            instructionFactory.createInvoke(CompiledAccessorFactory.class.getName(),
                                            utilityMethodName,
                                            returnType,
                                            new Type[] {argType},
                                            Constants.INVOKESTATIC);
        return invokeInstruction;
    }

    private static Type computeType (Class typeClass)
    {
        Type type = null;
        if (typeClass.isPrimitive()) {
            if (typeClass == Boolean.TYPE) {
                type = Type.BOOLEAN;
            }
            else if (typeClass == Integer.TYPE) {
                type = Type.INT;
            }
            else if (typeClass == Float.TYPE) {
                type = Type.FLOAT;
            }
            else if (typeClass == Double.TYPE) {
                type = Type.DOUBLE;
            }
            else if (typeClass == Short.TYPE) {
                type = Type.SHORT;
            }
            else if (typeClass == Character.TYPE) {
                type = Type.CHAR;
            }
            else if (typeClass == Byte.TYPE) {
                type = Type.BYTE;
            }
            else if (typeClass == Long.TYPE) {
                type = Type.LONG;
            }
            else if (typeClass == Void.TYPE) {
                type = Type.VOID;
            }
        }
        else if (typeClass.isArray()) {
            String arrayClssName = typeClass.getName();
            int dimensionCount = StringUtil.occurs(arrayClssName, '[');
            // Note: recursion here.
            Type arrayComponentType = computeType(typeClass.getComponentType());
            type = new ArrayType(arrayComponentType, dimensionCount);
        }
        else {
            String typeSignature = Utility.getSignature(typeClass.getName());
            type = Type.getType(typeSignature);
            Assert.that(type != null,
                        "Unexpected null returned from Type.getType(typeSignature)");
        }
        return type;
    }

    private static org.apache.bcel.classfile.Method handleReturnAndGenerateGetterMethod (
         String newClassName, Class returnTypeClass, InstructionList instructionList,
         InstructionFactory instructionFactory, ConstantPoolGen constantPoolGen)
    {
        if (returnTypeClass.isPrimitive()) {
            //The following session of if...else adds an additional instruction to
            //convert the primitive type filds to their respective Object types.
            Instruction instruction = null;
            if (returnTypeClass == Boolean.TYPE) {
                instruction = basicTypeTranslation(Boolean.class, Boolean.TYPE,
                                                   "getBoolean", instructionFactory);
            }
            else if (returnTypeClass == Integer.TYPE) {
                instruction = basicTypeTranslation(Integer.class, Integer.TYPE,
                                                   "getInteger", instructionFactory);
            }
            else if (returnTypeClass == Float.TYPE) {
                instruction = basicTypeTranslation(Float.class, Float.TYPE,
                                                   "getFloat", instructionFactory);
            }
            else if (returnTypeClass == Double.TYPE) {
                instruction = basicTypeTranslation(Double.class, Double.TYPE,
                                                   "getDouble", instructionFactory);
            }
            else if (returnTypeClass == Short.TYPE) {
                instruction = basicTypeTranslation(Short.class, Short.TYPE,
                                                   "getShort", instructionFactory);
            }
            else if (returnTypeClass == Character.TYPE) {
                instruction = basicTypeTranslation(Character.class, Character.TYPE,
                                                   "getCharacter", instructionFactory);
            }
            else if (returnTypeClass == Byte.TYPE) {
                instruction = basicTypeTranslation(Byte.class, Byte.TYPE,
                                                   "getByte", instructionFactory);
            }
            else if (returnTypeClass == Long.TYPE) {
                instruction = basicTypeTranslation(Long.class, Long.TYPE,
                                                   "getLong", instructionFactory);
            }
            else if (returnTypeClass == Void.TYPE) {
                instruction = new ACONST_NULL();
            }
            Assert.that(instruction != null,
                "unsupported primtive return type encountered: %s", returnTypeClass);
            instructionList.append(instruction);
        }
        instructionList.append(new ARETURN());
        MethodGen methodGen = new MethodGen(Constants.ACC_PUBLIC,
                                            Type.OBJECT,
                                            GetterArgTypesArray,
                                            GetterArgNamesArray,
                                            "getValue",
                                            newClassName,
                                            instructionList,
                                            constantPoolGen);
        methodGen.setMaxStack();
        methodGen.setMaxLocals();

        return methodGen.getMethod();
    }

    private static org.apache.bcel.classfile.Method handleReturnAndGenerateSetterMethod (
    String newClassName, InstructionList instructionList, ConstantPoolGen constantPoolGen)
    {
        instructionList.append(new RETURN());
        MethodGen methodGen = new MethodGen(Constants.ACC_PUBLIC,
                                            Type.VOID,
                                            SetterArgTypesArray,
                                            SetterArgNamesArray,
                                            "setValue",
                                            newClassName,
                                            instructionList,
                                            constantPoolGen);
        methodGen.setMaxStack();
        methodGen.setMaxLocals();
        return methodGen.getMethod();
    }

    private static void pushAndConvertValueArg (InstructionList instructionList,
        InstructionFactory instructionFactory, Class valueTypeClass, Type valueType)
    {
        instructionList.append(new ALOAD(2));
        if (valueTypeClass.isPrimitive()) {
            if (valueTypeClass == Boolean.TYPE) {
                convertToPrimitiveType("getBoolean", valueType, instructionList,
                                       instructionFactory);
            }
            else if (valueTypeClass == Integer.TYPE) {
                convertToPrimitiveType("getInt", valueType, instructionList,
                                       instructionFactory);
            }
            else if (valueTypeClass == Float.TYPE) {
                convertToPrimitiveType("getFloat", valueType, instructionList,
                                       instructionFactory);
            }
            else if (valueTypeClass == Double.TYPE) {
                convertToPrimitiveType("getDouble", valueType, instructionList,
                                       instructionFactory);
            }
            else if (valueTypeClass == Short.TYPE) {
                convertToPrimitiveType("getShort", valueType, instructionList,
                                       instructionFactory);
            }
            else if (valueTypeClass == Character.TYPE) {
                convertToPrimitiveType("getChar", valueType, instructionList,
                                       instructionFactory);
            }
            else if (valueTypeClass == Byte.TYPE) {
                convertToPrimitiveType("getByte", valueType, instructionList,
                                       instructionFactory);
            }
            else if (valueTypeClass == Long.TYPE) {
                convertToPrimitiveType("getLong", valueType, instructionList,
                                       instructionFactory);
            }
        }
        else {
            instructionList.append(
                instructionFactory.createCheckCast((ReferenceType)valueType));
        }
    }

    private static CompiledAccessor generateClassAndGetInstance (ClassGen classGen,
                                                                 String newClassName,
                                                                 ProtectionDomain protectionDomain)
    {
        try {
            JavaClass javaClass = classGen.getJavaClass();
            byte[] classBytes = javaClass.getBytes();
            Class newClass = ByteArrayClassLoader.loadClass(newClassName, classBytes, protectionDomain);
            return (CompiledAccessor)newClass.newInstance();
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new WrapperRuntimeException(illegalAccessException);
        }
        catch (InstantiationException instantiationException) {
            throw new WrapperRuntimeException(instantiationException);
        }
    }

    //////////////////////////////
    // Convert Primitve to Object
    //////////////////////////////
    public static Integer getInteger (int intValue)
    {
        return ariba.util.core.Constants.getInteger(intValue);
    }

    public static Byte getByte (byte byteValue)
    {
        return new Byte(byteValue);
    }

    public static Character getCharacter (char charValue)
    {
        return new Character(charValue);
    }

    public static Double getDouble (double doubleValue)
    {
        return new Double(doubleValue);
    }

    public static Float getFloat (float floatValue)
    {
        return new Float(floatValue);
    }

    public static Long getLong (long longValue)
    {
        return ariba.util.core.Constants.getLong(longValue);
    }

    public static Boolean getBoolean (boolean booleanValue)
    {
        return ariba.util.core.Constants.getBoolean(booleanValue);
    }

    public static Short getShort (short shortValue)
    {
        return new Short(shortValue);
    }

    //////////////////////////////
    // Convert Object to Primitve
    //////////////////////////////
    public static boolean getBoolean (Object value)
    {
        return ((Boolean)value).booleanValue();
    }

    public static byte getByte (Object value)
    {
        return ((Number)value).byteValue();
    }

    public static char getChar (Object value)
    {
        return ((Character)value).charValue();
    }

    public static short getShort (Object value)
    {
        return ((Number)value).shortValue();
    }

    public static int getInt (Object value)
    {
        return ((Number)value).intValue();
    }

    public static long getLong (Object value)
    {
        return ((Number)value).longValue();
    }

    public static float getFloat (Object value)
    {
        return ((Number)value).floatValue();
    }

    public static double getDouble (Object value)
    {
        return ((Number)value).doubleValue();
    }

    ////////////////////////
    // ByteArrayClassLoader
    ////////////////////////
    public static class ByteArrayClassLoader extends ClassLoader
    {
        private static final ByteArrayClassLoader SharedByteArrayClassLoader =
            new ByteArrayClassLoader();
        private static final GrowOnlyHashtable _classes = new GrowOnlyHashtable();

        public ByteArrayClassLoader ()
        {
            super(CompiledAccessorFactory.class.getClassLoader());
        }

        public static Class loadClass (String className, byte[] bytes, 
                                       ProtectionDomain protectionDomain)
        {
            try {
                SharedByteArrayClassLoader._registerBytes(className, bytes, 
                                                          protectionDomain);
                Class newClass = SharedByteArrayClassLoader.loadClass(className);
                return newClass;
            }
            catch (ClassNotFoundException classNotFoundException) {
                throw new WrapperRuntimeException(classNotFoundException);
            }
        }

        private void _registerBytes (String className, byte[] bytes, 
                                     ProtectionDomain protectionDomain)
        {
            synchronized (_classes) {
                Assert.that(_classes.get(className) == null,
                            "attempt to reregister same class %s", className);
                ClassData data = new ClassData(bytes, protectionDomain);
                _classes.put(className, data);
            }
        }

        public Class findClass (String className)
        {
            Class classObject = null;
            synchronized (_classes) {
                Object item = _classes.get(className);
                if (item instanceof Class) {
                    classObject = (Class)item;
                }
                else if (item instanceof ClassData) {
                    ClassData data = (ClassData)item;
                    classObject = defineClass(className, 
                                              data.bytes, 0, data.bytes.length,
                                              data.domain);
                    _classes.put(className, classObject);
                }
                else {
                    throw new WrapperRuntimeException(
                        Fmt.S("Unrecognized type: %s", item.getClass().getName()));
                }
            }
            if (VerboseEnabled) {
                String message = Fmt.S("***** findClass: %s",
                                       classObject);
                Log.util.debug(message);
            }
            return classObject;
        }

        private class ClassData
        {
            public byte[] bytes;
            public ProtectionDomain domain;

            public ClassData (byte[] b, ProtectionDomain d)
            {
                bytes = b;
                domain = d;
            }
        }

    }
}
