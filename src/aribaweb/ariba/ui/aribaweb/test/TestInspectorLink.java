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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestInspectorLink.java#9 $
*/
package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.ClassUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
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
    private String _validatorName;
    private String _humanFriendlyValidatorName;
    private Class _actualValidatedObjectClass;

    public static final String VALIDATION_LOAD_ERROR_HEADER =
            "Validation Failed: could not load the validator specified in the " +
            "test script (this is often caused by renaming/moving classes or " +
            "methods consult an application developer for help";

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

    public TestInspectorLink (TestValidator testInspector,
                              Object annotatedItem, String type)
    {
        _testInspector = testInspector;
        _annotatedItem = annotatedItem;
        _type = type;

        Log.aribaweb.debug(Fmt.S("TestInspectorLink: Invalid supertype class '%s'. " +
                "; classname: %s; name: %s; superType: %s; description: %s; annotatedItem: %s; type: %s.",new Object[]{
                testInspector.superType(), testInspector.classname(), testInspector.name(), testInspector.superType(),
                testInspector.description(), ((annotatedItem != null)?annotatedItem.toString():"null"),
                type}));

        if (_annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            Class[] types = method.getParameterTypes();
            types = TestLinkHolder.filterInternalParameters(types);
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
                for (Class type: types) {
                    if (!TestLinkHolder.isInternalParameter(type)) {
                        _objectClass = type;
                        break;
                    }
                }
            }
        }
    }

    /**
     * @return the class that was actually validated. This may have been a subclass of the object
     * defined for the validator.
     */
    public Class getActualClassValidated ()
    {
        if (_actualValidatedObjectClass != null) {
            return _actualValidatedObjectClass;
        }
        else {
            return _objectClass;
        }
    }

    private void decodeFromString (String encoded) throws ValidatorLoadException
    {
        String[] parts = encoded.split(",");
        if (parts[0].equals(APP_ENCODING_FLAG)) {
            _objectClass = ClassUtil.classForName(parts[1]);
            if (_objectClass == null) {
                throw new ValidatorLoadException (
                    Fmt.S(VALIDATION_LOAD_ERROR_HEADER +
                          " Test Expected Application Specific (e.g. AML group) " +
                          "validator on the class %s and could not load the " +
                          "specified class.", parts[1]));
            }
            _appSpecificValidatorName = parts[2];
        }
        else {
            String annotatedClassName = null;
            String annotatedMethodName = null;
            String objectClassName = null;
            String actualValidatedObjectClassName = null;
            if (parts.length < 4) {
                // use old encoding.
                annotatedClassName = parts[0];
                annotatedMethodName = parts[1];
                objectClassName = parts.length > 2 ? parts[2] : null;
            }
            else {
                // using new encoding.  In new encoding we always have 4 fields:
                // annoteadedclass, method, objectclass, validatedobjectclass.
                // these will be "null" if appropriate.
                annotatedClassName = parts[0];
                annotatedMethodName = parts[1];
                objectClassName = parts[2].equals("null") ? null : parts[2];
                actualValidatedObjectClassName =
                        parts[3].equals("null") ? null : parts[3];
            }

            Class annotatedClass = ClassUtil.classForName(annotatedClassName);
            if (annotatedClass == null) {
                if (objectClassName == null) {
                    throw new ValidatorLoadException(
                        constructNonAppSpecificError(annotatedClassName,
                                annotatedMethodName, annotatedClassName,
                                actualValidatedObjectClassName,
                                Fmt.S("Could not load specified class: %s",
                                        annotatedClassName)));
                }
                else {
                    throw new ValidatorLoadException(
                        constructNonAppSpecificError(annotatedClassName,
                                annotatedMethodName, objectClassName,
                                actualValidatedObjectClassName,
                                Fmt.S("Could not load specified class: %s",
                                        annotatedClassName)));
                }
            }
            if (objectClassName == null) {
                _objectClass = annotatedClass;
                _annotatedItem = getBestMethod (annotatedClass, annotatedMethodName, null);
                if (_annotatedItem == null) {
                    throw new ValidatorLoadException (
                        constructNonAppSpecificError(annotatedClassName,
                                annotatedMethodName, annotatedClassName,
                                actualValidatedObjectClassName,
                                Fmt.S("Could not find the specified method:%s",
                                        annotatedMethodName)));
                }
            }
            else {
                _objectClass = ClassUtil.classForName(objectClassName);
                if (_objectClass == null) {
                    throw new ValidatorLoadException (
                        constructNonAppSpecificError(annotatedClassName,
                                annotatedMethodName, objectClassName,
                                actualValidatedObjectClassName,
                                Fmt.S("Could not load specified class: %s",
                                        objectClassName)));
                }
                _annotatedItem = getBestMethod(annotatedClass, annotatedMethodName,
                            _objectClass);
                if (_annotatedItem == null) {
                    throw new ValidatorLoadException (
                        constructNonAppSpecificError(annotatedClassName,
                                annotatedMethodName, objectClassName,
                                actualValidatedObjectClassName,
                                Fmt.S("Could not find the method: %s",
                                        annotatedMethodName)));
                }
            }
            if (actualValidatedObjectClassName != null) {
                _actualValidatedObjectClass =
                        ClassUtil.classForName(actualValidatedObjectClassName);
                if (_actualValidatedObjectClass == null) {
                    throw new ValidatorLoadException(
                        constructNonAppSpecificError(annotatedClassName,
                                annotatedMethodName, objectClassName,
                                actualValidatedObjectClassName,
                                Fmt.S("Could not load specified class: %s",
                                        actualValidatedObjectClassName)));
                }
            }
        }
    }

    private Method getBestMethod (Class annotatedClass, String methodName,
                                  Class argumentClass)
    {
        Class currentArgumentClass = argumentClass;
        Method method = getMethodWithInternalParams(methodName, annotatedClass, currentArgumentClass);
        while (method == null && currentArgumentClass != null) {
            method = getMethodWithInternalParams(methodName, annotatedClass, currentArgumentClass);
            currentArgumentClass = currentArgumentClass.getSuperclass();
        }

        if (method == null && argumentClass != null) {
            // lets search the interfaces then.
            currentArgumentClass = argumentClass;
            while (method == null && currentArgumentClass != null) {
                Class[] interfaces = currentArgumentClass.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    method = getMethodWithInternalParams(methodName, annotatedClass, interfaces[i]);
                    if (method != null) {
                        break;
                    }
                }
                currentArgumentClass = currentArgumentClass.getSuperclass();
            }
        }
        return method;
    }

    private Method getMethodWithInternalParams(String methodName, Class annotatedClass, Class argumentClass) {
        Method method = null;
        Method[] methods = annotatedClass.getMethods();
        for (Method m : methods) {
            //If the method name matches, check for the parameters
            if (m.getName().equals(methodName)) {
                Class[] methodParams = TestLinkHolder.filterInternalParameters(m.getParameterTypes());
                if (argumentClass != null) {
                    if (methodParams.length == 1 &&
                        methodParams[0].equals(argumentClass)) {
                        method = m;
                        break;
                    }
                } else {
                    if (methodParams.length == 0) {
                        method = m;
                        break;
                    }
                }
            }
        }
        return method;
    }

    private String constructNonAppSpecificError (String classWithMethod,
                                               String methodName,
                                               String classToBeValidated,
                                               String actualClassValidated,
                                               String error)
    {
        String errorMessage = Fmt.S(VALIDATION_LOAD_ERROR_HEADER + "<br/>" +
            "Method Name                   : %s<br/>" +
            "Class with Method             : %s<br/>" +
            "Class Defined to be Validated : %s<br/>" +
            "Class Actually Validated      : %s<br/>" +
            "Problem                       : %s<br/><br/>",
            methodName, classWithMethod, classToBeValidated, actualClassValidated, error);
        return errorMessage;
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
                types = TestLinkHolder.filterInternalParameters(types);
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
        if (_annotatedItem != null && _annotatedItem.getClass() == Method.class) {
            Method method = (Method)_annotatedItem;
            name = method.getDeclaringClass().getName();
        }
        return name;
    }

    public String getInspectorName ()
    {
        if (_validatorName == null) {
            if (_appSpecificValidatorName != null) {
                _validatorName = _appSpecificValidatorName;
            }
            else if (_annotatedItem.getClass() == Method.class) {
                Method method = (Method)_annotatedItem;
                _validatorName = method.getName();
            }
        }
        return _validatorName;
    }

    public String getUserFriendlyValidatorName ()
    {
        if (_humanFriendlyValidatorName == null) {
            String refinedString = removeUselessLeadingStrings(getInspectorName());
            _humanFriendlyValidatorName = decamelize(refinedString);
        }
        return _humanFriendlyValidatorName;
    }

    public String getInspectorSecondaryName ()
    {
        String name = getInspectorName();
        if (_testInspector != null) {
            String description = _testInspector.description();
            if (!StringUtil.nullOrEmptyOrBlankString(description)) {
                name = name + ": " + description;
            }
        }

        return name;
    }

    public String getAppSpecificInspectorName ()
    {
        return _appSpecificValidatorName;
    }

    public List<TestValidationParameter> invoke (AWRequestContext requestContext,
                                                 Object objToValidate)
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
                            requestContext, testContext, method, obj, objToValidate);
            }
        }

        return result;
    }

    public List<TestValidationParameter> invoke (AWRequestContext requestContext)
    {
        return invoke(requestContext, null);
    }

    public String getEncodedString (String validatedObjectType)
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
            else {
                encoded.append(",");
                encoded.append("null");
            }
            if (validatedObjectType != null) {
                encoded.append(",");
                encoded.append(validatedObjectType);
            }
            else {
                encoded.append(",");
                encoded.append("null");
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
            throws ValidatorLoadException
    {
        TestInspectorLink newLink = new TestInspectorLink();
        newLink.decodeFromString(encoding);
        return newLink;
    }

    static String decamelize (String string)
    {
        FastStringBuffer buf = new FastStringBuffer();
            int lastUCIndex = -1;
        for (int i=0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i-1 != lastUCIndex) {
                    buf.append(" ");
                }
                lastUCIndex = i;
            }
            else if (Character.isLowerCase(c)) {
                if (i==0) {
                    c = Character.toUpperCase(c);
                }
            }
            else if (c == '_') {
                c = ' ';
            }
            buf.append(c);
        }
        return buf.toString();
    }

    private static final String[] USELESS_STRINGS = {
            "validate",
            "validation",
            "inspect"
    };

    private static String removeUselessLeadingStrings (String string)
    {
        for (int i = 0; i < USELESS_STRINGS.length; i++) {
            if (string.length() > USELESS_STRINGS[i].length()) {
                String leadingPart = string.substring(0,USELESS_STRINGS[i].length());
                if (leadingPart.equalsIgnoreCase(USELESS_STRINGS[i])) {
                    return string.substring(USELESS_STRINGS[i].length(), string.length());
                }
            }
        }
        return string;
    }
}
