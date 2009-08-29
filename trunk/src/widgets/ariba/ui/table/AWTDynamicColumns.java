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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTDynamicColumns.java#25 $
*/

package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.util.AWUtil;
import java.util.List;
import java.util.Map;

/*
    A pseudo column that can be used to dynamically create a data-driven set of columns (e.g. one column for each
    supplier in a sourcing event.

    Example usages:
    I.e. you can do something as simple as this:
        <AWTDynamicColumns list="$chosenCols" item="$currentColName"  key="$currentColName"/>
    in which case a column with the given key will be created for each item in the list.

    Or, you can do something like this:
    <AWTDynamicColumns list="$chosenCols" item="$currentColName" label="$currentColName">
            <AWSwtichComponent awcomponentName="$componentForCurColumn" key="$curentColName"/>
        </AWTDynamicColumns>

   Or,
        <AWTDynamicColumns list="$chosenCols" item="$currentColName" label="$currentColName" key="$currentColName">
            <!--- body template used for each dynamically created column -->
            <AWIf isEqual="$currentColName" value="Amount">
                <AWTextField value="$currentColValue" formatter="$formatters.moneyFormatter"/>
            <AWElse/>
                $currentColName: <AWString value="$currentColValue"/>
            </AWIf>
        </AWTDynamicColumns>

    Essentially, the list / item bindings are evaluated once at table initialization time to create the list of columns,
    and then, during rendering, the item is pushed and the body evaluated for each cell.
*/
public final class AWTDynamicColumns extends AWTDataTable.Column
{
    // public, but only for the use of AWTDataTable...
    public AWBinding      _listBinding;
    public AWBinding      _itemBinding;
    public AWBinding      _labelBinding;
    public AWBinding      _keyBinding;
    public AWBinding      _styleBinding;
    public AWBinding      _showColumnLabelBinding;
    public AWBinding      _isBlankBinding;
    public AWBinding      _initiallyVisibleBinding;
    public AWBinding      _noSortBinding;
    public AWBinding      _sortOrderingBinding;
    public AWBinding      _pivotMoveType;
    public AWBinding      _isOptionalBinding;
    public AWBinding      _isValueColumnBinding;
    public AWBinding      _nowrapBinding;
    public AWBinding      _wantsSpanBinding;
    public AWBinding      _actionBinding;
    public AWBinding      _isVisibleBinding;
    public AWBinding      _formatterBinding;
    public AWBinding      _uniquingKeyPathBinding;

    public void init (String tagName, Map bindingsHashtable)
    {
        _listBinding = (AWBinding)bindingsHashtable.remove(BindingNames.list);
        _itemBinding = (AWBinding)bindingsHashtable.remove(BindingNames.item);
        _labelBinding = (AWBinding)bindingsHashtable.remove(BindingNames.label);
        _keyBinding = (AWBinding)bindingsHashtable.remove(BindingNames.key);
        _styleBinding = (AWBinding)bindingsHashtable.remove(BindingNames.style);
        _showColumnLabelBinding = (AWBinding)bindingsHashtable.remove(
            BindingNames.showColumnLabel);
        _isBlankBinding = (AWBinding)bindingsHashtable.remove(BindingNames.isBlank);
        _initiallyVisibleBinding = (AWBinding)bindingsHashtable.remove(
            BindingNames.initiallyVisible);
        _noSortBinding = (AWBinding)bindingsHashtable.remove("nosort");
        _sortOrderingBinding = (AWBinding)bindingsHashtable.remove("sortOrdering");
        _isOptionalBinding = (AWBinding)bindingsHashtable.remove(BindingNames.isOptional);
        _pivotMoveType = (AWBinding)bindingsHashtable.remove("pivotMoveType");
        _isValueColumnBinding = (AWBinding)bindingsHashtable.remove(
            BindingNames.isValueColumn);
        _nowrapBinding = (AWBinding)bindingsHashtable.remove(BindingNames.nowrap);
        _wantsSpanBinding = (AWBinding)bindingsHashtable.remove(BindingNames.wantsSpan);
        _actionBinding = (AWBinding)bindingsHashtable.remove(BindingNames.action);
        _isVisibleBinding = (AWBinding)bindingsHashtable.remove(BindingNames.isVisible);
        _formatterBinding = (AWBinding)bindingsHashtable.remove(BindingNames.formatter);
        _uniquingKeyPathBinding = (AWBinding)bindingsHashtable.remove("uniquingKeyPath");

        super.init(tagName, bindingsHashtable);
    }

    public String rendererComponentName ()
    {
        // we are never rendered
        return null;
    }

    public void initializeColumn (AWTDataTable table)
    {
        List list = (List)table.valueForBinding(_listBinding);
        if (list == null) {
            return;
        }


        // iterate through column list, pushing item, and pulling data to initialize our column
        for (int i=0, c=list.size(); i<c; i++) {
            // push item
            Object item = list.get(i);
            table.setValueForBinding(item, _itemBinding);

            String key =  ((String)((_keyBinding != null) ? table.valueForBinding(_keyBinding): null));

            // see if an old copy can be recovered from in purgatory
            DynamicColumn col = (DynamicColumn)table.purgatoryMatch(key);

            if (col == null) {
                col = new DynamicColumn();
                col.setContentElement(this.contentElement());
                col.init("AWTColumn", AWUtil.map(AWBindingNames.key,
                    AWBinding.bindingWithNameAndConstant(AWBindingNames.key, key),
                    AWBindingNames.label, _labelBinding));
                col._item = item;
                col._parentColumn = this;
                col._styleBinding = _styleBinding;
                col._showColumnLabelBinding = _showColumnLabelBinding;
                col._isBlankBinding = _isBlankBinding;
                col._initiallyVisibleBinding = _initiallyVisibleBinding;
                col._noSortBinding = _noSortBinding;
                col._sortOrderingBinding = _sortOrderingBinding;
                col._pivotMoveType = _pivotMoveType;
                col._isValueColumnBinding = _isValueColumnBinding;
                col._nowrapBinding = _nowrapBinding;
                col._wantSpanBinding = _wantsSpanBinding;
                col._actionBinding = _actionBinding;
                col._isVisibleBinding = _isVisibleBinding;
                col._formatterBinding = _formatterBinding;
                col.setUniquingKeyPath(table.stringValueForBinding(_uniquingKeyPathBinding));
            }
            table.registerColumn(col);                
        }
    }

    protected static class DynamicColumn extends AWTColumn
    {
        protected Object            _item;
        protected AWTDynamicColumns _parentColumn;

        public void prepare (AWTDataTable table)
        {
            // called by renderer just prior to rendering -- push our item
            table.setValueForBinding(_item, _parentColumn._itemBinding);
        }

        public Object purgatoryKey ()
        {
            return keyPathString();
        }        
    }
}
