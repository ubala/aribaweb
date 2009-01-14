package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.util.test.*;
import ariba.util.test.TestValidationParameter;

import java.lang.reflect.Method;
import java.util.List;

/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestInspectorLink.java#1 $
*/
public class TestInspectorLink
{
    private TestInspector _testInspector;
    private Object _annotatedItem;
    private Class _objectClass;

    public TestInspectorLink (TestInspector testInspector, Object annotatedItem)
    {
        _testInspector = testInspector;
        _annotatedItem = annotatedItem;

        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            Class[] types = method.getParameterTypes();
            if (types.length == 0) {
                _objectClass = method.getDeclaringClass();
            }
            else {
                _objectClass = types[0];
            }
        }
    }

    public TestInspectorLink (String encoded)
    {
        String[] parts = encoded.split(",");
        String annotatedClassName = parts[0];
        String annotatedMethodName = parts[1];
        String objectClassName = parts.length > 2 ? parts[2] : null;

        Class annotatedClass = ClassUtil.classForName(annotatedClassName);
        try {
            if (objectClassName == null) {
                _objectClass = annotatedClass;
                _annotatedItem = annotatedClass.getMethod(annotatedMethodName);
            }
            else {
                _objectClass = ClassUtil.classForName(objectClassName);
                _annotatedItem = annotatedClass.getMethod(annotatedMethodName, _objectClass);
            }
        }
        catch (NoSuchMethodException ex) {
            Assert.assertNonFatal(false, ex.toString());
        }
    }

    public boolean isValid ()
    {
        boolean valid = true;
        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            if (method.getReturnType() != List.class) {
                valid = false;
            }
            if (valid) {
                Class[] types = method.getParameterTypes();
                if (types.length > 1) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    public String getObjectClassName ()
    {
        return _objectClass.getName();
    }

    public Class getObjectClass ()
    {
        return _objectClass;
    }

    public String getInspectorClassName ()
    {
        String name = null;
        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            name = method.getDeclaringClass().getName();
        }
        return name;
    }

    public String getInspectorName ()
    {
        String name = null;
        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            name = method.getName();
        }
        return name;
    }

    public List<TestValidationParameter> invoke (AWRequestContext requestContext)
    {
        List<TestValidationParameter> result = null;
        TestContext testContext = TestContext.getTestContext(requestContext); 
        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            Object obj = getObjectToInvoke(requestContext, method, testContext);
            result = (List<TestValidationParameter>)AnnotationUtil.invokeMethod(requestContext, testContext, method, obj);
        }
        return result;
    }

    public String getEncodedString ()
    {
        FastStringBuffer encoded = new FastStringBuffer();
        encoded.append(getInspectorClassName());
        encoded.append(",");
        encoded.append(getInspectorName());
        if (!getObjectClassName().equals(getInspectorClassName())) {
            encoded.append(",");
            encoded.append(getObjectClassName());
        }
        return encoded.toString();
    }

    private Object getObjectToInvoke (AWRequestContext requestContext, Method method, TestContext testContext)
    {
        Class[] types = method.getParameterTypes();
        if (types.length == 0) {
            return testContext.get(method.getDeclaringClass());
        }
        else {
            return AnnotationUtil.getObjectToInvoke(requestContext, method);
        }
    }
}
