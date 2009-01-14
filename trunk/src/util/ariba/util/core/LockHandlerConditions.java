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

    $Id: //ariba/platform/util/core/ariba/util/core/LockHandlerConditions.java#4 $
*/

package ariba.util.core;

/**
    @aribaapi private
*/
public interface LockHandlerConditions
{
    /**
        return false to indicate continuation
        context is allocated by caller to LockHandler.doWithLock and can be used
        to pass, by reference, any number of result data etc.
        Called WITH LOCK
    */
    public boolean doWithLock (LockHandlerContext lockHandlerContext);

    /**
        the time, in milliseconds, to wait
        Called WITH LOCK
    */
    public long timeoutIntervalMillis ();
}
