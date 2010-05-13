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

    $Id: //ariba/platform/util/core/ariba/util/formatter/UserDateFormatter.java#8 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

/**
    <code>UserDateFormatter</code> is a subclass of <code>DateFormatter</code>
    which is responsible for parsing user date strings, i.e. string typed in
    or selected by the user.  Additional natural language processing is done
    to make date input more user-friendly.  The localized strings for "today",
    "tomorrow", and "yesterday" are recognized and converted to the
    appropriate date relative to the current date.  Day of week strings
    (e.g. "monday", "sunday", etc.)  are also supported for input.

    @aribaapi documented
*/
public class UserDateFormatter extends DateFormatter
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.UserDateFormatter";

        // localized string resource key for our list of date formats
    private static final String UserFormatsKey   = "UserFormats";

        // resource tags for date format strings for today
    private static final String TodayTimeFormatKey = "TodayTimeFmt";
    private static final String TodayFormatKey     = "TodayFmt";


    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>UserDateFormatter</code>.

        @aribaapi private
    */
    public UserDateFormatter ()
    {
    }


    /*-----------------------------------------------------------------------
        Static User Date Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given <code>Date</code> in the
        default locale.  Recognizes if the given <code>Date</code> is today
        and formats the output accordingly.

        @param  date the <code>Date</code> to format into a string
        @return      a string representation of the <code>Date</code>
        @aribaapi documented
    */
    public static String getStringValue (Date date)
    {
        return getStringValue(date, getDefaultLocale(), getDefaultTimeZone());
    }

    /**
        Returns a formatted string for the given <code>Date</code> in the
        given <code>timeZone</code> using the default locale.  Recognizes if
        the given <code>Date</code> is today and formats the output
        accordingly.

        @param  date     the <code>Date</code> to format into a string
        @param  timeZone the <code>TimeZone</code> to use for formatting
        @return          a string representation of the <code>Date</code>
        @aribaapi documented
    */
    public static String getStringValue (Date date, TimeZone timeZone)
    {
        return getStringValue(date, getDefaultLocale(), timeZone);
    }

    /**
        Returns a formatted string for the given <code>Date</code> in the
        given locale.  Recognizes if the given <code>Date</code> is today and
        formats the output accordingly.

        @param  date     the <code>Date</code> to format into a string
        @param  locale   the <code>Locale</code> to use for formatting
        @param  timeZone the <code>TimeZone</code> to use for formatting
        @return          a string representation of the <code>Date</code>
        @aribaapi documented
    */
    public static String getStringValue (Date date, Locale locale, TimeZone timeZone)
    {
            // format dates for today with "Today" string
        if (Date.sameDay(date, new Date(date.calendarDate()), timeZone)) {
            String format;
            if (Date.timeIsMidnight(date, timeZone)) {
                format = TodayFormatKey;
            }
            else {
                format = TodayTimeFormatKey;
            }
            String pattern = lookupLocalizedFormat(format, locale);
            return DateFormatter.getStringValue(date, pattern, locale, timeZone);
        }
            // format other dates in the usual way
        else {
            return DateFormatter.getStringValue(date, locale, timeZone);
        }
    }

    public String getDateString (Date date, Locale locale, TimeZone timeZone)
    {
        return getStringValue(date, locale, timeZone);
    }

    /*-----------------------------------------------------------------------
        Static User Date Parsing
      -----------------------------------------------------------------------*/

    /**
        Tries to parse the given user string as a <code>Date</code> in the
        default locale.  In addition to the default date parsing, this method
        will also try to handle a number of more natural date strings,
        e.g. the string "today", "tomorrow", "monday", etc.  It also handles
        cases where the string is incomplete, e.g. "2/21".  In this case, the
        current year is used as the default.

        @param     string the string to parse as a <code>Date</code>
        @param calendarDate if true, a calendar date will be created
        @return           a <code>Date</code> derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Date</code>
        @aribaapi documented
    */
    public static Date parseUserDate (String string, boolean calendarDate)
      throws ParseException
    {
        return parseUserDate(string, getDefaultLocale(), calendarDate);
    }

    /**
        Tries to parse the given user string as a <code>Date</code> in the
        default locale.  In addition to the default date parsing, this method
        will also try to handle a number of more natural date strings,
        e.g. the string "today", "tomorrow", "monday", etc.  It also handles
        cases where the string is incomplete, e.g. "2/21".  In this case, the
        current year is used as the default.

        @param     string the string to parse as a <code>Date</code>
        @return           a <code>Date</code> derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Date</code>
        @aribaapi documented
    */
    public static Date parseUserDate (String string)
      throws ParseException
    {
        return parseUserDate(string, getDefaultLocale(), false);
    }

    /**
        Tries to parse the given user string as a <code>Date</code> in the
        given locale.  In addition to the default date parsing, this method
        will also try to handle a number of more natural date strings,
        e.g. the string "today", "tomorrow", "monday", etc.  It also handles
        cases where the string is incomplete, e.g. "2/21".  In this case, the
        current year is used as the default.

        @param     string the string to parse as a <code>Date</code>
        @param     locale the <code>Locale</code> to use for parsing
        @return           a <code>Date</code> derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Date</code>
        @aribaapi documented
    */
    public static Date parseUserDate (String string,
                                      Locale locale)
      throws ParseException
    {
        return parseUserDate(string, locale, false);
    }

    /**
        Tries to parse the given user string as a <code>Date</code> in the
        given locale.  In addition to the default date parsing, this method
        will also try to handle a number of more natural date strings,
        e.g. the string "today", "tomorrow", "monday", etc.  It also handles
        cases where the string is incomplete, e.g. "2/21".  In this case, the
        current year is used as the default.

        @param     string the string to parse as a <code>Date</code>
        @param     locale the <code>Locale</code> to use for parsing
        @param calendarDate if true, a calendar date will be created
        @return           a <code>Date</code> derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Date</code>
        @aribaapi documented
    */
    public static Date parseUserDate (String  string,
                                      Locale  locale,
                                      boolean calendarDate)
      throws ParseException
    {
        return parseUserDate(string, locale, getDefaultTimeZone(), calendarDate);
    }

    /**
        Tries to parse the given user string as a <code>Date</code> in the
        given locale.  In addition to the default date parsing, this method
        will also try to handle a number of more natural date strings,
        e.g. the string "today", "tomorrow", "monday", etc.  It also handles
        cases where the string is incomplete, e.g. "2/21".  In this case, the
        current year is used as the default.

        @param     string the string to parse as a <code>Date</code>
        @param     locale the <code>Locale</code> to use for parsing
        @param tz     the <code>TimeZone</code> to use for parsing
        @param calendarDate if true, a calendar date will be created
        @return           a <code>Date</code> derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Date</code>
        @aribaapi documented
    */
    public static Date parseUserDate (String   string,
                                      Locale   locale,
                                      TimeZone tz,
                                      boolean  calendarDate)
      throws ParseException
    {
            // try the default date parsing first
        try {
            return parseDate(string, locale, tz, calendarDate);
        }
        catch (ParseException e)
        {
        }

            // attempt to parse strings like "today", "saturday", etc.
        Date result = parseNaturalDate(string, locale, tz,calendarDate);
        if (result != null) {
            return result;
        }

            // try the localized list of user date formats
        result = parseDateUsingFormats(string, locale,
                            UserFormatsKey, tz, calendarDate);

        if (result != null) {
            return result;
        }

            // attempt to parse strings like "1/21"
        result = parseRelativeDate(string, locale, tz, calendarDate);
        if (result != null) {
            return result;
        }

        throw makeParseException(DateFormatter.CannotParseDateKey, string, 0);
    }

        // resource tags for date format strings used by parseNaturalDate()
    private static final String YesterdayStringKey = "Yesterday";
    private static final String TodayStringKey     = "Today";
    private static final String TomorrowStringKey  = "Tomorrow";

        // array of these keys for convenience below (order is important)
    private static final String[] RelativeDayKeys = {
        YesterdayStringKey, TodayStringKey, TomorrowStringKey
    };

    /**
        Handles natural language strings like days of the week or "today",
        "tomorrow", and "yesterday".  For example, if today is Thursday,
        December 17, 1998, and the user types in "Sun", it will return a Date
        for Sunday, December 20, 1998, 00:00:00.
        @aribaapi private
    */
    private static Date parseNaturalDate (String string, Locale locale,
                                          TimeZone timeZone,
                                          boolean calendarDate)
    {
        if (StringUtil.nullOrEmptyString(string)) {
            return null;
        }

        /*
            "now" doesn't need to be calendarDate-aware,
            since this is just a temp to find out
            what year, month, and day today is.
        */
        Date now = new Date();

        int thisYear = Date.getYear(now, timeZone, locale);
        int thisMonth = Date.getMonth(now, timeZone, locale);
        int thisDayOfMonth = Date.getDayOfMonth(now, timeZone, locale);
        int thisDayOfWeek = Date.getDayOfWeek(now, timeZone, locale);

        Integer daysToAdd = null;

            // if the user entered a day name, offset into the future to the
            // appropriate day of the month, month, and year
        int dayOfWeek = findDay(string);
        if (dayOfWeek >= 0) {
            int add = ((7 + dayOfWeek) - thisDayOfWeek) % 7;
            if (add == 0) {
                add = 7;
            }
            daysToAdd = Constants.getInteger(add);
        }
            // otherwise, check if the string is a prefix for strings
            // like "today", "tomorrow", etc.
        else {
            for (int i = -1, len = string.length(); i <= 1; i++) {
                String key = RelativeDayKeys[i+1];
                String test = lookupLocalizedFormat(key, locale);
                if (!StringUtil.nullOrEmptyString(test)) {
                    if (test.length() >= len) {
                        String sub = test.substring(0, len);
                        if (sub.equalsIgnoreCase(string)) {
                            daysToAdd = Constants.getInteger(i);
                            break;
                        }
                    }
                }
            }
        }
        Date result = null;
        if (daysToAdd != null) {
            result = new Date(thisYear, thisMonth, thisDayOfMonth,
                                   calendarDate, timeZone, locale);
            Date.addDays(result, daysToAdd.intValue(), timeZone, locale);
            return result;
        }
        else {
                // lastly, try TodayTimeFormatKey.  Get the number of milisecs
                // that the time represent with respect to GMT. Then add on to
                // today's.
            result = parseDateUsingFormats(string, locale,
                     TodayTimeFormatKey, TimeZone.getTimeZone("GMT"), calendarDate);
            if (result != null) {
                Date today = new Date(thisYear, thisMonth, thisDayOfMonth,
                                   calendarDate, timeZone, locale);
                if (result.getTime() < Date.MillisPerDay) {
                    result.setTime(result.getTime() + today.getTime());
                }
            }
            return result;
        }
    }

    /**
        Determines whether the given string is a prefix for one of the
        localized day of the week words, e.g. "monday", "thursday", etc.
        Returns the day of the week (Sunday = 0) that matches, or -1 if the
        string doesn't match.

        @aribaapi private
    */
    private static int findDay (String string)
    {
            // use the locale specified by the resource service
        Locale locale = ResourceService.getService().getLocale();
        DateFormatSymbols symbols = new DateFormatSymbols(locale);

        String[] days = symbols.getWeekdays();
        int length = string.length();

        for (int i = 0; i < days.length; i++) {
            if ((days[i].length() >= length) &&
                (days[i].substring(0, length).equalsIgnoreCase(string)))
            {
                return i;
            }
        }

        return -1;
    }

        // resource tag for date format string used by parseRelativeDate()
    private static final String YearRelativeFormat = "YearRelativeFmt";

    /**
        Attempts to parse the given string as a relative date, i.e. a month
        and date with no year.  If the string is in this form, the current
        year is used as the default.
    */
    private static Date parseRelativeDate (String string, Locale locale,
                                           TimeZone timeZone,
                                           boolean calendarDate)
    {
            // parse the date given the year-relative format
        String format = lookupLocalizedFormat(YearRelativeFormat, locale);
        Date date = null;
        try {
            date = parseDate(string, format, locale, timeZone, calendarDate);
        }
        catch (ParseException e) {
            return null;
        }

            // set the year for the date to this year
        Date.setYear(date, Date.getYear(new Date(calendarDate)));

        return date;
    }

    /*-- User Date Values ---------------------------------------------------*/

    /**
        Returns a new <code>Date</code> derived from the given object in the
        default locale.  If the object is not a <code>Date</code>, it is
        converted to a string and parsed as a user string.  Returns null if
        the object can't be converted to a <code>Date</code>.

        @param  object the object to convert to a <code>Date</code>
        @param calendarDate if true, a calendar date will be created
        @return        a <code>Date</code> derived from the given object
        @aribaapi documented
    */
    public static Date userDateValue (Object object, boolean calendarDate)
    {
        return userDateValue(object, getDefaultLocale(), calendarDate);
    }

    /**
        Returns a new <code>Date</code> derived from the given object in the
        default locale.  If the object is not a <code>Date</code>, it is
        converted to a string and parsed as a user string.  Returns null if
        the object can't be converted to a <code>Date</code>.

        @param  object the object to convert to a <code>Date</code>
        @return        a <code>Date</code> derived from the given object
        @aribaapi documented
    */
    public static Date userDateValue (Object object)
    {
        return userDateValue(object, getDefaultLocale(), false);
    }

    /**
        Returns a new <code>Date</code> derived from the given object in the
        given locale.  If the object is not a <code>Date</code>, it is
        converted to a string and parsed as a user string.  Returns null if
        the object can't be converted to a <code>Date</code>.

        @param  object the object to convert to a <code>Date</code>
        @param  locale the <code>Locale</code> to use for conversion
        @return        a <code>Date</code> derived from the given object
        @aribaapi documented
    */
    public static Date userDateValue (Object object, Locale locale)
    {
        return userDateValue(object, locale, false);
    }

    /**
        Returns a new <code>Date</code> derived from the given object in the
        given locale.  If the object is not a <code>Date</code>, it is
        converted to a string and parsed as a user string.  Returns null if
        the object can't be converted to a <code>Date</code>.

        @param  object the object to convert to a <code>Date</code>
        @param  locale the <code>Locale</code> to use for conversion
        @param calendarDate if true, a calendar date will be created
        @return        a <code>Date</code> derived from the given object
        @aribaapi documented
    */
    public static Date userDateValue (Object object, Locale locale,
                                      boolean calendarDate)
    {
        if (object == null) {
            return null;
        }
        else if (object instanceof Date) {
            return ((Date)object);
        }
        else {
            try {
                return parseUserDate(object.toString(), locale, calendarDate);
            }
            catch (ParseException e) {
            }
            return null;
        }
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the given
        locale.  The object must be a non-null <code>Date</code>.

        @param  object the <code>Date</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Date</code>
        @aribaapi documented
    */
    protected String formatObject (Object object, Locale locale)
    {
        Assert.that(object instanceof Date, "invalid type");
        return getStringValue((Date)object, locale);
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Parse the given string as a <code>Date</code> in the given
        locale and time zone.  The <code>locale</code> and
        <code>timeZone</code> parameters must be non-null.

        @param     string the string to parse
        @param     locale the <code>Locale</code> to use for parsing
        @param tz           the <code>TimeZone</code> to use for parsing
        @return           a <code>Date</code> object derived from the string
        @exception        ParseException if the string can't be parsed as a
                          <code>Date</code> object in the given locale
        @aribaapi documented
    */
    public Object parseString (String string, Locale locale, TimeZone tz) 
        throws ParseException
    {
        return parseString(string, locale, tz, false);
    }

    /**
        Parse the given string as a <code>Date</code> in the given
        locale and time zone.  The <code>locale</code> and
        <code>timeZone</code> parameters must be non-null.

        @param string       the string to parse as a <code>Date</code>
        @param locale       the <code>Locale</code> to use for parsing
        @param tz     the <code>TimeZone</code> to use for parsing
        @param calendarDate if true, a calendar date will be created
        @return             a <code>Date</code> derived from the string
        @exception          ParseException if the string can't be parsed as a
                            <code>Date</code>
        @aribaapi documented
    */
    public Object parseString (
                        String string,
                        Locale locale,
                        TimeZone tz,
                        boolean calendarDate)
      throws ParseException
    {
        return parseUserDate(string, locale, tz, calendarDate);
    }

    /**
        Overrides DateFormatter.parseString() to call our static method for
        converting an object into a date.
        @param  object the object to convert to a <code>Date</code>
        @param  locale the <code>Locale</code> to use for conversion
        @return             a <code>Date</code> derived from the string
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        return userDateValue(object, locale, false);
    }
}
