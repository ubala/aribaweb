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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ChooserState.java#10 $
*/

package ariba.ui.widgets;

import ariba.util.core.ListUtil;

import java.util.List;

public class ChooserState
{
    private ChooserSelectionState _selectionState;
    private Object _currentItem;
    private List _filteredSelections;
    private String _pattern;
    private List _matches;
    private List _recentSelectedObjects;
    private int _recentSelectedDisplayed;
    private boolean _focus;
    private boolean _render;
    private boolean _isInvalid;
    private boolean _multiSelect;
    private boolean _addMode;

    public ChooserState ()
    {
        _selectionState = new SelectionState();
    }

    public ChooserState (ChooserSelectionState selectionState)
    {
        _selectionState = selectionState;
    }

    public void setCurrentItem (Object item)
    {
        _currentItem = item;
    }

    public Object currentItem ()
    {
        return _currentItem;
    }

    public void setSelectionState (ChooserSelectionState selectionState)
    {
        _selectionState = selectionState;
    }

    public void updateSelectedObjects ()
    {
        updateSelectedObjects(_currentItem);
    }

    public void updateSelectedObjects (Object item)
    {
        if (!_multiSelect) {
            setSelectionState(item, true);
        }
        else {
            Object selectedObject = selectedObject();
            List selectedObjects = selectedObjects();
            if (_addMode) {
                if (_isInvalid) {
                    if (selectedObject != null) {
                        setSelectionState(selectedObject, false);
                    }
                }
                setSelectionState(item, !selectedObjects.contains(item));
            }
            else {
                if (selectedObject != null) {
                    setSelectionState(selectedObject, false);
                }
                setSelectionState(item, true);
            }
        }
    }

    public void setSelectionState(Object selection, boolean selected)
    {
        if (selection != null) {
            _selectionState.setSelectionState(selection, selected);
        }
    }

    /**
        The most recent selection.  Null if last action was a deselection.
    */
    public Object selectedObject ()
    {
        return _selectionState.selectedObject();
    }

    public List selectedObjects ()
    {
        return _selectionState.selectedObjects();
    }

    public boolean isSelectedItem ()
    {
        return _selectionState.isSelected(_currentItem);
    }

    public List recentSelectedObjects ()
    {
        if (_recentSelectedObjects == null) {
            _recentSelectedObjects = ListUtil.list();
            List selectedObjects = selectedObjects();
            int size = selectedObjects.size();
            int maxCount = Chooser.MaxRecentSelected;
            if (size > Chooser.MaxRecentSelected) {
                maxCount -= 1;
            }
            for (int i = size - 1; i >= 0 && _recentSelectedDisplayed < maxCount; i--) {
                Object selection = selectedObjects.get(i);
                _recentSelectedObjects.add(selection);
                _recentSelectedDisplayed++;
            }
        }
        return _recentSelectedObjects;
    }

    public int recentSelectedDisplayed ()
    {
        return _recentSelectedDisplayed;
    }

    public void clearRecentSelectedObjects ()
    {
        _recentSelectedObjects = null;
        _recentSelectedDisplayed = 0;
    }

    public Object displayObject ()
    {
        return selectedObject();
    }

    public void setDisplayObject (Object displayObject)
    {
        return;
    }

    public void setPattern (String pattern)
    {
        _pattern = pattern;
    }

    public String pattern ()
    {
        return _pattern;
    }

    private boolean _hasChanged = false;
    public void hasChanged (boolean flag)
    {
        _hasChanged = flag;
    }

    public boolean hasChanged ()
    {
        return _hasChanged;
    }

    public void setMatches (List matches)
    {
        _matches = matches;
    }

    public List matches ()
    {
        return _matches;
    }

    public void setFilteredSelections (List filteredSelections)
    {
        _filteredSelections = filteredSelections;
    }

    public List filteredSelections ()
    {
        return _filteredSelections;
    }

    public void setFocus (boolean focus)
    {
        _focus = focus;
    }

    public boolean focus ()
    {
        return  _focus;
    }

    public void setRender (boolean render)
    {
        _render = render;
    }

    public boolean render ()
    {
        return  _render;
    }

    public void setIsInvalid (boolean isInvalid)
    {
        _isInvalid = isInvalid;
    }

    public boolean isInvalid ()
    {
        return _isInvalid;
    }

    public void setMultiSelect (boolean multiSelect)
    {
        _multiSelect = multiSelect;
    }

    public boolean multiSelect ()
    {
        return _multiSelect;
    }

    public void setAddMode (boolean addMode)
    {
        _addMode = addMode;
    }

    public boolean addMode ()
    {
        return _addMode;
    }

    /**
        Default selection state
     */
    private class SelectionState implements ChooserSelectionState
    {
        private Object _selectedObject;
        private List _selectedObjects;

        public Object selectedObject() {
            return _selectedObject;
        }

        public List selectedObjects() {
            if (_selectedObjects == null) {
                _selectedObjects = ListUtil.list();
            }
            return _selectedObjects;
        }

        public void setSelectionState (Object selection, boolean selected)
        {
            if (selected) {
                _selectedObject = selection;
                if (_multiSelect) {
                    _selectedObjects.add(selection);
                }
            }
            else {
                if (_multiSelect) {
                    _selectedObjects.remove(selection);
                    _selectedObject = _selectedObjects.size() > 0 ? ListUtil.lastElement(_selectedObjects) : null;
                }
                else {
                    _selectedObject = null;
                }
            }
        }

        public boolean isSelected (Object selection)
        {
            boolean isSelected = false;
            if (_multiSelect) {
                isSelected = _selectedObjects.contains(selection);
            }
            else {
                if (_selectedObject != null) {
                    isSelected = _selectedObject == selection;
                }
            }
            return isSelected;
        }
    }

}
