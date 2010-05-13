/*
    Copyright (c) 1996-2007 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/EvaluationException.java#1 $

    Responsible: dfinlay
*/
package ariba.util.core;

/**
    @aribaapi ariba
*/
public class EvaluationException extends RuntimeException
{
    /**
        @aribaapi ariba
    */
    public EvaluationException ()
    {}

    /**
        @aribaapi ariba
    */
    public EvaluationException (String message)
    {
        super(message);
    }

    /**
        @aribaapi ariba
    */
    public EvaluationException (String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
        @aribaapi ariba
    */
    public EvaluationException (Throwable cause)
    {
        super(cause);
    }
}