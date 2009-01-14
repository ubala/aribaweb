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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TableHeaderRow.java#11 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.core.AWXBasicScriptFunctions;

public final class TableHeaderRow extends AWComponent
{
    public static final String SelectAllKey = "sa";
    public static final String UnSelectAllKey = SelectAllKey + "-";
    private static final AWEncodedString SelectAll = AWEncodedString.sharedEncodedString(SelectAllKey);
    private static final AWEncodedString UnSelectAll = AWEncodedString.sharedEncodedString(UnSelectAllKey);
    private static final AWEncodedString OnClickEquals = AWEncodedString.sharedEncodedString(" onClick=\"");
    private static final AWEncodedString Quote = AWEncodedString.sharedEncodedString("\"");
    private static AWEncodedString Zero = AWEncodedString.sharedEncodedString("0");
    private static AWEncodedString HashUrl = AWEncodedString.sharedEncodedString("#");

    public AWEncodedString _elementId;
    private AWBinding _itemBinding;
    private AWBinding _widthBinding;
    private AWBinding _alignBinding;
    private AWBinding _allowsWrappingBinding;
    private AWBinding _sortableBinding;
    private AWBinding _isVisibleBinding;
    private AWBinding _urlBinding;
    private AWBinding _indexBinding;
    private Object _selectedColumn;
    private Object _currentColumn;
    public int _currentIndex;
    public boolean _showSelectAll;
    public boolean _submitForm;
    private boolean _sortDirection;
    // selectAll support
    private AWBinding _allSelectedBinding;
    private AWBinding _selectAllUrlBinding;
    private boolean _firstColumnDisplayed;
    private boolean _allSelected;
    private AWEncodedString _tagId;

    public void awake ()
    {
        //required bindings
        _itemBinding = bindingForName(AWBindingNames.item);
        //optional bindings
        _widthBinding = bindingForName(AWBindingNames.width);
        _alignBinding = bindingForName(BindingNames.align);
        _allowsWrappingBinding = bindingForName(BindingNames.allowsWrapping);
        _urlBinding = bindingForName(AWBindingNames.url);
        _sortableBinding = bindingForName(BindingNames.sortable);
        _isVisibleBinding = bindingForName(AWBindingNames.isVisible);
        _indexBinding = bindingForName(AWBindingNames.index);
        _selectedColumn = valueForBinding(AWBindingNames.selection);

        _allSelectedBinding = bindingForName(BindingNames.allSelected);
        if (_allSelectedBinding != null) {
            _allSelected = booleanValueForBinding(_allSelectedBinding);
            AWBinding showSelectAllBinding = bindingForName(BindingNames.showSelectAll);
            _showSelectAll = (showSelectAllBinding == null) ?
                true : booleanValueForBinding(showSelectAllBinding);
            _selectAllUrlBinding = bindingForName(BindingNames.selectAllUrl);
        }
        else {
            _allSelected = false;
            _showSelectAll = false;
            _selectAllUrlBinding = null;
        }
        _submitForm = booleanValueForBinding(AWBindingNames.submitForm);
        _firstColumnDisplayed = false;
        _tagId = null;
        _sortDirection = booleanValueForBinding(BindingNames.sortDirection);
    }

    public String hspace ()
    {
        return isBrowserMicrosoft() ? "4" : "3";
    }

    public void setCurrentColumn (Object currentColumn)
    {
        setValueForBinding(currentColumn, _itemBinding);
        _currentColumn = currentColumn;
    }

    public void setCurrentIndex (int index)
    {
        setValueForBinding(index, _indexBinding);
        _currentIndex = index;
    }

    public boolean isCurrentColumnVisible ()
    {
        return (_isVisibleBinding == null) ?
            true : booleanValueForBinding(_isVisibleBinding);
    }

    public AWEncodedString currentWidth ()
    {
        return encodedStringValueForBinding(_widthBinding);
    }

    public AWEncodedString currentAlign ()
    {
        return encodedStringValueForBinding(_alignBinding);
    }

    public String currentAllowsWrapping ()
    {
        return booleanValueForBinding(_allowsWrappingBinding) ? null : AWBindingNames.awstandalone;
    }

    public Object currentUrl ()
    {
        return urlForBinding(_urlBinding);
    }

    public String onClickSort ()
    {
        if (_submitForm) {
            AWRequestContext requestContext = requestContext();
            AWResponse response = requestContext.response();
            response.appendContent(OnClickEquals);
                // This covers case where we're not using AWForm
            int index = _currentIndex + 1;
            if (_sortDirection) {
                index = -index;
            }
            AWXBasicScriptFunctions.appendSubmitFormAtIndex
                (response, Zero, tagId(),
                 AWEncodedString.sharedEncodedString(AWUtil.toString(index)));

            response.appendContent(Quote);
        }
        return null;
    }

    public AWEncodedString tagId ()
    {
        if (_tagId == null) {
            _tagId = encodedStringValueForBinding(BindingNames.tagId);
        }
        return _tagId;
    }

    public boolean isCurrentColumnSortable ()
    {
        return booleanValueForBinding(_sortableBinding);
    }

    public boolean isCurrentColumnSelected ()
    {
        return (_selectedColumn == _currentColumn);
    }

    public String sortDirectionFilename ()
    {
        return _sortDirection ? "widg/ascending.gif" : "widg/descending.gif";
    }

    public String sortDirectionHint ()
    {
        return _sortDirection ?
            localizedJavaString(1, "Sorted ascending (click to reverse)" /*  */) :
            localizedJavaString(2, "Sorted descending (click to reverse)" /*  */);
    }

    public String headStyle ()
    {
        return "tableHead";
    }

    public AWResponseGenerating sortClicked ()
    {
        setValueForBinding(_currentColumn, AWBindingNames.selection);
        return (AWResponseGenerating)valueForBinding(BindingNames.sortAction);
    }

    ////////////////
    // Select All
    ////////////////
    public Object selectAllUrl ()
    {
        return urlForBinding(_selectAllUrlBinding);
    }

    public String onClickSelectAll ()
    {
        if (_submitForm) {
            AWRequestContext requestContext = requestContext();
            AWResponse response = requestContext.response();
            response.appendContent(OnClickEquals);
                // This covers case where we're not using AWForm
            AWXBasicScriptFunctions.appendSubmitFormAtIndex(response, Zero, tagId(),
                                                            _allSelected ? UnSelectAll : SelectAll);
            response.appendContent(Quote);
        }
        return null;
    }

    public String checkboxFilename ()
    {
        return _allSelected ? "widg/checkedbox.gif" : "widg/checkbox.gif";
    }

    public AWResponseGenerating selectAllClicked ()
    {
        boolean allSelected = _allSelected;
        setValueForBinding(!allSelected, _allSelectedBinding);
        return null;
    }

    public boolean displaySeparator ()
    {
        if (_firstColumnDisplayed) {
            return true;
        }
        _firstColumnDisplayed = true;
        return false;
    }

    private Object urlForBinding (AWBinding urlBinding)
    {
        Object currentUrl = null;
        if (_submitForm) {
            currentUrl = HashUrl;
        }
        else {
            currentUrl = encodedStringValueForBinding(urlBinding);
            if (currentUrl == null) {
                currentUrl = AWComponentActionRequestHandler.SharedInstance.urlWithSenderId(requestContext(), _elementId);
            }
        }
        return currentUrl;
    }
}
