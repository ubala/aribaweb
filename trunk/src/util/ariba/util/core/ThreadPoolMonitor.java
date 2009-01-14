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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadPoolMonitor.java#2 $
*/
package ariba.util.core;


/**
    This interface specifies a hook to monitor the thread pool. Currently,
    we only use this for unit test.
    @aribaapi ariba
*/
public interface ThreadPoolMonitor
{
    /**
        monitor the specified thread pool
        @param pool the specified thread pool
    */
    public void monitor (ThreadPool pool);
}
