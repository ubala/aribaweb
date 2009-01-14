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

    $Id: //ariba/platform/util/core/ariba/util/io/TrueFilter.java#4 $
*/

package ariba.util.io;

import java.io.File;
import java.io.FilenameFilter;

/**
    A FilenameFilter that always returns true

    @aribaapi private
*/
public final class TrueFilter implements FilenameFilter
{
    /**
        This static is okay because the class has no internal state
    */
    public static final TrueFilter self = new TrueFilter();
    private TrueFilter ()
    {
    }
    public boolean accept (File dir, String name)
    {
        return true;
    }
}
