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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValueException.java#2 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.WrapperRuntimeException;

/**
The FieldValueException class provides a simple runtime exception
for all the various problems which might occur within the FieldValue
 dispatch mechanism.  See WrapperRuntimeException for details.
*/
public class FieldValueException extends WrapperRuntimeException
{
    /**
    Constructs a FieldValueException with a descriptive message.

    @param message a message desribing the error condition.
    */
    public FieldValueException (String message)
    {
        super(message);
    }

    /**
    Constructs a new FieldValueException that merely wraps a caught-exception.

    @param exception the exception which is wrapped (and converted into a runtime exception)
    */
    public FieldValueException (Exception exception)
    {
        super(exception);
    }

    /**
    @deprecated Use throwException(...)
    Converts an exception into a RuntimeException.  If the exception is already a
    RuntimeException, it is simply returned.  Otherwise, it is wrapped in a
    FieldValueException.

    @param exception the exception to be wrapped.

    @return a new instance of FieldValueException or the original exceptio
    if its already a RuntimeException.
    */
    public static RuntimeException wrapException (Exception exception)
    {
        return (exception instanceof RuntimeException) ?
            (RuntimeException)exception :
            new FieldValueException(exception);
    }

    /**
    Converts a Throwable into a RuntimeException or Error and rethrows it.
    If the Throwable is already a RuntimeException or Error, it is simply rethrown.
    Otherwise, it is wrapped in a FieldValueException.

    @param throwable the throwable to be converted and rethrown.
    */
    public static void throwException (Throwable throwable)
    {
        if (throwable instanceof Error) {
            throw (Error)throwable;
        }
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException)throwable;
        }
        else {
            throw new FieldValueException((Exception)throwable);
        }
    }
}
