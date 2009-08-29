/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/test/TestValidationParameter.java#7 $
*/
package ariba.util.test;

import java.util.List;

/**
    A class to encapsulate the values returned by Validators within the
    Ariba UI Test Infrastructure.

    The class contains 3 key data items:<br/>
    <ol>
        <li/><b>name</b> will be displayed in the UI to describe to testers what the paramter represents.
        <li/><b>value</b> a string representation of the value, parameters must not contain any object Ids
        or other values which will change from test run to test run.
        <li><b>key</b> a string which will be used to match the parameter with values in future runs to
        check consistency of the application.  You should minimize changes to keys in order to avoid
        artificially breaking existing test cases.
    </ol>

    <em>Devlopers writing Validators don't need to worry about the encoding/decoding described next.</em><br/>
    The class also provides a variety of utility methods to help manage the encoding of parameters and
    lists of parameters into a string that is stored as part of the recorded Selenium test case.
    The String encoding of a list of validation parameters currently looks like:
    <pre>
        "ENCODING of VALIDATOR METHOD","key1","value1","key2","value2",...
    </pre>

    @see TestValidator
    @aribaapi private
*/
public class TestValidationParameter {
    private String _name;
    private Object _value;
    private String _key;
    private String _validatorToUseForLists;
    private boolean _isObjectList = false;
    private boolean _validateOrder;
    private boolean _isErrorValue = false;

    /**
     * Note that when using this constructor the key for this
     * parameter will be set to the same value as the name.  Use this only if the
     * the name will be stable enough from run to run to avoid test failuers.
     * If the name is a translated UI string that might change with translation revisions
     * or with UI cleanup then use the constructor with a key and provide a more stable key.
     * @param name a human friendly desription of the parameter.
     * @param value a string representation of the value - must not contain any text that
     *      would change between test runs (e.g. object Ids, timestamps).
     */
    public TestValidationParameter (String name, String value)
    {
        this(name, name, value);
    }

    /**
     * Construct a TestValidationParameter based on the provided data.
     * @param name a human friendly desription of the parameter.
     * @param key a stable string which will be used to match the parameter with values in future runs to
        check consistency of the application.  You should minimize changes to keys in order to avoid
        artificially breaking existing test cases.
     * @param value a string representation of the value - must not contain any text that
     *      would change between test runs (e.g. object Ids, timestamps).
     */
    public TestValidationParameter (String name, String key, String value)
    {
        _name = name;
        _key = key;
        _value = value;
    }

    public TestValidationParameter (String name, TestValidationParameterList value)
    {
        this(name, name, value);
    }

    public TestValidationParameter (String name, String key,
                                    TestValidationParameterList value)
    {
        _name = name;
        _key = key;
        _value = value;
    }

    public TestValidationParameter (String name, List<TestValidationParameter> value)
    {
        this(name, name, value);
    }

    public TestValidationParameter (String name, List objects,
                                    String validatorToUseOnObjects, boolean validateOrder)
    {
        this(name, name, objects, validatorToUseOnObjects, validateOrder);
    }

    public TestValidationParameter (String name, String key, List objects,
                                    String validatorToUseOnObjects, boolean validateOrder)
    {
        _name = name;
        _key = key;
        _value = objects;
        _validatorToUseForLists = validatorToUseOnObjects;
        _validateOrder = validateOrder;
        _isObjectList = true;
    }


    public TestValidationParameter (String name, String key,
                                    List<TestValidationParameter> value)
    {
        TestValidationParameterList list =
                TestValidationParameterList.createFromListOfParameters(value);
        _name = name;
        _key = key;
        _value = list;
    }

    public String getName ()
    {
        return _name;
    }

    public Object getValue ()
    {
        return _value;
    }

    public String getKey ()
    {
        return _key;
    }

    public boolean isList ()
    {
        return _value instanceof TestValidationParameterList;
    }

    public boolean isObjectList ()
    {
        return _isObjectList;
    }

    public boolean isValidateOrder ()
    {
        return _validateOrder;
    }

    public String theValidatorToUseForLists ()
    {
        return _validatorToUseForLists;
    }

    public void explodeNestedList (List<TestValidationParameter> newListOfValues)
    {
        TestValidationParameterList list =
            TestValidationParameterList.createFromListOfParameters(newListOfValues);
                _value = list;
    }

    public void validateTheOrderOfTheList (boolean validateOrder)
    {
        _validateOrder = validateOrder;
    }

    public void objectList (boolean representsObjectList)
    {
        _isObjectList = representsObjectList;
    }

    /**
     * Used by validation code to teel the display if this value was "interesting" from
     * an error reporting perspective.
     * @return true if this paramter was involved in producing an error.
     */
    public boolean errorValue ()
    {
        return _isErrorValue;
    }

    public void setErrorValue (boolean isError)
    {
        _isErrorValue = isError;
        if (isList()) {
            TestValidationParameterList list = (TestValidationParameterList)_value;
            for (int i = 0; i < list.size(); i++) {
                TestValidationParameter param = list.getParameter(i);
                param.setErrorValue(isError);
            }
        }
    }
}
