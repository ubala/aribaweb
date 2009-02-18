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

    $Id: //ariba/platform/ui/opensourceui/examples/Demo/gallery/metaui/BasicForm.java#3 $
*/
package gallery.html;

import ariba.ui.aribaweb.util.AWUtil
import ariba.ui.aribaweb.util.AWContentType
import ariba.ui.aribaweb.core.AWComponent
import java.util.List
import ariba.ui.aribaweb.core.AWResponseGenerating
import ariba.ui.aribaweb.core.AWResponse
import ariba.ui.aribaweb.core.AWActionCallback
import ariba.ui.table.AWTDisplayGroup

class FileUploadDownload extends AWComponent
{
    public boolean isStateless() { return false; }

    AWTDisplayGroup displayGroup
    Upload currentUpload
    List uploads = []

    AWComponent add () {
        FileUploadPanel panel = (FileUploadPanel)pageWithName(FileUploadPanel.class.getName())
        panel.setup(new UploadCallback(this, null), 100000);
        return panel;
    }

    AWComponent update () {
        FileUploadPanel panel = (FileUploadPanel)pageWithName(FileUploadPanel.class.getName())
        panel.setup(new UploadCallback(this, currentUpload), 100000);
        return panel;
    }

    void remove () { displayGroup.selectedObjects().each { uploads.remove(it) } }

    Upload updateFile (Upload upload, fileName, mimeType, bytes) {
        upload.bytes = bytes
        upload.mimeType = mimeType
        upload.fileName = fileName
        return upload
    }

    AWResponseGenerating doDownload () {
        // requestContext().assertFileDownloadCompatibleRequestRequired();
        AWResponse fileResponse = application().createResponse();
        fileResponse.setContentType(AWContentType.contentTypeNamed(currentUpload.mimeType));
        fileResponse.setHeaderForKey("attachment; filename=\"${currentUpload.fileName}\"", "Content-Disposition");
        fileResponse.setContent(currentUpload.bytes)
        return fileResponse
    }
}

// in Java I'd just make this an anonymous inner class...
class UploadCallback extends AWActionCallback {
    FileUploadDownload _caller
    Upload _orig
    UploadCallback (FileUploadDownload caller, Upload orig) { super(caller); _caller = caller; _orig = orig }
    public AWResponseGenerating doneAction (AWComponent sender) {
        if (sender.bytes) {
            if (_orig) {
                _caller.updateFile(_orig, sender.fileName, sender.mimeType, sender.bytes)
            } else {
                _caller.uploads += _caller.updateFile(new Upload(), sender.fileName, sender.mimeType, sender.bytes)
            }
        }
        return _caller.pageComponent();
    }
}

class Upload {
    String fileName
    String description
    def mimeType
    byte[] bytes
}