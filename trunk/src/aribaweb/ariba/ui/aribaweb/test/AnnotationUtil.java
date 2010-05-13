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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/AnnotationUtil.java#12 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.test.TestDestager;
import ariba.util.test.TestPageLink;
import ariba.util.test.TestParam;
import ariba.util.test.TestStager;
import ariba.util.test.TestValidator;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;


public class AnnotationUtil
{
    static public String getAnnotationTypeList (Annotation annotation)
    {
        String typeList = null;
        if (TestPageLink.class.isAssignableFrom(annotation.annotationType())) {
            TestPageLink testLink = (TestPageLink)annotation;
            typeList = testLink.typeList();
        }
        else if (TestStager.class.isAssignableFrom(annotation.annotationType())) {
            TestStager testLink = (TestStager)annotation;
            typeList = testLink.typeList();
        }
        else if (TestDestager.class.isAssignableFrom(annotation.annotationType())) {
            TestDestager testLink = (TestDestager)annotation;
            typeList = testLink.typeList();
        }
        else if (TestValidator.class.isAssignableFrom(annotation.annotationType())) {
            TestValidator testValidator = (TestValidator)annotation;
            typeList = testValidator.typeList();
        }
        return typeList;
    }

    static public String getAnnotationSuperType (Annotation annotation)
    {
        String superType = null;
        if (TestPageLink.class.isAssignableFrom(annotation.annotationType())) {
            TestPageLink testLink = (TestPageLink)annotation;
            superType = testLink.superType();
        }
        else if (TestStager.class.isAssignableFrom(annotation.annotationType())) {
            TestStager testLink = (TestStager)annotation;
            superType = testLink.superType();
        }
        else if (TestDestager.class.isAssignableFrom(annotation.annotationType())) {
            TestDestager testLink = (TestDestager)annotation;
            superType = testLink.superType();
        }
        else if (TestValidator.class.isAssignableFrom(annotation.annotationType())) {
            TestValidator testValidator = (TestValidator)annotation;
            superType = testValidator.superType();
        }
        return superType;
    }

    static public String getAnnotationName  (Annotation annotation)
    {
        String name = null;
        if (TestPageLink.class.isAssignableFrom(annotation.annotationType())) {
            TestPageLink testLink = (TestPageLink)annotation;
            name = testLink.name();
        }
        else if (TestStager.class.isAssignableFrom(annotation.annotationType())) {
            TestStager testLink = (TestStager)annotation;
            name = testLink.name();
        }
        else if (TestDestager.class.isAssignableFrom(annotation.annotationType())) {
            TestDestager testLink = (TestDestager)annotation;
            name = testLink.name();
        }
        return name;
    }


    static public String getDescription  (Annotation annotation)
    {
        String name = null;
        if (TestPageLink.class.isAssignableFrom(annotation.annotationType())) {
            TestPageLink testLink = (TestPageLink)annotation;
            name = testLink.description();
        }
        else if (TestStager.class.isAssignableFrom(annotation.annotationType())) {
            TestStager testLink = (TestStager)annotation;
            name = testLink.description();
        }
        else if (TestDestager.class.isAssignableFrom(annotation.annotationType())) {
            TestDestager testLink = (TestDestager)annotation;
            name = testLink.description();
        }
        return name;
    }

    public static Object getObjectToInvoke (AWRequestContext requestContext,
                                            Method method)
    {
        Object obj = null;
        if (!Modifier.isStatic(method.getModifiers())) {
            obj = createObject(requestContext, method.getDeclaringClass());
        }
        return obj;
    }

    public static Object invokeMethod (AWRequestContext requestContext,
                                       TestContext testContext,
                                       Method method,
                                       Object onObject)
    {
        return invokeMethod(requestContext, testContext, method, onObject, null);
    }

    public static Object invokeMethod (AWRequestContext requestContext,
                                       TestContext testContext,
                                       Method method,
                                       Object onObject,
                                       Object objectForValidation)
    {
        Class[] methodTypes = method.getParameterTypes();
        Object[] args = new Object[methodTypes.length];
        for (int i=0; i<methodTypes.length; i++) {
            if (AWRequestContext.class.equals(methodTypes[i])) {
                args[i] = requestContext;
            }
            else if (TestContext.class.equals(methodTypes[i])) {
                args[i] = testContext;
            }
            else if (objectForValidation != null &&
                    methodTypes[i].isInstance(objectForValidation)) {
                args[i] = objectForValidation;
            }
            else {
                args[i] = testContext.get(methodTypes[i]);
            }
        }
        return invokeMethod(method, onObject, args);
    }

    public static Object invokeMethod (Method method,
                                       Object onObject,
                                       Object[] args)
    {
        try {
            return method.invoke(onObject, args);
        }
        catch (IllegalAccessException e) {
            throw new AWGenericException(e);
        }
        catch (InvocationTargetException e) {
            throw new AWGenericException (e);
        }
    }

    public static Object createObject (AWRequestContext requestContext, Class c)
    {
        Object obj;
        try {
            if (AWComponent.class.isAssignableFrom(c)) {
                // Do not strip the class path, it does no harm, and may disambiguate
                // situations, for example with MassEditTesting.java in collaborate.
                obj = requestContext.pageWithName(c.getName());
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

    public static void initializePageTestParams (AWRequestContext requestContext,
                                                 AWResponseGenerating page)
    {
        TestContext testContext = TestContext.getTestContext(requestContext); 
        // loop through all of the methods with TestParam Annotation and invoke
        // the with method passing the proper needed argument to the method.
        Map<Annotation, AnnotatedElement> annotations =
            TestLinkManager.instance().annotationsForClass(page.getClass().getName());
        Set keys = annotations.keySet();
        for (Object key : keys) {
            Annotation annotation = (Annotation)key;
            if (TestParam.class.isAssignableFrom(annotation.annotationType())) {
                Object ref = annotations.get(key);
                if (ref.getClass() == Method.class) {
                    AnnotationUtil.invokeMethod(requestContext, testContext,
                                                (Method)ref, page);
                }
            }
        }

    }
}
