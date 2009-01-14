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

    $Id: //ariba/platform/util/core/ariba/util/test/TestValidationError.java#2 $
    Responsible: jimh
*/
package ariba.util.test;

public class TestValidationError {
    private TestValidationParameter _oldValue;
    private TestValidationParameter _newValue;

    public TestValidationError (TestValidationParameter oldValue,
                                TestValidationParameter newValue)
    {
        _oldValue = oldValue;
        _newValue = newValue;
    }

    public String getParameterName ()
    {
        if (_newValue != null) {
            return _newValue.getName();
        }
        else if (_oldValue != null) {
            return _oldValue.getName();
        }
        return "";
    }

    public String getOldValue ()
    {
        if (_oldValue != null) {
            return _oldValue.getValue().toString();
        }
        return "";
    }

    public String getNewValue ()
    {
        if (_newValue != null) {
            return _newValue.getValue().toString();
        }
        return "MISSING PARAMETER";
    }
}
