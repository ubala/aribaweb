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

    $Id: //ariba/platform/util/core/ariba/util/core/LobException.java#5 $
*/
package ariba.util.core;

import java.io.PrintWriter;
import java.io.PrintStream;

/**
    Represents an exception that has occurred during an interaction with a
    large-object (lob.) <p>

    This class contains the ability to nest (or chain) exceptions, a feature which
    has been added to JDK 1.4.  At the time that we upgrade to that platform we
    should remove this functionality from this class. <p>

    @aribaapi ariba
*/
public class LobException extends RuntimeException
{
    //-------------------------------------------------------------------------
    // private data members

    private Exception _exception;

    //-------------------------------------------------------------------------
    // constructors

    /**
        Constructs a new instance with the specified <code>message</code>. <p>
    */
    public LobException (String message)
    {
        super(message);
        _exception = null;
    }

    /**
        Constructs a new instance with the specified nested exception. <p>
    */
    public LobException (Exception exception)
    {
        super(exception != null ? exception.toString() : null);
        _exception = exception;
    }

    /**
        Constructs a new instance with the specified nested exception and detail
        message. <p>
    */
    public LobException (String message, Exception exception)
    {
        super(message);
        _exception = exception;
    }

    //-------------------------------------------------------------------------
    // public methods

    /**
        Returns the nested <code>Exception</code> of <code>this</code>, if any
        or <code>null</code> if none. <p>
    */
    public Exception getException ()
    {
        return _exception;
    }

    /**
        Prints the stack trace of <code>this</code> followed by the stack trace
        of the nested exception, if any to the specified <code>PrintWriter</code>.<p>
    */
    public void printStackTrace (PrintWriter pw)
    {
        synchronized (pw) {
            super.printStackTrace(pw);
            if (_exception != null) {
                pw.print("Caused by: ");
                _exception.printStackTrace(pw);
            }
        }
    }

    /**
        Prints the stack trace of <code>this</code> followed by the stack trace
        of the nested exception, if any to the specified <code>PrintStream</code>.<p>
    */
    public void printStackTrace (PrintStream ps)
    {
        synchronized (ps) {
            super.printStackTrace(ps);
            if (_exception != null) {
                ps.print("Caused by: ");
                _exception.printStackTrace(ps);
            }
        }
    }

    /**
        Prints the stack trace of <code>this</code> followed by the stack trace
        of the nested exception, if any to <code>System.err</code>. <p>
    */
    public void printStackTrace ()
    {
        printStackTrace(System.err);
    }
}
