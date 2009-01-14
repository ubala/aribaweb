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

    $Id: //ariba/platform/util/core/ariba/util/core/StateFactory.java#4 $
*/

package ariba.util.core;

/**
    @aribaapi ariba
*/
public final class StateFactory
{
    private static final StateFactoryInterface stateFactoryImpl =
        getStateFactoryImpl();
    
    static StateFactoryInterface getStateFactoryImpl ()
    {
        Class threadLocalClass = ClassUtil.classForName("java.lang.ThreadLocal",
                                                   false);
        if (threadLocalClass != null) {
                // don't use reference ThreadLocalState directly or
                // through a static string in any way since some
                // compilers will introduce a dependancy.
            return (StateFactoryInterface)ClassUtil.newInstance(
                "ariba.util.core.ThreadLocalState");
        }
        return new PoorThreadLocalState();
    }
    
    public static State createState ()
    {
        return stateFactoryImpl.createState();
    }
}
