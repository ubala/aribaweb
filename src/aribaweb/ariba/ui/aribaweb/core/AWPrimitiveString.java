/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWPrimitiveString.java#9 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import java.lang.reflect.Field;
import java.util.Map;

public class AWPrimitiveString extends AWBindableElement
{
    private AWBinding _value;

    // ** Thread Safety Considerations: This is shared but ivar is immutable.

    public void init (String tagName, Map bindingsHashtable)
    {
        _value = (AWBinding)bindingsHashtable.remove(AWBindingNames.value);
        super.init(tagName, bindingsHashtable);
    }
    
    public AWBinding _value ()
    {
        return _value;
    }

    protected AWEncodedString stringValueForObjectInComponent (Object objectValue,
                                                               AWComponent component)
    {
        return (objectValue instanceof AWEncodedString) ?
                (AWEncodedString)objectValue :
                AWEncodedString.sharedEncodedString(AWUtil.toString(objectValue));
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        Object objectValue = _value.value(component);
        AWEncodedString stringValue = (objectValue == null) ?
                null : stringValueForObjectInComponent(objectValue, component);
        requestContext.response().appendContent(stringValue);
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
