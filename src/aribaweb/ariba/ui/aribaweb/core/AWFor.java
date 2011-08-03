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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWFor.java#7 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWSemanticKeyProvider;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.SemanticKeyProvider;
import ariba.ui.aribaweb.util.SemanticKeyProviderUtil;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.fieldvalue.OrderedList;

import java.lang.reflect.Field;
import java.util.Map;

public class AWFor extends AWContainerElement
{
    private AWBinding _list;
    private AWBinding _item;
    private AWBinding _count;
    private AWBinding _index;
    private AWBinding _start;
    private AWBinding _itemClassName;
    private boolean _hasItemBinding = false;
    private boolean _hasIndexBinding = false;
    private AWBinding _scopeSubcomponentsByItem;

    // record & playback
    private AWBinding _semanticKey;

    // ** Thread Safety Considerations:  Although instances of this class are shared by many threads, no locking is required because AWBindings are immutable and none of these change after being established.

    public void init (String tagName, Map bindingsHashtable)
    {
        _list = (AWBinding)bindingsHashtable.remove(AWBindingNames.list);
        _item = (AWBinding)bindingsHashtable.remove(AWBindingNames.item);
        _count = (AWBinding)bindingsHashtable.remove(AWBindingNames.count);
        _index = (AWBinding)bindingsHashtable.remove(AWBindingNames.index);
        _start = (AWBinding)bindingsHashtable.remove(AWBindingNames.start);
        _itemClassName = (AWBinding)bindingsHashtable.remove(AWBindingNames.itemClassName);
        if (_item != null) {
            _hasItemBinding = true;
        }
        if (_index != null) {
            _hasIndexBinding = true;
        }
        _semanticKey = (AWBinding)bindingsHashtable.remove(AWBindingNames.semanticKeyBindingName());
        _scopeSubcomponentsByItem = (AWBinding)bindingsHashtable.remove("scopeSubcomponentsByItem");

        super.init(tagName, bindingsHashtable);
    }

    public AWBinding _list ()
    {
        return _list;
    }

    public AWBinding _item ()
    {
        return _item;
    }

    public AWBinding _count ()
    {
        return _count;
    }

    public AWBinding _index ()
    {
        return _index;
    }

    public AWBinding _start ()
    {
        return _start;
    }

    public AWBinding _itemClassName ()
    {
        return _itemClassName;
    }

    public final int _startIndex (AWComponent component)
    {
        return _start == null ? 0 : _start.intValue(component);
    }

    public Object _orderedListInComponent (AWComponent component)
    {
        Object orderedList = _list.value(component);
        if (orderedList == null) {
            orderedList = EmptyVector;
        }
        return orderedList;
    }

    private int minimumRepeatCount (int repeatCount, AWComponent component)
    {
        if (_count != null) {
            int count = _count.intValue(component);
            if (count < repeatCount) {
                repeatCount = count;
            }
        }
        return repeatCount;
    }

    /*
        Setting this binding to true will set up stateful subcomponent scoping to be based on the
        item (object key) of the repetition, rather than the *position* of the element (as encoded in
        the elementId).  The effect is that stateful components (in, say, DataTables) will continue to
        rendezvous with their asociated stateful component sub-tree, even when objects are inserted
        or deleted in from of them in their list.

        The real "meat" behind this functionality is in requestContext._pushSubcomponentScope() etc.
    */
    protected boolean scopeSubcomponentsByItem (AWComponent component)
    {
        return (_scopeSubcomponentsByItem != null)
                ? _scopeSubcomponentsByItem.booleanValue(component) : false;
    }

    public void applyValues (AWRequestContext requestContext, AWComponent component)
    {
        AWBaseElement prev = requestContext.pushCurrentElement(this);
        final boolean allowsSkipping = requestContext.allowsSkipping();
        if (allowsSkipping) {
            // This block serves to skip the entire repetition
            AWElementIdPath targetFormIdPath = requestContext.targetFormIdPath();
            boolean shouldSkip = !requestContext.nextPrefixMatches(targetFormIdPath);
            if (shouldSkip) {
                // Since we bail out here, we must incementElementId to balance the
                // pushElementIdLevel we would have done otherwise (which increments before pushing)
                requestContext.incrementElementId();
                return;
            }
        }
        requestContext.pushElementIdLevel();
        AWElementIdPath parentPath = requestContext.currentElementIdPath();
        Object orderedList = null;
        OrderedList orderedListClassExtension = null;
        int repeatCount;
        if (_list != null) {
            orderedList = _orderedListInComponent(component);
            orderedListClassExtension = OrderedList.get(orderedList);
            repeatCount = orderedListClassExtension.size(orderedList);
            repeatCount = minimumRepeatCount(repeatCount, component);
        }
        else {
            repeatCount = _count.intValue(component);
        }

        // at this point, prefixMatches will always be true.
        int index = _startIndex(component);
        if (index > 0) requestContext.incrementElementId(index);  // bump elementId to match start pos
        boolean scoping = scopeSubcomponentsByItem(component);
        for (; index < repeatCount; index++) {
            boolean shouldSkip = false;
            if (allowsSkipping) {
                AWElementIdPath path = requestContext.targetFormIdPath();
                shouldSkip = !requestContext.nextPrefixMatches(path);
            }
            if (shouldSkip) {
                requestContext.incrementElementId();
            }
            else {
                requestContext.pushElementIdLevel();
                pushIterationValues(component, index, orderedListClassExtension, orderedList, parentPath, scoping);
                super.applyValues(requestContext, component);
                if (scoping) requestContext._popSubcomponentScope();
                requestContext.popElementIdLevel();

            }
        }
        requestContext.popElementIdLevel();
        requestContext.popCurrentElement(prev);
    }

    public AWResponseGenerating invokeAction (AWRequestContext requestContext, AWComponent component)
    {
        AWBaseElement prev = requestContext.pushCurrentElement(this);
        AWResponseGenerating actionResults = null;
        AWElementIdPath requestSenderIdPath = null;
        final boolean allowsSkipping = requestContext.allowsSkipping();
        if (allowsSkipping) {
            // This block serves to skip the entire repetition
            requestSenderIdPath = requestContext.requestSenderIdPath();
            boolean prefixMatches = requestContext.nextPrefixMatches(requestSenderIdPath);
            if (!prefixMatches) {
                // Since we bail out here, we must incementElementId to balance the
                // pushElementIdLevel we would have done otherwise (which increments before pushing)
                requestContext.incrementElementId();
                return null;
            }
        }
        // If we made it this far, the sender is somewhere in one of our iterations.
        boolean scoping = scopeSubcomponentsByItem(component);
        requestContext.pushElementIdLevel();
        AWElementIdPath parentPath = requestContext.currentElementIdPath();
        Object orderedList = null;
        OrderedList orderedListClassExtension = null;
        int repeatCount = -1;
        if (_list != null) {
            orderedList = _orderedListInComponent(component);
            orderedListClassExtension = OrderedList.get(orderedList);
            repeatCount = orderedListClassExtension.size(orderedList);
            repeatCount = minimumRepeatCount(repeatCount, component);
        }
        else {
            repeatCount = _count.intValue(component);
        }
        if (allowsSkipping) {
            // Determine the index of the repetition to fire
            int currentElementIdPathLength = requestContext.currentElementIdPathLength();
            int lastIndex = currentElementIdPathLength - 1;
            // subtract 1 here because indexing starts at 1 for the repetition since we do
            // two pushes in a row for the first iteration.
            int indexOfLoop = requestSenderIdPath.get(lastIndex) - 1;
            // advance the elementId the appropriate amount
            if (indexOfLoop < repeatCount) {
                requestContext.incrementElementId(indexOfLoop);

                requestContext.pushElementIdLevel();
                pushIterationValues(component, indexOfLoop, orderedListClassExtension, orderedList, parentPath, scoping);
                actionResults = super.invokeAction(requestContext, component);
                requestContext.popElementIdLevel();
                if (scoping) requestContext._popSubcomponentScope();
            }
            else {
                // assert in debug mode, and log warning in all cases
                String debugString =
                    Fmt.S("AWFor: %s, parentPath: %s, requestSenderIdPath: %s, requestSenderId: %s",
                        this,
                        AWElementIdPath.debugElementIdPath(parentPath),
                        AWElementIdPath.debugElementIdPath(requestSenderIdPath),
                        requestContext.requestSenderId());
                Log.aribaweb.warning(10690, debugString);
                Assert.that(!requestContext.isDebuggingEnabled(), debugString);
            }
        }
        else {
            int index = _startIndex(component);
            if (index > 0) requestContext.incrementElementId(index);  // bump elementId to match start pos
            for (; index < repeatCount; index++) {
                requestContext.pushElementIdLevel();
                pushIterationValues(component, index, orderedListClassExtension, orderedList, parentPath, scoping);
                actionResults = super.invokeAction(requestContext, component);
                requestContext.popElementIdLevel();
                if (scoping) requestContext._popSubcomponentScope();
                if (actionResults != null) {
                    break;
                }
            }
        }
        requestContext.popElementIdLevel();
        requestContext.popCurrentElement(prev);
        return actionResults;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        AWBaseElement prev = requestContext.pushCurrentElement(this);
        requestContext.pushElementIdLevel();
        AWElementIdPath parentPath = requestContext.currentElementIdPath();
        boolean scoping = scopeSubcomponentsByItem(component);

        // record & playback
        boolean shouldPushSemanticKeyPrefix =
                (AWConcreteApplication.IsAutomationTestModeEnabled &&
                requestContext.isDebuggingEnabled() &&
                !AWBindingNames.UseNamePrefixBinding) ||
                requestContext._debugShouldRecord();
        if (shouldPushSemanticKeyPrefix) {
            requestContext._debugPushSemanticKeyPrefix();
        }

        Object orderedList = null;
        int repeatCount;
        OrderedList orderedListClassExtension = null;
        if (_list != null) {
            orderedList = _orderedListInComponent(component);
            orderedListClassExtension = OrderedList.get(orderedList);
            repeatCount = orderedListClassExtension.size(orderedList);
            repeatCount = minimumRepeatCount(repeatCount, component);
        }
        else {
            repeatCount = _count.intValue(component);
        }
        int index = _startIndex(component);
        if (index > 0) requestContext.incrementElementId(index);  // bump elementId to match start pos
        for (; index < repeatCount; index++) {
            requestContext.pushElementIdLevel();
            pushIterationValues(component, index, orderedListClassExtension, orderedList, parentPath, scoping);
            // record & playback
            if (shouldPushSemanticKeyPrefix) {
                String key = null;
                if (_semanticKey != null) {
                    key = (String)_semanticKey.value(component);
                }
                if (key == null && orderedList != null) {
                    Object item = orderedListClassExtension.elementAt(orderedList, index);
                    if (item != null) {
                        SemanticKeyProvider provider = AWSemanticKeyProvider.get(item);
                        if (provider != null) {
                            key = SemanticKeyProviderUtil.getKey(provider, item, component);
                        }
                    }
                }
                requestContext._debugSetSemanticKeyPrefix(key);
            }
            super.renderResponse(requestContext, component);
            requestContext.popElementIdLevel();
            if (scoping) requestContext._popSubcomponentScope();
        }
        // record & playback
        if (shouldPushSemanticKeyPrefix) {
            requestContext._debugPopSemanticKeyPrefix();
        }
        requestContext.popElementIdLevel();
        requestContext.popCurrentElement(prev);
    }

    private void pushIterationValues (AWComponent component, int index,
            OrderedList orderedListClassExtension, Object orderedList, AWElementIdPath parentPath, boolean scoping)
    {
        if (_hasIndexBinding) {
            _index.setValue(index, component);
        }
        if (_hasItemBinding || scoping) {
            Object currentItem = orderedListClassExtension.elementAt(orderedList, index);
            if (_hasItemBinding) _item.setValue(currentItem, component);
            if (scoping) component.requestContext()._pushSubcomponentScope(parentPath, currentItem);
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

    public String toString ()
    {
        String superString = super.toString();
        return superString + " list: " + _list + " item: " + _item;
    }
}
