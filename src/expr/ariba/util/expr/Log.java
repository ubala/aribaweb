/*
    Copyright (c) 1996-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/expr/ariba/util/expr/Log.java#4 $
*/

package ariba.util.expr;

import ariba.util.log.Logger;

/**
    @aribaapi private
*/
public class Log extends ariba.util.log.Log
{
    /**
        Log message category to log messages from expression processing. 

        @aribaapi documented
    */
    public static final Logger expression =
        (Logger)Logger.getLogger("util.expression");

    /**
        Log message category to log messages from expression evaluation. 
        Off by default; output only if Priority set to INFO or DEBUG.

        @aribaapi private
    */
    public static final Logger exprTrace =
        (Logger)Logger.getLogger("exprTrace");
}
