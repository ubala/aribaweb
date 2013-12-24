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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWFileUploadInternals.java#11 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFileData;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWMimeReader;
import ariba.util.core.Fmt;
import ariba.util.core.MIME;
import ariba.util.core.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import java.net.URL;
import java.net.URLConnection;

/**
 * Because applyValues is overridden and super.applyValues is NOT
 * called, the template for this component should not contain ANY elements which
 * consume element id's.  The element id stack is maintained by the template in render
 * phase and the overridden applyValues method in the apply phase.
 * @aribaapi private
 */
public final class AWFileUploadInternals extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.inputStream, BindingNames.bytes, BindingNames.name,
        BindingNames.filename, BindingNames.mimeType, BindingNames.fileSizeExceeded,
        BindingNames.file, BindingNames.newMode, BindingNames.maxLength,
        BindingNames.encrypt, BindingNames.disabled
    };

    public AWEncodedString _elementId;
    public String _fileUploadName;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void sleep ()
    {
        _fileUploadName = null;
        _elementId = null;
    }

    public boolean hasMaxLength ()
    {
        return hasBinding(BindingNames.maxLength);
    }

    public String fileUploadName ()
    {
        if (_fileUploadName == null) {
            _fileUploadName = (String)valueForBinding(BindingNames.name);
            if (_fileUploadName == null) {
                _fileUploadName = _elementId.string();
            }
        }
        return _fileUploadName;
    }

    public Object disabled ()
    {
        boolean disabledBinding = booleanValueForBinding(BindingNames.disabled);
        return disabledBinding ? "true": null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        super.renderResponse(requestContext, component);

        // if there is a maxLength binding, then throw this value into the HttpSession
        // to allow it be used by the request handler during Mime parsing.  Note that we
        // us the HttpSession so we avoid any issues with locking the AWSession.
        AWSession session = session();
        if (parent().hasBinding(BindingNames.maxLength)) {
            Integer maxLength = new Integer(intValueForBinding(BindingNames.maxLength));
            session.httpSession().setAttribute(fileUploadName(),maxLength);
        }
        if (parent().hasBinding(BindingNames.encrypt)) {
            boolean encrypt = booleanValueForBinding(BindingNames.encrypt);
            if (encrypt && parent().hasBinding(BindingNames.file)) {
                throw new AWGenericException("File binding not supported when encrypt is requested. Use inputStream binding instead.");
            }
            session.httpSession().setAttribute(BindingNames.encrypt +"."+fileUploadName(),
                                               encrypt);
        }
        // stash the user's preferred locale for use to construct localized messages
        session.httpSession().setAttribute(Locale.class.getName(), session.preferredLocale());
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        // Note that this does not call super.applyValues() -- everything is
        // done in this method.  This really limits what can/should be done in the
        // template. The reason for this is due to the call to fileDataForKey() rather
        // than formValueForKey.  Perhaps I can put a hidden field in which tells the
        // request to look for NSData and convert those to AWFileData?
        _elementId = requestContext().nextElementId();

        String fileUploadName = fileUploadName();

        // Clean up the information stashed in the httpSession if necessary.
        if (hasBinding(BindingNames.maxLength)) {
            session().httpSession().removeAttribute(fileUploadName);
        }

        AWFileData fileData = requestContext.request().fileDataForKey(fileUploadName);
        if (fileData == null && inPlaybackMode()) {
            fileData = fileDataFromInputUrl();
        }

        if (fileData != null && (fileData.bytesRead() > 0 || booleanValueForBinding("newMode"))) {
            String filename = fileData.filename();
            if (filename != null) {
                //some version of IE on windowsXP give the full path to the file
                //trim this to get the filename
                filename = AWUtil.lastComponent(filename, '\\');
            }
            setValueForBinding(filename, BindingNames.filename);
            setValueForBinding(fileData.mimeType(), BindingNames.mimeType);
            setValueForBinding(fileData.fileIncomplete(), BindingNames.fileSizeExceeded);

            // Only one binding for file data is honored. Precedence order: file, input stream, byte array.
            if (parent().hasBinding(BindingNames.file)) {
                File file = fileData.file();
                if ((file == null) && (fileData.data() != null)) {
                    throw new AWGenericException("File binding not supported for in memory file uploads");
                }
                setValueForBinding(file, BindingNames.file);
            }
            else {
                if (parent().hasBinding(BindingNames.inputStream)) {
                    InputStream inputStream = fileData.inputStream();
                    setValueForBinding(inputStream, BindingNames.inputStream);
                }
                else if (parent().hasBinding(BindingNames.bytes)) {
                    byte[] byteArray = fileData.data();
                    setValueForBinding(byteArray, BindingNames.bytes);
                }
            }
        }
        // This is here to balance the glid stack since we do not call super.
        requestContext.popFormInputElementId();
    }

    public boolean inPlaybackMode ()
    {
        return requestContext()._debugIsInPlaybackMode();
        // return false;
    }

    public String urlInputName ()
    {
        return fileUploadName() + "_URL_FIELD";
    }

    AWFileData fileDataFromInputUrl ()
    {
        String urlString = requestContext().request().formValueForKey(urlInputName());
        if (StringUtil.nullOrEmptyOrBlankString(urlString)) return null;
        String fileName = AWUtil.lastComponent(urlString, '/');
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            byte[] bytes = AWUtil.getBytes(conn.getInputStream());
            String contentType = conn.getContentType();
            if(contentType == null) {
                contentType = MIME.ContentTypeApplicationOctetStream;
            }
            String uploadDirPath = AWMimeReader.fileUploadDirectory();
            if (uploadDirPath != null) {
                File uploadDirectory = new File(uploadDirPath);
                File uploadFile = File.createTempFile("awupload", ".tmp", uploadDirectory);
                AWUtil.writeToFile(bytes, uploadFile);
                return new AWFileData(fileName, uploadFile, contentType, false, bytes.length, false);
            }
            return new AWFileData(fileName, bytes);
        } catch (IOException e) {
            throw new AWGenericException(Fmt.S("Exception in processing playback simulated file upload on URL: %s", urlString), e);
        }
    }
}
