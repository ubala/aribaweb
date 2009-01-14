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

    $Id: //ariba/platform/util/core/ariba/util/core/ServerInterface.java#6 $
*/

package ariba.util.core;

/**
    Interface that defines basic functionalities that
    should be provided by the servers.

    @aribaapi ariba
*/
public interface ServerInterface
{
    // initialization topic

    public final static String TopicStartupComplete =
        "Ariba.RPC.Server.StartupComplete";

    /**
        Call to Terminate the application when a fatal error has been
        encountered.  Server gets restarted when it terminates via the 'fatal'
        exit method.

        @param reason Reason string

        @aribaapi ariba
    */
    public void fatal (String reason);

    /**
        Call to Terminate the application when a fatal error has been
        encountered.  Server does not get restarted when it terminates via
        the 'fatal' exit method.

        @param reason Reason string

        @aribaapi ariba
    */
    public void fatalNoRestart (String reason);

    /**

        Terminate the application when a normal safe exit is required.
        Server does not get restarted when it terminates from here.

        @aribaapi ariba
    */
    public void shutdown ();

    /**
        Get the parameters for this server.

        @aribaapi ariba
    */
    public Parameters getParameters (); 

    /**
        Event loop used for timers
    */
    public EventLoop timerEventLoop ();

    public boolean debug ();
    public boolean isProduction ();

    /**
        Name of the node

        @return the name of the Node or an empty String if not applicable
    */
    public String nodeName ();

    /**
       Returns the main ThreadFactory used by the server
       @return the main ThreadFactory managed by this server
       @aribaapi ariba
    */
    public ThreadFactory getServerThreadFactory ();

    /**
        Returns the named ThreadPool used by this server
    */
    public ThreadPool getThreadPool (String threadPoolName);

}
