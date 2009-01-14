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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/AWPrimitiveArrayClassExtension.java#2 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.WrapperRuntimeException;
import ariba.util.core.FastStringBuffer;

import java.lang.reflect.Array;

public final class AWPrimitiveArrayClassExtension extends OrderedList
{
    // ** Thread Safety Considerations: no state here -- no locking required.

    private void throwUnsupportedApiException (String methodName)
    {
        throw new WrapperRuntimeException("Error: Java arrays do not support: \"" + methodName + "\"");
    }

    public void addElement (Object receiver, Object element)
    {
        throwUnsupportedApiException("addElement(Object receiver, Object element)");
    }

    public boolean contains (Object receiver, Object targetElement)
    {
        boolean contains = (indexOf(receiver, targetElement) != -1);
        return contains;
    }

    public Object elementAt (Object receiver, int elementIndex)
    {
        return Array.get(receiver, elementIndex);
    }

    public java.util.Iterator elements (Object receiver)
    {
        throwUnsupportedApiException("elements(Object receiver)");
        return null;
    }

    public Object firstElement (Object receiver)
    {
        return Array.get(receiver, 0);
    }

    public int indexOf (Object receiver, Object targetElement)
    {
        int indexOfTarget = -1;
        int elementCount = Array.getLength(receiver);
        for (int index = 0; index < elementCount; index++) {
            Object currentElement = Array.get(receiver, index);
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
        int elementCount = Array.getLength(receiver);
        for (int index = 0; index < elementCount; index++) {
            Object currentElement = Array.get(receiver, index);
            if (targetElement == currentElement) {
                indexOfTarget = index;
                break;
            }
        }
        return indexOfTarget;
    }

    public void insertElementAt (Object receiver, Object element, int elementIndex)
    {
        throwUnsupportedApiException("insertElementAt (Object receiver, Object element, int elementIndex)");
    }

    public boolean isEmpty (Object receiver)
    {
        boolean isEmpty = true;
        int elementCount = Array.getLength(receiver);
        for (int index = 0; index < elementCount; index++) {
            Object currentElement = Array.get(receiver, index);
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
        int elementCount = Array.getLength(receiver);
        for (int index = elementCount - 1; index >= 0; index--) {
            Object currentElement = Array.get(receiver, index);
            if (currentElement != null) {
                lastElement = currentElement;
                break;
            }
        }
        return lastElement;
    }

    public void removeAllElements (Object receiver)
    {
        int elementCount = Array.getLength(receiver);
        for (int index = 0; index < elementCount; index++) {
            Array.set(receiver, index, null);
        }
    }

    public void setElementAt (Object receiver, Object element, int elementIndex)
    {
        Array.set(receiver, elementIndex, element);
    }

    public int size (Object receiver)
    {
        return Array.getLength(receiver);
    }

    public int length (Object receiver)
    {
        return size(receiver);
    }

    public String toString (Object receiver)
    {
        FastStringBuffer stringBuffer = new FastStringBuffer();
        stringBuffer.append("[Array:");
        int elementCount = Array.getLength(receiver);
        for (int index = elementCount - 1; index >= 0; index--) {
            Object currentElement = Array.get(receiver, index);
            stringBuffer.append("\n    ");
            stringBuffer.append(currentElement.toString());
            stringBuffer.append(",");
        }
        stringBuffer.append("]");
        return stringBuffer.toString();
    }

    public Object sublist (Object receiver, int beginIndex, int endIndex)
    {
        Object sublist[] = new Object[beginIndex - endIndex];
        int sublistIndex = 0;
        for (int index = beginIndex; index < endIndex; index++) {
            Object currentElement = Array.get(receiver, index);
            sublist[sublistIndex] = currentElement;
            sublistIndex++;
        }
        return sublist;
    }

    public Object sublist (Object receiver, int beginIndex)
    {
        return sublist(receiver, beginIndex, Array.getLength(receiver));
    }

    public Object mutableInstance (Object receiver)
    {
        throwUnsupportedApiException("mutableInstance (Object receiver)");
        return null;
    }

}
