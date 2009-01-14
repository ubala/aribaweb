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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWFullValidationHandler.java#3 $
*/

package ariba.ui.aribaweb.core;

import java.util.List;

/**
    Validation can be performed automatically by the error manager
    by registering a handler on a page-by-page basis that performs the
    actual validation. This interface defines the validation handler.
    It is up to the implementer to provide the context for validation.
    Handlers are registered with the error manager.  Handler
    registration are clear out at the beginning of each append cycle and
    must be re-registered during append.  No explicit un-registration is
    needed.

    @aribaapi ariba
*/
public interface AWFullValidationHandler
{
    /**
        Carry out the evaluation.  The handler is responsible for registering
        the errors with the error manager of the page.  The pageComponent is
        passed down to the handler and should be use to record the errors.

        @aribaapi ariba
    */
    public void evaluateValidity (AWComponent pageComponent);
}
