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

    $Id: //ariba/platform/util/core/ariba/util/test/TestValidationParameter.java#1 $
*/
package ariba.util.test;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.Constants;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TestValidationParameter {
    private String _name;
    private String _value;

    public TestValidationParameter (String name, String value)
    {
        _name = name;
        _value = value;
    }

    public String getName ()
    {
        return _name;
    }

    public String getValue ()
    {
        return _value;
    }

    public static String encodeParameter (TestValidationParameter parameter)
    {
        StringBuilder encoding = new StringBuilder();
        encoding.append(escapeString(parameter._name));
        encoding.append(",");
        encoding.append(escapeString(parameter._value));

        return encoding.toString();
    }

    public static String escapeString (String string)
    {
        if (string == null) {
            return null;
        }
        StringBuilder valueAdjustment = new StringBuilder(string);

        // escape any backslashes.
        for (int i = 0; i < valueAdjustment.length(); i++) {
            String s = valueAdjustment.substring(i,i+1);
            if (s.equals("\\")) {
                valueAdjustment.insert(i,"\\");
                i++;
            }
        }

        // surround with double quotes
        valueAdjustment.insert(0,"\"");
        valueAdjustment.append("\"");

        return valueAdjustment.toString();
    }

    private static String unescapeString (String string)
    {
        StringBuilder valueAdjustment = new StringBuilder(string);

        // remove begin and end quotes.
        if (!valueAdjustment.substring(0,1).equals("\"")) {
            Assert.assertNonFatal(false,
                    "String did not start with quote.  Started with:" +
                    valueAdjustment.substring(0,1));
        }
        valueAdjustment.deleteCharAt(0);

        if (!valueAdjustment.substring(valueAdjustment.length()-1,
                valueAdjustment.length()).equals("\"")) {
            Assert.assertNonFatal(false,
                    "String did not end with quote.  Ended with:" +
                    valueAdjustment.substring(valueAdjustment.length()-1,
                    valueAdjustment.length()));
        }
        valueAdjustment.deleteCharAt(valueAdjustment.length()-1);
        return valueAdjustment.toString();
    }

    private static TestValidationParameter decodeParameter (String name,
                                                            String value)
    {
        TestValidationParameter parameter =
                new TestValidationParameter(unescapeString(name),
                        unescapeString(value));
        return parameter;
    }

    public static String getInspectorEncoding (String parameterEncoding)
    {
        String[] tokens = tokenizeString(parameterEncoding);
        return  unescapeString(tokens[0]);
    }

    static List<TestValidationParameter> decodeParameterList (String encoding)
    {
        List<TestValidationParameter> parameters = ListUtil.list();
        String[] tokens = tokenizeString(encoding);
        for (int i = 0; i < (tokens.length-1)/2; i++) {
            parameters.add(decodeParameter(tokens[2*i+1], tokens[2*i+2]));
        }
        return parameters;
    }

    static String[] tokenizeString (String s)
    {
        if (!s.substring(0,1).equals("\"")) {
            Assert.assertNonFatal(false,"Expected Quote as first Character");
        }
        List<Integer> startIndices = ListUtil.list();
        List<Integer> endIndices = ListUtil.list();
        boolean lookingForStart = true;
        for (int i=0; i < s.length(); i++) {
            String subStr = s.substring(i,i+1);
            if (subStr.equals("\"")) {
                if (i > 0 && s.substring(i-1,i).equals("\\")) {
                    // this is an escaped quote, should ignore.
                }
                else if (lookingForStart) {
                    startIndices.add(Constants.getInteger(i));
                    lookingForStart = false;
                }
                else {
                    endIndices.add(Constants.getInteger(i));
                    lookingForStart = true;
                }
            }
        }
        if (!lookingForStart) {
            // this means we didn't have ane ven number of quotes,
            // so a malformed string was encoutnered.
            Assert.assertNonFatal(false,
                    "Encountered uneven number of quotes.");
        }
        String[] strings = new String[startIndices.size()];
        for (int i=0; i < startIndices.size(); i++) {
            strings[i] = s.substring(startIndices.get(i).intValue(),
                    endIndices.get(i).intValue()+1);
        }
        return strings;
    }

    public static String validateParameterLists (
            List<TestValidationParameter> newList,
            String oldEncoding) {
        List<TestValidationParameter> oldList =
                decodeParameterList(oldEncoding);

        FastStringBuffer errors = new FastStringBuffer();

        Map<String,String> newValues = MapUtil.map();
        // put new values into map for efficient lookup.
        for (TestValidationParameter param: newList) {
            newValues.put(param._name, param._value);
        }

        // loop through old values to make sure they are all there and equal.
        Iterator<TestValidationParameter> it = oldList.iterator();
        while (it.hasNext()) {
            TestValidationParameter oldParameter = it.next();
            String newValue = newValues.get(oldParameter._name);
            if (!newValue.equals(oldParameter._value)) {
                // values did not match, validation failed.
                errors.append(produceErrorString(oldParameter._name,
                        oldParameter._value, newValue));
            }
        }
        if (errors.length() > 0) {
            return errors.toString();
        }
        else {
            return null;
        }
    }

    private static String produceErrorString (String paramName, String oldValue,
                                             String newValue) {
        FastStringBuffer error = new FastStringBuffer();
        error.append("Error: Parameter Name:");
        error.append(paramName);
        error.append(", Old Value:");
        error.append(oldValue);
        error.append(", New Value:");
        error.append(newValue);
        error.append("\n");
        return error.toString();
    }
}
