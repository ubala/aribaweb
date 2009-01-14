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

import ariba.util.core.StringUtil;
import java.io.UnsupportedEncodingException;
import java.text.Collator;

/**
    Class CJKSupport is the support class for languages that has Chinese
    characters, namely, Simplified Chinese, Traditional Chinese, Japanese,
    Korean, whose suport class should extend this class.
    
    @aribaapi private    
*/

public abstract class CJKSupport extends I18NSupport
{
    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/
    
    /**
       Class constructor.
    */        
    public CJKSupport ()
    {
    }

    public String mimeHeaderEncoding ()
    {
        return I18NConstants.EncodingB;
    }    
    
    public String normalizeCatalog (String string)
    {
        return normalizeCatalogImpl(string);
    }    
    
    public String normalizeDate (String string)
    {
        return normalizeFormatter(string);
    }
    
    public String normalizeNumber (String string)
    {
        return normalizeFormatter(string);
    }
    
    public String normalizeMoney (String string)
    {
        return normalizeFormatter(string);
    }
    
    public int getStringWidth (String string)
    {
        if (StringUtil.nullOrEmptyOrBlankString(string)) {
            return 0;
        }        
        try {
                // length of CJK characters could be quite different for 
                // different fonts, so we use UTF-8 here as a tempoary solution
            String encoding = I18NConstants.IANACharset[I18NConstants.UTF8];
            return string.getBytes(encoding).length;
        }
        catch (UnsupportedEncodingException uee) {
                // this should never happen; and even if it does, make sure it falls
                // back to its default behavior
            return string.length();
        }
    }    
    
    public int decompositionFlag ()
    {
        return Collator.FULL_DECOMPOSITION;
    }


    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/
        
    private static String normalizeFormatter (String string)
    {
        char[] chars = string.toCharArray();
        int len = chars.length;
        char c;
        
        for (int i = 0; i < len; i++) {
            c = chars[i];
              // fullwidth (),./:0123456789 ?
            if (c >= '\uFF08' && c <= '\uFF1A') {
                chars[i] = (char)(c - '\uFEE0');
            }
        }
        
        return new String(chars);
    }
    
    private static String normalizeCatalogImpl (String string)
    {
        char[] chars = string.toCharArray();
        int len = chars.length;
        char c;
        
        for (int i = 0; i < len; i++) {
            c = chars[i];


              // halfwidth alphabet?
            if ((c > '\u0040' && c < '\u005b') || (c > '\u0060' && c < '\u007b')) {
                c = Character.toLowerCase(c);
            }
              // fullwidth ASCII?
            else if (c > '\uff01' && c < '\uff5f') {
                c -= '\ufee0';
                c = Character.toLowerCase(c);
            }
              // ideographic commas
            if (c == '\uff0c' || c == '\uff64' || c == '\u3001') {
                c = ',';
            }
              // ideographic periods
            else if (c == '\uff0e' || c == '\uff61' || c == '\u3002') {
                c = '.';
            }
              // ideographic forward slash
            else if (c == '\uff3c') {
                c = '/';
            }
              // hyphens or hyphen-like symbols
            else if ((c > '\u2009' && c < '\u2016') ||
                        c == '\u30fc' || c == '\u2212')
            {
                  // 2010 - 2015 various hyphens
                  // 30FC KATAKANA-HIRAGANA PROLONGED SOUND MARK
                c = '-';
            }
                // 30FB Nakaguro to become an ASCII space.
            else if (c == '\u30fb') {
                c = ' ';
            }

              // NB:  all other punctuation, whitespace, or symbols
              //      of any kind will be a breaker
            chars[i] = c;
        }

        return new String(chars);
    }
      
}
