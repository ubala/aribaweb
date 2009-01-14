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

class FileUploadDownload extends AWComponent
{
    public boolean isStateless() { return false; }

    Upload newUpload = new Upload(), currentUpload
    List uploads = []
    InputStream inputStream
    boolean fileSizeExceeded

    void doUpload () {
        if (fileSizeExceeded) {
            recordValidationError("file", "Your upload exceeds the maximuma allowable size", null);
            errorManager().checkErrorsAndEnableDisplay();
            return
        }
        
        newUpload.bytes = AWUtil.getBytes(inputStream)
        uploads += newUpload
        newUpload = new Upload()        
    }

    void fileSizeExceeded () {
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

class Upload {
    String title, fileName
    def mimeType
    byte[] bytes
}