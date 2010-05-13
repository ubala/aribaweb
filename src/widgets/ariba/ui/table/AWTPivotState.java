package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWChecksum;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.ArrayUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.OrderedList;
import ariba.ui.table.AWTDataTable.Column;
import ariba.ui.table.AWTDataTable.ProxyColumn;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.IdentityHashMap;

/*
    Pivot Support:
    Pivot mode is used to render a multi-dimensional data set along
    two dimensions: rows and columns.
    This is ultimately a form of *nested grouping* with objects sorted
    in the order of their left-hand side levels (Row Fields) and then
    their top to bottom levels (Column Fields).  When in pivot mode we
    set the DisplayGroup's primarySortOrderings accordingly.

    We build on the DataTable's existing simple Grouping support, creating one
    PivotGroup for each row (i.e. each unique set of RowField values) and store
    within that group references to each object in that group (i.e. for each unique
    set of Column Field values).

    While grouping we compute a set of unique table columns for each unique combination
    of Column Field values (via the EdgeCell hierarchy) and incorporates these columns
    in the DataTable's displayedColumns set.  Then, when the DataTable iterates
    across these columns we override the DisplayGroup currentItem to reflect the member
    of the Group that should actually be rendered in that position.
*/
public class AWTPivotState implements AWTDisplayGroup.Grouper
{
    protected static Column PlaceholderColumn = AWTColumn.createPlaceholderColumn();
    protected static final String PivotLayoutConfigKey = "pivotLayout";
    private static final String ShowingRowAttributesConfigKey = "P_sRA";
    private static final String PivotColumnFields = "P_FCol";
    private static final String PivotRowFields = "P_FRow";
    private static final String PivotColumnAttributes = "P_AttrCol";

    EdgeCell _columnEdgeRoot = null;
    Column[] _columnFields;
    Column[] _rowFields;
    List _visibleColumnAttributeFields;
    List _columnAttributeFields;
    Column[] _rowAttributeFields;
    private AWBinding _overrideAttributeBinding;
    private AWBinding _filteredColumnAttributesBinding;
    private AWBinding _useRowDetailExpansionControlBinding;
    private AWBinding _showColumnAttributeLabelOnRowBinding;
    boolean _showColumnAttributeLabelOnRow ;
    boolean _useRowDetailExpansionControl;
    public int _firstAttributeColumnIndex = 0;
    public int _lastRowFieldColumnIndex = 0;
    public AWEncodedString pivotPanelId = null;
    Object[] _originalConfig;
    public boolean _showingRowAttributes;
    long _layoutChecksum;
    private AWTDataTable _dataTable;

    // called when initing columns
    protected static void checkPivotLayout (AWTDataTable dataTable)
    {
        if (dataTable._pivotLayoutBinding == null) return;
        if (dataTable.booleanValueForBinding(dataTable._pivotLayoutBinding)) {
            if (dataTable._pivotState == null || dataTable._pivotState.hasLayoutChanged()) {
                dataTable._pivotState = AWTPivotState.createPivotState(dataTable);
            }
        } else {
            dataTable._pivotState = null;
            dataTable._displayGroup.setGrouper(null);
        }
    }

    protected static AWTPivotState createPivotState (AWTDataTable dataTable)
    {
        List columnFieldNames = (List)dataTable.valueForBinding(BindingNames.columnFields);
        List rowFieldNames = (List)dataTable.valueForBinding(BindingNames.rowFields);
        List columnAttributes = (List)dataTable.valueForBinding(BindingNames.columnAttributes);

        // Our stack of column fields is all the specified ones, plus one for the "column attributes field"
         return new AWTPivotState(dataTable,
                   dataTable.columnsMatchingKeys(columnFieldNames),
                   dataTable.columnsMatchingKeys(rowFieldNames),
                   dataTable.columnsMatchingKeys(columnAttributes));
    }

    protected static boolean processPivotTableConfig (AWTDataTable table, Map config)
    {
        String s = (String)config.get(PivotLayoutConfigKey);
        if (!StringUtil.nullOrEmptyOrBlankString(s) && Boolean.valueOf(s).booleanValue()) {
            checkPivotLayout(table);
            Assert.that(table._pivotState != null, "Table config says pivot, but table not configured in pivot layout");
            table._pivotState.processTableConfig(config);
            return true;
        }
        return false;
    }
    
    public AWTPivotState(AWTDataTable dataTable, List columnFields, List rowFields, List columnAttributeFields)
    {
        // Icky...
        _dataTable = dataTable;
        _dataTable._pivotState = this;

        boolean noColumnsFieldsOrAttrs = false;
        _originalConfig = ArrayUtil.array(ListUtil.copyList(columnFields),
                ListUtil.copyList(rowFields), ListUtil.copyList(columnAttributeFields));

        _stripNonVisibleColumns(columnAttributeFields);

        if (columnFields.size() == 0 && columnAttributeFields.size() == 0) noColumnsFieldsOrAttrs = true;

        // Keep around the set that hasn't been post-processed for callbacks to columneAttributeFields()
        _visibleColumnAttributeFields = columnAttributeFields;
        _columnAttributeFields = postProcessAttributeColumns(columnAttributeFields);

        // If we have no columnFields, then we don't need most of this...
        // just put the columnAttributes on the edge...
        // Todo: What if selection is enabled but we have only one real columnAttributeField?
        Column rootColumn = null;
        if (!noColumnsFieldsOrAttrs && _columnAttributeFields.size() == 1
                    && shouldCollapseColumn(null, 1) && columnFields.size() > 0) {
            // no level for attributes column -- just assign to the root so it will be the attributeColumn()
            rootColumn = (Column)_columnAttributeFields.get(0);
        } else {
            // join them together in a meta-column
            Column attributesColumn = new PivotAttributesColumn(this);
            columnFields.add(attributesColumn);
        }
        _columnFields = createColumnArray(columnFields);
        _rowFields = createColumnArray(rowFields);

        _overrideAttributeBinding = _dataTable.bindingForName(BindingNames.overrideAttribute);
        _filteredColumnAttributesBinding = _dataTable.bindingForName(BindingNames.filteredColumnAttributes);
        _useRowDetailExpansionControlBinding = _dataTable.bindingForName(BindingNames.useRowDetailExpansionControl);
        _showColumnAttributeLabelOnRowBinding = _dataTable.bindingForName(BindingNames.showColumnAttributeLabelOnRow);

        _showingRowAttributes = _dataTable.booleanValueForBinding(BindingNames.showRowAttributes);
        _columnEdgeRoot = new EdgeCell(null, null, rootColumn, null, null);

        // Set for sort order / grouping
        _dataTable._displayGroup.setGrouper(this);

        // force computation of groups now (so our columns are updated before displayedColumns is called)
        _dataTable._displayGroup.updateDisplayedObjects();
        _dataTable._displayGroup.filteredObjects();

        _layoutChecksum = layoutChecksum();
        this._dataTable = dataTable;
    }

    // Called by table displayedColumns() to get effective set of columns
    public List computeDisplayedColumns(List displayedColumns)
    {
        // The effective column set is:
        // rowFields + leaves of the column EdgeCell tree
        // Any other visible column is a rowAttribute

        List result = ListUtil.list();

        ListUtil.addToCollection(result, _rowFields);
        _lastRowFieldColumnIndex = result.size();

        // Check for magic column to introduce between the row fields and
        // column fields (used by S4 for RowDetailAttribute expansion toggle)
        boolean isExportMode = _dataTable.isExportMode();
        if (!isExportMode) {
            if (_showColumnAttributeLabelOnRowBinding != null || _useRowDetailExpansionControlBinding != null) {
                result.add(_RowAttributeExpandoColumn);
            }
            else {
                Column extra = _dataTable.findColumnForKey("PostRowFieldColumn");
                if (extra != null) result.add(extra);
            }
        }

        _firstAttributeColumnIndex = result.size();

        Set visibleColumns = _dataTable.visibleColumnSet();
        // TODO: add column if displaying _rowAttributeFields
        // TODO: mask for visibility!!!
        _columnEdgeRoot.createColumnsForLeafLevel(result);

        Set used = new HashSet();
        ListUtil.addToCollection(used, _rowFields);
        ListUtil.addToCollection(used, _columnFields);
        ListUtil.addToCollection(used, _allColumnAttributes().toArray());

        // All other visible fields are rowAttributeFields
        _optionalRowAttributes = ListUtil.list();
        List rowAttributes = ListUtil.list();
        List allColumns = _dataTable.columns();
        int count = allColumns.size();
        for (int i=0; i < count; i++) {
            Column col = (Column)allColumns.get(i);
            if (!used.contains(col)) {
                col.prepare(_dataTable);
                if (col.isOptional(_dataTable)) _optionalRowAttributes.add(col);
                if (visibleColumns.contains(col)) rowAttributes.add(col);
            }
        }
        _rowAttributeFields = createColumnArray(rowAttributes);

        _optionalAttributeColumns = null;

        if (isExportMode) {
            result = hideColumnsForExport(result);
        }
        else {
            result = restoreColumnsAfterExport(result);
        }

        return result;
    }

    Object _prepareColumnsForExport ()
    {
        _dataTable.invalidateColumnData();
        return null;
    }

    List hideColumnsForExport (List displayColumns)
    {
        // Build list of data columns, and hide non-visible edge columns in the EdgeCell set
        // to ensure that colspans are appropriately recomputed
        List dataColumns = ListUtil.list();
        for (int i=0, count=displayColumns.size(); i < count; i++) {
            Column c = (Column)displayColumns.get(i);
            c.prepare(_dataTable);
            if (c.isValueColumn(_dataTable)) {
                dataColumns.add(c);
            }
            else if (c instanceof PivotEdgeColumn) {
                ((PivotEdgeColumn)c).edgeCell().setHidden(true);
            }
        }
        return dataColumns;
    }

    void _restoreColumnsAfterExport (Object state)
    {
        _dataTable.invalidateColumnData();
    }

    List restoreColumnsAfterExport (List displayColumns)
    {
        // restore visibility of hidden columns
        for (int i=0, count=displayColumns.size(); i < count; i++) {
            Column c = (Column)displayColumns.get(i);
            if (c instanceof PivotEdgeColumn) {
                ((PivotEdgeColumn)c).edgeCell().setHidden(false);
            }
        }
        return displayColumns;
    }

    boolean hasLayoutChanged ()
    {
        return (_layoutChecksum != layoutChecksum());
    }

    long layoutChecksum ()
    {
        return layoutChecksumForColumns((List) _dataTable.valueForBinding(BindingNames.columnFields),
                                        (List) _dataTable.valueForBinding(BindingNames.rowFields),
                                        (List) _dataTable.valueForBinding(BindingNames.columnAttributes));
    }

    long layoutChecksumForColumns (List columnFields, List rowFields, List columnAttributes)
    {
        // Compute a CRC for the details of the pivot config.  This way we avoid an expensive reset
        // if nothing actually changed
        long crc = 1;
        crc = AWChecksum.crc32StringList(crc, columnFields);
        crc = AWChecksum.crc32StringList(crc, rowFields);
        crc = AWChecksum.crc32StringList(crc, columnAttributes);
        crc = AWChecksum.crc32HashList(crc, _dataTable.computeVisibleColumns());
        crc = AWChecksum.crc32(crc, (long) _dataTable.selectionMode());
        return crc;
    }

    // Disableable Column and row attributes
    List _optionalAttributeColumns;
    List _optionalRowAttributes;

    /*  For proper Configure Layout support...
    List _visibleOptionalColumnAttributes;
    List _visibleOptionalRowAttributes;
    */

    List postProcessAttributeColumns (List columnAttributeFields)
    {
        List result = ListUtil.list();
        result.addAll(columnAttributeFields);

        if (columnAttributeFields.size() == 0) {
            // add placeholder for row attributes
            result.add(PlaceholderColumn);
        }

        // Column attribute fields -- add one for selection control if necessary
        int selectMode = _dataTable.selectionMode();
        if (selectMode != AWTDataTable.SELECT_NONE) {
            Column newColumn = (selectMode == AWTDataTable.SELECT_MULTI)
                        ? AWTDataTable.MultiSelectColumn
                        : AWTDataTable.SingleSelectColumn;
            result.add(0, newColumn);
        }

        return result;
    }

    // Called during EdgeCell tree construction to get the list of attribute columns for current cell
    // Client can implement filteredColumnAttributes binding to get
    // AWTDataTable.currentInstance().pivotState().columnAttributes() and return
    // a filtered list based on displayGroup.currentItem()
    List computeAttributeColumns (Object obj)
    {
        List columns = null;
        if (_filteredColumnAttributesBinding != null) {
            _dataTable.setCurrentItem(obj);
            columns = (List) _dataTable.valueForBinding(_filteredColumnAttributesBinding);
            if (columns != null) {
                columns = postProcessAttributeColumns(columns);
            }
        }
        return (columns != null) ? columns : _columnAttributeFields;
    }

    public List columnAttributes()
    {
        return _visibleColumnAttributeFields;
    }



    public List _allColumnAttributes()
    {
        return (List)_originalConfig[2];
    }

    public List columnFields ()
    {
        return (List)_originalConfig[0];
    }

    public List rowFields()
    {
        return (List)_originalConfig[1];
    }

    public List<PivotEdgeColumn> leafLevelColumnsFor (Column columnFieldsColumn)
    {
        List<PivotEdgeColumn> all = ListUtil.list();
        List result = ListUtil.list();
        _columnEdgeRoot.createColumnsForLeafLevel(all);
        for (PivotEdgeColumn col : all) {
            if (col.edgeCell().attributeColumn() == columnFieldsColumn) result.add(col);
        }
        return result;
    }
    
    public List optionalAttributeColumns ()
    {
        // force computation
        _dataTable.displayedColumns();

        if (_optionalAttributeColumns == null) {
            _optionalAttributeColumns = ListUtil.list();
            _addOptionalColumns(_allColumnAttributes(), _optionalAttributeColumns);
            if (_optionalRowAttributes != null) _optionalAttributeColumns.addAll(_optionalRowAttributes);
        }
        return _optionalAttributeColumns;
    }

    void _addOptionalColumns (List columns, List collection)
    {
        if (columns == null) return;
        for (int i=0, c=columns.size(); i<c; i++) {
            Column col = (Column)columns.get(i);
            col.prepare(_dataTable);
            if (col.isOptional(_dataTable)) {
                collection.add(col);
            }
        }
    }

    void _stripNonVisibleColumns (List columns)
    {
        Set visibilitySet = _dataTable.visibleColumnSet();
        int i = columns.size();
        while (i-- > 0) {
            if (!visibilitySet.contains(columns.get(i))) columns.remove(i);
        }
    }

    Column _collapseCheckColumn;
    int _collapseCheckMemberCount;

    protected boolean shouldCollapseColumn (Column col, int memberCount)
    {
        AWBinding binding = table().bindingForName("shouldCollapseColumnLevel");
        if (binding != null) {
            _collapseCheckColumn = col;
            _collapseCheckMemberCount = memberCount;
            return table().booleanValueForBinding(binding);
        }
        // defaults: ColumnAttribute do collapse single
        //           ColumnField: collapse if 0 but not one
        return (col == null) ? true : (memberCount == 0);
    }

    // callbacks used by implementors of shouldCollapseColumn binding
    // Note: returns *null* for columnAttributes level
    public String collapseCheckColumnKey ()
    {
            return (_collapseCheckColumn == null) ? null
                    : _collapseCheckColumn.keyPathString();
    }

    public int collapseCheckMemberCount () { return _collapseCheckMemberCount; }

    public boolean isCurrentColumnDisplayed ()
    {
        return _dataTable.visibleColumnSet().contains(_dataTable._originalCurrentColumn);
    }

    public void pivotToggleCurrentColumnVisibility ()
    {
        // Need to get at the underlying column, not the proxy
        // setCurrentColumn(_currentColumn.prepareAndReplace(AWTDataTable.this));
        _dataTable.toggleCurrentColumnVisibility();

        // need to force a recomputation of the pivot layout
        updateWithConfig(originalConfig());
    }

    public AWTDataTable table()
    {
        return _dataTable;
    }

    protected Object[] originalConfig()
    {
        return _originalConfig;
    }

    public Object[] editableConfig()
    {
        List optionalAttributeColumns = optionalAttributeColumns();

        Object[] layout = new Object[5];
        layout[0] = ListUtil.copyList((List)_originalConfig[0]);
        layout[1] = ListUtil.copyList((List)_originalConfig[1]);
        layout[2] = ListUtil.collectionToList(ListUtil.equalElements((List)_originalConfig[2], optionalAttributeColumns));

        List list = ListUtil.arrayToList(_rowAttributeFields);
        layout[3] = ListUtil.collectionToList(ListUtil.equalElements(list, optionalAttributeColumns));
        list.addAll((List)layout[2]);
        layout[4] = ListUtil.minus(optionalAttributeColumns, list);
        return layout;
    }

    protected AWTPivotState updateWithEditableConfig (Object[] lists)
    {
        try {
            _dataTable._phasePrepare();
            _dataTable.invalidateColumns();
            List optionalAttributeColumns = optionalAttributeColumns();
            List cols = _dataTable.columns();
            List hiddenCols = (List)lists[4];
            for (int i=0,c=cols.size(); i<c; i++) {
                Column col = (Column)cols.get(i);
                if (optionalAttributeColumns.contains(col)) {
                    _dataTable.setColumnVisibility(col, !hiddenCols.contains(col));
                }
            }

            _dataTable._pivotState = new AWTPivotState(_dataTable, (List) lists[0], (List) lists[1], (List) lists[2]);
            _dataTable._pivotState._showingRowAttributes = _showingRowAttributes;
            _dataTable.pushTableConfig();
            return _dataTable._pivotState;
        }
        finally {
            _dataTable._phaseComplete();
        }
    }

    protected AWTPivotState updateWithConfig (Object[] lists)
    {
        _dataTable._pivotState = new AWTPivotState(_dataTable, (List) lists[0], (List) lists[1], (List) lists[2]);
        _dataTable._pivotState._showingRowAttributes = _showingRowAttributes;
        _dataTable.pushTableConfig();
        return _dataTable._pivotState;
    }

        // archive / restore config
    public void writeToTableConfig(Map config)
    {
        // Note that there's a pivot table...
        config.put(PivotLayoutConfigKey, Boolean.toString(true));
        config.put(ShowingRowAttributesConfigKey, Boolean.toString(_showingRowAttributes));
        config.put(PivotColumnFields, _dataTable.keysForColumns((List)_originalConfig[0]));
        config.put(PivotRowFields, _dataTable.keysForColumns((List)_originalConfig[1]));
        config.put(PivotColumnAttributes, _dataTable.keysForColumns((List)_originalConfig[2]));
    }

    public void processTableConfig (Map config)
    {
        String s = (String)config.get(ShowingRowAttributesConfigKey);
        _showingRowAttributes = !StringUtil.nullOrEmptyOrBlankString(s) && Boolean.valueOf(s).booleanValue();
        // Copy it?
        Object[] layout = originalConfig();
        long origLayoutChecksum = layoutChecksumForColumns(_dataTable.keysForColumns((List)layout[0]),
                _dataTable.keysForColumns((List)layout[1]), _dataTable.keysForColumns((List)layout[2]));
        _updateLayout(layout, (List)config.get(PivotColumnFields), 0);
        _updateLayout(layout, (List)config.get(PivotRowFields), 1);

        // Note:  not clear that this is robust when app adds new non-optional columns...
        layout[2] = _dataTable.columnsMatchingKeys((List)config.get(PivotColumnAttributes));
        long newLayoutChecksum = layoutChecksumForColumns(_dataTable.keysForColumns((List)layout[0]),
                _dataTable.keysForColumns((List)layout[1]), _dataTable.keysForColumns((List)layout[2]));
        if (origLayoutChecksum != newLayoutChecksum) {
            updateWithConfig(layout);
        }
    }

    // Idea here is to take saved config into account while preserving any new columns
    // added by add, and not erroring out on any columns that are not part of the app-provided layout
    void _updateLayout (Object[] layout, List keys, int destinationListId)
    {
        if (keys == null) return;
        int i = keys.size();
        while (i-- > 0) {
            _layoutMove(layout, (String)keys.get(i), destinationListId);
        }
    }

    void _layoutMove (Object[] layout, String key, int destinationListId)
    {
        // try to remove from each list
        Column col = _dataTable.findAndRemoveColumnWithKey((List)layout[0], key);
        if (col == null) col = _dataTable.findAndRemoveColumnWithKey((List)layout[1], key);

        // if we found it, insert in the front of the destination
        if (col != null) {
            ((List)layout[destinationListId]).add(0, col);
        }
    }

    protected void logSortedObjects (List list, List sortOrderings)
    {
        System.out.println("Sort orderings: " + sortOrderings);
        for (int j=0; j < sortOrderings.size(); j++) {
            AWTSortOrdering ordering = (AWTSortOrdering)sortOrderings.get(j);
            System.out.print(ordering.key());
            System.out.print("," );
        }
        System.out.println("");
        for (int i=0; i < list.size(); i++) {
            Object o = list.get(i);
            for (int j=0; j < sortOrderings.size(); j++) {
                AWTSortOrdering ordering = (AWTSortOrdering)sortOrderings.get(j);
                Object value = FieldValue.getFieldValue(o, ordering.key());
                System.out.print(value);
                System.out.print("," );
            }
            System.out.println("");
        }
    }

    /*
        Called by DataTable when computing FilteredObjects and by OutlineState when
        computing children.

        We create one group for each unique combination of RowField values.
        All objects within that group (which will occur successively in the sorted
        input) should have distinct ColumnEdge values.  We store each of these
        objects in the group, and put only the lead object in the returned List.

        For each object we look up / lazily create Column EdgeCells.  If we end up
        adding new columns in this process, we invalidate the DataTable's displayed
        columns so our revised set can be used in rendering.
     */
    public List groupObjects (List sortedObjects)
    {
        // logSortedObjects (sortedObjects, _displayGroup.effectiveSortOrderings());
        List groupedObjects = ListUtil.list();
        Iterator e = sortedObjects.iterator();
        Object lastGroupObject = null;

        PivotGroup groupingState = null;
        Map groupingStates = _dataTable._displayGroup.groupingValueState();
        List rowSorts = _sortOrderingArray(_rowFields);
        int rowFieldCount = rowSorts.size();

        // TODO: need to use sort orderings from DisplayGroup to get right sort selector...
        CellVisitor vistorRoot = createVistors(_columnFields, _dataTable);
        int beforeGroupSize = groupSize();

        int lastSlot = -1, rowBasePos = 0, rowCurPos = 0;

        while (e.hasNext()) {
            Object object = e.next();
            int arrayDiffIndex = 0;

            if (lastGroupObject == null || (arrayDiffIndex = _sortDiffIndex(rowSorts, object, lastGroupObject)) != -1) {
                // new group row keyed by current object
                groupingState = new PivotGroup(object, groupSize(), arrayDiffIndex);

                // Add this to the list of rows and reset base
                groupedObjects.add(object);
                rowBasePos = rowCurPos = groupedObjects.size() - 1;
                lastSlot = -1;

                // reset our column edge search position
                vistorRoot.resetSearchPosition();

                // remember base object
                lastGroupObject = object;
            }
            // Find the object slot for this object and store in in groupingState
            EdgeCell leafCell = _columnEdgeRoot.addOrFind(object, vistorRoot);
            int slot = leafCell.groupSlot();
            if (slot == lastSlot) {
                // slot collision -- two objects were not unique by their pivot field keys
                // We need to add another group/row (or use another if one was already pushed for a previous slot)
                if (rowCurPos == groupedObjects.size() - 1) {
                    groupingState = new PivotGroup(object, groupSize(), rowFieldCount - 1);
                    groupedObjects.add(object);
                } else {
                    groupingState = (PivotGroup)groupingStates.get(groupedObjects.get(rowCurPos + 1));
                }
                rowCurPos++;
            } else {
                // different slot, and we had created extra groups, so re-base our position
                if (rowCurPos != rowBasePos) {
                    rowCurPos = rowBasePos;
                    groupingState = (PivotGroup)groupingStates.get(groupedObjects.get(rowCurPos));
                }
                lastSlot = slot;
            }
            groupingState.put(object, slot);

            // register this object with the grouping state for the group
            groupingStates.put(object, groupingState);
        }

        // if we added columns, or a column went unused, force a recompute
        if (beforeGroupSize < groupSize()) _dataTable.invalidateColumnData();
        _detailAttributeExpansionMapInvalidated = true;

        return groupedObjects;
    }

    public void validateFilteredList (List filteredList)
    {
/* Debug -- display ids of items in groups
            Map groupingStates = _displayGroup.groupingValueState();
            for (int i=0, count=filteredList.size(); i < count; i++) {
                PivotGroup groupingState = (PivotGroup)groupingStates.get(filteredList.get(i));
                System.out.println("Row " + i);
                Iterator iter = groupingState.iterator();
                while (iter.hasNext()) {
                    System.out.println("  " + System.identityHashCode(iter.next()));
                }
            }
*/

        int origSpan = _columnEdgeRoot.span();
        // Invalidate current layout
        _columnEdgeRoot.updateColumnVisibility(filteredList, this);

        if (_columnEdgeRoot.span() != origSpan) _dataTable.invalidateColumnData();
    }

    public void invalidate ()
    {
        _dataTable.invalidateColumnData();

        // Even if the core data changes, don't forceably throw away our pivot config -- we want to
        // preserve / re-use the existing edgecell structure if possible so that we rendezvous with
        // the same PivotColumn instances (and therefore the same component state in the case of
        // scopeSubcomponentsByItem="$true" mode in the table
        //
        // updateWithConfig(originalConfig());

        // a softer rest...
        _dataTable._displayGroup.setGrouper(this);
        _dataTable._displayGroup.updateDisplayedObjects();
        _dataTable._displayGroup.filteredObjects();
    }

    public List objectsInGroups (List groupLeadItems)
    {
        List result = ListUtil.list();
        for (int i=0, count=groupLeadItems.size(); i < count; i++) {
            PivotGroup group = (PivotGroup) _dataTable._displayGroup.groupingState(groupLeadItems.get(i));
            Iterator iter = group.iterator();
            while (iter.hasNext()) {
                result.add(iter.next());
            }
        }
        return result;
    }

    int groupSize ()
    {
        int size = _columnEdgeRoot.groupSlot() + 1;
        return (size > 0) ? size : 1;
    }

    void _addSortOrderings (List orderings, Column[] columns)
    {
        // When adding we check if the user has any sort orderings specified and if so we use
        // their sort direction -- this way they can click on columns to set the sort, even for
        // grouping columns
        List userOrderings = _dataTable._displayGroup.sortOrderings();
        // N^2 -- but N is small...
        for (int i=0, c=columns.length; i < c; i++) {
            columns[i].prepare(_dataTable);
            AWTSortOrdering sortOrdering = columns[i].createSortOrdering(_dataTable);
            if (sortOrdering == null) continue;
            if (userOrderings != null) {
                int index = userOrderings.indexOf(sortOrdering);
                if (index != -1) {
                    sortOrdering.setSelector(((AWTSortOrdering)userOrderings.get(index)).selector());
                }
            }
            orderings.add(sortOrdering);
        }
    }

    public List computeSortOrderings()
    {
        List orderings = ListUtil.list();
        _addSortOrderings(orderings, _rowFields);
        _addSortOrderings(orderings, _columnFields);
        return orderings;
    }

    // sort handling -- we need to aggregate answer for all items in current group
    public List currentItemChildren ()
    {
        List result = null;
        PivotGroup group = (PivotGroup) _dataTable._displayGroup.currentGroupingState();
        Iterator iter = group.iterator();
        Object origItem = _dataTable._displayGroup.currentItem();
        while (iter.hasNext()) {
            Object item = iter.next();
            _dataTable.setCurrentItem(item);
            Object children = _dataTable.valueForBinding(ariba.ui.outline.BindingNames.children);
            if (children != null) {
                if (result == null) result = ListUtil.list();
                Iterator ci = OrderedList.get(children).elements(children);
                while (ci.hasNext()) result.add(ci.next());
            }
        }

        // restore orig item
        _dataTable.setCurrentItem(origItem);

        return result;
    }

    public boolean currentItemHasChildren ()
    {
        boolean result = false;
        PivotGroup group = (PivotGroup) _dataTable._displayGroup.currentGroupingState();
        Iterator iter = group.iterator();
        Object origItem = _dataTable._displayGroup.currentItem();
        while (iter.hasNext()) {
            Object item = iter.next();
            _dataTable.setCurrentItem(item);
            if (_dataTable.booleanValueForBinding(ariba.ui.outline.BindingNames.hasChildren)) {
                result = true;
                break;
            }
        }

        // restore orig item
        _dataTable.setCurrentItem(origItem);

        return result;
    }


    public int preEdgeColSpan ()
    {
        return _firstAttributeColumnIndex;
    }

    public int columnEdgeLevels ()
    {
        return _columnFields.length;
    }

    // Iteration called from AWWhile in the .awl to render the header rows...
    int _currentHeaderDepth = 0;
    public EdgeCell _currentEdgeCell;

    public boolean renderAsHeader ()
    {
        return !(_dataTable._originalCurrentColumn instanceof PivotEdgeColumn)
                ||  !((PivotEdgeColumn)_dataTable._originalCurrentColumn).shouldRenderDataColumn(_dataTable);
    }

    public Object nextLevel ()
    {
        do {
            _currentHeaderDepth++;
            if (_currentHeaderDepth >= columnEdgeLevels()) {
                // stop iteration, and reset
                _currentHeaderDepth = 0;
                return null;
            }

        } while (_columnEdgeRoot.shouldCollapseCurrentLevel(_currentHeaderDepth, this));
        // override td style so that when the Columns render their dimension *values* they are styled
        // as header cells
        return Boolean.TRUE;
    }

    public List currentLevelEdgeCells ()
    {
        List result = ListUtil.list();
        _columnEdgeRoot.collectCellsAtLevel(result, _currentHeaderDepth);
        return result;
    }

    public List topLevelEdgeCells ()
    {
        int depth = 1;
        while (depth < columnEdgeLevels() && _columnEdgeRoot.shouldCollapseCurrentLevel(depth, this)) depth++;
        if (depth >= columnEdgeLevels()) return null;
        List result = ListUtil.list();
        _columnEdgeRoot.collectCellsAtLevel(result, depth);
        return result;
    }

    public void setCurrentEdgeCell (EdgeCell cell)
    {
        _currentEdgeCell = cell;
        _dataTable.setCurrentColumn(cell._column);
        _dataTable.setCurrentItem(cell._object);
    }

    public String leafCellHeaderClass ()
    {
        // if we're the start of a new object then return style with left border
        if (_dataTable._originalCurrentColumn instanceof PivotEdgeColumn) {
            EdgeCell cell = ((PivotEdgeColumn) _dataTable._originalCurrentColumn)._edgeCell;
            if (!cell.isAttributeCell() || cell.isFirstChild()) return "tableHead pivotCell";
        }
        return "tableHead";
    }

    // Called from While around data row to potentially render detail rows for "RowAttributes"
    protected List _rowAttributeList;
    protected int _rowAttrPos = -1;
    protected int _currentVisibleRowAttrPos = 0;
    protected Column _rowOverrideColumn = null;
    protected LVal _primaryColAttributeLVal = new LVal();
    protected AWTDynamicDetailAttributes.DetailIterator _rowDetailIterator;
    boolean _currentRowHasDetailAttributes;
    Map <PivotGroup, Boolean> _detailAttributesExpanded = new IdentityHashMap();
    boolean _detailAttributeExpansionMapInvalidated;

    public void prepareForIteration () {
        _dataTable._renderingPrimaryRow = true;
        _currentGroupObject = null;
        _rowOverrideColumn = null;
        _skippingSlot = -1;
        _rowAttrPos = -1;
        _currentVisibleRowAttrPos = 0;        
        _skippingProperty = null;
        _rowDetailIterator = null;
    }

    public boolean showDetailAttributesExpando ()
    {
        return _showingRowAttributes && _useRowDetailExpansionControl && _currentRowHasDetailAttributes;
    }

    public boolean detailAttributesExpanded ()
    {
        PivotGroup group = (PivotGroup)_dataTable._displayGroup.currentGroupingState();
        Boolean expanded = detailAttributeExpansionMap().get(group);
        return expanded != null;
    }

    public void setDetailAttributesExpanded (Object item, boolean expanded)
    {
        PivotGroup group = (PivotGroup)_dataTable._displayGroup.groupingState(item);
        if (group != null) {
            if (expanded) {
                detailAttributeExpansionMap().put(group, true);
            } else {
                detailAttributeExpansionMap().remove(group);
            }
        }
    }

    public boolean getDetailAttributesExpanded (Object item)
    {
        PivotGroup group = (PivotGroup)_dataTable._displayGroup.groupingState(item);
        return (group != null) && detailAttributeExpansionMap().get(group) != null;
    }

    Map<PivotGroup, Boolean>detailAttributeExpansionMap ()
    {
        if (_detailAttributeExpansionMapInvalidated) {
            _detailAttributeExpansionMapInvalidated = false;
            // Since _detailAttributesExpanded is an IdentityHashMap, on regroup,
            // we need to remap to the new group instances
            if (_detailAttributesExpanded.size() > 0) {
                Collection<PivotGroup> newGroups = _dataTable._displayGroup.groupingValueState().values();
                Set<PivotGroup> expanded = new HashSet(_detailAttributesExpanded.keySet());
                _detailAttributesExpanded = new IdentityHashMap();
                for (PivotGroup g : newGroups) {
                    if (expanded.contains(g)) _detailAttributesExpanded.put(g, true);
                }
            }
        }
        return _detailAttributesExpanded;
    }

    public void toggleDetailAttributesExpanded ()
    {
        PivotGroup group = (PivotGroup)_dataTable._displayGroup.currentGroupingState();
        boolean expanded = !detailAttributesExpanded();
        if (expanded) {
            _detailAttributesExpanded.put(group, true);
        } else {
            _detailAttributesExpanded.remove(group);
        }
    }

	public void expandAllDetailAttributes ()
	{
		Collection<PivotGroup> groups = _dataTable._displayGroup.groupingValueState().values();
		for (PivotGroup g: groups) {
			_detailAttributesExpanded.put(g, true);
		}
	}

	public void collapseAllDetailAttributes ()
	{
		_detailAttributesExpanded.clear();   
	}


    class LVal { public Object val; }

    String primaryAttributeColumnLabel (LVal colLVal)
    {
        String label = null;
        if (colLVal != null) colLVal.val = null;
        if (_dataTable._renderingPrimaryRow && _showColumnAttributeLabelOnRow) {
            // calculate the actual column used to render our column attribute
            List columns = _dataTable.displayedColumns();
            for (int i = _firstAttributeColumnIndex, count = columns.size(); i < count; i++) {
                Column column = (Column)columns.get(i);
                if (column instanceof PivotEdgeColumn) {
                    column = column.prepareAndReplace(_dataTable);
                    label = column.label(_dataTable);
                    _resetDetailRowIteration();
                    if (!StringUtil.nullOrEmptyOrBlankString(label)) {
                        if (colLVal != null) colLVal.val = column;
                        break;
                    }
                }
            }
        }

        return label;
    }

    public boolean preparePrimaryRow (AWTDataTable table)
    {
        _showColumnAttributeLabelOnRow = _dataTable.booleanValueForBinding(_showColumnAttributeLabelOnRowBinding);
        _useRowDetailExpansionControl = _dataTable.booleanValueForBinding(_useRowDetailExpansionControlBinding);
        primaryAttributeColumnLabel(_primaryColAttributeLVal);
        // maybe need to clear more stale states here
        _rowOverrideColumn = null;
        if (_useRowDetailExpansionControl) {
            _currentRowHasDetailAttributes = _prepareDetailRow(table);
            if (_currentRowHasDetailAttributes) {
                _resetDetailRowIteration();
            }
        }
        return true;
    }

    public boolean prepareDetailRow (AWTDataTable table)
    {
        if (_useRowDetailExpansionControl && !detailAttributesExpanded()) return false;
        return _prepareDetailRow(table);
    }

    public boolean _prepareDetailRow (AWTDataTable table)
    {
        _currentGroupObject = null;
        _rowOverrideColumn = null;
        _skippingSlot = -1;
        _skippingProperty = AWTDataTable.NeverEqualValue;
        if (!_showingRowAttributes || _rowAttributeFields == null || _rowAttributeFields.length == 0) return false;
        if (_rowAttrPos == -1) {
            _rowAttrPos = 0;
        }

        while (_rowAttrPos < _rowAttributeFields.length) {
            _rowOverrideColumn =_rowAttributeFields[_rowAttrPos];

           /*
                Special handing for dynamic lists of detail attributes:
                If this is a AWTDynamicDetailAttributes column than we have
                it build us an iterator to render the necessary sub-rows
            */
            if (_rowOverrideColumn instanceof AWTDynamicDetailAttributes) {
                // if we're already iterating sub-rows, finish that...
                if (_rowDetailIterator != null) {
                    // if we have another row, render it
                    if (_rowDetailIterator.next()) return true;
                    // no more rows -- clear out and move to the next attribute
                    _rowDetailIterator = null;
                    _rowAttrPos++;
                    continue;
                }
                // Start of a new AWTDynamicDetailAttributes set -- create an iterator and
                // then cycle around to render the first row
                _rowDetailIterator = ((AWTDynamicDetailAttributes)_rowOverrideColumn).prepare(table(),
                        (PivotGroup) _dataTable._displayGroup.currentGroupingState());
                if (_rowDetailIterator == null) _rowAttrPos++;
                continue;
            }
            _rowAttrPos++;
            if (_rowOverrideColumn == _primaryColAttributeLVal.val) continue;

            _currentVisibleRowAttrPos++;
            if (!_allInGroupBlank(table(), (PivotGroup) _dataTable._displayGroup.currentGroupingState(), _rowOverrideColumn)) return true;
        }
        _resetDetailRowIteration();
        return false;
    }

    protected void _resetDetailRowIteration ()
    {
        _rowOverrideColumn = null;
        _rowAttrPos = -1;
        _currentVisibleRowAttrPos = 0;
        _rowDetailIterator = null;
        _currentGroupObject = null;
        _rowOverrideColumn = null;
        _skippingSlot = -1;        
        _skippingProperty = null;        
    }

    protected boolean _allInGroupBlank (AWTDataTable table, PivotGroup group, Column column)
    {
        boolean allBlank = true;
        Object origItem = table.currentItem();
        int count = group.slotCount();
        for (int i=0; i<count; i++) {
            Object o = group.get(i);
            if (o != null) {
                table.setCurrentItem(o);
                column.prepare(table);
                if (!column.isBlank(table)) {
                    allBlank = false;
                    break;
                }
            }
        }
        table.setCurrentItem(origItem);
        return allBlank;
    }

    public int currentVisibleRowAttrPos ()
    {
        return _currentVisibleRowAttrPos;
    }

    // Called when determining what to render for next data column
    // We blank rendering of leading columns to reflect row edge nesting, and leading cols
    // when rendering detail attributes.
    Column substituteForCurrentColumn (Column column)
    {
        PivotGroup group = (PivotGroup) _dataTable._displayGroup.currentGroupingState();
        if (group == null) {
            Assert.that(false, "Pivot called with no grouping for currentObject.  currentObject=%s, groupingState.size=%s",
                    _dataTable._displayGroup.currentItem(), Integer.toString(_dataTable._displayGroup.groupingValueState().size()));
        }
        if (_rowOverrideColumn != null) {
            if (_dataTable._currentColumnIndex == _lastRowFieldColumnIndex - 1) {
                // Make sure that we indent the label (or value)
                _dataTable.indentNextColumn();
                if (_rowOverrideColumn.renderValueInLabelColumn(_dataTable)) {
                    // display the value here in the attribute area
                    Column col = firstNonEmptyAttributeColumn(group);
                    if (col != null) return col;
                }
                // Swap in RowAttribute -- it will render the *label* of the current row column
                return _RowAttributeLabelColumn;
            }
            return (_dataTable._currentColumnIndex >= _firstAttributeColumnIndex) ? column : AWTDataTable.BlankColumn;
        }
        return (_dataTable._currentColumnIndex >= group._rowNestingLevel) ? column : AWTDataTable.BlankColumn;
    }

    Column firstNonEmptyAttributeColumn (PivotGroup group)
    {
        List columns = _dataTable.displayedColumns();
        for (int i = _firstAttributeColumnIndex, count = columns.size(); i < count; i++) {
            Column column = (Column)columns.get(i);
            if (column instanceof PivotEdgeColumn
                    && ((PivotEdgeColumn)column).edgeCell().itemInGroup(group) != null) {
                return column;
            }
        }
        return null;
    }

    // State when rendering body rows
    Object _currentGroupObject;
    int _skippingSlot = -1;
    Object _skippingProperty;

    /*
        PivotTable -- support for dimension-value-spanning Fields
        Sourcing has certain fields (e.g. Description, Requirements, Attributes) that are invariant across
        any dimension except item and therefore should not be rendered over and over across the row for each
        successive itemValue (i.e. for each supplier or region) -- instead these values should be rendered once
        and span across item-values for the same item.

        This provides support for this case via a new AWTColumn binding, "uniquingKeyPath".  E.g.:
            <AWTColumn key="Description" wantsSpan="$true" uniquingKeyPath="Region"/>

        Note that "Region" in this case is a *field path string* that must be rooted in the item
    */
    boolean skippingItem (Object item)
    {
        if (_skippingProperty != AWTDataTable.NeverEqualValue && _rowOverrideColumn != null) {
            // if we're skipping and the item is null, then skip it!
            if (item == null) return true;

            Object prop = _rowOverrideColumn.uniquingValue(table(), item);
            if (prop == _skippingProperty || (prop != null && prop.equals(_skippingProperty))) return true;
        }
        return false;
    }

    // Pivot Config panel support
    public AWResponseGenerating configurePivotLayout ()
    {
        AWTPivotConfigurationPanel panel = (AWTPivotConfigurationPanel) _dataTable.pageWithName(AWTPivotConfigurationPanel.class.getName());
        panel.setClientPanel(true);
        panel.setPivotState(this);
        return panel;
    }

    public void toggleShowingRowAttributes ()
    {
        // Need to invalidate _displayedColumns to force recomputation of _rowAttributeFields
        _showingRowAttributes = !_showingRowAttributes;
        _dataTable._displayedColumns = null;
        _dataTable.pushTableConfig();
    }


    public static class PivotGroup extends AWTDisplayGroup.GroupingState
    {
        // _objects: are the table items that appear on this pivot row.  Their index position corresponds to the
        //           groupSlot() of the EdgeCell().
        // _rowNestingLevel: the level on the row edge that this differs from the previous group
        Object[] _objects;
        int _rowNestingLevel;

        public PivotGroup (Object groupingObject, int slots, int rowNestingLevel)
        {
            _objects = new Object[slots];
            _rowNestingLevel = rowNestingLevel;
            // None of these are probably meaningful for us...
            isExpanded = false;
            count = 0;
            groupingRow = groupingObject;
        }

        public void put(Object o, int slot)
        {
            if (slot >= _objects.length) {
                // need to grow
                int newLen = (slot < 4) ? 4 : (slot + 1) * 2;
                Object[] array = new Object[newLen];
                System.arraycopy(_objects, 0, array, 0, _objects.length);
                _objects = array;
            }
            if (_objects[slot] != null) {
                Assert.that(_objects[slot] == null, "Assigning to already used grouping slot -- you likely haven't included all of the necessary row/column fields, Object1=%s, Object2=%s", _objects[slot], o);
            }
            _objects[slot] = o;
        }

        public Object get(int slot)
        {
            return (slot < _objects.length) ? _objects[slot] : null;
        }

        public void markUsed (boolean[] mask)
        {
            int count = Math.min(mask.length, _objects.length);
            for (int i=0; i < count; i++) {
                if (_objects[i] != null) mask[i] = true;
            }
        }

        public int rowNestingLevel ()
        {
            return _rowNestingLevel;
        }

        Iterator iterator ()
        {
            return new Iter();
        }

        public int slotCount ()
        {
            return _objects.length;
        }

        private class Iter implements java.util.Iterator {
            int _pos = -1;

            public Iter () {
                seek();
            }

            private void seek () {
                while (++_pos < _objects.length && _objects[_pos] == null);
                if (_pos >= _objects.length) _pos = -1;
            }
            public boolean hasNext () {
                return _pos != -1;
            }

            public Object next () {
                if (_pos == -1) throw new java.util.NoSuchElementException();
                Object result = _objects[_pos];
                seek();
                return result;
            }

            public void remove () {
                Assert.that(false, "not implemented");
            }
        }

        public int hashCode ()
        {
            // ordered hash of non-null object (which preserves match for extra trailing nulls)
            int hash = 1;
            for (int i=0, c=_objects.length; i < c; i++) {
                if (_objects[i] != null) hash = hash*31 + _objects[i].hashCode();
            }
            return hash;
        }

        public boolean equals (Object o)
        {
            if ((o == null) || !(o instanceof PivotGroup)) return false;
            Object[] oA1 = _objects, oA2 = ((PivotGroup)o)._objects;
            int c = Math.min(oA1.length, oA2.length);
            for (int i=0; i < c; i++) {
                Object o1 = oA1[i], o2 = oA2[i];
                if (o1 != o2 && (o1 == null || o2 == null || !o1.equals(o2))) return false;
            }
            // if the two arrays are of different length, but the longer is empty
            // in its extra elements, then they're still equal
            int d = oA1.length - oA2.length;
            if (d != 0) {
                Object[] longer = (d < 0) ? oA2 : oA1;
                for (int i=c; i < longer.length; i++) {
                    if (longer[i] != null) return false;
                }
            }
            return true;
        }
    }


    /*
        EdgeCells form a tree that represent the set of unique values on the column edge of the table.
        One level of the tree (currently always the bottom one) represent the Attribute Columns
        (e.g. Price, Quantity) while the other levels represent Grouping levels (like Supplier, Region, ...)
        Cells are insertion-sorted according to the by value based on the sortOrdering for their column.

        Ultimately we allocate PivotEdgeColumns for the leaves of the EdgeCell tree.
        These point back to their EdgeCells which in turn know the groupSlot in the PivotGroupState where
        the object that should be rendered for that cell will be found, and the _attributeColumn that
        should be used to render it.

        See CellVisitor for the mechanism to efficiently build and traverse that EdgeCell tree.
     */
    public static class EdgeCell
    {
        // It's weird that we need an object here -- it seems bogus to just happen to hold onto
        // one business object instead of the value -- however, SortOrderings work with objects, not
        // values, and objects are also what AWTColumns want to render...
        Object _object;
        Object _value;
        Column _column;
        Column _attributeColumn;
        Column _edgeColumn;
        EdgeCell _parent;
        List _children;
        int _span = -1;
        int _groupSlot = -1;

        // for cells with null dimension values, we mark whether they share any row with another cell,
        // or can share that row (be collapsed)
        boolean _isUsed;
        boolean _isNullValue;
        boolean _canCollapse;
        EdgeCell _collapsedSibling;
        EdgeCell _firstVisibleChild;
        boolean _isHidden;

        public EdgeCell (Object object, Object value, Column column, EdgeCell parent, Column attributeColumn)
        {
            Assert.that(object != null || parent == null, "EdgeCell must have non-null object");
            _object = object;
            _value = value;
            _column = column;
            _parent = parent;
            _children = null;
            _attributeColumn = attributeColumn;
            // initially true, but set to false if detected to share row with sibling
            _canCollapse = false;
        }

        EdgeCell addOrFind(Object object, CellVisitor visitor)
        {
            Column column = visitor._column;
            CellVisitor childVisitor = visitor._next;
            EdgeCell matchingChild = null;

            // If this column is at AttributeColumn, then we need to fan out here
            if (column instanceof PivotAttributesColumn) {
                // if we've been here before then our children are already here
                if (_children == null) {
                    List attributeColumns = ((PivotAttributesColumn)column).columns(object);
                    _children = ListUtil.list();
                    for (int i=0, count=attributeColumns.size(); i < count ; i++) {
                        Column attributeColumn = (Column)attributeColumns.get(i);
                        matchingChild = new EdgeCell(object, null, attributeColumn, this, attributeColumn);
                        _children.add(matchingChild);
                    }
                }
                // recurse
                for (int i=0, count=_children.size(); i < count ; i++) {
                    matchingChild = (EdgeCell)_children.get(i);
                    if (childVisitor != null) {
                        matchingChild = matchingChild.addOrFind(object, childVisitor);
                    }
                }
            } else {
                if (_children == null) _children = ListUtil.list();
                matchingChild = visitor.matchingChild(this, object);
                if (matchingChild == null) {
                    Object value = visitor._sortOrdering.getSortValue(object);
                    matchingChild = new EdgeCell(object, value, column, this, null);
                    visitor.insertChildAtCurrentPos(this, matchingChild);
                } else {
                    // Update the object with a more up to date object instance
                    matchingChild._object = object;
                }

                // recurse to next level
                if (childVisitor != null) {
                    matchingChild = matchingChild.addOrFind(object, childVisitor);
                }
            }
            return matchingChild;
        }

        EdgeCell find (Object object, CellVisitor visitor)
        {
            CellVisitor childVisitor = visitor._next;
            // PivotAtttributeCells have no sortOrdering, so we just match on the first child
            EdgeCell matchingChild = (visitor._sortOrdering == null)
                ? (EdgeCell)_children.get(0)
                : visitor.matchingChild(this, object);
            if (matchingChild == null) return null;
            return (childVisitor != null) ? matchingChild.find(object, childVisitor) : matchingChild;
        }

        boolean shouldRender ()
        {
            return _isUsed && !_isHidden;
        }

        void updateColumnVisibility (List items, final AWTPivotState pivotState)
        {
            // clear out state
            visit(new SimpleCellVisior() { public boolean process(EdgeCell cell) {
                        cell.invalidateLayout();
                        cell.computeIsNull(pivotState);
                        return false;
                    }
                }, true, true);

            // run through all items, marking used cells
            Map groupingStates = pivotState.table().displayGroup().groupingValueState();
            for (int i=0, count=items.size(); i < count; i++) {
                PivotGroup groupingState = (PivotGroup)groupingStates.get(items.get(i));
                checkUsage(groupingState);
            }

            // hide collapsible unmatrixed cells
            visit(new SimpleCellVisior() { public boolean process(EdgeCell cell) {
                        cell.hideCollapsibleCells(); return false;
                    }
                }, true, true);

            propagateIsUsed();
        }

        void checkUsage (PivotGroup group)
        {
            // iterate through children
            if (_parent != null && _groupSlot != -1 && _column != null && !isAttributeCell()) {
                boolean used = (group.get(_groupSlot) != null);
                if (!_isUsed) _isUsed = used;
                if (_isNullValue && used && _canCollapse) {
                    // I'm populated; what about my siblings?
                    List siblings = _parent._children;
                    for (int i=0, count = siblings.size(); i<count; i++) {
                        EdgeCell child = (EdgeCell)siblings.get(i);
                        if (child != this && (child._groupSlot != -1) && (group.get(child._groupSlot) != null)) {
                            _canCollapse = false;
                            break;
                        }
                    }
                }
            }
            else if (_children != null) {
                if (_parent == null) _isUsed = true;
                for (int i=0, count = _children.size(); i<count; i++) {
                    EdgeCell child = (EdgeCell)_children.get(i);
                    child.checkUsage(group);
                }
            }
        }

        void hideCollapsibleCells ()
        {
            if (_isNullValue && _canCollapse) {
                List visibleChildren = _parent.visibleChildren();
                if (visibleChildren.size() > 2) {
                    EdgeCell sibling = (EdgeCell)visibleChildren.get(1);
                    _isUsed = false;
                    sibling.visit(new SimpleCellVisior() {
                        public boolean process(EdgeCell cell) {
                            cell._collapsedSibling = EdgeCell.this;  return false;}
                        }, true, true);
                }
            }
        }

        void propagateIsUsed ()
        {
            if (_children != null) {
                for (int i=0, count=_children.size(); i<count; i++) {
                    EdgeCell child = (EdgeCell)_children.get(i);
                    child.propagateIsUsed();
                }
            }
            if (_parent != null) {
                if (isAttributeCell()) _isUsed = _parent._isUsed;
                else if (_isUsed) _parent._isUsed = true;
            }
        }

        void computeIsNull (AWTPivotState pivotState)
        {
            _isNullValue = false;
            if (_parent != null && (_attributeColumn == null) && (_column != null)) {
                // Need to establish our object and prepare column to see if this dimension value is null / blank
                AWTDataTable table = pivotState.table();
                table.setCurrentItem(_object);
                _column.prepare(table);
                _isNullValue = _column.isBlank(table);
                if (_isNullValue) {
                    _canCollapse = true;
                }
            }
        }

        void invalidateLayout ()
        {
            _isUsed = false;
            _span = -1;
            _canCollapse = false;
            _collapsedSibling = null;
            _firstVisibleChild = null;
        }

        int mappedSlot (PivotGroup group)
        {
            int slot = groupSlot();
            Assert.that(slot != -1, "Use of edgeCell with unassigned groupSlot");
            if (group.get(slot) == null && _collapsedSibling != null) slot = _collapsedSibling.groupSlot();
            return slot;
        }

        public Object itemInGroup (PivotGroup group)
        {
            int slot = groupSlot();
            Assert.that(slot != -1, "Use of edgeCell with unassigned groupSlot");
            Object result = group.get(slot);
            if (result == null && _collapsedSibling != null) result = group.get(_collapsedSibling.groupSlot());
            return result;
        }

        boolean renderAsBreakColumn (PivotGroup group)
        {
            EdgeCell objectCell = objectCell();
            EdgeCell objectCellParent = objectCell._parent;
            if (objectCellParent == null) {
                // if we're an attribute child of the root, then no break
                return _parent._parent != null || isFirstChild();
            } else if (!isAttributeCell() || isFirstChild()) {
                // Are we spanned by our parent's predecessor?
                EdgeCell firstObjectCell = objectCellParent.firstVisibleChild();
                if (firstObjectCell == objectCell || firstObjectCell._collapsedSibling == null
                        || group.get(firstObjectCell._collapsedSibling.groupSlot()) == null) return true;
            }
            return false;
        }

        List visibleChildren ()
        {
            if (_children == null) return null;
            List result = ListUtil.list();
            for (int i=0, count=_children.size(); i<count; i++) {
                EdgeCell child = (EdgeCell)_children.get(i);
                if (child.shouldRender()) result.add(child);
            }
            return result;
        }

        EdgeCell firstVisibleChild ()
        {
            if (_firstVisibleChild == null) {
                List visibleChildren = visibleChildren();
                _firstVisibleChild = (visibleChildren.size() > 0) ? (EdgeCell)visibleChildren.get(0) : this;
            }
            return (_firstVisibleChild != this) ? _firstVisibleChild : null;
        }

        // Create DataTable Columns that will render the leaf edge cells
        void createColumnsForLeafLevel(List collection)
        {
            if (_children == null) {
                if (shouldRender()) {
                    // we're a leaf -- create / cache a column
                    if (_edgeColumn == null) _edgeColumn =  new PivotEdgeColumn(this);
                    collection.add(_edgeColumn);
                }
            } else {
                // recurse
                for (int i=0, count = _children.size(); i<count; i++) {
                    EdgeCell child = (EdgeCell)_children.get(i);
                    child.createColumnsForLeafLevel(collection);
                }
            }
        }

        boolean shouldCollapseCurrentLevel (int level, AWTPivotState pivotState)
        {
            if (level == 0) {
                return pivotState.shouldCollapseColumn(_column, (_isNullValue ? 0 : 1));
            } else {
                // Inefficient implementation -- we can precompute and cache this!
                List visibleChildren = visibleChildren();
                int count = (visibleChildren != null) ? visibleChildren.size() : 0;

                if (count != 1) return false;

                EdgeCell child = (EdgeCell)visibleChildren.get(0);
                return child.shouldCollapseCurrentLevel(level -1, pivotState);
            }
        }

        public void collectCellsAtLevel(List collection, int level)
        {
            if (level == 0) {
                   if (shouldRender()) collection.add(this);
            } else {
                // recurse
                int count = (_children != null) ? _children.size() : 0;
                for (int i=0; i<count; i++) {
                    EdgeCell child = (EdgeCell)_children.get(i);
                    child.collectCellsAtLevel(collection, level - 1);
                }
            }
        }

        // colspan for this cell (1 for leaves, more for parents...)
        public int span ()
        {
            if (_span == -1) {
                if (_children == null) {
                    // we're a leaf
                    _span = shouldRender() ? 1 : 0;
                } else {
                    // recursively sum of span of our children
                    _span = 0;
                    int count = _children.size();
                    for (int i=0; i<count; i++) {
                        EdgeCell child = (EdgeCell)_children.get(i);
                        _span += child.span();
                    }
                }
            }
            return _span;
        }

        public void setHidden (boolean yn)
        {
            if (_isHidden != yn) {
                _isHidden = yn;
                // invalidate span
                for (EdgeCell cell=this; cell != null; cell=cell._parent) {
                    cell._span = -1;
                }
            }
        }

        public boolean isAttributeCell ()
        {
            return  _column == _attributeColumn;
        }

        // The index in the group array for the value of this edgeCell
        // For leaves, returns the actual index, for parent returns the last index
        public int groupSlot ()
        {
            if (_groupSlot == -1) {
                if (_parent != null && isAttributeCell()) {
                    _groupSlot = _parent.groupSlot();
                } else {
                    EdgeCell root = root();
                    _groupSlot = ++root._groupSlot;
                }
            }
            return _groupSlot;
        }

        public EdgeCell objectCell ()
        {
            Assert.that(_children == null, "Should be called only on leaf cells");
            return isAttributeCell() ? _parent : this;
        }

        EdgeCell root ()
        {
            EdgeCell cell = this;
            while (cell._parent != null) cell = cell._parent;
            return cell;
        }

        // what's the effective rendering column for this cell
        public Column attributeColumn ()
        {
            if (_attributeColumn != null) return _attributeColumn;
            if (_parent != null) return _parent.attributeColumn();
            // we're the root -- return our attribute
            Assert.that(_column != null, "Got to root edge cell with no attribute column");
            return _column;
        }

        public Column column ()
        {
            return _column;
        }

        public Object object ()
        {
            return _object;
        }

        public boolean isFirstChild ()
        {
            return (_parent == null) || (_parent.firstVisibleChild() == this);
        }

        public boolean visit (SimpleCellVisior visitor, boolean includeRoot, boolean recurse)
        {
            if (includeRoot) {
                if (visitor.process(this)) return true;
            }
            if (_children != null) {
                for (int i=0, count = _children.size(); i<count; i++) {
                    EdgeCell child = (EdgeCell)_children.get(i);
                    if (visitor.process(child)) return true;
                    if (recurse) {
                        if (child.visit(visitor, false, recurse)) return true;
                    }
                }
            }
            return false;
        }
    }

    protected interface SimpleCellVisior
    {
        public boolean process (EdgeCell cell);
    }

    /*
        CellVisitors encapsulate state for efficiently walking the EdgeCell tree.
        Since we always process rows in fully sorted order, we always want to walk
        *forward* (in a depth first search sort of way) through the EdgeCells.
        We pre-compute a CellVisitor stack (linked list) and each visitor remembers
        where we last were on its level of the EdgeCell tree.  When we search, we always
        search forward from that position (until our client hits a new major group,
     */
    public static class CellVisitor
    {
        Column _column;
        AWTSortOrdering _sortOrdering;
        int _nextPos;
        CellVisitor _next;

        protected CellVisitor (Column column, AWTDataTable table)
        {
            _column = column;
            column.prepare(table);
            _sortOrdering = column.createSortOrdering(table);
            _nextPos = 0;
            _next = null;
        }

        public EdgeCell matchingChild (EdgeCell parent, Object object)
        {
            // search forward from pos until next element > us
            List children = parent._children;
            if (children == null) return null;
            boolean alreadyResetChildren = false;
            while (_nextPos < children.size()) {
                EdgeCell child = (EdgeCell)children.get(_nextPos);
                Object value = _sortOrdering.getSortValue(object);
                int comp = (_sortOrdering != null) ? _sortOrdering.compareValues(value, child._value) : 0;
                if (comp == 0) return child;

                // we didn't match, so the search should restart at the next level
                if (_next != null  && !alreadyResetChildren) {
                    _next.resetSearchPosition();
                    alreadyResetChildren = true;
                }

                // parent is "bigger" -- we should be inserted here
                if (comp < 0) {
                    return null;
                }
                _nextPos++;
            }
            return null;
        }

        public void insertChildAtCurrentPos (EdgeCell parent, EdgeCell child)
        {
            parent._children.add(_nextPos, child);
        }

        public void resetSearchPosition ()
        {
            _nextPos = 0;
            if (_next != null) _next.resetSearchPosition();
        }
    }

    // Create a Visitor chain for navigating EdgeCells corresponding to the supplied columns
    public static CellVisitor createVistors (Column[] columns, AWTDataTable table)
    {
        CellVisitor root = null;
        CellVisitor parent = null;
        for (int i=0; i < columns.length; i++) {
            CellVisitor vistor = new CellVisitor(columns[i], table);
            if (root == null) root = vistor;
            if (parent != null) parent._next = vistor;
            parent = vistor;
        }
        return root;
    }

    /*
        PivotEdgeColumns are allocated for each leaf in the (column) EdgeCell tree
        and are added to the DataTable displayedColumns list for rendering.
        They, in turn, use their EdgeCell to set the correct object in the current GroupingState
        as the currentItem on the DataTable/DisplayGroup and then use their EdgeCell's attributeColumn
        to do the actual value rendering.
     */
    public static class PivotEdgeColumn extends ProxyColumn
    {
        EdgeCell _edgeCell;

        public PivotEdgeColumn (EdgeCell cell)
        {
            super(cell.attributeColumn());
            _edgeCell = cell;
        }

        public boolean shouldRenderDataColumn (AWTDataTable table)
        {
            return (_edgeCell._children == null && !_edgeCell.isAttributeCell());
        }

        public EdgeCell edgeCell()
        {
            return _edgeCell;
        }

        public Column prepareAndReplace (AWTDataTable table)
        {
            if (table._renderingHeader) {
                // if we're a leaf cell and we have only one columnField then we render the data column, not the attribute column
                if (shouldRenderDataColumn(table)) {
                    table.setCurrentItem(_edgeCell._object);
                    return _edgeCell._column;
                }
            } else {
                // If we're in the body (have a current item) then blank if we have no corresponding object in the group
                Object origItem = table._displayGroup.currentItem();

                if (origItem != null) {
                    AWTPivotState pivotState = table._pivotState;
                    PivotGroup group = (PivotGroup)table._displayGroup.currentGroupingState();
                    Assert.that(group != null, "Did Prepare and replace with no grouping state");

                    boolean isAttributeAreaColumn = table._currentColumnIndex >= pivotState._firstAttributeColumnIndex;
                    if (isAttributeAreaColumn && _edgeCell.renderAsBreakColumn(group)) {
                        table._isBreakColumn = true;
                    }

                    int slot = _edgeCell.groupSlot();
                    Object currentItem = _edgeCell.itemInGroup(group);
                    // if we have no item or we're already skipping all remaining value in this cell, then bail
                    if (currentItem == null) return AWTDataTable.BlankColumn;

                    // see if we have and override for all this items attribute columns

                    if (pivotState._currentGroupObject != currentItem) {
                        // Set item for this group on the table so it's there when we render
                        if (origItem != currentItem) table.setCurrentItem(currentItem);
                        pivotState._currentGroupObject = currentItem;
                        pivotState._skippingSlot = -1;

                        // if we're new to this item, then check if we have an override
                        if (table._renderingPrimaryRow) {
                            String name;
                            if (pivotState._overrideAttributeBinding != null
                                  && (name = (String)table.valueForBinding(pivotState._overrideAttributeBinding)) != null)
                            {
                                    pivotState._rowOverrideColumn = table.findColumnForKey(name);
                                    Assert.that(pivotState._rowOverrideColumn != null, "overrideColumn '%s' not found", name);
                            } else {
                                pivotState._rowOverrideColumn = null;
                            }
                        }
                    } else {
                        // if we're already skipping all remaining value in this cell, then bail
                        if (pivotState._skippingSlot == slot) return AWTDataTable.BlankColumn;
                    }

                    Column override = pivotState._rowOverrideColumn;

                    if (override != null) {
                        // we've overriding, but we need to start after the selection column...
                        // (unless we're rendering in the detail row label area...)
                        if ((!table._renderingPrimaryRow && !isAttributeAreaColumn)
                                    || _target.isValueColumn(table)) {
                            // after this one, skip the rest for this item
                            pivotState._skippingSlot = slot;
                            Column replacement = override.prepareAndReplace(table);
                            pivotState._skippingProperty = replacement.uniquingValue(table, currentItem);
                            return replacement;
                        }
                        else if (!table._renderingPrimaryRow) return AWTDataTable.BlankColumn;
                    }
                }
            }
            return super.prepareAndReplace(table);
        }

        public boolean isBlank(AWTDataTable table) {
            AWTPivotState pivotState = table._pivotState;
            PivotGroup group = (PivotGroup)table._displayGroup.currentGroupingState();
            Object item = _edgeCell.itemInGroup(group);
            return  ((item == null || pivotState._skippingSlot == _edgeCell.groupSlot())
                             && !_edgeCell.renderAsBreakColumn(group))
                    || table._pivotState.skippingItem(item);
        }
    }


    /*
        The PivotAttributesColumn is used to create a sort of multi-column for the ColumnAttributes
        living as a level in the Column Edge.  The PivotAttributesColumn is "fanned out" in
        the EdgeCell tree, with one branch for each Attribute (Column) that it contains
     */
    public static class PivotAttributesColumn extends Column
    {
        AWTPivotState _pivotState;

        // we will render the *value* of the current edge cell in the header (e.g. "Staples" instead of "Supplier")
        public String rendererComponentName() {
            return "AWTPivotAttributesColumnRenderer";
        }

        public PivotAttributesColumn (AWTPivotState pivotState)
        {
            _pivotState = pivotState;
        }

        public List columns (Object obj)
        {
            return _pivotState.computeAttributeColumns(obj);
        }
    }


    /*
        This column type exists merely to be swapped in for the last non-attribute
        column position when rendering the label for Row Attributes (rendered as detail rows)
     */
    public static Column _RowAttributeLabelColumn = new RowAttributeLabelColumn();

    public static class RowAttributeLabelColumn extends Column
    {
        // we will render as a value the *label* of the _pivotState._rowOverrideColumn
        public String rendererComponentName() {
            return "AWTRowAttributeLabelColumnRenderer";
        }

        public boolean wantsSpan (AWTDataTable sender)
        {
            return true;
        }
    }

    public static Column _RowAttributeExpandoColumn = new RowAttributeExpandoColumn();

    public static class RowAttributeExpandoColumn extends Column
    {
        // we will render as a value the *label* of the _pivotState._rowOverrideColumn
        public String rendererComponentName() {
            return "AWTRowAttributeExpandoColumnRenderer";
        }

        public boolean isBlank (AWTDataTable table)
        {
            return !table._renderingPrimaryRow;
        }
    }

    /*
    ** Utilities to Support Pivot
    */
    protected List _sortOrderingArray (Column[] columns)
    {
        List result = ListUtil.list();
        for (int i=0, c=columns.length; i < c; i++) {
            columns[i].prepare(_dataTable);
            AWTSortOrdering so = columns[i].createSortOrdering(_dataTable);
            if (so != null) result.add(so);
        }
        return result;
    }

    protected static int _sortDiffIndex (List sorts, Object a, Object b)
    {
        int count = sorts.size();
        for (int i=0; i < count; i++) {
            if (((AWTSortOrdering)sorts.get(i)).compare(a,b) != 0) return i;
        }
        return -1;
    }

    protected Column[] createColumnArray (List columns)
    {
        int c = columns.size();
        Column[] array = new Column[c];
        while (c-- > 0) {
            array[c] = (Column)columns.get(c);
        }
        return array;
    }
/*
    // unneeded utilities from when we were diffing on values directly instead of using SortOrdering

    public static boolean isEqual (Object o1, Object o2, AWTSortOrdering sortOrdering)
    {
        if (o1 == o2) {
            return true;
        }
        if ((o1 == null) || (o2 == null)) {
            return false;
        }

        if (sortOrdering != null) {
            return (sortOrdering.compare(o1,o2) == 0);
        }

        return o1.equals(o2);
    }

    public static int arrayDiffIndex (Object[] a, Object[] b)
    {
        int count = a.length;
        for (int i=0; i < count; i++) {
            if (!isEqual(a[i], b[i], null)) return i;
        }
        return -1;
    }

    public static void getValues (Object obj, FieldPath[] fields, Object[] values)
    {
        int count = fields.length;
        for (int i=0; i < count; i++) {
            FieldPath path = fields[i];
            values[i] = ((path != null) ? path.getFieldValue(obj) : null);
        }
    }

    static FieldPath[] _fieldPathArray (Column[] columns)
    {
        int count = columns.length;
        FieldPath[] array = new FieldPath[count];
        for (int i=0; i < count; i++) {
            String keyPathString = columns[i].keyPathString();
            array[i] = ((keyPathString != null) ? FieldPath.sharedFieldPath(keyPathString) : null);
        }
        return array;
    }
*/    
}

/* Todo: (Pivot)
High:
    - Selection control:  if no column dimension fan out, show it on far left
        - new binding: pivotSelectionPerRow="$true"
    - If single column edge dimension value, move selection column to far left

    - Apply table config to pivot layout -- should not throw if references Columns not in config
        (processTableConfig -->columnsMatchingKeys)

Medium
    - (Maybe) suppress column field levels with only a single dimension value (if it's null only?)
    - If columnEdge level consists solely of a single Blank value, then suppress rendering its Column Heading row
    - Table config drag/drop -- was able to end up with ItemId on both RowFields and DetailAttributes.
    - Detect whether we've stopped using some columns and do a recompute in that case as well.
        - We check on outline manipulation, but not on every update to the set of itemValues?

    - Outline doesn't work if parent values are matrixed on Field on RowEdge (e.g. Supplier)
        - We have workaround -- hook for client to get PivotLayout (so can eliminate matrixed values in this case)
          but can we filter them automatically?

    - added colspan to ColumnRenderer Header -- what about other renderers (like fieldsui)?
    - Hiding column attribute -- in Layout page doesn't show up as hidden

Future:
    -- new optionalColumns implementation -- just attribute columns

    - If no columnAttributes, start rendering detail on the main row (not below)?

    - Do we need an attribute for columns to indicate whether they're groupable?
        - Then in pivot mode, default for groupable columns is to be on row edge, and for non-groupable as attribute

Features:
    - Show detail row field name in additional column (wedged between rowFields and the ColumnFields)

    - Group Selection -- i.e. multiselect control to select all values in columns or rows

    - Cosmetic: Default Background row color for levels (and maybe bold for non-leaf levels, white for details)

Perf:
    - OutlineState: pushFullList() -- only do it on change!
*/
