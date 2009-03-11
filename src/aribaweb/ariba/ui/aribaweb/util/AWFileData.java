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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWFileData.java#15 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.IOUtil;
import ariba.util.core.MIME;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.mail.internet.SharedInputStream;

/**
    This object encapsulates the data from file uploads.

    If file uploads are memory based, the _data member will contain the bytes from the upload.

    If file uploads are file based, the _file member will contain the File for the temporary uploaded file.
    The temporary file will be removed when the finalize method for this class is called, and applications will
    need to copy this file if they want a persistent copy.
*/

public final class AWFileData extends AWBaseObject implements AWDisposable
{
    private final String _filename;
    private byte[] _data;
    private final String _mimeType;
    private final boolean _fileIncomplete;
    private final File _file;
    private InputStream _inputStream;
    private final int _bytesRead;
    private final SharedInputStream _sharedInputStream;
    private final boolean _encrypted;

    public AWFileData (String filename, File file, String mimeType, boolean fileIncomplete, int bytesRead, boolean encrypted)
    {
        super();
        _filename = filename;
        _data = null;
        _mimeType = mimeType;
        _fileIncomplete = fileIncomplete;
        _file = file;
        _bytesRead = bytesRead;
        _sharedInputStream = null;
        _encrypted = encrypted;
    }

    public AWFileData (String filename, byte[] data)
    {
        super();
        _filename = filename;
        _data = data;
        _mimeType = MIME.ContentTypeApplicationOctetStream;
        _fileIncomplete = false;
        _file = null;
        _bytesRead = data == null ? 0 : data.length;
        _sharedInputStream = null;
        _encrypted = false;

    }

    public AWFileData (String filename, SharedInputStream stream, int bytesRead)
    {
        this(filename, stream, MIME.ContentTypeApplicationOctetStream, bytesRead);
    }

    public AWFileData (String filename, SharedInputStream stream, String mimeType, int bytesRead)
    {
        super();
        _filename = filename;
        _data = null;
        _mimeType = mimeType;
        _fileIncomplete = false;
        _file = null;
        _bytesRead = bytesRead;
        _sharedInputStream = stream;
        _encrypted = false;

    }


    public AWFileData (String filename, byte[] data, String mimeType)
    {
        super();
        _filename = filename;
        _data = data;
        _mimeType = mimeType;
        _fileIncomplete = false;
        _file = null;
        _bytesRead = data == null ? 0 : data.length;
        _sharedInputStream = null;
        _encrypted = false;

    }

    public AWFileData (String filename, byte[] data, String mimeType, boolean fileIncomplete)
    {
        super();
        _filename = filename;
        _data = data;
        _mimeType = mimeType;
        _fileIncomplete = fileIncomplete;
        _file = null;
        _bytesRead = data == null ? 0 : data.length;
        _sharedInputStream = null;
        _encrypted = false;
    }

    public String filename ()
    {
        return _filename;
    }

    public byte[] data ()
    {
        if (_data == null) {
            if (_file != null) {
                _data = getFileBytes(_file);
            }
            else if (_sharedInputStream != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream(_bytesRead);
                IOUtil.inputStreamToOutputStream(_sharedInputStream.newStream(0, -1),
                        stream);
                _data = stream.toByteArray();
            }
        }
        return _data;
    }

    public String mimeType ()
    {
        return _mimeType;
    }

    public File file ()
    {
        return _file;
    }

    public boolean fileIncomplete ()
    {
        return _fileIncomplete;
    }

    public int bytesRead ()
    {
        return _bytesRead;
    }

    private byte[] getFileBytes (File file)
    {
        int length = (int)file.length();
        byte[] byteArray = new byte[length];
        try {
            InputStream istream = new FileInputStream(file);
            if (_encrypted) {
                istream = AWEncryptionProvider.getProvider().getCleartextInputStream(istream);
            }
            istream.read(byteArray);
            istream.close();
        }
        catch (IOException ioe) {
            throw new AWGenericException("Error generating byte array for file: " + ioe);
        }
        return byteArray;
    }

    public InputStream inputStream ()
    {
        _inputStream = null;
        if (_data != null) {
            _inputStream = new ByteArrayInputStream(_data);
        }
        else if (_file != null) {
            try {
                _inputStream = new FileInputStream(_file);
                if (_encrypted) {
                    _inputStream = AWEncryptionProvider.getProvider().getCleartextInputStream(_inputStream);
                }
            }
            catch (IOException ioe) {
                throw new AWGenericException("Could not create stream from downloaded file: " + ioe);
            }
        }
        else if (_sharedInputStream != null) {
            _inputStream =  _sharedInputStream.newStream(0, -1);
        }
        return _inputStream;
    }

    // dispose() is responsible for closing any input stream created and deleting the temporary file
    public void dispose ()
    {
        if (_inputStream != null) {
            AWUtil.close(_inputStream);
        }
        if (_file != null) {
            AWUtil.deleteRecursive(_file);
        }
        _data = null;
        _inputStream = null;
    }
}
