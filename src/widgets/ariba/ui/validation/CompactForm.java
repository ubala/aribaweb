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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/CompactForm.java#1 $
*/

package ariba.ui.validation;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWErrorInfo;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWRequestContext;

import java.util.List;

public final class CompactForm extends AWComponent
{
    private String _errorMessage;

    protected void sleep()
    {
        _errorMessage = null;
        super.sleep();
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        errorManager().disableErrorPanel();
        super.renderResponse(requestContext, component);
    }

    public boolean showError ()
    {
        return errorManager().isErrorDisplayEnabled() &&
               errorMessage() != null;
    }

    public String errorMessage ()
    {
        if (_errorMessage == null) {
            AWErrorManager errorManager = errorManager();
            if (errorManager.hasErrors()) {
                List<AWErrorInfo> errors = errorManager.getAllErrors();
                // only show first error for now
                _errorMessage = errors.get(0).getMessage();
            }
        }
        return _errorMessage;
    }
}