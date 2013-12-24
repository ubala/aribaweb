/*
   Dom.js - utilities for manipulating the HTML DOM tree and doing element sizing

   Includes higher level functions for overlaying "cover divs"
*/

ariba.Dom = function() {
    // imports
    var Util = ariba.Util;
    
    ////////////////
    // Browser type
    ////////////////
    // private vars
    var IdentIE5 = "MSIE 5.0";
    var IdentNS = "Netscape";
    var ELEMENT_NODE = 1;
    var WindowsArray = new Array();
    var _apV = navigator.appVersion;
    var IsIE = (window.attachEvent && !window.opera) ? true : false;
    var IsIE9 = _apV.indexOf("MSIE 9") != -1;
    var IsIE8 = _apV.indexOf("MSIE 8") != -1;
    var IsIE7 = _apV.indexOf("MSIE 7") != -1;
    var IsIE6 = _apV.indexOf("MSIE 6") != -1;
    var IsIE8Up = IsIE8 || IsIE9;
    var IsIE7Up = IsIE7 || IsIE8Up;
    var IsIE6Up = IsIE6 || IsIE7Up;

    var ViewportBelowState = 'below';
    var ViewportTopInState = 'topIn';
    var ViewportInState = 'in';
    var ViewportTopOutState = 'topOut';
    var ViewportAboveState = 'above';
    var ViewportStates = [
        ViewportBelowState,
        ViewportTopInState,
        ViewportInState,
        ViewportTopOutState,
        ViewportAboveState
    ];

    var Dom = {
        // Public Globals
        IsIE : IsIE,
        IsIE9 : IsIE9,
        IsIE8 : IsIE8,
        IsIE7 : IsIE7Up,
        IsIE6Only : IsIE6,
        IsIE6 :  IsIE6Up,
        IsIEonMac : (IsIE && (navigator.platform != "Win32") && (navigator.platform != "Win64")) ? true : false,
        IsNS6 : (!document.all && document.getElementById) ? true : false,
        IsMoz : (!document.all && document.getElementById) ? true : false,
        isChrome : navigator.appVersion.indexOf("Chrome") != -1,
        isSafari : navigator.appVersion.indexOf("Safari") != -1,
        isIPad : navigator.platform.indexOf("iPad") != -1,
        AWEmptyDocScriptlet : IsIE6 ? "javascript:false" : "javascript:void(0);",
        ApplicationType : "",
        AWOpenWindowErrorMsg : null,

        ///////////////////////////////
        // Functions for DOM searching
        ///////////////////////////////

        getElementById : function (id)
        {
            var element = null;
            try {
                if (id && id != null && id != "") {
                    element = document.getElementById ?
                              document.getElementById(id) :
                              document.all[id];
                }
            }
            catch (e) {
                // swallow
            }
            return element;
        },

        /**
         * Fetches elements under the root node with the provided tag that are
         * matched by the provided evaluation function. There is a fast path
         * to prevent additional iteration when you only want the first node.
         * @param elRoot The root element.
         * @param sTagName The tag name to look for (use '*' for all)
         * @param fnEval The function to evaluate each node. Should accept
         *               an element as its only argument and return true
         *               or false.
         * @param bReturnFirst Optional. When true, will only return the first
         *                     matched element.
         * @return {Array|Element} A list of matched elements (may be empty).
         */
        getElementsBy : function (elRoot, sTagName, fnEval, bReturnFirst)
        {
            var aNodes = [],
                aElements = elRoot.getElementsByTagName(sTagName),
                                i = 0, j = aElements.length;

            for (; i < j; ++i) {
                if (fnEval(aElements[i])) {
                    if (bReturnFirst) {
                        return aElements[i];
                    }
                    else {
                        aNodes.push(aElements[i]);
                    }
                }
            }

            return aNodes;
        },

        /**
         * Fetches the first element under the root node with the provided tag
         * that is matched by the provided evaluation function.
         * @param elRoot The root element.
         * @param sTagName The tag name to look for (use '*' for all)
         * @param fnEval The function to evaluate each node. Should accept
         *               an element as its only argument and return true
         *               or false.
         * @return {Element} A single node or null.
         */
        getElementBy : function (elRoot, sTagName, fnEval)
        {
            return Dom.getElementsBy(elRoot, sTagName, fnEval, true) || null;
        },

        /**
         * Fetches elements under the root node with the provided tag that are
         * matched by the provided evaluation function. There is a fast path
         * to prevent additional iteration when you only want the first node.
         * @param elRoot The root element.
         * @param sClassName The target class to look for.
         * @param sTagName Optional. The tag name to look for (use '*' for all).
         *                 Providing a tagName will drastically improve the
         *                 performance of this DOM search.
         * @return {Array|NodeList} A list of matched elements (may be empty).
         */
        getElementsByClassName : function (elRoot, sClassName, sTagName)
        {
            var aNodes = [],
                aElements;

            // use native function if possible (maximum performance)
            if (elRoot.getElementsByClassName) {
                aElements = elRoot.getElementsByClassName(sClassName);

                if (sTagName) {
                    for (var i = 0, j = aElements.length; i < j; ++i) {
                        if (sTagName === aElements[i].tagName) {
                            aNodes.push(aElements[i]);
                        }
                    }
                    return aNodes;
                }
                else {
                    return aElements;
                }
            }
            else {
                return Dom.getElementsBy(elRoot, sTagName || '*', function (el)
                {
                    return Dom.hasClass(el, sClassName);
                });
            }
        },

        getDocumentElementById : function (doc, id)
        {
            if (!doc)
                return null;

            return doc.getElementById ? doc.getElementById(id) : doc.all[id];
        },

        findParent : function (poTarget, sType, checkCurrent)
        {
            var node = checkCurrent ? poTarget : poTarget.parentNode;

            while (node != null && node.nodeName != sType) {
                node = node.parentNode;
            }
            return node;
        },

        findChild : function (poTarget, sType, checkCurrent)
        {
            var node = null;

            if (!poTarget) {
                return poTarget;
            }
            else if (checkCurrent && (poTarget.nodeName == sType)) {
                return poTarget;
            }
            else if (poTarget.childNodes) {
                var childNodes = poTarget.childNodes;
                for (var i = 0; i < childNodes.length && node == null; i++) {
                    node = this.findChild(childNodes[i], sType, true);
                }
            }

            return node;
        },

        findParentUsingPredicate : function (poTarget, matchFunc, checkCurrent)
        {
            var node = checkCurrent ? poTarget : poTarget.parentNode;

            while (node != null && !matchFunc(node)) {
                node = node.parentNode;
            }
            return node;
        },

        // note: getElementsBy is more efficient than this function
        findChildUsingPredicate : function (poTarget, matchFunc, checkCurrent)
        {
            var node = null;
            if (!poTarget) {
                return poTarget;
            }
            else if (checkCurrent && matchFunc(poTarget)) {
                return poTarget;
            }
            else if (poTarget.childNodes) {
                var childNodes = poTarget.childNodes;
                for (var i = 0; i < childNodes.length && node == null; i++) {
                    node = this.findChildUsingPredicate(childNodes[i], matchFunc, true);
                }
            }

            return node;
        },

        find : function (parent, selector) {
            // only supporting classname for now
            return this.findChildrenUsingPredicate(parent,
                function (e) {
                    return e.className && e.className.indexOf(selector) >= 0;
                }
            );
        },

        findChildrenUsingPredicate : function (poTarget, matchFunc, checkCurrent)
        {
            var nodes = [];

            if (!poTarget) {
                return poTarget;
            }
            else if (checkCurrent && matchFunc(poTarget)) {
                return [poTarget];
            }
            else if (poTarget.childNodes) {
                var childNodes = poTarget.childNodes;
                for (var i = 0; i < childNodes.length; i++) {
                    var matchedChildNodes = this.findChildrenUsingPredicate(childNodes[i], matchFunc, true);
                    nodes = nodes.concat(matchedChildNodes);
                }
            }

            return nodes;
        },

        elementInDom : function (e) {
            var de = this.documentElement();
            while (e && e != de) e = e.parentNode;
            return e == de;
        },

        /////////////////////////////////////////////
        // Functions for DOM reading and manipulation
        /////////////////////////////////////////////

        /**
         * Generic method for appending to the body element. Will alert,
         * if body is not found.
         */
        appendToBody: function (el) {
            if (document && document.body) {
                document.body.appendChild(el);
            }
            else {
                // ### debug
                alert("unable to find document / document.body");
            }
        },

        // append all children in array to node
        _appendChildren : function (n, arr)
        {
            for (var i = 0; i < arr.length; i++) {
                n.appendChild(arr[i]);
            }
        },

        //  innerText support
        //  Safari doesn't like the getter/setter syntax
        //  ie, HTMLElement.prototype.innerText getter =
        getInnerText : function (node)
        {
            var innerText;
            if (node) {
                if (node.innerText) {
                    innerText = node.innerText;
                }
                else if (node.textContent) {
                    innerText = node.textContent;
                }
                else {
                    innerText = node.innerHTML.replace(/<[^>]+>/g, "");
                    innerText = innerText.replace(/&amp;/g, "&");
                    innerText = innerText.replace(/&lt;/g, "<");
                    innerText = innerText.replace(/&gt;/g, ">");
                }
            }
            return innerText;
        },

        setInnerText : function (node, string)
        {
            if (node) {
                if (node.innerText || node.innerText == "") {
                    node.innerText = string;
                }
                else {
                    node.innerHTML = string;
                }
            }
        },

        copyInnerText : function (src, dest) {
            if (src && dest) {
                this.setInnerText(dest, this.getInnerText(src));
            }
        },

        isNetscape : function ()
        {
            return (navigator.appName.indexOf(IdentNS) != -1);
        },

        openWindow : function (urlString, windowName, attributesString)
        {
            var namedWindow = null;
            try {
                namedWindow = WindowsArray[windowName];
                if (namedWindow == null || namedWindow.closed) {
                    if (attributesString != null) {
                        namedWindow = window.open(urlString, windowName, attributesString);
                    }
                    else {
                        namedWindow = window.open(urlString, windowName);
                    }
                    WindowsArray[windowName] = namedWindow;
                    if (navigator.appVersion.indexOf(IdentIE5) != -1
                            || navigator.appName.indexOf(IdentNS) != -1) {
                        namedWindow.focus();
                    }
                    else if (this.IsIE6) {
                        // 6 or 7
                        function focusDelay(){namedWindow.focus();}
                        setTimeout(focusDelay);
                    }
                }
                else if (navigator.appVersion.indexOf(IdentIE5) != -1 || navigator.appName.indexOf(IdentNS) != -1) {
                    namedWindow.focus();
                    namedWindow.location.href = urlString;
                }
                else {
                    namedWindow.close();
                    if (attributesString != null) {
                        namedWindow = window.open(urlString, windowName, attributesString);
                    }
                    else {
                        namedWindow = window.open(urlString, windowName);
                    }
                }
            }
            catch (e) {
                // Safari crashes if we alert without a setTimeout.
                // It probably has to do the way we use eval and closures to execute callbacks. 
                function warn () {
                    alert(Dom.AWOpenWindowErrorMsg);
                }
                setTimeout(warn);
            }
            return namedWindow;
        },

        formForName : function (formName)
        {
            return document.forms[formName];
        },

        addFormField : function (formObject, fieldName, fieldValue)
        {
            return this.addFormFieldWithId(formObject, fieldName, fieldName, fieldValue);
        },

        addFormFieldWithId : function (formObject, fieldId, fieldName, fieldValue)
        {
            if (document.getElementById) {
                var inputObject = formObject[fieldId];
                if (inputObject == null) {
                    inputObject = document.createElement('input');
                    inputObject.type = 'hidden';
                    inputObject.id = fieldId;
                    inputObject.name = fieldName;
                }
                // safari sometimes removes from DOM, but not from the form's array, so add it back to the parent
                if (inputObject.parentNode != formObject) formObject.appendChild(inputObject);

                inputObject.value = fieldValue;
                return inputObject;
            }
        },

        removeFormField : function (formObject, fieldName)
        {
            var child = formObject[fieldName];
            if (child && child.parentNode) {
                child.parentNode.removeChild(child);
                if (ariba.Dom.IsMoz) {
                    delete formObject[fieldName];
                }
                else {
                    formObject[fieldName] = null;
                }
            }
        },

        lookupFormId : function (element)
        {
            // Search the enclosing element for a real form or a form target
            var form = this.findParentUsingPredicate(element, function(n) {
                return n.tagName == "FORM" || Dom.hasClass(n, 'formProxy');
            });
            var formId = null;
            if (form) {
                var formName = form.getAttribute('_fn');
                if (formName != null) {
                    form = this.formForName(formName);
                }

                formId = form.id;
            }
            return formId;
        },

        limitTextLength : function (textfield, maxlength) {
            if (maxlength < 1 ) return;
            var textFieldValue = textfield.value;
            // Mozilla doesn't add the \r in the js string, 
            // but does submit it with \r. These replacements make it consistent. 
            if (this.IsMoz) {
                textFieldValue = textFieldValue.replace(/([^\r])\n/g, "$1\r\n");
                textFieldValue = textFieldValue.replace(/^\n/g, "\r\n");
            }
            var textFieldLength = textFieldValue.length;
            if (textFieldLength > maxlength) {
                textfield.value = textFieldValue.substring(0, maxlength);
            }
            else {
                var indicator = this.getElementById(textfield.id + "MLI");
                if (!indicator) return;  
                var width = indicator.clientWidth;
                indicator.style.width = '';                
                indicator.innerHTML = maxlength - textFieldLength;
                var newWidth = indicator.clientWidth;
                indicator.style.width =
                    Math.max(width, newWidth) + 'px';
            }
        },

        addClass : function (n, className)
        {
            if (n.nodeType == ELEMENT_NODE) {
                // Only append a space if this is the second class.
                if (n.className != '' && className && className.charAt(className.length - 1) != " ") {
                    className = className + " ";
                }
                if (n.className && n.className.indexOf(className) == -1) {
                    n.className = className + n.className;
                }
                else if (!n.className) {
                    n.className = className;
                }
            }
        },

        removeClass : function (n, className)
        {
            var curName = n.className;
            if (!className || !curName) return
            var index = curName.indexOf(className);
            if (index != -1) {
                // See if we can remove trailing space
                var next = index + className.length;
                if (next < curName.length && curName.charAt(next) == " ") next++;
                n.className = curName.substring(0, index) +
                              curName.substring(next, curName.length);
            }
        },

        hasClass : function (e, className)
        {
            // todo: indexOf is faster than regex matching.
            return e.className && (e.className.match(new RegExp("(^|\\s)" + className + "(\\s|$)")) != null);
        },

        ///////////////
        // Wait Cursor
        ///////////////

        positionDialogBox : function (target)
        {
            var container = this.positioningParent(target.parentNode);
            var containerHeight = (container == this.documentElement()) ? this.documentClientHeight() : container.clientHeight;
            var containerWidth = (container == this.documentElement()) ? this.documentClientWidth() : container.clientWidth;
        // Debug.log ("awPositionDialogBox---  containerHeight=" + containerHeight + ", target.offsetHeight=" + target.offsetHeight);
            target.style.left =
            containerWidth / 2 - target.offsetWidth / 2 + this.getScrollLeft(container) + "px";
            target.style.top =
            containerHeight / 2 - target.offsetHeight / 2 + this.getScrollTop(container) + "px";
            if (target.onresize) target.onresize.call(target);
        },

        getDocumentElement : function ()
        {
            return document.documentElement;
        },

        // Firefox gives the whole document size for document.body.clientHeight.
        // window.innerHeight gives the number.
        documentClientHeight : function ()
        {
            if (window.innerHeight) {
                return window.innerHeight;
            }
            else {
                return document.documentElement.clientHeight;
            }
        },

        // Firefox gives the whole document size for document.body.clientWidth.
        // window.innerWidth gives the number.
        documentClientWidth : function ()
        {
            if (window.innerWidth) {
                return window.innerWidth;
            }
            else {
                return document.documentElement.clientWidth;
            }
        },

        setOpacity : function (element, opacity)
        {
            var style = element.style;
            if (IsIE) {
                style.filter = "alpha(opacity=" + opacity + ")";
            }
            else {
                style.opacity = (parseInt(opacity) / 100);
            }
        },

        ////////////////////
        // Positioning
        ////////////////////
        absoluteTop : function (element)
        {
            var absoluteTop = element.offsetTop;
            var parentElement = element.parentNode;
            var offsetParent = element.offsetParent;
            while (parentElement != null &&
                offsetParent != null &&
                parentElement != this.getPageScrollElement()) {
                // adjust offset if parent is an offset parent
                if (parentElement == offsetParent) {
                    absoluteTop += parentElement.offsetTop;
                    offsetParent = parentElement.offsetParent;
                }
                // subtract scrollTop for positioning inside of scrollable elements
                absoluteTop -= parentElement.scrollTop;
                parentElement = parentElement.parentNode;
            }
            return absoluteTop;
        },

        clientHeight : function (docElement, docScrollElement)
        {
            var clientHeight = docElement.clientHeight;
            if (!docScrollElement) {
                docScrollElement = docElement;
            }
            clientHeight += docScrollElement.scrollTop;
            return clientHeight;
        },

        /**
         * Determines if the top or bottom edge of an element
         * is visible in the current scroll area.
         */
        visibleInScrollArea : function (element)
        {
            var docElement = this.documentElement();
            var docScrollElement = this.getPageScrollElement();
            var scrollTop = docScrollElement.scrollTop;
            var scrollBottom = this.clientHeight(docElement, docScrollElement);
            var elementTop = this.absoluteTop(element);
            var elementBottom = elementTop + element.clientHeight;
            var elementTopVisible = (elementTop > scrollTop) && (scrollBottom > elementTop);
            var elementBottomVisible = (elementBottom > scrollTop) && (scrollBottom > elementBottom);
            return elementTopVisible || elementBottomVisible;
        },

        isElementInViewport : function (element)
        {
            var viewportState = this._viewportState(element);
            // in, topIn, and topOut are ok
            return viewportState != ViewportAboveState &&
                   viewportState != ViewportBelowState; 
        },

        setViewportState : function (element)
        {
            var viewportState = this._viewportState(element);
            this.setState(element, viewportState, ViewportStates);
        },
        
        _viewportState : function (element)
        {
            var clientRect = element.getBoundingClientRect();
            var windowSize = this.getWindowSize();
            var clientTop = clientRect.top;
            var clientBottom = clientRect.bottom;
            var windowHeight = windowSize[1];
            if (clientTop > windowHeight) {
                return ViewportBelowState;
            }
            else if (clientTop <= windowHeight &&
                clientBottom > windowHeight) {
                return ViewportTopInState;
            }
            else if (clientTop < 0 &&
                clientBottom <= windowHeight) {
                return ViewportTopOutState;
            }
            else if (clientBottom < 0) {
                return ViewportAboveState;
            }
            return ViewportInState;
        },

        //
        // Div Positioning
        //
        clientWidth : function (docElement, docScrollElement)
        {
            var clientWidth = docElement.clientWidth;
            if (!docScrollElement) {
                docScrollElement = docElement;
            }
            clientWidth += docScrollElement.scrollLeft;
            return clientWidth;
        },

        correctForRightEdge : function (divLeft, div)
        {
            var clientWidth =
                this.clientWidth(document.documentElement,
                                 this.getPageScrollElement());
            var adjustedDivLeft = clientWidth - div.offsetWidth;
            if (divLeft > adjustedDivLeft) {
                divLeft = adjustedDivLeft;
            }
            return divLeft;
        },
        
        correctForBottomEdge : function (divTop, div)
        {
            var clientHeight =
                this.clientHeight(document.documentElement,
                                  this.getPageScrollElement());
            var adjustedMenuTop = clientHeight - div.offsetHeight;
            if (divTop > adjustedMenuTop) {
                divTop = adjustedMenuTop;
            }
            return divTop;
        },

        repositionDivToWindow : function (div)
        {
            if (div.className.indexOf('noReposition') == -1) {
                var divTop = this.absoluteTop(div);
                var divLeft = this.absoluteLeft(div);
                var adjustedTop = this.correctForBottomEdge(divTop, div);
                var adjustedLeft = this.correctForRightEdge(divLeft, div);
                this.setAbsolutePosition(div, adjustedLeft, adjustedTop);
            }
        },

        absoluteLeft : function (element)
        {
            var absoluteLeft = element.offsetLeft;
            var parentElement = element.parentNode;
            var offsetParent = element.offsetParent;
            while (parentElement != null &&
                offsetParent != null &&
                parentElement != this.getPageScrollElement()) {
                // adjust offset if parent is an offset parent
                if (parentElement == offsetParent) {
                    absoluteLeft += parentElement.offsetLeft;
                    offsetParent = parentElement.offsetParent;
                }
                // subtract scrollLeft for positioning inside of scrollable elements
                absoluteLeft -= parentElement.scrollLeft;
                parentElement = parentElement.parentNode;
            }
            return absoluteLeft;
        },

        // min of inset of inner from outer on left or right
        minInsetWidth : function (outer, inner)
        {
            var oLeft = this.absoluteLeft(outer);
            var oRight = oLeft + outer.offsetWidth;
            var iLeft = this.absoluteLeft(inner);
            var iRight = iLeft + inner.offsetWidth;
            return Math.min((iLeft - oLeft), (oRight - iRight));
        },

        cssToJSName : function (cssName)
        {
            if (cssName.indexOf('-') < 0) return cssName;

            var list = cssName.split('-');
            var result = list[0];
            for (var i = 1, count = list.length; i < count; i++) {
                var str = list[i];
                result += str.charAt(0).toUpperCase() + str.substring(1);
            }

            return result;
        },

        // call with css-style name -- e.g. "z-index", not "zIndex"
        effectiveStyle : function (element, cssStyleName)
        {
            if (!element.style) return;
            var jsName = this.cssToJSName(cssStyleName);
            var value = element.style[jsName];
            if (!value) {
                if (document.defaultView && document.defaultView.getComputedStyle) {
                    var styles = document.defaultView.getComputedStyle(element, null);
                    value = styles ? styles.getPropertyValue(cssStyleName) : null;
                } else if (element.currentStyle) {
                    value = element.currentStyle[jsName];
                }
            }
            return value == 'auto' ? null : value;
        },

        offsetParent : function (element)
        {
            if (element.offsetParent) return element.offsetParent;
            if (element == document.body) return element;

            while ((element = element.parentNode) && element != document.body) {
                if (this.effectiveStyle(element, 'position') != 'static') return element;
            }
            return document.body;
        },

        positioningParent : function (element)
        {
            while (element && (element = element.parentNode) && element != document.documentElement) {
                var pos = this.effectiveStyle(element, 'position');
                if (pos == 'absolute' || pos == 'relative' || pos == 'fixed') break;
            }
            return element || document.documentElement;
        },

        /*
            Returns a container element that can be used for appendChild
        */
        contentParent : function (element)
        {
            var positioningParent = this.positioningParent(element);
            return positioningParent == document.documentElement ?
                   document.body : positioningParent;
        },

        containerParent : function (element)
        {
            var panelWrapper =
                    this.findParentUsingPredicate(element, function (e) {
                        return Dom.hasClass(e, "panelContainer");
                    });
            return panelWrapper != null ? panelWrapper : document.body;
        },

        setAbsolutePosition : function (element, left, top)
        {
            var offsetParent = this.offsetParent(element);
            if (offsetParent) {
                left -= this.absoluteLeft(offsetParent);
                top -= this.absoluteTop(offsetParent);
                if (offsetParent != this.getPageScrollElement()) {
                    left += offsetParent.scrollLeft;
                    top += offsetParent.scrollTop;
                }
            }
            element.style.left = left + 'px';
            element.style.top = top + 'px';
        },

        relativeOffset : function (element)
        {
            var top, left = 0;
            var done = false;
            do {
                top += element.offsetTop || 0;
                left += element.offsetLeft || 0;
                element = element.offsetParent;
                if (element) {
                    var pos = this.effectiveStyle(element, 'position');
                    done = (pos == 'absolute' || pos == 'relative');
                }
            } while (element && !done);
            return [left, top];
        },

        isVisible : function (element)
        {
            var doc = this.documentElement();
            while (element != doc) {
                if (element.style.display == "none") return false;
                element = element.parentNode;
            }
            return true;
        },

        // will deal with the firefox issue of display:inline elements returning 0 for offset ht/wd
        containerOffsetSize : function (elm)
        {
            var origDisplay;
            var wd = elm.offsetWidth, ht = elm.offsetHeight;
            if (!wd) {
                origDisplay = elm.style.display;
                elm.style.display = "BLOCK";
                wd = elm.offsetWidth;
                ht = elm.offsetHeight;
                elm.style.display = origDisplay;
            }
            return [wd, ht];
        },

        // returns array [width,height]
        getWindowSize : function ()
        {
            var myWidth = 0, myHeight = 0, myScrollHeight = 0;
            if (typeof( window.innerWidth ) == 'number') {
                //Non-IE
                myWidth = window.innerWidth;
                myHeight = window.innerHeight;
            }
            else if (document.documentElement &&
                     (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
                //IE 6+ in 'standards compliant mode'
                myWidth = document.documentElement.clientWidth;
                myHeight = document.documentElement.clientHeight;
                myScrollHeight = document.documentElement.scrollHeight;
            }
            else if (document.body && ( document.body.clientWidth || document.body.clientHeight )) {
                //IE 4 compatible
                myWidth = document.body.clientWidth;
                myHeight = document.body.clientHeight;
                myScrollHeight = document.body.scrollHeight;
            }
            return new Array(myWidth, myHeight, myScrollHeight);
        },

        isWindowNarrow : function ()
        {
            var winSize = this.getWindowSize();
            return (winSize[0] < 300);
        },

        //
        // div fade
        //
        fadeInElement : function (element)
        {
            this.fadeElement(element, "hidden", "visible");
        },

        fadeOutElement : function (element)
        {
            this.fadeElement(element, "visible", "hidden");
        },

        fadeElement : function (element, startVisibility, endVisibility)
        {
            try {
                if (element.filters) {
                    if (!element.filters.blendTrans) {
                        element.style.filter = "blendTrans(duration=.2)";
                    }
                    element.style.visibility = startVisibility;
                   // Make sure the filter is not playing.
                    if (element.filters.blendTrans) {
                        if (element.filters.blendTrans.status != 2) {
                            element.filters.blendTrans.apply();
                            element.style.visibility = endVisibility;
                            element.filters.blendTrans.play();
                        }
                    }
                }
            }
            catch (e) {
                element.style.visibility = endVisibility;
            }
        },

        setBodyClass : function (classString) {
            // Add in browser identifier for conditional styles
            if (this.IsIE) {
                classString += " IsIE"
                classString += this.IsIE7 ? " IsIE7" : " IsIE6";
            }
            else if (this.isSafari) {
                classString += " IsSaf";
                if (this.isIPad) {
                    classString += " IsIPad";
                }
                if (this.isChrome) {
                    classString += " IsChr";
                }
            }
            else {
                classString += " IsMoz";
            }
            this.addClass(document.body, classString);
        },

        findRow : function (poTBody, sId)
        {
            var row;
            var children = poTBody.childNodes;
            for (var i = 0; i < children.length; i++) {
                if (children[i].id == sId) {
                    row = children[i];
                    break;
                }
            }

            return row;
        },

        findRowIndex : function (sId, poTable)
        {
            var index = -1;
            var rows = poTable.rows;
            for (var i = 0; i < rows.length; i++) {
                if (rows[i].id == sId) {
                    index = i;
                    break;
                }
            }
            return index;
        },

        findElement : function (poArray, id)
        {
            if (!poArray)
                return null;
            for (var i = 0; i < poArray.length; i++) {
                if (poArray[i].id == id) {
                    return poArray[i];
                }
            }
            return null;
        },

        getHashLocation : function () {
            var hashValue = window.location.hash;
            if (!hashValue || hashValue == "" || hashValue == "#")
                return null;
            else if (hashValue.charAt(0) == "#")
                return hashValue.substring(1);
            else
                return hashValue;
        },

        // if key is missing, returns defVal
        boolAttr : function (elm, key, defVal) {
            var val = elm.getAttribute(key);
            return (val) ? (val == "true") : defVal;
        },

        //
        // Relocate Div support -- moving DIVs to correct IE zindex bug for dialogs
        //
        relocateDiv : function (divObject, markOriginalLocation)
        {
            if (divObject.getAttribute("_reloc") == '1') {
                var divId = divObject.id;
                this.removeRelocatedCopy(divId);

                var relocDestElm = this.findParentUsingPredicate(divObject, function(n) {
                    return n.className && n.className.indexOf("relocdest") != -1;
                }, true);
                if (relocDestElm) {
                    var container = document.createElement('div');
                    container.id = divId + "_MovedCopy";

                    if (markOriginalLocation) {
                        var origId = divId + "_OrigLocation";
                        var origMarker = document.createElement('div');
                        var origId = divId + "_OrigLocation";
                        origMarker.id = origId;

                        divObject.parentNode.replaceChild(origMarker, divObject);
                    }
                    else {
                        divObject.parentNode.removeChild(divObject);
                    }

                    relocDestElm.appendChild(container);
                    container.appendChild(divObject);
                    divObject.setAttribute('_reloc', 0);
                }
            }
        },

        revertRelocatedCopy : function (id)
        {
            var container = this.getElementById(id + "_MovedCopy");
            if (container) {
                var origMarker = this.getElementById(id + "_OrigLocation");
                //Ignore server-side div, the original marker may have been
                //removed with incremental update.
                if(origMarker) {
                    container.parentNode.removeChild(container);
                    var divObject = container.firstChild;
                    divObject.setAttribute("_reloc",'1');
                    origMarker.parentNode.replaceChild(divObject, origMarker);
                }
            }
        },

        removeRelocatedCopy : function (id)
        {
            var containerId = id + "_MovedCopy";
            var container = this.getElementById(containerId);
            if (container) container.parentNode.removeChild(container);
        },

        elementValue : function (fieldId) {
            var e = this.getElementById(fieldId);
            return e ? e.value : null;
        },

        elementIntValue : function (fieldId) {
            var e = this.getElementById(fieldId);
            return e ? parseInt(e.value) : 0;
        },

        setElementValue : function (fieldId, value) {
            if (fieldId) {
                var e = this.getElementById(fieldId);
                if (e) e.value = value;
            }
        },

        // works even for refresh region style container divs that have offsetHeight=0
        elmBottom : function (elm)
        {
            var n = elm;
            while (n = n.nextSibling) {
                if (n.offsetTop) return n.offsetTop;
            }
            return elm.offsetTop + elm.offsetHeight;
        },

        /**
         This handles the differences between netscape and safari.
         */
        getRowCells : function (trow)
        {
            var cells = trow.children;
            if (cells == null) {
                cells = trow.cells;
            }
            return cells;
        },

        overlay : function (divObject) {
            //no-op
        },
        unoverlay : function (divObject) {
            //no-op
        },

        getActiveElementId : function ()
        {
            var activeElement = document.activeElement;
            if (activeElement) {
                return activeElement.id;
            }
            return null;
        },

        getScrollTop : function (element)
        {
            return element.scrollTop;
        },

        getScrollLeft : function (element)
        {
            return element.scrollLeft;
        },

        // element holding the page scroll values
        getPageScrollElement : function ()
        {
            return document.documentElement;
        },
        
        checkWindowScrollbar : function (checkWindow, shouldScroll)
        {
            if (checkWindow) {
                var obj = document.documentElement;
                shouldScroll = (obj.scrollHeight > obj.clientHeight);
            }

            if (shouldScroll && document.documentElement.style.overflowY != "scroll") {
                document.documentElement.style.overflowY = "scroll";
                //debug("<font style='color:blue'>vertical scroll enabled</font>");
                return true;
            }

            if (!shouldScroll && document.documentElement.style.overflowY != "hidden") {
                document.documentElement.style.paddingRight = "0px";
                document.documentElement.style.overflowY = "hidden";
                return true;
            }
            return false;
        },

        /*
            Add CSS classes to the DOM element with the given new state.
            CSS rules can be specify that reflects the new state.

            DOM element can be in multiple states if they belong to different state groups.

            Example:
            
            setState("hover") on <div class="foo bar"> becomes

                <div class="foo bar foo-hover bar-hover">

            setState("topOut", ["in", "topOut"]) on <div class="foo bar foo-hover foo-in bar-in"> becomes

                <div class="foo bar foo-hover foo-topOut bar-topOut">
        */
        setState : function (elm, newState, allStates)
        {
            if (!elm) return;

            var elmClassName = elm.className;
            if (!elmClassName) return;

            if (!allStates) {
                allStates = [newState];
            }

            var i, j, state, stateClass, elmClass, isStateClass, addStateClass;
            var newClasses = [];
            
            var elmClasses = elmClassName.split(" ");
            for (i = 0; i < elmClasses.length; i++) {
                addStateClass = false;
                elmClass = elmClasses[i];
                
                for (j = 0; j < allStates.length; j++) {
                    state = allStates[j];
                    if (state == newState) {
                        // only add if matches new state
                        addStateClass = true;
                    }                    
                    isStateClass = this._isStateClass(elmClass, state);
                    if (isStateClass) {
                        break;
                    }
                }

                if (!isStateClass) {
                    // is normal class
                    newClasses.push(elmClass);
                    if (addStateClass) {
                        stateClass = elmClass + "-" + newState;
                        newClasses.push(stateClass);
                    }
                }

            }

            elm.className = newClasses.join(" ");
        },

        unsetState : function (elm, state)
        {
            if (!elm) return;

            var elmClassName = elm.className;
            if (!elmClassName) return;

            var i, j, stateClass, elmClass;
            var newClasses = [];

            var elmClasses = elmClassName.split(" ");
            for (i = 0; i < elmClasses.length; i++) {
                elmClass = elmClasses[i];
                if (!this._isStateClass(elmClass, state)) {
                    // only add if doesn't match state class 
                    newClasses.push(elmClass);
                }
            }

            elm.className = newClasses.join(" ");
        },

        _isStateClass : function (elmClass, state)
        {
            return elmClass.indexOf("-" + state) >= 0;
        },

        createElement : function (html)
        {
             var tmpDiv = document.createElement("div");
             tmpDiv.innerHTML = html;
             return tmpDiv.firstChild;
        },
        EOF:0};

    //
    //   Safari - specific methods
    //
    if (Dom.isSafari) Util.extend(Dom, function () {
        return {
            // page scroll values are stored on the body instead of the html element
            getScrollTop : function (element)
            {
                var scrollTop = element.scrollTop;
                if (element = document.documentElement) {
                    scrollTop = document.body.scrollTop;
                }
                return scrollTop;
            },

            getScrollLeft : function (element)
            {
                var scrollLeft = element.scrollLeft;
                if (element = document.documentElement) {
                    scrollLeft = document.body.scrollLeft;
                }
                return scrollLeft;
            },

            getPageScrollElement : function ()
            {
                return document.body;
            },

            EOF:0
        };
    }());

    //
    // IE - specific methods
    //
    if (Dom.IsIE) Util.extend(Dom, function () {

        /**
         This is a workaround to IE's problem where select's
         and other controls can not be hidden behind a div.
         The reason is that some controls are on a different
         display (windowed) plane.  The trick is to place an iframe
         underneath the div, since a iframe is a special element
         that exists in both window and windowless plane.

         For more info:
         http://support.microsoft.com:80/support/kb/articles/q177/3/78.asp&NoWebContent=1&NoWebContent=1&NoWebContent=1

         There can more then one popup div (ie, doc cover div and wait alert),
         so each div gets their own iframe.
         */

        // list of iframes and popup divs to update on incremental refreshes.
        var AWOverlayIframeList;


        // externally defined function to avoid circular reference (leak)
        var _awoverlayOnResizeHandler = function () {
            ariba.Dom.overlay(this);
        };

        return {
            setPageScroll : function (scrollLeft, scrollTop)
            {
                this.setPageScrollLeft(scrollLeft);
                this.setPageScrollTop(scrollTop);
            },
            
            setPageScrollTop : function (scrollValue)
            {
                this.documentElement().scrollTop = scrollValue;
            },

            getPageScrollTop : function ()
            {
                return this.documentElement().scrollTop;
            },

            setPageScrollLeft : function (scrollValue)
            {
                this.documentElement().scrollLeft = scrollValue;
            },

            getPageScrollLeft : function ()
            {
                return this.documentElement().scrollLeft;
            },

            documentElement : function ()
            {
                return ariba.Dom.IsIE6 ? document.documentElement : document.body;
            },

            ////////////////////////
            // DOM
            ////////////////////////
            getChildren : function (element)
            {
                return element.children;
            },

            getParentElement : function (element)
            {
                return element.parentElement;
            },

            styleSheetRules : function (styleSheet)
            {
                return styleSheet.rules;
            },

            getOuterHTML : function (element)
            {
                return element.outerHTML;
            },

            setOuterHTML : function (element, sHTML)
            {
                element.outerHTML = sHTML;
            },

            registerOverlayIframe : function (iframe)
            {
                if (!AWOverlayIframeList) {
                    AWOverlayIframeList = new Object();
                }
                AWOverlayIframeList[iframe.id] = iframe;
            },

            deregisterOverlayIframe : function (iframe)
            {
                if (AWOverlayIframeList && AWOverlayIframeList[iframe.id]) {
                    AWOverlayIframeList[iframe.id] = null;
                }
            },

            /** todo: clean up stale iframes **/
            updateOverlayIframes : function ()
            {
                if (this.IsIE6Only && AWOverlayIframeList) {
                    var overlayIframeList = AWOverlayIframeList;
                    AWOverlayIframeList = null;
                    for (var i in overlayIframeList) {
                        var overlayIFrame = overlayIframeList[i];
                        if (overlayIFrame) {
                            var hideIframe = true;
                            var divObject = this.getElementById(overlayIFrame.divObjectId);
                            if (this.elementInDom(divObject) &&
                                this.effectiveStyle(divObject, "display") != 'none' &&
                                divObject.overlayIframe == overlayIFrame) {
                                hideIframe = false;
                            }
                            if (hideIframe) {
                                overlayIFrame.style.display = 'none';
                            }
                            else if (divObject.awOnOverlayUpdate) {
                                divObject.awOnOverlayUpdate(divObject, overlayIFrame);
                            }
                        }
                    }
                }
            },

            overlay : function (divObject, needsUpdate) {
                var overlayIframe;
                var iframeDiv;
                var iframeId;
                if (this.IsIE6Only && divObject) {
                    if (divObject.style.display == 'none' ||
                        !divObject.currentStyle) {
                        return;
                    }
                    if (divObject.currentStyle.zIndex == 0) {
                        divObject.style.zIndex = 100;
                    }
                    iframeId = divObject.id + 'IFrame';
                // see if there is a cached iframe on the client
                    overlayIframe = divObject.overlayIframe;
                    if (!overlayIframe) {
                        // see if there is one rendered by the server
                        overlayIframe = this.getElementById(iframeId);
                    }
                    if (!overlayIframe) {
                        // create one
                        iframeId = divObject.id + 'IFrame';
                        iframeDiv = document.createElement('span');

                        var containerParent = this.containerParent(divObject);
                        containerParent.appendChild(iframeDiv);

                        iframeDiv.innerHTML = "<iframe src='" + this.AWEmptyDocScriptlet + "' id='"
                                + iframeId +
                                              "' style='position:absolute;top:0px;left:0px;display:none;filter:alpha(opacity=000);background-color:#FFFFFF'></iframe>";
                        overlayIframe = this.getElementById(iframeId);
                    }
                // cache it
                    divObject.overlayIframe = overlayIframe;

                // iframe needs to be updated on incremental refreshes, default is true
                    if (typeof(needsUpdate) == "undefined" || needsUpdate) {
                        overlayIframe.divObjectId = divObject.id;
                        this.registerOverlayIframe(overlayIframe);
                    }

                // style and show it
                    overlayIframe.style.width = divObject.offsetWidth;
                    overlayIframe.style.height = divObject.offsetHeight;
                    if (divObject.style.top) {
                        overlayIframe.style.top = divObject.style.top;
                    }
                    else {
                        overlayIframe.style.top = this.absoluteTop(divObject);
                    }
                    if (divObject.style.left) {
                        overlayIframe.style.left = divObject.style.left;
                    }
                    else {
                        overlayIframe.style.left = this.absoluteLeft(divObject)
                    }
                    overlayIframe.style.zIndex = divObject.currentStyle.zIndex - 1;
                    overlayIframe.style.display = 'block';
                    if (!divObject.onresize) {
                        divObject.onresize = _awoverlayOnResizeHandler;
                    }
                }
            },

            unoverlay : function (divObject) {
                var overlayIframe;
                if (this.IsIE6Only && divObject) {
                    overlayIframe = divObject.overlayIframe;
                    if (divObject.overlayIframe) {
                        overlayIframe.style.display = 'none';
                        this.deregisterOverlayIframe(overlayIframe);
                        divObject.onresize = null;
                    }
                }
            },

            EOF:0};
    }());

    //
    // Mozilla - specific methods
    //
    if (!Dom.IsIE) Util.extend(Dom, function () {

        /**
         Outer HTML support.  Can't use prototype approach since not all browsers
         support it, and extension needs to be declared in each frame we use.
         */
        var _emptyTags = {
            "IMG":   true,
            "BR":    true,
            "INPUT": true,
            "META":  true,
            "LINK":  true,
            "PARAM": true,
            "HR":    true
        };

        return {

            setPageScroll : function (scrollLeft, scrollTop)
            {
                window.scroll(scrollLeft, scrollTop);
            },

            setPageScrollTop : function (scrollValue)
            {
                window.scroll(this.getPageScrollLeft(), scrollValue);
            },

            getPageScrollTop : function ()
            {
                return window.pageYOffset;
            },

            setPageScrollLeft : function (scrollValue)
            {
                window.scroll(scrollValue, this.getPageScrollTop());
            },

            getPageScrollLeft : function ()
            {
                return window.pageXOffset;
            },

            documentElement : function ()
            {
                return document.documentElement;
            },

            getChildren : function (element)
            {
                return element.childNodes;
            },

            getParentElement : function (element)
            {
                return element.parentNode;
            },

            styleSheetRules : function (styleSheet)
            {
                // This cannot be made to work with NS7.1
                return null;
            },

            getOuterHTML : function (element)
            {
                var attrs = element.attributes;
                var outerHTML = "<" + element.tagName;
                for (var i = 0; i < attrs.length; i++) {
                    outerHTML += " " + attrs[i].name + "=\"" + attrs[i].value + "\"";
                }
                if (_emptyTags[this.tagName]) {
                    return outerHTML + ">";
                }
                return outerHTML + ">" + element.innerHTML + "</" + element.tagName + ">";
            },

            setOuterHTML : function (element, sHTML)
            {
                var r = element.ownerDocument.createRange();
                r.setStartBefore(element);
                var df = r.createContextualFragment(sHTML);
                element.parentNode.replaceChild(df, element);
            },

            EOF:0};
    }());

    return Dom;
}();
