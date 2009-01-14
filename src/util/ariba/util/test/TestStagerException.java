/*
    Copyright (c) 2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/test/TestStagerException.java#1 $

    Responsible: awysocki
*/

package ariba.util.test;

/**
 * This exception should be thrown when the stager execution fails.
 */
public class TestStagerException extends TestException
{
    public TestStagerException(String message) {
        super(message);
    }

    public TestStagerException(Throwable cause) {
        super(cause!=null?cause.getMessage():"", cause);
    }

    public TestStagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
