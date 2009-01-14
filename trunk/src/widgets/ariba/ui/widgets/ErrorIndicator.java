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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ErrorIndicator.java#2 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWHighLightedErrorScope;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWErrorInfo;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.table.AWTDataTable;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import java.util.List;

public class ErrorIndicator extends ErrorFlag
{
    private Boolean _showRequired;
    public AWEncodedString _customContentDivId;
    protected AWEncodedString _indicatorId;
    public AWEncodedString _errorContentDivId;
    private Boolean _autoHideBubble;
    private List _errorMsgs;

    // used in repetition binding
    public String curItem;

    protected void awake ()
    {
        super.awake();
        _customContentDivId = requestContext().nextElementId();
        _indicatorId = requestContext().nextElementId();
        _errorContentDivId = requestContext().nextElementId();
    }

    protected void sleep ()
    {
        super.sleep();
        _showRequired = null;
        _customContentDivId = null;
        _indicatorId = null;
        _errorContentDivId = null;
        _autoHideBubble = null;
        _errorMsgs = null;
        curItem = null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        super.renderResponse(requestContext, component);

        AWTDataTable datatable = (AWTDataTable)env().peek(AWTDataTable.EnvKey);
        Object tableItem = (datatable != null)
            ? datatable.displayGroup().currentItem() : null;

        // keep track of the table association information.
        // used later to auto scroll to an error in the table.
        if (tableItem != null) {
            Object[] errorKeys = AWErrorManager.getErrorKeyFromBindings(this);
            errorManager().setAssociatedTableItem(errorKeys, datatable, tableItem);
//                ariba.ui.aribaweb.util.Log.aribaweb_errorManager.debug(
//                    "***** ErrorIndicator: record table item association: item=%s error=%s",
//                    tableItem, AWErrorInfo.getKeysAsString(errorKeys));
        }
    }

    public boolean autoHideBubble ()
    {
        if (_autoHideBubble == null) {
            AWBinding binding = bindingForName("autoHideBubble", true);
            if (binding == null) {
                _autoHideBubble = Boolean.TRUE;
            }
            else {
                _autoHideBubble = Constants.getBoolean(booleanValueForBinding(binding));
            }
        }

        return _autoHideBubble.booleanValue();
    }

    public boolean autoScroll ()
    {
        // If the indicator is in a table, only auto scroll if
        // the table item has been forced visible.  Otherwise,
        // the bubble is displayed outside of the table.
        if (!ListUtil.nullOrEmptyList(_errorInfoList)) {
            boolean isInTable = false;
            boolean wasAutoScrolled = false;
            for (int i = 0; i < _errorInfoList.size(); i++) {
                AWErrorInfo error = (AWErrorInfo)_errorInfoList.get(i);
                if (error.getAssociatedTableItem() != null) {
                    isInTable = true;
                    wasAutoScrolled = wasAutoScrolled || error.getWasAutoScrolled();
                }
            }
            if (isInTable && !wasAutoScrolled) {
                Log.aribaweb_errorManager.debug("***** skipping bubble autoscroll");
                return false;
            }
        }
        return true;
    }

    public AWEncodedString customContentDivId ()
    {
        return _customContentDivId;
    }

    public AWEncodedString indicatorId ()
    {
        return _indicatorId;
    }

    public boolean showRequired ()
    {
        if (_showRequired == null) {
            AWBinding binding = bindingForName("showRequired", true);
            if (binding == null) {
                _showRequired = Boolean.FALSE;
            }
            else {
                _showRequired = Constants.getBoolean(booleanValueForBinding(binding));
            }
        }

        return _showRequired.booleanValue();
    }

    public String errorOrWarning ()
    {
        return error();
    }

    public boolean isHighLightedError ()
    {
        if (isNavigable()) {
            return AWHighLightedErrorScope.isInHighLightedErrorScope(env());
        }
        else {
            // don't popup the bubble if not navigable
            return false;
        }
    }

    public boolean hasMultipleErrors ()
    {
        return !ListUtil.nullOrEmptyList(_errorInfoList) && _errorInfoList.size() > 1;
    }

    public List getErrorMessages ()
    {
        if (_errorMsgs == null) {
            _errorMsgs = ListUtil.list();
            if (!ListUtil.nullOrEmptyList(_errorInfoList)) {
                for (int i = 0; i < _errorInfoList.size(); i++) {
                    AWErrorInfo error = (AWErrorInfo)_errorInfoList.get(i);
                    _errorMsgs.add(error.getMessage());
                }
            }
        }
        return _errorMsgs;
    }

    public String showBubbleScript ()
    {
        return "ariba.Widgets.showBubble(this); return false";
    }

    public String hideBubbleScript ()
    {
        if (autoHideBubble()) {
            return "ariba.Widgets.hideBubble()";
        }
        else {
            return null;
        }
    }
}
