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

    $Id: //ariba/platform/util/core/ariba/util/core/DateUtil.java#5 $
*/

package ariba.util.core;

import java.util.Calendar;
import java.util.TimeZone;

/**
    @aribaapi ariba
*/ 
public final class DateUtil
{
    public static boolean beforeIgnoreTimeOfDay (Date d1, TimeZone tz1,
                                                 Date d2, TimeZone tz2)
    {
        return diffIgnoreTimeOfDay(d1, tz1, d2, tz2) < 0;
    }

    public static boolean afterIgnoreTimeOfDay (Date d1, TimeZone tz1,
                                                Date d2, TimeZone tz2)
    {
        return diffIgnoreTimeOfDay(d1, tz1, d2, tz2) > 0;
    }   

    public static boolean equalsIgnoreTimeOfDay (Date d1, TimeZone tz1,
                                                 Date d2, TimeZone tz2)
    {
        return diffIgnoreTimeOfDay(d1, tz1, d2, tz2) == 0;
    }   
    
    private static long diffIgnoreTimeOfDay (Date d1, TimeZone tz1,
                                             Date d2, TimeZone tz2)
    {
        Assert.that(d1 != null && d2 != null, "date is null");
        Assert.that(tz1 != null && tz2 != null, "timezone is null");
        
        Calendar c1 = Calendar.getInstance(tz1);
        c1.setTime(d1);
        
        Calendar c2 = Calendar.getInstance(tz2);
        c2.setTime(d2);
        
        return (c1.get(Calendar.YEAR) - c2.get(Calendar.YEAR)) * 10000 +
               (c1.get(Calendar.MONTH) - c2.get(Calendar.MONTH)) * 100 +
               (c1.get(Calendar.DATE) - c2.get(Calendar.DATE));
    }
    
}
