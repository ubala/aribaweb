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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWMethodInvocation.java#7 $
*/

package ariba.ui.aribaweb.core;

import java.util.Map;
import java.lang.reflect.Field;

public final class AWMethodInvocation extends AWBindableElement
{
    private AWBinding _invoke;

    // ** Thread Safety Considerations: All ivars are immutable so no locking required for this class.

    public void init (String tagName, Map bindingsHashtable)
    {
        _invoke = (AWBinding)bindingsHashtable.remove(AWBindingNames.invoke);
        if (_invoke == null) {
            throw new AWMissingBindingException(getClass().getName() + ": Missing binding -- must have \"invoke\" binding.");
        }
        super.init(tagName, bindingsHashtable);
    }
    
    public AWBinding _invoke ()
    {
        return _invoke;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        _invoke.value(component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        _invoke.value(component);
        return null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _invoke.value(component);
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
