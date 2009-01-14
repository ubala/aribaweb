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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/AnnotationUtil.java#2 $
*/

package ariba.ui.aribaweb.test;

import ariba.util.test.TestLink;
import ariba.util.test.TestStager;
import ariba.util.core.ClassUtil;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.util.AWGenericException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

public class AnnotationUtil
{
    static public String getAnnotationTypeList (Annotation annotation)
    {
        String typeList = null;
        if (TestLink.class.isAssignableFrom(annotation.annotationType())) {
            TestLink testLink = (TestLink)annotation;
            typeList = testLink.typeList();
        }
        else if (TestStager.class.isAssignableFrom(annotation.annotationType())) {
            TestStager testLink = (TestStager)annotation;
            typeList = testLink.typeList();
        }
        return typeList;
    }

    static public String getAnnotationSuperType (Annotation annotation)
    {
        String superType = null;
        if (TestLink.class.isAssignableFrom(annotation.annotationType())) {
            TestLink testLink = (TestLink)annotation;
            superType = testLink.superType();
        }
        else if (TestStager.class.isAssignableFrom(annotation.annotationType())) {
            TestStager testLink = (TestStager)annotation;
            superType = testLink.superType();
        }
        return superType;
    }

    static public String getAnnotationName  (Annotation annotation)
    {
        String name = null;
        if (TestLink.class.isAssignableFrom(annotation.annotationType())) {
            TestLink testLink = (TestLink)annotation;
            name = testLink.name();
        }
        else if (TestStager.class.isAssignableFrom(annotation.annotationType())) {
            TestStager testLink = (TestStager)annotation;
            name = testLink.name();
        }
        return name;
    }

    public static Object getObjectToInvoke (AWRequestContext requestContext, Method method)
    {
        Object obj = null;
        if (!Modifier.isStatic(method.getModifiers())) {
            obj = createObject(requestContext, method.getDeclaringClass());
        }
        return obj;
    }

    public static Object invokeMethod (AWRequestContext requestContext, TestContext testContext, Method method, Object onObject)
    {
        Class[] methodTypes = method.getParameterTypes();
        Object[] args = new Object[methodTypes.length];
        for (int i=0; i<methodTypes.length; i++) {
            if (AWRequestContext.class.equals(methodTypes[i])) {
                args[i] = requestContext;
            } else {
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
