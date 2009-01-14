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

    $Id: //ariba/platform/util/core/ariba/util/shutdown/ShutdownSignalHandler.java#1 $
*/

package ariba.util.shutdown;

import ariba.util.core.ListUtil;
import ariba.util.core.SignalHandler;
import ariba.util.log.Log;
import java.util.List;


class ShutdownSignalHandler extends SignalHandler
{
    private final ShutdownManager _shutdownManager;
    private final List/*<String>*/ _signalsForAsyncShutdown = ListUtil.list();
    private final List/*<String>*/ _signalsForSyncShutdown = ListUtil.list();

    public ShutdownSignalHandler (ShutdownManager shutdownManager)
    {
        _shutdownManager = shutdownManager;
        setFollowSignalChain(false);
    }
    
    synchronized void registerForAsyncShutdown (String signalName)
    {
        ListUtil.addElementIfAbsent(_signalsForAsyncShutdown, signalName);
        _signalsForSyncShutdown.remove(signalName);
        SignalHandler.registerSignalHandler(this, signalName);
    }
    
    synchronized void registerForSyncShutdown (String signalName)
    {
        ListUtil.addElementIfAbsent(_signalsForSyncShutdown, signalName);
        _signalsForAsyncShutdown.remove(signalName);
        SignalHandler.registerSignalHandler(this, signalName);
    }
    
    protected void handle (String signalName)
    {        
        if (_signalsForAsyncShutdown.contains(signalName)) {
                // ShutdownSignalHandler handle method called, signal is {0}.
            Log.shutdown.info(9424, "async");
            _shutdownManager.shutdown(0, _shutdownManager.SignalKey);
        }
        else if (_signalsForSyncShutdown.contains(signalName)) {
            Log.shutdown.info(9424, "sync");
            _shutdownManager.forceShutdown(0);
        }
        else {
            Log.shutdown.info(9424, "not one of ours!");
        }
    }
}
