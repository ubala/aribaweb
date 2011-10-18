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

    $Id: //ariba/platform/util/core/ariba/util/core/StringArray.java#6 $
*/
package ariba.util.core;

/**
    A DynamicArray of Strings

    @see ariba.util.core.DynamicArray
    @aribaapi public
*/
public class StringArray extends DynamicArray
{
    private String[] localArray;

    public Object[] alloc (int size)
    {
        localArray = new String[ size ];
        return localArray;
    }

    public static final String[] EMPTY_ARRAY = new String[0];

    /**
        Typesafe access to the storage array
        @return the String[] array currently in use by this
        object. It is not copied.

        @aribaapi public
    */
    public final String[] array ()
    {
        return localArray;
    }

    public void setArray (String[] array)
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
                String s1 = (String)array[i];
                String s2 = (String)x;

                if (s1.equals(s2)) {
                    return true;
                }
            }
        }
        return false;
    }

}
