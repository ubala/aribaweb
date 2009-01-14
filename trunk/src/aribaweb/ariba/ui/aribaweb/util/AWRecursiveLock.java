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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWRecursiveLock.java#5 $
*/

package ariba.ui.aribaweb.util;

/**
AWRecursiveLock allows for blocking access to a block of code to all threads but the first one to grab access.  Once a thread locks this lock, it can continue running and lock it as many times as necessary (a count is incremented keeping track of the number of times).  Of course, it must balance the number of calls to lock with calls to unlock else a deadlock will occur.
*/
public final class AWRecursiveLock extends AWBaseObject
{
    private Thread _thread;
    private int _count;

    public synchronized void lock ()
    {
        Thread currentThread = Thread.currentThread();
        while (_thread != null && _thread != currentThread) {
            try {
                wait();
            }
            catch (InterruptedException exception) {
                // ignore
                exception = null;
            }
        }
        _thread = currentThread;
        _count++;
    }

    public synchronized void unlock ()
    {
        _count--;
        if (_count < 0 || _thread != Thread.currentThread()) {
            throw new AWGenericException("Unbalanced use of AWRecursiveLock. count: " + _count);
        }
        if (_count == 0) {
            _thread = null;
            notify();
        }
    }
}
