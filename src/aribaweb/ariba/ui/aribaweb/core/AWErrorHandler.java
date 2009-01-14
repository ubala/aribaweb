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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWErrorHandler.java#5 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import java.util.List;

/**
    This interface allows the implementer to navigate to an error spot on
    another page.  Handlers are registered with the error manager.  Handler
    registration are clear out at the beginning of each append cycle and
    must be re-registered during append.  No explicit un-registration is
    needed.

    @aribaapi ariba
*/
public interface AWErrorHandler
{
    /**
        Go to the error spot of the current error.

        @aribaapi ariba

        @param  error the error being navigated to
        @param  pageComponent the current page
        @return  Return null if don't you want to handle navigation for this error.
                 Return pageComponent if you want to stay on the current page.
                 Return a new page if you want to go to another page.
    */
    public AWComponent goToError (AWErrorInfo error, AWComponent pageComponent);

    public boolean canGoToErrorImmediately (AWErrorInfo error, AWComponent pageComponent);

    public boolean canGoToErrorWithLink (AWErrorInfo error, AWComponent pageComponent);

    public AWErrorInfo selectFirstError (List /*AWErrorInfo*/ errors);
}
