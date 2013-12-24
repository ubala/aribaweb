/*
    Handlers.js

    JavaScript actions and behaviors for native HTML elements (links, selects, ...)
*/

ariba.Handlers = function() {
    // imports
    var Event = ariba.Event;
    var Input = ariba.Input;
    var Request = ariba.Request;
    var Dom = ariba.Dom;
    var Util = ariba.Util;

    // private vars
    var AWFormActionKey = 'awfa';
    var AWDidChangeKey = "awdidchg";
    var AWPopupSelectedCaptured = 'AWPopupSelectedCaptured';
    var AWTextFieldChangedId = null;

    // caps lock checks
    var AWCapsLockErrorDiv;
    var AWDisableMouseClick = false;

    var _viewportContainerScrollInited = false;
    var _viewportContainerIds = [];

    // private functions
    function updateTextPlaceHolder (textField)
    {
        var placeholder = textField.getAttribute('_pl');
        if (placeholder) {
            var value = textField.value;
            if (placeholder == value) {
                textField.value = "";
                Dom.removeClass(textField, "ph");
            }
        }
    }

    var Handlers = {

       // Public Globals
       AWActionPopupEnabled : true,

        mouseWheelOnPopup : function (popup, event)
        {
            // cancel event
            Event.cancelBubble(event);
            return false;
        },

        actionPopupChanged : function (popup, mevent)
        {
            if (this.AWActionPopupEnabled) {
                popup.setAttribute(AWDidChangeKey, "0");
                var selectedOption = popup.options[popup.selectedIndex];
                var selectedPopupValue = selectedOption.value;
                if (selectedPopupValue == "awnop") {
                    popup.selectedIndex = popup.selectedIndex - 1;
                    return false;
                }
                this.AWActionPopupEnabled = false;
                var formObject;
                if (selectedPopupValue == 'awaction') {
                    formObject = popup.form;
                    Dom.addFormField(formObject, Request.AWSenderIdKey, selectedOption.id);
                    Request.submitForm(formObject, null, null, true);
                }
                else {
                    formObject = popup.form;
                    Dom.addFormField(formObject, Request.AWSenderIdKey, popup.name);
                    Request.submitForm(formObject, null, null, true);
                }
                return true;
            }
            this.AWActionPopupEnabled = true;
            return true;
        },

        actionPopupKeyDown : function (popup, mevent)
        {
            var keyCode = Event.keyCode(mevent);
            if (keyCode == Input.KeyCodeEnter || keyCode == Input.KeyCodeTab) {
                var selectedCaptured = popup.getAttribute(AWPopupSelectedCaptured);
                if (popup.getAttribute(AWDidChangeKey) == "1" ||
                    (!Util.isNullOrUndefined(selectedCaptured) &&
                     (popup.selectedIndex != selectedCaptured))) {
                    this.AWActionPopupEnabled = true;
                    return this.actionPopupChanged(popup, mevent);
                }
            }
            else if (keyCode == Input.KeyCodeArrowUp || keyCode == Input.KeyCodeArrowDown) {
                popup.setAttribute(AWDidChangeKey, "1");
                popup.setAttribute(AWPopupSelectedCaptured, null);
                this.AWActionPopupEnabled = false;
            }
                // capture current selected index in case the key changes the selection
            else {
                this.AWActionPopupEnabled = false;
                if (ariba.Dom.IsMoz) {
                    // In Mozilla Firefox, if you type letters using keyboard to choose
                    // an option; if the successive key presses do not change the index
                    // selected, actionPopupChanged doesn't get called. See the check
                    // above before calling actionPopupChanged-
                    //      popup.selectedIndex != selectedCaptured
                    // selectedIndex and selectedCaptured would contain the same value.
                    // So, handling it in an alternative way that the condition above
                    // evaluates to true and actionPopupChanged gets called.
                    popup.setAttribute(AWDidChangeKey, "1");
                }
                else {
                    popup.setAttribute(AWPopupSelectedCaptured, popup.selectedIndex);
                }
            }
            return true;
        },

        textFieldRefresh : function (formId, senderId)
        {
            var formObject = Dom.getElementById(formId);
            Dom.addFormField(formObject, Request.AWSenderIdKey, senderId);
            Request.submitForm(formObject, null);
            return true;
        },

        resetTextFieldChanged : function ()
        {
            if (AWTextFieldChangedId != null) {
                AWTextFieldChangedId = null;
                Event.updateDocHandler("click", null);
            }
        },

        textRefresh : function (mevent, textField)
        {
            var keyCode = Event.keyCode(mevent);
            if (keyCode == Input.KeyCodeShift) return true;
            if ((keyCode == Input.KeyCodeEnter && textField.nodeName != 'TEXTAREA') || keyCode == Input.KeyCodeTab) {
                if (textField.getAttribute(AWDidChangeKey) == "1") {
                    textField.setAttribute(AWDidChangeKey, "0");
                    return this.textFieldRefresh(textField.form.id, textField.name);
                }
            }
            else {
                textField.setAttribute(AWDidChangeKey, "1");
                if (Event.getDocHandler("click") == null) {
                    Event.registerUpdateCompleteCallback(this.resetTextFieldChanged.bind(this));
                    AWTextFieldChangedId = textField.id;
                    var docOnClickHandler = function (mevent) {
                        if (AWTextFieldChangedId != null) {
                            mevent = mevent ? mevent : event;
                            var textField = Dom.getElementById(AWTextFieldChangedId);
                            if (textField != null) {
                                var formObject = textField.form;
                                return Handlers.textFieldRefresh(formObject.id, textField.name)
                            }
                        }
                    }.bindEventHandler(this);
                    Event.updateDocHandler("click", docOnClickHandler);
                }
            }
            return true;
        },

        checkCapsLockErrorTxtRfrsh : function (mevent, textField)
        {
            this.checkCapsLockError(mevent);
            return this.textRefresh(mevent, textField);
        },

        checkCapsLockError : function (evt)
        {
            if (AWCapsLockErrorDiv) {
                if (Event.keyCode(evt) == Input.KeyCodeCapsLock || Event.keyCode(evt) == Input.KeyCodeBackspace) {
                    this.hideCapsLockError();
                }
            }
        },

        hideCapsLockError : function ()
        {
            if (AWCapsLockErrorDiv && AWCapsLockErrorDiv.style.display != "none") {
                Dom.fadeOutElement(AWCapsLockErrorDiv);
                AWCapsLockErrorDiv.style.display = "none";
                AWCapsLockErrorDiv = null;
            }
        },

        noCapsLockTxt : function (mevent, textField, warnid)
        {
            var keyCode = 0;
            var isShift = false;

            if (document.all) {
                // IE
                keyCode = mevent.keyCode;
                isShift = mevent.shiftKey;
            }
            else if (document.getElementById) {
                // FF
                keyCode = mevent.which;
                isShift = mevent.shiftKey;
            }
            else {
                // unsupported
                return true;
            }
        //alert('keycode: ' + keyCode + " isShift " + isShift);

            if ((( keyCode >= 65 && keyCode <= 90 ) && !isShift ) ||
                (( keyCode >= 97 && keyCode <= 122 ) && isShift )) {
                // upper case letters without shift key so caps lock or
                // lower case letters with shift key so caps lock
                var div = Dom.getElementById(warnid);
                if (div && div.style.display != 'block') {
                    div.style.display = 'block';
                    var newTop = Dom.absoluteTop(textField);
                    var newLeft = Dom.absoluteLeft(textField) + textField.offsetWidth + 2;
                    div.style.top = Dom.correctForBottomEdge(newTop, div) + 'px';
                    div.style.left = Dom.correctForRightEdge(newLeft, div) + 'px';
                    Dom.fadeInElement(div);
                    AWCapsLockErrorDiv = div;
                }
            }
            else {
                this.hideCapsLockError();
            }

            return true;
        },
        hPassFocus : function (passwordField, event)
        {
            Dom.removeClass(passwordField.parentNode, "pfc");
            Input.focus(passwordField);
        },
        hPassBlur : function (passwordField, event)
        {
            if (!passwordField.value) {
                Dom.addClass(passwordField.parentNode, "pfc");
            }
        },
        hTextKeyPress : function (textField, mevent)
        {
            if (Input.isCharChange(mevent)) {
                updateTextPlaceHolder(textField);
            }
        },
        hTextClick : function (textField, mevent)
        {
            updateTextPlaceHolder(textField);       
        },
        hTextBlur : function (textField, mevent)
        {
            var placeholder = textField.getAttribute('_pl');
            if (placeholder) {
                var value = textField.value;
                if (!value) {
                    textField.value = placeholder;
                    Dom.addClass(textField, "ph");
                }
            }
        },
        textNoSubmit : function (mevent, textField)
        {
            if (Event.keyCode(mevent) == Input.KeyCodeEnter) {
                Event.cancelBubble(mevent);
                return false;
            }
        },

        virtualFormKeyPress : function (spanObject, mevent)
        {
            mevent = mevent ? mevent : event;
            var keyCode = Event.keyCode(mevent);
            if (keyCode == Input.KeyCodeEnter) {
                var senderId = spanObject.id;
                var formObject = Dom.findParent(spanObject, "FORM", false);
                Dom.addFormField(formObject, Request.AWSenderIdKey, senderId);
                Request.submitForm(formObject, null);
                Event.cancelBubble(mevent);
            }
            return false;
        },
        /*
            Abbreviated alias
        */
        hSubmit : function (formObject, target)
        {
            Request.submitForm(formObject, target);
        },

        hKeyDown : function (formName, elementId, mevent)
        {
            if ((mevent.type == "keypress") && (Event.keyCode(mevent) != Input.KeyCodeEnter)) {
                return true;
            }
            return Request.submitFormForElementName(formName, elementId, mevent);
        },

        hPopupChanged : function (popup, mevent)
        {
            // Special code for IE here. actionPopupChanged can get called here or from
            // actionPopupKeyDown for Enter key press. IE used to require pressing Enter
            // key twice for it to fire the action. With this check for IE, we set
            // AWActionPopupEnabled to true so when the change event is fired in IE on
            // hitting the Enter key the first time, we can fire the action i.e.
            // Request.submitForm in actionPopupChanged.  
            if (ariba.Dom.IsIE) {
                this.AWActionPopupEnabled = true;
            }
            return this.actionPopupChanged(popup, mevent);
        },

        hLinkClick : function (senderId, windowName, windowAttributes, mevent)
        {
            if (AWDisableMouseClick) {
                AWDisableMouseClick = false;
                return false;
            }
            if ((mevent.type == "keypress") && (Event.keyCode(mevent) != Input.KeyCodeEnter)) {
                return true;
            }
            Request.gotoLink(senderId, windowName, windowAttributes, mevent);
            return false;
        },

        hTagClick : function (tagObject, formId, windowName, actionName, mevent, submitValue, windowAttributes)
        {
            return this.tagOnClick(tagObject, formId, windowName, actionName, mevent, submitValue, windowAttributes);
        },

        hTagKeyDown : function (tagObject, formId, windowName, actionName, mevent, windowAttributes)
        {
            return this.tagOnKeyPress(tagObject, formId, windowName, actionName, mevent, windowAttributes);
        },

        hTagRefreshKeyDown : function (tagObject, formId, windowName, actionName, mevent, windowAttributes)
        {
            if (this.tagOnKeyPress(tagObject, formId, windowName, actionName, mevent, windowAttributes)) {
                return this.textRefresh(mevent, tagObject);                
            }
            return false;
        },

        hPopupAction : function (popup, mevent)
        {
            var selectedOption = popup.options[popup.selectedIndex];
            if (selectedOption.value.match(/^aw/) == null) {
                this.AWActionPopupEnabled = false;
            }
            return this.actionPopupChanged(popup, mevent);
        },

        hSubmitAtIndex : function (formIndex, hiddenFieldName, value)
        {
            return Request.submitFormAtIndexWithHiddenField(formIndex, hiddenFieldName, value)
        },

        hOpenWindow : function (urlString, windowName, attributesString)
        {
            return Dom.openWindow(urlString, windowName, attributesString);
        },

        hActionPopupKeyDown : function (popup, mevent)
        {
            return this.actionPopupKeyDown(popup, mevent);
        },

        hMouseWheelOnPopup : function (popup, mevent)
        {
            return this.mouseWheelOnPopup(popup, mevent);
        },

        hVirtualFormKeyPress : function (spanObject, mevent)
        {
            return this.virtualFormKeyPress(spanObject, mevent);
        },

        /**
         * This is a front for Request.senderClicked().
         * @see senderClicked
         * @param tagObject
         * @param formId
         * @param windowName
         * @param actionName
         * @param mevent
         * @param addValue
         * @param windowAttributes
         */
        tagOnClick : function (tagObject, formId, windowName, actionName, mevent, addValue, windowAttributes)
        {
            return Request.senderClicked(tagObject.id, formId, windowName, actionName, mevent, addValue, tagObject.value, windowAttributes);
        },

        tagOnKeyPress : function (tagObject, formId, windowName, actionName, mevent, windowAttributes)
        {
            if (Event.keyCode(mevent) == Input.KeyCodeEnter) {
                this.tagOnClick(tagObject, formId, windowName, actionName, mevent, windowAttributes);
                ariba.Event.cancelBubble(mevent);
                return false;
            }
            return true;
        },

        fireActionInScope : function (formObject, mevent)
        {
            if (Event.keyCode(mevent) == Input.KeyCodeEnter) {
                // restrict to only text and password field
                var srcElement = Event.eventSourceElement(mevent);
                var isTextField = false;
                if (srcElement && srcElement.nodeName == 'INPUT') {
                    isTextField =
                    srcElement.type == 'text' || srcElement.type == 'password';
                }
                if (isTextField) {
                    var ret = ariba.Handlers.fireDefaultActionButton(formObject, mevent);
                    if (Dom.boolAttr(formObject, "_hfa", false) && Event.shouldBubble(mevent)) {
                        var formAction = formObject[AWFormActionKey];
                        Event.cancelBubble(mevent);
                        Dom.addFormField(formObject, Request.AWSenderIdKey, formAction.value);
                        Request.submitForm(formObject);
                        ret = false;
                    }
                    return ret;
                }
            }
            return true;
        },

        fakeClick : function (elm, evt)
        {
            if (elm) {
                Event.elementInvoke(elm, "mousein");
                Event.elementInvoke(elm, "mousedown");

                var result = Event.elementInvoke(elm, "click");
                Event.cancelBubble(evt);
                var cb = function () {
                    if (Dom.elementInDom(elm)) Event.elementInvoke(elm, "mouseout");
                }.bind(this);
                
                if (Request.isRequestInProgress()) {
                    Event.registerUpdateCompleteCallback(cb);
                } else {
                    setTimeout(cb, 1000);
                }

                return result;
            }
            return true;
        },

        fireDefaultActionButton : function (elm, evt)
        {
            var actionElm = Dom.findChildUsingPredicate(elm, function (e) { return e.tagName && Dom.boolAttr(e, "_isdef", false) });
            return this.fakeClick(actionElm, evt);
        },

        ////////////////
        // Disable Input
        ////////////////
        ignoreKey : function (mevent)
        {
            mevent = mevent ? mevent : event;
            Event.preventDefault(mevent);
            Event.cancelBubble(mevent);
            return false;
        },

        ignoreKeyDown : function (mevent)
        {
            Input.showWaitAlert();
            this.ignoreKey(mevent);
            return false;
        },

        _awHandlers_MARKER : function () {
        },

        ////////////////
        // Hover Behavior
        ////////////////

        hoverControlOver : function (hoverControl) {
            var hoverContainer = Dom.findParentUsingPredicate(hoverControl,
                function (e) {
                    return e.getAttribute("bh") == "AWHC";
                });
            if (hoverContainer) {
                Dom.setState(hoverContainer, "hover");
            }
        },

        hoverContainerOver : function (hoverContainer) {            
            Event.clearTimeout(hoverContainer);
        },

        hoverContainerOut : function (hoverContainer) {
            var hoverOut = function() {
                Dom.unsetState(hoverContainer, "hover");
            };
            Event.setTimeout(hoverContainer, hoverOut, 500);
            return true;            
        },

        ////////////////
        // Viewport Behavior
        ////////////////
        initViewportContainer : function (containerId)
        {
            if (!_viewportContainerScrollInited) {
                _viewportContainerScrollInited = true;
                var checkViewport = this.checkViewportContainers.bind(this);
                var enqueueCheckViewport = function () {
                    Event.eventEnqueue(checkViewport);
                };
                Event.registerRefreshCallback(enqueueCheckViewport);
                Event.registerWindowOnScroll(this.checkViewportContainers.bind(this));
            }
            Util.arrayAddIfNotExists(_viewportContainerIds, containerId);
        },

        checkViewportContainers : function ()
        {
            var i, containerId, elm, viewportState;
            var inDomViewportContainerIds = [];
            for (i = 0; i < _viewportContainerIds.length; i++) {
                containerId = _viewportContainerIds[i];
                elm = Dom.getElementById(containerId);
                if (elm) {
                    Util.arrayAddIfNotExists(inDomViewportContainerIds, containerId);
                    Dom.setViewportState(elm);
                }
            }
            _viewportContainerIds = inDomViewportContainerIds;
        },

        EOF:0};

    //********************************************************************
    // register handlers

    // GenericActionTag
    var GAT = {
        click : function (elm, evt) {
            if (elm.getAttribute("_sL")) {Request.redirect(elm.getAttribute("_sL")); return true; }
            if (Dom.boolAttr(elm, "_dC", false)) return true;
            var formId = Dom.boolAttr(elm, "_sf", true) ? Dom.lookupFormId(elm) : null;
            return ariba.Handlers.tagOnClick(elm, formId, elm.getAttribute("_t"), elm.getAttribute("_a"),
                    evt, Dom.boolAttr(elm, "_av"), elm.getAttribute("_w"));
        },


        keypress : function (elm, evt) {
            if (Dom.boolAttr(elm, "_dC", false)) return true;
            return (Event.keyCode(evt) == Input.KeyCodeEnter) ? GAT.click(elm, evt) : true;
        }
    };

    Event.registerBehaviors({
        GAT: GAT,

        // HyperLink
        HL : {
            prototype : GAT
        },

        // Text Field
        TF : {
            keypress : function (elm, evt) {
                ariba.Handlers.hTextKeyPress(elm, evt);
                return ariba.Handlers.textNoSubmit(evt, elm);
            },
            keydown : function (elm, evt) {
                var type = elm.getAttribute('_tf');
                if (!type) return true;
                var formId = Dom.lookupFormId(elm)
                return (type == "AC") ? ariba.Handlers.hTagKeyDown(elm, formId, null, null, evt, false, null)
                     : (type == "ROKP") ? ariba.Handlers.hTagRefreshKeyDown(elm, formId, null, null, evt, false, null)
                     : ariba.Handlers.textRefresh(evt, elm);
            },
            click : function (elm, evt) {
                ariba.Handlers.hTextClick(elm, evt);
                return true;
            }
        },

        PF : {
            click : function (elm, evt) {
                var passwordField = Dom.findChild(elm, "INPUT");
                ariba.Handlers.hPassFocus(passwordField);
                return true;
            }
        },
        
        TA : {
            // onKeyUp="$limitTextJavaScriptString" onKeyDown="$onKeyDownString"
            keyup : function (elm, evt) {
                ariba.Dom.limitTextLength(elm, elm.getAttribute("_mL"));
            },

            keydown : function (elm, evt) {
                return Dom.boolAttr(elm, "_isRF", false) ? ariba.Handlers.textRefresh(evt,elm) : true;
            }
        },

        // Action Scope
        AS : {
            keydown : function (elm, evt) {
                Handlers.fireActionInScope(elm, evt);
            }
        },

        // Rollover
        ROV : {
            mouseover : function (elm, evt) {
                var s = elm.getAttribute('roClass') || "hov";
                elm.setAttribute("origClass", elm.className);
                elm.className = s;
                return true;
            },

            mouseout : function (elm, evt) {
                elm.className = elm.getAttribute('origClass');
                return true;
            }
        },

        // Generic Hover
        GH : {
            mouseover : function (elm, evt) {
                Dom.setState(elm, "hover");
            },
            mouseout : function (elm, evt) {
                Dom.unsetState(elm, "hover");
            }
        },        

        // Hover Container
        AWHC : {
            mouseover : function (hoverContainer, evt) {
                ariba.Handlers.hoverContainerOver(hoverContainer);

            },
            mouseout : function (hoverContainer, evt) {
                ariba.Handlers.hoverContainerOut(hoverContainer);
            }
        },

        // Hover Control
        AWHCT : {
            mouseover : function (hoverControl, evt) {
                ariba.Handlers.hoverControlOver(hoverControl);
            }
        }

    });


    if (window == ariba.awCurrWindow) {
        // Need to do this here because Dom shouldn't depend on Event...
        if (Dom.IsIE6Only) {
            Event.registerRefreshCallback(Dom.updateOverlayIframes.bind(Dom));
        }
    }

    return Handlers;
}();

