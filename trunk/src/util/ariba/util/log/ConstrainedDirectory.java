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

    $Id: //ariba/platform/util/core/ariba/util/log/ConstrainedDirectory.java#8 $
*/

package ariba.util.log;

import java.io.File;

/**
    A subclass Directory whose size can be "constrained": methods are provided
    to purge the contents of the directory until the size is reduced to a specified
    value in megabytes.
    
    @aribaapi ariba
*/
public class ConstrainedDirectory extends Directory
{
    private Logger logger;
    
    public ConstrainedDirectory (File path)
    {
        this(path, (Logger)null);
    }

    public ConstrainedDirectory (File path, Logger logger)
    {
        super(path);
        this.logger = logger;
    }

    public ConstrainedDirectory (String path)
    {
        this(path, (Logger)null);
    }

    public ConstrainedDirectory (String path, Logger logger)
    {
        super(path);
        this.logger = logger;
    }

    public ConstrainedDirectory (String path, String file)
    {
        this(path, file, (Logger)null);
    }

    public ConstrainedDirectory (String path, String file, Logger logger)
    {
        super(path, file);
        this.logger = logger;
    }

    public ConstrainedDirectory (File path, String file)
    {
        this(path, file, (Logger)null);
    }

    public ConstrainedDirectory (File path, String file, Logger logger)
    {
        super(path, file);
        this.logger = logger;
    }

    /**
        negative numbers indicate unlimited
    */
    private int maxMegaBytes = -1;

    /**
        Sets the max size of this contrained directory in mega bytes.
        @param maxMegaBytes the max size of this contrained directory in mega bytes
        @aribaapi ariba
    */
    public void setMaxMegaBytes (int maxMegaBytes)
    {
        if (maxMegaBytes > 0) {
            this.maxMegaBytes = maxMegaBytes;
        }
    }

    int maxMegaBytes()
    {
        return this.maxMegaBytes;
    }

    /**
        negative numbers indicate unlimited. not implemented until
        there is a way to conveniently determine how much disk space
        is available
    */
    private int maxPrecentFull = -1;

    void setMaxPrecentFull(int maxPrecentFull)
    {
            // minimum of 5%
        if (maxPrecentFull > 0) {
            maxPrecentFull = Math.max(maxPrecentFull, 5);
            this.maxPrecentFull = maxPrecentFull;
        }
    }

    int maxPrecentFull()
    {
        return this.maxPrecentFull;
    }

    /**
        Purges this constrained directory of any bytes in
        excess of the max size allowed.
        @return the number of Mega bytes purged.
        @aribaapi ariba
    */
    public int purgeOnConstraint ()
    {
        int megaBytesToPurge = this.testConstraints();
        if (megaBytesToPurge > 0) {
            return this.purge(megaBytesToPurge);
        }
        return 0;
    }

    /**
        returns the number of mega bytes to purge
    */
    public int testConstraints ()
    {
        if (this.maxMegaBytes < 0) {
            return 0;
        }

        boolean success = this.scan();
        long difference = 0;
        int diffMegaBytes = 0;
        if (success) {
            difference = this.sumBytes() - this.maxMegaBytes * Directory.BytesPerMegaByte;
            if (difference > 0) {
                /* round up to the nearest MegaBytes */
                diffMegaBytes = (int)(difference/Directory.BytesPerMegaByte) + 1;
            }
        }

        if (logger != null) {
            logger.debug(
                "ConstrainedDirectory: number of MegaBytes that can be purged = %s",
                diffMegaBytes);
        }
        return diffMegaBytes;
    }

    /**
        Purges a specified number of bytes (in megabytes)
        from this constrained directory
        @param megaBytesToPurge the size in Mega Bytes to purge
        @return the number of Mega bytes purged
        @aribaapi ariba
    */
    public int purge (int megaBytesToPurge)
    {
        this.sortOnDate();
        long bytesPurged = 0;
        long bytesToPurge = megaBytesToPurge * Directory.BytesPerMegaByte;
        for (int idx = 0; idx < this.listing().size(); idx++)
        {
            File cursor = (File)this.listing().get(idx);
            long bytesInFile = cursor.length();
            if (cursor.delete()) {
                if (logger != null) {
                    logger.debug("deleted %s", cursor);
                }
                bytesPurged += bytesInFile;
                if (bytesPurged >= bytesToPurge) {
                    break;
                }
            }
        }
        return (int)(bytesPurged / Directory.BytesPerMegaByte);
    }
}
