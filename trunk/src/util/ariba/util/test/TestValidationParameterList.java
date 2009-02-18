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

    $Id: //ariba/platform/util/core/ariba/util/test/TestValidationParameterList.java#2 $
*/
package ariba.util.test;

import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.Fmt;
import ariba.util.core.SetUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestValidationParameterList implements Iterable {
    private List<TestValidationParameter> _list;

    private TestValidationParameterList ()
    {
        _list = ListUtil.list();
    }

    public static TestValidationParameterList createList ()
    {
        return new TestValidationParameterList();
    }

    public boolean add (TestValidationParameter testValidationParameter)
    {
        return _list.add(testValidationParameter);
    }

    public boolean add (String name, String value)
    {
        TestValidationParameter parameter = new TestValidationParameter(name, value);
        return add(parameter);
    }

    public boolean add (String name, String key, String value)
    {
        TestValidationParameter parameter = new TestValidationParameter(name, key, value);
        return add(parameter);
    }

    public boolean add (String name, TestValidationParameterList value)
    {
        TestValidationParameter parameter = new TestValidationParameter(name, value);
        return add(parameter);
    }

    public boolean add (String name, String key, TestValidationParameterList value)
    {
        TestValidationParameter parameter = new TestValidationParameter(name, key, value);
        return add(parameter);
    }

    public TestValidationParameter[] getValidationParameters ()
    {
        TestValidationParameter[] parameters = new TestValidationParameter[_list.size()];

        for (int i = 0; i < _list.size(); i++) {
            parameters[i] = (TestValidationParameter)_list.get(i);
        }
        return parameters;

    }

    public TestValidationParameter[] createFlatValidationParameterList ()
    {
        // we flatten out the whole hierarchy.
        List<TestValidationParameter> flattenedList = ListUtil.list();
        for (int i = 0; i < _list.size(); i++) {
            TestValidationParameter parameter = _list.get(i);
            if (!parameter.isList()) {
                flattenedList.add(parameter);
            }
            else {
                TestValidationParameter[] nestedList =
                    ((TestValidationParameterList)parameter.getValue()).
                            createFlatValidationParameterList();
                for (int j = 0; j < nestedList.length; j++) {
                    TestValidationParameter currentParam = nestedList[j];
                    TestValidationParameter newParameter =
                            new TestValidationParameter(
                                    parameter.getName() + "::" + currentParam.getName(),
                                    currentParam.getKey(),
                                    currentParam.getValue().toString());
                    flattenedList.add(newParameter);
                }
            }
        }

        TestValidationParameter[] parameters =
                new TestValidationParameter[flattenedList.size()];
        for (int i = 0; i < flattenedList.size(); i++) {
            parameters[i] = (TestValidationParameter)flattenedList.get(i);
        }
        return parameters;
    }

    public Iterator<TestValidationParameter> iterator ()
    {
        return _list.iterator();
    }

    /**
         * Validates that all of the keys are unique including keys within nested lists.
         * Note that the nested lists keys can collide with keys in the parent or other lists,
         * however the keys must all be unique for each entry in that list.
         * @return a list of Strings which are the names of the non-unique keys (including parent path delimited by ::.  The list will be empty if no problems were found.
         */
    public List<String> verifyUniqueKeys ()
    {
        return verifyUniqueKeys(_list);
    }

    public static TestValidationParameterList createFromListOfParameters (
            List<TestValidationParameter> list)
    {
        TestValidationParameterList newList = TestValidationParameterList.createList();
        for (int i = 0; i < list.size(); i++) {
            newList.add(list.get(i));
        }
        return newList;
    }

    private static List<String> verifyUniqueKeys (List<TestValidationParameter> list)
    {
        return verifyUniqueKeys(list, null);
    }

    private static List<String> verifyUniqueKeys (List<TestValidationParameter> list,
                                                  String parentPath) {
        List<String> problemKeys = ListUtil.list();
        Iterator<TestValidationParameter> iter = list.iterator();
        Set<String> keySet = SetUtil.set();
        while (iter.hasNext()) {
            TestValidationParameter param = iter.next();
            if (keySet.contains(param.getKey())) {
                if (parentPath == null) {
                    problemKeys.add(param.getKey());
                }
                else {
                    problemKeys.add(Fmt.S("%s::%s", parentPath, param.getKey()));
                }
            }
            else {
                keySet.add(param.getKey());
            }

            // if value is list then also check inside it.
            if (param.getValue() instanceof TestValidationParameterList) {
                String newParentPath;
                if (parentPath != null) {
                    newParentPath = Fmt.S("%s::%s", parentPath, param.getKey());
                }
                else {
                    newParentPath = param.getKey();
                }
                problemKeys.addAll(
                    verifyUniqueKeys(
                            ((TestValidationParameterList)param.getValue())._list,
                            newParentPath));
            }
        }
        return problemKeys;
    }
}
