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

    $Id: //ariba/platform/util/core/ariba/util/core/ModMofNPredicate.java#1 $
*/

package ariba.util.core;

/**
 * Implements a predicate on the mod of some hash function of T.
 * The actual predicate on the hashed value is defined as
 *    modValLower <= (hash % modWhat) < modValUpper
 * @param <T>
 */

public class ModMofNPredicate<T> extends Predicate<T>
{
    private int _modWhat;
    private int _modValLower;
    private int _modValUpper;
    private PropertyHasher<T> _hasher;

    private boolean modPredicate(int hash) {
        return (_modValLower <= (hash % _modWhat))
          &&  ((hash % _modWhat) < _modValUpper);
    }

    /**
     * The mod predicate (see above) is configured via the 3 parameters.
     * If the result of the mod satisfies this predicate: lowerBound <= result < upperBound
     * the the predicate is satisfied.
     *
     * @param hasher
     * @param modWhat
     * @param modValLower
     * @param modValUpper
     */
    public ModMofNPredicate(PropertyHasher<T> hasher,
                          int modWhat,
                          int modValLower,
                          int modValUpper
    )
    {
        _modWhat = modWhat;
        _modValLower = modValLower;
        _modValUpper = modValUpper;
        _hasher = hasher;
    }

    @Override
    public boolean evaluate(T t)
    {
        int h = _hasher.getPropertyHash(t);
        return modPredicate(h);
    }

}
