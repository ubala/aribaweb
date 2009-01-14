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

    $Id: //ariba/platform/util/core/ariba/util/core/FileComponentChecksum.java#10 $
*/
package ariba.util.core;

import ariba.util.log.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
    This class extends the ComponentChecksum class to compute a
    checksum for files and directories.  The checksum used is Adler32.
    Adler32 is generally considedered as good as CRC32, and is twice
    as fast.

    In addition to the implmentation of getChecksum, several methods
    for adding files and directories to the checksum are included.

    The FileComponentChecksum object is not synchronized, and uses a
    object that is not thread safe, viz. the Checksum.

    @aribaapi ariba
*/
public class FileComponentChecksum extends ComponentChecksum
{
    private Checksum cs = new Adler32();

    public FileComponentChecksum (String name)
    {
        super(name);
    }

    /**
        Return the accumulated checksum as a byte array
        (big endian).

        @return The checksum accumulated so far
        @aribaapi ariba
    */
    public byte[] getChecksum ()
    {
        byte[] b = new byte[4];
        long c = cs.getValue();
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte)(c & 0xff);
                // sign extend doesn't matter
            c >>= 8;
        }
        return b;
    }

    /*
        This internal routine does the work of adding the file name and 
        its contents to the checksum object.  By moving the buffer 
        allocation out of this routine, addFile and the addDirectory 
        calls can share the code, but can save ourselves a few buffer objects.
    */
    private void addFileContents (File f, byte[] buff)
      throws IOException
    {
        Log.startupUtil.debug("FileComponentChecksum: adding file %s", f);
            //get the file name into checksum
        String fileName = f.getName();
        byte[] buf = fileName.getBytes();
        int bSize = buf.length;
        cs.update(buf, 0, bSize);
            //get the file content into checksum
        int bufSize = buff.length;
        FileInputStream fis = new FileInputStream(f);
        int len;
        while (true) {
            len = fis.read(buff, 0, bufSize);
            if (len == -1) {
                break;
            }
            cs.update(buff, 0, len);
        }
        fis.close();
    }


    /**
        Add the specified File to the checksum.  If the
        File is not a normal file, throw an exception.  Use
        addDirectory or addDirectoryDeep for directories.
        If there are any IO errors, log them, then throw an
        exception.

        @param f File whose name and contents we want to add to the checksum
        @exception ComponentChecksumException  The file was not a
        normal file, the file did not exist, or there was some other
        IO error.
        @aribaapi ariba
    */
    public void addFile (File f)
      throws ComponentChecksumException
    {
            // aribitrarily make the buffer 16K
        int bufSize = 16 * 1024;

        if (! f.exists()) {
             throw new ComponentChecksumException(
                StringUtil.strcat(f.getAbsolutePath()," does not exist"));
        }

        if (! f.isFile()) {
            throw new ComponentChecksumException(
                StringUtil.strcat(f.getAbsolutePath()," is not a file"));
        }

        try {
            addFileContents(f, new byte[bufSize]);
        }
        catch (IOException e) {
            Log.startupUtil.debug("IO Exception: %s", e);
            throw new ComponentChecksumException(e.getMessage());
        }
    }


    /**
        Add the file specified by the string name

        @param name String name of the file to add
        @aribaapi ariba
    */
    public void addFile (String name)
      throws ComponentChecksumException
    {
        addFile(new File(name));
    }

    /**
        Adds all of the files in the given List.  The files are represented
        as filename Strings.  They are added to the checksum in the same order
        as the given List.

        @aribaapi ariba

        @param v    List containing the filenames
         @exception ComponentChecksumException  The file was not a
         normal file, the file did not exist, or there was some other
         IO error.
    */
    public void addFileList (List v)
      throws ComponentChecksumException
    {
        addFileList(v, true);
    }

    /**
        Adds all of the files in the given List.  The files are represented
        as filename Strings.  Optionally sorts the filenames before adding the
        files to the checksum.

        @aribaapi ariba

        @param v    List containing the filenames
        @param sortFirst    if true, sorts the filenames before adding them to
            this checksum (the checksum value is dependent on the order in
            which files are added)
         @exception ComponentChecksumException  The file was not a
         normal file, the file did not exist, or there was some other
         IO error.
    */
    public void addFileList (List v, boolean sortFirst)
      throws ComponentChecksumException
    {
        Object[] a = v.toArray();
        if (sortFirst) {
                // If specified, sort the files first because the checksum value
                // is dependent on the order in which files are added
            Sort.objects(a, StringCompare.self);
        }

            // Add the files to the checksum
        for (int i = 0; i < a.length; i++) {
            addFile((String)a[i]);
        }
    }

    /**
        Add all the files in the specified directory.  This call
        stays at this level, and does not go any deeper.  Use
        addDirectoryDeep if you want to recurse through all
        levels.

        @param dir File for the directory
        @exception ComponentChecksumException  The File specified is not
        a directory or did not exist, or some other IO exception occurred.
        @aribaapi ariba
    */
    public void addDirectory (File dir)
      throws ComponentChecksumException
    {
        if (! dir.exists()) {
             throw new ComponentChecksumException(
                StringUtil.strcat(dir.getAbsolutePath()," does not exist"));
        }

        if (! dir.isDirectory()) {
            throw new ComponentChecksumException(
                StringUtil.strcat(dir.getAbsolutePath()," is not a directory"));
        }

        int bufSize = 16 * 1024;
        byte[] buff = new byte[bufSize];
        File[] list = dir.listFiles();
        Sort.objects(list, FileCompare.self);
        for (int i = 0; i < list.length; i++) {
            if (list[i].exists()) {
                if (list[i].isFile()) {
                    try {
                        addFileContents(list[i], buff);
                    }
                    catch (IOException e) {
                        Log.startupUtil.debug("IO Exception, file %s, %s",
                                              list[i].getAbsolutePath(), e);
                        throw new ComponentChecksumException(e.getMessage());
                    }
                }
            }
        }
    }

    /**
        Add directory specified by string name

        @param name name of the directory to add
        @exception ComponentChecksumException the directory does not
        exit, is not a directory, or there was a file IO error while
        processing
        @aribaapi ariba
    */
    public void addDirectory (String name)
      throws ComponentChecksumException
    {
        addDirectory(new File(name));
    }

    /**
        Add all the files in the specified directory.  This call
        recurses down to each subdirectory.

        @param dir File for the directory
        @exception ComponentChecksumException  The File specified is not
        a directory or did not exist, or some other IO exception occurred.
        @aribaapi ariba
    */
    public void addDirectoryDeep (File dir)
      throws ComponentChecksumException
    {
        if (! dir.isDirectory()) {
            throw new ComponentChecksumException(
                StringUtil.strcat(dir.getAbsolutePath()," is not a directory"));
        }

        if (! dir.exists()) {
            throw new ComponentChecksumException(
                StringUtil.strcat(dir.getAbsolutePath()," does not exist"));
        }

        int bufSize = 16 * 1024;
        byte[] buff = new byte[bufSize];
        File[] list = dir.listFiles();
            //sort the files
        Sort.objects(list, FileCompare.self);
        for (int i = 0; i < list.length; i++) {
            if (list[i].exists()) {
                if (list[i].isFile()) {
                    try {
                        addFileContents(list[i], buff);
                    }
                    catch (IOException e) {
                        Log.startupUtil.debug("IO Exception, file %s, %s",
                                              list[i].getAbsolutePath(), e);
                        throw new ComponentChecksumException(e.getMessage());
                    }
                }
                else if (list[i].isDirectory()) {
                    addDirectoryDeep(list[i]);
                }
            }
        }
    }

    /**
        Add directory specified by string name, and recurse to all
        subdirectories.

        @param name name of the directory to add
        @exception ComponentChecksumException the directory does not
        exit, is not a directory, or there was a file IO error while
        processing
        @aribaapi ariba
    */
    public void addDirectoryDeep (String name)
      throws ComponentChecksumException
    {
        addDirectoryDeep(new File(name));
    }
}
