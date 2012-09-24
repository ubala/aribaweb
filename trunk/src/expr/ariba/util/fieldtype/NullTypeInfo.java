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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/NullTypeInfo.java#8 $
*/

package ariba.util.fieldtype;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @aribaapi private
*/
public class NullTypeInfo implements TypeInfo
{
    // Attribute Access Methods

    public static final NullTypeInfo instance = new NullTypeInfo();

    public String     getName ()
    {
        return "null";
    }

    public String     getImplementationName ()
    {
        return getName();
    }

    public TypeInfo getElementType ()
    {
        return null;
    }

    public int        getAccessibility ()
    {
        return TypeInfo.AccessibilitySafe;
    }

    public boolean    isCollection ()
    {
        return false;
    }

    public boolean    isAssignableFrom (TypeInfo other)
    {
        return false;
    }

    public boolean    isWideningTypeOf (TypeInfo other)
    {
        return false;
    }

    public boolean    isCompatible (TypeInfo other)
    {
        return !PrimitiveTypeProvider.isSimplePrimitiveType(other) &&
               other != PrimitiveTypeProvider.VoidTypeInfo;
    }

    public FieldInfo  getField (TypeRetriever retriever, String name)
    {
        return null;
    }

    public MethodInfo getMethodForName (TypeRetriever retriever,
                                        String name,
                                        List<String> parameters,
                                        boolean staticOnly)
    {
        return null;
    }
    
    public Set/*<MethodInfo>*/ getAllMethods (TypeRetriever retriever)
    {
        return Collections.EMPTY_SET;
    }

    public TypeInfo getElementTypeInfo (TypeRetriever retriever)
    {
        return null;
    }

    public PropertyResolver getPropertyResolver ()
    {
        return null;
    }
}
