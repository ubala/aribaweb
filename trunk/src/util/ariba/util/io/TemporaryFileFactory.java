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

    $Id: //ariba/platform/util/core/ariba/util/io/TemporaryFileFactory.java#3 $
*/
package ariba.util.io;

import java.io.File;
import java.io.IOException;

/**
    This interface provides isolation to the user from the exact mechanism of generating
    temporary file names. For example, the mechanism may be through the Base APIs or through
    ariba.util.

    @aribaapi ariba
 */
public interface TemporaryFileFactory
{
    /**
        Create a temporary file and return the name which should include the entire path.

        @return A unique name for the file.
        @aribaapi ariba
    */
    public String createTemporaryFileName () throws IOException;

    /**
        Create a temporary file and return the name which should include the entire path.
        Provide some input parameters for the name and location of the file.

        @param directory Name of the directory in which we create the temporary file.
        @param prefix This is prepended to the name of the file.
        @param suffix This is appended to the name of the file.
        @return A unique name for the file.
        @aribaapi ariba
    */
    public String createTemporaryFileName (File directory, String prefix,
                                                    String suffix)  throws IOException;



    /**
        Returns the tempDir

        @return A unique name for the file.
        @aribaapi ariba
    */
    public File getTempDir ()
      throws IOException;
}
