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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMemoryStats.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWMutableRefCount;
import ariba.util.core.Compare;
import java.util.Map;
import java.util.List;
import ariba.util.core.Compare;
import ariba.util.core.Sort;

public final class AWMemoryStats extends AWBaseObject
{
    public static Map RefCountTable;

    public static void addElement (Object object)
    {
        if (RefCountTable == null) {
            RefCountTable = MapUtil.map();
        }
        String className = object.getClass().getName();
        synchronized (RefCountTable) {
            AWMutableRefCount mutableRefCount = (AWMutableRefCount)RefCountTable.get(className);
            if (mutableRefCount == null) {
                mutableRefCount = new AWMutableRefCount(className);
                RefCountTable.put(className, mutableRefCount);
            }
            mutableRefCount.value++;            
        }
    }

    public static void removeElement (Object object)
    {
        String className = object.getClass().getName();
        synchronized (RefCountTable) {
            AWMutableRefCount mutableRefCount = (AWMutableRefCount)RefCountTable.get(className);
            mutableRefCount.value--;
        }
    }

    public static AWMutableRefCount[] sortedRefCounts ()
    {
        AWMutableRefCount[] refCountsArray = null;
        if (RefCountTable != null) {
            Object[] elementsArray = null;
            synchronized (RefCountTable) {
                elementsArray = MapUtil.elementsArray(RefCountTable);
            }
            int elementsArrayLength = elementsArray.length;
            if (elementsArrayLength > 0) {
                List vector = ListUtil.list();
                for (int index = 0; index < elementsArrayLength; index++) {
                    AWMutableRefCount refCount = (AWMutableRefCount)elementsArray[index];
                    if (refCount.value > 100 || refCount.value < 0) {
                        vector.add(refCount);
                    }
                }
                elementsArray = vector.toArray();
                elementsArrayLength = elementsArray.length;
                if (elementsArrayLength > 0) {
                    refCountsArray = new AWMutableRefCount[elementsArrayLength];
                    System.arraycopy(elementsArray, 0, refCountsArray, 0, elementsArrayLength);
                    Compare compare = refCountsArray[0];
                    Sort.objects(refCountsArray, compare, Sort.SortDescending);                
                }
            }
        }
        return refCountsArray;
    }
}
