/*
    Menu.js     -- support for popup menus

    (see ariba.ui.widgets.PopupMenu.awl)    
*/

ariba.Menu = function() {
    // imports
    var Util = ariba.Util;
    var Event = ariba.Event;
    var Request = ariba.Request;
    var Refresh = ariba.Refresh;
    var Widgets = ariba.Widgets;
    var Dom = ariba.Dom;
    
    // private vars

    // var AWMenuLinkSenderIdKey = 'awmls';
    // var AWPreviousMenuCell = null;

    var Menu = {
        // Public / Protected Globals
        AWActiveMenu : null,
        AWLinkId : null,
        AWMenuOffset : 15,


        unhiliteDiv : function (divObject)
        {
            if (divObject != null) {
                Dom.removeClass(divObject, 'awmenuCellHilite');
            }
        },

        hiliteDiv : function (divObject)
        {
            if (divObject != null) {
                Dom.addClass(divObject, 'awmenuCellHilite');
            }
        },

        hideActiveMenu : function ()
        {
            if (this.AWActiveMenu != null && Dom.elementInDom(this.AWActiveMenu)) {
                Event.disableDocumentClick();
                var menuLinks = this.menuLinks(this.AWActiveMenu.id);
                for (var i = 0; i < menuLinks.length; i++) {
                    this.cellMouseOut(menuLinks[i]);
                }
                Refresh.undisplayDiv(this.AWActiveMenu);
            }
            this.AWActiveMenu = null;
        },
        /*
            Called when the user types a key on the popup menu link
        */

        menuLinkOnKeyDown : function (positioningObject, menuName, linkId, mevent)
        {
            if (Event.keyCode(mevent) == 13) {
                this.menuLinkOnClick(positioningObject, menuName, linkId, mevent);
                var firstMenuLink = this.firstMenuLink(menuName);
                if (firstMenuLink != null) {
                    firstMenuLink.focus();
                }
                return false;
            }
            return true;
        },

        menuLinks : function (menuName)
        {
            var menu = Dom.getElementById(menuName);
            return Dom.findChildrenUsingPredicate(menu, function (e) {
                    return e.tagName == "A" &&
                           (e.className.indexOf("awmenuCell") > -1 || e.className.indexOf("mC") > -1) &&
                           e.getAttribute("skipLink") != "1";
                }, true);
        },

        firstMenuLink : function (menuName)
        {
            var menuLinks = this.menuLinks(menuName);
            return menuLinks[0];
        },

        lastMenuLink : function (menuName)
        {
            var menuLinks = this.menuLinks(menuName);
            return menuLinks[menuLinks.length - 1];
        },

        menu : function (menuCellDivLink)
        {
            return Dom.findParentUsingPredicate(menuCellDivLink, function(n) {
                    return (n.tagName == "DIV" && n.className == "awmenu")
                });
        },

        /**
         Called when user clicks a PopupMenuLink.
         */
        // used by both PopupMenu and PopupMenuLink to handle / cancel mouse down
        // both need to have the active menu closed and not affect any containing
        // component (table row select) -- use cover div in future to avoid having
        // to explicitly cancel mouse down.
        menuOnMouseDown : function (mevent)
        {
            this.hideActiveMenu();
            Event.cancelBubble(mevent);
            return false;
        },

        menuLinkOnClick : function (positioningObject, menuName, linkId, mevent)
        {
            var divObject = Dom.getElementById(menuName);
            var x;
            var y;
            if (mevent) {
                x = mevent.pageX;
                y = mevent.pageY;
            }
            Dom.relocateDiv(divObject);
            // coordinates changes after relocating the div on Firefox 3
            // workaround by fabricating a fake event containing the original coordinates

            if (mevent && (x != mevent.pageX || y != mevent.pageY)) {
                mevent = new Object();
                mevent.pageX = x;
                mevent.pageY = y;
            }

            if (this.AWActiveMenu != divObject) {
                this.hideActiveMenu();

            // assign event handler
                divObject.onmousedown = this.menuOnMouseDown.bindEventHandler(this);

                this.AWLinkId = linkId;
                this.AWActiveMenu = divObject;
                Event.enableDocumentClick(this.hideActiveMenu.bind(this));
                Dom.removeClass(divObject, "awmenuEx");  // collapse expanded menu on new open
                this.checkMenuLayout(divObject);
                if (positioningObject != null) {

                    // must display first to get proper coordinates.
                    divObject.style.display = '';
                // position to 0,0 to allow the popup to expand to full size
                    divObject.style.top = 0;
                    divObject.style.left = 0;
                // The PulldownButton uses an anchor around a table.
                    // On NS and Firefox, offsetHeights for anchors are 0.
                    // We use the containing table to do positioning.
                    if (positioningObject.offsetHeight == 0) {
                        positioningObject = positioningObject.firstChild;
                    }
                    var newTop = Dom.absoluteTop(positioningObject) + positioningObject.offsetHeight;
                    newTop = Dom.correctForBottomEdge(newTop, divObject);
                    var newLeft = Dom.absoluteLeft(positioningObject);
                    newLeft = Dom.correctForRightEdge(newLeft, divObject);
                    Dom.setAbsolutePosition(divObject, newLeft, newTop);
                    Refresh.displayDiv(divObject);
                }
                else {
                    this.positionAndDisplayDiv(divObject, mevent);
                }
            }
            else {
                this.hideActiveMenu();
            }

        // if the div has an onDisplay handler defined, then call it
            var onDisplay = divObject.getAttribute("onDisplay");
            var onDisFunc;
            if (onDisplay && (onDisFunc = this[onDisplay])) {
                onDisFunc.call(this, divObject);
            }

            // cancelBubble is required because the document.onmousedown gets called after this method is called,
            // and would, of course, call awhideActiveMenu which hides the menu displayed by this method.
            // To support netscape, cancelBubble needs to be set at the end of this method. On netscape,
            // execution of this event listener is stopped as soon as cancelBubble is set to true.
            Event.cancelBubble(mevent);
            return false;
        },

        // see if menus doesn't fit on screen; if so make it two column
        checkMenuLayout : function (div, lazyDiv)
        {
            // if we've messed with this menu before, undo the damage
            if (div.firstChild.className == "awmenu2col") {
                var td1 = Dom.findChild(div, "TD");
                var l = Util._arrayAdd(Util._arrayAdd([], td1.childNodes), td1.nextSibling.childNodes);
                div.removeChild(div.firstChild);
                Dom._appendChildren(div, l);
            }

            // Flag to call us back if a lazy div expands within...
            div.lazyCallback = this.checkMenuLayout.bind(this);
        
            // see if we need to adjust size
            div.style.display = '';
            var maxHt = Dom.getWindowSize()[1] - 30;
            if (div.offsetHeight < maxHt) return;

            // Run through items looking for where we should split it
            // Note: we can't divide the contents of refresh regions, so this is imperfect
            // When we find item in "split zone" we search forward to see if we can include
            // its siblings under the currenct section head
            var targetHt = Math.round(div.offsetHeight / 2);
            var i, items = Util._arrayAdd([], div.childNodes), middle = 0, n;
            for (i = 0; i < items.length; i++) {
                n = items[i];
                if (n.nodeType == 1) {
                    var bot = Dom.elmBottom(n);
                    if (bot > targetHt && n.offsetTop > 30) {
                        var isHeading = Dom.hasClass(n, "awmenuHead");
                    // if we passed max we're done
                        if (bot > maxHt) {
                            // if we hadn't already hit target, then prev item is it
                            if (!middle) middle = i - 1;
                            break;
                        }
                    // we found a new heading after hitting target.  Stop here.
                        if (middle && isHeading) {
                            middle = i - 1;
                            break;
                        }

                        if (!middle) middle = i - (isHeading ? 1 : 0);
                    // We'll keep scanning forward looking for a heading to use as separator
                    }
                }
            }

            // IE6 crasher workaround: remove children before adding them elsewhere
            for (i = 0; i < items.length; i++) {
                n = items[i];
                n.parentNode.removeChild(n);
            }
            if (middle <= 0) {
                // couldn't find a split
                div.innerHTML = '';
                Dom._appendChildren(div, items);
            } else {
                // create two column table and populate with items
                div.innerHTML = '<table class="awmenu2col"><tr><td></td><td style="border:none"></td></tr></table>';
                var head = [];
                while (middle-- >= 0) head.push(items.shift())
                var td = Dom.findChild(div, "TD");
                Dom._appendChildren(td, head);
                Dom._appendChildren(td.nextSibling, items);
            }
        },

        menuKeyDown : function (menuCellDivLink, senderId, formId, mevent)
        {
            var returnVal = this.shouldHandleMenuKeyDown(menuCellDivLink, mevent);
            if (returnVal) {
                returnVal = this.menuClicked(menuCellDivLink, senderId, formId);
                this.hideActiveMenu();
            }
            return returnVal;
        },

        /*
            Called when the user types a key while in the popup menu
        */
        shouldHandleMenuKeyDown : function (menuCellDivLink, mevent)
        {
            var shouldHandle = false;
            var keyCode = Event.keyCode(mevent);

            var menu = this.menu(menuCellDivLink);
            var menuDivId = menu.id;
            var i, menuLinks;

            if (keyCode == 16) {
                // ignore shift button press
                shouldHandle = false;
            }
            else if (keyCode == 13) {
                shouldHandle = true;
            }
            else if (keyCode == 27) {
                shouldHandle = false;
            // Escape key
                this.hideActiveMenu();
            }
            else if (keyCode == 9) {
                // tab key
                var stopBubbling = false;
                if (mevent.shiftKey) {
                    if (menuCellDivLink == this.firstMenuLink(menuDivId)) {
                        stopBubbling = true;
                        shouldHandle = false;
                    }
                }
                else {
                    if (menuCellDivLink == this.lastMenuLink(menuDivId)) {
                        stopBubbling = true;
                        shouldHandle = false;
                    }
                }
                if (stopBubbling) {
                    Event.cancelBubble(mevent);
                }
            }
            else if (keyCode == 38) {
                // up arrow
                menuLinks = this.menuLinks(menuDivId);
                var prevLink = null;
                if (menuCellDivLink == menuLinks[0]) {
                    prevLink = menuLinks[menuLinks.length - 1];
                }
                for (i = menuLinks.length - 1; i > 0; i--) {
                    menuLink = menuLinks[i];
                    if (menuLink == menuCellDivLink) {
                        prevLink = menuLinks[i - 1];
                    }
                }
                Event.elementInvoke(menuCellDivLink, "blur");
                Event.elementInvoke(prevLink, "focus");
                prevLink.focus();
                Event.cancelBubble(mevent);
            }
            else if (keyCode == 40) {
                // down arrow
                menuLinks = this.menuLinks(menuDivId);
                var nextLink = null;
                if (menuCellDivLink == menuLinks[menuLinks.length - 1]) {
                    nextLink = menuLinks[0];
                }
                for (i = 0; i < menuLinks.length - 1; i++) {
                    var menuLink = menuLinks[i];
                    if (menuLink == menuCellDivLink) {
                        nextLink = menuLinks[i + 1];
                    }
                }
                Event.elementInvoke(menuCellDivLink, "blur");
                Event.elementInvoke(nextLink, "focus");
                nextLink.focus();
                Event.cancelBubble(mevent);
            }
            return shouldHandle;
        },
        /**
         Called when user makes a menuItem choice.
         */
        menuClicked : function (menuCellDiv, senderId, formId, target)
        {
            // pass the menuItemSender and linkId as comma delimited list
            var senderList = (this.AWLinkId == null) ? senderId : this.AWLinkId + "," + senderId;
            if (formId != null) {
                Request.submitFormForElementName(formId, senderList);
            }
            else {
                var url = Request.formatUrl(senderList);
                this.AWLinkId = null;
                Request.setDocumentLocation(url, target);
            }
            return false;
        },

        menuButtonOver : function (menuButtonDiv)
        {
            menuButtonDiv.className = "btnOnBrand";
            return false;
        },

        menuButtonOut : function (menuButtonDiv)
        {
            menuButtonDiv.className = "btnOffBrand";
            return false;
        },

        // Called when user mouse enters a menu cell
        cellMouseOver : function (divObject, evt)
        {
            this.hiliteDiv(divObject);
            return false;
        },

        cellMouseOut : function (divObject)
        {
            this.unhiliteDiv(divObject);
            return false;
        },

        handleClientTrigger : function (elm, evt)
        {
            var clientTrigger = elm.getAttribute('_ct');
            if (clientTrigger) {
                Event.handleInline(clientTrigger, evt, elm);
            }
        },

        ///////////
        // Alerts
        ///////////
        unsupportedBrowser : function ()
        {
            alert("PopupMenus not supported on this browser");
        },
        /////////////
        // Util
        /////////////

        positionAndDisplayDiv : function (divObject, mevent)
        {
            this.positionMenu(divObject, mevent);
        // call awdisplayDiv after awpositionMenu so that
            // menu is in final place before computing the intersections
            Dom.fadeInElement(divObject);
            Refresh.displayDiv(divObject);
        },    // widgets_ie.js

        EOF:0};

    // overrides on Widgets    
    Util.extend(Widgets, {
        hideActiveMenu : function ()
        {
            Menu.hideActiveMenu();
        }
    });

    //
    // IE - specific methods
    //
    if (Dom.IsIE) Util.extend(Menu, function () {
        // overrides on Widgets
        Util.extend(Refresh, {
            _reloMenu : function (menuDiv, overlayDiv)
            {
                // 1-1HNH9 - set active menu
                Menu.AWActiveMenu = menuDiv;
            // reposition and redisplay div.
                menuDiv.style.top = overlayDiv.style.top;
                menuDiv.style.left = overlayDiv.style.left;
                this.displayDiv(menuDiv);
            },

            preDisplayDiv : function (divObject)
            {
                if (divObject.className == 'awmenu') divObject.awOnOverlayUpdate = this._reloMenu.bind(this);
            }
        });

        return {
            ////////////////////
            // Menu Positioning
            ////////////////////
            menuTop_IE : function (menuDiv, mevent, docElement)
            {
                var srcElement = mevent.srcElement;
                var menuTop = Dom.absoluteTop(srcElement.offsetParent) + mevent.offsetY;
                // ensure div is not displaying off screen
                var adjustedMenuTop = Dom.correctForBottomEdge(menuTop, menuDiv);
                if (adjustedMenuTop == menuTop) {
                    menuTop -= this.AWMenuOffset;
                }
                else {
                    menuTop = adjustedMenuTop;
                }
                return menuTop;
            },

            menuLeft_IE : function (menuDiv, mevent, docElement)
            {
                var srcElement = mevent.srcElement;
                var menuLeft = Dom.absoluteLeft(srcElement.offsetParent) + mevent.offsetX;
                // ensure div is not displaying off screen
                var adjustedMenuLeft = Dom.correctForRightEdge(menuLeft, menuDiv);
                if (adjustedMenuLeft == menuLeft) {
                    menuLeft -= this.AWMenuOffset;
                }
                else {
                    menuLeft = adjustedMenuLeft;
                }
                return menuLeft;
            },

            positionMenu : function (menuDiv, mevent)
            {
                // must display first to get proper coordinates.
                menuDiv.style.display = '';
                var docElement = Dom.documentElement();
                var menuTop = this.menuTop_IE(menuDiv, mevent, docElement);
                var menuLeft = this.menuLeft_IE(menuDiv, mevent, docElement);
                Dom.setAbsolutePosition(menuDiv, menuLeft, menuTop);
            },

        EOF:0};

    }());

    // WE CAN PROBABLY JUST DELETE THIS...
    if (Dom.IsIEonMac) Util.extend(Menu, function () {
        // active popup div is a direct decendent of the body tag. Used to display popups
        // for browsers that do not fully support positioning of elements
        var AWActivePopupDiv = null;

        return {

            // OVERRIDE
            // This method handles the special case for IE 5 running on Mac OS.
            // It utilizes the popup div to display the menu contents instead of using
            // the menu div directly.
            menuLinkOnClick : function (positioningObject, menuName, linkId, mevent)
            {
                var menuDiv = Dom.getElementById(menuName);
                var popupDiv = Dom.getElementById('awpopupDiv');
                if (this.AWActiveMenu == null) {
                    this.hideActiveMenu();
                    this.AWLinkId = linkId;
                    this.AWActiveMenu = menuDiv;
                    AWActivePopupDiv = popupDiv;
                    Event.enableDocumentClick(this.hideActiveMenu.bind(this));
                    this.positionDisplayMacIE(menuDiv, popupDiv, mevent);
                }
                else {
                    this.hideActiveMenu();
                }
                Event.cancelBubble(mevent);
                return false;
            },

            // OVERRIDE
            hideActiveMenu : function ()
            {
                // handles the special case for IE running on Mac OS
                // uses the popup div to display the menu contents instead of the menu div
                if (AWActivePopupDiv != null) {
                    Event.disableDocumentClick();
                    Refresh.undisplayDiv(AWActivePopupDiv);
                }
                AWActivePopupDiv = null;
                this.AWActiveMenu = null;
            },

            positionDisplayMacIE : function (menuDiv, popupDiv, mevent)
            {
                this.positionMenuMacIE(menuDiv, popupDiv, mevent);
                // call awdisplayDiv after awpositionMenu so that
                // menu is in final place before computing the intersections
                Refresh.displayDiv(popupDiv);
            },

            positionMenuMacIE : function (menuDiv, popupDiv, mevent)
            {
                var left = mevent.clientX + Dom.documentElement().scrollLeft;
                var top = mevent.clientY + Dom.documentElement().scrollTop;
                if ((left - 15) >= 0) {
                    left = left - 15;
                }
                if ((top - 15) >= 0) {
                    top = top - 15;
                }

                // the menu HTML is wrapped in a table, row, and cell in order to prevent the
                // display div from running off of the page on IE5 MacOS.
                var newHTML = '<table><tr><td class="awmenuCell">' + menuDiv.innerHTML + '</td></tr></table>';

                // The zIndex is set very high to tell the layout engine not to process the rest
                // of the page.
                popupDiv.style.zIndex = 1000000;
                popupDiv.innerHTML = newHTML;
                // position must be set absolute in order for the following code to set
                // the left and top positions to work.
                popupDiv.style.position = 'absolute'
                popupDiv.style.left = left + 'px';
                popupDiv.style.top = top + 'px';
            },

        EOF:0};
    }());

    //
    // Mozilla - specific methods
    //
    if (!Dom.IsIE) Util.extend(Menu, function () {

        // var AWMenuBorderWidth = 2;

        return {
            /*
            // unused?
            isMouseInDiv_NS : function (divObject, mevent)
            {
                var docBody = document.body;
                var isMouseInMenu = false;
                var mouseX = mevent.pageX;
                var menuLeft = parseInt(divObject.style.left);
                // Note: no need to adjust for scrolling
                var menuRight = menuLeft + divObject.offsetWidth;
                if (mouseX > (menuLeft + AWMenuBorderWidth) &&
                    mouseX < (menuRight - AWMenuBorderWidth))
                {
                    var mouseY = mevent.pageY;
                    var menuTop = parseInt(divObject.style.top);
                    // Note: no need to adjust for scrolling
                    var menuBottom = menuTop + divObject.offsetHeight;
                    if (mouseY > (menuTop + AWMenuBorderWidth) &&
                        mouseY < (menuBottom - AWMenuBorderWidth))
                    {
                        isMouseInMenu = true;
                    }
                }
                return isMouseInMenu;
            },
           */
            positionMenu_NS6 : function (menu, mevent)
            {
                var styleObject = menu.style;

                var styleDisplay = styleObject.display;
                if (styleDisplay == 'none') {
                    styleObject.display = '';
                }

                var menuTop = mevent.pageY;
                var menuLeft = mevent.pageX;
                // get the offsetHeight and the offsetWidth values before changing the styleObject,
                // because the change of the top and the left in the styleObject may impact these values
                var menuHeight = menu.offsetHeight;
                var menuWidth = menu.offsetWidth;

                // ensure div is not displaying off screen
                styleObject.top = 0;
                var delta;
                var menuMax = mevent.clientY + menuHeight;
                if (menuMax > window.innerHeight) {
                    delta = menuMax - window.innerHeight;
                    menuTop = menuTop - delta;
                }
                menuTop = menuTop - this.AWMenuOffset;
                if (menuTop < 0) {
                    menuTop = 0;
                }

                styleObject.left = 0;
                menuMax = mevent.clientX + menuWidth;
                if (menuMax > window.innerWidth) {
                    delta = menuMax - window.innerWidth;
                    menuLeft = menuLeft - delta;
                }
                menuLeft = menuLeft - this.AWMenuOffset;
                if (menuLeft < 0) {
                    menuLeft = 0;
                }
                Dom.setAbsolutePosition(menu, menuLeft, menuTop);

                /*
                // Hack for a defect in the Netscape, commenting out for Firefox
                // Hacked number for the correct display of the popupmenu,
                // we should figure out where it comes from
                var AWMagicNumber = 6;

                var newWidth  = menuWidth - AWMagicNumber;
                styleObject.width  = newWidth + 'px';
                // we don't handle it for the height, until there is a problem for it
                var newHeight = menuHeight - 6;
                styleObject.height = newHeight + 'px';
                */
                styleObject.display = styleDisplay;
            },

            positionMenu : function (menu, mevent)
            {
                if (Dom.IsNS6) {
                    this.positionMenu_NS6(menu, mevent);
                }
                else {
                    this.unsupportedBrowser();
                }
            },

        EOF:0};
    }());

    Event.registerBehaviors({
        // PopupMenuLink
        PML : {
             click : function (elm, evt) {
                 var ret = true;
                 var pos = elm.getAttribute("_pos");
                 var target = (pos == null || pos == "this") ? elm : Dom.getElementById(pos);
                 ret = Menu.menuLinkOnClick(target, elm.getAttribute("_mid"), elm.id, evt);
                 return ret;
             },


             keydown : function (elm, evt) {
                 return Menu.menuLinkOnKeyDown(elm, elm.getAttribute("_mid"), elm.id, evt);
             }
         },

         // PMI - PopupMenuItem
         PMI_NoHover : {
             mousedown : function (elm, evt) {
                 var formId = Dom.boolAttr(elm, "_sf", true) ? Dom.lookupFormId(elm) : null;
                 Menu.handleClientTrigger(elm, evt);
                 if (Event.shouldBubble(evt)) {
                     return Menu.menuClicked(elm, elm.id, formId, elm.getAttribute("_t"));
                 }
                 return false;
             },

             keydown : function (elm, evt) {
                 var formId = Dom.boolAttr(elm, "_sf", true) ? Dom.lookupFormId(elm) : null;
                 Menu.handleClientTrigger(elm, evt);
                 if (Event.shouldBubble(evt)) {
                     return Menu.menuKeyDown(elm, elm.id, formId, evt, elm.getAttribute("_t"));
                 }
                 return false;
             }
         }
    });

    Event.registerBehaviors({
        // Popup Menu Item
        PMI : {
            prototype : Event.behaviors.PMI_NoHover,

            mouseover : function (elm, evt) {
                return Menu.cellMouseOver(elm);
            },


            mouseout : function (elm, evt) {
                return Menu.cellMouseOut(elm);
            },


            focus : function (elm, evt) {
                return Menu.cellMouseOver(elm);
            },


            blur : function (elm, evt) {
                return Menu.cellMouseOut(elm);
            }
        }
    });

    Event.registerBehaviors({
       // Overflow expand item
       PMIO : {
            prototype : Event.behaviors.PMI,

            mousedown : function (elm, evt) {
                var menu = Dom.findParentUsingPredicate(elm, function (n) {
                    return Dom.hasClass(n, "awmenu");
                });
                Dom.addClass(menu, "awmenuEx");
                Menu.checkMenuLayout(menu);
                Dom.repositionDivToWindow(menu);
                return Event.cancelBubble(evt);
            },

            keydown : function (elm, evt) {
                return Event.behaviors.PMIO.mousedown(elm, evt);
            }
        }
    });

    return Menu;
}();
