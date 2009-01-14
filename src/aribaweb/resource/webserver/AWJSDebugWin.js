// AWJSDebugWin.js

// singleton
var AWDebugWin_Debug = null;

function getDebug ()
{
    if (AWDebugWin_Debug == null) {
        AWDebugWin_Debug = new Debug();
        AWDebugWin_Debug.print(" --- Debug initialized --- ");
    }

    return AWDebugWin_Debug;
}

function Debug ()
{
    var _objectList = new Array();
    var _stateList = new Array();
    var _pendingPrint = new Array();
    var _count = 0;
    var _start = 0;
    var _startRequest;
    var _visible = false;
    var _synchronous = true;
    var _enabled = true;

    var self = this;

    var AWAsyncCheckbox     = "AWDAsyncCheckbox";
    var AWDDisabledCheckbox = "AWDDisabledCheckbox";
    var AWDTopControls      = "AWDTopControls";

    var AWDClearControl =
        '<a href="javascript:void(0);" onclick="javascript:getDebug().clear();return false;">Clear</a>\n';
    var AWDFunctionNamesControl =
        '<a href="javascript:void(0);" onClick="javascript:getDebug().toggleFunctionNames();return false;">Function names</a>\n';
    var AWDAddLogMarkerControl =
        '<a href="javascript:void(0);" onClick="javascript:getDebug().addLogMarker();return false;">Mark</a>\n';

    var AWDASyncToggleControl =
        '<input id="AWDAsyncCheckbox" type="checkbox" value="sync" onClick="javascript:getDebug().toggleSynchronous();return true;"/>Async\n';
    var AWDDisabledToggleControl =
        '<input id="AWDDisabledCheckbox" type="checkbox" value="sync" onClick="javascript:getDebug().toggleEnabled();return true;"/>Disable\n';

    //
    // public methods (privileged methods)
    //
    Debug.prototype.toggleSynchronous = function ()
    {
        var checkbox = awgetElementById(AWAsyncCheckbox);
        _synchronous = !checkbox.checked;
        return true;
    }

    Debug.prototype.toggleEnabled = function ()
    {
        var checkbox = awgetElementById(AWDDisabledCheckbox);
        _enabled = !checkbox.checked;
        return true;
    }

    Debug.prototype.toggleFunctionNames = function ()
    {
        _visible = !_visible;
        var style = _visible ? 'inline' : 'none';
        for (var i=_start; i < _count; i++)
        {
            var span = awgetElementById(i);
            if (span) {
                span.style.display = style;
                //this.toggleFunctionName(i);
            }
        }
    }

    Debug.prototype.addLogMarker = function ()
    {
        this.addContent("<hr/>");
    }

    Debug.prototype.toggleFunctionName  = function (id)
    {
        var span = awgetElementById(id);
        if (span) {
            if (span.style.display == "none") {
                span.style.display = "inline";
            }
            else {
                span.style.display = "none";
            }
        }
    }

    Debug.prototype.clear = function()
    {
        // fix me!  Need to close the doc and restart doc write?
        var body = document.body; // awgetElementById(AWDTopControls);
        while (body.lastChild) {
            body.removeChild(body.lastChild);
        }
        document.write('<br/><b>AW Javascript Debug</b><br/>\n' +
            '<div id="AWDTopControls" style="background-color:#FFEAAA;border:1px solid black; position:absolute; left:10px; top:1px; padding:0px 0px 2px 2px; width:100%; z=index:1"></div>' +
            '<hr/>\n');
        var topControls = awgetElementById(AWDTopControls);
        topControls.innerHTML = AWDClearControl + AWDFunctionNamesControl + AWDAddLogMarkerControl +
                                AWDASyncToggleControl + AWDDisabledToggleControl;
        adjustControls();
        _start = _count;
    }


    var _PendingMove = false;
    function adjustControls ()
    {
        if (!_PendingMove) {
            _PendingMove=true;
            setTimeout(function() {
                awgetElementById(AWDTopControls).style.top=document.body.scrollTop;
                _PendingMove=false;
            }, 1);
        }
    }

    Debug.prototype.addContent = function (content)
    {
        document.write(content);
        adjustControls();
    }

    Debug.prototype.flush = function()
    {
        var style = _visible ? 'inline' : 'none';

        var str = "";
        var i;
        for (i=0; i<_pendingPrint.length; i++) {
            var rec=_pendingPrint[i];
            //'<p style="padding:1px 1px 1px 1px; margin:1px;border-bottom:1px solid #666699">' +
            str += '<p style="padding:1px 1px 1px 1px; margin:1px;">' +
               '<a href="javascript:void(0);" onClick="javascript:getDebug().toggleFunctionName(\''+_count+'\');return false;">' + _count + '</a>: ' +
               '<span id="'+_count+'" style="display:'+style+'">['+rec["method"]+']: </span>' +
               rec["msg"] +
            "</p>";
            _count++;
        }

        this.addContent(str);

        _pendingPrint = new Array();  // clear
        document.body.scrollTop = document.body.scrollHeight;
    }

    Debug.prototype.print = function(sMsg, sMethodName)
    {
        if (!_enabled) {
            return;
        }

        var rec = new Object();
        rec["msg"]=sMsg;
        rec["method"]=sMethodName;
        _pendingPrint[_pendingPrint.length] = rec;

        if (_synchronous) {
            this.flush();
        }
        else if (_pendingPrint.length == 1) {
            // print 1.5 sec from first print
            var theThis = this;
            setTimeout(function() {theThis.flush()}, 1500);
        }
    };

    Debug.prototype.printProperties = function(element)
    {
        this.print(getProperties(element));
    };

    Debug.prototype.nodeClick = function (poTarget, sIndex)
    {
        var parent = poTarget.parentNode;

        // ### figure out why node appending doesn't work
        //var newNode = document.createElement("span");
        //newNode.innerHTML = "clicked!";
        //parent.appendChild(newNode);

        var td = awgetElementById(parent.id);
        if (!_stateList[sIndex]) {
            // closed so open
            td.innerHTML = getObjectLink(sIndex) + "<br/>" + getProperties(_objectList[sIndex]);
            //td.bgColor = "#EEEEEE";
            _stateList[sIndex] = true;
        }
        else {
            //open so close
            td.innerHTML = getObjectLink(sIndex);
            _stateList[sIndex] = false;
            //td.bgColor = "#FFFFFF";
        }

        // ### todo: hilight label when object is opened
    };

    Debug.prototype.setStartRequest = function (requestStart)
    {
        _startRequest = requestStart;
    };

    Debug.prototype.getRequestStart = function ()
    {
        return _startRequest;
    };

    Debug.prototype.resizeWindow = function ()
    {
        var browserWidth, browserHeight;

        if (document.layers){
            browserWidth=window.outerWidth;
            browserHeight=window.outerHeight;
        }
        else if (document.all){
            browserWidth=document.body.clientWidth;
            browserHeight=document.body.clientHeight;
        }
        this.print("window resize " + browserWidth + "x" + browserHeight);
    }

    //
    // private methods
    //

    function printException (e,sMsg)
    {
        self.print(awGetExceptionMsg(e,sMsg));
    }

    function getProperties (element)
    {
        //self.print("getProperties " + element + " " + element.nodeName + " " + element.id);

        var str;
        try {
            var propertyList = new Array();
            var sMsg = "<li/>" + element.nodeName + "<li/>" + element.id;
            sMsg += '<table border=1>';
            var property;
            for (property in element) {
                propertyList[propertyList.length] = new String(property);
            }
            propertyList.sort();

            for (var i = 0; i < propertyList.length; i++) {
                property = propertyList[i];
                var value = null;
                try {
                    value = element[property];
                }
                catch (e) {
                    value = "unable to evaluate";
                }
                sMsg+="<tr><td valign=top>"+property+"</td>" + valueTD(value)+"</tr>";
            }
            sMsg+='</table>';
            str = sMsg;
        }
        catch (e) {
            str = awGetExceptionMsg(e,"Unable to find properties of: " + element);
        }
        return str;
    }

    function valueTD (pValue)
    {
        var str;

        if (pValue == null) {
            return '<td><i>undefined</i></td>';
        }

        var sValue = new String(pValue);

        if (sValue.indexOf("[") == 0 &&
            sValue.lastIndexOf("]") == sValue.length-1) {
            // save off object
            var index = _objectList.length;
            _objectList[index] = pValue;

            str = '<td id="data'+index+'">';
            str += getObjectLink(index);
            //'<a href="#" id="'+index+'" onclick="nodeClick(this,'+index+');return false;">object</a>';
            str += '</td>';
        }
        else {

            str = '<td>'+awHtmlEscapeValue(sValue)+'</td>';
        }

        return str;
    }

    function getObjectLink (sIndex)
    {
        return '<a href="javascript:void(0);" id="'+sIndex+'" onclick="getDebug().nodeClick(this,'+sIndex+');return false;">[object]</a>';
    }

// init
    {
        this.clear();
        window.onresize = adjustControls;
        document.body.onscroll = adjustControls;
    }

    //////////////////////////////////////////////
    // Copied from Util.js, Dom.js to make AWJSDebugWin standalone
    //////////////////////////////////////////////
    var _alertException = false;

    function awgetElementById (id)
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
    }

    function awPrintStack ()
    {
        var sMsg = "";

        var oFunc;
        if (arguments.length != 0) {
            oFunc = arguments[0];
        }
        else {
            oFunc = awPrintStack.caller;
        }

        while (oFunc != null) {
            var sFunc = oFunc.toString();
            sMsg += "<li>" + sFunc.substring(0,sFunc.search(/\n/)) + "<br/>";
            oFunc = oFunc.caller;
        }
        return sMsg;
    }

    function awGetExceptionMsg (e,sMsg)
    {
        if (_alertException) {
            alert(sMsg);
        }

        var str = "****************************<br/>" + sMsg + "<br/>" +
                  "Exception: " + e.message + "<br/>" +
                  awPrintStack(awGetExceptionMsg.caller) +
                  //"Thrown by: " + htmlEscapeValue(writeException.caller) +
                  "<br/>****************************<br/>";

        return str;
    }

    function awHtmlEscapeValue (sValue)
    {
        if (sValue == null)
            return;

        if (sValue.search(/</) != -1) {
            sValue = sValue.replace(/&/g,"&amp;");
            sValue = sValue.replace(/</g,"&lt;");
            sValue = sValue.replace(/>/g,"&gt;");
            sValue = "<pre>" + sValue + "</pre>";
        }
        else if (sValue.search(/function/) != -1) {
            sValue = "<pre>" + sValue + "</pre>";
        }

        return sValue;
    }
}

