/*
    Event.js  -- registries for event handlers

    Includes "behavior" mechanism for associating handlers with HTML elements
    and several event loop callbacks (refresh callback, update complete callback, ...)
    as well as global handlers for window resize, scroll, etc
 */

ariba.Event = function() {
    // imports
    var Util = ariba.Util;
    var Debug = ariba.Debug;
    var Dom = ariba.Dom;

    // private vars

    var AWDomCompleteCallbackList;
    var AWDomCompleteCallbackListArgs;
    var AWOnRefreshCallbacks;
    var AWVBScriptOnLoad;
    var _ScriptLockCount = 0;
    var _IdsByEvent = new Object();
    
    // Should fire events -- false on first / native pass.  True when we do our own
    // walk
    var _FE = false;
    var EventsEnabled = true;
    var AWDocHandler = new Object();

    var AWPrintWindowName = "AWPrintPage";
    var AWMouseDown = 'AWMouseDown';
    var AWMouseUp = 'AWMouseUp';

    /**
     Some click events are fired without prior mousedown/mouseup events.
     We keep of mouse events on the target element, and skip firing
     the click event if there is no prior mousedown and mouseup event.
     */

    // need in global scope for assignment from eval below...
    var _LastFunc = null;

    // non-recursive

    var _FakeEvent = function () {
        this.stopPropagation = function () {
        };
        this.preventDefault = function () {
        };
    }

    // fire a simulated event on an element (fabricate event)

    // Event queue
    var _awEventsPending = null, _awEventsPhase2Pending = null;
    var _awPendingLockCount = 0;

    var AWWindowResizeCallbackList;
    var _awPrevWS, _awProcessedWS, _awWSId, _awWSTime;
    var AWWindowFixedCallbackList;
    var AWWindowScrollCallback = null;
    var AWWindowOnScrollTimeout = null;
    var AWOrigDocumentOnMouseDown = window.document.onmousedown;

    // for the final doc ready check
    var _docReadyLockCount = 0;

    var Timeouts = new Object();

    var Event = {
        // All registered behaviors
        behaviors : {},

        registerBehaviors : function (map)
        {
            Util.extend(this.behaviors, map);
        },

        // Note: OnRefreshCallbacks are not cleared and will be executed for the page on
        //       every page load (incremental or full) until the top frame is reloaded.        
        registerRefreshCallback : function (f)
        {
            if (!AWOnRefreshCallbacks) {
                AWOnRefreshCallbacks = new Array(f);
            }
            else {
                AWOnRefreshCallbacks[AWOnRefreshCallbacks.length] = f;
            }
        },

        unregisterRefreshCallback : function (f)
        {
            Util.arrayRemoveMatching(AWOnRefreshCallbacks, f);
        },

        registerVBScriptRefreshCallback : function (name)
        {
            if (Dom.IsIE) {
                this.registerRefreshCallback(function() {
                    Event.GlobalEvalVBScript(name);
                });
            }
        },

        // Special case this because we can have only one instance of this
        registerVBScriptOnLoad : function (name)
        {
            Debug.log("awDomRegisterVBScriptOnLoad -- registering: " + name);
            AWVBScriptOnLoad = name;
        },

        refreshIncrementNesting : function ()
        {
            _ScriptLockCount++;
        },

        docReadyIncrementNesting : function ()
        {
            _docReadyLockCount++;
        },

        // called from main window during incremental update (AWRefreshRegion and AWLazyDiv)
        // expectation is that all content to be run is copied over to the main window
        // before this is run
        notifyRefreshComplete : function ()
        {
            this.eventLock();
            if (_ScriptLockCount > 0) {
                Debug.log("refreshComplete() -- deferred (" + _ScriptLockCount + ")");
                _ScriptLockCount--;
            // need to keep a lock (defer the unlock) so that we don't flush until we're really done...
                this.registerUpdateCompleteCallback(this.eventUnlock.bind(this));
                return false;
            }

            Debug.log("refreshComplete() -- executing...");
            // run any code that needs to be run after all updates are complete
            // (ie, datatable.postLoad() method)
            var i;
            if (AWDomCompleteCallbackList) {
                for (i = 0; i < AWDomCompleteCallbackList.length; i++) {

                    //debug("evaluating: " + Debug.getMethodName(AWDomCompleteCallbackList[i]));

                    try {
                        if (AWDomCompleteCallbackListArgs[i]) {
                            AWDomCompleteCallbackList[i].apply(this, AWDomCompleteCallbackListArgs[i]);
                        }
                        else {
                            AWDomCompleteCallbackList[i]();
                        }
                    }
                    catch (e) {
                        var msg = "refreshComplete: Exception evaluating: "
                                + AWDomCompleteCallbackList[i].toString() + "\n\n: " + e;
                        Debug.log(msg);
                        if (ariba.Request.AWDebugEnabled) {
                            alert(msg);
                        }
                    }

                // If we locked durring this function, then discard all items that we've run and bail out
                    if (_ScriptLockCount) {
                        Debug.log("refreshComplete() -- Script block (" + _ScriptLockCount
                                + ") while executing " + (i + 1) + " of " + AWDomCompleteCallbackList.length);
                        while (i-- >= 0) {
                            AWDomCompleteCallbackList.shift();
                        }
                        _ScriptLockCount--;
                    // need to keep a lock (defer the unlock) so that we don't flush until we're really done...
                        this.registerUpdateCompleteCallback(this.eventUnlock.bind(this));
                        return false;
                    }
                }
                Debug.log("refreshComplete() -- done...");
                AWDomCompleteCallbackList = null;
            }

        // execute on every page load (incremental or full page)
            if (ariba.Request.clearCancelRequestDelayHandle) {
                ariba.Request.clearCancelRequestDelayHandle();
            }
            if (AWOnRefreshCallbacks) {
                for (i = 0; i < AWOnRefreshCallbacks.length; i++) {
                    // try {
                        AWOnRefreshCallbacks[i]();
                /*    }

                    catch (e) {
                        alert("Exception in Refresh Callback: " + e.message + AWOnRefreshCallbacks[i]);
                    }
                    */
                }
            }

            if (AWVBScriptOnLoad && Dom.IsIE) {
                try {
                    this.GlobalEvalVBScript(AWVBScriptOnLoad);
                }
                catch (e) {
                    alert("Exception in VBScript OnLoad Callback: " + e.message + AWVBScriptOnLoad);
                }
            }

        // allow new requests to come in
            //debug("action pending: false");
            //document.body.style.cursor = "default";
            this.eventUnlock();

            return this.notifyDocReady();
        },

        notifyDocReady : function ()
        {
            if (_docReadyLockCount > 0) {
                Debug.log("waitForDocReady() -- deferred (" + _docReadyLockCount + ")");
                _docReadyLockCount--;

                return false;
            }

            Debug.log("waitForDocReady() -- executing...");
            this.invokeRegisteredHandlers("onDocReady");
            Debug.log("waitForDocReady() -- done...");

            return true;
        },

        evalJSSpan : function (id) {
            var elm = Dom.getElementById(id);
            if (elm) {
                var script = Dom.getInnerText(elm);
                eval(script);
            }
        },

        registerHandler : function (id, eventNames, func) {
            var list = eventNames.split(/\s+/);
            for (var i = 0; i < list.length; i++) {
                var eventName = list[i];
                Debug.log("Registering handler for: " + eventName + " -- " + id);
                var map = _IdsByEvent[eventName];
                if (!map) {
                    map = new Object();
                    _IdsByEvent[eventName] = map;
                }
                map[id] = func || id;
            }
        },

        invokeRegisteredHandlers : function (eventName) {
            var map = _IdsByEvent[eventName];
            if (map) {
                // Debug.log ("Invoking Handlers: " + eventName);
                for (var id in map) {
                    var func = map[id];
                    // Use func.call(this) instead of func(); otherwise a callback registered
                    // from a plugin in FF2 generates a security exception ("Permission denied
                    // to get property Function.__parent__").
                    try {
                         if (typeof(func) == 'function') func.call(this); else this.evalJSSpan(id);
                    } catch (exp) {
                        // Here we might be in FF4 selenium mode where callbacks
                        // can't be passed from plugin to resident page. We need to use
                        // standard events notifications
                        // In IE6/7/8 (other?) the API createEvent/dispatchEvent is not supported
                        // why we are adding a try catch
                        try {
                            if (ariba.Request.AWDebugEnabled) {
                                var event = document.createEvent("UIEvents");
                                event.initUIEvent(eventName, true, true, window, 1);
                                window.dispatchEvent(event);
                            }
                        } catch (exp2) {
                            //does nothing
                        }
                    }
                }
            }
        },

        registerUpdateCompleteCallback : function (method, args)
        {
            //debug(Debug.getMethodName(method));
            if (!AWDomCompleteCallbackList) {
                AWDomCompleteCallbackList = new Array();
                AWDomCompleteCallbackListArgs = new Array();
            }
            AWDomCompleteCallbackList[AWDomCompleteCallbackList.length] = method;
            AWDomCompleteCallbackListArgs[AWDomCompleteCallbackListArgs.length] = args;
        },

        addEvent : function (node, event, handler) {
            if (node.addEventListener) node.addEventListener(event.substring(2), handler, false);
            else if (node.attachEvent) node.attachEvent(event, handler);
        },

        removeEvent : function (node, event, handler) {
            if (node.removeEventListener) node.removeEventListener(event.substring(2), handler, false);
            else if (node.detachEvent) node.detachEvent(event, handler);
        },

        shouldBubble : function (evt)
        {
            return !evt.awCancelBubble;
        },

        // overridded by Input.js
        modallyDisabled :function (target)
        {
            return false;
        },

        // overridded by Input.js
        selectFirstText :function ()
        {
        },

        enableEvents : function ()
        {
            EventsEnabled = true;
        },

        disableEvents : function ()
        {
            EventsEnabled = false;
        },

        updateDocHandler : function (eventType, docHandler)
        {
            var origDocHandler = AWDocHandler[eventType];
            AWDocHandler[eventType] = docHandler;
            return origDocHandler;
        },

        getDocHandler : function (eventType)
        {
            return AWDocHandler[eventType];
        },

        // Called when events bubble up to our global handler on the document.
        // (We attempts to ensure that no handlers are enabled other than the doc handler so
        // that all events are actually dispatched here).
        // We then do our own event dispatch, this time:
        //      1) enabling _FE flag to enable "regular" handlers (which we disabled via awhandler wrapper)
        //      2) checking for x<event> (e.g. "xmouseover") handlers that were renamed by AW so that
        //         the stayed "out of the way" during the initial browser bubble.
        //      3) look for behavior (bh="...") attribute and see if the associate behavior object implements the event

        gl_handler : function (e) {
            // don't recurse into ourselves
            if (_FE) return true;
        // Events can be fired even when the doc is not fully loaded.
            if (document.readyState && document.readyState != "complete") return true;
            if (!Dom.IsIE && !e) return true;  // Mozilla bail out...
            if (!EventsEnabled) return false;
            if (window.name == AWPrintWindowName) return true;

            var evt = (e) ? e : event;
            var target = (evt.srcElement) ? evt.srcElement : evt.target;
            if (ariba.Request.AWDebugEnabled && Debug.checkDebugClick(evt)) { Event.cancelBubble(evt); return true; }
            if (this.modallyDisabled(target)) {
                // kill focus attempts outside the modal
                if ((evt.type == "activate" || evt.type == "focus" ||
                     evt.type == "mousedown" || evt.type == "click"||
                     evt.type == "keydown") && target != window) {
                    this.selectFirstText();
                }
                return true;
            }

            _FE = true;
            try {
                if (this.handleMouseEvent(target, evt)) {
                    // logStatus(evt.type);
                    // if (evt.type=="mousedown") Debug.log("mousedown", target, target.getAttribute("bh"));
                    var ret = this.fireBehaviors(target, evt, ("on" + evt.type), ("x" + evt.type));
                }
            } catch (ex) {
                ret = true;
            } finally {
                _FE = false;
            }
            return ret;
        },

        handleMouseEvent : function (target, evt)
        {
            var shouldFireEvent = true;
            if (target && target.setAttribute) {
                var evtType = evt.type;
                if (evtType == 'mousedown') {
                    target.setAttribute(AWMouseDown, "true");
                    target.removeAttribute(AWMouseUp);
                }
                else if (evtType == 'mouseup') {
                    target.setAttribute(AWMouseUp, "true");
                }
                else if (evtType == 'mouseout') {
                    target.removeAttribute(AWMouseDown);
                    target.removeAttribute(AWMouseUp);
                }
                else if (evtType == 'click') {
                    shouldFireEvent =
                    target.getAttribute(AWMouseDown) && target.getAttribute(AWMouseUp);
                    target.removeAttribute(AWMouseDown);
                    target.removeAttribute(AWMouseUp);
                }
            }
            return shouldFireEvent;
        },

        _elementInvoke : function (elm, evt, onName, xName) {
            var ret = true;
            var func;
            var evtType = evt.type;
            if (elm == window.document) {
                // run doc handler if any
                var docHandler = AWDocHandler[evtType];
                if (docHandler) {
                    return docHandler(evt);
                }
            }

        // check for local handler
            var handler = elm[onName];
        // note -- handler.call is undefined for some VBScript-based handlers!
            if (handler && (handler.call != null)) {
                // Debug.logEvent(evt, elm,  "Local Handler: " + handler.toString());
                ret = handler.call(elm, evt);
            }
            else if (elm.tagName) {
                // check for escaped inline handler (i.e. "xmouseover");
                handler = elm.getAttribute(xName);
                if (handler) {
                    ret = this.handleInline(handler, evt, elm);
                }
                else if (func = this.bhHandler(elm, evt.type)) {
                    // Debug.logEvent(evt, elm, "Behavior Handler: " + func.toString());
                    ret = func(elm, evt);
                }
                else if (evt.type == 'click') {
                    var sourceElm = this.eventSourceElement(evt);
                    // Fire the click event for input element that the label is for.
                    if (elm.tagName == 'LABEL' &&
                        sourceElm.type != "checkbox" &&
                        sourceElm.type != "radio" &&
                        sourceElm.tagName != "SELECT" &&
                        sourceElm.tagName != "A") {
                        var forId = elm.htmlFor;
                        if (forId) {
                            var target = Dom.getElementById(forId);
                            if (target && !target.disabled) {
                                // set radio button value
                                if (target.type == 'radio') {
                                    target.checked = true;
                                    ret = false;
                                    this.preventDefault(evt);
                                }
                                else if (target.type == 'checkbox') {
                                    target.checked = !target.checked;
                                    ret = false;
                                    this.preventDefault(evt);
                                }
                                else if (target.tagName == 'SELECT') {
                                    target.focus();
                                    ret = false;
                                    this.preventDefault(evt);
                                }
                                handler = target.getAttribute('xclick');
                                if (handler) {
                                    this.handleInline(handler, evt, target);
                                    ret = false;
                                    this.preventDefault(evt);
                                }
                            }
                        }
                    }
                }
            }

            return ret;
        },

        // recursive walk up the element hierarchy to do our enhanced event dispatch
        fireBehaviors : function (elm, evt, onName, xName) {
            if (!elm) return true;

            var ret = this._elementInvoke(elm, evt, onName, xName);
            //Debug.logEvent(evt, elm, this.shouldBubble(evt));
            return (this.shouldBubble(evt) && (elm != window.document))
                    ? (this.fireBehaviors(elm.parentNode, evt, onName, xName) && ret)
                    : ret;
        },

        handleInline : function (handler, evt, elm)
        {
            var ret = true;
        // Debug.logEvent(evt, elm, "Inline Handler: " + handler.toString());
            // Dynamic dispatch with 'this' and 'event' set correctly
            eval("_LastFunc = function(event) {" + handler + "}");
            ret = _LastFunc.call(elm, evt);
            _LastFunc = null;
            return ret;
        },

        elementInvoke : function (elm, evtName) {
            var evt = new _FakeEvent();
            evt.type = evtName;
            evt.srcElement = evt.target = elm;

            return this._elementInvoke(elm, evt, ("on" + evtName), ("x" + evtName));
        },

        // return handler func for given elm/type
        bhHandler : function (elm, type) {
            var bhName = elm.getAttribute("bh");
            if (bhName) {
                var handler = this.behaviors[bhName];
                if (handler) {
                    if (!handler.didInit) this.behaviorInit(handler);
                    return handler[type];
                }
            }
            return null;
        },
        
        // check if element has a handler for the given event type
        hasHandler : function (elm, type) {
            return elm["on" + type] || elm.getAttribute("x" + type) || this.bhHandler(elm, type);
        },

        behaviorInit : function (handler) {
            // copy prototype functions, if any
            handler.didInit = true;
            var proto = handler.prototype;
            for (var p in proto) {
                if (!handler[p]) handler[p] = proto[p];
            }

        // if IE, remap focus,blur to activate,deactivate
            if (Dom.IsIE) {
                var f;
                if (f = handler["focus"]) handler["activate"] = f;
                if (f = handler["blur"]) handler["deactivate"] = f;
            }
        },

        eh_stop : function (eml, evt) {
            this.cancelBubble(evt);
            return false;
        },

        eventEnqueue : function (target, event, isP2)
        {
            if (!_awEventsPending) _awEventsPending = [];
            if (!_awEventsPhase2Pending) _awEventsPhase2Pending = [];
            var q = isP2 ? _awEventsPhase2Pending : _awEventsPending;
        // de-dup -- if there's already an entry for this target, we'll move it to the end
            Util.arrayRemoveMatching(q, target, function (e) {
                return e[0]
            });
            q.push([target, event]);

            if (_awPendingLockCount == 0) this.eventsFire();
        },

        eventLock : function ()
        {
            _awPendingLockCount++;
        // Debug.log("LOCK: " + _awPendingLockCount + " -- pending events = " + (_awEventsPending && _awEventsPending.length));
        },

        eventUnlock : function ()
        {
            _awPendingLockCount--;
        // Debug.log("UNLOCK: " + _awPendingLockCount + " -- pending events = " + (_awEventsPending && _awEventsPending.length));
            if (_awPendingLockCount == 0) this.eventsFire();
        },

        eventsFire : function ()
        {
            _awPendingLockCount++;
            var didFire;
        // Process all _awEventsPending first, then _awEventsPhase2Pending
            do {
                didFire = false;
                var q = (_awEventsPending && _awEventsPending.length) ? _awEventsPending : _awEventsPhase2Pending;
                if (q && q.length) {
                    didFire = true;
                    var entry = q[0];
                // Fire!
                    //try {
                        entry[0](entry[1]);
                    /*
                    } catch (e) {
                        alert("_awEventsFire: Exception evaluating: " + entry[0].toString() + ": " + e.description);
                    }
                    */
                    this._arrayShift(q);
                }
            } while (didFire);
            _awPendingLockCount--;
            if (_awPendingLockCount != 0) alert("Unbalanced lock count!");
        // Debug.log ("ALL UNLOCKED");
        },

        _arrayShift : function (a)
        {
            return (a && a.length) ? a.splice(0, 1)[0] : null;
        },

        /*
            Global window resize handler
        */
        /* More advanced sizing approach:
            - If DOM update, then size immediately (i.e. clear last sized setting)
            - If other cause, see if we really changed
                - wait for no change for 100ms before acting (to avoid chasing user drag)
        */
        _onWindowResize : function (event, delay)
        {
            // only act if the size has actually changed and we don't already have a handler pending
            var ws = Dom.getWindowSize();
            if (!_awWSId && (!_awPrevWS || (_awPrevWS[0] != ws[0]) || (_awPrevWS[1] != ws[1]))) {
                delay = delay || 100;
                if (!_awPrevWS) _awPrevWS = [-1,-1];
                Debug.log("_awOnWindowResize -- delay:" + delay
                        + " -- _awPendingLockCount: " + _awPendingLockCount
                        + " -- size: " + ws[0] + ", " + ws[1] + "// "
                        + _awPrevWS[0] + ", " + _awPrevWS[1]);
                _awPrevWS = ws;
                if (_awPendingLockCount) {
                    this.eventEnqueue(this.onWindowResize.bind(this));
                } else {
                    if (_awWSId) clearTimeout(_awWSId);
                    _awWSId = setTimeout(this.onWindowResize.bind(this), delay);
                    _awWSTime = (new Date()).getTime() + delay;
                }
            }
        },

        onWindowResize : function (force)
        {
            var ws = Dom.getWindowSize();
            var delay = _awWSTime - (new Date()).getTime();
            Debug.log("Resize invoked: " + delay + (force ? " -- FORCE" : ""));
        // if size changed again, then wait...
            if (force) _awPrevWS = ws;
            if ((_awPrevWS && ((_awPrevWS[0] != ws[0]) || (_awPrevWS[1] != ws[1]))) || (!force && delay > 0)) {
                _awWSId = null;
                Debug.log("Bailing out / rescheduling!")
                this._onWindowResize(null, delay > 0 ? delay : 0);
                return;
            }
            Debug.log("Enqueuing resize: " + _awPendingLockCount + " / "
                    + (AWDomCompleteCallbackList ? AWDomCompleteCallbackList.length : 0));

        // keep track of the last one actually acted on, and only flush
            // if we have a real change
            if (!_awProcessedWS || force) _awProcessedWS = [-1,-1];
            if ((_awProcessedWS[0] != ws[0]) || (_awProcessedWS[1] != ws[1])) {
                _awProcessedWS = ws;
                this.wSFlush();
            }
            _awWSId = null;
        },

        forceOnWindowResize : function () {
            this.onWindowResize(true);
        },

        wSFlush : function ()
        {
            var ws = Dom.getWindowSize();
            Debug.log("^^^^^ Resize!!! -- " + ws[0] + ", " + ws[1] + " // "
                    + _awPrevWS[0] + ", " + _awPrevWS[1] + " ^^^^^");

            if (AWWindowResizeCallbackList) {
                for (var i = 0; i < AWWindowResizeCallbackList.length; i++) {
                    this.eventEnqueue(AWWindowResizeCallbackList[i]);
                }
            }
        // These go last
            this.fireWindowFixedCallback();
        },

        registerOnWindowFixed : function (callback)
        {
            if (!AWWindowFixedCallbackList) AWWindowFixedCallbackList = [];
            for (var i = 0; i < AWWindowFixedCallbackList.length; i++) {
                if (AWWindowFixedCallbackList[i] == callback) return;
            }
            AWWindowFixedCallbackList.push(callback);
        },

        fireWindowFixedCallback : function ()
        {
            if (AWWindowFixedCallbackList) {
                for (var i = 0; i < AWWindowFixedCallbackList.length; i++) {
                    this.eventEnqueue(AWWindowFixedCallbackList[i], null, true);
                }
            }

        },

        unregisterOnWindowFixed : function (method)
        {
            Util.arrayRemoveMatching(AWWindowFixedCallbackList, method);
        },

        _fireWindowCallback : function ()
        {
            if (AWWindowFixedCallbackList) {
                for (var i = 0; i < AWWindowFixedCallbackList.length; i++) {
                    AWWindowFixedCallbackList[i]();
                }
            }
        },

        registerOnWindowResize : function (method)
        {
            // Debug.log("registering windowresize " + Debug.getMethodName(method));
            if (!AWWindowResizeCallbackList) {
                AWWindowResizeCallbackList = new Array();
            }
            AWWindowResizeCallbackList[AWWindowResizeCallbackList.length] = method;
            return method;
        },

        unregisterOnWindowResize : function (method)
        {
            // Debug.log("UN-registering windowresize " + Debug.getMethodName(method));
            Util.arrayRemoveMatching(AWWindowResizeCallbackList, method);
        },

        registerWindowOnScroll : function (callback)
        {
            if (!AWWindowScrollCallback) AWWindowScrollCallback = [];
            for (var i = 0; i < AWWindowScrollCallback.length; i++) {
                if (AWWindowScrollCallback[i] == callback) return;
            }
            AWWindowScrollCallback.push(callback);
        },

        windowOnScroll : function ()
        {
            this.fireWindowOnScroll();
        },

        windowOnMouseWheel : function ()
        {
            this.fireWindowOnScroll();
        },

        fireWindowOnScroll : function ()
        {
            // Fire only one per event loop
            if (AWWindowOnScrollTimeout) {
                clearTimeout(AWWindowOnScrollTimeout);
            }
            AWWindowOnScrollTimeout = setTimeout(this._fireWindowOnScroll.bind(this), 0);
        },

        _fireWindowOnScroll : function ()
        {
            if (AWWindowScrollCallback) {
                for (var i = 0; i < AWWindowScrollCallback.length; i++) {
                    AWWindowScrollCallback[i]();
                }
            }
        },

        enableDocumentClick : function (func)
        {
            // this method makes it so a click
            // on the background makes the menu go away
            AWOrigDocumentOnMouseDown =
                this.updateDocHandler("mousedown", func.bindEventHandler(this));
        },

        disableDocumentClick : function ()
        {
            if (AWOrigDocumentOnMouseDown) {
                this.updateDocHandler("mousedown", AWOrigDocumentOnMouseDown);
                AWOrigDocumentOnMouseDown = null;
            }
        },
        // Walk parent chain looking for handlerName
        notifyParents : function (src, handlerName)
        {
            var node = src;
            while (node && node.nodeType == 1) {
                try {
                    var handler = node[handlerName];
                    if (handler) handler(node, src);
                    node = node.parentNode;
                } catch(ex) {
                }
            }
        },

        // Would like to get rid of this....
        GlobalEvalVBScript : function (str)
        {
            alert("GlobalEvalVBScript is IE only...");
        },

        setTimeout : function (elm, func, delay)
        {
            if (elm && elm.id) {
                this.clearTimeout(elm);
                var timeout = setTimeout(func, delay);
                Timeouts[elm.id] = timeout;
            }
        },

        clearTimeout : function (elm)
        {
            if (elm && elm.id) {
                var timeout = Timeouts[elm.id];
                if (timeout) {
                    clearTimeout(timeout);
                    delete Timeouts[elm.id]
                }
            }
        },

        EOF:0};

    //
    // iPad - specific methods
    //
    if (Dom.isIPad) Util.extend(Event, function () {
        return {
            enableDocumentClick : function (func)
            {
            },

            disableDocumentClick : function ()
            {
            },

        EOF:0};
    }());

    //
    // IE - specific methods
    //
    if (Dom.IsIE) Util.extend(Event, function () {
        return {
            keyCode : function (mevent)
            {
                return mevent.keyCode;
            },
            /////////////////////////
            // Events
            /////////////////////////

            cancelBubble : function (mevent, allowDefault)
            {
                if (!Util.isNullOrUndefined(mevent)) {
                    mevent.cancelBubble = true;
                // used by gl_handler
                    mevent.awCancelBubble = true;

                    if (!allowDefault) {
                       Event.preventDefault(mevent);
                    }
                }
            },

            eventSourceElement : function (mevent)
            {
                return mevent.srcElement;
            },

            preventDefault : function (mevent)
            {
                mevent.returnValue = false;
            },

            // Would like to get rid of this....
            GlobalEvalVBScript : function (str)
            {
                window.GlobalEvalVBScript(str);
            },

        EOF:0};
    }());

    //
    // Mozilla - specific methods
    //
    if (!Dom.IsIE) Util.extend(Event, function () {
        return {
            keyCode : function (mevent)
            {
                return mevent.which;
            },

            /////////////////////////
            // Events
            /////////////////////////
            cancelBubble : function (mevent, allowDefault)
            {
                if (mevent) {
                    mevent.stopPropagation();
                    // should really fix all usages to call 
                    // preventDefault separately if required
                    if (!allowDefault) {
                       mevent.preventDefault();
                    }
                // used by gl_handler
                    mevent.awCancelBubble = true;
                }
            },

            eventSourceElement : function (mevent)
            {
                return mevent.target;
            },

            preventDefault : function (mevent)
            {
                mevent.preventDefault();
            },


        EOF:0};
    }());

    // behavior.StopPropagation -- cancel bubble on any events that would percollate up
    var bh = Event.eh_stop.bind(Event);
    Util.extend(Event.behaviors, {
        StopPropagation : {
            click: bh,
            keydown: bh,
            keyup: bh,
            keypress: bh,
            mousedown : bh,
            mouseup : bh,
            mousein : bh,
            mouseover : bh,
            mousemove : bh,
            mouseout : bh,
            focus : bh,
            blur : bh
        }
    });

    // wrap global handlers so that we call our handler first and only call the
    // supplied handler on the re-percollate phase.

    Function.prototype.bindEventHandler = function() {
        var __method = this, a = Util.toArray(arguments), obj = a.shift();
        return function(evt) {
            return (_FE) ? __method.apply(obj, Util.concatArr([evt || window.event], a))
                    : true;
        }
    }

    /*
    Function.prototype.bindDocHandler = function() {
        var __method = this, a = Util.toArray(arguments), obj = a.shift();
        return function(evt) {
            return (_FE) ? __method.apply(obj, Util.concatArr([evt || window.event], a))
                    : Event.gl_handler.apply(Event, Util.concatArr([evt || window.event], a));
        }
    }
    */

    //********************************************************************
    // Global Handlers (for Behavior system)  ---------
    if (window == ariba.awCurrWindow) {
        var bHandler = function (evt) {
            return Event.gl_handler(evt);
        }
        var d = window.document;
        if (Dom.IsIE) {
            d.onmousein = bHandler;
            d.onmouseover = bHandler;
            d.onmouseout = bHandler;
            d.onmousedown = bHandler;
            d.onmouseup = bHandler;
            d.onmousemove = bHandler;
            d.onclick = bHandler;
            d.onkeydown = bHandler;
            d.onkeyup = bHandler;
            d.onkeypress = bHandler;
            d.ondeactivate = bHandler;
            window.onfocus = function(e) {
                Event.invokeRegisteredHandlers("onfocusin")
            };
            window.onblur = function(e) {
                Event.invokeRegisteredHandlers("onblur")
            };
            // These do not bubble, so we can't catch them here...
            // window.document.onchange = gl_handler;
            // window.document.onmousewheel = gl_handler;
        } else {
            d.addEventListener("mousein", bHandler, false);
            d.addEventListener("mouseover", bHandler, false);
            d.addEventListener("mouseout", bHandler, false);
            d.addEventListener("mousedown", bHandler, false);
            d.addEventListener("mouseup", bHandler, false);
            d.addEventListener("mousemove", bHandler, false);
            d.addEventListener("click", bHandler, false);
            d.addEventListener("keydown", bHandler, false);
            d.addEventListener("keyup", bHandler, false);
            d.addEventListener("keypress", bHandler, false);
            d.addEventListener("blur", bHandler, false);
            d.addEventListener("focus", bHandler, false);
            window.addEventListener("focus", function(e) {
                Event.invokeRegisteredHandlers("onfocusin")
            }, false);
            window.addEventListener("blur", function(e) {
                Event.invokeRegisteredHandlers("onblur")
            }, false);

            // Since IE doesn't bubble this one we don't trap it
            // d.addEventListener("change", gl_handler, false);
            // Not implemented by Mozilla
            // window.addEventListener("mousewheel", gl_handler, false);
        }

        function _bindEv (func, obj) {
            return function (evt) {
                return func.apply(obj, [evt || window.event])
            }
        }

        window.onscroll = _bindEv(Event.windowOnScroll, Event);
        window.onmousewheel = _bindEv(Event.windowOnMouseWheel, Event);
        window.onresize = _bindEv(Event._onWindowResize, Event);
        // Event.registerRefreshCallback(awForceOnWindowResize);

        // Register hook into AWClientSideScript executeOn="onupdate"
        Event.registerRefreshCallback(function() {
            Event.invokeRegisteredHandlers("onupdate");
        });
    }

    // Bindings for even handlers
    /*
    Function.prototype.bindEventHandler = function() {
        var __method = this, a = arguments, obj = Array.shift(a);
        return function(evt) {
            return __method.apply(obj, Array.concat([evt || window.event], arguments));
        }
    }
    */
    return Event;
}();
