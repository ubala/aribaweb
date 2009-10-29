/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/shutdown/RequestShutdownDelayer.java#4 $
*/

package ariba.util.shutdown;

import ariba.util.core.MapUtil;
import ariba.util.core.SetUtil;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/**
     Generic shutdown delayer that allows short running requests to register themselves and
     delay a shutdown. A timeout can be given as well.

     @aribaapi ariba
*/
public class RequestShutdownDelayer implements ShutdownDelayer
{
    private static RequestShutdownDelayer instance = new RequestShutdownDelayer();

    private Map requests = MapUtil.map();
    private boolean shuttingDown = false;

    private RequestShutdownDelayer ()
    {
        ShutdownManager.addShutdownDelayer(this);
    }

    public void initiateShutdown ()
    {
        shuttingDown = true;
    }

    public synchronized boolean canShutdown ()
    {
        return requests.isEmpty() || cleanupExpiredRequests();
    }

    public void cancelShutdown ()
    {
        shuttingDown = false;
    }
    
    private boolean cleanupExpiredRequests ()
    {
        Set toRemove = SetUtil.set();
        for (Iterator it = requests.keySet().iterator();it.hasNext();) {
            Object key = it.next();
            Request r = (Request)requests.get(key);
            if (r.isExpired()) {
                toRemove.add(key);
            }
            else {
                ariba.util.log.Log.shutdown.info(10338, r.name);
            }
        }
        for (Iterator it = toRemove.iterator();it.hasNext();) {
            requests.remove(it.next());
        }
        return requests.isEmpty();
    }


    public static boolean isShuttingDown ()
    {
        return instance.shuttingDown;
    }

    public static synchronized void add (Object key, String name, long timeout)
    {
        instance.requests.put(key, new Request(name, timeout));
    }

    public static synchronized void remove (Object key)
    {
        instance.requests.remove(key);
    }

    public static RequestShutdownDelayer getInstance ()
    {
        return instance;
    }

    private static class Request {
        private String name;
        private long expires;

        public Request (String name, long timeout)
        {
            this.name = name;
            if (timeout != 0) {
                expires = System.currentTimeMillis()+timeout;
            }
            else {
                expires = -1;
            }
        }

        protected boolean isExpired ()
        {
            return expires > 0 && System.currentTimeMillis() >= expires;
        }
    }
}
