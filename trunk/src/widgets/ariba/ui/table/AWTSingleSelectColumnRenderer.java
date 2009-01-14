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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTSingleSelectColumnRenderer.java#11 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWXBasicScriptFunctions;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.widgets.Constants;

public final class AWTSingleSelectColumnRenderer extends AWTDataTable.ColumnRenderer
{
    public AWEncodedString _elementId;

    public void sleep()
    {
        super.sleep();
        _elementId = null;
    }

    public AWComponent selectCurrentItem ()
    {
        _table.displayGroup().setSelectedObject(_table.displayGroup().currentItem());
        return null;
    }

    // if we're the sender, then item is selected during invoke action rather than takeValues
    public boolean isSender ()
    {
        return _elementId.equals(requestContext().requestSenderId());
    }

    public void setFormValue (String formValue)
    {
        if (!isSender() && formValue.equals(_elementId.string())) {
                // the awradioValue will be popped by the AWRadioButtonEnvironment.
                selectCurrentItem();
        }
    }

    /*
    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        if (_elementId.equals(requestContext().requestSenderId())) {
            selectCurrentItem();
            return null;
        } else {
            return super.invokeAction(requestContext, component);
        }
    }
    */
    public Object selectedOrNull ()
    {
        return (_table.displayGroup().currentSelectedState()) ? Boolean.TRUE : null;
    }

    public String onClickString ()
    {
        if (!_table.submitOnSelectionChangeBindingExists() || _table.submitOnSelectionChange()) {
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

    public String disabledString ()
    {
        return (_table.isItemSelectable()) ? null : AWBindingNames.awstandalone;
    }
}
