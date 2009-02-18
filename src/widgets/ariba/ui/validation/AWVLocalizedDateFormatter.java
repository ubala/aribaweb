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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVLocalizedDateFormatter.java#5 $
*/
package ariba.ui.validation;

import ariba.ui.aribaweb.util.AWFormatter;
import ariba.util.core.Fmt;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import ariba.util.formatter.DateFormatter;

import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.text.ParseException;

public abstract class AWVLocalizedDateFormatter extends  AWFormatter
{
    private TimeZone _timeZone;
    private Locale _locale;

    public AWVLocalizedDateFormatter (Locale locale, TimeZone timeZone)
    {
        _locale = locale;
        _timeZone = timeZone;
    }

    public Object parseObject (String stringToParse) throws ParseException
    {
        if (StringUtil.nullOrEmptyOrBlankString(stringToParse)) return null;
        Date date =  DateFormatter.parseDateUsingFormats(stringToParse, _locale,
                getFormatsKey(), _timeZone, true);

        if (date == null) {
            throw new ParseException(Fmt.S("Couldn't parse date '%s'", stringToParse), 0);
        }
        return date;
    }

    abstract public String getFormatsKey ();

    public String format (Object objectToFormat)
    {
        Assert.that(objectToFormat instanceof Date, "object to Format must be Date");
        Date date = (Date)objectToFormat;
        return DateFormatter.getStringValueUsingFormats(date, getFormatsKey(), _locale, _timeZone);
    }
}
