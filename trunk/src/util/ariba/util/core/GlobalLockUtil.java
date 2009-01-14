/**
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/GlobalLockUtil.java#1 $

    Responsible: sjohnson
*/

package ariba.util.core;
import ariba.util.log.Log;

/**
    A helper class for acquiring and releasing global lock
    @aribaapi ariba
*/
public class GlobalLockUtil
{
	public static GlobalLock acquireGlobalLock (String lockName, 
									long acquireTimeOut, long expiryTime)
    {
    	GlobalLockingService lockService = null;
    	GlobalLock lock = null;
        
        try {
        	Log.util.debug("Getting global lock %s", lockName);
            lockService= GlobalLocking.getService();
            lock = lockService.acquireLock(lockName, acquireTimeOut);
            if (lock != null) {
            	lock.setExpirationTime(expiryTime);
            }
        }
        catch (GlobalLockingException ex) {
            Log.util.error(Fmt.S("Error acquiring global lock for %s: %s",
                                 lockName, SystemUtil.stackTrace(ex)));
        }
        
        return lock;
    }
	
	public static boolean releaseGlobalLock (GlobalLock lock)
    {
        try {
            lock.release();
            return true;
        }
        catch (GlobalLockingException ex) {
            Log.util.error(SystemUtil.stackTrace(ex));
            return false;
        }
    }

}
