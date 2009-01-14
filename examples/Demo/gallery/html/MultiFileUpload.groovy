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

import ariba.ui.aribaweb.util.AWContentType
import ariba.ui.aribaweb.core.AWComponent
import java.util.List
import ariba.ui.aribaweb.core.AWResponseGenerating
import ariba.ui.aribaweb.core.AWResponse
import ariba.ui.table.AWTDisplayGroup
import ariba.ui.aribaweb.core.AWErrorInfo

class MultiFileUpload extends AWComponent
{
    public boolean isStateless() { return false; }

    AWTDisplayGroup displayGroup
    Upload currentUpload
    List uploads = []

    public void init ()
    {
        add()
    }

    void setFileSizeExceeded (isError)
    {
        if (isError) {
            AWErrorInfo error = new AWErrorInfo(currentUpload, "fileName", null,
                            "Maximum filesize exceeded", "100000", false);
            recordValidationError(error);
            
            currentUpload.bytes = null
            currentUpload.fileName = null
            currentUpload.mimeType = null
            println ("Overload for upload: ${currentUpload}")
        }
    }

    void add () {
        uploads += new Upload()
    }

    void update () {
        currentUpload.fileName = null
    }

    void upload () {
        errorManager().checkErrorsAndEnableDisplay()
    }

    void remove () { uploads.remove(currentUpload) }

    AWResponseGenerating doDownload () {
        // requestContext().assertFileDownloadCompatibleRequestRequired();
        AWResponse fileResponse = application().createResponse();
        fileResponse.setContentType(AWContentType.contentTypeNamed(currentUpload.mimeType));
        fileResponse.setHeaderForKey("attachment; filename=\"${currentUpload.fileName}\"", "Content-Disposition");
        fileResponse.setContent(currentUpload.bytes)
        return fileResponse
    }
    
}
