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

    $Id: //ariba/platform/util/core/ariba/util/log/ArchivingAppender.java#10 $
*/

package ariba.util.log;

import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.i18n.I18NUtil;
import ariba.util.formatter.IntegerFormatter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.helpers.LogLog;

/**
    This class extends org.apache.log4j.FileAppender to provide
    archiving support. It allows one to optionally specify the name of the
    directory of the file to archive and the archiving directory where
    the acrhived file will reside.

    @aribaapi documented
*/
public class ArchivingAppender extends FileAppender
{

    /**
        Property for what the name of the log file will be

        @aribaapi ariba
    */
    public static final String LogFileNameKey = "LogFileName";

    /**
        Property for what the directory of the log file will be

        @aribaapi ariba
    */
    public static final String DirectoryNameKey = "DirectoryName";

    /**
        Property for where the log file will be archive to

        @aribaapi ariba
    */
    public static final String ArchiveDirectoryNameKey = "ArchiveDirectoryName";

    /**
        Property for how large the archive directory can grow
        before being pruned.

        @aribaapi ariba
    */
    public static final String MaxArchiveSizeKey = "MaxArchiveSize";


    /**
        All of the stuff that is crucial to making file building and
        archiving work.
        @aribaapi private
    */
    private static final int DefaultLogMaxMegaBytes = -1;

    /**
        The default encoding to be used.
        @aribaapi private
    */
    public static final String DefaultEncoding = I18NUtil.EncodingUTF_8;

    /**
        List of all instances of this class
        @aribaapi private
    */
    private static List allInstances = ListUtil.list();

    /**
        @aribaapi private
    */
    private ConstrainedDirectory directory;

    /**
        @aribaapi private
    */
    protected ConstrainedDirectory archiveDirectory;

    /**
        @aribaapi private
    */
    private String fileBaseName;

    /**
        @aribaapi private
    */
    private String lastArchivedFileName;

    /**
        @aribaapi private
    */
    private String maxArchiveSize;

    /**
        @aribaapi private
    */
    private String directoryName;

    /**
        @aribaapi private
    */
    private String archiveDirectoryName;

    /**
        Construct an ArchivingAppender.

        @aribaapi documented
    */
    public ArchivingAppender ()
    {
        super();
        synchronized (allInstances) {
            allInstances.add(this);
        }
    }

    /**
        @return an iterator of all ArchivingAppender instances
    */
    public static Iterator getIteratorForAppenders ()
    {
        synchronized (allInstances) {
            return ListUtil.cloneList(allInstances).iterator();
        }
    }

    /**
        Removes this ArchivingAppender from the list of allInstances.

        @aribaapi ariba
    */
    public void removeFromAllInstances ()
    {
        synchronized (allInstances) {
            allInstances.remove(this);
        }
    }

    /**
        Gets the name of the most recent archived log file.

        @aribaapi ariba

        @return name of most recent archived log file
    */
    public String getLastArchivedFileName ()
    {
        return lastArchivedFileName;
    }

    /**
        Set the directory for the log file, should be called prior to
        calling #activateOptions. Note that this is not synchronized,
        the last caller wins.
        @param directoryName directory as String
        @aribaapi documented
    */
    public void setDirectoryName (String directoryName)
    {
        this.directoryName = directoryName;
    }

    /**
        Returns the directory for the log file.
        @return the directory for the log file.
        @aribaapi documented
    */
    public String getDirectoryName ()
    {
        return directoryName;
    }

    /**
        Set the archive directory for the log file. Should be called
        prior to calling #activateOptions. Note that this is not
        synchronized, the last caller wins.
        @param archiveDirectoryName directory as String
        @aribaapi documented
    */
    public void setArchiveDirectoryName (String archiveDirectoryName)
    {
        this.archiveDirectoryName = archiveDirectoryName;
    }

    /**
        @return the archive directory for the log file
        @aribaapi documented
    */
    public String getArchiveDirectoryName ()
    {
        return archiveDirectoryName;
    }

    /**
        Set the max archive directory size
        @param maxArchiveSize size in mega bytes
        @aribaapi documented
    */
    public void setMaxArchiveSize (String maxArchiveSize)
    {
        this.maxArchiveSize = maxArchiveSize;
    }

    /**
        @return max archive directory size
        @aribaapi documented
    */
    public String getMaxArchiveSize ()
    {
        return maxArchiveSize;
    }

    /**
        Archive the currently opened log file

        @aribaapi private
    */
    public void archiveLogFile ()
    {
        synchronized (this) {
            closeFile();
            createLogFile(fileBaseName);
            try {
                setFile(fileName, fileAppend, bufferedIO, bufferSize);
            }
            catch (IOException e) {
            }
        }
    }

    /**
        Create the standard directories that are needed for logging information
        from the specified Ariba product.

        @return a boolean value representing whether the initialization was
        sucessful.
        @aribaapi private
    */
    public boolean initLogDirectories ()
    {
        boolean success = this.directory.instantiate();
        if (!success) {
            this.directory.LogError("Unable to instantiate log directory");
        }

        boolean archiveSuccess = this.archiveDirectory.instantiate();
        if (!archiveSuccess) {
            this.archiveDirectory.LogError(
                "Unable to instantiate log archive directory");
        }

        return (success && archiveSuccess);
    }

    /**
        Create a new LogFile that is guaranteed to exist and adhere to the
        constraints set forth by the log file manager (e.g. disk use)
        If the file already exists an exception is raised

        @param fileName name of the log file to create
        @return a LogFile that can be written to.
        @aribaapi private
    */
    protected LogFile createLogFile (String fileName)
    {
        File rootDirectory = this.directory;

        lastArchivedFileName = null;

            // generate the file path, the constructor does not
            // materialize or verify the backing file

        int indexOfExtension = fileName.lastIndexOf('.');
        String fileSuffix = "";
        String filePrefix = null;
        if (indexOfExtension != -1) {
            fileSuffix = fileName.substring(indexOfExtension + 1);
            filePrefix = fileName.substring(0, indexOfExtension);
        }
        else {
            filePrefix = fileName;
        }

        LogFile logFile = new LogFile(rootDirectory, filePrefix, fileSuffix);
        boolean success = true;
        if (logFile.file().exists()) {
            File rootArchiveDirectory = this.archiveDirectory;
            success = logFile.moveTo(rootArchiveDirectory);
            if (!success) {
                LogLog.error("Unable to archive old log");
            }
        }
        lastArchivedFileName = logFile.archivedFileName();
        return logFile;
    }

    /**
        @aribaapi private
    */
    public void monitorDirectories ()
    {
        this.initLogDirectories();
        /*
            Since we shouldn't be creating log directories all the
            time the overhead to validate, init log directories is
            worthwhile. Someday, the log and archive directory will be
            dynamic and hence could have changed since the last log
            was created.
        */

        this.archiveDirectory.purgeOnConstraint();
    }

    /**
        Active the options set for this class. The log file is created
        when this method is executed.
        @aribaapi documented
    */
    public void activateOptions ()
    {
        if (getEncoding() == null) {
            setEncoding(DefaultEncoding);
        }

        if (directoryName == null) {
            directoryName = LogManager.getDirectoryName();
        }

        setAppend(false);
        directory = new ConstrainedDirectory(directoryName);

        fileBaseName = fileName;

        fileName = Fmt.S("%s/%s", directoryName, fileName);

        String archiveDir = 
            StringUtil.nullOrEmptyString(archiveDirectoryName) ?
            LogManager.getArchiveDirectoryName() :
            archiveDirectoryName;
        archiveDirectory = new ConstrainedDirectory(archiveDir);

        int maxArchiveSizeMegaBytes =
            IntegerFormatter.getIntValue(maxArchiveSize);

        archiveDirectory.setMaxMegaBytes(
            maxArchiveSizeMegaBytes <= 0 ?
            DefaultLogMaxMegaBytes :
            maxArchiveSizeMegaBytes);
        monitorDirectories();
        createLogFile(fileBaseName);
        if (getLayout() == null) {
            setLayout(new StandardLayout());
        }
        super.activateOptions();


    }
    
    /**
     * Override of org.apache.log4j.WriterAppender.reset()
     * Catch an IllegalStateException and keep going
     * instead of always failing during a reset().
     */
    protected void reset ()
    {
        try {
            super.reset();
        }
        catch (IllegalStateException e) {
            /* 
              After some errors, the CharsetEncoder in the output stream
              gets into a state where a close always fails.  Because log4j
              setFile() calls reset() which always tries to close the quiet
              writer (qw) and the close fails with illegal state, you
              can never set a new file and move on.  So, we catch
              the illegal state exception and set the quiet writer
              instance to null, so processing can continue 
              (e.g during archive log).
            */
            this.qw = null;
        }
        
    }
}
