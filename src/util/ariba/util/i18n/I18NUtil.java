/*
    Copyright (c) 2013-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/i18n/I18NUtil.java#19 $
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/i18n/I18NUtil.java#19 $
*/

package ariba.util.i18n;

import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Fmt;
import ariba.util.log.Log;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.StringTokenizer;

/**
    Class I18NUtil provides I18N related constants and utility functions

    @aribaapi documented
*/

public class I18NUtil extends I18NEncodingUtil
{
    /*-----------------------------------------------------------------------
        Public Constants
      -----------------------------------------------------------------------*/

    /**
        Languages that are not defined in java.util.Locale

        @aribaapi private
    */
    public static final Locale Hebrew = new Locale ("he","","");
    public static final Locale Arabic = new Locale ("ar","","");

    /**
        HTML direction parameter value

        @aribaapi private
    */
    public static final String LeftToRight = "LTR";
    public static final String RightToLeft = "RTL";
    public static final String Right       = "right";
    public static final String Left        = "left";

    /**
        We use the standard UTF8 encoding for XML files as well as
        doing CGI posts to our own server.

        @aribaapi private
    */
    public static final String EncodingUTF8 = "UTF8";

    /**
        We use the non-standard Shift_JIS encoding for the japanese HTML client.
        Because it is super set of standard SJIS.

        @aribaapi private
    */
    public static final String EncodingShiftJIS = "Shift_JIS";
    
    /**
        We use GB2312 encoding for the korean HTML client.

        @aribaapi private
    */
    public static final String EncodingKorean = "KSC5601";
    
    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
       Class constructor.

       @aribaapi private
    */
    public I18NUtil ()
    {
    }


    /**
        Returns true if locale is Latin-1:
        English, Danish, Dutch, French, German, Finnish, Icelandic, Irish,
        Italian, Spanish, Norwegian, Brazilian, Portuguese. The Locale language
        for Icelanic and Irish is unknown, need to update later.

        @param locale the locale to decide on
        @return boolean
        @aribaapi private
    */
    public static boolean isLatin1 (Locale locale)
    {
        String language = locale.getLanguage();
        return language.equalsIgnoreCase(Locale.ENGLISH.getLanguage()) ||
               language.equalsIgnoreCase("da") ||
               language.equalsIgnoreCase("nl") ||
               language.equalsIgnoreCase(Locale.FRENCH.getLanguage()) ||
               language.equalsIgnoreCase(Locale.GERMAN.getLanguage()) ||
               language.equalsIgnoreCase("fi") ||
               language.equalsIgnoreCase("icelandic") ||
               language.equalsIgnoreCase("irish") ||
               language.equalsIgnoreCase(Locale.ITALIAN.getLanguage()) ||
               language.equalsIgnoreCase("es") ||
               language.equalsIgnoreCase("no") ||
               language.equalsIgnoreCase("pt");
    }


    /**
        Returns true if locale is Chinese/Japanese/Korean

        @param locale the locale to decide on
        @return boolean
        @aribaapi private
    */
    public static boolean isCJK (Locale locale)
    {
        return (locale.getLanguage().equals(Locale.CHINESE.getLanguage()) ||
                locale.getLanguage().equals(Locale.JAPANESE.getLanguage()) ||
                locale.getLanguage().equals(Locale.KOREAN.getLanguage()));
    }


    /**
        Returns true if locale is Hebrew/Arabic

        @param locale the locale to get the encoding string of
        @return boolean
        @aribaapi private
    */
    public static boolean isBidirectional (Locale locale)
    {
        if (locale == null || locale.getLanguage() == null) {
            return false;
        }

        return locale.getLanguage().equals(Hebrew.getLanguage()) ||
               locale.getLanguage().equals(Arabic.getLanguage());
    }

    /**
        Returns value for HTML direction attribute according to the locale

        @param locale the locale to get the encoding string of
        @return String [RTL|LTR]
        @aribaapi private
    */
    public static String languageDirection (Locale locale)
    {
        return isBidirectional(locale) ? RightToLeft : LeftToRight;
    }

    /**
        Returns value for HTML direction attribute according to the locale

        @param locale the locale to get the encoding string of
        @return String [Right|Left]
        @aribaapi private
    */
    public static String languageLeft (Locale locale)
    {
        return isBidirectional(locale) ? Right : Left;
    }


    /**
        Returns value for HTML direction attribute according to the locale

        @param locale the locale to get the encoding string of
        @return String [Right|Left]
        @aribaapi private
    */
    public static String languageRight (Locale locale)
    {
        return isBidirectional(locale) ? Left : Right;
    }

    /**
        call this to get the correct CHARSET string to embed in MIME, HTML, or
        XML. These are the preferred names as specified in the IANA registry which
        is referrenced by these standards.

        For example:
            mime charset
                Content-Type: content=text/html charset="CHARSET"
            encoded words in header
                =?CHARSET?encoding?text?=
            html
                <meta http-equiv= "Content-Type" content=text/html charset="CHARSET">
            xml
                <?xml encoding = 'CHARSET'?>

        @param encoding the encoding string in java form
        @return the encoding string in IANA form
        @aribaapi private
    */
    public static String javaEncodingToIANACharset (String encoding)
    {
        if (encoding == null) {
            return null;
        }

        int count = I18NConstants.count();

        for (int i = 0; i < count; i++) {
            if (encoding.equalsIgnoreCase(I18NConstants.CharacterEncoding[i])) {
                return I18NConstants.IANACharset[i];
            }
        }

            // error log here, either invalid encoding or something wrong with I18NConstants
        return encoding;
    }

    /**
        Returns either B or Q mime encoding for the given charset
            B encoding is BASE64.
            Q is almost identical to quoted printable. That is, ASCII
            except for certain characters such as space the have to be escaped.
            For the full descrition, see rfc.2047

            All text can be represented as B.
            Q is somewhat more legible for Latin scripts since it preserves ASCII

        @param charset the charset string
        @return B or Q mime encoding
        @aribaapi private
    */
    public static String mimeEncoding (String charset)
    {
        if (charset.equalsIgnoreCase(
            I18NConstants.IANACharset[I18NConstants.ISO8859_1]) ||
            charset.equalsIgnoreCase(
                I18NConstants.IANACharset[I18NConstants.ISO8859_2]))
        {
            return I18NConstants.EncodingQ;
        }
        else {
            return I18NConstants.EncodingB;
        }
    }



    /*-- Locale Utilities ---------------------------------------------------*/

    /**
        @aribaapi private
    */
    private static final GrowOnlyHashtable LocaleCache =
        new GrowOnlyHashtable();

        // populate with a few constants so == will work for some
        // caches
    static {
        LocaleCache.put("en_US", Locale.US);
        LocaleCache.put("en", Locale.ENGLISH);
    }

    /**
        Create a Locale object from given language, country, variant

        @param language locale language
        @param country  locale country
        @return a new Locale object for the given language & country strings.
        @aribaapi private
    */
    public static Locale getLocale (String language, String country)
    {
        return getLocale(language, country, "");
    }

    /**
        Create a Locale object from given language, country, variant

        @param language locale language
        @param country  locale country
        @param localeVariant  locale variant
        @return a new Locale object for the given <B>language</B>, <B>country</B>,
            and <B>variant</B> strings.
        @aribaapi private
    */
    public static Locale getLocale (String language, String country, String localeVariant)
    {
        if ("".equals(localeVariant)) {
            if ("en".equals(language)) {
                if ("US".equals(country)) {
                    return Locale.US;
                }
                if ("".equals(country)) {
                    return Locale.ENGLISH;
                }
            }
        }
        return new Locale(language, country, localeVariant);
    }


    private static String LocaleDelimiter = "_";

    /**
        Create a Locale object for the locale specified by a String.

        @param locale  Locale description string in the form of
        Locale.toString() (e.g. "en_US").

        @return a new Locale object for the given <B>locale</B> string.
        @aribaapi documented
    */
    public static Locale getLocaleFromString (String locale)
    {
            // check if Locale already in hashtable
        Object cachedLocale = LocaleCache.get(locale);
        if (cachedLocale != null) {
            return (Locale)cachedLocale;
        }

        /* Don't create tokenizer unless we miss in the cache above. */
        String[] arr = {"","",""};
        StringTokenizer tokenizer = new StringTokenizer(locale, LocaleDelimiter, true);

        int idx = 0;
        boolean prev = false;
        while (tokenizer.hasMoreTokens() && idx < 3) {
            String token = tokenizer.nextToken();
            if (token.equals(LocaleDelimiter)) {
                if (prev || idx == 0) {
                    idx++;
                }
                prev = true;
                continue;
            }

            arr[idx++] = token;
            prev = false;
        }

        Locale newLocale = getLocale(arr[0], arr[1], arr[2]);
        LocaleCache.put(locale, newLocale);
        return (newLocale);
    }

    /**
        Returns the parent <code>Locale</code> of <code>locale</code>. <p/>

        By parent we mean the <code>Locale</code> that logically contains
        the passed-in <code>Locale</code> (where <code>language</code> is
        deemed to contain <code>country</code> which is deemed to contain
        <code>variant</code>. <p/>

        Thus 'en' is the parent of 'en_US' and 'fr_FR' is the parent of 'fr_FR_var'.
        Top-level Locales (those that specify languages only) do not
        have a parent and <code>null</code> is returned. <p/>

        @param locale the <code>Locale</code>, whose parent we wish to find; may not
               be <code>null</code>
        @return the parent <code>Locale</code> or <code>null</code> if there is no
                parent
        @aribaapi ariba
    */
    public static Locale getParent (Locale locale)
    {
        String localeName = null;
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        if (variant.length() > 0) {
            localeName = Fmt.S("%s_%s", language, country);
        }
        else if (country.length() > 0) {
            localeName = language;
        }
        else if (language.length() > 0) {

                // cannot relax further
            return null;
        }
        return localeName == null ? null : getLocaleFromString(localeName);
    }


    /*-----------------------------------------------------------------------
        Netscape bugs workaround
      -----------------------------------------------------------------------*/

    /**
        Returns boolean on whether to enable Tooltips

        @param isBrowserNetscape someone's still using Netscape?
        @param encodingString the HTML encoding
        @param locale the user's Locale
        @return enable Tooltips?
        @aribaapi private
    */
    public static boolean enableTooltips (boolean isBrowserNetscape,
                                          String encodingString,
                                          Locale locale)
    {
        return !isBrowserNetscape ||
               !I18NUtil.isCJK(locale) ||
            !encodingString.equalsIgnoreCase(
                I18NConstants.IANACharset[I18NConstants.UTF8]);
    }

    /**
        Calculate string width in ASCII character when printed.
        8859_1 chacaters occupies same width when printed.
        Japanese Kanji-charcter occupies twice of ASCII character's width.
        Currentry used in BaseObjectOnServer. and TextUtil.

        @param string printed string
        @param encoding string encoding

        @return printed width of string

        @aribaapi private
        @deprecated see LocaleSupport.widthInBytes(String, Locale)
    */
    public static int stringWidthWhenPrinted (String string, String encoding)
    {
        if (isValidEncoding(encoding)) {
            return stringLengthBytes(string, encoding);
        }
        Log.util.debug("%s is not a supported encoding for calculating string width",
                       encoding);
        return string.length();
    }

    /**
        Calculate byte count of String in specified encoding.

        @param string string
        @param encoding encoding

        @return byte count of string when string is converted with specified encoding.
        @aribaapi private
        @deprecated see LocaleSupport.widthInBytes(String, Locale)
    */
    public static int stringLengthBytes (String string, String encoding)
    {
        try {
            return string.getBytes(encoding).length;
        }
        catch (UnsupportedEncodingException e) {
            Log.util.debug("%s is not a supported encoding", encoding);
            return string.length();
        }
    }
}
