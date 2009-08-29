var _editor_url;
var _editor_skin = "ariba";
var XinhaNoOp = function () {};
var FullScreenPatched = false;
var XinhaStrings;

function patchXinha ()
{
    // override style for the iframe
    var xinhaAddCoreCSS = Xinha.addCoreCSS;
    Xinha.addCoreCSS = function(html) {
        var coreCSS =
            '<style type=\"text/css\">'
                + "p {margin: 0px;} \n"
                + "body {margin: 2px;font:normal 11px Verdana, Arial, Helvetica, sans-serif;} \n"
                +"</style>\n";
        if(html && /<head>/i.test(html)) {
            html = html.replace(/<head>/i, '<head>' + coreCSS);
        }
        else if ( html) {
            html = coreCSS + html;
        }
        else {
            html = coreCSS;
        }
        return xinhaAddCoreCSS(html);
    }

    Xinha._lc_catalog = XinhaStrings;
    // we load plugins ourselves, so always return true;
    Xinha.loadPlugins = function(){ return true; };

    // get these into into Xinha Core code
    // leaks on incremental update
    Xinha.prependDom0Event = XinhaNoOp;
    // handlers not detached correctly
    var xinhaAddEvent = Xinha._addEvent;
    Xinha._addEvent = function(el, evname, func)
    {
        if (evname != "drag") {
            return xinhaAddEvent(el, evname, func);
        }
    }
    var xinhaAddDom0Event = Xinha.addDom0Event;
    Xinha.addDom0Event = function(el,ev,fn)
    {
        if (ev != "resize" && ev != "unload") {
            return xinhaAddDom0Event(el, ev, fn);
        }
    }
}


/*
    IE's "RemoveFormat" command does not remove the style attribute from span tags.
    So implementating our own remove format and adding a confirmation dialog.
    Still needs to support format removal from a selection range.
*/
function xinhaRemoveFormatting (editor) {

    if (confirm(Xinha._lc("Are you sure you want to remove all formatting?"))) {
        var D = editor.getInnerHTML();
        // retain only new lines
        D=D.replace(/<p[^>]*>(.*?)<\/p>/gi,"!!p!!$1!!p!!");
        D=D.replace(/<br *\/?>/gi,"!!br!!");
        // We want to remove comments, iframes, scripts, styles
        // and all of their contents entirely
        var regex = "(<!--[\\s\\S]*?-->)";
        regex += "|(<iframe>[\\s\\S]*?<\\/iframe>)";
        regex += "|(<script>[\\s\\S]*?<\\/script>)";
        regex += "|(<style>[\\s\\S]*?<\\/style>)";
        // We also want to remove all remaining HTML tags
        regex += "|(<[^>]*?>)";
        var regexObject = new RegExp(regex, "gi");
        D=D.replace(regexObject,"");
        D=D.replace(/!!p!!(.*?)!!p!!/gi,"<p>$1</p>");
        D=D.replace(/!!br!!/gi,"<br/>");
        editor.setHTML(D);
        editor.execCommand("removeformat");
        editor.updateToolbar();
    }
};

function createXinhaConfig ()
{
    var toolbarItems =
        ["popupeditor", "bold", "italic", "underline", "insertorderedlist", "insertunorderedlist", "fontsize", "fontname", "forecolor", "hilitecolor", "removeformat"];
    var config = new Xinha.Config();
    config.toolbar=[toolbarItems];
    config.statusBar = false;
    config.getHtmlMethod = "TransformInnerHTML";
    config.fullScreenMargins = [30,30,30,30];
    config.popupURL = "";
    config.fontname={
        "&mdash; font &mdash;":"",
        "Arial":"arial,helvetica,sans-serif",
        "Courier New":"courier new,courier,monospace",
        "Georgia":"georgia,times new roman,times,serif",
        "Tahoma":"tahoma,arial,helvetica,sans-serif",
        "Times New Roman":"times new roman,times,serif",
        "Verdana":"verdana,arial,helvetica,sans-serif",
        "Impact":"impact",
        "WingDings":"wingdings",
        "MS UI Gothic":"MS UI Gothic,Tahoma,SimSun,sans-serif"
        };
    config.btnList.removeformat[3] = function(e){
            xinhaRemoveFormatting(e);
        };
    return config;
}

function patchFullScreen () {
    if (!FullScreenPatched) {
        FullScreenPatched = true;
        Xinha.prototype.xinhaFullScreen = Xinha.prototype._fullScreen;
        Xinha.prototype._fullScreen = function()
        {
            this.xinhaFullScreen();
            if (Xinha.is_gecko) this._htmlArea.style.border = '';
        }
    }
}

// Hash of editors currently displayed
var RichTextEditors = new Object();
var RichTextEditorsSize = 0;

// List of editors to be displayed
var RefreshRichTextEditorIds = new Array();

function createRTA (id)
{
    var config = createXinhaConfig();
    var editor = new Xinha(id, config);

    patchXinhaInstance(editor);

    RichTextEditors[id] = editor;
    RichTextEditorsSize++;

    function postRTALoad () {
        patchFullScreen();
        var wrapper =
            ariba.Dom.findParentUsingPredicate(editor._textArea, function (e) {
                    return ariba.Dom.hasClass(e, "rtaWrapper");
            });
        //For IE6, when a modal dialog is displayed the underlying 
        //select elements have to be disabled. If there are any edits
        //to the richtext editor, it gets refreshed and selects get enabled.
        //This method call with disable the select elements on IE6 if there
        //is a modal window displayed.
        ariba.Input.hideSelects(true);
        wrapper.style.visibility = "visible";

        // For Ariba Selenium test automation
        var awname = editor._textArea.getAttribute("awname");
        if (awname) {
            // A unique identifier on the body element within the iframe so
            // Selenium can find it on playback
            editor._doc.body.setAttribute("awname", "XinhaBody:" + awname);
            editor._doc.awXinhaId = editor._textArea.id;
            Xinha._addEvent(editor._doc, "blur", blurRTAHandler);
        }
    }

    var width = editor._textArea.offsetHeight;
    if (width == 0) {
        // Width/height is 0 if RTA is in a panel.
        // In that case, deferred sizing
        function deferredGenerate () {
            editor.generate();
            editor.whenDocReady(postRTALoad);
            // When it's done the doc can be ready
            editor.whenDocReady(ariba.Event.notifyDocReady.bind(ariba.Event));
        };
        // Delay the doc ready until Xinha is done loading
        ariba.Event.docReadyIncrementNesting();
        ariba.Event.eventEnqueue(deferredGenerate, null, true);
    }
    else {
        // Delay the refresh complete until Xinha is done loading
        ariba.Event.refreshIncrementNesting();
        editor.generate();
        editor.whenDocReady(postRTALoad);
        // When it's done the refresh can complete
        editor.whenDocReady(ariba.Event.notifyRefreshComplete.bind(ariba.Event));
    }
}

function blurRTAHandler (event)
{
    // target should be the HtmlDocument object where we stashed the ID earlier
    var doc = ariba.Event.eventSourceElement(event);
    var editor = Xinha.getEditor(doc.awXinhaId);

    // Now dispatch a change event on the textarea.  A change event on 
    // the document would make more sense, but Selenium isn't listening 
    // for that and it won't bubble up out of its own document to the
    // main document.
    var ev = doc.createEvent("HTMLEvents");
    ev.initEvent("change", true, true);
    editor._textArea.dispatchEvent(ev);
    return true;
}

// get this into into Xinha Core code
function patchXinhaInstance (editor)
{
    // prevent xinha from injecting form submission code
    editor._textArea.form.xinha_submit = true;
}

function initRTA (id)
{
    createRTA(id);
}

function initRTARO (id)
{
    var textArea = ariba.Dom.getElementById(id);
    var textDiv = document.createElement("div");
    textDiv.style.width = textArea.offsetWidth + 'px';
    textDiv.style.height = textArea.offsetHeight + 'px';
    textDiv.className = "rtd";
    textDiv.innerHTML = textArea.value;
    textArea.parentNode.replaceChild(textDiv,textArea);
}

function staleRTADiv ()
{
    var div = ariba.Dom.getElementById("StaleRTEDiv");
    if (!div) {
        div = document.createElement("div");
        div.id = "StaleRTEDiv";
        div.className = "hide";
        document.body.appendChild(div);
    }
    return div;
}

function registerRefreshRTEId(id)
{
    RefreshRichTextEditorIds.unshift(id);
}

function removeRTAs ()
{
    for (var id in RichTextEditors) {
        var shouldRemove = true;
        for (var i = 0; i < RefreshRichTextEditorIds.length; i++) {
            var refreshId = RefreshRichTextEditorIds[i];
            if (id == refreshId) {
                shouldRemove = false;
                break;
            }
        }
        if (shouldRemove) {
            relocStaleRTE(id);
        }
    }
    // reset for next refresh
    RefreshRichTextEditorIds = new Array();
}

function prepRTEReplacement (id)
{
    relocStaleRTE(id);
}

function relocStaleRTE (id)
{
    var editor = RichTextEditors[id];
    if (editor) {
        ariba.Debug.log("Relocating RTE: "+id);
        var div = staleRTADiv();
        var editorArea = editor._htmlArea;
        var editorCell = editor._framework.ed_cell;
        // avoid duplicate ids on incremental update
        editorCell.getElementsByTagName("IFRAME")[0].id = "";
        editorCell.getElementsByTagName("TEXTAREA")[0].id = "";
        editorArea.parentNode.removeChild(editorArea);
        div.appendChild(editorArea);
        delete RichTextEditors[id];
        RichTextEditorsSize--;
    }
}

function checkCleanRTE ()
{
    if (typeof Xinha != 'undefined' && RichTextEditorsSize == 0) {
        ariba.Debug.log('checkCleanRTE');
        if (Xinha.is_ie){
            Xinha.collectGarbageForIE();
        }
        staleRTADiv().innerHTML = "";
        // reset Xinha state
        __xinhas=[];
        Xinha.toFree=[];
        Xinha._eventFlushers=[];
        Xinha._someEditorHasBeenActivated=false;
        Xinha._currentlyActiveEditor=null;
        // re-register unload handler removed by collectGarbageForIE
        if (Xinha.is_ie) {
            Xinha._addEvent(window,"unload",Xinha.collectGarbageForIE);
        }
    }
}

function onSubmitRTE ()
{
    var editor;
    for (var editorId in  RichTextEditors) {
        editor = RichTextEditors[editorId];
        editor.firePluginEvent("onBeforeSubmit");
        editor._textArea.value=editor.outwardHtml(editor.getHTML());
    }
}

/*
    Rich Text editor incremental cleanup

    Xinha keeps a list of all references that needs to be free,
    but we need to keep around any stale editor DOM elements to avoid FPR memory leaks,
    and before we clean up the references.

    We keep a hash of every current editor currently being displayed (RichTextEditors)

    On incremental update, we do the following:

    1) Each editor register itself to the list of editors to be displayed (RefreshRichTextEditorIds)
       regardless if it has any content to update.
    2) If there is incremental updated content,
       prep for the DOM replacement (prepRTEReplacement) with the following:
       a) relocate the stale editor's DOM elements
       b) remove the editor from the current list of editors
    3) Diff the current list of editors and new list of editors,
       and "remove" any stale editors (removeRTAs) with the following:
       a) relocate the stale editor's DOM elements
       b) remove the editors from the current list of editors
    4) DOM update happens and adds entries to the current list of editors.
    5) If there is no editor being displayed, call Xinha's cleanup code,
       clear out the relocated DOM elements, and reset the Xinha state (checkCleanRTE)
*/
ariba.Event.registerHandler("RTERelocateStale", "onRefreshRequestComplete", removeRTAs);
ariba.Event.registerRefreshCallback(checkCleanRTE);
ariba.Event.registerHandler("RTESubmit", "onsubmit", onSubmitRTE);

ariba.registerRefreshRTEId = registerRefreshRTEId;
ariba.prepRTEReplacement = prepRTEReplacement;
