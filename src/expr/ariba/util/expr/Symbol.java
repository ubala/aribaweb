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

    $Id: //ariba/platform/util/expr/ariba/util/expr/Symbol.java#5 $
*/

package ariba.util.expr;

/**
    @aribaapi private
*/
public interface Symbol
{
    public static final Integer Variable = new Integer(0);
    public static final Integer Type = new Integer(1);
    public static final Integer Field = new Integer(2);
    public static final Integer Method = new Integer(3);
    public static final Integer Path = new  Integer(4);
    public static final Integer ThisField = new  Integer(5);
    public static final Integer Key = new  Integer(6);
    public static final Integer ProjectionFindAll = new  Integer(7);
    public static final Integer ProjectionFind = new  Integer(8);
    public static final Integer ProjectionCollect = new  Integer(8);
    public static final Integer ProjectionAggregate = new  Integer(8);
    public static final Integer This = new  Integer(9);


    public static final String[] SymbolNames = {
        "Variable",
        "Type",
        "Field",
        "Method",
        "Path",
        "ThisField",
        "Key",
        "ProjectionFindAll",
        "ProjectionFind",
        "ProjectionCollect",
        "ProjectionAggregate",
        "This"
    };

    public String getName ();
}
