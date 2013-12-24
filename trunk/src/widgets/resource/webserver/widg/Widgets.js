/*
    Widgets.js

    Support for various widgets and widget utility routines.
    Includes textbutton, hint message, dialog/confirmation panels
*/

ariba.Widgets = function() {
    // imports
    var Util = ariba.Util;
    var Dom = ariba.Dom;
    var Event = ariba.Event;
    var Input = ariba.Input;
    var Debug = ariba.Debug;
    var Refresh = ariba.Refresh;
    var Request = ariba.Request;
    var hoverCt = 0;

    // private vars
    var AWCover;
    var AWCover_cb;
    var AWPanelWrapper = null;
    var AWActiveDialogDiv = null;
    var _awMMDR = false;

    var awConfirmationId;
    var awConfirmationRegistered = false;

    var AWHintIdList;

    var AWMinNotificationTime = 5000;
    var AWMaxNotificationTime = 15000;
    var AWHideNotificationTimeout = null;

    var docWin = '';
    var IsMinimizedFieldName = 'PageErrorPanelIsMinimized';
    var AWPrintWindowName = "AWPrintPage";

    var AWHideBubbleTimeout = null;
    var CurrentErrorIndicatorId;

    var slideSpeed = 3;
    var slideTiming = 5;
    var AWSlideTimer = null;

    var AWFooter = null;

    var hcTimeout = null;

    var HoverCardContentBehavior = {
         mouseover : function (hoverLink, evt) {
             ariba.Widgets.clearHideHoverCard();
             hcTimeout = setTimeout(function() {ariba.Widgets.displayHoverCard(hoverLink);}, 500);
         },
         mouseout : function (hoverLink, evt) {
             if (hcTimeout) {
                 clearTimeout(hcTimeout);
                 hcTimeout = null;
                 return;
             }
             ariba.Widgets.hideActiveHoverCard();
         }
    };

    var Widgets = {
        HoverCardContentBehavior : HoverCardContentBehavior,

        showPanel : function (id, positioningObject, skipOverlay)
        {
            var divObject = Dom.getElementById(id);
            divObject.style.display = '';
            var newTop = Dom.absoluteTop(positioningObject) + (positioningObject.offsetHeight / 3);
            var newLeft = Dom.absoluteLeft(positioningObject) + (positioningObject.offsetWidth - divObject.offsetWidth) / 2;
            newTop = Math.max(3, Dom.correctForBottomEdge(newTop, divObject));
            newLeft = Math.max(3, Dom.correctForRightEdge(newLeft, divObject));
            Dom.setAbsolutePosition(divObject, newLeft, newTop);
            Refresh.displayDiv(divObject, '', false, skipOverlay);
        },

        hidePanel : function (id)
        {
            var divObject = Dom.getElementById(id);
            if (divObject) divObject.style.display = 'none';
            Dom.unoverlay(divObject);
        },

        notImplemented : function ()
        {
            alert("This feature is under construction and not currently available.");
            return false;
        },

        ///////////////
        // TextButton
        ///////////////
        btnMouseOver : function (divObject, styleName)
        {
            divObject.className = styleName;
        },

        btnMouseOut : function (divObject, styleName)
        {
            // if a request is in progress we defer changing the button style until the request is complete
            var cb = function() {
                if (Request.isRequestInProgress()) {
                    Event.registerUpdateCompleteCallback(function () {
                        if (Dom.elementInDom(divObject)) {
                            divObject.className = styleName;
                        }                        
                    }.bind(this));
                } else {
                    if (Dom.elementInDom(divObject)) {
                        divObject.className = styleName;
                    }
                }
            }.bind(this);
            setTimeout(cb, 1);
            return true;
        },

        applyFilterToImages : function (divObject, filterName)
        {
            var childImages = divObject.getElementsByTagName("IMG");
            for (var i = 0; i < childImages.length; i++) {
                childImages[i].style.filter = filterName;
            }
        },

        downloadContent : function (srcUrl)
        {
            Request.downloadContent(srcUrl);
        },

        copyFromDocument : function (sourceDocument, divId)
        {
            // This is only called from the iframe's document.
            var sourceDiv = sourceDocument.getElementById('AWLazyDivSource');
            var destDiv = Dom.getElementById(divId);
            if (sourceDiv == null) {
                //there is no menu item from server to show
                destDiv.innerHTML = "&nbsp;";
                destDiv.style.display = "none";
            } else {
                destDiv.innerHTML = sourceDiv.innerHTML;
                Refresh.postLoadLazyDiv();
            }
        },

        disablePage : function (opacity)
        {
            if (AWCover) return;
            AWCover = true;
            if (AWActiveDialogDiv) Input.registerModalDiv(AWActiveDialogDiv);
            if (Dom.IsIE6Only) Input.hideSelects(true);
        },

        enablePage : function ()
        {
            if (!AWCover) return;
            if (AWPanelWrapper) AWPanelWrapper.style.display = "none";
            if (Dom.IsIE6Only) Input.showSelects();
            if (AWActiveDialogDiv) Input.unregisterModalDiv(AWActiveDialogDiv);

            AWCover = false;
        },

        windowSizeDiv : function (div)
        {
            function size() {
                if (!Dom.elementInDom(div)) {
                    Event.unregisterOnWindowResize(size);
                    return;
                }
                var ws = Dom.getWindowSize();
                div.style.width = ws[0] - 2 + "px";
                div.style.height = ws[1] - 2 + "px";
            }
            Event.registerOnWindowResize(size);
            size();
        },

        openDialogBox : function (target)
        {
            target.style.visibility = "hidden";  // hide until repositioned (to avoid visible jerk on Firefox)
            target.style.display = (target.tagName == "TABLE" && !Dom.IsIE) ? "table" : "block";

            var panelWrapper = Dom.findParentUsingPredicate(target, function (e) {
                return Dom.hasClass(e, "panelContainer");
            });
            var container = Dom.getPageScrollElement();
            function position() {
                if (panelWrapper) {
                    // using width/height:100% in CSS doesn't work in IE6, so we have to do this
                    if (Dom.IsIE6Only) Widgets.windowSizeDiv(panelWrapper);

                    panelWrapper.style.left = container.scrollLeft + "px";
                    panelWrapper.style.top = container.scrollTop + "px";
                    panelWrapper.style.display = "block";
                    AWPanelWrapper = panelWrapper;
                } else {
                    Dom.positionDialogBox(target);
                }
            }
            position();

        // double-check that window doesn't scroll before initial display
            var origScroll = container.scrollTop;
            Event.eventEnqueue(function() {
                if (container.scrollTop != origScroll) {
                    position();
                }
            });
        },
        // called by datatable to tell us of unmet vertical space desires
        panelRegChildWants : function (parent, height)
        {
            if (parent && height > 0) {
                var lastHtStr = parent.getAttribute("childrenWant");
                if (!lastHtStr || height > parseInt(lastHtStr)) {
                    parent.setAttribute("childrenWant", height);
                }
            }
        },

        panelMaxWidth : function (panel, elm)
        {
            var panelInset = parseInt(Dom.effectiveStyle(panel, "padding-left"));
            var inner = Dom.findChildUsingPredicate(panel, function (e) {
                return e.tagName == "TABLE" && e.className == "dialogWrapper";
            });
            if (inner) {
                var outer = Dom.findParentUsingPredicate(inner, function (e) {
                    return e.className == "panel";
                });
                if (outer) panelInset += Dom.absoluteLeft(inner) - Dom.absoluteLeft(outer)
                if (elm) panelInset += Dom.minInsetWidth(inner, elm);
            }
            // -20 is fudge factor for possible scrollbar
            return Dom.getWindowSize()[0] - panelInset * 2 - 20;
        },

        panelMaxHt : function (panel)
        {
            var ws = Dom.getWindowSize();
            return ws[1] - 2;
        },

        checkPanelHeight : function (target)
        {
            // Panels will overflow if they need more height (although datatables will try to fit)
            // We see how long our content is (and if our children want more height) and adjust the
            // top position to pseudo-center the content
            var panelWrapper = Dom.findParentUsingPredicate(target, function (e) {
                return Dom.hasClass(e, "panelContainer");
            });
            var table = Dom.findChildUsingPredicate(target, function (e) {
                return e.tagName == "TABLE";
            });
            if (!table || !panelWrapper) {
                Dom.positionDialogBox(target);
                return;
            }
            var current = table.offsetHeight;
            var curTop = parseInt(target.style.marginTop) || 0;
            var pad = target.offsetHeight - current - curTop;
            var ws = Dom.getWindowSize();
            var max = ws[1] - pad
            var childrenWant = parseInt(panelWrapper.getAttribute("childrenWant")) || 0;
            var want = current + childrenWant;
            want = Math.min(want, max);
            var newTop = Math.floor((max - want) * 0.4);  // 40% above, 60% below
            // only move up (larger), not back down
            if (newTop < curTop || !target.style.marginTop) {
                target.style.marginTop = newTop + "px";
                if (childrenWant) Event.forceOnWindowResize(); // we moved; let tables use extra space
            }
            panelWrapper.removeAttribute("childrenWant");
        },

        showDialogDiv : function (target, preOpenFunction, documentClickFunction, preSize)
        {
            if (target == null) {
                return;
            }
        // displays the dialog and positions it in the middle of the browser window
            // optional documentClickFunction can be registered which will get called
            // on any click

            // if there was a previous dialog div, then call its closer first
            if (AWActiveDialogDiv && AWActiveDialogDiv.awCloseDialogFunc) {
                var modal = AWActiveDialogDiv.awCloseDialogFunc();
                if (modal) {
                    // modal so don't let other div's open
                    return;
                }
            }

            AWActiveDialogDiv = target;
            if (preOpenFunction) {
                preOpenFunction();
            }


            /* Approach
                - rely on browser layout (CSS, etc) to center panels, and generally to manage width
                - rely on datatable sizing to handle height adjustments (and some "minWidth" struting)
                --> We're just responsible for vertical centering
            */
            this.openDialogBox(target);

        // Register to update position if things change
            var r2 = null;
            var resize = function () {
                if (!Dom.elementInDom(target)) {
                    Event.unregisterOnWindowFixed(target);
                    if (r2) Event.unregisterRefreshCallback(r2);
                    return;
                }
                // vertical positioning -- check if our child needs more space (and we're not already positioned at top)
                Widgets.checkPanelHeight(target);
            };
            Event.registerOnWindowFixed(resize.bind(this));  // run last

            // enqueue check of height on refresh
            r2 = function () {
                Event.eventEnqueue(resize, null, true);
            };
            Event.registerRefreshCallback(r2);

        // initial positioning and panel reveal
            resize();
            target.style.visibility = "visible";

            this.macMozScrollCheck();

            if (documentClickFunction) {
                Event.enableDocumentClick(documentClickFunction);
            }

        // Make sure our callback is registered (but only once)
            Event.unregisterRefreshCallback(this.checkDialogStillPresent);
            Event.registerRefreshCallback(this.checkDialogStillPresent);

            return false;
        },


        /* Mac Mozilla Scrollbar bleed through fix -- see https://bugzilla.mozilla.org/show_bug.cgi?id=187435
          To force a redraw of our div over the (native) scrollbar we do a *late* set of overflow to auto.
          We also redo it on every window blur/focus transition.
        */
        macMozScrollCheck : function ()
        {
            // Only apply to Firefox 2.x (Firefox  3.0+ doesn't have the problem)
            if (!navigator.userAgent.match(/Mozilla.+Macintosh.+Firefox\/2.+/)) return;

            var didBlur = true;
            function fix() {
                if (AWPanelWrapper && didBlur) {
                    AWPanelWrapper.style.overflow = "hidden";
                    setTimeout(function() {
                        if (AWPanelWrapper) AWPanelWrapper.style.overflow = "auto";
                    }, 0);
                    didBlur = false;
                }
            }
            if (!_awMMDR) {
                _awMMDR = true;
                Event.registerHandler("MacId1", "onfocusin", fix);
            // Ick! When mac window is pushed to background the scrollbars will peek through.  But, if we
                // enable this blur handler it will interfere with control focus when clicking between fields...
                // Event.registerHandler ("MacId2", "onblur", function() { didBlur = true; fix(); didBlur=true; });
            }
            Event.eventEnqueue(function() {
                if (AWPanelWrapper) AWPanelWrapper.style.overflow = "auto"
            }, null, true);
        },

        // clean up our dialog (and cover div) if a refresh occurs that removed us from the DOM
        // (e.g. cross page incremental refresh to another page)
        checkDialogStillPresent : function () {
            if (AWActiveDialogDiv && !Dom.elementInDom(AWActiveDialogDiv)) {
                Widgets.clearDialog();
            }
        },

        clearDialog : function () {
            if (AWActiveDialogDiv && AWActiveDialogDiv.awCloseDialogFunc) {
                AWActiveDialogDiv.awCloseDialogFunc();
            }
        },

        hideDialogDiv : function ()
        {
            if (AWActiveDialogDiv) {
                AWActiveDialogDiv.setAttribute("_cfOpen", 0);
                Event.disableDocumentClick();
                if (AWActiveDialogDiv.awPreCloseDialogFunc) {
                    AWActiveDialogDiv.awPreCloseDialogFunc();
                }
                Refresh.undisplayDiv(AWActiveDialogDiv);
                AWActiveDialogDiv = null;
            }
            return false;
        },

        updateDialogWrapperClass : function (showAsDialg)
        {
            if (showAsDialg) {
                Dom.addClass(document.body, "dialogContentWrapper");
            }
            else {
                Dom.removeClass(document.body, "dialogContentWrapper");
            }
        },

        // Overridden by Menu.js
        hideActiveMenu : function ()
        {

        },

        showConfirmation : function (confirmationId, ss)
        {
            //debug('show confirmation');
            this.hideActiveMenu();
            awConfirmationId = confirmationId;
            var div = Dom.getElementById(confirmationId);
            // rehide selects that has been incremental updated
            Input.hideSelects(true);
            // if a refresh messed with our div, this flag will clear and we'll re-open
            if (!Dom.boolAttr(div, "_cfOpen", false)) {
                div.setAttribute("_cfOpen", "true");
                var f = function () {
                    this.disablePage("000");
                    Input.coverDocument(50, 50);
                }.bind(this);
                // set up the preclose function
                div.awPreCloseDialogFunc = this.enablePage.bind(this);
                div.awCloseDialogFunc = this.cancelConfirmation.bind(this);
                var panelWrapper =
                        Dom.findParentUsingPredicate(div, function (e) {
                            return Dom.hasClass(e, "panelContainer");
                        });
                Dom.relocateDiv(panelWrapper, true);
                this.showDialogDiv(div, f, null, true);
                if (!ss) {
                    // for client side confirmations, load lazy divs
                    Refresh.loadLazyChildren(div, this.confirmationLoadLazyDivCallback)
                    if (!awConfirmationRegistered) {
                        Event.registerRefreshCallback(this.checkConfirmationDisplay.bind(this));
                        awConfirmationRegistered = true;
                    }
                }
            }
        },

        checkConfirmationDisplay : function ()
        {
            // if there's a confirmationid then redisplay
            if (awConfirmationId != null) {
                this.showConfirmation(awConfirmationId, true);
            }
        },

        confirmationLoadLazyDivCallback : function (divObject, xmlhttp)
        {
            // copy content into the proper location
            divObject.innerHTML = xmlhttp.responseText;
            // evaluate all inline scripts
            Request.evalScriptTags(xmlhttp.responseText);
            // indicate that update is complete
            //Refresh.refreshComplete();

            Refresh.markDivLoadingDone(divObject);
            Input.hideWaitCursor();
            if (Input.AWAutomationTestModeEnabled) {
                setTimeout(Request.setStatusDone.bind(this), 0);
            }
        },

        cancelConfirmation : function ()
        {
            //debug('cancel confirmation');
            Input.uncoverDocument();
            var activeDiv = AWActiveDialogDiv;
            this.hideDialogDiv();
            //on incremental updates, we remove relocated copy (AWRelocatableDiv.awl)
            //and then show it again (Confirmation.awl). In this case, the active div 
            //will not have the parent node. 
            if (activeDiv && activeDiv.parentNode) {
                var id = activeDiv.parentNode.id;
                // If this is a client side confirmation, the revert will put the 
                // div back to its original location.
                Dom.revertRelocatedCopy(id);
                //If this is a server side confirmation, the orignal location marker
                //is overwritten, delete the moved copy. This HTML will be regenerated
                //when the dialog is displayed.
                Dom.removeRelocatedCopy(id);
            }
            awConfirmationId = null;
        },

        //////////////
        // About box scripts
        //////////////
        toggleAboutBox : function (targetId, evt)
        {
            evt = (evt) ? evt : event;

            // if a tab key caused the about box to be called, then noop
            if (evt.type == "keydown" && Event.keyCode(evt) != Input.KeyCodeEnter) {
                return true;
            }

            // This method must be registered on the mouse down event since the
            // awdisableDocumentClick is registered on the mouse down event.  If there is
            // an active dialog, then the user must have caused the awToggleAboutBox to be
            // called while the dialog is open so we should "close" the dialog.  In this case
            // cancelBubble so the document level click handler does not get called.

            // If there is not an active dialog, then we should open the dialog.  In this case
            // cancelBubble so the document.onMouseDown=awHideDialogDivthat we're going to
            // register via awShowDialogDiv doesn't get called.
            if (AWActiveDialogDiv) {
                this.hideDialogDiv();
            }
            else {
                this.hideActiveMenu();
                var toggleDiv = Dom.getElementById(targetId);
                this.showDialogDiv(toggleDiv, null, this.hideDialogDiv.bind(this));
                if (Refresh.childrenNeedLoading(toggleDiv)) {
                    Refresh.loadLazyChildren(toggleDiv);
                }
                Dom.overlay(toggleDiv);
                toggleDiv.awCloseDialogFunc = this.hideDialogDiv.bind(this);
            }
            Event.cancelBubble(evt);

            return false;
        },

        //////////////
        // Aliases
        //////////////
        aw01 : function (divObject, styleName)
        {
            return this.btnMouseOut(divObject, styleName);
        },

        aw02 : function (divObject, styleName)
        {
            return this.btnMouseOver(divObject, styleName)
        },
        // called in iframe to make the TOC hidden invisible if there's no content,
        // or put a min width if there is content
        updateTOC : function (hasVisibleContent)
        {
            var body = document.body;
            if (hasVisibleContent) {
                Dom.removeClass(body, "tocEmpty")
            } else {
                Dom.addClass(body, "tocEmpty")
            }
        },

        // Call with
        blinkItem : function (itemId, setCount) {
            Event.registerUpdateCompleteCallback(function () { Widgets._blinkItem(itemId, setCount * 6); });
        },

        _blinkItem : function (itemId, blinkCount) {
            // count down with quick flashes, followed by 4 sec delays every 6
            var field = document.getElementById(itemId);
            if (!field) return;
            field.className = (blinkCount % 2) ? "tocFlashing" : "tocItem";

            // flash 3 times in each set, up to blinkSetMax
            var delay = (blinkCount % 6) == 1 ? 4000 :  200;
            if (blinkCount) setTimeout(this._blinkItem.bind(this, itemId, blinkCount-1), delay);
        },


        registerHintMessage : function (id)
        {
            if (!AWHintIdList) {
                AWHintIdList = new Array(id);
                Event.registerOnWindowResize(this.sizeHintMessages.bind(this));
            }
            else {
                AWHintIdList[AWHintIdList.length] = id;
            }
        // initial sizing
            Event.eventEnqueue(this.sizeHintMessages.bind(this));
        },

        sizeHintMessages : function ()
        {
            var i = (AWHintIdList) ? AWHintIdList.length : 0;
            while (i--) {
                var hintMessage = Dom.getElementById(AWHintIdList[i]);
                if (hintMessage && hintMessage.className != "hintBoxOpen") {
                    hintMessage.style.overflowY = "auto";
                    if (hintMessage.scrollHeight > hintMessage.clientHeight) {
                        //alert('show expando');
                        hintMessage.className = "hintBoxClosed";
                    }
                    hintMessage.style.overflowY = "hidden";
                } else {
                    // remove
                    AWHintIdList.splice(i, 1);
                }
            }
        },

        openHintMessage : function (element)
        {
            Debug.log("awOpenHintMessage");
            var div = Dom.findParentUsingPredicate(element, function(n) {
                return (n.className == "hintBoxClosed")
            });
            div.className = "hintBoxOpen";

            Dom.checkWindowScrollbar(true);
        },

        closeHintMessage : function (element)
        {
            var div = Dom.findParentUsingPredicate(element, function(n) {
                return (n.className == "hintBoxOpen")
            });
            div.className = "hintBoxClosed";
            
            // hide scroll bar if not needed
            Dom.checkWindowScrollbar(true);
        },

        openHintMessageKeyDown : function (element, mevent)
        {
            if (Event.keyCode(mevent) == Input.KeyCodeEnter) {
                this.openHintMessage(element);
            }
        },

        closeHintMessageKeyDown : function (element, mevent)
        {
            if (Event.keyCode(mevent) == Input.KeyCodeEnter) {
                this.closeHintMessage(element);
            }
        },

        showSpan : function (id)
        {
            // A mouse over can be fired even when the doc is fully loaded.
            if (document.readyState &&
                document.readyState != "complete") {
                return;
            }
            var div = Dom.getElementById(id);
            div.style.display = "inline";
            var wrapper = div.parentNode;
            div.style.top = Dom.absoluteTop(wrapper);
            div.style.left = Dom.absoluteLeft(wrapper);
            Dom.overlay(div);
        },

        hideSpan : function (id)
        {
            var div = Dom.getElementById(id);
            div.style.display = "none";
            Dom.unoverlay(div);
        },

        popNotification : function ()
        {
            window.focus();
            var notificationDiv = Dom.getElementById('AWNotificationDiv');

        // enforce div max height
            notificationDiv.style.display = '';
            if (notificationDiv.clientHeight > 100) {
                var childDiv = Dom.findChild(notificationDiv, "DIV");
                childDiv.style.height = '100px';
            }
            notificationDiv.style.display = 'none';

            this.showNotification();

        // schedule the fade out based on the size of the notifications
            // Average reading speeding is 200 words/min.
            // Average word size is 10 characters (+1 for space)
            var characters = Dom.getInnerText(notificationDiv);
            var words = characters.length / 11;
            var time = words / 200 * 60000;
            if (time < AWMinNotificationTime) {
                time = AWMinNotificationTime;
            }
            else if (time > AWMaxNotificationTime) {
                time = AWMaxNotificationTime;
            }
            AWHideNotificationTimeout = setTimeout(this.hideNotification.bind(this), time);
        },

        showNotification : function ()
        {
            var notificationDiv = Dom.getElementById('AWNotificationDiv');
            Dom.fadeInElement(notificationDiv);
            Refresh.displayDiv(notificationDiv);
        },

        hideNotification : function ()
        {
            var notificationDiv = Dom.getElementById('AWNotificationDiv');
            if (notificationDiv) {
                var hideNotification = function () {
                    Refresh.undisplayDiv(notificationDiv);
                }
                if (notificationDiv.filters) {
                    Dom.fadeOutElement(notificationDiv);
                    AWHideNotificationTimeout = setTimeout(hideNotification.bind(this), 2000);
                }
                else {
                    AWHideNotificationTimeout = setTimeout(hideNotification.bind(this), 3000);
                }
            }
        },

        closeNotification : function ()
        {
            clearTimeout(AWHideNotificationTimeout);
            var notificationDiv = Dom.getElementById('AWNotificationDiv');
            Refresh.undisplayDiv(notificationDiv);
        },

        restoreNotification : function (event)
        {
            var notificationDiv = Dom.getElementById('AWNotificationDiv');
            if (notificationDiv.filters &&
                notificationDiv.filters.blendTrans) {
                notificationDiv.filters.blendTrans.stop();
            }
            if (AWHideNotificationTimeout) {
                clearTimeout(AWHideNotificationTimeout);
            }
            notificationDiv.style.visibility = "visible";
            Refresh.displayDiv(notificationDiv);
            Event.cancelBubble(event);
        },

        openDocWin : function (winAttr)
        {
            //var docWin = window.open('', 'AribaDocWin', 'scrollbars=yes, status=yes, resizable=yes, width=350, height=700, screenX=0, screenY=75, left=0, top=75');
            return Dom.openWindow('', 'AribaDocWin', winAttr);
        },

        /**
         * This opens a new window with the href specified by evt.
         * If evt was not generated by an A tag then there will either be
         * no action (very likely) or an incorrect href could be opened (technically possible).
         * @param evt
         */
        openWindowForEvent : function (evt)
        {
            if (!evt) {
                return false;
            }

            var src = Event.eventSourceElement(evt);
            if (!src) {
                return false;
            }

            var errStr = "DEBUG: Could not get href from link in Widgets.openWindowForEvent().";

            // we may have tags inside of the A, ie <a href="..."><b><i>LinkText</i></b></a>
            
            var i = 0;
            // we have to limit our traversal to 5 jumps to avoid the worst-case of
            // there not being an A tag.  This event may fire on ANY click so this is important.
            var matcher = function (node){
                if (i < 5 && "A" != node.nodeName) {
                    i++;
                    return false;
                }
                else {
                    return true;
                }
            };
            src = Dom.findParentUsingPredicate(src, matcher, true);
            if (!src || "A" != src.nodeName) {
                Debug.log(errStr);
                return false;
            }
            var href = src.href;
            // exit if it's an AWHyperlink
            if (!href || "#" == href || "#" == href.charAt(href.length -1)) {
                return false;
            }
            Dom.openWindow(href, '_blank');

            // cancel the default "clicked on a link" behavior
            Event.cancelBubble(evt);
            return false;
        },

        gotoDoc : function (url, ss, key, ut, un, rn, ul, anID, fts, area, winAttr)
        {
            // if the window is open, then close it.  This is unfortunately necessary since
            // the help "app" is on a remote machine.  once we write the form and submit, the
            // window is redirected to a different machine so we're no longer able to write
            // directly into the document.
            // This first check only catches the case that help window is opened from same
            // page.  see catch statement below for case where help is left open across pages.
            if (docWin && !docWin.closed && docWin.location) {
                docWin.close();
            }

            docWin = this.openDocWin(winAttr);

            var ftParams = '';
            var features = fts.split(',');
            for (var i = 0; i < features.length; i++) {
                ftParams += '<input name="ft" value="' + features[i] + '" type="hidden"/>';
            }

            var content =
                    '<html>' +
                    '<body onLoad="document.form1.submit();">' +
                    '<form method="post" action="' + url + '" id="form1" name="form1">' +
                    '<input name="ss" value="' + ss + '" type="hidden"/>' +
                    '<input name="doc" value="' + key + '" type="hidden"/>' +
                    '<input name="ut" value="' + ut + '" type="hidden"/>' +
                    '<input name="un" value="' + un + '" type="hidden"/>' +
                    '<input name="rn" value="' + rn + '" type="hidden"/>' +
                    '<input name="ul" value="' + ul + '" type="hidden"/>' +
                    '<input name="anId" value="' + anID + '" type="hidden"/>' +
                    ftParams +
                    '<input name="area" value="' + area + '" type="hidden"/>' +
                    '</form>' +
                    '</body>';
            try {
                docWin.document.write(content);
            }
            catch (e) {
                // for the case that help window left open across pages so close attempt
                // above does not catch it.  Close the window and try to write again.
                docWin.close();
                docWin = this.openDocWin();
                docWin.document.write(content);
            }

            docWin.document.close();

            if (docWin.focus) {
                docWin.focus();
            }
        },

        // Cue tip -- called when popup displayed
        sizeMsgDiv : function (div)
        {
            // if we haven't register, do it now -- we need to update post lazy div load
            if (!Dom.boolAttr(div, "_reg", false)) {
                div.setAttribute("_reg", "true");
                Event.registerUpdateCompleteCallback(this.sizeMsgDivUpdate.bind(this), [div]);
            }
            this.sizeMsgDivUpdate(div);
        },

        sizeMsgDivUpdate : function (div)
        {
            Debug.log("Updating div(" + div.id + "): " + div.id + " class:" + div.className);
        // If the clientWidth is > targetwidth, then
            // set the width to the targetwidth and explicitly set the div to allow wrapping.
            // Allows:
            // 1) longer tips to be set to targetwidth (with text wrap)
            // 2) shorter tips to be left alone (size to actual width)

            //awxdPrintProperties(div);
            // Debug.log('sizing -- actual: ' + div.style.width  + " cw:"+ div.clientWidth + " target:" + targetWidth);

            var screenSizeRatio = .33;
            var targetWidth = Dom.documentClientWidth() * screenSizeRatio;
            div.style.whiteSpace = "nowrap";
            if (div.clientWidth > targetWidth) {
                div.style.width = targetWidth + "px";
                div.style.whiteSpace = "normal";
            }
        },

        openPrintWindow : function (actionId)
        {
            // for Firefox we need to compute a full url
            var url = window.location.protocol + "//" + window.location.host + Request.formatUrl(actionId);
// alert("url = " + url + ", window = " + window);
            var w = Dom.openWindow(url, AWPrintWindowName, "location=0");
            if (w) {
                w.focus();
            }
        },

        printRefresh : function ()
        {
            // alert ("Refreshing window: \"" + window.name + "\" ...");
            top.location.href = top.ariba.Request.appendFrameName(top.Request.AWRefreshUrl);
        },

        printContents : function ()
        {
            // debugger;
            var content = Dom.getElementById("AWPrintContent");
            var pageWrapper = Dom.getElementById("BPR_Body");
            if (content && pageWrapper) {
                // pull out content area for display and hide the rest
                content.parentNode.removeChild(content);
                pageWrapper.parentNode.appendChild(content);
                pageWrapper.style.display = "none";
                Dom.removeClass(document.body, "hide");
                window.setTimeout(this.print.bind(this), 100);
            } else {
                alert("Failed to find print wrapper:  AWPrintContent=" + content + ", BPR_Body=" + pageWrapper);
            }
        },

        print : function ()
        {
            // invoke print dialog
            window.print();

        // force refresh of main window
            try {
                window.opener.ariba.Widgets.printRefresh();
            } catch (e) {
                // ignore
            }
            window.setTimeout(this.postPrint.bind(this), 1000);
        },

        postPrint : function ()
        {
            window.focus();
            window.close();
        },

        initErrorPanel : function ()
        {
            if (Dom.IsIE && Dom.IsIE6Only) {
                Event.registerWindowOnScroll(this.updateErrorPanel.bind(this));
                Event.registerOnWindowFixed(this.updateErrorPanel.bind(this));
            }
            var errorPanel = Dom.getElementById('PageErrorPanel');
            errorPanel.style.display = '';
        },

        updateErrorPanel : function ()
        {
            var errorPanel = Dom.getElementById('PageErrorPanel');
            if (errorPanel) {
                errorPanel.style.top = Dom.getPageScrollTop() + 'px';
                Dom.overlay(errorPanel, true);
            }
        },

        toggleErrorPanel : function (minimize)
        {
            var errorPanel = Dom.getElementById('PageErrorPanel');
            var minView = Dom.findChildUsingPredicate(errorPanel, function (e) {
                return e.id == "minimizedView";
            });
            var maxView = Dom.findChildUsingPredicate(errorPanel, function (e) {
                return e.id == "maximizedView";
            });
            var isMinimized;

            if (minimize == "true") {
                // minimize the error panel
                isMinimized = "true";
                minView.style.display = '';
                maxView.style.display = 'none';
            }
            else {
                // maximize the error panel
                isMinimized = "false";
                minView.style.display = 'none';
                maxView.style.display = '';
                this.slideErrorMessage();
            }

            for (var i = 0; i < document.forms.length; i++) {
                var formObject = document.forms[i];
                var hiddenObject = formObject[IsMinimizedFieldName];
                if (!hiddenObject) {
                    Dom.addFormField(formObject, IsMinimizedFieldName, isMinimized);
                }
                else {
                    hiddenObject.value = isMinimized;
                }
            }
        },

        showBubble : function (positioningObject)
        {
            var errorContentDivId = positioningObject.getAttribute('_errorContentDivId');
            var autoHideBubble = positioningObject.getAttribute('_autoHideBubble');
            var autoScroll = positioningObject.getAttribute('_autoScroll');
            var customContentDivId = positioningObject.getAttribute('_customContentDivId');
            var positionRight = positioningObject.getAttribute('_positionRight');

            var errorContentDiv = Dom.getElementById(errorContentDivId);
            var customDiv = Dom.getElementById(customContentDivId);
            var bubble = Dom.getElementById('bubble_tooltip_fade');
            var bubbleBody = Dom.getElementById('bubble_body');
            var bubbleContent = Dom.getElementById('bubble_tooltip_content');
            var closeControl = Dom.getElementById('bubble_close_control');
            var leftTip = Dom.getElementById('bubble_tip_left');
            var rightTip = Dom.getElementById('bubble_tip_right');
            var leftBottom = Dom.getElementById('bubble_bottom_left');
            var rightBottom = Dom.getElementById('bubble_bottom_right');
        // bubble content
            bubble.style.width = 130 + 'px';
            var content = (errorContentDiv ? errorContentDiv.innerHTML : "")
                    + (customDiv ? customDiv.innerHTML : "");
            bubbleContent.innerHTML = content;
            bubble.style.display = 'block';
        // close control
            if (autoHideBubble == "true") {
                closeControl.style.display = 'none';
            }
            else {
                closeControl.style.display = '';
            }
        // dynamic sizing
            var bubbleClientWidth = bubble.clientWidth;
            var bubbleScrollWidth = bubble.scrollWidth;
            if (bubbleScrollWidth > bubbleClientWidth) {
                bubble.style.width = bubbleScrollWidth + 'px';
            // workaround for IE: the content is not displayed after resizing.
                bubbleContent.innerHTML = content;
            }
            if (bubble.clientHeight > bubble.clientWidth) {
                bubble.style.width = Math.max(bubble.clientWidth * 1.6, bubble.clientHeight) + 'px';
            // workaround for IE: the content is not displayed after resizing.
                bubbleContent.innerHTML = content;
            }
        // position the bubble
            var newLeft = Dom.absoluteLeft(positioningObject);
            if (positionRight)  newLeft += Dom.containerOffsetSize(positioningObject)[0];
            var newBottom = Dom.absoluteTop(positioningObject) - 7;
            var newTop = newBottom - bubble.offsetHeight + 9;
            if ((newLeft + bubble.offsetWidth + 2) < Dom.documentElement().clientWidth) {
                newLeft += 3;
                leftTip.style.display = '';
                leftBottom.style.display = '';
                rightTip.style.display = 'none';
                rightBottom.style.display = 'none';
            }
            else {
                newLeft -= bubble.offsetWidth;
                leftTip.style.display = 'none';
                leftBottom.style.display = 'none';
                rightTip.style.display = '';
                rightBottom.style.display = '';
            }
            Dom.setAbsolutePosition(bubble, newLeft, newTop);
            Refresh.displayDiv(bubbleBody, 'block');
        // scroll if necessary to ensure that the bubble is visible
            if (autoScroll == "true") {
                var scrolltop = Dom.getPageScrollTop();
                var newScrollTop = Math.max(newTop - (Dom.documentElement().clientHeight / 2), 0);
                if (scrolltop > newTop) {
                    Dom.setPageScrollTop(newScrollTop);
                }
                else {
                    var scrollBottom = scrolltop + Dom.documentElement().clientHeight - 25;
                    if (scrollBottom < newBottom) {
                        Dom.setPageScrollTop(newScrollTop);
                    }
                }
            }
        },

        hideBubble : function ()
        {
            var obj = Dom.getElementById('bubble_tooltip_fade');
            if (obj) {
                Refresh.undisplayDiv(obj);
            }
            var bubbleBody = Dom.getElementById('bubble_body');
            if (bubbleBody) {
                Dom.unoverlay(bubbleBody);
            }
        },

        showBubbleWithFade : function (positioningObject)
        {
            this.showBubble(positioningObject);

        // schedule the fade out based on the size of the notifications
            // Average reading speeding is 200 words/min.
            // Average word size is 10 characters (+1 for space)
            var errorContentDivId = positioningObject.getAttribute('_errorContentDivId');
            var errorContentDiv = Dom.getElementById(errorContentDivId);
            var words = Dom.getInnerText(errorContentDiv).length / 11;
            var time = words / 200 * 60000;
            if (time < AWMinNotificationTime) {
                time = AWMinNotificationTime;
            }
            else if (time > AWMaxNotificationTime) {
                time = AWMaxNotificationTime;
            }
            if (AWHideBubbleTimeout) {
                clearTimeout(AWHideBubbleTimeout);
            }
            AWHideBubbleTimeout = setTimeout(this.hideBubble.bind(this), time);
        },

        highLightIndicator : function (indicatorId)
        {
            var indicatorElm = Dom.getElementById(indicatorId);
            if (indicatorElm) {
                // queue up the poppping of the bubble until all the panel
                // resizing and Confirmation panel reparenting are done.
                CurrentErrorIndicatorId = indicatorId;
            // enqueue to run after resizing complete
                Event.eventEnqueue(this.highLightIndicatorInternal.bind(this), null, true);
            }
        },

        highLightIndicatorInternal : function ()
        {
            var indicatorElm = Dom.getElementById(CurrentErrorIndicatorId);
            if (indicatorElm) {
                var autoHideBubble = indicatorElm.getAttribute('_autoHideBubble');
                if (autoHideBubble == "true") {
                    this.showBubbleWithFade(indicatorElm);
                }
                else {
                    this.showBubble(indicatorElm);
                }
            }
            Event.unregisterOnWindowFixed(this.highLightIndicatorInternal);
            CurrentErrorIndicatorId = null;
        },

        containedInConfirmationPanel : function (obj)
        {
            return Dom.findParentUsingPredicate(obj, function(n) {
                return n.className == 'panelContainer';
            }) != null;
        },

        resetSlidingErrorMessage : function ()
        {
            var slider = Dom.getElementById('slidingErrorMsg');
            var sliderContent = Dom.getElementById('slidingErrorMsgContent');
            sliderContent.style.top = 0 - sliderContent.offsetHeight + 'px';
            slider.style.display = 'none';
            slider.style.height = '1px';
        },

        slideErrorMessage : function ()
        {
            this.resetSlidingErrorMessage();
            var slider = Dom.getElementById('slidingErrorMsg');
            slider.style.display = 'block';
            slider.style.visibility = 'visible';
            this.slideMover(1);
        },

        slideMover : function (direction)
        {
            //debugger;
            var slider = Dom.getElementById('slidingErrorMsg');
            var sliderContent = Dom.getElementById('slidingErrorMsgContent');

            var height = slider.clientHeight;
            if (height == 0) {
                height = slider.offsetHeight;
            }
            height = height + (slideSpeed * direction);
            var rerun = true;
            if (height > sliderContent.offsetHeight) {
                height = sliderContent.offsetHeight;
                rerun = false;
            }
            if (height <= 1) {
                height = 1;
                rerun = false;
            }
            slider.style.height = height + 'px';

            var topPos = height - sliderContent.offsetHeight;
            if (topPos > 0) {
                topPos = 0;
            }
            sliderContent.style.top = topPos + 'px';

            if (rerun) {
                if (AWSlideTimer) {
                    clearTimeout(AWSlideTimer);
                }
                AWSlideTimer = setTimeout(this.slideMover.bind(this, 1), slideTiming);
            }
            else {
                if (height <= 1) {
                    slider.style.display = 'none';
                }
            }
        },

        initFooter : function (displayDoubleHeight)
        {
            AWFooter = new Object();
            AWFooter.footer = Dom.getElementById('Footer');
            AWFooter.floatingFooter = Dom.getElementById('FloatingFooter');
            AWFooter.footerControl = Dom.getElementById('FooterControl');
            AWFooter.footerExpand = Dom.getElementById('FooterExpand');
            AWFooter.footerCollapse = Dom.getElementById('FooterCollapse');
            AWFooter.footerLinks = Dom.getElementById('FooterLinks');
            AWFooter.expanded = false;
            AWFooter.displayDoubleHeight = displayDoubleHeight;
            this.updateFooterCollapseHeight();
            if (Dom.IsIE && Dom.IsIE6Only) {
                Event.registerOnWindowFixed(this.updateFooterBottom.bind(this));
                Event.fireWindowFixedCallback();
                Event.registerWindowOnScroll(this.updateFooterBottom.bind(this));
            }
            Event.registerOnWindowResize(this.updateFooter.bind(this));
        },

        hideFooter : function ()
        {
            AWFooter = null;
        },

        updateFooterCollapseHeight : function ()
        {
            var minHeight = AWFooter.displayDoubleHeight ? 28 : 16;
            minHeight = Math.max(minHeight, AWFooter.footerLinks.scrollHeight + 2);
            AWFooter.collapseHeight = minHeight + 'px';
            AWFooter.footer.style.height = AWFooter.collapseHeight;
        },

        expandFooter : function ()
        {
            var footer = AWFooter.footer;
            footer.style.height = footer.scrollHeight + 'px';
        },

        collapseFooter : function ()
        {
            var footer = AWFooter.footer;
            footer.style.height = AWFooter.collapseHeight;
        },

        updateFooter : function ()
        {
            if (AWFooter) {
                var footer = AWFooter.footer;
                var footerExpand = AWFooter.footerExpand;
                var footerCollapse = AWFooter.footerCollapse;
                var footerControl = AWFooter.footerControl;
                this.updateFooterCollapseHeight();
                if (AWFooter.footerControl) {
                    if (AWFooter.expanded) {
                        footerControl.innerHTML = footerCollapse.innerHTML;
                        this.expandFooter();
                    }
                    else {
                        if (footer.scrollHeight > footer.clientHeight) {
                            footerControl.innerHTML = footerExpand.innerHTML;
                        }
                        else {
                            footerControl.innerHTML = '';
                        }
                    }
                }
                this.updateFloatingFooter();
            }
        },

        updateFloatingFooter : function ()
        {
            var documentElement = Dom.documentElement();
            var floatingFooter = AWFooter.floatingFooter;
            var footer = AWFooter.footer;
            floatingFooter.innerHTML = footer.innerHTML;
            floatingFooter.style.left = Dom.absoluteLeft(footer) + 'px';
            floatingFooter.style.width = footer.clientWidth + 'px';
            floatingFooter.style.height = footer.clientHeight + 'px';
            floatingFooter.style.overflowY = Dom.effectiveStyle(footer, 'overflow-y');
        },

        hideFloatingFooter : function ()
        {
            if (Dom.IsIE && Dom.IsIE6Only && AWFooter) {
                var floatingFooter = AWFooter.floatingFooter;
                Refresh.undisplayDiv(floatingFooter);
                floatingFooter.style.top = '0px'
            }
        },

        updateFooterBottom : function ()
        {
            // Setting the floating footer to the bottom sometimes changes the document's scrollTop,
            // so only setting it at the end of all content (datatable) resizing.
            if (Dom.IsIE && Dom.IsIE6Only && AWFooter) {
                var floatingFooter = AWFooter.floatingFooter;
                var footer = AWFooter.footer;
                floatingFooter.style.top =
                document.documentElement.scrollTop +
                document.documentElement.clientHeight -
                footer.offsetHeight + 'px';
                floatingFooter.style.display = '';
                Dom.overlay(floatingFooter);
            }
        },

        toggleFooter : function ()
        {
            var footer = AWFooter.footer;
            var footerExpand = AWFooter.footerExpand;
            var footerCollapse = AWFooter.footerCollapse;
            var footerControl = AWFooter.footerControl;
            var floatingFooter = Dom.getElementById('FloatingFooter');
            if (AWFooter.expanded) {
                footerControl.innerHTML = footerExpand.innerHTML;
                footer.style.overflowY = 'hidden';
                this.collapseFooter();
            }
            else {
                footerControl.innerHTML = footerCollapse.innerHTML;
                footer.style.overflowY = 'visible';
                this.expandFooter();
            }
            AWFooter.expanded = !AWFooter.expanded;

            this.updateFooter();
            if (Dom.IsIE && Dom.IsIE6Only) {
                this.updateFooterBottom();
            }
        },

        toggleFooterKeyDown : function (mevent)
        {
            if (Event.keyCode(mevent) == Input.KeyCodeEnter) {
                this.toggleFooter();
            }
        },

        _widgetsHandlers_MARKER : function () {
        },

        positionActiveDialogBox : function ()
        {
            if (AWActiveDialogDiv) {
                Dom.positionDialogBox(AWActiveDialogDiv);
            }
        },

        // used to guarantee that only one count down is scheduled
        CountDownTimer : null,

        handlePollEvents : function (pollState, pollInfo)
        {

            var pollCountDown = Dom.getElementById('pollCountDown');
            var pollDialog = Dom.getElementById('pollDialog');
            var pollInterval = pollCountDown.getAttribute("_in");

            var timer = function () {
                clearTimeout(this.CountDownTimer);
                this.CountDownTimer = setTimeout(updatePollCountDown, 1000);
            }.bind(this);
            
            if (Request.AWPollState == pollState) {
                if (this.CountDownTimer) {
                    clearTimeout(this.CountDownTimer);
                    this.CountDownTimer = null;
                    this.hideDialogDiv();
                }
            }
            else if (Request.AWPollErrorState == pollState) {
                if (!AWActiveDialogDiv) {
                    pollCountDown.innerHTML = pollInterval;
                    pollDialog.awPreCloseDialogFunc = this.enablePage.bind(this);
                    pollDialog.awCloseDialogFunc = this.hideDialogDiv.bind(this);
                    this.showDialogDiv(pollDialog, this.disablePage.bind(this));
                    function updatePollCountDown () {                        
                        var countDown = pollCountDown.innerHTML;
                        countDown = parseInt(countDown);
                        if (countDown > 0) {
                            pollCountDown.innerHTML = countDown - 1;
                            timer();
                        }
                    }
                    timer();
                }
            }
        },

        ActiveHoverCard : null,
        HideActiveHoverCardTimeout : null,

        getHoverCard : function (hoverLink) {
            var hoverCard = Dom.find(hoverLink, "hcard")[0];
            // use hover card id if it has been relocated
            if (!hoverCard) {
                hoverCard = Dom.getElementById(hoverLink.getAttribute("hoverId"));
            }
            else {
                hoverLink.setAttribute("hoverId", hoverCard.id);
            }
            return hoverCard;
        },

        displayHoverCard : function (hoverLink)
        {
            var hoverCard = this.getHoverCard(hoverLink);
            Dom.relocateDiv(hoverCard);
            this._hideActiveHoverCard(hoverCard);

            var newTop = 0;
            var newLeft = 0;
            var position = hoverLink.getAttribute("_pos");
            if (position == "bottom") {
                Dom.addClass(hoverCard, "hoverBottom");
                // calculate newTop and newLeft
                newTop = Dom.absoluteTop(hoverLink) + hoverLink.offsetHeight;
                newLeft = Dom.absoluteLeft(hoverLink);
                var childrenArray = Dom.getChildren(hoverCard);
                var arrayLength = childrenArray.length;
                var hcContentWidth = 0;
                hoverCard.style.display = '';
                for (index = 0; index < arrayLength; index++) {
                    var childObject = childrenArray[index];
                    if (Dom.hasClass(childObject, "hcContent")) {
                        hcContentWidth = childObject.offsetWidth;
                        break;
                    }
                }
                if (newLeft + hcContentWidth >= Dom.documentClientWidth()-20) {
                    // hover will be shifted left due to space issue
                    Widgets.positionHoverCardBottomPointer(hoverLink, hoverCard, hcContentWidth, "left");
                    newLeft = Dom.absoluteLeft(hoverLink) + hoverLink.offsetWidth - hcContentWidth;
                }
                else {
                    Widgets.positionHoverCardBottomPointer(hoverLink, hoverCard, hcContentWidth, null);
                }
            }
            else {
                var newTop = Dom.absoluteTop(hoverLink) - 20;

                var hoverLinkLeft = Dom.absoluteLeft(hoverLink);
                var hoverLinkWidth = hoverLink.offsetWidth;
                var linkLeftCenter =
                     hoverLinkLeft + hoverLinkWidth / 2;
                var newLeft = 0;
                if (linkLeftCenter <= Dom.documentClientWidth() / 2) {
                    newLeft = hoverLinkLeft + hoverLinkWidth;
                }
                else {
                    hoverCard.style.display='';
                    newLeft = hoverLinkLeft - hoverCard.offsetWidth;
                    Dom.addClass(hoverCard, "hoverLeft");
                }
            }
            Dom.setAbsolutePosition(hoverCard, newLeft, newTop);
            Refresh.displayDiv(hoverCard);
            this.ActiveHoverCard = hoverCard;
            hcTimeout = null;
        },

        positionHoverCardBottomPointer : function (hoverLink, hoverCard, hcContentWidth, position)
        {
            var childrenArray = Dom.getChildren(hoverCard);
            var arrayLength = childrenArray.length;
            for (index = 0; index < arrayLength; index++) {
                var childObject = childrenArray[index];
                var pointerPosition = hoverLink.offsetWidth/2 + 'px';
                if (hoverLink.offsetWidth >= hcContentWidth) {
                    // If link is longer than hover card content, the position of
                    // the pointer will be half of the hover card content box
                    pointerPosition = hcContentWidth/2 + 'px';
                }
                if (Dom.hasClass(childObject, "hcPointer") || Dom.hasClass(childObject, "hcPointerInner")) {
                    if (position == "left") {
                        childObject.style.right = pointerPosition;
                        childObject.style.left = 'auto';
                    }
                    else {
                        childObject.style.left = pointerPosition;
                        childObject.style.right = 'auto';
                    }
                }
            }
        },

        hideActiveHoverCard : function ()
        {
            this.clearHideHoverCard();
            this.HideActiveHoverCardTimeout =
                setTimeout(this._hideActiveHoverCard.bind(this), 200);
        },

        _hideActiveHoverCard : function (newHoverCard)
        {
            var hoverCard = this.ActiveHoverCard;
            if (hoverCard && hoverCard != newHoverCard) {
                Refresh.undisplayDiv(hoverCard);
                Dom.removeClass(hoverCard, "hoverLeft");
            }
        },

        clearHideHoverCard : function ()
        {
            clearTimeout(this.HideActiveHoverCardTimeout);
        },

        showSpotlights : function (spotlightIds)
        {
            var spotlights = this.getSortedSpotlights(spotlightIds);
            var dimCovers = this.computeDimCols(spotlights);
            if (!dimCovers) {
                spotlights = spotlights.sort(this.spotlightTopSort);
                dimCovers = this.computeDimRows(spotlights);
            }
            if (!dimCovers) {
                console.log("Cannot compute dim covers");
                return;
            }
            for (var i = 0; i < dimCovers.length; i++) {
                var dimCover = dimCovers[i];
                if (Dom.IsIE) {
                    // workaround for rendering issue at higher opacity
                    this.dimDocument(dimCover, 20);
                    this.dimDocument(dimCover, 20);
                }
                else {
                    this.dimDocument(dimCover, 50);
                }
            }
            var spotlightsDiv = Dom.getElementById("spotlights");
            var documentElement = Dom.getDocumentElement();
            var docWidth = Util.max(documentElement.scrollWidth, documentElement.clientWidth);
            var rightCount = 0;
            for (i = 0; i < spotlights.length; i++) {
                var spotlight = spotlights[i];
                var spotlightTop = spotlight.top;
                var spotlightLeft = spotlight.left;
                var spotlightWidth = spotlight.width;
                var spotlightHeight = spotlight.height;
                var title = spotlight.elm.getAttribute("_t");
                var value = spotlight.elm.getAttribute("_v");
                var text =
                    Dom.createElement('<div class="spotlightText"><h2>' + title + '</h2>' + value + '</div>');
                var line = null;
                if (spotlightLeft >= spotlightTop) {
                    // top right half
                    text.style.top = spotlightTop + spotlightHeight + 35 + "px";

                    line = Dom.createElement('<div class="spotlightVLine"></div>');
                    var left = spotlightLeft + Math.min(50, spotlightWidth/2);
                    line.style.top = spotlightTop + spotlightHeight + "px";
                    line.style.left = left + "px";
                    line.style.height = "40px";
                    
                    if ((spotlightLeft + spotlightWidth / 2) > docWidth / 2) {
                        // right
                        text.style.right = docWidth - spotlightLeft - spotlightWidth + "px";
                        if (rightCount % 2 == 1) {
                            // flip even ones above spotlight
                            text.style.top = spotlightTop - 70 + "px";
                            line.style.top = spotlightTop - 25 + "px";
                        }
                        rightCount++;
                    }
                    else {
                        // top
                        text.style.left = spotlightLeft + 20 + "px";

                    }
                }
                else {
                    // bottom left half
                    text.style.top = spotlightTop + 35 + "px";
                    text.style.left = spotlightLeft + spotlightWidth + 20 + "px";

                    line = Dom.createElement('<div class="spotlightHLine"></div>');
                    line.style.top = spotlightTop + 50 + "px";
                    line.style.left = spotlightLeft + spotlightWidth + "px";
                    line.style.width = "25px";
                }
                spotlightsDiv.appendChild(text);
                spotlightsDiv.appendChild(line);
            }
        },

        dimDocument : function (dimCover, opacity)
        {
            if (dimCover.opacity !== undefined) {
                opacity = dimCover.opacity;
            }
            var coverDiv = ariba.Input.createCoverDiv(50, opacity);
            var coverStyle = coverDiv.style;
            coverStyle.top = dimCover.top + "px";
            coverStyle.left = dimCover.left + "px";
            coverStyle.width = dimCover.width + "px";
            coverStyle.height = dimCover.height + "px";
            var spotlightsDiv = Dom.getElementById("spotlights");
            spotlightsDiv.appendChild(coverDiv);
        },

        spotlightLeftSort : function (spotlightA, spotlightB)
        {
             return spotlightA.left - spotlightB.left;
        },

        spotlightTopSort : function (spotlightA, spotlightB)
        {
             return spotlightA.top - spotlightB.top;
        },

        getSortedSpotlights : function (spotlightIds)
        {
            var spotlights = [];
            var padding = 6;
            for (var i = 0; i < spotlightIds.length; i++) {
                var spotlight = Dom.getElementById(spotlightIds[i]);
                var top = Dom.absoluteTop(spotlight) - padding;
                var left = Dom.absoluteLeft(spotlight) - padding;
                var width = Util.max(spotlight.clientWidth, spotlight.scrollWidth);
                width = width + 2 * padding;
                var height =Util.max(spotlight.clientHeight, spotlight.scrollHeight);
                height = height + 2 * padding;
                spotlights.push({ elm : spotlight, top : top, left : left, width : width, height : height });
            }
            return spotlights.sort(this.spotlightLeftSort);
        },

        computeDimCols : function (spotlights)
        {
            var documentElement = Dom.getDocumentElement();
            var docWidth = Util.max(documentElement.scrollWidth, documentElement.clientWidth);
            var docHeight = Util.max(documentElement.scrollHeight, documentElement.clientHeight);
            var banner = Dom.getElementById("BPR_Banner");
            var bannerHeight = Util.max(banner.clientHeight, banner.scrollHeight)+10;
            var dimCovers = [];
            var top = 0;
            var left = 0;
            var width = 0;
            var height = 0;
            for (var i = 0; i < spotlights.length; i++) {
                var spotlight = spotlights[i];
                var spotlightTop = spotlight.top;
                var spotlightLeft = spotlight.left;
                var spotlightWidth = spotlight.width;
                var spotlightHeight = spotlight.height;

                // left full height cover
                top = bannerHeight;
                width = spotlightLeft - left;
                if (width < 0) {
                    return null;
                }
                height = docHeight - top;
                dimCovers.push(
                    {top : top, left : left, width : width, height : height }
                );

                //top cover
                left += width;
                width = spotlightWidth;
                height = spotlightTop - top;
                dimCovers.push(
                    {top : top, left : left, width : width, height : height }
                );

                // center cover
                top += height;
                height = spotlightHeight;
                dimCovers.push(
                    {top : top, left : left, width : width, height : height, opacity : 0 }
                );

                // bottom cover
                top += height;
                height = docHeight - top;
                dimCovers.push(
                    {top : top, left : left, width : width, height : height }
                );

                left += width;
            }
            // right most cover
            top = bannerHeight;
            width = docWidth - left;
            height = docHeight - top;
            dimCovers.push(
                {top : top, left : left, width : width, height : height }
            );
            return dimCovers;
        },

        computeDimRows : function (spotlights)
        {
            var documentElement = Dom.getDocumentElement();
            var docWidth = Util.max(documentElement.scrollWidth, documentElement.clientWidth);
            var docHeight = Util.max(documentElement.scrollHeight, documentElement.clientHeight);
            var banner = Dom.getElementById("BPR_Banner");
            var bannerHeight = Util.max(banner.clientHeight, banner.scrollHeight)+10;
            var dimCovers = [];
            var top = bannerHeight;
            var left = 0;
            var width = 0;
            var height = 0;
            for (var i = 0; i < spotlights.length; i++) {
                var spotlight = spotlights[i];
                var spotlightTop = spotlight.top;
                var spotlightLeft = spotlight.left;
                var spotlightWidth = spotlight.width;
                var spotlightHeight = spotlight.height;

                // top full width cover
                left = 0;
                width = docWidth;
                height = spotlightTop - top;
                if (height < 0) {
                    return null;
                }
                dimCovers.push(
                    {top : top, left : left, width : width, height : height }
                );

                //left cover
                top += height;
                height = spotlightHeight;
                width = spotlightLeft;
                dimCovers.push(
                    {top : top, left : left, width : width, height : height }
                );

                // center cover
                left += width;
                width = spotlightWidth;
                dimCovers.push(
                    {top : top, left : left, width : width, height : height, opacity : 0 }
                );

                // right cover
                left += width;
                width = docWidth - left;
                dimCovers.push(
                    {top : top, left : left, width : width, height : height }
                );

                top += height;
            }
            // bottom most cover
            left = 0;
            width = docWidth;
            height = docHeight - top;
            dimCovers.push(
                {top : top, left : left, width : width, height : height }
            );
            return dimCovers;
        },

    EOF:0};

    //
    // Behaviors
    //
    Event.registerBehaviors({
        // TextButton - no click handling (used when button has own onclick handler)
        TBc : {
            mousedown : function (elm, evt) {
                // Add "Over" if it's not already there
                var s = elm.getAttribute('_cl');
                s = s + "Over";
                return Widgets.btnMouseOver(elm, s);
            },

            focus : function (elm, evt) {
                return Event.behaviors.TBc.mouseover(elm, evt);
            },

            mouseup : function (elm, evt) {
                // Strip "Over"
                var s = elm.getAttribute('_cl');
                return Widgets.btnMouseOut(elm, s);
            },

            mouseout : function (elm, evt) {
                // Strip "Over"
                var s = elm.getAttribute('_cl');
                return Widgets.btnMouseOut(elm, s);
            },

            blur : function (elm, evt) {
                return Event.behaviors.TBc.mouseout(elm, evt);
            }
        }
    });
    Event.registerBehaviors({
        // TextButton - adds click handling
        TB : {
            prototype : Event.behaviors.TBc,

            click : function (elm, evt) {
                var confId = elm.getAttribute("_cnf");
                return (confId) ? Widgets.showConfirmation(confId)
                        : Event.behaviors.GAT.click(elm, evt);
            },


            keypress : function (elm, evt) {
                return (Event.keyCode(evt) == Input.KeyCodeEnter) ? Event.behaviors.TB.click(elm, evt) : true;
            }
        },

        // TextFormSubmitButton - adds click handling
        TFSB : {
             prototype : Event.behaviors.TBc,

             click : function (elm, evt) {
                 var formName = elm.getAttribute("_fn");
                 return Request.submitFormObjectNamed(formName);
             },

             keypress : function (elm, evt) {
                 return (Event.keyCode(evt) == Input.KeyCodeEnter) ? Event.behaviors.TFSB.click(elm, evt) : true;
             }
        },

        // Confirmation Hyperlink
        CHL : {
            prototype : Event.behaviors.HL,

            click : function (elm, evt) {
                var confId = elm.getAttribute("_cnf");
                return (confId) ? Widgets.showConfirmation(confId)
                        : Event.behaviors.HL.click(elm, evt);
            }
        },

        // HME HintMessage Expanded, HMC HintMessage Collapsed
        HME : {
            mousedown : function (elm, evt) {
                return Widgets.openHintMessage(elm);
            },


            keydown : function (elm, evt) {
                return Widgets.openHintMessageKeyDown(elm, evt);
            }
        },

        HMC : {
            mousedown : function (elm, evt) {
                return Widgets.closeHintMessage(elm);
            },

            keydown : function (elm, evt) {
                return Widgets.closeHintMessageKeyDown(elm, evt);
            }
        },

        // BI - TOC Bucket Item
        BI : {
            mouseover : function (elm, evt) {
                elm.className = 'tocItemRollover';
                return false;
            },

            mouseout : function (elm, evt) {
                elm.className = 'tocItem';
                return false;
            }
        },

        // OutC - AWXOutlineControl
        OutC : {
            mouseover : function (elm, evt) {
                elm.className = 'wizSubstepCurrent nav noWrap';
                return false;
            },

            mouseout : function (elm, evt) {
                elm.className = 'wizSubstep nav noWrap';
                return false;
            }
        },

        // ND - Notification Dialog
        ND : {
            mouseover : function (elm, evt) {
                return Widgets.restoreNotification(evt);
            },

            mouseout : function (elm, evt) {
                return Widgets.hideNotification();
            }
        },

        FT : {
            mousedown : function (elm, evt) {
                return Widgets.toggleFooter(evt);
            },

            keydown : function (elm, evt) {
                return Widgets.toggleFooterKeyDown(evt);
            }
        },

        // new window links - a href's open in new windows
        NWL : {
            click : function (elm, evt) {
                return Widgets.openWindowForEvent(evt);
            },
            keypress : function (elm, evt) {
                if (evt && (Input.KeyCodeEnter == evt.keyCode) ) {
                    return Widgets.openWindowForEvent(evt);
                }
            },
            // keydown is needed for Safari
            keydown : function (elm, evt) {
                if (evt && (Input.KeyCodeEnter == evt.keyCode) ) {
                    return Widgets.openWindowForEvent(evt);
                }
            }
        },

        // Pop bubble on drag
        DrgBub : {
            prototype : Event.behaviors.DrG,
            dragstart : Widgets.showBubble,
            dragend   : Widgets.hideBubble
        },

        // HoverCard
        HCC : Widgets.HoverCardContentBehavior,

        HC : {
            mouseover : function (hoverCard, evt) {
                ariba.Widgets.clearHideHoverCard();
            },
            mouseout : function (hoverLink, evt) {
                ariba.Widgets.hideActiveHoverCard();
            }
        }

    });

    return Widgets;
}();
