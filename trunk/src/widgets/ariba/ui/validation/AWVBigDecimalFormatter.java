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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVBigDecimalFormatter.java#6 $
*/

package ariba.ui.validation;

import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.formatter.DecimalParseInfo;
import ariba.ui.aribaweb.util.AWFormatter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;

public final class AWVBigDecimalFormatter extends AWFormatter
{
    private DecimalFormat _decimalFormat;
    private final int _scale;
    private final String _formatPattern;
    private Locale _locale;

    public AWVBigDecimalFormatter (String formatPattern, int scale, Locale locale)
    {
        super();
        _formatPattern = formatPattern;
        _scale = scale;
        _locale = locale;

        if (locale != null) {
           _decimalFormat = (DecimalFormat)DecimalFormat.getInstance(locale);
        }
        else {
            _decimalFormat = new DecimalFormat(_formatPattern);
        }

        if (_formatPattern != null) {
            _decimalFormat.applyPattern(_formatPattern);
        }

    }

    /**
        Returns a formatted string for this BigDecimal <code>bigDecimalValue</code> in
        the locale specified in the constructor.  Trailing decimal zeros are removed from the
        string.

        @param bigDecimalValue <code>bigDecimalValue</code> to convert to a string value

        @return Returns a string representation of the <code>bigDecimalValue</code>
    */
    public String format (Object bigDecimalValue)
    {
        // coerce non-BigDecimal Number objects to something we can format
        if (!(bigDecimalValue instanceof BigDecimal) && (bigDecimalValue instanceof Number)) {
            bigDecimalValue = new BigDecimal(((Number)bigDecimalValue).doubleValue());
        }

        return BigDecimalFormatter.getStringValue((BigDecimal)bigDecimalValue, _scale, _locale, _decimalFormat);
    }

    /**
        Returns a big decimal object for this String <code>stringToParse</code>

        @param stringToParse <code>stringToParse</code>

        @return Returns a Object representation of the <code>stringToParse</code>
    */
    public Object parseObject (String stringToParse) throws ParseException
    {
        Object bigDecimal = null;
        DecimalParseInfo decimalParseInfo = null;
        if ((stringToParse != null) && (stringToParse.length() > 0)) {
            decimalParseInfo = BigDecimalFormatter.parseBigDecimal(stringToParse, _decimalFormat);
            bigDecimal =  decimalParseInfo.number;
        }
        return bigDecimal;
    }
}
