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

    $Id: //ariba/platform/util/core/ariba/util/core/FatalAssertionException.java#8 $
*/

package ariba.util.core;

/**
    Class used to represent a Fatal Assert Exception.  Thrown by the methods in
    <code>ariba.util.core.Assert</code> if an invariant is found to be false.

    @aribaapi documented
*/
public class FatalAssertionException extends RuntimeException
{
    /**
        Create a new FatalAssertionException.
        @aribaapi documented
    */
    public FatalAssertionException ()
    {
    }

    /**
        Create a new FatalAssertionException.
        @param t the original cause of the exception
        @aribaapi documented
    */
    public FatalAssertionException (Throwable t)
    {
        super(t);
    }

    /**
        Create a new FatalAssertionException with a message.

        @param message a text message for the exception to hold
        @aribaapi documented
    */
    public FatalAssertionException (String message)
    {
        super(message);
    }

    public FatalAssertionException (Throwable cause, String message, Object... args)
    {
        super(Fmt.S(message, args), cause);
    }
}
