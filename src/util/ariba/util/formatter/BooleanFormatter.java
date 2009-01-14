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

    $Id: //ariba/platform/util/core/ariba/util/formatter/BooleanFormatter.java#7 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import java.text.ParseException;
import java.util.Locale;

/**
    <code>BooleanFormatter</code> is a subclass of <code>Formatter</code>
    which is responsible for formatting, parsing, and comparing
    <code>boolean</code> values and/or <code>Boolean</code> objects.

    @aribaapi documented
*/
public class BooleanFormatter extends Formatter
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Our Java class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.util.formatter.BooleanFormatter";

        // strings for true and false
    private static final String TrueString  = "true";
    private static final String FalseString = "false";
    
    private static final String StringTable = "ariba.util.core";
    private static final String BooleanTrue = "BooleanTrue";
    private static final String BooleanFalse = "BooleanFalse";
    private static final BooleanFormatter SharedBooleanFormatter = new BooleanFormatter();

    public static final String BooleanTrueOrFalseKey = "BooleanTrueOrFalse";

    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>BooleanFormatter</code>.

        @aribaapi private
    */
    public BooleanFormatter ()
    {
    }
    

    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given <code>Boolean</code> object
        in the default locale.

        @param  object the <code>Boolean</code> object to format into a string
        @return        a formatted string for the given <code>Boolean</code>
                       object in the default locale
        @aribaapi documented
    */
    public static String getStringValue (Boolean object)
    {
        return getStringValue(object, getDefaultLocale());
    }

    /**
        Returns a formatted string for the given <code>Boolean</code> object
        in the given locale.

        @param  object the <code>Boolean</code> object to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a formatted string for the given <code>Boolean</code>
                       object in the given locale
        @aribaapi documented
    */
    public static String getStringValue (Boolean object, Locale locale)
    {
        if (object == null) {
            return "";
        }
        else {
            return ResourceService.getString(
                         StringTable, 
                         object.booleanValue() ? BooleanTrue : BooleanFalse,
                         locale);
        }
    }

    /*-----------------------------------------------------------------------
        Static Parsing
      -----------------------------------------------------------------------*/

    /**
        Parses the given string as a <code>boolean</code> value.  Returns true
        if and only if the given string is <code>"true"</code>.

        @param  string the <code>String</code> to parse
        @return        <code>true</code> if the given string is
                       <code>"true"</code>; <code>false</code> otherwise.
        @aribaapi documented
    */
    public static boolean parseBoolean (String string)
    {
        return parseStringAsBoolean(string).booleanValue();
    }

    /**
        Parses the given string as a <code>Boolean</code> value.  Returns
        <code>Boolean.TRUE</code> if and only if the given string is
        <code>"true"</code>.

        @param  string the <code>String</code> to parse
        @return        <code>Boolean.TRUE</code> if the given string is
                       <code>"true"</code>; <code>Boolean.FALSE</code>
                       otherwise.
        @aribaapi documented
    */
    public static Boolean parseStringAsBoolean (String string)
    {
        try {
            return (Boolean)SharedBooleanFormatter.parseString(
                string, getDefaultLocale());
        }
        catch (ParseException parseException) {
            return Boolean.FALSE;
        }
    }

    /**
        Returns a <code>boolean</code> value derived from the given object.
        If the object is not a <code>Boolean</code>, it is converted to a
        string and compared against the string <code>"true"</code>.

        @param  object the object to covert to a <code>boolean</code>
        @return        a <code>boolean</code> derived from the given object
        @aribaapi documented
    */
    public static boolean getBooleanValue (Object object)
    {
        if (object == null) {
            return false;
        }
        else if (object instanceof Boolean) {
            return ((Boolean)object).booleanValue();
        }
        else {
            Boolean booleanObject = parseStringAsBoolean(object.toString());
            return booleanObject.booleanValue();
        }
    }


    /*-----------------------------------------------------------------------
        Static Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two <code>Boolean</code> objects for sorting purposes.
        Returns an <code>int</code> value which is less than, equal to, or
        greater than zero depending on whether the first object sorts before,
        the same, or after the second object.  Sorts <code>Boolean.TRUE</code>
        before <code>Boolean.FALSE</code>.

        @param  b1 the first <code>Boolean</code> to compare
        @param  b2 the second <code>Boolean</code> to compare
        @return    <code>int</code> value which determines how the two objects
                   should be ordered
        @aribaapi documented
    */
    public static int compareBooleans (Boolean b1, Boolean b2)
    {
        if (b1 == b2) {
            return 0;
        }
        else if (b1 == null) {
            return -1;
        }
        else if (b2 == null) {
            return 1;
        }
        else {
            return compareBooleans(b1.booleanValue(), b2.booleanValue());
        }
    }

    /**
        Compares two <code>boolean</code> values for sorting purposes.
        Returns an <code>int</code> value which is less than, equal to, or
        greater than zero depending on whether the first object sorts before,
        the same, or after the second object.  Sorts <code>true</code> before
        <code>false</code>.

        @param  b1 the first <code>boolean</code> to compare
        @param  b2 the second <code>boolean</code> to compare
        @return    <code>int</code> value which determines how the two objects
                   should be ordered
        @aribaapi documented
    */
    public static int compareBooleans (boolean b1, boolean b2)
    {
            // arbitrarily sort true before false
        return (b1 == b2) ? 0 : ((b1 && !b2) ? 1 : -1);
    }


    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the given
        locale.  The object must be a non-null <code>Boolean</code>.

        @param  object the <code>Boolean</code> to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the <code>Boolean</code>
        @aribaapi documented
    */
    protected String formatObject (Object object, Locale locale)
    {
        Assert.that(object instanceof Boolean, "invalid type");
        return getStringValue((Boolean)object, locale);
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        Parses the given string into a <code>Boolean</code> object.  The
        string is assumed to be non-null and trimmed of leading and trailing
        whitespace.  Returns <code>Boolean.TRUE</code> if and only if the
        given string is <code>"true"</code>.

        @param  string the string to parse
        @param  locale the <code>Locale</code> to use for parsing
        @return        a <code>Boolean</code> object derived from the string
        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
      throws ParseException
    {
        if (!StringUtil.nullOrEmptyString(string)) {
            if (TrueString.equalsIgnoreCase(string) ||
                string.equals(ResourceService.getString(
                    StringTable, BooleanTrue, locale))) {
                return Boolean.TRUE;
            }
            else if (FalseString.equalsIgnoreCase(string) ||
                     string.equals(ResourceService.getString(
                         StringTable, BooleanFalse, locale))) {
                return Boolean.FALSE;
            }
        }

        throw makeParseException(BooleanTrueOrFalseKey, 0);
    }

    /**
        Returns a new <code>Boolean</code> derived from the given object.  If
        the object is not a <code>Boolean</code>, it is converted to a string
        and compared against the string <code>"true"</code>.
        
        @param  object the object to convert to a <code>Boolean</code>
        @param  locale the <code>Locale</code> to use for conversion
        @return        a <code>Boolean</code> derived from the given object
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        return Constants.getBoolean(getBooleanValue(object));
    }


    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two objects for sorting purposes in the given locale.  The
        two objects must be non-null <code>Boolean</code> objects.  Returns a
        value which is less than, equal to, or greater than zero depending on
        whether the first object sorts before, the same, or after the second
        object.  Sorts <code>Boolean.TRUE</code> before
        <code>Boolean.FALSE</code>.
        
        @param  o1     the first <code>Boolean</code> to compare
        @param  o2     the second <code>Boolean</code> to compare
        @param  locale the <code>Locale</code> to use for comparison
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi documented
    */
    protected int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof Boolean, "invalid type");
        Assert.that(o2 instanceof Boolean, "invalid type");

        return compareBooleans((Boolean)o1, (Boolean)o2);
    }
}
