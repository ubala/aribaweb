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

import ariba.util.core.ClassUtil;
import ariba.util.core.EncodingMap;
import ariba.util.core.MapUtil;
import ariba.util.core.Fmt;
import java.util.Map;
import ariba.util.core.StringUtil;
import java.util.Locale;

/**
    Class LocaleSupport provides functions for I18N, include getting encoding string,
    normalize text, etc. All function calls are delegated to instance of the specified
    language support classes.
    
    @aribaapi documented
*/

public final class LocaleSupport
{
    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

    private static final String packageName   = "ariba.util.i18n";
    private static final String classNamePrefix = "Support";
        
    /*-----------------------------------------------------------------------
        Private Fields
      -----------------------------------------------------------------------*/

        // instance of I18NSupport as the last resort
    private static I18NSupport defaultSupport = null;
    
        // Map of I18NSupport instances
    private static Map supportLibrary = null;

        // initialize class members
    static {
        supportLibrary = MapUtil.map();
        defaultSupport = new I18NSupport();
    }
     
    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
       Class constructor.
    */
    public LocaleSupport ()
    {
    }

    /*-----------------------------------------------------------------------
        Character Encoding related functions
      -----------------------------------------------------------------------*/
    
    /**
        Returns the UI encoding string for the specified locale

        @param locale the locale to get the encoding string of
        @return the encoding string
        @aribaapi documented
    */
    public static String uiEncoding (Locale locale)
    {
        String encoding = EncodingMap.getEncodingMap().getUIEncoding(locale);
        if (encoding == null) {
            encoding = getI18NSupport(locale).uiEncoding();
        }
        return encoding;
    }

        // This function is needed by AW to set the default.
        // Default UI Encoding is set by CCE in Buyer and null for other products
    public static String defaultUIEncoding ()
    {
        return EncodingMap.getEncodingMap().getDefaultUIEncoding();
    }
    
    /**
        Returns the file encoding string for the specified locale

        @param locale the locale to get the encoding string of
        @return the encoding string
        @aribaapi documented    
    */    
    public static String fileEncoding (Locale locale)
    {
        return getI18NSupport(locale).fileEncoding();
    }
            
    /**
        Returns the MIME header encoding string for the specified locale

        @param locale the locale to get the encoding string of
        @return the encoding string
        @aribaapi documented    
    */        
    public static String mimeHeaderEncoding (Locale locale)
    {
        return getI18NSupport(locale).mimeHeaderEncoding();
    }

    /**
        Returns the MIME body encoding string for the specified locale

        @param locale the locale to get the encoding string of
        @return the encoding string
        @aribaapi documented    
    */            
    public static String mimeBodyEncoding (Locale locale)
    {
        String encoding = EncodingMap.getEncodingMap().getEmailEncoding(locale);
        if (encoding == null) {
            encoding = getI18NSupport(locale).mimeBodyEncoding();
        }
        return encoding;
    }
    
    /**
        Returns the MIME body charset string for the specified locale

        @param locale the locale to get the encoding string of
        @return the encoding string
        @aribaapi documented    
    */            
    public static String mimeBodyCharset (Locale locale)
    {
        String encoding = EncodingMap.getEncodingMap().getEmailEncoding(locale);
        if (encoding == null) {
            encoding = getI18NSupport(locale).mimeBodyCharset();
        }
        return encoding;
    }

    /**
        Returns the MIME HTML attachment encoding string for the specified locale

        @param locale the locale to get the encoding string of
        @return the encoding string
        @aribaapi documented    
    */            
    public static String mimeAttachmentEncoding (Locale locale)
    {
        String encoding = EncodingMap.getEncodingMap().getHTMLEncoding(locale);
        if (encoding == null) {
            encoding = uiEncoding(locale);
        }
        return encoding;
    }

    /**
        Returns the encoding string for the specified locale to be used
        for Strings that need to be encoded using the client machine's encoding

        @param locale the locale to get the encoding string of
        @return the encoding string
        @aribaapi documented    
    */            
    public static String clientSideEncoding (Locale locale)
    {
        return EncodingMap.getEncodingMap().getClientSideEncoding(locale);
    }

    
    /*-----------------------------------------------------------------------
        String related functions
      -----------------------------------------------------------------------*/
    
    /**
        Normalizes characters in the catalog string for the specified locale

        @param string the string to be normalized
        @param locale the locale to normalze the string for
        @return the normalized string
        @aribaapi documented    
    */            
    public static String normalizeCatalog (String string, Locale locale)
    {
        return getI18NSupport(locale).normalizeCatalog(string);
    }

    /**
        Normalizes the input date format string for the specified locale

        @param string the string to be normalized
        @param locale the locale to normalze the string for
        @return the normalized string
        @aribaapi documented    
    */                
    public static String normalizeDate (String string, Locale locale)
    {
        return getI18NSupport(locale).normalizeDate(string);
    }

    /**
        Normalizes the input decimal string for the specified locale

        @param string the string to be normalized
        @param locale the locale to normalze the string for
        @return the normalized string
        @aribaapi documented    
    */                    
    public static String normalizeNumber (String string, Locale locale)
    {
        return getI18NSupport(locale).normalizeNumber(string);
    }
    
    /**
        Normalizes the input money format string for the specified locale

        @param string the string to be normalized
        @param locale the locale to normalze the string for
        @return the normalized string
        @aribaapi documented    
    */                
    public static String normalizeMoney (String string, Locale locale)
    {
        return getI18NSupport(locale).normalizeMoney(string);
    }

    /**
        Normalizes the input read text string for the specified locale

        @param string the string to be normalized
        @param locale the locale to normalze the string for
        @return the normalized string
        @aribaapi documented    
    */                
    public static String readFileText (String string, Locale locale)
    {
        return getI18NSupport(locale).readFileText(string);
    }
    
    /**
        Normalizes the input write text string for the specified locale

        @param string the string to be normalized
        @param encoding the encoding to write the file text
        @param locale the locale to normalze the string for
        @return the normalized string
        @aribaapi documented    
    */                
    public static String writeFileText (String string, String encoding, Locale locale)
    {
        return getI18NSupport(locale).writeFileText(string, encoding);
    }

    /**
        Normalizes the input mail text string for the specified locale

        @param string the string to be normalized
        @param locale the locale to normalze the string for
        @return the normalized string
        @aribaapi documented    
    */                    
    public static String normalizeMailText (String string, Locale locale)
    {
        return getI18NSupport(locale).normalizeMailText(string);
    }
    
    /**
        Returns the number of bytes of the input string for the specified locale
        using its default encoding

        @param string the string to get the number of bytes
        @param locale the locale whose default encoding is to be used
        @return the width of the string
        @aribaapi documented    
    */                        
    public static int widthInBytes (String string, Locale locale)
    {
        return getI18NSupport(locale).widthInBytes(string);
    }
    
    /**
        Returns the width of the input string for the specified locale

        @param string the string to get the width of
        @param locale the locale for calculating width
        @return the width of the string
        @aribaapi documented    
    */                        
    public static int getStringWidth (String string, Locale locale)
    {
        return getI18NSupport(locale).getStringWidth(string);
    }
    

    /**
        Returns the decomposition flag for the specified locale

        @param locale the locale to get the decomposition flag for
        @return the decomposition flag
        @aribaapi documented    
    */                  
    public static int decompositionFlag (Locale locale)
    {
        return getI18NSupport(locale).decompositionFlag();
    }

    
    /**
        Check if the specified locale is supported by EM

        @param locale the locale to be checked
        @return <b>true</b> if supported, <b>false</b> otherwise.
        @aribaapi private
    */                  
    public static boolean isSupportedInEM (Locale locale)
    {
        return getI18NSupport(locale).isSupportedInEM();
    }

    
    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/
    
    /*
        Returns an instance of I18NSupport for the given locale
    */
    private static I18NSupport getI18NSupport (Locale locale)
    {
        if (locale == null) {
            return getDefaultSupport();
        }

        I18NSupport i18nsupport = getSupportFromLibrary(locale);
        
        if (i18nsupport == null) {
            i18nsupport = newI18NSupport(locale);
        }
        
        return i18nsupport;
    }

    /*
      Returns the support instance from the library
    */
    private static I18NSupport getSupportFromLibrary (Locale locale)
    {
        Object obj = supportLibrary.get(locale);

        if (obj instanceof String) {
            obj = getDefaultSupport();
        }
        
        return (I18NSupport)obj;
    }

    /*
        Returns the new instance of I18NSuppor for the specified locale
    */
    private static I18NSupport newI18NSupport (Locale locale)
    {
        String className1 = null;
        String className2 = null;
        String className3 = null;
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        
        className3 = Fmt.S("%s.%s_%s", 
                           packageName, classNamePrefix, language);
        if (!StringUtil.nullOrEmptyString(country)) {
            className2 = Fmt.S("%s.%s_%s_%s", 
                               packageName, classNamePrefix, language, country);
            if (!StringUtil.nullOrEmptyString(variant)) {
                className1 = Fmt.S("%s.%s_%s_%s_%s", 
                                   packageName, classNamePrefix, 
                                   language, country, variant);
            }
        }
            
        Object obj = null;
        I18NSupport i18nsupport = null;

        synchronized (supportLibrary) {
            i18nsupport = getSupportFromLibrary(locale);
            if (i18nsupport == null) {
                if (className1 != null) {
                    obj = ClassUtil.newInstance(className1, false);
                }
                if (obj == null && className2 != null) {
                    obj = ClassUtil.newInstance(className2, false);
                }
                if (obj == null && className3 != null) {
                    obj = ClassUtil.newInstance(className3, false);
                }
                
                if (obj == null) {
                    i18nsupport = getDefaultSupport();
                    supportLibrary.put(locale, "");
                }
                else {
                    i18nsupport = (I18NSupport)obj;
                    supportLibrary.put(locale, i18nsupport);
                }
            }
        }
        
        return i18nsupport;
    }
    
    /*
        Returns the default I18NSupport instance
    */
    private static I18NSupport getDefaultSupport ()
    {
        return defaultSupport;
    }

}
