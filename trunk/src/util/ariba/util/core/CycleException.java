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

    $Id: //ariba/platform/util/core/ariba/util/core/CycleException.java#3 $
*/

package ariba.util.core;


/**
    An exception that is thrown when a cycle is created.
    For example, such cycles can be created in Roles or Groups.
    While you may catch this exception, you must not force a
    database commit after it, and must instead rollback or abandon
    your transaction. If you do not, the code will make arbitrary
    deletions in an attempt to break the cycle. Even those deletions
    may not cover all cases and you may corrupt your data.

    @aribaapi ariba
*/
public class CycleException extends RuntimeException
{
    private Object _obj1;
    private Object _obj2;

    /**
        Creates a cycle exception between two nodes in a graph,
        representing the edge.

        @param obj1 one node of the cycle edge
        @param obj2 the other node of the cycle edge
        @aribaapi ariba
    */
    public CycleException (Object obj1, Object obj2)
    {
        super(Fmt.S("A cycle was detected between %s and %s", obj1, obj2));
        this._obj1=obj1;
        this._obj2=obj2;
    }

    /**
        Return the objects in context for the current cycle exception
        @aribaapi ariba
    */
    public Object[] getCycleContext ()
    {
        return new Object[]{_obj1, _obj2};
    }
}
