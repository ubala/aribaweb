/*
    Copyright 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/AndPredicate.java#1 $
*/

package ariba.util.core;

import java.util.Set;

/**
 * Implements a boolean and of the contained set of predicates
 * @param <T>
 */
public class AndPredicate<T> extends CompositePredicate<T>
{
    public AndPredicate(Set<Predicate<T>> predicates)
    {
        super(predicates);
    }

    /**
     * For and to be true, all predicates must be true.
     * Base-case of 0 predicates is defined as true
     *
     * @param t of type T
     * @return
     */
    @Override
    public boolean evaluate(T t)
    {
        for(Predicate<T> predicate : _predicates) {
            if (!predicate.evaluate(t)) {
                return false;
            }
        }
        return true;
    }
}
