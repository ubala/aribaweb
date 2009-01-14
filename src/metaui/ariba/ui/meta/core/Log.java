/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Log.java#2 $
*/
package ariba.ui.meta.core;

import ariba.util.log.Logger;

/**
    This class is extension of ariba.util.log.Log that defines
    extra LogMessageCategories that this package needs.

    @aribaapi private
*/
public final class Log extends ariba.util.log.Log
{
    /**
        @aribaapi ariba
    */
    public static final Logger meta =
        (Logger)Logger.getLogger("aribaweb.meta");

    /**
        @aribaapi ariba
    */
    public static final Logger meta_detail =
        (Logger)Logger.getLogger("aribaweb.meta_detail");

    /**
        @aribaapi ariba
    */
    public static final Logger meta_context =
        (Logger)Logger.getLogger("aribaweb.meta_context");

    /**
        Log classes cannot be instantiated.

        @aribaapi private
    */
    private Log ()
    {

    }
}
