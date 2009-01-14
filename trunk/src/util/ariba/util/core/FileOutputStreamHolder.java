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

    $Id: //ariba/platform/util/core/ariba/util/core/FileOutputStreamHolder.java#2 $
*/
package ariba.util.core;

import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

/**
    @aribaapi ariba
*/
public class FileOutputStreamHolder implements OutputStreamHolder
{
    //-----------------------------------------------------------------------
    // private data members

    private File _file;
    private FileOutputStream _outputStream;

    //-----------------------------------------------------------------------
    // constructors

    /**
        @aribaapi ariba
    */
    public FileOutputStreamHolder (File file)
    {
        _file = file;
        _outputStream = null;
    }

    /**
        @aribaapi ariba
    */
    public OutputStream getOutputStream () throws IOException
    {
        if (_outputStream == null) {
            _outputStream = new FileOutputStream(_file);
        }
        return _outputStream;
    }
}
