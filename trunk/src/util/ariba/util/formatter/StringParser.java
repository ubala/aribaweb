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

    $Id: //ariba/platform/util/core/ariba/util/formatter/StringParser.java#5 $
*/

package ariba.util.formatter;

import java.text.ParseException;
import java.util.Locale;

/**
    A StringParser is a class which can take a string and parse it into an
    object.  The Formatter class and its subclasses implement this interface;
    some customer-defined classes under config may also implement this
    interface.  It is used by the field defaulting framework to allow custom
    handling of string parsing.

    The parse() method takes a string and attempts to parse it into an object
    of the appropriate type.  If the string can't be parsed, a ParseException
    is thrown.

    The value() method takes an arbitrary object and attempts to create an
    object of the appropriate type from it.  In some cases the object returned
    may be the value itself (if it's already of the right type).  Often, this
    method just converts the value to a string and calls parse(), returning a
    reasonable default value if a parse exception is caught.

    @aribaapi documented
*/
public interface StringParser
{
    /**
        Parses <B>string</B> into an object of the appropriate type.

        @param string the string to be parsed
        @return The object parsed from the input string
        @exception ParseExceptionunexpected error occurred while parsing
                    <b>string</b>
        @aribaapi documented
    */
    public Object parse (String string)
      throws ParseException;

    /**
        Parses <B>string</B> into an object of the appropriate type
        using the given <b>locale</b>.

        @param string the string to be parsed
        @param locale the locale used when parsing the string
        @return The object parsed from the input string
        @exception ParseExceptionunexpected error occurred while parsing
                    <b>string</b>
        @aribaapi documented
    */
    public Object parse (String string, Locale locale)
      throws ParseException;

    /**
        Returns an object of the appropriate type for <B>object</B>.

        @param object The object being parsed
        @return Returns a object of the appropriate type
        @aribaapi documented
    */
    public Object getValue (Object object);
}
