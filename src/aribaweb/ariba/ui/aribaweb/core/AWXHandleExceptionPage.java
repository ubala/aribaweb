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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWXHandleExceptionPage.java#8 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWHandleExceptionPage;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.SystemUtil;

public class AWXHandleExceptionPage extends AWComponent implements AWHandleExceptionPage
{
    public static final String PageName = "AWXHandleExceptionPage";
    public Throwable _exception;
    public boolean _displayStackTrace;

    public void init ()
    {
        _displayStackTrace = application().isDebuggingEnabled();
        super.init();
    }

    public void setException (Throwable exception)
    {
        _exception = exception;
        if (_exception == null) {
            Log.logStack(Log.aribaweb, "** ERROR: exception may not be null.");
        }
    }
}
