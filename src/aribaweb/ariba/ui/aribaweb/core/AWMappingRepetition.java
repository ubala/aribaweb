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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWMappingRepetition.java#16 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWMap;
import ariba.ui.aribaweb.util.AWSemanticKeyProvider;
import ariba.ui.aribaweb.util.SemanticKeyProvider;
import ariba.ui.aribaweb.util.SemanticKeyProviderUtil;
import ariba.util.core.Compare;
import ariba.util.core.ListUtil;
import ariba.util.core.Sort;
import ariba.util.core.StringCompare;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Reimplement as an AWComponent?
public final class AWMappingRepetition extends AWContainerElement
{
    private static final int ActionApplyValues = 0;
    private static final int ActionInvokeAction = 1;
    private static final int ActionRenderResponse = 2;

    private AWBinding _map;
    private AWBinding _key;
    private AWBinding _value;
    private AWBinding _keyClassName;
    private AWBinding _valueClassName;
    private AWBinding _sort;

    private static final Compare ToStringCompare = new Compare ()
    {
        public int compare (Object o1, Object o2)
        {
            return StringCompare.self.compare(o1.toString(), o2.toString());
        }
    };

    // ** Thread Safety Considerations:  Although instances of this class are shared by many threads, no locking is required because AWBindings are immutable and none of these change after being established.

    public void init (String tagName, Map bindingsHashtable)
    {
        _map = (AWBinding)bindingsHashtable.remove(AWBindingNames.map);
        _key = (AWBinding)bindingsHashtable.remove(AWBindingNames.key);
        _value = (AWBinding)bindingsHashtable.remove(AWBindingNames.value);
        _keyClassName = (AWBinding)bindingsHashtable.remove(AWBindingNames.keyClassName);
        _valueClassName = (AWBinding)bindingsHashtable.remove(
                AWBindingNames.valueClassName);
        _sort = (AWBinding)bindingsHashtable.remove(AWBindingNames.sort);
        super.init(tagName, bindingsHashtable);
    }

    public AWBinding _map ()
    {
        return _map;
    }

    public AWBinding _key ()
    {
        return _key;
    }

    public AWBinding _value ()
    {
        return _value;
    }

    public AWBinding _keyClassName ()
    {
        return _keyClassName;
    }

    public AWBinding _valueClassName ()
    {
        return _valueClassName;
    }

    public AWBinding _sort ()
    {
        return _value;
    }

    public Object _mapInComponent (AWComponent component)
    {
        Object map = _map.value(component);
        if (map == null) {
            map = EmptyMap;
        }
        return map;
    }

    public void applyValues(
            AWRequestContext requestContext,
            AWComponent component
            )
    {
        doMappingRepetition(requestContext, component, ActionApplyValues);
    }

    public AWResponseGenerating invokeAction(
            AWRequestContext requestContext,
            AWComponent component
            )
    {
        return doMappingRepetition(requestContext, component,
                ActionInvokeAction);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        doMappingRepetition(requestContext, component, ActionRenderResponse);
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

    private AWResponseGenerating doMappingRepetition (
            AWRequestContext requestContext,
            AWComponent component,
            int action
            )
    {
        AWResponseGenerating actionResults = null;
        Object map = null;
        AWMap mapClassExtension = null;
        if (_map != null) {
            map = _mapInComponent(component);
            mapClassExtension = AWMap.get(map);
        }

        requestContext.pushElementIdLevel();

        // record & playback
        boolean shouldRecord =
                (action == ActionRenderResponse) &&
                (requestContext._debugShouldRecord());
        if (shouldRecord) {
            requestContext._debugPushSemanticKeyPrefix();
        }

        Iterator keys = mapClassExtension.keys(map);
        if (_sort != null) {
            if (Boolean.valueOf((String)_sort.value(component)).booleanValue()) {
                keys = sortKeys(keys);
            }
        }
        loop:
        while (keys.hasNext()) {
            Object currentKey = keys.next();
            if (_key != null) {
                _key.setValue(currentKey, component);
            }
            if (_value != null) {
                Object currentValue = mapClassExtension.get(map, currentKey);
                _value.setValue(currentValue, component);
            }
            switch (action) {
                case ActionApplyValues:
                    super.applyValues(requestContext, component);
                    break;
                case ActionInvokeAction:
                    actionResults = super.invokeAction(requestContext, component);
                    if (actionResults == null) {
                    }
                    else {
                        break loop;
                    }
                    break;
                case ActionRenderResponse:
                    if (shouldRecord) {
                        SemanticKeyProvider provider = AWSemanticKeyProvider.get(currentKey);
                        if (provider != null) {
                            String key = SemanticKeyProviderUtil.getKey(provider, currentKey, component);
                            requestContext._debugSetSemanticKeyPrefix(key);
                        }
                        else {
                            requestContext._debugSetSemanticKeyPrefix(null);
                        }
                    }
                    super.renderResponse(requestContext, component);
                    break;
            }
        }
        requestContext.popElementIdLevel();
        // record & playback
        if (shouldRecord) {
            requestContext._debugPopSemanticKeyPrefix();
        }
        return actionResults;
    }

    // Not very efficient; should only be used during testing to ensure
    // deterministic test output
    private static Iterator sortKeys (Iterator keys)
    {
        List v = ListUtil.list();
        while (keys.hasNext()) {
            v.add(keys.next());
        }
        Object[] a = new Object[v.size()];
        v.toArray(a);
        Sort.objects(a, ToStringCompare);
        for (int i = 0; i < a.length; i++) {
            v.set(i, a[i]);
        }
        return v.iterator();
    }
}
