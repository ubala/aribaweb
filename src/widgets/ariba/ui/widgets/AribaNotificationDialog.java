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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaNotificationDialog.java#6 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWNotification;

import java.util.List;


public class AribaNotificationDialog extends AWComponent
{

    private boolean _hasNotification;
    private boolean _hasNewNotifications;
    private List _notifications;
    public AWNotification _currentNotification;

    public boolean isStateless ()
    {
        return false;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (isNotificationEnabled()) {
            AWPage page = page();
            page.setPollingInitiated(true);
            AWSession session = session(false);
            _hasNewNotifications = session != null ? session.hasNotification() : false;
            if (_hasNewNotifications || !requestContext().isPollUpdateRequest()) {
                // update notification list
                _hasNotification = _hasNewNotifications;
                _notifications = _hasNotification ? session().getNotifications() : null;
            }
        }
        super.renderResponse(requestContext, component);
    }

    public boolean isNotificationEnabled ()
    {
        return ((AWConcreteApplication)application()).IsNotificationEnabled;
    }
    
    public boolean hasNotification ()
    {
        return _hasNotification;
    }

    public boolean hasNewNotifications ()
    {
        return _hasNewNotifications;
    }

    public List notifications ()
    {
        return _notifications;
    }

    public boolean disableCurrentContextLink ()
    {
        return !_currentNotification.hasContext();
    }

    public String currentNotificationFrom ()
    {
        return _currentNotification.getFrom(preferredLocale());
    }

    public AWComponent currentNotificationContext ()
    {
        return _currentNotification.getContextComponent(requestContext());
    }

    public String getCurrentNotificationImportanceClass ()
    {
        String cssClass = null;
        switch (_currentNotification.getImportance()) {
            case AWNotification.HighImportance:
                cssClass = "high";
        }
        return cssClass;
    }

    public String currentSubjectClass ()
    {
        if (disableCurrentContextLink()) {
            return "noLink";
        }
        return null;
    }
}



