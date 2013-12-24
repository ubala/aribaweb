/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTDisplayGroup.java#84 $
*/
package ariba.ui.table;

import ariba.ui.outline.OutlineState;
import ariba.util.core.EqHashtable;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.OrderedList;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public final class AWTDisplayGroup
{
    protected List _allObjects;

    /**
     * This can be where we get our row data.
     */
    protected AWTDataSource _dataSource;
    protected boolean _didInitialFetch;

    /**
        source of truth on the selection
    */
    protected List _selectedObjects;
    /**
        the first of the selectedObjects
    */
    protected Object _selectedObject;
    protected int _batchStartIndex = 0;
    protected int _scrollTopIndex = 0;
    protected int _topBufferRowCount = 0;
    protected int _topBufferMoveUpRowCount = 0;
    protected int _numberOfObjectsPerBatch;
    protected boolean _forceSingleSelection;
    protected Map _currentItemExtras;
    protected Object _currentItem;
    protected Object _itemToForceVisible;

    /**
        OrderedList
    */
    protected Object _origOrderedList;
    protected Object[] _origAllObjects;
    protected OutlineState _outlineState;

    protected FieldPath _groupByFieldPath;
    /**
        Boolean for each grouping key value.
        Is a Map<Object, GroupingState>.
    */
    protected Map /*Object, GroupingState*/ _groupingValueState;
    protected Object _lastGroupingRow;
    protected GroupingState _lastGroupingState;
    protected AWTSortOrdering _groupBySortOrdering;

    protected int _groupingExpansionDefault = GroupingDefaultFitBatch;
    public static final int GroupingDefaultClosed = 0;
    public static final int GroupingDefaultAllOpen = 1;
    public static final int GroupingDefaultFirstOpen = 2;
    /**
        dynamically choose open all, or close all
    */
    public static final int GroupingDefaultFitBatch = 3;

    // these values are computed lazily -- null means not computed
    /**
        sorted and filtered
    */
    protected List _filteredObjects;
    /**
        current batch
    */
    protected List _displayedObjects;

    protected List<AWTSortOrdering> _sortOrderings;
    protected List<AWTSortOrdering> _primarySortOrderings;
    protected List<AWTSortOrdering> _effectiveSortOrderings;
    protected boolean _useBatching;

    /**
     * This is the column that should hear about value source changes.
     */
    private Map<AWTDataTable.Column, Object> _itemListeners = null;

    /**
     * This is the the containing table.
     */
    private AWTDataTable _owningTable = null;

    /*******************************************************************************
     * Constructor and misc
     *******************************************************************************/
    public AWTDisplayGroup ()
    {
        setObjectArray(null);
        setUseBatching(false);
    }

    /**
     * Tell us about a column that needs to hear about changes
     * to the current item.<br>
     * A display group will inform a single column about changes
     * to the current item via callbacks to
     * <code>AWTDataTable.Column.setCurrentItem()</code>
     * @param listener the column that is told about current items
     * @aribaapi private
     */
    public void addItemListener (AWTDataTable.Column listener, Object listenerState)
    {
        /*
            This method is a slight wart, needed for ARWTGroup.  That special
            kind of column needs to distribute current item changes to its
            contained groupviews (so that they are gradually told the list
            of items).  Note that since there is only one listener, we only
            support a single ARWTGroup.
        */
        if(_itemListeners == null) {
            _itemListeners = new LinkedHashMap<AWTDataTable.Column, Object>();
        }
        _itemListeners.put(listener,listenerState);
    }

    public Object getListenerState (AWTDataTable.Column listener)
    {
        return MapUtil.nullOrEmptyMap(_itemListeners) ? null : _itemListeners.get(listener);
    }

    /**
     * Tell this displaygroup what table owns it
     * @param table the owning table
     * @aribaapi private
     */
    protected void setOwningTable (AWTDataTable table)
    {
        _owningTable = table;
    }

    public AWTPivotState pivotState ()
    {
        return _owningTable.pivotState();
    }

    /*******************************************************************************
     * Caching Support and misc methods
     *******************************************************************************/

    public void setObjectArray (List allObjects)
    {
        if (allObjects == null) {
            allObjects = ListUtil.list();
        }

        // if we had objects before then use simple heuristic to determine if we
        // should reset batch / scroll position.
        if (_origAllObjects != null) {
            int diff = _origAllObjects.length - allObjects.size();
            if (diff > 1 || diff < -1) {
                setScrollTopIndex(0);
                _isResetScrollTop = true;
            }
        }
        else {
            setScrollTopIndex(0);
        }

        _allObjects = allObjects;
        _origAllObjects = allObjects.toArray();

        // clear our state associated with the old objects
        _currentItemExtras = null;
        if (_grouper != null) _grouper.invalidate();
        _groupingValueState = null;
        setCurrentItem(null);

        // invalidate dependent values
        updateDisplayedObjects();

        // Use validateSelection() to attempt to preseve any compatible portions
        // of the selection...
        _validateSelection();

        if (_itemListeners != null) {
            for (AWTDataTable.Column listener : _itemListeners.keySet()) {
                listener.rowsReset(_owningTable);
            }
        }
    }

    private boolean _isResetScrollTop = false;
    public boolean isResetScrollTop ()
    {
        boolean returnVal = _isResetScrollTop;
        _isResetScrollTop = false;
        return returnVal;
    }

    /**
     * TODO: move to ListUtil.
     * @param list
     * @return
     */
    public static List vectorFromOrderedList (Object list)
    {
        List result = null;
        if (list != null) {
            if (list instanceof List) {
                return (List)list;
            }
            OrderedList orderedList = OrderedList.get(list);
            int count = orderedList.size(list);
            result = ListUtil.list(count);
            for (int i=0; i<count; i++) {
                result.add(orderedList.elementAt(list, i));
            }
        }
        return result;
    }

    /**
     * TODO: move to ListUtil or ArrayUtil.
     * @param list
     * @return
     */
    public static Object[] arrayFromOrderedList (Object list)
    {
        Object[] result = null;
        if (list != null) {
            if (list instanceof List) {
                return ((List)list).toArray();
            }
            OrderedList orderedList = OrderedList.get(list);
            int count = orderedList.size(list);
            result = new Object[count];
            for (int i=0; i<count; i++) {
                result[i] = orderedList.elementAt(list, i);
            }
        }
        return result;
    }

    /**
     * TODO: move to ListUtil or ArrayUtil.
     * @param list
     * @return
     */
    public static boolean orderedListArrayMatch (Object list, Object[] array)
    {
        OrderedList orderedList = (list != null) ? OrderedList.get(list) : null;

        if (list == null && array == null) {
            // if both null, then no change
            return true;
        }

        if ((list == null || array == null) || (orderedList.size(list) != array.length)) {
            // if one null and not the other, then changed
            return false;
        }

        // otherwise, we need to walk cached list and see if we happen
        // to have the same list
        for (int i=0, size=array.length; i < size; i++) {
            if (orderedList.elementAt(list, i) != array[i]) {
                return false;
            }
        }

        return true;
    }


    /**
     * Check if our objectArray is out of sync.<br>
     * As side effect, if the object array has changed, remember the list
     * as the new value.
     * @param list The new object array
     * @aribaapi private
     */
    public void checkObjectArray (Object list)
    {
        if (!orderedListArrayMatch(list, _origAllObjects)) {
            setObjectArray(vectorFromOrderedList(list));
        }
    }

    public void checkDataSource ()
    {
        if (_dataSource != null && (_dataSource.hasChanges() || !_didInitialFetch)) {
            _didInitialFetch = true;
            List list = _dataSource.fetchObjects();
            checkObjectArray(list);
        }
    }

    public void hibernate ()
    {
        setObjectArray(ListUtil.list());
        _didInitialFetch = false;
    }

    public List allObjects ()
    {
        return _allObjects;
    }

    /** This returns the concatenation of the primarySortOrdering and sortOrderings. */
    public List<AWTSortOrdering> effectiveSortOrderings ()
    {
        if (_effectiveSortOrderings == null) {
            _effectiveSortOrderings = ListUtil.list();
            if (_primarySortOrderings != null) {
                _effectiveSortOrderings.addAll(_primarySortOrderings);
            }
            if (_sortOrderings != null) {
                _effectiveSortOrderings.addAll(_sortOrderings);
            }
        }
        return _effectiveSortOrderings;
    }

    /**
     *
     * @param list of type List&lt;AWTSortOrdering&gt;
     * @param key
     * 
     * @return
     */
    static public AWTSortOrdering orderingMatchingKey (List /*AWTSortOrdering*/ list, String key)
    {
        if (list != null) {
            for (int i=0, count=list.size(); i < count; i++) {
                AWTSortOrdering ordering = (AWTSortOrdering)list.get(i);
                if (ordering.key().equals(key)) return ordering;
            }
        }
        return null;
    }

    public List computeSortedObjects (List objects)
    {
        List<Object> sortedObjects = null;
        List<AWTSortOrdering> orderings = effectiveSortOrderings();
        if (!ListUtil.nullOrEmptyList(orderings)) {
            sortedObjects =
                AWTSortOrdering.sortedArrayUsingKeyOrderArray(objects, orderings);
        }
        else {
            sortedObjects = ListUtil.collectionToList(objects);
        }

        return sortedObjects;
    }

    /**
     * @throws NullPointerException if {@link #setObjectArray}() has not been
     *   called yet.  setObjectArray will call this though...
     *
     * @return
     */
    public List filteredObjects ()
    {
        if (_filteredObjects == null) {
            // NPE if _allObjects null
            _filteredObjects = sortedMainList(_allObjects);
            if (_grouper != null) {
                _grouper.validateFilteredList(_filteredObjects);
            }
            detailExpansionPrepareForObjects(_filteredObjects);
        }
        return _filteredObjects;
    }

    // called from OutlineState to sort / group child lists
    public List sortedMainList (List objects)
    {
        boolean dataSourceSorts = _dataSource != null && _dataSource.dataSourceDoesSort();
        List sorted = dataSourceSorts
                      ? ListUtil.collectionToList(objects)
                      : computeSortedObjects(objects);
        return groupObjects(sorted);
    }

    // called from OutlineState to sort / group child lists
    public List sortedChildList (List objects)
    {
        return groupObjects(computeSortedObjects(objects));
    }

    /**
     * TODO: move to ListUtil.
     * @param l1
     * @param l2
     * @return
     */
    public static boolean listsIdentical (List l1, List l2)
    {
        if (l1 == null || l2 == null || l1 == l2) {
            return l1 == l2;
        }
        int count = l1.size();
        if (l2.size() != count) return false;

        for (int i=0; i<count; i++) {
            if (l1.get(i) != l2.get(i)) return false;
        }
        return true;
    }

    /**
        Called by OutlineRepetition with the unrolled set of objects.  For the purpose
        of batching, etc we need to treat this as the full displayed set.
     */
    public void setOutlineAllObjects (List list)
    {
        boolean shouldCheck = !listsIdentical(_filteredObjects, list);
        _filteredObjects = list;
        if (shouldCheck) {
            if (_grouper != null) _grouper.validateFilteredList(_filteredObjects);

            // update selection to remove any objects in collapsed set
            _validateSelection();
        }

        // Update our itemVisible batching
        if (_itemToForceVisible != null) {
            _setItemToForceVisible(_itemToForceVisible, _itemToForceVisible);
        }
    }

    /**
     * TODO: refactor, make static, move to ObjectUtil(?).
     * @param o1
     * @param o2
     * @return
     */
    protected boolean hasChanged (Object o1, Object o2)
    {
        if (o1 == o2) {
            return false;
        }
        if (((o1 == null) && (o2 != null)) || ((o2 == null) && (o1 != null))) {
            return true;
        }
        if (o1 == this || o2 == this) {
                // not both the same && neither is null
            return true;
        }
        if (_groupBySortOrdering != null) {
            return (_groupBySortOrdering.compare(o1,o2) != 0);
        }

        return !o1.equals(o2);
    }

    /*******************************************************************************
     * Grouping Support
     *******************************************************************************/

    /**
     *
     * @param sortedObjects may not be null.
     * 
     * @return
     */
    protected List groupObjects (List sortedObjects)
    {
        if (_grouper != null) {
            return _grouper.groupObjects(sortedObjects);
        }

        if (_groupByFieldPath == null && _groupBySortOrdering == null) {
            return sortedObjects;
        }

        int effectiveGroupingExpansion = _groupingExpansionDefault;
        if (_groupingExpansionDefault == GroupingDefaultFitBatch) {
            effectiveGroupingExpansion =
                ((sortedObjects.size() <= numberOfObjectsPerBatch()) ?
                 GroupingDefaultAllOpen :
                 GroupingDefaultClosed);
        }

        List groupedObjects = ListUtil.list();
        Iterator e = sortedObjects.iterator();
        /**
            ensure that we register a change on the first obj, even if null
        */
        // we're using an arbitrary non-null, non-matching value
        Object lastValue = null;
        boolean addingFromCurrentGroup = true;
        GroupingState groupingState = null;
        Map<Object, GroupingState> groupingStates = groupingValueState();

        // Filter all grouped objects that are not expanded
        while (e.hasNext()) {
            Object object = e.next();
            Object currentValue = (_groupBySortOrdering == null) ?
                _groupByFieldPath.getFieldValue(object) : object;
                // marker key for null
            currentValue = (currentValue != null) ? currentValue : this;

            if (lastValue == null || hasChanged(currentValue, lastValue)) {
                //addOrFindd grouping row keyed by current object
                groupingState = groupingStates.get(object);

                if (groupingState == null) {
                    groupingState = new GroupingState();
                    groupingValueState().put(object, groupingState);
                    boolean expandBecauseFirst =
                        (effectiveGroupingExpansion == GroupingDefaultFirstOpen)
                        && groupedObjects.isEmpty();
                    boolean expandAll =
                        effectiveGroupingExpansion == GroupingDefaultAllOpen;
                    groupingState.isExpanded = expandAll || expandBecauseFirst;
                }

                groupingState.groupingRow = object;
                groupingState.count = 0;

                lastValue = currentValue;
                addingFromCurrentGroup = groupingState.isExpanded;
                groupedObjects.add(object);
            }
            else {
                // register this object with the grouping state for the group
                groupingValueState().put(object, groupingState);

                if (addingFromCurrentGroup) {
                    groupedObjects.add(object);
                }
            }

            groupingState.count++;
        }

        return groupedObjects;
    }

    public int groupingExpansionDefault ()
    {
        return _groupingExpansionDefault;
    }

    public void setGroupingExpansionDefault (int value)
    {
        _groupingExpansionDefault = value;
            // force recomputation
        _groupingValueState = null;
    }

    public GroupingState currentGroupingState ()
    {
        return groupingState(_currentItem);
    }

    protected void resetGroupingState ()
    {
        _groupingValueState = null;
        _lastGroupingRow = null;
        _lastGroupingState = null;
    }

    /**
     * Lazy inits the groupingValueState as needed.
     *
     * @return Returns a Map<Object, GroupingState>.
     */
    protected Map /*Object, GroupingState*/ groupingValueState ()
    {
        if (_groupingValueState == null) {
            _groupingValueState = new EqHashtable();
            _lastGroupingRow = null;
            _lastGroupingState = null;
        }
        return _groupingValueState;
    }

    public GroupingState groupingState (Object row)
    {
        if (_lastGroupingRow != row) {
            _lastGroupingState = (GroupingState)groupingValueState().get(row);
            _lastGroupingRow = row;
        }
        return _lastGroupingState;
    }

    public boolean isGroupingRow (Object row)
    {
        GroupingState state = groupingState(row);
        return (state != null) && (state.groupingRow == row);
    }

    public boolean isCurrentItemGrouping ()
    {
        return (_groupByFieldPath != null || _groupBySortOrdering != null) &&
                isGroupingRow(_currentItem);
    }

    public boolean isGroupingExpanded (Object row)
    {
        GroupingState state = groupingState(row);
        return (state != null) && (state.isExpanded);
    }

    public void setGroupingExpanded (Object row, boolean expanded)
    {
        GroupingState state = groupingState(row);
        if ((state != null) && (state.isExpanded != expanded)) {
            state.isExpanded = expanded;
            updateDisplayedObjects();
        }
    }

    public void setGroupSortOrdering (AWTSortOrdering ordering)
    {
            // invalidate expansion hashtable
        resetGroupingState();
        _groupBySortOrdering = ordering;

        if (ordering != null) {
            if (ListUtil.nullOrEmptyList(primarySortOrderings()) ||
                !(ordering.key().equals(((AWTSortOrdering)primarySortOrderings().get(0)).key()))) {
                List baseOrderings = ListUtil.list();
                ordering.setSelector(AWTSortOrdering.CompareAscending);
                baseOrderings.add(ordering);
                setPrimarySortOrderings(baseOrderings);
            }
        }
        else {
                // clear out the primary sort orderings
            setPrimarySortOrderings(null);
        }

        updateDisplayedObjects();
    }

    public void setGroupingKey (String keyPathString)
    {
        FieldPath path = ((keyPathString == null) ?
                          null :
                          FieldPath.sharedFieldPath(keyPathString));
        _groupByFieldPath = path;
            // invalidate expansion hashtable
        resetGroupingState();

        if (keyPathString != null) {
            if (ListUtil.nullOrEmptyList(primarySortOrderings()) ||
                !(keyPathString.equals(((AWTSortOrdering)primarySortOrderings().get(0)).key()))) {
                List baseOrderings = ListUtil.list();
                baseOrderings.add(
                    new AWTSortOrdering(keyPathString,
                                        AWTSortOrdering.CompareAscending));
                setPrimarySortOrderings(baseOrderings);
            }
        }
        else {
            // clear out the primary sort orderings
            setPrimarySortOrderings(null);
        }

        updateDisplayedObjects();
    }

    public String groupingKey ()
    {
        return (_groupByFieldPath == null) ? null : _groupByFieldPath.fieldPathString();
    }

    public boolean currentGroupingExpanded ()
    {
        return isGroupingExpanded(_currentItem);
    }

    public int currentItemGroupingCount ()
    {
        GroupingState state = groupingState(_currentItem);
        return (state == null) ? 0 : state.count;
    }

    public boolean isCurrentItemVisible ()
    {
        // only hidden if it's a non-expanded grouping row
        return (_groupByFieldPath == null && _groupBySortOrdering == null)
                || !isCurrentItemGrouping() || isGroupingExpanded(_currentItem);
    }

    public void setCurrentGroupingExpanded (boolean expanded)
    {
        setGroupingExpanded(_currentItem, expanded);
    }

    /*******************************************************************************
     * Detail Rows Support
     *******************************************************************************/

    protected Map<Object, Boolean>_detailRowExpansions;
    protected boolean _detailRowAutoCollapse;
    public enum InitialExpandType { None, First, Last };
    protected InitialExpandType _detailInitialExpansionType = InitialExpandType.None;

    public InitialExpandType detailInitialExpansionType ()
    {
        return _detailInitialExpansionType;
    }

    public void setDetailInitialExpansionType (InitialExpandType detailInitialExpansionType)
    {
        _detailInitialExpansionType = detailInitialExpansionType;
    }

    public boolean detailRowAutoCollapse ()
    {
        return _detailRowAutoCollapse;
    }

    public void setDetailRowAutoCollapse (boolean detailRowAutoCollapse)
    {
        _detailRowAutoCollapse = detailRowAutoCollapse;
    }

    boolean detailExpansionEnabled ()
    {
        return _detailRowExpansions != null;
    }

    void setDetailExpansionEnabled (boolean yn)
    {
        if (yn != detailExpansionEnabled()) {
            _detailRowExpansions = (yn) ? new IdentityHashMap() : null;
        }
    }

    void detailExpansionPrepareForObjects (List objects) {
        if (detailExpansionEnabled() && detailInitialExpansionType() != InitialExpandType.None
                && !ListUtil.nullOrEmptyList(objects)) {

            // If any of our old expansions apply, no need to auto expand
            for (Object o : _detailRowExpansions.keySet()) {
                if (ListUtil.indexOfIdentical(objects, o) != -1) return;
            }

            _detailRowExpansions.clear();
            Object o = (detailInitialExpansionType() == InitialExpandType.First)
                            ? objects.get(0) : ListUtil.lastElement(objects);
            _detailRowExpansions.put(o, true);
        }
    }

    public boolean currentDetailExpanded ()
    {
        return (_detailRowExpansions == null) || _detailRowExpansions.get(currentItem()) != null;
    }

    public void toggleCurrentDetailExpanded ()
    {
        boolean expanded = !currentDetailExpanded();
        if (_detailRowAutoCollapse) _detailRowExpansions.clear();
        if (expanded) {
            _detailRowExpansions.put(currentItem(), true);
        } else {
            _detailRowExpansions.remove(currentItem());
        }
    }


    /*******************************************************************************
     * Misc methods
     *******************************************************************************/

    /**
     * Use with care -- causes array to get created.
     * This is the list of objects that will be displayed between batchStartIndex() and batchEndIndex().
     * Clients rendering are best using filteredObjects() directly with AWFor set to use.
     * @see #setUseBatching
     */
    public List displayedObjects ()
    {
        if (_displayedObjects == null) {
            List filteredObjects = filteredObjects();

            // grab section based on batch
            _validateBatch();

            int batchStart = batchStartIndex();
            int batchEnd = batchEndIndex();

            // copy subarray
            _displayedObjects =
                ListUtil.arrayToList((Object[])subarray(filteredObjects.toArray(),
                                              batchStart,
                                              batchEnd));
            if (_grouper != null) _displayedObjects = _grouper.objectsInGroups(_displayedObjects);
        }
        return _displayedObjects;
    }

    public OutlineState outlineState ()
    {
        if (_outlineState == null) {
            setOutlineState(new OutlineState());
        }
        return _outlineState;
    }

    public void setOutlineState (OutlineState state)
    {
        _outlineState = state;
        if (_outlineState != null) {
            _outlineState.setDisplayGroup(this);
        }
    }

    /**
     * This cleans up a lot of the internal state as a side effect.  <br />
     * This includes the scrollTopIndex, currentBatchIndex and batchStartIndex.
     * TODO: Move to "Batch Methods" section.
     */
    protected void _validateBatch ()
    {
        List filteredObjects = filteredObjects();
        // Will throw NPE if filteredObjects is null.
        int count = filteredObjects.size();

        // wrap-around, should we scroll to the bottom instead?
        if (_scrollTopIndex >= count) {
            setScrollTopIndex(0);
            return;
        }

        // make sure current indices are valid values
        if (_batchStartIndex >= count) {
            setCurrentBatchIndex(batchCount() - 1);
        }

        // Skipping sizing when using batching.
        // Since currentBatchIndex depends on batch start index,
        // and currentBatchIndex is used in some conditionals (see AWTBatchNavigatorBar.isFirstBatchDisplayed),
        // changing the batch start index will change these conditionals between phases.
        if (!_useBatching && (_batchStartIndex + _numberOfObjectsPerBatch > count)) {
            // size out batch to _numberOfObjectsPerBatch
            // if overflow at bottom
            // set to bottom
            _setBatchStartIndex(count - _numberOfObjectsPerBatch);
        }

        // check if none of the selection falls within the batch
        // skip check if there is an outline, since select object can be a child object
        // Todo:  we break scroll faulting by rejecting the scroll repositioning
        boolean ensureSelectionInBatch = false;
        if (ensureSelectionInBatch) {
            if (!_areAnyInRange(filteredObjects, selectedObjects(),
                                batchStartIndex(), batchEndIndex()) &&
               _outlineState == null) {
                // reset selection to include first object in selection
                int batchNum = (ListUtil.indexOfIdentical(filteredObjects, selectedObject()) /
                            _numberOfObjectsPerBatch + 1);
                setCurrentBatchIndex(batchNum);
            }
        }
    }

    protected boolean _areAnyInRange (List all, List targets, int start, int end)
    {
        // we won't adjust the selection if there's nothing to adjust to
        if (targets.isEmpty()) {
            return true;
        }

        int i = targets.size();
        while (i-- > 0) {
            if (ListUtil.indexOfIdentical(all, targets.get(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    public void updateDisplayedObjects ()
    {
        _filteredObjects = null;
        _displayedObjects = null;

        // Don't validate -- this can end up stripping selected objects when in outline mode
        // _validateSelection();
    }

    /**
     * TODO: move to "Selection Methods" area
     */
    protected void _validateSelection ()
    {
        // remove anything that's not part of the filteredObjects
        List filteredObjects = filteredObjects();
        if (_selectedObjects == null) _selectedObjects = ListUtil.list();

        int i = _selectedObjects.size();
        while (i-- > 0) {
            if (indexOf(filteredObjects, _selectedObjects.get(i)) < 0) {
                _selectedObjects.remove(i);
            }
        }
        if (_forceSingleSelection && (_selectedObjects.isEmpty())) {
            _selectFirst();
        }

        if (_selectedObject != null && indexOf(_selectedObjects, _selectedObject) < 0) {
            _selectedObject = ListUtil.firstElement(_selectedObjects);
        }
    }

    /****************************************************************************
     * Item / Current Item Support
     ****************************************************************************/

    public void setCurrentItem (Object item)
    {
        _currentItem = item;
        if (_itemListeners != null) {
            for (AWTDataTable.Column listener : _itemListeners.keySet()) {
                listener.setCurrentItem(item, _owningTable);
            }
        }

        return;
    }

    public Object currentItem ()
    {
        return _currentItem;
    }

    public void _setItemToForceVisible (Object rootItem, Object leafItem)
    {
        _itemToForceVisible = leafItem;
        int index = ListUtil.indexOfIdentical(filteredObjects(), rootItem);
        // Safe guard against item that is not in the filteredObjects.
        // Otherwise, this will cause a scrollTop reset.
        if (index < 0) return;
        int topIndex = 0;
        if (_useBatching) {
            // find the closest batch top index
            int batchIndex = (int)index / _numberOfObjectsPerBatch;
            topIndex = batchIndex *  _numberOfObjectsPerBatch;
        }
        else {
            // try to place 40% down list, but make sure still in bounds
             // if we get -1, the code below will fix it to 0
            topIndex = (int)(index - (_numberOfObjectsPerBatch * 0.4));
            if (topIndex + _numberOfObjectsPerBatch > filteredObjects().size()) {
                topIndex = filteredObjects().size() - _numberOfObjectsPerBatch;
            }
            if (topIndex < 0) {
                topIndex = 0;
            }
        }
        setBatchStartIndex(topIndex);
        _isResetScrollTop = true;
    }

    public void setItemToForceVisible (Object item)
    {
        _setItemToForceVisible(item, item);
    }

    public void setPathToForceVisible (List path)
    {
        if (!path.isEmpty()) {
            outlineState().setExpansionPath(path);
            _setItemToForceVisible(ListUtil.firstElement(path),
                ListUtil.lastElement(path));
        }
    }

    public boolean forceCurrentItemVisibleLatch ()
    {
        if (_currentItem == _itemToForceVisible) {
              // latch
            _itemToForceVisible = null;

            return true;
        }
        return false;
    }

    public Map extrasForItem (Object item)
    {
        if (_currentItemExtras == null) {
            _currentItemExtras = new EqHashtable();
        }

        Map extras = (Map)_currentItemExtras.get(item);
        if (extras == null) {
            extras = MapUtil.map();
            _currentItemExtras.put(item, extras);
        }
        return extras;

    }
    
    /** This is a useful bag for storing state associated with a row. */
    public Map currentItemExtras ()
    {
        return extrasForItem(_currentItem);
    }

    /**
     * Find the index of an element from a list.  This method will first
     * check reference-equality.
     * Not -- this used to also check object-equality (by invoking the equals() method)
     * but that is inconsistent with the use of reference-equality elsewhere, and creates
     * broken behavior when multiple equals() objects are in the same table.
     *
     * @param list
     * @param element
     *
     * @return the index of the element in the list.  If not found, return -1.
     */
    private static int indexOf (List list, Object element)
    {
        int result = -1;

        if (ListUtil.nullOrEmptyList(list)) {
            return result;
        }

        result = ListUtil.indexOfIdentical(list, element);
        return result;
    }

    /***************************************************************************
     * Selected / Selection Support
     ***************************************************************************/

    public boolean currentSelectedState ()
    {
        return (indexOf(_selectedObjects, _currentItem) != -1);
    }

    /*
        Buffering of selection edits to allow deferring selection change until invokeAction().
        See AWTDataTable and AWTMultiSelectColumnRenderer.
     */
    boolean _bufferSelectionEdits;
    boolean _selectionDidChange;
    List _bufferedSelection;

    public boolean bufferSelectionEdits ()
    {
        return _bufferSelectionEdits;
    }

    public void setBufferSelectionEdits (boolean yn)
    {
        _bufferSelectionEdits = yn;
    }

    protected List _editableSelection ()
    {
        if (_bufferSelectionEdits) {
            if (_bufferedSelection == null) _bufferedSelection = ListUtil.collectionToList(_selectedObjects);
            return _bufferedSelection;
        }
        return _selectedObjects;
    }

    public void flushSelectionEdits ()
    {
        if (_selectionDidChange) {
            if (_bufferedSelection != null) {
                _selectedObjects = _bufferedSelection;
                _bufferedSelection = null;
            }
            _selectedObject = _selectedObjects.isEmpty() ? null : _selectedObjects.get(0);
            _selectionDidChange = false;
        }
    }

    protected void _discardBufferedSelection ()
    {
        _bufferedSelection = null;
        _selectionDidChange = false;
    }

    public void setCurrentSelectedState (boolean state)
    {
        List selection = _editableSelection();
        boolean currentState = indexOf(selection, _currentItem) != -1;
        if (currentState != state) {
            if (state) {
                selection.add(_currentItem);
            }
            else {
                int index = indexOf(selection, _currentItem);
                if (index != -1) {
                    selection.remove(index);
                }
            }
            _selectionDidChange = true;
            if (!_bufferSelectionEdits) flushSelectionEdits();
        }
    }

    protected void _selectFirst ()
    {
        List filteredObjects = filteredObjects();
        if (!filteredObjects.isEmpty()) {
            setSelectedObject(ListUtil.firstElement(filteredObjects));
        }
    }

    /** the "source of truth" on the selection */
    public List selectedObjects ()
    {
        return _selectedObjects;
    }

    public void setSelectedObjects (List selection)
    {
        List origSelection = _selectedObjects;
        _selectedObjects = ListUtil.collectionToList(selection);

        //  Note: skipped to allow selection of objects outside the display (for Outline)
        // _validateSelection();

        // set _selectedObject
        _selectedObject = ListUtil.firstElement(selection);
        boolean selectionChanged =
            _selectedObject != null && !listsIdentical(origSelection, _selectedObjects);
        if (selectionChanged) {
            setItemToForceVisible(_selectedObject);
        }
    }

    /**
        the first of the selectedObjects that is part of the displayedObjects
    */
    public Object selectedObject ()
    {
        return _selectedObject;
    }

    /**
        Selection manipulation.  These will push the visible batch
        to include the first selected object.
    */
    public void setSelectedObject (Object object)
    {
        List v = ListUtil.list();
        if (object != null) {
            v.add(object);
        }
        setSelectedObjects(v);
    }

    public void selectNext ()
    {
        List filteredObjects = filteredObjects();
        int count = filteredObjects.size();

        if (count > 0) {
            if (_selectedObject == null) {
                _selectFirst();
            }
            else {
                int identicalI = indexOf(filteredObjects,_selectedObject);
                int pos = (identicalI + 1) % count;
                setSelectedObject(filteredObjects.get(pos));
            }
        }
    }

    public void selectPrevious ()
    {
        List filteredObjects = filteredObjects();
        int count = filteredObjects.size();

        if (count > 0) {
            if (_selectedObject == null) {
                _selectFirst();
            }
            else {
                int pos = indexOf(filteredObjects, _selectedObject) - 1;
                if (pos < 0) {
                    pos = count-1;
                }
                setSelectedObject(filteredObjects.get(pos));
            }
        }
    }

    public void clearSelection ()
    {
        setSelectedObject(null);
    }

    /*******************************************************************************
     * Batching Support
     *******************************************************************************/

    /**
     *  the useBatching() property determines whether displayedObjects() is a subarray of the whole array, or whether
     *  it's a the entire array (and the client is just rendering the subset of items from batchStartIndex() to batchEndIndex().
    */
    public void setUseBatching (boolean yn)
    {
        _useBatching = yn;
        // bigger default batch size when using faulting
        _setNumberOfObjectsPerBatch((yn) ? 20 : 50);
    }

    public boolean useBatching ()
    {
        return _useBatching;
    }

    private void _setBatchStartIndex (int index)
    {
        _batchStartIndex = (index > 0) ? index : 0;
        // invalidate
        _displayedObjects = null;
    }

    /**
     * This accepts any value.
     * @see #batchStartIndex
     * @param index This accepts any integer and edits it to a valid value.
     */
    public void setBatchStartIndex (int index)
    {
        _setBatchStartIndex(index);
          // need to set _scrollTopIndex to something...
        _scrollTopIndex = _batchStartIndex;
        _validateBatch();
    }

    /**
     * This is the index of the top row of the batch.
     * @return
     */
    public int batchStartIndex ()
    {
        return _batchStartIndex;
    }

    public int batchEndIndex ()
    {
        int end = _batchStartIndex + _numberOfObjectsPerBatch;
        int max = filteredObjects().size();
        if (end > max) {
            end = max;
        }
        return end;
    }

    public int numberOfDisplayedObjects ()
    {
        return batchEndIndex() - batchStartIndex();
    }

    public void _setNumberOfObjectsPerBatch (int size)
    {
        _numberOfObjectsPerBatch = size;

        if (!_useBatching) {
            // if scroll faulting, then offset batchindex from scrollindex
            // Scale number of buffer in proportion to batch size
            // We want to try to save 30 rows for the body, but have a minimum of
            // 3 rows buffer.  We'd like 2/3 of the buffer in the direction of scroll
            int remaining = size - 30;
            _topBufferRowCount = ((remaining/3 > 3) ? (remaining/3) : 3);
            _topBufferMoveUpRowCount = ((2*remaining/3 > 3) ? (2*remaining/3) : 3);
        }

            // invalidate
        _displayedObjects = null;
    }

    public void setNumberOfObjectsPerBatch (int size)
    {
        if (_useBatching) {
            _setNumberOfObjectsPerBatch(size);
        }
    }

    public void setScrollBatchSize (int size)
    {
        _setNumberOfObjectsPerBatch(size);
    }


    public int numberOfObjectsPerBatch ()
    {
        return _numberOfObjectsPerBatch;
    }

    public int batchCount ()
    {
        return ((filteredObjects().size() + _numberOfObjectsPerBatch - 1)
                    / _numberOfObjectsPerBatch);
    }

    public boolean hasMultipleBatches ()
    {
        return batchCount() > 1;
    }

    public int currentBatchIndex ()
    {
        return (_batchStartIndex / _numberOfObjectsPerBatch) + 1;
    }

    public void setCurrentBatchIndex (int num)
    {
            // 0-based array, not 1
        num--;
        int batchCount = batchCount();
        if ((num >= batchCount)) {
            num = 0;
        }
        if (num < 0) {
            num = (batchCount > 0) ? (batchCount-1) : 0;
        }
        _setBatchStartIndex(num * _numberOfObjectsPerBatch);
    }

    public void displayNextBatch ()
    {
        setCurrentBatchIndex(currentBatchIndex() + 1);
    }

    public void displayPreviousBatch ()
    {
        setCurrentBatchIndex(currentBatchIndex() - 1);
    }

    /**
        Accessors for AWTScrollTableWrapper for scroll faulting
     */
    public int scrollTopCount ()
    {
        return (_useBatching) ? 0 : _batchStartIndex;
    }

    public int scrollBottomCount ()
    {
        return (_useBatching) ? 0 : (filteredObjects().size() - batchEndIndex());
    }

    /**
     * Doesn't quite center the batch on the row, more like 33% above, 66% below.
     * Will adjust the input value to validity.
     * @see #setUseBatching
     * @see #scrollTopIndex
     * @param desiredScrollTopIndex The row index to move the batch to.
     */
    public void centerBatchOnRow (int desiredScrollTopIndex)
    {
        _setBatchStartIndex(desiredScrollTopIndex - _topBufferRowCount);
        _validateBatch();
    }

	/**
     * The scrollTopIndex is the index of the top row that is currently displayed.
     * @return The scrollTopIndex
     */
    public int scrollTopIndex ()
    {
        return _scrollTopIndex;
    }

    /**
     * See {@link #setScrollTopIndex(int, boolean)}().
     * @param index
     * @return
     */
    public int setScrollTopIndex (int index)
    {
        return setScrollTopIndex(index, true);
    }

    /**
     * This sets and validates the scroll top index.
     * @see #scrollTopIndex
     * @param index the new value, can be any int
     * @param forceUpdateBatchStartIndex force an update of the internal batching data.
     * @return the new (possibly modified) scroll top index.
     */
    public int setScrollTopIndex (int index, boolean forceUpdateBatchStartIndex)
    {
        // if the index is out of the range, then force an update
        boolean updateBatchStartIndex = forceUpdateBatchStartIndex ||
                (_scrollTopIndex < batchStartIndex() || _scrollTopIndex > batchEndIndex());

        // set batch to some area above the start index so upward scrolling
        // doesn't always cause a fetch
        int delta = 0;
        if (index > _scrollTopIndex) {
            delta = _topBufferRowCount;
        }
        else if (index < _scrollTopIndex) {
            // moving up so make upper buffer bigger
            delta = _topBufferMoveUpRowCount;
        }
        if (_scrollTopIndex != index) {
            _scrollTopIndex = index;
            if (updateBatchStartIndex) {            
                _setBatchStartIndex(index - delta);
                _validateBatch();
            }
        }
        return _scrollTopIndex;
    }

    // Pushed / pulled by AWTScrollTableWrapper (by way of AWTDataTable) to preserve positioning across
    // scroll fault fetches
    public int _scrollTopOffset;

    /**
     * TODO: move to ArrayUtil.
     * @param array
     * @param startIndex
     * @param stopIndex
     * @return
     */
    public static Object subarray (Object array, int startIndex, int stopIndex)
    {
        Object[] sourceArray = (Object[])array;
        Class componentType = sourceArray.getClass().getComponentType();
        int destinationArrayLength = stopIndex - startIndex;
        Object destinationArray = Array.newInstance(componentType,
                                                    destinationArrayLength);
        System.arraycopy(sourceArray,
                         startIndex,
                         destinationArray,
                         0,
                         destinationArrayLength);
        return destinationArray;
    }

    /*******************************************************************************
     * Sort Orderings Support
     *******************************************************************************/

    /**
     * Sorting -- these are set interactively by the user of the table
     *
     * @param sortOrderings of type List&lt;AWTSortOrdering&gt; 
     */
    @SuppressWarnings("unchecked")
    public void setSortOrderings (List /*AWTSortOrdering*/ sortOrderings)
    {
        _sortOrderings = (List<AWTSortOrdering>)sortOrderings;
        invalidateSortOrderings();
    }

    public List<AWTSortOrdering> sortOrderings ()
    {
        return _sortOrderings;
    }

    /**
     * These can be set as a baseline (unalterable by the user) pre-sort.
     *
     * @param sortOrderings of type List&ltAWTSortOrdering&gt;
     */
    @SuppressWarnings("unchecked")
    public void setPrimarySortOrderings (List /*AWTSortOrdering*/ sortOrderings)
    {
        _primarySortOrderings = (List<AWTSortOrdering>)sortOrderings;
        invalidateSortOrderings();
    }

    public List<AWTSortOrdering> primarySortOrderings ()
    {
        return _primarySortOrderings;
    }

    @SuppressWarnings("unchecked")
    protected void invalidateSortOrderings ()
    {
        _effectiveSortOrderings = null;
        if (_grouper != null) {
            _primarySortOrderings =
                (List<AWTSortOrdering>)_grouper.computeSortOrderings();
        }
        if (_outlineState != null) {
            _outlineState.invalidateSortState();
        }

        if (_dataSource != null && _dataSource.dataSourceDoesSort()) {
            _dataSource.setSortOrderings(effectiveSortOrderings());
            fetch();
        } else {
            updateDisplayedObjects();
        }
    }

    /****************************************************************************
     * Misc
     ****************************************************************************/

    /** FIXME: stub implementations... */
    public AWTDataSource dataSource ()
    {
        return _dataSource;
    }

    public void setDataSource (AWTDataSource dataSource)
    {
        _dataSource = dataSource;
        _didInitialFetch = false;
    }

    public void fetch ()
    {
        // We're lazy about performing the actual fetch -- wait until checkDataSource()...
        _didInitialFetch = false;
        updateDisplayedObjects();
    }

    public Object insert ()
    {
        return insert(true);
    }

    public Object insert (boolean autoAdd)
    {
        if (_dataSource == null) return null;  // Assert?
        Object obj = _dataSource.insert();
        // sync display group with data source
        checkDataSource();

        // some data sources don't actually insert obj, but only create
        if (autoAdd) {
            List allObjects = allObjects();
            if (!allObjects.contains(obj)) {
                List newAllObjs = new ArrayList(allObjects());
                newAllObjs.add(obj);
                setObjectArray(newAllObjs);
            }
            setSelectedObject(obj);
        }
        return obj;
    }

    /**
        use this type to mark grouping rows, tell if they're expanded, ...
    */
    public static class GroupingState
    {
        public boolean isExpanded;
        public int count;
        public Object groupingRow;
    }

    /*
    ** Pivot Support
    */
    public interface Grouper
    {
        public List groupObjects (List sortedObjects);
        public List computeSortOrderings();
        public void validateFilteredList(List filteredList);
        public void invalidate();
        public List objectsInGroups (List groupLeadItems);
    }

    Grouper _grouper = null;

    void setGrouper (Grouper grouper)
    {
        _grouper = grouper;
        invalidateSortOrderings();
        resetGroupingState();
        updateDisplayedObjects();
    }
}
