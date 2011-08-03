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

    $Id: //ariba/platform/ui/widgets/ariba/ui/outline/OutlineRepetition.java#5 $
*/
package ariba.ui.outline;

import java.util.List;
import ariba.util.core.ListUtil;
import ariba.util.core.Assert;
import ariba.util.fieldvalue.OrderedList;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWResponseGenerating;

public final class OutlineRepetition extends AWComponent
{
    protected AWBinding _childrenBinding;
    protected AWBinding _hasChildrenBinding;
    protected AWBinding _selectedObjectBinding;
    protected AWBinding _selectionPathBinding;
    protected AWBinding _expandCurrentItem;
    protected AWBinding _showExpansionControl;
    public AWBinding _fullList;
    protected int _maxLevels;
    protected boolean _expandAll;
    protected OutlineState _outlineState;
    protected int _start, _end;

    // state for currentItem
    protected Object _currentItem;
    protected boolean _isExpanded;
    public boolean _hasChildren;
    protected boolean _hideImages;
    protected Object _currentItemChildren;  // OrderedList
    protected boolean _useCachedChildren;

    // long-term (cross phase) state
    public static OutlineRepetition currentInstance (AWComponent currentComponent)
    {
        return (OutlineRepetition)currentComponent.env().peek("awxOutlineRepetition");
    }

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        _hasChildrenBinding = bindingForName(BindingNames.hasChildren);
        _childrenBinding = bindingForName(BindingNames.children);
        _selectionPathBinding = bindingForName(BindingNames.selectionPath);
        _selectedObjectBinding = bindingForName(BindingNames.selectedObject);
        _maxLevels = hasBinding(BindingNames.maxLevels) ? intValueForBinding(BindingNames.maxLevels) : 0;

        _expandCurrentItem = bindingForName(BindingNames.expandCurrentItem);
        _showExpansionControl = bindingForName(BindingNames.showExpansionControl);
        _fullList = bindingForName("fullList");

        // create an outline state if we don't have one
        _outlineState = (OutlineState)valueForBinding(BindingNames.outlineState);
        if (_outlineState == null) {
            // create it and push it
            _outlineState = new OutlineState();
            setValueForBinding(_outlineState, BindingNames.outlineState);
        }

        // create a selection path if we don't have one
        if (valueForBinding(_selectionPathBinding) == null) {
            // create it and push it
            setSelectionPath(_outlineState.expansionPath());
        }
    }

    protected void checkBindings ()
    {
        OutlineState outlineState = (OutlineState)valueForBinding(BindingNames.outlineState);
        if (outlineState != null) _outlineState = outlineState;
        
    	if (_outlineState.didExecuteCollapseAll()) {
            setValueForBinding(null, BindingNames.selectionPath);
            setSelectedObject(null);
            _outlineState.setDidExecuteCollapseAll(false);
    	}

        if (_selectionPathBinding != null) {
       	    _outlineState.setExpansionPath((List)valueForBinding(_selectionPathBinding));
        }
        if (_selectedObjectBinding != null) {
            _outlineState.setSelectedObject(valueForBinding(_selectedObjectBinding));
        }

        _expandAll = booleanValueForBinding(BindingNames.expandAll) ||
                     requestContext().isPrintMode();

        _hideImages = requestContext().isExportMode();
    }

    protected void accumulateList (List all, Object currSubList)
    {
        if (currSubList == null) return;

        incrementNestingLevel();
        OrderedList orderedListClassExtension = OrderedList.get(currSubList);
        int count = orderedListClassExtension.size(currSubList);
        for (int i=0; i<count; i++) {
            Object currObj = orderedListClassExtension.elementAt(currSubList, i);
            all.add(currObj);
            setCurrentItem(currObj);
            if (isExpanded()) {
                accumulateList(all, currentItemChildren());
            }
        }
        decrementNestingLevel();
    }

    /**
        Used to arm DataTable / DisplayGroup with the full extend of the list to enable
        scroll faulting
     */
    protected void pushFullList ()
    {
        // Todo: can we tell if we changed?
        if (_fullList != null) {
            _useCachedChildren = false;
            _outlineState._index = -1;
            List all = ListUtil.list();
            accumulateList(all, rootList());
            setValueForBinding(all, _fullList);
            _useCachedChildren = true;
        }
    }

    void prepareForPhase ()
    {
        _outlineState._index = -1;
        _start = hasBinding(BindingNames.start) ? intValueForBinding(BindingNames.start) : 0;
        _end = hasBinding(BindingNames.count)
                ? intValueForBinding(BindingNames.count)
                : Integer.MAX_VALUE;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        prepareForPhase();
        super.applyValues(requestContext, this);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        prepareForPhase();
        return super.invokeAction(requestContext, this);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // check for external changes to bindings
        checkBindings();

        pushFullList();

        prepareForPhase();
        // note: don't need to do for other phases - it must come back to -1
        // after each phase to preserve nesting balance.
        // -1 rather than 0 since we preincrement the nesting level in
        // OutlineInnerRepetition
        _outlineState._nestingLevel = -1;
        super.renderResponse(requestContext, component);
        Assert.that((_outlineState._nestingLevel==-1), "Nesting level unbalanced");
    }

    public Object rootList ()
    {
        // force it through outline state so that we sort it
        return _outlineState.displayListForChildren(null, valueForBinding(BindingNames.list));
    }

    public void setCurrentItem (Object item)
    {
        _currentItem = item;  // cache it

        // maintain a stack representing the current object path
        if (_outlineState._currentPath == null) {
            _outlineState._currentPath = ListUtil.list();
        }
        while (_outlineState._currentPath.size() > _outlineState._nestingLevel+1) {
            ListUtil.removeLastElement(_outlineState._currentPath);
        }
        if (_outlineState._nestingLevel >= _outlineState._currentPath.size()) {
            _outlineState._currentPath.add(item);
        } else {
            _outlineState._currentPath.set(_outlineState._nestingLevel, item);
        }

        // push our item
        setValueForBinding(item, AWBindingNames.item);

        _currentItemChildren = null;  // invalidate for lazy recalc

        // recalculate
        // if they don't have a hasChildren binding, use $^children!=null
        _hasChildren = (_hasChildrenBinding != null)
                                ? booleanValueForBinding(_hasChildrenBinding)
                                : (currentItemChildrenCount() > 0);

        if (_expandAll) {
            _isExpanded = _hasChildren;
        }
        else {
            _isExpanded =  _hasChildren &&
                                (_outlineState.isExpanded(_currentItem)
                                   || (_expandCurrentItem != null && booleanValueForBinding(_expandCurrentItem)));
        }

        if (_isExpanded && (_maxLevels != 0) && (_outlineState._nestingLevel >= _maxLevels)) {
            _isExpanded = false;
        }
    }

    public Object currentItem ()
    {
        return _currentItem;
    }

    public Object currentItemChildren ()
    {
        // compute lazily
        if (_currentItemChildren == null) {
            if (_useCachedChildren) {
                _currentItemChildren = _outlineState.lookupListForParent(_currentItem);
            } else {
                _currentItemChildren = _outlineState.displayListForChildren(_currentItem, valueForBinding(_childrenBinding));
            }
        }
        return _currentItemChildren;
    }

    public int currentItemChildrenCount ()
    {
        Object children = currentItemChildren();
        return (children == null) ? 0 : OrderedList.get(children).size(children);
    }

    public boolean isExpanded ()
    {
        return _isExpanded;
    }

    /*
        Called by OutlineInnerRepetition to determine whether the current item
        should be rendered or skipped (i.e. is it in the start<->end renderable range
     */
    public boolean shouldRender ()
    {
        int index = _outlineState._index;
        return ((index >= _start) && (index < _end));
    }

    public Object selectedObject ()
    {
        // for the moment, the tail of the path
        return _outlineState.selectedObject();
    }

    public void setSelectedObject (Object object)
    {
        _outlineState.setSelectedObject(object);
        setValueForBinding(object, BindingNames.selectedObject);
    }

    public AWComponent toggleExpansion ()
    {
        _outlineState.toggleExpansion(_outlineState._currentPath);
        setValueForBinding(_outlineState.expansionPath(), BindingNames.selectionPath);
        setSelectedObject(_currentItem);
        return null;
    }

    public void setSelectionPath (List path)
    {
        _outlineState.setExpansionPath(path);
        setValueForBinding(_outlineState.expansionPath(), BindingNames.selectionPath);
    }

    public int nestingLevel ()
    {
        return _outlineState._nestingLevel;
    }

    public void incrementNestingLevel ()
    {
        _outlineState._nestingLevel++;
    }

    public void decrementNestingLevel ()
    {
        _outlineState._nestingLevel--;
    }

    public boolean hideImages ()
    {
        return _hideImages || ((_showExpansionControl != null) && !booleanValueForBinding(_showExpansionControl));
    }

    public void incrementOutlineIndex()
    {
        _outlineState._index++;
        setValueForBinding(_outlineState._index, BindingNames.outlineIndex);
        // System.out.println("index: " + _outlineState._index);
    }
}

