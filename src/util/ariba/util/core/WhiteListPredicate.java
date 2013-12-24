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

    $Id: //ariba/platform/util/core/ariba/util/core/WhiteListPredicate.java#1 $
*/

package ariba.util.core;

import java.util.Set;

/**
 * Implements a predicate for checking whether a property on T (fetched via
 * property getter) is contained in the white-list
 * @param <T>
 */

public class WhiteListPredicate<T,P> extends Predicate<T>
{
    private Set<P> _whiteList;
    private PropertyGetter<T,P> _getter;

    public WhiteListPredicate(Set<P> whiteList, PropertyGetter<T,P> getter)
    {
        _whiteList = whiteList;
        _getter = getter;
    }

    /**
     * Use a property from T fetched via the property getter
     * to check if the whiteList contains the property
     *
     * @param t of type T
     * @return
     */
    @Override
    public boolean evaluate(T t)
    {
        P val = _getter.getProperty(t);
        return (val == null) ? false :_whiteList.contains(val);
    }
}
