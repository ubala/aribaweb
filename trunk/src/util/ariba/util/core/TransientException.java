/*
    Copyright (c) 2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/TransientException.java#1 $

    Responsible: rwells
*/

package ariba.util.core;

/**
    TransientException is designed to be thrown by application code that needs to halt
    execution of an action and display a localized error message to the user about the
    error while running an action in the UI.  Its message should be an already localized
    string in the current session's locale, so it can be displayed in the UI as if it
    were an error message provided by the validation conditions or the application UI
    code.  It is generally used for conditions that are too expensive or obscure to
    check in the UI, and get detected during the action execution, including in non-UI
    application business logic code in classes in collaborate.main, etc.
*/
public class TransientException extends RuntimeException
{
    protected boolean _forceHome;

    /**
        Construct a new TransientException with the given message string, which should
        have already been localized into the current session's locale; it will be
        displayed to the user in the UI on the same page where the exception is thrown, if
        the exception is thrown during action execution.  It should not be thrown during
        appendToResponse or takeValues unless it will be caught in the application,
        because the UI framework is unlikely to be able to do anything other than display
        a stack trace if it is thrown outside action execution and not caught by the
        application code.
    */
    public TransientException (String message)
    {
        this(message, false);
    }

    /**
        Construct a new TransientException with the given message string, which should
        have already been localized into the current session's locale; it will be
        displayed to the user in the UI.  If forceHome is true, the UI may attention
        to it and try to take the user to the application "home" page rather than staying
        on the same page, but the catching context may also ignore it.
    */
    public TransientException (String message, boolean forceHome)
    {
        super(message);
        _forceHome = forceHome;
    }

    /**
        Returns true if the forceHome flag was set to true when this TransientException
        was constructed.
    */
    public boolean forceHome ()
    {
        return _forceHome;
    }

    /**
        Returns TransientException that is the given Throwable or is its direct or
        indirect cause, and returns null if there is no TransientException in the causal
        chain.
    */
    public static TransientException asCauseOf (Throwable t)
    {
        for ( ; t != null; t = t.getCause()) {
            if (t instanceof TransientException) {
                return (TransientException)t;
            }
        }
        return null;
    }

}
