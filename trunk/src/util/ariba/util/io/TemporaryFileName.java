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

    $Id: //ariba/platform/util/core/ariba/util/io/TemporaryFileName.java#4 $
*/

package ariba.util.io;

import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import java.io.File;

/**
    Utility methods for creating temporary filenames.

    @aribaapi ariba
*/
public class TemporaryFileName
    extends TempFile
{
    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

    /** Default file extension to use for temporary files. */
    public static final String TempFileExtension = "tmp";

    /*-----------------------------------------------------------------------
        Private Class Fields
      -----------------------------------------------------------------------*/

    /** Default directory in which temporary files are created. */
    private static File TEMP_DIRECTORY;

    /** Default Extrinsic UID Generator */
    private static ExtrinsicUID ExtrinsicUID = new DefaultExtrinsicUID();

    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Prevents callers from instantiating this class.

        @aribaapi ariba
    */
    private TemporaryFileName ()
    {
    }

    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        Creates a temporary file name with the default location and extension.

        @aribaapi ariba

        @return pathname for the file
    */
    public static String createTemporaryFileName ()
    {
        File tempFile = TempFile.createTempFile(getTempDirectory(),
                                                ExtrinsicUID.getUID(),
                                                TempFileExtension);

        if (tempFile != null) {
            return tempFile.getPath();
        }
        else {
            return null;
        }
    }

    /**
        Creates a temporary file name with the given location, prefix, and
        extension.

        @aribaapi ariba

        @param directory    directory in which to create the file
        @param prefix       filename prefix
        @param extension    file extension
        @return             pathname for the file
    */
    public static String createTemporaryFileName (
        File directory,
        String prefix,
        String extension)
    {
        File tempFile =
            TempFile.createTempFile(directory,
                                    StringUtil.strcat(prefix, ExtrinsicUID.getUID()),
                                    extension);
        if (tempFile != null) {
            return tempFile.getPath();
        }
        else {
            return null;
        }

    }

    /**
        Sets the default directory for temporary files.  If this
        method is never called, the default behavior is to create temporary
        files in the current directory.

        @aribaapi ariba

        @param dir  default directory for temporary files
    */
    public static void setTempDirectory (File dir)
    {
        TEMP_DIRECTORY = dir;
        Assert.that(TEMP_DIRECTORY.exists(), "Temp directory %s does not exist",
            TEMP_DIRECTORY);
    }

    /**
        Gets the default directory for temporary files.

        @aribaapi ariba

        @return default directory for temporary files
    */
    public static File getTempDirectory ()
    {
        if (TEMP_DIRECTORY == null) {
            TEMP_DIRECTORY = SystemUtil.getLocalTempDirectory();
        }
        return TEMP_DIRECTORY;
    }


    /**
        Sets the Extrinsic UID generator

        @aribaapi ariba
    */
    public static void setExtrinsicUID (ExtrinsicUID extrinsicUID)
    {
        if (extrinsicUID != null) {
            synchronized (ExtrinsicUID) {
                ExtrinsicUID = extrinsicUID;
            }
        }
    }

    /**
        Extrinsic UID generator

        @aribaapi ariba
    */
    public interface ExtrinsicUID
    {
        /**
            Returns the extrinsic UID.

            @aribaapi ariba

            @return extrinsic UID
        */
        public String getUID ();
    }
}

/**
    Default Implementation of the unique id generator

    @aribaapi ariba
*/
class DefaultExtrinsicUID implements TemporaryFileName.ExtrinsicUID
{
    public String getUID ()
    {
        return "";
    }
}
