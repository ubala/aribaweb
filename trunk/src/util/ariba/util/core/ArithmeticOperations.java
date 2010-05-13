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

    $Id: //ariba/platform/util/core/ariba/util/core/ArithmeticOperations.java#5 $
*/

package ariba.util.core;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import java.math.BigDecimal;

/**
    @aribaapi private
*/
public abstract class ArithmeticOperations extends ClassExtension
{
    protected static final ClassExtensionRegistry ExtensionRegistry =
                                  new ClassExtensionRegistry();

    public static void registerClassExtension (
        Class targetClass,
        ArithmeticOperations classExtension)
    {
        ExtensionRegistry.registerClassExtension(targetClass, classExtension);
    }

    public static ArithmeticOperations get (Class targetClass)
    {
        return (ArithmeticOperations)ExtensionRegistry.get(targetClass);
    }

    public static ArithmeticOperations get (Object target)
    {
        return (ArithmeticOperations)ExtensionRegistry.get(target.getClass());
    }

    public static ArithmeticOperations getByName (String name)
    {
        Class classObj = ClassUtil.classForName(name, false);
        if (classObj == null) {
            return null;
        }
        return (ArithmeticOperations)ExtensionRegistry.get(classObj);
    }

    public abstract Object add (Object obj1, Object obj2);
    
    public abstract Class additionReturnType (Class objType1, Class objType2);

    public abstract Object substract (Object obj1, Object obj2);

    public abstract Class subtractionReturnType (Class objType1, Class objType2);    
    
    public abstract Object multiply (Object obj1, BigDecimal factor);
   
    public abstract Class multiplicationReturnType (Class objType1, Class objType2);
    
    public abstract Object divide (Object obj1, Object divisor);
    
    public abstract Class divisionReturnType (Class objType1, Class objType2);
    
    public abstract boolean canCastFrom (Class fromClass);

    public abstract int compare (Object obj1, Object obj2);
}
