/*
 Util.js -- basic javascript language utilities
*/

// Create Ariba package
if (!this.ariba) this.ariba = { awCurrWindow : null };

// Null logging implementation if Debug.js is not loaded
ariba.Debug = {
    log : function () {},
    debugEvent : function () {}
}

ariba.Util = function() {

    var AWAlertException = false;
    
    var Util = {

        extend : function(dest, source) {
            for (var p in source) dest[p] = source[p];
            return dest;
        },

        // add elements from src to dest, [contingent on pred], [unrolling children]
        _arrayAdd : function (dest, src, pred, childFn) {
            for (var i = 0; i < src.length; i++) {
                var o = src[i], children;
                if (!pred || pred(o)) dest.push(o);
                if (childFn && (children = childFn(o))) {
                    this._arrayAdd(dest, children, pred, childFn);
                }
            }
            return dest;
        },

        concatArr : function (a, b) {
            if (!a || !a.length) return b;
            if (!b || !b.length) return a;
            var r = new Array(a.length + b.length);
            var i = a.length; while (i--) r[i] = a[i];
            i = b.length; while (i--) r[i+a.length] = b[i];
            return r;
        },

        toArray : function (arr) {
          var i = (arr) ? arr.length : 0, r = new Array(i);
          while (i--) r[i] = arr[i];
          return r;
        },

        arrayRemoveMatching : function (arr, target, getter) {
            if (!arr) return;
            for (var i = 0, c = arr.length; i < c; i++) {
                var e = (getter) ? getter(arr[i]) : arr[i];
                if (e == target) {
                    arr.splice(i, 1);
                    break;
                }
            }
        },

        isUndefined : function (value)
        {
            return (typeof value == "undefined");
        },

        isNullOrUndefined : function (value)
        {
            return (value == null || (typeof value == "undefined"));
        },

        isNullOrBlank : function (value)
        {
            return (value == null || (typeof value == "undefined") || (value == ""));
        },

        stringEndsWith : function (sourceString, searchString)
        {
            return (sourceString.lastIndexOf(searchString) == (sourceString.length - searchString.length));
        },

        ////////////////////
        // Exception handling
        ////////////////////

        printStack : function ()
        {
            var sMsg = "";

            var oFunc;
            if (arguments.length != 0) {
                oFunc = arguments[0];
            }
            else {
                oFunc = this.printStack.caller;
            }

            while (oFunc != null) {
                var sFunc = oFunc.toString();
                sMsg += "<li>" + sFunc.substring(0, sFunc.search(/\n/)) + "<br/>";
                oFunc = oFunc.caller;
            }
            return sMsg;
        },

        getExceptionMsg : function (e, sMsg)
        {
            if (AWAlertException) {
                alert(sMsg);
            }

            var str = "****************************<br/>" + sMsg + "<br/>" +
                      "Exception: " + e.message + "<br/>" +
                      this.printStack(getExceptionMsg.caller) +
                      "<br/>****************************<br/>";

            return str;
        },

        htmlEscapeValue : function (sValue)
        {
            if (sValue == null)
                return;

            if (sValue.search(/</) != -1) {
                sValue = sValue.replace(/&/g, "&amp;");
                sValue = sValue.replace(/</g, "&lt;");
                sValue = sValue.replace(/>/g, "&gt;");
                sValue = "<pre>" + sValue + "</pre>";
            }
            else if (sValue.search(/function/) != -1) {
                sValue = "<pre>" + sValue + "</pre>";
            }

            return sValue;
        },

        uriEncode : function (s) {
            // strangely, the default JavaScript function doesn't handle "+" correctly
            return escape(s).replace("+", "%2B").replace("/", "%2F");
        },

        ///////////////
        // String util
        ///////////////

        strTrim : function (str)
        {
            //Match spaces at beginning and end of text and replace with null strings
            return str.replace(/^\s+/, '').replace(/\s+$/, '');
        },

        max : function (value1, value2)
        {
            return value1 > value2 ? value1 : value2;
        },

        ///////////
        // Util
        ///////////

        indexOf : function (list, item)
        {
            var length = list.length;
            for (var index = 0; index < length; index++) {
                if (list[index] == item) {
                    return index;
                }
            }
            return -1;
        },

        indexOfCharInSet : function (targetString, startIndex, charSet)
        {
            var length = targetString.length;
            for (var index = startIndex; index < length; index++) {
                if (charSet.indexOf(targetString.charAt(index)) != -1) {
                    return index;
                }
            }
            return -1;
        },

        indexOfCharNotInSet : function (targetString, startIndex, charSet)
        {
            var length = targetString.length;
            for (var index = startIndex; index < length; index++) {
                if (charSet.indexOf(targetString.charAt(index)) == -1) {
                    return index;
                }
            }
            return -1;
        },

        indexOfNotChar : function (targetString, startIndex, ch)
        {
            var length = targetString.length;
            for (var index = startIndex; index < length; index++) {
                if (targetString.charAt(index) != ch) {
                    return index;
                }
            }
            return -1;
        },

        /*
            A bug in parseInt prevents it from working on strings like "09"
            This strips off leading zeros and then calls parseInt()
        */
        parseInt : function (intString)
        {
            if (intString.charAt(0) == '0') {
                var index = this.indexOfNotChar(intString, 1, '0');
                intString = intString.substring(index, intString.length);
            }
            return parseInt(intString);
        },

        ///////////////////
        // Binding Support
        ///////////////////

        // This simplified binding support assumes all ivars are prefixed with "_"
        // and all setter methods begin with "setX" where X is the toUpper
        // version of the first char of the key.  If there is a getter
        // function, it will be the same as the key (no caps, no "_" prefix)

        valueForBinding : function (target, key)
        {
            var keyValue = target[key];
            if (typeof keyValue == "function") {
                keyValue = eval("target." + key + "()");
            }
            else {
                keyValue = target["_" + key];
            }
            return keyValue;
        },
        
        setValueForBinding : function (target, key, value)
        {
            var uppercaseKey = key.charAt(0).toUpperCase() + key.substr(1);
            var setterKey = "set" + uppercaseKey;
            var keyValue = target[setterKey];
            if (typeof keyValue == "function") {
                eval("target." + setterKey + "(value)");
            }
            else {
                eval("target._" + key + " = value");
            }
        },
        EOF:0};

    Function.prototype.bind = function() {
        var __method = this, a = Util.toArray(arguments), obj = a.shift();
        return function() {
            return __method.apply(obj, Util.concatArr(a, arguments));
        }
    }

    return Util;
}();
