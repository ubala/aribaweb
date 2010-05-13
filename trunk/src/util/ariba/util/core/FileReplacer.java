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

    $Id: //ariba/platform/util/core/ariba/util/core/FileReplacer.java#9 $
*/
package ariba.util.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import ariba.util.log.Log;

/**
    Utility class that supports replacing a given {@link #getBaseFile base file}
    with a new file (typically, a new version of that file.) <p/>

    There is support for obtaining an {@link #getOutputStream OutputStream}
    to write a changed version of the file, {@link #applyChanges applying} the
    changes, then {@link #revertChanges reverting} the changes (you can apply
    and revert changes as many times as desired) before finally
    {@link #commit committing} either the applied or reverted changes
    (dropping the files used for temporary storage.) <p/>

    The temporary files are stored in the same directory as the base file.
    <code>File.rename()</code> is used to apply and revert which is an
    attempt to make the file replacement happen as quickly as possible. <p/>

    <b>Important note:</b> This class is in no way multi-thread safe.

    @aribaapi ariba
*/
public class FileReplacer implements OutputStreamHolder
{
    //-----------------------------------------------------------------------
    // private data members

    private File _directory;
    private String _filePrefix;
    private String _fileSuffix;
    private File _baseFile;
    private Set/*<FileInputStream>*/ _fileInputStreams;
    private File _temporaryFile;
    private File _backupFile;
    private OutputStream _outputStream;

    //-----------------------------------------------------------------------
    // constructors

    /**
        @param directory the directory in which to create the temporary file
        @param baseFileName the name of the file
        @aribaapi ariba
    */
    public FileReplacer (File directory, String baseFileName)
    {
        Assert.that(directory != null, "Directory in which the file to be replaced " +
                                       "exists may not be null");
        Assert.that(baseFileName != null, "File name may not be null");
        Assert.that(baseFileName.length() > 0, "File name may not be an empty string");
        _baseFile = new File(directory, baseFileName);
        _fileInputStreams = SetUtil.set();
        _directory = (directory != null) ? directory : new File(".");
        /* File.createTempFile() requires a prefix of at least 3 chars.
           baseFileName is not empty; add 2 chars to guarantee we get to 3 */
        _filePrefix = getFilePrefix(baseFileName);
        _fileSuffix = getFileSuffix(baseFileName);
        _temporaryFile = null;
        _outputStream = null;
    }

    /**
        @aribaapi ariba
    */
    public FileReplacer (File baseFile)
    {
        this (baseFile.getParent() != null ? baseFile.getParentFile() : new File("."),
              baseFile.getName());
    }

    //-----------------------------------------------------------------------
    // nested class

    /**
        @aribaapi private
    */
    public static class BadStateException extends IOException
    {
        private File _backupFile;
        private File _temporaryFile;

        public BadStateException (File backupFile, Exception cause)
        {
            _backupFile = backupFile;
            _temporaryFile = null;
            initCause(cause);
        }

        public BadStateException (File backupFile, File temporaryFile)
        {
            super(Fmt.S("_temporaryFile '%s' and _backupFile '%s' may not both " +
                        "be non-null at the same time", temporaryFile, backupFile));
            _backupFile = backupFile;
            _temporaryFile = temporaryFile;
        }

        public File getBackupFile ()
        {
            return _backupFile;
        }

        public File getTemporaryFile ()
        {
            return _temporaryFile;
        }

        void setBackupFile (File backupFile)
        {
            _backupFile = backupFile;
        }

        void setTemporaryFile (File temporaryFile)
        {
            _temporaryFile = temporaryFile;
        }
    }

    //-------------------------------------------------------------------------
    // private methods

    /**
        @aribaapi private
    */
    private void closeOutputStream () throws IOException
    {
        if (_outputStream != null) {
            _outputStream.close();
            _outputStream = null;
        }
    }

    /**
        @aribaapi private
    */
    private void closeInputStreams ()
    {
        for (Iterator iter=_fileInputStreams.iterator(); iter.hasNext(); )
        {
            FileInputStream stream = (FileInputStream)iter.next();
            iter.remove();
            try {
                stream.close();
            }
            catch (IOException ex) {
                Log.util.warning(8510, SystemUtil.stackTrace(ex));
            }
        }
    }

    /**
        Backs up the base file. That is, creates a temporary file and
        copies the base file to the temp file.

        @return the new temp file
        @aribaapi private
    */
    private File backupBaseFile () throws IOException
    {
        File temp = File.createTempFile(_filePrefix, _fileSuffix, _directory);
        boolean success = IOUtil.copyFile(_baseFile, temp);
        if (!success) {
            throw new IOException(Log.util.localizeMessage("2770",
                                  ListUtil.list(_baseFile, temp, null)));
        }
        return temp;
    }

    /**
        Does the following: <ul>
        <li> {@link #backupBaseFile backs up} the base-file to a backup-file
        <li> deletes the base-file and
        <li> renames <code>toRename</code> to base-file,
        <li> returning the backup-file.
        </ul>
        <p/>

        @param toRename the <code>File</code> to be renamed.
        @return the backed up <code>File</code>
    */
    private File backupBaseDeleteAndRename (File toRename)
    throws IOException, BadStateException
    {
        File backup = backupBaseFile();
        try {
            FileUtil.deleteAndRename(_baseFile, toRename);
            return backup;
        }
        catch (FileUtil.FileDeletionException ex) {
            // attempt to get rid of the temporary file on disk
            if (!backup.delete()) {
                Log.util.warning(8498, backup);
                backup.deleteOnExit();
            }
            throw ex;
        }
        catch (FileUtil.FileRenameException ex) {
            // Bummer, we deleted _baseFile successfully but we could not
            // rename toRename to it.
            // Try repairing.
            Log.util.warning(8515, toRename, _baseFile, backup);
            if (backup.renameTo(_baseFile)) {
                // the repair worked but we still need to throw the
                // original exception to let the client know this did
                // not go according to plan
                throw ex;
            }
            Log.util.warning(9042, _baseFile, backup);
            // now we're in serious trouble. we deleted _baseFile but
            // could not rename toRename to it (though we do have the backup file.)
            throw new BadStateException(backup, ex);
        }
    }

    //-------------------------------------------------------------------------
    // public methods

    /**
        Returns <code>true</code> if no changes have been made to the
        base file (either pending or applied) and <code>false</code>
        otherwise.

        @aribaapi ariba
    */
    public boolean hasNoChanges ()
    {
        return _temporaryFile == null && _backupFile == null;
    }

    /**
        Returns <code>true</code> if there are any pending changes to be
        made to the base file.

        @aribaapi ariba
    */
    public boolean hasPendingChanges ()
    {
        return _temporaryFile != null;
    }

    /**
        Returns <code>true</code> if pending changes have been applied to
        the base file.
        @aribaapi ariba
    */
    public boolean hasAppliedChanges ()
    {
        return _backupFile != null;
    }

    /**
        Returns the file on which this replacer is based.
        @aribaapi ariba
    */
    public File getBaseFile ()
    {
        return _baseFile;
    }

    /**
        Returns a <code>FileInputStream</code> on the base file. <p/>

        Note that a reference is retained to the returned file stream.
        It will be forcibly closed when {@link #applyChanges} is
        called. <p/>

        @aribaapi ariba
    */
    public FileInputStream getBaseFileInputStream () throws IOException
    {
        FileInputStream result = new FileInputStream(_baseFile);
        _fileInputStreams.add(result);
        return result;
    }

    /**
        Returns a <code>FileInputStream</code> on the temp file.
        The temp file must exist--it is only created when <code>this</code>
        {@link #hasPendingChanges}. <p/>

        Note that a reference is retained to the returned file stream.
        It will be forcibly closed when {@link #applyChanges} is
        called. <p/>

        @aribaapi ariba
    */
    public FileInputStream getTempFileInputStream () throws IOException
    {
        Assert.that(hasPendingChanges(),
                    "No pending changes have been made; an input stream " +
                    "cannot be opened on the temp file as it does not exist");
        FileInputStream result = new FileInputStream(_temporaryFile);
        _fileInputStreams.add(result);
        return result;
    }


    /**
        Returns the output stream associated with the temporary file.
        If there is no output stream already opened on the temporary file
        opens an output stream on it and returns the stream. <p/>

        <b>Important note:</b> This <code>FileReplacer</code> must
        either have {@link #hasNoChanges no changes} or must
        have {@link #hasPendingChanges pending changes} when this
        method is called. It is an error to try to obtain an output
        stream if this has {@link #hasAppliedChanges applied changes}.
        (If the client wishes to do this, he must first {@link #revertChanges revert}
        the applied changes and then request the stream.
        <p/>

        It is the client's responsibility to close the stream. <p/>

        @aribaapi private
    */
    public OutputStream getOutputStream () throws IOException
    {
        Assert.that(_backupFile == null,
                    "Invalid attempt to obtain an OutputStream to " +
                    "make changes while the most recent set of changes " +
                    "remains uncommitted. Please commit or revert before " +
                    "trying to make more changes.");
        if (_outputStream == null) {
            if (_temporaryFile == null) {
                _temporaryFile = File.createTempFile(_filePrefix,
                                                     _fileSuffix,
                                                     _directory);
            }
            _outputStream = IOUtil.bufferedOutputStream(_temporaryFile);
        }
        return _outputStream;
    }

    /**
        Closes the existing output stream on the temporary file (if any)
        creates a new output stream and returns it. <p/>

        @aribaapi ariba
    */
    public OutputStream makeOutputStream () throws IOException
    {
        closeOutputStream();
        return getOutputStream();
    }

    /**
        Closes the stream opened in {@link #getOutputStream} and renames
        the temporary file to <code>realms.xml</code> (after making a backup
        copy of it).

        @aribaapi private
    */
    public void applyChanges () throws IOException, BadStateException
    {
        checkGoodState();
        closeOutputStream();
        if (_temporaryFile != null) {
            closeInputStreams();
            try {
                _backupFile = backupBaseDeleteAndRename(_temporaryFile);
                _temporaryFile = null;
            }
            catch (BadStateException ex) {
                _backupFile = ex.getBackupFile();
                ex.setTemporaryFile(_temporaryFile);
                throw ex;
            }
        }
    }

    /**
        Closes the stream opened in {@link #getOutputStream} and
        deletes the temporary file. <p/>
        @aribaapi ariba
    */
    public void revertChanges () throws IOException, BadStateException
    {
        checkGoodState();
        if (_backupFile != null) {
            closeInputStreams();
            try {
                _temporaryFile = backupBaseDeleteAndRename(_backupFile);
                _backupFile = null;
            }
            catch (BadStateException ex) {
                _temporaryFile = ex.getBackupFile();
                // dfinlay - 17-Dec-2004 - here we're switching the backup file
                // and the temporary file because the internal helper method
                // backupBaseDeleteAndRename() was operating under the assumption
                // that the temporary file was the backup file
                ex.setBackupFile(_backupFile);
                ex.setTemporaryFile(_temporaryFile);
                throw ex;
            }
        }
    }

    /**
        Drops the temporary and backup files associated with this
        <code>FileReplacer</code> thereby making the changes finally durable.
        (There is no way to further apply or revert the changes.) <p/>

        After this method is called <code>this</code> goes to the
        {@link #hasNoChanges} state. <p/>

        @aribaapi ariba
    */
    public void commit ()
    {
        try {
            closeOutputStream();
            closeInputStreams();
        }
        catch (IOException ex) {
            Log.util.warning(8511, SystemUtil.stackTrace(ex));
        }
        if (_temporaryFile != null) {
            boolean isDeleted = FileUtil.deleteFile(_temporaryFile);
            if (! isDeleted) {
                Log.util.warning(10410, _temporaryFile);
            }
            _temporaryFile = null;
        }
        if (_backupFile != null) {
        	boolean isDeleted = FileUtil.deleteFile(_backupFile);
            if (! isDeleted) {
                Log.util.warning(10410, _backupFile);
            }
            _backupFile = null;
        }
    }

    /**
        @aribaapi ariba
    */
    public String getBadStateMessage ()
    {
        return Log.util.localizeMessage("9043",
                                        ListUtil.list(_baseFile, _backupFile,
                                                      _temporaryFile));
    }

    /**
        Returns whether or not <code>this</code> is in a valid state. <p/>

        There is one invalid state for objects of this class, which is the
        state in which there exists a backup file (of the base file) <b>and</b>
        a temporary file (containing the changes to base file.)  <p/>

        In the normal course this should not happen: either the changes are applied
        and the base file is backed up (to allow a revert) or the
        changes are not applied, the base file is unchanged and there's no
        backup file. <p/>

        However, during {@link #applyChanges} or {@link #revertChanges}
        the base file is deleted and the temporary or backup file is
        renamed to the base file, respectively. In either case it's possible
        for the delete to succeed and the rename to fail leaving us in
        this invalid state.
        @aribaapi ariba
    */
    public boolean isInGoodState ()
    {
        return _backupFile == null || _temporaryFile == null;
    }

    /**
        @aribaapi ariba
    */
    public void checkGoodState () throws BadStateException
    {
        if (!isInGoodState()) {
            throw new BadStateException(_backupFile, _temporaryFile);
        }
    }

    /**
        @aribaapi ariba
    */
    public void repairFromBadState () throws IOException
    {
        Assert.that(!isInGoodState(), "FileReplacer.repairFromBadState() may " +
                                      "only be called when the replacer is in a" +
                                      "bad state");
        FileUtil.rename(_backupFile, _baseFile);
    }
    
    /**
        Returns the prefix that is used in creating the 
        temporary files, given the original filename.
    
        @param fileName the original file name that the prefix is made for.
        @return the prefix
        @aribaapi private
    */
    private String getFilePrefix (String fileName)
    {
        String filePrefix = ((fileName.length() >= 3) ? 
                fileName : StringUtil.strcat(fileName, "12"));

        int idx = fileName.lastIndexOf('.');
        if (idx != -1) {
            filePrefix = fileName.substring(0, idx);
        }

        return filePrefix;
    }

    /**
        Returns the suffix that is used in creating the 
        temporary files, given the original filename.

        @param fileName the original file name that the suffix is made for.
        @return the suffix
        @aribaapi private
    */
    private String getFileSuffix (String fileName)
    {
        String fileSuffix = null;

        int idx = fileName.lastIndexOf('.');
        if (idx != -1) {
            fileSuffix = StringUtil.strcat(fileName.substring(idx), ".tmp");
        }

        return fileSuffix;
    }

    /**
        Returns a <code>FilenameFilter</code> object that can be used 
        for filtering the temporary files that are created by 
        <code>FileReplacer</code> pattern. 

        @return the filename filter
        @aribaapi private
    */
    public FilenameFilter getTempFilenameFilter ()
    {
        TempFilenameFilter tff = new TempFilenameFilter();
        return tff;
    }

    /**
        Returns a <code>FilenameFilter</code> object that can be used 
        for filtering the temporary files that are created by 
        <code>FileReplacer</code> pattern. 

        @param file the file that the filter pattern is based on.
        @return the filename filter
        @aribaapi private
    */
    public static FilenameFilter getTempFilenameFilter (File file)
    {
        FileReplacer fr = new FileReplacer(file);
        return fr.getTempFilenameFilter();
    }

    /**
        TempFilenameFilter, is used to filter the temporary files that are
        created using <code>FileReplacer</code> pattern. 
        @aribaapi private
    */
    private class TempFilenameFilter implements FilenameFilter
    {
        public boolean accept (File dir, String name)
        {
            String prefix = getFilePrefix (_baseFile.getName());
            String suffix = getFileSuffix (_baseFile.getName());

            Pattern tempFilenamePttern = Pattern.compile(prefix + ".+" + suffix);
            Matcher matcher = tempFilenamePttern.matcher(name);

            return matcher.matches();
        }
    }
}
