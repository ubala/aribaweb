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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWSessionStatusManager.java#7 $
*/

package ariba.ui.aribaweb.core;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import ariba.util.core.MapUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.StringUtil;
import ariba.util.core.FastStringBuffer;
import ariba.ui.aribaweb.util.Log;

public class AWSessionStatusManager
{
    protected GrowOnlyHashtable _sessionStatusTable;

    public AWSessionStatusManager ()
    {
        _sessionStatusTable = initSessionStatusTable();
    }

    protected GrowOnlyHashtable initSessionStatusTable ()
    {
        // Dummy implementation, just used to convert from GrowOnlyHash to Map
        GrowOnlyHashtable table = new GrowOnlyHashtable(1);
        table.put(this,MapUtil.map());
        return table;
    }

    /**
     * @return map of maps keyed by session instance
     * @aribaapi private
     */
    public GrowOnlyHashtable getSessionStatusTable ()
    {
        return _sessionStatusTable;
    }

    public static AWSessionStatusManager getStatusManager ()
    {
        AWConcreteApplication application =
            (AWConcreteApplication)AWConcreteApplication.sharedInstance();

        return application.getSessionStatusManager();
    }

    private Map getSessionMap ()
    {
        return (Map)_sessionStatusTable.get(this);
    }

    protected void trackSessionConnect (AWSession session)
    {
        getSessionMap().put(session, "connected");
    }

    protected void trackSessionTerminate (AWSession session)
    {
        getSessionMap().remove(session);
    }

    protected void trackSessionPendingDisconnect (AWSession session)
    {
        getSessionMap().put(session, "pending");
    }

    protected void trackSessionReconnect (AWSession session)
    {
        getSessionMap().put(session, "connected");
    }

    public void updateSessionStatusTable (ConcurrentLinkedQueue<AWConcreteApplication.SessionWrapper> connectList, List existingSessions)
    {
        long currTime = System.currentTimeMillis();

        Iterator iterator = existingSessions.iterator();
        
        FastStringBuffer debugBuffer = null;
        boolean isDisconnectDebugEnabled =
            Log.aribaweb_userstatus_disconnectTime.isDebugEnabled();
        if (isDisconnectDebugEnabled) {
            debugBuffer = new FastStringBuffer();
        }

        while (iterator.hasNext()) {
            // check if the session is
            // a) invalidated -- remove from list and send out disconnect
            // b) is connected and has not been accessed in time --
            //       set disconnected and send out disconnect
            // c) is disconnected, but has reconnected --
            //       set connected and send out reconnect
            // d) needs to be added to the DB list
            AWSession session = (AWSession)iterator.next();
            long disconnectTime = session.disconnectTime();
            if (session.isInvalidated()) {
                trackSessionTerminate(session);
                iterator.remove();
            }
            else if (disconnectTime != -1) {
                // If disconnectTime is -1, then we don't know when the session
                // is going to disconnect so don't attempt to update connected state.
                // For these sessions, the only valid states are Connected and
                // Disconnected, there is no pending disconnect or reconnect.
                if (session.isConnected() &&
                    currTime > disconnectTime) {
                    session.setConnected(false);
                    // add to disconnect notification list
                    trackSessionPendingDisconnect(session);
                }
                else if (!session.isConnected() &&
                    currTime < session.disconnectTime()) {
                    session.setConnected(true);
                    trackSessionReconnect(session);
                }
            }

            if (isDisconnectDebugEnabled) {
                debugBuffer.append(session.debugDisconnectString());
                debugBuffer.append("\n");
            }
        }

        if (connectList != null) {
            AWConcreteApplication.SessionWrapper sessOp = null;
            while ((sessOp = connectList.poll()) != null) {
                if (sessOp.op == AWConcreteApplication.SessionOp.Add) {
                    try {
                        trackSessionConnect(sessOp.session);
                        existingSessions.add(sessOp.session);
                    }
                    catch (UserSessionExistsException fae) {
                        //In development mode, always log this error.
                        //In production, log it only if debug is enabled.
                        if (AWConcreteApplication.IsDebuggingEnabled
                            || Log.aribaweb_userstatus.isDebugEnabled())
                        {
                            Log.aribaweb_userstatus.error(fae.getMessage(), sessOp.callTrace);
                        }
                    }
                }
                else {
                    trackSessionTerminate(sessOp.session);
                    existingSessions.remove(sessOp.session);
                }
                if (isDisconnectDebugEnabled) {
                    debugBuffer.append(sessOp.session.debugDisconnectString());
                    debugBuffer.append("\n");
                }
            }
        }

        if (isDisconnectDebugEnabled) {
            String debugString = debugBuffer.toString();
            if (!StringUtil.nullOrEmptyOrBlankString(debugString)) {
                Log.aribaweb_userstatus_disconnectTime.debug(debugString);
                Log.aribaweb_userstatus_disconnectTime.setLevel(Log.WarnLevel);
            }
        }
        // Log.aribaweb_userstatus.debug("Session status table: %s", _sessionStatusTable);
    }

    public void test (AWRequestContext requestContext)
    {
    }

    public static class UserSessionExistsException extends RuntimeException
    {
        public UserSessionExistsException (String msg)
        {
            super(msg);
        }
    }
}
