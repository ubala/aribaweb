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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/OrderedList.java#4 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Iterator;
import java.util.Collection;

/**

Sample Usage:

    int index = OrderedList.get(someList).indexOf(someList, someObject);
*/
abstract public class OrderedList extends ClassExtension
{
    private static ClassExtensionRegistry OLClassExtensionRegistry = new ClassExtensionRegistry();

    /**
        Add in a thread local extension registry, it will take precedence over 
        the global registry.
    */
    private static ThreadLocal TLClassExtensionRegistry = new ThreadLocal();

    // ** Thread Safety Considerations: ClassExtension cache can be considered read-only, so it needs no external locking.

    static {
        boolean booleanArray[] = {};
        byte byteArray[] = {};
        char charArray[] = {};
        double doubleArray[] = {};
        float floatArray[] = {};
        int intArray[] = {};
        long longArray[] = {};
        short shortArray[] = {};

        registerClassExtension(ariba.util.core.Vector.class, new OrderedList_AribaVector());
        registerClassExtension(Collection.class, new OrderedList_Collection());
        registerClassExtension(List.class, new OrderedList_List());
        registerClassExtension(java.util.Vector.class, new OrderedList_JavaVector());

        registerClassExtension(Object.class, new OrderedList_ObjectArray());

        registerClassExtension(booleanArray.getClass(), new AWPrimitiveArrayClassExtension());
        registerClassExtension(byteArray.getClass(), new AWPrimitiveArrayClassExtension());
        registerClassExtension(charArray.getClass(), new AWPrimitiveArrayClassExtension());
        registerClassExtension(doubleArray.getClass(), new AWPrimitiveArrayClassExtension());
        registerClassExtension(floatArray.getClass(), new AWPrimitiveArrayClassExtension());
        registerClassExtension(intArray.getClass(), new AWPrimitiveArrayClassExtension());
        registerClassExtension(longArray.getClass(), new AWPrimitiveArrayClassExtension());
        registerClassExtension(shortArray.getClass(), new AWPrimitiveArrayClassExtension());
    }

    /////////////////
    // ClassExtension Caching
    /////////////////
    /**
        Register for the global class extension registry.
        @aribaapi private
    */
    public static void registerClassExtension (Class receiverClass, OrderedList orderedListClassExtension)
    {
        OLClassExtensionRegistry.registerClassExtension(receiverClass, orderedListClassExtension);
    }

    /**
        Register for the thread local class extension registry.
        @aribaapi private
    */
    public static void registerThreadLocalClassExtension (Class receiverClass, OrderedList orderedListClassExtension)
    {
        ClassExtensionRegistry tlRegistry = 
            (ClassExtensionRegistry)TLClassExtensionRegistry.get();
        if (tlRegistry == null) {
                // Make sure this registry is not added to the global
                // list of registries.
            tlRegistry = new ClassExtensionRegistry(false);
            TLClassExtensionRegistry.set(tlRegistry);
        }
        tlRegistry.registerClassExtension(receiverClass, orderedListClassExtension);
    }

    /**
        Clear the thread local class extension registry.
        @aribaapi private
    */
    public static void clearThreadLocalClassExtensionRegistry ()
    {
        ClassExtensionRegistry tlRegistry = 
            (ClassExtensionRegistry)TLClassExtensionRegistry.get();
        TLClassExtensionRegistry.set(null);
    }

    /**
        Retrieve the matching OrderedList.
        It will do the lookup in the thread local registry first, if not
        found, then try the global registry.
        @aribaapi private
    */
    public static OrderedList get (Object receiver)
    {
            // Try the thread local registry first.
        OrderedList list = getFromThreadLocalRegistry(receiver);
        if (list != null) {
            return list;
        }
        return (OrderedList)OLClassExtensionRegistry.get(receiver);
    }

    /**
        Query the thread local registry.
    */
    private static OrderedList getFromThreadLocalRegistry (Object receiver)
    {
        ClassExtensionRegistry registry = 
            (ClassExtensionRegistry)TLClassExtensionRegistry.get();
        if (registry == null) {
            return null;
        }
        return (OrderedList)registry.get(receiver);
    }

    /////////////////
    // OrderList Api
    /////////////////
    abstract public int size (Object receiver);

    abstract public Object elementAt (Object receiver, int elementIndex);

    abstract public void setElementAt (Object receiver, Object element, int elementIndex);

    abstract public void addElement (Object receiver, Object element);

    abstract public void insertElementAt (Object receiver, Object element, int elementIndex);

    abstract public Object mutableInstance (Object receiver);

    /*
        These can (and probably should -- for performance) be overridden by subclassers.
        However, these implementations will suffice given implementations of the abstract
        methods above.
     */

    public Object sublist (Object receiver, int beginIndex, int endIndex)
    {
        Object result = mutableInstance(receiver);
        OrderedList resultExtension = get(result);
        for (int i = beginIndex; i < endIndex; i++) {
            resultExtension.addElement(result, elementAt(receiver, i));
        }
        return result;
    }

    public java.util.Iterator elements (Object receiver)
    {
        return new OrderedListIterator(receiver);
    }

    protected class OrderedListIterator implements Iterator
    {
        private Object _list;
        private int _pos;

        public OrderedListIterator (Object list)
        {
            _list = list;
            _pos = 0;
        }

        public boolean hasNext()
        {
            return _pos < size(_list);
        }

        public java.lang.Object next()
        {
            return elementAt(_list, _pos++);
        }

        public void remove()
        {
            throw new WrapperRuntimeException("operation not supported");
        }
    }

    public int length (Object receiver)
    {
        return size(receiver);
    }

    public boolean isEmpty (Object receiver)
    {
        boolean isEmpty = true;
        int elementCount = size(receiver);
        for (int index = 0; index < elementCount; index++) {
            Object currentElement = elementAt(receiver, index);
            if (currentElement != null) {
                isEmpty = false;
                break;
            }
        }
        return isEmpty;
    }

    public Object lastElement (Object receiver)
    {
        // returns first non-null value searching from end of array.
        Object lastElement = null;
        int elementCount = size(receiver);
        for (int index = elementCount - 1; index >= 0; index--) {
            Object currentElement = elementAt(receiver, index);
            if (currentElement != null) {
                lastElement = currentElement;
                break;
            }
        }
        return lastElement;
    }

    public void removeAllElements (Object receiver)
    {
        int elementCount = size(receiver);
        for (int index = 0; index < elementCount; index++) {
            setElementAt(receiver, null, index);
        }
    }

    public boolean contains (Object receiver, Object targetElement)
    {
        boolean contains = (indexOf(receiver, targetElement) != -1);
        return contains;
    }

    public Object firstElement (Object receiver)
    {
        return elementAt(receiver, 0);
    }

    public int indexOf (Object receiver, Object targetElement)
    {
        int indexOfTarget = -1;
        int elementCount = size(receiver);
        for (int index = 0; index < elementCount; index++) {
            Object currentElement = elementAt(receiver, index);
            if (targetElement.equals(currentElement)) {
                indexOfTarget = index;
                break;
            }
        }
        return indexOfTarget;
    }

    public int indexOfIdentical (Object receiver, Object targetElement)
    {
        int indexOfTarget = -1;
        int elementCount = size(receiver);
        for (int index = 0; index < elementCount; index++) {
            Object currentElement = elementAt(receiver, index);
            if (targetElement == currentElement) {
                indexOfTarget = index;
                break;
            }
        }
        return indexOfTarget;
    }

    public Object sublist (Object receiver, int beginIndex)
    {
        return sublist(receiver, beginIndex, size(receiver));
    }

    public Object copy (Object receiver)
    {
        return sublist(receiver, 0);
    }

    public String toString (Object receiver)
    {
        FastStringBuffer stringBuffer = new FastStringBuffer();
        stringBuffer.append("[Array:");
        int elementCount = size(receiver);
        for (int index = elementCount - 1; index >= 0; index--) {
            Object currentElement = elementAt(receiver, index);
            stringBuffer.append("\n    ");
            stringBuffer.append(currentElement.toString());
            stringBuffer.append(",");
        }
        stringBuffer.append("]");
        return stringBuffer.toString();
    }

    public List toList (Object receiver)
    {
        OrderedList orderedList = OrderedList.get(receiver);
        int count = orderedList.size(receiver);
        List result = ListUtil.list(count);
        for (int i=0; i<count; i++) {
            result.add(orderedList.elementAt(receiver, i));
        }
        return result;
    }    
}
