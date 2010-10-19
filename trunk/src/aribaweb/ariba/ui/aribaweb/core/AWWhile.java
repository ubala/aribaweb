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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWWhile.java#20 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWSemanticKeyProvider;
import ariba.ui.aribaweb.util.SemanticKeyProvider;
import ariba.ui.aribaweb.util.SemanticKeyProviderUtil;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * Because of the nature of AWWhile, it is not possible to support either skipping nor state recording.
 * This is because AWWhile is based primarily on a stateful enumeration which necessarily changes during eval
 * and which must be provided anew at the beginning of each phase.
 */
public final class AWWhile extends AWContainerElement
{
    private AWBinding _notEqualNull;
    private AWBinding _enumeration;
    private AWBinding _iterator;
    private AWBinding _item;
    private AWBinding _index;
    // record & playback
      private AWBinding _semanticKey;


    // ** Thread Safety Considerations: This is shared but all immutable ivars.

    public void init (String tagName, Map bindingsHashtable)
    {
        _notEqualNull = (AWBinding)bindingsHashtable.remove(AWBindingNames.notEqualNull);
        _enumeration = (AWBinding)bindingsHashtable.remove(AWBindingNames.enumeration);
        _iterator = (AWBinding)bindingsHashtable.remove(AWBindingNames.iterator);
        _item = (AWBinding)bindingsHashtable.remove(AWBindingNames.item);
        _index = (AWBinding)bindingsHashtable.remove(AWBindingNames.index);
        _semanticKey = (AWBinding)bindingsHashtable.remove(AWBindingNames.semanticKeyBindingName());
        super.init(tagName, bindingsHashtable);
    }

    private Object nextItem (Enumeration enumeration, Iterator iterator, AWComponent component)
    {
        Object nextItem = null;
        if (_notEqualNull != null) {
            nextItem = _notEqualNull.value(component);
        }
        else if (enumeration != null) {
            if (enumeration.hasMoreElements()) {
                nextItem = enumeration.nextElement();
            }
        }
        else if (iterator != null) {
            if (iterator.hasNext()) {
                nextItem = iterator.next();
            }
        }
        return nextItem;
    }

    private void pushItemAndIndex (Object item, int index, AWComponent component)
    {
        if (_index != null) {
            _index.setValue(index, component);
        }
        if (_item != null) {
            _item.setValue(item, component);
        }
    }

    private Enumeration getEnumeration (AWComponent component)
    {
        return _enumeration == null ? null : (Enumeration)_enumeration.value(component);
    }

    private Iterator getIterator (AWComponent component)
    {
        return _iterator == null ? null : (Iterator)_iterator.value(component);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        Iterator iterator = getIterator(component);
        Enumeration enumeration = iterator == null ? getEnumeration(component) : null;
        requestContext.pushElementIdLevel();
        Object currentItem = null;
        int currentIndex = 0;
        while ((currentItem = nextItem(enumeration, iterator, component)) != null) {
            pushItemAndIndex(currentItem, currentIndex, component);
            currentIndex++;
            super.applyValues(requestContext, component);
        }
        requestContext.popElementIdLevel();
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        Iterator iterator = getIterator(component);
        Enumeration enumeration = iterator == null ? getEnumeration(component) : null;
        requestContext.pushElementIdLevel();
        Object currentItem = null;
        int currentIndex = 0;
        while ((currentItem = nextItem(enumeration, iterator, component)) != null) {
            pushItemAndIndex(currentItem, currentIndex, component);
            currentIndex++;
            actionResults = super.invokeAction(requestContext, component);
            if (actionResults != null) {
                break;
            }
        }
        requestContext.popElementIdLevel();
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        Iterator iterator = getIterator(component);
        Enumeration enumeration = iterator == null ? getEnumeration(component) : null;
        requestContext.pushElementIdLevel();
            // record & playback
        boolean shouldRecord = requestContext._debugShouldRecord();
        if (shouldRecord) {
            requestContext._debugPushSemanticKeyPrefix();
        }
        Object currentItem = null;
        int currentIndex = 0;
        while ((currentItem = nextItem(enumeration, iterator, component)) != null) {
            pushItemAndIndex(currentItem, currentIndex, component);
            // record & playback
            if (shouldRecord) {
                String key = null;
                if (_semanticKey != null) {
                    key = (String)_semanticKey.value(component);
                }
                if (key == null) {
                    SemanticKeyProvider provider = AWSemanticKeyProvider.get(currentItem);
                    if (provider != null) {
                        key = SemanticKeyProviderUtil.getKey(provider, currentItem, component);
                    }
                }
                requestContext._debugSetSemanticKeyPrefix(key);
            }
            currentIndex++;
            super.renderResponse(requestContext, component);
        }
        requestContext.popElementIdLevel();
            // record & playback
        if (shouldRecord) {
            requestContext._debugPopSemanticKeyPrefix();
        }
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
