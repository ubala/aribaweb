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

    $Id: //ariba/platform/util/core/ariba/util/core/RejectedExecutionHandler.java#2 $
*/
package ariba.util.core;


/**
    An interface for handling rejected executions.
    @see ariba.util.core.Executor
    @aribaapi ariba
*/
public interface RejectedExecutionHandler
{
    /**
        Called when a RejectedExecutionException is thrown when the
        {@link Executor#execute} method is invoked to handle the rejection.
        @param runnable the task to run, guaranteed to be non-null.
        @param executor the executor, guaranteed to be non-null.
        @exception RejectedExecutionException thrown if the way to handle the
        exception is to throw it.
        <p>
        <b>IMPORTANT</b> Since the executor is passed in, there is nothing
        to prevent this method to invoke the executor's execute method. But if
        this handler does call the executor's execute method, it must ensure
        that this execute call will not trigger this handler which can in turn
        trigger a call to execute, ultimately resulting in a stack overflow.
        @aribaapi ariba
    */
    public void handle (Runnable runnable, Executor executor)
      throws RejectedExecutionException;
}
