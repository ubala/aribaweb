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

    $Id: //ariba/platform/util/core/ariba/util/core/WrappedException.java#4 $
*/

package ariba.util.core;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

/**
    a generic exception subclass for all exceptions thrown. It wraps the
    the exception thrown by its subclasses and incorporates the exception
    in its getMessage method.

    @aribaapi documented
*/
public class WrappedException extends InvocationTargetException
{
    /* Public Constants for severity */
    /**
        Severity level for Warning.
        @aribaapi documented
    */
    public static final int SevWarning = 1;
    /**
        Severity level for Error. This is the default severity.
        @aribaapi documented
    */
    public static final int SevError = 2;
    /**
        Severity level for Fatal.
        @aribaapi documented
    */
    public static final int SevFatal = 3;
    
    /* private fields */
    protected String    _message;
    protected int       _severity;
    protected Exception _wrappedException;

    /**
        Constructor with message.
        @param aMessage the error message
        @aribaapi documented
    */
    public WrappedException (String aMessage)
    {
        this(aMessage, SevError, null);
    }
        
    /**
        Constructor with message and severity
        @param aMessage the error message
        @param severity 
        @aribaapi documented
    */
    public WrappedException (String aMessage, int severity)
    {
        this(aMessage, severity, null);
    }
    
    /**
        Specifies a message and an exception to wrap
        @param aMessage the error message
        @param anException the embedded exception
        @aribaapi documented
    */
    public WrappedException (String aMessage, Exception anException)
    {
        this(aMessage, SevError, anException);
    }

    /**
        Construct with an exception - will copy message from the exception
        and create it with severity Error.
        @param anException the embedded exception
        @aribaapi documented
    */
    public WrappedException (Exception anException)
    {
        this(anException.getMessage(),
             SevError,
             anException);
    }
    
    /**
        Specifies a message, a severity and an exception to wrap.
        @param aMessage the error message
        @param severity
        @param anException the embedded exception
        
        @aribaapi documented
    */
    public WrappedException (String    aMessage,
                             int       severity,
                             Exception anException)
    {
        super(anException, aMessage);
        _message          = aMessage;
        _severity         = severity;
        _wrappedException = anException;
    }

    /**
        return the passed in message
        @return the message string
        @aribaapi documented
    */
    public String getMessage ()
    {
        return _message;
    }
    
    /**
        Returns the wrapped exception
        @return the embedded exception
        @aribaapi documented
    */
    public Exception getWrappedException ()
    {
        return _wrappedException;
    }

    /**
        Get the severity of the exception.
        @return the severity of the exception.

        @aribaapi documented
    */
    public int getSeverity ()
    {
        return _severity;
    }
    
    /**
        Print the stack trace to the provided PrintWriter
        @param s the output print writer
        @aribaapi documented
    */
    public void printStackTrace (PrintWriter s)
    {
        if (_message != null) {
            Fmt.F(s, "Exception: %s\n", _message );
        }
        
        if (_wrappedException != null) {
            _wrappedException.printStackTrace(s);
            return;
        }
        super.printStackTrace(s);
    }
        
    /**
        Print the stack trace to the provided PrintStream
        @param s the ouput print stream
        @aribaapi documented
    */
    public void printStackTrace (PrintStream s)
    {
        if (_message != null) {
            s.println(StringUtil.strcat("Exception: ", _message));
        }
        
        if (_wrappedException != null) {
            _wrappedException.printStackTrace(s);
            return;
        }
        super.printStackTrace(s);
    }
}
