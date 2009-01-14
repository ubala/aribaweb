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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMap.java#10 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;

/**

Sample Usage:

    int index = AWMap.get(someList).indexOf(someList, someObject);

    @aribaapi private
*/
abstract public class AWMap extends ClassExtension
{
    private static ClassExtensionRegistry MapClassExtensionRegistry =
        new ClassExtensionRegistry();

    // ** Thread Safety Considerations: ClassExtension cache can be considered read-only, so it needs no external locking.

    static {
        registerClassExtension(AWBaseObject.AribaHashtableClass,
            new AWMap_AribaHashtable());
        registerClassExtension(AWBaseObject.JavaMapClass,
            new AWMap_JavaHashtable());
    }

    /////////////////
    // ClassExtension Caching
    /////////////////

    /**
        Registers the AWMap class extension for the given class.

        @aribaapi private

        @param receiverClass class
        @param mapClassExtension AWMap class extension
    */
    public static void registerClassExtension (Class receiverClass,
                                               AWMap mapClassExtension)
    {
        MapClassExtensionRegistry.registerClassExtension(receiverClass,
            mapClassExtension);
    }

    /**
        Gets the AWMap class extension for the given object.

        @aribaapi private

        @param receiver object
        @return AWMap for object
    */
    public static AWMap get (Object receiver)
    {
        return (AWMap)MapClassExtensionRegistry.get(receiver);
    }

    /**
        Gets the AWMap class extension for the given class.

        @aribaapi private

        @param targetClass class
        @return AWMap for class
    */
    public static AWMap get (Class targetClass)
    {
        return (AWMap)MapClassExtensionRegistry.get(targetClass);
    }

    /**
        Creates a new AWMap object.

        @aribaapi private
    */
    public AWMap ()
    {
        super();
    }

    /////////////////
    // Map Api
    /////////////////

    /**
        Removes all mappings from the given map.

        @aribaapi private

        @param receiver map implementation
    */
    abstract public void clear (Object receiver);

    /**
        Returns <code>true</code> if the given map contains a mapping for the
        specified key.

        @aribaapi private

        @param receiver map implementation
        @param key key whose presence in map is to be tested
    */
    abstract public boolean containsKey (Object receiver, Object key);

    /**
        Returns true if the given map maps one or more keys to the specified value.
        More formally, returns true if and only if this map contains at least
        one mapping to a value v such that (value==null ? v==null :
        value.equals(v)).

        @aribaapi private

        @param receiver map implementation
        @param value value whose presence in map is to be tested
        @return true if map maps one or more keys to the specified value
    */
    abstract public boolean containsValue (Object receiver, Object value);

    /**
        Returns the value to which the given map maps the specified key.  Returns
        <code>null</code> if the map contains no mapping for this key.

        @aribaapi private

        @param receiver map implementation
        @param key key whose associated value is to be returned
        @return value to which map maps the specified key, or null if the map
            contains no mapping for this key
    */
    abstract public Object get (Object receiver, Object key);

    /**
        Returns true if the map contains no key-value mappings.

        @aribaapi private

        @param receiver map implementation
        @return true if map contains no key-value mappings
    */
    abstract public boolean isEmpty (Object receiver);

    /**
        Returns an enumeration of the keys in the map.

        @aribaapi private

        @param receiver map implementation
        @return enumeration of the keys in the map
    */
    abstract public java.util.Iterator keys (Object receiver);

    /**
        Associates the specified value with the specified key in the map.  If
        the map previously contained a mapping for this key, the old value is
        replaced.

        @aribaapi private

        @param receiver map implementation
        @param key key with which the specified value is to be associated
        @param value value to be associated with the specified key
        @return previous value associated with specified key, or
            <code>null</code> if there was no mapping for key
    */
    abstract public Object put (Object receiver, Object key, Object value);

    /**
        Removes the mapping for this key from this map if present.

        @aribaapi private

        @param receiver map implementation
        @param key key whose mapping is to be removed from the map
        @return previous value associated with specified key, or
            <code>null</code> if there was no mapping for key
    */
    abstract public Object remove (Object receiver, Object key);

    /**
        Returns the number of key-value mappings in the map.

        @aribaapi private

        @param receiver map implementation
        @return number of key-value mappings in map
    */
    abstract public int size (Object receiver);

    /**
        Returns an enumeration of the values contained in the map.

        @aribaapi private

        @param receiver map implementation
        @return an enumeration of the values contained in the map
    */
    abstract public java.util.Iterator values (Object receiver);
}
