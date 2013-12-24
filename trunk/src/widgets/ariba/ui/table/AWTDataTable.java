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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTDataTable.java#204 $
*/

package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWChecksum;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWCycleable;
import ariba.ui.aribaweb.core.AWDragContainer;
import ariba.ui.aribaweb.core.AWDropContainer;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWRecordingManager;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWErrorHandler;
import ariba.ui.aribaweb.core.AWErrorInfo;
import ariba.ui.aribaweb.core.AWOutputRangeCheck;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWSemanticKeyProvider;
import ariba.ui.aribaweb.util.AWPagedVector;
import ariba.ui.outline.OutlineState;
import ariba.ui.widgets.Util;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.OrderedList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
    DataTable widget
    See the AWApi section of .awl file for usage information.
    Important Concepts:
      Scroll Faulting / Pagination
      Grouping
      Filtering
      Sorting
      Display Group
      Details
      Nested Tables

    @aribaapi ariba
*/
public final class AWTDataTable extends AWComponent
{
    /*
    Implementation Overview:
    - Half of the implementation ("controller") is in AWTDisplayGroup
    - Table manages a set of AWTDataTable.Column objects.  AWTColumn is the most important one.
    - each Column has a corresponding *Renderer.java/.awl component that handles rendering of that column type.
    - The children of the Table element are Column elements which are managed as a list (like Tabs).
    - during initialization the Table prods the columns and they register themselves appropriately
    */
    public static final String TotalRowsKey = "totalRows";
    private static final String ShowTotalItemsBinding = "showTotalItems";
    private static final String IndentDetailRowBinding = "indentDetailRow";

    private static final String HasColumnHeadingBinding = "hasColumnHeading";
    private static final String RowIndexBinding = "rowIndex";

    private static final String ReorderActionBinding = "reorderAction";
    private static final String DragTypeBinding  = "dragType";
    private static final String DropTypesBinding = "dropTypes";
    public AWEncodedString _tableId;

    private static final String ChildrenBinding = "children";
    // this binding is used to disable the ChildrenBinding.  Since the existence of
    // the children binding enables outline mode, we need to have another binding to
    // allow conditionalization of the children binding.
    private static final String DisableChildrenBinding = "disableChildrenBinding";

    // passed to the column renderer if the value rendered is in a group heading
    public static final String IsGroupHeadingBinding = "isGroupHeading";

    // table configuration keys
    private static final String HiddenColumnsConfigKey = "hiddenColumns";
    private static final String GroupByColumnConfigKey = "groupByColumn";
    private static final String SortOrderingsConfigKey = "sortOrderings";
    private static final String GroupingExpansionDefaultConfigKey = "groupingExpansion";
    private static final String OutlineExpansionDefaultConfigKey = "outlineExpansion";
    private static final String TableBodyExpansionDefaultConfigKey = "tableBodyExpansion";

    /**
     * Maximum number of characters being displayed in the menus for the column names
     * @aribaapi ariba
     */
    public static final int ColumnLabelMenuMaxSize = 25;

    // a type for our column subclasses
    abstract public static class Column extends AWContainerElement
    {
        static {
            AWSemanticKeyProvider.registerClassExtension(Column.class,
                                                        new SemanticKeyProvider_Column());
        }
        /** AWComponent to use to render this component */
        abstract public String rendererComponentName ();

        public void initializeColumn (AWTDataTable table) {}
        // called by renderer before the column is used
        public void prepare (AWTDataTable table) {}

        /**
         * Tell the column the current table item
         * @param item The new current item (row value)
         * @param table The table that is setting the item
         * @aribaapi private
         */
        public void setCurrentItem (Object item, AWTDataTable table) {return;}

        /**
         * Tell the column that the rows are being reset.
         * @param table The table that is setting the item
         * @aribaapi private
         */
        public void rowsReset (AWTDataTable table) {return;}

        public boolean isValueColumn () { return false; }
        public boolean isValueColumn (AWTDataTable sender) {
            return isValueColumn();
        }
        public boolean isSortableColumn (AWTDataTable sender) {
            return true;
        }

        public boolean isGroupableColumn (AWTDataTable sender) {
            // by default, if sortable, it is groupable
            return isSortableColumn(sender);
        }

        // Pivot grouping property -- consecutive columns sharing equals() values will be merged
        public Object uniquingValue (AWTDataTable sender, Object item) {
            return NeverEqualValue;
        }

        public boolean isOptional (AWTDataTable sender) { return false; }   // can we hide it?
        public boolean initiallyVisible () { return true; }   // should it be visible by default?
        public boolean initiallyVisible (AWTDataTable sender) {
            return initiallyVisible();
        }
        public boolean wantsSpan (AWTDataTable sender) {
            // does this column want to span any any blank columns to the right
            return false;
        }
        public boolean isBlank (AWTDataTable sender) {
            // can be used for span of columns to left
            return false;
        }
        public String keyPathString () { return null; }
        public String label (AWTDataTable table) { return label(); }
        protected String label () { return null; }

        // Pivot field location compatability:
        //      Field, RowField, ColumnField
        //      Attribute, ColumnAttribute, DetailAttribute
        //      Any, None (can't move), Current (Attr to Attr, Field to Field)
        public String pivotMoveType (AWTDataTable table) { return "Current"; }

        public boolean renderValueInLabelColumn (AWTDataTable table) { return false; }

        public AWTSortOrdering createSortOrdering (AWTDataTable table) {
            String keyPathString = keyPathString();
            return (keyPathString == null) ? null
                : AWTSortOrdering.sortOrderingWithKey(keyPathString, AWTSortOrdering.CompareCaseInsensitiveAscending);
        }


        public void prepareSortOrdering (AWTDataTable table, AWTSortOrdering ordering)
        {

        }

        public boolean matchesKey (String key)
        {
            String ourKey = keyPathString();
            return (ourKey == key) || ((ourKey != null) && ourKey.equals(key));
        }

        protected Object bindingValue (AWBinding binding, AWTDataTable table)
        {
            return (binding == null) ? null : table.valueForBinding(binding);
        }

        // Style to be laid down on column to assign minimum width for TD
        // Value in px.  0 (zero) means do not set width
        public int minWidthPx (AWTDataTable table)
        {
            return 0;
        }

        // The real column to use for rendering
        public Column prepareAndReplace (AWTDataTable table)
        {
            prepare(table);
            return this;
        }

        // used to compute column set signature
        public int hashCode() {
            String key = keyPathString();
            return ((key != null) ? key.hashCode() : 1) * 31 + this.getClass().hashCode();
        }

        // Overridden by columns (e.g. DynamicColumns) that want ability to be re-covered during
        // forceColumnUpdate
        public Object purgatoryKey () { return keyPathString(); }
    }

    public interface DetailColumn {
        boolean renderBeforeRow (AWTDataTable table);
    }

    public static class SemanticKeyProvider_Column extends AWSemanticKeyProvider
    {
        public String getKey (Object receiver, AWComponent component)
        {
            Column column = (Column)receiver;
            return column.keyPathString();
        }
    }

    public static abstract class ColumnRenderer extends AWComponent
    {
        // stateless
        public AWTDataTable _table;

        public void awake ()
        {
            _table = (AWTDataTable)valueForBinding(BindingNames.table);
        }

        public void sleep ()
        {
            _table = null;
        }

        public Object _currentItem ()
        {
            return _table.displayGroup().currentItem();
        }

        public Column column ()
        {
            return _table._currentColumn;
        }

        protected Object bindingValue (AWBinding binding)
        {
            Object bindingValue = null;
            if (binding != null) {
                AWComponent parent = this;
                while (!(parent instanceof AWTDataTable)) {
                    parent = parent.parent();
                }
                bindingValue = parent.valueForBinding(binding);
            }
            return bindingValue;
        }

        protected boolean booleanBindingValue (AWBinding binding, boolean defaultValue)
        {
            if (binding == null) return defaultValue;

            Object value = bindingValue(binding);
            return AWBinding.computeBooleanValue(value);
        }

        public Object columnWidth ()
        {
            // width information for the column
            return null;
        }

        /**
         * Width information for the column.  Returns null if the column
         * in the current data row should not contain a width specification.
         * Should only be used for columns in data rows (ie. not header row).
         */
        public Object dataRowColumnWidth ()
        {
            Object retVal = null;
            if (!booleanValueForBinding(HasColumnHeadingBinding) &&
                intValueForBinding(RowIndexBinding) == 0) {
                retVal = columnWidth();
            }
            return retVal;
        }

    }

    public static final Object NeverEqualValue = new NeverEqualClass();
    private static class NeverEqualClass {
        public boolean equals () { return false; }
    }

    public static String EnvKey = "dataTable";

    void _phasePrepare ()
    {
        if (_parentTable == null) {
            _parentTable = (AWTDataTable)env().peek(EnvKey);
        }

        env().push(EnvKey, this);
    }

    void _phaseComplete ()
    {
        env().pop(EnvKey);
    }

    public static AWTDataTable currentInstance (AWComponent component)
    {
        return (AWTDataTable)component.env().peek(EnvKey);
    }

    /*******************************************************************************
     * Instance Variables
     *******************************************************************************/

    /**
     * This is where we store our row data.
     * @see #initializeDisplayGroupObjects
     * @see #checkListBinding
     */
    protected AWTDisplayGroup _displayGroup;

    /**
     * This is stored to check for rapid turnaround changes.
     */
    private AWComponentReference _processedReference;
    private List<Column> _allColumns;
    private boolean _visibilityArray[];
    protected List<Column> _displayedColumns;
    private List<Column> _dataColumns;
    private List<Column> _optionalColumns;

    private Column _detailColumn;
    private Column _secondDetailColumn;
    private AWTButtonArea _selectionButtonArea;
    private AWTButtonArea _globalButtonArea;
    private AWTButtonArea _rightButtonArea;
    private Column _headingAreaColumn;
    private Column _wrapperColumn;

    private Column _groupByColumn;

    private boolean _createdDisplayGroup;
    private boolean _didHibernate = false;

    private String _globalValign;
    private AWBinding _isItemSelectableBinding;
    private AWBinding _groupByColumnBinding;
    private AWBinding _forceColumnUpdateBinding;
    private AWBinding _forceRenderRowsBinding;

    private boolean _showColumnHeader;
    private String  _style;
    private String _tableClass;
    private boolean _enableScrolling;
    /**
     * Used to buffer changes in scroll offset until renderResponse.
     */
    protected int _scrollTopIndex = -1;

    /**
     * Only used to retain horizontal scroll across requests.
     * Binding.
     */
    public int _scrollLeftPos = 0;


    /**
     * This is the rendering scratch state.
     */
    private boolean _rowToggleState;

    // 0 = none, 1 = current page, 2 = all rows
    private static final int EXPORT_NONE = 0;
    private static final int EXPORT_CURRENT = 1;
    private static final int EXPORT_ALL = 2;

    protected int _exportState = EXPORT_NONE;
    private boolean _showOptionsMenu;
    private boolean _showBatchNavigation;
    private boolean _expandAll;
    public boolean _useParentLayout;
    public boolean _isOutline;
    AWTDataTable _parentTable;
    protected AWBinding _detailRowClassBinding;
    protected AWBinding _rowClassBinding;
    protected AWBinding _tdClassBinding;
    public Object _elementIdForVisibleRow;

    public Column _currentColumn;
    public Column _originalCurrentColumn;
    protected int _dynamicColspan;
    public int _currentRowIndex;
    public int _currentColumnIndex;
    public boolean _isTopBar;
    protected boolean _skipTopLine;
    public boolean _useRefresh;
    public boolean _isMaximized;  // pushed through from ScrollTableWrapper
    protected boolean _showSelectionColumn;
    public boolean _tableBodyCollapsible;
    public boolean _tableBodyExpanded;

    protected boolean _isDragDropEnabled = false;

    // 0 = none, 1 = single, 2 = multi
    protected static final int SELECT_NONE = 0;
    protected static final int SELECT_SINGLE = 1;
    protected static final int SELECT_MULTI = 2;

    protected int _selectMode = SELECT_NONE;
    protected int _colsBeforeData;
    protected int _colsAfterFirstDataColumn;
    protected int _outlineColumnIndex;
    protected int _nextColumnIndentation;
    private List<AWTColumnManager> _columnManagers = null;
    // If true, the selection column is always rendered regardless of the previous value.
    private boolean _forceRenderSelectionColumn = false;
    private boolean _showSelectAll = true;
    protected AWBinding _scrollFaultActionBinding;
    private boolean _isScrollFaultAction = false;

    public AWTDataTable ()
    {
        // System.out.println ("*** New DataTable: " + this);
    }

    /*******************************************************************************
     * Bindings (some)
     *******************************************************************************/

    public int currentRowIndex ()
    {
        return _currentRowIndex;
    }

    public void setCurrentRowIndex (int index)
    {
        _currentRowIndex = index;
        _dynamicColspan = 0;
        _didRenderCurrentRow = false;
    }

    public int scrollTopIndex ()
    {
        return _scrollTopIndex;
    }

    public void setScrollTopIndex (int index)
    {
        _scrollTopIndex = index;
    }

    /////////////
    // Awake
    /////////////
    protected void awake ()
    {
        _isDragDropEnabled = hasBinding(ReorderActionBinding) ||
                   hasBinding(AWDragContainer.DragActionBinding)||
                   hasBinding(AWDropContainer.DropActionBinding);

        if (_isDragDropEnabled && hasBinding(AWBindingNames.isDragDropEnabled)) {
            _isDragDropEnabled =
                booleanValueForBinding(AWBindingNames.isDragDropEnabled);
        }
    }

    public boolean isStateless ()
    {
        return false;
    }

    private boolean shouldForceColumnUpdate ()
    {
        boolean forceTableRefresh = false;
        if (_forceColumnUpdateBinding != null) {
            forceTableRefresh = booleanValueForBinding(_forceColumnUpdateBinding);
            if (forceTableRefresh) {
                setValueForBinding(false,_forceColumnUpdateBinding);
            }
        }
        return forceTableRefresh;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        try {
            _phasePrepare();
            if ((_processedReference != componentReference()) ||
                parentTableLayoutChanged() ||
                hasDisplayGroupChanged()) {

                // make sure that columns are not being changed since we're trying to export
                // the visible columns during invoke
                Assert.that(!isExportMode(),
                    "Cannot change table structure during data export.");

                initForChangedReference();

                // if we're going to initForChangedReference, then we're going to update all
                // the columns and we need to ensure that our _forceColumnUpdateBinding
                // continues to operate as a latch by pushing false to the binding
                shouldForceColumnUpdate();

                // remember this template (and flag that we're done with initialization)
                _processedReference = componentReference();
            }
            else if (shouldForceColumnUpdate() && !isExportMode() && !_isScrollFaultAction) {
                invalidateColumns();
                columns();  // force refresh
            }

            if (_didHibernate) {
                initializeDisplayGroupObjects();
                _didHibernate = false;
            }

            checkListChanges();
            prepareForIteration();
            checkGroupByColumn();
            checkDetailExpandoEnabled();
            _rowToggleState = false;

            _showSelectionColumn = !hasBinding(BindingNames.showSelectionColumn)
                        || booleanValueForBinding(BindingNames.showSelectionColumn);

            // we delay buffered changes to the scroll offset until renderResponse
            checkScrollTop();

            _displayGroup._discardBufferedSelection();

            int cmLen = ListUtil.getListSize(_columnManagers);
            for (int ii=0; ii<cmLen; ii++) {
                _columnManagers.get(ii).preAppend(this);
            }

            // register error nav handler to provide default table nav support
            // We need to register with highest possible priority
            DataTableNavigationHandler handler = new DataTableNavigationHandler(this);
            errorManager().registerErrorHandler(handler, 1, true);

            // run the append on all our contained elements
            super.renderResponse(requestContext, component);

            for (int ii=0; ii<cmLen; ii++) {
                _columnManagers.get(ii).postAppend(this);
            }
            _forceRenderSelectionColumn = false;
        }
        finally {
            _phaseComplete();
        }
    }

    /*******************************************************************************
     * Scroll Faulting Support
     *******************************************************************************/

    public AWResponseGenerating  scrollFaultAction ()
    {
        _isScrollFaultAction = true;
        if (_scrollFaultActionBinding != null) {
            return (AWResponseGenerating)valueForBinding(BindingNames.scrollFaultAction);
        }
        return null;
    }

    void resetScrollTop ()
    {
        _scrollTopIndex = 0;
        displayGroup().setScrollTopIndex(_scrollTopIndex);
    }

    public void setOutlineAllObjects (List list)
    {
        _displayGroup.setOutlineAllObjects(list);
        checkScrollTop();
    }

    private void checkScrollTop ()
    {
        boolean updateScrollTop =
            displayGroup().isResetScrollTop() || (_scrollTopIndex < 0);
        if (updateScrollTop) {
            _scrollTopIndex = displayGroup().scrollTopIndex();
        }
        else if (_scrollTopIndex != displayGroup().scrollTopIndex()) {
            // update the batch only if it's a scroll fault.
            _scrollTopIndex = displayGroup().setScrollTopIndex(_scrollTopIndex, _isScrollFaultAction);
        }
        else if (_isScrollFaultAction) {
            displayGroup().centerBatchOnRow(_scrollTopIndex);
        }
        _isScrollFaultAction = false;
    }

    public boolean useScrollFaulting ()
    {
        return ((_displayGroup.scrollTopCount() > 0) ||
                (_displayGroup.scrollBottomCount() > 0) && !requestContext().isExportMode());
    }

    /*******************************************************************************
     * End Scroll Faulting Support
     *******************************************************************************/

    private void initForChangedReference ()
    {
        /*
        ** Initialize
        */

        // get bindings
        _showOptionsMenu = booleanValueForBinding(BindingNames.showOptionsMenu);

        _showBatchNavigation = (hasBinding(BindingNames.showBatchNavigation)) ?
            booleanValueForBinding(BindingNames.showBatchNavigation) : true;

        _isItemSelectableBinding = bindingForName(BindingNames.isItemSelectable);
        _groupByColumnBinding = bindingForName(BindingNames.groupByColumn);
        _pivotLayoutBinding = bindingForName(BindingNames.pivotLayout);
        _expandAll = booleanValueForBinding(BindingNames.expandAll);

        _useParentLayout = (hasBinding(BindingNames.useParentLayout)) ?
            booleanValueForBinding(BindingNames.useParentLayout) : false;
        _rowClassBinding = bindingForName(BindingNames.rowClass);
        _detailRowClassBinding = bindingForName(BindingNames.detailRowClass);
        _tdClassBinding = bindingForName(BindingNames.tdClass);

        _showColumnHeader = (hasBinding(BindingNames.showColumnHeader)) ?
            booleanValueForBinding(BindingNames.showColumnHeader) : true;
        _tableBodyCollapsible = booleanValueForBinding(BindingNames.tableBodyCollapsible);
        _tableBodyExpanded = _tableBodyCollapsible
            && hasBinding(BindingNames.initialTableBodyExpanded) ?
                booleanValueForBinding(BindingNames.initialTableBodyExpanded) : true;
        _style = (hasBinding(BindingNames.style)) ?
            (String)valueForBinding(BindingNames.style) : null;
        boolean scrollingAllowed = AWTScrollTableWrapper.scrollingAllowed(requestContext())
                && (_parentTable == null || !_parentTable._enableScrolling);
        _enableScrolling  = booleanValueForBinding(BindingNames.enableScrolling)
                      && scrollingAllowed;
        _tableClass = (hasBinding(BindingNames.classBinding)) ?
            (String)valueForBinding(BindingNames.classBinding)
                : ((_enableScrolling) ? "scrollTableWrapper" : "scrollTableWrapper noScroll");
        _useRefresh = hasBinding("useRefresh") ?
            booleanValueForBinding("useRefresh") : true;
        _isOutline = (!hasBinding(DisableChildrenBinding) ||
            !booleanValueForBinding(DisableChildrenBinding)) &&
            hasBinding(ChildrenBinding);

        Assert.that(!(_isOutline && hasBinding(ReorderActionBinding)),
                    "ReorderActionBinding not compatible with outline control.  "+
            "Use dragAction and dropAction to explicitly handle reordering.");

        // force refresh latch
        _forceColumnUpdateBinding = bindingForName("forceColumnUpdate");
        _forceRenderRowsBinding = bindingForName(BindingNames.forceRenderRows);

        if (hasBinding(BindingNames.showSelectAll)) {
            _showSelectAll = booleanValueForBinding(BindingNames.showSelectAll);
        }
        _scrollFaultActionBinding = bindingForName(BindingNames.scrollFaultAction);
        _isScrollFaultAction = false;
        _elementIdForVisibleRow = null;

        /*
        ** Invalidate previous configuration
        */
        invalidateColumns();

        // if we need to explicitly create a display group or if the display group
        // passed in has changed, then clear it so it can be recreated
        if (_createdDisplayGroup || hasDisplayGroupChanged ()) {
            _displayGroup = null;
        }

        // force column initialization -- this will cause columns to push configuration back to us
        columns();
    }

    protected boolean finishedInitialization ()
    {
        return _processedReference != null;

    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        try {
            _phasePrepare();

            // To render to excel the challenge is to render *just* this table (which may
            // be embedded in a page).  We do this by catching the action that requests
            // Excel rendering and, if the flag is set by the action, invoke renderResponse
            // *on ourselves* to render just our table.
            // WARNING:  ElementIds are screwed up by this, so stateful components in our content
            // will fail to rendezvous with their instances.
            _exportState = EXPORT_NONE;

            prepareForIteration();

            int cmLen = ListUtil.getListSize(_columnManagers);
            for (int ii=0; ii<cmLen; ii++) {
                _columnManagers.get(ii).preInvoke(this);
            }

            // Todo:  for export, snapshot the element before doing this and restore it before doing renderResponse
            AWResponseGenerating response =
                super.invokeAction(requestContext, component);

            boolean exportValidation =
                ariba.ui.aribaweb.util.Log.aribawebvalidation_exportMode.isDebugEnabled();

            // if export validation is on, do the export, but don't save the result
            // see AWRequestContext.recordFormInputId for the actual validation.
            if (isExportMode() || exportValidation) {
                AWResponse excelResponse = application().createResponse();
                excelResponse.setContentType(AWContentType.ApplicationVndMsexcel);
                Util.setHeadersForDownload(excelResponse, "data.xls");

                // temporarily hide non-data columns
                Object state = _prepareColumnsForExport();

                boolean origExpandAll = _expandAll;
                if (_exportState == EXPORT_ALL) {
                    // expand all if exporting all
                    _expandAll = true;
                }
                requestContext.setExportMode(true);
                requestContext.setResponse(excelResponse);
                renderResponse(requestContext, component);

                _restoreColumnsAfterExport(state);
                _expandAll = origExpandAll;

                // System.out.println("Returning response: \n" + excelResponse.contentString());

                if (!exportValidation) {
                    response = excelResponse;
                }
            }

            for (int ii=0; ii<cmLen; ii++) {
                _columnManagers.get(ii).postInvoke(this, response);
            }

            _exportState = EXPORT_NONE;
            return response;
        }
        finally {
            _phaseComplete();
        }
    }

    /*******************************************************************************
     * Excel Export Support
     *******************************************************************************/

    public AWComponent downloadToExcelAll ()
    {
        _exportState = EXPORT_ALL;
        return null;
    }

    public AWComponent downloadToExcelCurrent ()
    {
        _exportState = EXPORT_CURRENT;
        return null;
    }

    public boolean renderToExcel ()
    {
        return _exportState != EXPORT_NONE;
    }

    public boolean isExportMode ()
    {
        return _exportState != EXPORT_NONE;
    }

    /*******************************************************************************
     * End Excel Export Support
     *******************************************************************************/

    public boolean isDragDropEnabled ()
    {
        return _isDragDropEnabled && !renderToExcel() && !requestContext().isPrintMode();
    }


    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        try {
            _phasePrepare();
            prepareForIteration();

            int cmLen = ListUtil.getListSize(_columnManagers);
            for (int ii=0; ii<cmLen; ii++) {
                _columnManagers.get(ii).preTake(this);
            }

            super.applyValues(requestContext, component);

            for (int ii=0; ii<cmLen; ii++) {
                _columnManagers.get(ii).postTake(this);
            }
            // Clear the force visible row element id only if request is coming from the table's form.
            // This is when we have client's scroll info, and have no more need for it.
            // For requests not from the table's form, we need to maintain this element id
            // to avoid a refresh region diff in the Datatable.updateScrollTable script block,
            // and therefore wrongly trigger the scroll repositioning code.
            _elementIdForVisibleRow = null;
        }
        finally {
            _phaseComplete();
        }
    }

    /**
        We hibernate to keep from holding on to a large result set.
    */
    protected void hibernate ()
    {
        // should we hibernate even if we didn't create the displayGroup?
        if (_createdDisplayGroup) {
            _displayGroup.hibernate();
            _didHibernate = true;
        }
        // will restore value from list binding if we are accessed again.

        // don't do this if we don't have a list binding?
    }

    /**
     * Go through the child elements of the AWTDatatable, and initialize
     * the ones that are columns.<br>
     * Note that the initialization does NOT addOrFind the column to any columns
     * list.  That happens as a side effect of the call to
     * <code>initializeColumn()</code>
     * @aribaapi private
     */
    private void initializeElementArray ()
    {
        AWElement contentElement = componentReference().contentElement();
        if (contentElement != null) {
            if (contentElement instanceof AWTemplate) {
                AWTemplate elementsTemplate = (AWTemplate)contentElement;
                AWCycleable[] elementArray = elementsTemplate.elementArray();
                int elementCount = elementArray.length;
                for (int index = 0; index < elementCount; index++) {
                    AWElement currentElement = (AWElement)elementArray[index];
                    if ((currentElement != null) && (currentElement instanceof Column)) {
                        ((Column)currentElement).initializeColumn(this);
                    }
                }
            }
            else if (contentElement instanceof Column) {
                ((Column)contentElement).initializeColumn(this);
            }
        }
    }

    /**
     * Lazily inits the Columns.
     *
     * @return a reference to a List<AWTDataTable.Column>
     */
    public List /*Column*/ columns ()
    {
        if (_allColumns == null) {
            initializeColumns();
        }
        return _allColumns;
    }

    private void initializeColumns ()
    {
        _allColumns = ListUtil.list();

        // we need to have the displayGroup object available early,
        // but it isn't filled until later in the method.  Keep track of how
        // much work we had to do to set it up, so that we do the right stuff later
        boolean setupDisplayGroup = false;
        boolean createdDisplayGroup = false;

        // Force creation of the display group because we may need to push it to the parent
        // (don't expect it to lazily get created because it won't if showTable is false).
        if (_displayGroup == null) {
            _displayGroup = (AWTDisplayGroup)valueForBinding(BindingNames.displayGroup);
            setupDisplayGroup = true;
            if (_displayGroup == null) {
                createDisplayGroup();
                createdDisplayGroup = true;
            }
            _displayGroup.setOwningTable(this);

            // Maybe should probably always do this... (not just when submitOnSelectionChange())
            _displayGroup.setBufferSelectionEdits(submitOnSelectionChange());
        }

        // read the child elements & process columns
        // as a side effect this will populate various columns variables
        initializeElementArray();
        // set up the data source, possibly adding more columns
        initializeDataSource();

        checkDetailExpandoEnabled();

        // Apply any saved table configuration
        boolean didHaveConfig = processTableConfig();

        AWTPivotState.checkPivotLayout(this);

        if (_allColumns.size() == 0) {
            columns().add(BlankColumn);
        }

        // take care of the initialization of the display group, as needed
        if (setupDisplayGroup) {
            // we need to set the batch size even when the displayGroup already exists
            displayGroup().setUseBatching(!_enableScrolling);
            if (_displayGroup.useBatching()) {
                int batchSize = intValueForBinding(BindingNames.batchSize);
                if (batchSize > 0) _displayGroup.setNumberOfObjectsPerBatch(batchSize);
            }
            else {
                int batchSize = intValueForBinding(BindingNames.scrollBatchSize);
                if (batchSize > 0) _displayGroup.setScrollBatchSize(batchSize);
            }

            if (!didHaveConfig) {
                if (hasBinding(BindingNames.expandAll)) {
                    _displayGroup.setGroupingExpansionDefault(
                                (_expandAll) ? AWTDisplayGroup.GroupingDefaultAllOpen
                                             : AWTDisplayGroup.GroupingDefaultFitBatch);
                }

                initializeDisplayGroupSort();
                checkGroupByColumn();
            }
        }

        if (createdDisplayGroup) {
            initializeDisplayGroupObjects();
        }

        if (createdDisplayGroup) {
            setValueForBinding(_displayGroup, BindingNames.displayGroup);
        }

        clearColumnPurgatory();
    }

    private boolean[] visibilityArray ()
    {
        if (_visibilityArray == null) {
            List columns = columns();
            int count = columns.size();
            _visibilityArray = new boolean[count];
            for (int i=0; i<count; i++) {
                Column col = (Column)columns.get(i);
                col.prepare(this);
                // if a groupByColumn exists, make sure it's not visible
                if (col == _groupByColumn) {
                    _visibilityArray[i] = false;
                }
                else {
                    _visibilityArray[i] = col.initiallyVisible(this);
                }
            }
        }
        return _visibilityArray;
    }

    protected void addColumnsFromDataSource ()
    {
        if (hasValueColumn()) return;  // only addOrFind if there are no other data columns
        AWTEntity entity;
        AWTDataSource dataSource = displayGroup().dataSource();
        if ((dataSource != null) && ((entity = dataSource.entity()) != null)) {
            List keys = entity.propertyKeys();
            for (int i=0; i<keys.size(); i++) {
                String key = (String)keys.get(i);
                Column column = purgatoryMatch(key);
                if (column == null) {
                    column = new AWTColumn();
                    ((AWTColumn)column).init(key, entity.displayStringForKey(key),
                        entity.defaultFormatterNameForKey(key),
                            entity.defaultAlignmentForKey(key), null);
                }
                registerColumn(column);
            }
        }
    }

    protected int selectionMode ()
    {
        return booleanValueForBinding(BindingNames.multiSelect) ? SELECT_MULTI
                                : (booleanValueForBinding(BindingNames.singleSelect) ? SELECT_SINGLE : SELECT_NONE);
    }

    public AWTDataTable getTopLevelParentTable ()
    {
        if (!_useParentLayout || _parentTable == null) {
            return this;
        }
        else {
            return _parentTable.getTopLevelParentTable();
        }
    }

    // if useParentLayout is on, we can only show columns that are in our parent's visible set
    protected List _computeBasedOnParentCols ()
    {
        // run through the parent's visible cols, constructing a parallel list of our columns
        // (and inserting spacers when we don't have a match)

        List ourCols = columns();
        List ourNewCols = ListUtil.list();
        List parentCols = getTopLevelParentTable().displayedColumns();
        Iterator iter = parentCols.iterator();
        while (iter.hasNext()) {
            Column c = (Column)iter.next();
            c.prepare(_parentTable);
            Column matchingCol = findAndRemoveColumnWithLabel(ourCols, c.label(_parentTable));
            if (matchingCol == null) {
                matchingCol = BlankColumn;
            }
            ourNewCols.add(matchingCol);
        }
        return ourNewCols;
    }

    protected boolean parentTableLayoutChanged ()
    {
        return (_parentTable != null) && (displayedColumns().size() != getTopLevelParentTable().displayedColumns().size());
    }

    protected boolean hasDisplayGroupChanged ()
    {
        // if we didn't create the display group and there is a cached display group
        // and there is a display group binding, then verify that the value bound in
        // and the cached value are the same
        if (!_createdDisplayGroup
            && _displayGroup != null
            && hasBinding(BindingNames.displayGroup))
        {
            AWTDisplayGroup currGroup = (AWTDisplayGroup)valueForBinding(BindingNames.displayGroup);
            return !_displayGroup.equals(currGroup);
        }
        return false;
    }

    protected void invalidateColumnData ()
    {
        _displayedColumns = null;
        _dataColumns = null;
    }

    public void invalidateColumns ()
    {
        if (_allColumns != null) populateColumnPurgatory();
        _allColumns = null;
        _selectMode = SELECT_NONE;
        _optionalColumns = null;
        _detailColumn = null;
        _secondDetailColumn = null;
        _columnManagers = null;
        _visibilityArray = null;
        _visibleColumnSet = null;
        invalidateColumnData();
    }

    protected void removeColumnsOfClass (List columns, Class columnClass)
    {
        int i = columns.size();
        while (i-- > 0) {
            if (columns.get(i).getClass() == columnClass) {
                columns.remove(i);
            }
        }
    }

    /*******************************************************************************
     * Column Purgatory Support
     *******************************************************************************/

    /*
        Support for holding over columns during forceColumnUpdate() for recovery.
        E.g. to reuse meta-data allocated columns.
     */

    Map _columnPurgatory;
    void populateColumnPurgatory ()
    {
        List columns = columns();
        int i = columns.size();
        while (i-- > 0) {
            Column column = (Column)columns.get(i);
            Object key = column.purgatoryKey();
            if (key != null) {
                if (_columnPurgatory == null) _columnPurgatory = MapUtil.map();
                _columnPurgatory.put(key, column);
            }
        }
    }

    public Column purgatoryMatch (Object key)
    {
        return (_columnPurgatory != null) ? (Column)_columnPurgatory.get(key) : null;
    }

    void clearColumnPurgatory ()
    {
        _columnPurgatory = null;
    }

    /*******************************************************************************
     * End Column Purgatory Support
     *******************************************************************************/

    protected Column firstSortableValueColumn ()
    {
        List columns = displayedColumns();
        for (int i=0, count=columns.size(); i<count; i++) {
            Column c =(Column) columns.get(i);
            c.prepare(this);
            if (c.isValueColumn(this) && c.isSortableColumn(this)) return c;
        }
        return null;
    }

    protected boolean hasValueColumn ()
    {
        // Sad subtlety -- we'd like to implement this in terms of the method
        // above, but because of initialization order issues, we need to implement
        // this one in terms of _allObjects.
        List columns = columns();
        for (int i=0, count=columns.size(); i<count; i++) {
            Column c =(Column) columns.get(i);
            c.prepare(this);
            if (c.isValueColumn(this)) return true;
        }
        return false;
    }

    /** called by AWTCSVData tag, and the csvPath binding */
    public void setDataSource (AWTDataSource dataSource)
    {
        displayGroup().setDataSource(dataSource);
        if (dataSource != null) {
            addColumnsFromDataSource();
        }
    }

    public AWTDataSource dataSource ()
    {
        return displayGroup().dataSource();
    }

    public void registerColumn (Column c)
    {
        columns().add(c);
    }

    public void setCurrentColumn (Column c)
    {
        _originalCurrentColumn = c;
        _isBreakColumn = false;
        _currentColumn = (c != null) ? c.prepareAndReplace(this) : c;
    }

    public Column groupByColumn ()
    {
        return _groupByColumn;
    }

    public Column selectionButtonArea ()
    {
        setCurrentColumn(_selectionButtonArea);  // side effect -- make it current
        return _selectionButtonArea;
    }

    public void setSelectionButtonArea (AWTButtonArea c)
    {
        _selectionButtonArea = c;
    }

    public Column globalButtonArea ()
    {
        setCurrentColumn(_globalButtonArea);     // side effect -- make it current
        return _globalButtonArea;
    }

    public void setGlobalButtonArea (AWTButtonArea c)
    {
        _globalButtonArea = c;
    }

    public Column rightButtonArea ()
    {
        setCurrentColumn(_rightButtonArea);     // side effect -- make it current
        return _rightButtonArea;
    }

    public void setRightButtonArea (AWTButtonArea c)
    {
        _rightButtonArea = c;
    }

    public Column headingAreaColumn ()
    {
        return _headingAreaColumn;
    }

    public void setHeadingAreaColumn (Column c)
    {
        _headingAreaColumn = c;
    }

    /**
     * Tell the table about a column manager.<br>
     * This column manager will get callbacks in the various phase methods.
     *
     * @param mgr The new manager, may be null.
     * 
     * @aribaapi private
     */
    public void registerColumnManager (AWTColumnManager mgr)
    {
        if (_columnManagers == null) {
            _columnManagers = ListUtil.list();
        }
        if (!_columnManagers.contains(mgr)) {
            _columnManagers.add(mgr);
        }
    }

    public Column wrapperColumn ()
    {
        return _wrapperColumn;
    }

    public void setWrapperColumn (Column c)
    {
        _wrapperColumn = c;
    }

    public String wrapperColumnComponentName ()
    {
        if (_wrapperColumn == null) {
            return "AWTNullWrapper";
        }
        else {
            setCurrentColumn(_wrapperColumn);
            return _wrapperColumn.rendererComponentName();
        }
    }

    /*******************************************************************************
     * Detail Column Support
     *******************************************************************************/

    protected boolean _wasPrimaryRow;

    public boolean activateDetailColumn (boolean isTop)
    {
        if ((_detailColumn != null) && (((DetailColumn)_detailColumn).renderBeforeRow(this) == isTop)
                && displayGroup().currentDetailExpanded()) {
            setCurrentColumn(_detailColumn);
            _wasPrimaryRow = _renderingPrimaryRow;
            _renderingPrimaryRow = false;
            return true;
        }
        return false;
    }

    public void didRenderDetail ()
    {
        _renderingPrimaryRow = _wasPrimaryRow;
    }

    public boolean activateDetailColumnTop ()
    {
        return activateDetailColumn(true);
    }

    public boolean activateDetailColumnBottom ()
    {
        return activateDetailColumn(false);
    }

    public boolean activateSecondDetailColumn ()
    {
        setCurrentColumn(_secondDetailColumn);
        return _secondDetailColumn != null;
    }

    public void setDetailColumn (Column c)
    {
        if (_detailColumn == null) {
            _detailColumn = c;
        }
        else {
            _secondDetailColumn = c;
        }
    }

    /*******************************************************************************
     * End Detail Column Support
     *******************************************************************************/

    protected void checkListChanges()
    {
        if (_displayGroup.dataSource() != null) {
            _displayGroup.checkDataSource();
        }
        else {
            AWBinding listBinding = bindingForName(AWBindingNames.list);
            if (listBinding != null) {
                Object list = valueForBinding(listBinding);
                _displayGroup.checkObjectArray (list);
            }
        }
    }

    protected void prepareForIteration()
    {
            // if we're a nested table, then we push to our displayGroup binding
            // (if we have one) so the component has access to the right one for this
            // iteration
        if (_parentTable != null && _useParentLayout) {
            setValueForBinding(_displayGroup, BindingNames.displayGroup);
        }
        if (_pivotState != null) _pivotState.prepareForIteration();
    }

    public Object currentItem ()
    {
        return _displayGroup.currentItem();
    }

    /** Each time we move to render the next column, we check if this column wants additional column span (if available)
     * and, if so, how many subsequent columns can provide it (i.e. are blank).  We then supress rendering of these
     * subsequent columns as we count down.
     */
    protected static Column BlankColumn = AWTColumn.createBlankColumn();
    protected static Column MultiSelectColumn = new AWTMultiSelectColumn();
    protected static Column SingleSelectColumn = new AWTSingleSelectColumn();
    protected static Column RowDetailExpansionColumn = new AWTRowDetailExpandoColumn.Column();

    public void setCurrentColumnIndex (int index)
    {
        // clear state for new column
        _nextColumnIndentation = 0;

        List columns = displayedColumns();
        _currentColumnIndex = index;
        Column column = (Column)columns.get(index);
        if (_pivotState != null) {
            column = _pivotState.substituteForCurrentColumn(column);
        }

        // In case this is a proxy...
        setCurrentColumn(column);

        if (_dynamicColspan != 0) {
            // after first call to "dynamicColspan" this is a negative number and we count up until we show columns again
            _dynamicColspan++;
        }
        if (_dynamicColspan == 0 && _currentColumn.wantsSpan(this)) {
            // count how many columns we can skip
            for (int i=index+1, count = columns.size(); i<count; i++) {
                Column c = (Column)columns.get(i);
                // Todo: setCurrentColumn...
                c.prepare(this);
                if (c.isBlank(this)) {
                    _dynamicColspan++;
                }
                else {
                    break;
                }
            }
        }
    }

    public int currentColumnIndex ()
    {
        return _currentColumnIndex;
    }

    public boolean skippingColumn ()
    {
        return _dynamicColspan < 0;
    }

    public Integer dynamicColspan ()
    {
        if (_dynamicColspan == 0) return null;

        Integer result = Constants.getInteger(_dynamicColspan + 1);
        _dynamicColspan = 0 - _dynamicColspan - 1; // flip it so we know that we've done the first column
        return result;
    }

    // Called by outline control to tell us that the current column is the outline control
    public void noteOutlineColumnIndex ()
    {
        _outlineColumnIndex = _currentColumnIndex;
    }

    public void indentNextColumn()
    {
        int indent = 40;
        if (_isOutline && _currentColumnIndex == _outlineColumnIndex) {
            indent += outlineState().lastIndentationPx();
        }
        _nextColumnIndentation = indent;
    }

    private AWEncodedString StylePadding = new AWEncodedString(" style=\"padding-left:");
    private AWEncodedString StylePxPlusQuote = new AWEncodedString("px\" ");
    private AWEncodedString StylePxPlusSemiColon = new AWEncodedString("px;");
    private AWEncodedString StyleQuote = new AWEncodedString("\" ");

    public String emitColumnStyle (String moreStyle)
    {
        if (_nextColumnIndentation != 0) {
            AWResponse response = response();
            response.appendContent(StylePadding);
            response.appendContent(Integer.toString(_nextColumnIndentation));
            if (moreStyle != null) {
                response.appendContent(StylePxPlusSemiColon);
                response.appendContent(moreStyle);
                response.appendContent(StyleQuote);
            }
            else {
                response.appendContent(StylePxPlusQuote);
            }
            return null;
        }
        return moreStyle;
    }

    public void makeHeadingAreaColumnCurrent ()
    {
        setCurrentColumn(_headingAreaColumn);
    }

    public void makeGroupingRowCurrent ()
    {
        setCurrentColumn(_groupByColumn);
    }

    public void setCurrentItem (Object currentItem)
    {
        setValueForBinding(currentItem, AWBindingNames.item);
        _displayGroup.setCurrentItem(currentItem);
        if (_selectableObjects != null && _amAccumulating) {
            if (isItemSelectable() && showSelectionControl()) {
                _selectableObjects.add(currentItem);
            }
        }
    }

    public Object currentItemChildren ()
    {
        return (_pivotState == null) ? valueForBinding(ariba.ui.outline.BindingNames.children)
                : _pivotState.currentItemChildren();
    }

    public boolean currentItemHasChildren ()
    {
        if (hasBinding(ariba.ui.outline.BindingNames.hasChildren)) {
            return (_pivotState == null) ? booleanValueForBinding(ariba.ui.outline.BindingNames.hasChildren)
                    : _pivotState.currentItemHasChildren();
        }
        Object children = currentItemChildren();
        return (children != null) && (OrderedList.get(children).size(children) > 0);
    }

    /*******************************************************************************
     * Selection Support
     *******************************************************************************/

    boolean _amAccumulating = false;

    List _selectableObjects;

    public void beginSelectableObjectAccumulation ()
    {
        if (hasBinding(BindingNames.showSelectionControl)
            || hasBinding(BindingNames.disableRowSelection))
        {
            _selectableObjects = ListUtil.list();
            _amAccumulating = true;
        }
    }

    public void endSelectableObjectAccumulation ()
    {
        _amAccumulating = false;
    }

    /*******************************************************************************
     * End Selection Support
     *******************************************************************************/

    /** This is evil -- and probably doesn't work...

    public void setCurrentRowIndex (int currentRowIndex)
    {
            // set value for current row index
        setValueForBinding(currentRowIndex, CurrentRowIndexBinding);
        _currentRowIndex = currentRowIndex;

            // calculate the current absolute row index => the current row index offset by
            // the current batch index * current batch size
        int numPerBatch = displayGroup().numberOfObjectsPerBatch();
        int batchIndex = displayGroup().currentBatchIndex();
        setValueForBinding((numPerBatch * (batchIndex-1)) + currentRowIndex, CurrentAbsoluteRowIndexBinding);
    }
*/
    OutlineState outlineState ()
    {
        return displayGroup().outlineState();
    }

    public AWTDisplayGroup displayGroup ()
    {
        return _displayGroup;
    }

    public int batchStartIndex ()
    {
        return ((_exportState != EXPORT_ALL) && !requestContext().isPrintMode()) ? displayGroup().batchStartIndex() : 0;
    }

    public int batchEndIndex ()
    {
        // Note: the check on parentTable is meant to check for a sub / embedded table, but
        // will also be tripped if a Confirmation panel is embedded in a table
        // In the latter case we try to distinguish via enableScrolling (which is not quite right)

        // _parentTable can be equal to this in the export case.
        if (_exportState == EXPORT_ALL || requestContext().isPrintMode()
                || (_parentTable != null && _parentTable != this && !enableScrolling())) {
            // render all objects
            return displayGroup().filteredObjects().size();
        }
        else {
            // just the right ones!
            return displayGroup().batchEndIndex();
        }
    }

    public List displayedObjects ()
    {
        // if we're doing an Export-All in Excel, then return filteredObjects rather than displayed
        return (_exportState != EXPORT_ALL) ? displayGroup().displayedObjects() : displayGroup().filteredObjects();
    }

    public boolean isEmpty ()
    {
        return displayGroup().numberOfDisplayedObjects() == 0;
    }

    public boolean showNavigationBar ()
    {
        AWTDisplayGroup displayGroup = displayGroup();
        boolean showTotalItems = booleanValueForBinding(ShowTotalItemsBinding);
        return _showBatchNavigation && _displayGroup.useBatching() &&
               (displayGroup.hasMultipleBatches() || showTotalItems)
               && (_isTopBar || (!enableScrolling() && (displayGroup().numberOfDisplayedObjects() > 15)));
    }

    public boolean showSelectionButtonArea ()
    {
        return ((_selectionButtonArea != null) && _selectionButtonArea.isVisible(this))
                && (displayGroup().numberOfDisplayedObjects() > 0);  // hide if empty
    }

    public boolean showGlobalButtonArea ()
    {
        return ((_globalButtonArea != null) && _globalButtonArea.isVisible(this));
    }

    public boolean showRightButtonArea ()
    {
        return ((_rightButtonArea != null) && _rightButtonArea.isVisible(this));
    }

    public boolean hasButtons ()
    {
        return showSelectionButtonArea() || showGlobalButtonArea() || showRightButtonArea();
    }

    public boolean showButtonColumns ()
    {
        return hasButtons() && (!_isTopBar || (!(enableScrolling() || useNewLook())
                                                && (displayGroup().numberOfDisplayedObjects() > 15)));
    }

    public boolean showHeadingArea ()
    {
        return (_headingAreaColumn != null) && (_exportState == EXPORT_NONE);
    }

    public boolean showOptionsMenu ()
    {
        return _showOptionsMenu && _isTopBar;
    }

    public boolean hasOptionsMenuTemplate ()
    {
        return hasSubTemplateNamed("optionsMenu");
    }

    public boolean showTitle ()
    {
        return valueForBinding(BindingNames.title) != null;
    }

    public boolean showNavigationRowTop ()
    {
        _isTopBar = true;
        return (showTitle() || showHeadingArea() || showOptionsMenu() || showNavigationBar() || showButtonColumns()) && (_exportState == EXPORT_NONE);
    }

    public boolean showNavigationRowBottom ()
    {
        _isTopBar = false;
        return (showOptionsMenu() || showNavigationBar() || showButtonColumns()) && (_exportState == EXPORT_NONE);
    }

    public boolean showColumnLabelWhenGrouping ()
    {
        AWBinding binding = bindingForName(BindingNames.showColumnLabelWhenGrouping);
        return (binding == null) ? true : booleanValueForBinding(binding);
    }

    public boolean showBatchNavRow ()
    {
        return showNavigationBar() || showOptionsMenu();
    }

    public List displayedColumns ()
    {
        if (_displayedColumns == null) {
            List columns = computeVisibleColumns();
            if (_pivotState != null) columns = _pivotState.computeDisplayedColumns(columns);

            // figure out spans, etc
            initColInfo(columns);

            checkColumnSignature(columns);
            _displayedColumns = columns;
        }
        return _displayedColumns;
    }

    protected boolean hasLeadingSelectColumn ()
    {
        _selectMode = selectionMode();
        return _selectMode != SELECT_NONE && _pivotState == null;
    }

    protected List computeVisibleColumns ()
    {
        if (_useParentLayout && _parentTable != null) return _computeBasedOnParentCols();

        List result = ListUtil.list();
        if (hasLeadingSelectColumn()) {
            Column newColumn = (_selectMode == SELECT_MULTI)
                        ? MultiSelectColumn
                        : SingleSelectColumn;
            result.add(newColumn);
        }

        if (_usingDetailRowExpando()) result.add(RowDetailExpansionColumn);

        boolean[] visibility = visibilityArray();
        List columns = columns();
        for (int i=0, count=columns.size(); i < count; i++) {
            if (visibility[i]) {
                Column c = (Column)columns.get(i);
                c.prepare(this);
                result.add(c);
            }
        }
        return result;
    }

    Set _visibleColumnSet;

    Set visibleColumnSet ()
    {
        if (_visibleColumnSet == null) {
            _visibleColumnSet = new HashSet(computeVisibleColumns());

        }
        return _visibleColumnSet;
    }

    // Used to force flushing of stateful components if column structure changes
    long _displayedColumnsCRC = 0;
    public boolean _headerColumnResetLatch = false;
    public boolean _bodyColumnResetLatch = false;

    protected void checkColumnSignature (List columns)
    {
        long crc = AWChecksum.crc32HashList((long)1, columns());
        if (crc != _displayedColumnsCRC) {
            _headerColumnResetLatch = _bodyColumnResetLatch = true;
            _displayedColumnsCRC = crc;
        }
    }

    // include the group by column (which is not "visible)
    protected List computeAllDisplayedColumns()
    {
        List displayColumns = displayedColumns();
        if (_groupByColumn != null) {
            displayColumns = ListUtil.collectionToList(displayColumns);
            displayColumns.add(0, _groupByColumn);
        }
        return displayColumns;
    }

    public List dataColumns ()
    {
        if (_dataColumns == null) {
            _dataColumns = ListUtil.list();
            List displayColumns = displayedColumns(); // computeAllDisplayedColumns();

            for (int i=0, count=displayColumns.size(); i < count; i++) {
                Column c = (Column)displayColumns.get(i);
                c.prepare(this);
                if (c.isValueColumn(this)) {
                    _dataColumns.add(c);
                }
            }
        }
        return _dataColumns;
    }

    public List optionalColumns ()
    {
        if (_optionalColumns == null) {
            _optionalColumns = ListUtil.list();
            List columns = columns();
            for (int i=0, count=columns.size(); i < count; i++) {
                Column c = (Column)columns.get(i);
                c.prepare(this);
                if (c.isOptional(this)) {
                    _optionalColumns.add(c);
                }
            }
        }
        return _optionalColumns;
    }

    Object _prepareColumnsForExport ()
    {
        if (_pivotState != null) {
            return _pivotState._prepareColumnsForExport();
        }
        else {
            Object state = _displayedColumns;
            _displayedColumns = dataColumns();
            return state;
        }
    }

    void _restoreColumnsAfterExport (Object state)
    {
        if (_pivotState != null) {
            _pivotState._restoreColumnsAfterExport(state);
        }
        else {
            _displayedColumns = (List)state;
        }
    }

    /*******************************************************************************
     * Visibility Support
     *******************************************************************************/

    public boolean isCurrentColumnDisplayed ()
    {
        int index = ListUtil.indexOfIdentical(columns(), _originalCurrentColumn);
        return visibilityArray()[index];
    }

    public AWComponent toggleCurrentColumnVisibility ()
    {
        setColumnVisibility(_originalCurrentColumn, !isCurrentColumnDisplayed());
        _dataColumns = null;
        pushTableConfig();
        return null;
    }

    public void setColumnVisibilities (List columnsToMakeVisible)
    {
        List currentDisplayedColumns = displayedColumns();
        List optionalColumns = optionalColumns();
        boolean columnsChanged = false;
        int optionalColumnsSize = optionalColumns.size();
        for (int i = 0; i < optionalColumnsSize; i++) {
            AWTDataTable.Column optionalColumn =
                (AWTDataTable.Column)optionalColumns.get(i);
            boolean isCurrentDisplayed =
                ListUtil.containsIdentical(currentDisplayedColumns, optionalColumn);
            boolean isMakeVisible =
                ListUtil.containsIdentical(columnsToMakeVisible, optionalColumn);
            if (isMakeVisible != isCurrentDisplayed) {
                setColumnVisibility(optionalColumn, isMakeVisible);
                columnsChanged = true;
            }
        }
        if (columnsChanged) {
            _dataColumns = null;
            pushTableConfig();
        }
    }

    public void setColumnVisibility (Column c, boolean makeVisible) {
        int i = ListUtil.indexOfIdentical(columns(), c);
        boolean[] visibility = visibilityArray();
        if (i >=0) {
            visibility[i] = makeVisible;
            // invalidate
            _displayedColumns = null;
            _visibleColumnSet = null;

            // note: don't update _dataColumns here -- should not remove _dataColumns
            //_dataColumns = null;
        }
    }

    /*******************************************************************************
     * End Visibility Support
     *******************************************************************************/

    protected void initColInfo (List columns)
    {
        int i, count=columns.size();

        for(i=0; i<count; i++) {
            Column column = (Column)columns.get(i);
            column.prepare(this);
            if (column.isValueColumn(this)) {
                break;
            }
        }

        _colsBeforeData = i;  //  + 1 one for the spacer
        if (booleanValueForBinding(IndentDetailRowBinding)) {
            _colsBeforeData++;
        }

        _colsAfterFirstDataColumn = columns.size() - _colsBeforeData; // (omit +1 for the spacer)
    }

    public int colsBeforeData ()
    {
        return _colsBeforeData;
    }

    public int colsAfterFirstDataColumn ()
    {
        return _colsAfterFirstDataColumn;
    }

    public int colspanMinusOne ()
    {
        return columns().size(); // omit spacer column (this is the "minus one")
    }

    public String globalVAlignment ()
    {
        if (_globalValign == null){
            _globalValign = (String)valueForBinding(BindingNames.valign);
        }
        return _globalValign;
    }

    protected Object bindingValue (AWBinding binding)
    {
        return (binding == null) ? null : binding.value(parent());
    }

    public int tableColumnCount ()
    {
        return displayedColumns().size();
    }

    public boolean isSingleOrMultiSelect ()
    {
        return (booleanValueForBinding(BindingNames.multiSelect) ||
               (booleanValueForBinding(BindingNames.singleSelect)));
    }

    public boolean multiSelectOrSubmitForm ()
    {
            // used to determine whether the batch control should submit
        return (booleanValueForBinding(BindingNames.multiSelect) ||
               (hasBinding(AWBindingNames.submitForm)
                    ? booleanValueForBinding(AWBindingNames.submitForm) : (requestContext().currentForm()!=null)));
    }

    /*******************************************************************************
     * Begin Selection Support
     *******************************************************************************/

    public boolean submitOnSelectionChangeBindingExists ()
    {
        return hasBinding(BindingNames.submitOnSelectionChange);
    }

    public boolean submitOnSelectionChange ()
    {
        return booleanValueForBinding(BindingNames.submitOnSelectionChange);
    }

    public boolean showSelectAll ()
    {
        // if there is a groupByColumn, then disable selectall
        return !isGroupingByEnabled() && showSelectionColumn() && _showSelectAll && (_pivotState == null);
    }

    public boolean showSelectionColumn ()
    {
        return _showSelectionColumn;
    }

    public boolean hasInvisibleSelectionColumn ()
    {
        return hasLeadingSelectColumn() && !showSelectionColumn();
    }

    public boolean disableRowSelection ()
    {
        if (hasBinding(BindingNames.disableRowSelection)) {
            return booleanValueForBinding(BindingNames.disableRowSelection);
        }
        return false;
    }

    public boolean showSelectionControl ()
    {
        return !hasBinding(BindingNames.showSelectionControl)
                    || booleanValueForBinding(BindingNames.showSelectionControl);
    }

    /** Toggle Select All / Select None */
    public AWComponent toggleColumn ()
    {
        List selectableObjects = selectableObjects();

        // toggle selectAll / selectNone
        if (_displayGroup.selectedObjects().size() >= selectableObjects.size()) {
            // clear the column
            _displayGroup.clearSelection();
        }
        else {
            // check the whole column
            _displayGroup.setSelectedObjects(selectableObjects);
        }
        _forceRenderSelectionColumn = true;
        return null;
    }

    // value is set via toggleColumn so ignore the push
    public void setIsColumnSelected (boolean ignore)
    {
    }

    private List selectableObjects ()
    {
        return (_selectableObjects != null) ?
            _selectableObjects : _displayGroup.filteredObjects();
    }

    public boolean getIsColumnSelected ()
    {
        int numObjects = selectableObjects().size();
        if (numObjects > 0 && _displayGroup.selectedObjects().size() >= numObjects) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean disableSelectAll ()
    {
        return selectableObjects().size() == 0;
    }

    /**
        Gets call when all rows in the current batch are selected.
        Need to force render the select column.
    */
    public AWComponent updateSelectAllAction ()
    {
        _forceRenderSelectionColumn = true;
        return null;
    }

    /*******************************************************************************
     * End Selection Support
     *******************************************************************************/

    public boolean forceRenderRows ()
    {
        boolean forceRenderRows = false;
        if (_forceRenderRowsBinding != null) {
            forceRenderRows = booleanValueForBinding(_forceRenderRowsBinding);
        }
        return _forceRenderSelectionColumn || forceRenderRows;
    }

    public boolean isItemSelectable ()
    {
        return (_isItemSelectableBinding != null) ? booleanValueForBinding(_isItemSelectableBinding) : true;
    }

    public void setExpandAll (boolean yn)
    {
        _expandAll = yn;
    }

    public boolean expandAll ()
    {
        return _expandAll;
    }

    public boolean showColumnHeader ()
    {
        return _showColumnHeader;
    }

    public boolean enableScrolling ()
    {
        return _enableScrolling;
    }

    public String style ()
    {
        return _style;
    }

    public String tableClass ()
    {
        return _tableClass;
    }

    public String cornerImage ()
    {
        if (useNewLook()) {
            return "btnRowCornerBottomModern.gif";
        }
        else {
            return (_isTopBar) ? "btnRowCornerTop.gif" : "btnRowCornerBottom.gif";
        }
    }

    // Fun debugging options...
    public void toggleScrolling ()
    {
        _enableScrolling = !_enableScrolling;
        displayGroup().setUseBatching(!_enableScrolling);
    }

    public boolean useNewLook ()
    {
        return _tableClass.indexOf("scrollTableWrapper") >= 0;
    }

    public void toggleNewLook ()
    {
        _tableClass = ((useNewLook()) ? "classicTableWrapper" : "scrollTableWrapper");
    }

    public boolean disableRefresh ()
    {
        return !_useRefresh;
    }

    private int _dragIndex = -1;
    public void dragAction ()
    {
        if (hasBinding(AWDragContainer.DragActionBinding)) {
            valueForBinding(AWDragContainer.DragActionBinding);
        }

        // store dragIndex for possible ReorderAction
        _dragIndex = currentRowIndex();
    }

    public AWResponseGenerating dropAction ()
    {
        if (hasBinding(AWDropContainer.DropActionBinding)) {
            return (AWResponseGenerating)valueForBinding(AWDropContainer.DropActionBinding);
        }
        else if (hasBinding(ReorderActionBinding)) {
            reorderRows(_dragIndex, currentRowIndex());
            return (AWResponseGenerating)valueForBinding(ReorderActionBinding);
        }
        return null;
    }

    private void reorderRows (int dragIndex, int dropIndex)
    {
        List list = _displayGroup.allObjects();

        int batchAdjust = _displayGroup.numberOfObjectsPerBatch() * (_displayGroup.currentBatchIndex()-1);
        dragIndex += batchAdjust;
        dropIndex += batchAdjust - ((dragIndex < dropIndex) ? 1:0);

        list.add(dropIndex, list.remove(dragIndex));
        _displayGroup.setObjectArray(list);
    }

    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/

    private int sortDirectionForString (String direction)
    {
        if (direction == null || direction.equalsIgnoreCase("ascending")) {
            return AWTSortOrdering.CompareAscending;
        }
        if (direction.equalsIgnoreCase("descending")) {
            return AWTSortOrdering.CompareDescending;
        }
        if (direction.equalsIgnoreCase("caseinsensitiveascending")) {
            return AWTSortOrdering.CompareCaseInsensitiveAscending;
        }
        if (direction.equalsIgnoreCase("caseinsensitivedescending")) {
            return AWTSortOrdering.CompareCaseInsensitiveDescending;
        }
        Assert.that(false, "Unknown initial sort direction: %s", direction);
        return AWTSortOrdering.CompareAscending;
    }

    private void createDisplayGroup ()
    {
        _createdDisplayGroup = true;
        _displayGroup = new AWTDisplayGroup();
    }

    private void initializeDisplayGroupSort ()
    {
        if (_displayGroup.sortOrderings() == null) {
            // compute sort order
            String initialSortKey = (String)valueForBinding(BindingNames.initialSortKey);
            if (initialSortKey != null && initialSortKey.equals("")) {
                // explicit "no sort"
                _displayGroup.setSortOrderings(ListUtil.<AWTSortOrdering>list());
            } 
            else {
                String initialSortDirection = stringValueForBinding(BindingNames.initialSortDirection);
                Column initialSortColumn = null;
                AWTSortOrdering initialSortOrdering = null;
                    // by default, sort by first key
                initialSortColumn = (initialSortKey != null)
                                    ? findColumnForKey(initialSortKey)
                                    : firstSortableValueColumn();
                if (initialSortColumn != null) {
                    initialSortColumn.prepare(this);
                    initialSortOrdering = initialSortColumn.createSortOrdering(this);
                    if (initialSortOrdering != null && initialSortDirection != null) {
                        initialSortOrdering.setSelector(sortDirectionForString(initialSortDirection));
                    }
                }
                else if (initialSortKey != null) {
                    initialSortOrdering = new AWTSortOrdering(initialSortKey, sortDirectionForString(initialSortDirection));
                }

                if (initialSortOrdering != null) {
                    List<AWTSortOrdering> sortOrder = ListUtil.list();
                    sortOrder.add(initialSortOrdering);
                    _displayGroup.setSortOrderings(sortOrder);
                }
            }
        }
    }

    private void initializeDataSource ()
    {
        AWTDataSource dataSource = (AWTDataSource)valueForBinding("dataSource");
        if (dataSource != null) {
            setDataSource(dataSource);
        }
        else {
            String csvPath;
            if (((csvPath = stringValueForBinding(BindingNames.csvPath)) != null) && (_CSVSourceFactory != null)) {
                // note that "setDataSource" has a side effect of creating columns
                setDataSource(_CSVSourceFactory.dataSourceForPath(csvPath, parent()));
            }
        }
    }

    /**
     * @see AWTDataSource
     */
    private void initializeDisplayGroupObjects ()
    {
        checkListChanges();
    }

    private static final String DragStylePrefix = "awtDrg_";
    private static final String DropStylePrefix = " awtDrp_";
    private static final String StyleSeparator  = " ";

    // if there is a drag type use it, if there is a drop type use it
    // if either one does not exist, and there is a reorder action, then generate an
    // id for the type
    public String rowClass ()
    {
        String style = "";
        String dragType = null;

        // Drag Type
        if (hasBinding(DragTypeBinding)) {
            dragType = stringValueForBinding(DragTypeBinding);
        }

        if (dragType == null && hasBinding(ReorderActionBinding)) {
            // if reorder is bound, and there is no drag type, then scope drag/drop to the
            // current table by generate using the element id of the table for the type
            dragType = _tableId.string();
        }

        if (dragType != null) {
            style = StringUtil.strcat(DragStylePrefix, dragType);
        }

        // Drop Type
        String dropStyle = null;
        if (hasBinding(AWBindingNames.dropType) || hasBinding(DropTypesBinding)) {
            dropStyle = AWDropContainer.dropTypeToString(DropStylePrefix,
                                        stringValueForBinding(AWBindingNames.dropType),
                                        valueForBinding(DropTypesBinding));
        }

        if (dropStyle == null && hasBinding(ReorderActionBinding)) {
            // if reorder is bound, and there is no drop type, then scope drop to the
            // current table by using the element id of the table for the type
            dropStyle = StringUtil.strcat(DropStylePrefix, dragType);
        }

        if (dropStyle != null) {
            style = StringUtil.strcat(style != null ? style : "",dropStyle);
        }

        if (_displayGroup.currentSelectedState()) {
            style = StringUtil.strcat("tableRowSelected ", ((style != null) ? style : ""));
        }

        boolean firstRow = ((_currentRowIndex == 0) && !_useParentLayout) || _skipTopLine;
        if (firstRow) style = StringUtil.strcat(style, StyleSeparator, "firstRow");

        String customRowClass = null;
        if (!_renderingPrimaryRow && _detailRowClassBinding != null) {
            customRowClass = stringValueForBinding(_detailRowClassBinding);
        }
        if (customRowClass == null  && _rowClassBinding != null) {
            customRowClass = stringValueForBinding(_rowClassBinding);
        }

        if (customRowClass != null) {
            return StringUtil.strcat(style,StyleSeparator, customRowClass);
        }
        else {
            if (useNewLook()) {
                return StringUtil.strcat(style,StyleSeparator,"tableRow1");
            } 
            else {
                // old style -- alternate row colors
                return StringUtil.strcat(style,StyleSeparator,
                                         (_rowToggleState) ? "tableRow2" : "tableRow1");
            }
        }
    }

    public void skipTopLine ()
    {
        _skipTopLine = true;
    }

    public String tdClass ()
    {
        return tdClass(((_currentRowIndex == 0) && !_useParentLayout) || _skipTopLine);
    }


    // Called when TD is done rendering so we can check if we need to emit an nbsp for empty content
    public void setIteratorForTDContentCheck (AWPagedVector.AWPagedVectorIterator elements)
    {
        if (AWOutputRangeCheck.hasVisbibleContent(elements) || renderToExcel()) {
            return;
        }
        response().appendContent("&nbsp;");
    }

    boolean _isBreakColumn = false;

    public String tdClass (boolean isFirstRow)
    {
        if (_tdClassBinding != null) {
            return stringValueForBinding(_tdClassBinding);
        } 
        else {
            return (_isBreakColumn) ? "tableBody columnBreak": "tableBody";
        }
    }

    public String groupByTDClass ()
    {
        if (useNewLook()) {
            // eliminate topline for first row
            return (_currentRowIndex == 0) ? "firstRow tableGroupBy" : "tableGroupBy";
        }
        else {
            // old style
            return "tableGroupBy";
        }
    }

    public void didRenderRow ()
    {
        _rowToggleState = !_rowToggleState;
        _skipTopLine = false;
    }

    public boolean hasMinWidths ()
    {
        List cols = displayedColumns();
        int i = cols.size();
        while (i-- > 0) {
            if (((Column)cols.get(i)).minWidthPx(this) > 0) return true;
        }
        return false;
    }

    private static final AWEncodedString StylePaddingRight = AWEncodedString.sharedEncodedString(" style=\"padding-right:");
    private static final AWEncodedString PxQuote = AWEncodedString.sharedEncodedString("px\"");

    public String curColMinWidthStyle ()
    {
        // to avoid creating garbage, we append EncodedStrings ourselves, and then
        // return null so that another attribute is not rendered
        int width = _currentColumn.minWidthPx(this);
        if (width > 0) {
            AWResponse response = response();
            response.appendContent(StylePaddingRight);
            response.appendContent(Integer.toString(width));
            response.appendContent(PxQuote);
        }
        return null;
    }
    /**
     * Okay, this topic is a doozy...
     * When doing "scroll faulting" (bringing in contents of large scrollable tables "on demand") the client needs
     * to identify the row index of the row that was scrolled to.  In this calculation it should count only
     * real *data rows* from the *primary* (outermost, not nested) table, so that this index maps to the one
     * in the displayGroup (i.e. the one being batched).  We mark these data rows with the '  dr="1" '
     * attribute on the TR tag in the HTML.  For inner table rows, detail rows, group heading rows, etc, this
     * attribute is omitted, and thus these rows are excluded from the count on the client.
     */
    public String primaryDataRowIndicator ()
    {
        return ( _useParentLayout || !_renderingPrimaryRow ) ? null : "1";
    }

    public String groupingRowIndicator ()
    {
        return !displayGroup().currentGroupingExpanded() ||
            ((primaryDataRowIndicator() != null) && !_displayGroup.isCurrentItemVisible ())
            ? "1": null;
    }

    public void setCurrentItemElementId (Object elementId)
    {
        if (_displayGroup.forceCurrentItemVisibleLatch()) {
            _elementIdForVisibleRow = elementId;
        }
    }

    public Object currentItemElementId ()
    {
        Assert.that(false, "Should never pull this binding");
        return null;
    }

    /*
    ** Detail row show/hide support
    */
    protected void checkDetailExpandoEnabled ()
    {
        boolean enabled = (_pivotState == null && _detailColumn != null
                && booleanValueForBinding(BindingNames.useRowDetailExpansionControl));
        displayGroup().setDetailExpansionEnabled(enabled);

        if (enabled) {
            displayGroup().setDetailRowAutoCollapse(booleanValueForBinding("rowDetailAutoCollapse"));
            displayGroup().setDetailInitialExpansionType(
                    AWTDisplayGroup.InitialExpandType.values()[intValueForBinding("rowDetailInitialExpansion")]);
        }
    }

    protected boolean _usingDetailRowExpando ()
    {
        return displayGroup().detailExpansionEnabled();
    }

    /*******************************************************************************
     * Grouping Support
     *******************************************************************************/

    public boolean isGroupingByEnabled ()
    {
        return groupByColumn() != null;
    }

    public boolean isGroupByAllowed ()
    {
        return displayedColumns().size() > 1;
    }

    public boolean groupingByCurrentColumn ()
    {
        return _currentColumn == _groupByColumn;
    }

    public void groupByColumnClicked ()
    {
        // If they explicitly switch grouping, we discard the one specified in the binding
        _groupByColumnBinding = null;
        if (_groupByColumn == _currentColumn) {
            setGroupByColumn(null);  // toggle off
        }
        else {
            setGroupByColumn(_currentColumn);  // switch on
        }
        pushTableConfig();
    }

    protected void checkGroupByColumn ()
    {
        if (_groupByColumnBinding != null) {
            String key = stringValueForBinding(_groupByColumnBinding);
            Column c = (key == null) ? null : findColumnForKey(key);
            setGroupByColumn(c);
        }
    }

    protected void setGroupByColumn (Column column)
    {
        if (column != null && !isGroupByAllowed()) {
            return;
        }
        if (_groupByColumn != column) {
            if (_groupByColumn != null) {
                setColumnVisibility(_groupByColumn, true);  // restore visibility
            }
            if (column != null) {
                setColumnVisibility(column, false);  // hide grouping column, since we're grouping by it
            }
            _groupByColumn = column;

            if (column == null) {
                // reset grouping
                _displayGroup.setGroupSortOrdering(null);
                _displayGroup.setGroupingKey(null);
            }
            else {
                // check for override sortOrdering
                column.prepare(this);
                AWTSortOrdering ordering = column.createSortOrdering(this);
                String key = _groupByColumn.keyPathString();
                _displayGroup.setGroupingKey(key);
                if (ordering != null){
                    _displayGroup.setGroupSortOrdering(ordering);
                }
                else {
                    _displayGroup.setGroupSortOrdering(null);
                }
            }
            resetScrollTop();
        }
    }

    /*******************************************************************************
     * End Grouping Support
     *******************************************************************************/

    protected Column findColumnForKey (String key) {
        List columns = columns();
        for (int i=0, count=columns.size(); i<count; i++) {
            Column col = (Column)columns.get(i);
            col.prepare(this);
            if (col.matchesKey(key)) {
                return col;
            }
        }
        return null;
    }

    protected Column findAndRemoveColumnWithLabel (List columns, String label) {
        for (int i=0, count=columns.size(); i<count; i++) {
            Column col = (Column)columns.get(i);
            col.prepare(this);
            String l = col.label(this);
            if ((l == label) || (l!=null && label!=null && l.equals(label))) {
                columns.remove(i);
                return col;
            }
        }
        return null;
    }

    protected Column findAndRemoveColumnWithKey (List columns, String key) {
        for (int i=0, count=columns.size(); i<count; i++) {
            Column col = (Column)columns.get(i);
            col.prepare(this);
            if (col.matchesKey(key)) {
                columns.remove(i);
                return col;
            }
        }
        return null;
    }

    public void pushTableConfig ()
    {
        boolean hasTableConfigBinding = hasBinding("tableConfig");
        boolean hasTableConfigNameBinding = hasBinding("tableConfigName");
        Assert.that(!(hasTableConfigBinding && hasTableConfigNameBinding),
                    "Cannot have both tableConfig and tableConfigName bindings");
        if (hasTableConfigBinding) {
            Map config = computeTableConfig();
            setValueForBinding(config, "tableConfig");
        }
        if (hasTableConfigNameBinding) {
            Map config = computeTableConfig();
            String configName = stringValueForBinding("tableConfigName");
            setComponentConfiguration(configName, config);
        }
    }

    /**
     * todo: remove this method and provide mechanism to diff/merge external
     * tableConfig with newly added columns in the table. (ie, allow dynamic insertion of
     * new columns with the use of the forceColumnUpdate binding without losing state on
     * existing columns).
     *
     * @param tableConfig
     * @param columnKey
     * @deprecated
     * @aribaapi private
     */
    public static void setVisibleColumn (Map tableConfig, String columnKey)
    {
        List keys = (List)tableConfig.get(HiddenColumnsConfigKey);
        if (keys == null) {
            return;
        }
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            if (columnKey.equals(key)) {
                iter.remove();
            }
        }
        tableConfig.put(HiddenColumnsConfigKey, keys);
    }

    /**
     * Modify the tableConfig to clear the table expansion defaults -- use this
     * method to clear the table expansion default on table configs returned by the
     * data table.
     *
     * @param tableConfig
     * @aribaapi private
     */
    public static void clearOutlineExpansionDefault (Map tableConfig)
    {
        // outline expansion default
        tableConfig.remove(OutlineExpansionDefaultConfigKey);
    }

    protected Map computeTableConfig ()
    {
        /* config is:
            {
                hiddenColumns = (name, name, ...);
                groupByColumn = name;
                sortOrderings = (name, selector, name, selector, ...);
                groupingExpansionDefault = 1;
                outlineExpansionDefault = false;
             }
        */
        Map config = MapUtil.map();

        // hidden columns
        List columns = columns();
        boolean[] visibility = visibilityArray();
        List keys = ListUtil.list();
        for (int i=0, c=columns.size(); i<c; i++) {
            Column col = (Column)columns.get(i);
            col.prepare(this);
            String key = col.keyPathString();
            if (!StringUtil.nullOrEmptyString(key) && !visibility[i]) {
                keys.add(key);
            }
        }
        config.put(HiddenColumnsConfigKey, keys);

        // grouping column
        Column col = groupByColumn();
        if (col != null) {
            String key = col.keyPathString();
            if (key != null) {
                config.put(GroupByColumnConfigKey, key);
            }
        }

        // sort orderings
        List<AWTSortOrdering> sortOrderings = _displayGroup._sortOrderings;
        List/*<AWTSortOrdering, String...>*/ orderings = ListUtil.list();
        if (sortOrderings != null) {
            Iterator iter = sortOrderings.iterator();
            while (iter.hasNext()) {
                AWTSortOrdering sortOrdering = (AWTSortOrdering)iter.next();
                // is this necessary?
                if (sortOrdering.key() != null) {
                    orderings.add(sortOrdering.serialize());
                }
            }
        }
        config.put(SortOrderingsConfigKey, orderings);

        // grouping Expansion default
        config.put(GroupingExpansionDefaultConfigKey,
                   Integer.toString(_displayGroup.groupingExpansionDefault()));

        // outline expansion default
        config.put(OutlineExpansionDefaultConfigKey,
                   Boolean.toString(outlineState().defaultExpansionState()));

        if (_tableBodyCollapsible) {
            config.put(TableBodyExpansionDefaultConfigKey,
                    Boolean.toString(_tableBodyExpanded));
        }

        if (_pivotState != null) {
            _pivotState.writeToTableConfig(config);
        }

        return config;
    }

    protected boolean processTableConfig ()
    {
        boolean hasTableConfigBinding = hasBinding("tableConfig");
        boolean hasTableConfigNameBinding = hasBinding("tableConfigName");
        Assert.that(!(hasTableConfigBinding && hasTableConfigNameBinding),
                    "Cannot have both tableConfig and tableConfigName bindings");
        Map config = null;
        if (hasTableConfigBinding) {
            config = (Map)valueForBinding("tableConfig");
        }
        if (hasTableConfigNameBinding) {
            String configName = stringValueForBinding("tableConfigName");
            config = (Map)componentConfiguration(configName);
        }
        if (config != null) {
            // hidden columns
            List hiddenColumns = (List)config.get(HiddenColumnsConfigKey);
            if (hiddenColumns != null) {
                List columns = columns();
                for (int i=0, c=columns.size(); i<c; i++) {
                    Column col = (Column)columns.get(i);
                    col.prepare(this);
                    String key = col.keyPathString();
                    if (!StringUtil.nullOrEmptyOrBlankString(key)) {
                        boolean show = !hiddenColumns.contains(key);
                        setColumnVisibility(col, show);
                    }
                }
            }

            // group by columns
            String groupByKey = (String)config.get(GroupByColumnConfigKey);
            Column col = !StringUtil.nullOrEmptyOrBlankString(groupByKey) ? findColumnForKey(groupByKey) : null;
            setGroupByColumn(col);

            boolean isPivot = AWTPivotState.processPivotTableConfig(this, config);
            if (!isPivot) {
                // sort orderings
                List sortOrderings = (List)config.get(SortOrderingsConfigKey);
                if (sortOrderings != null) {
                    Iterator iter = sortOrderings.iterator();
                    List orderings = ListUtil.list();
                    while (iter.hasNext()) {
                        String serializedOrdering = (String)iter.next();
                        AWTSortOrdering sortOrdering =
                            AWTSortOrdering.deserialize(serializedOrdering, this);
                        if (sortOrdering != null ) {
                            Column sortedColumn = findColumnForKey(sortOrdering.key());
                            // check to make sure the column is still declared as sortable
                            if (sortedColumn != null && sortedColumn.isSortableColumn(this)) {
                                sortedColumn.prepareSortOrdering(this, sortOrdering);
                                orderings.add(sortOrdering);
                            }
                        }
                    }
                    _displayGroup.setSortOrderings(orderings);
                }

                // grouping expansion default
                String groupingExpansionDefaultString =
                        (String)config.get(GroupingExpansionDefaultConfigKey);
                if (!StringUtil.nullOrEmptyOrBlankString(groupingExpansionDefaultString)) {
                    int groupDefaultExpansion = Integer.parseInt(groupingExpansionDefaultString);
                    _displayGroup.setGroupingExpansionDefault(groupDefaultExpansion);
                }
            }
            // outline expansion default
            String outlineExpansionDefaultString =
                    (String)config.get(OutlineExpansionDefaultConfigKey);
            if (!StringUtil.nullOrEmptyOrBlankString(outlineExpansionDefaultString)) {
                boolean outlineExpansionDefault =
                        Boolean.valueOf(outlineExpansionDefaultString).booleanValue();
                // don't do clear/expand if outline already matches our setting -- it would clear
                // any current outline open/closed state
                if (outlineExpansionDefault != outlineState().defaultExpansionState()) {
                    if (outlineExpansionDefault) {
                        outlineState().expandAll();
                    }
                    else {
                        outlineState().collapseAll();
                    }
                }
            }

            if (_tableBodyCollapsible) {
                String tableBodyExpanded = (String)config.get(TableBodyExpansionDefaultConfigKey);
                if (!StringUtil.nullOrEmptyOrBlankString(tableBodyExpanded)) {
                    _tableBodyExpanded =
                        Boolean.valueOf(outlineExpansionDefaultString).booleanValue();
                }
            }
            return true;
        }
        return false;
    }

    public String awname ()
    {
        String awname = null;
        AWBinding binding = bindingForName(BindingNames.displayGroup);
        if (binding == null) {
            binding = bindingForName(BindingNames.list);
        }
        if (binding != null) {
            awname = AWRecordingManager.actionEffectiveKeyPathInComponent(binding, parent());
        }
        return awname;
    }

    /**
     * Silly interface to decouple us from demoshell
     */
    public interface CSVSourceFactory
    {
        public AWTDataSource dataSourceForPath(String csvPath, AWComponent parentComponent);
    }

    private static CSVSourceFactory _CSVSourceFactory;
    public static void setCSVSourceFactory (CSVSourceFactory factory)
    {
        _CSVSourceFactory = factory;
    }

    public AWTSortOrdering createSortOrderingForColumnKey (String key)
    {
        Column col = findColumnForKey(key);
        if (col == null) {
            // the column may no longer exist when this
            // is called due to resetting of the table.
            return null;
        }
        col.prepare(this);
        return col.createSortOrdering(this);
    }

    List columnsMatchingKeys (List keys)
    {
        int count = keys.size();
        List result = ListUtil.list(count);
        for (int i=0; i < count; i++) {
            String key = (String)keys.get(i);
            Column column = findColumnForKey(key);
            Assert.that((column != null), "Unable to find column: %s", key);
            result.add(column);
        }
        return result;
    }

    List keysForColumns (List columns)
    {
        int count = columns.size();
        List result = ListUtil.list(count);
        for (int i=0; i < count; i++) {
            Column column = (Column)columns.get(i);
            String pathString = column.keyPathString();
            result.add(pathString);
        }
        return result;
    }

    public static FieldPath[] pathsForColumns (Column[] columns)
    {
        int count = columns.length;
        FieldPath[] result = new FieldPath[count];
        for (int i=0; i < count; i++) {
            String pathString = columns[i].keyPathString();
            result[i] = ((pathString != null) ? FieldPath.sharedFieldPath(pathString) : null);
        }
        return result;
    }

    public boolean _renderingHeader = false;
    /* Called repeatedly when rendering data row to see if to render again (for pivot row attribute rows) */
    protected boolean _didRenderCurrentRow = false;
    protected boolean _renderingPrimaryRow = false;
    public Object renderCurrentRow ()
    {
        if (!_didRenderCurrentRow) {
            _didRenderCurrentRow = true;
            _renderingPrimaryRow = true;
            if (_pivotState != null) return _pivotState.preparePrimaryRow(this);
            return Boolean.TRUE;
        }
        _renderingPrimaryRow = false;
        if ((_pivotState != null) && _pivotState.prepareDetailRow(this)) {
            // reset pivot state re-processing this row
            return Boolean.TRUE;
        }
        _didRenderCurrentRow = false;
        return null;
    }

    /* Proxy Column -- forwards all methods to target column instance */
    public static class ProxyColumn extends Column
    {
        Column _target;

        public ProxyColumn (Column column)
        {
            _target = column;
        }

        public String rendererComponentName () { return _target.rendererComponentName(); }
        public void initializeColumn (AWTDataTable table) { _target.initializeColumn(table);}
        public void prepare (AWTDataTable table) { _target.prepare(table);}
        public void setCurrentItem (Object item, AWTDataTable table) { _target.setCurrentItem(item, table);}
        public boolean isValueColumn () { return _target.isValueColumn(); }
        public boolean isValueColumn (AWTDataTable sender) { return _target.isValueColumn(sender); }
        public boolean isSortableColumn (AWTDataTable sender) { return _target.isSortableColumn(sender); }
        public boolean isGroupableColumn (AWTDataTable sender) { return _target.isGroupableColumn(sender); }
        public boolean isOptional (AWTDataTable sender) { return _target.isOptional(sender); }
        public boolean initiallyVisible () { return _target.initiallyVisible(); }
        public boolean initiallyVisible (AWTDataTable sender) { return _target.initiallyVisible(sender); }
        public boolean wantsSpan (AWTDataTable sender) { return _target.wantsSpan(sender); }
        public boolean isBlank (AWTDataTable sender) { return _target.isBlank(sender); }
        public String keyPathString () { return _target.keyPathString(); }
        public String label (AWTDataTable table) { return _target.label(table); }
        protected String label () { return _target.label(); }
        public AWTSortOrdering createSortOrdering (AWTDataTable table) { return _target.createSortOrdering(table); }
        public boolean matchesKey (String key) { return _target.matchesKey(key); }
        protected Object bindingValue (AWBinding binding, AWTDataTable table) { return _target.bindingValue(binding, table); }
        public int minWidthPx (AWTDataTable table) { return _target.minWidthPx(table); }

        public Column prepareAndReplace (AWTDataTable table)
        {
            prepare(table);
            return _target;
        }
    }

    /*******************************************************************************
     * Pivot Support
     *******************************************************************************/

    /*
        Pivot mode is used to render a multi-dimensional data set along
        two dimensions: rows and columns.

        See AWTPivotState.java for the bulk of that implementation.
    */
    AWBinding _pivotLayoutBinding = null;
    public AWTPivotState _pivotState = null;

    public AWTPivotState pivotState ()
    {
        return _pivotState;
    }

    public boolean hasPivotHeadingRows ()
    {
        return (_pivotState != null)  && _pivotState.columnEdgeLevels() > 0;
    }

    /*******************************************************************************
     * ErrorHandler Support
     *******************************************************************************/
    
    /**
        data table assist in navigating to display item (row)
        with error
     */
    public class DataTableNavigationHandler implements AWErrorHandler
    {
        AWTDataTable thisTable;

        public DataTableNavigationHandler (AWTDataTable table)
        {
            thisTable = table;
        }

        public boolean canGoToErrorImmediately (AWErrorInfo error, AWComponent pageComponent)
        {
            if (error.getAssociatedTableItem() != null) {
                AWTDataTable datatable = (AWTDataTable)error.getAssociatedDataTable();
                if (datatable != null && datatable == thisTable) {
                    // Todo: If the datatable is already in the middle
                    //       of rendering, force visible may not be
                    //       honored.  We need to detect that
                    //       situation and return false accordingly.
                    return true;
                }
            }

            return false;
        }

        public boolean canGoToErrorWithLink (AWErrorInfo error, AWComponent pageComponent)
        {
            return false;
        }

        public AWComponent goToError (AWErrorInfo error, AWComponent pageComponent)
        {
            if (error.getAssociatedTableItem() != null) {
                AWTDataTable datatable = (AWTDataTable)error.getAssociatedDataTable();
                if (datatable != null && datatable == thisTable) {
                    AWTDisplayGroup displayGroup = datatable.displayGroup();
                    displayGroup.setItemToForceVisible(error.getAssociatedTableItem());
                    ariba.ui.aribaweb.util.Log.aribaweb_errorManager.debug(
                        "***** DataTableNavigationHandler: force visible item=%s, error=%s",
                        error.getAssociatedTableItem(), error);
                    return pageComponent;
                }
            }
            return null;
        }

        public AWErrorInfo selectFirstError (List /*AWErrorInfo*/ errors)
        {
            return null;
        }
    }
}

