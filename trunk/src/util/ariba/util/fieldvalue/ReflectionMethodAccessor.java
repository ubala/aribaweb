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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ReflectionMethodAccessor.java#2 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
The ReflectionMethodAccessor is a wrapper for java.lang.reflect.Method
and provides a uniform interface so that users needn't know if the accesor
they're dealing with is either a Method or a Field.
*/
public class ReflectionMethodAccessor extends BaseAccessor
{
    protected final Method _method;
    protected CompiledAccessor _compiledAccessor;
    protected int _accessCount = 0;

    /**
    Constructs a new ReflectionMethodAccessor with the given Class and Method.

    @param forClass the class for which the accessor should apply
    @param method the Method which this instance wraps
    */
    protected ReflectionMethodAccessor (Class forClass, String fieldName, Method method)
    {
        super(forClass, fieldName);
        _method = method;
        Assert.that(Modifier.isPublic(_method.getModifiers()),
                    "Methods referenced via FieldValue must be public: %s", _method);
        Assert.that(Modifier.isPublic(_method.getDeclaringClass().getModifiers()),
                    "Methods referenced via FieldValue must be within a public class:\n\t%s\n\t%s\n",
                    _method.getDeclaringClass(), _method);
    }

    public String toString ()
    {
        return Fmt.S("<%s method: %s>", getClass().getName(), _method);
    }
}
