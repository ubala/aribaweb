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

import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import java.util.Iterator;
import java.util.List;

/**
    Support class for Japanese.

    @aribaapi private
*/

public class Support_ja extends CJKSupport
{
    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

      // zero-base table for mapping half-width katakana to full-width
      // this is in 50-on order (akasatana...)
    private final static char FWKatakana[] = {
        '\u3002','\u300c','\u300d','\u3001','\u30fb',
        '\u30f2','\u30a1','\u30a3','\u30a5','\u30a7',
        '\u30a9','\u30e3','\u30e5','\u30e7','\u30c3',
        '\u30fc','\u30a2','\u30a4','\u30a6','\u30a8',
        '\u30aa','\u30ab','\u30ad','\u30af','\u30b1',
        '\u30b3','\u30b5','\u30b7','\u30b9','\u30bb',
        '\u30bd','\u30bf','\u30c1','\u30c4','\u30c6',
        '\u30c8','\u30ca','\u30cb','\u30cc','\u30cd',
        '\u30ce','\u30cf','\u30d2','\u30d5','\u30d8',
        '\u30db','\u30de','\u30df','\u30e0','\u30e1',
        '\u30e2','\u30e4','\u30e6','\u30e8','\u30e9',
        '\u30ea','\u30eb','\u30ec','\u30ed','\u30ef',
        '\u30f3','\u309b','\u309c'
    };


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
       Class constructor.
    */
    public Support_ja ()
    {
    }

    public String fileEncoding ()
    {
        return I18NConstants.IANACharset[I18NConstants.MS932];
    }

    public String mimeBodyEncoding ()
    {
        return I18NConstants.CharacterEncoding[I18NConstants.ISO2022JP];
    }

    public String mimeBodyCharset ()
    {
        return I18NConstants.IANACharset[I18NConstants.ISO2022JP];
    }

    public String normalizeCatalog (String string)
    {
        return super.normalizeCatalog(halfToFullWidthKatakana(string));
    }

    public String normalizeDate (String string)
    {
        return convertWareki(super.normalizeDate(string));
    }

    public String readFileText (String string)
    {
        return readText(string);
    }

    public String writeFileText (String string, String encoding)
    {
       if (isMS932(encoding)) {
            return string;
        }
        return writeText(string);
    }

    public String normalizeMailText (String string)
    {
        return writeText(halfToFullWidthKatakana(string));
    }


    /*-----------------------------------------------------------------------
        Protected Methods
      -----------------------------------------------------------------------*/

      // Normalize a JIS208 string to a MS932 string.
    protected String readText (String string)
    {
        if (StringUtil.nullOrEmptyString(string)) {
            return string;
        }

        char[] arr = string.toCharArray();
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            arr[i] = convertJIS208StandardCharToMS932SpecialChar(arr[i]);
        }
        return new String(arr);
    }

      // Normalize a MS932 string to a JIS208 string.
    protected String writeText (String string)
    {
        if (StringUtil.nullOrEmptyString(string)) {
            return string;
        }

        char[] arr = string.toCharArray();
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            arr[i] = convertMS932SpecialCharToJIS208StandardChar(arr[i]);
        }
        return new String(arr);
    }


    /*-----------------------------------------------------------------------
        Private Methods -- emperor date
      -----------------------------------------------------------------------*/

    private static List ctab;
    static {
         ctab = ListUtil.list();
           // heisei
         ctab.add(new WarekiMap('\u5e73', '\u6210', 1988, 100));
           //shouwa
         ctab.add(new WarekiMap('\u662d', '\u548c', 1925, 64));
           //taisho
         ctab.add(new WarekiMap('\u5927', '\u6b63', 1911, 15));
           //meiji
         ctab.add(new WarekiMap('\u660e', '\u6cbb', 1867, 45));
    }

    private static String convertWareki (String str)
    {
        FastStringBuffer sb = new FastStringBuffer();
        int slen = str.length();
        for (int cidx = 0 ; cidx < slen ; ) {
            for (Iterator e = ctab.iterator() ; e.hasNext() ; ) {
                WarekiMap cm = (WarekiMap)e.next();
                if (str.charAt(cidx) == cm.c1()) {
                    if (cidx == (slen - 1)) {
                        sb.append(str.charAt(cidx));
                        cidx++;
                        break;
                    }
                    else if (str.charAt(cidx + 1) == cm.c2()) {
                        if (cidx == slen - 2) {
                            sb.append(str.charAt(cidx));
                            sb.append(str.charAt(cidx+2));
                            cidx += 2;
                            break;
                        }
                        else {
                            int year = 0;
                            int yidx = cidx + 2;
                            while (yidx < slen) {
                                if (Character.isDigit(str.charAt(yidx))) {
                                    year = year * 10 +
                                           Character.digit(str.charAt(yidx), 10);
                                    yidx++;
                                }
                                else if (str.charAt(yidx) == '\u5143') {
                                    //gann-Nen
                                    if (yidx == cidx + 2) {
                                        year = 1;
                                        yidx++;
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                                else {
                                    break;
                                }
                            }
                            if (yidx == cidx + 2) {
                                sb.append(str.charAt(cidx));
                                sb.append(str.charAt(cidx+1));
                                cidx += 2;
                                break;
                            }
                            else if (year == 0) {
                                while (cidx < yidx) {
                                    sb.append(str.charAt(cidx));
                                    cidx++;
                                }
                                break;
                            }
                            else {
                                if (str.charAt(yidx) == '\u5e74') {
                                    if (year > cm.years()) {
                                        while (cidx < yidx) {
                                            sb.append(str.charAt(cidx));
                                            cidx++;
                                        }
                                        break;
                                    }
                                    else {
                                        int ryear = cm.startyear() + year;
                                        sb.append(Constants.getInteger(ryear).toString());
                                        cidx = yidx;
                                        break;
                                    }
                                }
                                else {
                                    while (cidx < yidx) {
                                        sb.append(str.charAt(cidx));
                                        cidx++;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        sb.append(str.charAt(cidx));
                        cidx++;
                        break;
                    }
                }
                else {
                    if (!e.hasNext()) {
                        sb.append(str.charAt(cidx));
                        cidx++;
                    }
                    continue;
                }
            }
        }
        return sb.toString();
    }

    /*-----------------------------------------------------------------------
        Private Methods -- text converter
      -----------------------------------------------------------------------*/

    /**
        @param c A JIS X 0208 character
        @return A Microsoft Code Page 932 character
    */
    private static char convertJIS208StandardCharToMS932SpecialChar (char c)
    {
        switch(c) {
          case '\u00A2':
            return '\uFFE0';
          case '\u00A3':
            return '\uFFE1';
          case '\u00AC':
            return '\uFFE2';
          case '\u2016':
            return '\u2225';
          case '\u2212':
            return '\uFF0D';
          case '\u301C':
            return '\uFF5E';
          default:
            return c;
        }
    }

    /**
        @param c A MS932 character
        @return A JIS X 0208 character
    */
    private static char convertMS932SpecialCharToJIS208StandardChar (char c)
    {
        switch(c) {
          case '\u2225':
            return '\u2016';
          case '\uFF0D':
            return '\u2212';
          case '\uFF5E':
            return '\u301C';
          case '\uFFE0':
            return '\u00A2';
          case '\uFFE1':
            return '\u00A3';
          case '\uFFE2':
            return '\u00AC';
          default:
            return c;
        }
    }

    /**
        Check specified encoding is a "MS932" encoding name or not.
        If specified string is "MS932" or a "MS932" alias, returns true.
        If not,returns false.
        This method strongly depends on JVM implementation.

        @param encoding An encoding name("Shift_JIS", "UTF8" etc)
        @return If equals "MS932" or MS932 aliases, returns ture.
                If not, returns false.
    */
    private final static boolean isMS932 (String encode)
    {
        if (encode.equalsIgnoreCase("shift_jis")    ||
            encode.equalsIgnoreCase("shift-jis")    ||
            encode.equalsIgnoreCase("x-sjis")       ||
            encode.equalsIgnoreCase("x_sjis")       ||
            encode.equalsIgnoreCase("ms932")        ||
            encode.equalsIgnoreCase("ms_kanji")     ||
            encode.equalsIgnoreCase("csshiftjis")   ||
            encode.equalsIgnoreCase("cswindows31j") ||
            encode.equalsIgnoreCase("windows-31j"))
        {
            return true;
        }
        return false;
    }

    /**
        convert a HANKAKU KATAKANA string to a ZENKAKU KATAKANA string.
        non-HANKAKU KATAKANA charactors have no influence.
        @param sourceString a string includes HANKAKU KATAKANA(s)
        @return a string includes ZENKAKU KATAKANA(s)
    */
    private static String halfToFullWidthKatakana (String string)
    {
        int ixIn = 0;
        int ixOut = 0;
        int bufferLength = string.length();
        char[] input = string.toCharArray();
        char[] output = new char[bufferLength + 1];

        while (ixIn < bufferLength) {
            if (input[ixIn] >= '\uff61' && input[ixIn] <= '\uff9f') {
                if (ixIn + 1 >= bufferLength) {
                    output[ixOut++] = FWKatakana[input[ixIn++] - '\uff61'];
                }
                else {
                    if (input[ixIn + 1] == '\uff9e' ||
                        input[ixIn + 1] == '\u3099' ||
                        input[ixIn + 1] == '\u309b')
                    {
                        if (input[ixIn] == '\uff73') {
                            output[ixOut++] = '\u30f4';
                            ixIn += 2;
                        }
                        else if (input[ixIn] >= '\uff76' && input[ixIn] <= '\uff84' ||
                                 input[ixIn] >= '\uff8a' && input[ixIn] <= '\uff8e')
                        {
                            output[ixOut] = FWKatakana[input[ixIn] - '\uff61'];
                            output[ixOut++]++;
                            ixIn += 2;
                        }
                        else {
                            output[ixOut++] = FWKatakana[input[ixIn++] - '\uff61'];
                        }
                    }
                    else if (input[ixIn + 1] == '\uff9f' ||
                             input[ixIn + 1] == '\u309a' ||
                             input[ixIn + 1] == '\u309c')
                    {
                        if (input[ixIn] >= '\uff8a' && input[ixIn] <= '\uff8e') {
                            output[ixOut] = FWKatakana[input[ixIn] - '\uff61'];
                            output[ixOut++] += 2;
                            ixIn += 2;
                        }
                        else {
                            output[ixOut++] = FWKatakana[input[ixIn++] - '\uff61'];
                        }
                    }
                    else {
                        output[ixOut++] = FWKatakana[input[ixIn++] - '\uff61'];
                    }
                }
            }
            else {
                output[ixOut++] = input[ixIn++];
            }
        }

        String strOutput = new String(output);
        return strOutput.substring(0, ixOut);
    }


    /**
        If the argument is a character can be converted to a DAKUON KANA,
        this function returns true.
        If not, returns false.
        @param target  a hankaku katakana character.
        @return true or false.
    */
    private static boolean isFullWidthVoicedSoundMarkAvailable (char target)
    {
        boolean result = false;
            // all semi-voiced mark available KANAs are also voiced mark available.
        if (isFullWidthSemiVoicedSoundMarkAvailable(target)) {
            result = true;
        }
        else {
            switch(target) {
              case '\u304B':  /* HIRAGANA LETTER KA */
              case '\u304D':  /* HIRAGANA LETTER KI */
              case '\u304F':  /* HIRAGANA LETTER KU */
              case '\u3051':  /* HIRAGANA LETTER KE */
              case '\u3053':  /* HIRAGANA LETTER KO */
              case '\u3055':  /* HIRAGANA LETTER SA */
              case '\u3057':  /* HIRAGANA LETTER SI */
              case '\u3059':  /* HIRAGANA LETTER SU */
              case '\u305B':  /* HIRAGANA LETTER SE */
              case '\u305D':  /* HIRAGANA LETTER SO */
              case '\u305F':  /* HIRAGANA LETTER TA */
              case '\u3061':  /* HIRAGANA LETTER TI */
              case '\u3064':  /* HIRAGANA LETTER TU */
              case '\u3066':  /* HIRAGANA LETTER TE */
              case '\u3068':  /* HIRAGANA LETTER TO */
              case '\u30AB':  /* KATAKANA LETTER KA */
              case '\u30AD':  /* KATAKANA LETTER KI */
              case '\u30AF':  /* KATAKANA LETTER KU */
              case '\u30B1':  /* KATAKANA LETTER KE */
              case '\u30B3':  /* KATAKANA LETTER KO */
              case '\u30B5':  /* KATAKANA LETTER SA */
              case '\u30B7':  /* KATAKANA LETTER SI */
              case '\u30B9':  /* KATAKANA LETTER SU */
              case '\u30BB':  /* KATAKANA LETTER SE */
              case '\u30BD':  /* KATAKANA LETTER SO */
              case '\u30BF':  /* KATAKANA LETTER TA */
              case '\u30C1':  /* KATAKANA LETTER TI */
              case '\u30C4':  /* KATAKANA LETTER TU */
              case '\u30C6':  /* KATAKANA LETTER TE */
              case '\u30C8':  /* KATAKANA LETTER TO */
                result = true;
                break;
              default:
                break;
            }
        }
        return result;
    }

    /**
        If the argument is a character can be converted to a HAN-DAKUON KANA,
        this function returns true.
        If not, returns false.
        @param target  a hankaku katakana character.
        @return true or false.
    */
    private static boolean isFullWidthSemiVoicedSoundMarkAvailable (char target)
    {
        boolean result = false;
        switch(target) {
          case '\u306F':  /* HIRAGANA LETTER HA */
          case '\u3072':  /* HIRAGANA LETTER HI */
          case '\u3075':  /* HIRAGANA LETTER HU */
          case '\u3078':  /* HIRAGANA LETTER HE */
          case '\u307B':  /* HIRAGANA LETTER HO */
          case '\u30CF':  /* KATAKANA LETTER HA */
          case '\u30D2':  /* KATAKANA LETTER HI */
          case '\u30D5':  /* KATAKANA LETTER HU */
          case '\u30D8':  /* KATAKANA LETTER HE */
          case '\u30DB':  /* KATAKANA LETTER HO */
            result = true;
            break;
          default:
            break;
        }
        return result;
    }

}

class WarekiMap {
    char c1;
    char c2;
    int startyear;
    int years;

    public WarekiMap (char c1, char c2, int startyear, int years)
    {
        this.c1 = c1;
        this.c2 = c2;
        this.startyear = startyear;
        this.years = years;
    }

    public int startyear ()
    {
        return this.startyear;
    }

    public int years ()
    {
        return this.years;
    }

    public char c1 ()
    {
        return this.c1;
    }

    public char c2 ()
    {
        return this.c2;
    }
}
