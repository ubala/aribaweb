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

    $Id: //ariba/platform/util/core/ariba/util/core/TunnelingException.java#3 $
*/

package ariba.util.core;


/**
    TunnelingException is used to wrap a RunTimeException around a Exception.<p>
    
    @aribaapi private
*/
public class TunnelingException extends RuntimeException
{
    // -------------------------------------------------------------------------
    // Constructor

    /**
        Constructs a new instance with <code>exception</code> as the
        underlying exception.
        @aribaapi ariba
    */
    public TunnelingException (Exception exception)
    {
        super(exception);
    }

    // -------------------------------------------------------------------------
    // Public Methods

    /**
        Returns the underlying <code>Exception</code> that caused
        <code>this</code>. <p>
        @aribaapi ariba
    */
    public Exception getNestedException ()
    {
        return (Exception)getCause();
    }
}