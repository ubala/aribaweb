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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWFileDownload.java#3 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBaseResponse;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWFileDownloadStatusCheck;

public final class AWFileDownload extends AWComponent
{
    private static final String CompleteAction = "completeAction";
    public boolean _hasCompleteAction = false;

    protected void awake ()
    {
        _hasCompleteAction = hasBinding(CompleteAction);
        super.awake();
    }

    protected void sleep ()
    {
        _hasCompleteAction = false;
        super.sleep();
    }

    public AWResponseGenerating downloadContentAction ()
    {
        if (_hasCompleteAction) {
            requestContext().setResponseCompleteCallback(new FileDownloadCallback(session()));
        }
        AWResponseGenerating action = (AWResponseGenerating)
                valueForBinding(AWBindingNames.action);
        return action;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWFileDownloadStatusCheck.FileStatusHandler handler =
                AWFileDownloadStatusCheck.createHandlerForSession(session());
        handler.setDownloadStarted();
        if (AWConcreteServerApplication.IsDebuggingEnabled && _hasCompleteAction) {
            requestContext.put(AWRequestContext.IgnoreRefreshCompleteKey, Boolean.TRUE);
        }
        super.renderResponse(requestContext, component);
    }

    public String statusCheckUrl ()
    {
        return ariba.ui.aribaweb.core.AWFileDownloadStatusCheck.statusCheckUrl(requestContext());
    }

    public static class FileDownloadCallback
            implements AWBaseResponse.AWResponseCompleteCallback
    {
        private String _id;

        public FileDownloadCallback (AWSession session)
        {
            _id = session.httpSession().getId();
        }

        public void responseCompleted ()
        {
            AWFileDownloadStatusCheck.FileStatusHandler handler =
                    AWFileDownloadStatusCheck.getHandlerForSessionId(_id);
            if (handler != null) {
                handler.setDownloadCompleted();
            }
        }
    }
}