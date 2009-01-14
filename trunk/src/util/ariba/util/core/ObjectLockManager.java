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

    $Id: //ariba/platform/util/core/ariba/util/core/ObjectLockManager.java#5 $
*/

package ariba.util.core;

import java.util.Map;

/**
    This class maintains a set of objects that can be exclusively
    locked. There are two methods: lock and unlock. The call to
    "lock(o)" doesn't return until an exclusive lock is set on object
    o; the call to "unlock(o)" releases the lock on o.

    @aribaapi private
*/

public class ObjectLockManager 
{
    private final Map objects = MapUtil.map();

    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>ObjectLockManager</code>.

        @aribaapi private
    */
    public ObjectLockManager ()
    {
    }

    /**
        Sets an exclusive lock on the specified object. This method
        doesn't return until it acquires a lock for the object.
        
        @param o - the object on which to set a lock

        @aribaapi private
    */
    public void lock (Object o)
    {
        synchronized (objects) {
            while (true) {
                if (objects.get(o) == null) {
                    objects.put(o, o);
                    return;
                }
                
                try {
                    objects.wait();
                }
                catch (InterruptedException e) {
                }
            }
        }
    }

    /**
        Releases the lock on the specified object. This method asserts
        that the specified object is already locked.
        
        @param o - the object whose lock should be released

        @aribaapi private
    */
    public void unlock (Object o)
    {
        synchronized (objects) {
            Assert.that(objects.get(o) != null,
                        "ObjectLockManager.unlock called on unlocked object %s.",
                        o);
            objects.remove(o);
            objects.notifyAll();
        }
    }
    
}
