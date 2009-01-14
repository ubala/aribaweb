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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWClassLoader.java#8 $
*/

package ariba.ui.aribaweb.util;

import java.io.File;

public interface AWClassLoader
{
    public static final String ClassReloadedTopic = "AWClassesReloaded";

    /**
        Load class.

        @return null if the class is not found, the class (or a
            suitable subclass) otherwise
    */
    public Class getClass (String className) throws ClassNotFoundException;

    /**
        @return true if the class might be reloaded, so should not be cached.
    */
    public boolean isReloadable (String name);

    /**
        @param className the name of the class for the component.
        @return The name of the component.

        The className may be that of a subclass of the actual class, not the
        real className.
    */
    public String getComponentNameForClass (String className);


    /**
        @param source The source code
        @return true if a compile was attempted, false if the ClassLoader doesn't
          do compiles.
    */
    public boolean compile (Class cls, AWResource source);


    public void checkForUpdates ();

    /*
        Return the File containing this class, or null if it cannot be found.
    */
    public File getClassFile (String className);
}
