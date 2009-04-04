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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/OrderedList_List.java#3 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.ListUtil;

import java.util.List;
import java.util.ArrayList;

public class OrderedList_List extends OrderedList_Collection
{
    // ** Thread Safety Considerations: no state here -- no locking required.

    public Object elementAt (Object receiver, int elementIndex)
    {
        return ((List)receiver).get(elementIndex);
    }

    public Object firstElement (Object receiver)
    {
        return ((List)receiver).get(0);
    }

    public int indexOf (Object receiver, Object targetElement)
    {
        return ((List)receiver).indexOf(targetElement);
    }

    public int indexOfIdentical (Object receiver, Object targetElement)
    {
        return ListUtil.indexOfIdentical(((List)receiver), targetElement);
    }

    public void insertElementAt (Object receiver, Object element, int elementIndex)
    {
        ((List)receiver).add(elementIndex, element);
    }

    public Object lastElement (Object receiver)
    {
        List List = (List)receiver;
        return List.get(List.size());
    }

    public void setElementAt (Object receiver, Object element, int elementIndex)
    {
        ((List)receiver).set(elementIndex, element);
    }

    public Object sublist (Object receiver, int beginIndex, int endIndex)
    {
        List List = (List)receiver;
        List sublist = new ArrayList(endIndex - beginIndex);
        for (int index = beginIndex; index < endIndex; index++) {
            Object currentElement = List.get(index);
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
        return new ArrayList();
    }

    public List toList (Object receiver)
    {
        return (List)receiver;
    }
}
