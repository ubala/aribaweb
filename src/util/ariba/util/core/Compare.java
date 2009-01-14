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

    $Id: //ariba/platform/util/core/ariba/util/core/Compare.java#4 $
*/

package ariba.util.core;

/**
    <code>Compare</code> is an interface that provides an object comparison
    function for use by the static sorting methods in <code>Sort</code>.

    @aribaapi documented
*/
public interface Compare
{
    /**
        Compares objects two objects for sort order.

        @param o1 the first object to compare
        @param o2 the second object to compare
        @return   a value less than, equal to, or greater than zero depending
                  on whether <B>o1</B> is less than, equal to, or greater than
                  <B>o2</B>

        @aribaapi documented
    */
    public int compare (Object o1, Object o2);

    /**
        The caller is not required to return LessThan, any number less
        than 0 will do.

        @aribaapi private
    */
    public static final int LessThan  =  -1;

    /**
        The caller is not required to return EqualTo, a hardcoded return of 0
        will be supported.
        
        @aribaapi private
    */
    public static final int EqualTo  =  0;

    /**
        The caller is not required to return GreaterThan, any number
        greater than 0 will do.

        @aribaapi private
    */
    public static final int GreaterThan  =  1;

}
