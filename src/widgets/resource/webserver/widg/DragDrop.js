/*
    DragDrop.js     -- functions / behaviors for enabling drag and drop
*/

ariba.DragDrop = function() {
    // imports
    var Event = ariba.Event;
    var Debug = ariba.Debug;
    var Request = ariba.Request;
    var Dom = ariba.Dom;

    // private vars
    var AWDropPrefix = "awDrp_";
    var AWDragPrefix = "awDrg_";
    var AWDragContentPrefix = "awDrgCnt_";
    var AWDragParent = "awDrgPrt";
    var AWDropStylePrefix = "awds_";

    // var gAWDragDropMetadata = new Array();
    var AWDragImage;
    var AWDragDiv;
    var AWPreviousDrop = new Object();
    //  var AWEventTarget;
    var AWDragDeniedImage;
    var AWDisableMouseClick = false;
    var AWDropStyle = "dropAreaSelected";
    var AWDragDelta = 10;
    var AWDCanceledDrag;

    var DragDrop = {
        MouseDownEvtHandler : null,

        getDiv : function ()
        {
            return AWDragDiv;
        },

        releaseDragDiv : function (isDropAction)
        {
            //if (!isDropAction) {
            //    var dragObject = AWDragDiv.dragObject;
            //    dragObject.style.filter = AWDragDiv.orig_styleFilter;
            //    dragObject.style.width = AWDragDiv.orig_styleWidth;
            //}
            // ### debug
            this.disallowZoneHightlight();
            this.hideDragScrollDebug();

            var src = Dom.getElementById(AWDragDiv.srcId);
            if (src) Event.elementInvoke(src, "dragend");

            document.body.removeChild(AWDragDiv);
            AWDragDiv = null;
        },

        showDragDiv : function ()
        {
            AWDragDiv.style.visibility = "visible";
            Dom.setOpacity(AWDragDiv, 50);
        //var dragObject = AWDragDiv.dragObject;
            //AWDragDiv.orig_styleFilter = dragObject.style.filter;
            //AWDragDiv.orig_styleWidth = dragObject.style.width;
            //dragObject.style.filter="alpha(opacity=050)";
            //dragObject.style.width = dragObject.offsetWidth;
        },

        allowZoneHightlight : function () {
            Dom.addClass(document.body, "onDrag");
        },

        disallowZoneHightlight : function () {
            Dom.removeClass(document.body, "onDrag");
        },

        clearPreviousDrop : function ()
        {
            if (AWPreviousDrop.container) {
                Dom.removeClass(AWPreviousDrop.container, AWPreviousDrop.style);
                AWPreviousDrop.container = null;
                AWPreviousDrop.style = null;
            }
        },

        createDragDiv : function (evt, dragObject, dragPrefix, dragId)
        {

            if (AWDragDiv) {
                // if there is an existing drag, then clear it out and do not
                // create the drag div.  this should only occur if the user is
                // currently dragging and uses the scroll wheel or other mouse
                // control to 'click'.  Rather than causing any confusion by actually
                // handling the click, we clear the drag and suppress all further
                // handling.
                DragDrop.clearDragDrop(evt);
                return null;
            }

            AWDragDiv = document.createElement("div");
            AWDragDiv.id = "AWDragDiv";
            document.body.appendChild(AWDragDiv);
            AWDragDiv.style.position = "absolute";
            AWDragDiv.style.zIndex = "249";
        //AWDragDiv.style.filter="alpha(opacity=050);";
            //AWDragDiv.style.filter= "progid:DXImageTransform.Microsoft.Shadow(color=#666666,direction=135,strength=8);"
            // ### for ns
            //AWDragDiv.-moz-opacity:0.6;
            // The 1 pixel is to avoid mouse events from firing on the drag indicator.
            AWDragDiv.initX = evt.clientX + 1;
            AWDragDiv.initY = evt.clientY + 1;
            AWDragDiv.style.visibility = "hidden";

            AWDragDiv.dragObject = dragObject;

            var classArray = dragObject.className.split(" ");
            for (var i = 0; i < classArray.length; i++) {
                if (classArray[i].indexOf(dragPrefix) == 0) {
                    AWDragDiv.dragType = classArray[i].substr(dragPrefix.length);
                }
            }
            AWDragDiv.srcId = dragId ? dragId : dragObject.id;
            var size = Dom.containerOffsetSize(dragObject);
            AWDragDiv.style.width = size[0] + "px";
            AWDragDiv.style.height = size[1];
            return AWDragDiv;
        },

        clearDragDrop : function (evt)
        {
            AWDCanceledDrag = false;
            if (AWDragDiv) {
                // reset -- invalid state
                // mouse -- down on drag item, out of window, up, over window, down
                this.releaseDragDiv();
            }

            this.clearPreviousDrop();
        },

        //---------------------------------------------------------
        // Drag drop event handlers
        //---------------------------------------------------------

        mouseDownEvtWrapper : function (target, evt)
        {
            //debug("handling event: " + target);
            var handled = false;
            evt = (evt) ? evt : event;
            target = (evt.target) ? evt.target : evt.srcElement;

            AWDCanceledDrag = false;
            // set up mouse down but continue to call application defined
            // mouse down event handler
            this.mouseDownEvtHandlerDrag(evt);

            if (arguments.length > 2) {
                var fun = arguments[2];
                var args = arguments[3];
                fun.apply(null, args);
            }

            return !handled;
        },

        mouseDownEvtHandler : function (evt)
        {
            // NOTE: debug in mouse up/down causes with mouse click event to not fire
            //debug("mouse down");
            var handled = false;
            evt = (evt) ? evt : event;
            // var target = (evt.target) ? evt.target : evt.srcElement;

            this.clearDragDrop(evt);

            return !handled;
        },

        mouseUpEvtHandler : function (evt)
        {
            // NOTE: debug in mouse up/down causes with mouse click event to not fire
            //debug("mouse up");
            var handled = false;
            evt = (evt) ? evt : event;
            // var target = (evt.target) ? evt.target : evt.srcElement;

            if (AWDragDiv) {
                if (AWDragDiv.style.visibility == "visible") {
                    AWDisableMouseClick = true;
                }

            // if we're dragging anything, let go of it
                this.releaseDragDiv();
                handled = true;
            }

            this.clearPreviousDrop();
            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        mouseMoveEvtHandler : function (evt)
        {
            //debug("aw mouse move");
            var handled = false;
            evt = (evt) ? evt : event;
            // var target = (evt.target) ? evt.target : evt.srcElement;

            if (AWDragDiv) {
                // check for movement and visibility
                if (this.dragDivMoved(evt)) {
                    this.allowZoneHightlight();
                }

                // someone is dragging something in a non-droppable area --
                // switch to non-drop
                AWDragDiv.droppable(false);

                // The 1 pixel is to avoid mouse events from firing on the drag div
                AWDragDiv.style.left = (evt.clientX + Dom.getPageScrollLeft() + 1) + "px";
                AWDragDiv.style.top = (evt.clientY + Dom.getPageScrollTop() + 1) + "px";
                handled = true;

                this.dragScroll(evt);
            }

            this.clearPreviousDrop();

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        shouldHandleMouseDown : function (evt)
        {
            var target = (evt.target) ? evt.target : evt.srcElement;
            return target.tagName == "SELECT";                    
        },

        mouseDownEvtHandlerDrag : function (evt)
        {
            // NOTE: debug in mouse up/down causes with mouse click event to not fire
            //debug("mouse down");
            if (this.shouldHandleMouseDown(evt)) return false;
            var handled = false;
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;

            AWDisableMouseClick = false;
            var div = Dom.findParentUsingPredicate(target, function(n) {
                return n.className && n.className.indexOf(AWDragPrefix) != -1;
            }, true);


            if (this.MouseDownEvtHandler == Event.getDocHandler("mousedown") && div) {
                //ariba.Debug.log("drag div found " + div.id);

                // save off the id in case we're swapping in our parent container as the
                // drag div
                var dragId = div.id;
                var dragPrefix = AWDragPrefix;

                if (div.className.indexOf(AWDragParent) != -1) {
                    var parent = Dom.findParentUsingPredicate(target, function(n) {
                        return n.className && n.className.indexOf(AWDragContentPrefix) != -1;
                    }, true);
                    if (parent) {
                        div = parent;
                        dragPrefix = AWDragContentPrefix;
                    }
                }

                var dragDiv = this.createDragDiv(evt, div, dragPrefix, dragId);
                if (dragDiv) {
                    // set the contents of the drag div -- seperate from awdCreateDragDiv since
                    // other draggable types may render their AWDragDiv differently (AWTDataTable)
                    // Debug.log("Drag innerHtml=" + div.tagName);
                    if (div.tagName == "TR") {
                        dragDiv.innerHTML = "<table>" + div.innerHTML + "</table>";
                    } else {
                        dragDiv.innerHTML = div.innerHTML;
                    }
                    dragDiv.style.border = "1px solid black";
                    dragDiv.style.backgroundColor = "#FFFFFF";

                    // default handler for change in drop status -- noop
                    dragDiv.droppable = function (isDroppable) {
                        if (isDroppable) {
                        }
                        else {
                        }
                    }

                    // capture the page height for drag scroll
                    dragDiv.pageHeight = Dom.documentElement().scrollHeight;
                    // Non-safari browsers takes the newly created div client height
                    // into account, so making Safari consistent.
                    // This browser side effect makes us able to drag scroll
                    // below the original page scroll height.
                    if (Dom.isSafari) {
                        dragDiv.pageHeight += dragDiv.clientHeight;
                    }
                    dragDiv.pageWidth = Dom.documentElement().scrollWidth;

                    this.clearPreviousDrop();

                    Event.elementInvoke(div, "dragstart");
                    handled = true;
                }
            }

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        mouseUpEvtHandlerDrag : function (evt)
        {
            // NOTE: debug in mouse up/down causes with mouse click event to not fire
            //debug("drag mouse up");
            var handled = false;
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;

            if (AWDragDiv) {
                if (AWDragDiv.style.visibility != "visible") {
                    // if we're not active yet, then just clean up
                    this.releaseDragDiv();
                }
                else {
                    AWDisableMouseClick = true;
                // active drag so initiate drop
                    var dropContainer = this.findDropContainer(target, AWDropPrefix);
                    handled = this.handleDropAction(dropContainer, evt, AWDropPrefix);
                }
            }

        //debug("handled mouseup: " + handled);

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        handleDropAction : function (dropContainer, evt, dropPrefix)
        {
            var handled = false;
            if (this.isDropContainerValid(dropContainer, dropPrefix)) {
                var senderId = AWDragDiv.srcId + "," + dropContainer.id;
                Request.invoke(dropContainer, senderId, evt);
                this.releaseDragDiv(true);
                handled = true;  // should this be drive by return value of awInvoke?
            }
            return handled;
        },

        removeFormField : function (formObject, fieldId)
        {
            var inputObject = formObject[fieldId];
            formObject.removeChild(inputObject);
        },

        findDropContainer : function (target, dropPrefix)
        {
            return Dom.findParentUsingPredicate(target, function(n) {
                return n.className && n.className.indexOf(dropPrefix) != -1;
            }, true);
        },

        dragActive : function ()
        {
            return (AWDragDiv && AWDragDiv.style.visibility == "visible");
        },

        dragDivMoved : function (evt)
        {
            if (this.dragActive()) return true;

            var moved = false;
            var currX = evt.clientX;
            var currY = evt.clientY;
            if (currX < AWDragDiv.initX) {
                moved = (AWDragDiv.initX - currX > AWDragDelta);
            }
            else {
                moved = (currX - AWDragDiv.initX > AWDragDelta);
            }

            if (!moved) {
                if (currY < AWDragDiv.initY) {
                    moved = (AWDragDiv.initY - currY > AWDragDelta);
                }
                else {
                    moved = (currY - AWDragDiv.initY > AWDragDelta);
                }
            }

            if (moved) {
                if (AWDragDiv.style.visibility != "visible") {
                    // just started moving it so set it visible
                    this.showDragDiv();
                }
            }
            return moved;
        },

        isDropContainerValid : function (dropContainer, dropPrefix)
        {
            if (!AWDragDiv.dragType) {
                // no drag type specified so only droppable in drop containers with no types
                return dropContainer.className.indexOf(dropPrefix) == -1;
            }

            var classArray = dropContainer.className.split(" ");
            for (var i = 0; i < classArray.length; i++) {
                if (classArray[i] && classArray[i].indexOf(dropPrefix) == 0) {
                    var dropType = classArray[i].substr(dropPrefix.length);
                    if (dropType == AWDragDiv.dragType) {
                        return true;
                    }
                }
            }
            return false;
        },

        dropContainerStyle : function (dropContainer)
        {
            var dropStyle = AWDropStyle;
            if (dropContainer.className) {
                var classArray = dropContainer.className.split(" ");
                for (var i = 0; i < classArray.length; i++) {
                    if (classArray[i].indexOf(AWDropStylePrefix) == 0) {
                        dropStyle = classArray[i].substr(AWDropStylePrefix.length);
                        break;
                    }
                }
            }
            return dropStyle;
        },

        mouseMoveEvtHandlerDrag : function (evt)
        {
            //debug("mouse move");
            var handled = false;
            evt = (evt) ? evt : event;
            var target = (evt.target) ? evt.target : evt.srcElement;

            if (AWDragDiv && this.dragDivMoved(evt)) {
                // someone is dragging something
                this.allowZoneHightlight();
                var dropContainer = this.findDropContainer(target, AWDropPrefix);
                if (this.isDropContainerValid(dropContainer, AWDropPrefix)) {
                    // set droppable
                    AWDragDiv.droppable(true);

                // position drag indicator
                    // The 1 pixel is to avoid mouse events from firing on the drag indicator
                    AWDragDiv.style.left = (evt.clientX + Dom.getPageScrollLeft() + 1) + "px";
                    AWDragDiv.style.top = (evt.clientY + Dom.getPageScrollTop() + 1) + "px";
                //(AWDragDiv.clientHeight/2)
                    //(AWDragDiv.clientWidth/2)

                    // highlight drop container
                    if (!AWPreviousDrop.container || AWPreviousDrop.container != dropContainer) {
                        this.clearPreviousDrop();
                    // check if drop container has an override style
                        var dropStyle = this.dropContainerStyle(dropContainer);
                        Dom.addClass(dropContainer, dropStyle);

                        AWPreviousDrop.container = dropContainer;
                        AWPreviousDrop.style = dropStyle;
                    }


                    handled = true;
                }

                this.dragScroll(evt);

            }

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        hideDragScrollDebug : function ()
        {
            var topDebugDiv = Dom.getElementById("awdDragScrollTop");
            if (topDebugDiv) {
                topDebugDiv.style.visibility = "hidden";
            }
            var bottomDebugDiv = Dom.getElementById("awdDragScrollBottom");
            if (bottomDebugDiv) {
                bottomDebugDiv.style.visibility = "hidden";
            }
            var leftDebugDiv = Dom.getElementById("awdDragScrollLeft");
            if (leftDebugDiv) {
                leftDebugDiv.style.visibility = "hidden";
            }
            var rightDebugDiv = Dom.getElementById("awdDragScrollRight");
            if (rightDebugDiv) {
                rightDebugDiv.style.visibility = "hidden";
            }
        },

        showDragScrollDebug : function (scrollDragHeight, scrollDragWidth)
        {
            var topDebugDiv = Dom.getElementById("awdDragScrollTop");
            if (!topDebugDiv) {
                topDebugDiv = document.createElement("div");
                topDebugDiv.id = "awdDragScrollTop";
                topDebugDiv.style.border = "1px red solid";
                topDebugDiv.style.position = "absolute";
                topDebugDiv.style.width = (Dom.documentElement().clientWidth - 2) + "px";
                topDebugDiv.style.height = scrollDragHeight + "px";
                document.body.appendChild(topDebugDiv);
            }
            topDebugDiv.style.left = Dom.getPageScrollLeft() + "px";
            topDebugDiv.style.top = Dom.getPageScrollTop() + "px";
            topDebugDiv.style.visibility = "visible";

            var bottomDebugDiv = Dom.getElementById("awdDragScrollBottom");
            if (!bottomDebugDiv) {
                bottomDebugDiv = document.createElement("div");
                bottomDebugDiv.id = "awdDragScrollBottom";
                bottomDebugDiv.style.border = "1px red solid";
                bottomDebugDiv.style.position = "absolute";
                bottomDebugDiv.style.width = (Dom.documentElement().clientWidth - 2) + "px";
                bottomDebugDiv.style.height = scrollDragHeight + "px";
                document.body.appendChild(bottomDebugDiv);
            }
            bottomDebugDiv.style.left = Dom.getPageScrollLeft() + "px";
            bottomDebugDiv.style.top = (Dom.getPageScrollTop() + Dom.documentElement().clientHeight - scrollDragHeight) + "px";
            bottomDebugDiv.style.visibility = "visible";

            var leftDebugDiv = Dom.getElementById("awdDragScrollLeft");
            if (!leftDebugDiv) {
                leftDebugDiv = document.createElement("div");
                leftDebugDiv.id = "awdDragScrollLeft";
                leftDebugDiv.style.border = "1px red solid";
                leftDebugDiv.style.position = "absolute";
                leftDebugDiv.style.width = scrollDragWidth + "px"
                leftDebugDiv.style.height = (Dom.documentElement().clientHeight - 2) + "px";
                document.body.appendChild(leftDebugDiv);
            }
            leftDebugDiv.style.top = Dom.getPageScrollTop() + "px";
            leftDebugDiv.style.left = Dom.getPageScrollLeft() + "px";
            leftDebugDiv.style.visibility = "visible";

            var rightDebugDiv = Dom.getElementById("awdDragScrollRight");
            if (!rightDebugDiv) {
                rightDebugDiv = document.createElement("div");
                rightDebugDiv.id = "awdDragScrollRight";
                rightDebugDiv.style.border = "1px red solid";
                rightDebugDiv.style.position = "absolute";
                rightDebugDiv.style.width = scrollDragWidth + "px"
                rightDebugDiv.style.height = (Dom.documentElement().clientHeight - 2) + "px";
                document.body.appendChild(rightDebugDiv);
            }
            rightDebugDiv.style.top = Dom.getPageScrollTop() + "px";
            rightDebugDiv.style.left = (Dom.getPageScrollLeft() + Dom.documentElement().clientWidth - scrollDragWidth) + "px";
            rightDebugDiv.style.visibility = "visible";
        },

        dragScroll : function (evt)
        {
            if (!AWDragDiv) {
                return;
            }
            var scrollDragHeight = 100;
            var scrollDragWidth = 100;

        // ### debug
            if (evt.ctrlKey) {
                this.showDragScrollDebug(scrollDragHeight, scrollDragWidth);
            }
            else {
                this.hideDragScrollDebug();
            }

        // drag scroll
            var clientHeight = Dom.documentElement().clientHeight;
            var pageHeight = AWDragDiv.pageHeight;
            var scrollTop = Dom.getPageScrollTop();
        // scroll up
            if (evt.clientY < 100 && scrollTop != 0) {
                //debug("clientY: "+ evt.clientY + " scrollTop: " + scrollTop);

                var move = (evt.clientY == 0) ? scrollTop : ((100 - evt.clientY) / 100) * scrollTop;
                Dom.setPageScrollTop(scrollTop - move);
            }
                // scroll down
            else if (clientHeight - evt.clientY < 100 &&
                     (pageHeight - (scrollTop + evt.clientY)) > 5) {

                var distance = clientHeight - evt.clientY;
                var maxMove = pageHeight - clientHeight - scrollTop;
                var move = (evt.clientY == clientHeight) ? maxMove : ((100 - distance) / 100) * maxMove;

            //debug("y: " + evt.clientY + " " + Dom.documentElement().clientHeight + " " + Dom.documentElement().scrollHeight + " " + Dom.getPageScrollTop());
                //debug("clientHeight-evt.clientY" + (clientHeight - evt.clientY) +
                //      " pageHeight" + pageHeight +
                //      " current absolute pos:" + (scrollTop + evt.clientY) +
                //      " distance: " + distance +
                //      " maxMove: " + maxMove +
                //      " move: " + move);

                Dom.setPageScrollTop(scrollTop + move);
            }

        // scroll left
            var clientWidth = Dom.documentElement().clientWidth;
            var pageWidth = AWDragDiv.pageWidth;
            var scrollLeft = Dom.getPageScrollLeft();
        // scroll up
            if (evt.clientX < 100 && scrollLeft != 0) {
                //debug("clientY: "+ evt.clientY + " scrollTop: " + scrollTop);

                var move = (evt.clientX == 0) ? scrollLeft : ((100 - evt.clientX) / 100) * scrollLeft;
                Dom.setPageScrollLeft(scrollLeft - move);
            }
                // scroll right
            else if (clientWidth - evt.clientX < 100 &&
                     (pageWidth - (scrollLeft + evt.clientX)) > 5) {

                var distance = clientWidth - evt.clientX;
                var maxMove = pageWidth - clientWidth - scrollLeft;
                var move = (evt.clientX == clientWidth) ? maxMove : ((100 - distance) / 100) * maxMove;

                Dom.setPageScrollLeft(scrollLeft + move);
            }
        },

        onClickEvtHandler : function (evt)
        {
            evt = (evt) ? evt : event;
            // var target = (evt.target) ? evt.target : evt.srcElement;
            var handled = false;

            if (handled) {
                Event.cancelBubble(evt);
            }
            return !handled;
        },

        //
        // drag drop metadata / image registration
        //

        /*
        registerMetadata : function (element, dragdropdata)
        {
            gAWDragDropMetadata[element.id] = dragdropdata;
            element.onmouseover = null;
            element.isSelectable = true;
            element.isDroppable = true;
        },*/

        registerDragIcon : function (id, imageId)
        {
            Debug.log("registering image: " + id + " " + Dom.getElementById(imageId));
            if (!AWDragImage) {
                AWDragImage = new Object();
            }
            AWDragImage[id] = Dom.getElementById(imageId);
        },

        registerDropDeniedIcon : function (imageId)
        {
            AWDragDeniedImage = Dom.getElementById(imageId);
        },

        EOF:0};

    // Register Behaviors
    Event.registerBehaviors({
        // Drag container
        DrG : {
            // need to assign handler directly to avoid text selection on IE
            mouseover : function (elm, evt) {
                elm.onmousedown = DragDrop.mouseDownEvtHandlerDrag.bindEventHandler(DragDrop);
            }
        },

        // Drop container
        DrP : {
            mouseup : function (elm, evt) {
                return DragDrop.mouseUpEvtHandlerDrag(evt);
            },

            mousemove : function (elm, evt) {
                return DragDrop.mouseMoveEvtHandlerDrag(evt);
            },

            click  :  function (elm, evt) {
                return DragDrop.onClickEvtHandler(evt);
            }
        },

        DrGP : {
            // need to assign handler directly to avoid text selection on IE
            mouseover : function (elm, evt) {
                elm.onmousedown = DragDrop.mouseDownEvtHandlerDrag.bindEventHandler(DragDrop);
            },

            mouseup : function (elm, evt) {
                return DragDrop.mouseUpEvtHandlerDrag(evt);
            },

            mousemove : function (elm, evt) {
                return DragDrop.mouseMoveEvtHandlerDrag(evt);
            },

            click  :  function (elm, evt) {
                return DragDrop.onClickEvtHandler(evt);
            }
        }
    });

    // Initialization
    DragDrop.MouseDownEvtHandler = DragDrop.mouseDownEvtHandler.bindEventHandler(DragDrop);
    Event.updateDocHandler("mousedown", DragDrop.MouseDownEvtHandler);
    Event.updateDocHandler("mouseup", DragDrop.mouseUpEvtHandler.bindEventHandler(DragDrop));
    Event.updateDocHandler("mousemove", DragDrop.mouseMoveEvtHandler.bindEventHandler(DragDrop));

    return DragDrop;
}();
