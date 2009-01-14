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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWPhaseInvocation.java#3 $
*/
package ariba.ui.aribaweb.core;

import java.util.Map;
import java.lang.reflect.Field;
import ariba.ui.aribaweb.core.AWBindableElement;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;

public final class AWPhaseInvocation extends AWBindableElement
{
    private AWBinding _invoke;
    private AWBinding _append;
    private AWBinding _take;

    // ** Thread Safety Considerations: All ivars are immutable so no locking required for this class.

    public void init (String tagName, Map bindingsHashtable)
    {
        _invoke = (AWBinding)bindingsHashtable.remove(AWBindingNames.invoke);
        _append = (AWBinding)bindingsHashtable.remove("append");
        _take = (AWBinding)bindingsHashtable.remove("take");
        super.init(tagName, bindingsHashtable);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        if (_take != null) _take.value(component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        if (_invoke != null) _invoke.value(component);
        return null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (_append != null) _append.value(component);
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
