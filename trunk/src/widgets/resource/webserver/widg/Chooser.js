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
    var Input = ariba.Input;

    // private vars
    var AWChooserInfo = new Object();
    var AWChooserPickListMode = new Object();
    var AWChooserAutoCompleteMode = new Object();

    var Chooser = {

        initChooser : function (chooserId, multiSelect, focus, isInvalid, itemCountChanged, basic)
        {
            var chooserInfo = new Object();
            AWChooserInfo[chooserId] = chooserInfo;
            chooserInfo.chooserId = chooserId;
            chooserInfo.isInvalid = isInvalid;
            chooserInfo.searchPattern = '';
            chooserInfo.skipBlur = false;
            chooserInfo.multiSelect = multiSelect;
            chooserInfo.textTimeoutId = null;
            chooserInfo.keyDownTimeoutId = null;
            chooserInfo.basic = basic;

            chooserInfo.initialized = false;

            if(itemCountChanged) {
                Event.forceOnWindowResize();
            }
        },

        fullInit: function(chooserInfo)
        {
            var wrapper = Dom.getElementById(chooserInfo.chooserId);
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
            chooserInfo.menuLinks = Menu.menuItems(chooserInfo.menu);
            if (!chooserInfo.basic) {
                var searchLinkText = Dom.findChildUsingPredicate(chooserInfo.menu, function (e) {
                    return e.tagName == "DIV" && Dom.hasClass(e, "chSearchLink");
                });
                chooserInfo.searchLink = searchLinkText.parentNode;
                chooserInfo.validSelection = this.hasSelection(chooserInfo) && !chooserInfo.isInvalid;
            }
            chooserInfo.selectionContainer = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "SPAN" && Dom.hasClass(e, "chSelections");
            });
            chooserInfo.matchesContainer = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "SPAN" && Dom.hasClass(e, "chMatches");
            });

            chooserInfo.fullMatchCheckbox = Dom.findChildUsingPredicate(wrapper, function (e) {
                return e.tagName == "INPUT" && Dom.hasClass(e, "chfullMatch");
            });

            this.chooserPickListMode(chooserInfo);

            if (chooserInfo.multiSelect) {
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
            chooserInfo.initialized = true;
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

        chooserMenuTrigger : function (elm, evt)
        {
            var menu = Menu.menu(elm);
            var selectionIndex = this.selectionIndex(elm);

            var menuItems = Menu.menuItems(menu);
            for (var i = 0; i < menuItems.length; i++) {
                if (menuItems[i] == elm) {
                    selectionIndex.value = i;
                    break;
                }
            }
            var selectionListName = this.selectionListName(elm);
            var selectionList = this.selectionList(elm);
            selectionList.value = selectionListName;
            var keyCode = Event.keyCode(evt);
            if (keyCode == Input.KeyCodeTab) {
                var formId = Dom.boolAttr(elm, "_sf", true) ? Dom.lookupFormId(elm) : null;
                Menu.menuClicked(elm, elm.id, formId);
            }
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
                Dom.removeClass(chooserInfo.textField, 'chNoSelection');
                Dom.addClass(chooserInfo.textField, 'chValidSelection');
            }

            return false;
        },

        chooserPickListMode : function (chooserInfo)
        {
            chooserInfo.mode = AWChooserPickListMode;
            chooserInfo.matchesContainer.innerHTML = '';
            if (!chooserInfo.basic) {
                chooserInfo.modeLink.src = chooserInfo.pickListImage.src;
                chooserInfo.modeLink.title = chooserInfo.pickListImage.title;
                chooserInfo.selectionContainer.style.display = '';
            }
            chooserInfo.searchPattern = '';
            this.chooserPickListLinks(chooserInfo, "0");
        },

        chooserAutoCompleteMode : function (chooserInfo)
        {
            chooserInfo.mode = AWChooserAutoCompleteMode;
            if (!chooserInfo.basic) {
                chooserInfo.modeLink.src = chooserInfo.searchImage.src;
                chooserInfo.modeLink.title = chooserInfo.searchImage.title;
            }
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
                Menu.menuLinkOnClick(chooserInfo.menuPositionObj, chooserInfo.menu.id, chooserInfo.textField.id, null, !chooserInfo.basic);
            }
        },

        chooserFocus : function (elm, evt)
        {
            var chooserInfo = this.getChooserInfo(elm);
            if (!chooserInfo) {
                return;
            }
            if (!chooserInfo.initialized) {
                this.fullInit(chooserInfo);
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
            if (!chooserInfo.initialized) {
                this.fullInit(chooserInfo);
            }
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
            Event.cancelBubble(evt);
            return false;
        },

        chooserKeyDown : function (elm, event)
        {
            var chooserInfo = this.getChooserInfo(elm);
            var keyCode = Event.keyCode(event);
            var sourceElm = Event.eventSourceElement(event);
            this.cancelChooserFetchTimeout(chooserInfo);

            if (sourceElm == chooserInfo.textField &&
                    elm == chooserInfo.textField) {
                chooserInfo.skipBlur = keyCode == Input.KeyCodeEnter;
                if (Menu.AWActiveMenu && Menu.AWLinkId == chooserInfo.textField.id) {
                    // forward key down to menu item
                    var activeMenuItem = Menu.getActiveItem();
                    var searchLinkId  = null;
                    if (!chooserInfo.basic) {
                        searchLinkId = chooserInfo.searchLink.id;
                    }
                    if (keyCode == Input.KeyCodeEnter && activeMenuItem &&
                            Menu.AWActiveItemId != searchLinkId) {
                        this.chooserMenuTrigger(activeMenuItem, event);
                        var formId = Dom.lookupFormId(chooserInfo.textField);
                        var senderId = chooserInfo.textField.id;
                        Request.submitFormForElementName(formId, senderId, event, null);
                        Menu.hideActiveMenu();
                    }
                    else {
                        Menu.menuKeyDown(event, chooserInfo.menu);
                        var menuItems = Menu.menuItems(chooserInfo.menu);
                        if (menuItems.length > 1) {
                            chooserInfo.skipBlur = keyCode == Input.KeyCodeTab;
                        }
                    }
                }
                Debug.log("skipBlur " + chooserInfo.skipBlur + " keycode " + keyCode);
            }
            if (!Event.shouldBubble(event)) {
                return false;
            }
            if (keyCode == Input.KeyCodeEnter) {
                if (sourceElm == chooserInfo.textField &&
                    elm == chooserInfo.textField) {
                    if (chooserInfo.basic) {
                        Event.cancelBubble(event);
                        Menu.hideActiveMenu();
                        var formId = Dom.lookupFormId(chooserInfo.textField);
                        var senderId = chooserInfo.textField.id;
                        Request.submitFormForElementName(formId, senderId, event, null);
                    }
                    else {
                        return this.chooserSearch(chooserInfo, event);
                    }
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
            else if (keyCode == Input.KeyCodeEscape) {
                if (sourceElm == chooserInfo.textField &&
                    elm == chooserInfo.textField) {
                    if (this.initiallyHadSelection(chooserInfo)) {
                        this.chooserPickListMode(chooserInfo);
                        chooserInfo.validSelection = false;
                        this.checkChooserText(chooserInfo);
                    }
                    return false;
                }
                return false;
            }
            else {
                if (chooserInfo.mode == AWChooserPickListMode) {
                    if (keyCode == Input.KeyCodeArrowDown) {
                        this.displayChooserMenu(chooserInfo);
                        Event.cancelBubble(event);
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
                    if (!Request.isRequestInProgress()) {
                        setTimeout(processChooseKeyDown.bind(this), 1);
                    }
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
                    var menuLinks = Menu.menuItems(chooserInfo.menu);
                    var displayChooserMenu = true;

                    var hiliteMenuItem = menuLinks.length >= 2;

                    if (chooserInfo.basic) {
                        displayChooserMenu = menuLinks.length > 0; 
                        hiliteMenuItem = false;
                    }
                    if (displayChooserMenu) {
                        this.displayChooserMenu(chooserInfo);
                    }
                    else {
                        Menu.hideActiveMenu();
                    }
                    if (hiliteMenuItem) {
                        Menu.hiliteMenuItem(menuLinks[0], chooserInfo.menu);
                    }
                }
            };

            var chooserFetchList = function ()
            {
                chooserInfo.searchPattern = searchPattern;
                var urlString = Request.formatInPageRequestUrl(chooserInfo.wrapper.id);
            // todo: need to url encode the searchPattern
                urlString = urlString + "&chsp=" + encodeURIComponent(searchPattern);
                if (this.addMode(chooserInfo)) {
                    urlString = urlString + "&chadd=1";
                }
                Request.initiateXMLHttpRequest(urlString, chooserFetchListCallback.bind(this));
            };

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
                if (Request.isRequestInProgress()) return;
                
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

    //
    // iPad - specific methods
    //
    if (Dom.isIPad) Util.extend(Chooser, function () {
        return {
            displayChooserMenu : function (chooserInfo)
            {
                Menu.menuLinkOnClick(chooserInfo.menuPositionObj, chooserInfo.menu.id, chooserInfo.textField.id, null, !chooserInfo.basic);
            },

        EOF:0};
    }());

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
