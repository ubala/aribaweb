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

    $Id: $
*/

package ariba.util.core;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
The WrapperRuntimeException class provides a simple runtime exception
for all the various problems which might occur within the FieldValue
dispatch mechanism.  In many cases, this simply wraps a caught-exception,
however, in a few cases, this is the original exception.  In either case,
this class overrides the pertinent api's from Exception so that appropriate
information can be extracted.

@aribaapi ariba

*/
public class WrapperRuntimeException extends RuntimeException
{
    public WrapperRuntimeException (Throwable e)
    {
        super(e);
    }
    
    public WrapperRuntimeException (String message)
    {
        super(message);
    }

    public WrapperRuntimeException (String message, Throwable e)
    {
        super(message, e);
    }

    /**
     * Provide a function to determine if this is wrapping a particular kind of exception.
     * 
     * @param exceptionClass The exception class we are checking for instance of in exception stack.
     * @return true If we wrap the passed in exception type.
     */
    public boolean wrapsException (Class exceptionClass)
    {
        Throwable cause = getCause();
        while (cause != null) {
            if (exceptionClass.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
    
    /**
     * Provide a function to get a particular type of exception in the cause stack.
     * 
     * @param exceptionClass The exception class we are checking for instance of in exception stack.
     * @return true If we wrap the passed in exception type.
     */
    public Exception getExceptionType (Class exceptionClass)
    {
        Throwable cause = getCause();
        while (cause != null) {
            if (exceptionClass.isInstance(cause)) {
                return (Exception)cause;
            }
            cause = cause.getCause();
        }
        return null;
    }

    public Throwable originalCause ()
    {
        Throwable last = this;
        while (last.getCause() != null) {
            last = last.getCause();
        }
        return last;
    }

    /**
    Provides access to the original exception, if one exists.

    @return the original exception which this WrapperRuntimeException is wrapping,
    or null if this exception was created with the "message" constructor.
    */
    public Exception originalException ()
    {
        Throwable cause = originalCause();
        return (cause instanceof Exception) ? (Exception)cause : null;
    }
}
