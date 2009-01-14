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

    $Id: //ariba/platform/util/core/ariba/util/core/FileUtil.java#21 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Random;
import java.util.List;

/**
    File Utilities. These are helper functions for dealing with
    files.

    @aribaapi documented
*/
public final class FileUtil
{
    //-----------------------------------------------------------------------
    // constants

    public static final String DefaultBackupFileSuffix = "bak";
    public static final int DefaultMaxBackupNumber = 20;

    //-----------------------------------------------------------------------
    // nested class

    /**
        Represents the exceptional condition where we expected to be
        able to delete a file but were not actually able to do so. <p/>

        @aribaapi ariba
    */
    public static class FileDeletionException extends IOException
    {
        private File _file;

        /**
            Constructs a new instance. <p/>
            @aribaapi ariba
        */
        public FileDeletionException (File file)
        {
            super(Log.util.localizeMessage("8508", ListUtil.list(file)));
            _file = file;
        }

        /**
            Returns the <code>File</code> that could not be deleted. <p/>
            @aribaapi ariba
        */
        public File getFile ()
        {
            return _file;
        }
    }

    //-----------------------------------------------------------------------
    // nested class

    /**
        Represents the exceptional condition where we expected to be
        able to rename a file but were not actually able to do so. <p/>

        @aribaapi ariba
    */
    public static class FileRenameException extends IOException
    {
        private File _fromFile;
        private File _toFile;

        /**
            Constructs a new instance. <p/>
            @aribaapi ariba
        */
        public FileRenameException (File fromFile, File toFile)
        {
            super(Log.util.localizeMessage("8499", ListUtil.list(fromFile, toFile)));
            _fromFile = fromFile;
            _toFile = toFile;
        }

        /**
            Returns the <code>File</code> from which we could not rename. <p/>
            @aribaapi ariba
        */
        public File getFromFile ()
        {
            return _fromFile;
        }

        /**
            Returns the <code>File</code> to which we could not rename. <p/>
            @aribaapi ariba
        */
        public File getToFile ()
        {
            return _toFile;
        }
    }

    //-----------------------------------------------------------------------
    // constructors

    /* prevent people from creating this class */
    private FileUtil ()
    {
    }

    //-------------------------------------------------------------------------
    // public methods

    /**
        Fix up file separators for platform independence. Converts
        <B>file</B> to use the appropriate separator character for the
        current platform.

        @param file a file potentially with non platform
        specific file separator characters ("/" or "\")

        @return the same <b>file</b> but with only
        File.separatorChar as the separator
        @aribaapi documented
    */
    public static File fixFileSeparators (File file)
    {
        if (file == null) {
            return null;
        }
        return new File(fixFileSeparators(file.getPath()));
    }

    /**
        Fix up filename separators for platform independence.
        Converts <B>filename</B> to use the appropriate separator
        character for the current platform.

        @param filename a filename potentially with non platform
        specific file separator characters ("/" or "\")

        @return the same <b>filename</b> but with only
        File.separatorChar as the separator
        @aribaapi documented
    */
    public static String fixFileSeparators (String filename)
    {
        if (filename == null) {
            return null;
        }
        switch (File.separatorChar) {
          case '/':
            return filename.replace('\\', File.separatorChar);
          case '\\':
            return filename.replace('/', File.separatorChar);
          default:
            Log.util.warning(2812, File.separator);
            filename = filename.replace('/', File.separatorChar);
            filename = filename.replace('\\', File.separatorChar);
            return filename;
        }
    }

    /**
        Returns the File for a named directory. If there is no
        directory <B>dir</B> relative to the File <B>base</B>, it and
        all needed parent directories will be created.

        @param base the parent directory of <b>dir</b>
        @param dir the name of the directory to find or create

        @return the directory found or created

        @exception IOException if there was an error finding or
        creating the directory
        @aribaapi documented
    */
    public static File directory (File base, String dir) throws IOException
    {
        return directory(base.getPath(), dir);
    }

    /**
        Returns the File for a named directory. If there is no
        directory <B>dir</B> relative to the directory <B>base</B>, it
        and all needed parent directories will be created.

        @param base the parent directory of <b>dir</b>
        @param dir the name of the directory to find or create

        @return the directory found or created

        @exception IOException if there was an error finding or
        creating the directory
        @aribaapi documented
    */
    public static File directory (String base, String dir)
      throws IOException
    {
        return directory(new File(new File(base), dir));
    }

    /**
        Returns the File for a named directory. If there is no
        directory it and all needed parent directories will be
        created.

        @param dir the name of the directory to find or create

        @return the directory found or created

        @exception IOException if there was an error finding or
        creating the directory
        @aribaapi documented
    */
    public static File directory (String dir) throws IOException
    {
        return directory(new File(dir));
    }

    /**
        Returns the File for a directory. If there is no directory it
        and all needed parent directories will be created.

        @param dir the directory to find or create

        @return the directory found or created

        @exception IOException if there was an error finding or
        creating the directory
        @aribaapi documented
    */
    public static File directory (File dir) throws IOException
    {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException(Fmt.S("can't create directory %s", dir));
            }
        }

        if (!dir.isDirectory()) {
            throw new IOException(Fmt.S("%s is not a directory", dir));
        }

        return dir;
    }

    /**
        Convert a file to canonical form. On error, returns the same
        file. Java doesn't specify what might cause a failure. Most
        likely it is if you have permissions problems on the parent
        directories.

        @param file the file to convert to canonical form
        @return current file converted to canonical format
        @aribaapi documented
    */
    public static File getCanonicalFile (File file)
    {
        try {
            return new File(file.getCanonicalPath());
        }
        catch (IOException e) {
            Log.util.debug("Could not create canonical path for %s: %s",
                           file, e);
            return file;
        }
    }

    /**
        Create directories for file if they do not exist.

        @param file the file containing possibly non-existent directories
        @exception IOException if there was an error finding or creating the directory
        @aribaapi documented
    */
    public static void createDirsForFile (File file) throws IOException
    {
        File dir = file.getParentFile();
        if (dir != null) {
            directory(dir);
        }
    }

    /**
        Convenience method that will call <code>delete()</code> on
        <code>file</code> and if that fails will call <code>file.deleteOnExit()</code>
        scheduling the file for deletion upon normal server exit. <p/>

        @param file the <code>File</code> to delete
        @return <code>true</code> if <code>file</code> was immediately deleted and
                <code>false</code> otherwise
        @aribaapi ariba
    */
    public static boolean deleteFile (File file)
    {
        boolean result = file.delete();
        if (!result) {
            Log.util.warning(8498, file);
            file.deleteOnExit();
        }
        return result;
    }

    /**
        Deletes the file identified by <code>toDelete</code>. <p/>

        @throws FileDeletionException if <code>toDelete</code> cannot be deleted
        @aribaapi ariba
    */
    public static void delete (File toDelete)
    throws FileDeletionException
    {
        if (!toDelete.delete()) {
            throw new FileDeletionException(toDelete);
        }
            // Note: sometimes the delete is successful but the
            // file still exists on the disk.  And it seems that calling
            // 'exists' can help complete the first 'delete' operation.
        if (toDelete.exists()) {
            Log.util.warning(8806, toDelete);
            throw new FileDeletionException(toDelete);
        }
    }

    /**
        Renames <code>fromFile</code> to <code>toFile</code>. <p/>

        @throws FileRenameException if <code>fromFile</code> cannot be
                renamed to toFile
        @aribaapi ariba
    */
    public static void rename (File fromFile, File toFile)
    throws FileRenameException
    {
        boolean success = false;
        Random random = null;
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                try {
                    // 1-562AXC Sometimes two nodes are trying to rename the same file
                    // and each causes the other's rename to fail.  Add a random
                    // value to sleep to try to break them up and allow one to succeed.
                    random = (random != null) ? random : new Random();
                    Thread.sleep(100 + random.nextInt(128));
                }
                catch (InterruptedException ex) {
                    Log.util.debug("Got InterrupttedException: %s", ex);
                }
            }
            if (fromFile.renameTo(toFile)) {
                success = true;
                break;
            }
            Log.util.debug("Trial %s failed, rename %s to %s",
                    Constants.getInteger(i), fromFile, toFile);
        }
        if (!success) {
            throw new FileRenameException(fromFile, toFile);
        }
    }

    /**
        Convenience method that deletes <code>toDelete</code> and renames
        <code>toRename</code> to <code>toDelete</code>. <p/>

        @throws FileDeletionException if <code>toDelete</code> cannot be deleted
        @throws FileRenameException if <code>toRename</code> cannot be
                renamed (assuming <code>toDelete</code> is deleted)
        @aribaapi ariba
    */
    public static void deleteAndRename (File toDelete, File toRename)
    throws FileDeletionException, FileRenameException
    {
        delete(toDelete);
        rename(toRename, toDelete);
    }

   /**

        Determines if the path specified by a given String is

        an absolute path or not. On UNIX systems, a pathname is absolute

        if its prefix is "/". On Win32 systems, a pathname is absolute

        if its prefix is a drive specifier followed by "\\", or if its

        prefix is "\\".



        @param path the input String.

        @return <b>true</b> if the path is an absolute path. <b>false</b>

        otherwise. Returns false if the input string is null or empty or blank.

        @aribaapi documented

    */

    public static boolean isAbsolutePath (String path)

    {

        if (StringUtil.nullOrEmptyOrBlankString(path)) {

            return false;

        }

        if (SystemUtil.isWin32()) {

                // windows find the index of the driver number. Then

                // we start searching from the index immediately after

                // it. Note that this code works even if the drive

                // character (':') does not exist because indexOf then

                // returns -1, and we start searching from index 0.

            int indexToSearch = path.indexOf(":") + 1;

            return (path.startsWith("\\", indexToSearch) ||

                    path.startsWith("/", indexToSearch));

        }

            // unix
            // ToDo: should I also check for "\\"? People could specify windows style
            // path character on Unix?

        return path.startsWith("/");

    }

    /**
        Recursively remove all files from the local temp directory. This
        unfiltered method is here as a convenience for code (e.g. startup
        stuff) that wants to remove the files unconditionally.

        @throws SecurityException if some files don't have read access.
        @aribaapi ariba
    */
    public static void purgeLocalTempDir ()
    {
        purgeLocalTempDir(null);
    }

    /**
        Recursively remove all files from the local temp directory. Use
        the supplied filter to select a subset of the files.

     @param filter the selection filter for the directory and its subdirectories;
                   May be null to indicate that all files are to be deleted.
     @throws SecurityException if some files don't have read access.
     @aribaapi ariba
    */
    public static void purgeLocalTempDir (FileFilter filter)
    {
        File dir = SystemUtil.getLocalTempDirectory();
        int filesDeleted = purgeDir(dir, filter);
        // Deleted {0} file(s) from local temp directory "{1}."
        Log.utilIO.info(8906, filesDeleted, dir.getPath());
    }

    /**
        Recursively remove all files from the shared temp directory. This
        unfiltered method is here as a convenience for code (e.g. startup
        stuff) that wants to remove the files unconditionally.

        @throws SecurityException if some files don't have read access.
        @aribaapi ariba
    */
    public static void purgeSharedTempDir ()
    {
        purgeSharedTempDir(null);
    }

    /**
        Recursively remove all files from the shared temp directory. Use
        the supplied filter to select a subset of the files.

     @param filter the selection filter for the directory and its subdirectories;
                   May be null to indicate that all files are to be deleted.
     @throws SecurityException if some files don't have read access.
        @aribaapi ariba
    */
    public static void purgeSharedTempDir (FileFilter filter)
    {
        File dir = SystemUtil.getSharedTempDirectory();
        int filesDeleted = purgeDir(dir, filter);
        // Deleted {0} file(s) from shared temp directory "{1}."
        Log.utilIO.info(8909, filesDeleted, dir.getPath());
    }

    /**
        Get a snapshot of the contents of shared temp dir. The result will be a List of
        File objects representing the tree structure of the shared temp dir. depending on
        the definition of the path in shared temp dir the snapshot will include File objects
        with relative or absolute paths.

        @return a list of File objects that represent a tree structure of the shared temp dir

        @aribaapi private
    */
    public static List getSharedTempDirSnapshot ()
    {
        List snapshot = ListUtil.list();

        File rootDir = SystemUtil.getSharedTempDirectory();
        if (rootDir == null) {
            Log.utilIO.error(8912);
            return snapshot;
        }

        getFiles(rootDir, snapshot);

        return snapshot;
    }

    /**
        Recursively get the list of the files in a given dir and add it to the list passed
        in the second argument.

        @param dir the root directory for the tree structure that its contents are to be listed.

        @param outList is the list that will contain the list of Files under the dir.
            The list of files will be added to this list recursively.

        @aribaapi private
    */
    private static void getFiles (File dir, List outList)
    {
        if (dir == null) {
            return;
        }

        try {
            if (!dir.isDirectory()) {
                return;
            }

            File[] list = dir.listFiles();
            if (list != null) {
                for (int i=0; i<list.length; ++i) {
                    File file = list[i];
                    if (!file.isDirectory()) {
                        outList.add(file);
                    }
                    else {
                        getFiles(file, outList);
                    }
                }
            }
        }
        catch (SecurityException se) {
            Log.utilIO.error(8911, dir);
        }
    }

    /**
        Purge the temp shared dir by only removing the files specified in the snapshot input parameter.
        The files will be removed if they have not beed modified after the baseTime.
        The reason that we have this method is to enable purging temp shared dir (or any directory)
        asynchronously.  Meaning we only remove the files that we first got a snapshot of.  This way
        the new files created by concurrent threads will not be deleted.  By checking the
        last modified property of the file against the base line we make sure that the method
        does not delete the files that are being used since the time that we got the snapshot.

        @param sharedTempDirSnapshot the list of the files inthe snapshot to be removed.
            Note that the files can have relative or absolute paths.  The directories
            will not be removed.

        @param baseTime a base time to be used for delteing the files.  Files that are mmodified after
            this base time will not be removed.

        @aribaapi private
    */
    public static void purgeSharedTempDirSnapshot (
        List/*<File>*/ sharedTempDirSnapshot,
        long baseTime)
    {
        if (sharedTempDirSnapshot == null) {
            return;
        }

        for (int i=0; i<sharedTempDirSnapshot.size(); ++i) {
            File file = (File)sharedTempDirSnapshot.get(i);
            if (file.isDirectory()) {
                continue;
            }
            if (file.lastModified() < baseTime) {
                try {
                    if (!file.delete()) {
                        Log.utilIO.error(8910, file);
                    }
                }
                catch (SecurityException se) {
                        Log.utilIO.error(8910, file);
                }
            }
        }
    }

    /**
        Recursively remove all files from the specified directory. Use
        the supplied filter to select a subset of the files.

     @param dir the root of the deletion process
     @param filter the selection filter for the directory and its subdirectories;
                   May be null to indicate that all files are to be deleted.
     @return the number of files actually deleted
     @throws SecurityException if some files don't have read access.
    */
    public static int purgeDir (File dir, FileFilter filter)
    {
        int filesDeleted = 0;
        if (dir != null) {
            File[] files = dir.listFiles(filter);
            if (files == null) {
                    // I/O error while trying to get the files from directory: {0}
                Log.utilIO.error(8907, dir);
                return filesDeleted;
            }

            int deleteErrors = 0;
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isDirectory()) {
                    filesDeleted += purgeDir(f, filter);
                    /**
                        Note that we don't attempt to delete an empty directory
                        here. There's no real advantage (it will probably be
                        recreated soon anyway), and we don't know if we'll
                        correctly identify an empty directory because the file
                        system may be deleting the files asynchronously.
                     */
                }
                else {
                    try {
                        if (f.delete()) {
                            filesDeleted++;
                        }
                        else {
                            deleteErrors++;
                        }
                    }
                    catch (SecurityException se) {
                        deleteErrors++;
                    }
                }
            }
            if (deleteErrors > 0) {
                    // Security or other exceptions kept {0} file(s) from being deleted from directory {1}.
                Log.utilIO.error(
                    8908,
                    deleteErrors,
                    dir);
            }
        }
        else {
            Log.utilIO.debug("Null directory passed to purgeDir.");
        }
        return filesDeleted;
    }

    /**
        Lists the files in a specific directory that satisfy the filter.

        @param dir the directory containig the listing files
        @param filter the selection filter for the files to be listed;
            May be null, then all files in the dir will be listed
        @return the file list
        @aribaapi ariba
    */
    public static File [] ListFiles (File dir, FilenameFilter filter)
    {
        File[] files = null;
        if (dir != null && dir.isDirectory()) {
            files = dir.listFiles(filter);
        }
        return files;
    }

    /**
        Deletes the files in a specific directory that satisfy the filter.

        @param dir the directory containig the files to be deleted
        @param filter the selection filter for the files to be deleted;
            May be null, then all files in the dir will be deleted
        @aribaapi ariba
    */
    public static void DeleteFiles (File dir, FilenameFilter filter)
    {
        if (dir != null && dir.isDirectory()) {
            File[] files = ListFiles(dir, filter);
            if (!ArrayUtil.nullOrEmptyArray(files)) {
                for (int i=0; i<files.length; ++i) {
                    if (files[i].isFile()) {
                        deleteFile(files[i]);
                    }
                }
            }
        }
    }

    /**
        Returns the most recently modified file in a specific directory
        that satisfies the filter.

        @param dir the directory containig the listing files
        @param filter the selection filter for the files to be listed;
            May be null, then all files in the dir will be considerd
        @return the most recently modified file that satisfies the filter
        @aribaapi ariba
    */
    public static File mostRecentModifiedFile (File dir, FilenameFilter filter)
    {
        File newestFile = null;
        if (dir != null && dir.isDirectory()) {
            File[] files = ListFiles(dir, filter);
            if (!ArrayUtil.nullOrEmptyArray(files)) {
                long maxTime = Long.MIN_VALUE;
                for (int i=0; i<files.length; ++i) {
                    long lastModified = files[i].lastModified();
                    if (lastModified > maxTime) {
                        maxTime = lastModified;
                        newestFile = files[i];
                    }
                }
            }
        }
        return newestFile;
    }

}
