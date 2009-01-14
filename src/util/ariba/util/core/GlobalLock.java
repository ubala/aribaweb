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

    $Id: //ariba/platform/util/core/ariba/util/core/GlobalLock.java#4 $
*/

package ariba.util.core;

/**
    Interface for a GlobalLock.  The GlobalLockingService implementation
    will return an instance of one of these for the locking implementation.
    All operations on a lock object (extending the timeout, releasing
    the lock, etc) happen through this interface.
    @aribaapi ariba
*/
public interface GlobalLock
{
    /**
        @return true if this lock is valid (has not expired),
        false otherwise.
    */
    public boolean isValid ();
    
    /**
        @return The name of the lock
    */
    public String getName ();
    
    /**
        @return the group this lock belongs to.  Null if it belongs to
        no group
    */
    public String getGroup ();

    /**
        Add this lock to the specified lock group
        @param groupName The name of the group.  It must already exist
        @exception Throw global locking exception on error.  Errors are
        1) the lock is not valid, 2) the lock group does not exist, and
        3) there is no more room in the group
    */
    public void setGroup (String groupName) throws GlobalLockingException;

    /**
        relelase the lock.
        @exception if the lock is invalid (has expired or was released)
    */
    public void release () throws GlobalLockingException;

    /**
        Set the lifetime of the lock.  This sets the new expiration time
        to the current time plus the number of milliseconds requested.
        @param millisFromNow Milliseconds from now for the new expiration
        @exception GlobalLockingException if the lock is not valid (has
        already expired, or was released)
    */
    public void setExpirationTime (long millisFromNow)
      throws GlobalLockingException;
    
}
