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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTMultiSelectColumnRenderer.java#13 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWXBasicScriptFunctions;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.html.BindingNames;
import ariba.ui.widgets.Constants;

public final class AWTMultiSelectColumnRenderer extends AWTDataTable.ColumnRenderer
{
    public AWEncodedString _elementId;

    public void sleep()
    {
        super.sleep();
        _elementId = null;
    }

    public boolean disabled ()
    {
        return !_table.isItemSelectable();
    }

    public boolean isSender ()
    {
        return _elementId.equals(requestContext().requestSenderId());
    }

    public void setFormValue (String formValue)
    {
        if (disabled()) {
            return;
        }
        boolean booleanValue = (formValue != null) && (formValue.length() != 0);
        _table.displayGroup().setCurrentSelectedState(booleanValue);
    }

    public String checkedString ()
    {
        return _table.displayGroup().currentSelectedState() ? ariba.ui.aribaweb.html.BindingNames.awstandalone : null;
    }

    public String disabledString ()
    {
        return disabled() ? BindingNames.awstandalone : null;
    }


    public AWEncodedString onClickString ()
    {
        if (_table.submitOnSelectionChange()) {
            AWResponse response = response();
            response.appendContent(Constants.Space);
            response.appendContent(Constants.OnClick);
            response.appendContent(Constants.Equals);
            response.appendContent(Constants.Quote);
            AWRequestContext requestContext = requestContext();
            AWXBasicScriptFunctions.appendSubmitCurrentForm(requestContext, _elementId);
            response.appendContent(Constants.Quote);
        }
        return null;
    }
}
