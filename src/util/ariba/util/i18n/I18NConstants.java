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

    $Id: $
*/

package ariba.util.i18n;


/**
    Class I18NConstants contains all public constants used by I18N.

    @aribaapi ariba
*/

public final class I18NConstants
{
    /*
        Encoding strings for email header
    */
    public static final String EncodingQ  = "Q";
    public static final String EncodingB  = "B";

    private static int      count         = 0;

    public static final int ASCII         = count++;
    public static final int ISO8859_1     = count++;
    public static final int ISO8859_2     = count++;
    public static final int ISO8859_8     = count++;
    public static final int ISO2022CN     = count++;
    public static final int ISO2022JP     = count++;
    public static final int ISO2022KR     = count++;
    public static final int Big5          = count++;
    public static final int GB2312        = count++;
    public static final int KSC5601       = count++;
    public static final int UTF8          = count++;
    public static final int ShiftJIS      = count++;
    public static final int EUC_KR        = count++;
    public static final int EUC_JP        = count++;
    public static final int MS932         = count++;
    public static final int Cp1252        = count++;
    public static final int Cp1251        = count++;

    public static final String[] CharacterEncoding = new String[count];
    public static final String[] IANACharset       = new String[count];


    static {
            // Java encoding names
        CharacterEncoding[ASCII]         = "ASCII";
        CharacterEncoding[ISO8859_1]     = "ISO8859_1";
        CharacterEncoding[ISO8859_2]     = "ISO8859_2";
        CharacterEncoding[ISO8859_8]     = "ISO8859_8";
        CharacterEncoding[ISO2022CN]     = "ISO2022CN";
        CharacterEncoding[ISO2022JP]     = "ISO2022JP";
        CharacterEncoding[ISO2022KR]     = "ISO2022KR";
        CharacterEncoding[Big5]          = "Big5";
        CharacterEncoding[GB2312]        = "GB2312";
        CharacterEncoding[KSC5601]       = "KSC5601";
        CharacterEncoding[UTF8]          = "UTF8";
        CharacterEncoding[ShiftJIS]      = "Shift_JIS";
        CharacterEncoding[EUC_KR]        = "EUC_KR";
        CharacterEncoding[EUC_JP]        = "EUC_JP";
        CharacterEncoding[MS932]         = "MS932";
        CharacterEncoding[Cp1252]        = "Cp1252";
        CharacterEncoding[Cp1251]        = "Cp1251";

        /*
            MIME, HTML, and XML charset names (defined by IANA)
            note: they are not case-sensitive.
            IANA prefered forms are all cited in upper case MIME Charset.
        */
        IANACharset[ASCII]               = "ASCII";
        IANACharset[ISO8859_1]           = "ISO-8859-1";
        IANACharset[ISO8859_2]           = "ISO-8859-2";
        IANACharset[ISO8859_8]           = "ISO-8859-8";
        IANACharset[ISO2022CN]           = "ISO-2022-CN";
        IANACharset[ISO2022JP]           = "ISO-2022-JP";
        IANACharset[ISO2022KR]           = "ISO-2022-KR";
        IANACharset[Big5]                = "Big5";
        IANACharset[GB2312]              = "GB2312";
        IANACharset[KSC5601]             = "KS_C_5601-1987";
        IANACharset[UTF8]                = "UTF-8";
        IANACharset[ShiftJIS]            = "Shift_JIS";
        IANACharset[EUC_KR]              = "EUC-KR";
        IANACharset[EUC_JP]              = "EUC-JP";
        IANACharset[MS932]               = "Windows-31J";
        IANACharset[Cp1252]              = "windows-1252";
        IANACharset[Cp1251]              = "windows-1251";
    }

        // For reading files only
        // Latin 1 + extensions
    public static final String CharacterEncodingCp1252     = "Cp1252";
        // Hebrew
    public static final String CharacterEncodingCp1255     = "Cp1255";
        // Russian
    public static final String CharacterEncodingCp1251     = "Cp1251";
        // Japanese
    public static final String CharacterEncodingMS932      = "MS932";
        // Simplified Chinese
    public static final String CharacterEncodingMS936      = "MS936";
        // Korean
    public static final String CharacterEncodingMS949      = "MS949";
        // Traditional Chinese
    public static final String CharacterEncodingMS950      = "MS950";


    public static int count ()
    {
        return count;
    }

}
