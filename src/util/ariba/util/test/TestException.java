/*
    Copyright (c) 2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/test/TestException.java#1 $

    Responsible: awysocki
*/

package ariba.util.test;

/**
 * Base exception for any Test related exceptions. 
 * All the stager and validator related exceptions should extend this class.
 */
public class TestException extends Exception
{
    public TestException(String message) {
        super(message);
    }

    public TestException(String message, Throwable cause) {
        super(message, cause);
    }
}
