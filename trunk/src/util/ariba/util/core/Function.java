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

    $Id: //ariba/platform/util/core/ariba/util/core/Function.java#2 $
*/
package ariba.util.core;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

/**
    Generic class that represents a function returning a type <code>K</code>.
    <p/>

    @aribaapi ariba
*/
public abstract class Function<K>
{
    /**
        Evaluates the supplied <code>arguments</code> returning an instance of
        type <code>K</code>. <p/>

        @param arguments the arguments to be evaulated
        @return the instance of <code>K</code>
        @aribaapi ariba
    */
    public abstract K evaluate (Object ... arguments);

    /**
        Returns <code>true</code> if each of the elements in <code>elements</code>
        has the same value according to this function. <p/>

        @aribaapi ariba
    */
    public boolean hasSameValue (Collection elements)
    {
        if (elements.isEmpty()) {
            return true;
        }
        Iterator iter = elements.iterator();
        Object first = evaluate(iter.next());
        while (iter.hasNext()) {
            Object current = evaluate(iter.next());
            if (!SystemUtil.equal(current, first)) {
                return false;
            }
        }
        return true;
    }

    /**
        Efficiently, splits the supplied <code>collection</code> of a value type
        <code>V</code> returning a map which is a partition of the collection using
        this function. <p/>
     
        Specifically, this method splits <code>collection</code> into sub-lists
        each of which has the same value when evaluated by this function.

        @aribaapi ariba
    */
    public <V> Map<K,List<V>> split (Collection<V> collection)
    {
        if (collection == null || collection.isEmpty()) {
            return MapUtil.map();
        }
        Iterator<V> iter = collection.iterator();
        K first = this.evaluate(iter.next());
        Map<K,List<V>> result = null;
        int idx = 1;
        while (iter.hasNext()) {
            V value = iter.next();
            K key = this.evaluate(value);
            if (!SystemUtil.equal(first, key)) {
                /* keys not all the same */
                if (result == null) {
                    result = MapUtil.map();
                    List<V> firstValues = ListUtil.subList(collection, 0, idx, true);
                    result.put(first, firstValues);
                }
            }
            if (result != null) {
                MapUtil.merge(result, key, value);
            }
            ++idx;
        }
        if (result == null) {
            /* all the same */
            result = MapUtil.map();
            List<V> values = (collection instanceof List)
                             ? (List<V>)collection
                             : new ArrayList(collection);
            result.put(first, values);
        }
        return result;
    }
}
