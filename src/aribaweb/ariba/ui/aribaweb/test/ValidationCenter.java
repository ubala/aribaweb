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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/ValidationCenter.java#1 $
*/
package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWPage;
import ariba.util.core.ClassUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.test.TestValidationParameter;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

public class ValidationCenter extends AWComponent {
    public static final String VALIDATE_IGNORE_AWNAME = "VALIDATION_IGNORE";
    public static final String _VALIDATION_STRING_ENTRY_AW_NAME =
            "AUTOMATION_VALIDATION_ENTRY_STRING";
    public static final String _VALIDATION_STRING_READ_AW_NAME =
                "AUTOMATION_VALIDATION_READ_STRING";
    public static final String _VALIDATION_ADD_AW_NAME =
            "AUTOMATION_ADD_VALIDATION";
    public static final String _VALIDATION_COMPARE_AW_NAME =
            "AUTOMATION_COMPARE_VALIDATION";
        public static final String _CANCEL_VALIDATION_AW_NAME = "Cancel" + "."
                + ValidationCenter.VALIDATE_IGNORE_AWNAME;

    public String _testStringToValidate;
    public String _validationErrors = "";

    private SortedMap<String,List<TestInspectorLink>> _classToInspectors;
    public TestInspectorLink _currentInspector;

    public Object _classesWithInspectors;
    public Object _currentClassName;

    public String _selectedClassName = "";
    public TestInspectorLink _selectedInspector;
    public List<TestValidationParameter> _parameters;
    public TestValidationParameter _currentParameter;
    public String _storeString;
    private AWPage _fromPage;

    public void initialize (AWPage fromPage)
    {
        _fromPage = fromPage;
        loadInspectors();
    }

    public List<TestInspectorLink> getInspectorsForCurrentClass ()
    {
        return _classToInspectors.get(_currentClassName);
    }

    public void loadInspectors ()
    {
        _classToInspectors = MapUtil.sortedMap();
        TestContext testContext = TestContext.getTestContext(requestContext());
        Set keys = testContext.keys();
        for (Object c: keys) {
            List<TestInspectorLink> inspectors = getInspectors((Class)c);
            if (inspectors != null && !inspectors.isEmpty()) {
               _classToInspectors.put(((Class)c).getName(), inspectors);
            }
        }
        _classesWithInspectors = _classToInspectors.keySet().toArray();
    }

    public boolean getHighlightInspector ()
    {
        if (_currentInspector.equals(_selectedInspector) &&
            _currentClassName.equals(_selectedClassName)) {
            return true;
        }
        return false;
    }
    private List<TestInspectorLink> getInspectors (Class c)
    {
        if (TestLinkManager.instance().hasObjectInspectors(c)) {
            List<TestInspectorLink> inspectors =
                    TestLinkManager.instance().getObjectInspectors(c);
            return inspectors;
        }
        return null;
    }

    public String getInspectorAWName ()
    {
        return _currentClassName + "." + _currentInspector + VALIDATE_IGNORE_AWNAME;
    }

    public AWComponent chooseValidationAction ()
    {
        TestContext testContext = TestContext.getTestContext(requestContext());
        Class classType = ClassUtil.classForName((String)_currentClassName);
        Object objectToValidate = testContext.get(classType);
        _parameters = _currentInspector.invoke(requestContext());
        _storeString = encodeParameterList(_currentInspector, _parameters);
        _selectedClassName = (String)_currentClassName;
        _selectedInspector = _currentInspector;
        return this;
    }

    public AWComponent compareAction () throws Exception
    {
        String inspectorEncoding = TestValidationParameter.
                getInspectorEncoding(_testStringToValidate);
        TestInspectorLink inspector = new TestInspectorLink(inspectorEncoding);
        List<TestValidationParameter> params = inspector.invoke(requestContext());
        String errors = TestValidationParameter.
                validateParameterLists(params, _testStringToValidate);
        if (errors == null) {
            _validationErrors = "Validation Passed.";
            return _fromPage.pageComponent();
        }
        else {
            _validationErrors = errors.toString();
        }
        return this;
    }

    public AWComponent validationAction ()
    {
        return _fromPage.pageComponent();
    }

    public AWComponent cancelValidationAction ()
    {
        return _fromPage.pageComponent();
    }

    static String encodeParameterList (TestInspectorLink inspector,
                                       List<TestValidationParameter> list)
    {
        FastStringBuffer stringBuffer = new FastStringBuffer();
        stringBuffer.append(TestValidationParameter.escapeString(inspector.getEncodedString()));
        stringBuffer.append(",");
        Iterator<TestValidationParameter> it = list.iterator();
        while (it.hasNext()) {
            stringBuffer.append(TestValidationParameter.encodeParameter(it.next()));
            stringBuffer.append(",");
        }
        return stringBuffer.toString();
    }

}
