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

    $Id: //ariba/platform/util/core/ariba/util/io/DeserializationException.java#6 $
*/

package ariba.util.io;

/**
    Exception signaling an exceptional condition during
    deserialization. This exception also provides a means for a
    Deserializer client to determine which line generated the
    exception.

    @aribaapi public
*/
public class DeserializationException extends Exception
{
    /**
        @aribaapi private
    */
    private int lineNumber = -1;

    /**
        Constructs a DeserializationException with the descriptive
        message <b>string</b> and <b>lineNumber</b>, the line number
        on which the error occurred.

        @param string error message
        @param lineNumber line number at which error occurred
        @aribaapi public
    */
    public DeserializationException (String string, int lineNumber)
    {
        super(string);
        this.lineNumber = lineNumber;
    }

    /**
        Returns the line number at which the DeserializationException
        occurred.

        @return the line number

        @aribaapi public
    */
    public int lineNumber ()
    {
        return lineNumber;
    }
}
