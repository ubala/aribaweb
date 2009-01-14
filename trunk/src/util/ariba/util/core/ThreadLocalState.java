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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadLocalState.java#4 $
*/

package ariba.util.core;

/**
    Do not make this class anything other than package private. Would
    be callers should use StateFactory.
*/

class ThreadLocalState implements State, StateFactoryInterface
{
    final ThreadLocal threadLocal = new ThreadLocal();

    public void set (Object value)
    {
        threadLocal.set(value);
    }
    public Object get ()
    {
        return threadLocal.get();
    }

    public State createState ()
    {
        return new ThreadLocalState();
    }
}
