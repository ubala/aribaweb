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

    $Id: //ariba/platform/util/core/ariba/util/shutdown/ShutdownDelayer.java#3 $
*/

package ariba.util.shutdown;

/**
 * Instances of this interface are invoked during the asynchornous shutdown sequence. 
 * They are responsible for delaying the shutdown until the system is in a state which 
 * allows it.
 * @aribaapi ariba
 */
public interface ShutdownDelayer
{
    /**
     * Method invoked to indicate the beginning of the shutdown sequence.
     * 
     * This is being called by the ShutdownManager, when an asynchronous shutdown is 
     * requested. Within this call, the application starts stopping some functionalities 
     * (for instance, new new user can login on this server)
     * 
     * If any exception is being thrown, the delayer will be removed from the sequence.
     * 
     * @aribaapi ariba
     */
    public void initiateShutdown ();
    
    /**
     * Specifies whether the system can safely shutdown.
     * 
     * This is being called on a regular basis by the ShutdownManager to determine whether
     * we can exit the VM.
     * 
     * If any exception is being thrown, the delayer will be removed from the sequence.
     * 
     * @return <code>true</code> if the system can shutdown
     * @aribaapi ariba
     */
    public boolean canShutdown ();
    
    /**
     * Method invoked to indicate the cancelling of the shutdown sequence.
     * 
     * This method is called by the ShutdownManager, when an asynchronous shutdown is 
     * cancelled.  Within this call, the application should reverse the actions
     * it took in initiateShutdown.
     * 
     * The most common reason for cancelling a shutdown is the start of a rolling
     * upgrade or rolling restart where the Ops code determines when each node is
     * shutdown.
     * 
     * If any exception is being thrown, the delayer will be removed from the sequence.
     * 
     * @aribaapi ariba
     */
    public void cancelShutdown ();
}

