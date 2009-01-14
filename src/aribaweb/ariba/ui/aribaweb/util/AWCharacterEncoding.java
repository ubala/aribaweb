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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWCharacterEncoding.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.util.AWCaseInsensitiveHashtable;
import ariba.ui.aribaweb.util.AWGenericException;
import java.util.Map;
import ariba.util.core.StringUtil;
import ariba.util.i18n.I18NConstants;
import ariba.util.i18n.LocaleSupport;
import java.io.UnsupportedEncodingException;

public final class AWCharacterEncoding extends AWBaseObject
{
    // Note: these constants are based on IANA definition.
    //       Java accepts these values although it is undocumented.
    //
    private static final String UTF8String         = I18NConstants.IANACharset[I18NConstants.UTF8];
    private static final String ShiftJISString     = I18NConstants.IANACharset[I18NConstants.ShiftJIS];
    private static final String ASCIIString        = I18NConstants.IANACharset[I18NConstants.ASCII];
    private static final String ISO8859_1String    = I18NConstants.IANACharset[I18NConstants.ISO8859_1];
    private static final String ksc56011987String  = I18NConstants.IANACharset[I18NConstants.KSC5601];
    private static final String big5String         = I18NConstants.IANACharset[I18NConstants.Big5];
    private static final String gb2312String       = I18NConstants.IANACharset[I18NConstants.GB2312];
        // the following xsjisString should be removed later
        // right now, only obsoletely used by acsn/service/apps/Supplier/DirectAction.java
    private static final String xsjisString        = "x-sjis";

    public static final int SupportedEncodingCount = 8;
    private static Map CharacterEncodingsByName;
    private static AWCharacterEncoding[] CharacterEncodingsByIndex;
    public static final AWCharacterEncoding UTF8 = AWCharacterEncoding.registerCharacterEncoding(UTF8String);
    public static final AWCharacterEncoding ShiftJIS = AWCharacterEncoding.registerCharacterEncoding(ShiftJISString);
    public static final AWCharacterEncoding ISO8859_1 = AWCharacterEncoding.registerCharacterEncoding(ISO8859_1String);
    public static final AWCharacterEncoding ASCII = AWCharacterEncoding.registerCharacterEncoding(ASCIIString);
        // the following xsjis should be removed later, see comment in definition of xsjisString
    public static final AWCharacterEncoding xsjis = AWCharacterEncoding.registerCharacterEncoding(xsjisString);
    public static final AWCharacterEncoding Ksc56011987 = AWCharacterEncoding.registerCharacterEncoding(ksc56011987String);
    public static final AWCharacterEncoding Big5 = AWCharacterEncoding.registerCharacterEncoding(big5String);
    public static final AWCharacterEncoding Gb2312 = AWCharacterEncoding.registerCharacterEncoding(gb2312String);

    public static final AWCharacterEncoding Default = AWCharacterEncoding.defaultEncoding();
    private static int NextIndex = 0;

    public String name;
    public int index;

    private static AWCharacterEncoding registerCharacterEncoding (String characterEncodingName)
    {
        if (CharacterEncodingsByName == null) {
            CharacterEncodingsByName = new AWCaseInsensitiveHashtable();
            CharacterEncodingsByIndex = new AWCharacterEncoding[SupportedEncodingCount];
        }
        AWCharacterEncoding characterEncoding = new AWCharacterEncoding(characterEncodingName);
        CharacterEncodingsByName.put(characterEncoding.name, characterEncoding);
        CharacterEncodingsByIndex[characterEncoding.index] = characterEncoding;
        return characterEncoding;
    }

    private static AWCharacterEncoding defaultEncoding ()
    {
        AWCharacterEncoding awencoding = UTF8;
        String encoding = LocaleSupport.defaultUIEncoding();
        if (!StringUtil.nullOrEmptyOrBlankString(encoding)) {
            awencoding = characterEncodingNamed(encoding);
            if (awencoding == null) {
                awencoding = UTF8;
            }
        }
        return awencoding;
    }
    
    private AWCharacterEncoding (String encodingName)
    {
        super();
        name = encodingName;
        index = NextIndex;
        NextIndex++;
    }

    public static AWCharacterEncoding characterEncodingNamed (String characterEncodingName)
    {
        AWCharacterEncoding characterEncoding = null;
        if (characterEncodingName != null) {
            characterEncoding = (AWCharacterEncoding)CharacterEncodingsByName.get(characterEncodingName);
        }
        return characterEncoding;
    }

    public static AWCharacterEncoding characterEncodingWithIndex (int index)
    {
        return CharacterEncodingsByIndex[index];
    }

    public static AWCharacterEncoding[] supportedEncodings ()
    {
        return CharacterEncodingsByIndex;
    }

    public byte[] getBytes (String string)
    {
        try {
            return string.getBytes(name);
        }
        catch (UnsupportedEncodingException unsuportedCharacterEncodingException) {
            throw new AWGenericException(unsuportedCharacterEncodingException);
        }
    }

    public String newString (byte[] bytes)
    {
        try {
            return new String(bytes, name);
        }
        catch (UnsupportedEncodingException unsuportedCharacterEncodingException) {
            throw new AWGenericException(unsuportedCharacterEncodingException);
        }
    }

}
