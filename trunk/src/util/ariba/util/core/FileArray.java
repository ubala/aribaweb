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

    $Id: //ariba/platform/util/core/ariba/util/core/FileArray.java#5 $
*/
package ariba.util.core;

import java.io.File;

/**
    A DynamicArray of Files

    @aribaapi private
*/
public class FileArray extends DynamicArray
{
    private File[] localArray;

    public Object[] alloc (int size)
    {
        localArray = new File[ size ];
        return localArray;
    }

    public final File[] array ()
    {
        return localArray;
    }

    /**
        array had better be of the right type or the next call to
        array() will cause a ClassCastException
    */
    public void setArray (File[] array)
    {
        super.setArray(array);
        this.localArray = array;
    }


    public boolean containsIdentical (Object x)
    {
        for (int i = 0, n = inUse(); i < n; i++) {
            if (array[i] == x) {
                return true;
            }
            else {
                File s1 = (File)array[i];
                File s2 = (File)x;

                if (s1.equals(s2)) {
                    return true;
                }
            }
        }
        return false;
    }

}
