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

    $Id: //ariba/platform/util/core/ariba/util/core/ClassExtensionRegistry.java#11 $
*/

package ariba.util.core;

import java.util.List;

/**
    The ClassExtensionRegistry class provides a convenient and
    consistent way to cache ClassExtension subclasses.  All
    ClassExtensions must know the class for which they are
    implemented so that the FieldPath can compare its
    _previousCalssExtension's class and possible avoid a hash lookup.

    The ClassExtensionRegistry allows for registering ClassExtension
    instances by targetClass (where targetClass may be an interface).

    The lookup algorithm for a ClassExtension for a given class is recursive.
    Upon looking up a ClassExtension for a given target object,
    the ClassExtensionRegistry will recurse up the target object's
    superclass chain and interface inheritance graph until it finds a
    match.  To avoid doing this recursion each time, a clone of the
    ClassExtension is registered for each subclass as the recursion
    unwinds.  The next time the target object's ClassExtension is
    sought, it will be found immediately without recursion.  Of
    course, this applies for all subclasses between the base class and
    the target object as those wil have been cloned/registered along
    the way.

    @aribaapi private
*/
public class ClassExtensionRegistry extends Object
{
    private static final ClassExtension Dummy;
    private static final int MaxMruEntries = 8;
    private static ClassExtensionRegistry[] ClassExtensionRegistries =
        new ClassExtensionRegistry[0];
    private final GrowOnlyHashtable _categoriesByClass;
    private final ClassExtension[] _mruClassExtensions;
    private int _mruHead  = 0;

    static {
        Dummy = new DummyClassExtension();
        Dummy.forClass = Dummy.getClass();
    }

    public static List getClassExtensions (Class targetClass)
    {
        List classExtensions = ListUtil.list();
        ClassExtensionRegistry[] classExtensionRegistries = ClassExtensionRegistries;
        int count = classExtensionRegistries.length;
        for (int index = 0; index < count; index++) {
            ClassExtensionRegistry currentRegistry = classExtensionRegistries[index];
            ClassExtension currentClassExtension =
                currentRegistry.get(targetClass);
            if (currentClassExtension != null) {
                classExtensions.add(currentClassExtension);
            }
        }
        return classExtensions;
    }

    private static void addRegistry (ClassExtensionRegistry classExtensionRegistry)
    {
        synchronized (ClassExtensionRegistry.class) {
            int classExtensionCount = ClassExtensionRegistries.length;
            ClassExtensionRegistry[] classExtensionRegistries = new
                ClassExtensionRegistry[classExtensionCount + 1];
            System.arraycopy(ClassExtensionRegistries, 0, classExtensionRegistries, 0,
                             classExtensionCount);
            classExtensionRegistries[classExtensionCount] = classExtensionRegistry;
            ClassExtensionRegistries = classExtensionRegistries;
        }
    }

    /**
        constructred ClassExtensionRegistry.
    */
    public ClassExtensionRegistry ()
    {
        this(true);
    }

    /**
        Constructs a ClassExtensionRegistry.

        @param addToGlobalRegistries whether to add myself to the
            global list of ClassExtensionRegistries.
    */
    public ClassExtensionRegistry (boolean addToGlobalRegistries)
    {
        _mruHead = 0;
        _mruClassExtensions = new ClassExtension[MaxMruEntries];
        // Initialize with Dummy to avoid null check elsewhere
        for (int index = 0; index < MaxMruEntries; index++) {
            _mruClassExtensions[index] = Dummy;
        }
        _categoriesByClass = new GrowOnlyHashtable.IdentityMap();
        if (addToGlobalRegistries) {
            ClassExtensionRegistry.addRegistry(this);
        }
    }

    /**
        Allow a ClassExtension implementation to be registered/cached
        for a given base class.  Note that the classExtension is
        cloned before caching the cloned version.

        @param targetClass the class for which the classExtension applies.
        @param classExtension the classExtension to be registered.
        @return returns the cloned version of the classExtension whose
        class has been set to targetClass.
    */
    public ClassExtension registerClassExtension (Class targetClass,
                                                  ClassExtension classExtension)
    {
        if (classExtension != null) {
            classExtension = putClassExtension(targetClass, classExtension);
            //classExtension.checkForCollisions();
        }
        return classExtension;
    }

    private ClassExtension putClassExtension (Class targetClass,
                                                  ClassExtension classExtension)
    {
        // returns a copy of the classExtension with the targetClass set.
        if (classExtension != null) {
            classExtension = (ClassExtension)classExtension.clone();
            classExtension.setForClass(targetClass);
            _categoriesByClass.put(targetClass, classExtension);
        }
        return classExtension;
    }
    
    /**
        @deprecated use get(Object)
    */
    public ClassExtension getClassExtension (Class targetClass)
    {
        return get(targetClass);
    }

    /**
        Lookup a classExtension whose class matches the class of
        target.  If no classExtension is located for the target's
        class, then we recurse up the class' interface graph and
        superclass chain until a match is found.  Once found, copies
        of the classExtension are registered for each subclass in the
        recursion.  This avoids future recursions for the target's
        class and its superclasses.

        @param targetClass the for which the classExtension applies.
        @return the ClassExtension which was located with the class
        ivar set appropriately (to targetClass)
    */
    public ClassExtension get (Class targetClass)
    {
        ClassExtension classExtension = mruClassExtension(targetClass);
        if (classExtension == null) {
            classExtension =
                (ClassExtension)_categoriesByClass.get(targetClass);
            if (classExtension == null) {
                // lookup is factored out since its recursive
                classExtension = lookupClassExtensionForClass(targetClass);
            }
            mruAddClassExtension(classExtension);
        }
        return classExtension;
    }

    /**
     * Return the closest fitting ClassExtension for the given target Object,
     * which must be non-null. It appears that null can be returned if no
     * registered extensions are found for target. If the target is a kind of
     * ClassProxy, we use its RealClass instead of its Class, so we lookup
     * based on the object the proxy represents, rather than on the proxy class
     * itself; this is much more functional and helps proxies play the role of
     * their classes without breakage or instantiation.
     * 
     * @aribaapi private
     */
    public ClassExtension get (Object target)
    {
        Class targetClass = ClassExtension.getRealClass(target);
        // TODO: Fix 1-4GRKDL so this code handles extensions registered on
        // interfaces correctly.
        return get(targetClass);
    }

    /**
        Return an array of ClassExtension representing all the extensions
        that have been registered.
    */
    public Object[] get ()
    {
        return _categoriesByClass.elementsArray();
    }

    private ClassExtension mruClassExtension (Class targetClass)
    {
        int currentHead = _mruHead;
        for (int index = currentHead; index < MaxMruEntries; index++) {
            ClassExtension currentClassExtension = _mruClassExtensions[index];
            if (currentClassExtension.forClass == targetClass) {
                _mruHead = index;
                return currentClassExtension;
            }
        }
        for (int index = 0; index < currentHead; index++) {
            ClassExtension currentClassExtension = _mruClassExtensions[index];
            if (currentClassExtension.forClass == targetClass) {
                _mruHead = index;
                return currentClassExtension;
            }
        }
        return null;
    }

    private void mruAddClassExtension (ClassExtension classExtension)
    {
        if (classExtension != null) {
            int mruHead = _mruHead;
            int newHead = ((mruHead == 0) ? MaxMruEntries : mruHead) - 1;
            _mruClassExtensions[newHead] = classExtension;
            _mruHead = newHead;
        }
    }

    /**
        (see description in get()).

        @see #get
        @param targetClass the class for which we need a ClassExtension.
        @return the ClassExtension which was located with the class
        ivar set appropriately (to targetClass)
    */
    private ClassExtension lookupClassExtensionForClass (Class targetClass)
    {
        ClassExtension classExtension =
            (ClassExtension)_categoriesByClass.get(targetClass);
        if (classExtension == null) {
            // Look at *all* interfaces implemented by targetClass
            // before recursing up the superclass chain.

            // TODO: Fix 1-4GRKDL so this code handles extensions registered on
            // interfaces correctly.

            Class[] targetInterfaces = targetClass.getInterfaces();
            int interfaceCount = targetInterfaces.length;
            for (int index = 0; index < interfaceCount; index++) {
                Class currentInterface = targetInterfaces[index];
                // this will register a different classExtension for each interface.
                classExtension = lookupClassExtensionForClass(currentInterface);
                if (classExtension != null) {
                    putClassExtension(targetClass, classExtension);
                    break;
                }
            }
            if (classExtension == null && !targetClass.isInterface()) {
                Class targetSuperclass = targetClass.getSuperclass();
                if (targetSuperclass != null) {
                    // this will register a different classExtension for each subclass.
                    classExtension = lookupClassExtensionForClass(targetSuperclass);
                    // no need to check for null here as we'll always have Object as a backstop
                    // as long as targetClass is not an interface (which we test for above).
                    classExtension = putClassExtension(targetClass, classExtension);
                }
            }
        }
        return classExtension;
    }
}

class DummyClassExtension extends ClassExtension
{
}

