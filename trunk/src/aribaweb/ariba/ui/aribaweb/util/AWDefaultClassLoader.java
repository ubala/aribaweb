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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWDefaultClassLoader.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ClassUtil;
import java.io.File;

/**
    This default class loader is used in production environments.  It is optimized
    for performance, not for rapid turnaround.
*/
public final class AWDefaultClassLoader implements AWClassLoader
{
    public Class getClass (String className) throws ClassNotFoundException
    {
        return ClassUtil.classForName(className, false);
    }

    public boolean isReloadable (String name)
    {
        return false; 
    }

    public String getComponentNameForClass (String className)
    {
        return className;
    }
        
    /**
        By default, don't compile
    */
    public boolean compile (Class cls, AWResource source)
    {
        return false;
    }

    public void checkForUpdates ()
    {
        // ignore
    }
    
    /*
        Return the File containing this class, or null if it cannot be found.
    */
    public File getClassFile (String className)
    {
        return null;
    }
}