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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestInspectorLink.java#4 $
*/
package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.StringUtil;
import ariba.util.test.TestValidationParameter;
import ariba.util.test.TestValidator;

import java.lang.reflect.Method;
import java.util.List;

public class TestInspectorLink
{
    private TestValidator _testInspector;
    private Object _annotatedItem;
    private Class _objectClass;
    private String _type;
    
    // this is used to store the descriptionof the AML Group or for Network the
    // EOF Equivalent.
    private String _appSpecificValidatorName;
    private static final String APP_ENCODING_FLAG = "APP_SPECIFIC";

    private TestInspectorLink ()
    {
        // empty constructor.  Only used internally to decode from a String.
    }

    public TestInspectorLink (String appSpecificValidatorName, Class classToValidate)
    {
        _appSpecificValidatorName = appSpecificValidatorName;
        _objectClass = classToValidate;
    }

    public TestInspectorLink (TestValidator testInspector, Object annotatedItem, String type)
    {
        _testInspector = testInspector;
        _annotatedItem = annotatedItem;
        _type = type;

        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            Class[] types = method.getParameterTypes();
            if (types.length == 1) {
                Class paramClass = types[0];
                Class typeClass = !StringUtil.nullOrEmptyOrBlankString(_type) ?
                        ClassUtil.classForName(_type) : null;
                if (typeClass != null && paramClass.isAssignableFrom(typeClass)) {
                    _objectClass = typeClass;
                }
                else {
                    _objectClass = paramClass;
                }
            }
            else {
                _objectClass = method.getDeclaringClass();
            }
        }
    }

    public TestInspectorLink (TestValidator testInspector, Object annotatedItem)
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

    private void decodeFromString (String encoded)
    {
        String[] parts = encoded.split(",");
        if (parts[0].equals(APP_ENCODING_FLAG)) {
            _objectClass = ClassUtil.classForName(parts[1]);
            _appSpecificValidatorName = parts[2];
        }
        else {
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
                    _annotatedItem = annotatedClass.getMethod(annotatedMethodName,
                                                              _objectClass);
                }
            }
            catch (NoSuchMethodException ex) {
                Assert.assertNonFatal(false, ex.toString());
            }
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
        if (_appSpecificValidatorName != null) {
            name = _appSpecificValidatorName;
        }
        else if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            name = method.getName();
        }
        return name;
    }

    public String getAppSpecificInspectorName ()
    {
        return _appSpecificValidatorName;
    }

    public List<TestValidationParameter> invoke (AWRequestContext requestContext)
    {
        List<TestValidationParameter> result = null;
        if (_appSpecificValidatorName != null) {
            TestSessionSetup testSessionSetup =
                    TestLinkManager.instance().getTestSessionSetup();
            result = testSessionSetup.invokeValidator(this, requestContext);
        }
        else {
            TestContext testContext = TestContext.getTestContext(requestContext);
            if (_annotatedItem.getClass() == Method.class) {
                Method method = (Method)_annotatedItem;
                Object obj = getObjectToInvoke(requestContext, method, testContext);
                result =
                    (List<TestValidationParameter>)AnnotationUtil.invokeMethod(
                            requestContext, testContext, method, obj);
            }
        }
            
        return result;
    }

    public String getEncodedString ()
    {
        FastStringBuffer encoded = new FastStringBuffer();
        if (_appSpecificValidatorName != null) {
            encoded.append(APP_ENCODING_FLAG);
            encoded.append(",");
            encoded.append(getObjectClassName());
            encoded.append(",");
               encoded.append(_appSpecificValidatorName);
        }
        else {
            encoded.append(getInspectorClassName());
            encoded.append(",");
            encoded.append(getInspectorName());
            if (!getObjectClassName().equals(getInspectorClassName())) {
                encoded.append(",");
                encoded.append(getObjectClassName());
            }
        }
        return encoded.toString();
    }

    private Object getObjectToInvoke (AWRequestContext requestContext, Method method,
                                      TestContext testContext)
    {
        Class[] types = method.getParameterTypes();
        if (types.length == 0) {
            return testContext.get(method.getDeclaringClass());
        }
        else {
            return AnnotationUtil.getObjectToInvoke(requestContext, method);
        }
    }

    public static TestInspectorLink decodeTestInspectorLink (String encoding)
    {
        TestInspectorLink newLink = new TestInspectorLink();
        newLink.decodeFromString(encoding);
        return newLink;
    }
    
}
