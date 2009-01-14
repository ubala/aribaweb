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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadManager.java#3 $
*/

package ariba.util.core;

/**
   Interface to allow Ariba thread management. Here are some examples, converting 
   a normal thread to a ObjectServerThread and cleaning up the ObjectServerThread's
   session before the thread is reused.
   
   @aribaapi ariba
*/
public interface ThreadManager
{
    /**
       Convert the current thread to an Ariba thread of your choice 
      
       @aribaapi ariba
    */
    public void setupThread ();
    
    /**
       Cleanup the Ariba thread context before it is reused
      
       @aribaapi ariba
    */
    public void cleanupThread ();
    
    /**
     * Ultra simple implementation: all methods are no-op  
     * @aribaapi ariba
     */
    public static class NoOp implements ThreadManager
    {
        public void setupThread ()
        {
        }

        public void cleanupThread ()
        {
        }        
    }
}
