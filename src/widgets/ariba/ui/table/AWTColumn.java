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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTColumn.java#38 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.util.core.Compare;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldPath;

import java.util.Map;

public class AWTColumn extends AWTDataTable.Column
{
    // public, but only for the use of AWTDataTable...
    public AWBinding      _labelBinding;
    public AWBinding      _styleBinding;
    public AWBinding      _classBinding;
    public AWBinding      _widthBinding;
    public AWBinding      _minWidthPxBinding;
    public AWBinding      _alignBinding;
    public AWBinding      _vAlignmentBinding;
    public AWBinding      _formatterBinding;
    public AWBinding      _nowrapBinding;
    public AWBinding      _noSortBinding;
    public AWBinding      _sortCaseSensitivelyBinding;
    public AWBinding      _sortKeyBinding;
    public AWBinding      _sortOrderingBinding;
    public FieldPath      _keyPath;
    public AWBinding      _actionBinding;
    public AWBinding      _initiallyVisibleBinding;
    public AWBinding      _isVisibleBinding;
    public AWBinding      _isValueColumnBinding;
    public AWBinding      _wantSpanBinding;
    public AWBinding      _isBlankBinding;
    public AWBinding      _showColumnLabelBinding;
    public AWBinding      _noGroupBinding;
    public AWBinding      _isOptionalBinding;
    public AWBinding      _pivotMoveType;
    public AWBinding      _renderValueInLabelColumnBinding;
    public FieldPath      _uniquingKeyPath;

    public static AWTColumn createBlankColumn ()
    {
        AWTColumn col = new AWTColumn();
        col.init(null, "", null, null, null);
        col._isBlankBinding =
            AWBinding.bindingWithNameAndConstant(BindingNames.isBlank,
                                                 Boolean.TRUE);
        return col;
    }

    public static AWTColumn createPlaceholderColumn ()
    {
        AWTColumn col = new AWTColumn();
        col.init(null, "", null, null, null);
        col._isValueColumnBinding =
            AWBinding.bindingWithNameAndConstant(BindingNames.isValueColumn,
                                                 Boolean.TRUE);
        return col;
    }

    public String rendererComponentName ()
    {
        return AWTColumnRenderer.class.getName();
    }

    public void initializeColumn (AWTDataTable table)
    {
        if ((_isVisibleBinding == null) ||
            (table.booleanValueForBinding(_isVisibleBinding)))
        {
            // register as a data column
            table.registerColumn(this);
        }
    }

    public boolean isValueColumn (AWTDataTable sender)
    {
        if (_isValueColumnBinding != null) {
            return sender.booleanValueForBinding(_isValueColumnBinding);
        }
        return _keyPath != null;
    }

    public boolean hasNullValue (Object object)
    {
        return (object != null) && (_keyPath != null)
                && (_keyPath.getFieldValue(object) == null);
    }

    public boolean isSortableColumn (AWTDataTable sender)
    {
        if (_noSortBinding != null) {
            return !sender.booleanValueForBinding(_noSortBinding);
        }
        return super.isSortableColumn(sender);
    }

    public boolean isGroupableColumn (AWTDataTable sender)
    {
        if (_noGroupBinding != null) {
            return !sender.booleanValueForBinding(_noGroupBinding);
        }
        return super.isGroupableColumn(sender);

    }

    public boolean isOptional (AWTDataTable table)
    {
        return  (_isOptionalBinding != null)
                    ? table.booleanValueForBinding(_isOptionalBinding)
                    : (label(table) != null);
    }

    public String pivotMoveType (AWTDataTable table)
    {
        return (_pivotMoveType != null)
                ? (String)table.valueForBinding(_pivotMoveType)
                : super.pivotMoveType(table);
    }

    public boolean renderValueInLabelColumn (AWTDataTable table)
    {
        return  (_renderValueInLabelColumnBinding != null) && table.booleanValueForBinding(_renderValueInLabelColumnBinding);
    }

    public boolean initiallyVisible (AWTDataTable sender)
    {
        if (_initiallyVisibleBinding == null) {
            return true;
        }
        else {
            return sender.booleanValueForBinding(_initiallyVisibleBinding);
        }
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        _labelBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.label);
        _widthBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.width);
        _minWidthPxBinding = (AWBinding)bindingsHashtable.remove("minWidthPx");
        _styleBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.style);
        _classBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.classBinding);
        _formatterBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.formatter);
        _nowrapBinding = (AWBinding)bindingsHashtable.remove(BindingNames.nowrap);
        _noSortBinding = (AWBinding)bindingsHashtable.remove("nosort");
        _sortCaseSensitivelyBinding = (AWBinding)bindingsHashtable.remove("sortCaseSensitively");
        _sortKeyBinding = (AWBinding)bindingsHashtable.remove("sortKey");
        _sortOrderingBinding = (AWBinding)bindingsHashtable.remove("sortOrdering");
        _noGroupBinding = (AWBinding)bindingsHashtable.remove("nogroup");

        AWBinding binding = (AWBinding)bindingsHashtable.remove(AWBindingNames.key);
        if (binding != null) {
            String keyString = binding.stringValue(null);
            if (keyString != null) {
                _keyPath = FieldPath.sharedFieldPath(keyString);
            }
        }

        binding = (AWBinding)bindingsHashtable.remove("uniquingKeyPath");
        if (binding != null) {
            setUniquingKeyPath(binding.stringValue(null));
        }
        
        _alignBinding = (AWBinding)bindingsHashtable.remove("align");
        _vAlignmentBinding = (AWBinding)bindingsHashtable.remove("valign");
        _actionBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.action);
        _initiallyVisibleBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.initiallyVisible);
        _isVisibleBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.isVisible);
        _isValueColumnBinding = (AWBinding)bindingsHashtable.remove(BindingNames.isValueColumn);
        _wantSpanBinding = (AWBinding)bindingsHashtable.remove(BindingNames.wantsSpan);
        _isBlankBinding = (AWBinding)bindingsHashtable.remove(BindingNames.isBlank);
        _showColumnLabelBinding = (AWBinding)bindingsHashtable.remove(BindingNames.showColumnLabel);
        _isOptionalBinding = (AWBinding)bindingsHashtable.remove(BindingNames.isOptional);
        _pivotMoveType = (AWBinding)bindingsHashtable.remove("pivotMoveType");
        _renderValueInLabelColumnBinding = (AWBinding)bindingsHashtable.remove("renderValueInLabelColumn");

        super.init(tagName, bindingsHashtable);
    }

    /** Convenience for initing a dynamic column */
    public void init (String keyPath, String label, String formatterName, String align, Map others)
    {
        Map bindings = (others != null) ? MapUtil.cloneMap(others) : MapUtil.map();
        bindings.put(AWBindingNames.key, AWBinding.bindingWithNameAndConstant(AWBindingNames.key, keyPath));
        bindings.put(AWBindingNames.label, AWBinding.bindingWithNameAndConstant(AWBindingNames.label, label));
        bindings.put("align", AWBinding.bindingWithNameAndConstant("align", align));
        if (formatterName != null) {
                bindings.put(AWBindingNames.formatter, AWBinding.bindingWithNameAndKeyPath(AWBindingNames.formatter, "formatters."+formatterName));
        }
        init ("AWTColumn", bindings);
    }

    public String keyPathString ()
    {
        return (_keyPath != null) ? _keyPath.fieldPathString() : null;
    }


    public AWTSortOrdering createSortOrdering(AWTDataTable table) {
        if (_noSortBinding != null && table.booleanValueForBinding(_noSortBinding)) return null;
        if (_sortOrderingBinding != null) return (AWTSortOrdering)table.valueForBinding(_sortOrderingBinding);

        String key = (_sortKeyBinding != null) ? table.stringValueForBinding(_sortKeyBinding) : null;
        if (key == null) key = keyPathString();
        if (key == null) return null;
        AWTSortOrdering ordering = AWTSortOrdering.sortOrderingWithKey(key, AWTSortOrdering.CompareCaseInsensitiveAscending);
        prepareSortOrdering(table, ordering);
        return ordering;
    }

    public void prepareSortOrdering (AWTDataTable table, AWTSortOrdering ordering)
    {
        Object formatter = table.valueForBinding(_formatterBinding);
        if (formatter != null && formatter instanceof Compare) ordering.setComparator((Compare)formatter);
    }


    public Object valueForRow (Object target)
    {
        return _keyPath.getFieldValue(target);
    }

    public String label (AWTDataTable table)
    {
        return (_labelBinding != null) ? table.stringValueForBinding(_labelBinding) :
                 ((_keyPath != null) ? _keyPath.fieldPathString() : null);
    }

    public boolean wantsSpan (AWTDataTable table)
    {
        return (_wantSpanBinding != null) ? table.booleanValueForBinding(_wantSpanBinding) : false;
    }

    public boolean isBlank (AWTDataTable table)
    {
        return (_isBlankBinding != null) ? table.booleanValueForBinding(_isBlankBinding) : false;
    }

    protected void setUniquingKeyPath (String pathString)
    {
        _uniquingKeyPath = (pathString != null) ? FieldPath.sharedFieldPath(pathString) : null;
    }
    // Pivot grouping property -- consecutive columns sharing equals() values will be merged
    public Object uniquingValue (AWTDataTable sender, Object item)
    {
        return (_uniquingKeyPath != null) ? _uniquingKeyPath.getFieldValue(item) : super.uniquingValue(sender, item);
    }

    public boolean showColumnLabel (AWTDataTable table)
    {
        return (_showColumnLabelBinding != null) ? table.booleanValueForBinding(_showColumnLabelBinding) : true;
    }

    public int minWidthPx (AWTDataTable table)
    {
        return (_minWidthPxBinding != null) ? table.intValueForBinding(_minWidthPxBinding) : 0;
    }
}
