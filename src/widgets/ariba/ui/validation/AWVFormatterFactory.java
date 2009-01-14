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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVFormatterFactory.java#22 $
*/
package ariba.ui.validation;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.util.AWFormatter;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.widgets.FormatterFactory;
import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Gridtable;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.formatter.IntegerFormatter;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class AWVFormatterFactory
{
    public static final String RequiredStringFormatterKey = "requiredString";
    public static final String IntegerFormatterKey = "integer";
    public static final String MoneyFormatterKey = "money";
    public static final String ShortDateFormatterKey = "shortDate";
    public static final String LongDateFormatterKey = "longDate";
    public static final String BigDecimalFormatterKey = "bigDecimal";
    public static final String DateTimeFormatterKey = "dateTime";
    public static final String LongDateTimeFormatterKey = "longDateTime";
    public static final String BooleanFormatterKey = "boolean";
    public static final String TimeMillisFormatterKey = "timeMillis";
    public static final String HiddenPassword = "hiddenPassword";
    public static final String Identifier = "identifier";
    public static final String XMLFormattersKey = "xml";

    public static final Object CanonicalDateFormatter;
    public static final Object JavaFormatDateFormatter;
    public static final Object CanonicalNumberFormatter;

    public static final String CanonicalDateFormatString = "MM/dd/yy";
    public static final String CanonicalDateTimeFormatString = "EEE MMM dd HH:mm:ss zzz yyyy";

    private static int CanonicalFormatter = 0;
    private static int ObjectFormatter = 1;

    static {
        TimeZone defaultTimeZone = DateFormatter.getDefaultTimeZone();
        Locale defaultLocale = DateFormatter.getDefaultLocale();
        JavaFormatDateFormatter =
                new AWVDateFormatter(CanonicalDateTimeFormatString, defaultLocale, defaultTimeZone);
        CanonicalDateFormatter = new AWVDateFormatter(CanonicalDateFormatString, defaultLocale, defaultTimeZone);
        CanonicalNumberFormatter = new AWVBigDecimalFormatter("####.##", 0, defaultLocale);
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
        Map formatters = MapUtil.map();

        // $formatters.xml.<xxx> returns a formatter that will convert from a canonical string
        // represenation of a date or number, to whatever <xxx> does..
        Map xml = MapUtil.map();
        formatters.put(XMLFormattersKey, xml);

        formatters.put(RequiredStringFormatterKey, new NonBlankString());

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
            Object money = new AWVBigDecimalFormatter("$#,###.##", 2, locale);
            formatters.put(MoneyFormatterKey, money);
            xml.put(MoneyFormatterKey, new PipedFormatter(CanonicalNumberFormatter, money));
        }

        Object bigDecimal = new AWVBigDecimalFormatter("#,###.##", 2, locale);
        formatters.put(BigDecimalFormatterKey, bigDecimal);
        xml.put(BigDecimalFormatterKey, new PipedFormatter(CanonicalNumberFormatter, bigDecimal));

        Object integer = new IntegerFormatter();
        formatters.put(IntegerFormatterKey, integer);
        Object intXML = new AWVBigDecimalFormatter("#,###", 0, locale);
        xml.put(IntegerFormatterKey, new PipedFormatter(CanonicalNumberFormatter, intXML));

        // Todo:  Should use either CanonicalDateFormatter, or JavaFormatDateFormatter, but not both!
        Object shortDate = new AWVShortDateFormatter(locale, timeZone);
        formatters.put(ShortDateFormatterKey, shortDate);
        xml.put(ShortDateFormatterKey, new PipedFormatter(CanonicalDateFormatter, shortDate));

        Object longDate = new AWVLongDateFormatter(locale, timeZone);
        formatters.put(LongDateFormatterKey, longDate);
        xml.put(LongDateFormatterKey, new PipedFormatter(CanonicalDateFormatter, longDate));

        Object dateTime = new AWVConciseDateTimeFormatter(locale, timeZone);
        formatters.put(DateTimeFormatterKey, dateTime);
        xml.put(DateTimeFormatterKey, new PipedFormatter(JavaFormatDateFormatter, dateTime));

        Object longDateTime = new AWVLongDateTimeFormatter(locale, timeZone);
        formatters.put(LongDateTimeFormatterKey, longDateTime);
        xml.put(LongDateTimeFormatterKey, new PipedFormatter(JavaFormatDateFormatter, longDateTime));

        Object timeMillis = new AWVTimeMillisFormatter(locale, timeZone);
        formatters.put(TimeMillisFormatterKey, timeMillis);
        xml.put(TimeMillisFormatterKey, new PipedFormatter(timeMillis, timeMillis));

        Object booleanFormatter = new CanonicalBooleanForamatter();
        formatters.put(BooleanFormatterKey, booleanFormatter);

        formatters.put(HiddenPassword, new HiddenPasswordFormatter());

        formatters.put(Identifier, new AWVIdentifierFormatter(locale));
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

    static public final class NonBlankString extends AWFormatter
    {
        public NonBlankString ()
        {
            super();
        }

        public String format (Object value)
        {
            return value.toString();
        }

        public Object parseObject (String stringToParse) throws ParseException
        {
            if (StringUtil.nullOrEmptyOrBlankString(stringToParse)) throw new ParseException("Value required", 0);
            return stringToParse;
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
}
