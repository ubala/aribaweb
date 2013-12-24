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

    $Id: //ariba/platform/util/core/ariba/util/core/GlobalLockingService.java#6 $
*/

package ariba.util.core;

/**
    provide services for acquiring global locks and creating
    lock groups.  Global Locks provide cluster-wide synchronization.

    .  A lock is identified by its name.  The name must be unique.
    .  A lock is represented by a lock object.  There can be only
       one valid (i.e. locked and unexpired) lock object for the lock
       throughout the cluster.
    .  You can obtain a lock only if it is currently unlocked.
    .  A lock is created if it does not exist.  It is created in the
       locked state.
    .  Each lock has a specific lifetime.  If that lifetime is
       exceeded, the lock expires, and becomes unlocked.
    .  A lock is acquired with the default lifetime (currently 2 minutes).
       The lifetime must be extended if the lock is to be held longer
       than that.
    .  If a lock expires, a cleanup notification will be sent when the
       lock is next acquired.  The notification is posted on a local
       topic to the default Notification Center.
    .  All lock operations other than acquiring the lock are performed
       on the lock object.
    .  All operations on an invalid (expired or released) lock object
       throw an exception.
    .  No synchronization is done on any of these methods.  If the user
       wishes to share a lock object between threads on a node, the
       user is responsible for proper synchronization.
    .  An acquired lock can be added to a Lock Group.  Once added,
       the lock can be later acquired through a request to the Lock Group.
    .  When a node goes down, all locks acquired by that node become
       expired.  However, the lock may not become available until the
       node restarts.  This means that a lock that becomes available
       due to a node crashing will always have the cleanup notification
       sent when the lock is next acquired.
    .  On Cluster Restart, Global Locking is initialized (i.e. there are
       no locks, and there are no lock groups).  This is necessary, or
       else creating new Lock Groups with different numbers of locks
       becomes unnecessarily complicated.
       @aribaapi ariba
*/

public interface GlobalLockingService
{
    /**
        Acquire requested GlobalLock.  If the lock does not
        exist, it is created and obtained.  Return immediately if
        the lock is not available.
        @param lockName The name of the lock.  Must be unique
        @return The lock object, or null if the lock is not free
    */
    public GlobalLock acquireLock (String lockName);

    /**
        Aacquire requested GlobalLock with an acquistion timeout.
        If the lock does not exist, it is created and obtained.
        Try to obtain the lock for specified number of milliseconds,
        then return a null if the lock is not available.
        @param lockName The name of the lock.  Must be unique
        @param acquireTimeout The number of milliseconds to try for
        @return The lock object, or null if the lock is not free
    */
    public GlobalLock acquireLock (String lockName, long acquireTimeout);

    /**
        Acquire a lock from the requested lock group.  The lock group
        must exist, and there must be locks assigned to it. Returns
        immediatley if no lock is available
        @param groupName The lock group
        @return The lock object if one is available, null otherwise
        @exception GlobalLockingException thrown if the lock group does
        not exist, or if there are no locks assigned to the group.
    */
    public GlobalLock acquireLockFromGroup (String groupName)
      throws GlobalLockingException;

        /**
        Acquire a lock from the requested lock group.  The lock group
        must exist, and there must be locks assigned to it. Try to obtain
        the lock for acquireTimeOut milliseconds before returning.
        @param groupName The lock group
        @param acquireTimeout The number of milliseconds to try to obtain
        the lock
        @return The lock object if one is available, null otherwise
        @exception GlobalLockingException thrown if the lock group does
        not exist, or if there are no locks assigned to the group.
    */
    public GlobalLock acquireLockFromGroup (String groupName,
                                            long acquireTimeout)
      throws GlobalLockingException;

    /**
        Create a new lock group, and allow numLocks locks to be assigned
        to it.  The name must be unique
        @param groupName The name of the group
        @param numLocks How many locks can be in this group
        @exception GlobalLockingException if the group already exists
    */
    public void createLockGroup (String groupName, int numLocks)
      throws GlobalLockingException;

    /**
        Create a new lockgroup, and a pool of locks for it.  The
        locks are given the name <groupName>Lock<n>, and are added to
        the group.  Each lock is then released.  When this call successfully
        completes, you may immediately acquire locks by calling
        acquireLockFromGroup.
        @param groupName The name of the group to create
        @param numLocks How many locks to create
        @exception GlobalLockingException if the group already exists, or
        if any of the locks associated with the group already exits and are
        assigned to the group.

    */
    public void createLockPool (String groupName, int numLocks)
      throws GlobalLockingException;

    /**
        Delete the specified lock group.  The lock group can be
        deleted only if all the locks in the group are free.  If any
        of the locks are not free, the lockgroup is not deleted.
        
        @param groupName The name of the group to delete
        @exception GlobalLockingException if the group does not exist,
        or if any of the locks within the group are locked or expired.
    */
    public void deleteLockGroup (String groupName)
      throws GlobalLockingException;


    /**
        Returns true if the specified lock is held by the specified node.
        
        @param lockName The name of the lock
        @param nodeName The name of the node
    */
    public boolean nodeHasLock (String lockName, String nodeName);
    
    public final static String CleanupTopic =
        "ariba.util.GlobalLocking.Cleanup";
}
    

