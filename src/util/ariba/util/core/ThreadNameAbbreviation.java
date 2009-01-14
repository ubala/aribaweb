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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadNameAbbreviation.java#4 $
*/

package ariba.util.core;

/**
    Maintain short names for threads.  The short names are
    abbreviations used in the log to name threads. For Hashthreads,
    the short name is stored in a field of the Hashthread.  Names for
    other threads are kept in a table indexed by the thread's hash
    code.  The first time a short name is requested, it is returned as
    "<shortName>=<longName>".

    This assumes that the hashcodes of different threads are unlikely
    to be the same.  Perhaps we should also check the name when using
    the hashcode.

    We should periodically clean out the table.

    @aribaapi private
*/
public class ThreadNameAbbreviation
{
    /*
        Since threads are a VM wide resource and not just application
        wide, these statics are fine. java.lang.Thread does a similar
        thing for it's automatic thread naming scheme...
    */

    /*
        Don't use java's thread local storage because then threads are
        not able to ask for the name of another thread (which is
        useful when reporting deadlocks.)
    */
    private static final GrowOnlyHashtable THREAD_DESCS =
        new GrowOnlyHashtable();
    private static int THREAD_NUMBER = 0;

    /**
        Return the short name of the current thread.
    */
    public static String getName ()
    {
        Thread thread = Thread.currentThread();
        return getName(thread);
    }

    /**
        Return the short name of the given thread.  For hashthreads,
        the short name is stored in a field on the hashthread.  Names
        for other threads are kept in a table indexed by the thread's
        hash code.  The first time a short name is requested, it is
        returned as "<shortName>=<longName>".
    */
    public static String getName (Thread thread)
    {
        Integer key = Constants.getInteger(thread.hashCode());

        String shortName = (String)THREAD_DESCS.get(key);
        if (shortName == null) {
                // synchronize and add it. While unlikely, one thread
                // could call to ask for another thread's id, so we
                // need to make sure it doesn't get an ID from
                // multiple threads at once.
            synchronized (THREAD_DESCS) {
                shortName = (String)THREAD_DESCS.get(key);
                if (shortName == null) {
                    int next;
                    next = ++THREAD_NUMBER;
                    
                    shortName = Fmt.S("T%s", next);
                    THREAD_DESCS.put(key, shortName);
                    return Fmt.S("%s[%s tabled]=%s", shortName,
                                 Constants.getInteger(THREAD_DESCS.size()),
                                 thread.getName());
                }
            }
        }
        return shortName;
    }
}

