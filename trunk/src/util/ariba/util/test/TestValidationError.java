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

    $Id: //ariba/platform/util/core/ariba/util/test/TestValidationError.java#5 $
    Responsible: jimh
*/
package ariba.util.test;

import ariba.util.core.Fmt;

public class TestValidationError {
    private TestValidationParameter _oldValue;
    private TestValidationParameter _newValue;
    private String _nestedParameterPrefix = "";

    public TestValidationError (TestValidationParameter oldValue,
                                TestValidationParameter newValue,
                                String nestedParameterPrefix)
    {
        _oldValue = oldValue;
        _newValue = newValue;
        if (nestedParameterPrefix != null) {
            _nestedParameterPrefix = nestedParameterPrefix;
        }
    }

    public String getParameterName ()
    {
        String name = "";
        if (_newValue != null) {
            name = _newValue.getName();
        }
        else if (_oldValue != null) {
            name = _oldValue.getName();
        }
        if (_nestedParameterPrefix.length() != 0) {
            name = Fmt.S("%s::%s", _nestedParameterPrefix, name);
        }
        return name; 
    }

    public String getOldValue ()
    {
        if (_oldValue != null) {
            if (_oldValue.getValue() == null) {
                return "null";
            }
            return _oldValue.getValue().toString();
        }
        return "MISSING PARAMETER*";
    }

    public String getNewValue ()
    {
        if (_newValue != null) {
            if (_newValue.getValue() == null) {
                return "null";
            }
            return _newValue.getValue().toString();
        }
        return "MISSING PARAMETER";
    }

    public boolean isObjectList ()
    {
        if ((_newValue != null && _newValue.isObjectList()) ||
             _oldValue != null && _oldValue.isObjectList()) {
            return true;   
        }
        return false;
    }

    public TestValidationParameterList getOldList ()
    {
        if (_oldValue != null && _oldValue.isList()) {
            return (TestValidationParameterList)_oldValue.getValue();
        }
        return null;
    }

    public TestValidationParameterList getNewList ()
    {
        if (_newValue != null && _newValue.isList()) {
            return (TestValidationParameterList)_newValue.getValue();
        }
        return null;
    }
}
