/*
    Debug.js    - Debug logging (via special window (AWJSDebugWindow.js))

    Also includes debug time form validation
*/
var _AWXMainWindow = null;
var _AWXRefreshMainActionId = null;

ariba.Util.extend(ariba.Debug, function() {
    // imports
    var Event = ariba.Event;
    var Request = ariba.Request;
    var Dom = ariba.Dom;

    // private vars
    var gAWDebugWin = null;
    var _AWDebugWindow;
    var awDeepestChild;
    var _AWHierarchy = null;

    /**
     Set the event flag(s) to true to turn on debug logging.
     */
    var AWDebugEvents = {
        mousein    : false,
        mouseover  : false,
        mouseout   : false,
        mousedown  : false,
        mouseup    : false,
        mousemove  : false,
        click      : false,
        keydown    : false,
        keypress   : false,
        activate   : false,
        deactivate : false,
        blur       : false,
        focus      : false
    }

    var _AWCPIWindOpts = "height=750,width=320,left=0,top=0,menubar=no,location=no,status=no,toolbar=no,resizable=yes";
    var _ComponentInspectorWin;

    // pointer from debug panel back to main window that spawned it
    var _AWXUpdateCPICallback = null;


    Event.registerBehaviors({
        // remote open
        _RO : {
            click : function (elm, evt) {
                var fileLocation = elm.getAttribute("_fl");
                if (!fileLocation) {
                    fileLocation = elm.innerHTML;
                }
                fileLocation = fileLocation.replace(/^file:/, "");
                fileLocation = fileLocation.replace(/.*:\//, "");
                fileLocation = fileLocation.replace("\\", "/");
                fileLocation = fileLocation.replace(/^\//, "");
                var parts = fileLocation.split(".");
                fileLocation = parts[0];
                var length = parts.length;
                for (var i = 1; i < length - 1; i++) {
                    fileLocation = fileLocation + "/" + parts[i];
                }
                fileLocation = fileLocation + "." + parts[length - 1];
                var url = "http://localhost:28073/" + fileLocation;
                var iframe = Request.createRefreshIFrame("AWDebugRemoteOpen");
                iframe.src = url;
                return false;
            }
        },
        _MO : {
            mousemove : function(elm, evt) {
                var currElm = evt.target;
                if(!currElm){
                    // For IE, target is not set. find target based on mouse position.
                    currElm = document.elementFromPoint(evt.clientX, evt.clientY);
                }
                if(ariba.Debug._AWHierarchy && (!this.lastTarget || this.lastTarget != currElm)) {
                    this.lastTarget = currElm;
                    var rowElm = null;

                    //find the primary data row, this has the attribute "dr" set to 1
                    while(!rowElm && currElm.parentNode){
                         currElm = currElm.parentNode;
                        if(currElm.tagName == "TR" && currElm.getAttribute("dr") == "1") {
                            rowElm = currElm;
                        }
                    }

                    if(rowElm) {
                        //search child nodes for span tag with targetId
                        var spanElm = Dom.findChildUsingPredicate(rowElm, function(e) {
                            try{
                                return e.getAttribute("targetId") != null;
                            }catch(e){
                                //handle error when getAttribute is not available
                            }
                            return false;
                        },false);
                        if(spanElm) {
                            var targetId = spanElm.getAttribute("targetId");
                            _AWXMainWindow.ariba.Debug.highlightElementIds(targetId, ariba.Debug._AWHierarchy);
                        }
                    }
                }
            }
        } 
    });

    var Debug = {

        // Parameters put in the panel by AWComponentInspector.awl
        _AWCPISession : "",
        _AWCPIRefreshId : "",

        // Public Globals
        AWDebugJSUrl : '',

        logEvent : function (evt, target, sMsg, iHistory) {
            if (evt && AWDebugEvents[evt.type]) {
                var source = Event.eventSourceElement(evt);
                var sourceTag = '';
                if (source) {
                    if (source.tagName) {
                        sourceTag = '/' + source.tagName;
                    }
                    if (source.id) {
                        sourceTag += '/' + source.id;
                    }
                }

                var targetTag = '';
                if (target) {
                    if (target.tagName) {
                        targetTag = '/' + target.tagName;
                    }
                    if (target.id) {
                        targetTag += '/' + target.id;
                    }
                }
                this.log('Event: ' + evt.type +
                      '<br>Source: ' + source + sourceTag +
                      '<br>Target: ' + target + targetTag + '<br>' +
                      sMsg, iHistory);
            }
        },

        findDebugWindow : function ()
        {
            if (gAWDebugWin == null || gAWDebugWin.closed) {

                var debugJS = this.AWDebugJSUrl;
                if (debugJS == null || debugJS == "") {
                    alert("AWDebugJS not set");
                    return;
                }

                gAWDebugWin = Dom.openWindow('', 'debugWin', 'scrollbars=yes, status=yes, resizable=yes, width=350, height=700, screenX=0, screenY=75, left=0, top=75');
                if (!gAWDebugWin) return;
                if (this.getDocElement(gAWDebugWin.document, "AWDTopControls") == null) {

                    // ### this is actually still a bit hacked -- should just have a single div
                    // in the body and allow AWJSDebugWin to initialize all the contents
                    var content =
                            '<html>\n' +
                            '<head>\n' +
                            '<script language="JavaScript" src="' + debugJS + '"></script>\n' +
                            '<style type="text/css">\n' +
                            'A, DIV, P, TD, TH, BLOCKQUOTE, LI, OL, UL, BODY { font:normal 8pt Verdana, Arial, Helvetica, sans-serif;}\n' +
                            'B { font:bold 10pt Verdana, Arial, Helvetica, sans-serif;}\n' +
                            '</style>\n' +
                            '</head>\n' +
                            '<body>\n';
                    // '</body>\n';
                    gAWDebugWin.document.write(content);
                }

            // wait for window to finish (only for IE -- NS does not process
                // scripts in debug win until main window processing is complete
                if (navigator.appName == "Microsoft Internet Explorer") {
                    while (!gAWDebugWin.document || !gAWDebugWin.getDebug) {
                    }
                }
            }
        // if the window's already open, just leave it where it is
            // gAWDebugWin.focus();
        },

        setRequestStartTime : function (requestStart)
        {
            if (Request.AWJSDebugEnabled) {
                this.findDebugWindow();
                if (gAWDebugWin.getDebug) {
                    gAWDebugWin.getDebug().setStartRequest(requestStart);
                }
            }
        },

        getRequestStartTime : function ()
        {
            var requestStart = null;
            if (Request.AWJSDebugEnabled) {
                this.findDebugWindow();
                if (gAWDebugWin.getDebug) {
                    requestStart = gAWDebugWin.getDebug().getRequestStart();
                }
            }
            return requestStart;
        },

        getMethodName : function (func)
        {
            if (!func) return func;
            var sFunc = func.toString();
        // function <sFunc> (
            return sFunc.substring(9, sFunc.search(/\(/) - 1);
        },

        getCaller : function (numAncestor)
        {
            if (!numAncestor) {
                numAncestor = 0;
            }
            var func = this.getCaller.caller;
            for (var i = 0; i <= numAncestor; i++) {
                func = func.caller;
                if (!func) {
                    return "--"; //"[Error requested level:"+numAncestor+" @ level: "+i+"]";
                }
            }

            if (func) {
                return this.getMethodName(func);
            }
            return "--";
        },
        // override null implementation in AWXRichClientScriptFunctions.js
        log : function (sMsg, iHistory)
        {
            if (Request.AWJSDebugEnabled) {
                this.findDebugWindow();
                if (gAWDebugWin && gAWDebugWin.getDebug) {
                    if (!iHistory) iHistory = 2;
                    var callerString = '';
                    for (var i = 0; i < iHistory; i++) {
                        if (i != 0) callerString += ",";
                        callerString += this.getCaller(i);
                    }
                    gAWDebugWin.getDebug().print(sMsg, callerString);
                }
            }
            if (typeof(console) != "undefined") {
                console.log(sMsg);
            }
        },

        printProperties : function (element)
        {
            if (Request.AWJSDebugEnabled) {
                this.findDebugWindow();
                if (gAWDebugWin.getDebug) {
                    gAWDebugWin.getDebug().printProperties(element);
                }
            }
        },

        debugDOM : function ()
        {
            if (Request.AWJSDebugEnabled) {
                this.findDebugWindow();
                if (gAWDebugWin.getDebug) {
                    gAWDebugWin.getDebug().print("starting dom debug");
                    gAWDebugWin.getDebug().printProperties(document);
                }
            }
        },

        getDocElement : function (doc, id)
        {
            return doc.getElementById ? doc.getElementById(id) : doc.all[id];
        },

        openSizedWindow : function (width, height)
        {
            var name = "DebugSize_w" + width + "_h" + height;
            var win = Dom.openWindow('', name, 'scrollbars=no, status=no, resizable=no, width=' + width + ', height=' + height + ', screenX=0, screenY=0, left=0, top=0');
            if (!win) return;

            var content =
                    '<html>\n' +
                    '<head>\n' +
                        //'<script language="JavaScript" src="'+debugJS+'"></script>\n' +
                    '<title>Debug Size ' + width + ' x ' + height + '</title>' +
                    '<style type="text/css">\n' +
                    'A, DIV, P, TD, TH, BLOCKQUOTE, LI, OL, UL, BODY { font:normal 8pt Verdana, Arial, Helvetica, sans-serif;}\n' +
                    'B { font:bold 10pt Verdana, Arial, Helvetica, sans-serif;}\n' +
                    '</style>\n' +
                    '</head>\n' +
                    '<body>\n' +
                    '<b>' + width + ' x ' + height + '</b><br/>\n' +
                    '<i>Content area of this window represents available screen size with taskbar hidden</i>\n' +
                    '</body>\n';
            win.document.write(content);
            win.document.close();
            win.focus();
        },

        viewSource : function ()
        {
            var w = Dom.openWindow('', 'awviewsource', 'resizable=yes,height=660,width=850');
            if (!w) return;
            var source = document.body.parentNode.innerHTML;
            source = source.replace(/&/g, '&amp;');
            source = source.replace(/>/g, '&gt;');
            source = source.replace(/\</g, '&lt;');
            source = source.replace(/\"/g, '&quot;');
            w.document.write('<html><head><title>View Source</title></head><body><textarea cols=100 rows=40>' +
                             source + '</textarea></body></html>');
            w.document.close();
            w.focus();
        },

        DebugWindow : function ()
        {
            if (_AWDebugWindow == null || _AWDebugWindow.closed) {
                var w = Dom.openWindow("", "awdebugWindow", "height=400,width=400,left=0,top=0,resizable=1,scrollbars=1");
                if (!w) {
                    _AWDebugWindow = null;
                }
                else {
                    w.document.close();
                    w.document.write("<html>\n<head>");
                    w.document.write('<script>function clr(){var b=document.body;while(b.lastChild) b.removeChild(b.lastChild);}');
                    w.document.write('function w(s){document.write(s);}</script>');
                    w.document.write("<style>BODY {background-color: #FFEAAA;color: #000000;font: normal 8pt Verdana, Arial, Helvetica, sans-serif;}</style>");
                    w.document.write("</head>\n<body>\n");
                    _AWDebugWindow = w;
                }
            }
            return _AWDebugWindow;
        },

        clearDebugWindow : function () {
            if (Dom.IsIE) _AWDebugWindow = null;
            var w = this.DebugWindow();
            if (!Dom.IsIE) w.clr();
            return w;
        },

        showDebug : function (evt)
        {
            var evt = (evt) ? evt : window.event;
            var spanTag = (evt.srcElement) ? evt.srcElement : evt.target;
            if (evt.ctrlKey && spanTag) {
                // see if we are in the parent chain of the current child.  If not, we're the new deepestChild
                var tag = awDeepestChild;
                while (tag != null) {
                    if (tag == spanTag) {
                        return;  // bail out -- we're in the parent chain!
                    }
                    tag = tag.parentNode;
                }
                if (awDeepestChild == null) setTimeout(this.flushDebug.bind(self), 0);

                awDeepestChild = spanTag;
            }
            return true;
        },

        flushDebug : function () {
            // var w = this.DebugWindow();
            var w = this.clearDebugWindow();
            w.focus();
            w.w('<h3>Component Path!</h3>\n');

            var tag = awDeepestChild;

            while (tag != null) {
                var tagString = tag.id;
                if (tagString && tagString.length > 0 && tagString.indexOf(":") > 0) {
                    w.w('<li>');
                    w.w(tagString);
                    w.w('</li>');
                }
                tag = tag.parentNode;
            }
            awDeepestChild = null;
        },

        prepOpenCI : function ()
        {
            _ComponentInspectorWin = window.open("", "AWComponentPath", _AWCPIWindOpts);
        },

        recurseComputeBounds : function(currId, hierarchy, bounds) {
            var elm = ariba.Dom.getElementById(currId);
            if(elm){
                // found element on the dom, get bounds
                var ntop = ariba.Dom.absoluteTop(elm);
                var nleft = ariba.Dom.absoluteLeft(elm);
                var nright = elm.offsetWidth + nleft;
                var nbottom = elm.offsetHeight + ntop;
                if(nright == 0 && nbottom == 0 )
                    //This is happening for some anchor tags, we don't get the
                    //correct position? need to investigate.
                    return;
                if(ntop < bounds.top)
                    bounds.top = ntop;
                if(nleft < bounds.left)
                    bounds.left = nleft;
                if(nbottom > bounds.bottom)
                    bounds.bottom = nbottom;
                if(nright > bounds.right)
                    bounds.right = nright;                
            }
            else {
                // get child elements and digg further
                var children = hierarchy[currId];
                if(children){
                    for(var i in children){
                        if(children[i] != currId){
                            this.recurseComputeBounds(children[i], hierarchy, bounds);
                        }
                    }
                }
            }
        },

        highlightElementIds : function (targetId, hierarchy) 
        {
            var bounds = {
                top : 0xFFFFF,
                left : 0xFFFFF,
                bottom : 0,
                right : 0
            };
            
            this.recurseComputeBounds(targetId, hierarchy, bounds);
            this.highlightRegion(bounds.top, bounds.left, bounds.bottom, bounds.right);
        },

        highlightElement : function (elm)
        {
            var top = ariba.Dom.absoluteTop(elm);
            var left = ariba.Dom.absoluteLeft(elm);
            var width = elm.offsetWidth;
            var height = elm.offsetHeight;
            this.highlightRegion(top, left, top + height, left + width);           
        },

        highlightRegion : function (top, left, bottom, right)
        {
            if(!this.highlightRect){
                this.highlightRect = document.createElement("div");
                document.body.appendChild(this.highlightRect);

                this.highlightRect.style.backgroundColor = "#BBD6EA";
                this.highlightRect.style.filter = "alpha(opacity=50)";
                this.highlightRect.style.opacity = "0.5";
                this.highlightRect.style.borderColor = "blue";
                this.highlightRect.style.position = "absolute";
                this.highlightRect.style.borderStyle = "solid";
                this.highlightRect.style.borderWidth = 1;
            }
            this.highlightRect.style.width = (right - left + 2) + "px";
            this.highlightRect.style.height = (bottom - top + 2) + "px";
            this.highlightRect.style.left = (left - 2) + "px";
            this.highlightRect.style.top = (top - 2) + "px";
            this.highlightRect.style.visibility = "visible";
        },
        
        // Called by AWXRichClientScriptFunctions.js:gl_handler to see if an event is
        // an invocation of component inspector        
        checkDebugClick : function (evt)
        {
            //Continue on two conditions
            // 1) ALT key is pressed
            // 2) This is a click or mousemove
            if (!evt || !evt.altKey || (evt.type != "click" && evt.type != "mousemove")) {
                if(this.highlightRect && evt.type == "mousemove" && this.highlightRect.style.visibility == "visible") {
                    //If this is a mousemove and there is a highlight on the screen
                    //hide the highlight and return.
                    this.highlightRect.style.visibility = "hidden";
                }
                return false;
            }

            var senderId = null;
            var target = (evt.srcElement) ? evt.srcElement : evt.target;

            //If the target was the highlight, hide the highlight
            //and check for the element under the hightlight region
            if(target == this.highlightRect) {
                this.highlightRect.style.visibility = "hidden";
                target = document.elementFromPoint(evt.clientX, evt.clientY);
            }

            while (target) {
                senderId = target.name;
                if (senderId) break;
                senderId = target.id;
                if (senderId) break;
                if (target.getAttribute) {
                    senderId = target.getAttribute("for");
                    if (senderId) break;
                }
                target = target.parentNode;
            }
            if(senderId) {
                if(evt.type == "mousemove") {
                    this.highlightElement(target);
                }
                else if(evt.type == "click") {
                    senderId = senderId + "&cpDebug=" + ((evt.shiftKey) ? "2" : "1");
                    this.log("** Component Path this.log, id=" + senderId);

                    var openNewWindow = false;
                    // pop window to foreground
                    try {
                        if (!_ComponentInspectorWin || !_ComponentInspectorWin.open
                                || !_ComponentInspectorWin.ariba.Request.invoke)
                        {
                            openNewWindow = true;
                        }
                    } catch (e) {
                        openNewWindow = true;
                    }

                    if (openNewWindow) {
                        _ComponentInspectorWin = window.open("", "AWComponentPath", _AWCPIWindOpts);
                    }

                    // Setup back pointer
                    var mainWindow = window;
                    setTimeout(function() {
                        try {
                            _ComponentInspectorWin._AWXMainWindow = mainWindow;
                        } catch (e) {}
                    }, 2000);

                    // Call on main window with path debug click
                    Request.invoke(null, senderId)

                    return true;
                }
            }
            return false;
        },

        checkSelectionPos : function ()
        {
            // check selection visibility
            var scrollDiv = Dom.getElementById("sourceDiv");
            if (scrollDiv) {
                var selLine = Dom.findChildUsingPredicate(scrollDiv, function (e) {
                    return Dom.hasClass(e, "plsel");
                });
            // alert ("scrollDiv=" + scrollDiv + ", selLine=" + selLine + ", scrollDiv.scrollTop=" + scrollDiv.scrollTop);
                if (selLine && (selLine.offsetTop < scrollDiv.scrollTop
                        || selLine.offsetTop > (scrollDiv.scrollTop + scrollDiv.offsetHeight))) {
                    scrollDiv.scrollTop = Math.max(0, selLine.offsetTop - (scrollDiv.offsetHeight / 2));
                }
            }
        },

        cpiCheckWindowSize : function (w, h)
        {
            try {
                var win = open("", "AWComponentPath", "");
                if (win) win.resizeTo(w, h);
            } catch (e) { /* ignore */
            }
        },

        updateComponentInspector : function (sessionId, openActionId, refreshMainActionId, enabled)
        {
            if (enabled) {
                var win = _ComponentInspectorWin;
                try {
                    if (win && win.open && win.ariba.Request.invoke && win.ariba.Debug._AWCPISession == sessionId) {
                        // try to do an incremental refresh
                        // alert("Going incremental...");
                        win.ariba.Request.invoke.call(win.ariba.Request, null, win.ariba.Debug._AWCPIRefreshId);
                        return;
                    }
                } catch (e) {
                    // alert("window invoke exception:" + e);
                }

            // alert("Not incremental...");
                // full page refresh version
                _ComponentInspectorWin = null;
                try {
                    var url = Request.formatUrl(openActionId);
                    window.name="Main";
                    /*
                    alert("Opening window = " + self + opener  + ", window.name = " + window.name
                           + ", _ComponentInspectorWin.opener = " + _ComponentInspectorWin.opener
                           + ", _ComponentInspectorWin.name = " + _ComponentInspectorWin.name
                           + ", _ComponentInspectorWin.opener.name = " + _ComponentInspectorWin.opener.name);
                           */
                    _ComponentInspectorWin = window.open(url, "AWComponentPath", _AWCPIWindOpts);

                    var mainWindow = window;
                    setTimeout(function() {
                        // alert ("trying to set " + _ComponentInspectorWin + " to " + window);
                        try {
                            if (_ComponentInspectorWin) {
                                _ComponentInspectorWin._AWXMainWindow = mainWindow;
                                _ComponentInspectorWin._AWXRefreshMainActionId = refreshMainActionId;
                            }
                        }
                        catch (e) {}
                    },3000);
                } catch (e) {
                }
            }
        },

        fireMainWindowAction : function (actionId)
        {
            /*
            alert("Opener = " + opener + ", ariba = " + window.opener.ariba
                   + ", opener.name = " + window.opener.name
                   + ", window.name = " + window.name
                   + ", window=opener == " + (window == window.opener));
            */
            if (_AWXMainWindow && _AWXMainWindow.open && _AWXMainWindow.ariba) {
                _AWXMainWindow.ariba.Request.invoke.call(_AWXMainWindow.ariba.Request, null, actionId);
                setTimeout(function() {
                    // this._AWXMainWindow.ariba.Request.redirectRefresh.call(this._AWXMainWindow.ariba.Request);
                }.bind(this), 200);
            } else {
                alert("Debug.fireMainWindowAction() - Trying to invoke in main window, but pointer not set");
            }
        },

        layoutInfoFor : function (elm) {
            var info = document.createElement("div");
            elm.appendChild(info);
            info.className = "posinfo";
            info.style.position = "absolute";
            info.style.zIndex = "249";
            info.style.filter = "alpha(opacity=050);";
            info.style.filter = "progid:DXImageTransform.Microsoft.Shadow(color=#666666,direction=135,strength=8);"
        // info.-moz-opacity:0.6;

            // alert ("Layout for " + elm + " - " + pos);
            // info.initX = info.initY = 0;
            info.style.left = Dom.absoluteLeft(elm) + "px";
            info.style.top = Dom.absoluteTop(elm) + "px";

            info.innerHTML = "clientHeight: " + elm.clientHeight + "<br/>"
                    + "offsetHeight: " + elm.offsetHeight + "<br/>"
                    + "scrollHeight: " + elm.scrollHeight + "<br/>";
        },

        applyToMatchingClass : function (className, func) {
            var all = document.body.getElementsByTagName('*');
            for (var i = all.length - 1; i >= 0; i--) {
                var e = all[i];
                if (e.className && e.className.match(new RegExp("(^|\\s)" + className + "(\\s|$)"))) {
                    func(e);
                }
            }
        },

        setWindowTitle : function (windowTitle) {
            if (windowTitle != '') {
                document.title = windowTitle;
            }
        },

        /*
        _createFloater : function ()  {
            if (!document || !document.body) return null;
            var div  = document.createElement("div");
            // alert("created div!");
            div.style.position="absolute";
            div.style.zIndex="249";
            div.style.border = "2px solid red";
            div.style.backgroundColor = "yellow";
            div.style.left = "600px";
            div.style.top = "10px";
            div.innerHTML="NOT INITIALIZED";
            document.body.appendChild(div);
            return div;
        },

        var _DODiv, _RCounter = 0;
        setInterval(function() {
                if (!_DODiv) _DODiv = _createFloater();
                if (!_DODiv) return;
                _DODiv.innerHTML = "JS: "
                    + (AWDomCompleteCallbackList ? AWDomCompleteCallbackList.length : 0)
                    + ", " + _ScriptLockCount + " -- EvQ: "
                    + (_awEventsPending ? _awEventsPending.length : 0) + ", " + _awPendingLockCount + " -- "
                    + _RCounter++;
        }, 500);
        */

        EOF:0};

    Event.registerBehaviors({
        DOpCI : {
            click : function (elm, evt) {
                if (!Dom.isSafari) Debug.prepOpenCI();
                return false;
            },
            mousedown : function (elm, evt) {
                if (Dom.isSafari) Debug.prepOpenCI();
                return false;
            }
        }
    });

    return Debug;

}());

//****************************************************
// Validation.js
//****************************************************
ariba.DebugValidation = function() {
    // imports
    var Dom = ariba.Dom;
    var Event = ariba.Event;

    // Array to store validation functions
    var AWXValidations = new Array();

    var DebugValidation = {
        addValidation : function (validation)
        {
            AWXValidations[AWXValidations.length] = validation;
        },

        runValidations : function ()
        {
            for (var i = 0; i < AWXValidations.length; i++) {
                this.runValidation(AWXValidations[i]);
            }
        },
        // Main validation driver function
        runValidation : function (validation)
        {
            try {
                validation();
            }
            catch (e) {
                // ValidationException or generic Exception
                var message = e.message ? e.message : e;
                var errorIndicator = top.Dom.getElementById('AWDevErrorIndicator');
                var indicatorContent = Dom.getElementById('AWJSValidationMessage');
                indicatorContent = indicatorContent.cloneNode(false);
                indicatorContent.style.visibility="visible";
                indicatorContent.style.padding="5px 0px 5px 10px";
                if (e.detail) {
                    message += ' <u>[View Detail]</u>';
                    indicatorContent.setAttribute('detail', e.detail);
                }
                indicatorContent.innerHTML = message;
                errorIndicator.appendChild(indicatorContent);
            }
        },
        // View detail action
        showValidationDetail : function (validationDiv)
        {
            var detail = validationDiv.getAttribute('detail');
            if (detail) {
                var w = Dom.openWindow("Validation Detail","awValidationWindow","height=700,width=700,left=0,top=0,resizable=1,scrollbars=1");
                if (!w) return;
                w.document.write("<html><head><style>");
                w.document.write("BODY {background-color: #FFEAAA;color: #000000;font: normal 8pt Verdana, Arial, Helvetica, sans-serif;}");
                w.document.write("</style></head><body>");
                w.document.write(detail);
                w.document.write("</body></html>");
                w.document.close();
                w.focus();
            }
        },

        ValidationException : function (message, detailObject)
        {
            this.message = message;
            var detailString = '';
            var detailProp;
            if (typeof(detailObject) != 'string') {
                for (var i = 0 ; i < detailObject.length; i++) {
                    detailString += '<li>' + detailObject[i] + '</li>';
                }
            }
            else {
                detailString = '<li>' + detailObject + '</li>';;
            }
            this.detail = detailString;
        },

        failValidation : function (message, detail, node)
        {
            throw new this.ValidationException(message, detail, node);
        },

        escapeHTML : function (html)
        {
            html = html.replace(/&/g, '&amp;');
            html = html.replace(/>/g, '&gt;');
            html=  html.replace(/\</g, '&lt;');
            html = html.replace(/\"/g, '&quot;');
            return html;
        },

        /**
           Validation form field addition and removal.
           Invalid html pages can break this.
        */
        removeFormFieldValidation : function ()
        {
            var form = null;
            try {
                var forms = document.forms;
                var length = forms.length;
                for (var index = 0; index < length; index++) {
                    form = forms[index];
                // IE might add the form field as a grandchild.
                    // This is the case if the immediate form child is a <tbody>
                    // Removal fails since we assume the form field to be a child.
                    Dom.addFormField(form, "testField1", "testValue1");
                    Dom.removeFormField(form, "testField1");
                // Firefox removes the form field reference from the DOM the first time,
                    // but not from the form array.  We null it out in awRemoveFormField
                    // Second add/remove should be okay.
                    Dom.addFormField(form, "testField1", "testValue1");
                    Dom.removeFormField(form, "testField1");
                }
            }
            catch (e) {
                if (form) {
                    this.failValidation('Form field validation failed',
                            ['Form with id is broken: ' + form.id,
                                'View source and find form with id.  Look for invalid html around the form. ' +
                                'Example, &lt;form&gt; between &lt;table&gt; and &lt;tr&gt;, missing &lt;td&gt; inside &lt;tr&gt;.',
                                '<span onMouseDown="window.opener.Debug.viewSource();"><u>View Source</u></span>']);
                }
            }
        },

    EOF:0};

    if (!Dom.isSafari) {
        DebugValidation.addValidation(DebugValidation.removeFormFieldValidation.bind(DebugValidation));
    }

    Event.registerUpdateCompleteCallback(DebugValidation.runValidations.bind(DebugValidation));

    return DebugValidation;
}();
