/*
    Refresh.js      -- Incremental page update for AW responses

    Responses from the application (via Request.js) call back to routines here to
    incrementally update portions of the DOM and update (pseudo) backtrack history.
    Includes necessary support for deferred (properly enqueued) inline JavaScript and
    incremental loading of .js files.
*/
window.ariba_IR = false;
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
    var _currentUpdateSource = null;
    var _isXMLHttpResponse = false;
    var _IDPat = /\s+id=(.+?)[\s>]/
    var _IDPatQuote = /\s+id=["\'](.+?)["\']/
    var _ScriptAllPat = /<script[^>]*>([\s\S]*?)<\/script>/ig;
    var _ScriptOnePat = /<script([^>]*)>([\s\S]*?)<\/script>/i;
    var _currScript = null;
    var _pendingCompleteRequestRun = false;
    var AWRefreshScriptEnabled = true;
    var _ignoreRefreshComplete = false;

    // Handler called with IE History IFrame is loaded
    var _historyHandler = function () {};

    var _MarkedRRs;

    // For Safari missing image issue. See completeRequest and completeRefreshOnLoad
    var _historyCurrent;
    var _historyLength;
    var _runCompleteRefreshOnLoad = false;

    var _lazyActionScrollInited = false;
    var _lazyActionIds = [];
    var _inViewportLazyActionIds = [];
    var _FireLazyActionsTimeout;


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
        AWAllowParentFrame : false,

        _refreshTable : function (sourceHandle, poTarget)
        {
            // pick up the scratch div
            var tmpDiv = document.createElement("div");
            tmpDiv.innerHTML = _currentUpdateSource.getOuterHtml(sourceHandle);
            // CR #1-CI005F ahebert 08/26/13 :
            // First child should work but invalid html mark-up may
            // cause floating markup before the table get rendered. It can
            // happen that we have span or any other tag in table templates.
            // If we are in this function, the source and the target better
            // be tables so this loop check for the child and set tmpTable
            // to the first child that is actually a table.
            var tmpTable = null;
            for (var i = 0 ; i < tmpDiv.childNodes.length ; ++i){
                if (tmpDiv.childNodes[i].tagName === poTarget.tagName){
                    tmpTable = tmpDiv.childNodes[i];
                    break;
                }
            }
            if (tmpTable == null){
                tmpTable = tmpDiv.firstChild;
            }
            this.replaceRows(tmpTable, poTarget);
        },

        _scopeUpdate : function (sourceHandle)
        {
            var id = _currentUpdateSource.getId(sourceHandle);
            var target = Dom.getElementById(id);
            if (target) {
                Dom.setOuterHTML(target, _currentUpdateSource.getOuterHtml(sourceHandle));
            //debug("done ScopeUpdate: " + Util.htmlEscapeValue(Dom.getOuterHTML(target)));
                this._markRR(Dom.getElementById(id));
            }
            else {
                alert("scopeUpdate target not found: " + id);
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
                this._markRR(element);
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
                // rows with no id are not refresh regions
                if (sourceRow.nodeName == "TR" && sourceRow.id) {                
                    var targetRow = Dom.findRow(targetTBody, sourceRow.id);
                    if (targetRow == null) {
                        //debug("could not find row for: " + sourceRow.nodeName + " " + sourceRow.id);
                        //poSourceTable.childNodes[0].removeChild(sourceRow);
                        i++;
                    }
                    else {
                        // Debug.log("replacing: " + sourceRow.id + " " + targetRow.id);
                        targetTBody.replaceChild(sourceRow, targetRow);
                        this._markRR(sourceRow);
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
                var msg = ['<h1>Refresh Trace</h1><textarea cols="150" rows="46">'];

                if (AWRefreshTrace != null) {
                    for (var i = 0; i < AWRefreshTrace.length; i = i + 2) {
                        msg.push('\n=============== Main content (' + i / 2 + ') ====================');
                        msg.push(AWRefreshTrace[i]);
                        msg.push('\n=============== Incremental update (' + i / 2 + ') ====================');
                        msg.push(AWRefreshTrace[i + 1]);
                    }
                }
                msg.push('</textarea>');

                Request.displayErrorDiv(msg.join("\n"));
            }
            else {
                top.location.href = top.ariba.Request.appendFrameName(top.ariba.Request.AWRefreshUrl);
            }
            return false;
        },

        // HUH?
        _updateRefreshTrace : function ()
        {
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
        },

        IFrameUpdateSource : function () {
            var refreshFrame = Dom.getElementById("AWRefreshFrame");
            var body = Dom.findChild(refreshFrame.contentWindow.document, "BODY");

            var elements = [];

            if (body != null) {
                var refreshNodes = body.childNodes;
                for (var i = 0; i < refreshNodes.length; i++) {
                    var source = refreshNodes[i];
                //debug("refresh element: " + source.id);
                    // ### if the source does not have an id ... assume it's a script and skip it?
                    if (source.id && source.getAttribute("ignore") != "true") {
                        elements.push(source);
                    }
                }

            }
            return {
                getHandles : function () { return elements; },

                getId : function (handle) { return handle.id; },

                getHandleForId : function (id) { return refreshFrame.contentWindow.document.getElementById(id); },

                getNodeName : function (handle) { return handle.nodeName; },

                getInnerHtml : function (handle) { return handle.innerHTML; },

                getOuterHtml : function (handle) {
                    return Dom.getOuterHTML(handle);
                }
            }
        },

        XMLHTTPUpdateSource : function (response) {
            // " head <!--@&@--> re 1 <!--@&@-->re2<!--@&@-->end"
            var ids = [];
            var info = {};
            function init() {
                var start = 0; // response.indexOf("<!--@&@-->");
                while (start != -1) {
                    start += 10;
                    var end = response.indexOf("<!--@&@-->", start);
                    var body = (end == -1) ? response.substring(start) : response.substring(start, end);
                    var tagEnd = body.indexOf(">")
                    var tag = body.substring(0, tagEnd+1)
                    var nodeNameEnd = tag.indexOf(" ");
                    var nodeName = tag.substring(1,nodeNameEnd).toUpperCase()
                    var m = _IDPatQuote.exec(tag);
                    if (!m) {
                        m = _IDPat.exec(tag);
                    }
                    if (m) {
                        var id = m[1]
                        // Debug.log("RR: " + id + " -> " + body);
                        ids.push(id);
                        info[id] = { body:body, nodeName:nodeName }
                    }

                    start = end;
                }
                /*
                var matches = response.match(RRPat);
                for (var i=0; i < matches.length; i++) {
                    var body = matches[i].substring(10);
                    var tagEnd = body.indexOf(">")
                    var tag = body.substring(0, tagEnd+1)
                    var nodeNameEnd = tag.indexOf(" ");
                    var nodeName = tag.substring(1,nodeNameEnd).toUpperCase()
                    var m = _IDPat.exec(tag);
                    var id = m[1]

                    Debug.log("RR: " + id + " -> " + body);

                    ids.push(id);
                    info[id] = { body:body, nodeName:nodeName }
                }
                */
            }

            init();

            return {
                getHandles : function () { return ids; },

                getId : function (handle) { return handle; },

                getHandleForId : function (id) { return id; },

                getNodeName : function (handle) { return info[handle].nodeName; },

                getInnerHtml : function (handle) {
                    var body = info[handle].body;
                    var start = body.indexOf(">"), end = body.lastIndexOf("<");
                    return body.substring(start + 1, end);
                },

                getOuterHtml : function (handle) {
                    return info[handle].body;
                }
            }
        },

        evalScriptTags : function (str)
        {
            var matches = str.match(_ScriptAllPat) || [];
            for (var i=0; i < matches.length; i++) {
                var outer = matches[i];
                var m = _ScriptOnePat.exec(outer);
                _currScript = m[2];
                if (m[1].indexOf("VBScript") == -1) {
                    // Debug.log("Evaluating: " + script);
                    eval(_currScript);
                } else {
                    // Debug.log("Evaluating VBScript: " + script);
                    Event.GlobalEvalVBScript(_currScript);
                }
                _currScript = null;
            }
        },

        processXMLHttpResponse : function (response) {
            window.ariba_IR = _isXMLHttpResponse = true;
            try {
                _pendingCompleteRequestRun = true;

                _currentUpdateSource = new this.XMLHTTPUpdateSource(response);
                this.evalScriptTags(response);

            } catch (e) {
                Request.displayErrorDiv("<h1>Error applying incremental refresh</h1>"
                        + "<b>" + e + "</b><br/><br/>"
                        + "<b>Script:</b> <pre><code>" + _currScript + "</code></pre><br/><br/>"
                        + "<h2>Attaching full response content below...</h2>"
                        + response);
                throw(e);
            } finally {
                window.ariba_IR = _isXMLHttpResponse = false;
            }

            // A good response would either 1) run completeRequest (and clear our flag)
            // or 2) trigger a redirect (and therefore stop the run of setTimeout)
            setTimeout(function () {
                if (_pendingCompleteRequestRun) {
                    Request.displayErrorDiv("<h1>Bad XMLHTTP Incremental Refresh Response</h1>"
                            + "<h2>Attaching full response content below...</h2>"
                            + response);
                }
            }, 1);
        },

        _markRR : function (target) {
            if (this.AWMarkRefreshRegions) {
                Dom.addClass(target, "showRR");
                if (!_MarkedRRs) _MarkedRRs = [];
                _MarkedRRs.push(target);
                // target.style.backgroundColor = "#FFE080";
            }
        },

        clearPendingCompleteRequestRun : function () {
            _pendingCompleteRequestRun = false;
        },

        enableRefreshScript : function () {
            AWRefreshScriptEnabled = true;
        },

        domRefreshContentCallback : function ()
        {
            if (_MarkedRRs) {
                while (_MarkedRRs.length) {
                    var e = _MarkedRRs.pop();
                    try { Dom.removeClass(e, "showRR"); } catch (e) {}
                }
            }

            //debug("start content refresh");
            if (Request.AWJSDebugEnabled) {
                AWRefreshStartTime = (new Date()).getTime();
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

            var handles = _currentUpdateSource.getHandles();

            for (i = 0; i < handles.length; i++) {
                var handle = handles[i];
                var id = _currentUpdateSource.getId(handle);
                var nodeName = _currentUpdateSource.getNodeName(handle);

                target = Dom.getElementById(id);
                if (Util.isNullOrUndefined(target)) {
                    this.handleUpdateError("AW: Error detected during update. Unable to find element '" + id + "'");
                }

                if (AWDomScopeUpdateList &&
                    AWDomScopeUpdateList[id] == "true") {
                    //debug("scope update for: " + source.id);
                    this._scopeUpdate(handle);
                }
                    // differentiate tables and divs
                else if (nodeName == "TABLE") {
                    this._refreshTable(handle, target);
                }
                else if (nodeName == "DIV" || nodeName == "SPAN") {
                    //debug("working on a div/span " + Util.htmlEscapeValue(source.innerHTML));
                    var scrollTop = target.scrollTop;
                    target.innerHTML = _currentUpdateSource.getInnerHtml(handle);
                    if (scrollTop) {
                        target.scrollTop = scrollTop;
                    }
                    this._markRR(target);
                }
                else if (Dom.isNetscape() && target.nodeName == "PRE") {
                    // NS parses PRE tags inside table structure differently -- places
                    // on DOM tree
                    target.innerHTML = _currentUpdateSource.getInnerHtml(handle);
                    this._markRR(target);
                }
                else {
                    // debug
                    //debug("unknown refresh node type: " + source.nodeName +"["+ source.id +"]");
                    this.handleUpdateError("AW: Error detected during update. Unknown refresh node type '" + target.nodeName + ", element '" + id + "'");
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

        checkParentFrame : function (allowParentFrame)
        {
            // enable page if allow parent frame explicity or no parent frame
            var enablePage = allowParentFrame || top == self;

            if (!enablePage) {
                // does not allow parent frame and has parent frame
                var pop = true;
                try {
                    //If we are not the top frame, check if the top frame is
                    //from the same host. This could be a punch out session.
                    if (top.location.hostname == document.location.hostname ){
                        pop = false;
                        enablePage = true;
                    }
                }
                catch (e) {
                   //If the top frame is from a differant host, we are
                   //likely to get an exception, in which case promote the
                   //current frame to the top.
                }
                if (pop) {
                    // this could fail, but page will stay disabled.
                    top.location = self.location;
                }
            }
            if (enablePage) {
                document.body.style.visibility = 'visible';
                document.body.style.display = '';
            }
        },
        
        refreshComplete : function ()
        {
            Debug.log("Refresh complete called...");

            // Refresh region marking
            if (this.AWMarkRefreshRegions) {
                Dom.addClass(document.body, "rrVis");
            } else {
                Dom.removeClass(document.body, "rrVis");
            }

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

        // Run script string used by incremental updates
        RSS : function (sync, isGlobalScope, funcString)
        {
            _RunningIncrementalAction = true;
            //ariba.Debug.log("RSS (register): " + funcString);
            var func = function() {      // execute all embedded scripts (ie AWClientSideScript's)
                try {
                    if (isGlobalScope) {
                        var bodyString = Refresh.extractFuncBody(funcString);
                        ariba.Debug.log("Executing AWClientSideScript ["+bodyString+"]");
                        Refresh.registerGlobalJS(bodyString);
                    } else {
                        //ariba.Debug.log("RSS (run): " + funcString);
                        eval("var f=" + funcString + "; f.call();");
                    }
                }
                catch (e) {
                    e.message = "Exception evaluating script: \n\n" + funcString + "\n -- \n" + e.message;
                    throw e;
                }
            }
           
            Refresh.RSF(sync, false, func);
        },

        // Run script function
        RSF : function (sync, isGlobalScope, func)
        {
            if (!AWRefreshScriptEnabled) return;
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
                //ariba.Debug.log("RSF (register): " + func.toString());
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

        // Called from AWFormRedirect during an incremental refresh (either XHR or IFrame)
        // We extract the form from the current response submit it (within our main window)
        iFrameFormSubmit : function (iframeFormName)
        {
            ariba.Request.prepareRedirectRequest();
            // in the case of XHR, the _currentUpdateSource is set before eval-ing the
            // inline JS that calls us (but not so for IFrame)
            var us = _currentUpdateSource ||  new this.IFrameUpdateSource();
            var handle = us.getHandleForId(iframeFormName);
            var formHtml = us.getOuterHtml(handle);
            var div = document.createElement("div");
            document.body.appendChild(div);
            div.innerHTML = formHtml;
            var realForm = div.firstChild;
            realForm.submit();
        },
        
        // awCompleteRequest is executed inline from incremental update content as well as
        // full page refresh content.  awCompleteRequest is responsible for cleaning up
        // request complete timers and for initiating incremental update, post load scripts,
        // and history setup. length is the history list length, 
        // and current is the index in the history list.
        // awWindowOnLoad is only called from full page refresh content and handles all
        // clean up / post load calls for full page refreshes.
        // NOTE: for responses with a different mime type (file download), neither
        //       awCompleteRequest nor awWindowOnLoad will be called.
        completeRequest : function (current, length, isRefreshRequest, ignoreRefreshComplete) {
            _ignoreRefreshComplete = ignoreRefreshComplete;
            if (!_isXMLHttpResponse && Dom.isSafari && isRefreshRequest) {
                // Todo: Conditionalize for Safari
                // defer so that the IFrame's script is finish loading when we are processing
                // (fixes image refresh issue in Safari)
                _historyCurrent = current;
                _historyLength = length;
                _runCompleteRefreshOnLoad = true;
            } else {
                this._completeRequest(current, length, isRefreshRequest);
            }
        },

        ignoreRefreshComplete : function () {
            return _ignoreRefreshComplete;
        },

        completeRefreshOnLoad : function () {
            if (_runCompleteRefreshOnLoad) {
                // prevent this from running again during iframe destruction
                _runCompleteRefreshOnLoad = false;
                ariba.Debug.log('completeRefreshOnLoad')
                this._completeRequest(_historyCurrent, _historyLength, true);
            }            
        },

        _completeRequest : function (current, length, isRefreshRequest)
        {
            _pendingCompleteRequestRun = false;
            AWRefreshScriptEnabled = false;

            // this method always gets run inline.  For a refresh request, this initiates
            // the page update, etc.  For a full page refresh, queue up on awWindowOnLoad
            Event.eventLock();
            if (_isXMLHttpResponse) isRefreshRequest = true;
            if (isRefreshRequest) {
                Debug.log("*** refresh");
                Event.invokeRegisteredHandlers("onRefreshRequestComplete");
                Request.refreshRequestComplete();

                if (_isXMLHttpResponse) {
                    // already set...
                    // _currentUpdateSource = new this.XMLHTTPUpdateSource();

                    // update all content
                    this.domRefreshContentCallback();
                } else {
                    _currentUpdateSource = new this.IFrameUpdateSource();

                    // update all content
                    this.domRefreshContentCallback();

                    // kill the iframe used to initiate thecurrent request
                    if (!Request.AWShowRequestFrame) {
                        Request.destroyRequestIFrame("AWRefreshFrame");
                    }
                }
                _currentUpdateSource = null;

                if (current != null && length != null) {
                    Event.registerUpdateCompleteCallback(function() {
                        Refresh.updateHistory(current, length);
                    });
                }
                if (Input.AWAutomationTestModeEnabled) {
                    Event.registerUpdateCompleteCallback(function() {
                        setTimeout(Request.setStatusDone.bind(Request), 0);
                    });
                }

            // execute all user registered callbacks
                this.refreshComplete();
            }
            else {
                Debug.log("*** full page update");
                if (current != null && length != null) {
                    Event.registerUpdateCompleteCallback(this.updateHistory.bind(this), [current, length]);
                }
                Event.registerRefreshCallback(Request.requestComplete.bind(Request));
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
            // evaluate all inline scripts
            this.evalScriptTags(xmlhttp.responseText);
            AWRefreshScriptEnabled = false;
            // copy content into the proper location
            Dom.setOuterHTML(divObject, xmlhttp.responseText);

            // indicate that update is complete
            Refresh.refreshComplete();

            this.markDivLoadingDone(divObject);
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
                // immediately mark the div as "in progress" to avoid 
                // double loading in cases where other lazyDiv respones
                // causes another lazy div scan.
                this.markDivLoadingInProgress(divObject);
                Request.prepareForRequest();
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

        markDivLoadingInProgress : function (divObject)
        {
            divObject.setAttribute("awneedsLoading", "inProgress");
        },

        markDivLoadingDone : function (divObject)
        {
            divObject.setAttribute("awneedsLoading", "false");
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

        initLazyAction : function (lazyActionId)
        {
            if (!_lazyActionScrollInited) {
                _lazyActionScrollInited = true;
                Event.registerRefreshCallback(this.checkLazyActions.bind(this));
                Event.registerWindowOnScroll(this.checkLazyActions.bind(this));
            }
            _lazyActionIds.push(lazyActionId);
        },

        checkLazyActions : function ()
        {
            var i, lazyActionId, elm, fireLazyActions;
            var notInViewportLazyActionIds = [];
            for (i = 0; i < _lazyActionIds.length; i++) {
                // bucket 
                lazyActionId = _lazyActionIds[i];
                elm = Dom.getElementById(lazyActionId);
                if (elm) {
                    if (Dom.isElementInViewport(elm)) {
                        Debug.log(lazyActionId + " in viewport");
                        Util.arrayAddIfNotExists(_inViewportLazyActionIds, lazyActionId);
                        fireLazyActions = true;
                    }
                    else {
                        Util.arrayAddIfNotExists(notInViewportLazyActionIds, lazyActionId);
                    }
                }
            }
            if (fireLazyActions) {
                this.fireLazyActions();
            }
            _lazyActionIds = notInViewportLazyActionIds;
        },

        fireLazyActions : function ()
        {
            if (_FireLazyActionsTimeout) {
                clearTimeout(_FireLazyActionsTimeout);
            }
            _FireLazyActionsTimeout = setTimeout(this._fireLazyActions.bind(this), 500);                
        },

        _fireLazyActions : function ()
        {
            if (!Request.isRequestInProgress()) {
                var senderId = _inViewportLazyActionIds.join(",");
                _inViewportLazyActionIds = [];                
                Request.getContent(Request.formatInPageRequestUrl(senderId));
            }
            else {
                this.fireLazyActions();
            }
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

function RJS (inc, sync, isGS, f)
{
    // IFrame version overridden in AWXBasicScriptFunctions.awl
    if (ariba.Refresh._isXMLHttpResponse) {
        if (inc) ariba.Refresh.RSS(sync, isGS, f.toString());
    } else {
        ariba.Refresh.RSF(sync, isGS, f);
    }
}
