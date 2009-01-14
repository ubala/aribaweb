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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/PipedFormatter.java#8 $
*/
package ariba.ui.validation;

import ariba.util.core.Date;
import ariba.ui.aribaweb.util.AWFormatter;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.table.AWTSortOrdering;
import ariba.util.core.Compare;

import java.text.ParseException;

/**
 * This formatter pipes String object values *backward* through its
 * coercion formatter, and then forward through its data formatter.
 *
 * This is valuable when binding a UI field to an XML property that has
 * a canonical *string* representation for a number or date, than then needs
 * to be formatted by a conventional number or date formatter.
 */
public final class PipedFormatter extends AWFormatter implements Compare
{
    private Object _stringCoercionFormatter;
    private Object _dataFormatter;

    public PipedFormatter (Object stringCoercionFormatter, Object dataFormatter)
    {
        super();
        _stringCoercionFormatter = stringCoercionFormatter;
        _dataFormatter = dataFormatter;
    }

    public Object parseCanonicalXMLString (String xmlValue)
    {
        return AWFormatting.get(_stringCoercionFormatter).parseObject(_stringCoercionFormatter, xmlValue);
    }

    /**
        format the "object value" by first parsing the canonical string value, and then
        passing that object value through our data formatter.
    */
    public String format (Object objectValue)
    {
        Object coercedValue = (objectValue instanceof String)
            ? parseCanonicalXMLString((String)objectValue) : objectValue;
        return AWFormatting.get(_dataFormatter).format(_dataFormatter, coercedValue);
    }

    /**
        parse the presentation string by first parsing it into an objectValue, and
        then formatting that into a canonical string representation
    */
    public Object parseObject (String stringToParse) throws ParseException
    {
        Object objectValue = AWFormatting.get(_dataFormatter).parseObject(
            _dataFormatter, stringToParse);
        return AWFormatting.get(_stringCoercionFormatter).format(
            _dataFormatter, objectValue);
    }

    // For sorting, compare as objects rather than as strings
    public int compare(Object o1, Object o2) {
        Object v1 = (o1 instanceof String) ? parseCanonicalXMLString((String)o1) : o1;
        Object v2 = (o2 instanceof String) ? parseCanonicalXMLString((String)o2) : o2;
        return AWTSortOrdering.basicCompare(v1, v2);
    }
}
