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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVDateFormatter.java#8 $
*/

package ariba.ui.validation;

import ariba.util.formatter.DateFormatter;
import ariba.ui.aribaweb.util.AWFormatter;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Wrapper class around util's DateFormatter.  Remembers a format string and locale...
 */
public final class AWVDateFormatter extends AWFormatter
{
    private final String _formatString;
    private final Locale _locale;
    private final TimeZone _timeZone;

    public AWVDateFormatter (String format, Locale locale, TimeZone timeZone)
    {
        _formatString = format;
        _locale = locale;
        _timeZone = timeZone;
        Assert.that(_locale != null, "locale must not be null");
        Assert.that(_timeZone != null, "timeZone must not be null");
    }

    public Object parseObject (String stringToParse) throws ParseException
    {
        return parseObject(stringToParse, false);
    }

    public Object parseObject (String stringToParse, boolean calendarDate) throws ParseException
    {
        if (StringUtil.nullOrEmptyString(stringToParse)) {
            return null;
        }

        Object date = null;
        if (_formatString == null) {
            date = DateFormatter.parseDate(stringToParse, _locale, _timeZone, calendarDate);
        }
        else {
            date = DateFormatter.parseDate(stringToParse, _formatString, _locale, _timeZone, calendarDate);
        }
        return date;
    }

    public String format (Object objectToFormat)
    {
        if (objectToFormat == null) {
            return null;
        }

        String formattedString = null;
        if (_formatString == null) {
            formattedString = DateFormatter.getStringValue((Date)objectToFormat, _locale, _timeZone);
        }
        else {
            formattedString = DateFormatter.getStringValue((Date)objectToFormat, _formatString, _locale, _timeZone);
        }
        return formattedString;
    }
}
