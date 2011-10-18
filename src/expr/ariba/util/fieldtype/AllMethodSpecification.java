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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/AllMethodSpecification.java#3 $
*/

package ariba.util.fieldtype;

import ariba.util.core.ArrayUtil;
import java.lang.reflect.Method;

/**
    @aribaapi private
*/
public class AllMethodSpecification extends MethodSpecification
{
    private Class _class;
    
    public AllMethodSpecification (Class aClass)
    {
        _class = aClass;
    }
    
    public boolean isSatisfiedBy (Method method)
    {
        if (method.getDeclaringClass() == _class) {
            return true;
        }
        //call to _class.getDeclaredMethod() is more expensive than this logic as
        //it tries to match all the methods for possible return type override
        String name = method.getName();
        Class[] types = method.getParameterTypes();
        Method[] methods = _class.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(name)) {
                Class[] ts = m.getParameterTypes();
                if (ArrayUtil.arrayEquals(ts, types)) {
                    return true;
                }
            }
        }
        return false;
    }
}
