/*
    Copyright 1996-2011 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/JavaTypeProvider.java#20 $
*/

package ariba.util.fieldtype;

import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.SetUtil;
import ariba.util.core.FastStringBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import ariba.util.log.Log;
import ariba.util.fieldvalue.FieldValueAccessorUtil;


/**
    @aribaapi private
*/
public class JavaTypeProvider extends TypeProvider
{
    // The mapping table explicit mapping between types beyond the Java
    // referencing widening.  The first entry is the convertTo type and
    // the second entry is the convertFrom type.
    protected static final String[][]  _conversionTable =
        {{String.class.getName(), Character.class.getName()},
         {String.class.getName(), Character.TYPE.getName()}};

    /**
     * A character used in the class name to indicate the dimension of an
     * array element.
     */
    static final char ArrayDimensionIndicator = '[';

    private static JavaTypeProvider DefaultTypeProvider = new JavaTypeProvider();
    private static Map _classMap = MapUtil.map();
    private static Map _safeJavaClassMap = MapUtil.map();

    static {
       populateSafeClassAlias();
    }

    private JavaTypeProvider ()
    {
        super(JavaTypeProviderId);
    }

    public static JavaTypeProvider instance ()
    {
        return DefaultTypeProvider;
    }

    public TypeInfo getTypeInfo (String name)
    {
        TypeInfo info = (TypeInfo)_classMap.get(name);
        if (info == null) {
            synchronized(this) {
                info = (TypeInfo)_classMap.get(name);
                if (info == null) {
                    Class classForName = ClassUtil.classForName(name, false);
                    if (classForName != null) {
                         info = createTypeInfo(classForName);
                         _classMap.put(name, info);
                    }
                    else {
                        info = getAliasedClass(this, name);
                    }
                }
            }
        }

        return (info != null ?
                createContainerTypeIfNecessary((JavaTypeInfo)info) :
                null);
    }

    public TypeInfo getTypeInfo (Class cls)
    {
        String name = cls.getName();
        TypeInfo info = getTypeInfo(name);
        if (info == null) {
            synchronized(this) {
                 info = createTypeInfo(cls);
                // Todo: invalidate on reload?
                // In the event that this class came from an alternate class loader
                // and may be reloaded (e.g. in Groovy) we may end up with an ambiguous or
                // stale class under this name...
                 _classMap.put(name, info);
            }
        }

        return (info != null ?
                createContainerTypeIfNecessary((JavaTypeInfo)info) :
                null);
    }

    private TypeInfo createTypeInfo (Class classObj)
    {
        return new JavaTypeInfo(classObj);
    }

    private TypeInfo createContainerTypeIfNecessary (JavaTypeInfo type)
    {
        Class collectionClass = java.util.Collection.class;
        boolean isContainerType =
                collectionClass.isAssignableFrom(type._proxiedClass);

        if (isContainerType) {
            TypeInfo objType = getTypeInfo("java.lang.Object");
            Assert.that(objType != null, "Fail to find type info for 'java.lang.Object'");
            return new ContainerTypeInfo(type, objType);
        }

        TypeInfo arrayType = createArrayTypeInfoIfNecessary(type);
        return (arrayType != null ? arrayType : type);
    }

    /**
     * Create a <code>ContainerTypeInfo</code> if the type is an array type.
     * @param type - the name of the array class
     * @return the <code>TypeInfo</code> for the array class.  If the
     * name does not corresponding to an array class, return null.
     */
    protected TypeInfo createArrayTypeInfoIfNecessary (JavaTypeInfo type)
    {
        Class proxiedClass = type.getProxiedClass();
        if (proxiedClass == null || !proxiedClass.isArray()) {
            return null;
        }

        String name = type.getName();
        int  index = 0;
        char current = name.charAt(index);
        while (index < name.length() && current == ArrayDimensionIndicator) {
            current = name.charAt(++index);
        }

        if (index > 0 && index < name.length()) {
            if ((name.length() - (index+1) < 2) || current != 'L') {
                return null;
            }

            String elementTypeName = name.substring(index+1,name.length()-1);
            TypeInfo elementInfo = getTypeInfo(elementTypeName);

            if (elementInfo != null) {
                TypeInfo arrayInfo = new JavaArrayTypeInfo(
                                                     proxiedClass, index+1);

                return new ContainerTypeInfo(arrayInfo, elementInfo);
            }
        }

        return null;
    }

    /**
     * Populate the cache with safe java alias
     */
    private static void populateSafeClassAlias ()
    {
        Set safeClassNames = SafeJavaRepository.getInstance().getAllSafeClassNames();
        Iterator iter = safeClassNames.iterator();
        while (iter.hasNext()) {
            String name = (String)iter.next();
            String[] elements = StringUtil.delimitedStringToArray(name, '.');
            if (elements.length > 0) {
                String shortName = elements[elements.length-1];
                if (!shortName.equals(name)) {
                    // Is this classname has a known short name defined in
                    // ClassAliasRepository? If so, then skip this classname.
                    String knownShortName =
                            ClassAliasRepository.getInstance().getAliasForClassName(name);
                    if (StringUtil.nullOrEmptyOrBlankString(knownShortName)) {
                        // If there is no other class using this short name,
                        // put it in the class map.
                        if (_safeJavaClassMap.get(shortName) == null) {
                            _safeJavaClassMap.put(shortName, name);
                        }
                        else {
                            // If there is another class using this shortname,
                            // then assert.
                            Assert.that(true, Fmt.S("%s%s%s",
                                "The shortname '" + shortName + "' is ambiguous.",
                                " It is being defined in classes '" + name + "'",
                                " and '" + _safeJavaClassMap.get(shortName) + "'.",
                                " Please register the proper short name in platform/util/expr/fieldtype/ClassAlias.csv."));
                        }
                    }
                }
            }
        }
    }

    protected static TypeInfo getAliasedClass (TypeRetriever retriever, String name)
    {
        String className =
                ClassAliasRepository.getInstance().getClassNameForAlias(name);
        if (!StringUtil.nullOrEmptyOrBlankString(className) &&
            !className.equals(name)) {
            return retriever.getTypeInfo(className);
        }
        className = (String)_safeJavaClassMap.get(name);
        if (!StringUtil.nullOrEmptyOrBlankString(className) &&
            !className.equals(name)) {
            return retriever.getTypeInfo(className);
        }
        return null;
    }

    //---------------------------------------------------------------------
    // JavaTypeInfo

    /**
     * Subclass of <code>TypeInfo</code> for Java class.
     */

    public static class JavaTypeInfo implements TypeInfo, PropertyResolver
    {

        protected Class   _proxiedClass;
        protected Map<String,FieldInfo>   _fieldInfos;
        protected Map<String,JavaMethodInfo>   _methodInfos;
        protected boolean _hasMethodInfoLoaded = false;
        protected boolean _isJavaObjectType;
        /**
         * If this variable is not null, that means we are pseudo TypeInfo
         * and get all fields and methods from _proxiedClass, but behave as if
         * they come from _pseudoClass
         */
        private TypeInfo _pseudoClass = null;

        JavaTypeInfo ()
        {
            _fieldInfos = MapUtil.map();
            _methodInfos = MapUtil.map();
        }

        JavaTypeInfo (Class proxiedClass)
        {
            this();
            setProxiedClass(proxiedClass);
        }

        /**
         * For FMD classes, implementation would be FlexMasterData java type.
         * However class type should be from classMeta e.g. vrealm.fmd.MYFMD.
         * This method narrows the newly created JavaTypeInfo to the type passed in the
         * argument. 
         * @param pseudoClass has TypeInfo for vrealm.fmd.MYFMD
         * @return
         */
        public JavaTypeInfo castToPseudoType (TypeInfo pseudoClass)
        {
            JavaTypeInfo ret = new JavaTypeInfo(this._proxiedClass);
            ret._pseudoClass = pseudoClass;
            return ret;
        }

        public String getName ()
        {
            return _proxiedClass.getName();
        }

        public String getImplementationName ()
        {
            return getName();
        }

        public Class getProxiedClass ()
        {
            return _proxiedClass;
        }

        public void setProxiedClass (Class proxiedClass)
        {
            _proxiedClass = proxiedClass;
            _isJavaObjectType = (proxiedClass == Object.class);
        }

        /**
         * @see TypeInfo#getElementType()
         */
        public TypeInfo   getElementType ()
        {
            return null;
        }

        public boolean isAssignableFrom (TypeInfo other)
        {
            JavaTypeInfo otherType = getTypeInfoInProvider(other);
            if (otherType == null) {
                return false;
            }

            return _proxiedClass.isAssignableFrom(otherType._proxiedClass);
        }

        public boolean isCompatible (TypeInfo other)
        {
            TypeInfo otherType = getTypeInfoInProvider(other);
            if (otherType == null) {
                return false;
            }

            return equals(otherType) ||
                   isWideningTypeOf(otherType) ||
                   otherType.isWideningTypeOf(this);
        }

        public boolean isWideningTypeOf (TypeInfo other)
        {
            TypeInfo type = getTypeInfoInProvider(other);
            if (type == null) {
                return false;
            }

            // A type is a widening type from the other, if this type
            // is a assignable from the other type.
            if (isAssignableFrom(other)) {
                return true;
            }

            // if the this is not a superclass of "other", then check
            // the mapping table.
            for (int i=0; i < JavaTypeProvider._conversionTable.length; i++) {
                String[] entry = JavaTypeProvider._conversionTable[i];
                if (entry[0].equals(this.getName()) &&
                    entry[1].equals(other.getName())) {
                    return true;
                }
            }

            return false;
        }

        private JavaTypeInfo getTypeInfoInProvider (TypeInfo other)
        {
            if (other == null) {
                return null;
            }

            if (other instanceof NullTypeInfo) {
                return null;
            }

            JavaTypeInfo otherInfo = null;
            otherInfo = (JavaTypeInfo)(
                other instanceof JavaTypeInfo ?
                other :
                getTypeProvider().getTypeInfo(other.getImplementationName()));
            return otherInfo;
        }

        public FieldInfo getField (TypeRetriever retriever, String name)
        {
            try {
                FieldInfo fieldInfo = (FieldInfo)_fieldInfos.get(name);
                if (fieldInfo == null) {
                    synchronized (this) {
                        fieldInfo = (FieldInfo)_fieldInfos.get(name);
                        if (fieldInfo == null) {
                            Field field = _proxiedClass.getField(name);
                            fieldInfo = createFieldInfo(field);
                            _fieldInfos.put(name, fieldInfo);
                        }
                    }
                }
                return fieldInfo;
            }
            catch (Throwable e) {
                // ToDo
            }

            return null;
        }

        public MethodInfo getMethod (TypeRetriever retriever,
                                     String name,
                                     List parameters)
        {
            return getMethod(retriever, name, parameters, false);
        }

        public MethodInfo getMethod (TypeRetriever retriever,
                                     String name,
                                     List parameters,
                                     boolean staticOnly)
        {
            JavaMethodInfo methodInfo = null;
            try {
                if (!_hasMethodInfoLoaded) {
                    synchronized (this) {
                        if (!_hasMethodInfoLoaded) {
                            loadMethods(retriever);
                        }
                    }
                }
                String mungedName = JavaMethodInfo.getMungedMethodName(name, parameters);
                methodInfo = _methodInfos.get(mungedName);
                if (methodInfo == null || (staticOnly && !methodInfo.isStatic())) {
                    methodInfo = findAppropriateMethodInfo(retriever, name,
                            parameters, staticOnly);
                }
                if (_pseudoClass != null && methodInfo != null &&
                    methodInfo.isStatic() &&    
                    methodInfo.firstParameterIsClassName()) {
                    /*
                    Check if the static method takes first parameter as className.
                    If so create a copy and provide the first parameter information
                    */
                   methodInfo = methodInfo.createProxiedMethodInfo(_pseudoClass);
                }
            }
            catch (Throwable e) {
                Log.util.warn("getMethod failed",e);
            }
            return methodInfo;
        }

        public Set/*<MethodInfo>*/ getAllMethods (TypeRetriever retriever)
        {
            try {
                if (!_hasMethodInfoLoaded) {
                    synchronized (this) {
                        if (!_hasMethodInfoLoaded) {
                            loadMethods(retriever);
                        }
                    }
                }
                Set/*<MethodInfo>*/ result = SetUtil.set();
                result.addAll(_methodInfos.values());
                return result;
            }
            catch (Throwable e) {
                return null;
                // ToDo
                //e.printStackTrace();
            }
        }

        public int getAccessibility ()
        {
            int modifiers = _proxiedClass.getModifiers();
            return (Modifier.isPublic(modifiers) ?
                         TypeInfo.AccessibilitySafe :
                         TypeInfo.AccessibilityPrivate);
        }

        public String toString ()
        {
            return getName();
        }

        public PropertyResolver getPropertyResolver ()
        {
            return this;
        }

        public TypeInfo resolveTypeForName (TypeRetriever retriever, String name)
        {
            PropertyInfo property = resolvePropertyForName(retriever, name);
            if (property != null) {
                return property.getType(retriever);
            }

            return null;
        }

        public PropertyInfo resolvePropertyForName (TypeRetriever retriever,
                                                    String name)
        {
            FieldInfo field = getField(retriever, name);
            if (field != null) {
                return field;
            }

            loadMethods(retriever);
            Iterator methods = _methodInfos.keySet().iterator();
            while (methods.hasNext()) {
                String key = (String)methods.next();
                JavaMethodInfo method = (JavaMethodInfo)_methodInfos.get(key);
                String methodName = method.getMethod().getName();
                if (FieldValueAccessorUtil.matchForGetter(name, methodName)) {
                    return method;
                }
            }

            return null;
        }

        protected FieldInfo createFieldInfo (Field field)
        {
            return new JavaFieldInfo(this, field);
        }

        protected MethodInfo createMethodInfo (TypeRetriever retriever,
                                               Method method)
        {
            return new JavaMethodInfo(retriever, this, method, false);
        }

        protected MethodInfo createMethodInfo (TypeRetriever retriever,
                                               Method method,
                                               boolean isSafe)
        {
            return new JavaMethodInfo(retriever, this, method, isSafe);
        }

        private void loadMethods (TypeRetriever retriever)
        {
            if (!_hasMethodInfoLoaded) {
                Method[] methods = _proxiedClass.getMethods();
                if (methods != null) {
                    MethodSpecification specification =
                        SafeJavaRepository.getInstance().getAllSafeMethodsForClass(
                            _proxiedClass);
                    Set<JavaMethodInfo> firstParamClassNameMethods = SetUtil.set();
                    for (int i=0; i < methods.length; i++) {
                        Method method = methods[i];
                        boolean isSafe = (specification != null ?
                                          specification.isSatisfiedBy(method) :
                                          false);
                        JavaMethodInfo methodInfo = (JavaMethodInfo)
                            createMethodInfo(retriever, method, isSafe);
                        _methodInfos.put(
                            methodInfo.getMungedName(),
                            methodInfo);

                        if (methodInfo.firstParameterIsClassName()) {
                            firstParamClassNameMethods.add(methodInfo);
                        }
                    }
                    /*
                     Add another method which automatically adds classname parameter during runtime
                     but.. with the different signature of ignoring first param
                     Do this in another loop so that we overwrite any accidental declaration of similar
                     methods with our new mungedName
                    */
                    Iterator<JavaMethodInfo> mi = firstParamClassNameMethods.iterator();
                    while (mi.hasNext()) {
                        JavaMethodInfo oneInfo = mi.next();
                        String mungedName = oneInfo.getMungedMethodNameSansFirstParam();
                        if (mungedName!=null ) {
                            _methodInfos.put(mungedName,oneInfo);
                        }
                    }
                }
                _hasMethodInfoLoaded = true;
            }
        }


        public JavaMethodInfo findAppropriateMethodInfo (TypeRetriever retriever,
                                                      String name,
                                                      List parameters,
                                                      boolean staticOnly)
        {
            return findMethodInfo(retriever,
                    _methodInfos.values(), name,
                    parameters, staticOnly);
        }

        private JavaMethodInfo findMethodInfo (TypeRetriever retriever,
                                           Collection<JavaMethodInfo> methods,
                                           String name,
                                           List parameters,
                                           boolean staticOnly)
        {
            JavaMethodInfo mostSpecific = null;

            // Find matching method without narrowing or widening
            Iterator<JavaMethodInfo> iter = methods.iterator();
            while (iter.hasNext()) {
                JavaMethodInfo method = iter.next();
                if (isMatchingMethod(retriever, method, name, parameters,
                        false, false, staticOnly)) {
                    return method;
                }
            }

            // Find matching method with type widening
            iter = methods.iterator();
            while (iter.hasNext()) {
                JavaMethodInfo method = iter.next();
                if (isMatchingMethod(retriever, method, name, parameters,
                        true, false, staticOnly)) {
                    if (mostSpecific == null ||
                        isMoreSpecific(retriever, mostSpecific, method)) {
                        mostSpecific = method;
                    }
                }
            }

            if (mostSpecific != null) {
                return mostSpecific;
            }

            // Find metching method with type narrowing
            iter = methods.iterator();
            while (iter.hasNext()) {
                JavaMethodInfo method = iter.next();
                if (isMatchingMethod(retriever, method, name, parameters,
                        false, true, staticOnly)) {
                     if (mostSpecific == null ||
                        isMoreSpecific(retriever, mostSpecific, method)) {
                        mostSpecific = method;
                    }
                }
            }

            return mostSpecific;
        }

        private boolean isMatchingMethod (TypeRetriever retriever,
                                          MethodInfo method,
                                          String name,
                                          List parameters,
                                          boolean allowWidening,
                                          boolean allowNarrowing,
                                          boolean staticOnly)
        {
            // Name match test
            if (!method.getName().equals(name)) {
                return false;
            }

            if (staticOnly && !method.isStatic()) {
                return false;
            }

            // p1 - formal parameters
            // p2 - acutal parameters
            List p1 = method.getParameters(retriever);
            List p2 = parameters;

            // Number of Params match test
            int  ps1 = (p1 != null ? p1.size() : 0);
            int  ps2 = (p2 != null ? p2.size() : 0);
            if (ps1 != ps2) {
                return false;
            }

            // parameter type test.  Actual parameter (p2) must be assignable
            // to method signature.
            if (ps1 == 0 && ps2 == 0) {
                return true;
            }

            for (int i=0; i < p2.size(); i++) {
                TypeInfo p2Info = (TypeInfo)p2.get(i);
                TypeInfo p1Info = (TypeInfo)p1.get(i);
                if (p2Info instanceof NullTypeInfo &&
                    !PrimitiveTypeProvider.isSimplePrimitiveType(p1Info)) {
                    continue;
                }

                // Check for type equality
                if (p1Info.isWideningTypeOf(p2Info) &&
                    p2Info.isWideningTypeOf(p1Info)) {
                    continue;
                }

                // Check for widening
                if (allowWidening && p1Info.isWideningTypeOf(p2Info)) {
                    continue;
                }

                // If both P1 and P2 are primitives, check if it is possible
                // to do a implicit cast for narrowing conversion.
                if (allowNarrowing &&
                    PrimitiveTypeProvider.isSupportedType(p1Info.getName()) &&
                    PrimitiveTypeProvider.isSupportedType(p2Info.getName()) &&
                    PrimitiveTypeProvider.isWideningTypeOf(p2Info, p1Info)) {
                    continue;
                }

                return false;
            }

            return true;
        }

        private boolean isMoreSpecific (TypeRetriever retriever,
                                        MethodInfo method1,
                                        MethodInfo method2 )
        {
            List <TypeInfo> p1Types = method1.getParameters(retriever);
            List <TypeInfo> p2Types = method2.getParameters(retriever);

            for ( int index=0, count=p1Types.size(); index < count; ++index )
            {
                TypeInfo p1Info = p1Types.get(index);
                TypeInfo p2Info = p2Types.get(index);

                // P2 is more specific
                if (p1Info.isWideningTypeOf(p2Info)) {
                    return false;
                }

                // p1 is more specific
                if (p2Info.isWideningTypeOf(p1Info)) {
                    return true;
                }
            }

            // They are the same!  So the first is not more specific than the second.
            return false;
        }

        protected TypeProvider getTypeProvider ()
        {
            return JavaTypeProvider.instance();
        }
    }
    
    //---------------------------------------------------------------------
    // ArrayTypeInfo

    /**
    * Subclass of <code>TypeInfo</code> for array type.
    */
    static class JavaArrayTypeInfo extends JavaTypeInfo
    {
        protected int      _dimension;

        JavaArrayTypeInfo (Class proxiedClass, int dimension)
        {
            super(proxiedClass);
            _dimension = dimension;
        }

        public int getDimension ()
        {
            return _dimension;
        }

        public void setDimension (int dimension)
        {
            _dimension = dimension;
        }
    }

    //---------------------------------------------------------------------
    // JavaFieldInfo
    
    static class JavaFieldInfo implements FieldInfo
    {
        protected Field _proxiedField;
        protected TypeInfo _parentType;

        JavaFieldInfo (TypeInfo type, Field field)
        {
            _proxiedField = field;
            _parentType = type;
        }

        public String  getName ()
        {
            return _proxiedField.getName();
        }

        public TypeInfo    getType (TypeRetriever retriever)
        {
            Class fieldType = _proxiedField.getType();
            return retriever.getTypeInfo(fieldType.getName());
        }

        public TypeInfo getParentType ()
        {
            return _parentType;
        }

        public int getAccessibility ()
        {
            int modifiers = _proxiedField.getModifiers();
            return (Modifier.isPublic(modifiers) ?
                         TypeInfo.AccessibilitySafe :
                         TypeInfo.AccessibilityPrivate);
        }

        public String toString ()
        {
            return getName();
        }
    }

    public static class JavaMethodInfo implements MethodInfo,Cloneable
    {
        protected Method     _proxiedMethod;
        protected TypeInfo   _parentType;
        protected String     _mungedName;
        protected boolean    _isSafe;

        protected List /* <TypeInfo> */ _parametersType = null;
        protected boolean   _hasParametersLoaded = false;

        protected TypeInfo   _returnType = null;
        // Class on which I am a pseudo method e.g: vrealm_1.fmd_1.myMFD
        private String _proxiedClass = null;
        private  SafeMethodOptions _annotation;

        JavaMethodInfo (TypeRetriever retriever,
                        TypeInfo type,
                        Method method,
                        boolean isSafe)
        {
            _proxiedMethod = method;
            _parentType = type;
            _parametersType = getParameters(retriever);
            _returnType = getReturnType(retriever);
            _mungedName = getMungedMethodName(getName(), _parametersType);
            _isSafe = isSafe;
            _annotation = method.getAnnotation(SafeMethodOptions.class);
        }

       /**
        * Creates a new instance and fakes that methodInfo belongs to
        * a class sent in the argument. This is used to assign methods to
        * FMD classes. Depending upon the annotation about covariantReturn,
        * it also narrows down the return type of the method to the type
        * passed.
        * @param otherProxy expected to be not null, has type info for vrealm.fmd.MyFMD class
        */
        protected JavaMethodInfo createProxiedMethodInfo (TypeInfo otherProxy)
        {
            JavaMethodInfo ret = null;
            try {
                ret = (JavaMethodInfo)super.clone();
                ret._proxiedClass = otherProxy.getName();
                if (hasCovariantReturn()) {
                    //Change the return type based on annotation
                    ret._returnType = otherProxy;
                }
            }
            catch (CloneNotSupportedException e) {
                Assert.fail(e, "JavaMethodInfo should be clonable");
            }
            return ret;
        }

        /**
         * if annotation is set, returns the firstArgumentClassName
         * that was set. 
         * @return could be null
         */
        public String getFirstArgumentClassName ()
        {
            if (firstParameterIsClassName()) {
                return _proxiedClass;
            }
            return null;
        }

        public String getName ()
        {
            return _proxiedMethod.getName();
        }

        public String  getMungedName ()
        {
            return _mungedName;
        }

        public boolean isStatic ()
        {
            return Modifier.isStatic(_proxiedMethod.getModifiers());
        }

        public Method getMethod ()
        {
            return _proxiedMethod;
        }

        /**
         *
         * @param retriever
         * @return Return an empty list if there is no parameter.  If any
         * parameter type cannot be retrieved, return null.
         */
        public List /* <TypeInfo> */ getParameters (TypeRetriever retriever)
        {
            if (_parametersType == null && !_hasParametersLoaded) {
                Class[] classes = _proxiedMethod.getParameterTypes();
                List types = ListUtil.list();
                for (int i=0; i < classes.length; i++) {
                    TypeInfo type = retriever.getTypeInfo(classes[i].getName());
                    if (type != null) {
                        types.add(type);
                    }
                    else {
                        types = null;
                        break;
                    }
                }
                _hasParametersLoaded = true;
                _parametersType = types;
            }
            return _parametersType;
        }

        /**
         *
         * @param retriever
         * @return Return a void class if there is no return type.  If the
         * return type exists but cannot be found, then return null.
         */
        public TypeInfo getReturnType (TypeRetriever retriever)
        {
            if (_returnType == null) {
                Class type = _proxiedMethod.getReturnType();
                if (type != null) {
                    _returnType = retriever.getTypeInfo(type.getName());
                }
            }
            return _returnType;
        }

        public TypeInfo getType (TypeRetriever retriever)
        {
            return getReturnType(retriever);
        }

        public int getAccessibility ()
        {
            return (_isSafe ?
                         TypeInfo.AccessibilitySafe :
                         TypeInfo.AccessibilityPrivate);
        }

        public String toString ()
        {
            return getName();
        }

        public TypeInfo    getParentType ()
        {
            return _parentType;
        }

        private boolean hasCovariantReturn ()
        {
            if (_annotation !=null) {
                return _annotation.covariantReturn();
            }
            return false;
        }

        boolean firstParameterIsClassName ()
        {
            if (_annotation!=null && _annotation.firstParameterIsClassName()) {
                Class[] params = _proxiedMethod.getParameterTypes();
                if (params.length >0) {
                    if (params[0].equals(String.class)) {
                        return true;
                    }
                }
            }
            return false;
        }

        String getMungedMethodNameSansFirstParam ()
        {
            int size = _parametersType.size();
            if (size < 1 )return null;
            List params = Collections.EMPTY_LIST;
            if (size > 1) {
                params = ListUtil.list();
                ListUtil.copyInto(_parametersType,params,1,size-1);
            }
            String mungedName = getMungedMethodName(getName(),params);
            return mungedName;
        }

        static String getMungedMethodName (String methodName,
                                           List parameters)
        {
            FastStringBuffer buffer = new FastStringBuffer();
            buffer.append(methodName);
            if (!ListUtil.nullOrEmptyList(parameters)) {
                for (int i=0; i < parameters.size(); i++) {
                    TypeInfo paramType = (TypeInfo)parameters.get(i);
                    buffer.append('#');
                    buffer.append(paramType.getName());
                }
            }
            return buffer.toString();
        }

        static boolean hasNullParameter (List parameters)
        {
            if (!ListUtil.nullOrEmptyList(parameters)) {
                for (int i=0; i < parameters.size(); i++) {
                    TypeInfo paramType = (TypeInfo)parameters.get(i);
                    if (paramType instanceof NullTypeInfo) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
