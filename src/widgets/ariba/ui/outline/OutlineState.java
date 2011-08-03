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

    $Id: //ariba/platform/ui/widgets/ariba/ui/outline/OutlineState.java#3 $
*/
package ariba.ui.outline;

import java.util.List;
import ariba.util.core.ListUtil;
import ariba.util.core.EqHashtable;
import java.util.Map;
import ariba.ui.table.AWTDisplayGroup;

public final class OutlineState
{
    public AWTDisplayGroup _displayGroup;
    public List _crumbTrailPath;
    public List _expansionPath;
    protected Object _selectedObject;
    protected Map _childrenForObject;
    protected boolean _allowMultiPath = false;
    protected Map _expansionStates;
    protected boolean _defaultExpansionState = false;  // i.e. closed
    protected boolean _didExecuteCollapseAll = false;

    // state for current rendering -- pushed through by OutlineRepetition
    protected int _nestingLevel;
    protected List _currentPath;
    protected int _index;
    protected int _lastIndentationPx;

    public OutlineState()
    {
        _expansionStates = new EqHashtable();
    }

    public List expansionPath ()
    {
        if (_expansionPath == null) {
            _expansionPath = ListUtil.list();
        }
        return _expansionPath;
    }

    public void setExpansionPath (List path)
    {
        _expansionPath = path;
        for (int i=0,c=expansionPath().size(); i<c; i++) {
            setExpansionState(_expansionPath.get(i), true);
        }
    }

    public boolean defaultExpansionState ()
    {
        return _defaultExpansionState;    
    }

    public void expandAll ()
    {
        _expansionStates.clear();
        _defaultExpansionState = true;
    }

    public void collapseAll ()
    {
        _expansionStates.clear();
        _defaultExpansionState = false;
        _didExecuteCollapseAll = true;
    }

    public boolean didExecuteCollapseAll () 
    {
        return _didExecuteCollapseAll;
    }
    
    public void setDidExecuteCollapseAll (boolean didExecuteCollapseAll) 
    {
        _didExecuteCollapseAll = didExecuteCollapseAll; 
    }
    
    public Object selectedObject ()
    {
        return _selectedObject;
    }

    public void setSelectedObject (Object obj)
    {
        _selectedObject = obj;
    }

    public List currentPath ()
    {
        return _currentPath;
    }

    public void setDisplayGroup (AWTDisplayGroup displayGroup)
    {
        _displayGroup = displayGroup;
        invalidateSortState();
    }

    public void invalidateSortState ()
    {
        _childrenForObject = null;  // invalidate sort cache
    }

    public boolean isExpanded (Object o)
    {
        Boolean value = (Boolean)_expansionStates.get(o);
        return (value != null) ? value.booleanValue() : _defaultExpansionState;
    }

    public int nestingLevel ()
    {
        return _nestingLevel;
    }

    public int lastIndentationPx()
    {
        return _lastIndentationPx;
    }

    public void setLastIndentationPx (int lastIndentationPx)
    {
        _lastIndentationPx = lastIndentationPx;
    }
    
    public void toggleExpansion (List currentPath)
    {
        Object item = ListUtil.lastElement(currentPath);
        boolean newState = !isExpanded(item);
        setExpansionState(item, newState);

        List expansionPath = ListUtil.cloneList(currentPath);
        if (newState == false) {
            ListUtil.removeLastElement(expansionPath);
        }
        setExpansionPath(expansionPath);
    }

    // NOTE: children and return value are AWOrderedLists
    public SortedListRecord listRecordForParent (Object parent)
    {
        if (parent == null) parent = this;

        if (_childrenForObject == null) {
            _childrenForObject = new EqHashtable();
        }

        SortedListRecord rec = (SortedListRecord)_childrenForObject.get(parent);
        return rec;
    }


    public Object lookupListForParent (Object parent)
    {
        SortedListRecord rec = listRecordForParent(parent);
        return (rec != null) ? rec.list() : null;
    }

    public Object displayListForChildren (Object parent, Object children)
    {
        Object returnList = null;

        SortedListRecord rec = listRecordForParent(parent);

        // check if we have a cached record and its list is still valid
        if ((children != null) && ((rec == null) || ((returnList = rec.listForOrig(children)) == null))) {
            // compute new sorted list and cache it
            if (_displayGroup != null) {
                List origVector = AWTDisplayGroup.vectorFromOrderedList(children);
                returnList = _displayGroup.sortedChildList(origVector);
            } else {
                // Copy?
                returnList = children;
            }
            rec = new SortedListRecord(children, returnList);
            if (parent == null) parent = this;
            _childrenForObject.put(parent, rec);
        }
        return returnList;
    }

    public void setExpansionState (Object o, boolean isOpen)
    {
        if (isOpen == _defaultExpansionState) {
            _expansionStates.remove(o);
        } else {
            _expansionStates.put(o, ((isOpen) ? Boolean.TRUE : Boolean.FALSE));
        }
    }

    /** Cache record for sorted child arrays */
    static class SortedListRecord
    {
        protected Object[] _copy;
        protected Object _processed;
        // checksum hashcodes as a way to detect object substitution?

        public SortedListRecord (Object orig, Object processed) {
            if (orig != null) {
                _copy = AWTDisplayGroup.arrayFromOrderedList(orig);
            }
            _processed = processed;
        }

        /** Returns null if cached value is invalid */
        Object listForOrig (Object orig)
        {
            if (AWTDisplayGroup.orderedListArrayMatch(orig, _copy)) {
                return list();
            }
            return null;
        }

        Object list ()
        {
            return _processed;
        }
    }
}
