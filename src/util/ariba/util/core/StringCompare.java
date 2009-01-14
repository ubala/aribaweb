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

    $Id: //ariba/platform/util/core/ariba/util/core/StringCompare.java#5 $
*/

package ariba.util.core;

/**
    An implementation of Compare for Strings

    @aribaapi private
*/
public final class StringCompare implements Compare
{
    /**
        Performs case-sensitive string comparison
    */
    public static final Compare self = new StringCompare(true);
    
    /**
        Performs case-insensitive string comparison
    */
    public static final Compare caseInsensitive = new StringCompare(false);

    private boolean isCaseSensitive = true;
    
    /**
        Private to force use of singleton instance
    */
    private StringCompare (boolean isCaseSensitive)
    {
        this.isCaseSensitive = isCaseSensitive;
    }

    
    /**
        Compares objects <B>o1</B> and <B>o2</B>, returning a value less than,
        equal to, or greater than zero depending on whether <B>o1</B> is less
        than, equal to, or greater than <B>o2</B>.
    */
    public int compare (Object o1, Object o2)
    {
            // Handle null values
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }
        
            // Both strings are non-null
        String s1, s2;

        if (this.isCaseSensitive) {
            s1 = (String)o1;
            s2 = (String)o2;
        }
        else {
            s1 = ((String)o1).toLowerCase();
            s2 = ((String)o2).toLowerCase();
        }
        
        return s1.compareTo(s2);
    }
}
