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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestLinkHolder.java#1 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.ClassUtil;
import ariba.util.core.StringUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

public class TestLinkHolder
{
    private static String _DefaultFirstLevelCategory = "General";

    Annotation _annotation;
    Object _annotatedItem;
    String _type;

    String _displayName;
    String _secondaryName;
    
    String _firstLevelCategory;
    String _secondLevelCategory;

    boolean _hidden = false;
    
    public TestLinkHolder ()
    {
        _hidden = true;
    }

    public TestLinkHolder(Annotation annotation, Object annotatedItem)
    {
        init(annotation, annotatedItem, null);
    }

    public TestLinkHolder(Annotation annotation, Object annotatedItem, String type)
    {
        init(annotation, annotatedItem, type);
    }

    private void init (Annotation annotation, Object annotatedItem, String type)
    {
        _annotation = annotation;
        _annotatedItem = annotatedItem;
        _type = type;

        _displayName = computeDisplayName ();
        _secondaryName = computeSecondaryName();
        if (!StringUtil.nullOrEmptyOrBlankString(_type)) {
            _firstLevelCategory = categoryForClassName(_type, _DefaultFirstLevelCategory);
            _secondLevelCategory = _type;
        }
        else {
            if (_annotatedItem.getClass() == Method.class) {
                Method m = (Method) _annotatedItem;
                _firstLevelCategory = categoryForClassName(
                        m.getDeclaringClass().getName(), _DefaultFirstLevelCategory);
                _secondLevelCategory = m.getDeclaringClass().getName();
            }
            else if (_annotatedItem.getClass() == Class.class) {
                _firstLevelCategory = categoryForClassName(
                        ((Class)_annotatedItem).getName(), _DefaultFirstLevelCategory);
                _secondLevelCategory = ((Class)_annotatedItem).getName();
            }
        }
    }

    private String computeSecondaryName ()
    {
        String name = null;
        if (!StringUtil.nullOrEmptyOrBlankString(_type)) {
            if (_annotatedItem.getClass() == Method.class) {
                Method m = (Method) _annotatedItem;
                    name = m.getDeclaringClass().getName();
            }
        }
        return name;
    }
    private String computeDisplayName ()
    {
        String displayName = null;
        displayName = AnnotationUtil.getAnnotationName(_annotation);

        if (StringUtil.nullOrEmptyOrBlankString(displayName)) {
            if (_annotatedItem.getClass() == Method.class) {
                Method m = (Method) _annotatedItem;
                displayName = m.getName();
            }
            else if (_annotatedItem.getClass() == Class.class) {
                displayName = ClassUtil.stripPackageFromClassName(((Class) _annotatedItem).getName());
            }
        }
        return displayName;
    }

    public String getSecondLevelCategoryName ()
    {
        return _secondLevelCategory;
    }

    public String getFirstLevelCategoryName ()
    {
        return _firstLevelCategory;
    }

    public String getDisplayName ()
    {
        return _displayName;
    }

    public String getSecondaryName ()
    {
        return _secondaryName;
    }

    private static String categoryForClassName (String className, String defaultValue)
    {
        String categoryName = null;
        String[] strings = className.split("\\.");
        if (strings.length >= 2) {
            if (strings[0].equals("ariba")) {
                categoryName = strings[1];
            }
        }
        if (StringUtil.nullOrEmptyOrBlankString(categoryName)) {
            categoryName = defaultValue;
        }
        return categoryName;
    }

    public boolean isTestLink ()
    {
        return TestLink.class.isAssignableFrom(_annotation.annotationType());
    }

    public boolean isTestLinkParam ()
    {
        return TestLinkParam.class.isAssignableFrom(_annotation.annotationType());
    }

    public boolean isActive (AWRequestContext requestContext)
    {
        boolean active = true;
        if (requiresParam()) {
            if (_annotatedItem.getClass() == Method.class) {
                Method method = (Method) _annotatedItem;
                Class[] methodTypes = method.getParameterTypes();
                TestContext testContext = TestContext.getTestContext(requestContext);
                for (int i=0; i<methodTypes.length; i++) {
                    if (testContext.get(methodTypes[i]) == null) {
                        active = false;
                        break;
                    }
                }
            }
        }
        return active;    
    }

    public boolean isHidden ()
    {
        return _hidden;
    }
    public boolean isInteractive ()
    {
        boolean isInteractive = false;
        if (_annotatedItem.getClass() == Method.class) {
            Method m = (Method) _annotatedItem;
            if (AWComponent.class.isAssignableFrom(m.getReturnType())) {
                isInteractive = true;
            }
        }
        else {
            if (AWComponent.class.isAssignableFrom(_annotatedItem.getClass())) {
                isInteractive = true;
            }
        }
        return isInteractive;
    }

    public boolean requiresParam ()
    {
        boolean requiresParam = false;
        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method) _annotatedItem;
            Class[] methodTypes = method.getParameterTypes();
            if (methodTypes.length > 0) {
                if (StringUtil.nullOrEmptyOrBlankString(_type)) {
                    requiresParam = true;
                }
                else if (!(methodTypes.length == 1 && methodTypes[0] == String.class)){
                    requiresParam = true;                    
                }
            }
        }
        return requiresParam;
    }
    
    public AWResponseGenerating click (AWRequestContext requestContext)
    {
        TestContext testContext = TestContext.getTestContext(requestContext);
        if (isTestLink()) {
            // If this is a test link, then either call pageForName
            // otherwise, call pageForName, invoke the method specified.

            // After that set any @TestLinkParams that are specified on the page.
            AWResponseGenerating page = null;
            if (_annotatedItem.getClass() == Method.class) {
                Method m = (Method) _annotatedItem;
                Object obj = getObjectToInvoke(requestContext, m);
                page = (AWResponseGenerating)invokeMethod(testContext, m, obj);
            }
            else {
                String name = ClassUtil.stripPackageFromClassName(((Class) _annotatedItem).getName());
                page = requestContext.pageWithName(name);
            }
            initializeTestLinkParams(testContext, page);
            return page;
        }
        else {
            if (_annotatedItem.getClass() == Method.class) {
                Method m = (Method) _annotatedItem;
                Object obj = getObjectToInvoke(requestContext, m);
                Object res = invokeMethod(testContext, m, obj);
                if (res != null) {
                    testContext.put(res);
                }
            }
            return null;
        }
    }
    
    private void initializeTestLinkParams (TestContext testContext, Object page)
    {
        Class testClass;
        if (_annotatedItem.getClass() == Method.class) {
            Method m = (Method) _annotatedItem;
            testClass = m.getDeclaringClass();
        }
        else {
            testClass = _annotatedItem.getClass();
        }
        Map<Class, Object> annotations = TestLinkManager.instance().annotationsForClass(testClass.getName());
        Set keys = annotations.keySet();
        for (Object key : keys) {
            Annotation annotation = (Annotation)key;
            if (TestLinkParam.class.isAssignableFrom(annotation.annotationType())) {
                Object ref = annotations.get(key);
                if (ref.getClass() == Method.class) {
                    invokeMethod(testContext, (Method)ref, page);
                }
            }
        }
    }

    private Object getObjectToInvoke (AWRequestContext requestContext, Method method)
    {
        Object obj = null;
        if (!Modifier.isStatic(method.getModifiers())) {
            obj = createObject(requestContext, method.getDeclaringClass());
        }
        return obj;
    }
    private Object invokeMethod (TestContext testContext, Method method, Object onObject)
    {
        try {
            Class[] methodTypes = method.getParameterTypes();
            Object[] args = new Object[methodTypes.length];
            for (int i=0; i<methodTypes.length; i++) {
                args[i] = testContext.get(methodTypes[i]);
            }
            return method.invoke(onObject, args);
        }
        catch (IllegalAccessException e) {
            throw new AWGenericException (e);
        }
        catch (InvocationTargetException e) {
            throw new AWGenericException (e);
        }
    }

    private Object createObject (AWRequestContext requestContext, Class c)
    {
        Object obj;
        try {
            if (AWComponent.class.isAssignableFrom(c)) {
                String name = ClassUtil.stripPackageFromClassName(c.getName());
                obj = requestContext.pageWithName(name);
            }
            else {
                obj = c.newInstance();
            }
            if (AWDirectAction.class.isAssignableFrom(c)) {
                ((AWDirectAction)obj).init(requestContext);
            }
            return obj;
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new AWGenericException(illegalAccessException);
        }
        catch (InstantiationException instantiationException) {
            throw new AWGenericException(instantiationException);
        }
    }
}
