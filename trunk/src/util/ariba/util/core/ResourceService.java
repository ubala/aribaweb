/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/ResourceService.java#60 $
*/

package ariba.util.core;

import ariba.util.core.FileReplacer.BadStateException;
import ariba.util.i18n.I18NUtil;
import ariba.util.io.CSVReader;
import ariba.util.io.DeserializationException;
import ariba.util.io.Deserializer;
import ariba.util.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
    The fundamental API to resolving localized resources.  Subclassers should
    implement the get* primitives based on specific resource resolution
    strategies.

    @aribaapi documented
*/
public class ResourceService
{
    /*-----------------------------------------------------------------------
        Nested interface
      -----------------------------------------------------------------------*/

    /**
        @aribaapi ariba
    */
    public static interface Localizer
    {
        /**
            Returns the string associated with the given
            <code>compositeKey</code> in the given <code>locale</code>.
            A composite key is a string that has the following form: <ul>
            <li> <code>@&lt;stringtable&gt;/&lt;stringkey&gt;</code>
            </ul>
            If <code>compostiteKey</code> is not in the above form then
            it is simply returned. If <code>compositeKey</code> is in the above
            form then the value for <code>stringkey</code> in the
            <code>stringtable</code> and <code>locale</code> is returned. If there
            is no value <code>stringkey</code> is returned. <p/>

            @param compositeKey string to localize
            @param locale locale
            @return localized composite key
            @aribaapi private
        */
        public String getLocalizedCompositeKey (String compositeKey, Locale locale);
    }

    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

    /**
        Key for the character set for the .table string file format.

        @aribaapi private
    */
    private static final String CharSetKey = "CharSet";


    /*-----------------------------------------------------------------------
        Protected Constants
      -----------------------------------------------------------------------*/

    /**
        The subdirectory that resources are located under.

        @aribaapi private
    */
    protected static final String ResourceRoot = "resource";

    /**
        Image directory.  Value is "images".

        @aribaapi private
    */
    protected static final String ImagesDirectory = "images";

    /**
        Strings directory.  Value is "strings".

        @aribaapi private
    */
    protected static final String StringsDirectory = "strings";

    /**
        Help directory.  Value is "help".

        @aribaapi private
    */
    protected static final String HelpDirectory = "help";

    /**
        Prefix for resource keys.

        @aribaapi private
    */
    public static final String NlsTag          = "@";
    public static final String EmbeddedSubstitutionsFlag          = "@@";

    /**
        Reports Template directory.  Value is "reporttemplates".

        @aribaapi private
    */
    public static final String ReportTemplatesDirectory = "reporttemplates";

    /*-----------------------------------------------------------------------
        Public Constants
      -----------------------------------------------------------------------*/

    /**
        Locale of last resort.  Value is "en", "US", "".

        @aribaapi private
    */
    public static final Locale LocaleOfLastResort = Locale.US;

    /**
        Relaxed locale of last resort.  Value is "en", "", "".

        @aribaapi private
    */
    protected static final Locale RelaxedLocaleOfLastResort =
        Locale.ENGLISH;

    /*-----------------------------------------------------------------------
        Private Fields
      -----------------------------------------------------------------------*/

    /**
        Note: The DefaultImagesRoot itself should be pre-pended by a
        base root equivalent to Application.codeBase() (ex:
        Applet.getCodeBase()) to get the absolute path.

        @aribaapi private
    */
    private String defaultImagesRoot;

    /**
        The URL of the root for string resources
        @aribaapi private
    */
    protected URL stringBaseURL;

    /**
        The base URL for resolving resources

        @aribaapi private
    */
    public URL baseURL;


    /**
        Anyone who instantiates a ResourceService must specify a "locale of
        next-to-last-resort" which indicates the second-to-last directory in
        which to look for a match when resolving a given resource key.

        The very last place the Resource Manager looks for a match is the
        "locale of last resort", which is always en_US.

        @aribaapi private
    */
    private Locale defaultLocale;

    /**
        This is to add a backdoor to ResourceService to allow docs and QA folks
        to force the HTML UI to render in a certain locale.

        @aribaapi private
    */
    private Locale overrideLocale = Locale.US;

    /**
        This is to add a backdoor to ResourceService to allow docs and QA folks
        to force the HTML UI to render in a certain locale.

        @aribaapi private
    */
    private boolean overrideLocaleOn;

    /**
        This is another backdoor for Inspector, updated by
        LocaleID. It is the list of String UniqueNames

        @aribaapi private
    */
    private List locales = ListUtil.list(LocaleOfLastResort.toString());


    /**
        mapping of the string caches with their <code>StringTableProcessor</code>.
        @aribaapi private
    */
    protected final Map stringTableProcessors = MapUtil.map();

    /**
        default string table processor. this processor read the 3 column strings csv files.
        @aribaapi private
    */
    protected final StringTableProcessor defaultStringTableProcessor;

    /**
        cache of image maps by locale

        @aribaapi private
    */
    protected final Map imageMaps = MapUtil.map();

    /**
        Cache of help directories by locale

        @aribaapi private
    */
    private final Map helpDirs = MapUtil.map();


    /**
        Cache of report directories by locale

        @aribaapi private
    */
    private final Map reportTemplatesDirs = MapUtil.map();


    /*-----------------------------------------------------------------------
        Static Methods to set and get ResourceService object
      -----------------------------------------------------------------------*/

    /**
        @aribaapi private
    */
    private static final Map groupToService = MapUtil.map();

    /**
        Returns a reference to the shared ResourceService.  Does not create
        one if a service doesn't exist.

        @return reference to shared ResourceService; may be null;
        @aribaapi private
    */
    public static ResourceService checkForService ()
    {
        return ResourceService.serviceCouldBeNull();
    }

    /**
        Returns a reference to the shared ResourceService.

        Unlike other service objects (base, core, fields), getService() always
        returns a non-null reference to a ResourceService object.

        @return ResourceService an instance of the resource service
        which can be used to invoke non static methods. Do not save
        this pointer past the end of your method invocation as it may
        change depending on the application context at a later point.
        @aribaapi documented
    */
    public static ResourceService getService ()
    {
        ResourceService service = serviceCouldBeNull();

        /*
            i18n Note: This code is commented out because it will
            break all command line tools because they do not setup
            the ResourceService correctly.  See CommandLineHTTPServer
            for an example of the correct service setup.
            Assert.that(service != null,
            "Resource service must be properly instantiated " +
            "before use.");
        */

        if (service == null) {
            if (DEFAULT_SERVICE == null) {
                service = new ResourceService(URLUtil.url(), null);
                ResourceService.setService(service);
            }
            else {
                    // ToDo: Fix this with service discovery
                    // If the service is null, then use the DEFAULT_SERVICE,
                    // which in the case of server is a singleton object.
                    // On the client side, we use the thread-group to look
                    // up, and it will not be null (if it was used properly
                    // on the client side), and we will return that. If that
                    // is null,this class supports creating new service object.
                    // This is not an issue for server.
                    // Under some condition, when two clients are running under
                    // one JVM, and only one of them initialized the service,
                    // then the client who didn't do the initialization may get
                    // the service object of the clien-who-did-the-init. We
                    // need to fix the error case, where the client doesn't
                    // do proper initialization anyway.
                service = DEFAULT_SERVICE;
            }
        }
        return service;
    }


    private static final State ThisThreadResourceService =
        StateFactory.createState();

    /**
        Returns pointer to shared ResourceService.  Does not create one if it
        does not exist.

        @return shared ResourceService; may be null
        @aribaapi private
    */
    public static ResourceService serviceCouldBeNull ()
    {
        ResourceService service = (ResourceService)ThisThreadResourceService.get();
        if (service != null) {
            return service;
        }

        ThreadGroup group = Thread.currentThread().getThreadGroup();
        synchronized (groupToService) {
            service = (ResourceService)groupToService.get(group);
        }
        ThisThreadResourceService.set(service);
        return service;
    }

    private static ResourceService DEFAULT_SERVICE = null;
    /**
        Sets up the shared ResourceService.  This should only be called once
        for a process/applet.

        @param service shared ResourceService
        @aribaapi private
    */
    public static void setService (ResourceService service)
    {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThisThreadResourceService.set(null);
        synchronized (groupToService) {
            if (service != null) {
                groupToService.put(group, service);
            }
            else {
                groupToService.remove(group);
            }
            DEFAULT_SERVICE = service;
        }
    }

      /*-----------------------------------------------------------------------
          END OF Discovery methods for ResourceService object
      -----------------------------------------------------------------------*/

    /**
        A convenience using the service() and the locale, which is
        obtained by calling {@link #getLocale}.

        @param stringTable the name of the string table to retrieve
        the resource from.
        @param key they key to look up in that resource table.
        @return String the value mapped in that string table.
        @see #getLocale
        @aribaapi documented
    */
    public static String getString (String stringTable, String key)
    {
        return ResourceService.getService().getLocalizedString(stringTable,
                                                               key);
    }

    /**
        Looks up the string in the given table using the given key.

        @deprecated Replaced by getString(String, String)

        @param stringTable table
        @param key key
        @aribaapi private
    */
    public static String string (String stringTable, String key)
    {
        return getString(stringTable, key);
    }

    /**
        A convenience using the service() and the specified locale.
        @param stringTable the name of the string table to retrieve
        the resource from.
        @param key they key to look up in that resource table.
        @param locale the locale to use for determining which
        translation of the string table to use.
        @return String the value mapped in that string table.

        @aribaapi documented
    */
    public static String getString (String stringTable, String key, Locale locale)
    {
        return ResourceService.getService().getLocalizedString(stringTable,
                                                               key,
                                                               locale);
    }

    // format: optionalPrefix.key
    private final static String OptionPrefixKeyFormat = "%s.%s";

    /**
        A convenience using the service() and the specified locale.
        Looks up the value of the key or a combination of the key
        and the prefix.  The priority is given to the value associated
        with the combination key and prefix.  If the combination key,
        does not have an associated value, it defaults to the value
        associated with the key.

        @param stringTable the name of the string table to retrieve
        the resource from.
        @param key they key to look up in that resource table.
        @param optionalKeyPrefix key prefix if any.
        @param locale the locale to use for determining which
        translation of the string table to use.
        @return String the value mapped in that string table.

        @aribaapi documented
    */
    public static String getString (String stringTable, String key,
                                    String optionalKeyPrefix, Locale locale)
    {
        String value = null;
        if (!StringUtil.nullOrEmptyOrBlankString(optionalKeyPrefix)) {
            String combinationKey = Fmt.S(OptionPrefixKeyFormat,
                                          optionalKeyPrefix, key);
            value = ResourceService.getService().getLocalizedString(stringTable,
                                                                    combinationKey,
                                                                    locale, false);
            if (combinationKey.equals(value)) {
                value = null;
            }
        }
        if (value == null) {
            value = ResourceService.getService().getLocalizedString(stringTable,
                                                                    key,
                                                                    locale);
        }
        return value;
    }

/**
        A convenience using the service(), the specified locale, and whether
        to display a warning if not found.

        @param stringTable the name of the string table to retrieve
        the resource from.
        @param key they key to look up in that resource table.
        @param locale the locale to use for determining which
        translation of the string table to use.
        @param displayWarning if false and the key is not found in the
        specified table a warning will be suppressed rather than
        printed.
        @return String the value mapped in that string table.

        @aribaapi documented
    */
    public static String getString (String stringTable,
                                    String key,
                                    Locale locale,
                                    boolean displayWarning)
    {
        return ResourceService.getService().getLocalizedString(stringTable,
                                                               key,
                                                               locale,
                                                               displayWarning);
    }

    /**
        Looks up the string in the given table using the given key and locale.

        @deprecated Replaced by getString(String, String, Locale)

        @param stringTable table
        @param key key
        @param locale locale
        @aribaapi private
    */
    public static String string (String stringTable, String key, Locale locale)
    {
        return getString(stringTable, key, locale);
    }

    /**
        A convenience for checking if a given string is a resource key.
        @param str a string to check if it follows our convention
        for a resource key. A null will always return false.
        @return true if and only if it begins with an @.
        @aribaapi documented
    */
    public static boolean isNlsKey (String str)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(str)) {
            return str.startsWith(NlsTag);
        }

        return false;
    }

    /**
        This is to add a backdoor to ResourceService to allow docs and QA folks
        to force the HTML UI to render in a certain locale.

        @param overrideLocale locale to use as an override
        @aribaapi private
    */
    public void setOverrideLocale (Locale overrideLocale)
    {
        this.overrideLocale = overrideLocale;
    }

    /**
        This is to add a backdoor to ResourceService to allow docs and QA folks
        to force the HTML UI to render in a certain locale.

        @return override locale
        @aribaapi private
    */
    public Locale getOverrideLocale ()
    {
        return overrideLocale;
    }

    /**
        This is to add a backdoor to ResourceService to allow docs and QA folks
        to force the HTML UI to render in a certain locale.

        @param overrideLocaleOn flag for override locale
        @aribaapi private
    */
    public void setOverrideLocaleOn (boolean overrideLocaleOn)
    {
        this.overrideLocaleOn = overrideLocaleOn;
    }

    /**
        This is to add a backdoor to ResourceService to allow docs and QA folks
        to force the HTML UI to render in a certain locale.

        @return override locale flag
        @aribaapi private
    */
    public boolean getOverrideLocaleOn ()
    {
        return overrideLocaleOn;
    }

    /**
        This is to add a backdoor to ResourceService for LocaleID to
        set a list of Locale UniqueNames

        @param locales list of Locale UniqueName Strings
        @aribaapi ariba
    */
    public void setLocales (List locales)
    {
        this.locales = locales;
    }

    /**
        This is to add a backdoor to ResourceService for Inspector to
        get a list of Locale UniqueName Strings

        @return the list of Locale UniqueName Strings
        @aribaapi private
    */
    public List getLocales ()
    {
        return locales;
    }

    /*-----------------------------------------------------------------------
        Public Constructors
      -----------------------------------------------------------------------*/

    /**
        Creates a new ResourceService with the given base URL and default locale.

        @param baseURL  base URL
        @param defaultLocale    default locale
        @aribaapi private
    */
    public ResourceService (URL baseURL, Locale defaultLocale)
    {
        this(baseURL, baseURL, defaultLocale);
    }

    public ResourceService (URL stringBaseURL, URL baseURL, Locale defaultLocale)
    {
        this.stringBaseURL = stringBaseURL;
        this.baseURL = baseURL;
        setDefaultLocale(defaultLocale);
        defaultStringTableProcessor = new DefaultStringCSVProcessor();
        registerStringProcessor(defaultStringTableProcessor);
    }

    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/
    /**
        Returns the "default" locale that clients should use.  This locale
        is also used for the locale-less versions of the localized* methods.
        This is to be used over Locale.getDefault() because this method may be overridden
        by subclassers to return the appropriate locale.

        @return the current Locale for the application context at the
        time it is invoked.
        @aribaapi documented
    */
    public Locale getLocale ()
    {
        return LocaleOfLastResort;
    }

    /**
     * Get a locale that is guaranteed to be available to the realm.
     * A realm may not have all locales available to it, for ui display.  This
     * method will check the locale, make sure it is one of the ones available to
     * the realm, or swizzle to an available locale.
     * @param locale - the prospective locale
     * @return the locale that the realm can use
     * @aribaapi documented
     */
    public Locale getRestrictedLocale (Locale locale)
    {
        // default implementation is to allow any locales
        return locale;
    }

    /**
        Returns the default locale.  See definition of default locale,
        above.  Note that the locale() method by contrast returns the
        locale of last resort, at least in this implementation
        (subclasses presumably might return something different).

        @return the Locale that this application has associated with
        the given context when the code is run. It may be specific to
        the user who triggered the action.

        @aribaapi documented
    */
    public Locale getDefaultLocale ()
    {
        return defaultLocale;
    }

    /**
        Sets the default locale.  See definition of default locale,
        above.  Note that the locale() method by contrast returns the
        locale of last resort, at least in this implementation
        (subclasses presumably might return something different).

        @param locale locale to set as the default
        @aribaapi ariba
    */
    public void setDefaultLocale (Locale locale)
    {
        if (locale != null) {
            this.defaultLocale = locale;
            defaultImagesRoot = Fmt.S("%s/%s/%s/%s",
                                        SystemUtil.SystemDirectoryString,
                                        ResourceRoot,
                                        defaultLocale,
                                        ImagesDirectory);
        }
        else {
            this.defaultLocale = LocaleOfLastResort;
            defaultImagesRoot = Fmt.S("%s/%s/%s/%s",
                                        SystemUtil.SystemDirectoryString,
                                        ResourceRoot,
                                        LocaleOfLastResort,
                                        ImagesDirectory);
        }
    }

    /**
        Returns the defaultImagesRoot.
        @return a string as a file path for root directory for images.

        @aribaapi documented
    */
    public String getDefaultImagesRoot ()
    {
        return defaultImagesRoot;
    }

    /**
        register string processor. we need to know this so that when clearCache is called, we
        can also callback to those string processor and clean all string caches out.

        @param processor the StringTableProcessor to register, must not be null.
        @aribaapi ariba
    */
    public void registerStringProcessor (StringTableProcessor processor)
    {
        Assert.that(stringTableProcessors.put(processor, new Gridtable()) == null,
                    "the stringTableProcessor %s was already registered !",
                    processor);

    }

    /**
        Allows you to "reboot" the Resource Manager, so you don't have to
        shut down everything just to reload a string resource that's changed
        on disk.

        @aribaapi ariba
    */
    public void clearCache ()
    {
        for (Iterator i = stringTableProcessors.keySet().iterator(); i.hasNext(); ) {
            Object processor = i.next();
            Gridtable stringTables = (Gridtable)stringTableProcessors.get(processor);
            synchronized (stringTables) {
                stringTables.clear();
            }
        }
    }


    /*-----------------------------------------------------------------------
        Help Methods
      -----------------------------------------------------------------------*/

    /**
        Returns the path of the root of the help system for this service's
        locale.

        @param helpPath helpPath to localize
        @return localized help path
        @aribaapi private
    */
    public String getLocalizedHelpPath (String helpPath)
    {
        return getLocalizedHelpPath(helpPath, getLocale());
    }


    /**
        Returns the path of the root of the help system for the specified
        locale.

        @param helpPath
        @param locale
        @aribaapi private
    */
    public String getLocalizedHelpPath (String helpPath, Locale locale)
    {
        return Fmt.S("%s/%s", helpDirectoryForLocale(locale), helpPath);
    }


    /**
        Returns the locale directory that contains help for the specified
        locale.

        @param locale locale
        @return help directory for locale
        @aribaapi private
    */
    public String helpDirectoryForLocale (Locale locale)
    {
        return resourceDirectoryForLocale(helpDirs, locale, HelpDirectory);
    }


    /**
        Returns the locale directory that contains reports template for the specified locale.

        @param locale locale
        @return  reports template directory for locale
        @aribaapi private
    */
    public String reportTemplateDirectoryForLocale (Locale locale)
    {
        return resourceDirectoryForLocale(reportTemplatesDirs, locale, ReportTemplatesDirectory);
    }

    /**
        Returns the locale directory that locates within resource directory
        for the specified locale.
     * @param dirMap cache directory map
     * @param locale locale
     * @param directory TODO

        @return a directory under resource directory for the specified locale
        @aribaapi private
    */
    protected String resourceDirectoryForLocale (Map dirMap, Locale locale, String directory)
    {
        String localeDir = (String)dirMap.get(locale);

        if (localeDir == null) {
            localeDir = getResourceDirectoryForLocale(locale, directory);
            dirMap.put(locale, localeDir);
        }

        return localeDir;
    }


    /**
        Returns the locale directory that contains help for the specified
        locale.

        @param locale locale
        @return help directory for locale
        @aribaapi private
    */
    protected String getHelpDirectoryForLocale (Locale locale)
    {
        return getHelpRootForLocale(locale);
    }


    /**
        Returns the root help directory for the given locale.

        @param locale locale
        @return root help directory for locale
        @aribaapi private
    */
    protected String getHelpRootForLocale (Locale locale)
    {
        return getResourceDirectoryForLocale(locale, HelpDirectory);
    }

    /**
        Returns a directory under resource directory for the given locale.

        @param locale locale
        @param directory directory name under resource directory
        @return root directory under resource directory for locale
        @aribaapi private
    */
    protected String getResourceDirectoryForLocale (Locale locale, String directory)
    {
        return Fmt.S("%s/%s/%s/%s", SystemUtil.SystemDirectoryString,
                ResourceRoot, locale, directory);
    }


    /*-----------------------------------------------------------------------
        String Methods
      -----------------------------------------------------------------------*/

    /**
        Lookup a localized string for this service's locale, which is
        obtained by calling {@link #getLocale}.

        @param stringTable the name of the stringTable to use
        @param key the key to lookup in the string table for the
        locale

        @return a localized string for this service's locale.
        @see #getLocale
        @aribaapi documented
    */
    public String getLocalizedString (String stringTable, String key)
    {
        return getLocalizedString(stringTable, key, getLocale());
    }

    /**
        Lookup a localized string for the specified locale.
        Will display an error message if the key cannot be resolved.

        @param stringTable the name of the stringTable to use
        @param key the key to lookup in the string table for the
        locale
        @param locale the locale to format the string in

        @return a localized string in the specified locale.

        @aribaapi documented
    */
    public String getLocalizedString (String stringTable,
                                   String key,
                                   Locale locale)
    {
        return getLocalizedString(stringTable, key, locale, true);
    }

    /**
        Lookup a localized string for the specified locale.

        @param stringTable the name of the stringTable to use
        @param key the key to lookup in the string table for the
        locale
        @param locale the locale to format the string in
        @param displayWarning toggles the display of missing-key warning

        @return a localized string in the specified locale.

        @aribaapi documented
    */
    public String getLocalizedString (String stringTable,
                                      String key,
                                      Locale locale,
                                      boolean displayWarning)
    {
        return getLocalizedString(stringTable, key, locale, displayWarning, true);
    }

    /**
        Lookup a localized string for the specified locale.

        @param stringTable the name of the stringTable to use
        @param key the key to lookup in the string table for the
         locale
        @param locale the locale to format the string in
        @param displayWarning toggles the display of missing-key warning
        @param defaultingLocale toggles defaulting to canonical/lastresort locale
        @param defaultingSystem toggles defaulting to system resource

        This is being overwritten by BaseResourceService.java, only meaningful
        for realm resources.

        @aribaapi ariba
    */
    public String getLocalizedString (String stringTable,
                                      String key,
                                      Locale locale,
                                      boolean displayWarning,
                                      boolean defaultingLocale,
                                      boolean defaultingSystem)
    {
        Assert.that(defaultingSystem, "DefaultingSystem has to be true for " +
            "system resources");
        return getLocalizedString(stringTable,
                                  key,
                                  locale,
                                  displayWarning,
                                  defaultingLocale);
    }


    // Check if there's an overlay table/string for the given lookup
    protected String lookupOverrideString (String stringTable,
                                      String key,
                                      Locale locale,
                                      boolean defaultingLocale,
                                      boolean defaultingSystem)

    {
        String localized = null;
        String overlayTable = _dynamicOverlayForTable(stringTable);
        if (overlayTable == null) {
            return null;
        }
        Map strings = stringTable(overlayTable,
                                  locale,
                                  defaultStringTableProcessor,
                                  defaultingLocale, defaultingSystem);
        if (strings == null) {
                // "Null string table returned in Resource Service"
            Log.util.error(2962, overlayTable);
        }
        else {
            localized = (String)strings.get(key);
        }
        return localized;
    }

    /**
        Lookup a localized string for the specified locale.

        @param stringTable the name of the stringTable to use
        @param key the key to lookup in the string table for the
        locale
        @param locale the locale to format the string in
        @param displayWarning toggles the display of missing-key warning
        @param defaultingLocale toggles defaulting to canonical/lastresort locale

        @return a localized string in the specified locale. If defaulting is set
        to false and no string was found, it will return null. If defaulting is
        true and no string is found, the *key* is returned. Logging *relies* on
        this behavior.

        @aribaapi ariba
    */
    public String getLocalizedString (String stringTable,
                                      String key,
                                      Locale locale,
                                      boolean displayWarning,
                                      boolean defaultingLocale)

    {
        if (overrideLocaleOn) {
            locale = overrideLocale;
        }

        String localized = lookupOverrideString(stringTable, key, locale,
                                                    defaultingLocale, true);
        if (localized != null) {
            return localized;
        }

        Map strings = stringTable(stringTable,
                                  locale,
                                  defaultStringTableProcessor,
                                  defaultingLocale);

        if (strings == null) {
                // "Null string table returned in Resource Service"
            Log.util.error(2962, stringTable);
        }
        else {
            localized = (String)strings.get(key);
        }

        if (localized == null) {
            if (displayWarning) {
                String msg = Fmt.S("Missing string resource: %s in %s for locale %s",
                                   key,
                                   stringTable,
                                   locale);
                SystemUtil.err().println(msg);
                SystemUtil.err().flush();
                if (Log.i18n.isDebugEnabled()) {
                    Log.logStack(Log.i18n, msg);
                }
            }

            localized = key;
            // Prevent the same error from happening. Synchronize because other methods
            // can use iterators on the map and could get ConcurrentModificationException.
            synchronized (strings) {
                strings.put(key, localized);
            }     
        }

        return localized;
    }

    /**
       Lookup a localized string for the specified locale. It is the same
       as getLocalizedString in Util

       @param stringTable the name of the stringTable to use
       @param key the key to lookup in the string table for the
       locale
       @param locale the locale to format the string in

       This is being overwritten by BaseResourceService.java, only meaningful
       for realm resources.

       @aribaapi ariba
    */
    public String getLocalizedFormat (String stringTable,
                                      String key,
                                      Locale locale)
    {
        return getLocalizedFormat(stringTable, key, locale, true, true, true);
    }

    /**
       Lookup a localized string for the specified locale. It is the same
       as getLocalizedString in Util

       @param stringTable the name of the stringTable to use
       @param key the key to lookup in the string table for the
       locale
       @param locale the locale to format the string in
       @param displayWarning toggles the display of missing-key warning
       @param defaultingLocale toggles defaulting to canonical/lastresort locale
       @param defaultingSystem toggles defaulting to system resource

       This is being overwritten by BaseResourceService.java, only meaningful
       for realm resources.

       @aribaapi ariba
    */
    public String getLocalizedFormat (String stringTable,
                                      String key,
                                      Locale locale,
                                      boolean displayWarning,
                                      boolean defaultingLocale,
                                      boolean defaultingSystem)
    {
        return getLocalizedString(stringTable,
                                  key,
                                  locale,
                                  displayWarning,
                                  defaultingLocale,
                                  defaultingSystem);
    }

    /**
        If the specified string starts with an '@', it is assummed to be a
        composed localized string key (the string table and key are
        concatenated, separated by a slash (e.g. "@AStringTable/MyString"),
        and it will be localized in the specified locale.  If the string does
        not start with an '@', the string itself is returned.

        @param string string to localize
        @param locale locale
        @return localized composite key
        @aribaapi private
    */
    public String getLocalizedCompositeKey (String string,
                                            Locale locale)
    {
        return getLocalizedCompositeKey(string, locale, true);
    }

    /**
        If the specified string starts with an '@', it is assummed to be a
        composed localized string key (the string table and key are
        concatenated, separated by a slash (e.g. "@AStringTable/MyString"),
        and it will be localized in the specified locale.  If the string does
        not start with an '@', the string itself is returned.

        @param string string to localize
        @param locale locale
        @param displayWarning toggles the display of missing-key warning
        @return localized composite key
        @aribaapi private
    */
    public String getLocalizedCompositeKey (String string,
                                            Locale locale,
                                            boolean displayWarning)
    {
        return getLocalizedCompositeKey(string, locale, displayWarning, true, true);
    }


    /*
        Support for Dynamic contextual resource file overlays

        The Problem:
        Our apps often have labels (or helpTips, or...) on fields of classes that need to be
        contextually specified based on how the object is being used or who is looking at it.
        For instance, in Sourcing, the label for "Supplier" on an Event should really read
        "Respondent" when that event is actually a survey.  The AML references resource string
        for the "generic" (Event) terminology, but we want to "overlay" more specific terminology
        for some resource strings when what we're really looking at is a Survey.

        Solution:
        - We let the app register certain resource files as "dynamic" (able to be overlaid)
        - When normal static resolution of resource keys is performed (say by FieldProperties) we
          leave keys refering to dynamic table unresolved (return the "@tableName/key" string back
        - App code uses push/popOverlayForTable() to set context on the thread to specify
          contextual overlay for dynamic resource files (e.g. specifying that "sourcing.core.Content"
          should be overlaid with "sourcing.core.Content.Survey" for any lookups in this context.
          (see also AWResourceOverlay)
        - When FieldProperties (or other resource lookup code) calls
          getLocalizedStringWithDynamicResolution() we check for keys using overlaid files, and
          check the overlay for a match first, thereby giving the app a contextually tailored string
          
     */

    // Thread Local -- current dynamic overlays (for the current thread)
    private static final State _dynamicOverlayState = StateFactory.createState();
    private static GrowOnlyHashtable _DynamicTables = new GrowOnlyHashtable();

    /**
        Called by app code at initialization to register resource files ("tables")
        that may later be overlaid via pushOverlayForTable()

        @param tableName Name of resource file that will be overlaid (e.g. "sourcing.core.Content")
        @aribaapi private
    */
    public static void registerDynamicTableName (String tableName)
    {
        _DynamicTables.put(tableName, Boolean.TRUE);
    }

    protected static String _dynamicOverlayForTable (String tableName)
    {
        Map overlays = (Map)_dynamicOverlayState.get();
        return (overlays != null && !overlays.isEmpty())
                ? (String)overlays.get(tableName) : null;
    }

    /**
        Called by app code to push an "overlay" resource file for a particular file
        onto the current thread state.
        Calling code should stash the returned (original) string, and pass it
        back in a bracketed call to popOverlayForTable()
        (App code should be run in a try {} finally block, with pop called in finally

        @param tableName file to be overlaid (e.g. "sourcing.core.Content")
        @param overlayTableName the overlay file (e.g. "sourcing.core.Content.Survey")
        @return previous value for this key -- should be passed back to pop call
        @aribaapi private
    */
    static public String pushOverlayForTable (String tableName, String overlayTableName)
    {
        String restoreName = null;
        Map overlays = (Map)_dynamicOverlayState.get();
        if (overlays == null) {
            overlays = MapUtil.map();
            _dynamicOverlayState.set(overlays);
        }
        else {
            restoreName = (String)overlays.get(tableName);
        }
        overlays.put(tableName, overlayTableName);
        return restoreName;
    }

    /**
        Pop a previously pushed overlay from the thread state

        @param tableName file that had been overlaid (e.g. "sourcing.core.Content")
        @param prev the value returned from the corresponding call to push...()
        @aribaapi private
    */
    static public void popOverlayForTable (String tableName, String prev)
    {
        Map overlays = (Map)_dynamicOverlayState.get();
        Assert.that(overlays != null, "Must have called pop without first pushing");
        if (prev == null) {
            overlays.remove(tableName);
        }
        else {
            overlays.put(tableName, prev);
        }
    }


    /**
        If the specified string starts with an '@', it is assummed to be a
        composed localized string key (the string table and key are
        concatenated, separated by a slash (e.g. "@AStringTable/MyString"),
        and it will be localized in the specified locale.  If the string does
        not start with an '@', the string itself is returned.

        @param string string to localize
        @param locale locale
        @param displayWarning toggles the display of missing-key warning
        @param defaulting toggles defaulting to rooted/last resort locale
        @return localized composite key
        @aribaapi private
    */
    public String getLocalizedCompositeKey (String string,
                                            Locale locale,
                                            boolean displayWarning,
                                            boolean defaulting)
    {
        return getLocalizedCompositeKey(string, locale, displayWarning, defaulting, true);
    }

    /**
        If the specified string starts with an '@', it is assummed to be a
        composed localized string key (the string table and key are
        concatenated, separated by a slash (e.g. "@AStringTable/MyString"),
        and it will be localized in the specified locale.  If the string does
        not start with an '@', the string itself is returned.

        @param string string to localize
        @param locale locale
        @param displayWarning toggles the display of missing-key warning
        @param defaulting toggles defaulting to rooted/last resort locale
        @param resolveDynamicKeys whether keys that support dynamic substitution should be resolved.  If false, caller
               should later call getLocalizedStringWithDynamicResolution() at display time to get the final version
        @return localized composite key
        @aribaapi private
    */
    public String getLocalizedCompositeKey (String string,
                                            Locale locale,
                                            boolean displayWarning,
                                            boolean defaulting,
                                            boolean resolveDynamicKeys)
    {
        if (string == null) {
            return null;
        }

        String[] parsedKey = parseCompositeKey(string);

        // If the tableName is registered as dynamic, return the unresolved key for later dynamic resolution
        if (parsedKey[0] != null
                && (_DynamicTables.get(parsedKey[0]) == null || resolveDynamicKeys)) {
            String result = getLocalizedString(parsedKey[0],
                parsedKey[1],
                locale,
                displayWarning,
                defaulting);
            if (result != null && result.startsWith(EmbeddedSubstitutionsFlag)) {
                result = resolveEmbeddedStrings(result, locale, resolveDynamicKeys);
            }
            return result;
        }
        else {
            return string;
        }
    }

    /**
        Called on strings that may need further (contextual / dynamic) evaluation.
        E.g if a resource string was not fully resolved at the time of field properties
        initialization (because it referenced a resource file that was registered as
        Dynamic (see ResourceService.registerDynamicTableName())) then this will perform
        that file resolution.

        @aribaapi private
    */
    public static String translateDynamicString (String string, Locale locale)
    {
        if (string != null && string.startsWith(NlsTag)
                && (locale != null)) {
            // if @@ then this is a string with embeddings, if just @ then it's a key
            ResourceService rs = ResourceService.getService();
            if (string.startsWith(EmbeddedSubstitutionsFlag)) {
                string = rs.resolveEmbeddedStrings(string, locale, true);
            } else {
                string = rs.getLocalizedCompositeKey(string, locale, true, true, true);
            }
        }
        return string;
    }

    /**
     * Break a composite key into its parts.
     * For the resource service, a "composite key" is a key of the form:
     * "@stringtable/key" where stringtable is the name of some resource string table
     * and the key is a key within that file.  This method will break up such a key
     * into its parts.  The 0th element of the return value is the string table and
     * the 1th element is the key.  If the compositeKey is not of the composite key
     * form then the the 0th element will be null and the string will just be the
     * composite key
     * @param compositeKey The composite key to be parsed
     * @return the parsed composite key
     * @aribaapi documented
     */
    public String[] parseCompositeKey (String compositeKey)
    {
        String[] result = new String[2];
        // by default, there is no string table and we hand back
        // the key as the string
        result[1] = compositeKey;

        // if it looks like a compositeKey, put together the result
        if (compositeKey != null && compositeKey.startsWith(NlsTag)) {
            int index = compositeKey.indexOf('/');
            if (index >= 0) {
                // string table:
                result[0] = compositeKey.substring(1, index);
                // the key:
                result[1] = compositeKey.substring(index + 1);
            }
        }


        return result;
    }

    public String getLocalizedCompositeFormat (String string,
                                            Locale locale,
                                            boolean displayWarning,
                                            boolean defaulting)
    {
        if (string != null && string.startsWith(NlsTag)) {
            int index = string.indexOf('/');
            if (index >= 0) {
                String key = string.substring(index + 1);
                String stringTable = string.substring(1, index);
                string = getLocalizedFormat(stringTable,
                    key,
                    locale,
                    displayWarning,
                    defaulting,
                    true);
            }
        }
        return string;
    }


    /**
        If a Map contains a string starting with an '@', it is
        assumed to be a localized string key which is localized using the
        localizedCompositeKey(). This is a recursive method.

        @param table data for creating map
        @param locale locale for creating map
        @aribaapi private
    */
    public static void createLocalizedMap (Map table,
                                                 Locale locale)
    {
        Iterator e = table.keySet().iterator();
        while (e.hasNext()) {
            String fieldName = (String)e.next();
            Object element = table.get(fieldName);
            if (element instanceof Map) {
                createLocalizedMap((Map)element, locale);
            }
            else if (element instanceof String) {
                element = getService().getLocalizedCompositeKey((String)element,
                                             locale);
                table.put(fieldName, element);
            }
        }
    }

    /**
        Returns a string table with the specified path in the specified
        locale.

        @param path path
        @param locale locale
        @return string table with the specified path in the specified
                locale
        @aribaapi ariba
    */
    public Map stringTable (String path, Locale locale)
    {
        return stringTable(path, locale, defaultStringTableProcessor);
    }

    /**
        Returns a string table with the specified path in the specified
        locale.

        @param path path
        @param locale locale
        @param defaultingSystem defaulting to system
        @return string table with the specified path in the specified
         locale
        @aribaapi ariba
    */
    public Map stringTable (String path, Locale locale, boolean defaultingSystem)
    {
        return stringTable(path,
                           locale,
                           defaultStringTableProcessor,
                           true,
                           defaultingSystem);
    }


    /**
        Returns a string table with the specified path in the specified
        locale.

        @param path path
        @param locale locale
        @param processor customer string csv processor
        @return string table with the specified path in the specified
                locale
        @aribaapi ariba
    */
    public Map stringTable (String path, Locale locale, StringTableProcessor processor)
    {
        return stringTable(path, locale, processor, true);
    }

     /**
        Returns a string table with the specified path in the specified
        locale.

        @param path path
        @param locale locale
        @param processor customer string csv processor
        @param defaultingLocale toggle defaulting to relaxed locale

        @return string table with the specified path in the specified
         locale
        @aribaapi ariba
    */
    public Map stringTable (String path,
                            Locale locale,
                            StringTableProcessor processor,
                            boolean defaultingLocale)
    {
        return stringTable(path, locale, processor, defaultingLocale, true);
    }

    /**
        Returns a string table with the specified path in the specified
        locale.

        @param path path
        @param locale locale
        @param processor customer string csv processor
        @param defaultingLocale toggle defaulting to relaxed locale
        @param defaultingSystem toggles defaulting to system resource

        @return string table with the specified path in the specified
                locale
        @aribaapi ariba
    */
    public Map stringTable (String path,
                            Locale locale,
                            StringTableProcessor processor,
                            boolean defaultingLocale,
                            boolean defaultingSystem)
    {
        Assert.that(processor != null, "StringCSVProcess must not be null");
        if (path == null) {
            return null;
        }

        Gridtable stringTables = (Gridtable)stringTableProcessors.get(processor);
        Map twinMap;
        Map strings = null;
        synchronized (stringTables) {
            twinMap = (Map)stringTables.get(path, locale);
        }

        if (twinMap != null) {
            strings = (Map)twinMap.get(Constants.getBoolean(defaultingLocale));
        }

        if (strings == null) {
            twinMap = getStringTable(path,
                                     locale,
                                     true,
                                     processor,
                                     defaultingLocale,
                                     defaultingSystem);
            synchronized (stringTables) {
                    // at this point we may be a client-side ResourceService storing
                    // a map returned by a server-side ResourceService
                if (twinMap != null) {
                    stringTables.put(path, locale, twinMap);
                }
            }
            strings = (Map)twinMap.get(Constants.getBoolean(defaultingLocale));
        }

        return strings;
    }

    /**
        Returns a flat string table with the specified path in the specified
        locale.

        @param path path
        @param locale locale
        @param defaultingLocale toggle defaulting to relaxed locale
        @param defaultingSystem toggles defaulting to system resource

        @return string table with the specified path in the specified
            locale
        @aribaapi ariba
    */

    public Map stringTableContent (String path,
                                   Locale locale,
                                   boolean defaultingLocale,
                                   boolean defaultingSystem)
    {
       return stringTableContent(path,
                                 locale,
                                 defaultStringTableProcessor,
                                 defaultingLocale,
                                 defaultingSystem);

    }
    /**
        Returns a string table with the specified path in the specified
        locale.

        @param path path
        @param locale locale
        @param processor customer string csv processor
        @param defaultingLocale toggle defaulting to relaxed locale
        @param defaultingSystem toggles defaulting to system resource

        @return string table with the specified path in the specified
            locale
        @aribaapi ariba
    */

	public Map stringTableContent (String path,
                                   Locale locale,
                                   StringTableProcessor processor,
                                   boolean defaultingLocale,
                                   boolean defaultingSystem)
    {
		return stringTable(path,
                           locale,
                           processor,
                           defaultingLocale,
                           defaultingSystem);

    }

    /**
        Allows you to prepopulate the string table cache.  Used for optimizing
        RPC.  Not for general use.

        @see ariba.base.client.BaseClient#getStartupInfo
        @param path path
        @param stringTable  string table to populate
        @aribaapi ariba
    */
    public void cacheStringTable (String path, Map stringTable)
    {
        cacheStringTable(path, defaultLocale, stringTable, defaultStringTableProcessor);
    }

    /**
        Allows you to prepopulate the string table cache.
        This should be used for optimization of RPC or for by StringTableProcessor only.

        @param path path
        @param locale locale for the resource table
        @param stringTable  string table to populate
        @param processor the processor who processed the table
        @aribaapi ariba
    */
    public void cacheStringTable (String path,
                                  Locale locale,
                                  Map stringTable,
                                  StringTableProcessor processor)
    {
        Gridtable stringTables = (Gridtable)stringTableProcessors.get(processor);
        synchronized (stringTables) {
            stringTables.put(path, locale, stringTable);
        }
    }


    /**
        Returns the string table corresponding to the specified path and
        locale.  The string table maps locale independent tokens (Strings) to
        localized strings.

        @param path       Path to the string file (can use either slash).
        @return           Returns a (potentially empty) map
                          Never returns null.

        @aribaapi private
    */
    protected Map getStringTable (String path,
                                  Locale locale)
    {
        return getStringTable(path, locale, true);
    }

    protected Map getStringTable (String path,
                                  Locale locale,
                                  boolean displayWarning)
    {
        return getStringTable(path, locale, displayWarning, defaultStringTableProcessor);
    }

    protected Map getStringTable (String path,
                                  Locale locale,
                                  boolean displayWarning,
                                  StringTableProcessor processor)
    {
        return getStringTable(path, locale, displayWarning, processor, true);
    }

    protected Map getStringTable (String path,
                                  Locale locale,
                                  boolean displayWarning,
                                  StringTableProcessor processor,
                                  boolean defaulting)
    {
        return getStringTable(path,
                              locale,
                              displayWarning,
                              processor,
                              defaulting,
                              true);
    }

    protected Map getStringTable (String path,
                                  Locale locale,
                                  boolean displayWarning,
                                  StringTableProcessor processor,
                                  boolean defaultingLocale,
                                  boolean defaultingSystem)
    {
        Assert.that(processor != null,
                    "StringCSVProcessor must not be null for getStringTable");
        Gridtable stringTables = (Gridtable)stringTableProcessors.get(processor);
        loadStringsIntoTable(stringBaseURL,
                             stringTables,
                             getSearchDirs(locale, defaultingLocale),
                             path,
                             displayWarning,
                             processor,
                             defaultingLocale);
        synchronized (stringTables) {
            return (Map)stringTables.get(path, locale);
        }
    }

    /* Debug facility for runtime pseudo localization */
    public interface PseudoLocalizer {
        // Should process strings if this is the pseudo-localized locale, or return unmolested map otherwise
        Map process (Locale locale, Map strings);
        String process (Locale locale, String string);
    }

    static PseudoLocalizer _PseudoLocalizer = null;

    public static void _setPseudoLocalizer (PseudoLocalizer pl)
    {
        _PseudoLocalizer = pl;
    }

    public static PseudoLocalizer _getPseudoLocalizer ()
    {
        return _PseudoLocalizer;
    }
    
    private static void loadStringsIntoTable (URL stringBaseURL,
                                              Gridtable stringTables,
                                              List<Locale> searchLocales,
                                              String path,
                                              boolean displayWarning,
                                              StringTableProcessor processor,
                                              boolean defaultingLocale)
    {
        Map allStrings = MapUtil.map();
        /*
            Iterate through our chosen search path and overlay onto the more
            general locale strings those found in the more specific locales.
            For each locale dir in the search path, we look first in the system
            directory then in the config directory (whose entries take precedence).
        */
        for (int i = 0; i < searchLocales.size(); i++) {
            Locale searchLocale = searchLocales.get(i);

            Map thisLocaleStrings = null;
            Map twinMap;
            synchronized (stringTables) {
                    // get the strings for this level in the search path
                twinMap = (Map)stringTables.get(path, searchLocale);
            }
            if (twinMap != null) {
                thisLocaleStrings =
                    (Map)twinMap.get(Constants.getBoolean(defaultingLocale));
            }

                // if strings have been loaded for this level already, just
                // merge them into the strings from all the previous levels
            if (thisLocaleStrings != null) {
            	// Synchronized because getLocalizedString could modify source map during merge.
            	synchronized (thisLocaleStrings) {
                    processor.mergeStringTables(allStrings, thisLocaleStrings);
            	}
            }
            else {
                Map strings = getStringsForLocale(stringBaseURL,
                                              searchLocale,
                                              path,
                                              displayWarning,
                                              processor);
                processor.mergeStringTables(allStrings, strings);
                if (twinMap == null) {
                    twinMap = MapUtil.map();
                }
                //twinMap contains two keys, Boolean.TRUE and Boolean FALSE,
                //that indicated the detaultingLocale is true or false. The
                //values are the strings tables. twinMap resides in the gridtable.
                if (_PseudoLocalizer != null) {
                    allStrings = _PseudoLocalizer.process(searchLocale, allStrings);
                }
                twinMap.put(Constants.getBoolean(defaultingLocale),
                            MapUtil.copyMap(allStrings));
                synchronized (stringTables) {
                    stringTables.put(path, searchLocale, twinMap);
                }
            }
        }
    }

    /**
        Is an enumerated type representing the locations where string resources may
        be found under the string resource base URL.

        @aribaapi ariba
    */
    public static enum ResourceLocation {
        internal,
        ariba,
        config
    }

    private static String getRelativePathToStringResources (ResourceLocation location, Locale locale)
    {
        return Fmt.S("%s/%s/%s/%s", location, ResourceRoot, locale, StringsDirectory);
    }

    private static final Pattern StringTablePattern = Pattern.compile("(.*)\\.csv");

    /**
        @aribaapi ariba
    */
    protected void collectStringTableNames (Locale locale, Collection<String> collector)
    {
        if (!"file".equals(stringBaseURL.getProtocol())) {
            Assert.that(false, "not supported for non-file base-URLs");
        }
        File baseFile = new File(stringBaseURL.getFile());
        for (ResourceLocation location : ResourceLocation.values()) {
            File dir = new File(baseFile, getRelativePathToStringResources(location, locale));
            String[] fileNames = dir.list();
            if (fileNames != null) {
                Matcher matcher = StringTablePattern.matcher("");
                for (String fileName : fileNames) {
                    if (matcher.reset(fileName).matches()) {
                        collector.add(matcher.group(1));
                    }
                }
            }
        }
    }

    /**
        Returns the set of all string table names that exist and may contain
        resources for the specified locale. <p/>

        @aribaapi ariba
    */
    public Set<String> getAllStringTableNames (Locale locale)
    {
        List<Locale> locales = getSearchDirs(locale, true);
        Set<String> result = SetUtil.set();
        Set<Locale> processed = SetUtil.set();
        for (Locale loc : locales) {
            if (!processed.contains(loc)) {
                collectStringTableNames(loc, result);
                processed.add(loc);
            }
        }
        return result;
    }

    protected static Map getStringsForLocale (URL stringBaseURL,
                                              Locale searchLocale,
                                              String path,
                                              boolean displayWarning,
                                              StringTableProcessor processor)
    {
        Map allStrings = MapUtil.map();
        // if no strings have been loaded for this level,
        // load them and merge them

        String baseUrl = stringWithTrailingSlash(stringBaseURL);
        // load from internal resource first so that it will
        // be overriden by the production strings if any
        String urlPath = Fmt.S("%s%s/%s",
                               baseUrl,
                               getRelativePathToStringResources(ResourceLocation.internal,
                                                                searchLocale),
                               path);

        Map internalStrings =
            loadStringsFromURL(urlPath, displayWarning, processor);
        processor.mergeStringTables(allStrings, internalStrings);

            // <baseURL>/ariba: "ariba" is hard-coded intentionally
        urlPath = Fmt.S("%s%s/%s",
                        baseUrl,
                        getRelativePathToStringResources(ResourceLocation.ariba, searchLocale),
                        path);

        Map systemStrings =
            loadStringsFromURL(urlPath, displayWarning, processor);
        processor.mergeStringTables(allStrings, systemStrings);

        // From classpath
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = null;
        // 1-BU48HL Some system threads (for example, a finalizer thread) do not have a
        // class loader, so if null, skip and just use config's strings.
        if (classLoader != null) {
            url = classLoader.getResource(
                Fmt.S("%s/%s/%s/%s.csv", ResourceRoot, searchLocale, 
                      StringsDirectory, path)
            );
        }
        if (url != null) {
            StringCSVConsumer consumer =
                processor.createStringCSVConsumer(url, displayWarning);
            Map classPathStrings = loadStringsFromCSV(url, consumer);
            if (classPathStrings != null) {
                processor.mergeStringTables(allStrings, classPathStrings);
            }
        }

        // note: we do config second so that its strings
        // take precedence over the strings found in the
        // system directory
        // "config" is hard-coded intentionally
        urlPath = Fmt.S("%s%s/%s",
                        baseUrl,
                        getRelativePathToStringResources(ResourceLocation.config, searchLocale),
                        path);

        Map configStrings =
            loadStringsFromURL(urlPath, displayWarning, processor);
        processor.mergeStringTables(allStrings, configStrings);

        return allStrings;
    }

    /**
        Dumps the contents of the given map to SystemUtil.out().

        @param ht map to display
        @aribaapi private
    */
    protected void dumpMap (Map ht)
    {
        Iterator e = ht.keySet().iterator();
        while (e.hasNext()) {
            Object key = e.next();
            Object value = ht.get(key);
            Fmt.F(SystemUtil.out(), "\t%s: %s", key, value);
        }
    }

    /**
        Loads either a .csv or .table file of strings into a map.  First
        tries the .csv, and then tries the .table.

        @param urlString Path to the file without the extension.
        @param displayWarning true to display warning messages if any,
        false to suppress warnings
        @return Map containing string table
        @aribaapi private
    */
    protected static Map loadStringsFromURL (String urlString,
                                           boolean displayWarning,
                                           StringTableProcessor processor)
    {
        try {

            URL url = URLUtil.makeURL(Fmt.S("%s.csv", urlString));
            StringCSVConsumer consumer =
                processor.createStringCSVConsumer(url, displayWarning);
            Map strings = loadStringsFromCSV(url, consumer);
            if (strings != null) {
                return strings;
            }

            url = URLUtil.makeURL(Fmt.S("%s.table", urlString));
            strings = loadStringsFromTable(url);
            if (strings != null) {
                return strings;
            }
        }
        catch (MalformedURLException e) {
            Log.util.warning(2804, urlString);
        }

        Log.i18n.debug("string table '%s' not found", urlString);

        return MapUtil.map();
    }

    /**
        Verifies that the supplied <code>resourceStream</code> is
        a stream on a file that may legitimately be considered a
        CSV resource file. <p/>

        By this we mean that the {@link CSVReader} can parse the
        file with a {@link CSVStringTableConsumer} consuming the
        tokens. <p/>

        No exception is thrown if the stream is good.

        <b>NOTE:</b>

        @param resourceStream the stream to check
        @param filePath the logical name of the resource (for debugging and
                error messages)
        @throws IOException if <code>resourceStream</code> can not be
                considered a CSV resource file
        @aribaapi private
    */
    public void verifyCSV (InputStream resourceStream, String filePath)
    throws IOException
    {
        StringCSVConsumer consumer = new CSVStringTableConsumer(filePath, false);
        CSVReader reader = new CSVReader(consumer);
        try {
            reader.readForSpecifiedEncoding(resourceStream, filePath);
        }
        catch (TunnelingException ex) {
            throw (IOException)ex.getNestedException();
        }
        finally {
            resourceStream.close();
        }
    }

    /**
        Loads the csv file specified by url into a map.  Returns null if
        the url is not found.

        @see ariba.util.io.CSVReader#readForSpecifiedEncoding

        @param url URL for CSV file
        false to suppress warnings
        @return map of CSV file contents; null if not found
        @aribaapi private
    */
    private static Map loadStringsFromCSV (URL url, StringCSVConsumer consumer)
    {
        Log.util.debug("Load CSV file : %s", url);
        InputStream in = null;
        try {
            if (!URLUtil.maybeURLExists(url)) {
                return null;
            }

            in = url.openStream();
            CSVReader reader = new CSVReader(consumer);
            reader.readForSpecifiedEncoding(in, url.toString());
            return consumer.getStrings();
        }
        catch (TunnelingException ex) {
            /* this only happens when the StringCSVConsumer has an exception
               current behavior is to assert */
            Exception nested = ex.getNestedException();
            Assert.that(false, nested.getMessage());
        }
        catch (IOException e) {
            if (in != null) {
                    // do not suppress this important one,
                    // we will ignore displayWarning.
                Log.util.warning(2805, url, e);
            }
        }
        finally {
            IOUtil.close(in);
        }
        return null;
    }

    /**
        Loads a string table in serialized map form.  The CharSet key
        must be specified to indicate the character set encoding for the file.

        @param url URL for string table
        @return Map with contents of string table
        @aribaapi private
    */
    private static Map loadStringsFromTable (URL url)
    {
        Log.util.debug("Load table file : %s", url);
            // We actually read the table twice.  Once to get the charset
            // (using the default charset), then once with the correct
            // charset.
        Map strings = TableUtil.loadMap(url, false);

        if (strings == null) {
            return null;
        }

        String encoding = (String)strings.get(CharSetKey);
        Reader reader = null;
        try {
            if (!URLUtil.maybeURLExists(url)) {
                return null;
            }
            InputStream in = url.openStream();
            reader = IOUtil.bufferedReader(in, encoding);
            Map table = (Map)
                new Deserializer(reader).readObject();
            return table;
        }
        catch (IOException e) {
            return null;
        }
        catch (DeserializationException e) {
            return null;
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException e) {
            }
        }
    }

    /*
        Embedded String Substitution -- "Glossary" Support.

        We now support Resource strings that themselves reference other resource strings
        that should be evaluated and substituted dynamically at usage time.
        Combined with the AWResourceOverlay/registerDynamicTable()/pushOverlayForTable()
        support, this enables swapping in alternate terminology into shared strings
        based on "context" (e.g. the subtype of document being viewed, the type of
        user viewing it) or realm-specific (customer customized) terminology overrided.

        Example of use:
        ----------------
        Say that aml.sourcing.Core.csv declares the following:

            EnableTieBidRule, "Can participants submit tie bids",

        We can change it as follows (note the @{} notation for embeddings):

            EnableTieBidRule, "Can @{glossary.sourcing.Core/participants} submit tie bids",

        We can then create glossary.sourcing.Core.csv as follows:

            participant, "participant",
            participants, "participants",
            RFX, "RFX",

        And, say, glossary.sourcing.Core.RFP.csv like this:
            participant, "respondent",
            participants, "respondents",
            RFX, "RFP",

        And, say, glossary.sourcing.Core.Auction.csv like this:
            participant, "supplier",
            participants, "suppliers",
            RFX, "Auction",

        Note that the base, ".RFP" and ".Auction" versions of the files each have
        different terminology for the referenced term, "participants".

        The app can then wrap UI content with something like this:

            <AWResourceOverlay glossary.sourcing.Core="$glossaryOverlayName">\
            ... content here ...
            </AWResourceOverlay>\

        and the inline substitution will occur based on the current dynamic context.

        Known Issues:
        - This works well for English, but is generally inadequate for languages
          where noun gender may require alternate forms of surrounding definite articles
          (La, El), adjectives, or even verbs.

        Thus, for now it use may need to be limited to formal product names (e.g.
        "Ariba Buyer"), where references typically to not have these issues.
     */
    
    // Matches strings like @{foo.bar/baz}
    private static final Pattern EmbeddedKeyPattern
            = java.util.regex.Pattern.compile("@\\{([\\w\\.]+)\\/(\\w+)\\}");

    protected static String prepareResourceString (String string)
    {
        if (string.indexOf("@{") >= 0) {
            // for embedded @ (embedded terminology lookup) put "@@" at start
            // as a quickly-checkable flag for dynamic resolution
            string = EmbeddedSubstitutionsFlag.concat(string);
        }
        return string;
    }

    protected String resolveEmbeddedStrings (String string, Locale locale, boolean resolveDynamic)
    {
        String result = string;
        if (string.startsWith(EmbeddedSubstitutionsFlag)) {
            Matcher m = EmbeddedKeyPattern.matcher(string.substring(2));
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String tableName = m.group(1);
                String key = m.group(2);
                String resolved = (_DynamicTables.get(tableName) == null || resolveDynamic)
                    ? getLocalizedString(tableName, key, locale, true, true)
                    : m.group(0);
                m.appendReplacement(sb, resolved);
            }
            m.appendTail(sb);
            result = sb.toString();

            // if not fully resolving we need to flag any remaining unresolved strings
            if (!resolveDynamic) {
                result = prepareResourceString(result);
            }
        }

        return result;
    }
        
    /**
     * Updates the designated resource file with the objects stored in
     * the given Map <b>Important</b>: The caller is responsible for the
     * synchronization on the file (using GlobalLocking for instance)
     *
     * @param stringTableName the name of the resource table to update
     * @param locale the locale of the resources
     * @param updates the map with only the key/value to update or add. For
     *            deletion see
     *            {@link #unsetLocalizedString(List, String, Locale)}
     * @throws BadStateException when the system is in an unstable state
     * @throws IOException when the operation didn't succeed
     * @aribaapi ariba
     */
    public void writeStringTable (String stringTableName, Locale locale, Map updates)
    throws IOException, BadStateException
    {
        throw new UnsupportedOperationException(
            "writeStringTable is not supported on this implementation");
    }

    /**
     * Updates the designated resource file with contents of <code>inputStream</code>
     * <p>
     * <b>Important notes:</b> <ul>
     * <li> The caller is responsible for ensuring that there is
     * global synchronization on the file (using global locking, for instance.)
     * <li> <code>inputStream</code> is not closed by this method (though it
     *      is exhausted). It is responsibility of the client to close the
     *      stream.
     * </ul>
     *
     * @param stringTableName the name of the resource table to update
     * @param locale the locale of the resources
     * @param inputStream the new file that will replace the stringTable
     *        identified by <code>stringTableName</code>
     * @throws BadStateException when the system is in an unstable state
     * @throws IOException when the operation didn't succeed
     * @aribaapi ariba
     */
    public void writeStringTable (
            String stringTableName,
            Locale locale,
            InputStream inputStream
    )
    throws IOException, BadStateException
    {
        throw new UnsupportedOperationException(
            "writeStringTable is not supported on this implementation");
    }

    /**
     * Updates the designated resource file's by deleting the resources
     * pointed by the keys stored in <code>listOfKeysToUnset</code>.
     *
     * @param listKeysToUnset the list of keys to delete from the resource file
     * @param stringTableName the name of the resource file
     * @param locale the locale of the resource to delete
     * @throws BadStateException when the system reached an unrecoverable state
     * @throws IOException when the operation didn't succeed
     * @aribaapi ariba
    */
    public void unsetLocalizedString (List listKeysToUnset,
                                      String stringTableName,
                                      Locale locale)
    throws IOException, BadStateException
    {
        throw new UnsupportedOperationException(
            "unsetLocalizedString is not supported on this implementation");
    }



    /*-----------------------------------------------------------------------
        Image Methods
      -----------------------------------------------------------------------*/

    /**
        Returns a localized image path in the service's locale.

        @param path path
        @return localized image path for service's locale
        @aribaapi private
    */
    public String getLocalizedImagePath (String path)
    {
        return getLocalizedImagePath(path, getLocale());
    }

    /**
        Returns a localized image path in the specified locale.
        The path is relative to the resource URL - meaning there is no leading slash
        in the return value.

        @param path path
        @param locale
        @return localized image path
        @aribaapi private
    */
    public String getLocalizedImagePath (String path, Locale locale)
    {
        String localizedPath = (String)imageMap(locale).get(path);

        if (localizedPath == null) {
            localizedPath = Fmt.S("%s/%s", defaultImagesRoot, path);
        }

        localizedPath = stringWithoutLeadingSlash(localizedPath);

        return localizedPath;
    }

    /**
        Returns an image map localized in the specified locale.  Provides
        caching on top of getImageMap.

        @param locale locale
        @return localized image map
        @aribaapi private
    */
    public Map imageMap (Locale locale)
    {
        synchronized (imageMaps) {
            Map imageMap = (Map)imageMaps.get(locale);

            if (imageMap == null) {
                imageMap = getImageMap(locale);
                imageMaps.put(locale, imageMap);
            }
            return imageMap;
        }
    }

    /**
        Puts the <code>imageMap</code> in the cache under the given
        <code>locale</code>.

        @param imageMap image map
        @param locale locale with which the image map will be associated
        @aribaapi private
    */
    public void cacheImageMap (Map imageMap, Locale locale)
    {
        synchronized (imageMaps) {
            imageMaps.put(locale, imageMap);
        }
    }

    /**
        Resolves an image map localized in the specified locale.  Subclassers
        should override.

        @param locale locale
        @return localized image map
        @aribaapi private
    */
    protected Map getImageMap (Locale locale)
    {
        return MapUtil.map();
    }


    /*-----------------------------------------------------------------------
        Utility Methods
      -----------------------------------------------------------------------*/

    /**
        Returns list of directories to search for the given locale and given default
        locale.

        @param givenLocale
        @param givenDefaultLocale
        @param defaulting toggles between defaulting to relaxed locale or not
        @return list of directories to search for locale
        @aribaapi ariba
    */
    protected List<Locale> getSearchDirsWithGivenDefault (Locale givenLocale,
                                                  Locale givenDefaultLocale,
                                                  boolean defaulting)
    {
        List<Locale> searchDirs = ListUtil.list();
        if (defaulting) {
                // 1) locale of last resort and its relaxed brother
            searchDirs.add(LocaleOfLastResort);
            searchDirs.add(RelaxedLocaleOfLastResort);
                // 2) defaultLocale and its relaxed brethren
            appendSearchLocales(searchDirs, givenDefaultLocale);
                // 3) given locale and its relaxed brethren
            appendSearchLocales(searchDirs, givenLocale);
        }
        else {
            appendSearchLocale(searchDirs, givenLocale);
        }

        return searchDirs;
    }


    /**
        Returns list of directories to search for the given locale.

        @param givenLocale
        @param defaulting toggles between defaulting to relaxed locale or not
        @return list of directories to search for locale
        @aribaapi ariba
    */
    public List<Locale> getSearchDirs (Locale givenLocale, boolean defaulting)
    {
        return getSearchDirsWithGivenDefault(givenLocale,
                                             defaultLocale,
                                             defaulting);
    }

    /**
        e.g., fr_FR_foo is added in general-to-specific order:
        fr, then fr_FR, then fr_FR_foo
    */
    private void appendSearchLocales (List searchDirs,
                                      Locale unrelaxedLocale)
    {
        Locale relaxedLocale = relaxLocale(unrelaxedLocale);
        Locale doublyRelaxedLocale = relaxLocale(relaxedLocale);

        if (doublyRelaxedLocale != null) {
            appendSearchLocale(searchDirs, doublyRelaxedLocale);
        }

        if (relaxedLocale != null) {
            appendSearchLocale(searchDirs, relaxedLocale);
        }

        appendSearchLocale(searchDirs, unrelaxedLocale);
    }

    /**
        appends a locale while avoiding duplicate entries
    */
    private void appendSearchLocale (List searchDirs, Locale locale)
    {
            // every locale needs to be appended in order to maintain correctness
        searchDirs.add(locale);
    }


    /**
        "Relaxes" the specificity of the specified locale.  Meaning the least
        significant component is dropped (fr_CA -> fr).

        @param locale locale to relax, is allowed to be <code>null</code> (in
               which case <code>null</code> is returned)
        @return relaxed locale
        @aribaapi private
    */
    protected final Locale relaxLocale (Locale locale)
    {
        return locale != null ? I18NUtil.getParent(locale) : null;
    }

    /**
        Returns the system directory.

        @return system directory
        @aribaapi private
    */
    protected String getSystemDirectory ()
    {
        return SystemUtil.SystemDirectoryString;
    }

    /**
        Gets the configuration directory.

        @return configuration directory
        @aribaapi private
    */
    protected String getConfigDirectory ()
    {
        return SystemUtil.ConfigDirectoryString;
    }


    /**
        Traverses an object (if it is an Map, List, or String),
        and for all strings that start with '@', replaces it with a
        localized string for the specified locale, using the method
        {@link #getLocalizedCompositeKey}.  Will return a copy of the
        appropriate sub-structure if any localization is done--this can be
        understood as the fact that <code>object</code> is not modified.<p>

        If the object is not a {@link Map}, {@link List} or
        a {@link String}, it is not altered and simply returned. <p>

        @param object the object to traverse and localize
        @param locale the <code>Locale</code> for which the localization should be
               done
        @return the localized version of <code>object</code>

        @aribaapi ariba
    */
    public Object descendAndLocalizeObject (
            Object object,
            Locale locale,
            Localizer localizer
    )
    {
        if (object instanceof String) {
            return (localizer == null)
                    ? getLocalizedCompositeKey((String)object, locale, true, true, false)
                    : localizer.getLocalizedCompositeKey((String)object, locale);
        }
        else if (object instanceof Map) {
            Map table = (Map)object;
                // Clone only if necessary
            boolean hasCloned = false;

            Iterator e = table.keySet().iterator();
            while (e.hasNext()) {
                Object key = e.next();
                Object value = table.get(key);
                Object newValue = descendAndLocalizeObject(value, locale, localizer);

                if (newValue != value) {
                    if (!hasCloned) {
                        hasCloned = true;
                        table = MapUtil.cloneMap(table);
                    }
                    table.put(key, newValue);
                }
            }
            return table;
        }
        else if (object instanceof List) {
            List vector = (List)object;
                // Clone only if necessary
            boolean hasCloned = false;

            for (int i = 0, count = vector.size(); i < count; i++) {
                Object value = vector.get(i);
                Object newValue = descendAndLocalizeObject(value, locale, localizer);

                if (value != newValue) {
                    if (!hasCloned) {
                        hasCloned = true;
                        vector = ListUtil.cloneList(vector);
                    }
                    vector.set(i, newValue);
                }
            }
            return vector;
        }
        return object;
    }

    /**
        Traverses an object (if it is an Map, List, or String),
        and for all strings that start with '@', replaces it with a
        localized string for the specified locale, using the method
        {@link #getLocalizedCompositeKey}.  Will return a copy of the
        appropriate sub-structure if any localization is done--this can be
        understood as the fact that <code>object</code> is not modified.<p>

        If the object is not a {@link Map}, {@link List} or
        a {@link String}, it is not altered and simply returned. <p>

        @param object the object to traverse and localize
        @param locale the <code>Locale</code> for which the localization should be
               done
        @return the localized version of <code>object</code>

        @aribaapi ariba
    */
    public Object descendAndLocalizeObject (Object object, Locale locale)
    {
        return descendAndLocalizeObject(object, locale, null);
    }

    /**
        Returns a string version of the given <code>obj</code> and guarantees
        that the string is terminated in a slash if it is not empty or null.

        @param obj Object to convert into a string
        @return A string version of the object with a terminating slash

        @aribaapi documented
    */
    protected static String stringWithTrailingSlash (Object obj)
    {
        if (obj == null) {
            return Constants.EmptyString;
        }

        String objStr = obj.toString();
        if (StringUtil.nullOrEmptyOrBlankString(objStr)) {
            return Constants.EmptyString;
        }

        if (!objStr.endsWith("/")) {
            objStr = Fmt.S("%s/", objStr);
        }

        return objStr;
    }

    /**
        Returns a string version of the given <code>obj</code> and guarantees
        that the string is not started with a slash if it is not empty or null.

        @param obj Object to convert into a string
        @return A string version of the object without a leading slash

        @aribaapi ariba
    */
    protected static String stringWithoutLeadingSlash (Object obj)
    {
        if (obj == null) {
            return Constants.EmptyString;
        }

        String objStr = obj.toString();
        if (StringUtil.nullOrEmptyOrBlankString(objStr)) {
            return Constants.EmptyString;
        }

        if (objStr.startsWith("/") || objStr.startsWith("\\")) {
            objStr = objStr.substring(1);
        }

        return objStr;
    }

    /**
        Returns the url string of file for the given locale

        @param filename name of the file
        @param locale locale for the file path
        @return url string of the file for the given locale

        @aribaapi documented
    */
    public String findResourceURL (String filename, Locale locale)
    {
        if (StringUtil.nullOrEmptyOrBlankString(filename) || locale == null) {
            return null;
        }

        String filepath = null;

        List searchDirs = getSearchDirs(locale, true);
        for (int i = searchDirs.size()-1; i > -1; i--) {
            Locale searchLocale = (Locale)searchDirs.get(i);

            filename = filename.replace('\\', '/');

                // check the input filename
            String urlPath = filename;
            if (URLUtil.maybeURLExists(URLUtil.url(urlPath))) {
                filepath = urlPath;
                break;
            }

                // check the docroot/config directory
            urlPath = Fmt.S("%s%s/%s/%s/%s",
                            stringWithTrailingSlash(baseURL),
                            "config",
                            ResourceRoot,
                            searchLocale,
                            stringWithoutLeadingSlash(filename));
            if (URLUtil.maybeURLExists(URLUtil.url(urlPath))) {
                filepath = urlPath;
                break;
            }
                // check the coreserver/config directory
            urlPath = Fmt.S("%s%s/%s/%s",
                            stringWithTrailingSlash(
                                URLUtil.url(SystemUtil.getConfigDirectory())),
                            ResourceRoot,
                            searchLocale,
                            stringWithoutLeadingSlash(filename));
            if (URLUtil.maybeURLExists(URLUtil.url(urlPath))) {
                filepath = urlPath;
                break;
            }

                // check the coreserver/ariba directory
            urlPath = Fmt.S("%s%s/%s/%s",
                            stringWithTrailingSlash(
                                URLUtil.url(SystemUtil.getSystemDirectory())),
                            ResourceRoot,
                            searchLocale,
                            stringWithoutLeadingSlash(filename));
            if (URLUtil.maybeURLExists(URLUtil.url(urlPath))) {
                filepath = urlPath;
                break;
            }

                // check the docroot/ariba directory
            urlPath = Fmt.S("%s%s/%s/%s/%s",
                            stringWithTrailingSlash(baseURL),
                            "ariba",
                            ResourceRoot,
                            searchLocale,
                            stringWithoutLeadingSlash(filename));
            if (URLUtil.maybeURLExists(URLUtil.url(urlPath))) {
                filepath = urlPath;
                break;
            }

                // check the docroot/internal directory
            urlPath = Fmt.S("%s%s/%s/%s/%s",
                            stringWithTrailingSlash(baseURL),
                            "internal",
                            ResourceRoot,
                            searchLocale,
                            stringWithoutLeadingSlash(filename));
            if (URLUtil.maybeURLExists(URLUtil.url(urlPath))) {
                filepath = urlPath;
                break;
            }
        }

        return filepath;
    }

    public StringTableProcessor getDefaultStringTableProcessor ()
    {
        return defaultStringTableProcessor;
    }

    //--------------------------------------------------------------------------
    // nested class

    /**
        CSVReader that knows how to read a csv into a string table.
        @aribaapi private
    */
    protected static class CSVStringTableConsumer implements StringCSVConsumer
    {
        /**
            Strings

            @aribaapi private
        */
        public Map strings = MapUtil.map();

        /**
            Url for the csv resource.
        */
        private String fileName;
        private boolean displayWarning;

        /**
            Constructor

            @param fileName the file for the csv resource, or at least the logical
                   name of the CSV resource content
            @param displayWarning unused

            @aribaapi private
        */
        public CSVStringTableConsumer (
                String fileName,
                boolean displayWarning
        )
        {
            this.fileName = fileName;
            this.displayWarning = displayWarning;
        }

        /**
            Constructor

            @param url the url for the csv resource, the caller must guarantee that
                   this url exists.
            @param displayWarning unused

            @aribaapi private
        */
        public CSVStringTableConsumer (URL url, boolean displayWarning)
        {
            this(url.getFile(), displayWarning);
        }

        /**
            Verifies that there are at least 2 columns in the line, and puts them
            in the currentStrings table which is assumes was set up by
            loadStringTableCSV.

            @see #loadStringsFromCSV

            @param path         path
            @param lineNumber   line number
            @param line         List representing a line; each element represents
            a column of the line
            @aribaapi private
        */
        public void consumeLineOfTokens (
                String path,
                int lineNumber,
                List line
        )
        {
            if (line.size() < 2) {
                IOException exception = new IOException(
                        Fmt.S("%s:%s requires at least 2 columns it has %s columns: %s",
                              path,
                              Constants.getInteger(lineNumber),
                              Constants.getInteger(line.size()),
                              line));
                throw new TunnelingException(exception);
            }

            // Ignore empty tokens for comments
            // (e.g. ",, THESE ARE THE APPROVAL TAB BUTTONS")
            if (((String)ListUtil.firstElement(line)).length() == 0) {
                return;
            }

            String key = (String)ListUtil.firstElement(line);

            // check for duplicate entry and warn if duplicate exists
            if (strings.get(key) != null && displayWarning) {
                Log.util.warning(7012, fileName, key, Constants.getInteger(lineNumber));
            }

            //handle embedded '\n', '\t' and '\r'
            String resourceString = (String)line.get(1);

            if (resourceString.indexOf('\\') != -1) {

                FastStringBuffer buf = new FastStringBuffer();
                // return the delimiters as well as the tokens
                StringTokenizer st = new StringTokenizer(resourceString, "\\", true);

                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (token.charAt(0) == '\\') {
                        /*
                        if this is not the end of the string then
                        get the next token and process.
                        Otherwise, a single final backslash gets dropped.
                        */
                        if (st.hasMoreTokens()) {
                            token = st.nextToken();
                            switch (token.charAt(0)) {
                                case 'n':
                                    buf.append("\n");
                                    token  = token.substring(1, token.length());
                                    break;
                                case 't':
                                    buf.append("\t");
                                    token  = token.substring(1, token.length());
                                    break;
                                case 'r':
                                    buf.append("\r");
                                    token  = token.substring(1, token.length());
                                    break;
                                default:
                            }
                            buf.append(token);
                        }
                    }
                    else {
                        // no escaped character
                        buf.append(token);
                    }
                }
                resourceString = buf.toString();
                strings.put(key, buf.toString());
            }

            resourceString = ResourceService.prepareResourceString(resourceString);
            strings.put(key, resourceString);
        }

        public Map getStrings ()
        {
            return strings;
        }
    }
}

class DefaultStringCSVProcessor implements StringTableProcessor
{
    public StringCSVConsumer createStringCSVConsumer (URL url,
                                                      boolean displayWarning)
    {
        return new ResourceService.CSVStringTableConsumer(url, displayWarning);
    }

    public void mergeStringTables (Map dest, Map source)
    {
        MapUtil.mergeMapIntoMap(dest, source);
    }

    public Object defaultValueIfNullStringTable (Object key)
    {
        return key;
    }
}

