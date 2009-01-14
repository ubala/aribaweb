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

    $Id: //ariba/platform/util/core/ariba/util/io/FileLockManager.java#4 $
*/

package ariba.util.io;

import ariba.util.core.ObjectLockManager;
import java.io.File;
import java.io.IOException;

/**
    Maintains a set of files that can be locked exclusively. The lock
    is actually set on the file's underlying canonical path, not on
    the File object. Thus, two File objects that reference the same
    underlying file will resolve to the same lock. If there's an
    exception when looking up the canonical path, then the underlying
    uncanonical path is used.

    @aribaapi private
*/
public class FileLockManager extends ObjectLockManager
{
    /**
        Sets a lock on the specified file.
        
        @param file - the file to lock

        @aribaapi private
    */
    public void lock (File file)
    {
        super.lock(toPath(file));
    }

    /**
        Releases the lock on the specified file. 
        
        @param file - the file lock should be released

        @aribaapi private
    */
    public void unlock (File file)
    {
        super.unlock(toPath(file));
    }

    private String toPath (File file)
    {
        try {
            return file.getCanonicalPath();
        }
        catch (IOException e) {
            return file.getPath();
        }
    }
}
