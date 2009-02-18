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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWTimeZone.java#11 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Date;
import ariba.util.formatter.IntegerFormatter;
import ariba.ui.aribaweb.core.AWConcreteApplication;

import java.lang.NumberFormatException;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.text.ParseException;

public final class AWTimeZone extends Object
{
    private static Map<String,TimeZone> DefaultTimeZonesByOffsetKeys;

    static public int STD_YEAR;
    static public int STD_MONTH = 1;
    static public int STD_DATE = 1;
    static public int DST_YEAR;
    static public int DST_MONTH = 7;
    static public int DST_DATE = STD_DATE;

    /**
        Given a key, looks up the corresponding Java timezone instance for that key.  The key is of the
        form:  <offset in millis from UTC for Feb.1>:<offset in millis from UTC for Aug.1>.  The use of
        Feb. 1 and Aug. 1 are because these two dates are guaranteed to be in different daylight-savings-time
        portions of a given timezone and so we can identify a given timezone by its daylight-savings-time offset
        and its standard-time offset taken together.
    */

    static {

        AWConcreteApplication app =
                (AWConcreteApplication)AWConcreteApplication.sharedInstance();
        // get preferred timezones from the application
        List preferredTZs = app.getPreferredTimezones();

        DefaultTimeZonesByOffsetKeys = createTimeZoneByOffSetMap(preferredTZs,false);
    }

    private static String makeTimeZoneKey(TimeZone timezone)
    {
        int febOffset = timezone.getOffset(1, STD_YEAR, STD_MONTH, STD_DATE, 7, 0);
        int augOffset = timezone.getOffset(1, DST_YEAR, DST_MONTH, DST_DATE, 6, 0);
        return Fmt.S("%s:%s", Constants.getInteger(febOffset), Constants.getInteger(augOffset));
    }

    private static TimeZone timeZoneFromOffsetKey (int offsetFeb, int offsetAug)
    {
        String offsetKey = Fmt.S("%s:%s", Constants.getInteger(offsetFeb),
                                 Constants.getInteger(offsetAug));
        return timeZoneFromOffsetKey(offsetKey);
    }

    /**
     * Create a map of offset keys to timezones.  Since multiple timezones can map to a particular offset key, the
     * preferredTimezones can be used to specify a preferred timezone for a given offset.  If no preferred timezone
     * is given, then the default is use the last timezone for a given offset based on the order from
     * java.util.TimeZone.getAvailableIDs().
     *
     * @param preferredTimezones null or list of preferred timezones (see java.util.Timezone.getID()
     * @param sparse if true, then only insert entries for preferred timezones.  If false, then create a full map
     * including all non-preferred timezones.
     * @return map of offset key to timezone
     */
    public static Map<String,TimeZone> createTimeZoneByOffSetMap (List preferredTimezones, boolean sparse)
    {
        Map<String,TimeZone> timezoneByOffsetKeys = new GrowOnlyHashtable();

        /*
            Changed the STD_YEAR to use the current year instead of hardcoding
            2002.  If you pass a  previous year to get the offset, java
            will return timezone as of that year, any changes to timezones the
            occured since then are not included.
            This change will work correctly for most cases execpt for changes
            to day light time saving changes that occur in countries in the
            southern hemisphere where the day light time saving period
            spans 2 calendar years.  The logic that assume the day light time zone
            between Feb 1 and Aug 1, might not be correct for those countries.
         */
        STD_YEAR = Calendar.getInstance().get(Calendar.YEAR);
        DST_YEAR = STD_YEAR;
        String[] timezones = TimeZone.getAvailableIDs();

        for (int i = 0; i < timezones.length; ++i) {
            // iterate through the list of timezones returned by the JVM and
            // construct "key" for the given timezone
            TimeZone timezone = TimeZone.getTimeZone(timezones[i]);
            int febOffset = timezone.getOffset(1, STD_YEAR, STD_MONTH, STD_DATE, 7, 0);
            int augOffset = timezone.getOffset(1, DST_YEAR, DST_MONTH, DST_DATE, 6, 0);
            String newKey = Fmt.S("%s:%s", Constants.getInteger(febOffset), Constants.getInteger(augOffset));
            // now insert key/timezone into the timezone map. If the key has already
            // been inserted (ie, a different timezone name has already been inserted
            // for the same timezone, GMT-8 is US/SanFrancisco or US/LosAngeles), then
            // check if the current value (ie timezone id) is a preferred value.  If not
            // then overwrite, if so, then keep the preferred value.
            // The "last entry" wins mechanism needs to be maintained for backward
            // compatibility, but we've added the extra "preferred timezone" concept.

            // Note that this caches the value at the system level and we'll need to
            // add a per-realm override and possibly a per-user override in the future.

            if (timezoneByOffsetKeys.containsKey(newKey)) {
                String currId = timezoneByOffsetKeys.get(newKey).getID();
                if (preferredTimezones == null || !preferredTimezones.contains(currId)) {
                    Log.aribaweb.debug("Overwriting existing timezone key: %s value: %s with new value: %s",
                            newKey, currId, timezones[i]);
                    timezoneByOffsetKeys.put(newKey, timezone);
                }
                else {
                    Log.aribaweb.debug("Preferred timezone not overwritten: %s", currId);
                }
            }
            else if (sparse) {
                if (preferredTimezones != null && preferredTimezones.contains(timezone.getID())) {
                    // for sparse maps, only insert preferred timezones
                    timezoneByOffsetKeys.put(newKey, timezone);
                }
            }
            else {
                // not sparse so create full map by inserting non-preferred timezones
                // last timezone from TimeZone.getAvailableIDs() will win for any given offset key
                timezoneByOffsetKeys.put(newKey, timezone);
            }
        }
        return timezoneByOffsetKeys;
    }

    public static TimeZone getPreferredTimeZone (TimeZone tz)
    {
        String offsetKey = makeTimeZoneKey(tz);
        return timeZoneFromOffsetKey(offsetKey);
    }

    public static TimeZone timeZoneFromOffsetKey (String offsetKey)
    {
        // Create a hashtable of timezones keyed by offset values of the form:
        // <offset in millis from GMT for Feb.1>:<offset in millis from GMT for Aug.1>
        // We choose Feb. 1 and Aug. 1 because we know that they will be in daylight
        // savings time and/or standard time for each time zone.
        // lookup the timezone and, if it doesn't exist, create one on the fly using the
        // default timezone
        if (offsetKey == null) {
            return TimeZone.getDefault();
        }

        Map<String,TimeZone> timezoneMap =
            ((AWConcreteApplication)AWConcreteApplication.sharedInstance()).getTimeZoneByOffsetKeys();

        if (timezoneMap == null) {
            timezoneMap = DefaultTimeZonesByOffsetKeys;
        }

        TimeZone timezone = timezoneMap.get(offsetKey);

        if (timezone == null) {
            // if it's not in the override, then check the default
            timezone = DefaultTimeZonesByOffsetKeys.get(offsetKey);
            if (timezone == null) {
                synchronized (DefaultTimeZonesByOffsetKeys) {
                    timezone = DefaultTimeZonesByOffsetKeys.get(offsetKey);
                    if (timezone == null) {
                        // parse out the Feb. 1 offset value in the offset key (it's the value
                        // to the left of the colon)
                        int indexOfColon = offsetKey.indexOf(':');
                        if (indexOfColon != -1) {
                            String feb1OffsetString = offsetKey.substring(0, indexOfColon);
                            try {
                                int feb1Offset = Integer.parseInt(feb1OffsetString);
                                timezone = new SimpleTimeZone(feb1Offset, feb1OffsetString);
                            }
                            catch (NumberFormatException e) {
                                // ill-formed offset key => should we let this throw
                                // propagate up?
                                e.printStackTrace();
                            }
                        }

                            // if timezone still equals null, then use default
                        if (timezone == null) {
                            timezone = TimeZone.getDefault();
                        }

                            // put the new timezone in the hashtable
                        DefaultTimeZonesByOffsetKeys.put(offsetKey, timezone);
                    }
                }
            }
        }
        return timezone;
    }


    public static TimeZone getTimeZone (String offsetStr, String offsetStrFeb,
                                        String offsetStrAug,
                                        boolean dayLightSavings)
    {
        TimeZone tz = null;

        try {

            // determine the timezone offset for the client,
            // using the key for the hidden input field for the client timezone
            // offset (set via JavaScript and passed in when the user logs in)

            Log.aribaweb.debug("timezone feb offset = %s, aug offset = %s",
                               offsetStrFeb, offsetStrAug);

            // javascript will give us the wrong polarity on the offset
            // reverse it to make it right
            int offsetNow = -IntegerFormatter.parseInt(offsetStr);

            // Workaround for defect # 34104
            // Basically, on Mac with IE 5.x, we are not getting
            // the timezone offset properly.  It does not return
            // daylight saving timezone offset.
            if (dayLightSavings) {
                offsetNow += 60;
                Log.aribaweb.debug("Adjust time zone for Mac for daylight saving time");
            }
            else {
                try {
                    // javascript will give us the wrong polarity on the offset
                    // reverse it to make it right.
                    int offsetFeb = -IntegerFormatter.parseInt(offsetStrFeb)*60*1000;
                    int offsetAug = -IntegerFormatter.parseInt(offsetStrAug)*60*1000;

                    tz = timeZoneFromOffsetKey(offsetFeb, offsetAug);
                    if (tz != null) {
                        Log.aribaweb.debug("found DST-aware timezone = %s", tz);
                    }
                }
                catch (ParseException e) {
                    Log.aribaweb.debug(
                        "problem parsing feb/aug timezone offsets: %s", e);
                }
            }

            if (tz == null) {
                Log.aribaweb.debug("create timezone from offsetNow = %s",
                                      Constants.getInteger(offsetNow));
                tz = Date.getTimeZoneFromOffset(offsetNow);
            }
        }
        catch (ParseException e) {
            Log.aribaweb.debug("problem parsing timezone offsetNow: %s", e);
        }

        if (tz == null) {
            Log.aribaweb.debug("timezone cannot be obtained. use default timezone");
            tz = TimeZone.getDefault();
        }

        return tz;
    }
}
