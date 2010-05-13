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

    $Id: //ariba/platform/util/core/ariba/util/core/Function.java#7 $
*/
package ariba.util.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

/**
    Generic class that represents a function returning a type <code>K</code>.
    <p/>

    Useful for generically splitting, mapping and collecting over collections.
    E.g. say you have a list of strings and you want to identify all of the strings
    in the collection that begin with the same three characters:<p>
    <pre>
    final Function&lt;String> substring = new Function&lt;String>() {
        public String evaluate (Object... arguments) {
            String string = (String)arguments[0];
            return string.length() > 2 ? string.substring(0,3) : null;
        }
    };
    List&lt;String> someBigListOfStrings = ...;
    Map&lt;String,List&lt;String>> result = substring.split(someBigListOfStrings);
    </pre>
    <code>result</code> will hold a map mapping the first 3 characters to all the
    strings in the orginal list that begin with the same three characters. <p/>

    Feel free to add more methods to this class. We could use a
    <code>int find(Collection&lt;V> list, K k)</code> method returning the index of the
    <code>V</code> in the list that for which the application of the <code>Function</code>
    yields <code>k</code>.  Similarly we could use a <code>collectWhere()</code>.

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
    public abstract K evaluate (Object ... arguments) throws EvaluationException;

    /**
        Returns <code>true</code> if each of the elements in <code>elements</code>
        has the same value according to this function. <p/>

        @aribaapi ariba
    */
    public boolean hasSameValue (Iterable elements)
    {
        Iterator iter = elements.iterator();
        if (!iter.hasNext()) {
            return true;
        }
        Object[] args = new Object[1];
        args[0] = iter.next();
        Object first = evaluate(args);
        while (iter.hasNext()) {
            args[0] = iter.next();
            Object current = evaluate(args);
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

        Using the aggregator function  when merging the value to the map.

        Specifically, this method splits <code>collection</code> into sub-lists
        each of which has the same value when evaluated by this function.

        @aribaapi ariba
    */
    public <V, W> void splitInto (Iterable<V> collection,
                                  Map<K,W> result,
                                  Aggregator<W,V> aggregator,
                                  boolean includeNulls)
    {
        if (collection == null) {
            return;
        }
        Iterator<V> iter = collection.iterator();
        if (!iter.hasNext()) {
            return;
        }
        boolean mutatesAggregate = aggregator.mutatesAggregate();
        Object[] args = new Object[1];
        while (iter.hasNext()) {
            V value = iter.next();
            args[0] = value;
            K key = this.evaluate(args);
            if (key != null || includeNulls) {
                W w = result.get(key);
                W aggregate = aggregator.aggregate(w, value);
                if (w == null || !mutatesAggregate) {
                    result.put(key, aggregate);
                }
            }
        }
    }

    /**
        Efficiently, splits the supplied <code>collection</code> of a value type
        <code>V</code> returning a map which is a partition of the collection using
        this function. <p/>

        Specifically, this method splits <code>collection</code> into sub-lists
        each of which has the same value when evaluated by this function.

        @aribaapi ariba
    */
    public <V> void splitInto (Iterable<V> collection, Map<K,List<V>> result, boolean includeNulls)
    {
        Aggregator<List<V>, V> aggregator = Aggregator.collector();
        splitInto(collection, result, aggregator, includeNulls);
    }

    /**
        Efficiently, splits the supplied <code>collection</code> of a value type
        <code>V</code> returning a map which is a partition of the collection using
        this function. <p/>

        Specifically, this method splits <code>collection</code> into sub-lists
        each of which has the same value when evaluated by this function.

        @aribaapi ariba
    */
    public <V> void splitInto (Iterable<V> collection, Map<K,List<V>> result)
    {
        splitInto(collection, result, false);
    }

    /**
        Efficiently, splits the supplied <code>collection</code> of a value type
        <code>V</code> returning a map which is a partition of the collection using
        this function. <p/>

        Specifically, this method splits <code>collection</code> into sub-lists
        each of which has the same value when evaluated by this function.

        @aribaapi ariba
    */
    public <V> Map<K,List<V>> split (Iterable<V> collection)
    {
        Iterator<V> iter = collection != null ? collection.iterator() : null;
        if (iter == null || !iter.hasNext()) {
            return MapUtil.map();
        }
        Object[] args = new Object[1];
        args[0] = iter.next();
        K first = this.evaluate(args);
        Map<K,List<V>> result = null;
        int idx = 1;
        while (iter.hasNext()) {
            V value = iter.next();
            args[0] = value;
            K key = this.evaluate(args);
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
            result   = MapUtil.map();
            List<V> values;
            if (collection instanceof List) {
                values = (List<V>)collection;
            }
            else {
                values = ListUtil.list();
                ListUtil.addAll(values, collection);
            }
            result.put(first, values);
        }
        return result;
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding the mapping to <code>collector</code>.

        @param values the collection of values
        @param collector the resulting map
        @param includeNulls whether or not <code>null</code> evaluations should be mapped
        @aribaapi ariba
    */
    public <V> void mapInto (Iterable<V> values, Map<V, K> collector, boolean includeNulls)
    {
        Object[] args = new Object[1];
        for (V value : values) {
            args[0] = value;
            K k = this.evaluate(args);
            if (k != null || includeNulls) {
                collector.put(value, k);
            }
        }
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding the mapping to the returned map.

        @param values the collection of values
        @param includeNulls whether or not <code>null</code> evaluations should be mapped
        @return the resulting map
        @aribaapi ariba
    */
    public final <V> Map<V,K> map (Iterable<V> values, boolean includeNulls)
    {
        Map<V,K> result = MapUtil.map();
        mapInto(values, result, includeNulls);
        return result;
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding the mapping to the returned map.

        @param values the collection of values
        @return the resulting map
        @aribaapi ariba
    */
    public final <V> Map<V,K> map (Iterable<V> values)
    {
        return map(values, false);
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding the reverse mapping to <code>collector</code>.

        @param values the collection of values
        @param collector the resulting map
        @param includeNulls whether or not <code>null</code> evaluations should be mapped
        @aribaapi ariba
    */
    public final <V> void reverseMapInto (Iterable<V> values, Map<K,V> collector, boolean includeNulls)
    {
        Object[] args = new Object[1];
        for (V value : values) {
            args[0] = value;
            K k = this.evaluate(args);
            if (k != null || includeNulls) {
                collector.put(k, value);
            }
        }
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding the reverse mapping to <code>collector</code>.

        @param values the collection of values
        @param collector the resulting map
        @param includeNulls whether or not <code>null</code> evaluations should be mapped
        @aribaapi ariba
    */
    public final <V> void reverseMapInto (V[] values, Map<K,V> collector, boolean includeNulls)
    {
        Object[] args = new Object[1];
        for (V value : values) {
            args[0] = value;
            K k = this.evaluate(args);
            if (k != null || includeNulls) {
                collector.put(k, value);
            }
        }
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding tje results of the evaulations to
        <code>collector</code>

        @param values the collection of values
        @param collector the resulting map
        @param includeNulls whether or not <code>null</code> evaluations should be included
        @aribaapi ariba
    */
    public final <V> void collectInto (Iterable<V> values, Collection<K> collector, boolean includeNulls)
    {
        Object[] args = new Object[1];
        for (V value : values) {
            args[0] = value;
            K k = this.evaluate(args);
            if (k != null || includeNulls) {
                collector.add(k);
            }
        }
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding the results of the evaluations to
        the returned list.

        @param values the collection of values
        @return the resulting list
        @param includeNulls whether or not <code>null</code> evaluations should be included
        @aribaapi ariba
    */
    public final <V> List<K> collect (Iterable<V> values, boolean includeNulls)
    {
        List<K> result = ListUtil.list();
        collectInto(values, result, includeNulls);
        return result;
    }

    /**
        Iterates over <code>values</code> applying <code>this</code> to
        each instance of <code>V</code> and adding the results of the evaluations to
        the returned list.

        @param values the collection of values
        @return the resulting list
        @aribaapi ariba
    */
    public final <V> List<K> collect (Iterable<V> values)
    {
        return collect(values, false);
    }

    /**
        Convenience nested class that adapts a <code>Method</code> into a
        <code>Function</code>
        @aribaapi ariba
    */
    public static class MethodFunction<T> extends Function<T>
    {
        private Method _method;

        public MethodFunction (Method method)
        {
            _method = method;
        }

        public T evaluate (Object... arguments)
        {
            try {
                return (T)_method.invoke(arguments);
            }
            catch (Exception ex) {
                throw new EvaluationException(ex);
            }
        }
    }

    /**
        Convenience function that returns a new <code>Function</code> based on
        the supplied <code>Method</code>. <p/>

        @aribaapi ariba
    */
    public static <X> Function<X> make (Method method)
    {
        return new MethodFunction<X>(method);
    }

    /**
        Convenience function that returns a new <code>Function</code> based on
        the supplied <code>Method</code>. <p/>

        @aribaapi ariba
    */
    public static <X> Function<X> make (Class cls, String methodName, Class... argumentTypes)
    throws NoSuchMethodException
    {
        Method method = cls.getMethod(methodName, argumentTypes);
        return new MethodFunction<X>(method);
    }

    public static Function Identity = new Function() {
        public Integer evaluate (Object... arguments)
        {
            return (Integer)arguments[0];
        }
    };

    public static <X> Function<X> identity ()
    {
        return (Function<X>)Identity;
    }
}
