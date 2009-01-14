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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWFileDownloadStatusCheck.java#2 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.util.core.StringUtil;
import ariba.util.core.MapUtil;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.util.Map;

public class AWFileDownloadStatusCheck extends AWDirectAction
{
    private final static String KeySessionId = "sessionId";

    protected boolean shouldValidateSession ()
    {
        return false;
    }

    public AWResponseGenerating pollAction ()
    {
        AWRequest request = requestContext().request();
        String sessionId = request.formValueForKey(KeySessionId);
        FileStatusHandler handler = null;
        if (!StringUtil.nullOrEmptyOrBlankString(sessionId)) {
            handler = getHandlerForSessionId(sessionId);
        }
        String msg = handler == null ? "none" : handler.status();

        AWResponse response = AWConcreteApplication.sharedInstance().createResponse();
        response.appendContent(msg);
        return response;
    }

    public static class FileStatusHandler
    {
        private String _status;

        public void setDownloadStarted ()
        {
            _status = "started";
        }

        public void setDownloadCompleted ()
        {
            _status = "completed";
        }

        public String status ()
        {
            return _status;
        }
    }

    public static String statusCheckUrl (AWRequestContext requestContext)
    {
        String directActionName =
                StringUtil.strcat("poll/",
                        AWFileDownloadStatusCheck.class.getName());
        String sessionId = requestContext.session().httpSession().getId();
        String url = AWDirectActionUrl.fullUrlForDirectAction(directActionName, requestContext,
                KeySessionId, sessionId);
        return url;
    }

    // Need to store outside the session so that we can access without the session lock.
    // Will be accessed by multiple threads -- must protect access.
    private static Map _StatusBySessionId = MapUtil.map();

    public static FileStatusHandler createHandlerForSession (AWSession session)
    {
        HttpSession httpSession = session.httpSession();
        String sessionId = httpSession.getId();
        FileStatusHandler handler = new FileStatusHandler();
        synchronized (_StatusBySessionId) {
            _StatusBySessionId.put(sessionId, handler);
        }

        // register for session time out clean up
        // should only register once. If we replaced the existing value,
        // the handler would be removed by the valueUnbound() on the old instance.
        // according to the spec, The valueBound method must be called before the object is
        // made available via the getAttribute method of the HttpSession interface. The
        // valueUnbound method must be called after the object is no longer available
        // via the getAttribute.  So the order in which methods are invoked is:
        // 1. valueBound() on the new value
        // 2. replace the old value on the session attribute HashMap
        // 3. valueUnbound() on the old value
        String removerKey = StringUtil.strcat(SessionLookupPrefix, sessionId);
        if (httpSession.getAttribute(removerKey) == null) {
            httpSession.setAttribute(removerKey, new KeyRemover(sessionId));
        }
        return handler;
    }

    public static FileStatusHandler getHandlerForSessionId (String sessionId)
    {
        synchronized (_StatusBySessionId) {
            return (FileStatusHandler)_StatusBySessionId.get(sessionId);
        }
    }

    static final String SessionLookupPrefix="AWFDSC-"; // key for our keys in session


    private static class KeyRemover implements HttpSessionBindingListener
    {
        public String _key;

        public KeyRemover (String key)
        {
            _key = key;
        }

        public void valueBound (HttpSessionBindingEvent event) {
            // noop
        }

        public void valueUnbound (HttpSessionBindingEvent event) {
            synchronized (_StatusBySessionId) {
                _StatusBySessionId.remove(_key);
            }
        }
    }
}
