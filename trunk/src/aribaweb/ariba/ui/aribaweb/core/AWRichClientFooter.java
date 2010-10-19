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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRichClientFooter.java#10 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.Log;

public final class AWRichClientFooter extends AWComponent
{
    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        super.renderResponse(requestContext, component);
        if (Log.domsync.isDebugEnabled()) {
            AWSession session = requestContext.session(false);
            if (session != null) {
                Log.domsync.debug("history state -- pos:%s length:%s",
                                  session().historyPosition(),
                                  session().historyLength());
            }
            else {
                Log.domsync.debug("history state -- no session");
            }
        }
    }

    public boolean isDebuggingEnabled ()
    {
        return Log.aribaweb.isDebugEnabled();
    }

    public int getHistoryPosition ()
    {
        return requestContext().isMetaTemplateMode() ? 0 : session().historyPosition();
    }

    public int getHistoryLength ()
    {
        return requestContext().isMetaTemplateMode() ? 0 : session().historyLength();
    }

    public boolean omitHistory ()
    {
        return booleanValueForBinding(AWBindingNames.sessionless) ||
               requestContext().isStaticGeneration() ||
               AWRecordingManager.isInPlaybackMode(requestContext()); 
    }

    public boolean ignoreRefreshComplete ()
    {
        return AWConcreteServerApplication.IsDebuggingEnabled &&
            (requestContext().isPollUpdateRequest() ||
             requestContext().get(AWRequestContext.IgnoreRefreshCompleteKey) != null);
    }
}
