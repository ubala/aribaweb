/*
    Input.js - routines for managing user input

    Includes managing modality -- i.e. blocking input when panels are in play -- as
    well as showing "wait alerts" -- showing that client is busy and input is blocked.
*/

ariba.Input = function() {
    // imports
    var Util = ariba.Util;
    var Event = ariba.Event;
    var Debug = ariba.Debug;
    var Dom = ariba.Dom;

    // private vars

    // Id for the setTimeout function -- allows for calling clearTimeout()
    var AWWaitTimeoutId;
    // The div where the barber pole image displays

    var AWWaitAlertDiv = null;
    var AWWaitAlertTimeoutId = null;

    // Holds the window's onscroll and onresize functions while alert active
    var AWOriginalWindowOnScroll = null;
    var AWOriginalWindowOnResize = null;
    var AWShowWaitCursorDisabled = false;
    var AWShowWaitAlertDisabled = false;
    var AWIsWaitAlertActive = false;

    var AWCoverDiv = null, AWPrevFocusId = null;
    var AWActiveElementId = null;
    var AWTextFocusId = null;
    var AWAllowSelectFirstText = false;
    var AWModalPanelId = null;
    var _disableShowUntil = 0;

    var Input = {

        // Public Globals
        AWWaitAlertMillis : 2000,
        AWWaitMillis : 20000,
        AWAutomationTestModeEnabled : false,

        keyDownEvtHandler : function (evt)
        {
            var sourceElm = Event.eventSourceElement(evt);
            var preventDefault = Input.modallyDisabled(sourceElm);
            if (preventDefault) {
                Event.cancelBubble(evt);
            // remove focus from source element
                window.focus();
            }
            return !preventDefault;
        },

        setShowWaitCursorDisabled : function (yn)
        {
            AWShowWaitCursorDisabled = yn;
        },

        showWaitCursor : function ()
        {
            if (this.AWAutomationTestModeEnabled) {
                window.status = "Processing request ...";
            }

            if (AWShowWaitCursorDisabled) {
                return;
            }
            AWWaitAlertDiv = document.getElementById('awwaitAlertDiv');
            if (AWWaitAlertDiv != null) {
                AWWaitAlertDiv.style.display = "none";
                AWWaitAlertDiv.style.visibility = "";
            }

            _disableShowUntil = (new Date()).getTime() + 500;
            this.disableInput(true);

            if (!AWShowWaitAlertDisabled) {
                AWWaitAlertTimeoutId = setTimeout(this.showWaitAlert.bind(this), this.AWWaitAlertMillis);
                AWWaitTimeoutId = setTimeout(this.hideWaitCursor.bind(this), this.AWWaitAlertMillis + this.AWWaitMillis);
            }
        },

        updateWaitMessage : function (message)
        {
            if (!AWIsWaitAlertActive) this.showWaitAlert();
            var span = Dom.getElementById("awwaitMessage");
            if (span) span.innerHTML = message;
        },

        hideWaitCursor : function ()
        {
            if (AWShowWaitCursorDisabled) {
                return;
            }
        // When this is called explicitly, we need to avoid the call
            // which is pending from the setTimeout() in awshowWaitCursor
            clearTimeout(AWWaitTimeoutId);
            clearTimeout(AWWaitAlertTimeoutId);
            this.enableInput();
            this.hideWaitAlert();
        },

        showWaitAlert : function ()
        {
            if ((new Date()).getTime() < _disableShowUntil) return;

            clearTimeout(AWWaitAlertTimeoutId);
            if (!AWIsWaitAlertActive && !AWShowWaitAlertDisabled) {
                AWIsWaitAlertActive = true;
                if (AWWaitAlertDiv != null) {
                    AWWaitAlertDiv.style.display = "";
                    AWOriginalWindowOnScroll = window.onscroll;
                    AWOriginalWindowOnResize = window.onresize;
                    window.onscroll = function () {
                        Dom.positionDialogBox(AWWaitAlertDiv)
                    };
                    window.onresize = window.onscroll;
                    Dom.positionDialogBox(AWWaitAlertDiv);
                    Dom.overlay(AWWaitAlertDiv);
                }
            }
        },

        hideWaitAlert : function ()
        {
            if (AWIsWaitAlertActive) {
                AWIsWaitAlertActive = false;
                window.onscroll = AWOriginalWindowOnScroll;
                window.onresize = AWOriginalWindowOnResize;
                AWOriginalWindowOnScroll = null;
                AWOriginalWindowOnResize = null;
                if (AWWaitAlertDiv != null) {
                    AWWaitAlertDiv.style.display = "none";
                    var span = Dom.getElementById("awwaitMessage");
                    if (span) span.innerHTML = "";
                    Dom.unoverlay(AWWaitAlertDiv);
                }
            }
        },

        showWaitAlertInWindow : function (namedWindow)
        {
            if (namedWindow != null) {
                namedWindow.document.writeln('<html><head><title>');
                namedWindow.document.writeln(document.title);
                namedWindow.document.writeln('</title></head><body>');
            // IE pause wait alert animation when there is a pending response.
                // Skipping for now.
                if (!Dom.IsIE) {
                    var waitAlertDiv = Dom.getElementById('awwaitAlertDiv');
                    if (waitAlertDiv != null) {
                        var newDiv = waitAlertDiv.cloneNode(true);
                        newDiv.style.visibility = '';
                        newDiv.style.display = '';
                    // Center it
                        newDiv.style.left = '50%';
                        newDiv.style.top = '50%';
                        newDiv.style.marginLeft = '-' + waitAlertDiv.offsetWidth / 2 + 'px';
                        newDiv.style.marginTop = '-' + waitAlertDiv.offsetHeight / 2 + 'px';
                        var newNode = document.createElement("span");
                        newNode.appendChild(newDiv);
                        namedWindow.document.writeln(newNode.innerHTML);
                    }
                }
                namedWindow.document.writeln('</body></html>');
                namedWindow.document.close();

                if (this.AWAutomationTestModeEnabled) {
                    namedWindow.status = "Processing request ...";
                }

            }
        },

        //////////////////
        // Doc Cover Util
        // Used to put a div on top of the entire document
        // to block out clicks and other intereations.
        //////////////////

        hideSelects : function ()
        {
            //no-op
        },

        showSelects : function ()
        {
            //no-op
        },

        coverDocument : function (zIndex, opacity)
        {
            var coverDiv = document.createElement('div');
            var coverStyle = coverDiv.style;
            coverStyle.position = "absolute";
            coverStyle.zIndex = zIndex;
            var docBody = document.body
            coverStyle.backgroundColor = "#FFFFFF";
            Dom.setOpacity(coverDiv, opacity);
            this.updateCoverSize(coverDiv);
            // Need to insert at beginning (not end) otherwise on Firefox it will occlude the dialog despite z-index order            
            docBody.appendChild(coverDiv, document.body.firstChild);
            coverDiv.style.display = "";

            this.registerCoverDiv(coverDiv);
            this.hideSelects();
            return coverDiv;
        },

        updateCoverSize : function (coverDiv)
        {
            var coverStyle = coverDiv.style;
            var documentElement = Dom.getDocumentElement();
            var width = Util.max(documentElement.scrollWidth, documentElement.clientWidth);
            var height = Util.max(documentElement.scrollHeight, documentElement.clientHeight);
            coverStyle.top = "0px";
            coverStyle.left = "0px";
            coverStyle.width = width + "px";
            coverStyle.height = height + "px";
        },

        uncoverDocument : function (element)
        {
            this.showSelects();
            this.unregisterCoverDiv(element);

            document.body.removeChild(element);
        },

        registerCoverDiv : function (div) {
            AWCoverDiv = div;
            AWPrevFocusId = Dom.getActiveElementId();
            if (AWPrevFocusId) {
                Debug.log("reg cover div " + AWPrevFocusId);
                var prevFocusElement = Dom.getElementById(AWPrevFocusId);
                if (prevFocusElement && prevFocusElement.blur) {
                    prevFocusElement.blur(); // release main window focus                    
                };
            }
        },

        unregisterCoverDiv : function (div) {
            if (AWPrevFocusId) {
                Debug.log("unreg cover div " + AWPrevFocusId)
                this.registerActiveElementId(AWPrevFocusId);
            }
            AWCoverDiv = AWPrevFocusId = null;
        },

        registerModalDiv : function (div) {
            this.registerCoverDiv(div);
        },

        unregisterModalDiv : function (div) {
            this.unregisterCoverDiv(div);
        },

        modallyDisabled : function (elm)
        {
            if (!Dom.elementInDom(AWCoverDiv)) return false;
            if (!AWCoverDiv) return false;
        // see if we're a child of the modal
            var e = elm;
            while (e) {
                if (e == AWCoverDiv) return false;
                e = e.parentNode;
            }
            return this.modallyDisabled_zindex(elm);
        },

        // Is element disabled by a cover div?
        modallyDisabled_zindex : function (elm) {
            // Debug.log("awModallyDisabled:  AWCoverDiv=" + AWCoverDiv + ", posParent=" + Dom.positioningParent(elm)
            //       + ", zindex-elm=" + Dom.effectiveStyle(elm, 'z-index'));
            if (!AWCoverDiv)  return false;
            var posParent = Dom.positioningParent(elm);
            if (posParent == null || posParent == Dom.documentElement()) return true;
            var elmZ = Dom.effectiveStyle(posParent, 'z-index');
            if (!elmZ) return true;
            var coverZ = Dom.effectiveStyle(Dom.positioningParent(AWCoverDiv), 'z-index');
            // Debug.log("awModallyDisabled: " + coverZ + ", " + elmZ);
            return parseInt(elmZ) < parseInt(coverZ);
        },

        registerActiveElementId : function (elementId) {
            Debug.log("registerActiveElementId: " + elementId);
            AWActiveElementId = elementId ? elementId : Dom.getActiveElementId();
        },

        // focus on control when page load complete
        postLoadFocusOnActiveElement : function ()
        {
            // Enqueue twice to ensure that it is the very last event to fire.
            Event.eventEnqueue(function () {
                  Event.eventEnqueue(Input.focusOnActiveElement.bind(Input));
          }, null, true);
        },

        focusOnActiveElement : function () {
            var elementFocused = false;
            if (AWActiveElementId) {
                try {
                    var activeElement = Dom.getElementById(AWActiveElementId);
                    if (Dom.elementInDom(activeElement) &&
                        !this.modallyDisabled(activeElement)) {
                        Debug.log("Focusing on element id: " + AWActiveElementId);
                        if (activeElement.focus) {
                            activeElement.focus();
                            activeElement.focus();
                        }
                        elementFocused = true;
                        function checkFocus () {
                            if (!Dom.getActiveElementId()) {
                                Debug.log("Refocusing on element id: " + AWActiveElementId);
                                if (activeElement.focus) {
                                    activeElement.focus();
                                    activeElement.focus();
                                }
                            }
                        }
                        if (Dom.IsIE) {
                            setTimeout(checkFocus, 0);
                        }
                    }
                }
                catch (e) {
                    Debug.log("Focusing exception: " + e);                    
                }
                finally {
                    AWActiveElementId = null;
                }
            }
            if (!elementFocused && AWAllowSelectFirstText) {
                AWAllowSelectFirstText = false;
                Debug.log("Focusing on first text: ");
                this.selectFirstText();
            }
        },
        ////////////////////////////
        // AWTextField
        // Select First TextField
        // or TextArea
        ///////////////////////////

        selectFirstText : function ()
        {
            // focus on the first text field or the one specified
            var text = null;
            if (AWTextFocusId) {
                text = Dom.getElementById(AWTextFocusId);
                AWTextFocusId = null;
            }
            if (!text) {
                var forms = document.forms;
                var length = forms.length;
                for (var index = 0; index < length; index++) {
                    text = this.findFirstText(forms[index]);
                    if (text != null) {
                        break;
                    }
                }
            }
            if (text) {
                if (text.value == null) {
                    text.value = " ";
                }
                try {
                    text.focus();
                    text.select();
                } catch (e) { /* ignore -- may heppen if element is hidden */
                }
            }
        },

        findFirstText : function (form)
        {
            if (form == null) return null;
            var inputs = form.getElementsByTagName("input");
            for (var i = 0, c = inputs.length; i < c; i++) {
                var element = inputs[i];
                if (( ((element.type == "text" || element.type == "password") &&
                       element.getAttribute('awautoselect') != "0" ) ||
                      element.nodeName == "TEXTAREA"
                        ) &&
                    Dom.visibleInScrollArea(element) && !element.getAttribute('disabled') &&
                    !this.modallyDisabled(element) &&
                    Dom.effectiveStyle(element, "display") != 'none') {
                    return element;
                }
            }
            return null;
        },

        setTextFocus : function (id)
        {
            AWTextFocusId = id;
        },

        // allow select first input field when page load complete
        allowSelectFirstText : function ()
        {
            AWAllowSelectFirstText = true;
        },

        EOF:0};

    //
    // IE6 - specific methods
    //
    if (Dom.IsIE6Only) Util.extend(Input, function () {
        var AWDummySelects = new Array();

        return {
            hideSelects : function (skipDummySelects)
            {
                var text;
                var select;
                var selects = document.getElementsByTagName('select');
                if (!skipDummySelects) {
                    this.disposeDummySelects();
                }
                // overlay a dummy select for each real select
                for (var i = 0; i < selects.length; i++) {
                    select = selects[i];
                    if (!skipDummySelects && select.offsetWidth > 0) {
                        // only supporting single option selects for now
                        if (select.size > 1) {
                            continue;
                        }
                        var j;
                        // find text for selected option to insert into dummy
                        for (j = 0; j < select.options.length; j++) {
                            if (select.options[j].selected) {
                                text = select.options[j].text;
                            }
                        }

                        // create dummy from template
                        var dummySelectDivTemplate = document.getElementById('AWDummySelect');
                        var dummySelectDiv = dummySelectDivTemplate.cloneNode(true);
                        document.body.appendChild(dummySelectDiv);

                        //  insert selected option text
                        var tds = dummySelectDiv.getElementsByTagName('td');
                        for (j = 0; j < tds.length; j++) {
                            if (tds[j].className == 'dummySelect') {
                                tds[j].innerText = text;
                            }
                        }

                        // style and show dummy
                        dummySelectDiv.style.width = select.offsetWidth - 1;
                        dummySelectDiv.style.zIndex = "-100";
                        dummySelectDiv.style.position = "absolute";
                        dummySelectDiv.style.display = '';
                        dummySelectDiv.style.top = Dom.absoluteTop(select);
                        dummySelectDiv.style.left = Dom.absoluteLeft(select);

                        // register for cleanup
                        AWDummySelects[AWDummySelects.length] = dummySelectDiv;
                    }

                    // hide the real select
                    if (this.modallyDisabled(select)) {
                        select.style.visibility = 'hidden';
                    }
                }
            },

            showSelects : function ()
            {
                var selects = document.getElementsByTagName('select');
                for (var i = 0; i < selects.length; i++) {
                    var select = selects[i];
                    select.style.visibility = 'visible';
                }
                this.disposeDummySelects();
            },

            disposeDummySelects : function ()
            {
                // clean up dummy selects
                for (var i = 0; i < AWDummySelects.length; i++) {
                    AWDummySelects[i].removeNode(true);
                }
                AWDummySelects = new Array();
            },
        EOF:0};
    }());

    //
    // IE - specific methods
    //
    if (Dom.IsIE) Util.extend(Input, function () {
        /**
         *  focusedElement is the form element which is currently focused.  If non-null, this will be
         *  restored as the focused element once Input.enableInput() is called.
         */
        var AWCaptureDiv = null;

        return {
            disableInput : function (showWaitAlert)
            {
                Event.disableEvents();
                if (AWCaptureDiv == null) {
                    AWCaptureDiv = document.createElement("div");
                    document.body.appendChild(AWCaptureDiv);
                }
                AWCaptureDiv.setCapture(false);
                AWCaptureDiv.style.cursor = "wait";
                if (showWaitAlert) {
                    AWCaptureDiv.onmousedown = this.showWaitAlert.bindEventHandler(this);
                    AWCaptureDiv.onkeydown = this.showWaitAlert.bindEventHandler(this);
                }                
            },

            enableInput : function ()
            {
                Event.enableEvents();
                if (AWCaptureDiv == null) {
                    return;
                }
                AWCaptureDiv.onmousedown = null;
                AWCaptureDiv.onkeydown = null;
                AWCaptureDiv.releaseCapture();

                AWCaptureDiv.style.cursor = "pointer";                
            },

        EOF:0};
    }());

    //
    // Mozilla - specific methods
    //
    if (!Dom.IsIE) Util.extend(Input, function () {
        function showWaitAlertNS (mevent)
        {
            Input.showWaitAlert();
            Event.cancelBubble(mevent);
        }

        return {
            disableInput : function (showWaitAlert)
            {
                var docBody = document.body;
                if (showWaitAlert) {
                    docBody.addEventListener("mousedown", showWaitAlertNS, true);
                    docBody.addEventListener("keydown", showWaitAlertNS, true);
                    docBody.addEventListener("click", showWaitAlertNS, true);
                }
                docBody.addEventListener("mouseover", Event.cancelBubble, true);
                docBody.style.cursor = "wait";
            },

            enableInput : function ()
            {
                var docBody = document.body;
                docBody.removeEventListener("mousedown", showWaitAlertNS, true);
                docBody.removeEventListener("mouseover", Event.cancelBubble, true);
                docBody.removeEventListener("click", showWaitAlertNS, true);
                docBody.removeEventListener("keydown", showWaitAlertNS, true);
                docBody.style.cursor = "";
            },
        EOF:0};
    }());

    // Patch Event to provide implementations of these
    Util.extend(Event, {
        // overridded by Input.js
        modallyDisabled :function (target)
        {
            return Input.modallyDisabled(target);
        },

        // overridded by Input.js
        selectFirstText :function ()
        {
            return Input.selectFirstText();
        }
    });

    // Initialization
    return Input;
}();
