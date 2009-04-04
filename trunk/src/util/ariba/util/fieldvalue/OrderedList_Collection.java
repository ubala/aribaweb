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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/OrderedList_Collection.java#1 $
*/
package ariba.util.fieldvalue;

import ariba.util.core.WrapperRuntimeException;
import ariba.util.core.ListUtil;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
    Partial implementation of OrderedList protocol for Collections
    (Useful for Sets)
 */
public class OrderedList_Collection extends OrderedList
{
    public void addElement (Object receiver, Object element)
    {
        ((Collection)receiver).add(element);
    }

    public boolean contains (Object receiver, Object targetElement)
    {
        return ((Collection)receiver).contains(targetElement);
    }

    public java.util.Iterator elements (Object receiver)
    {
        return ((Collection)receiver).iterator();
    }

    public boolean isEmpty (Object receiver)
    {
        return ((Collection)receiver).isEmpty();
    }

    public void removeAllElements (Object receiver)
    {
        ((Collection)receiver).clear();
    }

    public int size (Object receiver)
    {
        return ((Collection)receiver).size();
    }

    public String toString (Object receiver)
    {
        return receiver.toString();
    }

    public Object mutableInstance (Object receiver)
    {
        try {
            return receiver.getClass().newInstance();
        } catch (InstantiationException e) {
            throw new WrapperRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new WrapperRuntimeException(e);
        }
    }

    /*
        Positional Operations - pseudo supported
     */
    public Object firstElement (Object receiver)
    {
        return elements(receiver).next();
    }
    
    public void insertElementAt (Object receiver, Object element, int elementIndex)
    {
        ((Collection)receiver).add(element);
    }


    public Object lastElement (Object receiver)
    {
        throw new WrapperRuntimeException("operation not supported");
    }

    public void setElementAt (Object receiver, Object element, int elementIndex)
    {
        ((Collection)receiver).add(element);
    }

    /*
        Positional Operations - not supported
     */
    public Object elementAt (Object receiver, int elementIndex)
    {
        throw new WrapperRuntimeException("operation not supported");
    }

    public int indexOf (Object receiver, Object targetElement)
    {
        throw new WrapperRuntimeException("operation not supported");
    }

    public int indexOfIdentical (Object receiver, Object targetElement)
    {
        throw new WrapperRuntimeException("operation not supported");
    }

    public Object sublist (Object receiver, int beginIndex, int endIndex)
    {
        throw new WrapperRuntimeException("operation not supported");
    }

    public Object sublist (Object receiver, int beginIndex)
    {
        return sublist(receiver, beginIndex, ((Collection)receiver).size());
    }

    public List toList (Object receiver)
    {
        return new ArrayList((Collection)receiver);
    }
}
