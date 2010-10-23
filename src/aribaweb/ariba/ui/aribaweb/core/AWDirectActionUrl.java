/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWDirectActionUrl.java#34 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import java.util.Map;
import java.util.Iterator;
import javax.servlet.http.HttpSession;

public final class AWDirectActionUrl extends AWBaseObject
{
    // Static Variables;

    private static final Object Lock = new Object();
    private static String AdaptorUrl;
    private static String ApplicationName;
    private static String ApplicationSuffix;
    private static AWDirectActionUrl SharedDirectActionUrl;
    private static String AlternateSecurePort;
    // Instance Variables
    private String _httpProtocol;
    private String _hostName;
    private String _portNumber;
    private String _adaptorUrl;
    private String _applicationName;
    private String _applicationSuffix;
    private String _applicationNumber;
    private String _directActionName;
    private String _directActionClassName;
    private final Map _queryStringValues = MapUtil.map();


    /**
     * Defines the API for a DirectAction decorator -- the list of decorators registered
     * using the addURLDecorator method will be called during URL construction and
     * each decorator can add a single key/value to the direact action URL generated.
     *
     * If either the key or the value returns null, empty, or blank string, then the
     * decorator is ignored and nothing is added to the direct action URL.
     *
     * @aribaapi private
     */
    abstract public static class AWUrlDecorator
    {
        abstract public String getKey ();
        abstract public String getValue (AWRequestContext requestContext);
        public boolean decorateComponentActions ()
        {
            return true;
        }
    }

    public static void setup (String adaptorUrl,
                              String applicationName,
                              String applicationSuffix,
                              String alternateSecurePort)
    {
        ApplicationName = applicationName;
        setAlternateSecurePort(alternateSecurePort);
        setDefaultAdaptorUrl(adaptorUrl);
        setDefaultApplicationSuffix(applicationSuffix);
        if (applicationSuffix.endsWith("/")) {
            ApplicationSuffix = applicationSuffix.substring(0, applicationSuffix.length() - 1);
        }
        else {
            ApplicationSuffix = applicationSuffix;
        }
    }

    public static void setAlternateSecurePort (String alternateSecurePort)
    {
        AlternateSecurePort = alternateSecurePort;
    }

    public static String alternateSecurePort ()
    {
        return AlternateSecurePort;
    }

    public static void setDefaultAdaptorUrl (String adaptorUrl)
    {
        if (adaptorUrl.endsWith("/")) {
            AdaptorUrl = adaptorUrl.substring(0, adaptorUrl.length() - 1);
        }
        else {
            AdaptorUrl = adaptorUrl;
        }
        // reset the direct action URL (so we don't vend a stale AdaptorURL)
        synchronized (Lock) {
            SharedDirectActionUrl = null;
        }

    }

    public static void setDefaultApplicationSuffix (String applicationSuffix)
    {
        if (applicationSuffix.endsWith("/")) {
            ApplicationSuffix = applicationSuffix.substring(0, applicationSuffix.length() - 1);
        }
        else {
            ApplicationSuffix = applicationSuffix;
        }
    }

    public static AWDirectActionUrl checkoutUrl ()
    {
        AWDirectActionUrl directActionUrl = null;
        synchronized (Lock) {
            directActionUrl = SharedDirectActionUrl;
            SharedDirectActionUrl = null;
        }
        if (directActionUrl == null) {
            directActionUrl = new AWDirectActionUrl();
        }
        return directActionUrl;
    }

    public static AWDirectActionUrl checkoutFullUrl (AWRequestContext requestContext)
    {
        AWDirectActionUrl directActionUrl = checkoutUrl();
        String fullAdaptorUrl = fullAdaptorUrlForRequestContext(requestContext);
        directActionUrl.setAdaptorUrl(fullAdaptorUrl);
        return directActionUrl;
    }

    public static void checkinUrl (AWDirectActionUrl directActionUrl)
    {
        if (directActionUrl != null) {
            directActionUrl.reset();
            SharedDirectActionUrl = directActionUrl;
        }
    }

    public AWDirectActionUrl ()
    {
        super();
        reset();
    }

    private void reset ()
    {
        _httpProtocol = null;
        _hostName = null;
        _portNumber = null;
        _adaptorUrl = AdaptorUrl;
        _applicationName = ApplicationName;
        _applicationSuffix = ApplicationSuffix;
        _applicationNumber = null;
        _directActionName = null;
        _directActionClassName = null;
        _queryStringValues.clear();
    }

    /*  -------------------
        Standard Accessors
        ------------------- */
    public void setHttpProtocol (String httpProtocol)
    {
        _httpProtocol = httpProtocol;
    }

    public void setHostName (String hostName)
    {
        _hostName = hostName;
    }

    public void setPortNumber (String portNumber)
    {
        _portNumber = portNumber;
    }

    public void setAdaptorUrl (String adaptorUrl)
    {
        _adaptorUrl = adaptorUrl;
    }

    public void setApplicationName (String applicationName)
    {
        _applicationName = applicationName;
    }

    public void setApplicationSuffix (String applicationSuffix)
    {
        _applicationSuffix = applicationSuffix;
    }

    public void setApplicationNumber (String applicationNumber)
    {
        _applicationNumber = applicationNumber;
    }

    public void setDirectActionName (String directActionName)
    {
        _directActionName = directActionName;
    }

    public void setDirectActionClassName (String directActionClassName)
    {
        _directActionClassName = directActionClassName;
    }


    /*  --------------
        Query String
        -------------- */
    public void setSessionId (String sessionId)
    {
        if (sessionId != null && !AWConcreteApplication.IsCookieSessionTrackingEnabled) {
            _queryStringValues.put(AWRequestContext.SessionIdKey, sessionId);
        }
    }

    public void setResponseId (AWEncodedString responseId)
    {
        if (responseId != null) {
            _queryStringValues.put(AWRequestContext.ResponseIdKey, responseId);
        }
    }

    public void setFrameName (AWEncodedString frameName)
    {
        if (frameName != null) {
            _queryStringValues.put(AWRequestContext.FrameNameKey, frameName);
        }
    }

    public void put (String key, String value)
    {
        if (key != null && value != null) {
            _queryStringValues.put(key, value);
        }
    }

    /*  --------------
        Decorators
        -------------- */
    static Map /*AWUrlDecorator*/ _decorators;

    /**
     * Register a AWUrlDecorator with the AWDirectActionUrl class.  Each of
     * the decorators registered will be run during URL construction.
     *
     * If a decorator is registered which has the same key as an existing decorator then
     * this method will throw a FatalAssertionException.
     *
     * @see AWUrlDecorator
     * @throws ariba.util.core.FatalAssertionException if decorator registered with the
     * same key as an existing decorator
     * @param decorator
     */
    public static synchronized void registerURLDecorator (AWUrlDecorator decorator)
    {
        if (decorator == null) return;
        if (_decorators == null) {
            _decorators = MapUtil.map();
        }
        String key = decorator.getKey();
        Assert.that(!_decorators.containsKey(key),
                    Fmt.S("AWUrlDecorator contains key '%s'" +
                          " which has already been registered.", key));
        _decorators.put(key, decorator);
    }

    private void decorateUrl (AWRequestContext requestContext)
    {
        if (_decorators == null) {
            return;
        }
        Iterator it = _decorators.values().iterator();
        while (it.hasNext()) {
            AWUrlDecorator decorator = (AWUrlDecorator)it.next();
            String key = decorator.getKey();
            String value = decorator.getValue(requestContext);
            // if key and value are non-null then add to query params for this
            // DirectActionURL
            if (!StringUtil.nullOrEmptyOrBlankString(key) &&
                !StringUtil.nullOrEmptyOrBlankString(value)) {
                put(key, value);
            }
        }
    }

    public static String decorateUrl (AWRequestContext requestContext, String url)
    {
        return decorateUrl(requestContext, url, false);
    }

    public static String decorateUrl (AWRequestContext requestContext, String url,
                                      boolean isComponentAction)
    {
        if (_decorators == null) {
            return url;
        }
        StringBuffer urlBuffer = new StringBuffer(url);
        Iterator it = _decorators.values().iterator();
        while (it.hasNext()) {
            AWUrlDecorator decorator = (AWUrlDecorator)it.next();
            if (isComponentAction && !decorator.decorateComponentActions()) {
                continue;
            }

            String key = decorator.getKey();
            String value = decorator.getValue(requestContext);
            // if key and value are non-null then add to query params for this
            // DirectActionURL
            if (!StringUtil.nullOrEmptyOrBlankString(key) &&
                !StringUtil.nullOrEmptyOrBlankString(value)) {
                if (urlBuffer.indexOf("?") == -1) {
                    urlBuffer.append("?");
                }
                else if (urlBuffer.charAt(urlBuffer.length()-1) != '&') {
                    urlBuffer.append('&');
                }
                urlBuffer.append(key).append("=").append(value);
            }
        }

        return urlBuffer.toString();
    }

    /*  --------------
        Session rendevous
        -------------- */
    /**
     * @param flag
     * @deprecated
     */
    public void setSessionRendevous (boolean flag)
    {
    }

    /*  --------------
        Composing
        -------------- */

    protected String composeUrl ()
    {
        FastStringBuffer fastStringBuffer = new FastStringBuffer(256);
        if (_httpProtocol != null) {
            fastStringBuffer.append(_httpProtocol);
            fastStringBuffer.append("://");
            if (_hostName == null) {
                throw new AWGenericException("Invalid AWDirectActionUrl: missing hostName.");
            }
            fastStringBuffer.append(_hostName);
            if (_portNumber != null) {
                fastStringBuffer.append(":");
                fastStringBuffer.append(_portNumber);
            }
        }
        fastStringBuffer.append(_adaptorUrl);
        if (!_adaptorUrl.endsWith("/")) {
            fastStringBuffer.append("/");
        }
        if (!StringUtil.nullOrEmptyString(_applicationName) ||
            !StringUtil.nullOrEmptyString(_applicationSuffix)) {
            fastStringBuffer.append(_applicationName);
            fastStringBuffer.append(_applicationSuffix);
            fastStringBuffer.append("/");
        }
        if (_applicationNumber != null) {
            fastStringBuffer.append(_applicationNumber);
            fastStringBuffer.append("/");
        }
        if (_directActionName != null) {
            fastStringBuffer.append(AWConcreteApplication.DirectActionRequestHandlerKey);
            fastStringBuffer.append("/");
            fastStringBuffer.append(_directActionName);
            if (_directActionClassName != null) {
                fastStringBuffer.append("/");
                fastStringBuffer.append(_directActionClassName);
            }
        }
        if ((_queryStringValues != null) && !_queryStringValues.isEmpty()) {
            fastStringBuffer.append("?");
            Iterator keyEnumerator = _queryStringValues.keySet().iterator();
            while (true) {
                String currentQueryStringKey = (String)keyEnumerator.next();
                String currentQueryStringValue = _queryStringValues.get(currentQueryStringKey).toString();
                fastStringBuffer.append(currentQueryStringKey);
                fastStringBuffer.append("=");
                currentQueryStringValue = AWUtil.encodeString(currentQueryStringValue);
                fastStringBuffer.append(currentQueryStringValue);
                if (keyEnumerator.hasNext()) {
                    fastStringBuffer.append("&");
                }
                else {
                    break;
                }
            }

        }

        return fastStringBuffer.toString();
    }

    public String finishUrl ()
    {
        String urlString = composeUrl();
        checkIn();
        return urlString;
    }

    /*  --------------
        Utility
        -------------- */

    private static String fullAdaptorUrlForRequestContext (AWRequestContext requestContext, boolean useSecureScheme)
    {
        String adaptorUrl = null;
        AWApplication application = requestContext.application();
        if (useSecureScheme) {
            adaptorUrl = application.adaptorUrlSecure();
        }
        else {
            adaptorUrl = application.adaptorUrl();
        }
        return adaptorUrl;
    }

    public static String fullAdaptorUrlForRequestContext (AWRequestContext requestContext)
    {
        boolean useSecureScheme = true;
        AWRequest request = requestContext.request();
        if (request != null) useSecureScheme = request.isSecureScheme();
        return fullAdaptorUrlForRequestContext(requestContext,
                                               useSecureScheme);
    }

    /*  --------------
        Convenience
        -------------- */

    // Note: It is not an absolute requirement that, once a shared url has been checked out, it be checked back in again.
    // If a url is never checked back in, it will simply be garbarge collected later and a new one will be created in its place.
    // The upshot of this is that you don't need to use try/finally when checking out a shared url.

    public void checkIn ()
    {
        checkinUrl(this);
    }

    public void setRequestContext (AWRequestContext requestContext)
    {
        if (requestContext != null) {
            HttpSession existingHttpSession = requestContext.existingHttpSession();
            if (existingHttpSession != null) {
                setSessionId(existingHttpSession.getId());
                setResponseId(requestContext.responseId());
            }
            AWEncodedString frameName = requestContext.frameName();
            if (frameName != null) {
                setFrameName(frameName);
            }
            if (requestContext.request() != null) {
                String applicationNumber = requestContext.request().applicationNumber();
                setApplicationNumber(applicationNumber);
            }
        }
    }

    /* ------------------------------------
        urlForDirectAction
        ----------------------------------- */
    private static AWDirectActionUrl _urlForDirectAction (String directActionName, AWRequestContext requestContext)
    {
        AWDirectActionUrl directActionUrl = checkoutUrl();
        directActionUrl.setDirectActionName(directActionName);
        directActionUrl.setRequestContext(requestContext);
        directActionUrl.decorateUrl(requestContext);
        return directActionUrl;
    }

    public static String defaultAppUrl (AWRequestContext requestContext)
    {
        return urlForDirectAction(null, null, requestContext);
    }

    public static String urlForDirectAction (String directActionName, AWRequestContext requestContext)
    {
        return urlForDirectAction(directActionName, null, requestContext);
    }

    public static String urlForDirectAction (String directActionName, String directActionClassName, AWRequestContext requestContext)
    {
        AWDirectActionUrl directActionUrl = _urlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        return directActionUrl.finishUrl();
    }

    public static String urlForDirectAction (String directActionName, String directActionClassName, AWRequestContext requestContext, Map parameters)
    {
        AWDirectActionUrl directActionUrl = _urlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        Iterator keys = parameters.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            directActionUrl.put((String)key, (String)parameters.get(key));
        }
        return directActionUrl.finishUrl();
    }

    public static String urlForDirectAction (String directActionName, AWRequestContext requestContext, String key, String value)
    {
        return urlForDirectAction(directActionName, null, requestContext, key, value);
    }

    public static String urlForDirectAction (String directActionName, String directActionClassName, AWRequestContext requestContext, String key, String value)
    {
        AWDirectActionUrl directActionUrl = _urlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        directActionUrl.put(key, value);
        return directActionUrl.finishUrl();
    }

    public static String urlForDirectAction (String directActionName, AWRequestContext requestContext, String key1, String value1, String key2, String value2)
    {
        return urlForDirectAction (directActionName, null, requestContext, key1, value1, key2, value2);
    }

    public static String urlForDirectAction (String directActionName, String directActionClassName, AWRequestContext requestContext, String key1, String value1, String key2, String value2)
    {
        AWDirectActionUrl directActionUrl = _urlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        return directActionUrl.finishUrl();
    }

    /* ------------------------------------
        fullUrlForDirectAction
        ----------------------------------- */
    private static AWDirectActionUrl _fullUrlForDirectAction (String directActionName, AWRequestContext requestContext)
    {
        AWDirectActionUrl directActionUrl = _urlForDirectAction(directActionName, requestContext);
        String fullAdaptorUrl = fullAdaptorUrlForRequestContext(requestContext);
        directActionUrl.setAdaptorUrl(fullAdaptorUrl);
        return directActionUrl;
    }

    private static AWDirectActionUrl _fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, boolean useSecureScheme)
    {
        AWDirectActionUrl directActionUrl = _urlForDirectAction(directActionName, requestContext);
        String fullAdaptorUrl = fullAdaptorUrlForRequestContext(requestContext, useSecureScheme);
        directActionUrl.setAdaptorUrl(fullAdaptorUrl);
        return directActionUrl;
    }

    public static String fullDefaultAppUrl (AWRequestContext requestContext, Map parameters)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(null, requestContext);
        if (parameters != null) {
            Iterator keys = parameters.keySet().iterator();
            while (keys.hasNext()) {
                Object key = keys.next();
                directActionUrl.put((String)key, (String)parameters.get(key));
            }
        }
        return directActionUrl.finishUrl();
    }

    public static String fullDefaultAppUrl (AWRequestContext requestContext, String key, String value)
    {
        return fullUrlForDirectAction(null, requestContext, key, value);
    }

    public static String fullDefaultAppUrl (AWRequestContext requestContext,
                                            String key1, String value1,
                                            String key2, String value2)
    {
        return fullUrlForDirectAction(null, requestContext, key1, value1, key2, value2);
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, boolean useSecureScheme)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext, useSecureScheme);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, String key, String value)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.put(key, value);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, String key1, String value1, String key2, String value2)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, String key1, String value1, String key2, String value2, String key3, String value3)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, AWRequestContext requestContext, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        directActionUrl.put(key6, value6);
        return directActionUrl.finishUrl();
    }

    /* ------------------------------------
        fullUrlForDirectAction (with classname)
        ----------------------------------- */
    public static String fullUrlForDirectAction (String directActionName, String directActionClassName, AWRequestContext requestContext, Map parameters)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        if (parameters != null) {
            Iterator keys = parameters.keySet().iterator();
            while (keys.hasNext()) {
                Object key = keys.next();
                directActionUrl.put((String)key, (String)parameters.get(key));
            }
        }
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, String directActionClassName, AWRequestContext requestContext, String key1, String value1)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        directActionUrl.put(key1, value1);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName, String directActionClassName, AWRequestContext requestContext, String key1, String value1, String key2, String value2)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName,
                                                 String directActionClassName,
                                                 AWRequestContext requestContext,
                                                 String key1,
                                                 String value1,
                                                 String key2,
                                                 String value2,
                                                 String key3,
                                                 String value3)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName,
                                                 String directActionClassName,
                                                 AWRequestContext requestContext,
                                                 String key1,
                                                 String value1,
                                                 String key2,
                                                 String value2,
                                                 String key3,
                                                 String value3,
                                                 String key4,
                                                 String value4)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectAction (String directActionName,
                                                 String directActionClassName,
                                                 AWRequestContext requestContext,
                                                 String key1,
                                                 String value1,
                                                 String key2,
                                                 String value2,
                                                 String key3,
                                                 String value3,
                                                 String key4,
                                                 String value4,
                                                 String key5,
                                                 String value5)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectAction(directActionName, requestContext);
        directActionUrl.setDirectActionClassName(directActionClassName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        return directActionUrl.finishUrl();
    }
    /* ------------------------------------
        fullUrlForDirectActionToApplication
        ----------------------------------- */
    public static AWDirectActionUrl _fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName)
    {
        AWDirectActionUrl directActionUrl = checkoutUrl();
        String fullAdaptorUrl = fullAdaptorUrlForRequestContext(requestContext);
        directActionUrl.setAdaptorUrl(fullAdaptorUrl);
        directActionUrl.setApplicationName(applicationName);
        directActionUrl.setDirectActionName(directActionName);
        return directActionUrl;
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key, String value)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key, value);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        directActionUrl.put(key6, value6);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        directActionUrl.put(key6, value6);
        directActionUrl.put(key7, value7);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7, String key8, String value8)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        directActionUrl.put(key6, value6);
        directActionUrl.put(key7, value7);
        directActionUrl.put(key8, value8);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7, String key8, String value8, String key9, String value9)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        directActionUrl.put(key6, value6);
        directActionUrl.put(key7, value7);
        directActionUrl.put(key8, value8);
        directActionUrl.put(key9, value9);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7, String key8, String value8, String key9, String value9, String key10, String value10)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        directActionUrl.put(key5, value5);
        directActionUrl.put(key6, value6);
        directActionUrl.put(key7, value7);
        directActionUrl.put(key8, value8);
        directActionUrl.put(key9, value9);
        directActionUrl.put(key10, value10);
        return directActionUrl.finishUrl();
    }

    /* ------------------------------------
        fullUrlForDirectActionToApplication
        with external control over the
        isSecureScheme
        ----------------------------------- */
    public static AWDirectActionUrl _fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, boolean useSecureScheme)
    {
        AWDirectActionUrl directActionUrl = checkoutUrl();
        String fullAdaptorUrl = fullAdaptorUrlForRequestContext(requestContext, useSecureScheme);
        directActionUrl.setAdaptorUrl(fullAdaptorUrl);
        directActionUrl.setApplicationName(applicationName);
        directActionUrl.setDirectActionName(directActionName);
        return directActionUrl;
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, boolean useSecureScheme)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName, useSecureScheme);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, boolean useSecureScheme, String key, String value)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName, useSecureScheme);
        directActionUrl.put(key, value);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, boolean useSecureScheme, String key1, String value1, String key2, String value2)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName, useSecureScheme);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, boolean useSecureScheme, String key1, String value1, String key2, String value2, String key3, String value3)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName, useSecureScheme);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        return directActionUrl.finishUrl();
    }

    public static String fullUrlForDirectActionToApplication (String directActionName, AWRequestContext requestContext, String applicationName, boolean useSecureScheme, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4)
    {
        AWDirectActionUrl directActionUrl = _fullUrlForDirectActionToApplication(directActionName, requestContext, applicationName, useSecureScheme);
        directActionUrl.put(key1, value1);
        directActionUrl.put(key2, value2);
        directActionUrl.put(key3, value3);
        directActionUrl.put(key4, value4);
        return directActionUrl.finishUrl();
    }

    /* ------------------------------------
    utility to concatenate Direct Action Name / Class to a base direct action URL
    base direct action URL defined as everything in the URL up to the
    AWConcreteApplication.DirectActionRequestHandlerKey
    ----------------------------------- */
    public static String concatenateDirectActionUrl (String baseUrl, String directActionName)
    {
        return concatenateDirectActionUrl(baseUrl, directActionName, null);
    }

    public static String concatenateDirectActionUrl (String baseUrl, String directActionName,
                                                     String directActionClassName)
    {
        FastStringBuffer fastStringBuffer = new FastStringBuffer();
        fastStringBuffer.append(baseUrl);
        if (!baseUrl.endsWith("/")) {
            fastStringBuffer.append("/");
        }
        fastStringBuffer.append(directActionName);
        if (directActionClassName != null) {
            fastStringBuffer.append("/");
            fastStringBuffer.append(directActionClassName);
        }
        return fastStringBuffer.toString();
    }

    /**
     * Identify if URL is a will direct back to this same application.  (i.e. is it a candidate for internal dispatch)
     */
    private static final String AD_Suffix = 
        Fmt.S("/%s/", AWConcreteApplication.DirectActionRequestHandlerKey);

    public static boolean isLocalDirectActionUrl (String url, AWRequestContext requestContext)
    {
        String base = AWRequestUtil.applicationBaseUrl(requestContext);
        int baseLength = base.length();
        if(url.startsWith(base)) {
            return     // same as base http://s/app/Main
                   url.equals(base)|| 
                       // direct action http://s/app/Main/ad/actionName
                   url.regionMatches(baseLength, AD_Suffix, 0, AD_Suffix.length()) ||
                       // default action with query string http://s/app/Main?param=value
                   url.charAt(baseLength) == '?' ||
                       // default action with query string http://s/app/Main/?param=value
                   url.regionMatches(baseLength, "/?", 0, 2);
        }
        return false;
    }
}
