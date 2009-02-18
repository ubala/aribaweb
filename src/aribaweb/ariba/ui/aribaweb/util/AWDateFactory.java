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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWDateFactory.java#4 $
*/

package ariba.ui.aribaweb.util;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;

/**
 * Provided as a means for users of AW Date manipulators to
 * create their own date objects.
 */
public interface AWDateFactory
{
    // Create the current date
    public Date createDate ();

    // Create a date with the local calendar
    public Date createDate (int year, int month, int day);

    // Create a date with the calendar in the specified timezone and locale
    public Date createDate (int year, int month, int day, TimeZone timezone, Locale locale);

    // Get absolute year with the calendar in the specified timezone and locale.
    // Example, 1973, not 73.
    public int getYear (java.util.Date date, TimeZone timezone, Locale locale);

    // Get month with the calendar in the specified timezone and locale.
    public int getMonth (java.util.Date date, TimeZone timezone, Locale locale);

    // Get day of month with the calendar in the specified timezone and locale.
    public int getDayOfMonth (java.util.Date date, TimeZone timezone, Locale locale);

}
