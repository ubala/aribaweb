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

    $Id: //ariba/platform/util/core/ariba/util/core/RejectedExecutionException.java#3 $
*/

package ariba.util.core;

/**
    Exception thrown by an {@link Executor} when a task cannot be
    accepted for execution.

    Copy of java.util.concurrent.RejectedExecutionException which is
    part of Java 5.0
    
    @aribaapi ariba

*/
public class RejectedExecutionException extends RuntimeException
{
    public RejectedExecutionException ()
    {
    }

    public RejectedExecutionException (String message)
    {
        super(message);
    }

    public RejectedExecutionException (String message, Throwable cause)
    {
        super(message, cause);
    }

    public RejectedExecutionException (Throwable cause)
    {
        super(cause);
    }
}
