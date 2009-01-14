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

    $Id: //ariba/platform/util/core/ariba/util/io/TemporaryFileFactoryImpl.java#4 $
*/
package ariba.util.io;

import java.io.IOException;
import java.io.File;

/**
    This implementation uses the <code>ariba.channel.io</code> package to generate
    temporary file names.

    @aribaapi ariba
*/
public class TemporaryFileFactoryImpl implements TemporaryFileFactory
{
    /**
        Creates a new TemporaryFileFactoryImpl.

        @aribaapi ariba
    */
    public TemporaryFileFactoryImpl ()
    {
    }

    /**
        Create a temporary file and return the name which should include the entire path.
        The file is created in the current "." directory.
        @aribaapi ariba

        @return The name of the temporary file created.
    */
    public String createTemporaryFileName ()
    {
        return TemporaryFileName.createTemporaryFileName();
    }

    /**
        Create a temporary file and return the name which should include the entire path.
        Provide some input parameters for the name and location of the file.

        @aribaapi ariba
        @param directory Name of the directory in which we create the temporary file.
        @param prefix This is prepended to the name of the file.
        @param suffix This is appended to the name of the file.
        @return String The name of the temporary file created.
    */
    public String createTemporaryFileName (File directory, String prefix, String suffix)
    {
        return TemporaryFileName.createTemporaryFileName(directory,prefix,suffix);
    }

    /**
        Returns the tempDir

        @return String A unique name for the file.
        @aribaapi ariba
    */
    public File getTempDir ()
      throws IOException
    {
        return TemporaryFileName.getTempDirectory();
    }
}
