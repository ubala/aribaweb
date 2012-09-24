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

    $Id: //ariba/platform/util/core/ariba/util/core/Date.java#35 $
*/

package ariba.util.core;

import ariba.util.formatter.DateFormatter;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
    The <code>Date</code> class implements helper functions on top of
    the <code>java.util.Date</code> class. This also has the
    flexibility to represent a calendar date which is a date/time
    representation independent of timezone when used with the Ariba
    Formatter classes. For example a calendar date representing 7:30
    AM, Friday July 9th 1999 will appear as such independent of the
    timezone the client inspecting the date is in.

    <p>

    The Date object using and not using the calendar date flag are not
    interchangeable in all cases. For example you may not change the
    metadata layer to use a calendar date unless the code is also
    aware of the change. Calendar dates are not fully supported in
    this release and are only intended to be used with any time cards
    or expense report modules.

    @aribaapi documented
*/
public class Date extends java.util.Date implements Externalizable
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.core.Date";

    /**
        Constant for number of days in a week.
        @aribaapi documented
    */
    public static final long DaysPerWeek = 7;

    /**
        Constant for number of hours in a day.
        @aribaapi documented
    */
    public static final long HoursPerDay = 24;

    /**
        Constant for number of minutes per hour.
        @aribaapi documented
    */
    public static final long MinutesPerHour = 60;

    /**
        Constant for number of seconds per minute.
        @aribaapi documented
    */
    public static final long SecondsPerMinute = 60;

    /**
        Constant for number of milliseconds per second.
        @aribaapi documented
    */
    public static final long MillisPerSecond = 1000;

    /**
        Constant for number of milliseconds per minute.
        @aribaapi documented
    */
    public static final long MillisPerMinute = MillisPerSecond * SecondsPerMinute;

    /**
        Constant for number of milliseconds per hour.
        @aribaapi documented
    */
    public static final long MillisPerHour = MillisPerMinute * MinutesPerHour;

    /**
        Constant for number of milliseconds per day.
        @aribaapi documented
    */
    public static final long MillisPerDay = MillisPerHour * HoursPerDay;

    /**
        Constant for number of milliseconds per week.
        @aribaapi documented
    */
    public static final long MillisPerWeek = MillisPerDay * DaysPerWeek;


    /*-----------------------------------------------------------------------
        Private Static Fields
      -----------------------------------------------------------------------*/

        // a cache of Calendar instances
    private static final GrowOnlyHashtable CALENDAR_CACHE = new GrowOnlyHashtable();

        // Cache of custom timezones created from offset values
    private static final GrowOnlyHashtable TIMEZONE_CACHE = new GrowOnlyHashtable();


    /*-----------------------------------------------------------------------
        Private Fields
      -----------------------------------------------------------------------*/

    /**
        @serial Flag indicating whether we have a calendarDate object.
        @aribaapi private
    */
    private boolean calendarDate = false;


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>Date</code> for the current date and time.

        @aribaapi documented
    */
    public Date ()
    {
    }

    /**
        Creates a new <code>Date</code> for the current date and
        time. If calendarDate is true, a calendar date object will be
        created.

        If calendarDate is true, the Date object created will
        represent the same time in hours, minutes, and seconds as the
        current date does in the local time zone.
        @param calendarDate <code>true</code> if the object should be a calendarDate
        @aribaapi documented
    */
    public Date (boolean calendarDate)
    {
        super();
        if (calendarDate) {
            long offset = timezoneOffsetInMillis(this);
            setTime(getTime() - offset);
        }
        this.calendarDate = calendarDate;

    }

    /**
        Creates a new <code>Date</code> with the given timestamp.

        See java.lang.System.currentTimeMillis()

        @param time time in milliseconds

        @aribaapi documented
    */
    public Date (long time)
    {
        super(time);
    }

    /**
        Creates a new <code>Date</code> with the given timestamp.

        See java.lang.System.currentTimeMillis()

        @param time time in milliseconds
        @param calendarDate if <code>true</code> a calendar date
        object will be created.


        If calendarDate is true, the Date object created will
        represent the same time in hours, minutes, and seconds as the
        timestamp has does in the local time zone.


        @aribaapi documented
    */
    public Date (long time, boolean calendarDate)
    {
        super(time);
        this.calendarDate = calendarDate;
    }

    /**
        Creates a new <code>Date</code> from an existing <code>Date</code>
        object.

        @param other a <code>Date</code> object to copy the time from

        @aribaapi documented
    */
    public Date (Date other)
    {
        super(other.getTime());
        this.calendarDate = other.calendarDate();
    }

    /**
        Creates a new <code>Date</code> for the given year, month, and day.

        @param year  the year for the Gregorian calendar, e.g. 1998
        @param month the zero-based month (December is 11)
        @param day   the one-based day-of-month (first of the month is 1)

        @aribaapi documented
    */
    public Date (int year, int month, int day)
    {
        this(year, month, day, false);
    }

    /**
        Creates a new <code>Date</code> for the given year, month, and day.

        @param year  the year for the Gregorian calendar, e.g. 1998
        @param month the zero-based month (December is 11)
        @param day   the one-based day-of-month (first of the month is 1)
        @param calendarDate if <code>true</code> a calendar date
        object will be created

        @aribaapi documented
    */
    public Date (int year, int month, int day, boolean calendarDate)
    {
        this.calendarDate = calendarDate;
            // get a calendar instance for our current locale
        Calendar calendar = getCalendarInstance(this.calendarDate);
        synchronized (calendar) {
            clearCalendarFields(calendar);
                // set the year, month, and day
            calendar.set(year, month, day, 0, 0, 0);
            calendar.clear(Calendar.MILLISECOND);

                // set our time in milliseconds
            updateDateFromCalendar(this, calendar);
        }
    }

    /**
        Creates a new <code>Date</code> for the given year, month, and day.

        @param year  the year for the Gregorian calendar, e.g. 1998
        @param month the zero-based month (December is 11)
        @param day   the one-based day-of-month (first of the month is 1)
        @param calendarDate if <code>true</code> a calendar date
        @param tz the TimeZone to use if calendarDate is <code>false</code>
        @param locale the Locale for year, month, day
        object will be created

        @aribaapi documented
    */
    public Date (int year, int month, int day, boolean calendarDate,
                                TimeZone tz, Locale locale)
    {
        this.calendarDate = calendarDate;

            // get a calendar instance for our current locale
        Calendar calendar = getCalendarInstance(calendarDate, tz, locale);
        synchronized (calendar) {
            clearCalendarFields(calendar);
                // set the year, month, and day
            calendar.set(year, month, day, 0, 0, 0);
            calendar.clear(Calendar.MILLISECOND);

                // set our time in milliseconds
            updateDateFromCalendar(this, calendar);
        }
    }
    /**
        Creates a new <code>Date</code> for the given year, month, and day.

        @param year  the year for the Gregorian calendar, e.g. 1998
        @param month the zero-based month (December is 11)
        @param day   the one-based day-of-month (first of the month is 1)
        @param h     the number of hours since midnight
        @param m     the number of minutes past the hour
        @param calendarDate if <code>true</code> a calendar date
        @param tz the TimeZone to use if calendarDate is <code>false</code>
        @param locale the Locale for year, month, day
        object will be created

        @aribaapi documented
    */
    public Date (int year, int month, int day, int h, int m, boolean calendarDate,
                                TimeZone tz, Locale locale)
    {
        this.calendarDate = calendarDate;

            // get a calendar instance for our current locale
        Calendar calendar = getCalendarInstance(calendarDate, tz, locale);
        synchronized (calendar) {
            clearCalendarFields(calendar);
                // set the year, month, and day
            calendar.set(year, month, day, h, m, 0);
            calendar.clear(Calendar.MILLISECOND);

                // set our time in milliseconds
            updateDateFromCalendar(this, calendar);
        }
    }

    /*-----------------------------------------------------------------------
        Public Static Methods
      -----------------------------------------------------------------------*/

        // the special time stamp for our null date value
    private static final int NullDateMillis = 100800000;

    /**
        Returns a freshly allocated date representing a null value. A null
        value is mostly of interest because it prints as an empty string.
        Thus, it can be used to indicate an empty string in a TextField that
        always returns a Date object.
        <p>
        Note: the null date is actually Date(100800000) instead of Date(0)
        because of stupid GMT to local timezone conversion problems.  It is
        important that all dates in the system use Date.newNull() instead of
        Date(0) to get starting point for all dates. Date(0) is verboten.

        @param calendarDate true if the object created should be of
        type calendar date
        @return a new Date object representing a null value.
        @aribaapi documented
    */
    public static Date newNull (boolean calendarDate)
    {
            // Thu Jan 01 20:00:00 1970
        return new Date(NullDateMillis, calendarDate);
    }

    /**
        Returns a freshly allocated, non-calendarDate date
        representing a null value.
        @return a new Date object representing a null value.
        @see #newNull

        @aribaapi documented
    */
    public static Date newNull ()
    {
        return newNull(false);
    }

    /**
        Returns <code>true</code> if this <code>Date</code> is the special
        null date value.

        @see #newNull
        @return true if and only if this date object represents a null
        date as returned by Date.newNull()
        @aribaapi documented
    */
    public boolean isNull ()
    {
        return (getTime() == NullDateMillis);
    }

    /**
        Creates a calendar date object that represents the time and
        date of this Date object in its current time zone. For
        example, if <code>this</code> is not a calendar date and has
        the time 7:30 AM, Friday July 9th 1999 PDT, and the caller is
        in the PDT timezone, a calendar date will be created that will
        display as 7:30 AM, Friday July 9th 1999 in any timezone when
        using the Ariba formatters.

        To be clear, a Date does not know "its current time zone", and
        this method does not allow you to pass in the user session's
        TimeZone.  When it says "this Date object in its current time
        zone", you should read it as "this Date object in the SERVER's
        time zone", which is always "America/Los_Angeles" (aka PT, PST,
        PDT) in On Demand.  To verify this, see timezoneOffsetInMillis,
        which calls DateFormatter getDefaultTimeZone, which returns JDK
        TimeZone getDefault().  See updateToBeCalendarDate(TimeZone) for
        a better alternative that can handle the user session's current
        time zone, or any other desired time zone.

        @return a new <code>Date</code> object if this is not a
        calendar date object, or <code>this</code> if this object is
        already a calendar date

        @aribaapi documented
    */
    public Date makeCalendarDate ()
    {
        if (calendarDate()) {
            return this;
        }
        if (isNull()) {
            return newNull(true);
        }
        long offset = timezoneOffsetInMillis(this);
        return new Date(this.getTime() - offset, true);
    }
    
    /**
        Updates this Date to be an Ariba calendar date that represents the same day and
        time of day universally that this Date currently represents in the given TimeZone.
        If this Date is already a calendar date, nothing is changed. If this Date has the
        special NullDateMillis value, we leave the value unchanged and just mark it to be
        a calendar date.  Otherwise, we adjust the time milliseconds value from the given
        TimeZone to GMT, and mark the updated value as a calendar date.  For example, if
        this is the non-calendar date "7:30 AM, Friday July 9th 1999 PDT" and the TimeZone
        "America/Los_Angeles" is passed in, then this Date will become the calendar date
        "7:30 AM, Friday July 9th 1999 GMT", and the time value internally will have had 7
        hours worth of milliseconds subtracted from it, since PDT is GMT-7.  The Ariba
        DateFormatter code will display the same day and time for a calendar date,
        regardless of the session timezone.  Usually for a calendar date the time will not
        be displayed, but it is present internally.  Often the time is set to midnight at
        the beginning of the desired day before being updated to be a calendar date, see
        setTimeToMidnight(Date,TimeZone).
        @param TimeZone if the TimeZone is null, the default host server TimeZone will be
        used, but it is safer to pass an explicit TimeZone, since conversion to a calendar
        date requires a contextual TimeZone for correct behavior.
        @aribaapi private
    */
    public void updateToBeCalendarDate (TimeZone tz)
    {
        if (!calendarDate) {
            long dateMillis = getTime();
            if (dateMillis != NullDateMillis) {
                long offset = timezoneOffsetInMillis(this, tz);
                setTime(dateMillis - offset);
            }
            calendarDate = true;
        }
    }

    /**
     Added this helper method because ariba.util.core.Date.makeCalendarDate
    does not strip off the time before converting the date to a calendar date.
    Fixing CR 1-9GIM3D -adding an api makeCalendarDateWithoutTime() which
    will strip the time off of the date.

    @aribaapi documented
    */

    public static Date makeCalendarDateWithoutTime (Date date)
    {        
        if (date != null && !date.calendarDate()) {    
            Date newDate = new Date(date);    
            if (!Date.timeIsMidnight(newDate)) {    
                Date.setTimeToMidnight(newDate);    
            }    
            newDate = newDate.makeCalendarDate();    
            return newDate;    
        }    
        return date;    
    }



    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /*-- Localized Formats --------------------------------------------------*/

    /**
        Returns a string representation of the date portion of this
        <code>Date</code> in a concise, unpadded format, e.g. "1/12/1999".
        The exact format is locale-specific.

        @return concise date string
        @aribaapi private
    */
    public String toConciseDateString ()
    {
        return DateFormatter.toConciseDateString(this);
    }

    /**
        Returns a string representation of the date portion of this
        <code>Date</code> in a concise, padded format, e.g. "1/12/1999".  The
        exact format is locale-specific.

        @return padded concise date string
        @aribaapi private
    */
    public String toPaddedConciseDateString ()
    {
        return DateFormatter.toPaddedConciseDateString(this);
    }

    /**
        Returns a string representation of the date and time for this
        <code>Date</code> in a concise, unpadded format, e.g.  "1/12/1999 3:14
        PM".  The exact format is locale-specific.

        @return concise date and time string
        @aribaapi private
    */
    public String toConciseDateTimeString ()
    {
        return DateFormatter.toConciseDateTimeString(this);
    }

    /**
        Returns a string representation of the date and time for this
        <code>Date</code> in a concise, padded format, e.g.  "01/12/1999 03:14
        PM".  The exact format is locale-specific.

        @return padded concise date and time string
        @aribaapi private
    */
    public String toPaddedConciseDateTimeString ()
    {
        return DateFormatter.toPaddedConciseDateTimeString(this);
    }

    /**
        Returns a string representation of the date portion of this
        <code>Date</code> which includes an abbreviated day of week, day of
        month, month, and year, e.g. "Mon, 02 Jun, 1999".  The exact format is
        locale-specific.

        @return date string with abbreviated day of week, day of month, month,
                and year
        @aribaapi private
    */
    public String toDayDateMonthYearString ()
    {
        return DateFormatter.toDayDateMonthYearString(this);
    }

    /**
        Returns a string representation of the date portion of this
        <code>Date</code> which includes the full day of week, month, day of
        month and year, e.g. "Monday, January 31, 1999".  The exact format is
        locale-specific.

        @return string with full day of week, month, day of month, and year
        @aribaapi private
    */
    public String toLongDayDateMonthYearString ()
    {
        return DateFormatter.toLongDayDateMonthYearString(this);
    }

    /**
        Returns a string representation of this <code>Date</code> (including
        time) which includes the full day of week, month, day of month, year
        and time e.g. "Monday, January 31, 1999 at 3:15 PM".  The exact format
        is locale-specific.

        @return string with full day of week, month, day of month, year, and time
        @aribaapi private
    */
    public String toFullDayDateMonthYearString ()
    {
        return DateFormatter.toFullDayDateMonthYearString(this);
    }

    /**
        Returns a string representation of the date portion of this
        <code>Date</code> which includes the full month, day of month and
        year, e.g. "January 31, 1999".  The exact format is locale-specific.

        @return date string with full month, day of month, and year
        @aribaapi private
    */
    public String toDateMonthYearString ()
    {
        return DateFormatter.toDateMonthYearString(this);
    }

    /**
        Returns a string representation of this <code>Date</code> which
        includes the full month, day of month, year and time, e.g. "January
        31, 1999 at 3:15 PM".  The exact format is locale-specific.

        @return string with full month, day of month, year, and time
        @aribaapi private
    */
    public String toFullDateMonthYearString ()
    {
        return DateFormatter.toFullDateMonthYearString(this);
    }

    /**
        Returns a string representation of the time portion of this
        <code>Date</code> which includes hours, minutes and seconds, e.g.
        "3:01:42".  The exact format is locale-specific.

        @return string with hours, minutes, and seconds
        @aribaapi private
    */
    public String toHourMinSecString ()
    {
        return DateFormatter.toHourMinSecString(this);
    }

    /**
        Returns an abbreviated string representation of the day of the week
        represented by this <code>Date</code>, e.g. "Mon", "Tue", etc.

        @return abbreviated day of the week
        @aribaapi private
    */
    public String toDayOfWeekString ()
    {
        return DateFormatter.toDayOfWeekString(this);
    }

        // Trying to work around Sun Bug Id 4335714 (see Ariba PR31516).
        //
        // Since we use our java.util.Date.toString can have trouble, we
        // try to use our own DateFormatter. They both use
        // SimpleDateFormat, but perhaps whatever is mysteriously is
        // causing their failure will be fixed be using a slightly
        // different version. Perhaps it is a JIT bug, or a problem with
        // the SoftReference they are using for caching in JDK1.2.x.
    /**
        Converts this Date object to a String

        @return a String in EEE MMM dd HH:mm:ss zzz yyyy format
        @aribaapi public
    */
    public String toString ()
    {
        return DateFormatter.toJavaFormat(this, Locale.US);
    }

    /*-- Non-Localized Formats ----------------------------------------------*/

    /**
        Returns a string representing this <code>Date</code> in a standard GMT
        format, e.g. "12 Aug 1995 02:30:00 GMT".  We override this deprecated
        method but leave it marked as deprecated so any customization code
        that might call this deprecated method will still get a reasonable
        implementation.

        @deprecated  replaced by <code>toGMT</code>
        @aribaapi private
    */
    public String toGMTString ()
    {
        return toGMT();
    }

    /**
        Returns a string representing this <code>Date</code> in a standard GMT
        format, e.g. "12 Aug 1995 02:30:00 GMT".  This method should be used
        in place of the deprecated <code>toGMTString</code>.  This format is
        not not locale-specific.

        @return string in standard GMT format
        @aribaapi private
    */
    public String toGMT ()
    {
        return DateFormatter.toGMT(this);
    }

    /**
        Returns a string representation of this <code>Date</code> in a compact
        format with year, month, and day of month run together, e.g.
        "19990521".  This format is not locale-specific.

        @return compact date string with year, month, and day of month run together
        @aribaapi private
    */
    public String toYearMonthDate ()
    {
        return DateFormatter.toYearMonthDate(this);
    }

    /**
        Returns a string representation of the time portion of this
        <code>Date</code> in military time format, e.g. "21:05:35".  This
        format is not locale-specific.

        @return time string in military time format
        @aribaapi private
    */
    public String toMilitaryTimeString ()
    {
        return DateFormatter.toMilitaryTimeString(this);
    }

    /**
        Returns a string representation of the time portion of this
        <code>Date</code> in a format suitable for use as a filename suffix.

        @return time string for filename suffix
        @aribaapi private
    */
    public String toFileTimeString ()
    {
        return DateFormatter.toFileTimeString(this);
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        Returns true if this <code>Date</code> is a calendarDate
        @return true if this date is a calendarDate
        @aribaapi documented
    */
    public boolean calendarDate ()
    {
        return calendarDate;
    }

    /**
        Returns true if this <code>Date</code> is before the given timestamp
        in milliseconds.
        @param when The number of milliseconds to compare against as
        returned by getTime()
        @return true if this date occurred before <code>when</code> and
        false otherwise
        @aribaapi documented
    */
    public boolean before (long when)
    {
        return getTime() < when;
    }

    /**
        Returns true if this <code>Date</code> is between the 2
        timestamps in milliseconds. If it is the same as one
        of the two, it will not be considered between, and will
        return false.
        @param from The number of milliseconds representing the lower
        boundary of the interval.
        @param to The number of milliseconds representing the upper
        boundary of the interval.
        @return true is this date occurred between <code>to</code> and
        <code>from</code> and false otherwise.
        @aribaapi ariba
    */
    public boolean between (long from, long to)
    {
        long time = getTime();
        if (time <= from || time >= to) {
            return false;
        }
        return true;
    }


    /**
        Returns true if this <code>Date</code> is between the 2
        timestamps in milliseconds. If it is the same as one
        of the two, it will not be considered between, and will
        return false.
        @param from The <code>Date</code> representing the lower
        boundary of the interval.
        @param to The <code>Date</code> representing the upper
        boundary of the interval.
        @return true is this date occurred between <code>to</code> and
        <code>from</code> and false otherwise.
        @aribaapi ariba
    */
    public boolean between (Date from, Date to)
    {
        return between(from.getTime(), to.getTime());
    }

    /**
        Returns the current <code>Date</code> on the system
        @return a new Date object containing the time that the method
        was invoked.
        @aribaapi documented
    */
    public static Date getNow ()
    {
        return new Date(System.currentTimeMillis());
    }

    /**
        The non-static methods that follow are deprecated in JDK 1.1; however,
        since we can get a reasonable locale from the resource service and do
        the "right thing" using the Calendar class, we define them here (still
        marked as deprecated) to use the corresponding static implementations.
        This allows us to get rid of the compiler warnings but is safer for
        customization code which might call the deprecated versions.  Ariba
        code should now use the static versions to avoid compiler warnings.
    */

    /*-- Time Zone ----------------------------------------------------------*/

    /**
        @deprecated  replaced by <code>timezoneOffset</code>
        @aribaapi private
    */
    public int getTimezoneOffset ()
    {
        return timezoneOffset(this);
    }

    /**
        Returns the time zone offset from GMT for the given <code>Date</code>
        in minutes.  The result is the number of minutes that should be added
        to GMT time to get the locale time for the given date.
        <p>
        The <code>date</code> parameter is a <code>java.util.Date</code>, so
        this method can be used with <code>java.sql.Timestamp</code>, which is
        a subclass of <code>java.util.Date</code>.

        @param date a <code>java.util.Date</code> or a
        <code>ariba.util.core.Date</code> object that identifies the
        date to use when calculating the offset.
        @return a number of <b>minutes</b> that this date is off from
        GMT timezone.
        @aribaapi documented
    */
    public static int timezoneOffset (java.util.Date date)
    {
        return timezoneOffset(date, null);
    }

    /**
        Returns the time zone offset from GMT for the given <code>Date</code>
        in minutes.  The result is the number of minutes that should be added
        to GMT time to get the locale time for the given date.
        <p>
        The <code>date</code> parameter is a <code>java.util.Date</code>, so
        this method can be used with <code>java.sql.Timestamp</code>, which is
        a subclass of <code>java.util.Date</code>.

        @param date a <code>java.util.Date</code> or a
        <code>ariba.util.core.Date</code> object that identifies the
        date to use when calculating the offset.
        @param tz the <code>TimeZone</code> that identifies Timezone the offset
        should be based on. to used. If null,
        DateFormatter.getDefaultTimeZone() will be used.
        @return a number of <b>minutes</b> that this date is off from
        GMT timezone.
        @aribaapi documented
    */
    public static int timezoneOffset (java.util.Date date, TimeZone tz)
    {
        return (int)(timezoneOffsetInMillis(date, tz) / MillisPerMinute);
    }

    /**
        Returns the time zone offset from GMT for the given <code>Date</code>
        in milliseconds.  The result is the number of milliseconds that should
        be added to GMT time to get the locale time for the given date.
        <p>

        @param date a Date object that identifies the date to use when
        calculating the offset.
        @return a number of <b>milliseconds</b> that this date is off
        from GMT timezone.
        @aribaapi documented
    */
    public static int timezoneOffsetInMillis (Date date)
    {
        return timezoneOffsetInMillis(date, null);
    }

    /**
        Returns the time zone offset from GMT for the given <code>Date</code>
        in milliseconds.  The result is the number of milliseconds that should
        be added to GMT time to get the locale time for the given date.
        <p>

        @param date a Date object that identifies the date to use when
        calculating the offset.
        @param tz the <code>TimeZone</code> that identifies Timezone the offset
        should be based on. to used. If null,
        DateFormatter.getDefaultTimeZone() will be used.
        @return a number of <b>milliseconds</b> that this date is off
        from GMT timezone.
        @aribaapi documented
    */
    public static int timezoneOffsetInMillis (Date date, TimeZone tz)
    {
        TimeZone timezone = (tz == null ?
                       DateFormatter.getDefaultTimeZone() :
                       tz);

        Calendar cal = getCalendarInstance(date.calendarDate(),
                                           timezone,
                                           Locale.US);
        synchronized (cal) {
            clearCalendarFields(cal);
            updateCalendarFromDate(cal, date);


            return -(cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET));
        }
    }

    /**
        Returns the time zone offset from GMT for the given <code>Date</code>
        in milliseconds.  The result is the number of milliseconds that should
        be added to GMT time to get the locale time for the given date.
        <p>
        The <code>date</code> parameter is a <code>java.util.Date</code>, so
        this method can be used with <code>java.sql.Timestamp</code>, which is
        a subclass of <code>java.util.Date</code>.

        @param date a <code>java.util.Date</code> or
        <code>ariba.util.core.Date</code> object that identifies the
        date to use when calculating the offset.
        @return a number of <b>milliseconds</b> that this date is off
        from GMT timezone.
        @aribaapi documented
    */
    public static int timezoneOffsetInMillis (java.util.Date date)
    {
        return timezoneOffsetInMillis(date, null);
    }


    /**
        Returns the time zone offset from GMT for the given <code>Date</code>
        in milliseconds.  The result is the number of milliseconds that should
        be added to GMT time to get the locale time for the given date.
        <p>
        The <code>date</code> parameter is a <code>java.util.Date</code>, so
        this method can be used with <code>java.sql.Timestamp</code>, which is
        a subclass of <code>java.util.Date</code>.

        @param date a <code>java.util.Date</code> or
        <code>ariba.util.core.Date</code> object that identifies the
        date to use when calculating the offset.
        @param tz the <code>TimeZone</code> that identifies Timezone the offset
        should be based on. to used. If null,
        DateFormatter.getDefaultTimeZone() will be used.
        @return a number of <b>milliseconds</b> that this date is off
        from GMT timezone.
        @aribaapi documented
    */

    public static int timezoneOffsetInMillis (java.util.Date date, TimeZone tz)
    {
            // get the appropriate Calendar instance
        if (date instanceof Date) {
            return(timezoneOffsetInMillis((Date)date, tz));
        }
        Calendar cal = getCalendarInstance(false);
        synchronized (cal) {
            clearCalendarFields(cal);
            updateCalendarFromDate(cal, date);

                // return timezone offset plus the Daylight Savings Time offset
            return -(cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET));
        }
    }

    private static final String EastOfGMTTimeZoneID = "GMT+%s:00";
    private static final String WestOfGMTTimeZoneID = "GMT-%s:00";

    /**
        Creates and returns a <code>TimeZone</code> for the given
        <code>minuteOffset</code>

        @aribaapi private
    */
    public static TimeZone createTimeZoneFromOffset (int minuteOffset)
    {
        int hourOffset = (int)(minuteOffset / Date.MinutesPerHour);
        String timeZoneId;
        if (hourOffset >= 0) {
            timeZoneId = Fmt.S(EastOfGMTTimeZoneID, Math.abs(hourOffset));
        }
        else {
            timeZoneId = Fmt.S(WestOfGMTTimeZoneID, Math.abs(hourOffset));
        }
        TimeZone tz = TimeZone.getTimeZone(timeZoneId);

            // Java always sets the ID to "custom" which causes caching
            // problems since we hash by id.
        if (tz != null) {
            tz.setID(timeZoneId);
        }
        return tz;
    }

    /**
        Returns a <code>TimeZone</code> for the given <code>minuteOffset</code>
        <p>
        The time zones are cached by offset number.

        @aribaapi private
    */
    public static TimeZone getTimeZoneFromOffset (int minuteOffset)
    {
            // No need to synchronize on a GrowOnlyHashtable
        Integer offsetObject = Constants.getInteger(minuteOffset);
        TimeZone tz = (TimeZone)TIMEZONE_CACHE.get(offsetObject);
        if (tz != null) {
            return tz;
        }

        tz = createTimeZoneFromOffset(minuteOffset);
        TIMEZONE_CACHE.put(offsetObject, tz);
        return tz;
    }


    /*-- Year ---------------------------------------------------------------*/

    /**
        @deprecated  replaced by <code>static getYear</code>
        @aribaapi private
    */
    public int getYear ()
    {
            // the pre-JDK 1.1 representation of year
        return getYear(this) - 1900;
    }

    /**
        @deprecated  replaced by <code>static setYear(Date, int)</code>
        @aribaapi private
    */
    public void setYear (int year)
    {
        setYear(this, year + 1900);
    }

    /**
        Returns the calendar year (assuming a Gregorian calendar) for the
        given <code>Date</code>, e.g. 1999.  Note that this is different than
        the JDK 1.0 <code>getYear</code> method, which returned the year minus
        1900.
        @param date The date object to return the year from.
        @return int an integer representing the yar of the date
        object. Unlike java's getYear, 1973 will actually be returned
        as 1973 (rather than 73)
        @aribaapi documented
    */
    public static int getYear (java.util.Date date)
    {
        return getCalendarField(date, Calendar.YEAR);
    }

    /**
        Returns the calendar year (assuming a Gregorian calendar) for the
        given <code>Date</code>, e.g. 1999.  Note that this is different than
        the JDK 1.0 <code>getYear</code> method, which returned the year minus
        1900.
        @param date The date object to return the year from.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @return int an integer representing the year of the date
        object. Unlike java's getYear, 1973 will actually be returned
        as 1973 (rather than 73)
        @aribaapi documented
    */
    public static int getYear (java.util.Date date, TimeZone tz, Locale locale)
    {
        return getCalendarField(date, Calendar.YEAR, tz, locale);
    }

    /**
        Sets the calendar year (assuming a Gregorian calendar) for the given
        <code>Date</code>.
        @param date The date object to set the year in.
        @param year an integer representing the year of the date
        object. The year used should be a fully represented year. If
        you specify 99 you will get the year 99, rather than the year
        1999.

        @aribaapi documented
    */
    public static void setYear (Date date, int year)
    {
        setCalendarField(date, Calendar.YEAR, year);
    }

    /**
        Sets the calendar year (assuming a Gregorian calendar) for the given
        <code>Date, TimeZone, and Locale</code>.
        @param date The date object to set the year in.
        @param year an integer representing the year of the date
        object. The year used should be a fully represented year. If
        you specify 99 you will get the year 99, rather than the year
        1999.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar

        @aribaapi documented
    */
    public static void setYear (Date date, int year, TimeZone tz, Locale locale)
    {
        setCalendarField(date, Calendar.YEAR, year, tz, locale);
    }
    /*-- Month --------------------------------------------------------------*/

    /**
        @deprecated  replaced by <code>static getMonth</code>
        @aribaapi private
    */
    public int getMonth ()
    {
        return getMonth(this);
    }

    /**
        @deprecated  replaced by <code>static setMonth(Date, int)</code>
        @aribaapi private
    */
    public void setMonth (int month)
    {
        setMonth(this, month);
    }

    /**
        Returns a zero-based number which represents the calendar month
        (assuming a Gregorian calendar) for the given <code>Date</code>.
        @param date the date to return the month from.
        @return an int from 0-11 representing the month of the year
        @aribaapi documented
    */
    public static int getMonth (Date date)
    {
        return getCalendarField(date, Calendar.MONTH);
    }

    /**
        Returns a zero-based number which represents the calendar month
        (assuming a Gregorian calendar) for the given <code>Date</code>.
        @param date the date to return the month from.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @return an int from 0-11 representing the month of the year

        @aribaapi documented
    */
    public static int getMonth (java.util.Date date, TimeZone tz, Locale locale)
    {
        return getCalendarField(date, Calendar.MONTH, tz, locale);
    }

    /**
        Sets the calendar month (assuming a Gregorian calendar) for the given
        <code>Date</code>.  The value given should be zero-based, i.e.
        January = 0, December = 11.
        @param date the date object to set the month in.
        @param month an int from 0-11 representing the month to set.
        @aribaapi documented
    */
    public static void setMonth (Date date, int month)
    {
        setCalendarField(date, Calendar.MONTH, month);
    }

    /**
        Sets the calendar month (assuming a Gregorian calendar) for the given
        <code>Date, TimeZone and Locale</code>.  The value given should be zero-based, i.e.
        January = 0, December = 11.
        @param date the date object to set the month in.
        @param month an int from 0-11 representing the month to set.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @aribaapi documented
    */
    public static void setMonth (Date date, int month, TimeZone tz, Locale locale)
    {
        setCalendarField(date, Calendar.MONTH, month, tz, locale);
    }
    /*-- Day Of Month (Date) ------------------------------------------------*/

    /**
        @deprecated  replaced by <code>static getDayOfMonth</code>
        @aribaapi private
    */
    public int getDate ()
    {
        return getDayOfMonth(this);
    }

    /**
        @deprecated  replaced by <code>static setDayOfMonth(Date, int)</code>
        @aribaapi private
    */
    public void setDate (int date)
    {
        setDayOfMonth(this, date);
    }

    /**
        Returns a one-based number which represents the day of month (assuming
        a Gregorian calendar) for the given <code>Date</code>.
        @param date The date object to retrieve the day of month from.
        @return an int from 1-31 of the day of the month
        @aribaapi documented
    */
    public static int getDayOfMonth (Date date)
    {
        return getCalendarField(date, Calendar.DAY_OF_MONTH);
    }

    /**
        Returns a one-based number which represents the day of month (assuming
        a Gregorian calendar) for the given <code>Date</code>.
        @param date The date to get the day of the month from.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @return an int from 1-31 of the day of the month

        @aribaapi documented
    */
    public static int getDayOfMonth (java.util.Date date, TimeZone tz, Locale locale)
    {
        return getCalendarField(date, Calendar.DAY_OF_MONTH, tz, locale);
    }

    /**
        Sets the day of month (assuming a Gregorian calendar) for the given
        <code>Date</code>.  The value given should be one-based, so a value
        of 1 represents the first day of the month.
        @param date The date to set the day of the month in.
        @param dayOfMonth an int from 1-31 of the day of the month

        @aribaapi documented
    */
    public static void setDayOfMonth (Date date, int dayOfMonth)
    {
        setCalendarField(date, Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    /**
        Sets the day of month (assuming a Gregorian calendar) for the given
        <code>Date, TimeZone and Locale</code>.  The value given should be one-based, so a value
        of 1 represents the first day of the month.
        @param date The date to set the day of the month in.
        @param dayOfMonth an int from 1-31 of the day of the month
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar

        @aribaapi documented
    */
    public static void setDayOfMonth (Date date, int dayOfMonth,
                                      TimeZone tz, Locale locale)
    {
        setCalendarField(date, Calendar.DAY_OF_MONTH, dayOfMonth, tz, locale);
    }
    /*-- Day Of Week (Day) --------------------------------------------------*/

    /**
        @deprecated  replaced by <code>static getDayOfWeek</code>
        @aribaapi private
    */
    public int getDay ()
    {
        return getDayOfWeek(this)-1;
    }

    /**
        @deprecated  replaced by <code>static setDayOfWeek(Date, int)</code>
        @aribaapi private
    */
    public void setDay (int day)
    {
        setDayOfWeek(this, day);
    }

    /**
        Returns a one-based number which represents the day of the
        week (assuming a Gregorian calendar) for the given
        <code>Date</code>. This method should be used with caution as
        different locales determine different days as the first day of
        the week.
        @param date the date to return the day of the week from.
        @return int an int from 1-7 representing the day of the week.
        @aribaapi documented
    */
    public static int getDayOfWeek (Date date)
    {
        return getCalendarField(date, Calendar.DAY_OF_WEEK);
    }

    /**
        Returns a one-based number which represents the day of the
        week (assuming a Gregorian calendar) for the given
        <code>Date</code>. This method should be used with caution as
        different locales determine different days as the first day of
        the week.
        @param date the date to return the day of the week from.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @return int an int from 1-7 representing the day of the week.

        @aribaapi documented
    */
    public static int getDayOfWeek (Date date, TimeZone tz, Locale locale)
    {
        return getCalendarField(date, Calendar.DAY_OF_WEEK, tz, locale);
    }

    /**
        Sets the day of the week (assuming a Gregorian calendar) for
        the given <code>Date</code>.  The value given should be
        one-based, so a value of 1 represents the first day of the
        week. This method should be used with caution as different
        locales determine different days as the first day of the week.
        @param date the date to set the day of the week from.
        @param dayOfWeek an int from 1-7 representing the day of the week.
        @aribaapi documented
    */
    public static void setDayOfWeek (Date date, int dayOfWeek)
    {
        setCalendarField(date, Calendar.DAY_OF_WEEK, dayOfWeek);
    }

    /**
        Sets the day of the week (assuming a Gregorian calendar) for
        the given <code>Date, TimeZone, and Locale</code>.  The value given should be
        one-based, so a value of 1 represents the first day of the
        week. This method should be used with caution as different
        locales determine different days as the first day of the week.
        @param date the date to set the day of the week from.
        @param dayOfWeek an int from 1-7 representing the day of the week.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @aribaapi documented
    */
    public static void setDayOfWeek (Date date, int dayOfWeek,
                                     TimeZone tz, Locale locale)

    {
        setCalendarField(date, Calendar.DAY_OF_WEEK, dayOfWeek, tz, locale);
    }

    /*-- Hours --------------------------------------------------------------*/

    /**
        @deprecated  replaced by <code>static getHours</code>
        @aribaapi private
    */
    public int getHours ()
    {
        return getHours(this);
    }

    /**
        @deprecated  replaced by <code>static setHours(Date, int)</code>
        @aribaapi private
    */
    public void setHours (int hours)
    {
        setHours(this, hours);
    }

    /**
        Returns the hours from midnight for the given <code>Date</code>.
        @param date The object to get the number of hours from.
        @return The number of hours past the start of the day.
        @aribaapi documented
    */
    public static int getHours (Date date)
    {
        return getCalendarField(date, Calendar.HOUR_OF_DAY);
    }

    /**
        Returns the hours from midnight for the given <code>Date</code>.
        @param date The object to get the number of hours from.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @return The number of hours past the start of the day.
        @aribaapi documented
    */
    public static int getHours (Date date, TimeZone tz, Locale locale)
    {
        return getCalendarField(date, Calendar.HOUR_OF_DAY, tz, locale);
    }

    /**
        Sets the hours from midnight for the given <code>Date</code>.
        @param date The object to set the number of hours in.
        @param hours The number of hours past the start of the day.
        @aribaapi documented
    */
    public static void setHours (Date date, int hours)
    {
        setCalendarField(date, Calendar.HOUR_OF_DAY, hours);
    }

    /**
        Sets the hours from midnight for the given <code>Date, TimeZone and Locale</code>.
        @param date The object to set the number of hours in.
        @param hours The number of hours past the start of the day.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @aribaapi documented
    */
    public static void setHours (Date date, int hours, TimeZone tz, Locale locale)
    {
        setCalendarField(date, Calendar.HOUR_OF_DAY, hours, tz, locale);

    }
    /*-- Hours --------------------------------------------------------------*/

    /**
        @deprecated  replaced by <code>static getMinutes</code>
        @aribaapi private
    */
    public int getMinutes ()
    {
        return getMinutes(this);
    }

    /**
        @deprecated  replaced by <code>static setMinutes(Date, int)</code>
        @aribaapi private
    */
    public void setMinutes (int minutes)
    {
        setMinutes(this, minutes);
    }

    /**
        Returns the minutes portion of the time for the given
        <code>Date</code>.
        @param date The object to get the number of minutes from.
        @return The number of minutes past the hour.
        @aribaapi documented
    */
    public static int getMinutes (Date date)
    {
        return getCalendarField(date, Calendar.MINUTE);
    }

    /**
        Sets the minutes portion of the time for the given <code>Date</code>.
        @param date The object to modify the number of minutes in.
        @param minutes The number of minutes past the hour to set.
        @aribaapi documented
    */
    public static void setMinutes (Date date, int minutes)
    {
        setCalendarField(date, Calendar.MINUTE, minutes);
    }

    /**
        Sets the minutes portion of the time for the given <code>Date</code>.
        @param date The object to modify the number of minutes in.
        @param minutes The number of minutes past the hour to set.
        @aribaapi documented
    */
    public static void setMinutes (Date date, int minutes, TimeZone tz, Locale locale)
    {
        setCalendarField(date, Calendar.MINUTE, minutes, tz, locale);
    }

    /**
        Returns the minutes past the hour for the given <code>Date</code>.
        @param date The object to get the number of hours from.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @return The number of minutes past the hour.
        @aribaapi documented
    */
    public static int getMinutes (Date date, TimeZone tz, Locale locale)
    {
        return getCalendarField(date, Calendar.MINUTE, tz, locale);
    }

    /*-- Hours --------------------------------------------------------------*/

    /**
        @deprecated  replaced by <code>static getSeconds</code>
        @aribaapi private
    */
    public int getSeconds ()
    {
        return getSeconds(this);
    }

    /**
        @deprecated  replaced by <code>static setSeconds(Date, int)</code>
        @aribaapi private
    */
    public void setSeconds (int seconds)
    {
        setSeconds(this, seconds);
    }

    /**
        Returns the seconds portion of the time for the given
        <code>Date</code>.
        @param date The object to return the number of seconds from.
        @return The number of seconds past the minute.
        @aribaapi documented
    */
    public static int getSeconds (Date date)
    {
        return getCalendarField(date, Calendar.SECOND);
    }

    /**
        Sets the seconds portion of the time for the given <code>Date</code>.
        @param date The object to modify the number of seconds in.
        @param seconds The number of seconds past the minute to set.
        @aribaapi documented
    */
    public static void setSeconds (Date date, int seconds)
    {
        setCalendarField(date, Calendar.SECOND, seconds);
    }

    /*-- Month/Day Arithmetic -----------------------------------------------*/

    /**
        Adds a specified number of years to the given <code>Date</code>.
        @param date The <code>Date</code> object to modify.
        @param yearsToAdd an integer of the number of years to add.
        @aribaapi documented
    */
    public static void addYears (Date date, int yearsToAdd)
    {
        addToCalendarField(date, yearsToAdd, Calendar.YEAR);
    }

    /**
        Adds a specified number of years to the given <code>Date, TimeZone, and Locale</code>.
        @param date The <code>Date</code> object to modify.
        @param yearsToAdd an integer of the number of years to add.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @aribaapi documented
    */
    public static void addYears (Date date, int yearsToAdd,
                                 TimeZone tz, Locale locale)
    {
        addToCalendarField(date, yearsToAdd, Calendar.YEAR, tz, locale);
    }

    /**
        Adds a specified number of months to the given <code>Date</code>.
        @param date The <code>Date</code> object to modify.
        @param monthsToAdd an integer of the number of months to add.

        @aribaapi documented
    */
    public static void addMonths (Date date, int monthsToAdd)
    {
        addToCalendarField(date, monthsToAdd, Calendar.MONTH);
    }

    /**
        Adds a specified number of months to the given <code>Date, TimeZone and Locale</code>.
        @param date The <code>Date</code> object to modify.
        @param monthsToAdd an integer of the number of months to add.
        @param tz TimeZone of the calendar
        @param locale Locale of the calendar
        @aribaapi documented
    */
    public static void addMonths (Date date, int monthsToAdd,
                                TimeZone tz, Locale locale)
    {
        addToCalendarField(date, monthsToAdd, Calendar.MONTH, tz, locale);
    }

    /**
        Adds a specified number of days to the given <code>Date</code>.
        @param date The <code>Date</code> object to modify.
        @param daysToAdd an integer of the number of days to add.
        @aribaapi documented
    */
    public static void addDays (Date date, int daysToAdd)
    {
        addToCalendarField(date, daysToAdd, Calendar.DATE);
    }

    /**
        Adds a specified number of days to the given <code>Date</code>.
        @param date The <code>Date</code> object to modify.
        @param daysToAdd an integer of the number of days to add.
        @param tz TimeZone of the calendar used to add days
        @param locale Locale of the calendar used to add days
        @aribaapi documented
    */
    public static void addDays (Date date, int daysToAdd,
                                TimeZone tz, Locale locale)
    {
        addToCalendarField(date, daysToAdd, Calendar.DATE, tz, locale);
    }


    /**
        Adds a specified number of days to the given
        <code>Date</code>.  If you know that the time is an exact
        number of days, call the method with an int instead.

        @param date The <code>Date</code> object to modify.
        @param daysToAdd a double of the number of days to add. If
        fractional the time of day will also be changed rather than
        just the day of the month.
        @aribaapi documented
    */
    public static void addDays (Date date, double daysToAdd)
    {
            // must deal with fractional days
        date.setTime(date.getTime() + ((long)(MillisPerDay * daysToAdd)));
    }

    /**
        Adds a specified number of minutes to the given <code>Date</code>.
        @param date The <code>Date</code> object to modify.
        @param minutesToAdd an integer of the number of minutes to add.
        @aribaapi documented
    */
    public static void addMinutes (Date date, int minutesToAdd)
    {
        addToCalendarField(date, minutesToAdd, Calendar.MINUTE);
    }
    
    private static void addToCalendarField (Date date, int number, int field)
    {
            // get the appropriate Calendar instance
        Calendar calendar = getCalendarInstance(date.calendarDate());
        synchronized (calendar) {
            clearCalendarFields(calendar);
            updateCalendarFromDate(calendar, date);

                // add the given amount to the specified calendar field
            calendar.add(field, number);
            updateDateFromCalendar(date, calendar);
        }
    }

    private static void addToCalendarField (Date date, int number, int field,
                                            TimeZone tz, Locale locale)
    {
        Calendar calendar = getCalendarInstance(date.calendarDate(), tz, locale);
        synchronized (calendar) {
            clearCalendarFields(calendar);
            updateCalendarFromDate(calendar, date);

                // add the given amount to the specified calendar field
            calendar.add(field, number);
            updateDateFromCalendar(date, calendar);
        }
    }

    /**
        Returns true if the two <code>Date</code> objects are on the
        same calendar day (assuming a Gregorian calendar). The two
        dates must either both be calendar dates or must both not be
        calendar dates. This uses DateFormatter.getDefaultTimeZone to
        determine the time zone.

        @param date1 the first <code>Date</code> to compare
        @param date2 the second <code>Date</code> to compare
        @return      <code>true</code> of the given dates fall on the
                     same calendar day, <code>false</code> otherwise

        @aribaapi documented
    */
    public static boolean sameDay (Date date1, Date date2)
    {
        return sameDay(date1, date2, DateFormatter.getDefaultTimeZone());
    }

    /**
        Returns true if the two <code>Date</code> objects are on the
        same calendar day (assuming a Gregorian calendar). The two
        dates must either both be calendar dates or must both not be
        calendar dates. This version allows the caller to private a
        <code>TimeZone</code>


        @param date1 the first <code>Date</code> to compare
        @param date2 the second <code>Date</code> to compare
        @param timeZone the <code>TimeZone</code> to use for comparison
        @return      <code>true</code> of the given dates fall on the
                     same calendar day, <code>false</code> otherwise

        @aribaapi documented
    */
    public static boolean sameDay (Date date1, Date date2, TimeZone timeZone)
    {
            /*
                If we have a calendarDate and a non calendarDate, then we just convert
                the non calendarDate to a calendar date to compare. This is useful for
                comparing if now is on the same day as the calendarDate.
        
                When comparing calendar dates, we override timeZone
                parameter and use the GMT TimeZone
            */
        Date d1 = date1;
        Date d2 = date2;
        
        if (d1.calendarDate() || d2.calendarDate()) {
            d1 = d1.makeCalendarDate();
            d2 = d2.makeCalendarDate();
            timeZone = getTimeZoneFromOffset(0);
        }

            // get appropriate Calendar instances
        Locale locale = ResourceService.getService().getLocale();
        Calendar cal1 = Calendar.getInstance(timeZone, locale);
        Calendar cal2 = Calendar.getInstance(timeZone, locale);

            // set the calendar times from ourselves and the give date
        updateCalendarFromDate(cal1, d1);
        updateCalendarFromDate(cal2, d2);

            // check that year, month, date are the same
        if (cal1.get(Calendar.DATE) == cal2.get(Calendar.DATE)) {
            if (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)) {
                if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
        Sets the number of hours, minutes, and seconds past midnight for
        the given <code>Date</code>.  The default timezone is used to
        determine when midnight occurs.

        @param date the <code>Date</code> to modify
        @param time the <code>Date</code> object that provides the hour, minute,
        and second information to set
        @aribaapi documented
    */
    public static void setHoursMinutesSeconds (Date date, Date time)
    {
        int hours = Date.getHours(time);
        int mins = Date.getMinutes(time);
        int secs = Date.getSeconds(time);
        setHoursMinutesSeconds(date, hours, mins, secs);
    }

    /**
        Sets the number of hours, minutes, and seconds past midnight for
        the given <code>Date</code>.  The default timezone is used to
        determine when midnight occurs.

        @param date the <code>Date</code> to modify
        @param h    the number of hours since midnight
        @param m    the number of minutes past the hour
        @param s    the number of seconds past the minute

        @aribaapi documented
    */
    public static void setHoursMinutesSeconds (Date date, int h, int m, int s)
    {
        setHoursMinutesSeconds(date, h, m, s,
                               DateFormatter.getDefaultTimeZone());
    }

    /**
        Sets the number of hours, minutes, and seconds past midnight for
        the given <code>Date</code> and <code>TimeZone</code>.

        @param date the <code>Date</code> to modify
        @param h    the number of hours since midnight
        @param m    the number of minutes past the hour
        @param s    the number of seconds past the minute
        @param timeZone the timezone to use for determining midnight

        @aribaapi documented
    */
    public static void setHoursMinutesSeconds (Date date, int h, int m, int s,
                                               TimeZone timeZone)
    {
            // get the appropriate Calendar instance
        Calendar calendar = getCalendarInstance(date.calendarDate(), timeZone);
        synchronized (calendar) {
            clearCalendarFields(calendar);
            updateCalendarFromDate(calendar, date);
                // clear the hours, minutes, seconds, and milliseconds
            calendar.set(Calendar.HOUR_OF_DAY, h);
            calendar.set(Calendar.MINUTE, m);
            calendar.set(Calendar.SECOND, s);
            calendar.clear(Calendar.MILLISECOND);
                // update the date
            updateDateFromCalendar(date, calendar);
        }
    }

    /**
        Sets the hours, minutes, and seconds past midnight for the given
        <code>Date</code> to zero.  The default timezone is used to determine
        when midnight occurs.
        @param date the Date object to clear the hours, minutes and
        seconds in.
        @aribaapi documented
    */
    public static void setTimeToMidnight (Date date)
    {
        setHoursMinutesSeconds(date, 0, 0, 0);
    }

    /**
        Sets the hours, minutes, and seconds past midnight for the given
        <code>Date</code> to zero.
        @param date the Date object to clear the hours, minutes and
        seconds in.
        @param tz the timezone to make the calculations for.
        @aribaapi documented
    */
    public static void setTimeToMidnight (Date date, TimeZone tz)
    {
        setHoursMinutesSeconds(date, 0, 0, 0, tz);
    }

    private static final int[] MidnightZeroFields = {
        Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND
    };

    /**
        Returns true if the hours, minutes, and seconds past midnight for the
        given <code>Date</code> are all zero, i.e. the time is midnight.  We
        use the default timezone to determine when midnight occurs.
        @param date a date to check for 0 hours, minutes, seconds, millis
        @return true if and only if the hours, minutes, seconds, and
        millis are 0.
        @aribaapi documented
    */
    public static boolean timeIsMidnight (Date date)
    {
        return timeIsMidnight(date, DateFormatter.getDefaultTimeZone());
    }

    /**
        Returns true if the hours, minutes, and seconds past midnight for the
        given <code>Date</code> are all zero, i.e. the time is midnight,
        in the given <code>TimeZone</code>.

        @aribaapi private
    */
    public static boolean timeIsMidnight (Date date, TimeZone timeZone)
    {
        Calendar calendar = getCalendarInstance(date.calendarDate(), timeZone);
        synchronized (calendar) {
            clearCalendarFields(calendar);
            updateCalendarFromDate(calendar, date);

                // return true if hours, minutes, seconds are all zero
            for (int i = 0; i < MidnightZeroFields.length; i++) {
                if (calendar.get(MidnightZeroFields[i]) != 0) {
                    return false;
                }
            }
        return true;
        }
    }


    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/

    private static Calendar getCalendarInstance (boolean calendarDate)
    {
        return getCalendarInstance(calendarDate,
                                   DateFormatter.getDefaultTimeZone());
    }

    private static Calendar getCalendarInstance (
        boolean calendarDate,
        TimeZone tz)
    {
        Locale locale = ResourceService.getService().getLocale();
        return getCalendarInstance(calendarDate, tz, locale);
    }

    private static Calendar getCalendarInstance (
        boolean calendarDate,
        TimeZone tz,
        Locale   locale)
    {
        if (calendarDate) {
            tz = TimeZone.getTimeZone("GMT");
        }

        return getCalendarInstance(tz, locale);
    }

    private static GrowOnlyHashtable localeToDisplayNameCache = new GrowOnlyHashtable();
    private static String getLocaleDisplayName (Locale locale)
    {
        String name = (String)localeToDisplayNameCache.get(locale);
        if (name == null) {
            name = (String)localeToDisplayNameCache.get(locale);
            if (name == null) {
                name =  locale.getDisplayName();
                localeToDisplayNameCache.put(locale, name);
            }
        }
        return name;

    }

    /**
        Returns a Calendar instance to be used. The instance is from a
        internal cache to improve efficiency. All subsequent access of
        the returned instance should be protected against concurrent
        access from different threads.
        @param timeZone the time zone to be used.
        @param locale the locale to be used.
        @return the calendar instance.
    */
    private static Calendar getCalendarInstance (TimeZone timeZone,
                                                 Locale   locale)
    {
        Assert.that(timeZone != null, "Null TimeZone in getCalendarInstance()");
        Assert.that(locale != null, "Null Locale in getCalendarInstance()");

        String hashKey = StringUtil.strcat(getLocaleDisplayName(locale),
                                           timeZone.getID());

            // Since cache is a GrowOnlyHashtable, we don't need to
            // synchronize on get or put
        Calendar cal = (Calendar)CALENDAR_CACHE.get(hashKey);
        if (cal != null) {
            return cal;
        }

        cal = Calendar.getInstance(timeZone, locale);
        CALENDAR_CACHE.put(hashKey, cal);

        return cal;
    }

    /**
        Clears the internal fields of the given calendar instance. This should
        be called after getCalendarInstance is called because apparently
        jkd13 does not clear the fields properly. (jdk14 is okay though.) So
        we have to do this. We don't call java.util.Calendar.clear() because
        that method unnecessarily create new objects, resulting in unnecessary
        garbage to be collected.

        @param cal the calendar whose fields are to be cleared.
    */
    private static void clearCalendarFields (Calendar cal)
    {
        for (int i=0; i<Calendar.FIELD_COUNT; i++) {
            cal.clear(i);
        }

        /*  Note: need to explicitly do a get on the fields to get
            around a IBM 1.4.1 VM for AIX bug. Apparently, the fields
            do not get evaluated when they should be some time later.
            This caused application code to get back a bad object,
            possibly a "null" Date object which the app may try to
            insert into a DB column where null is not expected.

            Problem observed only for IBM 1.4.1 on AIX. (IBM 1.4.1
            on Windows work fine with the need to do an explict get,
            for example.
        */
        for (int i=0; i<Calendar.FIELD_COUNT; i++) {
            cal.get(i);
        }
    }

    static boolean calendarDate (java.util.Date date)
    {
        return (date instanceof ariba.util.core.Date &&
                ((ariba.util.core.Date)date).calendarDate());
    }

    private static int getCalendarField (java.util.Date date, int field)
    {
            // get a Calendar instance
        Calendar calendar = getCalendarInstance(calendarDate(date));
        synchronized (calendar) {
            clearCalendarFields(calendar);
            updateCalendarFromDate(calendar, date);

                // get the field
            return calendar.get(field);
        }
    }

    private static int getCalendarField (java.util.Date date, int field,
                            TimeZone tz, Locale locale)
    {
            // get a Calendar instance
        Calendar calendar =
            getCalendarInstance(calendarDate(date), tz, locale);
        synchronized (calendar) {
            clearCalendarFields(calendar);
            updateCalendarFromDate(calendar, date);

                // get the field
            return calendar.get(field);
        }
    }

    private static void setCalendarField (Date date, int field, int value)
    {
        setCalendarField(date, field, value,
                         DateFormatter.getDefaultTimeZone(),
                         ResourceService.getService().getLocale());
    }

    private static void setCalendarField (Date date, int field, int value,
                                          TimeZone tz, Locale locale)
    {
            // get a Calendar instance
        Calendar calendar = getCalendarInstance(date.calendarDate(),
                                                tz, locale);
        synchronized (calendar) {
            clearCalendarFields(calendar);
            updateCalendarFromDate(calendar, date);

                // NOTE: if we move to JDK 1.4, this is a potential bug in
                // 1.4. We might need to add these lines of code. See bug ID
                // 4860664 in javasoft's bug database.
                //            if (Calendar.DAY_OF_WEEK == field) {
                //                calendar.get(field);
                //            }

                // set the field, update the date
            calendar.set(field, value);
            updateDateFromCalendar(date, calendar);
        }
    }

    private static void updateCalendarFromDate (Calendar cal, java.util.Date date)
    {
        cal.setTime(date);

            // NOTE - this is a hack to work around a JDK 1.1.3 bug; none of
            // the internal fields for the GregorianCalendar get set when
            // setTime() above is called; this tickles the caluclation
        cal.get(Calendar.YEAR);
    }

    private static void updateDateFromCalendar (java.util.Date date, Calendar cal)
    {
        date.setTime(cal.getTime().getTime());
    }


    /*-----------------------------------------------------------------------
        Externalizable Interface
      -----------------------------------------------------------------------*/

    /**
        Writes the contents of this Date object.

        @serialData calendarDate property of type boolean; getTime() value of
                    type long
        @param output the stream to write the object to
        @exception IOException Includes any I/O exceptions that may occur
        @aribaapi private
    */
    public void writeExternal (ObjectOutput output) throws IOException
    {
            // don't call superclass because the super class does not
            // implement Externalizable
        output.writeBoolean(calendarDate());
        output.writeLong(getTime());
    }

    /**
        Restores the contents of this Date object.

        @param input the stream to read data from in order to restore the object
        @exception IOException if I/O errors occur
        @exception ClassNotFoundException If the class for an object being
        @aribaapi private
    */
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
            // don't call superclass because the super class does not
            // implement Externalizable
        calendarDate = input.readBoolean();
        setTime(input.readLong());
    }
}


