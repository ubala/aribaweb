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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValue.java#3 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;

/**
The FieldValue class provides convenience methods which dispatch
methods of the FieldValue interface for a target object.  This class
maintains a cache of ClassExtension implementations for the FieldValue
interface and will lookup a classExtension for each convenience method in
here before forwarding to the classExtension for the actual implementation of the method.

You may use these convenience methods to always do the dispatching,
but this incurrs a lookup of the classExtension each time.  If you will be invoking
these methods on several instances all known to be the same class, you can
 do the lookup of the classExtension yourself (see get(...)) and
invoke the classExtension methods directly.
*/
abstract public class FieldValue extends ClassExtension
{
    public static final int Setter = 0;
    public static final int Getter = Setter + 1;
    protected static final ClassExtensionRegistry
        FieldValueClassExtensionRegistry = new ClassExtensionRegistry();

    /**
        Force the loading/initialization of the FieldValue_Object.
    */
    static {
        Class dummyClass = FieldValue_Object.class;
        registerClassExtension(Object.class,
                               new FieldValue_Object());
        registerClassExtension(Class.class,
                               new FieldValue_Class());
        registerClassExtension(java.util.Map.class,
                               new FieldValue_JavaHashtable());
        registerClassExtension(Extensible.class, new FieldValue_Extensible());
    }

    /**
    Put a ClassExtension implementation of the FieldValue interface into the
    cache of categories which is used to dispatch the FieldValue cover
     methods in this class.  Note that the FieldValue_Object will be cloned
    before caching, so you cannot depend upon getting back the exact same
     instance later with get(...).

    @param targetObjectClass the root class for which the classExtension applies
    @param fieldValueClassExtension the classExtension implementation of the FieldValue interface
    */
    public static void registerClassExtension (
        Class targetObjectClass,
        FieldValue fieldValueClassExtension)
    {
        FieldValueClassExtensionRegistry.registerClassExtension(targetObjectClass,
                                                             fieldValueClassExtension);
    }

    /**
    Retrieve a ClassExtension registered by registerClassExtension(...).  Note that
    this will clone the ClassExtension objects which are registered so that each
    subclass will have its own classExtension implementation.  See ClassExtensionRegistry
    for details on this.

    @param target the object for which a classExtension applies
    @return the classExtension which applies for the target
    */
    public static FieldValue get (Class targetClass)
    {
        return (FieldValue)FieldValueClassExtensionRegistry.get(targetClass);
    }

    public static FieldValue get (Object target)
    {
        return (FieldValue)FieldValueClassExtensionRegistry.get(target.getClass());
    }

    /**
    Converts fieldPathString into a FieldPath (from a pool of shared
    FieldPaths) and calls setFieldValue(FieldPath, Object).

    @param target see the FieldPath version of this method
    @param fieldPathString the dotted fieldPath String from which a shared FieldPath
    instance will be obtained
    @param value see the FieldPath version of this method
    @return see the FieldPath version of this method
    */
    public static void setFieldValue (Object target,
        String fieldPathString, Object value)
    {
        FieldPath fieldPath = FieldPath.sharedFieldPath(fieldPathString);
        FieldValue.get(target).setFieldValue(target, fieldPath, value);
    }

    /**
    Converts fieldPathString into a FieldPath (from a pool of
    shared FieldPaths) and calls getFieldValue(FieldPath).

    @param target see the FieldPath version of this method
    @param fieldName the dotted fieldPath String from which a shared FieldPath
    instance will be obtained
    @return see the FieldPath version of this method
    */
    public static Object getFieldValue (Object target, String fieldPathString)
    {
        FieldPath fieldPath = FieldPath.sharedFieldPath(fieldPathString);
        return FieldValue.get(target).getFieldValue(target, fieldPath);
    }

    /**
    Creates and returns a new FieldValueAccessor (by default,
    a ReflectionFieldValueAccessor) for the given target and fieldName.
    No caching is done by this method. This method is designed to be
    overridden by subclasses of FieldValue_Object which want to define
    their own specialized accessors.  Note that the target is passed
    rather than its class so that accessors can be created at a finer
    granularity than class.  Certain meta-data driven classes require
    this flexibility.

    @param target the object for which the accessor will be created
    @param fieldName the name of the field for which the accessor will be created
    @return a new FieldValueAccessor (ReflectionFieldValueAccessor by default)
    */
    abstract public FieldValueAccessor createAccessor (Object target, String fieldName,
                                                       int type);

    /**
    Maintains a cache of FieldValueAccessor's for the instance by fieldName.
    In general, all instances of a given class will share the same
    FieldValueAccessor for a given fieldName.  However, certain meta-data
    driven classes may need to introspect upon the instance before returning
    the proper accessor.

    @param target the object for which the accessor will be looked up
    @param fieldName the name of the field for the accessor
    @return the cached FieldValueAccessor (ReflectionFieldValueAccessor by default)
    */
    abstract public FieldValueAccessor getAccessor (Object target, String fieldName,
                                                    int type);

    /**
    Sets the value on the reveiver using the fieldName indicated by
    fieldPath -- only the first node of the fieldPath is considered if it is
    a multi-node path.

    @param target the object on which the value will be set for the field identiifed by
     fieldPath
    @param the fieldPath node (which contains a candidate accessor) to be
    used to set the value on target.
    @param value the value to set on the target
    */
    abstract public void setFieldValuePrimitive (Object target, FieldPath fieldPath,
                                                 Object value);

    /**
    Gets the value from the reveiver using the fieldName indicated by
    fieldPath -- only the first node of the fieldPath is considered if
    it is a multi-node path.

    @param target the object from which to get the value of the field identified by
     fieldPath
    @param fieldPath the fieldPath node which identifes the field to get.
    @return the value obtained from the target using the fieldPath
    */
    abstract public Object getFieldValuePrimitive (Object target, FieldPath fieldPath);

    /**
    Recursively calls getFieldValuePrimitive() with the head of the fieldPath
    list up to the last fieldPath node and then calls setFieldValuePrimitive().
    Each time the recursion iterates, the receiver is the value of the
    previous getFieldValuePrimitive().

    @param target the object on which to start the recursion for setting
    the value using the fieldPath
    @param fieldPath the linked list of fieldPath nodes used to navigate from
    target to the final object in the chain, upon which the value is set
    @param value the value which is set on the final object in the chain
    */
    abstract public void setFieldValue (Object target,
                               FieldPath fieldPath, Object value);

    /**
    Recursively calls getFieldValuePrimitive() with the head of the fieldPath list.
    Each time the recursion iterates, the receiver is the value of the previous
    getFieldValuePrimitive().

    @param target the first object from which to start the recursion for getting
    the value identified by the fieldPath.
    @param fieldPath the linked list of fieldPath nodes that identifes the value to get
    @return the value obtained from the last object in the chain
    */
    abstract public Object getFieldValue (Object target, FieldPath fieldPath);
}
