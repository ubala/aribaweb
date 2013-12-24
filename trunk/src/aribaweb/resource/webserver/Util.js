/*
 Util.js -- basic javascript language utilities
*/

// Create Ariba package
if (!this.ariba) this.ariba = { awCurrWindow : null };

// Null logging implementation if Debug.js is not loaded
ariba.Debug = {
    log : function () {},
    logEvent : function () {}
};

ariba.Util = function() {

    var AWAlertException = false;
    
    var Util = {

        /**
         * @deprecated use http://api.jquery.com/jQuery.proxy/
         */
        bind : function (f, oCtx)
        {
            return $.proxy(f, oCtx);
        },

        /**
         * @deprecated use http://api.jquery.com/jQuery.extend/
         */
        extend : function (oDest, oSrc)
        {
            return $.extend(oDest, oSrc);
        },

        /**
         * Add elements from one array to another, but with optional
         * restrictions determined by fnPredicate and fnChildren.
         * Note: Does not modify the original array.
         * Note: I don't like this function, avoid it -msnider
         * @param aArray1 {Array|Array-like} The first array (all values copied).
         * @param aArray2 {Array|Array-like} A second array (some or all values copied).
         * @param fnPredicate {Function} A predicate function indicating
         *                               if the value at index in aArray2
         *                               should be copied.
         * @param fnChildren {Function} A function to fetch children from
         *                              value at index in aArray2.
         * @returns {Array} The new array.
         * @private
         */
        _arrayAdd : function (aArray1, aArray2, fnPredicate, fnChildren) {
            var aNewArray = [].concat(aArray1);

            // no predicate function, so just return true
            if (!fnPredicate) {
                fnPredicate = function () {
                    return true;
                };
            }

            // no children function, so just return null
            if (!fnChildren) {
                fnChildren = function () {
                    return null;
                };
            }

            $.each(aArray2, function(i, o) {
                var oChildren;

                if (fnPredicate(o)) {
                    aNewArray.push(o);
                }

                if ((oChildren = fnChildren(o))) {
                    aNewArray = Util._arrayAdd(
                        aNewArray, oChildren, fnPredicate, fnChildren);
                }
            });

            return aNewArray;
        },

        /**
         * @deprecated use Array.concat instead.
         */
        concatArr : function (a, b)
        {
            // ensure a is defined and an array
            if (!(a && a.length)) {
                a = [];
            }
            // ensure b is defined and an array
            if (!(b && b.length)) {
                b = [];
            }
            return a.concat(b);
        },

        /**
         * @deprecated use http://api.jquery.com/jQuery.makeArray/
         */
        toArray : function (o)
        {
            return $.makeArray(o);
        },

        arrayRemoveMatching : function (arr, target, getter)
        {
            if (!arr) return;
            for (var i = 0, c = arr.length; i < c; i++) {
                var e = (getter) ? getter(arr[i]) : arr[i];
                if (e == target) {
                    arr.splice(i, 1);
                    break;
                }
            }
        },

        /**
         * @deprecated use http://api.jquery.com/jQuery.isArray/
         */
        isArray : function (o)
        {
            return $.isArray(o);
        },

        /**
         * Note: This function is very magic and should be avoided -msnider
         */
        itemOrArrAdd : function (orig, obj)
        {
            return !orig ? obj : ($.isArray(orig) ? orig.push(obj) : [orig].push(obj));
        },

        /**
         * @deprecated use http://api.jquery.com/jQuery.inArray/
         */
        arrayIndexOf : function (a, o)
        {
            $.inArray(a, o);
        },

        /**
         * Add an element to the array if it does not already exist.
         * @param a {Array} Required. Array to add into.
         * @param o {Object} Required. Value to evaluate and add.
         */
        arrayAddIfNotExists : function (a, o)
        {
            if ($.inArray(a, o) < 0) {
                a.push(o);
            }
        },

        /**
         * Creates a copy of the provided array with only the unique values.
         * @param aOriginal The array to make unique.
         * @return {Array} A unique version of the array.
         */
        arrayMakeUnique : function (aOriginal)
        {
            var oFound = {},
                aCopy = [],
                i = aOriginal.length;

            while (i-- >= 0) {
                if (!oFound[aOriginal[i]]) {
                    oFound[aOriginal[i]] = true;
                    aCopy.push(aOriginal[i]);
                }
            }

            return aCopy;
        },

        /**
         * Evaluate if the object undefined.
         * @param o {object} Required. Any object to evaluate.
         * @returns {boolean} If undefined.
         */
        isUndefined : function (o)
        {
            return o === undefined;
        },

        /**
         * Evaluate if the object undefined or null.
         * @param o {object} Required. Any object to evaluate.
         * @returns {boolean} If undefined or null.
         */
        isNullOrUndefined : function (o)
        {
            return o === null || Util.isUndefined(o);
        },

        /**
         * Evaluate if the object undefined or null or empty string.
         * @param o {object} Required. Any object to evaluate.
         * @returns {boolean} If undefined or null or empty string.
         */
        isNullOrBlank : function (o)
        {
            return Util.isNullOrUndefined(o) || o === "";
        },

        stringEndsWith : function (sourceString, searchString)
        {
            return (sourceString.lastIndexOf(searchString) == (sourceString.length - searchString.length));
        },

        takeValue : function (obj, keypath, value) {
            function  set (obj, keyArr, value) {
                if (keyArr.length == 1) {
                    obj[keyArr[0]] = value;
                } else {
                    set(obj[keyArr.shift()], keyArr, value);
                }
            }
            set(obj, keypath.split("."), value);
        },

        takeValues : function (obj, keys, values) {
            for (var i=0; i < values.length; i++) {
                this.takeValue(obj, keys[i], values[i]);
            }
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
            return encodeURIComponent(s).replace("+", "%2B").replace("/", "%2F");
        },

        ///////////////
        // String util
        ///////////////

        /**
         * @deprecated use http://api.jquery.com/jQuery.trim/
         */
        strTrim : function (s)
        {
            return $.trim(s)
        },

        /**
         * @deprecated use Math.max instead.
         */
        max : function (iNumber1, iNumber2)
        {
            return Math.max(iNumber1, iNumber2);
        },

        ///////////
        // Util
        ///////////

        /**
         * @deprecated use http://api.jquery.com/jQuery.inArray/
         */
        indexOf : function (a, o)
        {
            $.inArray(a, o);
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
            if (intString && typeof intString == 'number') {
                return intString;
            }
            
            if (intString && typeof intString == 'string' && intString.charAt(0) == '0') {
                var index = this.indexOfNotChar(intString, 1, '0');
                intString = intString.substring(index, intString.length);
            }
            return parseInt(intString);
        },

        incrementAttribute : function (object, attributeName)
        {
            // guard
            if (!object || !attributeName) {
                return;
            }

            var attributeValue = object.getAttribute(attributeName);
            if (attributeValue) {
                attributeValue = this.parseInt(attributeValue) + 1;
            }
            else {
                attributeValue = 1;
            }
            object.setAttribute(attributeName, attributeValue);
        },

        getIntAttribute : function (object, attributeName)
        {
            // guard
            if (!object || !attributeName) {
                return 0;
            }
            
            var attributeValue = object.getAttribute(attributeName);
            if (attributeValue) {
                return this.parseInt(attributeValue);
            }
            return 0;
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

    /**
     * @deprecated use http://api.jquery.com/jQuery.proxy/
     */
    Function.prototype.bind = function() {
        var __method = this, a = Util.toArray(arguments), obj = a.shift();
        return function() {
            return __method.apply(obj, Util.concatArr(a, Util.toArray(arguments)));
        }
    };

    return Util;
}();
