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

    $Id: //ariba/platform/util/core/ariba/util/core/ReferenceReaderException.java#7 $
*/

package ariba.util.core;

/**
    Exception class to be used for ReferenceReader
    @aribaapi ariba
*/
public class ReferenceReaderException extends RuntimeException
{
    /**
        Constructor for ReferenceReaderException.

        @param s the message associated with the exception.

        @aribaapi ariba
    */
    public ReferenceReaderException (String s)
    {
        super(s);
    }

    /**
        Constructor for AppInfoException with a given throwable. This
        is basically a wrapper around the orignal throwable.

        @param t the throwable
        @aribaapi ariba
    */
    public ReferenceReaderException (Throwable t)
    {
        super(t);
    }

    /**
        Constructor for AppInfoException with a given throwable and message. This
        is basically a wrapper around the orignal throwable.

        @param msg the message
        @param t the throwable
        @aribaapi ariba
    */
    public ReferenceReaderException (String msg,
                                     Throwable t)
    {
        super(msg, t);
    }

}
