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

    $Id: //ariba/platform/util/core/ariba/util/core/Factory.java#2 $
*/
package ariba.util.core;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
    Convenience generic factory class. <p/>

    @aribaapi ariba
*/
public abstract class Factory<T>
{
    /**
        Creates and returns an instance of the generic type, <code>T</code>. <p/>

        @return  the newly created instance of <code>T</code>
        @aribaapi ariba
    */
    public abstract T create (Object ... arguments);

    /**
        @aribaapi ariba
    */
    public static final Factory<List<Object>> ListFactory = new Factory<List<Object>>()
    {
        public List<Object> create (Object ... arguments)
        {
            return ListUtil.list();
        }
    };

    /**
        @aribaapi ariba
    */
    public static <T> Factory<List<T>> listFactory ()
    {
        return (Factory)ListFactory;
    }

    /**
        @aribaapi ariba
    */
    public static final Factory<Set<Object>> SetFactory = new Factory<Set<Object>>()
    {
        public Set<Object> create (Object ... arguments)
        {
            return SetUtil.set();
        }
    };

    /**
        @aribaapi ariba
    */
    public static <T> Factory<Set<T>> setFactory ()
    {
        return (Factory)SetFactory;
    }

    /**
        @aribaapi ariba
    */
    public static final Factory<SortedSet<Object>> SortedSetFactory
            = new Factory<SortedSet<Object>>()
                {
                    public SortedSet<Object> create (Object ... arguments)
                    {
                        return SetUtil.sortedSet();
                    }
                };

    /**
        @aribaapi ariba
    */
    public static <T> Factory<SortedSet<T>> sortedSetFactory ()
    {
        return (Factory)SortedSetFactory;
    }
}
