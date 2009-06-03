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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVFormatterFactory.java#29 $
*/
package ariba.ui.validation;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWFormatter;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.widgets.FormatterFactory;
import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Gridtable;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.formatter.DoubleFormatter;
import ariba.util.formatter.LongFormatter;
import ariba.util.formatter.Formatter;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.List;

/**
    Provides a default (localized) set of formatters accessible via "$formatters.{name}" bindings
    off of AWComponents.
    <p>
    Bundled formatters include:
    <ol>
        <li>boolean</li>
        <li>integer</li>
        <li>long</li>
        <li>double</li>
        <li>bigDecimal</li>
        <li>money</li>
        <li>shortDate</li>
        <li>longDate</li>
        <li>dateTime</li>
        <li>longDateTime</li>
        <li>shortAbsoluteDate</li>
        <li>longAbsoluteDate</li>
        <li>absoluteDateTime</li>
        <li>longAbsoluteDateTime</li>
        <li>duration</li>
        <li>timeMillis</li>
        <li>hiddenPassword</li>
    </ol>
 */
public final class AWVFormatterFactory
{
    public static final String RequiredStringFormatterKey = "requiredString";
    public static final String IntegerFormatterKey = "integer";
    public static final String LongFormatterKey = "long";
    public static final String DoubleFormatterKey = "double";
    public static final String MoneyFormatterKey = "money";
    public static final String ShortDateFormatterKey = "shortDate";
    public static final String LongDateFormatterKey = "longDate";
    public static final String ShortAbsoluteDateFormatterKey = "shortAbsoluteDate";
    public static final String LongAbsoluteDateFormatterKey = "longAbsoluteDate";
    public static final String BigDecimalFormatterKey = "bigDecimal";
    public static final String DateTimeFormatterKey = "dateTime";
    public static final String AbsoluteDateTimeFormatterKey = "absoluteDateTime";
    public static final String LongDateTimeFormatterKey = "longDateTime";
    public static final String LongAbsoluteDateTimeFormatterKey = "longAbsoluteDateTime";
    public static final String BooleanFormatterKey = "boolean";
    public static final String DurationFormatterKey = "duration";
    public static final String TimeMillisFormatterKey = "timeMillis";
    public static final String HiddenPassword = "hiddenPassword";
    public static final String Identifier = "identifier";
    public static final String XMLFormattersKey = "xml";
    public static final String BlankNullFormattersKey = "blankNull";

    public static final Object CanonicalDateFormatter;
    public static final Object JavaFormatDateFormatter;
    public static final Object CanonicalNumberFormatter;

    public static final String CanonicalDateFormatString = "MM/dd/yy";
    public static final String CanonicalDateTimeFormatString = "EEE MMM dd HH:mm:ss zzz yyyy";

    private static int CanonicalFormatter = 0;
    private static int ObjectFormatter = 1;

    static String DefaultCurrencyName = "USD";

    public interface FormatterProvider
    {
        void populateFormatters (Map<String, Object> formatters, Locale locale, TimeZone timeZone);
    }

    static List<FormatterProvider> _Providers = ListUtil.list();

    public static void registerProvider (FormatterProvider provider)
    {
        _Providers.add(provider);
    }

    static {
        TimeZone defaultTimeZone = DateFormatter.getDefaultTimeZone();
        Locale defaultLocale = DateFormatter.getDefaultLocale();
        JavaFormatDateFormatter =
                new AWVDateFormatter(CanonicalDateTimeFormatString, defaultLocale, defaultTimeZone);
        CanonicalDateFormatter = new AWVDateFormatter(CanonicalDateFormatString, defaultLocale, defaultTimeZone);
        CanonicalNumberFormatter = new AWVBigDecimalFormatter("####.##", 0, defaultLocale);

        // Register ours as the first provider (so that other sources can override ours)
        registerProvider(new FormatterProvider() {
            public void populateFormatters (Map<String, Object> formatters, Locale locale, TimeZone timeZone)
            {
                populate(formatters, locale, timeZone);
            }
        });
    }

    private static boolean _DidInit = false;
    private static Gridtable _formattersByLocaleAndTimezone = new Gridtable();
    private static Map _formatterFactories = MapUtil.map();

    public static void init ()
    {
        if (!_DidInit) {
            // force registration of class extension
            AWVFormatterAccess.init();
            _DidInit = true;
        }
    }

    protected static final String PageKey = "AWVFs";
    public static Map formattersForComponent (AWComponent component)
    {
        AWPage page = component.page();
        Map formatters = (Map) page.get(PageKey);
        if (formatters == null) {
            formatters = formattersForSession(component.session());
            assignFormattersForPage(page, formatters);
        }
        return formatters;
    }

    public static String getDefaultCurrencyName ()
    {
        return DefaultCurrencyName;
    }

    public static void setDefaultCurrencyName (String defaultCurrencyName)
    {
        DefaultCurrencyName = defaultCurrencyName;
    }

    // called directly by sessionless renderers to initialize with appropriate timezone / locale
    public static void assignFormattersForPage (AWPage page, Map formatters)
    {
        page.put(PageKey, formatters);
    }

    public static Map formattersForSession (AWSession session)
    {
        Locale locale = session.preferredLocale();
        TimeZone timeZone = session.clientTimeZone();
        if (timeZone == null) {
            timeZone = DateFormatter.getDefaultTimeZone();
        }
        return formattersForLocaleTimeZone(locale, timeZone);
    }

    public static Map formattersForLocaleTimeZone (Locale locale, TimeZone timeZone)
    {
        Map formatters = (Map)_formattersByLocaleAndTimezone.get(locale, timeZone);
        if (formatters ==  null) {
            synchronized (_formattersByLocaleAndTimezone) {
                formatters = (Map)_formattersByLocaleAndTimezone.get(locale, timeZone);
                if (formatters == null) {
                    formatters = createFormattersForSession(locale, timeZone);
                    _formattersByLocaleAndTimezone.put(locale, timeZone, formatters);
                }
            }
        }
        return formatters;
    }

    private static TimeZone getTimeZone (AWSession session)
    {
        TimeZone timeZone = session.clientTimeZone();
        if (timeZone == null) {
            timeZone = DateFormatter.getDefaultTimeZone();
        }
        return timeZone;
    }

    public static Map createFormattersForSession (AWSession session)
    {
        Locale locale = session.preferredLocale();
        TimeZone timeZone = getTimeZone(session);
        return createFormattersForSession(locale, timeZone);
    }

    public static void registerFormatterFactoryForType (String type,
                                                        FormatterFactory factory)
    {
        synchronized (_formatterFactories) {
            _formatterFactories.put(type, factory);
        }
    }

    public static FormatterFactory getFormatterFactoryForType (String type)
    {
        FormatterFactory factory = null;
        synchronized (_formatterFactories) {
            factory = (FormatterFactory)_formatterFactories.get(type);
        }
        return factory;
    }

    public static Map createFormattersForSession (Locale locale, TimeZone timeZone)
    {
        Map<String, Object> formatters = MapUtil.map();
        for (FormatterProvider provider : _Providers) {
            provider.populateFormatters(formatters, locale, timeZone);
        }
        return formatters;
    }

    static Map populate (Map<String, Object>formatters, Locale locale, TimeZone timeZone)
    {
        // $formatters.xml.<xxx> returns a formatter that will convert from a canonical string
        // represenation of a date or number, to whatever <xxx> does..
        formatters.put(RequiredStringFormatterKey, new NonBlankString());
        Map xml = MapUtil.map();

        // Currency-specific big decimals formatters
        // Used as: $formatters.currency.USD
        formatters.put("currency", BigDecimalMoneyFormatter.formattersByCurrency());
        formatters.put("currencyLong", BigDecimalMoneyFormatter.formattersByCurrencyWithSuffix());

        // Because of component dependency, we cannot reference the money formatter
        // directly.  Instead, it is registered dynamically.  If none has been register,
        // we fallback to old questionable formatting.
        FormatterFactory moneyFactory = getFormatterFactoryForType(MoneyFormatterKey);
        if (moneyFactory != null) {
            Object canonicalFmt = moneyFactory.getCanonicalFormatterForType(locale);
            Object objectFmt = moneyFactory.getObjectFormatterForType(locale);
            formatters.put(MoneyFormatterKey, objectFmt);
            xml.put(MoneyFormatterKey, new PipedFormatter(canonicalFmt, objectFmt));
        }
        else {
            // Object money = new AWVBigDecimalFormatter("$#,###.##", 2, locale);
            Object money = BigDecimalMoneyFormatter.formattersByCurrency().get(DefaultCurrencyName);
            formatters.put(MoneyFormatterKey, money);
            xml.put(MoneyFormatterKey, new PipedFormatter(CanonicalNumberFormatter, money));
        }

        Object bigDecimal = new AWVBigDecimalFormatter("#,###.##", 2, locale);
        formatters.put(BigDecimalFormatterKey, bigDecimal);
        xml.put(BigDecimalFormatterKey, new PipedFormatter(CanonicalNumberFormatter, bigDecimal));
        Object dbl = new DoubleFormatter() {
            protected String formatObject (Object object, Locale locale)
            {
                Assert.that(object instanceof Double, "invalid type");
                return getStringValue((Double)object, 3, 3, "#,###.000", locale);
            }
        };
        
        formatters.put(DoubleFormatterKey, dbl);
        Object dblXML = new AWVBigDecimalFormatter("#,###.##", 0, locale);
        xml.put(DoubleFormatterKey, new PipedFormatter(CanonicalNumberFormatter, dblXML));

        Object integer = new IntegerFormatter();
        formatters.put(IntegerFormatterKey, integer);
        Object intXML = new AWVBigDecimalFormatter("#,###", 0, locale);
        xml.put(IntegerFormatterKey, new PipedFormatter(CanonicalNumberFormatter, intXML));

        Object longFormatter = new LongFormatter();
        formatters.put(LongFormatterKey, longFormatter);
        Object longXML = new AWVBigDecimalFormatter("#,###", 0, locale);
        xml.put(LongFormatterKey, new PipedFormatter(CanonicalNumberFormatter, longXML));

        // Todo:  Should use either CanonicalDateFormatter, or JavaFormatDateFormatter, but not both!
        Object shortDate = new AWVShortDateFormatter(locale, timeZone);
        formatters.put(ShortDateFormatterKey, shortDate);
        xml.put(ShortDateFormatterKey, new PipedFormatter(CanonicalDateFormatter, shortDate));

        AWVShortDateFormatter shortAbsoluteDate = new AWVShortDateFormatter(locale, timeZone);
        shortAbsoluteDate.setCalendarDate(false);
        formatters.put(ShortAbsoluteDateFormatterKey, shortAbsoluteDate);
        xml.put(ShortAbsoluteDateFormatterKey, new PipedFormatter(CanonicalDateFormatter, shortAbsoluteDate));

        Object longDate = new AWVLongDateFormatter(locale, timeZone);
        formatters.put(LongDateFormatterKey, longDate);
        xml.put(LongDateFormatterKey, new PipedFormatter(CanonicalDateFormatter, longDate));

        AWVLongDateFormatter longAbsoluteDate = new AWVLongDateFormatter(locale, timeZone);
        longAbsoluteDate.setCalendarDate(false);
        formatters.put(LongAbsoluteDateFormatterKey, longAbsoluteDate);
        xml.put(LongAbsoluteDateFormatterKey, new PipedFormatter(CanonicalDateFormatter, longAbsoluteDate));

        Object dateTime = new AWVConciseDateTimeFormatter(locale, timeZone);
        formatters.put(DateTimeFormatterKey, dateTime);
        xml.put(DateTimeFormatterKey, new PipedFormatter(JavaFormatDateFormatter, dateTime));

        AWVConciseDateTimeFormatter absoluteDateTime = new AWVConciseDateTimeFormatter(locale, timeZone);
        absoluteDateTime.setCalendarDate(false);
        formatters.put(AbsoluteDateTimeFormatterKey, absoluteDateTime);
        xml.put(AbsoluteDateTimeFormatterKey, new PipedFormatter(JavaFormatDateFormatter, absoluteDateTime));

        Object longDateTime = new AWVLongDateTimeFormatter(locale, timeZone);
        formatters.put(LongDateTimeFormatterKey, longDateTime);
        xml.put(LongDateTimeFormatterKey, new PipedFormatter(JavaFormatDateFormatter, longDateTime));

        AWVLongDateTimeFormatter longAbsoluteDateTime = new AWVLongDateTimeFormatter(locale, timeZone);
        longAbsoluteDateTime.setCalendarDate(false);
        formatters.put(LongAbsoluteDateTimeFormatterKey, longAbsoluteDateTime);
        xml.put(LongAbsoluteDateTimeFormatterKey, new PipedFormatter(JavaFormatDateFormatter, longAbsoluteDateTime));

        Object timeMillis = new AWVTimeMillisFormatter(locale, timeZone);
        formatters.put(TimeMillisFormatterKey, timeMillis);
        xml.put(TimeMillisFormatterKey, new PipedFormatter(timeMillis, timeMillis));

        Object durationFormatter = new TimeDurationFormatter();
        formatters.put(DurationFormatterKey, durationFormatter);
        xml.put(DurationFormatterKey, new PipedFormatter(CanonicalNumberFormatter, durationFormatter));

        Object booleanFormatter = new CanonicalBooleanForamatter();
        formatters.put(BooleanFormatterKey, booleanFormatter);

        formatters.put(HiddenPassword, new HiddenPasswordFormatter());

        formatters.put(Identifier, new AWVIdentifierFormatter(locale));

        Map blankNull = MapUtil.map();
        for (Map.Entry<String, Object> e : formatters.entrySet()) {
            blankNull.put(e.getKey(), new BlankNullChainedFormatter(e.getValue()));
        }

        formatters.put(BlankNullFormattersKey, blankNull);
        formatters.put(XMLFormattersKey, xml);

        return formatters;
    }

    public static String formattedValue (String formatterKey, Object value, AWSession session)
    {
        Object formatter = formattersForSession(session).get(formatterKey);
        return AWFormatting.get(formatter).format(formatter, value);
    }

    static class CanonicalBooleanForamatter extends AWFormatter
    {
        public Object parseObject (String stringToParse) throws ParseException
        {
            return stringToParse.equalsIgnoreCase("true") ? Boolean.TRUE : Boolean.FALSE;
        }

        public String format (Object objectToFormat)
        {
            return ((Boolean)objectToFormat).booleanValue() ? "true" : "false";
        }
    }

    static public final class NonBlankString extends Formatter
    {
        public NonBlankString ()
        {
            super();
        }

        protected String formatObject (Object object, Locale locale)
        {
            return object.toString();
        }

        protected Object parseString (String stringToParse, Locale locale) throws ParseException
        {
            if (StringUtil.nullOrEmptyOrBlankString(stringToParse)) {

                throw new ParseException(AWBaseObject.localizedJavaString(AWVFormatterFactory.class.getName(), 1,
                        "Value required" /*  */, AWConcreteApplication.SharedInstance.resourceManager(locale)), 0);
            }
            return stringToParse;
        }

        public Object getValue (Object object, Locale locale)
        {
            return object;
        }

        protected int compareObjects (Object o1, Object o2, Locale locale)
        {
            return (o1 instanceof Comparable && o2 instanceof Comparable)
                    ? ((Comparable)o1).compareTo(o2)
                    : 0;
        }
    }

    static public final class BlankNullChainedFormatter extends AWFormatter
    {
        /* AWFormatting */ Object formatter;

        public BlankNullChainedFormatter (Object otherFormatter)
        {
            formatter = otherFormatter;
        }

        public String format (Object value)
        {
            return (value == null) ? "" : AWFormatting.get(formatter).format(formatter, value);
        }

        public Object parseObject (String stringToParse) throws ParseException
        {
            return (StringUtil.nullOrEmptyOrBlankString(stringToParse))
                ? null
                : AWFormatting.get(formatter).parseObject(formatter,  stringToParse);
        }
    }

    static public final class HiddenPasswordFormatter extends AWFormatter
    {
        public String format (Object value)
        {
            if (value == null) return null;
            int count = ((String)value).length();
            FastStringBuffer buf = new FastStringBuffer(count);
            while (count-- > 0) {
                buf.append("*");
            }
            return buf.toString();
        }

        public Object parseObject (String stringToParse) throws ParseException
        {
            return stringToParse;
        }
    }

    /**
        Covert time in seconds to format like 1:23:22.01
     */
    static public final class TimeDurationFormatter extends AWFormatter
    {
        public String format (Object value)
        {
            if (value == null) return null;
            Assert.that (value instanceof Number, "Incorrect input type: %s", value.getClass());

            int time = (int)(((Number)value).doubleValue() *1000);
            FastStringBuffer buf = new FastStringBuffer(11);
            int millis = time % 1000;
            int seconds = (time/1000) % 60;
            int minutes = (time/60000) % 60;
            int hours = (time/3600000) % 24;
            if (hours != 0) append(buf, hours, ":");
            append(buf, minutes, ":");
            append(buf, seconds, (millis != 0) ? "." : null);
            if (millis != 0) append(buf, millis, null);
            return buf.toString();
        }

        void append(FastStringBuffer buf, int comp, String sep) {
            String s = Integer.toString(comp);
            if (s.length() < 2) buf.append("0");
            buf.append(s);
            if (sep != null) buf.append(sep);
        }

        public Object parseObject (String stringToParse) throws ParseException
        {
            Assert.that(false, "This formatter doesn not support input conversion");
            return null;
        }
    }
}
