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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMutableRefCount.java#4 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Compare;
import ariba.util.core.StringUtil;

public final class AWMutableRefCount extends AWBaseObject implements Compare
{
    public Object object;
    public int value;

    public AWMutableRefCount (Object objectValue)
    {
        super();
        object = objectValue;
    }

    public String toString ()
    {
        return StringUtil.strcat(AWUtil.toString(value), " ", object.toString());
    }

    public int compare (Object object1, Object object2)
    {
        AWMutableRefCount refCount1 = ((AWMutableRefCount)object1);
        AWMutableRefCount refCount2 = ((AWMutableRefCount)object2);
        int count1 = refCount1.value;
        int count2 = refCount2.value;
        int comparison = 0;
        if (count1 < count2) {
            comparison = -1;
        }
        else if (count1 > count2) {
            comparison = 1;
        }
        return comparison;
    }
}
