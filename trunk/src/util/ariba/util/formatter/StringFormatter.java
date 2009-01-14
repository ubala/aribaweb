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

    $Id: //ariba/platform/util/core/ariba/util/formatter/StringFormatter.java#9 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.HTML;
import ariba.util.core.MapUtil;
import ariba.util.i18n.LocaleSupport;
import java.text.Collator;
import java.util.Locale;
import java.util.Map;

/**
    <code>StringFormatter</code> is a subclass of <code>Formatter</code> which
    is responsible for formatting, parsing, and comparing strings.

    @aribaapi documented
*/
public class StringFormatter extends Formatter
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.StringFormatter";

    /*-----------------------------------------------------------------------
        Static members
      -----------------------------------------------------------------------*/

    private static Map collators = MapUtil.map();

    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>StringFormatter</code>.

        @aribaapi private
    */
    public StringFormatter ()
    {
    }


    /*-----------------------------------------------------------------------
        Static Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two <code>String</code> values for sorting purposes.
        Returns an <code>int</code> value which is less than, equal
        to, or greater than zero depending on whether the first object
        sorts before, the same, or after the second object.  Uses the
        default locale to create a <code>Collator</code> instance to
        do the comparison.

        @param  s1 the first <code>String</code> to compare
        @param  s2 the second <code>String</code> to compare
        @return    <code>int</code> value which determines how the two strings
                   should be ordered
        @aribaapi documented
    */
    public static int compareStrings (String s1, String s2)
    {
        return compareStrings(s1, s2, getDefaultLocale());
    }

    /**
        Compares two <code>String</code> values for sorting purposes.  Returns
        an <code>int</code> value which is less than, equal to, or greater
        than zero depending on whether the first object sorts before, the
        same, or after the second object.  Uses the given <code>locale</code>
        to create a <code>Collator</code> instance to do the comparison.  The
        <code>locale</code> must be non-null.

        @param  s1 the first <code>String</code> to compare
        @param  s2 the second <code>String</code> to compare
        @param  locale the locale to do the comparison
        @return    <code>int</code> value which determines how the two strings
                   should be ordered
        @aribaapi documented
    */



    public static int compareStrings (String s1, String s2, Locale locale)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        if (s1 == s2) {
            return 0;
        }
        else if (s1 == null) {
            return -1;
        }
        else if (s2 == null) {
            return 1;
        }
        else {
            Collator c = (Collator)collators.get(locale);
            if (c == null) {
                c = Collator.getInstance(locale);
                c.setDecomposition(LocaleSupport.decompositionFlag(locale));
                collators.put(locale, c);
            }
            return c.compare(s1, s2);
        }
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the given
        locale..

        @param  object the object to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        the <code>String</code> itself
        @aribaapi documented
    */
    protected String formatObject (Object object, Locale locale)
    {
        return (object == null) ? "" : object.toString();
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Parses the given string into a <code>String</code> object.  For this
        subclass of <code>Formatter</code>, this method returns the
        given string, after converting any html escaped euro characters to unicode
        euro characters.  The <code>locale</code> parameter is currently unused.

        @param  string the string to parse
        @param  locale the <code>Locale</code> to use for parsing (unused)
        @return the <code>String</code> itself, with euro conversion
        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
    {
        // turn Euro characters (strange HTML escaping)
        // into proper unicode euro characters
        string = HTML.unescapeEuro(string);

        return string;
    }

    /**
        Returns a new <code>String</code> derived from the given object.  If
        the object is not a <code>String</code>, returns the result of calling
        the <code>toString</code> method on the object.  The
        <code>locale</code> parameter is currently unused.

        @param  object the object to convert to a <code>String</code>
        @param  locale the <code>Locale</code> to use for conversion (unused)
        @return        a <code>String</code> derived from the given object
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        return (object == null) ? "" : object.toString();
    }


    /*-----------------------------------------------------------------------
        Equality Testing
      -----------------------------------------------------------------------*/

    /**
        Returns true if and only if the two objects should be considered equal
        in the given locale.  The values must be non-null <code>String</code>
        objects.  The two strings must match exactly to be considered equal.

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

            // string must match exactly
        return o1.equals(o2);
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

        return compareStrings((String)o1, (String)o2, locale);
    }
}
