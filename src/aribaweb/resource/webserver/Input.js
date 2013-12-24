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

    if (Dom.IsIE) {
        document.createStyleSheet().addRule("v\\: *", "behavior:url(#default#VML);");
        !document.namespaces.rvml && document.namespaces.add("v", "urn:schemas-microsoft-com:vml");
    }

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
    var AWFocusRegionId = null;
    var AWAllowSelectFirstText = false;
    var AWModalPanelId = null;
    var _disableShowUntil = 0;

    var Input = {

        // Public Globals
        AWWaitAlertMillis : 2000,
        AWWaitMillis : 20000,
        AWAutomationTestModeEnabled : false,

        KeyCodeBackspace : 8,
        KeyCodeTab       : 9,
        KeyCodeEnter     : 13,
        KeyCodeShift     : 16,
        KeyCodeCapsLock  : 20,
        KeyCodeEscape    : 27,
        KeyCodeArrowUp   : 38,
        KeyCodeArrowDown : 40,
        KeyCodeDelete    : 46,

        isCharChange : function (event) {
            var keyCode = event.keyCode;
            if (keyCode == this.KeyCodeBackspace ||
                keyCode == this.KeyCodeDelete) {
                return true;
            }
            var character = null;
            if (event.which == null) {
                 character = String.fromCharCode(event.keyCode);    // IE
            }
            else if (event.which != 0 && event.charCode != 0) {
                 character = String.fromCharCode(event.which);      // All others
            }
            return character != null;
        },
        focus : function (elm)
        {
            try {
                elm.focus();
            }
            catch (e) {                
            }
        },
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

            var waitAlertMillis = this.waitAlertSettings();
            this.disableInput(true);

            if (!AWShowWaitAlertDisabled) {
                // clear any existing timeout if any
                // this can happen when we are loading multiple lazy divs
                clearTimeout(AWWaitAlertTimeoutId);
                clearTimeout(AWWaitTimeoutId);
                AWWaitAlertTimeoutId = setTimeout(this.showWaitAlert.bind(this), waitAlertMillis);
                AWWaitTimeoutId = setTimeout(this.hideWaitCursor.bind(this), waitAlertMillis + this.AWWaitMillis);
            }
        },

        waitAlertSettings : function (message)
        {
            _disableShowUntil = (new Date()).getTime() + 500;
            return this.AWWaitAlertMillis;
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
            if (AWShowWaitCursorDisabled) {
                return;
            }
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

        createCoverDiv : function (zIndex, opacity)
        {
            var coverDiv = null;
            var coverStyle = null;
            if (Dom.IsIE) {
                coverDiv = document.createElement('<v:rect stroked="False">');
                var fill = document.createElement('<v:fill color="black">');
                fill.opacity = opacity + "%";
                coverDiv.appendChild(fill);
                coverStyle = coverDiv.style;
            }
            else {
                coverDiv = document.createElement('div');
                coverStyle = coverDiv.style;
                coverStyle.backgroundColor = "#000000";
                Dom.setOpacity(coverDiv, opacity);
            }            
            coverStyle.position = "absolute";
            coverStyle.zIndex = zIndex;
            return coverDiv;
        },

        coverDocument : function (zIndex, opacity)
        {
            var coverDiv = this.createCoverDiv(zIndex, opacity);            
            this.updateCoverSize(coverDiv);
            var docBody = document.body
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
            coverStyle.width = width -1 + "px";
            coverStyle.height = height -1 + "px";            
        },

        uncoverDocument : function (element)
        {
            element = element || AWCoverDiv;
            this.showSelects();
            if (element) {
                this.unregisterCoverDiv(element);
                document.body.removeChild(element);
            }
        },

        registerCoverDiv : function (div) {
            AWCoverDiv = div;
            if (AWActiveElementId) {
                Debug.log("reg cover div " + AWActiveElementId);
                AWPrevFocusId = AWActiveElementId;
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
            var posParent = elm;
            while ((posParent = Dom.positioningParent(posParent)) && posParent != Dom.documentElement()) {
                // Debug.log("posParent = " + posParent);
                var elmZ = Dom.effectiveStyle(posParent, 'z-index');
                if (elmZ) {
                    var coverZ = Dom.effectiveStyle(Dom.positioningParent(AWCoverDiv), 'z-index');
                    Debug.log("awModallyDisabled: " + coverZ + ", " + elmZ);
                    if (!coverZ) return false;
                    if (parseInt(elmZ) > parseInt(coverZ)) return false;
                }
            }
            return true;
        },

        // call before Dom update to save current active element
        registerActiveElementId : function (elementId) {
            AWActiveElementId = elementId ? elementId : Dom.getActiveElementId();
            Debug.log("registerActiveElementId: " + AWActiveElementId);
        },

        setFocusRegion : function (regionId)
        {
            AWFocusRegionId = regionId;
        },

        // focus on control when page load complete
        postLoadFocusOnActiveElement : function ()
        {
            // Enqueue twice to ensure that it is the very last event to fire.
            Event.eventEnqueue(function () {
                  Event.eventEnqueue(Input.focusOnActiveElement.bind(Input));
          }, null, true);
        },

        /////////////////////////////////////////
        //   Precendence:                      //
        //   first text in focus region        //
        //   current browser active element    //
        //   first text on page if allowed     //
        /////////////////////////////////////////
        focusOnActiveElement : function () {
            if (AWFocusRegionId) {
                var focusRegion = Dom.getElementById(AWFocusRegionId);
                AWFocusRegionId = null;
                if (focusRegion) {
                    var firstRegionText = this.findFirstText(focusRegion);
                    if (firstRegionText) {
                        AWActiveElementId = firstRegionText.id;
                    }
                }
            }
            if (AWActiveElementId) {
                try {
                    var activeElement = Dom.getElementById(AWActiveElementId);
                    if (Dom.elementInDom(activeElement) &&
                        !this.modallyDisabled(activeElement)) {
                        Debug.log("Focusing on element id: " + AWActiveElementId);
                        var activeElementId = AWActiveElementId;
                        function checkFocus () {
                            try {
                                // no active element, refocus
                                if (!Dom.getActiveElementId()) {
                                    Debug.log("Refocusing on element id: " + activeElementId);
                                    if (activeElement.focus) {
                                        activeElement.focus();
                                        activeElement.focus();
                                        if (activeElement.select) {
                                            activeElement.select();
                                        }
                                    }
                                }
                            }
                            catch (fe) {}
                        }
                        setTimeout(checkFocus, 1000);
                        if (activeElement.focus) {
                            activeElement.focus();
                            activeElement.focus();
                            if (activeElement.select) {
                                activeElement.select();
                            }                            
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
            if (!Dom.getActiveElementId() && AWAllowSelectFirstText) {
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

        findFirstText : function (parentElm)
        {
            if (parentElm == null) return null;
            var allowAnchor = Dom.boolAttr(parentElm, "_aa", false);
            var inputs = Dom.findChildrenUsingPredicate(parentElm, function (e) {
                    return e.tagName == "INPUT" || e.tagName == "TEXTAREA" || (allowAnchor && e.tagName == "A");
                });
            for (var i = 0, c = inputs.length; i < c; i++) {
                var element = inputs[i];
                if (( ((element.type == "text" || element.type == "password") &&
                       element.getAttribute('awautoselect') != "0" ) ||
                      element.nodeName == "TEXTAREA" || element.nodeName == "A"
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

		/**
		 * A useful function that automates implementing a client-side only
		 * message related to an input element.
		 * 
		 * This can be tricky to use as their are a bunch of required data
		 * attributes. Here is an explanation:
		 * 
		 *	 The input must have the following attributes set:
		 *		~ data-rxmatcher="..." This should contain the regular expression you want to use to match the input against. Use parenthesis to make groups available in the replacement.
		 *		~ data-placeholder="..." This should be a character to use as a placeholder when the user input does not yet equal max length.
		 *		~ data-replacement="..." This should be the replacement string to use in conjunction with the rxmatcher before inserting into the sibling element.
		 *		~ maxlength="..." The maximum length that can be entered for this value (should match the regex).
		 *
		 *   Here is a sample input element:
		 *		<input data-placeholder="0" data-rxmatcher="(\d{3})(\d{3})(\d{4})" data-replacement="$1-$2-$3" maxlength="10" id="id_test_input"/>
		 *
		 *	 Here is sample JavaScript to initialize:
		 *		ariba.Input.initRegexNote("id_test_input");
		 * 
		 * @param sId {String} The id of an input element.
		 * @param oOptions? {Object} Object literal overriding defaults.
		 */
        initRegexNote : function (sId, oOptions) {
            var oDefaults = {
                    dataAttrMatcherRegex: 'rxmatcher',
                    dataAttrPlaceholder: 'placeholder',
                    dataAttrReplacement: 'replacement',
                    invalidText: 'Invalid input',
                    replacementFunction: null,
                    targetClass: 'regexInputNote'
                },
                oSettings,
                fnReplacer,
                iMaxLength,
                iTimeoutId,
                rxMatcher,
                sPlaceholder,
                sReplacement,
                el = document.getElementById(sId),
                elTarget;

            function fnUpdate() {
                clearTimeout(iTimeoutId);
                var sVal = el.value;

                if (fnReplacer) {
                    elTarget.innerHTML = fnReplacer(sVal);
                }
                else {
                    while (sVal.length < iMaxLength) {
                        sVal += sPlaceholder;
                    }

                    elTarget.innerHTML = sVal.match(rxMatcher) ?
                            sVal.replace(rxMatcher, sReplacement) :
                            oSettings.invalidText;
                }
            }

            function fnOnChange() {
                fnUpdate();
            }

            function fnOnKey() {
                clearTimeout(iTimeoutId);
                iTimeoutId = setTimeout(function() {
                    fnUpdate();
                }, 100);
            }

            function init() {
                var sTemp,
                    elTemp = el;
                oSettings = ariba.Util.extend({}, oDefaults);
                oSettings = ariba.Util.extend(oSettings, oOptions);

                while (elTemp.nextSibling) {
                    elTemp = elTemp.nextSibling;
                    if (elTemp.className && -1 !== elTemp.className.indexOf(
                            oSettings.targetClass)) {
                        elTarget = elTemp;
                        break;
                    }
                }

                sTemp = el.getAttribute(
                    "data" + oSettings.dataAttrMatcherRegex);
                iMaxLength = parseInt(el.getAttribute('maxlength'), 10);
                sPlaceholder = '' + el.getAttribute(
                    "data" + oSettings.dataAttrPlaceholder);
                sReplacement = el.getAttribute(
                    "data" + oSettings.dataAttrReplacement);
                rxMatcher = new RegExp(sTemp);
                fnReplacer = oSettings.replacementFunction;
                Event.addEvent(el, 'onchange', fnOnChange);
                Event.addEvent(el, 'onkeyup', fnOnKey);

                if (!fnReplacer) {
                    if (!iMaxLength) {
                        alert('RegexInputNode - please specify the maxlength ' +
                            'attribute');
                    }
                    if (!sPlaceholder) {
                        alert('RegexInputNode - please specify dataAttrPlaceholder ' +
                            'or use `' + oSettings.dataAttrPlaceholder +
                            '` attribute.');
                    }
                    if (!sReplacement) {
                        alert('RegexInputNode - please specify dataAttrReplacement ' +
                            'or use `' + oSettings.dataAttrReplacement +
                            '` attribute.');
                    }
                    if (!rxMatcher) {
                        alert('RegexInputNode - please specify dataAttrMatcherRegex ' +
                            'or use `' + oSettings.dataAttrMatcherRegex +
                            '` attribute.');
                    }
                    if (!elTarget) {
                        alert('RegexInputNode - please specify targetClass ' +
                            'or apply `' + oSettings.targetClass +
                            '` to a nextSibling of the target element.');
                    }
                }
            }

            init();
        },

        EOF:0};

    //
    // iPad - specific methods
    //
    if (Dom.isIPad) Util.extend(Input, function () {
        return {
            waitAlertSettings : function (message)
            {
                _disableShowUntil = (new Date()).getTime();
                return 500;
            },

            updateWaitMessage : function (message)
            {
                if (!AWIsWaitAlertActive) this.showWaitAlert();
                var span = Dom.getElementById("awwaitMessage");
                if (span) {
                    span.innerHTML = message;
                    span.style.paddingTop = "15px";
                    Dom.positionDialogBox(AWWaitAlertDiv);
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
                        if (span) {
                            span.innerHTML = "";
                            span.style.paddingTop = "0px";
                        }
                        Dom.unoverlay(AWWaitAlertDiv);
                    }
                }
            },

        EOF:0};
    }());

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
                // Wait cursor is broken in Safari (only changes on mouse move) -- disabling...
                if (Dom.isSafari && !Dom.isIPad) return;

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
