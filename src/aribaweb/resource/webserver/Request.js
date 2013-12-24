/*
    Request.js      -- functions for initiating requests to the AW application

    Includes IFrame use for incremental requests, enforcing modality when request is
    in progress, polling for status updates on long running requests, ...
*/

ariba.Request = function() {
    // imports
    var Util = ariba.Util;
    var Event = ariba.Event;
    var Debug = ariba.Debug;
    var Input = ariba.Input;
    var Dom = ariba.Dom;

    // private vars
    var AWRequestInProgress = true;
    var AWSenderClickedCallbackList = null;
    var AWPollEnabled = true;
    var AWPollOnError = false;
    var AWPollTimeoutId, AWPollSenderId, AWPollUpdateSenderId, AWPollInterval;
    
    // register a form value for submission -- allows submission of form values for
    // elements that are not in the current form.
    var AWRequestValueList;
    var AWRefreshCompleteTimeout;
    var AWDocumentLoadTimeout;
    var AWRefreshCount = 0;

    //****************************************************
    // ping
    //****************************************************
    var AWShowPingFrame = false;
    var AWPingCompleteTimeout;
    var AWPingCheckCount = 0;
    var _AWProgressTimerHandle;
    var AWCancelRequestDelay = 0;
    var AWCancelRequestDelayHandle;

    var _XMLHTTP_COUNT = 0;
    var _XMLQUEUE = [];

    document.cookie = 'awscreenstats=' + window.screen.width + 'x' + window.screen.height;

    var Request = {
        // Public Globals
        AWSenderIdKey : 'awsn',
        AWResponseId : '',
        AWRefreshUrl : '',
        AWPingUrl : '',
        AWProgressUrl : '',
        AWFrameName : null,
        AWReqUrl : null,
        AWSessionIdKey : null,
        AWSessionId : null,
        AWDebugEnabled : false,
        AWJSDebugEnabled : false,
        AWShowRequestFrame : false,
        AWUpdateCompleteTime : 0,
        AWSessionSecureId : '',
        UseXmlHttpRequests : false,
        AWPollCallback : null,
        AWPollErrorState : "pollError",
        AWPollState : "poll",

        initParams : function (/* varargs */) {
            Util.takeValues(ariba, ["Request.AWResponseId", "Request.AWSessionSecureId", "Request.AWRefreshUrl",
                "Request.AWPingUrl", "Request.AWProgressUrl", "Request.AWReqUrl", "Request.UseXmlHttpRequests",
                "Request.AWSessionIdKey", "Request.AWSessionId", "Request.AWFrameName",
                "Refresh.AWBackTrackUrl", "Refresh.AWForwardTrackUrl",
                "Input.AWWaitAlertMillis", "Dom.AWOpenWindowErrorMsg"],
                arguments);
        },

        setDocumentLocation : function (hrefString, windowName, windowAttributes)
        {
            if (Util.isNullOrUndefined(windowName)) {
                this.getContent(hrefString);
            }
            else {
                if (Util.isNullOrUndefined(windowAttributes)) {
                    windowAttributes = '';
                }
                window.open(hrefString, windowName, windowAttributes);
            }
            return false;
        },

        openWaitWindow : function (windowName, windowAttributes)
        {
            var namedWindow = Dom.openWindow('', windowName, windowAttributes);
            namedWindow.focus();
            Input.showWaitAlertInWindow(namedWindow);
            return namedWindow;
        },

        submitFormAtIndexWithHiddenField : function (formIndex, hiddenFieldName, value)
        {
            var formObject = document.forms[formIndex];
            var hiddenFieldObject = formObject[hiddenFieldName];
            hiddenFieldObject.value = value;
            this.submitForm(formObject);
            return false;
        },

        submitFormForElementName : function (formName, elementId, mevent, target)
        {
            var formObject = Dom.formForName(formName);
            if (elementId) {
                Dom.addFormField(formObject, this.AWSenderIdKey, elementId);
            }
            this.submitForm(formObject, target);
            Event.cancelBubble(mevent);
            return false;
        },
        /////////////////
        // Key handling
        /////////////////
        // Either element or senderId are required.  Everything else is optional
        invoke : function (element, senderId, mevent, suppressForm, windowName, tagObjectName, submitValue, senderValue)
        {
            var formId = null;
            if (!senderId) senderId = element.id;
            if (element && !suppressForm) {
                formId = Dom.lookupFormId(element);
            }

            return this.senderClicked(senderId, formId, windowName, tagObjectName, mevent, submitValue, senderValue)
        },

        senderClicked : function (senderId, formId, windowName, tagObjectName, mevent, submitValue, senderValue, windowAttributes)
        {
            var formObject = null;
            if (formId != null) {
                formObject = Dom.getElementById(formId);
            }
        //alert("awsenderClicked() - windowName:"+windowName + ", formObj:"+formObject + ", formId:" + formId);
            if (formObject != null) {
                Event.cancelBubble(mevent);
                var inputObject;
                if (submitValue) {
                    // if we need to submit the actual value of the button
                    inputObject = document.createElement('input');
                    inputObject.type = 'hidden';
                    inputObject.id = senderId;
                    inputObject.name = senderId;
                    formObject.appendChild(inputObject);
                    inputObject.value = senderValue ? senderValue : senderId;
                }

            // pass the senderId
                var actionFieldObject = formObject["wzrd_action"];
                if ((actionFieldObject != null) && (tagObjectName != null)) {
                    actionFieldObject.value = tagObjectName;
                }
                Dom.addFormField(formObject, this.AWSenderIdKey, senderId);

                this.invokeSenderClickedCallbacks(senderId, formId);

                if (windowName != null && windowName != "_self") {
                    this.submitForm(formObject, windowName, windowAttributes);
                }
                else {
                    this.submitForm(formObject);
                }

                if (submitValue) {
                    // remove the value we added to the DOM
                    formObject.removeChild(inputObject);
                }
            }
            else {
                var urlString = this.formatUrl(senderId);
                this.invokeSenderClickedCallbacks(senderId, null);
                this.setDocumentLocation(urlString, windowName, windowAttributes);
            }
            if (mevent) Event.cancelBubble(mevent);
            return false;
        },

        registerSenderClickedCallback : function (method)
        {
            if (!AWSenderClickedCallbackList) {
                AWSenderClickedCallbackList = new Array();
            }

        // Debug.log("registering: " + Debug.getMethodName(method));
            AWSenderClickedCallbackList[AWSenderClickedCallbackList.length] = method;
        },

        invokeSenderClickedCallbacks : function (senderId, formId)
        {
            if (AWSenderClickedCallbackList) {
                for (var i = 0; i < AWSenderClickedCallbackList.length; i++) {
                    // Debug.log("evaluating: " + Debug.getMethodName(AWSenderClickedCallbackList[i]));
                    AWSenderClickedCallbackList[i](senderId, formId);
                }
            }
        },

        formatUrl : function (senderId)
        {
            var urlString = this.formatSenderUrl(senderId);
            return this.appendScrollValues(urlString);
        },

        formatSenderUrl : function (senderId)
        {
            return this.partialUrl() + this.AWSenderIdKey + '=' + senderId;
        },

        formatInPageRequestUrl : function (senderId)
        {
            return this.formatSenderUrl(senderId) +"&awip=1"
        },

        appendScrollValues : function (urlString)
        {
            if (urlString.indexOf("#") == -1) {
                urlString = urlString + '&awst=' + Dom.getPageScrollTop() + '&awsl=' + Dom.getPageScrollLeft();
            }
            return urlString;
        },

        gotoLink : function (senderId, windowName, windowAttributes, mevent)
        {
            var href = this.formatUrl(senderId);
            this.setDocumentLocation(href, windowName, windowAttributes);
            Event.cancelBubble(mevent);
            return false;
        },

        setResponseId : function (responseId)
        {
            this.AWResponseId = responseId;
        },

        appendQueryValue : function (url, key, value)
        {
            var urlString = url.toString();
            var separator = null;
            if (urlString.match(/\?/) == null) {
                separator = '?';
            }
            else {
                separator = '&';
            }
            urlString = urlString + separator + key + "=" + encodeURIComponent(value);
            return urlString;
        },

        setupPoll : function (enabled, intervalSecs, senderId, updateId, pollOnError)
        {
            AWPollEnabled = enabled;
            
            var pollInterval = intervalSecs * 1000;
            if (AWPollInterval != pollInterval) {
                AWPollInterval = pollInterval;
                clearTimeout(AWPollTimeoutId);
                AWPollTimeoutId = null;
            }

            AWPollSenderId = senderId;
            AWPollUpdateSenderId = updateId;
            AWPollOnError = pollOnError
            
        // kick off initial timer
            timer();

            function timer()
            {
                if (AWPollEnabled && AWPollTimeoutId == null) {
                    AWPollTimeoutId = setTimeout(poll.bind(Request), AWPollInterval);
                }
            }

            function pollNow ()
            {
                if (AWPollEnabled) {
                    clearTimeout(AWPollTimeoutId)
                    AWPollTimeoutId = setTimeout(poll.bind(Request), 0);
                }
            }

            // make it public
            this.pollNow = pollNow;

            function callback(xmlhttp)
            {
                // somebody else might have been using the XML http request too,
                // so just in case we have multiple timers going, we clear the one we are
                // aware of and then reset it to null so our timer starts the poll again
                clearTimeout(AWPollTimeoutId);
                AWPollTimeoutId = null;
                var response = xmlhttp.responseText;
                var status = xmlhttp.status;
                Debug.log("poll response: " + Util.htmlEscapeValue(response));
                Debug.log("poll status: " + status);

                // always poll even if error
                var scheduleTimer = AWPollOnError;

                if (response == "<AWPoll state='update'/>") {
                    Debug.log("page changed -- go get content");
                    // use the special sender id to indicate that we should re-render for update
                    if (!AWRequestInProgress) {
                        this.getContent(this.formatInPageRequestUrl(AWPollUpdateSenderId));
                    }
                    scheduleTimer = true;
                } else if (response == "<AWPoll state='nochange'/>") {
                    scheduleTimer = true;
                }
                else {
                    if (this.AWPollCallback) {
                        this.AWPollCallback(this.AWPollErrorState);
                    }                    
                }
                // check for no change explicitly, since the session might have expired
                if (scheduleTimer) {
                    timer();
                }
            }

            function poll()
            {
                // FIXME -- should check interval since last request against interval
                Debug.log("AWRequestInProgress: " + AWRequestInProgress + ", AWPollEnabled=" + AWPollEnabled);
                if (!AWRequestInProgress && AWPollEnabled) {
                    if (this.AWPollCallback) {
                        this.AWPollCallback(this.AWPollState);
                    }
                    var url = this.formatInPageRequestUrl(AWPollSenderId);
                // wrap the awLoadLazyDivCallback in an anonymous function so we can
                    // pass the additional divObject to it
                    this.initiateXMLHttpRequest(url, callback.bind(this));
                } else {
                    // somebody else might have been using the XML http request too,
                    // so just in case we have multiple timers going, we clear the one we are
                    // aware of and then reset it to null so our timer starts the poll again
                    clearTimeout(AWPollTimeoutId);
                    AWPollTimeoutId = null;
                    timer();
                }
            }

        },

        submitFormObjectNamed : function (formName)
        {
            var formObject = document.forms(formName);
            this.submitForm(formObject);
            return false;
        },

        // This function should be used for all form submissions
        // in AW javascript.
        //
        // This method is overwritten in Sourcing to catch multiple form
        // submissions from the same page for the purpose of confirming
        // or preventing multiple submissions.

        submitForm : function (formObject, target, targetAttributes, async)
        {
            // This allows for onSubmit to be associated with a form.
            // If the onSubmit returns:
            //        false, the form will *not* be submitted
            //        undefined, the form *will* be submitted.
            // This is the behavior of a regular button click.
            var shouldSubmit = true;
            if (formObject.onsubmit) {
                shouldSubmit = formObject.onsubmit();
            // Note: typeof(var) is the safest way to check for undefined
                if (typeof(shouldSubmit) == "undefined") {
                    shouldSubmit = true;
                }
            }
            if (shouldSubmit) {
                Event.invokeRegisteredHandlers("onsubmit");
                this.addAWFormFields(formObject);

                if (target && target != "_self") {
                    //debug("<--- start target post");
                    Dom.removeFormField(formObject, 'awii');
                    if (target == '_blank') {
                        target = 'AWWindow_' + new Date().getTime();
                    }
                    formObject.target = target;

                    // Note:  1) Doing this will bomb for target="_self"
                    // 2) Setting a target on the hyperlink, but not passing a target here (on the JS)
                    // (as previously done by AWHyperlink pre-behavior work will cause the
                    // progress bar to come up on a file download.  So, I'm disabling...
                    //
                    // this.openWaitWindow(target, targetAttributes);
                    formObject.submit();
                }
                else {
                    this.prepareForRequest(async);
                    try {
                        var fileUpload = this.hasPopulatedFileInputContol(formObject, true);
                        var origEncType = null;
                        if (fileUpload) {
                            origEncType = formObject.enctype;
                            if (!origEncType || (origEncType.indexOf("multipart") != 0)) {
                                formObject.enctype = "multipart/form-data";
                                formObject.encoding = "multipart/form-data";
                            } else {
                                origEncType = null;
                            }
                        }

                        if (this.UseXmlHttpRequests && !fileUpload) {
                            Debug.log("<--- Incremental post: XMLHTTP");
                            Dom.addFormField(formObject, 'awii', "xmlhttp");
                            var postBody = this.encodedFormValueString(formObject)
                            Request.initiateXMLHttpRequest(this.partialUrl(), function (xmlhttp) {
                                // XXX: Error handling?
                                var string = xmlhttp.responseText;
                                ariba.Refresh.processXMLHttpResponse(string);
                            }, postBody);

                        } else {
                            Debug.log("<--- Incremental post: IFRAME");
                            var iframe = this.createRefreshIFrame();
                            formObject.target = iframe.name;
                            Dom.addFormField(formObject, 'awii', iframe.name);
                            formObject.submit();
                            if (origEncType) formObject.enctype = origEncType;
                        }
                    }
                    catch (e) {
                        // unblock user interaction
                        this.requestComplete();
                        this.handleFileUploadError(e);
                        throw(e);
                    }
                }
            }

            this.removeAWFormFields(formObject);
            formObject.target = null;
        },

        formValueAccessors : {
            input: function(elm)
            {
                switch (elm.type.toLowerCase()) {
                    case 'checkbox':
                    case 'radio':
                        return elm.checked ? elm.value : null;
                    default:
                        return elm.value;
                }
            },

            textarea: function (elm) { return elm.value; },

            select: function(elm)
            {
                function value (option) {
                    return option.value || option.text;
                }

                if (elm.type.toLowerCase() == 'select-one') {
                    var index = elm.selectedIndex;
                    return index >= 0 ? value(elm.options[index]) : null;
                } else {
                    var values, length = elm.length;
                    if (!length) return null;

                    for (var i = 0, values = []; i < length; i++)
                    {
                        var option = elm.options[i];
                        if (option.selected) values.push(value(option));
                    }
                    return values;
                }
            }
        },

        serialize : function (elm) {
            var type = elm.tagName.toLowerCase();
            var accessor = this.formValueAccessors[type];
            return (accessor) ? accessor(elm) : null;
        },

        formValueMap : function (formObject) {
            var elms = formObject.getElementsByTagName('*');
            var data = {}
            for (var i = 0; i < elms.length; i++) {
                var e = elms[i]
                if (!e.disabled && e.name) {
                    var val = this.serialize(e);
                    if (val) {
                        var name = e.name;
                        data[name] = Util.itemOrArrAdd(data[name], val)
                    }
                }

            }
            return data;
        },

        encodedFormValueString : function (formObject) {
            var map = this.formValueMap(formObject);
            var arr = [];
            for (var key in map) {
                var encKey = encodeURIComponent(key);
                var val = map[key];
                if (Util.isArray(val)) {
                    for (var i=0; i < val.length; i++) {
                        arr.push(encKey + "=" + encodeURIComponent(val[i] || ""));
                    }
                } else {
                    arr.push(encKey + "=" + encodeURIComponent(val || ""));
                }
            }
            return arr.join("&");
        },

        addRequestValue : function (key, value)
        {
            if (!AWRequestValueList) {
                AWRequestValueList = new Object();
            }
            AWRequestValueList[key] = value;
        },

        addAWFormFields : function (formObject)
        {
            Dom.addFormField(formObject, 'awr', this.AWResponseId);
            Dom.addFormField(formObject, 'awst', Dom.getPageScrollTop());
            Dom.addFormField(formObject, 'awsl', Dom.getPageScrollLeft());
            Dom.addFormField(formObject, 'awssk', this.AWSessionSecureId);
            if (AWRequestValueList) {
                for (var key in AWRequestValueList) {
                    Dom.addFormField(formObject, key, AWRequestValueList[key]);
                }
            }
        },

        removeAWFormFields : function (formObject)
        {
            Dom.removeFormField(formObject, this.AWSenderIdKey);
            Dom.removeFormField(formObject, 'awr');
            Dom.removeFormField(formObject, 'awst');
            Dom.removeFormField(formObject, 'awsl');
            Dom.removeFormField(formObject, 'awssk');
            if (AWRequestValueList) {
                for (var key in AWRequestValueList) {
                    Dom.removeFormField(formObject, key);
                }
            }
        },

        handleFileUploadError : function (e)
        {
            var fileupload = Dom.getElementById("AWFileUploadErrorMessage");
            if (fileupload) {
                alert(fileupload.innerText);
            }
        },

        partialUrl : function ()
        {
            var val = this.AWReqUrl + "?awr=" + this.AWResponseId;
            if (this.AWSessionIdKey) {
                val += "&" + this.AWSessionIdKey + "=" + this.AWSessionId;
            }
            if (this.AWFrameName) {
                val += "&awf=" + this.AWFrameName;
            }
            if (this.AWSessionSecureId) {
                val += "&awssk=" + this.AWSessionSecureId;
            }
            return val + "&";
        },

        simpleXMLHTTP : function (url, callback)
        {
            // To avoid Firefox bug: Need to do in main event loop...
            this._asyncGet(function(http) {
                if (http) {
                    http.onreadystatechange = function() {
                        if (http.readyState == 4) {
                            if (http.status == 200) {
                                callback(http);
                            // alert("Callback! ");
                            }
                            else {
                                if (Request.AWDebugEnabled) {
                                    alert('Got "' + http.status + ' ' + http.statusText + '" for ' + url);
                                }
                            }
                            Request._asyncDone(http);
                        }
                    }
                    try {
                        http.open("GET", url, true);
                        http.send(null);
                    }
                    catch (e) {
                        alert("XMLHTTP Error in send: " + e.message);
                        Request._asyncDone(http);
                    }
                }
            });
        },

        //****************************************************
        // Refresh.js
        //****************************************************

        appendFrameName : function (url)
        {
            if (!Util.isNullOrUndefined(this.AWFrameName) &&
                url.match(/awf=/) == null) {
                return this.appendQueryValue(url, "awf", this.AWFrameName);
            }
            return url;
        },

        redirectRefresh : function ()
        {
            var url = this.appendFrameName(this.AWRefreshUrl);
            this.redirect(url);
        },

        // Called from an AWRedirect (either XHR or IFrame)
        redirect : function (url)
        {
            this.prepareRedirectRequest();

            var location = window.location;
            // Todo conditionalize for Safari < 4
            // setting the href = url if there a hash on the URL.
            // append/remove a dummy param to make the URL different
            // than the current one and make the set take effect.
            if (Dom.isSafari) {
                if (url.indexOf("/") == 0) {
                    if (location.href.indexOf("awrdt=1") < 0) {
                        url = url + "&awrdt=1";
                    }
                    else {
                        url = url.replace(/\&awrdt=1/, "");
                    }
                }
            }
            location.href = url;
            // in some cases, we are trying to redirect while the download 'Save/Open' dialog is up.
            // We need to retry in those cases.
            if (Dom.IsIE6Only) {
                function retry() {
                    // Retry only if redirect is not in progress
                    if (document.readyState != "loading") {
                        window.location.href = url;
                        setTimeout(retry, 500);
                    }
                }
                setTimeout(retry, 500);
            }
            // window.location.href gets canceled on Chrome (tested on version 27) and
            // We are using the following trick of redirecting after a short delay to get the redirect to take effect.
            // This isn't a problem on Safari but delayed redirect is harmless
            // Once the Chrome redirect problem is fixed, this code can be removed.
            if (Dom.isSafari) {
                setTimeout(
                    function () {
                        if (document.readyState != "loading") {
                            window.location.href = url;
                        }
                    },500);
            }
        },

        // Obsolete -- use awInvoke instead
        invokeAction : function (senderId)
        {
            this.getContent(this.formatUrl(senderId));
        },

        addAWQueryValues : function (url)
        {
            var newUrl = url;
            if (AWRequestValueList) {
                for (var key in AWRequestValueList) {
                    newUrl = this.appendQueryValue(newUrl, key, AWRequestValueList[key]);
                }
            }

            return newUrl;
        },

        // initiate content retrieval
        getContent : function (url, forceIFrame)
        {
            // Debug.log("--- awGetContent --> " + url + "  [windowName:" + window.name + ", this.AWReqUrl:" + AWReqUrl + "]");
            this.prepareForRequest();
            url = this.addAWQueryValues(url);
            if (this.UseXmlHttpRequests && !forceIFrame) {
                Debug.log("<--- Incremental get: XMLHTTP");
                url = this.appendQueryValue(url, "awii", "xmlhttp");
                Request.initiateXMLHttpRequest(url, function (xmlhttp) {
                    // XXX: Error handling?
                    var string = xmlhttp.responseText;
                    ariba.Refresh.processXMLHttpResponse(string);
                });
            } else {
                Debug.log("<--- Incremental get: IFRAME");
                var iframe = this.createRefreshIFrame();
            // Debug.log("<--- initiate incremental get " + url);
                url = this.appendQueryValue(url, "awii", iframe.name);
                iframe.src = this.appendFrameName(url);
            }
        },

        __retryRequest : function (senderId)
        {
            Debug.log("Server responded with retry request (" + senderId +") -- doing IFrame retry ");
            this.getContent(this.formatUrl(senderId), true);
        },

        prepareForRequest : function (noWaitCursor)
        {
            this.requestComplete();
            AWRequestInProgress = true;
            ariba.Refresh.enableRefreshScript();
            Event.invokeRegisteredHandlers("onRefreshRequestBegin");
            if (!noWaitCursor) Input.showWaitCursor();
        // start timer to make sure something comes back in the iframe
            setTimeout(this.startRefreshTimer.bind(this), 1);
            if (this.AWProgressUrl) {
                Input.AWWaitMillis = 20 * 60 * 1000;  // force off auto-hinding of panel -- 20 mins, anyway...
                this.initProgressCheck(this.AWProgressUrl, Input.AWWaitAlertMillis + 2000, Input.AWWaitAlertMillis);
            }
        },

        isRequestInProgress : function ()
        {
            return AWRequestInProgress;
        },

        requestNotInProgress : function ()
        {
            AWRequestInProgress = false;
        },

        requestComplete : function ()
        {
            this.requestNotInProgress();
            Input.hideWaitCursor();
        },

        // clear all server pings, but keep UI locked.
        prepareRedirectRequest : function ()
        {
            this.refreshRequestComplete();
            AWPollEnabled = false;
        },

        isRequestInProgress : function() {
            return AWRequestInProgress;
        },

        displayErrorDiv : function (innerHtml) {
            // CR# 1-C397FU: Unable to navigate to Downstream from Upstream with IE6.
            // Refer 2248549, where the check below was changed to Request.AWDebugEnabled
            // In IE6, this check gets evaluated correctly to false in PROD (expected).
            // But that triggers the else portion- redirectRefresh, bringing the user
            // back to Upstream. We had to move this back to just AWDebugEnabled.
            // The end users now hit a javascript error in IE6- AWDebugEnabled is
            // undefined. But depending on the Browser settings, it shows up as a small
            // icon on the bottom left of IE6. The flow works fine thereafter with this
            // trade-off.  
            if (AWDebugEnabled) {
                var div = document.createElement("div");
                div.className="debugFloat";
                div.innerHTML = innerHtml;
                document.body.appendChild(div);
            } 
            else {
                this.redirectRefresh();
            }
        },

        createRequestIFrame : function (frameName, showFrame)
        {
            var divName = frameName + "Div";
            var iframeDiv = Dom.getElementById(divName);
            if (!iframeDiv) {
                iframeDiv = document.createElement("div");
                if (!showFrame) iframeDiv.style.display = "none";
                iframeDiv.id = divName;
                document.body.appendChild(iframeDiv);
            }
            var style = showFrame ? " style='border:2px solid blue;' height='300px' width='400px'"
                    : " style='border:0px;display:none' height='0px' width='0px'";
            iframeDiv.innerHTML =
            "<iframe src='" + Dom.AWEmptyDocScriptlet + "' id='" + frameName + "' name='" + frameName + "'" + style + "></iframe>";

            return Dom.getElementById(frameName);
        },

        destroyRequestIFrame : function (frameName)
        {
            var iframe = Dom.getElementById(frameName);
            if (iframe) {
                // To deal with IE leaks we first load an empty doc in the iframe
                // and then innerHTML clear the wrapper div
                function didLoad() {
                    Event.removeEvent(iframe, "onload", didLoad);
                    setTimeout(function () {
                        // check that we are still in the DOM,
                        // since createRequestIFrame might have been 
                        // called in between.
                        if (!Dom.elementInDom(iframe)) {
                            Debug.log("Skipped destroying Iframe: " + frameName + ": no longer in DOM");
                            return;
                        }
                        var iframeDiv = Dom.getElementById(frameName + "Div");
                        if (iframeDiv) {
                            iframeDiv.innerHTML = "";
                            Debug.log("Destroyed Iframe: " + frameName);
                        }
                    }, 1);
                }
            // Assign our handler and kick off the process...
                Event.addEvent(iframe, "onload", didLoad);
                iframe.src = Dom.AWEmptyDocScriptlet;
            }
        },
        // see awStartRefreshTimer before modifying
        createRefreshIFrame : function ()
        {
            if (this.AWJSDebugEnabled) {
                var date = new Date();
                Debug.setRequestStartTime(date.getTime());
            }

            return this.createRequestIFrame("AWRefreshFrame", this.AWShowRequestFrame);
        },

        startRefreshTimer : function ()
        {
            if (!Dom.IsMoz) {
                AWRefreshCompleteTimeout = setTimeout(this.checkRequestComplete.bind(this), 500);
            }
        },

        setWindowLocation : function (url)
        {
            window.location = url;
        },

        checkRequestComplete : function ()
        {
            Debug.log("awCheckRequestComplete");
            var iframe = Dom.getElementById("AWRefreshFrame");
            if (!iframe) {
                // iframe removed already so document load must have completed
                return;
            }

            var handled = false;

            try {
                if (iframe.contentWindow && iframe.contentWindow.document &&
                    iframe.contentWindow.document.URL.indexOf(Dom.AWEmptyDocScriptlet) == -1) {

                    var doc = iframe.contentWindow.document;

                // make sure that an AW page was returned to the iframe
                    // wait for the document to load
                    if (doc.readyState) {
                        // ie
                        //debug("readyState: " + doc.readyState);
                        if (doc.readyState != "complete") {
                            AWDocumentLoadTimeout = setTimeout(this.checkDocumentLoad.bind(this), 500);
                        }
                        this.checkDocumentLoad();
                    }
                    /*
                    else {
                        // ns -- no ready state available so just give ns a bit to
                        // finish loading
                        //debug("starting awCheckDocumentLoad timer");
                        AWDocumentLoadTimeout = setTimeout(this.checkDocumentLoad, 2000);
                    }
                    */
                    handled = true;
                }
            }
            catch (e) {
                handled = true;
                Input.hideWaitCursor();
            // ping server to see if it is up ...
                this.pingServer();
            }

            if (!handled) {
                Debug.log("continuing " + AWRefreshCount);
                AWRefreshCount++;
                if (AWRefreshCount < 30) {
                    AWRefreshCompleteTimeout = setTimeout(this.checkRequestComplete.bind(this), 10000);
                }
                else {
                    Debug.log("request not initiated ...");
                }
            }
        },

        checkDocumentLoad : function ()
        {
            //debug("load check: " + AWDocumentLoadTimeout);
            var iframe = Dom.getElementById("AWRefreshFrame");
            if (!iframe) {
                // iframe removed already so document load must have completed
                return;
            }

            var refreshComplete;
            try {
                var doc = iframe.contentWindow.document;

            // ie only
                if (doc.readyState) {
                    Debug.log("readyState: " + doc.readyState);
                    if (doc.readyState != "complete") {
                        AWDocumentLoadTimeout = setTimeout(this.checkDocumentLoad.bind(this), 200);
                        return;
                    }
                }
            //debug("document: " + doc);
                refreshComplete = Dom.getDocumentElementById(doc, "AWRefreshComplete");
            }
            catch (e) {
                //debug("exception caught: " + e);
            }

        //debug("refreshComplete object: " + refreshComplete);

            if (!refreshComplete) {
                this.handleRequestError();
            }
        },

        refreshRequestComplete : function ()
        {
            //debug("--> incremental request complete :" + AWRefreshCompleteTimeout);
            AWRefreshCount = 0;
            clearTimeout(AWRefreshCompleteTimeout);
            clearTimeout(AWDocumentLoadTimeout);
            ariba.Refresh.clearPendingCompleteRequestRun();
        },

        handleRequestError : function ()
        {
            Input.hideWaitCursor();
            var iframe = Dom.getElementById("AWRefreshFrame");
            iframe.style.left = 0;
            iframe.style.top = 0;

            var iframeDiv = Dom.getElementById("AWRefreshFrameDiv");
            if (iframeDiv) {
                Debug.log("setting location of iframediv");
                iframeDiv.style.position = "absolute";
                iframeDiv.style.left = 0;
                iframeDiv.style.top = 0;
            }
            else {
                Debug.log("just setting iframe");
            //iframe.style.position="absolute";
            }

            var container;
            if (window.innerHeight) {
                // NS
                container = document.body;
                window.scroll(0, 0);
            }
            else {
                // IE
                container = document.documentElement;
                container.scrollTop = 0;
                container.scrollLeft = 0;
            }

            var height = container.scrollHeight > screen.availHeight ?
                         container.scrollHeight : screen.availHeight;
            var width = container.scrollWidth > (screen.availWidth) ?
                        container.scrollWidth : (screen.availWidth);

            iframeDiv.style.width = width + "px";
            iframeDiv.style.height = height + "px";

            iframe.style.width = width + "px";
            iframe.style.height = height + "px";
        },

        pingServer : function ()
        {
            //debug("pinging ...");
            var iframe = this.createRequestIFrame("AWPingFrame", AWShowPingFrame);
            iframe.src = this.AWPingUrl;
            AWPingCompleteTimeout = setTimeout(this.checkPingRequestComplete.bind(this), 1000);
        },

        checkPingRequestComplete : function ()
        {
            var iframe = Dom.getElementById("AWPingFrame");
            if (!iframe) {
                // iframe removed already so document load must have completed
                return;
            }

            var handled = false;

            try {
                if (iframe.contentWindow && iframe.contentWindow.document &&
                    iframe.contentWindow.document.URL.indexOf(Dom.AWEmptyDocScriptlet) == -1) {
                    // something came back so we we're ok
                    var doc = iframe.contentWindow.document;
                    handled = true;
                }
            }
            catch (e) {
                handled = true;
                this.handleRequestError();
            }

            if (!handled) {
                //debug("ping continuing " + AWPingCheckCount);
                AWPingCheckCount++;
                if (AWPingCheckCount < 30) {
                    AWRefreshCompleteTimeout = setTimeout(this.checkPingRequestComplete.bind(this), 10000);
                }
                else {
                    this.handleRequestError();
                }
            }
            else {
                if (!AWShowPingFrame) {
                    var iframeDiv = Dom.getElementById("AWPingFrameDiv");
                    if (iframeDiv) {
                        document.body.removeChild(iframeDiv);
                    }
                }
            }
        },

        initProgressCheck : function (url, initialDelay, pollInterval)
        {
            function statusUpdate(xmlhttp) {
                if (!AWRequestInProgress) return;

                var message = xmlhttp.responseText;
                Debug.log("Progress check.  Message:" + message);
                if (message == "--NO_REQUEST--") {
                    // no request in progress now.  Hide panel
                    var notifyRefreshComplete = function() {
                        Event.notifyRefreshComplete();
                        Request.requestComplete();
                    };

                    if (AWCancelRequestDelay <= 0) {
                        notifyRefreshComplete();
                    }
                    else {
                        Debug.log("setTimeout: AWCancelRequestDelayHandle, delay = " + AWCancelRequestDelay);
                        AWCancelRequestDelayHandle = setTimeout(
                            notifyRefreshComplete, AWCancelRequestDelay);
                    }

                } else {
                    Input.updateWaitMessage(xmlhttp.responseText);

                    // reset to keep running pollServer
                    timer(pollInterval);

                    // Soft-link to sso package... (Ick!)
                    // reset session timeout warning
                    if (window.awStartInactivityTimer) awStartInactivityTimer();
                }
            }

            function pollServer() {
                _AWProgressTimerHandle = null;
                if (!AWRequestInProgress) return;
                Request.initiateXMLHttpRequest(url, statusUpdate.bind(Request));
            }

            function timer(delay) {
                if (_AWProgressTimerHandle) clearTimeout(_AWProgressTimerHandle);
                _AWProgressTimerHandle = setTimeout(pollServer.bind(Request), delay); // async call
            }

            timer(initialDelay);
        },

        setCancelRequestDelay : function (delay)
        {
            AWCancelRequestDelay = delay;
        },

        getCancelRequestDelay : function ()
        {
            return AWCancelRequestDelay;
        },

        clearCancelRequestDelayHandle : function ()
        {
            if (AWCancelRequestDelayHandle) {
                Debug.log("clearTimeout: AWCancelRequestDelayHandle");
                clearTimeout(AWCancelRequestDelayHandle);
                AWCancelRequestDelayHandle = null;
            }
        },

        setStatusDone : function ()
        {
            window.status = "Done";
        },

        //****************************************************
        // File Upload Status
        //****************************************************

        hasPopulatedFileInputContol : function (form, includeUnpopulated) {
            var elements = form.getElementsByTagName('input');
            for (var i = 0; i < elements.length; i++) {
                var e = elements.item(i);
                if (e.type == "file") {
                    if (includeUnpopulated || e.value.length > 0) return true;
                }
            }
            return false;
        },
        //****************************************************
        // XMLHttp
        //****************************************************

        getXMLHttp : function ()
        {
            var http = null;
            try {
                http = new XMLHttpRequest();
            } catch (e1) {
                try {
                    http = new ActiveXObject("Msxml2.XMLHTTP");
                } catch (e2) {
                    try {
                    } catch (e3) {
                        http = new ActiveXObject("Microsoft.XMLHTTP");
                    }
                }
            }

            return http;
        },

        _asyncGet : function (func)
        {
            _XMLQUEUE.push(func);
            this._notifyGet();
        },

        nullFunc : function () {},

        _asyncDone : function (http)
        {
            _XMLHTTP_COUNT--;
            if (http) {
                http.onreadystatechange = this.nullFunc;
            }
            this._notifyGet();
        },

        _notifyGet : function ()
        {
            if (_XMLHTTP_COUNT > 2 || (_XMLQUEUE.length == 0)) return;
            var func = _XMLQUEUE.shift();
            _XMLHTTP_COUNT++;
            var xmlhttp = this.getXMLHttp();
        // alert ("GET: " + xmlhttp);
            // need to do async for Firefox...
            setTimeout(function () {
                func(xmlhttp);
            }, 0);
        },

        initiateXMLHttpRequest : function (url, callback, formPostData)
        {
            function doGet(xmlhttp) {
                // method, url, asynchronous, username, password
                xmlhttp.open("GET", url, true);
                xmlhttp.setRequestHeader("Content-type", "text/html");

            // Define an event handler for processing
                var _this = this;
                xmlhttp.onreadystatechange = function() {
                    _this.manageStateChange(xmlhttp, callback);
                };

            // Execute the request
                try {
                    xmlhttp.send(null);
                }
                catch (e) {
                    alert("Error initiating request");
                }
            }

            function doPost(xmlhttp) {
                // method, url, asynchronous, username, password
                xmlhttp.open("POST", url, true);
                xmlhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            // Define an event handler for processing
                var _this = this;
                xmlhttp.onreadystatechange = function() {
                    _this.manageStateChange(xmlhttp, callback);
                };

            // Execute the request
                try {
                    xmlhttp.send(formPostData);
                }
                catch (e) {
                    alert("Error initiating request");
                }
            }

            if (formPostData) {
                this._asyncGet(doPost.bind(this));
            } else {
                this._asyncGet(doGet.bind(this));
            }
        },
        //(0) (UNINITIALIZED) The object has been created, but not initialized
        //                   (open method has not been called).
        //(1) LOADING The object has been created, but the send method has not been called.
        //(2) LOADED The send method has been called and the status and headers are available,
        //           but the response is not yet available.
        //(3) INTERACTIVE Some data has been received. You can call responseBody and
        //                responseText to get the current partial results.
        //(4) COMPLETED All the data has been received, and the complete data is available in
        //              responseBody and responseText.
        manageStateChange : function (xmlhttp, callback)
        {
            switch (xmlhttp.readyState) {

                case 2,3:
                // error handling?
                    break;

                case 4:
                // convoluted callback to make sure callback happens in main event loop
                    var deferredCallBack = function () {
                        callback(xmlhttp);
                        this._asyncDone(xmlhttp);
                    };
                    setTimeout(deferredCallBack.bind(this), 0);

                    break;
            }
        },

        downloadContent : function (srcUrl)
        {
            var iframe = Request.createRequestIFrame("AWDownload");
            iframe.src = this.addAWQueryValues(srcUrl);
        },

        fileDownloadCompleteCheck : function (statusUrl, completeUrl, delay)
        {
            function statusUpdate(xmlhttp)
            {
                var string = xmlhttp.responseText;
                if (string == "completed") {
                    downloadCompleted();
                }
                else if (string == "started") {
                    setTimeout(pollServer.bind(this), delay);
                }
                else {
                    // unable to get valid status from server, do nothing
                    Debug.log("Error running fileDownloadCompleteCheck -- received: " + string);
                }
            }

            function downloadCompleted()
            {
                Request.getContent(completeUrl);
            }

            function pollServer()
            {
                //debug("poll server with statusUrl" + statusUrl);
                Request.initiateXMLHttpRequest(statusUrl, statusUpdate);
            }

            if (statusUrl != null && statusUrl.length > 0) {
                //debug("set timeout to poll server");
                setTimeout(pollServer.bind(this), delay); // async call
            }
            else {
                //debug("set time out to display download page");
                setTimeout(downloadCompleted.bind(this), delay);
            }
        },
        // For AWBody.awl
        progressBarSetWidth : function () {
            if (Dom.isWindowNarrow()) {
                var img = Dom.getElementById('awProgressBar');
                if (img && img.width > 150) img.width = "150px";
            }
        },

        EOF:0};

    // Initialization
    Event.registerRefreshCallback(Request.progressBarSetWidth.bind(Request));

    return Request;
}();

