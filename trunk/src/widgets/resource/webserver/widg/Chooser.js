/*
    Chooser.js  - type-ahead combo-box chooser

    (see ariba.ui.widgets.Chooser.awl)    
*/

ariba.Chooser = function() {
    // imports
    var Util = ariba.Util;
    var Menu = ariba.Menu;
    var Event = ariba.Event;
    var Debug = ariba.Debug;
    var Dom = ariba.Dom;
    var Request = ariba.Request;

    // private vars
    var CPBackgroundColor = "#ccc";
    var CPCurrentCompletionPopup = null;
    var AWChooserInfo = new Object();
    var AWChooserPickListMode = new Object();
    var AWChooserAutoCompleteMode = new Object();


    var Chooser = {

        cPCompletionPopup : function (spanId, ignoreCase, minPrefixLength)
        {
            this._keyDownTimeoutId = 0;
            this._backspaceTimeoutId = 0;
            this._fetchedDataPrefix = "";
            this._selectedItem = null;
            this._enclosingSpan = Dom.getElementById(spanId);
            this._enclosingSpan.cpcompletionPopup = this;
            this._popupDiv = Dom.findChild(this._enclosingSpan, "DIV");
            this._textField = Dom.findChild(this._enclosingSpan, "INPUT");
            this._textFieldOriginalValue = this._textField.value;
        // cannot simply assign since we may get null or undefined for this arg
            this._ignoreCase = ignoreCase;
            this._minPrefixLength = minPrefixLength;
            this._hasOthers = false;

            if (!Dom.IsIE) {
                this._popupDiv.style.overflow = "auto";
            }

            this.handleKeyDown = function (mevent) {
                var keyCode = Event.keyCode(mevent);
                clearTimeout(this._keyDownTimeoutId);
                if (keyCode == 16) {
                    // ignore Shift key
                }
                    // escape
                else if (keyCode == 27) {
                    this.hidePopupDiv();
                }
                    // up arrow
                else if (keyCode == 38) {
                    this.selectPreviousItem(1);
                }
                    // down arrow
                else if (keyCode == 40) {
                    this.selectNextItem(1);
                }
                    // page up
                else if (keyCode == 33) {
                    var pageSize = this.pageSize();
                    var selectedItem = this._selectedItem;
                    while (pageSize > 0) {
                        pageSize = pageSize - 1;
                        selectedItem = this.findPreviousItem(selectedItem, false);
                    }
                    this.setSelectedItem(selectedItem);
                }
                    // page down
                else if (keyCode == 34) {
                    var pageSize = this.pageSize();
                    var selectedItem = this._selectedItem;
                    while (pageSize > 0) {
                        pageSize = pageSize - 1;
                        selectedItem = this.findNextItem(selectedItem, false);
                    }
                    this.setSelectedItem(selectedItem);
                }
                    // CR == 13 tab == 9
                else if (keyCode == 13 || keyCode == 9) {
                    if (this._popupDiv.cpisVisible) {
                        this.setTextFieldValue(this._selectedItem);
                        this.hidePopupDiv();
                    }
                }
                else {
                    if (this._fetchedDataPrefix == "") {
                        this._keyDownTimeoutId = this.fetchListWithDelay(500);
                    }
                        // backspace == 8
                    else if (keyCode == 8 && this._fetchedDataPrefix.length >= this._textField.value.length) {
                        // refetch with broader search
                        clearTimeout(this._backspaceTimeoutId);
                        this._backspaceTimeoutId = this.fetchListWithDelay(250);
                    }
                    else {
                        if (this._hasOthers) {
                            this._keyDownTimeoutId = this.fetchListWithDelay(500);
                        }
                    // hack use setTimeout() to allow character to
                        // get into the textfield before we process it.
                        CPCurrentCompletionPopup = this;
                        setTimeout(CPCurrentCompletionPopup.filterList.bind(CPCurrentCompletionPopup), 1);
                    }
                }
            }

            this.handleMouseDown = function (mevent) {
                var item = Event.eventSourceElement(mevent);
                if (item.cpinnerText == null && item != this._popupDiv && item != this._enclosingSpan) {
                    this.hidePopupDiv();
                }
            }

            this.handleMouseDownNS = function (mevent) {
                // The way event capture works in netscape, we must
                // communicate via the global CPCurrentCompletionPopup
                // see showPopupDiv() below
                CPCurrentCompletionPopup.handleMouseDown(mevent);
            }

            this.handleClick = function (mevent) {
                var item = Event.eventSourceElement(mevent);
                if (item == this._textField) {
                    // user clicked the textField
                    // todo: do we need this?
                    //this._keyDownTimeoutId = this.fetchListWithDelay(1500);
                }
                else if (item != this._popupDiv) {
                    if (item.cpinnerText != null) {
                        this.setTextFieldValue(item);
                        Event.preventDefault(mevent);
                    }
                    this.hidePopupDiv();
                }
            }

            this.handleMouseOver = function (mevent) {
                var item = Event.eventSourceElement(mevent);
                if (Dom.IsIE && (item == this._popupDiv || item.cpinnerText != null)) {
                    // disable capture while mouse is within
                    // bounds of the popupdiv.  This compensates for
                    // a bug in IE where the mouseDown event on the
                    // scrollbar for a div is messed up (sets
                    // item to whatever is behind it)
                    this._enclosingSpan.releaseCapture();
                }
                if (item.cpinnerText != null) {
                    this.setSelectedItem(item);
                }
            }

            this.handleMouseOut = function (mevent) {
                var item = Event.eventSourceElement(mevent);
                if (Dom.IsIE && (item == this._popupDiv || item.cpinnerText != null)) {
                    // re-enable setCapture for explanation,
                    // see comment in this.handleMouseOver()
                    this._enclosingSpan.setCapture(false);
                }
            }

            this.handleTextFieldFocus = function () {
                clearTimeout(this._keyDownTimeoutId);
                this._keyDownTimeoutId = this.fetchListWithDelay(1500);
            }

            /*
                    this._textField.onfocus = function () {
                        // in this context, "this" is the _textField
                        this.parentNode.cpcompletionPopup.handleTextFieldFocus();
                    }
            */

            this.fetchListWithDelay = function (delay) {
                CPCurrentCompletionPopup = this;
                return setTimeout(CPCurrentCompletionPopup.fetchList.bind(CPCurrentCompletionPopup), delay);
            }

            this.fetchList = function () {
                var prefix = this._textField.value;
                this._fetchedDataPrefix = prefix;
                if (prefix.length < this._minPrefixLength) {
                    return;
                }

                var urlString = Request.formatSenderUrl(this._enclosingSpan.id);
            // todo: need to url encode the _fetchedDataPrefix
                urlString = urlString + "&cpprefix=" + prefix;
                var xmlHttp = Request.getXMLHttp();
            // synchronous, for now.
                xmlHttp.open("GET", urlString, false);
                xmlHttp.send(null);
                var responseText = xmlHttp.responseText;
                if (responseText.length > 0) {
                    this._popupDiv.innerHTML = responseText;

                    // preconvert the innerText's to lowercase for case-insensitive filtering
                    // we could do this on the server, but then we'd be transmitting ~2x data.
                    var items = this._popupDiv.childNodes;
                    var length = items.length;
                    var ignoresCase = this._ignoreCase;
                    for (var index = 0; length > index; index++) {
                        var item = items[index];
                        item.cpisVisible = true;
                        var innerText = Dom.getInnerText(item);
                        item.cpinnerText = ignoreCase ? innerText.toLowerCase() : innerText;
                        if (index == (length - 1) && item.className.indexOf("CPOthers") != -1) {
                            item.cpothers = true;
                            this._hasOthers = true;
                            var CPOthersLocalizedTextSpan = Dom.getElementById("CPOthersLocalizedText");
                            Dom.copyInnerText(CPOthersLocalizedTextSpan, item);
                        }
                    }
                    this.showPopupDiv();
                    this.positionPopupDiv();
                    this.filterList();
                }
            }

            this.filterList = function () {
                var textField = this._textField;
                var popupDiv = this._popupDiv;
            // all comparisons are lowercase (case-insensitive)
                var prefix = textField.value;
                if (this._ignoreCase) {
                    prefix = prefix.toLowerCase();
                }
                var items = popupDiv.childNodes;
                var length = items.length;
                this.setSelectedItem(null);
                for (var index = 0; length > index; index++) {
                    var item = items[index];
                    var string = item.cpinnerText;
                    if (string.indexOf(prefix) == 0 || item.cpothers == true) {
                        this.showElement(item);
                        if (this._selectedItem == null) {
                            this.setSelectedItem(item);
                        }
                    }
                    else {
                        this.hideElement(item);
                    }
                }

                this.showPopupDiv();
            // adjust the height of the scrolling div
                popupDiv.style.height = null;
                if (parseInt(popupDiv.offsetHeight) > 100) {
                    if (Dom.IsIE) {
                        popupDiv.style.paddingRight = 16;
                        popupDiv.style.height = 100;
                    }
                    else {
                        popupDiv.style.height = "100px";
                    }
                }
                else {
                    popupDiv.style.paddingRight = null;
                }
            }

            this.selectNextItem = function () {
                var item = this.findNextItem(this._selectedItem, true);
                this.setSelectedItem(item);
            }

            this.selectPreviousItem = function () {
                var item = this.findPreviousItem(this._selectedItem, true);
                this.setSelectedItem(item);
            }

            this.findNextItem = function (selectedItem, allowWrap) {
                var items = this._popupDiv.childNodes;
                var length = items.length;
                var selectedIndex = Util.indexOf(items, selectedItem)
                for (index = selectedIndex + 1; length > index; index++) {
                    var item = items[index];
                    if (item.cpisVisible == true) {
                        return item;
                    }
                }
                if (allowWrap) {
                    for (index = 0; selectedIndex > index; index++) {
                        var item = items[index];
                        if (item.cpisVisible) {
                            return item;
                        }
                    }
                }
                return items[length - 1];
            }

            this.findPreviousItem = function (selectedItem, allowWrap) {
                var items = this._popupDiv.childNodes;
                var selectedIndex = Util.indexOf(items, selectedItem)
                for (var index = selectedIndex - 1; index > -1; index--) {
                    var item = items[index];
                    if (item.cpisVisible == true) {
                        return item;
                    }
                }
                if (allowWrap) {
                    var length = items.length;
                    for (index = length - 1; index > selectedIndex; index--) {
                        var item = items[index];
                        if (item.cpisVisible == true) {
                            return item;
                        }
                    }
                }
                return items[0];
            }

            this.pageSize = function () {
                var itemHeight = this._popupDiv.childNodes[0].offsetHeight;
                if (itemHeight == 0) {
                    itemHeight = 1;
                }
                var pageSize = this._popupDiv.clientHeight / itemHeight;
                return parseInt(pageSize);
            }

            this.setSelectedItem = function (item) {
                if (this._selectedItem != null) {
                    this._selectedItem.style.backgroundColor = "white";
                }
                this._selectedItem = item;
                if (this._selectedItem != null) {
                    this._selectedItem.style.backgroundColor = CPBackgroundColor;
                    this.scrollToVisible(item);
                }
                else {
                    this.hidePopupDiv();
                }
            }

            this.scrollToVisible = function (item) {
                var popupDiv = this._popupDiv;
                var divHeight = popupDiv.clientHeight;
                var effectiveOffset = item.offsetTop - popupDiv.scrollTop;
                if ((effectiveOffset + item.offsetHeight) >= divHeight) {
                    popupDiv.scrollTop = item.offsetTop - divHeight + item.offsetHeight;
                }
                else if (0 > effectiveOffset) {
                    popupDiv.scrollTop = item.offsetTop;
                }
            }

            this.showPopupDiv = function () {
                this.showElement(this._popupDiv);
                if (Dom.IsIE) {
                    this._enclosingSpan.setCapture(false);
                }
                else {
                    window.addEventListener("mousedown", this.handleMouseDownNS, true);
                }
            }

            this.hidePopupDiv = function () {
                this.hideElement(this._popupDiv);
                if (Dom.IsIE) {
                    this._enclosingSpan.releaseCapture();
                }
                else {
                    CPCurrentCompletionPopup = this;
                    window.removeEventListener("mousedown", this.handleMouseDownNS, true);
                }
            }

            this.positionPopupDiv = function () {
                var textField = this._textField;
                var popupDiv = this._popupDiv;
                var top = (Dom.absoluteTop(textField) + textField.offsetHeight);
                var left = Dom.absoluteLeft(textField);
                if (!Dom.IsIE) {
                    top = top + "px";
                    left = left + "px";
                }
                popupDiv.style.top = top;
                popupDiv.style.left = left;
            }

            this.setTextFieldValue = function (item) {
                if (item.cpothers == true) {
                    this._textField.value = this._textFieldOriginalValue;
                // submit the form and fire othersAction
                    var senderId = this._popupDiv.id;
                    var formObject = this._textField.form;
                    Dom.addFormField(formObject, Request.AWSenderIdKey, senderId);
                    Request.submitForm(formObject);
                    Dom.removeFormField(formObject, AWSenderIdKey);
                }
                else {
                    this._textField.value = Dom.getInnerText(item);
                }
            }

            this.hideElement = function (element)
            {
                element.style.display = "none";
                element.cpisVisible = false;
            }

            this.showElement = function (element)
            {
                element.style.display = "";
                element.cpisVisible = true;
            }

            /***** end of CPCompletionPopup ******/
        },

        initChooser : function (chooserId, multiSelect, focus, isInvalid)
        {
            var chooserInfo = new Object();
            AWChooserInfo[chooserId] = chooserInfo;
            var wrapper = Dom.getElementById(chooserId);
            chooserInfo.wrapper = wrapper;
            chooserInfo.noSelectionValue = wrapper.getAttribute('_ns');
            chooserInfo.menuPositionObj = Dom.findChild(wrapper, "TR", false);
            chooserInfo.textField = Dom.findChild(wrapper, "INPUT", false);
            chooserInfo.initValue = chooserInfo.textField.value;
            chooserInfo.modeLink = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "IMG" && Dom.hasClass(e, "chModeLink");
            });
            chooserInfo.pickListImage = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "IMG" && Dom.hasClass(e, "chPickListImg");
            });
            chooserInfo.searchImage = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "IMG" && Dom.hasClass(e, "chSearchImg");
            });
            chooserInfo.menu = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "DIV" && Dom.hasClass(e, "awmenu");
            });
            chooserInfo.menuLinks = Menu.menuLinks(chooserInfo.menu.id);
            var searchLinkText = Dom.findChildUsingPredicate(chooserInfo.menu, function (e) {
                return e.tagName == "DIV" && Dom.hasClass(e, "chSearchLink");
            });
            chooserInfo.searchLink = searchLinkText.parentNode;
            chooserInfo.selectionContainer = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "SPAN" && Dom.hasClass(e, "chSelections");
            });
            chooserInfo.matchesContainer = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "SPAN" && Dom.hasClass(e, "chMatches");
            });
            chooserInfo.searchPattern = '';
            chooserInfo.validSelection = this.hasSelection(chooserInfo) && !isInvalid;
            chooserInfo.fullMatchCheckbox = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "INPUT" && Dom.hasClass(e, "chfullMatch");
            });

            chooserInfo.isInvalid = isInvalid;
            if (isInvalid) {
                Dom.addClass(chooserInfo.textField, 'chInvalidSelection');
            }
            else {
                this.checkChooserText(chooserInfo);
            }
            chooserInfo.skipBlur = false;
            this.chooserPickListMode(chooserInfo);
            chooserInfo.multiSelect = multiSelect;
            if (multiSelect) {
                chooserInfo.multiSelectTextRegion = Dom.findChildUsingPredicate(wrapper, function (e) {
                    return e.tagName == "TD" && Dom.hasClass(e, "chMultiSelected");
                });
                chooserInfo.addLink = Dom.findChildUsingPredicate(wrapper, function (e) {
                    return e.tagName == "IMG" && Dom.hasClass(e, "chAddLink");
                });
                chooserInfo.addOffImage = Dom.findChildUsingPredicate(wrapper, function (e) {
                    return e.tagName == "IMG" && Dom.hasClass(e, "chAddOffImg");
                });
                chooserInfo.addOnImage = Dom.findChildUsingPredicate(wrapper, function (e) {
                    return e.tagName == "IMG" && Dom.hasClass(e, "chAddOnImg");
                });
                chooserInfo.addModeCheckbox = Dom.findChildUsingPredicate(wrapper, function (e) {
                    return e.tagName == "INPUT" && Dom.hasClass(e, "chAddMode");
                });
                chooserInfo.recentSelections = Dom.findChildrenUsingPredicate(wrapper, function (e) {
                    return e.tagName == "TR" && Dom.hasClass(e, "chMultiRow");
                });
                chooserInfo.moreSelectedRow = Dom.findChildUsingPredicate(wrapper, function (e) {
                    return e.tagName == "TR" && Dom.hasClass(e, "chMoreSelectedRow");
                });
                if (chooserInfo.moreSelectedRow) {
                    chooserInfo.moreSelected = Dom.findChildUsingPredicate(chooserInfo.moreSelectedRow, function (e) {
                        return e.tagName == "SPAN" && Dom.hasClass(e, "chMoreSelected");
                    });
                    chooserInfo.moreSelectedPlusOne = Dom.findChildUsingPredicate(chooserInfo.moreSelectedRow, function (e) {
                        return e.tagName == "SPAN" && Dom.hasClass(e, "chMoreSelectedPlusOne");
                    });
                    chooserInfo.moreSelectedPlusTwo = Dom.findChildUsingPredicate(chooserInfo.moreSelectedRow, function (e) {
                        return e.tagName == "SPAN" && Dom.hasClass(e, "chMoreSelectedPlusTwo");
                    });
                    if (chooserInfo.moreSelected.innerHTML == '') {
                        chooserInfo.moreSelectedRow.style.display = 'none';
                    }
                    chooserInfo.moreSelectedRow.style.height = chooserInfo.recentSelections[1].offsetHeight + 'px';
                }
                chooserInfo.maxRecentSelection = parseInt(wrapper.getAttribute('_mrs'));
                this.chooserAddMode(chooserInfo);
            }

            chooserInfo.textTimeoutId = null;
            chooserInfo.keyDownTimeoutId = null;
        },

        hasSelection : function (chooserInfo)
        {
            var v = chooserInfo.textField.value;
            return v !='' && v != chooserInfo.noSelectionValue;
        },

        initiallyHadSelection : function (chooserInfo)
        {
            var v = chooserInfo.initValue;
            return v != '' && v != chooserInfo.noSelectionValue;
        },

        getChooserInfo : function (elm)
        {
            var wrapper = Dom.findParentUsingPredicate(elm, function (e) {
                return e.tagName == "SPAN" && Dom.hasClass(e, "chWrapper");
            }, true);
            return AWChooserInfo[wrapper.id];
        },

        chooserSetFocus : function (chooserInfo, noSelect) {
            var chooserFocus = function () {
                chooserInfo.textField.focus();
                if (!noSelect) {
                    chooserInfo.textField.select();
                }
            }
            setTimeout(chooserFocus.bind(this), 1);
        },

        chooserMenuTrigger : function (evt)
        {
            var menuCellDivLink = Event.eventSourceElement(evt);
            var menu = Menu.menu(menuCellDivLink);
            var selectionIndex = this.selectionIndex(menuCellDivLink);
            var menuLinks = Menu.menuLinks(menu.id);
            for (var i = 0; i < menuLinks.length; i++) {
                if (menuLinks[i] == menuCellDivLink) {
                    selectionIndex.value = i;
                    break;
                }
            }
            var selectionListName = this.selectionListName(menuCellDivLink);
            var selectionList = this.selectionList(menuCellDivLink);
            selectionList.value = selectionListName;
        },

        chooserRemoveClick : function (evt)
        {
            var sourceElm = Event.eventSourceElement(evt);
            var container = Dom.findParentUsingPredicate(sourceElm, function(n) {
                return (n.tagName == "SPAN" && Dom.hasClass(n, "chSelectionContainer"))
            });
            var removeCheckbox = Dom.findChildUsingPredicate(container, function(n) {
                return (n.tagName == "INPUT" && Dom.hasClass(n, "chRemove"))
            });
            removeCheckbox.checked = true;
        },

        selectionIndex : function (menuCellDivLink)
        {
            var container = Dom.findParentUsingPredicate(menuCellDivLink, function(n) {
                return (n.tagName == "SPAN" && Dom.hasClass(n, "chSelectionContainer"))
            });
            return  Dom.findChildUsingPredicate(container, function(n) {
                return (n.tagName == "INPUT" && Dom.hasClass(n, "chSelectIndex"))
            });
        },

        selectionListName : function (menuCellDivLink)
        {
            var selectionList = Dom.findParentUsingPredicate(menuCellDivLink, function(n) {
                return (n.tagName == "SPAN" && Dom.hasClass(n, "chList"))
            });
            return selectionList.getAttribute("chlname");
        },

        selectionList : function (menuCellDivLink)
        {
            var container = Dom.findParentUsingPredicate(menuCellDivLink, function(n) {
                return (n.tagName == "SPAN" && Dom.hasClass(n, "chSelectionContainer"))
            });
            var selectionList = Dom.findChildUsingPredicate(container, function(n) {
                return (n.tagName == "INPUT" && Dom.hasClass(n, "chSelectList"))
            });
            return selectionList;
        },

        setAddMode : function (chooserInfo, on)
        {
            if (chooserInfo.addModeCheckbox) {
                chooserInfo.addModeCheckbox.checked = on;
            }
        },

        addMode : function (chooserInfo)
        {
            if (chooserInfo.addModeCheckbox) {
                return chooserInfo.addModeCheckbox.checked;
            }
            return false;
        },

        checkChooserText : function (chooserInfo)
        {
            if (!chooserInfo.validSelection) {
                if (this.initiallyHadSelection(chooserInfo)) {
                    if (chooserInfo.multiSelect) {
                        chooserInfo.validSelection = true;
                        chooserInfo.textField.value = chooserInfo.initValue;
                        this.setAddMode(chooserInfo, false);
                        this.chooserAddMode(chooserInfo);
                    }
                }
                else {
                    Dom.addClass(chooserInfo.textField, 'chNoSelection');
                    if (chooserInfo.textField.value != chooserInfo.noSelectionValue)
                        chooserInfo.textField.value = chooserInfo.noSelectionValue;
                }
                this.chooserPickListMode(chooserInfo);
            }

            if (chooserInfo.validSelection) {
                Dom.removeClass(chooserInfo.textField, 'chNoSelection')
                Dom.addClass(chooserInfo.textField, 'chValidSelection');
            }

            return false;
        },

        chooserPickListMode : function (chooserInfo)
        {
            chooserInfo.mode = AWChooserPickListMode;
            chooserInfo.matchesContainer.innerHTML = '';
            chooserInfo.modeLink.src = chooserInfo.pickListImage.src;
            chooserInfo.modeLink.title = chooserInfo.pickListImage.title;
            chooserInfo.selectionContainer.style.display = '';
            chooserInfo.searchPattern = '';
            this.chooserPickListLinks(chooserInfo, "0");
        },

        chooserAutoCompleteMode : function (chooserInfo)
        {
            chooserInfo.mode = AWChooserAutoCompleteMode;
            chooserInfo.modeLink.src = chooserInfo.searchImage.src;
            chooserInfo.modeLink.title = chooserInfo.searchImage.title;
            chooserInfo.selectionContainer.style.display = 'none';
            this.chooserPickListLinks(chooserInfo, "1");
            chooserInfo.validSelection = false;
            Dom.removeClass(chooserInfo.textField, 'chValidSelection');
        },

        chooserHideShow : function (elmToHide, elmToShow)
        {
            if (elmToHide) {
                elmToHide.style.display = 'none';
            }
            if (elmToShow) {
                elmToShow.style.display = '';
            }
            // Notify content resize (so DataTables can expand/contract)
            Event.forceOnWindowResize();
        },

        previousRow : function (tr)
        {
            return tr.previousSibling.tagName == 'TR' ? tr.previousSibling : tr.previousSibling.previousSibling;
        },

        chooserHideShowSelected : function (selectedToHide, selectedToShow)
        {
            var prevRow = null;
            if (selectedToHide) {
                selectedToHide.style.display = 'none';
                prevRow = this.previousRow(selectedToHide);
                prevRow.style.display = 'none';
            }
            if (selectedToShow) {
                selectedToShow.style.display = '';
                prevRow = this.previousRow(selectedToShow);
                prevRow.style.display = '';
            }
            // Notify content resize (so DataTables can expand/contract)
            Event.forceOnWindowResize();
        },

        chooserAddMode : function (chooserInfo)
        {
            if (!chooserInfo.multiSelect) {
                return;
            }
            var addLink = chooserInfo.addLink;
            var recentSelections = chooserInfo.recentSelections;
            var recentSelection = recentSelections[0];
            var lastRecentSelection = null;
            var secondLastRecentSelection = null;
            var maxJustReached = recentSelections.length == chooserInfo.maxRecentSelection &&
                                 chooserInfo.moreSelected.innerHTML == '';
            var maxReached = recentSelections.length == chooserInfo.maxRecentSelection - 1 &&
                             chooserInfo.moreSelected != null;
            if (maxReached || maxJustReached) {
                lastRecentSelection = recentSelections[recentSelections.length - 1];
            }
            if (maxJustReached) {
                secondLastRecentSelection = recentSelections[recentSelections.length - 2];
            }
            if (chooserInfo.validSelection) {
                addLink.src = chooserInfo.addOnImage.src;
                addLink.title = chooserInfo.addOnImage.title;
                addLink.parentNode.setAttribute('href', '#');
                this.chooserHideShowSelected(recentSelection, lastRecentSelection);
                if (maxReached) {
                    this.chooserHideShow(chooserInfo.moreSelectedPlusOne, chooserInfo.moreSelected);
                }
                if (maxJustReached) {
                    this.chooserHideShow(chooserInfo.moreSelectedPlusTwo, secondLastRecentSelection);
                    chooserInfo.moreSelectedRow.style.display = 'none';
                }

            }
            else {
                addLink.src = chooserInfo.addOffImage.src;
                addLink.title = chooserInfo.addOffImage.title;
                addLink.parentNode.removeAttribute('href');
                this.chooserHideShowSelected(lastRecentSelection, recentSelection);
                if (maxReached) {
                    this.chooserHideShow(chooserInfo.moreSelected, chooserInfo.moreSelectedPlusOne);
                }
                if (maxJustReached) {
                    this.chooserHideShow(secondLastRecentSelection, chooserInfo.moreSelectedPlusTwo);
                    chooserInfo.moreSelectedRow.style.display = '';
                }
            }
        },

        chooserPickListLinks : function (chooserInfo, skip)
        {
            for (var i = 0; i < chooserInfo.menuLinks.length - 1; i++) {
                chooserInfo.menuLinks[i].setAttribute("skipLink", skip);
            }
        },

        displayChooserMenu : function (chooserInfo)
        {
            if (!Menu.AWActiveMenu || Menu.AWActiveMenu != chooserInfo.menu) {
                Menu.menuLinkOnClick(chooserInfo.menuPositionObj, chooserInfo.menu.id, null, null);
            }
        },

        chooserFocus : function (elm, evt)
        {
            var chooserInfo = this.getChooserInfo(elm);
            if (!chooserInfo) {
                return;
            }
            this.cancelChooserBlurTimeout(chooserInfo);
            var sourceElm = Event.eventSourceElement(evt);
            Debug.log('focus ' + sourceElm.tagName + ' ' + sourceElm.id + ' ' + elm.tagName, 5);
            if (chooserInfo.mode == AWChooserPickListMode &&
                sourceElm == chooserInfo.textField) {
                if (!this.hasSelection(chooserInfo)) {
                    Dom.removeClass(chooserInfo.textField, 'chNoSelection');
                    chooserInfo.textField.value = '';
                    chooserInfo.searchPattern = '';
                }
                // chooserInfo.textField.select();
            }
            return false;
        },

        isValidChooser : function (chooserInfo)
        {
            return chooserInfo.textField.parentNode;
        },

        chooserBlur : function (elm, evt)
        {
            var chooserInfo = this.getChooserInfo(elm);
            var sourceElm = Event.eventSourceElement(evt);
            var keyCode = Event.keyCode(evt);

            Debug.log('blur ' + sourceElm.tagName + ' ' + sourceElm.id + ' ' + elm.tagName + ' ' + chooserInfo.skipBlur, 5);
            Debug.log('blur ' + this.addMode(chooserInfo), 5);

            if (!chooserInfo.skipBlur) {
                Menu.hideActiveMenu();
                var chooserBlur = function () {
                    if (this.isValidChooser(chooserInfo)) {
                        if (!chooserInfo.multiSelect || !this.addMode(chooserInfo)) {
                            if (((this.hasSelection(chooserInfo) || this.initiallyHadSelection(chooserInfo))
                                    && (chooserInfo.textField.value != chooserInfo.initValue)) ||
                                        chooserInfo.isInvalid) {
                                this.initChooserFullMatch(chooserInfo);
                            }
                            else {
                                this.checkChooserText(chooserInfo);
                            }
                        }
                        else {
                            // multi && added
                            if (!this.hasSelection(chooserInfo) && !chooserInfo.isInvalid) {
                                this.checkChooserText(chooserInfo);
                            }
                            else {
                                this.initChooserFullMatch(chooserInfo);
                            }
                        }
                    }
                }

                this.cancelChooserBlurTimeout(chooserInfo);
                chooserInfo.textTimeoutId = setTimeout(chooserBlur.bind(this), 200);
            }
            chooserInfo.skipBlur = false;

            return true;
        },

        cancelChooserBlurTimeout : function (chooserInfo)
        {
            if (chooserInfo.textTimeoutId) {
                clearTimeout(chooserInfo.textTimeoutId);
                chooserInfo.textTimeoutId = null;
            }
        },

        cancelChooserFetchTimeout : function (chooserInfo)
        {
            if (chooserInfo.keyDownTimeoutId) {
                clearTimeout(chooserInfo.keyDownTimeoutId);
                chooserInfo.keyDownTimeoutId = null;
            }
        },

        chooserSearch : function (chooserInfo, evt)
        {
            var sourceElm = Event.eventSourceElement(evt);
            if (sourceElm == chooserInfo.textField ||
                sourceElm == chooserInfo.modeLink ||
                sourceElm == chooserInfo.modeLink.parentNode) {
                Event.elementInvoke(chooserInfo.searchLink, 'mousedown');
                Event.cancelBubble(evt);
            }
            return false;
        },

        chooserClick : function (elm, evt)
        {
            var chooserInfo = this.getChooserInfo(elm);
            var sourceElm = Event.eventSourceElement(evt);
            this.cancelChooserBlurTimeout(chooserInfo);
            this.cancelChooserFetchTimeout(chooserInfo);
            if (sourceElm == chooserInfo.modeLink) {
                if (chooserInfo.mode == AWChooserAutoCompleteMode) {
                    this.chooserSearch(chooserInfo, evt);
                }
                else {
                    this.displayChooserMenu(chooserInfo);
                }
            }
            else if (sourceElm == chooserInfo.addLink) {
                if (chooserInfo.validSelection) {
                    chooserInfo.validSelection = false;
                    this.chooserAddMode(chooserInfo);
                    chooserInfo.textField.value = '';
                    this.chooserSetFocus(chooserInfo);
                    chooserInfo.validSelection = false;
                    this.setAddMode(chooserInfo, true);
                }
            }
            else {
                chooserInfo.textField.select();
            }
            return false;
        },

        chooserKeyDown : function (elm, event)
        {
            var chooserInfo = this.getChooserInfo(elm);
            var keyCode = Event.keyCode(event);
            var sourceElm = Event.eventSourceElement(event);
            this.cancelChooserFetchTimeout(chooserInfo);

            chooserInfo.skipBlur = keyCode == 38 || keyCode == 40 || keyCode == 13;

            if (keyCode == 9) {
                // tab
                return true;
            }

            if (keyCode == 13) {
                if (sourceElm == chooserInfo.textField &&
                    elm == chooserInfo.textField) {
                    return this.chooserSearch(chooserInfo, event);
                }
                else if (sourceElm == chooserInfo.modeLink ||
                         sourceElm == chooserInfo.modeLink.parentNode) {
                    return this.displayChooserMenu(chooserInfo);
                }
                else if (chooserInfo.addLink) {
                    if (sourceElm == chooserInfo.addLink ||
                        sourceElm == chooserInfo.addLink.parentNode) {
                        if (chooserInfo.validSelection) {
                            chooserInfo.validSelection = false;
                            this.chooserAddMode(chooserInfo);
                            chooserInfo.textField.value = '';
                            this.chooserSetFocus(chooserInfo);
                            chooserInfo.validSelection = false;
                            this.setAddMode(chooserInfo, true);
                        }
                    }
                }
            }
            else if (keyCode == 27) {
                // escape
                if (sourceElm == chooserInfo.textField &&
                    elm == chooserInfo.textField) {
                    Menu.hideActiveMenu();
                    if (this.initiallyHadSelection(chooserInfo)) {
                        this.chooserSetFocus(chooserInfo);
                        this.chooserPickListMode(chooserInfo);
                        chooserInfo.validSelection = false;
                        this.checkChooserText(chooserInfo);
                    }
                    return false;
                }
                else {
                    if (chooserInfo.mode == AWChooserPickListMode) {
                        Menu.hideActiveMenu();
                    }
                    this.chooserSetFocus(chooserInfo, true);
                }
                return false;
            }
            else {
                if (chooserInfo.mode == AWChooserPickListMode) {
                    // up arrow
                    if (keyCode == 38) {
                        this.displayChooserMenu(chooserInfo);
                        var lastMenuLink = Menu.lastMenuLink(chooserInfo.menu.id);
                        lastMenuLink.focus();
                    }
                        // down arrow
                    else if (keyCode == 40) {
                        this.displayChooserMenu(chooserInfo);
                        var firstMenuLink = Menu.firstMenuLink(chooserInfo.menu.id);
                        firstMenuLink.focus();
                    }
                }
                else {
                    // up arrow
                    if (keyCode == 38) {
                        if (Menu.AWActiveMenu && Menu.AWActiveMenu == chooserInfo.menu) {
                            var lastMenuLink = Menu.lastMenuLink(chooserInfo.menu.id);
                            lastMenuLink.focus();
                        }
                    }
                        // down arrow
                    else if (keyCode == 40) {
                        if (Menu.AWActiveMenu && Menu.AWActiveMenu == chooserInfo.menu) {
                            var firstMenuLink = Menu.firstMenuLink(chooserInfo.menu.id);
                            firstMenuLink.focus();
                        }
                    }
                }

                function processChooseKeyDown()
                {
                    var textValue = chooserInfo.textField.value;
                    Dom.removeClass(chooserInfo.textField, 'chInvalidSelection');
                    if (chooserInfo.mode == AWChooserPickListMode) {
                        if (textValue != '') {
                            if (chooserInfo.isInvalid ||
                                chooserInfo.initValue != textValue &&
                                textValue != chooserInfo.noSelectionValue) {
                                this.chooserAutoCompleteMode(chooserInfo);
                            }
                        }
                    }
                    if (chooserInfo.mode == AWChooserAutoCompleteMode) {
                        if (textValue.length == 0) {
                            Menu.hideActiveMenu();
                            chooserInfo.searchPattern = textValue;
                            this.chooserPickListMode(chooserInfo);
                        }
                        else if (textValue == chooserInfo.searchPattern) {
                            return true;
                        }
                        else {
                            this.cancelChooserFetchTimeout(chooserInfo);
                            chooserInfo.keyDownTimeoutId = this.initChooserFetchList(chooserInfo, 1);
                        }
                    }
                    return false;
                }

                if (sourceElm == chooserInfo.textField &&
                    elm == chooserInfo.textField) {
                    setTimeout(processChooseKeyDown.bind(this), 1);
                }
            }
        },

        initChooserFetchList : function (chooserInfo, delay)
        {
            var searchPattern = chooserInfo.textField.value;

            var chooserFetchListCallback = function (xmlHttp)
            {
                //debug(xmlHttp.responseText);
                if (searchPattern == chooserInfo.textField.value &&
                    xmlHttp.responseText.indexOf('chooser match') > -1) {
                    chooserInfo.matchesContainer.innerHTML = xmlHttp.responseText;
                    this.displayChooserMenu(chooserInfo);
                    var menuLinks = Menu.menuLinks(chooserInfo.menu.id);
                    if (menuLinks.length >= 2) {
                        Menu.cellMouseOver(menuLinks[0]);
                    }
                }
            }

            var chooserFetchList = function ()
            {
                chooserInfo.searchPattern = searchPattern;
                var urlString = Request.formatSenderUrl(chooserInfo.wrapper.id);
            // todo: need to url encode the searchPattern
                urlString = urlString + "&chsp=" + encodeURIComponent(searchPattern);
                if (this.addMode(chooserInfo)) {
                    urlString = urlString + "&chadd=1";
                }
                Request.initiateXMLHttpRequest(urlString, chooserFetchListCallback.bind(this));
            }

            return setTimeout(chooserFetchList.bind(this), delay);
        },

        initChooserFullMatch : function (chooserInfo)
        {
            var searchPattern = chooserInfo.textField.value;

            var chooserFullMatchCallback = function (xmlHttp)
            {
                var iframe = Request.createRequestIFrame('AWRefreshFrame');
                var iframeDoc = iframe.document;
                iframeDoc.open();
                iframeDoc.write(xmlHttp.responseText);
                iframeDoc.close();
            }

            var chooserFullMatch = function ()
            {
                chooserInfo.searchPattern = searchPattern;

                /*
                var urlString = Request.formatSenderUrl(chooserInfo.fullMatchActionId);
                urlString = urlString + "&chsp=" + searchPattern;
                urlString = urlString + "&chadd=" + chooserInfo.added;
                urlString = Request.appendFrameName(urlString);
                Request.getContent(urlString, true);
                */

                var formObject = chooserInfo.textField.form;
                chooserInfo.fullMatchCheckbox.checked = true;
                Dom.addFormField(formObject, 'chsp', searchPattern);
                Request.submitForm(formObject, null, null, true);

            //Request.initiateXMLHttpRequest(urlString, chooserFullMatchCallback);
            }

            return setTimeout(chooserFullMatch.bind(this), 1);
        },

        chooserMouseOver : function (elm, evt)
        {
            var chooserInfo = this.getChooserInfo(elm);
            this.cancelChooserFetchTimeout(chooserInfo);
            var removeImgs = Dom.findChildrenUsingPredicate(elm, function (e) {
                return e.tagName == "IMG";
            });
            removeImgs[0].src = removeImgs[2].src;
            removeImgs[0].title = removeImgs[2].title;
        },

        chooserMouseOut : function (elm, evt)
        {
            var removeImgs = Dom.findChildrenUsingPredicate(elm, function (e) {
                return e.tagName == "IMG";
            });
            removeImgs[0].src = removeImgs[1].src;
            removeImgs[0].title = removeImgs[1].title;
        },
        EOF:0};

    // Behaviors
    ariba.Event.registerBehaviors({
        // CH - Chooser
        CH : {
            click : function  (elm, evt) {
                return Chooser.chooserClick(elm, evt);
            },

            keydown : function  (elm, evt) {
                return Chooser.chooserKeyDown(elm, evt);
            }
        },

        CHM : {
            mouseover : function  (elm, evt) {
                return Chooser.chooserMouseOver(elm, evt);
            },

            mouseout : function  (elm, evt) {
                return Chooser.chooserMouseOut(elm, evt);
            },

            focus : function  (elm, evt) {
                return Chooser.chooserFocus(elm, evt);
            }
        },

        CHR : {
            click : function (elm, evt) {
                Chooser.chooserRemoveClick(evt);
                return Event.behaviors.GAT.click(elm, evt);
            }
        }
    });

    return Chooser;
}();
