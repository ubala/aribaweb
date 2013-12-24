/*
    Copyright (c) 2013-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/MIME.java#12 $

    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/MIME.java#12 $
*/

package ariba.util.core;

import ariba.util.i18n.LocaleSupport;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
    MIME.  The Multipurpose Internet Mail Extensions standard.

    See http://info.internet.isi.edu/in-notes/rfc/files/rfc2045.txt
    and http://info.internet.isi.edu/in-notes/rfc/files/rfc2046.txt
    and http://www.isi.edu/in-notes/iana/assignments/character-sets

    @aribaapi private
*/
public final class MIME extends MimeEncoding
{


    /*-----------------------------------------------------------------------
        Public Constants
      -----------------------------------------------------------------------*/

        // carriage return - line feed sequence
    public static final String CRLF = "\r\n";
    public static final byte[] CRLFArray  = { 0x0d, 0x0a };


    public static final String HeaderMimeVersion = "MIME-Version";
    public static final String HeaderContentType = "Content-type";
    public static final String HeaderContentEncoding = "Content-encoding";
    public static final String HeaderContentId = "Content-ID";
    public static final String HeaderContentLanguage = "Content-language";
    public static final String HeaderContentLength = "Content-length";
    public static final String HeaderContentDisposition =
        "Content-disposition";
    public static final String HeaderContentTransferEncoding =
        "Content-transfer-encoding";

    public static final String MIMEVersion = "1.0";

    public static final String EncodingQuotedPrintable = "quoted-printable";
    public static final String EncodingBase64          = "base64";
    public static final String Encoding7Bit            = "7bit";
    public static final String Encoding8Bit            = "8bit";
    public static final String EncodingBinary          = "binary";
        //public static final String Attachment              = "";

        // various media types
    public static final String ContentTypeMultipartMixed =
        "multipart/mixed";
    public static final String ContentTypeMultipartFormData =
        "multipart/form-data";
    public static final String ContentTypeMultipartRelated =
        "multipart/related";
    public static final String ContentTypeTextPlain =
        "text/plain";
    public static final String ContentTypeTextHTML =
        "text/html";
    public static final String ContentTypeTextXML =
        "text/xml";
    public static final String ContentTypeApplicationCSV =
        "application/csv";
    public static final String ContentTypeApplicationOctetStream =
        "application/octet-stream";
    public static final String ContentTypeApplicationXAriba =
        "application/x-ariba";
    public static final String ContentTypeApplicationXWWWFormURLEncoded =
        "application/x-www-form-urlencoded";
    public static final String ContentTypeApplicationMSExcel =
        "application/vnd.ms-excel";
    public static final String ContentTypeImageGIF =
        "image/gif";
    public static final String ContentTypeImageJPEG =
        "image/jpeg";
    public static final String ContentTypeZip = "application/zip";
    public static final String ContentTypePDF = "application/pdf";

        // used with ContentTypeMultipartMixed
    public static final String ParameterBoundary = "boundary";

        // used with ContentTypeMultipartRelated
    public static final String ParameterStart = "start";

        // used with ContentTypeTextHTML
    public static final String ParameterCharSet  = "charset";
    public static final String CharSetUSASCII    = "US-ASCII";
    public static final String CharSetISO88593   = "ISO-8859-3";
    public static final String CharSetISO88594   = "ISO-8859-4";
    public static final String CharSetISO88595   = "ISO-8859-5";
    public static final String CharSetISO88596   = "ISO-8859-6";
    public static final String CharSetISO88597   = "ISO-8859-7";
    public static final String CharSetISO88599   = "ISO-8859-9";
    public static final String EncodingISO2022JP  = "ISO2022JP";
    public static final String CharSetISO2022JP2 = "ISO-2022-JP-2";
    public static final String CharSetKOI8R      = "KOI8-R";
    public static final String CharSetEUCCN      = "EUC-CN";
    public static final String CharSetEUCTW      = "EUC-TW";
    public static final String CharSetISO2022CN2 = "ISO-2022-CN-EXT";


    private MIME ()
    {
       super();
       EncodingSet.add(CharSetUSASCII.toLowerCase());
       EncodingSet.add(CharSetISO88593.toLowerCase());
       EncodingSet.add(CharSetISO88594.toLowerCase());
       EncodingSet.add(CharSetISO88595.toLowerCase());
       EncodingSet.add(CharSetISO88596.toLowerCase());
       EncodingSet.add(CharSetISO88597.toLowerCase());
       EncodingSet.add(CharSetISO88599.toLowerCase());
       EncodingSet.add(CharSetISO2022JP2.toLowerCase());
       EncodingSet.add(CharSetKOI8R.toLowerCase());
       EncodingSet.add(CharSetEUCCN.toLowerCase());
       EncodingSet.add(CharSetEUCTW.toLowerCase());
       EncodingSet.add(CharSetISO2022CN2.toLowerCase());
    }

        // used with HeaderContentDisposition
    public static final String ParameterName     = "name";
    public static final String ParameterFilename = "filename";

    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

    private List MIME_charsets = null;

    //--- String --------------------------------------------------------------

    /**
        Format a String header in memory
    */
    public static String header (String type, String body)
    {
        return Fmt.S("%s: %s" + CRLF,  type, body);
    }

    /**
        Format a String header in memory with a parameter value
    */
    public static String header (String type,  String body,
                                 String param, String value)
    {
        return Fmt.S("%s: %s; %s=%s" + CRLF, type, body, param, value);
    }


    //--- PrintWriter ---------------------------------------------------------

    /**
        Format a String header directly to out
    */
    public static void header (PrintWriter out, String type, String body)
    {
        crlf(out, "%s: %s",  type, body);
    }

    /**
        Convenience routine for printing lines of HTML
    */
    public static void crlf (PrintWriter out)
    {
        Fmt.F(out, CRLF);
    }

    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (PrintWriter out, String format)
    {
        Fmt.F(out, "%s", format);
        crlf(out);
    }

    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (PrintWriter out, String format,
                             Object o1)
    {
        Fmt.F(out, format, o1);
        crlf(out);
    }

    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (PrintWriter out, String format,
                             Object o1, Object o2)
    {
        Fmt.F(out, format, o1, o2);
        crlf(out);
    }

    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (PrintWriter out, String format,
                             Object o1, Object o2, Object o3)
    {
        Fmt.F(out, format, o1, o2, o3);
        crlf(out);
    }

    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (PrintWriter out, String format,
                             Object o1, Object o2, Object o3, Object o4)
    {
        Fmt.F(out, format, o1, o2, o3, o4);
        crlf(out);
    }

    //--- OutputStream --------------------------------------------------------

    /**
        Format a String header directly to out
    */
    public static void header (OutputStream out, String type, String body)
      throws IOException
    {
        crlf(out, "%s: %s",  type, body);
    }

    /**
        Convenience routine for printing lines of HTML
    */
    public static void crlf (OutputStream out)
      throws IOException
    {
        Fmt.O(out, CRLF);
    }

    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (OutputStream out, String format)
      throws IOException
    {
        Fmt.O(out, format);
        crlf(out);
    }
    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (OutputStream out, String format,
                             Object o1)
      throws IOException
    {
        Fmt.O(out, format, o1);
        crlf(out);
    }
    /**
        Convenience routine for printing CRLF terminated lines
    */
    public static void crlf (OutputStream out, String format,
                             Object o1, Object o2)
      throws IOException
    {
        Fmt.O(out, format, o1, o2);
        crlf(out);
    }

    public static String getCharset (Locale locale)
    {
        return LocaleSupport.mimeBodyCharset(locale);
    }

    public static String getMailEncoding (Locale locale)
    {
        return LocaleSupport.mimeBodyEncoding(locale);
    }

    public static String getMetaCharset (Locale locale)
    {
        return LocaleSupport.mimeAttachmentEncoding(locale);
    }

    public static boolean isValidCharset (String charset)
    {
        if (EncodingSet.contains(charset.toLowerCase())) {
                return true;
        }
        else {
            return false;
        }
    }
}
