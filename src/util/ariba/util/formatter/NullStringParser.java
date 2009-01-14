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

    $Id: //ariba/platform/util/core/ariba/util/formatter/NullStringParser.java#6 $
*/

package ariba.util.formatter;

import java.util.Locale;


/**
    NullStringParser is a trivial implementation of StringParser that always
    parses any string as null.  This is useful for CONSTANT style defaulters
    where you want the value to be defaulted as null (specifying this class
    as the implementer gets the effect).

    @aribaapi documented
*/
public class NullStringParser implements StringParser
{
    /**
        Creates a new NullStringParser.

        @aribaapi private
    */
    public NullStringParser ()
    {
    }

    /**
        Trivial implementation of StringParser.parse which always returns a null.
        Assumes the default locale.
        @param string The string to be parsed.
        @return null
        @aribaapi documented
    */
    public Object parse (String string)
    {
        return null;
    }


    /**
        Trivial implementation of StringParser.parse which always returns a null.
        @param string The string to be parsed
        @param locale The locale used for parsing
        @return null
        @aribaapi documented
    */
    public Object parse (String string, Locale locale)
    {
        return null;
    }


    /**
        Trivial implementation of StringParser.getValue which always returns a null.

        @param object object to be parsed
        @return null
        @aribaapi documented
    */
    public Object getValue (Object object)
    {
        return null;
    }
}
