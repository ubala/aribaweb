/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRequestUtil.java#12 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.http.multitab.MultiTabSupport;

import java.util.Map;
import java.util.StringTokenizer;

public class AWRequestUtil
{
    private static final String GETMethod = "GET";

    public static final String TokenizerDelim = "&";
    public static final char BeginQueryChar = '?';
    public static final char QueryDelimiter ='&';
    public static final char Equals = '=';

    private static final char HTTP_DELIMITER = '/';
    private static final String HTTP_DELIMITERSTRING = "/";

    private static String _applicationBaseUrl = null;
    private static String _urlKey = null;
    private static String _urlKeyDelimited = null;

    public static boolean isGet (AWRequestContext requestContext)
    {
        return requestContext.request().method().equals(GETMethod);
    }

    public static void addQueryParam (FastStringBuffer url, String name, String value)
    {
        if (url.indexOf(BeginQueryChar) == -1) {
            url.append(BeginQueryChar);
        }
        else {
            if (url.charAt(url.length()-1) != QueryDelimiter) {
                url.append(QueryDelimiter);
            }
        }
        url.append(name);
        url.append(Equals);
        url.append(AWUtil.encodeString(value));
    }

    public static String addQueryParam (String url, String name, String value)
    {
        FastStringBuffer fullUrl = new FastStringBuffer(url);
        addQueryParam(fullUrl, name, value);
        return fullUrl.toString();
    }

    public static void addRedirectParam (AWFormRedirect redirect, FastStringBuffer url,
                                         String name, String value, boolean asQueryParam)
    {
        if (asQueryParam) {
            if (url.indexOf(BeginQueryChar) == -1) {
                url.append(BeginQueryChar);
            }
            else if (url.charAt(url.length()-1) != BeginQueryChar) {
                url.append(QueryDelimiter);
            }
            url.append(name);
            url.append(Equals);
            url.append(AWUtil.encodeString(value));
        }
        else {
            redirect.addFormValue(name,value);
        }
    }

    /**
        Adds the given <code>requestHandlerKey</code> to the given <code>url</code>
        and returns the result.  If the given <code>url</code> is <code>null</code>
        or empty or blank, <code>url</code> is simply returned.
     */
    private static String addRequestHandlerKey (String url, String key)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(url)) {
            int qIndex = url.indexOf(BeginQueryChar);
            String urlMinusQueryString = (qIndex != -1)? url.substring(0, qIndex): url;
            FastStringBuffer buffer = new FastStringBuffer(urlMinusQueryString);
            if (!urlMinusQueryString.endsWith(HTTP_DELIMITERSTRING)) {
                buffer.append(HTTP_DELIMITERSTRING);
            }
            buffer.append(key);
            if (qIndex != -1) {
                String queryString = url.substring(qIndex, url.length());
                buffer.append(queryString);
            }
            return buffer.toString();
        }
        return url;
    }

    /**
        Adds the {@link AWConcreteApplication#DirectActionRequestHandlerKey} to
        the given <code>url</code> and returns the result.
     */
    public static String addDirectActionRequestHandlerKey (String url)
    {
        return addRequestHandlerKey(
                url, AWConcreteApplication.DirectActionRequestHandlerKey);
    }

    public static Map parseParameters (String url)
    {
        Map map = MapUtil.map();
        int startIndex = url.indexOf(BeginQueryChar);
        if (startIndex == url.length() -1) {
            return map;
        }
        url = url.substring(startIndex+1);
        StringTokenizer st = new StringTokenizer(url, TokenizerDelim);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int indexOfEquals = token.indexOf(Equals);
            if (indexOfEquals != -1) {
                String key = token.substring(0, indexOfEquals);
                if (indexOfEquals == token.length()) {
                    map.put(key, "");
                }
                else {
                    String value = token.substring(indexOfEquals+1, token.length());
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    static synchronized void initApplicationUrls (AWRequestContext requestContext)
    {
        AWApplication application = requestContext.application();
        String baseUrl = application.adaptorUrl();
        if (baseUrl == null) {
            return;
        }
        String applicationName = application.name();
        if (!baseUrl.endsWith(HTTP_DELIMITERSTRING) &&
            applicationName.indexOf(HTTP_DELIMITER) != 0) {
            // no slash
            baseUrl += HTTP_DELIMITERSTRING + applicationName;
        }
        else if (baseUrl.endsWith(HTTP_DELIMITERSTRING) &&
            applicationName.indexOf(HTTP_DELIMITER) == 0) {
            // both have slash
            baseUrl += applicationName.substring(0);
        }
        else {
            //PERFECT! only one slash
            baseUrl += applicationName;
        }
        int endIndex = baseUrl.length();
        // strip off trailing slash if any
        if (baseUrl.endsWith(HTTP_DELIMITERSTRING)) {
            baseUrl = baseUrl.substring(0,endIndex);
            endIndex--;
        }

        // find the index of the final two '/'
        int[] end = {-1,-1,-1};
        end[0] = baseUrl.indexOf(HTTP_DELIMITER);
        if (end[0] != -1) {
            end[1] = baseUrl.indexOf(HTTP_DELIMITER,end[0]+1);
            if (end[1] != -1) {
                end[2] = baseUrl.indexOf(HTTP_DELIMITER,end[1]+1);
                if (end[2] != -1) {
                    while (end[2] != -1) {
                        end[0] = end[1];
                        end[1] = end[2];
                        end[2] = baseUrl.indexOf(HTTP_DELIMITER,end[1]+1);
                    }
                }
                // end[2] == -1 now
                end[2] = endIndex;
            }
        }

        if (end[0] == -1 || end[1] == -1 || end[2] == -1) {
            throw new AWGenericException("Invalid base application url: " +
                baseUrl);
        }

        // now get the value between last two '/'
        // ex: /Analysis/Main
        _applicationBaseUrl = baseUrl;
        _urlKey = baseUrl.substring(end[0]);
        _urlKeyDelimited = _urlKey + HTTP_DELIMITERSTRING;
    }

    public static String applicationBaseUrl (AWRequestContext requestContext)
    {
        // get the application base url and strip off trailing '/'
        // example: http://host:port/Analysis/Main
        if (_applicationBaseUrl == null) {
            initApplicationUrls(requestContext);
        }
        return _applicationBaseUrl;
    }

    /**
     * Returns the URL key for the given request context. The URL key
     * is essentially the web app name + the name of the servlet separated
     * by a slash (e.g. /Buyer/Main).
     * @param requestContext
     * @return URL key
     */
    public static String urlKey (AWRequestContext requestContext)
    {
        if (_urlKey == null) {
            initApplicationUrls(requestContext);
        }
        return _urlKey;
    }

    /**
     * Returns the "delimited URL key" i.e. the URL key with a trailing
     * slash.
     * @param requestContext
     * @return The "delimited URL key"
     */
    public static String urlKeyDelimited (AWRequestContext requestContext)
    {
        if (_urlKeyDelimited == null) {
            initApplicationUrls(requestContext);
        }
        return _urlKeyDelimited;
    }

    /**
     * validates and returns the request URL.  The validation is checking the url for
     * the "urlKey" - which is the <ServletContext>/<ServletName> form.
     * If the urlKey is not found, a AWGenericException is thrown.
     * @param requestContext
     * @return The request url including any querystring.
     */
    public static String getRequestUrl (AWRequestContext requestContext)
    {
        // get what the appserver believes to be the current request
        MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
        String requestUrl = multiTabSupport.stripTabFromUri(requestContext.requestUrl());
        String urlSuffix;
        int keyIndex = requestUrl.indexOf(urlKeyDelimited(requestContext));
        String urlKey = urlKey(requestContext);

        if (keyIndex == -1) {
            // look without slash
            keyIndex = requestUrl.indexOf(urlKey);
            while (keyIndex != -1 &&
                    keyIndex + urlKey.length() != requestUrl.length() &&
                    requestUrl.charAt(keyIndex + urlKey.length()) !=
                            BeginQueryChar) {
                // if the key found is not the last part of the requestURL
                // or it doesn't end with ? or /, then keep searching since
                // we've just found a spurious match
                keyIndex = requestUrl.indexOf(urlKey, keyIndex + 1);
            }
        }

        if (keyIndex != -1) {
            // found it
            keyIndex += urlKey.length();
            urlSuffix = requestUrl.substring(keyIndex);
        }
        else {
            // could not find either key or delimited key
            throw new AWGenericException("Unable to match request url: " + requestUrl +
                    " to application base url: " + applicationBaseUrl(requestContext));
        }

        return multiTabSupport.insertTabInUri(applicationBaseUrl(requestContext) +
                urlSuffix, requestContext.getTabIndex(), true);
    }

    public static String getRequestUrlMinusQueryString (AWRequestContext requestContext)
    {
        // This *appears* to be a sufficient version of the *external* Url...
        // The AWServletRequest:httpServletRequest.getRequestURL() might be better
        // (but isn't portable...)
        String requestUrl = requestContext.requestUrl();
        int qIndex = requestUrl.indexOf("?");
        return (qIndex != -1) ? requestUrl.substring(0, qIndex): requestUrl;
    }
}
