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

    $Id: //ariba/platform/util/core/ariba/util/i18n/LocalizedStringInterface.java#1 $
*/

package ariba.util.i18n;

import java.util.Locale;

/**
    This is an extremely powerful concept, a universal source of multi-locale string data.
    There are several instances already of anonymous implementations of this interface,
    which are then used with LocaleUtil.getPopulatedMLS to create MultiLocaleStrings with
    a localized value in each of the realm's locales, using refactored code that
    previously just provided a value in a single locale.  It should be implemented by
    LocalizedJavaString, MultiLocaleStringSource, MultiLocaleString, and
    MultiLingualString.
*/
public interface LocalizedStringInterface
{
    /**
        Implementations of the single parameter getString should always just call
        getString with a second parameter of true, for useDefaulting.
    */
    public String getString (Locale locale);

    /**
        @return String value in given locale for localized/multiLocale/multiLingual
        string.  If translation in that locale is not available, then if useDefaulting is
        true, it will return a non-null value, with an ultimate default to "", if no other
        default is available.  If no translation in that locale is available and
        useDefaulting is false, it will simply return null.
    */
    public String getString (Locale locale, boolean useDefaulting);

    /**
        @return Locale which is the "source of truth" for this
        localized/multiLocale/multiLingual string.  This has also been known as the
        "OriginalLocale", and as the BaseLocale.  It is the source of truth for
        translating this string into other locales.  For a LocalizedJavaString, this will
        default to returning Locale.US, since the source of truth is the originalString in
        the Java code, which is often in American English.  For a MultiLocaleString it is
        the OriginalLocale, and for a MultiLingualString it is probably the default locale
        of the realm/partition.
    */
    public Locale getSourceOfTruthLocale ();

}
