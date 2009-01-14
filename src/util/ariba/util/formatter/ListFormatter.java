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

    $Id: //ariba/platform/util/core/ariba/util/formatter/ListFormatter.java#3 $
*/

package ariba.util.formatter;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import java.util.List;
import java.text.ParseException;
import java.util.Locale;

/**
    <code>ListFormatter</code> is a subclass of <code>Formatter</code> which
    is responsible for formatting, parsing, and comparing vectors.

    @aribaapi documented
*/
public class ListFormatter extends Formatter
{
    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>ListFormatter</code>.

        @aribaapi private
    */
    public ListFormatter ()
    {
    }
    

    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the <code>vector</code> as a list.
        
        @param v <code>List</code> to format.
        @param locale The <code>Locale</code> to use for formatting.
        @param useHTML True if HTML tags should be inserted into the
                       string to create a list.
        @return Returns a string representation of the vector
        @aribaapi documented
    */
    public static String formatList (List v, Locale locale, boolean useHTML)
    {
        if (locale == null) {
            locale = getDefaultLocale();
        }
        
        FastStringBuffer list = new FastStringBuffer();

        for (int i = 0; i < v.size(); i++) {
            Object obj = v.get(i);
            String line = null;
            Formatter formatter =
                Formatter.getFormatterForType(obj.getClass().getName());
            if (formatter != null) {
                line = formatter.getFormat(formatter.getValue(obj, locale), locale);
            }
            else {
                line = obj.toString();
            }
            if (useHTML) {
                list.append("<li>");
                line = HTML.fullyEscape(line);
            }
            list.append(line);
            if (useHTML) {
                list.append("</li>");
            }
            else {
                list.append(MIME.CRLF);
            }
        }

        return list.toString();
    }

    /**
        Returns a string representation of the given object in the given
        locale.  Since, for this subclass of <code>Formatter</code>, the
        object must be a non-null <code>String</code>, the value returned is
        just the string itself.

        @param  object the object to format into a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        the <code>String</code> itself
        @aribaapi documented
    */
    protected String formatObject (Object object, Locale locale)
    {
        Assert.that(object instanceof List, "invalid type");
        return formatList((List)object, locale, false);
    }


    /*-----------------------------------------------------------------------
        Parsing
      -----------------------------------------------------------------------*/

    /**
        This is not implemented for vector types.
        
        @param  string the string to parse
        @param  locale the <code>Locale</code> to use for parsing
        @return        an object of the appropriate type for this formatter
        @exception     ParseException if the string can't be parsed to create
                       an object the appropriate type
        @aribaapi documented
    */
    protected Object parseString (String string, Locale locale)
      throws ParseException
    {
        throw new ParseException("Not supported", 0);
    }

    /**
        Returns a <code>List</code> from the given <code>object</code>.
        We currently do not support converting other object types to 
        <code>List</code>, so the method will either return the 
        <code>List</code> that is passed in or, if the <code>object</code>
        isn't a <code>List</code>, it will return null.
        
        @param  object the object to convert to a <code>List</code>
        @param  locale the <code>Locale</code> to use for conversion (unused)
        @return        a <code>List</code> derived from the given object
        @aribaapi documented
    */
    public Object getValue (Object object, Locale locale)
    {
        if (object == null) {
            return null;
        }
        else if (object instanceof List) {
            return ((List)object);
        }
        else {
            return null;
        }
    }


    /*-----------------------------------------------------------------------
        Comparison
      -----------------------------------------------------------------------*/

    /**
        Always returns zero.  Comparison is currently not supported
        for vectors.
        @param o1 List being compared
        @param o2 List being compared
        @param locale The locale used for the comparision
        @return Always returns zero. Comparision is currently not supported.
        @aribaapi documented
    */
    protected int compareObjects (Object o1, Object o2, Locale locale)
    {
        Assert.that(o1 instanceof List, "invalid type");
        Assert.that(o2 instanceof List, "invalid type");

        return 0;
    }
}
