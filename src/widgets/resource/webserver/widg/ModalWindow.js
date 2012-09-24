/*
    ModalWindow.js

    Support for full browser windows that behave as modal panels relative to the main
    browser window.
*/

ariba.ModalWindow = function() {
    // imports
    var Event = ariba.Event;
    var Debug = ariba.Debug;
    var Input = ariba.Input;
    var Request = ariba.Request;
    var Dom = ariba.Dom;

    var WindowClosingRedirectUrl;

    // private vars
    var AWModalWindowParent = null;
    var AWDocumentCover = null;
    var AWModalWindowChild = null;

    var ModalWindow = {

        setWindowClosingRedirectUrl : function (url)
        {
            WindowClosingRedirectUrl = url;
        },

        convertWindowName : function (name)
        {
            // semi-hack to get rid of illegal dollar sign in name
            name = name.replace('$', 'D');
            return name.replace('$', 'D');
        },

        modalWindowId : function (parentWindowId)
        {
            var modalWindowId = "aw" + parentWindowId;
            if (Request.AWDebugEnabled) {
                if (window.name == "AWDebugModalWindow") {
                    modalWindowId = window.name + "NESTED";
                }
                else {
                    modalWindowId = "AWDebugModalWindow";
                }
            }
            return this.convertWindowName(modalWindowId);
        },

        openModalWindow : function (parentWindowId, tile, width, height)
        {
            if (AWModalWindowParent != null) {
                alert('Error -- modal window already open');
            }

            AWModalWindowParent = new Object();
            AWModalWindowParent.id = parentWindowId;
            var newWindowName = this.modalWindowId(parentWindowId);

            // check the size of the parent window and current screen size
            //var screenWidth = screen.availWidth;
            //var screenHeight = screen.availHeight;
            var screen = window.screen;
            var screenWidth = screen.availWidth;
            var screenHeight = screen.availHeight;
            var targetWidth = 0;
            var targetHeight = 0;
            var top = 0;
            var left = 0;

            if (tile) {

                var windowDim = Dom.getWindowSize();
                //var top = self.screenTop?self.screenTop:self.screenY;
                //var left = self.screenLeft?self.screenLeft:self.screenX;
                //debug("parent window -- width:" + windowDim[0] +
                //      " height:" + windowDim[1] +
                //      " scr w/h:" + screenWidth +"/"+screenHeight +
                //      " position: " + left + " " + top);

                // change parent window size -- only resize width -- and remember the delta
                var browserWidthDelta =
                        (screenWidth * .65) - windowDim[0] - 15;
                AWModalWindowParent.widthDelta = browserWidthDelta;
                window.resizeBy(browserWidthDelta, 0);
                window.moveTo(0, 0);

                // size for child window
                targetWidth = screenWidth * .35 - 10;
                targetHeight = screenHeight;

                left = screenWidth - targetWidth;

            }
            else {
                // width: a value < 1 is treated as a fraction of the window width
                targetWidth = width <= 1 ? screenWidth * width : width;

                // height: a value < 1 is treated as a fraction of the window height
                targetHeight = height <= 1 ? screenHeight * height : height;

                top = 40;
                left = 40;
            }

            //debug("Target: " + targetWidth + "x" + targetHeight);
            //debug("Screen: " + screenWidth  + "x" + screenHeight);

            // ### make top / left browser compatible
            var newWindowAttributes =
                    "toolbar=no,location=no,directories=no,status=no,scrollbars=yes,menubar=no,resizable=yes" +
                    ",width=" + targetWidth + ",height=" + targetHeight + ",top=" + top + ",left=" + left;

            AWDocumentCover = Input.coverDocument(1000, 50);
            AWModalWindowParent.onResizeOrig = window.onresize;
            AWModalWindowParent.onFocusOrig = window.onfocus;
            window.onresize = this.modalWindowOnResize.bind(this);
            window.onfocus = function () {
                // call focusOnChild out-of-band to increase
                // the odds that it works (20msec is arbitrary).
                setTimeout(ModalWindow.focusOnChild.bind(ModalWindow), 20);
            }


            var urlString = Request.formatUrl(AWModalWindowParent.id);
            AWModalWindowChild = Dom.openWindow(urlString, newWindowName, newWindowAttributes);
            if (AWModalWindowChild) {
                AWModalWindowChild.focus();
                AWModalWindowChild.resizeTo(targetWidth, targetHeight);

                AWDocumentCover.onmousedown = this.focusOnChild.bind(this);
            }
        },

        saveParentWindowData : function (elementId)
        {
            if (!AWModalWindowParent) {
                AWModalWindowParent = new Object();
                AWModalWindowParent.id = elementId;

                AWModalWindowParent.onResizeOrig = window.onresize;
                AWModalWindowParent.onFocusOrig = window.onfocus;
                window.onresize = this.modalWindowOnResize.bind(this);
                window.onfocus = function () {
                    // call focusOnChild out-of-band to increase
                    // the odds that it works (20msec is arbitrary).
                    setTimeout(ModalWindow.focusOnChild.bind(ModalWindow), 20);
                }
            }
        },

        /**
         * Event handler for the "covered" window -- keeps the cover div the right size.
         */
        modalWindowOnResize : function (mevent)
        {
            mevent = mevent ? mevent : event;
            Input.updateCoverSize(AWDocumentCover);
            Event.cancelBubble(mevent);
            return false;
        },

        resyncModalWindow : function (elementId)
        {
            var newWindowName = this.modalWindowId(elementId);

            Debug.log("awResyncModalWindow: " + AWDocumentCover);
            if (!AWDocumentCover) {
                // make an assumption that the doc cover div being gone implies a FPR has
                // occured.  Need to resync parent with modal.
                Debug.log("awResyncModalWindow recreating cover");
                AWDocumentCover = Input.coverDocument(1000, 50);
                AWDocumentCover.onmousedown = this.focusOnChild.bind(this);

                this.saveParentWindowData(elementId);
            }

            AWModalWindowChild = Dom.openWindow('', newWindowName);
            if (AWModalWindowChild) {
                AWModalWindowChild.focus();
            // should check here that modal window has content in it
            }
        },

        baseWindowRefresh : function ()
        {
            var urlString = Request.formatUrl(AWModalWindowParent.id);

            // todo: need to signal to stick "sync up with modal window" hint on this URL
            Request.setDocumentLocation(urlString + "&awModalWindowId=" + AWModalWindowParent.id);
        },

        isModalInProgress : function ()
        {
            return AWModalWindowChild != null;
        },

        /**
         *  This is called by javascript from the modal window -- executes in the parent window
         *  -- when the modal session is complete
         */
        modalComplete : function ()
        {
            Input.uncoverDocument(AWDocumentCover);
            AWDocumentCover = null;

            // set resize handler to null, resize the parent window back to original
            // size and then reestablish original resize handler
            Debug.log("Resize window width by: " + (-AWModalWindowParent.widthDelta));

            window.onresize = null;
            window.resizeBy(-AWModalWindowParent.widthDelta, 0);
            window.onfocus = null;
            //window.onresize = AWModalWindowParent.onResizeOrig;
            //window.onfocus = AWModalWindowParent.onFocusOrig;

            var urlString = Request.formatUrl(AWModalWindowParent.id);
            urlString += "&awshouldClose=1";

            AWModalWindowChild = null;
            AWModalWindowParent = null;

            Request.setDocumentLocation(urlString, null);
        },

        // Split this out as separate function to workaround bug where
        // sometimes the focus() call doesn't work.  Allow this to be called
        // via setTimeout so we can increase the odds that it works.
        focusOnChild : function ()
        {
            try {
                if (AWModalWindowChild != null) {
                    if (AWModalWindowChild.closed) {
                        this.modalComplete();
                    }
                    else{
                        AWModalWindowChild.focus();
                    }
                }
            }
            catch (e) {
                // if we're unable to focus on the child, then simply close the modal
                // window session -- this can happen if the child window is closed but
                // the event is not caught by the modal window framework and the
                // AWModalWindowChild is actually a reference to a non-existent window.
                this.modalComplete();
            }
        },

        rehideSelects : function ()
        {
            if (AWDocumentCover) {
                Input.hideSelects();
            }
        },

        closeModalWindow : function ()
        {
            // recursively notify all children
            if (AWModalWindowChild != null) {
                AWModalWindowChild.closeModalWindowChild();
            }
            this.notifyServerWindowIsClosing();
            if (window.opener != null && window.opener.baseWindowRefresh != null) {
                window.opener.baseWindowRefresh(false);
            }
        },

        closeModalWindowChild : function ()
        {
            // prevent this.closeModalWindow() from being called
            window.onbeforeunload = null;
            if (AWModalWindowChild != null) {
                AWModalWindowChild.closeModalWindowChild();
            }
        // this function is implemented in an awl file.
            this.notifyServerWindowIsClosing();
            window.close();
        },

        notifyServerWindowIsClosing : function ()
        {
            window.location.href = WindowClosingRedirectUrl;
        },

        EOF:0};

    Event.registerBehaviors({
        // HyperLink
        MWL : {
            mousedown :  function (elm, evt) {
                // disable other mousedown events (ie, from PopupMenuItem)
                // can't call open window because
                // it throws an access denied exception
                // see http://support.microsoft.com/kb/904947
                Event.cancelBubble(evt);
            },
            click :  function (elm, evt) {
                var parentWindowId = elm.getAttribute('_wn');
                var tile = Dom.boolAttr(elm, '_tw');
                var width = elm.getAttribute('_w');
                var height = elm.getAttribute('_h');
                return ariba.ModalWindow.openModalWindow(parentWindowId, tile, width, height);
            }
        }
    });

    /////////////////////////
    // Window Close Handling
    /////////////////////////
    window.onbeforeunload = function (mevent) {
        mevent = mevent ? mevent : event;
        // if the mouse click came from above the document are of the window
        // (ie in the title bar) then we assume its from the close button, which
        // makes sense since its the only thing in the title bar.
        if (0 > mevent.clientY) {
            ModalWindow.closeModalWindow();
        }
    }
    return ModalWindow;
}();

