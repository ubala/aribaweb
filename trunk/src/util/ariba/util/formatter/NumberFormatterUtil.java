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

    $Id: //ariba/platform/util/core/ariba/util/formatter/NumberFormatterUtil.java#5 $
*/

package ariba.util.formatter;

import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.StringUtil;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
    @aribaapi private
*/
public class NumberFormatterUtil
{
    /**
        @aribaapi private
    */

    public static DecimalFormat getFormat (MultiKeyHashtable cache, Locale locale,
                                           String pattern)
    {
            // make sure our pattern key is non-null
        pattern = (pattern == null) ? "" : pattern;

        synchronized (cache) {
                // check the cache first
            DecimalFormat fmt = (DecimalFormat)cache.get(locale, pattern);
            if (fmt != null) {
                return fmt;
            }
            
                // create one if cache missed, set the pattern
            fmt = (DecimalFormat)NumberFormat.getNumberInstance(locale);
            if (!StringUtil.nullOrEmptyString(pattern)) {
                fmt.applyLocalizedPattern(pattern);
            }
            
                // cache it & return
            cache.put(locale, pattern, fmt);
            return fmt;
        }
    }
}
