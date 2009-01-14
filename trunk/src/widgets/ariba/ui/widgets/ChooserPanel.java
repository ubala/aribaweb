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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ChooserPanel.java#14 $
*/


package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.table.AWTDisplayGroup;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValueException;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.ArrayList;

public class ChooserPanel extends AWComponent
{
    public AWTDisplayGroup _displayGroup;
    public AWTDisplayGroup _selectedDisplayGroup;
    public ChooserState _chooserState;
    private ChooserSelectionSource _selectionSource;
    public Object /*AWFormatting*/ _formatter;

    public boolean isStateless() {

        return false;
    }

    public boolean isClientPanel() {
        return true;
    }

    public void setup (ChooserState chooserState,
                       ChooserSelectionSource selectionSource,
                       Object /*AWFormatting*/ formatter)
    {
        _chooserState = chooserState;
        _selectionSource = selectionSource;
        _displayGroup = new AWTDisplayGroup();
        _selectedDisplayGroup = new AWTDisplayGroup();
        _formatter = formatter;
        List initialSelectedObjects = _chooserState.selectedObjects();
        if (initialSelectedObjects != null) {
            _selectedDisplayGroup.setObjectArray(initialSelectedObjects);
            _selectedDisplayGroup.setSelectedObjects(initialSelectedObjects);
        }
        if (!_chooserState.hasChanged()) {
            _chooserState.setPattern("");
        }
        search();
    }

    public String currentItemString ()
    {
        Object currentItem = _chooserState.currentItem();
        return (_formatter == null)
            ? currentItem.toString()
            : AWFormatting.get(_formatter).format(_formatter, currentItem, preferredLocale());
    }

    public AWComponent searchClicked ()
    {
        search();
        return null;
    }

    private void search ()
    {
        List matches = _selectionSource.match(_chooserState.pattern(), Integer.MAX_VALUE);
        _chooserState.setMatches(matches);
        removeSelected(matches);
        _displayGroup.setObjectArray(matches);
    }

    private void removeSelected (List matches)
    {
        List selectedObjects = _chooserState.selectedObjects();
        if (selectedObjects != null) {
            int size = selectedObjects.size();
            for (int i = 0; i < size; i++) {
                matches.remove(selectedObjects.get(i));
            }
        }
    }

    public AWComponent okClicked ()
    {
        // Adds
        List newlySelectedObjects = _displayGroup.selectedObjects();
        List initialSelectedObjects = new ArrayList(_selectedDisplayGroup.allObjects());
        int size = newlySelectedObjects.size();
        for (int i = 0; i < size; i++) {
            _chooserState.setSelectionState(newlySelectedObjects.get(i), true);
        }
        List selectedObjects = _selectedDisplayGroup.selectedObjects();

        // Removes
        int i = initialSelectedObjects.size();
        while (i-- > 0) {
            Object initialSelectedObject = initialSelectedObjects.get(i);
            if (selectedObjects.indexOf(initialSelectedObject) < 0) {
                _chooserState.setSelectionState(initialSelectedObject, false);
            }
        }

        _chooserState.setIsInvalid(false);
        return ModalPageWrapper.instance(this).returnPage();
    }

    public String title ()
    {
        return _chooserState.multiSelect() ?
            localizedJavaString(1, "Choose Values") : localizedJavaString(2, "Choose Value"); 
    }

    public int searchFieldSize ()
    {
        return ModalWindowWrapper.isInModalWindow(this) ? 20 : 50;
    }
}
