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

    $Id: //ariba/platform/util/core/ariba/util/core/LockHandlerContext.java#5 $
*/

package ariba.util.core;

import ariba.util.log.Logger;

/**
    @aribaapi private
*/
public class LockHandlerContext
{
    public int     waitTimeSeconds;
    public int     iteration;
    public Object  payload;

    public Logger logger;
    public String  message;

    public void log (Logger logger, String message)
    {
        this.logger = logger;
        this.message = message;
    }
}
