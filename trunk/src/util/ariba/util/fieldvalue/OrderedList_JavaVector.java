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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/OrderedList_JavaVector.java#2 $
*/

package ariba.util.fieldvalue;

import java.util.Vector;

public final class OrderedList_JavaVector extends OrderedList
{
    // ** Thread Safety Considerations: no state here -- no locking required.

    public void addElement (Object receiver, Object element)
    {
        ((Vector)receiver).addElement(element);
    }

    public boolean contains (Object receiver, Object targetElement)
    {
        return ((Vector)receiver).contains(targetElement);
    }

    public Object elementAt (Object receiver, int elementIndex)
    {
        return ((Vector)receiver).elementAt(elementIndex);
    }

    public java.util.Iterator elements (Object receiver)
    {
        return ((Vector)receiver).iterator();
    }

    public Object firstElement (Object receiver)
    {
        return ((Vector)receiver).firstElement();
    }

    public int indexOf (Object receiver, Object targetElement)
    {
        return ((Vector)receiver).indexOf(targetElement);
    }

    public int indexOfIdentical (Object receiver, Object targetElement)
    {
        int indexOfTarget = -1;
        Vector vector = (Vector)receiver;
        int elementCount = vector.size();
        for (int index = 0; index < elementCount; index++) {
            Object currentElement = vector.elementAt(index);
            if (targetElement == currentElement) {
                indexOfTarget = index;
                break;
            }
        }
        return indexOfTarget;
    }

    public void insertElementAt (Object receiver, Object element, int elementIndex)
    {
        ((Vector)receiver).insertElementAt(element, elementIndex);
    }

    public boolean isEmpty (Object receiver)
    {
        return ((Vector)receiver).isEmpty();
    }

    public Object lastElement (Object receiver)
    {
        return ((Vector)receiver).lastElement();
    }

    public void removeAllElements (Object receiver)
    {
        ((Vector)receiver).removeAllElements();
    }

    public void setElementAt (Object receiver, Object element, int elementIndex)
    {
        ((Vector)receiver).setElementAt(element, elementIndex);
    }

    public int size (Object receiver)
    {
        return ((Vector)receiver).size();
    }

    public String toString (Object receiver)
    {
        return receiver.toString();
    }

    public Object sublist (Object receiver, int beginIndex, int endIndex)
    {
        Vector vectorReceiver = (Vector)receiver;
        Vector sublist = new Vector(endIndex - beginIndex);
        for (int index = beginIndex; index < endIndex; index++) {
            Object currentElement = vectorReceiver.elementAt(index);
            sublist.addElement(currentElement);
        }
        return sublist;
    }

    public Object sublist (Object receiver, int beginIndex)
    {
        return sublist(receiver, beginIndex, ((Vector)receiver).size());
    }

    public Object mutableInstance (Object receiver)
    {
        return new Vector();
    }
}
