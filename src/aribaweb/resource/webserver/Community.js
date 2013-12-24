/*
 Community.js

Copyright (c) 2013 Ariba, Inc.
 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

$Id:


 Support for:
 traversing DOM to find divs containing community context tags
 traversing DOM to find divs containing the whole community context
 creating URL to call into community application
 viewing community context
 */


ariba.Community = function() {

    if (!ENABLE_COMMUNITY) {
        return;
    }

    // Variable for document window
    var docWin = '';
    
    // imports
    var Dom = ariba.Dom;
    var Util = ariba.Util;
    var Event = ariba.Event;

    /**
     * Given an array of tags of the form "domain:tag", create an object and
     * for each domain add that domain as the key to an array of tags.
     *
     * so given tags that contribute this:
     * "app:Discovery,page:long page name,domainObject:posting,activity:Review,av:,matched leads,av:blah blah"
     * return a JSON object:
     *    { app:[Discovery], page:[long page name], domainObject:[posting], activity:[Review], av:[matched leads, blah blah] }
     * Depend on the value of stripDomain, either deep the domain or not
     */
    var partitionTagsByDomain = function(tagArray, stripDomain)
    {

        var cContext = {};
        if (tagArray) {
            for(var i = 0; i < tagArray.length; i++) {
                var val = tagArray[i];
                var splits = val.split(":");
                if (splits) {
                    var domain;
                    var val;
                    // if only one value, the that is the value and assign "misc" as the
                    // domain, if more than 2, then ignore 3rd, 4th components, etc.
                    if (splits.length == 1) {
                        domain = "misc";
                        val = splits[0];
                    }
                    else {
                        domain = splits[0];
                        val = splits[1];
                    }

                    if (!stripDomain) {
                        val = domain + ":" + val;
                    }

                    var dict = cContext[domain];
                    if (!dict) {
                        dict = [];
                        cContext[domain] = dict;
                    }
                    dict.push(val);
                }
            }
        }
        return cContext;
    };

    /**
     * Find nodes with a certain class name.  Use native implementation if possible.
     * Convert the nodelist to an array (this is mostly important if the caller
     * will change elements in the returned nodelist).
     * This constant: _community_context  MUST agree with
     */
    var findCommunityTagElems = function(elem)
    {
        var nodes;
        var a = [];
        if (elem.getElementsByClassName) {
            nodes = elem.getElementsByClassName("_community_context");
            // convert nodelist to array
            for(var i = 0; i < nodes.length; i++) {
                a.push(nodes[i]);
            }
        }
        else {
            // this is the Dom method provided to us, TODO: have Matt move a fast version into that ocd
            a = Dom.findChildrenUsingPredicate(elem,
                function (e) {
                    return e.className && e.className.indexOf("_community_context") >= 0;
                }
                );
        }
       
        return a;
    };


    /**
     * Very simple CSV parser
     *
     * We do not attempt to deal with embedded quotes, etc.  We expect input like this
     * " first , blah, two words ,good,also good..."
     *
     * Any embedded new-lines are NOT treated as delimiters
     *
     * We first condense all white-space down to a single white space and then after breaking up by CSV trim the results
     *
     * @param s string delimited csv string
     * @return array of strings
     */
    var parseCSV = function(csv) {
        // first get ride of all new-lines and extra spaces.  This will compact runs of white space
        // to a single white space
        csv = csv.replace(/\s\s+/g, ' ');

        var tags = [];

        // this may still leave extraneous space like so " , blah, two words ,good,also good..."
        // below when we break up into tags, so trim at that point
        var a = csv.split(",");

        for (var i = 0; i < a.length; i++) {
            if(typeof String.prototype.trim !== 'function') {
                 // Javascript Trim function does not work in IE.
                 // See http://stackoverflow.com/questions/2308134/trim-in-javascript-not-working-in-ie
                 a[i] = Util.strTrim(a[i]);
            } else {
                a[i] = a[i].trim();
            }
            if (a[i].length > 0) {
                tags.push(a[i]);
            }
        }

        return tags;

    };


    /**
     * Given an array of tags, first create a set of tags, and then return a CSV from that set
     *
     * TODO: this is copied from Util.js see: arrayMakeUnique  -- when we merge to current platform
 	 * remove this and call that
 	 *
     * @param tags  array of strings (may be duplicates)
     * @return set of strings in an array
     */
    var dedup = function(aOriginal) {

        var oFound = {},
        aCopy = [],
        i = 0,
        j = aOriginal.length;

        for (; i < j; ++i) {
            if (!oFound[aOriginal[i]]) {
                oFound[aOriginal[i]] = true;
                aCopy.push(aOriginal[i]);
            }
        }

        return aCopy;
    };


    /**
     * Given an array of Strings, return a CSV from that array
     *
     * @param a  array of strings (do not check for duplicates here)
     * @return   CSV of those strings
     */
    var arrayToCSV = function(a) {
        var retStr = "";

        // append each to the CSV, do not add an extraneous ','
        for (var i = 0; i < a.length; i++) {
            if (retStr.length == 0) {
                retStr = a[i];
            }
            else {
                retStr = retStr.concat(",", a[i]);
            }
        }
        return retStr;
    };

    var Community = {

        /**
         * Public API to get the community tags as an array of strings
         *
         * Note that each div may contain several tags as a CSV.
         * Implementation:
         *   (1) gather tags with magic class
         *   (2) concatenate all the CSV from each div into a longer CSV
         *   (3) normalize (at most one space in each tag, trim white space from start and end)
         *   (4) de-duplicate by creating a set
         *
         */
        getCommunityTags : function(root) {

            if (!root) {
                root = Dom.documentElement();
            }

            var nodes = findCommunityTagElems(root);
            var tagArray;

            var tagStr = "";

            for (var index = 0; index < nodes.length; index++) {
                var n = nodes[index];
                // not in old browsers var txt = n.textContent;
                var txt = n.innerHTML;
                if (tagStr.length == 0) {
                    tagStr = txt;
                } else {
                    tagStr = tagStr.concat(",", txt);
                }
            }
            
            tagArray = parseCSV(tagStr);            
            tagArray = dedup(tagArray);
            
            return tagArray;
        },

        /**
         * Public API to get the community tags as a CSV string
         */
        getCommunityTagsCSV : function(root) {

            if (!root) {
                root = Dom.documentElement();
            }
            var retStr;
            var tagArray = Community.getCommunityTags(root);

            retStr = arrayToCSV(tagArray);

            return retStr;
        },

        /*
        * Public API that gets called, if we want to open new community window with context
        */

        gotoCommunityWithContext : function(url, winAttr) {
            // args: communityUrl, windowAttr, passContext
            Community.gotoCommunity (url, winAttr, true);
        },

        /*
        * Public API that gets called, if we want to open new community window without context
        */

        gotoCommunityNoContext : function(url, winAttr) {
            // args: communityUrl, windowAttr, passContext
            Community.openCommunityWindow(url, winAttr, false);
        },

        /**
         * Public API to navigate to community and pass args
         * This API gets called, if we want to open community in new window with context
         */
        gotoCommunity : function(url, winAttr, passContext) {
            // args: helpUrl, windowAttr and passContext
            // by features may be empty, but we additionally look up the help context from the DOM using a magic
            // class and add that to the set of features

            var contextTagParams = '';

            if (passContext) {
                var tagCSV = "";
                var len = 0;

                var tagArray = Community.getCommunityTags(Dom.documentElement());

                // TODO: currently do not know when fts (features) is.  Array? In any case we need
                // to merge it with the tags we just fetched
                if (tagArray) {
                    len = tagArray.length;
                }

                if (tagArray) {
                    tagCSV = arrayToCSV(tagArray);
                }

                // Pass the context parameters as hidden input type
                contextTagParams += '<input name="ctxtTags" value="' + tagCSV + '" type="hidden"/>';
            }
            // Open new window to display community
            Community.openCommunityWindow(url, winAttr, contextTagParams);
        },

        /*
        * Public API to open new community window with specific attributes (winAttr)
        */
		openDocWin : function (winAttr) {
            return Dom.openWindow('', 'AribaDocWin', winAttr);
        },

        /*
        * Public API to get main window's height
        */
        getCurrentWindowHeight : function () {
            return screen.height;
        },

        /*
        * Public API that opens new window having URL passed
        */
        openCommunityWindow : function (url, winAttr, contextTagParams)
        {
            // if the window is open, then close it.  This is unfortunately necessary since
            // the help "app" is on a remote machine.  once we write the form and submit, the
            // window is redirected to a different machine so we're no longer able to write
            // directly into the document.
            // This first check only catches the case that help window is opened from same
            // page.  see catch statement below for case where help is left open across pages.
            if (!docWin.closed && docWin.location) {
                docWin.close();
            }

            //Check if the winAttr has height parameter in it, which will be passed by product.
            var regEx = new RegExp("height\s*=\s*[0-9]+", "g");
            //Format a string with new height which has height parameter as main window's height
            var newHeight = 'height=' + Community.getCurrentWindowHeight();
            if (regEx.test(winAttr) === true) {
                //Replace height param with newly calculated height
                winAttr = winAttr.replace(regEx, newHeight);
            }

            docWin = Community.openDocWin(winAttr);

            var content = '';

            if (contextTagParams != '') {
                content = '<html>' +
                    '<body onLoad="document.form1.submit();">' +
                    '<form method="post" action="' + url + '" id="form1" name="form1">' +
                    contextTagParams +
                    '</form>' +
                    '</body>';
            } else {
                content = '<html>' +
                    '<body onLoad="document.form1.submit();">' +
                    '<form method="post" action="' + url + '" id="form1" name="form1">' +
                    '</form>' +
                    '</body>';
            }
            try {
                docWin.document.write(content);
            }
            catch (e) {
                // for the case that help window left open across pages so close attempt
                // above does not catch it.  Close the window and try to write again.
                docWin.close();
                docWin = Community.openDocWin();
                docWin.document.write(content);
            }

            docWin.document.close();

            if (docWin.focus) {
                docWin.focus();
            }
        },


        /**
         * Public API to get the community context (from components in HTML) and then
         * and store them on an object a key/value pairs where the keys are the domain
         * and the values are arrays of tags.
         *
         * so given tags that contribute this:
         * "app:Discovery,page:long page name,domainObject:posting,activity:Review,av:,matched leads,misc:blah blah"
         * return a JSON object (and note that values are arrays):
         *    { app:[Discovery], page:[long page name], domainObject:[posting], etc. }
         *
         * @param  stripDomain if true, then strip the domain from the returned tag
         * values (app:Discovery -> Discovery instead of "app:Discovery")
         */
        getCommunityContextObject : function(stripDomain) {

            var tagArray = Community.getCommunityTags(Dom.documentElement());
            var cContext = partitionTagsByDomain(tagArray, stripDomain);
            return cContext;
        },

        /**
         * Create or find a window with known name and then populate it with the key-value pairs
         * of the community context.
         */
        viewCommunityContext : function() {

            var w = Dom.openWindow('', 'awcommunitycontext', 'resizable=yes,height=660,width=850');
            if (!w) return;

            var cContext = Community.getCommunityContextObject(true);
            var source = "";
            // this is the order that we want to see
            var preferredOrder = [ "app", "page", "activity", "domainObject", "av" ];
            // here is where we keep track of the ones that we have added
            var alreadyAdded = {};
            var i;
            // put the ones that we want at the top
            for (i = 0; i < preferredOrder.length; i++) {
                prop = preferredOrder[i];
                if (cContext.hasOwnProperty(prop)) {
                    alreadyAdded[prop] = true;
                    var val = arrayToCSV(cContext[prop]);
                    source = source.concat("<tr><td class='tableBody'>", prop, "</td><td class='tableBody'>", val, "</td></tr>");
                }
            }
            // now if any of the rest are not already on the return object
            for (var prop in cContext) {
              if (!alreadyAdded[prop] && cContext.hasOwnProperty(prop)) {
                var val = arrayToCSV(cContext[prop]);
                source = source.concat("<tr><td class='tableBody'>", prop, "</td><td class='tableBody'>", val, "</td></tr>");
              }
            }

            w.document.write('<html><head><title>View CommunityContext</title></head><body><h2>View CommunityContext</h2><table><tbody>' +
                "<tr><th class='tableHead' align='left'> Name </th><th class='tableHead' align='left'> Value </th></tr>" +
                source + '</tbody></body></html>');
            w.document.close();
            w.focus();
        },

        /**
        *  Pass the community context tags to community, by setting src of the iframe
        **/
        passCommunityTags : function () {            
            var iframe = Dom.getElementById('communityContentIframe');
            if (iframe) {
                var communityTags = Community.getCommunityTagsCSV (Dom.documentElement());
                //Check if community tags are not undefined
                if (communityTags != undefined) {
                    var srcString = iframe.src;
                    if (srcString.indexOf("#") == -1) {
                        srcString = srcString + "#" + communityTags;
                    } else {
                        srcString = srcString.substring(0,srcString.indexOf("#"));
                        srcString = srcString + "#" + communityTags;
                    }
                    iframe.src = srcString;
                }
            }
        },

        /**
        * Community Content pane should be collapsed, if screen resolution is < 1280 px.
        * and should be expanded if screen resolution is > 1280 px.
        **/
        resizeCommunityContentIframe : function () {
            var collapsedParamString = "&collapsed=";
            var collapsedParam, urlWithParam, styleClassParam, contextParam = "";
            //Get the current screen resolution based on user's window size
            var windowWidth = Dom.getWindowSize().slice(0,1);
            var contentIframe = Dom.getElementById('communityContentIframe');

            if (contentIframe) {
                var srcString = contentIframe.src;
                //If url contains context parameters separated by #
                if (srcString.indexOf("#") != -1) {
                    //Part of the url having parameters in it
                    urlWithParam = srcString.substring(0,srcString.indexOf("#"));
                    //Part of the url having context parameters in it
                    contextParam = srcString.substring(srcString.indexOf("#"));
                }
                //If url does not contain context parameters
                else {
                    urlWithParam = srcString;
                }
                //If user's screen resolution is < SCREEN_RESOLUTION_WIDTH (defined in config),
                //then we need to collapse the iframe.
                if (windowWidth < SCREEN_RESOLUTION_WIDTH)
                {
                    collapsedParam = "true";
                    styleClassParam = "aucIframeCollapsed";
                }
                //If user's screen resolution is >= SCREEN_RESOLUTION_WIDTH (defined in config),
                //then we need to expand the iframe
                else if (windowWidth >= SCREEN_RESOLUTION_WIDTH) {
                    collapsedParam = "false";
                    styleClassParam = "aucIframeExpanded";
                }
                if (urlWithParam.indexOf(collapsedParamString) != -1) {
                        urlWithParam = urlWithParam.substring(0,srcString.indexOf(collapsedParamString));
                }
                //Pass the parameter either collapsed = true or false along with rest of the parameters
                //The url with parameters will look like : https://<app url>?collapsed=true#<context_parameters>
                urlWithParam = urlWithParam + collapsedParamString + collapsedParam;
                //Set the new style sheet for either collapsed/expanded iframe
                contentIframe.className = styleClassParam;
                //Set the iframe source to new url having collapsed parameter in it
                contentIframe.src = urlWithParam + contextParam;
            }
        },

        resizeCommunityContentIframeThroughDirectAction  : function (iframeName, displayVar, styleClassName) {
            var contentIframe = Dom.getElementById(iframeName);
            if (contentIframe) {
                contentIframe.style.display= displayVar;
                contentIframe.className = styleClassName;
            }
        },

        EOF:0};

    //Pass community tags on page load
    Event.addEvent(window, 'onload', Community.passCommunityTags);
    //Pass community tags for AWRefreshRegion
    Event.registerRefreshCallback (Community.passCommunityTags);
    Event.addEvent(window, "onload", Community.resizeCommunityContentIframe);
    Event.addEvent(window, "onresize", Community.resizeCommunityContentIframe);

    return Community;

}();

