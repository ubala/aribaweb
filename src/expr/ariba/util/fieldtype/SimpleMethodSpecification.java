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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/SimpleMethodSpecification.java#4 $
*/

package ariba.util.fieldtype;

import ariba.util.core.ArrayUtil;
import ariba.util.core.ListUtil;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
    @aribaapi ariba
*/
public class SimpleMethodSpecification extends MethodSpecification
{
    private List<Method> _methods;
    
    public SimpleMethodSpecification ()
    {
        _methods = ListUtil.list();
    }

    /**
        Returns true if second satisfies the specification of first and
        false otherwise.

        By "satisfy", we mean that the name and parameter types of first
        and second are the same and that the declaring class of
        second is a non-strict subclass of first.

        @param first the first Method, may not be null
        @param second the second Method, may not be null
    */
    public static boolean secondSatisfiesFirst (Method first, Method second)
    {
        return first.getName().equals(second.getName()) &&
            first.getDeclaringClass().isAssignableFrom(second.getDeclaringClass()) &&
            ArrayUtil.arrayEquals(first.getParameterTypes(), second.getParameterTypes());
    }
    
    public boolean isSatisfiedBy (Method method)
    {
        for (Iterator i = _methods.iterator(); i.hasNext();) {
            Method methodInSpec = (Method)i.next();
            if (secondSatisfiesFirst(methodInSpec, method)) {
                return true;
            }
        }
        return false;
    }
    
    public List getMethods ()
    {
        return _methods;
    }
    
    public void add (Method method)
    {
        _methods.add(method);
    }
    
    public void addIfAbsent (Method method)
    {
        ListUtil.addElementIfAbsent(_methods, method);
    }
    
    public void addAll (Collection/*<Method>*/ collection)
    {
        _methods.addAll(collection);
    }
    
    public void addAllIfAbsent (Collection/*<Method>*/ collection)
    {
        ListUtil.addElementsIfAbsent(_methods, collection);
    }    
}
