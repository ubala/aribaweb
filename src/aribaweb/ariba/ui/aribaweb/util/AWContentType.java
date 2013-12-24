/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWContentType.java#26 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.StringUtil;

public final class AWContentType extends AWBaseObject
{
    public static final int SupportedContentTypeCount = 17;
    private static GrowOnlyHashtable ContentTypesByName;
    private static AWCaseInsensitiveHashtable ContentTypesByFileExtension = new AWCaseInsensitiveHashtable();
    private static AWContentType[] ContentTypesByIndex;

    public static final AWContentType TextHtml = AWContentType.registerContentType("text/html");
    public static final AWContentType TextCss = AWContentType.registerContentType("text/css");
    public static final AWContentType TextXml = AWContentType.registerContentType("text/xml");
    public static final AWContentType TextAscii = AWContentType.registerContentType("text/ascii");
    public static final AWContentType TextPlain = AWContentType.registerContentType("text/plain");
    public static final AWContentType ImageGif = AWContentType.registerContentType("image/gif");
    public static final AWContentType ImageIco = AWContentType.registerContentType("image/vnd.microsoft.icon");
    public static final AWContentType ImageJpeg = AWContentType.registerContentType("image/jpeg");
    public static final AWContentType ImagePng = AWContentType.registerContentType("image/png");
    public static final AWContentType ApplicationWWWFormUrlEncoded = AWContentType.registerContentType("application/x-www-form-urlencoded");
    public static final AWContentType ApplicationXJavascript = AWContentType.registerContentType("application/x-javascript");
    public static final AWContentType ApplicationXVBscript = AWContentType.registerContentType("application/x-vbscript");
    public static final AWContentType ApplicationCsv = AWContentType.registerContentType("application/csv");
    public static final AWContentType ApplicationVndMsexcel = AWContentType.registerContentType("application/vnd.ms-excel");
    public static final AWContentType ApplicationVndMsexcel2007 = AWContentType.registerContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    public static final AWContentType ApplicationMSWord = AWContentType.registerContentType("application/msword");
    public static final AWContentType ApplicationMSProject = AWContentType.registerContentType("application/vnd.ms-project");
    public static final AWContentType ApplicationUnknown = AWContentType.registerContentType("application/unknown");
    public static final AWContentType ApplicationOctetStream = AWContentType.registerContentType("application/octet-stream");
    public static final AWContentType ApplicationZip = AWContentType.registerContentType("application/zip");
    public static final AWContentType ApplicationXZipCompressed = AWContentType.registerContentType("application/x-zip-compressed");
    public static final AWContentType ApplicationDownload = AWContentType.registerContentType("application/download");
    public static final AWContentType ApplicationXCompressed = AWContentType.registerContentType("application/x-compressed");
    public static final AWContentType ApplicationJnlp = AWContentType.registerContentType("application/jnlp");
    public static final AWContentType ApplicationJson = AWContentType.registerContentType("application/json");
    public static final AWContentType MultipartRelated = AWContentType.registerContentType("multipart/related");
    public static final AWContentType MultipartFormData = AWContentType.registerContentType("multipart/form-data");

    public static final AWContentType Default = TextHtml;
    private static int NextIndex = 0;

    // We cannot use final here due to a bug in the compiler
    public String name;
    public int index;
    private String[] _contentTypeHeadersByCharacterEncodingIndex;

    static {
        registerContentTypeForFileExtension("html", TextHtml);
        registerContentTypeForFileExtension("css", TextCss);
        registerContentTypeForFileExtension("xml", TextXml);
        registerContentTypeForFileExtension("txt", TextAscii);
        registerContentTypeForFileExtension("gif", ImageGif);
        registerContentTypeForFileExtension("jpeg", ImageJpeg);
        registerContentTypeForFileExtension("ico", ImageIco);
        registerContentTypeForFileExtension("jpg", ImageJpeg);
        registerContentTypeForFileExtension("js", ApplicationXJavascript);
        registerContentTypeForFileExtension("vbs", ApplicationXVBscript);
        registerContentTypeForFileExtension("csv", ApplicationCsv);
        registerContentTypeForFileExtension("zip", ApplicationDownload);
        registerContentTypeForFileExtension("jnlp", ApplicationJnlp);
        registerContentTypeForFileExtension("json", ApplicationJson);
        registerContentTypeForFileExtension("xls", ApplicationVndMsexcel);
        registerContentTypeForFileExtension("xlsx", ApplicationVndMsexcel2007);
        registerContentTypeForFileExtension("doc", ApplicationMSWord);
        registerContentTypeForFileExtension("png", ImagePng);
        // if you add a new Content Type For File Extension,
        // do not forget to increase the SupportedContentTypeCount
    }

    private static AWContentType registerContentType (String contentTypeName)
    {
        if (ContentTypesByName == null) {
            ContentTypesByName = new GrowOnlyHashtable();
            ContentTypesByIndex = new AWContentType[SupportedContentTypeCount];
        }
        AWContentType contentType = new AWContentType(contentTypeName);
        ContentTypesByName.put(contentType.name, contentType);
        if (contentType.index >= ContentTypesByIndex.length) {
            ContentTypesByIndex = (AWContentType[])AWUtil.realloc(ContentTypesByIndex, contentType.index + 1);
        }
        ContentTypesByIndex[contentType.index] = contentType;
        return contentType;
    }

    public static AWContentType contentTypeNamed (String contentTypeName)
    {
        AWContentType contentType = null;
        if (contentTypeName != null) {
            contentType = (AWContentType)ContentTypesByName.get(contentTypeName);
            if (contentType == null) {
                contentType = AWContentType.registerContentType(contentTypeName);
            }
        }
        return contentType;
    }

    public static void registerContentTypeForFileExtension (String fileExtension, AWContentType contentType)
    {
        synchronized (ContentTypesByFileExtension) {
            ContentTypesByFileExtension.put(fileExtension, contentType);
        }
    }

    public static AWContentType contentTypeForFileExtension (String fileExtension)
    {
        synchronized (ContentTypesByFileExtension) {
            return (AWContentType)ContentTypesByFileExtension.get(fileExtension);
        }
    }

    private AWContentType (String contentTypeName)
    {
        super();
        name = contentTypeName;
        index = NextIndex;
        NextIndex++;
        if (contentTypeName.startsWith("text/")) {
            // only "text" content-types need to have the charset parameter
            _contentTypeHeadersByCharacterEncodingIndex = new String[0];
        }
    }

    public String header (AWCharacterEncoding characterEncoding)
    {
        String contentTypeHeader = name;
        if (characterEncoding != null && _contentTypeHeadersByCharacterEncodingIndex != null) {
            int characterEncodingIndex = characterEncoding.index;
            if (characterEncodingIndex >= _contentTypeHeadersByCharacterEncodingIndex.length) {
                _contentTypeHeadersByCharacterEncodingIndex = (String[])AWUtil.realloc(_contentTypeHeadersByCharacterEncodingIndex, characterEncodingIndex + 1);
            }
            if ((contentTypeHeader = _contentTypeHeadersByCharacterEncodingIndex[characterEncodingIndex]) == null) {
                contentTypeHeader = StringUtil.strcat(name, "; charset=", characterEncoding.name);
                _contentTypeHeadersByCharacterEncodingIndex[characterEncodingIndex] = contentTypeHeader;
            }
        }
        return contentTypeHeader;
    }
}
