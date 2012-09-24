/*
    Datatable.js  -- scrolling data table

    (see ariba.ui.table.AWTDataTable.awl)
*/

ariba.Datatable = function() {
    // imports
    var Util = ariba.Util;
    var Dom = ariba.Dom;
    var Event = ariba.Event;
    var Request = ariba.Request;
    var DragDrop = ariba.DragDrop;
    var Input = ariba.Input;
    var Widgets = ariba.Widgets;
    var Debug = ariba.Debug;

    // private vars
    var _awtTables = new Object();  // hashtable -- key == elementId, value == tableInfo
    var _awtMouseDown;
    var _awtMouseDownTarget;
    var _AWTInPostLoad = false;
    var _AWTPendingScroll = false;
    var _AWTDidRegRefreshCallback = false;
    var _AWTDidRegWindowResizeCallback = false;
    var AWTForceDragStyle = "awtForceDrag";
    var _FlexId = 0;
    var _AWTPendingScrollInfo;
    var UseScrollPad = Dom.IsIE;
    var ScrollSize = 18;
    var _AWTRowHeight = 25;
    var _awtScrollTimer = null;
    var _AWTScrollDelay = 250;
    var _awtLastScrollMillis;
    var ScrollPanelTimeoutId;

    // Scroll faulting
    var AWTFaultIndex_Position = 0; // values: -1 top, 0 no fault, 1 bottom
    var AWTFaultIndex_ScrollType = 1; // values: top, up, down, bottom
    var AWTFaultIndex_TopRow = 2;
    var AWTFaultIndex_TopOffset = 3;
    var AWTFaultIndex_BottomRow = 4;
    var AWTFaultIndex_BottomOffset = 5;
    var AWTFaultIndex_size = 6;

    var _AWTDragHoverStyle = "tableRowDragHover ";
    // var _AWTDragStyle = "tableRowDrag ";
    var _AWTSelectStyle = "tableRowSelected ";
    var _AWTHoverStyle = "tableRowHover ";
    var AWTableDragPrefix = "awtDrg_";
    var AWTableDropPrefix = "awtDrp_";
    var AWTSelectColumnIndex = 0;
    var _awtSelectStart = false;
    var _awtMouseDragIndicator;
    var AWNullValue = new Object();
    var _numTablesKey = "numTables";

    // externally defined function to avoid circular reference
    // http://msdn.microsoft.com/library/default.asp?url=/library/en-us/IETechCol/dnwebgen/ie_leak_patterns.asp
    var _awtMMControlOnClickHandler = function (evt) {
                var target = (evt.srcElement) ? evt.srcElement : evt.target;
                var id = target.getAttribute("_tbId");
                var tableInfo = ariba.Datatable.infoForScrollableTable(id);
                Debug.log("Click!!!")
                return ariba.Datatable.maxMin(tableInfo);
            }

    var _awtTableBodyOnScrollHandler = function () {
                var id = this.getAttribute("_tbId");
                var tableInfo = ariba.Datatable.infoForScrollableTable(id);
                if (tableInfo.head) {
                    tableInfo.head.scrollLeft = tableInfo.body.scrollLeft;
                }
                ariba.Datatable.handleVerticalScroll(tableInfo);
            }

    var Datatable = {

        registerTableInfo : function (id, tableInfo)
        {
            _awtTables[id] = tableInfo;
            var container = Dom.findParentUsingPredicate(tableInfo.wrapperTable, function(n) {
                return Dom.hasClass(n, "flexContainer");
            });
            if (container && !container.getAttribute("_cid")) {
                container.setAttribute("_cid", _FlexId++);
            }
            tableInfo.flexContainer = container;
            tableInfo.positioningParent = (!tableInfo.flexContainer) ? null
                    : Dom.findParentUsingPredicate(tableInfo.flexContainer, function (e) {
                return Dom.hasClass(e, "panelContainer");
            });
            if (!tableInfo.positioningParent) {
                tableInfo.positioningParent = Dom.positioningParent(tableInfo.flexContainer);
            }
            else {
                Util.incrementAttribute(tableInfo.positioningParent, _numTablesKey);
            }

            tableInfo.bodyHt = 0;
        },

        registerNonScrollTableId : function (id, isDraggable, disableRowSelection, disableCheckNbsps, checkSelectionStyle)
        {
            //debug("Register nonScrollTable: id=" + id);
            var bodyTable = Dom.getElementById(id);
            var wrapperTable = this.wrapperForBody(bodyTable);
            var tableInfo = _awtTables[id];
            if (!tableInfo || (wrapperTable != tableInfo.wrapperTable) || (bodyTable != tableInfo.bodyTable)) {
                tableInfo = new Object();
                tableInfo.isDraggable = isDraggable;
                tableInfo.wrapperTable = wrapperTable;
                tableInfo.bodyTable = bodyTable;
                tableInfo.isScrollTable = false;
                tableInfo.disableRowSelection = disableRowSelection;
                tableInfo.checkNbsps = !disableCheckNbsps;
                tableInfo.checkSelectionStyle = checkSelectionStyle;
                this.registerTableInfo(id, tableInfo);
            }
            var table = tableInfo.bodyTable;
            if (table == null) alert("can't find table in wrapperTable: " + id);
            this.setupTable(tableInfo);
        },

        wrapperForBody : function (bodyTable)
        {
            return Dom.findParentUsingPredicate(bodyTable, function (e) {
                return e.tagName == "TABLE" && e.getAttribute("minHeight");
            });
        },

        // called on table creation, or change to table contents
        registerScrollTable : function (id, disableRowSelection, disableCheckNbsps, checkSelectionStyle, tableHeaderId)
        {
            var bodyTable = Dom.getElementById(id);
            var wrapperTable = this.wrapperForBody(bodyTable);
            var tableInfo = _awtTables[id];
            Debug.log("*** awtRegisterScrollTable: " + id);
            if (!tableInfo || (bodyTable != tableInfo.bodyTable)) {
                // Debug.log("    awtRegisterScrollTable -- creating new tableInfo");
                // new table or need to reset table
                tableInfo = new Object();
                tableInfo.disableRowSelection = disableRowSelection;
                tableInfo.checkNbsps = !disableCheckNbsps;
                tableInfo.checkSelectionStyle = checkSelectionStyle;
                tableInfo.bodyTable = bodyTable;
                tableInfo.wrapperTable = wrapperTable;
                tableInfo.head = Dom.findChildUsingPredicate(wrapperTable, function (e) {
                    return e.tagName == "DIV" && e.className == "tableHead";
                });
                // Debug.log("tableInfo.head = " + tableInfo.head);
                tableInfo.headTable = Dom.findChild(tableInfo.head, "TABLE");
                // Debug.log("tableInfo.headTable = " + tableInfo.headTable);
                tableInfo.body = Dom.findParent(tableInfo.bodyTable, "DIV");
                tableInfo.minHeight = parseInt(wrapperTable.getAttribute("minHeight"));
                tableInfo.maxHeight = parseInt(wrapperTable.getAttribute("maxHeight"));

                tableInfo.isScrollTable = true;
                this.registerTableInfo(id, tableInfo);

                if (!_AWTDidRegRefreshCallback) {
                    // make sure we re-check scroll table sizing on any refresh
                    // -- enqueue so that it's coalesced with any real resize event
                    _AWTDidRegRefreshCallback = true;
                }

                // set up maximize control  -- (performance: restrict scope of search to header if possible)
                // If this is legacy usage (i.e. Sourcing) then tableHeaderId==null and we need to search whole wrapperTable,
                // otherwise '' means no search, and an id means search that table
                var isLegacy = (tableHeaderId == null);
                var searchElement = (isLegacy ? wrapperTable
                        : ((tableHeaderId.length) ? Dom.getElementById(tableHeaderId) : null));
                // Debug.log("tableHeaderId: " + ((tableHeaderId == null) ? "*NULL*" : ("'" + tableHeaderId + "'")) + " --> " + searchElement);
                if (searchElement) {

                    var mmControl = Dom.findChildUsingPredicate(searchElement, function (e) {
                        return e.tagName == "TD" && e.className == "awtMMNone";
                    });
                    if (tableInfo.mmControl != mmControl) {
                        tableInfo.mmControl = mmControl;
                        if (tableInfo.mmControl) {
                            tableInfo.mmControl.setAttribute("_tbId", id);
                            tableInfo.mmControl.onclick = _awtMMControlOnClickHandler.bindEventHandler(null);
                        }
                    }
                }
            }

            this.registerPostLoad();
        },

        /**
         * This is called on init, and on change to parameters.
         * @param id
         * @param isDraggable
         * @param isMaximizedId
         * @param topCount
         * @param bottomCount
         * @param topIndexId This is the same as the topIndex in AWTDisplayGroup.
         * @param topOffsetId
         * @param scrollFaultActionId
         * @param rowIdToForceVisible
         * @param updateSelectAllActionId
         *
         * @see ariba.ui.table.AWTDataTable
         * @see ariba.ui.table.AWTDisplayGroup
         */
        updateScrollTable : function(id, isDraggable, isMaximizedId,
                                      topCount, bottomCount, topIndexId, topOffsetId, 
                                      leftPosId, scrollFaultActionId,
                                      rowIdToForceVisible, updateSelectAllActionId)
        {
            var tableInfo = this.infoForTable(id);
            if (!tableInfo) return;  // Assert?
            tableInfo.isDraggable = isDraggable;
            tableInfo.isMaximizedId = (isMaximizedId == "") ? null : isMaximizedId;
            tableInfo.maximize = (isMaximizedId) ? (Dom.elementValue(isMaximizedId) == "true") : false;

            tableInfo.rowIdToForceVisible = (rowIdToForceVisible == "") ? null : rowIdToForceVisible;

            // table faulting properties
            if (topCount) {
                tableInfo.topCount = parseInt(topCount);
                tableInfo.bottomCount = parseInt(bottomCount);
                tableInfo.topIndexId = topIndexId;
                tableInfo.topOffsetId = topOffsetId;
                tableInfo.leftPosId =leftPosId;
                tableInfo.scrollFaultActionId = scrollFaultActionId;
                tableInfo.repositionScroll = true;
            }

            tableInfo.updateSelectAllActionId = updateSelectAllActionId;
            this.registerPostLoad();
        },



        registerPostLoad : function ()
        {
            // register callback to execute after all the content on the page
            // has been rendered
            if (!_AWTInPostLoad) {
                _AWTInPostLoad = true;
                Event.registerUpdateCompleteCallback(this.postLoad.bind(this));
            }
        },

        infoForScrollableTable : function (id)
        {
            var info = this.infoForTable(id);
            return (info && info.isScrollTable) ? info : null;
        },

        infoForTable : function (id)
        {
            var info = _awtTables[id];

            // make sure table still in document
            if (!Dom.getElementById(id)) {
                // delete stale info
                delete _awtTables[id];
                return null;
            }

            return info;
        },

        setupTable : function (tableInfo)
        {
            var bodyTable = tableInfo.bodyTable;

            // Debug.log("table init " + bodyTable.id + " " + isDraggable);

            // add scrolling / selection event handlers
            if (!bodyTable.onmouseover) {
                bodyTable.onmouseover = this.mouseOverEventHandler.bindEventHandler(this);
                bodyTable.onmouseout = this.mouseOutEventHandler.bindEventHandler(this);
                bodyTable.onclick = this.rowClickedEventHandler.bindEventHandler(this);
                bodyTable.onmousedown = this.mouseDownEvtHandler.bindEventHandler(this);
                bodyTable.onmouseup = this.mouseUpEvtHandler.bindEventHandler(this);
                bodyTable.onmousemove = this.mouseMoveEvtHandler.bindEventHandler(this);

                // This one can't be Event.handler() wrapped because the deferring of the handling will allow the
                // browser text selection to grab the event and mess everything up...
                if (Dom.IsIE) {
                    bodyTable.onselectstart = function (evt) {
                        Datatable.selectStartEvtHandler(evt);
                    }
                }
            }

            // highlight selected rows
            if (tableInfo.checkSelectionStyle) this._updateSelections(tableInfo);

            if (tableInfo.checkNbsps) {
                Debug.log("adding nbsps to body");
                this.addNbsps(bodyTable);
            }
        },

        addNbsps : function (table)
        {
            // add nbsps to empty cells  --- Expensive!
            var rows = table.rows;

            for (var i = 0; i < rows.length; i++) {
                var cells = rows[i].cells;
                // Safari will more rows, but they they are really
                // rows.  So skipping these.
                if (cells) {
                    for (var j = 0; j < cells.length; j++) {
                        var cell = cells[j];
                        var numChildren = cell.childNodes.length;
                        if (numChildren == 0) {
                            cell.innerHTML = "&nbsp;";
                        } else if (numChildren == 1) {
                            // look for an empty span
                            var innerCell = cell.childNodes[0];
                            if (((innerCell.tagName == "SPAN") || (innerCell.tagName == "A")) && innerCell.childNodes.length == 0) {
                                cell.innerHTML = "&nbsp;";
                            }
                        }
                    }
                }
            }
        },

        setupScrollTable : function (tableInfo)
        {
            // ToDo: What happens if change causes number of columns to change?  Then need to redo setup?
            if (tableInfo.bodyTable.rows.length == 0) {
                tableInfo.isScrollTable = false;
            }
            else if (!tableInfo.scrollSetup) {
                tableInfo.scrollSetup = true;

                // Debug.printProperties(tableInfo);
                // Add an invisible row to tableBody that mirrors header row -- so body cols
                // do not compress more than the header should allow
                var bodyCells = tableInfo.bodyTable.rows[0].cells;
                var newBodyRow = document.createElement('TR');
                newBodyRow.className = "AWTColAlignRow";

                if (tableInfo.headTable) {
                    var headerRows = tableInfo.headTable.rows;
                    var headerRow = headerRows[0];
                    var fixHeader = !headerRow.getAttribute("_awtspacer");
                    // check empty cells in the header
                    if (tableInfo.checkNbsps) this.addNbsps(tableInfo.headTable);

                    // and a head table row, so we can sync header widths with body table
                    var newHeaderRow = document.createElement('TR');

                    // Debug.log("Adding body cells to preserve spacing for header: ");
                    var headCells = headerRow.cells;
                    var headCount = headCells.length - (fixHeader ? 0 : 1);
                    for (var i = 0; i < headCount; i++) {
                        var headCell = headCells[i];

                        // for each of the cells in the header, add an empty TD to the hidden body
                        // spacer row with a padding that equals the minimum width of the header cell
                        var td = document.createElement('TD');
                        td.appendChild(document.createTextNode(''));
                        var wd = headCell.offsetWidth;
                        td.style.paddingRight = wd + "px";
                        td.width = headCell.width;
                        // Debug.log("Setting spacer td.width = " + headCell.width);

                        // Debug.log("--- cell[" + i + "]" + ".offsetWidth = " + wd);
                        newBodyRow.appendChild(td);

                        // create an empty TH - later will set padding to sync with body columns
                        var th = document.createElement('TH');
                        th.appendChild(document.createTextNode(''));
                        newHeaderRow.appendChild(th);
                    }
                    var td = document.createElement('TD');
                    td.appendChild(document.createTextNode(''));
                    td.className = "spacer";
                    newBodyRow.appendChild(td);

                    if (fixHeader) {
                        // add extra padding cell to header (for scrollbar padding)
                        var rowspan = 0;
                        for (var i = 0; i < headerRows.length; i++) {
                            if (rowspan > 1) {
                                rowspan--;
                            } else {
                                var row = headerRows[i];
                                if (row && row.cells && row.cells.length > 0) {
                                    var protoCell = row.cells[row.cells.length - 1];
                                    rowspan = protoCell.rowSpan;
                                    var th = document.createElement(protoCell.tagName);
                                    th.appendChild(document.createTextNode(''));
                                    if (i > 0) th.innerHTML = "&nbsp;"
                                    th.className = protoCell.className + " thSpacer";
                                    if (rowspan) th.rowSpan = rowspan;
                                    row.appendChild(th);
                                }
                            }
                        }

                        newHeaderRow.appendChild(document.createElement('TH'));
                        tableInfo.headTable.tBodies[0].insertBefore(newHeaderRow, tableInfo.headTable.rows[0]);

                        // remove the forced width on the header table.  This is initially set to 1px in
                        // so the header cells are initially displayed with their minimum widths.
                        tableInfo.headTable.style.width = "auto";
                        newHeaderRow.setAttribute("_awtspacer", 1);
                    }
                } else {
                    // add one extra TD to the first row
                    for (var i = 0; i < (bodyCells.length + 1); i++) {
                        // create an empty TD with a padding that equals the space that we need to consume
                        var td = document.createElement('TD');
                        td.appendChild(document.createTextNode(''));
                        // minimize the last TD (scrollbar padding)
                        if (i == bodyCells.length) {
                            td.className = "spacer";
                        }
                        newBodyRow.appendChild(td);
                    }
                }

                tableInfo.bodyTable.tBodies[0].insertBefore(newBodyRow, tableInfo.bodyTable.rows[0]);

                //When the response comes in, the table content gets inserted first into
                //the body table. This can be smaller than the previous view causing the
                //the table body to shrink and the horizontal scroll to shift right.
                //We restore the original width by inserting a dummy row with the same
                //width as the previous view(above). Now we restore the horizontal 
                //scroll to the same position as the previous view. 
                var leftPos = Dom.elementIntValue(tableInfo.leftPosId);
                if (leftPos > 0) {
                    tableInfo.body.scrollLeft = leftPos;
                }
                // Debug.log("Initial Scroll Pos: " + tableInfo.body.scrollLeft + ", " + tableInfo.body.scrollTop + "!!");

                // Scroll sync between body and header
                tableInfo.body.setAttribute("_tbId", tableInfo.bodyTable.id);
                tableInfo.body.onscroll = _awtTableBodyOnScrollHandler;
            }

            // generic setup
            this.setupTable(tableInfo);
        },

        handleDataTableException : function (exception, id)
        {
            var msg = "datatable exception: " + exception + ", id: " + id;
            Debug.log(msg);
            if (Request.AWDebugEnabled) {
                alert(msg);
            }
        },

        postLoad : function ()
        {
            // re-enable the WaitCursor ()  (see this.scrollFaultAction(...))
            Input.setShowWaitCursorDisabled(false);
            Debug.log("awtPostLoad running...");
            var faultTableInfo = new Array();
            for (var id in _awtTables) {
                try {
                    var tableInfo = this.infoForScrollableTable(id);
                    // Debug.log("Looking for Id: " + id + ", info:" + _awtTables[id] + ", scroll" + tableInfo);
                    if (tableInfo) {
                        this.setupScrollTable(tableInfo);
                        // keep a list of all faulting tables, for additional initialization
                        if (tableInfo.topIndexId) {
                            faultTableInfo.push(tableInfo);
                        }
                    }
                } catch (e) { this.handleDataTableException(e, id); }
            }

            // initialize faulting tables`
            for (var i = 0; i < faultTableInfo.length; i++) {
                try {
                    // setup top and bottom fault areas, and restore scroll position
                    this.setupScrollFaulting(faultTableInfo[i]);
                } catch (e) { this.handleDataTableException(e, ""); } 
            }

            // force an initial resize
            Event.eventEnqueue(this.windowResized.bind(this));

            if (!_AWTDidRegWindowResizeCallback) {
                Event.registerOnWindowResize(this.windowResized.bind(this));
                _AWTDidRegWindowResizeCallback = true;
            }

            _AWTInPostLoad = false;
            _AWTPendingScroll = false;

            if (!_AWTPendingScrollInfo) {
                this.scrollHidePanel();
            }
            else {
                var tableInfo = _AWTPendingScrollInfo;
                //debug("pending scroll info");
                _AWTPendingScrollInfo = null;
                setTimeout(function() {
                    Datatable.processScrollFault(tableInfo);
                }, 0);
            }
            //debug("awtPostLoad complete!");
        },

        fixHeadingWidths : function (tableInfo)
        {
            if (!tableInfo.headTable) return;

            // go through each cell in the head and resize
            var headCells = tableInfo.headTable.rows[0].cells;
            var bodyRows = tableInfo.bodyTable.rows;
            var bodyCells = bodyRows[0].cells;

            Debug.log("Setting header cell widths...");
            var totalWd = 0;
            var empty = (bodyRows.length > 1 && bodyRows[1].cells.length > 0) && Dom.hasClass(bodyRows[1].cells[0], "empty");
            if (!empty) {
                for (var i = 0; i < bodyCells.length - 1; i++) {
                    var headCell = headCells[i];
                    var bodyCell = bodyCells[i];
                    var newWd = bodyCell.offsetWidth;
                    totalWd += newWd;

                    // Idea: If scroll faulting, we might want to force the columns to be the max of
                    // the old size and the new size
                    headCell.style.paddingRight = newWd + "px";
                }
                headCells[headCells.length-1].style.paddingRight = "100px";
            } else {
                tableInfo.headTable.style.width = "100%";
            }
            //debug("--- total = " + totalWd + "  (wrapper=" + tableInfo.wrapperTable.offsetWidth + ")");
        },

        fixAllHeadingWidths : function ()
        {
            for (var id in _awtTables) {
                try {
                    var info = this.infoForScrollableTable(id);
                    if (info) {
                        this.fixHeadingWidths(info);
                    }
                } catch (e) { this.handleDataTableException(e, id); }
            }
        },

        spacerWd : function (tableInfo) {
            return (UseScrollPad && Dom.hasClass(tableInfo.wrapperTable, "yScroll")) ? ScrollSize + 1 : 1;
        },

        desiredWidth : function (tableInfo) {
            var cells = tableInfo.bodyTable.rows[0].cells;
            var spacerCell = cells[cells.length - 1];
            var curSpacer = spacerCell.clientWidth;
            var needSpacer = this.spacerWd(tableInfo);
            var tableWidth = tableInfo.bodyTable.offsetWidth;
            return tableWidth - curSpacer + needSpacer;
        },

        checkWidthStrut : function (tableInfo, positioningParent, desiredWidth)
        {
            // dialog boxes only
            if (positioningParent == Dom.documentElement()) {
                return;
            }
            var panelWidth = Widgets.panelMaxWidth(positioningParent, tableInfo.wrapperTable);
            var isOnlyTable = Util.getIntAttribute(positioningParent, _numTablesKey) <= 1;
            var tallRows = this.avgRowHeight(tableInfo) > 35;
            if (tallRows && isOnlyTable) {
                desiredWidth *= 1.3;
            }
            var pad = (Dom.absoluteLeft(tableInfo.bodyTable) - Dom.absoluteLeft(tableInfo.wrapperTable)) * 2;
            desiredWidth = Math.min(desiredWidth + pad, panelWidth);

            var awtstrut = tableInfo.wrapperTable.parentNode.nextSibling;
            var currentWidth = parseInt(awtstrut.style.width) || 0;

            // Temp fix.  Should clean up stale relocatable div instead
            if (desiredWidth > 0 && currentWidth != desiredWidth) {
                awtstrut.style.width = desiredWidth + "px";
            }
        },

        innerSize : function (elm)
        {
            var w = (Dom.isSafari) ? elm.offsetWidth : elm.clientWidth;
            var h = (Dom.isSafari) ? elm.offsetHeight : elm.clientHeight;
            return [w - parseInt(Dom.effectiveStyle(elm, "padding-left"))
                    - parseInt(Dom.effectiveStyle(elm, "padding-right")),
                h - parseInt(Dom.effectiveStyle(elm, "padding-top"))
                        - parseInt(Dom.effectiveStyle(elm, "padding-bottom"))];
        },

        computeMinMaxHt : function (tableInfo, desiredWidth) {
            var rec = new Object();

            var origHt = null;

            // Hack! Under Firefox (v2.0, 12/2006) body table are sometimes initially sizing too short, resulting
            // in bad layouts.  The (mysterious) workarond is to resize the parent div before asking for the size
            // -- for some reason this yields the correct answer.  When Firefox 3.0 comes out, check if we can
            // eliminate this hack.
            if (!Dom.IsIE && !tableInfo.didSize) {
                origHt = tableInfo.body.style.height;
                tableInfo.body.style.height = "1px";
                tableInfo.didSize = true;
            }

            // Max...
            var contentNeeds = tableInfo.bodyTable.offsetHeight;
            var wrapperHt = tableInfo.wrapperTable.offsetHeight - tableInfo.body.offsetHeight;
            // if we need a scrollbar, provide for that as well
            if (desiredWidth - 2 > tableInfo.bodyTable.parentNode.offsetWidth) {
                contentNeeds += ScrollSize;
            }

            rec.maxHt = contentNeeds + wrapperHt;

            // Min...
            rec.minHt = (tableInfo.maximize)
                    ? Math.min(rec.maxHt, Math.max(document.documentElement.clientHeight - 20, tableInfo.minHeight))
                    : Math.min(rec.maxHt, tableInfo.minHeight + wrapperHt);

            rec.desiredWidth = desiredWidth;
            /*
                Debug.log ("awtComputeMinMaxHt -- bodyTable.offsetHeight=" + contentNeeds
                            + ", wrapperTable.offsetHeight=" + tableInfo.wrapperTable.offsetHeight
                        + ", parent=" + tableInfo.bodyTable.parentNode.offsetHeight
                        + ", bodyTable.offsetWidth=" + tableInfo.bodyTable.offsetWidth
                        + ", wrapperTable.offsetWidth=" + tableInfo.wrapperTable.offsetWidth
                        + " -->  min:" + rec.minHt + ", max:" + rec.maxHt);
            */
            // Firefox hack continued...
            if (origHt) tableInfo.body.style.height = origHt;

            return rec;
        },

        maxMin : function (tableInfo)
        {
            tableInfo.maximize = !(tableInfo.maximize);
            Dom.setElementValue(tableInfo.isMaximizedId, ((tableInfo.maximize) ? "true" : "false")); // push value to server
            tableInfo.newScrollTop = tableInfo.body.scrollTop;
            Event.eventEnqueue(this.windowResized.bind(this));

            if (tableInfo.maximize) {
                Dom.setPageScrollTop(Dom.absoluteTop(tableInfo.wrapperTable) - 10);
            }

            return false;
        },

        windowResized : function ()
        {
            // avoid the recalc ping-pong
            // the ping pong occurs when the max height computation toggles between two values.
            // It happens when we have a horizontal scroll, but we failed to detect it correctly.
            // This causes the max height to have a difference of 18 (the horizontal scrollbar size)
            // The smaller max height causes the browser to display a vertical scrollbar.
            // This in turn changes the client width, and therefore the horizontal scroll detection.
            // See awtComputeMaxHt

            this.fixAllHeadingWidths();
            
            // compute groups by table
            var footerHidden = false;
            var groups = new Object();
            for (var id in _awtTables) {
                try {
                    var info = this.infoForScrollableTable(id);
                    if (info) {
                        var containerId = (info.flexContainer) ? info.flexContainer.getAttribute("_cid") : "00";
                        var list = (groups[containerId] || (groups[containerId] = new Object()));
                        list[id] = info;
                        if (!footerHidden) {
                            Widgets.hideFloatingFooter();
                            footerHidden = true;
                        }
                    }
                } catch (e) { this.handleDataTableException(e, id); }
            }
            // Debug.log("awtWindowResized: groups: %d", groups.length-1)

            // size by group
            for (var g in groups) {
                this.windowResizedForGroup(groups[g]);
            }

            // double-check that we got the window scrollbar right
            if (this.checkWindowScrollbar(true)) {
                // if things change, then run through header/body table sync up
                for (var g in groups) {
                    for (var id in groups[g]) {
                        try {
                            var tableInfo = this.infoForScrollableTable(id);
                            if (tableInfo) {
                                this.fixHeadingWidths(tableInfo);
                            }
                        } catch (e) { this.handleDataTableException(e, id); }
                    }
                }
            }
        },

        windowResizedForGroup : function (tableGroup)
        {
            var totalReq = 0;
            var totalUsed = 0;
            var totalExtraWanted = 0;
            var windowExtra = 0;
            var positioningParent = document.documentElement;

            // calculate needs
            var htInfo = [];
            Debug.log("<b><u>**** Datatable Resizing Group</u></b>");
            for (var id in tableGroup) {
                try {
                    Debug.log("Table <b>" + id + "</b>");
                    var tableInfo = this.infoForScrollableTable(id);
                    if (!tableInfo) continue;
                    var wrapperTable = tableInfo.wrapperTable;

                    //Panel support: set strut width to influence parent size.  If our rows
                    // appear to be wrapping, ask for more
                    var desiredWidth = this.desiredWidth(tableInfo);
                    this.checkWidthStrut(tableInfo, tableInfo.positioningParent, desiredWidth);

                    // Compute size for this one
                    var rec = this.computeMinMaxHt(tableInfo, desiredWidth);
                    totalUsed += wrapperTable.offsetHeight;
                    // Debug.log ("SIZE (" + id + "): " + rec.minHt + "/" + rec.maxHt);
                    htInfo[id] = rec;

                    totalExtraWanted += (rec.maxHt - rec.minHt);
                    totalReq += rec.minHt;
                } catch (e) { this.handleDataTableException(e, id); }
            }

            // Figure out window / container space
            var flexContainer = tableInfo.flexContainer;
            var positioningParent = tableInfo.positioningParent;
            var positioningParentHeight = 0;
            if (flexContainer) {
                Debug.log("flexContainer.clientHeight=" + flexContainer.clientHeight + ", offsetHeight=" + flexContainer.offsetHeight);

                if (Dom.hasClass(positioningParent, "panelContainer")) {
                    Debug.log("&&&& PanelContainer is positioning parent -- height=" + positioningParent.offsetHeight);
                    positioningParentHeight = Widgets.panelMaxHt(positioningParent);
                }

                // Iterate up parent boxes accumulating free space
                var curFC = flexContainer;
                while (curFC) {
                    // curFC.style.border="1px solid blue";
                    var flexBox = Dom.findParentUsingPredicate(curFC, function(n) {
                        return n.tagName == "TD";
                    }) || document.documentElement;
                    if (flexBox) {
                        // flexBox.style.border="1px solid red";
                        Debug.log("flexBox.clientHeight=" + flexBox.clientHeight + ", offsetHeight=" + flexBox.offsetHeight);
                        windowExtra += this.innerSize(flexBox)[1] - curFC.offsetHeight;
                    }
                    curFC = Dom.findParentUsingPredicate(curFC, function(n) {
                        return Dom.hasClass(n, "flexContainer");
                    });
                    // Don't leak out beyond our positioningParent!
                    if (curFC && !Dom.findParentUsingPredicate(curFC, function(n) {
                        return n == positioningParent;
                    })) curFC = null;
                }
            }
            if (positioningParentHeight == 0) positioningParentHeight = positioningParent.clientHeight;
            var container = (positioningParent.tagName == "HTML") ? document.body :
                            ((positioningParent.childNodes.length == 1) ? positioningParent.childNodes[0] : positioningParent);
            var containerContentHeight = container.offsetHeight;

            windowExtra += positioningParentHeight - containerContentHeight;

            Debug.log("positioningParent.clientHeight:" + positioningParentHeight + ", content:" + containerContentHeight);

            Debug.log("--- Resize!  offsetHeight=" + positioningParentHeight
                    + ", totalUsed=" + totalUsed + ", totalReq=" + totalReq + ", windowExtra=" + windowExtra);
    
            // -8 is for padding below the bottom table
            windowExtra = windowExtra + totalUsed - totalReq - 8;

            // Take out best guess now (before we force another layout) if we should have a window scroll bar
            // -- if we had extra space to dole out, then we fit in the window
            // this.checkWindowScrollbar(false, (windowExtra <= 0));

            var totalScroll = 0;
            // resize tables, doling out any extra space
            for (var id in tableGroup) {
                try {
                    Debug.log("** Table: <b>" + id + "</b>");
                    var tableInfo = this.infoForScrollableTable(id);
                    if (!tableInfo) continue;
                    var rec = htInfo[id];
                    var wrapperTable = tableInfo.wrapperTable;
                    var bodyDiv = tableInfo.body;
                    var maxHeight = rec.maxHt;
                    var minHeight = rec.minHt;

                    var extraWanted = (maxHeight - minHeight);

                    var max = ((windowExtra > 0) && (extraWanted > 0))
                            ? (minHeight + Math.round(extraWanted * 1.0 / totalExtraWanted * windowExtra))
                            : minHeight;
                    var newHeight = Math.min(maxHeight, max);
                    if (tableInfo.maxHeight) {
                        newHeight = Math.min(newHeight, tableInfo.maxHeight);
                    }
                    var bodyHt = newHeight - (wrapperTable.offsetHeight - bodyDiv.offsetHeight);

                    // fix the table body height
                    var hasVertScroll = maxHeight > newHeight; // (div.scrollHeight > div.clientHeight);
                    if (hasVertScroll) totalScroll += (maxHeight - newHeight);
                    Debug.log("newHeight=" + newHeight + ", maxHeight=" + maxHeight + "  (showVertScroll:" + hasVertScroll + ")");


                    // Skip if we're the same as before
                    if (bodyHt != tableInfo.bodyHt || rec.desiredWidth != tableInfo.desiredWidth) {
                        // remember for next time
                        tableInfo.bodyHt = bodyHt;
                        tableInfo.desiredWidth = rec.desiredWidth;

                        var cells = tableInfo.bodyTable.rows[0].cells;
                        var spacerCell = cells[cells.length - 1];
                        var curSpacer = spacerCell.clientWidth;
                        var realScrollWidth = tableInfo.bodyTable.offsetWidth - curSpacer;
                        var visibleWidth = bodyDiv.offsetWidth; // ((UseScrollPad && Dom.hasClass(wrapperTable, "yScroll")) ? ScrollSize : 0)
                        var clippedWidth = realScrollWidth - visibleWidth;
                        var showHorizScroll = (clippedWidth > 2);  // 2 is fudge factor

                        Debug.log("bodyHt (" + id + ") = " + bodyHt);
                        /*
                        Debug.log("realScrollWidth=" + realScrollWidth + ", visibleWidth=" + visibleWidth +", clippedWidth=" + clippedWidth
                            + ", curSpacer=" + curSpacer + "-->" + wrapperTable.className);
                        Debug.log ("Horiz scroll: " +  showHorizScroll + " -- realScrollWidth=" + realScrollWidth + ", offsetWidth=" + tableInfo.wrapperTable.offsetWidth);
                        */

                        bodyDiv.style.height = bodyHt + "px";

                        if (hasVertScroll) {
                            // spacer cell auto-set by yScroll
                            if (!Dom.hasClass(bodyDiv, "yScroll")) {
                                Dom.addClass(bodyDiv, "yScroll");
                            }
                        } else {
                            bodyDiv.scrollTop = 0;
                            if (tableInfo.head) tableInfo.head.scrollTop = 0;
                            Dom.removeClass(bodyDiv, "yScroll");
                        }
                        if (showHorizScroll) {
                            if (!Dom.hasClass(bodyDiv, "xScroll")) {
                                bodyDiv.style.overflowY = "auto";
                                Dom.addClass(bodyDiv, "xScroll");
                                bodyDiv.style.overflowY = "";
                            }
                        }
                        else {
                            bodyDiv.scrollLeft = 0;
                            if (tableInfo.head) tableInfo.head.scrollLeft = 0;
                            Dom.removeClass(bodyDiv, "xScroll");
                        }

                        // Fix min/maximize control
                        if (tableInfo.mmControl) {
                            var newClass = (tableInfo.maximize) ? "awtMMMax" : ((hasVertScroll) ? "awtMMScroll" : "awtMMNone");
                            if (tableInfo.mmControl.className != newClass) tableInfo.mmControl.className = newClass;
                        }

                        //Panel support: set strut width to influence parent size.  If our rows appear to be wrapping, ask for more
                        this.checkWidthStrut(tableInfo, positioningParent, this.desiredWidth(tableInfo));

                        // force a resize to fix heading width
                        this.fixHeadingWidths(tableInfo);
                    }

                    this.tryScrollSet(tableInfo);
                } catch (e) { this.handleDataTableException(e, id); }
            }

            // Panel support: record with positioningParent if we want more space
            Widgets.panelRegChildWants(positioningParent, totalScroll);
        },

        tryScrollSet : function (tableInfo, retry)
        {
            var bodyDiv = tableInfo.body;
            var visRow;
            if (tableInfo.rowIdToForceVisible && (visRow = Dom.getElementById(tableInfo.rowIdToForceVisible))) {
                Debug.log("<b>Force Visible:</b><pre>" + Dom.getOuterHTML(visRow) + "</pre>");

                // if row already visible, do nothing
                var scrollHeight = bodyDiv.offsetHeight;

                if (visRow.offsetTop > bodyDiv.scrollTop
                        && (visRow.offsetTop + visRow.offsetHeight) <= (bodyDiv.scrollTop + scrollHeight)) {
                    Debug.log("Bailing on scroll pos set...:  scrollHeight=" + scrollHeight + ", visRow.offsetTop=" + visRow.offsetTop
                            + ", tableInfo.body.scrollTop=" + bodyDiv.scrollTop);
                    tableInfo.newScrollTop = tableInfo.rowIdToForceVisible = null;
                    return;
                }

                var topRow = this.firstRealRow(tableInfo);
                var topPos = (topRow) ? topRow.offsetTop : 0;
                var bottomRow = this.lastRealRow(tableInfo);
                var bottomPos = (bottomRow) ? bottomRow.offsetTop + bottomRow.offsetHeight : bodyDiv.offsetHeight;

                Debug.log("<b>Row Pos: " + visRow.offsetTop + "</b>, topPos: " + topPos + ", bottomPos: " + bottomPos);

                // goal is to put top of row 40% from top of view
                var scrollTop = Math.floor(visRow.offsetTop - (scrollHeight * 0.4));
                if (scrollTop < topPos) scrollTop = topPos;
                if (scrollTop + scrollHeight > bottomPos) scrollTop = bottomPos - scrollHeight;

                Debug.log("rowIdToForceVisible -- scrollTop: " + scrollTop);

                tableInfo.newScrollTop = scrollTop ? scrollTop : 1;
            }

            if (tableInfo.newScrollTop) {
                bodyDiv.scrollTop = tableInfo.newScrollTop;
                // leave this in place if the scroll didn't take
                // (e.g. when in a panel that goes through multiple sizing phases)
                // if (bodyDiv.scrollTop == tableInfo.newScrollTop) tableInfo.newScrollTop = null;
            }
            if (!retry && (tableInfo.newScrollTop || tableInfo.rowIdToForceVisible)) {
                setTimeout(function() {
                    Datatable.tryScrollSet(tableInfo, true)
                }, 1);
            } else {
                tableInfo.newScrollTop = tableInfo.rowIdToForceVisible = null;
            }
        },

        avgRowHeight : function (tableInfo)
        {
            var ht = tableInfo.bodyTable.offsetHeight;
            var count = tableInfo.bodyTable.rows.length;
            if (tableInfo.topRow) {
                ht -= tableInfo.topRow.offsetHeight;
                count--;
            }
            if (tableInfo.bottomRow) {
                ht -= tableInfo.bottomRow.offsetHeight;
                count--;
            }
            return (count > 0) ? ht / count : 0;
        },

        fixSpacerRow : function (tableInfo, table, row, isTop, fakeCount)
        {
            if (fakeCount > 0) {
                var newRow = null;
                if (!row) {
                    newRow = row = document.createElement('TR');
                    var sampleRow = this.firstRealRow(tableInfo);
                    newRow.className = sampleRow ? sampleRow.className : "tableRow1";
                    row.setAttribute("dr", "1");  // mark as a "real row"
                    var td = document.createElement('TD');
                    td.className = "rowLines";
                    td.innerHTML = "&nbsp";
                    row.appendChild(td);
                    td.colSpan = table.rows[0].childNodes.length;

                } else {
                    // make sure that table edits didn't move us out of position.
                    if ((isTop && (table.tBodies[0].childNodes[1] != row)) || (!isTop && (table.tBodies[0].lastChild != row))) {
                        newRow = row;
                        table.tBodies[0].removeChild(row);
                    }
                }

                if (newRow) {
                    if (isTop && table.rows.length > 1) {
                        table.tBodies[0].insertBefore(newRow, table.rows[1]);
                    } else {
                        table.tBodies[0].appendChild(newRow);
                    }
                }

                // size the cell to show desired number of fake rows
                var size = (fakeCount > 0) ? (fakeCount * _AWTRowHeight) : 0;
                row.firstChild.style.height = size + "px";
            } else {
                if (row) {
                    table.tBodies[0].removeChild(row);
                }
                row = null;
            }
            return row;
        },

        setupScrollFaulting : function (tableInfo)
        {
            Debug.log("****** awtSetupScrollFaulting --- topCount: " + tableInfo.topCount
                    + ", topIndex: " + tableInfo.topIndexId + " = "
                    + Dom.elementIntValue(tableInfo.topIndexId));
            tableInfo.topRow = this.fixSpacerRow(tableInfo, tableInfo.bodyTable,
                    tableInfo.topRow, true, tableInfo.topCount);
            tableInfo.bottomRow = this.fixSpacerRow(tableInfo, tableInfo.bodyTable,
                    tableInfo.bottomRow, false, tableInfo.bottomCount);

            if (tableInfo.continueScroll) {
                //debug("continue scrolling");

                tableInfo.continueScroll = null;
                _AWTPendingScrollInfo = tableInfo;

                /*
                    var faultData = tableInfo.continueScroll;

                    // unjiggle
                    // if we moved up or down and we refaulted in the same direction
                    // AND some of the original content was visible during refault
                    // then pin the old content to refault location
                    if (faultData[AWTFaultIndex_ScrollType] == tableInfo.scrollType &&
                        (faultData[AWTFaultIndex_ScrollType] == "up" ||
                         faultData[AWTFaultIndex_ScrollType] == "down")) {
                        Dom.setElementValue(tableInfo.topIndexId, faultData[AWTFaultIndex_TopRow]);
                        Dom.setElementValue(tableInfo.topOffsetId, faultData[AWTFaultIndex_TopOffset]);
                        tableInfo.bottomIndex = faultData[AWTFaultIndex_BottomRow];
                        tableInfo.bottomOffset = faultData[AWTFaultIndex_BottomOffset];
                    }
                    else {
                        return;
                    }
                */
                return;
            }

            //----------------------  Fix scroll position ------------------------
            // we'll do this on resize....
            if (tableInfo.rowIdToForceVisible) return;

            // if the change that caused the table to be refreshed did not set the reposition
            // to true, then skip repositioning -- ie for grouping / hierarchy toggling, the
            // scroll position values are not posted since the change always occurs below the
            // "parent node" or the node that causes the change and the basic HTML div scrolling
            // handles this correctly and does not modify the scroll position of the "parent node"
            if (!tableInfo.repositionScroll) {
                return;
            }
            tableInfo.repositionScroll = false;


            var topIndex = Dom.elementIntValue(tableInfo.topIndexId);
            var topOffset = Dom.elementIntValue(tableInfo.topOffsetId);
            // Debug.log("topIndex:"+topIndex+ ", topOffset:"+topOffset + ", scrollTop=" + tableInfo.body.scrollTop);

            // optimize for the common case...
            if (topIndex == 0 && topOffset == 0 && tableInfo.body.scrollTop == 0) return;

            var topPos = this.posOfRow(tableInfo, topIndex) - topOffset;
            var bottomPos = null;
            if (tableInfo.bottomIndex) {// && tableInfo.bottomOffset)
                bottomPos = this.posOfRow(tableInfo, tableInfo.bottomIndex, true) - tableInfo.bottomOffset;
                //debug("bottomIndex: "+bottomPos+" "+tableInfo.bottomIndex+" "+tableInfo.bottomOffset);
            }
            // if scroll down --
            //    if the top position is a "virtual row", then pin to lower row position
            //    else pin to upper row position
            // if scroll up --
            //    if the lower position is a "virtual row", then pin to upper row position
            //    else pin to lower row position
            // - scroll up / scroll down checks cover base case of incrementally showing additional
            // rows while keeping visible content stable
            // - "virtual row" checks cover case of faulting in an entire page of content -- no
            // visible content to keep stable, just need to make sure the scroll bar doesn't move
            // This is most visible when scrolling to the very bottom or the very top of a table.

            // Debug.log("Orig tableInfo.body.scrollTop = " + tableInfo.body.scrollTop);

            // if possible / necessary check for cases where we have to pin to bottom row
            if (tableInfo.scrollType &&
                (tableInfo.scrollType == "up" || tableInfo.scrollType == "bottom")) {
                //debug("<font style='color:blue;'>bottom pin: " + tableInfo.bottomIndex + " pos: " + bottomPos + " " + tableInfo.body.offsetHeight+"</font>");
                tableInfo.newScrollTop = bottomPos - tableInfo.body.offsetHeight;
                Debug.log("tableInfo.scrollType = " + tableInfo.scrollType + " -- " + tableInfo.body.scrollTop);
            }
            else {
                //debug("<font style='color:blue;'>top pin: " + Dom.elementIntValue(tableInfo.topIndexId) + " pos: " + this.posOfRow(tableInfo, Dom.elementIntValue(tableInfo.topIndexId))+"</font>");

                // default pin to top row
                tableInfo.newScrollTop = topPos;
                Debug.log("setting toppos = " + topPos);
            }
        },

        checkWindowScrollbar : function (checkWindow, shouldScroll)
        {
            return Dom.checkWindowScrollbar(checkWindow, shouldScroll);
        },


        checkWindowScrollbarOnRefresh : function ()
        {
            this.checkWindowScrollbar(true);
        },

        handleVerticalScroll : function (tableInfo)
        {
            // if we're loading or not scroll faulting, then ignore
            if (_AWTInPostLoad || !tableInfo.scrollFaultActionId || tableInfo.newScrollTop) {
                // Debug.log("<font style='color:blue'>scroll ignored: "+ _AWTInPostLoad + "</font>");
                return;
            }

            // quick out for common case of setting scroll to top, with no fault
            if (tableInfo.topCount == 0 && tableInfo.body.scrollTop == 0) return;

            // if we're already processing, hide the panel
            if (_AWTPendingScroll) {
                // this.scrollHidePanel();
            }

            // set a timer if we don't already have one running
            _awtLastScrollMillis = (new Date()).getTime();
            if (!_awtScrollTimer) _awtScrollTimer = setTimeout(function() {
                Datatable.checkScrollFault(tableInfo);
            }, _AWTScrollDelay);

            // Debug.log("awtHandleVerticalScroll: " + tableInfo.topIndexId + " " + tableInfo.body.scrollTop);
        },

        checkScrollFault : function (tableInfo)
        {
            _awtScrollTimer = null;
            var waited = (new Date()).getTime() - _awtLastScrollMillis;
            if (waited < _AWTScrollDelay) {
                Debug.log("Premature firing! " + waited + " -- rescheduling");
                _awtScrollTimer = setTimeout(function() {
                    Datatable.checkScrollFault(tableInfo);
                }, _AWTScrollDelay - waited);
                return;
            }

            // if we're already processing, cache the last one received
            if (_AWTPendingScroll) {
                Debug.log("continue scroll");
                tableInfo.continueScroll = this.calculateScrollFault(tableInfo);
                this.scrollShowPanel(tableInfo);
                return;
            }
            Debug.log("awtCheckScrollFault!!");
            this.processScrollFault(tableInfo);
        },

        // We want to fire only if:
        //   - the current scroll pos has been maintained for 0.25 sec
        //   - we aren't currently loading (i.e. the scroll is user initiated)
        // If we're in the midst of a a request and user continues to scroll, then
        //   - cache the last scroll to and fire it when we're done with the current one.
        // Note: If we're loading and the scroll bar is moved by the end user, we still end up
        //     "losing" the user initiated scroll ... How to differentiate?
        
        processScrollFault : function (tableInfo)
        {
            // if we're loading or already processing cancel
            if (_AWTInPostLoad || _AWTPendingScroll) {
                // Debug.log("<font color='green'>race fixed</font>");
                return;
            }

            var faultData = this.calculateScrollFault(tableInfo);
            Debug.log("Datatable.ProcessScrollFault() -- processing... TopRow: " + faultData[AWTFaultIndex_TopRow] + ", pos=" + tableInfo.body.scrollTop);

            // always push offset data, so the scroll pos is preserved if the user leaves this page and comes back
            Dom.setElementValue(tableInfo.topIndexId, faultData[AWTFaultIndex_TopRow]);
            Dom.setElementValue(tableInfo.topOffsetId, faultData[AWTFaultIndex_TopOffset]);

            Dom.setElementValue(tableInfo.leftPosId, tableInfo.body.scrollLeft);

            // clear any stale scroll type
            delete tableInfo.scrollType;

            if (faultData[AWTFaultIndex_Position] != 0) {
                if (_AWTPendingScroll) {
                    // Debug.log("<font color='red'>RACE CONDITION</font>");
                    return;
                }

                // Debug.log("fault type:" + faultData);

                // store the bottom index on the client side
                tableInfo.bottomIndex = faultData[AWTFaultIndex_BottomRow];
                tableInfo.bottomOffset = faultData[AWTFaultIndex_BottomOffset];

                tableInfo.scrollType = faultData[AWTFaultIndex_ScrollType];

                tableInfo.repositionScroll = true;

                this.scrollFaultAction(tableInfo);
            }
            else {
                this.scrollHidePanel();
            }
        },

        scrollFaultAction : function (tableInfo)
        {
            // fire request to server
            _AWTPendingScroll = true;
            var form = Dom.getElementById(tableInfo.topIndexId).form;
            //debug("<font style='color:red'>Calling awsenderClicked: " + form.id + "</font>");
            this.scrollShowPanel(tableInfo);
            // disable the WaitCursor () while waiting for scroll fault to complete
            Input.setShowWaitCursorDisabled(true);
            // In case our table postLoad doesn't get called (perhaps because of an aborted request, then this
            // should still get called
            Event.registerUpdateCompleteCallback(function() {
                if (_AWTPendingScroll) {
                    Debug.log("--> SCROLL FAULT UPDATE COMPLETE");
                    _AWTPendingScroll = false;
                    this.scrollHidePanel();
                    // Make sure we're still in a real data range
                    setTimeout(function() {
                        tableInfo = this.infoForTable(tableInfo.bodyTable.id);
                        this.handleVerticalScroll(tableInfo);
                    }.bind(this), 10);
                };
            }.bind(this));
            Request.senderClicked(tableInfo.scrollFaultActionId, form.id, null, null, null, null);
        },

        firstRealRow : function (tableInfo)
        {
            var rows = tableInfo.bodyTable.rows;
            var index = (tableInfo.topRow) ? 2 : 1;  // ship spacer + topRow (if present)
            return (index < rows.length) ? rows[index] : null;
        },

        lastRealRow : function (tableInfo)
        {
            var rows = tableInfo.bodyTable.rows;
            var lastIndex = rows.length - 1;
            var index = (tableInfo.bottomRow) ? lastIndex - 1 : lastIndex;  // skip bottom fault
            return (index > 0) ? rows[index] : null;
        },

        calculateScrollFault : function (tableInfo)
        {
            var faultData = new Array(AWTFaultIndex_size);

            var div = tableInfo.body;
            var scrollTop = div.scrollTop;

            var rows = tableInfo.bodyTable.rows;
            var scrollHeight = div.offsetHeight;
            // debug current scroll position and the height of the viewport
            //debug("<pre>    Scroll:  pos=" + scrollTop + ", scrollHeight=" + scrollHeight + "</pre>");
            var i;
            var rowCount = 0, topRow = -1, topOffset, bottomRow = -1, bottomOffset;
            var lastRowIndex = rows.length - 1;
            for (i = 0; i <= lastRowIndex; i++) {
                var row = rows[i];
                if (row.getAttribute("dr") == "1") {  // data row
                    rowCount++;

                    var topPos = row.offsetTop - scrollTop;
                    // seek to the last non-data child row to find the "bottom" of this row
                    //var debugDataRowIndex = i;
                    //var debugDataRowBottomPos = topPos + row.offsetHeight;
                    while (i + 1 <= lastRowIndex && rows[i + 1].getAttribute("dr") != "1") i++;
                    var bottomPos = rows[i].offsetTop - scrollTop + rows[i].offsetHeight;

                    // the y coord of each row (top / bottom)
                    //debug("&nbsp;&nbsp;&nbsp;row "+rowCount+
                    //      " rowindex="+debugDataRowIndex+"/"+ i + " pos=" + topPos +
                    //      ", data row bottom=" + debugDataRowBottomPos +", bottom=" + bottomPos);

                    // find first visible row -- and how much of the row is above the viewport
                    if (topRow == -1 && ((topPos >= 0) || (bottomPos >= 0))) {
                        topRow = rowCount;
                        topOffset = topPos;
                        //debug("top row: " + topRow + " offset: " + topOffset);
                    }
                    // and last visible row -- and how much of the row is below the viewport
                    if (bottomRow == -1 && ((bottomPos >= scrollHeight) || (i == lastRowIndex))) {
                        bottomRow = rowCount;
                        bottomOffset = topPos - scrollHeight;
                        //debug("bottom row: " + bottomRow + " offset: " + bottomOffset);
                    }
                }
            }

            //debug ("&nbsp;&nbsp;&nbsp;top index:" + topRow + ", offset=" + topOffset + " (rows.length=" + rows.length + ")");
            //debug ("&nbsp;&nbsp;&nbsp;bottom index:" + bottomRow + ", offset=" + bottomOffset);
            //debug ("&nbsp;&nbsp;&nbsp;topRow=" + tableInfo.topRow + ", bottomRow=" + tableInfo.bottomRow);
            //debug (Debug.printProperties(tableInfo.bodyTable));

            var topFaultExposed = false;
            var bottomFaultExposed = false;
            var topSpacerCount = (tableInfo.topRow) ? 2 : 1;
            var topCount = tableInfo.topCount;
            if (tableInfo.topRow && (topRow <= 1)) {
                // any part of top fault exposed
                topFaultExposed = true;
                faultData[AWTFaultIndex_ScrollType] = "up";
                if (topRow == 1) {
                    // figure out where in the top spacer area we are
                    if (topOffset > 0) {
                        topRow = 0;
                    } else {
                        topRow = Math.floor(-1 * topOffset / _AWTRowHeight);
                        topOffset += (topRow * _AWTRowHeight);
                        topFaultExposed = true;
                    }
                }

                if (tableInfo.topRow && bottomRow <= 1) {
                    //debug("<font style='color:blue'>top</font>"+topRow);
                    faultData[AWTFaultIndex_ScrollType] = "top";
                }
                else {
                    bottomRow += topCount - topSpacerCount;
                }
            }
            else if (tableInfo.bottomRow && (topRow == rowCount)) {
                // completely in bottom fault area

                faultData[AWTFaultIndex_ScrollType] = "bottom";

                // figure out where in the bottom spacer area we are
                var fakeRow = Math.floor(-1 * topOffset / _AWTRowHeight);
                topRow = topCount + rowCount - topSpacerCount + fakeRow;
                topOffset += (fakeRow * _AWTRowHeight);
                //debug("<font style='color:blue'>bottom area </font>");

                bottomFaultExposed = true;

                fakeRow = Math.floor(-1 * bottomOffset / _AWTRowHeight);
                bottomRow = topCount + rowCount - topSpacerCount + fakeRow;
                //debug("topCount: " + topCount + " rowCount: " + rowCount + " topSpacerCount: " + topSpacerCount +
                //      " fakeBottomRow: " + fakeRow + " bottomOffset: " + bottomOffset);
                bottomOffset += (fakeRow * _AWTRowHeight);
            } else if (topRow != 0) {
                topRow += (topCount - topSpacerCount);
            }

            if (tableInfo.bottomRow && (bottomRow == rowCount)) {
                bottomFaultExposed = true;
                if (faultData[AWTFaultIndex_ScrollType] != "bottom") {
                    faultData[AWTFaultIndex_ScrollType] = "down";

                    // use top row for positioning
                    bottomRow = null;
                    bottomOffset = 0;
                }
            }

            //debug("--- fault exposed: " + topFaultExposed + "/" + bottomFaultExposed +
            //      " scrollType: " + tableInfo.scrollType +
            //      " topIndex: " + topRow + " topOffset: " + topOffset +
            //      " bottomIndex: " + bottomRow + " bottomOffset: " + bottomOffset);

            //    Dom.setElementValue(tableInfo.topIndexId, topRow);
            //    Dom.setElementValue(tableInfo.topOffsetId, topOffset);

            faultData[AWTFaultIndex_TopRow] = topRow;
            faultData[AWTFaultIndex_TopOffset] = topOffset;

            // store the bottom index on the client side
            //    tableInfo.bottomIndex = bottomRow;
            //    tableInfo.bottomOffset = bottomOffset;
            faultData[AWTFaultIndex_BottomRow] = bottomRow;
            faultData[AWTFaultIndex_BottomOffset] = bottomOffset;

            //debug("&nbsp;&nbsp;&nbsp;Scroll - top row:" + topRow + ", offset=" + topOffset);
            //debug("&nbsp;&nbsp;&nbsp;       - bot row:" + bottomRow + ", offset=" + bottomOffset);
            //debug("&nbsp;&nbsp;&nbsp;topFaultExposed=" + topFaultExposed + ", bottomFaultExposed=" + bottomFaultExposed);
            faultData[AWTFaultIndex_Position] = topFaultExposed ? -1 : (bottomFaultExposed ? 1 : 0);
            return faultData;
        },

        posOfRow : function (tableInfo, logicalRowNum, bottom)
        {
            var topCount = tableInfo.topCount;
            if (logicalRowNum < topCount) {
                // in the top area
                return (logicalRowNum * _AWTRowHeight);
            } else {
                // how many real rows do we need to count?
                logicalRowNum = logicalRowNum - topCount + (tableInfo.topRow ? 2 : 1);

                // map the logicalRowNum to a physical row index (skipping "non-real rows").
                var rows = tableInfo.bodyTable.rows;
                var i;
                for (i = 0; i < rows.length; i++) {
                    var row = rows[i];
                    if (row.getAttribute("dr") == "1") {  // data row
                        logicalRowNum--;
                    }
                    if (logicalRowNum <= 0) {
                        return row.offsetTop;
                    }
                }

                // we ran out of rows so either we're running bottom calc and we're at an edge
                if (bottom) {
                    var row = rows[rows.length - 1];
                    return row.offsetTop + row.offsetHeight;
                }
                else {
                    // or we're running top calc and we're in the footer
                    return rows[rows.length - 1].offsetTop + ((logicalRowNum - 1) * _AWTRowHeight);
                }
            }
        },

        scrollShowPanel : function (tableInfo)
        {
            Input.disableInput(false);

            // On firefox, showing the panel can cause a scroll reset, so remember the position so we can restore
            var e = tableInfo.body, sx = e.scrollLeft, sy = e.scrollTop;
            ScrollPanelTimeoutId = setTimeout(function() {
                ScrollPanelTimeoutId = null;
                Widgets.showPanel("awtFaultingPanel", tableInfo.body, true);
                if (sx || sy) {
                    e.scrollLeft = sx;
                    e.scrollTop = sy;
                }
            }.bind(this), 2000);

        },

        scrollHidePanel : function ()
        {
            if (ScrollPanelTimeoutId) clearTimeout(ScrollPanelTimeoutId);
            ScrollPanelTimeoutId = null;
            Widgets.hidePanel("awtFaultingPanel");
            Input.enableInput();
        },

        rowSibling : function (row, dir) {
            // on firefox the sibbling of a row could be a TEXT, so skip these to get to the next TR
            do {
                row = (dir > 0) ? row.nextSibling : row.previousSibling;
                // Debug.log("row sibling (" + i++ + "): " + row + " - " + (row ? row.tagName : row));
            } while (row && row.tagName != "TR");
            return row;
        },

        addRowStyle : function (style, row, skipDetail)
        {
            do {
                if (row.className && row.className.indexOf(style) == -1) {
                    row.className = style + row.className;
                }
                row = this.rowSibling(row, 1);
            }
            while (row && !this.isPrimaryRow(row) && !skipDetail);
        },

        removeRowStyle : function (style, row)
        {
            do {
                this.removeClassPrefix(row, style);
                row = this.rowSibling(row, 1);
            }
            while (row && !this.isPrimaryRow(row));
        },

        removeClassPrefix : function (n, className) {
            // remove hover style, if present
            var curName = n.className;
            if (curName && curName.substring(0, className.length) == className) {
                n.className = curName.substring(className.length, curName.length);
            }
        },

        isPrimaryRow : function (row)
        {
            // cached value
            var isPrim = row.getAttribute("_AWTIsPrimaryRow");
            if (isPrim) return isPrim == "1";

            if (row.getAttribute("dr") == "1") {
                // it's marked as primary data row
                row.setAttribute("_AWTIsPrimaryRow", "1");
            }
            else if ((row.cells && row.cells.length > 0 && row.className &&
                      row.className.indexOf("firstRow") == -1) ||
                     this.rowSelectElement(row)) {
                // it's primary if the row has topline or if it has a select column
                row.setAttribute("_AWTIsPrimaryRow", "1");
            }
            else {
                // test for real first row
                row.setAttribute("_AWTIsPrimaryRow", "1");

                var prevRow = this.rowSibling(row, -1);
                if (!((prevRow && prevRow.cells && prevRow.cells.length > 0 && prevRow.cells[0].nodeName == "TH") ||
                      (prevRow && prevRow.className && prevRow.className.indexOf("AWTColAlignRow") != -1))) {
                    row.setAttribute("_AWTIsPrimaryRow", "0");
                }
            }

            return row.getAttribute("_AWTIsPrimaryRow") == "1";
        },

        rowForChild : function (target, checkCurrent)
        {
            var row = Dom.findParentUsingPredicate(target, function(n) {
                return n.nodeName == "TR" && n.parentNode.parentNode.className == "tableBody";
            }, checkCurrent);

            var prev;
            while (row && !this.isPrimaryRow(row) && (prev = this.rowSibling(row, -1))) {
                row = prev;
            }

            return row;
        },

        tableInfoForRow : function (row)
        {
            var bodyTable = Dom.findParentUsingPredicate(row, function(n) {
                return n.nodeName == "TABLE" && n.className.indexOf("tableBody") != -1;
            });
            return this.infoForTable(bodyTable.id);
        },
        //
        // Event Handlers
        //

        mouseOverEventHandler : function (evt)
        {
            var evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;
    // Debug.log(evt.type + " - " + target.tagName);

            var parentRow = this.rowForChild(target);

            if (parentRow) {
                // Debug.log(" - in: " + parentRow.id);
                if (!this.isRowSelected(parentRow)) {
                    if (!DragDrop.getDiv()) {
                        this.addRowStyle(_AWTHoverStyle, parentRow);
                    }
                }
            }
        },

        mouseOutEventHandler : function (evt)
        {
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;
    // Debug.log(evt.type + " - " + target.tagName);

            var parentRow = this.rowForChild(target);

            if (parentRow) {
                //debug(" - out: " + parentRow.id);
                if (DragDrop.getDiv()) {
                    this.removeRowStyle(_AWTDragHoverStyle, parentRow);
                }
                else {
                    this.removeRowStyle(_AWTHoverStyle, parentRow);
                }
            }
        },

        rowClickedEventHandler : function (evt)
        {
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;
            var handled = false;

            // Debug.log("Row clicked: " + evt.type + " - " + target.tagName + " " + target.className);
            var row = this.rowForChild(target, true);
            if (row && _awtMouseDown) {
                var tableInfo = this.tableInfoForRow(row);
                var targetTable = tableInfo.bodyTable;
                var isSelected = this.isRowSelected(row);
                var selectElement = this.rowSelectElement(row);

                if (tableInfo.disableRowSelection || _awtSelectStart || target.tagName == "TEXTAREA") {
                    _awtSelectStart = false;
                    handled = false;
                }
                    // start mouse and end click on an input field
                    // follows behavior of IE -- if mouse down outside of checkbox and mouse up
                    // inside, then checkbox does not get selected
                    // also keeps disables row selection for other input fields (textfield, etc)
                else if (this.isSelectElement(target) ||
                         (_awtMouseDownTarget && _awtMouseDownTarget.nodeName == "INPUT" &&
                          target.tagName == "INPUT")) {
                    var force = _awtMouseDownTarget ? this.isSelectElement(_awtMouseDownTarget) : true;
            // Note: using isSelected here since the checkbox should have already changed
                    // states by the time mouse click event bubbles to the table
                    this.setRowSelect(row, isSelected, force, tableInfo);
                    this.updateSelectAll(tableInfo);
            // still need to allow default event handling so "check" appears
                    handled = false;
                }
                else if (target.tagName == "A") {
                    // for Sourcing / default <a href=""> action.  The standard AWHyperLink
                    // registers an onclick handler and does not reach this event handler.
                    handled = false;
                }
                else if (selectElement && !selectElement.disabled) {
                    var selectVisible = selectElement && Dom.isVisible(selectElement);
                    var index = Dom.findRowIndex(row.id, targetTable);
                    var isMultiSelect = this.isMultiSelect(row);
                    var stateChanged = false;

                    if (isMultiSelect) {
                        if (evt.shiftKey) {
                            this.clearSelection(tableInfo);
                            if (tableInfo.lastSelectionIndex != -1) {
                                this.setSelection(targetTable, tableInfo.lastSelectionIndex, index, tableInfo);
                                stateChanged = true;
                            }
                            else {
                                tableInfo.lastSelectionIndex = index;
                            }
                        } else {
                            var done = false;
                            if (!evt.ctrlKey && !selectVisible) {
                                // if we're clicking on the lone selected row then this is a no-op
                                if (isSelected && this.selectionCount(tableInfo)[1] == 1)  done = true;
                                else this.clearSelection(tableInfo);
                            }
                            if (!done) {
                                this.setRowSelect(row, !isSelected, false, tableInfo);
                                this.updateSelectAll(tableInfo);
                                tableInfo.lastSelectionIndex = index;
                                stateChanged = true;
                            }
                        }
                    } else {
                        // single select
                        if (!isSelected) {
                            this.clearSelection(tableInfo);
                            this.setRowSelect(row, true, false, tableInfo);
                            tableInfo.lastSelectionIndex = index;
                            stateChanged = true;
                        }
                    }

                    // Fire click event handler on selection control (if any)
                    if (stateChanged) {
                        if (selectElement) Event.elementInvoke(selectElement, "click");
                    }

                    handled = true;
                }
            }
            this.clearMouseDown();

            // prevent propagation
            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        clearSelection : function (tableInfo) {
            var rows = tableInfo.bodyTable.rows;
            for (var i = 0; i < rows.length; i++) {
                this.setRowSelect(rows[i], false, false, tableInfo);
            }
            this.updateSelectAll(tableInfo);
        },

        setSelection : function (table, start, end, tableInfo)
        {
            var rows = table.rows;
            if (start > end) {
                var tmp = end;
                end = start;
                start = tmp;
            }
            for (var i = start; i <= end; i++) {
                if (this.isPrimaryRow(rows[i])) {
                    this.setRowSelect(rows[i], true, false, tableInfo);
                }
            }
            this.updateSelectAll(tableInfo);
        },

        updateRowSelectColor : function (row, yn)
        {
            // sync row color
            if (yn) {
                this.addRowStyle(_AWTSelectStyle, row);
            } else {
                // remove style name
                this.removeRowStyle(_AWTSelectStyle, row);
                this.removeRowStyle(_AWTHoverStyle, row);
            }
        },
        // return count of [total, selected]
        selectionCount : function (tableInfo) {
            var rows = tableInfo.bodyTable.rows;
            var selectAll = this.selectAllForTable(tableInfo);

            var total = 0, selected = 0;
            for (var i = 0; i < rows.length; i++) {
                if (rows[i].cells) {
                    var n = this.rowSelectElement(rows[i]);
            // n is null for the padding row and "empty table" message row
                    if (n && n != selectAll) {
                        total++;
                // n can be equal selectAll in a non scrolling table
                        if (n.checked) selected++;
                    }
                }
            }
            return [total, selected];
        },

        updateSelectAll : function (tableInfo)
        {
            var selectAll = this.selectAllForTable(tableInfo);

            if (selectAll && !selectAll.checked) {
                var selCount = this.selectionCount(tableInfo);

                if (selCount[0] > 0 && selCount[0] == selCount[1]) {
                    if (tableInfo.topRow || tableInfo.bottomRow) {
                        //  ask the server to update the select all checkbox
                        Request.senderClicked(tableInfo.updateSelectAllActionId,
                                selectAll.form.id, null, null, null, null);
                    }
                    else {
                        // all the rows are on the client, so just update on the client.
                        selectAll.checked = true;
                    }
                }
            }
        },

        // lookup select all checkbox
        selectAllForTable : function (tableInfo)
        {
            var selectAll;
            if (tableInfo.headTable) {
                selectAll = this.rowSelectElement(tableInfo.headTable.rows[1], true);
            }
            if (!selectAll) {
                // non scrolling table case
                selectAll = this.rowSelectElement(tableInfo.bodyTable.rows[0], true);
            }
            return selectAll;
        },

        // gets called when we need to force refresh the selection column
        updateSelections : function (tableId)
        {
            var tableInfo = this.infoForTable(tableId);
            return this._updateSelections(tableInfo);
        },

        _updateSelections : function (tableInfo)
        {
            Debug.log("_awtUpdateSelections");
            if (!tableInfo.disableRowSelection) {
                var bodyTable = tableInfo.bodyTable;
                for (var i = 0; i < bodyTable.rows.length; i++) {
                    var row = bodyTable.rows[i];
                    // call awtUpdateRowSelectColor only for primary rows.
                    // the child rows will get updated by it.
                    if (this.isPrimaryRow(row)) {
                        var isSelected = this.isRowSelected(row, true);
                        this.updateRowSelectColor(row, isSelected);
                    }
                }
            }
        },

        setRowSelect : function (row, yn, force, tableInfo)
        {
            var n = this.rowSelectElement(row);
            if (n && n.disabled) {
                if (force) {
                    n.checked = yn;
                    this.updateRowSelectColor(row, yn);
                }
                return;
            }

            var isMultiSelect = (n && n.tagName == "INPUT" && n.type == "checkbox");
            if (!isMultiSelect) {
                if (!tableInfo) {
                    tableInfo = this.tableInfoForRow(row);
                }

                if (tableInfo) {
                    if (tableInfo.lastSelectedRow) {
                        this.updateRowSelectColor(tableInfo.lastSelectedRow, false);
                    }
                    tableInfo.lastSelectedRow = row;
                }
            }
            else {
                var selectAll = this.selectAllForTable(tableInfo);

                if (!yn && selectAll && selectAll.checked) {
                    // make sure we unselect the select all icon since we're unselecting one of
                    // the multi-select rows
                    selectAll.checked = false;
                }
            }

            if (n && (n.checked != yn || force)) {
                // change checkbox
                n.checked = yn;
                this.updateRowSelectColor(row, yn);
            }

        },

        isSelectElement : function (node)
        {
            return node.tagName == "INPUT" && (node.type == "checkbox" || node.type == "radio");
        },

        rowSelectElement : function (row, refresh)
        {
            //  The selection checkbox can be incrementally updated,
            //  so need to refresh the cached reference in this case.
            if (refresh) {
                // invalidate cache
                row._AWTSelectElement = null;
            }
            // return the checkbox, if we can find it
            if (row._AWTSelectElement) {
                // return cached value if available
                return (row._AWTSelectElement != AWNullValue) ? row._AWTSelectElement : null;
            }
            var select = null;
            var selectCell = row.cells ? row.cells[AWTSelectColumnIndex] : null;
            if (selectCell && selectCell.childNodes) {
                select = Dom.findChildUsingPredicate(selectCell, this.isSelectElement.bind(this));
            }

            row._AWTSelectElement = (select != null) ? select : AWNullValue;
            return select;
        },

        isRowSelected : function (row, refresh)
        {
            var n = this.rowSelectElement(row, refresh);
            return n ? n.checked : false;
        },

        isMultiSelect : function (row)
        {
            var child = this.rowSelectElement(row);
            if (child) {
                if (child.tagName == "INPUT" && child.type == "checkbox") {
                    return true;
                }
            }
            return false;
        },

        mouseDownEvtHandler : function (evt)
        {
            // NOTE: debug in mouse up/down causes with mouse click event to not fire
            //debug("mouse down");
            if (DragDrop.shouldHandleMouseDown(evt)) return false;
            var handled = false;
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;

            // set up for possible on click
            _awtMouseDown = true;
            _awtMouseDownTarget = target;

            // track for text selection
            _awtSelectStart = false;

            // if the target is an input field or a textarea and does not have the force drag
            // enabled, then assume drag is disabled
            if ((target.tagName == "INPUT" || target.tagName == "TEXTAREA") &&
                target.className.indexOf(AWTForceDragStyle) == -1) {
                if (handled) {
                    Event.cancelBubble(evt);
                }
                return !handled;
            }

            var row = this.rowForChild(target);

            //debug(row.className);
            if (row) {
                var tableInfo = this.tableInfoForRow(row);

                if (tableInfo.isDraggable &&
                    row.className && row.className.indexOf(AWTableDragPrefix) != -1) {

                    //debug("selectable TD " + target.id + " " + row.id);
                    var rowId = row.id;
                    if (!Util.isNullOrUndefined(rowId) && rowId != "") {

                        var dragDiv = DragDrop.createDragDiv(evt, row, AWTableDragPrefix);
                        if (!dragDiv) {
                            // if no dragDiv, then there must have been a drag already occurring
                            // clean up datatable drag state
                            this.removeRowStyle(_AWTDragHoverStyle, row);                            
                        }
                        else {
                            dragDiv.style.border = "1px #333333 solid";
                            dragDiv.style.width = row.scrollWidth;
                            var tmpTable = row.parentNode.parentNode.cloneNode(false);
                            var tmpTBody = document.createElement("tbody");
                            tmpTBody.appendChild(row.cloneNode(true));
                            tmpTable.appendChild(tmpTBody);
                            dragDiv.appendChild(tmpTable);

                            // change table styles based on droppable status
                            dragDiv.droppable = function (isDroppable) {
                                if (isDroppable) {
                                    dragDiv.style.border = "1px #333333 solid";
                                }
                                else {
                                    dragDiv.style.border = "1px red solid";
                                }
                            }

                            dragDiv.nextSrcId =
                            target.parentNode.nextSibling ? target.parentNode.nextSibling.id : null;
                        }

                        handled = true;
                    }
                }
            }

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        mouseUpEvtHandler : function (evt)
        {
            // NOTE: debug in mouse up/down causes with mouse click event to not fire
            //debug("mouse up");

            var handled = false;

            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;
            var row;
            var dragDiv = DragDrop.getDiv();

            if (dragDiv && (row = this.rowForChild(target))) {

                // hide mouse drag icon
                //this.hideMouseDragIcon();
                var targetRowId = row.id;

                //debug("mouse up: " + dragDiv.srcId + " " + dragDiv.nextSrcId + " " + targetRowId);
                // Debug.log("dragDiv.srcId=" + dragDiv.srcId + ",  targetRowId=" + targetRowId + ", dragDiv.nextSrcId=" + AWDragDiv.nextSrcId);

                // if we're on the same row, then let onclick handle the event
                if (dragDiv.srcId == targetRowId) {
                    // but not if
                    if (DragDrop.dragActive()) {
                        handled = true;
                    }
                    this.removeClassPrefix(Dom.getElementById(dragDiv.srcId), _AWTHoverStyle);
                    DragDrop.releaseDragDiv();
                }
                else if (dragDiv.srcId != targetRowId && dragDiv.nextSrcId != targetRowId) {
                    //debug("handling data table drop: " + row.id);
                    this.removeRowStyle(_AWTDragHoverStyle, row);
                    handled = DragDrop.handleDropAction(row, evt, AWTableDropPrefix);
                }
                if (handled) {
                    this.clearMouseDown();
                }
            }

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        clearMouseDown : function ()
        {
            _awtMouseDown = false;
            _awtMouseDownTarget = null;
        },

        mouseMoveEvtHandler : function (evt)
        {
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;
            var handled = false;
            var dragDiv = DragDrop.getDiv();
            //debug("mouse move " + dragDiv);

            if (dragDiv && DragDrop.dragDivMoved(evt)) {
                //debug("dragging");
                var dropContainer = DragDrop.findDropContainer(target, AWTableDropPrefix);

                if (dropContainer && DragDrop.isDropContainerValid(dropContainer, AWTableDropPrefix)) {
                    //this.showMouseDragIcon();
                    //this.updateMouseDragIcon(evt);

                    // set droppable since this may have been set to false by the "superclass"
                    // ie, document level awMouseMoveEvtHandler
                    dragDiv.droppable(true);

                    // update position of drag div
                    // The 1 pixel is to avoid mouse events from firing on the drag div
                    dragDiv.style.left = (evt.clientX + document.body.scrollLeft + 1) + "px";
                    dragDiv.style.top = (evt.clientY + Dom.getPageScrollTop() + 1) + "px";

                    var parentRow = this.rowForChild(target);
                    if (parentRow) {
                        // "insert" indicator in mouse over row (ie apply style)
                        if (!this.isRowSelected(parentRow)) {
                            var rowId = parentRow.id;
                            // skip selected row and the next row since we're doing insert before)
                            if (rowId != dragDiv.srcId && rowId != dragDiv.nextSrcId) {
                                this.addRowStyle(_AWTDragHoverStyle, parentRow, true);
                            }
                        }
                    }

                    handled = true;
                }
            }

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        selectStartEvtHandler : function (evt)
        {
            //debug("awtSelectStartEvtHandler");
            // used to avoid selecting background text as we're dragging
            // ### IE only.
            // for Netscape,  use the style
            //  .styleName {
            //      -moz-user-select: none;
            //  }
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;

            // if drag then disable selection
            // if shift/ctrl down, then assume we're doing row selection
            var handled = !!(DragDrop.getDiv() || evt.shiftKey || evt.ctrlKey);

            // track text selection
            _awtSelectStart = !handled;

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        showMouseDragIcon : function ()
        {
            if (!_awtMouseDragIndicator) {
                _awtMouseDragIndicator = Dom.getElementById("AWDragImage");
            }
            //debug("drag icon: " + dragType + " " + _mouseIcn + " " + AWDragImage);
            _awtMouseDragIndicator.style.visibility = "visible";
        },

        updateMouseDragIcon : function (evt)
        {
            var x = evt.clientX + document.body.scrollLeft;
            var y = evt.clientY + Dom.getPageScrollTop();

            _awtMouseDragIndicator.style.left = x + 10;
            _awtMouseDragIndicator.style.top = y + 10;
        },


        hideMouseDragIcon : function ()
        {
            if (_awtMouseDragIndicator) {
                _awtMouseDragIndicator.style.top = 0;
                _awtMouseDragIndicator.style.left = 0;
                _awtMouseDragIndicator.style.visibility = "hidden";
            }
        },

        EOF:0};


    // Check scrollbar on resize or refresh
    Event.registerRefreshCallback(function() {
        Event.eventEnqueue(Datatable.checkWindowScrollbarOnRefresh.bind(Datatable));
    });
    Event.registerOnWindowResize(Datatable.checkWindowScrollbarOnRefresh.bind(Datatable));

    return Datatable;
}();
