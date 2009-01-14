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

    $Id: //ariba/platform/util/core/ariba/util/core/DebugState.java#5 $
*/

package ariba.util.core;

/**
    DebugState is used on conjunction with the ThreadDebugState class
    to print out information on the current application state when
    problems occur, or for general debugging.
    @aribaapi documented
    
    @see ThreadDebugState
*/
public interface DebugState
{
    /**
        Returns an object that will be toStringed when the debug
        information needs to be printed. This method will only be
        called when the information is to be printed out - which will
        be rare. This method may safely be slow. It is also ok for the
        value that would be returned to change over time, even after
        the time that it is intially set in ThreadDebugState.
       
        <p>
        The most common implementation is to create a new Map
        and set key and value pairs of information in that Map
        which it will then return.

        @return an object that encapsulates the state of the current
        application. The toString() method of the returned object
        should be made safe even if the object that implements
        DebugState is being modified at the time. Exceptions from
        toString are tollerable in this case, but data corruption is
        not.

        @see ThreadDebugState#set
        
        @aribaapi documented
    */
    public Object debugState ();
}
