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

    $ $
*/

package ariba.util.fieldvalue;

import ariba.util.core.ClassExtension;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
    Used to manipulate Collection-typed properties.
    E.g. RelationshipField.addTo(obj, "permissions", newPerm) may call obj.addToPermissions(newPerm)
    or may manipulate the underlying field directly.
 */
public class RelationshipField extends ClassExtension
{
    protected static final ClassExtensionRegistry Registry = new ClassExtensionRegistry();

    GrowOnlyHashtable<String, Adder>_adderForProp = new GrowOnlyHashtable<String, Adder>();
    GrowOnlyHashtable<String, Remover>_removerForProp = new GrowOnlyHashtable<String, Remover>();
    FieldInfo.Collection _fieldInfos;

    static {
        Registry.registerClassExtension(Object.class, new RelationshipField());
    }

    public static RelationshipField get (Object target)
    {
        return ((RelationshipField)Registry.get(target.getClass()));
    }

    public static void addTo (Object target, String key, Object val)
    {
        get(target).addToProp(target, FieldPath.sharedFieldPath(key), val);
    }

    public static void removeFrom (Object target, String key, Object val)
    {
        get(target).removeFromProp(target, FieldPath.sharedFieldPath(key), val);
    }

    public static void addTo (Object target, FieldPath fieldPath, Object val)
    {
        get(target).addToProp(target, fieldPath, val);
    }

    public static void removeFrom (Object target, FieldPath fieldPath, Object val)
    {
        get(target).removeFromProp(target, fieldPath, val);
    }

    public void addToProp (Object target, FieldPath fieldPath, Object val)
    {
        FieldPath fieldPathCdr = fieldPath._nextFieldPath;
        if (fieldPathCdr == null) {
            String key = fieldPath._fieldName;
            Adder adder = _adderForProp.get(key);
            if (adder == null) {
                adder = createAdder(key, val.getClass());
                _adderForProp.put(key, adder);
            }
            adder.addTo(target, val);
        }
        else {
            Object nextTargetObject = FieldValue.get(target).getFieldValuePrimitive(target, fieldPath);
            if (nextTargetObject != null) {
                RelationshipField.get(nextTargetObject).addToProp(nextTargetObject, fieldPathCdr, val);
            }
        }
    }

    public void removeFromProp (Object target, FieldPath fieldPath, Object val)
    {
        FieldPath fieldPathCdr = fieldPath._nextFieldPath;
        if (fieldPathCdr == null) {
            String key = fieldPath._fieldName;
            Remover remover = _removerForProp.get(key);
            if (remover == null) {
                remover = createRemover(key, val.getClass());
                _removerForProp.put(key, remover);
            }
            remover.removeFrom(target, val);
        }
        else {
            Object nextTargetObject = FieldValue.get(target).getFieldValuePrimitive(target, fieldPath);
            if (nextTargetObject != null) {
                RelationshipField.get(nextTargetObject).removeFromProp(nextTargetObject, fieldPathCdr, val);
            }
        }
    }

    Adder createAdder (String key, Class argClass)
    {
        // introspect  for method of form addTo<PropName>(obj)
        String methodName = Fmt.S("addTo%s%s", key.substring(0,1).toUpperCase(), key.substring(1));
        try {
            Method m = forClass.getMethod(methodName, argClass);
            if (m != null) {
                return new MethodAdder(m);
            }
        } catch (NoSuchMethodException e) {
            /* ignore */
        }
        FieldInfo info = infoForField(key);
        Assert.that(info != null, Fmt.S("Unknown property for class %s: %s", forClass, key));
        return new CollectionFieldAdder(key);
    }

    Remover createRemover (String key, Class argClass)
    {
        // introspect for method of form removeFrom<PropName>(obj)
        String methodName = Fmt.S("removeFrom%s%s", key.substring(0,1).toUpperCase(), key.substring(1));
        try {
            Method m = forClass.getMethod(methodName, argClass);
            if (m != null) {
                return new MethodRemover(m);
            }
        } catch (NoSuchMethodException e) {
            /* ignore */
        }

        return new CollectionFieldRemover(key);
    }

    FieldInfo infoForField (String name)
    {
        if (_fieldInfos == null) {
            _fieldInfos = FieldInfo.fieldInfoForClass(forClass, true, true);
        }
        return _fieldInfos.infoForField(name);
    }

    protected Collection newCollectionOfType (Class type)
    {
        if (type == List.class) {
            type = ArrayList.class;
        }
        else if (type == Set.class) {
            // Identity?
            type = HashSet.class;
        }
        Collection col;
        try {
            col = (Collection)type.newInstance();
        } catch (InstantiationException e) {
            throw new WrapperRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new WrapperRuntimeException(e);
        }
        return col;
    }

    protected Collection newCollectionForKey (String key)
    {
        FieldInfo info = infoForField(key);
        Assert.that(info != null, Fmt.S("Unknown property for class %s: %s", forClass, key));
        return newCollectionOfType(info.getType());
    }

    public static abstract class Adder {
        abstract public void addTo (Object obj, Object val);

    }
    abstract static class Remover {
        abstract public void removeFrom (Object obj, Object val);
    }

    class CollectionFieldAdder extends Adder
    {
        FieldPath _fieldPath;

        CollectionFieldAdder (String key)
        {
            _fieldPath = new FieldPath(key);
        }

        public void addTo (Object obj, Object val)
        {
            Collection col = (Collection)_fieldPath.getFieldValue(obj);
            if (col == null) {
                col = newCollectionForKey(_fieldPath.fieldPathString());
                _fieldPath.setFieldValue(obj, col);
            }
            if (!col.contains(val)) col.add(val);
        }
    }

    class CollectionFieldRemover extends Remover
    {
        FieldPath _fieldPath;

        CollectionFieldRemover (String key)
        {
            _fieldPath = new FieldPath(key);
        }

        public void removeFrom (Object obj, Object val)
        {
            Collection col = (Collection)_fieldPath.getFieldValue(obj);
            if (col != null) col.remove(val);
        }
    }

    class MethodAdder extends Adder
    {
        Method _adderMethod;

        MethodAdder (Method adderMethod)
        {
            _adderMethod = adderMethod;
        }

        public void addTo (Object obj, Object val)
        {
            try {
                _adderMethod.invoke(obj, val);
            } catch (IllegalAccessException e) {
                throw new WrapperRuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new WrapperRuntimeException(e);
            }
        }
    }

    class MethodRemover extends Remover
    {
        Method _removerMethod;

        MethodRemover (Method removerMethod)
        {
            _removerMethod = removerMethod;
        }

        public void removeFrom (Object obj, Object val)
        {
            try {
                _removerMethod.invoke(obj, val);
            } catch (IllegalAccessException e) {
                throw new WrapperRuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new WrapperRuntimeException(e);
            }
        }
    }
}
