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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWPollInterval.java#12 $
*/

package ariba.ui.aribaweb.core;

public class AWPollInterval extends AWComponent
{
    public static final String AWPollSenderId = "awpoll";
    public static final String AWPollUpdateSenderId = "awpollupdate";
    public static final int FrequentPollInterval = 20;

    public int getInterval ()
    {
        return page().getPollInterval();
    }

    /**
     * Do we in general require a AWPollInterval to be available on the page for use.
     * So if there are any possibly active AWChangeNotifiers or if Notification is enabled
     * then render the AWPollInterval for possible use.
     * @aribaapi private
     */
    public boolean requiresChangeUpdate ()
    {
        return page().hasChangeNotifier() || AWConcreteApplication.IsNotificationEnabled;
    }

    /**
     * Initiate the AWPollInterval on this page based on the flag in AWPage -- actually
     * kicks off the polling.
     */
    public boolean getInitiatePolling ()
    {
        return page().isPollingInitiated();
    }

    public boolean pollOnError ()
    {
        return page().pollOnError();
    }
}
