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

    $Id: //ariba/platform/util/core/ariba/util/i18n/LocalizedJavaString.java#11 $
*/

package ariba.util.i18n;

import java.util.Locale;

/**
    Class LocalizedJavaString provides a way to strings in Java classes that should
    be localized.

    @aribaapi ariba
*/
public final class LocalizedJavaString implements LocalizedStringInterface
{
    /**
        A Localizer knows how to return a localized string given the key and class name.

        @aribaapi ariba
    */
    public static interface Localizer
    {
        /**
            @param className name of the class in which the string is created.
            @param key unique key for the string within that classes
            @param originalString original string
            @param locale locale for the string
            @return localized String. if localized string is not found, return the originalString

            @aribaapi ariba
        */
        public String getLocalizedString (String className,
                                          String key,
                                          String originalString,
                                          Locale locale);
    }

    /**
        A Localizer knows how to return a localized string given the key and class name.
        A LocalizerWithSourceOfTruthLocale also knows how to return the source of truth
        locale in which the original string was composed. 
        
        @aribaapi ariba
    */
    public static interface LocalizerWithSourceOfTruthLocale extends Localizer
    {
        /**
            @param className name of the class in which the string is created.
            @param key unique key for the string within that classes
            @return Locale which is the "source of truth" for this
            localized/multiLocale/multiLingual string.  This has also been known as the
            "OriginalLocale", and as the BaseLocale.  It is the source of truth for
            translating this string into other locales.  For a LocalizedJavaString, if the
            registered Localizer doesn't implement this extended interface, Locale.US will
            be used as the source of truth.  For a MultiLocaleString it is the
            OriginalLocale, and for a MultiLingualString it is probably the default locale
            of the realm/partition.
            @aribaapi ariba
        */
        public Locale getSourceOfTruthLocale (String className, String key);
    }

    private static Localizer _localizer;

    private String _keyString;
    private String _originalString;
    private String _className;

    /**
        @param className name of the class in which the string is created
        @param key integer key unique within the class
        @param originalString string in en_US

        @aribaapi ariba
    */
    public LocalizedJavaString (String className, int key, String originalString)
    {
        this(className, Integer.toString(key), originalString);
    }

    /**
        @param className name of the class in which the string is created
        @param keyString String key unique within the class
        @param originalString string in en_US

        @aribaapi ariba
    */
    public LocalizedJavaString (String className, String keyString, String originalString)
    {
        _keyString = keyString;
        _originalString = originalString;
        _className = className;
    }

    /**
        @param locale locale
        @return localized string

        @aribaapi ariba
    */
    public String getLocalizedString (Locale locale)
    {
        return _localizer == null ? _originalString :
                _localizer.getLocalizedString(_className, _keyString,
                                              _originalString, locale);
    }

    /**
        Returns the original string
        @return the string in en_US locale

        @aribaapi ariba
    */
    public String getOriginalString ()
    {
        return _originalString;
    }

    /**
        Returns the key associated with the original string
        @return String the key

        @aribaapi ariba
    */
    public String getKey ()
    {
        return _keyString;
    }

    /**
        Returns the classname associated with the original string
        @return the classname

        @aribaapi ariba
    */
    public String getClassName ()
    {
        return _className;
    }

    /**
        Implementations of the single parameter getString should always just call
        getString with a second parameter of true, for useDefaulting.
        Part of LocalizedStringSource interface.
    */
    public String getString (Locale locale)
    {
        return getString(locale, true);
    }

    /**
        @return String value in given locale for localized/multiLocale/multiLingual
        string.  If useDefaulting is false, we return null if the locale is not
        the source of truth, yet the value is the same as the source of truth value.
        Part of the LocalizedStringSource interface.
    */
    public String getString (Locale locale, boolean useDefaulting)
    {
        String result = getLocalizedString(locale);
        if (result != null && !useDefaulting && locale != null) {
            Locale defaultLocale = getSourceOfTruthLocale();
            if (!locale.equals(defaultLocale)) {
                String defaultResult = getLocalizedString(defaultLocale);
                if (result.equals(defaultResult)) {
                    result = null;
                }
            }
        }
        return result;
    }

    /**
        @return Locale which is the "source of truth" for this
        localized/multiLocale/multiLingual string.  This has also been known as the
        "OriginalLocale", and as the BaseLocale.  It is the source of truth for
        translating this string into other locales.  For a LocalizedJavaString, this will
        always return the Locale "en_US", since the source of truth is the strings is the
        originalString in the Java code, which is always in American English.  For a
        MultiLocaleString it is the OriginalLocale, and for a MultiLingualString it is
        probably the default locale of the realm/partition.  Part of LocalizedStringSource
        interface.
    */
    public Locale getSourceOfTruthLocale ()
    {
        Locale truthLocale = Locale.US;
        if (_localizer instanceof LocalizerWithSourceOfTruthLocale) {
            LocalizerWithSourceOfTruthLocale truthLocalizer =
                (LocalizerWithSourceOfTruthLocale)_localizer;
            truthLocale = truthLocalizer.getSourceOfTruthLocale(_className, _keyString);
        }
        return truthLocale;
    }

    /**
        Register an instance of Localizer.  This should be
        done at app startup time.  if this is invoked multiple times,
        the last one wins.

       @param localizer the localize to register.

        @aribaapi ariba
    */
    public static void registerLocalizer (Localizer localizer)
    {
        _localizer = localizer;
    }

    /**
        Static method to return an aribaweb-style localized package string for the given
        arguments, without needless allocations.

        @param className String full class path, with package name followed by dot and bare
        class name.
        @param key int secondary key within the class.
        @param originalString String value that appears as a literal in the source code call
        to localizedJavaStringHere, or equivalent.
        @param locale Locale for the string
        @return String localized. if localized string is not found, return the originalString.

        @aribaapi ariba
    */
    public static String getLocalizedString (String className, int key,
                                             String originalString, Locale locale)
    {
        String keyString = Integer.toString(key);
        return getLocalizedString(className, keyString, originalString, locale);
    }

    /**
        Static method to return an aribaweb-style localized package string for the given
        arguments, without needless allocations.

        @param className String full class path, with package name followed by dot and bare
        class name.
        @param keyString String secondary key within the class.
        @param originalString String value that appears as a literal in the source code call
        to localizedJavaStringHere, or equivalent.
        @param locale Locale for the string
        @return String localized. if localized string is not found, return the originalString.

        @aribaapi ariba
    */
    public static String getLocalizedString (String className, String keyString,
                                             String originalString, Locale locale)
    {
        return _localizer == null ? originalString :
            _localizer.getLocalizedString(className, keyString, originalString, locale);
    }

}
