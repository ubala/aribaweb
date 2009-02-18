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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/FileUploadChooser.java#3 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.util.AWMimeReader;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.SystemUtil;
import ariba.util.core.Fmt;

import java.io.File;
import java.io.IOException;

public class FileUploadChooser extends AWComponent
{
    static {
        // ensure that we have a file upload dir
        if (AWMimeReader.fileUploadDirectory() == null) {
            File tmpDir = SystemUtil.getLocalTempDirectory();
            AWMimeReader.setFileUploadDirectory(createTmpDir("awupload", ".dir", tmpDir).getAbsolutePath());
        }
    }

    String _filename;
    public String _displayName;

    protected void awake ()
    {
        super.awake();
        _displayName = stringValueForBinding(BindingNames.filename);
        if (_displayName == null) {
            File file = (File)valueForBinding(BindingNames.file);
            if (file != null) _displayName = file.getName();
        }
        if (_displayName == null) {
            byte[] bytes = (byte[])valueForBinding(BindingNames.bytes);
            if (bytes != null) _displayName = String.format("%,d bytes" /* */, bytes.length);
        }
    }

    protected void sleep ()
    {
        _filename = null;
        _displayName = null;
    }

    public void clear ()
    {
        setValueForBinding(null, BindingNames.filename);
        setValueForBinding(null, BindingNames.file);
        setValueForBinding(null, BindingNames.bytes);
    }

    public void setFilename (String filename)
    {
        _filename = filename;
        setValueForBinding(_filename, BindingNames.filename);        
    }
        
    public void setFile (File file)
    {
        File uploadDirectory = new File(AWMimeReader.fileUploadDirectory());
        File uploadDir = createTmpDir("awupload", ".dir", uploadDirectory);
        File newFile = new File(uploadDir, _filename);

        file.renameTo(newFile);
        newFile.deleteOnExit();
        setValueForBinding(newFile, BindingNames.file);
    }
    
    static File createTmpDir (String prefix, String extension, File parentDir)
    {
        File dir;
        try {
            dir = File.createTempFile(prefix, extension, parentDir);
            dir.delete();
            dir = new File(dir.getPath());
            dir.mkdirs();
            return dir;
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    public boolean hasContent ()
    {
        return hasContentNamed(null);
    }
}
