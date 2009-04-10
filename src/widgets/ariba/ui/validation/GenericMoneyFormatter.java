/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/GenericMoneyFormatter.java#1 $
*/

package ariba.ui.validation;

import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import java.util.Map;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.ClassUtil;

import java.util.List;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.formatter.DecimalParseInfo;
import ariba.util.formatter.Formatter;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.i18n.LocaleSupport;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.StringTokenizer;

/**
    Responsible for formatting, parsing, and comparing money values.

    This class will format money objects using a special locale-specific
    format string.  This string is of the form:

       {1}#,##0.## {2}

    where '{1}' and '{2}' are tokens for the currency prefix and suffix.  The
    method will replace these tokens with the appropriate values given the
    currency and then pass the remaining format string to the number
    formatter.

    Parsing occurs in a similar fashion where the MoneyFormatter will use the
    format string (pattern string) to parse a string.

    @aribaapi documented
*/
public class GenericMoneyFormatter extends Formatter
{
    /**
        Class Extension to adapt MoneyFormatter to various Money implementations.
        Provides both accessory (getAmount, getCurrency) and factory (create) capabilities.
     */
    abstract static class MoneyAdapter extends ClassExtension
    {
        /**
            Specifies that the currency's default precision should be
            used for rounding.
        */
        public static final int CurrencyPrecision = -100;

        /**
            Specifies that maximum precision should be used for rounding.
        */
        public static final int MaxPrecision = -101;

        static ClassExtensionRegistry _Registry = new ClassExtensionRegistry();

        public static MoneyAdapter registerClassExtension(Class cls, MoneyAdapter extension)
        {
            return (MoneyAdapter)_Registry.registerClassExtension(cls, extension);
        }

        public static MoneyAdapter get (Object target)
        {
            return (MoneyAdapter)_Registry.get(target);
        }

        public static MoneyAdapter get (Class targetClass)
        {
            return (MoneyAdapter)_Registry.get(targetClass);
        }

        public boolean isInstance (Object obj)
        {
            return ClassUtil.instanceOf(obj.getClass(), forClass);
        }

        public abstract BigDecimal getAmount (Object target);

        public abstract Object create (BigDecimal amount, Object currency);

        public Object getCurrency (Object target)
        {
            // formatter should use default
            return null;
        }

        public abstract int getSign (Object target);

        public abstract Object negate (Object target);

        public Object convertToCurrency (Object money, Object currency)
        {
            return money;
        }

        public abstract BigDecimal convertAmount (Object money, Object toCurrency,
                                                  Date date, int      precision);

        public int moneyScale ()
        {
            return 28;
        }

        public int moneyPrecision ()
        {
            return 10;
        }

        public abstract CurrencyAdapter getCurrencyAdapter ();
    }


    /**
        ClassExtension to adapt MoneyFormatter to various Currency implementations.
     */
    abstract static class CurrencyAdapter extends ClassExtension
    {
        static ClassExtensionRegistry _Registry = new ClassExtensionRegistry();

        public static CurrencyAdapter registerClassExtension(Class cls, CurrencyAdapter extension)
        {
            return (CurrencyAdapter)_Registry.registerClassExtension(cls, extension);
        }

        public static CurrencyAdapter get (Object target)
        {
            return (CurrencyAdapter)_Registry.get(target);
        }

        public static CurrencyAdapter get (Class targetClass)
        {
            return (CurrencyAdapter)_Registry.get(targetClass);
        }

        public abstract String getPrefix (Object currency);
        public abstract String getSuffix (Object currency);
        public abstract boolean isEuro (Object currency);
        public abstract int getPrecision (Object currency);

        public abstract List getCurrencyGivenPrefixAndSuffix( String prefix, String suffix);
        public abstract Object getCurrency(String name);
    }


    public interface MoneyConverter
    {
        /**
            Converts a money into another currency.

            @param toCurrency   The currency to convert to.
            @param money        The money to convert.
            @return The converted money
         */
        public Object convertToCurrency (Object toCurrency, Object money);
    }

    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        The Euro currency prefix symbol

        @aribaapi documented
    */
    public static final String EuroSymbol = "\u20ac";

    /**
        Japanese Currency prefix used in Japanese Environment
        @aribaapi private
    */
    protected static final String Symbol_backslash = "\\";

    /**
        ISO suffix and unique name for the Euro currency

        @aribaapi documented
    */
    public static final String EuroUniqueName = "EUR";

    /**
        Japanese Currency UniqueName
        @aribaapi private
    */
    protected static final String JapaneseCurrencyName = "JPY";
    /**
        Korean Currency UniqueName
        @aribaapi private
    */
    protected static final String KoreanCurrencyName = "KRW";
    /**
        Korean Currency Symbol - won sign
        @aribaapi private
    */
    protected static final String KoreanCurrencySymbol = "\u20A9";
    protected static final String KoreanCurrencySymbolFullWidth = "\uffe6";

    /**
        localized string resource file
        @aribaapi private
    */
    protected static final String StringTable = "resource.widgets.money";

    /*-----------------------------------------------------------------------
        locale-specific format strings
      -----------------------------------------------------------------------*/
    /**
        @aribaapi private
    */
    protected static final String MoneyPatternKey         = "MoneyPattern";

    /**
        @aribaapi private
    */
    protected static final String MoneyNoSuffixPatternKey = "MoneyNoSuffixPattern";

    /**
        @aribaapi private
    */
    protected static final String MoneyComboPatternKey    = "MoneyComboPattern";
    /**
        @aribaapi private
    */
    protected static final String MoneyParsePatternKey    = "MoneyParsePattern";
    /**
        @aribaapi private
    */
    protected static final String MoneyUserEuroPatternKey = "MoneyUserEuroPattern";

    /**
        @aribaapi private
    */
    private static final String InvalidValueErrorKey = "MoneyTextFieldInvalidValueError";

    /**
        @aribaapi private
    */
    private static final String MoneyStringTooLongErrorKey = "MoneyStringTooLongError";

    /**
        @aribaapi private
    */
    private static final String CurrencyNotFoundErrorKey = "CurrencyNotFoundErrorKey";

    /**
        Parameter for turning on the display of the Euro symbol
        @aribaapi documented
    */
    public static final String ParameterDisplayEuroSymbol =
        "Application.UI.DisplayEuroSymbol";

    public static final int DefaultPrecision = 2;

    /*-----------------------------------------------------------------------
        Static Fields for caching formats
      -----------------------------------------------------------------------*/

    /**
        Format cache used in value methods.  Hash key: locale
        @aribaapi private
    */
    protected static Map valueFormats = MapUtil.map();

    /**
        Format cache used in value methods.  Hash key: locale
        @aribaapi private
    */
    private static Map valueNoSuffixFormats = MapUtil.map();

    /**
        Format cache used in parsing methods.  Hash key: locale
        @aribaapi private
    */
    private static Map parseFormats = MapUtil.map();

    /**
        Format cache for standard currency formats
        @aribaapi private
    */
    private static Map currencyFormats = MapUtil.map();

    private MoneyAdapter    _moneyAdapter;
    private CurrencyAdapter _currencyAdapter;
    private boolean         _appendSuffix;
    private String          _suffix;
    private boolean         _zeroable;
    private boolean         _absoluteValue;
    private int             _precision;
    private Object          _leadCurrency;
    private boolean         _displayInLeadCurrency;
    private MoneyConverter  _moneyConverter;

    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Create a new <code>MoneyFormatter</code>.

        @aribaapi ariba
    */
    public GenericMoneyFormatter (MoneyAdapter moneyAdapter)
    {
        _moneyAdapter = moneyAdapter;
        _currencyAdapter = _moneyAdapter.getCurrencyAdapter();
        _appendSuffix = true;
        _suffix = null;
        _zeroable = false;
        _absoluteValue = false;
        _precision = -1;
        _leadCurrency = null;
        _displayInLeadCurrency = false;
        _moneyConverter = null;
    }


    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns an HTML-escaped, formatted string for this Money object.
        This method is useful for formatting Euro currencies for
        HTML display.

        @param object The <code>Money</code> object to be formatted.
        @return An HTML-escaped, formatted string for this Money object.
        This method is useful for formatting Euro currencies for
        HTML display.

        @aribaapi documented
    */
    public String getEscapedStringValue (Object object)
    {
        String moneyString = getStringValue(object);
        return HTML.escape(moneyString);
    }

    /**
        Returns an HTML-escaped, formatted string for this Money object.  The
        money is forced into the format of that locale.  Currency remains
        the same.  This method is useful for formatting Euro currencies for
        HTML display.

        @param object The <code>Money</code> object to be formatted.
        @param locale The <code>Locale</code> to be used in the
        formatting of <code>object</code>.
        @return An HTML-escaped, formatted string for this Money object.
        This method is useful for formatting Euro currencies for
        HTML display.

        @aribaapi documented
    */
    public String getEscapedStringValue (Object object, Locale locale)
    {
        String moneyString = getStringValue(object, locale);
        return HTML.escape(moneyString);
    }

    /**
        Returns a formatted string for the given Money <code>object</code>.

        @param object The <code>Money</code> object to be formatted.
        @return A formatted string for the given Money <code>object</code>.

        @aribaapi documented
    */
    public String getDefaultStringValue (Object object)
    {
            // get a string value with no alternate currency shown
        return getStringValue(object, getDefaultLocale(), null, true);
    }

    /**
        Returns a formatted string for the given Money <code>object</code>
        Turn on/off the suffix with <code>suffix</code> flag

        @param object The <code>Money</code> object to be formatted.
        @param suffix Determines if the suffix should be in the return
        value
        @return A formatted string for the given Money <code>object</code>.

        @aribaapi documented
    */
    public String getStringValue (Object object, boolean suffix)
    {
            // get a string value with no alternate currency shown
        return getStringValue(object, getDefaultLocale(), null, suffix);
    }

    /**
        Returns a formatted string for this Money object.  The money
        is forced into the format of that locale.  Currency remains
        the same.

        @param object The <code>Money</code> object to be formatted.
        @param locale The <code>Locale</code> to be used in the
        formatting of <code>object</code>.
        @return A formatted string for the given Money <code>object</code>.

        @aribaapi documented
    */
    public String getDefaultStringValue (Object object, Locale locale)
    {
        return getStringValue(object, locale, null, true);
    }

    /**
        Returns a formatted string for this Money object.  The money
        is forced into the format of that locale.  Currency remains
        the same.  Turn on/off the suffix with <code>suffix</code>
        flag

        @param object The <code>Money</code> object to be formatted.
        @param locale The <code>Locale</code> to be used in the
        formatting of <code>object</code>.
        @param suffix Used to determine if the suffix should be in the
        return value
        @return A formatted string for the given Money <code>object</code>.

        @aribaapi documented
    */
    public String getStringValue (Object object, Locale locale, boolean suffix)
    {
        return getStringValue(object, locale, null, suffix);
    }

    /**
        Returns a formatted string for this Money object in the
        default locale.  If <code>euroPrefix</code> is non-null, we
        will always add the prefix string as the Euro currency's
        prefix.  If <code>suffix</code> is true, we will generate the
        suffix.

        @param money The <code>Money</code> object to be formatted.
        @param euroPrefix The prefix to be added as the Euro
        currency's prefix
        @param suffix Used to determine if the suffix should be in the
        return value
        @return A formatted string for the given Money <code>object</code>.

        @aribaapi documented
    */
    public String getStringValue (Object money, String euroPrefix, boolean suffix)
    {
        return getStringValue(money, getDefaultLocale(), euroPrefix, suffix);
    }

    /**
        Returns a formatted string for this Money object in the given
        <code>locale</code>.  If <code>euroPrefix</code> is non-null, we will always add
        the prefix string as the Euro currency's prefix.  If <code>suffix</code> is
        true, we will generate the suffix.

        @param money The <code>Money</code> object to be formatted.
        @param locale The <code>Locale</code> to be used in the
        formatting of <code>object</code>.
        @param euroPrefix The prefix to be added as the Euro
        @param suffix Used to determine if the suffix should be in the
        return value
        @return A formatted string for the given Money <code>object</code>.

        @aribaapi documented
    */
    public String getStringValue (Object money,
                                         Locale locale,
                                         String euroPrefix,
                                         boolean suffix)
    {
        if (money == null) {
            return Constants.EmptyString;
        }

        return getStringValue(money,
                              locale,
                              euroPrefix,
                              suffix,
                              getPrecision(money));
    }

    /**
        Returns a formatted string for this Money object in the given
        <code>locale</code>.  If <code>euroPrefix</code> is non-null, we will always add
        the prefix string as the Euro currency's prefix.  If <code>suffix</code> is
        true, we will generate the suffix.

        @param money The <code>Money</code> object to be formatted.
        @param locale The <code>Locale</code> to be used in the
        formatting of <code>object</code>.
        @param euroPrefix The prefix to be added as the Euro
        @param suffix Used to determine if the suffix should be in the
        return value
        @param precision The precision to be used when rounding the string.
        @return A formatted string for the given Money object.

        @aribaapi private
    */
    public String getStringValue (Object money,
                                         Locale locale,
                                         String euroPrefix,
                                         boolean suffix,
                                         int precision)
    {
        if (money == null) {
            return Constants.EmptyString;
        }
        BigDecimal amount = getAmount(money);
        Object currency  = getCurrency(money);
        return getStringValue(amount, currency, locale, euroPrefix, suffix, precision);
    }

    /**
        Returns a formatted string based on a given amount and currency in the given
        <code>locale</code>.  If <code>euroPrefix</code> is non-null, we will always add
        the prefix string as the Euro currency's prefix.  If <code>suffix</code> is
        true, we will generate the suffix.

        @param amount A <code>BigDecimal</code> object representing a monetary amount
        @param currency A <code>Currency</code> object representing a monetary currency
        @param locale The <code>Locale</code> to be used in the
        formatting of <code>object</code>.
        @param euroPrefix The prefix to be added as the Euro
        @param suffix Used to determine if the suffix should be in the
        return value
        @param precision The precision to be used when rounding the string.
        @return A formatted string for the given Money object.

        @aribaapi private
    */
    public static String getStringValue (BigDecimal amount,
                                         Object currency,
                                         Locale locale,
                                         String euroPrefix,
                                         boolean suffix,
                                         int precision)
    {
        if (amount == null || currency == null) {
            return Constants.EmptyString;
        }

        Assert.that(locale != null, "invalid null Locale");

            // Generate a format string that embeds the currency amount in it.
            // e.g., "{1}1,243.11 {2}"
        DecimalFormat fmt = getDecimalFormat(locale, false, suffix);
        String amtFmt =
            BigDecimalFormatter.getStringValue(amount,
                                            precision,
                                            locale,
                                            fmt);

        /*
            Some locales use the single quote character as a
            separator, but MessageFormat treats this as a special
            character, so we need to double them...
        */
        amtFmt = StringUtil.replaceCharByString(amtFmt, '\'', "''");

            // Since support for the Euro symbol is unreliable on Unix servers
            // and pre-JRE 1.1.7 VMs, we'll only use this symbol if we find
            // we're on the right VM or if the caller explicitly passes us a
            // euro symbol
        String prefix = CurrencyAdapter.get(currency).getPrefix(currency);
        if (CurrencyAdapter.get(currency).isEuro(currency) && displayEuroSymbol() &&
            ((!StringUtil.nullOrEmptyString(euroPrefix)) || supportsEuroSymbol()))
        {
            prefix = euroPrefix;
            if (prefix == null) {
                prefix = EuroSymbol;
            }
        }

            // Determine if we need to generate the suffix
        if (suffix) {
                // Return the currency prefix and suffix inserted into the
                // amount string
            return Fmt.Si(amtFmt, "", prefix, CurrencyAdapter.get(currency).getSuffix(currency));
        }
        else {
            return Fmt.Si(amtFmt, "", prefix, "");
        }
    }

    static Object _DefaultCurrency;

    /**
        Get the currency for the money.
        Could be overridden to return reporting currency or basecurrency

        @param money The <code>Money</code> object containing the
        <code>Currency</code>.
        @return The currency for the specified <code>Money</code>
        object.

        @aribaapi documented
    */
    public Object getCurrency (Object money)
    {
        Object currency = MoneyAdapter.get(money).getCurrency(money);
        return currency == null ? _leadCurrency : currency;
    }

    /**
        Get the amount for the money. Could be overridden to return
        reporting amount or base amount

        @param money The <code>Money</code> object containing the
        amount.
        @return The amount for the specified <code>Money</code>
        object.

        @aribaapi documented
    */
    public static BigDecimal getAmount (Object money)
    {
        return MoneyAdapter.get(money).getAmount(money);
    }

    int getPrecision (Object money)
    {
        Object currency = getCurrency(money);
        return CurrencyAdapter.get(currency).getPrecision(currency);
    }



    /**
        Returns a string value for the money object in two currencies,
        its default currency and the <code>alternate</code> currency.

        @param money The <code>Money</code> object to be formatted.
        @param alternate The second <code>Currency</code> to be used
        in the formatting of the <code>Money</code> object.
        @return A formatted string for the given <code>Money</code>
        object in two currencies.

        @aribaapi documented
    */
    public static String dualStringValue (Object money, Object alternate)
    {
            // Get the dual money display format
        ResourceService rservice = ResourceService.getService();
        String comboPatternKey = rservice.getLocalizedFormat(
            StringTable,
            MoneyComboPatternKey,
            rservice.getLocale());

        String converted = stringValueInCurrency(money, alternate);
        String amt = getStringValue(money);

        return Fmt.Si(comboPatternKey, amt, converted);
    }

    /**
        Returns a string value for the money object in the given currency.

        @param money The <code>Money</code> object to be formatted.
        @param forcedCurrency The <code>Currency</code> to be used in the
        formatting of the <code>Money</code> object.
        @return A formatted string for the given <code>Money</code>
        object in the specified <code>Currency</code>.

        @aribaapi documented
    */
    public static String stringValueInCurrency (Object money, Object forcedCurrency)
    {
        return getStringValue(MoneyAdapter.get(money).convertToCurrency(money, forcedCurrency));
    }

    /**
        Returns a string value for this <code>money</code> converted into
        the currency <code>forced</code>.  Turn on/off the suffix with
        <code>suffix</code> boolean.

        @param money The <code>Money</code> object to be formatted.
        @param forcedCurrency The <code>Currency</code> to be used in the
        formatting of the <code>Money</code> object.
        @param suffix Determines if the suffix should be in the return
        value
        @return A formatted string for the given <code>Money</code>
        object in the specified <code>Currency</code>.

        @aribaapi documented
    */
    public String stringValueInCurrency (Object money, Object forcedCurrency,
                                                boolean suffix)
    {
        return getStringValue(MoneyAdapter.get(money).convertToCurrency(money, forcedCurrency), suffix);
    }

    /**
        Returns a standard currency format object for the given <code>locale</code>.
        @aribaapi private
    */
    private static synchronized DecimalFormat getCurrencyFormat (Locale locale)
    {
        DecimalFormat fmt = (DecimalFormat)currencyFormats.get(locale);
        if (fmt != null) {
            return fmt;
        }

        fmt = (DecimalFormat)NumberFormat.getCurrencyInstance(locale);
        currencyFormats.put(locale, fmt);
        return fmt;
    }

    /**
        Creates a DecimalFormat that is initialized with the localized
        money format pattern.
        @aribaapi private
    */
    private static synchronized DecimalFormat getDecimalFormat (
        Locale locale, boolean forParsing, boolean suffix)
    {
        String patternKey;
        Map formatCache;
        if (forParsing) {
            patternKey = MoneyParsePatternKey;
            formatCache = parseFormats;
        }
        else {
            if (suffix) {
                patternKey = MoneyPatternKey;
                formatCache = valueFormats;
            }
            else {
                patternKey = MoneyNoSuffixPatternKey;
                formatCache = valueNoSuffixFormats;
            }
        }

            // Try to get the format out of the cache first
        DecimalFormat fmt = (DecimalFormat)formatCache.get(locale);
        if (fmt != null) {
            return fmt;
        }

        NumberFormat numFmt = NumberFormat.getCurrencyInstance(locale);
        if (!(numFmt instanceof DecimalFormat)) {
            Assert.that(false, "parseMoney: Cannot cast NumberFormat to DecimalFormat");
            return null;
        }

            // Apply the money number pattern (excludes currency prefix/suffix)
        fmt = (DecimalFormat)numFmt;
        if (patternKey != null) {
            String pattern =
                ResourceService.getService().getLocalizedFormat(StringTable,
                    patternKey,
                    locale);
            fmt.applyPattern(pattern);
        }

        formatCache.put(locale, fmt);

        return fmt;
    }

    /**
        Returns true if the Java VM can display the Euro symbol and
        we're running from the server.

        We only support Sun VM 1.1.7+ and Microsoft VM 1.1.4+ on Win32
        platforms.

        @deprecated Should remove post Buyer8.1 release, as this seems
        to be Applet specific code, it always returns true in server
        environment. Command line clients rendering text would just
        display "?" if the encoding didn't support Euro. At the very
        least, in 8.1+, we should just change to do the isOnClient
        check, and skip the obsolete VM checks.

        @aribaapi private
    */
    private static boolean supportsEuroSymbol ()
    {
        return true;
    }


    /**
        Returns the value of the DisplayEuroSymbol parameter.

        @return The value of the DisplayEuroSymbol parameter.

        @aribaapi documented
    */
    public static boolean displayEuroSymbol ()
    {
        return true;
    }

    /**
        Gets the next token and converts it to an integer.
        If the token isn't a number, it returns zero.
        @aribaapi private
    */
    private static int getNumberToken (StringTokenizer tokens)
    {
        String token = tokens.nextToken();

            // Remove any non-digit suffix from the token
        int i = 0;
        for (; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                break;
            }
        }
        token = token.substring(0, i);

            // Return the integer value of the token
        return IntegerFormatter.getIntValue(token);
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given <code>object</code>.

         @param  object Money object to format as a string
         @param  locale the <code>Locale</code> to use for formatting
         @return        a string representation of Money object
         @aribaapi public
    */
    protected String formatObject (Object object, Locale locale)
    {
        Assert.that(_moneyAdapter.isInstance(object), "invalid type");
        Object money = (Object)object;
        if (_absoluteValue) {
            if (MoneyAdapter.get(money).getSign(money) == -1) {
                money = MoneyAdapter.get(money).negate(money);
            }
        }
        if (_displayInLeadCurrency) {
            if (_moneyConverter != null) {
                money = _moneyConverter.convertToCurrency(_leadCurrency, money);
            }
            else {
                money = MoneyAdapter.get(money).convertToCurrency(money, _leadCurrency);
            }
        }
        return getStringValue(money, locale, null, _appendSuffix, precision(money));
    }

    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Parses the given <code>string</code> to create a new Money object.

         @param  string the string to parse
         @param  locale the <code>Locale</code> to use for parsing
         @return        money object
         @exception     ParseException if the string can't be parsed to create
                        an Money object.
         @aribaapi public

    */
    protected Object parseString (String string, Locale locale)
      throws ParseException
    {
        return parseMoneyString(string,
                          getLeadCurrency(),
                          locale);
    }

    protected Object parseMoneyString (String   moneyString,
                                      Object currency,
                                      Locale   locale)
      throws ParseException
    {
        if (StringUtil.nullOrEmptyOrBlankString(moneyString)) {
                // if we are zeroable, treat empty string as a localized zero
            if (_zeroable) {
                moneyString = BigDecimalFormatter.getStringValue(Constants.ZeroBigDecimal,
                                                           locale);
            }
            else {
                    // treat empty string as null value
                return null;
            }
        }
        return parseMoney(moneyString, currency, locale, _suffix);
    }

    /**
        Returns a new Money object for the given <code>object</code>.

        @param object The <code>Object</code> to be parsed.
        @param locale The <code>Locale</code> to be used in parsing
        <code>object</code>.
        @return A Money object for the given <code>object</code> in
        the specified locale.

        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        return moneyValue(object, locale);
    }

    /**
        Returns a Money object for the given <code>object</code> in
        the default locale.  If <code>object</code> is not a Money
        object, it is converted to a string and parsed as a currency
        string.  If there is a problem parsing the string, or if the
        Money object is null, returns null.

        @param object The <code>Object</code> to be parsed.
        @return A Money object for the given <code>object</code> in
        the default locale.

        @aribaapi documented
    */
    public Object moneyValue (Object object)
    {
        return moneyValue(object, getDefaultLocale());
    }

    /**
        Returns a Money object for the given <code>object</code> in
        the given <code>locale</code>.  If <code>object</code> is not
        a Money object, it is converted to a string and parsed as a
        currency string.  If there is a problem parsing the string, or
        if the Money object is null, returns null.

        @param object The <code>Object</code> to be parsed.
        @param locale The <code>Locale</code> to be used in parsing
        <code>object</code>.
        @return A Money object for the given <code>object</code> in
        the specified locale.

        @aribaapi documented
    */
    public Object moneyValue (Object object, Locale locale)
    {
        return moneyValue(object,
                          getLeadCurrency(),
                          locale);
    }

    /**
        Returns a Money object for the given <code>object</code> in
        the default locale.  If <code>object</code> is not a Money
        object, it is converted to a string and parsed as a currency
        string.  If there is a problem parsing the string, or if the
        Money object is null, returns null.

        If the object doesn't specify a currency, then the
        <code>currency</code> parameter will be used.  If
        <code>currency</code> is also null, then null is returned.

        @param object The <code>Object</code> to be parsed.
        @param currency The <code>Currency</code> to be used in
        parsing <code>object</code>.
        @return A Money object for the given <code>object</code> in
        the default locale.

        @aribaapi documented
    */
    public Object moneyValue (Object object, Object currency)
    {
        return moneyValue(object, currency, getDefaultLocale());
    }

    /**
        Returns a Money object for the given <code>object</code> using
        the given <code>locale</code>.  If <code>locale</code> is
        null, we use the default locale for parsing.

        If <code>object</code> is not a Money object, it is converted
        to a string and parsed as a currency string.  If there is a
        problem parsing the string, or if the Money object is null,
        returns null.

        If the object doesn't specify a currency, then the
        <code>currency</code> parameter will be used.  If
        <code>currency</code> is also null, then null will be
        returned.

        @param object The <code>Object</code> to be parsed.
        @param currency The <code>Currency</code> to be used in
        parsing <code>object</code>.
        @param locale The <code>Locale</code> to be used in parsing
        <code>object</code>.
        @return A Money object for the given <code>object</code> in
        the specified currency and locale.

        @aribaapi documented
    */
    public Object moneyValue (Object object,
                                    Object currency,
                                    Locale locale)
    {
        if (object == null) {
            return null;
        }
        else if (_moneyAdapter.isInstance(object)) {
            return object;
        }
        else if (object instanceof BigDecimal) {
            return _moneyAdapter.create((BigDecimal)object, currency);
        }
        else if (object instanceof Number) {
            return _moneyAdapter.create(new BigDecimal(((Number)object).doubleValue()), currency);
        }
        else {
            try
            {
                if (StringUtil.nullOrEmptyOrBlankString(object.toString())) {
                    return null;
                }
                else {
                    return parseMoney(object.toString(), currency, locale);
                }
            }
            catch (ParseException e) {
                return null;
            }
        }
    }

    /**
        Parses the given <code>string</code> as a money with the appropriate
        currency.

        @param string The <code>String</code> object to be parsed.
        @param currency The <code>Currency</code> object to be used in
        the parsing of the <code>String</code>.
        @return A <code>Money</code> object represented by the
        <code>String</code>.

        @aribaapi documented
    */
    public Object parseMoney (String string, Object currency)
      throws ParseException
    {
        return parseMoney(string, currency, getDefaultLocale(), null);
    }

    /**
        Takes a string and returns a <code>money</code> object in the
        given <code>currency</code>.

        @param moneyString The string to parse into a
        <code>Money</code>.
        @param currency The currency the <code>Money</code> object
        should be in.
        @param locale The <code>Locale</code> to assume when parsing.
        @return a new <code>Money</code> object.

        @aribaapi documented
    */
    public Object parseMoney (String   moneyString,
                                    Object currency,
                                    Locale   locale)
      throws ParseException
    {
        return parseMoney(moneyString, currency, locale, null);
    }


    public Object parseMoney (String   moneyString,
                                    Object currency,
                                    Locale   locale,
                                    String   suffix)
      throws ParseException
    {
        Assert.that(locale != null, "invalid null Locale");

        if (stringIsTooLong(moneyString)) {
            String errorMessage =
               ResourceService.getString(StringTable, MoneyStringTooLongErrorKey, locale);
            throw new ParseException(errorMessage, 0);
        }

        String prefix = null;
        BigDecimal amount = Constants.ZeroBigDecimal;
            // Try to get the suffix from string money by examining the last character
            // The assumption we are using is that if the last character is not a digit
            // or a decimal separator, then it must be currency suffix.
        moneyString = moneyString.trim();
        char lastChar = moneyString.charAt(moneyString.length() - 1);
        DecimalFormat fmt = getDecimalFormat(locale, true, true);
        char decimalSeparator = fmt.getDecimalFormatSymbols().getDecimalSeparator();
        boolean moneyStringHasSuffix = !Character.isDigit(lastChar) &&
                                       lastChar != decimalSeparator;
        DecimalParseInfo info = parseMoneyNumber(moneyString, locale,
                                                 moneyStringHasSuffix);
        amount = info.number;
        prefix = info.prefix;
        if (moneyStringHasSuffix) {
            suffix = info.suffix;
        }

            // figure out the currency based on the prefix/suffix strings
        List results = null;
        Object match = null;
        if ((!StringUtil.nullOrEmptyOrBlankString(prefix)) ||
            (!StringUtil.nullOrEmptyOrBlankString(suffix))) {

            results = _currencyAdapter.getCurrencyGivenPrefixAndSuffix(
                prefix, suffix);
            if (results != null && results.size() == 1) {
                match = ListUtil.firstElement(results);
            }
            else if (results != null && results.contains(currency)) {
                match = currency;
            }
            else {
                String errorMessage =
                   Fmt.Sil(locale, StringTable, CurrencyNotFoundErrorKey, prefix, suffix);
                throw new ParseException(errorMessage, 0);
            }
        }
        else {
            match = currency;
        }

            // We need a valid currency in order to create a money object.
        if (match == null) {
            return null;
        }

        return _moneyAdapter.create(amount, match);
    }

    /**
        Is this string in valid money format?  Use the default locale for
        parsing.

        @param moneyString The <code>String</code> object to be parsed.
        @return true if the <code>moneyString</code> represents a
        <code>Money</code> object in the default <code>Locale</code>.

        @aribaapi documented
    */
    public boolean isMoney (String moneyString)
    {
        return isMoney(moneyString, getDefaultLocale());
    }

    /**
        Is this string a valid money format for this <code>locale</code>?

        @param moneyString The <code>String</code> object to be parsed.
        @param locale The <code>Locale</code> to assume when parsing.
        @return true if the <code>moneyString</code> represents a
        <code>Money</code> object in the specified <code>Locale</code>.

        @aribaapi documented
    */
    public boolean isMoney (String moneyString, Locale locale)
    {
        Assert.that(locale != null, "invalid null Locale");
        try {
            parseMoneyNumber(moneyString, locale);
        }
        catch (ParseException e) {
            return false;
        }

        return true;
    }

    /**
        Returns true if this <code>moneyString</code> has too many digits.

        @param moneyString The string to check.
        @return true if this <code>moneyString</code> has too many digits.

        @aribaapi documented
    */
    public boolean stringIsTooLong (String moneyString)
    {
        return stringIsTooLong(moneyString, false, 0);
    }

    /**
        Checks if the money string has too many digits given the size of our
        money representation.

        @param moneyString The string to check.
        @param insertingAfterDecimal True if the user is inserting
        digits after the decimal point.
        @param digitsToInsert The number of digits are going to be
        inserted into the money string.  This parameter is added to
        the existing digit count to see if we exceed size limits.
        @return true if this <code>moneyString</code> has too many
        digits given the size of the money representation

        @aribaapi documented
    */


    public boolean stringIsTooLong (String  moneyString,
                                    boolean insertingAfterDecimal,
                                    int     digitsToInsert)
    {
        try {
                // Get precision and scale info for money amounts
            DecimalParseInfo info = moneyInfo(moneyString);
            int scale = _currencyAdapter.getPrecision(_leadCurrency);
            int decimalDigits =
                (insertingAfterDecimal) ? (info.decimalDigits + digitsToInsert) : scale;
            int intDigits = info.integerDigits +
                (insertingAfterDecimal ? 0 : digitsToInsert);
            int digits = info.integerDigits + decimalDigits;

            return
                (digits > _moneyAdapter.moneyPrecision()) ||
                (insertingAfterDecimal && (decimalDigits > scale));
        }
        catch (ParseException e) {
            return false;
        }
    }

    /**
        Parses the money string to generate some information about the
        number of digits and the location of the decimal separator
        (among other things).

        @param moneyString The <code>String</code> object to be parsed.
        @return information about the number of digits and the
        location of the decimal separator (among other things).

        @aribaapi documented
    */
    public DecimalParseInfo moneyInfo (String  moneyString)
      throws ParseException
    {
        DecimalFormat fmt = getDecimalFormat(getDefaultLocale(), true, true);
        return BigDecimalFormatter.parseBigDecimal(moneyString, fmt);
    }


    /**
        Parses the specified <code>String</code> in the specified
        <code>Locale</code>.

        @param string The <code>String</code> object to be parsed.
        @param locale The <code>Locale</code> object to be used in
        parsing <code>string</code>.
        @return A <code>DecimalParseInfo</code> object containing the
        Decimal info for the specified <code>String</code>.

        @aribaapi documented
    */
    public DecimalParseInfo parseMoneyNumber (String string, Locale locale)
      throws ParseException
    {
        return parseMoneyNumber(string, locale, true);
    }

    /**
        Parses the specified <code>String</code> in the specified
        <code>Locale</code>.

        @param string The <code>String</code> object to be parsed.
        @param locale The <code>Locale</code> object to be used in
        parsing <code>string</code>.
        @param suffix
        @return A <code>DecimalParseInfo</code> object containing the
        Decimal info for the specified <code>String</code>.

        @aribaapi documented
    */
    public DecimalParseInfo parseMoneyNumber (String string, Locale locale,
                                                     boolean suffix)
      throws ParseException
    {
        string = LocaleSupport.normalizeMoney(string, locale);

        try {
            DecimalFormat fmt = getDecimalFormat(locale, true, suffix);
            return parseBigDecimal(string, fmt, locale);
        }
        catch (ParseException e) {
            DecimalFormat fmt = getCurrencyFormat(locale);
            return BigDecimalFormatter.parseBigDecimal(string, fmt);
        }
    }

    /**
        Call the big decimal formatter to parse the money string.  If the
        we find a Euro symbol prefix, then set the suffix to be EUR if it's
        not already specified and clear the prefix.

        We need to do this because databases do not consistently handle the
        Euro symbol and thus we can't perform currency lookups with the
        Euro as our prefix.

        @aribaapi private
    */
    private DecimalParseInfo parseBigDecimal (String string, DecimalFormat fmt, Locale locale)
      throws ParseException
    {
        DecimalParseInfo info =
            BigDecimalFormatter.parseBigDecimal(string, fmt);

        /*
            If we find the euro prefix, then we'll set the suffix to be
            EUR as long as the user didn't type in another suffix value.

            Note that Netscape with ShiftJIS charsets will render and submit
            the euro symbol as "EUR".  So we detect that case here.
        */
        if (EuroSymbol.equals(info.prefix) ||
            EuroUniqueName.equalsIgnoreCase(info.prefix)) {
            Object euro = _currencyAdapter.getCurrency(EuroUniqueName);
            if (euro != null) {
                String euroSuffix = _currencyAdapter.getSuffix(euro);
                if (StringUtil.nullOrEmptyOrBlankString(info.suffix)) {
                    info.prefix = null;
                    info.suffix = euroSuffix;
                }
                else if (info.suffix.equalsIgnoreCase(euroSuffix)) {
                    info.prefix = null;
                }
            }
        }
            // In Japanese Environment, backslash is used as Yen Currency
        else if (isValidJapaneseCurrencyPrefix(info.prefix, locale)) {
            info.prefix = _currencyAdapter.getPrefix(_currencyAdapter.getCurrency(JapaneseCurrencyName));
        }
          // In Korean Environment, backslash is used as Won Currency
        else if (isValidKoreanCurrencyPrefix(info.prefix, locale)) {
            info.prefix = _currencyAdapter.getPrefix(_currencyAdapter.getCurrency(KoreanCurrencyName));
        }

        return info;
    }

    /**
        In Japanese Environment, backslash is used as Yen Currency
        @aribaapi private
    */
    private static boolean isValidKoreanCurrencyPrefix (String prefix, Locale locale)
    {
        if (locale.getLanguage().equals(Locale.KOREAN.getLanguage())) {
            return KoreanCurrencySymbolFullWidth.equals(prefix) ||
                   KoreanCurrencySymbol.equals(prefix) ||
                   Symbol_backslash.equals(prefix);
        }
        return false;
    }

    /**
        In Korean Environment, backslash is used as Won Currency
        @aribaapi private
    */
    private static boolean isValidJapaneseCurrencyPrefix (String prefix, Locale locale)
    {
        if (locale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
            return Symbol_backslash.equals(prefix);
        }
        return false;
    }




    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares <code>o1</code> and <code>o1</code> for sorting purposes in the given
        <code>locale</code>.

         @param  o1     the first object to compare
         @param  o2     the second object to compare
         @param  locale the <code>Locale</code> to use for comparison
         @return        <code>int</code> value which determines how the two
                        objects should be ordered
         @aribaapi public

    */
    protected int compareObjects (Object o1, Object o2, Locale locale)
    {

        Assert.that(_moneyAdapter.isInstance(o1), "invalid type");
        Assert.that(_moneyAdapter.isInstance(o2), "invalid type");

        return compareMoneys(o1, o2, false, locale);
    }


    /*-----------------------------------------------------------------------
        Quick Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two BigDecimal amounts assuming they are values returned by the
        quickCompareValue() method below.  Arbitrarily sorts null values
        before non-null values, and treats two nulls as equal.

        @aribaapi private
    */
    protected int quickCompareObjects (Object o1, Object o2, Locale locale)
    {
        if (o1 == o2) {
            return 0;
        }
        else if (o1 instanceof BigDecimal && o2 instanceof BigDecimal) {
            return BigDecimalFormatter.compareBigDecimals(
                (BigDecimal)o1, (BigDecimal)o2);
        }
        else if (o1 == null) {
            return -1;
        }
        else if (o2 == null) {
            return 1;
        }
        else {
            // Log.fixme.warning(1145, (o1 != null) ? o1.getClass().getName() : "nulls");
            return 0;
        }
    }

    /**
        Computes a BigDecimal value for a given Money object.  The
        <code>previous</code> argument is used to make sure that all
        amounts returned are expressed in terms of the same currency.

        Note: this should be fixed to always convert into the base
        currency.

        @param money The <code>Money</code> object to be computed.
        @param previous The <code>Money</code> object whose currency
        will be used to compute the return value.
        @return The <code>BigDecimal</code> value for the given
        <code>Money</code> object.

        @aribaapi ariba
    */
    public Object quickCompareValue (Object money, Object previous)
    {
        if (money == null) {
            return null;
        }

        Object currency = null;
        if (previous != null) {
            Assert.that(_moneyAdapter.isInstance(previous), "invalid type");
            currency = getCurrency(previous);
        }

        Assert.that(_moneyAdapter.isInstance(money), "invalid type");
        BigDecimal amount = getAmount(money);
        Object fromCurrency = getCurrency(money);

        if (currency != null && currency != fromCurrency) {
            amount =
                MoneyAdapter.get(money).convertAmount(money, currency, (Date)null, MoneyAdapter.CurrencyPrecision);
        }

        return amount;
    }


    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two money values in the default locale.  Returns a value less
        than, equal to, or greater than zero depending on whether <code>m1</code> is
        less than, equal to, or greater than <code>m2</code>.  The currency of
        <code>m2</code> is converted to that of <code>m1</code> if they're not already the
        same.

        @param m1 The first <code>Money</code> object to be compared
        @param m2 The second <code>Object</code> object to be compared
        @return A value less than, equal to, or greater than zero
        depending on whether <code>m1</code> is less than, equal to,
        or greater than <code>m2</code>.

        @aribaapi documented
    */
    public int compareMoneys (Object m1, Object m2)
    {
        return compareMoneys(m1, m2, false, getDefaultLocale());
    }

    /**
        Compares two money values in the default locale.  Returns a value less
        than, equal to, or greater than zero depending on whether <code>m1</code> is
        less than, equal to, or greater than <code>m2</code>.  The currency of
        <code>m2</code> is converted to that of <code>m1</code> if they're not already the
        same.

        If <code>round</code> is set to true, the money amounts will be rounded to
        the default precision of their currencies before the comparison is
        made.

        @param m1 The first <code>Money</code> object to be compared
        @param m2 The second <code>Money</code> object to be compared
        @param round Determines if the money amounts will be rounded
        before the comparison is made.
        @return A value less than, equal to, or greater than zero
        depending on whether <code>m1</code> is less than, equal to,
        or greater than <code>m2</code>.

        @aribaapi documented
    */
    public int compareMoneys (Object m1, Object m2, boolean round)
    {
        return compareMoneys (m1, m2, round, getDefaultLocale());
    }

    /**
        Compares two money values in the given <code>locale</code>.  Returns a value
        less than, equal to, or greater than zero depending on whether
        <code>m1</code> is less than, equal to, or greater than <code>m2</code>.  The
        currency of <code>m2</code> is converted to that of <code>m1</code> if they're not
        already the same.

        If <code>round</code> is set to true, the money amounts will be rounded to
        the default precision of their currencies before the comparison is
        made.

        The <code>locale</code> parameter is currently ignored.

        @param m1 The first <code>Money</code> object to be compared
        @param m2 The second <code>Money</code> object to be compared
        @param round Determines if the money amounts will be rounded
        before the comparison is made.
        @param locale This parameter is currently ignored.
        @return A value less than, equal to, or greater than zero
        depending on whether <code>m1</code> is less than, equal to,
        or greater than <code>m2</code>.
        Returns zero if the two objects are if both are null.
        Otherwise, arbitrarily returns -1 if the first object is
        null, or 1 if the second is null.

        @aribaapi documented
    */
    public int compareMoneys (Object m1, Object m2, boolean round, Locale locale)
    {
        if (m1 == null) {
            if (m2 == null) {
                return 0;
            }
            else {
                return -1;
            }
        }
        else if (m2 == null) {
            return 1;
        }
        if (!getCurrency(m2).equals(getCurrency(m1))) {
            m2 = MoneyAdapter.get(m2).convertToCurrency(m2, getCurrency(m1));
        }

        BigDecimal amt1 = getAmount(m1);
        BigDecimal amt2 = getAmount(m2);

        if (round) {
            amt1 = BigDecimalFormatter.round(amt1, getPrecision(m1));
            amt2 = BigDecimalFormatter.round(amt2, getPrecision(m2));
        }
        return amt1.compareTo(amt2);
    }


    /**
        Clean up formatter's state
        @aribaapi ariba
    */
    public void sleep ()
    {
        _appendSuffix = true;
        _suffix = null;
        _zeroable = false;
        _absoluteValue = false;
        _precision = -1;
        _leadCurrency = null;
        _displayInLeadCurrency = false;
        _moneyConverter = null;
    }

    /**
        Gets the lead currency associated with the formatter.
        @return lead currency
        @aribaapi ariba
    */
    protected Object getLeadCurrency ()
    {
        return _leadCurrency;
    }

    protected void setLeadCurrency (Object currency)
    {
        _leadCurrency = currency;
    }

    public boolean appendSuffix ()
    {
        return _appendSuffix;
    }

    public void setAppendSuffix (boolean appendSuffix)
    {
        _appendSuffix = appendSuffix;
    }

    /**
        Returns the precision the field viewer should use to render
        money amounts.  Precision is defined as the number of decimal places
        to display.

        We first check for the field property.   If not found,
        then we check the precision of the money object.
        If money is null, we use the lead currency's precision.

        @aribaapi private
    */
    private int precision (Object money)
    {
        if (_precision != -1) {
            return _precision;
        }

        if (money != null) {
            return getPrecision(money);
        }

        if (_leadCurrency != null) {
            return CurrencyAdapter.get(_leadCurrency).getPrecision(_leadCurrency);
        }
        return DefaultPrecision;
    }

}
