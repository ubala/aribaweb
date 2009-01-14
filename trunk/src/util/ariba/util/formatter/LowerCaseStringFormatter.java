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

    $Id: //ariba/platform/util/core/ariba/util/formatter/LowerCaseStringFormatter.java#3 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import java.util.Locale;

/**
    <code>LowerCaseStringFormatter</code> is a subclass of 
    <code>StringFormatter</code> which is responsible for formatting, 
    parsing, and comparing strings. LowerCaseStringFormatter converts
    all strings to lower case.

    @aribaapi documented
*/
public class LowerCaseStringFormatter extends StringFormatter
{
    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Convert string to lower case. This method is invoked from the
        file channel infrastructure when we do an import/pull the loaded 
        object will be parsed using a specified formatter, in this case
        this one.

        @param  string the string to parse
        @param  locale the <code>Locale</code> to use for parsing (unused)
        @return        the lower case of <code>String</code> itself

        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(string)) {
            return string.toLowerCase();
        }

        return string;
    }

    /*-----------------------------------------------------------------------
        Equality Testing
      -----------------------------------------------------------------------*/

    /**
        Returns true if and only if the two objects should be considered equal
        in the given locale.  The values must be non-null <code>String</code>
        objects.  The locale-dependent lowercase forms of the two strings must
        match exactly to be considered equal.

        @param  o1     the first <code>String</code> to test for equality
        @param  o2     the second <code>String</code> to test for equality
        @param  locale the <code>Locale</code> to use for equality testing
        @return        <code>true</code> if the two <code>String</code> objects
                       are equal; <code>false</code> otherwise.
        @aribaapi documented
    */
    protected boolean objectsEqual (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof String, "invalid type");
        Assert.that(o2 instanceof String, "invalid type");
        String s1 = (String)o1;
        String s2 = (String)o2;

            // string must match in lowercase form
        return super.objectsEqual(s1.toLowerCase(locale), s2.toLowerCase(locale), locale);
    }

    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two objects for sorting purposes in the given locale.  The
        two objects must be non-null <code>String</code> objects.  Returns a
        value which is less than, equal to, or greater than zero depending on
        whether the first object sorts before, the same, or after the second
        object.  Uses the given <code>locale</code> to do the comparison.  The
        <code>locale</code> must be non-null.

        @param  o1     the first <code>String</code> to compare
        @param  o2     the second <code>String</code> to compare
        @param  locale the <code>Locale</code> to use for comparison
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi documented
    */
    protected int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof String, "invalid type");
        Assert.that(o2 instanceof String, "invalid type");
        String s1 = (String)o1;
        String s2 = (String)o2;

        return compareStrings(s1.toLowerCase(locale), s2.toLowerCase(locale), locale);
    }
}
