/*
    Refresh.js      -- Incremental page update for AW responses

    Responses from the application (via Request.js) call back to routines here to
    incrementally update portions of the DOM and update (pseudo) backtrack history.
    Includes necessary support for deferred (properly enqueued) inline JavaScript and
    incremental loading of .js files.
*/

ariba.Refresh = function() {
    // imports
    var Util = ariba.Util;
    var Event = ariba.Event;
    var Input = ariba.Input;
    var Debug = ariba.Debug;
    var Request = ariba.Request;
    var Dom = ariba.Dom;

    // private vars
    var AWDELETE = "d";
    var AWINSERT = "i";

    // [elementid][delete] = array of sourceId
    //            [insert] = array of {predecessorId, sourceId}
    var AWDomSyncData = new Object();
    var AWDomScopeUpdateList = new Object();

    // performance
    var AWRefreshStartTime;
    var AWRefreshTrace;
    var AWMaxRefreshTraceLength = 10;
    var AWWindowLoadStartTime;
    var AWWindowOnLoad = false;
    var _RunningIncrementalAction = false;
    var _LoadedJSStrs = [];
    
    // Can be disabled by AWSafeClientSideScript if ActiveX system failed to initialize
    var VBSEnqueue = false;
    var VBSArray = null;

    var _LoadedJS = new Array();

    var AWHandlingNewRequest = false;
    var AWHandlingTrackRequest = false;
    var AWHistoryDebugString;
    var AWHistoryBack = 0;
    var AWHistoryForward = 0;
    var AWHistoryLimit = 2;
    var _CheckLocInterval = null;
    var _LocationCheckActive = false;
    
    // Handler called with IE History IFrame is loaded
    var _historyHandler = function () {};


    // Incremental request completion is indicated by the refreshRequestComplete()
    // callback from the incremental update content.  In addition, there is expected to
    // be a DOM element (typically the script tag itself) which has an id equal
    // to "AWRefreshComplete".
    // The refreshRequestComplete() method / "AWRefreshComplete" element are used to
    // determine if the refresh request completed successfully.  Post processing of the
    // request results is handled by awCompleteRequest and awWindowOnLoad.
    //
    // To initiate incremental update completion verification, startRefreshTimer()
    // should be called when the request is initiated.
    // -- awStartRefreshTimer is called when the request is initiated.
    //    awStartRefreshTimer calls awCheckRequestComplete with delay.
    // -- awCheckRequestComplete spins until either
    // a) a document exists in the iframe and the URL of the iframe changes from the
    //    default AWEmptyDocScriptlet value
    // b) an exception is thrown
    //    this can occur if the webserver is not available and IE / NS inserts a
    //    "Action canceled" page or the info.netscape.com page
    // If a) occurs, then awCheckDocumentLoad is called.  For IE, we can use the
    // readyState to make sure the document is fully loaded.  For NS, we wait 2 seconds
    // to make sure the document is fully loaded.
    // -- awCheckDocumentLoad looks for the AWRefreshComplete element on the document. If
    // this is not found, then we can assume that a 404 or some other webserver error
    // page was returned and we call handleRequestError().
    // -- awHandleRequestError repositions and resizes the AWRefreshFrame to the full
    // size of the window.
    
    var Refresh = {
        // Public Globals
        AWShowHistoryFrame : false,
        AWBackTrackUrl : '',
        AWForwardTrackUrl : '',
        AWMarkRefreshRegions : false,

        _refreshTable : function (poSource, poTarget)
        {
            // pick up the scratch div
            var tmpDiv = document.createElement("div");
            if (Dom.IsIE) {
                tmpDiv.innerHTML = poSource.outerHTML;
            } else {
                tmpDiv.innerHTML = "<table>" + poSource.innerHTML + "</table>";;
                tmpDiv.firstChild.id = poSource.id;
            }
            var tmpTable = tmpDiv.firstChild;
            this.replaceRows(tmpTable, poTarget);
        },

        _scopeUpdate : function (poSource)
        {
            // pick up the scratch div
            var tmpDiv = document.createElement("div");
            tmpDiv.innerHTML = Dom.getOuterHTML(poSource);

            var target = Dom.getElementById(poSource.id);
            if (target) {
                Dom.setOuterHTML(target, tmpDiv.innerHTML);
            //debug("done ScopeUpdate: " + Util.htmlEscapeValue(Dom.getOuterHTML(target)));
            }
            else {
                alert("scopeUpdate target not found: " + poSource.id);
            }
        },

        replaceRows : function (poSourceTable, poTargetTable)
        {
            var targetTBody = Dom.findChild(poTargetTable, "TBODY");
            var sourceRows = Dom.findChild(poSourceTable, "TBODY").childNodes;
            // [elementid][delete] = array of sourceId
            //            [insert] = array of {predecessorId, sourceId}
            var elementDomSyncData = this.findDomSyncElementData(poSourceTable.id);
            var i, target;
            // create hash of source rows to avoid N^2
            /*
            var sourceById = {};
            var j = sourceRows.length;
            while (j--) {
                var r = sourceRows[j];
                sourceById[r.id] = r;
            }
            */
            Debug.log("replaceRows -- table: " + poSourceTable.id + ", count: " + sourceRows.length);
            //Debug.printProperties(elementDomSyncData);

            // insertions
            // ----------
            var insertArray = elementDomSyncData[AWINSERT];
            for (i = 0; insertArray && i < insertArray.length; i += 2) {
                var element = Dom.findElement(sourceRows, insertArray[i + 1]);
            // var element = Dom.getElementById(insertArray[i].AWSOURCE);
                // var element = sourceById[insertArray[i].AWSOURCE];
                target = null;
                if (insertArray[i] == "null") {
                    //    Debug.log("insert " + insertArray[i].s + " at head of table");
                    // look for first row with an id (we want to insert after any client-inserted rows -- e.g. by
                    // the scrolling AWTDataTable
                    var rows = targetTBody.childNodes;
                    var rowNum = 0;
                    while (rowNum < rows.length) {
                        var row = rows[rowNum++];
                        if (row.id != "") {
                            target = row;
                        // alert("hit=" + Dom.getOuterHTML(target));
                            break;
                        }
                    // alert("skipped=" + Dom.getOuterHTML(row));
                    }
                }
                else {
                    //    Debug.log("insert: " + insertArray[i].s + " after: " + insertArray[i].p);
                    target = Dom.getElementById(insertArray[i]);
                    target = target.nextSibling;
                    
                    while (target && !target.tagName) {
                        target = target.nextSibling;
                    }
            //    Debug.log("---> insert: " + insertArray[i].s + " before: " + target.id);
                }

                // Check that it is non-text node.  Firefox counts text as a node
                if (target) {
                    //debug("inserting before: " + target.id + " " + element.id);
                    targetTBody.insertBefore(element, target);
                }
                else {
                    //debug("appending: " + element.id);
                    targetTBody.appendChild(element);
                }
            }

        // updates
            // -------
            // sourceRows is used as a stack of rows to replace.  Each
            // replaceChild call removes sourceRow[0] from sourceRows and
            // adds it to targetTBody
            // use topOfStack to skip over script blocks
            i = 0;
            while (i < sourceRows.length) {
                var sourceRow = sourceRows[i];
            //debug("replace: " + sourceRow.nodeName + " " + sourceRow.id);
                if (sourceRow.nodeName == "TR") {
                    var targetRow = Dom.findRow(targetTBody, sourceRow.id);
                    if (targetRow == null) {
                        //debug("could not find row for: " + sourceRow.nodeName + " " + sourceRow.id);
                        //poSourceTable.childNodes[0].removeChild(sourceRow);
                        i++;
                    }
                    else {
                        // Debug.log("replacing: " + sourceRow.id + " " + targetRow.id);
                        targetTBody.replaceChild(sourceRow, targetRow);
                    }
                }
                else {
                    i++;
                }
            }
        },

        handleUpdateError : function (message)
        {
            var shouldDebug = false;

            if (Request.AWDebugEnabled) {
                shouldDebug = confirm(message + '.\n\nShow details?');
            }

            if (shouldDebug) {
                var updateErrorWin = Dom.openWindow('', 'AWUpdateErrorWin');
                var refreshFrame = Dom.getElementById("AWRefreshFrame");
                var doc = updateErrorWin.document;
                doc.writeln('<html><head><title>Refresh Trace</title></head><body><textarea cols="150" rows="46">');

                doc.writeln(message);
                if (AWRefreshTrace != null) {
                    for (var i = 0; i < AWRefreshTrace.length; i = i + 2) {
                        doc.writeln('\n=============== Main content (' + i / 2 + ') ====================');
                        doc.writeln(AWRefreshTrace[i]);
                        doc.writeln('\n=============== Incremental update (' + i / 2 + ') ====================');
                        doc.writeln(AWRefreshTrace[i + 1]);
                    }
                }

                doc.writeln('</textarea></body></html>');
                doc.close();
            }
            else {
                top.location.href = top.ariba.Request.appendFrameName(top.ariba.Request.AWRefreshUrl);
            }
            return false;
        },

        domRefreshContentCallback : function ()
        {
            //debug("start content refresh");
            if (Request.AWJSDebugEnabled) {
                AWRefreshStartTime = (new Date()).getTime();
            }

        // get the BODY tag for the frame
            var refreshFrame = Dom.getElementById("AWRefreshFrame");
            if (false && Request.AWDebugEnabled) {
                // capture main content DOM and increment update
                if (AWRefreshTrace == null) {
                    AWRefreshTrace = new Array();
                }
                var refreshBody = refreshFrame.contentWindow.document.body;
                if (refreshBody) {
                    if (AWRefreshTrace.length == AWMaxRefreshTraceLength) {
                        for (var traceIndex = 0; traceIndex < AWRefreshTrace.length - 2; traceIndex++) {
                            AWRefreshTrace[traceIndex] = AWRefreshTrace[traceIndex + 1];
                        }
                        AWRefreshTrace.length = AWMaxRefreshTraceLength - 2;
                    }
                    AWRefreshTrace[AWRefreshTrace.length] = Dom.getOuterHTML(document.body);
                    AWRefreshTrace[AWRefreshTrace.length] = Dom.getOuterHTML(refreshBody);
                }
            }

        // make sure refreshContent call back is the last thing on the page otherwise
            // elements lower down will not be loaded yet so they will not be picked up by
            // DOM scan

            // debug
            //var refreshList = "";
            //var actualList  = "";
            //var ignoreList  = "";

            //
            // handle all deletes
            //
            var i, target;
            if (!Util.isNullOrUndefined(AWDomSyncData)) {
                for (var elementId in AWDomSyncData) {
                    var elementDomSyncData = AWDomSyncData[elementId];
                    var deleteArray = elementDomSyncData[AWDELETE];
                    if (deleteArray) {
                        target = Dom.getElementById(elementId);
                        var targetTBody = Dom.findChild(target, "TBODY");
                        for (i = 0; i < deleteArray.length; i++) {
                            //debug("delete: " + deleteArray[i]);
                            var child = Dom.getElementById(deleteArray[i]);
                            if (Util.isNullOrUndefined(child)) {
                                return this.handleUpdateError("AW: Error detected during delete. Unable to find element '" + deleteArray[i] + "'");
                            }
                            targetTBody.removeChild(child);
                        }
                    }
                }
            }

            var body = Dom.findChild(refreshFrame.contentWindow.document, "BODY");
        //debug("--- body: " + body);

            if (body != null) {

                var refreshNodes = body.childNodes;

                for (i = 0; i < refreshNodes.length; i++) {
                    var source = refreshNodes[i];
                //debug("refresh element: " + source.id);
                    // ### if the source does not have an id ... assume it's a script and skip it?
                    if (!source.id) {
                        //debug("unknown element type found: " + source.nodeName);
                        continue;
                    }
                    target = Dom.getElementById(source.id);

                    if (Util.isNullOrUndefined(target)) {
                        this.handleUpdateError("AW: Error detected during update. Unable to find element '" + source.id + "'");
                    }

                //debug("refresh element: " + source.id);

                    if (target.getAttribute("ignore") == "true") {
                        // debug
                        //ignoreList += source.id + " ";
                        continue;
                    }

                    if (AWDomScopeUpdateList &&
                        AWDomScopeUpdateList[source.id] == "true") {
                        //debug("scope update for: " + source.id);
                        this._scopeUpdate(source);

                    // debug
                        //actualList += source.id;
                    }
                        // differentiate tables and divs
                    else if (target.nodeName == "TABLE") {
                        this._refreshTable(source, target);

                    // debug
                        //actualList += source.id + " ";
                    }
                    else if (target.nodeName == "DIV" || target.nodeName == "SPAN") {
                        //debug("working on a div/span " + Util.htmlEscapeValue(source.innerHTML));
                        var scrollTop = target.scrollTop;
                        target.innerHTML = source.innerHTML;
                        if (scrollTop) {
                            target.scrollTop = scrollTop;
                        }

                    // debug
                        //actualList += source.id + " ";
                        if (this.AWMarkRefreshRegions) {
                            target.style.backgroundColor = "#FFE080";
                        }
                    }
                    else if (Dom.isNetscape() && target.nodeName == "PRE") {
                        // NS parses PRE tags inside table structure differently -- places
                        // on DOM tree
                        target.innerHTML = source.innerHTML;
                    }
                    else {
                        // debug
                        //debug("unknown refresh node type: " + source.nodeName +"["+ source.id +"]");
                        this.handleUpdateError("AW: Error detected during update. Unknown refresh node type '" + target.nodeName + ", element '" + source.id + "'");
                    }

                // debug
                    //refreshList += source.id + " ";
                }
            }


        // clear out the domsync metadata
            AWDomSyncData = null;
            AWDomScopeUpdateList = null;

        // Debug.log("done refreshContentCallback: ");
        },

        windowOnLoad : function ()
        {
            AWWindowOnLoad = true;

            AWWindowLoadStartTime = (new Date()).getTime();

            this.refreshComplete();
            AWWindowOnLoad = false;
        },

        refreshComplete : function ()
        {
            Debug.log("Refresh complete called...")
            Event.eventEnqueue(Request.requestComplete.bind(Request), null, true);
            Event.notifyRefreshComplete();
        },

        findDomSyncElementData : function (parentBufferName)
        {
            // [elementid][delete] = array of sourceId
            //            [insert] = array of {predecessorId, sourceId}
            if (AWDomSyncData == null) {
                AWDomSyncData = new Object();
            }
            if (!AWDomSyncData[parentBufferName]) {
                AWDomSyncData[parentBufferName] = new Object();
            }
            return AWDomSyncData[parentBufferName];
        },

        registerScopeChanges : function (parentBufferName, inserts, deletes)
        {
            var elementDomSyncData = this.findDomSyncElementData(parentBufferName);
            elementDomSyncData[AWINSERT] = inserts;
            elementDomSyncData[AWDELETE] = deletes;
        },

        registerScopeUpdate : function (elementName)
        {
            //debug("awDomRegisterScopeUpdate: " + elementName);
            if (AWDomScopeUpdateList == null) {
                AWDomScopeUpdateList = new Object();
            }

            AWDomScopeUpdateList[elementName] = "true";
        //debug("done awDomRegisterScopeUpdate");
        },

        registerGlobalJS : function (str)
        {
            if (_LoadedJSStrs[str] == "true") {
                Debug.log("awRegisterGlobalJS: Skipping reload of already registered JS: " + str);
            } else {
                _LoadedJSStrs[str] = "true";
            // Debug.log("awRegisterGlobalJS: Loading new global JS: " + str);
                this.insertGlobalJS(str);
            }
        },

        RSS : function (sync, isGlobalScope, funcString)
        {
            _RunningIncrementalAction = true;

            var func = function() {      // execute all embedded scripts (ie AWClientSideScript's)
                // try {
                    if (isGlobalScope) {
                        var bodyString = Refresh.extractFuncBody(funcString);
                        Refresh.registerGlobalJS(bodyString);
                    } else {
                        eval("var f=" + funcString + "; f.call();");
                    }
                }
            /*
                catch (e) {
                    var msg = "Exception evaluating script: \n\n" + funcString + "\n -- \n" + e.description;
                    alert(msg);
                }
            }
            */
            Refresh.RSF(sync, false, func);
        },

        RSF : function (sync, isGlobalScope, func)
        {
            if (sync) {
                func.call(null);
            } else {
                if (isGlobalScope) {
                    // Need to remove "function(){ };" wrapper
                    var funcStr = this.extractFuncBody(func.toString());
                    func = function() {
                        Refresh.registerGlobalJS(funcStr);
                    };
                }
                Event.registerUpdateCompleteCallback(func);
            }
        },

        RVBS : function (id, isGlobalScope)
        {
            Event.registerUpdateCompleteCallback(function() {
                Refresh.execVBS(id, isGlobalScope);
            });
        },

        execVBS : function (id, isGlobalScope)
        {
            // enqueue it for later possible flushing via flushVBSQueue
            if (VBSEnqueue) {
                if (!VBSArray) VBSArray = [];
                VBSArray[VBSArray.length] = [id, isGlobalScope];
                return;
            }

            var pre = Dom.getElementById(id);
            var preInnerText = Dom.getInnerText(pre);
            if (Dom.IsIE) {
                try {
                    // Debug.log("evaluating vbscript (" + window.name + "):<pre>" + preInnerText + "</pre>");
                    Event.GlobalEvalVBScript(preInnerText);
                }
                catch (e) {
                    var msg = "execVBS: exception evaluting script at id: " + id + "\n" + e.description;
                    if (pre && preInnerText) {
                        msg += "\n\n" + preInnerText;
                    }
                    alert(msg);
                }
            }
        },

        flushVBSQueue : function ()
        {
            VBSEnqueue = false;
            if (VBSArray != null) {
                for (var i = 0; i < VBSArray.length; i++) {
                    this.execVBS(VBSArray[i][0], VBSArray[i][1]);
                }
            }
        },

        extractFuncBody : function (str)
        {
            var re = /function\s*\(\)\s*\{((.|\s)*)\}$\s*/m;
            var m = re.exec(str);
            var bodyString = "";
            if (m) {
                bodyString = m[1];
            // alert ("Body String: " + bodyString);
            } else {
                alert("No Match: !  -- " + str);
            }
            return bodyString;
        },

        insertGlobalJS : function (str)
        {
            var head = document.getElementsByTagName('head')[0];
            var scriptElem = document.createElement('script');
            scriptElem.setAttribute('type', 'text/javascript');
            head.appendChild(scriptElem);
            scriptElem.text = str;
        },

        loadJSFile : function (url, noRetry)
        {
            if (_LoadedJS[url] == 1) return;
            _LoadedJS[url] = 1;

            Debug.log("JS Load initiated (" + _RunningIncrementalAction + "): " + url);

            var scriptHolder = [];

            function applyJS() {
                if (scriptHolder.length > 0) {
                    Debug.log("Applying JS: " + url);
                    this.insertGlobalJS(scriptHolder[0]);
                    // Firefox 2.0.0.2 workaround -- newly registered JS isn't available for invocation
                    // until after a pass through the event loop.  Ick!
                    if (!Dom.IsIE) {
                        Event.refreshIncrementNesting();
                        setTimeout(Event.notifyRefreshComplete.bind(Event), 0);
                    }
                } else {
                    if (noRetry) {
                        alert("Failed to load JS: " + url);
                    }
                    else {
                        _LoadedJS[url] = 0;
                        this.loadJSFile(url, true);
                    }
                }
            }

            function httpSuccess(http) {
                scriptHolder[0] = http.responseText;

                Debug.log("JS Load complete: " + url);

                // unlock (unnest)
                Event.notifyRefreshComplete();
            }

            // No running JS until we return
            Event.refreshIncrementNesting();

            Event.registerUpdateCompleteCallback(applyJS.bind(this));

            Request.simpleXMLHTTP(url, httpSuccess.bind(this));
        },
        // Creates a form in the current script context which is a copy of the iframeForm
        // argument and submits the form.  Used to work around issue with form submits
        // inside iframes that have display:none set.
        iFrameFormSubmit : function (iframeForm)
        {
            var realForm = document.createElement("form");
            document.body.appendChild(realForm);
            realForm.method = iframeForm.method;
            realForm.action = iframeForm.action;
            realForm.innerHTML = iframeForm.innerHTML;
            realForm.submit();
        },
        
        // awCompleteRequest is executed inline from incremental update content as well as
        // full page refresh content.  awCompleteRequest is responsible for cleaning up
        // request complete timers and for initiating incremental update, post load scripts,
        // and history setup.
        // awWindowOnLoad is only called from full page refresh content and handles all
        // clean up / post load calls for full page refreshes.
        // NOTE: for responses with a different mime type (file download), neither
        //       awCompleteRequest nor awWindowOnLoad will be called.
        completeRequest : function (current, length, isRefreshRequest)
        {
            Input.ShoudCheckActiveElement = !isRefreshRequest;

            // this method always gets run inline.  For a refresh request, this initiates
            // the page update, etc.  For a full page refresh, queue up on awWindowOnLoad
            Event.eventLock();
            if (isRefreshRequest) {
                Debug.log("*** refresh");
                Event.invokeRegisteredHandlers("onRefreshRequestComplete");
                Request.refreshRequestComplete();

            // update all content
                this.domRefreshContentCallback();

            // kill the iframe used to initiate thecurrent request
                if (!Request.AWShowRequestFrame) {
                    Request.destroyRequestIFrame("AWRefreshFrame");
                }

                Event.registerUpdateCompleteCallback(function() {
                    Refresh.updateHistory(current, length);
                    if (Input.AWAutomationTestModeEnabled) {
                        setTimeout(Request.setStatusDone.bind(Request), 0);
                    }
                });

            // execute all user registered callbacks
                this.refreshComplete();
            }
            else {
                Debug.log("*** full page update");
                Event.registerUpdateCompleteCallback(this.updateHistory.bind(this), [current, length]);
            }

        // Show / hide FPR alert
            var e = Dom.getElementById("FPR_Warning");
            if (e) {
                if (isRefreshRequest) {
                    if (e.tagName == "DIV") Dom.getElementById("debugBar").className = "debugBar";
                    e.className = "";
                    e.innerHTML = "";
                } else {
                    Dom.getElementById("debugBar").className = "debugBarVis";
                    var msg = e.innerHTML;
                    if (msg && msg.indexOf("(OK)") >= 0) {
                        e.className = "debugWarning";
                        e.innerHTML = "Full Page Refresh: <br/>" + e.innerHTML;
                    } else {
                        e.className = "debugError";
                        e.innerHTML = "FULL PAGE REFRESH!: <br/>" + e.innerHTML;
                    }
                }
            }

            if (Request.AWJSDebugEnabled) {
                Request.AWUpdateCompleteTime = (new Date()).getTime();
                setTimeout(this.debugRequestComplete.bind(this), 0);
            }
            Event.eventUnlock();
        },

        debugRequestComplete : function ()
        {
            if (this.AWJSDebugEnabled) {
                var currTime = (new Date()).getTime();
                var total = "n/a";
                var refreshTime = "n/a";
                var postRefreshTime = "n/a";
                var onloadTime = "n/a";
                var requestStartTime = Debug.getRequestStartTime();
                if (!Util.isNullOrUndefined(requestStartTime)) {
                    total = currTime - requestStartTime;
                }
                else {
                    Debug.log("Null start time. Request not initiated through iframe.");
                }

                if (!Util.isNullOrUndefined(AWRefreshStartTime)) {
                    refreshTime = currTime - AWRefreshStartTime;
                }

                if (!Util.isNullOrUndefined(this.AWUpdateCompleteTime)) {
                    postRefreshTime = currTime - this.AWUpdateCompleteTime;
                }

                if (!Util.isNullOrUndefined(AWWindowLoadStartTime)) {
                    onloadTime = currTime - AWWindowLoadStartTime;
                }

                Debug.log("--> request complete - total:" + total + " onload: " + onloadTime + " refresh:" + refreshTime + " postRefresh:" + postRefreshTime);
            }
        },

        //////////////////////
        // Lazy Loading Divs
        //////////////////////
        /**
         This utility function is for the convenience
         of the menu system and allows for the placement of
         an <AWLazyDiv> tag within another div (divObject) that the system
         knows about.  It searches for a child div within divObject
         and calls this.loadLazyDiv(child) until its loaded.
         */
        loadLazyChildren : function (divObject, postLoadCallback)
        {
            //alert("awloadLazyChildren " + postLoadCallback);
            var childrenArray = Dom.getChildren(divObject);
            var arrayLength = childrenArray.length;
            var index = 0;
            var val = false, loaded;
            for (index = 0; index < arrayLength; index++) {
                var childObject = childrenArray[index];
                if (childObject.tagName == 'DIV') {
                    loaded = this.loadLazyDiv(childObject, postLoadCallback);
                    val = val || loaded;
                }
                    //  skip span's
                else if (childObject.tagName == 'SPAN') {
                    loaded = this.loadLazyChildren(childObject, postLoadCallback);
                    val = val || loaded;
                }
            }
            return val;
        },

        /**
         Force a lazy div, divObject, to fetch from the server and copy
         results into the div.
         */
        loadLazyDivCallback : function (divObject, xmlhttp)
        {
            var parent = divObject.parentNode;
            // copy content into the proper location
            Dom.setOuterHTML(divObject, xmlhttp.responseText);
            // evaluate all inline scripts
            Request.evalScriptTags(xmlhttp.responseText);
            // indicate that update is complete
            Refresh.refreshComplete();

            divObject.setAttribute("awneedsLoading", "false");
            Input.hideWaitCursor();
            Event.notifyParents(parent, "lazyCallback");
            this.postLoadLazyDiv();
            if (Input.AWAutomationTestModeEnabled) {
                setTimeout(Request.setStatusDone.bind(Request), 0);
            }
        },

        loadLazyDiv : function (divObject, postLoadCallback)
        {
            if (this.divNeedsLoading(divObject)) {

                Input.showWaitCursor();
                var divId = divObject.id;
                var url = Request.formatSenderUrl(divId);

                // wrap the awLoadLazyDivCallback in an anonymous function so we can
                // pass the additional divObject to it
                Request.initiateXMLHttpRequest(url,
                        function (xmlhttp) {
                            // make sure divObject is still here -- in case other activity removed it
                            var div = Dom.getElementById(divId);
                            if (div) {
                                if (postLoadCallback) {
                                    postLoadCallback(div, xmlhttp);
                                }
                                else {
                                    Refresh.loadLazyDivCallback(div, xmlhttp);
                                }
                            }
                        });

                return true;
            }
            else if (this.loadLazyChildren(divObject, postLoadCallback)) {
                return true;
            }
            return false;
        },

        divNeedsLoading : function (divObject)
        {
            return divObject.getAttribute("awneedsLoading") == "true";
        },

        childrenNeedLoading : function (divObject)
        {
            var childrenArray = Dom.getChildren(divObject);
            var arrayLength = childrenArray.length;
            var index = 0;
            for (index = 0; index < arrayLength; index++) {
                var childDiv = childrenArray[index];
                if (childDiv.tagName == 'DIV' || childDiv.tagName == 'SPAN') {
                    if (this.divNeedsLoading(childDiv)) {
                        return true;
                    }
                    else {
                        return this.childrenNeedLoading(childDiv);
                    }
                }
            }
            return false;
        },

        evalOnVisibleScript : function (element)
        {
            var children = element.childNodes;
            for (var index = children.length - 1; index > -1; index--) {
                var child = children[index];
                if (child.id == "_awonVisible") {
                    var onVisibleScript = child.innerHTML;
                    eval(onVisibleScript);
                    return;
                }
            }
        },

        undisplayDiv : function (divObject)
        {
            if (divObject != null) {
                divObject.style.display = 'none';
                Dom.unoverlay(divObject);
            }
        },                   
        
        ////////////////////
        // Backtracking / History support
        ////////////////////

        getHistoryIFrame : function ()
        {
            return Dom.getElementById("AWHistoryControl");
        },

        createHistoryIFrame : function ()
        {
            // jetison the old history iframe and create a new one
            var iframeDiv = Dom.getElementById("AWHistoryFrameDiv");
            if (!iframeDiv) {
                alert("AWHistoryFrameDiv not found");
            // Note: we're unable to create the history div on the fly since
                // at the point we're doing this, the document is not fully rendered
                // so IE throws an "Unable to load internet site ..." error.
                //iframeDiv = document.createElement("div");
                //iframeDiv.id = "AWHistoryFrameDiv";
                //document.body.appendChild(iframeDiv);
            }
            var height = this.AWShowHistoryFrame ? "height='55px'" : " height='0px' width='0px'";

            iframeDiv.innerHTML = "<iframe src='" + Dom.AWEmptyDocScriptlet + "' id='AWHistoryControl' name='AWHistoryControl' style='border:0px'" + height + "></iframe>";

            return this.getHistoryIFrame();
        },

        updateHistory : function (current, length)
        {
            if (Dom.isSafari) return;
            
            //debug("history: " + current + " " + length);

            // limit multi-backrack to AWHistoryLimit
            var backCount = current;
            if (backCount > AWHistoryLimit) {
                backCount = AWHistoryLimit;
            }

        // limit forward track to 1
            var forwardCount = 0;
            if (length - 1 - current >= 1) {
                forwardCount = 1;
            }
        //debug("history -- back: " + backCount + " forward: " + forwardCount);

            var iframe = this.getHistoryIFrame();
            if (iframe) {
                // if the backtrack state has not changed and there's no forward
                // track state (ie progression of new requests), then we don't have
                // to update the history
                // if we're back or forward tracking, still need to update the history
                // since the browser is going to be modifying our back/forward track
                // history pseudo-url's
                if (AWHistoryBack == backCount && AWHistoryForward == forwardCount) {
                    return;
                }
            }
            else {
                // if there wasn't an iframe to start with, then this must
                // be a full page refresh
            }

            AWHandlingNewRequest = true;

        // debug
            AWHistoryDebugString = "back: " + (backCount + 1) + " forward: " + forwardCount;

        // track current back/forward track info
            AWHistoryBack = backCount;
            AWHistoryForward = forwardCount;
        // add one since history doesn't start to accumulate until
            // the content in the hist changes
            //createBacktrackHistory(backCount+1, forwardCount);
            //        Debug.log("Initiate... spin up history: " + AWHistoryBack + " " + AWHistoryForward);
            setTimeout(this.startBacktrackHistoryCreate.bind(this), 10);
        // this.startBacktrackHistoryCreate();
        },

        historyRequest : function (distance)
        {
            // called during pseudo-url setup
            if (AWHandlingNewRequest) {
                AWHandlingNewRequest = false;
                return;
            }

        // cancel check until request completes and it's restarted
            if (_CheckLocInterval) {
                clearInterval(_CheckLocInterval);
                _CheckLocInterval = null;
            }

        // real request
            if (distance == -1) {
                // back track
                AWHandlingTrackRequest = true;
                Request.getContent(this.AWBackTrackUrl);
            }
            else if (distance == 1) {
                // forward track
                AWHandlingTrackRequest = true;
                Request.getContent(this.AWForwardTrackUrl);
            }
            else {
                // refresh?
                alert("refresh?");
            }
        },

        startBacktrackHistoryCreate : function ()
        {
            //        Debug.log("spin up history: " + AWHistoryBack + " " + AWHistoryForward);

            // create a new history frame
            if (Dom.IsIE) {
                var iframe = this.createHistoryIFrame();
            } else {
                if (!_CheckLocInterval) {
                    _CheckLocInterval = setInterval(this.checkForLocationChange.bind(this), 100);
                }
            }
            // add one since history doesn't start to accumulate until
            // the content in the hist changes
            _LocationCheckActive = false;
            this.createBacktrackHistory(AWHistoryBack + 1, AWHistoryForward);
        },

        checkForLocationChange : function () {
            if (!_LocationCheckActive) return;
            var loc = Dom.getHashLocation();
            this.processLocationChange(loc);
        },

        processLocationChange : function (loc)
        {
            if (loc && loc != "b0") {
                var dir = loc.charAt(0);
                if (dir == 'b') {
                    this.historyRequest(-1);
                } else if (dir == 'f') {
                    this.historyRequest(1);
                }
            }
        },

        createBacktrackHistory : function (backCount, forwardCount)
        {
            // Debug.log("backtrack: " + backCount + " " + forwardCount);
            if (backCount > 0) {
                AWHandlingNewRequest = true;
                this.backtrackHistory(backCount - 1, forwardCount);
            }
            else {
                this.createForwardtrackHistory(forwardCount, forwardCount);
            }
        },

        createForwardtrackHistory : function (forwardCount, currentCount)
        {
            //        Debug.log("forwardTrack: " + forwardCount + " " + currentCount);
            if (forwardCount > 0) {
                AWHandlingNewRequest = true;
                this.forwardtrackHistory(forwardCount - 1, currentCount);
            }
            else {
                // rewind to current position
                if (currentCount != 0) {
                    AWHandlingNewRequest = true;
                    _historyHandler = function () {
                        _historyHandler = Refresh.processLocationChange.bind(Refresh);
                    };
                    history.go(-currentCount);
                } else {
                    _historyHandler = Refresh.processLocationChange.bind(Refresh);
                }
                AWHandlingNewRequest = false;
                _LocationCheckActive = true;
            }
        },

        historyKey : function (url) {
            // pull out the "k=()" value from the query string
            var m = url.toString().match(/(&|\?)k=(\w+)/);
            return m ? m[2] : null;
        },

        historyEvent : function (url)
        {
            // Debug.log ("IE History frame callback: " + url);
            _historyHandler(this.historyKey(url));
        },

        addHistory : function (key, postAddHandler)
        {
            // history iframe content is vended  by special history key in AWComponentActionRequestHandler
            // This page, when loaded will call awHistoryEvent passing its url
            var iframe = this.getHistoryIFrame();
        // we need to make sure the src that we're adding is different than the one currently
            // there or IE won't add the history.  Just add "1" the end to make it unique
            // (i.e. if we're adding 3 history entries we'll add b, b1, b).
            if (this.historyKey(iframe.src) == key) key += "1";
            iframe.src = Request.AWReqUrl + "?awh=s&k=" + key;
            _historyHandler = postAddHandler;
        },

        backtrackHistory : function (backCount, forwardCount)
        {
            // ### get the page title dynamically from the application
            // Debug.log ("Add Backtrack: " + backCount);
            if (Dom.IsIE) {
                this.addHistory("b", function (key) {
                    Refresh.createBacktrackHistory(backCount, forwardCount);
                });
            } else {
                function bt() {
                    window.location.hash = "b" + backCount;
                    Refresh.createBacktrackHistory(backCount, forwardCount);
                }
                setTimeout(bt, 50);
            }
        },

        forwardtrackHistory : function (forwardCount, currentCount)
        {
            // ### get the page title dynamically from the application
            Debug.log("Add Forwardtrack: " + forwardCount);
            if (Dom.IsIE) {
                this.addHistory("f", function (key) {
                    Refresh.createForwardtrackHistory(forwardCount, currentCount);
                });
            } else {
                function ft() {
                    window.location.hash = "f" + forwardCount;
                    Refresh.createForwardtrackHistory(forwardCount, currentCount);
                }
                setTimeout(ft, 50);
            }
        },

        EOF:0};

    //
    // IE - specific methods
    //
    if (Dom.IsIE) Util.extend(Refresh, function () {
        // private variables
        var AWCurrDiv_IE;

        return {
            ////////////////////////////////
            // Div positioning / lazy div
            ////////////////////////////////
            postLoadLazyDiv : function ()
            {
                if (AWCurrDiv_IE && Dom.elementInDom(AWCurrDiv_IE)) {
                    Dom.repositionDivToWindow(AWCurrDiv_IE);
                    Dom.overlay(AWCurrDiv_IE, true);
                }
                AWCurrDiv_IE = null;
            },

            // overridden by Menu.js
            preDisplayDiv : function (divObject)
            {

            },

            displayDiv : function (divObject, mode, needsUpdate, skipOverlay)
            {
                if (divObject != null) {
                    divObject.style.display = (mode ? mode : '');
                    AWCurrDiv_IE = divObject;
                    this.loadLazyDiv(divObject);
                    if (!skipOverlay) {
                        this.preDisplayDiv(divObject);
                        Dom.overlay(divObject, needsUpdate);
                    }
                    this.evalOnVisibleScript(divObject);
                }
            },


        EOF:0};
    }());

    //
    // Mozilla - specific methods
    //
    if (!Dom.IsIE) Util.extend(Refresh, function () {
        // private variables
        var AWCurrDiv_NS;

        return {

            ////////////////////////////////
            // Div positioning / lazy div
            ////////////////////////////////
            // The following methods are used to workaround the problem that NS has with
            // resizing divs when dynamic content is wider than the original div.  IE automatically
            // resizes the awmenu div to fit all content brought in by AWLazyDiv, but NS does not.
            postLoadLazyDiv : function (mode)
            {
                if (AWCurrDiv_NS) {
                    // display the div so all sizes are established
                    mode = mode ? mode : '';
                    AWCurrDiv_NS.style.display = mode;
                    Dom.repositionDivToWindow(AWCurrDiv_NS);
                    AWCurrDiv_NS = null;
                }
            },

            displayDiv : function (divObject, mode, needsUpdate)
            {
                if (divObject != null) {
                    // display the div so user sees something while we go and fetch the lazy div
                    mode = mode ? mode : '';
                    divObject.style.display = mode;

                // save the main div for post child div load callback
                    AWCurrDiv_NS = divObject;

                    if (!Refresh.loadLazyDiv(divObject)) {
                        // if we don't need to load lazy divs, then just call awPostLoadLazyDiv
                        this.postLoadLazyDiv(mode);
                    }

                    this.evalOnVisibleScript(divObject);
                }
            },

        EOF:0};
    }());

    // Initialization
    window.onload = Refresh.windowOnLoad.bind(Refresh);

    return Refresh;
}();

