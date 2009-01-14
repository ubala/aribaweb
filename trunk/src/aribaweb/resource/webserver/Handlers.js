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

    // private vars
    var AWFormActionKey = 'awfa';
    var AWDidChangeKey = "awdidchg";
    var AWPopupSelectedCaptured = 'AWPopupSelectedCaptured';
    var AWTextFieldChangedId = null;

    // caps lock checks
    var AWCapsLockErrorDiv;
    var AWDisableMouseClick = false;

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
            var keyCode = mevent.keyCode;
        // accept CR or TAB keys
            if (keyCode == 13 || keyCode == 9) {
                var selectedCaptured = popup.getAttribute(AWPopupSelectedCaptured);
                if (popup.getAttribute(AWDidChangeKey) == "1" ||
                    (!Util.isNullOrUndefined(selectedCaptured) &&
                     (popup.selectedIndex != selectedCaptured))) {
                    this.AWActionPopupEnabled = true;
                    return this.actionPopupChanged(popup, mevent);
                }
            }
                // 38 == uparrow   40 == downarrow
            else if (keyCode == 38 || keyCode == 40) {
                popup.setAttribute(AWDidChangeKey, "1");
                popup.setAttribute(AWPopupSelectedCaptured, null);
                this.AWActionPopupEnabled = false;
            }
                // capture current selected index in case the key changes the selection
            else {
                this.AWActionPopupEnabled = false;
                popup.setAttribute(AWPopupSelectedCaptured, popup.selectedIndex);
            }
            return true;
        },

        textFieldRefresh : function (formId, senderId)
        {
            var formObject = Dom.getElementById(formId);
            Dom.addFormField(formObject, Request.AWSenderIdKey, senderId);
            Request.submitForm(formObject, null);
            return false;
        },

        resetTextFieldChanged : function ()
        {
            if (AWTextFieldChangedId != null) {
                AWTextFieldChangedId = null;
                document.onclick = null;
            }
        },

        textRefresh : function (mevent, textField)
        {
            var keyCode = mevent.keyCode;
            if ((keyCode == 13 && textField.nodeName != 'TEXTAREA') || keyCode == 9) {
                if (textField.getAttribute(AWDidChangeKey) == "1") {
                    textField.setAttribute(AWDidChangeKey, "0");
                    return this.textFieldRefresh(textField.form.id, textField.name);
                }
            }
            else {
                textField.setAttribute(AWDidChangeKey, "1");
                if (document.onclick == null) {
                    Event.registerUpdateCompleteCallback(this.resetTextFieldChanged.bind(this));
                    AWTextFieldChangedId = textField.id;
                    document.onclick = function (mevent) {
                        if (AWTextFieldChangedId != null) {
                            mevent = mevent ? mevent : event;
                            var textField = Dom.getElementById(AWTextFieldChangedId);
                            if (textField != null) {
                                var formObject = textField.form;
                                return Handlers.textFieldRefresh(formObject.id, textField.name)
                            }
                        }
                    }.bindDocHandler(this);
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
                if (evt.keyCode == 20 || evt.keyCode == 8) {
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

        textNoSubmit : function (mevent, textField)
        {
            if (Event.keyCode(mevent) == 13 && !Event.hasHandler(textField.form, "keypress"))
            {
                Event.cancelBubble(mevent);
                return false;
            }
        },

        virtualFormKeyPress : function (spanObject, mevent)
        {
            mevent = mevent ? mevent : event;
            var keyCode = Event.keyCode(mevent);
            if (keyCode == 13) {
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
            if ((mevent.type == "keypress") && (Event.keyCode(mevent) != 13)) {
                return true;
            }
            return Request.submitFormForElementName(formName, elementId, mevent);
        },

        hPopupChanged : function (popup, mevent)
        {
            return this.actionPopupChanged(popup, mevent);
        },

        hLinkClick : function (senderId, windowName, windowAttributes, mevent)
        {
            if (AWDisableMouseClick) {
                AWDisableMouseClick = false;
                return false;
            }
            if ((mevent.type == "keypress") && (Event.keyCode(mevent) != 13)) {
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

        tagOnClick : function (tagObject, formId, windowName, actionName, mevent, addValue, windowAttributes)
        {
            return Request.senderClicked(tagObject.id, formId, windowName, actionName, mevent, addValue, tagObject.value, windowAttributes);
        },

        tagOnKeyPress : function (tagObject, formId, windowName, actionName, mevent, windowAttributes)
        {
            if (mevent.keyCode == 13) {
                this.tagOnClick(tagObject, formId, windowName, actionName, mevent, windowAttributes);
                return false;
            }
            return true;
        },

        formCR : function (formObject, mevent)
        {
            if (Event.keyCode(mevent) == 13) {
                // restrict to only text and password field
                var srcElement = Event.eventSourceElement(mevent);
                var isTextField = false;
                if (srcElement && srcElement.nodeName == 'INPUT') {
                    isTextField =
                    srcElement.type == 'text' || srcElement.type == 'password';
                }
                if (isTextField) {
                    var formAction = formObject[AWFormActionKey];
                    Event.cancelBubble(mevent);
                    Dom.addFormField(formObject, Request.AWSenderIdKey, formAction.value);
                    Request.submitForm(formObject);
                    return false;
                }
            }
            return true;
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

        EOF:0};

    //********************************************************************
    // register handlers

    // GenericActionTag
    var GAT = {
        click : function (elm, evt) {
            if (Dom.boolAttr(elm, "_dC", false)) return true;
            var formId = Dom.boolAttr(elm, "_sf", true) ? Dom.lookupFormId(elm) : null;
            return ariba.Handlers.tagOnClick(elm, formId, elm.getAttribute("_t"), elm.getAttribute("_a"),
                    evt, Dom.boolAttr(elm, "_av"), elm.getAttribute("_w"));
        },


        keypress : function (elm, evt) {
            if (Dom.boolAttr(elm, "_dC", false)) return true;
            return (evt.keyCode == 13) ? GAT.click(elm, evt) : true;
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
                return ariba.Handlers.textNoSubmit(evt, elm);
            },
            keydown : function (elm, evt) {
                var type = elm.getAttribute('_tf');
                if (!type) return true;
                var formId = Dom.lookupFormId(elm)
                return (type == "AC") ? ariba.Handlers.hTagKeyDown(elm, formId, null, null, evt, false, null)
                     : (type == "ROKP") ? ariba.Handlers.hTagRefreshKeyDown(elm, formId, null, null, evt, false, null)
                     : ariba.Handlers.textRefresh(evt, elm);
            }
        },

        // Rollover
        ROV : {
            mouseover : function (elm, evt) {
                var s = elm.getAttribute('roClass');
                elm.setAttribute("origClass", elm.className);
                elm.className = s;
                return true;
            },

            mouseout : function (elm, evt) {
                elm.className = elm.getAttribute('origClass');;
                return true;
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

