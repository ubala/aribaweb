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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/OrderedList_AribaVector.java#2 $
*/

package ariba.util.fieldvalue;

import java.util.List;
import ariba.util.core.ListUtil;

public class OrderedList_AribaVector extends OrderedList
{
    // ** Thread Safety Considerations: no state here -- no locking required.

    public void addElement (Object receiver, Object element)
    {
        ((List)receiver).add(element);
    }

    public boolean contains (Object receiver, Object targetElement)
    {
        return ((List)receiver).contains(targetElement);
    }

    public Object elementAt (Object receiver, int elementIndex)
    {
        return ((List)receiver).get(elementIndex);
    }

    public java.util.Iterator elements (Object receiver)
    {
        return ((List)receiver).iterator();
    }

    public Object firstElement (Object receiver)
    {
        return ListUtil.firstElement(((List)receiver));
    }

    public int indexOf (Object receiver, Object targetElement)
    {
        return ((List)receiver).indexOf(targetElement);
    }

    public int indexOfIdentical (Object receiver, Object targetElement)
    {
        return ListUtil.indexOfIdentical(((List)receiver),targetElement);
    }

    public void insertElementAt (Object receiver, Object element, int elementIndex)
    {
        ((List)receiver).add(elementIndex, element);
    }

    public boolean isEmpty (Object receiver)
    {
        return ((List)receiver).isEmpty();
    }

    public Object lastElement (Object receiver)
    {
        return ListUtil.lastElement((List)receiver);
    }

    public void removeAllElements (Object receiver)
    {
        ((List)receiver).clear();
    }

    public void setElementAt (Object receiver, Object element, int elementIndex)
    {
        ((List)receiver).set(elementIndex, element);
    }

    public int size (Object receiver)
    {
        return ((List)receiver).size();
    }

    public String toString (Object receiver)
    {
        return receiver.toString();
    }

    public Object sublist (Object receiver, int beginIndex, int endIndex)
    {
        List vectorReceiver = (List)receiver;
        List sublist = ListUtil.list(endIndex - beginIndex);
        for (int index = beginIndex; index < endIndex; index++) {
            Object currentElement = vectorReceiver.get(index);
            sublist.add(currentElement);
        }
        return sublist;
    }

    public Object sublist (Object receiver, int beginIndex)
    {
        return sublist(receiver, beginIndex, ((List)receiver).size());
    }

    public Object mutableInstance (Object receiver)
    {
        return ListUtil.list();
    }
}
