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

    $Id: //ariba/platform/util/core/ariba/util/core/Executor.java#3 $
*/

package ariba.util.core;

/**
    Copy of the java.util.concurrent.Executor which is part
    of Java 5.0
    @aribaapi ariba
*/
public interface Executor
{
    /**
        Executes the given command at some time in the future.  The command
        may execute in a new thread, in a pooled thread, or in the calling
        thread, at the discretion of the <tt>Executor</tt> implementation.

        @param command the runnable task
        @throws RejectedExecutionException if this task cannot be
                accepted for execution.
        @throws NullPointerException if command is null
    */
    public void execute (Runnable command);
}
