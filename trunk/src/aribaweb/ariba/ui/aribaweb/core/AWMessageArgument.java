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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWMessageArgument.java#6 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import java.util.Map;
import java.lang.reflect.Field;

public final class AWMessageArgument extends AWContainerElement
{
    private int _argumentNumber;

    public void init (String tagName, Map bindingsHashtable)
    {
        AWBinding argBinding =
            (AWBinding)bindingsHashtable.remove(AWBindingNames.key);
        if (argBinding != null) {
            _argumentNumber = argBinding.intValue(null);
        }
        else {
            throw new AWGenericException(
                "message argument number is null");
        }
        super.init(tagName, bindingsHashtable);
    }
    
    public int argumentNumber ()
    {
        return _argumentNumber;
    }
    
    protected Object getFieldValue (Field field)
      throws IllegalArgumentException, IllegalAccessException
    {
        try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }
}
