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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWCaseInsensitiveHashtable.java#5 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Hashtable;

public final class AWCaseInsensitiveHashtable extends Hashtable
{
    public Object put (Object key, Object element)
    {
        if (key instanceof String) {
            return super.put(key, element);
        }
        else {
            throw new AWGenericException(getClass().getName() + ": key must be a String");
        }
    }

    protected boolean objectsAreEqualEnough (Object obj1, Object obj2)
    {
        if (obj1 == obj2) {
            return true;
        }
        return ((String)obj1).equalsIgnoreCase(((String)obj2));
    }

    // This code borrowed from ariba.util.core.InternCharToString.java/normalHashCode()
    protected int getHashValueForObject (Object object)
    {
        String string = (String)object;
        int hashCode = 0;
        int offset = 0;
        int len = string.length();

        if (len < 16) {
            for (int i = len ; i > 0; i--) {
                hashCode = (hashCode * 37) + Character.toUpperCase(string.charAt(offset));
                offset++;
            }
        }
        else {
                // only sample some characters
            int skip = len / 8;
            for (int i = len ; i > 0; i -= skip, offset += skip) {
                hashCode = (hashCode * 39) + Character.toUpperCase(string.charAt(offset));
            }
        }
        return hashCode;
    }
}
