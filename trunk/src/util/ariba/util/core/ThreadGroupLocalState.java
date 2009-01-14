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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadGroupLocalState.java#5 $
*/

package ariba.util.core;

import java.util.Map;

/**
    @aribaapi ariba
*/
public class ThreadGroupLocalState implements State
{
    final Map threadToValue = MapUtil.map();

    public void set (Object value)
    {
        synchronized (threadToValue) {
            threadToValue.put(Thread.currentThread().getThreadGroup(),
                              value);
        }
    }
    public Object get ()
    {
        synchronized (threadToValue) {
            return threadToValue.get(Thread.currentThread().getThreadGroup());
        }
    }
}
