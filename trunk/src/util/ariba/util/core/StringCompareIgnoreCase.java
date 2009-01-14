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

    $Id: //ariba/platform/util/core/ariba/util/core/StringCompareIgnoreCase.java#4 $
*/

package ariba.util.core;

/**
    An case insensitve implementation of Compare for Strings

    @aribaapi private
*/
public final class StringCompareIgnoreCase implements Compare
{
    /**
        The only instance you'll ever need
    */
    public static final Compare self = new StringCompareIgnoreCase();

    /**
        Private to force use of singleton instance
    */
    private StringCompareIgnoreCase ()
    {
    }

    /**
        Compares objects <B>o1</B> and <B>o2</B>, returning a value less than,
        equal to, or greater than zero depending on whether <B>o1</B> is less
        than, equal to, or greater than <B>o2</B>.
    */
    public int compare (Object o1, Object o2)
    {
        String s1 = (String)o1;
        String s2 = (String)o2;
        return s1.compareToIgnoreCase(s2);
    }
}
