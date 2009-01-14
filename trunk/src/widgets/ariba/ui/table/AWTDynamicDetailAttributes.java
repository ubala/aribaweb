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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTDynamicDetailAttributes.java#5 $
*/

package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBareString;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWElement;

import java.util.List;
import java.util.Map;

/*
    A pseudo column that can be used to dynamically create a data-driven set of Pivot Table
    detail attributes.

    Example usages:
        - Support <AWTDynamicDetailAttributes list="$attachmentAttrs" item="$curAttr" renderingColumnKey="REQRenderer"/>
            - Impl: when rendering detail attrs, for one of these:
                - iterate across object in group building list of attrLists
                - while < lists.max(size) render via rendering column for each value

    Note: we subclass AWT column to pick up label rendering behavior, as well as the
    uniquingValue support.  However, we (almost) never render ourselves: we generally
    proxy through to our "renderingColumn" -- if we do render ourself it is as a
    blank (but with special spanning behavior -- see below).
*/
public class AWTDynamicDetailAttributes extends AWTColumn
{
     // public, but only for the use of AWTDataTable...
    public AWBinding      _listBinding;
    public AWBinding      _itemBinding;
    public AWBinding      _renderingColumnKeyBinding;
    protected static AWElement _BlankElement =  AWBareString.getInstance("");

    public void init (String tagName, Map bindingsHashtable)
    {
        _listBinding = (AWBinding)bindingsHashtable.remove(BindingNames.list);
        _itemBinding = (AWBinding)bindingsHashtable.remove(BindingNames.item);
        _renderingColumnKeyBinding = (AWBinding)bindingsHashtable.remove("renderingColumnKey");
        super.init(tagName, bindingsHashtable);
    }

    public DetailIterator prepare (AWTDataTable table, AWTPivotState.PivotGroup group)
    {
        Object origItem = table.currentItem();
        int count = group.slotCount();
        List[] lists = null;

        for (int i=0; i<count; i++) {
            Object o = group.get(i);
            if (o != null) {
                table.setCurrentItem(o);
                List l = (List)table.valueForBinding(_listBinding);
                // allocate lazily to deal with the common case of no detail attributes
                if (l != null && l.size() > 0) {
                    if (lists == null) lists = new List[count];
                    lists[i] = l;
                }
            }
        }
        table.setCurrentItem(origItem);
        return (lists != null) ? new DetailIterator(lists) : null;
    }

    // The real column to use for rendering
    public AWTDataTable.Column prepareAndReplace(AWTDataTable table)
    {
        AWTDataTable.Column orig = table._originalCurrentColumn;
        // Using "this" as "blank" column -- see ** note below...
        if (!(orig instanceof AWTPivotState.PivotEdgeColumn)) return this;
        AWTPivotState.PivotEdgeColumn edgeCol = (AWTPivotState.PivotEdgeColumn)orig;

        // Get current slot, push current value, lookup replacement column
        int slot = edgeCol.edgeCell().mappedSlot((AWTPivotState.PivotGroup)table.displayGroup().currentGroupingState());
        Object object = table.pivotState()._rowDetailIterator.columnStateForSlot(slot);
        if (object == null) return this;

        table.setValueForBinding(object, _itemBinding);
        String replacementName = (String)table.valueForBinding(_renderingColumnKeyBinding);
        AWTDataTable.Column replacement = (replacementName != null) ? table.findColumnForKey(replacementName) : null;
        return (replacement != null) ? replacement.prepareAndReplace(table) : this;
    }

    /*
        ** Note: Using this as "Blank" data column.
        Okay, this is... subtle:
        When we are missing a value (but one of our peers in the group has more
        values) we want to render a blank, but we want that blank to *span*
        the full area defined by our uniquingKey (e.g. across the whole Region).
        To do this, we need to use our specified uniquingValue and to wantSpan.
        We inherit this from AWTColumn (yeay!) but we need to detect below when
        we've successfully proxied through to our real "renderingColumn" and when
        we're blank (i.e. prepareAndReplace() just returns ourself).
     */
    public Object uniquingValue (AWTDataTable sender, Object item)
    {
        // get the column for this item and ask it its uniquing value
        AWTDataTable.Column col = prepareAndReplace(sender);
        return (col == this) ? super.uniquingValue(sender, item) : col.uniquingValue(sender, item);
    }

    public boolean wantsSpan(AWTDataTable table)
    {
        return true;
    }

    public String label (AWTDataTable table)
    {
        // only show label on first row
        DetailIterator iter = table.pivotState()._rowDetailIterator;
        return (iter == null || iter._curIndex == 0) ? super.label(table) : "";
    }

    // Ensure that we render as empty
    public AWElement contentElement()
    {
        return _BlankElement;
    }

    public String pivotMoveType (AWTDataTable table)
    {
        return "DetailAttribute";
    }
    
    public static class DetailIterator
    {
        List[] _columnListsBySlot;
        int _curIndex;
        AWTPivotState.PivotGroup _group;

        public DetailIterator (List[] lists)
        {
            _curIndex = -1;
            _columnListsBySlot = lists;
        }

        public boolean next ()
        {
            if (_curIndex == -2) return false;
            _curIndex++;
            for (int i=0, count=_columnListsBySlot.length; i < count; i++) {
                if (columnStateForSlot(i) != null) return true;
            }
            _curIndex = -2;
            return false;
        }
        

        public Object columnStateForSlot (int slot)
        {
            if (slot >= _columnListsBySlot.length) return null;
            List list = _columnListsBySlot[slot];
            return (list != null && list.size() > _curIndex) ? list.get(_curIndex) : null;
        }
    }
}
