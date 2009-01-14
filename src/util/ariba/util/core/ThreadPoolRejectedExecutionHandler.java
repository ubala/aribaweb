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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadPoolRejectedExecutionHandler.java#3 $
*/

package ariba.util.core;
/**
    A concrete implementation of RejectedExecutionHandler for {@link ThreadPool}s.
    This handler will allocate extra threads as needed and attempts to execute
    the specified runnable task. This handler is appropriate to be used in
    situations where response time is important and the system is capable of
    providing extra resources (threads) on a temporary basis to handle sudden
    burst of activity.

    @aribaapi ariba
 */
public class ThreadPoolRejectedExecutionHandler
    implements RejectedExecutionHandler
{
    public void handle (Runnable runnable, Executor executor)
      throws RejectedExecutionException
    {
        if (!(executor instanceof ThreadPool)) {
            throw new RejectedExecutionException(
                Fmt.S("expected executor to be a ThreadPool, got %s",
                    executor.getClass().getName()));
        }
        ThreadPool threadPool = (ThreadPool)executor;
        threadPool.forceExecuteWithoutNotify(runnable);
    }
}
